/*
 * Created on Mar 21, 2003
 */
package field.graphics.core;

import static org.lwjgl.opengl.ARBProgram.GL_MATRIX0_ARB;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW_MATRIX;
import static org.lwjgl.opengl.GL11.GL_TEXTURE;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.lwjgl.util.glu.GLU;

import field.graphics.core.Base.StandardPass;
import field.math.linalg.Vector3;


/**
 * @author marc
 * 
 */
public class AdvancedUtilities {

//	static public class DeProjectionMatrix extends BasicUtilities.OnePassElement {
//		BasicCamera realCamera;
//
//		BasicCamera textureCamera;
//
//		int matrixNumber = 1;
//
//		int width = 1;
//
//		int height = 1;
//
//		float h = 0.5f;
//
//		public DeProjectionMatrix(int matrixBase, BasicCamera realCamera, BasicCamera textureCamera) {
//			this(matrixBase, realCamera, textureCamera, 1, 1);
//		}
//
//		public DeProjectionMatrix(int matrixBase, BasicCamera realCamera, BasicCamera textureCamera, int width, int height) {
//			super(StandardPass.render);
//			matrixNumber = matrixBase;
//			this.textureCamera = textureCamera;
//			this.realCamera = realCamera;
//			this.width = width;
//			this.height = height;
//			this.matrix = new float[] { h * width, 0, 0, 0, 0, -h * height, 0, 0, 0, 0, 0, 0, h * width, h * height, 0, 1};
//			// this.matrix= new float[] { h * width, 0, 0, h*width, 0, 0*h * width, 0, 0*h*height, 0, 0, 0, 0, 0,0,0,1};
//		}
//
//		float[] matrix;
//
//		float[] deView = new float[16];
//
//		float[] entexture = new float[16];
//
//		float[] mult = new float[16];
//
//		/*
//		 * @see innards.graphics.basic.BasicUtilities.OnePassElement#performPass()
//		 */
//		public void performPass() {
//
//			// here we get to compute the matrix
//			// what we are going to do is freeze GL's matrix state, and use the camera code
//			glPushMatrix();
//			double[] viewMatrix = new double[16];
//			Vector3 position = new Vector3();
//			Vector3 lookAt = new Vector3();
//			Vector3 up = new Vector3();
//
//			glMatrixMode(GL_MATRIX0_ARB + matrixNumber);
//			glLoadIdentity();
//			realCamera.getPosition(position);
//			realCamera.getLookAt(lookAt);
//			realCamera.getUp(up);
//			GLU.gluLookAt(position.x, position.y, position.z, lookAt.x, lookAt.y, lookAt.z, up.x, up.y, up.z);
//			glGetFloatv(GL_MODELVIEW_MATRIX, deView, 0);
//
//			glMatrixMode(GL_MATRIX0_ARB + matrixNumber + 1);
//			glLoadIdentity();
//			glMultMatrixf(matrix, 0);
//
//			// now the PV of the texture camera
//
//			float fov = textureCamera.getFov();
//			float aspect = textureCamera.getAspect();
//			float near = textureCamera.getNear();
//			float far = textureCamera.getFar();
//
//
//			glu.gluPerspective((double) fov, (double) aspect, (double) near, (double) far);
//
//			textureCamera.getPosition(position);
//			textureCamera.getLookAt(lookAt);
//			textureCamera.getUp(up);
//			glu.gluLookAt(position.x, position.y, position.z, lookAt.x, lookAt.y, lookAt.z, up.x, up.y, up.z);
//			glGetFloatv(GL_MODELVIEW_MATRIX, entexture, 0);
//
//			glMatrixMode(GL_MODELVIEW);
//
//			glPopMatrix();
//
//		}
//
//		
//	}
	

	static public class DeProjectionMatrixFast extends BasicUtilities.OnePassElement {
		BasicCamera realCamera;

		BasicCamera textureCamera;

		float width = 1;

		float height = 1;

		float h = 0.5f;

		public DeProjectionMatrixFast(BasicCamera realCamera, BasicCamera textureCamera, float width, float height) {
			super(StandardPass.preTransform);
			this.textureCamera = textureCamera;
			this.realCamera = realCamera;
			this.width = width;
			this.height = height;
//			this.matrix = new float[] { h * width, 0, 0, 0, 0, -h * height, 0, 0, 0, 0, 1, 0, h * width, h * height, 0, 1};
			this.matrix.put(new float[] { h * width, 0, 0, 0, 0, h * height, 0, 0, 0, 0, h, 0, h * width, h * height, 0, 1});
			this.matrix.rewind();
		}

		public DeProjectionMatrixFast(BasicCamera realCamera, BasicCamera textureCamera, float width, float height, boolean flipH) {
			super(StandardPass.preTransform);
			this.textureCamera = textureCamera;
			this.realCamera = realCamera;
			this.width = width;
			this.height = height;
			System.err.println(" building deprojection matrix");

			this.matrix.put(new float[] { (flipH ? 1f : 1) * h * width, 0, 0, 0, 0, (flipH ? 1f : -1) * h * height, 0, 0, 1, 0, 0, 0, (flipH ? 1 : 1) * h * width, (flipH ? 1 : 1) * h * height, 0, 1});
			this.matrix.rewind();
			// this.matrix= new float[] { h * width, 0, 0, h*width, 0, 0*h * width, 0, 0*h*height, 0, 0, 0, 0, 0,0,0,1};
		}

//		float[] matrix;

		FloatBuffer deView = ByteBuffer.allocateDirect(4*16).order(ByteOrder.nativeOrder()).asFloatBuffer();
		FloatBuffer matrix = ByteBuffer.allocateDirect(4*16).order(ByteOrder.nativeOrder()).asFloatBuffer();

//		float[] entexture = new float[16];
		FloatBuffer entexture = ByteBuffer.allocateDirect(4*16).order(ByteOrder.nativeOrder()).asFloatBuffer();

		float[] mult = new float[16];

		float[] texturingMatrix = new float[16];

//		float[] deViewInverse = new float[16];

		float[] temp_debug1 = new float[16];

		float[] temp_debug2 = new float[16];

		public void rebuildMatrix(float w1, float h1, float w2, float h2) {
			this.matrix.rewind();
			this.matrix.put(new float[] { w1, 0, 0, 0, 0, h1, 0, 0, 0, 0, 0, 0, w2, h2, 0, 1});
			this.matrix.rewind();
		}

		/*
		 * @see innards.graphics.basic.BasicUtilities.OnePassElement#performPass()
		 */
		public void performPass() {

			// here we get to compute the matrix
			// what we are going to do is freeze GL's matrix state, and use the camera code
			CoreHelpers.glPushMatrix();
			double[] viewMatrix = new double[16];
			Vector3 position = new Vector3();
			Vector3 lookAt = new Vector3();
			Vector3 up = new Vector3();
			assert (glGetError() == 0);
			CoreHelpers.glLoadIdentity();
			realCamera.getPosition(position);
			realCamera.getLookAt(lookAt);
			realCamera.getUp(up);
			CoreHelpers.gluLookAt(position.x, position.y, position.z, lookAt.x, lookAt.y, lookAt.z, up.x, up.y, up.z);
			glGetFloat(GL_MODELVIEW_MATRIX, deView);
//			transposeNegate(deView, deViewInverse);
			assert (glGetError() == 0);

			CoreHelpers.glLoadIdentity();
			CoreHelpers.glMultMatrix(matrix);

			float fov = textureCamera.getFov();
			float aspect = textureCamera.getAspect();
			float near = textureCamera.getNear();
			float far = textureCamera.getFar();

			assert (glGetError() == 0);
			
//			CoreHelpers.gluPerspective((float) fov, (float) aspect, (float) near, (float) far);
			float right = (float) (near * Math.tan((Math.PI * fov / 180f) / 2) * aspect) ;
			float top = (float) (near * Math.tan((Math.PI * fov / 180f) / 2)) ;
			CoreHelpers.glFrustum(-right , right , -top , top , near, far);

			assert (glGetError() == 0);

			// now the PV of the texture camera


			textureCamera.getPosition(position);
			textureCamera.getLookAt(lookAt);
			textureCamera.getUp(up);
			CoreHelpers.gluLookAt(position.x, position.y, position.z, lookAt.x, lookAt.y, lookAt.z, up.x, up.y, up.z);
			assert (glGetError() == 0);
			//glMultMatrixf(deViewInverse, 0);

			
			entexture.rewind();
			glGetFloat(GL_MODELVIEW_MATRIX, entexture);

			
			
			
			CoreHelpers.glActiveTexture(GL_TEXTURE0);
			CoreHelpers.glMatrixMode(GL_TEXTURE);
			CoreHelpers.glLoadIdentity();
		
			
			
//			for(int i=0;i<entexture.length;i++)
//			{
//				entexture[i] = (float) Math.random();
//			}
			
			entexture.rewind();
			CoreHelpers.glMultMatrix(entexture);

			assert (glGetError() == 0);
			CoreHelpers.glMatrixMode(GL_MODELVIEW);

			CoreHelpers.glPopMatrix();

		}

		

//		public void installParameters(BasicGLSLangProgram into, String name) {
//			for (int m = 0; m < 4; m++) {
//				final int n = m;
//
//				into.new SetMatrixUniform(name, new iProvider<Matrix4>(){
//
//					public Matrix4 get() {
//						
//						Matrix4 m = new Matrix4();
//						
//						for(int x=0;x<4;x++)
//						{
//							for(int y=0;y<4;y++)
//							{
//								m.setElement(x, y, deView[4*y+x]);
//							}
//						}
//						return m;
//					}
//				});
//			}
//		}
	}

	// matrix "inversion"

	static public void transposeNegate(float[] src, float[] dst) {

		dst[0 * 4 + 0] = src[0 * 4 + 0];
		dst[1 * 4 + 0] = src[0 * 4 + 1];
		dst[2 * 4 + 0] = src[0 * 4 + 2];
		dst[3 * 4 + 0] = -dot(src, 4 * 3, src, 4 * 0);

		dst[0 * 4 + 1] = src[1 * 4 + 0];
		dst[1 * 4 + 1] = src[1 * 4 + 1];
		dst[2 * 4 + 1] = src[1 * 4 + 2];
		dst[3 * 4 + 1] = -dot(src, 4 * 3, src, 4 * 1);

		dst[0 * 4 + 2] = src[2 * 4 + 0];
		dst[1 * 4 + 2] = src[2 * 4 + 1];
		dst[2 * 4 + 2] = src[2 * 4 + 2];
		dst[3 * 4 + 2] = -dot(src, 4 * 3, src, 4 * 2);

		dst[0 * 4 + 3] = 0;
		dst[1 * 4 + 3] = 0;
		dst[2 * 4 + 3] = 0;
		dst[3 * 4 + 3] = 1;
	}

	static public float dot(float[] src, int i, float[] src2, int j) {
		return src[i] * src2[j] + src[i + 1] * src2[j + 1] + src[i + 2] * src2[j + 2];
	}

}
