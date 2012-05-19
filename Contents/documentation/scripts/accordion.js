//by Valerio Proietti (http://mad4milk.net). MIT-style license.
//accordion.js - depends on prototype.js or prototype.lite.js + moo.fx.js
//version 2.0

Fx.Accordion = Class.create();
Fx.Accordion.prototype = Object.extend(new Fx.Base(), {
	
	extendOptions: function(options){
		Object.extend(this.options, Object.extend({
			start: 'open-first',
			fixedHeight: false,
			fixedWidth: false,
			alwaysHide: false,
			wait: false,
			onActive: function(){},
			onBackground: function(){},
			height: true,
			opacity: true,
			width: false
		}, options || {}));
	},

	initialize: function(togglers, elements, options){
		this.now = {};
		this.elements = $A(elements);
		this.togglers = $A(togglers);
		this.setOptions(options);
		this.extendOptions(options);
		this.previousClick = 'nan';
		this.togglers.each(function(tog, i){
			if (tog.onclick) tog.prevClick = tog.onclick;
			else tog.prevClick = function(){};
			$(tog).onclick = function(){
				tog.prevClick();
				this.showThisHideOpen(i);
			}.bind(this);
		}.bind(this));
		this.h = {}; this.w = {}; this.o = {};
		this.elements.each(function(el, i){
			this.now[i+1] = {};
			el.style.height = '0';
			el.style.overflow = 'hidden';
		}.bind(this));
		switch(this.options.start){
			case 'first-open': this.elements[0].style.height = this.elements[0].scrollHeight+'px'; break;
			case 'open-first': this.showThisHideOpen(0); break;
		}
	},
	
	setNow: function(){
		for (var i in this.from){
			var iFrom = this.from[i];
			var iTo = this.to[i];
			var iNow = this.now[i] = {};
			for (var p in iFrom) iNow[p] = this.compute(iFrom[p], iTo[p]);
		}
	},

	custom: function(objObjs){
		if (this.timer && this.options.wait) return;
		var from = {};
		var to = {};
		for (var i in objObjs){
			var iProps = objObjs[i];
			var iFrom = from[i] = {};
			var iTo = to[i] = {};
			for (var prop in iProps){
				iFrom[prop] = iProps[prop][0];
				iTo[prop] = iProps[prop][1];
			}
		}
		return this._start(from, to);
	},

	hideThis: function(i){
		if (this.options.height) this.h = {'height': [this.elements[i].offsetHeight, 0]};
		if (this.options.width) this.w = {'width': [this.elements[i].offsetWidth, 0]};
		if (this.options.opacity) this.o = {'opacity': [this.now[i+1]['opacity'] || 1, 0]};
	},

	showThis: function(i){
		if (this.options.height) this.h = {'height': [this.elements[i].offsetHeight, this.options.fixedHeight || this.elements[i].scrollHeight]};
		if (this.options.width) this.w = {'width': [this.elements[i].offsetWidth, this.options.fixedWidth || this.elements[i].scrollWidth]};
		if (this.options.opacity) this.o = {'opacity': [this.now[i+1]['opacity'] || 0, 1]};
	},

	showThisHideOpen: function(iToShow){
		if (iToShow != this.previousClick || this.options.alwaysHide){
			this.previousClick = iToShow;
			var objObjs = {};
			var err = false;
			var madeInactive = false;
			this.elements.each(function(el, i){
				this.now[i] = this.now[i] || {};
				if (i != iToShow){
					this.hideThis(i);
				} else if (this.options.alwaysHide){
					if (el.offsetHeight == el.scrollHeight){
						this.hideThis(i);
						madeInactive = true;
					} else if (el.offsetHeight == 0){
						this.showThis(i);
					} else {
						err = true;
					}
				} else if (this.options.wait && this.timer){
					this.previousClick = 'nan';
					err = true;
				} else {
					this.showThis(i);
				}
				objObjs[i+1] = Object.extend(this.h, Object.extend(this.o, this.w));
			}.bind(this));
			if (err) return;
			if (!madeInactive) this.options.onActive.call(this, this.togglers[iToShow], iToShow);
			this.togglers.each(function(tog, i){
				if (i != iToShow || madeInactive) this.options.onBackground.call(this, tog, i);
			}.bind(this));
			return this.custom(objObjs);
		}
	},
	
	increase: function(){
		for (var i in this.now){
			var iNow = this.now[i];
			for (var p in iNow) this.setStyle(this.elements[parseInt(i)-1], p, iNow[p]);
		}
	}

});