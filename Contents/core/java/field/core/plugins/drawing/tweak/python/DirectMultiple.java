package field.core.plugins.drawing.tweak.python;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.tweak.TweakSplineUI.SelectedVertex;
import field.core.plugins.drawing.tweak.TweakSplineUI.SubSelection;
import field.namespace.generic.Generics.Pair;


public class DirectMultiple implements iNodeSelection {

	private final int[] pathNodeNumber;

	public DirectMultiple(int[] pathNodeNumber) {
		this.pathNodeNumber = pathNodeNumber;
	}

	public List<Pair<SelectedVertex, Float>> selectFrom(List<CachedLine> here) {

		List<Pair<SelectedVertex, Float>> r = new ArrayList<Pair<SelectedVertex, Float>>();

		for (int i = 0; i < pathNodeNumber.length / 3; i++) {
			int pathNumber = pathNodeNumber[i * 3];
			int nodeNumber = pathNodeNumber[i * 3 + 1];
			int code = pathNodeNumber[i * 3 + 2];

			if (here.size() <= pathNumber) return null;
			if (here.get(pathNumber) == null) return null;
			if (here.get(pathNumber).events.size() <= nodeNumber) return null;

			SelectedVertex v = new SelectedVertex(here.get(pathNumber), nodeNumber);
			if ((code & 1) != 0) v.whatSelected.add(SubSelection.previousControl);
			if ((code & 2) != 0) v.whatSelected.add(SubSelection.postion);
			if ((code & 4) != 0) v.whatSelected.add(SubSelection.nextControl);

			r.add(new Pair<SelectedVertex, Float>(v, 1f));
		}
		return r;
	}

}
