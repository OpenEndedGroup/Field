package marc.plugins.topology;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;

import field.bytecode.protect.DeferedInQueue.iProvidesQueue;
import field.bytecode.protect.DeferedInQueue.iRegistersUpdateable;
import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.InQueue;
import field.bytecode.protect.annotations.Yield;
import field.bytecode.protect.yield.YieldUtilities;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.execution.BasicRunner.Delegate;
import field.core.execution.InterpretPythonAsDelegate;
import field.core.execution.PythonScriptingSystem;
import field.core.execution.PythonScriptingSystem.Promise;
import field.launch.iUpdateable;
import field.util.TaskQueue;

@Woven
public abstract class GraphRunner implements iUpdateable, iProvidesQueue {

	public class At {

		public iVisualElement from;
		public iVisualElement to;

		public int ticks;
		public int duration;

		public Promise promiseTo;
		public Delegate ongoing;
	}

	static PrintStream out = System.out;

	static {
		File f = new File("/dev/ttys004");
		if (f.canWrite()) {
			try {
				out = new PrintStream(new FileOutputStream(f));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	At current = null;

	TaskQueue q = new TaskQueue();

	@InQueue
	public void begin(iVisualElement e) {
		System.out.println(" inside begin <" + e + "> <" + current + ">");
		if (current != null) {
			current.ongoing.stop(convertTime(current.ticks / (float) current.duration, current.to), current.promiseTo, false);
		}
		At a = new At();
		a.from = null;
		a.to = e;

		PythonScriptingSystem pss = PythonScriptingSystem.pythonScriptingSystem.get(e);
		System.out.println(" pss is <" + pss + ">");
		Promise promise = wrapPromise(pss.promiseForKey(e));

		Object ret = ongoingObject(current, e);
		System.out.println(" ongoing object");
		if (ret == null) {
			current = null;
			finished();
		}

		Delegate d = InterpretPythonAsDelegate.delegateForReturnValue_impl(convertTime(0, e), promise, true, ret, false);
		if (d == null) {
			current = null;
			finished();
		}

		d.start(convertTime(0, e), promise, true);

		a.promiseTo = promise;
		a.ongoing = d;
		updateDuration(a);
		current = a;
	}

	public iRegistersUpdateable getQueueFor(Method m) {
		return q;
	}

	public void update() {

		q.update();

		out.println(" updating graph, runner at <" + current + "> -------------");

		if (current == null) {
			out.println(" updating graph, nothing to do ");
			return;
		}

		coreUpdate();
		out.println(" updating graph, complete runner at <" + current + "> -------------");

	}

	private float convertTime(float fraction, iVisualElement next) {
		Rect f = next.getFrame(null);
		return (float) (f.x + f.w * fraction);
	}

	protected boolean canNext(At current) {
		return true;
	}

	protected void consumeNext(At current2) {
	}

	@Yield
	protected void coreUpdate() {

		current.ticks++;
		updateDuration(current);
		float _t = current.ticks / (float) current.duration;

		out.println(this + " updating ticks <" + current.ticks + "> / <" + current.duration + ">");
		if (_t >= 1) {
			iVisualElement next = getNext(current);
			if (next != null) {
				out.println(this+" next will be <"+next+"> while current is <"+current.to+">");
				consumeNext(current);
				willNext(current);
				while (!canNext(current)) {
					out.println(this + " waiting to go forward at <" + current.ticks + ">");
					current.ongoing.continueToBeActive(convertTime(_t, current.to), current.promiseTo, true);
					YieldUtilities.yield(null);
				}
				out.println(this + " can go forward at <" + current.ticks + ">");
				didNext(current);

				out.println(this+" calling stop");
				current.ongoing.stop(1, current.promiseTo, true);


				PythonScriptingSystem pss = PythonScriptingSystem.pythonScriptingSystem.get(next);
				Promise promise = wrapPromise(pss.promiseForKey(next));

				Object ret = ongoingObject(current, next);
				if (ret == null) {
					current = null;
					finished();
					return;
				}

				Delegate d = InterpretPythonAsDelegate.delegateForReturnValue_impl(convertTime(0, next), promise, true, ret, false);

				if (d == null) {
					current = null;
					finished();
					return;
				}

				d.start(convertTime(0, next), promise, true);

				At a = new At();
				a.from = current.to;
				a.to = next;
				a.ticks = 0;
				a.promiseTo = promise;
				a.ongoing = d;
				updateDuration(a);
				current = a;
			} else {
				out.println(this+" calling stop");
				if (current.ongoing!=null);
//				current.ongoing.stop(1, current.promiseTo, true);
					current.ongoing.continueToBeActive(1, current.promiseTo, true);

//				current = null;
//				finished();
			}

			return;
		} else {
			current.ongoing.continueToBeActive(convertTime(_t, current.to), current.promiseTo, true);
		}
		return;

	}

	protected void didNext(At current) {
	}

	abstract protected void finished();

	abstract protected iVisualElement getNext(At current2);

	abstract protected Object ongoingObject(At last, iVisualElement next);

	abstract protected void updateDuration(At of);

	protected void willNext(At current) {
	}

	protected Promise wrapPromise(Promise promiseForKey) {
		return promiseForKey;
	}
}
