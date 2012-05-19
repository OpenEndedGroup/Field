package field.core.plugins.drawing.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfNumber;
import com.lowagie.text.pdf.PdfShading;
import com.lowagie.text.pdf.PdfStream;
import com.lowagie.text.pdf.PdfWriter;

import field.core.plugins.drawing.opengl.CachedLine;
import field.graphics.core.Base;
import field.graphics.core.Base.iGeometry;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.graphics.dynamic.CompleteEdgeRep2;
import field.graphics.dynamic.CompleteEdgeRep2.Edge;
import field.graphics.dynamic.CompleteEdgeRep2.Face;
import field.graphics.dynamic.CompleteEdgeRep2.Vertex;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;

public class MeshToPdfType4 {

	private CompleteEdgeRep2 rep;

	private final TriangleMesh mesh;

	public MeshToPdfType4(TriangleMesh mesh) {
		this.mesh = mesh;
		rep = new CompleteEdgeRep2().add(mesh);
	}

	public class EdgeRecord {
		int originalFace;

		int addedVertex;

		int additionalV1;

		int additionalV2;

		EdgeRecord next;

		Edge nextSharedEdge;

		EdgeRecord previous;

		boolean dead = false;

	}

	public List<EdgeRecord> floatStream() {
		List<EdgeRecord> out = new ArrayList<EdgeRecord>();

		for (int i = 0; i < rep.allFace.size(); i++) {
			EdgeRecord r = new EdgeRecord();
			r.originalFace = i;

			out.add(r);
		}

		boolean touched = false;
		do {
			touched = false;

			for (int i = 0; i < rep.allFace.size(); i++) {
				EdgeRecord r = out.get(i);
				if (r.next == null && !r.dead) {
					// try to connect it to something
					Face of = rep.allFace.get(r.originalFace);
					HashSet<CompleteEdgeRep2.Face> potentialFaces = new LinkedHashSet<CompleteEdgeRep2.Face>();
					potentialFaces.addAll(of.edges[0].faces);
					potentialFaces.addAll(of.edges[1].faces);
					potentialFaces.addAll(of.edges[2].faces);
					potentialFaces.remove(of);

					// are any of these free

					boolean found = false;
					int foundAt = 0;
					for (CompleteEdgeRep2.Face f : potentialFaces) {
						EdgeRecord rec = out.get(f.faceNumber);
						if (rec.previous == null) {

							rec.previous = r;
							r.next = rec;

							Face otherFace = rep.allFace.get(f.faceNumber);

							// these two things share and edge
							for (int j = 0; j < 3; j++) {
								int index = CompleteEdgeRep2.indexOf(otherFace.edges[j], of.edges);
								if (index != -1) {
									Edge sharedEdge = otherFace.edges[j];
									r.nextSharedEdge = sharedEdge;

									if (sharedEdge == rec.nextSharedEdge) {
										r.nextSharedEdge = null;
									} else
										break;
								}
							}

							if (r.nextSharedEdge == null) {
								r.next = null;
								rec.previous = null;
								continue;
							}

							foundAt = f.faceNumber;
							found = true;
							break;
						}
					}
					if (found) {
						touched = true;
						i = foundAt;
					}
				}
			}
		} while (touched);
		return out;
	}

	public byte[] serializeFloatStream(List<EdgeRecord> r, float xmin, float xmax, float ymin, float ymax) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		HashSet<EdgeRecord> all = new HashSet<EdgeRecord>(r);
		while (all.size() > 0) {
			EdgeRecord start = all.iterator().next();

			chase(all, start, bos, xmin, xmax, ymin, ymax, 0, -1, -1, -1);
		}

		return bos.toByteArray();
	}

	private void chase(HashSet<EdgeRecord> all, EdgeRecord start, ByteArrayOutputStream bos, float xmin, float xmax, float ymin, float ymax, int code, int va2, int vb2, int vc2) {
		if (!all.contains(start)) return;
		

		if ((va2==vb2 || va2==vc2 || vc2==vb2) && (va2!=-1 && vb2!=-1 && vc2!=-1)) return;
		
		if (code == 0) {
			all.remove(start);
			if (start.nextSharedEdge != null) {

				// we can't have edge AB, only edge AC or BC

				int va = rep.allFace.get(start.originalFace).vertex[0].vertexNumber;
				int vb = rep.allFace.get(start.originalFace).vertex[1].vertexNumber;
				int vc = rep.allFace.get(start.originalFace).vertex[2].vertexNumber;

				if ((start.nextSharedEdge.start.vertexNumber == va || start.nextSharedEdge.start.vertexNumber == vb) && (start.nextSharedEdge.end.vertexNumber == va || start.nextSharedEdge.end.vertexNumber == vb)) {
					// we guessed wrong
					int o = vc;
					vc = vb;
					vb = o;
				}

				
				bos.write(0);
				bos.write(toByte(xmin, xmax, mesh.vertex().get(3 * va + 0)));
				bos.write(toByte(ymin, ymax, mesh.vertex().get(3 * va + 1)));
				Vector3 c = getColorFor(mesh.aux(Base.color0_id, 4), va);
				bos.write(toByte(0, 1f, c.x));
				bos.write(toByte(0, 1f, c.y));
				bos.write(toByte(0, 1f, c.z));
				bos.write(0);
				bos.write(toByte(xmin, xmax, mesh.vertex().get(3 * vb + 0)));
				bos.write(toByte(ymin, ymax, mesh.vertex().get(3 * vb + 1)));
				c = getColorFor(mesh.aux(Base.color0_id, 4), vb);
				bos.write(toByte(0, 1f, c.x));
				bos.write(toByte(0, 1f, c.y));
				bos.write(toByte(0, 1f, c.z));
				bos.write(0);
				bos.write(toByte(xmin, xmax, mesh.vertex().get(3 * vc + 0)));
				bos.write(toByte(ymin, ymax, mesh.vertex().get(3 * vc + 1)));
				c = getColorFor(mesh.aux(Base.color0_id, 4), vc);
				bos.write(toByte(0, 1f, c.x));
				bos.write(toByte(0, 1f, c.y));
				bos.write(toByte(0, 1f, c.z));

				if ((start.nextSharedEdge.start.vertexNumber == va || start.nextSharedEdge.start.vertexNumber == vc) && (start.nextSharedEdge.end.vertexNumber == va || start.nextSharedEdge.end.vertexNumber == vc)) {
					chase(all, start.next, bos, xmin, xmax, ymin, ymax, 2, va, vb, vc);
				} else {
					assert ((start.nextSharedEdge.start.vertexNumber == vb || start.nextSharedEdge.start.vertexNumber == vc) && (start.nextSharedEdge.end.vertexNumber == vb || start.nextSharedEdge.end.vertexNumber == vc));
					chase(all, start.next, bos, xmin, xmax, ymin, ymax, 1, va, vb, vc);
				}

			} else {
				int va = rep.allFace.get(start.originalFace).vertex[0].vertexNumber;
				int vb = rep.allFace.get(start.originalFace).vertex[1].vertexNumber;
				int vc = rep.allFace.get(start.originalFace).vertex[2].vertexNumber;

				bos.write(0);
				bos.write(toByte(xmin, xmax, mesh.vertex().get(3 * va + 0)));
				bos.write(toByte(ymin, ymax, mesh.vertex().get(3 * va + 1)));
				Vector3 c = getColorFor(mesh.aux(Base.color0_id, 4), va);
				bos.write(toByte(0, 1f, c.x));
				bos.write(toByte(0, 1f, c.y));
				bos.write(toByte(0, 1f, c.z));
				bos.write(0);
				bos.write(toByte(xmin, xmax, mesh.vertex().get(3 * vb + 0)));
				bos.write(toByte(ymin, ymax, mesh.vertex().get(3 * vb + 1)));
				c = getColorFor(mesh.aux(Base.color0_id, 4), vb);
				bos.write(toByte(0, 1f, c.x));
				bos.write(toByte(0, 1f, c.y));
				bos.write(toByte(0, 1f, c.z));
				bos.write(0);
				bos.write(toByte(xmin, xmax, mesh.vertex().get(3 * vc + 0)));
				bos.write(toByte(ymin, ymax, mesh.vertex().get(3 * vc + 1)));
				c = getColorFor(mesh.aux(Base.color0_id, 4), vc);
				bos.write(toByte(0, 1f, c.x));
				bos.write(toByte(0, 1f, c.y));
				bos.write(toByte(0, 1f, c.z));
			}
		} else if (code == 1) {
			all.remove(start);
			int va = rep.allFace.get(start.originalFace).vertex[0].vertexNumber;
			int vb = rep.allFace.get(start.originalFace).vertex[1].vertexNumber;
			int vc = rep.allFace.get(start.originalFace).vertex[2].vertexNumber;

			// which one of these is new?
			int newVertex = -1;
			if (va != va2 && va != vb2 && va != vc2) newVertex = va;
			if (vb != va2 && vb != vb2 && vb != vc2) newVertex = vb;
			if (vc != va2 && vc != vb2 && vc != vc2) newVertex = vc;

			assert newVertex != -1 : va+" "+vb+" "+vc+" "+va2+" "+vb2+" "+vc2;

			bos.write(code);
			bos.write(toByte(xmin, xmax, mesh.vertex().get(3 * newVertex + 0)));
			bos.write(toByte(ymin, ymax, mesh.vertex().get(3 * newVertex + 1)));
			Vector3 c = getColorFor(mesh.aux(Base.color0_id, 4), newVertex);
			bos.write(toByte(0, 1f, c.x));
			bos.write(toByte(0, 1f, c.y));
			bos.write(toByte(0, 1f, c.z));

			// shared edge should with be Bn (2) or Cn (1)

			
//			if (newVertex == vb || newVertex == vc)
//			{
//				assert false : newVertex+" "+vb+" "+vc+" "+va;
//				return;
//			}
			
			
			if (all.contains(start.next)) 
			{
				if (start.nextSharedEdge != null) {
					if ((start.nextSharedEdge.start.vertexNumber == vb2 || start.nextSharedEdge.start.vertexNumber == newVertex) && (start.nextSharedEdge.end.vertexNumber == vb2 || start.nextSharedEdge.end.vertexNumber == newVertex)) {
						chase(all, start.next, bos, xmin, xmax, ymin, ymax, 2, newVertex, vb, vc);
					} else {
						assert ((start.nextSharedEdge.start.vertexNumber == vc2 || start.nextSharedEdge.start.vertexNumber == newVertex) && (start.nextSharedEdge.end.vertexNumber == vc2 || start.nextSharedEdge.end.vertexNumber == newVertex));
						chase(all, start.next, bos, xmin, xmax, ymin, ymax, 1, vb, newVertex, vc);
					}
				}
			}
		} else if (code == 2) {
			all.remove(start);
			int va = rep.allFace.get(start.originalFace).vertex[0].vertexNumber;
			int vb = rep.allFace.get(start.originalFace).vertex[1].vertexNumber;
			int vc = rep.allFace.get(start.originalFace).vertex[2].vertexNumber;

			// which one of these is new?
			int newVertex = -1;
			if (va != va2 && va != vb2 && va != vc2) newVertex = va;
			if (vb != va2 && vb != vb2 && vb != vc2) newVertex = vb;
			if (vc != va2 && vc != vb2 && vc != vc2) newVertex = vc;

			assert newVertex != -1;

			bos.write(code);
			bos.write(toByte(xmin, xmax, mesh.vertex().get(3 * newVertex + 0)));
			bos.write(toByte(ymin, ymax, mesh.vertex().get(3 * newVertex + 1)));
			Vector3 c = getColorFor(mesh.aux(Base.color0_id, 4), newVertex);
			bos.write(toByte(0, 1f, c.x));
			bos.write(toByte(0, 1f, c.y));
			bos.write(toByte(0, 1f, c.z));

			// shared edge should with be An (2) or Cn (1)

			
			if (newVertex == va || newVertex == vc)
			{
				assert false;
				return;
			}

			if (all.contains(start.next)) 
			{
				if (start.nextSharedEdge != null) {
					if ((start.nextSharedEdge.start.vertexNumber == vc2 || start.nextSharedEdge.start.vertexNumber == newVertex) && (start.nextSharedEdge.end.vertexNumber == vc2 || start.nextSharedEdge.end.vertexNumber == newVertex)) {
						chase(all, start.next, bos, xmin, xmax, ymin, ymax, 1, newVertex, va, vc);
					} else {
						assert ((start.nextSharedEdge.start.vertexNumber == va2 || start.nextSharedEdge.start.vertexNumber == newVertex) && (start.nextSharedEdge.end.vertexNumber == va2 || start.nextSharedEdge.end.vertexNumber == newVertex)) : start.nextSharedEdge + " " + va2 + " " + newVertex + "      "+ vb2 + " " + vc2;
						chase(all, start.next, bos, xmin, xmax, ymin, ymax, 2, va, vc, newVertex);
					}
				}
			}
		}
	}

	private Vector3 getColorFor(FloatBuffer buffer, int va) {
		Vector3 q = new Vector3(buffer.get(4 * va + 0), buffer.get(4 * va + 1), buffer.get(4 * va + 2));
		return q;
	}

	protected int toByte(float xmin, float xmax, float f) {

		float x = 255 * (f - xmin) / (xmax - xmin);
		if (x < 0) x = 0;
		if (x > 255) x = 255;
		return (int) x;
	}

	static public class PdfShading_type4 extends PdfShading {
		public PdfShading_type4(PdfWriter writer, TriangleMesh mesh) {
			super(writer);

			float minx = Float.POSITIVE_INFINITY;
			float miny = Float.POSITIVE_INFINITY;
			float maxx = Float.NEGATIVE_INFINITY;
			float maxy = Float.NEGATIVE_INFINITY;

			FloatBuffer f = mesh.vertex();
			for (int i = 0; i < mesh.numVertex(); i++) {
				float x = f.get();
				float y = f.get();
				f.get();

				if (x > maxx) maxx = x;
				if (x < minx) minx = x;
				if (y > maxy) maxy = y;
				if (y < miny) miny = y;

			}

			MeshToPdfType4 toMesh = new MeshToPdfType4(mesh);
			byte[] serialized = toMesh.serializeFloatStream(toMesh.floatStream(), minx, maxx, miny, maxy);

			this.shading = new PdfStream(serialized);
			this.shadingType = 4;
			this.shading.put(PdfName.SHADINGTYPE, new PdfNumber(this.shadingType));
			this.setColorSpace(new Color(0, 0, 0));
			this.shading.put(new PdfName("BitsPerCoordinate"), new PdfNumber(8));
			this.shading.put(new PdfName("BitsPerComponent"), new PdfNumber(8));
			this.shading.put(new PdfName("BitsPerFlag"), new PdfNumber(8));
			this.shading.put(PdfName.DECODE, new PdfArray(new float[] { minx, maxx, miny, maxy, 0, 1, 0, 1, 0, 1}));
		}
	}

	public static void dilateMesh(iGeometry underlyingGeometry, CachedLine line) {

		// this is going to be unplesant

		CompleteEdgeRep2 rep = new CompleteEdgeRep2(underlyingGeometry);

		HashMap<Vertex, Vector2> newPositions = new HashMap<Vertex, Vector2>();
		for (Vertex v : rep.allVertex) {
			// move this vertex away from its neighbourd
			
			// we should preferentially weight the edges that have more than one face attached (since they are interior?)

			Vector2 p = positionFor(v, underlyingGeometry);
			Vector2 d = new Vector2();
			float n = 0;
			for (Edge e : v.edges) {
				Vector2 p2 = positionFor(e.start != v ? e.start : e.end, underlyingGeometry);
				d.add(p2).sub(p);
				n++;
				if (e.faces.size()>1)
				{
					d.add(p2).sub(p);
					n++;
				}
			}
			Vector2 edgeness = new Vector2();
			float ee = 0;
			for (Edge e : v.edges) {
				Vector2 p2 = positionFor(e.start != v ? e.start : e.end, underlyingGeometry);
				if (e.faces.size()==1)
				{
					Vector2 edgeDir = new Vector2(p2).sub(p);
					if (edgeness.dot(edgeDir)<0) edgeDir.scale(-1);
					
					edgeness.add(edgeDir);
					ee++;
				}
			}
			if (ee>0)
			{
				edgeness.scale(1/ee);
				d.projectOut(edgeness);
			}
			d.scale(-1f / n);
			d.normalize();
			d.scale(1);
			
			newPositions.put(v, p.add(d));
		}
		for (Vertex v : rep.allVertex) {
			setPosition(v, newPositions.get(v), underlyingGeometry);
		}
	}

	private static void setPosition(Vertex vertex, Vector2 vector2, iGeometry underlyingGeometry) {
		FloatBuffer v = underlyingGeometry.vertex();
		v.put(3 * vertex.vertexNumber + 0, vector2.x);
		v.put(3 * vertex.vertexNumber + 1, vector2.y);
	}

	private static Vector2 positionFor(Vertex vertex, iGeometry underlyingGeometry) {
		FloatBuffer v = underlyingGeometry.vertex();
		return new Vector2(v.get(3 * vertex.vertexNumber + 0), v.get(3 * vertex.vertexNumber + 1));
	}
}
