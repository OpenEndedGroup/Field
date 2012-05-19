package field.extras.jsrscripting;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map.Entry;

import field.core.util.FieldPyObjectAdaptor.iHandlesAttributes;
import field.math.abstraction.iProvider;

public class GenDictSource {

	private final HashMap<String, Object> map;

	static HashMap<Long, GenDictSource> refMap = new HashMap<Long, GenDictSource>();
	static long ref = 0;

	public GenDictSource(HashMap<String, Object> map) {
		this.map = map;
		ref++;
		refMap.put(ref, this);
	}

	public String getClassSource(String name) {
		String s = "import field.extras.jsrscripting.GenDictSource;\nclass " + name + "{";

		for (Entry<String, Object> e : map.entrySet()) {
			s += "    static public "+classForValue(e.getValue())+" "+e.getKey()+"(){\n" +
					"return ("+classForValue(e.getValue())+")GenDictSource.get("+ref+", "+classForValue(e.getValue())+".class, \""+e.getKey()+"\");}";
		}

		s += "}";

		return s;
	}

	private String classForValue(Object value) {
		if (value == null) return "Object";
		
		Class c = value.getClass();
		while(c.isAnonymousClass() || !Modifier.isPublic(c.getModifiers()))
		{
			c = c.getSuperclass();
		}
		
		return c.getName();
	}

	private String classForKey(Object value) {
		if (value == null)
			return "iProvider<Object>";
		return "field.math.abstraction.iProvider<" + value.getClass().getName() + ">";
	}

	private String valueFor(String key, Object value) {
		return "GenDictSource.provider(" + ref + ", " + value.getClass().getName() + ".class, \"" + key + "\")";
	}

	static public <T> iProvider<T> provider(final long r, Class<T> t, final String name) {
		return new iProvider<T>() {

			public T get() {
				return (T) refMap.get(r).map.get(name);
			}
		};
	}
	
	static public <T> T get(final long r, Class<T> t, final String name)
	{
		return (T) refMap.get(r).map.get(name);
	}

	
	static public class ExtensibleHashMap<K,V> extends HashMap<K, V> implements iHandlesAttributes
	{

		public Object getAttribute(String name) {
			return get(name);
		}

		public void setAttribute(String name, Object value) {
			put((K)name, (V)value);
		}

		
	}
	
	
}
