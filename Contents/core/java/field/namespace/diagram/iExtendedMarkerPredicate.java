package field.namespace.diagram;

import field.namespace.diagram.DiagramZero.iMarker;

public interface iExtendedMarkerPredicate<T>{

	enum PredicateReturn
	{
		is, better, worse;
	}
	
	public PredicateReturn is(iMarker<T> newMarker, iMarker<T> currentMarker);
}
