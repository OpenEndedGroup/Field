package field.core.util;

import java.util.ArrayList;

import field.core.execution.PythonInterface;

public class Bounce {

	private ArrayList args;

	public Bounce(boolean b) {
		
		
		PythonInterface.getPythonInterface().execString("import os");
		PythonInterface.getPythonInterface().execString("__pid = os.getpid()");
		Object pid = PythonInterface.getPythonInterface().getVariable("__pid");

		ExecuteCommand ex = new ExecuteCommand(".", new String[] { "/bin/ps", "" + pid, "-o command=\"\"" }, true);
		ex.waitFor();

		String commandline = ex.getOutput().split("\n")[1];
		final ArrayList args = new ArrayList();
		for (String n : commandline.split(" ")) {
			if (n.startsWith("-psn")) {
			} else
				args.add(n);
		}

		this.args = args;
		System.runFinalizersOnExit(true);
		System.exit(0);
	}
	
	@Override
	protected void finalize() throws Throwable {
		System.err.println(" -- and bouncing");
		new ExecuteCommand(".", (String[])args.toArray(new String[]{}), true);
	}
}
