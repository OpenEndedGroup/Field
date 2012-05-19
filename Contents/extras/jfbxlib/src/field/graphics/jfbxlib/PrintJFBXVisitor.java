package field.graphics.jfbxlib;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import field.math.linalg.Matrix4;
import field.math.linalg.Quaternion;
import field.math.linalg.Vector3;


public class PrintJFBXVisitor extends AbstractVisitor {

	public PrintJFBXVisitor(JFBXVisitor delegate) {
		this.delegate = delegate;
	}

	int indent = 0;

	public void beginScene(long time) {
		System.out.println(">> begin scene +" + indent);
		indent++;
		super.beginScene(time);
	}

	public void visitTransformBegin(String name, int type, int uid) {
		
		System.out.println(">>" + spaces(indent) + " begin transform <" + name + "> of type <" + (type+1<NodeTypes.values().length  ? NodeTypes.values()[type + 1] : "UNKNOWN") + "> id=" + uid);
		indent++;
		super.visitTransformBegin(name, type, uid);
	}

	public void visitTransformInfo(float oq0, float oq1, float oq2, float oq3, float ot0, float ot1, float ot2, float os0, float os1, float os2, float q0, float q1,
			float q2, float q3, float t0, float t1, float t2, float s0, float s1, float s2) {

		System.out.println(">>" + spaces(indent) + " offset ");
		System.out.println(">>" + spaces(indent) + "  rotation: " + oq0 + " " + oq1 + " " + oq2 + " " + oq3);
		System.out.println(">>" + spaces(indent) + "  translation: " + ot0 + " " + ot1 + " " + ot2);
		System.out.println(">>" + spaces(indent) + "  scale: " + os0 + " " + os1 + " " + os2);
		System.out.println(">>" + spaces(indent) + " non-offset ");
		System.out.println(">>" + spaces(indent) + "  rotation: " + q0 + " " + q1 + " " + q2 + " " + q3);
		System.out.println(">>" + spaces(indent) + "  translation: " + t0 + " " + t1 + " " + t2);
		System.out.println(">>" + spaces(indent) + "  scale: " + s0 + " " + s1 + " " + s2);

		Quaternion q = new Quaternion(oq0, oq1, oq2, oq3);
		Vector3 t = new Vector3(ot0, ot1, ot2);
		Vector3 s = new Vector3(os0, os1, os2);
		Matrix4 m = new Matrix4(q, t, s.x);
		System.out.println(" matrix \n" + m + ">");
		super.visitTransformInfo(oq0, oq1, oq2, oq3, ot0, ot1, ot2, os0, os1, os2, q0, q1, q2, q3, t0, t1, t2, s0, s1, s2);
	}

	@Override
	public boolean visitTransformUserProperty(String name, int type, float min, float max, boolean animateable, double value) {
		System.out.println(">>"+spaces(indent)+" transform user property");
		System.out.println(">>"+spaces(indent)+" name:"+name);
		System.out.println(">>"+spaces(indent)+" type:"+type);
		System.out.println(">>"+spaces(indent)+" min:"+min);
		System.out.println(">>"+spaces(indent)+" max:"+max);
		System.out.println(">>"+spaces(indent)+" anim:"+animateable);
		System.out.println(">>"+spaces(indent)+" value:"+value);
		return super.visitTransformUserProperty(name, type, min, max, animateable, value) || true;
	}
	
	@Override
	public void visitTransformUserPropertyKeyInfo(long time, double value) {
		System.out.println(">>"+spaces(indent)+"     key:"+convertJFBXTimeToSeconds(time)+" -> "+value);
		super.visitTransformUserPropertyKeyInfo(time, value);
	}	
	public void visitTransformEnd() {
		indent--;
		System.out.println(">>" + spaces(indent) + " end transform");
		super.visitTransformEnd();
	}

	public void endScene() {
		indent--;
		System.out.println(">> end scene +" + indent);
		super.endScene();
	}

	public void visitMeshBegin(int numVertex, int numPolygon) {
		System.out.println(">>" + spaces(indent) + " begin mesh vertex=<" + numVertex + "> polygon=<" + numPolygon + ">");
		indent++;
		super.visitMeshBegin(numVertex, numPolygon);
	}

	public void visitMeshBindBegin() {
		System.out.println(">>" + spaces(indent) + " bind follows");
		indent++;
		super.visitMeshBindBegin();
	}

	public void visitMeshBindVertexArray(ByteBuffer vertexData) {
		System.out.println(">>" + spaces(indent) + " bind vertex data has <" + vertexData.capacity() + "> size");
		{
			for (int i = 0; i < vertexData.capacity() / (3 * 4); i++) {
				float x = vertexData.getFloat();
				float y = vertexData.getFloat();
				float z = vertexData.getFloat();
				System.out.println("  " + spaces(indent) + x + ", " + y + ", " + z + "; ");
			}
			System.out.println();
		}
		super.visitMeshBindVertexArray(vertexData);
	}

	public void visitMeshBindPolygonBegin(int numVertex) {
		System.out.print(">>" + spaces(indent) + " polygon(" + numVertex + ")= [");
		super.visitMeshBindPolygonBegin(numVertex);
	}

	public void visitMeshBindPolygonIndex(int vertexIndex) {
		System.out.print(vertexIndex + " ");
		super.visitMeshBindPolygonIndex(vertexIndex);
	}

	public void visitMeshBindPolygonTexCoord(int textureCoordinateIndex, float u, float w) {
		System.out.println("(" + u + "," + w + ")");
		super.visitMeshBindPolygonTexCoord(textureCoordinateIndex, u, w);
	}

	public void visitMeshBindPolygonEnd() {
		System.out.println("]");
		super.visitMeshBindPolygonEnd();
	}

	public void visitMeshBindEnd() {
		indent--;
		System.out.println(">>" + spaces(indent) + " bind mesh complete");
		super.visitMeshBindEnd();
	}

	public void visitMeshShapeDeformBegin() {
		System.out.println(">> " + spaces(indent) + "mesh shape deform begin");
		indent++;
		super.visitMeshShapeDeformBegin();
	}

	public void visitMeshShapeDeformShape(ByteBuffer vertexData, float weight) {
		System.out.println(">>" + spaces(indent) + " mesh shapde deform got vertex data with weight <" + weight + ">");
		super.visitMeshShapeDeformShape(vertexData, weight);
	}

	public void visitMeshShapeDeformEnd() {
		indent--;
		System.out.println(">>" + spaces(indent) + " mesh shape deform end");
		super.visitMeshShapeDeformEnd();
	}

	public void visitMeshSkinDeformBegin() {
		System.out.println(">> " + spaces(indent) + "mesh skin deform begin");
		indent++;
		super.visitMeshSkinDeformBegin();
	}

	public void visitMeshSkinDeformDefineBone(int boneNumber, int isUiq, float oq0, float oq1, float oq2, float oq3, float ot0, float ot1, float ot2, float os0, float os1, float os2, float gq0, float gq1, float gq2, float gq3, float gt0, float gt1, float gt2, float gs0, float gs1, float gs2) {
		System.out.println(">> " + spaces(indent) + " define bone <" + boneNumber + "> is <" + isUiq + ">");
		super.visitMeshSkinDeformDefineBone(boneNumber, isUiq, oq0, oq1, oq2, oq3, ot0, ot1, ot2, os0, os1, os2, gq0, gq1, gq2, gq3, gt0, gt1, gt2, gs0, gs1, gs2);
	}

	public void visitMeshSkinDeformSetInfluence(int boneNumber, int vertexIndex, float isWeight) {
		System.out.println(">> " + spaces(indent) + "weight <" + boneNumber + " -> " + vertexIndex + " (" + isWeight + ")");
		super.visitMeshSkinDeformSetInfluence(boneNumber, vertexIndex, isWeight);
	}

	public void visitMeshSkinDeformEnd() {
		indent--;
		System.out.println(">> " + spaces(indent) + "mesh skin deform end");
		super.visitMeshSkinDeformEnd();
	}

	public void visitMeshResultBegin() {
		System.out.println(">>" + spaces(indent) + " mesh result follows");
		indent++;
		super.visitMeshResultBegin();
	}

	public void visitMeshResultVertexArray(ByteBuffer vertexData) {
		
		vertexData.order(ByteOrder.nativeOrder());
		
		System.out.println(">>" + spaces(indent) + " result vertex data has <" + vertexData.capacity() + "> size");
		{
			for (int i = 0; i < vertexData.capacity() / (3 * 4); i++) {
				float x = vertexData.getFloat();
				float y = vertexData.getFloat();
				float z = vertexData.getFloat();
				System.out.println("  " + spaces(indent) + x + ", " + y + ", " + z + "; ");
			}
			System.out.println();
		}
		super.visitMeshResultVertexArray(vertexData);
	}

	public void visitMeshResultEnd() {
		indent--;
		System.out.println(">>" + spaces(indent) + " mesh result finished");
		super.visitMeshResultEnd();
	}

	public void visitMeshEnd() {
		indent--;
		System.out.println(">>" + spaces(indent) + " mesh ends ");
		super.visitMeshResultEnd();
	}

	public void visitCameraInfo(float oq0, float oq1, float oq2, float oq3, float ot0, float ot1, float ot2, float os0, float os1, float os2, float extraXaxisRoll) {
		System.out.println(">>" + spaces(indent) + " camera info");
		super.visitCameraInfo(oq0, oq1, oq2, oq3, ot0, ot1, ot2, os0, os1, os2, extraXaxisRoll);
	}

	protected static String spaces(int indent) {
		String s = "";
		for (int i = 0; i < indent * 2; i++)
			s = s + " ";
		return s;
	}
}
