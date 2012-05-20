package field.math.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public interface iAverage<T_accepts, T_produces> {

	static public class Best<T_accepts> implements iAverage<T_accepts, T_accepts> {
		private double best;

		private T_accepts bestIs;

		public <X extends T_accepts> void accept(X accept, double weight) {
			if (weight > best) {
				best = weight;
				bestIs = accept;
			}
		}

		public void begin(int num, double totalWeight) {
			bestIs = null;
			best = Float.NEGATIVE_INFINITY;
		}

		public T_accepts end() {
			return bestIs;
		}
	}

	static public class ChiSquared<T2_accepts> {
		public float chiSquared(iHistogram<T2_accepts> one, iHistogram<T2_accepts> two) {
			final Map<T2_accepts, Double> left = new HashMap<T2_accepts, Double>();
			one.average(new iAverage<T2_accepts, Float>(){
				public <X extends T2_accepts> void accept(X accept, double weight) {
					left.put(accept, weight);
				}

				public void begin(int num, double totalWeight) {
				}

				public Float end() {
					return 0f;
				}
			});

			return two.average(new iAverage<T2_accepts, Float>(){

				double total = 0;

				public <X extends T2_accepts> void accept(X accept, double weight) {
					Double f = left.get(accept);
					if (f == null || f == 0) {
						total += (weight == 0 ? 0 : 1);
					} else {
						total += Math.abs(f - weight) / (f + weight);
					}
				}

				public void begin(int num, double totalWeight) {
				}

				public Float end() {
					return (float) total;
				}
			});
		}
	}

	static public class DistanceFrom implements iAverage<Number, Float> {
		private float center;

		private float radius;

		private float priorCenter;

		private double totalWeight;

		private float totalDistance;

		private float priorWeight;

		private double totalAmount;

		public <X extends Number> void accept(X accept, double weight) {

			float d = Math.abs(accept.floatValue() - center);
			float w = (float) Math.exp(-d * d / (radius * radius));

			totalWeight += w * weight / totalAmount;
			totalDistance += w * d * weight / totalAmount;
		}

		public void begin(int num, double totalWeight) {
			this.totalAmount = totalWeight;
			totalDistance = 0;
			this.totalWeight = 0;
		}

		public Float end() {

			float d = Math.abs(center - priorCenter);
			float w = priorWeight;

			totalWeight += w;
			totalDistance += w * d;

			return (float) (totalDistance / totalWeight);
		}

		public DistanceFrom setCenter(float center) {
			this.center = center;
			return this;
		}

		public DistanceFrom setPriorCenter(float priorCenter) {
			this.priorCenter = priorCenter;
			return this;
		}

		public DistanceFrom setPriorWeight(float priorWeight) {
			this.priorWeight = priorWeight;
			return this;
		}

		public DistanceFrom setRadius(float radius) {
			this.radius = radius;
			return this;
		}
	}

	static public class NumberAverage implements iAverage<Number, Float> {

		private float value;

		private float weight;

		private double totalWeight;

		float t = 0;

		public <X extends Number> void accept(X accept, double weight) {
			t += accept.floatValue() * weight;
		}

		public void begin(int num, double totalWeight) {
			this.totalWeight = totalWeight;
			t = 0;
		}

		public Float end() {
			t += value;
			if (weight == 0) {
				if (totalWeight == 0) return value;
			}
			t /= (1 + weight);
			return t;
		}

		public NumberAverage setDefault(float value, float weight) {
			this.value = value;
			this.weight = weight;
			return this;
		}
	}

	public class OrderedPrint<X extends Comparable<X>> implements iAverage<X, X> {
		TreeMap<X, Double> map = new TreeMap<X, Double>();

		public <Z extends X> void accept(Z accept, double weight) {
			map.put(accept, weight);
		}

		public void begin(int num, double totalWeight) {
			map = new TreeMap<X, Double>();
		}

		public X end() {
			for(Map.Entry<X, Double> e : map.entrySet())
			{
				;//System.out.println(" accepted <"+e.getKey()+" ->"+e.getValue()+">");
			}
			return null;
		}
	}

	static public class Print<T_accepts> implements iAverage<T_accepts, T_accepts> {
		public <X extends T_accepts> void accept(X accept, double weight) {
			;//System.out.println(" accepted <" + accept + "> at <" + weight + ">");
		}

		public void begin(int num, double totalWeight) {
		}

		public T_accepts end() {
			return null;
		}
	}

	static public class VarianceFrom<X extends Number> {
		public float totalWeightedVariance(iHistogram<X> histogram, float radius, float priorCenter, float priorWeight) {
			final Map<X, Float> left = new HashMap<X, Float>();
			histogram.average(new iAverage<X, Float>(){
				public <Y extends X> void accept(Y accept, double weight) {
					left.put(accept, (float) weight);
				}

				public void begin(int num, double totalWeight) {
				}

				public Float end() {
					return 0f;
				}
			});
			Iterator<Entry<X, Float>> i = left.entrySet().iterator();
			float total = 0;
			float totalWeight = 0;
			while (i.hasNext()) {
				Entry<X, Float> e = i.next();
				Float d = histogram.average(new DistanceFrom().setCenter(e.getKey().floatValue()).setRadius(radius).setPriorCenter(priorCenter).setPriorWeight(priorWeight));
				total += e.getValue() * d;


				totalWeight += e.getValue();
			}
			if (totalWeight < 1e-4) return 0;
			return total / totalWeight;
		}
	}

	public <X extends T_accepts> void accept(X accept, double weight);

	public void begin(int num, double totalWeight);

	public T_produces end();


}
