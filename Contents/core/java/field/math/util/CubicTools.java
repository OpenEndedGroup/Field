package field.math.util;

import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;

/**
 * A class of static methods for performing cubic interpolation.
 */
public class CubicTools {

	public static boolean useTangentWeighting = true;

	/**
	 * Yields a new float array for each thread that calls tmp.get(). Like a
	 * thread-safe static variable. Should be private.
	 */
	// static ThreadLocal tmp= new ThreadLocal()
	// {
	// protected Object initialValue()
	// {
	// return new float[4];
	// }
	// };

	static float[] tmp2 = new float[4];

	public static double cubic(double a, double before, double beforeTime, double now, double nowTime, double next, double nextTime, double after, double afterTime) {

		double nowBefore = nowTime - beforeTime;
		double afterNext = afterTime - nextTime;
		double nextNow = nextTime - nowTime;

		nowBefore = nowBefore == 0 ? 1 : nowBefore;
		afterNext = afterNext == 0 ? 1 : afterNext;
		nextNow = nextNow == 0 ? 1 : nextNow;

		double t1x = 0.5 * ((next - now) / (nextNow) + (after - next) / (afterNext)) * (nextNow);
		double t2x = 0.5 * ((now - before) / (nowBefore) + (next - now) / (nextNow)) * (nextNow);

		H((float) a, tmp2);
		// return tmp2[0] * now + tmp2[1] * next + tmp2[2] * t2 +
		// tmp2[3] * t1;
		return (tmp2[0] * now + tmp2[1] * next + tmp2[2] * t2x + tmp2[3] * t1x);
	}

	/**
	 * Performs cubic interpolation of a scalar function given four local
	 * samples.
	 * 
	 * @param alpha
	 *                The blending parameter. <code>alpha = 0</code> yields
	 *                <code>now</code>, <code>alpha = 1</code> yields
	 *                <code>next</code>.
	 * @param before
	 *                The first in a sequence of four local samples.
	 * @param now
	 *                The second local sample.
	 * @param next
	 *                The third local sample.
	 * @param after
	 *                The fourth local sample.
	 */
	static public float cubic(float alpha, float before, float now, float next, float after) {
		// float[] tmp= (float[]) CubicTools.tmp.get();
		H(alpha, tmp2);
		float t1 = 1 * (next - before) / 2;
		float t2 = 1 * (after - now) / 2;

		return tmp2[0] * now + tmp2[1] * next + tmp2[2] * t1 + tmp2[3] * t2;
	}

	public static void cubic(float a, Vector2 before, float beforeTime, Vector2 now, float nowTime, Vector2 next, float nextTime, Vector2 after, float afterTime, Vector2 out) {

		float nowBefore = nowTime - beforeTime;
		float afterNext = afterTime - nextTime;
		float nextNow = nextTime - nowTime;

		nowBefore = nowBefore == 0 ? 1 : nowBefore;
		afterNext = afterNext == 0 ? 1 : afterNext;
		nextNow = nextNow == 0 ? 1 : nextNow;

		float t1x = (float) 0.5 * ((next.x - now.x) / (nextNow) + (after.x - next.x) / (afterNext)) * (nextNow);
		float t2x = (float) 0.5 * ((now.x - before.x) / (nowBefore) + (next.x - now.x) / (nextNow)) * (nextNow);
		float t1y = (float) 0.5 * ((next.y - now.y) / (nextNow) + (after.y - next.y) / (afterNext)) * (nextNow);
		float t2y = (float) 0.5 * ((now.y - before.y) / (nowBefore) + (next.y - now.y) / (nextNow)) * (nextNow);

		H(a, tmp2);
		// return tmp2[0] * now + tmp2[1] * next + tmp2[2] * t2 +
		// tmp2[3] * t1;
		out.x = (tmp2[0] * now.x + tmp2[1] * next.x + tmp2[2] * t2x + tmp2[3] * t1x);
		out.y = (tmp2[0] * now.y + tmp2[1] * next.y + tmp2[2] * t2y + tmp2[3] * t1y);
	}

	public static void cubic(float a, Vector3 before, float beforeTime, Vector3 now, float nowTime, Vector3 next, float nextTime, Vector3 after, float afterTime, Vector3 out) {

		if (!useTangentWeighting) {
			cubic(a, before, now, next, after, out);
			return;
		}

		float nowBefore = nowTime - beforeTime;
		float afterNext = afterTime - nextTime;
		float nextNow = nextTime - nowTime;

		nowBefore = nowBefore == 0 ? 1 : nowBefore;
		afterNext = afterNext == 0 ? 1 : afterNext;
		nextNow = nextNow == 0 ? 1 : nextNow;

		float t1x = (float) 0.5 * ((next.x - now.x) / (nextNow) + (after.x - next.x) / (afterNext)) * (nextNow);
		float t2x = (float) 0.5 * ((now.x - before.x) / (nowBefore) + (next.x - now.x) / (nextNow)) * (nextNow);
		float t1y = (float) 0.5 * ((next.y - now.y) / (nextNow) + (after.y - next.y) / (afterNext)) * (nextNow);
		float t2y = (float) 0.5 * ((now.y - before.y) / (nowBefore) + (next.y - now.y) / (nextNow)) * (nextNow);
		float t1z = (float) 0.5 * ((next.z - now.z) / (nextNow) + (after.z - next.z) / (afterNext)) * (nextNow);
		float t2z = (float) 0.5 * ((now.z - before.z) / (nowBefore) + (next.z - now.z) / (nextNow)) * (nextNow);

		H(a, tmp2);
		// return tmp2[0] * now + tmp2[1] * next + tmp2[2] * t2 +
		// tmp2[3] * t1;
		out.x = (tmp2[0] * now.x + tmp2[1] * next.x + tmp2[2] * t2x + tmp2[3] * t1x);
		out.y = (tmp2[0] * now.y + tmp2[1] * next.y + tmp2[2] * t2y + tmp2[3] * t1y);
		out.z = (tmp2[0] * now.z + tmp2[1] * next.z + tmp2[2] * t2z + tmp2[3] * t1z);
	}

	/**
	 * Performs cubic interpolation of a 3-dimensional function given four
	 * local samples.
	 * 
	 * @return
	 */
	static public Vector3 cubic(float alpha, Vector3 before, Vector3 now, Vector3 next, Vector3 after, Vector3 out) {
		if (out == null)
			out = new Vector3();
		out.x = (cubic(alpha, before.x, now.x, next.x, after.x));
		out.y = (cubic(alpha, before.y, now.y, next.y, after.y));
		out.z = (cubic(alpha, before.z, now.z, next.z, after.z));
		return out;
	}

	static public void cubic(float alpha, Vector3 before, Vector3 now, Vector3 next, Vector3 after, Vector3 out, float tmul) {
		out.x = (straightCubic(alpha, before.x, now.x, next.x, after.x, tmul));
		out.y = (straightCubic(alpha, before.y, now.y, next.y, after.y, tmul));
		out.z = (straightCubic(alpha, before.z, now.z, next.z, after.z, tmul));
	}

	public static void cubic(float a, Vector4 before, float beforeTime, Vector4 now, float nowTime, Vector4 next, float nextTime, Vector4 after, float afterTime, Vector4 out) {

		if (!useTangentWeighting) {
			cubic(a, before, now, next, after, out);
			return;
		}

		float nowBefore = nowTime - beforeTime;
		float afterNext = afterTime - nextTime;
		float nextNow = nextTime - nowTime;

		nowBefore = nowBefore == 0 ? 1 : nowBefore;
		afterNext = afterNext == 0 ? 1 : afterNext;
		nextNow = nextNow == 0 ? 1 : nextNow;

		float t1x = (float) 0.5 * ((next.x - now.x) / (nextNow) + (after.x - next.x) / (afterNext)) * (nextNow);
		float t2x = (float) 0.5 * ((now.x - before.x) / (nowBefore) + (next.x - now.x) / (nextNow)) * (nextNow);
		float t1y = (float) 0.5 * ((next.y - now.y) / (nextNow) + (after.y - next.y) / (afterNext)) * (nextNow);
		float t2y = (float) 0.5 * ((now.y - before.y) / (nowBefore) + (next.y - now.y) / (nextNow)) * (nextNow);
		float t1z = (float) 0.5 * ((next.z - now.z) / (nextNow) + (after.z - next.z) / (afterNext)) * (nextNow);
		float t2z = (float) 0.5 * ((now.z - before.z) / (nowBefore) + (next.z - now.z) / (nextNow)) * (nextNow);
		float t1w = (float) 0.5 * ((next.w - now.w) / (nextNow) + (after.w - next.w) / (afterNext)) * (nextNow);
		float t2w = (float) 0.5 * ((now.w - before.w) / (nowBefore) + (next.w - now.w) / (nextNow)) * (nextNow);

		H(a, tmp2);
		// return tmp2[0] * now + tmp2[1] * next + tmp2[2] * t2 +
		// tmp2[3] * t1;
		out.x = (tmp2[0] * now.x + tmp2[1] * next.x + tmp2[2] * t2x + tmp2[3] * t1x);
		out.y = (tmp2[0] * now.y + tmp2[1] * next.y + tmp2[2] * t2y + tmp2[3] * t1y);
		out.z = (tmp2[0] * now.z + tmp2[1] * next.z + tmp2[2] * t2z + tmp2[3] * t1z);
		out.w = (tmp2[0] * now.w + tmp2[1] * next.w + tmp2[2] * t2w + tmp2[3] * t1w);
	}

	static public float cubicTCB(float before, float now, float next, float after, float alpha, Vector4 tcbm1, Vector4 tcbm2) {
		float t1 = (float) (0.5 * ((1 - tcbm2.x) * (1 - tcbm2.y) * (1 + tcbm2.z) * (next - now) + (1 - tcbm2.x) * (1 + tcbm2.y) * (1 - tcbm2.z) * (after - next)) * tcbm2.w);
		float t2 = (float) (0.5 * ((1 - tcbm1.x) * (1 + tcbm1.y) * (1 + tcbm1.z) * (now - before) + (1 - tcbm1.x) * (1 - tcbm1.y) * (1 - tcbm1.z) * (next - now)) * tcbm1.w);

		H(alpha, tmp2);
		return tmp2[0] * now + tmp2[1] * next + tmp2[2] * t2 + tmp2[3] * t1;
	}

	/**
	 * Calculates the interpolation coefficients for the four samples. The
	 * cubic-interpolated value for a given alpha is given by:<br>
	 * <code>out[0]*1st_sample + out[1]*2nd_sample + out[2]*3rd_sample + out[3]*4th_sample</code>
	 */
	static public void H(float alpha, float[] out) {
		/*
		 * float a2 = a*a; float a3 = a2*a;
		 * 
		 * float o0 = 2.0*a3-3.0*a2+1.0; float o1 = -2.0*a3+3.0*a2;
		 * float o2 = a3-2.0*a2+a; float o3 = a3-a2;
		 * 
		 * vec4 pos =
		 * gl_Vertex*o0+s_Ten*o1+(s_Ten-s_Nine)*0.5*o2+(s_Noise
		 * -gl_Vertex)*0.5*o3;
		 */
		float alpha2 = alpha * alpha;
		float alpha3 = alpha * alpha2;
		out[0] = 2 * alpha3 - 3 * alpha2 + 1;
		out[1] = -2 * alpha3 + 3 * alpha2;
		out[2] = alpha3 - 2 * alpha2 + alpha;
		out[3] = alpha3 - alpha2;
	}

	/**
	 * Calculates the interpolation coefficient for the first sample.
	 */
	static public float H0(float alpha) {
		float alpha2 = alpha * alpha;
		float alpha3 = alpha * alpha2;
		return 2 * alpha3 - 3 * alpha2 + 1;
	}

	/**
	 * Calculates the interpolation coefficient for the second sample.
	 */
	static public float H1(float alpha) {
		float alpha2 = alpha * alpha;
		float alpha3 = alpha * alpha2;
		return -2 * alpha3 + 3 * alpha2;
	}

	/**
	 * Calculates the interpolation coefficient for the third sample.
	 */
	static public float H2(float alpha) {
		float alpha2 = alpha * alpha;
		float alpha3 = alpha * alpha2;
		return alpha3 - 2 * alpha2 + alpha;
	}

	/**
	 * Calculates the interpolation coefficient for the fourth sample.
	 */
	static public float H3(float alpha) {
		float alpha2 = alpha * alpha;
		float alpha3 = alpha * alpha2;
		return alpha3 - alpha2;
	}

	/**
	 * Test drive method.
	 */
	static public void main(String[] s) {
		for (int i = 0; i < 11; i++) {
		}
	}

	public static float smoothStep(float x) {
		if (x < 0)
			return 0;
		if (x > 1)
			return 1;
		return -2 * x * x * x + 3 * x * x;
	}

	public static float hermiteExtrapolate(float t, float m0, float m1) {
		if (t >= 0 && t <= 1) {
			float t2 = t * t;
			float t3 = t2 * t;
			return (t3 - 2 * t2 + t) * m0 + (-2 * t3 + 3 * t2) + (t3 - t2) * m1;
		} else if (t > 1) {
			return -m1+1+m1 * t;
		} else {
			return m0 * t;
		}
	}

	static public float straightCubic(float alpha, float before, float now, float next, float after, float tmul) {
		// float[] tmp= (float[]) CubicTools.tmp.get();
		H(alpha, tmp2);
		float t1 = tmul * (next - before) / 2;
		float t2 = tmul * (next - now) / 2;
		return tmp2[0] * now + tmp2[1] * next + tmp2[2] * t1 + tmp2[3] * t2;
	}

	private static float checkNAN(float f, float x) {
		return Float.isNaN(f) ? x : f;
	}

	private static void cubic(float alpha, Vector4 before, Vector4 now, Vector4 next, Vector4 after, Vector4 out) {
		out.x = (cubic(alpha, before.x, now.x, next.x, after.x));
		out.y = (cubic(alpha, before.y, now.y, next.y, after.y));
		out.z = (cubic(alpha, before.z, now.z, next.z, after.z));
		out.w = (cubic(alpha, before.w, now.w, next.w, after.w));
	}

	public static double smoothStep(double x) {
		if (x < 0)
			return 0;
		if (x > 1)
			return 1;
		return -2 * x * x * x + 3 * x * x;
	}

	public static double smootherStep(double x) {
		if (x < 0)
			return 0;
		if (x > 1)
			return 1;
		return x * x * x * (x * (x * 6 - 15) + 10);
	}

}
