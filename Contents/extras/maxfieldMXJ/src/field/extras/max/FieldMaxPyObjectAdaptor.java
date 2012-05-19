package field.extras.max;

import java.util.HashMap;
import java.util.Map;

import org.python.core.Py;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.adapter.ExtensiblePyObjectAdapter;

/**
 * new style adaptation for new style java classes. Jython 2.5b1.
 * 
 * @author marc
 * 
 */
public class FieldMaxPyObjectAdaptor {

	public interface iCallable {
		public Object call(Object[] args);
	}

	public interface iHandlesAttributes {
		public Object getAttribute(String name);

		public void setAttribute(String name, Object value);
	}

	static protected boolean initialized = false;
	public static FieldMaxPyObjectAdaptor fieldPyObjectAdaptor;
	public static ExtensiblePyObjectAdapter adaptor;

	public static void initialize() {
		if (!initialized) {

			System.out.println(" initializing adaptor2 ");

			ExtensiblePyObjectAdapter a = Py.getAdapter();
			fieldPyObjectAdaptor = new FieldMaxPyObjectAdaptor().install(a);
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

				Object r = ((iCallable) Py.tojava(self, iCallable.class)).call(x);
				PyObject p = Py.java2py(r);
				return p;
			}
		};
		PyType.fromClass(c).addMethod(meth);
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
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setattr__", 2) {
				@Override
				public PyObject __call__(PyObject name, PyObject to) {
					iHandlesAttributes d = Py.tojava(self, iHandlesAttributes.class);

					d.setAttribute(((String) Py.tojava(name, String.class)), Py.tojava(to, Object.class));
					return Py.None;
				}
			};
			PyType.fromClass(c).addMethod(meth);
		}
	}

	protected FieldMaxPyObjectAdaptor install(ExtensiblePyObjectAdapter ex) {

		isCallable(iCallable.class);
		isHandlesAttributes(MaxField.class);
		return this;
	}
}
