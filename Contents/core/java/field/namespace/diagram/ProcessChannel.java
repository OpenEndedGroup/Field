package field.namespace.diagram;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import field.bytecode.protect.iInside;
import field.namespace.diagram.Channel.Marker;
import field.namespace.diagram.DiagramZero.iChannel;
import field.namespace.diagram.DiagramZero.iMarker;
import field.namespace.diagram.DiagramZero.iMarkerFactory;


public class ProcessChannel<T> implements iInside {

	private final iMarkerFactory<T> factory;

	protected final iChannel<T> outputTo;

	Map<Object, iMarker<T>> markers = new HashMap<Object, iMarker<T>>();

	HashSet<iMarker<T>> touched = new HashSet<iMarker<T>>();

	int openCount = 0;

	public ProcessChannel(iChannel<T> outputTo, DiagramZero.iMarkerFactory<T> factory) {
		this.outputTo = outputTo;
		this.factory = factory;
	}

	public void close() {
		openCount--;

		if (openCount == 0) {
			Iterator<Entry<Object, iMarker<T>>> i = markers.entrySet().iterator();
			HashSet<Object> toRemove = new HashSet<Object>();
			while (i.hasNext()) {
				Entry<Object, iMarker<T>> e = i.next();
				if (!touched.contains(e.getValue())) {
					factory.removeMarker(outputTo, e.getValue());
					toRemove.add(e.getKey());
				} else {
				}
			}

			for (Object n : toRemove) {
				markers.remove(n);
			}
			touched.clear();
		}
		assert openCount >= 0;
	}

	public iMarker<T> getExistingMarker(Object named) {
		return markers.get(named);
	}

	public iMarker<T> getExistingMarker(Object named, boolean touch) {
		iMarker<T> m = markers.get(named);
		if (m != null && touch) {
			touched.add(m);
		}
		return m;
	}

	public iMarker<T> getMarker(Object named, double start, double duration, T payload, boolean overwriteOld) {
		iMarker<T> m = markers.get(named);
		if (m != null) {
			if (overwriteOld) {
				if (m instanceof Marker) {
					outputTo.beginOperation();
					((Marker) m).setStart(start);
					((Marker) m).setDuration(duration);
					((Marker<T>) m).setPayload(payload);
					outputTo.endOperation();
				}
			}
			touched.add(m);
			return m;
		}
		m = newMarkerFor(named, start, duration, payload);
		touched.add(m);
		markers.put(named, m);
		return m;
	}

	public void open() {

		// clearing in close only, allows new markers to be created outside open() close() pairs
		openCount++;
	}

	public void removeAllForEnd() {
		Iterator<Entry<Object, iMarker<T>>> i = markers.entrySet().iterator();
		HashSet<Object> toRemove = new HashSet<Object>();
		while (i.hasNext()) {
			Entry<Object, iMarker<T>> e = i.next();
			factory.removeMarker(outputTo, e.getValue());
			toRemove.add(e.getKey());
		}
		for (Object n : toRemove) {
			markers.remove(n);
		}
	}

	protected iMarker<T> newMarkerFor(Object named, double start, double duration, T payload) {
		return factory.makeMarker(outputTo, start, duration, payload);
	}

}
