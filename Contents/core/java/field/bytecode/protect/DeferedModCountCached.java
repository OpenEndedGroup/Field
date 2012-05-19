/**
 * 
 */
package field.bytecode.protect;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;


import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

import field.bytecode.protect.annotations.CacheParameter;
import field.bytecode.protect.cache.ModCountArrayWrapper;
import field.bytecode.protect.cache.ModCountCache;
import field.namespace.generic.Bind.iFunction;

public final class DeferedModCountCached extends BasicInstrumentation2.DeferCallingFast implements iFunction<Object, ModCountArrayWrapper> {

	ModCountCache<ModCountArrayWrapper, Object> cache = new ModCountCache<ModCountArrayWrapper, Object>(new ModCountArrayWrapper(null));

	java.lang.reflect.Method original = null;

	private final HashMap<String, Object> parameters;

	private java.lang.reflect.Method ongoingMethod;

	private Object[] ongoingArgs;

	private ModCountArrayWrapper ongoingIAW;

	private Object originalTarget;

	public DeferedModCountCached(String name, int access, Method method, ClassVisitor delegate, MethodVisitor to, String signature, HashMap<String, Object> parameters) {
		super(name, access, method, delegate, to, signature, parameters);
		this.parameters = parameters;
	}

	public Object handle(int fromName, Object fromThis, String originalMethod, Object[] argArray) {
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

		Object[] na = new Object[argArray.length];
		System.arraycopy(argArray, 0, na, 0, argArray.length);

		ModCountArrayWrapper iaw = new ModCountArrayWrapper(na);

		ongoingMethod = original;
		ongoingArgs = argArray;
		ongoingIAW = iaw;
		originalTarget = fromThis;

		Object object = cache.get(iaw, this);

		return object;

	}

	public Object f(ModCountArrayWrapper in) {
		try {
			Object object = original.invoke(originalTarget, ongoingArgs);
			return object;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}
}