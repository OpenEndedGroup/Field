package field.bytecode.protect;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Type;

import field.bytecode.protect.annotations.FromContext;
import field.namespace.context.CT.ContextTopology;
import field.namespace.context.CT.Dispatch;
import field.namespace.context.CT.iProvidesContextTopology;
import field.namespace.context.CT.iStorage;
import field.namespace.context.CT.iSupportsBeginEnd;
import field.namespace.generic.Generics.Pair;

public class ContextAnnotationTools {

	static public ThreadLocal<HashMap<Class, ArrayList<Pair<Field, FromContext>>>> cachedParameters = new ThreadLocal<HashMap<Class, ArrayList<Pair<Field, FromContext>>>>() {
		@Override
		protected HashMap<Class, ArrayList<Pair<Field, FromContext>>> initialValue() {
			return new HashMap<Class, ArrayList<Pair<Field, FromContext>>>();
		}
	};

	static public void begin(ContextTopology<?, ?> topology, Object value) {
		if (value == null)
			throw new Error(" null value for context_begin ");
		if (topology == null)
			throw new Error(" null topology for context_begin ");
		if (!(topology instanceof iSupportsBeginEnd))
			throw new Error(" topology <" + topology + "> doesn't support begin / end");
		iSupportsBeginEnd<Object> s = (iSupportsBeginEnd<Object>) topology;
		Class<Object> supportedClass = s.getBeginEndSupportedClass();
		if (!supportedClass.isAssignableFrom(value.getClass())) {
			throw new Error(" value is of wrong type <" + value + " / " + value.getClass() + "> not assignable from <" + supportedClass + ">, loaders are <" + value.getClass().getClassLoader() + " " + supportedClass.getClass().getClassLoader() + ">");
		}

		s.begin(value);
	}

	static public ContextTopology<?, ?> contextFor(Object fromThis, Map<String, Object> annotationParameters, Map<Integer, Pair<String, String>> markedArguments, Object[] arguments) throws ClassNotFoundException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Object type = getParameter("topology", annotationParameters, markedArguments, arguments);
		if (type instanceof Type) {
			Class<?> cloaded = fromThis.getClass().getClassLoader().loadClass(((Type) type).getClassName());
			if (cloaded != Object.class) {
				Field contextField;
				contextField = cloaded.getField("context");
				Object cc = contextField.get(null);
				return (ContextTopology<?, ?>) cc;
			}
		} else if (type instanceof ContextTopology) {
			return (ContextTopology<?, ?>) type;
		} else if (type instanceof iProvidesContextTopology) {
			return ((iProvidesContextTopology) type).getContextTopology();
		} else {
			throw new Error(" couldn't find context from <" + annotationParameters + "> <" + markedArguments + "> <" + Arrays.asList(arguments) + ">\n got <" + type + ">,  don't know how to convert that a ContextTopology");
		}

		return null;
	}

	static public void end(ContextTopology<?, ?> topology, Object value) {
		iSupportsBeginEnd<Object> s = (iSupportsBeginEnd<Object>) topology;
		s.end(value);
	}

	static public Object getParameter(String name, Map<String, Object> annotationParameters, Map<Integer, Pair<String, String>> markedArguments, Object[] arguments) {
		Object n = annotationParameters.get(name);
		if (n == null || (n instanceof String && n.equals(""))) {
			for (Map.Entry<Integer, Pair<String, String>> e : markedArguments.entrySet()) {
				if (e.getValue().left.toLowerCase().endsWith(name.toLowerCase() + ";")) {
					return arguments[e.getKey()];
				}
			}
		}
		return n;
	}

	static public void populateContexted(ContextTopology topology, Object inside) {
		Class<? extends Object> c = inside.getClass();

		ArrayList<Pair<Field, FromContext>> parameters = cachedParameters.get().get(c);
		if (parameters == null) {
			parameters = new ArrayList<Pair<Field, FromContext>>();
			Field[] fields = StandardTrampoline.getAllFields(c);
			for (Field f : fields) {
				f.setAccessible(true);
				FromContext ann = f.getAnnotation(FromContext.class);
				if (ann != null) {
					parameters.add(new Pair<Field, FromContext>(f, ann));
				}
			}
			cachedParameters.get().put(c, parameters);
		}

		Dispatch d = new Dispatch(topology);
		iStorage<?>  getter = (iStorage<?>) d.getBackwardsOverrideProxyFor(iStorage.class);


		for (Pair<Field, FromContext> p : parameters) {
			String name = p.right.name();
			if (p.right.name() == null || p.right.name().equals(""))
				name = p.left.getName();

			BaseRef<Object> res = new BaseRef<Object>(null);
			((iStorage) getter).get(name, res);

			try {
				p.left.setAccessible(true);
				p.left.set(inside, res.get());

			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	static public Object valueFor(Object fromThis, Map<String, Object> annotationParameters, Map<Integer, Pair<String, String>> markedArguments, Object[] arguments) {
		Object value = getParameter("value", annotationParameters, markedArguments, arguments);
		return value;
	}

}
