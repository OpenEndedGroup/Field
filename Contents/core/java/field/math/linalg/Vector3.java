package field.math.linalg;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.python.core.Py;
import org.python.core.PyInteger;
import org.python.core.PyNone;
import org.python.core.PyObject;
import org.python.core.PySequence;
import org.python.core.PySlice;

import field.math.abstraction.iBlendAlgebra;
import field.math.abstraction.iBlendable;
import field.math.abstraction.iDistanceAlgebra;
import field.math.util.CubicTools;

/**
 * A generic 3-element tuple that is represented by single precision-floating
 * point x,y,z coordinates.
 * 
 */
public class Vector3 implements java.io.Serializable, Cloneable,
		iBlendAlgebra<Vector3>, iDistanceAlgebra<Vector3>, iToFloatArray,
		iBlendable<Vector3> {

	static final long serialVersionUID = 5019834619484343712L;

	/**
	 * The x coordinate.
	 */
	public float x;

	/**
	 * The y coordinate.
	 */
	public float y;

	/**
	 * The z coordinate.
	 */
	public float z;

	/**
	 * Constructs and initializes a Tuple3f from the specified xyz coordinates.
	 * 
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @param z
	 *            the z coordinate
	 */
	public Vector3(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Constructs and initializes a Tuple3f from the array of length 3.
	 * 
	 * @param t
	 *            the array of length 3 containing xyz in order
	 */
	public Vector3(float[] t) {
		this.x = t[0];
		this.y = t[1];
		this.z = t[2];
	}

	/**
	 * Constructs and initializes a Tuple3f from the specified Tuple3f.
	 * 
	 * @param t1
	 *            the Tuple3f containing the initialization x y z data
	 */
	public Vector3(Vector3 t1) {
		this.x = t1.x;
		this.y = t1.y;
		this.z = t1.z;
	}

	/**
	 * Constructs and initializes a Tuple3f to (0,0,0).
	 */
	public Vector3() {
		this.x = 0.0f;
		this.y = 0.0f;
		this.z = 0.0f;
	}

	public Vector3(double x, double y, double z) {
		this.x = (float) x;
		this.y = (float) y;
		this.z = (float) z;
	}

	public Vector3(FloatBuffer positions, int vertexNumber) {
		this.x = positions.get(3 * vertexNumber + 0);
		this.y = positions.get(3 * vertexNumber + 1);
		this.z = positions.get(3 * vertexNumber + 2);
	}

	public Vector3(FloatBuffer v) {
		this.x = v.get();
		this.y = v.get();
		this.z = v.get();
	}

	public Vector3(String[] p, int offset) {
		this.x = Float.parseFloat(p[offset]);
		this.y = Float.parseFloat(p[offset + 1]);
		this.z = Float.parseFloat(p[offset + 2]);
	}

	/**
	 * Returns a string that contains the values of this Tuple3f. The form is
	 * (x,y,z).
	 * 
	 * @return the String representation
	 */
	public String toString() {
		return "(" + this.x + ", " + this.y + ", " + this.z + ")";
	}

	/**
	 * Sets the value of this tuple to the specified xyz coordinates.
	 * 
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @param z
	 *            the z coordinate
	 * @return
	 */
	public final Vector3 set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	/**
	 * Sets the value of this tuple to the specified xyz coordinates.
	 * 
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @param z
	 *            the z coordinate
	 * @return
	 */
	public final Vector3 set(double x, double  y, double  z) {
		this.x = (float) x;
		this.y = (float) y;
		this.z = (float) z;
		return this;
	}

	public final Vector3 setValue(float x, float y, float z) {
		return this.set(x, y, z);
	}

	public final Vector3 setValue(Vector3 v) {
		return this.set(v);
	}

	/**
	 * Sets the value of this tuple to the xyz coordinates specified in the
	 * array of length 3.
	 * 
	 * @param t
	 *            the array of length 3 containing xyz in order
	 * @return
	 */
	public final Vector3 set(float[] t) {
		this.x = t[0];
		this.y = t[1];
		this.z = t[2];
		return this;
	}

	/**
	 * Sets the value of this tuple to the xyz coordinates specified in the
	 * array of length 3.
	 * 
	 * @param t
	 *            the array of length 3 containing xyz in order
	 * @return
	 */
	public Vector3 set(double[] tmp_scale) {
		x = (float) tmp_scale[0];
		y = (float) tmp_scale[1];
		z = (float) tmp_scale[2];
		return this;
	}

	/**
	 * Sets the value of this tuple to the value of tuple t1.
	 * 
	 * @param t1
	 *            the tuple to be copied
	 */
	public final Vector3 set(Vector3 t1) {
		this.x = t1.x;
		this.y = t1.y;
		this.z = t1.z;
		return this;
	}

	/**
	 * Gets the value of this tuple and copies the values into t.
	 * 
	 * @param t
	 *            the array of length 3 into which the values are copied
	 */
	public final float[] get(float[] t) {
		if (t == null)
			t = new float[3];
		t[0] = this.x;
		t[1] = this.y;
		t[2] = this.z;
		return t;
	}

	public final float get(int index) {
		switch (index) {
		case 0:
			return x;
		case 1:
			return y;
		case 2:
			return z;
		}
		throw new ArrayIndexOutOfBoundsException(index);
	}

	/**
	 * Gets the value of this tuple and copies the values into t.
	 * 
	 * @param t
	 *            the Tuple3f object into which the values of this object are
	 *            copied
	 */
	public final void get(Vector3 t) {
		t.x = this.x;
		t.y = this.y;
		t.z = this.z;
	}

	/**
	 * Sets the value of this tuple to the vector sum of tuples t1 and t2.
	 * 
	 * @param t1
	 *            the first tuple
	 * @param t2
	 *            the second tuple
	 */
	public final Vector3 add(Vector3 t1, Vector3 t2) {
		this.x = t1.x + t2.x;
		this.y = t1.y + t2.y;
		this.z = t1.z + t2.z;
		return this;
	}

	/**
	 * Sets the value of this tuple to the vector sum of itself and tuple t1.
	 * 
	 * @param t1
	 *            the other tuple
	 */
	public final Vector3 add(Vector3 t1) {
		this.x += t1.x;
		this.y += t1.y;
		this.z += t1.z;
		return this;
	}

	/**
	 * Sets the value of this tuple to the vector difference of tuples t1 and t2
	 * (this = t1 - t2).
	 * 
	 * @param t1
	 *            the first tuple
	 * @param t2
	 *            the second tuple
	 * @return
	 */
	public final Vector3 sub(Vector3 t1, Vector3 t2) {
		this.x = t1.x - t2.x;
		this.y = t1.y - t2.y;
		this.z = t1.z - t2.z;
		return this;
	}

	/**
	 * Sets the value of this tuple to the vector difference of itself and tuple
	 * t1 (this = this - t1) .
	 * 
	 * @param t1
	 *            the other tuple
	 */
	public final Vector3 sub(Vector3 t1) {
		this.x -= t1.x;
		this.y -= t1.y;
		this.z -= t1.z;
		return this;
	}

	/**
	 * Sets the value of this tuple to the negation of tuple t1.
	 * 
	 * @param t1
	 *            the source tuple
	 * @return
	 */
	public final Vector3 negate(Vector3 t1) {
		this.x = -t1.x;
		this.y = -t1.y;
		this.z = -t1.z;
		return this;
	}

	/**
	 * Negates the value of this tuple in place.
	 * 
	 * @return
	 */
	public final Vector3 negate() {
		this.x = -this.x;
		this.y = -this.y;
		this.z = -this.z;
		return this;
	}

	/**
	 * Sets the value of this vector to the scalar multiplication of tuple t1.
	 * 
	 * @param s
	 *            the scalar value
	 * @param t1
	 *            the source tuple
	 * @return
	 */
	public final Vector3 scale(float s, Vector3 t1) {
		this.x = s * t1.x;
		this.y = s * t1.y;
		this.z = s * t1.z;
		return this;
	}

	/**
	 * Sets the value of this tuple to the scalar multiplication of the scale
	 * factor with this.
	 * 
	 * @param s
	 *            the scalar value
	 * @return
	 */
	public final Vector3 scale(float s) {
		this.x *= s;
		this.y *= s;
		this.z *= s;
		return this;
	}

	/**
	 * Sets the value of this tuple to the scalar multiplication of tuple t1 and
	 * then adds tuple t2 (this = s*t1 + t2).
	 * 
	 * @param s
	 *            the scalar value
	 * @param t1
	 *            the tuple to be scaled and added
	 * @param t2
	 *            the tuple to be added without a scale
	 * @return
	 */
	public final Vector3 scaleAdd(float s, Vector3 t1, Vector3 t2) {
		this.x = s * t1.x + t2.x;
		this.y = s * t1.y + t2.y;
		this.z = s * t1.z + t2.z;
		return this;
	}

	/**
	 * Sets the value of this tuple to the scalar multiplication of itself and
	 * then adds tuple t1 (this = s*this + t1).
	 * 
	 * @param s
	 *            the scalar value
	 * @param t1
	 *            the tuple to be added
	 * @return
	 */
	public final Vector3 scaleAdd(float s, Vector3 t1) {
		this.x = s * this.x + t1.x;
		this.y = s * this.y + t1.y;
		this.z = s * this.z + t1.z;
		return this;
	}

	/**
	 * Returns true if the Object t1 is of type Tuple3f and all of the data
	 * members of t1 are equal to the corresponding data members in this
	 * Tuple3f.
	 * 
	 * @param t1
	 *            the vector with which the comparison is made
	 * @return true or false
	 */
	public boolean equals(Vector3 t1) {
		try {
			return (this.x == t1.x && this.y == t1.y && this.z == t1.z);
		} catch (NullPointerException e2) {
			return false;
		}
	}

	/**
	 * Returns true if the Object t1 is of type Tuple3f and all of the data
	 * members of t1 are equal to the corresponding data members in this
	 * Tuple3f.
	 * 
	 * @param t1
	 *            the Object with which the comparison is made
	 * @return true or false
	 */
	public boolean equals(Object t1) {
		try {
			Vector3 t2 = (Vector3) t1;
			return (this.x == t2.x && this.y == t2.y && this.z == t2.z);
		} catch (NullPointerException e2) {
			return false;
		} catch (ClassCastException e1) {
			return false;
		}
	}

	/**
	 * Returns true if the L-infinite distance between this tuple and tuple t1
	 * is less than or equal to the epsilon parameter, otherwise returns false.
	 * The L-infinite distance is equal to MAX[abs(x1-x2), abs(y1-y2),
	 * abs(z1-z2)].
	 * 
	 * @param t1
	 *            the tuple to be compared to this tuple
	 * @param epsilon
	 *            the threshold value
	 * @return true or false
	 */
	public boolean epsilonEquals(Vector3 t1, float epsilon) {
		float diff;

		diff = x - t1.x;
		if ((diff < 0 ? -diff : diff) > epsilon)
			return false;

		diff = y - t1.y;
		if ((diff < 0 ? -diff : diff) > epsilon)
			return false;

		diff = z - t1.z;
		if ((diff < 0 ? -diff : diff) > epsilon)
			return false;

		return true;

	}

	/**
	 * Returns a hash code value based on the data values in this object. Two
	 * different Tuple3f objects with identical data values (i.e.,
	 * Tuple3f.equals returns true) will return the same hash code value. Two
	 * objects with different data members may return the same hash value,
	 * although this is not likely.
	 * 
	 * @return the integer hash code value
	 */
	public int hashCode() {
		long bits = 1L;
		bits = 31L * bits + (long) Float.floatToIntBits(x);
		bits = 31L * bits + (long) Float.floatToIntBits(y);
		bits = 31L * bits + (long) Float.floatToIntBits(z);
		return (int) (bits ^ (bits >> 32));
	}

	/**
	 * Clamps the tuple parameter to the range [low, high] and places the values
	 * into this tuple.
	 * 
	 * @param min
	 *            the lowest value in the tuple after clamping
	 * @param max
	 *            the highest value in the tuple after clamping
	 * @param t
	 *            the source tuple, which will not be modified
	 */
	public final void clamp(float min, float max, Vector3 t) {
		if (t.x > max) {
			x = max;
		} else if (t.x < min) {
			x = min;
		} else {
			x = t.x;
		}

		if (t.y > max) {
			y = max;
		} else if (t.y < min) {
			y = min;
		} else {
			y = t.y;
		}

		if (t.z > max) {
			z = max;
		} else if (t.z < min) {
			z = min;
		} else {
			z = t.z;
		}

	}

	/**
	 * Clamps the minimum value of the tuple parameter to the min parameter and
	 * places the values into this tuple.
	 * 
	 * @param min
	 *            the lowest value in the tuple after clamping
	 * @param t
	 *            the source tuple, which will not be modified
	 */
	public final void clampMin(float min, Vector3 t) {
		if (t.x < min) {
			x = min;
		} else {
			x = t.x;
		}

		if (t.y < min) {
			y = min;
		} else {
			y = t.y;
		}

		if (t.z < min) {
			z = min;
		} else {
			z = t.z;
		}

	}

	/**
	 * Clamps the maximum value of the tuple parameter to the max parameter and
	 * places the values into this tuple.
	 * 
	 * @param max
	 *            the highest value in the tuple after clamping
	 * @param t
	 *            the source tuple, which will not be modified
	 */
	public final void clampMax(float max, Vector3 t) {
		if (t.x > max) {
			x = max;
		} else {
			x = t.x;
		}

		if (t.y > max) {
			y = max;
		} else {
			y = t.y;
		}

		if (t.z > max) {
			z = max;
		} else {
			z = t.z;
		}

	}

	/**
	 * Sets each component of the tuple parameter to its absolute value and
	 * places the modified values into this tuple.
	 * 
	 * @param t
	 *            the source tuple, which will not be modified
	 */
	public final void absolute(Vector3 t) {
		x = Math.abs(t.x);
		y = Math.abs(t.y);
		z = Math.abs(t.z);
	}

	/**
	 * Clamps this tuple to the range [low, high].
	 * 
	 * @param min
	 *            the lowest value in this tuple after clamping
	 * @param max
	 *            the highest value in this tuple after clamping
	 */
	public final void clamp(float min, float max) {
		if (x > max) {
			x = max;
		} else if (x < min) {
			x = min;
		}

		if (y > max) {
			y = max;
		} else if (y < min) {
			y = min;
		}

		if (z > max) {
			z = max;
		} else if (z < min) {
			z = min;
		}

	}

	/**
	 * Clamps the minimum value of this tuple to the min parameter.
	 * 
	 * @param min
	 *            the lowest value in this tuple after clamping
	 */
	public final void clampMin(float min) {
		if (x < min)
			x = min;
		if (y < min)
			y = min;
		if (z < min)
			z = min;

	}

	/**
	 * Clamps the maximum value of this tuple to the max parameter.
	 * 
	 * @param max
	 *            the highest value in the tuple after clamping
	 */
	public final void clampMax(float max) {
		if (x > max)
			x = max;
		if (y > max)
			y = max;
		if (z > max)
			z = max;

	}

	/**
	 * Sets each component of this tuple to its absolute value.
	 */
	public final void absolute() {
		x = Math.abs(x);
		y = Math.abs(y);
		z = Math.abs(z);

	}

	/**
	 * Linearly interpolates between tuples t1 and t2 and places the result into
	 * this tuple: this = (1-alpha)*t1 + alpha*t2.
	 * 
	 * @param t1
	 *            the first tuple
	 * @param t2
	 *            the second tuple
	 * @param alpha
	 *            the alpha interpolation parameter
	 */
	public final Vector3 interpolate(Vector3 t1, Vector3 t2, float alpha) {
		this.x = (1 - alpha) * t1.x + alpha * t2.x;
		this.y = (1 - alpha) * t1.y + alpha * t2.y;
		this.z = (1 - alpha) * t1.z + alpha * t2.z;
		return this;
	}

	/**
	 * Linearly interpolates between this tuple and tuple t1 and places the
	 * result into this tuple: this = (1-alpha)*this + alpha*t1.
	 * 
	 * @param t1
	 *            the first tuple
	 * @param alpha
	 *            the alpha interpolation parameter
	 * @return
	 */
	public final Vector3 interpolate(Vector3 t1, float alpha) {
		this.x = (1 - alpha) * this.x + alpha * t1.x;
		this.y = (1 - alpha) * this.y + alpha * t1.y;
		this.z = (1 - alpha) * this.z + alpha * t1.z;
		return this;
	}

	/**
	 * Creates a new object of the same class as this object.
	 * 
	 * @return a clone of this instance.
	 * @exception OutOfMemoryError
	 *                if there is not enough memory.
	 * @see java.lang.Cloneable
	 * @since Java 3D 1.3
	 */
	public Object clone() {
		// Since there are no arrays we can just use Object.clone()
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			// this shouldn't happen, since we are Cloneable
			throw new InternalError();
		}
	}

	/**
	 * Returns the squared length of this vector.
	 * 
	 * @return the squared length of this vector
	 */
	public final float magSquared() {
		return (this.x * this.x + this.y * this.y + this.z * this.z);
	}

	/**
	 * Returns the length of this vector.
	 * 
	 * @return the length of this vector
	 */
	public final float mag() {
		return (float) Math.sqrt(this.x * this.x + this.y * this.y + this.z
				* this.z);
	}

	/**
	 * Sets this vector to be the vector cross product of vectors v1 and v2.
	 * 
	 * @param v1
	 *            the first vector
	 * @param v2
	 *            the second vector
	 * @return
	 */
	public final Vector3 cross(Vector3 v1, Vector3 v2) {
		float x, y;

		x = v1.y * v2.z - v1.z * v2.y;
		y = v2.x * v1.z - v2.z * v1.x;
		this.z = v1.x * v2.y - v1.y * v2.x;
		this.x = x;
		this.y = y;
		return this;
	}

	/**
	 * Computes the dot product of this vector and vector v1.
	 * 
	 * @param v1
	 *            the other vector
	 * @return the dot product of this vector and v1
	 */
	public final float dot(Vector3 v1) {
		return (this.x * v1.x + this.y * v1.y + this.z * v1.z);
	}

	/**
	 * Sets the value of this vector to the normalization of vector v1.
	 * 
	 * @param v1
	 *            the un-normalized vector
	 */
	public final void normalize(Vector3 v1) {
		float norm;

		norm = (float) (1.0 / Math
				.sqrt(v1.x * v1.x + v1.y * v1.y + v1.z * v1.z));
		this.x = v1.x * norm;
		this.y = v1.y * norm;
		this.z = v1.z * norm;
	}

	/**
	 * Normalizes this vector in place.
	 */
	public final Vector3 normalize() {
		float norm;

		norm = (float) (1.0 / Math.sqrt(this.x * this.x + this.y * this.y
				+ this.z * this.z));
		this.x *= norm;
		this.y *= norm;
		this.z *= norm;
		return this;
	}

	/**
	 * Returns the angle in radians between this vector and the vector
	 * parameter; the return value is constrained to the range [0,PI].
	 * 
	 * @param v1
	 *            the other vector
	 * @return the angle in radians in the range [0,PI]
	 */
	public final float angle(Vector3 v1) {
		double vDot = this.dot(v1) / (this.mag() * v1.mag());
		if (vDot < -1.0)
			vDot = -1.0;
		if (vDot > 1.0)
			vDot = 1.0;
		return ((float) (Math.acos(vDot)));
	}

	public float distanceFrom(Vector3 o) {
		return (float) Math.sqrt((x - o.x) * (x - o.x) + (y - o.y) * (y - o.y)
				+ (z - o.z) * (z - o.z));
	}

	public double distanceFromSquared(Vector3 o) {
		return (float) ((x - o.x) * (x - o.x) + (y - o.y) * (y - o.y)
				+ (z - o.z) * (z - o.z));
	}

	static public Vector3 add(Vector3 a, float w, Vector3 b, Vector3 o) {
		if (o == null)
			o = new Vector3();

		o.x = b.x + a.x * w;
		o.y = b.y + a.y * w;
		o.z = b.z + a.z * w;
		return o;
	}

	public Vector3 blendRepresentation_add(Vector3 a, Vector3 b, Vector3 o) {
		if (o == null)
			o = new Vector3();

		o.x = b.x + a.x;
		o.y = b.y + a.y;
		o.z = b.z + a.z;
		return o;
	}

	public Vector3 sub(Vector3 a, Vector3 b, Vector3 o) {
		if (o == null)
			o = new Vector3();

		o.x = -b.x + a.x;
		o.y = -b.y + a.y;
		o.z = -b.z + a.z;
		return o;
	}

	public Vector3 lerp(Vector3 from, Vector3 to, float a) {
		x = from.x * (1 - a) + to.x * (a);
		y = from.y * (1 - a) + to.y * (a);
		z = from.z * (1 - a) + to.z * (a);
		return this;
	}

	// for representation
	public Vector3 blendRepresentation_multiply(float by, Vector3 in) {
		in.x = in.x * by;
		in.y = in.y * by;
		in.z = in.z * by;
		return in;
	}

	public Vector3 blendRepresentation_newZero() {
		return new Vector3();
	}

	public Vector3 blendRepresentation_duplicate(Vector3 t) {
		return new Vector3(t);
	}

	public float distanceFrom(Vector3 from1, Vector3 from2) {
		return from1.distanceFrom(from2);
	}

	public float[] get() {
		return new float[] { x, y, z };
	}

	public Vector3 scale(Vector3 scale) {
		x *= scale.x;
		y *= scale.y;
		z *= scale.z;
		return this;
	}

	public Vector3 cerp(Vector3 before, float beforeTime, Vector3 now,
			float nowTime, Vector3 next, float nextTime, Vector3 after,
			float afterTime, float a) {
		CubicTools.cubic(a, before, beforeTime, now, nowTime, next, nextTime,
				after, afterTime, this);
		return this;
	}

	public Vector3 zero() {
		x = 0;
		y = 0;
		z = 0;
		return this;
	}

	public Vector3 set(int i, float z) {
		switch (i) {
		case 0:
			this.x = z;
			break;
		case 1:
			this.y = z;
			break;
		case 2:
			this.z = z;
			break;
		}
		return this;
	}

	public Vector3 setMagnitude(float f) {
		this.scale(f / this.mag());
		return this;
	}

	public Vector3 projectOut(Vector3 direction) {
		float magDir = direction.mag();

		float dot = this.dot(direction) / (magDir * magDir);

		this.x -= dot * direction.x;
		this.y -= dot * direction.y;
		this.z -= dot * direction.z;
		return this;
	}

	public Vector3 projectOut(Vector3 direction, float scale) {
		float magDir = direction.mag();

		float dot = this.dot(direction) / (magDir * magDir);

		this.x -= scale * dot * direction.x;
		this.y -= scale * dot * direction.y;
		this.z -= scale * dot * direction.z;
		return this;
	}

	// / ----------------------- python access
	// --------------------------------------------

	public void __setitem__(PyObject key, PyObject value) {
		if (key instanceof PyInteger) {
			__setitem__(((PyInteger) key).getValue(), value);
		} else if (key instanceof PySlice) {
			int start = ((PySlice) key).start instanceof PyNone ? 0
					: ((PyInteger) ((PySlice) key).start).getValue();
			int stop = ((PySlice) key).start instanceof PyNone ? __len__()
					: ((PyInteger) ((PySlice) key).stop).getValue();
			int step = ((PySlice) key).start instanceof PyNone ? 1
					: ((PyInteger) ((PySlice) key).step).getValue();
			for (int i = start; i < stop; i += step) {
				PyObject v = null;
				if (value instanceof PySequence)
					v = ((PySequence) value).__getitem__(i
							% ((PySequence) value).__len__());
				else
					v = value;
				__setitem__(i, v);
			}
		}
	}

	public void __setitem__(int value, PyObject v) {
		switch (value) {
		case 0:
			x = ((Number) v.__tojava__(Number.class)).floatValue();
			return;
		case 1:
			y = ((Number) v.__tojava__(Number.class)).floatValue();
			return;
		case 2:
			z = ((Number) v.__tojava__(Number.class)).floatValue();
			return;
		}
		throw new ArrayIndexOutOfBoundsException(value);
	}

	public PyObject __finditem__(int key) {
		try {
			return Py.java2py(getIndex(key));
		} catch (IndexOutOfBoundsException exc) {
			return null;
		}
	}

	private float getIndex(int key) {
		switch (key) {
		case 0:
			return x;
		case 1:
			return y;
		case 2:
			return z;
		}
		throw new ArrayIndexOutOfBoundsException(key);
	}

	public PyObject __finditem__(PyObject key) {
		if (key instanceof PyInteger) {
			return __finditem__(((PyInteger) key).getValue());
		}
		int start = ((PySlice) key).start instanceof PyNone ? 0
				: ((PyInteger) ((PySlice) key).start).getValue();
		int stop = ((PySlice) key).stop instanceof PyNone ? 3
				: ((PyInteger) ((PySlice) key).stop).getValue();
		int step = ((PySlice) key).step instanceof PyNone ? 1
				: ((PyInteger) ((PySlice) key).step).getValue();
		List<Float> r = new ArrayList<Float>();
		for (int i = start; i < stop; i += step) {
			r.add(getIndex(i));
		}
		return Py.java2py(r);
	}

	public PyObject __getitem__(PyObject key) {
		if (key instanceof PyInteger) {
			return __finditem__(((PyInteger) key).getValue());
		}
		int start = ((PySlice) key).start instanceof PyNone ? 0
				: ((PyInteger) ((PySlice) key).start).getValue();
		int stop = ((PySlice) key).start instanceof PyNone ? __len__()
				: ((PyInteger) ((PySlice) key).stop).getValue();
		int step = ((PySlice) key).start instanceof PyNone ? 1
				: ((PyInteger) ((PySlice) key).step).getValue();
		List<Float> r = new ArrayList<Float>();
		for (int i = start; i < stop; i += step) {
			r.add(getIndex(i));
		}
		return Py.java2py(r);
	}

	public int __len__() {
		return 3;
	}

	public Vector3 noise(double tt) {
		this.x += (Math.random() - 0.5) * tt;
		this.y += (Math.random() - 0.5) * tt;
		this.z += (Math.random() - 0.5) * tt;
		return this;
	}

	public Vector2 toVector2() {
		return new Vector2(x, y);
	}

	public boolean isNaN() {
		return Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z)
				|| Float.isInfinite(x) || Float.isInfinite(y)
				|| Float.isInfinite(z);
	}

	public void min(Vector3 a) {
		this.x = Math.min(this.x, a.x);
		this.y = Math.min(this.y, a.y);
		this.z = Math.min(this.z, a.z);
	}

	public void max(Vector3 a) {
		this.x = Math.max(this.x, a.x);
		this.y = Math.max(this.y, a.y);
		this.z = Math.max(this.z, a.z);
	}

	public Tuple4 toVector4() {
		return new Vector4(x, y, z, 1);
	}

	public double distanceFromWeighted(Vector3 o, Vector3 w) {
		return (float) Math.sqrt((x - o.x) * (x - o.x) * w.x + (y - o.y)
				* (y - o.y) * w.y + (z - o.z) * (z - o.z) * w.z);

	}

	public Vector2 toVector2H() {
		return new Vector2(x / z, y / z);
	}

	public Vector3 pow(float g) {
		x = (float) Math.pow(x, g);
		y = (float) Math.pow(y, g);
		z = (float) Math.pow(z, g);
		return this;
	}

	public Vector3 ipow(float g) {
		x = 1 - (float) Math.pow(1 - x, g);
		y = 1 - (float) Math.pow(1 - y, g);
		z = 1 - (float) Math.pow(1 - z, g);
		return this;
	}

	public void setValue(FloatBuffer v, int vertex) {
		x = v.get(vertex * 3 + 0);
		y = v.get(vertex * 3 + 1);
		z = v.get(vertex * 3 + 2);
	}

	public Vector3 multThree(Vector3 abc, Vector3 a, Vector3 b, Vector3 c) {
		this.x = abc.x * a.x + abc.y * b.x + abc.z * c.x;
		this.y = abc.x * a.y + abc.y * b.y + abc.z * c.y;
		this.z = abc.x * a.z + abc.y * b.z + abc.z * c.z;
		return this;
	}

	public void setItem(int value, double v) {
		switch (value) {
		case 0:
			x = (float) v;
			return;
		case 1:
			y = (float) v;
			return;
		case 2:
			z = (float) v;
			return;
		}
		throw new ArrayIndexOutOfBoundsException(value);
	}

	public float getItem(int value) {
		switch (value) {
		case 0:
			return x;
		case 1:
			return y;
		case 2:
			return z;
		}
		return 0;
	}

	public VectorN toVectorN() {
		return new VectorN(this);
	}

	public Vector3 add(Vector3 value, float a) {
		this.x += value.x * a;
		this.y += value.y * a;
		this.z += value.z * a;
		return this;
	}

	public Vector3 rotateBy(Vector3 axis, float angle) {
		return new Quaternion().set(axis, angle).rotateVector(this);
	}

	public Vector3 rotateBy(float angle) {
		return new Quaternion(angle).rotateVector(this);
	}

	static public Vector3 blend(List<Vector3> q, List<? extends Number> w) {
		Vector3 a = new Vector3();
		float tot = 0;

//		System.out.println(" blending over <" + q + "> <" + w + ">");

		for (int i = 0; i < q.size(); i++) {
			a.add(new Vector3(q.get(i)).scale(w.get(i).floatValue()));

			tot += w.get(i).floatValue();
//			System.out.println(" tot :" + w.get(i).floatValue() + " tot = "
//					+ tot);
		}

//		System.out.println(" total weight is <" + tot + ">");

		a.scale(1 / tot);

//		System.out.println(" returning <" + a + ">");
		return a;
	}

	public void generateComplementBasis(Vector3 rkU, Vector3 rkV) {
		float fInvLength;

		if (Math.abs(this.x) >= Math.abs(this.y)) {
			// W.x or W.z is the largest magnitude component, swap
			// them
			fInvLength = (float) Math.sqrt(this.x * this.x + this.z * this.z);
			rkU.x = -this.z * fInvLength;
			rkU.y = (float) 0.0;
			rkU.z = +this.x * fInvLength;
			rkV.x = this.y * rkU.z;
			rkV.y = this.z * rkU.x - this.x * rkU.z;
			rkV.z = -this.y * rkU.x;
		} else {
			// W.y or W.z is the largest magnitude component, swap
			// them
			fInvLength = (float) Math.sqrt(this.y * this.y + this.z * this.z);
			rkU.x = (float) 0.0;
			rkU.y = +this.z * fInvLength;
			rkU.z = -this.y * fInvLength;
			rkV.x = this.y * rkU.z - this.z * rkU.y;
			rkV.y = -this.x * rkU.z;
			rkV.z = this.x * rkU.y;
		}
	}

	public int maxIndex() {
		if (Math.abs(this.x) > Math.abs(this.y)) {
			if (Math.abs(this.x) > Math.abs(this.z)) {
				return 0;
			} else {
				return 2;
			}
		} else {
			if (Math.abs(this.y) > Math.abs(this.z)) {
				return 1;
			} else {
				return 2;
			}
		}
	}

	public int minIndex() {
		if (Math.abs(this.x) <= Math.abs(this.y)) {
			if (Math.abs(this.x) <= Math.abs(this.z)) {
				return 0;
			} else {
				return 2;
			}
		} else {
			if (Math.abs(this.y) <= Math.abs(this.z)) {
				return 1;
			} else {
				return 2;
			}
		}
	}
	
	public Vector3 computeAnOrthogonalVector3()
	{
		int x = maxIndex();
		int m = minIndex();
		
		Vector3 q = new Vector3(this);
		
		float a = q.get(x);
		float b = q.get(m);
		
		q.set(m, -a);
		q.set(x, b);
		
		return new Vector3().cross(this, q);
	}

	public Vector3 noise(Random r, float level) {
		this.x += (r.nextFloat()-0.5)*level;
		this.y += (r.nextFloat()-0.5)*level;
		this.z += (r.nextFloat()-0.5)*level;
		return this;
	}

	
}
