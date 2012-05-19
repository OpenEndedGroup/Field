package field.launch;

import java.lang.reflect.Method;

import field.namespace.generic.ReflectionTools;

public interface iUpdateable {

	static public final Method method_update = ReflectionTools.methodOf("update", iUpdateable.class);

	public void update();
}