package field.context;

import field.context.Generator.Channel;
import field.launch.iUpdateable;
import field.math.abstraction.iDoubleProvider;

public class Culler<X> implements iUpdateable{

	
	private Channel<X> c;
	private iDoubleProvider time;
	private double history;

	public Culler(Channel<X> c, iDoubleProvider time, double historyLength)
	{
		this.c = c;
		this.time = time;
		this.history = historyLength;
	}
	
	@Override
	public void update() {
		c.range(Float.NEGATIVE_INFINITY, time.evaluate()-history).clear();
	}

}
