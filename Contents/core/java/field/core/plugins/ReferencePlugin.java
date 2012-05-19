package field.core.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;

import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.Ref;
import field.core.plugins.drawing.ConnectiveThickArc2;
import field.core.plugins.drawing.SplineComputingOverride;
import field.core.plugins.python.PythonPlugin;
import field.core.plugins.python.PythonPluginEditor;
import field.core.windowing.components.PlainComponent;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.iComponent;
import field.core.windowing.components.SelectionGroup.iSelectionChanged;
import field.launch.iUpdateable;
import field.math.abstraction.iFloatProvider;
import field.math.abstraction.iProvider;
import field.math.graph.NodeImpl;
import field.math.graph.iMutableContainer;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Pair;
import field.namespace.generic.Generics.Triple;

@Deprecated 
public class ReferencePlugin implements iPlugin {

	public class LocalVisualElement extends NodeImpl<iVisualElement> implements iVisualElement {

		public <T> void deleteProperty(VisualElementProperty<T> p) {
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
		public <T> VisitCode setProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> to) {

			// this doesn't work if we're a peer, not a child of the python plugin
			// if (prop.getName().equals(PythonPlugin.python_source.getName()))
			{
				if (debug)
					System.out.println("REF: python source changed");

				// why was this in?
				// if (!seenBefore.containsKey(source))
				{
					seenBefore.put(source, null);
					updateReferencesFor(source);
				}

				if (to.get() instanceof String)
					verifyAllReferences(source);
			}
			if (prop.getName().startsWith("__minimalReference")) {
				updateReferencesFor(source);
			}
			return super.setProperty(source, prop, to);
		}
	}

	static public boolean debug = false;

	private iVisualElement root;

	private Overrides elementOverride;

	private LocalVisualElement lve;

	private SelectionGroup<iComponent> group;

	final protected String pluginId = "//reference_plugin";

	Set<iVisualElement> currentSelection = new HashSet<iVisualElement>();

	WeakHashMap<iVisualElement, String> seenBefore = new WeakHashMap<iVisualElement, String>();

	Map<Object, Object> properties = new HashMap<Object, Object>();

	public void close() {
	}

	public Object getPersistanceInformation() {
		return new Pair<String, Object>("version_1", null);
	}

	public iVisualElement getWellKnownVisualElement(String id) {
		if (id.equals(pluginId))
			return lve;
		return null;
	}

	public void registeredWith(iVisualElement root) {
		this.root = root;
		lve = new LocalVisualElement();

		elementOverride = createElementOverrides();
		// root.addChild(lve);

		Ref<PythonPlugin> ref = new Ref<PythonPlugin>(null);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(root).getProperty(root, PythonPlugin.python_plugin, ref);

		// lve.addChild(ref.get().getWellKnownVisualElement(PythonPlugin.pluginId));
		(ref.get().getWellKnownVisualElement(PythonPlugin.pluginId)).addChild(lve);

		group = root.getProperty(iVisualElement.selectionGroup);
		group.registerNotification(new iSelectionChanged<iComponent>() {

			public void selectionChanged(Set<iComponent> selected) {
				currentSelection.clear();
				for (iComponent c : selected) {
					iVisualElement cc = c.getVisualElement();
					if (cc != null)
						currentSelection.add(cc);
				}
			}
		});

	}

	public void setPersistanceInformation(Object o) {

		// should iterate over everything and call updatereferences and verify all references

	}

	public void update() {
	}

	private Overrides createElementOverrides() {
		return new Overrides();
	}

	private void ensureConnection(String propertyName, final iVisualElement source, List<iVisualElement> connectedTo) {
		Triple<VisualElement, PlainComponent, SplineComputingOverride> created = VisualElement.createWithToken("reflected:" + propertyName, root, new Rect(0, 0, 0, 0), VisualElement.class, PlainComponent.class, SplineComputingOverride.class);

		if (debug)
			System.out.println("REF: adding connection <" + source + "> <" + connectedTo + ">");
		// these must be cached once they are working

		List<iUpdateable> instructions = new ArrayList<iUpdateable>();
		for (final iVisualElement v : connectedTo) {

			if (debug)
				System.out.println(" connecting to <" + v + "> from <" + source + ">");

			ConnectiveThickArc2 con = new ConnectiveThickArc2(created.left, new iProvider.Constant<Vector4>(new Vector4(0.0f, 0.0f, 0.0f, 0.15f)), new iProvider.Constant<Vector4>(new Vector4(0, 0, 0, 0.5f)), source, new iFloatProvider.Constant(10), v, new iFloatProvider.Constant(0));
			con.addGate(new iFloatProvider() {
				public float evaluate() {
					return currentSelection.contains(v) || currentSelection.contains(source) ? 1 : 0;
				}
			});
		}

		created.left.setProperty(SplineComputingOverride.computed_drawingInstructions, new ArrayList<iUpdateable>(instructions));
		created.left.setProperty(iVisualElement.doNotSave, true);
	}

	private int indexOf(List<String> text, String name) {
		for (String q : text) {
			if (text.indexOf(name) != -1)
				return text.indexOf(name);
		}
		return -1;
	}

	private void removeConnection(String name, iVisualElement source) {
		if (debug)
			System.out.println("REF: deleting reference marker");
		VisualElement.deleteWithToken("reflected:" + name, root);
	}

	protected void updateReferencesFor(iVisualElement source) {

		if (debug)
			System.out.println("REF: update references for <" + source + ">");

		Map<Object, Object> allProperties = source.payload();
		for (Entry<Object, Object> o : new HashMap<Object, Object>(allProperties).entrySet()) {
			if (o.getKey() instanceof VisualElementProperty) {
				if (((VisualElementProperty) o.getKey()).getName().startsWith("__minimalReference")) {
					if (debug)
						System.out.println("REF: got property <" + o + ">");
					if (o.getValue() instanceof List) {
						List<iVisualElement> connectedTo = (List<iVisualElement>) o.getValue();
						if (connectedTo != null) {
							if (debug)
								System.out.println("REF: ensuring connection");
							ensureConnection(((VisualElementProperty) o.getKey()).getName(), source, connectedTo);
						}
					}
				}
			}
		}
	}

	protected void verifyAllReferences(iVisualElement source) {

		List<String> texts = new ArrayList<String>();

		for (VisualElementProperty p : PythonPluginEditor.knownPythonProperties.values()) {
			String pp = (String) source.getProperty(p);
			if (pp != null)
				texts.add(pp);
		}

		verifyReferencesFor(source, texts);
	}
	protected void verifyReferencesFor(iVisualElement source, List<String> text) {

		if (debug)
			System.out.println("REF: verify references for <" + source + "> with text <" + text + ">");

		Map<Object, Object> allProperties = source.payload();
		for (Entry<Object, Object> o : new HashMap<Object, Object>(allProperties).entrySet()) {
			if (o.getKey() instanceof VisualElementProperty) {
				if (((VisualElementProperty) o.getKey()).getName().startsWith("__minimalReference") && !((VisualElementProperty) o.getKey()).getName().endsWith("source")) {

					if (debug)
						System.out.println(" checking <" + o + "> for in name <" + text + ">");

					if (indexOf(text, ((VisualElementProperty) o.getKey()).getName()) == -1) {
						removeConnection(((VisualElementProperty) o.getKey()).getName(), source);
						source.deleteProperty((VisualElementProperty) o.getKey());
						source.deleteProperty(new VisualElementProperty(((VisualElementProperty) o.getKey()).getName() + "-source"));
					}
				}
			}
		}

	}

}
