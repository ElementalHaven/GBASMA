/** A relative path to the folder containing metadata files for the specific disassembled rom */
const basePath = "gsc/";
const appName = "Game Boy ASM Analyzer";
/** A list of z80 register names for the purpose of checking if a token is a register during syntax highlighting */
const registerNames = [ 'a', 'b', 'c', 'd', 'e', 'f', 'h', 'l', "af", "bc", "de", "hl", "hli", "hld", "sp", "pc" ];
/** A list of z80 condition names for the purpose of checking if a token is a condition during syntax highlighting */
const conditionNames = [ "c", "nc", "z", "nz" ];
/** A list of z80 instruction names for the purpose of checking if a token is an instruction during syntax highlighting */
const instrNames = [
	"ret", "jp", "jr",
	"call", "rst", // "farcall",
	"ld", "ldh",
	"bit", "set", "res",
	"inc", "dec",
	"add", "adc", "sub", "sbc", "cp",
	"and", "or", "xor",
	"srl", "rr", "rrc", "rrca", "sla", "sra",
	"push", "pop", "scf", "nop", "swap"
];
/** A list of z80 math symbols for the purpose of checking if a token is a math symbol during syntax highlighting */
const compilerMath = [
	'+', '-', '*', '/', '<', '>', '<<', '>>', '==', '=', '>=', '<=', '|', '&'
];
const actionMap = {
	"method": viewMethod,
	"variable": viewVariable,
	"mtdhier": viewMethodHierarchy,
	"varhier": viewVariableHierarchy
}

var gbasma_methods = {};
var gbasma_variables = {};
var gbasma_history = [];

/**
 * Not JQuery.
 * This is simply shorthand for document.querySelector
 * @param {string} selector 
 * @returns {HTMLElement}
 */
function $(selector) {
	return document.querySelector(selector);
}

function noe(array) {
	return array === null || array.length == 0;
}

/**
 * Creates a new tag with the given tag name and
 * appends it to the optional provided parent element
 * @param {string} tagName The name of the tag to create
 * @param {HTMLElement} [parent] An optional parent to append the new tag to
 * @returns {HTMLElement} The newly created tag
 */ 
function nt(tagName, parent) {
	let tag = document.createElement(tagName);
	if(parent) parent.append(tag);
	return tag;
}

/**
 * Creates a new tag with the given tag name,
 * sets its singular class name to className,
 * and adds it to the optionally provided parent element
 * @param {string} tagName The name of the tag to create
 * @param {HTMLElement} [parent] An optional parent to append the new tag to
 * @param {string} className The singular class name to set for the tag.
 * Because of how this is implemented, it should not contain multiple class names in any form.
 * @returns {HTMLElement} The newly created tag
 */
function ntc(tagName, className, parent) {
	let tag = nt(tagName, parent);
	if(className) tag.classList.add(className);
	return tag;
}

/**
 * Event listener so we don't pollute the browser with hundreds of capturing lambdas
 *	@param {MouseEvent} e The click event that triggered this listener
 */
function clickViewMethod(e) {
	if(e.button == 0 && !e.ctrlKey) {
		let mtd = e.target.innerText;
		if(mtd.endsWith("*") || mtd.endsWith(":")) {
			mtd = mtd.substr(0, mtd.length - 1);
		}
		viewMethod(mtd);
		e.preventDefault();
		return false;
	}
}

/**
 * Event listener so we don't pollute the browser with hundreds of capturing lambdas
 *	@param {MouseEvent} e The click event that triggered this listener
 */
function clickViewVariable(e) {
	if(e.button == 0 && !e.ctrlKey) {
		viewVariable(e.target.innerText);
		e.preventDefault();
		return false;
	}
}

function applySearch() {
	let list = $("#method-list");
	let filterText = $(".navbar input").value.toLowerCase();
	if(filterText) {
		let count = 0;
		/**
		 * @param {HTMLElement} t 
		 */
		let mtd = t => {
			if(t.innerText.toLowerCase().includes(filterText)) {
				t.classList.remove("filtered");
				count++;
			} else {
				t.classList.add("filtered");
			}
		}
		list.children.forEach(mtd);
		list.previousElementSibling.innerText = `Methods(${count}/${list.childElementCount})`;
		count = 0;
		list = $("#var-list");
		list.children.forEach(mtd);
		list.previousElementSibling.innerText = `Variables(${count}/${list.childElementCount})`;
	} else {
		document.querySelectorAll(".navbar .filtered").forEach(li => li.classList.remove("filtered"));
		list.previousElementSibling.innerText = `Methods(${list.childElementCount})`;
		list = $("#var-list");
		list.previousElementSibling.innerText = `Variables(${list.childElementCount})`;
	}
}

function gbasma_init() {
	if(!HTMLCollection.prototype.forEach) HTMLCollection.prototype.forEach = Array.prototype.forEach;

	$(".navbar input").addEventListener("keyup", () => applySearch());
	$("#theme-btn").addEventListener("click", () => {
		let cl = $("body").classList;
		cl.toggle("theme-light");
		cl.toggle("theme-dark");
	});
	$("#toggle-line-nos").addEventListener("click", () => $("body").classList.toggle("line-numbers"));
	$("#view-hier").addEventListener("click", (e) => {
		if(e.target.hasAttribute("disabled")) return;
		let info = $(".info-pane h1").innerText.split(" ");
		if(info[0] == "Method") viewMethodHierarchy(info[1]);
		if(info[0] == "Variable") viewVariableHierarchy(info[1]);
	})
	window.addEventListener("popstate", navigateByUrl);
	loadList("methods.txt", $("#method-list"), clickViewMethod, "method");
	loadList("variables.txt", $("#var-list"), clickViewVariable, "variable");
	navigateByUrl();
}

function clearContent() {
	let existing = $("#main-content").querySelector(".info-pane");
	if(existing) existing.remove(); // would use existing?.remove() but thats an extremely new feature
}

function addDefaultContent() {
	document.title = appName;
	// consider having a welcome/how-to page here
}

function navigateByUrl() {
	const src = document.location.search;
	if(src) {
		let params = new URLSearchParams(src);
		for(const [action, val] of params) {
			let mtd = actionMap[action];
			if(mtd) {
				mtd(val, true);
				return;
			}
		}
	}
	clearContent();
	addDefaultContent();
}

function loadList(txtName, list, listener, type) {
	fetch(basePath + txtName).then(res => res.text()).then(text => {
		let lines = text.split("\n");
		for(let i in lines) {
			lines[i] = lines[i].replace("\r", "");
		}
		lines.sort();
		lines.forEach((varName) => {
			if(!varName) return;
			let li = nt("li", list);
			mklink(type, varName, li, listener);
		});
		applySearch();
	});
}

/**
 * Creates a new token for syntax highlighting and adds it to the given line
 * @param {string} type Type type of the token
 * @param {string} text The text for this token
 * @param {HTMLElement} line The line to add this token to
 * @returns {HTMLElement} The created element. Use this to provide further information
 */
function st(type, text, line) {
	let tt = "span";
	const isVar = type === "variable";
	if(isVar || type === "method") {
		tt = "a";
	}
	let token = ntc("span", type, line);
	token.innerText = text;
	return token;
}

/**
 * 
 * @param {'method' | 'variable'} type Whether this link is a method or a variable
 * @param {string} name The method or variable name 
 * @param {HTMLElement} parent the parent to add the link to
 */
function mklink(type, name, parent, listener) {
	let a = ntc("a", type, parent);
	a.innerText = name;
	let idx = name.indexOf(':');
	if(idx != -1) name = name.substr(0, idx);
	if(name.endsWith('*')) name = name.substr(0, name.length - 1); // fix for hierarchy pages
	a.href = `?${type}=${name}`;
	a.addEventListener('click', listener);
	return a;
}

/**
 * Appends text to the end of the parent elements children,
 * either by appending to an existing text node,
 * or by creating a new one if none exists
 * @param {String} text The text to append
 * @param {HTMLElement} parent The parent element that contains the text
 */
function at(text, parent) {
	let last = parent.lastChild;
	if(!last || last.ndoeName != "#text") {
		parent.append(document.createTextNode(text));
	} else {
		last.innerText += text;
	}
}

function getTokenStart(line, start) {
	let i;
	for(i = start; i < line.length; i++) {
		let c = line[i];
		if(c != ' ' && c != '\t') break;
	}
	return i;
}

function getStringTokenEnd(line, start) {
	while(true) {
		let idx = line.indexOf('"', start + 1);
		if(idx == -1) return line.length;
		if(line[idx - 1] != '\\') return idx + 1;
		start = idx;
	}
}

function getGenericTokenEnd(line, start, endDelimiter) {
	let i;
	for(i = start; i < line.length; i++) {
		let c = line[i];
		if(c == ' ' || c == '\t' || c == ',' || c == endDelimiter) break;
	}
	return i;
}

/**
 * @param {*} methodData 
 * @param {HTMLElement} lineTag 
 * @param {String} lineStr 
 * @param {String} [endDelimiter] 
 */
function syntaxHighlightLine(data, lineTag, lineStr, endDelimiter) {
	let parseStart = 0;
	let hasInstr = endDelimiter !== undefined;
	let hadCond = true;
	while(parseStart < lineStr.length) {
		let tokenStart = getTokenStart(lineStr, parseStart);
		if(parseStart != tokenStart) {
			at(lineStr.substring(parseStart, tokenStart), lineTag);
		}

		const firstChar = lineStr[tokenStart];
		if(firstChar == endDelimiter) {
			return tokenStart;
		} else if(firstChar == '"') {
			parseStart = getStringTokenEnd(lineStr, tokenStart);
			st("string", lineStr.substring(tokenStart, parseStart), lineTag);
		} else if(firstChar == '(') {
			tokenStart++;
			at('(', lineTag);
			parseStart = tokenStart + syntaxHighlightLine(data, lineTag, lineStr.substring(tokenStart), ')') + 1;
			if(parseStart <= lineStr.length) at(')', lineTag);
		} else if(firstChar == '[') {
			tokenStart++;
			at('[', lineTag);
			parseStart = tokenStart + syntaxHighlightLine(data, lineTag, lineStr.substring(tokenStart), ']') + 1;
			if(parseStart <= lineStr.length) at(']', lineTag);
		} else if(firstChar == ';') {
			st("comment", lineStr.substr(tokenStart), lineTag);
			parseStart = lineStr.length;
		} else if(firstChar == ',') {
			// ignoring for now
			at(',', lineTag);
			parseStart = tokenStart + 1;
		} else {
			parseStart = getGenericTokenEnd(lineStr, tokenStart, endDelimiter);
			const token = lineStr.substring(tokenStart, parseStart);
			if(firstChar == '.') {
				st("label", token, lineTag);
			} else if(token.endsWith(':')) {
				mklink("method", token, lineTag, clickViewMethod);
			} else if(firstChar == '$') {
				st("numeral", token, lineTag).classList.add("hex");
			} else if(firstChar == '%' && token.length > 1) {
				st("numeral", token, lineTag).classList.add("bin");
			} else if((firstChar >= '0' && firstChar <= '9') || token == "-1") {
				st("numeral", token, lineTag).classList.add("dec");
			} else if(compilerMath.includes(token)) {
				at(token, lineTag);
			} else {
				if(!hasInstr) {
					let instrIdx = instrNames.indexOf(token);
					if(instrIdx == -1) {
						st("macro", token, lineTag);
						// TODO consider having a description or a link to the macro
					} else {
						st("instruction", token, lineTag);
						// TODO add tooltip for instruction summary
						hadCond = instrIdx > 3;
					}
					hasInstr = true;
				} else {
					if(!hadCond && conditionNames.includes(token)) {
						st("condition", token, lineTag);
					} else if(registerNames.includes(token)) {
						st("register", token, lineTag);
					} else if(data.constantsUsed.includes(token)) {
						st("enum", token, lineTag);
					} else if(data.methodsCalled.includes(token) || data.methodAddressesUsed.includes(token)) {
						mklink("method", token, lineTag, clickViewMethod);
					} else if(data.varsUsed.includes(token) || data.varsSet.includes(token) || data.varAddressesUsed.includes(token)) {
						mklink("variable", token, lineTag, clickViewVariable);
					} else {
						st("error", token, lineTag);
					}
					hadCond = true;
				}
			}
		}
	}
	if(endDelimiter) return parseStart;
}

function createSyntaxHighlightedCode(methodData) {
	let code = ntc("div", "code");

	let lineNo = methodData.lineNo;
	methodData.lines.forEach(line => {
		let lt = ntc("div", "line", code);

		let lnwrap = ntc("span", "line-no", lt);
		nt("span", lnwrap).innerText = lineNo;

		let bp = st("breakpoint", "", lt);
		// TODO enable if its in the breakpoint list
		bp.addEventListener("click", ev => {
			let active = bp.classList.toggle("on");
			if(active) {
				// TODO add breakpoint to list
			} else {
				// TODO remove breakpoint from list
			}
		});

		syntaxHighlightLine(methodData, lt, line);

		// I'm still not 100% certain where I want to allow breakpoints
		if(!lt.querySelector(".instruction, .macro")) bp.remove();

		lineNo++;
	});

	return code;
}

function addList(header, list, type, pane) {
	if(!list || list.length == 0) return;
	nt("h2", pane).innerText = header;

	let listener = null;
	if(type === "method") listener = clickViewMethod;
	if(type === "variable") listener = clickViewVariable;

	let didFirst = false;
	for(let item of list) {
		if(didFirst) at(", ", pane);
		if(listener) mklink(type, item, pane, listener);
		else st(type, item, pane);
		didFirst = true;
	}
}

/**
 * 
 * @param {String} type 
 * @param {String} valName 
 * @returns {Promise}
 */
function getX(type, valName) {
	const path = `${basePath}${type}s/${valName}.json`;

	let group = window["gbasma_" + type + "s"];
	let preload = group[valName];
	return preload ? Promise.resolve(preload) : fetch(path).catch(err => console.log("error when fetching " + path + ": " + err)).then(res => res.json()).then(obj => {
		if(obj.names) {
			for(const name of obj.names) {
				group[name] = obj;
			}
		} else {
			group[obj.name] = obj;
		}
		return obj;
	});
}

/**
 * @param {string} type 
 * @param {string} valName 
 * @param {string} abbrev 
 * @param {Boolean} noPush 
 */
function viewX(type, valName, abbrev, loader, noPush) {
	let pane = ntc("div", "info-pane");
	nt("h1", pane).innerText = type[0].toUpperCase() + type.substring(1) + ' ' + valName;

	getX(type, valName).then(obj => loader(obj, pane), err => {
		pane.classList.add("error");
		nt("span", pane).innerText = `No data for this ${type}. Please make sure the ${type} exists and re-run the metadata generation tool.`;
	}).finally(() => {
		clearContent();
		$("#main-content").append(pane);
		$("#view-hier").removeAttribute("disabled");
		if(!noPush) window.history.pushState(null, "", `?${type}=${valName}`);
		document.title = `[${abbrev}] ${valName} - ${appName}`;
	});
}

function viewMethod(methodName, noPush) {
	viewX("method", methodName, 'm', (obj, pane) => {
		addList("Location", [ obj.file + ", line " + obj.lineNo ], null, pane);
		let aliases = obj.names.slice();
		aliases.splice(aliases.indexOf(methodName), 1);
		addList("Aliases", aliases, "method", pane);
		addList("Calling Methods", obj.callingMethods, "method", pane);
		addList("Referencing Methods", obj.addressUsedBy, "method", pane);
		addList("Methods Called", obj.methodsCalled, "method", pane);
		addList("Methods Referenced", obj.methodAddressesUsed, "method", pane);
		if(obj.fallsInto) addList("Fallthrough Into", [ obj.fallsInto ], "method", pane);
		addList("Variables Set", obj.varsSet, "variable", pane);
		addList("Variables Used", obj.varsUsed, "variable", pane);
		addList("Variables Referenced", obj.varAddressesUsed, "variable", pane);
		addList("Constants Used", obj.constantsUsed, "constant", pane);
		nt("h2", pane).innerText = "Code";
		pane.append(createSyntaxHighlightedCode(obj));
	}, noPush);
}

function viewVariable(varName, noPush) {
	viewX("variable", varName, 'v', (obj, pane) => {
		// name isn't useful atm
		addList("Type", [ obj.type ], null, pane);
		addList("Valid Values", obj.validValues, "constant", pane);
		addList("Setting Methods", obj.settingMethods, "method", pane);
		addList("Using Methods", obj.usingMethods, "method", pane);
		addList("Referencing Methods", obj.addressUsedBy, "method", pane);
	}, noPush);
}

/**
 * 
 * @param {HierNode} mtd 
 * @param {HierNode} info 
 * @param {string} use 
 */
function setUseOrAddChild(mtd, info, use) {
	/** @type {ChildInfo} */
	let child = null;
	for(let c of mtd.children) {
		if(c.name == info.name) {
			child = c;
			break;
		}
	}
	if(child === null) {
		child = new ChildInfo(info.name);
		mtd.children.push(child);
	}
	if(!child.uses.includes(use)) {
		child.uses.push(use);
	}
}

class ChildInfo {
	/**
	 * @param {string} name 
	 */
	constructor(name) {
		this.name = name;
		/**
		 * @type {string[]} An array of uses. Possible values include "call", "ref", "use", and "set"
		 */
		this.uses = [];
	}
}

class HierNode {
	/**
	 * @param {string} name 
	 * @param {string} type 
	 */
	constructor(name, type) {
		this.name = name;
		this.type = type;
		this.inTree = false;
		/**
		 * @type {ChildInfo[]}
		 */
		this.children = [];
	}
}

/**
 * 
 * @param {string[]} names A list of method names that are the parent of this variable or method in some way
 * @param {Object<string, HierNode>} byName The map of all methods in the hierarchy
 * @param {string[]} toFind A list of methods that have yet to be resolved
 * @param {HierNode} info The method or variable that should be considered a child of the methods in the list
 * @param {string} use How the method names listed in names use this method or variable
 */
function addParents(names, byName, toFind, info, use) {
	if(noe(names)) return;
	for(let mtdName of names) {
		let mtd = byName[mtdName];
		if(!mtd) {
			mtd = new HierNode(mtdName, "method");
			byName[mtdName] = mtd;
			toFind.push(mtdName);
		}
		setUseOrAddChild(mtd, info, use);
	}
}

/**
 * 
 * @param {Object<string, HierNode>} byName 
 * @param {string[]} topLevel 
 * @param {string[]} toFind 
 * @param {HierNode} info 
 */
async function resolveHierarchy(byName, topLevel, toFind, info) {
	return getX(info.type, info.name).then(obj => {
		addParents(obj.addressUsedBy, byName, toFind, info, "ref");
		if(info.type == "variable") {
			addParents(obj.settingMethods, byName, toFind, info, "set");
			addParents(obj.usingMethods, byName, toFind, info, "used");
		} else {
			addParents(obj.callingMethods, byName, toFind, info, "call");
			if(noe(obj.callingMethods) && noe(obj.addressUsedBy)) {
				topLevel.push(info.name);
			}
		}
	});
}

/**
 * 
 * @param {HTMLElement} parent 
 * @param {HierNode} self 
 * @param {Object<string, HierNode>} byName 
 */
function buildTree(parent, self, byName) {
	let li = nt("li", parent);
	let span = mklink(self.type, self.name, li, self.type == "method" ? clickViewMethod : clickViewVariable);
	if(self.children.length > 0) {
		let ul = nt("ul", li);
		if(self.inTree) {
			let cont = nt("a", nt("li", ul));
			cont.innerText = "...";
			cont.href = "#mtd-" + self.name;
		} else {
			self.inTree = true;
			span.id = "mtd-" + self.name;
			for(let child of self.children) {
				let c = buildTree(ul, byName[child.name], byName);
				c.classList.add(...child.uses);
			}
		}
	}
	return span;
}

/**
 * 
 * @param {string} useType 
 * @param {string} type 
 * @param {string} name 
 */
async function viewHierarchy(useType, type, name) {
	let pane = ntc("div", "info-pane");
	nt("h1", pane).innerText = useType + " Hierarchy of " + type + ' ' + name;
	type = type.toLowerCase();

	let legend = ntc("div", "legend", pane);
	legend.innerHTML = "<div style='background: #0C0'></div> Used <div style='background: #C00'></div> Set <div style='background: #36F'></div> Referenced";

	let hier = nt("ul", ntc("div", "hier", pane));

	let toFind = [ name ];
	let topLevel = [];
	/**
	 * @type {Object<string, HierNode>}
	 */
	let byName = {};
	byName[name] = new HierNode(name, type);
	let count = 0;
	while(toFind.length > 0) {
		count++;
		await resolveHierarchy(byName, topLevel, toFind, byName[toFind.pop()]);
	}
	byName[name].name += '*'; // make the thing we're viewing a hierarchy of easier to find

	console.log("building tree with " + topLevel.length + " top level elements after resolving " + count + " items");
	for(let top of topLevel) {
		buildTree(hier, byName[top], byName);
	}
	// Firefox will hold on to this memory if we dont delete parts
	// With a limited scope, that still ends up at 3gb+ for wTempMonSpecies
	delete byName;

	clearContent();
	$("#main-content").append(pane);
	$("#view-hier").setAttribute("disabled", true);
}

function viewVariableHierarchy(varName, noPush) {
	viewHierarchy("Use", "Variable", varName);
	if(!noPush) window.history.pushState(null, "", `?varhier=${varName}`);
	document.title = `[hv] ${varName} - ${appName}`;
}

function viewMethodHierarchy(mtdName, noPush) {
	viewHierarchy("Call", "Method", mtdName);
	if(!noPush) window.history.pushState(null, "", `?mtdhier=${mtdName}`);
	document.title = `[hm] ${mtdName} - ${appName}`;
}