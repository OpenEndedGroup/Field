/*  Prototype JavaScript framework, version 1.4.0
 *  (c) 2005 Sam Stephenson <sam@conio.net>
 *
 *  Prototype is freely distributable under the terms of an MIT-style license.
 *  For details, see the Prototype web site: http://prototype.conio.net/
 *
/*--------------------------------------------------------------------------*/

//note: stripped down version of prototype, to be used with moo.fx by mad4milk (http://moofx.mad4milk.net).

var Class = {
	create: function() {
		return function() {
			this.initialize.apply(this, arguments);
		}
	}
};

Object.extend = function(destination, source) {
	for (var property in source) destination[property] = source[property];
	return destination;
};

Function.prototype.bind = function(object) {
	var __method = this;
	return function() {
		return __method.apply(object, arguments);
	}
};

if (!Array.prototype.forEach){
	Array.prototype.forEach = function(fn, bind){
		for(var i = 0; i < this.length ; i++) fn.call(bind, this[i], i);
	};
}

Array.prototype.each = Array.prototype.forEach;

String.prototype.camelize = function(){
	return this.replace(/-\D/gi, function(match){
		return match.charAt(match.length - 1).toUpperCase();
	});
};

var $A = function(iterable) {
	var nArray = [];
	for (var i = 0; i < iterable.length; i++) nArray.push(iterable[i]);
	return nArray;
};

function $() {
	if (arguments.length == 1) return get$(arguments[0]);
	var elements = [];
	$c(arguments).each(function(el){
		elements.push(get$(el));
	});
	return elements;

	function get$(el){
		if (typeof el == 'string') el = document.getElementById(el);
		return el;
	}
};

if (!window.Element) var Element = {};

Object.extend(Element, {

	remove: function(element) {
		element = $(element);
		element.parentNode.removeChild(element);
	},

	hasClassName: function(element, className) {
		element = $(element);
		return !!element.className.match(new RegExp("\\b"+className+"\\b"));
	},

	addClassName: function(element, className) {
		element = $(element);
		if (!Element.hasClassName(element, className)) element.className = (element.className+' '+className);
	},

	removeClassName: function(element, className) {
		element = $(element);
		if (Element.hasClassName(element, className)) element.className = element.className.replace(className, '');
	}

});

document.getElementsByClassName = function(className){
	var elements = [];
	var all = document.getElementsByTagName('*');
	$A(all).each(function(el){
		if (Element.hasClassName(el, className)) elements.push(el);
	});
	return elements;
};