package field.bytecode.protect.dispatch;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import field.bytecode.protect.iInside;
import field.bytecode.protect.annotations.InsideParameter;
import field.namespace.generic.ReflectionTools;


public class InsideSupport {

	static Map<String, Map<Class, Collection<Field>>> cachedInsideFields = new HashMap<String, Map<Class, Collection<Field>>>();

	static Map<String, Map<Object, Integer>> inside = new HashMap<String, Map<Object, Integer>>();

	static public void enter(Object instance, String group) {
		if (group == null) group = "";
		Map<Object, Integer> insideGroup = inside.get(group);
		if (insideGroup == null) {
			inside.put(group, insideGroup = new WeakHashMap<Object, Integer>());
		}
		Integer integer = insideGroup.get(instance);
		if (integer != null) {
			insideGroup.put(instance, integer + 1);
			return;
		}
		insideGroup.put(instance, 1);

		Map<Class, Collection<Field>> cache = cachedInsideFields.get(group);
		if (cache == null) cachedInsideFields.put(group, cache = new HashMap<Class, Collection<Field>>());

		Collection<Field> c = cache.get(instance.getClass());
		if (c == null) cache.put(instance.getClass(), c = findFieldsForGroupAndClass(instance.getClass(), group));

		for (Field f : c) {
			Object object = null;
			try {
				object = f.get(instance);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			peformEntryOn(object);
		}
	}

	private static Collection<Field> findFieldsForGroupAndClass(Class< ? extends Object> clazz, String group) {
		ArrayList<Field> ret = new ArrayList<Field>();

		Field[] allFields = ReflectionTools.getAllFields(clazz);
		for (Field f : allFields) {
			InsideParameter a = f.getAnnotation(InsideParameter.class);
			if (a != null) {
				if (a.group().equals(group)) {
					f.setAccessible(true);
					ret.add(f);
				}
			}
		}
		return ret;
	}

	static public void exit(Object instance, String group) {
		if (group == null) group = "";
		Map<Object, Integer> insideGroup = inside.get(group);
		assert insideGroup != null : "exit without any kind of group entry";
		Integer integer = insideGroup.get(instance);
		assert integer != null : "exit without entry";
		if (integer != 1) {
			insideGroup.put(instance, integer - 1);
			return;
		}
		insideGroup.remove(instance);

		Collection<Field> c = cachedInsideFields.get(group).get(instance.getClass());
		for (Field f : c) {
			Object object = null;
			try {
				object = f.get(instance);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			performExitOn(object);
		}
	}

	private static void performExitOn(Object object) {
		if (object != null) {
			((iInside) object).close();
		}
	}

	private static void peformEntryOn(Object object) {
		if (object != null) {
			((iInside) object).open();
		}
	}

}
