package field.extras.jsrscripting;

import java.io.PrintWriter;
import java.io.Writer;

import org.jruby.RubyArray;
import org.jruby.RubyObjectAdapter;
import org.jruby.RubyProc;

import field.core.execution.BasicRunner.Delegate;
import field.core.execution.InterpretPythonAsDelegate;
import field.core.execution.InterpretPythonAsDelegate.iDelegateForReturnValue;
import field.core.execution.PythonInterface;
import field.core.execution.PythonScriptingSystem.Promise;
import field.launch.SystemProperties;

public class JRubyDelegate {

	
	
	public JRubyDelegate (final JRubyScriptingInterface ii)
	{
		
		InterpretPythonAsDelegate.factories.add(new iDelegateForReturnValue() {
			@Override
			public Delegate delegateForReturnValue(float t, Promise p, boolean forwards, Object ret, final boolean noDefaultBackwards) {
		
				
				if (ret instanceof RubyProc)
				{
					final RubyProc proc = ((RubyProc)ret);
					
					return new Delegate() {

						float ot = 0;

						public void continueToBeActive(float t, Promise p, boolean forwards) {
							if( p.isPaused()) return;
							if (!(forwards || !noDefaultBackwards))
								return;
							p.beginExecute();
							PythonInterface.getPythonInterface().setVariable("_x", new Float(t));
							t = (t - p.getStart()) / (p.getEnd() - p.getStart());
							
							t = InterpretPythonAsDelegate.clamp(t);
							
							PythonInterface.getPythonInterface().setVariable("_t", new Float(t));
							PythonInterface.getPythonInterface().setVariable("_dt", new Float(t-ot));
							PythonInterface.getPythonInterface().setVariable("_backwards", !forwards);
							ot = t;
							
							Writer error = PythonInterface.getPythonInterface().getErrorRedirects().peek();
							Writer out = PythonInterface.getPythonInterface().getOutputRedirects().peek();

							try {
			
								
								ii.pushOutput(out, error);
								
//								System.out.println(" >> pushed <"+out+" "+error+">");
								
								ii.getAdaptor().callMethod(proc, "call");

//								System.out.println(" >> popping <"+out+" "+error+">");
							
							} catch (Throwable tr) {
								System.err.println(" exception throw while executing python ...<" + tr + ">");
								tr.printStackTrace();
								
								PrintWriter w = new PrintWriter(PythonInterface.getPythonInterface().errOut);
								tr.printStackTrace(w);
								
								InterpretPythonAsDelegate.throwExceptionInside("trying to evaluate function stack", p.getOngoingEnvironments(), tr);
								new Exception().printStackTrace();
								if (SystemProperties.getIntProperty("exitOnException", 0) == 1)
									System.exit(1);
							}
							finally
							{
								ii.popOutput();
							}
							p.endExecute();
						}


						public void jumpStop(float t, Promise p, boolean forwards) {
						}

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
					};

					
				}
				
				return null;
			}
		});
		InterpretPythonAsDelegate.factories.add(new iDelegateForReturnValue() {
			
			@Override
			public Delegate delegateForReturnValue(float time, Promise p, boolean forwards, Object ret, boolean noDefaultBackwards) {
				
				if (ret instanceof RubyArray)
				{
					RubyArray a = ((RubyArray)ret);
					int n = a.size();
					
					final Delegate beginning = InterpretPythonAsDelegate.delegateForReturnValue_impl(time, p, forwards, a.get(0), noDefaultBackwards);
					final Delegate middle = InterpretPythonAsDelegate.delegateForReturnValue_impl(time, p, forwards, a.get(1 % n), noDefaultBackwards);
					final Delegate end = InterpretPythonAsDelegate.delegateForReturnValue_impl(time, p, forwards, a.get(2 % n), noDefaultBackwards);

					
					return new Delegate() {
						boolean hasStarted = false;

					

						public void continueToBeActive(float t, Promise p, boolean forwards) {
							if (p.isPaused()) return;
							
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
