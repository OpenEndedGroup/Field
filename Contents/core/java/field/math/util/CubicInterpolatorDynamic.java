package field.math.util;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import field.math.abstraction.iBlendable;
import field.math.abstraction.iTemporalFunction;
import field.namespace.generic.Bind.iFunction;

public class CubicInterpolatorDynamic<T extends iBlendable<T>> implements iTemporalFunction<T>, Serializable {

	// static private final long serialVersionUID = -119976144885796527L;
	static private final long serialVersionUID = 494798624173987102L;

	public class Sample implements Serializable {

		static private final long serialVersionUID = -4587582997695641410L;

		public T data;

		public float time;

		int generation;

		public Sample(T data, float time) {
			this.data = data;
			this.time = time;

			int index = Collections.binarySearch(samples, this, comparator);
			if (index < 0)
				index = -index - 1;
			samples.add(index, this);

			cacheInvalid = true;
		}

		protected Sample() {
		}

		@Override
		public String toString() {
			return data + "@" + time;
		}

	}

	public final class MComparator implements Comparator<Sample>, Serializable {

		static private final long serialVersionUID = 3550071215193353975L;

		public int compare(Sample s1, Sample s2) {

			return s1.time < s2.time ? -1 : s1.time > s2.time ? 1 : 0;
		}
	}

	private boolean extrapolation = false;

	ArrayList<Sample> samples = new ArrayList<Sample>();

	Comparator<Sample> comparator = new MComparator();

	// getting values
	boolean cacheInvalid = true;

	float now = Float.POSITIVE_INFINITY;

	float next = Float.NEGATIVE_INFINITY;

	float before = Float.POSITIVE_INFINITY;

	float after = Float.NEGATIVE_INFINITY;

	int indexNow = -1;

	int indexBefore = 0;

	int indexAfter = 0;

	int indexNext = 0;

	float duration = 0;

	boolean linear = false;

	Sample temp = new Sample();

	public CubicInterpolatorDynamic() {
	}

	public CubicInterpolatorDynamic<T> copy() {
		CubicInterpolatorDynamic<T> ret = new CubicInterpolatorDynamic<T>();

		for (int i = 0; i < getNumSamples(); i++) {
			Sample s = getSample(i);
			ret.new Sample(s.data, s.time);
		}

		return ret;
	}

	public T debugGet(float alpha) {
		if (getNumSamples() == 0)
			return null;
		Sample sample = getSample(0);
		T data = sample.data;

		T t = data.blendRepresentation_newZero();
		debugGetValue(alpha, t);

		return t;
	}

	public boolean debugGetValue(float time, T value) {

		System.err.println(" num samples are <" + samples.size() + ">");

		if (samples.size() < 1)
			return false;

		if (samples.size() == 1)
			value.setValue(getSample(0).data);

		validateCache(time);

		float a = (time - now) / (duration);

		System.err.println(" time <" + time + "> <" + now + "> <" + duration + ">");

		if (Float.isNaN(a) || Float.isInfinite(a))
			a = 0.5f;

		if (!extrapolation) {
			if (a > 1)
				a = 1;
			if (a < 0)
				a = 0;

			System.err.println(" clamping to <" + a + ">");

		} else {
			if (a < 0) {
				indexNow = 0;
				indexNext = 1;

			} else if (a > 1) {
				indexNow = samples.size() - 2;
				indexNext = samples.size() - 1;
			}

			if (indexNow == indexNext && indexNow != 0) {
				indexNow--;
				indexBefore--;
				if (indexBefore < 0)
					indexBefore = 0;
				a += 1;
			}

			System.err.println(" extrapolating <" + getSample(indexNow).data + "> <" + getSample(indexNext).data + "> <" + indexNow + "> <" + indexNext + "> <" + a + ">");

			value.lerp(getSample(indexNow).data, getSample(indexNext).data, a);
			return true;

		}

		// if (a>1)
		// {
		// System.err.println(" trying to extrapolate <"+time+"> <"+now+"> <"+duration+"> <"+a+"> <"+indexBefore+"> <"+indexNow+"> <"+indexNext+"> <"+indexAfter+">");
		// }

		// System.err.println("          "+a+" "+getSample(indexBefore)+" "+getSample(indexNow)+" "+getSample(indexNext)+" "+getSample(indexAfter));

		if (linear) {
			System.err.println(" is linear <" + indexNow + "> <" + indexNext + "> <" + getSample(indexNow) + "> <" + getSample(indexNext) + ">");

			value.lerp(getSample(indexNow).data, getSample(indexNext).data, a);

			return true;
		}

		System.err.println(" cerp <" + getSample(indexBefore).data + " " + getSample(indexBefore).time + " " + getSample(indexNow).data + " " + getSample(indexNow).time + " " + getSample(indexNext).data + " " + getSample(indexNext).time + " " + getSample(indexAfter).data + " " + getSample(indexAfter).time + " " + a + ">");

		value.cerp(getSample(indexBefore).data, getSample(indexBefore).time, getSample(indexNow).data, getSample(indexNow).time, getSample(indexNext).data, getSample(indexNext).time, getSample(indexAfter).data, getSample(indexAfter).time, a);

		System.err.println(" value = " + value);
		return true;
	}

	public void dirty() {
		this.cacheInvalid = false;
		now = Float.POSITIVE_INFINITY;
		next = Float.NEGATIVE_INFINITY;
		before = Float.POSITIVE_INFINITY;
		after = Float.NEGATIVE_INFINITY;
	}

	public CubicInterpolatorDynamic<T> extrapolate() {
		extrapolation = true;
		return this;
	}

	public int findSampleIndexAfter(float from) {
		temp.time = from;
		int n = Collections.binarySearch(samples, temp, comparator);
		if (n < 0)
			n = -n - 1;
		else
			n++;
		return n;
	}

	public int findSampleIndexBefore(float from) {
		temp.time = from;
		int n = Collections.binarySearch(samples, temp, comparator);
		if (n < 0)
			n = -n - 2;
		else
			n--;
		return n;
	}

	public T get(float alpha) {
		if (getNumSamples() == 0)
			return null;
		Sample sample = getSample(0);
		T data = sample.data;

		T t = data.blendRepresentation_newZero();
		getValue(alpha, t);

		return t;
	}

	public float getDomainMax() {
		if (getNumSamples() == 0)
			return Float.NEGATIVE_INFINITY;
		return getSample(getNumSamples() - 1).time;
	}

	public float getDomainMin() {
		if (getNumSamples() == 0)
			return Float.POSITIVE_INFINITY;
		return getSample(0).time;
	}

	public float getDuration() {
		if (samples.size() < 2)
			return 0;
		return getSample(samples.size() - 1).time - getSample(0).time;
	}

	public float getEndTime() {
		if (samples.size() == 0)
			return 0;
		return getSample(samples.size() - 1).time;
	}

	public int getNumSamples() {
		return samples.size();
	}

	public Sample getSample(int i) {
		if (i >= samples.size() - 1)
			i = samples.size() - 1;
		if (i < 0)
			i = 0;
		return samples.get(i);
	}

	public float getStartTime() {
		if (samples.size() == 0)
			return 0;
		return getSample(0).time;
	}

	public boolean getValue(float time, T value) {
		if (samples.size() < 1)
			return false;

		if (samples.size() == 1) {
			value.setValue(getSample(0).data);
			return true;
		}

		validateCache(time);

		float a = (time - now) / (duration);

		if (Float.isNaN(a) || Float.isInfinite(a))
			a = 0.5f;

		if (!extrapolation) {
			if (a > 1)
				a = 1;
			if (a < 0)
				a = 0;
		} else {
			if (a < 0) {
				indexNow = 0;
				indexNext = 1;

			} else if (a > 1) {
				indexNow = samples.size() - 2;
				indexNext = samples.size() - 1;
			}

			if (indexNow == indexNext && indexNow != 0) {
				indexNow--;
				indexBefore--;
				if (indexBefore < 0)
					indexBefore = 0;
				a += 1;
			}

			CubicInterpolatorDynamic<T>.Sample i0 = getSample(indexNow);
			CubicInterpolatorDynamic<T>.Sample i1 = getSample(indexNext);

			if (i0.generation < i1.generation) {
				value.setValue(i0.data);
				return true;
			} else if (i0.generation > i1.generation) {
				value.setValue(i1.data);
				return true;
			}
			// System.err.println(" extrapolating <"+getSample(indexNow).data+"> <"+getSample(indexNext).data+"> <"+indexNow+"> <"+indexNext+"> <"+a+">");

			value.lerp(i0.data, i1.data, a);
			return true;

		}

		// if (a>1)
		// {
		// System.err.println(" trying to extrapolate <"+time+"> <"+now+"> <"+duration+"> <"+a+"> <"+indexBefore+"> <"+indexNow+"> <"+indexNext+"> <"+indexAfter+">");
		// }

		// System.err.println("          "+a+" "+getSample(indexBefore)+" "+getSample(indexNow)+" "+getSample(indexNext)+" "+getSample(indexAfter));

		Sample i0 = getSample(indexNow);
		Sample i1 = getSample(indexNext);
		if (i0.generation < i1.generation) {
			value.setValue(i0.data);
			return true;
		} else if (i0.generation > i1.generation) {
			value.setValue(i1.data);
			return true;
		}

		if (linear) {
			value.lerp(i0.data, i1.data, a);
			return true;
		}

		Sample ib = getSample(indexBefore);
		Sample ia = getSample(indexAfter);

		if (i0.generation < i1.generation) {
			value.setValue(i0.data);
			return true;
		} else if (i0.generation > i1.generation) {
			value.setValue(i1.data);
			return true;
		}

		value.cerp(ib.data, ib.time, i0.data, i0.time, i1.data, i1.time, ia.data, ia.time, a);
		return true;
	}

	public boolean isInDomain(double now) {
		return now >= getDomainMin() && now <= getDomainMax();
	}

	public void mergeInto(CubicInterpolatorDynamic<T> from) {
		if (from.getDomainMax() > this.getDomainMax() && from.getDomainMin() < this.getDomainMax())
			System.err.println(" warning, overlapping merge of cubic interpolators <" + this + "> <" + from + ">");

		for (int i = 0; i < from.getNumSamples(); i++) {
			new Sample(from.getSample(i).data, from.getSample(i).time);
		}
	}

	public void printSamples(PrintStream p) {
		for (int i = 0; i < samples.size(); i++) {
			p.println(i + " " + samples.get(i));
		}
	}

	public int protect(int i) {
		if (i < 0)
			i = 0;
		if (i >= samples.size())
			i = samples.size() - 1;
		return i;
	}

	// public Vector3CubicInterpolatorDynamic downsampleWithError(int
	// maxSamples, float minError) {
	// Vector3CubicInterpolatorDynamic dynamic = new
	// Vector3CubicInterpolatorDynamic();
	// if (this.getNumSamples() == 0) return dynamic;
	// dynamic.new Sample(this.getSample(0).data, this.getSample(0).time);
	// if (this.getNumSamples() == 1) return dynamic;
	// dynamic.new Sample(this.getSample(this.getNumSamples() - 1).data,
	// this.getSample(this.getNumSamples() - 1).time);
	// if (this.getNumSamples() == 2) return dynamic;
	//
	// float min = Float.POSITIVE_INFINITY;
	// while (min > minError && dynamic.getNumSamples() < maxSamples) {
	// float e = Float.NEGATIVE_INFINITY;
	// int eat = 0;
	// Vector3 a1 = new Vector3();
	// Vector3 a2 = new Vector3();
	//
	// for (int i = 0; i < this.getNumSamples(); i++) {
	// this.getValue(this.getSample(i).time, a1);
	// dynamic.getValue(this.getSample(i).time, a2);
	// float eHere = a1.distanceFrom(a2);
	// if (eHere > e) {
	// e = eHere;
	// eat = i;
	// }
	// }
	//
	// dynamic.new Sample(this.getSample(eat).data,
	// this.getSample(eat).time);
	// min = e;
	// }
	// return dynamic;
	//
	// }

	public void removeSample(int i) {
		samples.remove(i);
		if (indexAfter == i || indexBefore == i || indexNext == i || indexNow == i)
			cacheInvalid = true;
	}

	public void removeSample(Sample sample) {
		int n = Collections.binarySearch(samples, sample, comparator);
		if (n >= 0) {
			removeSample(n);
		} else
			throw new ArrayIndexOutOfBoundsException(" couldn't find sample <" + sample + "> from <" + samples + ">");
	}

	public void resort() {
		cacheInvalid = true;
		Collections.sort(samples, comparator);
	}

	public CubicInterpolatorDynamic<T> setLinear(boolean linear) {
		this.linear = linear;
		return this;
	}

	public void startAtZero() {
		if (samples.size() < 1)
			return;
		float start = getSample(0).time;
		for (int i = 0; i < samples.size(); i++) {
			getSample(i).time -= start;
		}
		cacheInvalid = true;
	}

	@Override
	public String toString() {
		return "v3cubic:" + this.getNumSamples() + "(" + this.getStartTime() + " -> " + this.getEndTime() + ")";
	}

	public void trimStartTo(int maxSamples) {
		if (samples.size() > maxSamples)
			samples = new ArrayList(samples.subList(samples.size() - maxSamples, samples.size()));
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		comparator = new MComparator();
	}

	private void validateCache(float time) {

		if (!cacheInvalid) {
			if (time >= now && time <= next) {
				return;
			} else {
				if (time >= next && time <= after) {
					indexBefore = indexNow;
					indexNow = indexNext;
					indexNext = indexAfter;
					indexAfter = protect(indexAfter + 1);

					before = now;
					now = next;
					next = after;
					after = getSample(indexAfter).time;
					duration = next - now;
					if (duration < 1e-10)
						duration = 1;

					return;
				}
			}
		}

		// from scratch
		temp.time = time;
		int n = Collections.binarySearch(samples, temp, comparator);
		if (n < 0)
			n = -n - 2;

		indexBefore = protect(n - 1);
		indexNow = protect(n);
		indexNext = protect(n + 1);
		indexAfter = protect(n + 2);

		before = getSample(indexBefore).time;
		now = getSample(indexNow).time;
		next = getSample(indexNext).time;
		after = getSample(indexAfter).time;

		duration = next - now;
		if (indexNow == indexNext) {
			duration = Math.max(now - before, after - next);
		}

		cacheInvalid = false;
	}

	public void internValues() {
		HashMap<T, T> intern = new HashMap<T, T>();
		for (Sample s : samples) {
			T tt = intern.get(s.data);
			if (tt != null) {
				s.data = tt;
			} else {
				intern.put(s.data, s.data);
			}
		}
	}

	public void remapTime(iFunction<Number, Number> iFunction) {

		for (int i = 0; i < getNumSamples(); i++) {
			getSample(i).time = iFunction.f(getSample(i).time).floatValue();
		}

		resort();
		cacheInvalid = true;

	}

}