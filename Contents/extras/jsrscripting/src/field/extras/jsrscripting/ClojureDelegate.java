package field.extras.jsrscripting;

import java.io.PrintWriter;

import clojure.lang.AFunction;
import field.core.execution.BasicRunner.Delegate;
import field.core.execution.InterpretPythonAsDelegate;
import field.core.execution.InterpretPythonAsDelegate.iDelegateForReturnValue;
import field.core.execution.PythonInterface;
import field.core.execution.PythonScriptingSystem.Promise;

public class ClojureDelegate {

	public ClojureDelegate(ClojureScriptingInterface ii) {
		InterpretPythonAsDelegate.factories.add(new iDelegateForReturnValue() {

			@Override
			public Delegate delegateForReturnValue(float t, Promise p, boolean forwards, final Object ret, final boolean noDefaultBackwards) {
				if (ret instanceof AFunction) {


					return new Delegate() {
						float ot = 0;

						public void start(float t, Promise p, boolean forwards) {
							t = (t - p.getStart()) / (p.getEnd() - p.getStart());
							ot = t;
							p.willExecute();
						}

						public void startAndStop(float t, Promise p, boolean forwards) {
							PythonInterface.getPythonInterface().setVariable("_jump", true);
							try {
								continueToBeActive(t, p, forwards);
							} finally {
								PythonInterface.getPythonInterface().setVariable("_jump", null);
							}

						}

						public void stop(float t, Promise p, boolean forwards) {
							p.wontExecute();
						}

						@Override
						public void jumpStop(float t, Promise p, boolean forwards) {

						}

						@Override
						public void continueToBeActive(float t, Promise p, boolean forwards) {
							if( p.isPaused()) return;
							if (!(forwards || !noDefaultBackwards))
								return;
							
							PythonInterface.getPythonInterface().setVariable("_x", new Float(t));
							t = (t - p.getStart()) / (p.getEnd() - p.getStart());
							
							t = InterpretPythonAsDelegate.clamp(t);
							
							PythonInterface.getPythonInterface().setVariable("_t", new Float(t));
							PythonInterface.getPythonInterface().setVariable("_dt", new Float(t-ot));
							PythonInterface.getPythonInterface().setVariable("_backwards", !forwards);
							ot = t;
							
							try {
								((AFunction)ret).call();
							} catch (Throwable tr) {
								System.err.println(" exception throw while executing python ...<" + tr + ">");
								tr.printStackTrace();
								
								PrintWriter w = new PrintWriter(PythonInterface.getPythonInterface().errOut);
								tr.printStackTrace(w);
							}
							
						}
					};
				} else
					return null;
			}
		});
	}
}
