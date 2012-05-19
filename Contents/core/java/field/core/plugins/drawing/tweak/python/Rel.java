package field.core.plugins.drawing.tweak.python;

import field.core.plugins.drawing.tweak.TweakSplineUI.SelectedVertex;
import field.core.plugins.drawing.tweak.TweakSplineUI.SubSelection;
import field.math.linalg.Vector2;

public class Rel implements iCoordTransformation{

	private final Vector2 previousControl;
	private final Vector2 position;
	private final Vector2 nextControl;

	public Rel(Vector2 previous, Vector2 position, Vector2 next)
	{
		this.previousControl = previous == null ? null : new Vector2(previous).add(position);
		this.position = position;
		this.nextControl = next == null ? null : new Vector2(next).add(position);
	}
	
	public void transformNode(float selectedAmount, SelectedVertex vertex) {
		
		System.out.println("\nInside transformnode for Rel---------------------------");
		
		if (previousControl!=null)
			vertex.setPositionFor(vertex.onLine, SubSelection.previousControl, vertex.positionFor(SubSelection.previousControl, null).add(new Vector2().interpolate(new Vector2(), previousControl, selectedAmount)));
		if (position!=null)
			vertex.setPositionFor(vertex.onLine, SubSelection.postion, vertex.positionFor(SubSelection.postion, null).add(new Vector2().interpolate(new Vector2(), position, selectedAmount)));
		if (nextControl!=null)
			vertex.setPositionFor(vertex.onLine, SubSelection.nextControl, vertex.positionFor(SubSelection.nextControl, null).add(new Vector2().interpolate(new Vector2(), nextControl, selectedAmount)));
		System.out.println("Outside transformnode for Rel---------------------------");
	}

}
