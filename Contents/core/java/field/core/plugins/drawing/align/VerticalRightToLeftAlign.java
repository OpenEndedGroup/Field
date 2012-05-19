package field.core.plugins.drawing.align;

import java.util.Set;

import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;


public class VerticalRightToLeftAlign extends VerticalLeftToLeftAlign {

	static public class Resize extends VerticalRightToLeftAlign
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
			newRect.w = best.iterator().next().x-newRect.x;
		}

	}

	public VerticalRightToLeftAlign(float baseScore) {
		super(baseScore);
	}

	@Override
	public ArrowDirection getArrowDirectionForLocalPoint() {
		return ArrowDirection.left;
	}

	@Override
	public ArrowDirection getArrowDirectionForNonLocalPoint() {
		return ArrowDirection.right;
	}

	@Override
	protected double distance(Vector2 sourcePoint, Rect targetRect) {
		return Math.abs(targetRect.x-sourcePoint.x);
	}

	@Override
	protected Class<? extends DefaultOverride> getConstraintClass() {
		return null;
	}

	@Override
	protected Vector3 localPoint(Rect currentNewRect) {
		return currentNewRect.midPointRightEdge();
	}

	@Override
	protected Vector2 originalPoint(Rect currentRect) {
		if (forbidSmallSources() && currentRect.w<15) return null;
				return new Vector2(currentRect.x+currentRect.w, currentRect.y+currentRect.h/2);
	}
	@Override
	protected void processRects(final Set<Vector2> best, Rect newRect) {
		newRect.x = best.iterator().next().x-newRect.w;
	}

	@Override
	protected Vector2 targetPoint(Rect targetRect) {
		return new Vector2(targetRect.x, targetRect.y+targetRect.h/2);
	}
}
