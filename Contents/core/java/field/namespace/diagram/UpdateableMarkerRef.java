package field.namespace.diagram;

import java.lang.ref.WeakReference;
import java.util.LinkedHashSet;

import field.launch.iUpdateable;
import field.namespace.diagram.DiagramZero.iMarker;
import field.namespace.diagram.DiagramZero.iMarkerNotify;
import field.namespace.diagram.DiagramZero.iMarkerRef;
import field.namespace.diagram.DiagramZero.iMarkerRefNotify;
import field.namespace.generic.ReflectionTools;


public class UpdateableMarkerRef<T> implements iMarkerRef<T>, iUpdateable {

	private iMarkerNotify<T> no;

	protected WeakReference<iMarker<T>> ref;

	boolean valid = true;

	boolean wasValid = true;

	boolean insideCheck = false;

	LinkedHashSet<iMarkerRefNotify> notify = new LinkedHashSet<iMarkerRefNotify>();

	public UpdateableMarkerRef(iMarker<T> at) {
		ref = new WeakReference<iMarker<T>>(at);
		if (at!=null)
			at.addNotify(no = newNotify());
		if (at==null)
		{
			wasValid = false;
			valid = false;
		}
	}

	public void addNotify(iMarkerRefNotify<T> notify) {
		this.notify.add(notify);
	}

	public iMarker<T> getMarker() {
		checkMarker();
		return ref.get();
	}


	public void removeNotify(iMarkerRefNotify<T> notify) {
		this.notify.remove(notify);
	}

	public void update() {
		checkMarker();
	}

	private void fireDifferent(iMarker<T> was) {
		ReflectionTools.apply(notify, iMarkerRefNotify.markerRefNowDifferent, this, was);
	}

	protected void checkMarker() {
		if (insideCheck) return;
		insideCheck = true;
		try {
			if (valid && ref.get() != null) {
				iMarker<T> newMarker = markerPresent();
				if (newMarker != ref.get()) {
					iMarker<T> was = ref.get();
					ref.get().removeNotify(no);
					ref = new WeakReference<iMarker<T>>(newMarker);
					newMarker.addNotify(no = newNotify());
					fireDifferent(was);
				}
			} else if ((valid && ref.get() == null) || (!valid && wasValid)) {
				if (wasValid) {
					wasValid = false;
					iMarker<T> newMarker = markerMissing();
					if (newMarker != null) {
						boolean different = newMarker != ref.get();
						iMarker<T> was = ref.get();
						ref = new WeakReference<iMarker<T>>(newMarker);
						newMarker.addNotify(no = newNotify());
						valid = true;
						wasValid = true;
						if (different) fireDifferent(was);
					} else {
						fireLost();
					}
				}
			} else if (ref.get()==null && wasValid == false)
			{
				iMarker<T> newMarker = markerStillMissing();
				if (newMarker!=null)
				{
					ref = new WeakReference<iMarker<T>>(newMarker);
					newMarker.addNotify(no = newNotify());
					valid = true;
					wasValid = true;


					 fireDifferent(null);
				}
			}
			if (!valid) ref = new WeakReference<iMarker<T>>(null);
		} finally {
			insideCheck = false;
		}
	}

	protected void fireLost() {
		ReflectionTools.apply(notify, iMarkerRefNotify.markerRefNowInvalid, this);
	}

	protected iMarker<T> markerMissing() {
		return null;
	}

	protected iMarker<T> markerPresent() {
		return ref.get();
	}

	protected iMarker<T> markerStillMissing() {
		return null;
	}
	protected iMarkerNotify<T> newNotify() {
		return new iMarkerNotify<T>(){

			public void beginMarkerNotify() {
			}

			public void endMarkerNotify() {
			}

			public void markerChanged(iMarker<T> thisMarker) {
			}

			public void markerRemoved(iMarker<T> thisMarke) {
				valid = false;
			}
		};
	}
}
