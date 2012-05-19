package field.math.linalg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import field.math.abstraction.iBlendable;
import field.math.abstraction.iInplaceProvider;
import field.math.abstraction.iProvider;

// need to turn this into QuatVec again, the math into and out of matrix is unstable and expensive
public class CoordinateFrame implements iCoordinateFrame.iMutable, iInplaceProvider<iCoordinateFrame.iMutable>, iBlendable<CoordinateFrame>, Serializable {

	static final long serialVersionUID = 5132350781464648322L;

	enum Dirt {
		clean, matrixDirty, trsDirty;
	}

	Vector3 translation = new Vector3();

	Quaternion rotation = new Quaternion();

	Vector3 scale = new Vector3(1, 1, 1);

	Matrix4 matrix = new Matrix4().setIdentity();

	Dirt dirt = Dirt.clean;

	boolean isUnityScale = false;

	public CoordinateFrame() {
	}

	public CoordinateFrame(Matrix4 m4) {
		this.matrix = new Matrix4(m4);
		dirt = Dirt.trsDirty;
		cleanTRS();
		isUnityScale = scale.mag() == 1;
	}

	public CoordinateFrame(Quaternion rotation, Vector3 translation, Vector3 scale) {
		this.setRotation(rotation);
		this.setTranslation(translation);
		this.setScale(scale);

		isUnityScale = scale.mag() == 1;
	}

	public CoordinateFrame(Quaternion rotation, Vector3 translation) {
		this.setRotation(rotation);
		this.setTranslation(translation);
		isUnityScale = true;
	}

	public CoordinateFrame blendRepresentation_newZero() {
		return new CoordinateFrame();

	}

	public CoordinateFrame cerp(CoordinateFrame before, CoordinateFrame now, CoordinateFrame next, CoordinateFrame after, float a) {
		before.cleanTRS();
		now.cleanTRS();
		next.cleanTRS();
		after.cleanTRS();

		rotation.interpolate(now.rotation, next.rotation, a);
		translation.cerp(before.translation, 0, now.translation, 0, next.translation, 0, after.translation, 0, a);
		scale.cerp(before.scale, 0, now.scale, 0, next.scale, 0, after.scale, 0, a);
		dirt = Dirt.matrixDirty;
		return this;
	}

	public CoordinateFrame cerp(CoordinateFrame before, float beforeTime, CoordinateFrame now, float nowTime, CoordinateFrame next, float nextTime, CoordinateFrame after, float afterTime, float a) {
		before.cleanTRS();
		now.cleanTRS();
		next.cleanTRS();
		after.cleanTRS();

		rotation.interpolate(new Quaternion(now.rotation), new Quaternion(next.rotation), a);

		// doesn't work

		// Quaternion t1
		// = new
		// Quaternion();
		// Quaternion t2
		// = new
		// Quaternion();
		// Quaternion t3
		// = new
		// Quaternion();
		//
		// Quaternion a1
		// = new
		// Quaternion();
		// Quaternion b1
		// = new
		// Quaternion();
		//
		// Quaternion.computeA(before.rotation,
		// now.rotation,
		// next.rotation,
		// a1,
		// t1,t2,t3);
		// Quaternion.computeA(now.rotation,
		// next.rotation,
		// after.rotation,
		// b1,
		// t1,t2,t3);
		//
		// Quaternion.squad(now.rotation,
		// a1, b1,
		// next.rotation,
		// a, t1,t2,
		// t3);
		//
		// rotation.set(t3);

		translation.cerp(before.translation, 0, now.translation, 0, next.translation, 0, after.translation, 0, a);
		scale.cerp(before.scale, 0, now.scale, 0, next.scale, 0, after.scale, 0, a);
		dirt = Dirt.matrixDirty;

		return this;
	}

	public CoordinateFrame duplicate() {
		CoordinateFrame cf = new CoordinateFrame();
		cf.rotation.set(rotation);
		cf.translation.set(translation);
		cf.scale.set(scale);
		cf.matrix.set(matrix);
		cf.dirt = dirt;
		cf.isUnityScale = isUnityScale;
		return cf;
	}

	public iCoordinateFrame.iMutable get(iCoordinateFrame.iMutable inplace) {
		if (inplace == null)
			return new CoordinateFrame(this.rotation, this.translation, this.scale);
		return inplace.setRotation(rotation).setTranslation(translation).setScale(scale);
	}

	public Matrix4 getMatrix(Matrix4 matrix) {
		cleanMatrix();
		if (matrix == null)
			matrix = new Matrix4();
		matrix.set(this.matrix);
		return matrix;
	}

	public Quaternion getRotation(Quaternion rotation) {
		cleanTRS();
		if (rotation == null)
			rotation = new Quaternion();
		return rotation.set(this.rotation);
	}

	public Vector3 getScale(Vector3 scale) {
		cleanTRS();
		if (scale == null)
			scale = new Vector3();
		return scale.set(this.scale);
	}

	public Vector3 getTranslation(Vector3 translation) {
		cleanTRS();
		if (translation == null)
			translation = new Vector3();
		return translation.set(this.translation);
	}

	public CoordinateFrame invert() {
		cleanMatrix();
		matrix.invert();
		dirt = Dirt.trsDirty;
		return this;
	}

	public CoordinateFrame lerp(CoordinateFrame before, CoordinateFrame now, float a) {
		before.cleanTRS();
		now.cleanTRS();
		rotation.interpolate(before.rotation, now.rotation, a);
		translation.lerp(before.translation, now.translation, a);
		scale.lerp(before.scale, now.scale, a);
		dirt = Dirt.matrixDirty;
		return this;
	}

	public CoordinateFrame multiply(iCoordinateFrame left, iCoordinateFrame right) {
		
		
		
		if (left instanceof CoordinateFrame && right instanceof CoordinateFrame) {
			if (left != this && right != this) {
				if (((CoordinateFrame) left).isUnityScale && ((CoordinateFrame) right).isUnityScale) {

					((CoordinateFrame) left).cleanTRS();
					((CoordinateFrame) right).cleanTRS();

					rotation.mul(((CoordinateFrame) left).rotation, ((CoordinateFrame) right).rotation);
					translation.setValue(((CoordinateFrame) right).translation);
					((CoordinateFrame) left).rotation.rotateVector(translation);
					translation.add(translation, ((CoordinateFrame) left).translation);

					dirt = Dirt.matrixDirty;
				} else {

					((CoordinateFrame) left).cleanMatrix();
					((CoordinateFrame) right).cleanMatrix();
					this.matrix.mul(((CoordinateFrame) left).matrix, ((CoordinateFrame) right).matrix);

					this.dirt = Dirt.trsDirty;
					this.isUnityScale = ((CoordinateFrame) left).isUnityScale && ((CoordinateFrame) right).isUnityScale;
					this.cleanTRS();

					this.isUnityScale = Math.abs(scale.mag() - 1) < 1e-20;
				}
			} else {
				matrix.mul(left.getMatrix(null), right.getMatrix(null));
				dirt = Dirt.trsDirty;
				this.isUnityScale = false;
				this.cleanTRS();

				this.isUnityScale = Math.abs(scale.mag() - 1) < 1e-20;

			}
		} else {
			matrix.mul(left.getMatrix(null), right.getMatrix(null));
			this.dirt = Dirt.trsDirty;
			this.cleanTRS();

			this.isUnityScale = Math.abs(scale.mag() - 1) < 1e-20;
			this.isUnityScale = false;
		}
		return this;
	}

	public iProvider<Vector3> position() {
		return new iProvider<Vector3>() {
			public Vector3 get() {
				return getTranslation(null);
			}
		};
	}

	public CoordinateFrame setRotation(Quaternion rotation) {
		cleanTRS();
		this.rotation.set(rotation);
		return this;
	}

	public CoordinateFrame setScale(Vector3 scale) {
		cleanTRS();
		this.scale.set(scale);
		dirt = Dirt.matrixDirty;
		isUnityScale = scale.mag() == 1;
		return this;
	}

	public CoordinateFrame setTranslation(Vector3 translation) {
		cleanTRS();
		this.translation.set(translation);
		dirt = Dirt.matrixDirty;
		return this;
	}

	public CoordinateFrame setValue(CoordinateFrame to) {
		to.getRotation(rotation);
		to.getScale(scale);
		to.getTranslation(translation);
		dirt = Dirt.matrixDirty;
		return this;
	}

	@Override
	public String toString() {
		cleanTRS();
		return "[cf: r" + rotation + " t" + translation + (((scale.x == 1) && (scale.y == 1) && (scale.z == 1)) ? "" : " s" + scale) + "]";
	}

	public Vector3 transformDirection(Vector3 position) {
		cleanMatrix();
		return matrix.transformDirection(position);
	}

	public Vector3 transformPosition(Vector3 position) {
		cleanMatrix();
		return matrix.transformPosition(position);
	}

	private void cleanMatrix() {
		if (dirt == Dirt.matrixDirty) {

			rotation.normalize();

			matrix.set(rotation, translation, scale);
			dirt = Dirt.clean;
		}
	}

	private void cleanTRS() {
		if (dirt == Dirt.trsDirty) {
			matrix.get(rotation, translation, scale);

			rotation.normalize();

			dirt = Dirt.clean;
		}
	}
	
	static public CoordinateFrame blend(List<CoordinateFrame> c, List<? extends Number> w)
	{
		List<Quaternion> q = new ArrayList<Quaternion>();
		List<Vector3> t = new ArrayList<Vector3>();
		List<Vector3> s = new ArrayList<Vector3>();
		for(CoordinateFrame f : c)
		{
			if (f == null) return null;
			q.add(f.getRotation(null));
			t.add(f.getTranslation(null));
			s.add(f.getScale(null));
		}
		
		CoordinateFrame f = new CoordinateFrame(Quaternion.blend(q, w),Vector3.blend(t, w),Vector3.blend(s, w)); 
		return f;
	}

	public void forceTRS() {
		cleanTRS();
		isUnityScale = true;
	}
}
