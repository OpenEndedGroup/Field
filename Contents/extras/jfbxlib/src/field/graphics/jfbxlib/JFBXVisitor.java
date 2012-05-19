package field.graphics.jfbxlib;

import java.nio.ByteBuffer;
import java.util.Map;

public interface JFBXVisitor {

	public enum NodeTypes {
		error_unknown, unidentified, error_null, marker, skeleton, mesh, nurb, patch, camera, camera_switcher, light, optical_reference, optical_marker, constraint;
	}

	public void beginScene(long time);

	public void visitTransformBegin(String name, int type, long uid);

	public void visitTransformInfo(float oq0, float oq1, float oq2, float oq3, float ot0, float ot1, float ot2, float os0, float os1, float os2, float q0, float q1,
			float q2, float q3, float t0, float t1, float t2, float s0, float s1, float s2);

	public void visitMeshBegin(int numVertex, int numPolygon);

	public void visitMeshBindBegin();
	
	public void visitMeshBindVertexArray(ByteBuffer vertexData);

	public void visitMeshBindPolygonBegin(int numVertex);

	public void visitMeshBindPolygonIndex(int vertexIndex);
	public void visitMeshBindPolygonTexCoord(int textureCoordinateIndex, float u, float w);
	public void visitMeshBindPolygonVertexAttribute(int attriuteNumber, float a);
	public void visitMeshBindPolygonVertexAttribute(int attriuteNumber, float a, float b);
	public void visitMeshBindPolygonVertexAttribute(int attriuteNumber, float a, float b, float c);
	public void visitMeshBindPolygonVertexAttribute(int attriuteNumber, float a, float b, float c, float d);
	public void visitMeshBindPolygonEnd();

	public void visitMeshBindEnd();

	public void visitMeshShapeDeformBegin();
	public void visitMeshShapeDeformShape(ByteBuffer vertexData, float weight);
	public void visitMeshShapeDeformEnd();
	
	public void visitMeshSkinDeformBegin();
	public void visitMeshSkinDeformDefineBone(int boneNumber, long isUiq, float oq0, float oq1, float oq2, float oq3, float ot0, float ot1, float ot2, float os0, float os1, float os2, float gq0, float gq1, float gq2, float gq3, float gt0, float gt1, float gt2, float gs0, float gs1, float gs2);
	public void visitMeshSkinDeformSetInfluence(int boneNumber, int vertexIndex, float isWeight);
	public void visitMeshSkinDeformEnd();
	
	public void visitMeshResultBegin();
	public void visitMeshResultVertexArray(ByteBuffer vertexData);
	public void visitMeshResultEnd();

	public void visitMeshEnd();

	public void visitCameraInfo(float oq0, float oq1, float oq2, float oq3, float ot0, float ot1, float ot2, float os0, float os1, float os2, float extraXaxisRoll);
	
	public void visitTransformEnd();
	
	public boolean visitTransformUserProperty(String name, int type, float min, float max, boolean animateable, double value);
	public void visitTransformUserPropertyKeyInfoBegin();
	public void visitTransformUserPropertyKeyInfoEnd();
	public void visitTransformUserPropertyKeyInfo(long time, double value);

	public void endScene();
	
	public void finalizeTransformNames(Map<Long, String> map);

}
