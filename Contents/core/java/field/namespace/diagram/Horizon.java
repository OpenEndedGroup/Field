package field.namespace.diagram;

import java.util.List;

import field.launch.iUpdateable;
import field.math.abstraction.iDoubleProvider;


/**
 * also needed
 *
 * connections that autoreconnect a la geometry
 * connections that, upon disconnecting from one, delees the other
 * union of many iChannels
 *
 * we've promissed a meta-grbf-9new windows)-marker
 * how does slicing  work with that? Well, we need to know if the grbf needs resorting or not after a grbf time update
 * we'll need iMutableMarker
 * perhaps grbf should be implemented in terms fo a channel... this makes sense.
 *
 * for he mutable case, a linked list structure (inside the, meta list)
 *
 * slices could be handled by having slice markers in there, or having slices keep access to their first and last and search out on modification
 *
 * so we need a mutable channel then a meta-grbf-window thing then
 *
 * batching is good, but we could do with the the production dispatch stuff as well if we wanted to go there. However, we could make maore use of batching
 *
 *
 * if this class is on a mutable channel, it needs to register for mmarker modified events, and check formarkers moving accross the threshold.
 *
 * @author marc
 * Created on Dec 20, 2004 \u2014 bus to boston
 */
public abstract class Horizon implements iUpdateable {

	protected float alpha = 0.95f;

	protected double clockLastTick = 0;

	protected double clockUpdateEstimation = 0;

	protected boolean firstUpdate = true;

	protected double lastSliceEndsAt = 0;

	protected iDoubleProvider nowClock;

	protected DiagramZero.iChannel outputStream;

	protected float standoff = 1;

	protected DiagramZero.iChannel slice;

	boolean noReadAhead = false;

	public Horizon(iDoubleProvider nowClock, DiagramZero.iChannel outputStream) {
		this.nowClock = nowClock;
		this.outputStream = outputStream;

	}

	public iDoubleProvider getClockSource() {
		return this.nowClock;
	}

	public Horizon setClockSource(iDoubleProvider clockSource) {
		this.nowClock = clockSource;
		return this;
	}

	public void setNoReadAhead() {
		noReadAhead = true;
	}

	public Horizon setStandoff(float standoff) {
		this.standoff = standoff;
		return this;
	}

	public void update() {

		if (noReadAhead) {

			double now = nowClock.evaluate();
			if (now < clockLastTick)
				firstUpdate = true;

			if (firstUpdate) {
				clockLastTick = now;
				lastSliceEndsAt = clockLastTick;
				firstUpdate = false;
			}

			double updateFrom = clockLastTick;
			double updateTo = now;
			update(now, updateFrom, updateTo);

			clockLastTick = now;
			lastSliceEndsAt = updateTo;

		} else {

			double now = nowClock.evaluate();
			if (now < clockLastTick)
				firstUpdate = true;

			if (firstUpdate) {
				clockLastTick = nowClock.evaluate();
				lastSliceEndsAt = clockLastTick;
				firstUpdate = false;
			}

			double currentClockUpdate = now - clockLastTick;

			clockUpdateEstimation = clockUpdateEstimation * alpha + (1 - alpha) * currentClockUpdate;

			double updateFrom = now + clockUpdateEstimation * standoff;
			double updateTo = now + clockUpdateEstimation * (standoff + 1);

			updateFrom = lastSliceEndsAt;
			if (updateTo < lastSliceEndsAt)
				updateTo = lastSliceEndsAt;

			update(now, updateFrom, updateTo);

			lastSliceEndsAt = updateTo;
			clockLastTick = now;
		}
	}

	protected void update(double now, double from, double to) {
		if (outputStream instanceof Channel) ((Channel)outputStream).setSliceIsGreedy(false);
		List l = outputStream.getSlice((float) from, (float) to).getIterator().remaining();
		updateWithList(now, l);
	}

	abstract protected void updateWithList(double now, List l);

}
