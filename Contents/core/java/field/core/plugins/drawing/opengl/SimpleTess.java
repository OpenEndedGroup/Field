package field.core.plugins.drawing.opengl;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.GLUtessellatorCallbackAdapter;
import org.lwjgl.util.glu.tessellation.GLUtessellatorImpl;

import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;

public abstract class SimpleTess {

	GLUtessellatorImpl tess = new GLUtessellatorImpl();

	public class VInfo {
		int vertex = -1;

		Vector3 position = new Vector3();

		List<Object> properties;

		public VInfo(int vertex, Vector3 position, List<Object> properties) {
			super();
			this.vertex = vertex;
			this.position = position;
			this.properties = properties;
		}

		@Override
		public String toString() {
			return vertex + "@ " + position;
		}
	}

	public SimpleTess() {

		tess.gluTessCallback(GLU.GLU_TESS_VERTEX_DATA, new GLUtessellatorCallbackAdapter() {
			@Override
			public void vertexData(Object arg0, Object arg1) {
				VInfo i = (VInfo) arg0;

				nextTriangle(i);
			}
		});

		tess.gluTessCallback(GLU.GLU_TESS_COMBINE_DATA, new GLUtessellatorCallbackAdapter() {

			@Override
			public void combineData(double[] coords, Object[] data, float[] weight, Object[] outdata, Object arg4) {

				// todo, interpolate properties correctly
				outdata[0] = new VInfo(-1, new Vector3(coords[0], coords[1], coords[2]), interpolateProperites(data, weight));
			}
		});
		tess.gluTessCallback(GLU.GLU_EDGE_FLAG, new GLUtessellatorCallbackAdapter() {
		});

		// GLU.gluTessProperty(tess, GLU.GLU_TESS_WINDING_RULE,
		// GLU.GLU_TESS_WINDING_NONZERO);
		tess.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ODD);

	}

	public void begin() {
		tess.gluTessBeginPolygon(null);
	}

	boolean contourFirst = false;

	public void beginContour() {
		tess.gluTessBeginContour();
		contourFirst = true;
	}

	public void end() {

		tess.gluTessEndPolygon();
	}

	public void endContour() {
		tess.gluTessEndContour();
	}

	public void line(Vector3 a, Vector3 b, List<Object> pa, List<Object> pb) {

		System.out.println(" line <" + a + " -> " + b + "> <" + contourFirst + ">");

		if (contourFirst) {
			VInfo info = new VInfo(-1, a, pa);
			nextVertex(info);
			tess.gluTessVertex(new double[] { a.x, a.y, a.z }, 0, info);
		}

		VInfo info = new VInfo(-1, b, pb);
		nextVertex(info);
		tess.gluTessVertex(new double[] { b.x, b.y, b.z }, 0, info);

		contourFirst = false;
	}

	protected List<Object> interpolateProperites(Object[] data, float[] weight) {
		List<Object> o = new ArrayList<Object>();

		int l = (((VInfo) data[0]).properties).size();
		for (int i = 0; i < l; i++) {
			Object ty = (((VInfo) data[0]).properties).get(i);

			Object zz = null;

			try {
				if (ty instanceof Vector4) {
					Vector4 z = new Vector4();
					float m = 0;
					for (int q = 0; q < data.length; q++) {
						if (((VInfo) data[q]) == null) {
						} else {
							z.x += (((Vector4) (((VInfo) data[q]).properties).get(i))).x * weight[q];
							z.y += (((Vector4) (((VInfo) data[q]).properties).get(i))).y * weight[q];
							z.z += (((Vector4) (((VInfo) data[q]).properties).get(i))).z * weight[q];
							z.w += (((Vector4) (((VInfo) data[q]).properties).get(i))).w * weight[q];
							m += weight[q];
						}
					}
					z.x /= m;
					z.y /= m;
					z.z /= m;
					z.w /= m;
					zz = z;
				} else if (ty instanceof Vector3) {
					Vector3 z = new Vector3();
					float m = 0;
					for (int q = 0; q < data.length; q++) {
						if (((VInfo) data[q]) == null) {
						} else {
							z.x += (((Vector3) (((VInfo) data[q]).properties).get(i))).x * weight[q];
							z.y += (((Vector3) (((VInfo) data[q]).properties).get(i))).y * weight[q];
							z.z += (((Vector3) (((VInfo) data[q]).properties).get(i))).z * weight[q];
							m += weight[q];
						}
					}
					z.x /= m;
					z.y /= m;
					z.z /= m;
					zz = z;
				} else if (ty instanceof Vector2) {
					Vector2 z = new Vector2();
					float m = 0;
					for (int q = 0; q < data.length; q++) {
						if (((VInfo) data[q]) == null) {
						} else {
							z.x += (((Vector2) (((VInfo) data[q]).properties).get(i))).x * weight[q];
							z.y += (((Vector2) (((VInfo) data[q]).properties).get(i))).y * weight[q];
							m += weight[q];
						}
					}
					z.x /= m;
					z.y /= m;
					zz = z;
				} else if (ty instanceof Number) {
					float z = 0f;
					float m = 0;
					for (int q = 0; q < data.length; q++) {
						if (((VInfo) data[q]) == null) {
						} else {
							z += (((Number) (((VInfo) data[q]).properties).get(i))).floatValue() * weight[q];
							m += weight[q];
						}
					}
					z /= m;
					zz = z;
				}
			} finally {
				o.add(zz);
			}
		}

		return o;
	}

	List<VInfo> ongoingTriangle = new ArrayList<VInfo>();

	protected void nextTriangle(VInfo i) {

//		System.out.println(" next triangle <" + i.vertex + ">");

		if (i.vertex == -1) {
//			System.out.println(" needs a new vertex <" + i.position + ">");
			nextVertex(i);
		}

		if (ongoingTriangle.size() == 2) {
			nextTriangle(ongoingTriangle.get(0), ongoingTriangle.get(1), i);
			ongoingTriangle.clear();
		} else
			ongoingTriangle.add(i);
	}

	protected void nextTriangle(VInfo a, VInfo b, VInfo c) {
		nextFace(a.vertex, b.vertex, c.vertex);
	}

	protected abstract void nextFace(int a, int b, int c);

	protected void nextVertex(VInfo info) {
		if (info.vertex == -1) {
			info.vertex = nextVertex(info.position);
			decorateVertex(info.vertex, info.properties);
		}
	}

	protected abstract void decorateVertex(int vertex, List<Object> properties);

	abstract protected int nextVertex(Vector3 position);

}
