package field.core.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.python.core.Py;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.core.PyException;
import org.python.core.PyFloat;
import org.python.core.PyJavaType;
import org.python.core.PyObject;
import org.python.core.PyObjectDerived;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.adapter.ClassAdapter;
import org.python.core.adapter.ExtensiblePyObjectAdapter;
import org.python.core.adapter.PyObjectAdapter;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.drawing.FieldGraphics2D;
import field.core.plugins.drawing.SplineComputingOverride;
import field.core.plugins.drawing.SplineComputingOverride.PLineList;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.python.PythonPlugin;
import field.core.util.FieldPyObjectAdaptor.iCallable;
import field.core.util.FieldPyObjectAdaptor.iCallable_keywords;
import field.core.util.FieldPyObjectAdaptor.iExtensible;
import field.core.util.FieldPyObjectAdaptor.iHandlesAttributes;
import field.core.util.FieldPyObjectAdaptor.iHandlesDeletionOfAttributes;
import field.core.util.FieldPyObjectAdaptor.iHandlesFindItem;
import field.math.abstraction.iHasScalar;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.Generics.Pair;
import field.namespace.generic.Generics.Triple;
import field.util.Dict;
import field.util.Dict.Prop;

/**
 * new style adaptation for new style java classes. Jython 2.5b1.
 * 
 * @author marc
 * 
 */
public class FieldPyObjectAdaptor2 {

	static protected boolean initialized = false;
	public static FieldPyObjectAdaptor2 fieldPyObjectAdaptor;
	public static ExtensiblePyObjectAdapter adaptor;

	public static void initialize() {
		if (!initialized) {

			System.out.println(" initializing adaptor2 ");

			ExtensiblePyObjectAdapter a = Py.getAdapter();
			fieldPyObjectAdaptor = new FieldPyObjectAdaptor2().install(a);
			adaptor = a;
			initialized = true;
		}
	}

	static public Map<String, PyObject> injectedSelfMethods = new HashMap<String, PyObject>();

	static public void isCallable(Class<? extends iCallable> c) {
		// iCallable
		// ---------------------------------------------------------------------
		PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__call__") {
			@Override
			public PyObject __call__(PyObject[] args, String[] kw) {
				Object[] x = new Object[args.length];
				for (int i = 0; i < x.length; i++) {
					x[i] = args[i].__tojava__(Object.class);
				}

				iCallable cc = (iCallable) Py.tojava(self, iCallable.class);
				Object r = null;
				if (cc instanceof iCallable_keywords) {
					
					Map<String, Object> kwmap = new HashMap<String, Object>();
					for(int i=0;i<kw.length;i++)
					{
						kwmap.put(kw[i], x[x.length-kw.length+i]);
					}
					
					Object[] x2 = new Object[args.length-kw.length];
					System.arraycopy(x, 0, x2, 0, x2.length);
					
					r = ((iCallable_keywords)cc).callWithKeywords(x2, kwmap);
				} else {
					r = cc.call(x);
				}
				PyObject p = Py.java2py(r);
				return p;
			}
		};
		PyType.fromClass(c).addMethod(meth);
	}

	static public void isExtensible(Class<? extends iExtensible> c) {
		// iCallable
		// ---------------------------------------------------------------------
		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__getattribute__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__getattribute__", 1) {
				@Override
				public PyObject __call__(PyObject name) {
					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {
						if (!Py.matchException(e, Py.AttributeError)) {
							throw e;
						}
					}
					iExtensible d = Py.tojava(self, iExtensible.class);
					PyObject r = Py.java2py(d.getDict().get(new Prop((String) Py.tojava(name, String.class))));
					return r;
				}
			};
			PyType.fromClass(c).addMethod(meth);
		}
		{
			final PyObject oldset = PyObject.TYPE.__getattr__("__setattr__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setattr__", 2) {
				@Override
				public PyObject __call__(PyObject name, PyObject to) {
					try {
						return oldset.__call__(self, name, to);
					} catch (PyException e) {
						if (!Py.matchException(e, Py.AttributeError)) {
							throw e;
						}
					}

					iExtensible d = Py.tojava(self, iExtensible.class);

					d.getDict().put(new Prop((String) Py.tojava(name, String.class)), Py.tojava(to, Object.class));
					return Py.None;
				}
			};
			PyType.fromClass(c).addMethod(meth);
		}
	}

	static public void isHandlesAttributes(Class<? extends iHandlesAttributes> c) {
		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__getattribute__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__getattribute__", 1) {
				@Override
				public PyObject __call__(PyObject name) {

					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {

						if (!Py.matchException(e, Py.AttributeError)) {
							throw e;
						}
					}
					iHandlesAttributes d = Py.tojava(self, iHandlesAttributes.class);
					PyObject r = Py.java2py(d.getAttribute(((String) Py.tojava(name, String.class))));
					return r;
				}
			};
			PyType.fromClass(c).addMethod(meth);
		}
		{
			final PyObject oldset = PyObject.TYPE.__getattr__("__setattr__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setattr__", 2) {
				@Override
				public PyObject __call__(PyObject name, PyObject to) {

					PyType t = ((PyObjectDerived) self).getType();

					if (!t.getModule().toString().contains("__main__"))

						try {
							return oldset.__call__(self, name, to);
						} catch (PyException e) {
							if (!Py.matchException(e, Py.AttributeError)) {
								throw e;
							}
						}

					iHandlesAttributes d = Py.tojava(self, iHandlesAttributes.class);

					d.setAttribute(((String) Py.tojava(name, String.class)), Py.tojava(to, Object.class));
					return Py.None;
				}
			};
			PyType.fromClass(c).addMethod(meth);
		}
	}

	static public void isHandlesFindItem(Class<? extends iHandlesFindItem> c) {
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__finditem__", 1) {
				@Override
				public PyObject __call__(PyObject name) {
					iHandlesFindItem d = Py.tojava(self, iHandlesFindItem.class);
					PyObject r = Py.java2py(d.getItem(((Object) Py.tojava(name, Object.class))));
					return r;
				}
			};
			PyType.fromClass(c).addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__getitem__", 1) {
				@Override
				public PyObject __call__(PyObject name) {
					iHandlesFindItem d = Py.tojava(self, iHandlesFindItem.class);
					PyObject r = Py.java2py(d.getItem(((Object) Py.tojava(name, Object.class))));
					return r;
				}
			};
			PyType.fromClass(c).addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setitem__", 2) {
				@Override
				public PyObject __call__(PyObject name, PyObject to) {
					iHandlesFindItem d = Py.tojava(self, iHandlesFindItem.class);

					d.setItem(((Object) Py.tojava(name, Object.class)), Py.tojava(to, Object.class));
					return Py.None;
				}
			};
			PyType.fromClass(c).addMethod(meth);
		}
	}

	static public class PairToPyTuple extends ClassAdapter {

		public PairToPyTuple() {
			super(Pair.class);
		}

		public PyObject adapt(Object o) {
			Pair p = (Pair) o;
			return new PyTuple(new PyObject[] { Py.java2py(p.left), Py.java2py(p.right) });
		}

	}

	static public class TripleToPyTuple extends ClassAdapter {

		public TripleToPyTuple() {
			super(Triple.class);
		}

		public PyObject adapt(Object o) {
			Triple p = (Triple) o;
			return new PyTuple(new PyObject[] { Py.java2py(p.left), Py.java2py(p.middle), Py.java2py(p.right) });
		}

	}

	protected FieldPyObjectAdaptor2 install(ExtensiblePyObjectAdapter ex) {

		isCallable(iCallable.class);
		isExtensible(iExtensible.class);
		isHandlesAttributes(iHandlesAttributes.class);
		isHandlesFindItem(iHandlesFindItem.class);

		ex.add(new PairToPyTuple());
		ex.add(new TripleToPyTuple());

		// Dict
		// ------------------------------------------------------------------------------
		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__getattribute__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__getattribute__", 1) {
				@Override
				public PyObject __call__(PyObject name) {
					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {

						if (!Py.matchException(e, Py.AttributeError)) {
							throw e;
						}
					}
					Dict d = Py.tojava(self, Dict.class);
					PyObject r = Py.java2py(d.get(new Prop((String) Py.tojava(name, String.class))));
					return r;
				}
			};
			PyType.fromClass(Dict.class).addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setattr__", 2) {
				@Override
				public PyObject __call__(PyObject name, PyObject to) {
					Dict d = Py.tojava(self, Dict.class);

					d.put(new Prop((String) Py.tojava(name, String.class)), Py.tojava(to, Object.class));
					return Py.None;
				}
			};
			PyType.fromClass(Dict.class).addMethod(meth);
		}

		// iExtensible
		// -------------------------------------------------------------------------------

		// iHandlesAttribtues
		// ---------------------------------------------------------------------------

		// iHandlesFindItem
		// ---------------------------------------------------------------------------

		// iHasScalar
		// -------------------------------------------------------------------------------------------------
		ex.addPreClass(new HasScalarToPyHasScalar());

		// iVisualElement
		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__getattribute__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__getattribute__", 1) {
				@Override
				public PyObject __call__(PyObject nn) {

					String name = Py.tojava(nn, String.class);

					PyObject alt = null;

					try {
						alt = objectGetattribute.__call__(self, nn);
					} catch (PyException e) {
						if (!Py.matchException(e, Py.AttributeError)) {
							throw e;
						}
					}

					final iVisualElement d = Py.tojava(self, iVisualElement.class);

					final PyObject found = injectedSelfMethods.get(name);
					if (found != null) {
						return Py.java2py(new iCallable() {

							public Object call(Object[] args) {
								PyObject[] args2 = new PyObject[args.length + 1];
								for (int i = 0; i < args.length; i++) {
									args2[i + 1] = Py.java2py(args[i]);
								}
								args2[0] = self;

								return found.__call__(args2);
							}
						});
					}

					if (alt == null || alt == Py.None || alt.__tojava__(Object.class) instanceof VisualElementProperty || name.equals("frame")) {
						Object o = PythonPlugin.getAttr(d, name);

						if (o == null)
							return Py.None;
						return Py.java2py(o);
					} else
						return alt;
				}
			};

			PyType.fromClass(iVisualElement.class).addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setattr__", 2) {
				@Override
				public PyObject __call__(PyObject n, PyObject value) {
					String name = Py.tojava(n, String.class);
					iVisualElement contents = Py.tojava(self, iVisualElement.class);
					if (name.equals("frame")) {
						contents.getProperty(iVisualElement.overrides).shouldChangeFrame(contents, ((Rect) value.__tojava__(Rect.class)), contents.getFrame(null), true);
					} else
						PythonPlugin.setAttr(contents, contents, name, value.__tojava__(Object.class));

					return Py.None;
				}
			};
			PyType.fromClass(iVisualElement.class).addMethod(meth);
		}

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__delattr__", 1) {
				@Override
				public PyObject __call__(PyObject n) {
					String name = Py.tojava(n, String.class);
					iVisualElement contents = Py.tojava(self, iVisualElement.class);

					contents.deleteProperty(new VisualElementProperty(name));

					return Py.None;
				}
			};
			PyType.fromClass(iVisualElement.class).addMethod(meth);
		}
		
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__delattr__", 1) {
				@Override
				public PyObject __call__(PyObject n) {
					String name = Py.tojava(n, String.class);
					iHandlesDeletionOfAttributes contents = Py.tojava(self, iHandlesDeletionOfAttributes.class);

					contents.deleteAttribute(name);

					return Py.None;
				}
			};
			PyType.fromClass(iHandlesDeletionOfAttributes.class).addMethod(meth);
		}
		
		
		

		
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__call__", 0, 1) {
				@Override
				public PyObject __call__(PyObject[] args, String[] kw) {

					iVisualElement d = Py.tojava(self, iVisualElement.class);
					if (args.length == 0)
						SplineComputingOverride.executeMain(d);
					if (args.length == 1)
						SplineComputingOverride.executeMainWithLabel(d, Py.tojava(args[0], String.class));

					return Py.None;
				}
			};
			PyType.fromClass(iVisualElement.class).addMethod(meth);
		}

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__call__", 0, 1) {
				@Override
				public PyObject __call__(PyObject[] args, String[] kw) {

					iFunction d = Py.tojava(self, iFunction.class);
					return Py.java2py(d.f(Py.tojava(args[0], Object.class)));

				}
			};
			PyType.fromClass(iFunction.class).addMethod(meth);
		}

		PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__ilshift__", 1) {
			@Override
			public PyObject __call__(PyObject composeWith) {

				PLineList s = Py.tojava(self, PLineList.class);

				Object m = composeWith.__tojava__(CachedLine.class);
				if (m != Py.NoConversion) {
					s.add(m);
					return self;
				} else {
					m = composeWith.__tojava__(Collection.class);
					if (m != Py.NoConversion) {
						s.addAll((Collection) m);
						return self;
					} else {
						PyObject drawMethod = composeWith.__findattr__("draw");
						if (drawMethod != null) {
							FieldGraphics2D g = new FieldGraphics2D();
							drawMethod.__call__(Py.java2py(g));
							s.addAll(g.getGeometry());
							return self;
						}
					}
				}

				throw Py.TypeError(" can't figure out how to turn a <" + composeWith + "> into geometry");
			}
		};
		PyType.fromClass(PLineList.class).addMethod(meth);

		
		
		// {
		// final PyBuiltinMethodNarrow meth = new
		// PyBuiltinMethodNarrow("__tojava__", 1, 1) {
		// @Override
		// public PyObject __call__(PyObject o) {
		//
		// PyObject s = this.getSelf();
		// try {
		// Method m = PyObject.class.getDeclaredMethod("getJavaProxy");
		// m.setAccessible(true);
		// Vector4 actual = (Vector4) m.invoke(s);
		//
		// if (((PyType) o).getName().equals("int")) {
		// return Py.java2py(actual.toInt());
		// }
		// return s;
		// } catch (IllegalArgumentException e) {
		// } catch (SecurityException e) {
		// } catch (IllegalAccessException e) {
		// } catch (InvocationTargetException e) {
		// } catch (NoSuchMethodException e) {
		// }
		//
		// return Py.None;
		// }
		// };
		// PyType.fromClass(Vector4.class).addMethod(meth);
		// }
		// {
		// final PyBuiltinMethodNarrow meth = new
		// PyBuiltinMethodNarrow("__coerce__", 1, 1) {
		// @Override
		// public PyObject __call__(PyObject o) {
		//
		// System.out.println(" inside coerce")
		//
		// return Py.None;
		// }
		// };
		// PyType.fromClass(Vector4.class).addMethod(meth);
		// }

		return this;
	}

	static public class HasScalarToPyHasScalar implements PyObjectAdapter {

		public HasScalarToPyHasScalar() {
		}

		public PyObject adapt(Object o) {
			return new PyHasScalar((iHasScalar) o);
		}

		public boolean canAdapt(Object o) {
			return o instanceof iHasScalar;
		}

	}

	static public class PyHasScalar extends PyFloat {

		private final iHasScalar contents;
		private final PyObjectDerived instance;

		public PyHasScalar(iHasScalar e) {
			super(e.getDoubleValue());
			this.contents = e;
			instance = (PyObjectDerived) PyJavaType.wrapJavaObject(e);
		}

		@Override
		public PyObject __findattr_ex__(String name) {
			return instance.__findattr_ex__(name);
		}

		@Override
		public Object __tojava__(Class c) {
			return contents;
		}

		@Override
		public double getValue() {
			return contents.getDoubleValue();
		}

	}
}
