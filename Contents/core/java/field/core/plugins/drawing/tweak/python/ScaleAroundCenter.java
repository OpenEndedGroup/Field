package field.core.plugins.drawing.tweak.python;

import java.util.List;

import field.core.plugins.drawing.tweak.TweakSplineUI.SelectedVertex;
import field.core.plugins.drawing.tweak.TweakSplineUI.SubSelection;
import field.math.linalg.Vector2;
import field.namespace.generic.Generics.Pair;


public class ScaleAroundCenter implements iWholeCoordTransform {

	private final float dx;

	private final float dy;

	private final float ox;

	private final float oy;

	private float scaleBy;

	Vector2 center = new Vector2();

	public ScaleAroundCenter(float ox, float oy, float dx, float dy) {
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

		float d1 = new Vector2(ox, oy).distanceFrom(center);
		float d2 = new Vector2(ox + dx, oy + dy).distanceFrom(center);
		scaleBy = d2 / d1;

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
		Vector2 o = new Vector2(a).sub(center).scale(scaleBy).add(center);
		return o;
	}

}
