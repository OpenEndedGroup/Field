package field.math.graph;

import java.util.List;

public interface iMutable<P extends iMutable<P>> extends iGraphNode<P> {

	public List< ? extends iMutable<P>> getParents();

	public void addChild(P newChild);

	public void notifyAddParent(iMutable<P> newParent);

	public void removeChild(P newChild);

	public void notifyRemoveParent(iMutable<P> newParent);

	public void beginChange();

	public void endChange();

	public void registerListener(iNotification<iMutable<P>> note);

	public void deregisterListener(iNotification<iMutable<P>> note);

	public void catchupListener(iNotification<iMutable<P>> note);
}