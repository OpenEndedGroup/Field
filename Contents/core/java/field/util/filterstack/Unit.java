package field.util.filterstack;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.AliasedParameter;
import field.bytecode.protect.annotations.Aliases;
import field.math.abstraction.iFilter;

/**
 * alpha, beta, gamma?
 * 
 * well, three ways of blending a unit: alpha: blending with the input, beta: blending with the last output that this unit had, gamma: blending with the last output that the whole filterstack had if these are zero or not present, no blending happens if these are 1 then this unit doesn't move
 */
@Woven
public abstract class Unit<T> {

	public enum WindowStatus {
		unknown, finished, ongoing, error;

		public WindowStatus combine(WindowStatus w) {
			if (w == null)
				return this;
			if (this.ordinal() < w.ordinal())
				return w;
			return this;
		}
	}

	public interface iProvidesWindowStatus {
		public WindowStatus getWindowState();
	}

	protected T input;

	protected T blendInput;

	String name;

	protected String inputName;

	protected String blendInputName;

	protected String outputName;

	protected T lastOutput;

	protected T lastTotalOutput;

	protected iFilter<Double, Double> alpha;

	protected iFilter<Double, Double> beta;

	protected iFilter<Double, Double> gamma;

	@AliasedParameter
	protected FilterStack<T> filterStack;

	protected double timeNow;
	protected double previousTime;
	protected boolean firstUpdate = true;
	

	public Unit(String name) {
		this.name = name;
	}

	@Aliases
	public void update(double time) {

		timeNow = time;
		
		System.out.println(" update <"+this+"> inside <"+filterStack+">");
		
		double ea = alpha == null ? 0 : alpha.filter(time);
		double eb = beta == null ? 0 : beta.filter(time);
		double eg = gamma == null ? 0 : gamma.filter(time);

		if (lastOutput == null)
			eb = 0;
		if (lastTotalOutput == null)
			eg = 0;

		T o = filter(input);

		System.out.println(" this <" + this + "> in <" + input + "> raw out <" + o + "> inside <" + filterStack + "> <"+lastOutput+"> <"+lastTotalOutput+">");
		System.out.println("        params <" + ea + " " + eb + " " + eg + ">");

		T eo = null;
		if (o == null) {
			eo = input;
		} else {
			eo = filterStack.blend(filterStack.blend(filterStack.blend(o, blendInput, ea), lastOutput, eb), lastTotalOutput, eg);
		}

		lastOutput = eo;
		previousTime = timeNow;
		firstUpdate = false;
	}

	abstract protected T filter(T input);

	protected void cull() {
		filterStack.removeUnit(this);
	}

	protected WindowStatus getAccumulativeWindowStatus() {
		WindowStatus w = WindowStatus.unknown;
		
		if (this instanceof iProvidesWindowStatus)
			w = w.combine(((iProvidesWindowStatus)this).getWindowState());
		
		if (alpha != null && alpha instanceof iProvidesWindowStatus)
			w = w.combine(((iProvidesWindowStatus) alpha).getWindowState());
		if (beta != null && beta instanceof iProvidesWindowStatus)
			w = w.combine(((iProvidesWindowStatus) beta).getWindowState());
		if (gamma != null && gamma instanceof iProvidesWindowStatus)
			w = w.combine(((iProvidesWindowStatus) gamma).getWindowState());
		return w;
	}
	
	public String getName() {
		return name;
	}
	
}
