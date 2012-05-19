package field.graphics.dynamic;

import field.graphics.core.BasicUtilities;
import field.graphics.core.PointList;
import field.graphics.core.BasicGeometry.LineList;
import field.math.linalg.Vector3;

public class SubPoint extends SubMesh{

	public SubPoint() {
		super(false);

		dummyMesh = new PointList(new BasicUtilities.Position());
		dummyMesh.rebuildTriangle(0);
		dummyMesh.rebuildVertex(0);
		delegate = new DynamicPointlist((PointList) dummyMesh);

	}
	
	public PointList getUnderlyingGeometry() {
		return (PointList) delegate.getUnderlyingGeometry();
	}

	
	public int nextFace(Vector3 v1, Vector3 v2, Vector3 v3) {
		return delegate.nextFace(v1,v2,v3);
	}

	public int nextFace(int v1, int v2, int v3) {
		return delegate.nextFace(v1, v2, v3);
	}
}
