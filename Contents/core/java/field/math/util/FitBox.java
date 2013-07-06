package field.math.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import field.math.linalg.EigenStructure;
import field.math.linalg.EigenStructure.EigenStructureException;
import field.math.linalg.MatrixN;
import field.math.linalg.MatrixN.DimensionMismatchException;
import field.math.linalg.Vector3;
import field.math.linalg.VectorN;

/**
 * 
 * Created on Apr 18, 2004
 * 
 * @author marc
 */
public class FitBox {

	public FitBox() {
	}

	public Vector3 center = new Vector3();

	MatrixN m3 = new MatrixN(3, 3);

	Vector3 kDiff = new Vector3();

	EigenStructure structure;

	public class Axis {
		public Vector3 direction = new Vector3();

		public float length;

		public String toString() {
			return direction + ":" + length;
		}

		public float positiveLength;

		public float negativeLength;
	}

	public Vector3 getCloseFit(Vector3 xyz, Axis[] a) {
		Vector3 at = new Vector3(center);

		for (int z = 0; z < a.length; z++) {
			Vector3.add(a[z].direction, xyz.get(z) * (a[z].positiveLength - a[z].negativeLength) + a[z].negativeLength, at, at);
		}
		return at;
	}

	public Vector3 getCloseFitNormalized(Vector3 xyz, Axis[] a) {
		Vector3 at = new Vector3(center);

		for (int z = 0; z < a.length; z++) {
			Vector3.add(a[z].direction, xyz.get(z) * (2) + -1, at, at);
		}
		return at;
	}

	public Vector3 getCloseDirection(Vector3 xyz, Axis[] a) {
		Vector3 at = new Vector3();

		for (int z = 0; z < a.length; z++) {
			Vector3.add(a[z].direction, xyz.get(z), at, at);
		}
		return at;
	}

	public Axis[] axes = new Axis[3];

	public Vector3 getCenter() {
		return center;
	}

	public Axis[] fit(Vector3[] points) {
		center.zero();
		for (int i = 0; i < points.length; i++) {
			center.add(points[i], center);
		}
		float fInvQuantity = 1 / (float) points.length;
		center.scale(1 / (float) points.length);

		float fSumXX = 0.0f, fSumXY = 0.0f, fSumXZ = 0.0f;
		float fSumYY = (float) 0.0, fSumYZ = (float) 0.0, fSumZZ = (float) 0.0;
		for (int i = 0; i < points.length; i++) {
			kDiff.sub(points[i], center);
			fSumXX += kDiff.x * kDiff.x;
			fSumXY += kDiff.x * kDiff.y;
			fSumXZ += kDiff.x * kDiff.z;
			fSumYY += kDiff.y * kDiff.y;
			fSumYZ += kDiff.y * kDiff.z;
			fSumZZ += kDiff.z * kDiff.z;
		}
		fSumXX *= fInvQuantity;
		fSumXY *= fInvQuantity;
		fSumXZ *= fInvQuantity;
		fSumYY *= fInvQuantity;
		fSumYZ *= fInvQuantity;
		fSumZZ *= fInvQuantity;

		// compute eigenvectors for covariance MatrixN
		m3.set(0, 0, fSumXX);
		m3.set(0, 1, fSumXY);
		m3.set(0, 2, fSumXZ);

		m3.set(1, 0, fSumXY);
		m3.set(1, 1, fSumYY);
		m3.set(1, 2, fSumYZ);

		m3.set(2, 0, fSumXZ);
		m3.set(2, 1, fSumYZ);
		m3.set(2, 2, fSumZZ);

		try {
			structure = new EigenStructure(m3);

			// lets sort the eigen structure
			VectorN values = structure.getEigenvalues();
			MatrixN m = structure.getEigenvectors();

			for (int i = 0; i < 3; i++) {
				axes[i] = new Axis();
				axes[i].length = (float) Math.sqrt(values.get(i));
				for (int n = 0; n < 3; n++) {
					axes[i].direction.set(n, (float) m.get(n, i));
				}
				axes[i].direction.normalize();
			}

			for (int i = 0; i < 3; i++)
				if (Float.isNaN(axes[i].length))
					axes[i].length = 0f;

			Arrays.sort(axes, axesComparator);
			return axes;

		} catch (DimensionMismatchException e) {
			e.printStackTrace();
		} catch (EigenStructureException e) {
			e.printStackTrace();
			;// System.out.println( Arrays.asList(points));
		}
		return null;
	}

	public void closeFit(Axis[] axis, List<Vector3> points) {
		for (int z = 0; z < axis.length; z++) {
			float max = Float.NEGATIVE_INFINITY;
			float min = Float.POSITIVE_INFINITY;
			float ds = center.dot(axis[z].direction);
			for (int q = 0; q < points.size(); q++) {
				float d2 = points.get(q).dot(axis[z].direction) - ds;
				if (d2 < min)
					min = d2;
				if (d2 > max)
					max = d2;
			}
			axis[z].positiveLength = max;
			axis[z].negativeLength = min;
		}
	}

	public void medianFit(Axis[] axis, List<Vector3> points) {
		for (int z = 0; z < axis.length; z++) {
			
			List<Float>fp = new ArrayList<Float>();
			List<Float>fn = new ArrayList<Float>();
			
			float max = Float.NEGATIVE_INFINITY;
			float min = Float.POSITIVE_INFINITY;
			float ds = center.dot(axis[z].direction);
			for (int q = 0; q < points.size(); q++) {
				float d2 = points.get(q).dot(axis[z].direction) - ds;
				if (d2>0)
					fp.add(d2);
				else 
					fn.add(d2);
			}
			
			Collections.sort(fp);
			Collections.sort(fn);
			
			axis[z].positiveLength = fp.get(fp.size()/2);
			axis[z].negativeLength = fn.get(fn.size()/2);
			
//			if (fn.size()>fp.size())
//			{
//				float t = axis[z].positiveLength;
//				axis[z].positiveLength= axis[z].negativeLength;
//				axis[z].negativeLength= t;
//			}
			
		}
	}

	Comparator<Axis> axesComparator = new Comparator<Axis>() {
		public int compare(Axis o1, Axis o2) {

			if (((Axis) o1).length > ((Axis) o2).length)
				return 1;
			return -1;
		}
	};

	public Collection<Vector3> extremalPoints(Axis[] along, List<Vector3> p) {
		HashSet<Vector3> s = new HashSet<Vector3>();

		float[] max = new float[along.length];
		float[] min = new float[along.length];
		Vector3[] minis = new Vector3[along.length];
		Vector3[] maxis = new Vector3[along.length];
		for (int i = 0; i < max.length; i++) {
			max[i] = Float.NEGATIVE_INFINITY;
			min[i] = Float.POSITIVE_INFINITY;
		}

		Vector3 t = new Vector3();
		for (int j = 0; j < p.size(); j++) {
			Vector3 q = t.setValue(p.get(j)).sub(center);
			for (int i = 0; i < along.length; i++) {
				float dd = q.dot(along[i].direction);
				if (dd < min[i]) {
					min[i] = dd;
					minis[i] = p.get(j);
				}
				if (dd > max[i]) {
					max[i] = dd;
					maxis[i] = p.get(j);
				}
			}

		}

		for (int i = 0; i < maxis.length; i++) {
			if (maxis[i] != null)
				s.add(maxis[i]);
			if (minis[i] != null)
				s.add(minis[i]);
		}

		return s;
	}

	Vector3 tmp = new Vector3();

	public void axesRescale(Vector3 from, Vector3 to, float scale1, float scale2, float scale3) {
		tmp.sub(from, center);
		float d1 = tmp.dot(axes[0].direction) * scale1 * axes[0].length;
		float d2 = tmp.dot(axes[1].direction) * scale2 * axes[1].length;
		float d3 = tmp.dot(axes[2].direction) * scale3 * axes[2].length;

		to.zero();
		Vector3.add(axes[0].direction, d1, to, to);
		Vector3.add(axes[1].direction, d2, to, to);
		Vector3.add(axes[2].direction, d3, to, to);
	}

}