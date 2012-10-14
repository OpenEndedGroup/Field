package field.graphics.dynamic;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import field.bytecode.protect.iInside;
import field.bytecode.protect.annotations.HiddenInAutocomplete;
import field.core.plugins.drawing.opengl.HashedCopy;
import field.graphics.core.Base;
import field.graphics.core.Base.StandardPass;
import field.graphics.core.Base.iAcceptsSceneListElement;
import field.graphics.core.Base.iSceneListElement;
import field.graphics.core.BasicGeometry;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.graphics.core.BasicGeometry.TriangleMesh_long;
import field.graphics.core.BasicUtilities;
import field.graphics.core.ResourceMonitor;
import field.launch.SystemProperties;
import field.math.graph.iMutable;
import field.math.linalg.Vector3;

/**
 * @author marc Created on Oct 21, 2003
 */
public class DynamicMesh_long implements iDynamicMesh, iInside, iRemoveable, iAcceptsSceneListElement {

	@HiddenInAutocomplete
	public static DynamicMesh_long unshadedMesh() {
		BasicGeometry.TriangleMesh_long lines = new BasicGeometry.TriangleMesh_long(new BasicUtilities.Position());
		lines.rebuildTriangle(0);
		lines.rebuildVertex(0);
		// lines.addChild(new BasicUtilities.Smooth());
		// lines.addChild(new BasicUtilities.DisableDepthTest(false));
		// lines.addChild(new BasicUtilities.PolygonOffset(20, 20));

		DynamicMesh_long ret = new DynamicMesh_long(lines);
		return ret;
	}

	@HiddenInAutocomplete
	public static DynamicMesh_long unshadedMesh(StandardPass pass) {
		BasicGeometry.TriangleMesh_long lines = new BasicGeometry.TriangleMesh_long(pass);
		lines.setCoordindateFrameProvider(new BasicUtilities.Position());
		lines.rebuildTriangle(0);
		lines.rebuildVertex(0);
		// lines.addChild(new BasicUtilities.Smooth());
		// lines.addChild(new BasicUtilities.DisableDepthTest(false));
		// lines.addChild(new BasicUtilities.PolygonOffset(20, 20));

		DynamicMesh_long ret = new DynamicMesh_long(lines);
		return ret;
	}

	protected int shrink_policy_tries = 0;

	protected float shrink_policy_min_fraction = 0.75f;

	protected int elementSize = 3; // triangles

	protected Base.iLongGeometry basis;

	protected int vertexCursor;

	protected int triangleCursor;

	protected int underlayingMaxStorageVertex;

	protected int underlayingMaxStorageTriangle;

	protected FloatBuffer cachedVertexBuffer;

	protected IntBuffer cachedTriangleBuffer;

	protected FloatBuffer[] cachedAuxBuffer = new FloatBuffer[16];

	protected int[] cachedAuxBufferWidth = new int[16];

	protected ArrayList faceToVertex = new ArrayList();

	protected boolean shrink = false;

	protected int shrink_vertex_to = 0;

	protected int shrink_triangle_to = 0;

	float growthFactor = 1.5f;

	int openCount = 0;

	int closeCount = 0;

	public DynamicMesh_long(Base.iLongGeometry from) {
		this.basis = from;
		this.basis.rebuildTriangle(0);
		this.basis.rebuildVertex(0);
		this.underlayingMaxStorageVertex = from.numVertex();
		this.underlayingMaxStorageTriangle = from.numTriangle();
	}

	public DynamicMesh_long() {
		this.basis = new TriangleMesh_long();
		this.basis.rebuildTriangle(0);
		this.basis.rebuildVertex(0);
		this.underlayingMaxStorageVertex = basis.numVertex();
		this.underlayingMaxStorageTriangle = basis.numTriangle();
	}

	public void close() {
		openCount--;
		if (openCount == 0) {

			if (!dirty) {
				// ;//System.out.println(" cached mesh is clean ");
				if (this.getUnderlyingGeometry() instanceof TriangleMesh)
					((TriangleMesh) this.getUnderlyingGeometry()).forceClean();

			}
			// ;//System.out.println(" limits are <" + vertexCursor
			// +
			// "> <" + triangleCursor + ">");

			if (basis instanceof BasicGeometry.TriangleMesh) {
				((BasicGeometry.TriangleMesh) basis).setVertexLimit(vertexCursor);
				((BasicGeometry.TriangleMesh) basis).setTriangleLimit(triangleCursor);

			}

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

	HashedCopy copy;

	int[] zeroTo15 = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };

	@HiddenInAutocomplete
	public void copyFrom(SubMesh_long subMesh) {
		copyFrom(subMesh, true);
	}

	boolean iWouldHaveCached = false;

	@HiddenInAutocomplete
	static public final boolean noCache = SystemProperties.getIntProperty("disableCanvasCache", 0) == 1;

	@HiddenInAutocomplete
	public void copyFrom(SubMesh_long subMesh, boolean canSkip) {

		if (subMesh.delegate.cachedVertexBuffer == null)
			return;
		if (subMesh.delegate.cachedTriangleBuffer == null)
			return;

		dirty = true;

		if (copy == null) {
			// ;//System.out.println(" new hashed copy");
			copy = new HashedCopy();
		}

		if (canSkip && !noCache)
			if (subMesh.copy != null) {
				if (!subMesh.copy.copyInto(copy, vertexCursor)) {

					// ;//System.out.println(" -- I would have cached -- ");
					iWouldHaveCached = true;

					// debugMeshCaching(subMesh,
					// vertexCursor);
					//
					vertexCursor += subMesh.delegate.vertexCursor;
					triangleCursor += subMesh.delegate.triangleCursor;
					//
					// ;//System.out.println(" cursor now "+vertexCursor+" "+triangleCursor+">");
					//
					// // if (this.getUnderlyingGeometry()
					// // instanceof TriangleMesh)
					// // ((TriangleMesh)
					// //
					// this.getUnderlyingGeometry()).forceClean();
					//
					// //dirty = true;
					//
					return;
				} else {
					iWouldHaveCached = false;
				}
			} else {
				iWouldHaveCached = false;
			}
		else {
			iWouldHaveCached = false;
		}

		dirty = true;

		for (int i = 0; i < subMesh.delegate.cachedAuxBuffer.length; i++) {
			if (subMesh.delegate.cachedAuxBuffer[i] != null) {
				subMesh.delegate.cachedAuxBuffer[i].rewind();
				subMesh.delegate.cachedAuxBuffer[i].limit(subMesh.delegate.vertexCursor * subMesh.delegate.cachedAuxBufferWidth[i]);
			}
		}

		subMesh.delegate.cachedVertexBuffer.rewind();
		subMesh.delegate.cachedVertexBuffer.limit(subMesh.delegate.vertexCursor * 3);
		subMesh.delegate.cachedTriangleBuffer.limit(subMesh.delegate.triangleCursor * subMesh.delegate.elementSize);

		subMesh.delegate.cachedTriangleBuffer.rewind();

		this.inject(subMesh.delegate.cachedVertexBuffer, subMesh.delegate.cachedTriangleBuffer, zeroTo15, subMesh.delegate.cachedAuxBuffer, 0);

		// subMesh.delegate.cachedVertexBuffer.clear();
		// subMesh.delegate.cachedTriangleBuffer.clear();

	}

	private void debugMeshCaching(SubMesh_long subMesh, int vertexCursor) {

		System.err.println(" debugging mesh caching ");
		try {
			FloatBuffer from = subMesh.delegate.cachedVertexBuffer;
			IntBuffer triangle = subMesh.delegate.cachedTriangleBuffer;

			int offset = (0 - vertexCursor);

			if (cachedVertexBuffer == null)
				cachedVertexBuffer = basis.vertex();

			cachedVertexBuffer.position(vertexCursor * 3);

			if (cachedTriangleBuffer == null)
				cachedTriangleBuffer = basis.longTriangle();

			cachedTriangleBuffer.position(triangleCursor * elementSize);

			subMesh.delegate.cachedVertexBuffer.rewind();
			subMesh.delegate.cachedVertexBuffer.limit(subMesh.delegate.vertexCursor * 3);
			subMesh.delegate.cachedTriangleBuffer.limit(subMesh.delegate.triangleCursor * subMesh.delegate.elementSize);

			subMesh.delegate.cachedTriangleBuffer.rewind();

			int r = from.remaining();
			for (int i = 0; i < r; i++) {
				float f1 = cachedVertexBuffer.get();
				float f2 = from.get();
				;// System.out.println((f1 != f2 ? "!!!!" : "")
					// + f1 + " " + f2);
			}

			;// System.out.println(" -- triangle -- ");

			int num = triangle.remaining();
			for (int i = 0; i < num; i++) {
				int t1 = cachedTriangleBuffer.get();
				int t2 = (triangle.get() - offset);

				;// System.out.println((t1 != t2 ? "!!!!" : "")
					// + t1 + " " + t2);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@HiddenInAutocomplete
	public void clearCache() {
		copy = null;
	}

	@HiddenInAutocomplete
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

	@HiddenInAutocomplete
	public void hintPrimitiveType(int primType) {
	}

	@HiddenInAutocomplete
	public void inject(FloatBuffer vertex, IntBuffer triangle, int[] auxs, FloatBuffer[] auxBuffers, int oldVertexCursor) {

		dirty = true;

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

		checkTriangleStorage(triangleCursor, triangle.remaining() / elementSize);
		cachedTriangleBuffer.position(triangleCursor * elementSize);

		// cachedTriangleBuffer.put(triangle);
		int num = triangle.remaining();
		if (offset != 0)
			for (int i = 0; i < num; i++) {
				int c = (triangle.get() - offset);

				// assert c>0;
				cachedTriangleBuffer.put(c);
			}
		else
			safePut(cachedTriangleBuffer, triangle);

		cachedTriangleBuffer.position(0);
		triangleCursor += triangle.limit() / elementSize;

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

	/**
	 * todo: remove this nonsense once we'er running in java 6
	 */
	private void safePut(FloatBuffer to, FloatBuffer from) {

		// if (true)
		// {
		// to.put(from);
		// return;
		// }

		if (to.isDirect() && from.isDirect()) {

			if (iWouldHaveCached) {
				// new Exception().printStackTrace();
				// int a = from.remaining();
				// int s = from.position();
				// int s2 = to.position();
				// for (int i = 0; i < a; i++) {
				// float x = from.get(s + i);
				// float y = to.get(s2 + i);
				// ;//System.out.println((x!=y ?
				// "!!!!!!!!!!!!!!!!!! " : "")+x + " " + y);
				// }
			} else
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

	private void safePut(IntBuffer to, IntBuffer from) {

		// if (true)
		// {
		// to.put(from);
		// return;
		// }

		if (to.isDirect() && from.isDirect()) {
			if (iWouldHaveCached) {

			} else
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
		dirty = true;

		checkTriangleStorage(triangleCursor, 1);
		int triangleCursor3 = 3 * triangleCursor;
		cachedTriangleBuffer.put(triangleCursor3, v1);
		cachedTriangleBuffer.put(triangleCursor3 + 1, v2);
		cachedTriangleBuffer.put(triangleCursor3 + 2, v3);

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

	DynamicMesh_tessSupport t;

	public void nextContour(List<Vector3> m) {

		if (m.size() < 3)
			return;
		if (m.size() == 3) {
			nextFace(m.get(0), m.get(1), m.get(2));
			return;
		}

		if (t == null)
			t = new DynamicMesh_tessSupport(this);
		t.tessSingleContour(m);
	}

	/**
	 * returns face number
	 */
	public int nextFace(Vector3 v1, Vector3 v2, Vector3 v3) {
		dirty = true;
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

		cachedTriangleBuffer.put(triangleCursor3, vertexCursor);
		cachedTriangleBuffer.put(triangleCursor3 + 1, (vertexCursor + 1));
		cachedTriangleBuffer.put(triangleCursor3 + 2, (vertexCursor + 2));

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
		dirty = true;
		checkVertexStorage(vertexCursor, 3);
		int vertexCursor3 = 3 * vertexCursor;

		cachedVertexBuffer.put(vertexCursor3, v1.get(0));
		cachedVertexBuffer.put(vertexCursor3 + 1, v1.get(1));
		cachedVertexBuffer.put(vertexCursor3 + 2, v1.get(2));

		clearCache();
		return vertexCursor++;
	}

	@HiddenInAutocomplete
	public Vector3 oldVertex() {
		try {
			checkVertexStorage(vertexCursor, 3);
			float x = cachedVertexBuffer.get(vertexCursor * 3);
			float y = cachedVertexBuffer.get(vertexCursor * 3 + 1);
			float z = cachedVertexBuffer.get(vertexCursor * 3 + 2);
			return new Vector3(x, y, z);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public int nextVertex(float x, float y, float z) {
		dirty = true;
		checkVertexStorage(vertexCursor, 3);
		int vertexCursor3 = 3 * vertexCursor;

		cachedVertexBuffer.put(vertexCursor3, x);
		cachedVertexBuffer.put(vertexCursor3 + 1, y);
		cachedVertexBuffer.put(vertexCursor3 + 2, z);

		clearCache();
		return vertexCursor++;
	}

	boolean dirty = false;

	public void open() {
		if (openCount == 0) {
			dirty = false;
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
			if (copy == null)
				copy = new HashedCopy();
			else
				copy.reset();
		}
		openCount++;
	}

	@HiddenInAutocomplete
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
		dirty = true;
		checkAuxStorage(id, 1);
		cachedAuxBuffer[id].put(vertex * 1, a);
		clearCache();
	}

	public void setAux(int vertex, int id, float a, float b) {
		dirty = true;
		checkAuxStorage(id, 2);
		cachedAuxBuffer[id].put(vertex * 2, a);
		cachedAuxBuffer[id].put(vertex * 2 + 1, b);
		clearCache();
	}

	public void setAux(int vertex, int id, float a, float b, float c) {
		dirty = true;
		checkAuxStorage(id, 3);
		cachedAuxBuffer[id].put(vertex * 3, a);
		cachedAuxBuffer[id].put(vertex * 3 + 1, b);
		cachedAuxBuffer[id].put(vertex * 3 + 2, c);
		clearCache();
	}

	public void setAux(int vertex, int id, float a, float b, float c, float d) {
		dirty = true;
		checkAuxStorage(id, 4);
		cachedAuxBuffer[id].put(vertex * 4, a);
		cachedAuxBuffer[id].put(vertex * 4 + 1, b);
		cachedAuxBuffer[id].put(vertex * 4 + 2, c);
		cachedAuxBuffer[id].put(vertex * 4 + 3, d);
		clearCache();
	}

	@HiddenInAutocomplete
	public void setPositionOfVertex(int n, Vector3 v1) {
		dirty = true;
		checkVertexStorage(n, 3);
		int vertexCursor3 = 3 * n;

		assert vertexCursor3 < cachedVertexBuffer.capacity() : n + " " + vertexCursor3 + " " + cachedVertexBuffer.capacity() + " " + cachedVertexBuffer.limit();

		cachedVertexBuffer.put(vertexCursor3, v1.get(0));
		cachedVertexBuffer.put(vertexCursor3 + 1, v1.get(1));
		cachedVertexBuffer.put(vertexCursor3 + 2, v1.get(2));
		if (vertexCursor < n + 1)
			vertexCursor = n + 1;
		clearCache();
	}

	@HiddenInAutocomplete
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
			cachedTriangleBuffer = basis.longTriangle();
		cachedTriangleBuffer.clear();
		if (cachedTriangleBuffer.capacity() < ((at + plus) * elementSize)) {
			basis.rebuildTriangle((int) (plus * elementSize + growthFactor * cachedTriangleBuffer.capacity() / elementSize));
			dirty = true;
			IntBuffer tt = basis.longTriangle();
			tt.clear();
			cachedTriangleBuffer = tt.put((IntBuffer) cachedTriangleBuffer.rewind());
		}
	}

	protected void checkVertexStorage(int at, int plus) {

		// System.err.println(" check vertex storage
		// <"+Runtime.getRuntime().freeMemory()+">");
		try {
			if (cachedVertexBuffer == null)
				cachedVertexBuffer = basis.vertex();
			cachedVertexBuffer.clear();
			if (cachedVertexBuffer.capacity() < ((at * 3 + plus))) {

				basis.rebuildVertex((int) (plus / 3 + growthFactor * Math.max((at + plus / 3), cachedVertexBuffer.capacity() / 3)));
				cachedVertexBuffer = basis.vertex().put((FloatBuffer) cachedVertexBuffer.rewind());
				dirty = true;

				for (int i = 0; i < cachedAuxBuffer.length; i++)
					if (cachedAuxBuffer[i] != null) {
						cachedAuxBuffer[i] = basis.aux(i, cachedAuxBufferWidth[i]).put((FloatBuffer) cachedAuxBuffer[i].rewind());
					}
			}
		} catch (OutOfMemoryError e) {
			throw e;
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

	int[] elementFace = new int[2];
	int elementAt = 0;

	@HiddenInAutocomplete
	public void nextFace(int v1) {
		dirty = true;
		if (elementAt == 2) {
			nextFace(elementFace[0], elementFace[1], v1);
			elementAt = 0;
		} else
			elementFace[elementAt++] = v1;
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

	public void rect(float x, float y, float w, float h) {
		int a = nextVertex(x, y, 0);
		int b = nextVertex(x + w, y, 0);
		int c = nextVertex(x + w, y + h, 0);
		int d = nextVertex(x, y + h, 0);
		nextFace(a, b, c);
		nextFace(a, c, d);
	}

	public void rectTextured(float x, float y, float w, float h, float tw, float th) {
		int a = nextVertex(x, y, 0);
		int b = nextVertex(x + w, y, 0);
		int c = nextVertex(x + w, y + h, 0);
		int d = nextVertex(x, y + h, 0);
		nextFace(a, b, c);
		nextFace(a, c, d);
		
		setAux(a, Base.texture0_id, 0, 0);
		setAux(b, Base.texture0_id, tw, 0);
		setAux(c, Base.texture0_id, tw, th);
		setAux(d, Base.texture0_id, 0, th);
	}

}
