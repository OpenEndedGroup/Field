from field.graphics.ci import *
from FluidTools import *
from TweakTools import *
from field.graphics.ci.CoreImageCanvasUtils import Destination
from field.graphics.ci import Destination2
from field.graphics.ci.CoreImageCanvasUtils import Image
from field.graphics.ci.CoreImageCanvasUtils import Accumulator
from field.graphics.ci.CoreImageCanvasUtils import Filter
from field.core.util import *
from java.awt import Font
from java.awt import BasicStroke
from java.io import File
from java.nio import *
utils = CoreImageCanvasUtils()

class image(Image):
	def __init__(self, *s):
		Image.__init__(self, utils, *s)
		self.defaultPosition = Vector2()
		self.defaultScale = 1.0

	def show(self, at=None, blending=1, decor=1, name=None, extents = None, scale=None):
		if (not at): at = self.defaultPosition
		r1 = extents or self.getExtents()
		scale = scale or self.defaultScale
		line = PLine().moveTo(at[0]+r1.x*scale, at[1]+r1.y*scale)
		line.containsImages=1
		self.blending = blending
		line.image_v=self
		line.imageDrawScale_v = scale or self.defaultScale
		getSelf().lines.add(line)
		if (decor):
			rOuter = PLine().rect(Rect(at[0]+r1.x*scale, at[1]+r1.y*scale, r1.w*scale, r1.h*scale))
			rInner = PLine().rect(Rect(at[0],at[1],r1.w*scale+2*r1.x*scale,r1.h*scale+2*r1.y*scale))
			rOuter.color=Vector4(0,0,0,0.5)
			rInner.color=Vector4(0,0,0,0.5)
			rOuter.derived=1
			rInner.derived=1
			rOuter.thickness=0.5
			rInner.thickness=0.5
			rOuter.strokeType = BasicStroke(0.5, 1, 1, 1, (10,10),0)
			rOuter.notForExport=1
			rInner.notForExport=1
			getSelf().lines.add(rOuter)
			getSelf().lines.add(rInner)
			if (name):
				annotation = PLine().moveTo(at[0]+(r1.w+2*r1.x)*scale/2,at[1]+r1.h*scale+16)
				annotation.containsText=1
				annotation.text_v = name
				annotation.font_v = Font("Gill Sans", 0, 14)
				annotation.derived=1
				annotation.notForExport=1
				annotation.color_v=Vector4(0,0,0,0.15)
				getSelf().lines.add(annotation)
	
	def __lshift__(self, other):
		if (other.getClass()==Filter):
			other.inputImage = self
			oi = other.outputImage
			i= image(oi.coreimage)
			oi.coreimage = 0
			i.defaultPosition = self.defaultPosition
			i.defaultScale = self.defaultScale
			return i 
	
	def __mul__(self, other):
		if (isinstance(other, (int, float))):
			return self << filter("CILanczosScaleTransform", inputScale=other, inputAspectRatio=1.0)
		if (isinstance(other, (Vector2,))):
			return self << filter("CILanczosScaleTransform", inputScale=other.x, inputAspectRatio=other.y/other.x)

	def __div__(self, other):
		return self *(1.0/other)

class accumulator(Accumulator):
	def __init__(self, *s):
		Accumulator.__init__(self, utils, *s)
		self.defaultPosition = Vector2()
		self.defaultScale = 1.0

	def image(self):
		return image(CoreImage().accumulator_getOutputImage(self.accumulator))
	
	def show(self, at=None, blending=1, decor=1, name=None, extents=None, scale = None):
		if (not at): at = self.defaultPosition
		r1 = extents or self.image().getExtents()
		line = PLine().moveTo(at[0]+r1.x, at[1]+r1.y)
		line.containsImages=1
		self.blending = blending
		line.image_v=self
		line.imageDrawScale_v = scale or self.defaultScale
		getSelf().lines.add(line)
		if (decor):
			rOuter = PLine().rect(Rect(at[0]+r1.x, at[1]+r1.y, r1.w, r1.h))
			rInner = PLine().rect(Rect(at[0],at[1],r1.w+2*r1.x,r1.h+2*r1.y))
			rOuter.color=Vector4(0,0,0,0.5)
			rInner.color=Vector4(0,0,0,0.5)
			rOuter.derived=1
			rInner.derived=1
			rOuter.thickness=0.5
			rInner.thickness=0.5
			rOuter.strokeType = BasicStroke(0.5, 1, 1, 1, (10,10),0)
			getSelf().lines.add(rOuter)
			getSelf().lines.add(rInner)
			if (name):
				annotation = PLine().moveTo(at[0]+(r1.w+2*r1.x)/2,at[1]+r1.h+16)
				annotation.containsText=1
				annotation.text_v = name
				annotation.font_v = Font("Gill Sans", 0, 14)
				annotation.derived=1
				annotation.color_v=Vector4(0,0,0,0.15)
				getSelf().lines.add(annotation)


	def __ilshift__(self, other):
		if (isinstance(other, (Filter,))):
			other.inputImage = self.image()
			self.setImage(other.outputImage)
			return self

		if (isinstance(other, (image,))):
			self.setImage(other)
			return self

	def __lshift__(self, other):
		if (other.getClass()==Filter):
			other.inputImage = self.getOutputImage()
			oi = other.outputImage
			i= image(oi.coreimage)
			oi.coreimage = 0
			i.defaultPosition = self.defaultPosition
			return i 


def makeFloatImageData(width, height):
	return ByteBuffer.allocateDirect(4*4*width*height).order(ByteOrder.nativeOrder()).asFloatBuffer()

def rasterizeLines(something = None, bounds = None, background = (0,0,0,0), scale = 1):
	if (not something):
		something = getSelf().lines
	
	pdfContext = BasePDFGraphicsContext()

	SimplePDFLineDrawing().installInto(pdfContext)

	if (bounds==None):
		for n in something:
			bounds = Rect.union(LineUtils().slowBounds(n),bounds)
		bounds.x-=2
		bounds.y-=2
		bounds.w+=4
		bounds.h+=4

	pdfContext.paperWidth=bounds.w
	pdfContext.paperHeight=bounds.h

	SimplePDFLineDrawing.outputTransform.z=(-bounds.x)*pdfContext.paperWidth/bounds.w
	SimplePDFLineDrawing.outputTransform.w=(bounds.y+bounds.h)*pdfContext.paperWidth/bounds.w
	SimplePDFLineDrawing.outputTransform.x=pdfContext.paperWidth/bounds.w
	SimplePDFLineDrawing.outputTransform.y=-pdfContext.paperWidth/bounds.w

	name = File.createTempFile("field", ".pdf", None)

	pdfContext.drawTo = name
	pdfContext.windowDisplayEnter()
	for n in something:
		pdfContext.submitLine(n, n.getProperties())
	pdfContext.windowDisplayExit()
	
	print "rasterizing: make sure to run 'defaults write com.apple.versioner.python Prefer-32-Bit -bool yes' from terminal for this to work"
	print "... about to execute command %s " % ( ("/usr/bin/python", "raster.py", name.getAbsolutePath(), name.getAbsolutePath().replace(".pdf",".tif"), "1", "%s"%background[0],"%s"%background[1], "%s"%background[2], "%s"%background[3]),)
	ExecuteCommand(".", ("/usr/bin/python", "raster.py", name.getAbsolutePath(), name.getAbsolutePath().replace(".pdf",".tif"), "%s"%scale, "%s"%background[0],"%s"%background[1], "%s"%background[2], "%s"%background[3]), 1).waitFor()
	
	print "... loading from %s " % name.getAbsolutePath().replace(".pdf",".tif");

	i = image("file://"+name.getAbsolutePath().replace(".pdf",".tif"))
	i.defaultPosition = Vector2(bounds.x, bounds.y)
	i.defaultScale = scale
	return i

def filter(name, **kw):
	f = utils.filter(name)
	for n in kw:
		f.set(n, kw[n])
	return f

def customFilter(name, args):
	def localFilter(**kw):
		f = utils.customFilter(name, args)
		for n in kw:
			f.set(n, kw[n])
		return f
	return localFilter

#some standard things

def blur(radius):
	return filter("CIGaussianBlur", inputRadius=radius)


simpleImageDrawing = SimpleImageDrawing()
simpleImageDrawing.installInto(getSelf().plugin_basicDrawingPlugin.installedContext)

