package field.math.abstraction;

import java.lang.reflect.Method;

import field.namespace.generic.ReflectionTools;


public interface iAcceptor<T> {

	static public final Method method_set = ReflectionTools.methodOf("set", iAcceptor.class, Object.class);
	
	public iAcceptor<T> set(T to);
}
