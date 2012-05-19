package field.extras.jsrscripting;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import field.bytecode.protect.Trampoline2;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.execution.PythonInterface;
import field.core.ui.UbiquitousLinks;
import field.core.ui.text.embedded.MinimalTextField_blockMenu;
import field.launch.iLaunchable;
import field.namespace.generic.Generics.Pair;


/**
 * we should be able to generate the class that compiles to a class that contains the contents of a dict and can be statically imported;
 * 
 * 
 */
public class JavaCScriptingInterface extends JSRInterface {

	static HashMap<String, Field> knownGlobals = new LinkedHashMap<String, Field>();

	static public class Globals {
		public static iLaunchable T;
		public static iVisualElement _self;
		public static PrintStream out;
		public static PrintStream err;
	}


	static public final VisualElementProperty<GenDictSource.ExtensibleHashMap> java = new VisualElementProperty<GenDictSource.ExtensibleHashMap>("java");
	static public final GenDictSource.ExtensibleHashMap bound = new GenDictSource.ExtensibleHashMap();
	
	static {
		Field[] f = Globals.class.getFields();
		for (Field ff : f) {
			knownGlobals.put(ff.getName(), ff);
		}
	}

	@Override
	protected ScriptEngine makeEngine() {

		try {
			Class<?> c;
			c = this.getClass().getClassLoader().loadClass("com.sun.script.java.JavaScriptEngineFactory");
			ScriptEngineFactory fact = (ScriptEngineFactory) c.newInstance();

			ScriptEngine engine = fact.getScriptEngine();

			engine.getContext().setAttribute("com.sun.script.java.parentLoader", Trampoline2.trampoline.getClassLoader(), ScriptContext.ENGINE_SCOPE);
			engine.getContext().setAttribute("parentLoader", Trampoline2.trampoline.getClassLoader(), ScriptContext.ENGINE_SCOPE);
			return engine;

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public JavaCScriptingInterface() {
		super();
		
		String method = "def JavaC_method(name):\n" + 
		"	def applyMethod(context, definition, glob):\n" + 
		"		d2 = \"class __tempDef{ public void q() {\" + definition+ \"}}\";\n" + 
		"		JavaC(context, d2, glob)\n" + 
		"		global __tempDef\n" + 
		"		globals()[name] = __tempDef().q\n" + 
		"	return applyMethod\n" + 
		"";
		
		PythonInterface.getPythonInterface().execString(method);

		
		MinimalTextField_blockMenu.knownTextTransforms.add(new Pair<String, String>(transformName()+"_method", "JavaC_method(\"somemethod\") declares a parameterless method called \"somemethod\" that's defined in Java"));

	}

	@Override
	public void pushOutput(final Writer output, final Writer error) {
		super.pushOutput(output, error);
		fixOutput();
	}

	@Override
	public void popOutput() {
		super.popOutput();
		fixOutput();
	}

	protected void fixOutput() {
		Globals.out = new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				newContext.getWriter().append((char) b);
			}
		});
		Globals.err = new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				newContext.getErrorWriter().append((char) b);
			}
		});
	}

	@Override
	protected String preamble(String eval) {
		fixOutput();
		String s1 = "import static field.extras.jsrscripting.JavaCScriptingInterface.Globals.*;\n";
		String importLines = "";
		for(String s : eval.split("\n"))
		{
			if (s.trim().startsWith("import")) 
				importLines+= s+"\n";
		}
		String s2 = new GenDictSource(bound).getClassSource("Bound");

		return s1 + importLines+s2 + "\n";
	}
	
	@Override
	protected String body(String eval) {
		
		Pattern c = Pattern.compile("[.]*?(interface|class)[\\s]+([a-zA-Z$_][a-zA-Z0-9$_.]*)");
		Matcher m = c.matcher(eval);
		boolean found = m.find();
		if (!found)
			throw new IllegalArgumentException("JavaC body must contain an interface or class definition");
		
		String name = m.group(2)+".java";
		
		engine.put(ScriptEngine.FILENAME, name);
		
		String x = "";
		for(String s : eval.split("\n"))
		{
			if (!s.trim().startsWith("import"))
			{
				x+=s+"\n";
			}
		}
		return x;
	}
	
	@Override
	protected void postBind(Object evaluation, String eval) {
		super.postBind(evaluation, eval);

		if (evaluation instanceof Class) {
			// System.out.println(" name is <" + ((Class)
			// evaluation).getSimpleName() + ">");
			propagateAttribute(((Class) evaluation).getSimpleName(), evaluation);
		}
	}
	

	@Override
	public void setVariable(String name, Object value) {
		super.setVariable(name, value);

		if (value instanceof PyObject)
			value = ((PyObject) value).__tojava__(Object.class);

		Field nn = knownGlobals.get(name);
		if (nn != null)
			try {
				nn.set(null, value);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
	}

	static public String installed(JSRPlugin plugin) {

		try {
			System.err.println(" checking javac as script engine availablility...");
			Class<?> x = plugin.getClass().getClassLoader().loadClass("com.sun.script.java.JavaScriptEngineFactory");
			if (x != null) {
				System.err.println(" javac as script is available");
				return "";
			}
		} catch (ClassNotFoundException e) {
			return "need to install java-engine.jar (from " + UbiquitousLinks.links.simpleLink("http://scripting.dev.java.net") + ") into classpath using the Plugin Manager";
		}
		System.out.println(" no javac");
		return "unspecified error";
	}

	
	@Override
	protected String transformName() {
		return "JavaC";
	}

	@Override
	protected String description() {
		return "Compile (and execute) text as pure-Java code";
	}

	@Override
	public void setRoot(iVisualElement root) {
		super.setRoot(root);
		root.setProperty(java, bound);
		PythonInterface.getPythonInterface().setVariable("_java", bound);
	}
	
}
