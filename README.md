# GBASMA
 GameBoy ASM Analyzer

 This program generates a stylized view of Z80 assembly in the form of web pages from source .asm files.
 Code is split up into pages for individual methods and variables with all references being precomputed.
 Example output of this program can be found [here](https://ElementalHaven.github.io/GBASMAExample)

## Requirements

- JDK 8 or newer to build and Java 8 or newer to run.
- Gson. Gson 2.8.6 was used but other versions will likely work just as well.

## Usage

```
java MetaGen [-src SRCPATH] [-main MAINFILE] [-dst DSTPATH]
-src,-i,-in		Use the path specified by SRCPATH as the location of ASM files
-dst,-o,-out	Use the path specified by DSTPATH as the destination for
				generated files
-main			Generate metadata by using the file specified by MAINFILE
				(resolved against SRCPATH, or the current directory if none
				specified) as the main file and only generate data for it
				and files included with the INCLUDE directive
-help			Displays this help method and exits
```

## Limitations

- Currently only one input file can be specified. This can be circumvented by either creating
an ASM file that INCLUDEs all all relevant files or by editing the code to add additional files.
Additionally, existing hardcoded files may need to be removed from the code.
- There's a number of unsupported preprocessor statements/keywords, including DEF outside of if statements.
A number of the supported things only have partial functionality as well.
- This software was based on an understanding of z80 instructions and an existing codebase of disassembly.
In reality, it should have been based on 
[the documentation for rgbasm/rgbds](https://rgbds.gbdev.io/docs/master/rgbasm.5/) instead.
