package field.core.plugins.autoexecute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

import field.bytecode.protect.Woven;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.Ref;
import field.core.plugins.SimpleConstraints;
import field.core.plugins.iPlugin;
import field.core.plugins.drawing.SplineComputingOverride;
import field.core.plugins.python.PythonPlugin;
import field.core.plugins.python.PythonPluginEditor;
import field.launch.SystemProperties;
import field.math.graph.NodeImpl;
import field.math.graph.iMutableContainer;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.namespace.generic.Generics.Pair;

@Woven
public class AutoExecutePythonPlugin implements iPlugin {

	public class LocalVisualElement extends NodeImpl<iVisualElement> implements iVisualElement {

		public <T> void deleteProperty(VisualElementProperty<T> p) {
			properties.remove(p);
		}

		public void dispose() {
		}

		public Rect getFrame(Rect out) {
			return null;
		}

		public <T> T getProperty(iVisualElement.VisualElementProperty<T> p) {
			if (p == overrides)
				return (T) elementOverride;
			Object o = properties.get(p);
			return (T) o;
		}

		public String getUniqueID() {
			return pluginId;
		}

		public Map<Object, Object> payload() {
			return properties;
		}

		public void setFrame(Rect out) {
		}

		public iMutableContainer<Map<Object, Object>, iVisualElement> setPayload(Map<Object, Object> t) {
			properties = t;
			return this;
		}

		public <T> iVisualElement setProperty(iVisualElement.VisualElementProperty<T> p, T to) {
			properties.put(p, to);
			return this;
		}

		public void setUniqueID(String uid) {
		}
	}

	public class Overrides extends iVisualElementOverrides.Adaptor {

		@Override
		public VisitCode added(iVisualElement newSource) {

			System.out.println(" added <" + newSource + ">");

			check(newSource);
			return VisitCode.cont;
		}

	}

	static public final VisualElementProperty<String> python_autoExec = new VisualElementProperty<String>("python_autoExec_v");
	static public final VisualElementProperty<Integer> autoExecuteDelay = new VisualElementProperty<Integer>("autoExecuteDelay");
	static public final VisualElementProperty<Integer> autoExecuteDelayedFor = new VisualElementProperty<Integer>("autoExecuteDelayedFor_");

	static public final String pluginId = "//AudoExecutePython";

	private iVisualElement root;

	private SimpleConstraints simpleConstraintsPlugin;

	private iVisualElementOverrides elementOverride;

	private PythonPlugin pythonPlugin;

	protected LocalVisualElement lve;

	Map<Object, Object> properties = new HashMap<Object, Object>();

	Pattern c = Pattern.compile("#--\\{[^\\{]*?auto.*?\\}.*?$", Pattern.MULTILINE);

	public AutoExecutePythonPlugin() {
		lve = new LocalVisualElement();
	}

	public void autoExecute(final iVisualElement newSource, final String string) {
		assert false;
	}

	TreeSet<iVisualElement> elements = new TreeSet<iVisualElement>(new Comparator<iVisualElement>() {

		public int compare(iVisualElement o1, iVisualElement o2) {
			Rect r1 = o1.getFrame(new Rect());
			Rect r2 = o2.getFrame(new Rect());
			int c = Double.compare(r1.x, r2.x);
			return c == 0 ? Double.compare(System.identityHashCode(o1), System.identityHashCode(o2)) : c;
		}
	});

	public void check(iVisualElement newSource) {
		elements.add(newSource);
	}

	public void perform(iVisualElement newSource) {
		// pull auto execute information from the properties

		Ref<String> ref = new Ref<String>("");
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(newSource).getProperty(newSource, PythonPlugin.python_source_forExecution, ref);

		String[] s = c.split(ref.get());

		if (s.length > 1) {
			autoExecute(newSource, s[1]);
		}

		String q = python_autoExec.get(newSource);
		if (q != null) {
			System.out.println(" about to auto exec for <" + newSource + ">");
			SplineComputingOverride.executePropertyOfElement(python_autoExec, newSource);
		}

	}

	public void close() {
	}

	public Object getPersistanceInformation() {
		return new Pair<String, Object>(pluginId + "version_0", null);
	}

	public iVisualElement getWellKnownVisualElement(String id) {
		if (id.equals(pluginId))
			return lve;
		return null;
	}

	public void registeredWith(iVisualElement root) {

		PythonPluginEditor.knownPythonProperties.put("Automatically Executed", python_autoExec);

		this.root = root;

		pythonPlugin = PythonPlugin.python_plugin.get(root);

		root.addChild(lve);
		elementOverride = createElementOverrides();
	}

	public void setPersistanceInformation(Object o) {
	}

	boolean noAuto = SystemProperties.getIntProperty("noAuto", 0) == 1;

	public void update() {

		if (noAuto)
			elements.clear();

		if (elements.size() > 0) {
			ArrayList<iVisualElement> a1 = new ArrayList<iVisualElement>(elements);
			ArrayList<iVisualElement> readd = new ArrayList<iVisualElement>();

			Collections.sort(a1, new Comparator<iVisualElement>() {

				public int compare(iVisualElement o1, iVisualElement o2) {
					Rect f1 = o1.getFrame(null);
					Rect f2 = o2.getFrame(null);
					
					int c = Double.compare(f1.x, f2.x);
					return c == 0 ? Double.compare(f1.y, f2.y) : c;
				}
			});

			System.out.println(" about to exec in this order :" + a1);

			for (iVisualElement e : a1) {
				Number m = e.getProperty(autoExecuteDelay);
				if (m != null && m.intValue() > 0) {
					Integer soFar = e.getProperty(autoExecuteDelayedFor);
					if (soFar == null)
						soFar = 0;
					
					System.out.println(" needs delay of <"+m+"> has delay of <"+soFar+">");
					
					if (soFar >= m.intValue()) {
						perform(e);
					} else {
						e.setProperty(autoExecuteDelayedFor, ++soFar);
						readd.add(e);
						System.out.println(" readding <"+e+">");
					}
				} else
					perform(e);
			}

			// note that this is deliberately late \u2014 elements that
			// are added during somebody else's perform are not
			// autoexec'd
			elements.clear();
			elements.addAll(readd);
		}
	}

	protected iVisualElementOverrides createElementOverrides() {
		return new Overrides();
	}
}
