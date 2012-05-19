package field.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class WeakArrayList<T> implements List<T> {

	List<WeakReference<T>> list = new ArrayList<WeakReference<T>>();

	public void add(int index, T element) {
		list.add(index, new WeakReference<T>(element));
	}

	public boolean add(T o) {
		return list.add(new WeakReference<T>(o));
	}

	public boolean addAll(Collection< ? extends T> c) {
		Iterator< ? extends T> i = c.iterator();
		while (i.hasNext())
			add(i.next());
		return c.size() > 0;
	}

	public boolean addAll(int index, Collection c) {
		Iterator< ? extends T> i = c.iterator();
		while (i.hasNext()) {
			add(index, i.next());
			index++;
		}
		return c.size() > 0;
	}

	public void clear() {
		list.clear();
	}

	public boolean contains(Object o) {
		return list.contains(o);
	}

	public boolean containsAll(Collection c) {
		return list.containsAll(c);
	}

	public boolean equals(Object o) {
		return list.equals(o);
	}

	public T get(int index) {
		WeakReference<T> r = list.get(index);
		return r.get();
	}

	public int hashCode() {
		return list.hashCode();
	}

	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public Iterator<T> iterator() {
		final Iterator<WeakReference<T>> ii = list.iterator();
		return new Iterator<T>(){
			public boolean hasNext() {
				return ii.hasNext();
			}

			public T next() {
				return ii.next().get();
			}

			public void remove() {
				ii.remove();
			}
		};
	}

	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	public ListIterator listIterator() {
		return list.listIterator();
	}

	public ListIterator listIterator(int index) {
		return list.listIterator(index);
	}

	public T remove(int index) {
		WeakReference<T> n = list.remove(index);
		if (n==null) return null;
		return n.get();
	}

	public boolean remove(Object o) {
		for(int i=0;i<list.size();i++)
		{
			WeakReference<T> a = list.get(i);
			if (a == o)
				list.remove(i);
			return true;
		}
		return false;
	}

	public boolean removeAll(Collection c) {
		return list.removeAll(c);
	}

	public boolean retainAll(Collection c) {
		return list.retainAll(c);
	}

	public T set(int index, T element) {
		return list.set(index, new WeakReference<T>(element)).get();
	}

	public int size() {
		return list.size();
	}

	public List<T> subList(int fromIndex, int toIndex) {
		WeakArrayList nn = new WeakArrayList<T>();
		nn.list = this.list.subList(fromIndex, toIndex);
		return nn;
	}

	public Object[] toArray() {
		return list.toArray();
	}

	public <T> T[] toArray(T [] a) {
		WeakReference[] r = list.toArray(new WeakReference[0]);
		if (a.length != r.length)
		{
			a = (T[]) new Object[r.length];
		}
		for(int i=0;i<r.length;i++)
		{
			a[i] = (T) r[i].get();
		}
		return a;
	}

	public void clean() {
		Iterator<WeakReference<T>> i = list.iterator();
		while(i.hasNext()) {
			WeakReference<T> nn = i.next();
			if (nn==null || nn.get()==null) i.remove();
		}
	}

}
