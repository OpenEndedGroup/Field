package field.core.plugins.drawing.opengl;

import java.awt.BasicStroke;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.antlr.op.Eq;

import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.plugins.drawing.tweak.Visitors;
import field.core.plugins.drawing.tweak.TweakSplineUI.SubSelection;
import field.core.plugins.drawing.tweak.Visitors.BaseFilter;
import field.graphics.core.BasicCamera.Projector;
import field.math.BaseMath;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.IntersectionPrimatives;
import field.math.linalg.Quaternion;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.IntersectionPrimatives.LinePointIntersectionInfo;
import field.math.util.Brent;
import field.math.util.FitBox;
import field.math.util.Brent.F;
import field.math.util.FitBox.Axis;
import field.namespace.generic.Generics.Pair;
import field.util.Dict.Prop;

public class LineUtils {

	static public class AllDistanceMinima {
		public class Minimum {
			float min = Float.POSITIVE_INFINITY;

			int minAtIndex = 0;

			float minT = 0;

			public float getMinDistance() {
				return min;
			}

			public int getMinIndex() {
				return minAtIndex;
			}

			public float getMinParameter() {
				return minT;
			}

			public Vector2 getMinPoint() {
				CachedLineCursor cc = new CachedLineCursor(on, minAtIndex);
				if (cc.nextIsCubic()) {
					cc.nextCubicFrame(a, c1, c2, b);
					Vector2 out = new Vector2();
					evaluateCubicFrame(a, c1, c2, b, getMinParameter(), out);
					return out;
				} else {
					cc.nextLinearFrame(a, b);
					Vector2 out = new Vector2();
					out.interpolate(a, b, getMinParameter());
					return out;
				}
			}

			@Override
			public String toString() {
				return "minimum @ " + minAtIndex + " " + min + " " + minT + "  " + getMinPoint();
			}
		}

		public static double ptSegDistSqT(double x1, double y1, double x2, double y2, double px, double py) {
			x2 -= x1;
			y2 -= y1;
			px -= x1;
			py -= y1;
			double dotprod = (px * x2 + py * y2) / (x2 * x2 + y2 * y2);

			if (dotprod < 0)
				dotprod = 0;
			if (dotprod > 1)
				dotprod = 1;
			if (Double.isNaN(dotprod))
				dotprod = 0;
			if (Double.isInfinite(dotprod))
				dotprod = 0;

			return Math.sqrt(dotprod);
		}

		private final F cubicF;

		private final CachedLine on;

		protected Brent brent;

		Vector2 a = new Vector2(), c1 = new Vector2(), c2 = new Vector2(), b = new Vector2(), c21 = new Vector2(), c12 = new Vector2(), m = new Vector2(), t = new Vector2();

		Vector2 oc1 = new Vector2();

		Vector2 oc2 = new Vector2();

		Vector2 oa = new Vector2();

		Vector2 ob = new Vector2();

		List<Minimum> minima = new ArrayList<Minimum>();

		public AllDistanceMinima(CachedLine line, final Vector2 point) {
			this.on = line;
			brent = new Brent();
			cubicF = new Brent.F() {
				public double f(double x) {

					x = 1 - x;
					double ax = a.x * x * x * x + 3 * c1.x * x * x * (1 - x) + 3 * c2.x * x * (1 - x) * (1 - x) + b.x * (1 - x) * (1 - x) * (1 - x);
					double ay = a.y * x * x * x + 3 * c1.y * x * x * (1 - x) + 3 * c2.y * x * (1 - x) * (1 - x) + b.y * (1 - x) * (1 - x) * (1 - x);

					double d = (point.x - ax) * (point.x - ax) + (point.y - ay) * (point.y - ay);

					return d;
				}
			};

			CachedLineCursor cc = new CachedLineCursor(line);
			while (cc.hasNextSegment()) {
				cc.next();
				if (cc.nextIsCubic()) {
					if (cc.nextCubicFrame(oa, oc1, oc2, ob)) {

						c1.setValue(oc1);
						c2.setValue(oc2);
						b.setValue(ob);
						a.setValue(oa);

						splitCubicFrame(a, c1, c2, b, 0.5f, c12, m, c21, t);

						oc2.setValue(c2);
						c2.setValue(c12);
						b.setValue(m);

						{

							double x = brent.fmin(0, 1, cubicF, 0.1f / (1 + a.distanceFrom(b)));
							double d = cubicF.f(x);
							double d0 = cubicF.f(Math.max(0, x - 1e-2));
							double d1 = cubicF.f(Math.min(1, x + 1e-2));
							if (d <= d0 && d <= d1)
								newMinimum(x / 2, d, cc.getCurrentIndex());
						}
						b.setValue(ob);
						a.setValue(m);
						c2.setValue(oc2);
						c1.setValue(c21);
						{

							double x = brent.fmin(0, 1, cubicF, 0.1f / (1 + a.distanceFrom(b)));
							double d = cubicF.f(x);
							double d0 = cubicF.f(Math.max(0, x - 1e-2));
							double d1 = cubicF.f(Math.min(1, x + 1e-2));
							if (d <= d0 && d <= d1)
								newMinimum(0.5 + x / 2, d, cc.getCurrentIndex());
						}
					}
				} else {
					if (cc.nextLinearFrame(a, b)) {
						double d = Line2D.ptSegDistSq(a.x, a.y, b.x, b.y, point.x, point.y);
						newMinimum(ptSegDistSqT(a.x, a.y, b.x, b.y, point.x, point.y), d, cc.getCurrentIndex());
					}
				}
			}

			System.err.println(" minima before filtering :");
			for (Minimum m : minima) {
				System.err.println("    " + m);
			}

			if (minima.size() > 1)
				// now filter
				// the minima
				for (int i = 0; i < minima.size(); i++) {
					if (minima.get(i).minT > (1 - 1e-2)) {
						if (minima.get((i + 1) % minima.size()).minT < 1e-2) {
							minima.get(i).min = Math.min(minima.get(i).min, minima.get((i + 1) % minima.size()).min);
							minima.remove(i + 1);
							minima.get(i).minT = 1;
							continue;
						}
					}

					// if (minima.get(i).minT < 1e-2 ||
					// minima.get(i).minT > 1-1e-2)
					// {
					// minima.remove(i);
					// i--;
					// continue;
					// }

					// if (minima.get(i).minAtIndex ==
					// minima.get((i + 1) %
					// minima.size()).minAtIndex) {
					// if (minima.get(i).min < minima.get((i
					// + 1) % minima.size()).min) {
					// minima.remove((i + 1) %
					// minima.size());
					// continue;
					// } else {
					// minima.remove((i) % minima.size());
					// continue;
					// }
					// }
				}

			System.err.println(" minima after filtering :");
			for (Minimum m : minima) {
				System.err.println("    " + m);
			}

		}

		private void newMinimum(double x, double d, int currentIndex) {
			Minimum m = new Minimum();
			m.min = (float) d;
			m.minT = (float) x;
			m.minAtIndex = currentIndex;
			minima.add(m);
		}
	}

	static public class ClosestEvent {
		private final CachedLine target;

		private float d;

		int closetAt = 0;

		public ClosestEvent(CachedLine target) {
			this.target = target;
		}

		public CachedLine.Event closestTo(Vector2 v) {
			d = Float.POSITIVE_INFINITY;
			Vector2 v2 = new Vector2();
			Event found = null;
			closetAt = 0;
			int n = 0;
			for (Event e : target.events) {
				if (e.hasDestination()) {
					e.getDestination(v2);
					float d2 = v2.distanceFrom(v);
					if (d2 < d) {
						d = d2;
						found = e;
						closetAt = n;
					}
				}
				n++;
			}
			return found;
		}

		public int returnClosestIndex() {
			return closetAt;
		}

		public float returnLastDistance() {
			return d;
		}
	}

	static public class ClosestPointToSpline {
		public static double ptSegDistSqT(double x1, double y1, double x2, double y2, double px, double py) {
			x2 -= x1;
			y2 -= y1;

			px -= x1;
			py -= y1;

			double dotprod = (px * x2 + py * y2) / (x2 * x2 + y2 * y2);

			if (dotprod < 0)
				dotprod = 0;
			if (dotprod > 1)
				dotprod = 1;
			if (Double.isNaN(dotprod))
				dotprod = 0;
			if (Double.isInfinite(dotprod))
				dotprod = 0;

			return /* Math.sqrt */(dotprod);
		}

		private final F cubicF;

		private final CachedLine on;

		protected Brent brent;

		Vector2 a = new Vector2(), c1 = new Vector2(), c2 = new Vector2(), b = new Vector2(), c21 = new Vector2(), c12 = new Vector2(), m = new Vector2(), t = new Vector2();

		Vector2 oc1 = new Vector2();

		Vector2 oc2 = new Vector2();

		Vector2 oa = new Vector2();

		Vector2 ob = new Vector2();

		float min = Float.POSITIVE_INFINITY;

		int minAtIndex = 0;

		float minT = 0;

		public ClosestPointToSpline(CachedLine line, final Vector2 point) {
			this.on = line;
			brent = new Brent();
			cubicF = new Brent.F() {
				public double f(double x) {

					x = 1 - x;
					double ax = a.x * x * x * x + 3 * c1.x * x * x * (1 - x) + 3 * c2.x * x * (1 - x) * (1 - x) + b.x * (1 - x) * (1 - x) * (1 - x);
					double ay = a.y * x * x * x + 3 * c1.y * x * x * (1 - x) + 3 * c2.y * x * (1 - x) * (1 - x) + b.y * (1 - x) * (1 - x) * (1 - x);

					double d = (point.x - ax) * (point.x - ax) + (point.y - ay) * (point.y - ay);
					return d;
				}
			};

			CachedLineCursor cc = new CachedLineCursor(line);
			while (cc.hasNextSegment()) {
				cc.next();
				if (cc.nextIsCubic()) {
					if (cc.nextCubicFrame(oa, oc1, oc2, ob)) {

						c1.setValue(oc1);
						c2.setValue(oc2);
						b.setValue(ob);
						a.setValue(oa);

						splitCubicFrame(a, c1, c2, b, 0.5f, c12, m, c21, t);

						oc2.setValue(c2);
						c2.setValue(c12);
						b.setValue(m);
						{
							double x = brent.fmin(0, 1, cubicF, 0.1f / (1 + a.distanceFrom(b)));
							double d = cubicF.f(x);
							if (d < min) {
								min = (float) d;
								minT = (float) x / 2;
								minAtIndex = cc.getCurrentIndex();
							}
						}

						b.setValue(ob);
						a.setValue(m);
						c2.setValue(oc2);
						c1.setValue(c21);
						{
							double x = brent.fmin(0, 1, cubicF, 0.1f / (1 + a.distanceFrom(b)));
							double d = cubicF.f(x);
							if (d < min) {
								min = (float) d;
								minT = (float) (0.5 + x / 2);
								minAtIndex = cc.getCurrentIndex();
							}
						}
					}
				} else {
					if (cc.nextLinearFrame(a, b)) {

						;// System.out.println(" linear frame is <"
							// + a + " " + b +
							// "> looking for <" +
							// point + ">");

						double d = Line2D.ptSegDistSq(a.x, a.y, b.x, b.y, point.x, point.y);
						if (d < min) {

							;// System.out.println(" distance is <"
								// + d + ">");

							min = (float) d;
							minAtIndex = cc.getCurrentIndex();
							minT = (float) ptSegDistSqT(a.x, a.y, b.x, b.y, point.x, point.y);

							;// System.out.println(" minT is <"
								// + minT +
								// ">");

						}
					}
				}
			}
		}

		public float getMinDistance() {
			return min;
		}

		public int getMinIndex() {
			return minAtIndex;
		}

		public float getMinParameter() {
			return minT;
		}

		public Vector2 getMinPoint() {
			CachedLineCursor cc = new CachedLineCursor(on, minAtIndex);
			if (cc.nextIsCubic()) {
				cc.nextCubicFrame(a, c1, c2, b);
				Vector2 out = new Vector2();
				evaluateCubicFrame(a, c1, c2, b, getMinParameter(), out);
				return out;
			} else {
				cc.nextLinearFrame(a, b);
				Vector2 out = new Vector2();
				out.interpolate(a, b, getMinParameter());
				return out;
			}
		}

	}

	static public class ClosestPointToSpline3 {
		public static double ptSegDistSqT(double x1, double y1, double z1, double x2, double y2, double z2, double px, double py, double pz) {
			x2 -= x1;
			y2 -= y1;
			z2 -= z1;
			px -= x1;
			py -= y1;
			pz -= z1;
			double dotprod = (px * x2 + py * y2 + pz * z2) / (x2 * x2 + y2 * y2 + z2 * z2);

			if (dotprod < 0)
				dotprod = 0;
			if (dotprod > 1)
				dotprod = 1;
			if (Double.isNaN(dotprod))
				dotprod = 0;
			if (Double.isInfinite(dotprod))
				dotprod = 0;

			return Math.sqrt(dotprod);
		}

		public static double ptSegDistSq3(double x1, double y1, double z1, double x2, double y2, double z2, double px, double py, double pz) {
			x2 -= x1;
			y2 -= y1;
			z2 -= z1;
			px -= x1;
			py -= y1;
			pz -= z1;
			double dotprod = px * x2 + py * y2 + pz * z2;
			double projlenSq;
			if (dotprod <= 0.0) {
				projlenSq = 0.0;
			} else {
				px = x2 - px;
				py = y2 - py;
				pz = z2 - pz;
				dotprod = px * x2 + py * y2 + pz * z2;
				if (dotprod <= 0.0) {
					projlenSq = 0.0;
				} else {
					projlenSq = dotprod * dotprod / (x2 * x2 + y2 * y2 + z2 * z2);
				}
			}
			double lenSq = px * px + py * py + pz * pz - projlenSq;
			if (lenSq < 0) {
				lenSq = 0;
			}
			return lenSq;
		}

		private final F cubicF;

		private final CachedLine on;

		protected Brent brent;

		Vector3 a = new Vector3(), c1 = new Vector3(), c2 = new Vector3(), b = new Vector3(), c21 = new Vector3(), c12 = new Vector3(), m = new Vector3(), t = new Vector3();

		Vector3 oc1 = new Vector3();

		Vector3 oc2 = new Vector3();

		Vector3 oa = new Vector3();

		Vector3 ob = new Vector3();

		float min = Float.POSITIVE_INFINITY;

		int minAtIndex = 0;

		float minT = 0;

		public ClosestPointToSpline3(CachedLine line, final Vector3 point) {
			this.on = line;
			brent = new Brent();
			cubicF = new Brent.F() {
				public double f(double x) {

					x = 1 - x;
					double ax = a.x * x * x * x + 3 * c1.x * x * x * (1 - x) + 3 * c2.x * x * (1 - x) * (1 - x) + b.x * (1 - x) * (1 - x) * (1 - x);
					double ay = a.y * x * x * x + 3 * c1.y * x * x * (1 - x) + 3 * c2.y * x * (1 - x) * (1 - x) + b.y * (1 - x) * (1 - x) * (1 - x);
					double az = a.z * x * x * x + 3 * c1.z * x * x * (1 - x) + 3 * c2.z * x * (1 - x) * (1 - x) + b.z * (1 - x) * (1 - x) * (1 - x);

					double d = (point.x - ax) * (point.x - ax) + (point.y - ay) * (point.y - ay) + (point.z - az) * (point.z - az);
					;
					return d;
				}
			};

			CachedLineCursor cc = new CachedLineCursor(line);
			while (cc.hasNextSegment()) {
				cc.next();
				if (cc.nextIsCubic()) {
					if (cc.nextCubicFrame3(oa, oc1, oc2, ob)) {

						// ;//System.out.println(" next cubic frame3 "+oa+" "+oc1+" "+oc2+" "+ob);

						c1.setValue(oc1);
						c2.setValue(oc2);
						b.setValue(ob);
						a.setValue(oa);

						splitCubicFrame3(a, c1, c2, b, 0.5f, c12, m, c21, t);

						oc2.setValue(c2);
						c2.setValue(c12);
						b.setValue(m);
						{
							double x = brent.fmin(0, 1, cubicF, 0.1f / (1 + a.distanceFrom(b)));
							double d = cubicF.f(x);
							if (d < min) {
								min = (float) d;
								minT = (float) x / 2;
								minAtIndex = cc.getCurrentIndex();
							}
						}

						b.setValue(ob);
						a.setValue(m);
						c2.setValue(oc2);
						c1.setValue(c21);
						{
							double x = brent.fmin(0, 1, cubicF, 0.1f / (1 + a.distanceFrom(b)));
							double d = cubicF.f(x);
							if (d < min) {
								min = (float) d;
								minT = (float) (0.5 + x / 2);
								minAtIndex = cc.getCurrentIndex();
							}
						}
					}
				} else {
					if (cc.nextLinearFrame3(a, b)) {

						// ;//System.out.println(" next line frame3 "+a+" "+b);

						// TODO:
						double d = ptSegDistSq3(a.x, a.y, a.z, b.x, b.y, b.z, point.x, point.y, point.z);
						if (d < min) {
							min = (float) d;
							minAtIndex = cc.getCurrentIndex();
							minT = (float) ptSegDistSqT(a.x, a.y, a.z, b.x, b.y, b.z, point.x, point.y, point.z);
						}
					}
				}
			}
		}

		public float getMinDistance() {
			return min;
		}

		public int getMinIndex() {
			return minAtIndex;
		}

		public float getMinParameter() {
			return minT;
		}

		public Vector3 getMinPoint() {
			CachedLineCursor cc = new CachedLineCursor(on, minAtIndex);
			if (cc.nextIsCubic()) {
				cc.nextCubicFrame3(a, c1, c2, b);
				Vector3 out = new Vector3();
				evaluateCubicFrame3(a, c1, c2, b, getMinParameter(), out);
				return out;
			} else {
				cc.nextLinearFrame3(a, b);
				Vector3 out = new Vector3();
				out.interpolate(a, b, getMinParameter());
				return out;
			}
		}

	}

	static public Vector2 evaluateCubicFrame(Vector2 a, Vector2 c1, Vector2 c2, Vector2 b, float alpha, Vector2 out) {
		float oma = 1 - alpha;
		float oma2 = oma * oma;
		float oma3 = oma2 * oma;
		float alpha2 = alpha * alpha;
		float alpha3 = alpha2 * alpha;

		out.x = a.x * oma3 + 3 * c1.x * alpha * oma2 + 3 * c2.x * alpha2 * oma + b.x * alpha3;
		out.y = a.y * oma3 + 3 * c1.y * alpha * oma2 + 3 * c2.y * alpha2 * oma + b.y * alpha3;

		return out;
	}

	static public float evaluateCubicFrame(float a, float c1, float c2, float b, float alpha) {
		float oma = 1 - alpha;
		float oma2 = oma * oma;
		float oma3 = oma2 * oma;
		float alpha2 = alpha * alpha;
		float alpha3 = alpha2 * alpha;

		return a * oma3 + 3 * c1 * alpha * oma2 + 3 * c2 * alpha2 * oma + b * alpha3;
	}

	static public Vector2 evaluateDCubicFrame(Vector2 a, Vector2 c1, Vector2 c2, Vector2 b, float alpha, Vector2 out) {

		out.x = -3 * c1.x * alpha * (2 - 2 * alpha) + 6 * c2.x * alpha * (1 - alpha) - 3 * a.x * (1 - alpha) * (1 - alpha) - 3 * c2.x * alpha * alpha + 3 * b.x * alpha * alpha + 3 * c1.x * (1 - alpha) * (1 - alpha);
		out.y = -3 * c1.y * alpha * (2 - 2 * alpha) + 6 * c2.y * alpha * (1 - alpha) - 3 * a.y * (1 - alpha) * (1 - alpha) - 3 * c2.y * alpha * alpha + 3 * b.y * alpha * alpha + 3 * c1.y * (1 - alpha) * (1 - alpha);

		return out;
	}

	static public Vector3 evaluateDCubicFrame3(Vector3 a, Vector3 c1, Vector3 c2, Vector3 b, float alpha, Vector3 out) {

		out.x = -3 * c1.x * alpha * (2 - 2 * alpha) + 6 * c2.x * alpha * (1 - alpha) - 3 * a.x * (1 - alpha) * (1 - alpha) - 3 * c2.x * alpha * alpha + 3 * b.x * alpha * alpha + 3 * c1.x * (1 - alpha) * (1 - alpha);
		out.y = -3 * c1.y * alpha * (2 - 2 * alpha) + 6 * c2.y * alpha * (1 - alpha) - 3 * a.y * (1 - alpha) * (1 - alpha) - 3 * c2.y * alpha * alpha + 3 * b.y * alpha * alpha + 3 * c1.y * (1 - alpha) * (1 - alpha);
		out.z = -3 * c1.z * alpha * (2 - 2 * alpha) + 6 * c2.z * alpha * (1 - alpha) - 3 * a.z * (1 - alpha) * (1 - alpha) - 3 * c2.z * alpha * alpha + 3 * b.z * alpha * alpha + 3 * c1.z * (1 - alpha) * (1 - alpha);

		return out;
	}

	static public Vector2 evaluateD2CubicFrame(Vector2 a, Vector2 c1, Vector2 c2, Vector2 b, float alpha, Vector2 out) {

		out.x = -12 * c2.x * alpha - 6 * c1.x * (2 - 2 * alpha) + 3 * a.x * (2 - 2 * alpha) + 6 * b.x * alpha + 6 * c1.x * alpha + 6 * c2.x * (1 - alpha);
		out.y = -12 * c2.y * alpha - 6 * c1.y * (2 - 2 * alpha) + 3 * a.y * (2 - 2 * alpha) + 6 * b.y * alpha + 6 * c1.y * alpha + 6 * c2.y * (1 - alpha);

		return out;
	}

	static public Vector3 evaluateD2CubicFrame3(Vector3 a, Vector3 c1, Vector3 c2, Vector3 b, float alpha, Vector3 out) {

		out.x = -12 * c2.x * alpha - 6 * c1.x * (2 - 2 * alpha) + 3 * a.x * (2 - 2 * alpha) + 6 * b.x * alpha + 6 * c1.x * alpha + 6 * c2.x * (1 - alpha);
		out.y = -12 * c2.y * alpha - 6 * c1.y * (2 - 2 * alpha) + 3 * a.y * (2 - 2 * alpha) + 6 * b.y * alpha + 6 * c1.y * alpha + 6 * c2.y * (1 - alpha);
		out.z = -12 * c2.z * alpha - 6 * c1.z * (2 - 2 * alpha) + 3 * a.z * (2 - 2 * alpha) + 6 * b.z * alpha + 6 * c1.z * alpha + 6 * c2.z * (1 - alpha);

		return out;
	}

	static public Vector3 evaluateCubicFrame3(Vector3 a, Vector3 c1, Vector3 c2, Vector3 b, float alpha, Vector3 out) {
		float oma = 1 - alpha;
		float oma2 = oma * oma;
		float oma3 = oma2 * oma;
		float alpha2 = alpha * alpha;
		float alpha3 = alpha2 * alpha;

		out.x = a.x * oma3 + 3 * c1.x * alpha * oma2 + 3 * c2.x * alpha2 * oma + b.x * alpha3;
		out.y = a.y * oma3 + 3 * c1.y * alpha * oma2 + 3 * c2.y * alpha2 * oma + b.y * alpha3;
		out.z = a.z * oma3 + 3 * c1.z * alpha * oma2 + 3 * c2.z * alpha2 * oma + b.z * alpha3;

		return out;
	}

	public static Vector2[] fastBounds(CachedLine c) {
		Vector2[] minMax = new Vector2[] { new Vector2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY), new Vector2(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY) };

		boolean touched = false;
		for (int i = 0; i < c.events.size(); i++) {

			if (c.events.get(i).args != null)
				for (int q = 0; q < c.events.get(i).args.length / 2; q++) {
					float tx = ((Number) c.events.get(i).args[q * 2 + 0]).floatValue();
					float ty = ((Number) c.events.get(i).args[q * 2 + 1]).floatValue();

					if (tx < minMax[0].x)
						minMax[0].x = tx;
					if (ty < minMax[0].y)
						minMax[0].y = ty;
					if (tx > minMax[1].x)
						minMax[1].x = tx;
					if (ty > minMax[1].y)
						minMax[1].y = ty;
					touched = true;
				}

		}

		return touched ? minMax : null;

	}

	public static Vector3[] fastBounds3(CachedLine c) {
		Vector3[] minMax = new Vector3[] { new Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY), new Vector3(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY) };

		boolean touched = false;
		for (int i = 0; i < c.events.size(); i++) {

			if (c.events.get(i).args != null) {
				Object tz = c.events.get(i).getAttributes().get(iLinearGraphicsContext.z_v);
				if (tz == null) {
					tz = new Vector3(0, 0, 0);
				} else if (tz instanceof Number) {
					tz = new Vector3(((Number) tz).floatValue(), ((Number) tz).floatValue(), ((Number) tz).floatValue());
				}
				for (int q = 0; q < c.events.get(i).args.length / 2; q++) {

					float tx = ((Number) c.events.get(i).args[q * 2 + 0]).floatValue();
					float ty = ((Number) c.events.get(i).args[q * 2 + 1]).floatValue();

					float ttz = ((Vector3) tz).get(q);

					if (tx < minMax[0].x)
						minMax[0].x = tx;
					if (ty < minMax[0].y)
						minMax[0].y = ty;
					if (ttz < minMax[0].z)
						minMax[0].z = ttz;
					if (tx > minMax[1].x)
						minMax[1].x = tx;
					if (ty > minMax[1].y)
						minMax[1].y = ty;
					if (ttz > minMax[1].z)
						minMax[1].z = ttz;
					touched = true;
				}
			}
		}

		return touched ? minMax : null;

	}

	public static Vector2[] fastBoundsAndMidpoints(CachedLine c) {
		Vector2[] minMax = new Vector2[] { new Vector2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY), new Vector2(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY) };

		boolean touched = false;
		Vector2 a = new Vector2();
		Vector2 c1 = new Vector2();
		Vector2 c2 = new Vector2();
		Vector2 b = new Vector2();
		Vector2 oo = new Vector2();
		for (int i = 0; i < c.events.size(); i++) {

			if (c.events.get(i).args != null) {
				for (int q = 0; q < c.events.get(i).args.length / 2; q++) {
					float tx = ((Number) c.events.get(i).args[q * 2 + 0]).floatValue();
					float ty = ((Number) c.events.get(i).args[q * 2 + 1]).floatValue();

					if (tx < minMax[0].x)
						minMax[0].x = tx;
					if (ty < minMax[0].y)
						minMax[0].y = ty;
					if (tx > minMax[1].x)
						minMax[1].x = tx;
					if (ty > minMax[1].y)
						minMax[1].y = ty;
					touched = true;
				}
				if (c.events.get(i).args.length == 6) {
					Object[] a1 = c.events.get(i - 1).args;
					Object[] a2 = c.events.get(i).args;
					a.x = ((Number) a1[a1.length - 2]).floatValue();
					a.y = ((Number) a1[a1.length - 1]).floatValue();
					b.x = ((Number) a2[a2.length - 2]).floatValue();
					b.y = ((Number) a2[a2.length - 1]).floatValue();
					c1.x = ((Number) a2[a2.length - 6]).floatValue();
					c1.y = ((Number) a2[a2.length - 5]).floatValue();
					c2.x = ((Number) a2[a2.length - 4]).floatValue();
					c2.y = ((Number) a2[a2.length - 3]).floatValue();

					LineUtils.evaluateCubicFrame(a, c1, c2, b, 0.5f, oo);

					float tx = oo.x;
					float ty = oo.y;
					if (tx < minMax[0].x)
						minMax[0].x = tx;
					if (ty < minMax[0].y)
						minMax[0].y = ty;
					if (tx > minMax[1].x)
						minMax[1].x = tx;
					if (ty > minMax[1].y)
						minMax[1].y = ty;

				}
			}
		}

		return touched ? minMax : null;

	}

	public static Vector2[] fastBoundsMoveTos(CachedLine c) {
		Vector2[] minMax = new Vector2[] { new Vector2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY), new Vector2(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY) };
		if (c == null) {
			;// System.out.println(" null c ? ");
			return new Vector2[] { new Vector2(), new Vector2() };
		}

		CachedLineCursor cursor = new CachedLineCursor(c);
		Vector2 t = new Vector2();
		boolean touched = false;
		while (cursor.hasNextSegment()) {
			cursor.next();
			if (cursor.getCurrent().method.equals(iLine_m.moveTo_m)) {
				boolean z = cursor.getAt(t);
				if (z) {
					if (t.x < minMax[0].x)
						minMax[0].x = t.x;
					if (t.y < minMax[0].y)
						minMax[0].y = t.y;
					if (t.x > minMax[1].x)
						minMax[1].x = t.x;
					if (t.y > minMax[1].y)
						minMax[1].y = t.y;
					touched = true;
				}
			}
		}

		return touched ? minMax : null;
	}

	// takes parameter in node.alpha format
	static public Vector2 getPointOnLine(CachedLine of, float dot) {

		if (of.events.size() == 0)
			return null;

		float leftIndex = dot;

		int leftI = (int) leftIndex;
		float leftAlpha = (leftIndex - leftI);

		if (leftI >= of.events.size()) {
			leftI = of.events.size();
			leftAlpha = 0;
		}

		CachedLineCursor cc = new CachedLineCursor(of, leftI);

		if (leftAlpha != 0) {
			if (cc.nextIsCubic()) {
				Vector2 va = new Vector2();
				Vector2 vc1 = new Vector2();
				Vector2 vc2 = new Vector2();
				Vector2 vb = new Vector2();
				cc.nextCubicFrame(va, vc1, vc2, vb);

				return evaluateCubicFrame(va, vc1, vc2, vb, leftAlpha, new Vector2());
			} else if (cc.nextIsSkip()) {

				Vector2 a = new Vector2();
				cc.getAt(a);

				return a;
			} else {
				Vector2 va = new Vector2();
				Vector2 vc1 = new Vector2();
				Vector2 vc2 = new Vector2();
				Vector2 vb = new Vector2();
				cc.nextLinearFrame(va, vb);

				return new Vector2(va).interpolate(vb, leftAlpha);
			}
		} else {
			return cc.current.getDestination();
		}
	}

	/*
	 * forms two new cubic frames:
	 * 
	 * a, c1, c12, m and m, c21, c2, b
	 * 
	 * note, c1 and c2 are mutated
	 */

	public static boolean hitTest(CachedLine l, Vector2 point, float distance) {
		CachedLineCursor cursor = new CachedLineCursor(l);
		Vector2 t = new Vector2();
		boolean touched = false;
		Vector2 a = new Vector2();
		Vector2 b = new Vector2();
		Vector2 c1 = new Vector2();
		Vector2 c2 = new Vector2();
		CubicCurve2D.Float cubicTester = new CubicCurve2D.Float();
		Line2D.Float lineTester = new Line2D.Float();
		while (cursor.hasNextSegment()) {
			cursor.next();
			if (cursor.nextIsCubic()) {
				if (cursor.nextCubicFrame(a, c1, c2, b)) {
					cubicTester.x1 = a.x;
					cubicTester.y1 = a.y;
					cubicTester.x2 = b.x;
					cubicTester.y2 = b.y;
					cubicTester.ctrlx1 = c1.x;
					cubicTester.ctrly1 = c1.y;
					cubicTester.ctrlx2 = c2.x;
					cubicTester.ctrly2 = c2.y;
					if (cubicTester.intersects(point.x - distance, point.y - distance, distance * 2, distance * 2))
						return true;
				}
			} else {
				if (cursor.nextLinearFrame(a, b)) {
					lineTester.x1 = a.x;
					lineTester.y1 = a.y;
					lineTester.x2 = b.x;
					lineTester.y2 = b.y;
					if (lineTester.intersects(point.x - distance, point.y - distance, distance * 2, distance * 2))
						return true;
				}
			}
		}
		return false;
	}

	static public CachedLineCursor hitTest2(CachedLine l, Vector2 point, float distance) {
		CachedLineCursor cursor = new CachedLineCursor(l);
		Vector2 t = new Vector2();
		boolean touched = false;
		Vector2 a = new Vector2();
		Vector2 b = new Vector2();
		Vector2 c1 = new Vector2();
		Vector2 c2 = new Vector2();
		CubicCurve2D.Float cubicTester = new CubicCurve2D.Float();
		Line2D.Float lineTester = new Line2D.Float();
		while (cursor.hasNextSegment()) {
			cursor.next();
			if (cursor.nextIsCubic()) {
				if (cursor.nextCubicFrame(a, c1, c2, b)) {
					cubicTester.x1 = a.x;
					cubicTester.y1 = a.y;
					cubicTester.x2 = b.x;
					cubicTester.y2 = b.y;
					cubicTester.ctrlx1 = c1.x;
					cubicTester.ctrly1 = c1.y;
					cubicTester.ctrlx2 = c2.x;
					cubicTester.ctrly2 = c2.y;
					if (cubicTester.intersects(point.x - distance, point.y - distance, distance * 2, distance * 2))
						return cursor;
				}
			} else {
				if (cursor.nextLinearFrame(a, b)) {
					lineTester.x1 = a.x;
					lineTester.y1 = a.y;
					lineTester.x2 = b.x;
					lineTester.y2 = b.y;
					if (lineTester.intersects(point.x - distance, point.y - distance, distance * 2, distance * 2))
						return cursor;
				}
			}
		}
		return null;
	}

	public static void markWithSelf(CachedLine line, Prop<Event> attribute) {
		for (Event e : line.events) {
			e.getAttributes().put(attribute, e);
		}
	}

	public static Pair<FitBox, Axis[]> orientedBounds(List<CachedLine> cc) {

		FitBox fb = new FitBox();
		List<Vector3> points = new ArrayList<Vector3>();

		Vector2 t = new Vector2();
		boolean touched = false;
		for (CachedLine c : cc)

			for (int i = 0; i < c.events.size(); i++) {
				if (c.events.get(i).hasDestination()) {

					points.add(c.events.get(i).getDestination(t).toVector3());
					touched = true;
				}
			}
		if (!touched)
			return null;

		Axis[] axis = fb.fit(points.toArray(new Vector3[0]));
		fb.closeFit(axis, points);

		return new Pair<FitBox, Axis[]>(fb, axis);
	}

	static public void splitCubicFrame(Vector2 a, Vector2 c1, Vector2 c2, Vector2 b, float alpha, Vector2 c12, Vector2 m, Vector2 c21, Vector2 tmp) {

		tmp.interpolate(c1, c2, alpha);

		evaluateCubicFrame(a, c1, c2, b, alpha, m);

		c1.interpolate(a, c1, alpha);
		c12.interpolate(c1, tmp, alpha);

		c2.interpolate(c2, b, alpha);
		c21.interpolate(tmp, c2, alpha);
	}

	static public void splitCubicFrame3(Vector3 a, Vector3 c1, Vector3 c2, Vector3 b, float alpha, Vector3 c12, Vector3 m, Vector3 c21, Vector3 tmp) {

		tmp.interpolate(c1, c2, alpha);

		evaluateCubicFrame3(a, c1, c2, b, alpha, m);

		c1.interpolate(a, c1, alpha);
		c12.interpolate(c1, tmp, alpha);

		c2.interpolate(c2, b, alpha);
		c21.interpolate(tmp, c2, alpha);
	}

	public static CachedLine transformLine(CachedLine input, Vector2 before, Vector2 scale, Quaternion rotate, Vector2 after) {
		CachedLine r = new CachedLine();

		for (CachedLine.Event<?> e : input.events) {
			Event ll = e.copy();
			ll.container = r;
			if (ll.args != null)
				for (int i = 0; i < ll.args.length / 2; i++) {
					Vector2 v2 = new Vector2(((Number) e.args[2 * i]).floatValue(), ((Number) e.args[2 * i + 1]).floatValue());
					if (before != null) {
						v2.add(before);
					}
					if (scale != null) {
						v2.x *= scale.x;
						v2.y *= scale.y;
					}
					if (rotate != null) {
						v2 = rotate.rotateVector(v2);
					}
					if (after != null) {
						v2.add(after);
					}
					(ll.args)[2 * i] = new Float(v2.x);
					(ll.args)[2 * i + 1] = new Float(v2.y);
				}
			r.events.add(ll);

		}
		r.getProperties().putAll(input.getProperties());
		return r;
	}

	public static CachedLine transformLine3(CachedLine input, Vector3 before, Vector3 scale, Quaternion rotate, Vector3 after) {
		CachedLine r = new CachedLine();
		float lastZ = 0;

		for (CachedLine.Event<?> e : input.events) {
			Event ll = e.copy();

			if (e.args == null || e.args.length == 0) {

			} else if (e.args.length == 2) {
				Object z = e.getAttributes().get(iLinearGraphicsContext.z_v);
				if (z == null)
					z = 0.0f;
				if (z instanceof Vector3)
					z = ((Vector3) z).x;

				Vector3 a = new Vector3(((Number) e.args[0]).floatValue(), ((Number) e.args[1]).floatValue(), ((Number) z).floatValue());
				if (before != null)
					a.add(before);
				if (scale != null)
					a.scale(scale);
				if (rotate != null)
					rotate.rotateVector(a);
				if (after != null)
					a.add(after);

				ll.args[0] = new Float(a.x);
				ll.args[1] = new Float(a.y);
				ll.getAttributes().put(iLinearGraphicsContext.z_v, new Float(a.z));
			} else if (e.args.length == 6) {
				Vector3 mainZ = new Vector3();
				{
					Object z = e.getAttributes().get(iLinearGraphicsContext.z_v);
					if (z == null)
						z = 0.0f;
					if (z instanceof Vector3)
						z = ((Vector3) z).x;

					Vector3 a = new Vector3(((Number) e.args[0]).floatValue(), ((Number) e.args[1]).floatValue(), ((Number) z).floatValue());
					if (before != null)
						a.add(before);
					if (scale != null)
						a.scale(scale);
					if (rotate != null)
						rotate.rotateVector(a);
					if (after != null)
						a.add(after);

					ll.args[0] = new Float(a.x);
					ll.args[1] = new Float(a.y);
					mainZ.x = a.z;
				}
				{
					Object z = e.getAttributes().get(iLinearGraphicsContext.z_v);
					if (z == null)
						z = 0.0f;
					if (z instanceof Vector3)
						z = ((Vector3) z).y;

					Vector3 a = new Vector3(((Number) e.args[2]).floatValue(), ((Number) e.args[3]).floatValue(), ((Number) z).floatValue());
					if (before != null)
						a.add(before);
					if (scale != null)
						a.scale(scale);
					if (rotate != null)
						rotate.rotateVector(a);
					if (after != null)
						a.add(after);

					ll.args[2] = new Float(a.x);
					ll.args[3] = new Float(a.y);
					mainZ.y = a.z;
				}
				{
					Object z = e.getAttributes().get(iLinearGraphicsContext.z_v);
					if (z == null)
						z = 0.0f;
					if (z instanceof Vector3)
						z = ((Vector3) z).z;

					Vector3 a = new Vector3(((Number) e.args[4]).floatValue(), ((Number) e.args[5]).floatValue(), ((Number) z).floatValue());
					if (before != null)
						a.add(before);
					if (scale != null)
						a.scale(scale);
					if (rotate != null)
						rotate.rotateVector(a);
					if (after != null)
						a.add(after);

					ll.args[4] = new Float(a.x);
					ll.args[5] = new Float(a.y);
					mainZ.z = a.z;
				}
				ll.getAttributes().put(iLinearGraphicsContext.z_v, mainZ);
			}

			r.events.add(ll);

		}
		r.getProperties().put(iLinearGraphicsContext.containsDepth, 1f);
		r.getProperties().putAll(input.getProperties());
		return r;
	}

	public static CachedLine transformLine3(CachedLine input, CoordinateFrame frame) {
		CachedLine r = new CachedLine();
		float lastZ = 0;

		for (CachedLine.Event<?> e : input.events) {
			Event ll = e.copy();

			if (e.args == null || e.args.length == 0) {

			} else if (e.args.length == 2) {
				Object z = e.getAttributes().get(iLinearGraphicsContext.z_v);
				if (z == null)
					z = 0.0f;
				if (z instanceof Vector3)
					z = ((Vector3) z).x;

				Vector3 a = new Vector3(((Number) e.args[0]).floatValue(), ((Number) e.args[1]).floatValue(), ((Number) z).floatValue());
				frame.transformPosition(a);

				ll.args[0] = new Float(a.x);
				ll.args[1] = new Float(a.y);
				ll.getAttributes().put(iLinearGraphicsContext.z_v, new Float(a.z));
			} else if (e.args.length == 6) {
				Vector3 mainZ = new Vector3();
				{
					Object z = e.getAttributes().get(iLinearGraphicsContext.z_v);
					if (z == null)
						z = 0.0f;
					if (z instanceof Vector3)
						z = ((Vector3) z).x;

					Vector3 a = new Vector3(((Number) e.args[0]).floatValue(), ((Number) e.args[1]).floatValue(), ((Number) z).floatValue());
					frame.transformPosition(a);

					ll.args[0] = new Float(a.x);
					ll.args[1] = new Float(a.y);
					mainZ.x = a.z;
				}
				{
					Object z = e.getAttributes().get(iLinearGraphicsContext.z_v);
					if (z == null)
						z = 0.0f;
					if (z instanceof Vector3)
						z = ((Vector3) z).y;

					Vector3 a = new Vector3(((Number) e.args[2]).floatValue(), ((Number) e.args[3]).floatValue(), ((Number) z).floatValue());
					frame.transformPosition(a);

					ll.args[2] = new Float(a.x);
					ll.args[3] = new Float(a.y);
					mainZ.y = a.z;
				}
				{
					Object z = e.getAttributes().get(iLinearGraphicsContext.z_v);
					if (z == null)
						z = 0.0f;
					if (z instanceof Vector3)
						z = ((Vector3) z).z;

					Vector3 a = new Vector3(((Number) e.args[4]).floatValue(), ((Number) e.args[5]).floatValue(), ((Number) z).floatValue());
					frame.transformPosition(a);

					ll.args[4] = new Float(a.x);
					ll.args[5] = new Float(a.y);
					mainZ.z = a.z;
				}
				ll.getAttributes().put(iLinearGraphicsContext.z_v, mainZ);
			}

			r.events.add(ll);

		}
		r.getProperties().putAll(input.getProperties());
		r.getProperties().put(iLinearGraphicsContext.containsDepth, 1f);
		return r;
	}

	public static float interpret(Object object, int i) {
		if (object == null)
			return 0;
		if (object instanceof Number)
			return ((Number) object).floatValue();
		if (object instanceof Vector3) {
			Vector3 v = ((Vector3) object);
			if (i == -1)
				return v.get(2);
			return v.get(i);
		}
		return 0;
	}

	public CachedLine fixProperties(CachedLine newLine, CachedLine oldLine) {
		if (newLine == null)
			return null;

		ClosestEvent ll = new ClosestEvent(oldLine);
		Event previousMoveTo = null;
		for (Event e : newLine.events) {
			if (e.method.equals(iLine_m.moveTo_m)) {
				previousMoveTo = e;
			}
			if (e.hasDestination()) {
				Vector2 at = e.getDestination();
				Event aa = ll.closestTo(at);

				if (aa != null) {
					e.attributes = aa.attributes;
				}
			} else {
				Vector2 at = previousMoveTo.getDestination();
				Event aa = ll.closestTo(at);
				if (aa != null) {
					e.attributes = aa.attributes;
				}
			}
		}
		return newLine;
	}

	public boolean isIntersecting(CachedLine line, Rect bounds) {
		GeneralPath g = this.lineToGeneralPath(line);

		if (g != null) {
			Area a = new Area(g);
			Rectangle2D.Double d = new Rectangle2D.Double(bounds.x, bounds.y, bounds.w, bounds.h);
			Area a2 = new Area(d);
			a2.intersect(a);
			Rectangle res = a2.getBounds();
			float area = res.width * res.height;
			return area > 1;
		} else {
			return false;
		}
	}

	public float amountIntersecting(CachedLine line, Rect bounds) {
		GeneralPath g = this.lineToGeneralPath(line);

		if (g != null) {
			Area a = new Area(g);
			Rectangle2D.Double d = new Rectangle2D.Double(bounds.x, bounds.y, bounds.w, bounds.h);
			Area a2 = new Area(d);
			a2.intersect(a);
			Rectangle res = a2.getBounds();
			float area = (float) ((res.width * res.height) / (bounds.w * bounds.h));
			return area;
		} else {
			return 0;
		}
	}

	public CachedLine lineAsStroked(CachedLine line, BasicStroke stroke, boolean fixProperties) {
		GeneralPath path = lineToGeneralPath(line);
		Shape shape = stroke.createStrokedShape(path);

		PathIterator pi = new Area(shape).getPathIterator(null);

		// PathIterator pi = shape.getPathIterator(null);

		CachedLine ret = piToCachedLine(pi);

		if (ret != null) {
			if (fixProperties) {
				fixProperties(ret, line);
			}
			ret.finish();

		} else {
		}
		return ret;
	}

	public CachedLine lineAsStroked(CachedLine line, float width) {

		return lineAsStroked(line, new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER), true);

	}

	public GeneralPath lineToGeneralPath(CachedLine line) {
		return lineToGeneralPath(line, false);
	}

	public GeneralPath lineToGeneralPath(CachedLine transformedLine, boolean linesOnly) {

		GeneralPath path = new GeneralPath();

		for (CachedLine.Event e : transformedLine.events) {
			if (e.method.equals(iLine_m.moveTo_m))
				path.moveTo(((Number) e.args[0]).floatValue(), ((Number) e.args[1]).floatValue());
			else if (e.method.equals(iLine_m.lineTo_m))
				path.lineTo(((Number) e.args[0]).floatValue(), ((Number) e.args[1]).floatValue());
			else if (e.method.equals(iLine_m.close_m))
				path.closePath();
			else if (e.method.equals(iLine_m.cubicTo_m)) {
				if (!linesOnly)
					path.curveTo(((Number) e.args[0]).floatValue(), ((Number) e.args[1]).floatValue(), ((Number) e.args[2]).floatValue(), ((Number) e.args[3]).floatValue(), ((Number) e.args[4]).floatValue(), ((Number) e.args[5]).floatValue());
				else
					path.lineTo(((Number) e.args[4]).floatValue(), ((Number) e.args[5]).floatValue());
			}
		}

		path.setWindingRule(PathIterator.WIND_EVEN_ODD);

		return path;
	}

	public CachedLine piToCachedLine(PathIterator pi) {
		CachedLine ret = new CachedLine();
		iLine in = ret.getInput();
		float[] cc = new float[6];

		Vector2 lastAt = null;

		while (!pi.isDone()) {
			int ty = pi.currentSegment(cc);

			if (ty == PathIterator.SEG_CLOSE) {
				lastAt = null;
				in.close();
			} else if (ty == PathIterator.SEG_CUBICTO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[4]) + Math.abs(lastAt.y - cc[5]) > 1e-15))
					in.cubicTo(cc[0], cc[1], cc[2], cc[3], cc[4], cc[5]);
				if (lastAt == null)
					lastAt = new Vector2(cc[4], cc[5]);
				else {
					lastAt.x = cc[4];
					lastAt.y = cc[5];
				}
			} else if (ty == PathIterator.SEG_LINETO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[0]) + Math.abs(lastAt.y - cc[1]) > 1e-15))
					in.lineTo(cc[0], cc[1]);
				if (lastAt == null)
					lastAt = new Vector2(cc[0], cc[1]);
				else {
					lastAt.x = cc[0];
					lastAt.y = cc[1];
				}
			} else if (ty == PathIterator.SEG_MOVETO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[0]) + Math.abs(lastAt.y - cc[1]) > 1e-15))
					in.moveTo(cc[0], cc[1]);
				if (lastAt == null)
					lastAt = new Vector2(cc[0], cc[1]);
				else {
					lastAt.x = cc[0];
					lastAt.y = cc[1];
				}
			} else if (ty == PathIterator.SEG_QUADTO) {
				if ((Math.abs(lastAt.x - cc[2]) + Math.abs(lastAt.y - cc[3]) > 1e-15)) {
					in.cubicTo((cc[0] - lastAt.x) * (2 / 3f) + lastAt.x, (cc[1] - lastAt.y) * (2 / 3f) + lastAt.y, (cc[0] - cc[2]) * (2 / 3f) + cc[2], (cc[1] - cc[3]) * (2 / 3f) + cc[3], cc[2], cc[3]);
					lastAt.x = cc[2];
					lastAt.y = cc[3];
				}

			}

			pi.next();
		}

		// now remove degenerate move() close() pairs

		for (int i = 0; i < ret.events.size() - 1; i++) {
			if (ret.events.get(i).method.equals(iLine_m.moveTo_m) && ret.events.get(i + 1).method.equals(iLine_m.close_m)) {
				ret.events.remove(i);
				ret.events.remove(i);
				i--;
			}
		}

		if (ret.events.size() == 0)
			return null;

		return ret;
	}

	public void piToCachedLine(PathIterator pi, CachedLine contInto, boolean firstIsMoveto, boolean fixClose) {

		iLine in = contInto.getInput();
		float[] cc = new float[6];
		Vector2 lastAt = null;
		Vector2 lastMoveTo = null;
		while (!pi.isDone()) {
			int ty = pi.currentSegment(cc);
			if (ty == PathIterator.SEG_CLOSE) {
				if (!fixClose) {
					lastAt = null;
					in.close();
				} else {
					if (lastMoveTo != null && lastAt.distanceFrom(lastMoveTo) > 1e-6)
						in.lineTo(lastMoveTo.x, lastMoveTo.y);
					lastAt = null;
				}
			} else if (ty == PathIterator.SEG_CUBICTO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[4]) + Math.abs(lastAt.y - cc[5]) > 1e-15))
					in.cubicTo(cc[0], cc[1], cc[2], cc[3], cc[4], cc[5]);
				if (lastAt == null)
					lastAt = new Vector2(cc[4], cc[5]);
				else {
					lastAt.x = cc[4];
					lastAt.y = cc[5];
				}
			} else if (ty == PathIterator.SEG_LINETO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[0]) + Math.abs(lastAt.y - cc[1]) > 1e-15))
					in.lineTo(cc[0], cc[1]);
				if (lastAt == null)
					lastAt = new Vector2(cc[0], cc[1]);
				else {
					lastAt.x = cc[0];
					lastAt.y = cc[1];
				}
			} else if (ty == PathIterator.SEG_MOVETO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[0]) + Math.abs(lastAt.y - cc[1]) > 1e-15))
					in.moveTo(cc[0], cc[1]);

				lastMoveTo = new Vector2(cc[0], cc[1]);

				if (lastAt == null)
					lastAt = new Vector2(cc[0], cc[1]);
				else {
					lastAt.x = cc[0];
					lastAt.y = cc[1];
				}
			} else if (ty == PathIterator.SEG_QUADTO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[2]) + Math.abs(lastAt.y - cc[3]) > 1e-15))
					in.cubicTo((cc[0] - lastAt.x) * (2 / 3f) + lastAt.x, (cc[1] - lastAt.y) * (2 / 3f) + lastAt.y, (cc[0] - cc[2]) * (2 / 3f) + cc[2], (cc[1] - cc[3]) * (2 / 3f) + cc[3], cc[2], cc[3]);

				if (lastAt == null)
					lastAt = new Vector2(cc[2], cc[3]);
				else {
					lastAt.x = cc[2];
					lastAt.y = cc[3];
				}

			}

			pi.next();
		}
	}

	public void piToCachedLine(PathIterator pi, CachedLine contInto, boolean firstIsMoveto) {
		piToCachedLine(pi, contInto, firstIsMoveto, false);
	}

	public CachedLine piToCachedLine(PathIterator pi, float tol) {
		CachedLine ret = new CachedLine();
		iLine in = ret.getInput();
		float[] cc = new float[6];

		Vector2 lastAt = null;

		while (!pi.isDone()) {
			int ty = pi.currentSegment(cc);
			if (ty == PathIterator.SEG_CLOSE) {
				lastAt = null;
				in.close();
			} else if (ty == PathIterator.SEG_CUBICTO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[4]) + Math.abs(lastAt.y - cc[5]) > tol))
					in.cubicTo(cc[0], cc[1], cc[2], cc[3], cc[4], cc[5]);
				if (lastAt == null)
					lastAt = new Vector2(cc[4], cc[5]);
				else {
					lastAt.x = cc[4];
					lastAt.y = cc[5];
				}
			} else if (ty == PathIterator.SEG_LINETO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[0]) + Math.abs(lastAt.y - cc[1]) > tol))
					in.lineTo(cc[0], cc[1]);
				if (lastAt == null)
					lastAt = new Vector2(cc[0], cc[1]);
				else {
					lastAt.x = cc[0];
					lastAt.y = cc[1];
				}
			} else if (ty == PathIterator.SEG_MOVETO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[0]) + Math.abs(lastAt.y - cc[1]) > tol))
					in.moveTo(cc[0], cc[1]);
				if (lastAt == null)
					lastAt = new Vector2(cc[0], cc[1]);
				else {
					lastAt.x = cc[0];
					lastAt.y = cc[1];
				}
			} else if (ty == PathIterator.SEG_QUADTO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[2]) + Math.abs(lastAt.y - cc[3]) > tol)) {

					in.cubicTo((cc[0] - lastAt.x) * (2 / 3f) + lastAt.x, (cc[1] - lastAt.y) * (2 / 3f) + lastAt.y, (cc[0] - cc[2]) * (2 / 3f) + cc[2], (cc[1] - cc[3]) * (2 / 3f) + cc[3], cc[2], cc[3]);
					if (lastAt == null)
						lastAt = new Vector2(cc[2], cc[3]);
					else {
						lastAt.x = cc[2];
						lastAt.y = cc[3];
					}

				}
			}

			pi.next();
		}

		// now remove degenerate move() close() pairs

		for (int i = 0; i < ret.events.size() - 1; i++) {
			if (ret.events.get(i).method.equals(iLine_m.moveTo_m) && ret.events.get(i + 1).method.equals(iLine_m.close_m)) {
				ret.events.remove(i);
				ret.events.remove(i);
				i--;
			}
		}

		if (ret.events.size() == 0)
			return null;

		return ret;
	}

	public CachedLine reverse(CachedLine c) {
		CachedLine out = new CachedLine();

		Vector2 lastAt = new Vector2();
		Vector2 nextAt = new Vector2();

		for (int i = c.events.size() - 1; i >= 0; i--) {
			Event e = c.events.get(i);
			Event ne = i > 0 ? c.events.get(i - 1) : null;
			Event pe = i < c.events.size() - 1 ? c.events.get(i + 1) : null;

			if (i == c.events.size() - 1 || pe.method.equals(iLine_m.moveTo_m)) {
				e.getDestination(lastAt);
				out.getInput().moveTo(lastAt.x, lastAt.y);
			}

			if (e.method.equals(iLine_m.lineTo_m)) {
				ne.getDestination(lastAt);
				out.getInput().lineTo(lastAt.x, lastAt.y);
			} else if (e.method.equals(iLine_m.cubicTo_m)) {
				ne.getDestination(lastAt);
				Vector2 c1 = e.getAt(0);
				Vector2 c2 = e.getAt(1);
				out.getInput().cubicTo(c2.x, c2.y, c1.x, c1.y, lastAt.x, lastAt.y);
			} else if (e.method.equals(iLine_m.moveTo_m)) {
				// do nothing,
				// we should
				// already be
				// there
			}
		}

		out.getProperties().putAll(c.getProperties());

		return out;
	}

	public List<CachedLine> segmentSubpaths(CachedLine c) {
		List<CachedLine> cl = new ArrayList<CachedLine>();
		CachedLine workingOn = null;
		for (CachedLine.Event e : c.events) {
			if (e.method.equals(iLine_m.moveTo_m)) {
				if (workingOn != null)
					cl.add(workingOn);
				CachedLine next = new CachedLine();
				next.getProperties().putAll(c.getProperties());
				workingOn = next;
			}
			if (workingOn != null)
				workingOn.events.add(e);
		}
		if (workingOn != null)
			cl.add(workingOn);

		return cl;
	}

	public CachedLine simpleSubdivideAll(CachedLine c) {
		CachedLine out = new CachedLine();

		Vector2 lastAt = new Vector2();
		Vector2 nextAt = new Vector2();

		for (Event e : c.events) {
			if (e.method.equals(iLine_m.moveTo_m)) {
				e.getDestination(lastAt);
				out.getInput().moveTo(lastAt.x, lastAt.y);
			} else if (e.method.equals(iLine_m.lineTo_m)) {
				e.getDestination(nextAt);
				out.getInput().lineTo((lastAt.x + nextAt.x) / 2, (lastAt.y + nextAt.y) / 2);
				out.getInput().lineTo(nextAt.x, nextAt.y);
				lastAt.set(nextAt);
			} else if (e.method.equals(iLine_m.close_m)) {
				out.getInput().close();
			} else if (e.method.equals(iLine_m.cubicTo_m)) {
				Vector2 c1 = e.getAt(0);
				Vector2 c2 = e.getAt(1);
				Vector2 b = e.getAt(2);

				Vector2 c12 = new Vector2();
				Vector2 m = new Vector2();
				Vector2 c21 = new Vector2();
				Vector2 t = new Vector2();

				LineUtils.splitCubicFrame(lastAt, c1, c2, b, 0.5f, c12, m, c21, t);

				out.getInput().cubicTo(c1.x, c1.y, c12.x, c12.y, m.x, m.y);
				out.getInput().cubicTo(c21.x, c21.y, c2.x, c2.y, b.x, b.y);

				lastAt.set(b);
			} else
				assert false : e;
		}

		out.getProperties().putAll(c.getProperties());

		return out;
	}

	public CachedLine simpleSubdivideAllAsCurves(CachedLine c) {
		CachedLine out = new CachedLine();

		Vector2 lastAt = new Vector2();
		Vector2 lastMoveTo = new Vector2();
		for (Event e : c.events) {
			if (e.method.equals(iLine_m.moveTo_m)) {
				e.getDestination(lastAt);
				lastMoveTo.setValue(lastAt);
				out.getInput().moveTo(lastAt.x, lastAt.y);
			} else if (e.method.equals(iLine_m.lineTo_m)) {

				Vector2 c1 = new Vector2().interpolate(lastAt, e.getDestination(), 0.3333f);
				Vector2 c2 = new Vector2().interpolate(lastAt, e.getDestination(), 0.666f);
				Vector2 b = e.getDestination();

				Vector2 c12 = new Vector2();
				Vector2 m = new Vector2();
				Vector2 c21 = new Vector2();
				Vector2 t = new Vector2();

				LineUtils.splitCubicFrame(lastAt, c1, c2, b, 0.5f, c12, m, c21, t);

				out.getInput().cubicTo(c1.x, c1.y, c12.x, c12.y, m.x, m.y);
				out.getInput().cubicTo(c21.x, c21.y, c2.x, c2.y, b.x, b.y);

				lastAt.set(b);

				e.getDestination(lastAt);

			} else if (e.method.equals(iLine_m.close_m)) {

				if (lastMoveTo.distanceFrom(lastAt) > 1e-2) {
					Vector2 c1 = new Vector2().interpolate(lastAt, lastMoveTo, 0.3333f);
					Vector2 c2 = new Vector2().interpolate(lastAt, lastMoveTo, 0.666f);
					Vector2 b = lastMoveTo;

					Vector2 c12 = new Vector2();
					Vector2 m = new Vector2();
					Vector2 c21 = new Vector2();
					Vector2 t = new Vector2();

					LineUtils.splitCubicFrame(lastAt, c1, c2, b, 0.5f, c12, m, c21, t);

					out.getInput().cubicTo(c1.x, c1.y, c12.x, c12.y, m.x, m.y);
					out.getInput().cubicTo(c21.x, c21.y, c2.x, c2.y, b.x, b.y);

					lastAt.set(b);

				}

				out.getInput().close();
			} else if (e.method.equals(iLine_m.cubicTo_m)) {
				Vector2 c1 = e.getAt(0);
				Vector2 c2 = e.getAt(1);
				Vector2 b = e.getAt(2);

				Vector2 c12 = new Vector2();
				Vector2 m = new Vector2();
				Vector2 c21 = new Vector2();
				Vector2 t = new Vector2();

				LineUtils.splitCubicFrame(lastAt, c1, c2, b, 0.5f, c12, m, c21, t);

				out.getInput().cubicTo(c1.x, c1.y, c12.x, c12.y, m.x, m.y);
				out.getInput().cubicTo(c21.x, c21.y, c2.x, c2.y, b.x, b.y);

				lastAt.set(b);
			} else
				assert false : e;
		}

		out.getProperties().putAll(c.getProperties());

		return out;
	}

	public CachedLine simpleSubdivideAllAsCurves3(CachedLine c) {
		CachedLine out = new CachedLine();

		Vector3 lastAt = new Vector3();
		Vector3 lastMoveTo = new Vector3();
		for (Event e : c.events) {
			if (e.method.equals(iLine_m.moveTo_m)) {
				lastAt = getDestination3(e);
				lastMoveTo.setValue(lastAt);
				out.getInput().moveTo(lastAt.x, lastAt.y);
				out.getInput().setPointAttribute(iLinearGraphicsContext.z_v, lastAt.z);

			} else if (e.method.equals(iLine_m.lineTo_m)) {

				Vector3 c1 = new Vector3().interpolate(lastAt, getDestination3(e), 0.3333f);
				Vector3 c2 = new Vector3().interpolate(lastAt, getDestination3(e), 0.666f);
				Vector3 b = getDestination3(e);

				Vector3 c12 = new Vector3();
				Vector3 m = new Vector3();
				Vector3 c21 = new Vector3();
				Vector3 t = new Vector3();

				LineUtils.splitCubicFrame3(lastAt, c1, c2, b, 0.5f, c12, m, c21, t);

				out.getInput().cubicTo(c1.x, c1.y, c12.x, c12.y, m.x, m.y);
				out.getInput().setPointAttribute(iLinearGraphicsContext.z_v, new Vector3(c1.z, c12.z, m.z));
				out.getInput().cubicTo(c21.x, c21.y, c2.x, c2.y, b.x, b.y);
				out.getInput().setPointAttribute(iLinearGraphicsContext.z_v, new Vector3(c21.z, c2.z, b.z));

				lastAt.set(b);

				lastAt = getDestination3(e);

			} else if (e.method.equals(iLine_m.close_m)) {

				if (lastMoveTo.distanceFrom(lastAt) > 1e-2) {
					Vector3 c1 = new Vector3().interpolate(lastAt, lastMoveTo, 0.3333f);
					Vector3 c2 = new Vector3().interpolate(lastAt, lastMoveTo, 0.666f);
					Vector3 b = lastMoveTo;

					Vector3 c12 = new Vector3();
					Vector3 m = new Vector3();
					Vector3 c21 = new Vector3();
					Vector3 t = new Vector3();

					LineUtils.splitCubicFrame3(lastAt, c1, c2, b, 0.5f, c12, m, c21, t);

					out.getInput().cubicTo(c1.x, c1.y, c12.x, c12.y, m.x, m.y);
					out.getInput().setPointAttribute(iLinearGraphicsContext.z_v, new Vector3(c1.z, c12.z, m.z));
					out.getInput().cubicTo(c21.x, c21.y, c2.x, c2.y, b.x, b.y);
					out.getInput().setPointAttribute(iLinearGraphicsContext.z_v, new Vector3(c21.z, c2.z, b.z));

					lastAt.set(b);

				}

				out.getInput().close();
			} else if (e.method.equals(iLine_m.cubicTo_m)) {

				Vector3 c1 = getControl1(e);
				Vector3 c2 = getControl2(e);
				Vector3 b = getDestination3(e);

				Vector3 c12 = new Vector3();
				Vector3 m = new Vector3();
				Vector3 c21 = new Vector3();
				Vector3 t = new Vector3();

				LineUtils.splitCubicFrame3(lastAt, c1, c2, b, 0.5f, c12, m, c21, t);

				out.getInput().cubicTo(c1.x, c1.y, c12.x, c12.y, m.x, m.y);
				out.getInput().setPointAttribute(iLinearGraphicsContext.z_v, new Vector3(c1.z, c12.z, m.z));
				out.getInput().cubicTo(c21.x, c21.y, c2.x, c2.y, b.x, b.y);
				out.getInput().setPointAttribute(iLinearGraphicsContext.z_v, new Vector3(c21.z, c2.z, b.z));

				lastAt.set(b);
			} else
				assert false : e;
		}

		out.getProperties().putAll(c.getProperties());

		return out;
	}

	public CachedLine simpleSubdivideAllCurves(CachedLine c) {
		CachedLine out = new CachedLine();

		Vector2 lastAt = new Vector2();
		for (Event e : c.events) {
			if (e.method.equals(iLine_m.moveTo_m)) {
				e.getDestination(lastAt);
				out.getInput().moveTo(lastAt.x, lastAt.y);
			} else if (e.method.equals(iLine_m.lineTo_m)) {
				e.getDestination(lastAt);
				out.getInput().lineTo(lastAt.x, lastAt.y);
			} else if (e.method.equals(iLine_m.close_m)) {
				out.getInput().close();
			} else if (e.method.equals(iLine_m.cubicTo_m)) {
				Vector2 c1 = e.getAt(0);
				Vector2 c2 = e.getAt(1);
				Vector2 b = e.getAt(2);

				Vector2 c12 = new Vector2();
				Vector2 m = new Vector2();
				Vector2 c21 = new Vector2();
				Vector2 t = new Vector2();

				LineUtils.splitCubicFrame(lastAt, c1, c2, b, 0.5f, c12, m, c21, t);

				out.getInput().cubicTo(c1.x, c1.y, c12.x, c12.y, m.x, m.y);
				out.getInput().cubicTo(c21.x, c21.y, c2.x, c2.y, b.x, b.y);

				lastAt.set(b);
			} else
				assert false : e;
		}

		out.getProperties().putAll(c.getProperties());

		return out;
	}

	public Rect slowBounds(CachedLine c) {
		GeneralPath g = this.lineToGeneralPath(c);
		if (g != null) {
			Area a = new Area(g);
			Rectangle2D r = a.getBounds2D();
			return new Rect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
		}
		return null;
	}

	static public Vector3 getDestination3(Event event) {
		Vector2 d = event.getDestination();
		Object o = event.getAttributes().get(iLinearGraphicsContext.z_v);
		if (o == null)
			o = 0f;
		if (o instanceof Number)
			return new Vector3(d.x, d.y, ((Number) o).floatValue());
		else
			return new Vector3(d.x, d.y, ((Vector3) o).z);
	}

	static public Vector3 getControl1(Event event) {
		Vector2 d = event.getAt(0);
		Object o = event.getAttributes().get(iLinearGraphicsContext.z_v);
		if (o == null)
			o = 0f;
		if (o instanceof Number)
			return new Vector3(d.x, d.y, ((Number) o).floatValue());
		else
			return new Vector3(d.x, d.y, ((Vector3) o).x);
	}

	static public Vector3 getControl2(Event event) {
		Vector2 d = event.getAt(1);
		Object o = event.getAttributes().get(iLinearGraphicsContext.z_v);
		if (o == null)
			o = 0f;
		if (o instanceof Number)
			return new Vector3(d.x, d.y, ((Number) o).floatValue());
		else
			return new Vector3(d.x, d.y, ((Vector3) o).y);
	}

	static public Vector3 getDestination3(Event event, float def_z) {
		Vector2 d = event.getDestination();
		Object o = event.getAttributes().get(iLinearGraphicsContext.z_v);
		if (o == null)
			o = def_z;
		if (o instanceof Number)
			return new Vector3(d.x, d.y, ((Number) o).floatValue());
		else
			return new Vector3(d.x, d.y, ((Vector3) o).z);
	}

	static public Vector3 getControl1(Event event, float def_z) {
		Vector2 d = event.getAt(0);
		Object o = event.getAttributes().get(iLinearGraphicsContext.z_v);
		if (o == null)
			o = def_z;
		if (o instanceof Number)
			return new Vector3(d.x, d.y, ((Number) o).floatValue());
		else
			return new Vector3(d.x, d.y, ((Vector3) o).x);
	}

	static public Vector3 getControl2(Event event, float def_z) {
		Vector2 d = event.getAt(1);
		Object o = event.getAttributes().get(iLinearGraphicsContext.z_v);
		if (o == null)
			o = def_z;
		if (o instanceof Number)
			return new Vector3(d.x, d.y, ((Number) o).floatValue());
		else
			return new Vector3(d.x, d.y, ((Vector3) o).y);
	}

	static public boolean circleTo(CachedLine into, Vector2 c1, Vector2 dest, float limit, float over) {
		assert into.events.size() != 0;
		Vector2 at = into.events.get(into.events.size() - 1).getDestination();

		Vector2 center = BaseMath.circumcenterOf(at, c1, dest);

		float d = center.distanceFrom(at);

		System.err.println(" d = " + d + " " + center + " " + at + " from <" + c1 + "> <" + dest + ">");

		if (d > limit || d == 0 || Float.isInfinite(d) || Float.isNaN(d)) {
			iLine in = into.getInput();
			in.lineTo(dest.x, dest.y);
			return false;
		} else {
			Arc2D.Float arc = new Arc2D.Float();

			Vector2 t1 = new Vector2(dest).sub(at);
			Vector2 t2 = new Vector2(center).sub(at);

			Vector2 nc11 = new Vector2(t2.y, -t2.x).add(at);
			Vector2 nc21 = new Vector2(-t2.y, t2.x).add(at);

			Vector2 newCenter1 = (nc11.distanceFrom(c1) < nc21.distanceFrom(c1) ? nc11 : nc21).sub(at);

			t2 = new Vector2(center).sub(dest);
			Vector2 nc12 = new Vector2(t2.y, -t2.x).add(dest);
			Vector2 nc22 = new Vector2(-t2.y, t2.x).add(dest);

			Vector2 newCenter2 = (nc12.distanceFrom(c1) < nc22.distanceFrom(c1) ? nc12 : nc22).sub(dest);

			// intersect newCenter

			LinePointIntersectionInfo in = IntersectionPrimatives.lineToLine(at.toVector3(), newCenter1.toVector3(), dest.toVector3(), newCenter2.toVector3());

			Vector2 newCenter = in.closestPoint.toVector2();

			arc.setArcByTangent(new Point2D.Float(at.x, at.y), new Point2D.Float(newCenter.x, newCenter.y), new Point2D.Float(dest.x, dest.y), d);

			double start = arc.getAngleStart();
			double ex = arc.getAngleExtent();

			start -= ex * over;
			ex *= (1 + over * 2);

			arc.setAngleStart(start);
			arc.setAngleExtent(ex);

			new LineUtils().piToCachedLine(arc.getPathIterator(null), into, true);

			return true;
		}
	}

	static public CachedLine noise(CachedLine line, final float level) {
		line = new BaseFilter().visitPositions(line, new Visitors.PositionVisitor() {

			public void visitPosition(Vector2 v, SubSelection part, BaseFilter inside) {
				v.noise(level);
			}
		});
		return line;
	}

	static public CachedLine noClose(CachedLine line) {
		Iterator<Event> ii = line.events.iterator();
		while (ii.hasNext())
			if (!ii.next().hasDestination())
				ii.remove();
		return line;
	}

	static public CachedLine fixClose(CachedLine line) {
		Iterator<Event> ii = line.events.iterator();
		Event lastMoveTo = null;
		while (ii.hasNext()) {
			Event nn = ii.next();

			if (nn.method.equals(iLine_m.moveTo_m)) {
				lastMoveTo = nn;
			}

			if (nn.method.equals(iLine_m.close_m)) {
				if (lastMoveTo == null) {
					ii.remove();
				} else {
					nn.method = iLine_m.lineTo_m;
					nn.args = lastMoveTo.args;
				}
			}
		}
		return line;
	}

	public static CachedLine transformLineOffsetFrom(CachedLine input, Rect frame, Vector2 o) {
		CachedLine r = new CachedLine();

		for (CachedLine.Event<?> e : input.events) {
			Event ll = e.copy();
			if (ll.args != null)
				for (int i = 0; i < ll.args.length / 2; i++) {
					Vector2 v2 = new Vector2(((Number) e.args[2 * i]).floatValue(), ((Number) e.args[2 * i + 1]).floatValue());

					Vector2 oo = o;
					if (e.attributes != null) {
						Vector2 x = e.attributes.get(iLinearGraphicsContext.offsetFromSource_v);
						if (x != null)
							oo = x;
					}

					(ll.args)[2 * i] = new Float(v2.x + (oo.x * frame.w + frame.x));
					(ll.args)[2 * i + 1] = new Float(v2.y + (oo.y * frame.h + frame.y));
				}
			r.events.add(ll);

		}
		r.getProperties().putAll(input.getProperties());
		return r;

	}

	public CachedLine transformLine(CachedLine input, AffineTransform t) {

		CachedLine r = new CachedLine();

		Point2D in = new Point2D.Float();
		Point2D out = new Point2D.Float();

		for (CachedLine.Event<?> e : input.events) {
			Event ll = e.copy();
			if (ll.args != null)
				for (int i = 0; i < ll.args.length / 2; i++) {
					in.setLocation(((Number) e.args[2 * i]).floatValue(), ((Number) e.args[2 * i + 1]).floatValue());

					t.transform(in, out);

					(ll.args)[2 * i] = new Float(out.getX());
					(ll.args)[2 * i + 1] = new Float(out.getY());
				}
			r.events.add(ll);

		}
		r.getProperties().putAll(input.getProperties());
		return r;

	}

	public GeneralPath lineToGeneralPathProjected(CachedLine transformedLine, Projector projector, float width, float height) {

		GeneralPath path = new GeneralPath();

		for (CachedLine.Event e : transformedLine.events) {
			if (e.method.equals(iLine_m.moveTo_m)) {
				Vector3 b = getDestination3(e);
				Vector3 pixel = projector.toPixel(b, width, height);
				;// System.out.println(" transformed <" + b +
					// " -> " + pixel + ">");
					// if (pixel.z>0.99f) return null;
				path.moveTo(pixel.x, height - pixel.y);
			} else if (e.method.equals(iLine_m.lineTo_m)) {
				Vector3 b = getDestination3(e);
				Vector3 pixel = projector.toPixel(b, width, height);
				;// System.out.println(" transformed <" + b +
					// " -> " + pixel + ">");
					// if (pixel.z>0.99f) return null;
				path.lineTo(pixel.x, height - pixel.y);
			} else if (e.method.equals(iLine_m.close_m))
				path.closePath();
			else if (e.method.equals(iLine_m.cubicTo_m)) {
				Vector3 c1 = getControl1(e);
				Vector3 c2 = getControl2(e);
				Vector3 b = getDestination3(e);

				Vector3 pixel_c1 = projector.toPixel(c1, width, height);
				Vector3 pixel_c2 = projector.toPixel(c2, width, height);
				Vector3 pixel_b = projector.toPixel(b, width, height);

				// if (pixel_b.z>0.99f) return null;

				path.curveTo(pixel_c1.x, height - pixel_c1.y, pixel_c2.x, height - pixel_c2.y, pixel_b.x, height - pixel_b.y);

			}
		}

		path.setWindingRule(PathIterator.WIND_EVEN_ODD);

		return path;

	}

	/*
	 * static public Vector2 evaluateCubicFrame(Vector2 a, Vector2 c1,
	 * Vector2 c2, Vector2 b, float alpha, Vector2 out) { float oma = 1 -
	 * alpha; float oma2 = oma * oma; float oma3 = oma2 * oma; float alpha2
	 * = alpha * alpha; float alpha3 = alpha2 * alpha;
	 * 
	 * 
	 * return out; }

		out.x = a.x * oma3 + 3 * c1.x * alpha * oma2 + 3 * c2.x * alpha2 * oma + b.x * alpha3;
		out.y = a.y * oma3 + 3 * c1.y * alpha * oma2 + 3 * c2.y * alpha2 * oma + b.y * alpha3;

	 *
	 *
	 */
	public Pair<Vector2, Vector2> controlPointsFor(Vector2 a, Vector2 b, Vector2 c, Vector2 d, Number t1, Number t2) {
		t1 = t1==null ? b.distanceFrom(a) / (b.distanceFrom(a) + c.distanceFrom(b) + d.distanceFrom(c)) : t1.floatValue();
		t2 = t2==null ? 1-(c.distanceFrom(d) / (b.distanceFrom(a) + c.distanceFrom(b) + d.distanceFrom(c))) : t2.floatValue();

		System.out.println(" t1 :"+t1+" "+t2);

		float q0 = (1-t1.floatValue())*(1-t1.floatValue())*(1-t1.floatValue());
		float q1 = 3*(1-t1.floatValue())*(1-t1.floatValue())*t1.floatValue();
		float q2 = 3*(1-t1.floatValue())*t1.floatValue()*t1.floatValue();
		float q3 = t1.floatValue()*t1.floatValue()*t1.floatValue();

		float k0 = (1-t2.floatValue())*(1-t2.floatValue())*(1-t2.floatValue());
		float k1 = 3*(1-t2.floatValue())*(1-t2.floatValue())*t2.floatValue();
		float k2 = 3*(1-t2.floatValue())*t2.floatValue()*t2.floatValue();
		float k3 = t2.floatValue()*t2.floatValue()*t2.floatValue();

		// b = q0*a+q1*c1+q2*c2+q3*d
		// c = k0*a+k1*c1+k2*c2+k3*d
		
		float c1x = ((b.x-q0*a.x-q3*d.x)*k2-(c.x-k0*a.x-k3*d.x)*q2)/(q1*k2-k1*q2);
		float c1y = ((b.y-q0*a.y-q3*d.y)*k2-(c.y-k0*a.y-k3*d.y)*q2)/(q1*k2-k1*q2);
		
		float c2x = (b.x-q0*a.x-q1*c1x-q3*d.x)/q2;
		float c2y = (b.y-q0*a.y-q1*c1y-q3*d.y)/q2;
		
		return new Pair<Vector2, Vector2>(new Vector2(c1x, c1y), new Vector2(c2x, c2y));
	}
}
