package field.core.util;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.python.core.Py;
import org.python.core.PyFunction;
import org.python.core.PyObject;

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

	protected Callable newCallable(PyFunction f) {
		return PythonOverridden.callableForFunction(f, (CapturedEnvironment) PythonInterface.getPythonInterface().getVariable("_environment"));
	}
	
	protected Callable newCallable(String name, iUpdateable f) {
		return PythonOverridden.callableForUpdatable(name, f);
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

	public void invoke(Object... a) {
		PythonInterface.getPythonInterface().setVariable("hooks", this);

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
		if (u!=null)
			this.known.putAll(u.known);
	}
	
	static public PythonCallableMap merge(iVisualElement from, VisualElementProperty<PythonCallableMap> parameter)
	{
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
