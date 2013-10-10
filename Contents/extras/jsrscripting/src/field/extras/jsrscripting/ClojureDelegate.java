package field.extras.jsrscripting;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.python.core.PyList;
import org.python.core.PyObject;

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
							if (p.isPaused())
								return;
							if (!(forwards || !noDefaultBackwards))
								return;

							PythonInterface.getPythonInterface().setVariable("_x", new Float(t));
							t = (t - p.getStart()) / (p.getEnd() - p.getStart());

							t = InterpretPythonAsDelegate.clamp(t);

							PythonInterface.getPythonInterface().setVariable("_t", new Float(t));
							PythonInterface.getPythonInterface().setVariable("_dt", new Float(t - ot));
							PythonInterface.getPythonInterface().setVariable("_backwards", !forwards);
							ot = t;

							try {
								((AFunction) ret).call();
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

		InterpretPythonAsDelegate.factories.add(new iDelegateForReturnValue() {
			public Delegate delegateForReturnValue(float time, Promise p, boolean forwards, Object ret, final boolean noDefaultBackwards) {
				if (ret instanceof Collection) {

					Collection t = ((Collection) ret);

					final List<Delegate> delegates = new ArrayList<Delegate>();

					Iterator ti = t.iterator();
					for (int i = 0; i < t.size(); i++) {
						Object item = ti.next();
						Delegate delegate = InterpretPythonAsDelegate.delegateForReturnValue_impl(time, p, forwards, item, noDefaultBackwards);
						if (delegate != null)
							delegates.add(delegate);
					}

					System.out.println(" delegates are :" + delegates);

					return new Delegate() {
						boolean hasStarted = false;

						public void continueToBeActive(float t, Promise p, boolean forwards) {
							if (p.isPaused())
								return;

							Delegate d = delegates.get(1 % delegates.size());
							
							if (!hasStarted) {
								if (d != null)
									d.start(t, p, forwards);
								hasStarted = true;
							}
							if (d != null)
								d.continueToBeActive(t, p, forwards);
						}

						public void jumpStop(float t, Promise p, boolean forwards) {
							Delegate d = delegates.get(2 % delegates.size());
							d.continueToBeActive(t, p, forwards);
						}

						public void start(float t, Promise p, boolean forwards) {
							Delegate d = delegates.get(0 % delegates.size());

							
							d.start(t, p, forwards);
							d.continueToBeActive(t, p, forwards);
							d.stop(t, p, forwards);
						}

						public void startAndStop(float t, Promise p, boolean forwards) {
							for (Delegate d : delegates)
								d.startAndStop(t, p, forwards);
						}

						public void stop(float t, Promise p, boolean forwards) {
							Delegate d = delegates.get(2 % delegates.size());
							d.start(t, p, forwards);
							d.continueToBeActive(t, p, forwards);
							d.stop(t, p, forwards);
						}

					};
				}
				return null;
			}
		});
	}
}
