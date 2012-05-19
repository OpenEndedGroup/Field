package field.extras.jsrscripting;

public class JRubyScriptingInterface_test {

	static public String installed(JSRPlugin inside) {
		try {
			Class<?> c = inside.getClass().getClassLoader().loadClass("org.jruby.embed.jsr223.JRubyEngineFactory");
			if (c != null) {
				return "";
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "need to install jruby-complete.jar";
		}
		return "unspecified error";

	}

}
