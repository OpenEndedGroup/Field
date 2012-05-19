package field.math.abstraction;

public interface iBlendable<T> {

	public T cerp(T before, float beforeTime, T now, float nowTime, T  next, float nextTime, T after, float afterTime, float a);
	public T lerp(T before, T now, float a);
	
	public T setValue(T to);
	
	public T blendRepresentation_newZero();
}
