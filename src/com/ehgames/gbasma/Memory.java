package com.ehgames.gbasma;

import com.ehgames.gbasma.types.Variable;

// FIXME needs reworked to handle banks
public class Memory {
	private static int		currentAddress;
	private static boolean	readingUnion;
	private static int		unionStart;
	private static int		unionEnd;
	private static boolean	exceeded16bit;

	public static void startUnion() {
		if(readingUnion) {
			System.err.println("Attempted to declare a new union when a union is already being declared");
			return;
		}
		
		unionStart = currentAddress;
		unionEnd = currentAddress;
	}

	public static void nextUnionBlock() {
		if(!readingUnion) {
			System.err.println("Attempted to start next struct in union without a union being declared");
			return;
		}
		
		currentAddress = unionStart;
	}
	
	public static void endUnion() {
		if(!readingUnion) {
			System.err.println("Attempted to end a union without a union being declared");
			return;
		}
		
		currentAddress = unionEnd;
	}
	
	public static void advanceBytes(int bytes) {
		currentAddress += bytes;
		if(readingUnion && unionEnd < currentAddress) {
			unionEnd = currentAddress;
		}
		if(!exceeded16bit && currentAddress > 0xFFFF) {
			exceeded16bit = true;
			System.err.println("16-BIT ADDRESS SPACE HAS BEEN EXCEEDED. MEMORY NEEDS SUBDIVIDED INTO BANKS/SECTIONS.");
		}
	}
	
	public static int getCurrentAddress() {
		return currentAddress;
	}
	
	public static int getSize(String token, Tokenizer tokens) {
		return getSize(token, tokens, null);
	}
	
	public static int getSize(String token, Tokenizer tokens, Variable var) {
		switch(token) {
			case "db":
				String text = tokens.nextAsString();
				if(text != null) {
					if(text.charAt(0) == '"') {
						if(var != null) var.setType("String");
						return text.length() - 1; // -2 for quotes. +1 for null termination
					}
				}
				if(var != null) var.setType("byte");
				return 1;
			case "dw":
				if(var != null) var.setType("short");
				return 2;
			case "ds":
				Object obj = tokens.next();
				if(obj != null && obj.getClass() == Integer.class) {
					int size = (Integer) obj;
					if(var != null) var.setType("byte[" + size + ']');
					return size;
				}
				System.err.println("Array type does not have a size");
				return 0;
			default:
				System.err.println("Invalid data size token: " + token);
				return 0;
		}
	}
}