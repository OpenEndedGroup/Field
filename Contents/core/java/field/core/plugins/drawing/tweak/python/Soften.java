package field.core.plugins.drawing.tweak.python;

import java.util.ArrayList;
import java.util.List;

import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.tweak.TweakSplineUI.SelectedVertex;
import field.core.plugins.drawing.tweak.TweakSplineUI.SubSelection;
import field.math.linalg.Vector2;
import field.namespace.generic.Generics.Pair;


public class Soften implements iNodeSelection {

	public class AllNodesFrom implements iNodeSelection {

		private final iNodeSelection[] s2;

		public AllNodesFrom(iNodeSelection[] s) {
			s2 = s;
		}

		public List<Pair<SelectedVertex, Float>> selectFrom(List<CachedLine> here) {
			List<Pair<SelectedVertex, Float>> r = new ArrayList<Pair<SelectedVertex, Float>>();
			for (iNodeSelection ss : s2) {
				r.addAll(ss.selectFrom(here));
			}
			return r;
		}

	}

	private final iNodeSelection s;

	private final float sharp;

	public Soften(iNodeSelection s, float sharp) {
		this.s = s;
		this.sharp = sharp;
	}

	public Soften(iNodeSelection[] s, float sharp) {
		this.s = new AllNodesFrom(s);
		this.sharp = sharp;
	}

	public List<Pair<SelectedVertex, Float>> selectFrom(List<CachedLine> here) {
		List<Pair<SelectedVertex, Float>> all = s.selectFrom(here);

		if (all.size() == 0) return all;
		if (all.size() == 1) return all;
		float total = 0;
		Vector2 center = new Vector2();
		float max = 0;
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

		for (int i = 0; i < all.size(); i++) {
			if (all.get(i).left.whatSelected.contains(SubSelection.previousControl)) {
				float d = all.get(i).left.positionFor(SubSelection.previousControl, null).distanceFrom(center);
				if (d > max) max = d;
			}
			if (all.get(i).left.whatSelected.contains(SubSelection.postion)) {
				float d = all.get(i).left.positionFor(SubSelection.postion, null).distanceFrom(center);
				if (d > max) max = d;
			}
			if (all.get(i).left.whatSelected.contains(SubSelection.nextControl)) {
				float d = all.get(i).left.positionFor(SubSelection.nextControl, null).distanceFrom(center);
				if (d > max) max = d;
			}
		}

		if (max == 0) return all;

		for (int i = 0; i < all.size(); i++) {
			if (all.get(i).left.whatSelected.contains(SubSelection.previousControl)) {
				float d = all.get(i).left.positionFor(SubSelection.previousControl, null).distanceFrom(center);
				all.get(i).left.setAmountSelectedFor(SubSelection.previousControl, dFor(d, max));
			}
			if (all.get(i).left.whatSelected.contains(SubSelection.postion)) {
				float d = all.get(i).left.positionFor(SubSelection.postion, null).distanceFrom(center);
				all.get(i).left.setAmountSelectedFor(SubSelection.postion, dFor(d, max));
			}
			if (all.get(i).left.whatSelected.contains(SubSelection.nextControl)) {
				float d = all.get(i).left.positionFor(SubSelection.nextControl, null).distanceFrom(center);
				all.get(i).left.setAmountSelectedFor(SubSelection.nextControl, dFor(d, max));
			}
		}
		return all;
	}

	private float dFor(float d, float max) {
		float q = (float) Math.exp(-sharp*d / max);
		return q;
	}
}
