package field.math.util;

import field.math.linalg.Vector2;
import field.math.linalg.Vector3;

public class Circumcenter {

	public Vector3 circumcenterOf(Vector3 A, Vector3 B, Vector3 C) {
		Vector3 r = new Vector3();
		if (A.z == 0.0 && B.z == 0.0 && C.z == 0.0) {
			double u = ((A.x - B.x) * (A.x + B.x) + (A.y - B.y) * (A.y + B.y)) / 2.0;
			double v = ((B.x - C.x) * (B.x + C.x) + (B.y - C.y) * (B.y + C.y)) / 2.0;
			double den = (A.x - B.x) * (B.y - C.y) - (B.x - C.x) * (A.y - B.y);
			r.set(0, (float) ((u * (B.y - C.y) - v * (A.y - B.y)) / den));
			r.set(1, (float) ((v * (A.x - B.x) - u * (B.x - C.x)) / den));
			r.set(2, (float) 0.0);
		} else {
			Vector3 BmA = new Vector3();
			Vector3 CmA = new Vector3();
			BmA.sub(B, A);
			CmA.sub(C, A);

			double BC = BmA.dot(CmA);
			double B2 = BmA.magSquared(), C2 = CmA.magSquared();
			double den = 2.0 * (B2 * C2 - BC * BC);
			double s = C2 * (B2 - BC) / den;
			double t = B2 * (C2 - BC) / den;

			Vector3.add(BmA, (float) s, A, r);
			Vector3.add(CmA, (float) t, r, r);
		}
		return r;
	}

	public Vector2 circumcenterOf(Vector2 A, Vector2 B, Vector2 C) {
		Vector2 r = new Vector2();

		Vector2 BmA = new Vector2();
		Vector2 CmA = new Vector2();
		BmA.sub(B, A);
		CmA.sub(C, A);

		double BC = BmA.dot(CmA);
		double B2 = BmA.mag()*BmA.mag(), C2 = CmA.mag()*CmA.mag();
		double den = 2.0 * (B2 * C2 - BC * BC);
		double s = C2 * (B2 - BC) / den;
		double t = B2 * (C2 - BC) / den;

		Vector2.add(BmA, (float) s, A, r);
		Vector2.add(CmA, (float) t, r, r);

		return r;
	}

}
