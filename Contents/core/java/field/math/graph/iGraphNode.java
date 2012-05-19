package field.math.graph;

import java.util.List;

/**
 * strong because you can go up and down
 * 
 * @author marc
 * 
 */
public interface iGraphNode<X extends iGraphNode<X>> {

	public List<? extends iGraphNode<X>> getParents();

	public List<X> getChildren();

	public interface iNotification<P extends iMutable> {
		public void beginChange();

		public void newRelationship(P parent, P child);

		public void deletedRelationship(P parent, P child);

		public void endChange();
	}

	public interface iContainer<T, P extends iGraphNode<P>> extends iGraphNode<P> {
		public T payload();
	}

	public interface iMutableContainerNotification<T, P extends iMutable<P>> extends iNotification<P> {
		public void payloadChanged(iMutableContainer<T,P> on, T to);
	}
}
