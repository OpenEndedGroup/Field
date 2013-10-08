package field.extras.jsrscripting;

import java.io.PrintWriter;
import java.io.Writer;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.internal.runtime.ScriptFunction;
import jdk.nashorn.internal.runtime.ScriptObject;
import field.core.execution.BasicRunner.Delegate;
import field.core.execution.InterpretPythonAsDelegate;
import field.core.execution.InterpretPythonAsDelegate.iDelegateForReturnValue;
import field.core.execution.PythonInterface;
import field.core.execution.PythonScriptingSystem.Promise;
import field.launch.SystemProperties;
import field.launch.iUpdateable;

public class NashornDelegate {

	public NashornDelegate(final NashornScriptingInterface ii) {
		InterpretPythonAsDelegate.factories.add(new iDelegateForReturnValue() {
			public Delegate delegateForReturnValue(float t, Promise p, boolean forwards, Object ret, boolean noDefaultBackwards) {

				if (ret instanceof ScriptFunction) {
					final iUpdateable u = ((NashornScriptEngine) ii.engine).getInterface(ret, iUpdateable.class);
					return new Delegate() {
						float ot = 0;

						@Override
						public void continueToBeActive(float t, Promise p, boolean forwards) {
							if (p.isPaused())
								return;
							p.beginExecute();
							((NashornScriptEngine) ii.engine).put("_x", t);
							t = (t - p.getStart()) / (p.getEnd() - p.getStart());
							t = InterpretPythonAsDelegate.clamp(t);

							PythonInterface.getPythonInterface().setVariable("_t", t);
							PythonInterface.getPythonInterface().setVariable("_dt", t - ot);
							PythonInterface.getPythonInterface().setVariable("_backwards", !forwards);
							ot = t;

							Writer error = PythonInterface.getPythonInterface().getErrorRedirects().peek();
							Writer out = PythonInterface.getPythonInterface().getOutputRedirects().peek();

							try {
								ii.pushOutput(out, error);
								u.update();
							} catch (Throwable tr) {
								System.err.println(" exception throw while executing python ...<" + tr + ">");
								tr.printStackTrace();

								PrintWriter w = new PrintWriter(PythonInterface.getPythonInterface().errOut);
								tr.printStackTrace(w);

								InterpretPythonAsDelegate.throwExceptionInside("trying to evaluate function stack", p.getOngoingEnvironments(), tr);
								new Exception().printStackTrace();
								if (SystemProperties.getIntProperty("exitOnException", 0) == 1)
									System.exit(1);
							} finally {
								ii.popOutput();
							}
							p.endExecute();
						}

						@Override
						public void jumpStop(float t, Promise p, boolean forwards) {

						}

						@Override
						public void start(float t, Promise p, boolean forwards) {
							t = (t - p.getStart()) / (p.getEnd() - p.getStart());
							ot = t;
							p.willExecute();
						}

						@Override
						public void startAndStop(float t, Promise p, boolean forwards) {
							continueToBeActive(t, p, forwards);
						}

						@Override
						public void stop(float t, Promise p, boolean forwards) {
							p.wontExecute();
						}

					};
				}
				return null;
			}
		});

		InterpretPythonAsDelegate.factories.add(new iDelegateForReturnValue() {
			public Delegate delegateForReturnValue(float time, Promise p, boolean forwards, Object ret, boolean noDefaultBackwards) {
				if (ret instanceof ScriptObject) {

					System.out.println(" NA _r is :" + ret + " of type " + ret.getClass());

					ScriptObject a = ((ScriptObject) ret);
					int n = a.size();

					if (n == 1)
						return null;

					System.out.println(" size is :" + n);

					final Delegate beginning = InterpretPythonAsDelegate.delegateForReturnValue_impl(time, p, forwards, a.get(0), noDefaultBackwards);
					final Delegate middle = InterpretPythonAsDelegate.delegateForReturnValue_impl(time, p, forwards, a.get(1 % n), noDefaultBackwards);
					final Delegate end = InterpretPythonAsDelegate.delegateForReturnValue_impl(time, p, forwards, a.get(2 % n), noDefaultBackwards);

					System.out.println(" bme, :" + beginning + " " + middle + " " + end);

					return new Delegate() {
						boolean hasStarted = false;

						public void continueToBeActive(float t, Promise p, boolean forwards) {
							if (p.isPaused())
								return;

							if (!hasStarted) {
								if (middle != null)
									middle.start(t, p, forwards);
								hasStarted = true;
							}
							if (middle != null)
								middle.continueToBeActive(t, p, forwards);
						}

						public void jumpStop(float t, Promise p, boolean forwards) {
							if (hasStarted) {
								if (middle != null)
									middle.jumpStop(t, p, forwards);
							}
							if (end != null)
								end.jumpStop(t, p, forwards);
						}

						public void start(float t, Promise p, boolean forwards) {
							if (beginning != null) {
								beginning.start(t, p, forwards);
								beginning.continueToBeActive(t, p, forwards);
								beginning.stop(t, p, forwards);
							}
						}

						public void startAndStop(float t, Promise p, boolean forwards) {
							if (beginning != null)
								beginning.startAndStop(t, p, forwards);
							if (end != null)
								end.startAndStop(t, p, forwards);
						}

						public void stop(float t, Promise p, boolean forwards) {
							if (hasStarted) {
								if (middle != null)
									middle.stop(t, p, forwards);
								hasStarted = false;
							}
							if (end != null) {
								end.start(t, p, forwards);
								end.continueToBeActive(t, p, forwards);
								end.stop(t, p, forwards);
							}
						}

					};
				}

				return null;
			}
		});
	}
}
