package field.core.plugins.drawing.align;

import java.util.Set;

import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;


public class VerticalLeftToRightAlign extends VerticalLeftToLeftAlign {

	static public class Resize extends VerticalLeftToRightAlign
	{
		public Resize(float baseScore) {
			super(baseScore);
		}

		@Override
		protected Class<? extends DefaultOverride> getConstraintClass() {
			return null;
		}
		@Override
		protected void processRects(final Set<Vector2> best, Rect newRect) {
			float or = (float) (newRect.x+newRect.w);
			super.processRects(best, newRect);
			newRect.w = or-newRect.x;
		}
	}

	public VerticalLeftToRightAlign(float baseScore) {
		super(baseScore);
	}

	@Override
	public ArrowDirection getArrowDirectionForLocalPoint() {
		return ArrowDirection.right;
	}

	@Override
	public ArrowDirection getArrowDirectionForNonLocalPoint() {
		return ArrowDirection.left;
	}

	@Override
	protected double distance(Vector2 sourcePoint, Rect targetRect) {
		return Math.abs(targetRect.x+targetRect.w-sourcePoint.x);
	}

	@Override
	protected Class<? extends DefaultOverride> getConstraintClass() {
		return null;
	}

	@Override
	protected Vector3 localPoint(Rect currentNewRect) {
		return currentNewRect.midPointLeftEdge();
	}

	@Override
	protected Vector2 originalPoint(Rect currentRect) {
		if (forbidSmallSources() && currentRect.w<15) return null;
				return new Vector2(currentRect.x, currentRect.y+currentRect.h/2);
	}
	@Override
	protected void processRects(final Set<Vector2> best, Rect newRect) {
		newRect.x = best.iterator().next().x;
	}


	@Override
	protected Vector2 targetPoint(Rect targetRect) {
		return new Vector2(targetRect.x+targetRect.w, targetRect.y+targetRect.h/2);
	}

}
