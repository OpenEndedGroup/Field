package field.graphics.core;

import static org.lwjgl.opengl.APPLEVertexArrayObject.glBindVertexArrayAPPLE;
import static org.lwjgl.opengl.ARBShaderObjects.glUseProgramObjectARB;
import static org.lwjgl.opengl.GL11.GL_POINTS;
import static org.lwjgl.opengl.GL11.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnableClientState;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glPointSize;
import static org.lwjgl.opengl.GL11.glVertex3f;
import static org.lwjgl.opengl.GL11.glVertexPointer;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.GL_VERTEX_PROGRAM_POINT_SIZE;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

import java.nio.FloatBuffer;
import java.util.Map.Entry;
import java.util.Set;

import field.core.Platform.OS;
import field.core.plugins.PythonOverridden;
import field.core.util.PythonCallableMap;
import field.core.windowing.GLComponentWindow;
import field.graphics.core.Base.iGeometry;
import field.graphics.core.BasicGeometry.VertexBuffer;
import field.launch.SystemProperties;
import field.math.abstraction.iInplaceProvider;
import field.math.linalg.iCoordinateFrame.iMutable;

/**
 * like a triangle mesh, with no triangles, this is an array of points. useful
 * for some applications, not so useful for others if you want: a variable
 * number of point sprites and / or a custom texturemap and / or hand control
 * over their size (via vertex program). you will need the classes above
 * 
 * attaching a program that has VERTEX_PROGRAM_POINT_SIZE_ARB set will let you
 * programmatically change point sizes (still, no change of textures)
 */
public class PointList extends BasicGeometry.TriangleMesh implements iGeometry {

	static public final boolean useATIPointSpriteWorkaround = SystemProperties.getIntProperty("useATIPointSpriteWorkaround", 0) == 1;

	boolean doDynamicFrameRateCulling = false;

	float size = 5;

	public PointList(iInplaceProvider<iMutable> coordinateFrame) {
		super(coordinateFrame);
		rebuildVertex(0);
		rebuildTriangle(0);
	}

	public PointList() {
		super(new BasicUtilities.Position());
		rebuildTriangle(0);
		rebuildVertex(0);
	}

	public PointList setSize(float t) {
		size = t;
		return this;
	}

	PythonCallableMap drawArraysOverrides = new PythonCallableMap();

	public PythonCallableMap getDrawArraysOverrides() {
		return drawArraysOverrides;
	}

	@Override
	protected void doPerformPass() {
		glPointSize(size);

		CoreHelpers.glBindVertexArrayAPPLE(0);

		int vertexObjectID = BasicContextManager.getId(this);
		assert (glGetError() == 0);
		assert (glGetError() == 0);

		if (triangleLimit * 3 > triangleBuffer.sBuffer.capacity()) {
			triangleLimit = triangleBuffer.sBuffer.capacity() / 3;
		} else if (triangleLimit < 0) {
			triangleLimit = 0;
		}
		assert (glGetError() == 0);

		clean();
		CoreHelpers.doCameraState();

		if (GLComponentWindow.rendererInfo != null && GLComponentWindow.rendererInfo.startsWith("intel")) {

			int p = BasicGLSLangProgram.currentProgram.getShader();
			glUseProgramObjectARB(0);
			FloatBuffer v = vertex();
			FloatBuffer a = aux(Base.color0_id, 0);
			FloatBuffer ps = aux(13, 0);

			;// System.out.println(" dawing <" + numVertex() +
				// "> points the slow way");

			glDisable(GL_VERTEX_PROGRAM_POINT_SIZE);
			glPointSize(ps == null ? size : ps.get());
			glBegin(GL_POINTS);
			while (v.position() < numVertex() * 3) {
				float x = v.get(), y = v.get(), z = v.get();
				glVertex3f(x, y, z);
				if (a != null) {
					float r = a.get(), g = a.get(), b = a.get(), alpha = a.get();
					// ;//System.out.println(x+" "+y+" "+z+"   "+r+" "+g+" "+b+" "+alpha);
					glColor4f(r, g, b, alpha);
				}
			}
			v.rewind();
			if (a != null)
				a.rewind();
			while (v.position() < numVertex() * 3) {
				float x = v.get(), y = v.get(), z = v.get();
				glVertex3f(x, y, z);
				if (a != null) {
					float r = a.get(), g = a.get(), b = a.get(), alpha = a.get();
					// ;//System.out.println(x+" "+y+" "+z+"   "+r+" "+g+" "+b+" "+alpha);
					glColor4f(r, g, b, alpha);
				}
			}
			glEnd();
			glUseProgramObjectARB(p);
		} else {
			CoreHelpers.glBindVertexArrayAPPLE(vertexObjectID);

			if (!CoreHelpers.isCore && useATIPointSpriteWorkaround && field.core.Platform.getOS() == OS.mac) {

				;// System.out.println(" using ati point sprite workaround ");

				glBindVertexArrayAPPLE(0);
				glBindBuffer(GL_ARRAY_BUFFER, 0);

				// for(int i=0;i<16;i++)
				// glDisableVertexAttribArray(i);

				// workaround for ATI driver issues
				glVertexPointer(3, 12, vertex());
				glEnableClientState(GL_VERTEX_ARRAY);
				Set<Entry<Integer, VertexBuffer>> es = auxBuffers.entrySet();
				for (Entry<Integer, VertexBuffer> e : es) {
					glVertexAttribPointer(e.getKey(), e.getValue().elementSize, false, 0, e.getValue().buffer);
					glEnableVertexAttribArray(e.getKey());
				}
			}

			// glDisable(GL_BLEND);

			if (doDynamicFrameRateCulling) {
				glDrawArrays(GL_POINTS, 0, DynamicFrameRateCuller.advise(this.vertexLimit, this));
			} else {

				if (drawArraysOverrides.isEmpty())
					glDrawArrays(GL_POINTS, 0, this.vertexLimit);
				else
					drawArraysOverrides.invoke(GL_POINTS, 0, this.vertexLimit);

			}
		}

		CoreHelpers.glBindVertexArrayAPPLE(0);

	}

	@Override
	protected void doSetup() {
		super.doSetup();
	}

}