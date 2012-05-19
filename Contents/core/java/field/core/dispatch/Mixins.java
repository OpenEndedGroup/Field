package field.core.dispatch;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import field.core.dispatch.iVisualElementOverrides.iDefaultOverride;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.namespace.generic.Bind.iFunction;



public class Mixins {

	public interface iMixinProxy<T> {
		public Class<T> getMixinInterface();

		public List<T> getCallList();
	}

	public <T> T make(final Class<T> t, final iFunction<Object, List<Object>> combine, T... over) {
		final iMixinProxy<T> m = new iMixinProxy<T>() {
			List<T> callList = new ArrayList<T>();

			public List<T> getCallList() {
				return callList;
			}

			public Class<T> getMixinInterface() {
				return t;
			}
		};
		if (over!=null)
			m.getCallList().addAll(Arrays.asList(over));
		Object o = Proxy.newProxyInstance(t.getClassLoader(), new Class[] { t, iDefaultOverride.class,  iMixinProxy.class }, new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (method.getName().equals("getCallList")) {
					return method.invoke(m, args);
				}
				if (method.getName().equals("getMixinInterface")) {
					return method.invoke(m, args);
				} else {
					List<T> l = m.getCallList();
					List<Object> r = new ArrayList<Object>();
					for (T t : l) {
						Object rr = method.invoke(t, args);
						r.add(rr);
					}
					Object ret = null;
					if (combine != null) {
						ret = combine.f(r);
					} else {
						ret = r.size() > 0 ? r.get(0) : null;
					}
					return ret;
				}
			}
		});
		return (T) o;
	}

	public static iFunction<Object, List<Object>> visitCodeCombiner = new iFunction<Object, List<Object>>() {

		public Object f(List<Object> in) {
			if (in.size() == 0)
				return null;
			if (in.size() == 1)
				return in.get(0);
			boolean isVisitCode = false;
			for (int i = 0; i < in.size(); i++) {
				if (in == VisitCode.stop)
					return VisitCode.stop;
				if (in == VisitCode.skip)
					return VisitCode.skip;
				if (in instanceof VisitCode)
					isVisitCode = true;
			}
			return isVisitCode ? VisitCode.cont : in.get(0);
		}
	};

	public iMixinProxy<iVisualElementOverrides> upgradeOverrides(iVisualElement e) {
		iVisualElementOverrides o = e.getProperty(iVisualElement.overrides);
		if (o instanceof iMixinProxy)
			return (iMixinProxy<iVisualElementOverrides>) o;

		iVisualElementOverrides oo = make(iVisualElementOverrides.class, visitCodeCombiner, o);
		e.setProperty(iVisualElement.overrides, oo);
		return (iMixinProxy<iVisualElementOverrides>) oo;
	}

	public <T extends iDefaultOverride> T mixInOverride(Class<T> ty, iVisualElement e) {
		iMixinProxy<iVisualElementOverrides> m = upgradeOverrides(e);
		List<iVisualElementOverrides> c = m.getCallList();
		for (iVisualElementOverrides o : c) {
			if (ty.isInstance(o)) {
				return (T) o;
			}
		}
		try {
			iDefaultOverride o = (iDefaultOverride) ty.newInstance();
			o.setVisualElement(e);
			c.add((iVisualElementOverrides) o);
			return (T) o;
		} catch (InstantiationException e1) {
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
		}
		return null;
	}

}
