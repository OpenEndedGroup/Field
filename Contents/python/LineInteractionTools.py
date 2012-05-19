from field.core.plugins.drawing.opengl import LineInteraction
from java.util import *
from FluidTools import getSelf

class Eventer(LineInteraction.EventHandler):
    def __init__(self):
        self.begun = 0

    def begin(self):
        self.restoreTo = [ArrayList(getSelf().lines or [])]
        self.begun = 1

    def restore(self):
        print "restore to is %s " % self.restoreTo
        if (len(self.restoreTo)>0):
            getSelf().lines.clear()
            pp = self.restoreTo.pop()
            print "popped %s " % pp
            getSelf().lines.addAll(pp)
            print "lines are %s" % getSelf().lines

    def save(self):
        self.restoreTo.append(ArrayList(getSelf().lines or [] ))

        print "restore to is now %s " % self.restoreTo
    
