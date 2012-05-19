package field.bytecode.protect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.objectweb.asm.Type;

import field.bytecode.protect.annotations.ConstantContext;
import field.namespace.context.CT.ContextTopology;
import field.namespace.generic.ReflectionTools;
import field.namespace.generic.Generics.Pair;

public class Cc {

	static public class ContextFor {
		boolean constant = true;

		Object contextOnCurrentEntry;

		Object contextTarget;

		int entryCount = 0;

		boolean immediate = true;

		ContextTopology  on = null;

		boolean resets = true;

		public void enter() {

			if (entryCount == 0) contextOnCurrentEntry = on.getAt();
			if (entryCount == 0 && contextTarget != null) on.setAt(contextTarget);
			entryCount++;
		}

		public void exit() {
			entryCount--;
			if (entryCount == 0) if (immediate || on.getAt() != contextOnCurrentEntry) if (contextTarget == null || !constant) contextTarget = on.getAt();
			if (resets && entryCount == 0) on.setAt(contextOnCurrentEntry);

			assert entryCount >= 0;
		}
	}

	static WeakHashMap<Object, Map<String, ContextFor>> contextMemories = new WeakHashMap<Object, Map<String, ContextFor>>();

	static public void handle_entry(Object fromThis, String name, Map<String, Object> parameterName, Map<Integer, Pair<String, String>> markedArguments, Object[] arguments) {

		Map<String, ContextFor> cf = contextMemories.get(fromThis);
		if (cf == null) {
			contextMemories.put(fromThis, cf = new HashMap<String, ContextFor>());
		}
		String n2 = (String) parameterName.get("group");

		if (n2 == null || n2.equals("--object--"))
			n2 = "for:" + System.identityHashCode(fromThis);
		else if (n2.equals("--method--")) n2 = ((org.objectweb.asm.commons.Method) parameterName.get("method")).getName();


		ContextFor context = cf.get(n2);
		if (context == null) {
			context = new ContextFor();
			if (parameterName.containsKey("immediate")) context.immediate = (Boolean) parameterName.get("immediate");
			if (parameterName.containsKey("constant")) context.constant = (Boolean) parameterName.get("constant");
			if (parameterName.containsKey("resets")) context.resets = (Boolean) parameterName.get("resets");

			try {
				context.on = ContextAnnotationTools.contextFor(fromThis, parameterName, markedArguments, arguments);
			} catch (Exception e) {
				e.printStackTrace();
				Error ee = new Error();
				ee.initCause(e);
			}

			if (parameterName.containsKey("topology")) {
				try {
					Type c = (Type) parameterName.get("topology");
					Class< ? > cloaded = fromThis.getClass().getClassLoader().loadClass(c.getClassName());
					if (cloaded != Object.class) {
						Field contextField;
						contextField = cloaded.getField("context");
						Object cc = contextField.get(null);
						context.on = (ContextTopology) cc;
					}
				} catch (Throwable t) {
					t.printStackTrace();
					assert false;
				}
				if (context.on == null)
					throw new Error(" no context for Cc");
			}

			cf.put(n2, context);
		}

		context.enter();
	}

	static public void handle_exit(Object fromThis, String name, Map<String, Object> parameterName) {
		Map<String, ContextFor> cf = contextMemories.get(fromThis);
		if (cf == null) {
			contextMemories.put(fromThis, cf = new HashMap<String, ContextFor>());
		}
		String n2 = (String) parameterName.get("group");
		if (n2 == null || n2.equals("--object--"))
			n2 = "for:" + System.identityHashCode(fromThis);
		else if (n2.equals("--method--")) n2 = ((org.objectweb.asm.commons.Method) parameterName.get("method")).getName();

		ContextFor context = cf.get(n2);
		assert context != null : cf + " " + n2;

		context.exit();
	}

	static public void setContextFor(Object fromThis, Method m, Object target) {
		m = ReflectionTools.findMethodWithParametersUpwards(m.getName(), m.getParameterTypes(), fromThis.getClass());
		assert m != null;

		ConstantContext c = m.getAnnotation(ConstantContext.class);

		String n2 = c.group();
		if (n2.equals("--method--")) n2 = m.getName();

		Map<String, ContextFor> cf = contextMemories.get(fromThis);
		if (cf == null) {
			contextMemories.put(fromThis, cf = new HashMap<String, ContextFor>());
		}

		ContextFor context = cf.get(n2);
		if (context == null) context = new ContextFor();

		context.immediate = c.immediate();
		context.constant = c.constant();
		context.resets = c.resets();
		context.contextTarget = target;

		cf.put(n2, context);
	}

}
