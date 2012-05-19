package field.core.plugins.drawing.tweak.python;

import java.util.List;

import field.core.plugins.drawing.tweak.TweakSplineUI.SelectedVertex;
import field.core.plugins.drawing.tweak.TweakSplineUI.SubSelection;
import field.math.linalg.Vector2;
import field.namespace.generic.Generics.Pair;


public class ScaleAroundCenterNI implements iWholeCoordTransform {

	private final float dx;

	private final float dy;

	private final float ox;

	private final float oy;

	private float scaleXBy;

	private float scaleYBy;

	Vector2 center = new Vector2();

	public ScaleAroundCenterNI(float ox, float oy, float dx, float dy) {
		this.dx = dx;
		this.dy = dy;
		this.ox = ox;
		this.oy = oy;
	}

	public void setNodes(List<Pair<SelectedVertex, Float>> all) {
		int n = 0;
		if (all.size() == 0) return;

		float total = 0;
		center.scale(0);
		for (int i = 0; i < all.size(); i++) {
			if (all.get(i).left.whatSelected.contains(SubSelection.previousControl)) {
				Vector2.add(all.get(i).left.positionFor(SubSelection.previousControl, null), all.get(i).right, center, center);
				total += all.get(i).right;
			}
			if (all.get(i).left.whatSelected.contains(SubSelection.postion)) {
				Vector2.add(all.get(i).left.positionFor(SubSelection.postion, null), all.get(i).right, center, center);
				total += all.get(i).right;
			}
			if (all.get(i).left.whatSelected.contains(SubSelection.nextControl)) {
				Vector2.add(all.get(i).left.positionFor(SubSelection.nextControl, null), all.get(i).right, center, center);
				total += all.get(i).right;
			}
		}
		if (total > 0) {
			center.scale(1 / total);
		}

		float d1x = Math.abs(ox - center.x);
		float d2x = Math.abs(ox + dx - center.x);
		float d1y = Math.abs(oy - center.y);
		float d2y = Math.abs(oy + dy - center.y);
		scaleXBy = d2x / d1x;
		scaleYBy = d2y / d1y;

	}

	public void transformNode(float selectedAmount, SelectedVertex vertex) {
		if (vertex.whatSelected.contains(SubSelection.previousControl)) {
			Vector2 a = transform(vertex.positionFor(SubSelection.previousControl, null));
			vertex.setPositionFor(vertex.onLine, SubSelection.previousControl, a);
		}
		if (vertex.whatSelected.contains(SubSelection.postion)) {
			Vector2 a = transform(vertex.positionFor(SubSelection.postion, null));
			vertex.setPositionFor(vertex.onLine, SubSelection.postion, a);
		}
		if (vertex.whatSelected.contains(SubSelection.nextControl)) {
			Vector2 a = transform(vertex.positionFor(SubSelection.nextControl, null));
			vertex.setPositionFor(vertex.onLine, SubSelection.nextControl, a);
		}
	}

	private Vector2 transform(Vector2 a) {
		// Vector2 o = new Vector2(a).sub(center).scale(scaleBy).add(center);
		Vector2 o = new Vector2();
		o.x = (a.x - center.x) * scaleXBy + center.x;
		o.y = (a.y - center.y) * scaleYBy + center.y;
		return o;
	}
}
