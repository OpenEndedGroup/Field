package field.extras.jsrscripting;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import field.core.ui.UbiquitousLinks;

public class GroovyScriptingInterface extends JSRInterface {

	@Override
	protected ScriptEngine makeEngine() {
		try {
			Class<?> c;
			c = this.getClass().getClassLoader().loadClass("com.sun.script.groovy.GroovyScriptEngineFactory");
			return ((ScriptEngineFactory) c.newInstance()).getScriptEngine();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	static public String installed(JSRPlugin plugin) {

		try {
			System.err.println(" checking Groovy availablility...");
			Class<?> x = plugin.getClass().getClassLoader().loadClass("com.sun.script.groovy.GroovyScriptEngineFactory");
			if (x != null) {
				System.err.println(" Groovy is available");
				return "";
			}
		} catch (ClassNotFoundException e)
		{
			return "need to install groovy-all-1.5.7.jar (from "+UbiquitousLinks.links.simpleLink("http://groovy.codehaus.org/")+") and groovy-engine.jar (from "+UbiquitousLinks.links.simpleLink("http://scripting.dev.java.net")+") into classpath using the Plugin Manager";
		}
		return "unspecified error";
	}

	@Override
	protected String transformName() {
		return "Groovy";
	}
	
	@Override
	protected String description() {
		return "Execute text as Groovy";
	}
}
