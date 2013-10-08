package field.extras.jsrscripting;

import java.io.PrintStream;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import scala.reflect.internal.util.Position;
import scala.tools.nsc.interpreter.Completion.ScalaCompleter;
import scala.tools.nsc.interpreter.IMain;
import scala.tools.nsc.interpreter.JLineCompletion;
import scala.tools.nsc.reporters.Reporter;
import scala.tools.nsc.settings.MutableSettings.BooleanSetting;
import field.core.execution.PythonInterface;

public class ScalaScriptingInterface extends JSRInterface {

	private ScalaCompleter completer;

	@Override
	protected ScriptEngine makeEngine() {

		this.engine = new ScriptEngineManager().getEngineByName("scala");
		((BooleanSetting) (((IMain) this.engine).settings().usejavacp())).value_$eq(true);
		this.context = this.engine.getContext();

		((IMain) this.engine).global().reporter_$eq(new Reporter() {
			public void info0(Position arg0, String arg1, Severity arg2, boolean arg3) {
				if (arg2.toString().contains("ERROR")) {
					PrintStream o = PythonInterface.getPythonInterface().getErrorStream();
					o.println(arg0.safeLine()+": "+arg1);
				} else {
					PrintStream o = PythonInterface.getPythonInterface().getOutputStream();
					o.println(arg0.safeLine()+": "+arg1);
				}
			}
		});

		try {
			this.engine.eval("import field.core.execution.PythonInterface\n" + "import scala.language.dynamics\n" + "\n" + "\n" + "class PythonAccess extends Dynamic\n" + "{\n" + "	def selectDynamic( s : String) =\n" + "		PythonInterface.getPythonInterface().getVariable(s)\n" + "	def updateDynamic( s : String)(v : Any) =\n" + "		PythonInterface.getPythonInterface().setVariable(s, v)\n" + "}\n" + "var _python = new PythonAccess()\n");
		} catch (ScriptException e) {
			e.printStackTrace();
		}

		completer = new JLineCompletion(((IMain) this.engine)).completer();

		return engine;
	}

	static public String installed(JSRPlugin plugin) {

		try {
			System.err.println(" checking scala availablility...");
			Class<?> x = plugin.getClass().getClassLoader().loadClass("scala.tools.nsc.Global");
			if (x != null) {
				System.err.println(" scala is available");
				return "";
			}
		} catch (ClassNotFoundException e) {
			return "working on it";
		}
		return "unspecified error";
	}

	// HashSet<String> permitted = new
	// LinkedHashSet<String>(Arrays.asList(new String[]{"_r"}));

	@Override
	public void setVariable(String name, Object value) {

		// System.out.println(" -- set var :" + name + "  (skipped)");

		// // TODO Auto-generated method stub
		// super.setVariable(name, value);
	}

	@Override
	protected String transformName() {
		return "Scala";
	}

	@Override
	protected String description() {
		return "Execute text as Scala";
	}
}
