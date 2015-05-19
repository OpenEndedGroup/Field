#include <Python.h>
#include <structmember.h>

static PyTypeObject PListType;
static PyTypeObject PListIterType;

typedef struct PListStruct{
  PyObject_HEAD
  PyObject *first;
  struct PListStruct *rest;
} PList;

typedef struct PListIterStruct{
  PyObject_HEAD
  PList *head;
} PListIter;

static PyMemberDef plist_members[] = {
  {"first", T_OBJECT_EX, offsetof(PList, first), READONLY, "First element"},
  {"rest", T_OBJECT_EX, offsetof(PList, rest), READONLY, "Rest of the list"},
  {NULL}
};

void print(const char *str)
{
  printf(str);
  printf("\n");
  fflush(stdout);
}

void pyprint(PyObject* obj)
{
  PyObject_Print(obj, stdout, 0);
  printf("\n");
  fflush(stdout);
}

static PyObject *
PList_cons(PList *self, PyObject *arg)
{
  PList *new_list = (PList*)PyObject_CallFunctionObjArgs((PyObject*)self->ob_type, 
                                                         arg, self, NULL);
  if (new_list) {
    Py_INCREF(new_list);
    return (PyObject*)new_list;
  }
  else {
    return NULL;
  }
}

static PyMethodDef plist_methods[] = {
  {"cons", (PyCFunction)PList_cons, METH_O, "Add an item to the list"},
  {NULL}
};


static int
PList_init(PList *self, PyObject *args, PyObject *kwds)
{
  PyObject *first=NULL, *rest=NULL, *tmp;

  static char *kwlist[] = {"first", "rest", NULL};
  if (! PyArg_ParseTupleAndKeywords(args, kwds, "O|O", kwlist,
                                    &first, &rest))
    return -1;

  if (first){
    tmp = first;
    Py_INCREF(first);
    self->first = first;
    Py_XDECREF(tmp);
  }
  else{
    return -1;
  }

  if (rest) {
    tmp = (PyObject*)self->rest;
    Py_INCREF(rest);
    self->rest = (PList*)rest;
    Py_XDECREF(tmp);
  }
  else {
    Py_INCREF(Py_None);
    // Could be BAD!
    self->rest = (PList*)Py_None;
  }

  return 0;
}

static int
PList_traverse(PList *self, visitproc visit, void *arg)
{
  Py_VISIT(self->first);
  Py_VISIT(self->rest);
  return 0;
}

static int
PList_clear(PList *self)
{
  Py_CLEAR(self->first);
  Py_CLEAR(self->rest);
  return 0;
}

static void PList_dealloc(PList *self)
{
  PList_clear(self);
  self->ob_type->tp_free((PyObject*)self);
}

static PyObject*
PList_iter(PList *self)
{
  PListIter *iter = PyObject_New(PListIter, &PListIterType);
  Py_XINCREF(iter);
  iter->head = self;
  Py_INCREF(self);
  return (PyObject*)iter;
}

static PyTypeObject PListType= {
    PyObject_HEAD_INIT(NULL)
    0,                         /*ob_size*/
    "pysistence._persistent_list.PList",             /*tp_name*/
    sizeof(PList), /*tp_basicsize*/
    0,                         /*tp_itemsize*/
    (destructor)PList_dealloc,                         /*tp_dealloc*/
    0,                         /*tp_print*/
    0,                         /*tp_getattr*/
    0,                         /*tp_setattr*/
    0,                         /*tp_compare*/
    0,                         /*tp_repr*/
    0,                         /*tp_as_number*/
    0,                         /*tp_as_sequence*/
    0,                         /*tp_as_mapping*/
    0,                         /*tp_hash */
    0,                         /*tp_call*/
    0,                         /*tp_str*/
    PyObject_GenericGetAttr,                         /*tp_getattro*/
    PyObject_GenericSetAttr,                         /*tp_setattro*/
    0,                         /*tp_as_buffer*/
    Py_TPFLAGS_DEFAULT 
    | Py_TPFLAGS_BASETYPE
    | Py_TPFLAGS_HAVE_GC,        /*tp_flags*/
    "Persistent list",           /* tp_doc */
    (traverseproc)PList_traverse,                       /*New tp_traverse */
    (inquiry)PList_clear,                       /* tp_clear */
    0,                       /* tp_richcompare */
    0,                       /* tp_weaklistoffset */
    (getiterfunc)PList_iter,                       /* tp_iter */
    0,                       /* tp_iternext */
    plist_methods,          /* tp_methods */
    plist_members,                       /* tp_members */
    0,                       /* tp_getset */
    0,                       /* tp_base */
    0,                       /* tp_dict */
    0,                       /* tp_descr_get */
    0,                       /* tp_descr_set */
    0,                       /* tp_dictoffset */
    (initproc)PList_init,   /* tp_init */
    0,                       /* tp_alloc */
    0,                       /* tp_new */
};


static PyObject*
PListIter_iter(PyObject *o)
{
  Py_INCREF(o);
  return o;
}

static PyObject*
PListIter_next(PListIter *o)
{
  PList *head, *nextval; PList *rest;
  if (o->head == Py_None){
    PyErr_SetNone(PyExc_StopIteration);
    return NULL;
  }
  head = o->head;
  nextval = (PyObject *)o->head->first;
  Py_INCREF(nextval);
  rest = o->head->rest;
  Py_INCREF(rest);
  o->head = rest;
  Py_XDECREF(head);
  return (PyObject *)nextval;
}

static void PListIter_dealloc(PListIter *self)
{
  Py_XDECREF(self->head);
  self->ob_type->tp_free((PyObject*)self);
}

static PyMemberDef PListIterTypeMembers[]={
  {NULL}
};

static PyMethodDef PListIterTypeMethods[]={
  {NULL}
};

static PyTypeObject PListIterType= {
    PyObject_HEAD_INIT(NULL)
    0,                         /*ob_size*/
    "pysistence._persistent_list.PListIter",             /*tp_name*/
    sizeof(PListIter), /*tp_basicsize*/
    0,                         /*tp_itemsize*/
    (destructor)PListIter_dealloc,                         /*tp_dealloc*/
    0,                         /*tp_print*/
    0,                         /*tp_getattr*/
    0,                         /*tp_setattr*/
    0,                         /*tp_compare*/
    0,                         /*tp_repr*/
    0,                         /*tp_as_number*/
    0,                         /*tp_as_sequence*/
    0,                         /*tp_as_mapping*/
    0,                         /*tp_hash */
    0,                         /*tp_call*/
    0,                         /*tp_str*/
    0,                         /*tp_getattro*/
    0,                         /*tp_setattro*/
    0,                         /*tp_as_buffer*/
    Py_TPFLAGS_DEFAULT
    | Py_TPFLAGS_HAVE_ITER,        /*tp_flags*/
    "Persistent list iterator",           /* tp_doc */
    0,                       /* tp_traverse */
    0,                       /* tp_clear */
    0,                       /* tp_richcompare */
    0,                       /* tp_weaklistoffset */
    (getiterfunc)PListIter_iter,                       /* tp_iter */
    (iternextfunc)PListIter_next,                       /* tp_iternext */
    PListIterTypeMethods,                       /* tp_methods */
    PListIterTypeMembers,    /* tp_members */
    
};

#ifndef PyMODINIT_FUNC
#define PyMODINIT_FUNC void
#endif

PyMODINIT_FUNC
init_persistent_list(void)
{
  PyObject *m;

  PListType.tp_new = PyType_GenericNew;
  if (PyType_Ready(&PListType) < 0)
    return;

  PListType.tp_new = PyType_GenericNew;
  if (PyType_Ready(&PListIterType) < 0)
    return;

  m = Py_InitModule3("pysistence._persistent_list", 0,
                     "Docstring");
  Py_INCREF(&PListType);
  Py_INCREF(&PListIterType);
  PyModule_AddObject(m, "PList", (PyObject*)&PListType);
  PyModule_AddObject(m, "PListIter", (PyObject*)&PListIterType);
}

