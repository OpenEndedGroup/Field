package field.math.graph;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import field.bytecode.protect.annotations.HiddenInAutocomplete;
import field.namespace.generic.Bind.iFunction;

@HiddenInAutocomplete
public class NodeImpl<C extends iMutable<C>> implements iMutable<C>, Serializable {

	static final long serialVersionUID = -4300376251521807702L;

	
	protected List<C> children = new ArrayList<C>();
	protected List<iMutable<C>> parents = new ArrayList<iMutable<C>>();
	
	protected transient LinkedHashSet<WeakReference<iNotification<iMutable<C>>>> notes = new LinkedHashSet<WeakReference<iNotification<iMutable<C>>>>();
	
	protected boolean reverseInsertionOrder = false;
	
	@HiddenInAutocomplete
	public void addChild(C newChild) {
		if (reverseInsertionOrder)
			children.add(0, newChild);
		else
			children.add(newChild);
		newChild.notifyAddParent(this);
		boolean cleanNeeded = false;
		
		for(WeakReference<iNotification<iMutable<C>>> n : new ArrayList<WeakReference<iNotification<iMutable<C>>>>(notes))
		{
			iNotification<iMutable<C>> note = n.get();
			if (note!=null)
				note.newRelationship(this, newChild);
			else
				cleanNeeded = true;
		}
		if (cleanNeeded) clean(notes);
	}

	public void main()
	{
	}
	
	private void clean(LinkedHashSet<WeakReference<iNotification<iMutable<C>>>> notes2) {
		 Iterator<WeakReference<iNotification<iMutable<C>>>> w = notes2.iterator();
		 while(w.hasNext())
			 if (w.next().get()==null) w.remove();
	}

	public void notifyAddParent(iMutable<C> newParent) {
		parents.add(newParent);
	}

	@HiddenInAutocomplete
	public void removeChild(C newChild) {
		children.remove(newChild);
		newChild.notifyRemoveParent(this);
		boolean cleanNeeded = false;
		for(WeakReference<iNotification<iMutable<C>>> n : new ArrayList<WeakReference<iNotification<iMutable<C>>>>(notes))
		{
			iNotification<iMutable<C>> note = n.get();
			if (note!=null)
				note.deletedRelationship(this, newChild);
			else
				cleanNeeded = true;
		}
		if (cleanNeeded) clean(notes);
	}
	
	public void removeAll()
	{
		for(C c : new ArrayList<C>(children))
			removeChild(c);
	}

	public void removeMatching(iFunction<Boolean, C> cc)
	{
		for(C c : new ArrayList<C>(children))
			if (cc.f(c))
				removeChild(c);
	}

	public void notifyRemoveParent(iMutable<C> newParent) {
		parents.remove(newParent);
	}

	int changeCount = 0;

	public void beginChange() {
		changeCount++;
		boolean cleanNeeded = false;
		if (changeCount == 1) for (WeakReference<iNotification<iMutable<C>>> n : new ArrayList<WeakReference<iNotification<iMutable<C>>>>(notes)) {
			iNotification<iMutable<C>> nn = n.get();
			if (nn != null)
				nn.beginChange();
			else
				cleanNeeded = true;
		}
		if (cleanNeeded) clean(notes);
	}

	public void endChange() {
		changeCount--;
		boolean cleanNeeded = false;
		if (changeCount == 0) for (WeakReference<iNotification<iMutable<C>>> n : new ArrayList<WeakReference<iNotification<iMutable<C>>>>(notes)) {
			iNotification< iMutable<C>> nn = n.get();
			if (nn != null)
				nn.endChange();
			else
				cleanNeeded = true;
		}
		if (cleanNeeded) clean(notes);
	}

	public List<? extends iMutable<C>> getParents() {
		return parents;
	}

	public List<C> getChildren() {
		return children;
	}

	public void registerListener(iNotification<iMutable<C>> note) {
		notes.add(new MWeakReference<iNotification<iMutable<C>>>(note));
	}
	
	public class MWeakReference<T> extends WeakReference<T>
	{

		public MWeakReference(T referent) {
			super(referent);
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj instanceof MWeakReference && ((MWeakReference)obj).get()==this.get();
		}
		
		@Override
		public int hashCode() {
			T t = this.get();
			return t==null ? 0 : t.hashCode();
		}
		
	}

	public void deregisterListener(iNotification<iMutable<C>> note) {
		 Iterator<WeakReference<iNotification< iMutable< C>>>> w = notes.iterator();
		 while(w.hasNext())
			 if (w.next().get()==note) w.remove();
	}

	public void catchupListener(iNotification<iMutable<C>> note) {
		for(C c : children)
		{
			note.newRelationship(this, c);
		}
	}
}
