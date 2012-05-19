package field.extras.jsrscripting;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.embed.EmbedRubyObjectAdapter;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;

import field.bytecode.protect.Trampoline2;
import field.core.dispatch.iVisualElement;
import field.core.execution.PythonInterface;
import field.core.execution.ScriptingInterface;
import field.core.ui.text.embedded.MinimalTextField_blockMenu;
import field.namespace.generic.Generics.Pair;

public class JRubyScriptingInterface implements ScriptingInterface {

	private ScriptingContainer container;

	public JRubyScriptingInterface() {

		container = new ScriptingContainer(LocalVariableBehavior.PERSISTENT);
		container.setClassLoader(Trampoline2.trampoline.getClassLoader());
//		container.setCompileMode(CompileMode.FORCE);
		
		adaptor = container.newObjectAdapter();

		PythonInterface.getPythonInterface().execString("\n" + "class JRubyNamespaceAccess:\n" + "	def __getattr__(self, name):\n" + "		return JSRPlugin.getInterface(\"JRuby\").getVariable(name)\n" + "	def __setattr__(self, name, value):\n" + "		JSRPlugin.getInterface(\"JRuby\").setVariable_(name, value, 100)\n" + "_ruby=JRubyNamespaceAccess()");

		PythonInterface.getPythonInterface().execString("from org.jruby import RubyObject\n" + "def __call__(self, *x):\n" + "	JSRPlugin.getInterface(\"JRuby\").container.callMethod(self, \"call\", *x)\n" + "\n" + "RubyObject.__call__ = __call__\n");

		new JRubyDelegate(this);

		PythonInterface.getPythonInterface().execString("from field.extras.jsrscripting import JSRPlugin");
		PythonInterface.getPythonInterface().execString("def " + "JRuby" + "(inside, text, glob):\n\treturn JSRPlugin.getInterface(\"" + "JRuby" + "\").eval(text)");

		MinimalTextField_blockMenu.knownTextTransforms.add(new Pair<String, String>("JRuby", "embedded ruby enviroment"));

		PythonInterface.getPythonInterface().addSharedScriptingInterface(this);

	}

	EmbedRubyObjectAdapter adaptor = null;

	@Override
	public void finishInstall() {
		getAdaptor();
	}

	public EmbedRubyObjectAdapter getAdaptor() {
		return adaptor;
	}

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

	@Override
	public void execString(String exec) {
		System.out.println(" exec string <" + exec + ">");
		run(exec);
		System.out.println(" exec string finished ");
	}

	@Override
	public Object getVariable(String name) {
		return container.get("$"+name);
	}

	@Override
	public void setVariable(String name, Object value) {
//		container.put("$"+name, value);
	}

	public void setVariable_(String name, Object value) {
		container.put("$"+name, value);
	}

	public void setVariable_(String name, Object value, int level) {
		container.put("$"+name, value);
	}

	@Override
	public Map<String, Object> getVariables() {
		return container.getVarMap();
	}

	StringWriter sw;
	
	@Override
	public void pushOutput(Writer output, Writer error) {

		container.setWriter(output);
		container.setErrorWriter(error);

	}

	@Override
	public void popOutput() {

	}

	@Override
	public Object eval(String eval) {
		Object o = run(eval);
		return o;
	}

	protected Object run(String eval) {
		setVariable("$_r", null);

		Object o = container.runScriptlet(eval);
		
		Object m = getVariable("_r");
		if (m!=null)
			PythonInterface.getPythonInterface().setVariable("_r", m);
		
		return o;
		
	}

	@Override
	public Object executeStringReturnValue(String script, String tag) {
		System.out.println(" exec string <" + script + ">");
		run(script);
		System.out.println(" exec string  finished >");
		return getVariable(tag);
	}

	@Override
	public Object executeStringReturnRawValue(String script, String tag) {
		return executeStringReturnValue(script, tag);
	}

	@Override
	public void importJava(String pack, String clas) {
	}

	@Override
	public Language getLanguage() {
		return Language.ruby;
	}

	@Override
	public void pushGlobalTrap(iGlobalTrap gt) {
	}

	@Override
	public void popGlobalTrap() {
	}

	@Override
	public void addSharedScriptingInterface(ScriptingInterface s) {
	}

	public void setRoot(iVisualElement e) {

	}
}
