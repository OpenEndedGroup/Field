package field.core.plugins.python;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

import org.python.core.Py;
import org.python.core.PyFunction;
import org.python.core.PyObject;

import field.core.util.FieldPyObjectAdaptor.iCallable;
import field.core.util.FieldPyObjectAdaptor.iHandlesAttributes;
import field.core.util.PythonCallableMap;

/**
 * this is an additional generalization of the kind of things that we're doing
 * in PythonOverridden: Attaching "callable" things from python-land to
 * interfaces in java-land a few points: - each callable thing (and we're being
 * very general here, it could be a method, or a function, or a generator or so
 * on...) has a name associated with it the - whole thing is easily reset (a
 * begin(), end() pattern) - this thing can be persisted, but comes back empty
 * when it does. This slightly simplifies it's use.
 * 
 */
public class DynamicExtensionPoint<T> implements iHandlesAttributes, iCallable {

	// static
	// {
	// FieldPyObjectAdaptor2.isHandlesAttributes(DynamicExtensionPoint.class);
	// FieldPyObjectAdaptor2.isCallable(DynamicExtensionPoint.class);
	// }

	protected Class<T> forClass;

	transient boolean isInited = false;

	transient private T proxyInstance;
	transient private PythonCallableMap defaultExtension;

	transient HashMap<String, Method> aliases = new HashMap<String, Method>();

	transient HashMap<Method, PythonCallableMap> extensions = new HashMap<Method, PythonCallableMap>();

	transient PythonCallableMap unhandled = new PythonCallableMap();

	public DynamicExtensionPoint(Class<T> forClass) {
		this.forClass = forClass;
		init(forClass);
	}

	private void init(Class<T> forClass) {
		if (isInited)
			return;

		if (aliases == null) {
			aliases = new HashMap<String, Method>();
			extensions = new HashMap<Method, PythonCallableMap>();
			unhandled = new PythonCallableMap();
		}
		proxyInstance = (T) Proxy.newProxyInstance(forClass.getClassLoader(), new Class[] { forClass }, new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				return handleInvocation(method, args);
			}
		});

		Method[] m = forClass.getMethods();
		for (Method mm : m) {
			aliases.put(mm.getName(), mm);
			extensions.put(mm, new PythonCallableMap());
		}
		if (m.length == 1) {
			defaultExtension = extensions.get(m[0]);
		}
		isInited = true;
	}

	protected Object handleInvocation(Method method, Object[] args) {

		System.out.println(" handle invocation <" + method + "> <" + args + ">");

		init(forClass);
		PythonCallableMap ep = extensions.get(method);
		if (ep == null)
			ep = unhandled;
		ep.invoke(args);
		return null;
	}

	public T getProxy() {
		init(forClass);
		return proxyInstance;
	}

	public PythonCallableMap getExtensionPoint(String name) {

		init(forClass);
		if (name.equals("unhandled"))
			return unhandled;

		Method m = aliases.get(name);
		PythonCallableMap table = extensions.get(m);
		return table;
	}

	public Object getAttribute(String name) {
		init(forClass);
		PyObject o = Py.java2py(getExtensionPoint(name));
		return o;
	}

	public void setAttribute(String name, Object value) {
		init(forClass);
		getExtensionPoint(name).register((PyFunction) value);
	}

	public void reset() {
		init(forClass);
		for (PythonCallableMap m : extensions.values())
			m.reset();
		unhandled.reset();
	}

	public Object call(Object[] args) {
		init(forClass);
		if (defaultExtension == null)
			System.out.println(" known methods are <" + aliases + ">");
		return defaultExtension.call(args);
	}
}
