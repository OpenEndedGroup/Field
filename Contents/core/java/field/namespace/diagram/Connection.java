package field.namespace.diagram;

import java.util.ArrayList;
import java.util.List;

import field.namespace.diagram.Channel.Marker;
import field.namespace.diagram.DiagramZero.iConnectionNotify;
import field.namespace.diagram.DiagramZero.iMarker;
import field.namespace.diagram.DiagramZero.iMarkerRef;
import field.namespace.generic.ReflectionTools;


/**
 * @author marc Created on Sep 20, 2004
 */
public class Connection<t_up, t_down> implements DiagramZero.iConnection {

	iMarkerRef<t_up> up;

	iMarkerRef<t_down> down;

	DiagramZero.iMarkerRefNotify<t_up> no_up;

	DiagramZero.iMarkerRefNotify<t_down> no_down;

	private final Object relationshipFrom;

	private final Object relationshipTo;

	boolean wasValid = true;

	public Connection(iMarkerRef<t_up> from, final Object relationshipFrom, iMarkerRef<t_down> to, final Object relationshipTo) {
		this.relationshipFrom = relationshipFrom;
		this.relationshipTo = relationshipTo;
		up = from;
		down = to;

		up.addNotify(no_up = new DiagramZero.iMarkerRefNotify<t_up>(){

			int begin = 0;

			public void beginMarkerRefNotify() {
				if (begin == 0) ReflectionTools.apply(notify, DiagramZero.iConnectionNotify.beginConnectionNotify);
				begin++;
			}

			public void endMarkerRefNotify() {
				begin--;
				if (begin == 0) ReflectionTools.apply(notify, DiagramZero.iConnectionNotify.endConnectionNotify);
			}

			public void markerRefNowInvalid(iMarkerRef ref) {
				if (wasValid) {
					ReflectionTools.apply(notify, DiagramZero.iConnectionNotify.connectionNowInvalid, Connection.this);
					wasValid = false;
					remove();
				}
			}

			public void markerRefNowDifferent(iMarkerRef<t_up> ref, iMarker<t_up> was) {
				if (wasValid) {
					if (was != null && was instanceof Marker) ((Marker) was).removeConnection(relationshipFrom);
					iMarker<t_up> marker = ref.getMarker();
					if (marker != null && marker instanceof Marker) ((Marker) marker).addConnection(relationshipFrom, Connection.this);
				} else {
				}
			}

		});

		down.addNotify(no_down = new DiagramZero.iMarkerRefNotify<t_down>(){

			int begin = 0;

			public void beginMarkerRefNotify() {
				if (begin == 0) ReflectionTools.apply(notify, DiagramZero.iConnectionNotify.beginConnectionNotify);
				begin++;
			}

			public void endMarkerRefNotify() {
				begin--;
				if (begin == 0) ReflectionTools.apply(notify, DiagramZero.iConnectionNotify.endConnectionNotify);
			}

			public void markerRefNowInvalid(iMarkerRef ref) {
				if (wasValid) {
					ReflectionTools.apply(notify, DiagramZero.iConnectionNotify.connectionNowInvalid, Connection.this);
					wasValid = false;
					remove();
				}
			}

			public void markerRefNowDifferent(iMarkerRef<t_down> ref, iMarker<t_down> was) {
				if (wasValid) {
					if (was != null && was instanceof Marker) ((Marker) was).removeConnection(relationshipTo);
					iMarker<t_down> marker = ref.getMarker();
					if (marker != null && marker instanceof Marker) ((Marker) marker).addConnection(relationshipTo, Connection.this);
				} else {
				}
			}
		});

		((Marker) from.getMarker()).addConnection(relationshipFrom, this);
		((Marker) to.getMarker()).addConnection(relationshipTo, this);
	}

	public Connection(Channel.Marker from, final Object relationshipFrom, Channel.Marker to, final Object relationshipTo) {
		this(new UpdateableMarkerRef<t_up>(from), relationshipFrom, new UpdateableMarkerRef<t_down>(to), relationshipTo);
	}

	public iMarker getMarkerUp() {
		return up.getMarker();
	}

	public iMarker getMarkerDown() {
		return down.getMarker();
	}

	List<iConnectionNotify> notify = new ArrayList<iConnectionNotify>();

	public void addNotify(iConnectionNotify notify) {
		this.notify.add(notify);
	}

	public void removeNotify(iConnectionNotify notify) {
		this.notify.remove(notify);
	}

	public void invalidate(iMarker end) {
		if (wasValid) {
			ReflectionTools.apply(notify, DiagramZero.iConnectionNotify.beginConnectionNotify);
			ReflectionTools.apply(notify, DiagramZero.iConnectionNotify.connectionNowInvalid, this);
			ReflectionTools.apply(notify, DiagramZero.iConnectionNotify.endConnectionNotify);
		}

		iMarker<t_up> u = up.getMarker();
		if (u != null && u instanceof Marker) ((Marker) u).removeConnection(relationshipFrom);
		iMarker<t_down> d = down.getMarker();
		if (u != null && d instanceof Marker) ((Marker) d).removeConnection(relationshipTo);

		wasValid = false;
	}

	public void remove() {
		if (wasValid) {
			ReflectionTools.apply(notify, DiagramZero.iConnectionNotify.beginConnectionNotify);
			ReflectionTools.apply(notify, DiagramZero.iConnectionNotify.connectionNowInvalid, this);
			ReflectionTools.apply(notify, DiagramZero.iConnectionNotify.endConnectionNotify);
		}
		iMarker<t_up> u = up.getMarker();
		if (u != null && u instanceof Marker) ((Marker) u).removeConnection(relationshipFrom);
		iMarker<t_down> d = down.getMarker();
		if (d != null && d instanceof Marker) ((Marker) d).removeConnection(relationshipTo);
		wasValid = false;
	}

	public boolean isValid() {
		return wasValid && (up.getMarker() != null && down.getMarker() != null);
	}

	public String toString() {
		return "connection:" + up + "->" + down;
	}
}