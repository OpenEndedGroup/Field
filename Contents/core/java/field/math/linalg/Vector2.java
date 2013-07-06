/*
 * $RCSfile: Tuple2f.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.3 $
 * $Date: 2005/02/18 16:28:14 $
 * $State: Exp $
 */

package field.math.linalg;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.python.core.Py;
import org.python.core.PyInteger;
import org.python.core.PyNone;
import org.python.core.PyObject;
import org.python.core.PySequence;
import org.python.core.PySlice;

import field.core.ui.text.protect.ClassDocumentationProtect.Comp;
import field.math.abstraction.iBlendable;
import field.math.util.CubicTools;

/**
 * A generic 2-element tuple that is represented by single-precision floating
 * point x,y coordinates.
 * 
 */
public class Vector2 implements java.io.Serializable, Cloneable, iToFloatArray, iBlendable<Vector2> {

	static final long serialVersionUID = 9011180388985266884L;

	public static Vector2 add(Vector2 a, float w, Vector2 b, Vector2 out) {
		if (out == null)
			out = new Vector2();
		out.x = a.x * w + b.x;
		out.y = a.y * w + b.y;
		return out;
	}

	/**
	 * The x coordinate.
	 */
	public float x;

	/**
	 * The y coordinate.
	 */
	public float y;

	/**
	 * Constructs and initializes a Tuple2f to (0,0).
	 */
	public Vector2() {
		this.x = (float) 0.0;
		this.y = (float) 0.0;
	}

	public Vector2(double d, double e) {
		this.x = (float) d;
		this.y = (float) e;
	}

	/**
	 * Constructs and initializes a Tuple2f from the specified xy
	 * coordinates.
	 * 
	 * @param x
	 *                the x coordinate
	 * @param y
	 *                the y coordinate
	 */
	public Vector2(float x, float y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Constructs and initializes a Tuple2f from the specified array.
	 * 
	 * @param t
	 *                the array of length 2 containing xy in order
	 */
	public Vector2(float[] t) {
		this.x = t[0];
		this.y = t[1];
	}

	/**
	 * Constructs and initializes a Tuple2f from the specified Tuple2f.
	 * 
	 * @param t1
	 *                the Tuple2f containing the initialization x y data
	 */
	public Vector2(Vector2 t1) {
		this.x = t1.x;
		this.y = t1.y;
	}

	public Vector2(FloatBuffer position) {
		this(position.get(), position.get());
	}

	public Vector2(String[] q, int i) {
		this.x = Float.parseFloat(q[i]);
		this.y = Float.parseFloat(q[i+1]);
	}

	public PyObject __finditem__(int key) {
		try {
			return Py.java2py(getIndex(key));
		} catch (IndexOutOfBoundsException exc) {
			return null;
		}
	}

	public PyObject __finditem__(PyObject key) {
		if (key instanceof PyInteger) {
			return __finditem__(((PyInteger) key).getValue());
		}
		int start = ((PySlice) key).start instanceof PyNone ? 0 : ((PyInteger) ((PySlice) key).start).getValue();
		int stop = ((PySlice) key).stop instanceof PyNone ? 2 : ((PyInteger) ((PySlice) key).stop).getValue();
		int step = ((PySlice) key).step instanceof PyNone ? 1 : ((PyInteger) ((PySlice) key).step).getValue();
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
		int start = ((PySlice) key).start instanceof PyNone ? 0 : ((PyInteger) ((PySlice) key).start).getValue();
		int stop = ((PySlice) key).start instanceof PyNone ? __len__() : ((PyInteger) ((PySlice) key).stop).getValue();
		int step = ((PySlice) key).start instanceof PyNone ? 1 : ((PyInteger) ((PySlice) key).step).getValue();
		List<Float> r = new ArrayList<Float>();
		for (int i = start; i < stop; i += step) {
			r.add(getIndex(i));
		}
		return Py.java2py(r);
	}

	public int __len__() {
		return 2;
	}

	public void __setitem__(int value, PyObject v) {
		switch (value) {
		case 0:
			x = ((Number) v.__tojava__(Number.class)).floatValue();
			return;
		case 1:
			y = ((Number) v.__tojava__(Number.class)).floatValue();
			return;
		}
		throw new ArrayIndexOutOfBoundsException(value);
	}

	public void __setitem__(PyObject key, PyObject value) {
		if (key instanceof PyInteger) {
			__setitem__(((PyInteger) key).getValue(), value);
		} else if (key instanceof PySlice) {
			int start = ((PySlice) key).start instanceof PyNone ? 0 : ((PyInteger) ((PySlice) key).start).getValue();
			int stop = ((PySlice) key).start instanceof PyNone ? __len__() : ((PyInteger) ((PySlice) key).stop).getValue();
			int step = ((PySlice) key).start instanceof PyNone ? 1 : ((PyInteger) ((PySlice) key).step).getValue();
			for (int i = start; i < stop; i += step) {
				PyObject v = null;
				if (value instanceof PySequence)
					v = ((PySequence) value).__getitem__(i % ((PySequence) value).__len__());
				else
					v = value;
				__setitem__(i, v);
			}
		}
	}

	/**
	 * Sets each component of this tuple to its absolute value.
	 */
	public final void absolute() {
		x = Math.abs(x);
		y = Math.abs(y);
	}

	/**
	 * Sets each component of the tuple parameter to its absolute value and
	 * places the modified values into this tuple.
	 * 
	 * @param t
	 *                the source tuple, which will not be modified
	 */
	public final void absolute(Vector2 t) {
		x = Math.abs(t.x);
		y = Math.abs(t.y);
	}

	/**
	 * Sets the value of this tuple to the vector sum of itself and tuple
	 * t1.
	 * 
	 * @param t1
	 *                the other tuple
	 */
	public final Vector2 add(Vector2 t1) {
		this.x += t1.x;
		this.y += t1.y;
		return this;
	}

	/**
	 * Sets the value of this tuple to the vector sum of tuples t1 and t2.
	 * 
	 * @param t1
	 *                the first tuple
	 * @param t2
	 *                the second tuple
	 * @return
	 */
	public final Vector2 add(Vector2 t1, Vector2 t2) {
		this.x = t1.x + t2.x;
		this.y = t1.y + t2.y;
		return this;
	}

	/**
	 * Returns the angle in radians between this vector and the vector
	 * parameter; the return value is constrained to the range [0,PI].
	 * 
	 * @param v1
	 *                the other vector
	 * @return the angle in radians in the range [0,PI]
	 */
	public final float angle(Vector2 v1) {
		double vDot = this.dot(v1) / (this.length() * v1.length());
		if (vDot < -1.0)
			vDot = -1.0;
		if (vDot > 1.0)
			vDot = 1.0;
		return ((float) (Math.acos(vDot)));
	}
	
	/**
	 * Returns the signed angle in radians between this vector and the vector
	 * parameter; the return value is constrained to the range [-PI,PI].
	 * 
	 * @param b
	 *                the other vector
	 * @return the angle in radians in the range [-PI,PI] +ve is counter-clockwise from this to b
	 */
	public final float signedAngle(Vector2 b) {
		float angle = angle(b);
		if(this.x*b.y - this.y*b.x < 0)
			    angle = -angle;
		return angle;
	}

	public Vector2 blendRepresentation_newZero() {
		return new Vector2();
	}

	public Vector2 cerp(Vector2 before, float beforeTime, Vector2 now, float nowTime, Vector2 next, float nextTime, Vector2 after, float afterTime, float a) {

		CubicTools.cubic(a, before, beforeTime, now, nowTime, next, nextTime, after, afterTime, this);
		return this;
	}

	/**
	 * Clamps this tuple to the range [low, high].
	 * 
	 * @param min
	 *                the lowest value in this tuple after clamping
	 * @param max
	 *                the highest value in this tuple after clamping
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

	}

	/**
	 * Clamps the tuple parameter to the range [low, high] and places the
	 * values into this tuple.
	 * 
	 * @param min
	 *                the lowest value in the tuple after clamping
	 * @param max
	 *                the highest value in the tuple after clamping
	 * @param t
	 *                the source tuple, which will not be modified
	 */
	public final void clamp(float min, float max, Vector2 t) {
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

	}

	/**
	 * Clamps the maximum value of this tuple to the max parameter.
	 * 
	 * @param max
	 *                the highest value in the tuple after clamping
	 */
	public final void clampMax(float max) {
		if (x > max)
			x = max;
		if (y > max)
			y = max;
	}

	/**
	 * Clamps the maximum value of the tuple parameter to the max parameter
	 * and places the values into this tuple.
	 * 
	 * @param max
	 *                the highest value in the tuple after clamping
	 * @param t
	 *                the source tuple, which will not be modified
	 */
	public final void clampMax(float max, Vector2 t) {
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

	}

	/**
	 * Clamps the minimum value of this tuple to the min parameter.
	 * 
	 * @param min
	 *                the lowest value in this tuple after clamping
	 */
	public final void clampMin(float min) {
		if (x < min)
			x = min;
		if (y < min)
			y = min;
	}

	/**
	 * Clamps the minimum value of the tuple parameter to the min parameter
	 * and places the values into this tuple.
	 * 
	 * @param min
	 *                the lowest value in the tuple after clamping
	 * @param t
	 *                the source tuple, which will not be modified
	 */
	public final void clampMin(float min, Vector2 t) {
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

	}

	/**
	 * Creates a new object of the same class as this object.
	 * 
	 * @return a clone of this instance.
	 * @exception OutOfMemoryError
	 *                    if there is not enough memory.
	 * @see java.lang.Cloneable
	 * @since Java 3D 1.3
	 */
	@Override
	public Object clone() {
		// Since there are no arrays we can just use
		// Object.clone()
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			// this shouldn't happen, since
			// we are Cloneable
			throw new InternalError();
		}
	}

	public float distanceFrom(Vector2 b) {
		return (float) Math.sqrt((x - b.x) * (x - b.x) + (y - b.y) * (y - b.y));
	}

	/**
	 * Computes the dot product of the this vector and vector v1.
	 * 
	 * @param v1
	 *                the other vector
	 */
	public final float dot(Vector2 v1) {
		return (this.x * v1.x + this.y * v1.y);
	}

	/**
	 * Returns true if the L-infinite distance between this tuple and tuple
	 * t1 is less than or equal to the epsilon parameter, otherwise returns
	 * false. The L-infinite distance is equal to MAX[abs(x1-x2),
	 * abs(y1-y2)].
	 * 
	 * @param t1
	 *                the tuple to be compared to this tuple
	 * @param epsilon
	 *                the threshold value
	 * @return true or false
	 */
	public boolean epsilonEquals(Vector2 t1, float epsilon) {
		float diff;

		diff = x - t1.x;
		if ((diff < 0 ? -diff : diff) > epsilon)
			return false;

		diff = y - t1.y;
		if ((diff < 0 ? -diff : diff) > epsilon)
			return false;

		return true;
	}

	/**
	 * Returns true if the Object t1 is of type Tuple2f and all of the data
	 * members of t1 are equal to the corresponding data members in this
	 * Tuple2f.
	 * 
	 * @param t1
	 *                the object with which the comparison is made
	 * @return true or false
	 */
	@Override
	public boolean equals(Object t1) {
		try {
			Vector2 t2 = (Vector2) t1;
			return (this.x == t2.x && this.y == t2.y);
		} catch (NullPointerException e2) {
			return false;
		} catch (ClassCastException e1) {
			return false;
		}

	}

	/**
	 * Returns true if all of the data members of Tuple2f t1 are equal to
	 * the corresponding data members in this Tuple2f.
	 * 
	 * @param t1
	 *                the vector with which the comparison is made
	 * @return true or false
	 */
	public boolean equals(Vector2 t1) {
		try {
			return (this.x == t1.x && this.y == t1.y);
		} catch (NullPointerException e2) {
			return false;
		}

	}

	public float[] get() {
		return new float[] { x, y };
	}

	/**
	 * Copies the value of the elements of this tuple into the array t.
	 * 
	 * @param t
	 *                the array that will contain the values of the vector
	 */
	public final void get(float[] t) {
		t[0] = this.x;
		t[1] = this.y;
	}

	public float getItem(int value) {
		switch (value) {
		case 0:
			return x;
		case 1:
			return y;
		}
		return 0;
	}

	/**
	 * Returns a hash code value based on the data values in this object.
	 * Two different Tuple2f objects with identical data values (i.e.,
	 * Tuple2f.equals returns true) will return the same hash code value.
	 * Two objects with different data members may return the same hash
	 * value, although this is not likely.
	 * 
	 * @return the integer hash code value
	 */
	@Override
	public int hashCode() {
		long bits = 1L;
		bits = 31L * bits + Float.floatToIntBits(x);
		bits = 31L * bits + Float.floatToIntBits(y);
		return (int) (bits ^ (bits >> 32));
	}

	/**
	 * Linearly interpolates between this tuple and tuple t1 and places the
	 * result into this tuple: this = (1-alpha)*this + alpha*t1.
	 * 
	 * @param t1
	 *                the first tuple
	 * @param alpha
	 *                the alpha interpolation parameter
	 */
	public final Vector2 interpolate(Vector2 t1, float alpha) {

		this.x = (1 - alpha) * this.x + alpha * t1.x;
		this.y = (1 - alpha) * this.y + alpha * t1.y;

		return this;
	}

	/**
	 * Linearly interpolates between tuples t1 and t2 and places the result
	 * into this tuple: this = (1-alpha)*t1 + alpha*t2.
	 * 
	 * @param t1
	 *                the first tuple
	 * @param t2
	 *                the second tuple
	 * @param alpha
	 *                the alpha interpolation parameter
	 * @return
	 */
	public final Vector2 interpolate(Vector2 t1, Vector2 t2, float alpha) {
		this.x = (1 - alpha) * t1.x + alpha * t2.x;
		this.y = (1 - alpha) * t1.y + alpha * t2.y;
		return this;
	}

	public boolean isNaN() {
		return Float.isNaN(x) || Float.isNaN(y) || Float.isInfinite(x) || Float.isInfinite(y);
	}

	/**
	 * Returns the length of this vector.
	 * 
	 * @return the length of this vector
	 */
	public final float length() {
		return (float) Math.sqrt(this.x * this.x + this.y * this.y);
	}

	/**
	 * Returns the squared length of this vector.
	 * 
	 * @return the squared length of this vector
	 */
	public final float lengthSquared() {
		return (this.x * this.x + this.y * this.y);
	}

	public Vector2 lerp(Vector2 a, Vector2 b, float f) {
		x = a.x * (1 - f) + b.x * f;
		y = a.y * (1 - f) + b.y * f;
		return this;
	}

	public float mag() {
		return (float) Math.sqrt(x * x + y * y);
	}

	public Vector2 max(Vector2 o) {
		this.x = Math.max(this.x, o.x);
		this.y = Math.max(this.y, o.y);
		return this;
	}

	public Vector2 min(Vector2 o) {
		this.x = Math.min(this.x, o.x);
		this.y = Math.min(this.y, o.y);
		return this;
	}

	public Vector2 mul(Vector2 scale) {
		this.x *= scale.x;
		this.y *= scale.y;
		return this;
	}

	/**
	 * Negates the value of this vector in place.
	 * 
	 * @return
	 */
	public final Vector2 negate() {
		this.x = -this.x;
		this.y = -this.y;
		return this;
	}

	/**
	 * Sets the value of this tuple to the negation of tuple t1.
	 * 
	 * @param t1
	 *                the source tuple
	 */
	public final void negate(Vector2 t1) {
		this.x = -t1.x;
		this.y = -t1.y;
	}

	public Vector2 noise(float noise) {
		this.x += (Math.random() - 0.5) * noise;
		this.y += (Math.random() - 0.5) * noise;
		return this;
	}

	/**
	 * Normalizes this vector in place.
	 */
	public final Vector2 normalize() {
		float norm;

		float q = (float) Math.sqrt(this.x * this.x + this.y * this.y);
		if (q > 0) {
			norm = (1.0f / q);
			this.x *= norm;
			this.y *= norm;
		}
		return this;
	}

	/**
	 * Sets the value of this vector to the normalization of vector v1.
	 * 
	 * @param v1
	 *                the un-normalized vector
	 */
	public final void normalize(Vector2 v1) {
		float norm;

		norm = (float) (1.0 / Math.sqrt(v1.x * v1.x + v1.y * v1.y));
		this.x = v1.x * norm;
		this.y = v1.y * norm;
	}

	// / ----------------------- python access
	// --------------------------------------------

	public Vector2 orthogonal() {
		float xt = -y;
		this.y = x;
		this.x = xt;
		return this;
	}

	public Vector2 projectOut(Vector2 direction) {
		float magDir = direction.mag();

		float dot = this.dot(direction) / (magDir * magDir);

		this.x -= dot * direction.x;
		this.y -= dot * direction.y;
		return this;
	}

	public Vector2 projectOut(Vector2 direction, float f) {
		float magDir = direction.mag();

		float dot = this.dot(direction) / (magDir * magDir);

		this.x -= dot * direction.x * f;
		this.y -= dot * direction.y * f;
		return this;
	}

	/**
	 * Sets the value of this tuple to the scalar multiplication of itself.
	 * 
	 * @param s
	 *                the scalar value
	 */
	public final Vector2 scale(float s) {
		this.x *= s;
		this.y *= s;
		return this;
	}

	/**
	 * Sets the value of this tuple to the scalar multiplication of tuple
	 * t1.
	 * 
	 * @param s
	 *                the scalar value
	 * @param t1
	 *                the source tuple
	 */
	public final void scale(float s, Vector2 t1) {
		this.x = s * t1.x;
		this.y = s * t1.y;
	}

	/**
	 * Sets the value of this tuple to the scalar multiplication of itself
	 * and then adds tuple t1 (this = s*this + t1).
	 * 
	 * @param s
	 *                the scalar value
	 * @param t1
	 *                the tuple to be added
	 */
	public final void scaleAdd(float s, Vector2 t1) {
		this.x = s * this.x + t1.x;
		this.y = s * this.y + t1.y;
	}

	/**
	 * Sets the value of this tuple to the scalar multiplication of tuple t1
	 * and then adds tuple t2 (this = s*t1 + t2).
	 * 
	 * @param s
	 *                the scalar value
	 * @param t1
	 *                the tuple to be multipled
	 * @param t2
	 *                the tuple to be added
	 */
	public final void scaleAdd(float s, Vector2 t1, Vector2 t2) {
		this.x = s * t1.x + t2.x;
		this.y = s * t1.y + t2.y;
	}

	public Vector2 scaleOnAxis(Vector2 axis, float hscale) {
		float d = axis.dot(this) * (hscale - 1);
		Vector2.add(axis, d, this, this);
		return this;
	}

	/**
	 * Sets the value of this tuple to the specified xy coordinates.
	 * 
	 * @param x
	 *                the x coordinate
	 * @param y
	 *                the y coordinate
	 */
	public final Vector2 set(float x, float y) {
		this.x = x;
		this.y = y;
		return this;
	}

	/**
	 * Sets the value of this tuple from the 2 values specified in the
	 * array.
	 * 
	 * @param t
	 *                the array of length 2 containing xy in order
	 */
	public final void set(float[] t) {
		this.x = t[0];
		this.y = t[1];
	}

	/**
	 * Sets the value of this tuple to the value of the Tuple2f argument.
	 * 
	 * @param t1
	 *                the tuple to be copied
	 */
	public final void set(Vector2 t1) {
		this.x = t1.x;
		this.y = t1.y;
	}

	public void setItem(int value, double v) {
		switch (value) {
		case 0:
			x = (float) v;
			return;
		case 1:
			y = (float) v;
			return;
		}
		throw new ArrayIndexOutOfBoundsException(value);
	}

	public Vector2 setValue(Vector2 to) {
		this.x = to.x;
		this.y = to.y;
		return this;
	}

	/**
	 * Sets the value of this tuple to the vector difference of itself and
	 * tuple t1 (this = this - t1).
	 * 
	 * @param t1
	 *                the other tuple
	 * @return
	 */
	public final Vector2 sub(Vector2 t1) {
		this.x -= t1.x;
		this.y -= t1.y;
		return this;
	}

	/**
	 * Sets the value of this tuple to the vector difference of tuple t1 and
	 * t2 (this = t1 - t2).
	 * 
	 * @param t1
	 *                the first tuple
	 * @param t2
	 *                the second tuple
	 */
	public final Vector2 sub(Vector2 t1, Vector2 t2) {
		this.x = t1.x - t2.x;
		this.y = t1.y - t2.y;
		return this;
	}

	/**
	 * Returns a string that contains the values of this Tuple2f. The form
	 * is (x,y).
	 * 
	 * @return the String representation
	 */
	@Override
	public String toString() {
		return ("(" + this.x + ", " + this.y + ")");
	}

	public Vector3 toVector3() {
		return new Vector3(x, y, 0);
	}

	/**
	 * rotates this vector anti-clockwise
	 */
	public Vector2 rotateBy(float radians) {

		double s = Math.sin(radians);
		double c = Math.cos(radians);

		double x = c * this.x - s * this.y;
		double y = s * this.x + c * this.y;

		this.x = (float) x;
		this.y = (float) y;

		return this;
	}

	private float getIndex(int key) {
		switch (key) {
		case 0:
			return x;
		case 1:
			return y;
		}
		throw new ArrayIndexOutOfBoundsException(key);
	}

	static public List<Comp> getClassCustomCompletion(String prefix, Object on) {
		if (prefix.equals("")) {
			ArrayList<Comp> c = new ArrayList<Comp>();
			Comp c0 = new Comp("(0)", "the <i>x</i> component of this Vector2, currently <b>" + ((Vector2) on).x);
			Comp c1 = new Comp("(1)", "the <i>y</i> component of this Vector2, current <b>" + ((Vector2) on).y);
			c.add(c0);
			c.add(c1);
			return c;
		} else {
			return null;
		}
	}

	public Vector2 add(Vector2 vector2, float f) {
		this.x += vector2.x * f;
		this.y += vector2.y * f;
		return this;
	}
}
