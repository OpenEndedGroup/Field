
from field.core.plugins.python import PythonPlugin
from field.util import PythonUtils
from field.util.PythonUtils import OKeyByName
from field.graphics.core import Base

import types
from java.lang import System

from FluidTools import *

def marked():
    mm = [x.getVisualElement() for x in attribDict(getSelf(),getSelf()).markingGroup.getSelection()]
    sel = [x.getVisualElement() for x in attribDict(getSelf(),getSelf()).selectionGroup.getSelection()]
    for n in mm:
        if (not n):
            mm.remove(n)
#        if (sel.__contains__(n)):
#            mm.remove(n)
    return mm

def selected():
    mm = [x.getVisualElement() for x in attribDict(getSelf(),getSelf()).markingGroup.getSelection()]
    sel = [x.getVisualElement() for x in attribDict(getSelf(),getSelf()).selectionGroup.getSelection()]
    for n in sel:
        if (not n):
            sel.remove(n)
#        if (mm.__contains__(n)):
#            sel.remove(n)
    return sel


def markedForRoot(root):
    mm = [x.getVisualElement() for x in attribDict(root,root).markingGroup.getSelection()]
    sel = [x.getVisualElement() for x in attribDict(root,root).selectionGroup.getSelection()]
    for n in mm:
        if (not n):
            mm.remove(n)
#        if (sel.__contains__(n)):
#            mm.remove(n)
    return mm

def allOpenRoots():
    return [s.sheet.getRoot() for s in FieldMenus.fieldMenus.allOpenSheets]

def allMarked():
    s = set()
    for x in allOpenRoots():
        for m in markedForRoot(x):
            s.add(m)
    return s
