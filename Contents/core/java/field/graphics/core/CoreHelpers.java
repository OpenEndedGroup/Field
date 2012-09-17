package field.graphics.core;

import static org.lwjgl.opengl.APPLEVertexArrayObject.glGenVertexArraysAPPLE;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.lwjgl.opengl.APPLEVertexArrayObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.lwjgl.util.glu.GLU;

import field.core.Platform.OS;
import field.launch.SystemProperties;
import field.math.linalg.Matrix4;
import field.math.linalg.SingularMatrixException;
import field.math.linalg.Vector3;

public class CoreHelpers {

	static public boolean isCore = SystemProperties.getIntProperty("opengl32", 0) == 1;
	static public boolean isCoreCompat = SystemProperties.getIntProperty("opengl32_compat", 0) == 1;

	static public class GLMatrix {
		float[] head = new float[16];
		List<float[]> stack = new ArrayList<float[]>();

		public GLMatrix() {
			stack.add(head);
			loadIdentity(head);
		}

		public GLMatrix(GLMatrix copy) {
			for (float[] f : copy.stack) {
				stack.add(Arrays.copyOf(f, f.length));
			}

			if (stack.size() > 0)
				head = stack.get(stack.size() - 1);
		}

		@Override
		public String toString() {
			return "\n" + head[0] + " " + head[1] + " " + head[2] + " " + head[3] + "\n" + "\n" + head[4] + " " + head[5] + " " + head[6] + " " + head[7] + "\n" + "\n" + head[8] + " " + head[9] + " " + head[10] + " " + head[11] + "\n" + "\n" + head[12] + " " + head[13] + " " + head[14] + " " + head[15] + "\n";
		}

	}

	static public GLMatrix modelview = new GLMatrix();
	static public GLMatrix projection = new GLMatrix();
	static public GLMatrix texture0 = new GLMatrix();
	static public GLMatrix texture1 = new GLMatrix();
	static public GLMatrix texture2 = new GLMatrix();
	static public GLMatrix texture3 = new GLMatrix();
	static public GLMatrix texture4 = new GLMatrix();
	static public GLMatrix texture5 = new GLMatrix();
	static public GLMatrix texture = texture0;

	static int mode;

	static public class Attrib {
		GLMatrix modelview = new GLMatrix(CoreHelpers.modelview);
		GLMatrix projection = new GLMatrix(CoreHelpers.projection);
		GLMatrix texture0 = new GLMatrix(CoreHelpers.texture0);
		GLMatrix texture1 = new GLMatrix(CoreHelpers.texture1);
		GLMatrix texture2 = new GLMatrix(CoreHelpers.texture2);
		GLMatrix texture3 = new GLMatrix(CoreHelpers.texture3);
		GLMatrix texture4 = new GLMatrix(CoreHelpers.texture4);
		GLMatrix texture5 = new GLMatrix(CoreHelpers.texture5);
		GLMatrix texture = CoreHelpers.texture;
		int mode = CoreHelpers.mode;
		int[] viewport = new int[4];

		IntBuffer ii = ByteBuffer.allocateDirect(4 * 4 * 16).order(ByteOrder.nativeOrder()).asIntBuffer();

		public Attrib() {
			ii.rewind();
			GL11.glGetInteger(GL11.GL_VIEWPORT, ii);
			ii.get(viewport);
			ii.rewind();
		}

		public void pop() {
			CoreHelpers.texture = texture;
			CoreHelpers.mode = mode;

			GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

		}

	}

	static public void glMatrixMode(int mode) {
		CoreHelpers.mode = mode;
		if (!isCore || isCoreCompat)
			GL11.glMatrixMode(mode);
	}

	static public void glActiveTexture(int tex) {
		switch (tex) {
		case GL13.GL_TEXTURE0:
			texture = texture0;
			break;
		case GL13.GL_TEXTURE1:
			texture = texture1;
			break;
		case GL13.GL_TEXTURE2:
			texture = texture2;
			break;
		case GL13.GL_TEXTURE3:
			texture = texture3;
			break;
		case GL13.GL_TEXTURE4:
			texture = texture4;
			break;
		case GL13.GL_TEXTURE5:
			texture = texture5;
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	static public void glLoadIdentity() {
		if (!isCore || isCoreCompat)
			GL11.glLoadIdentity();

		switch (mode) {
		case GL11.GL_PROJECTION:
			loadIdentity(projection.head);			
			break;
		case GL11.GL_TEXTURE:
			loadIdentity(texture.head);
			break;
		case GL11.GL_MODELVIEW:
			loadIdentity(modelview.head);
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	private static void loadIdentity(float[] m) {
		m[0] = m[5] = m[10] = m[15] = 1;
		m[1] = m[2] = m[3] = m[4] = m[6] = m[7] = m[8] = m[9] = m[11] = m[12] = m[13] = m[14] = 0;
	}

	static public void gluOrtho2D(double left, double right, double bottom, double top) {
		glOrtho(left, right, bottom, top, -1, 1);
	}

	static public void glOrtho(double left, double right, double bottom, double top, double zNear, double zFar) {
		// ;//System.out.println(" ortho :" + left + " " + right + " " +
		// bottom + " " + top + " " + zNear + " " + zFar);
		if (!isCore  || isCoreCompat)
			GL11.glOrtho(left, right, bottom, top, zNear, zFar);

		float tx = (float) (-(right + left) / (right - left));
		float ty = (float) (-(top + bottom) / (top - bottom));
		float tz = (float) (-(zFar + zNear) / (zFar - zNear));

		float[] m = new float[16];

		m[0] = (float) (2 / (right - left));
		m[5] = (float) (2 / (top - bottom));
		m[10] = (float) (-2 / (zFar - zNear));
		m[15] = 1;
		m[3] = tx;
		m[7] = ty;
		m[11] = tz;

		glMultiply(m);
	}

	static private void glMultiply(float[] m) {
		switch (mode) {
		case GL11.GL_PROJECTION:
			System.arraycopy(glMultiply(projection.head, m), 0, projection.head, 0, 16);
			break;
		case GL11.GL_MODELVIEW:
			System.arraycopy(glMultiply(modelview.head, m), 0, modelview.head, 0, 16);
			break;
		case GL11.GL_TEXTURE:
			System.arraycopy(glMultiply(texture.head, m), 0, texture.head, 0, 16);
			break;
		default:
			throw new IllegalArgumentException();
		}

	}

	private static float[] glMultiply(float[] a, float[] b) {
		Matrix4 aa = new Matrix4(a);
		Matrix4 bb = new Matrix4(b);
		float[] out = new float[16];
		new Matrix4().mul(aa, bb).get(out);

		return out;
	}

	public static void getModelView(FloatBuffer model) {
		model.rewind();
		model.put(CoreHelpers.modelview.head[0 + 0]);
		model.put(CoreHelpers.modelview.head[0 + 4]);
		model.put(CoreHelpers.modelview.head[0 + 8]);
		model.put(CoreHelpers.modelview.head[0 + 12]);
		model.put(CoreHelpers.modelview.head[1 + 0]);
		model.put(CoreHelpers.modelview.head[1 + 4]);
		model.put(CoreHelpers.modelview.head[1 + 8]);
		model.put(CoreHelpers.modelview.head[1 + 12]);
		model.put(CoreHelpers.modelview.head[2 + 0]);
		model.put(CoreHelpers.modelview.head[2 + 4]);
		model.put(CoreHelpers.modelview.head[2 + 8]);
		model.put(CoreHelpers.modelview.head[2 + 12]);
		model.put(CoreHelpers.modelview.head[3 + 0]);
		model.put(CoreHelpers.modelview.head[3 + 4]);
		model.put(CoreHelpers.modelview.head[3 + 8]);
		model.put(CoreHelpers.modelview.head[3 + 12]);
		model.rewind();
	}

	public static void getProjection(FloatBuffer projection) {
		projection.rewind();
		projection.put(CoreHelpers.projection.head[0 + 0]);
		projection.put(CoreHelpers.projection.head[0 + 4]);
		projection.put(CoreHelpers.projection.head[0 + 8]);
		projection.put(CoreHelpers.projection.head[0 + 12]);
		projection.put(CoreHelpers.projection.head[1 + 0]);
		projection.put(CoreHelpers.projection.head[1 + 4]);
		projection.put(CoreHelpers.projection.head[1 + 8]);
		projection.put(CoreHelpers.projection.head[1 + 12]);
		projection.put(CoreHelpers.projection.head[2 + 0]);
		projection.put(CoreHelpers.projection.head[2 + 4]);
		projection.put(CoreHelpers.projection.head[2 + 8]);
		projection.put(CoreHelpers.projection.head[2 + 12]);
		projection.put(CoreHelpers.projection.head[3 + 0]);
		projection.put(CoreHelpers.projection.head[3 + 4]);
		projection.put(CoreHelpers.projection.head[3 + 8]);
		projection.put(CoreHelpers.projection.head[3 + 12]);
		projection.rewind();
	}

	public static void glBindVertexArrayAPPLE(int i) {
		if (!isCore  || isCoreCompat) {
			if (field.core.Platform.getOS() == OS.mac)
				APPLEVertexArrayObject.glBindVertexArrayAPPLE(i);
			else
				glBindVertexArray(i);
		} else {
			glBindVertexArray(i);
		}

	}

	public static int glGenVertexArraysApple() {
		if (!isCore  || isCoreCompat) {
			if (field.core.Platform.getOS() == OS.mac)
				return glGenVertexArraysAPPLE();
			else
				return glGenVertexArrays();
		} else {
			return glGenVertexArrays();

		}
	}

	public static void glPushMatrix() {

		if (!isCore  || isCoreCompat)
			GL11.glPushMatrix();

		switch (mode) {
		case GL11.GL_PROJECTION: {
			float[] c = new float[16];
			System.arraycopy(projection.head, 0, c, 0, 16);
			projection.head = c;
			projection.stack.add(c);
		}
			break;
		case GL11.GL_MODELVIEW: {
			float[] c = new float[16];
			System.arraycopy(modelview.head, 0, c, 0, 16);
			modelview.head = c;
			modelview.stack.add(c);
		}
			break;
		case GL11.GL_TEXTURE: {
			float[] c = new float[16];
			System.arraycopy(texture.head, 0, c, 0, 16);
			texture.head = c;
			texture.stack.add(c);
		}
			break;
		}
	}

	static float[] tmp = new float[16];

	public static void glMultMatrix(FloatBuffer matrixm) {
		if (!isCore  || isCoreCompat)
			GL11.glMultMatrix(matrixm);

		matrixm.rewind();
		matrixm.get(tmp);

		matrixm.rewind();

		glMultiply(tmp);

	}

	public static void glPopMatrix() {
		if (!isCore  || isCoreCompat)
			GL11.glPopMatrix();

		switch (mode) {
		case GL11.GL_PROJECTION:
			projection.stack.remove(projection.stack.size() - 1);
			projection.head = projection.stack.get(projection.stack.size() - 1);
			break;
		case GL11.GL_MODELVIEW:
			modelview.stack.remove(modelview.stack.size() - 1);
			modelview.head = modelview.stack.get(modelview.stack.size() - 1);
			break;
		case GL11.GL_TEXTURE:
			texture.stack.remove(texture.stack.size() - 1);
			texture.head = texture.stack.get(texture.stack.size() - 1);
			break;
		}
		// ;//System.out.println(" -------- pop"+projection.stack.size()+" "+modelview.stack.size()+" "+texture.stack.size());

	}

	static FloatBuffer tq = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

	public static void doCameraState() {
		UniformCache u = BasicGLSLangProgram.currentProgram.getUniformCache();
		{
			int vm = u.find(null, BasicGLSLangProgram.currentProgram.getShader(), "_viewMatrix");

			// ;//System.out.println(" finding model view matrix, got :"
			// + vm);

			if (vm != -1) {
				// hack, no caching
				tq.rewind();
				tq.put(modelview.head);
				tq.rewind();
				// ;//System.out.println(" -- updating model view matrix -- ");
				//
				// for (int j = 0; j < 4; j++) {
				// for (int i = 0; i < 4; i++) {
				// System.out.print(tq.get() + " ");
				// }
				// ;//System.out.println();
				// }
				// tq.rewind();

				GL20.glUniformMatrix4(vm, true, tq);
			}
		}
		{
			int vm = u.find(null, BasicGLSLangProgram.currentProgram.getShader(), "_projMatrix");
			// ;//System.out.println(" finding projection matrix, got :"
			// + vm);
			if (vm != -1) {
				// hack, no caching
				tq.rewind();
				tq.put(projection.head);
				tq.rewind();
				// ;//System.out.println(" -- updating projeciton matrix -- ");
				//
				// for (int j = 0; j < 4; j++) {
				// for (int i = 0; i < 4; i++) {
				// System.out.print(tq.get() + " ");
				// }
				// ;//System.out.println();
				// }
				// tq.rewind();

				GL20.glUniformMatrix4(vm, true, tq);
			}
		}
	}

	public static void glLineWidth(float f) {
		if (!isCore  || isCoreCompat)
			GL11.glLineWidth(f);
		else {

		}
	}

	public static void glFrustum(float left, float right, float bottom, float top, float zNear, float zFar) {
		if (!isCore  || isCoreCompat)
			GL11.glFrustum(left, right, bottom, top, zNear, zFar);
		float A = (right + left) / (right - left);
		float B = (top + bottom) / (top - bottom);
		float C = -(zFar + zNear) / (zFar - zNear);
		float D = -(2 * zFar * zNear) / (zFar - zNear);

		float[] m = new float[16];

		m[0] = (float) (2 * zNear) / (right - left);
		m[5] = (float) (2 * zNear) / (top - bottom);
		m[10] = C;
		m[14] = -1;
		m[2] = A;
		m[6] = B;
		m[11] = D;

		glMultiply(m);
	}

	public static void gluLookAt(float eyex, float eyey, float eyez, float centerx, float centery, float centerz, float upx, float upy, float upz) {

		if (!isCore  || isCoreCompat)
			GLU.gluLookAt(eyex, eyey, eyez, centerx, centery, centerz, upx, upy, upz);

		Vector3 forward = new Vector3(centerx - eyex, centery - eyey, centerz - eyez);
		Vector3 up = new Vector3(upx, upy, upz);

		forward.normalize();

		/* Side = forward x up */
		Vector3 side = new Vector3().cross(forward, up);
		side.normalize();

		/* Recompute up as: up = side x forward */
		up.cross(side, forward);

		float[] ret = new float[16];

		ret[0 * 4 + 0] = side.get(0);
		ret[1 * 4 + 0] = side.get(1);
		ret[2 * 4 + 0] = side.get(2);

		ret[0 * 4 + 1] = up.get(0);
		ret[1 * 4 + 1] = up.get(1);
		ret[2 * 4 + 1] = up.get(2);

		ret[0 * 4 + 2] = -forward.get(0);
		ret[1 * 4 + 2] = -forward.get(1);
		ret[2 * 4 + 2] = -forward.get(2);

		ret[3 * 4 + 3] = 1;

		Matrix4 m = new Matrix4(ret);
		try {
			m.invert();
			Vector3 e = m.transformPosition(new Vector3(eyex, eyey, eyez));
			ret[3 * 4 + 0] = -e.x;
			ret[3 * 4 + 1] = -e.y;
			ret[3 * 4 + 2] = -e.z;

			Matrix4 q = new Matrix4(ret);
			q.transpose();
			q.get(ret);

			glMultiply(ret);
		} catch (SingularMatrixException e) {
			e.printStackTrace();
			System.err.println(" WARNING: gluLookat didn't apply");
		}
	}

	static Set<Integer> noLongerEnabled = new LinkedHashSet<Integer>();
	static {
		noLongerEnabled.add(GL11.GL_TEXTURE_1D);
		noLongerEnabled.add(GL11.GL_TEXTURE_2D);
		noLongerEnabled.add(GL31.GL_TEXTURE_RECTANGLE);
	}

	public static void glEnable(int i) {
		if (isCore && noLongerEnabled.contains(i))
			return;
		else
			GL11.glEnable(i);
	}

	public static void glDisable(int i) {
		if (isCore && noLongerEnabled.contains(i))
			return;
		else
			GL11.glDisable(i);
	}

	public static void glTranslated(double x, double y, double z) {
		glMultiply(new float[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, (float) x, (float) y, (float) z, 1 });
	}

	static List<Attrib> attrib = new LinkedList<Attrib>();

	public static void glPushAttrib(int glAllAttribBits) {
		attrib.add(new Attrib());
	}

	public static void glPopAttrib() {
		attrib.remove(attrib.size() - 1).pop();
	}

	public static void backOutStacks() {
		while (modelview.stack.size() > 1) {
			CoreHelpers.glMatrixMode(GL11.GL_MODELVIEW);
			CoreHelpers.glPopMatrix();
		}
		while (projection.stack.size() > 1) {
			CoreHelpers.glMatrixMode(GL11.GL_PROJECTION);
			CoreHelpers.glPopMatrix();
		}
		while (texture0.stack.size() > 1) {
			CoreHelpers.glMatrixMode(GL13.GL_TEXTURE0);
			CoreHelpers.glPopMatrix();
		}
		while (texture1.stack.size() > 1) {
			CoreHelpers.glMatrixMode(GL13.GL_TEXTURE1);
			CoreHelpers.glPopMatrix();
		}
		while (texture2.stack.size() > 1) {
			CoreHelpers.glMatrixMode(GL13.GL_TEXTURE2);
			CoreHelpers.glPopMatrix();
		}

		while (texture3.stack.size() > 1) {
			CoreHelpers.glMatrixMode(GL13.GL_TEXTURE3);
			CoreHelpers.glPopMatrix();
		}
		while (texture4.stack.size() > 1) {
			CoreHelpers.glMatrixMode(GL13.GL_TEXTURE4);
			CoreHelpers.glPopMatrix();
		}
		while (texture5.stack.size() > 1) {
			CoreHelpers.glMatrixMode(GL13.GL_TEXTURE5);
			CoreHelpers.glPopMatrix();
		}

	}

}
