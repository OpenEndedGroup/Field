package field.util;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class BiMap<K, V> implements Map<K, V>, Serializable{

	private static final long serialVersionUID = 1L;

	LinkedHashMap<K, V> forwards = new LinkedHashMap<K, V>();

	LinkedHashMap<V, K> backwards = new LinkedHashMap<V, K>();

	public BiMap() {
	};

	protected BiMap(LinkedHashMap<V, K> backwards, LinkedHashMap<K, V> forwards) {
		this.backwards = backwards;
		this.forwards = forwards;
	}

	public void clear() {
		forwards.clear();
		backwards.clear();
	}

	public boolean containsKey(Object arg0) {
		return forwards.containsKey(arg0);
	}

	public boolean containsValue(Object arg0) {
		return backwards.containsKey(arg0);
	}

	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return forwards.entrySet();
	}

	public V get(Object arg0) {
		return forwards.get(arg0);
	}

	public V getForwards(K arg0) {
		return forwards.get(arg0);
	}

	public K getBackwards(V arg0) {
		return backwards.get(arg0);
	}

	public boolean isEmpty() {
		return forwards.isEmpty();
	}

	public Set<K> keySet() {
		return forwards.keySet();
	}

	public V put(K arg0, V arg1) {
		V r = forwards.put(arg0, arg1);
		backwards.remove(r);
		backwards.put(arg1, arg0);
		return r;
	}

	public void putAll(Map< ? extends K, ? extends V> arg0) {
		for (Map.Entry e : arg0.entrySet()) {
			this.put((K) e.getKey(), (V) e.getValue());
		}
	}

	public V remove(Object arg0) {
		V v = forwards.remove(arg0);
		backwards.remove(v);
		return v;
	}

	public int size() {
		assert forwards.size() == backwards.size();
		return forwards.size();
	}

	public Set<V> values() {
		return backwards.keySet();
	}

	public K removeBackwards(V key) {
		K k = backwards.remove(key);
		forwards.remove(k);
		return k;
	}

	public BiMap<V, K> getBackwardsMap() {
		BiMap<V, K> r = new BiMap<V, K>(forwards, backwards);
		return r;
	}

	@Override
	public String toString() {
		return "bimap" + forwards;
	}

}
