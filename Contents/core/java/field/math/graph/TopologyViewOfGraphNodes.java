package field.math.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import field.namespace.generic.ReflectionTools;


public class TopologyViewOfGraphNodes<T extends iMutable<T>> implements iSynchronizedTopology<T> {

	private boolean backwards;
	private boolean everything;

	public TopologyViewOfGraphNodes(boolean backwards) {
		this.backwards = backwards;
		this.everything = false;
	}
	
	public TopologyViewOfGraphNodes<T> setEverything(boolean everything) {
		this.everything = everything;
		return this;
	}

	public TopologyViewOfGraphNodes() {
		this(false);
	}

	public void begin() {
		ReflectionTools.apply(notes, iMutableTopology.method_begin);
	}

	public void end() {
		ReflectionTools.apply(notes, iMutableTopology.method_end);
	}

	public void addChild(T from, T to) {
		from.addChild(to);
		ReflectionTools.apply(notes, iMutableTopology.method_addChild, from, to);
	}

	public void removeChild(T from, T to) {
		from.removeChild(to);
		ReflectionTools.apply(notes, iMutableTopology.method_removeChild, from, to);
	}

	Set<iMutableTopology< ? super T>> notes = new LinkedHashSet<iMutableTopology< ? super T>>();

	public void registerNotify(iMutableTopology< ? super T> here) {
		notes.add(here);
	}

	public void deregisterNotify(iMutableTopology< ? super T> here) {
		notes.remove(here);
	}

	public void added(T t) {
	}

	public void removed(T t) {
	}

	public void update(T t) {
	}

	public List<T> getParentsOf(T of) {
		if (everything)
		{
			ArrayList<T> r = new ArrayList<T>();
			r.addAll((Collection< ? extends T>) of.getParents());
			r.addAll((Collection< ? extends T>) of.getChildren());
			return r;
		}
		if (!backwards) return (List<T>) of.getParents();
		return (List<T>) of.getChildren();
	}

	public List<T> getChildrenOf(T of) {
		if (everything)
		{
			ArrayList<T> r = new ArrayList<T>();
			r.addAll((Collection< ? extends T>) of.getParents());
			r.addAll((Collection< ? extends T>) of.getChildren());
			return r;
		}
		if (!backwards) return of.getChildren();
		return (List<T>) of.getParents();
	}

	public List<T> getAll() {
		return null;
	}
}
