package com.ehgames.gbasma;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.ehgames.gbasma.types.Method;
import com.ehgames.gbasma.types.Variable;
import com.google.gson.Gson;

public class MetaGen {

	public static File	rootFile	= new File(".");
	public static Path	rootPath;
	public static File	dstDir		= new File(".");
	private static String	mainFile;

	private static void showHelp() {
		System.out.println("MetaGen: Metadata generator for GBASMA");
		System.out.println();
		System.out.println("Usage: MetaGen [-src SRCPATH] [-main MAINFILE] [-dst DSTPATH]");
		System.out.println("-src,-i,-in		Use the path specified by SRCPATH as the location of ASM files");
		System.out.println("-dst,-o,-out	Use the path specified by DSTPATH as the destination for");
		System.out.println("				generated files");
		System.out.println("-main			Generate metadata by using the file specified by MAINFILE");
		System.out.println("				(resolved against SRCPATH, or the current directory if none");
		System.out.println("				specified) as the main file and only generate data for it");
		System.out.println("				and files included with the INCLUDE directive");
		System.out.println("-help			Displays this help method and exits");
		System.exit(0);
	}

	private static boolean parseArgs(String[] args) {
		if(args == null) return true;
		boolean ok = true;

		for(int i = 0; i < args.length; i++) {
			String arg = args[i];
			char c = arg.charAt(0);
			if(c == '/' || c == '-') arg = arg.substring(1);
			File f;
			switch(arg.toLowerCase()) {
				case "?":
				case "help":
					showHelp();
					break;
				case "i":
				case "in":
				case "src":
					if(++i == args.length) {
						System.err.println("Missing parameter for flag " + arg);
						return false;
					}
					f = new File(args[i]);
					if(!f.isDirectory()) {
						System.err.println("Source folder is not a valid directory.");
						ok = false;
					} else {
						rootFile = f;
					}
					break;
				case "o":
				case "out":
				case "dst":
					if(++i == args.length) {
						System.err.println("Missing parameter for flag " + arg);
						return false;
					}
					f = new File(args[i]);
					if(!f.isDirectory() && (f.isFile() || !f.mkdirs())) {
						System.err.println("Destination path can not be used. It is either a file or could not be created.");
						ok = false;
					} else {
						dstDir = f;
					}
					break;
				case "main":
					if(++i == args.length) {
						System.err.println("Missing parameter for flag " + arg);
						return false;
					}
					f = new File(rootFile, args[i]);
					if(f.isFile()) {
						mainFile = args[i];
					} else {
						System.err.println("Main file is not a valid file.");
						ok = false;
					}
					break;
			}
		}
		return ok;
	}

	public static void main(String[] args) {
		if(!parseArgs(args)) {
			System.err.println("Please resolve the above errors and try again");
			System.exit(1);
		}
		rootPath = rootFile.toPath();
		if(mainFile == null) {
			mainFile = "main.asm";
			if(!new File(rootFile, mainFile).isFile()) {
				System.err.println("No main file specified and default value of \"main.asm\" could not be resolved to a valid readable file");
				System.exit(2);
			}
		}
		
		Parser parser = new Parser();
		// hardcoding in files because of stupid makefile shit thats even more elaborate than asm I'm trying to parse
		parser.parse("wram.asm"); 
		parser.parse(mainFile);
		parser.cleanup();

		Method.createAllLinks();

		writeAll("methods", Method.ALL_METHODS);
		writeAll("variables", Variable.ALL_VARIABLES);
	}

	private static void writeAll(String type, Map<String, ?> map) {
		// list file first
		try(PrintWriter out = new PrintWriter(new File(dstDir, type + ".txt"))) {
			boolean first = true;
			for(String methodName : map.keySet()) {
				if(!first) {
					out.println();
				}
				out.print(methodName);
				first = false;
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}

		File dir = new File(dstDir, type);
		dir.mkdir();

		Gson gson = new Gson();
		for(Map.Entry<String, ?> kv : map.entrySet()) {
			String json = gson.toJson(kv.getValue());
			Path path = new File(dir, kv.getKey() + ".json").toPath();
			try {
				Files.write(path, json.getBytes(StandardCharsets.UTF_8));
			} catch(IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
}