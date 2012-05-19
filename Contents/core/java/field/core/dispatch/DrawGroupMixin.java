package field.core.dispatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.iComponent;
import field.launch.iUpdateable;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.linalg.Vector4;


public class DrawGroupMixin extends field.core.dispatch.iVisualElementOverrides.DefaultOverride {

	static public VisualElementProperty<Vector4> groupFillColor = new VisualElementProperty<Vector4>("groupFillColor");
	static public VisualElementProperty<Vector4> groupStrokeColor = new VisualElementProperty<Vector4>("groupStrokeColor");

	static public void mixin(iVisualElement e) {
		new Mixins().mixInOverride(DrawGroupMixin.class, e);
	}

	transient CachedLine frameLine;

	@Override
	public VisitCode menuItemsFor(iVisualElement source, Map<String, iUpdateable> items) {

		if (source == forElement) {

			final Ref<SelectionGroup<iComponent>> group = new Ref<SelectionGroup<iComponent>>(null);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(source).getProperty(source, iVisualElement.selectionGroup, group);

			items.put("Groups (" + forElement.getProperty(iVisualElement.name) + ")", null);

			items.put("   \u21e3  unpack selection from this group", new iUpdateable() {

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

					frameLine = null;
					forElement.setProperty(iVisualElement.dirty, true);
				}
			});

			items.put("   \u21e3  put selection into this group", new iUpdateable() {

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
					frameLine = null;
					forElement.setProperty(iVisualElement.dirty, true);
				}
			});

			items.put("   \u21e3  put selection into this group exclusively", new iUpdateable() {

				public void update() {
					List<iVisualElement> parents = (List<iVisualElement>) forElement.getParents();

					Set<iComponent> selection = group.get().getSelection();

					for (iComponent c : selection) {
						iVisualElement ve = c.getVisualElement();
						if (ve != null) {
							if (!parents.contains(ve) && ve != forElement) {
								List<iVisualElement> cp = new ArrayList<iVisualElement>(ve.getChildren());
								for(iVisualElement cc : cp)
								{
									ve.removeChild(cc);
								}
								ve.addChild(forElement);
							}
						}
					}
					frameLine = null;
					forElement.setProperty(iVisualElement.dirty, true);
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
	public VisitCode paintNow(iVisualElement source, Rect bounds, boolean visible) {
		if (source == forElement) {

			if (frameLine == null) {
				frameLine = computeFrameLine();
			}

			if (frameLine != null)
				GLComponentWindow.currentContext.submitLine(frameLine, frameLine.getProperties());
		}
		return super.paintNow(source, bounds, visible);
	}

	@Override
	public VisitCode shouldChangeFrame(iVisualElement source, Rect newFrame, Rect oldFrame, boolean now) {
		if (isChild(source)) {
			frameLine = null;
		}
		return super.shouldChangeFrame(source, newFrame, oldFrame, now);
	}

	private boolean isChild(iVisualElement source) {
		return forElement.getParents().contains(source);
	}

	protected CachedLine computeFrameLine() {
		List<iVisualElement> q = (List<iVisualElement>) forElement.getParents();

		Rect u = null;
		for (iVisualElement e : q) {
			u = Rect.union(u, e.getFrame(null));
		}

		CachedLine c = new CachedLine();
		iLine in = c.getInput();
		in.moveTo((float) u.x, (float) u.y);
		in.lineTo((float) (u.x + u.w), (float) (u.y));
		in.lineTo((float) (u.x + u.w), (float) (u.y + u.h));
		in.lineTo((float) u.x, (float) (u.y + u.h));
		in.lineTo((float) u.x, (float) u.y);

		Vector4 f = groupFillColor.get(forElement);
		if (f == null)
			f = new Vector4(0, 0, 0, 0.1);
		Vector4 s = groupStrokeColor.get(forElement);
		if (s == null)
			s = new Vector4(0, 0, 0, 0.1);

		c.getProperties().put(iLinearGraphicsContext.color, s);
		c.getProperties().put(iLinearGraphicsContext.fillColor, f);
		c.getProperties().put(iLinearGraphicsContext.filled, true);

		return c;
	}

}
