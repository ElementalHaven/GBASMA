package com.ehgames.gbasma;

import java.util.Set;

import com.ehgames.gbasma.types.Instruction;
import com.ehgames.gbasma.types.Method;

public class MethodParser {
	/**
	 * @return True if the instruction did not have any conditions
	 */
	private static boolean handleJumpOrReturn(Method method, Instruction instr, Tokenizer tokens) {
		switch(instr.name) {
			case "reti":
				return true;
			case "ret":
				return tokens.next() == null;
			case "jp":
			case "jr":
				return !parseCall(method, tokens);
		}
		// impossible to get here but compiler doesn't know that
		return true;
	}
	
	private static void handleLoad(Method method, Tokenizer tokens, boolean isHigh) {
		boolean isFirstParam = true;
		boolean isPointer = false;
		boolean likelyConstant = false;
		boolean firstInPointer = true;
		while(true) {
			Object token = tokens.next();
			if(token == null) return;
			
			Class<?> cls = token.getClass();
			if(cls == Character.class) {
				char c = (Character) token;
				if(c == ',') {
					isFirstParam = false;
					isPointer = false;
					likelyConstant = false;
				} else if(c == '[') {
					isPointer = true;
					firstInPointer = true;
				} else if(c == ']') {
					isPointer = false;
				} else if(c == '(') {
					likelyConstant = true;
				}
			} else if(cls == String.class) {
				String str = token.toString();
				if(Keywords.isMathSymbol(str)) {
					likelyConstant = true;
					continue;
				}
				if(Keywords.isRegister(str)) continue;
				if(str.charAt(0) == '.') continue;
				
				if(isPointer) {
					Set<String> addTo = firstInPointer ? (isFirstParam ? method.varsSet : method.varsUsed) : method.constantsUsed; 
					addTo.add(str);
					firstInPointer = false;
				} else {
					if(likelyConstant) {
						System.out.println(str + " is likely a constant but not making assumptions");
					}
					method.thingsReferenced.add(str);
				}
			}
		}
	}
	
	private static boolean parseCall(Method method, Tokenizer tokens) {
		String called = tokens.nextAsString();
		boolean hadCondition = tokens.next() != null; 
		if(hadCondition) {
			// if this happens, it means we had a condition
			called = tokens.nextAsString();
		}
		if(called != null && !Keywords.isRegister(called) && called.charAt(0) != '.') {
			method.methodsCalled.add(called);
		}
		
		return hadCondition;
	}
	
	public static boolean parseInstruction(Method method, Instruction instr, Tokenizer tokens) {
		// instruction and parameter dependent but it doesnt matter currently
		Memory.advanceBytes(1);
		
		switch(instr.type) {
			case LOAD:
				handleLoad(method, tokens, instr.name.equals("ldh"));
				return false;
			case JUMP:
				return handleJumpOrReturn(method, instr, tokens);
			case CALL:
				parseCall(method, tokens);
				return false;
				//$CASES-OMITTED$
			default:
				return false;
		}
	}
}