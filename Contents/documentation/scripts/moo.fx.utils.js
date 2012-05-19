//by Valerio Proietti (http://mad4milk.net). MIT-style license.
//moo.fx.utils.js - depends on prototype.js OR prototype.lite.js + moo.fx.js
//version 2.0

Fx.Height = Class.create();
Fx.Height.prototype = Object.extend(new Fx.Base(), {

	initialize: function(el, options){
		this.element = $(el);
		this.setOptions(options);
		this.element.style.overflow = 'hidden';
	},

	toggle: function(){
		if (this.element.offsetHeight > 0) return this.custom(this.element.offsetHeight, 0);
		else return this.custom(0, this.element.scrollHeight);
	},

	show: function(){
		return this.set(this.element.scrollHeight);
	},

	increase: function(){
		this.setStyle(this.element, 'height', this.now);
	}

});

Fx.Width = Class.create();
Fx.Width.prototype = Object.extend(new Fx.Base(), {

	initialize: function(el, options){
		this.element = $(el);
		this.setOptions(options);
		this.element.style.overflow = 'hidden';
		this.iniWidth = this.element.offsetWidth;
	},

	toggle: function(){
		if (this.element.offsetWidth > 0) return this.custom(this.element.offsetWidth, 0);
		else return this.custom(0, this.iniWidth);
	},

	show: function(){
		return this.set(this.iniWidth);
	},

	increase: function(){
		this.setStyle(this.element, 'width', this.now);
	}

});

Fx.Opacity = Class.create();
Fx.Opacity.prototype = Object.extend(new Fx.Base(), {

	initialize: function(el, options){
		this.element = $(el);
		this.setOptions(options);
		this.now = 1;
	},

	toggle: function(){
		if (this.now > 0) return this.custom(1, 0);
		else return this.custom(0, 1);
	},

	show: function(){
		return this.set(1);
	},
	
	increase: function(){
		this.setStyle(this.element, 'opacity', this.now);
	}

});