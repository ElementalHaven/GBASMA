package com.ehgames.gbasma;

import java.util.Stack;

public class Tokenizer {
	// check all references to this if you want to find out what exactly it does
	public static char[] binaryStupidity = null;
	
	private static boolean breaksGenericToken(char c) {
		return Character.isWhitespace(c) || c == ',' || c == '(' || c == ')' || c == '[' || c == ']';
	}
	
	public static boolean isQuotedString(Object obj) {
		if(obj == null) return false;
		String str = obj.toString();
		if(str != obj || str.length() < 2) return false;
		return str.charAt(0) == '"' && str.charAt(str.length() - 1) == '"';
	}
	
	public static String unquote(String str) {
		return str.substring(1, str.length() - 1);
	}
	
	public static boolean isLabel(String str) {
		return str.endsWith(":");
	}

	private Stack<Character>	delimStack	= new Stack<>();

	private char				endDelim;
	private String				text;
	private int					offset;
	private int					length;
	private int					tokensParsed;

	public void init(String text) {
		this.text = text;
		length = text.length();
		reset();
	}
	
	public void reset() {
		offset = 0;
		delimStack.clear();
		endDelim = '\0';
		tokensParsed = 0;
	}
	
	public void skip(int count) {
		for(int i = 0; i < count; i++) {
			next();
		}
	}
	
	public void rewind(int count) {
		if(count > tokensParsed || count <= 0) {
			throw new IllegalArgumentException("Count must be a positive value <= tokensParsed");
		}
		int dst = tokensParsed - count;
		reset();
		skip(dst);
	}
	
	public int getTokensParsed() {
		return tokensParsed;
	}
	
	public int getOffsetInChars() {
		return offset;
	}

	private int advanceToNextTokenChar() {
		for(; offset < length; offset++) {
			char c = text.charAt(offset);
			if(c == endDelim) break;
			if(!Character.isWhitespace(c)) break;
		}
		return offset;
	}
	
	public boolean mightHaveNext() {
		return offset < length;
	}
	
	public String nextAsString() {
		Object obj = next();
		return obj == null ? null : obj.toString();
	}

	public Object next() {
		advanceToNextTokenChar();
		if(offset == length) return null;

		tokensParsed++;
		final char c = text.charAt(offset);
		if(c == endDelim) {
			endDelim = delimStack.isEmpty() ? '\0' : delimStack.pop();
			offset++;
			return c;
		}
		if(c == ',') {
			offset++;
			//return next();
			return c; // having it return so we can tell parameters apart easier
		}
		if(c == '(') return nextDelimiter('(', ')');
		if(c == '[') return nextDelimiter('[', ']');
		if(c == '"') return nextQuotedString();
		if(c == ';') return nextComment();
		String generic = nextGeneric();
		if(generic.length() > 1) {
			int base = c == '%' ? 2 : c == '$' ? 16 : 0;
			if(base != 0) {
				String numStr = generic.substring(1);
				if(base == 2 && binaryStupidity != null) {
					numStr = numStr.replace(binaryStupidity[0], '0');
					numStr = numStr.replace(binaryStupidity[1], '1');
				}
				return Integer.parseInt(numStr, base);	
			}
		}
		try {
			return Integer.parseInt(generic);
		} catch(NumberFormatException e) {
			return generic;
		}
	}
		
	private char nextDelimiter(char start, char end) {
		if(endDelim != '\0') {
			delimStack.push(endDelim);
		}
		endDelim = end;
		offset++;
		return start;
	}
	
	private String nextQuotedString() {
		int start = offset++;
		boolean isNextEscaped = false;
		while(offset < length) {
			char c = text.charAt(offset++);
			if(isNextEscaped) {
				isNextEscaped = false;
			} else if(c == '\\') {
				isNextEscaped = true;
			} else if(c == '"') {
				break;
			}
		}
		return text.substring(start, offset);
	}
	
	private String nextComment() {
		// modified method to never return comments. they're never useful for this program
		//String comment = text.substring(offset);
		offset = length;
		//return comment;
		--tokensParsed;
		return null;
	}
	
	/*
	private int nextHex() {
		int start = ++offset;
		while(offset < length && isHexDigit(text.charAt(offset))) {
			offset++;
		}
		return Integer.parseInt(text.substring(start, offset), 16);
	}
	*/
	
	private String nextGeneric() {
		int start = offset++;
		while(offset < length && !breaksGenericToken(text.charAt(offset))) {
			offset++;
		}
		return text.substring(start, offset);
	}
	
	public String getSource() {
		return text;
	}
}