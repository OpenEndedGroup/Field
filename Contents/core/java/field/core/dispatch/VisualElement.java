package field.core.dispatch;

import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import field.core.plugins.autoexecute.AutoExecutePythonPlugin;
import field.core.plugins.pseudo.PseudoPropertiesPlugin;
import field.core.ui.PopupTextBox;
import field.core.ui.text.PythonTextEditor;
import field.core.ui.text.protect.ClassDocumentationProtect.Comp;
import field.core.util.FieldPyObjectAdaptor2;
import field.core.windowing.components.DraggableComponent;
import field.core.windowing.components.PlainComponent;
import field.core.windowing.components.PlainDraggableComponent;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.iComponent;
import field.core.windowing.components.iDraggableComponent;
import field.core.windowing.overlay.OverlayAnimationManager;
import field.launch.iUpdateable;
import field.math.abstraction.iAcceptor;
import field.math.graph.NodeImpl;
import field.math.graph.TopologySearching;
import field.math.graph.TopologyViewOfGraphNodes;
import field.math.graph.iMutableContainer;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.graph.TopologySearching.TopologyVisitor_breadthFirst;
import field.namespace.generic.Generics.Triple;

public class VisualElement extends NodeImpl<iVisualElement> implements iVisualElement {
	static {
		FieldPyObjectAdaptor2.initialize();
	}

	static public <T extends VisualElement, S extends iComponent, U extends iVisualElementOverrides.DefaultOverride> Triple<T, S, U> create(Rect bounds, Class<T> visualElementclass, Class<S> componentClass, Class<U> overrideClass) {
		try {
			S s = componentClass.getConstructor(new Class[] { Rect.class }).newInstance(bounds);
			T t = visualElementclass.getConstructor(new Class[] { iComponent.class }).newInstance(s);
			U u = overrideClass.newInstance();
			t.setElementOverride(u);
			u.setVisualElement(t);
			t.setFrame(bounds);
			s.setVisualElement(t);
			return new Triple<T, S, U>(t, s, u);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	static public <T extends VisualElement, S extends iComponent, U extends iVisualElementOverrides.DefaultOverride> Triple<T, S, U> createAddAndName(Rect bounds, final iVisualElement root, String defaultName, Class<T> visualElementclass, Class<S> componentClass, Class<U> overrideClass, final iUpdateable continuation) {
		try {
			final S s = componentClass.getConstructor(new Class[] { Rect.class }).newInstance(bounds);
			final T t = visualElementclass.getConstructor(new Class[] { iComponent.class }).newInstance(s);
			U u = overrideClass.newInstance();
			t.setElementOverride(u);
			u.setVisualElement(t);
			t.setFrame(bounds);
			s.setVisualElement(t);

			t.addChild(root);
			
			t.setProperty(AutoExecutePythonPlugin.python_autoExec, "");
			
			new iVisualElementOverrides.MakeDispatchProxy().getBackwardsOverrideProxyFor(t).added(t);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(t).added(t);

			PopupTextBox.Modal.getStringOrCancel(PopupTextBox.Modal.elementAt(t), "name :", defaultName, new iAcceptor<String>() {
				public iAcceptor<String> set(String to) {
					iVisualElement.name.set(t, t, to);
					if (continuation != null)
						continuation.update();

					iVisualElement.dirty.set(t, t, true);
					Rect rect = s.getBounds();

					OverlayAnimationManager.notifyAsText(root, "created element '" + to + "'", rect);

					
					SelectionGroup<iComponent> selectionGroup = iVisualElement.selectionGroup.get(t);
					selectionGroup.addToSelection(iVisualElement.localView.get(t));
					iVisualElement.localView.get(t).setSelected(true);
					
					return this;
				}

			}, new iUpdateable() {
				
				@Override
				public void update() {
					delete(t);
				}
			});

			return new Triple<T, S, U>(t, s, u);

		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	static public <T extends VisualElement, S extends iComponent, U extends iVisualElementOverrides.DefaultOverride> Triple<T, S, U> createWithName(Rect bounds, final iVisualElement root, Class<T> visualElementclass, Class<S> componentClass, Class<U> overrideClass, String name) {
		try {
			S s = componentClass.getConstructor(new Class[] { Rect.class }).newInstance(bounds);
			final T t = visualElementclass.getConstructor(new Class[] { iComponent.class }).newInstance(s);
			U u = overrideClass.newInstance();
			t.setElementOverride(u);
			u.setVisualElement(t);
			t.setFrame(bounds);
			s.setVisualElement(t);


			t.addChild(root);
			new iVisualElementOverrides.MakeDispatchProxy().getBackwardsOverrideProxyFor(t).added(t);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(t).added(t);

			iVisualElement.name.set(t, t, name);
			iVisualElement.dirty.set(t, t, true);

			return new Triple<T, S, U>(t, s, u);

		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	static public <T extends VisualElement, S extends iComponent, U extends iVisualElementOverrides.DefaultOverride> Triple<T, S, U> createWithToken(final Object token, iVisualElement root, Rect bounds, Class<T> visualElementclass, Class<S> componentClass, Class<U> overrideClass) {
		try {
			final iVisualElement[] ans = new iVisualElement[1];
			TopologyVisitor_breadthFirst<iVisualElement> search = new TopologySearching.TopologyVisitor_breadthFirst<iVisualElement>(true) {
				@Override
				protected VisitCode visit(iVisualElement n) {
					Object tok = n.getProperty(iVisualElement.creationToken);
					if (tok != null && tok.equals(token)) {
						ans[0] = n;
						return VisitCode.stop;
					}
					return VisitCode.cont;
				}

			};
			search.apply(new TopologyViewOfGraphNodes<iVisualElement>(false).setEverything(true), root);

			if (ans[0] == null) {

				Triple<T, S, U> r = create(bounds, visualElementclass, componentClass, overrideClass);
				r.left.addChild(root);
				new iVisualElementOverrides.MakeDispatchProxy().getBackwardsOverrideProxyFor(r.left).added(r.left);
				new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(r.left).added(r.left);
				r.left.setProperty(iVisualElement.creationToken, token);
				return r;
			} else {
				return new Triple<T, S, U>((T) ans[0], (S) ans[0].getProperty(iVisualElement.localView), (U) ans[0].getProperty(iVisualElement.overrides));
			}

		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static void delete(final iVisualElement node) {
		delete(node, node);
	}
	
	public static void delete(iVisualElement root, final iVisualElement node) {
		if (root == null)
			root = node;

		final iVisualElement froot = root;

		// Launcher.getLauncher().registerUpdateable(new iUpdateable(){
		//
		// public void update() {
		iVisualElementOverrides.topology.begin(froot);
		iVisualElementOverrides.forward.deleted.f(node);
		iVisualElementOverrides.backward.deleted.f(node);
		iVisualElementOverrides.topology.end(froot);

		for (iVisualElement ve : new ArrayList<iVisualElement>((Collection<iVisualElement>) node.getParents())) {
			ve.removeChild(node);

			// if there are parents that
			// have no children right now,
			// delete them too
			if (ve.getChildren().size() == 0 && ve.getParents().size() == 0)
				delete(root, ve);

		}
		for (iVisualElement ve : new ArrayList<iVisualElement>(node.getChildren())) {
			node.removeChild(ve);

			// if there are parents that
			// have no children right now,
			// delete them too
			if (ve.getChildren().size() == 0 && ve.getParents().size() == 0)
				delete(root, ve);

		}

		// }});
	}

	static public void deleteWithToken(final Object token, iVisualElement root) {
		final iVisualElement[] ans = new iVisualElement[1];
		TopologyVisitor_breadthFirst<iVisualElement> search = new TopologySearching.TopologyVisitor_breadthFirst<iVisualElement>(true) {
			@Override
			protected VisitCode visit(iVisualElement n) {
				Object tok = n.getProperty(iVisualElement.creationToken);
				if (tok != null && tok.equals(token)) {
					ans[0] = n;
					return VisitCode.stop;
				}
				return VisitCode.cont;
			}

		};
		search.apply(new TopologyViewOfGraphNodes<iVisualElement>(false).setEverything(true), root);

		if (ans[0] != null) {

			iVisualElement source = ans[0];
			delete(root, source);
		}

	}

	private iComponent d;

	private HashSet<iVisualElement> cachedChildren;

	Rect frame = new Rect(0, 0, 0, 0);

	String id = "__" + new UID().toString();

	Map<Object, Object> properties = new HashMap<Object, Object>();

	iVisualElementOverrides elementOverride;

	public VisualElement() {
		reverseInsertionOrder = true;
		setElementOverride(new iVisualElementOverrides.DefaultOverride().setVisualElement(this));
		;//System.out.println(" -- new visual element --");
	}

	public VisualElement(iComponent d) {
		reverseInsertionOrder = true;
		this.d = d;
		properties.put(localView, d);
		setElementOverride(new iVisualElementOverrides.DefaultOverride().setVisualElement(this));

		;//System.out.println(" -- new visual element --");
	}

	@Override
	public void addChild(iVisualElement newChild) {
		super.addChild(newChild);
		cachedChildren = new HashSet<iVisualElement>(this.getChildren());
	}

	public <T> void deleteProperty(VisualElementProperty<T> p) {
		properties.remove(p);
	}

	public void dispose() {
		if (d == null)
			return;

		if (d instanceof DraggableComponent)
			((DraggableComponent) d).dispose();

		if (d instanceof PlainComponent)
			((PlainComponent) d).dispose();

	}

	public HashSet<iVisualElement> getCachedChildren() {
		if (cachedChildren == null)
			cachedChildren = new HashSet<iVisualElement>(this.getChildren());
		return cachedChildren;
	}

	public Rect getFrame(Rect out) {
		if (out == null)
			out = new Rect(0, 0, 0, 0);
		out.setValue(frame);
		return out;
	}

	public <T> T getProperty(iVisualElement.VisualElementProperty<T> p) {
		Object o = properties.get(p);
		return (T) o;
	}

	public String getUniqueID() {
		return id;
	}

	public Map<Object, Object> payload() {
		return properties;
	}

	@Override
	public void removeChild(iVisualElement newChild) {
		super.removeChild(newChild);
		cachedChildren = new HashSet<iVisualElement>(this.getChildren());
	}

	public VisualElement setElementOverride(iVisualElementOverrides elementOverride) {
		this.elementOverride = elementOverride;
		properties.put(overrides, elementOverride);
		return this;
	}

	public void setFrame(Rect out) {
		if (frame == null)
			frame = new Rect(0, 0, 0, 0);
		frame.setValue(out);
		if (d != null)
			d.setBounds(frame);
	}

	public iMutableContainer<Map<Object, Object>, iVisualElement> setPayload(Map<Object, Object> t) {
		properties = t;
		if (properties.get(overrides) != null)
			this.elementOverride = (iVisualElementOverrides) properties.get(overrides);
		if (properties.get(localView) != null) {
			this.d = (iComponent) properties.get(localView);
			this.d.setVisualElement(this);
		}
		if (properties.get(iVisualElement.hidden) != null) {
			if (d instanceof DraggableComponent)
				((iDraggableComponent) d).setHidden((Boolean) properties.get(iVisualElement.hidden));
			if (d instanceof PlainDraggableComponent)
				((iDraggableComponent) d).setHidden((Boolean) properties.get(iVisualElement.hidden));
		}
		return this;
	}

	public <T> iVisualElement setProperty(iVisualElement.VisualElementProperty<T> p, T to) {

		properties.put(p, to);

		if (p.equals(iVisualElement.dirty)) {
			if (d instanceof DraggableComponent)
				((iDraggableComponent) d).setDirty();
			if (d instanceof PlainComponent)
				((PlainComponent) d).setDirty();
			if (d instanceof PlainDraggableComponent)
				((iDraggableComponent) d).setDirty();
		}
		if (p.equals(iVisualElement.hidden)) {
			if (d instanceof DraggableComponent)
				((iDraggableComponent) d).setHidden((Boolean) to);
			if (d instanceof PlainDraggableComponent)
				((iDraggableComponent) d).setHidden((Boolean) to);
		}
		return this;
	}

	public void setUniqueID(String uid) {
		id = uid;
	}

	@Override
	public String toString() {
		return "Element, named <" + getProperty(iVisualElement.name) + "> : <"+id+"("+System.identityHashCode(this)+")>";
	}
	
	static public List<Comp> getClassCustomCompletion(String prefix, Object of) {
		VisualElement adaptor = ((VisualElement) of);
		List<Comp> c = new ArrayList<Comp>();
		if (prefix.length() == 0) {
			c
				.add(new Comp(
					"The _self variable (and any other box reference) gives you read and write access to properties which are stored in this element or it's parents. A great many things in Field are properties, and understanding them is often the key to managing the power of many visual elements or customizing the behavior of Field."));
		} else {
			String name = iVisualElement.name.get(adaptor);
			if (name == null)
				name = "unnamed element";
			c.add(new Comp("<h3><i><font color='#555555' >\u2014\u2014</font> " + name + " <font color='#555555' >\u2014\u2014</font></i> . " + prefix + " <font color='#555555' size=+3>\u2041\u2014</font></h3>"));
		}

		List<Comp> ps = new ArrayList<Comp>();
		ps.add(new Comp("", "<i>Psuedo</i>properties (generally read only)").setTitle(true));
		for (VisualElementProperty p : PseudoPropertiesPlugin.properties) {
			if (p.getName().startsWith(prefix)) {
				ps.add(new Comp(p.getName(), PythonTextEditor.limit("" + p.get(adaptor))));
			}
		}
		if (ps.size() > 1)
			c.addAll(ps);
		Map<Object, Object> set = adaptor.payload();
		String groupname = "Already set, local to this element";
		addPropertiesByInspection(prefix, adaptor, c, set, groupname);
		return c;
	}

	private static void addPropertiesByInspection(String prefix, iVisualElement visualElement, List<Comp> c, Map<Object, Object> set, String groupname) {
		if (set.size() > 0) {
			List<Comp> sub = new ArrayList<Comp>();
			Set<Entry<Object, Object>> e = set.entrySet();
			for (Entry<Object, Object> ee : e) {
				VisualElementProperty k = (VisualElementProperty) ee.getKey();
				String name = k.getName();
				if (name.startsWith(prefix)) {
					Object value = ee.getValue();

					Comp cc = new Comp(name, PythonTextEditor.limit("\u2190" + value));
					sub.add(cc);
				}
			}
			if (sub.size() > 0) {
				Comp t = new Comp("", groupname).setTitle(true);
				c.add(t);
				c.addAll(sub);
			}
		}
		List<iVisualElement> childern = visualElement.getChildren();
		if (c != null) {
			for (iVisualElement vv : childern) {
				Map<Object, Object> setp = vv.payload();
				String name = vv.getProperty(iVisualElement.name);
				if (name == null)
					name = vv.getClass().getName();
				addPropertiesByInspection(prefix, vv, c, setp, "Set in parent <b>" + name + "</b>");
			}
		}
	}
	
}
