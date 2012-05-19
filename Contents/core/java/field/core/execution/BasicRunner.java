package field.core.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import field.core.dispatch.iVisualElement;
import field.core.execution.PythonScriptingSystem.Promise;
import field.core.execution.PythonScriptingSystem.Runner;
import field.core.plugins.log.Logging;
import field.core.plugins.log.ElementInvocationLogging.ElementTextWasExecuted;
import field.launch.SystemProperties;
import field.math.abstraction.iFloatProvider;

/**
 * a runner than knows how to deal with many kinds of things that python can
 * throw at it
 * 
 * @author marc created on Jan 31, 2004
 */
public class BasicRunner extends Runner implements iExecutesPromise {

	public interface Delegate {

		public void continueToBeActive(float t, Promise p, boolean forwards);

		public void jumpStop(float t, Promise p, boolean forwards);

		public void start(float t, Promise p, boolean forwards);

		public void startAndStop(float t, Promise p, boolean forwards);

		public void stop(float t, Promise p, boolean forwards);
	}

	static public final iVisualElement.VisualElementProperty<BasicRunner> basicRunner = new iVisualElement.VisualElementProperty<BasicRunner>("basicRunner_");

	static protected Delegate createDelegateForPromise_static(float t, Promise p, boolean forwards, boolean noDefaultBackwards) {
		String text = p.getText();

		PythonInterface.getPythonInterface().setVariable("_x", new Float(t));
		t = (t - p.getStart()) / (p.getEnd() - p.getStart());
		t = InterpretPythonAsDelegate.clamp(t);
		p.beginExecute();
		PythonInterface.getPythonInterface().setVariable("_t", new Float(t));
		PythonInterface.getPythonInterface().setVariable("_dt", new Float(0));
		Object ret = null;
		try {
			ret = PythonInterface.getPythonInterface().executeStringReturnValue(text, "_r");
		} catch (Throwable tr) {

			System.err.println(" exception throw while executing python ...<" + text + "> <" + tr + ">");
			tr.printStackTrace();
			new Exception().printStackTrace();
			if (SystemProperties.getIntProperty("exitOnException", 0) == 1)
				System.exit(1);
		}

		p.endExecute();
		Delegate d = delegateForReturnValue_static(t, p, forwards, ret, noDefaultBackwards);
		return d;
	}

	static protected Delegate delegateForReturnValue_static(float t, Promise p, boolean forwards, Object ret, final boolean noDefaultBackwards) {
		Delegate d = InterpretPythonAsDelegate.delegateForReturnValue_impl(t, p, forwards, ret, noDefaultBackwards);
		if (d == null) {
			return new Delegate() {

				public void continueToBeActive(float t, Promise p, boolean forwards) {
				}

				public void jumpStop(float t, Promise p, boolean forwards) {
				}

				public void start(float t, Promise p, boolean forwards) {
				}

				public void startAndStop(float t, Promise p, boolean forwards) {
				}

				public void stop(float t, Promise p, boolean forwards) {
				}

			};
		}
		;
		return d;
	}

	protected final PythonScriptingSystem system;

	protected boolean noDefaultBackwards = false;

	protected boolean dontStartAndStopSkipped = false;

	protected HashMap runningDelegates = new HashMap();

	protected HashMap timeDelegates = new HashMap();

	List alsoActive = new ArrayList();

	Comparator activeComparator = new Comparator() {

		public int compare(Object o1, Object o2) {
			Promise p1 = (Promise) o1;
			Promise p2 = (Promise) o2;
			float y1 = p1.getPriority();
			float y2 = p2.getPriority();
			return y1 < y2 ? -1 : 1;
		}
	};

	boolean continueToBeActiveCanCreate = true;

	public BasicRunner(PythonScriptingSystem system, float timeStart) {
		system.super(timeStart);
		this.system = system;

	}

	public void addActive(float t, Promise p) {
		if (!filter(p))
			return;
		start(t, p, true);
		alsoActive.add(p);
	}

	public void addActive(iFloatProvider timeProvider, Promise p) {

		if (!filter(p))
			return;
		if ((Delegate) runningDelegates.get(p) != null) {
			boolean also = alsoActive.contains(p);
			removeActive(timeProvider.evaluate(), p);
			runningDelegates.remove(p);
			start(timeProvider.evaluate(), p, true);
			if (also)
				alsoActive.add(p);
			return;
		}
		start(timeProvider.evaluate(), p, true);
		alsoActive.add(p);
		timeDelegates.put(p, timeProvider);
	}

	@Override
	public boolean continueToBeActive(float t, Promise p, boolean forwards) {
		if (!filter(p))
			return true;
		t = timeDelegates.get(p) != null ? ((iFloatProvider) timeDelegates.get(p)).evaluate() : t;

		Delegate delegate;
		if (continueToBeActiveCanCreate) {
			delegate = delegateForPromise(t, p, forwards);
			runningDelegates.put(p, delegate);
		} else
			delegate = (Delegate) runningDelegates.get(p);

		if (delegate != null) {

			delegate.continueToBeActive(rewriteTime(t, p), p, forwards);
			return false;
		} else {
			alsoActive.remove(p);
			runningDelegates.remove(p);
			timeDelegates.remove(p);
			return true;
		}
	}

	public iExecutesPromise dontExecuteBackwards() {
		noDefaultBackwards = true;
		return this;
	}

	public iExecutesPromise dontStartAndStopSkipped() {
		dontStartAndStopSkipped = true;
		return this;
	}

	public iExecutesPromise doStartAndStopSkipped() {
		dontStartAndStopSkipped = false;
		return this;
	}

	public iExecutesPromise executeBackwards() {
		noDefaultBackwards = false;
		return this;
	}

	public ArrayList getActive() {
		ArrayList al = new ArrayList(active);
		al.addAll(alsoActive);
		return al;
	}

	@Override
	public void jumpStop(float t, Promise p, boolean forwards) {
		if (!filter(p))
			return;
		t = timeDelegates.get(p) != null ? ((iFloatProvider) timeDelegates.get(p)).evaluate() : t;
		Delegate delegate = delegateForPromise(t, p, forwards);
		if (delegate != null)
			delegate.jumpStop(rewriteTime(t, p), p, forwards);
		runningDelegates.remove(p);
	}

	public void removeActive(float t, Promise p) {
		t = timeDelegates.get(p) != null ? ((iFloatProvider) timeDelegates.get(p)).evaluate() : t;
		stop(t, p, true);
		alsoActive.remove(p);
		active.remove(p);
		timeDelegates.remove(p);

		runningDelegates.remove(p);
	}

	public void removeActive(Promise p) {
		
		if (runningDelegates.get(p) == null)
			return;
		if (timeDelegates.get(p) == null) {
			removeActive(0, p);
			return;
		}

		float t = ((iFloatProvider) timeDelegates.remove(p)).evaluate();

		removeActive(t, p);

		active.remove(p);
	}

	@Override
	public void start(float t, Promise p, boolean forwards) {
		if (!filter(p))
			return;
		t = timeDelegates.get(p) != null ? ((iFloatProvider) timeDelegates.get(p)).evaluate() : t;
		Delegate delegate = delegateForPromise(t, p, forwards);

		if (delegate != null) {
			delegate.start(rewriteTime(t, p), p, forwards);
			runningDelegates.put(p, delegate);
		}
	}

	@Override
	public void startAndStop(float t, Promise p, boolean forwards) {
		if (!filter(p))
			return;
		if (dontStartAndStopSkipped)
			return;

		t = timeDelegates.get(p) != null ? ((iFloatProvider) timeDelegates.get(p)).evaluate() : t;
		try {
			PythonInterface.getPythonInterface().setVariable("_jump", true);
			PythonInterface.getPythonInterface().setVariable("_backwards", !forwards);

			Delegate delegate = delegateForPromise(t, p, forwards);
			if (delegate != null)
				delegate.startAndStop(rewriteTime(t, p), p, forwards);
		} finally {
			PythonInterface.getPythonInterface().setVariable("_jump", null);
		}
	}

	@Override
	public void stop(float t, Promise p, boolean forwards) {
		if (!filter(p))
			return;
		t = timeDelegates.get(p) != null ? ((iFloatProvider) timeDelegates.get(p)).evaluate() : t;
		Delegate delegate = (Delegate) runningDelegates.get(p);
		if (delegate != null)
			delegate.stop(rewriteTime(t, p), p, forwards);
		runningDelegates.remove(p);
	}

	public void stopAll(float t) {
		for (int i = alsoActive.size() - 1; i >= 0; i--) {
			stop(t, (Promise) alsoActive.remove(i), true);
		}
		Iterator i = new HashSet(runningDelegates.keySet()).iterator();
		while (i.hasNext()) {
			Promise p = (Promise) i.next();
			stop(t, p, true);
			active.remove(p);
		}
	}

	@Override
	public void update(float t) {
		continueToBeActiveCanCreate = false;

		super.update(t);

		for (int i = 0; i < alsoActive.size(); i++) {
			Promise p = (Promise) alsoActive.get(i);
			boolean removed = continueToBeActive(t, p, true);
			if (removed)
				i--;
		}
		continueToBeActiveCanCreate = true;
	}

	private String info(Promise p) {
		return p.toString();
	}

	private float rewriteTime(float t, Promise p) {
		return system.rewriteTime(t, p);
	}

	protected Delegate createDelegateForPromise(float t, Promise p, boolean forwards, boolean noDefaultBackwards) {

		t = rewriteTime(t, p);

		String text = p.getText();

		PythonInterface.getPythonInterface().setVariable("_x", new Float(t));

		t = (t - p.getStart()) / (p.getEnd() - p.getStart());
		p.beginExecute();
		t = InterpretPythonAsDelegate.clamp(t);
		PythonInterface.getPythonInterface().setVariable("_t", new Float(t));
		PythonInterface.getPythonInterface().setVariable("_dt", new Float(0));
		PythonInterface.getPythonInterface().setVariable("_forwards", new Boolean(forwards));
		Object ret = null;

		if (Logging.enabled())
			Logging.logging.addEvent(new ElementTextWasExecuted(text));

		try {
		
			ret = PythonInterface.getPythonInterface().executeStringReturnValue(text, "_r");

		
		} catch (Throwable tr) {
			System.err.println(" exception throw while executing python ...<" + text + "> <" + tr + ">");
			System.err.println(" python stack trace:");
			PythonInterface.writeException(tr);
			tr.printStackTrace();

			InterpretPythonAsDelegate.throwExceptionInside("trying to evaluate _r", p.getOngoingEnvironments(), tr);

			System.err.println(" java stack trace");
			new Exception().printStackTrace();
			if (SystemProperties.getIntProperty("exitOnException", 0) == 1)
				System.exit(1);
		}
		p.endExecute();
		Delegate d = delegateForReturnValue(t, p, forwards, ret, noDefaultBackwards);
		return d;
	}

	protected Delegate delegateForPromise(float t, Promise p, boolean forwards) {
		t = timeDelegates.get(p) != null ? ((iFloatProvider) timeDelegates.get(p)).evaluate() : t;
		Delegate ret = (Delegate) runningDelegates.get(p);
		if (ret == null) {
			return createDelegateForPromise(t, p, forwards, noDefaultBackwards);
		}
		return ret;
	}

	protected Delegate delegateForReturnValue(float t, Promise p, boolean forwards, Object ret, final boolean noDefaultBackwards) {
		Delegate d = InterpretPythonAsDelegate.delegateForReturnValue_impl(t, p, forwards, ret, noDefaultBackwards);

		return d;
	}

	protected boolean filter(Promise p) {
		return true;
	}

	@Override
	protected void sortActive() {
		Collections.sort(active, activeComparator);
	}
	
	

}