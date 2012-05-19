package field.core.plugins.drawing.tweak.python;

import java.util.List;

import field.core.plugins.drawing.tweak.TweakSplineUI.SelectedVertex;
import field.namespace.generic.Generics.Pair;


public interface iWholeCoordTransform extends iCoordTransformation {

	public void setNodes(List<Pair<SelectedVertex, Float>> all);
	
}
