package field.math.util;



public interface iHistogram<T> {
	public float visit(T t, float amount);

	public boolean has(T t);

	public float get(T t, float def);

	public <X> X average(iAverage<? super T, X> average);
}