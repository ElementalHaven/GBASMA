package com.ehgames.gbasma;

import java.util.List;

import com.ehgames.gbasma.types.Macro;

public class MacroParser {
	public static int parse(Macro macro, List<String> lines, int lineNo) {
		boolean finished = false;
		final int lineCount = lines.size();
		for(; lineNo < lineCount && !finished; lineNo++) {
			final String line = lines.get(lineNo);
			if(line.trim().equals("ENDM")) {
				finished = true;
			} else {
				macro.lines.add(line);
			}
		}
		return lineNo;
	}
}