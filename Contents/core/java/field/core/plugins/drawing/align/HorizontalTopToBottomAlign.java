package field.core.plugins.drawing.align;

import java.util.Set;

import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;


public class HorizontalTopToBottomAlign extends VerticalLeftToLeftAlign {

	static public class Resize extends HorizontalTopToBottomAlign {
		public Resize(float baseScore) {
			super(baseScore);
		}

		@Override
		protected void processRects(final Set<Vector2> best, Rect newRect) {
			float or = (float) (newRect.y + newRect.h);
			super.processRects(best, newRect);
			newRect.h = or - newRect.y;
		}
	}

	public HorizontalTopToBottomAlign(float baseScore) {
		super(baseScore);
	}

	@Override
	public ArrowDirection getArrowDirectionForLocalPoint() {
		return ArrowDirection.down;
	}

	@Override
	public ArrowDirection getArrowDirectionForNonLocalPoint() {
		return ArrowDirection.up;
	}

	@Override
	protected double distance(Vector2 sourcePoint, Rect targetRect) {
		return Math.abs(targetRect.y + targetRect.h - sourcePoint.y);
	}

	@Override
	protected Class<? extends DefaultOverride> getConstraintClass() {
		return null;
	}

	@Override
	protected Vector3 localPoint(Rect currentNewRect) {
		return new Vector3(currentNewRect.x + currentNewRect.w / 2, currentNewRect.y, 0);
	}

	@Override
	protected Vector2 originalPoint(Rect currentRect) {
		if (forbidSmallSources() && currentRect.w < 15) return null;
		return new Vector2(currentRect.x + currentRect.w / 2, currentRect.y);
	}

	@Override
	protected void processRects(final Set<Vector2> best, Rect newRect) {
		newRect.y = best.iterator().next().y;
	}

	@Override
	protected Vector2 targetPoint(Rect targetRect) {
		return new Vector2(targetRect.x + targetRect.w / 2, targetRect.y + targetRect.h);
	}
}
