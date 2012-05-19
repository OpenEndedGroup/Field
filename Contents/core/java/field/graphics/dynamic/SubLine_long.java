package field.graphics.dynamic;

import field.graphics.core.BasicUtilities;
import field.graphics.core.ResourceMonitor;
import field.graphics.core.BasicGeometry.LineList_long;
import field.math.abstraction.iInplaceProvider;
import field.math.linalg.Vector3;
import field.math.linalg.iCoordinateFrame;

public class SubLine_long extends SubMesh_long implements iLineOutput {

	public SubLine_long() {
		super(false);
		dummyMesh = new LineList_long((iInplaceProvider<iCoordinateFrame.iMutable>)new BasicUtilities.Position());
		dummyMesh .setNative(false);
		dummyMesh.rebuildTriangle(0);
		dummyMesh.rebuildVertex(0);
		delegate = new DynamicLine_long((LineList_long) dummyMesh);
	}
	
	public void fastDispose()
	{
		dummyMesh.deallocate(ResourceMonitor.resourceMonitor.getQueue());
	}

	public void beginSpline(iLineIdentifier identitfier) {
		((DynamicLine_long) delegate).beginSpline(identitfier);
	}

	public void curveTo(Vector3 to, Vector3 c1, Vector3 c2, int numSamples) {
		((DynamicLine_long) delegate).curveTo(to, c1, c2, numSamples);
	}

	public void endSpline() {
		((DynamicLine_long) delegate).endSpline();
	}

	public void lineTo(Vector3 v3) {
		((DynamicLine_long) delegate).lineTo(v3);

	}

	public void moveTo(Vector3 v3) {
		((DynamicLine_long) delegate).moveTo(v3);
	}

	public void setAuxOnSpline(int id, float a1) {
		((DynamicLine_long) delegate).setAuxOnSpline(id, a1);
	}

	public void setAuxOnSpline(int id, float a1, float a2) {
		((DynamicLine_long) delegate).setAuxOnSpline(id, a1, a2);
	}

	public void setAuxOnSpline(int id, float a1, float a2, float a3) {
		((DynamicLine_long) delegate).setAuxOnSpline(id, a1, a2, a3);
	}

	public void setAuxOnSpline(int id, float a1, float a2, float a3, float a4) {
		((DynamicLine_long) delegate).setAuxOnSpline(id, a1, a2, a3, a4);
	}

	public DynamicLine_long getLine()
	{
		return (DynamicLine_long)delegate;
	}
	
}
