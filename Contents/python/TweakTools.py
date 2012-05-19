




from field.core.plugins.drawing.tweak.Visitors import BaseFilter
from field.core.plugins.drawing.tweak.Visitors import PositionVisitor
from field.core.plugins.drawing.tweak.Visitors import NodeVisitor
from field.core.plugins.drawing.opengl.Polar import iPolarVisitor
from field.core.plugins.drawing.opengl.Polar import PolarMove
from field.core.plugins.drawing.opengl.Polar import PolarFilter
from field.math.linalg import CoordinateFrame
from field.core.plugins.drawing import FieldGraphics2D

from field.core.plugins.drawing import FieldGraphics2D2

from field.core.plugins.drawing.tweak.python import iCoordTransformation
from field.core.util import PythonCallableMap

from java.awt.geom import Ellipse2D

from field.math.linalg import Vector2
from field.math.linalg import Vector3

from field.util import Dict
from field.util.Dict import  Prop
from java.awt import BasicStroke
from java.awt.geom import Area
from field.core.dispatch.iVisualElement import Rect
from java.lang import Double

from field.core.plugins.drawing.opengl import Cursor
from field.core.plugins.drawing.opengl import CachedLine

from field.core.plugins.drawing.opengl import *
from field.core.plugins.drawing.tweak.python import *

from FluidTools import *

try:
	from org.apache.batik.swing.svg import GVTTreeBuilder
	from org.apache.batik.swing import *
	from org.apache.batik.bridge import *
except:
	print "((no batik support))"


import types
def flatten(l):
        """ flattens simple nested lists into a single list.                                                                                          
                                                                                                                                                      
        For example [[1,[2],3], [4], [5,6], [], [7]] becomes                                                                                          
        [1,2,3,4,5,6,7]                                                                                                                               
        """
        if isinstance(l,types.ListType):
                return sum(map(flatten,l),[])
        elif isinstance(l, Collection):
                return sum(map(flatten,l),[])
        else:
                return [l]

class floatRange:
	
	def __init__(self, start, stop, steps, inclusive=1):
		self.start = start
		self.stop = stop
		self.steps = steps
		def inside():
			if (steps==1):
				yield start
			else:
				if (inclusive):
					for n in range(0, steps):
						yield start+n*float((stop-start))/(steps-1)
				else:
					for n in range(0, steps):
						yield start+n*float((stop-start))/(steps)
		self.inner = inside()

	def __iter__(self):
		return self.inner


def baseTool(**args):

	c = args["coords"]
	
	cameraState = None
	if (args.has_key("camera")):
		cameraState = args["camera"]

	ax = attribDict(getSelf(), getSelf())
	
	if (cameraState):
		for n in ax.lines:	
			if (n.properties.context):
				if (hasattr(n.properties.context, "pushTransformState")):
					n.properties.context.pushTransformState(cameraState)
					break

	n = args["node"]
	selection = n.selectFrom(ax.lines)
	for cc in c:
		if (hasattr(cc, "setNodes")):
			cc.setNodes(selection)

	if (selection!=None):
		for p in selection:
			for cc in c:
				cc.transformNode(p[1], p[0])
	
	if (cameraState):
		for n in ax.lines:
			if (n.properties.context):
				if (hasattr(n.properties.context, "popTransformState")):
					n.properties.context.popTransformState()
					break

def aroundEvent(n=0):
	def around(line):
		b = LineUtils.getPointOnLine(line.line, n*(line.line.events.size()-1))
		return b
	return around


class first(list):
	def __init__(self, ll=[]):
		for x in ll:
			self.append(x)

	def __getattr__(self, at):
		try:
			q = list.__getattr__(at)
			if (q):
				return q
		except:
			for x in self:
				if (hasattr(x, at)):
					l = getattr(x, at)
					if (l): return l
		return None

	def __setattr__(self, att, at):
		[setattr(x, att, at) for x in self]
		
		def __call__(self, *args, **kwargs):
			for x in self:
				l = x(*args, **kwargs)
				if (l):
					return l
			return None

class wl(list):
	def __init__(self, ll=[], reduction=None):
		for x in ll:
			self.append(x)
		self.reduction=reduction

	def maybeReduce(self, a):
		if (self.reduction):
			return reduce(self.reduction, a)
		else:
			return wl(a)

	def __getattr__(self, at):
		if (at=="reduction"): return self.__dict__["reduction"]
		try:
			q = list.__getattr__(at)
			if (q):
				return q
		except:
			return self.maybeReduce([getattr(x, at) for x in self])

	def __setattr__(self, att, at):
		if (att=="reduction"):
			self.__dict__["reduction"]=at
			return
		return self.maybeReduce([setattr(x, att, at) for x in self])

	def __call__(self, *args, **kwargs):
		return self.maybeReduce([x(*args, **kwargs) for x in self])

	def __add__(self, a):
		return self.maybeReduce([x+a for x in self])
	def __mul__(self, a):
		return self.maybeReduce([x*a for x in self])
	def __sub__(self, a):
		return self.maybeReduce([x-a for x in self])
	def __div__(self, a):
		return self.maybeReduce([x/a for x in self])
	def __radd__(self, a):
		return self.maybeReduce([x+a for x in self])
	def __rmul__(self, a):
		return self.maybeReduce([x*a for x in self])
	def __rsub__(self, a):
		return self.maybeReduce([-x+a for x in self])
	def __rdiv__(self, a):
		return self.maybeReduce([a/x for x in self])

	def __lshift__(self, a):
		return self.maybeReduce([x << a for x in self])

	def __iadd__(self, a):
		for x in range(0, len(self)):
			self[x]+=a
		return self

	def __imul__(self, a):
		for x in range(0, len(self)):
			self[x]*=a
		return self

	def __isub__(self, a):
		for x in range(0, len(self)):
			self[x]-=a
		return self

	def __idiv__(self, a):
		for x in range(0, len(self)):
			self[x]/=a
		return self
	
	def __exit__(self, type, value, traceback):
		for n in range(0, len(self)):
			e = getattr(self[n], "__exit__")
			if (e):
				e(type,value,traceback)
		return 0

def maintainSubpath(name, selection, allLines):
	ax = attribDict(getSelf(), getSelf())
	line = selection.onLine
	if (line!=None):
		lineIndex = ax.lines.indexOf(line)
	found = None

	
	if (ax.elaboratedBy!=None):
		for x in ax.elaboratedBy:
			if (attribDict(x, x).name==name):
				found = x
				break
			
	if (found):
		print "found existing"
		ax.lines.set(lineIndex, None)
		print "setting %s to %s " % (found, line)
		attribDict(found, found).inputSpline_ = line
	else:
		print " making new "
		ax.lines.set(lineIndex, None)
		made = ax.overrides.createSubElaboration(name, line)

class ExtractPath(iCoordTransformation):
	def __init__(self, name):
		self.name = name

	def transformNode(self, amount, vertex):
		print "extract path will maintain %s %s" % (self.name, vertex)
		maintainSubpath(self.name, vertex, attribDict(getSelf(), getSelf()).lines)

			
class MapDict:
	def __init__(self, on):
		self.__dict__["on"]=on

	def __setattr__(self, att, to):
		on = self.__dict__["on"]
		r = on.put(Prop(att), to)
		return r
		
	def __getattr__(self, att):
		on = self.__dict__["on"]
		return on.get(Prop(att))
	

class PCursor(Cursor):
	def __init__(self, *x):
		Cursor.__init__(self, *x)

	def split(self):
		a,b = Cursor.split(self)
		return (PLine(a), PLine(b))


class __pv(PositionVisitor):
	def __init__(self, lam): 
		self.lam = lam
	def visitPosition(self, pos, part, inside):
		pp = self.lam(pos.x, pos.y)
		pos.x = pp[0]
		pos.y = pp[1]

class __pv2(iPolarVisitor):
	def __init__(self, f): self.f = f
	def beginSubpath(self, a,b): pass
	def visitPolarMove(self, move, inside):
		self.f(move)

class __nf(NodeVisitor):
	def __init__(self,f): self.f = f
	def visitNode(self, before, now, after, beforeIsCurve, afterIsCurve):
		self.f(before, now, after, beforeIsCurve, afterIsCurve)

from java.awt.geom.RoundRectangle2D import Double as RoundRect

class PLine(object):
	def __init__(self, theLine = None):
		if (theLine == None):
			self.__dict__["line"]= CachedLine()
		else:
			if (hasattr(theLine, "draw") and theLine.draw):
				self.__dict__["line"]= CachedLine()
				self.addDrawable(theLine)
			else:
				self.__dict__["line"]= theLine
		self.__dict__["fix"]=0
		self.__dict__["doauto"]=0
		

	def addDrawable(theLine, draw):
		print "calling add drawable on this object here %s " % draw
		g2 = FieldGraphics2D()
		draw.draw(g2)
		lines = g2.getGeometry()
		
		if (len(lines)==0): return
		if (len(lines)==1):
			if (len(theLine.__dict__["line"].events)==0):
				theLine.__dict__["line"]=lines[0]
			else:
				theLine.__dict__["line"].events.addAll(lines[0].events)
		else:
			for n in lines:
				theLine.__dict__["line"].events.addAll(n.events)
		return theLine

	def __call__(self, **kw):
		"""Assign properties to line.

		Use keyword arguments to assign properties to line. For example line(color=Color4(1,0,0,1), pointed=1)"""
		for k, v in kw.items():
			self.__setattr__(k,v)
		return self

	def doFixProperties(self):
		"""Don't automatically remap properties on transformation"""
		self.__dict__["fix"]=1
		return self
		
	def dontFixProperties(self):
		"""Automatically remap properties on transformation.

		This can be slow, but vertex properties are always preserved with this turned on. When turned off, some properties can be lost on transformations that change the number of nodes in the line."""
		self.__dict__["fix"]=0
		return self

	def rect(self, rr):
		"""Draw a rectangle.

		e.g .rect(Rect(10,10,20,30)) draws with top-left at (10,10) that is 20 wide and 30 tall."""
		self.moveTo(rr.x, rr.y).lineTo(rr.x+rr.w, rr.y).lineTo(rr.x+rr.w, rr.y+rr.h).lineTo(rr.x, rr.y+rr.h).lineTo(rr.x, rr.y)
		return self


	def roundRect(self, rect, radius=5):
		"""Draw a rounded rectangle.

		e.g. .roundedRect(Rect(10,10,20,20), radius=2) draws a rectangle with a "corner radius" of 2."""
		shape = RoundRect(rect.x, rect.y, rect.w, rect.h, radius, radius)
		self.appendShape(shape)
		return self


	def maybeFix(self, newLine):
		if (self.__dict__["fix"]):
			return LineUtils().fixProperties(newLine, self.__dict__["line"])		
		return newLine
		
	def subdivideAllAsCurves(self):
		"""Convert all to curves and split each curve into two."""
		self.__dict__["line"] = LineUtils().simpleSubdivideAllAsCurves(self.__dict__["line"])
		return self

	def getInput(self):
		return self.line.getInput()

	def breakNext(self):
		"""Break at the start of the next segment.

		The next .lineTo or .cubicTo will actually become a .moveTo()."""
		self.__dict__["reset"]=1
		return self

	def moveTo(self, *x):
		"""Move to point.

		Moves the 'pen' to point 'x' without drawing. For example .moveTo(10,20) or .moveTo(*Vector2(10,20))."""
		self.getInput().moveTo(x[0],x[1])
		self.__dict__["reset"]=0		
		return self

	def m(self, *x):
		"""see MoveTo"""
		return self.moveTo(*x)

	def moveBy(self, *x):
		"""Move by a relative offset (x, y) from the last destination,
		or do a moveTo() if this PLine is empty.
		"""
		cachedLine = self.line
		if len(cachedLine.events) == 0:
			return self.moveTo(*x)
		else:
			sx, sy = cachedLine.events[len(cachedLine.events) - 1].getDestination()
			dx, dy = x
			return self.moveTo(sx + dx, sy + dy)

	def lineTo(self, *x):
		"""Draws a straight line.
		
		Draws a straight line to point 'x'. For example .lineTo(10,20) or .lineTo(*Vector2(10,20))."""

		if (self.__dict__["line"].events.size()==0 or self.__dict__["reset"]==1):
			self.moveTo(x[0],x[1])
			self.__dict__["reset"]=0
			return self
		self.getInput().lineTo(x[0],x[1])
		return self

	def l(self, *x):
		"""see lineTo"""
		return self.lineTo(*x)

	def lineBy(self, *x):
		"""Draw a line by a relative offset (x, y) from the last destination,
		or do a lineTo() if this PLine is empty.
		"""
		cachedLine = self.line
		if len(cachedLine.events) == 0:
			return self.lineTo(*x)
		else:
			sx, sy = cachedLine.events[len(cachedLine.events) - 1].getDestination()
			dx, dy = x
			return self.lineTo(sx + dx, sy + dy)

	def cubicTo(self, c1x, c1y, c2x, c2y, x, y):
		"""Draws a curved segment.

		The curve moves to x,y, inflecting towards c1x,c1y then c2x,c2y.
		"""
		if (self.__dict__["line"].events.size()==0 or self.__dict__["reset"]==1):
			self.moveTo(x,y)
			self.__dict__["reset"]=0
			return self
		self.getInput().cubicTo(c1x, c1y, c2x, c2y, x, y)
		return self

	def c(self, *x):
		"""see cubicTo"""
		return self.cubicTo(*x)

	def circleTo(self, x1, y1, x2, y2, overShoot=0, limit=500):
		"""Appends an arc segment"""
		LineUtils.circleTo(self.__dict__["line"], Vector2(x1,y1), Vector2(x2, y2), limit, overShoot)
		return self

	def appendShape(self, shape):
		"""Appends a java.awt.Shape"""
		LineUtils().piToCachedLine(shape.getPathIterator(None), self, 1)
		return self

	def ellipse(self, rx, ry, x, y):
		"""Draws an ellipse."""
		return self.appendShape(Ellipse2D.Double(x-rx, y-ry, rx*2,ry*2))

	def circle(self, radius, x, y):
		"""Draws a circle."""
		return self.appendShape(Ellipse2D.Double(x-radius, y-radius, 2*radius, 2*radius))

	def line(self, x1,y1, x2, y2):
		"""Draw line segment.
		
		Draws a line segment from x1,y1 to x2,y2. Equivalent to .moveTo(x1, y1).lineTo(x2, y2)"""
		self.moveTo(x1,y1).lineTo(x2,y2)

	def relCubicTo(self, cx1, cy1, cx2, cy2, x, y):
		if (self.__dict__["line"].events.size()==0 or self.__dict__["reset"]==1):
			self.moveTo(x,y)
			self.__dict__["reset"]=0
			return self
		was = self.__dict__["line"].events[self.__dict__["line"].events.size()-1].getDestination(None)
		self.getInput().cubicTo(was.x+(x-was.x)/3.0+cx1, was.y+(y-was.y)/3.0+cy1, was.x+2*(x-was.x)/3.0+cx2, was.y+2*(y-was.y)/3.0+cy2, x, y)
		return self


	def polarCubicTo(self, a1, len1, a2, len2, x2, y2):
		if (self.__dict__["line"].events.size()==0 or self.__dict__["reset"]==1):
			self.moveTo(x,y)
			self.__dict__["reset"]=0
			return self
		was = self.__dict__["line"].events[self.__dict__["line"].events.size()-1].getDestination(None)
		d1 = Vector2(x2-was.x, y2-was.y).scale(len1/3.0).rotateBy(a1)
		d2 = Vector2(x2-was.x, y2-was.y).scale(-len2/3.0).rotateBy(a2)
		self.cubicTo(d1.x+was.x, d1.y+was.y, x2+d2.x, y2+d2.y, x2, y2)
		return self

	def close(self, x, y):
		self.getInput().close()
		return self

	def addCode(self, code):
		LateExecutingDrawing.addCode(self.line.events.get(self.line.events.size()-1), code)
		return self

	def noise(self, level):
		self.__dict__["line"]=self.maybeFix(LineUtils.noise(self.__dict__["line"], level))
		return self

	def __getattr__(self, att):
		if (att.startswith("__")): return object.__getattribute__(self,att)
		if (att=="line"): return self.__dict__["line"]
		if (att=="nodes"): return self.__dict__["line"].events
		if (att=="position"): return self.line.events.get(self.line.events.size()-1).getDestination(None)
		if (att=="current"): return self.line.events.get(self.line.events.size()-1)

		if (att.endswith("_v")):
			return getattr(self.line.events.get(self.line.events.size()-1).getAttributes(), att)
		else:
			return getattr(self.line.getProperties(), att)

		return None
	
	def __setattr__(self, att, to):
		if (att.startswith("__")): 
			return object.__setattr__(self, att, to)
		if (att=="position"):
			if (self.line.events.size()==0):
				return moveTo(to[0], to[1])
			else:
				return lineTo(to[0], to[1])
		else:
			if (att.endswith("_v")):
				return setattr(self.line.events.get(self.line.events.size()-1).getAttributes(), att, to)
			else:
				return setattr(self.line.getProperties(), att, to)      


	def __iadd__(self, translation):
		if (isinstance(translation, Vector2)):
			self.__dict__["line"] = (LineUtils().transformLine(self.line, Vector2(translation.x, translation.y), None, None, None))
		if (isinstance(translation, Vector3)):
			self.__dict__["line"] = (LineUtils().transformLine(self.line, Vector2(translation.x, translation.y), None, None, None))
		if (isinstance(translation, Quaternion)):
			self.__dict__["line"] = (LineUtils().transformLine(self.line, None, translation, None, None))
		if (isinstance(translation, rotation)):
			tx = None
			if (translation.around):
				try:
					tx = translation.around(self)*-1.0
					tx = Vector2(tx[0], tx[1])
				except:
					tx = Vector2(-translation.around[0], -translation.around[1])
			else:
				tx = self.bounds().midpoint2().scale(-1)
			self.__dict__["line"] = (LineUtils().transformLine(self.line, tx, None, Quaternion().set(Vector3(0,0,1), translation.angle), Vector2(tx).scale(-1)))
		if (isinstance(translation, CoordinateFrame)):
			self.__dict__["line"] = (LineUtils().transformLine3(self.line, translation));

		return self

	def __imul__(self, scaleBy):
		"""
		Scale this line.

		This method scales (in place) this line. The parameter 'scale' can be a Vector2 (or a Vector3) in which case this specifies a non-uniform scale around the midpoint of the bounding box of this line.
		The parameter 'scale' can also be a scale object, which gives an opportunity to specify a center. For example thisLine *= scale(Vector2(0.5, 0.5), around = centerFunction) scales this line by half around a center (a Vector2) given by the function 'centerFunction(line)'
		"""
		translation = scaleBy
		if (isinstance(translation, Vector2)):
			tx = self.bounds().midpoint2().scale(-1)
			self.__dict__["line"] = (LineUtils().transformLine(self.line, tx, Vector2(translation.x, translation.y), None, Vector2(tx).scale(-1)))
		if (isinstance(translation, scale)):
			if (translation.around):
				try:
					tx = translation.around(self)*-1.0
					tx = Vector2(tx[0], tx[1])
				except:
					tx = Vector2(-translation.around[0], -translation.around[1])
			else:
				tx = self.bounds().midpoint2().scale(-1)

			self.__dict__["line"] = (LineUtils().transformLine(self.line, Vector2(tx[0], tx[1]), Vector2(1,1) * translation.amount, None, Vector2(-tx[0], -tx[1])))
		if (isinstance(translation, Vector3)):
			tx = self.bounds().midpoint2().scale(-1)
			self.__dict__["line"] = (LineUtils().transformLine(self.line, tx, Vector2(translation.x, translation.y), None, Vector2(tx).scale(-1)))
		if (isinstance(translation, CoordinateFrame)):
			self.__dict__["line"] = (LineUtils().transformLine3(self.line, translation));

		return self
	
	def doAutoStroke(self):
		self.__dict__["doauto"]=1
		return self
		
	def dontAutoStroke(self):
		self.__dict__["doauto"]=0
		return self
		
	def autoStroke(self):
		if (not self.__dict__["doauto"]): return self.line
		if (self.line.events.size()==0): return self.line
		else:
			if (not self.line.events.get(self.line.events.size()-1).method.getName()=="close"): 
				t = 1
				t2 = self.line.getProperties().get(iLinearGraphicsContext.line_globalThickness)
				if (t2):
					t = t2
				return self.strokeWith(thickness=t).line
		return self.line
			
	def __iand__(self, intersectWith):
		if (isinstance(intersectWith, Area)):
			a1 = Area(LineUtils().lineToGeneralPath(self.autoStroke()))
			a1.intersect(intersectWith)
			self.__dict__["line"] = self.maybeFix(LineUtils().piToCachedLine(a1.getPathIterator(None)))
		return self

	def area(self):
		return Area(LineUtils().lineToGeneralPath(self.__dict__["line"]))

	def __isub__(self, intersectWith):
		if (isinstance(intersectWith, Area)):
			a1 = Area(LineUtils().lineToGeneralPath(self.autoStroke()))
			a1.subtract(intersectWith)
			self.__dict__["line"] = self.maybeFix(LineUtils().piToCachedLine(a1.getPathIterator(None)))
		return self
			
	def strokeWith(self, thickness=1, join=0, cap=0, miter=10, dashed=0, dash=None, phase=0):
		"""Apply a stroke style to this PLine.
		
		This method replaces the contents of this PLine with the line stroked with a particular style."""
		if (dash):
			stroke = BasicStroke(thickness, join, cap, miter, dash, phase)		
		else:
			stroke = BasicStroke(thickness, join, cap, miter)
		self.__dict__["line"] = self.maybeFix(LineUtils().lineAsStroked(self.__dict__["line"], stroke, 0))
		return self
		
	def segment(self):
		"""Returns a 'wrapped list' of this line split into pieces.

		Each piece starts with each .moveTo(...)"""
		return wl([PLine(x) for x in LineUtils().segmentSubpaths(self.line)])

	def bounds(self):
		"""Returns the Rect that this line fits in """
		bound = LineUtils().fastBounds(self.__dict__["line"])
		return Rect(bound[0].x,bound[0].y, bound[1].x-bound[0].x, bound[1].y-bound[0].y)

	def setBounds(self, bounds):
		"""Rescale this line to fit in the Rect 'bounds'"""
		bb = self.bounds()
		return self.visitPositions(lambda x,y : bounds.convertFromNDC(bb.convertToNDC(Vector3(x,y,0))))

	def visitPositions(self, lam):
		"""Filter this line.

		This method applies a function 'f' to each piece of positional data in this line. This function f(x,y) returns Vector2
		"""
		self.__dict__["line"] = self.maybeFix(BaseFilter().visitPositions(self.__dict__["line"], __pv(lam)))
		return self

	def visitNodes(self, f):
		"""Filter this line.

		This method applies a function 'f' to each node in this line. 

		f(before, now, after, beforeIsCurve, afterIsCurve) --- before, now, and after are Vector2's that this function can mutate. They might be None if you are at the start or end of a line segment. beforeIsCurve and afterIsCurve are 1 if before and after refer to curve segments.
		"""

		self.__dict__["line"] = self.maybeFix(BaseFilter().visitNodes(self.__dict__["line"], __nf(f)))
		return self


	def copy(self):
		"""Duplicate this line"""		
		return PLine( self.maybeFix(LineUtils().transformLine(self.__dict__["line"], None, None, None, None)))

	def resample(self, targetNum):
		"""Coarsely resample this line.

		This resamples this line into roughly 'targetNum' pieces that are roughly equally spaced. For more accurate subdivision use a .cursor(). """
		resampler = ConstantDistanceResampling(5, 8)
		self.__dict__["line"] = self.maybeFix( resampler.resample(targetNum, self.__dict__["line"]))
        
	def englass(self, echos):
		""" Overstroke this line with copies with varying thicknesses.

		The parameter echos is a list of (thickness, opacity) pairs and this function returns a 'wrapped list' of duplicates of this line with new opacity and thicknesses."""
		line = self.__dict__["line"]
		ret = []
	
		for parameters in echos:
			thickness = parameters[0]
			opacity = parameters[1]

			line2 = LineUtils().transformLine(line, None, None, None, None)
			line2.getProperties().putAll(line.getProperties())

			oldt = line.getProperties().get(line_globalThickness)
			if (oldt == None): oldt = 1
			newt = thickness(oldt)
			line2.getProperties().put(line_globalThickness, newt)

			oldc = line.getProperties().get(line_globalStrokeColor)
			if (oldc == None): oldc = line.getProperties().get(line_globalColor)
			if (oldc == None): oldc = Vector4(0,0,0,1)
		
			newo = opacity(oldc.w)
			line2.getProperties().put(line_globalStrokeColor, Vector4(oldc.x,oldc.y, oldc.z, newo))
		
			for e in line2.events:
				if (e.attributes):
					oldc = e.getAttributes().get(line_strokeColor)	
					if (oldc!=None):
						newo = opacity(oldc.w)
						e.getAttributes().put(line_strokeColor, Vector4(oldc.x,oldc.y, oldc.z, newo))
			ret.append(line2)
		
		return wl(ret)

	def __tojava__(self, ty):
       		return self.__dict__["line"]


	def __xor__(self, other):
		"""
		Returns a list of Vector2 points where this line intersects with another
		"""
		x = Intersections.intersectAndSubdivide([self.copy().line], [other.copy().line], 3)
		rr = []
		for xx in x:
			rr.append( xx[0].getAt())
		return rr

	def minima(self, position):
		"""
		Find minimal points on line.

		returns a list of Cursors that are at all the places where this line reaches points that are minima of the distance function between this line and 'position'
		"""
		ll = Cursor.cursorsFromMinimalApproach(self.line, Vector2(position[0], position[1]))
		return [PCursor(x) for x in ll]

	def visitPolar(self, f):
		"""
		Filters this line in polar space.
		
		Applies a function 'f' that filters PolarMove objects to this line. This can be useful to manipulate a line in local polar space, applying cumulative curves to the positions and control points of the line.
		"""
		pf = PolarFilter(self)
		result = pf.visitPolar(__pv2(f))
		self.__dict__["line"] = self.maybeFix(result)
		return self

	def closest(self, position):
		"""
		Obtain a Cursor at the point closest to 'position'
		
		Finds the point on this PLine that's closest to the point 'position' and returns a Cursor at that point
		"""
		return PCursor(Cursor.cursorFromClosestPoint(self.line, position))

	def cursor(self, at=0):
		"""Returns a 'cursor' object for this PLine.
		
		This cursor can be used to inspect points on the line and edit it's contents.
		The optional parameter 'at' sets the initial position in nodeNumber.t format. For example 4.25 means between node 4 and node 5, 25% of the way between them."""
		return PCursor(self.__dict__["line"], at)

	def intersections(self, otherLine):
		"""Intersects two lines.

                   Inserts nodes at all points in this line and 'otherLine' at the places where they cross, and returns a list of 2-tupes containing cursors for both lines at these points"""
		ret = Intersections.intersectAndSubdivide([self], [otherLine], 100)
		rr = []
		for r in ret:
			a,b = Intersections.resolve(r[0]), Intersections.resolve(r[1])
			h1 = PCursor(a.getContainer(), a.getContainer().events.indexOf(a) )
			h2 = PCursor(b.getContainer(), b.getContainer().events.indexOf(b) )
			rr.append( (h1,h2) )

		return rr

	def attachSurfaceLines(self, lineList):
		"""Attaches lines to this line to form a surface.

		(This function only works with the AdvancedGeometry plugin activated). This attaches the ordered list of lines 'lineList' to this line to form a NURBS surface across these lines.
		Automatically turns the attached lines into .nurbs=1 lines, and automatically sets this line to be a .nurbsSurface=1 line
		"""
		self.surfaceLines = ArrayList([x.line for x in lineList if (x!=self)])
		self.nurbsSurface = 1
		for x in lineList: x.nurbs=1



class PLine3(PLine):
	def __init__(self, theLine = None):
		PLine.__init__(self, theLine)
		self.containsDepth=1
		self.defaultDepth=None

	def setDefaultDepth(self, dd=0):
		self.defaultDepth = dd

	def getDefaultDepth(self):
		return self.defaultDepth or 0.0

	def moveTo(self, *x):
		if (len(x)==2):
			PLine.moveTo(self, *x)
			self.z_v = self.getDefaultDepth()
		else:
			PLine.moveTo(self, x[0], x[1])
			self.z_v = x[2]
		return self

	def lineTo(self, *x):
		if (len(x)==2):
			PLine.lineTo(self, *x)
			self.z_v = self.getDefaultDepth()
		else:
			PLine.lineTo(self, x[0], x[1])
			self.z_v = x[2]
		return self
	def cubicTo(self, c1x, c1y, c1z, c2x, c2y, c2z, x,y,z):
		PLine.cubicTo(self, c1x, c1y, c2x, c2y, x, y)
		self.z_v =  Vector3(c1z, c2z, z)
		return self

	def position3(self):
		p1 = self.position
		z = self.line.events.get(self.line.events.size()-1).z_v
		if (isinstance(z, Vector3)):
			return Vector3(p1.x, p1.y, z.z)
		else:
			return Vector3(p1.x, p1.y, z)

	def __iadd__(self, translation):
		if (isinstance(translation, Vector2)):
			self.__dict__["line"] = (LineUtils().transformLine(self.line, Vector2(translation.x, translation.y), None, None, None))
		if (isinstance(translation, Vector3)):
			self.__dict__["line"] = (LineUtils().transformLine3(self.line, translation, None, None, None))
		if (isinstance(translation, Quaternion)):
			self.__dict__["line"] = (LineUtils().transformLine3(self.line, None, translation, None, None))
		if (isinstance(translation, rotation)):
			tx = None
			if (translation.around):
				try:
					tx = translation.around(self)*-1.0
					if (len(tx)==2):
						tx = Vector3(tx[0], tx[1], self.averageDepth())
					else: 
						tx = Vector3(tx[0], tx[1], tx[2])
				except:
					if (len(translation.around)==2):
						tx = Vector3(translation.around[0], translation.around[1], self.averageDepth())
					else:
						tx = Vector3(translation.around[0], translation.around[1], translation.around[2])
			else:
				tx = self.bounds().midpoint2().scale(-1)
				tx = Vector3(tx[0], tx[1], -self.averageDepth())

			self.__dict__["line"] = (LineUtils().transformLine3(self.line, tx, None, Quaternion().set(Vector3(0,0,1), translation.angle), Vector3(tx).scale(-1)))
		if (isinstance(translation, CoordinateFrame)):
			self.__dict__["line"] = (LineUtils().transformLine3(self.line, translation));

		return self

	def __imul__(self, scaleBy):
		translation = scaleBy
		if (isinstance(translation, Vector2)):
			tx = self.bounds().midpoint2().scale(-1)
			self.__dict__["line"] = (LineUtils().transformLine(self.line, tx, Vector2(translation.x, translation.y), None, Vector2(tx).scale(-1)))
		if (isinstance(translation, scale)):
			if (translation.around):
				try:
					tx = translation.around(self)*-1.0
					if (len(tx)==2):
						tx = Vector3(tx[0], tx[1], self.averageDepth())
					else:
						tx = Vector3(tx[0], tx[1], tx[2])
				except:
					if (len(translation.around)==2):
						tx = Vector3(translation.around[0], translation.around[1], self.averageDepth())
					else:
						tx = Vector3(translation.around[0], translation.around[1], translation.around[2])
			else:
				tx = self.bounds().midpoint2().scale(-1)
				tx = Vector3(tx[0], tx[1], -self.averageDepth())
				self.__dict__["line"] = (LineUtils().transformLine3(self.line, tx, Vector3(1,1,1) * translation.amount, None, Vector3(-tx[0], -tx[1], -tx[2])))

		if (isinstance(translation, Vector3)):
			tx = self.bounds().midpoint2().scale(-1)
			tx = Vector3(tx[0], tx[1], -self.averageDepth())
			self.__dict__["line"] = (LineUtils().transformLine3(self.line, tx, translation, None, Vector3(tx).scale(-1)))
		if (isinstance(translation, CoordinateFrame)):
			self.__dict__["line"] = (LineUtils().transformLine3(self.line, translation));

		return self

	def averageDepth(self):
		tot =0
		totN = 0
		for n in self.line.events:
			q = n.z_v
			if (q != None):
				if (isinstance(q, Vector3)):
					tot=tot+q.x+q.y+q.z
					totN+=3.0
				else:
					tot+=q
					totN+=1.0
		if (totN==0.0): return 0.0
		return tot/totN

	def subdivideAllAsCurves(self):
                """Convert all to curves and split each curve into two."""
		self.__dict__["line"] = LineUtils().simpleSubdivideAllAsCurves3(self.__dict__["line"])
		return self




from java.io import File
from field.core.plugins.drawing.pdf import *

def makePDF(geometry = None, bounds = None, background = None, scale=1, filename=None, is3d=0):
	"""
	makePDF exports PLines to a PDF file.

	Every parameter is actually optional. 
	X{geometry} specifies a list of PLines to draw, if it's
	missing, then everything on the canvas gets exported (you can set .notForExport on a line to
	make sure it doesn't get exported). 

	X{bounds} specifies a Rect or a PLine that encloses all of the geometry 
	that gets drawn. It becomes the "paper size" of the PDF that's exported. If it's ommitted then the 
	bounds of the geometry that's going to get exported is used instead. One trick is to make a PLine 
	with a rect, then pass that PLine into here as the parameter bounds.

	X{background} is an optional background color. 

	X{filename} is an optional filename (a temp file is used if this is omitted)

	X{scale} increases the "dpi" of the canvas, making the paper and the drawing bigger (while keeping the line thickness the same).
	
	"""

	if (not geometry):
		geometry = sum([x.lines for x in getSelf().all if hasattr(x, "lines") and x.lines], [])
	else:
		try:
			geometry = sum(geometry, [])
		except:
			pass

	something = geometry

	pdfContext = BasePDFGraphicsContext()
	
	if (not is3d):
		ld = SimplePDFLineDrawing()
		ld.installInto(pdfContext)
		SimplePDFImageDrawing().installInto(pdfContext, ld)
		ctx = SimplePDFLineDrawing
	else:
		ld = SimplePDFLineDrawing_3d()
		ld.installInto(pdfContext)		
		#SimplePDFImageDrawing().installInto(pdfContext, ld)
		ctx = SimplePDFLineDrawing_3d

	if (bounds==None):
		for n in something:
			b = LineUtils().fastBounds(n)
			if (b):
				b = Rect(b[0].x,b[0].y, b[1].x-b[0].x, b[1].y-b[0].y)
				if (b.w>0 or b.h>0):
					print b
					bounds = Rect.union(b, bounds)
		if (not bounds):
			print "makePDF error: no bounds specified"
			return None

		bounds.x-=2
		bounds.y-=2
		bounds.w+=4
		bounds.h+=4

	if (isinstance(bounds, PLine)):
		bounds = bounds.bounds()

	if (isinstance(bounds, FLine)):
		bounds = bounds.bounds2()


	pdfContext.paperWidth=bounds.w*scale
	pdfContext.paperHeight=bounds.h*scale
	
	if (background):
		pdfContext.getGlobalProperties().paperColor = background

	ctx.outputTransform.z=(-bounds.x)*pdfContext.paperWidth/bounds.w
	ctx.outputTransform.w=(bounds.y+bounds.h)*pdfContext.paperHeight/bounds.h
	ctx.outputTransform.x=pdfContext.paperWidth/bounds.w
	ctx.outputTransform.y=-pdfContext.paperHeight/bounds.h

	if (not filename):
		name = File.createTempFile("field", ".pdf", None)
	else:
		name = File(filename)

	pdfContext.drawTo = name

	pdfContext.windowDisplayEnter()
	for n in something:
		pdfContext.submitLine(n, n.getProperties())
	pdfContext.windowDisplayExit()

	return name.getPath()

from field.core.util import ExecuteCommand

def openFile(filename):
	"""Opens a file with that file's default Application.

	For example open("/var/tmp/something.pdf") will open that file in Preview (or Acrobat)
	"""
	ExecuteCommand(".", ("/usr/bin/open", filename),0).waitFor()


def postProcessLines(c):
	"""Registers a function to be called after 'tweaks'.

	This function is called after 'tweaks' (mouse based line edits) have been applied to the _self.lines variable, but before the lines are displayed. It's also called during line editing itself.
	The function should look like def myFunction(lines): pass where 'lines' is a list of PLines
	"""
	getSelf().postProcessLine_ = getSelf().postProcessLine_ or PythonCallableMap()
	getSelf().postProcessLine_.register(c)
	return c


def saveCroppedPNG(filename, rect):
	""" Saves a png file (cropped by 'rect') from Field's canvas"""
	_self = getSelf()
	x = Vector2(rect.x, rect.y)
	_self.enclosingFrame.transformDrawingToWindow(x)

	y = Vector2(rect.x+rect.w, rect.y+rect.h)
	_self.enclosingFrame.transformDrawingToWindow(y)

	_self.enclosingFrame.saveWindowAsPNGCropped("/var/tmp/window.png", int(x.x), _self.enclosingFrame.frame.height-int(y.y), int(y.x-x.x), int(y.y-x.y))


def loadSVG(filename, quality=1):

	loader =DocumentLoader(UserAgentAdapter())

	svgDocument =loader.loadDocument("file:///"+filename)
	bridgeContext = BridgeContext(UserAgentAdapter(), loader)

	treeBuilder = GVTTreeBuilder(svgDocument, bridgeContext)
	builder =GVTBuilder()
	gvtRoot = builder.build(bridgeContext, svgDocument)

	target = FieldGraphics2D2()
	target.strokeQuality=quality

	gvtRoot.paint(target)


	return [PLine(n) for n in target.geometry]



