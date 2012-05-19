package field.core.execution;

import java.util.ArrayList;
import java.util.List;

import field.launch.SystemProperties;
import field.math.abstraction.iDoubleProvider;
import field.math.abstraction.iProvider;
import field.util.filterstack.FilterStack;

/**
 *
 * @author marc
 *
 */
public class TimeSystem implements iDoubleProvider {

	class Manipulation {
		boolean started = false;

		double deltaProc;

		double deltaEx;

		double startedAtProc;

		public Manipulation(float deltaProcessingTime, float deltaExecutionTime) {
			this.deltaProc = deltaProcessingTime;
			this.deltaEx = deltaExecutionTime;
		}

		public void ensureStart() {
			if (!started) {
				this.startedAtProc = processingTimeNow;
				started = true;
			}
		}
	}
	static public boolean isRealTime = false;

	public boolean thisIsRealTime = isRealTime;

	private final FilterStack<Double> processingTime_rate;

	private final FilterStack<Double> processingTime;

	private boolean supressNewStack;

	double processingTime_rate_root;

	double processingTime_root;

	double baseTime = 1;

	double processingTimeNow;

	double executionTimeNow;

	long lastTime = 0;

	boolean twentyFrames = SystemProperties.getIntProperty("fixedFrame", 0) == 1;

	List<Manipulation> manipulations = new ArrayList<Manipulation>();

	boolean supressNewStackAdditions = false;

	boolean clearOnce = false;

	public TimeSystem() {
		processingTime = new FilterStack<Double>(new iProvider<Double>() {
			public java.lang.Double get() {
				return processingTime_root;
			}
		}, new iDoubleProvider() {
			public double evaluate() {
				return processingTime_root;
			}
		}) {

			@Override
			public java.lang.Double addImpl(java.lang.Double a, double w, java.lang.Double to) {
				return to + a * w;
			}

			@Override
			protected java.lang.Double blendImpl(java.lang.Double a, java.lang.Double b, double ea) {
				return a * (1 - ea) + ea * b;
			}
		};

		processingTime_rate = new FilterStack<Double>(new iProvider<Double>() {
			public java.lang.Double get() {
				return new Double(thisIsRealTime ? timeElapsed() : baseTime);
			}
		}, new iDoubleProvider() {
			public double evaluate() {
				return processingTime_rate_root;
			}
		}) {

			@Override
			public java.lang.Double addImpl(java.lang.Double a, double w, java.lang.Double to) {
				return to + a * w;
			}

			@Override
			protected java.lang.Double blendImpl(java.lang.Double a, java.lang.Double b, double ea) {
				return a * (1 - ea) + ea * b;
			}
		};

	}

	public double evaluate() {
		return executionTimeNow;
	}

	public double getExecutionTime() {
		return executionTimeNow;
	}

	public double getProcessingTime() {
		return processingTimeNow;
	}

	public FilterStack<Double> getRateFilterStack() {
		return processingTime_rate;
	}

	public FilterStack<Double> getTimeFilterStack() {
		return processingTime;
	}

	public boolean isSpeculative() {
		return false;
	}

	public boolean setSuppressNewStackAdditions(boolean supress) {
		boolean was = this.supressNewStack;
		this.supressNewStack = supress;
		return was;
	}

	public void supplyTimeManipulation(float deltaProcessingTime, float deltaExecutionTime) {
		if (supressNewStack) {
			if (manipulations.size() == 0)
				manipulations.add(new Manipulation(deltaProcessingTime, deltaExecutionTime));
		} else {
			if (clearOnce) {
				manipulations.clear();
				clearOnce = false;
			}
			manipulations.add(new Manipulation(deltaProcessingTime, deltaExecutionTime));
		}
	}

	public boolean update() {

		processingTime_rate_root += 1;
		processingTime_rate.update();

		processingTime_root += processingTime_rate.get();

		double timeWas = processingTimeNow;
		processingTime.update();
		processingTimeNow = processingTime.get();

		clearOnce = true;

//		System.err.println(" -- updating <" + manipulations + "> at <" + processingTimeNow + "> with rate <" + processingTime_rate.get() + "> output is <"+executionTimeNow+">");
		// now lets look at the stack
		if (manipulations.size() == 0)
			return true;

		Manipulation m = manipulations.get(0);
		if (m.deltaProc == 0) {
			executionTimeNow += m.deltaEx;
			manipulations.remove(0);
			return false;
		} else {
			m.ensureStart();

			double a1 = (processingTimeNow - m.startedAtProc);
			double a2 = (timeWas - m.startedAtProc);

			System.err.println("@@@@@@@@@@@@@@@@@@@@@@@@ a1 =" + a1 + " a2 =" + a2);

			if (a1 > m.deltaProc)
				a1 = m.deltaProc;
			else if (a1 < 0)
				a1 = 0;

			if (a2 > m.deltaProc)
				a2 = m.deltaProc;
			else if (a2 < 0)
				a2 = 0;

			double change = m.deltaEx * (a1 - a2) / m.deltaProc;

			System.err.println(" change is <" + change + "> -- looking for a total change of <" + m.deltaEx + "> over <" + m.deltaProc + "> and we are at <" + a1 + ">\n   executionTimeNow was <" + executionTimeNow + ">");

			executionTimeNow += change;

			System.err.println("    and now is  <" + executionTimeNow + ">");

			if (a1 == m.deltaProc) {
				manipulations.remove(0);
			}
			return false;
		}

	}

	private double timeElapsed() {

		if (twentyFrames) {
			long timeNow = System.currentTimeMillis();

			System.err.println(" time elapsed is <" + (timeNow - lastTime) + ">");

			double inc = 0;
			inc = (50f) / (33d + 5);
			lastTime = timeNow;
			return inc;

		} else {

			long timeNow = System.currentTimeMillis();

			System.err.println(" \n\n\n\n\n\n\n\n time elapsed is <" + (timeNow - lastTime) + "> \n\n\n\n\n\n\n\n\n");

			double inc = 0;
			inc = (timeNow - lastTime) / (1d);
			lastTime = timeNow;
			return inc;
		}
	}

}
