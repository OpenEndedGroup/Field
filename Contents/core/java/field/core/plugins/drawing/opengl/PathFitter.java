package field.core.plugins.drawing.opengl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import field.core.plugins.drawing.opengl.CachedLine;
import field.math.linalg.Vector2;
import field.namespace.generic.Generics.Pair;

public class PathFitter {

	private List<Vector2> points;
	private final float error;

	public PathFitter(List<Vector2> points, float error) {
		this.points = points;
		this.error = error;
	}

	public class Curve {
		public Curve(Vector2 pt1, Vector2 t1, Vector2 t2, Vector2 pt2) {
			this.a = new Vector2(pt1);
			this.b = new Vector2(pt2);
			this.t1 = new Vector2(t1);
			this.t2 = new Vector2(t2);
		}

		Vector2 a, t1, t2, b;

		public Vector2 get(int i) {
			switch (i) {
			case 0:
				return this.a;
			case 1:
				return this.t1;
			case 2:
				return this.t2;
			case 3:
				return this.b;
			}

			throw new ArrayIndexOutOfBoundsException(i);
		}

		public List<Vector2> toList() {
			return Arrays.asList(a, t1, t2, b);
		}
	}

	public void fit() {
		
		;//System.out.println(" fit called on <"+points+">");
		
		fitCubic(0, points.size() - 1, new Vector2(points.get(1)).sub(points.get(0)).normalize(), new Vector2(points.get(points.size() - 2)).sub(points.get(points.size() - 1)).normalize());
	}

	public CachedLine toCachedLine()
	{
		CachedLine cl = new CachedLine();
		cl.getInput().moveTo(curves.get(0).a.x, curves.get(0).a.y);
		for(Curve c : curves)
		{
			cl.getInput().cubicTo(c.t1.x, c.t1.y, c.t2.x, c.t2.y, c.b.x, c.b.y);
		}
		return cl;
	}
	
	public void fitCubic(int first, int last, Vector2 tan1, Vector2 tan2) {
		
		{
			float dist = points.get(last).distanceFrom(points.get(first))/3;
			;//System.out.println(" fit cubic :"+dist+" "+Arrays.asList(points.get(first), new Vector2(points.get(first)).add(new Vector2(tan1).normalize().scale(dist)), new Vector2(points.get(last)).add(new Vector2(tan2).normalize().scale(dist)), points.get(last)));
		}
		
		if (last - first == 1) {
			;//System.out.println("adding curve as 0 length ");
			float dist = points.get(last).distanceFrom(points.get(first))/3;
			addCurve(points.get(first), new Vector2(points.get(first)).add(new Vector2(tan1).normalize().scale(dist)), new Vector2(points.get(last)).add(new Vector2(tan2).normalize().scale(dist)), points.get(last));
			return;
		}

		float[] uprime = chordLengthParameterize(last, first);
		float maxError = Math.max(this.error, this.error * this.error);
		int split = 0;

		for (int i = 0; i < 4; i++) {
			Curve curve = this.generateBezier(first, last, uprime, tan1, tan2);
			Pair<Float, Integer> max = this.findMaxError(first, last, curve, uprime);

			if (max.left < maxError) {
				;//System.out.println(" adding curve as correct enough ");
				this.addCurve(curve.a, curve.t1, curve.t2, curve.b);
				return;
			}
			split = max.right;
			if (max.left > maxError)
				break;
			this.reparamterize(first, last, uprime, curve);
			maxError = max.left;
		}

		Vector2 V1 = new Vector2(this.points.get(split - 1)).sub(this.points.get(split));
		Vector2 V2 = new Vector2(this.points.get(split)).sub(this.points.get(split + 1));
		Vector2 tanCenter = new Vector2(V1).add(V2).normalize();
		this.fitCubic(first, split, new Vector2(tan1), tanCenter);
		this.fitCubic(split, last, new Vector2(tanCenter).negate(), new Vector2(tan2));
	}
	
	
	

	private Pair<Float, Integer> findMaxError(int first, int last, Curve curve, float[] u) {
		int index = (int) Math.floor((last - first + 1) / 2);
		float maxDist = 0;
		for (int i = first + 1; i < last; i++) {
			Vector2 P = this.evaluate(3, curve.toList(), u[i - first]);
			
			;//System.out.println(" evaluate <"+curve.toList()+" @ :"+u[i-first]+" => "+P);
			
			Vector2 v = new Vector2(P).sub(this.points.get(i));
			float dist = v.x * v.x + v.y * v.y;
			if (dist >= maxDist) {
				maxDist = dist;
				index = i;
			}
		}
		return new Pair<Float, Integer>(maxDist, index);
	}

	private float[] chordLengthParameterize(int last, int first) {
		float[] u = new float[last - first + 1];

		for (int i = first + 1; i <= last; i++) {
			u[i - first] = u[i - first - 1] + this.points.get(i).distanceFrom(this.points.get(i - 1));
		}
		for (int i = 1, m = last - first; i <= m; i++) {
			u[i] /= u[m];
		}
		return u;
	}

	private void reparamterize(int first, int last, float[] u, Curve curve) {
		for (int i = first; i <= last; i++) {
			u[i - first] = this.findRoot(curve, this.points.get(i), u[i - first]);
		}
	}

	private float findRoot(Curve curve, Vector2 point, float u) {

		List<Vector2> curve1 = new ArrayList<Vector2>();
		List<Vector2> curve2 = new ArrayList<Vector2>();
		for (int i = 0; i < 3; i++) {
			curve1.add(new Vector2(curve.get(i + 1)).sub(curve.get(i)).scale(3));
		}

		for (int i = 0; i < 2; i++) {
			curve2.add(new Vector2(curve.get(i + 1)).sub(curve.get(i)).scale(2));
		}

		Vector2 pt = this.evaluate(3, curve.toList(), u);
		Vector2 pt1 = this.evaluate(2, curve1, u);
		Vector2 pt2 = this.evaluate(1, curve2, u);

		Vector2 diff = new Vector2(pt).sub(point);
		float df = pt1.dot(pt1) + diff.dot(pt2);
		if (Math.abs(df) < 1e-10)
			return u;
		return u - diff.dot(pt1) / df;
	}

	private Vector2 evaluate(int degree, List<Vector2> list, float t) {
		ArrayList<Vector2> tmp = new ArrayList<Vector2>(list);
		for (int i = 1; i <= degree; i++) {
			for (int j = 0; j <= degree - 1; j++) {
				tmp.set(j, new Vector2(tmp.get(j)).scale(1 - t).add(new Vector2(tmp.get(j + 1)).scale(t)));
			}
		}
		return tmp.get(0);
	}

	private Curve generateBezier(int first, int last, float[] uprime, Vector2 tan1, Vector2 tan2) {
		float epsilon = 1e-10f;
		Vector2 pt1 = points.get(first);
		Vector2 pt2 = points.get(last);

		float[][] C = { { 0, 0 }, { 0, 0 } };
		float[] X = { 0, 0 };

		for (int i = 0, l = last - first + 1; i < l; i++) {
			float u = uprime[i];
			float t = 1 - u;
			float b = 3 * u * t;
			float b0 = t * t * t;
			float b1 = b * t;
			float b2 = b * u;
			float b3 = u * u * u;
			Vector2 a1 = new Vector2(tan1).normalize().scale(b1);
			Vector2 a2 = new Vector2(tan2).normalize().scale(b2);
			Vector2 tmp = new Vector2(this.points.get(first + i)).sub(new Vector2(pt1).scale(b0 + b1)).sub(new Vector2(pt2).scale(b2 + b3));
			C[0][0] += a1.dot(a1);
			C[0][1] += a1.dot(a2);
			C[1][0] = C[0][1];
			C[1][1] += a2.dot(a2);
			X[0] += a1.dot(tmp);
			X[1] += a2.dot(tmp);
		}

		float detC0C1 = C[0][0] * C[1][1] - C[1][0] * C[0][1], alpha1, alpha2;
		if (Math.abs(detC0C1) > epsilon) {
			float detC0X = C[0][0] * X[1] - C[1][0] * X[0], detXC1 = X[0] * C[1][1] - X[1] * C[0][1];
			alpha1 = detXC1 / detC0C1;
			alpha2 = detC0X / detC0C1;
		} else {
			float c0 = C[0][0] + C[0][1], c1 = C[1][0] + C[1][1];
			if (Math.abs(c0) > epsilon) {
				alpha1 = alpha2 = X[0] / c0;
			} else if (Math.abs(c0) > epsilon) {
				alpha1 = alpha2 = X[1] / c1;
			} else {
				alpha1 = alpha2 = 0.0f;
			}
		}

		float segLength = pt2.distanceFrom(pt1);
		epsilon *= segLength;
		if (alpha1 < epsilon || alpha2 < epsilon) {
			alpha1 = alpha2 = segLength / 3;
		}

		;//System.out.println(" generate bez <"+alpha1+" "+alpha2+">");
		
		return new Curve(pt1, new Vector2(pt1).add(new Vector2(tan1).normalize().scale(alpha1)), new Vector2(pt2).add(new Vector2(tan2).normalize().scale(alpha2)), pt2);

	}

	List<Curve> curves = new ArrayList<Curve>();

	private void addCurve(Vector2 a, Vector2 ta, Vector2 tb, Vector2 b) {
		;//System.out.println(" add curve :"+a+" "+ta+" "+tb+" "+b);
		curves.add(new Curve(a, ta, tb, b));
	}

}
