package field.graphics.dynamic;

import org.lwjgl.opengl.GL11;

import field.graphics.core.Base;
import field.graphics.core.Base.StandardPass;
import field.graphics.core.Base.iAcceptsSceneListElement;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.BasicGeometry.QuadMesh_long;
import field.graphics.core.BasicUtilities;
import field.graphics.core.BasicUtilities.DisableDepthTestWrap;
import field.graphics.core.BasicUtilities.SetBlendMode;
import field.math.linalg.Vector3;

/**
 * Created on Nov 2, 2003
 * 
 * @author marc
 */
public class DynamicPointlist_quad extends DynamicQuad_long implements iDynamicMesh {

	public DynamicPointlist_quad(QuadMesh_long from) {
		super(from);
	}

	@Override
	public int nextVertex(Vector3 v1) {

		int a = super.nextVertex(v1);
		int b = super.nextVertex(v1);
		int c = super.nextVertex(v1);
		int d = super.nextVertex(v1);

		super.setAux(a, Base.texture0_id, 0, 0);
		super.setAux(b, Base.texture0_id, 1, 0);
		super.setAux(c, Base.texture0_id, 1, 1);
		super.setAux(d, Base.texture0_id, 0, 1);

		super.nextFace(a, b, c, d);

		return a / 4;
	}

	public int nextVertexD(double x, double y, double z) {
		return nextVertex((float)x,(float)y,(float)z);
	}
	
	@Override
	public int nextVertex(float x, float y, float z) {
		int a = super.nextVertex(x,y,z);
		int b = super.nextVertex(x,y,z);
		int c = super.nextVertex(x,y,z);
		int d = super.nextVertex(x,y,z);

		super.setAux(a, Base.texture0_id, 0, 0);
		super.setAux(b, Base.texture0_id, 1, 0);
		super.setAux(c, Base.texture0_id, 1, 1);
		super.setAux(d, Base.texture0_id, 0, 1);

		super.nextFace(a, b, c, d);

		return a / 4;
		
	}
	
	@Override
	public void setAux(int vertex, int id, float a) {
		super.setAux(vertex * 4, id, a);
		super.setAux(vertex * 4 + 1, id, a);
		super.setAux(vertex * 4 + 2, id, a);
		super.setAux(vertex * 4 + 3, id, a);
	}

	@Override
	public void setAux(int vertex, int id, float a, float b) {
		super.setAux(vertex * 4, id, a, b);
		super.setAux(vertex * 4 + 1, id, a, b);
		super.setAux(vertex * 4 + 2, id, a, b);
		super.setAux(vertex * 4 + 3, id, a, b);
	}

	@Override
	public void setAux(int vertex, int id, float a, float b, float c) {
		super.setAux(vertex * 4, id, a, b, c);
		super.setAux(vertex * 4 + 1, id, a, b, c);
		super.setAux(vertex * 4 + 2, id, a, b, c);
		super.setAux(vertex * 4 + 3, id, a, b, c);
	}

	@Override
	public void setAux(int vertex, int id, float a, float b, float c, float d) {
		super.setAux(vertex * 4, id, a, b, c, d);
		super.setAux(vertex * 4 + 1, id, a, b, c, d);
		super.setAux(vertex * 4 + 2, id, a, b, c, d);
		super.setAux(vertex * 4 + 3, id, a, b, c, d);
	}
	
	public void setAuxD(int vertex, int id, double a, double b, double c, double d) {
		setAux(vertex, id, (float)a, (float)b, (float)c, (float)d);
	}
	
	float pointSize = 1;
	
	public void setPointSize(float size)
	{
		this.pointSize = size;
	}
	

	public static DynamicPointlist_quad texturedColoredPoints(iAcceptsSceneListElement acceptsSceneListElement, float width) {

		BasicGLSLangProgram program = new BasicGLSLangProgram("content/shaders/TestGLSLangVertex_fakePoints.glslang", "content/shaders/Texture2DForFakePoints.glslang");
		program.new SetIntegerUniform("texture", 0);
		
		QuadMesh_long lines = new QuadMesh_long(new BasicUtilities.Position());
		lines.rebuildTriangle(0);
		lines.rebuildVertex(0);
		lines.addChild(program);
		
		lines.addChild(new DisableDepthTestWrap());
		lines.addChild(new SetBlendMode(StandardPass.preRender, GL11.GL_SRC_ALPHA, GL11.GL_ONE));
		

		if (acceptsSceneListElement != null)
			acceptsSceneListElement.addChild(lines);
		
		DynamicPointlist_quad ret = new DynamicPointlist_quad(lines);
		return ret;
	}
}
