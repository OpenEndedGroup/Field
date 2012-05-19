package field.math.graph;

import java.util.List;

import field.math.graph.iTopology.iMutableTopology;



public interface iSynchronizedTopology<T> extends iMutableTopology<T> {
	public void added(T t);

	public void removed(T t);

	public void update(T t);
	
	public List<T> getAll();
		
}