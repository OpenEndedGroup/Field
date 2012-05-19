package field.math.linalg;

import java.util.ArrayList;
import java.util.List;

import field.math.BaseMath;


/**
 help me
 */
public class RotationTools {
	static private Quaternion output;

	static public double angleSubtract(double a, double b) {
		if (a - b > Math.PI) {
			return a - b - Math.PI * 2;
		}
		if (a - b < -Math.PI) {
			return a - b + Math.PI * 2;
		} else {
			return a - b;
		}
	}

	static public double findEulerZRotation(Quaternion of) {
		Vector3 eulerN = new Vector3();

		// commented out as a service to bruce
		//    sketches.action.simple.pup.math.QuatEulerConversion.quatToEuler(of,eulerN);
		return eulerN.z;
	}



	// could be faster
	static public void rollAngles(List l1) {
		float largestInterval = Float.NEGATIVE_INFINITY;
		int index = 0;
		for (int i = 0; i < l1.size(); i++) {
			float f1 = ((Number) l1.get(i)).floatValue();
			float f2 = ((Number) l1.get((i + 1) % l1.size())).floatValue();
			float n = (float) angleSubtract(f1, f2);
			if (Math.abs(n) > largestInterval) {
				largestInterval = Math.abs(n);
				index = i;
			}
		}

		// break should go at i
		if (index != l1.size() - 1)
			for (int n = 0; n < index + 1; n++) {
				l1.add(l1.remove(0));
			}
	}

	
	/**
	 finds the rotation around z that this quaternion does
	 */
	static public double findForwardRotation(Quaternion of) {
		Vector3 negY = new Vector3(0, -1, 0);
		Vector3 rot = new Vector3();
		of.rotateVector(negY, rot);


		// project onto x,y plane
		rot.set(2, 0);

		double h = Math.sqrt(rot.get(1) * rot.get(1) + rot.get(0) * rot.get(0));

		// signed angle between them is now

		double theta = Math.abs(BaseMath.acos(-rot.get(1) / h));

		// need the sign
		if (rot.get(0) < 0)
			theta *= -1;

		return theta;
	}
	/**
	 rotates this quaternion by a z rotation
	 */
	static public void rotateThisMoreZ(Quaternion me, double z) {
		Quaternion zRotation = new Quaternion().set(new Vector3(0, 0, 1), (float) z);
		Quaternion output = new Quaternion();
		output.mul(zRotation, me);
		me.set(output);
	}

	/**

	 makes this quaternion rotate around z this much

	 */
	static public void setYRotationOfQuaternion(Quaternion me, double z) {
		double oz = findYRotation(me);
		double diff = angleSubtract(z, oz);
		rotateThisMoreY(me, diff);
	}
	
	/**
	 finds the rotation around z that this quaternion does
	 */
	static public double findYRotation(Quaternion of) {
		Vector3 negY = new Vector3(0, 0, -1);
		Vector3 rot = new Vector3();
		of.rotateVector(negY, rot);


		// project onto x,z plane
		rot.set(1, 0);

//		double h = Math.sqrt(rot.get(2) * rot.get(2) + rot.get(0) * rot.get(0));

//		// signed angle between them is now
//		double theta = Math.abs(BaseMath.acos(-rot.get(2) / h));
//
//		// need the sign
//		if (rot.get(2) < 0)
//			theta *= -1;

		double theta = Math.atan2(rot.x, -rot.z);
		
		return theta;
	}
	/**
	 rotates this quaternion by a z rotation
	 */
	static public void rotateThisMoreY(Quaternion me, double z) {
		Quaternion zRotation = new Quaternion().set(new Vector3(0, 1, 0), -(float) z);
		Quaternion output = new Quaternion();
		output.mul(zRotation, me);
		me.set(output);
	}

	static public void rotateThisMoreY(Vector3 me, double z) {
		Quaternion zRotation = new Quaternion().set(new Vector3(0, 1, 0), -(float) z);
		me.setValue(zRotation.rotateVector(me));
	}

	/**

	 makes this quaternion rotate around z this much

	 */
	static public void setZRotationOfQuaternion(Quaternion me, double z) {
		double oz = findForwardRotation(me);
		double diff = angleSubtract(z, oz);
		rotateThisMoreZ(me, diff);
	}

	// O(N^2)
	static public void sortAngles(List l1) {
		if (l1.size() == 0)
			return;

		boolean cont = true;

		ArrayList sorted = new ArrayList();
		Number n = (Number) l1.remove(0);
		float at = n.floatValue();
		sorted.add(n);

		while (cont) {
			float c = Float.POSITIVE_INFINITY;
			int index = -1;
			for (int i = 0; i < l1.size(); i++) {
				float d = (float) RotationTools.angleSubtract(((Number) l1.get(i)).floatValue(), at);
				if (d > 0) {
					if (d < c) {
						c = d;
						index = i;
					}
				} else {
					if (d + Math.PI * 2 < c) {
						c = (float) (d + Math.PI * 2);
						index = i;
					}
				}
			}
			if (index == -1)
				break;
			n = (Number) l1.remove(index);
			at = (n).floatValue();
			sorted.add(n);
		}

		l1.clear();
		l1.addAll(sorted);
	}

}