package field.core.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

import field.core.dispatch.iVisualElement;
import field.core.plugins.python.OutputInserts;
import field.core.plugins.python.PythonPlugin.CapturedEnvironment;
import field.launch.iUpdateable;
import field.math.BaseMath;
import field.math.util.Histogram;


/**
 * for quickly grabbing statistics about numbers and things
 * @author marc
 *
 */
public class Taps {

	static public abstract class StatisticsPackage {

		abstract public void accept(Number n);

		abstract public String descriptiveString();

		abstract public void output(String key, CapturedEnvironment env, iVisualElement inside);
	}

	static public class DefaultStats extends StatisticsPackage {
		float min = Float.POSITIVE_INFINITY;

		float max = Float.NEGATIVE_INFINITY;

		float total = 0;

		int num;

		ArrayList<Float> rawData = new ArrayList<Float>();

		@Override
		public void accept(Number n) {
			if (n == null)
				return;
			float ff = n.floatValue();
			if (ff < min)
				min = ff;
			if (ff > max)
				max = ff;
			total += ff;
			num += 1;
			rawData.add(ff);
		}

		public float getMedian() {
			if (rawData.size() == 0)
				return Float.NaN;
			if (rawData.size() % 2 == 1)
				return rawData.get(rawData.size() / 2);
			return (rawData.get(rawData.size() / 2 - 1) + rawData.get(rawData.size() / 2));
		}

		public float getAverage() {
			return total / num;
		}

		public Histogram<Number> getHistogram(int numBins) {
			Histogram<Number> h1 = new Histogram<Number>();

			for (int i = 0; i < rawData.size(); i++) {
				float mm = rawData.get(i);
				mm = (float) (((long) mm * 1000) / 1000.0);
				h1.visit(mm, 1);
			}

			if (h1.getNumBins() > numBins * 2)

			{

				Histogram<Number> h = new Histogram<Number>();

				for (int i = 0; i < rawData.size(); i++) {
					float mm = rawData.get(i);
					int bin = (int) (numBins * (mm - min) / (1e-5 + max - min));
					float fbin = (float) (bin * (1e-5 + max - min) / numBins + min);
					h.visit(fbin, 1);
				}
				return h;
			}
			return h1;
		}

		@Override
		public String descriptiveString() {
			return BaseMath.toDP(min, 4) + " -> " + BaseMath.toDP(max, 4) + " a:" + BaseMath.toDP(getAverage(), 4) + " m:" + BaseMath.toDP(getMedian(), 4) + " n:" + num;
		}

		@Override
		public void output(String key, CapturedEnvironment env, iVisualElement inside) {
			OutputInserts.printHistogram(key + ": " + descriptiveString(), key, inside, getHistogram(15));
		}
	}

	static WeakHashMap<Object, Map<String, StatisticsPackage>> stats = new WeakHashMap<Object, Map<String, StatisticsPackage>>();

	static public void tap(Number n, Object executionUID, String key) {
		StatisticsPackage p = getPackage(executionUID, key);
		p.accept(n);

	}

	static public void tapAndPrint(Number n, final Object executionUID, final String key, final CapturedEnvironment env, final iVisualElement inside) {
		if (!env.hasExitHandler(key)) {
			env.addExitHandler(key, new iUpdateable() {
				public void update() {
					if (hasPackage(executionUID, key)) {
						StatisticsPackage p = getPackage(executionUID, key);
						p.output(key, env, inside);

					}
				}
			});
		}
		tap(n, executionUID, key);
	}

	static public void finish(Object executionUID) {
		stats.remove(executionUID);
	}

	public static boolean hasPackage(Object executionUID, String key) {
		if (stats.get(executionUID) == null)
			return false;
		if (!stats.get(executionUID).containsKey(key))
			return false;
		return true;
	}

	public static StatisticsPackage getPackage(Object executionUID, String key) {
		Map<String, StatisticsPackage> map = getMap(executionUID);
		StatisticsPackage sp = map.get(key);
		if (sp == null) {
			map.put(key, sp = new DefaultStats());
		}
		return sp;
	}

	private static Map<String, StatisticsPackage> getMap(Object executionUID) {
		Map<String, StatisticsPackage> m = stats.get(executionUID);
		if (m == null)
			stats.put(executionUID, m = new WeakHashMap<String, StatisticsPackage>());
		return m;
	}

}
