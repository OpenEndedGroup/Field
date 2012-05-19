package field.math.abstraction;

public interface iBlendAlgebra<T> {

	public T blendRepresentation_multiply(float by, T input);
	public T blendRepresentation_add(T one, T two, T out);
	public T blendRepresentation_newZero();
	public T blendRepresentation_duplicate(T t);
	
}
