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

from pysistence import make_list
from pysistence.persistent_list import PList
from pysistence.exceptions import ItemNotFoundError

def test_construct():
    ls = PList(1)
    assert ls.first == 1

def test_cons():
    ls = PList(1)
    ls = ls.cons(2)
    first_item = ls.first
    assert first_item == 2

def test_iter():
    ls = PList(1)
    ls = ls.cons(2)
    items = list(iter(ls))
    assert items == [2, 1]

def test_rest():
    ls = make_list(1, 2)
    assert 2 == ls.rest.first

def test_frest():
    ls = make_list(1, 2)
    assert 2 == ls.frest

def test_convenience():
    ls = make_list(1,2,3)
    items = list(iter(ls))
    assert items == [1,2,3]

def test_without():
    ls = make_list(1,2,3)
    assert 2 not in ls.without(2)

def test_contains():
    ls = make_list(1,2,3)
    def check_number(num):
        assert num in ls

    for number in ls:
        yield check_number, number
        
def test_contains_negative():
    ls = make_list(1, 2, 3)
    assert 5 not in ls

def test_equals():
    assert make_list(1, 2, 3) == make_list(1, 2, 3)

def test_equals_noniterable():
    assert not make_list(1, 2, 3) == 1
    
def test_equals_empty():
    assert not make_list(1, 2, 3) == make_list()

def test_equals_uneven():
    assert not make_list(1, 2, 3) == make_list(1, 2)

def test_concat():
    list1 = make_list(1, 2)
    list2 = make_list(3, 4)
    actual_list = list1 + list2
    expected_list = make_list(1, 2, 3, 4)
    assert actual_list == expected_list

def test_reverse():
    test_list = make_list(1,2,3,4)
    actual_list = test_list.reverse()
    expected_list = make_list(4,3,2,1)
    assert actual_list == expected_list
    
def test_replace():
    test_list = make_list(1,2,3,4)
    actual_list = test_list.replace(old=1, new=0)
    expected_list = make_list(0,2,3,4)
    assert actual_list == expected_list

@raises(ItemNotFoundError)
def test_replace_raises_error_on_nonexistant_old():
    test_list = make_list(1,2,3,4)
    test_list.replace(old=1000, new=1)
