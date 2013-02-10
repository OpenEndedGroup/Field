package field.core.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.persistance.VisualElementReference;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.iComponent;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.graph.NodeImpl;
import field.math.graph.iMutableContainer;
import field.namespace.generic.Generics.Pair;
import field.util.HashMapOfLists;
import field.util.RectangleAllocator;

public class SimpleConstraints implements iPlugin {

	static public class AtPoint extends Constraint {
		private final float x;

		private final float y;

		private final float width;

		private final float height;

		private final float ox;

		private final float oy;

		public AtPoint(iVisualElement fireOn, iVisualElement inside, iVisualElement control, float ox, float oy) {
			super(fireOn, inside, control);
			// this.oy = oy;
			// this.ox = ox;
			Rect oldParentFrame = inside.getFrame(null);
			Rect oldFrame = control.getFrame(null);
			this.width = (float) oldFrame.w;
			this.height = (float) oldFrame.h;

			// this.x = (float)
			// ((oldFrame.x-oldParentFrame.x)/oldParentFrame.w);
			// this.y = (float)
			// ((oldFrame.y-oldParentFrame.y)/oldParentFrame.h);

			this.x = 1;
			this.y = 0;
			this.ox = -(float) (-ox + oldFrame.x - oldParentFrame.x - oldParentFrame.w);
			this.oy = -(float) (-oy + oldFrame.y - oldParentFrame.y);

			Rect newFrame = new Rect(5 + oldParentFrame.x + x * oldParentFrame.w - ox, oldParentFrame.y + y * oldParentFrame.h - oy, width, height);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(control).shouldChangeFrame(control, newFrame, oldFrame, true);
		}

		public AtPoint(iVisualElement fireOn, iVisualElement inside, iVisualElement control, float x, float y, float width, float height, float ox, float oy) {
			super(fireOn, inside, control);
			this.oy = oy;
			this.ox = ox;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			Rect oldFrame = control.getFrame(null);
			Rect oldParentFrame = inside.getFrame(null);
			Rect newFrame = new Rect(oldParentFrame.x + x * oldParentFrame.w - ox, oldParentFrame.y + y * oldParentFrame.h - oy, width, height);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(control).shouldChangeFrame(control, newFrame, oldFrame, true);
		}

		@Override
		protected boolean doFire(iVisualElement root, Rect newRect, Rect oldRect, Rect currentRect) {
			Rect oldParentFrame = from.get(root).getFrame(null);
			Rect newFrame = new Rect(oldParentFrame.x + x * oldParentFrame.w - ox, oldParentFrame.y + y * oldParentFrame.h - oy, currentRect.w, currentRect.h);

			if (newFrame.distanceFrom(currentRect) > 0) {
				currentRect.setValue(newFrame);
				return true;
			}
			return false;
		}
	}

	static public class AtPointBelow extends Constraint {

		float oy;

		public AtPointBelow(iVisualElement fireOn, iVisualElement inside, iVisualElement control, float x, float y) {
			super(fireOn, inside, control);
			// this.oy = oy;
			// this.ox = ox;
			Rect oldParentFrame = inside.getFrame(null);
			Rect oldFrame = control.getFrame(null);

			oy = (float) (y - (oldParentFrame.y + oldParentFrame.h));
		}

		@Override
		protected boolean doFire(iVisualElement root, Rect newRect, Rect oldRect, Rect currentRect) {
			Rect oldParentFrame = from.get(root).getFrame(null);
			float delta = (float) ((newRect.y+newRect.h)-(oldRect.y+oldRect.h));

//			Rect newFrame = new Rect(oldParentFrame.x, oldParentFrame.y + oldParentFrame.h + oy, oldParentFrame.w, currentRect.h);
			Rect newFrame = new Rect(oldParentFrame.x, currentRect.y+delta, oldParentFrame.w, currentRect.h);

			if (newFrame.distanceFrom(currentRect) > 0) {
				currentRect.setValue(newFrame);
				return true;
			}
			return false;
		}
	}

	static public class AtPointBelowMinWidth extends Constraint {

		float oy;
		private final float minWidth;

		public AtPointBelowMinWidth(iVisualElement fireOn, iVisualElement inside, iVisualElement control, float x, float y, float minWidth) {
			super(fireOn, inside, control);
			this.minWidth = minWidth;
			// this.oy = oy;
			// this.ox = ox;
			Rect oldParentFrame = inside.getFrame(null);
			Rect oldFrame = control.getFrame(null);

			oy = (float) (y - (oldParentFrame.y + oldParentFrame.h));
		}

		@Override
		protected boolean doFire(iVisualElement root, Rect newRect, Rect oldRect, Rect currentRect) {
			Rect oldParentFrame = from.get(root).getFrame(null);

			Rect newFrame = new Rect(oldParentFrame.x, oldParentFrame.y + oldParentFrame.h + oy, Math.max(minWidth, oldParentFrame.w), currentRect.h);

			if (newFrame.distanceFrom(currentRect) > 0) {
				currentRect.setValue(newFrame);
				return true;
			}
			return false;
		}
	}

	static public class RectangleAllocatorConstraint extends Constraint {

		private final VisualElementProperty<RectangleAllocator> allocator;

		public RectangleAllocatorConstraint(iVisualElement fireOn, iVisualElement from, iVisualElement to, VisualElementProperty<RectangleAllocator> allocator) {
			super(fireOn, from, to);
			this.allocator = allocator;
		}

		@Override
		protected boolean doFire(iVisualElement root, Rect newRect, Rect oldRect, Rect currentRect) {
			Rect oldParentFrame = from.get(root).getFrame(null);

			RectangleAllocator a = allocator.get(from.get(root));
			if (a == null)
				return false;

			Rect r = new Rect(oldParentFrame.x + oldParentFrame.w + 10, currentRect.y, currentRect.w, currentRect.h);
			r = a.allocate(to.get(root).getUniqueID(), r, RectangleAllocator.Move.down, 5);
			if (currentRect.equals(r))
				return false;

			currentRect.setValue(r);
			return true;
		}

	}

	static public abstract class Constraint {
		VisualElementReference fireOn;

		VisualElementReference from;

		VisualElementReference to;

		boolean inside = false;

		public Constraint(iVisualElement fireOn, iVisualElement from, iVisualElement to) {
			this.fireOn = new VisualElementReference(fireOn);
			this.from = new VisualElementReference(from);
			this.to = new VisualElementReference(to);
		}

		public boolean fire(iVisualElement root, Rect newRect, Rect oldRect) {
			if (inside)
				return false;
			inside = true;

			if (to == null || to.get(root) == null)
				return false;
			if (from == null || from.get(root) == null)
				return false;

			Rect currentRect = to.get(root).getFrame(null);
			Rect oldCurrentRect = to.get(root).getFrame(null);
			boolean b = doFire(root, newRect, oldRect, currentRect);
			if (b) {
				new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(to.get(root)).shouldChangeFrame(to.get(root), currentRect, oldCurrentRect, true);
			}
			inside = false;
			return b;
		}

		protected abstract boolean doFire(iVisualElement root, Rect newRect, Rect oldRect, Rect currentRect);
	}

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

	public class Overrides extends iVisualElementOverrides.DefaultOverride {
		@Override
		public VisitCode deleted(iVisualElement source) {

			constraints.remove(source);
			Iterator<Entry<iVisualElement, Collection<Constraint>>> i = constraints.entrySet().iterator();
			while (i.hasNext()) {
				Entry<iVisualElement, Collection<Constraint>> e = i.next();
				Iterator<Constraint> q = e.getValue().iterator();
				while (q.hasNext()) {
					Constraint c = q.next();
					if (c.fireOn == source || c.from == source || c.to == source) {
						q.remove();
					}
				}
			}

			return VisitCode.cont;
		}

		@Override
		public VisitCode shouldChangeFrame(iVisualElement source, Rect newFrame, Rect oldFrame, boolean now) {
			List<Constraint> list = constraints.getList(source);
			if (list != null) {
				for (Constraint c : list) {
					c.fire(root, newFrame, oldFrame);
				}
			}
			return VisitCode.cont;
		}
	}

	static public final String pluginId = "//plugin_simpleConstraints";

	public static final VisualElementProperty<SimpleConstraints> simpleConstraints_plugin = new VisualElementProperty<SimpleConstraints>("simpleConstraints_plugin");

	private final field.core.plugins.SimpleConstraints.LocalVisualElement lve;

	private iVisualElement root;

	private SelectionGroup<iComponent> group;

	HashMapOfLists<iVisualElement, Constraint> constraints = new HashMapOfLists<iVisualElement, Constraint>();

	iVisualElementOverrides elementOverride;

	Map<Object, Object> properties = new HashMap<Object, Object>();

	public SimpleConstraints() {
		lve = new LocalVisualElement();

	}

	public void addConstraint(Constraint c) {
		constraints.addToList(c.fireOn.get(root), c);
	}

	public void close() {
	}

	public Object getPersistanceInformation() {
		return new Pair<String, Collection<Collection<Constraint>>>(pluginId + "version_1", new ArrayList(constraints.values()));
	}

	public iVisualElement getWellKnownVisualElement(String id) {
		if (id.equals(pluginId))
			return lve;
		return null;
	}

	public void registeredWith(iVisualElement root) {

		this.root = root;

		// add a next
		// to root that
		// adds some
		// overrides
		root.addChild(lve);

		lve.setProperty(simpleConstraints_plugin, this);
		// register for
		// selection
		// updates? (no,
		// do it in
		// subclass)
		group = root.getProperty(iVisualElement.selectionGroup);

		elementOverride = createElementOverrides();
	}

	public void setPersistanceInformation(Object o) {
		if (o instanceof Pair) {
			Pair<String, Collection<Collection<Constraint>>> p = (Pair<String, Collection<Collection<Constraint>>>) o;
			if (p.left.equals(pluginId + "version_1")) {
				for (Collection<Constraint> cc : p.right) {
					for (Constraint c : cc)
						constraints.addToList(c.fireOn.get(root), c);
				}
			}
		}
	}

	public void update() {
	}

	protected iVisualElementOverrides createElementOverrides() {
		return new Overrides().setVisualElement(lve);
	}

}
