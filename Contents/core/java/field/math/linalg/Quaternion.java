/*
 * $RCSfile: Quat4f.java,v $
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

import java.util.List;

import field.math.BaseMath;

/**
 * A 4 element unit quaternion represented by single precision floating point x,y,z,w coordinates. The quaternion is always normalized.
 * 
 */
public class Quaternion extends Tuple4 implements java.io.Serializable {

	// Combatible with 1.1
	static final long serialVersionUID = 2675933778405442383L;

	final static double EPS = 0.000001;

	final static double EPS2 = 1.0e-30;

	final static double PIO2 = 1.57079632679;

	/**
	 * Constructs a (counter-clockwise) rotation around z of "angle".
	 * @param angle
	 */
	public Quaternion(float angle)
	{
		this.set(new Vector3(0,0,1), angle);
	}

	/**
	 * Constructs a (counter-clockwise) rotation around 'axis' of 'angle'
	 * @param axis
	 * @param angle
	 */
	public Quaternion(Vector3 axis, float angle)
	{
		this.set(axis, angle);
	}

	
	/**
	 * Constructs and initializes a Quat4f from the specified xyzw coordinates.
	 * 
	 * @param x
	 *                        the x coordinate
	 * @param y
	 *                        the y coordinate
	 * @param z
	 *                        the z coordinate
	 * @param w
	 *                        the w scalar component
	 */
	public Quaternion(float x, float y, float z, float w) {
		float mag;
		mag = (float) (1.0 / Math.sqrt(x * x + y * y + z * z + w * w));
		this.x = x * mag;
		this.y = y * mag;
		this.z = z * mag;
		this.w = w * mag;

	}

	/**
	 * Constructs and initializes a Quat4f from the array of length 4.
	 * 
	 * @param q
	 *                        the array of length 4 containing xyzw in order
	 */
	public Quaternion(float[] q) {
		float mag;
		mag = (float) (1.0 / Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]));
		x = q[0] * mag;
		y = q[1] * mag;
		z = q[2] * mag;
		w = q[3] * mag;
	}

	/**
	 * Constructs and initializes a Quat4f from the specified Quat4f.
	 * 
	 * @param q1
	 *                        the Quat4f containing the initialization x y z w data
	 */
	public Quaternion(Quaternion q1) {
		super(q1);
	}

	public Quaternion(Vector3 to, Vector3 from) {
		// Quaternion qfrom = new Quaternion(0.0, from.x(), from.y(), from.z());
		// Quaternion qto = new Quaternion(0.0, to.x(), to.y(), to.z());
		// qfrom.normalize();
		// qto.normalize();
		// qto.conjugate();
		//
		// Quaternion.mult(qto, qfrom, this);
		// this.normalize();
		// this.sqrt();

		Quaternion qfrom = new Quaternion(to);
		Quaternion qto = new Quaternion(from);

		qfrom.normalize();
		qto.normalize();

		qto.conjugate();
		this.mul(qfrom, qto);
		this.normalize();
		this.sqrt();
	}
	
	
	public Quaternion set(Vector3 to, Vector3 from)
	{
		Quaternion qfrom = new Quaternion(from);
		Quaternion qto = new Quaternion(to);

		qfrom.normalize();
		qto.normalize();

		qto.conjugate();
		this.mul(qfrom, qto);
		this.normalize();
		this.sqrt();	
		return this;
	}

	/**
	 * Constructs and initializes a Quat4f from the specified Tuple4f.
	 * 
	 * @param t1
	 *                        the Tuple4f containing the initialization x y z w data
	 */
	public Quaternion(Tuple4 t1) {
		float mag;
		mag = (float) (1.0 / Math.sqrt(t1.x * t1.x + t1.y * t1.y + t1.z * t1.z + t1.w * t1.w));
		x = t1.x * mag;
		y = t1.y * mag;
		z = t1.z * mag;
		w = t1.w * mag;

	}

	/**
	 * Constructs and initializes a Quat4f to (1.0,0.0,0.0,0.0).
	 */
	public Quaternion() {
		super(0, 0, 0, 1);
	}

	public Quaternion(Vector3 from) {
		super(from.x, from.y, from.z, 0);
	}

	// a rotation around positive z (comes 'out' of the 'page')
	public Quaternion(double d) {
		set(new AxisAngle(0,0,1,(float) d));
	}

	/**
	 * Sets the value of this quaternion to the conjugate of quaternion q1.
	 * 
	 * @param q1
	 *                        the source vector
	 * @return this (mutated)
	 */
	public final Quaternion conjugate(Quaternion q1) {
		this.x = -q1.x;
		this.y = -q1.y;
		this.z = -q1.z;
		this.w = q1.w;
		return this;
	}

	/**
	 * Sets the value of this quaternion to the conjugate of itself.
	 * 
	 * @return this (mutated)
	 */
	public final Quaternion conjugate() {
		this.x = -this.x;
		this.y = -this.y;
		this.z = -this.z;
		return this;
	}

	/**
	 * Sets the value of this quaternion to the quaternion product of quaternions q1 and q2 (this = q1 * q2). Note that this is safe for aliasing (e.g. this can be q1 or q2).
	 * 
	 * @param q1
	 *                        the first quaternion
	 * @param q2
	 *                        the second quaternion
	 * @return this (mutated)
	 * 
	 */
	public final Quaternion mul(Quaternion q1, Quaternion q2) {
		if (this != q1 && this != q2) {
			this.w = q1.w * q2.w - q1.x * q2.x - q1.y * q2.y - q1.z * q2.z;
			this.x = q1.w * q2.x + q2.w * q1.x + q1.y * q2.z - q1.z * q2.y;
			this.y = q1.w * q2.y + q2.w * q1.y - q1.x * q2.z + q1.z * q2.x;
			this.z = q1.w * q2.z + q2.w * q1.z + q1.x * q2.y - q1.y * q2.x;
		} else {
			float x, y, w;

			w = q1.w * q2.w - q1.x * q2.x - q1.y * q2.y - q1.z * q2.z;
			x = q1.w * q2.x + q2.w * q1.x + q1.y * q2.z - q1.z * q2.y;
			y = q1.w * q2.y + q2.w * q1.y - q1.x * q2.z + q1.z * q2.x;
			this.z = q1.w * q2.z + q2.w * q1.z + q1.x * q2.y - q1.y * q2.x;
			this.w = w;
			this.x = x;
			this.y = y;
		}
		return this;
	}

	/**
	 * Sets the value of this quaternion to the quaternion product of itself and q1 (this = this * q1).
	 * 
	 * @param q1
	 *                        the other quaternion
	 * @return this (mutated)
	 */
	public final Quaternion mul(Quaternion q1) {
		float x, y, w;

		w = this.w * q1.w - this.x * q1.x - this.y * q1.y - this.z * q1.z;
		x = this.w * q1.x + q1.w * this.x + this.y * q1.z - this.z * q1.y;
		y = this.w * q1.y + q1.w * this.y - this.x * q1.z + this.z * q1.x;
		this.z = this.w * q1.z + q1.w * this.z + this.x * q1.y - this.y * q1.x;
		this.w = w;
		this.x = x;
		this.y = y;
		return this;
	}

	/**
	 * Multiplies quaternion q1 by the inverse of quaternion q2 and places the value into this quaternion. The value of both argument quaternions is preservered (this = q1 * q2^-1).
	 * 
	 * @param q1
	 *                        the first quaternion
	 * @param q2
	 *                        the second quaternion
	 * @return this (mutated)
	 */
	public final Quaternion mulInverse(Quaternion q1, Quaternion q2) {
		Quaternion tempQuat = new Quaternion(q2);

		tempQuat.inverse();
		return this.mul(q1, tempQuat);

	}

	/**
	 * Multiplies this quaternion by the inverse of quaternion q1 and places the value into this quaternion. The value of the argument quaternion is preserved (this = this * q^-1).
	 * 
	 * @param q1
	 *                        the other quaternion
	 * @return this (mutated)
	 */
	public final Quaternion mulInverse(Quaternion q1) {
		Quaternion tempQuat = new Quaternion(q1);

		tempQuat.inverse();
		return this;
	}

	/**
	 * Sets the value of this quaternion to quaternion inverse of quaternion q1.
	 * 
	 * @param q1
	 *                        the quaternion to be inverted
	 * @return this (mutated)
	 */
	public final Quaternion inverse(Quaternion q1) {
		float norm;

		norm = 1.0f / (q1.w * q1.w + q1.x * q1.x + q1.y * q1.y + q1.z * q1.z);
		this.w = norm * q1.w;
		this.x = -norm * q1.x;
		this.y = -norm * q1.y;
		this.z = -norm * q1.z;
		return this;
	}

	/**
	 * Sets the value of this quaternion to the quaternion inverse of itself.
	 * 
	 * @return this (mutated)
	 */
	public final Quaternion inverse() {
		float norm;

		norm = 1.0f / (this.w * this.w + this.x * this.x + this.y * this.y + this.z * this.z);
		this.w *= norm;
		this.x *= -norm;
		this.y *= -norm;
		this.z *= -norm;
		return this;
	}

	/**
	 * Sets the value of this quaternion to the normalized value of quaternion q1.
	 * 
	 * @param q1
	 *                        the quaternion to be normalized.
	 * @return this (mutated)
	 */
	public final Quaternion normalize(Quaternion q1) {
		float norm;

		norm = (q1.x * q1.x + q1.y * q1.y + q1.z * q1.z + q1.w * q1.w);

		if (norm > 0.0f) {
			norm = 1.0f / (float) Math.sqrt(norm);
			this.x = norm * q1.x;
			this.y = norm * q1.y;
			this.z = norm * q1.z;
			this.w = norm * q1.w;
		} else {
			this.x = (float) 0.0;
			this.y = (float) 0.0;
			this.z = (float) 0.0;
			this.w = (float) 0.0;
		}
		return this;
	}

	/**
	 * Normalizes the value of this quaternion in place.
	 * 
	 * @return this (mutated)
	 */
	public final Quaternion normalize() {
		float norm;

		norm = (this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w);

		if (norm > 0.0f) {
			norm = 1.0f / (float) Math.sqrt(norm);
			this.x *= norm;
			this.y *= norm;
			this.z *= norm;
			this.w *= norm;
		} else {
			this.x = (float) 0.0;
			this.y = (float) 0.0;
			this.z = (float) 0.0;
			this.w = (float) 1.0;
		}
		return this;
	}

	/**
	 * Sets the value of this quaternion to the rotational component of the passed matrix.
	 * 
	 * @param m1
	 *                        the Matrix4f
	 * @return this (mutated)
	 */
	public final Quaternion set(Matrix4 m1) {
		float ww = 0.25f * (m1.m00 + m1.m11 + m1.m22 + m1.m33);

		if (ww >= 0) {
			if (ww >= EPS2) {
				this.w = (float) Math.sqrt((double) ww);
				ww = 0.25f / this.w;
				this.x = (m1.m21 - m1.m12) * ww;
				this.y = (m1.m02 - m1.m20) * ww;
				this.z = (m1.m10 - m1.m01) * ww;
				return this;
			}
		} else {
			this.w = 0;
			this.x = 0;
			this.y = 0;
			this.z = 1;
			return this;
		}

		this.w = 0;
		ww = -0.5f * (m1.m11 + m1.m22);

		if (ww >= 0) {
			if (ww >= EPS2) {
				this.x = (float) Math.sqrt((double) ww);
				ww = 1.0f / (2.0f * this.x);
				this.y = m1.m10 * ww;
				this.z = m1.m20 * ww;
				return this;
			}
		} else {
			this.x = 0;
			this.y = 0;
			this.z = 1;
			return this;
		}

		this.x = 0;
		ww = 0.5f * (1.0f - m1.m22);

		if (ww >= EPS2) {
			this.y = (float) Math.sqrt((double) ww);
			this.z = m1.m21 / (2.0f * this.y);
			return this;
		}

		this.y = 0;
		this.z = 1;

		return this;
	}

	/**
	 * Sets the value of this quaternion to the rotational component of the passed matrix.
	 * 
	 * @param m1
	 *                        the Matrix3f
	 * @return this (mutated)
	 */
	public final Quaternion set(Matrix3 m1) {
		float ww = 0.25f * (m1.m00 + m1.m11 + m1.m22 + 1.0f);

		if (ww >= 0) {
			if (ww >= EPS2) {
				this.w = (float) Math.sqrt((double) ww);
				ww = 0.25f / this.w;
				this.x = (m1.m21 - m1.m12) * ww;
				this.y = (m1.m02 - m1.m20) * ww;
				this.z = (m1.m10 - m1.m01) * ww;
				return this;
			}
		} else {
			this.w = 0;
			this.x = 0;
			this.y = 0;
			this.z = 1;
			return this;
		}

		this.w = 0;
		ww = -0.5f * (m1.m11 + m1.m22);
		if (ww >= 0) {
			if (ww >= EPS2) {
				this.x = (float) Math.sqrt((double) ww);
				ww = 0.5f / this.x;
				this.y = m1.m10 * ww;
				this.z = m1.m20 * ww;
				return this;
			}
		} else {
			this.x = 0;
			this.y = 0;
			this.z = 1;
			return this;
		}

		this.x = 0;
		ww = 0.5f * (1.0f - m1.m22);
		if (ww >= EPS2) {
			this.y = (float) Math.sqrt((double) ww);
			this.z = m1.m21 / (2.0f * this.y);
			return this;
		}

		this.y = 0;
		this.z = 1;
		return this;
	}

	/**
	 * Sets the value of this quaternion to the equivalent rotation of the AxisAngle argument.
	 * 
	 * @param a
	 *                        the AxisAngle to be emulated
	 * @return this (mutated)
	 */
	public final Quaternion set(AxisAngle a) {
		float mag, amag;
		// Quat = cos(theta/2) + sin(theta/2)(roation_axis)
		amag = (float) Math.sqrt(a.x * a.x + a.y * a.y + a.z * a.z);
		if (amag < EPS) {
			w = 0.0f;
			x = 0.0f;
			y = 0.0f;
			z = 0.0f;
		} else {
			amag = 1.0f / amag;
			mag = (float) Math.sin(a.angle / 2.0);
			w = (float) Math.cos(a.angle / 2.0);
			x = a.x * amag * mag;
			y = a.y * amag * mag;
			z = a.z * amag * mag;
		}
		return this;
	}

	/**
	 * Performs a great circle interpolation between this quaternion and the quaternion parameter and places the result into this quaternion.
	 * 
	 * @param q1
	 *                        the other quaternion
	 * @param alpha
	 *                        the alpha interpolation parameter
	 * @return this (mutated)
	 */
	public final Quaternion interpolate(Quaternion q1, float alpha) {
		// From "Advanced Animation and Rendering Techniques"
		// by Watt and Watt pg. 364, function as implemented appeared to be
		// incorrect. Fails to choose the same quaternion for the double
		// covering. Resulting in change of direction for rotations.
		// Fixed function to negate the first quaternion in the case that the
		// dot product of q1 and this is negative. Second case was not needed.

		double dot, s1, s2, om, sinom;

		dot = x * q1.x + y * q1.y + z * q1.z + w * q1.w;

		if (dot < 0) {
			// negate quaternion
			q1.x = -q1.x;
			q1.y = -q1.y;
			q1.z = -q1.z;
			q1.w = -q1.w;
			dot = -dot;
		}

		if ((1.0 - dot) > EPS) {
			om = Math.acos(dot);
			sinom = Math.sin(om);
			s1 = Math.sin((1.0 - alpha) * om) / sinom;
			s2 = Math.sin(alpha * om) / sinom;
		} else {
			s1 = 1.0 - alpha;
			s2 = alpha;
		}

		w = (float) (s1 * w + s2 * q1.w);
		x = (float) (s1 * x + s2 * q1.x);
		y = (float) (s1 * y + s2 * q1.y);
		z = (float) (s1 * z + s2 * q1.z);

		return this;
	}
	
	public final Quaternion interpolateToUnity(float alpha)
	{
		double dot, s1, s2, om, sinom;

		double one = 1;
		
		dot = w;
		
		if (w < 0) {
			// negate quaternion
			one = -1;
			dot = -dot;
		}

		if ((1.0 - dot) > EPS) {
			om = Math.acos(dot);
			sinom = Math.sin(om);
			s1 = Math.sin((1.0 - alpha) * om) / sinom;
			s2 = Math.sin(alpha * om) / sinom;
		} else {
			s1 = 1.0 - alpha;
			s2 = alpha;
		}

		w = (float) (s1 * w + s2 * one);
		x = (float) (s1 * x );
		y = (float) (s1 * y );
		z = (float) (s1 * z );

		return this;
	}

	/**
	 * Performs a great circle interpolation between quaternion q1 and quaternion q2 and places the result into this quaternion.
	 * 
	 * @param q1
	 *                        the first quaternion
	 * @param q2
	 *                        the second quaternion
	 * @param alpha
	 *                        the alpha interpolation parameter
	 * @return this (mutated)
	 */
	public final Quaternion interpolate(Quaternion q1, Quaternion q2, float alpha) {
		// From "Advanced Animation and Rendering Techniques"
		// by Watt and Watt pg. 364, function as implemented appeared to be
		// incorrect. Fails to choose the same quaternion for the double
		// covering. Resulting in change of direction for rotations.
		// Fixed function to negate the first quaternion in the case that the
		// dot product of q1 and this is negative. Second case was not needed.

		double dot, s1, s2, om, sinom;

		dot = q2.x * q1.x + q2.y * q1.y + q2.z * q1.z + q2.w * q1.w;

		if (dot < 0) {
			// negate quaternion
			q1.x = -q1.x;
			q1.y = -q1.y;
			q1.z = -q1.z;
			q1.w = -q1.w;
			dot = -dot;
		}

		if ((1.0 - dot) > EPS) {
			om = Math.acos(dot);
			sinom = Math.sin(om);
			s1 = Math.sin((1.0 - alpha) * om) / sinom;
			s2 = Math.sin(alpha * om) / sinom;
		} else {
			s1 = 1.0 - alpha;
			s2 = alpha;
		}
		w = (float) (s1 * q1.w + s2 * q2.w);
		x = (float) (s1 * q1.x + s2 * q2.x);
		y = (float) (s1 * q1.y + s2 * q2.y);
		z = (float) (s1 * q1.z + s2 * q2.z);

		return this;
	}

	Quaternion tmp = null;
	/**
	 * rotates parameter by this quaterion
	 * 
	 * @param v,
	 *                        the vector to rotate
	 * @return v, mutated
	 */
	public Vector3 rotateVector(Vector3 v) {

		if (tmp==null)
			tmp = new Quaternion();
		
		// this should be inlined --- the double inverse is silly
		
		this.mul(v, tmp);
		this.inverse();
		tmp.mul(this, v);
		this.inverse();
		return v;
	}

	/**
	 * rotates parameter by this quaterion
	 * 
	 * @param v,
	 *                        the vector to rotate
	 * @return v, mutated
	 */
	public Vector3 rotateVector(Vector3 v, Vector3 out) {
		if (out == null) out = new Vector3();
		if (tmp==null)
			tmp = new Quaternion();
		
		this.mul(v, tmp);
		this.inverse();
		tmp.mul(this, out);
		this.inverse();
		return out;
	}

	/**
	 * Returns a string that contains the values of this Quaternion4f. The form is (w,x,y,z).
	 * 
	 * @return the String representation
	 */
	public String toString() {
		return "{" + this.w + ", " + this.x + ", " + this.y + ", " + this.z + "}";
	}

	final void mul(Tuple4 q1, Tuple4 out) {
		float x, y, w;

		w = this.w * q1.w - this.x * q1.x - this.y * q1.y - this.z * q1.z;
		x = this.w * q1.x + q1.w * this.x + this.y * q1.z - this.z * q1.y;
		y = this.w * q1.y + q1.w * this.y - this.x * q1.z + this.z * q1.x;
		out.z = this.w * q1.z + q1.w * this.z + this.x * q1.y - this.y * q1.x;
		out.w = w;
		out.x = x;
		out.y = y;
	}

	final void mul(Vector3 q1, Tuple4 out) {
		float x, y, w;

		w = this.w * 0 - this.x * q1.x - this.y * q1.y - this.z * q1.z;
		x = this.w * q1.x + 0 * this.x + this.y * q1.z - this.z * q1.y;
		y = this.w * q1.y + 0 * this.y - this.x * q1.z + this.z * q1.x;
		out.z = this.w * q1.z + 0 * this.z + this.x * q1.y - this.y * q1.x;
		out.w = w;
		out.x = x;
		out.y = y;
	}

	final void mul(Tuple4 q1, Vector3 out) {
		float x, y;

		x = this.w * q1.x + q1.w * this.x + this.y * q1.z - this.z * q1.y;
		y = this.w * q1.y + q1.w * this.y - this.x * q1.z + this.z * q1.x;
		out.z = this.w * q1.z + q1.w * this.z + this.x * q1.y - this.y * q1.x;
		out.x = x;
		out.y = y;
	}

	public Quaternion sqrt() {
		return pow(0.5f);
	}

	public Quaternion pow(float p) {
		ln();
		x *= p;
		y *= p;
		z *= p;
		return exp();
	}

	public Quaternion exp() {
		double omega = mag();
		double sinc_omega = BaseMath.sinc(omega);

		this.w = (float) Math.cos(omega);
		this.x = (float) (this.x * sinc_omega);
		this.y = (float) (this.y * sinc_omega);
		this.z = (float) (this.z * sinc_omega);
		this.normalize();
		return this;
	}

	public double mag() {
		return Math.sqrt(x * x + y * y + z * z + w * w);
	}

	public Quaternion ln() {

		float EPSILON = 1e-10f;
		float omega = (float) BaseMath.acos(w);
		w = 0;

		float sinc = (float) BaseMath.sinc(omega);
		if (Math.abs(sinc) < EPSILON) sinc = EPSILON;

		x/=sinc;
		y/=sinc;
		z/=sinc;
		
		return this;
	}
	
	

	@Override
	public Quaternion absolute() {
		return (Quaternion) super.absolute();
	}

	@Override
	public Quaternion absolute(Tuple4 t) {
		return (Quaternion) super.absolute(t);
	}

	@Override
	public Quaternion add(Tuple4 t1, Tuple4 t2) {
		return (Quaternion) super.add(t1, t2);
	}

	@Override
	public Quaternion add(Tuple4 t1) {
		return (Quaternion) super.add(t1);
	}

	@Override
	public Quaternion clamp(float min, float max, Tuple4 t) {
		return (Quaternion) super.clamp(min, max, t);
	}

	@Override
	public Quaternion clamp(float min, float max) {
		return (Quaternion) super.clamp(min, max);
	}

	@Override
	public Quaternion clampMax(float max, Tuple4 t) {
		return (Quaternion) super.clampMax(max, t);
	}

	@Override
	public Quaternion clampMax(float max) {
		return (Quaternion) super.clampMax(max);
	}

	@Override
	public Quaternion clampMin(float min, Tuple4 t) {
		return (Quaternion) super.clampMin(min, t);
	}

	@Override
	public Quaternion clampMin(float min) {
		return (Quaternion) super.clampMin(min);
	}

	@Override
	public Quaternion interpolate(Tuple4 t1, float alpha) {
		return (Quaternion) super.interpolate(t1, alpha);
	}

	@Override
	public Quaternion interpolate(Tuple4 t1, Tuple4 t2, float alpha) {
		return (Quaternion) super.interpolate(t1, t2, alpha);
	}

	@Override
	public Quaternion negate() {
		return (Quaternion) super.negate();
	}

	@Override
	public Quaternion negate(Tuple4 t1) {
		return (Quaternion) super.negate(t1);
	}

	@Override
	public Quaternion scale(float s, Tuple4 t1) {
		return (Quaternion) super.scale(s, t1);
	}

	@Override
	public Quaternion scale(float s) {
		return (Quaternion) super.scale(s);
	}

	@Override
	public Quaternion scaleAdd(float s, Tuple4 t1, Tuple4 t2) {
		return (Quaternion) super.scaleAdd(s, t1, t2);
	}

	@Override
	public Quaternion scaleAdd(float s, Tuple4 t1) {
		return (Quaternion) super.scaleAdd(s, t1);
	}

	@Override
	public Quaternion set(float x, float y, float z, float w) {
		return (Quaternion) super.set(x, y, z, w);
	}

	@Override
	public Quaternion set(float[] t) {
		return (Quaternion) super.set(t);
	}

	@Override
	public Quaternion set(Tuple4 t1) {
		return (Quaternion) super.set(t1);
	}

	@Override
	public Quaternion sub(Tuple4 t1, Tuple4 t2) {
		return (Quaternion) super.sub(t1, t2);
	}

	@Override
	public Quaternion sub(Tuple4 t1) {
		return (Quaternion) super.sub(t1);
	}

	public Quaternion set(Vector3 vector3, float rad) {
		this.set(new AxisAngle(vector3, rad));
		return this;
	}

	public Vector2 rotateVector(Vector2 coordinates) {
		Vector3 v = coordinates.toVector3();
		rotateVector(v);
		coordinates.x = v.x;
		coordinates.y = v.y;
		return coordinates;
	}

	public float dot(Quaternion rotation) {
		return this.x*rotation.x+this.y*rotation.y+this.z*rotation.z+this.w*rotation.w;
	}
	
	
	public static void powerSlerp(Quaternion p, Quaternion q, double t, Quaternion qt) {
		qt.set(p);
		qt.conjugate();
		qt.mul(qt, q);
		qt.pow((float) t);
		qt.mul(p, qt);
	}

	public static void squad(Quaternion q0, Quaternion a, Quaternion b, Quaternion q1, double alpha, Quaternion qtemp1, Quaternion qtemp2, Quaternion qout) {
		powerSlerp(q0, q1, alpha, qtemp1);
		powerSlerp(a, b, alpha, qtemp2);
		powerSlerp(qtemp1, qtemp2, 2.0 * alpha * (1.0 - alpha), qout);
	}
	
	static public void computeA(Quaternion before, Quaternion value, Quaternion after, Quaternion a, Quaternion scratch1, Quaternion scratch2, Quaternion scratch3)
	{

		value.conjugate();

		scratch1.mul(value, before);
		scratch1.ln();

		scratch2.mul(value, after);
		scratch2.ln();

		scratch3.add(scratch1, scratch2);
		scratch3.scale(-0.25f);

		scratch2.exp();

		value.conjugate();
		a.mul(value, scratch3);

	}
	
	public static double distAngular(Quaternion p, Quaternion q) {
		double ct = p.dot(q);
		return BaseMath.acos(ct);
	}

	public void add(Quaternion leftRotation, float leftD) {
		this.x += leftRotation.x*leftD;
		this.y += leftRotation.y*leftD;
		this.z += leftRotation.z*leftD;
		this.w += leftRotation.w*leftD;
	}
	
	static public Quaternion blend(List<Quaternion> q, List<? extends Number> w)
	{
		if (q.size()==0) return new Quaternion();
		if (q.size()==1) return new Quaternion(q.get(0));
		if (q.size()==2)
		{
			if (w.get(0).floatValue()>=0 && w.get(0).floatValue()<=1 && w.get(1).floatValue()>=0 && w.get(1).floatValue()<=1 && Math.abs(w.get(1).floatValue()+w.get(0).floatValue()-1)<1e-3)
			{
				return new Quaternion().interpolate(q.get(0), q.get(1), w.get(1).floatValue());
			}
		}
		
		
		Quaternion a = new Quaternion().scale(0);
		float tot = 0;
		for(int i=0;i<q.size();i++)
		{
			a.add(new Quaternion(q.get(i)).ln().scale(w.get(i).floatValue()));
			tot += w.get(i).floatValue();
		}
		
		a.scale(1/tot);
		return a.exp();
	}

}
