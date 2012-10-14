import cPickle
from java.lang import System

def safeText(t):
    t = t.replace(u"\u003b", ":")
    t = t.replace("<", "")
    t = t.replace(">", "")
    if (len(t)>250):
        t = t[:250]
    return t

x = "a"


def completionsFor(text):
    if (text==""):
        m = globals()
        d = globals().keys()
        f = 1
    else:
        m = eval(text)
        d = dir(m)
        f = 0
    ret = []
    for dd in d:
        try:
            if (f):
                a = m[dd]
            else:
                a = getattr(m, dd)
            if (hasattr(a, "im_func") and hasattr(a.im_func, "argslist")):
                for arglist in a.im_func.argslist:
                    data = "%s"%arglist.data
                    data = data[data.index("(")+1: data.rindex(")")]
                    data = data.replace(",", ", ")
                    ret.append(["javamethod", safeText("%s"%dd), safeText(data)])
            elif (inspect.isroutine(a)):
                a,b,c,d = inspect.getargspec(a)
                formatted = inspect.formatargspec(a,b,c,d)
                formatted = formatted.replace("self, ", "")
                formatted = formatted.replace("(self)", "()")
                ret.append(["pythonmethod", safeText("%s"%(dd)), safeText("%s"%formatted)])
            else:
                ret.append(["field", safeText("%s" % type(a)), safeText("%s" % (dd)), safeText("%s" % a)])
        except : 
            pass

    return ret

def evalxvalue(_, text, ret):
    _.getRoot().out.simpleSend(ret, ("%s" %cPickle.dumps(completionsFor((text))),))

from types import GeneratorType
from types import TupleType
from types import IntType

def runGenerator(_, a, defaultDelay=1000):
	stack = [a]
	def shim(*a):
		delay = defaultDelay
		if (len(stack)==0): return
		top = stack.pop()
		try:
			rr = top.next()
			if (type(rr)==GeneratorType):
				stack.append(top)
				stack.append(rr)
			elif (type(rr)==IntType):
				stack.append(top)
				delay = rr
			elif (type(rr)==TupleType):
				stack.append(top)
				stack.append(rr[1])
				delay = rr[0]
		except StopIteration:
			pass
		if (len(stack)>0):
			_.callInDelay(delay, shim, ())
	_.callInDelay(1, shim, ())

import inspect
