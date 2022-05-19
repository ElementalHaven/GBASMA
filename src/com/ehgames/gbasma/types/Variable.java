package com.ehgames.gbasma.types;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Variable extends Label {
	public static final Map<String, Variable>	ALL_VARIABLES	= new HashMap<>();

	public String								type;
	public Set<String>							validValues		= new LinkedHashSet<>();
	public Set<String>							settingMethods	= new TreeSet<>();
	public Set<String>							usingMethods	= new TreeSet<>();
	
	private transient boolean					isList;

	public Variable(Label label) {
		super(label);
		ALL_VARIABLES.put(name, this);
	}
	
	public void setType(String type) {
		if(this.type == null) {
			this.type = type;
		} else {
			if(!sameBasicType(type)) {
				System.err.println("Variable is different basic type from new array/struct member: " + this.type + " vs " + type);
			} else {
				int start = this.type.lastIndexOf('[') + 1;
				if(isList) {
					int end = this.type.lastIndexOf(']');
					int count = Integer.parseInt(this.type.substring(start, end));
					count++;
					this.type = this.type.substring(0, start) + count + ']';
				} else {
					if(start > 0) {
						// the arg spots are backwards but I can't update it otherwise
						this.type = this.type.substring(0, start - 1) + "[]";
					}
					this.type += "[2]"; 
					isList = true;
				}
			}
		}
	}
	
	public boolean sameBasicType(String type) {
		String basicType = this.type;
		if(basicType == null || type == null) return basicType == type;
		
		if(isList) {
			basicType = basicType.substring(0, basicType.lastIndexOf('['));
		}
		int specifiedArrayIdx = type.indexOf('[');
		int existingArrayIdx = basicType.indexOf('[');
		if(specifiedArrayIdx != existingArrayIdx) return false;
		if(existingArrayIdx != -1) {
			basicType = basicType.substring(0, existingArrayIdx);
			type = type.substring(0, specifiedArrayIdx);
		}
		return basicType.equals(type);
	}
}