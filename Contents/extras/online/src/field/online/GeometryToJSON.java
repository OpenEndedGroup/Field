package field.online;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.Map.Entry;

import field.graphics.core.BasicGeometry.LineList_long;
import field.graphics.core.BasicGeometry.TriangleMesh_long;
import field.graphics.core.BasicGeometry.VertexBuffer;

public class GeometryToJSON {

	public GeometryToJSON() {
	}

	public String convertMesh(TriangleMesh_long source) {
		StringBuffer b = new StringBuffer(source.numVertex() * 20);
		b.append("{\"vertex\":[");
		FloatBuffer v = source.vertex();
		{
			int n = source.numVertex();
			for (int i = 0; i < n; i++) {
				b.append(v.get() + ",");
				b.append(v.get() + ",");
				b.append(v.get() + ",");
			}
		}
		b.append("],\"triangle\":[");
		{
			int n = source.numTriangle();
			for (int i = 0; i < n; i++) {
				b.append(v.get() + ",");
				b.append(v.get() + ",");
				b.append(v.get() + ",");
			}
		}
		b.append("],");
		Iterator<Entry<Integer, VertexBuffer>> ii = source.auxBuffers.entrySet().iterator();
		while (ii.hasNext()) {
			Entry<Integer, VertexBuffer> ee = ii.next();
			b.append("\"aux" + ee.getKey() + "\":[");
			int n = source.numVertex();
			for (int i = 0; i < n; i++) {
				for (int q = 0; q < ee.getValue().elementSize; q++)
					b.append(v.get() + ",");
			}
			b.append("],");
		}
		b.append("}");
		return b.toString();
	}

	public String convertLine(LineList_long source) {
		StringBuffer b = new StringBuffer(source.numVertex() * 20);
		b.append("{\"vertex\":[");
		{
			FloatBuffer v = source.vertex();
			int n = source.numVertex();
			for (int i = 0; i < n; i++) {
				b.append(v.get() + ",");
				b.append(v.get() + ",");
				b.append(v.get() + ",");
			}
		}
		b.append("],\"triangle\":[");
		{
			int n = source.numTriangle();
			IntBuffer v = source.longTriangle();
			for (int i = 0; i < n; i++) {
				b.append(v.get() + ",");
				b.append(v.get() + ",");
			}
		}
		b.append("],");
		Iterator<Entry<Integer, VertexBuffer>> ii = source.auxBuffers.entrySet().iterator();
		while (ii.hasNext()) {
			Entry<Integer, VertexBuffer> ee = ii.next();
			b.append("\"aux" + ee.getKey() + "\":[");
			int n = source.numVertex();
			FloatBuffer v = source.aux(ee.getKey().intValue(),0);
			for (int i = 0; i < n; i++) {
				for (int q = 0; q < ee.getValue().elementSize; q++)
					b.append(v.get() + ",");
			}
			b.append("],");
		}
		b.append("}");
		return b.toString();
	}

}
