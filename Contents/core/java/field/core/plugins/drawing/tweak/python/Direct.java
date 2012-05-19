package field.core.plugins.drawing.tweak.python;

import java.util.Collections;
import java.util.List;

import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.tweak.TweakSplineUI.SelectedVertex;
import field.core.plugins.drawing.tweak.TweakSplineUI.SubSelection;
import field.namespace.generic.Generics.Pair;


public class Direct implements iNodeSelection {

	private final int pathNumber;

	private final int nodeNumber;

	private String elements;

	public Direct(int pathNumber, int nodeNumber) {
		this.pathNumber = pathNumber;
		this.nodeNumber = nodeNumber;
		elements = "bna";
	}

	public Direct(int pathNumber, int nodeNumber, String elements) {
		this.pathNumber = pathNumber;
		this.nodeNumber = nodeNumber;
		this.elements = elements;
	}

	public List<Pair<SelectedVertex, Float>> selectFrom(List<CachedLine> here) {
		if (here.size() <= pathNumber) return null;
		if (here.get(pathNumber) == null) return null;
		if (here.get(pathNumber).events.size() <= nodeNumber) return null;

		SelectedVertex v = new SelectedVertex(here.get(pathNumber), nodeNumber);
		v.whatSelected.clear();
		if (elements.contains("a")) v.whatSelected.add(SubSelection.nextControl);
		if (elements.contains("n")) v.whatSelected.add(SubSelection.postion);
		if (elements.contains("b")) v.whatSelected.add(SubSelection.previousControl);

		return Collections.singletonList(new Pair<SelectedVertex, Float>(v, 1f));
	}

}
