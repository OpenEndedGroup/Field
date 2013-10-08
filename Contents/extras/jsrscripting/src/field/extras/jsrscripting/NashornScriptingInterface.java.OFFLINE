package field.extras.jsrscripting;

import java.io.Writer;
import java.lang.reflect.Field;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import jdk.nashorn.internal.runtime.ScriptObject;
import field.core.execution.PythonInterface;
import field.core.ui.text.embedded.MinimalTextField_blockMenu;
import field.namespace.generic.Generics.Pair;

public class NashornScriptingInterface extends JSRInterface {

	@Override
	protected String description() {
		return "Nashorn (fast, JDK8-only Javascript)";
	}

	@Override
	protected String transformName() {
		return "Nashorn";
	}

	@Override
	protected ScriptEngine makeEngine() {

		try {
			ScriptEngineFactory fact = (ScriptEngineFactory) this.getClass().getClassLoader().loadClass("jdk.nashorn.api.scripting.NashornScriptEngineFactory").newInstance();
			ScriptEngine engine = fact.getScriptEngine();
			
			PythonInterface.getPythonInterface().execString("\n" + "class NashornNamespaceAccess:\n" + "	def __getattr__(self, name):\n" + "		return JSRPlugin.getInterface(\"Nashorn\").engine.get(name)\n" + "	def __setattr__(self, name, value):\n" + "		JSRPlugin.getInterface(\"Nashorn\").engine.put(name, value)\n" + "_js=NashornNamespaceAccess()");

			PythonInterface.getPythonInterface().execString("from field.core.ui.text.embedded import MinimalTextField_blockMenu\n" + 
					"\n" + 
					"def coffeeToJavascript(n):\n" + 
					"	f = File.createTempFile(\"field\", \".coffee\")\n" + 
					"	f2 = file(f.getAbsolutePath(), \"w\")\n" + 
					"	print >> f2, n\n" + 
					"	f2.close()\n" + 
					"	ex = ExecuteCommand(\".\", [\"/bin/bash\", \"-c\", \"cat \"+f.getAbsolutePath()+\" | \"+\"coffee -bsp\"], 1)\n" + 
//					"	ex = ExecuteCommand(\".\", [\"/bin/bash\", \"-c\", \"cat \"+f.getAbsolutePath()+\" | \"+\"coffee -j -b\"], 1)\n" + 
					"	if (ex.waitFor(1)==1):\n" + 
					"		raise BaseException(ex.getOutput())\n" + 
					"	return ex.getOutput()\n" + 
					"\n" + 
					"\n" + 
					"");

			PythonInterface.getPythonInterface().execString("from field.core.ui.text.embedded import MinimalTextField_blockMenu\n" + 
					"\n" + 
					"def livescriptToJavascript(n):\n" + 
					"	f = File.createTempFile(\"field\", \".coffee\")\n" + 
					"	f2 = file(f.getAbsolutePath(), \"w\")\n" + 
					"	print >> f2, n\n" + 
					"	f2.close()\n" + 
					"	ex = ExecuteCommand(\".\", [\"/bin/bash\", \"-c\", \"cat \"+f.getAbsolutePath()+\" | \"+\"lsc -pcb\"], 1)\n" + 
//					"	ex = ExecuteCommand(\".\", [\"/bin/bash\", \"-c\", \"cat \"+f.getAbsolutePath()+\" | \"+\"coffee -j -b\"], 1)\n" + 
					"	if (ex.waitFor(1)==1):\n" + 
					"		raise BaseException(ex.getOutput())\n" + 
					"	return ex.getOutput()\n" + 
					"\n" + 
					"\n" + 
					"");
			
			PythonInterface.getPythonInterface().execString("from jdk.nashorn.api.scripting import ScriptObjectMirror\n"+
					"def __som_call__(self, *args):\n" + 
					"	return self.sobj.invoke(None, *args)\n" + 
					"\n" + 
					"\n" + 
					"ScriptObjectMirror.__call__ = __som_call__\n");
			
			PythonInterface.getPythonInterface().execString("def Coffee(inside, text, glob):\n\treturn JSRPlugin.getInterface(\"" + transformName() + "\").eval(coffeeToJavascript(text))");
			PythonInterface.getPythonInterface().execString("def LiveScript(inside, text, glob):\n\treturn JSRPlugin.getInterface(\"" + transformName() + "\").eval(livescriptToJavascript(text))");

			MinimalTextField_blockMenu.knownTextTransforms.add(new Pair<String, String>("Coffee", "Nashorn + Coffee-Script (needs coffee on PATH)"));
			MinimalTextField_blockMenu.knownTextTransforms.add(new Pair<String, String>("LiveScript", "Nashorn + Live-Script (needs lsc on PATH)"));

			new NashornDelegate(this);
			return engine;
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;

	}
	
	@Override
	public void pushOutput(Writer output, Writer error) {
		super.pushOutput(output, error);
	}

	@Override
	public Object eval(String eval) {
		
		if (eval.indexOf("\n")==-1 && eval.startsWith("print "))
			eval = eval.replace("print ", "print(")+")";
		
		engine.put("_r", null);
		Object o = super.eval(eval);
		Object _r = engine.get("_r");
		if (_r != null) {
			try {
				Field f = _r.getClass().getDeclaredField("global");
				f.setAccessible(true);
				ScriptObject global = (ScriptObject) f.get(_r);
				_r = global.get("_r");
				
				System.out.println(" after unwarping :"+_r);
				
				PythonInterface.getPythonInterface().setVariable("_r", _r);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		return o;
	}

	@Override
	public void setVariable(String name, Object value) {

	}

	static public String installed(JSRPlugin plugin) {
		try {
			ScriptEngineFactory fact = (ScriptEngineFactory) plugin.getClass().getClassLoader().loadClass("jdk.nashorn.api.scripting.NashornScriptEngineFactory").newInstance();
			return "";
		} catch (Throwable t) {
			return "Not running on JDK8, or nashorn.jar not installed";
		}
	}

}
