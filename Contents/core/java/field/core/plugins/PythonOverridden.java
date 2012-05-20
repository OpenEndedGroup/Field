package field.core.plugins;

import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.python.core.Py;
import org.python.core.PyFunction;
import org.python.core.PyObject;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.dispatch.iVisualElementOverrides_m;
import field.core.plugins.python.PythonPlugin.CapturedEnvironment;
import field.launch.iUpdateable;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.util.Dict;
import field.util.HashMapOfLists;

public class PythonOverridden extends DefaultOverride {

	static public abstract class Callable {
		public final Object source;
		public String name;
		Dict info;

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

		public Dict getInfo() {
			if (info == null)
				info = new Dict();
			return info;
		}

	}

	static public Object removeMe = new Object();

	transient HashMapOfLists<String, Callable> methods = new HashMapOfLists<String, Callable>() {
		@Override
		protected Collection<Callable> newList() {
			return new ArrayList<Callable>();
		}
	};

	public void add(String methodname, PyFunction call) {
		Callable c = callableForFunction(call);
		;//System.out.println(" adding <" + methodname + "> <" + call + "> <" + c + ">");
		// methods.addToList(methodname, c);
		Collection<Callable> cc = methods.getCollection(methodname);
		if (cc != null) {
			Iterator<Callable> ccc = cc.iterator();
			while (ccc.hasNext()) {
				Callable n = ccc.next();
				if (n.equals(c))
					ccc.remove();
			}
		}
		methods.addToList(methodname, c);

		;//System.out.println("  now <" + methods + ">");
	}

	@Override
	public VisitCode added(iVisualElement newSource) {
		Method method = iVisualElementOverrides_m.added_m;
		Object[] args = { newSource };
		String methodName = "added";

		return call(method, args, methodName);
	}

	@Override
	public VisitCode beginExecution(iVisualElement source) {
		Method method = iVisualElementOverrides_m.beginExecution_m;
		Object[] args = { source };
		String methodName = "beginExecution";

		return call(method, args, methodName);
	}

	@Override
	public VisitCode deleted(iVisualElement source) {
		Method method = iVisualElementOverrides_m.deleted_m;
		Object[] args = { source };
		String methodName = "deleted";

		return call(method, args, methodName);
	}

	@Override
	public <T> VisitCode deleteProperty(iVisualElement source, VisualElementProperty<T> prop) {
		Method method = iVisualElementOverrides_m.deleteProperty_m;
		Object[] args = { source, prop };
		String methodName = "deleteProperty";

		return call(method, args, methodName);
	}

	@Override
	public VisitCode endExecution(iVisualElement source) {
		Method method = iVisualElementOverrides_m.endExecution_m;
		Object[] args = { source };
		String methodName = "endExecution";

		return call(method, args, methodName);
	}

	@Override
	public <T> VisitCode getProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> ref) {
		Method method = iVisualElementOverrides_m.getProperty_m;
		Object[] args = { source, prop, ref };
		String methodName = "getProperty";

		return call(method, args, methodName);
	}

	@Override
	public VisitCode handleKeyboardEvent(iVisualElement newSource, org.eclipse.swt.widgets.Event event) {
		Method method = iVisualElementOverrides_m.handleKeyboardEvent_m;
		Object[] args = { newSource, event };
		String methodName = "handleKeyboardEvent";

		return call(method, args, methodName);
	}

	@Override
	public VisitCode menuItemsFor(iVisualElement source, Map<String, iUpdateable> items) {
		Method method = iVisualElementOverrides_m.menuItemsFor_m;
		Object[] args = { source, items };
		String methodName = "menuItemsFor";

		return call(method, args, methodName);
	}

	@Override
	public VisitCode paintNow(iVisualElement source, Rect bounds, boolean visible) {
		Method method = iVisualElementOverrides_m.paintNow_m;
		Object[] args = { source, bounds, visible };
		String methodName = "paintNow";

		return call(method, args, methodName);
	}

	public void replace(String methodname, PyFunction call) {
		methods.remove(methodname);
		methods.addToList(methodname, callableForFunction(call));
	}

	@Override
	public <T> VisitCode setProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> to) {
		Method method = iVisualElementOverrides_m.setProperty_m;
		Object[] args = { source, prop, to };
		String methodName = "setProperty";

		return call(method, args, methodName);
	}

	@Override
	public VisitCode shouldChangeFrame(iVisualElement source, Rect newFrame, Rect oldFrame, boolean now) {
		Method method = iVisualElementOverrides_m.shouldChangeFrame_m;
		Object[] args = { source, newFrame, oldFrame, now };
		String methodName = "shouldChangeFrame";

		return call(method, args, methodName);
	}

	private VisitCode call(Method method, Object[] args, String methodName) {
		Collection<Callable> c = methods.get(methodName);
		if (c == null || c.size() == 0)
			return VisitCode.cont;

		HashMap<Callable, Throwable> faulted = null;
		for (Callable cc : c) {
			try {
				Object r = cc.call(method, args);

				if (r == VisitCode.stop)
					return VisitCode.stop;
				if (r == VisitCode.stop)
					return VisitCode.skip;
				if (r == removeMe)
					faulted.put(cc, null);

			} catch (Throwable t) {
				t.printStackTrace();
				if (faulted == null)
					faulted = new HashMap<Callable, Throwable>();
				faulted.put(cc, t);
			}
		}
		if (faulted != null) {
			notifyFaulted(faulted);
			for (Callable f : faulted.keySet()) {
				methods.remove(methodName, f);
			}
		}
		return VisitCode.cont;
	}

	static public Callable callableForFunction(final PyFunction call) {
		return new Callable(call, call.__name__) {
			@Override
			public Object call(Method arg0, Object[] arg1) {
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
			}
		};
	}

	static public Callable callableForFunction(final PyObject call, final CapturedEnvironment e) {
		return new Callable(call, call instanceof PyFunction ? ((PyFunction)call).__name__ : (""+call.hashCode())) {
			@Override
			public Object call(Method arg0, Object[] arg1) {
				if (e != null)
					e.enter();
				try {
					PyObject[] objects = new PyObject[arg1==null ? 0 : arg1.length];
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
					if (e != null)
						e.exit();
				}
			}
		};
	}

	static public Callable callableForUpdatable(String name, final iUpdateable up) {
		return new Callable(null, name) {
			@Override
			public Object call(Method arg0, Object[] arg1) {
				up.update();
				return null;
			}
		};
	}

	private void notifyFaulted(HashMap<Callable, Throwable> faulted) {

	}

}
