package field.math.util;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import field.math.BaseMath.MutableFloat;
import field.namespace.generic.Bind;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.ReflectionTools.Pair;
import field.util.ANSIColorUtils;

public class Histogram<T> implements iHistogram<T>, Serializable {
	Map<T, MutableFloat> counts = new HashMap<T, MutableFloat>();

	float normalization = 0;

	public <X> X average(iAverage<? super T, X> average) {
		double total = 0;
		Iterator<Entry<T, MutableFloat>> i = counts.entrySet().iterator();
		while (i.hasNext()) {
			Entry<T, MutableFloat> e = i.next();
			total += e.getValue().d;
		}
		average.begin(counts.size(), total);
		i = counts.entrySet().iterator();
		while (i.hasNext()) {
			Entry<T, MutableFloat> e = i.next();
			average.accept(e.getKey(), e.getValue().d / total);
		}

		return average.end();
	}

	public float get(T t, float def) {
		MutableFloat f = counts.get(t);
		return (float) (f == null ? def : f.d);
	}

	public Set<Entry<T, MutableFloat>> getEntries() {
		return counts.entrySet();
	}

	public float getEntropy() {
		float tot = 0;
		Iterator<T> c = counts.keySet().iterator();
		int num = 0;
		while (c.hasNext()) {
			T d = c.next();
			float p = getNormalized(d, 0);
			assert p <= 1;
			if (p != 0)
				tot += Math.log(p) * p;
			num++;
		}
		return (float) (-tot / Math.log(1f / num));
	}

	public float getNormalized(T t, float def) {
		return get(t, def) / normalization;
	}

	public int getNumBins() {
		return counts.size();
	}

	public Set<Entry<T, MutableFloat>> getSortedEntries() {
		TreeMap<T, MutableFloat> map = new TreeMap<T, MutableFloat>(new Comparator<T>() {

			public int compare(T o1, T o2) {
				MutableFloat m1 = counts.get(o1);
				MutableFloat m2 = counts.get(o2);
				int m = Double.compare(m1.d, m2.d);
				if (m == 0)
					return o1 == o2 ? 0 : 1;
				return m;
			}
		});
		map.putAll(counts);
		return map.entrySet();
	}

	public boolean has(T t) {
		return counts.containsKey(t);
	}

	public void incorporate(Histogram<T> h, float rawWeight, float normalizedWeight, float partialWeight) {
		float norm = h.normalization;
		Set<Entry<T, MutableFloat>> e = h.counts.entrySet();
		for (Entry<T, MutableFloat> entry : e) {
			float dd = (float) entry.getValue().d;
			this.visit(entry.getKey(), rawWeight * dd + normalizedWeight * dd / norm + partialWeight * normalization * dd / (norm * norm));
		}
	}

	public Histogram<T> mul(Histogram<T> h) {
		Set<Entry<T, MutableFloat>> e = h.counts.entrySet();
		for (Entry<T, MutableFloat> entry : e) {
			MutableFloat c = counts.get(entry.getKey());
			if (c != null) {
				c.d *= entry.getValue().d;
			}
		}

		counts.keySet().retainAll(h.counts.keySet());

		// redo the normalization

		Iterator<MutableFloat> c = counts.values().iterator();
		normalization = 0;
		while (c.hasNext()) {
			normalization += c.next().d;
		}

		return this;
	}


	public Histogram<T> mul(Histogram<T> h, float empty) {
		Set<Entry<T, MutableFloat>> e = counts.entrySet();
		for (Entry<T, MutableFloat> entry : e) {
			MutableFloat c = h.counts.get(entry.getKey());
			if (c != null) {
				entry.getValue().d *= c.d;
			}
			else
			{
				entry.getValue().d *= empty;
			}
		}

//		counts.keySet().retainAll(h.counts.keySet());

		// redo the normalization

		Iterator<MutableFloat> c = counts.values().iterator();
		normalization = 0;
		while (c.hasNext()) {
			normalization += c.next().d;
		}

		return this;
	}
	
	public Histogram<T> intersectAndAdd(Histogram<T> h) {
		Set<Entry<T, MutableFloat>> e = counts.entrySet();
		for (Entry<T, MutableFloat> entry : e) {
			MutableFloat c = h.counts.get(entry.getKey());
			if (c != null) {
				entry.getValue().d += c.d;
			}
		}

		counts.keySet().retainAll(h.counts.keySet());

		// redo the normalization

		Iterator<MutableFloat> c = counts.values().iterator();
		normalization = 0;
		while (c.hasNext()) {
			normalization += c.next().d;
		}

		return this;
	}


	public void multiplyWeightBy(T t, float by) {
		MutableFloat f = counts.get(t);
		if (f == null)
			f = new MutableFloat(0);
		normalization -= f.d;
		f.d *= by;
		normalization += f.d;
	}

	public String prettyPrint() {
		Set<Entry<T, MutableFloat>> es = counts.entrySet();
		float big = Float.NEGATIVE_INFINITY;
		Entry<T, MutableFloat> biggest = null;
		float norm = 0;
		for (Entry<T, MutableFloat> e : es) {
			if (e.getValue().floatValue() > big) {
				big = e.getValue().floatValue();
				biggest = e;
			}
			norm += e.getValue().floatValue();
		}
		String r = "";
		for (Entry<T, MutableFloat> e : es) {
			if (e == biggest) {
				r += ANSIColorUtils.red(e.getKey() + ":" + (e.getValue().floatValue() / norm)) + " ";
			} else {
				r += e.getKey() + ":" + (e.getValue().floatValue() / norm) + " ";
			}
		}
		return r;
	}

	public void remove(iFunction<Boolean, Pair<T, Number>> f) {
		Set<Entry<T, MutableFloat>> e = counts.entrySet();

		Iterator<Entry<T, MutableFloat>> ii = e.iterator();
		while (ii.hasNext()) {
			Entry<T, MutableFloat> n = ii.next();
			Boolean shouldRemove = f.f(new Pair<T, Number>(n.getKey(), n.getValue().d / normalization));
			if (shouldRemove) {
				normalization -= n.getValue().d;
				ii.remove();
			}
		}
	}

	public void removeKey(T t) {
		if (counts.containsKey(t)) {
			normalization -= counts.remove(t).d;
		}

	}

	public T select() {
		Iterator<Entry<T, MutableFloat>> i = counts.entrySet().iterator();
		double cum = 0;
		double v = (float) Math.random();
		while (i.hasNext()) {
			Entry<T, MutableFloat> e = i.next();
			cum += e.getValue().floatValue();
			if (v <= cum / normalization)
				return e.getKey();
		}
		assert false : counts + " " + normalization;
		return null;
	}
	
	public T best() {
		
		Entry<T, MutableFloat> a = Bind.argMax(counts.entrySet(), new iFunction<Double, Entry<T, MutableFloat>>() {
			@Override
			public Double f(Entry<T, MutableFloat> in) {
				return in.getValue().d;
			}
		});
		
		return a==null ? null : a.getKey();
	}

	public T select(float power) {

		double n1 = 0;
		for (MutableFloat m : counts.values()) {
			n1 += Math.pow(m.d, power);
		}

		double v = Math.random();
		double c = 0;
		for (Map.Entry<T, MutableFloat> m : counts.entrySet()) {
			c += Math.pow(m.getValue().d, power);
			if (v <= c / n1) {
				return m.getKey();
			}
		}
		return null;
	}

	public T selectOrNothing() {
		if (normalization == 0)
			return null;

		Iterator<Entry<T, MutableFloat>> i = counts.entrySet().iterator();
		float cum = 0;
		float v = (float) Math.random();
		while (i.hasNext()) {
			Entry<T, MutableFloat> e = i.next();
			cum += e.getValue().floatValue();
			if (v <= cum / normalization)
				return e.getKey();
		}
		return null;
	}

	@Override
	public String toString() {
		return "" + counts;
	}

	public float visit(T t, float p) {
		MutableFloat f = counts.get(t);
		if (f == null) {
			counts.put(t, f = new MutableFloat(p));
		} else
			f.d += p;
		normalization += p;

		return (float) (f.d / normalization);
	}

	public double getTotal() {
		double f = 0;
		for (MutableFloat ff : counts.values()) {
			f += ff.d;
		}
		return f;
	}

	public void renormalize() {
		for (MutableFloat ff : counts.values()) {
			ff.d /= normalization;
		}
		normalization = 1;

	}

}