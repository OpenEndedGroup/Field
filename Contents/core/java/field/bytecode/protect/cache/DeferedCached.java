/**
 *
 */
package field.bytecode.protect.cache;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

import field.bytecode.protect.BasicInstrumentation2;
import field.bytecode.protect.StandardTrampoline;
import field.bytecode.protect.annotations.CacheParameter;

public final class DeferedCached extends BasicInstrumentation2.DeferCallingFast {

	private final HashMap<String, Object> parameters;

	Map<ImmutableArrayWrapper, Object> cache;

	List<Field> implicatedFields = null;

	int maxSize = 100;

	java.lang.reflect.Method original = null;

	public DeferedCached(String name, int access, Method method, ClassVisitor delegate, MethodVisitor to, String signature, HashMap<String, Object> parameters) {
		super(name, access, method, delegate, to, signature, parameters);
		this.parameters = parameters;
		final Integer max = (Integer) parameters.get("max");
		if (max == null || max == -1) {
			cache = new WeakHashMap<ImmutableArrayWrapper, Object>();
		} else {
			cache = new LinkedHashMap<ImmutableArrayWrapper, Object>() {
				@Override
				protected boolean removeEldestEntry(Entry<ImmutableArrayWrapper, Object> eldest) {
					if (size() > max)
						return true;
					return false;
				}
			};
		}
	}

	@Override
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
		if (implicatedFields == null) {
			implicatedFields = new ArrayList<Field>();
			Field[] allFields = StandardTrampoline.getAllFields(fromThis.getClass());
			for (Field f : allFields) {

				CacheParameter ann = f.getAnnotation(CacheParameter.class);
				if (ann != null) {
					if ((ann.name() == null && parameters.get("name") == null) || ann.name().equals(parameters.get("name"))) {
						f.setAccessible(true);
						implicatedFields.add(f);
					}
				}
			}
		}
		if (implicatedFields.size() == 0) {

			// first check the cache
			ImmutableArrayWrapper iaw = new ImmutableArrayWrapper(argArray, false);

			Object object = cache.get(iaw);
			if (object == null && !cache.containsKey(iaw)) {
				try {
//					;//System.out.println(" cache miss");
					cache.put(iaw, object = original.invoke(fromThis, argArray));
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
			else
			{
//				;//System.out.println(" cache hit");
			}
			return object;
		}
		Object[] na = new Object[argArray.length + implicatedFields.size()];
		System.arraycopy(argArray, 0, na, 0, argArray.length);
		for (int i = 0; i < implicatedFields.size(); i++)
			try {
				na[argArray.length + i] = implicatedFields.get(i).get(fromThis);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		ImmutableArrayWrapper iaw = new ImmutableArrayWrapper(na, false);

		Object object = cache.get(iaw);
		if (object == null && !cache.containsKey(iaw)) {
			try {
				cache.put(iaw, object = original.invoke(fromThis, argArray));
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		} 
//		else
//			System.err.println(" cache hit ");
//		
		return object;

	}
}