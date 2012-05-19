package field.core.execution;

import java.io.Writer;
import java.util.Map;

public interface ScriptingInterface {

	static public interface iGlobalTrap {
		public Object findItem(String name, Object actuallyIs);

		public Object setItem(String name, Object was, Object to);
	}
	
	static public enum Language {
		python, ruby, scala, jsr, ioke;
	}

	public void setVariable(String name, Object value);

	public Object getVariable(String name);

	public Map<String, Object> getVariables();

	public void pushOutput(Writer output, Writer error);

	public void popOutput();

	public void execString(String exec);

	public Object eval(String eval);

	public Object executeStringReturnValue(String script, String tag);

	public Object executeStringReturnRawValue(String script, String tag);

	public void importJava(String pack, String clas);

	public Language getLanguage();

	public void pushGlobalTrap(iGlobalTrap gt);

	public void popGlobalTrap();
	
	public void addSharedScriptingInterface(ScriptingInterface s);
	
	public void finishInstall();
	
}
