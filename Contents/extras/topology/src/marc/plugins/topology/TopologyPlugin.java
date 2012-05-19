package marc.plugins.topology;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import field.core.Constants;
import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.persistance.VisualElementReference;
import field.core.plugins.PluginList;
import field.core.plugins.iPlugin;
import field.core.plugins.connection.Connections;
import field.core.plugins.connection.LineDrawingOverride;
import field.core.plugins.drawing.BasicDrawingPlugin;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.plugins.drawing.opengl.Cursor;
import field.core.plugins.drawing.opengl.Intersections;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.python.PythonPlugin;
import field.core.plugins.selection.SelectionSetDriver;
import field.core.windowing.components.DraggableComponent;
import field.core.windowing.components.PlainComponent;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.iComponent;
import field.extras.plugins.hierarchy.HierarchyHandler;
import field.extras.plugins.hierarchy.HierarchyPlugin;
import field.launch.iUpdateable;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.graph.NodeImpl;
import field.math.graph.iMutableContainer;
import field.math.graph.iTopology;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Pair;
import field.namespace.generic.Generics.Triple;
import field.util.NPermuteMIterator;
import field.util.PythonUtils;

/**
 * uses connections to draw a topology or two over visual elements
 * 
 * @author marc
 * 
 */
public class TopologyPlugin implements iPlugin {

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
				return (T) TopologyPlugin.this.overrides;
			Object o = properties.get(p);

			if (p.equals(iVisualElement.name)) {
				return (T) "topology plugin";
			}

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

	public class Overrides extends iVisualElementOverrides.DefaultOverride {

		@Override
		public VisitCode menuItemsFor(final iVisualElement source, Map<String, iUpdateable> items) {
			if (source == null)
				return super.menuItemsFor(source, items);

			SelectionGroup<iComponent> markingGroup = iVisualElement.markingGroup.get(source);
			Set<iComponent> marked = markingGroup.getSelection();
			HashSet<iVisualElement> markedElements = new HashSet<iVisualElement>();
			for (iComponent c : marked) {
				iVisualElement v = c.getVisualElement();
				if (v != null && v != source)
					markedElements.add(v);
			}

			if (markedElements.size() == 1) {
				final iVisualElement v = markedElements.iterator().next();

				items.put("Topology", null);

				final iVisualElement c1 = isConnected(source, v);
				final iVisualElement c2 = isConnected(v, source);

				items.put("         \u2194 <b>" + connectText(c1) + "</b> <i>from</i> '" + describe(source) + "' <i>to</i> '" + describe(v) + "'", new iUpdateable() {

					public void update() {
						connections.connect(source, v, TopologyConnective.class, false);
					}
				});
				items.put("         \u2194 <b>" + connectText(c2) + "</b> <i>from</i> '" + describe(v) + "' <i>to</i> '" + describe(source) + "'", new iUpdateable() {

					public void update() {
						connections.connect(v, source, TopologyConnective.class, false);
					}
				});
				items.put("         \u2194 <b>Connect both ways</b> '" + describe(v) + "' <i>and</i> '" + describe(source) + "'", new iUpdateable() {

					public void update() {
						if (c2 == null)
							connections.connect(v, source, TopologyConnective.class, false);
						if (c1 == null)
							connections.connect(source, v, TopologyConnective.class, false);
					}
				});

				items.put("         \u2194 <b>Interpose 1, bi-connected </b> element between '" + describe(v) + "' <i>and</i> '" + describe(source) + "'", new iUpdateable() {

					public void update() {

						Rect rr = v.getFrame(null).blendTowards(0.5f, source.getFrame(null));
						rr.w /= 2;
						rr.h /= 2;
						iVisualElement newElement = makeNewElement(rr);
						connections.connect(v, newElement, TopologyConnective.class, false);
						connections.connect(newElement, v, TopologyConnective.class, false);
						connections.connect(source, newElement, TopologyConnective.class, false);
						connections.connect(newElement, source, TopologyConnective.class, false);
					}
				});
				items.put("         \u2194 <b>Interpose 2, bi-connected</b> elements between '" + describe(v) + "' <i>and</i> '" + describe(source) + "'", new iUpdateable() {

					public void update() {

						Rect rr1 = v.getFrame(null).blendTowards(0.3f, source.getFrame(null));
						rr1.w /= 2;
						rr1.h /= 2;
						iVisualElement newElement1 = makeNewElement(rr1);
						Rect rr2 = v.getFrame(null).blendTowards(0.6f, source.getFrame(null));
						rr2.w /= 2;
						rr2.h /= 2;
						iVisualElement newElement2 = makeNewElement(rr2);
						connections.connect(v, newElement1, TopologyConnective.class, false);
						connections.connect(newElement1, v, TopologyConnective.class, false);
						connections.connect(newElement2, newElement1, TopologyConnective.class, false);
						connections.connect(newElement1, newElement2, TopologyConnective.class, false);
						connections.connect(newElement2, source, TopologyConnective.class, false);
						connections.connect(source, newElement2, TopologyConnective.class, false);
					}
				});

				List<iVisualElement> existing = getExistingConnections(v, source);
				for (final iVisualElement ex : existing) {
					items.put("         \u2194 <b>Interpose</b> inside '" + describe(ex) + "' new element from '" + describe(source) + "' <i>to</i> '" + describe(v) + "'", new iUpdateable() {

						public void update() {
							interpose(ex, source, v, false);
						}
					});
					items.put("         \u2194 <b>Add Interposing</b> inside '" + describe(ex) + "' new element from '" + describe(source) + "' <i>to</i> '" + describe(v) + "'", new iUpdateable() {

						public void update() {
							interpose(ex, source, v, true);
						}
					});
				}
			}
			return super.menuItemsFor(source, items);
		}

		@Override
		public <T> VisitCode getProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> ref) {
			if (prop.equals(defaultTopology)) {
				ref.set((T) new TopologyOverElements(root, null).getTopology(source));
				return VisitCode.stop;
			} else

				return super.getProperty(source, prop, ref);
		}

		private String connectText(iVisualElement c1) {
			if (c1 == null)
				return "Connect";
			return "Add another connection";
		}

	}

	static public class TopologyConnective extends LineDrawingOverride {

		
	}

	static public final VisualElementProperty<iTopology<iVisualElement>> defaultTopology = new VisualElementProperty<iTopology<iVisualElement>>("topology_");

	static public final VisualElementProperty<TopologyPlugin> topology_plugin = new VisualElementProperty<TopologyPlugin>("topology_plugin");

	static public final VisualElementProperty<Object> line_decorate = new VisualElementProperty<Object>("connectionDecorate_");

	static public final VisualElementProperty<Number> line_mainHeight = new VisualElementProperty<Number>("connectionHeight");

	static public final String pluginId = "//topology_plugin";

	private LocalVisualElement lve;

	private Connections connections;

	private iVisualElement root;

	protected Overrides overrides;

	protected Map<Object, Object> properties = new HashMap<Object, Object>();

	public void close() {
	}

	public String describe(iVisualElement source) {
		String n = source.getProperty(iVisualElement.name);
		if (n == null)
			return "no name";
		return n;
	}

	public List<iVisualElement> getExistingConnections(iVisualElement from, iVisualElement to) {

		List<iVisualElement> r = new ArrayList<iVisualElement>();
		Collection<iVisualElement> v = connections.connections.values();
		for (iVisualElement e : v) {
			VisualElementReference e1 = LineDrawingOverride.lineDrawing_from.get(e);
			VisualElementReference e2 = LineDrawingOverride.lineDrawing_to.get(e);
			if (e1.get(root) == from && e2.get(root) == to) {
				r.add(e);
			}
		}
		return r;
	}

	public Object getPersistanceInformation() {
		return new Pair<String, Object>("version_1", null);
	}

	public iVisualElement getWellKnownVisualElement(String id) {
		if (id.equals(pluginId))
			return lve;
		return null;
	}

	public iVisualElement isConnected(iVisualElement from, iVisualElement to) {

		System.out.println(" connections <" + connections + "> <" + (connections != null ? connections.connections : null) + ">");

		Set<Entry<String, iVisualElement>> cc = connections.connections.entrySet();
		for (Entry<String, iVisualElement> e : cc) {
			iVisualElement v = e.getValue();
			VisualElementReference f = LineDrawingOverride.lineDrawing_from.get(v);
			VisualElementReference t = LineDrawingOverride.lineDrawing_to.get(v);
			if (f.get(root) == from && t.get(root) == to) {
				return v;
			}
		}
		return null;
	}

	public void registeredWith(iVisualElement root) {

		this.root = root;

		overrides = new Overrides();
		lve = new LocalVisualElement();
		lve.setProperty(topology_plugin, this);
		overrides.setVisualElement(lve);

		connections = Connections.connections_plugin.get(root);

		if (connections == null) {
			System.err.println(" warning: couldn't find connections (needed for topologyPlugin)");
			List<iVisualElement> c = root.getChildren();
			System.err.println("              children are <" + c + ">");
		} else
			root.addChild(lve);

		BasicDrawingPlugin drawing = BasicDrawingPlugin.simpleConstraints_plugin.get(root);
		if (drawing != null) {
			SelectionSetDriver driver = drawing.getSelectionSetDriver();
			driver.addNotibleOverridesClass(TopologyPlugin.TopologyConnective.class, "topology connective");
		} else {
			System.out.println(" warning, drawing plugin was null");
		}

		HierarchyPlugin hp = HierarchyPlugin.hierarchyPlugin.get(root);
		if (hp != null) {
			HierarchyHandler hh = new HierarchyHandler(root) {
				@Override
				public void finalizeConnection(iVisualElement origin, iVisualElement target, int buttons) {
					if (buttons == 1) {
						connections.connect(origin, target, TopologyConnective.class, false);
					} else if (buttons == 3) {
						final iVisualElement c1 = isConnected(origin, target);
						final iVisualElement c2 = isConnected(target, origin);
						if (c1 == null)
							connections.connect(origin, target, TopologyConnective.class, false);
						if (c2 == null)
							connections.connect(target, origin, TopologyConnective.class, false);
					}
				}

				@Override
				public String getMessage(iVisualElement origin, iVisualElement target, int buttons) {
					String no = iVisualElement.name.get(origin);
					String nt = iVisualElement.name.get(target);
					if (buttons == 1) {
						final iVisualElement c1 = isConnected(origin, target);
						if (c1 != null) {
							return "will connect '" + no + "' to '" + nt + "' again";
						} else
							return "will connect '" + no + "' to '" + nt+"'";
					} else if (buttons == 3) {
						final iVisualElement c1 = isConnected(origin, target);
						final iVisualElement c2 = isConnected(target, origin);
						if (c1 == null)
							if (c2 == null)
								return "will connect '" + no + "' and '" + nt + "' together, both ways";
							else
								return "will connect '" + no + "' to '" + nt + "' (already connected the other way around)";
						else if (c2 == null)
							return "will connect '" + nt + "' to '" + no + "' (already connected the other way around)";
						else
							return "\u2014\u2014 already connected both ways \u2014\u2014";
					}
					return "\u2014\u2014 do nothing \u2014\u2014";
				}
			};

			hh.install("icons/loop_16x16.png", hp, "Make elements that connect between elements", "Topology Tool", "# Topology Tool (T)\n\nThis tool creates elements that connect between other elements, allowing the creation of graphs and trees. You can also connect and interpose elements by 'marking' multiple elements (command-click) and using the right-click menu at any time.");
		}

		PluginList.addStringToPluginEntry(this.getClass(), "Installs a <i>modal tool</i> available from the mouse pallette that allows you to connect elements together, allowing the creation of graphs and trees.");

	}

	public void setPersistanceInformation(Object o) {
	}

	public void update() {
	}

	private iVisualElement makeNewConnection(iVisualElement exampleConnection) {
		Triple<VisualElement, PlainComponent, TopologyConnective> c1 = VisualElement.create(new Rect(30, 30, 30, 30), VisualElement.class, PlainComponent.class, TopologyConnective.class);

		c1.left.addChild(root);
		new iVisualElementOverrides.MakeDispatchProxy().getBackwardsOverrideProxyFor(c1.left).added(c1.left);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(c1.left).added(c1.left);

		connections.connections.put(c1.left.getUniqueID(), c1.left);

		// copySource properties from example connection

		String s = PythonPlugin.python_source.get(exampleConnection);
		PythonPlugin.python_source.set(exampleConnection, exampleConnection, s);

		return c1.left;
	}

	protected void interpose(iVisualElement exampleConnection, iVisualElement from, iVisualElement to, boolean keepOriginal) {

		iVisualElement newElement = makeNewElement(from.getFrame(null).blendTowards(0.5f, to.getFrame(null)));

		iVisualElement e1 = keepOriginal ? makeNewConnection(exampleConnection) : exampleConnection;

		LineDrawingOverride.lineDrawing_from.set(e1, e1, new VisualElementReference(from));
		LineDrawingOverride.lineDrawing_to.set(e1, e1, new VisualElementReference(newElement));

		iVisualElement e2 = makeNewConnection(exampleConnection);

		LineDrawingOverride.lineDrawing_from.set(e2, e2, new VisualElementReference(newElement));
		LineDrawingOverride.lineDrawing_to.set(e2, e2, new VisualElementReference(to));

	}

	protected iVisualElement makeNewElement(Rect bounds) {
		Triple<VisualElement, DraggableComponent, DefaultOverride> created = VisualElement.createAddAndName(bounds, root, "untitled", VisualElement.class, DraggableComponent.class, DefaultOverride.class, null);
		return created.left;
	};

}
