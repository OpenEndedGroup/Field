package field.bytecode.protect;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;


public class DeferedNewThread extends BasicInstrumentation2.DeferCallingFast {

	private int priority=0;

	java.lang.reflect.Method original = null;
	public DeferedNewThread(String name, int access, Method onMethod, ClassVisitor classDelegate, MethodVisitor delegateTo, String signature, HashMap<String, Object> parameters) {
		super(name, access, onMethod, classDelegate, delegateTo, signature, parameters);

		Integer p = (Integer)parameters.get("priority");
		if (p==null)
			priority=0;
		else priority=p;
	}

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
			assert original!=null : originalMethod;
		}
		original.setAccessible(true);

		Thread t = new Thread(new Runnable() {

			public void run() {
				try {
					original.invoke(fromThis, argArray);
				} catch (IllegalArgumentException e) {
					System.err.println(" inside deferednewthread +"+fromThis+" "+originalMethod+" "+Arrays.asList(argArray));
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					System.err.println(" inside deferednewthread +"+fromThis+" "+originalMethod+" "+Arrays.asList(argArray));
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					System.err.println(" inside deferednewthread +"+fromThis+" "+originalMethod+" "+Arrays.asList(argArray));
					e.printStackTrace();
				} catch (Throwable t)
				{
					System.err.println(" inside deferednewthread +"+fromThis+" "+originalMethod+" "+Arrays.asList(argArray));
					t.printStackTrace();
				}
			}

		});

		t.setPriority(priority == 0 ? Thread.NORM_PRIORITY : (priority<0 ? Thread.MIN_PRIORITY : Thread.MAX_PRIORITY));

		t.start();

		return null;
	}

}
