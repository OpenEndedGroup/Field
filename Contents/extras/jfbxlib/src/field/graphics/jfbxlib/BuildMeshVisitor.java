package field.graphics.jfbxlib;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import field.math.linalg.Matrix4;
import field.math.linalg.Quaternion;
import field.math.linalg.Vector3;
import field.namespace.generic.Generics.Pair;
import field.util.HashMapOfLists;


/**
 * handles triangles and quads, pushes offset into mesh statically
 * 
 * todo: textures, normals, pvc's
 */
public class BuildMeshVisitor extends AbstractVisitor {

	private final boolean pushOffsetTransformsDown;

	Map<String, Mesh> constructedMeshes = new HashMap<String, Mesh>();
	HashMapOfLists<String, Pair<Double, Mesh>> constructedMeshesList = new HashMapOfLists<String, Pair<Double, Mesh>>();

	public BuildMeshVisitor(boolean pushOffsetTransformsDown) {
		this.pushOffsetTransformsDown = pushOffsetTransformsDown;
	}

	public BuildMeshVisitor(boolean pushOffsetTransformsDown, JFBXVisitor delegate) {
		this(pushOffsetTransformsDown);
		super.delegate = delegate;
	}

	public Map<String, Mesh> getMeshes() {
		return constructedMeshes;
	}

	boolean captureResultMeshes = false;

	private double currentTime;

	public BuildMeshVisitor setCaptureResultMeshes(boolean captureResultMeshes) {
		this.captureResultMeshes = captureResultMeshes;
		return this;
	}

	@Override
	public void beginScene(long time) {
		currentTime = convertJFBXTimeToSeconds(time);
	}
	
	long  currentUID = -1;

	Matrix4 currentPushdownTransform = null;

	private Mesh currentMesh;

	private boolean isTriangle;

	private String currentName;

	@Override
	public void visitTransformBegin(String name, int type, long uid) {
		super.visitTransformBegin(name, type, uid);
		currentUID = uid;
		currentName = name;
	}

	@Override
	public void visitTransformInfo(float oq0, float oq1, float oq2, float oq3, float ot0, float ot1, float ot2, float os0, float os1, float os2, float q0, float q1, float q2, float q3, float t0, float t1, float t2, float s0, float s1, float s2) {
		super.visitTransformInfo(oq0, oq1, oq2, oq3, ot0, ot1, ot2, os0, os1, os2, q0, q1, q2, q3, t0, t1, t2, s0, s1, s2);

		if (pushOffsetTransformsDown) {
			Quaternion oq = new Quaternion(oq0, oq1, oq2, oq3);
			Vector3 ot = new Vector3(ot0, ot1, ot2);
			Vector3 os = new Vector3(os0, os1, os2);

			Quaternion q = new Quaternion(q0, q1, q2, q3);
			Vector3 t = new Vector3(t0, t1, t2);
			Vector3 s = new Vector3(s0, s1, s2);

			Matrix4 above = new Matrix4(q, t, s);
			Matrix4 here = new Matrix4(oq, ot, os);

			above.invert();
			Matrix4 left = new Matrix4().mul(above, here);

			currentPushdownTransform = left;
		}
	}

	@Override
	public void visitMeshBegin(int numVertex, int numPolygon) {
		assert currentUID != -1;
		;//;//System.out.println(" mesh begin ");
		super.visitMeshBegin(numVertex, numPolygon);
		currentMesh = new Mesh();
		if (numVertex > 0 && numPolygon > 0)
		{
			if (constructedMeshes.containsKey(currentName))
			{
				System.err.println(" warning, overwrote mesh called <"+currentName+">");
			}
			constructedMeshes.put(currentName, currentMesh);
			constructedMeshesList.addToList(currentName, new Pair<Double, Mesh>(currentTime, currentMesh));
		}
		currentMesh.numTriangle = numPolygon * 2;
		currentMesh.numVertex = numVertex;
		currentMesh.triangleArray = new int[currentMesh.numTriangle * 3];
	}

	@Override
	public void visitMeshBindBegin() {
		super.visitMeshBindBegin();
	}

	@Override
	public void visitMeshBindVertexArray(ByteBuffer vertexData) {
		vertexData.order(ByteOrder.nativeOrder());
		super.visitMeshBindVertexArray(vertexData);

		assert vertexData.capacity() == currentMesh.numVertex * 4 * 3 : vertexData.capacity() + " " + currentMesh.numVertex;

		currentMesh.vertexArray = new float[3 * currentMesh.numVertex];
		vertexData.rewind();

		vertexData.asFloatBuffer().get(currentMesh.vertexArray);

		if (pushOffsetTransformsDown) {
			Vector3 v = new Vector3();
			for (int i = 0; i < currentMesh.numVertex; i++) {
				v.x = currentMesh.vertexArray[3 * i + 0];
				v.y = currentMesh.vertexArray[3 * i + 1];
				v.z = currentMesh.vertexArray[3 * i + 2];
				currentPushdownTransform.transformPosition(v);
				currentMesh.vertexArray[3 * i + 0] = v.x;
				currentMesh.vertexArray[3 * i + 1] = v.y;
				currentMesh.vertexArray[3 * i + 2] = v.z;
			}
		}
	}

	int[] indexArray = new int[4];

	int indexCursor = 0;

	private int lastVertexIndex;

	@Override
	public void visitMeshBindPolygonBegin(int numVertex) {
		super.visitMeshBindPolygonBegin(numVertex);
		if (numVertex == 3)
			isTriangle = true;
		else if (numVertex == 4)
			isTriangle = false;
		else {
			assert false : "unhandled polygon (we can do triangles or quads, but not <" + numVertex + "> in mesh <" + this.currentName + ">";
			throw new ArrayIndexOutOfBoundsException("unhandled polygon (we can do triangles or quads, but not <" + numVertex + "> in mesh <" + this.currentName + ">");
		}

		indexCursor = 0;
	}

	@Override
	public void visitMeshBindPolygonIndex(int vertexIndex) {
		super.visitMeshBindPolygonIndex(vertexIndex);

		indexArray[indexCursor++] = vertexIndex;
		lastVertexIndex = vertexIndex;
	}

	@Override
	public void visitMeshBindPolygonTexCoord(int textureCoordinateIndex, float u, float w) {
		super.visitMeshBindPolygonTexCoord(textureCoordinateIndex, u, w);

		w = 1 - w;

		// here we drop the possibility of per-polygon texturing silently.
		float[] a = currentMesh.getAttributeArray(8, 2);

		if (a[2 * lastVertexIndex + 0] > 0 || a[2 * lastVertexIndex + 1] > 0) {
			float d = (a[2 * lastVertexIndex + 0] - u) * (a[2 * lastVertexIndex + 0] - u) + (a[2 * lastVertexIndex + 1] - w) * (a[2 * lastVertexIndex + 1] - w);
			if (d > 1e-2) {
				;//;//System.out.println(" warning: per poly tex coords, really mean it <" + u + ", " + w + "> <- " + a[2 * lastVertexIndex + 0] + ", " + a[2 * lastVertexIndex + 1] + ">");
			}
		}

		a[2 * lastVertexIndex + 0] = u;
		a[2 * lastVertexIndex + 1] = w;

	}

	@Override
	public void visitMeshBindPolygonVertexAttribute(int attriuteNumber, float a) {
		super.visitMeshBindPolygonVertexAttribute(attriuteNumber, a);

		// here we drop the possibility of per-polygon attributes silently.
		float[] array = currentMesh.getAttributeArray(attriuteNumber, 1);
		array[lastVertexIndex] = a;
	}

	@Override
	public void visitMeshBindPolygonVertexAttribute(int attriuteNumber, float a, float b) {
		super.visitMeshBindPolygonVertexAttribute(attriuteNumber, a, b);

		// here we drop the possibility of per-polygon attributes silently.
		float[] array = currentMesh.getAttributeArray(attriuteNumber, 2);
		array[2 * lastVertexIndex + 0] = a;
		array[2 * lastVertexIndex + 1] = b;
	}

	// three-component attributes (normals) are added rather than blown away, this means that they need to be normalized afterwards

	@Override
	public void visitMeshBindPolygonVertexAttribute(int attriuteNumber, float a, float b, float c) {
		super.visitMeshBindPolygonVertexAttribute(attriuteNumber, a, b, c);

		// here we drop the possibility of per-polygon attributes silently.
		float[] array = currentMesh.getAttributeArray(attriuteNumber, 3);

		array[3 * lastVertexIndex + 0] += a;
		array[3 * lastVertexIndex + 1] += b;
		array[3 * lastVertexIndex + 2] += c;
	}

	@Override
	public void visitMeshBindPolygonVertexAttribute(int attriuteNumber, float a, float b, float c, float d) {
		super.visitMeshBindPolygonVertexAttribute(attriuteNumber, a, b, c, d);

		// here we drop the possibility of per-polygon attributes silently.
		float[] array = currentMesh.getAttributeArray(attriuteNumber, 4);
		array[4 * lastVertexIndex + 0] = a;
		array[4 * lastVertexIndex + 1] = b;
		array[4 * lastVertexIndex + 2] = c;
		array[4 * lastVertexIndex + 3] = d;

	}

	@Override
	public void visitMeshBindPolygonEnd() {
		super.visitMeshBindPolygonEnd();
		if (isTriangle) {
			currentMesh.triangleArray[currentMesh.triangleCursor++] = (int) indexArray[0];
			currentMesh.triangleArray[currentMesh.triangleCursor++] = (int) indexArray[1];
			currentMesh.triangleArray[currentMesh.triangleCursor++] = (int) indexArray[2];
		} else {
			currentMesh.triangleArray[currentMesh.triangleCursor++] = (int) indexArray[0];
			currentMesh.triangleArray[currentMesh.triangleCursor++] = (int) indexArray[1];
			currentMesh.triangleArray[currentMesh.triangleCursor++] = (int) indexArray[2];

//			currentMesh.triangleArray[currentMesh.triangleCursor++] = (int) indexArray[0];
//			currentMesh.triangleArray[currentMesh.triangleCursor++] = (int) indexArray[2];
//			currentMesh.triangleArray[currentMesh.triangleCursor++] = (int) indexArray[3];

			currentMesh.triangleArray[currentMesh.triangleCursor++] = (int) indexArray[2];
			currentMesh.triangleArray[currentMesh.triangleCursor++] = (int) indexArray[3];
			currentMesh.triangleArray[currentMesh.triangleCursor++] = (int) indexArray[0];

		}
	}

	@Override
	public void visitMeshBindEnd() {
		super.visitMeshBindEnd();
		if (currentMesh.triangleCursor != currentMesh.triangleArray.length) {
			int[] old = currentMesh.triangleArray;
			currentMesh.triangleArray = new int[currentMesh.triangleCursor];
			System.arraycopy(old, 0, currentMesh.triangleArray, 0, currentMesh.triangleCursor);

			for (int i = 0; i < currentMesh.extraInfo.length; i++) {
				if (currentMesh.extraInfo[i] != null && currentMesh.extraInfo[i].length == currentMesh.numVertex * 3) {
					for (int q = 0; q < currentMesh.numVertex; q++) {
						float x = currentMesh.extraInfo[i][3 * q + 0];
						float y = currentMesh.extraInfo[i][3 * q + 1];
						float z = currentMesh.extraInfo[i][3 * q + 2];

						float n = x * x + y * y + z * z;

						n = (float) Math.sqrt(n);

						if (n > 0) {
							currentMesh.extraInfo[i][3 * q + 0] /= n;
							currentMesh.extraInfo[i][3 * q + 1] /= n;
							currentMesh.extraInfo[i][3 * q + 2] /= n;
						}
					}
				}
			}

		}
		assert currentMesh.triangleCursor % 3 == 0;
		currentMesh.numTriangle = currentMesh.triangleCursor / 3;
	}

	@Override
	public void visitMeshResultBegin() {
	}

	@Override
	public void visitMeshResultEnd() {
	}

	@Override
	public void visitMeshResultVertexArray(ByteBuffer vertexData) {
		if (captureResultMeshes) {
			vertexData.order(ByteOrder.nativeOrder());
			super.visitMeshBindVertexArray(vertexData);

			assert vertexData.capacity() == currentMesh.numVertex * 4 * 3 : vertexData.capacity() + " " + currentMesh.numVertex;


			currentMesh.vertexArray = new float[3 * currentMesh.numVertex];
			vertexData.rewind();

			vertexData.asFloatBuffer().get(currentMesh.vertexArray);


			if (pushOffsetTransformsDown) {
				Vector3 v = new Vector3();
				for (int i = 0; i < currentMesh.numVertex; i++) {
					v.x = currentMesh.vertexArray[3 * i + 0];
					v.y = currentMesh.vertexArray[3 * i + 1];
					v.z = currentMesh.vertexArray[3 * i + 2];
					currentPushdownTransform.transformPosition(v);
					currentMesh.vertexArray[3 * i + 0] = v.x;
					currentMesh.vertexArray[3 * i + 1] = v.y;
					currentMesh.vertexArray[3 * i + 2] = v.z;
				}
			}

		}
	}

	@Override
	public void visitTransformEnd() {
		super.visitTransformEnd();
	}

	/**
	 * this compressed format will be good for loading and saving
	 */
	static public class Mesh implements Serializable {
		public int numVertex;

		public float[] vertexArray;

		public int numTriangle;

		public int[] triangleArray;

		public int triangleCursor;

		// uses opengl attribute indexing.
		// the reader gives us enough info to do per poly attributes, but we just do per vertex
		public float[][] extraInfo = new float[16][];

		public float[] getAttributeArray(int attribute, int elementSize) {
			if (extraInfo[attribute] == null)
				extraInfo[attribute] = new float[numVertex * elementSize];
			return extraInfo[attribute];
		}
	}

	public HashMapOfLists<String, Pair<Double, Mesh>> getMeshAnimations() {
		return constructedMeshesList;
	}

}
