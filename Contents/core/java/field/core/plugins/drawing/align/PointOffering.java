package field.core.plugins.drawing.align;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map.Entry;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.OfferedAlignment;
import field.core.plugins.drawing.OfferedAlignment.iDrawable;
import field.core.plugins.drawing.OfferedAlignment.iOffering;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.overlay.OverlayAnimationManager;
import field.math.linalg.Vector2;

/**
 * baseclass for point to point offerings
 * 
 * @author marc
 * 
 */
public abstract class PointOffering implements iOffering {

	static public abstract class BaseDrawable implements iDrawable {
		public int fadeIn = 10;
		public int fadeOut = 10;
		private final float score;
		private final String token;
		protected final Vector2 from;
		protected final Set<Vector2> to;

		public float alpha = 0;
		boolean stopping = false;
		boolean stopped = false;

		public BaseDrawable(String uid, float score, Vector2 from, Set<Vector2> to) {
			this.score = score;
			this.from = from;
			this.to = to;

			this.token = uid + "" + to;
		}

		public void draw(OfferedAlignment alignment) {

			if (stopped)
				return;
			if (stopping)
				alpha = alpha * 0.85f;
			else
				alpha = alpha * 0.9f + 1 * 0.1f;

			if (stopping && alpha < 0.01f)
				stopped = true;

			drawWithOpacity(alignment, alpha);
		}

		public float getScore() {
			return score;
		}

		public String getToken() {
			return token;
		}

		public boolean hasStopped() {
			return stopped;
		}

		public boolean isStopping() {
			return stopping;
		}

		abstract public void process(Rect currentrect, Rect newRect);

		public boolean restart() {
			if (stopping)
				stopping = false;
			if (stopped)
				stopped = false;
			return true;
		}

		public void stop() {
			stopping = true;
		}

		abstract public void update(Rect currentrect, Rect newRect);

		abstract protected void drawWithOpacity(OfferedAlignment alignment, float alpha);

		public iDrawable merge(iDrawable d) {
			return this;
		}

	}

	public PointOffering() {
	}

	public void createConstraint(iVisualElement root, LinkedHashMap<iVisualElement, Rect> current, iVisualElement element, Rect or, Rect originalRect, Rect currentRect) {

		Vector2 oPoint = originalPoint(currentRect);
		if (oPoint == null)
			return;

		Set<iVisualElement> best = new HashSet<iVisualElement>();
		float bestScore = Float.NEGATIVE_INFINITY;

		Set<Entry<iVisualElement, Rect>> es = current.entrySet();
		for (Entry<iVisualElement, Rect> e : es) {

			if (e.getKey().getProperty(OfferedAlignment.alignment_doNotParticipate) != null)
				continue;

			Vector2 aPoint = connectionPoint(element, oPoint, e.getKey(), e.getValue());
			if (aPoint != null) {
				float s = returnLastScore();
				if (s > bestScore) {
					best.clear();
					best.add(e.getKey());
					bestScore = s;
				} else if (s == bestScore && bestScore > Float.NEGATIVE_INFINITY) {
					best.add(e.getKey());
				}
			}
		}

		if (bestScore == Float.NEGATIVE_INFINITY)
			return;

		if (best.size() == 0)
			return;

		iVisualElement b = best.iterator().next();
		createConstraint(root, b, element);

		GLComponentWindow c = GLComponentWindow.getCurrentWindow(null);
		if (c != null) {
			OverlayAnimationManager.notifyAsText(root, "Created constraint between " + b.getProperty(iVisualElement.name) + " & " + element.getProperty(iVisualElement.name), b.getFrame(null));
		}

	}

	public iDrawable score(LinkedHashMap<iVisualElement, Rect> current, iVisualElement element, Rect originalRect, Rect currentRect, Rect newRect) {

		Vector2 oPoint = originalPoint(currentRect);
		if (oPoint == null)
			return null;

		Set<Vector2> best = new HashSet<Vector2>();
		float bestScore = Float.NEGATIVE_INFINITY;

		Set<Entry<iVisualElement, Rect>> es = current.entrySet();
		for (Entry<iVisualElement, Rect> e : es) {

			if (e.getKey().getProperty(OfferedAlignment.alignment_doNotParticipate) != null)
				continue;

			Vector2 aPoint = connectionPoint(element, oPoint, e.getKey(), e.getValue());
			if (aPoint != null) {
				float s = returnLastScore();
				if (s > bestScore) {
					best.clear();
					best.add(aPoint);
					bestScore = s;
				} else if (s == bestScore && bestScore > Float.NEGATIVE_INFINITY) {
					best.add(aPoint);
				}
			}
		}

		if (bestScore == Float.NEGATIVE_INFINITY)
			return null;

		return newDrawableFor(bestScore, best, oPoint, current, element, originalRect, currentRect, newRect);

	}

	abstract protected Vector2 connectionPoint(iVisualElement source, Vector2 sourcePoint, iVisualElement target, Rect targetRect);

	protected void createConstraint(iVisualElement root, iVisualElement from, iVisualElement to) {
	}

	abstract protected Vector2 originalPoint(Rect currentRect);

	abstract protected float returnLastScore();

	abstract iDrawable newDrawableFor(float bestScore, Set<Vector2> best, Vector2 point, LinkedHashMap<iVisualElement, Rect> current, iVisualElement element, Rect originalRect, Rect currentRect, Rect newRect);
}
