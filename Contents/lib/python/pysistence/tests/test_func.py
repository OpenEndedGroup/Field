import unittest
import functools
import operator
import pysistence.func as func

class TestFlip(unittest.TestCase):
    def test_flip(self):
        def myfunc(a, b):
            return (a, b)
        result = func.flip(myfunc)(1, 2)
        self.assertEqual(result, (2, 1))

    def test_flip_with_more_args(self):
        def myfunc(a, b, c):
            return a, b, c
        result = func.flip(myfunc)(1, 2, 3)
        self.assertEqual(result, (2, 1, 3))

class TestConst(unittest.TestCase):
    def test_const(self):
        myfunc = func.const(1)
        # pass in arbitrary arguments just to make sure we can handle those
        self.assertEqual(myfunc(1, 2, foo="bar"), 1)

class TestCompose(unittest.TestCase):
    def setUp(self):
        self.add_to_hello = functools.partial(operator.add, "Hello, ")

    def test_compose_twoarg(self):
        hellocat = func.compose(str, self.add_to_hello)
        self.assertEqual(hellocat(1), "Hello, 1")

    def test_compose_threearg(self):
        append_exclamation_point = (functools.partial(func.flip(operator.add), "!"))
        make_hello = func.compose(str, self.add_to_hello, append_exclamation_point)
        self.assertEqual(make_hello(1), "Hello, 1!")

class TestIdentity(unittest.TestCase):
    def test_identity_one_arg(self):
        result = func.identity(1)
        self.assertEqual(result, 1)

    def test_identity_multi_arg(self):
        result = func.identity(1, 2)
        self.assertEqual(result, (1, 2))

    def test_identity_no_arg(self):
        result = func.identity()
        self.assertEqual(result, ())

class TestTrampoline(unittest.TestCase):
    def test_no_func(self):
        # Test that if no function is given, trampoline is basically a glorified
        # apply.
        def myfunc():
            return 1
        self.assertEqual(func.trampoline(myfunc), 1)

    def test_with_other_functions(self):
        def myfunc1():
            return myfunc2

        def myfunc2():
            return myfunc3

        def myfunc3():
            return 1

        self.assertEqual(func.trampoline(myfunc1), 1)

    def test_with_args(self):
        def myfunc(a):
            return a
        self.assertEqual(func.trampoline(myfunc, 1), 1)
