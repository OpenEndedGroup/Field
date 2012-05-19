package field.math.linalg;
import java.io.Serializable;
import java.util.Arrays;

import field.math.linalg.MatrixN.DimensionMismatchException;


/**
 * A vector of arbitrary but immutable length.
 * <P>
 *
 * Indices start at zero.
 */

public final class VectorN implements Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Adds <code>a</code> and <code>b</code> and stores the sum in <code>out</code>.<code>a</code>,<code>b</code>, and <code>out</code> must have equal numbers of elements.
	 *
	 * @exception DimensionMismatchException
	 *                     if dimensions are wrong It IS safe for a == out or b == out.
	 */
	public static void add(VectorN a, VectorN b, float w, VectorN out) {
		if (a.dim() != b.dim() || a.dim() != out.dim()) {
			throw new DimensionMismatchException("add: can't add Vec with different dimension");
		}
		for (int i = 0; i < a.dim(); i++) {
			out.rep[i] = a.rep[i] + w * b.rep[i];
		}
	}


	/**
	 * Adds <code>a</code> and <code>b</code> and stores the sum in <code>out</code>.<code>a</code>,<code>b</code>, and <code>out</code> must have equal numbers of elements.
	 *
	 * @exception DimensionMismatchException
	 *                     if dimensions are wrong It IS safe for a == out or b == out.
	 */
	public static void add(VectorN a, VectorN b, VectorN out) {
		if (a.dim() != b.dim() || a.dim() != out.dim()) {
			throw new DimensionMismatchException("add: can't add Vec with different dimension");
		}
		for (int i = 0; i < a.dim(); i++) {
			out.rep[i] = a.rep[i] + b.rep[i];
		}
	}

	/**
	 * homogeneous add
	 * @param at
	 * @param to
	 * @param weight
	 * @param at2
	 */
	public static void hadd(VectorN in, Vector3 to, float weight, VectorN out) {
		assert(in.rep.length==4);
		out.rep[0] = in.rep[0]+to.x*weight;
		out.rep[1] = in.rep[1]+to.y*weight;
		out.rep[2] = in.rep[2]+to.z*weight;
		out.rep[3] = in.rep[3]+weight;
	}

	/**
	 * Performs a weighted linear interpolation between <code>from</code> and <code>to</code> and places the result in <code>out</code>.
	 *
	 * @param from
	 *                The start point.
	 * @param alpha
	 *                The weighting. <code>alpha = 0</code> results in <code>out == from</code>.<code>alpha = 1</code> results in <code>out == to</code>.
	 */
	public static void lerp(VectorN from, float alpha, VectorN to, VectorN out) {
		for (int i = 0; i < from.dim(); i++) {
			out.set(i, from.get(i) * (1 - alpha) + to.get(i) * alpha);
		}
	}

	/**
	 * Test driver method.
	 */
	public static void main(String[] argv) {
		VectorN a = new VectorN(3);
		float[] arr = new float[3];
		arr[0] = 3.0f;
		arr[1] = 0.0f;
		arr[2] = 0.0f;
		VectorN b = new VectorN(arr);
		a.set(0, 0.0f);
		a.set(1, 2.0f);
		a.set(2, 0.0f);
		print("a: " + a);
		print("b: " + b);

		print("a dot b: " + a.dot(b));
		print("angle: " + a.angleBetween(b));
		print("cross: " + a.cross(b));
		print("amag2: " + a.magSquared());
		print("amag: " + a.mag());
		a.normalize();
		print("normalized a: " + a);
		print(" a dot (a cross b) better be 0.0: " + a.dot(a.cross(b)));

		print("a:" + a);
		print("b:" + b);

		print("a + b: " + a.add(b));

		print("a:" + a);
		print("b:" + b);

		print("a - b: " + a.sub(b));

		print("a - a: " + a.sub(a));

	}

	/**
	 * Subtracts <code>b</code> from <code>a</code> and stores the sum in <code>out</code>.<code>a</code>,<code>b</code>, and <code>out</code> must have equal numbers of elements.
	 *
	 * @exception DimensionMismatchException
	 *                     if dimensions are wrong It IS safe for a == out or b == out.
	 */
	public static void sub(VectorN a, VectorN b, VectorN out) {
		if (a.dim() != b.dim() || a.dim() != out.dim()) {
			throw new DimensionMismatchException("sub: can't add Vec with different dimension");
		}
		for (int i = 0; i < a.dim(); i++) {
			out.rep[i] = a.rep[i] - b.rep[i];
		}
	}

	/**
	 * Returns a copySource of <code>a</code>.
	 */
	protected static float[] copyArray(double[] a) {
		try {
			float[] cpy = new float[a.length];
			for (int i = 0; i < a.length; i++) {
				cpy[i] = (float) a[i];
			}
			return cpy;

		} catch (Exception e) {
			error(e.toString());
			return null;
		}
	}

	/**
	 * Returns a copySource of <code>a</code>.
	 */
	protected static float[] copyArray(float[] a) {
		try {
			float[] cpy = new float[a.length];
			for (int i = 0; i < a.length; i++) {
				cpy[i] = a[i];
			}
			return cpy;

		} catch (Exception e) {
			error(e.toString());
			return null;
		}
	}

	/**
	 * Convenience method to print to System.err
	 */
	protected static void error(String s) {
		System.err.println("(Vec): " + s);
	}

	/**
	 * Convenience wrapper around <code>System.out.println(String s)</code>
	 */
	protected static void print(String s) {
		System.out.println(s);
	}

	protected float[] rep;

	/**
	 * Constructs a 1-dimensional Vec <code>[x0]</code>.
	 */
	public VectorN(double x0) {
		this(1);
		set(0, (float) x0);
	}

	/**
	 * Constructs a 2-dimensional Vec <code>[x0 x1]</code>.
	 */
	public VectorN(double x0, double x1) {
		this(2);
		set(0, (float) x0);
		set(1, (float) x1);
	}

	/**
	 * Constructs a 3-dimensional Vec <code>[x0 x1 x2]</code>.
	 */
	public VectorN(double x0, double x1, double x2) {
		this(3);
		set(0, (float) x0);
		set(1, (float) x1);
		set(2, (float) x2);
	}

	/**
	 * Constructs a 4-dimensional Vec <code>[x0 x1 x2 x3]</code>.
	 */
	public VectorN(double x0, double x1, double x2, double x3) {
		this(4);
		set(0, (float) x0);
		set(1, (float) x1);
		set(2, (float) x2);
		set(3, (float) x3);
	}

	/**
	 * Creates a Vec by copying values from a float array.
	 */
	public VectorN(double[] v) {
		rep = copyArray(v);
	}

	/**
	 * Creates a Vec by copying values from a float array.
	 */
	public VectorN(float[] v) {
		rep = copyArray(v);
	}

	/**
	 * Creates a vector of dimension <code>dim</code>
	 */
	public VectorN(int dim) {
		rep = new float[dim];
	}

	public VectorN(Vector2 v) {
		this(2);
		set(0, v.x);
		set(1, v.y);
	}

	/**
	 * @param v
	 */
	public VectorN(Vector3 v) {
		this(3);
		set(0, v.get(0));
		set(1, v.get(1));
		set(2, v.get(2));
	}

	/**
	 * Copy constructor.
	 */
	public VectorN(VectorN v) {
		rep = copyArray(v.rep);
	}

	/**
	 * Returns <code>this + b</code>
	 *
	 * @exception DimensionMismatchException
	 *                     for mismatch dimensions
	 */
	public VectorN add(VectorN b) {
		VectorN out = new VectorN(this.dim());
		add(this, b, out);
		return out;
	}

	/**
	 * Returns the angle between <code>this</code> and <code>b</code>, in radians.
	 *
	 * @exception DimensionMismatchException
	 *                     if different dimension vectors
	 */
	public double angleBetween(VectorN b) {
		double amag = this.mag();
		double bmag = b.mag();
		if (amag == 0.0 || bmag == 0.0)
			return 0.0;

		double d = this.dot(b);
		d /= (amag * bmag);
		return field.math.BaseMath.acos(d);
	}

	/**
	 * Clones this Vec.
	 */
	@Override
	public Object clone() {
		try {
			VectorN v = (VectorN) super.clone();
			v.rep = copyArray(rep);
			return v;
		} catch (Exception e) {
			error(e.toString());
			return null;
		}
	}

	/**
	 * Convenience method that calls clone(), but saves the trouble of casting from an Object to a Vec.
	 */
	public VectorN copy() {
		return (VectorN) clone();
	}

	/**
	 * Copies the values of <code>from</code> into <code>this</code>.
	 *
	 * @exception DimensionMismatchException
	 *                     if this.dim() != from.dim()
	 */
	public void copyFrom(VectorN from) {
		if (dim() != from.dim()) {
			throw new DimensionMismatchException("copyFrom: different size arrays");
		}
		int i;
		for (i = 0; i < dim(); i++) {
			rep[i] = from.rep[i];
		}
	}

	/**
	 * Returns the cross product <code>this</code> x <code>b</code>.
	 *
	 * @exception IllegalArgumentException
	 *                     if this or b is not of dimension 3.
	 */
	public VectorN cross(VectorN b) throws IllegalArgumentException {
		if (dim() != 3 || b.dim() != 3) {
			throw new IllegalArgumentException("this.cross(Vec b, Vec out): this and b must dimension 3 vectors");
		}

		VectorN cross = new VectorN(3);
		cross.rep[0] = rep[1] * b.rep[2] - rep[2] * b.rep[1];
		cross.rep[1] = -rep[0] * b.rep[2] + rep[2] * b.rep[0];
		cross.rep[2] = rep[0] * b.rep[1] - rep[1] * b.rep[0];
		return cross;
	}

	/**
	 * Calculates the cross product <code>this</code> x <code>b</code> and stores it in <code>out</code>. It is acceptable for <code>this</code> to be <code>b</code> or <code>out</code>, or both.
	 *
	 * @exception IllegalArgumentException
	 *                     if this or b is not of dimension 3.
	 */
	public void cross(VectorN b, VectorN out) throws IllegalArgumentException {
		if (dim() != 3 || b.dim() != 3) {
			throw new IllegalArgumentException("this.cross(Vec b): this and b must dimension 3 vectors");
		}

		// in case output is larger than input
		if (out.dim() > 3)
			out.zero();

		float x = rep[1] * b.rep[2] - rep[2] * b.rep[1];
		float y = -rep[0] * b.rep[2] + rep[2] * b.rep[0];
		float z = rep[0] * b.rep[1] - rep[1] * b.rep[0];

		out.rep[0] = x;
		out.rep[1] = y;
		out.rep[2] = z;
	}

	/**
	 * Returns the number of elements.
	 */
	public int dim() {
		return dimension();
	}

	/**
	 * Returns the number of elements.
	 */
	public int dimension() {
		return rep.length;
	}

	/**
	 * Returns a normalized copySource of this vector.
	 */
	public VectorN direction() {
		VectorN vdir = this.copy();
		vdir.normalize();

		return vdir;
	}

	/**
	 * Returns the euclidean distance between <code>this</code> and <code>v</code>.
	 *
	 * @exception DimensionMismatchException
	 *                     if different dimensions
	 */
	public double distanceFrom(VectorN v) {
		if (dim() != v.dim()) {
			throw new DimensionMismatchException("scale: different size arrays");
		}
		double tot = 0;

		for (int i = 0; i < dimension(); i++)
			tot += Math.pow(rep[i] - v.rep[i], 2);
		return Math.sqrt(tot);
	}

	/**
	 * Returns the dot product <code>this</code> dot <code>v</code>.
	 *
	 * @exception DimensionMismatchException
	 *                     if different dimensions
	 */
	public double dot(VectorN v) {
		if (dim() != v.dim())
			throw new DimensionMismatchException("can't dot differing dimension vectors");

		double sum = 0.0;
		for (int i = 0; i < dimension(); i++)
			sum += rep[i] * v.rep[i];
		return sum;
	}

	/**
	 * Returns the <code>i</code> th element.
	 */
	public double elementAt(int i) {
		return ref(i);
	}

	/**
	 * Returns true if <code>otherVec</code> is a <code>Vec<code>, and all elements of <code>this</code> are equal to coresponding elements of <code>otherVec</code>.
	 */
	@Override
	public boolean equals(Object otherVec) {
		if (otherVec instanceof VectorN) {
			return equals((VectorN) otherVec);
		} else
			return false;
	}

	/**
	 * Returns true if all elements of <code>this</code> are equal to coresponding elements of <code>vec</code>.
	 */
	public boolean equals(VectorN vec) {
		return Arrays.equals(vec.rep, rep);
	}

	/**
	 * Copies the data in <code>v</code> to <code>this</code>.
	 */
	public void fromVec(VectorN v) {
		rep = copyArray(v.rep);
	}

	/**
	 * Copies the data in <code>v</code> to <code>this</code>.
	 */
	public void fromVec3(Vector3 v) {
		rep = new float[3];
		rep[0] = v.x;
		rep[1] = v.y;
		rep[2] = v.z;
	}

	/**
	 * Returns the <code>i</code> th element.
	 */
	public float get(int i) {
		return ref(i);
	}

	/**
	 * Returns the number of elements.
	 */
	public int getDimension() {
		return dim();
	}

	/**
	 * Returns the maximum value in the vector, a.k.a the L^infinity norm. Returns zero if the vector is of dimension zero.
	 */
	public double infinityNorm() {
		return max();
	}

	/**
	 * Returns the length of this vector.
	 */
	public double mag() {
		return Math.sqrt(magSquared());
	}

	/**
	 * Returns the squared length of this vector.
	 */
	public double magSquared() {
		double sum = 0.0;
		for (int i = 0; i < dimension(); i++) {
			sum += rep[i] * rep[i];
		}
		return sum;
	}

	/**
	 * Returns the size of the largest element, or zero if size() == 0.
	 */
	public double max() {
		if (rep.length == 0)
			return 0.0;

		if (rep.length == 1)
			return rep[0];

		double m = rep[0];
		int i;
		for (i = 1; i < rep.length; i++) {
			if (rep[i] > m)
				m = rep[i];
		}
		return m;
	}

	public int maxIndex() {
		if (rep.length == 0)
			return -1;

		if (rep.length == 1)
			return 0;

		double m = rep[0];
		int i;
		int index = 0;
		for (i = 1; i < rep.length; i++) {
			if (rep[i] > m) {
				m = rep[i];
				index = i;
			}
		}
		return index;
	}

	/**
	 * Returns the size of the smallest element. Returns zero if size() == 0.
	 */
	public double min() {
		if (rep.length == 0)
			return 0.0;

		if (rep.length == 1)
			return rep[0];

		double m = rep[0];
		int i;
		for (i = 1; i < rep.length; i++) {
			if (rep[i] < m)
				m = rep[i];
		}
		return m;
	}
	/**
	 * Scales this vector to be unit length.
	 */
	public void normalize() {
		double d = mag();
		if (d != 0.0)
			scale(1.0 / d);
	}

	/**
	 * Returns the outer product of <code>this</code> and <code>b</code>.
	 */
	public MatrixN outer(VectorN b) {
		int xn = dimension();
		int yn = b.dimension();
		MatrixN A = new MatrixN(xn, yn);
		for (int i = 0; i < xn; i++)
			for (int j = 0; j < yn; j++)
				A.set(i, j, ref(i) * b.ref(j));
		return A;
	}
	/**
	 * Calculates the outer product of <code>this</code> and <code>b</code>, storing it in <code>out</code>.
	 */
	public void outer(VectorN b, MatrixN out) {
		int xn = dimension();
		int yn = b.dimension();
		for (int i = 0; i < xn; i++)
			for (int j = 0; j < yn; j++)
				out.set(i, j, ref(i) * b.ref(j));
	}

	/**
	 * Raises each element in this vector to the <code>p</code> th power.
	 */
	public void pow(double p) {
		for (int i = 0; i < dimension(); i++)
			rep[i] = (float) Math.pow(rep[i], p);
	}
	/**
	 * Returns the <code>i</code> th element.
	 */
	public float ref(int i) {
		return rep[i];
	}

	/**
	 * Changes the dimension of this Vec. Truncates or pads with zeros as needed. If <code>n</code> is the current size, then nothing is done.
	 */
	public void resize(int n) {
		if (rep.length == n)
			return;

		float[] new_rep = new float[n];
		int i;
		for (i = 0; i < n; i++) {
			if (i < rep.length)
				new_rep[i] = rep[i];
			else
				new_rep[i] = 0.0f;
		}
		rep = new_rep;
	}

	/**
	 * Scales this vector by <code>d</code>.
	 */
	public void scale(double d) {
		for (int i = 0; i < dimension(); i++)
			rep[i] *= d;
	}

	/**
	 * Returns a new vector equal to <code>this</code> with the elements multiplied by corresponding elements in <code>v</code>. Does not change <code>this</code>.
	 *
	 * @exception DimensionMismatchException
	 *                     if different dimensions
	 */
	public VectorN scale(VectorN v) {
		if (dim() != v.dim()) {
			throw new DimensionMismatchException("scale: different size arrays");
		}
		VectorN out = new VectorN(this.dim());

		for (int i = 0; i < dimension(); i++)
			out.rep[i] = rep[i] * v.rep[i];
		return out;
	}

	/**
	 * Multiplies each element in <code>a</code> by the corresponding element in <code>b</code> and stores the result in <code>this</code>.
	 *
	 * @exception DimensionMismatchException
	 *                     if <code>a</code> and <code>b</code> are of different dimensions
	 */
	public void scale(VectorN a, VectorN b) {
		if ((dim() != a.dim()) || (dim() != b.dim())) {
			throw new DimensionMismatchException("scale: different size arrays");
		}
		for (int i = 0; i < dimension(); i++)
			rep[i] = a.rep[i] * b.rep[i];
	}

	/**
	 * Sets the <code>i</code> th element to <code>val</code>
	 */
	public void set(int i, double val) {
		rep[i] = (float) val;
	}

	/**
	 * Sets the <code>i</code> th element to <code>val</code>
	 */
	public void set(int i, float val) {
		rep[i] = val;
	}

	/**
	 * Copies the data in <code>v</code> to <code>this</code>.
	 */
	public void set(VectorN v) {
		fromVec(v);
	}

	/**
	 * Copies the data in <code>v</code> to <code>this</code>.
	 */
	public void setValue(float[] v) {
		int i;
		resize(v.length);
		for (i = 0; i < v.length; i++)
			set(i, v[i]);
	}

	/**
	 * Copies the data in <code>v</code> to <code>this</code>.
	 */
	public void setValue(Vector3 v) {
		fromVec3(v);
	}

	/**
	 * Copies the data in <code>v</code> to <code>this</code>.
	 */
	public void setValue(VectorN v) {
		fromVec(v);
	}
	/**
	 * Returns the number of elements.
	 */
	public int size() {
		return dim();
	}

	/**
	 * Returns <code>this - b</code>
	 *
	 * @exception DimensionMismatchException
	 *                     for mismatched dimension
	 */
	public VectorN sub(VectorN b) {
		VectorN out = new VectorN(this.dim());
		sub(this, b, out);
		return out;
	}

	/**
	 * Returns the sum of the elements.
	 */
	public double sum() {
		double sum = 0.0;
		for (int i = 0; i < dimension(); i++)
			sum += rep[i];
		return sum;
	}

	/**
	 * Returns the elements.
	 */
	public float[] toArray() {
		return rep;
	}

	/**
	 * @param tmp
	 * @return
	 */
	public float[] toArray(float[] tmp) {
		if (tmp==null) tmp = new float[this.dim()];
		if (tmp.length!=this.rep.length) tmp = new float[this.dim()];
		System.arraycopy(this.rep, 0, tmp, 0, this.rep.length);
		return tmp;
	}

	/**
	 * Convert this Vec into a n x 1 matrix.
	 */
	public MatrixN toColumnVector() {
		MatrixN a = new MatrixN(dim(), 1);
		int i;
		for (i = 0; i < dim(); i++) {
			a.set(i, 0, ref(i));
		}
		return a;
	}

	/**
	 * Converts this Vec into a 1 x n matrix.
	 */
	public MatrixN toRowVector() {
		MatrixN a = new MatrixN(1, dim());
		int i;
		for (i = 0; i < dim(); i++) {
			a.set(0, i, ref(i));
		}
		return a;
	}

	/**
	 * Returns a String representation.
	 */
	@Override
	public String toString() {
		String s = "[ ";
		if (dim() > 0) {
			for (int i = 0; i < dim() - 1; i++) {
				s = s + rep[i] + " ";
			}
			s = s + rep[dim() - 1] + "] ";
		} else {
			s += "] ";
		}
		return s;
	}

	/***********************************************************************************************************************************************************************************************************************************************************************
	 * Test Driver
	 **********************************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Copies the data in <code>this</code> to <code>v</code>.
	 */
	public Vector3 toVec3(Vector3 v) {
		if (v==null)
			v = new Vector3();
		if (dim() != 3)
			throw new DimensionMismatchException("Converting Vec to Vec3.");

		v.x = rep[0];
		v.y = rep[1];
		v.z = rep[2];
		return v;
	}

	/**
	 * Sets all elements in this vector to zero.
	 */
	public void zero() {
		scale(0.0);
	}

	/**
	 * Returns the internal representation for fast multiplies within subclasses.
	 */
	protected float[] getRep() {
		return rep;
	}


	public VectorN max(VectorN in) {
		for(int i=0;i<in.dim();i++)
		{
			this.rep[i] = Math.max(this.rep[i], in.rep[i]);
		}
		return this;
		
	}
	public VectorN min(VectorN in) {
		for(int i=0;i<in.dim();i++)
		{
			this.rep[i] = Math.min(this.rep[i], in.rep[i]);
		}
		return this;
		
	}


	public VectorN divide(VectorN sub) {
		for(int i=0;i<rep.length;i++)
		{
			this.rep[i] /= sub.get(i);
		}
		return this;
	}
}
