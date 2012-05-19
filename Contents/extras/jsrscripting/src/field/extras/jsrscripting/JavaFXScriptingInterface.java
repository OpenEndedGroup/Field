package field.extras.jsrscripting;

import java.io.Writer;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.python.core.PyObject;

import field.core.ui.UbiquitousLinks;

public class JavaFXScriptingInterface extends JSRInterface {

	@Override
	protected ScriptEngine makeEngine() {
		
		try {
			Class<?> c;
			c = this.getClass().getClassLoader().loadClass("com.sun.tools.javafx.script.JavaFXScriptEngineFactory");
			ScriptEngineFactory fact = (ScriptEngineFactory) c.newInstance();
			
			ScriptEngine engine = fact.getScriptEngine();
			
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
	}

	@Override
	protected String preamble(String eval) {
		fixOutput();
		return "";
	}

	@Override
	protected void postBind(Object evaluation, String eval) {
		super.postBind(evaluation, eval);

//		if (evaluation instanceof Class) {
//			System.out.println(" name is <" + ((Class) evaluation).getSimpleName() + ">");
//			propagateAttribute(((Class) evaluation).getSimpleName(), evaluation);
//		}
	}

	@Override
	public void setVariable(String name, Object value) {
		super.setVariable(name, value);

		if (value instanceof PyObject)
			value = ((PyObject) value).__tojava__(Object.class);

//		Field nn = knownGlobals.get(name);
//		if (nn != null)
//			try {
//				nn.set(null, value);
//			} catch (IllegalArgumentException e) {
//				e.printStackTrace();
//			} catch (IllegalAccessException e) {
//				e.printStackTrace();
//			}
	}

	static public String installed(JSRPlugin plugin) {

		try {
			System.err.println(" checking javafx as script engine availablility...");
			Class<?> x = plugin.getClass().getClassLoader().loadClass("com.sun.tools.javafx.script.JavaFXScriptEngineFactory");
			if (x != null) {
				System.err.println(" javafx as script is available");
				return "";
			}
		} catch (ClassNotFoundException e) {
			System.err.println(" javafx is not available ");
			return "please install a JavaFX1.0  SDK (from "+UbiquitousLinks.links.simpleLink("http://javafx.com")+") and then add '/System/Library/Frameworks/JavaFX.framework/Versions/1.0/lib' to Field's classpath using the Plugin Manager";
		}
		System.out.println(" no javafx");
		return "unspecified error";
	}

	@Override
	protected String transformName() {
		return "JavaFX";
	}
	
	@Override
	protected String description() {
		return "Compile (and execute) text as JavaFX code"; 
	}

}
