package field.math.util;

import java.util.ArrayList;
import java.util.List;

import field.math.linalg.Vector3;
import field.math.util.FitBox.Axis;
import field.namespace.generic.Generics.Triple;

public class PlaneRansac {

	public class Model {
		public Vector3 offset;
		public Vector3 up;
		public FitBox f;
		public Axis[] axes;
		public List<Vector3> points;
		float area = 0;

		public Model(Triple<Vector3, Vector3, Vector3> t) {
			offset = new Vector3().add(t.left).add(t.right).add(t.middle).scale(1 / 3f);
			up = new Vector3().cross(new Vector3(t.left).sub(offset), new Vector3(t.right).sub(offset)).normalize();
		}

		public float distanceTo(Vector3 v) {
			float d = new Vector3(v).sub(offset).dot(up);

			if (offset.distanceFrom(v) < 1f)
				return Math.abs(d);
			else
				return Math.abs(d) +1;
		}

		public Model(List<Vector3> v, Model m) {
			f = new FitBox();
			axes = f.fit(v.toArray(new Vector3[v.size()]));

			float d0 = distance(axes[0].direction.normalize(), m.up);
			float d1 = distance(axes[1].direction.normalize(), m.up);
			float d2 = distance(axes[2].direction.normalize(), m.up);

			if (d1 < d0 && d1 < d2) {
				Axis a0 = axes[0];
				axes[0] = axes[1];
				axes[1] = a0;
			} else if (d2 < d0 && d2 < d1) {
				Axis a0 = axes[0];
				axes[0] = axes[2];
				axes[2] = a0;
			}

			points = v;

			offset = f.center;
			up = f.axes[0].direction.normalize();
			f.closeFit(axes, v);

			area = (axes[2].positiveLength - axes[2].negativeLength) * (axes[1].positiveLength - axes[2].negativeLength);

		}

		private float distance(Vector3 a, Vector3 b) {
			return Math.min(a.distanceFrom(b), new Vector3(a).scale(-1).distanceFrom(b));
		}

		@Override
		public String toString() {
			return offset + "//" + up;
		}

	}

	public float best;
	public Model bestModel;
	public int maxMembership;

	public PlaneRansac(List<Vector3> points, float inside, int numIterations, int minMembership) {
		best = Float.POSITIVE_INFINITY;
		bestModel = null;

		if (points.size() < 3)
			return;
		
		maxMembership = 0;
			
		FitBox f = new FitBox();
		Axis[] axes = f.fit(points.toArray(new Vector3[points.size()]));

		f.closeFit(axes, points);

		float size = Math.max(axes[0].positiveLength, axes[0].negativeLength);
		Math.max(size, Math.max(axes[1].positiveLength, axes[1].negativeLength));
		Math.max(size, Math.max(axes[2].positiveLength, axes[2].negativeLength));

		;//System.out.println(" cloud size is <" + size + ">");

		for (int i = 0; i < numIterations; i++) {
			Triple<Vector3, Vector3, Vector3> model = randomModel(points);
			Model m = new Model(model);

			List<Vector3> insidePoints = new ArrayList<Vector3>();

			int num = 0;
			for (Vector3 v : points) {

				// ;//System.out.println(" distances from <"+m+"> to <"+v+"> is <"+m.distanceTo(v)+">");

				if (m.distanceTo(v) < inside) {
					insidePoints.add(v);
					num++;
				}
			}
			if (num < minMembership) {
				//;//System.out.println(" membership is only <" + num + ">");
				if (num>maxMembership)
					maxMembership = num;
				continue;
			}

			Model m2 = new Model(insidePoints, m);
			;//System.out.println(m + " " + m2 + "   m1/m2");

			float d = 0;
			int num2 = 0;
			for (Vector3 v : points) {

				// ;//System.out.println(" distances from <" + m +
				// "> to <" + v + "> is <" + m.distanceTo(v) +
				// ">");

				float distanceTo = m2.distanceTo(v);
				if (distanceTo < inside) {
					d += distanceTo;
					num2++;
				}
			}

			float areaDensity = num2 / m2.area;
			//;//System.out.println(" area density is <" + areaDensity + ">");

			if (num2 < minMembership) {
				//;//System.out.println(" membership2 is only <" + num2 + ">");
				continue;
			}

			float distance = d / num2;

			if (distance < best) {
				best = distance;
				bestModel = m2;
			}
		}
	}

	private Triple<Vector3, Vector3, Vector3> randomModel(List<Vector3> points) {
		int i1 = 0;
		int i2 = 0;
		int i3 = 0;

		while (i1 == i2 || i2 == i3 || i1 == i3) {
			i1 = (int) (Math.random() * points.size());
			i2 = (int) (Math.random() * points.size());
			i3 = (int) (Math.random() * points.size());
		}

		return new Triple<Vector3, Vector3, Vector3>(points.get(i1), points.get(i2), points.get(i3));
	}

}
