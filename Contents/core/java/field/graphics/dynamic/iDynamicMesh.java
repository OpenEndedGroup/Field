package field.graphics.dynamic;

import field.bytecode.protect.iInside;
import field.graphics.core.Base.iGeometry;
import field.math.linalg.Vector3;

/**
 * @author marc
 * Created on Nov 2, 2003
 */
public interface iDynamicMesh extends iInside, iRemoveable
{
	static public final int hint_kill= -1;

	static public final int hint_plane= 0;
	static public final int hint_line = 1;
	static public final int hint_point = 2;

	
	public void hintPrimitiveType(int primType);
	
	
	public abstract void open();
	public abstract int nextFace(Vector3 v1, Vector3 v2, Vector3 v3);
	public abstract int nextFace(int v1, int v2, int v3);
	public abstract int nextVertex(Vector3 v1);
	public abstract void setAux(int vertex, int id, float a);
	public abstract void setAux(int vertex, int id, float a, float b);
	public abstract void setAux(int vertex, int id, float a, float b, float c);
	public abstract void setAux(int vertex, int id, float a, float b, float c, float d);
	public abstract void close();


	public iGeometry getUnderlyingGeometry();
}