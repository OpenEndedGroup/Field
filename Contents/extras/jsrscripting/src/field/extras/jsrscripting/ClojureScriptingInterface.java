package field.extras.jsrscripting;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.python.core.Py;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.util.PythonInterpreter;

import clojure.lang.AFunction;
import clojure.lang.Var;
import field.core.execution.PythonInterface;
import field.extras.jsrscripting.clojure.ClojureScriptEngine;

public class ClojureScriptingInterface extends JSRInterface {
	static public final HashSet<String> forbiddenNames = new HashSet<String>(Arrays.asList(new String[] { "flatten", "name", "alias", "Math", "Double", "System", "first", "load", "cons", "_ex", "_clojure", "Runtime", "_now", "trans" }));
	private Object out;
	private Method outMethod;
	private Object err;
	private Method errMethod;

	@Override
	protected ScriptEngine makeEngine() {

		;//;//System.out.println(" making engine ");

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

			PythonInterface.getPythonInterface().execString("from clojure.lang import RT\nfrom clojure.lang import Symbol\n" + "from clojure.lang import Namespace\n" + "from clojure.lang import Var\n" + "class ClojureNamespaceAccess:\n" + "	def __init__(self, nsName):\n" + "		self.__dict__[\"nsName\"]= nsName\n" + "	\n" + "	def __getattr__(self, name):\n" +
			// "		return RT.var(self.nsName, name).get()\n" +
					"		return Namespace.findOrCreate(Symbol.intern(self.nsName)).getMappings()[Symbol.intern(name)].get()\n" +
					//
					"\n" + "	def __setattr__(self, name, val):\n" + "		return RT.var(self.nsName, name).bindRoot(name, val)\n" + "\n" + "_clojure = ClojureNamespaceAccess(\"user\")\n");

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

	@Override
	public Object eval(String eval) {
		;//;//System.out.println(" -- eval in >>>>>>>>>");

		final PrintStream oldOut = System.out;
		final PrintStream oldErr = System.err;

		;//;//System.out.println(" outputstack error peek <" + outputStack.peek() + ">");

		System.setOut(new PrintStream(new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				// oldOut.println(" writing to <"+outputStack.peek().right+">");
				context.getWriter().append((char) b);
				outputStack.peek().left.write(b);
			}

			@Override
			public void flush() throws IOException {
				outputStack.peek().left.flush();
			}
		}));

		System.setErr(new PrintStream(new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				// oldErr.write((char)b);
				context.getErrorWriter().append((char) b);
				outputStack.peek().right.write(b);

			}

			@Override
			public void flush() throws IOException {
				outputStack.peek().right.flush();
				// context.getErrorWriter().flush();
			}

		}));

		try {
			outMethod.invoke(out, outputStack.peek().left);
			errMethod.invoke(err, outputStack.peek().right);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		try {

			ns_enter();

			Object e = super.eval(eval);

			if (e != null)
			{
				;//;//System.out.println(e+" "+e.getClass());
				
				if (e instanceof Var)
				{
					Object name = ((Var)e).getTag();
					e = ((Var)e).get();
					if (e != null)
					{
						if (e instanceof AFunction)
						{
							PythonInterface.getPythonInterface().setVariable("_r", e);
						}
						
					}
				}
			}

			
			
			
			return e;
			// return null;
		} finally {

			ns_exit();
			System.out.flush();
			System.err.flush();
			//
			System.setOut(oldOut);
			System.setErr(oldErr);

			;//;//System.out.println(" -- eval out <<<<<<<<<<<<<<");

		}
	}

	private void ns_exit() {
	}

	private void ns_enter() {

	}

	@Override
	protected boolean filterSetVariable(String name, Object value) {
		// return false;

		if (forbiddenNames.contains(name))
			return false;

		return !forbiddenNames.contains(name);
	}

	@Override
	protected void handleScriptException(ScriptException e) {
		e.printStackTrace();
		super.handleScriptException(e);
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
