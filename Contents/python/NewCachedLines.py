from field.core.plugins.drawing.text import AdvancedTextToCachedLine
from java.awt import Font
from TweakTools import wl
from TweakTools import floatRange

from field.math.util import Circumcenter
from java.lang import Math

from field.core.plugins.drawing.tweak.Visitors import BaseFilter
from field.core.plugins.drawing.tweak.Visitors import PositionVisitor
from field.core.plugins.drawing.tweak.Visitors import NodeVisitor
from field.core.plugins.drawing.tweak.Visitors3 import *
from field.core.plugins.drawing.opengl.Polar import iPolarVisitor
from field.core.plugins.drawing.opengl.Polar import PolarMove
from field.core.plugins.drawing.opengl.Polar import PolarFilter
from field.core.plugins.drawing.opengl import PathFitter
from field.math.linalg import CoordinateFrame

from field.core.plugins.drawing import FieldGraphics2D2

from field.core.plugins.drawing.tweak.python import iCoordTransformation
from field.core.util import PythonCallableMap

from java.awt.geom import Ellipse2D

from field.math.linalg import Vector2
from field.math.linalg import Vector3
from field.math.linalg import Vector4

from field.math.linalg import Quaternion

from field.util import Dict
from field.util.Dict import  Prop
from java.awt import BasicStroke
from java.awt.geom import Area
from field.core.dispatch.iVisualElement import Rect
from java.lang import Double
from java.awt.geom import Arc2D

from field.core.plugins.drawing.opengl import Cursor
from field.core.plugins.drawing.opengl import CachedLine

from field.core.plugins.drawing.opengl import *
from field.core.plugins.drawing.tweak.python import *
from field.core.plugins.drawing import ConstantDistanceResampling3

from TweakTools import PCursor

#FLine = CachedLine

def FLine(**kw):
	"""Makes a new piece of geometry.

	This geometry is built up line by line, moveTo, lineTo, cubicTo ... and can be stroked, filled or pointed."""
	c = CachedLine(**kw)
	return c



def cl__setattr__(self, att, to):
	if (att.endswith("_v")):
		return setattr(self.events[-1].getAttributes(), att, to)
	else:
		return setattr(self.getProperties(), att, to)      


def cl__call__(self, **kw):
	"""Assign properties to line.
	Use keyword arguments to assign properties to line. For example line(color=Color4(1,0,0,1), pointed=1)
	"""
	for k, v in kw.items():
		self.__setattr__(k,v)

	return self

def cl__getattr__(self, att):
		if (att.startswith("__")): return object.__getattribute__(self,att)
		if (att=="nodes"):
			return self.events
		if (att=="position3"): 
			return self.events.get(self.events.size()-1).getDestination(None)
		if (att=="position"): 
			v2 = self.events.get(self.events.size()-1).getDestination(None)
			d = self.z_v
			if (d!=None):
				return Vector3(v2.x, v2.y, d)
			else:
				return v2

		if (att=="current"): return self.events.get(self.events.size()-1)

		if (att.endswith("_v")):
			return getattr(self.events[-1].getAttributes(), att)
		else:
			return getattr(self.getProperties(), att)

		return None

CachedLine.__setattr__ = cl__setattr__
CachedLine.__call__ = cl__call__
CachedLine.__getattr__ = cl__getattr__



def moveTo(self, *x):
	"""Move to point.
	Moves the 'pen' to point 'x' without drawing. For example .moveTo(10,20) or .moveTo(*Vector2(10,20))."""
	if (len(x)<2 or len(x)>3): raise AttributeError("moveTo() takes 2 or 3 parameters")
	self.getInput().moveTo(x[0],x[1])
	if (len(x)==3):
		self.z_v = x[2]
		self.containsDepth=1
	self.breakNext__=0
	return self

def lineTo(self, *x):
	"""Draws a straight line.		
	Draws a straight line to point 'x'. For example .lineTo(10,20) or .lineTo(*Vector2(10,20))."""
	if (len(x)<2 or len(x)>3): raise AttributeError("lineTo() takes 2 or 3 parameters")
	if (self.breakNext__ or len(self.events)==0):
		self.getInput().moveTo(x[0],x[1])
	else:
		self.getInput().lineTo(x[0],x[1])
	if (len(x)==3):
		self.z_v = x[2]
		self.containsDepth=1

	self.breakNext__=0
	return self

def cubicTo(self, *x):
	"""Draws a curved segment.
for cubicTo(c1x,c1y, c2x, c2y, x, y) the curve moves to x,y by inflecting first towards c1x, c1y and then c2x, c2y. 
Likewise, but in 3d, for cubicTo(c1x,c1y,c1z,c2x,c2y,c2z,x,y,z)
	"""
	if (len(x)!=6 and len(x)!=9): raise AttributeError("cubicTo() takes 6 or 9 parameters")
	if (self.breakNext__):
		if (len(x)==6):
			self.getInput().moveTo(x[-2], x[-1])
		else:
			self.getInput().moveTo(x[-3], x[-2], x[-1])
	else:
		if (len(x)==6):
			self.getInput().cubicTo(*x)
		else:
			self.getInput().cubicTo(x[0], x[1], x[3], x[4], x[6], x[7])
			self.z_v = Vector3(x[2], x[5], x[8])
			self.containsDepth=1
	self.breakNext__=0
	return self


def relLineTo(self, *x):
	"""Draws a straight line.		
	Draws a straight line to point position+'x'. For example .relLineTo(10,20) or .relLineTo(*Vector2(10,20))."""
	return self.lineTo(*(self.position3+x))

def relMoveTo(self, *x):
	"""Moves to a point		
	Moves the 'pen' to point position+'x' without drawing. For example .relMoveTo(10,20) or .relMoveTo(*Vector2(10,20))."""
	return self.moveTo(*(self.position3+x))

def relCubicTo(self, *x):
	"""Draws a curve segment relative to the current position.

	For cubicTo(c1x,c1y, c2x, c2y, x, y) the curve moves to position+x,y by inflecting first towards c1x, c1y and then c2x, c2y. Likewise, but in 3d, for cubicTo(c1x,c1y,c1z,c2x,c2y,c2z,x,y,z).
	"""
	if (len(x)==6):
		p = self.position
		self.cubicTo(x[0]+p.x,x[1]+p.y, x[2]+p.x,x[3]+p.y, x[4]+p.x, x[5]+p.y)
	elif (len(x)==9):
		p = self.position3
		self.cubicTo(x[0]+p.x,x[1]+p.y, x[2]+p.z,x[3]+p.x, x[4]+p.y, x[5]+p.z, x[6]+p.x, x[7]+p.y, x[8]+p.z)
	return self

def polarCubicTo(self, a1, len1, a2, len2, x2, y2):
	"""Draws a curve segment to x2,y2 that curves relative to the straight line.
	
	a1 and a2 control the angle of the curve (measured clockwise from the striaght line; len1, and len2 control the length of the tangent (a1=0,len1=1,a2=0,len2=1 yields a straight even line).
	
	Only works (and only makes sense) in 2d.

	"""
	was = self.position
	d1 = Vector2(x2-was.x, y2-was.y).scale(len1/3.0).rotateBy(a1)
	d2 = Vector2(x2-was.x, y2-was.y).scale(-len2/3.0).rotateBy(a2)
	self.cubicTo(d1.x+was.x, d1.y+was.y, x2+d2.x, y2+d2.y, x2, y2)
	return self

CachedLine.moveTo  =moveTo
CachedLine.lineTo  =lineTo
CachedLine.relLineTo  =relLineTo
CachedLine.relMoveTo  =relMoveTo
CachedLine.cubicTo  =cubicTo
CachedLine.relCubicTo  =relCubicTo
CachedLine.polarCubicTo  =polarCubicTo

CachedLine.completesAsPython__=1


def addCode(self, code):
	"""Add code to this node.

	This code will be called whenever this line is updated by the tweaks() system."""

	LateExecutingDrawing.addCode(self.events[-1], code)
	return self

CachedLine.addCode=addCode


def rect(self, x,y,w,h):
	"""Draw a rectangle.

	e.g .rect(10,10,20,30) draws with top-left at (10,10) that is 20 wide and 30 tall."""
	self.moveTo(x, y).lineTo(x+w, y).lineTo(x+w, y+h).lineTo(x, y+h).lineTo(x, y)
	return self

CachedLine.rect=rect

from java.awt.geom import RoundRectangle2D


def roundRect(self, x,y,w,h, radius=5):
	"""Draw a rounded rectangle.

	e.g. .roundedRect(10,10,20,20, radius=2) draws a rectangle with a "corner radius" of 2."""
	shape = RoundRectangle2D.Float(x, y, w, h, radius, radius)
	self.appendShape(shape)
	return self

CachedLine.roundRect = roundRect

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
	return self

CachedLine.ellipse = ellipse
CachedLine.circle = circle
CachedLine.line = line

def segment(self):
	"""Returns a 'wrapped list' of this line split into pieces.     
                                                                                
	Each piece starts with each .moveTo(...)"""
	return wl([x for x in LineUtils().segmentSubpaths(self)])

CachedLine.segment = segment


def replaceContents(self, source, deep=0):
	"""Replaces the contents of this line with the contents of another"""
	self.forceNew=1
	self.events.clear()
	if (not source): return self
	if (not deep):
		self.events.addAll(source.events)
	else:
		for n in source.events:
			self.events.add(n.copy())
	return self

def append(self, source, deep=0, merge=1):
	"""Appends the contents of this line with the contents of another."""
	if (not deep):
		self.events.addAll(source.events)
	else:
		for n in source.events:
			nc = n.copy()
			nc.container=self
			self.events.add(nc)
	self.forceNew=1
	if (merge==1):
		self.properties.mergeAll(source.properties)
	elif (merge==2):
		self.properties.putAll(source.properties)
	return self

def clear(self):
	"""Removes all nodes from this line"""
	self.events.clear()
	return self

CachedLine.clear = clear
CachedLine.append = append

def copy(self):
	"""Returns a new copy of this line"""
	return LineUtils().transformLine(self, None, None, None, None).remapProperties(self)

def remapProperties(self, source):
	if 0:
		LineUtils().fixProperties(self, source)		
	return self

def newLineByStroking(self, thickness=1, join=0, cap=0, miter=10, dashed=0, dash=None, phase=0):
	"""Apply a stroke style to this PLine.
		
	Returns a new line with this line stroked with a particular style."""
	if (dash):
		stroke = BasicStroke(thickness, join, cap, miter, dash, phase)		
	else:
		stroke = BasicStroke(thickness, join, cap, miter)
	s = LineUtils().lineAsStroked(self, stroke, 1)
	LineUtils().fixClose(s)
	return s

def newLineByJoiningEndsWith(self, other):
	""" Makes a new line (a closed loop) by joining the ends of this line to another line """
	one = self
	d1 = (one.nodes[0].position()-other.nodes[0].position()).mag()+(one.nodes[-1].position()-other.nodes[-1].position()).mag()
	d2 = (one.nodes[0].position()-other.nodes[-1].position()).mag()+(one.nodes[-1].position()-other.nodes[0].position()).mag()
	
	
	if (d1<d2):
		other = LineUtils().reverse(other)
	else:
		pass
	
	both = FLine()
	both.events.addAll(one.events)
	both.lineTo(*other.events[0].position())
	both.events.addAll(other.events[1:])
	both.lineTo(*one.events[0].position())
	return both

def newLineByReversing(self):
	""" Makes a new line by reversing this one """
	return LineUtils().reverse(self)

CachedLine.newLineByReversing = newLineByReversing
CachedLine.newLineByJoiningEndsWith = newLineByJoiningEndsWith


def newLineBySubdivision(self):
	"""Convert all to curves and split each curve into two."""
	return LineUtils().simpleSubdivideAllAsCurves3(self)


def newLinesBySegmenting(self):
	"""Returns a 'wrapped list' of this line split into pieces.

	Each piece starts with each .moveTo(...)"""
	return wl([PLine(x) for x in LineUtils().segmentSubpaths(self)])

CachedLine.append = append
CachedLine.replaceContents = replaceContents
CachedLine.copy = copy
CachedLine.remapProperties = remapProperties
CachedLine.newLineByStroking = newLineByStroking
CachedLine.newLineBySubdivision = newLineBySubdivision


def appendVectorText2(self, text, x, y, font="Gill Sans", attributes=0, size=20):
	"""Appends (vector) text to this FLine with the specified font, attributes and size.
	
	Set attributes to be 1 for bold, 2 for italics and 3 for bold-italics""" 
	at = AdvancedTextToCachedLine(Font(font, attributes, size))
	fc = LineUtils().fixClose(at.getLine(text, x, y))
	self.append(fc)
	return self


CachedLine.appendVectorText2 = appendVectorText2



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


def newLineByFilteringPositions2(self, lam):
	"""Make a new line by filtering this one.

	This method applies a function 'f' to each piece of positional data in this line. This function f(x,y) returns Vector2 or (x,y)
	"""
	return BaseFilter().visitPositions(self, __pv(lam))


def newLineByTransformingSpace2(self, transformer, transformDensity=0.1, maxError=1.5):
	""" Transforms this line through a function "transformer" which takes one parameter (a Vector2) and returns a Vector2 of the transformed position.

	Unlike, say, newLineByFilteringNodes() this function can transform the "insides" of curves, not just the positions of the nodes. It does this by densly resampling the line, transforming these samples, then fitting a new path through the resulting shape. The optional parameter "transformDensity" sets the number of dense samples per unit length of the input line.  "maxError" controls the tightness of the fit."""

	numSamples = 2 + int(transformDensity*self.cursor().length())
	x = [x.position2() for x in self.newLineByResampling(numSamples).nodes]
	y= [transformer(x) for x in x]
	fit = PathFitter(y, maxError)
	fit.fit()
	return fit.toCachedLine()

def newLineByFitting2(self, maxError=1.5, resample=0):
	""" Fits a FLine to this path.

	For oversampled or "noisy" lines, this new line may end up being much simpler (especially if you set maxError to be quite large). For very simple lines you may end up with a few more spline sections (the fitting algorithm used is not perfect). Set resample to a number > 0 to resample this line before fitting, otherwise this line is treated a a mere series of points """

	if (resample>0):
		x = [x.position2() for x in self.newLineByResampling(resample).nodes]
	else:
		x = [x.position2() for x in self.nodes]

	fit = PathFitter(x, maxError)
	fit.fit()
	return fit.toCachedLine()

CachedLine.newLineByTransformingSpace2 = newLineByTransformingSpace2
CachedLine.newLineByFitting2 = newLineByFitting2


def newLineByFilteringNodes2(self, f):
	"""Make a new line by filtering this one.

	This method applies a function 'f' to each node in this line. 

	f(before, now, after, beforeIsCurve, afterIsCurve) --- before, now, and after are Vector2's that this function can mutate. They might be None if you are at the start or end of a line segment. beforeIsCurve and afterIsCurve are 1 if before and after refer to curve segments.
	"""

	return BaseFilter().visitNodes(self, __nf(f))
	

def newLineByFilteringPolar2(self, f):
	"""Filters this line in polar space.
		
	Applies a function 'f' that filters PolarMove objects to this line. This can be useful to manipulate a line in local polar space, applying cumulative curves to the positions and control points of the line.
	"""

	pf = PolarFilter(self)
	return pf.visitPolar(__pv2(f))


def filterPositions2(self, lam):
	"""Filter this line.

	This method applies a function 'f' to each piece of positional data in this line. This function f(x,y) returns Vector2 or (x,y)
	"""
	return self.replaceContents(newLineByFilteringPositions2(self, lam))

def filterNodes2(self, lam):
	"""Make a new line by filtering this one.

	This method applies a function 'f' to each node in this line. 

	f(before, now, after, beforeIsCurve, afterIsCurve) --- before, now, and after are Vector2's that this function can mutate. They might be None if you are at the start or end of a line segment. beforeIsCurve and afterIsCurve are 1 if before and after refer to curve segments.
	"""
	return self.replaceContents(newLineByFilteringNodes2(self, lam))

def filterPolar2(self, lam):
	"""Filters this line in polar space.
		
	Applies a function 'f' that filters PolarMove objects to this line. This can be useful to manipulate a line in local polar space, applying cumulative curves to the positions and control points of the line.
	"""
	return self.replaceContents( newLineByFilteringPolar2(self, lam))


CachedLine.newLineByFilteringPositions2 = newLineByFilteringPositions2
CachedLine.newLineByFilteringNodes2 = newLineByFilteringNodes2
CachedLine.newLineByFilteringPolar2 = newLineByFilteringPolar2

CachedLine.filterPositions2 = filterPositions2
CachedLine.filterNodes2 = filterNodes2
CachedLine.filterPolar2 = filterPolar2



class __pv3(PositionVisitor3):
	def __init__(self, lam): 
		self.lam = lam
	def visitPosition(self, pos, part, inside):
		pp = self.lam(pos.x, pos.y, pos.z)
		pos.x = pp[0]
		pos.y = pp[1]
		pos.z = pp[2]

class __nf3(NodeVisitor3):
	def __init__(self,f): self.f = f
	def visitNode(self, before, now, after, beforeIsCurve, afterIsCurve):
		self.f(before, now, after, beforeIsCurve, afterIsCurve)

def newLineByFilteringPositions(self, lam):
	"""Make a new line by filtering this one.

	This method applies a function 'f' to each piece of positional data in this line. This function f(x,y) returns Vector2 or (x,y)
	"""
	return BaseFilter3().visitPositions(self, __pv3(lam))

def newLineByFilteringNodes(self, f):
	"""Make a new line by filtering this one.

	This method applies a function 'f' to each node in this line. 

	f(before, now, after, beforeIsCurve, afterIsCurve) --- before, now, and after are Vector2's that this function can mutate. They might be None if you are at the start or end of a line segment. beforeIsCurve and afterIsCurve are 1 if before and after refer to curve segments.
	"""

	return BaseFilter3().visitNodes(self, __nf3(f))
	
def filterPositions(self, lam):
	"""Filter this line.

	This method applies a function 'f' to each piece of positional data in this line. This function f(x,y) returns Vector2 or (x,y)
	"""
	return self.replaceContents(newLineByFilteringPositions(self, lam))


def filterNodes(self, lam):
	"""Make a new line by filtering this one.

	This method applies a function 'f' to each node in this line. 

	f(before, now, after, beforeIsCurve, afterIsCurve) --- before, now, and after are Vector2's that this function can mutate. They might be None if you are at the start or end of a line segment. beforeIsCurve and afterIsCurve are 1 if before and after refer to curve segments.
	"""
	return self.replaceContents(newLineByFilteringNodes(self, lam))

CachedLine.newLineByFilteringPositions = newLineByFilteringPositions
CachedLine.newLineByFilteringNodes = newLineByFilteringNodes

CachedLine.filterPositions = filterPositions
CachedLine.filterNodes = filterNodes



def newLineByResampling(self, targetNum, rough=1):
	"""Resample this line.
	This resamples this line into (roughly) 'targetNum' pieces that are (roughly) equally spaced. For more accurate subdivision set rough=0. """

	if (rough):
		resampler = ConstantDistanceResampling3(5, 8)
		return resampler.resample(targetNum, self)
	else:
		c = self.cursor()
		x = []
		out = FLine()
		for n in floatRange(0, c.length(), targetNum):
			forward = c.tangentForward3()
			op = c.position3()
			c.setD(n)
			backward = c.tangentBackward3()
			p = c.position3()
			if (forward and backward):
	
				forward.normalize().scale(p.distanceFrom(op)/3)
				backward.normalize().scale(-p.distanceFrom(op)/3)
	
				forward += op
				backward += p
				out.cubicTo(forward.x,forward.y, forward.z, backward.x, backward.y, backward.z, *p)
			else:
				out.lineTo(*p)
		return out



CachedLine.newLineByResampling = newLineByResampling



def bounds2(self, fast=1):
	"""Returns the Rect that this line fits in """
	if (fast):
		bound = LineUtils().fastBounds(self)
		return Rect(bound[0].x,bound[0].y, bound[1].x-bound[0].x, bound[1].y-bound[0].y)
	else:
		bb = self.toArea2().getBounds()
		return Rect(bb.x, bb.y, bb.width, bb.height)

def bounds(self):
	"""Returns the approximate (Vector3, Vector3) (min -> max) that this line fits in """
	return LineUtils().fastBounds3(self)
	

CachedLine.bounds2 = bounds2
CachedLine.bounds = bounds


def __iadd__(self, translation):
	if (isinstance(translation, Vector2)):
		f = LineUtils().transformLine3(self, Vector3(translation.x, translation.y, 0), None, None, None)
		f.remapProperties(self)
		self.replaceContents(f)
	elif (isinstance(translation, Vector3)):
		f = (LineUtils().transformLine3(self, translation, None, None, None))
		f.remapProperties(self)
		self.replaceContents(f)
	elif (isinstance(translation, Quaternion)):
		f = (LineUtils().transformLine3(self, None, None, translation, None))
		f.remapProperties(self)
		self.replaceContents(f)
	elif (isinstance(translation, CoordinateFrame)):
		f = (LineUtils().transformLine3(self, translation));
		f.remapProperties(self)
		self.replaceContents(f)
	return self

def appendShape(self, shape):
	"""Appends a java2D Shape to this line"""
	LineUtils().piToCachedLine(shape.getPathIterator(None), self, 1, 1)
	#LineUtils().fixClose(self)
	return self

CachedLine.appendShape = appendShape

def arc(self, radius, x, y, start, end, join=0):
	""" Draws an arc. An Arc is a segment of a circle radius 'radius' centered at 'x,y' from 'start' to 'end' radians. 

	start=0 corresponds to "East" or 3 o'clock. Consider using arcTo."""

	start = 180*start/Math.PI
	end = 180*end/Math.PI
	z = Arc2D.Double(x-radius, y-radius, radius*2, radius*2, start, end-start, 0)
	num = len(self.nodes)
	self.appendShape(z)
	if (join):
		del self.nodes[num]
	return self

CachedLine.arc = arc

def arcTo(self, cx, cy, p2x, p2y):
	"""Adds a segment from the current position to p2x, p2y by drawing part of a circle centered on 'cx,cy'"""
	p1x, p1y = self.nodes[-1].position2()
	angle1 = Math.atan2(-(p1y-cy), p1x-cx)
	angle2 = Math.atan2(-(p2y-cy), p2x-cx)
	if (angle2-angle1>Math.PI):
		angle2-=Math.PI*2
	if (angle2-angle1<-Math.PI):
		angle2+=Math.PI*2
	self.arc(Math.sqrt( (p1x-cx)*(p1x-cx)+(p1y-cy)*(p1y-cy)), cx,cy, angle1, angle2, join=1)
	return self

CachedLine.arcTo = arcTo

def arcThrough(self, p1x, p1y, p2x, p2y):
        """Adds a segment from the current position to p2x, p2y by drawing part of a circle. The circle is given by the circle that would pass through the current position, p1 and p2"""
	c = Circumcenter().circumcenterOf(arced.nodes[-1].position2(), Vector2(p1x, p1y), Vector2(p2x, p2y))
	return self.arcTo(c.x, c.y, p2x, p2y)

CachedLine.arcThrough = arcThrough


def __and__(self, otherLine):
	if (isinstance(otherLine, CachedLine)):
		otherLine = otherLine.toArea2()
	a = self.toArea2()
	print a.intersect(otherLine)
	return CachedLine().fromArea2(a)

def __or__(self, otherLine):
	if (isinstance(otherLine, CachedLine)):
		otherLine = otherLine.toArea2()
	a = self.toArea2()
	a.add(otherLine)
	return CachedLine().fromArea2(a)

def __sub__(self, otherLine):
	if (isinstance(otherLine, CachedLine)):
		otherLine = otherLine.toArea2()
	a = self.toArea2()
	a.subtract(otherLine)
	return CachedLine().fromArea2(a)

def toArea2(self):
	"""Convert this line to a java.geom.Area"""
	return Area(LineUtils().lineToGeneralPath(self))

def fromArea2(self, area):
	"""Set this line to be the contents of a java.geom.Area"""
	self.replaceContents(LineUtils().piToCachedLine(area.getPathIterator(None)))
	LineUtils().fixClose(self)
	return self

def __imul__(self, translation):
	if (isinstance(translation, Vector2)):
		tx = self.bounds2().midpoint2().scale(-1)
		f = (LineUtils().transformLine(self, tx, Vector2(translation.x, translation.y), None, Vector2(tx).scale(-1)))
		f.remapProperties(self)
		self.replaceContents(f)
	elif (isinstance(translation, Vector3)):
		tx = self.bounds()
		tx = (tx[0]+tx[1])*-0.5
		f = (LineUtils().transformLine3(self, tx, translation, None, tx*-1.0))
		f.remapProperties(self)
		self.replaceContents(f)
	elif (isinstance(translation, Quaternion)):
		f = (LineUtils().transformLine3(self, None, translation, None, None))
		f.remapProperties(self)
		self.replaceContents(f)
	elif (isinstance(translation, CoordinateFrame)):
		f = (LineUtils().transformLine3(self, translation));
		f.remapProperties(self)
		self.replaceContents(f)
	return self

CachedLine.__imul__ = __imul__
CachedLine.__iadd__ = __iadd__
CachedLine.toArea2 = toArea2
CachedLine.fromArea2 = fromArea2
CachedLine.__sub__ = __sub__
CachedLine.__and__ = __and__
CachedLine.__or__ = __or__



def cursor(self, at=0):
	"""Returns a 'cursor' object for this line.
		
	This cursor can be used to inspect points on the line and edit it's contents.
	The optional parameter 'at' sets the initial position in nodeNumber.t format. For example 4.25 means between node 4 and node 5, 25% of the way between them."""
	return PCursor(self, at)

CachedLine.cursor = cursor

def cursorAtPosition(self, position):
	"""Returns a 'cursor' object for this line thats closest to this position
        """

	return PCursor(Cursor.cursorFromClosestPoint(self, position))

CachedLine.cursor = cursor

def pointsOfIntersection2(self, otherLine):
	"""Returns a list of points where this line intersects with otherLine"""
	x = Intersections.intersectAndSubdivide([self.copy()], [otherLine.copy()], 5)
	rr = []
	for xx in x:
		rr.append( xx[0].getAt())
	return rr

def doIntersection2(self, otherLine):
	"""Intersects this line with otherLine; mutates both

	This method will insert nodes in both lines where they intersect and return cursors for each line at these intersection points. These cursors are returned as [(this, other), (this, other) ... ]"""
	ret = Intersections.intersectAndSubdivide([self], [otherLine], 5)
	rr = []
	for r in ret:
		a,b = Intersections.resolve(r[0]), Intersections.resolve(r[1])
		h1 = PCursor(a.getContainer(), a.getContainer().events.indexOf(a) )
		h2 = PCursor(b.getContainer(), b.getContainer().events.indexOf(b) )
		rr.append( (h1,h2) )

	return rr

CachedLine.pointsOfIntersection2 = pointsOfIntersection2
CachedLine.cursorAtPosition = cursorAtPosition
CachedLine.doIntersection2 = doIntersection2


from field.core.plugins.drawing.opengl import CachedLine
from field.core.plugins.drawing.opengl.CachedLine import Event


Event.completesAsPython__=1


def position(self):
	"""Returns the position of this node"""
	return LineUtils.getDestination3(self)

def points(self):
	"""Returns either [position()] or [control1(), control2(), position()] if this node is cubic"""
	if (len(self.args)==2):
		return [self.position()]
	elif (len(self.args)==6):
		return [self.control1(), self.control2(), self.position()]
	return []

def setPoints(self, *p):
	"""Sets the positions associated with this node (either .position() or [.control1(), control2(), and position()]"""
	if (self.isCubic()):
		self.setControl1(*p[0 % len(p)])
		self.setControl2(*p[1 % len(p)])
	self.setPosition(*p[-1])


Event.setPoints = setPoints
Event.points = points

def setPosition(self, *p):
	"""Sets the position of this node"""
	if (len(p)==2):
		self.setAt(-1, Vector2(p[0], p[1]))
	elif(len(p)==3):
		self.setAt(-1, Vector2(p[0], p[1]))
		if (len(self.args)==6):
			if(self.z_v):
				self.z_v.z = p[2]
			else:
				self.z_v = Vector3(p[2], p[2], p[2])
		else:
			self.z_v = p[2]
	return self

def setControl1(self, *p):
	"""Sets the control1 of this (cubic) node"""
	if (len(p)==2):
		self.setAt(0, Vector2(p[0], p[1]))
	elif(len(p)==3):
		self.setAt(0, Vector2(p[0], p[1]))
		if (len(self.args)==6):
			if(self.z_v):
				self.z_v.x = p[2]
			else:
				self.z_v = Vector3(p[2], p[2], p[2])
		else:
			self.z_v = p[2]
	return self

def setControl2(self, *p):
	"""Sets the control2 of this (cubic) node"""
	if (len(p)==2):
		self.setAt(1, Vector2(p[0], p[1]))
	elif(len(p)==3):
		self.setAt(1, Vector2(p[0], p[1]))
		if (len(self.args)==6):
			if(self.z_v):
				self.z_v.y = p[2]
			else:
				self.z_v = Vector3(p[2], p[2], p[2])
		else:
			self.z_v = p[2]
	return self

def control1(self):
	"""Returns the control point1 of this (cubic) node"""
	return LineUtils.getControl1(self)

def control2(self):
	"""Returns the control point2 of this (cubic) node"""
	return LineUtils.getControl2(self)

def  isCubic(self):
	"""Returns 1 if this is a cubic node"""
	if (self.args and len(self.args)==6): return 1
	return 0

def isConnected(self):
	"""Returns 1 if this is a line or a cubic node"""
	return not self.method.getName()[0]=="m";

def position2(self):
	"""Returns the 2d position of this node"""
	return LineUtils.getDestination3(self).toVector2()

Event.position = position
Event.position2 = position2
Event.isCubic = isCubic
Event.control2 = control2
Event.control1 = control1
Event.setPosition = setPosition
Event.setControl1 = setControl1
Event.setControl2 = setControl2
Event.isConnected = isConnected

def __iadd__(self, translation):
	if (isinstance(translation, Vector2)):
		self.setPoints(*[x+translation for x in self.points()])
		return self
	elif (isinstance(translation, Vector3)):
		self.setPoints(*[x+translation for x in self.points()])
		return self
	elif (isinstance(translation, Quaternion)):
		self.setPoints(*[translation.rotateVector(x) for x in self.points()])
		return self
	elif (isinstance(translation, CoordinateFrame)):
		self.setPoints(*[translation.transformPosition(x) for x in self.points()])
		return self
	return self

Event.__iadd__ = __iadd__

def previous(self):
	"""Returns the node prior to this node in the line (or None if this is the start of the line)"""
	ii = self.container.nodes.indexOf(self)
	if (ii<1): return None
	return self.container.nodes[ii-1]

def next(self):
	"""Returns the node prior to this node in the line (or None if this is the end of the line)"""
	ii = self.container.nodes.indexOf(self)
	if (ii>len(self.container.nodes)-2): return None
	return self.container.nodes[ii+1]

Event.previous = previous
Event.next = next

def head(self):
	"""Returns the node prior to this node in the line (or this node if this is a .moveTo node)"""
	if (not self.isConnected()): return self
	return self.previous() or self

Event.head = head

cf = CoordinateFrame()

import types
isinstance(0.3, (float,int,long))

def CFrame(r=Quaternion(), s=Vector3(1,1,1), t=Vector3(0,0,0), center=None):
	"""Constructs a Coordinate frame transformation with rotation 'r', scale 's', translation 't' around a center.

	all parmaters are optional, and a wide variety of types are understood (Vector2, Vector3, Quaternion, FLine, FLine Nodes, Tuples and lists)"""
	if (isinstance(center, (CachedLine, ))):
		a,b = center.bounds()
		center = (a+b)*0.5
	if (isinstance(t, (CachedLine, ))):
		a,b = t.bounds()
		t = (a+b)*0.5
	if (isinstance(r, (float,int,long))):
		r = Quaternion(r)
	if (isinstance(r, (Quaternion, ))):
		pass
	else:
		r = Quaternion(*r)
	if (isinstance(s, (float,int,long))):
		s = Vector3(s,s,s)
	if (isinstance(t, Event)):
		t = t.position()
	if (isinstance(t, Vector2)):
		t = Vector3(t.x, t.y,0)
	if (isinstance(s, Vector2)):
		s = Vector3(t.x, t.y,1)
	if (isinstance(s, (tuple, list))):
		if (len(s)==2):
			s = Vector3(s[0], s[1], 1)
		else:
			s = Vector3(s[0], s[1], s[2])
	if (isinstance(t, (tuple, list))):
		if (len(t)==2):
			t = Vector3(t[0], t[1], 1)
		else:
			t = Vector3(t[0], t[1], t[2])

	zz = CoordinateFrame(r, t, s)
	if (isinstance(center, (Event, ))):
		center = center.position()

	if (center):
		return CFrame(t=center)*zz*CFrame(t=-1.0*center)
	return zz

def __call__(self, r=None, s=None,t=None):
	if (r):
		self.setRotation(r)
	if (s):
		self.setScale(s)
	if (t):
		self.setTranslation(t)
	return self

def __getattr__(self, name):
	if (name=="r"): return self.getRotation(None)
	elif (name=="s"): return self.getScale(None)
	elif (name=="t"): return self.getTranslation(None)
	raise AttributeError()
	

def __setattr__(self, name, value):
	if (name=="r"): return self.setRotation(value)
	elif (name=="s"): return self.setScale(value)
	elif (name=="t"): return self.setTranslation(value)
	raise AttributeError()

CoordinateFrame.__call__ = __call__
CoordinateFrame.__getattr__ = __getattr__
CoordinateFrame.__setattr__ = __setattr__

def __mul__(self, other):
	print "cf mull self : %s other %s " % (self, other)
	if (isinstance(other, CoordinateFrame)):
		return CoordinateFrame().multiply(self, other)
	if (isinstance(other, Vector3)):
		return self.transformPosition(Vector3(other))
def __add__(self, other):
	if (isinstance(other, CoordinateFrame)):
		return CoordinateFrame().multiply(self, other)
	if (isinstance(other, Vector3)):
		return self.transformDirection(Vector3(other))

CoordinateFrame.__mul__ = __mul__
CoordinateFrame.__add__ = __add__


def __call__(self, **kw):
	x = kw.get("x")
	if (x!=None): self.x = x
	y = kw.get("y")
	if (y!=None): self.y = y
	z = kw.get("z")
	if (z!=None): self.z = z
	w = kw.get("w")
	if (w!=None): self.w = w
	r = kw.get("r")
	if (r!=None): self.x = x
	g = kw.get("g")
	if (g!=None): self.y = y
	b = kw.get("b")
	if (b!=None): self.z = z
	a = kw.get("a")
	if (a!=None): self.w = w
	return self

Vector4.__call__ = __call__
Vector3.__call__ = __call__
Vector2.__call__ = __call__






