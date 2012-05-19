package field.context;

import field.context.Generator.Channel;
import field.launch.iUpdateable;
import field.math.abstraction.iDoubleProvider;

public abstract class Horizon<X> implements iUpdateable{

	
	protected Channel<X> c;
	private iDoubleProvider time;
	private double history;

	double previous = Double.NEGATIVE_INFINITY;
	public Horizon(Channel<X> c, iDoubleProvider time)
	{
		this.c = c;
		this.time = time;
	}
	
	@Override
	public void update() {
		double t = time.evaluate();
		process(t, c.range(previous, t-history));
		previous = t;
	}

	abstract protected void process(double t, Channel<X> range);
}
