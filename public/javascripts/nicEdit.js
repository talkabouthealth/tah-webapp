/* NicEdit - Micro Inline WYSIWYG
 * Copyright 2007-2008 Brian Kirchoff
 *
 * NicEdit is distributed under the terms of the MIT license
 * For more information visit http://nicedit.com/
 * Do not remove this copyright message
 */
var bkExtend = function(){
	var args = arguments;
	if (args.length == 1) args = [this, args[0]];
	for (var prop in args[1]) args[0][prop] = args[1][prop];
	return args[0];
};
function bkClass() { }
bkClass.prototype.construct = function() {};
bkClass.extend = function(def) {
  var classDef = function() {
      if (arguments[0] !== bkClass) { return this.construct.apply(this, arguments); }
  };
  var proto = new this(bkClass);
  bkExtend(proto,def);
  classDef.prototype = proto;
  classDef.extend = this.extend;      
  return classDef;
};

var bkElement = bkClass.extend({
	construct : function(elm,d) {
		if(typeof(elm) == "string") {
			elm = (d || document).createElement(elm);
		}
		elm = $BK(elm);
		return elm;
	},
	appendTo : function(elm) {
		elm.appendChild(this);	
		return this;
	},
	appendBefore : function(elm) {
		elm.parentNode.insertBefore(this,elm);	
		return this;
	},
	addEvent : function(type, fn) {
		bkLib.addEvent(this,type,fn);
		return this;	
	},
	setContent : function(c) {
		if(c == 'Share your experience') { 
			this.innerHTML = "";
		} else if(c == 'Add New Answer') {
			this.innerHTML = "";
		} else {
			this.innerHTML = c;
		}
		return this;
	},
	pos : function() {
		var curleft = curtop = 0;
		var o = obj = this;
		if (obj.offsetParent) {
			do {
				curleft += obj.offsetLeft;
				curtop += obj.offsetTop;
			} while (obj = obj.offsetParent);
		}
		var b = (!window.opera) ? parseInt(this.getStyle('border-width') || this.style.border) || 0 : 0;
		return [curleft+b,curtop+b+this.offsetHeight];
	},
	noSelect : function() {
		bkLib.noSelect(this);
		return this;
	},
	
	parentTag : function(t) {
		var elm = this;
		 do {
			if(elm && elm.nodeName && elm.nodeName.toUpperCase() == t) {
				return elm;
			}
			elm = elm.parentNode;
		} while(elm);
		return false;
	},
	
	hasClass : function(cls) {
		return this.className.match(new RegExp('(\\s|^)nicEdit-'+cls+'(\\s|$)'));
	},
	
	addClass : function(cls) {
		if (!this.hasClass(cls)) { this.className += " nicEdit-"+cls };
		return this;
	},
	
	removeClass : function(cls) {
		if (this.hasClass(cls)) {
			this.className = this.className.replace(new RegExp('(\\s|^)nicEdit-'+cls+'(\\s|$)'),' ');
		}
		return this;
	},

	setStyle : function(st) {
		var elmStyle = this.style;
		for(var itm in st) {
			switch(itm) {
				case 'float':
					elmStyle['cssFloat'] = elmStyle['styleFloat'] = st[itm];
					break;
				case 'opacity':
					elmStyle.opacity = st[itm];
					elmStyle.filter = "alpha(opacity=" + Math.round(st[itm]*100) + ")"; 
					break;
				case 'className':
					this.className = st[itm];
					break;
				default:
					//if(document.compatMode || itm != "cursor") { // Nasty Workaround for IE 5.5
						elmStyle[itm] = st[itm];
					//}		
			}
		}
		return this;
	},
	
	getStyle : function( cssRule, d ) {
		var doc = (!d) ? document.defaultView : d; 
		if(this.nodeType == 1)
		return (doc && doc.getComputedStyle) ? doc.getComputedStyle( this, null ).getPropertyValue(cssRule) : this.currentStyle[ bkLib.camelize(cssRule) ];
	},
	
	remove : function() {
		this.parentNode.removeChild(this);
		return this;	
	},
	
	setAttributes : function(at) {
		for(var itm in at) {
			this[itm] = at[itm];
		}
		return this;
	}
});

var bkLib = {
	isMSIE : (navigator.appVersion.indexOf("MSIE") != -1),
	
	addEvent : function(obj, type, fn) {
		(obj.addEventListener) ? obj.addEventListener( type, fn, false ) : obj.attachEvent("on"+type, fn);	
	},
	
	toArray : function(iterable) {
		var length = iterable.length, results = new Array(length);
    	while (length--) { results[length] = iterable[length] };
    	return results;	
	},
	
	noSelect : function(element) {
		if(element.setAttribute && element.nodeName.toLowerCase() != 'input' && element.nodeName.toLowerCase() != 'textarea') {
			element.setAttribute('unselectable','on');
		}
		for(var i=0;i<element.childNodes.length;i++) {
			bkLib.noSelect(element.childNodes[i]);
		}
	},
	camelize : function(s) {
		return s.replace(/\-(.)/g, function(m, l){return l.toUpperCase()});
	},
	inArray : function(arr,item) {
	    return (bkLib.search(arr,item) != null);
	},
	search : function(arr,itm) {
		for(var i=0; i < arr.length; i++) {
			if(arr[i] == itm)
				return i;
		}
		return null;	
	},
	cancelEvent : function(e) {
		e = e || window.event;
		if(e.preventDefault && e.stopPropagation) {
			e.preventDefault();
			e.stopPropagation();
		}
		return false;
	},
	domLoad : [],
	domLoaded : function() {
		if (arguments.callee.done) return;
		arguments.callee.done = true;
		for (i = 0;i < bkLib.domLoad.length;i++) bkLib.domLoad[i]();
	},
	onDomLoaded : function(fireThis) {
		this.domLoad.push(fireThis);
		if (document.addEventListener) {
			document.addEventListener("DOMContentLoaded", bkLib.domLoaded, null);
		} else if(bkLib.isMSIE) {
			document.write("<style>.nicEdit-main p { margin: 0; }</style><scr"+"ipt id=__ie_onload defer " + ((location.protocol == "https:") ? "src='javascript:void(0)'" : "src=//0") + "><\/scr"+"ipt>");
			$BK("__ie_onload").onreadystatechange = function() {
			    if (this.readyState == "complete"){bkLib.domLoaded();}
			};
		}
	    window.onload = bkLib.domLoaded;
	}
};

function $BK(elm) {
	if(typeof(elm) == "string") {
		elm = document.getElementById(elm);
	}
	return (elm && !elm.appendTo) ? bkExtend(elm,bkElement.prototype) : elm;
}

var bkEvent = {
	addEvent : function(evType, evFunc) {
		if(evFunc) {
			this.eventList = this.eventList || {};
			this.eventList[evType] = this.eventList[evType] || [];
			this.eventList[evType].push(evFunc);
		}
		return this;
	},
	fireEvent : function() {
		var args = bkLib.toArray(arguments), evType = args.shift();
		if(this.eventList && this.eventList[evType]) {
			for(var i=0;i<this.eventList[evType].length;i++) {
				this.eventList[evType][i].apply(this,args);
			}
		}
	}	
};

function __(s) {
	return s;
}

Function.prototype.closure = function() {
  var __method = this, args = bkLib.toArray(arguments), obj = args.shift();
  return function() { if(typeof(bkLib) != 'undefined') { return __method.apply(obj,args.concat(bkLib.toArray(arguments))); } };
}
	
Function.prototype.closureListener = function() {
  	var __method = this, args = bkLib.toArray(arguments), object = args.shift(); 
  	return function(e) { 
  	e = e || window.event;
  	if(e.target) { var target = e.target; } else { var target =  e.srcElement };
	  	return __method.apply(object, [e,target].concat(args) ); 
	};
}		


/* START CONFIG */

var nicEditorConfig = bkClass.extend({
	buttons : {
		'bold' : {name : __('Click to Bold'), command : 'Bold', tags : ['B','STRONG'], css : {'font-weight' : 'bold'}, key : 'b'},
		'italic' : {name : __('Click to Italic'), command : 'Italic', tags : ['EM','I'], css : {'font-style' : 'italic'}, key : 'i'},
		'underline' : {name : __('Click to Underline'), command : 'Underline', tags : ['U'], css : {'text-decoration' : 'underline'}, key : 'u'},
		'left' : {name : __('Left Align'), command : 'justifyleft', noActive : true},
		'center' : {name : __('Center Align'), command : 'justifycenter', noActive : true},
		'right' : {name : __('Right Align'), command : 'justifyright', noActive : true},
		'justify' : {name : __('Justify Align'), command : 'justifyfull', noActive : true},
		'ol' : {name : __('Insert Ordered List'), command : 'insertorderedlist', tags : ['OL']},
		'ul' : 	{name : __('Insert Unordered List'), command : 'insertunorderedlist', tags : ['UL']},
		'subscript' : {name : __('Click to Subscript'), command : 'subscript', tags : ['SUB']},
		'superscript' : {name : __('Click to Superscript'), command : 'superscript', tags : ['SUP']},
		'strikethrough' : {name : __('Click to Strike Through'), command : 'strikeThrough', css : {'text-decoration' : 'line-through'}},
		'removeformat' : {name : __('Remove Formatting'), command : 'removeformat', noActive : true},
		'indent' : {name : __('Indent Text'), command : 'indent', noActive : true},
		'outdent' : {name : __('Remove Indent'), command : 'outdent', noActive : true},
		'hr' : {name : __('Horizontal Rule'), command : 'insertHorizontalRule', noActive : true}
	},
	iconsPath : '/public/images/nicEditorIcons.gif',
	buttonList : ['save','bold','italic','underline','left','center','right','justify','ol','ul','fontSize','fontFamily','fontFormat','indent','outdent','image','upload','link','unlink','forecolor','bgcolor'],
	iconList : {"bold":1,"center":2,"hr":3,"indent":4,"italic":5,"justify":6,"left":7,"ol":8,"outdent":9,"removeformat":10,"right":11,"save":12,"strikethrough":13,"subscript":14,"superscript":15,"ul":16,"underline":17,"link":18,"unlink":19,"close":20,"upload":21}
	
});
/* END CONFIG */


var nicEditors = {
	nicPlugins : [],
	editors : [],
	
	registerPlugin : function(plugin,options) {
		this.nicPlugins.push({p : plugin, o : options});
	},

	allTextAreas : function(nicOptions) {
		var textareas = document.getElementsByTagName("textarea");
		for(var i=0;i<textareas.length;i++) {
			nicEditors.editors.push(new nicEditor(nicOptions).panelInstance(textareas[i]));
		}
		return nicEditors.editors;
	},
	
	findEditor : function(e) {
		var editors = nicEditors.editors;
		for(var i=0;i<editors.length;i++) {
			if(editors[i].instanceById(e)) {
				return editors[i].instanceById(e);
			}
		}
	}
};


var nicEditor = bkClass.extend({
	construct : function(o) {
		this.options = new nicEditorConfig();
		bkExtend(this.options,o);
		this.nicInstances = new Array();
		this.loadedPlugins = new Array();
		
		var plugins = nicEditors.nicPlugins;
		for(var i=0;i<plugins.length;i++) {
			this.loadedPlugins.push(new plugins[i].p(this,plugins[i].o));
		}
		nicEditors.editors.push(this);
		bkLib.addEvent(document.body,'mousedown', this.selectCheck.closureListener(this) );
	},
	
	panelInstance : function(e,o) {
		e = this.checkReplace($BK(e));
		var panelElm = new bkElement('DIV').setStyle({width : '100%'}).appendBefore(e);
		this.setPanel(panelElm);
		return this.addInstance(e,o);	
	},

	checkReplace : function(e) {
		var r = nicEditors.findEditor(e);
		if(r) {
			r.removeInstance(e);
			r.removePanel();
		}
		return e;
	},

	addInstance : function(e,o) {
		e = this.checkReplace($BK(e));
		if( e.contentEditable || !!window.opera ) {
			var newInstance = new nicEditorInstance(e,o,this);
		} else {
			var newInstance = new nicEditorIFrameInstance(e,o,this);
		}
		this.nicInstances.push(newInstance);
		return this;
	},
	
	removeInstance : function(e) {
		e = $BK(e);
		var instances = this.nicInstances;
		for(var i=0;i<instances.length;i++) {	
			if(instances[i].e == e) {
				instances[i].remove();
				this.nicInstances.splice(i,1);
			}
		}
	},

	removePanel : function(e) {
		if(this.nicPanel) {
			this.nicPanel.remove();
			this.nicPanel = null;
		}	
	},

	instanceById : function(e) {
		e = $BK(e);
		var instances = this.nicInstances;
		for(var i=0;i<instances.length;i++) {
			if(instances[i].e == e) {
				return instances[i];
			}
		}	
	},

	setPanel : function(e) {
		this.nicPanel = new nicEditorPanel($BK(e),this.options,this);
		this.fireEvent('panel',this.nicPanel);
		return this;
	},
	
	nicCommand : function(cmd,args) {	
		if(this.selectedInstance) {
			this.selectedInstance.nicCommand(cmd,args);
		}
	},
	
	getIcon : function(iconName,options) {
		var icon = this.options.iconList[iconName];
		var file = (options.iconFiles) ? options.iconFiles[iconName] : '';
		return {backgroundImage : "url('"+((icon) ? this.options.iconsPath : file)+"')", backgroundPosition : ((icon) ? ((icon-1)*-18) : 0)+'px 0px'};	
	},
		
	selectCheck : function(e,t) {
		var found = false;
		do{
			if(t.className && t.className.indexOf('nicEdit') != -1) {
				return false;
			}
		} while(t = t.parentNode);
		this.fireEvent('blur',this.selectedInstance,t);
		this.lastSelectedInstance = this.selectedInstance;
		this.selectedInstance = null;
		return false;
	}
	
});
nicEditor = nicEditor.extend(bkEvent);

 
var nicEditorInstance = bkClass.extend({
	isSelected : false,
	
	construct : function(e,options,nicEditor) {
		this.ne = nicEditor;
		this.elm = this.e = e;
		this.options = options || {};
		
		newX = parseInt(e.getStyle('width')) || e.clientWidth;
		newY = parseInt(e.getStyle('height')) || e.clientHeight;
		this.initialHeight = newY-8;
		
		var isTextarea = (e.nodeName.toLowerCase() == "textarea");
		if(isTextarea || this.options.hasPanel) {
			var ie7s = (bkLib.isMSIE && !((typeof document.body.style.maxHeight != "undefined") && document.compatMode == "CSS1Compat"))
			var s = {width: '100%', border : '1px solid #ccc', borderTop : 0, overflowY : 'auto', overflowX: 'hidden' };
			s[(ie7s) ? 'height' : 'maxHeight'] = (this.ne.options.maxHeight) ? this.ne.options.maxHeight+'px' : null;
			this.editorContain = new bkElement('DIV').setStyle(s).appendBefore(e);
			var editorElm = new bkElement('DIV').setStyle({width : '100%', margin: '4px', minHeight : newY+'px'}).addClass('main').appendTo(this.editorContain);

			e.setStyle({display : 'none'});
				
			editorElm.innerHTML = e.innerHTML;		
			if(isTextarea) {
				editorElm.setContent(e.value);
				this.copyElm = e;
				var f = e.parentTag('FORM');
				if(f) { bkLib.addEvent( f, 'submit', this.saveContent.closure(this)); }
			}
			editorElm.setStyle((ie7s) ? {height : newY+'px'} : {overflow: 'hidden'});
			this.elm = editorElm;	
		}
		this.ne.addEvent('blur',this.blur.closure(this));

		this.init();
		this.blur();
	},
	
	init : function() {
		this.elm.setAttribute('contentEditable','true');	
		if(this.getContent() == "") {
			this.setContent('<br />');
		} 
		this.instanceDoc = document.defaultView;
		this.elm.addEvent('mousedown',this.selected.closureListener(this)).addEvent('keypress',this.keyDown.closureListener(this)).addEvent('focus',this.selected.closure(this)).addEvent('blur',this.blur.closure(this)).addEvent('keyup',this.selected.closure(this));
		this.ne.fireEvent('add',this);
		
		 /* CLEAN WORD PASTE MOD */
        this.elm.addEvent('paste',this.initPasteClean.closureListener(this));
        
	},
	
	initPasteClean : function() {
        this.pasteCache = this.getElm().innerHTML;
        setTimeout(this.pasteClean.closure(this),100);
    },
    
    /* CLEAN WORD PASTE MOD : pasteClean method added for clean word paste */
    pasteClean : function() {
        var matchedHead = "";
        var matchedTail = "";
        var newContent = this.getElm().innerHTML;
        this.ne.fireEvent("get",this);
        var newContentStart = 0;
        var newContentFinish = 0;
        var newSnippet = "";
        var tempNode = document.createElement("div");

        /* Find start of both strings that matches */

        for (newContentStart = 0; newContent.charAt(newContentStart) == this.pasteCache.charAt(newContentStart); newContentStart++)
        {
            matchedHead += this.pasteCache.charAt(newContentStart);
        }

        /* If newContentStart is inside a HTML tag, move to opening brace of tag */
        for (var i = newContentStart; i >= 0; i--)
        {
            if (this.pasteCache.charAt(i) == "<")
            {
                newContentStart = i;
                matchedHead = this.pasteCache.substring(0, newContentStart);

                break;
            }
            else if(this.pasteCache.charAt(i) == ">")
            {
                break;
            }
        }

        newContent = this.reverse(newContent);
        this.pasteCache = this.reverse(this.pasteCache);

        /* Find end of both strings that matches */
        for (newContentFinish = 0; newContent.charAt(newContentFinish) == this.pasteCache.charAt(newContentFinish); newContentFinish++)
        {
            matchedTail += this.pasteCache.charAt(newContentFinish);
        }

        /* If newContentFinish is inside a HTML tag, move to closing brace of tag */
        for (var i = newContentFinish; i >= 0; i--)
        {
            if (this.pasteCache.charAt(i) == ">")
            {
                newContentFinish = i;
                matchedTail = this.pasteCache.substring(0, newContentFinish);

                break;
            }
            else if(this.pasteCache.charAt(i) == "<")
            {
                break;
            }
        }

        matchedTail = this.reverse(matchedTail);

        /* If there's no difference in pasted content */
        if (newContentStart == newContent.length - newContentFinish)
        {
            return false;
        }

        newContent = this.reverse(newContent);
        newSnippet = newContent.substring(newContentStart, newContent.length - newContentFinish);
        newSnippet = this.validTags(newSnippet);

        /* Replace opening bold tags with strong */
        newSnippet = newSnippet.replace(/<b(\s+|>)/g, "<strong$1");
        /* Replace closing bold tags with closing strong */
        newSnippet = newSnippet.replace(/<\/b(\s+|>)/g, "</strong$1");

        /* Replace italic tags with em */
        newSnippet = newSnippet.replace(/<i(\s+|>)/g, "<em$1");
        /* Replace closing italic tags with closing em */
        newSnippet = newSnippet.replace(/<\/i(\s+|>)/g, "</em$1");

        /* strip out comments -cgCraft */
        newSnippet = newSnippet.replace(/<!(?:--[\s\S]*?--\s*)?>\s*/g, "");

        /* strip out &nbsp; -cgCraft */
        newSnippet = newSnippet.replace(/&nbsp;/gi, " ");
        /* strip out extra spaces -cgCraft */
        newSnippet = newSnippet.replace(/ <\//gi, "</");

	/*Added to add link external */
	newSnippet = newSnippet.replace(/<a/g,"<a target='_blank'");

        while (newSnippet.indexOf("  ") != -1) {
            var anArray = newSnippet.split("  ")
            newSnippet = anArray.join(" ")
        }

        /* strip &nbsp; -cgCraft */
        newSnippet = newSnippet.replace(/^\s*|\s*$/g, "");

        /* Strip out unaccepted attributes */

        newSnippet = newSnippet.replace(/<[^>]*>/g, function(match)
            {
                match = match.replace(/ ([^=]+)="[^"]*"/g, function(match2, attributeName)
                    {
                        if (attributeName == "alt" || attributeName == "href" || attributeName == "src" || attributeName == "title")
                        {
                            return match2;
                        }
                        if (attributeName == "style"){
                        	return "";
                        }
                        return "";
                    });

                return match;
            }
            );

        /* Final cleanout for MS Word cruft */
        newSnippet = newSnippet.replace(/<\?xml[^>]*>/g, "");
        newSnippet = newSnippet.replace(/<[^ >]+:[^>]*>/g, "");
        newSnippet = newSnippet.replace(/<\/[^ >]+:[^>]*>/g, "");

        /* remove undwanted tags */
        newSnippet = newSnippet.replace(/<(div|span|style|meta|link|img|font|br){1}.*?>/gi,'');

        this.content = matchedHead + newSnippet + matchedTail;
        this.ne.fireEvent("set",this);
        this.elm.innerHTML = this.content;
    },
	
    reverse : function(sentString) {
        var theString = "";
        for (var i = sentString.length - 1; i >= 0; i--) {
            theString += sentString.charAt(i);
        }
        return theString;
    },

    /* CLEAN WORD PASTE MOD : validTags method added for clean word paste */
    validTags : function(snippet) {
        var theString = snippet;

        /* Replace uppercase element names with lowercase */
        theString = theString.replace(/<[^> ]*/g, function(match){return match.toLowerCase();});

        /* Replace uppercase attribute names with lowercase */
        theString = theString.replace(/<[^>]*>/g, function(match) {
            match = match.replace(/ [^=]+=/g, function(match2){return match2.toLowerCase();});
            return match;
        });

        /* Put quotes around unquoted attributes */
        theString = theString.replace(/<[^>]*>/g, function(match) {
            match = match.replace(/( [^=]+=)([^"][^ >]*)/g, "$1\"$2\"");
            return match;
        });

        return theString;
    },
    
	remove : function() {
		this.saveContent();
		if(this.copyElm || this.options.hasPanel) {
			this.editorContain.remove();
			this.e.setStyle({'display' : 'block'});
			this.ne.removePanel();
		}
		this.disable();
		this.ne.fireEvent('remove',this);
	},
	
	disable : function() {
		this.elm.setAttribute('contentEditable','false');
	},
	
	getSel : function() {
		return (window.getSelection) ? window.getSelection() : document.selection;	
	},
	
	getRng : function() {
		var s = this.getSel();
		if(!s || s.rangeCount === 0) { return; }
		return (s.rangeCount > 0) ? s.getRangeAt(0) : s.createRange();
	},
	
	selRng : function(rng,s) {
		if(window.getSelection) {
			s.removeAllRanges();
			s.addRange(rng);
		} else {
			rng.select();
		}
	},
	
	selElm : function() {
		var r = this.getRng();
		if(!r) { return; }
		if(r.startContainer) {
			var contain = r.startContainer;
			if(r.cloneContents().childNodes.length == 1) {
				for(var i=0;i<contain.childNodes.length;i++) {
					var rng = contain.childNodes[i].ownerDocument.createRange();
					rng.selectNode(contain.childNodes[i]);					
					if(r.compareBoundaryPoints(Range.START_TO_START,rng) != 1 && 
						r.compareBoundaryPoints(Range.END_TO_END,rng) != -1) {
						return $BK(contain.childNodes[i]);
					}
				}
			}
			return $BK(contain);
		} else {
			return $BK((this.getSel().type == "Control") ? r.item(0) : r.parentElement());
		}
	},
	
	saveRng : function() {
		this.savedRange = this.getRng();
		this.savedSel = this.getSel();
	},
	
	restoreRng : function() {
		if(this.savedRange) {
			this.selRng(this.savedRange,this.savedSel);
		}
	},
	
	keyDown : function(e,t) {
		if(e.ctrlKey) {
			this.ne.fireEvent('key',this,e);
		}
	},
	
	selected : function(e,t) {
		if(!t && !(t = this.selElm)) { t = this.selElm(); }
		if(!e.ctrlKey) {
			var selInstance = this.ne.selectedInstance;
			if(selInstance != this) {
				if(selInstance) {
					this.ne.fireEvent('blur',selInstance,t);
				}
				this.ne.selectedInstance = this;	
				this.ne.fireEvent('focus',selInstance,t);
			}
			this.ne.fireEvent('selected',selInstance,t);
			this.isFocused = true;
			this.elm.addClass('selected');
		}
		return false;
	},
	
	blur : function() {
		this.isFocused = false;
		this.elm.removeClass('selected');
	},
	
	saveContent : function() {
		if(this.copyElm || this.options.hasPanel) {
			this.ne.fireEvent('save',this);
			(this.copyElm) ? this.copyElm.value = this.getContent() : this.e.innerHTML = this.getContent();
		}	
	},
	
	getElm : function() {
		return this.elm;
	},
	
	getContent : function() {
		var tempConten = this.getElm().innerHTML; 
		this.content = tempConten.replace(/^(<br>)*|(<br>$)/g,"");
		this.ne.fireEvent('get',this);
		return this.content;
	},
	
	setContent : function(e) {
		this.content = e;
		this.ne.fireEvent('set',this);
		this.elm.innerHTML = this.content;	
	},
	
	nicCommand : function(cmd,args) {
		document.execCommand(cmd,false,args);
	}		
});

var nicEditorIFrameInstance = nicEditorInstance.extend({
	savedStyles : [],
	
	init : function() {	
		var c = this.elm.innerHTML.replace(/^\s+|\s+$/g, '');
		this.elm.innerHTML = '';
		(!c) ? c = "<br />" : c;
		this.initialContent = c;
		
		this.elmFrame = new bkElement('iframe').setAttributes({'src' : 'javascript:;', 'frameBorder' : 0, 'allowTransparency' : 'true', 'scrolling' : 'no'}).setStyle({height: '100px', width: '100%'}).addClass('frame').appendTo(this.elm);

		if(this.copyElm) { this.elmFrame.setStyle({width : (this.elm.offsetWidth-4)+'px'}); }
		
		var styleList = ['font-size','font-family','font-weight','color'];
		for(itm in styleList) {
			this.savedStyles[bkLib.camelize(itm)] = this.elm.getStyle(itm);
		}
     	
		setTimeout(this.initFrame.closure(this),50);
	},
	
	disable : function() {
		this.elm.innerHTML = this.getContent();
	},
	
	initFrame : function() {
		var fd = $BK(this.elmFrame.contentWindow.document);
		fd.designMode = "on";		
		fd.open();
		var css = this.ne.options.externalCSS;
		fd.write('<html><head>'+((css) ? '<link href="'+css+'" rel="stylesheet" type="text/css" />' : '')+'</head><body id="nicEditContent" style="margin: 0 !important; background-color: transparent !important;">'+this.initialContent+'</body></html>');
		fd.close();
		this.frameDoc = fd;

		this.frameWin = $BK(this.elmFrame.contentWindow);
		this.frameContent = $BK(this.frameWin.document.body).setStyle(this.savedStyles);
		this.instanceDoc = this.frameWin.document.defaultView;
		
		this.heightUpdate();
		this.frameDoc.addEvent('mousedown', this.selected.closureListener(this)).addEvent('keyup',this.heightUpdate.closureListener(this)).addEvent('keydown',this.keyDown.closureListener(this)).addEvent('keyup',this.selected.closure(this));
		this.ne.fireEvent('add',this);
	},
	
	getElm : function() {
		return this.frameContent;
	},
	
	setContent : function(c) {
		this.content = c;
		this.ne.fireEvent('set',this);
		this.frameContent.innerHTML = this.content;	
		this.heightUpdate();
	},
	
	getSel : function() {
		return (this.frameWin) ? this.frameWin.getSelection() : this.frameDoc.selection;
	},
	
	heightUpdate : function() {	
		this.elmFrame.style.height = Math.max(this.frameContent.offsetHeight,this.initialHeight)+'px';
	},
    
	nicCommand : function(cmd,args) {
		this.frameDoc.execCommand(cmd,false,args);
		setTimeout(this.heightUpdate.closure(this),100);
	}

	
});
var nicEditorPanel = bkClass.extend({
	construct : function(e,options,nicEditor) {
		this.elm = e;
		this.options = options;
		this.ne = nicEditor;
		this.panelButtons = new Array();
		this.buttonList = bkExtend([],this.ne.options.buttonList);
		
		this.panelContain = new bkElement('DIV').setStyle({overflow : 'hidden', width : '100%', border : '1px solid #cccccc', backgroundColor : '#efefef'}).addClass('panelContain');
		this.panelElm = new bkElement('DIV').setStyle({margin : '2px', marginTop : '0px', zoom : 1, overflow : 'hidden'}).addClass('panel').appendTo(this.panelContain);
		this.panelContain.appendTo(e);

		var opt = this.ne.options;
		var buttons = opt.buttons;
		for(button in buttons) {
				this.addButton(button,opt,true);
		}
		this.reorder();
		e.noSelect();
	},
	
	addButton : function(buttonName,options,noOrder) {
		var button = options.buttons[buttonName];
		var type = (button['type']) ? eval('(typeof('+button['type']+') == "undefined") ? null : '+button['type']+';') : nicEditorButton;
		var hasButton = bkLib.inArray(this.buttonList,buttonName);
		if(type && (hasButton || this.ne.options.fullPanel)) {
			this.panelButtons.push(new type(this.panelElm,buttonName,options,this.ne));
			if(!hasButton) {	
				this.buttonList.push(buttonName);
			}
		}
	},
	
	findButton : function(itm) {
		for(var i=0;i<this.panelButtons.length;i++) {
			if(this.panelButtons[i].name == itm)
				return this.panelButtons[i];
		}	
	},
	
	reorder : function() {
		var bl = this.buttonList;
		for(var i=0;i<bl.length;i++) {
			var button = this.findButton(bl[i]);
			if(button) {
				this.panelElm.appendChild(button.margin);
			}
		}	
	},
	
	remove : function() {
		this.elm.remove();
	}
});
var nicEditorButton = bkClass.extend({
	
	construct : function(e,buttonName,options,nicEditor) {
		this.options = options.buttons[buttonName];
		this.name = buttonName;
		this.ne = nicEditor;
		this.elm = e;

		this.margin = new bkElement('DIV').setStyle({'float' : 'left', marginTop : '2px'}).appendTo(e);
		this.contain = new bkElement('DIV').setStyle({width : '20px', height : '20px'}).addClass('buttonContain').appendTo(this.margin);
		this.border = new bkElement('DIV').setStyle({backgroundColor : '#efefef', border : '1px solid #efefef'}).appendTo(this.contain);
		this.button = new bkElement('DIV').setStyle({width : '18px', height : '18px', overflow : 'hidden', zoom : 1, cursor : 'pointer'}).addClass('button').setStyle(this.ne.getIcon(buttonName,options)).appendTo(this.border);
		this.button.addEvent('mouseover', this.hoverOn.closure(this)).addEvent('mouseout',this.hoverOff.closure(this)).addEvent('mousedown',this.mouseClick.closure(this)).noSelect();
		
		if(!window.opera) {
			this.button.onmousedown = this.button.onclick = bkLib.cancelEvent;
		}
		
		nicEditor.addEvent('selected', this.enable.closure(this)).addEvent('blur', this.disable.closure(this)).addEvent('key',this.key.closure(this));
		
		this.disable();
		this.init();
	},
	
	init : function() {  },
	
	hide : function() {
		this.contain.setStyle({display : 'none'});
	},
	
	updateState : function() {
		if(this.isDisabled) { this.setBg(); }
		else if(this.isHover) { this.setBg('hover'); }
		else if(this.isActive) { this.setBg('active'); }
		else { this.setBg(); }
	},
	
	setBg : function(state) {
		switch(state) {
			case 'hover':
				var stateStyle = {border : '1px solid #666', backgroundColor : '#ddd'};
				break;
			case 'active':
				var stateStyle = {border : '1px solid #666', backgroundColor : '#ccc'};
				break;
			default:
				var stateStyle = {border : '1px solid #efefef', backgroundColor : '#efefef'};	
		}
		this.border.setStyle(stateStyle).addClass('button-'+state);
	},
	
	checkNodes : function(e) {
		var elm = e;	
		do {
			if(this.options.tags && bkLib.inArray(this.options.tags,elm.nodeName)) {
				this.activate();
				return true;
			}
		} while(elm = elm.parentNode && elm.className != "nicEdit");
		elm = $BK(e);
		while(elm.nodeType == 3) {
			elm = $BK(elm.parentNode);
		}
		if(this.options.css) {
			for(itm in this.options.css) {
				if(elm.getStyle(itm,this.ne.selectedInstance.instanceDoc) == this.options.css[itm]) {
					this.activate();
					return true;
				}
			}
		}
		this.deactivate();
		return false;
	},
	
	activate : function() {
		if(!this.isDisabled) {
			this.isActive = true;
			this.updateState();	
			this.ne.fireEvent('buttonActivate',this);
		}
	},
	
	deactivate : function() {
		this.isActive = false;
		this.updateState();	
		if(!this.isDisabled) {
			this.ne.fireEvent('buttonDeactivate',this);
		}
	},
	
	enable : function(ins,t) {
		this.isDisabled = false;
		this.contain.setStyle({'opacity' : 1}).addClass('buttonEnabled');
		this.updateState();
		this.checkNodes(t);
	},
	
	disable : function(ins,t) {		
		this.isDisabled = true;
		this.contain.setStyle({'opacity' : 0.6}).removeClass('buttonEnabled');
		this.updateState();	
	},
	
	toggleActive : function() {
		(this.isActive) ? this.deactivate() : this.activate();	
	},
	
	hoverOn : function() {
		if(!this.isDisabled) {
			this.isHover = true;
			this.updateState();
			this.ne.fireEvent("buttonOver",this);
		}
	}, 
	
	hoverOff : function() {
		this.isHover = false;
		this.updateState();
		this.ne.fireEvent("buttonOut",this);
	},
	
	mouseClick : function() {
		if(this.options.command) {
			this.ne.nicCommand(this.options.command,this.options.commandArgs);
			if(!this.options.noActive) {
				this.toggleActive();
			}
		}
		this.ne.fireEvent("buttonClick",this);
	},
	
	key : function(nicInstance,e) {
		if(this.options.key && e.ctrlKey && String.fromCharCode(e.keyCode || e.charCode).toLowerCase() == this.options.key) {
			this.mouseClick();
			if(e.preventDefault) e.preventDefault();
		}
	}
	
});

 
var nicPlugin = bkClass.extend({
	
	construct : function(nicEditor,options) {
		this.options = options;
		this.ne = nicEditor;
		this.ne.addEvent('panel',this.loadPanel.closure(this));
		
		this.init();
	},

	loadPanel : function(np) {
		var buttons = this.options.buttons;
		for(var button in buttons) {
			np.addButton(button,this.options);
		}
		np.reorder();
	},

	init : function() {  }
});



 
 /* START CONFIG */
var nicPaneOptions = { };
/* END CONFIG */

var nicEditorPane = bkClass.extend({
	construct : function(elm,nicEditor,options,openButton) {
		this.ne = nicEditor;
		this.elm = elm;
		this.pos = elm.pos();
		
		this.contain = new bkElement('div').setStyle({zIndex : '99999', overflow : 'hidden', position : 'absolute', left : this.pos[0]+'px', top : this.pos[1]+'px'})
		this.pane = new bkElement('div').setStyle({fontSize : '12px', border : '1px solid #ccc', 'overflow': 'hidden', padding : '4px', textAlign: 'left', backgroundColor : '#ffffc9'}).addClass('pane').setStyle(options).appendTo(this.contain);
		
		if(openButton && !openButton.options.noClose) {
			this.close = new bkElement('div').setStyle({'float' : 'right', height: '16px', width : '16px', cursor : 'pointer'}).setStyle(this.ne.getIcon('close',nicPaneOptions)).addEvent('mousedown',openButton.removePane.closure(this)).appendTo(this.pane);
		}
		
		this.contain.noSelect().appendTo(document.body);
		
		this.position();
		this.init();	
	},
	
	init : function() { },
	
	position : function() {
		if(this.ne.nicPanel) {
			var panelElm = this.ne.nicPanel.elm;	
			var panelPos = panelElm.pos();
			var newLeft = panelPos[0]+parseInt(panelElm.getStyle('width'))-(parseInt(this.pane.getStyle('width'))+8);
			if(newLeft < this.pos[0]) {
				this.contain.setStyle({left : newLeft+'px'});
			}
		}
	},
	
	toggle : function() {
		this.isVisible = !this.isVisible;
		this.contain.setStyle({display : ((this.isVisible) ? 'block' : 'none')});
	},
	
	remove : function() {
		if(this.contain) {
			this.contain.remove();
			this.contain = null;
		}
	},
	
	append : function(c) {
		c.appendTo(this.pane);
	},
	
	setContent : function(c) {
		this.pane.setContent(c);
	}
	
});


 
var nicEditorAdvancedButton = nicEditorButton.extend({
	
	
	init : function() {
		var selectInstance;
		//this.ne.addEvent('selected',this.removePane.closure(this));.addEvent('blur',this.removePane.closure(this));	
	},
	
	mouseClick : function() {
		if(!this.isDisabled) {
			if(this.pane && this.pane.pane) {
				//this.removePane();
			} else {
				this.pane = new nicEditorPane(this.contain,this.ne,{width : (this.width || '270px'), backgroundColor : '#fff'},this);
				this.addPane();
				selectInstance = this.ne.selectedInstance;
				this.ne.selectedInstance.saveRng();
			}
		}
	},
	
	addForm : function(f,elm) {
		this.form = new bkElement('form').addEvent('submit',this.submit.closureListener(this));
		this.pane.append(this.form);
		this.inputs = {};
		selectInstance = this.ne.selectedInstance;
		for(itm in f) {
			var field = f[itm];
			var val = '';
			if(elm) {
				val = elm.getAttribute(itm);
			}
			if(!val) {
				val = field['value'] || '';
			}
			var type = f[itm].type;
			
			if(type == 'title') {
					new bkElement('div').setContent(field.txt).setStyle({fontSize : '14px', fontWeight: 'bold', padding : '0px', margin : '2px 0'}).appendTo(this.form);
			} else {
				var contain = new bkElement('div').setStyle({overflow : 'hidden', clear : 'both'}).appendTo(this.form);
				if(field.txt) {
					new bkElement('label').setAttributes({'for' : itm}).setContent(field.txt).setStyle({margin : '2px 4px', fontSize : '13px', width: '50px', lineHeight : '20px', textAlign : 'left', 'float' : 'left'}).appendTo(contain);
				}
				switch(type) {
					case 'text':
						this.inputs[itm] = new bkElement('input').setAttributes({id : itm, 'value' : val, 'type' : 'text'}).setStyle({margin : '2px 0', fontSize : '13px', 'float' : 'left', height : '20px', border : '1px solid #ccc', overflow : 'hidden'}).setStyle(field.style).appendTo(contain);
						autoCompleteEditor(itm,selectInstance);
						break;
					case 'select':
						this.inputs[itm] = new bkElement('select').setAttributes({id : itm}).setStyle({border : '1px solid #ccc', 'float' : 'left', margin : '2px 0'}).appendTo(contain);
						for(opt in field.options) {
							var o = new bkElement('option').setAttributes({value : opt, selected : (opt == val) ? 'selected' : ''}).setContent(field.options[opt]).appendTo(this.inputs[itm]);
						}
						break;
					case 'content':
						this.inputs[itm] = new bkElement('textarea').setAttributes({id : itm}).setStyle({border : '1px solid #ccc', 'float' : 'left'}).setStyle(field.style).appendTo(contain);
						this.inputs[itm].value = val;
				}
			}
		}
		new bkElement('input').setAttributes({'type' : 'submit','value' : 'Link'}).setStyle({backgroundColor : '#efefef',border : '1px solid #ccc', margin : '3px 0', 'float' : 'right', 'clear' : 'both'}).appendTo(this.form);
		this.form.onsubmit = bkLib.cancelEvent;	
	},
	
	submit : function() { },
	
	findElm : function(tag,attr,val) {
		var list = this.ne.selectedInstance.getElm().getElementsByTagName(tag);
		for(var i=0;i<list.length;i++) {
			if(list[i].getAttribute(attr) == val) {
				return $BK(list[i]);
			}
		}
	},
	
	removePane : function() {
		if(this.pane) {
			this.pane.remove();
			this.pane = null;
			this.ne.selectedInstance = selectInstance;
			this.ne.selectedInstance.restoreRng();
		}	
	}	
});


var nicButtonTips = bkClass.extend({
	construct : function(nicEditor) {
		this.ne = nicEditor;
		nicEditor.addEvent('buttonOver',this.show.closure(this)).addEvent('buttonOut',this.hide.closure(this));

	},
	
	show : function(button) {
		this.timer = setTimeout(this.create.closure(this,button),400);
	},
	
	create : function(button) {
		this.timer = null;
		if(!this.pane) {
			this.pane = new nicEditorPane(button.button,this.ne,{fontSize : '12px', marginTop : '5px'});
			this.pane.setContent(button.options.name);
		}		
	},
	
	hide : function(button) {
		if(this.timer) {
			clearTimeout(this.timer);
		}
		if(this.pane) {
			this.pane = this.pane.remove();
		}
	}
});
nicEditors.registerPlugin(nicButtonTips);



/* START CONFIG */
var nicLinkOptions = {
	buttons : {
		'link' : {name : 'Add Link', type : 'nicLinkButton', tags : ['A']},
		'unlink' : {name : 'Remove Link',  command : 'unlink', noActive : true}
	}
};
/* END CONFIG */

var nicLinkButton = nicEditorAdvancedButton.extend({	
	addPane : function() {
		this.ln = this.ne.selectedInstance.selElm().parentTag('A');
		this.addForm({
			'' : {type : 'title', txt : 'Link to other page'},
			'href' : {type : 'text', txt : 'URL', value : 'http://', style : {width: '210px'}}
			/*
			,'title' : {type : 'text', txt : 'Title'},
			'target' : {type : 'select', txt : 'Open In', options : {'' : 'Current Window', '_blank' : 'New Window'},style : {width : '100px'}}
			*/
		},this.ln);
	},
	
	submit : function(e) {
		var url = this.inputs['href'].value;
		if(url == "http://" || url == "") {
			alert("You must enter a URL to Create a Link");
			return false;
		}
		this.removePane();
		
		if(!this.ln) {
			var tmp = this.inputs['href'].value; //'javascript:nicTemp();';
			this.ne.nicCommand("createlink",tmp);
			this.ln = this.findElm('A','href',tmp);
		}
		if(this.ln) {
			this.ln.setAttributes({
				href : this.inputs['href'].value,
				//title : this.inputs['title'].value,
				target : '_blank'
			});
		}
	}
});

nicEditors.registerPlugin(nicPlugin,nicLinkOptions);



/* START CONFIG */
var nicUploadOptions = {
	buttons : {
		'upload' : {name : 'Upload Image', type : 'nicUploadButton'}
	}
	
};
/* END CONFIG */

var nicUploadButton = nicEditorAdvancedButton.extend({	
	nicURI : '/conversations/uploadAnswerImage',
  errorText : 'Failed to upload image',

	addPane : function() {
    if(typeof window.FormData === "undefined") {
      return this.onError("Image uploads are not supported in this browser, use Chrome, Firefox, or Safari instead.");
    }
    this.im = this.ne.selectedInstance.selElm().parentTag('IMG');

    var container = new bkElement('div')
      .setStyle({ padding: '10px' })
      .appendTo(this.pane.pane);

		new bkElement('div')
      .setStyle({ fontSize: '14px', fontWeight : 'bold', paddingBottom: '5px' })
      .setContent('Insert an Image')
      .appendTo(container);

    this.fileInput = new bkElement('input')
      .setAttributes({ 'type' : 'file' })
      .appendTo(container);

    this.progress = new bkElement('progress')
      .setStyle({ width : '100%', display: 'none' })
      .setAttributes('max', 100)
      .appendTo(container);

    this.fileInput.onchange = this.uploadFile.closure(this);
	},

  onError : function(msg) {
    this.removePane();
    alert(msg || "Failed to upload image");
  },

  uploadFile : function() {
    var file = this.fileInput.files[0];
    if (!file || !file.type.match(/image.*/)) {
      this.onError("Only image files can be uploaded");
      return;
    }
    this.fileInput.setStyle({ display: 'none' });
    this.setProgress(0);

    var fd = new FormData(); // https://hacks.mozilla.org/2011/01/how-to-develop-a-html5-image-uploader/
    fd.append("imageFile", file);
    fd.append("key", "b7ea18a4ecbda8e92203fa4968d10660");
    var xhr = new XMLHttpRequest();
    xhr.open("POST", this.ne.options.uploadURI || this.nicURI);
    xhr.onload = function() {
      try {
        //var res = JSON.parse(xhr.responseText);
    	  var res = xhr.responseText;
    	  var parts = res.split('|');
      } catch(e) {
        return this.onError();
      }
      if(parts.length < 1){
    	  return this.onError();
      } else {
    	  this.onUploaded(parts[0],parts[1]);
      }
    }.closure(this);
    xhr.onerror = this.onError.closure(this);
    xhr.upload.onprogress = function(e) {
      this.setProgress(e.loaded / e.total);
    }.closure(this);
    xhr.send(fd);
  },

  setProgress : function(percent) {
    this.progress.setStyle({ display: 'block' });
    if(percent < .98) {
      this.progress.value = percent;
    } else {
      this.progress.removeAttribute('value');
    }
  },

  onUploaded : function(width,options) {
    this.removePane();
    var src = options;//.links.original;
    if(!this.im) {
      this.ne.selectedInstance.restoreRng();
      var tmp = 'javascript:nicImTemp();';
      this.ne.nicCommand("insertImage", src);
      this.im = this.findElm('IMG','src', src);
    }
    if(this.im) {
      this.im.setAttributes({
        src : src,
        width :Math.min(width, 550)
      });
      
    }
  }
});

function autoCompleteEditor(elm,selectInstance) {
	var url = "/search/ajaxEditorSearch";
	var cache = {};
	$("#"+elm).autocomplete({
		minLength: 1,
		source: function(request, response) {
			var term = ""
			if ( request.term in cache ) {
					response( cache[ request.term ] );
					return;
			}
			request.term = request.term;
			$.ajax({
				url: url,
				dataType: "json",
				data: request,
				success: function( data ) {
					cache[ term ] = data;
					response( data );
				}
			});
		},
		select: function(event, ui) {
			event.preventDefault();
			var url = ui.item.url;
			$("#"+elm).val(url);
		}
	})
	.data( "autocomplete" )._renderItem = function( ul, item ) {
			return $( "<li></li>" )
			.data( "item.autocomplete", item )
			.append( "<a>" + item.label + "&nbsp;<span>" + item.type + "</span></a>" )
			.appendTo( ul );
	};
}

nicEditors.registerPlugin(nicPlugin,nicUploadOptions);
