"""
A set of common functional programming utility functions.
"""

from functools import wraps, partial

def flip(func):
    """Returns a function that calls func flipping its first two arguments.
       Note that the returned function will not accept keyword arguments."""
    @wraps(func)
    def wrapper(arg1, arg2, *args):
        return func(arg2, arg1, *args)
    return wrapper

def const(retval):
    """Returns a function that always returns the same value, no matter what
       arguments it is given."""
    def constfunc(*args, **kwargs):
        return retval
    return constfunc

def compose(func1, *funcs):
    """Compose several functions together."""
    class ComposedFunc(object):
        _funcs = (func1,) + funcs
        __doc__ = "Function composed from {0}".format(_funcs)

        def __call__(self, *args, **kwargs):
            result = func1(*args, **kwargs)
            for func in funcs:
                result = func(result)
            return result

        def __repr__(self):
            return "<function composed from {0}>".format(self._funcs)
    return ComposedFunc()

def identity(*args):
    """Function that returns what is passed in.  If one item is given, that item
       will be returned.  Otherwise, a tuple of the arguments will be passed in."""
    if len(args) == 1:
        return args[0]
    else:
        return args

def trampoline(func, *args, **kwargs):
    """Calls func with the given arguments.  If func returns another function,
       it will call that function and repeat this process until a non-callable
       is returned."""
    result = func(*args, **kwargs)
    while callable(result):
        result = result()
    return result
