package field.bytecode.protect.dispatch;

import java.util.Map;
import java.util.Stack;


import org.objectweb.asm.Type;

import field.bytecode.protect.StandardTrampoline;

/**
 * support for extensible dispatch over arbitrary iTopology
 */
public class DispatchSupport {

	static ThreadLocal<Stack<Level>> ongoing = new ThreadLocal<Stack<Level>>(){
		@Override
		protected Stack<Level> initialValue() {
			return new Stack<Level>();
		}
	};

	public class Level {
		String name;

		Apply topology;

		int seen;

		public Object[] args;

		public DispatchProvider provider;

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Level)) return false;
			return ((Level) obj).name.equals(name) && ((Level) obj).topology.equals(topology);
		}

		@Override
		public int hashCode() {
			return name.hashCode() + (topology == null ? 0 : topology.hashCode());
		}
	}

	public interface Apply {
		public void head(Object[] args);

		public Object tail(Object[] args, Object returnWas);
	}

	public interface DispatchProvider {
		public Apply getTopologyForEntrance(Object root, Map<String, Object> parameters, Object[] args, String className);

		public Apply getTopologyForExit(Object root, Map<String, Object> parameters, Object[] args, String className);

		public void notifyExecuteBegin(Object fromThis, Map<String, Object> parameterName);

		public void notifyExecuteEnds(Object fromThis, Map<String, Object> parameterName);
	}

	public void enter(String uniq, String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, Object[] argArray, String className) {
		if (StandardTrampoline.debug) if (StandardTrampoline.debug) System.out.println(" ---- enter <" + uniq + ">");
		// is this level already running?
		Stack<Level> stack = ongoing.get();
		String name = (String) parameterName.get("id");
		if (name == null) name = "";
		if (stack.size() == 0 || (!(stack.peek().name.equals(name)) || name.equals(""))) {
			try {
				if (StandardTrampoline.debug)
					if (StandardTrampoline.debug) System.out.println(" paramemters <" + parameterName + ">");
				DispatchProvider c = null;
				if (parameterName.containsKey("topology_cached")) {
					c = (DispatchProvider) parameterName.get("topology_cached");
				} else {
					Type typeToInst = (Type) parameterName.get("topology");
					Class classToInst = fromThis.getClass().getClassLoader().loadClass(typeToInst.getClassName());
					c = (DispatchProvider) ((Class) classToInst).newInstance();
					parameterName.put("topology_cached", c);
				}
				Apply top = c.getTopologyForEntrance(fromThis, parameterName, argArray, className);
				Level l = new Level();
				l.name = name;
				l.topology = top;
				l.seen = 1;
				l.args = argArray;
				l.provider = c;

				stack.push(l);

				if (top != null) {
					top.head(argArray);
				}
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			stack.peek().seen++;
			stack.peek().provider.notifyExecuteBegin(fromThis, parameterName);
		}
		if (StandardTrampoline.debug) if (StandardTrampoline.debug) System.out.println(" stack is now <" + stack + ">");
	}

	public Object exit(String string, Object fromThis, Object returningThis, Map<String, Object> parameterName, String className) {
		if (StandardTrampoline.debug) if (StandardTrampoline.debug) System.out.println(" ---- exit <" + string + "> stack is <" + ongoing.get() + ">");
		// is this level already running?
		Stack<Level> stack = ongoing.get();
		String name = (String) parameterName.get("id");
		if (name == null) name = "";
		if (stack.size() == 0) assert false : "stack size reached zero and we are exiting";
		if (!stack.peek().name.equals(name) && !stack.peek().name.equals(""))
			assert false : "expected name <" + stack.peek().name + "> got <" + name + "> stack is <" + stack + ">";

		Level l = stack.peek();

		Apply ap = l.provider.getTopologyForExit(fromThis, parameterName, l.args, className);
		if (ap != null) {
			l.topology = ap;
			returningThis = ap.tail(l.args, returningThis);
		}

		stack.peek().provider.notifyExecuteEnds(fromThis, parameterName);
		stack.peek().seen--;
		if (stack.peek().seen == 0) stack.pop();

		return returningThis;
	}

}
