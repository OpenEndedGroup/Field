from java.util.concurrent import *
from field.util import FieldExecutorService

def SendToField(x):
    """Will cause this function to be executed in the main Field thread 
    (this is the thread that does Animation and Event handling and everything else, block this and you'll block the UI)
    """
    def wrapper(*a, **kw):
        def callable():
            x(*a, **kw)
        return FieldExecutorService.service.submit(callable)
    return wrapper

def Delay(delay):
    """Will cause this function to be executed in the main Field thread 
    (this is the thread that does Animation and Event handling and everything else, block this and you'll block the UI)
    """
    def outer(x):
        def wrapper(*a, **kw):
            def callable():
                x(*a, **kw)
            return FieldExecutorService.service.executeLater(callable, delay)
        return wrapper
    return outer

defaultPool = ThreadPoolExecutor(5, 15, 10, TimeUnit.SECONDS, LinkedBlockingQueue())

def SendToExecutor(x):
    """Will cause this function to be executed at some later point on a separate worker thread. 
    To call back into Field use SendToField
    """
    def wrapper(*a, **kw):
        def callable():
           x(*a, **kw)
        return defaultPool.submit(callable)
    return wrapper



