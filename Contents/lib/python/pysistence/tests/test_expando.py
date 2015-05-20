from pysistence import Expando, make_expando_class

def test_is_subclass():
    class foo(object):
        pass
    ExpandoFoo = make_expando_class(foo, 'ExpandoFoo')
    assert issubclass(ExpandoFoo, foo)

def test_class_attr():
    ExpandoCls = make_expando_class(object, 'ExpandoCls', foo='bar')
    assert ExpandoCls.foo == 'bar'

def test_instance_attr():
    ExpandoCls = make_expando_class(object, 'ExpandoCls')
    e = ExpandoCls(foo='bar')
    assert e.foo == 'bar'

def test_to_dict():
    e = Expando()
    actual_dict = e.to_dict()
    expected_dict = e.__dict__
    assert expected_dict == actual_dict

def test_to_dict_copies():
    e = Expando()
    actual_dict = e.to_dict()
    expected_dict = e.__dict__
    assert expected_dict is not actual_dict

def test_to_public_dict():
    e = Expando(_foo='bar')
    actual_dict = e.to_public_dict()
    assert '_foo' not in actual_dict

def test_without():
    e = Expando(foo='bar').without('foo')
    actual_dict = e.to_dict()
    assert 'foo' not in actual_dict

def test_using():
    e = Expando().using(foo='bar')
    assert e.foo == 'bar'
