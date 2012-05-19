package field.math.abstraction;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.WeakHashMap;
import java.util.Map.Entry;

import field.launch.iUpdateable;


public abstract class WhiteBlock implements iUpdateable {

	// output to input
	WeakHashMap<MappedProvider, iFloatProvider> map = new WeakHashMap<MappedProvider, iFloatProvider>();

	boolean needEvaluate = true;

	public iFloatProvider map(iFloatProvider input) {
		MappedProvider provider = new MappedProvider();
		needEvaluate = true;

		map.put(provider, input);
		return provider;
	}

	public void update() {

		MappedProvider[] values = new MappedProvider[map.size()];
		Iterator<Entry<MappedProvider, iFloatProvider>> i = map.entrySet().iterator();
		int n = 0;
		while (i.hasNext()) {
			Entry<MappedProvider, iFloatProvider> e = i.next();
			values[n] = e.getKey();
			values[n].rawValue = e.getValue().evaluate();
			n++;
		}

		if (n != values.length) {
			MappedProvider[] values2 = new MappedProvider[n];
			System.arraycopy(values, 0, values2, 0, n);
			values2 = values;
		}

		Arrays.sort(values);

		filter(values);
		
		needEvaluate = false;
	}

	protected void filter(MappedProvider[] values) {
		if (values.length == 0) return;

		float min = values[0].rawValue;
		float max = values[values.length - 1].rawValue;
		float median = values.length % 2 == 1 ? values[values.length / 2].rawValue : (values[values.length / 2].rawValue + values[values.length / 2 - 1].rawValue) / 2;

		for (int i = 0; i < values.length; i++) {
			filter(values[i], i, values.length, max, min, median);
		}
	}

	abstract protected void filter(MappedProvider provider, int i, int length, float max, float min, float median);

	public class MappedProvider implements iFloatProvider, Comparable<MappedProvider> {
		float value;

		float rawValue;

		public float evaluate() {
			if (needEvaluate) update();
			return value;
		}

		public int compareTo(MappedProvider o) {
			return o.rawValue > rawValue ? -1 : o.rawValue == rawValue ? 0 : 1;
		}
	}

	static public class StandardWhite extends WhiteBlock {
		private final iFloatProvider robustAmount;

		private final iFloatProvider robustPower;

		private final iFloatProvider meanPower;

		private final iFloatProvider min;

		private final iFloatProvider max;

		public StandardWhite(iFloatProvider robustAmount, iFloatProvider robustPower, iFloatProvider meanPower, iFloatProvider min, iFloatProvider max) {
			this.robustAmount = robustAmount;
			this.robustPower = robustPower;
			this.meanPower = meanPower;
			this.min = min;
			this.max = max;
		}

		@Override
		protected void filter(MappedProvider provider, int i, int length, float max, float min, float median) {

			float robust = i / (float) (length - 1);
			robust = (float) Math.pow(robust, robustPower.evaluate());

			float mean = (provider.rawValue - min) / (max - min);
			if (max - min == 0) mean = 0;
			mean = (float) Math.pow(mean, meanPower.evaluate());

			float alpha = robustAmount.evaluate();
			provider.value = robust * alpha + (1 - alpha) * mean;
			
			float mi =this.min.evaluate();
			float mx = this.max.evaluate();
			
			provider.value = provider.value*(mx-mi)+mi;
		}

	}

	public void debugToString(PrintStream out) {
		Iterator<Entry<MappedProvider, iFloatProvider>> i = map.entrySet().iterator();
		int n = 0;
		out.println(" white block <"+this.getClass()+"> has <"+map.size()+">");
		while (i.hasNext()) {
			Entry<MappedProvider, iFloatProvider> e = i.next();
			out.println(e.getKey().rawValue+" -> "+e.getKey().value+" ("+e.getValue()+")");
		}
	}

}
