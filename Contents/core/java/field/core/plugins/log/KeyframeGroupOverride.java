package field.core.plugins.log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.python.core.PyObject;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.execution.PythonInterface;
import field.core.execution.iExecutesPromise;
import field.core.execution.PythonScriptingSystem.Promise;
import field.core.plugins.GroupOverride;
import field.core.plugins.drawing.SplineComputingOverride;
import field.core.plugins.log.AssemblingLogging.Move;
import field.core.plugins.log.AssemblingLogging.PartiallyEvaluatedFunction;
import field.core.plugins.log.AssemblingLogging.SimpleChange;
import field.core.plugins.log.AssemblingLogging.iBlendSupport;
import field.core.plugins.python.PythonPlugin;
import field.core.plugins.python.PythonPluginEditor;
import field.core.ui.text.BaseTextEditor2;
import field.core.ui.text.TokenMaker;
import field.core.ui.text.BaseTextEditor2.Completion;
import field.core.ui.text.PythonTextEditor.EditorExecutionInterface;
import field.math.abstraction.iFloatProvider;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.util.Dict;

/**
 * exposes a special element inside the attributes _a.kf such that this becomes
 * a keyframe group, draws interpolation lines inside the group as well
 * 
 * needs to prevent the child frames from being casually executed ? yes at least
 * by time sliders
 */
public class KeyframeGroupOverride extends GroupOverride {

	static public class ChangeSet {
		String target;

		LinkedHashMap<Object, SimpleChange> changes = new LinkedHashMap<Object, SimpleChange>();

		public ChangeSet(String t) {
			target = t;
		}

		public iBlendSupport getBlendSupport(AssemblingLogging assemblingLogging) {
			Object v = changes.values().iterator().next().getValue();

			return assemblingLogging.getBlendFor(v);
		}

		public Dict propertiesFor(SimpleChange s) {
			if (s.value instanceof PartiallyEvaluatedFunction) {
				return ((PartiallyEvaluatedFunction) s.value).attributes;
			}
			return null;
		}

		public Object read() {
			assert changes.size() > 0;
			SimpleChange s = changes.values().iterator().next();
			return s.previousValue;
		}

		public void write(Object value) {

			;//System.out.println(" writing <" + value + "> to <" + target + ">");

			assert changes.size() > 0;
			SimpleChange s = changes.values().iterator().next();

			;//System.out.println(" simple change is <" + s + "> of class <" + s.getClass() + ">");

			s.writeChange(value);
		}

		public void write(Object value, float amount) {
			assert changes.size() > 0;
			SimpleChange s = changes.values().iterator().next();

			s.writeChange(value, amount);
		}
	}

	// a no-op group promise execution
	public class GroupPromiseExecution implements iExecutesPromise {

		public void addActive(iFloatProvider timeProvider, Promise p) {
		}

		public void removeActive(Promise p) {
		}

		public void stopAll(float t) {
		}


	}

	public class InterpolationFrame {
		LinkedHashMap<String, ChangeSet> changes = new LinkedHashMap();

		public float getPositionFor(iVisualElement e, float x, Position p) {
			float pp = KeyframeGroupOverride.this.getPositionFor(e, x, p);
			return pp;
		}

		public int indexOf(List<iVisualElement> e, iVisualElement q) {
			for (int i = 0; i < e.size(); i++) {
				if (e.get(i) == q)
					return i;
			}
			return -1;
		}

		public float weight(iVisualElement e, float x, Position p) {

			List<iVisualElement> m = sortedMembers(p);
			int at = indexOf(m, e);

			float center = getPositionFor(e, x, p);

			if (at == 0 && x < center)
				return 1;
			if (at == m.size() - 1 && x > center)
				return 1;
			if (x == center)
				return 1;

			if (x < center) {
				float other = getPositionFor(m.get(at - 1), x, p);
				float d = center - other;
				if (d == 0)
					return 0.5f;
				return Math.max(0, (x - other) / d);
			} else {
				float other = getPositionFor(m.get(at + 1), x, p);
				float d = other - center;
				if (d == 0)
					return 0.5f;
				return Math.max(0, (other - x) / d);
			}
		}

	}

	public enum Position {
		start, middle, stop, startstop;
	}

	static public final VisualElementProperty<Boolean> keyframeGroupDisabled = new VisualElementProperty<Boolean>("keyframeGroupDisabled");

	static public final VisualElementProperty<PyObject> pythonKeyframeHelp = new VisualElementProperty<PyObject>("kf_");

	// transient HashMap<iVisualElement, PyObject> keyframeHelp = new
	// HashMap<iVisualElement, PyObject>();

	// this has to be shared as well as the keyframehelper

	static public final VisualElementProperty<KeyframeGroupOverride> keyframeGroup = new VisualElementProperty<KeyframeGroupOverride>("kfGroup_");

	static public LinkedHashMap<String, ChangeSet> getAllChanges(Map<Object, ArrayList<SimpleChange>> changes) {
		LinkedHashMap<String, ChangeSet> c = new LinkedHashMap();
		Set<Entry<Object, ArrayList<SimpleChange>>> es = changes.entrySet();
		for (Entry<Object, ArrayList<SimpleChange>> e : es) {
			ArrayList<SimpleChange> aa = e.getValue();
			for (SimpleChange s : aa) {
				String t = s.target;
				ChangeSet cs = c.get(t);
				if (cs == null) {
					c.put(t, cs = new ChangeSet(t));
				}
				cs.changes.put(e.getKey(), s);
				s.update();
			}
		}
		return c;
	}

	transient boolean inside = false;

	transient HashMap<iVisualElement, ArrayList<SimpleChange>> changeSets = new HashMap<iVisualElement, ArrayList<SimpleChange>>();

	transient HashSet<iVisualElement> clean = new HashSet<iVisualElement>();

	transient AssemblingLogging assemblingLogging = new AssemblingLogging();

	public HashMap<iVisualElement, ArrayList<SimpleChange>> getChanges() {
		HashMap<iVisualElement, ArrayList<SimpleChange>> ret = new HashMap<iVisualElement, ArrayList<SimpleChange>>();

		for (iVisualElement e : (Collection<iVisualElement>) forElement.getParents()) {
			if (clean.contains(e)) {
				ret.put(e, changeSets.get(e));
			} else {
				clean.add(e);
				changeSets.put(e, new ArrayList<SimpleChange>());

				ArrayList<Move> m = assemblingLogging.resetReturningMoves();

				if (m.size() != 0) {
					for (int i = 0; i < m.size(); i++) {
						Move move = m.get(m.size() - 1 - i);
						assemblingLogging.new MoveEvent("", move).executeSimpleUndo();
					}
				}

				executeChildGroup(e);

				ArrayList<SimpleChange> res = assemblingLogging.getSimpleChangeSet(m = assemblingLogging.resetReturningMoves());
				changeSets.put(e, res);
				ret.put(e, res);

				if (m.size() != 0) {
					for (int i = 0; i < m.size(); i++) {
						Move move = m.get(m.size() - 1 - i);
						assemblingLogging.new MoveEvent("", move).executeSimpleUndo();
					}
				}
			}
		}

		return ret;
	}

	public InterpolationFrame getInterpolationFrame(HashMap<Object, ArrayList<SimpleChange>> changes) {
		InterpolationFrame frame = new InterpolationFrame();

		frame.changes = getAllChanges(changes);

		return frame;
	}

	public float getPositionFor(iVisualElement e, float x, Position p) {
		List<iVisualElement> s = sortedMembers(p);
		if (s.size() < 2)
			return getRawPositionFor(e, x, p);
		float minA = (float) (s.get(0).getFrame(null).x - 5);
		Rect end = s.get(s.size() - 1).getFrame(null);
		float maxA = (float) (end.x + end.w + 5);

		float minB = getRawPositionFor(s.get(0), x, p);
		float maxB = getRawPositionFor(s.get(s.size() - 1), x, p);

		float a = minA + (maxA - minA) * (getRawPositionFor(e, x, p) - minB) / (maxB - minB);
		return a;
	}

	@Override
	public <T> VisitCode getProperty(final iVisualElement source, VisualElementProperty<T> prop, Ref<T> ref) {

		if (isChild(source)) {

			if (prop == iExecutesPromise.promiseExecution) {
				if (inside)
					return super.getProperty(source, prop, ref);
				ref.set((T) new GroupPromiseExecution());
				return VisitCode.cont;
			} else if (prop == PythonPluginEditor.editorExecutionInterface) {

				final EditorExecutionInterface delegate = (EditorExecutionInterface) ref.get();
				assert delegate != null;

				ref.set((T) new EditorExecutionInterface() {

					public void executeFragment(String fragment) {

						if (automaticallyExecuteAll(source)) {
							clean.remove(source);
							executeThis();
						} else
							delegate.executeFragment(fragment);
					}

					public Object executeReturningValue(String string) {
						Object r = delegate.executeReturningValue(string);
						// executeThis();
						return r;
					}
					
					@Override
					public boolean globalCompletionHook(String leftText, boolean publicOnly, ArrayList<Completion> comp, BaseTextEditor2 inside) {
						return false;
					}

					@Override
					public TokenMaker getCustomTokenMaker() {
						return delegate.getCustomTokenMaker();
					}
				});

			} else if (prop.equals(pythonKeyframeHelp)) {
				if (ref.get() == null) {
					PyObject helper = ensureHelper(source);
					ref.set((T) helper);
				}

			}
		}

		if (isChild(source) || source == forElement)
			if (prop.equals(keyframeGroup)) {
				ref.set((T) this);
			}

		return super.getProperty(source, prop, ref);
	}

	public float getRawPositionFor(iVisualElement e, float x, Position p) {
		Rect f = e.getFrame(null);
		if (p == Position.start)
			return (float) f.x;
		if (p == Position.stop)
			return (float) (f.x + f.w);
		if (p == Position.middle)
			return (float) (f.x + f.w / 2);
		if (p == Position.startstop) {
			if (x < f.x)
				return (float) f.x;
			if (x > f.x + f.w)
				return (float) (f.x + f.w);
			return x;
		}
		assert false : p;
		return 0;
	}

	@Override
	public <T> VisitCode setProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> to) {
		if (isChild(source) && prop.equals(PythonPlugin.python_source)) {
			clean.remove(source);
		}
		return super.setProperty(source, prop, to);
	}

	@Override
	public VisitCode shouldChangeFrame(iVisualElement source, Rect newFrame, Rect oldFrame, boolean now) {
		if (!newFrame.equals(oldFrame)) {
			if (isChild(source)) {
				SplineComputingOverride.fireChange(forElement);
			}
		}
		return super.shouldChangeFrame(source, newFrame, oldFrame, now);
	}

	public List<iVisualElement> sortedMembers(final Position p) {
		List<iVisualElement> c = new ArrayList<iVisualElement>((List<iVisualElement>) forElement.getParents());
		Collections.sort(c, new Comparator<iVisualElement>() {

			public int compare(iVisualElement o1, iVisualElement o2) {
				float p1 = getRawPositionFor(o1, o1.getFrame(null).midPoint().x, p);
				float p2 = getRawPositionFor(o2, o2.getFrame(null).midPoint().x, p);
				return Float.compare(p1, p2);
			}
		});
		return c;
	}

	private PyObject ensureHelper(iVisualElement source) {
		PyObject k = source.getProperty(pythonKeyframeHelp);
		if (k == null) {
			k = newHelper(source);
			source.setProperty(pythonKeyframeHelp, k);
		}
		return k;
	}

	private void executeChildGroup(iVisualElement e) {
		inside = true;
		try {
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(e).beginExecution(e);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(e).endExecution(e);
		} finally {
			inside = false;
		}
	}

	private boolean isChild(iVisualElement source) {
		return forElement.getParents().contains(source);
	}

	private PyObject newHelper(iVisualElement source) {
		PythonInterface.getPythonInterface().setVariable("__x0", source);
		PythonInterface.getPythonInterface().setVariable("__x1", assemblingLogging);
		PyObject object = PythonInterface.getPythonInterface().executeStringReturnPyObject("__x = KeyframeGroupOverrideHelper(__x0, globals(), __x1)", "__x");
		return object;
	}

	protected boolean automaticallyExecuteAll(iVisualElement source) {
		return true;
	}

	protected void executeThis() {
		inside = true;
		try {
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).beginExecution(forElement);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).endExecution(forElement);
		} finally {
			inside = false;
		}
	}

}
