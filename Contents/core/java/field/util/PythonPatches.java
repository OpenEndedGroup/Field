package field.util;

import org.python.core.Py;
import org.python.core.PyObject;

import field.core.plugins.python.PythonPlugin.CapturedEnvironment;
import field.namespace.generic.Bind.iFunction;

public class PythonPatches {

	static public class Filter<T> implements iFunction<T, T> {

		public PyObject callable;

		public CapturedEnvironment env;

		private final String name;

		private final Class<Object> coerceTo;

		public Filter(String name) {
			this.name = name;
			coerceTo = Object.class;
		}

		public Filter(String name, Class coerceTo) {
			this.name = name;
			this.coerceTo = coerceTo;
		}

		public T f(T in) {
			if (callable == null)
				return computeDefault(in);

			enter();
			try {
				PyObject o = callable.__call__(Py.java2py(in));
				if (o == null)
					return null;
				return (T) o.__tojava__(coerceTo);
			} catch (Throwable t) {
				System.err.println(" exception thrown in callable <" + this.getName() + "> -- it will be called no more");
				callable = null;
				return computeDefault(in);
			} finally {
				exit();
			}

		}

		public void set(PyObject to) {
			env = null;
			callable = to;
		}

		public void set(PyObject to, CapturedEnvironment env) {
			this.env = env;
			callable = to;
		}

		protected T computeDefault(T in) {
			return in;
		}

		protected void enter() {
			if (env != null)
				env.enter();
		}

		protected void exit() {
			if (env != null)
				env.exit();
		}

		protected String getName() {
			return name;
		}
	}



}
