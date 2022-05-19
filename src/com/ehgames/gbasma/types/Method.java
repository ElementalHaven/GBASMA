package com.ehgames.gbasma.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Method extends Label {
	public static final Map<String, Method> ALL_METHODS = new HashMap<>();

	public static void createAllLinks() {
		for(Method m : ALL_METHODS.values()) {
			m.resolveUnknowns();
			m.markVariables();
			m.markMethodCalls();
		}
	}

	public Set<String>						names				= new LinkedHashSet<>();

	public Set<String>						callingMethods		= new TreeSet<>();
	public Set<String>						methodsCalled		= new TreeSet<>();
	public Set<String>						methodAddressesUsed	= new TreeSet<>();

	public String							fallsInto;

	public Set<String>						varsSet				= new TreeSet<>();
	public Set<String>						varsUsed			= new TreeSet<>();
	public Set<String>						varAddressesUsed	= new TreeSet<>();

	public Set<String>						constantsUsed		= new TreeSet<>();

	/** the method's code itself, split into lines. this includes a possible initial comments, aliases, and fallthrough */
	public List<String>						lines				= new ArrayList<>();

	/** variables, methods, or constants that are just loaded onto a register in a way I can't tell what it is */
	public transient Set<String>			thingsReferenced	= new TreeSet<>();

	public void copyLines(List<String> fileLines, int lastLine) {
		if(lines != null) throw new IllegalArgumentException("Method already has lines!");
		// the stored lineNo is 1-based
		// lastLine is included
		lines = fileLines.subList(lineNo - 1, lastLine + 1);
	}
	
	public boolean setUsed(String name) {
		Method mref = ALL_METHODS.get(name);
		if(mref != null) {
			methodAddressesUsed.add(name);
			mref.addressUsedBy.addAll(names);
		} else {
			Variable vref = Variable.ALL_VARIABLES.get(name);
			if(vref != null) {
				varAddressesUsed.add(name);
				vref.addressUsedBy.addAll(names);
			} else {
				Object cref = Constants.get(name);
				if(cref != null) {
					constantsUsed.add(name);
				} else {
					return false;
				}
			}
		}
		return true;
	}

	private void resolveUnknowns() {
		Iterator<String> it = thingsReferenced.iterator();
		while(it.hasNext()) {
			String thing = it.next();
			
			boolean resolved = setUsed(thing);
			if(resolved) {
				it.remove();
			} else {
				System.err.println("Could not resolve \"" + thing + "\" to any known method, variable, or constant.");
			}
		}
	}

	private void markVariables() {
		for(String varSet : varsSet) {
			Variable var = Variable.ALL_VARIABLES.get(varSet);
			if(var != null) {
				var.settingMethods.addAll(names);
			}
		}
		for(String varUsed : varsUsed) {
			Variable var = Variable.ALL_VARIABLES.get(varUsed);
			if(var != null) {
				var.usingMethods.addAll(names);
			}
		}
	}
	
	private void markMethodCalls() {
		for(String calledText : methodsCalled) {
			Method called = ALL_METHODS.get(calledText);
			if(called != null) {
				called.callingMethods.addAll(names);
			}
		}
		if(fallsInto != null) {
			Method called = ALL_METHODS.get(fallsInto);
			if(called != null) {
				called.callingMethods.addAll(names);
			}
		}
	}
	
	public void removeTrailingEmptyLines() {
		for(int i = lines.size() - 1; i >= 0; i--) {
			String line = lines.get(i);
			// XXX there may be the need to remove trailing comments too, but we'll figure that out later
			if(!line.isEmpty()) return;
			lines.remove(i);
		}
	}
}