package field.graphics.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import field.core.util.FieldPyObjectAdaptor.iHandlesAttributes;
import field.math.abstraction.iProvider;
import field.namespace.generic.Generics.Pair;
import field.util.BiMap;

public class ContextualUniform {

	static public final ThreadLocal<BiMap<String, String>> tags = new ThreadLocal<BiMap<String, String>>() {
		protected BiMap<String, String> initialValue() {
			return new BiMap<String, String>();
		};
	};

	static public void setTag(String key, String value) {
		tags.get().put(key, value);
	}

	static public class TagGroup {
		Map<String, String> tags = new LinkedHashMap<String, String>();

		Map<String, String> pushed = null;

		public void push() {

			if (Base.trace) if (tags.size() > 0)
				;//System.out.println(" pushing uniform <" + tags + ">");

			pushed = new LinkedHashMap<String, String>();

			Map<String, String> target = ContextualUniform.tags.get();
			for (Map.Entry<String, String> t : tags.entrySet()) {
				String displaced = target.put(t.getKey(), t.getValue());

				pushed.put(t.getKey(), displaced);
			}

		}

		public void pop() {

			if (pushed == null)
				return;
			// throw new IllegalStateException(" pop without push");

			if (Base.trace)  ;//System.out.println(" popping <" + pushed + ">");

			Map<String, String> target = ContextualUniform.tags.get();
			for (Map.Entry<String, String> e : pushed.entrySet()) {
				if (e.getValue() == null) {
					target.remove(e.getKey());
				} else {
					target.put(e.getKey(), e.getValue());
				}
			}

			pushed = null;

		}

		public void put(String key, String value) {
			tags.put(key, value);
		}
	}

	static public class Matches<T> implements iHandlesAttributes {
		List<Pair<String, String>> m = new ArrayList<Pair<String, String>>();

		T value;

		public void add(String key, String value) {
			m.add(new Pair<String, String>(key, value));
		}

		public boolean matches(Map<String, String> current) {
			for (Pair<String, String> s : m) {
				String tt = current.get(s.left);
				if (tt == null && s.right == null)
					continue;
				if (tt == null)
					return false;

				if (!tt.equals(s.right))
					return false;
			}
			return true;
		}

		@Override
		public Object getAttribute(String name) {
			for (Pair<String, String> q : m) {
				if (q.left.equals(name))
					return q.right;
			}
			return null;
		}

		@Override
		public void setAttribute(String name, Object value) {
			for (Pair<String, String> q : m) {
				if (q.left.equals(name)) {
					q.right = "" + value;
					return;
				}
			}
			add(name, "" + value);
		}

		public Matches(T value) {
			this.value = value;
		}

	}

	static public class Multivalue<T> implements iProvider<T> {
		List<Matches<T>> matchers = new ArrayList<Matches<T>>();

		T defaultValue;

		public Multivalue(T value) {
			this.defaultValue = value;

		}

		public Multivalue<T> add(Matches<T> m) {
			matchers.add(m);
			return this;
		}

		@Override
		public T get() {
			BiMap<String, String> target = ContextualUniform.tags.get();
			for (Matches<T> m : matchers) {
				if (m.matches(target))
					return m.value;
			}
			return this.defaultValue;
		}
	}

	static public class FlatContextualValue implements iProvider<Object> {
		LinkedHashMap<String, Object> values = new LinkedHashMap<String, Object>();

		Object defaultValue = null;

		public Object get() {
			BiMap<String, String> target = ContextualUniform.tags.get();

			for (Map.Entry<String, Object> e : values.entrySet()) {
				if (target.containsValue(e.getKey())) {
					return e.getValue();
				}
			}
			return defaultValue;
		}

	}

}
