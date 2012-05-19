package field.bytecode.protect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;


public class Protected {

	public interface iShim
	{
		public Object run();
	}

	public Object loadSerialized(String filename) {
		try {
			Class< ? > c = StandardTrampoline.trampoline.loader.loadClass("field.bytecode.NonProtected");

			Method[] m = StandardTrampoline.getAllMethods(c);
			for (Method method : m) {
				if (method.getName().equals("loadSerialized"))
				{
					Object inst = c.newInstance();
					return method.invoke(inst, new Object[]{filename});
				}
			}
			assert false : "no method called run in "+Arrays.asList(m);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
		assert false;
		return null;
	}

	// doesn't work
	public Object run(iShim r)
	{
		try {
			Class< ? > c = StandardTrampoline.trampoline.loader.loadClass("field.bytecode.NonProtected");

			Method[] m = StandardTrampoline.getAllMethods(c);
			for (Method method : m) {
				if (method.getName().equals("run"))
				{
					Object inst = c.newInstance();
					return method.invoke(inst, new Object[]{r});
				}
			}
			assert false : "no method called run in "+Arrays.asList(m);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
		assert false;
		return null;
	}
}
