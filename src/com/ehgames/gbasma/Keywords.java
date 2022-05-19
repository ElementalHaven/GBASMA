package com.ehgames.gbasma;

import java.util.Arrays;
import java.util.List;

import com.ehgames.gbasma.types.Instruction;

public class Keywords {
	public static final List<String>	INSTRUCTIONS	= Arrays.asList(
			"push", "pop",
			// farcall is actually a macro. also, apparently both farcall and callfar exist
			"call", "ret", "rst", "jp", "jr",
			"ld", "ldh",
			"set", "res", "bit",
			"add", "sub", "sbc", "cp", "inc", "dec",
			"xor", "or", "and",
			"srl", "rr",
			"swap",
			"scf", "nop"
	);
	private static final List<String>	EQUALITY		= Arrays.asList("==", ">=", "<=", "<", ">");
	private static final List<String>	MATHS			= Arrays.asList("+", "-", "*", "/", "<<", ">>", "%", "|", "&", "=", ">=", "<=");
	public static final List<String>	DATASIZES		= Arrays.asList("db", "dw", "ds");
	public static final List<String>	CONDITIONS		= Arrays.asList("z", "nz", "c", "nc");
	public static final List<String>	REGISTERS		= Arrays.asList(
			"a", "b", "c", "d", "e", "f", "h", "l", "af", "bc", "de", "hl", "hli", "hld", "sp", "pc"
	);
	
	public static boolean isInstruction(String token) {
		// might need to lowercase the token
		//return INSTRUCTIONS.contains(token);
		return Instruction.get(token) != null;
	}
	
	public static boolean isRegister(String token) {
		// might need to lowercase the token
		return REGISTERS.contains(token);
	}
	
	public static boolean isDataSize(String token) {
		// might need to lowercase the token
		return DATASIZES.contains(token);
	}
	
	public static boolean isMathSymbol(String token) {
		return MATHS.contains(token);
	}
	
	public static boolean isEqualitySymbol(String token) {
		return EQUALITY.contains(token);
	}
	
	public static boolean isCondition(String token) {
		return CONDITIONS.contains(token);
	}
}