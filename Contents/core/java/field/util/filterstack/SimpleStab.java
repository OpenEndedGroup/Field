package field.util.filterstack;


public class SimpleStab<T> extends ConstantUnit<T> {

	private final double duration;

	public SimpleStab(String name, double duration, T constant) {
		super(name, constant);
		this.duration = duration;

		setAlpha(new SimpleWindow().set(duration, 0.5f, 1));
		//setGamma(new SimpleWindow().set(duration, 0.5f, 1));
	}

	public SimpleStab(String name, double duration, T constant, float middle) {
		super(name, constant);
		this.duration = duration;

		setAlpha(new SimpleWindow().set(duration,  middle, 1));
		//setGamma(new SimpleWindow().set(duration, 0.5f, 1));
	}

	double firstTime = -1;

	boolean first = true;

	@Override
	protected T filter(T input) {

		double t = filterStack.getTime();

		if (first) {
			firstTime = t;
		}

		WindowStatus status = getAccumulativeWindowStatus();
		if (status.ordinal() == WindowStatus.finished.ordinal()) {
			cull();
		}
		return super.filter(input);
	}

}
