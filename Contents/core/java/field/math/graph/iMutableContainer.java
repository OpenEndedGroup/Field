/**
 * 
 */
package field.math.graph;


public interface iMutableContainer<T, P extends iMutable<P>> extends iMutable<P>, iGraphNode.iContainer<T,P> {
	public iMutableContainer<T, P> setPayload(T t);
}