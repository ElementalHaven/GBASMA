package com.ehgames.gbasma.types;

import java.util.ArrayList;
import java.util.List;

import com.ehgames.gbasma.Memory;

public class Label {
	public String		name;
	/** The local path of the file containing this method */
	public String		file;
	/** The line number where this label was declared */
	public int			lineNo;
	/** the virtual address this label refers to */
	public int			address;
	/** A list of methods that load this address onto a register pair */
	public List<String>	addressUsedBy	= new ArrayList<>();
	
	protected Label() {}
	
	public Label(String name, String filename, int lineNo) {
		init(name, filename, lineNo);
	}
	
	public Label(Label label) {
		init(label);
	}
	
	public void init(Label label) {
		name = label.name;
		file = label.file;
		lineNo = label.lineNo;
		address = label.address;
	}

	public void init(String name, String filename, int lineNo) {
		this.name = name;
		this.file = filename;
		this.lineNo = lineNo + 1;
		address = Memory.getCurrentAddress();
	}

	public boolean isLocal() {
		return name.charAt(0) == '.';
	}
	
	public String toString() {
		return name + '@' + file + ':' + lineNo;
	}
}