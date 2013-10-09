package field.extras.jsrscripting;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.python.core.Py;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.core.PyObject;
import org.python.core.PyType;

import clojure.lang.AFunction;
import clojure.lang.Var;
import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.execution.PythonInterface;
import field.extras.jsrscripting.clojure.ClojureScriptEngine;

@Woven
public class ClojureScriptingInterface extends JSRInterface {
	static public final HashSet<String> forbiddenNames = new HashSet<String>(Arrays.asList(new String[] { "flatten", "name", "alias", "Math", "Double", "System", "first", "load", "cons", "_ex", "_clojure", "Runtime", "_now", "trans" }));
	private Object out;
	private Method outMethod;
	private Object err;
	private Method errMethod;

	@Override
	protected ScriptEngine makeEngine() {

		System.out.println(" making engine ");

		try {

			Class<?> rt = this.getClass().getClassLoader().loadClass("clojure.lang.RT");

			Object var = rt.getField("USE_CONTEXT_CLASSLOADER").get(null);
			var.getClass().getMethod("bindRoot", Object.class).invoke(var, true);

			out = rt.getDeclaredMethod("var", String.class, String.class).invoke(null, "clojure.core", "*out*");
			outMethod = out.getClass().getMethod("bindRoot", Object.class);

			err = rt.getDeclaredMethod("var", String.class, String.class).invoke(null, "clojure.core", "*err*");
			errMethod = err.getClass().getMethod("bindRoot", Object.class);

			new ClojureDelegate(this);

			// register some adaptors for jython

			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__call__") {
				@Override
				public PyObject __call__(PyObject[] args, String[] kw) {
					Object[] x = new Object[args.length];
					Class[] s = new Class[args.length];
					for (int i = 0; i < x.length; i++) {
						x[i] = args[i].__tojava__(Object.class);
						s[i] = Object.class;
					}

					Object o = Py.tojava(self, Object.class);
					try {
						Method m = o.getClass().getMethod("invoke", s);
						return Py.java2py(m.invoke(o, x));
					} catch (SecurityException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
					return null;
				}
			};
			PyType.fromClass(this.getClass().getClassLoader().loadClass("clojure.lang.IFn")).addMethod(meth);

			// register helper class _clojure

			PythonInterface.getPythonInterface().execString("from clojure.lang import RT\nfrom clojure.lang import Symbol\n" + "from clojure.lang import Namespace\n" + "from clojure.lang import *\n" + "class ClojureNamespaceAccess:\n" + "	def __init__(self, nsName=\"user\", hinted=0):\n" + "		self.__dict__[\"nsName\"]= nsName\n" + "		self.__dict__[\"hinted\"]= hinted\n" + "	\n" + "	def __getattr__(self, name):\n" +
			// "		return RT.var(self.nsName, name).get()\n" +
					"		return Namespace.findOrCreate(Symbol.intern(self.nsName)).getMappings()[Symbol.intern(name)].get()\n" +
					//
					"\n" + "	def __setattr__(self, name, val):\n" + "		v = RT.var(self.nsName, name)\n" + "		v.bindRoot(val)\n" + "		if (self.hinted and val!=None): v.setMeta(v.meta().assoc(Keyword.intern(\"tag\"), type(val)))\n" +

					"\n" + "_clojure = ClojureNamespaceAccess(\"user\")\n" + "_clojure_t = ClojureNamespaceAccess(\"user\", hinted=1)\n");

			lateInitCompletionHook();
			System.out.println(" finished making engine ");

			return new ClojureScriptEngine();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return null;
	}

	boolean ns_bootstrapped = false;

	@NextUpdate
	public void lateInitCompletionHook() {

		// PythonInterface.getPythonInterface().execString("from java.lang import Character\n"
		// + "\n" + "def completions(a):\n" + "\n" +
		// "	for n in range(len(a)-1, -1, -1):\n" +
		// "		if ( Character.isJavaIdentifierPart(a[n]) or a[n]==\"-\"):\n"
		// + "			pass\n" + "		else:\n" + "			a = a[n+1:]\n" +
		// "			break\n" +"\n" +
		// "	stem = [x for x in _clojure.__field__possibleFor.invoke(\"^\"+a)]\n"
		// + "	rr = []\n" + "	for n in stem:\n" +
		// "		dd = _clojure.__field__docstringfor.invoke(n)\n"
		// +"		if (dd):\n" + "			rr += [\"\\n\"+dd+\"\\n\", n]\n" +
		// "		else:\n" + "			rr += [n]\n" + "	return rr\n" + "\n" +
		// "Clojure.completions = completions\n");
		// eval("(use `clojure.repl)\n" + "\n" +
		// "(defn __field__docstringfor [a]\n" +
		// "	(:doc (meta (.get (.getMappings (find-ns 'user)) (symbol a)))))\n"
		// + "\n" + "(defn __field__possibleFor [a]\n" +
		// "	(apropos (re-pattern a)))\n" + "");

		eval("\n" + "(ns field)\n" + "(defmacro _python [variable] `(.getVariable (field.core.execution.PythonInterface/getPythonInterface)  ~(str variable) ) )\n" + "(def ^:dynamic *sticky-ns* (atom nil))\n" + "\n" + "(defn set-sticky-ns \n" + "	[x] (reset! *sticky-ns* x))\n" + "\n" + "(print field/*sticky-ns*)\n" + "", false);

		ns_bootstrapped = true;
	}

	static public String installed(JSRPlugin plugin) {

		try {
			System.err.println(" checking clojure availablility... in classloader <" + plugin.getClass().getClassLoader() + ">");
			Class<?> x = plugin.getClass().getClassLoader().loadClass("field.extras.jsrscripting.clojure.ClojureScriptEngine");
			if (x != null) {
				System.err.println(" clojure is available");
				return "";
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return "need to install clojure.jar (from http://clojure.org/) ";
		}

		return "unspecified error";
	}

	boolean first = true;

	public Object eval(String eval) {
		return eval(eval, true);
	}

	public Object eval(String eval, boolean do_ns_shuffle) {

		if (eval.startsWith("\n"))
			eval = eval.substring(1);

		if (eval.trim().startsWith("print"))
			eval = "(" + eval + ")";

		if (first) {
			first = false;
			((ClojureScriptEngine) engine).pushTopLevelThreadBindings(context);
		}

		try {

			if (do_ns_shuffle && ns_bootstrapped)
				ns_enter();

			context.setErrorWriter(PythonInterface.getPythonInterface().errOut);
			Object e = super.eval(eval);
			System.out.println(" out :" + e);

			if (e != null) {
				System.out.println(e + " " + e.getClass());

				if (e instanceof Var) {

					System.out.println(" tag is :"+((Var) e).sym.getName());
					
					if (("" + ((Var) e).sym.getName()).equals("_r")) {

						e = ((Var) e).get();

						PythonInterface.getPythonInterface().setVariable("_r", e);
					}
				}

			}

			return e;
		}

		finally {

			if (do_ns_shuffle && ns_bootstrapped)
				ns_exit();
			System.out.flush();
			System.err.flush();

		}
	}

	protected void handleScriptException(ScriptException e) {

		System.out.println(" << stack trace begins >>");
		e.printStackTrace();
		System.out.println(" << stack trace ends>>");

		try {
			String message = e.getMessage();
			System.out.println(" ---------- m");
			System.out.println(message);
			System.out.println(" ---------- m");

			newContext.getErrorWriter().append(message + "\n");
			Throwable c = e.getCause();
			if (c != null) {
				StackTraceElement[] st = c.getStackTrace();
				for (int i = 0; i < st.length; i++) {
					if (st[i].getClassName().equals("clojure.lang.Compiler"))
						break;
					newContext.getErrorWriter().append("  " + st[i] + "\n");
				}
			}
			// System.out.println("cause is :"+Arrays.asList(e.getStackTrace()));
			// System.out.println("cause is :"+e.getCause());
			// System.out.println("cause is :"+Arrays.asList(e.getCause().getStackTrace()));

		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// e.printStackTrace(new PrintStream(new OutputStream() {
		// Writer w = newContext.getErrorWriter();
		//
		// @Override
		// public void write(int b) throws IOException {
		// w.append((char) b);
		// }
		// }));
	}

	static public final VisualElementProperty<String> sticky_ns = new VisualElementProperty<String>("sticky_ns");

	private void ns_exit() {
		Object ns = ((ClojureScriptEngine) engine).getStickyNS();
		System.out.println(" EXIT :" + ns + " " + (ns == null ? null : ns.getClass()));

		if (ns instanceof String) {
			Object self = PythonInterface.getPythonInterface().getVariable("_self");
			if (self instanceof iVisualElement) {
				((iVisualElement) self).setProperty(sticky_ns, (String) ns);
			}
		}
	}

	private void ns_enter() {
		System.out.println(" Enter cleared :");
		((ClojureScriptEngine) engine).clearStickyNS();
		boolean set = false;
		Object self = PythonInterface.getPythonInterface().getVariable("_self");
		if (self instanceof iVisualElement) {
			String ns = ((iVisualElement) self).getProperty(sticky_ns);
			if (ns != null) {
				System.out.println(" Enter forced :" + ns);
				((ClojureScriptEngine) engine).setCurrentNS(ns);
				set = true;
			}
		}

		if (!set) {
			((ClojureScriptEngine) engine).setCurrentNS("user");
			System.out.println(" Enter set to default (user)");
		}
	}

	@Override
	protected boolean filterSetVariable(String name, Object value) {
		// return false;

		if (forbiddenNames.contains(name))
			return false;

		return !forbiddenNames.contains(name);
	}

	@Override
	protected String transformName() {
		return "Clojure";
	}

	@Override
	protected String description() {
		return "Execute text as Clojure";
	}
}
