package field.extras.max;

import java.util.ArrayList;
import java.util.List;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.execution.BasicRunner;
import field.core.execution.PythonScriptingSystem;
import field.core.execution.PythonScriptingSystem.Promise;
import field.core.execution.TemporalSliderOverrides;
import field.core.execution.TimeSystem;
import field.core.execution.iExecutesPromise;
import field.core.network.OSCInput.DispatchableHandler;
import field.core.plugins.python.PythonPlugin.LocalPromise;
import field.core.ui.text.BaseTextEditor2;
import field.core.ui.text.BaseTextEditor2.Completion;
import field.core.ui.text.PythonTextEditor.EditorExecutionInterface;
import field.core.ui.text.PythonTextEditor.PickledCompletionInformation;
import field.core.util.LocalFuture;
import field.launch.iUpdateable;
import field.math.abstraction.iFloatProvider;

public class MaxExecution implements iUpdateable {

	static public final VisualElementProperty<List<String>> maxBox = new VisualElementProperty<List<String>>("maxBox");

	private MaxPlugin inside;
	private iExecutesPromise ep;

	BasicRunner runner;

	public MaxExecution(MaxPlugin maxPlugin) {
		this.inside = maxPlugin;

		ep = new iExecutesPromise() {
			public void addActive(iFloatProvider timeProvider, Promise p) {
				;// ;//System.out.println("add active : " +
					// ((LocalPromise)
					// p).element+"  "+p.getText());

				executeFragment(((LocalPromise) p).element, p.getText(), null);
			}

			public void removeActive(Promise p) {
			}

			public void stopAll(float t) {
			}
		};

		runner = new BasicRunner(PythonScriptingSystem.pythonScriptingSystem.get(maxPlugin.getRoot()), 0) {
			@Override
			protected boolean filter(Promise p) {
				iVisualElement v = (iVisualElement) system.keyForPromise(p);

				if (v == null)
					return false;

				iExecutesPromise ep = iExecutesPromise.promiseExecution.get(v);
				return ep == MaxExecution.this.ep;
			}

			protected Delegate delegateForPromise(float t, Promise p, boolean forwards) {
				return new Delegate() {

					@Override
					public void continueToBeActive(float t, Promise p, boolean forwards) {
						// TODO Auto-generated method
						// stub

					}

					@Override
					public void jumpStop(float t, Promise p, boolean forwards) {
						// TODO Auto-generated method
						// stub

					}

					@Override
					public void start(float t, Promise p, boolean forwards) {
						executeFragment(((LocalPromise) p).element, p.getText(), null);
					}

					@Override
					public void startAndStop(float t, Promise p, boolean forwards) {
						// TODO Auto-generated method
						// stub

					}

					@Override
					public void stop(float t, Promise p, boolean forwards) {
						// TODO Auto-generated method
						// stub

					}

				};
			};

		};

	}

	public EditorExecutionInterface getEditorExecutionInterface(final iVisualElement e, final EditorExecutionInterface delegateTo) {
		return new EditorExecutionInterface() {

			public void executeFragment(String fragment) {
				MaxExecution.this.executeFragment(e, fragment, delegateTo);
			}

			public Object executeReturningValue(String string) {
				LocalFuture lf = new LocalFuture();
				MaxExecution.this.executeReturningValue(e, string, delegateTo, lf);
				return lf;
			}

			@Override
			public boolean globalCompletionHook(String leftText, boolean publicOnly, ArrayList<Completion> comp, BaseTextEditor2 inside) {
				return false;
			}

		};
	}

	int talkback = 0;

	protected void executeReturningValue(iVisualElement e, String fragment, EditorExecutionInterface delegateTo, final LocalFuture lf) {

		String command = "evalxvalue(_, r\"\"\"" + fragment + "\"\"\", \"/return/" + (talkback) + "\")";

		inside.in.registerHandler("/return/" + talkback, new DispatchableHandler() {

			public void handle(String s, Object[] args) {

				// todo, deregister

				String ss = (String) args[0];


				Object r = field.core.execution.PythonInterface.getPythonInterface().eval("cPickle.loads(r\"\"\"" + ss + "\"\"\")");

				System.out.println(" unpickled and got <" + r+ ">");

				lf.set(new PickledCompletionInformation((List) r));
			}
		});

		executeFragment(e, command, delegateTo);

		talkback++;
	}

	protected void executeFragment(iVisualElement e, String fragment, EditorExecutionInterface delegateTo) {

		;// ;//System.out.println(" execute fragment <" + fragment +
			// ">");

		// todo
		List<String> target = targetsFor(e);
		if (target.size() == 0) {
			// PythonInterface.getPythonInterface().printError("Warning: python exected nowhere --- set the 'maxBox' property to be the name of the box in Max that you want to send code to for execution");
			target.add(iVisualElement.name.get(e));
		}
		for (String t : target)
			inside.out.simpleSend("/message/" + t, fragment);

	}

	private List<String> targetsFor(iVisualElement e) {
		List m = maxBox.accumulateList(e);
		List<String> mm = new ArrayList<String>();
		for (Object o : m) {
			if (o instanceof List)
				mm.addAll(((List<String>) o));
			else
				mm.add((String) o);
		}
		return mm;
	}

	public iExecutesPromise getExecutesPromise(iVisualElement e, iExecutesPromise delegateTo) {
		return ep;
	}

	public void update() {

		TimeSystem t = TemporalSliderOverrides.currentTimeSystem.get(inside.getRoot());
		runner.update(t == null ? 0 : (float) t.evaluate());

	}

}
