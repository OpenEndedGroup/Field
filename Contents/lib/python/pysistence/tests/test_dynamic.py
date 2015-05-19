from pysistence.dynamic import Parameter

def test_simple_case():
    p = Parameter(1)
    assert p.value == 1

def test_one_change():
    p = Parameter(1)
    p.push(2)
    assert p.value == 2

def test_push_pop():
    p = Parameter(1)
    p.push(2)
    p.pop()
    assert p.value == 1

def test_within_context_manager():
    p = Parameter(1)
    with p.parameterize(2):
        assert p.value == 2

def test_outside_context_manager():
    p = Parameter(1)
    with p.parameterize(2):
        pass
    assert p.value == 1
    
