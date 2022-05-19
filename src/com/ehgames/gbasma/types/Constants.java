package com.ehgames.gbasma.types;

import java.util.HashMap;
import java.util.Map;

public class Constants {
	private static final Map<String, Object>	ALL_CONSTANTS	= new HashMap<>();

	public static Object get(String name) {
		return ALL_CONSTANTS.get(name);
	}
	
	public static void set(String name, Object value) {
		ALL_CONSTANTS.put(name, value);
	}
}