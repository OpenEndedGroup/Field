package field.extras.max;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.python.core.Py;
import org.python.core.PyFunction;
import org.python.core.PyObject;

import field.extras.max.FieldMaxPyObjectAdaptor.iCallable;

public class PythonCallableMap implements iCallable {

	static public abstract class Callable {
		public final Object source;
		public String name;
		
		public Callable(Object o, String name) {
			this.source = o;
			this.name = name;
		}

		abstract public Object call(Method m, Object[] args);

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			Callable other = (Callable) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return name + "//" + System.identityHashCode(this) + ":" + source;
		}


	}
	public LinkedHashMap<String, Callable> known = new LinkedHashMap<String, Callable>();

	public PyFunction register(String name, PyFunction f) {
		Callable newCallable = newCallable(f);
		Callable oldCallable = known.get(name);

		doRegister(name, newCallable, oldCallable);
		return f;
	}

	static public Object removeMe = new Object();

	static public Callable callableForFunction(final PyObject call) {
		return new Callable(call, call instanceof PyFunction ? ((PyFunction)call).__name__ : (""+call.hashCode())) {
			@Override
			public Object call(Method arg0, Object[] arg1) {
				
				try {
					PyObject[] objects = new PyObject[arg1.length];
					for (int i = 0; i < objects.length; i++) {
						Object a = arg1[i];
						objects[i] = Py.java2py(a);
					}

					PyObject o = call.__call__(objects);
					if (o == null)
						return null;
					if (o == Py.None)
						return null;
					if (o == Py.Zero)
						return removeMe;
					return o.__tojava__(Object.class);
				} finally {
					
				}
			}
		};
	}

	protected Callable newCallable(PyFunction f) {
		return callableForFunction(f);
	}
	

	protected void doRegister(String name, Callable newCallable, Callable oldCallable) {
		known.put(name, newCallable);
	}

	public PyFunction register(PyFunction f) {
		return register(f.__name__, f);
	}

	long uniq = 0;

	public PyFunction registerUniq(PyFunction f) {
		return register("" + (uniq++), f);
	}

	LinkedHashSet<String> clear = new LinkedHashSet<String>();
	private String current;

	public void invoke(Object... a) {
		
		clear.clear();
		try {
			for (Map.Entry<String, Callable> c : known.entrySet()) {
				current = c.getKey();
				c.getValue().call(null, a);
			}
		} finally {
			for (String s : clear) {
				known.remove(s);
			}
			clear.clear();
		}
	}

	public Object invokeChained(Object... a) {
		
		Object[] aa = new Object[a == null ? 1 : a.length + 1];
		if (a != null)
			System.arraycopy(a, 0, aa, 1, a.length);
		aa[0] = Py.None;

		clear.clear();
		try {
			for (Map.Entry<String, Callable> c : known.entrySet()) {
				current = c.getKey();
				Object r = c.getValue().call(null, aa);
				aa[0] = r;
			}
		} finally {
			for (String s : clear) {
				known.remove(s);
			}
			clear.clear();
		}
		return aa[0] instanceof PyObject ? Py.tojava((PyObject) aa[0], Object.class) : aa[0];
	}

	public boolean isEmpty() {
		return known.size() == 0;
	}

	public void remove() {
		clear.add(current);
	}

	public void remove(PyFunction f) {
		known.remove(f.__name__);
	}

	public void remove(String n) {
		known.remove(n);
	}

	public void reset() {
		known.clear();
	}

	// this is this way around so that PythonCallableMaps will be usable as
	// a function decorator
	public Object call(Object[] args) {
		if (args.length == 1)
			return register((PyFunction) args[0]);
		else
			return register((String) args[0], (PyFunction) args[1]);
	}

}
