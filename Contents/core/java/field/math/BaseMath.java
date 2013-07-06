package field.math;

import java.text.NumberFormat;

import field.math.abstraction.iBlendable;
import field.math.abstraction.iFloatProvider;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.iToFloatArray;
import field.math.util.CubicTools;

/**
 * BaseMath is meant to be an extension of java.lang.Math to cover issues they
 * have and also to add some functionality.
 * 
 * based on work by Michael Patrick Johnson <aries@media.mit.edu>, although any
 * bugs are surely ours, not his.
 */

public class BaseMath {
	public final static float epsilon = 1e-8f;

	/**
	 * Clamps the value to the specified range.
	 * 
	 * @param val
	 *                the value to clamp
	 * @param low
	 *                the lower value that will replace val if val < low.
	 * @param high
	 *                the upper value that will replace val if val > high.
	 * @return the clamped value in the range [low, high].
	 */
	public static final double clamp(double val, double low, double high) {
		if (val > high)
			return high;
		if (val < low)
			return low;
		return val;
	}

	static double[] acos_lookup = null;

	/**
	 * Input-checking inverse cosine. Clamps the input to the domain of acos
	 * (i.e. [-1..1]) before evaluating, instead of returning a silent NaN
	 * like java.lang.Math.acos. Useful when there's a fear of rounding
	 * errors pushing the arg beyond 1 or -1.
	 */
	public static final double acos(double cos_theta) {
		
		if (acos_lookup==null)
		{
			acos_lookup = new double[1000];
			for(int i=0;i<acos_lookup.length;i++)
				acos_lookup[i] = Math.acos(clamp(-1.0+2*i/(acos_lookup.length-1f), -1, 1));
		}
		
		
		cos_theta = clamp(cos_theta, -1, 1);
	
		float indx = (float) ((acos_lookup.length-1f)*(cos_theta+1)/2);
		int left = (int)indx;
		int right= (int)indx+1;
		float alpha= indx-left;
		
		left = left<0 ? 0 : (left>acos_lookup.length-1 ? (acos_lookup.length-1) : left);
		right= right<0 ? 0 : (right>acos_lookup.length-1 ? (acos_lookup.length-1) : right);
		
		return acos_lookup[left]*(1-alpha)+alpha*acos_lookup[right];
		
//		return Math.acos(clamp(cos_theta, -1.0, 1.0));
	}

	/**
	 * Input-checking inverse sine. Clamps the input to the domain of acos
	 * [-1..1] before evaluating, instead of returning a silent NaN like
	 * java.lang.Math.asin. Useful when there's a fear of rounding errors
	 * pushing the arg beyond 1 or -1.
	 */
	public static final double asin(double sin_theta) {
		return Math.asin(clamp(sin_theta, -1.0, 1.0));
	}

	/**
	 * a sign safe version of pow, _always_ monotically increasing for
	 * positive pow
	 */
	static public double safePow(double x, double pow) {
		int sign = pow < 0 ? -1 : 1;
		double ret = Math.pow(x * sign, pow) * sign;
		return ret;
	}

	static final double sinc_lookup[] = { 1.0000000000000000e+000, 9.9932880203694100e-001, 9.9731682984747660e-001, 9.9396894386629840e-001, 9.8929322928435600e-001, 9.8330097279963240e-001, 9.7600663017022280e-001, 9.6742778467129840e-001, 9.5758509658600000e-001, 9.4650224388831540e-001, 9.3420585430350560e-001, 9.2072542895852920e-001, 9.0609325786111200e-001, 8.9034432747150920e-001, 8.7351622065555020e-001, 8.5564900933114440e-001, 8.3678514014298590e-001, 8.1696931352166510e-001, 7.9624835650368560e-001, 7.7467108970794610e-001, 7.5228818888200670e-001, 7.2915204144787160e-001, 7.0531659849201880e-001, 6.8083722265795170e-001, 6.5577053241160130e-001, 6.3017424316041650e-001, 6.0410700571592420e-001, 5.7762824259689140e-001, 5.5079798267594950e-001, 5.2367669467663940e-001,
			4.9632512003028100e-001, 4.6880410560287790e-001, 4.4117443680140560e-001, 4.1349667156634410e-001, 3.8583097575317760e-001, 3.5823696039983620e-001, 3.3077352136970930e-001, 3.0349868185094890e-001, 2.7646943818232700e-001, 2.4974160946396760e-001, 2.2336969139787360e-001, 1.9740671478835150e-001, 1.7190410911627600e-001, 1.4691157158367040e-001, 1.2247694200637710e-001, 9.8646083912710340e-002, 7.5462772185011250e-002, 5.2968587559005830e-002, 3.1202818272899560e-002, 1.0202369134296560e-002, -9.9983217516106900e-003, -2.9367358374493620e-002, -4.7875454139870700e-002, -6.5495990953028560e-002, -8.2205069927258430e-002, -9.7981553605101640e-002, -1.1280709960888980e-001, -1.2666618566462450e-001, -1.3954612597107600e-001, -1.5143707891381250e-001,
			-1.6233204615157880e-001, -1.7222686312997410e-001, -1.8112018110459860e-001, -1.8901344078269100e-001, -1.9591083771866030e-001, -2.0181927962473900e-001, -2.0674833578317200e-001, -2.1071017877082060e-001, -2.1371951873072370e-001, -2.1579353044794820e-001, -2.1695177350889340e-001, -2.1721610584403640e-001, -2.1661059097383420e-001, -2.1516139929608260e-001, -2.1289670377041100e-001, -2.0984657037171240e-001, -2.0604284369911690e-001, -2.0151902814057680e-001, -1.9631016500519630e-001, -1.9045270604607760e-001, -1.8398438380563720e-001, -1.7694407922304120e-001, -1.6937168694961350e-001, -1.6130797882274430e-001, -1.5279446595199010e-001, -1.4387325987267890e-001, -1.3458693322243740e-001, -1.2497838039463620e-001, -1.1509067861981620e-001,
			-1.0496694992174230e-001, -9.4650224388831680e-002, -8.4183305194373640e-002, -7.3608635790207540e-002, -6.2968169688401420e-002, -5.2303243234022990e-002, -4.1654451759341330e-002, -3.1061529495821900e-002, -2.0563233605102720e-002, -1.0197232673846340e-002, -3.8980430910514780e-017 };

	private final static double SINC_EPSILON = .001;

	/**
	 * Evaluates sinc(x). Defined as sin(x)/x. Remains valid as x -> 0.
	 * (Uses linear interpolation on a fine-grained lookup table.)
	 */
	public static final double sinc(double x) {
		double n = sinc_lookup.length;
		double dn = 1.0 / n;

		/*
		 * if far enough from zero, it's ok to calculate it
		 */

		if (x <= -SINC_EPSILON || x >= SINC_EPSILON) {
			return Math.sin(x) / x;
		}

		if (x == 0.0)
			return 1.0;

		double ax = Math.abs(x);
		int left_idx = (int) ((ax / (2 * Math.PI)) * sinc_lookup.length);
		if (left_idx == sinc_lookup.length - 1)
			return sinc_lookup[sinc_lookup.length - 1];
		int right_idx = left_idx + 1;

		double left_val = left_idx * dn;
		double t = (ax / (2 * Math.PI) - left_val) / dn;
		return (1.0 - t) * sinc_lookup[left_idx] + t * sinc_lookup[right_idx];
	}

	/**
	 * The tanh function. Defined as (exp(x)-exp(-x))/(exp(x)+exp(-x))
	 * 
	 * @author marc
	 */
	public static final double tanh(double x) {

		if (x == Double.NEGATIVE_INFINITY)
			return -1;
		if (x == Double.POSITIVE_INFINITY)
			return 1;

		double temp = Math.exp(-2 * x);
		return (1 - temp) / (1 + temp);
	}

	/**
	 * The sinh function. Defined as (exp(x)-exp(-x))/2
	 * 
	 * @author marc
	 */
	public static final double sinh(double x) {

		double temp = Math.exp(x);
		return (temp - 1 / temp) / 2;
	}

	public static final double fastExp(double x) {
		final long tmp = (long) (1512775 * x + (1072693248 - 60801));
		return Double.longBitsToDouble(tmp << 32);
	}

	/**
	 * Reads input as a two's complement unsigned byte, returns the
	 * numerical value as an int.
	 * 
	 * @author marc
	 */
	public static int intify(byte argh) {
		int i = (argh < 0 ? ((int) argh) + 256 : (int) argh);
		return i;
	}

	/**
	 * Reads input as a two's complement unsigned byte, returns the
	 * numerical value as an int.
	 * 
	 * @author marc
	 */
	public static int intifyB(byte argh) {
		int i = (argh < 0 ? ((int) argh) + 256 : (int) argh);
		return i;
	}

	/**
	 * Reads input as a two's complement unsigned short, returns the
	 * numerical value as an int.
	 * 
	 * @author marc
	 */
	public static int intify(short argh) {
		int i = (argh < 0 ? ((int) argh) + 256 * 256 : (int) argh);
		return i;
	}

	/**
	 * Reads input as a two's complement unsigned int, returns the numerical
	 * value as a long.
	 * 
	 * @author marc
	 */
	public static long longify(int argh) {
		long i = (argh < 0 ? ((long) argh) + (long) 4294967296L : (long) argh);
		return i;
	}

	/**
	 * Returns the string with this number of decimal places (convenience
	 * function).
	 * 
	 * @author marc
	 */
	static public String toDP(double number, int i) {
		if (_format == null) {
			_format = NumberFormat.getInstance();
		}
		if (_lastDigits != i) {
			_format.setMaximumFractionDigits(i);
			_format.setMinimumFractionDigits(i);
			_lastDigits = i;
		}

		return _format.format(number);
	}

	static public String toDPMax(double number, int i) {
		if (_format == null) {
			_format = NumberFormat.getInstance();
		}
		// if (_lastDigits != i)
		{
			_format.setMaximumFractionDigits(i);
			_format.setMinimumFractionDigits(0);
			_lastDigits = -2;
		}

		return _format.format(number);
	}

	static private NumberFormat _format = null;

	static private int _lastDigits = -1;

	/**
	 * Mutable version of java.lang.Float; especially useful for hashmaps.
	 * 
	 * @author marc
	 */
	static public class MutableFloat extends Number implements iFloatProvider, Comparable<Number>, iBlendable<MutableFloat>, iToFloatArray {

		private static final long serialVersionUID = 1L;

		public double d;

		public MutableFloat(float d) {
			this.d = d;
		}

		public MutableFloat(double d) {
			this.d = d;
		}

		public byte byteValue() {
			return (byte) d;
		}

		public double doubleValue() {
			return d;
		}

		public float floatValue() {
			return (float) d;
		}

		public int intValue() {
			return (int) d;
		}

		public long longValue() {
			return (long) d;
		}

		public short shortValue() {
			return (short) d;
		}

		public String toString() {
			return "" + d;
		}

		public float evaluate() {
			return (float) d;
		}

		public int compareTo(Number o) {
			return o.floatValue() > d ? -1 : o.floatValue() == d ? 0 : 1;
		}

		public void setValue(double startAt) {
			this.d = startAt;
		}

		// if these are "correctly" defined, then AStar
		// might not work.

		// @Override
		// public int hashCode() {
		// final int prime = 31;
		// int result = 1;
		// long temp;
		// temp = Double.doubleToLongBits(d);
		// result = prime * result + (int) (temp ^ (temp
		// >>> 32));
		// return result;
		// }
		// @Override
		// public boolean equals(Object obj) {
		// if (this == obj) return true;
		// if (obj == null) return false;
		// if (getClass() != obj.getClass()) return
		// false;
		// final MutableFloat other = (MutableFloat)
		// obj;
		// if (Double.doubleToLongBits(d) !=
		// Double.doubleToLongBits(other.d)) return
		// false;
		// return true;
		// }

		public MutableFloat cerp(MutableFloat before, float beforeTime, MutableFloat now, float nowTime, MutableFloat next, float nextTime, MutableFloat after, float afterTime, float a) {
			this.d = CubicTools.cubic(a, before.d, beforeTime, now.d, nowTime, next.d, nextTime, after.d, afterTime);
			return this;
		}

		public MutableFloat lerp(MutableFloat before, MutableFloat now, float a) {
			this.d = before.d + (now.d - before.d) * a;
			return this;
		}

		public MutableFloat setValue(MutableFloat to) {
			this.d = to.d;
			return this;
		}

		public MutableFloat blendRepresentation_newZero() {
			return new MutableFloat(0);
		}

		public float[] get() {
			return new float[] { (float) d };
		}
	}

	static public class MutableFloatFromProvider extends Number implements iFloatProvider {
		public iFloatProvider of;

		public MutableFloatFromProvider(iFloatProvider of) {
			this.of = of;

		}

		public byte byteValue() {
			return (byte) of.evaluate();
		}

		public double doubleValue() {
			return of.evaluate();
		}

		public float floatValue() {
			return of.evaluate();
		}

		public int intValue() {
			return (int) of.evaluate();
		}

		public long longValue() {
			return (long) of.evaluate();
		}

		public short shortValue() {
			return (short) of.evaluate();
		}

		public String toString() {
			return "fp(" + of.evaluate() + ")";
		}

		public float evaluate() {
			return of.evaluate();
		}
	}

	/**
	 * Hyperbolic secant function. Defined as 1/cosh(x), or
	 * 1/(exp(-x)+exp(x)
	 * 
	 * @author marc
	 */
	static public double sech(double x) {
		double temp = Math.exp(x);
		return temp / (temp * temp + 1);
	}

	/**
	 * Returns sech(x)^2
	 * 
	 * @author marc
	 */
	static public double sech2(double x) {
		double temp = Math.exp(2 * x);
		return temp / (1 + temp * temp + 2 * temp);
	}

	/**
	 * Returns sqrt(a^2 + b^2) without under/overflow.
	 */
	public static double hypot(double a, double b) {
		double r;
		if (Math.abs(a) > Math.abs(b)) {
			r = b / a;
			r = Math.abs(a) * Math.sqrt(1 + r * r);
		} else if (b != 0) {
			r = a / b;
			r = Math.abs(b) * Math.sqrt(1 + r * r);
		} else {
			r = 0.0;
		}
		return r;
	}

	public static boolean floatEqualTo(float a, float b) {
		return (Math.abs(a - b) <= epsilon);
	}

	/**
	 * returns error function (or an approximation thereof
	 * 
	 * @author marc
	 */
	public static float erf(float x) {
		double t, z, ans;
		z = Math.abs(x);
		t = 1.0 / (1.0 + 0.5 * z);
		ans = t * Math.exp(-z * z - 1.26551223 + t * (1.00002368 + t * (0.37409196 + t * (0.09678418 + t * (-0.18628806 + t * (0.27886807 + t * (-1.13520398 + t * (1.48851587 + t * (-0.82215223 + t * 0.17087277)))))))));
		return (float) (1 - (x >= 0.0 ? ans : 2.0 - ans));
	}

	public static int sgn(float input_ox) {
		return input_ox < 0 ? -1 : input_ox > 0 ? 1 : 0;
	}

	public static int gcd(int numerator, int denom) {
		if (numerator < 0)
			return 1;
		if (denom < 0)
			return 1;

		if (numerator < denom) {
			int t = numerator;
			numerator = denom;
			denom = t;
		}

		int r = numerator % denom;
		if (r == 0)
			return denom;
		return gcd(denom, r);
	}

	public static <T> int indexOf(T[] array, T c) {
		int i = 0;
		for (T t : array) {
			if (t == c)
				return i;
			i++;
		}
		return -1;
	}

	public static <T> int indexOfEq(T[] array, T c) {
		int i = 0;
		for (T t : array) {
			if (t.equals(c))
				return i;
			i++;
		}
		return -1;
	}

	public static int indexOf(char[] array, char c) {
		int i = 0;
		for (char t : array) {
			if (t == c)
				return i;
			i++;
		}
		return -1;
	}

	public static int indexOf(int[] array, int c) {
		int i = 0;
		for (int t : array) {
			if (t == c)
				return i;
			i++;
		}
		return -1;
	}

	public static double safeAcos(double d) {
		if (d > 1)
			d = 1;
		if (d < -1)
			d = -1;
		return Math.acos(d);
	}

	static public Vector3 circumcenterOf(Vector3 A, Vector3 B, Vector3 C) {
		Vector3 r = new Vector3();
		if (A.z == 0.0 && B.z == 0.0 && C.z == 0.0) {
			double u = ((A.x - B.x) * (A.x + B.x) + (A.y - B.y) * (A.y + B.y)) / 2.0;
			double v = ((B.x - C.x) * (B.x + C.x) + (B.y - C.y) * (B.y + C.y)) / 2.0;
			double den = (A.x - B.x) * (B.y - C.y) - (B.x - C.x) * (A.y - B.y);
			r.set(0, (float) ((u * (B.y - C.y) - v * (A.y - B.y)) / den));
			r.set(1, (float) ((v * (A.x - B.x) - u * (B.x - C.x)) / den));
			r.set(2, (float) 0.0);
		} else {
			Vector3 BmA = new Vector3();
			Vector3 CmA = new Vector3();
			BmA.sub(B, A);
			CmA.sub(C, A);

			double BC = BmA.dot(CmA);
			double B2 = BmA.magSquared(), C2 = CmA.magSquared();
			double den = 2.0 * (B2 * C2 - BC * BC);
			double s = C2 * (B2 - BC) / den;
			double t = B2 * (C2 - BC) / den;

			Vector3.add(BmA, (float) s, A, r);
			Vector3.add(CmA, (float) t, r, r);
		}
		return r;
	}

	static public Vector2 circumcenterOf(Vector2 A, Vector2 B, Vector2 C) {
		Vector2 r = new Vector2();

		Vector2 BmA = new Vector2();
		Vector2 CmA = new Vector2();
		BmA.sub(B, A);
		CmA.sub(C, A);

		double BC = BmA.dot(CmA);
		double B2 = BmA.mag() * BmA.mag(), C2 = CmA.mag() * CmA.mag();
		double den = 2.0 * (B2 * C2 - BC * BC);
		double s = C2 * (B2 - BC) / den;
		double t = B2 * (C2 - BC) / den;

		Vector2.add(BmA, (float) s, A, r);
		Vector2.add(CmA, (float) t, r, r);

		return r;
	}

}