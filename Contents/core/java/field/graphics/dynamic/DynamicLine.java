package field.graphics.dynamic;

import java.util.Stack;

import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.BasicGeometry;
import field.graphics.core.BasicUtilities;
import field.graphics.core.Base.iAcceptsSceneListElement;
import field.graphics.core.Base.iSceneListElement;
import field.graphics.core.BasicGeometry.LineList;
import field.math.linalg.Vector3;
import field.math.util.CubicTools;
import field.util.ANSIColorUtils;

/**
 * @author marc Created on Oct 22, 2003
 */
public class DynamicLine extends DynamicMesh implements iLineOutput {

	public static DynamicLine coloredLine(iAcceptsSceneListElement sceneList, float width) {

		BasicGLSLangProgram program = new BasicGLSLangProgram("content/shaders/TestGLSLangVertex.glslang", "content/shaders/VertexColorFragment.glslang");
		BasicGeometry.LineList lines = new BasicGeometry.LineList(new BasicUtilities.Position()).setWidth(width);
		lines.rebuildTriangle(0);
		lines.rebuildVertex(0);
		lines.addChild(program);
		if (sceneList != null)
			sceneList.addChild(lines);
		DynamicLine ret = new DynamicLine(lines);
		return ret;
	}


	public static DynamicLine unshadedLine(iSceneListElement into, float width) {
		BasicGeometry.LineList lines = new BasicGeometry.LineList(new BasicUtilities.Position()).setWidth(width);
		lines.rebuildTriangle(0);
		lines.rebuildVertex(0);
		lines.addChild(new BasicUtilities.Smooth());

		if (into != null)
			into.addChild(lines);

		DynamicLine ret = new DynamicLine(lines);
		return ret;
	}

	protected int lineStartAt = -1;

	int splineStartAt = -1;

	Vector3 lastPosition = new Vector3();

	Vector3 splineTmp = new Vector3();

	Vector3 before = new Vector3();

	Vector3 after = new Vector3();

	int[] auxSplineCursors = new int[16];

	float[][] auxSplineCursorData = new float[16][4];

	int[] auxSplineDimensions = new int[16];

	int beginCount = 0;

	public DynamicLine(LineList from) {
		super(from);
		this.elementSize = 2;
	}

	public void beginSpline(iLineIdentifier identifier) {
		splineStartAt = vertexCursor;
		beginCount = 1;
		lineStartAt = -2;
	}

	public void curveTo(Vector3 to, Vector3 cp1, Vector3 cp2, int samples) {
		before.set(lastPosition.x - (cp1.x - lastPosition.x) * 6, lastPosition.y - (cp1.y - lastPosition.y) * 6, lastPosition.z - (cp1.z - lastPosition.z) * 6);
		after.set(to.x + (to.x - cp2.x) * 6, to.y + (to.y - cp2.y) * 6, to.z + (to.z - cp2.z) * 6);
		for (int i = 0; i < samples; i++) {
			float a = i / (float) (samples - 1);
			CubicTools.cubic(a, before, lastPosition, to, after, splineTmp);
			nextVertex(splineTmp);
		}
		lastPosition.set(splineTmp);
	}

	boolean insideLine = false;

	public void endLine() {
		insideLine = false;
		// if (lineStartAt == -1) throw new
		// IllegalStateException(" line ended without being started");
		if (lineStartAt == -1) {
			System.err.println(ANSIColorUtils.red(" -- \n"));
			System.err.println(ANSIColorUtils.red(" -- \n"));
			System.err.println(ANSIColorUtils.red(" -- \n"));
			System.err.println(ANSIColorUtils.red(" -- \n"));
			System.err.println(ANSIColorUtils.red(" -- warning: line ended without being started "));
			new Exception().printStackTrace();
			System.err.println(ANSIColorUtils.red(" -- \n"));
			System.err.println(ANSIColorUtils.red(" -- \n"));
			System.err.println(ANSIColorUtils.red(" -- \n"));
			System.err.println(ANSIColorUtils.red(" -- \n"));
		} else {
			checkTriangleStorage(triangleCursor, vertexCursor - lineStartAt);
			for (int i = 0; i < vertexCursor - lineStartAt - 1; i++) {
				int triangleCursor2 = 2 * triangleCursor;
				cachedTriangleBuffer.put(triangleCursor2, (short) (i + lineStartAt));
				cachedTriangleBuffer.put(triangleCursor2 + 1, (short) (i + lineStartAt + 1));
				triangleCursor++;
			}
			lineStartAt = -1;
		}
	}

	public void endLineWithAdjacency() {
		// if (lineStartAt == -1) throw new
		// IllegalStateException(" line ended without being started");
		if (lineStartAt == -1) {
			System.err.println(ANSIColorUtils.red(" -- \n"));
			System.err.println(ANSIColorUtils.red(" -- \n"));
			System.err.println(ANSIColorUtils.red(" -- \n"));
			System.err.println(ANSIColorUtils.red(" -- \n"));
			System.err.println(ANSIColorUtils.red(" -- warning: line ended without being started "));
			new Exception().printStackTrace();
			System.err.println(ANSIColorUtils.red(" -- \n"));
			System.err.println(ANSIColorUtils.red(" -- \n"));
			System.err.println(ANSIColorUtils.red(" -- \n"));
			System.err.println(ANSIColorUtils.red(" -- \n"));
		} else {
			checkTriangleStorage(triangleCursor, (vertexCursor - lineStartAt) * 2);
			for (int i = 0; i < vertexCursor - lineStartAt - 1; i++) {
				int triangleCursor2 = 2 * triangleCursor;

				short a1 = (short) ((i > 0 ? i - 1 : i) + lineStartAt);
				cachedTriangleBuffer.put(triangleCursor2, a1);
				short a2 = (short) (i + lineStartAt);
				cachedTriangleBuffer.put(triangleCursor2 + 1, a2);
				short a3 = (short) (i + lineStartAt + 1);
				cachedTriangleBuffer.put(triangleCursor2 + 2, a3);
				short a4 = (short) ((i < (vertexCursor - lineStartAt - 2) ? i + 1 : i) + lineStartAt + 1);
				cachedTriangleBuffer.put(triangleCursor2 + 3, a4);

				// ;//System.out.println(" wrote <"+triangleCursor2+"> <"+a1+" "+a2+" "+a3+" "+a4+">");

				triangleCursor++;
				triangleCursor++;
			}
			lineStartAt = -1;
		}
	}

	public void endSpline() {
		endLine();
	}

	Stack<Integer> vertexCursorOpenStack = new Stack<Integer>();
	Stack<Integer> triangleCursorOpenStack = new Stack<Integer>();

	public void open() {
		super.open();
		vertexCursorOpenStack.push(vertexCursor);
		triangleCursorOpenStack.push(triangleCursor);
	};

	@Override
	public void close() {
		if (openCount > 0) {
			vertexCursorOpenStack.pop();
			triangleCursorOpenStack.pop();
		}
		super.close();

	}

	public int getOpenCount() {
		return openCount;
	}

	/** @return */
	@Override
	public LineList getUnderlyingGeometry() {
		return (LineList) basis;
	}

	public boolean isClosed() {
		assert openCount == 0 : openCount;
		return true;
	}

	public void lineTo(Vector3 to) {
		if (!insideLine)
			moveTo(to);
		else {
			nextVertex(to);
			lastPosition.set(to);
		}
	}

	public void moveTo(Vector3 to) {

		startLine();
		for (int i = 0; i < auxSplineCursors.length; i++) {
			auxSplineCursors[i] = this.vertexCursor;
			// auxSplineCursorData[i] = null;
		}
		nextVertex(to);
		lastPosition.set(to);
	}

	public int nextEdge(Vector3 v1, Vector3 v2) {
		checkTriangleStorage(triangleCursor, 1);
		checkVertexStorage(vertexCursor, 6);

		int vertexCursor3 = 3 * vertexCursor;
		int triangleCursor2 = 2 * triangleCursor;
		cachedVertexBuffer.put(vertexCursor3, v1.get(0));
		cachedVertexBuffer.put(vertexCursor3 + 1, v1.get(1));
		cachedVertexBuffer.put(vertexCursor3 + 2, v1.get(2));
		vertexCursor3 += 3;
		cachedVertexBuffer.put(vertexCursor3, v2.get(0));
		cachedVertexBuffer.put(vertexCursor3 + 1, v2.get(1));
		cachedVertexBuffer.put(vertexCursor3 + 2, v2.get(2));
		vertexCursor3 += 3;

		cachedTriangleBuffer.put(triangleCursor2, (short) vertexCursor);
		cachedTriangleBuffer.put(triangleCursor2 + 1, (short) (vertexCursor + 1));

		int[] rec;
		if (faceToVertex.size() <= triangleCursor)
			faceToVertex.add(rec = new int[2]);
		else
			rec = (int[]) faceToVertex.get(triangleCursor);
		rec[0] = vertexCursor;
		rec[1] = vertexCursor + 1;

		vertexCursor += 2;
		return triangleCursor++;
	}

	public int nextFace(int v1, int v2) {
		checkTriangleStorage(triangleCursor, 1);
		int triangleCursor2 = 2 * triangleCursor;
		cachedTriangleBuffer.put(triangleCursor2, (short) v1);
		cachedTriangleBuffer.put(triangleCursor2 + 1, (short) v2);

		int[] rec;
		if (faceToVertex.size() <= triangleCursor)
			faceToVertex.add(rec = new int[2]);
		else
			rec = (int[]) faceToVertex.get(triangleCursor);
		rec[0] = v1;
		rec[1] = v2;

		return triangleCursor++;
	}

	@Override
	public void nextFace(int v1) {
		if (elementAt == 1) {
			nextFace(elementFace[0], v1);
			elementAt = 0;
		} else
			elementFace[elementAt++] = v1;
	}

	public void setAuxOnLine(int id, float a1) {
		for (int m = vertexCursorOpenStack.peek(); m < vertexCursor; m++)
			setAux(m, id, a1);
	}

	public void setAuxOnLine(int id, float a1, float a2) {
		for (int m = vertexCursorOpenStack.peek(); m < vertexCursor; m++)
			setAux(m, id, a1, a2);
	}

	public void setAuxOnLine(int id, float a1, float a2, float a3) {
		for (int m = vertexCursorOpenStack.peek(); m < vertexCursor; m++)
			setAux(m, id, a1, a2, a3);
	}

	public void setAuxOnLine(int id, float a1, float a2, float a3, float a4) {
		for (int m = vertexCursorOpenStack.peek(); m < vertexCursor; m++)
			setAux(m, id, a1, a2, a3, a4);
	}

	public void setAuxOnSpline(int id, float a1) {
		float[] from = auxSplineCursorData[id];
		if (from == null)
			auxSplineCursorData[id] = from = new float[1];

		for (int n = auxSplineCursors[id]; n < vertexCursor; n++) {
			float alpha = (n - auxSplineCursors[id]) / (vertexCursor - auxSplineCursors[id] - 1f);
			if (alpha != alpha)
				alpha = 1;
			float becomes = from[0] * (1 - alpha) + alpha * a1;

			setAux(n, id, becomes);
		}

		from[0] = a1;
		auxSplineCursors[id] = vertexCursor;
	}

	public void setAuxOnSpline(int id, float a1, float a2) {
		float[] from = auxSplineCursorData[id];
		if (from == null)
			auxSplineCursorData[id] = from = new float[2];

		for (int n = auxSplineCursors[id]; n < vertexCursor; n++) {
			float alpha = (n - auxSplineCursors[id]) / (vertexCursor - auxSplineCursors[id] - 1f);
			if (alpha != alpha)
				alpha = 1;
			float becomes0 = from[0] * (1 - alpha) + alpha * a1;
			float becomes1 = from[1] * (1 - alpha) + alpha * a2;

			setAux(n, id, becomes0, becomes1);
		}

		from[0] = a1;
		from[1] = a2;
		auxSplineCursors[id] = vertexCursor;
	}

	public void setAuxOnSpline(int id, float a1, float a2, float a3) {
		float[] from = auxSplineCursorData[id];
		if (from == null)
			auxSplineCursorData[id] = from = new float[3];

		for (int n = auxSplineCursors[id]; n < vertexCursor; n++) {
			float alpha = (n - auxSplineCursors[id]) / (vertexCursor - auxSplineCursors[id] - 1f);
			if (alpha != alpha)
				alpha = 1;
			float becomes0 = from[0] * (1 - alpha) + alpha * a1;
			float becomes1 = from[1] * (1 - alpha) + alpha * a2;
			float becomes2 = from[2] * (1 - alpha) + alpha * a3;

			setAux(n, id, becomes0, becomes1, becomes2);
		}

		from[0] = a1;
		from[1] = a2;
		from[2] = a3;
		auxSplineCursors[id] = vertexCursor;
	}

	public void setAuxOnSpline(int id, float a1, float a2, float a3, float a4) {

		float[] from = auxSplineCursorData[id];
		if (from == null) {
			auxSplineCursorData[id] = from = new float[] { a1, a2, a3, a4 };
		}
		if (auxSplineCursors[id] > vertexCursor)
			auxSplineCursors[id] = splineStartAt;

		for (int n = auxSplineCursors[id]; n < vertexCursor; n++) {
			float alpha = (n - auxSplineCursors[id]) / (vertexCursor - auxSplineCursors[id] - 1f);
			if (alpha != alpha)
				alpha = 1;
			float becomes0 = from[0] * (1 - alpha) + alpha * a1;
			float becomes1 = from[1] * (1 - alpha) + alpha * a2;
			float becomes2 = from[2] * (1 - alpha) + alpha * a3;
			float becomes3 = from[3] * (1 - alpha) + alpha * a4;

			setAux(n, id, becomes0, becomes1, becomes2, becomes3);
		}

		from[0] = a1;
		from[1] = a2;
		from[2] = a3;
		from[3] = a4;

		auxSplineCursors[id] = vertexCursor;
	}

	public void startLine() {
		lineStartAt = vertexCursor;
		insideLine = true;
	}

	public int[] vertexForEdge(int edge) {
		return vertexForFace(edge);
	}

}