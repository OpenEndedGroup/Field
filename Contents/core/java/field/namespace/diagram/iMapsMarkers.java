package field.namespace.diagram;

import field.namespace.diagram.DiagramZero.iMarker;

public interface iMapsMarkers<A, B> {

	public iMarker<B> mapLeftToRight(iMarker<A> left);
	public iMarker<A> mapRightToLeft(iMarker<B> left);
	
}
