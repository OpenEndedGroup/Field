package field.core.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.python.core.Py;
import org.python.core.PyFunction;
import org.python.core.PyObject;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import clojure.lang.IFn;
import field.core.dispatch.FastVisualElementOverridesPropertyCombiner;
import field.core.dispatch.FastVisualElementOverridesPropertyCombiner.iCombiner;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.execution.PythonInterface;
import field.core.plugins.PythonOverridden;
import field.core.plugins.PythonOverridden.Callable;
import field.core.plugins.python.PythonPlugin.CapturedEnvironment;
import field.core.util.FieldPyObjectAdaptor.iCallable;
import field.launch.iUpdateable;
import field.namespace.generic.Bind.iFunction;

public class PythonCallableMap implements iCallable {

	// static
	// {
	// FieldPyObjectAdaptor2.isCallable(PythonCallableMap.class);
	// }
	//
	public LinkedHashMap<String, Callable> known = new LinkedHashMap<String, Callable>();

	public PyFunction register(String name, PyFunction f) {
		Callable newCallable = newCallable(f);
		Callable oldCallable = known.get(name);

		doRegister(name, newCallable, oldCallable);
		return f;
	}

	public iUpdateable register(String name, iUpdateable f) {
		Callable newCallable = newCallable(name, f);
		Callable oldCallable = known.get(name);

		doRegister(name, newCallable, oldCallable);
		return f;
	}

	public IFn register(String name, final IFn f) {
		Callable newCallable = new Callable(f, name) {
			@Override
			public Object call(Method m, Object[] args) {
				if (args.length == 0)
					return f.invoke();
				if (args.length == 1)
					return f.invoke(args[0]);
				if (args.length == 2)
					return f.invoke(args[0], args[1]);
				throw new NotImplementedException();
			}
		};
		Callable oldCallable = known.get(name);

		doRegister(name, newCallable, oldCallable);
		return f;
	}

	public iFunction<Object, Object> register(String name, iFunction<Object, Object> f) {
		Callable newCallable = newCallable(name, f);
		Callable oldCallable = known.get(name);

		doRegister(name, newCallable, oldCallable);
		return f;

	}

	protected Callable newCallable(PyFunction f) {
		return PythonOverridden.callableForFunction(f, (CapturedEnvironment) PythonInterface.getPythonInterface().getVariable("_environment"));
	}

	protected Callable newCallable(String name, iUpdateable f) {
		return PythonOverridden.callableForUpdatable(name, f);
	}

	protected Callable newCallable(String name, iFunction f) {
		return PythonOverridden.callableForFunction(name, f);
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

	public LinkedHashSet<String> clear = new LinkedHashSet<String>();
	public String current;

	public Object invoke(Object... a) {
		PythonInterface.getPythonInterface().setVariable("hooks", this);

		clear.clear();
		try {
			Object r = null;
			for (Map.Entry<String, Callable> c : known.entrySet()) {
				current = c.getKey();
				r = c.getValue().call(null, a);
			}
			return r;
		} finally {

			for (String s : clear) {
				known.remove(s);
			}
			clear.clear();

		}
	}

	public Collection<Object> gather(Object... a) {
		PythonInterface.getPythonInterface().setVariable("hooks", this);

		clear.clear();
		try {
			Object r = null;
			ArrayList<Object> aa = new ArrayList<Object>();
			for (Map.Entry<String, Callable> c : known.entrySet()) {
				current = c.getKey();
				r = c.getValue().call(null, a);
				if (r != null)
					aa.add(r);
			}
			return aa;
		} finally {

			for (String s : clear) {
				known.remove(s);
			}
			clear.clear();

		}
	}

	public Object invokeChained(Object... a) {
		PythonInterface.getPythonInterface().setVariable("hooks", this);

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

	public void merge(PythonCallableMap u) {
		if (u != null)
			this.known.putAll(u.known);
	}

	static public PythonCallableMap merge(iVisualElement from, VisualElementProperty<PythonCallableMap> parameter) {
		return new FastVisualElementOverridesPropertyCombiner<PythonCallableMap, PythonCallableMap>(false).getProperty(from, parameter, new iCombiner<PythonCallableMap, PythonCallableMap>() {

			@Override
			public PythonCallableMap unit() {
				return new PythonCallableMap();
			}

			@Override
			public PythonCallableMap bind(PythonCallableMap t, PythonCallableMap u) {
				PythonCallableMap m = new PythonCallableMap();
				m.merge(t);
				m.merge(u);
				return m;
			}
		});

	}

}
