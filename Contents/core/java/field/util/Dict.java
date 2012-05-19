package field.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.python.core.PyObject;

import field.math.abstraction.iProvider;

public class Dict implements Serializable {
	private static final long serialVersionUID = 4506062700963421662L;

	static public class Prop<T> implements Serializable {
		String name;

		public Prop(String name) {
			this.name = name;
		}

		public boolean containsSuffix(String string) {
			if (name.contains("_")) {
				String[] s = name.split("_");
				for (int i = 1; i < s.length; i++) {
					if (s[i].equals(string))
						return true;
				}
			}
			return false;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final Prop other = (Prop) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		public String getName() {
			return name;
		}

		@Override
		public int hashCode() {
			final int PRIME = 31;
			int result = 1;
			result = PRIME * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public String toString() {
			return name;
		}

	}

	Map<Prop, Object> dictionary = new LinkedHashMap<Prop, Object>();

	public Dict duplicate() {
		Dict r = new Dict();

		r.dictionary = new LinkedHashMap<Prop, Object>(dictionary);
		return r;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Dict other = (Dict) obj;
		if (dictionary == null) {
			if (other.dictionary != null)
				return false;
		} else if (!dictionary.equals(other.dictionary))
			return false;
		return true;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(Prop<T> key) {
		return (T) dictionary.get(key);
	}

	public float getFloat(Prop<? extends Number> n, float def) {
		Object x = dictionary.get(n);
		if (x instanceof Float[])
			return ((Float[]) x)[0].floatValue();
		if (x instanceof float[])
			return ((float[]) x)[0];

		Number gotten = (Number) x;
		if (gotten != null)
			return gotten.floatValue();
		return def;
	}

	public Map<Prop, Object> getMap() {
		return dictionary;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dictionary == null) ? 0 : safeHash(dictionary));
		return result;
	}

	private int safeHash(Map<Prop, Object> d) {

		Set<Entry<Prop, Object>> ee = d.entrySet();
		int c = 0;
		for (Entry<Prop, Object> e : ee) {
			Prop k = e.getKey();
			Object v = e.getValue();
			c += k.hashCode();
			if (v instanceof PyObject)
				c += System.identityHashCode(v);
			else
				c += (v == null ? 0 : v.hashCode());
		}
		return c;
	}

	public boolean isTrue(Prop<?> prop, boolean def) {
		if (!dictionary.containsKey(prop))
			return def;

		Object p = dictionary.get(prop);
		if (p == null)
			return def;
		if (p instanceof Boolean)
			return ((Boolean) p);
		if (p instanceof Number)
			return ((Number) p).intValue() > 0;
		if (p instanceof String)
			return ((String) p).length() > 0;
		return true;
	}

	public <T> iProvider<T> provide(final Prop<T> p) {
		return new iProvider<T>() {
			public T get() {
				return Dict.this.get(p);
			}
		};
	}

	public <T> Dict put(Prop<T> key, T value) {
		dictionary.put(key, value);
		return this;
	}

	@SuppressWarnings("unchecked")
	public Dict putAll(Dict d) {
		for (Map.Entry<Prop, Object> e : d.dictionary.entrySet()) {
			put(e.getKey(), e.getValue());
		}
		return this;
	}

	public <T> Dict putToList(Prop<? extends Collection<T>> key, T value) {
		Collection<T> c = (Collection<T>) dictionary.get(key);
		if (c == null)
			dictionary.put(key, c = new HashSet<T>());
		c.add(value);
		return this;
	}

	public <T> Dict putToMap(Prop<? extends Map<String, T>> key, String tok, T value) {
		Map<String, T> c = (Map<String, T>) dictionary.get(key);
		if (c == null)
			dictionary.put(key, c = new HashMap<String, T>());
		c.put(tok, value);
		return this;
	}

	public <T> Dict putToMapOfLists(Prop<? extends HashMapOfLists<String, T>> key, String tok, T value) {
		HashMapOfLists<String, T> c = (HashMapOfLists<String, T>) dictionary.get(key);
		if (c == null)
			dictionary.put(key, c = new HashMapOfLists<String, T>());
		c.addToList(tok, value);
		return this;
	}

	public <T> void removeFromList(Prop<? extends Collection<T>> p, T q) {
		Collection<T> ll = get(p);
		if (ll == null)
			return;
		ll.remove(q);
	}

	@Override
	public String toString() {
		return ""+dictionary;
	}

	public <T> T remove(Prop<T> t) {
		Object x = dictionary.remove(t);
		return (T) x;
	}

	public long longHash() {
		long start = 1;
		Set<Entry<Prop, Object>> e = dictionary.entrySet();
		for (Entry ee : e) {
			start = start * 31L + (long) (ee.getValue() instanceof PyObject ? System.identityHashCode(ee.getValue()) : ee.hashCode());
		}
		return start;
	}

	public long longHash(Set<String> ex) {
		long start = 1;
		Set<Entry<Prop, Object>> e = dictionary.entrySet();
		for (Entry<Prop, Object> ee : e) {
			if (!ex.contains(ee.getKey().name))
				start = start * 31L + (long) (ee.getValue() instanceof PyObject ? System.identityHashCode(ee.getValue()) : ee.hashCode());
		}
		return start;
	}

	public boolean has(Prop<?> context) {
		return dictionary.containsKey(context);
	}

	public void removeValue(Object c) {
		Set<Entry<Prop, Object>> es = dictionary.entrySet();
		Iterator<Entry<Prop, Object>> is = es.iterator();
		while(is.hasNext())
		{
			Entry<Prop, Object> n = is.next();
			if (n.getValue().equals(c))
				is.remove();
		}
	}


	public Dict mergeAll(Dict d) {
		for (Map.Entry<Prop, Object> e : d.dictionary.entrySet()) {
			if (!dictionary.containsKey(e.getKey()))
				put(e.getKey(), e.getValue());
		}
		return this;
	}

}
