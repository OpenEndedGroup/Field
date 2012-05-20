package field.bytecode.protect.dispatch;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

import org.objectweb.asm.Type;

import field.namespace.generic.Generics.Pair;

/**
 * translates asm method's to refllectionmethods and, more importantly, caches the result
 *
 * @author marc
 *
 */
public class MethodUtilities {

	HashMap<Pair<org.objectweb.asm.commons.Method, String>, Method> cache = new HashMap<Pair<org.objectweb.asm.commons.Method, String>,  Method>();

	Pair<org.objectweb.asm.commons.Method, String> ch = new Pair<org.objectweb.asm.commons.Method, String>(null, null);

	Method first = null;


	public Method getMethodFor(ClassLoader loader, org.objectweb.asm.commons.Method method, Class onClass, String className) {

		Method m = getPossibleMethodFor(loader, method, onClass, className);
		if (m == null) {
			String name = method.getName();
			Type[] argumentTypes = method.getArgumentTypes();
			Class[] classTypes = new Class[argumentTypes.length];

			;//System.out.println(" -- trouble ");
			;//System.out.println("  onClass <"+onClass+">");
			;//System.out.println("   onClassLoader <" + onClass.getClassLoader() + ">");
			for (int i = 0; i < classTypes.length; i++)
				;//System.out.println("          " + classTypes[i].getClassLoader());

			throw new IllegalArgumentException(onClass + " " + name + " " + Arrays.asList(classTypes));
		}
		return m;
	}

	public Method getPossibleMethodFor(ClassLoader loader, org.objectweb.asm.commons.Method method, Class onClass, String className) {

		ch.left = method;
		ch.right = className;

		Method m = cache.get(ch);
		if (m != null || cache.containsKey(ch))
		{
			return m;
		}

		if (onClass == null)
			try {
				onClass = loader.loadClass(className.replace('/', '.'));
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			}

		String name = method.getName();
		Type[] argumentTypes = method.getArgumentTypes();
		Class[] classTypes = new Class[argumentTypes.length];
		for (int i = 0; i < argumentTypes.length; i++) {
			if (argumentTypes[i].getSort() == Type.OBJECT) {
				try {
					classTypes[i] = loader.loadClass(argumentTypes[i].getClassName());
				} catch (ClassNotFoundException e) {
					assert false : argumentTypes[i].getClassName();
					e.printStackTrace();
				}
			} else {
				switch (argumentTypes[i].getSort()) {
				case (Type.INT):
					classTypes[i] = Integer.TYPE;
					break;
				case (Type.FLOAT):
					classTypes[i] = Float.TYPE;
					break;
				case (Type.DOUBLE):
					classTypes[i] = Double.TYPE;
					break;
				case (Type.LONG):
					classTypes[i] = Long.TYPE;
					break;
				case (Type.SHORT):
					classTypes[i] = Short.TYPE;
					break;
				case (Type.ARRAY):
					Type elementType = argumentTypes[i].getElementType();
					if (elementType.getSort() == Type.OBJECT)
						try {
							// ugh!
							classTypes[i] = Array.newInstance(loader.loadClass(elementType.getClassName()), 0).getClass();
						} catch (ClassNotFoundException e) {
							assert false : elementType.getClassName() + " " + argumentTypes[i].getClassName();
							e.printStackTrace();
						}
					else
						switch (elementType.getSort()) {
						case (Type.INT):
							classTypes[i] = int[].class;
							break;
						case (Type.FLOAT):
							classTypes[i] = float[].class;
							break;
						case (Type.DOUBLE):
							classTypes[i] = double[].class;
							break;
						case (Type.LONG):
							classTypes[i] = long[].class;
							break;
						case (Type.SHORT):
							classTypes[i] = short[].class;
							break;
						default:
							assert false : "unknown array sort, " + argumentTypes[i].getSort();
						}
					break;
				default:
					assert false : "unknown sort, " + argumentTypes[i].getSort();
				}
			}
		}

		try {
			java.lang.reflect.Method found = m = onClass.getDeclaredMethod(name, classTypes);
			m.setAccessible(true);
			cache.put(ch, found);
			ch = new Pair<org.objectweb.asm.commons.Method, String>(null, null);


			first = m;
			return m;
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		}
		try {
			java.lang.reflect.Method found = m = onClass.getMethod(name, classTypes);
			m.setAccessible(true);
			cache.put(ch, found);
			ch = new Pair<org.objectweb.asm.commons.Method, String>(null, null);

			first = m;
			return m;
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		}

		cache.put(ch, null);
		ch = new Pair<org.objectweb.asm.commons.Method, String>(null, null);
		return null;
	}
}
