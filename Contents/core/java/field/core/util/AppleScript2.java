package field.core.util;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import field.core.Platform;

//import apple.applescript.AppleScriptEngineFactory;

public class AppleScript2 {

	static public ScriptEngine e;
	static
	{
		if (Platform.isMac())
		{
			try {
				e = ((ScriptEngineFactory)Class.forName("apple.applescript.AppleScriptEngineFactory").newInstance()).getScriptEngine();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	private Object output;
	
	public AppleScript2(String s)
	{
		if (e!=null)
		try {
			output = e.eval(s);
		} catch (ScriptException e) {
			e.printStackTrace();
		}
	}
	
}
