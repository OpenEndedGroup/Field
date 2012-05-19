import xmlrpclib

_orig_Method = xmlrpclib._Method

class KeywordArgMethod(_orig_Method):
    def __call__(self, *args, **kwargs):
        aa = []
        aa += args
        aa.append(kwargs)
        return _orig_Method.__call__(self, *tuple(aa))

xmlrpclib._Method = KeywordArgMethod

def dictXMLRPCClient(url="http://localhost:8999"):
    return xmlrpclib.ServerProxy(url)

