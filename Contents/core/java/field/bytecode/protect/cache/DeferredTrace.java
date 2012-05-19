/**
 *
 */
package field.bytecode.protect.cache;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

import field.bytecode.protect.BasicInstrumentation2;
import field.bytecode.protect.StandardTrampoline;

public final class DeferredTrace extends BasicInstrumentation2.DeferCallingFast {

	private final HashMap<String, Object> parameters;
	java.lang.reflect.Method original = null;

	public DeferredTrace(String name, int access, Method method, ClassVisitor delegate, MethodVisitor to, String signature, HashMap<String, Object> parameters) {
		super(name, access, method, delegate, to, signature, parameters);
		this.parameters = parameters;
	}

	@Override
	public Object handle(int fromName, Object fromThis, String originalMethod, Object[] argArray) {
		if (original == null) {
			java.lang.reflect.Method[] all = StandardTrampoline.getAllMethods(fromThis.getClass());
			for (java.lang.reflect.Method m : all) {
				if (m.getName().equals(originalMethod)) {
					original = m;
					break;
				}
			}
			original.setAccessible(true);
			assert original != null : originalMethod;
		}
		Object object;
		boolean success = false;
		try{
		
			System.out.println(">> "+originalMethod+" ["+Arrays.asList(argArray)+"] "+System.identityHashCode(fromThis));
			object = original.invoke(fromThis, argArray);
			System.out.println("<< "+originalMethod+" ("+object+") "+System.identityHashCode(fromThis));
			success = true;
			return object;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		finally
		{
			if (!success)
				System.out.println("<< (exception) "+originalMethod+" "+System.identityHashCode(fromThis));
		}
		return null;
	}
}