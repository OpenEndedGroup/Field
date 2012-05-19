from field.graphics.dynamic import *
from field.graphics.core import BasicGLSLangProgram
from field.launch import Launcher
from field.launch.Launcher import iExceptionHandler
from field.graphics.windowing import FullScreenCanvasSWT
from field.graphics.dynamic import *
#from field.graphics.imageprocessing import *
from field.graphics.core.BasicGeometry import *
from field.graphics.core.Base import StandardPass

from field.graphics.ci import *
from field.launch import SystemProperties

from field.core.plugins.drawing import ThreedComputingOverride
from field.core.windowing.components import PlainDraggableComponent
from field.core.dispatch import VisualElement
from field.math.linalg import Vector4
from field.core.dispatch.iVisualElement import Rect

from FluidTools import getSelf

globalCanvas = None

def makeFullscreenCanvas(clearBackground=1):
    """Makes a drawing window that fills a display. By default this will go on any second monitor connected to the system (i.e. one without a menu bar on it)"""
    SystemProperties.setProperty("background.clear","%s"%clearBackground);
    canvas = FullScreenCanvasSWT().withKeyboardControlledCamera_smooth()
    canvas.pressSpaceToSave("/var/tmp/field_")
    canvas.addDefaultHandlers()
    global globalCanvas
    globalCanvas = canvas
    Launcher.getLauncher().registerUpdateable(canvas)
    canvas.setVisible(1)
    canvas.camera.far=10000
    return canvas


def makeEmbeddedCanvas(name, x=None, y=None, w=500, h=500, background=Vector4()):
	"""Makes a self-enclosed drawing window and places it inside the main Field canvas. Like makeFullscreenCanvas and makeWindowedCanvas, but in this case the window is embedded inside Field"""
	if (x==None): x = getSelf().frame.x
	if (y==None): y = getSelf().frame.y+getSelf().frame.h+25
        print x,y,w,h
	a,b,c = VisualElement.createWithToken(name, getSelf(), Rect(x,y,w,h), VisualElement, PlainDraggableComponent, ThreedComputingOverride)
	a.name = "canvas: "+name
	if (a.where.python_source_v==getSelf()): a.python_source_v=""
	return a.canvas	


def getFullscreenCanvas(createOnDemand=1,clearBackground=1):
    """
    Gets (or makes) a full screen canvas.

    By default this goes on a second display, unless -onMainScreen 1 is specified at launch
    """
    if (not globalCanvas):
        if (createOnDemand): return makeFullscreenCanvas(clearBackground)
    return globalCanvas

def makeWindowedCanvas():
    canvas = FullScreenCanvasSWT(1).withKeyboardControlledCamera_smooth()
    canvas.pressSpaceToSave("/var/tmp/field_")
    canvas.addDefaultHandlers()
    global globalCanvas
    globalCanvas = canvas
    Launcher.getLauncher().registerUpdateable(canvas)
    canvas.setVisible(1)
    canvas.camera.far=10000
    return canvas

def getWindowedCanvas(createOnDemand=1, bounds=None):
    """
    Gets (or makes) a windowed canvas.

    set bounds = (x,y,w,h) to set the window dimensions
    """
    global globalCanvas
    if (not globalCanvas):
        if (createOnDemand): 
            globalCanvas = makeWindowedCanvas()
    if (bounds):
        globalCanvas.getFrame().setBounds(bounds[0], bounds[1], bounds[2], bounds[3])
    return globalCanvas

def meshContainer():
    """
    A general purpose container for triangles
    """
    return DynamicMesh_long.unshadedMesh()

def lineContainer():
    """
    A general purpose container for lines
    """
    return DynamicLine_long.unshadedLine(None, 1)

def pointContainer():
    """
    A general purpose container for points
    """
    return DynamicPointlist.unshadedPoints(None, 1)

def pointContainerWithQuads():
    """
    A general purpose container for points that draws Quads instead of Points. You'll need a shader that uses s_Texture to "fatten" up your points.
    """
    qm = QuadMesh_long(StandardPass.render)
    qm.rebuildTriangle(0)
    qm.rebuildVertex(0)
    points = DynamicPointlist_quad(qm)
    return points


def quadContainer():
    """
    A general purpose container for quads
    """
    return DynamicQuad_long.unshadedQuad(None)



dynamicMesh = DynamicMesh.unshadedMesh
def dynamicLine():
    return DynamicLine.unshadedLine(None, 1)

def dynamicPoints():
    return DynamicPointlist.unshadedPoints(None, 1)

def dynamicMesh_long():
    return DynamicMesh_long.unshadedMesh()

def dynamicLine_long():
    return DynamicLine_long.unshadedLine(None, 1)

def dynamicQuad_long():
    return DynamicQuad_long.unshadedQuad(None)


glslangShader = BasicGLSLangProgram

from field.graphics.core import BasicFrameBuffers
from field.graphics.core.BasicFrameBuffers import SingleFrameBuffer
from field.graphics.core.BasicFrameBuffers import DoubleFrameBuffer


def makeFrameBuffer(width, height, useRect=1, useFloat=0, genMipmaps=0):
    """Makes a frame buffer object (for offscreen rendering).

    width and height are the dimensions of the buffer.

    useRectangle means that the buffer uses GL_TEXTURE_RECTANGLE_ARB rather than GL_TEXTURE_2D

    useFloat means that the buffer uses floating point precision

    genMipmaps means that the resulting texture map has mipmap levels (and is thus much slower to update)
    """
    return SingleFrameBuffer(width, height, useRect, useFloat, genMipmaps)

def make2FrameBuffer(width, height, useRect=1, useFloat=0):
    """Makes a frame buffer object (for offscreen rendering).

    width and height are the dimensions of the buffer.

    useRectangle means that the buffer uses GL_TEXTURE_RECTANGLE_ARB rather than GL_TEXTURE_2D

    useFloat means that the buffer uses floating point precision
    """
    return DoubleFrameBuffer(width, height, useRect, useFloat)


def twoPassImageProcessing(input, width, height, useRect=1, useFloat=0, generateMipmaps=0, doBothPasses=0):
    return TwoPassImageProcessing(input, width, height, useRect, generateMipmaps, useFloat, doBothPasses)

def imageProcessing(input, width, height, useRect=1, useFloat=0, generateMipmaps=0):
    return imageProcessing(input, width, height, useRect, generateMipmaps, useFloat)


from field.graphics.core import BasicTextures
from field.graphics.core.BasicTextures import TextureFromImage
from field.graphics.core.BasicTextures import TextureUnit

def textureFromFile(name, isRectangle=1, unit=0):
    if (isRectangle):
        return TextureUnit(unit, TextureFromImage(name).rectangle())
    else:
        return TextureUnit(unit, TextureFromImage(name).square())

def textureFromImage(canvas, image, width=None, height=None):
    if (not width):
        width = int(image.extents.w)
        height = int(image.extents.h)
    dest = Destination2(width, height)
    dest.setImage(image)
    canvas ** dest
    return dest

def textureFromAccumulator(canvas, image, width=None, height=None):
    if (not width):
        width = int(image.getOutputImage().extents.w)
        height = int(image.getOutputImage().extents.h)
    dest = Destination2(width, height)
    dest.setAccumulator(image)
    canvas ** dest
    return dest




