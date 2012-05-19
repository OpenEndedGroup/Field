package field.graph;

import java.util.ArrayList;
import java.util.List;

import field.core.plugins.drawing.opengl.CachedLine;
import field.math.util.CubicTools;
import field.util.Dict.Prop;

public class FLineInterpolator {

	public FLineInterpolator() {
	}

	List<CachedLine> previousOutput = new ArrayList<CachedLine>();

	CachedLineInterpolator ongoingInterpolator;
	int duration;
	int tick;

	public List<CachedLine> update() {
		if (ongoingInterpolator == null)
			return previousOutput;

		tick++;

		if (tick <= duration) {
			previousOutput = ongoingInterpolator.blend(shape(tick / (float) duration));
		}

		if (tick == duration) {
			ongoingInterpolator = null;
		}

		return previousOutput;
	}

	public boolean isDoingWork() {
		return ongoingInterpolator != null;
	}

	private float shape(float f) {
		return CubicTools.smoothStep(f);
	}

	public void setTarget(List<CachedLine> next, String hash, int duration) {
		ongoingInterpolator = new CachedLineInterpolator(previousOutput, next, new Prop<String>(hash));
		previousOutput = ongoingInterpolator.blend(shape(0));
		this.duration = duration;
		tick = 0;
	}

}
