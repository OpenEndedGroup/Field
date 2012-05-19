package field.graphics.core;

import java.util.WeakHashMap;

public class DynamicFrameRateCuller {

	static boolean inside;

	public DynamicFrameRateCuller() {

	}

	static long lastAt = System.currentTimeMillis();
	static float averageFPS = 0;

	static float targetFPS = 25f;

	public void enter() {
		long time = System.currentTimeMillis();

		long delta = time - lastAt;
		if (delta > 500)
			delta = 500;

		float fps = (float) (1000.0 / delta);

		float alpha = 0.95f;
		averageFPS = averageFPS * alpha + (1 - alpha) * fps;

		lastAt = time;
		inside = true;
	}

	public void exit() {
		inside = false;
	}

	static WeakHashMap<Object, Integer> last = new WeakHashMap<Object, Integer>();

	public static int advise(int vertexLimit, PointList pointList) {
		if (!inside)
			return vertexLimit;

		float ratio = averageFPS / targetFPS;
		if (ratio > 1)
			ratio = 1;
		if (ratio < 0.25f)
			ratio = 0.25f;

		ratio *= ratio;

		int desired = (int) (vertexLimit * ratio);

		Integer m = last.get(pointList);

		if (m == null) {
			last.put(pointList, desired);
			return desired;
		}

		int diff = Math.abs(m - desired);

		if (diff < 100) {
			last.put(pointList, desired);
			return desired;
		}

		if (m < desired) {
			int lim = Math.max(0, Math.min(vertexLimit, m + (int)(vertexLimit*0.005f)));
			last.put(pointList, lim);
			return lim;
		} else {
			int lim = Math.max(0, Math.min(vertexLimit, m - (int)(vertexLimit*0.005f)));
			last.put(pointList, lim);
			return lim;
		}

	}

}
