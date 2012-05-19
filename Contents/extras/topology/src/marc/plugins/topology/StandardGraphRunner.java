package marc.plugins.topology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.python.core.PyObject;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.Yield;
import field.bytecode.protect.yield.YieldUtilities;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.execution.PythonInterface;
import field.core.execution.PythonScriptingSystem.Promise;
import field.core.plugins.python.PythonPlugin.CapturedEnvironment;
import field.util.PythonUtils;

/*
 * conventionalizes a GraphRunner
 */
public class StandardGraphRunner extends GraphRunner {

	static public final VisualElementProperty<Object> rGraph = new VisualElementProperty<Object>("rGraph_");
	static public final VisualElementProperty<Object> rDuration = new VisualElementProperty<Object>("rDuration_");

	public int defaultDuration = 100;

	public boolean hasFinished = true;
	Stack<iVisualElement> path = new Stack<iVisualElement>();

	iVisualElement lastTarget;

	public void beginPath(List<iVisualElement> element) {
		beginPath(element, true);
	}

	public void beginPath(List<iVisualElement> element, boolean trim) {
		if (element.size() > 0) {
			ArrayList<iVisualElement> r = new ArrayList<iVisualElement>(element);
			if (trim && lastTarget != null && element.get(0).equals(lastTarget)) {
				r.remove(0);
			}
			if (r.size()==0) return;

			Collections.reverse(r);
			final boolean restartRequired = current == null;


			out.println(" begin path <" + r + ">");
			path.clear();
			path.addAll(r);

			q.new Task() {
				@Override
				@Woven
				@Yield
				protected void run() {
					if (restartRequired) {

						willNext(current);
						while (!canNext(current)) {
							recur();
							YieldUtilities.yield(null);
						}
						didNext(current);

						out.println("\n\n" + this + " restart required\n\n");

						if (hasFinished) {
							iVisualElement nn = getNext(null);
							consumeNext(null);
							begin(nn);
							hasFinished = false;
						}
					} else {

					}
				}
			};
		}
	}

	public iVisualElement getLastTarget() {
		return lastTarget;
	}

	@Override
	protected void consumeNext(At current2) {
		iVisualElement popped = path.pop();
		lastTarget = popped;
	}

	@Override
	protected void finished() {
		hasFinished = true;
	}

	@Override
	protected iVisualElement getNext(At current2) {
		if (path.isEmpty())
			return null;
		return path.peek();
	}

	protected Object getState() {
		return null;
	}

	@Override
	protected Object ongoingObject(At last, iVisualElement next) {
		return rGraph.get(next);
	}

	@Override
	protected void updateDuration(At of) {
		Object o = of.to.getProperty(rDuration);
		if (o == null) {
			if (of.duration == 0)
				of.duration = defaultDuration;
			return;
		}

		Number n = new PythonUtils().toNumber(o);
		if (n != null) {
			of.duration = n.intValue();
		} else {
			if (of.duration == 0)
				of.duration = defaultDuration;
		}
	}

	@Override
	protected Promise wrapPromise(Promise promiseForKey) {
		final Promise p = super.wrapPromise(promiseForKey);
		Promise r = new Promise() {
			Promise pp = p;

			public void beginExecute() {
				pp.beginExecute();
				PythonInterface.getPythonInterface().setVariable("_state", getState());
			}

			public void endExecute() {
				pp.endExecute();
			}

			public PyObject getAttributes() {
				return pp.getAttributes();
			}

			public float getEnd() {
				return pp.getEnd();
			}

			public Stack<CapturedEnvironment> getOngoingEnvironments() {
				return pp.getOngoingEnvironments();
			}

			public float getPriority() {
				return pp.getPriority();
			}

			public float getStart() {
				return pp.getStart();
			}

			public String getText() {
				return pp.getText();
			}

			public void willExecute() {
				pp.willExecute();
			}

			public void willExecuteSubstring(String actualSubstring, int start, int end) {
				pp.willExecuteSubstring(actualSubstring, start, end);
			}

			public void wontExecute() {
				pp.wontExecute();
			}
			
			public boolean isPaused() {
				return pp.isPaused();
			}
		};
		return r;
	}

}
