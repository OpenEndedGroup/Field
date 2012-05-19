package field.core.util;

import java.util.ArrayList;
import java.util.List;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.launch.iUpdateable;
import field.math.abstraction.iAcceptor;

@Woven
public class LocalFuture<T> {
	boolean set = false;

	T t;
	List<iAcceptor<T>> continuation = new ArrayList<iAcceptor<T>>();

	public void addContinuation(final iUpdateable u) {
		continuation.add(new iAcceptor<T>() {

			public iAcceptor<T> set(T to) {
				u.update();
				return this;
			}
		});
	}
	
	public void addContinuation(final iAcceptor<T> a)
	{
		continuation.add(a);
	}

	public T get() {
		return t;
	}

	public boolean has() {
		return set;
	}

	@NextUpdate
	public void set(T t) {
		this.t = t;
		for(iAcceptor<T> u : continuation)
			u.set(t);
	}
}