package field.namespace.diagram;

import java.util.List;

import field.namespace.diagram.DiagramZero.aChannelNotify;
import field.namespace.diagram.DiagramZero.iChannel;
import field.namespace.diagram.DiagramZero.iMarker;
import field.namespace.diagram.iExtendedMarkerPredicate.PredicateReturn;


public class PredicateReference<T> extends UpdateableMarkerRef<T> {

	protected aChannelNotify<T> channelNotify;

	protected iExtendedMarkerPredicate<T> predicate;

	protected iMarker<T> newMarkerShouldBe;

	public PredicateReference(iChannel<T> from, iExtendedMarkerPredicate<T> predicate) {
		super(scanAll(from, predicate));
		this.predicate = predicate;

		channelNotify = new aChannelNotify<T>(){
			@Override
			public void markerAdded(iMarker<T> added) {
				PredicateReturn b = PredicateReference.this.predicate.is(added, ref.get());
				if ((ref.get() == null && b == PredicateReturn.is) || (b == PredicateReturn.better)) {
					newMarkerShouldBe = added;
				}
			}

			@Override
			public void markerRemoved(iMarker<T> removed) {
				if (removed == newMarkerShouldBe) newMarkerShouldBe = null;
			}
		};

		from.addNotify(channelNotify);
	}

	@Override
	protected iMarker<T> markerPresent() {
		if (newMarkerShouldBe != null) {
			iMarker<T> o = newMarkerShouldBe;
			newMarkerShouldBe = null;
			return o;
		}
		return ref.get();
	}

	@Override
	protected iMarker<T> markerMissing() {
		if (newMarkerShouldBe != null) {
			iMarker<T> o = newMarkerShouldBe;
			newMarkerShouldBe = null;
			return o;
		}
		return null;
	}

	@Override
	protected iMarker<T> markerStillMissing() {
		if (newMarkerShouldBe != null) {
			iMarker<T> o = newMarkerShouldBe;
			newMarkerShouldBe = null;
			return o;
		}
		return null;
	}

	protected static <T> iMarker<T> scanAll(iChannel<T> from, iExtendedMarkerPredicate<T> predicate) {
		List<iMarker<T>> m = from.getIterator().remaining();

		iMarker<T> c = null;
		for (iMarker<T> marker : m) {
			PredicateReturn r = predicate.is(marker, c);
			if (c == null && r == PredicateReturn.is)
				c = marker;
			else if (r == PredicateReturn.better) c = marker;
		}
		return c;
	}

}
