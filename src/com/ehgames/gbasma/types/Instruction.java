package com.ehgames.gbasma.types;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Instruction {
	public enum Type {
		JUMP, CALL, LOAD, MATH, UTIL, BIT, SHIFT;
	}

	private static final Map<String, Instruction> ALL = new HashMap<>();

	static {
		File f = new File("instructions.ini");
		try(Scanner scanner = new Scanner(f)) {
			while(scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();
				if(line.isEmpty() || line.charAt(0) == '#') continue;
				String[] parts = line.split("\\s+");

				String name = parts[0].toLowerCase();
				Type type = Type.valueOf(parts[1]);
				boolean condition = Boolean.valueOf(parts[2]);
				int count = Integer.valueOf(parts[3]);
				boolean optional = Boolean.valueOf(parts[4]);
				new Instruction(name, type, condition, count, optional);
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public static Instruction get(String name) {
		// might need to lowercase name
		return ALL.get(name);
	}

	public final String		name;
	public final Type		type;
	public final boolean	canHaveCondition;
	public final int		argCount;
	public final boolean	firstArgIsOptional;

	private Instruction(String name, Type type, boolean canHaveCondition, int argCount, boolean firstArgIsOptional) {
		this.name = name;
		this.type = type;
		this.canHaveCondition = canHaveCondition;
		this.argCount = argCount;
		this.firstArgIsOptional = firstArgIsOptional;
		ALL.put(name, this);
	}
}