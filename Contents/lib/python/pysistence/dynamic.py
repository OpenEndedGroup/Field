"""Dynamic scoping emulation"""
import threading

class Parameter(object):
    ns = threading.local()
    def __init__(self, base_value):
        self.base_value = base_value
        self.ns.context_stack = []

    @property
    def value(self):
        if self.ns.context_stack:
            return self.ns.context_stack[-1]
        return self.base_value

    def push(self, value):
        self.ns.context_stack.append(value)

    def pop(self):
        self.ns.context_stack.pop()

    def parameterize(self, newvalue):
        class ParameterContext(object):
            def __enter__(self_):
                self.push(newvalue)
                return newvalue

            def __exit__(self_, exc_type, exc_val, exc_tb):
                self.pop()
                return False
            
        return ParameterContext()
