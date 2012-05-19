package field.core.plugins.drawing.opengl;

import java.util.ArrayList;
import java.util.List;

import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.math.linalg.Vector2;
import field.namespace.generic.ReflectionTools;
import field.namespace.generic.Generics.Pair;
import field.util.Dict;

public class Intersections {

	static public class Intersection_CubicCubic {
		float fuzz = 1e-5f;

		int maxIndex = 15;

		LineUtils u = new LineUtils();

		Vector2 t = new Vector2();

		// out is a T
		// vector, not a
		// position
		public boolean intersectCubic(Vector2 la, Vector2 l1, Vector2 l2, Vector2 lb, Vector2 ra, Vector2 r1, Vector2 r2, Vector2 rb, Vector2 out, int index) {

			float mlx = Math.max(la.x, Math.max(l1.x, Math.max(l2.x, lb.x)));
			float mly = Math.max(la.y, Math.max(l1.y, Math.max(l2.y, lb.y)));
			float mrx = Math.max(ra.x, Math.max(r1.x, Math.max(r2.x, rb.x)));
			float mry = Math.max(ra.y, Math.max(r1.y, Math.max(r2.y, rb.y)));

			float ilx = Math.min(la.x, Math.min(l1.x, Math.min(l2.x, lb.x)));
			float ily = Math.min(la.y, Math.min(l1.y, Math.min(l2.y, lb.y)));
			float irx = Math.min(ra.x, Math.min(r1.x, Math.min(r2.x, rb.x)));
			float iry = Math.min(ra.y, Math.min(r1.y, Math.min(r2.y, rb.y)));

			float lambda = mlx - ilx + mly - ily + mrx - irx + mry - iry;

			if ((mlx + fuzz >= irx) && (mly + fuzz >= iry) && (mrx + fuzz >= ilx) && (mry + fuzz >= ily)) {
				if (lambda < fuzz || index > maxIndex) {
					out.x = 0;
					out.y = 0;

					return true;
				}

				Vector2 l1n = new Vector2(l1);
				Vector2 l2n = new Vector2(l2);
				Vector2 r1n = new Vector2(r1);
				Vector2 r2n = new Vector2(r2);
				Vector2 ml = new Vector2();
				Vector2 mr = new Vector2();

				Vector2 c12l = new Vector2();
				Vector2 c12r = new Vector2();
				Vector2 c21l = new Vector2();
				Vector2 c21r = new Vector2();

				u.splitCubicFrame(la, l1n, l2n, lb, 0.5f, c12l, ml, c21l, t);
				u.splitCubicFrame(ra, r1n, r2n, rb, 0.5f, c12r, mr, c21r, t);

				if (intersectCubic(la, l1n, c12l, ml, ra, r1n, c12r, mr, out, index + 1)) {
					out.x *= 0.5f;
					out.y *= 0.5f;
					return true;
				}

				if (intersectCubic(la, l1n, c12l, ml, mr, c21r, r2n, rb, out, index + 1)) {
					out.x *= 0.5f;
					out.y = out.y * 0.5f + 0.5f;
					return true;
				}
				if (intersectCubic(ml, c21l, l2n, lb, mr, c21r, r2n, rb, out, index + 1)) {
					out.x = out.x * 0.5f + 0.5f;
					out.y = out.y * 0.5f + 0.5f;
					return true;
				}
				if (intersectCubic(ml, c21l, l2n, lb, ra, r1n, c12r, mr, out, index + 1)) {
					out.x = out.x * 0.5f + 0.5f;
					out.y = out.y * 0.5f;
					return true;
				}
			}
			return false;
		}
	}

	static public class Intersection_LineCubic {
		float fuzz = 1e-5f;

		int maxIndex = 10;

		LineUtils u = new LineUtils();

		Vector2 t = new Vector2();

		// out is a T
		// vector, not a
		// position
		public boolean intersectCubic(Vector2 la, Vector2 lb, Vector2 ra, Vector2 r1, Vector2 r2, Vector2 rb, Vector2 out, int index) {

			float mlx = Math.max(la.x, lb.x);
			float mly = Math.max(la.y, lb.y);
			float mrx = Math.max(ra.x, Math.max(r1.x, Math.max(r2.x, rb.x)));
			float mry = Math.max(ra.y, Math.max(r1.y, Math.max(r2.y, rb.y)));

			float ilx = Math.min(la.x, lb.x);
			float ily = Math.min(la.y, lb.y);
			float irx = Math.min(ra.x, Math.min(r1.x, Math.min(r2.x, rb.x)));
			float iry = Math.min(ra.y, Math.min(r1.y, Math.min(r2.y, rb.y)));

			float lambda = mlx - ilx + mly - ily + mrx - irx + mry - iry;

			if ((mlx + fuzz >= irx) && (mly + fuzz >= iry) && (mrx + fuzz >= ilx) && (mry + fuzz >= ily)) {
				if (lambda < fuzz || index > maxIndex) {
					out.x = 0;
					out.y = 0;

					return true;
				}

				Vector2 r1n = new Vector2(r1);
				Vector2 r2n = new Vector2(r2);
				Vector2 ml = new Vector2();
				Vector2 mr = new Vector2();

				Vector2 c12r = new Vector2();
				Vector2 c21r = new Vector2();

				ml.interpolate(la, lb, 0.5f);
				u.splitCubicFrame(ra, r1n, r2n, rb, 0.5f, c12r, mr, c21r, t);

				if (intersectCubic(la, ml, ra, r1n, c12r, mr, out, index + 1)) {
					out.x *= 0.5f;
					out.y *= 0.5f;
					return true;
				}

				if (intersectCubic(la, ml, mr, c21r, r2n, rb, out, index + 1)) {
					out.x *= 0.5f;
					out.y = out.y * 0.5f + 0.5f;
					return true;
				}
				if (intersectCubic(ml, lb, mr, c21r, r2n, rb, out, index + 1)) {
					out.x = out.x * 0.5f + 0.5f;
					out.y = out.y * 0.5f + 0.5f;
					return true;
				}
				if (intersectCubic(ml, lb, ra, r1n, c12r, mr, out, index + 1)) {
					out.x = out.x * 0.5f + 0.5f;
					out.y = out.y * 0.5f;
					return true;
				}
			}
			return false;
		}
	}

	static public class Intersection_LineLine {
		Vector2 t1 = new Vector2();

		Vector2 t2 = new Vector2();

		float fuzz = 1e-5f;

		public boolean intersectLine(Vector2 la, Vector2 lb, Vector2 ra, Vector2 rb, Vector2 out) {

			float mlx = Math.max(la.x, lb.x);
			float mly = Math.max(la.y, lb.y);
			float mrx = Math.max(ra.x, rb.x);
			float mry = Math.max(ra.y, rb.y);

			float ilx = Math.min(la.x, lb.x);
			float ily = Math.min(la.y, lb.y);
			float irx = Math.min(ra.x, rb.x);
			float iry = Math.min(ra.y, rb.y);

			float lambda = mlx - ilx + mly - ily + mrx - irx + mry - iry;

			if ((mlx + fuzz >= irx) && (mly + fuzz >= iry) && (mrx + fuzz >= ilx) && (mry + fuzz >= ily)) {

				t1.sub(lb, la);
				t2.sub(rb, ra);

				float m1 = t1.mag();
				float m2 = t2.mag();

				t1.normalize();
				t2.normalize();

				float r1r2 = la.dot(ra);
				float t1t2 = t1.dot(t2);

				float r1t1 = la.dot(t1);
				float r2t2 = ra.dot(t2);

				float r1t2 = la.dot(t2);
				float r2t1 = ra.dot(t1);

				float d1 = (r2t1 + r1t2 * t1t2 - r2t2 * t1t2 - r1t1) / (1 - t1t2 * t1t2);
				float d2 = r1t2 + d1 * t1t2 - r2t2;

				out.x = d1 / m1;
				out.y = d2 / m2;

				return out.x >= 0 && out.x <= 1 && out.y >= 0 && out.y <= 1;
			}
			return false;
		}
	}

	static public float tol = 0.5f;

	static public List<Vector2> intersect(CachedLine line, Vector2 a, Vector2 b) {
		List<Vector2> in = new ArrayList<Vector2>();
		for (int i = 1; i < line.events.size(); i++) {
			if (!line.events.get(i - 1).hasDestination())
				continue;

			Vector2 prev = line.events.get(i - 1).getDestination(null);
			if (line.events.get(i).method.equals(iLine_m.lineTo_m)) {
				Vector2 o = new Vector2();
				Intersection_LineLine ll = new Intersection_LineLine();
				if (ll.intersectLine(prev, line.events.get(i).getDestination(), a, b, o)) {
					Vector2 aa = new Vector2(a).interpolate(b, o.y);
					// if (in.size() == 0 ||
					// in.get(in.size() -
					// 1).distanceFrom(aa) > 1e-6)
					in.add(aa);
				}
			} else if (line.events.get(i).method.equals(iLine_m.cubicTo_m)) {
				Vector2 o = new Vector2();
				Intersection_LineCubic lc = new Intersection_LineCubic();

				if (lc.intersectCubic(a, b, prev, line.events.get(i).getAt(0), line.events.get(i).getAt(1), line.events.get(i).getAt(2), o, 0)) {
					Vector2 aa = new Vector2(a).interpolate(b, o.x);
					// if (in.size() == 0 ||
					// in.get(in.size() -
					// 1).distanceFrom(aa) > 1e-6)
					in.add(aa);
				}
			}
		}

		return in;
	}

	static public List<Pair<CachedLine.Event, CachedLine.Event>> intersectAndSubdivide(List<CachedLine> p1, List<CachedLine> p2, int limit) {

		ArrayList<Pair<Event, Event>> ret = new ArrayList<Pair<CachedLine.Event, CachedLine.Event>>();

		Intersection_CubicCubic cc = new Intersection_CubicCubic();
		Intersection_LineCubic lc = new Intersection_LineCubic();
		Intersection_LineLine ll = new Intersection_LineLine();

		int n = 0;
		boolean skipOut = true;
		while (skipOut) {
			for (CachedLine c1 : p1) {
				for (int i1 = 1; i1 < c1.events.size(); i1++) {
					skipOut = false;
					Event e1 = c1.events.get(i1);
					if (e1.method.equals(iLine_m.moveTo_m))
						continue;

					Event e1_last = c1.events.get(i1 - 1);

					if (i1 > limit)
						return ret;

					outer: for (CachedLine c2 : p2) {
						for (int i2 = 1; i2 < c2.events.size(); i2++) {

							Event e2 = c2.events.get(i2);
							Event e2_last = c2.events.get(i2 - 1);
							if (e2 == e1 || e2 == e1_last || e2_last == e1)
								continue;
							if (e2.method.equals(iLine_m.moveTo_m))
								continue;
							if (!e2.hasDestination())
								continue;
							if (!e1.hasDestination())
								continue;
							if (!e2_last.hasDestination())
								continue;
							if (!e1_last.hasDestination())
								continue;

							Event intersectionLeft = null;
							Event intersectionRight = null;

							if (e1.method.equals(iLine_m.cubicTo_m)) {
								if (e2.method.equals(iLine_m.cubicTo_m)) {

									Vector2 out = new Vector2();
									Vector2 c1_cx1 = e1.getAt(0);
									Vector2 c1_cx2 = e1.getAt(1);
									Vector2 b1 = e1.getAt(2);
									Vector2 a1 = e1_last.getDestination();
									Vector2 a2 = e2_last.getDestination();
									Vector2 c2_cx1 = e2.getAt(0);
									Vector2 c2_cx2 = e2.getAt(1);
									Vector2 b2 = e2.getAt(2);
									if (cc.intersectCubic(a1, c1_cx1, c1_cx2, b1, a2, c2_cx1, c2_cx2, b2, out, 0)) {

										if (out.x * a1.distanceFrom(b1) > tol && (1 - out.x) * a1.distanceFrom(b1) > tol) {
											Event e = c1.events.remove(i1);

											System.out.println(" cc intersection <" + out + "> <" + i1 + "/ <" + c1.events.size() + ", " + i2 + "/ " + c2.events.size() + ">");
											Vector2 c12 = new Vector2();
											Vector2 m = new Vector2();
											Vector2 c21 = new Vector2();
											Vector2 tmp = new Vector2();
											LineUtils.splitCubicFrame(a1, c1_cx1, c1_cx2, b1, out.x, c12, m, c21, tmp);

											Event ne1 = c1.new Event();
											ne1.method = iLine_m.cubicTo_m;
											if (e1.attributes != null) {
												ne1.attributes = new Dict();
												ne1.attributes.putAll(e1.getAttributes());
											}
											ne1.args = new Object[] { c1_cx1.x, c1_cx1.y, c12.x, c12.y, m.x, m.y };

											Event ne2 = c1.new Event();
											ne2.method = iLine_m.cubicTo_m;
											if (e1.attributes != null) {
												ne2.attributes = new Dict();
												ne2.attributes.putAll(e1.getAttributes());
											}
											ne2.args = new Object[] { c21.x, c21.y, c1_cx2.x, c1_cx2.y, b1.x, b1.y };

											System.out.println(" result of intersection <" + a1 + " " + c1_cx1 + " " + c12 + "  >>" + m + "<< " + " " + c21 + " " + c1_cx2 + " " + b1);

											c1.events.add(i1, ne1);
											c1.events.add(i1 + 1, ne2);

											intersectionLeft = ne1;

//											if (c2 == c1) {
//												if (i2 > i1)
//													i2 += 1;
//											}
											i1--;

											skipOut = true;
										}
//										else
//											intersectionLeft = e1_last;
										if (out.y * a2.distanceFrom(b2) > tol && (1 - out.y) * a2.distanceFrom(b2) > tol) {
											Event<?> x = c2.events.remove(i2);

											System.out.println(" cc intersection <" + out + " <" + i1 + "/ <" + c1.events.size() + ", " + i2 + "/ " + c2.events.size() + ">");

											Vector2 c12 = new Vector2();
											Vector2 m = new Vector2();
											Vector2 c21 = new Vector2();
											Vector2 tmp = new Vector2();
											LineUtils.splitCubicFrame(a2, c2_cx1, c2_cx2, b2, out.y, c12, m, c21, tmp);

											Event ne1 = c2.new Event();
											ne1.method = iLine_m.cubicTo_m;
											if (e2.attributes != null) {
												ne1.attributes = new Dict();
												ne1.attributes.putAll(e2.getAttributes());
											}
											ne1.args = new Object[] { c2_cx1.x, c2_cx1.y, c12.x, c12.y, m.x, m.y };

											Event ne2 = c2.new Event();
											ne2.method = iLine_m.cubicTo_m;
											if (e2.attributes != null) {
												ne2.attributes = new Dict();
												ne2.attributes.putAll(e2.getAttributes());
											}
											ne2.args = new Object[] { c21.x, c21.y, c2_cx2.x, c2_cx2.y, b2.x, b2.y };

											System.out.println(" result of intersection <" + a2 + " " + c2_cx1 + " " + c12 + "  >>" + m + "<< " + " " + c21 + " " + c2_cx2 + " " + b2);

											c2.events.add(i2, ne1);
											c2.events.add(i2 + 1, ne2);
											i2--;

											intersectionRight = ne1;

											skipOut = true;
										}
//										else
//											intersectionRight = e2_last;
									}

								} else {
									Vector2 out = new Vector2();
									Vector2 c1_cx1 = e1.getAt(0);
									Vector2 c1_cx2 = e1.getAt(1);
									Vector2 b1 = e1.getAt(2);
									Vector2 a1 = e1_last.getDestination();
									Vector2 a2 = e2_last.getDestination();
									Vector2 b2 = e2.getAt(0);
									if (lc.intersectCubic(a2, b2, a1, c1_cx1, c1_cx2, b1, out, 0)) {

										if (out.y * a1.distanceFrom(b1) > tol && (1 - out.y) * a1.distanceFrom(b1) > tol) {
											c1.events.remove(i1);

											Vector2 c12 = new Vector2();
											Vector2 m = new Vector2();
											Vector2 c21 = new Vector2();
											Vector2 tmp = new Vector2();
											LineUtils.splitCubicFrame(a1, c1_cx1, c1_cx2, b1, out.y, c12, m, c21, tmp);

											Event ne1 = c1.new Event();
											ne1.method = iLine_m.cubicTo_m;
											if (e1.attributes != null) {
												ne1.attributes = new Dict();
												ne1.attributes.putAll(e1.getAttributes());
											}
											ne1.args = new Object[] { c1_cx1.x, c1_cx1.y, c12.x, c12.y, m.x, m.y };

											Event ne2 = c1.new Event();
											ne2.method = iLine_m.cubicTo_m;
											if (e1.attributes != null) {
												ne2.attributes = new Dict();
												ne2.attributes.putAll(e1.getAttributes());
											}
											ne2.args = new Object[] { c21.x, c21.y, c1_cx2.x, c1_cx2.y, b1.x, b1.y };

											c1.events.add(i1, ne1);
											c1.events.add(i1 + 1, ne2);
//											if (c2 == c1) {
//												if (i2 > i1)
//													i2 += 1;
//											}

											intersectionLeft = ne1;

											i1--;
											skipOut = true;
										}
//										else
//											intersectionLeft = e1_last;
										if (out.x * a2.distanceFrom(b2) > tol && (1 - out.x) * a2.distanceFrom(b2) > tol) {
											c2.events.remove(i2);

											Vector2 m = new Vector2(a2).interpolate(b2, out.x);

											Event ne1 = c2.new Event();
											ne1.method = iLine_m.lineTo_m;
											if (e2.attributes != null) {
												ne1.attributes = new Dict();
												ne1.attributes.putAll(e2.getAttributes());
											}
											ne1.args = new Object[] { m.x, m.y };

											Event ne2 = c2.new Event();
											ne2.method = iLine_m.lineTo_m;
											if (e2.attributes != null) {
												ne2.attributes = new Dict();
												ne2.attributes.putAll(e2.getAttributes());
											}
											ne2.args = new Object[] { b2.x, b2.y };

											c2.events.add(i2, ne1);
											c2.events.add(i2 + 1, ne2);
											i2--;

											intersectionRight = ne1;

											skipOut = true;
										}
//										else
//											intersectionRight = e2_last;

									}
								}
							} else {
								if (e2.method.equals(iLine_m.cubicTo_m)) {

									Vector2 out = new Vector2();
									Vector2 b1 = e1.getAt(0);
									Vector2 a1 = e1_last.getDestination();
									Vector2 a2 = e2_last.getDestination();
									Vector2 c2_cx1 = e2.getAt(0);
									Vector2 c2_cx2 = e2.getAt(1);
									Vector2 b2 = e2.getAt(2);
									if (lc.intersectCubic(a1, b1, a2, c2_cx1, c2_cx2, b2, out, 0)) {

										if (out.x * a1.distanceFrom(b1) > tol && (1 - out.x) * a1.distanceFrom(b1) > tol) {
											c1.events.remove(i1);

											Vector2 m = new Vector2(a1).interpolate(b1, out.x);

											Event ne1 = c1.new Event();
											ne1.method = iLine_m.lineTo_m;
											if (e1.attributes != null) {
												ne1.attributes = new Dict();
												ne1.attributes.putAll(e1.getAttributes());
											}
											ne1.args = new Object[] { m.x, m.y };

											Event ne2 = c1.new Event();
											ne2.method = iLine_m.lineTo_m;
											if (e1.attributes != null) {
												ne2.attributes = new Dict();
												ne2.attributes.putAll(e1.getAttributes());
											}
											ne2.args = new Object[] { b1.x, b1.y };

											c1.events.add(i1, ne1);
											c1.events.add(i1 + 1, ne2);
//											if (c2 == c1) {
//												if (i2 > i1)
//													i2 += 1;
//											}
											intersectionLeft = ne1;
											i1--;
											skipOut = true;
										}
//										else
//											intersectionLeft = e1_last;
										if (out.y * a2.distanceFrom(b2) > tol && (1 - out.y) * a2.distanceFrom(b2) > tol) {
											c2.events.remove(i2);
	
											Vector2 c12 = new Vector2();
											Vector2 m = new Vector2();
											Vector2 c21 = new Vector2();
											Vector2 tmp = new Vector2();
											LineUtils.splitCubicFrame(a2, c2_cx1, c2_cx2, b2, out.y, c12, m, c21, tmp);

											Event ne1 = c2.new Event();
											ne1.method = iLine_m.cubicTo_m;
											if (e2.attributes != null) {
												ne1.attributes = new Dict();
												ne1.attributes.putAll(e2.getAttributes());
											}
											ne1.args = new Object[] { c2_cx1.x, c2_cx1.y, c12.x, c12.y, m.x, m.y };

											Event ne2 = c2.new Event();
											ne2.method = iLine_m.cubicTo_m;
											if (e2.attributes != null) {
												ne2.attributes = new Dict();
												ne2.attributes.putAll(e2.getAttributes());
											}
											ne2.args = new Object[] { c21.x, c21.y, c2_cx2.x, c2_cx2.y, b2.x, b2.y };

											c2.events.add(i2, ne1);
											c2.events.add(i2 + 1, ne2);
											intersectionRight = ne1;

											i2--;
											skipOut = true;
										}
//										else
//											intersectionRight = e2_last;
									}
								} else {
									Vector2 out = new Vector2();
									Vector2 b1 = e1.getAt(0);
									Vector2 a1 = e1_last.getDestination();
									Vector2 a2 = e2_last.getDestination();
									Vector2 b2 = e2.getAt(0);
									if (ll.intersectLine(a1, b1, a2, b2, out)) {

										if (out.x * a1.distanceFrom(b1) > tol && (1 - out.x) * a1.distanceFrom(b1) > tol) {
											c1.events.remove(i1);

											Vector2 m = new Vector2(a1).interpolate(b1, out.x);

											Event ne1 = c1.new Event();
											ne1.method = iLine_m.lineTo_m;
											if (e1.attributes != null) {
												ne1.attributes = new Dict();
												ne1.attributes.putAll(e1.getAttributes());
											}
											ne1.args = new Object[] { m.x, m.y };

											Event ne2 = c1.new Event();
											ne2.method = iLine_m.lineTo_m;
											if (e1.attributes != null) {
												ne2.attributes = new Dict();
												ne2.attributes.putAll(e1.getAttributes());
											}
											ne2.args = new Object[] { b1.x, b1.y };

											c1.events.add(i1, ne1);
											c1.events.add(i1 + 1, ne2);
//											if (c2 == c1) {
//												if (i2 > i1)
//													i2 += 1;
//											}
											intersectionLeft = ne1;
											i1--;
											skipOut = true;
										}
//										else
//											intersectionLeft = e1_last;

										if (out.y * a2.distanceFrom(b2) > tol && (1 - out.y) * a2.distanceFrom(b2) > tol) {
											c2.events.remove(i2);

											Vector2 m = new Vector2(a2).interpolate(b2, out.y);

											Event ne1 = c2.new Event();
											ne1.method = iLine_m.lineTo_m;
											if (e2.attributes != null) {
												ne1.attributes = new Dict();
												ne1.attributes.putAll(e2.getAttributes());
											}
											ne1.args = new Object[] { m.x, m.y };

											Event ne2 = c2.new Event();
											ne2.method = iLine_m.lineTo_m;
											if (e2.attributes != null) {
												ne2.attributes = new Dict();
												ne2.attributes.putAll(e2.getAttributes());
											}
											ne2.args = new Object[] { b2.x, b2.y };

											c2.events.add(i2, ne1);
											c2.events.add(i2 + 1, ne2);
											intersectionRight = ne1;
											i2--;
											skipOut = true;
										}
//										else
//											intersectionRight = e2_last;

									}
								}
							}

//							System.out.println(" left :"+intersectionLeft+" "+intersectionRight);

							if (intersectionLeft!=null && intersectionRight!=null)
								ret.add(new Pair<Event, Event>(intersectionLeft, intersectionRight));
							if (intersectionLeft == null ^ intersectionRight==null)
								System.err.println(" warning: intersection invarient broken ? ");

							if (skipOut)
								break outer;
						}
					}

				}
			}

			skipOut = false;
		}
		return ret;
	}

	static public Event resolve(final Event left) {
		if (left.container.events.contains(left))
			return left;

		return ReflectionTools.argMin(left.container.events, new Object() {
			public float f(Event e) {
				if (e.hasDestination())
					return e.getDestination().distanceFrom(left.getDestination());
				else
					return Float.POSITIVE_INFINITY;
			}
		});
	}

}
