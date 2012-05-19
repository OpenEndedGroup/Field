from field.core.ui.text.util import IndentationUtils
from field.util.PythonUtils import *
from field.util.PythonUtils import OKeyByName
from telnetlib import *

from field.core.util import AppleScript
from field.core.util import ExecuteCommand

from java.awt import GradientPaint
from java.awt import Color
from java.awt import Graphics2D
from java.awt import BasicStroke
from java.util import *
from field.util import PythonUtils
from FluidTools import getSelf

from java.io import FileWriter
from java.io import File
from java.io import BufferedWriter

import sys

def _getNamespace(back=1):
    dd = {}
    dd.update(globals())
    dd.update(sys._getframe(back).f_locals)
    return dd

def rawPython(a,b,c):
    """Execute the text as a new CPython process"""
    tmpFile = File.createTempFile("fieldRawPython", ".py", None)
    writer = BufferedWriter(FileWriter(tmpFile))
    writer.write(b, 0, len(b))
    writer.close()
    com = ExecuteCommand(".", ("/System/Library/Frameworks/Python.framework/Versions/Current/bin/python", tmpFile.getAbsolutePath()),1)
    com.waitFor(1)
    print com.getOutput()


from TextTransforms import _getNamespace
def bash(*args):
	"""Execute in a bash shell.
	There are two ways to call this, first as just 'bash' this executes the text and prints the output. Secondly, bash("varname"), executes the text and sets varname equal to this output.
	"""
	if (len(args)==3):
		a,b,c = args
		ss = b % _getNamespace(2)
		print ss
		pp = File.createTempFile("field_tmpBash", "").getAbsolutePath()
		f = file(pp, "w")
		print >> f, ss
		f.close()
		ex = ExecuteCommand(".", ["/bin/bash", pp], 1)
		ex.waitFor(1)
		print ex.getOutput()
		return ex.getOutput()
	else:
		def doBash(a,b,c):
			ss = b % _getNamespace(2)
			pp = File.createTempFile("field_tmpBash", "").getAbsolutePath()
			f = file(pp, "w")
			print >> f, ss
			f.close()
			ex = ExecuteCommand(".", ["/bin/bash", pp], 1)
			ex.waitFor(1)
			c[args[0]]=ex.getOutput()
			return ex.getOutput()
		return doBash


u = PythonUtils()

def Once(inside,text,glob):
    """Execute this block once (or until a menu item is selected)"""
    #print "exec: about to look up %s inside %s " % (inside.name, getSelf())
    hasExecuted = getattr(getSelf(), inside.name)
    if (not hasExecuted):
        exec text in glob
    else:
        pass
    setattr(getSelf(), inside.name, 1)


def __Once_menu(inside):
    map = LinkedHashMap()
    def rearm():
        setattr(getSelf(), inside.name,0)
    map.put(u"\u00b0 <b>execute once more</b>", u.asUpdateable(rearm))
    def dont():
        setattr(getSelf(), inside.name,1)
    map.put(u"\u00b0 <b>don't execute</b>", u.asUpdateable(dont))
    return map

def __Once_paint(inside, g, bounds):
    print "paint: about to look up %s inside %s " % (inside.name, getSelf())
    if (not getattr(getSelf(), inside.name)): return
    g = g.create()
    g.setPaint(GradientPaint(bounds.x, bounds.y, Color(0.4, 0.4, 0.4, 0.8), bounds.x, bounds.y+bounds.h, Color(0.3, 0.3, 0.3, 0.5)))
    g.fillRect(int(bounds.x), int(bounds.y), int(bounds.w-15), int(bounds.h))
    g.setColor(Color(1,1,1,0.1))
    g.setStroke(BasicStroke())
    g.clipRect(int(bounds.x),int(bounds.y), int(bounds.w-15), int(bounds.h))
    for n in range(-20, bounds.w, 10):
        g.drawLine(int(bounds.x+n), int(bounds.y+1), int(bounds.x+20+n), int(bounds.y+bounds.h-3))

Once.paint= __Once_paint
Once.menu= __Once_menu

def Initially(inside,text,glob):
    """Execute this block once per Field session (or until a menu item is selected)"""
    hasExecuted = getattr(getSelf(), inside.name+"_")
    if (not hasExecuted):
        exec text in glob
    else:
        pass
    setattr(getSelf(), inside.name+"_", 1)


def __Once_menu(inside):
    map = LinkedHashMap()
    def rearm():
        setattr(getSelf(), inside.name+"_",0)
    map.put(u"\u00b0 <b>execute once more</b>", u.asUpdateable(rearm))
    def dont():
        setattr(getSelf(), inside.name+"_",1)
    map.put(u"\u00b0 <b>don't execute</b>", u.asUpdateable(dont))
    return map

def __Once_paint(inside, g, bounds):
    if (not getattr(getSelf(), inside.name+"_")): return
    g = g.create()
    g.setPaint(GradientPaint(bounds.x, bounds.y, Color(0.4, 0.4, 0.4, 0.8), bounds.x, bounds.y+bounds.h, Color(0.3, 0.3, 0.3, 0.5)))
    g.fillRect(int(bounds.x), int(bounds.y), int(bounds.w-15), int(bounds.h))
    g.setColor(Color(1,1,1,0.1))
    g.setStroke(BasicStroke())
    g.clipRect(int(bounds.x),int(bounds.y), int(bounds.w-15), int(bounds.h))
    for n in range(-20, bounds.w, 10):
        g.drawLine(int(bounds.x+n), int(bounds.y+1), int(bounds.x+20+n), int(bounds.y+bounds.h-3))

Initially.paint= __Once_paint
Initially.menu= __Once_menu

def applescript(inside, text, l, back=2):
    """Execute as AppleScript"""
    script = AppleScript(text % _getNamespace(back), 1)
    print "script:%s"%script.getOutput()

def aftereffects(inside, text, l):
    """Send text to After Effects CS3 as JavaScript"""
    text2 = """
tell application "Adobe After Effects CS3"
DoScript "%s"
end tell
""" % text.replace("\"", "\\\"")
    applescript(inside, text2, l, back=3);

def aftereffects4(inside, text, l):
    """Send text to After Effects CS4 as JavaScript"""
    text2 = """
tell application "Adobe After Effects CS4"
DoScript "%s"
end tell
""" % text.replace("\"", "\\\"")
    applescript(inside, text2, l, back=3);
    

telnetOutput = {}

def telnet(*k, **kw):
    """
    sends text via telnet
    example: telnet(port=2000, host="someotherhost.com")
    """
    if (len(k)==3 and len(kw)==0):
        port=8999
        host="127.0.0.1"
        if (not telnetOutput.has_key((port, host))):
            telnetOutput[(port, host)] = Telnet(host, port)
        _within = k[0]
        _text = k[1]
        _text = _text % _getNamespace(back=2)
        print "sending %s " % _text
        telnetOutput[(port, host)].write(_text)
        return
    port = kw.get("port", 8999)
    host = kw.get("host", "127.0.0.1")

    if (not telnetOutput.has_key((port, host))):
        telnetOutput[(port, host)] = Telnet(host, port)
    def telnettransform(within, _text, g):
        _text = _text % _getNamespace(back=2)
        print "sending %s " % _text
        telnetOutput[(port, host)].write(_text)

    return telnettransform


def telnetMaya(*k, **kw):
    """Sends code (python) to Maya.
    example: telnetMaya(port=8000,host="someotherhost.com",injectGlobals=1)
    """
    if (len(k)==3 and len(kw)==0):
        port=8999
        host="127.0.0.1"
        injectGlobals=1
        if (not telnetOutput.has_key((port, host))):
            telnetOutput[(port, host)] = Telnet(host, port)
        within = k[0]
        _text = k[1]
        g = k[2]
        _text = IndentationUtils.indentTo(0, _text)
        _text = _text.replace("\n","\\n")
        _text = _text.replace("\t","\\t")
        _text = _text.replace("\"","\\\"")
        if (injectGlobals):
            _text = _text % _getNamespace(2)
        _text = "python\"%s\"" % _text
        telnetOutput[(port, host)].write(_text)
        return 
    port = kw.get("port", 8999)
    host = kw.get("host", "127.0.0.1")
    injectGlobals = kw.get("injectGlobals", 1)
    if (not telnetOutput.has_key((port, host))):
        telnetOutput[(port, host)] = Telnet(host, port)
    def telnettransform(within, _text, g):
        _text = IndentationUtils.indentTo(0, _text)
        _text = _text.replace("\n","\\n")
        _text = _text.replace("\t","\\t")
        _text = _text.replace("\"","\\\"")
        if (injectGlobals):
            _text = _text % _getNamespace(2)
        _text = "python\"%s\"" % _text
        telnetOutput[(port, host)].write(_text)
    return telnettransform


def defaultTransform(within, _text, g):
    """ do nothing, just execute the text itself """
    exec _text in _getNamespace(back=2)

def insideGenerator(within, _text, g):
    """ wrap inside generator, and perform """
    genPreamble = """
#from __future__ import generators
def __exit0():
	_environment.exit()
def __enter0():
	_environment.enter()
def __tmp0():
	yield 0
"""
    genPostamble = """
#	globals().update(locals())
u.stackPrePost(__enter0, __tmp0(), _progress, __exit0)
"""
    _text = IndentationUtils.indentTo(1, _text)
    _text = genPreamble+_text+genPostamble
    localLocal = {"_progress":None}
    if (within):
	if (within.get()):
	    if (within.get().reference):
		localLocal["_progress"]=within.get().reference

    exec _text in _getNamespace(back=2), localLocal
