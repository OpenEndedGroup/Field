package field.graphics.dynamic;

import java.util.Stack;

import field.graphics.core.Base.iAcceptsSceneListElement;
import field.graphics.core.BasicGeometry;
import field.graphics.core.BasicGeometry.QuadMesh_long;
import field.graphics.core.BasicUtilities;

/**
 * @author marc Created on Oct 22, 2003
 */
public class DynamicQuad_long extends DynamicMesh_long {

	public static DynamicQuad_long unshadedQuad(iAcceptsSceneListElement into) {
		BasicGeometry.QuadMesh_long lines = new BasicGeometry.QuadMesh_long(new BasicUtilities.Position());
		lines.rebuildTriangle(0);
		lines.rebuildVertex(0);
		lines.addChild(new BasicUtilities.Smooth());

		if (into != null) into.addChild(lines);

		DynamicQuad_long ret = new DynamicQuad_long(lines);
		return ret;
	}

	public DynamicQuad_long(QuadMesh_long from) {
		super(from);
		this.elementSize = 4;
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
		if (openCount>0)
		{
			vertexCursorOpenStack.pop();
			triangleCursorOpenStack.pop();
		}
		super.close();
		
	}
	
	/** @return */
	@Override
	public QuadMesh_long getUnderlyingGeometry() {
		return (QuadMesh_long) basis;
	}

	public boolean isClosed() {
		assert openCount == 0 : openCount;
		return true;
	}


	public int nextFace(int v1, int v2, int v3, int v4) {
		checkTriangleStorage(triangleCursor, 1);
		int triangleCursor2 = 4 * triangleCursor;
		cachedTriangleBuffer.put(triangleCursor2, v1);
		cachedTriangleBuffer.put(triangleCursor2 + 1, v2);
		cachedTriangleBuffer.put(triangleCursor2 + 2, v3);
		cachedTriangleBuffer.put(triangleCursor2 + 3, v4);

		int[] rec;
		if (faceToVertex.size() <= triangleCursor)
			faceToVertex.add(rec = new int[4]);
		else
			rec = (int[]) faceToVertex.get(triangleCursor);
		rec[0] = v1;
		rec[1] = v2;
		rec[2] = v3;
		rec[3] = v4;

		return triangleCursor++;
	}

	
	
	int[] elementFace = new int[3];
	int elementAt = 0;

	public void nextFace(int v1) {
		if (elementAt == 3) {
			nextFace(elementFace[0], elementFace[1], elementFace[2], v1);
			elementAt = 0;
		} else
			elementFace[elementAt++] = v1;
	}

}