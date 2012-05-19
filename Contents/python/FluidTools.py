#from __future__ import generators, nested_scopes

from field.core.plugins.python import PythonPlugin
from field.namespace.key import CKey
from field.util import PythonUtils
from field.util.PythonUtils import OKeyByName
from field.core.util import PythonCallableMap
from field.graphics.core import Base
from field.math.linalg import Vector2
from field.math.linalg import Vector3
from field.math.linalg import Vector4
from field.math.linalg import VectorN
from field.math.linalg import Quaternion
from field.math.abstraction import iFloatProvider
from field.math.abstraction import iDoubleProvider
from field.math.abstraction import iFilter
from field.math.abstraction import iTemporalFunction
from field.core.dispatch import iVisualElement
from field.core.dispatch import Mixins
from field.core import StandardFluidSheet

from java.lang import System
from field.core.plugins.python import OutputInsertsOnSheet
from field.core.execution import TemporalSliderOverrides
from field.core.plugins.log import SimpleExtendedAssignment
from field.core.plugins.log import *
from field.math.abstraction import *
from field.math.linalg import *
from field.core.ui import PresentationParameters

import sys
import types


_self = None

def setSelf(value):
	global self
	self = value

def getSelf():
	global _self
	return _self

class attribDict:
	def __init__(self, f, t):
		self.__dict__["local"] = {}
		self.__dict__["f"] = f
		self.__dict__["t"] = t
		pass

        def __getattr__(self, name):
                if (name=="__completions__"):
                        nn = PythonPlugin.listAttr(self.__dict__["f"], self.__dict__["t"])
                        rr = []
                        for n in nn:
                                rr.append(n)
                        return rr

                rr = PythonPlugin.getAttr(self.__dict__["f"], self.__dict__["t"], name)
		return rr

	def __setattr__(self, name, value):
		PythonPlugin.setAttr(self.__dict__["f"], self.__dict__["t"], name, value)

	def completions(self):
		return self.__dict__["local"]


def findByName(reg):
	return StandardFluidSheet.findVisualElementWithName(getSelf(), reg)




class transp(iFloatProvider):
	def __init__(self, expression, *inputs):
		self.expression = expression
		self.inputs = inputs

	def evaluate(self):
		return self.expression(*self.inputs)

	def __call__(self):
		return self.expression(*self.inputs)

	def evaluateObject(self, x):
		if (isinstance(x, (int, float, long, Vector2, Vector3, Vector4))):
			return x
		if (isinstance(x, iHasScalar)):
			return x.getDoubleValue()
		elif (callable(x)):
			return x()
		elif (isinstance(x, iFloatProvider)):
			return x.evaluate()
		else:
			print "unknown type, deep within transp.evaluateObject %s " %x

	def __add__(self, right):
		return transp(lambda x,y : x.evaluate()+self.evaluateObject(y), self, right)

	def __radd__(self, right):
		return transp(lambda x,y : x.evaluate()+self.evaluateObject(y), self, right)

	def __sub__(self, right):
		return transp(lambda x,y : x.evaluate()-self.evaluateObject(y), self, right)

	def __rsub__(self, right):
		return transp(lambda x,y : -x.evaluate()+self.evaluateObject(y), self, right)

	def __mul__(self, right):
		return transp(lambda x,y : x.evaluate()*self.evaluateObject(y), self, right)

	def __rmul__(self, right):
		return transp(lambda x,y : x.evaluate()*self.evaluateObject(y), self, right)

	def __div__(self, right):
		return transp(lambda x,y : x.evaluate()/self.evaluateObject(y), self, right)

	def __rdiv__(self, right):
		return transp(lambda x,y : self.evaluateObject(y)/x.evaluate(), self, right)

	def __str__(self):
		return "%s"%self.expression(*self.inputs)

	def __repr__(self):
		return "transp: %s <- %s " % (self.expression, self.inputs)

	def __int__(self):
		return int(self.evaluate())

	def __float__(self):
		return float(self.evaluate())

	def __getattr__(self, at):
		if (not at.startswith("_")):
			return getattr(self.evaluate(),at)
		raise AttributeError, at

	def __tojava__(self, c):		
		return self.evaluate()



class transpFilter(iFilter):
	def __init__(self, lam, pref=None, *args):
		self.lam = lam
		self.pref = pref or (lambda x : x)
		self.args = args
		self.tot = []

	def filter(self, t):
		return self.lam(self.pref(t), *self.args)

	def __getattr__(self, a):
		if (a=="input"):
			return transpFilter(lambda x : self.pref(x))
		else:
			raise AttributeError,a

	def prefix(self, pp):
		sf = transpFilter(self.lam, lambda t : self.pref(pp(t)), *self.args)
		sf.tot = []+self.tot
		return sf
	
	def postfix(self, mm):
		sf = transpFilter(lambda t, *a : mm(t, self, *a), self.pref, *self.args)
		sf.tot = []+self.tot
		return sf

	def apply(self, x, param):
		if (isinstance(x, (int, float, long, Vector2, Vector3, Vector4, transp))):
			return x
		elif (callable(x)):
			return x(param)
		elif (isinstance(x, iFloatProvider)):
			return x.evaluate()
		elif (isinstance(x, iFilter)):
			return x.filter(param)
		else:
			print "unknown type, deep within transpFilter.evaluateObject %s " %x

	def __add__(self, right):
		return self.postfix(lambda t, x, *a : x.filter(t)+self.apply(right,t))

	def __sub__(self, right):
		return self.postfix(lambda t, x, *a : x.filter(t)-self.apply(right,t))

	def __mul__(self, right):
		return self.postfix(lambda t, x, *a : x.filter(t)*self.apply(right,t))

	def __div__(self, right):
		return self.postfix(lambda t, x, *a : x.filter(t)/self.apply(right,t))

	def __radd__(self, right):
		return self.postfix(lambda t, x, *a : x.filter(t)+self.apply(right,t))

	def __rsub__(self, right):
		return self.postfix(lambda t, x, *a : -x.filter(t)+self.apply(right,t))

	def __rmul__(self, right):
		return self.postfix(lambda t, x, *a : x.filter(t)*self.apply(right,t))

	def __rdiv__(self, right):
		return self.postfix(lambda t, x, *a : x.filter(t)/self.apply(right,t))

	def __lshift__(self, right):
		if (isinstance(right, (int, float, long, Vector2, Vector3, Vector4, transp))):
			return self.prefix(lambda t : t+right)
		elif (isinstance(right, transpFilter)):
			return self.prefix(lambda t : right.filter(t))
		elif (callable(right)):
			return self.prefix(lambda t : right(t))
		elif (isinstance(right, iFilter)):
			return self.prefix(lambda t : right.filter(t))
		else:
			print "unknown type, within transpFilter.lshift %s " %right
	
	def __call__(self, param):
		return self.filter(param)


def trans(x):
	if (isinstance(x, (int, float, long, Vector2, Vector3, Vector4))):
		return transp(lambda y : x, 0)
	if (isinstance(x, iHasScalar)):
		return transp(lambda y : x.getDoubleValue(), 0)
	if (isinstance(x, iFloatProvider)):
		return transp(lambda x : x.evaluate(), x)
	if (isinstance(x, iProvider)):
		return transp(lambda x : x.get(), x)
	if (callable(x)):
		return transp(lambda y : x(), 0)
	return None

def transFilter(x):
	if (isinstance(x, (int, float, long, Vector2, Vector3, Vector4))):
		return transpFilter(lambda y : x)
	if (isinstance(x, iFloatProvider)):
		return transpFilter(lambda y : x.evaluate())
	if (callable(x)):
		return transpFilter(lambda y : x(y))
	if (isinstance(x, iTemporalFunction)):
		return transpFilter(lambda y : x.get(y))
	return None



from field.namespace.diagram import Channel

class NoteMaker:
	def __init__(self, channel):
		self.pitch=60
		self.time=0
		self.duration=1
		self.channel = channel
		self.velocity = 0.5
		self.utils = NoteChannelUtils()

	def next(self, **options):
		self.readOptions(options)
		ret = self.utils.newNote(self.channel, self.pitch, self.time, self.duration)
		self.time += self.duration
		ret.getPayload().velocity=self.velocity
		return ret

	def readOptions(self, options):
		if (options.has_key("pitch")): self.pitch = options["pitch"]
		if (options.has_key("time")): self.time = options["time"]
		if (options.has_key("duration")): self.duration = options["duration"]
		if (options.has_key("channel")): self.channel = options["channel"]
		if (options.has_key("velocity")): self.velocity = options["velocity"]


def getTimeSystem():
	return attribDict(getSelf(), getSelf()).currentTimeSystem_
#	return _a.currentTimeSystem_;


def getElementFromName(name):
	if (isinstance(name, types.StringType)):
		return getSelf().find[name][0]
	return name

class timeSystem_control:
	def __init__(self):
		pass

	def __getattr__(self, name):
		if (name=="value"):
			return getTimeSystem().getExecutionTime()
		return None

	def __setattr__(self, name, value):
		if (name=="value"):
			getTimeSystem().supplyTimeManipulation(0, value-self.value)
		elif (name=="wait"):
			getTimeSystem().supplyTimeManipulation(value,0)
		elif (name=="line"):
			getTimeSystem().supplyTimeManipulation(value[0], value[1]-self.value)
		elif (name=="end"):
			getTimeSystem().supplyTimeManipulation(value, getSelf().getFrame(None).x+getSelf().getFrame(None).w+1-self.value)
		elif (name=="start"):
			getTimeSystem().supplyTimeManipulation(value, getSelf().getFrame(None).x+1-self.value)
		elif (name=="endOf"):
			if (isinstance(value, types.ListType)):				
				getTimeSystem().supplyTimeManipulation(value[0], getElementFromName(value[1]).getFrame(None).x+getElementFromName(value[1]).getFrame(None).w-self.value+1)
			else:
				getTimeSystem().supplyTimeManipulation(0, getElementfromName(value).getFrame(None).x+value.getFrame(None).w-self.value+1)
		elif (name=="startOf"):
			if (isinstance(value, types.ListType)):
				getTimeSystem().supplyTimeManipulation(value[0], getElementFromName(value[1]).getFrame(None).x-self.value+1)
			else:
				getTimeSystem().supplyTimeManipulation(0, getElementFromName(value).getFrame(None).x-self.value+1)
		elif (name=="pendOf"):
			if (isinstance(value, types.ListType)):
				getTimeSystem().supplyTimeManipulation(value[0], getElementFromName(value[1]).getFrame(None).x+getElementFromName(value[1]).getFrame(None).w-getSelf().getFrame(None).x+1)
			else:
				getTimeSystem().supplyTimeManipulation(0, getElementFromName(value).getFrame(None).x+getElementFromName(value).getFrame(None).w-getSelf().getFrame(None).x+1)

	def __repr__(self):
		return "timeSystem_control:%f" % getTimeSystem().getExecutionTime()

	def __str__(self):
		return "timeSystem_control:%f" % getTimeSystem().getExecutionTime()

	def __eq__(self, other):
		return 0

	def __hash__(self):
		return 1

global _now
_now = timeSystem_control()

from field.core.plugins.log import InvocationLogging
#l = InvocationLogging.logging

class KeyframeGroupOverrideHelper:
	def __init__(self, visualElement, gg, logTo):
		self.__dict__["logTo"] = logTo;
		self.__dict__["gg"] = gg;


	def __getattr__(self, name):
		if (name=="__repr__"):
			PythonUtils.printStackTraceNow()
		return pxy(self.gg[name], name, 1, self.logTo)

	def __setattr__(self, name, value):
		p = pxy(self.gg, name, 1, self.logTo)
		p.__setattr__(name, value)



class pxy:
	def __init__(self, at, name, isroot , logTo):
		self.__dict__["_at"]=at
		self.__dict__["_name"]=name
		self.__dict__["_isroot"]=isroot
		self.__dict__["_l"]=logTo

	def __getattr__(self, name):
		value = self._l.linkGetAttr(self._at, name, self._isroot, self._name, self)
		p = pxy(value,"%s.%s" % (self._name, name), 0, self._l)
		self._l.linkGetAttrFinish(self, p)
		return p

	def __setattr__(self, name, value):
		#print "inside setattr %s, %s " % (self._l, type(self._l))
		self._l.linkSetAttr(self._at, name, value, self._isroot, self._name, self)

	def __call__(self, *args):
		result = self._at(*args)
		self._l.linkCall(self._at, args, self._name, self._isroot, result, self)
		p= pxy(result, "%s(%s)"% (self._name, args), 0, self._l)
		self._l.linkCallFinish(self, p)
		return p

	def __len__(self):
		return len(self._at)

	def __iter__(self):
		a = self._at
		def wrapIter():
			for n in range(0, len(a)):
				yield self.__getitem__(n)
		return wrapIter()

	def __getitem__(self, a):
		# this is incorrect for now
		return pxy(self._at[a], "%s[%s]"%(self._name, a), 0, self._l)

	def __repr__(self):
		return "%s" % self._at.toString()

	def __str__(self):
		return "%s" % self._at



"""function lam over a range, returns min, minAt, max, maxAt"""
def boundsOf(lam, range):
	from java.lang import Float
	min = Float.POSITIVE_INFINITY
	max = Float.NEGATIVE_INFINITY
	maxIs = None
	minIs = None
	for x in range:
		q = lam(x)
		if (q>max):
			max = q
			maxIs = x
		if (q<min):
			min = q
			minIs = x

	return (min, minIs, max, maxIs)


slicetype = type(slice(0,0,0))

def issequence(n):
	try: it = iter(n)
	except TypeError: return 0
	return 1


class rotation(object):
	def __init__(self, angle, around=None):
                self.around = around
                self.angle = angle

def aroundBound(x=0.5, y=0.5):
	def around(line):
                b = line.bounds()
                #print "bounds are %s " % b                                                                                                                  
                a = b.convertFromNDC(Vector3(x,y,0))
                #print "  aroudn is %s " % a                                                                                                                 
                return a
        return around

class scale(object):
        def __init__(self, amount, around=aroundBound()):
                self.amount = amount
                self.around = around


def vector234_sequenceAccess_set(self, index, value):
	if (isinstance(index, slicetype)):
		for n in range(index.start or 0, index.stop or min(len(value), len(self)), index.step or 1):
			self.setItem(n, value[n % len(value)])
	else:
		self.setItem(index,value)

def vector234_sequenceAccess_get(self, index):
	if (isinstance(index, slicetype)):
		re = []
		for n in range(index.start or 0 , index.stop or len(self), index.step or 1):
			re.append(self.getItem(n))
		return re
	elif (index>=0 and index<len(self)):
		return self.getItem(index)
	else:
		raise IndexError()


Vector2.__setitem__ = vector234_sequenceAccess_set
Vector3.__setitem__ = vector234_sequenceAccess_set
Vector4.__setitem__ = vector234_sequenceAccess_set
Quaternion.__setitem__ = vector234_sequenceAccess_set
Vector2.__getitem__ = vector234_sequenceAccess_get
Vector3.__getitem__ = vector234_sequenceAccess_get
Vector4.__getitem__ = vector234_sequenceAccess_get
Quaternion.__getitem__ = vector234_sequenceAccess_get


def vector234_imul(self, other):
	if (isinstance(other, (Vector2, Vector3, Vector4, list, tuple))):
		for n in range(0, min(len(self), len(other))):
			self[n]*=other[n]
	elif (isinstance(other,CoordinateFrame)):
		other.transformPosition(self)
	elif (isinstance(other, Quaternion)):
		other.rotateVector(self)
	elif (isinstance(other, rotation)):
		if (other.around):
			self-=other.around
		if (isinstance(other.angle, Quaternion)):
			self*=other.angle
		else:
			self*=Quaternion().set(Vector3(0,0,1), other.angle)
		if (other.around):
			self+=other.around
	else:
		for n in range(0, len(self)):
			self[n]*=other
	return self

def vector234_iadd(self, other):
	if (isinstance(other, (Vector2, Vector3, Vector4, list, tuple))):
		for n in range(0, min(len(self), len(other))):
			self[n]+=other[n]
	else:
		for n in range(0, len(self)):
			self[n]+=other
	return self

def vector234_isub(self, other):
	if (isinstance(other, (Vector2, Vector3, Vector4, list, tuple))):
		for n in range(0, min(len(self), len(other))):
			self[n]-=other[n]
	else:
		for n in range(0, len(self)):
			self[n]-=other
	return self

def vector234_idiv(self, other):
	if (isinstance(other, (Vector2, Vector3, Vector4, list, tuple))):
		for n in range(0, min(len(self), len(other))):
			self[n]/=other[n]
	elif (isinstance(other, Quaternion)):
		other = other.inverse()
		other.rotateVector(self)
		other = other.inverse()
	elif (isinstance(other, rotation)):
		if (other.around):
			self-=other.around
		if (isinstance(other.angle, Quaternion)):
			self/=other.angle
		else:
			self*=Quaternion().set(Vector3(0,0,1), -other.angle)
		if (other.around):
			self+=other.around
	else:
		for n in range(0, len(self)):
			self[n]/=other
	return self

def vector234_add(ty):
	def _op(self, other):
		ret = ty(self)
		ret+=other
		return ret
	return _op
def vector234_mul(ty):
	def _op(self, other):
		ret = ty(self)
		ret*=other
		return ret
	return _op
def vector234_sub(ty):
	def _op(self, other):
		ret = ty(self)
		ret-=other
		return ret
	return _op
def vector234_div(ty):
	def _op(self, other):
		ret = ty(self)
		ret/=other
		return ret
	return _op

Vector2.__imul__ = vector234_imul
Vector3.__imul__ = vector234_imul
Vector4.__imul__ = vector234_imul

Vector2.__iadd__ = vector234_iadd
Vector3.__iadd__ = vector234_iadd
Vector4.__iadd__ = vector234_iadd

Vector2.__isub__ = vector234_isub
Vector3.__isub__ = vector234_isub
Vector4.__isub__ = vector234_isub

Vector2.__idiv__ = vector234_idiv
Vector3.__idiv__ = vector234_idiv
Vector4.__idiv__ = vector234_idiv


Vector2.__add__ = vector234_add(Vector2)
Vector3.__add__ = vector234_add(Vector3)
Vector4.__add__ = vector234_add(Vector4)
Vector2.__sub__ = vector234_sub(Vector2)
Vector3.__sub__ = vector234_sub(Vector3)
Vector4.__sub__ = vector234_sub(Vector4)
Vector2.__div__ = vector234_div(Vector2)
Vector3.__div__ = vector234_div(Vector3)
Vector4.__div__ = vector234_div(Vector4)
Vector2.__mul__ = vector234_mul(Vector2)
Vector3.__mul__ = vector234_mul(Vector3)
Vector4.__mul__ = vector234_mul(Vector4)

Vector2.__radd__ = vector234_add(Vector2)
Vector3.__radd__ = vector234_add(Vector3)
Vector4.__radd__ = vector234_add(Vector4)
Vector2.__rsub__ = vector234_sub(Vector2)
Vector3.__rsub__ = vector234_sub(Vector3)
Vector4.__rsub__ = vector234_sub(Vector4)
Vector2.__rdiv__ = vector234_div(Vector2)
Vector3.__rdiv__ = vector234_div(Vector3)
Vector4.__rdiv__ = vector234_div(Vector4)
Vector2.__rmul__ = vector234_mul(Vector2)
Vector3.__rmul__ = vector234_mul(Vector3)
Vector4.__rmul__ = vector234_mul(Vector4)




class OKeyByName_Python(OKeyByName):
	def __init__(self, name, defa):
		OKeyByName.__init__(self, name, defa)
	def __getattr__(self, name):
		return getattr(self.get(), name)


from field.core.plugins.log import AssemblingLogging
from field.core.plugins.log.AssemblingLogging import PartiallyEvaluatedFunction
from java.util import *
#import sys


def middleBlender(sharpness, value, depth=1):
	def b(blendSupport, values, weights, newValue):
		m = depth*(1-Math.pow(Math.abs(max(weights)-0.5)*2, sharpness))
		weights = [1-m, m]
		values = [newValue, value]
		return blendSupport.blend(values, weights)
	return b


class StoredKeyFrame:
	def __init__(self, changeSet, weight, cb={}):
		self.changeSet = changeSet
		self.weight = weight
		self.customBlenders = cb
	
	def addBlender(self, name, b):
		self.customBlenders[name] = b


	def apply(self, amount=1):
		mm = LinkedHashMap()
		for x in self.changeSet:
			mm.put(System.identityHashCode(x),x)
		total = KeyframeGroupOverride.getAllChanges(mm)
		for change in total.entrySet():
			v = change.getValue()
			bs = v.getBlendSupport(AssemblingLogging())
			values = []

			for c in v.changes.values():
				if (isinstance(c.value, PartiallyEvaluatedFunction)):
					values.append(c.value.call())
				else:
					values.append(c.value)


			nv = bs.blend(values, self.weight)

			if (self.customBlenders.has_key(change.getKey())):
				nv = self.customBlenders[change.getKey()](bs, values, self.weight, nv)
			
			if (amount!=1):
				v.write(nv, amount)
			else:
				v.write(nv)



	def __add__(self, otherStoredKeyFrame):
		cs = [x for x in otherStoredKeyFrame.changeSet]
		cs += self.changeSet
		w = [x for x in otherStoredKeyFrame.weight]
		w += self.weight
		cb = {}
		cb.update(self.customBlenders)
		cb.update(otherStoredKeyFrame.customBlenders)
		return StoredKeyFrame(cs, w, cb)

	def __sub__(self, otherStoredKeyFrame):
		cs = [x for x in otherStoredKeyFrame.changeSet]
		cs += self.changeSet
		w = [-x for x in otherStoredKeyFrame.weight]
		w+= self.weight
		cb = {}
		cb.update(self.customBlenders)
		cb.update(otherStoredKeyFrame.customBlenders)
		return StoredKeyFrame(cs, w, cb)

	def __mul__(self, by):
		cs = [x for x in self.changeSet]
		w = [x*by for x in self.weight]
		return StoredKeyFrame(cs, w, self.customBlenders)

	def __div__(self, by):
		cs = [x for x in self.changeSet]
		w = [x/by for x in self.weight]
		return StoredKeyFrame(cs, w, self.customBlenders)

class KeyFrameCreator:
	def __init__(self): pass
	
	def begin(self, g=None):
		if (not g):
			g = sys._getframe(1).f_globals
		self.al = AssemblingLogging()
		_k = KeyframeGroupOverrideHelper(_self, g, self.al)
		return _k

	def end(self, _k):
		cs = _k.__dict__["logTo"].getSimpleChangeSet(_k.__dict__["logTo"].resetReturningMoves())
		return StoredKeyFrame([cs], [1.0])


	
from field.core.plugins.python import OutputInsertsOnSheet



def sheetCombo(name, initialValue=None, update=None, below=0):
	""" 'prints' a combo box to the sheet.

	takes four parameters: 
	-> a name for this UI element
	-> an optional 'initial value" which is a tuple such as ( ["a", "b", "c"], 0 ), where ["a", "b", "c"] are the menu items and 0 is the index of the default selection
	-> an optional 'update' function that's called when a new selection occurs
	-> whether this element should be positioned 'below' the box or to the right (the default)

	this will return an object that can be used to query and set the selected item.

	Typical usage pattern is:
	myVariable = sheetCombo("myVariable", ( ["A", "B", "C"], 0)).value()

	Note that the selected item is persisted accross restarts of Field and is stored in the sheet itself.
	"""

	r = OutputInsertsOnSheet.printCombo(name, getSelf(), update, below)
	if (OutputInsertsOnSheet.lastWasNew and initialValue):
		r.setValues(initialValue[0], initialValue[1])
	return r


def sheetSlider(name, initialValue=None, update=None, below=0):
	""" 'prints' a slider to the sheet.

	takes four parameters:
	-> a name for this UI element
	-> an optional 'initial value" which is a number between 0 and 1.0
	-> an optional 'update' function that's called when change happens
	-> whether this element should be positioned 'below' the box or to the right (the default)
	"""

	r = OutputInsertsOnSheet.printSlider(name, getSelf(), update, below)
	if (OutputInsertsOnSheet.lastWasNew and initialValue):
		r.set(initialValue)
	return r

def sheetLazy(name, initialValue=None, below=0):
	""" 'prints' a lazy-box to the sheet.

	takes four parameters:
	-> a name for this UI element
	-> an optional 'initial value" which is a number between 0 and 1.0
	-> whether this element should be positioned 'below' the box or to the right (the default)
	"""

	r = OutputInsertsOnSheet.printLazy(name, getSelf(), below)
	if (OutputInsertsOnSheet.lastWasNew and initialValue):
		r.set(initialValue)
	return r

def sheetXY(name, initialValue=None, update=None, below=0):
	""" 'prints' a slider to the sheet.

	takes four parameters:
	-> a name for this UI element
	-> an optional 'initial value" which is a Vector2(x,y) with components between 0 and 1
	-> an optional 'update' function that's called when change happens
	-> whether this element should be positioned 'below' the box or to the right (the default)
	"""
	r=OutputInsertsOnSheet.printXYSlider(name, getSelf(), update, below)
	if (OutputInsertsOnSheet.lastWasNew and initialValue):
		r.set(initialValue)
	return r

def sheetGraph(name, update=None, below=0):
	""" 'prints' a curve editor to the sheet --- use sheetGraph2, it's better"""	
	return OutputInsertsOnSheet.printCurve(name, getSelf(), update, below)

def sheetGraph2(name, update=None, below=0):
	""" 'prints' a slider to the sheet.

	takes three parameters:
	-> a name for this UI element
	-> an optional 'update' function that's called when change happens
	-> whether this element should be positioned 'below' the box or to the right (the default)
	"""
	return OutputInsertsOnSheet.printCurve2(name, getSelf(), update, below)

def sheetDeleteUI(name):
	""" delete's a sheet UI element """
	getSelf().outputInsertsOnSheet_.delete(getSelf(), name)


def sheetProperty(name, callback=None):
	OutputInsertsOnSheet.printProperty(getSelf(), name, callback)
	try:
		p = StandardFluidSheet.findVisualElementWithName(getSelf().root, getSelf().outputInsertsOnSheet_knownComponents["_property_%s"%name])
		cc = p.outputInsertsOnSheet_providedComponent.component
	

		@muchLater(20)
		def validate(cc, p):
			cc.validate()
			cc.doLayout()

			for c in cc.getComponents():
				c.doLayout()
			p.dirty=1

		validate(cc, p)
	except:
		pass

from field.core.plugins.python import OutputInserts

def printButton(name, toCall, width=100):
	OutputInserts.printButton(name, getSelf(), width, toCall)


def ExtendedAssignment():
	return KeyframeGroupOverrideHelper(_self, sys._getframe(1).f_globals, SimpleExtendedAssignment())

def LocalExtendedAssignment():
	return KeyframeGroupOverrideHelper(_self, sys._getframe(1).f_locals, SimpleExtendedAssignment())


from field.namespace.generic.Generics import Triple

def tripleIter(self):
	ll = [self.left, self.middle, self.right]
	return ll.__iter__()

Triple.__iter__ = tripleIter

from field.namespace.generic.Generics import Pair

def tripleIter(self):
	ll = [self.left, self.right]
	return ll.__iter__()

Pair.__iter__ = tripleIter


from field.core.windowing import GLComponentWindow
from field.core.plugins import PythonOverridden

def getOver(inside):
	"""Upgrades the visual element 'inside' to include the ability to have graph dispatch methods overridden in Python"""
	return Mixins().mixInOverride(PythonOverridden, inside)

def paintFLineNow(line):
	"""Sends a PLine to the renderer (you can only do this inside paint methods)"""
	GLComponentWindow.currentContext.submitLine(line, line.getProperties())

def overridePaint(callback):
	"""(Decorator) Add a paint function to this override.
	For example:
	@overridePaint
	def customPaint(source, bounds, visible):
	    pass
	This function will now be called to paint this visual element and all visual elements below it in the dispatch graph"""
	getOver(getSelf()).add("paintNow", callback)
	return callback

def overrideDeleted(callback):
	"""(Decorator) Add a deletion notification function to this override.
	For example:
	@overrideDeleted
	def customDelete(source):
	    pass
	This function will now be called when this element or any element below it in the dispatch graph is deleted"""
	getOver(getSelf()).add("deleted", callback)
	return callback

def overrideAdded(callback):
	"""(Decorator) Add an added notification function to this override.
	For example:
	@overrideAdded
	def customAdded(source):
	    pass
	This function will now be called when this element or any element below it in the dispatch graph is added to the graph"""
	getOver(getSelf()).add("added", callback)
	return callback


def overrideBegin(callback):
	"""(Decorator) Add an execution begin notification function to this override.
	For example:
	@overrideBegin
	def customBegin(source):
	    pass
	This function will now be called when this element or any element below it in the dispatch graph is option-clicked or _element.begin()"""
	getOver(getSelf()).add("beginExecution", callback)
	return callback

def overrideEnd(callback):
	"""(Decorator) Add an execution end notification function to this override.
	For example:
	@overrideEnd
	def customEnd(source):
	    pass
	This function will now be called when this element or any element below it in the dispatch graph is finished executing or _element.end()"""
	getOver(getSelf()).add("endExecution", callback)
	return callback

def overrideGetProperty(callback):
	"""(Decorator) Add a property get function to this override.
	For example:
	@overrideGetProperty
	def customGetProperty(source, property, result):
	    pass
	This function will now be called when this element or any element below it in the dispatch graph needs to get a property. You can set the result using result.set(value) and monitor other properties with result.get()"""
	getOver(getSelf()).add("getProperty", callback)
	return callback

def overrideSetProperty(callback):
	"""(Decorator) Add a property set function to this override.
	For example:
	@overrideSetProperty
	def customSetProperty(source, property, result):
	    pass
	This function will now be called when this element or any element below it in the dispatch graph needs to set a property. You can set the result using result.set(value) and monitor other properties with result.get()"""
	getOver(getSelf()).add("setProperty", callback)
	return callback

def overrideShouldSetFrame(callback):
	"""(Decorator) Add a property set function to this override.
	For example:
	@overrideShouldSetFrame
	def customShouldSetFrame(source, oldRect, newRect, willSet):
	    pass
	This function will now get called when this element or any element below it in the dispatch graph changes it's position and size
	"""
	getOver(getSelf()).add("shouldChangeFrame", callback)



def later(call):
	"""(Decorator) Defer calling this function.

	When you call this function it doesn't get called immediately, but at the end of this update cycle"""

	def cc(*args):
		def doCall():
			call(*args)
		PythonUtils().callLater(doCall)

	return cc

class muchLater(object):
	def __init__(self, n):
		self.n = n
	def __call__(self, call):
		def cc(*args):
			def doCall():
				call(*args)
			PythonUtils().callLater(doCall, self.n)

		return cc


def onSelection(f):
	"""(Decorator) Call this function on element select / deselect

	for example:
	@onSelection
	def myFunction(element, wasSelected):
		pass
	"""
	x = getSelf().selectionStateCallback_ =  getSelf().selectionStateCallback_ or PythonCallableMap()
	@x
	def call(*args):
		f(*args)
	return call

import tokenize
import token

def __tokenizeHelper(string):
	lines = string.split("\n")
	def getLine():
		while (len(lines)>0):
			x = lines[0]
			del lines[0]
			if (x!=""):
				return x
		return ""

	ret = []
	for n in tokenize.generate_tokens(getLine):
		ret += [n]
	return ret


class dictAsClass(dict):
	def __getattr__(self, name):
		return self[name]
	def __setattr__(self, name, val):
		self[name] = val


def newSubTimeSlider(name, parent, position=None, predicate=None, exclusive=1):
    """Makes a new sub-time-slider for the box parent. 
        
        You can set the initial position, the set (predicate) of all boxes that this slider should be able to execute and whether this execution is exclusive or not. Note, you have to actually call the object returned by this function to have the slider execute things.
    """
    if (position==None): position = parent.frame.x
    if (predicate==None): 
        def x(ss=parent):
            return ss.subelements.values()
        predicate = x
    
    z = TemporalSliderOverrides.newLocalTemporalSliderFromFunction(name, parent, position, predicate)

    def update(z=z):
        z[1].update(z[0].frame.x)
    
    if (exclusive):
        for n in predicate():
            n.promiseExecution_ = z[1]
    
    return update

