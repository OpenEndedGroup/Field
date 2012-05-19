package field.core.plugins;

import java.util.List;
import java.util.Map;
import java.util.Set;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.iComponent;
import field.launch.iUpdateable;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.util.Dict.Prop;

public class GroupOverride extends iVisualElementOverrides.DefaultOverride{

	static public final VisualElementProperty<Integer> groupOutset = new VisualElementProperty<Integer>("groupOutset_i");

	boolean inside = false;

	@Override
	public VisitCode added(iVisualElement newSource) {
		shouldChangeFrame(this.forElement, forElement.getFrame(null),forElement.getFrame(null), true);
		return super.added(newSource);
	}


	@Override
	public VisitCode inspectablePropertiesFor(iVisualElement source, List<Prop> properties) {
		if (source != forElement) { return super.inspectablePropertiesFor(source, properties); }

		int outset = getOutset();

//		properties.add(new Prop("group outset", "" + outset){
//			@Override
//			protected String change(String old, String newValue) {
//				int i = Integer.parseInt(newValue);
//				forElement.setProperty(groupOutset, i);
//				forElement.setProperty(iVisualElement.dirty, true);
//				forElement.setFrame(computeNewBoundingFrame((List<iVisualElement>) forElement.getParents(), null, null, null, forElement.getFrame(null), i));
//				return "" + i;
//			}
//
//			@Override
//			protected String update(String oldValue) {
//				return "" + forElement.getProperty(groupOutset);
//			}
//		});

		return VisitCode.cont;
	}

	@Override
	public VisitCode menuItemsFor(iVisualElement source, Map<String, iUpdateable> items) {

		if (source == forElement) {

			final Ref<SelectionGroup<iComponent>> group = new Ref<SelectionGroup<iComponent>>(null);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(source).getProperty(source, iVisualElement.selectionGroup, group);

			items.put("Groups (" + forElement.getProperty(iVisualElement.name) + ")", null);

			items.put("   \u21e3  unpack selection from this group", new iUpdateable(){

				public void update() {
					List<iVisualElement> parents = (List<iVisualElement>) forElement.getParents();

					Set<iComponent> selection = group.get().getSelection();

					for (iComponent c : selection) {
						iVisualElement ve = c.getVisualElement();
						if (ve != null) {
							if (parents.contains(ve) && parents.size() > 1) {
								ve.removeChild(forElement);
							}
						}
					}

					groupMembershipChanged();

				}
			});

			items.put("   \u21e3  put selection into this group", new iUpdateable(){

				public void update() {
					List<iVisualElement> parents = (List<iVisualElement>) forElement.getParents();

					Set<iComponent> selection = group.get().getSelection();

					for (iComponent c : selection) {
						iVisualElement ve = c.getVisualElement();
						if (ve != null) {
							if (!parents.contains(ve) && ve != forElement) {
								ve.addChild(forElement);
							}
						}
					}
					groupMembershipChanged();
				}
			});

		} else {
			List<iVisualElement> c = (List<iVisualElement>) this.forElement.getParents();
			if (c.contains(source)) {

			}
		}

		return VisitCode.cont;
	}

	@Override
	public VisitCode shouldChangeFrame(iVisualElement source, Rect newFrame, Rect oldFrame, boolean now) {

		if (inside) {
			super.shouldChangeFrame(source, newFrame, oldFrame, now);
			return VisitCode.cont;
		}
		int out = getOutset();

		if (source != forElement) {
			List<iVisualElement> c = (List<iVisualElement>) this.forElement.getParents();
			if (!c.contains(source)) { return super.shouldChangeFrame(source, newFrame, oldFrame, now); }
			// recalculate forElement's
			// frame
			inside = true;
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).shouldChangeFrame(forElement, computeNewBoundingFrame(c, source, newFrame, oldFrame, forElement.getFrame(null), out), forElement.getFrame(null), true);
			inside = false;
			return VisitCode.cont;
		}

		List<iVisualElement> c = (List<iVisualElement>) this.forElement.getParents();
		inside = true;
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).shouldChangeFrame(forElement, computeNewBoundingFrame(c, source, newFrame, oldFrame, forElement.getFrame(null), out), forElement.getFrame(null), true);
		for (iVisualElement e : c) {
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(e).shouldChangeFrame(e, computeNewSubFrame(e, newFrame, oldFrame, e.getFrame(null), out), e.getFrame(null), true);
		}
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).shouldChangeFrame(forElement, computeNewBoundingFrame(c, source, newFrame, oldFrame, forElement.getFrame(null), out), forElement.getFrame(null), true);
		inside = false;
		return VisitCode.cont;
	}

	private Rect computeNewSubFrame(iVisualElement e, Rect newParentFrame, Rect oldParentFrame, Rect oldChildFrame, int out) {

		double x1 = (oldChildFrame.x - oldParentFrame.x) / oldParentFrame.w;
		double y1 = (oldChildFrame.y - oldParentFrame.y) / oldParentFrame.h;
		double x2 = (oldChildFrame.x + oldChildFrame.w - oldParentFrame.x) / oldParentFrame.w;
		double y2 = (oldChildFrame.y + oldChildFrame.h - oldParentFrame.y) / oldParentFrame.h;

		x1 = newParentFrame.x + x1 * newParentFrame.w;
		y1 = newParentFrame.y + y1 * newParentFrame.h;
		x2 = newParentFrame.x + x2 * newParentFrame.w;
		y2 = newParentFrame.y + y2 * newParentFrame.h;

		return new Rect(x1, y1, x2 - x1, y2 - y1);
	}

	protected Rect computeNewBoundingFrame(List<iVisualElement> c, iVisualElement source, Rect newSourceFrame, Rect oldSourceFrame, Rect oldParentFrame, int out) {
		if (c.size() == 0) return oldParentFrame;
		float mx = Float.POSITIVE_INFINITY;
		float my = Float.POSITIVE_INFINITY;
		float xx = Float.NEGATIVE_INFINITY;
		float yy = Float.NEGATIVE_INFINITY;
		Rect t = new Rect(0, 0, 0, 0);
		boolean set = false;
		for (iVisualElement ve : c) {
			ve.getFrame(t);
			if (t.x < mx) mx =(float) t.x;
			if (t.y < my) my = (float)t.y;
			if (t.x + t.w > xx) xx = (float)(t.x + t.w);
			if (t.y + t.h > yy) yy =(float)( t.y + t.h);
		}

		return new Rect(mx - out, my - out, out * 2 + xx - mx, out * 2 + yy - my);
	}

	protected int getOutset() {
		Integer outset = forElement.getProperty(groupOutset);
		if (outset == null) {
			forElement.setProperty(groupOutset, outset = 5);
		}
		return outset;
	}

	protected void groupMembershipChanged() {
	}

}
