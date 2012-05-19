import sys

srcfile = sys.argv[1]
scale = sys.argv[3]
dest = sys.argv[2]

from CoreGraphics import *
from CoreGraphics import _CoreGraphics
from CoreGraphics._CoreGraphics import *

pdf = CGPDFDocumentCreateWithProvider (CGDataProviderCreateWithFilename (srcfile))
mediaBox = pdf.getPage(1).getBoxRect(kCGPDFMediaBox)

print "raster arguments:",sys.argv
scale = int(scale)
print "raster size",mediaBox.getWidth(),mediaBox.getHeight()

aa = CGFloatArray(4)
back = (0,0,0,0)
if (len(sys.argv)>4):
    back = (float(sys.argv[4]),float(sys.argv[5]),float(sys.argv[6]),float(sys.argv[7]))

#right now you can't pass in 'back' here, and you can't set elements in a CGFloatArray. Thanks Apple.

#context = CGBitmapContextCreateWithColor(int(mediaBox.getWidth()*scale),int(mediaBox.getHeight()*scale), CGColorSpaceCreateDeviceRGB(), aa)
context2 = CGBitmapContextCreateWithColor(int(mediaBox.getWidth()*scale),int(mediaBox.getHeight()*scale),  CGColorSpaceCreateWithName( kCGColorSpaceGenericRGB ), aa)
context2.drawPDFDocument(CGRectMake(0,0,int(mediaBox.getWidth()*scale),int(mediaBox.getHeight()*scale)),pdf, 1)
context2.writeToFile(dest, kCGImageFormatTIFF)


