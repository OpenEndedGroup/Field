package field.graphics.core;

import static org.lwjgl.opengl.GL11.glMultMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;

import java.io.IOException;

import static org.lwjgl.opengl.APPLEVertexArrayObject.glBindVertexArrayAPPLE;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glMultMatrix;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.glDrawRangeElements;

public class RawPointList2 extends RawMesh2 {
	public RawPointList2(String filename) throws IOException {
		super(filename);
	}

	float size = 5;
	
	@Override
	public void doPerformPass() {
		glPointSize(size);
		
		glBindVertexArrayAPPLE(0);
		int vertexObjectID = BasicContextManager.getId(this);
		clean();
		if (inFrame != null) {
			glPushMatrix();
			
			matrix = inFrame.get().getMatrix(tmpStorage).getColumnMajor(matrix);
			matrixm.rewind();
			matrixm.put(matrix);
			matrixm.rewind();
			glMultMatrix(matrixm);

		}


		
		//;//System.out.println(" drawing <" + frame.get(0).length / 3+"> points <"+matrix+"> <"+System.identityHashCode(this)+">");

		glBindVertexArrayAPPLE(vertexObjectID);

		glDrawArrays(GL_POINTS, 0, (int) frame.get(0).length / 3);
		glBindVertexArrayAPPLE(0);

		if (inFrame != null) {
			glPopMatrix();
		}
	}
	
	@Override
	public void doSetup() {
		elementBufferNeedsReconstruction = false;
		super.doSetup();
	}
	
	@Override
	protected void clean() {
		elementBufferNeedsReconstruction = false;
			super.clean();
	}
}
