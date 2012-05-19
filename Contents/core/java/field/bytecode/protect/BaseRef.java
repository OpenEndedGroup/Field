package field.bytecode.protect;


public class BaseRef<T> {
	public T to;

	protected boolean unset = true;

	public BaseRef(T to) {
		this.to = to;
	}

	public T get() {
		return to;
	}

	public boolean isUnset() {
		return unset;
	}

	public BaseRef<T> set(T to) {
		this.to = to;
		unset = false;
		return this;
	}


	@Override
	public String toString() {
		return "REF:" + to;
	}

}
