package field.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author marc
 * created on Jan 9, 2004
 */
public class LinkedHashMapOfLists<K,V> extends LinkedHashMap<K, Collection<V>> {

	public void addToList(K key, V value)
	{
		Collection<V> l = this.get(key);
		if (l==null)
		{
			this.put(key, l = newList());
		}
		l.add(value);
	}
	
	public Collection<V> getCollection(K key)
	{
		Collection l = this.get(key);
		return l;
	}
	
	public List<V> getList(Object key)
	{
		List<V> l = (List<V>)this.get(key);
		return l;
	}
	
	protected Collection<V> newList()
	{
		return new ArrayList<V>();
	}

	public void remove(K key, V value) {
		Collection<V> c = getCollection(key);
		if (c!=null)
		{
			c.remove(value);
		}
	}

	public void removeFromAllLists(V with) {
		Iterator i = this.values().iterator();
		while(i.hasNext())
		{
			((Collection)i.next()).remove(with);
		}
		
		i = this.entrySet().iterator();
		while(i.hasNext())
		{
			Map.Entry e = (Entry) i.next();
			if (((Collection)e.getValue()).size()==0) i.remove();
		}
	}
	
	static public class LinkedHashMapOfSets<K, V> extends LinkedHashMapOfLists<K, V>
	{
		@Override
		protected Collection<V> newList() {
			return new LinkedHashSet();
		}
	}
	
}
