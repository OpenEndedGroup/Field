package field.core.plugins;

import java.util.List;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.drawing.SplineComputingOverride;
import field.math.graph.GraphNodeSearching.VisitCode;

public class LightweightGroup extends SplineComputingOverride {

	static public final VisualElementProperty<Integer> groupOutset = new VisualElementProperty<Integer>("groupOutset_i");

	boolean inside = false;

	@Override
	public DefaultOverride setVisualElement(iVisualElement ve) {
		ve.setProperty(shouldAutoComputeRect, false);
		return super.setVisualElement(ve);
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
			if (!c.contains(source)) {
				return super.shouldChangeFrame(source, newFrame, oldFrame, now);
			}
			inside = true;
			if (subElementHasChangedWillChangeBounds()) {
				new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).shouldChangeFrame(forElement, computeNewBoundingFrame(c, forElement.getFrame(null), out), forElement.getFrame(null), true);
			}
			inside = false;
			return VisitCode.cont;
		}
		
		
		oldFrame = new Rect(oldFrame);
		newFrame = new Rect(newFrame);
		
		List<iVisualElement> c = (List<iVisualElement>) this.forElement.getParents();
		inside = true;
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).shouldChangeFrame(forElement, computeNewBoundingFrame(c, forElement.getFrame(null), out), forElement.getFrame(null), true);
		for (iVisualElement e : c) {
			Rect f = e.getFrame(null);
			Rect q = computeNewSubFrame(e, newFrame, oldFrame, f, out);
			
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(e).shouldChangeFrame(e, q, f, true);
		}
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).shouldChangeFrame(forElement, computeNewBoundingFrame(c, forElement.getFrame(null), out), forElement.getFrame(null), true);
		
		inside = false;
		return VisitCode.cont;
	}

	protected boolean subElementHasChangedWillChangeBounds() {
		return true;
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

	protected Rect computeNewBoundingFrame(List<iVisualElement> c, Rect oldParentFrame, int out) {
		if (c.size() == 0)
			return oldParentFrame;
		float mx = Float.POSITIVE_INFINITY;
		float my = Float.POSITIVE_INFINITY;
		float xx = Float.NEGATIVE_INFINITY;
		float yy = Float.NEGATIVE_INFINITY;
		Rect t = new Rect(0, 0, 0, 0);
		boolean set = false;
		for (iVisualElement ve : c) {
			if (shouldBound(ve)) {
				ve.getFrame(t);
				if (t.x < mx)
					mx = (float) t.x;
				if (t.y < my)
					my = (float) t.y;
				if (t.x + t.w > xx)
					xx = (float) (t.x + t.w);
				if (t.y + t.h > yy)
					yy = (float) (t.y + t.h);
			}
		}

		return new Rect(mx - out, my - out, out * 2 + xx - mx, out * 2 + yy - my);
	}

	protected boolean shouldBound(iVisualElement ve) {
		return true;
	}

	protected int getOutset() {
		Integer outset = forElement.getProperty(groupOutset);
		if (outset == null) {
			forElement.setProperty(groupOutset, outset = 5);
		}
		return outset;
	}

}
