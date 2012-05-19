package field.extras.jsrscripting;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import field.core.ui.UbiquitousLinks;

public class JavaScriptScriptingInterface extends JSRInterface {

	@Override
	protected ScriptEngine makeEngine() {
		try {
			Class<?> c;
			c = this.getClass().getClassLoader().loadClass("com.sun.phobos.script.javascript.RhinoScriptEngineFactory");
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
			System.err.println(" checking javascript availablility...");
			Class<?> x = plugin.getClass().getClassLoader().loadClass("com.sun.phobos.script.javascript.RhinoScriptEngineFactory");
			if (x != null) {
				System.err.println(" javascript is available");
				return "";
			}
		} catch (ClassNotFoundException e)
		{
			return "need to install js.jar (from "+UbiquitousLinks.links.simpleLink("http://www.mozilla.org/rhino/")+") and js-engine.jar (from "+UbiquitousLinks.links.simpleLink("http://scripting.dev.java.net")+") into classpath using the Plugin Manager";
		}
		System.out.println(" no javascript?");
		return "unspecified error";
	}

	@Override
	protected String transformName() {
		return "JavaScript";
	}
	
	@Override
	protected String description() {
		return "Execute text as JavaScript";
	}
}
