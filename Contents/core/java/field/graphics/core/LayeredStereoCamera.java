package field.graphics.core;

import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_TEXTURE;
import static org.lwjgl.opengl.GL11.glFrustum;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.util.Random;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import field.graphics.windowing.FullScreenCanvasSWT;
import field.launch.SystemProperties;
import field.math.linalg.Vector3;

public class LayeredStereoCamera extends BasicCamera {

	// there are two ways of doing stereo, modifying the position of the
	// camera and angling the cameras in
	// or translating the frustum a little

	float io_frustra = 0.0f;

	Vector3 io_position = new Vector3();

	float io_lookat = 0.0f;

	boolean noStereo = SystemProperties.getIntProperty("zeroStereo", 0) == 1;

	float multiplyDisparity = (float) SystemProperties.getDoubleProperty("multiplyDisparity", 1);
	float multiplyFust= (float) SystemProperties.getDoubleProperty("multiplyFust", 1);

	public LayeredStereoCamera setIOFrustra(float i) {
		this.io_frustra = i;
		return this;
	}

	public LayeredStereoCamera setIOPosition(Vector3 v) {
		this.io_position = new Vector3(v);
		return this;
	}

	public LayeredStereoCamera setIOPosition(float m) {
		this.io_position = new Vector3(m, m, m);
		return this;
	}

	public LayeredStereoCamera setIOLookAt(float x) {
		this.io_lookat = x;
		return this;
	}

	public float getIOLookAt() {
		return io_lookat;
	}

	static float flipped = (SystemProperties.getIntProperty("stereoEyeFlipped", 0) == 1 ? -1 : 1);
	static boolean passive = (SystemProperties.getIntProperty("passiveStereo", 0) == 1);

	double disparityPerDistance = SystemProperties.getDoubleProperty("defaultDisparityPerDistance", 0);

	boolean texture0IsRight = false;

	public float[] previousModelView;

	float extraAmount = 1;

	@Override
	public void performPass() {
		pre();

		previousModelView = new float[16];
		System.arraycopy(CoreHelpers.projection.head, 0, previousModelView, 0, 16);
		
		boolean wasDirty = projectionDirty || modelViewDirty;
		{
			glViewport(oX, oY, width, height);
			float right = (float) (near * Math.tan((Math.PI * fov / 180f) / 2) * aspect) * frustrumMul*multiplyFust;
			float top = (float) (near * Math.tan((Math.PI * fov / 180f) / 2)) * frustrumMul*multiplyFust;
			
			float x = flipped * io_frustra ;
			
			if (noStereo)
				x = 0;
			
			CoreHelpers.glMatrixMode(CoreHelpers.PROJECTION_0);
			CoreHelpers.glLoadIdentity();
			CoreHelpers.glFrustum(-right + (right * (rshift + FullScreenCanvasSWT.currentCanvas.extraShiftX * extraAmount + x)), right + right * (rshift + FullScreenCanvasSWT.currentCanvas.extraShiftX * extraAmount + x), -top + top * tshift, top + top * tshift, near, far);
			
			x *= -1;
			CoreHelpers.glMatrixMode(CoreHelpers.PROJECTION_1);
			CoreHelpers.glLoadIdentity();
			CoreHelpers.glFrustum(-right + (right * (rshift + FullScreenCanvasSWT.currentCanvas.extraShiftX * extraAmount + x)), right + right * (rshift + FullScreenCanvasSWT.currentCanvas.extraShiftX * extraAmount + x), -top + top * tshift, top + top * tshift, near, far);

			x *= 0;
			CoreHelpers.glMatrixMode(GL11.GL_PROJECTION);
			CoreHelpers.glLoadIdentity();
			CoreHelpers.glFrustum(-right + (right * (rshift + FullScreenCanvasSWT.currentCanvas.extraShiftX * extraAmount + x)), right + right * (rshift + FullScreenCanvasSWT.currentCanvas.extraShiftX * extraAmount + x), -top + top * tshift, top + top * tshift, near, far);

			CoreHelpers.glMatrixMode(GL_MODELVIEW);

			projectionDirty = false;
		}

		{
			
			Vector3 left = new Vector3().cross(getViewRay(null), getUp(null)).normalize();
			Vector3 io_position = new Vector3(this.io_position);
			io_position.x += disparityPerDistance * lookAt.distanceFrom(position);
			io_position.y += disparityPerDistance * lookAt.distanceFrom(position);
			io_position.z += disparityPerDistance * lookAt.distanceFrom(position);
			io_position.scale(multiplyDisparity);

			float x = 1;

			CoreHelpers.glMatrixMode(CoreHelpers.MODELVIEW_0);
			CoreHelpers.glLoadIdentity();
			CoreHelpers.gluLookAt(position.x + flipped * (io_position.x) * x * left.x, position.y + flipped * io_position.y * x * left.y, position.z + flipped * io_position.z * x * left.z, lookAt.x + flipped * io_lookat * x * left.x, lookAt.y + flipped * io_lookat * x * left.y, lookAt.z + flipped * io_lookat * x * left.z, up.x, up.y, up.z);

			x *= -1;
			CoreHelpers.glMatrixMode(CoreHelpers.MODELVIEW_1);
			CoreHelpers.glLoadIdentity();
			CoreHelpers.gluLookAt(position.x + flipped * (io_position.x) * x * left.x, position.y + flipped * io_position.y * x * left.y, position.z + flipped * io_position.z * x * left.z, lookAt.x + flipped * io_lookat * x * left.x, lookAt.y + flipped * io_lookat * x * left.y, lookAt.z + flipped * io_lookat * x * left.z, up.x, up.y, up.z);

			x *= 0;
			CoreHelpers.glMatrixMode(GL11.GL_MODELVIEW);
			CoreHelpers.glLoadIdentity();
			CoreHelpers.gluLookAt(position.x + flipped * (io_position.x) * x * left.x, position.y + flipped * io_position.y * x * left.y, position.z + flipped * io_position.z * x * left.z, lookAt.x + flipped * io_lookat * x * left.x, lookAt.y + flipped * io_lookat * x * left.y, lookAt.z + flipped * io_lookat * x * left.z, up.x, up.y, up.z);

			modelViewDirty = false;
		}
		post();

		currentCamera = this;
	}

	public void copyTo(BasicCamera shim) {
		super.copyTo(shim);
		if (shim instanceof LayeredStereoCamera) {
			((LayeredStereoCamera) shim).io_frustra = io_frustra;
			((LayeredStereoCamera) shim).io_position = io_position;
			((LayeredStereoCamera) shim).io_lookat = io_lookat;
		}
	}

	
	public float getIOFrustra() {
		return io_frustra;
	}

	public Vector3 getIOPosition() {
		return io_position;
	}


}
