package field.bytecode.protect;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

import field.launch.Launcher;
import field.launch.iUpdateable;

public class DeferedNextUpdate extends BasicInstrumentation2.DeferCallingFast {

	public DeferedNextUpdate(String name, int access, Method onMethod, ClassVisitor classDelegate, MethodVisitor delegateTo, String signature, HashMap<String, Object> parameters) {
		super(name, access, onMethod, classDelegate, delegateTo, signature, parameters);
	}

	java.lang.reflect.Method original = null;

	@Override
	public Object handle(int fromName, final Object fromThis, final String originalMethod, final Object[] argArray) {

		if (original == null) {
			java.lang.reflect.Method[] all = StandardTrampoline.getAllMethods(fromThis.getClass());
			for (java.lang.reflect.Method m : all) {
				if (m.getName().equals(originalMethod)) {
					original = m;
					break;
				}
			}
			assert original != null : originalMethod;
		}
		original.setAccessible(true);

		boolean doSaveAliasing = parameters.get("saveAliasing") == null ? true : ((Boolean) parameters.get("saveAliasing")).booleanValue();
		boolean doSaveContextLocation = parameters.get("saveAliasing") == null ? true : ((Boolean) parameters.get("saveAliasing")).booleanValue();

		
		
		iUpdateable u = new iUpdateable() {

			public void update() {
				try {
										
					original.invoke(fromThis, argArray);
				} catch (IllegalArgumentException e) {
					System.err.println(" inside deferednextupdate +" + fromThis + " " + originalMethod + " " + Arrays.asList(argArray));
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					System.err.println(" inside deferednextupdate +" + fromThis + " " + originalMethod + " " + Arrays.asList(argArray));
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					System.err.println(" inside deferednextupdate +" + fromThis + " " + originalMethod + " " + Arrays.asList(argArray));
					e.printStackTrace();
				} catch (Throwable t) {
					System.err.println(" inside deferednextupdate +" + fromThis + " " + originalMethod + " " + Arrays.asList(argArray));
					t.printStackTrace();
				}
				
				Launcher.getLauncher().deregisterUpdateable(this);
			}
		};
		Launcher.getLauncher().registerUpdateable(u);

		return null;
	}
}
