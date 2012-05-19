package field.bytecode.protect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import field.bytecode.protect.BasicInstrumentation2.CallOnEntryFast;
import field.bytecode.protect.dispatch.Cont;
import field.bytecode.protect.dispatch.Cont.ReturnCode;

/**
 * there is a new FastEntry for each method of each class, it doesn't support multiple entry (calls to super, for example), but it sure is fast
 *
 * @author marc
 *
 */
final public class FastEntry extends CallOnEntryFast {
	public static Map<String, Map<String, FastEntry>> knownEntries = new HashMap<String, Map<String, FastEntry>>();

	static public void linkWith(java.lang.reflect.Method method, Class clazz, Cont.Run run) {
		String methodDescriptor = Type.getMethodDescriptor(method);
		String methodName = method.getName();

		Map<String, FastEntry> map = knownEntries.get(clazz.getName());
		assert map != null;
		FastEntry entry = map.get(methodName + ":" + methodDescriptor);
		assert entry != null;

		if (entry.execute == null) entry.execute = new ArrayList<Cont.Run>();
		entry.execute.add(run);
	}

	static public void unlinkWith(java.lang.reflect.Method method, Class clazz, Cont.Run run) {
		String methodDescriptor = Type.getMethodDescriptor(method);
		String methodName = method.getName();

		Map<String, FastEntry> map = knownEntries.get(clazz.getName());
		assert map != null;
		FastEntry entry = map.get(methodName + ":" + methodDescriptor);
		assert entry != null;

		if (entry.execute != null) entry.execute.remove(run);
		if (entry.execute.size() == 0) entry.execute = null;
	}

	List<Cont.Run> execute;

	public FastEntry(String name, int access, Method onMethod, MethodVisitor delegateTo, HashMap<String, Object> parameters, String className) {
		super(name, access, onMethod, delegateTo, parameters);

		Map<String, FastEntry> methodMap = knownEntries.get(className);
		if (methodMap == null) knownEntries.put(className, methodMap = new HashMap<String, FastEntry>());

		methodMap.put(onMethod.getName() + ":" + onMethod.getDescriptor(), this);
	}

	@Override
	public void handle(int fromName, Object fromThis, Object[] argArray) {
		if (execute == null) return;

		for (int i = 0; i < execute.size(); i++) {
			ReturnCode code = execute.get(i).head(fromThis, argArray);
			// if (code == ReturnCode.stop) return;
		}
	}
}
