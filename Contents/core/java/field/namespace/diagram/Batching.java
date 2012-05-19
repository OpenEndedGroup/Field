package field.namespace.diagram;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import field.namespace.generic.ReflectionTools;


/**
 * @author marc
 * Created on Sep 20, 2004 - bus to boston
 */
public class Batching {

	List methodList = new ArrayList();

	List operandList = new ArrayList();

	Object target;

	protected Batching(Object target) {
		this.target = target;
	}

	protected void add(Method m, Object op) {
		methodList.add(m);
		operandList.add(op);
	}

	static Object none = new Object();

	static public Object masquerade(Class as, Object target, String fireOn) {
		return masquerade(as, target, fireOn, fireOn);
	}

	static public Object masquerade(Class as, Object target, String up, String down) {
		final Batching b = new Batching(target);
		assert as.isAssignableFrom(target.getClass()) : as + " " + target + " " + target.getClass();
		final Method mdown = ReflectionTools.methodOf(down, as);
		final Method mup = ReflectionTools.methodOf(up, as);

		Object proxy = Proxy.newProxyInstance(Batching.class.getClassLoader(), new Class[] { as}, new InvocationHandler() {

			int i = 0;

			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				b.add(method, args == null ? none : (args.length == 0 ? none : args[0]));
				if (method.equals(mup)) i++;
				if (method.equals(mdown)) {
					i--;
					if (i == 0) b.fire();
				}

				return null;
			}
		});
		return proxy;
	}

	protected void fire() {
		Object[] zero = new Object[0];
		Object[] one = new Object[1];

		for (int i = 0; i < methodList.size(); i++) {
			Method m = (Method) methodList.get(i);
			Object o = operandList.get(i);
			if (o == none) {
				try {
					m.invoke(target, zero);
				} catch (IllegalArgumentException e) {
					throw e;
				} catch (IllegalAccessException e) {
					throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
				} catch (InvocationTargetException e) {
					throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
				}
			} else {
				one[0] = o;
				try {
					m.invoke(target, one);
				} catch (IllegalArgumentException e) {
					throw e;
				} catch (IllegalAccessException e) {
					throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
				} catch (InvocationTargetException e) {
					throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
				}

			}
		}
		methodList.clear();
		operandList.clear();
	}

}