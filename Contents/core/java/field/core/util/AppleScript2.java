package field.core.util;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import apple.applescript.AppleScriptEngineFactory;

public class AppleScript2 {

	static public final ScriptEngine e = new AppleScriptEngineFactory().getScriptEngine();
	private Object output;
	
	public AppleScript2(String s)
	{
		try {
			output = e.eval(s);
		} catch (ScriptException e) {
			e.printStackTrace();
		}
	}
	
}
