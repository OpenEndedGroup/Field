package field.math.abstraction;

/**
 * Similar to iObjectProvider, but inplace.
 * @see iObjectProvider
 * @author synchar
 *
 */
public interface iInplaceProvider<T>
{
	public T get(T o);
}
