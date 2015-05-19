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
"""
This module contains the Expando class.
"""

from copy import copy
from pprint import pformat

def make_expando_class(base, name, **kwargs):
    """This function defines an 'expando' class.  An expando class is one that
       allows arbitrary properties to be defined.  To use it, you would want to
       pass in a base class, a name, and arbitrary keyword arguments that define
       properties on the class.  For example, this call:

           ExpandoSomeClass = make_expando_class(SomeClass, 'ExpandoSomeClass,
                                                 x=1, y=2)
       ...is equivalent to the following statement:

           class ExpandoSomeClass(SomeClass):
               x = 1
               y = 2
               def __init__(self, **kwargs):
                   self.__dict__.update(**kwargs)

       Expando classes also allow arbitrary keyworks arguments in constructor
       calls.  For example, the following would set expando.mbr_id equal to 32
       and expando.store to an object:

           expando = ExpandoSomeClass(mbr_id=32, store=object())
    """

    kwargs['__init__'] = init_expando
    kwargs['to_dict'] = expando_to_dict
    kwargs['without'] = expando_without
    kwargs['using'] = expando_using
    kwargs['to_public_dict'] = expando_to_public_dict
    kwargs['__repr__'] = expando_repr
    return type(name, (base,), kwargs)

# stub for backwards compatibility
mkExpandoClass = make_expando_class

def init_expando(self, **instance_kwargs_):
    self.__dict__.update(**instance_kwargs_)

def expando_to_dict(self):
    """
    Get a *copy* of the expando's __dict__.
    """
    original_dict = self.__dict__
    return copy(original_dict)

def expando_to_public_dict(self):
    """
    Get a *copy* of the expando's __dict__ without any keys
    beginning with an underscore
    """
    new_dict = self.to_dict()
    filtered_dict = dict(
        [(key, value) for key, value in new_dict.items()
             if not key.startswith('_')])
    return filtered_dict

def expando_without(self, *args):
    """
    Make a copy of an expando without certain attributes
    """
    new_dict = self.to_dict()
    for attr_name in args:
        del new_dict[attr_name]
    return self.__class__(**new_dict)

def expando_using(self, **kwargs):
    """
    Create a new copy of an expando with additional attributes.
    """
    new_dict = self.to_dict()
    new_dict.update(kwargs)
    return self.__class__(**new_dict)    

def expando_repr(self):
    return pformat(self.__dict__)

Expando = make_expando_class(object, 'Expando')
