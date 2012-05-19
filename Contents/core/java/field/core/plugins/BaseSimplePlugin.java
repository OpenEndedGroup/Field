package field.core.plugins;

import java.util.HashMap;
import java.util.Map;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.math.graph.NodeImpl;
import field.math.graph.iMutableContainer;
import field.namespace.generic.Generics.Pair;

public abstract class BaseSimplePlugin implements iPlugin {

	protected class LocalVisualElement extends NodeImpl<iVisualElement> implements iVisualElement {

		public <T> void deleteProperty(VisualElementProperty<T> p) {
		}

		public void dispose() {
		}

		public Rect getFrame(Rect out) {
			return null;
		}

		public <T> T getProperty(iVisualElement.VisualElementProperty<T> p) {
			if (p == overrides)
				return (T) BaseSimplePlugin.this.overrides;
			Object o = properties.get(p);
			return (T) o;
		}

		public String getUniqueID() {
			return getPluginName();
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
		
		@Override
		public String toString() {
			return "lve for plugin:"+BaseSimplePlugin.this.getClass();
		}
	}

	protected class Overrides extends iVisualElementOverrides.DefaultOverride {
	}

	protected iVisualElement element;
	protected iVisualElementOverrides.DefaultOverride overrides;
	protected Map<Object, Object> properties = new HashMap<Object, Object>();

	protected iVisualElement root;

	public void close() {
	}

	public Object getPersistanceInformation() {
		return new Pair<String, Object>("version0", null);
	}

	public iVisualElement getWellKnownVisualElement(String id) {
		String n = getPluginName();
		if (id.equals(n))
			return element;
		return null;
	}

	public void registeredWith(iVisualElement root) {
		this.root = root;

		element = newVisualElement();
		overrides = newVisualElementOverrides();
		overrides.setVisualElement(element);
		element.setProperty(iVisualElement.overrides, overrides);

		root.addChild(element);

		new VisualElementProperty(getPluginName()).set(root, root, element);

	}
	
	public void setPersistanceInformation(Object o) {
	}

	public void update() {
	}

	protected String getPluginName() {
		String name = getPluginNameImpl();
		if (!name.startsWith("//plugin_"))
			name = "//plugin_" + name;
		return name;
	}

	protected abstract String getPluginNameImpl();

	protected iVisualElement newVisualElement() {
		return new LocalVisualElement();
	}

	protected DefaultOverride newVisualElementOverrides() {
		return new Overrides();
	};
	
	
}
