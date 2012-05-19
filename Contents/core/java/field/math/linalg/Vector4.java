/*
 * $RCSfile: Vector4f.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/02/18 16:28:16 $
 * $State: Exp $
 */

package field.math.linalg;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.python.core.Py;
import org.python.core.PyInteger;
import org.python.core.PyNone;
import org.python.core.PyObject;
import org.python.core.PySequence;
import org.python.core.PySlice;

import field.math.abstraction.iBlendAlgebra;
import field.math.abstraction.iBlendable;
import field.math.util.CubicTools;

/**
 * A 4-element vector represented by single-precision floating point x,y,z,w
 * coordinates.
 * 
 */
public class Vector4 extends Tuple4 implements java.io.Serializable, iToFloatArray, iBlendable<Vector4>, iBlendAlgebra<Vector4> {

	// Compatible with 1.1
	static final long serialVersionUID = 8749319902347760659L;

	/**
	 * Constructs and initializes a Vector4f to (0,0,0,0).
	 */
	public Vector4() {
		super();
	}

	public Vector4(double d, double e, double f, double g) {
		super((float) d, (float) e, (float) f, (float) g);
	}

	public Vector4(Vector3 a, float w) {
		super((float) a.x, (float) a.y, (float) a.z, w);
	}

	/**
	 * Constructs and initializes a Vector4f from the specified xyzw
	 * coordinates.
	 * 
	 * @param x
	 *                the x coordinate
	 * @param y
	 *                the y coordinate
	 * @param z
	 *                the z coordinate
	 * @param w
	 *                the w coordinate
	 */
	public Vector4(float x, float y, float z, float w) {
		super(x, y, z, w);
	}

	/**
	 * Constructs and initializes a Vector4f from the array of length 4.
	 * 
	 * @param v
	 *                the array of length 4 containing xyzw in order
	 */
	public Vector4(float[] v) {
		super(v);
	}

	/**
	 * Constructs and initializes a Vector4f from the specified Tuple4f.
	 * 
	 * @param t1
	 *                the Tuple4f containing the initialization x y z w data
	 */
	public Vector4(Tuple4 t1) {
		super(t1);
	}

	/**
	 * Constructs and initializes a Vector4f from the specified Tuple3f. The
	 * x,y,z components of this vector are set to the corresponding
	 * components of tuple t1. The w component of this vector is set to 0.
	 * 
	 * @param t1
	 *                the tuple to be copied
	 * 
	 * @since Java 3D 1.2
	 */
	public Vector4(Vector3 t1) {
		super(t1.x, t1.y, t1.z, 0.0f);
	}

	/**
	 * Constructs and initializes a Vector4f from the specified Vector4f.
	 * 
	 * @param v1
	 *                the Vector4f containing the initialization x y z w
	 *                data
	 */
	public Vector4(Vector4 v1) {
		super(v1);
	}

	public Vector4(FloatBuffer color, int i) {
		super(color.get(i * 4 + 0), color.get(i * 4 + 1), color.get(i * 4 + 2), color.get(i * 4 + 3));
	}

	public Vector4(FloatBuffer peek) {
		super(peek.get(),peek.get(),peek.get(),peek.get());
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
		int stop = ((PySlice) key).start instanceof PyNone ? 4 : ((PyInteger) ((PySlice) key).stop).getValue();
		int step = ((PySlice) key).start instanceof PyNone ? 1 : ((PyInteger) ((PySlice) key).step).getValue();
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
		return 4;
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
		case 3:
			w = ((Number) v.__tojava__(Number.class)).floatValue();
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

	@Override
	public Vector4 absolute() {
		return (Vector4) super.absolute();
	}

	// overridden with covarient returns

	@Override
	public Vector4 absolute(Tuple4 t) {
		return (Vector4) super.absolute(t);
	}

	@Override
	public Vector4 add(Tuple4 t1) {
		return (Vector4) super.add(t1);
	}

	@Override
	public Vector4 add(Tuple4 t1, Tuple4 t2) {
		return (Vector4) super.add(t1, t2);
	}

	public Vector4 add(Vector4 t, Vector4 a, float thisLayerMultiplier) {
		this.x = t.x + a.x * thisLayerMultiplier;
		this.y = t.y + a.y * thisLayerMultiplier;
		this.z = t.z + a.z * thisLayerMultiplier;
		this.w = t.w + a.w * thisLayerMultiplier;
		return this;
	}

	/**
	 * Returns the (4-space) angle in radians between this vector and the
	 * vector parameter; the return value is constrained to the range
	 * [0,PI].
	 * 
	 * @param v1
	 *                the other vector
	 * @return the angle in radians in the range [0,PI]
	 */
	public final float angle(Vector4 v1) {
		double vDot = this.dot(v1) / (this.length() * v1.length());
		if (vDot < -1.0)
			vDot = -1.0;
		if (vDot > 1.0)
			vDot = 1.0;
		return ((float) (Math.acos(vDot)));
	}

	public Vector4 blendRepresentation_add(Vector4 one, Vector4 two, Vector4 out) {
		if (out == null)
			return new Vector4(one).add(two);
		return out.setValue(one).add(two);
	}

	public Vector4 blendRepresentation_duplicate(Vector4 t) {
		return new Vector4(t);
	}

	public Vector4 blendRepresentation_multiply(float by, Vector4 input) {
		return new Vector4(input).scale(by);
	}

	public Vector4 blendRepresentation_newZero() {
		return new Vector4();
	}

	public Vector4 cerp(Vector4 before, float beforeTime, Vector4 now, float nowTime, Vector4 next, float nextTime, Vector4 after, float afterTime, float a) {
		CubicTools.cubic(a, before, beforeTime, now, nowTime, next, nextTime, after, afterTime, this);
		return this;
	}

	@Override
	public Vector4 clamp(float min, float max) {
		return (Vector4) super.clamp(min, max);
	}

	@Override
	public Vector4 clamp(float min, float max, Tuple4 t) {
		return (Vector4) super.clamp(min, max, t);
	}

	@Override
	public Vector4 clampMax(float max) {
		return (Vector4) super.clampMax(max);
	}

	@Override
	public Vector4 clampMax(float max, Tuple4 t) {
		return (Vector4) super.clampMax(max, t);
	}

	@Override
	public Vector4 clampMin(float min) {
		return (Vector4) super.clampMin(min);
	}

	@Override
	public Vector4 clampMin(float min, Tuple4 t) {
		return (Vector4) super.clampMin(min, t);
	}

	public float distanceFrom(Vector4 value) {
		return (float) Math.sqrt((value.x - x) * (value.x - x) + (value.y - y) * (value.y - y) + (value.z - z) * (value.z - z) + (value.w - w) * (value.w - w));
	}

	/**
	 * returns the dot product of this vector and v1
	 * 
	 * @param v1
	 *                the other vector
	 * @return the dot product of this vector and v1
	 */
	public final float dot(Vector4 v1) {
		return (this.x * v1.x + this.y * v1.y + this.z * v1.z + this.w * v1.w);
	}

	public float[] get() {
		return new float[] { x, y, z, w };
	}

	public float get(int i) {
		switch (i) {
		case 0:
			return x;
		case 1:
			return y;
		case 2:
			return z;
		case 3:
			return w;
		}
		throw new ArrayIndexOutOfBoundsException(" on vec4 :" + i);
	}

	@Override
	public float getItem(int i) {
		switch (i) {
		case 0:
			return x;
		case 1:
			return y;
		case 2:
			return z;
		case 3:
			return w;
		}
		return 0;
	}

	public Vector4 hadd(Vector4 in, Vector3 to, float weight) {
		this.x = in.x + to.x * weight;
		this.y = in.y + to.y * weight;
		this.z = in.z + to.z * weight;
		this.w = in.w + weight;
		return this;
	}

	public int hashcodeFor() {
		return Float.floatToIntBits(x) + 31 * (Float.floatToIntBits(y) + 31 * (Float.floatToIntBits(z) + 31 * Float.floatToIntBits(w)));
	}

	@Override
	public Vector4 interpolate(Tuple4 t1, float alpha) {
		return (Vector4) super.interpolate(t1, alpha);
	}

	@Override
	public Vector4 interpolate(Tuple4 t1, Tuple4 t2, float alpha) {
		return (Vector4) super.interpolate(t1, t2, alpha);
	}

	/**
	 * Returns the length of this vector.
	 * 
	 * @return the length of this vector as a float
	 */
	public final float length() {
		return (float) Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w);
	}

	/**
	 * Returns the squared length of this vector
	 * 
	 * @return the squared length of this vector as a float
	 */
	public final float lengthSquared() {
		return (this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w);
	}

	public Vector4 lerp(Vector4 c0, Vector4 c1, float c) {
		float mc = (1 - c);
		this.x = c0.x * mc + c * c1.x;
		this.y = c0.y * mc + c * c1.y;
		this.z = c0.z * mc + c * c1.z;
		this.w = c0.w * mc + c * c1.w;
		return this;
	}

	// / ----------------------- python access
	// --------------------------------------------

	public Vector4 lerp(Vector4 a, Vector4 b, Vector4 x) {
		this.x = a.x * (1 - x.x) + x.x * b.x;
		this.y = a.y * (1 - x.y) + x.y * b.y;
		this.z = a.z * (1 - x.z) + x.z * b.z;
		this.w = a.w * (1 - x.w) + x.w * b.w;
		return this;
	}

	public float mag() {
		return (float) Math.sqrt(x * x + y * y + z * z + w * w);
	}

	@Override
	public Vector4 negate() {
		return (Vector4) super.negate();
	}

	@Override
	public Vector4 negate(Tuple4 t1) {
		return (Vector4) super.negate(t1);
	}

	/**
	 * Normalizes this vector in place.
	 * 
	 * @return
	 */
	public final Vector4 normalize() {
		float norm;

		norm = (float) (1.0 / Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w));
		this.x *= norm;
		this.y *= norm;
		this.z *= norm;
		this.w *= norm;
		return this;
	}

	/**
	 * Sets the value of this vector to the normalization of vector v1.
	 * 
	 * @param v1
	 *                the un-normalized vector
	 */
	public final Vector4 normalize(Vector4 v1) {
		float norm;

		norm = (float) (1.0 / Math.sqrt(v1.x * v1.x + v1.y * v1.y + v1.z * v1.z + v1.w * v1.w));
		this.x = v1.x * norm;
		this.y = v1.y * norm;
		this.z = v1.z * norm;
		this.w = v1.w * norm;
		return this;
	}

	@Override
	public Vector4 scale(float s) {
		return (Vector4) super.scale(s);
	}

	@Override
	public Vector4 scale(float s, Tuple4 t1) {
		return (Vector4) super.scale(s, t1);
	}

	@Override
	public Vector4 scaleAdd(float s, Tuple4 t1) {
		return (Vector4) super.scaleAdd(s, t1);
	}

	@Override
	public Vector4 scaleAdd(float s, Tuple4 t1, Tuple4 t2) {
		return (Vector4) super.scaleAdd(s, t1, t2);
	}

	@Override
	public Vector4 set(float x, float y, float z, float w) {
		return (Vector4) super.set(x, y, z, w);
	}

	@Override
	public Vector4 set(float[] t) {
		return (Vector4) super.set(t);
	}

	public void set(int i, float to) {
		switch (i) {
		case 0:
			x = to;
			break;
		case 1:
			y = to;
			break;
		case 2:
			z = to;
			break;
		case 3:
			w = to;
			break;
		default:
			throw new ArrayIndexOutOfBoundsException(" on vec4 :" + i);
		}
	}

	@Override
	public Vector4 set(Tuple4 t1) {
		return (Vector4) super.set(t1);
	}

	/**
	 * Sets the x,y,z components of this vector to the corresponding
	 * components of tuple t1. The w component of this vector is set to 0.
	 * 
	 * @param t1
	 *                the tuple to be copied
	 * @return
	 * 
	 * @since Java 3D 1.2
	 */
	public final Vector4 set(Vector3 t1) {
		this.x = t1.x;
		this.y = t1.y;
		this.z = t1.z;
		this.w = 0.0f;
		return this;
	}

	public Vector4 setValue(Vector4 to) {
		this.set(to);
		return this;
	}

	@Override
	public Vector4 sub(Tuple4 t1) {
		return (Vector4) super.sub(t1);
	}

	@Override
	public Vector4 sub(Tuple4 t1, Tuple4 t2) {
		return (Vector4) super.sub(t1, t2);
	}

	public Color toColor() {
		return new Color(x < 0 ? 0 : (x > 1 ? 1 : x), y < 0 ? 0 : (y > 1 ? 1 : y), z < 0 ? 0 : (z > 1 ? 1 : z), w < 0 ? 0 : (w > 1 ? 1 : w));
	}

	private float getIndex(int key) {
		switch (key) {
		case 0:
			return x;
		case 1:
			return y;
		case 2:
			return z;
		case 3:
			return w;
		}
		throw new ArrayIndexOutOfBoundsException(key);
	}

	public Vector3 toVector3() {
		return new Vector3(x, y, z);
	}

	public int toInt() {
		int ia = (int) Math.max(0, Math.min(255, w * 255));
		int ir = (int) Math.max(0, Math.min(255, x * 255));
		int ig = (int) Math.max(0, Math.min(255, y * 255));
		int ib = (int) Math.max(0, Math.min(255, z * 255));

		return (ia << 24) | (ir << 16) | (ig << 8) | ib;
	}

	public boolean isNaN() {
		return Float.isNaN(x) ||Float.isInfinite(x) ||Float.isNaN(y) ||Float.isInfinite(y) ||Float.isNaN(z) ||Float.isInfinite(z) ||Float.isNaN(w) ||Float.isInfinite(w);
	}

}
