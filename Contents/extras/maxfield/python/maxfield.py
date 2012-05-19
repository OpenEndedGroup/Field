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

from java.lang import System
from field.core.plugins.python import OutputInsertsOnSheet
from field.core.plugins.log import SimpleExtendedAssignment
from field.core.plugins.log import *
from field.math.abstraction import *
from field.math.linalg import *
from field.bytecode.protect.aliasing import AliasingSystem
import sys
import types

from field.core.plugins.connection import LineDrawingOverride
from field.core.plugins.connection.LineDrawingOverride import ClosestEdge

from field.core.windowing.components import PlainDraggableComponent
from field.core.dispatch import VisualElement
from field.core.dispatch import MergeGroup
from org.json import *
from field.core.plugins.drawing.tweak.Visitors import BaseFilter
from field.core.plugins.drawing.tweak.Visitors import PositionVisitor
from field.core.plugins.drawing.tweak.Visitors import NodeVisitor
from field.core.plugins.drawing.opengl.Polar import iPolarVisitor
from field.core.plugins.drawing.opengl.Polar import PolarMove
from field.core.plugins.drawing.opengl.Polar import PolarFilter
from field.math.linalg import CoordinateFrame

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

from field.core.plugins.drawing import *
from field.core.plugins.drawing.opengl import *
from field.core.plugins.drawing.tweak.python import *

from FluidTools import *
from TweakTools import *

from java.awt import Font


def importMaxFileHere(_self, filename):

	Mixins().mixInOverride(SplineComputingOverride, _self)
	
	
	f = file(filename)
	ff = f.read(-1)
	f.close()
	
	_self.lines.clear()
	
	offsetx = _self.frame.x+_self.frame.w+3
	offsety = _self.frame.y+_self.frame.h+3
	
	mergeGroup = MergeGroup(_self)
	
	
	
	def rectForBox(b):
		rr = b.get("box").get("patching_rect")
		return Rect(rr.get(0)+offsetx, rr.get(1)+offsety, rr.get(2), rr.get(3))
	
	def drawBox(b):
		rr = rectForBox(b)
	
		outputTo, qq, qq2 = mergeGroup.create(b.get("box").get("id"), rr, VisualElement, PlainDraggableComponent, SplineComputingOverride)
		outputTo.name=b.get("box").get("id")
		if (mergeGroup.getLastWasNew()):
			outputTo.python_source_v = ""
		if (outputTo.where.python_autoExec_v!=outputTo):
			outputTo.python_autoExec_v=""

		outputTo.decoration_frame = ArrayList()
	
		if (b.get("box").has("varname")):
			tt = b.get("box").get("varname")
			text = PLine().moveTo(0, -9)
			text.containsText=1
			text.text_v = tt
			text.font_v = Font("Gill Sans", 2, 10)
			text.derived=1
			text.alignment_v=-1
			text(offsetFromSource=Vector2(1, 0))
			text(color=Vector4(0.25, 0, 0, 1))
			outputTo.decoration_frame.add(text)
			hasVarName = 1
			outputTo.maxBox = tt
			outputTo.needsMax = True
		else:
			hasVarName = 0
	
		outputTo.setFrame(rr)
	
		pp = PLine().roundRect(Rect(0,0,rr.w, rr.h), radius=15)(offsetFromSource=Vector2(0,0))
		outputTo.noFrame=1
		pp(derived=1, filled=1, color=Vector4([0, 0.25][hasVarName],0,0,0.2))
		outputTo.decoration_frame.add(pp)
		pp = PLine().roundRect(Rect(0,0,rr.w, rr.h), radius=15)(offsetFromSource=Vector2(0,0))
		outputTo.noFrame=1
		pp(derived=1, filled=0, color=Vector4(0,0,0,0.5),onSourceSelectedOnly=1)
		outputTo.decoration_frame.add(pp)
	
	
		if (b.get("box").has("text")):
			tt = b.get("box").get("text")
			text = PLine().moveTo(5, -3)
			text.containsText=1
			text.text_v = tt
			text.font_v = Font("Gill Sans", 0, 12)
			text.derived=1
			text(offsetFromSource=Vector2(0, 0.5))
			outputTo.decoration_frame.add(text)
	
	mergeGroup.begin()
	
	loaded = JSONObject(ff)
	patcher = loaded.get("patcher")
	boxes = patcher.get("boxes")
	for b in range(boxes.length()):
		drawBox(boxes.get(b))
	
	mergeGroup.end()
	
	def outletForBox(element, num, of):
		rr = element.getFrame(None)
		w = 10
		o = 4
		if (of ==1):
			alpha = 0
		else:
			alpha = (rr.w-w-o*2)*num/(of-1.0)
		return Rect(rr.x+alpha+o, rr.y+rr.h, w, 2)
	
	def inletForBox(element, num, of):
		rr = element.getFrame(None)
		w = 10
		o = 4
		if (of ==1):
			alpha = 0
		else:
			alpha = (rr.w-w-o*2)*num/(of-1.0)
		return Rect(rr.x+alpha+o, rr.y-2, w, 2)
	
	_self.connections = 	[]
	
	lines =patcher.get("lines")
	for l in range(lines.length()):
		ll =lines.get(l)
		p = ll.get("patchline")
		source = p.get("source")
		dest = p.get("destination")
		print "looking for %s %s " % (source, dest)
		sourceb = [b for b in boxes.myArrayList if b.get("box").get("id")==source.get(0)]
		destb = [b for b in boxes.myArrayList if b.get("box").get("id")==dest.get(0)]
	
		if (len(sourceb)>0 and len(destb)>0):
			_self.connections.append( [mergeGroup.findByToken(source.get(0)), source.get(1), sourceb[0].get("box").get("numoutlets"), mergeGroup.findByToken(dest.get(0)), dest.get(1), destb[0].get("box").get("numinlets")])
	
	
		print p
	
	def drawConnections(_self):
		_self.decoration_connections = ArrayList()
	
		for n in _self.connections:
		
			r1 = outletForBox(n[0], n[1], n[2])
			r2 = inletForBox(n[3], n[4], n[5])
			
			_self.decoration_connections.add(PLine().moveTo(*r1.midpoint2()).lineTo(*r2.midpoint2())(color=Vector4(0,0,0,0.4)).line)
			_self.decoration_connections.add(PLine().rect(r1)(filled=1, color=Vector4(0,0,0,1), derived=1, stroked=0).line)
			_self.decoration_connections.add(PLine().rect(r2)(filled=1, color=Vector4(0,0,0,1), derived=1, stroked=0).line)
	
		for nn in _self.subelements:
			for qq in nn.subelements:
				a, b = _self.maxPlugin.computeLine(nn, qq)
				_self.decoration_connections.add(PLine().moveTo(a.x, a.y).lineTo(b.x, b.y)(pointed=1, color=Vector4(0.5,0,0,0.5), thickness=1))
	
	drawConnections(_self)
	
	_self.this = _self
	
	print "about to add callbacks somewhere %s %s " % (_self, getSelf())

	@overrideShouldSetFrame
	def frameChange(source, newRect, oldRect, willSet):
		if (source.this):
			drawConnections(source.this)

	@overrideAdded
	def frameChange(source, newRect, oldRect, willSet):
		if (source.this):
			drawConnections(source.this)

	@overrideDeleted
	def frameChange(source, newRect, oldRect, willSet):
		if (source.this):
			drawConnections(source.this)
	

