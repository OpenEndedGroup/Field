package field.math;

import java.util.LinkedHashMap;
import java.util.List;

import field.namespace.generic.Bind.iFunction;

public class BinSelection {

	private float max;
	private float min;
	private float width;
	private final List<? extends Number> data;

	static LinkedHashMap<List<? extends Number>, Float> cachedWidths = new LinkedHashMap<List<? extends Number>, Float>() {
		protected boolean removeEldestEntry(java.util.Map.Entry<java.util.List<? extends Number>, Float> arg0) {
			return this.size() > 5;
		}
	};

	public BinSelection(List<? extends Number> data, int searchFor) {
		this.data = data;
		max = Float.NEGATIVE_INFINITY;
		min = Float.POSITIVE_INFINITY;
		for (Number f : data) {
			max = Math.max(max, f.floatValue());
			min = Math.min(min, f.floatValue());
		}

		float c = Float.POSITIVE_INFINITY;
		float best = 1;

		Float o = cachedWidths.get(data);
		if (o != null)
			this.width = o;
		else {
			for (int i = 0; i < searchFor; i += 4) {
				float w = (max - min) / (i + 1);
				float z = cost(w, data);

				if (z < c) {
					c = z;
					best = w;
				}
			}

			this.width = best;
			cachedWidths.put(data, best);
		}

	}

	public float getWidth() {
		return width;
	}

	public int getNumBins() {
		int b = (int) ((max - min) / width) / 2;
		if (b < 1)
			b = 1;
		return b;
	}

	public float evaluate(float d) {
		float z = 0;
		for (Number dat : data) {
			z += gauss(d - dat.floatValue(), width);
		}
		return z / data.size();
	}

	public float evaluate(float d, iFunction<Number, Number> nn) {
		float z = 0;
		for (Number dat : data) {
			z += gauss(d - nn.f(dat.floatValue()).floatValue(), width);
		}
		return z / data.size();
	}

	private float gauss(float x, float w) {
		return (float) (1 / Math.sqrt(2 * Math.PI) / w * Math.exp(-x * x / 2 / w / w));
	}

	private float cost(float w, List<? extends Number> data) {

		float a = 0;
		for (int i = 0; i < data.size(); i++) {
			for (int j = i + 1; j < data.size(); j++) {
				float x = data.get(i).floatValue() - data.get(j).floatValue();
				if (Math.abs(x) < 5 * w)
					a += 2 * Math.exp(-x * x / 4 / w / w) - 4 * Math.sqrt(2) * Math.exp(-x * x / 2 / w / w);
			}
		}

		return (float) ((data.size() / w + a / w) / 2 / Math.sqrt(Math.PI));
	}

}