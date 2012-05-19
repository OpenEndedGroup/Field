package field.graphics.core;

import field.graphics.core.Base.iSceneListElement;
import field.math.linalg.Vector3;

public interface iBasicCamera {

	/**
	 * aspect is width / height
	 */
	public abstract iSceneListElement setAspect(float f);

	/**
	 * sets the fov (IN ANGLES), aspect ratio (width/height), near and far clipping planes for use by gluPerspective().
	 */
	public abstract void setPerspective(float fov, float aspect, double e, float far);

	public abstract Vector3 unProject(double x, double y, double distance);

	/**
	 * sets the location of the camera
	 */
	public abstract void setPosition(Vector3 position);

	/**
	 * inplace method for getting the position of the camera (inplane created on demand)
	 */
	public abstract Vector3 getPosition(Vector3 inplace);

	/**
	 * sets the lookAt point for the camera
	 */
	public abstract void setLookAt(Vector3 lookAt);

	/**
	 * 
	 * inplace method for getting the camera's lookAt (inplane created on demand)
	 */
	public abstract Vector3 getLookAt(Vector3 inplace);

	/**
	 * 
	 * sets the camera's up vector
	 */
	public abstract void setUp(Vector3 up);

	/**
	 * inplace method for getting the camera's up vector (inplane created on demand)
	 */
	public abstract Vector3 getUp(Vector3 inplace);

	public abstract void setFrustrumShift(float rightShift, float topShift);

	public abstract void setFrustrumMul(float frustrumMul);

	/**
	 * @return
	 */
	public abstract float getAspect();

	/**
	 * @return
	 */
	public abstract float getFar();

	/**
	 * @return
	 */
	public abstract float getFov();

	/**
	 * @return
	 */
	public abstract float getNear();

	/**
	 * @param minIntersectsAt
	 */
	public abstract Vector3 project3(Vector3 worldPt);

	public abstract void setFOV(float f);

	public abstract float getFrustrumShiftX();

	public abstract float getFrustrumShiftY();

	public abstract float getFrustrumMul();

}