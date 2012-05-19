package field.graphics.dynamic;

import field.graphics.core.BasicUtilities;
import field.graphics.core.ResourceMonitor;
import field.graphics.core.BasicGeometry.LineList;
import field.math.linalg.Vector3;

public class SubLine extends SubMesh implements iLineOutput {

	public SubLine() {
		super(false);
		dummyMesh = new LineList(new BasicUtilities.Position());
		dummyMesh.rebuildTriangle(0);
		dummyMesh.rebuildVertex(0);
		delegate = new DynamicLine((LineList) dummyMesh);
	}
	
	public void fastDispose()
	{
		dummyMesh.deallocate(ResourceMonitor.resourceMonitor.getQueue());
	}

	public void beginSpline(iLineIdentifier identitfier) {
		((DynamicLine) delegate).beginSpline(identitfier);
	}

	public void curveTo(Vector3 to, Vector3 c1, Vector3 c2, int numSamples) {
		((DynamicLine) delegate).curveTo(to, c1, c2, numSamples);
	}

	public void endSpline() {
		((DynamicLine) delegate).endSpline();
	}

	public void lineTo(Vector3 v3) {
		((DynamicLine) delegate).lineTo(v3);

	}

	public void moveTo(Vector3 v3) {
		((DynamicLine) delegate).moveTo(v3);
	}

	public void setAuxOnSpline(int id, float a1) {
		((DynamicLine) delegate).setAuxOnSpline(id, a1);
	}

	public void setAuxOnSpline(int id, float a1, float a2) {
		((DynamicLine) delegate).setAuxOnSpline(id, a1, a2);
	}

	public void setAuxOnSpline(int id, float a1, float a2, float a3) {
		((DynamicLine) delegate).setAuxOnSpline(id, a1, a2, a3);
	}

	public void setAuxOnSpline(int id, float a1, float a2, float a3, float a4) {
		((DynamicLine) delegate).setAuxOnSpline(id, a1, a2, a3, a4);
	}

	public DynamicLine getLine()
	{
		return (DynamicLine)delegate;
	}
	
}
