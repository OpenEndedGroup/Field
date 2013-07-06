package field.graphics.dynamic;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import field.bytecode.protect.iInside;
import field.graphics.core.Base;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.BasicGeometry;
import field.graphics.core.PointList;
import field.graphics.core.ResourceMonitor;
import field.graphics.core.Base.iAcceptsSceneListElement;
import field.graphics.core.Base.iSceneListElement;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.math.graph.iMutable;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Vector3;

/**
 * @author marc Created on Oct 21, 2003
 */
public class DynamicMesh implements iDynamicMesh, iInside, iRemoveable, field.graphics.core.Base.iAcceptsSceneListElement, AutoCloseable {

	public static DynamicMesh unshadedMesh() {
		BasicGeometry.TriangleMesh lines = new BasicGeometry.TriangleMesh(new CoordinateFrame());
		lines.rebuildTriangle(0);
		lines.rebuildVertex(0);
		// lines.addChild(new BasicUtilities.Smooth());
		// lines.addChild(new BasicUtilities.DisableDepthTest(false));
		// lines.addChild(new BasicUtilities.PolygonOffset(20, 20));

		DynamicMesh ret = new DynamicMesh(lines);
		return ret;
	}

	public static DynamicMesh coloredMesh(iAcceptsSceneListElement acceptsSceneListElement) {
		BasicGLSLangProgram program = new BasicGLSLangProgram("content/shaders/TestGLSLangVertex.glslang", "content/shaders/VertexColorFragment.glslang");
		BasicGeometry.TriangleMesh lines = new BasicGeometry.TriangleMesh(new CoordinateFrame());
		lines.rebuildTriangle(0);
		lines.rebuildVertex(0);
		lines.addChild(program);
		if (acceptsSceneListElement != null)
			acceptsSceneListElement.addChild(lines);
		DynamicMesh ret = new DynamicMesh(lines);
		return ret;
	}

	public static DynamicMesh coloredTexturedMesh(iAcceptsSceneListElement basicSceneList) {
		BasicGLSLangProgram program = new BasicGLSLangProgram("content/shaders/TestGLSLangVertex.glslang", "content/shaders/Texture2DSquareTimesDiffuseFragment.glslang");
		BasicGeometry.TriangleMesh lines = new BasicGeometry.TriangleMesh(new CoordinateFrame());
		lines.rebuildTriangle(0);
		lines.rebuildVertex(0);
		lines.addChild(program);
		if (basicSceneList != null)
			basicSceneList.addChild(lines);
		DynamicMesh ret = new DynamicMesh(lines);
		return ret;
	}

	static public DynamicPointlist unshadedPoints(iAcceptsSceneListElement acceptsSceneListElement) {
		BasicGeometry.TriangleMesh lines = new PointList(new CoordinateFrame());
		lines.rebuildTriangle(0);
		lines.rebuildVertex(0);
		if (acceptsSceneListElement != null)
			acceptsSceneListElement.addChild(lines);

		DynamicPointlist ret = new DynamicPointlist(lines);
		return ret;
	}

	protected int shrink_policy_tries = 0;

	protected float shrink_policy_min_fraction = 0.75f;

	protected int elementSize = 3; // triangles

	protected Base.iGeometry basis;

	public int vertexCursor;

	public int triangleCursor;

	protected int underlayingMaxStorageVertex;

	protected int underlayingMaxStorageTriangle;

	public FloatBuffer cachedVertexBuffer;

	public ShortBuffer cachedTriangleBuffer;

	public FloatBuffer[] cachedAuxBuffer = new FloatBuffer[16];

	protected int[] cachedAuxBufferWidth = new int[16];

	protected ArrayList faceToVertex = new ArrayList();

	protected boolean shrink = false;

	protected int shrink_vertex_to = 0;

	protected int shrink_triangle_to = 0;

	float growthFactor = 1.5f;

	public int openCount = 0;

	int closeCount = 0;

	public DynamicMesh(Base.iGeometry from) {
		this.basis = from;
		this.underlayingMaxStorageVertex = from.numVertex();
		this.underlayingMaxStorageTriangle = from.numTriangle();
	}

	public void close() {
		elementAt = 0;

		openCount--;
		if (openCount == 0) {

			if (basis instanceof BasicGeometry.TriangleMesh) {
				((BasicGeometry.TriangleMesh) basis).setVertexLimit(vertexCursor);
				((BasicGeometry.TriangleMesh) basis).setTriangleLimit(triangleCursor);

			} 
//			else if (basis instanceof BasicGeometry.InputOutputMesh) {
//				((BasicGeometry.InputOutputMesh) basis).setVertexLimit(vertexCursor);
//				((BasicGeometry.InputOutputMesh) basis).setTriangleLimit(triangleCursor);
//			}

			if ((shrink_policy_tries != 0) && (closeCount % shrink_policy_tries == 0)) {
				if ((vertexCursor / shrink_policy_min_fraction < cachedVertexBuffer.capacity() / 3) || (triangleCursor / shrink_policy_min_fraction < cachedTriangleBuffer.capacity() / elementSize)) {
					shrink = true;
					shrink_vertex_to = (int) (vertexCursor * Math.sqrt(growthFactor));
					shrink_triangle_to = (int) (triangleCursor * Math.sqrt(growthFactor));
				}

			}
		}
		if (openCount < 0)
			assert false : "to many closes";
	}

	public void copyFrom(SubMesh subMesh) {

//		;//System.out.println(" copy from <" + subMesh.delegate.cachedVertexBuffer + " " + subMesh.delegate.cachedTriangleBuffer);

		if (subMesh.delegate.cachedVertexBuffer == null)
			return;
		// if (subMesh.delegate.cachedTriangleBuffer == null)
		// return;

		for (int i = 0; i < subMesh.delegate.cachedAuxBuffer.length; i++) {
			if (subMesh.delegate.cachedAuxBuffer[i] != null) {
				subMesh.delegate.cachedAuxBuffer[i].rewind();
				subMesh.delegate.cachedAuxBuffer[i].limit(subMesh.delegate.vertexCursor * subMesh.delegate.cachedAuxBufferWidth[i]);
			}
		}

		subMesh.delegate.cachedVertexBuffer.rewind();
		subMesh.delegate.cachedVertexBuffer.limit(subMesh.delegate.vertexCursor * 3);
		if (subMesh.delegate.cachedTriangleBuffer != null) {
			subMesh.delegate.cachedTriangleBuffer.limit(subMesh.delegate.triangleCursor * subMesh.delegate.elementSize);
			subMesh.delegate.cachedTriangleBuffer.rewind();
		}
		this.inject(subMesh.delegate.cachedVertexBuffer, subMesh.delegate.cachedTriangleBuffer, new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 }, subMesh.delegate.cachedAuxBuffer, 0);

		// subMesh.delegate.cachedVertexBuffer.clear();
		// subMesh.delegate.cachedTriangleBuffer.clear();

	}

	/**
	 * @param v1
	 * @return
	 */
	public Vector3 getPositionOfVertex(int v1, Vector3 v) {
		if (v == null)
			v = new Vector3();
		return v.set(cachedVertexBuffer.get(3 * v1), cachedVertexBuffer.get(3 * v1 + 1), cachedVertexBuffer.get(3 * v1 + 2));
	}

	/** @return */
	public Base.iGeometry getUnderlyingGeometry() {
		return basis;
	}

	/**
	 * @return
	 */
	public int getVertexCursor() {
		return vertexCursor;
	}

	public void hintPrimitiveType(int primType) {
	}

	public void inject(FloatBuffer vertex, ShortBuffer triangle, int[] auxs, FloatBuffer[] auxBuffers, int oldVertexCursor) {

		// dirty = true;

		// ;//System.out.println(" about to copy <"+vertex.limit()+"> <"+triangle.limit()+">");

		int offset = (oldVertexCursor - vertexCursor);

		checkVertexStorage(vertexCursor, vertex.remaining());
		cachedVertexBuffer.position(vertexCursor * 3);
		int originalVertexCursor = vertexCursor;

		// ;//System.out.println(cachedVertexBuffer + " " + vertex);

		safePut(cachedVertexBuffer, vertex);
		// cachedVertexBuffer.put(vertex);

		vertexCursor += vertex.limit() / 3;
		cachedVertexBuffer.position(0);

		if (triangle != null) {
			checkTriangleStorage(triangleCursor, triangle.remaining() / elementSize);
			cachedTriangleBuffer.position(triangleCursor * elementSize);

			// cachedTriangleBuffer.put(triangle);
			int num = triangle.remaining();
			if (offset != 0)
				for (int i = 0; i < num; i++) {
					short c = (short) (triangle.get() - offset);

					// assert c>0;
					cachedTriangleBuffer.put(c);
				}
			else
				safePut(cachedTriangleBuffer, triangle);

			cachedTriangleBuffer.position(0);
			triangleCursor += triangle.limit() / elementSize;
		}
		for (int i = 0; i < auxs.length; i++) {
			if (auxBuffers[i] != null)
				if (auxBuffers[i].remaining() > 0) {

					if (cachedAuxBuffer[auxs[i]] == null) {
						checkAuxStorage(auxs[i], auxBuffers[i].capacity() / (vertex.capacity() / 3));
					} else
						checkAuxStorage(auxs[i], cachedAuxBufferWidth[auxs[i]]);
					cachedAuxBuffer[auxs[i]].position(cachedAuxBufferWidth[auxs[i]] * originalVertexCursor);

					// cachedAuxBuffer[auxs[i]].put(auxBuffers[i]);
					safePut(cachedAuxBuffer[auxs[i]], auxBuffers[i]);
					cachedAuxBuffer[auxs[i]].position(0);
				}
			// else
		}
	}

	private void safePut(FloatBuffer to, FloatBuffer from) {

		// if (true)
		// {
		// to.put(from);
		// return;
		// }

		if (to.isDirect() && from.isDirect()) {

			to.put(from);
			return;
		}
		if (!to.isDirect() && !from.isDirect()) {
			to.put(from);
			return;
		}
		int r = from.remaining();
		for (int i = 0; i < r; i++) {
			to.put(from.get());
		}
	}

	private void safePut(ShortBuffer to, ShortBuffer from) {

		// if (true)
		// {
		// to.put(from);
		// return;
		// }

		if (to.isDirect() && from.isDirect()) {
			to.put(from);
			return;
		}
		if (!to.isDirect() && !from.isDirect()) {
			to.put(from);
			return;
		}
		int r = from.remaining();
		for (int i = 0; i < r; i++) {
			to.put(from.get());
		}
	}

	public int nextFace(int v1, int v2, int v3) {
		checkTriangleStorage(triangleCursor, 1);
		int triangleCursor3 = 3 * triangleCursor;
		cachedTriangleBuffer.put(triangleCursor3, (short) v1);
		cachedTriangleBuffer.put(triangleCursor3 + 1, (short) v2);
		cachedTriangleBuffer.put(triangleCursor3 + 2, (short) v3);

		int[] rec;
		if (faceToVertex.size() <= triangleCursor)
			faceToVertex.add(rec = new int[3]);
		else
			rec = (int[]) faceToVertex.get(triangleCursor);
		rec[0] = v1;
		rec[1] = v2;
		rec[2] = v3;

		return triangleCursor++;
	}

	int[] elementFace = new int[2];
	int elementAt = 0;

	public void nextFace(int v1) {
//		;//System.out.println(" next face <" + v1 + "> : " + elementAt + " " + elementFace[0] + " " + elementFace[1]);

		if (elementAt == 2) {
			nextFace(elementFace[0], elementFace[1], v1);
			elementAt = 0;
		} else
			elementFace[elementAt++] = v1;
	}

	/**
	 * returns face number
	 */
	public int nextFace(Vector3 v1, Vector3 v2, Vector3 v3) {
		checkTriangleStorage(triangleCursor, 1);
		checkVertexStorage(vertexCursor, 9);

		int vertexCursor3 = 3 * vertexCursor;
		int triangleCursor3 = 3 * triangleCursor;
		cachedVertexBuffer.put(vertexCursor3, v1.get(0));
		cachedVertexBuffer.put(vertexCursor3 + 1, v1.get(1));
		cachedVertexBuffer.put(vertexCursor3 + 2, v1.get(2));
		vertexCursor3 += 3;
		cachedVertexBuffer.put(vertexCursor3, v2.get(0));
		cachedVertexBuffer.put(vertexCursor3 + 1, v2.get(1));
		cachedVertexBuffer.put(vertexCursor3 + 2, v2.get(2));
		vertexCursor3 += 3;
		cachedVertexBuffer.put(vertexCursor3, v3.get(0));
		cachedVertexBuffer.put(vertexCursor3 + 1, v3.get(1));
		cachedVertexBuffer.put(vertexCursor3 + 2, v3.get(2));
		vertexCursor3 += 3;

		cachedTriangleBuffer.put(triangleCursor3, (short) vertexCursor);
		cachedTriangleBuffer.put(triangleCursor3 + 1, (short) (vertexCursor + 1));
		cachedTriangleBuffer.put(triangleCursor3 + 2, (short) (vertexCursor + 2));

		int[] rec;
		if (faceToVertex.size() <= triangleCursor)
			faceToVertex.add(rec = new int[3]);
		else
			rec = (int[]) faceToVertex.get(triangleCursor);
		rec[0] = vertexCursor;
		rec[1] = vertexCursor + 1;
		rec[2] = vertexCursor + 2;
		vertexCursor += 3;

		return triangleCursor++;
	}

	public int nextVertex(Vector3 v1) {
		checkVertexStorage(vertexCursor, 3);
		int vertexCursor3 = 3 * vertexCursor;

		cachedVertexBuffer.put(vertexCursor3, v1.get(0));
		cachedVertexBuffer.put(vertexCursor3 + 1, v1.get(1));
		cachedVertexBuffer.put(vertexCursor3 + 2, v1.get(2));
		return vertexCursor++;
	}
	public int nextVertex(float  x, float y, float z) {
		checkVertexStorage(vertexCursor, 3);
		int vertexCursor3 = 3 * vertexCursor;

		cachedVertexBuffer.put(vertexCursor3, x);
		cachedVertexBuffer.put(vertexCursor3 + 1, y);
		cachedVertexBuffer.put(vertexCursor3 + 2, z);
		return vertexCursor++;
	}

	public void open() {
		if (openCount == 0) {
			vertexCursor = 0;
			triangleCursor = 0;
			if (shrink) {
				basis.rebuildTriangle(shrink_triangle_to);
				basis.rebuildVertex(shrink_vertex_to);
				shrink = false;
			}
			cachedVertexBuffer = null;
			cachedTriangleBuffer = null;
			cachedAuxBuffer = new FloatBuffer[16];
		}
		openCount++;
	}

	public void remove() {

		if (this.getUnderlyingGeometry().getParents().size() != 0) {

			List<? extends iMutable<iSceneListElement>> p = new ArrayList<iMutable<iSceneListElement>>(this.getUnderlyingGeometry().getParents());
			Iterator<? extends iMutable<iSceneListElement>> n = p.iterator();
			while (n.hasNext()) {
				iMutable<iSceneListElement> nn = n.next();
				nn.removeChild(this.getUnderlyingGeometry());
			}

			((TriangleMesh) this.getUnderlyingGeometry()).deallocate(ResourceMonitor.resourceMonitor.getQueue());
		}
		// (((Base.iSceneList)
		// this.getUnderlyingGeometry().getParent())).removeChild(getUnderlyingGeometry());
	}

	public void setAux(int vertex, int id, float a) {
		checkAuxStorage(id, 1);
		cachedAuxBuffer[id].put(vertex * 1, a);
	}

	public void setAux(int vertex, int id, float a, float b) {
		checkAuxStorage(id, 2);
		cachedAuxBuffer[id].put(vertex * 2, a);
		cachedAuxBuffer[id].put(vertex * 2 + 1, b);
	}

	public void setAux(int vertex, int id, float a, float b, float c) {
		checkAuxStorage(id, 3);
		cachedAuxBuffer[id].put(vertex * 3, a);
		cachedAuxBuffer[id].put(vertex * 3 + 1, b);
		cachedAuxBuffer[id].put(vertex * 3 + 2, c);
	}

	public void setAux(int vertex, int id, float a, float b, float c, float d) {
		checkAuxStorage(id, 4);
		cachedAuxBuffer[id].put(vertex * 4, a);
		cachedAuxBuffer[id].put(vertex * 4 + 1, b);
		cachedAuxBuffer[id].put(vertex * 4 + 2, c);
		cachedAuxBuffer[id].put(vertex * 4 + 3, d);
	}

	public void setPositionOfVertex(int n, Vector3 v1) {
		checkVertexStorage(n, 3);
		int vertexCursor3 = 3 * n;

		assert vertexCursor3 < cachedVertexBuffer.capacity() : n + " " + vertexCursor3 + " " + cachedVertexBuffer.capacity() + " " + cachedVertexBuffer.limit();

		cachedVertexBuffer.put(vertexCursor3, v1.get(0));
		cachedVertexBuffer.put(vertexCursor3 + 1, v1.get(1));
		cachedVertexBuffer.put(vertexCursor3 + 2, v1.get(2));
		if (vertexCursor < n + 1)
			vertexCursor = n + 1;
	}

	public iDynamicMesh setShrinkPolicy(int shrink_policy_tries, float shrink_policy_min_fraction) {
		this.shrink_policy_min_fraction = shrink_policy_min_fraction;
		this.shrink_policy_tries = shrink_policy_tries;
		return this;
	}

	public int[] vertexForFace(int face) {
		return (int[]) faceToVertex.get(face);
	}

	protected void checkAuxStorage(int id, int width) {
		if (cachedAuxBuffer[id] == null) {
			cachedAuxBuffer[id] = basis.aux(id, width);
		}
		cachedAuxBuffer[id].clear();

		cachedAuxBufferWidth[id] = width;
	}

	protected void checkTriangleStorage(int at, int plus) {

		if (cachedTriangleBuffer == null)
			cachedTriangleBuffer = basis.triangle();
		cachedTriangleBuffer.clear();
		if (cachedTriangleBuffer.capacity() < ((at + plus) * elementSize)) {
			basis.rebuildTriangle((int) (plus * elementSize + growthFactor * cachedTriangleBuffer.capacity() / elementSize));
			cachedTriangleBuffer = basis.triangle().put((ShortBuffer) cachedTriangleBuffer.rewind());
		}
	}

	protected void checkVertexStorage(int at, int plus) {

		if (cachedVertexBuffer == null)
			cachedVertexBuffer = basis.vertex();
		cachedVertexBuffer.clear();
		if (cachedVertexBuffer.capacity() < ((at * 3 + plus))) {

			basis.rebuildVertex((int) (plus / 3 + growthFactor * Math.max((at + plus / 3), cachedVertexBuffer.capacity() / 3)));
			cachedVertexBuffer = basis.vertex().put((FloatBuffer) cachedVertexBuffer.rewind());

			for (int i = 0; i < cachedAuxBuffer.length; i++)
				if (cachedAuxBuffer[i] != null) {
					cachedAuxBuffer[i] = basis.aux(i, cachedAuxBufferWidth[i]).put((FloatBuffer) cachedAuxBuffer[i].rewind());
				}
		}
	}

	private List<iMutable<iSceneListElement>> oldParents;

	public void off() {
		if (oldParents != null)
			return;

		oldParents = new ArrayList<iMutable<iSceneListElement>>(getUnderlyingGeometry().getParents());
		for (iMutable<iSceneListElement> m : oldParents) {
			m.removeChild(getUnderlyingGeometry());
		}
	}

	public void on() {
		if (oldParents == null)
			return;
		for (iMutable<iSceneListElement> m : oldParents) {
			m.addChild(getUnderlyingGeometry());
		}
		oldParents = null;
	}

	public void addChild(iSceneListElement e) {
		this.getUnderlyingGeometry().addChild(e);
	}

	public boolean isChild(iSceneListElement e) {
		return this.getUnderlyingGeometry().getChildren().contains(e);
	}

	public void removeChild(iSceneListElement e) {
		this.getUnderlyingGeometry().removeChild(e);
	}
}