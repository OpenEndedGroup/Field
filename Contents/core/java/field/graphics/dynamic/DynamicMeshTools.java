package field.graphics.dynamic;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import field.core.dispatch.iVisualElement.Rect;
import field.graphics.core.Base;
import field.graphics.core.BasicUtilities;
import field.graphics.core.Base.StandardPass;
import field.graphics.core.Base.iGeometry;
import field.math.abstraction.iFilter;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.math.util.CubicTools;

/**
 * Created on Nov 11, 2003
 *
 * @author marc
 */
public class DynamicMeshTools {

	// an aux copier that can cope with changing amounts of vertex and triangle data

	static public class AuxCopier {

		Base.iGeometry target;

		int numLastV = 0;

		int numLastT = 0;

		int id;

		public AuxCopier(Base.iGeometry target, int id) {
			this.id = id;
			this.target = target;
			target.addChild(onPre());
			target.addChild(onPost());
		}

		public Base.iSceneListElement onPost() {
			return new BasicUtilities.OnePassElement(StandardPass.postRender){

				@Override
				public void performPass() {
					target.aux(id, 3).put(target.vertex());
					numLastV = target.numVertex();
					numLastT = target.numTriangle();
				}
			};
		}

		public Base.iSceneListElement onPre() {
			return new BasicUtilities.OnePassElement(StandardPass.preRender){

				@Override
				public void performPass() {
					if (numLastV != target.numVertex() || numLastT != target.numTriangle()) {
						target.aux(id, 3).put(target.vertex());
					}
				}
			};
		}
	}

	static public class ShapedNoiseBuffer {

		FloatBuffer[] buffers;

		iGeometry target;

		int id;

		int at;

		int capacity;

		public ShapedNoiseBuffer(Base.iGeometry target, int id, int num, int initialSize) {
			this.id = id;
			this.target = target;
			at = 0;
			buffers = new FloatBuffer[num];
			for (int i = 0; i < num; i++) {
				buffers[i] = ByteBuffer.allocateDirect(4 * initialSize).order(ByteOrder.nativeOrder()).asFloatBuffer();
				fill(buffers[i]);
				buffers[i].limit(0);
			}
			capacity = initialSize;
		}

		public Base.iSceneListElement onPre() {
			return new BasicUtilities.OnePassElement(StandardPass.preRender){

				@Override
				public void performPass() {
					if (buffers[at].limit() != target.numVertex() * 3) {
						changeSize(target.numVertex());
					}
					target.aux(id, 3).put(buffers[at]);
					buffers[at].rewind();
					at = (at + 1) % buffers.length;
				}

			};
		}

		// no need for fast random here...?
		private void fill(FloatBuffer buffer) {
			buffer.rewind();
			for (int i = 0; i < buffer.capacity(); i++) {
				buffer.put((float) (Math.random() - 0.5));
			}
		}

		protected void changeSize(int i) {
			if (i <= capacity) {
				for (int n = 0; n < buffers.length; n++) {
					buffers[n].limit(i);
				}
			} else {
				int s = capacity;
				while (s < i)
					s *= 2;
				for (int n = 0; n < buffers.length; n++) {
					buffers[n] = ByteBuffer.allocateDirect(4 * s * 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
					fill(buffers[n]);
					buffers[n].limit(i);
					buffers[n].rewind();
				}
			}
		}
	}

	static protected Vector3 t1 = new Vector3();

	static protected Vector3 t2 = new Vector3();

	static protected Vector3 t3 = new Vector3();

	static protected Vector3 t4 = new Vector3();

	static float noise = 0.4f;

	static public void drawRightXYLine(Vector3 from, Vector3 to, float r, float g, float b, float a, DynamicLine line, String splineName, int samples) {
		drawRightXYLine(from, to, r, g, b, a, line, splineName, samples, 0, 1);
	}

	static public void drawRightXYLine(Vector3 from, Vector3 to, float r, float g, float b, float a, DynamicLine line, String splineName, int samples, float noise, float fractionDrawn) {
		DynamicMeshTools.noise = noise;

		t1.set((from.y - to.y + from.x), -(from.x - to.x) + from.y, (from.z + to.z) / 2);
		t2.set((from.y - to.y + to.x), -(from.x - to.x) + to.y, (from.z + to.z) / 2);

		noise(t1);
		noise(t2);

		noise(from);
		noise(to);

		line.beginSpline(new LineIdentifier(splineName));
		line.setAuxOnSpline(Base.color0_id, r, g, b, a);
		for (int i = 0; i < Math.max(samples * fractionDrawn, 2); i++) {
			float al = i / (float) (samples - 1);
			CubicTools.cubic(al, t1, from, to, t2, t3);
			if (i == 0)
				line.moveTo(t3);
			else
				line.lineTo(t3);
			line.setAuxOnSpline(Base.color0_id, r, g, b, a);
		}
		line.endSpline();
	}

	static public void drawSquareXY(Rect rect, float fr, float fg, float fb, float fa, float or, float og, float ob, float oa, iDynamicMesh mesh, DynamicLine line) {
		drawSquareXY(new Vector3(rect.x, rect.y, 0), new Vector3(rect.x + rect.w, rect.y + rect.h, 0), fr, fg, fb, fa, or, og, ob, oa, mesh, line, 0);
	}

	public static void drawSquareXY(Rect tile, iFilter<Vector3, Vector3> warp, float fr, float fg, float fb, float fa, float or, float og, float ob, float oa, iDynamicMesh mesh, DynamicLine line) {

		noise = 0;

		if (mesh != null) {
			int v1 = mesh.nextVertex(noise(warp == null ? tile.topLeft() : warp.filter(tile.topLeft())));
			int v2 = mesh.nextVertex(noise(warp == null ? tile.bottomLeft() : warp.filter(tile.bottomLeft())));
			int v3 = mesh.nextVertex(noise(warp == null ? tile.bottomRight() : warp.filter(tile.bottomRight())));
			int v4 = mesh.nextVertex(noise(warp == null ? tile.topRight() : warp.filter(tile.topRight())));

			mesh.nextFace(v1, v2, v3);
			mesh.nextFace(v1, v3, v4);

			mesh.setAux(v1, Base.color0_id, fr, fg, fb, fa);
			mesh.setAux(v2, Base.color0_id, fr, fg, fb, fa);
			mesh.setAux(v3, Base.color0_id, fr, fg, fb, fa);
			mesh.setAux(v4, Base.color0_id, fr, fg, fb, fa);
		}
		if (line != null) {
			int v1 = line.nextVertex(noise(warp == null ? tile.topLeft() : warp.filter(tile.topLeft())));
			int v2 = line.nextVertex(noise(warp == null ? tile.bottomLeft() : warp.filter(tile.bottomLeft())));
			int v3 = line.nextVertex(noise(warp == null ? tile.bottomRight() : warp.filter(tile.bottomRight())));
			int v4 = line.nextVertex(noise(warp == null ? tile.topRight() : warp.filter(tile.topRight())));

			line.nextFace(v1, v2);
			line.nextFace(v2, v3);
			line.nextFace(v3, v4);
			line.nextFace(v4, v1);

			line.setAux(v1, Base.color0_id, or, og, ob, oa);
			line.setAux(v2, Base.color0_id, or, og, ob, oa);
			line.setAux(v3, Base.color0_id, or, og, ob, oa);
			line.setAux(v4, Base.color0_id, or, og, ob, oa);
		}
	}

	public static void drawSquareXY(Rect tile, iFilter<Vector3, Vector3> warp, float fr, float fg, float fb, float fa, float or, float og, float ob, float oa, iDynamicMesh mesh, iLineOutput line) {
		noise = 0;

		if (mesh != null) {
			mesh.open();
			int v1 = mesh.nextVertex(noise(warp == null ? tile.topLeft() : warp.filter(tile.topLeft())));
			int v2 = mesh.nextVertex(noise(warp == null ? tile.bottomLeft() : warp.filter(tile.bottomLeft())));
			int v3 = mesh.nextVertex(noise(warp == null ? tile.bottomRight() : warp.filter(tile.bottomRight())));
			int v4 = mesh.nextVertex(noise(warp == null ? tile.topRight() : warp.filter(tile.topRight())));

			mesh.nextFace(v1, v2, v3);
			mesh.nextFace(v1, v3, v4);

			mesh.setAux(v1, Base.color0_id, fr, fg, fb, fa);
			mesh.setAux(v2, Base.color0_id, fr, fg, fb, fa);
			mesh.setAux(v3, Base.color0_id, fr, fg, fb, fa);
			mesh.setAux(v4, Base.color0_id, fr, fg, fb, fa);
			mesh.close();
		}
		if (line != null) {
			line.open();
			line.beginSpline(new LineIdentifier(""));
			line.setAuxOnSpline(Base.color0_id, or, og, ob, oa);

			line.moveTo(noise(warp == null ? tile.topLeft() : warp.filter(tile.topLeft())));
			line.lineTo(noise(warp == null ? tile.bottomLeft() : warp.filter(tile.bottomLeft())));
			line.lineTo(noise(warp == null ? tile.bottomRight() : warp.filter(tile.bottomRight())));
			line.lineTo(noise(warp == null ? tile.topRight() : warp.filter(tile.topRight())));
			line.lineTo(noise(warp == null ? tile.topLeft() : warp.filter(tile.topLeft())));
			line.setAuxOnSpline(Base.color0_id, or, og, ob, oa);

			line.endSpline();
			line.close();
		}
	}

	public static void drawSquareXY(Rect tile, iFilter<Vector3, Vector3> warp, float fr, float fg, float fb, float fa, float or, float og, float ob, float oa, iDynamicMesh mesh, iLineOutput line, Vector4 noise) {
		DynamicMeshTools.noise = 0;

		if (mesh != null) {
			mesh.open();
			int v1 = mesh.nextVertex(noise(warp == null ? tile.topLeft() : warp.filter(tile.topLeft())));
			int v2 = mesh.nextVertex(noise(warp == null ? tile.bottomLeft() : warp.filter(tile.bottomLeft())));
			int v3 = mesh.nextVertex(noise(warp == null ? tile.bottomRight() : warp.filter(tile.bottomRight())));
			int v4 = mesh.nextVertex(noise(warp == null ? tile.topRight() : warp.filter(tile.topRight())));

			mesh.nextFace(v1, v2, v3);
			mesh.nextFace(v1, v3, v4);

			mesh.setAux(v1, Base.color0_id, fr, fg, fb, fa);
			mesh.setAux(v2, Base.color0_id, fr, fg, fb, fa);
			mesh.setAux(v3, Base.color0_id, fr, fg, fb, fa);
			mesh.setAux(v4, Base.color0_id, fr, fg, fb, fa);
			mesh.close();
		}
		if (line != null) {
			line.open();
			line.beginSpline(new LineIdentifier(""));
			line.setAuxOnSpline(Base.color0_id, or, og, ob, oa);
			line.setAuxOnSpline(11, noise.x, noise.y, noise.z, noise.w);

			line.moveTo(noise(warp == null ? tile.topLeft() : warp.filter(tile.topLeft())));
			line.lineTo(noise(warp == null ? tile.bottomLeft() : warp.filter(tile.bottomLeft())));
			line.lineTo(noise(warp == null ? tile.bottomRight() : warp.filter(tile.bottomRight())));
			line.lineTo(noise(warp == null ? tile.topRight() : warp.filter(tile.topRight())));
			line.lineTo(noise(warp == null ? tile.topLeft() : warp.filter(tile.topLeft())));
			line.setAuxOnSpline(11, noise.x, noise.y, noise.z, noise.w);
			line.setAuxOnSpline(Base.color0_id, or, og, ob, oa);

			line.endSpline();
			line.close();
		}
	}

	static public void drawSquareXY(Vector3 p1, Vector3 p2, float fr, float fg, float fb, float fa, float or, float og, float ob, float oa, iDynamicMesh mesh, DynamicLine line) {
		drawSquareXY(p1, p2, fr, fg, fb, fa, or, og, ob, oa, mesh, line, 0);
	}

	static public void drawSquareXY(Vector3 p1, Vector3 p2, float fr, float fg, float fb, float fa, float or, float og, float ob, float oa, iDynamicMesh mesh, DynamicLine line, float noise) {
		DynamicMeshTools.noise = noise;

		if (mesh != null) {
			int v1 = mesh.nextVertex(noise(p1));
			t1.set(p1.x, p2.y, (p1.z + p2.z) / 2);
			int v2 = mesh.nextVertex(noise(t1));
			int v3 = mesh.nextVertex(noise(p2));
			t1.set(p2.x, p1.y, (p1.z + p2.z) / 2);
			int v4 = mesh.nextVertex(noise(t1));

			mesh.nextFace(v1, v2, v3);
			mesh.nextFace(v1, v3, v4);

			mesh.setAux(v1, Base.color0_id, fr, fg, fb, fa);
			mesh.setAux(v2, Base.color0_id, fr, fg, fb, fa);
			mesh.setAux(v3, Base.color0_id, fr, fg, fb, fa);
			mesh.setAux(v4, Base.color0_id, fr, fg, fb, fa);
		}
		if (line != null) {
			int v1 = line.nextVertex(noise(p1));
			t1.set(p1.x, p2.y, (p1.z + p2.z) / 2);
			int v2 = line.nextVertex(noise(t1));
			int v3 = line.nextVertex(noise(p2));
			t1.set(p2.x, p1.y, (p1.z + p2.z) / 2);
			int v4 = line.nextVertex(noise(t1));

			line.nextFace(v1, v2);
			line.nextFace(v2, v3);
			line.nextFace(v3, v4);
			line.nextFace(v4, v1);

			line.setAux(v1, Base.color0_id, or, og, ob, oa);
			line.setAux(v2, Base.color0_id, or, og, ob, oa);
			line.setAux(v3, Base.color0_id, or, og, ob, oa);
			line.setAux(v4, Base.color0_id, or, og, ob, oa);
		}
	}

	static public void setAll(iDynamicMesh mesh, int fromVertex, int numVertex, int attribute, Vector4 to) {
		float[] zz = { to.x, to.y, to.z, to.w};
		FloatBuffer buffer = mesh.getUnderlyingGeometry().aux(attribute, 4);
		buffer.position(4 * fromVertex);
		for (int z = fromVertex; z < (fromVertex + numVertex); z++) {
			buffer.put(zz);
		}
	}

	static public void setAll(iDynamicMesh mesh, int attribute, Vector4 to) {
		setAll(mesh.getUnderlyingGeometry(), attribute, to);
	}

	static public void setAll(iGeometry geometry, int attribute, Vector4 to) {
		float[] zz = { to.x, to.y, to.z, to.w};
		FloatBuffer buffer = geometry.aux(attribute, 4);
		for (int z = 0; z < buffer.limit() / 4; z++) {
			buffer.put(zz);
		}
	}

	private static Vector3 noise(Vector3 t) {
		t.set((float) (t.x + (Math.random() - 0.5) * noise), (float) (t.y + (Math.random() - 0.5) * noise), (float) (t.z + (Math.random() - 0.5) * noise));
		return t;
	}

}