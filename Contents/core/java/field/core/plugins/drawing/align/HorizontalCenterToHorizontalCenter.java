package field.core.plugins.drawing.align;

import java.util.Set;

import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;


public class HorizontalCenterToHorizontalCenter extends VerticalLeftToLeftAlign {

	static public class Resize extends HorizontalCenterToHorizontalCenter {
		public Resize(float baseScore) {
			super(baseScore);
		}

		@Override
		protected boolean forbidSmallSources() {
			return true;
		}

		@Override
		protected boolean forbidSmallTargets() {
			return true;
		}

		@Override
		protected Class<? extends DefaultOverride> getConstraintClass() {
			return null;
		}
		@Override
		protected void processRects(final Set<Vector2> best, Rect newRect) {
			newRect.h = 2 * (best.iterator().next().y - newRect.y);
		}

	}

	public HorizontalCenterToHorizontalCenter(float baseScore) {
		super(baseScore);
	}

	@Override
	public ArrowDirection getArrowDirectionForLocalPoint() {
		return ArrowDirection.updown;
	}
	@Override
	public ArrowDirection getArrowDirectionForNonLocalPoint() {
		return ArrowDirection.updown;
	}

	@Override
	protected double distance(Vector2 sourcePoint, Rect targetRect) {
		return Math.abs(targetRect.y + targetRect.h / 2 - sourcePoint.y);
	}
	@Override
	protected boolean forbidSmallSources() {
		return false;
	}

	@Override
	protected boolean forbidSmallTargets() {
		return false;
	}

	@Override
	protected Class<? extends DefaultOverride> getConstraintClass() {
		return null;
	}

	@Override
	protected Vector3 localPoint(Rect currentNewRect) {
		return currentNewRect.midPoint();
	}

	@Override
	protected Vector2 originalPoint(Rect currentRect) {
//		if (forbidSmallSources() && currentRect.w < 15) return null;
		return new Vector2(currentRect.x + currentRect.w / 2, currentRect.y + currentRect.h / 2);
	}

	@Override
	protected void processRects(final Set<Vector2> best, Rect newRect) {
		newRect.y = best.iterator().next().y - newRect.h / 2;
	}
	@Override
	protected Vector2 targetPoint(Rect targetRect) {
		return new Vector2(targetRect.x + targetRect.w / 2, targetRect.y + targetRect.h / 2);
	}

}
