package field.namespace.generic;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import field.util.ANSIColorUtils;

/**
 * @author marc
 */
public class ReflectionTools {

	// this pair needs "protection"
	static public class Pair<A, B> implements Serializable {
		public A left;

		public B right;

		public Pair(A a, B b) {
			left = a;
			right = b;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Pair))
				return false;
			Pair p = (Pair) o;
			if ((((left == null) && (p.left == null)) || (left.equals(p.left))) && (((right == null) && (p.right == null)) || (right.equals(p.right))))
				return true;
			return false;
		}

		@Override
		public int hashCode() {
			int l = (left == null ? 0 : left.hashCode());
			int r = (right == null ? 0 : right.hashCode());
			return l + r;
		}

		@Override
		public String toString() {
			return "left:" + left + " right:" + right;
		}
	}

	static public Comparator methodComparator = new Comparator() {

		public int compare(Object arg0, Object arg1) {
			return ((Method) arg0).getName().compareTo(((Method) arg1).getName());
		}
	};

	static public Comparator fieldComparator = new Comparator() {

		public int compare(Object arg0, Object arg1) {
			return ((Field) arg0).getName().compareTo(((Field) arg1).getName());
		}
	};

	static HashMap methodsCache = new HashMap();

	static HashMap fieldsCache = new HashMap();

	static HashMap constructorsCache = new HashMap();

	static public void apply(Collection list, Method m) throws IllegalArgumentException {
		Object[] zero = new Object[] {};
		for (Object o : list) {
			try {
				// needed
				// for
				// weak
				// maps
				if (o != null)
					m.invoke(o, zero);

			} catch (IllegalArgumentException e) {
				throw e;
			} catch (IllegalAccessException e) {
				IllegalArgumentException e2 = (IllegalArgumentException) new IllegalArgumentException().initCause(e);
				throw e2;
			} catch (InvocationTargetException e) {
				IllegalArgumentException e2 = (IllegalArgumentException) new IllegalArgumentException().initCause(e);
				throw e2;
			}
		}
	}

	static public void apply(Collection list, Method m, Object parameter) throws IllegalArgumentException {
		Object[] zero = new Object[] { parameter };
		for (Object o : new ArrayList(list)) {
			try {
				if (o instanceof WeakReference)
					o = ((WeakReference) o).get();
				if (o != null) {
					m.invoke(o, zero);
				}
			} catch (IllegalArgumentException e) {
				throw e;
			} catch (IllegalAccessException e) {
				IllegalArgumentException e2 = (IllegalArgumentException) new IllegalArgumentException().initCause(e);
				throw e2;
			} catch (InvocationTargetException e) {
				IllegalArgumentException e2 = (IllegalArgumentException) new IllegalArgumentException().initCause(e);
				throw e2;
			}
		}
	}

	static public void apply(Collection list, Method m, Object... parameter) throws IllegalArgumentException {
		for (Object o : list) {
			try {
				if (o != null)
					m.invoke(o, parameter);
			} catch (IllegalArgumentException e) {
				throw e;
			} catch (IllegalAccessException e) {
				IllegalArgumentException e2 = (IllegalArgumentException) new IllegalArgumentException().initCause(e);
				throw e2;
			} catch (InvocationTargetException e) {
				IllegalArgumentException e2 = (IllegalArgumentException) new IllegalArgumentException().initCause(e);
				throw e2;
			}
		}
	}

	static public void apply(Collection list, Method m, Object parameter, Object parameter2) throws IllegalArgumentException {
		Object[] zero = new Object[] { parameter, parameter2 };
		for (Object o : list) {
			try {
				if (o != null)
					m.invoke(o, zero);
			} catch (IllegalArgumentException e) {
				throw e;
			} catch (IllegalAccessException e) {
				IllegalArgumentException e2 = (IllegalArgumentException) new IllegalArgumentException().initCause(e);
				throw e2;
			} catch (InvocationTargetException e) {
				IllegalArgumentException e2 = (IllegalArgumentException) new IllegalArgumentException().initCause(e);
				throw e2;
			}
		}
	}

	static public <T> T argMax(Collection<T> over, Object run) {
		Method m = run.getClass().getDeclaredMethods()[0];
		float min = Float.NEGATIVE_INFINITY;
		Iterator<T> i = over.iterator();
		T ret = null;
		m.setAccessible(true);
		try {
			while (i.hasNext()) {
				T arg = i.next();
				float f = ((Number) m.invoke(run, new Object[] { arg })).floatValue();
				if (f > min) {
					min = f;
					ret = arg;
				}
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return ret;
	}

	static public <T> T argMin(Collection<T> over, Object run) {
		Method m = run.getClass().getDeclaredMethods()[0];
		float min = Float.POSITIVE_INFINITY;
		Iterator<T> i = over.iterator();
		T ret = null;
		m.setAccessible(true);
		try {
			while (i.hasNext()) {
				T arg = i.next();
				float f = ((Number) m.invoke(run, new Object[] { arg })).floatValue();
				if (f < min) {
					min = f;
					ret = arg;
				}
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return ret;
	}

	static public <T> float min(Collection<T> over, Object run) {
		Method m = run.getClass().getDeclaredMethods()[0];
		float min = Float.POSITIVE_INFINITY;
		Iterator<T> i = over.iterator();
		T ret = null;
		m.setAccessible(true);
		try {
			while (i.hasNext()) {
				T arg = i.next();
				float f = ((Number) m.invoke(run, new Object[] { arg })).floatValue();
				if (f < min) {
					min = f;
					ret = arg;
				}
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return min;
	}

	static public <T> Pair<T, Float> bothMax(Collection<T> over, Object run) {
		Method m = run.getClass().getDeclaredMethods()[0];
		float min = Float.NEGATIVE_INFINITY;
		Iterator<T> i = over.iterator();
		T ret = null;
		m.setAccessible(true);
		try {
			while (i.hasNext()) {
				T arg = i.next();
				float f = ((Number) m.invoke(run, new Object[] { arg })).floatValue();
				if (f > min) {
					min = f;
					ret = arg;
				}
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return ret == null ? null : new Pair<T, Float>(ret, min);
	}

	public static String debugToString(Object of) {
		return debugToString("", of);
	}

	public static String debugToString(String indent, Object of) {
		String total = "";

		if (of == null)
			return "((null))";

		if (of.getClass().isPrimitive())
			return indent + of;
		else if (of instanceof Number) {
			return indent + of;
		} else if (of instanceof Boolean) {
			return indent + of;
		} else if (of instanceof String) {
			return indent + of;
		}
		Field[] fields = of.getClass().getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Object value;
			if (fields[i].getName().indexOf("$") == -1)
				try {
					fields[i].setAccessible(true);
					value = fields[i].get(of);

					if (value == null) {
						total += indent + ANSIColorUtils.yellow(fields[i].getName()) + "= null\n";
					} else if (value.getClass().isArray()) {
						total += indent + ANSIColorUtils.yellow(fields[i].getName()) + "= [(" + Array.getLength(value) + ")\n";
						for (int j = 0; j < Array.getLength(value); j++)
							total += debugToString(indent + " ", Array.get(value, j));
						total += indent + "]\n";
					} else if (value.getClass().isPrimitive()) {
						total += indent + ANSIColorUtils.yellow(fields[i].getName()) + "= " + value + "\n";
					} else if (value instanceof Number) {
						total += indent + ANSIColorUtils.yellow(fields[i].getName()) + "= " + value + "\n";
					} else if (value instanceof Boolean) {
						total += indent + ANSIColorUtils.yellow(fields[i].getName()) + "= " + value + "\n";
					} else if (value instanceof String) {
						total += indent + ANSIColorUtils.yellow(fields[i].getName()) + "= " + value + "\n";
					} else {
						total += indent + ANSIColorUtils.yellow(fields[i].getName()) + "= [(" + value.getClass() + ") \n" + debugToString(indent + " ", value) + "\n";
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
		}
		return total;
	}

	public static String debugToStringNoRecur(String indent, Object of) {
		String total = "";

		if (of == null)
			return "((null))";

		Class<?> ofc = of.getClass();
		if (ofc.isPrimitive())
			return indent + of;
		else if (of instanceof Number) {
			return indent + of;
		} else if (of instanceof Boolean) {
			return indent + of;
		} else if (of instanceof String) {
			return indent + of;
		}

		do {

			Method [] m= ofc.getDeclaredMethods();
			for(int i=0;i<m.length;i++)
				total += indent + m[i]+"\n";
			
			Field[] fields = ofc.getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				Object value;
				if (fields[i].getName().indexOf("$") == -1)
					try {
						fields[i].setAccessible(true);
						value = fields[i].get(of);

						if (value == null) {
							total += indent + ANSIColorUtils.yellow(fields[i].getName()) + "= null\n";
						} else if (value.getClass().isArray()) {
							total += indent + ANSIColorUtils.yellow(fields[i].getName()) + "= [(" + Array.getLength(value) + ")\n";
							for (int j = 0; j < Array.getLength(value); j++)
								total += " " + Array.get(value, j);
							total += indent + "]\n";
						} else if (value.getClass().isPrimitive()) {
							total += indent + ANSIColorUtils.yellow(fields[i].getName()) + "= " + value + "\n";
						} else if (value instanceof Number) {
							total += indent + ANSIColorUtils.yellow(fields[i].getName()) + "= " + value + "\n";
						} else if (value instanceof Boolean) {
							total += indent + ANSIColorUtils.yellow(fields[i].getName()) + "= " + value + "\n";
						} else if (value instanceof String) {
							total += indent + ANSIColorUtils.yellow(fields[i].getName()) + "= " + value + "\n";
						} else {
							total += indent + ANSIColorUtils.yellow(fields[i].getName()) + "= [(" + value.getClass() + ") \n" + (value) + "\n";
						}
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
			}
			ofc = ofc.getSuperclass();
		} while (ofc != null);
		return total;
	}

	public static Method[] findAllMethodsCalled(Class from, String called) {
		Method[] methods = getAllMethods(from);
		List<Method> r = new ArrayList<Method>();
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().equals(called))
				r.add(methods[i]);
		}
		return r.toArray(new Method[0]);
	}

	/***********************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************
	 * @param string
	 * @return *
	 **********************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************/
	public static Method findFirstMethodCalled(Class from, String called) {
		Method[] methods = getAllMethods(from);
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().equals(called)) {
				methods[i].setAccessible(true);
				return methods[i];
			}
		}
		return null;
	}

	public static Method findFirstMethodCalled(Class from, String called, int numArgs) {
		Method[] methods = getAllMethods(from);
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().equals(called) && methods[i].getParameterTypes().length == numArgs) {
				methods[i].setAccessible(true);
				return methods[i];
			}
		}
		return null;
	}

	/**
	 * @param classes
	 * @param method
	 * @return
	 */
	public static Method findMethodWithParameters(Class[] classes, Method[] method) {
		for (int i = 0; i < method.length; i++) {
			Class[] c = method[i].getParameterTypes();
			if (c.length == classes.length) {
				boolean cool = true;
				for (int m = 0; m < c.length; m++) {
					if (!classes[m].isAssignableFrom(c[m])) {
						cool = false;
						break;
					}
				}
				if (cool) {
					method[i].setAccessible(true);
					return method[i];
				}
			}
		}
		return null;
	}

	/**
	 * @param classes
	 * @param method
	 * @return
	 */
	public static Method findMethodWithParameters(String name, Class[] classes, Method[] method) {
		for (int i = 0; i < method.length; i++) {
			Class[] c = method[i].getParameterTypes();
			if (method[i].getName().equals(name))
				if (c.length == classes.length) {
					boolean cool = true;
					for (int m = 0; m < c.length; m++) {
						if (!classes[m].isAssignableFrom(c[m])) {
							cool = false;
							break;
						}
					}
					if (cool) {
						method[i].setAccessible(true);
						return method[i];
					}
				}
		}
		return null;
	}

	/**
	 * @param classes
	 * @param method
	 * @return
	 */
	public static Method findMethodWithParametersUpwards(String name, Class[] parameters, Class startAt) {
		Class oStartAt = startAt;
		while (startAt != null) {
			{
				Method[] method = startAt.getDeclaredMethods();
				for (int i = 0; i < method.length; i++) {
					Class[] c = method[i].getParameterTypes();
					if (method[i].getName().equals(name))
						if (c.length == parameters.length) {
							boolean cool = true;
							for (int m = 0; m < c.length; m++) {
								if (!parameters[m].isAssignableFrom(c[m])) {
									cool = false;
									break;
								}
							}
							if (cool) {
								method[i].setAccessible(true);
								return method[i];
							}
						}
				}
			}
			Class[] inters = startAt.getInterfaces();
			for (int n = 0; n < inters.length; n++) {
				Method[] method = inters[n].getDeclaredMethods();
				for (int i = 0; i < method.length; i++) {
					Class[] c = method[i].getParameterTypes();
					if (method[i].getName().equals(name))
						if (c.length == parameters.length) {
							boolean cool = true;
							for (int m = 0; m < c.length; m++) {
								if (!parameters[m].isAssignableFrom(c[m])) {
									cool = false;
									break;
								}
							}
							if (cool) {
								method[i].setAccessible(true);
								return method[i];
							}
						}
				}
			}
			startAt = startAt.getSuperclass();
		}

		return null;
	}

	/**
	 * @param classes
	 * @param method
	 * @return
	 */
	public static Method findMethodWithParametersUpwardsNotIncluding(String name, Class[] parameters, Class startAtNotIncluding) {
		Class oStartAt = startAtNotIncluding;
		while (startAtNotIncluding != null) {
			Class[] inters = startAtNotIncluding.getInterfaces();
			for (int n = 0; n < inters.length; n++) {
				Method[] method = inters[n].getDeclaredMethods();
				for (int i = 0; i < method.length; i++) {
					Class[] c = method[i].getParameterTypes();
					if (method[i].getName().equals(name))
						if (c.length == parameters.length) {
							boolean cool = true;
							for (int m = 0; m < c.length; m++) {
								if (!parameters[m].isAssignableFrom(c[m])) {
									cool = false;
									break;
								}
							}
							if (cool) {
								method[i].setAccessible(true);
								return method[i];
							}
						}
				}
			}
			startAtNotIncluding = startAtNotIncluding.getSuperclass();
			if (startAtNotIncluding != null) {
				Method[] method = startAtNotIncluding.getDeclaredMethods();
				for (int i = 0; i < method.length; i++) {
					Class[] c = method[i].getParameterTypes();
					if (method[i].getName().equals(name))
						if (c.length == parameters.length) {
							boolean cool = true;
							for (int m = 0; m < c.length; m++) {
								if (!parameters[m].isAssignableFrom(c[m])) {
									cool = false;
									break;
								}
							}
							if (cool) {
								method[i].setAccessible(true);
								return method[i];
							}
						}
				}
			}
		}

		return null;
	}

	public static void forceLoad(Class[] forceLoad) {
		for (int i = 0; i < forceLoad.length; i++) {
			Field[] f = getAllFields(forceLoad[i]);
			for (int j = 0; j < f.length; j++) {
				try {
					f[j].get(null);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}

	static public Constructor[] getAllConstructors(Class of) {
		Constructor[] ret = (Constructor[]) constructorsCache.get(of);
		if (ret == null) {
			List constructorsList = new ArrayList();
			_getAllConstructors(of, constructorsList);
			constructorsCache.put(of, ret = (Constructor[]) constructorsList.toArray(new Constructor[0]));
		}
		return ret;
	}

	static public Field[] getAllFields(Class of) {
		Field[] ret = (Field[]) fieldsCache.get(of);
		if (ret == null) {
			List fieldsList = new ArrayList();
			_getAllFields(of, fieldsList);
			fieldsCache.put(of, ret = (Field[]) fieldsList.toArray(new Field[0]));
		}
		return ret;
	}

	static public Method[] getAllMethods(Class of) {
		Method[] ret = (Method[]) methodsCache.get(of);
		if (ret == null) {
			ArrayList methodsList = new ArrayList();
			_getAllMethods(of, methodsList);
			methodsCache.put(of, ret = (Method[]) methodsList.toArray(new Method[0]));
		}
		return ret;
	}

	static public Field getFirstFIeldCalled(Class of, String name) {
		Field[] allFields = getAllFields(of);
		for (Field f : allFields) {
			if (f.getName().equals(name)) {
				f.setAccessible(true);
				return f;
			}
		}
		return null;
	}

	static public Field getFirstFIeldCalled(Class of, String name, Class type) {
		Field[] allFields = getAllFields(of);
		for (Field f : allFields) {
			if (f.getName().equals(name) && f.getType().isAssignableFrom(type)) {
				f.setAccessible(true);
				return f;
			}
		}
		return null;
	}

	public static Object illegalGetObject(Class client, String string) {
		Field[] f = getAllFields(client);
		for (int i = 0; i < f.length; i++) {
			if (f[i].getName().equals(string)) {
				f[i].setAccessible(true);
				try {
					return f[i].get(null);
				} catch (IllegalArgumentException e) {
					// TODO
					// Auto-generated
					// catch
					// block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO
					// Auto-generated
					// catch
					// block
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	public static Object illegalGetObject(Object client, String string) {
		Field[] f = getAllFields(client.getClass());
		for (int i = 0; i < f.length; i++) {
			if (f[i].getName().equals(string)) {
				f[i].setAccessible(true);
				try {
					return f[i].get(client);
				} catch (IllegalArgumentException e) {
					// TODO
					// Auto-generated
					// catch
					// block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO
					// Auto-generated
					// catch
					// block
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	public static Object illegalSetObject(Class o, String string, Object to) {
		Field[] f = getAllFields(o);
		for (int i = 0; i < f.length; i++) {
			if (f[i].getName().equals(string)) {
				f[i].setAccessible(true);
				try {
					f[i].set(null, to);
				} catch (IllegalArgumentException e) {
					// TODO
					// Auto-generated
					// catch
					// block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO
					// Auto-generated
					// catch
					// block
					e.printStackTrace();
				}
			}
		}
		return to;
	}

	public static Object illegalSetObject(Object o, String string, Object to) {
		Field[] f = getAllFields(o.getClass());
		for (int i = 0; i < f.length; i++) {
			if (f[i].getName().equals(string)) {
				f[i].setAccessible(true);
				try {
					f[i].set(o, to);
				} catch (IllegalArgumentException e) {
					// TODO
					// Auto-generated
					// catch
					// block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO
					// Auto-generated
					// catch
					// block
					e.printStackTrace();
				}
			}
		}
		return to;
	}

	public static Object illegalSetObjectPrimativeAware(Object o, String string, Object to) {
		Field[] f = getAllFields(o.getClass());
		boolean found = false;
		for (int i = 0; i < f.length; i++) {
			if (f[i].getName().equals(string)) {
				f[i].setAccessible(true);
				try {
					if (f[i].getType().isPrimitive()) {
						if (f[i].getType() == Integer.TYPE)
							to = ((Number) to).intValue();
						else if (f[i].getType() == Float.TYPE)
							to = ((Number) to).floatValue();
						else if (f[i].getType() == Double.TYPE)
							to = ((Number) to).floatValue();
						else if (f[i].getType() == Short.TYPE)
							to = ((Number) to).shortValue();
						else if (f[i].getType() == Byte.TYPE)
							to = ((Number) to).byteValue();
						else if (f[i].getType() == Boolean.TYPE)
							to = ((Number) to).doubleValue() > 0;
					}

					f[i].set(o, to);
					found = true;

				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		assert found : "not found " + o + " " + string + " " + to + " " + o.getClass();
		if (!found) {
			System.err.println(" not found " + o + " " + string + " " + to + " " + o.getClass());
			new Exception().printStackTrace();
		}
		return to;

	}

	public static int indexMax(Collection over, Object run) {
		Method m = run.getClass().getDeclaredMethods()[0];
		float min = Float.NEGATIVE_INFINITY;
		Iterator i = over.iterator();
		int ret = -1;
		m.setAccessible(true);
		int r = 0;
		try {
			while (i.hasNext()) {
				Object arg = i.next();
				float f = ((Number) m.invoke(run, new Object[] { arg })).floatValue();
				if (f > min) {
					min = f;
					ret = r;
				}
				r++;
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return ret;

	}

	/**
	 * runs the default, no-arg constrctor of this plane. Returns only null
	 * (and a .printStackTrace()) on failure
	 */
	public static Object instantiate(Class clazz) {
		try {
			Constructor c = clazz.getConstructor(new Class[] {});
			Object i = c.newInstance(new Object[] {});
			return i;
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Object instantiate(Class clazz, Object[] parameters) {
		try {
			Constructor[] c = clazz.getConstructors();
			for (int m = 0; m < c.length; m++) {
				Class[] t = c[m].getParameterTypes();
				if (t.length == parameters.length) {
					boolean good = true;
					for (int l = 0; l < t.length; l++) {
						if (!((parameters[l] == null) || t[l].isAssignableFrom(parameters[l].getClass()) || samePrimative(t[l], parameters[l].getClass()))) {
							good = false;
							break;
						}
					}
					if (good) {
						return c[m].newInstance(parameters);
					}
				}
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	static public <T> T listProxy(final Collection<T> t, Class<? super T> clazz) {
		return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, new InvocationHandler() {

			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				method.setAccessible(true);
				for (T tt : new ArrayList<T>(t)) {
					method.invoke(tt, args);
				}
				return null;
			}

		});
	}

	static public Method methodOf(String name, Class clazz, Class... parameters) throws IllegalArgumentException {
		try {
			Method method = clazz.getMethod(name, parameters);
			method.setAccessible(true);
			return method;
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		}
		try {
			Method declaredMethod = clazz.getDeclaredMethod(name, parameters);
			declaredMethod.setAccessible(true);
			return declaredMethod;
		} catch (SecurityException e) {
			throw ((IllegalArgumentException) new IllegalArgumentException().initCause(e));
		} catch (NoSuchMethodException e) {
			throw ((IllegalArgumentException) new IllegalArgumentException().initCause(e));
		}
	}

	public static <T> T newInstance(Class<? extends T> histogramClass) {
		try {
			return histogramClass.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static <T> T reflectiveCopy(T playable) throws CloneNotSupportedException {
		try {
			T ret = (T) playable.getClass().newInstance();
			Field[] allFields = getAllFields(playable.getClass());
			for (Field f : allFields) {
				if (!Modifier.isStatic(f.getModifiers())) {
					f.setAccessible(true);
					f.set(ret, f.get(playable));
				}
			}
			return ret;
		} catch (Throwable t) {
			throw ((CloneNotSupportedException) new CloneNotSupportedException().initCause(t));
		}
	}

	public static boolean samePrimative(Class c1, Class c2) {
		if ((c1 == Integer.TYPE) && (c2 == Integer.class))
			return true;
		if ((c1 == Float.TYPE) && (c2 == Float.class))
			return true;
		if ((c1 == Double.TYPE) && (c2 == Double.class))
			return true;
		if ((c1 == Boolean.TYPE) && (c2 == Boolean.class))
			return true;
		if ((c1 == Short.TYPE) && (c2 == Short.class))
			return true;
		if ((c1 == Character.TYPE) && (c2 == Character.class))
			return true;
		if ((c1 == Byte.TYPE) && (c2 == Byte.class))
			return true;
		return false;
	}

	static protected void _getAllConstructors(Class of, List into) {
		if (of == null)
			return;
		Constructor[] m = of.getDeclaredConstructors();
		into.addAll(Arrays.asList(m));
		_getAllConstructors(of.getSuperclass(), into);
		Class[] interfaces = of.getInterfaces();
		for (int i = 0; i < interfaces.length; i++)
			_getAllConstructors(interfaces[i], into);
	}

	static protected void _getAllFields(Class of, List into) {
		if (of == null)
			return;
		Field[] m = of.getDeclaredFields();
		List list = Arrays.asList(m);
		Collections.sort(list, fieldComparator);
		into.addAll(list);
		_getAllFields(of.getSuperclass(), into);
		Class[] interfaces = of.getInterfaces();
		for (int i = 0; i < interfaces.length; i++)
			_getAllFields(interfaces[i], into);
	}

	static protected void _getAllMethods(Class of, List into) {
		if (of == null)
			return;
		Method[] m = of.getDeclaredMethods();
		Arrays.sort(m, methodComparator);
		List list = Arrays.asList(m);
		into.addAll(list);
		_getAllMethods(of.getSuperclass(), into);
		Class[] interfaces = of.getInterfaces();
		for (int i = 0; i < interfaces.length; i++)
			_getAllMethods(interfaces[i], into);
	}

	static protected Field getField(Class from, String called) {
		Field[] ff = getAllFields(from);
		for (int i = 0; i < ff.length; i++) {
			if (ff[i].getName().equals(called))
				return ff[i];
		}
		return null;
	}

}