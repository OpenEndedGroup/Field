package field.namespace.diagram;

import java.util.List;
import java.util.Map;

import field.namespace.diagram.DiagramZero.iChannel;
import field.namespace.diagram.DiagramZero.iMarker;
import field.namespace.diagram.DiagramZero.iMarkerIterator;
import field.namespace.diagram.DiagramZero.iMarkerNotify;


/*
 * GA stuff should use this
 */
public class Cursor<T> implements iMarkerIterator<T>, iMarker<T> {

	private iMarker<T> marker;

	private iMarkerIterator<T> iterator;

	public Cursor(iMarkerIterator<T> on) {
		assert on.hasNext();
		this.iterator = on.clone();
		this.marker = this.iterator.next();
	}

	protected Cursor(iMarkerIterator<T> on, iMarker<T> m) {
		assert on.hasNext();
		this.marker = m;
		this.iterator = on;
	}

	public void addNotify(iMarkerNotify notify) {
		marker.addNotify(notify);
	}

	public Map getConnections() {
		return marker.getConnections();
	}

	public double getDuration() {
		return marker.getDuration();
	}

	public iMarkerIterator<T> getIterator() {
		return marker.getIterator();
	}

	public T getPayload() {
		return marker.getPayload();
	}

	public iChannel<T> getRootChannel() {
		return marker.getRootChannel();
	}

	public double getTime() {
		return marker.getTime();
	}

	public void removeNotify(iMarkerNotify notify) {
		marker.removeNotify(notify);
	}

	public Cursor<T> clone() {
		return new Cursor<T>(iterator.clone(), marker);
	}

	public boolean hasNext() {
		if (lastWasPrevious) {
			iterator.next();
			lastWasPrevious = false;
		}
		return iterator.hasNext();
	}

	public boolean hasPrevious() {
		if (!lastWasPrevious) {
			iterator.previous();
			lastWasPrevious = true;
		}
		return iterator.hasPrevious();
	}

	public Cursor<T> next() {
		marker = iterator.next();
		if (lastWasPrevious) {
			marker = iterator.next();
		}
		lastWasPrevious = false;
		return this;
	}

	boolean lastWasPrevious = false;

	// warning, next() followed by previous() would give you the
	// same
	// thing. This means that the first previous() will always give
	// you the sam-ish thing
	public Cursor<T> previous() {
		marker = iterator.previous();
		if (!lastWasPrevious) {
			marker = iterator.previous();
		}
		lastWasPrevious = true;
		return this;
	}

	public List<iMarker<T>> remaining() {
		return iterator.remaining();
	}

	public void remove() {
		iterator.remove();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Cursor))
			return false;

		return ((Cursor) obj).marker == marker;
	}

	@Override
	public int hashCode() {
		return marker.hashCode();
	}

	@Override
	public String toString() {
		return "c:" + marker;
	}
	
	public boolean isPresent()
	{
		return marker.isPresent();
	}

}
