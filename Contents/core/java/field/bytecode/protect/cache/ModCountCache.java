package field.bytecode.protect.cache;

import java.util.List;
import java.util.WeakHashMap;

import field.namespace.generic.Bind.iFunction;


/**
 * there are more efficient ways of doing this if the cache rarely changes. In this case, I should push the dirty flags into this structure
 */
public class ModCountCache<T, V> {

	private final iGetModCount<T> modCount;

	public interface iGetModCount<T> {
		public int countFor(T t);
	}

	public class CacheRecord {
		V value;

		int atCount;
	}

	WeakHashMap<T, CacheRecord> cache = new WeakHashMap<T, CacheRecord>();

	private final iFunction<V, T> defaultOr;

	public ModCountCache(iGetModCount<T> modCount) {
		this.modCount = modCount;
		this.defaultOr = null;
	}

	public ModCountCache(iGetModCount<T> modCount, iFunction<V, T> defaultOr) {
		this.modCount = modCount;
		this.defaultOr = defaultOr;
	}

	static public <T extends iGetModCount<T>> iGetModCount<T> askKey(Class<T> f) {
		return new iGetModCount<T>(){
			public int countFor(T t) {
				return t.countFor(t);
			}
		};
	}
	
	static public <T extends iGetModCount<T>> iGetModCount<List<T>> askListKey(Class<T> f) {
		return new iGetModCount<List<T>>(){
			public int countFor(List<T> t) {
				int z = 0;
				for(int n=0;n<t.size();n++)
				{
					T tt = t.get(n);
					z += tt.countFor(tt);
				}
				return z;
			}
		};
	}
	

	public V get(T t, iFunction<V, T> or) {
		CacheRecord r = cache.get(t);
		if (r == null) {
			V v = or.f(t);
			r = new CacheRecord();
			r.value = v;
			r.atCount = modCount.countFor(t);
			cache.put(t, r);
			return v;
		}

		int nmc = modCount.countFor(t);
		if (r.atCount != nmc) {
			r.atCount = nmc;
			r.value = or.f(t);
		}

		return r.value;
	}

	public V get(T t) {
		CacheRecord r = cache.get(t);
		if (r == null) {
			V v = defaultOr.f(t);
			r = new CacheRecord();
			r.value = v;
			r.atCount = modCount.countFor(t);
			cache.put(t, r);
			return v;
		}

		int nmc = modCount.countFor(t);
		if (r.atCount != nmc) {
			r.atCount = nmc;
			r.value = defaultOr.f(t);
		}

		return r.value;
	}

}
