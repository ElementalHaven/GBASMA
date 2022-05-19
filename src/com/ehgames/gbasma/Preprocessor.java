package com.ehgames.gbasma;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ehgames.gbasma.types.Constants;

public class Preprocessor {
	/** variables use during execution of macros */
	private static final Map<String, Object>	VARS	= new HashMap<>();
	
	public static Object getVar(String name) {
		return VARS.get(name);
	}
	
	public static void setVar(String name, Object obj) {
		VARS.put(name, obj);
	}
	private static int asInt(boolean bool) {
		return bool ? 1 : 0;
	}
	
	public static int handleSymbol(int left, String symbol, int right) {
		switch(symbol) {
			case "&":
				return left & right;
			case "|":
				return left | right;
			case "%":
				return left % right;
			case "+":
				return left + right;
			case "-":
				return left - right;
			case "^":
				return left ^ right;
			case ">>":
				return left >>> right;
			case "<<":
				return left << right;
			case "*":
				return left * right;
			case "/":
				return left / right;
			case "==":
				return asInt(left == right);
			case "!=":
				return asInt(left != right);
			case ">=":
				return asInt(left >= right);
			case "<=":
				return asInt(left <= right);
			case ">":
				return asInt(left > right);
			case "<":
				return asInt(left < right);
		}
		
		System.err.println("Unhandled math symbol: " + symbol);
		return 0;
	}
	
	public static int getPrecedence(String symbol) {
		// this uses java precedence which may not exactly be correct
		switch(symbol) {
			case "!":
				return 11;
			case "*":
			case "/":
			case "%":
				return 10;
			case "+":
			case "-":
				return 9;
			case "<<":
			case ">>":
				return 8;
			case "<":
			case ">":
			case "<=":
			case ">=":
				return 7;
			case "==":
			case "!=":
				return 6;
			case "&":
				return 5;
			case "^":
				return 4;
			case "|":
				return 3;
			case "&&":
				return 2;
			case "||":
				return 1;
			case "=":
				return 0;
		}
		System.out.println("Unhandled math symbol: " + symbol);
		return -1;
	}
	
	private static void fixDivisionBullshit(Tokenizer tokens) {
		String source = tokens.getSource();
		while(true) {
			int start = source.indexOf("DIV("); 
			if(start == -1) return;

			// ITS SUPER HACK TIME
			int offset = tokens.getTokensParsed();
			
			int end = source.indexOf(')', start);
			String divArgs = source.substring(start + 4, end);
			divArgs = divArgs.replace(",", " / ");
			tokens.init(divArgs);
			int result = doMath(tokens);
			
			source = source.substring(0, start) + result + source.substring(end + 1);
			tokens.init(source);
			tokens.skip(offset);
		}
	}
	
	public static int doMath(Tokenizer tokens) {
		fixDivisionBullshit(tokens);
		
		List<Object> args = new ArrayList<>();
		while(true) {
			Object arg = collectSingleArg(tokens);
			if(arg == null) break;
			args.add(arg);
		}
		
		int argc = args.size();
		if(argc == 1) {
			return tokenAsInt(args.get(0), 0);
		}
		if(argc % 2 != 1) {
			System.err.println("Attempt to do math with an even number of tokens. Something will likely break");
		}
		
		while(argc > 1) {
			int highest = -1;
			int highestIdx = -1;
			for(int i = 1; i < argc; i += 2) {
				String symbol = args.get(i).toString();
				int prec = getPrecedence(symbol);
				if(prec > highest) {
					highest = prec;
					highestIdx = i - 1;
				}
			}
			
			int left = tokenAsInt(args.get(highestIdx), 0);
			int right = tokenAsInt(args.get(highestIdx + 2), 0);
			String symbol = args.get(highestIdx + 1).toString();
			int result = handleSymbol(left, symbol, right);
			args.set(highestIdx, result);
			args.remove(highestIdx + 1);
			args.remove(highestIdx + 1);
			argc -= 2;
		}
		
		return (Integer) args.get(0);
	}
	
	/**
	 * Collects 1 entire top-level token.
	 * For simple tokens, this is the same as calling Tokenizer.next()<br>
	 * If it is a complex statement enclosed in parenthesis however, the entire statement is returned as a string
	 */
	public static Object collectSingleArg(Tokenizer tokens) {
		int start = tokens.getOffsetInChars();
		Object token = tokens.next();
		if(token == null) return token;
		
		Class<?> cls = token.getClass();
		if(cls == Integer.class) return token;
		if(cls == Character.class) {
			int open = 1;
			do {
				token = tokens.next();
				if(token == null) break;
				cls = token.getClass();
				if(cls == Character.class) {
					Character c = (Character) token;
					if(c == '(' || c == '[') open++;
					else open--;
				}
			} while(open > 0);
			int end = tokens.getOffsetInChars();
			return tokens.getSource().substring(start, end);
		} else {
			// I guess? it would be a string at this point
			return token;
		}
	}
	
	private static Integer tryAsInt(Object obj) {
		if(obj == null) return 0;
		if(obj.getClass() == Integer.class) return (Integer) obj;
		return null;
	}
	
	public static Integer tokenAsInt(Object obj) {
		return tokenAsInt(obj, 0);
	}
	
	public static Integer tokenAsInt(Object obj, Integer nVal) {
		Integer ret = tryAsInt(obj);
		if(ret == null) {
			String name = obj.toString();
			Object val = Constants.get(name);
			if(val == null) val = Preprocessor.getVar(name);
			ret = tryAsInt(val);
			if(ret == null) {
				System.err.println("Constant/Preprocessor variable is not directly convertible to int: " + name + " = " + val);
				ret = nVal;
			}
		}

		return ret;
	}
	
	public static boolean isTruthy(Object obj) {
		if(obj.getClass() == Integer.class) return (Integer) obj != 0;

		String name = obj.toString();
		Object val = Constants.get(name);
		if(val == null) Preprocessor.getVar(name);
		if(val == null) return false;
		
		if(val.getClass() == Integer.class) return (Integer) val != 0;
		
		System.out.println("Constant/Preprocessor variable is not directly convertible to int: " + name + '=' + val);
		return true;
	}
}