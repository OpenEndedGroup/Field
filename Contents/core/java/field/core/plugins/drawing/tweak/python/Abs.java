package field.core.plugins.drawing.tweak.python;

import field.core.plugins.drawing.tweak.TweakSplineUI.SelectedVertex;
import field.core.plugins.drawing.tweak.TweakSplineUI.SubSelection;
import field.math.linalg.Vector2;

public class Abs implements iCoordTransformation{

	private final Vector2 previousControl;
	private final Vector2 position;
	private final Vector2 nextControl;

	public Abs(Vector2 previous, Vector2 position, Vector2 next)
	{
		this.previousControl = previous;
		this.position = position;
		this.nextControl = next;
	}
	
	public void transformNode(float selectedAmount, SelectedVertex vertex) {
		if (previousControl!=null)
			vertex.setPositionFor(vertex.onLine, SubSelection.previousControl, new Vector2().interpolate(vertex.positionFor(SubSelection.previousControl, new Vector2()), previousControl, selectedAmount));
		if (position!=null)
			vertex.setPositionFor(vertex.onLine, SubSelection.postion, new Vector2().interpolate(vertex.positionFor(SubSelection.postion, new Vector2()), position, selectedAmount));
		if (nextControl!=null)
			vertex.setPositionFor(vertex.onLine, SubSelection.nextControl, new Vector2().interpolate(vertex.positionFor(SubSelection.nextControl, new Vector2()), nextControl, selectedAmount));
	}

}
