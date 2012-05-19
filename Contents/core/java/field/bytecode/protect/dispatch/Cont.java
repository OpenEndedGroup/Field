package field.bytecode.protect.dispatch;

import java.lang.reflect.Method;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import field.bytecode.protect.dispatch.DispatchSupport.Apply;
import field.launch.iUpdateable;
import field.namespace.generic.ReflectionTools;


public class Cont implements DispatchSupport.DispatchProvider {

	static public class aRun implements Run
	{

		public ReturnCode head(Object calledOn, Object[] args) {
			return ReturnCode.cont;
		}

		public ReturnCode tail(Object calledOn, Object[] args, Object returnWas) {
			return ReturnCode.cont;
		}
	}

	public enum ReturnCode {
		cont, stop;
	}

	public interface Run {
		public ReturnCode head(Object calledOn, Object[] args);

		public ReturnCode tail(Object calledOn, Object[] args, Object returnWas);
	}

	static HashMap<Method, List<Run>> class_links = new HashMap<Method, List<Run>>();

	static WeakHashMap<Object, HashMap<Method, List<Run>>> instance_links = new WeakHashMap<Object, HashMap<Method, List<Run>>>();

	static public void linkWith(Object o, Method m, Run r) {

		Method om = m;
		// Method m might refer to a superclass, so we have to do a little work here
		m = ReflectionTools.findMethodWithParametersUpwards(m.getName(), m.getParameterTypes(), o.getClass());
		assert m!=null : "couldn't really find <"+om+"> <"+m.getDeclaringClass()+"> on <"+o+"> <"+o.getClass()+">";
		HashMap<Method, List<Run>> name = instance_links.get(o);
		if (name != null) {
			List<Run> n = name.get(m);
			if (n != null) {
				n.add(r);
			} else {
				name.put(m, n = new ArrayList<Run>());
				n.add(r);
			}
		} else {
			List<Run> n = new ArrayList<Run>();
			n.add(r);
			HashMap<Method, List<Run>> me = new HashMap<Method, List<Run>>();
			me.put(m, n);
			instance_links.put(o, me);
		}

	}

	static public void linkWith_static(Method m, Class c, Run r) {

		Method om = m;
		// Method m might refer to a superclass, so we have to do a little work here
		m = ReflectionTools.findMethodWithParametersUpwards(m.getName(), m.getParameterTypes(), c);
		assert m!=null : "couldn't really find <"+om+"> <"+m.getDeclaringClass()+"> on <"+c+">";


		List<Run> name = class_links.get(m);
		if (name != null)
			name.add(r);
		else {
			ArrayList<Run> al = new ArrayList<Run>();
			al.add(r);
			class_links.put(m, al);
		}
	}

	static public void linkWith_static(Method m, Run r) {
		List<Run> name = class_links.get(m);
		if (name != null)
			name.add(r);
		else {
			ArrayList<Run> al = new ArrayList<Run>();
			al.add(r);
			class_links.put(m, al);
		}
	}

	static public void unlinkWith(Object o, Method m, Run r) {
		Method om = m;
		// Method m might refer to a superclass, so we have to do a little work here
		m = ReflectionTools.findMethodWithParametersUpwards(m.getName(), m.getParameterTypes(), o.getClass());
		assert m!=null : "couldn't really find <"+om+"> <"+m.getDeclaringClass()+"> on <"+o+"> <"+o.getClass()+">";

		HashMap<Method, List<Run>> name = instance_links.get(o);
		if (name != null) {
			List<Run> n = name.get(m);
			if (n != null) {
				n.remove(r);
				if (n.size() == 0) {
					name.remove(m);
					if (name.size() == 0) instance_links.remove(name);
				}
			} else {
			}
		} else {
		}
	}


	static public void unlinkWith_static(Method m, Class c, Run r) {

		Method om = m;
		// Method m might refer to a superclass, so we have to do a little work here
		m = ReflectionTools.findMethodWithParametersUpwards(m.getName(), m.getParameterTypes(), c);
		assert m!=null : "couldn't really find <"+om+"> <"+m.getDeclaringClass()+"> on <"+c+">";

		List<Run> name = class_links.get(m);
		if (name != null)
			name.remove(r);
		else {
		}
	}


	static public void unlinkWith_static(Method m, Run r) {
		List<Run> name = class_links.get(m);
		if (name != null)
			name.remove(r);
		else {
		}
	}

	public MethodUtilities utilities = new MethodUtilities();

	HashSet<Object> alreadyDone = new HashSet<Object>();

	String uid = new UID().toString();

	public Apply getTopologyForEntrance(final Object root, Map<String, Object> parameters, Object[] args, String className) {
		if (!alreadyDone.contains(root)) {

			Method m = (Method) parameters.get(uid);
			if (m==null)
			{
				m = utilities.getMethodFor(root.getClass().getClassLoader(),(org.objectweb.asm.commons.Method) parameters.get("method"),null, className);
				parameters.put(uid, m );
			}

			final List<Run> r1 = class_links.get(m);
			HashMap<Method, List<Run>> mm = instance_links.get(root);
			List<Run> rr2 = null;
			if (mm != null) rr2 = mm.get(m);
			final List<Run> r2 = rr2;

			if (r1 != null || r2 != null) {
				alreadyDone.add(root);
				return new Apply(){

					public void head(Object[] args) {
						if (r1 != null) for (Run run : new ArrayList<Run>(r1)) {
							ReturnCode c = run.head(root, args);
							if (c == ReturnCode.stop) return;
						}
						if (r2 != null) for (Run run : new ArrayList<Run>(r2)) {
							ReturnCode c = run.head(root, args);
							if (c == ReturnCode.stop) return;
						}
					}

					public Object tail(Object[] args, Object returnWas) {
						return returnWas;
					}
				};
			}
		}
		return null;
	}

	public Apply getTopologyForExit(final Object root, Map<String, Object> parameters, Object[] args, String className) {
		if (alreadyDone.contains(root)) {

			// experimental
			Method m = (Method) parameters.get(uid);
			if (m==null)
			{
				m = utilities.getMethodFor(root.getClass().getClassLoader(),(org.objectweb.asm.commons.Method) parameters.get("method"),null, className);
				parameters.put(uid, m );
			}

			final List<Run> r1 = class_links.get(m);

			HashMap<Method, List<Run>> mm = instance_links.get(root);
			List<Run> rr2 = null;
			if (mm != null) rr2 = mm.get(m);
			final List<Run> r2 = rr2;

			if (r1 != null || r2 != null) return new Apply(){

				public void head(Object[] args) {
				}

				public Object tail(Object[] args, Object returnWas) {
					if (r1 != null) for (Run run : new ArrayList<Run>(r1)) {
						ReturnCode c = run.tail(root, args, returnWas);
						if (c == ReturnCode.stop) return returnWas;
					}
					if (r2 != null) for (Run run : new ArrayList<Run>(r2)) {
						ReturnCode c = run.tail(root, args, returnWas);
						if (c == ReturnCode.stop) return returnWas;
					}
					return returnWas;
				}
			};
		}
		return null;
	}

	public void notifyExecuteBegin(Object fromThis, Map<String, Object> parameterName) {
	}

	public void notifyExecuteEnds(Object fromThis, Map<String, Object> parameterName) {
		alreadyDone.remove(fromThis);
	}

	public static void wrap(Object inside, Method on, final iUpdateable enter, final iUpdateable exit) {
		Cont.linkWith(inside, on, new Run(){

			public ReturnCode head(Object calledOn, Object[] args) {
				enter.update();
				return ReturnCode.cont;
			}

			public ReturnCode tail(Object calledOn, Object[] args, Object returnWas) {
				exit.update();
				return ReturnCode.cont;
			}});


	}

}
