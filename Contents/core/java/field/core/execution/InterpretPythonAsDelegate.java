package field.core.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyFunction;
import org.python.core.PyGenerator;
import org.python.core.PyList;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PyTuple;

import field.core.execution.BasicRunner.Delegate;
import field.core.execution.PythonScriptingSystem.Promise;
import field.core.plugins.python.PythonPlugin.CapturedEnvironment;
import field.launch.SystemProperties;
import field.math.abstraction.iProvider;

/**
 * things that you can set the magic variable 'r' to decide what is executed
 * when a visualelement is "run"
 * 
 * @author marc
 * 
 */
public class InterpretPythonAsDelegate {

	public interface iDelegateForReturnValue {
		public Delegate delegateForReturnValue(float t, Promise p, boolean forwards, Object ret, final boolean noDefaultBackwards);
	}

	static public List<iDelegateForReturnValue> factories = new ArrayList<iDelegateForReturnValue>();

	static {
		factories.add(new iDelegateForReturnValue() {

			/*
			 * 
			 * r = (some number)
			 * 
			 * keeps executing all of the
			 * text in the visualelement
			 * should r become something
			 * other than a number, moves to
			 * executing that instead
			 * 
			 */

			public Delegate delegateForReturnValue(float t, Promise p, boolean forwards, final Object ret, final boolean noDefaultBackwards) {
				if (ret instanceof Number)
					return new Delegate() {

						Delegate downwards;
						
						float ot;
						public void continueToBeActive(float t, Promise p, boolean forwards) {

							if (((Number) ret).doubleValue() <= 0) {
								return;
							}

							if (!(forwards || !noDefaultBackwards))
								return;
							if (downwards != null)
								downwards.continueToBeActive(t, p, forwards);
							else {
								String text = p.getText();
								p.beginExecute();
								PythonInterface.getPythonInterface().setVariable("_x", new Float(t));
								t = (t - p.getStart()) / (p.getEnd() - p.getStart());
								t = clamp(t);
								PythonInterface.getPythonInterface().setVariable("_t", new Float(t));
								PythonInterface.getPythonInterface().setVariable("_dt", new Float(t-ot));
								PythonInterface.getPythonInterface().setVariable("_backwards", !forwards);
								ot = t;
								Object ret = null;
								try {
									ret = PythonInterface.getPythonInterface().executeStringReturnValue(text, "_r");
								} catch (Throwable tr) {
									tr.printStackTrace();
									throwExceptionInside("trying to evaluate _r while continuing to be active", p.getOngoingEnvironments(), tr);
									if (SystemProperties.getIntProperty("exitOnException", 0) == 1)
										System.exit(1);
								}
								p.endExecute();
								if (ret != null) {
									downwards = delegateForReturnValue_impl(t, p, forwards, ret, noDefaultBackwards);
									if (downwards != null) {
										downwards.start(t, p, forwards);
									}
								}
							}
						}

						public void jumpStop(float t, Promise p, boolean forwards) {
							if (((Number) ret).doubleValue() <= 0) {
								return;
							}

							p.wontExecute();
							if (!(forwards || !noDefaultBackwards))
								return;
							if (downwards != null)
								downwards.jumpStop(t, p, forwards);
						}

						public void start(float t, Promise p, boolean forwards) {
							ot = t;
							if (((Number) ret).doubleValue() <= 0) {
								return;
							}

							p.willExecute();
							if (!(forwards || !noDefaultBackwards))
								return;
							if (downwards != null)
								downwards.start(t, p, forwards);
						}

						public void startAndStop(float t, Promise p, boolean forwards) {
							if (((Number) ret).doubleValue() <= 0) {
								return;
							}

							if (!(forwards || !noDefaultBackwards))
								return;
							if (downwards != null)
								downwards.startAndStop(t, p, forwards);
						}

						public void stop(float t, Promise p, boolean forwards) {
							if (((Number) ret).doubleValue() <= 0) {
								return;
							}

							p.wontExecute();
							if (!(forwards || !noDefaultBackwards))
								return;
							if (downwards != null)
								downwards.stop(t, p, forwards);
						}
					};
				return null;
			}
		});

		factories.add(new iDelegateForReturnValue() {

			/*
			 * 
			 * r = (some function)
			 * 
			 * keeps executing that function
			 * while this box is active
			 * 
			 */

			public Delegate delegateForReturnValue(float t, Promise p, boolean forwards, Object ret, final boolean noDefaultBackwards) {
				if (ret instanceof PyFunction) {
					final PyFunction f = (PyFunction) ret;
					final EvaluationGeneratorStack stack = new EvaluationGeneratorStack(f, new ArrayList());

					return new Delegate() {

						float ot = 0;

						public void continueToBeActive(float t, Promise p, boolean forwards) {
							if( p.isPaused()) return;
							if (!(forwards || !noDefaultBackwards))
								return;
							p.beginExecute();
							PythonInterface.getPythonInterface().setVariable("_x", new Float(t));
							t = (t - p.getStart()) / (p.getEnd() - p.getStart());
							
							t = clamp(t);
							
							PythonInterface.getPythonInterface().setVariable("_t", new Float(t));
							PythonInterface.getPythonInterface().setVariable("_dt", new Float(t-ot));
							PythonInterface.getPythonInterface().setVariable("_backwards", !forwards);
							ot = t;
							try {
								stack.evaluateReturn();
							} catch (Throwable tr) {
								System.err.println(" exception throw while executing python ...<" + tr + ">");
								tr.printStackTrace();
								throwExceptionInside("trying to evaluate function stack", p.getOngoingEnvironments(), tr);
								new Exception().printStackTrace();
								if (SystemProperties.getIntProperty("exitOnException", 0) == 1)
									System.exit(1);
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
		
		factories.add(new iDelegateForReturnValue() {

			/*
			 * 
			 * r = (some function)
			 * 
			 * keeps executing that function
			 * while this box is active
			 * 
			 */

			public Delegate delegateForReturnValue(float t, Promise p, boolean forwards, Object ret, final boolean noDefaultBackwards) {
				if (ret instanceof PyMethod) {
					final PyMethod f = (PyMethod) ret;
					final EvaluationGeneratorStack stack = new EvaluationGeneratorStack(f, new ArrayList());

					return new Delegate() {

						float ot = 0;

						public void continueToBeActive(float t, Promise p, boolean forwards) {
							if( p.isPaused()) return;
							if (!(forwards || !noDefaultBackwards))
								return;
							p.beginExecute();
							PythonInterface.getPythonInterface().setVariable("_x", new Float(t));
							t = (t - p.getStart()) / (p.getEnd() - p.getStart());
							
							t = clamp(t);
							
							PythonInterface.getPythonInterface().setVariable("_t", new Float(t));
							PythonInterface.getPythonInterface().setVariable("_dt", new Float(t-ot));
							PythonInterface.getPythonInterface().setVariable("_backwards", !forwards);
							ot = t;
							try {
								stack.evaluateReturn();
							} catch (Throwable tr) {
								System.err.println(" exception throw while executing python ...<" + tr + ">");
								tr.printStackTrace();
								throwExceptionInside("trying to evaluate function stack", p.getOngoingEnvironments(), tr);
								new Exception().printStackTrace();
								if (SystemProperties.getIntProperty("exitOnException", 0) == 1)
									System.exit(1);
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
		
		factories.add(new iDelegateForReturnValue() {

			/*
			 * 
			 * r = (some function)
			 * 
			 * keeps executing that function
			 * while this box is active
			 * 
			 */

			public Delegate delegateForReturnValue(float t, Promise p, boolean forwards, Object ret, final boolean noDefaultBackwards) {
				if (ret instanceof iProvider) {
					final iProvider f = (iProvider) ret;

					return new Delegate() {

						float ot = 0;
						boolean going = false;
						
						

						public void continueToBeActive(float t, Promise p, boolean forwards) {
							if(p.isPaused())
								return;
							if (!going) return;
							
							if (!(forwards || !noDefaultBackwards))
								return;
							p.beginExecute();
							PythonInterface.getPythonInterface().setVariable("_x", new Float(t));
							t = (t - p.getStart()) / (p.getEnd() - p.getStart());
							t = clamp(t);
							PythonInterface.getPythonInterface().setVariable("_t", new Float(t));
							PythonInterface.getPythonInterface().setVariable("_dt", new Float(t-ot));
							PythonInterface.getPythonInterface().setVariable("_backwards", !forwards);
							ot = t;
							try {
								Object m = f.get();
								if (m==null)
								{
									stop(t, p, forwards);
								}
							} catch (Throwable tr) {
								System.err.println(" exception throw while executing python ...<" + tr + ">");
								tr.printStackTrace();
								throwExceptionInside("trying to evaluate function stack", p.getOngoingEnvironments(), tr);
								new Exception().printStackTrace();
								if (SystemProperties.getIntProperty("exitOnException", 0) == 1)
									System.exit(1);
							}
							p.endExecute();
						}

						public void jumpStop(float t, Promise p, boolean forwards) {
						}

						public void start(float t, Promise p, boolean forwards) {
							p.willExecute();
							t = (t - p.getStart()) / (p.getEnd() - p.getStart());
							ot = t;
							going = true;
						}

						public void startAndStop(float t, Promise p, boolean forwards) {
							going = false;

							PythonInterface.getPythonInterface().setVariable("_jump", true);
							try {
								continueToBeActive(t, p, forwards);
							} finally {
								PythonInterface.getPythonInterface().setVariable("_jump", null);
							}

						}

						public void stop(float t, Promise p, boolean forwards) {
							going = false;
							p.wontExecute();
						}
					};
				}
				return null;
			}
		});

		factories.add(new iDelegateForReturnValue() {

			/*
			 * 
			 * r = (some generator)
			 * 
			 * keeps executing that
			 * generator while this box is
			 * active (and while that
			 * generator has got something
			 * to give)
			 * 
			 */

			public Delegate delegateForReturnValue(float t, Promise p, boolean forwards, Object ret, final boolean noDefaultBackwards) {
				if (ret instanceof PyGenerator) {
					final PyGenerator go = (PyGenerator) ret;

					final EvaluationGeneratorStack stack = new EvaluationGeneratorStack(go, new ArrayList());

					return new Delegate() {

						float ot =0;

						public void continueToBeActive(float t, Promise p, boolean forwards) {
							if(p.isPaused())
								return;
							if (!(forwards || !noDefaultBackwards))
								return;
							p.beginExecute();
							PythonInterface.getPythonInterface().setVariable("_x", new Float(t));
							t = (t - p.getStart()) / (p.getEnd() - p.getStart());
							t = clamp(t);
							PythonInterface.getPythonInterface().setVariable("_t", new Float(t));
							PythonInterface.getPythonInterface().setVariable("_dt", new Float(t-ot));
							PythonInterface.getPythonInterface().setVariable("_backwards", !forwards);
							ot = t;
							try {
								stack.evaluateReturn();
							} catch (Throwable tr) {
								System.err.println(" exception throw while executing python ...<" + tr + ">");
								tr.printStackTrace();
								throwExceptionInside("trying to evaluate generator stack", p.getOngoingEnvironments(), tr);
								new Exception().printStackTrace();
								if (SystemProperties.getIntProperty("exitOnException", 0) == 1)
									System.exit(1);
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
						}

						public void stop(float t, Promise p, boolean forwards) {
							p.wontExecute();
						}
					};
				}
				return null;
			}
		});
		factories.add(new iDelegateForReturnValue() {
			public Delegate delegateForReturnValue(float time, Promise p, boolean forwards, Object ret, final boolean noDefaultBackwards) {
				if (ret instanceof PyDictionary) {

					PyDictionary t = ((PyDictionary)ret);
					
					final Delegate beginning = delegateForReturnValue_impl(time, p, forwards, t.__finditem__(Py.java2py("start")), noDefaultBackwards);
					final Delegate middle = delegateForReturnValue_impl(time, p, forwards, t.__finditem__(Py.java2py("continue")), noDefaultBackwards);
					final Delegate end = delegateForReturnValue_impl(time, p, forwards, t.__finditem__(Py.java2py("end")), noDefaultBackwards);

					;//System.out.println(" delegate for dictionary <"+beginning+" -> "+middle+" "+end+">");
					
					
					return new Delegate() {
						boolean hasStarted = false;


						public void continueToBeActive(float t, Promise p, boolean forwards) {
							if(p.isPaused())
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
		factories.add(new iDelegateForReturnValue() {
			public Delegate delegateForReturnValue(float time, Promise p, boolean forwards, Object ret, final boolean noDefaultBackwards) {
				if (ret instanceof PyList) {

					PyList t = ((PyList)ret);

					final List<Delegate> delegates = new ArrayList<Delegate>();
					for(int i=0;i<t.__len__();i++)
					{
						PyObject item = t.__getitem__(i);
						Delegate delegate = delegateForReturnValue_impl(time, p, forwards, item, noDefaultBackwards);
						if (delegate!=null)
							delegates.add(delegate);
					}
					
					;//System.out.println(" delegate for list<"+delegates+">");
					
					return new Delegate() {
						boolean hasStarted = false;
					

						public void continueToBeActive(float t, Promise p, boolean forwards) {
							if(p.isPaused())
								return;
							for(Delegate d : delegates)
								d.continueToBeActive(t, p, forwards);
						}

						public void jumpStop(float t, Promise p, boolean forwards) {
							for(Delegate d : delegates)
								d.jumpStop(t, p, forwards);
								
						}

						public void start(float t, Promise p, boolean forwards) {
							for(Delegate d : delegates)
								d.start(t, p, forwards);
						}

						public void startAndStop(float t, Promise p, boolean forwards) {
							for(Delegate d : delegates)
								d.startAndStop(t, p, forwards);
						}

						public void stop(float t, Promise p, boolean forwards) {
							for(Delegate d : delegates)
								d.stop(t, p, forwards);
						}

					};
				}
				return null;
			}
		});
		factories.add(new iDelegateForReturnValue() {

			/*
			 * 
			 * r = (a,b,c) (a 3-tuple)
			 * 
			 * executes 'a' at the start;
			 * 'b' while we are continuing
			 * and 'c' at the end;
			 * 
			 * a, b, and c are evaluated
			 * according to other factories,
			 * this means that you can embed
			 * generators and functions in
			 * here, note that ( (a,b,c),
			 * (d,e,f), (g,h,i)) calls
			 * (a,b,c) at the same time,
			 * d+e, then (f,g,h,i) in that
			 * sequence
			 * 
			 * r = (a,) does what you'd
			 * expect (i.e. it's equivelent
			 * to r = a) r = (a, b) uses a
			 * for the start and stop and b
			 * for the middle
			 */

			public Delegate delegateForReturnValue(float time, Promise p, boolean forwards, Object ret, final boolean noDefaultBackwards) {
				if (ret instanceof PyTuple) {

					PyTuple t = (PyTuple) ret;
					int num = t.__len__();
					if (num < 1)
						return null;

					final Delegate beginning = delegateForReturnValue_impl(time, p, forwards, t.__getitem__(0), noDefaultBackwards);
					final Delegate middle = delegateForReturnValue_impl(time, p, forwards, t.__getitem__(1 % num), noDefaultBackwards);
					final Delegate end = delegateForReturnValue_impl(time, p, forwards, t.__getitem__(2 % num), noDefaultBackwards);

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

		// factories.add(new iDelegateForReturnValue() {
		//
		// /*
		// *
		// * if all else fails tries calling __iter__()
		// on the pyObject.
		// This might return somethng that we can
		// handle.
		// *
		// * often you write 'someGenerator' (which is
		// actually a
		// function that returns an iterator) rather
		// than
		// 'someGenerator()' (which is the iterator)
		// */
		// public Delegate delegateForReturnValue(float
		// time, Promise p,
		// boolean forwards, Object ret, final boolean
		// noDefaultBackwards) {
		// if (ret instanceof PyObject) {
		//
		// PyObject m = null;
		// try {
		// m = ((PyObject) ret).__iter__();
		// } catch (Throwable t) {
		// }
		// if (m != null) {
		// return delegateForReturnValue_impl(time, p,
		// forwards, m,
		// noDefaultBackwards);
		// }
		//
		// }
		// return null;
		// }
		//
		// });
	}

	static public Delegate delegateForReturnValue_impl(float t, Promise p, boolean forwards, Object ret, final boolean noDefaultBackwards) {
		
		;//System.out.println(" dfrv_i :"+ret+" "+(ret == null ? null : ret.getClass()));
		
		for (iDelegateForReturnValue f : factories) {
			Delegate d = f.delegateForReturnValue(t, p, forwards, ret, noDefaultBackwards);
			if (d != null)
				return d;
		}
		return null;

	}

	static public void throwExceptionInside(String when, Stack<CapturedEnvironment> ongoingEnvironments, Throwable t) {

		;//System.out.println(" ongoing environments are <" + ongoingEnvironments + ">");

		CapturedEnvironment parent = null;
		for (CapturedEnvironment e : ongoingEnvironments) {
			e.throwException(when, t, parent);
			parent = e;
		}

	}

	static public float clamp(float t) {
		return Math.max(0, Math.min(1, t));
	}

}
