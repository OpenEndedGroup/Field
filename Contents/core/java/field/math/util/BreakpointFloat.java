package field.math.util;

import field.core.plugins.drawing.opengl.LineUtils;
import field.math.abstraction.iBlendable;
import field.math.abstraction.iHasScalar;

public class BreakpointFloat implements iBlendable<BreakpointFloat>, iHasScalar {

	public enum Next {
		cubic, linear, flat_here, flat_next;
	}

	public float value;

	public float controlNext;
	public float controlPrevious;

	public Next next = Next.cubic;
	
	public BreakpointFloat(float value) {
		this.value = value;
		this.controlNext = value;
		this.controlPrevious = value;
	}

	protected BreakpointFloat() {
	}
	
	public BreakpointFloat blendRepresentation_newZero() {
		BreakpointFloat f = new BreakpointFloat();
		return f;
	}

	public BreakpointFloat cerp(BreakpointFloat before, float beforeTime, BreakpointFloat now, float nowTime, BreakpointFloat next, float nextTime, BreakpointFloat after, float afterTime, float a) {
		return lerp(now, next, a);
	}

	public BreakpointFloat lerp(BreakpointFloat before, BreakpointFloat now, float a) {

		switch (before.next) {
		case cubic: {
			float x = LineUtils.evaluateCubicFrame(before.value, before.controlNext, now.controlPrevious, now.value, a);
			BreakpointFloat r = new BreakpointFloat();
			r.value = x;
			r.controlNext = x;
			r.controlPrevious = x;
			return setValue(r);
		}
		case linear: {
			float x = before.value * (1 - a) + a * now.value;
			BreakpointFloat r = new BreakpointFloat();
			r.value = x;
			r.controlNext = x;
			r.controlPrevious = x;
			return setValue(r);
		}
		case flat_here: {
			float x = before.value;
			BreakpointFloat r = new BreakpointFloat();
			r.value = x;
			r.controlNext = x;
			r.controlPrevious = x;
			return setValue(r);
		}
		case flat_next: {
			float x = now.value;
			BreakpointFloat r = new BreakpointFloat();
			r.value = x;
			r.controlNext = x;
			r.controlPrevious = x;
			return setValue(r);
		}
		default:
			return null;
		}
	}

	public BreakpointFloat setValue(BreakpointFloat to) {
		this.value = to.value;
		this.controlNext = to.controlNext;
		this.controlPrevious = to.controlPrevious;
		this.next = to.next;
		return this;
	}

	public double getDoubleValue() {
		return value;
	}

	@Override
	public String toString() {
		return "bpf: "+value+" "+controlNext+" "+controlPrevious+" "+next;
	}
	
}
