package field.core.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.plugins.help.ContextualHelp;
import field.core.plugins.help.HelpBrowser;
import field.core.plugins.python.PythonPlugin;
import field.core.plugins.python.PythonPluginEditor;
import field.core.ui.NewInspector2;
import field.core.ui.NewInspector2.BaseControl;
import field.core.ui.NewInspector2.BooleanControl;
import field.core.ui.NewInspector2.ColorControl;
import field.core.ui.NewInspector2.SpinnerControl;
import field.core.ui.NewInspectorFromProperties;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.SelectionGroup.iSelectionChanged;
import field.core.windowing.components.iComponent;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.abstraction.iProvider;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.graph.NodeImpl;
import field.math.graph.iMutableContainer;
import field.namespace.generic.Generics.Triple;

@Woven
public class NewInspectorPlugin implements iPlugin {

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
		public <T> VisitCode getProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> ref) {
			if (prop.equals(inspectorPlugin)) {
				ref.set((T) NewInspectorPlugin.this);
			}
			return super.getProperty(source, prop, ref);
		}

		@Override
		public <T> VisitCode setProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> to) {
			if (source == currentInspection && !prop.equals(PythonPlugin.python_areas) && !prop.equals(PythonPlugin.python_source) && !prop.equals(PythonPluginEditor.python_customInsertPersistanceInfo)) {
				needsInspection = 30;
				// live update is commented out right now for
				// performance reasons
				// needsInspection = 0;
			}

			if (prop.equals(iVisualElement.name)) {
				source.setProperty(iVisualElement.dirty, true);
			}
			return super.setProperty(source, prop, to);
		}

	}

	public static HashMap<String, String> inspectableProperties = new HashMap<String, String>();

	static public final VisualElementProperty<NewInspectorPlugin> inspectorPlugin = new VisualElementProperty<NewInspectorPlugin>("inspectorPlugin_");

	static public void addInspectableProperty(String propertyName, String displayName) {
		inspectableProperties.put(propertyName, displayName);
	}

	private NewInspector2 inspector;
	private NewInspectorFromProperties helper;

	private SelectionGroup<iComponent> group;

	private iVisualElement currentInspection;

	final protected String pluginId = "//inspector_python";

	protected LocalVisualElement lve;

	protected iVisualElement root;

	protected Overrides elementOverride;

	boolean[] ex = new boolean[0];

	int needsInspection = 0;

	Map<Object, Object> properties = new HashMap<Object, Object>();

	public NewInspectorPlugin() {
		lve = new LocalVisualElement();

		// TODO swt color well
		{
			LinkedHashMap<String, Class<? extends BaseControl>> decorationSet = new LinkedHashMap<String, Class<? extends BaseControl>>();
			decorationSet.put("color1", ColorControl.class);
			decorationSet.put("color2", ColorControl.class);
			decorationSet.put("isWindowSpace", BooleanControl.class);

			helper.activeSets.add(new Triple<String, LinkedHashMap<String, Class<? extends BaseControl>>, Boolean>("Decoration", decorationSet, false));
		}
		{
			LinkedHashMap<String, Class<? extends BaseControl>> decorationSet = new LinkedHashMap<String, Class<? extends BaseControl>>();
			decorationSet.put("autoExecuteDelay", SpinnerControl.class);

			helper.activeSets.add(new Triple<String, LinkedHashMap<String, Class<? extends BaseControl>>, Boolean>("Execution (advanced)", decorationSet, false));
		}
	}

	public void close() {
	}

	public String formatName(String name) {

		if (inspectableProperties.containsKey(name))
			return inspectableProperties.get(name);

		int li = name.lastIndexOf("_");
		if (li == -1)
			return name;
		if (li < name.length() - 3)
			return name;
		return "<b>" + name.substring(0, li + 1) + "</b><font size=-3><i>" + name.substring(li + 1) + "</i>";
	}

	public Object getPersistanceInformation() {
		return null;
	}

	public iVisualElement getWellKnownVisualElement(String id) {
		if (id.equals(pluginId))
			return lve;
		return null;
	}

	public void registeredWith(final iVisualElement root) {
		this.root = root;

		inspector = new NewInspector2() {
			protected java.util.LinkedHashMap<String, iUpdateable> getMenuItems() {
				return helper.getMenuItems(new iUpdateable() {

					@Override
					public void update() {
						changeSelection(root.getProperty(iVisualElement.selectionGroup).getSelection());
					}
				});
			};
		};

		helper = new NewInspectorFromProperties(inspector);

		// UbiquitousLinks.links.install(inspector.tree);

		elementOverride = createElementOverrides();
		root.addChild(lve);
		group = root.getProperty(iVisualElement.selectionGroup);

		group.registerNotification(new iSelectionChanged<iComponent>() {
			public void selectionChanged(Set<iComponent> selected) {
				changeSelection(selected);
			}
		});

		final GLComponentWindow window = root.getProperty(iVisualElement.enclosingFrame);

		installHelpBrowser(root);
	}

@NextUpdate(delay=3)
	private void installHelpBrowser(final iVisualElement root) {
		HelpBrowser h = HelpBrowser.helpBrowser.get(root);
		ContextualHelp ch = h.getContextualHelp();
		ch.addContextualHelpForWidget("inspector", inspector.getContents(), ch.providerForStaticMarkdownResource("contextual/inspector.md"), 50);
	}
	public void setPersistanceInformation(Object o) {
	}

	int consecutiveUpdates = 0;

	int suppressChangeSelection = 0;

	boolean updatedLast = false;

	private ArrayList<iVisualElement> sel = new ArrayList<iVisualElement>();

	public void update() {
		needsInspection--;
		if (needsInspection == 0)
			changeSelection(root.getProperty(iVisualElement.selectionGroup).getSelection());

		if (needsInspection < 0)
			needsInspection = 0;
	}

	@NextUpdate(delay = 15)
	protected void changeSelection(Set<iComponent> selected) {

		// if (FocusManager.getCurrentManager().getFocusOwner()
		// instanceof JTextField) {
		// needsInspection = 15;
		// return;
		// }

		Control focusControl = Launcher.display.getFocusControl();

		try {
			if (Launcher.display.getFocusControl().getShell() == inspector.getShell()) {
				if (focusControl instanceof Text || focusControl instanceof Spinner) {
					needsInspection = 15;
					return;
				}
			}
		} catch (NullPointerException e) {
		}
		;
		if (selected.isEmpty())
			inspector.clear();

		sel = new ArrayList<iVisualElement>();
		for (iComponent c : selected) {
			iVisualElement m = c.getVisualElement();
			if (m != null)
				sel.add(m);
		}
		inspector.clear();
		helper.rebuild(sel);

		// inspector.setMenu(helper.getMenu(new iUpdateable() {
		//
		// @Override
		// public void update() {
		// changeSelection(root.getProperty(iVisualElement.selectionGroup).getSelection());
		// }
		// }));
	}

	protected Overrides createElementOverrides() {
		return new Overrides() {
			@Override
			public <T> VisitCode setProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> to) {
				if (sel.contains(source) && !prop.equals(PythonPlugin.python_areas) && !prop.equals(PythonPlugin.python_source) && !prop.equals(PythonPluginEditor.python_customInsertPersistanceInfo)) {
					needsInspection = 30;
				}

				if (prop.equals(iVisualElement.name)) {
					source.setProperty(iVisualElement.dirty, true);
				}
				return super.setProperty(source, prop, to);
			}

			@Override
			public VisitCode shouldChangeFrame(iVisualElement source, Rect newFrame, Rect oldFrame, boolean now) {
				if (sel.contains(source))
					needsInspection = 30;
				return super.shouldChangeFrame(source, newFrame, oldFrame, now);
			}
		};
	}

}
