package field.bytecode.protect.cache;

import java.util.Arrays;

public class ImmutableArrayWrapper implements iImmutableContainer {

	public Object[] a;

	private int hash;

	private String bigBase;

	private boolean doBigBase;

	public ImmutableArrayWrapper(Object[] a) {
		this.a = a;
		this.hash = Arrays.hashCode(a);
		this.doBigBase = true;
	}
	public ImmutableArrayWrapper(Object[] a, boolean doBig) {
		this.a = a;
		this.hash = Arrays.hashCode(a);
		this.doBigBase = doBig;
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj instanceof ImmutableArrayWrapper)) {
			if (((ImmutableArrayWrapper) obj).hash != hash) return false;
			if (doBigBase)
				if (((ImmutableArrayWrapper) obj).getBigBaseHashCode().equals(getBigBaseHashCode())) return true;
			return Arrays.equals(a, ((ImmutableArrayWrapper) obj).a);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return hash;
	}

	public String getBigBaseHashCode() {
		if (bigBase != null) return bigBase;

		return bigBase = ImmutableArrayList.getBigBaseHashCodeForIterable(this.a);
	}
	
	@Override
	public String toString() {
		return "iaw:"+Arrays.asList(a)+" (hash+"+hashCode()+")";
	}
}
