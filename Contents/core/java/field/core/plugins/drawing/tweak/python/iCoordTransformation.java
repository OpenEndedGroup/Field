package field.core.plugins.drawing.tweak.python;

import field.core.plugins.drawing.tweak.TweakSplineUI.SelectedVertex;

public interface iCoordTransformation {

	public void transformNode(float selectedAmount, SelectedVertex vertex);
	
}
