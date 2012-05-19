package field.core.plugins.drawing.align;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.plugins.constrain.BaseConstraintOverrides;
import field.core.plugins.constrain.constraints.VerticalLeftToLeftConstraint;
import field.core.plugins.drawing.OfferedAlignment;
import field.core.plugins.drawing.OfferedAlignment.iDrawable;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.PlainDraggableComponent;
import field.math.linalg.AxisAngle;
import field.math.linalg.Quaternion;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Triple;

public class VerticalLeftToLeftAlign extends PointOffering {

	static public class Resize extends VerticalLeftToLeftAlign {
		public Resize(float baseScore) {
			super(baseScore);
		}

		@Override
		protected void processRects(final Set<Vector2> best, Rect newRect) {
			float or = (float) (newRect.x + newRect.w);
			super.processRects(best, newRect);
			newRect.w = or - newRect.x;
		}
	}

	enum ArrowDirection {
		up, left, down, right, none, updown, leftright;
	}

	private final float baseScore;

	private float lastScore;

	float snap = 15;

	public VerticalLeftToLeftAlign(float baseScore) {
		this.baseScore = baseScore;
	}

	public ArrowDirection getArrowDirectionForLocalPoint() {
		return ArrowDirection.right;
	}

	public ArrowDirection getArrowDirectionForNonLocalPoint() {
		return ArrowDirection.right;
	}

	@Override
	protected Vector2 connectionPoint(iVisualElement source, Vector2 sourcePoint, iVisualElement target, Rect targetRect) {

		if (distance(sourcePoint, targetRect) < snap) {
			if (forbidSmallTargets() && targetRect.w < 25)
				return null;

			lastScore = (float) (snap - distance(sourcePoint, targetRect)) / snap;
			return targetPoint(targetRect);
		}
		return null;
	}

	@Override
	protected void createConstraint(iVisualElement root, iVisualElement from, iVisualElement to) {

		Class<? extends DefaultOverride> cc = getConstraintClass();
		if (cc != null) {
			Triple<VisualElement, PlainDraggableComponent, ? extends iVisualElementOverrides.DefaultOverride> created = VisualElement.create(new Rect(10, 10, 10, 10), VisualElement.class, PlainDraggableComponent.class, cc);

			Map<String, iVisualElement> parameters = new HashMap<String, iVisualElement>();
			parameters.put("left", from);
			parameters.put("right", to);

			created.left.setProperty(BaseConstraintOverrides.constraintParameters, parameters);

			created.left.addChild(root);

			iVisualElementOverrides.topology.begin(created.left);
			iVisualElementOverrides.forward.added.f(created.left);
			iVisualElementOverrides.backward.added.f(created.left);
			iVisualElementOverrides.topology.end(created.left);
		}
	}

	protected double distance(Vector2 sourcePoint, Rect targetRect) {
		return Math.abs(targetRect.x - sourcePoint.x);
	}

	protected boolean forbidSmallSources() {
		return true;
	}

	protected boolean forbidSmallTargets() {
		return true;
	}

	protected Class<? extends iVisualElementOverrides.DefaultOverride> getConstraintClass() {
		return VerticalLeftToLeftConstraint.class;
	}

	protected Vector3 localPoint(Rect currentNewRect) {
		return currentNewRect.midPointLeftEdge();
	}

	@Override
	protected Vector2 originalPoint(Rect currentRect) {

		if (forbidSmallSources() && currentRect.w < 15)
			return null;

		return new Vector2(currentRect.x, currentRect.y + currentRect.h / 2);
	}

	protected void processRects(final Set<Vector2> best, Rect newRect) {
		newRect.x = best.iterator().next().x;
	}

	@Override
	protected float returnLastScore() {
		return lastScore;
	}

	protected Vector2 targetPoint(Rect targetRect) {
		return new Vector2(targetRect.x, targetRect.y + targetRect.h / 2);
	}

	@Override
	iDrawable newDrawableFor(float bestScore, final Set<Vector2> best, Vector2 originalPoint, LinkedHashMap<iVisualElement, Rect> current, iVisualElement element, Rect originalRect, final Rect currentRect, Rect newRect) {
		return new BaseDrawable(element.getUniqueID(), bestScore, originalPoint, best) {

			private Rect currentNewRect;

			@Override
			public void process(Rect currentrect, Rect newRect) {

				newRect.setValue(currentrect);
				processRects(best, newRect);
				this.currentNewRect = new Rect(0, 0, 0, 0).setValue(newRect);
				return;
			}

			@Override
			public String toString() {
				return "iDrawable for <" + VerticalLeftToLeftAlign.this.getClass() + "> <" + currentRect + ">";
			}

			@Override
			public void update(Rect currentrect, Rect newRect) {

				newRect.setValue(currentrect);
				processRects(best, newRect);
				this.currentNewRect = new Rect(0, 0, 0, 0).setValue(newRect);
				return;
			}

			@Override
			protected void drawWithOpacity(OfferedAlignment al, float alpha) {

				iLinearGraphicsContext context = GLComponentWindow.fastContext;
				// if (context == null)
				context = GLComponentWindow.currentContext;

				CachedLine thick = new CachedLine();
				CachedLine hair = new CachedLine();
				CachedLine outline = new CachedLine();
				CachedLine hair2 = new CachedLine();

				outline.getInput().moveTo((float) (currentNewRect.x), (float) (currentNewRect.y));
				outline.getInput().lineTo((float) (currentNewRect.x + currentNewRect.w), (float) (currentNewRect.y));
				outline.getInput().lineTo((float) (currentNewRect.x + currentNewRect.w), (float) (currentNewRect.y + currentNewRect.h));
				outline.getInput().lineTo((float) (currentNewRect.x), (float) (currentNewRect.y + currentNewRect.h));
				outline.getInput().lineTo((float) (currentNewRect.x), (float) (currentNewRect.y));

				outline.getProperties().put(iLinearGraphicsContext.filled, true);
				outline.getProperties().put(iLinearGraphicsContext.color, new Vector4(0.25, 0, 0, alpha / 5f));

				thick.getProperties().put(iLinearGraphicsContext.color, new Vector4(0.25, 0, 0, alpha / 5f));
				thick.getProperties().put(iLinearGraphicsContext.filled, true);
				thick.getProperties().put(iLinearGraphicsContext.thickness, 14f);

				// al.thickLine.beginSpline(new
				// LineIdentifier(this.getToken()));
				//
				// al.thickLine.setAuxOnSpline(Base.color0_id,
				// 0.25f, 0, 0, alpha / 3);
				// al.thickLine.moveTo(currentNewRect.topLeft());
				// al.thickLine.setAuxOnSpline(Base.color0_id,
				// 0.25f, 0, 0, alpha / 3);
				// al.thickLine.lineTo(currentNewRect.topRight());
				// al.thickLine.setAuxOnSpline(Base.color0_id,
				// 0.25f, 0, 0, alpha / 2.5f);
				// al.thickLine.lineTo(currentNewRect.bottomRight());
				// al.thickLine.setAuxOnSpline(Base.color0_id,
				// 0.25f, 0, 0, alpha / 2.25f);
				// al.thickLine.lineTo(currentNewRect.bottomLeft());
				// al.thickLine.setAuxOnSpline(Base.color0_id,
				// 0.25f, 0, 0, alpha / 2);
				// al.thickLine.lineTo(currentNewRect.topLeft());
				// al.thickLine.setAuxOnSpline(Base.color0_id,
				// 0.25f, 0, 0, alpha / 3f);
				// al.thickLine.endSpline();
				//
				Vector3 pn1 = localPoint(currentNewRect);
				Vector3 pn2 = best.iterator().next().toVector3();
				Vector3 p1 = new Vector3().lerp(pn1, pn2, 1000 / (0.1f + pn1.distanceFrom(pn2)));
				Vector3 p2 = new Vector3().lerp(pn2, pn1, 1000 / (0.1f + pn1.distanceFrom(pn2)));

				hair.getInput().moveTo(p1.x, p1.y);
				hair.getInput().lineTo(p2.x, p2.y);
				hair.getProperties().put(iLinearGraphicsContext.color, new Vector4(0.25, 0, 0, alpha / 3));
				hair.getProperties().put(iLinearGraphicsContext.filled, true);

				hair2.getProperties().put(iLinearGraphicsContext.color, new Vector4(0.25, 0, 0, alpha / 6));
				hair2.getProperties().put(iLinearGraphicsContext.thickness, 2.5f);

				//
				// al.hairLine.beginSpline(new
				// LineIdentifier(this.getToken()));
				// al.hairLine.moveTo(p1);
				// al.hairLine.setAuxOnSpline(Base.color0_id,
				// 0.25f, 0, 0, alpha);
				// al.hairLine.lineTo(p2);
				// al.hairLine.setAuxOnSpline(Base.color0_id,
				// 0.25f, 0, 0, alpha);
				// al.hairLine.endSpline();
				//
				Vector3 pn1x = localPoint(currentNewRect);
				Vector3 pn2x = best.iterator().next().toVector3();

				Vector3 v = new Vector3().sub(pn2x, pn1x).setMagnitude(15);
				Vector3 c = new Vector3().lerp(pn2x, pn1x, (float) 0.5);
				Vector3 v2 = new Quaternion().set(new AxisAngle(new Vector3(0, 0, 1), (float) (Math.PI / 2))).rotateVector(new Vector3(v));
				Vector3 v3 = new Quaternion().set(new AxisAngle(new Vector3(0, 0, 1), (float) (Math.PI / 2))).rotateVector(new Vector3(v));

				// hair.getInput().moveTo(pn1x.x, pn1x.y);
				// hair.getInput().cubicTo(pn1x.x+v2.x*2,
				// pn1x.y+v2.y*2, c.x-v2.x, c.y-v2.y,
				// c.x+(v2.x+v3.x)/2, c.y+(v2.y+v3.y)/2);
				// hair.getInput().cubicTo(c.x-v3.x, c.y-v3.y,
				// pn2x.x+v3.x*2, pn2x.y+v3.y*2, pn2x.x,
				// pn2x.y);
				//
				// hair2.getInput().moveTo(pn1x.x, pn1x.y);
				// hair2.getInput().cubicTo(pn1x.x+v2.x*2,
				// pn1x.y+v2.y*2, c.x-v2.x, c.y-v2.y,
				// c.x+(v2.x+v3.x)/2, c.y+(v2.y+v3.y)/2);
				// hair2.getInput().cubicTo(c.x-v3.x, c.y-v3.y,
				// pn2x.x+v3.x*2, pn2x.y+v3.y*2, pn2x.x,
				// pn2x.y);

				thick.getInput().moveTo(pn1x.x, pn1x.y);
				thick.getInput().lineTo(pn2x.x, pn2x.y);

				//
				// al.thickLine.beginSpline(new
				// LineIdentifier(this.getToken()));
				// al.thickLine.setAuxOnSpline(Base.color0_id,
				// 0.25f, 0, 0, alpha);
				// al.thickLine.moveTo(pn1x);
				// al.thickLine.setAuxOnSpline(Base.color0_id,
				// 0.25f, 0, 0, alpha);
				// al.thickLine.lineTo(pn2x);
				// al.thickLine.setAuxOnSpline(Base.color0_id,
				// 0.25f, 0, 0, alpha);
				// al.thickLine.endSpline();

				ArrowDirection a1 = getArrowDirectionForLocalPoint();

				// if (a1 == ArrowDirection.up || a1 ==
				// ArrowDirection.updown) {
				// int vertex = al.upArrows.nextVertex(pn1);
				// al.upArrows.setAux(vertex, 4, 1, 0, 0, 0);
				// al.upArrows.setAux(vertex, Base.color0_id, 0,
				// 0, 0, 0.5f * alpha);
				// }
				// if (a1 == ArrowDirection.down || a1 ==
				// ArrowDirection.updown) {
				// int vertex = al.downArrows.nextVertex(pn1);
				// al.downArrows.setAux(vertex, 4, 1, 0, 0, 0);
				// al.downArrows.setAux(vertex, Base.color0_id,
				// 0, 0, 0, 0.5f * alpha);
				// }
				// if (a1 == ArrowDirection.left || a1 ==
				// ArrowDirection.leftright) {
				// int vertex = al.leftArrows.nextVertex(pn1);
				// al.leftArrows.setAux(vertex, 4, 1, 0, 0, 0);
				// al.leftArrows.setAux(vertex, Base.color0_id,
				// 0, 0, 0, 0.5f * alpha);
				// }
				// if (a1 == ArrowDirection.right || a1 ==
				// ArrowDirection.leftright) {
				// int vertex = al.rightArrows.nextVertex(pn1);
				// al.rightArrows.setAux(vertex, 4, 1, 0, 0, 0);
				// al.rightArrows.setAux(vertex, Base.color0_id,
				// 0, 0, 0, 0.5f * alpha);
				//
				// }
				// ArrowDirection a2 =
				// getArrowDirectionForNonLocalPoint();
				// if (a2 == ArrowDirection.up || a2 ==
				// ArrowDirection.updown) {
				// int vertex = al.upArrows.nextVertex(pn2);
				// al.upArrows.setAux(vertex, 4, 1, 0, 0, 0);
				// al.upArrows.setAux(vertex, Base.color0_id, 0,
				// 0, 0, 0.5f * alpha);
				// }
				// if (a2 == ArrowDirection.down || a2 ==
				// ArrowDirection.updown) {
				// int vertex = al.downArrows.nextVertex(pn2);
				// al.downArrows.setAux(vertex, 4, 1, 0, 0, 0);
				// al.downArrows.setAux(vertex, Base.color0_id,
				// 0, 0, 0, 0.5f * alpha);
				// }
				// if (a2 == ArrowDirection.left || a2 ==
				// ArrowDirection.leftright) {
				// int vertex = al.leftArrows.nextVertex(pn2);
				// al.leftArrows.setAux(vertex, 4, 1, 0, 0, 0);
				// al.leftArrows.setAux(vertex, Base.color0_id,
				// 0, 0, 0, 0.5f * alpha);
				// }
				// if (a2 == ArrowDirection.right || a2 ==
				// ArrowDirection.leftright) {
				// int vertex = al.rightArrows.nextVertex(pn2);
				// al.rightArrows.setAux(vertex, 4, 1, 0, 0, 0);
				// al.rightArrows.setAux(vertex, Base.color0_id,
				// 0, 0, 0, 0.5f * alpha);
				// }

				context.submitLine(thick, thick.getProperties());
				context.submitLine(hair, hair.getProperties());
				context.submitLine(hair2, hair2.getProperties());
				context.submitLine(outline, outline.getProperties());

				GLComponentWindow.getCurrentWindow(null).requestRepaint();

			}

		};
	}

}
