package field.math.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import field.namespace.generic.ReflectionTools;


public abstract class GraphNodesForTopologyView<T> implements iTopology.iMutableTopology<T> {

	HashMap<T, iMutable> forward = new HashMap<T, iMutable>();
	HashMap<iMutable,T > backward = new HashMap< iMutable,T >();

	LinkedHashSet<iMutableTopology< ? super T>> notes = new LinkedHashSet<iMutableTopology< ? super T>>();

	abstract protected iMutable newGraphNode(T from);
	abstract boolean removeGraphNode(iMutable fromP, T from);

	public Set<iMutable> getAllNodes()
	{
		return backward.keySet();
	}
	
	public void begin() {
		ReflectionTools.apply(notes, iMutableTopology.method_begin);
	}

	public void end() {
		ReflectionTools.apply(notes, iMutableTopology.method_begin);
	}

	public void addChild(T from, T to) {
		if (!forward.containsKey(from)) install(from);
		if (!forward.containsKey(to)) install(to);

		forward.get(from).addChild(forward.get(to));

		ReflectionTools.apply(notes, iMutableTopology.method_addChild, from, to);
	}

	protected iMutable install(T from) {
		iMutable node = newGraphNode(from);
		forward.put(from, node);
		backward.put(node, from);
		return node;
	}


	public void removeChild(T from, T to) {
		iMutable fromP = forward.get(from);
		iMutable toP = forward.get(to);
		fromP.removeChild(toP);
		if (fromP.getParents().size() == 0) if (removeGraphNode(fromP, from)) backward.remove(forward.remove(from));
		if (toP.getParents().size() == 0) if (removeGraphNode(toP, to)) backward.remove(forward.remove(toP));
		ReflectionTools.apply(notes, iMutableTopology.method_removeChild, from, to);
	}

	public void registerNotify(iMutableTopology< ? super T> here) {
		notes.add(here);
	}

	public void deregisterNotify(iMutableTopology< ? super T> here) {
		notes.remove(here);
	}

	public List<T> getParentsOf(T of) {
		iMutable fromP = forward.get(of);		
		if (fromP ==  null) fromP = install(of);
		List<iMutable> parents = fromP.getParents();
		if (parents.size()==0) return Collections.EMPTY_LIST;
		ArrayList<T> r = new ArrayList<T>(parents.size());
		for(iMutable m : parents)
			r.add(backward.get(m));
		return r;
	}

	public List<T> getChildrenOf(T of) {
		iMutable fromP = forward.get(of);		
		if (fromP ==  null) fromP = install(of);
		List<iMutable> parents = fromP.getChildren();
		if (parents.size()==0) return Collections.EMPTY_LIST;
		ArrayList<T> r = new ArrayList<T>(parents.size());
		for(iMutable m : parents)
			r.add(backward.get(m));
		return r;
	}
}
