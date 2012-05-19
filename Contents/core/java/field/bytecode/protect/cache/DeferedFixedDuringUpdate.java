package field.bytecode.protect.cache;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.WeakHashMap;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import field.bytecode.protect.BasicInstrumentation2;
import field.bytecode.protect.StandardTrampoline;
import field.launch.Launcher;
import field.launch.iUpdateable;

/**
 * a type of cache of a method that cannot change inside a single update. Cached
 * statically, single parameter only, by speed
 */
public class DeferedFixedDuringUpdate extends BasicInstrumentation2.DeferCallingFast{

	private Method original;

	long count = 0;
	long lastCount = 0;

	WeakHashMap<Object, Object> cache = new WeakHashMap<Object, Object>();

	public DeferedFixedDuringUpdate(String name, int access, org.objectweb.asm.commons.Method method, ClassVisitor delegate, MethodVisitor to, String signature, HashMap<String, Object> parameters) {
		super(name, access, method, delegate, to, signature, parameters);
		Launcher.getLauncher().registerUpdateable(new iUpdateable() {

			public void update() {
				count++;
			}
		});
	}

	Object[] zeroArgs ={}; 
	
	@Override
	public Object handle(int fromName, Object fromThis, String originalMethod, Object[] args) {
		if (original == null) {
			java.lang.reflect.Method[] all = StandardTrampoline.getAllMethods(fromThis.getClass());
			for (java.lang.reflect.Method m : all) {
				if (m.getName().equals(originalMethod)) {
					original = m;
					break;
				}
			}
			original.setAccessible(true);
			assert original != null : originalMethod;
		}
//		assert args.length == 1;

		if (lastCount != count) {
			cache.clear();
			lastCount = count;
		}

		Object c = null;
		if (args.length==1)
			c = cache.get(args[0]);
		else
			c = cache.get(zeroArgs);

		if (c != null)
			return c;
		try {
			Object o = original.invoke(fromThis, args);
			cache.put(args.length==1 ? args[0] : zeroArgs, o);
			return o;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw e;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			IllegalArgumentException ll = new IllegalArgumentException();
			ll.initCause(e);
			throw ll;
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			IllegalArgumentException ll = new IllegalArgumentException();
			ll.initCause(e);
			throw ll;
		}

	}

}
