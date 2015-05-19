# Copyright (c) 2009 Jason M Baker
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.

def not_implemented_method(*args, **kwargs):
    raise NotImplementedError('Cannot set values in a PDict')    

class PDict(dict):
    __setitem__ = not_implemented_method
    __delitem__ = not_implemented_method
    update = not_implemented_method
    clear = not_implemented_method
    pop = not_implemented_method
    popitem = not_implemented_method
    def _as_transient(self):
        return dict(self)
    
    def copy(self):
        return PDict(self)

    def without(self, *keys):
        new_dict = self._as_transient()
        for key in keys:
            del new_dict[key]
        return PDict(new_dict)

    def using(self, **kwargs):
        new_dict = self._as_transient()
        new_dict.update(kwargs)
        return PDict(new_dict)
    
make_dict = PDict
    
