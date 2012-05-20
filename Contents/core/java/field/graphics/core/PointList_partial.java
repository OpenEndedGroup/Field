package field.graphics.core;

import java.util.Iterator;
import java.util.Iterator;
import java.util.Map.Entry;
import static org.lwjgl.opengl.APPLEVertexArrayObject.glBindVertexArrayAPPLE;
import static org.lwjgl.opengl.APPLEVertexArrayObject.glGenVertexArraysAPPLE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_SHORT;
import static org.lwjgl.opengl.GL11.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnableClientState;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glMultMatrix;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glVertexPointer;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STREAM_DRAW;
import static org.lwjgl.opengl.GL15.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL15.glMapBuffer;
import static org.lwjgl.opengl.GL15.glUnmapBuffer;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL32.GL_LINES_ADJACENCY;
import field.graphics.core.BasicGeometry.VertexBuffer;

public class PointList_partial extends PointList {

	static public int vertexPerFrame = 5000;

	int uploadedVertex = 0;

	boolean uploadComplete = false;
	boolean uploadStarted = false;

	/**
	 * call this when you've finished putting data into the buffers
	 */
	public void beginUpload() {
		uploadStarted = true;
	}

	@Override
	protected void doPerformPass() {
		if (uploadComplete)
			super.doPerformPass();
		else if (uploadStarted)
			clean();
	}

	@Override
	protected void clean() {

		if (!uploadStarted)
			return;
		if (uploadComplete)
			return;

		Object context = BasicContextManager.getCurrentContext();

		if (uploadedVertex < vertexLimit) {
			glBindBuffer(GL_ARRAY_BUFFER, attributeBuffers[0]);

			;//System.out.println(glGetError() == 0);

			int start = uploadedVertex * 4 * vertexStride;
			int end = Math.min(4 * vertexStride * vertexLimit, 4 * vertexStride * (uploadedVertex + vertexPerFrame));

			// int size = 4 * vertexStride * vertexLimit;

			vertexBuffer.bBuffer.position(4 * uploadedVertex * vertexStride);
			vertexBuffer.bBuffer.limit(end);

			glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer.bBuffer);

			;//System.out.println(glGetError() == 0);

			Iterator<Entry<Integer, VertexBuffer>> aux = auxBuffers.entrySet().iterator();
			while (aux.hasNext()) {
				Entry<Integer, VertexBuffer> e = aux.next();

				VertexBuffer vbuffer = e.getValue();
				int aid = e.getKey();
				{

					start = uploadedVertex * 4 * vbuffer.elementSize;
					end = Math.min(4 * vbuffer.elementSize * vertexLimit, 4 * vbuffer.elementSize * (uploadedVertex + vertexPerFrame));

					glBindBuffer(GL_ARRAY_BUFFER, attributeBuffers[aid]);
					vbuffer.bBuffer.position(4 * uploadedVertex * vbuffer.elementSize);
					vbuffer.bBuffer.limit(vertexLimit * 4 * vbuffer.elementSize);
					glBufferSubData(GL_ARRAY_BUFFER, 0, vbuffer.bBuffer);
					vbuffer.clean(context);
				}
			}

		} else {
		}

		uploadedVertex = Math.min(vertexLimit, uploadedVertex + vertexPerFrame);

		if (uploadedVertex == vertexLimit)
			uploadComplete = true;

	}

}
