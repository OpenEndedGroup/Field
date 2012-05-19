package field.math.util;

public class Brent {

	public interface F {
		public double f(double x);
	}

	public static double fmin(double a, double b, F minclass, double tol) {

		double c, d, e, eps, xm, p, q, r, tol1, t2, u, v, w, fu, fv, fw, fx, x, tol3;

		c = .5 * (3.0 - Math.sqrt(5.0));
		d = 0.0;

		// 1.1102e-16 is machine precision

		eps = 1.2e-16;
		tol1 = eps + 1.0;
		eps = Math.sqrt(eps);

		v = a + c * (b - a);
		w = v;
		x = v;
		e = 0.0;
		fx = minclass.f(x);
		fv = fx;
		fw = fx;
		tol3 = tol / 3.0;

		xm = .5 * (a + b);
		tol1 = eps * Math.abs(x) + tol3;
		t2 = 2.0 * tol1;

		// main loop

		
		double lx = -10;
		
		int stalled = 0;
		
		while (Math.abs(x - xm) > (t2 - .5 * (b - a))) {

			p = q = r = 0.0;

			if (Math.abs(e) > tol1) {

				// fit the parabola

				r = (x - w) * (fx - fv);
				q = (x - v) * (fx - fw);
				p = (x - v) * q - (x - w) * r;
				q = 2.0 * (q - r);

				if (q > 0.0) {
					p = -p;
				} else {
					q = -q;
				}

				r = e;
				e = d;

				// brace below corresponds to statement 50
			}

			if ((Math.abs(p) < Math.abs(.5 * q * r)) && (p > q * (a - x)) && (p < q * (b - x))) {

				// a parabolic interpolation step

				d = p / q;
				u = x + d;

				// f must not be evaluated too close to a or b

				if (((u - a) < t2) || ((b - u) < t2)) {

					d = tol1;
					if (x >= xm) d = -d;

				}

				// brace below corresponds to statement 60
			} else {

				// a golden-section step

				if (x < xm) {

					e = b - x;

				} else {

					e = a - x;

				}

				d = c * e;

			}

			// f must not be evaluated too close to x
			if (Math.abs(d) >= tol1) {

				u = x + d;

			} else {

				if (d > 0.0) {

					u = x + tol1;

				} else {

					u = x - tol1;

				}

			}

			fu = minclass.f(u);

			// Update a, b, v, w, and x

			if (fx <= fu) {
				if (u < x) {
					a = u;
				} else {
					b = u;
				}
			}

			if (fu <= fx) {
				if (u < x) {
					b = x;
				} else {
					a = x;
				}

				v = w;
				fv = fw;
				w = x;
				fw = fx;
				x = u;
				fx = fu;

				xm = .5 * (a + b);
				tol1 = eps * Math.abs(x) + tol3;
				t2 = 2.0 * tol1;

				// brace below corresponds to statement 170
			} else {
				if ((fu <= fw) || (w == x)) {

					v = w;
					fv = fw;
					w = u;
					fw = fu;

					xm = .5 * (a + b);
					tol1 = eps * Math.abs(x) + tol3;
					t2 = 2.0 * tol1;
				} else if ((fu > fv) && (v != x) && (v != w)) {

					xm = .5 * (a + b);
					tol1 = eps * Math.abs(x) + tol3;
					t2 = 2.0 * tol1;
				} else {

					v = u;
					fv = fu;

					xm = .5 * (a + b);
					tol1 = eps * Math.abs(x) + tol3;
					t2 = 2.0 * tol1;
				}
			}
			
			
		//	System.out.println(" lx"+lx+" "+x);
			
			if (x==lx) if (stalled++>5) break;
			lx = x;
		}
		return x;
	}

}
