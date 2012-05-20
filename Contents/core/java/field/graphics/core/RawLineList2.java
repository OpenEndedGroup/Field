package field.graphics.core;

import static org.lwjgl.opengl.APPLEVertexArrayObject.glBindVertexArrayAPPLE;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glMultMatrix;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.glDrawRangeElements;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;

public class RawLineList2 extends RawMesh2 {
	public RawLineList2(String filename) throws IOException {
		super(filename);
	}
	
	

	public RawLineList2(String filename, MappedByteBuffer source) throws IOException {
		super(filename, source);
	}

	FloatBuffer matrixm = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

	float width = 1;

	public void setWidth(float width) {
		this.width = width;
	}
	
	@Override
	public void doPerformPass() {
		glBindVertexArrayAPPLE(0);
		int vertexObjectID = BasicContextManager.getId(this);
		clean();

		glLineWidth(width);
		
		if (inFrame != null) {
			glPushMatrix();
			
			matrix = inFrame.get().getMatrix(tmpStorage).getColumnMajor(matrix);
			matrixm.rewind();
			matrixm.put(matrix);
			matrixm.rewind();
			glMultMatrix(matrixm);

		}

		glBindVertexArrayAPPLE(vertexObjectID);

		//;//System.out.println(" drawing <" + frame.get(0).length / 3 + " / <" + frame.get(-1).length + "> lines <"+matrix+"> <"+System.identityHashCode(this)+">");

		glDrawRangeElements(GL_LINES, 0, (int) frame.get(0).length / 3, (int) frame.get(-1).length, GL_UNSIGNED_INT, 0);
		glBindVertexArrayAPPLE(0);

		if (inFrame != null) {
			glPopMatrix();
		}
	}
}
