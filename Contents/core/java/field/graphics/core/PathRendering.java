package field.graphics.core;

import static org.lwjgl.opengl.NVPathRendering.GL_CONVEX_HULL_NV;
import static org.lwjgl.opengl.NVPathRendering.GL_PATH_FORMAT_PS_NV;
import static org.lwjgl.opengl.NVPathRendering.glCoverStrokePathNV;
import static org.lwjgl.opengl.NVPathRendering.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.NVPathRendering.glStencilStrokePathNV;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.NVPathRendering;

import field.graphics.core.Base.StandardPass;
import field.graphics.core.BasicUtilities.OnePassElement;

public class PathRendering extends OnePassElement {

	
	String curve;
	
	public PathRendering(String psString) {
		super(StandardPass.render);
		this.curve = psString;
	}

	@Override
	public void performPass() {

		int id = BasicContextManager.getId(this);
		if (id == BasicContextManager.ID_NOT_FOUND || !BasicContextManager.isValid(this))
			BasicContextManager.putId(this, setup());

		glClearStencil(0);
		glDisable(GL_DEPTH_TEST);
		glClearColor(0, 0, 0, 0);
		glStencilMask(~0);
		glPathParameterfNV(id, GL_PATH_STROKE_WIDTH_NV, 1.5f);
		glStencilStrokePathNV(id, 0x1, ~0);

		glEnable(GL_STENCIL_TEST);
		glDisable(GL_DEPTH_TEST);
		glStencilFunc(GL_NOTEQUAL, 0, 0x1F);
		glStencilOp(GL_KEEP, GL_KEEP, GL_ZERO);
		glCoverStrokePathNV(id, GL_CONVEX_HULL_NV);
	}
	
	public void setCurve(String curve) {
		if (curve.equals(this.curve))
			return;
		
		this.curve = curve;
		BasicContextManager.markAsInvalidInAllContexts(this);
	}

	private int setup() {

		int p = BasicContextManager.getId(this);;
		p = (p==BasicContextManager.ID_NOT_FOUND ? NVPathRendering.glGenPathsNV(1) : p);

		byte[] b = curve.getBytes();
		ByteBuffer bb = ByteBuffer.allocateDirect(b.length + 1);
		bb.put(b);
		bb.rewind();
		glPathStringNV(p, GL_PATH_FORMAT_PS_NV, bb);

		BasicContextManager.markAsValidInThisContext(this);
		return p;
	}

}
