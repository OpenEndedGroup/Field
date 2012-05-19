package field.math.linalg;

import java.lang.reflect.Method;

import field.namespace.generic.ReflectionTools;


public interface iCoordinateFrame {

	public Quaternion getRotation(Quaternion rotation);
	public Vector3 getTranslation(Vector3 translation);
	public Vector3 getScale(Vector3 scale);
	public Matrix4 getMatrix(Matrix4 matrix);
	
	public Vector3 transformPosition(Vector3 position);
	public Vector3 transformDirection(Vector3 position);
	
	public iCoordinateFrame invert();
	public iCoordinateFrame duplicate();
	
	public iCoordinateFrame multiply(iCoordinateFrame left, iCoordinateFrame right);
	
	public interface iMutable extends iCoordinateFrame
	{
		static public Method method_setRotation = ReflectionTools.methodOf("setRotation", iMutable.class, Quaternion.class);
		static public Method method_setTranslation = ReflectionTools.methodOf("setTranslation", iMutable.class, Quaternion.class);
		static public Method method_setScale = ReflectionTools.methodOf("setScale", iMutable.class, Quaternion.class);

		public iMutable setRotation(Quaternion rotation);
		public iMutable setTranslation(Vector3 translation);
		public iMutable setScale(Vector3 scale);
	}
}
