package field.math.abstraction;

public interface iTemporalFunction<T> {
	public T get(float alpha);
	public float getDomainMin();
	public float getDomainMax();
}
