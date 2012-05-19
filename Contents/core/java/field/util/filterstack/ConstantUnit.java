package field.util.filterstack;

import field.math.abstraction.iFilter;
import field.math.abstraction.iFloatProvider;

public class ConstantUnit<T> extends Unit<T> {

	public ConstantUnit(String name, T constant) {
		super(name);
		setConstant(constant);
	}

	T constant;

	public ConstantUnit<T> setConstant(T constant) {
		this.constant = constant;
		return this;
	}

	public ConstantUnit<T> setAlpha(iFilter<Double, Double> alpha) {
		this.alpha = alpha;
		return this;
	}

	public ConstantUnit<T> setBeta(iFilter<Double, Double> beta) {
		this.beta = beta;
		return this;
	}

	public ConstantUnit<T> setGamma(iFilter<Double, Double> gamma) {
		this.gamma= gamma;
		return this;
	}
	
	public ConstantUnit<T> setBlendInputName(String name)
	{
		this.blendInputName = name;
		return this;
	}

	public ConstantUnit<T> setBlendOutputName(String name)
	{
		this.outputName = name;
		return this;
	}

	@Override
	protected T filter(T input) {
		return constant;
	}

}
