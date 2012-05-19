package field.bytecode.protect;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

import field.launch.iUpdateable;

public class DeferedInQueue extends BasicInstrumentation2.DeferCallingFast {

	private final boolean through;

	public interface iProvidesQueue {
		public iRegistersUpdateable getQueueFor(java.lang.reflect.Method m);
	}

	public DeferedInQueue(String name, int access, Method onMethod, ClassVisitor classDelegate, MethodVisitor delegateTo, String signature, HashMap<String, Object> parameters) {
		this(name, access, onMethod, classDelegate, delegateTo, signature, parameters, false);
	}

	public DeferedInQueue(String name, int access, Method onMethod, ClassVisitor classDelegate, MethodVisitor delegateTo, String signature, HashMap<String, Object> parameters, boolean through) {
		super(name, access, onMethod, classDelegate, delegateTo, signature, parameters);
		this.through = through;
	}

	java.lang.reflect.Method original = null;

	static public class AtomicIntegerMap<T> {

		Map<T, AtomicInteger> m = new HashMap<T, AtomicInteger>();

		public boolean test(T t) {
			synchronized (m) {
				AtomicInteger o = m.get(t);
				if (o == null || o.get() == 0)
					return false;
				return true;
			}
		}

		public void inc(T t) {
			synchronized (m) {
				AtomicInteger o = m.get(t);
				if (o == null)
					m.put(t, new AtomicInteger(1));
				else {
					o.incrementAndGet();
				}
			}
		}

		public void dec(T t) {
			synchronized (m) {
				AtomicInteger o = m.get(t);
				if (o == null)
					throw new IllegalStateException();
				else {
					int m = o.decrementAndGet();
					if (m < 0) {
						throw new IllegalStateException();
					}
				}
			}

		}
	}

	public interface iRegistersUpdateable {
		public void deregisterUpdateable(iUpdateable up);
	
		public void registerUpdateable(iUpdateable up);
	}

	static AtomicIntegerMap<iRegistersUpdateable> inside = new AtomicIntegerMap<iRegistersUpdateable>();

	@Override
	public Object handle(int fromName, final Object fromThis, final String originalMethod, final Object[] argArray) {

		if (original == null) {
			java.lang.reflect.Method[] all = StandardTrampoline.getAllMethods(fromThis.getClass());
			outer: for (java.lang.reflect.Method m : all) {
				if (m.getName().equals(originalMethod)) {
					Class<?>[] p = m.getParameterTypes();
					if (p.length == argArray.length) {

						// System.out.println(" looking at <"+Arrays.asList(p)+"> <"+Arrays.asList(argArray)+">");

						for (int i = 0; i < p.length; i++) {
							// System.out.println(argArray[i]+" "+p[i]+" "+p[i].isAssignableFrom(argArray[i].getClass()));

							if (p[i].isPrimitive()) {
								if (p[i] == Integer.TYPE && !(argArray[i] instanceof Integer))
									continue outer;
								if (p[i] == Long.TYPE && !(argArray[i] instanceof Long))
									continue outer;
								if (p[i] == Short.TYPE && !(argArray[i] instanceof Short))
									continue outer;
								if (p[i] == Double.TYPE && !(argArray[i] instanceof Double))
									continue outer;
								if (p[i] == Byte.TYPE && !(argArray[i] instanceof Byte))
									continue outer;
								if (p[i] == Character.TYPE && !(argArray[i] instanceof Character))
									continue outer;
								if (p[i] == Float.TYPE && !(argArray[i] instanceof Float))
									continue outer;
							} else if (!(argArray[i] == null || p[i].isAssignableFrom(argArray[i].getClass())))
								continue outer;
						}

						original = m;
						break;
					}
				}
			}
			assert original != null : originalMethod;
		}

		if (original == null) {
			System.out.println(" couldn't find <" + originalMethod + ">");
		}
		original.setAccessible(true);

		boolean doSaveAliasing = parameters.get("saveAliasing") == null ? true : ((Boolean) parameters.get("saveAliasing")).booleanValue();
		boolean doSaveContextLocation = parameters.get("saveAliasing") == null ? true : ((Boolean) parameters.get("saveAliasing")).booleanValue();


		final iRegistersUpdateable queue = ((iProvidesQueue) fromThis).getQueueFor(original);

		final Exception stack = new Exception();

//		System.out.println(" exec :" + originalMethod + " " + inside.test(queue) + " " + through+" ");

		if (through && inside.test(queue)) {
			go(fromThis, originalMethod, argArray, queue, stack, null);
			return null;
		}

		iUpdateable u = new iUpdateable() {

			public void update() {
				go(fromThis, originalMethod, argArray, queue, stack, this);
			}

		};
		queue.registerUpdateable(u);

		return null;
	}

	private void go(final Object fromThis, final String originalMethod, final Object[] argArray, final iRegistersUpdateable queue, final Exception stack, iUpdateable out) {
		try {

			inside.inc(queue);

			original.invoke(fromThis, argArray);
		} catch (IllegalArgumentException e) {
			System.err.println(" inside DeferedInQueue +" + fromThis + " " + originalMethod + " " + Arrays.asList(argArray));
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			System.err.println(" inside DeferedInQueue +" + fromThis + " " + originalMethod + " " + Arrays.asList(argArray));
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// System.err.println("
			// inside
			// DeferedInQueue
			// +" +
			// fromThis
			// + "
			// " +
			// originalMethod
			// + "
			// " +
			// Arrays.asList(argArray));
			stack.printStackTrace();
			e.printStackTrace();
		} catch (Throwable t) {
			System.err.println(" inside DeferedInQueue +" + fromThis + " " + originalMethod + " " + Arrays.asList(argArray));
			t.printStackTrace();
		} finally {
			if (out != null)
				queue.deregisterUpdateable(out);
			inside.dec(queue);

		}
	}

}
