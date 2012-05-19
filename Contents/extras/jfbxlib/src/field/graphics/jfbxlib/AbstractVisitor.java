package field.graphics.jfbxlib;

import java.nio.ByteBuffer;
import java.util.Map;

import field.bytecode.protect.Trampoline2;


public class AbstractVisitor implements JFBXVisitor {

	JFBXVisitor delegate;

	public void beginScene(long time) {
		try {
			if (delegate != null) delegate.beginScene(time);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void endScene() {
		try {
			if (delegate != null) delegate.endScene();
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitCameraInfo(float oq0, float oq1, float oq2, float oq3, float ot0, float ot1, float ot2, float os0, float os1, float os2, float extraXaxisRoll) {
		try {
			if (delegate != null) delegate.visitCameraInfo(oq0, oq1, oq2, oq3, ot0, ot1, ot2, os0, os1, os2, extraXaxisRoll);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public boolean visitTransformUserProperty(String name, int type, float min, float max, boolean animateable, double value) {
		try {
			if (delegate != null) return delegate.visitTransformUserProperty(name, type, min, max, animateable, value);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
		return false;
	}

	public void visitTransformUserPropertyKeyInfoBegin() {
		try {
			if (delegate != null) delegate.visitTransformUserPropertyKeyInfoBegin();
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitTransformUserPropertyKeyInfoEnd() {
		try {
			if (delegate != null) delegate.visitTransformUserPropertyKeyInfoEnd();
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitTransformUserPropertyKeyInfo(long time, double value) {
		try {
			if (delegate != null) delegate.visitTransformUserPropertyKeyInfo(time, value);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshBegin(int numVertex, int numPolygon) {
		try {
			if (delegate != null) delegate.visitMeshBegin(numVertex, numPolygon);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshBindBegin() {
		try {
			if (delegate != null) delegate.visitMeshBindBegin();
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshBindEnd() {
		try {
			if (delegate != null) delegate.visitMeshBindEnd();
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshBindPolygonBegin(int numVertex) {
		try {
			if (delegate != null) delegate.visitMeshBindPolygonBegin(numVertex);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshBindPolygonEnd() {
		try {
			if (delegate != null) delegate.visitMeshBindPolygonEnd();
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshBindPolygonIndex(int vertexIndex) {
		try {
			if (delegate != null) delegate.visitMeshBindPolygonIndex(vertexIndex);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshBindPolygonTexCoord(int textureCoordinateIndex, float u, float w) {
		try {
			if (delegate != null) delegate.visitMeshBindPolygonTexCoord(textureCoordinateIndex, u, w);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshBindPolygonVertexAttribute(int attriuteNumber, float a) {
		try {
			if (delegate != null) delegate.visitMeshBindPolygonVertexAttribute(attriuteNumber, a);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshBindPolygonVertexAttribute(int attriuteNumber, float a, float b) {
		try {
			if (delegate != null) delegate.visitMeshBindPolygonVertexAttribute(attriuteNumber, a, b);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshBindPolygonVertexAttribute(int attriuteNumber, float a, float b, float c) {
		try {
			if (delegate != null) delegate.visitMeshBindPolygonVertexAttribute(attriuteNumber, a, b, c);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshBindPolygonVertexAttribute(int attriuteNumber, float a, float b, float c, float d) {
		try {
			if (delegate != null) delegate.visitMeshBindPolygonVertexAttribute(attriuteNumber, a, b, c, d);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshBindVertexArray(ByteBuffer vertexData) {
		try {
			if (delegate != null) delegate.visitMeshBindVertexArray(vertexData);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshEnd() {
		try {
			if (delegate != null) delegate.visitMeshEnd();
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshResultBegin() {
		try {
			if (delegate != null) delegate.visitMeshResultBegin();
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshResultEnd() {
		try {
			if (delegate != null) delegate.visitMeshResultEnd();
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshResultVertexArray(ByteBuffer vertexData) {
		try {
			if (delegate != null) delegate.visitMeshResultVertexArray(vertexData);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshShapeDeformBegin() {
		try {
			if (delegate != null) delegate.visitMeshShapeDeformBegin();
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshShapeDeformEnd() {
		try {
			if (delegate != null) delegate.visitMeshShapeDeformEnd();
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshShapeDeformShape(ByteBuffer vertexData, float weight) {
		try {
			if (delegate != null) delegate.visitMeshShapeDeformShape(vertexData, weight);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshSkinDeformBegin() {
		try {
			if (delegate != null) delegate.visitMeshSkinDeformBegin();
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshSkinDeformDefineBone(int boneNumber, long isUiq, float oq0, float oq1, float oq2, float oq3, float ot0, float ot1, float ot2, float os0, float os1, float os2, float gq0, float gq1, float gq2, float gq3, float gt0, float gt1, float gt2, float gs0, float gs1, float gs2) {
		try {
			if (delegate != null) delegate.visitMeshSkinDeformDefineBone(boneNumber, isUiq, oq0, oq1, oq2, oq3, ot0, ot1, ot2, os0, os1, os2, gq0, gq1, gq2, gq3, gt0, gt1, gt2, gs0, gs1, gs2);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshSkinDeformEnd() {
		try {
			if (delegate != null) delegate.visitMeshSkinDeformEnd();
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitMeshSkinDeformSetInfluence(int boneNumber, int vertexIndex, float isWeight) {
		try {
			if (delegate != null) delegate.visitMeshSkinDeformSetInfluence(boneNumber, vertexIndex, isWeight);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitTransformBegin(String name, int type, long uid) {
		try {
			if (delegate != null) delegate.visitTransformBegin(name, type, uid);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitTransformEnd() {
		try {
			if (delegate != null) delegate.visitTransformEnd();
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	public void visitTransformInfo(float oq0, float oq1, float oq2, float oq3, float ot0, float ot1, float ot2, float os0, float os1, float os2, float q0, float q1, float q2, float q3, float t0, float t1, float t2, float s0, float s1, float s2) {
		try {
			if (delegate != null) delegate.visitTransformInfo(oq0, oq1, oq2, oq3, ot0, ot1, ot2, os0, os1, os2, q0, q1, q2, q3, t0, t1, t2, s0, s1, s2);
		} catch (Throwable e) {
			Trampoline2.handle(e);
		}
	}

	static public double convertJFBXTimeToSeconds(long time) {
		return time / (double) 46186158000L;
	}

	public void finalizeTransformNames(Map<Long, String> map) {
		if (delegate != null) delegate.finalizeTransformNames(map);
	}

}
