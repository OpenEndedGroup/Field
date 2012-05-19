/*
 * $RCSfile: Matrix4f.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.3 $
 * $Date: 2005/02/18 16:28:10 $
 * $State: Exp $
 */

package field.math.linalg;

/**
 * A single precision floating point 4 by 4 matrix. Primarily to support 3D
 * rotations.
 * 
 */
public class Matrix4 implements java.io.Serializable, Cloneable {

	/*
	 * double[] tmp = new double[9]; double[] tmp_scale = new double[3];
	 * double[] tmp_rot = new double[9];
	 */
	private static final double EPS = 1.0E-8;

	// Compatible with 1.1
	static final long serialVersionUID = -8405036035410109353L;

	/**
	 * Solves a set of linear equations. The input parameters "matrix1", and
	 * "row_perm" come from luDecompostionD4x4 and do not change here. The
	 * parameter "matrix2" is a set of column vectors assembled into a 4x4
	 * matrix of floating-point values. The procedure takes each column of
	 * "matrix2" in turn and treats it as the right-hand side of the matrix
	 * equation Ax = LUx = b. The solution vector replaces the original
	 * column of the matrix.
	 * 
	 * If "matrix2" is the identity matrix, the procedure replaces its
	 * contents with the inverse of the matrix from which "matrix1" was
	 * originally derived.
	 */
	//
	// Reference: Press, Flannery, Teukolsky, Vetterling,
	// _Numerical_Recipes_in_C_, Cambridge University Press,
	// 1988, pp 44-45.
	//
	static void luBacksubstitution(double[] matrix1, int[] row_perm, double[] matrix2) {

		int i, ii, ip, j, k;
		int rp;
		int cv, rv;

		// rp = row_perm;
		rp = 0;

		// For each column vector of matrix2 ...
		for (k = 0; k < 4; k++) {
			// cv = &(matrix2[0][k]);
			cv = k;
			ii = -1;

			// Forward substitution
			for (i = 0; i < 4; i++) {
				double sum;

				ip = row_perm[rp + i];
				sum = matrix2[cv + 4 * ip];
				matrix2[cv + 4 * ip] = matrix2[cv + 4 * i];
				if (ii >= 0) {
					// rv = &(matrix1[i][0]);
					rv = i * 4;
					for (j = ii; j <= i - 1; j++) {
						sum -= matrix1[rv + j] * matrix2[cv + 4 * j];
					}
				} else if (sum != 0.0) {
					ii = i;
				}
				matrix2[cv + 4 * i] = sum;
			}

			// Backsubstitution
			// rv = &(matrix1[3][0]);
			rv = 3 * 4;
			matrix2[cv + 4 * 3] /= matrix1[rv + 3];

			rv -= 4;
			matrix2[cv + 4 * 2] = (matrix2[cv + 4 * 2] - matrix1[rv + 3] * matrix2[cv + 4 * 3]) / matrix1[rv + 2];

			rv -= 4;
			matrix2[cv + 4 * 1] = (matrix2[cv + 4 * 1] - matrix1[rv + 2] * matrix2[cv + 4 * 2] - matrix1[rv + 3] * matrix2[cv + 4 * 3]) / matrix1[rv + 1];

			rv -= 4;
			matrix2[cv + 4 * 0] = (matrix2[cv + 4 * 0] - matrix1[rv + 1] * matrix2[cv + 4 * 1] - matrix1[rv + 2] * matrix2[cv + 4 * 2] - matrix1[rv + 3] * matrix2[cv + 4 * 3]) / matrix1[rv + 0];
		}
	}

	/**
	 * Given a 4x4 array "matrix0", this function replaces it with the LU
	 * decomposition of a row-wise permutation of itself. The input
	 * parameters are "matrix0" and "dimen". The array "matrix0" is also an
	 * output parameter. The vector "row_perm[4]" is an output parameter
	 * that contains the row permutations resulting from partial pivoting.
	 * The output parameter "even_row_xchg" is 1 when the number of row
	 * exchanges is even, or -1 otherwise. Assumes data type is always
	 * double.
	 * 
	 * This function is similar to luDecomposition, except that it is tuned
	 * specifically for 4x4 matrices.
	 * 
	 * @return true if the matrix is nonsingular, or false otherwise.
	 */
	//
	// Reference: Press, Flannery, Teukolsky, Vetterling,
	// _Numerical_Recipes_in_C_, Cambridge University Press,
	// 1988, pp 40-45.
	//
	static boolean luDecomposition(double[] matrix0, int[] row_perm) {

		double row_scale[] = new double[4];

		// Determine implicit scaling information by looping over rows
		{
			int i, j;
			int ptr, rs;
			double big, temp;

			ptr = 0;
			rs = 0;

			// For each row ...
			i = 4;
			while (i-- != 0) {
				big = 0.0;

				// For each column, find the largest element in
				// the row
				j = 4;
				while (j-- != 0) {
					temp = matrix0[ptr++];
					temp = Math.abs(temp);
					if (temp > big) {
						big = temp;
					}
				}

				// Is the matrix singular?
				if (big == 0.0) {
					return false;
				}
				row_scale[rs++] = 1.0 / big;
			}
		}

		{
			int j;
			int mtx;

			mtx = 0;

			// For all columns, execute Crout's method
			for (j = 0; j < 4; j++) {
				int i, imax, k;
				int target, p1, p2;
				double sum, big, temp;

				// Determine elements of upper diagonal matrix U
				for (i = 0; i < j; i++) {
					target = mtx + (4 * i) + j;
					sum = matrix0[target];
					k = i;
					p1 = mtx + (4 * i);
					p2 = mtx + j;
					while (k-- != 0) {
						sum -= matrix0[p1] * matrix0[p2];
						p1++;
						p2 += 4;
					}
					matrix0[target] = sum;
				}

				// Search for largest pivot element and
				// calculate
				// intermediate elements of lower diagonal
				// matrix L.
				big = 0.0;
				imax = -1;
				for (i = j; i < 4; i++) {
					target = mtx + (4 * i) + j;
					sum = matrix0[target];
					k = j;
					p1 = mtx + (4 * i);
					p2 = mtx + j;
					while (k-- != 0) {
						sum -= matrix0[p1] * matrix0[p2];
						p1++;
						p2 += 4;
					}
					matrix0[target] = sum;

					// Is this the best pivot so far?
					if ((temp = row_scale[i] * Math.abs(sum)) >= big) {
						big = temp;
						imax = i;
					}
				}

				if (imax < 0) {
					throw new RuntimeException("" + imax);
				}

				// Is a row exchange necessary?
				if (j != imax) {
					// Yes: exchange rows
					k = 4;
					p1 = mtx + (4 * imax);
					p2 = mtx + (4 * j);
					while (k-- != 0) {
						temp = matrix0[p1];
						matrix0[p1++] = matrix0[p2];
						matrix0[p2++] = temp;
					}

					// Record change in scale factor
					row_scale[imax] = row_scale[j];
				}

				// Record row permutation
				row_perm[j] = imax;

				// Is the matrix singular
				if (matrix0[(mtx + (4 * j) + j)] == 0.0) {
					return false;
				}

				// Divide elements of lower diagonal matrix L by
				// pivot
				if (j != (4 - 1)) {
					temp = 1.0 / (matrix0[(mtx + (4 * j) + j)]);
					target = mtx + (4 * (j + 1)) + j;
					i = 3 - j;
					while (i-- != 0) {
						matrix0[target] *= temp;
						target += 4;
					}
				}
			}
		}

		return true;
	}

	/**
	 * The first element of the first row.
	 */
	public float m00;

	/**
	 * The second element of the first row.
	 */
	public float m01;

	/**
	 * The third element of the first row.
	 */
	public float m02;

	/**
	 * The fourth element of the first row.
	 */
	public float m03;

	/**
	 * The first element of the second row.
	 */
	public float m10;

	/**
	 * The second element of the second row.
	 */
	public float m11;

	/**
	 * The third element of the second row.
	 */
	public float m12;

	/**
	 * The fourth element of the second row.
	 */
	public float m13;

	/**
	 * The first element of the third row.
	 */
	public float m20;

	/**
	 * The second element of the third row.
	 */
	public float m21;

	/**
	 * The third element of the third row.
	 */
	public float m22;

	/**
	 * The fourth element of the third row.
	 */
	public float m23;

	/**
	 * The first element of the fourth row.
	 */
	public float m30;

	/**
	 * The second element of the fourth row.
	 */
	public float m31;

	/**
	 * The third element of the fourth row.
	 */
	public float m32;

	/**
	 * The fourth element of the fourth row.
	 */
	public float m33;

	/**
	 * Constructs and initializes a Matrix4f to all zeros.
	 */
	public Matrix4() {
		this.m00 = (float) 0.0;
		this.m01 = (float) 0.0;
		this.m02 = (float) 0.0;
		this.m03 = (float) 0.0;

		this.m10 = (float) 0.0;
		this.m11 = (float) 0.0;
		this.m12 = (float) 0.0;
		this.m13 = (float) 0.0;

		this.m20 = (float) 0.0;
		this.m21 = (float) 0.0;
		this.m22 = (float) 0.0;
		this.m23 = (float) 0.0;

		this.m30 = (float) 0.0;
		this.m31 = (float) 0.0;
		this.m32 = (float) 0.0;
		this.m33 = (float) 0.0;

	}

	/**
	 * Constructs and initializes a Matrix4f from the specified 16 values.
	 * 
	 * @param m00
	 *                the [0][0] element
	 * @param m01
	 *                the [0][1] element
	 * @param m02
	 *                the [0][2] element
	 * @param m03
	 *                the [0][3] element
	 * @param m10
	 *                the [1][0] element
	 * @param m11
	 *                the [1][1] element
	 * @param m12
	 *                the [1][2] element
	 * @param m13
	 *                the [1][3] element
	 * @param m20
	 *                the [2][0] element
	 * @param m21
	 *                the [2][1] element
	 * @param m22
	 *                the [2][2] element
	 * @param m23
	 *                the [2][3] element
	 * @param m30
	 *                the [3][0] element
	 * @param m31
	 *                the [3][1] element
	 * @param m32
	 *                the [3][2] element
	 * @param m33
	 *                the [3][3] element
	 */
	public Matrix4(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20, float m21, float m22, float m23, float m30, float m31, float m32, float m33) {
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;
		this.m03 = m03;

		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;
		this.m13 = m13;

		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;
		this.m23 = m23;

		this.m30 = m30;
		this.m31 = m31;
		this.m32 = m32;
		this.m33 = m33;

	}

	/**
	 * Constructs and initializes a Matrix4f from the specified 16 element
	 * array. this.m00 =v[0], this.m01=v[1], etc.
	 * 
	 * @param v
	 *                the array of length 16 containing in order
	 */
	public Matrix4(float[] v) {
		this.m00 = v[0];
		this.m01 = v[1];
		this.m02 = v[2];
		this.m03 = v[3];

		this.m10 = v[4];
		this.m11 = v[5];
		this.m12 = v[6];
		this.m13 = v[7];

		this.m20 = v[8];
		this.m21 = v[9];
		this.m22 = v[10];
		this.m23 = v[11];

		this.m30 = v[12];
		this.m31 = v[13];
		this.m32 = v[14];
		this.m33 = v[15];

	}

	/**
	 * Constructs and initializes a Matrix4f from the specified 16 element
	 * array. this.m00 =v[0], this.m01=v[1], etc.
	 * 
	 * @param v
	 *                the array of length 16 containing in order
	 */
	public Matrix4(double[] v) {
		this.m00 = (float) v[0];
		this.m01 = (float) v[1];
		this.m02 = (float) v[2];
		this.m03 = (float) v[3];

		this.m10 = (float) v[4];
		this.m11 = (float) v[5];
		this.m12 = (float) v[6];
		this.m13 = (float) v[7];

		this.m20 = (float) v[8];
		this.m21 = (float) v[9];
		this.m22 = (float) v[10];
		this.m23 = (float) v[11];

		this.m30 = (float) v[12];
		this.m31 = (float) v[13];
		this.m32 = (float) v[14];
		this.m33 = (float) v[15];

	}

	/**
	 * Constructs and initializes a Matrix4f from the rotation matrix,
	 * translation, and scale values; the scale is applied only to the
	 * rotational components of the matrix (upper 3x3) and not to the
	 * translational components of the matrix.
	 * 
	 * @param m1
	 *                the rotation matrix representing the rotational
	 *                components
	 * @param t1
	 *                the translational components of the matrix
	 * @param s
	 *                the scale value applied to the rotational components
	 */
	public Matrix4(Matrix3 m1, Vector3 t1, float s) {
		this.m00 = m1.m00 * s;
		this.m01 = m1.m01 * s;
		this.m02 = m1.m02 * s;
		this.m03 = t1.x;

		this.m10 = m1.m10 * s;
		this.m11 = m1.m11 * s;
		this.m12 = m1.m12 * s;
		this.m13 = t1.y;

		this.m20 = m1.m20 * s;
		this.m21 = m1.m21 * s;
		this.m22 = m1.m22 * s;
		this.m23 = t1.z;

		this.m30 = 0.0f;
		this.m31 = 0.0f;
		this.m32 = 0.0f;
		this.m33 = 1.0f;

	}

	/**
	 * Constructs a new matrix with the same values as the Matrix4f
	 * parameter.
	 * 
	 * @param m1
	 *                the source matrix
	 */
	public Matrix4(Matrix4 m1) {
		this.m00 = m1.m00;
		this.m01 = m1.m01;
		this.m02 = m1.m02;
		this.m03 = m1.m03;

		this.m10 = m1.m10;
		this.m11 = m1.m11;
		this.m12 = m1.m12;
		this.m13 = m1.m13;

		this.m20 = m1.m20;
		this.m21 = m1.m21;
		this.m22 = m1.m22;
		this.m23 = m1.m23;

		this.m30 = m1.m30;
		this.m31 = m1.m31;
		this.m32 = m1.m32;
		this.m33 = m1.m33;

	}

	/**
	 * Constructs and initializes a Matrix4f from the quaternion,
	 * translation, and scale values; the scale is applied only to the
	 * rotational components of the matrix (upper 3x3) and not to the
	 * translational components.
	 * 
	 * @param q1
	 *                the quaternion value representing the rotational
	 *                component
	 * @param t1
	 *                the translational component of the matrix
	 * @param s
	 *                the scale value applied to the rotational components
	 */
	public Matrix4(Quaternion q1, Vector3 t1, float s) {
		m00 = (float) (s * (1.0 - 2.0 * q1.y * q1.y - 2.0 * q1.z * q1.z));
		m10 = (float) (s * (2.0 * (q1.x * q1.y + q1.w * q1.z)));
		m20 = (float) (s * (2.0 * (q1.x * q1.z - q1.w * q1.y)));

		m01 = (float) (s * (2.0 * (q1.x * q1.y - q1.w * q1.z)));
		m11 = (float) (s * (1.0 - 2.0 * q1.x * q1.x - 2.0 * q1.z * q1.z));
		m21 = (float) (s * (2.0 * (q1.y * q1.z + q1.w * q1.x)));

		m02 = (float) (s * (2.0 * (q1.x * q1.z + q1.w * q1.y)));
		m12 = (float) (s * (2.0 * (q1.y * q1.z - q1.w * q1.x)));
		m22 = (float) (s * (1.0 - 2.0 * q1.x * q1.x - 2.0 * q1.y * q1.y));

		m03 = t1.x;
		m13 = t1.y;
		m23 = t1.z;

		m30 = 0.0f;
		m31 = 0.0f;
		m32 = 0.0f;
		m33 = 1.0f;

	}

	/**
	 * Constructs and initializes a Matrix4f from the quaternion,
	 * translation, and scale values; the scale is applied only to the
	 * rotational components of the matrix (upper 3x3) and not to the
	 * translational components.
	 * 
	 * @param q1
	 *                the quaternion value representing the rotational
	 *                component
	 * @param t1
	 *                the translational component of the matrix
	 * @param s
	 *                the scale value applied to the rotational components
	 */
	public Matrix4(Quaternion q1, Vector3 t1, Vector3 s) {
		m00 = (float) (s.x * (1.0 - 2.0 * q1.y * q1.y - 2.0 * q1.z * q1.z));
		m10 = (float) (s.y * (2.0 * (q1.x * q1.y + q1.w * q1.z)));
		m20 = (float) (s.z * (2.0 * (q1.x * q1.z - q1.w * q1.y)));

		m01 = (float) (s.x * (2.0 * (q1.x * q1.y - q1.w * q1.z)));
		m11 = (float) (s.y * (1.0 - 2.0 * q1.x * q1.x - 2.0 * q1.z * q1.z));
		m21 = (float) (s.z * (2.0 * (q1.y * q1.z + q1.w * q1.x)));

		m02 = (float) (s.x * (2.0 * (q1.x * q1.z + q1.w * q1.y)));
		m12 = (float) (s.y * (2.0 * (q1.y * q1.z - q1.w * q1.x)));
		m22 = (float) (s.z * (1.0 - 2.0 * q1.x * q1.x - 2.0 * q1.y * q1.y));

		m03 = t1.x;
		m13 = t1.y;
		m23 = t1.z;

		m30 = 0.0f;
		m31 = 0.0f;
		m32 = 0.0f;
		m33 = 1.0f;

	}

	/**
	 * Adds a scalar to each component of this matrix.
	 * 
	 * @param scalar
	 *                the scalar adder
	 */
	public final void add(float scalar) {
		m00 += scalar;
		m01 += scalar;
		m02 += scalar;
		m03 += scalar;
		m10 += scalar;
		m11 += scalar;
		m12 += scalar;
		m13 += scalar;
		m20 += scalar;
		m21 += scalar;
		m22 += scalar;
		m23 += scalar;
		m30 += scalar;
		m31 += scalar;
		m32 += scalar;
		m33 += scalar;
	}

	/**
	 * Adds a scalar to each component of the matrix m1 and places the
	 * result into this. Matrix m1 is not modified.
	 * 
	 * @param scalar
	 *                the scalar adder
	 * @param m1
	 *                the original matrix values
	 */
	public final void add(float scalar, Matrix4 m1) {
		this.m00 = m1.m00 + scalar;
		this.m01 = m1.m01 + scalar;
		this.m02 = m1.m02 + scalar;
		this.m03 = m1.m03 + scalar;
		this.m10 = m1.m10 + scalar;
		this.m11 = m1.m11 + scalar;
		this.m12 = m1.m12 + scalar;
		this.m13 = m1.m13 + scalar;
		this.m20 = m1.m20 + scalar;
		this.m21 = m1.m21 + scalar;
		this.m22 = m1.m22 + scalar;
		this.m23 = m1.m23 + scalar;
		this.m30 = m1.m30 + scalar;
		this.m31 = m1.m31 + scalar;
		this.m32 = m1.m32 + scalar;
		this.m33 = m1.m33 + scalar;
	}

	/**
	 * Sets the value of this matrix to the sum of itself and matrix m1.
	 * 
	 * @param m1
	 *                the other matrix
	 * @return
	 */
	public final Matrix4 add(Matrix4 m1) {
		this.m00 += m1.m00;
		this.m01 += m1.m01;
		this.m02 += m1.m02;
		this.m03 += m1.m03;

		this.m10 += m1.m10;
		this.m11 += m1.m11;
		this.m12 += m1.m12;
		this.m13 += m1.m13;

		this.m20 += m1.m20;
		this.m21 += m1.m21;
		this.m22 += m1.m22;
		this.m23 += m1.m23;

		this.m30 += m1.m30;
		this.m31 += m1.m31;
		this.m32 += m1.m32;
		this.m33 += m1.m33;
		return this;
	}

	/**
	 * Sets the value of this matrix to the matrix sum of matrices m1 and
	 * m2.
	 * 
	 * @param m1
	 *                the first matrix
	 * @param m2
	 *                the second matrix
	 */
	public final void add(Matrix4 m1, Matrix4 m2) {
		this.m00 = m1.m00 + m2.m00;
		this.m01 = m1.m01 + m2.m01;
		this.m02 = m1.m02 + m2.m02;
		this.m03 = m1.m03 + m2.m03;

		this.m10 = m1.m10 + m2.m10;
		this.m11 = m1.m11 + m2.m11;
		this.m12 = m1.m12 + m2.m12;
		this.m13 = m1.m13 + m2.m13;

		this.m20 = m1.m20 + m2.m20;
		this.m21 = m1.m21 + m2.m21;
		this.m22 = m1.m22 + m2.m22;
		this.m23 = m1.m23 + m2.m23;

		this.m30 = m1.m30 + m2.m30;
		this.m31 = m1.m31 + m2.m31;
		this.m32 = m1.m32 + m2.m32;
		this.m33 = m1.m33 + m2.m33;
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
		Matrix4 m1 = null;
		try {
			m1 = (Matrix4) super.clone();
		} catch (CloneNotSupportedException e) {
			// this shouldn't happen, since we are Cloneable
			throw new InternalError();
		}

		return m1;
	}

	/**
	 * Computes the determinate of this matrix.
	 * 
	 * @return the determinate of the matrix
	 */
	public final float determinant() {
		float det;

		// cofactor exapainsion along first row

		det = m00 * (m11 * m22 * m33 + m12 * m23 * m31 + m13 * m21 * m32 - m13 * m22 * m31 - m11 * m23 * m32 - m12 * m21 * m33);
		det -= m01 * (m10 * m22 * m33 + m12 * m23 * m30 + m13 * m20 * m32 - m13 * m22 * m30 - m10 * m23 * m32 - m12 * m20 * m33);
		det += m02 * (m10 * m21 * m33 + m11 * m23 * m30 + m13 * m20 * m31 - m13 * m21 * m30 - m10 * m23 * m31 - m11 * m20 * m33);
		det -= m03 * (m10 * m21 * m32 + m11 * m22 * m30 + m12 * m20 * m31 - m12 * m21 * m30 - m10 * m22 * m31 - m11 * m20 * m32);

		return (det);
	}

	/**
	 * Returns true if the L-infinite distance between this matrix and
	 * matrix m1 is less than or equal to the epsilon parameter, otherwise
	 * returns false. The L-infinite distance is equal to MAX[i=0,1,2,3 ;
	 * j=0,1,2,3 ; abs(this.m(i,j) - m1.m(i,j)]
	 * 
	 * @param m1
	 *                the matrix to be compared to this matrix
	 * @param epsilon
	 *                the threshold value
	 */
	public boolean epsilonEquals(Matrix4 m1, float epsilon) {

		boolean status = true;

		if (Math.abs(this.m00 - m1.m00) > epsilon)
			status = false;
		if (Math.abs(this.m01 - m1.m01) > epsilon)
			status = false;
		if (Math.abs(this.m02 - m1.m02) > epsilon)
			status = false;
		if (Math.abs(this.m03 - m1.m03) > epsilon)
			status = false;

		if (Math.abs(this.m10 - m1.m10) > epsilon)
			status = false;
		if (Math.abs(this.m11 - m1.m11) > epsilon)
			status = false;
		if (Math.abs(this.m12 - m1.m12) > epsilon)
			status = false;
		if (Math.abs(this.m13 - m1.m13) > epsilon)
			status = false;

		if (Math.abs(this.m20 - m1.m20) > epsilon)
			status = false;
		if (Math.abs(this.m21 - m1.m21) > epsilon)
			status = false;
		if (Math.abs(this.m22 - m1.m22) > epsilon)
			status = false;
		if (Math.abs(this.m23 - m1.m23) > epsilon)
			status = false;

		if (Math.abs(this.m30 - m1.m30) > epsilon)
			status = false;
		if (Math.abs(this.m31 - m1.m31) > epsilon)
			status = false;
		if (Math.abs(this.m32 - m1.m32) > epsilon)
			status = false;
		if (Math.abs(this.m33 - m1.m33) > epsilon)
			status = false;

		return (status);

	}

	/**
	 * Returns true if all of the data members of Matrix4f m1 are equal to
	 * the corresponding data members in this Matrix4f.
	 * 
	 * @param m1
	 *                the matrix with which the comparison is made.
	 * @return true or false
	 */
	public boolean equals(Matrix4 m1) {
		try {
			return (this.m00 == m1.m00 && this.m01 == m1.m01 && this.m02 == m1.m02 && this.m03 == m1.m03 && this.m10 == m1.m10 && this.m11 == m1.m11 && this.m12 == m1.m12 && this.m13 == m1.m13 && this.m20 == m1.m20 && this.m21 == m1.m21 && this.m22 == m1.m22 && this.m23 == m1.m23 && this.m30 == m1.m30 && this.m31 == m1.m31 && this.m32 == m1.m32 && this.m33 == m1.m33);
		} catch (NullPointerException e2) {
			return false;
		}

	}

	/**
	 * Returns true if the Object t1 is of type Matrix4f and all of the data
	 * members of t1 are equal to the corresponding data members in this
	 * Matrix4f.
	 * 
	 * @param t1
	 *                the matrix with which the comparison is made.
	 * @return true or false
	 */
	@Override
	public boolean equals(Object t1) {
		try {
			Matrix4 m2 = (Matrix4) t1;
			return (this.m00 == m2.m00 && this.m01 == m2.m01 && this.m02 == m2.m02 && this.m03 == m2.m03 && this.m10 == m2.m10 && this.m11 == m2.m11 && this.m12 == m2.m12 && this.m13 == m2.m13 && this.m20 == m2.m20 && this.m21 == m2.m21 && this.m22 == m2.m22 && this.m23 == m2.m23 && this.m30 == m2.m30 && this.m31 == m2.m31 && this.m32 == m2.m32 && this.m33 == m2.m33);
		} catch (ClassCastException e1) {
			return false;
		} catch (NullPointerException e2) {
			return false;
		}
	}

	public float[] get(float[] m) {
		if (m == null)
			m = new float[16];
		m[0] = m00;
		m[1] = m01;
		m[2] = m02;
		m[3] = m03;
		m[4] = m10;
		m[5] = m11;
		m[6] = m12;
		m[7] = m13;
		m[8] = m20;
		m[9] = m21;
		m[10] = m22;
		m[11] = m23;
		m[12] = m30;
		m[13] = m31;
		m[14] = m32;
		m[15] = m33;
		return m;
	}

	/**
	 * Performs an SVD normalization of this matrix in order to acquire the
	 * normalized rotational component; the values are placed into the
	 * Matrix3f parameter.
	 * 
	 * @param m1
	 *                matrix into which the rotational component is placed
	 */
	public final void get(Matrix3 m1) {
		double[] tmp_rot = new double[9]; // scratch matrix
		double[] tmp_scale = new double[3]; // scratch matrix

		getScaleRotate(tmp_scale, tmp_rot);

		m1.m00 = (float) tmp_rot[0];
		m1.m01 = (float) tmp_rot[1];
		m1.m02 = (float) tmp_rot[2];

		m1.m10 = (float) tmp_rot[3];
		m1.m11 = (float) tmp_rot[4];
		m1.m12 = (float) tmp_rot[5];

		m1.m20 = (float) tmp_rot[6];
		m1.m21 = (float) tmp_rot[7];
		m1.m22 = (float) tmp_rot[8];

	}

	/**
	 * Performs an SVD normalization of this matrix to calculate the
	 * rotation as a 3x3 matrix, the translation, and the scale. None of the
	 * matrix values are modified.
	 * 
	 * @param m1
	 *                the normalized matrix representing the rotation
	 * @param t1
	 *                the translation component
	 * @return the scale component of this transform
	 */
	public final float get(Matrix3 m1, Vector3 t1) {
		double[] tmp_rot = new double[9]; // scratch matrix
		double[] tmp_scale = new double[3]; // scratch matrix

		getScaleRotate(tmp_scale, tmp_rot);

		m1.m00 = (float) tmp_rot[0];
		m1.m01 = (float) tmp_rot[1];
		m1.m02 = (float) tmp_rot[2];

		m1.m10 = (float) tmp_rot[3];
		m1.m11 = (float) tmp_rot[4];
		m1.m12 = (float) tmp_rot[5];

		m1.m20 = (float) tmp_rot[6];
		m1.m21 = (float) tmp_rot[7];
		m1.m22 = (float) tmp_rot[8];

		t1.x = m03;
		t1.y = m13;
		t1.z = m23;

		return ((float) Matrix3.max3(tmp_scale));

	}

	/**
	 * Performs an SVD normalization of this matrix in order to acquire the
	 * normalized rotational component; the values are placed into the
	 * Quat4f parameter.
	 * 
	 * @param q1
	 *                quaternion into which the rotation component is placed
	 * @return
	 */
	public final Quaternion get(Quaternion q1) {
		if (q1 == null)
			q1 = new Quaternion();
		double[] tmp_rot = new double[9]; // scratch matrix
		double[] tmp_scale = new double[3]; // scratch matrix
		getScaleRotate(tmp_scale, tmp_rot);

		double ww;

		ww = 0.25 * (1.0 + tmp_rot[0] + tmp_rot[4] + tmp_rot[8]);
		if (ww < 0)
			ww = 0;

		if (!((ww < 0 ? -ww : ww) < 1.0e-7)) {
			double w = (float) Math.sqrt(ww);
			ww = 0.25 / w;
			double x = (float) ((tmp_rot[7] - tmp_rot[5]) * ww);
			double y = (float) ((tmp_rot[2] - tmp_rot[6]) * ww);
			double z = (float) ((tmp_rot[3] - tmp_rot[1]) * ww);

			q1.x = (float) x;
			q1.y = (float) y;
			q1.z = (float) z;
			q1.w = (float) w;
			return q1;
		}

		q1.w = 0.0f;
		ww = -0.5 * (tmp_rot[4] + tmp_rot[8]);
		if (ww < 0)
			ww = 0;
		if (!((ww < 0 ? -ww : ww) < 1.0e-7)) {
			q1.x = (float) Math.sqrt(ww);
			ww = 0.5 / q1.x;
			q1.y = (float) (tmp_rot[3] * ww);
			q1.z = (float) (tmp_rot[6] * ww);
			return q1;
		}

		q1.x = 0.0f;
		ww = 0.5 * (1.0 - tmp_rot[8]);
		if (ww < 0)
			ww = 0;
		if (!((ww < 0 ? -ww : ww) < 1.0e-7)) {
			q1.y = (float) (Math.sqrt(ww));
			q1.z = (float) (tmp_rot[7] / (2.0 * q1.y));
			return q1;
		}

		q1.y = 0.0f;
		q1.z = 1.0f;
		return q1;
	}

	public void get(Quaternion q1, Vector3 translation, Vector3 scale) {

		double[] tmp_rot = new double[9]; // scratch matrix
		double[] tmp_scale = new double[3]; // scratch matrix
		getScaleRotate(tmp_scale, tmp_rot);

		scale.set(tmp_scale);

		// !!
		translation.x = m03;
		translation.y = m13;
		translation.z = m23;

		double ww;

		ww = 0.25 * (1.0 + tmp_rot[0] + tmp_rot[4] + tmp_rot[8]);
		if (!((ww < 0 ? -ww : ww) < 1.0e-30)) {
			q1.w = (float) Math.sqrt(Math.abs(ww));
			ww = 0.25 / q1.w;
			q1.x = (float) ((tmp_rot[7] - tmp_rot[5]) * ww);
			q1.y = (float) ((tmp_rot[2] - tmp_rot[6]) * ww);
			q1.z = (float) ((tmp_rot[3] - tmp_rot[1]) * ww);

			q1.normalize();
			return;
		}

		q1.w = 0.0f;
		ww = -0.5 * (tmp_rot[4] + tmp_rot[8]);
		if (!((ww < 0 ? -ww : ww) < 1.0e-30)) {
			q1.x = (float) Math.sqrt(Math.abs(ww));
			ww = 0.5 / q1.x;
			q1.y = (float) (tmp_rot[3] * ww);
			q1.z = (float) (tmp_rot[6] * ww);

			q1.normalize();
			return;
		}

		q1.x = 0.0f;
		ww = 0.5 * (1.0 - tmp_rot[8]);
		if (!((ww < 0 ? -ww : ww) < 1.0e-30)) {
			q1.y = (float) (Math.sqrt(Math.abs(ww)));
			q1.z = (float) (tmp_rot[7] / (2.0 * q1.y));

			q1.normalize();
			return;
		}

		q1.y = 0.0f;
		q1.z = 1.0f;

	}

	/**
	 * Retrieves the translational components of this matrix.
	 * 
	 * @param trans
	 *                the vector that will receive the translational
	 *                component
	 */
	public final void get(Vector3 trans) {
		// !!
		trans.x = m03;
		trans.y = m13;
		trans.z = m23;
		// trans.x = m30;
		// trans.y = m31;
		// trans.z = m32;
	}

	/**
	 * Copies the matrix values in the specified column into the array
	 * parameter.
	 * 
	 * @param column
	 *                the matrix column
	 * @param v
	 *                the array into which the matrix row values will be
	 *                copied
	 */
	public final void getColumn(int column, float v[]) {
		if (column == 0) {
			v[0] = m00;
			v[1] = m10;
			v[2] = m20;
			v[3] = m30;
		} else if (column == 1) {
			v[0] = m01;
			v[1] = m11;
			v[2] = m21;
			v[3] = m31;
		} else if (column == 2) {
			v[0] = m02;
			v[1] = m12;
			v[2] = m22;
			v[3] = m32;
		} else if (column == 3) {
			v[0] = m03;
			v[1] = m13;
			v[2] = m23;
			v[3] = m33;
		} else {
			throw new ArrayIndexOutOfBoundsException(column);
		}

	}

	/**
	 * Copies the matrix values in the specified column into the vector
	 * parameter.
	 * 
	 * @param column
	 *                the matrix column
	 * @param v
	 *                the vector into which the matrix row values will be
	 *                copied
	 */
	public final void getColumn(int column, Vector4 v) {
		if (column == 0) {
			v.x = m00;
			v.y = m10;
			v.z = m20;
			v.w = m30;
		} else if (column == 1) {
			v.x = m01;
			v.y = m11;
			v.z = m21;
			v.w = m31;
		} else if (column == 2) {
			v.x = m02;
			v.y = m12;
			v.z = m22;
			v.w = m32;
		} else if (column == 3) {
			v.x = m03;
			v.y = m13;
			v.z = m23;
			v.w = m33;
		} else {
			throw new ArrayIndexOutOfBoundsException(column);
		}

	}

	public float[] getColumnMajor(float[] m) {
		if (m == null)
			m = new float[16];
		m[0] = m00;
		m[1] = m10;
		m[2] = m20;
		m[3] = m30;
		m[4] = m01;
		m[5] = m11;
		m[6] = m21;
		m[7] = m31;
		m[8] = m02;
		m[9] = m12;
		m[10] = m22;
		m[11] = m32;
		m[12] = m03;
		m[13] = m13;
		m[14] = m23;
		m[15] = m33;
		return m;
	}

	/**
	 * Retrieves the value at the specified row and column of this matrix.
	 * 
	 * @param row
	 *                the row number to be retrieved (zero indexed)
	 * @param column
	 *                the column number to be retrieved (zero indexed)
	 * @return the value at the indexed element
	 */
	public final float getElement(int row, int column) {
		switch (row) {
		case 0:
			switch (column) {
			case 0:
				return (this.m00);
			case 1:
				return (this.m01);
			case 2:
				return (this.m02);
			case 3:
				return (this.m03);
			default:
				break;
			}
			break;
		case 1:
			switch (column) {
			case 0:
				return (this.m10);
			case 1:
				return (this.m11);
			case 2:
				return (this.m12);
			case 3:
				return (this.m13);
			default:
				break;
			}
			break;

		case 2:
			switch (column) {
			case 0:
				return (this.m20);
			case 1:
				return (this.m21);
			case 2:
				return (this.m22);
			case 3:
				return (this.m23);
			default:
				break;
			}
			break;

		case 3:
			switch (column) {
			case 0:
				return (this.m30);
			case 1:
				return (this.m31);
			case 2:
				return (this.m32);
			case 3:
				return (this.m33);
			default:
				break;
			}
			break;

		default:
			break;
		}
		throw new ArrayIndexOutOfBoundsException(column + " " + row);
	}

	/**
	 * Gets the upper 3x3 values of this matrix and places them into the
	 * matrix m1.
	 * 
	 * @param m1
	 *                the matrix that will hold the values
	 */
	public final void getRotationScale(Matrix3 m1) {
		m1.m00 = m00;
		m1.m01 = m01;
		m1.m02 = m02;
		m1.m10 = m10;
		m1.m11 = m11;
		m1.m12 = m12;
		m1.m20 = m20;
		m1.m21 = m21;
		m1.m22 = m22;
	}

	/**
	 * Copies the matrix values in the specified row into the array
	 * parameter.
	 * 
	 * @param row
	 *                the matrix row
	 * @param v
	 *                the array into which the matrix row values will be
	 *                copied
	 */
	public final void getRow(int row, float v[]) {
		if (row == 0) {
			v[0] = m00;
			v[1] = m01;
			v[2] = m02;
			v[3] = m03;
		} else if (row == 1) {
			v[0] = m10;
			v[1] = m11;
			v[2] = m12;
			v[3] = m13;
		} else if (row == 2) {
			v[0] = m20;
			v[1] = m21;
			v[2] = m22;
			v[3] = m23;
		} else if (row == 3) {
			v[0] = m30;
			v[1] = m31;
			v[2] = m32;
			v[3] = m33;
		} else {
			throw new ArrayIndexOutOfBoundsException(row);
		}

	}

	/**
	 * Copies the matrix values in the specified row into the vector
	 * parameter.
	 * 
	 * @param row
	 *                the matrix row
	 * @param v
	 *                the vector into which the matrix row values will be
	 *                copied
	 */
	public final void getRow(int row, Vector4 v) {
		if (row == 0) {
			v.x = m00;
			v.y = m01;
			v.z = m02;
			v.w = m03;
		} else if (row == 1) {
			v.x = m10;
			v.y = m11;
			v.z = m12;
			v.w = m13;
		} else if (row == 2) {
			v.x = m20;
			v.y = m21;
			v.z = m22;
			v.w = m23;
		} else if (row == 3) {
			v.x = m30;
			v.y = m31;
			v.z = m32;
			v.w = m33;
		} else {
			throw new ArrayIndexOutOfBoundsException(row);
		}

	}

	/**
	 * Performs an SVD normalization of this matrix to calculate and return
	 * the uniform scale factor. If the matrix has non-uniform scale
	 * factors, the largest of the x, y, and z scale factors will be
	 * returned. This matrix is not modified.
	 * 
	 * @return the scale factor of this matrix
	 */
	public final float getScale() {
		double[] tmp_rot = new double[9]; // scratch matrix
		double[] tmp_scale = new double[3]; // scratch matrix

		getScaleRotate(tmp_scale, tmp_rot);

		return ((float) Matrix3.max3(tmp_scale));
	}

	public final Vector3 getScale(Vector3 scale) {
		if (scale == null)
			scale = new Vector3();
		double[] tmp_rot = new double[9]; // scratch matrix
		double[] tmp_scale = new double[3]; // scratch matrix

		getScaleRotate(tmp_scale, tmp_rot);

		scale.set(tmp_scale);
		return scale;
	}

	/**
	 * Returns a hash code value based on the data values in this object.
	 * Two different Matrix4f objects with identical data values (i.e.,
	 * Matrix4f.equals returns true) will return the same hash code value.
	 * Two objects with different data members may return the same hash
	 * value, although this is not likely.
	 * 
	 * @return the integer hash code value
	 */
	@Override
	public int hashCode() {
		long bits = 1L;
		bits = 31L * bits + Float.floatToIntBits(m00);
		bits = 31L * bits + Float.floatToIntBits(m01);
		bits = 31L * bits + Float.floatToIntBits(m02);
		bits = 31L * bits + Float.floatToIntBits(m03);
		bits = 31L * bits + Float.floatToIntBits(m10);
		bits = 31L * bits + Float.floatToIntBits(m11);
		bits = 31L * bits + Float.floatToIntBits(m12);
		bits = 31L * bits + Float.floatToIntBits(m13);
		bits = 31L * bits + Float.floatToIntBits(m20);
		bits = 31L * bits + Float.floatToIntBits(m21);
		bits = 31L * bits + Float.floatToIntBits(m22);
		bits = 31L * bits + Float.floatToIntBits(m23);
		bits = 31L * bits + Float.floatToIntBits(m30);
		bits = 31L * bits + Float.floatToIntBits(m31);
		bits = 31L * bits + Float.floatToIntBits(m32);
		bits = 31L * bits + Float.floatToIntBits(m33);
		return (int) (bits ^ (bits >> 32));
	}

	/**
	 * Inverts this matrix in place.
	 */
	public final void invert() {
		invertGeneral(this);
	}

	/**
	 * Sets the value of this matrix to the matrix inverse of the passed
	 * (user declared) matrix m1.
	 * 
	 * @param m1
	 *                the matrix to be inverted
	 */
	public final void invert(Matrix4 m1) {

		invertGeneral(m1);
	}

	public Matrix4 madd(float w, Matrix4 m) {
		this.m00 += w * m.m00;
		this.m01 += w * m.m01;
		this.m02 += w * m.m02;
		this.m03 += w * m.m03;
		this.m10 += w * m.m10;
		this.m11 += w * m.m11;
		this.m12 += w * m.m12;
		this.m13 += w * m.m13;
		this.m20 += w * m.m20;
		this.m21 += w * m.m21;
		this.m22 += w * m.m22;
		this.m23 += w * m.m23;
		this.m30 += w * m.m30;
		this.m31 += w * m.m31;
		this.m32 += w * m.m32;
		this.m33 += w * m.m33;
		return this;
	}

	/**
	 * Multiplies each element of this matrix by a scalar.
	 * 
	 * @param scalar
	 *                the scalar multiplier.
	 * @return
	 */
	public final Matrix4 mul(float scalar) {
		m00 *= scalar;
		m01 *= scalar;
		m02 *= scalar;
		m03 *= scalar;
		m10 *= scalar;
		m11 *= scalar;
		m12 *= scalar;
		m13 *= scalar;
		m20 *= scalar;
		m21 *= scalar;
		m22 *= scalar;
		m23 *= scalar;
		m30 *= scalar;
		m31 *= scalar;
		m32 *= scalar;
		m33 *= scalar;
		return this;
	}

	/**
	 * Multiplies each element of matrix m1 by a scalar and places the
	 * result into this. Matrix m1 is not modified.
	 * 
	 * @param scalar
	 *                the scalar multiplier.
	 * @param m1
	 *                the original matrix.
	 */
	public final void mul(float scalar, Matrix4 m1) {
		this.m00 = m1.m00 * scalar;
		this.m01 = m1.m01 * scalar;
		this.m02 = m1.m02 * scalar;
		this.m03 = m1.m03 * scalar;
		this.m10 = m1.m10 * scalar;
		this.m11 = m1.m11 * scalar;
		this.m12 = m1.m12 * scalar;
		this.m13 = m1.m13 * scalar;
		this.m20 = m1.m20 * scalar;
		this.m21 = m1.m21 * scalar;
		this.m22 = m1.m22 * scalar;
		this.m23 = m1.m23 * scalar;
		this.m30 = m1.m30 * scalar;
		this.m31 = m1.m31 * scalar;
		this.m32 = m1.m32 * scalar;
		this.m33 = m1.m33 * scalar;
	}

	/**
	 * Sets the value of this matrix to the result of multiplying itself
	 * with matrix m1.
	 * 
	 * @param m1
	 *                the other matrix
	 */
	public final void mul(Matrix4 m1) {
		float m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33; // vars
													// for
													// temp
													// result
													// matrix

		m00 = this.m00 * m1.m00 + this.m01 * m1.m10 + this.m02 * m1.m20 + this.m03 * m1.m30;
		m01 = this.m00 * m1.m01 + this.m01 * m1.m11 + this.m02 * m1.m21 + this.m03 * m1.m31;
		m02 = this.m00 * m1.m02 + this.m01 * m1.m12 + this.m02 * m1.m22 + this.m03 * m1.m32;
		m03 = this.m00 * m1.m03 + this.m01 * m1.m13 + this.m02 * m1.m23 + this.m03 * m1.m33;

		m10 = this.m10 * m1.m00 + this.m11 * m1.m10 + this.m12 * m1.m20 + this.m13 * m1.m30;
		m11 = this.m10 * m1.m01 + this.m11 * m1.m11 + this.m12 * m1.m21 + this.m13 * m1.m31;
		m12 = this.m10 * m1.m02 + this.m11 * m1.m12 + this.m12 * m1.m22 + this.m13 * m1.m32;
		m13 = this.m10 * m1.m03 + this.m11 * m1.m13 + this.m12 * m1.m23 + this.m13 * m1.m33;

		m20 = this.m20 * m1.m00 + this.m21 * m1.m10 + this.m22 * m1.m20 + this.m23 * m1.m30;
		m21 = this.m20 * m1.m01 + this.m21 * m1.m11 + this.m22 * m1.m21 + this.m23 * m1.m31;
		m22 = this.m20 * m1.m02 + this.m21 * m1.m12 + this.m22 * m1.m22 + this.m23 * m1.m32;
		m23 = this.m20 * m1.m03 + this.m21 * m1.m13 + this.m22 * m1.m23 + this.m23 * m1.m33;

		m30 = this.m30 * m1.m00 + this.m31 * m1.m10 + this.m32 * m1.m20 + this.m33 * m1.m30;
		m31 = this.m30 * m1.m01 + this.m31 * m1.m11 + this.m32 * m1.m21 + this.m33 * m1.m31;
		m32 = this.m30 * m1.m02 + this.m31 * m1.m12 + this.m32 * m1.m22 + this.m33 * m1.m32;
		m33 = this.m30 * m1.m03 + this.m31 * m1.m13 + this.m32 * m1.m23 + this.m33 * m1.m33;

		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;
		this.m03 = m03;
		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;
		this.m13 = m13;
		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;
		this.m23 = m23;
		this.m30 = m30;
		this.m31 = m31;
		this.m32 = m32;
		this.m33 = m33;
	}

	/**
	 * Sets the value of this matrix to the result of multiplying the two
	 * argument matrices together.
	 * 
	 * @param m1
	 *                the first matrix
	 * @param m2
	 *                the second matrix
	 */
	public final Matrix4 mul(Matrix4 m1, Matrix4 m2) {
		if (this != m1 && this != m2) {

			this.m00 = m1.m00 * m2.m00 + m1.m01 * m2.m10 + m1.m02 * m2.m20 + m1.m03 * m2.m30;
			this.m01 = m1.m00 * m2.m01 + m1.m01 * m2.m11 + m1.m02 * m2.m21 + m1.m03 * m2.m31;
			this.m02 = m1.m00 * m2.m02 + m1.m01 * m2.m12 + m1.m02 * m2.m22 + m1.m03 * m2.m32;
			this.m03 = m1.m00 * m2.m03 + m1.m01 * m2.m13 + m1.m02 * m2.m23 + m1.m03 * m2.m33;

			this.m10 = m1.m10 * m2.m00 + m1.m11 * m2.m10 + m1.m12 * m2.m20 + m1.m13 * m2.m30;
			this.m11 = m1.m10 * m2.m01 + m1.m11 * m2.m11 + m1.m12 * m2.m21 + m1.m13 * m2.m31;
			this.m12 = m1.m10 * m2.m02 + m1.m11 * m2.m12 + m1.m12 * m2.m22 + m1.m13 * m2.m32;
			this.m13 = m1.m10 * m2.m03 + m1.m11 * m2.m13 + m1.m12 * m2.m23 + m1.m13 * m2.m33;

			this.m20 = m1.m20 * m2.m00 + m1.m21 * m2.m10 + m1.m22 * m2.m20 + m1.m23 * m2.m30;
			this.m21 = m1.m20 * m2.m01 + m1.m21 * m2.m11 + m1.m22 * m2.m21 + m1.m23 * m2.m31;
			this.m22 = m1.m20 * m2.m02 + m1.m21 * m2.m12 + m1.m22 * m2.m22 + m1.m23 * m2.m32;
			this.m23 = m1.m20 * m2.m03 + m1.m21 * m2.m13 + m1.m22 * m2.m23 + m1.m23 * m2.m33;

			this.m30 = m1.m30 * m2.m00 + m1.m31 * m2.m10 + m1.m32 * m2.m20 + m1.m33 * m2.m30;
			this.m31 = m1.m30 * m2.m01 + m1.m31 * m2.m11 + m1.m32 * m2.m21 + m1.m33 * m2.m31;
			this.m32 = m1.m30 * m2.m02 + m1.m31 * m2.m12 + m1.m32 * m2.m22 + m1.m33 * m2.m32;
			this.m33 = m1.m30 * m2.m03 + m1.m31 * m2.m13 + m1.m32 * m2.m23 + m1.m33 * m2.m33;
		} else {
			float m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33; // vars
														// for
														// temp
														// result
														// matrix
			m00 = m1.m00 * m2.m00 + m1.m01 * m2.m10 + m1.m02 * m2.m20 + m1.m03 * m2.m30;
			m01 = m1.m00 * m2.m01 + m1.m01 * m2.m11 + m1.m02 * m2.m21 + m1.m03 * m2.m31;
			m02 = m1.m00 * m2.m02 + m1.m01 * m2.m12 + m1.m02 * m2.m22 + m1.m03 * m2.m32;
			m03 = m1.m00 * m2.m03 + m1.m01 * m2.m13 + m1.m02 * m2.m23 + m1.m03 * m2.m33;

			m10 = m1.m10 * m2.m00 + m1.m11 * m2.m10 + m1.m12 * m2.m20 + m1.m13 * m2.m30;
			m11 = m1.m10 * m2.m01 + m1.m11 * m2.m11 + m1.m12 * m2.m21 + m1.m13 * m2.m31;
			m12 = m1.m10 * m2.m02 + m1.m11 * m2.m12 + m1.m12 * m2.m22 + m1.m13 * m2.m32;
			m13 = m1.m10 * m2.m03 + m1.m11 * m2.m13 + m1.m12 * m2.m23 + m1.m13 * m2.m33;

			m20 = m1.m20 * m2.m00 + m1.m21 * m2.m10 + m1.m22 * m2.m20 + m1.m23 * m2.m30;
			m21 = m1.m20 * m2.m01 + m1.m21 * m2.m11 + m1.m22 * m2.m21 + m1.m23 * m2.m31;
			m22 = m1.m20 * m2.m02 + m1.m21 * m2.m12 + m1.m22 * m2.m22 + m1.m23 * m2.m32;
			m23 = m1.m20 * m2.m03 + m1.m21 * m2.m13 + m1.m22 * m2.m23 + m1.m23 * m2.m33;

			m30 = m1.m30 * m2.m00 + m1.m31 * m2.m10 + m1.m32 * m2.m20 + m1.m33 * m2.m30;
			m31 = m1.m30 * m2.m01 + m1.m31 * m2.m11 + m1.m32 * m2.m21 + m1.m33 * m2.m31;
			m32 = m1.m30 * m2.m02 + m1.m31 * m2.m12 + m1.m32 * m2.m22 + m1.m33 * m2.m32;
			m33 = m1.m30 * m2.m03 + m1.m31 * m2.m13 + m1.m32 * m2.m23 + m1.m33 * m2.m33;

			this.m00 = m00;
			this.m01 = m01;
			this.m02 = m02;
			this.m03 = m03;
			this.m10 = m10;
			this.m11 = m11;
			this.m12 = m12;
			this.m13 = m13;
			this.m20 = m20;
			this.m21 = m21;
			this.m22 = m22;
			this.m23 = m23;
			this.m30 = m30;
			this.m31 = m31;
			this.m32 = m32;
			this.m33 = m33;
		}
		return this;
	}

	/**
	 * Multiplies the transpose of matrix m1 times the transpose of matrix
	 * m2, and places the result into this.
	 * 
	 * @param m1
	 *                the matrix on the left hand side of the multiplication
	 * @param m2
	 *                the matrix on the right hand side of the
	 *                multiplication
	 */
	public final Matrix4 mulTransposeBoth(Matrix4 m1, Matrix4 m2) {
		if (this != m1 && this != m2) {
			this.m00 = m1.m00 * m2.m00 + m1.m10 * m2.m01 + m1.m20 * m2.m02 + m1.m30 * m2.m03;
			this.m01 = m1.m00 * m2.m10 + m1.m10 * m2.m11 + m1.m20 * m2.m12 + m1.m30 * m2.m13;
			this.m02 = m1.m00 * m2.m20 + m1.m10 * m2.m21 + m1.m20 * m2.m22 + m1.m30 * m2.m23;
			this.m03 = m1.m00 * m2.m30 + m1.m10 * m2.m31 + m1.m20 * m2.m32 + m1.m30 * m2.m33;

			this.m10 = m1.m01 * m2.m00 + m1.m11 * m2.m01 + m1.m21 * m2.m02 + m1.m31 * m2.m03;
			this.m11 = m1.m01 * m2.m10 + m1.m11 * m2.m11 + m1.m21 * m2.m12 + m1.m31 * m2.m13;
			this.m12 = m1.m01 * m2.m20 + m1.m11 * m2.m21 + m1.m21 * m2.m22 + m1.m31 * m2.m23;
			this.m13 = m1.m01 * m2.m30 + m1.m11 * m2.m31 + m1.m21 * m2.m32 + m1.m31 * m2.m33;

			this.m20 = m1.m02 * m2.m00 + m1.m12 * m2.m01 + m1.m22 * m2.m02 + m1.m32 * m2.m03;
			this.m21 = m1.m02 * m2.m10 + m1.m12 * m2.m11 + m1.m22 * m2.m12 + m1.m32 * m2.m13;
			this.m22 = m1.m02 * m2.m20 + m1.m12 * m2.m21 + m1.m22 * m2.m22 + m1.m32 * m2.m23;
			this.m23 = m1.m02 * m2.m30 + m1.m12 * m2.m31 + m1.m22 * m2.m32 + m1.m32 * m2.m33;

			this.m30 = m1.m03 * m2.m00 + m1.m13 * m2.m01 + m1.m23 * m2.m02 + m1.m33 * m2.m03;
			this.m31 = m1.m03 * m2.m10 + m1.m13 * m2.m11 + m1.m23 * m2.m12 + m1.m33 * m2.m13;
			this.m32 = m1.m03 * m2.m20 + m1.m13 * m2.m21 + m1.m23 * m2.m22 + m1.m33 * m2.m23;
			this.m33 = m1.m03 * m2.m30 + m1.m13 * m2.m31 + m1.m23 * m2.m32 + m1.m33 * m2.m33;
		} else {
			float m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, // vars
												// for
												// temp
												// result
												// matrix
			m30, m31, m32, m33;

			m00 = m1.m00 * m2.m00 + m1.m10 * m2.m01 + m1.m20 * m2.m02 + m1.m30 * m2.m03;
			m01 = m1.m00 * m2.m10 + m1.m10 * m2.m11 + m1.m20 * m2.m12 + m1.m30 * m2.m13;
			m02 = m1.m00 * m2.m20 + m1.m10 * m2.m21 + m1.m20 * m2.m22 + m1.m30 * m2.m23;
			m03 = m1.m00 * m2.m30 + m1.m10 * m2.m31 + m1.m20 * m2.m32 + m1.m30 * m2.m33;

			m10 = m1.m01 * m2.m00 + m1.m11 * m2.m01 + m1.m21 * m2.m02 + m1.m31 * m2.m03;
			m11 = m1.m01 * m2.m10 + m1.m11 * m2.m11 + m1.m21 * m2.m12 + m1.m31 * m2.m13;
			m12 = m1.m01 * m2.m20 + m1.m11 * m2.m21 + m1.m21 * m2.m22 + m1.m31 * m2.m23;
			m13 = m1.m01 * m2.m30 + m1.m11 * m2.m31 + m1.m21 * m2.m32 + m1.m31 * m2.m33;

			m20 = m1.m02 * m2.m00 + m1.m12 * m2.m01 + m1.m22 * m2.m02 + m1.m32 * m2.m03;
			m21 = m1.m02 * m2.m10 + m1.m12 * m2.m11 + m1.m22 * m2.m12 + m1.m32 * m2.m13;
			m22 = m1.m02 * m2.m20 + m1.m12 * m2.m21 + m1.m22 * m2.m22 + m1.m32 * m2.m23;
			m23 = m1.m02 * m2.m30 + m1.m12 * m2.m31 + m1.m22 * m2.m32 + m1.m32 * m2.m33;

			m30 = m1.m03 * m2.m00 + m1.m13 * m2.m01 + m1.m23 * m2.m02 + m1.m33 * m2.m03;
			m31 = m1.m03 * m2.m10 + m1.m13 * m2.m11 + m1.m23 * m2.m12 + m1.m33 * m2.m13;
			m32 = m1.m03 * m2.m20 + m1.m13 * m2.m21 + m1.m23 * m2.m22 + m1.m33 * m2.m23;
			m33 = m1.m03 * m2.m30 + m1.m13 * m2.m31 + m1.m23 * m2.m32 + m1.m33 * m2.m33;

			this.m00 = m00;
			this.m01 = m01;
			this.m02 = m02;
			this.m03 = m03;
			this.m10 = m10;
			this.m11 = m11;
			this.m12 = m12;
			this.m13 = m13;
			this.m20 = m20;
			this.m21 = m21;
			this.m22 = m22;
			this.m23 = m23;
			this.m30 = m30;
			this.m31 = m31;
			this.m32 = m32;
			this.m33 = m33;
		}
		return this;

	}

	/**
	 * Multiplies the transpose of matrix m1 times matrix m2, and places the
	 * result into this.
	 * 
	 * @param m1
	 *                the matrix on the left hand side of the multiplication
	 * @param m2
	 *                the matrix on the right hand side of the
	 *                multiplication
	 */
	public final Matrix4 mulTransposeLeft(Matrix4 m1, Matrix4 m2) {
		if (this != m1 && this != m2) {
			this.m00 = m1.m00 * m2.m00 + m1.m10 * m2.m10 + m1.m20 * m2.m20 + m1.m30 * m2.m30;
			this.m01 = m1.m00 * m2.m01 + m1.m10 * m2.m11 + m1.m20 * m2.m21 + m1.m30 * m2.m31;
			this.m02 = m1.m00 * m2.m02 + m1.m10 * m2.m12 + m1.m20 * m2.m22 + m1.m30 * m2.m32;
			this.m03 = m1.m00 * m2.m03 + m1.m10 * m2.m13 + m1.m20 * m2.m23 + m1.m30 * m2.m33;

			this.m10 = m1.m01 * m2.m00 + m1.m11 * m2.m10 + m1.m21 * m2.m20 + m1.m31 * m2.m30;
			this.m11 = m1.m01 * m2.m01 + m1.m11 * m2.m11 + m1.m21 * m2.m21 + m1.m31 * m2.m31;
			this.m12 = m1.m01 * m2.m02 + m1.m11 * m2.m12 + m1.m21 * m2.m22 + m1.m31 * m2.m32;
			this.m13 = m1.m01 * m2.m03 + m1.m11 * m2.m13 + m1.m21 * m2.m23 + m1.m31 * m2.m33;

			this.m20 = m1.m02 * m2.m00 + m1.m12 * m2.m10 + m1.m22 * m2.m20 + m1.m32 * m2.m30;
			this.m21 = m1.m02 * m2.m01 + m1.m12 * m2.m11 + m1.m22 * m2.m21 + m1.m32 * m2.m31;
			this.m22 = m1.m02 * m2.m02 + m1.m12 * m2.m12 + m1.m22 * m2.m22 + m1.m32 * m2.m32;
			this.m23 = m1.m02 * m2.m03 + m1.m12 * m2.m13 + m1.m22 * m2.m23 + m1.m32 * m2.m33;

			this.m30 = m1.m03 * m2.m00 + m1.m13 * m2.m10 + m1.m23 * m2.m20 + m1.m33 * m2.m30;
			this.m31 = m1.m03 * m2.m01 + m1.m13 * m2.m11 + m1.m23 * m2.m21 + m1.m33 * m2.m31;
			this.m32 = m1.m03 * m2.m02 + m1.m13 * m2.m12 + m1.m23 * m2.m22 + m1.m33 * m2.m32;
			this.m33 = m1.m03 * m2.m03 + m1.m13 * m2.m13 + m1.m23 * m2.m23 + m1.m33 * m2.m33;
		} else {
			float m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, // vars
												// for
												// temp
												// result
												// matrix
			m30, m31, m32, m33;

			m00 = m1.m00 * m2.m00 + m1.m10 * m2.m10 + m1.m20 * m2.m20 + m1.m30 * m2.m30;
			m01 = m1.m00 * m2.m01 + m1.m10 * m2.m11 + m1.m20 * m2.m21 + m1.m30 * m2.m31;
			m02 = m1.m00 * m2.m02 + m1.m10 * m2.m12 + m1.m20 * m2.m22 + m1.m30 * m2.m32;
			m03 = m1.m00 * m2.m03 + m1.m10 * m2.m13 + m1.m20 * m2.m23 + m1.m30 * m2.m33;

			m10 = m1.m01 * m2.m00 + m1.m11 * m2.m10 + m1.m21 * m2.m20 + m1.m31 * m2.m30;
			m11 = m1.m01 * m2.m01 + m1.m11 * m2.m11 + m1.m21 * m2.m21 + m1.m31 * m2.m31;
			m12 = m1.m01 * m2.m02 + m1.m11 * m2.m12 + m1.m21 * m2.m22 + m1.m31 * m2.m32;
			m13 = m1.m01 * m2.m03 + m1.m11 * m2.m13 + m1.m21 * m2.m23 + m1.m31 * m2.m33;

			m20 = m1.m02 * m2.m00 + m1.m12 * m2.m10 + m1.m22 * m2.m20 + m1.m32 * m2.m30;
			m21 = m1.m02 * m2.m01 + m1.m12 * m2.m11 + m1.m22 * m2.m21 + m1.m32 * m2.m31;
			m22 = m1.m02 * m2.m02 + m1.m12 * m2.m12 + m1.m22 * m2.m22 + m1.m32 * m2.m32;
			m23 = m1.m02 * m2.m03 + m1.m12 * m2.m13 + m1.m22 * m2.m23 + m1.m32 * m2.m33;

			m30 = m1.m03 * m2.m00 + m1.m13 * m2.m10 + m1.m23 * m2.m20 + m1.m33 * m2.m30;
			m31 = m1.m03 * m2.m01 + m1.m13 * m2.m11 + m1.m23 * m2.m21 + m1.m33 * m2.m31;
			m32 = m1.m03 * m2.m02 + m1.m13 * m2.m12 + m1.m23 * m2.m22 + m1.m33 * m2.m32;
			m33 = m1.m03 * m2.m03 + m1.m13 * m2.m13 + m1.m23 * m2.m23 + m1.m33 * m2.m33;

			this.m00 = m00;
			this.m01 = m01;
			this.m02 = m02;
			this.m03 = m03;
			this.m10 = m10;
			this.m11 = m11;
			this.m12 = m12;
			this.m13 = m13;
			this.m20 = m20;
			this.m21 = m21;
			this.m22 = m22;
			this.m23 = m23;
			this.m30 = m30;
			this.m31 = m31;
			this.m32 = m32;
			this.m33 = m33;
		}

		return this;

	}

	/**
	 * Multiplies matrix m1 times the transpose of matrix m2, and places the
	 * result into this.
	 * 
	 * @param m1
	 *                the matrix on the left hand side of the multiplication
	 * @param m2
	 *                the matrix on the right hand side of the
	 *                multiplication
	 */
	public final void mulTransposeRight(Matrix4 m1, Matrix4 m2) {
		if (this != m1 && this != m2) {
			this.m00 = m1.m00 * m2.m00 + m1.m01 * m2.m01 + m1.m02 * m2.m02 + m1.m03 * m2.m03;
			this.m01 = m1.m00 * m2.m10 + m1.m01 * m2.m11 + m1.m02 * m2.m12 + m1.m03 * m2.m13;
			this.m02 = m1.m00 * m2.m20 + m1.m01 * m2.m21 + m1.m02 * m2.m22 + m1.m03 * m2.m23;
			this.m03 = m1.m00 * m2.m30 + m1.m01 * m2.m31 + m1.m02 * m2.m32 + m1.m03 * m2.m33;

			this.m10 = m1.m10 * m2.m00 + m1.m11 * m2.m01 + m1.m12 * m2.m02 + m1.m13 * m2.m03;
			this.m11 = m1.m10 * m2.m10 + m1.m11 * m2.m11 + m1.m12 * m2.m12 + m1.m13 * m2.m13;
			this.m12 = m1.m10 * m2.m20 + m1.m11 * m2.m21 + m1.m12 * m2.m22 + m1.m13 * m2.m23;
			this.m13 = m1.m10 * m2.m30 + m1.m11 * m2.m31 + m1.m12 * m2.m32 + m1.m13 * m2.m33;

			this.m20 = m1.m20 * m2.m00 + m1.m21 * m2.m01 + m1.m22 * m2.m02 + m1.m23 * m2.m03;
			this.m21 = m1.m20 * m2.m10 + m1.m21 * m2.m11 + m1.m22 * m2.m12 + m1.m23 * m2.m13;
			this.m22 = m1.m20 * m2.m20 + m1.m21 * m2.m21 + m1.m22 * m2.m22 + m1.m23 * m2.m23;
			this.m23 = m1.m20 * m2.m30 + m1.m21 * m2.m31 + m1.m22 * m2.m32 + m1.m23 * m2.m33;

			this.m30 = m1.m30 * m2.m00 + m1.m31 * m2.m01 + m1.m32 * m2.m02 + m1.m33 * m2.m03;
			this.m31 = m1.m30 * m2.m10 + m1.m31 * m2.m11 + m1.m32 * m2.m12 + m1.m33 * m2.m13;
			this.m32 = m1.m30 * m2.m20 + m1.m31 * m2.m21 + m1.m32 * m2.m22 + m1.m33 * m2.m23;
			this.m33 = m1.m30 * m2.m30 + m1.m31 * m2.m31 + m1.m32 * m2.m32 + m1.m33 * m2.m33;
		} else {
			float m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, // vars
												// for
												// temp
												// result
												// matrix
			m30, m31, m32, m33;

			m00 = m1.m00 * m2.m00 + m1.m01 * m2.m01 + m1.m02 * m2.m02 + m1.m03 * m2.m03;
			m01 = m1.m00 * m2.m10 + m1.m01 * m2.m11 + m1.m02 * m2.m12 + m1.m03 * m2.m13;
			m02 = m1.m00 * m2.m20 + m1.m01 * m2.m21 + m1.m02 * m2.m22 + m1.m03 * m2.m23;
			m03 = m1.m00 * m2.m30 + m1.m01 * m2.m31 + m1.m02 * m2.m32 + m1.m03 * m2.m33;

			m10 = m1.m10 * m2.m00 + m1.m11 * m2.m01 + m1.m12 * m2.m02 + m1.m13 * m2.m03;
			m11 = m1.m10 * m2.m10 + m1.m11 * m2.m11 + m1.m12 * m2.m12 + m1.m13 * m2.m13;
			m12 = m1.m10 * m2.m20 + m1.m11 * m2.m21 + m1.m12 * m2.m22 + m1.m13 * m2.m23;
			m13 = m1.m10 * m2.m30 + m1.m11 * m2.m31 + m1.m12 * m2.m32 + m1.m13 * m2.m33;

			m20 = m1.m20 * m2.m00 + m1.m21 * m2.m01 + m1.m22 * m2.m02 + m1.m23 * m2.m03;
			m21 = m1.m20 * m2.m10 + m1.m21 * m2.m11 + m1.m22 * m2.m12 + m1.m23 * m2.m13;
			m22 = m1.m20 * m2.m20 + m1.m21 * m2.m21 + m1.m22 * m2.m22 + m1.m23 * m2.m23;
			m23 = m1.m20 * m2.m30 + m1.m21 * m2.m31 + m1.m22 * m2.m32 + m1.m23 * m2.m33;

			m30 = m1.m30 * m2.m00 + m1.m31 * m2.m01 + m1.m32 * m2.m02 + m1.m33 * m2.m03;
			m31 = m1.m30 * m2.m10 + m1.m31 * m2.m11 + m1.m32 * m2.m12 + m1.m33 * m2.m13;
			m32 = m1.m30 * m2.m20 + m1.m31 * m2.m21 + m1.m32 * m2.m22 + m1.m33 * m2.m23;
			m33 = m1.m30 * m2.m30 + m1.m31 * m2.m31 + m1.m32 * m2.m32 + m1.m33 * m2.m33;

			this.m00 = m00;
			this.m01 = m01;
			this.m02 = m02;
			this.m03 = m03;
			this.m10 = m10;
			this.m11 = m11;
			this.m12 = m12;
			this.m13 = m13;
			this.m20 = m20;
			this.m21 = m21;
			this.m22 = m22;
			this.m23 = m23;
			this.m30 = m30;
			this.m31 = m31;
			this.m32 = m32;
			this.m33 = m33;
		}

	}

	/**
	 * Negates the value of this matrix: this = -this.
	 */
	public final void negate() {
		m00 = -m00;
		m01 = -m01;
		m02 = -m02;
		m03 = -m03;
		m10 = -m10;
		m11 = -m11;
		m12 = -m12;
		m13 = -m13;
		m20 = -m20;
		m21 = -m21;
		m22 = -m22;
		m23 = -m23;
		m30 = -m30;
		m31 = -m31;
		m32 = -m32;
		m33 = -m33;
	}

	/**
	 * Sets the value of this matrix equal to the negation of of the
	 * Matrix4f parameter.
	 * 
	 * @param m1
	 *                the source matrix
	 */
	public final void negate(Matrix4 m1) {
		this.m00 = -m1.m00;
		this.m01 = -m1.m01;
		this.m02 = -m1.m02;
		this.m03 = -m1.m03;
		this.m10 = -m1.m10;
		this.m11 = -m1.m11;
		this.m12 = -m1.m12;
		this.m13 = -m1.m13;
		this.m20 = -m1.m20;
		this.m21 = -m1.m21;
		this.m22 = -m1.m22;
		this.m23 = -m1.m23;
		this.m30 = -m1.m30;
		this.m31 = -m1.m31;
		this.m32 = -m1.m32;
		this.m33 = -m1.m33;
	}

	/**
	 * Sets the value of this matrix to a counter clockwise rotation about
	 * the x axis.
	 * 
	 * @param angle
	 *                the angle to rotate about the X axis in radians
	 */
	public final void rotX(float angle) {
		float sinAngle, cosAngle;

		sinAngle = (float) Math.sin(angle);
		cosAngle = (float) Math.cos(angle);

		this.m00 = (float) 1.0;
		this.m01 = (float) 0.0;
		this.m02 = (float) 0.0;
		this.m03 = (float) 0.0;

		this.m10 = (float) 0.0;
		this.m11 = cosAngle;
		this.m12 = -sinAngle;
		this.m13 = (float) 0.0;

		this.m20 = (float) 0.0;
		this.m21 = sinAngle;
		this.m22 = cosAngle;
		this.m23 = (float) 0.0;

		this.m30 = (float) 0.0;
		this.m31 = (float) 0.0;
		this.m32 = (float) 0.0;
		this.m33 = (float) 1.0;
	}

	/**
	 * Sets the value of this matrix to a counter clockwise rotation about
	 * the y axis.
	 * 
	 * @param angle
	 *                the angle to rotate about the Y axis in radians
	 */
	public final void rotY(float angle) {
		float sinAngle, cosAngle;

		sinAngle = (float) Math.sin(angle);
		cosAngle = (float) Math.cos(angle);

		this.m00 = cosAngle;
		this.m01 = (float) 0.0;
		this.m02 = sinAngle;
		this.m03 = (float) 0.0;

		this.m10 = (float) 0.0;
		this.m11 = (float) 1.0;
		this.m12 = (float) 0.0;
		this.m13 = (float) 0.0;

		this.m20 = -sinAngle;
		this.m21 = (float) 0.0;
		this.m22 = cosAngle;
		this.m23 = (float) 0.0;

		this.m30 = (float) 0.0;
		this.m31 = (float) 0.0;
		this.m32 = (float) 0.0;
		this.m33 = (float) 1.0;
	}

	/**
	 * Sets the value of this matrix to a counter clockwise rotation about
	 * the z axis.
	 * 
	 * @param angle
	 *                the angle to rotate about the Z axis in radians
	 */
	public final void rotZ(float angle) {
		float sinAngle, cosAngle;

		sinAngle = (float) Math.sin(angle);
		cosAngle = (float) Math.cos(angle);

		this.m00 = cosAngle;
		this.m01 = -sinAngle;
		this.m02 = (float) 0.0;
		this.m03 = (float) 0.0;

		this.m10 = sinAngle;
		this.m11 = cosAngle;
		this.m12 = (float) 0.0;
		this.m13 = (float) 0.0;

		this.m20 = (float) 0.0;
		this.m21 = (float) 0.0;
		this.m22 = (float) 1.0;
		this.m23 = (float) 0.0;

		this.m30 = (float) 0.0;
		this.m31 = (float) 0.0;
		this.m32 = (float) 0.0;
		this.m33 = (float) 1.0;
	}

	/**
	 * Sets the value of this matrix to the matrix conversion of the (single
	 * precision) axis and angle argument.
	 * 
	 * @param a1
	 *                the axis and angle to be converted
	 */
	public final void set(AxisAngle a1) {
		float mag = (float) Math.sqrt(a1.x * a1.x + a1.y * a1.y + a1.z * a1.z);
		if (mag < EPS) {
			m00 = 1.0f;
			m01 = 0.0f;
			m02 = 0.0f;

			m10 = 0.0f;
			m11 = 1.0f;
			m12 = 0.0f;

			m20 = 0.0f;
			m21 = 0.0f;
			m22 = 1.0f;
		} else {
			mag = 1.0f / mag;
			float ax = a1.x * mag;
			float ay = a1.y * mag;
			float az = a1.z * mag;

			float sinTheta = (float) Math.sin(a1.angle);
			float cosTheta = (float) Math.cos(a1.angle);
			float t = 1.0f - cosTheta;

			float xz = ax * az;
			float xy = ax * ay;
			float yz = ay * az;

			m00 = t * ax * ax + cosTheta;
			m01 = t * xy - sinTheta * az;
			m02 = t * xz + sinTheta * ay;

			m10 = t * xy + sinTheta * az;
			m11 = t * ay * ay + cosTheta;
			m12 = t * yz - sinTheta * ax;

			m20 = t * xz - sinTheta * ay;
			m21 = t * yz + sinTheta * ax;
			m22 = t * az * az + cosTheta;
		}
		m03 = 0.0f;
		m13 = 0.0f;
		m23 = 0.0f;

		m30 = 0.0f;
		m31 = 0.0f;
		m32 = 0.0f;
		m33 = 1.0f;
	}

	/**
	 * Sets the value of this matrix to a scale matrix with the the passed
	 * scale amount.
	 * 
	 * @param scale
	 *                the scale factor for the matrix
	 */
	public final void set(float scale) {
		this.m00 = scale;
		this.m01 = (float) 0.0;
		this.m02 = (float) 0.0;
		this.m03 = (float) 0.0;

		this.m10 = (float) 0.0;
		this.m11 = scale;
		this.m12 = (float) 0.0;
		this.m13 = (float) 0.0;

		this.m20 = (float) 0.0;
		this.m21 = (float) 0.0;
		this.m22 = scale;
		this.m23 = (float) 0.0;

		this.m30 = (float) 0.0;
		this.m31 = (float) 0.0;
		this.m32 = (float) 0.0;
		this.m33 = (float) 1.0;
	}

	/**
	 * Sets the value of this transform to a scale and translation matrix;
	 * the scale is not applied to the translation and all of the matrix
	 * values are modified.
	 * 
	 * @param scale
	 *                the scale factor for the matrix
	 * @param t1
	 *                the translation amount
	 */
	public final void set(float scale, Vector3 t1) {
		this.m00 = scale;
		this.m01 = (float) 0.0;
		this.m02 = (float) 0.0;
		this.m03 = t1.x;

		this.m10 = (float) 0.0;
		this.m11 = scale;
		this.m12 = (float) 0.0;
		this.m13 = t1.y;

		this.m20 = (float) 0.0;
		this.m21 = (float) 0.0;
		this.m22 = scale;
		this.m23 = t1.z;

		this.m30 = (float) 0.0;
		this.m31 = (float) 0.0;
		this.m32 = (float) 0.0;
		this.m33 = (float) 1.0;
	}

	/**
	 * Sets the values in this Matrix4f equal to the row-major array
	 * parameter (ie, the first four elements of the array will be copied
	 * into the first row of this matrix, etc.).
	 * 
	 * @param m
	 *                the single precision array of length 16
	 */
	public final void set(float[] m) {
		m00 = m[0];
		m01 = m[1];
		m02 = m[2];
		m03 = m[3];
		m10 = m[4];
		m11 = m[5];
		m12 = m[6];
		m13 = m[7];
		m20 = m[8];
		m21 = m[9];
		m22 = m[10];
		m23 = m[11];
		m30 = m[12];
		m31 = m[13];
		m32 = m[14];
		m33 = m[15];
	}

	/**
	 * Sets the rotational component (upper 3x3) of this matrix to the
	 * matrix values in the single precision Matrix3f argument; the other
	 * elements of this matrix are initialized as if this were an identity
	 * matrix (i.e., affine matrix with no translational component).
	 * 
	 * @param m1
	 *                the single-precision 3x3 matrix
	 */
	public final void set(Matrix3 m1) {
		m00 = m1.m00;
		m01 = m1.m01;
		m02 = m1.m02;
		m03 = 0.0f;
		m10 = m1.m10;
		m11 = m1.m11;
		m12 = m1.m12;
		m13 = 0.0f;
		m20 = m1.m20;
		m21 = m1.m21;
		m22 = m1.m22;
		m23 = 0.0f;
		m30 = 0.0f;
		m31 = 0.0f;
		m32 = 0.0f;
		m33 = 1.0f;
	}

	/**
	 * Sets the value of this matrix from the rotation expressed by the
	 * rotation matrix m1, the translation t1, and the scale factor. The
	 * translation is not modified by the scale.
	 * 
	 * @param m1
	 *                the rotation component
	 * @param t1
	 *                the translation component
	 * @param scale
	 *                the scale component
	 */
	public final void set(Matrix3 m1, Vector3 t1, float scale) {
		this.m00 = m1.m00 * scale;
		this.m01 = m1.m01 * scale;
		this.m02 = m1.m02 * scale;
		this.m03 = t1.x;

		this.m10 = m1.m10 * scale;
		this.m11 = m1.m11 * scale;
		this.m12 = m1.m12 * scale;
		this.m13 = t1.y;

		this.m20 = m1.m20 * scale;
		this.m21 = m1.m21 * scale;
		this.m22 = m1.m22 * scale;
		this.m23 = t1.z;

		this.m30 = 0.0f;
		this.m31 = 0.0f;
		this.m32 = 0.0f;
		this.m33 = 1.0f;
	}

	/**
	 * Sets the value of this matrix to a copySource of the passed matrix
	 * m1.
	 * 
	 * @param m1
	 *                the matrix to be copied
	 */
	public final void set(Matrix4 m1) {
		this.m00 = m1.m00;
		this.m01 = m1.m01;
		this.m02 = m1.m02;
		this.m03 = m1.m03;

		this.m10 = m1.m10;
		this.m11 = m1.m11;
		this.m12 = m1.m12;
		this.m13 = m1.m13;

		this.m20 = m1.m20;
		this.m21 = m1.m21;
		this.m22 = m1.m22;
		this.m23 = m1.m23;

		this.m30 = m1.m30;
		this.m31 = m1.m31;
		this.m32 = m1.m32;
		this.m33 = m1.m33;
	}

	/**
	 * Sets the value of this matrix to the matrix conversion of the single
	 * precision quaternion argument.
	 * 
	 * @param q1
	 *                the quaternion to be converted
	 */
	public final void set(Quaternion q1) {
		this.m00 = (1.0f - 2.0f * q1.y * q1.y - 2.0f * q1.z * q1.z);
		this.m10 = (2.0f * (q1.x * q1.y + q1.w * q1.z));
		this.m20 = (2.0f * (q1.x * q1.z - q1.w * q1.y));

		this.m01 = (2.0f * (q1.x * q1.y - q1.w * q1.z));
		this.m11 = (1.0f - 2.0f * q1.x * q1.x - 2.0f * q1.z * q1.z);
		this.m21 = (2.0f * (q1.y * q1.z + q1.w * q1.x));

		this.m02 = (2.0f * (q1.x * q1.z + q1.w * q1.y));
		this.m12 = (2.0f * (q1.y * q1.z - q1.w * q1.x));
		this.m22 = (1.0f - 2.0f * q1.x * q1.x - 2.0f * q1.y * q1.y);

		this.m03 = (float) 0.0;
		this.m13 = (float) 0.0;
		this.m23 = (float) 0.0;

		this.m30 = (float) 0.0;
		this.m31 = (float) 0.0;
		this.m32 = (float) 0.0;
		this.m33 = (float) 1.0;
	}

	/**
	 * Sets the value of this matrix from the rotation expressed by the
	 * quaternion q1, the translation t1, and the scale s.
	 * 
	 * @param q1
	 *                the rotation expressed as a quaternion
	 * @param t1
	 *                the translation
	 * @param s
	 *                the scale value
	 */
	public final void set(Quaternion q1, Vector3 t1, float s) {

		this.m00 = (s * (1.0f - 2.0f * q1.y * q1.y - 2.0f * q1.z * q1.z));
		this.m10 = (s * (2.0f * (q1.x * q1.y + q1.w * q1.z)));
		this.m20 = (s * (2.0f * (q1.x * q1.z - q1.w * q1.y)));

		this.m01 = (s * (2.0f * (q1.x * q1.y - q1.w * q1.z)));
		this.m11 = (s * (1.0f - 2.0f * q1.x * q1.x - 2.0f * q1.z * q1.z));
		this.m21 = (s * (2.0f * (q1.y * q1.z + q1.w * q1.x)));

		this.m02 = (s * (2.0f * (q1.x * q1.z + q1.w * q1.y)));
		this.m12 = (s * (2.0f * (q1.y * q1.z - q1.w * q1.x)));
		this.m22 = (s * (1.0f - 2.0f * q1.x * q1.x - 2.0f * q1.y * q1.y));

		this.m03 = t1.x;
		this.m13 = t1.y;
		this.m23 = t1.z;

		this.m30 = (float) 0.0;
		this.m31 = (float) 0.0;
		this.m32 = (float) 0.0;
		this.m33 = (float) 1.0;
	}

	public void set(Quaternion q1, Vector3 t1, Vector3 s) {
		if (Math.abs(q1.mag() - 1) > 0.2)
			throw new IllegalArgumentException("not normalized <" + q1 + ">");

		this.m00 = (s.x * (1.0f - 2.0f * q1.y * q1.y - 2.0f * q1.z * q1.z));
		this.m10 = (s.y * (2.0f * (q1.x * q1.y + q1.w * q1.z)));
		this.m20 = (s.z * (2.0f * (q1.x * q1.z - q1.w * q1.y)));

		this.m01 = (s.x * (2.0f * (q1.x * q1.y - q1.w * q1.z)));
		this.m11 = (s.y * (1.0f - 2.0f * q1.x * q1.x - 2.0f * q1.z * q1.z));
		this.m21 = (s.z * (2.0f * (q1.y * q1.z + q1.w * q1.x)));

		this.m02 = (s.x * (2.0f * (q1.x * q1.z + q1.w * q1.y)));
		this.m12 = (s.y * (2.0f * (q1.y * q1.z - q1.w * q1.x)));
		this.m22 = (s.z * (1.0f - 2.0f * q1.x * q1.x - 2.0f * q1.y * q1.y));

		this.m03 = t1.x;
		this.m13 = t1.y;
		this.m23 = t1.z;

		this.m30 = (float) 0.0;
		this.m31 = (float) 0.0;
		this.m32 = (float) 0.0;
		this.m33 = (float) 1.0;
	}

	/**
	 * Sets the value of this matrix to a translate matrix with the passed
	 * translation value.
	 * 
	 * @param v1
	 *                the translation amount
	 */
	public final void set(Vector3 v1) {
		this.m00 = (float) 1.0;
		this.m01 = (float) 0.0;
		this.m02 = (float) 0.0;
		this.m03 = v1.x;

		this.m10 = (float) 0.0;
		this.m11 = (float) 1.0;
		this.m12 = (float) 0.0;
		this.m13 = v1.y;

		this.m20 = (float) 0.0;
		this.m21 = (float) 0.0;
		this.m22 = (float) 1.0;
		this.m23 = v1.z;

		this.m30 = (float) 0.0;
		this.m31 = (float) 0.0;
		this.m32 = (float) 0.0;
		this.m33 = (float) 1.0;
	}

	/**
	 * Sets the value of this transform to a scale and translation matrix;
	 * the translation is scaled by the scale factor and all of the matrix
	 * values are modified.
	 * 
	 * @param t1
	 *                the translation amount
	 * @param scale
	 *                the scale factor for the matrix
	 */
	public final void set(Vector3 t1, float scale) {
		this.m00 = scale;
		this.m01 = (float) 0.0;
		this.m02 = (float) 0.0;
		this.m03 = scale * t1.x;

		this.m10 = (float) 0.0;
		this.m11 = scale;
		this.m12 = (float) 0.0;
		this.m13 = scale * t1.y;

		this.m20 = (float) 0.0;
		this.m21 = (float) 0.0;
		this.m22 = scale;
		this.m23 = scale * t1.z;

		this.m30 = (float) 0.0;
		this.m31 = (float) 0.0;
		this.m32 = (float) 0.0;
		this.m33 = (float) 1.0;
	}

	/**
	 * Sets the specified column of this matrix4f to the four values
	 * provided.
	 * 
	 * @param column
	 *                the column number to be modified (zero indexed)
	 * @param v
	 *                the replacement column
	 */
	public final void setColumn(int column, float v[]) {
		switch (column) {
		case 0:
			this.m00 = v[0];
			this.m10 = v[1];
			this.m20 = v[2];
			this.m30 = v[3];
			break;

		case 1:
			this.m01 = v[0];
			this.m11 = v[1];
			this.m21 = v[2];
			this.m31 = v[3];
			break;

		case 2:
			this.m02 = v[0];
			this.m12 = v[1];
			this.m22 = v[2];
			this.m32 = v[3];
			break;

		case 3:
			this.m03 = v[0];
			this.m13 = v[1];
			this.m23 = v[2];
			this.m33 = v[3];
			break;

		default:
			throw new ArrayIndexOutOfBoundsException(column);
		}
	}

	/**
	 * Sets the specified column of this matrix4f to the four values
	 * provided.
	 * 
	 * @param column
	 *                the column number to be modified (zero indexed)
	 * @param x
	 *                the first row element
	 * @param y
	 *                the second row element
	 * @param z
	 *                the third row element
	 * @param w
	 *                the fourth row element
	 */
	public final void setColumn(int column, float x, float y, float z, float w) {
		switch (column) {
		case 0:
			this.m00 = x;
			this.m10 = y;
			this.m20 = z;
			this.m30 = w;
			break;

		case 1:
			this.m01 = x;
			this.m11 = y;
			this.m21 = z;
			this.m31 = w;
			break;

		case 2:
			this.m02 = x;
			this.m12 = y;
			this.m22 = z;
			this.m32 = w;
			break;

		case 3:
			this.m03 = x;
			this.m13 = y;
			this.m23 = z;
			this.m33 = w;
			break;

		default:
			throw new ArrayIndexOutOfBoundsException(column);
		}
	}

	public void setColumn(int column, Vector3 v) {
		switch (column) {
		case 0:
			this.m00 = v.x;
			this.m10 = v.y;
			this.m20 = v.z;
			break;

		case 1:
			this.m01 = v.x;
			this.m11 = v.y;
			this.m21 = v.z;
			break;

		case 2:
			this.m02 = v.x;
			this.m12 = v.y;
			this.m22 = v.z;
			break;

		case 3:
			this.m03 = v.x;
			this.m13 = v.y;
			this.m23 = v.z;
			break;

		default:
			throw new ArrayIndexOutOfBoundsException(column);
		}

	}

	/**
	 * Sets the specified column of this matrix4f to the vector provided.
	 * 
	 * @param column
	 *                the column number to be modified (zero indexed)
	 * @param v
	 *                the replacement column
	 */
	public final void setColumn(int column, Vector4 v) {
		switch (column) {
		case 0:
			this.m00 = v.x;
			this.m10 = v.y;
			this.m20 = v.z;
			this.m30 = v.w;
			break;

		case 1:
			this.m01 = v.x;
			this.m11 = v.y;
			this.m21 = v.z;
			this.m31 = v.w;
			break;

		case 2:
			this.m02 = v.x;
			this.m12 = v.y;
			this.m22 = v.z;
			this.m32 = v.w;
			break;

		case 3:
			this.m03 = v.x;
			this.m13 = v.y;
			this.m23 = v.z;
			this.m33 = v.w;
			break;

		default:
			throw new ArrayIndexOutOfBoundsException(column);
		}
	}

	/**
	 * Sets the specified element of this matrix4f to the value provided.
	 * 
	 * @param row
	 *                the row number to be modified (zero indexed)
	 * @param column
	 *                the column number to be modified (zero indexed)
	 * @param value
	 *                the new value
	 */
	public final void setElement(int row, int column, float value) {
		switch (row) {
		case 0:
			switch (column) {
			case 0:
				this.m00 = value;
				break;
			case 1:
				this.m01 = value;
				break;
			case 2:
				this.m02 = value;
				break;
			case 3:
				this.m03 = value;
				break;
			default:
				throw new ArrayIndexOutOfBoundsException(column + " " + row);
			}
			break;

		case 1:
			switch (column) {
			case 0:
				this.m10 = value;
				break;
			case 1:
				this.m11 = value;
				break;
			case 2:
				this.m12 = value;
				break;
			case 3:
				this.m13 = value;
				break;
			default:
				throw new ArrayIndexOutOfBoundsException(column + " " + row);
			}
			break;

		case 2:
			switch (column) {
			case 0:
				this.m20 = value;
				break;
			case 1:
				this.m21 = value;
				break;
			case 2:
				this.m22 = value;
				break;
			case 3:
				this.m23 = value;
				break;
			default:
				throw new ArrayIndexOutOfBoundsException(column + " " + row);
			}
			break;

		case 3:
			switch (column) {
			case 0:
				this.m30 = value;
				break;
			case 1:
				this.m31 = value;
				break;
			case 2:
				this.m32 = value;
				break;
			case 3:
				this.m33 = value;
				break;
			default:
				throw new ArrayIndexOutOfBoundsException(column + " " + row);
			}
			break;

		default:
			throw new ArrayIndexOutOfBoundsException(column + " " + row);
		}
	}

	/**
	 * Sets this Matrix4f to identity.
	 */
	public final Matrix4 setIdentity() {
		this.m00 = (float) 1.0;
		this.m01 = (float) 0.0;
		this.m02 = (float) 0.0;
		this.m03 = (float) 0.0;

		this.m10 = (float) 0.0;
		this.m11 = (float) 1.0;
		this.m12 = (float) 0.0;
		this.m13 = (float) 0.0;

		this.m20 = (float) 0.0;
		this.m21 = (float) 0.0;
		this.m22 = (float) 1.0;
		this.m23 = (float) 0.0;

		this.m30 = (float) 0.0;
		this.m31 = (float) 0.0;
		this.m32 = (float) 0.0;
		this.m33 = (float) 1.0;
		return this;
	}

	/**
	 * Sets the rotational component (upper 3x3) of this matrix to the
	 * matrix equivalent values of the axis-angle argument; the other
	 * elements of this matrix are unchanged; a singular value decomposition
	 * is performed on this object's upper 3x3 matrix to factor out the
	 * scale, then this object's upper 3x3 matrix components are replaced by
	 * the matrix equivalent of the axis-angle, and then the scale is
	 * reapplied to the rotational components.
	 * 
	 * @param a1
	 *                the axis-angle to be converted (x, y, z, angle)
	 */
	public final void setRotation(AxisAngle a1) {
		double[] tmp_rot = new double[9]; // scratch matrix
		double[] tmp_scale = new double[3]; // scratch matrix

		getScaleRotate(tmp_scale, tmp_rot);

		double mag = Math.sqrt(a1.x * a1.x + a1.y * a1.y + a1.z * a1.z);
		if (mag < EPS) {
			m00 = 1.0f;
			m01 = 0.0f;
			m02 = 0.0f;

			m10 = 0.0f;
			m11 = 1.0f;
			m12 = 0.0f;

			m20 = 0.0f;
			m21 = 0.0f;
			m22 = 1.0f;
		} else {
			mag = 1.0 / mag;
			double ax = a1.x * mag;
			double ay = a1.y * mag;
			double az = a1.z * mag;

			double sinTheta = Math.sin(a1.angle);
			double cosTheta = Math.cos(a1.angle);
			double t = 1.0 - cosTheta;

			double xz = a1.x * a1.z;
			double xy = a1.x * a1.y;
			double yz = a1.y * a1.z;

			m00 = (float) ((t * ax * ax + cosTheta) * tmp_scale[0]);
			m01 = (float) ((t * xy - sinTheta * az) * tmp_scale[1]);
			m02 = (float) ((t * xz + sinTheta * ay) * tmp_scale[2]);

			m10 = (float) ((t * xy + sinTheta * az) * tmp_scale[0]);
			m11 = (float) ((t * ay * ay + cosTheta) * tmp_scale[1]);
			m12 = (float) ((t * yz - sinTheta * ax) * tmp_scale[2]);

			m20 = (float) ((t * xz - sinTheta * ay) * tmp_scale[0]);
			m21 = (float) ((t * yz + sinTheta * ax) * tmp_scale[1]);
			m22 = (float) ((t * az * az + cosTheta) * tmp_scale[2]);
		}

	}

	/**
	 * Sets the rotational component (upper 3x3) of this matrix to the
	 * matrix values in the single precision Matrix3f argument; the other
	 * elements of this matrix are unchanged; a singular value decomposition
	 * is performed on this object's upper 3x3 matrix to factor out the
	 * scale, then this object's upper 3x3 matrix components are replaced by
	 * the passed rotation components, and then the scale is reapplied to
	 * the rotational components.
	 * 
	 * @param m1
	 *                single precision 3x3 matrix
	 */
	public final Matrix4 setRotation(Matrix3 m1) {
		double[] tmp_rot = new double[9]; // scratch matrix
		double[] tmp_scale = new double[3]; // scratch matrix

		getScaleRotate(tmp_scale, tmp_rot);

		m00 = (float) (m1.m00 * tmp_scale[0]);
		m01 = (float) (m1.m01 * tmp_scale[1]);
		m02 = (float) (m1.m02 * tmp_scale[2]);

		m10 = (float) (m1.m10 * tmp_scale[0]);
		m11 = (float) (m1.m11 * tmp_scale[1]);
		m12 = (float) (m1.m12 * tmp_scale[2]);

		m20 = (float) (m1.m20 * tmp_scale[0]);
		m21 = (float) (m1.m21 * tmp_scale[1]);
		m22 = (float) (m1.m22 * tmp_scale[2]);
		return this;
	}

	/**
	 * Sets the rotational component (upper 3x3) of this matrix to the
	 * matrix equivalent values of the quaternion argument; the other
	 * elements of this matrix are unchanged; a singular value decomposition
	 * is performed on this object's upper 3x3 matrix to factor out the
	 * scale, then this object's upper 3x3 matrix components are replaced by
	 * the matrix equivalent of the quaternion, and then the scale is
	 * reapplied to the rotational components.
	 * 
	 * @param q1
	 *                the quaternion that specifies the rotation
	 */
	public final Matrix4 setRotation(Quaternion q1) {
		double[] tmp_rot = new double[9]; // scratch matrix
		double[] tmp_scale = new double[3]; // scratch matrix
		getScaleRotate(tmp_scale, tmp_rot);

		m00 = (float) ((1.0f - 2.0f * q1.y * q1.y - 2.0f * q1.z * q1.z) * tmp_scale[0]);
		m10 = (float) ((2.0f * (q1.x * q1.y + q1.w * q1.z)) * tmp_scale[0]);
		m20 = (float) ((2.0f * (q1.x * q1.z - q1.w * q1.y)) * tmp_scale[0]);

		m01 = (float) ((2.0f * (q1.x * q1.y - q1.w * q1.z)) * tmp_scale[1]);
		m11 = (float) ((1.0f - 2.0f * q1.x * q1.x - 2.0f * q1.z * q1.z) * tmp_scale[1]);
		m21 = (float) ((2.0f * (q1.y * q1.z + q1.w * q1.x)) * tmp_scale[1]);

		m02 = (float) ((2.0f * (q1.x * q1.z + q1.w * q1.y)) * tmp_scale[2]);
		m12 = (float) ((2.0f * (q1.y * q1.z - q1.w * q1.x)) * tmp_scale[2]);
		m22 = (float) ((1.0f - 2.0f * q1.x * q1.x - 2.0f * q1.y * q1.y) * tmp_scale[2]);
		return this;
	}

	/**
	 * Replaces the upper 3x3 matrix values of this matrix with the values
	 * in the matrix m1.
	 * 
	 * @param m1
	 *                the matrix that will be the new upper 3x3
	 */
	public final void setRotationScale(Matrix3 m1) {
		m00 = m1.m00;
		m01 = m1.m01;
		m02 = m1.m02;
		m10 = m1.m10;
		m11 = m1.m11;
		m12 = m1.m12;
		m20 = m1.m20;
		m21 = m1.m21;
		m22 = m1.m22;
	}

	/**
	 * Sets the specified row of this matrix4f to the four values provided
	 * in the passed array.
	 * 
	 * @param row
	 *                the row number to be modified (zero indexed)
	 * @param v
	 *                the replacement row
	 */
	public final void setRow(int row, float v[]) {
		switch (row) {
		case 0:
			this.m00 = v[0];
			this.m01 = v[1];
			this.m02 = v[2];
			this.m03 = v[3];
			break;

		case 1:
			this.m10 = v[0];
			this.m11 = v[1];
			this.m12 = v[2];
			this.m13 = v[3];
			break;

		case 2:
			this.m20 = v[0];
			this.m21 = v[1];
			this.m22 = v[2];
			this.m23 = v[3];
			break;

		case 3:
			this.m30 = v[0];
			this.m31 = v[1];
			this.m32 = v[2];
			this.m33 = v[3];
			break;

		default:
			throw new ArrayIndexOutOfBoundsException(row);
		}
	}

	/**
	 * Sets the specified row of this matrix4f to the four values provided.
	 * 
	 * @param row
	 *                the row number to be modified (zero indexed)
	 * @param x
	 *                the first column element
	 * @param y
	 *                the second column element
	 * @param z
	 *                the third column element
	 * @param w
	 *                the fourth column element
	 */
	public final void setRow(int row, float x, float y, float z, float w) {
		switch (row) {
		case 0:
			this.m00 = x;
			this.m01 = y;
			this.m02 = z;
			this.m03 = w;
			break;

		case 1:
			this.m10 = x;
			this.m11 = y;
			this.m12 = z;
			this.m13 = w;
			break;

		case 2:
			this.m20 = x;
			this.m21 = y;
			this.m22 = z;
			this.m23 = w;
			break;

		case 3:
			this.m30 = x;
			this.m31 = y;
			this.m32 = z;
			this.m33 = w;
			break;

		default:
			throw new ArrayIndexOutOfBoundsException(row);
		}
	}

	/**
	 * Sets the specified row of this matrix4f to the Vector provided.
	 * 
	 * @param row
	 *                the row number to be modified (zero indexed)
	 * @param v
	 *                the replacement row
	 */
	public final void setRow(int row, Vector4 v) {
		switch (row) {
		case 0:
			this.m00 = v.x;
			this.m01 = v.y;
			this.m02 = v.z;
			this.m03 = v.w;
			break;

		case 1:
			this.m10 = v.x;
			this.m11 = v.y;
			this.m12 = v.z;
			this.m13 = v.w;
			break;

		case 2:
			this.m20 = v.x;
			this.m21 = v.y;
			this.m22 = v.z;
			this.m23 = v.w;
			break;

		case 3:
			this.m30 = v.x;
			this.m31 = v.y;
			this.m32 = v.z;
			this.m33 = v.w;
			break;

		default:
			throw new ArrayIndexOutOfBoundsException(row);
		}
	}

	/**
	 * Sets the scale component of the current matrix by factoring out the
	 * current scale (by doing an SVD) from the rotational component and
	 * multiplying by the new scale.
	 * 
	 * @param scale
	 *                the new scale amount
	 * @return
	 */
	public final Matrix4 setScale(float scale) {

		double[] tmp_rot = new double[9]; // scratch matrix
		double[] tmp_scale = new double[3]; // scratch matrix
		getScaleRotate(tmp_scale, tmp_rot);

		m00 = (float) (tmp_rot[0] * scale);
		m01 = (float) (tmp_rot[1] * scale);
		m02 = (float) (tmp_rot[2] * scale);

		m10 = (float) (tmp_rot[3] * scale);
		m11 = (float) (tmp_rot[4] * scale);
		m12 = (float) (tmp_rot[5] * scale);

		m20 = (float) (tmp_rot[6] * scale);
		m21 = (float) (tmp_rot[7] * scale);
		m22 = (float) (tmp_rot[8] * scale);

		return this;
	}

	/**
	 * Modifies the translational components of this matrix to the values of
	 * the Vector3f argument; the other values of this matrix are not
	 * modified.
	 * 
	 * @param trans
	 *                the translational component
	 * @return
	 */
	public final Matrix4 setTranslation(Vector3 trans) {
		m03 = trans.x;
		m13 = trans.y;
		m23 = trans.z;
		return this;
	}

	/**
	 * Sets this matrix to all zeros.
	 */
	public final void setZero() {
		m00 = 0.0f;
		m01 = 0.0f;
		m02 = 0.0f;
		m03 = 0.0f;
		m10 = 0.0f;
		m11 = 0.0f;
		m12 = 0.0f;
		m13 = 0.0f;
		m20 = 0.0f;
		m21 = 0.0f;
		m22 = 0.0f;
		m23 = 0.0f;
		m30 = 0.0f;
		m31 = 0.0f;
		m32 = 0.0f;
		m33 = 0.0f;
	}

	/**
	 * Sets this matrix to the matrix difference of itself and matrix m1
	 * (this = this - m1).
	 * 
	 * @param m1
	 *                the other matrix
	 * @return
	 */
	public final Matrix4 sub(Matrix4 m1) {
		this.m00 -= m1.m00;
		this.m01 -= m1.m01;
		this.m02 -= m1.m02;
		this.m03 -= m1.m03;

		this.m10 -= m1.m10;
		this.m11 -= m1.m11;
		this.m12 -= m1.m12;
		this.m13 -= m1.m13;

		this.m20 -= m1.m20;
		this.m21 -= m1.m21;
		this.m22 -= m1.m22;
		this.m23 -= m1.m23;

		this.m30 -= m1.m30;
		this.m31 -= m1.m31;
		this.m32 -= m1.m32;
		this.m33 -= m1.m33;
		return this;
	}

	/**
	 * Performs an element-by-element subtraction of matrix m2 from matrix
	 * m1 and places the result into matrix this (this = m2 - m1).
	 * 
	 * @param m1
	 *                the first matrix
	 * @param m2
	 *                the second matrix
	 */
	public final void sub(Matrix4 m1, Matrix4 m2) {
		this.m00 = m1.m00 - m2.m00;
		this.m01 = m1.m01 - m2.m01;
		this.m02 = m1.m02 - m2.m02;
		this.m03 = m1.m03 - m2.m03;

		this.m10 = m1.m10 - m2.m10;
		this.m11 = m1.m11 - m2.m11;
		this.m12 = m1.m12 - m2.m12;
		this.m13 = m1.m13 - m2.m13;

		this.m20 = m1.m20 - m2.m20;
		this.m21 = m1.m21 - m2.m21;
		this.m22 = m1.m22 - m2.m22;
		this.m23 = m1.m23 - m2.m23;

		this.m30 = m1.m30 - m2.m30;
		this.m31 = m1.m31 - m2.m31;
		this.m32 = m1.m32 - m2.m32;
		this.m33 = m1.m33 - m2.m33;
	}

	/**
	 * Returns a string that contains the values of this Matrix4f.
	 * 
	 * @return the String representation
	 */
	@Override
	public String toString() {
		return this.m00 + ", " + this.m01 + ", " + this.m02 + ", " + this.m03 + "\n" + this.m10 + ", " + this.m11 + ", " + this.m12 + ", " + this.m13 + "\n" + this.m20 + ", " + this.m21 + ", " + this.m22 + ", " + this.m23 + "\n" + this.m30 + ", " + this.m31 + ", " + this.m32 + ", " + this.m33 + "\n";
	}

	/**
	 * Transform the vector vec using this Transform and place the result
	 * back into vec.
	 * 
	 * @param vec
	 *                the single precision vector to be transformed
	 * @return
	 */
	public final <T extends Tuple4> T transform(T vec) {
		float x, y, z;

		x = m00 * vec.x + m01 * vec.y + m02 * vec.z + m03 * vec.w;
		y = m10 * vec.x + m11 * vec.y + m12 * vec.z + m13 * vec.w;
		z = m20 * vec.x + m21 * vec.y + m22 * vec.z + m23 * vec.w;
		vec.w = m30 * vec.x + m31 * vec.y + m32 * vec.z + m33 * vec.w;
		vec.x = x;
		vec.y = y;
		vec.z = z;
		return vec;
	}

	/**
	 * Transform the vector vec using this Matrix4f and place the result
	 * into vecOut.
	 * 
	 * @param vec
	 *                the single precision vector to be transformed
	 * @param vecOut
	 *                the vector into which the transformed values are
	 *                placed
	 */
	public final void transform(Tuple4 vec, Tuple4 vecOut) {
		float x, y, z;
		x = m00 * vec.x + m01 * vec.y + m02 * vec.z + m03 * vec.w;
		y = m10 * vec.x + m11 * vec.y + m12 * vec.z + m13 * vec.w;
		z = m20 * vec.x + m21 * vec.y + m22 * vec.z + m23 * vec.w;
		vecOut.w = m30 * vec.x + m31 * vec.y + m32 * vec.z + m33 * vec.w;
		vecOut.x = x;
		vecOut.y = y;
		vecOut.z = z;
	}

	/**
	 * Transforms the normal parameter by this transform and places the
	 * value back into normal. The fourth element of the normal is assumed
	 * to be zero.
	 * 
	 * @param normal
	 *                the input normal to be transformed.
	 */
	public final Vector3 transformDirection(Vector3 normal) {
		float x, y;

		x = m00 * normal.x + m01 * normal.y + m02 * normal.z;
		y = m10 * normal.x + m11 * normal.y + m12 * normal.z;
		normal.z = m20 * normal.x + m21 * normal.y + m22 * normal.z;
		normal.x = x;
		normal.y = y;
		return normal;
	}

	/**
	 * Transforms the normal parameter by this Matrix4f and places the value
	 * into normalOut. The fourth element of the normal is assumed to be
	 * zero.
	 * 
	 * @param normal
	 *                the input normal to be transformed.
	 * @param normalOut
	 *                the transformed normal
	 * @return
	 */
	public final Vector3 transformDirection(Vector3 normal, Vector3 normalOut) {
		if (normalOut == null)
			normalOut = new Vector3();
		float x, y;
		x = m00 * normal.x + m01 * normal.y + m02 * normal.z;
		y = m10 * normal.x + m11 * normal.y + m12 * normal.z;
		normalOut.z = m20 * normal.x + m21 * normal.y + m22 * normal.z;
		normalOut.x = x;
		normalOut.y = y;
		return normalOut;
	}

	/**
	 * Transforms the point parameter with this Matrix4f and places the
	 * result back into point. The fourth element of the point input
	 * paramter is assumed to be one.
	 * 
	 * @param point
	 *                the input point to be transformed.
	 */
	public final Vector3 transformPosition(Vector3 point) {
		float x, y;
		x = m00 * point.x + m01 * point.y + m02 * point.z + m03;
		y = m10 * point.x + m11 * point.y + m12 * point.z + m13;
		point.z = m20 * point.x + m21 * point.y + m22 * point.z + m23;
		point.x = x;
		point.y = y;
		return point;
	}

	/**
	 * Transforms the point parameter with this Matrix4f and places the
	 * result into pointOut. The fourth element of the point input paramter
	 * is assumed to be one.
	 * 
	 * @param point
	 *                the input point to be transformed.
	 * @param pointOut
	 *                the transformed point
	 */
	public final void transformPosition(Vector3 point, Vector3 pointOut) {
		float x, y;
		x = m00 * point.x + m01 * point.y + m02 * point.z + m03;
		y = m10 * point.x + m11 * point.y + m12 * point.z + m13;
		pointOut.z = m20 * point.x + m21 * point.y + m22 * point.z + m23;
		pointOut.x = x;
		pointOut.y = y;
	}

	public Matrix4 translate(float x, float y, float z) {
		m30 = m00 * x + m10 * y + m20 * z + m30;
		m31 = m01 * x + m11 * y + m21 * z + m31;
		m32 = m02 * x + m12 * y + m22 * z + m32;
		m33 = m03 * x + m13 * y + m23 * z + m33;
		return this;
	}

	/**
	 * Sets the value of this matrix to its transpose in place.
	 */
	public final Matrix4 transpose() {
		float temp;

		temp = this.m10;
		this.m10 = this.m01;
		this.m01 = temp;

		temp = this.m20;
		this.m20 = this.m02;
		this.m02 = temp;

		temp = this.m30;
		this.m30 = this.m03;
		this.m03 = temp;

		temp = this.m21;
		this.m21 = this.m12;
		this.m12 = temp;

		temp = this.m31;
		this.m31 = this.m13;
		this.m13 = temp;

		temp = this.m32;
		this.m32 = this.m23;
		this.m23 = temp;
		return this;
	}

	/**
	 * Sets the value of this matrix to the transpose of the argument
	 * matrix.
	 * 
	 * @param m1
	 *                the matrix to be transposed
	 */
	public final void transpose(Matrix4 m1) {
		if (this != m1) {
			this.m00 = m1.m00;
			this.m01 = m1.m10;
			this.m02 = m1.m20;
			this.m03 = m1.m30;

			this.m10 = m1.m01;
			this.m11 = m1.m11;
			this.m12 = m1.m21;
			this.m13 = m1.m31;

			this.m20 = m1.m02;
			this.m21 = m1.m12;
			this.m22 = m1.m22;
			this.m23 = m1.m32;

			this.m30 = m1.m03;
			this.m31 = m1.m13;
			this.m32 = m1.m23;
			this.m33 = m1.m33;
		} else
			this.transpose();
	}

	public Matrix4 transposeTranslation() {
		float temp;

		temp = this.m30;
		this.m30 = this.m03;
		this.m03 = temp;

		temp = this.m31;
		this.m31 = this.m13;
		this.m13 = temp;

		temp = this.m32;
		this.m32 = this.m23;
		this.m23 = temp;
		return this;
	}

	public Matrix4 zero() {
		this.m00 = (float) 0.0;
		this.m01 = (float) 0.0;
		this.m02 = (float) 0.0;
		this.m03 = (float) 0.0;

		this.m10 = (float) 0.0;
		this.m11 = (float) 0.0;
		this.m12 = (float) 0.0;
		this.m13 = (float) 0.0;

		this.m20 = (float) 0.0;
		this.m21 = (float) 0.0;
		this.m22 = (float) 0.0;
		this.m23 = (float) 0.0;

		this.m30 = (float) 0.0;
		this.m31 = (float) 0.0;
		this.m32 = (float) 0.0;
		this.m33 = (float) 0.0;
		return this;
	}

	private final void getScaleRotate(double scales[], double rots[]) {

		double[] tmp = new double[9]; // scratch matrix
		tmp[0] = m00;
		tmp[1] = m01;
		tmp[2] = m02;

		tmp[3] = m10;
		tmp[4] = m11;
		tmp[5] = m12;

		tmp[6] = m20;
		tmp[7] = m21;
		tmp[8] = m22;

		Matrix3.compute_svd(tmp, scales, rots);

		return;
	}

	/**
	 * General invert routine. Inverts m1 and places the result in "this".
	 * Note that this routine handles both the "this" version and the
	 * non-"this" version.
	 * 
	 * Also note that since this routine is slow anyway, we won't worry
	 * about allocating a little bit of garbage.
	 * 
	 * @return
	 */
	final Matrix4 invertGeneral(Matrix4 m1) {
		double temp[] = new double[16];
		double result[] = new double[16];
		int row_perm[] = new int[4];
		int i, r, c;

		// Use LU decomposition and backsubstitution code specifically
		// for floating-point 4x4 matrices.

		// Copy source matrix to t1tmp
		temp[0] = m1.m00;
		temp[1] = m1.m01;
		temp[2] = m1.m02;
		temp[3] = m1.m03;

		temp[4] = m1.m10;
		temp[5] = m1.m11;
		temp[6] = m1.m12;
		temp[7] = m1.m13;

		temp[8] = m1.m20;
		temp[9] = m1.m21;
		temp[10] = m1.m22;
		temp[11] = m1.m23;

		temp[12] = m1.m30;
		temp[13] = m1.m31;
		temp[14] = m1.m32;
		temp[15] = m1.m33;

		// Calculate LU decomposition: Is the matrix singular?
		if (!luDecomposition(temp, row_perm)) {
			// Matrix has no inverse
			throw new SingularMatrixException();
		}

		// Perform back substitution on the identity matrix
		for (i = 0; i < 16; i++)
			result[i] = 0.0;
		result[0] = 1.0;
		result[5] = 1.0;
		result[10] = 1.0;
		result[15] = 1.0;
		luBacksubstitution(temp, row_perm, result);

		this.m00 = (float) result[0];
		this.m01 = (float) result[1];
		this.m02 = (float) result[2];
		this.m03 = (float) result[3];

		this.m10 = (float) result[4];
		this.m11 = (float) result[5];
		this.m12 = (float) result[6];
		this.m13 = (float) result[7];

		this.m20 = (float) result[8];
		this.m21 = (float) result[9];
		this.m22 = (float) result[10];
		this.m23 = (float) result[11];

		this.m30 = (float) result[12];
		this.m31 = (float) result[13];
		this.m32 = (float) result[14];
		this.m33 = (float) result[15];
		return this;

	}

	public float minusIdentityFrobenius() {
		float m = (this.m00-1)*(this.m00-1)+this.m01*this.m01+this.m02*this.m02+this.m03*this.m03;
		m+=this.m10*this.m10+(this.m11-1)*(this.m11-1)+this.m12*this.m12+this.m13*this.m13;
		m+=this.m20*this.m20+this.m21*this.m21+(this.m22-1)*(this.m22-1)+this.m23*this.m23;
		m+=this.m30*this.m30+this.m31*this.m31+this.m32*this.m32+(this.m33-1)*(this.m33-1);
		return (float)Math.sqrt(m);
	}

	public float frobenius() {
		float m = this.m00*this.m00+this.m01*this.m01+this.m02*this.m02+this.m03*this.m03;
		m+=this.m10*this.m10+this.m11*this.m11+this.m12*this.m12+this.m13*this.m13;
		m+=this.m20*this.m20+this.m21*this.m21+this.m22*this.m22+this.m23*this.m23;
		m+=this.m30*this.m30+this.m31*this.m31+this.m32*this.m32+this.m33*this.m33;
		
		return (float) Math.sqrt(m);
	}

	public Matrix4 sqrt() {
		Matrix4 x = new Matrix4(this);
		Matrix4 y = new Matrix4().setIdentity();

		int q = 0;
		while (new Matrix4().mul(x, x).sub(this).frobenius() > 1e-6 && q < 10) {
			Matrix4 ix = new Matrix4().invertGeneral(x);
			Matrix4 iy = new Matrix4().invertGeneral(y);

			x = new Matrix4(x).add(iy).mul(0.5f);
			y = new Matrix4(y).add(ix).mul(0.5f);
			q++;
		}
		this.set(x);
		return this;
	}
	
	public Matrix4 exp()
	{
		float zz = frobenius();
		int j = (int) Math.max(0, 1+Math.log10(zz)/Math.log10(2));
		
		Matrix4 a = new Matrix4(this).mul((float) Math.pow(2, -j));
		Matrix4 d = new Matrix4().setIdentity();
		Matrix4 n = new Matrix4().setIdentity();
		Matrix4 x = new Matrix4().setIdentity();
		float c = 1;
		int q = 6;
		for(int k=1;k<q+1;k++)
		{
			c = c*(q-k+1)/(k*(2*q-k+1));
			x = new Matrix4().mul(a, x);
			n = n.madd(c, x);
			d = d.madd((float) (Math.pow(-1, k)*c), x);
			
			
			System.out.println(" -- "+x+" "+n+" "+d+"   "+c);
			
		}
		
		x = new Matrix4().mul(new Matrix4().invertGeneral(d), n);
		int p = (int)Math.pow(2, j);

		System.out.println(" x "+x+" "+p);
		
		Matrix4 x2 = new Matrix4(x);
		for(int i=0;i<p-1;i++)
		{
			x2 = new Matrix4().mul(x2, x);
		}
		
		this.set(x2);
		return this;
	}
	

	public Matrix4 log() {
		int k = 0;
		while (minusIdentityFrobenius() > 1e-4 && k < 20) {
			sqrt();
			k++;
		}
		this.m00 -= 1;
		this.m11 -= 1;
		this.m22 -= 1;
		this.m33 -= 1;

		Matrix4 z = new Matrix4(this);
		Matrix4 x = new Matrix4(this);
		int i = 1;
		while (z.frobenius() > 1e-9 && i < 9) {			
			z = new Matrix4().mul(z, this);
			i++;
			x = new Matrix4(x).madd(1f / i, z);
		}
		x.mul((float) Math.pow(2, k));

		this.set(x);
		return this;
	}
	
	public Matrix4 alexaDot(float s)
	{
		log();
		mul(s);
		exp();
		return this;
	}

	public Matrix4 alexaPlus(Matrix4 m)
	{
		Matrix4 m2 = new Matrix4(m);
		m2.log();
		log();
		add(m2);
		exp();
		return this;
	}
	
	static public Matrix4 alexaInterpolate(Matrix4 a, Matrix4 b, float t)
	{
		a = new Matrix4(a);
		b = new Matrix4(b);
		
		a.alexaDot(1-t);
		b.alexaDot(t);
		
		return a.alexaPlus(b);
		
	}
	
	

}
