package field.util.filterstack;

import field.math.abstraction.iDoubleProvider;
import field.math.abstraction.iFilter;
import field.math.util.CubicTools;
import field.util.filterstack.Unit.WindowStatus;
import field.util.filterstack.Unit.iProvidesWindowStatus;

public class SimpleWindow implements iFilter<Double, Double>, iProvidesWindowStatus
{

	protected double duration;
	protected double middle;
	protected double height;

	public SimpleWindow() {
	}

	public SimpleWindow set(double duration, double middle, double height) {
		this.duration = duration;
		this.middle = middle;
		this.height = height;
		return this;
	}

	boolean first = true;
	private double startAt;
	WindowStatus status = WindowStatus.unknown;

	public Double filter(Double a) {

		if (first) {
			first = false;
			startAt = a;
		}

		double t = a - startAt;
		
		

		status = t/duration  > 1 ?  WindowStatus.finished : WindowStatus.ongoing;
		
		if (t / duration < middle) {
			double r = 1.0- height * CubicTools.cubic((float) ((t / duration) / middle), 1, 0, 1, 0);
			return r;
		} 
		else {
			double r = 1.0-height * CubicTools.cubic((float) Math.min(1, ((t / duration - middle) / (1 - middle))), 0, 1, 0, 1);
			return r;
		}
		
		
	}

	public WindowStatus getWindowState() {
		return status;
	}
	
}
