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

from nose.tools import raises

from pysistence import make_dict
from pysistence.persistent_dict import PDict

def test_getitem():
    d = make_dict(foo='bar')
    assert d['foo'] == 'bar'

@raises(NotImplementedError)
def test_setitem():
    d = make_dict(foo='bar')
    d['bar'] = 'asdf'

@raises(NotImplementedError)
def test_update():
    d = make_dict(foo='bar')
    d.update({'bar' : 'asdf'})

@raises(NotImplementedError)
def test_clear():
    d = make_dict(foo='bar')
    d.clear()

@raises(NotImplementedError)
def test_pop():
    d = make_dict(foo='bar')
    d.pop('foo')

@raises(NotImplementedError)
def test_popitem():
    d = make_dict(foo='bar')
    d.popitem()

def test_copy_value():
    d = make_dict(foo='bar')
    d2 = d.copy()
    assert d == d2

def test_copy_returns_PDict():
    d = make_dict(foo='bar')
    d2 = d.copy()
    assert isinstance(d2, PDict)
    
def test_without():
    d = make_dict(foo='bar')
    empty_d = d.without('foo')
    assert empty_d == {}

def test_without_copies():
    d = make_dict(foo='bar')
    empty_d = d.without('foo')
    assert empty_d is not d

def test_using_value():
    d = make_dict()
    new_dict = d.using(foo='bar')
    assert new_dict['foo'] == 'bar'

def test_using_copies():
    d = make_dict()
    new_dict = d.using(foo='bar')
    assert new_dict is not d
