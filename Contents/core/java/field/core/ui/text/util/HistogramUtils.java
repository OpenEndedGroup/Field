package field.core.ui.text.util;

import java.util.ArrayList;
import java.util.List;

import field.core.dispatch.iVisualElement.Rect;
import field.math.linalg.Vector2;
import field.math.util.Histogram;
import field.math.util.iAverage;

public class HistogramUtils {

	public HistogramUtils() {
	}

	public Histogram<Number> rebin(Histogram<Number> in, final int numBins) {

		final Rect bounds = bounds(in);
		if (bounds.w == 0) bounds.w = 1;
		if (bounds.h == 0) bounds.h = 1;

		final Histogram<Number> out = new Histogram<Number>();

		for (int i = 0; i < numBins; i++) {
			out.visit(bounds.x + bounds.w * i / (numBins - 1f), 0);
		}

		in.average(new iAverage<Number, Object>(){

			public <X extends Number> void accept(X accept, double weight) {
				
				int bin = (int) Math.round(numBins*(accept.floatValue()-bounds.x)/bounds.w);
				out.visit(bounds.x + bounds.w * bin / (numBins - 1f), (float) weight);
			}

			public void begin(int num, double totalWeight) {
			}

			public Object end() {
				return null;
			}
		});
		
		return out;
	}

	public Rect bounds(Histogram<Number> h) {
		final float[] minX = { Float.POSITIVE_INFINITY};
		final float[] maxX = { Float.NEGATIVE_INFINITY};
		final float[] minY = { Float.POSITIVE_INFINITY};
		final float[] maxY = { Float.NEGATIVE_INFINITY};

		final List<Vector2> toDraw = new ArrayList<Vector2>();

		h.average(new iAverage<Number, Object>(){

			public <X extends Number> void accept(X accept, double weight) {
				if (accept.floatValue() > maxX[0]) maxX[0] = accept.floatValue();
				if (accept.floatValue() < minX[0]) minX[0] = accept.floatValue();
				if (weight > maxX[0]) maxX[0] = (float) weight;
				if (weight < minX[0]) minX[0] = (float) weight;

				toDraw.add(new Vector2(accept.floatValue(), weight));

			}

			public void begin(int num, double totalWeight) {
			}

			public Object end() {
				return null;
			}
		});

		return new Rect(minX[0], minY[0], maxX[0] - minX[0], maxY[0] - minY[0]);
	}
}
