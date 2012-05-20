package field.extras.scrubber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import field.core.network.OSCInput;
import field.core.network.OSCInput.Handler;
import field.namespace.generic.Generics.Pair;

public class OSCClockSource {

	private final OSCInput input;

	boolean ballistic = false;

	public OSCClockSource(OSCInput input) {
		this.input = input;

		input.registerHandler("float", new Handler() {
			public void handle(float f) {
				if (!ballistic) {
					;//;//System.out.println(" time -- <" + f + ">");
					OSCClockSource.this.time(f * 1000);
				} else {
					// clock drift.
				}
			}
		});
	}

	List<Pair<Float, Long>> times = new ArrayList<Pair<Float, Long>>();

	private Long zero;

	float lastf = 0;
	protected void time(float f) {

		if (ballistic)
			return;

		if (f > lastf) {
			times.add(new Pair<Float, Long>(f, System.currentTimeMillis()));
			if (times.size() == 20) {
				estimateAndGoBallistic();
			}
			lastf = f;
		}
		else if (f<lastf)
		{
			times.clear();
			lastf = f;
		}
	}

	private void estimateAndGoBallistic() {
		
		List<Long> zeroTimes = new ArrayList<Long>();
		for(Pair<Float, Long> t : times)
		{
			long z = (long) (t.right-t.left);
			zeroTimes.add(z);
		}
		Collections.sort(zeroTimes);
		zero = zeroTimes.get(zeroTimes.size()/2);
		
		System.err.println(" << clock is now ballistic >>");
		ballistic = true;
	}
	
	public long getTime()
	{
		if (!ballistic)
		{
			return 0;
		}
		return System.currentTimeMillis()-zero;
	}

}
