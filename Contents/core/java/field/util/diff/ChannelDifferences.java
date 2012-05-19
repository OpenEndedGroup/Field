package field.util.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import quicktime.streaming.EditEntry;

import field.namespace.diagram.Channel;
import field.namespace.diagram.Channel.Marker;
import field.namespace.diagram.DiagramZero.iChannel;
import field.namespace.diagram.DiagramZero.iMarker;
import field.namespace.diagram.DiagramZero.iMarkerIterator;
import field.namespace.generic.Generics.Pair;
import field.util.ExternalHashMap.iEquality;
import field.util.diff.ChannelDifferences.EditRelationship;
import field.util.diff.Diff.Change;

public class ChannelDifferences<A> {
    
	static public class DefaultMarkerEquality<A> implements iEquality<List<iMarker<A>>> {
        
		public boolean areEqual(List<iMarker<A>> o1, List<iMarker<A>> o2) {
			if (o1 == null || o2 == null)
				return false;
			if (o1.size() != o2.size())
				return false;
			for (int i = 0; i < o1.size(); i++)
				if (!o1.get(i).getPayload().equals(o2.get(i).getPayload()))
					return false;
			return true;
		}
        
		public int hashCodeFor(List<iMarker<A>> o1) {
			if (o1 == null)
				return 0;
			int i = 0;
			for (int n = 0; n < o1.size(); n++) {
				i = i << 2 + 31 * o1.get(n).getPayload().hashCode();
			}
			return i;
		}
        
	}
    
	public class EditRelationship {
		public List<iMarker<A>> left;
        
		public List<iMarker<A>> right;
        
		public EditType type;
        
		public String extraType = null;
        
		@Override
		public String toString() {
			return type + " :" + left + " <- " + right;
		}
	}
    
	public enum EditType {
		insertion, deletion, equivalence, unequivalence, movement, other;
	}
    
	private final Diff diff;
    
	private final Change change;
    
	private final Object[] aLeft;
    
	private final Object[] aRight;
    
	private final Channel<A> left;
    
	private final Channel<A> right;
    
	public ChannelDifferences(Channel<A> left, Channel<A> right) {
		this.left = left;
		this.right = right;
		aLeft = unwrap(left, new ArrayList()).toArray();
		aRight = unwrap(right, new ArrayList()).toArray();
        
		diff = new Diff(aLeft, aRight);
		// diff.no_discards = true;
        
		change = diff.diff_2(false);
	}
    
	public ChannelDifferences(Channel<A> left, Channel<A> right, iEquality<A> e) {
		this.left = left;
		this.right = right;
		aLeft = unwrap(left, new ArrayList()).toArray();
		aRight = unwrap(right, new ArrayList()).toArray();
        
		diff = new Diff(aLeft, aRight, e);
		// diff.no_discards = true;
        
		change = diff.diff_2(false);
	}
    
	public Channel<EditRelationship> generateTranspositions(Channel<EditRelationship> in, int lookb, iEquality<List<iMarker<A>>> equality) {
		LinkedList<Marker<EditRelationship>> stack = new LinkedList<Marker<EditRelationship>>();
		for (iMarker<EditRelationship> e : new ArrayList<iMarker<EditRelationship>>(in.getIterator().remaining())) {
			Marker<EditRelationship> removed = checkBehindAndRemove(e, stack, equality);
			if (removed != null) {
				EditRelationship er2 = new EditRelationship();
				if (e.getPayload().type == EditType.deletion) {
					er2.left = e.getPayload().left;
					er2.right = removed.getPayload().right;
					er2.type = EditType.movement;
					in.makeMarker(in, er2.left.get(0).getTime(), er2.left.get(er2.left.size() - 1).getTime() + er2.left.get(er2.left.size() - 1).getDuration() - er2.left.get(0).getTime(), er2);
				} else if (e.getPayload().type == EditType.insertion) {
					er2.left = removed.getPayload().left;
					er2.right = e.getPayload().right;
					er2.type = EditType.movement;
					in.makeMarker(in, er2.left.get(0).getTime(), er2.left.get(er2.left.size() - 1).getTime() + er2.left.get(er2.left.size() - 1).getDuration() - er2.left.get(0).getTime(), er2);
				}
			} else {
				stack.add(0, (Marker<EditRelationship>) e);
				if (stack.size() > lookb)
					stack.removeLast();
			}
		}
		return in;
	}
    
	public Channel<EditRelationship> generateUnequivalences(Channel<EditRelationship> in, int min) {
		iMarker<EditRelationship> last = null;
		for (iMarker<EditRelationship> e : new ArrayList<iMarker<EditRelationship>>(in.getIterator().remaining())) {
			if (last != null && last.getPayload().type == EditType.deletion && e.getPayload().type == EditType.insertion && last.getPayload().left != null && e.getPayload().right != null && Math.abs(last.getPayload().left.size() - e.getPayload().right.size()) < min) {
				((Marker<EditRelationship>) last).remove();
				((Marker<EditRelationship>) e).remove();
				EditRelationship er2 = new EditRelationship();
				er2.left = last.getPayload().left;
				er2.right = e.getPayload().right;
				er2.type = EditType.unequivalence;
				in.makeMarker(in, last.getPayload().left.get(0).getTime(), last.getPayload().left.get(last.getPayload().left.size() - 1).getTime() + last.getPayload().left.get(last.getPayload().left.size() - 1).getDuration() - last.getPayload().left.get(0).getTime(), er2);
			}
			last = e;
		}
		return in;
	}
    
	public List<Change> getChanges() {
		List<Change> c = new ArrayList<Change>();
		Change ch = change;
		while (ch != null) {
			c.add(ch);
			ch = ch.link;
		}
		return c;
	}
    
	public List<Pair<iMarker<A>, iMarker<A>>> getNonChanges() {
		List<Change> changes = getChanges();
		List<iMarker<A>> leftMarkers = left.getIterator().remaining();
		List<iMarker<A>> rightMarkers = right.getIterator().remaining();
		List<Pair<iMarker<A>, iMarker<A>>> p = new ArrayList<Pair<iMarker<A>, iMarker<A>>>();
		int previous = 0;
		int offset = 0;
		int readOffset = 0;
		for (int i = 0; i < changes.size(); i++) {
			Change c = changes.get(i);
			for (int n = previous; n < c.line0; n++) {
				p.add(new Pair<iMarker<A>, iMarker<A>>(leftMarkers.get(n), rightMarkers.get(n + offset)));
				previous++;
			}
			offset += c.inserted;
			offset -= c.deleted;
			previous += c.deleted;
			readOffset += c.inserted;
			readOffset -= c.deleted;
		}
		for (int n = previous; n < leftMarkers.size(); n++) {
			p.add(new Pair<iMarker<A>, iMarker<A>>(leftMarkers.get(n), rightMarkers.get(n + offset)));
		}
		return p;
	}
    
	public Channel<EditRelationship> makeRelationships() {
		Channel<EditRelationship> editRelationships = new Channel<EditRelationship>();
		List<Change> changes = getChanges();
		List<Pair<iMarker<A>, iMarker<A>>> nonChanges = getNonChanges();
		List<iMarker<A>> leftMarkers = left.getIterator().remaining();
		List<iMarker<A>> rightMarkers = right.getIterator().remaining();
		int previous = 0;
		int offset = 0;
		int readOffset = 0;
		for (int i = 0; i < changes.size(); i++) {
			Change c = changes.get(i);
			for (int n = previous; n < c.line0; n++) {
				EditRelationship er = new EditRelationship();
				er.type = EditType.equivalence;
				er.left = Collections.singletonList(leftMarkers.get(n));
				er.right = Collections.singletonList(rightMarkers.get(n + offset));
				editRelationships.makeMarker(editRelationships, leftMarkers.get(n).getTime(), leftMarkers.get(n).getDuration(), er);
				previous++;
			}
			if (c.inserted != 0) {
				EditRelationship er = new EditRelationship();
				er.type = EditType.insertion;
				er.left = null;
				er.right = new ArrayList<iMarker<A>>(rightMarkers.subList(c.line1, c.line1 + c.inserted));
				editRelationships.makeMarker(editRelationships, er.right.get(0).getTime(), er.right.get(er.right.size() - 1).getTime() + er.right.get(er.right.size() - 1).getDuration() - er.right.get(0).getTime(), er);
				// editRelationships.makeMarker(editRelationships,
				// leftMarkers.get(Math.max(0,
				// c.line0-1)).getTime()+leftMarkers.get(Math.max(0,
				// c.line0-1)).getDuration()-0.1f, 0, er);
			}
			if (c.deleted != 0) {
				EditRelationship er = new EditRelationship();
				er.type = EditType.deletion;
				er.left = new ArrayList<iMarker<A>>(leftMarkers.subList(previous, previous + c.deleted));
				er.right = null;
				editRelationships.makeMarker(editRelationships, er.left.get(0).getTime(), er.left.get(er.left.size() - 1).getTime() + er.left.get(er.left.size() - 1).getDuration() - er.left.get(0).getTime(), er);
			}
			offset += c.inserted;
			offset -= c.deleted;
			previous += c.deleted;
			readOffset += c.inserted;
			readOffset -= c.deleted;
		}
		for (int n = previous; n < leftMarkers.size(); n++) {
			EditRelationship er = new EditRelationship();
			er.type = EditType.equivalence;
			er.left = Collections.singletonList(leftMarkers.get(n));
			er.right = Collections.singletonList(rightMarkers.get(n + offset));
			editRelationships.makeMarker(editRelationships, leftMarkers.get(n).getTime(), leftMarkers.get(n).getDuration(), er);
		}
		return editRelationships;
	}
    
	public <U, T extends Collection<? super U>> T unwrap(iChannel<U> in, T collection) {
		iMarkerIterator<U> iterator = in.getIterator();
		while (iterator.hasNext()) {
			iMarker<U> m = iterator.next();
			collection.add(m.getPayload());
		}
		return collection;
	}
    
	protected Marker<EditRelationship> checkBehindAndRemove(iMarker<EditRelationship> e, LinkedList<Marker<EditRelationship>> stack, iEquality<List<iMarker<A>>> equality) {
		if (e.getPayload().type == EditType.insertion) {
			for (Marker<EditRelationship> marker : stack) {
				if (marker.getPayload().type == EditType.deletion && equality.areEqual(marker.getPayload().left, e.getPayload().right)) {
					stack.remove(marker);
					((Marker<EditRelationship>) e).remove();
					return marker;
				}
			}
		}
		if (e.getPayload().type == EditType.deletion) {
			for (Marker<EditRelationship> marker : stack) {
				if (marker.getPayload().type == EditType.insertion && equality.areEqual(marker.getPayload().right, e.getPayload().left)) {
					stack.remove(marker);
					((Marker<EditRelationship>) e).remove();
					return marker;
				}
			}
		}
		return null;
	}
    
	public Channel<EditRelationship> clumpEquivalences(Channel<EditRelationship> relationships) {
		iMarkerIterator<EditRelationship> e = relationships.getIterator();
		iMarker<EditRelationship> last = null;
		while (e.hasNext()) {
			iMarker<EditRelationship> m = e.next();
			if (m.getPayload().type == EditType.equivalence) {
				if (last == null) {
					last = m;
				} else {
					last.getPayload().left = new ArrayList<iMarker<A>>(last.getPayload().left );
					last.getPayload().left .addAll(m.getPayload().left);
					last.getPayload().right= new ArrayList<iMarker<A>>(last.getPayload().right);
					last.getPayload().right.addAll(m.getPayload().right);
					e.remove();
				}
			} else
				last = null;
		}
		return relationships; 
	}
}
