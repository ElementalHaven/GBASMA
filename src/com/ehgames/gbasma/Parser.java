package com.ehgames.gbasma;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.ehgames.gbasma.types.Constants;
import com.ehgames.gbasma.types.Instruction;
import com.ehgames.gbasma.types.Label;
import com.ehgames.gbasma.types.Macro;
import com.ehgames.gbasma.types.Method;
import com.ehgames.gbasma.types.Variable;

public class Parser {
	private static List<String> getLines(final String localPath) {
		Path path = MetaGen.rootPath.resolve(localPath);
		try {
			return Files.readAllLines(path);
		} catch(IOException ioe) {
			System.err.println("Failed to read lines for file \"" + localPath + '"');
			return null;
		}
	}

	// currently active sections
	private Method		activeMethod;
	private Variable	activeVariable;
	/** whether or not we're handling a macro. macro lines should not be added to the method */ 
	private int			macroLevel;
	
	/** if false, a newly encountered global label should be considered fallthrough */
	private boolean		instrWasUnconditional;
	
	private Tokenizer	tokens			= new Tokenizer();
	
	// labels
	private List<Label>	globalLabels	= new ArrayList<>();
	private List<Label>	localLabels		= new ArrayList<>();
	
	// preprocessor if/else stuff
	private boolean		codeInactive	= false;
	private boolean		hadActiveBlock	= false;
	// FIXME getting looping preprocessor stuff implemented will be a mess and a half
	//int loopStart = -1;
	//int loopCount = 0;
	//char macrosub = '1';

	public void parse(final String localPath) {
		System.out.println("Parsing " + localPath + " ...");
		final List<String> lines = getLines(localPath);
		if(lines != null) {
			parseInternal(localPath, lines);
		}
	}
	
	public void parseMacroCode(final List<String> lines) {
		macroLevel++;
		try {
			parseInternal(null, lines);
		} finally {
			macroLevel--;
		}
	}

	/** code to be shared by the normal file parsing and running macros */
	private void parseInternal(String localPath, final List<String> lines) {
		final int lineCount = lines.size();
		int lineNo = 0;

		// not a for loop so we can rip macros out
		while(lineNo < lineCount) {
			final String line = lines.get(lineNo);
			tokens.init(line);
			int nextLine = parseLine(localPath, lines, lineNo);

			if(activeMethod != null && macroLevel == 0) {
				activeMethod.lines.add(line);
			}

			lineNo = nextLine;
		}
	}
	
	private boolean evaluatePreprocessorIfStatement() {
		boolean invert = false;
		Object token = tokens.next();
		
		// Full on hardcoding, hacks, assumptions, and featureset that exactly encompasses my use cases
		// I'm not putting in the insane amount of work that would be effectively a full blown compiler so whatever
		if("!DEF".equals(token)) {
			token = "DEF";
			invert = true;
		}
		if("DEF".equals(token)) {
			// if DEF(whatever)
			// if !DEF(whatever)
			// if DEF(this) || DEF(that) || DEF(whoknows)
			
			tokens.next();
			String defName = tokens.nextAsString();
			boolean result = Macro.get(defName) != null;
			if(invert) result = !result;
			// dont care about multiple conditions. you get 1
			return result;
		} else if("STRSUB".equals(token)) {
			// if STRSUB("\2", 1, 1) == "\""
			
			System.err.println("Messy substring check: " + tokens.getSource()); 
			return false;
		} else if("STRIN".equals(token)) {
			// if STRIN("\1", "VAR_") != 1
			
			tokens.next(); // (
			String testStr = Tokenizer.unquote(tokens.nextAsString());
			tokens.next(); // ,
			String srcFor = Tokenizer.unquote(tokens.nextAsString());
			tokens.next();
			
			boolean has = testStr.contains(srcFor);
			String extra = tokens.nextAsString();
			if(extra == null) return has;
			if("!=".equals(extra)) has = !has;
			boolean expected = Preprocessor.tokenAsInt(tokens.next()) == 1;
			return expected == has;
		} else {
			if(Tokenizer.isQuotedString(token)) {
				if("==".equals(tokens.next())) {
					String other = tokens.nextAsString();
					return token.equals(other);
				} else {
					// this should never happen in code that compiles
					return false;
				}
			}
			
			tokens.rewind(1);
			return Preprocessor.doMath(tokens) != 0;
		}
	}

	private boolean parsePreprocessorLine(String token) {
		switch(token) {
			case "if":
			case "elif":
				if(hadActiveBlock) {
					codeInactive = true;
				} else {
					hadActiveBlock = evaluatePreprocessorIfStatement();
					codeInactive = !hadActiveBlock;
				}
				return true;
			case "else":
				codeInactive = hadActiveBlock;
				hadActiveBlock = true;
				return true;
			case "endc":
				codeInactive = false;
				hadActiveBlock = false;
				return true;
			case "rept": // indicates loop start. parameter is loop count
			case "endr": // indicates loop end
			case "shift": // 
				// these preprocessor directives aren't supported atm
				return true;
			// these three are only ever used once, and for the purpose of visualizing an image in binary form
			case "pusho":
				return true;
			case "opt":
				String replacement = tokens.nextAsString();
				if(replacement != null && replacement.length() > 2 && replacement.charAt(0) == 'b') {
					Tokenizer.binaryStupidity = new char[] { replacement.charAt(1), replacement.charAt(2) };
				}
				return true;
			case "popo":
				Tokenizer.binaryStupidity = null;
				return true;
		}
		return false;
	}

	private int parseLine(final String localPath, List<String> lines, final int lineNo) {
		String token = tokens.nextAsString();
		if(token != null) {
			boolean handled = parsePreprocessorLine(token);

			if(!handled && !codeInactive) {
				switch(token) {
					case "INCLUDE":
						String filename = Tokenizer.unquote(tokens.nextAsString());
						parse(filename);
						if(instrWasUnconditional) {
							// make sure the INCLUDE "filename.asm" isn't tacked onto the end
							closeAllActive(null, null);
						}
						break;
					case "INCBIN":
						// no point including binary files atm
						break;
					case "MACRO":
						Macro macro = new Macro(mostRecentLabel().name);
						return MacroParser.parse(macro, lines, lineNo + 1);
					case "SECTION":
						// TODO ex: SECTION "WRRAM", WRAM0
						break;
					case "UNION":
						Memory.startUnion();
						break;
					case "NEXTU":
						Memory.nextUnionBlock();
						break;
					case "ENDU":
						Memory.endUnion();
						break;
					default:
						if(token.endsWith(":") || token.charAt(0) == '.') {
							parseLabel(token, localPath, lines, lineNo);
							return parseLine(localPath, lines, lineNo);
						} else {
							Instruction instr = Instruction.get(token);
							if(instr != null) {
								if(activeMethod == null) {
									List<Label> aliases = mostRecentLabelAndAliases();
									activeMethod = newMethod(aliases, localPath, lines, lineNo);
								}
								instrWasUnconditional = MethodParser.parseInstruction(activeMethod, instr, tokens);
							} else if(Keywords.isDataSize(token)) {
								parseData(token);
							} else {
								String equ = tokens.nextAsString();
								if("EQU".equals(equ) || "EQUS".equals(equ)) {
									Object value = tokens.next();
									// this should work just fine atm since the parser is smart enough
									// it should be noted however that EQUS is a text replacement
									// and EQU is a value or macro parameter
									Constants.set(token, value);
								} else if("=".equals(equ)) {
									int val = Preprocessor.doMath(tokens);
									Preprocessor.setVar(token, val);
								} else {
									tokens.rewind(1);
									
									macro = Macro.get(token);
									if(macro != null) {
										macro.collectArgsAndRun(this, tokens);
									} else {
										System.err.println("Unknown first token \"" + token + "\" of line: " + tokens.getSource());
									}
								}
							}
						}
						break;
				}
			}
		}
		return lineNo + 1;
	}

	private void parseData(String sizeType) {
		int size;
		if(activeMethod != null) {
			size = Memory.getSize(sizeType, tokens);
		} else {
			Variable var = activeVariable;
			if(var == null) {
				Label label = mostRecentLabel();
				var = new Variable(label);
			}
			size = Memory.getSize(sizeType, tokens, var);
		}
		Memory.advanceBytes(size);
	}

	private void parseLabel(String name, String localPath, List<String> lines, int lineNo) {
		int idx = name.indexOf(':');
		if(idx != -1) name = name.substring(0, idx);

		Label label = new Label(name, localPath, lineNo);
		if(label.isLocal()) {
			localLabels.add(label);
		} else {
			closeAllActive(name, tokens.getSource());
			globalLabels.add(label);
		}
	}

	private Method newMethod(List<Label> aliases, String path, List<String> lines, int lineNo) {
		Label label = aliases.get(0);
		Method m = new Method();
		m.name = label.name;
		m.address = label.address;
		m.file = path;

		if(label.file.equals(path)) {
			m.lineNo = label.lineNo;
			for(int i = label.lineNo - 1; i < lineNo; i++) {
				m.lines.add(lines.get(i));
			}
		} else {
			m.lineNo = lineNo + 1;
		}

		for(Label l : aliases) {
			m.names.add(l.name);
			Method.ALL_METHODS.put(l.name, m);
		}

		System.out.println("\tParsing method " + m.name);
		return m;
	}

	private void closeAllActive(String fallthroughInto, String fallthroughLine) {
		activeVariable = null;
		if(activeMethod == null) return;

		localLabels.clear();
		if(instrWasUnconditional) {
			activeMethod.removeTrailingEmptyLines();
		} else {
			activeMethod.fallsInto = fallthroughInto;
			activeMethod.lines.add(fallthroughLine);
		}
		System.out.println("\tEnded method by line\"" + fallthroughLine + "\". total " + activeMethod.lines.size() + " lines");

		activeMethod = null;
	}

	private Label mostRecentLabel() {
		return globalLabels.get(globalLabels.size() - 1);
	}

	// this is meant for when the initial parser call ends and all files have been read
	public void cleanup() {
		instrWasUnconditional = true;
		closeAllActive(null, null);
	}

	public List<Label> mostRecentLabelAndAliases() {
		int end = globalLabels.size();
		int i = end - 1;
		Label mostRecent = globalLabels.get(i);
		int lineNo = mostRecent.lineNo;
		if(lineNo != 1) {
			for(; i > 0; i--) {
				Label l = globalLabels.get(i - 1);
				// null check put in to prevent exceptions. not sure if thats the proper thing though
				if(l.file == null || !l.file.equals(mostRecent.file)) break;
				if(l.lineNo + 1 != lineNo) break;
	
				lineNo = l.lineNo;
			}
		}
		return globalLabels.subList(i, end);
	}
}