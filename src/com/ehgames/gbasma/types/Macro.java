package com.ehgames.gbasma.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ehgames.gbasma.Parser;
import com.ehgames.gbasma.Preprocessor;
import com.ehgames.gbasma.Tokenizer;

public class Macro {
	private static final Map<String, Macro>		ALL		= new HashMap<>();

	public static Macro get(String name) {
		return ALL.get(name);
	}
	
	private static List<Object> collectArgs(Tokenizer tokens) {
		List<Object> args = new ArrayList<>();
		do {
			Object arg = Preprocessor.collectSingleArg(tokens);
			if(arg != null) args.add(arg);
		} while(tokens.next() != null); // should be ','
		return args;
	}

	public final String	name;
	public List<String>	lines	= new ArrayList<>();

	public Macro(String name) {
		this.name = name;
		ALL.put(name, this);
	}
	
	public void collectArgsAndRun(Parser parser, Tokenizer tokens) {
		List<Object> args = collectArgs(tokens);
		List<String> lines = run(args);
		parser.parseMacroCode(lines);
	}

	public List<String> run(List<Object> args) {
		int argc = args.size();
		Preprocessor.setVar("_NARG", args == null ? 0 : argc);
		List<String> converted = new ArrayList<>();
		char sub = '1';
		for(String line : lines) {
			while(true) {
				int idx = line.indexOf('\\');
				if(idx == -1) break;
				
				int argIdx = line.charAt(idx + 1) - sub;
				Object arg = argIdx < argc ? args.get(argIdx) : "0";
				line = line.substring(0, idx) + arg + line.substring(idx + 2);	
			}
			converted.add(line);
		}
		return converted;
	}
}