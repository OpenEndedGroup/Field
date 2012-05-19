package field.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;


/**
 * @author marc created on Jan 9, 2004
 */
public class HashMapOfLists<K, V> extends HashMap<K, Collection<V>> {

	public HashMapOfLists() {
	}

	public HashMapOfLists(HashMapOfLists<K, V> map) {
		super(map);
	}

	public void addAllToList(K key, Collection<V> value) {
		Collection<V> l = this.get(key);
		if (l == null) {
			this.put(key, l = newList());
		}
		l.addAll(value);
	}

	public void addToList(K key, V value) {
		Collection<V> l = this.get(key);
		if (l == null) {
			this.put(key, l = newList());
		}
		l.add(value);
	}

	public void addToListHead(K key, V value) {
		Collection<V> l = this.get(key);
		if (l == null) {
			this.put(key, l = newList());
		}
		((List<V>)l).add(0, value);
	}

	public Collection<V> getAndMakeCollection(K key) {
		Collection l = this.get(key);
		if (l == null)
			this.put(key, l = newList());
		return l;
	}

	public Collection<V> getCollection(K key) {
		Collection l = this.get(key);
		return l;
	}

	public List<V> getList(Object key) {
		List<V> l = (List<V>) this.get(key);
		return l;
	}

	public HashMapOfLists<V, K> invert() {
		HashMapOfLists<V, K> invertedMap = new HashMapOfLists<V, K>();

		Set<Entry<K, Collection<V>>> e = this.entrySet();
		for (Entry<K, Collection<V>> n : e) {
			for (V v : n.getValue()) {
				invertedMap.addToList(v, n.getKey());
			}
		}

		return invertedMap;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void putAll(Map<? extends K, ? extends Collection<V>> a) {
		Iterator<?> es = a.entrySet().iterator();
		while (es.hasNext()) {
			Entry entry = (Entry) es.next();
			K k = (K) entry.getKey();
			Collection<V> v = (Collection<V>) entry.getValue();
			for (V vv : v) {
				addToList(k, vv);
			}
		}
	}

	public void remove(K key, V value) {
		Collection<V> c = getCollection(key);
		if (c != null) {
			c.remove(value);
			if (c.size() == 0)
				this.remove(key);
		}
	}

	public void removeFromAllLists(V with) {
		Iterator i = this.values().iterator();
		while (i.hasNext()) {
			((Collection) i.next()).remove(with);
		}

		i = this.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry e = (Entry) i.next();
			if (((Collection) e.getValue()).size() == 0)
				i.remove();
		}
	}

	protected Collection<V> newList() {
		return new ArrayList<V>();
	}

}
