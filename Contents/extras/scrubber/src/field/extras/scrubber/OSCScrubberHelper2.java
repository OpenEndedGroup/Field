package field.extras.scrubber;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElementOverrides;
import field.core.network.OSCInput;
import field.core.network.OSCInput.Handler;
import field.launch.iUpdateable;

public class OSCScrubberHelper2 implements iUpdateable {

	private final OSCInput input;
	private iVisualElement target;

	float start = 0;
	float stop = 1000;

	float base = 1;// in ms
	float lead = 10; // in pixels

	float inputPlayStart = 0;
	float inputPlayStop = 0;

	public OSCScrubberHelper2(OSCInput input) {
		this.input = input;

		input.registerHandler("float", new Handler() {
			public void handle(float f) {
				;//;//System.out.println(" time -- <" + f + ">");
				OSCScrubberHelper2.this.time(f*1000);
			}
		});
	}

	public OSCScrubberHelper2 setTarget(iVisualElement target) {
		this.target = target;
		return this;
	}

	public OSCScrubberHelper2 setPlayRange(float start, float stop) {
		this.start = start;
		this.stop = stop;
		return this;
	}

	long timeAt = 0;
	double timeIs = 0;

	int ticksSinceUpdate = 100;

	boolean ongoing = true;

	boolean ballistic = false;
	int ballisticNext = 0;
	
	public void update() {
		
		if (ballistic && ongoing)
		{
			double adjTimeIs = (System.currentTimeMillis() - timeAt) * base + timeIs;

			double f = (adjTimeIs - inputPlayStart) / (inputPlayStop - inputPlayStart);

			if (f < 0)
				f = 0;
			if (f > 1)
				f = 1;

			double now = start + f * (stop - start);

			if (target != null) {
				Rect m = target.getFrame(null);
				if (Math.abs(m.x - (now + lead)) > 1e-2) {
					m.x = now + lead;

					setFrame(target, m);
				}
			}
			
		}
		else
		if (ticksSinceUpdate++ < 10 && ongoing) {

			double adjTimeIs = (System.currentTimeMillis() - timeAt) * base + timeIs;

			double f = (adjTimeIs - inputPlayStart) / (inputPlayStop - inputPlayStart);

			if (f < 0)
				f = 0;
			if (f > 1)
				f = 1;

			double now = start + f * (stop - start);

			if (target != null) {
				Rect m = target.getFrame(null);
				if (Math.abs(m.x - (now + lead)) > 1e-2) {
					m.x = now + lead;

					setFrame(target, m);
				}
			}

		}
	}

	public double convertOSCToPixels(double timeIs) {
		double adjTimeIs = timeIs;

		double f = (adjTimeIs - inputPlayStart) / (inputPlayStop - inputPlayStart);

		return start+f*(stop-start);
	}
	
	public double convertPixelsToOSC(double pixels)
	{
		
		return (inputPlayStop-inputPlayStart)*(pixels-start)/(stop-start)+inputPlayStart;
		
	}

	protected void setFrame(iVisualElement target, Rect m) {

		System.err.println("\n{{{{{{{{{{{{{{{{{{{{{{{{{{ set frame to be <"+m+"> (from <"+target.getFrame(null)+">)\n");

		// todo \u2014 filter output here, let's just use a Unit<T> thingy
		
		iVisualElementOverrides.topology.begin(target);
		iVisualElementOverrides.forward.shouldChangeFrame.shouldChangeFrame(target, m, target.getFrame(null), true);
		iVisualElementOverrides.topology.end(target);
	}

	public void stopTracking() {
		ongoing = false;
		ticksSinceUpdate = 100;
		ballisticNext = 0;
	}

	public void startTracking() {
		ongoing = true;
	}

	public void startTrackingBallistally() {
		ongoing = true;
		ballisticNext = 10;
	}

	protected void time(float f) {

		if (ballistic)
			return;
		
		if (timeIs != f)
			ticksSinceUpdate = 0;
		timeAt = System.currentTimeMillis();
		timeIs = f;
		
		// ticksSinceUpdate = 0;
		if (--ballisticNext==0)
		{
			ballistic = true;
		}
	}

}
