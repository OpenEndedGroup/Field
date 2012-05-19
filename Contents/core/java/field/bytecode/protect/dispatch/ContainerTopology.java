package field.bytecode.protect.dispatch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import field.bytecode.protect.dispatch.DispatchSupport.Apply;


public class ContainerTopology implements DispatchSupport.DispatchProvider {

	static public ThreadLocal<MethodUtilities> utilities = new ThreadLocal<MethodUtilities>() {
		@Override
		protected MethodUtilities initialValue() {
			return new MethodUtilities();
		}
	};

	HashSet<Object> alreadyDone = new HashSet<Object>();

	ClassLoader cachedLoader = null;

	Apply nullApply = new Apply() {

		public void head(Object[] args) {
		}

		public Object tail(Object[] args, Object returnWas) {
			return returnWas;
		}

	};

	public Apply getTopologyForEntrance(final Object root, final Map<String, Object> parameters, Object[] args, final String className) {
		if (!alreadyDone.contains(root)) {
			alreadyDone.add(root);
			if (parameters.get("forwards") == null || (Boolean) parameters.get("forwards")) {
				final ClassLoader loader = cachedLoader == null ? (cachedLoader = root.getClass().getClassLoader()) : cachedLoader;
				final org.objectweb.asm.commons.Method method = (org.objectweb.asm.commons.Method) parameters.get("method");
				final Method m = utilities.get().getMethodFor(loader, method, null, className);
				return new Apply() {

					public void head(Object[] args) {
						applyToForwards(root, args, m, loader, method, className, (String) parameters.get("id"));
					}

					public Object tail(Object[] args, Object returnWas) {
						return returnWas;
					}
				};
			}
		}
		return nullApply;
	}

	public Apply getTopologyForExit(final Object root, final Map<String, Object> parameters, Object[] args, final String className) {
		if (alreadyDone.contains(root)) {
			if (parameters.get("forwards") != null && !(Boolean) parameters.get("forwards")) {
				final ClassLoader loader = cachedLoader == null ? (cachedLoader = root.getClass().getClassLoader()) : cachedLoader;
				final org.objectweb.asm.commons.Method method = (org.objectweb.asm.commons.Method) parameters.get("method");
				final Method m = utilities.get().getMethodFor(loader, method, null, className);
				return new Apply() {

					public void head(Object[] args) {
					}

					public Object tail(Object[] args, Object returnWas) {
						applyToBackwards(root, args, m, loader, method, className, (String) parameters.get("id"));
						return returnWas;
					}
				};
			}
			alreadyDone.remove(root);
		}
		return null;
	}

	public void notifyExecuteBegin(Object fromThis, Map<String, Object> parameterName) {
	}

	public void notifyExecuteEnds(Object fromThis, Map<String, Object> parameterName) {
		alreadyDone.remove(fromThis);
	}

	protected void applyToBackwards(Object root, Object[] args, Method m, ClassLoader loader, org.objectweb.asm.commons.Method method, String className, String id) {
		if (root instanceof iContainer) {
			List list = ((iContainer) root).propagateTo(id, null, m, args);
			if (list == null)
				return;

			for (int i = 0; i < list.size(); i++) {
				Object o = list.get(list.size() - 1 - i);
				Method nMethod = utilities.get().getPossibleMethodFor(loader, method, o.getClass(), o.getClass().getName());
				if (nMethod != null)
					try {
						nMethod.invoke(o, args);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
			}
			for (int i = 0; i < list.size(); i++) {
				Object o = list.get(list.size() - 1 - i);
				if (o instanceof iContainer) {
					applyToBackwards(o, args, m, loader, method, className, id);
				}
			}
		}
	}

	protected void applyToForwards(Object root, Object[] args, Method m, ClassLoader loader, org.objectweb.asm.commons.Method method, String className, String id) {
		if (root instanceof iContainer) {
			List list = ((iContainer) root).propagateTo(id, null, m, args);
			if (list == null)
				return;

			for (int i = 0; i < list.size(); i++) {
				Object o = list.get(i);
				if (o instanceof iContainer) {
					applyToForwards(o, args, m, loader, method, className, id);
				}
			}
			for (int i = 0; i < list.size(); i++) {
				Object o = list.get(i);
				Method nMethod = utilities.get().getPossibleMethodFor(loader, method, o.getClass(), o.getClass().getName());
				if (nMethod != null)
					try {
						nMethod.invoke(o, args);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
			}

		}
	}

}
