package field.graphics.dynamic;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import field.graphics.core.Base;
import field.graphics.core.BasicGeometry.TriangleMesh_long;
import field.math.linalg.Vector3;
import field.namespace.change.SimpleChangable;
import field.namespace.change.iChangable;

/**
 * slower than completeEdgeRep, but more working for dynamic meshes
 * 
 * Created on Apr 30, 2004
 * 
 * @author marc
 */
public class CompleteEdgeRep2 implements Serializable {

	static int static_vertex_uniqNumber = 0;

	static int static_edge_uniqNumber = 0;

	static int static_face_uniqNumber = 0;

	static int static_biface_uniqNumber = 0;

	static public class Vertex implements Serializable {

		public List<Edge> edges = new ArrayList();

		public int vertexNumber;

		protected int uniqeNumber = (static_vertex_uniqNumber++);

		public Vertex(int i) {
			this.vertexNumber = i;
		}

		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (obj == null) return false;
			return ((Vertex) obj).uniqeNumber == uniqeNumber;
		}

		public int hashCode() {
			return uniqeNumber * 18493;
		}

		public String toString() {
			return "v:" + vertexNumber + "(" + System.identityHashCode(this) + ")";
		}
	}

	static public class Edge implements Serializable {

		public Vertex end;

		public List<Face> faces = new ArrayList();

		public List<Vertex> nonEdgeVertex = new ArrayList();

		public Vertex start;

		public int edgeNumber;

		protected int uniqeNumber = (static_edge_uniqNumber++);

		public boolean equals(Object obj) {
			if (obj == this) return true;
			Edge o = (Edge) obj;

			return (start.vertexNumber == o.start.vertexNumber && end.vertexNumber == o.end.vertexNumber) || (start.vertexNumber == o.end.vertexNumber && end.vertexNumber == o.start.vertexNumber);

		}

		public int hashCode() {
			return (start == null ? 0 : start.vertexNumber) + (end == null ? 0 : end.vertexNumber);
		}

		public String toString() {
			return "e:[" + start + "," + end + "]";
		}
	}

	static public class Face implements Serializable {

		public Edge[] edges = new Edge[3];

		public Vertex[] vertex = new Vertex[3];

		public int faceNumber;

		public String toString() {
			return "f:[" + vertex[0] + "," + vertex[1] + "," + vertex[2] + "]";
		}

		List bifaces = new ArrayList(0);

		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (obj == null) return false;
			return ((Face) obj).uniqeNumber == uniqeNumber;
		}

		public int hashCode() {
			return uniqeNumber * 18493;
		}

		protected int uniqeNumber = (static_face_uniqNumber++);

	}

	static public class BiFace implements Serializable {

		public Face face1;

		public Face face2;

		public Edge hiddenEdge;

		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (obj == null) return false;
			return ((BiFace) obj).uniqeNumber == uniqeNumber;
		}

		public int hashCode() {
			return uniqeNumber * 18493;
		}

		protected int uniqeNumber = (static_biface_uniqNumber++);
	}

	public interface iNotification extends Serializable {

		public void beginOperation();

		public void edgeAdded(Edge e);

		public void edgeRemoved(Edge e);

		public void endOperation();

		public void faceAdded(Face f);

		public void faceRemoved(Face f);

		public void vertexAdded(Vertex v);

		public void vertexRemoved(Vertex v);

		public void biFaceAdded(BiFace v);

		public void biFaceRemoved(BiFace v);
	}

	static public abstract class aNotification implements iNotification {

		public void beginOperation() {
		}

		public void edgeAdded(Edge e) {
		}

		public void edgeRemoved(Edge e) {
		}

		public void endOperation() {
		}

		public void faceAdded(Face f) {
		}

		public void faceRemoved(Face f) {
		}

		public void vertexAdded(Vertex v) {
		}

		public void vertexRemoved(Vertex v) {
		}

		public void biFaceAdded(BiFace v) {
		}

		public void biFaceRemoved(BiFace v) {
		}
	}

	transient int creationCount = 0;

	protected abstract class Opcode implements Serializable {

		int priority;

		transient int subpriority = (creationCount++);

		public Opcode(int priority) {
			this.priority = priority;
		}

		protected Opcode() {
		}

		abstract public void fire();
	}

	Vector3 _t1 = new Vector3();

	Vector3 _t2 = new Vector3();

	Vector3 _t3 = new Vector3();

	public ArrayList<Edge> allEdge = new ArrayList<Edge>();

	SimpleChangable allEdge_change = new SimpleChangable();

	public ArrayList<Face> allFace = new ArrayList<Face>();

	SimpleChangable allFace_change = new SimpleChangable();

	public ArrayList<BiFace> allBiFace = new ArrayList<BiFace>();

	SimpleChangable allBiFace_change = new SimpleChangable();

	public ArrayList<Vertex> allVertex = new ArrayList<Vertex>();

	SimpleChangable allVertex_change = new SimpleChangable();

	int beginCount = 0;

	iChangable.iRecompute compute_openEdgeList = new iChangable.iRecompute(){

		public Object recompute() {

			List ret = new ArrayList();
			for (int i = 0; i < allEdge.size(); i++) {
				Edge e = (Edge) allEdge.get(i);
				if (e.faces.size() == 1) {
					ret.add(e);
				}
			}

			return ret;
		}
	};

	protected List notifications = new ArrayList();

	protected Opcode opcode_edgeAdded = new Opcode(2){

		public void fire() {
			Edge v = (Edge) operandList.get(operand_index++);
			for (int i = 0; i < notifications.size(); i++)
				((iNotification) notifications.get(i)).edgeAdded(v);
		}
	};

	protected Opcode opcode_edgeRemoved = new Opcode(-2){

		public void fire() {
			Edge v = (Edge) operandList.get(operand_index++);
			for (int i = 0; i < notifications.size(); i++)
				((iNotification) notifications.get(i)).edgeRemoved(v);
		}
	};

	protected Opcode opcode_faceAdded = new Opcode(1){

		public void fire() {
			Face v = (Face) operandList.get(operand_index++);
			for (int i = 0; i < notifications.size(); i++)
				((iNotification) notifications.get(i)).faceAdded(v);
		}
	};

	protected Opcode opcode_faceRemoved = new Opcode(-1){

		public void fire() {
			Face v = (Face) operandList.get(operand_index++);
			for (int i = 0; i < notifications.size(); i++)
				((iNotification) notifications.get(i)).faceRemoved(v);
		}
	};

	protected Opcode opcode_vertexAdded = new Opcode(3){

		public void fire() {
			Vertex v = (Vertex) operandList.get(operand_index++);
			for (int i = 0; i < notifications.size(); i++)
				((iNotification) notifications.get(i)).vertexAdded(v);
		}
	};

	protected Opcode opcode_vertexRemoved = new Opcode(-3){

		public void fire() {
			Vertex v = (Vertex) operandList.get(operand_index++);
			for (int i = 0; i < notifications.size(); i++)
				((iNotification) notifications.get(i)).vertexRemoved(v);
		}
	};

	protected Opcode opcode_biFaceAdded = new Opcode(3){

		public void fire() {
			BiFace v = (BiFace) operandList.get(operand_index++);
			for (int i = 0; i < notifications.size(); i++)
				((iNotification) notifications.get(i)).biFaceAdded(v);
		}
	};

	protected Opcode opcode_biFaceRemoved = new Opcode(-3){

		public void fire() {
			BiFace v = (BiFace) operandList.get(operand_index++);
			for (int i = 0; i < notifications.size(); i++)
				((iNotification) notifications.get(i)).biFaceRemoved(v);
		}
	};

	transient protected Comparator opcodeComparator;

	protected List opcodeList = new ArrayList();

	iChangable.iModCount openEdgeList_mod = allEdge_change.getModCount(new ArrayList());

	protected int operand_index = 0;

	protected List operandList = new ArrayList();

	
	transient private FloatBuffer vertex2;

	public CompleteEdgeRep2() {
		opcodeComparator = new Comparator(){

			public int compare(Object o1, Object o2) {
				int subpriority = ((Opcode) o1).subpriority < ((Opcode) o2).subpriority ? -1 : 1;
				return ((Opcode) o1).priority < ((Opcode) o2).priority ? 1 : (((Opcode) o1).priority == ((Opcode) o2).priority ? subpriority : -1);
			}
		};
	}

	public CompleteEdgeRep2(Base.iGeometry from) {
		this();
		add(from);
	}

	public CompleteEdgeRep2 add(List< ? > vertex, List<Integer> triangles) {

		Edge tmpEdge = new Edge();
		HashMap alreadySeenEdges = new HashMap();


		beginOperation();

		int voffset = allVertex.size();

		for (int i = 0; i < vertex.size(); i++) {
			Vertex v = new Vertex(i + voffset);
			allVertex.add(v);
			vertexAdded(v);
		}

		for (int i = 0; i < triangles.size() / 3; i++) {
			int v1 = triangles.get(3 * i + 0) + voffset;
			int v2 = triangles.get(3 * i + 1) + voffset;
			int v3 = triangles.get(3 * i + 2) + voffset;

			Face face = new Face();
			face.vertex[0] = vertex(v1);
			face.vertex[1] = vertex(v2);
			face.vertex[2] = vertex(v3);
			allFace.add(face);
			faceAdded(face);

			tmpEdge.start = vertex(v1);
			tmpEdge.end = vertex(v2);
			Edge already = (Edge) alreadySeenEdges.get(tmpEdge);
			if (already == null) {

				Edge newEdge = new Edge();
				newEdge.start = tmpEdge.start;
				newEdge.end = tmpEdge.end;
				allEdge.add(newEdge);
				edgeAdded(newEdge);
				Object q = alreadySeenEdges.put(newEdge, newEdge);
				assert q == null;
				already = newEdge;
			} else {
			}
			already.faces.add(face);
			face.edges[0] = already;

			if (!vertex(v1).edges.contains(already)) vertex(v1).edges.add(already);
			if (!vertex(v2).edges.contains(already)) vertex(v2).edges.add(already);

			tmpEdge.start = vertex(v1);
			tmpEdge.end = vertex(v3);
			already = (Edge) alreadySeenEdges.get(tmpEdge);
			if (already == null) {
				Edge newEdge = new Edge();
				newEdge.start = tmpEdge.start;
				newEdge.end = tmpEdge.end;
				allEdge.add(newEdge);
				edgeAdded(newEdge);
				Object q = alreadySeenEdges.put(newEdge, newEdge);
				assert q == null;
				already = newEdge;
			} else {
			}
			already.faces.add(face);
			face.edges[1] = already;

			if (!vertex(v1).edges.contains(already)) vertex(v1).edges.add(already);
			if (!vertex(v3).edges.contains(already)) vertex(v3).edges.add(already);

			tmpEdge.start = vertex(v2);
			tmpEdge.end = vertex(v3);
			already = (Edge) alreadySeenEdges.get(tmpEdge);
			if (already == null) {
				Edge newEdge = new Edge();
				newEdge.start = tmpEdge.start;
				newEdge.end = tmpEdge.end;
				allEdge.add(newEdge);
				edgeAdded(newEdge);
				Object q = alreadySeenEdges.put(newEdge, newEdge);
				assert q == null;
				already = newEdge;
			} else {
			}
			already.faces.add(face);
			face.edges[2] = already;

			if (!vertex(v2).edges.contains(already)) vertex(v2).edges.add(already);
			if (!vertex(v3).edges.contains(already)) vertex(v3).edges.add(already);
		}


		for (int i = 0; i < allEdge.size(); i++) {
			Edge e = edge(i);

			for (int n = 0; n < e.faces.size(); n++) {
				Face f = (Face) e.faces.get(n);
				for (int q = 0; q < f.vertex.length; q++) {
					if ((f.vertex[q] != e.start) && (f.vertex[q] != e.end)) {
						e.nonEdgeVertex.add(f.vertex[q]);
						break;
					}
				}
			}
		}

		allVertex_change.dirty();
		allFace_change.dirty();
		allEdge_change.dirty();

		endOperation();

		return this;
	}

	public CompleteEdgeRep2 add(Base.iGeometry from) {
		ShortBuffer triangles = from.triangle();
		Edge tmpEdge = new Edge();
		HashMap alreadySeenEdges = new HashMap();


		FloatBuffer vertex = from.vertex();
		beginOperation();

		int voffset = allVertex.size();

		for (int i = 0; i < from.numVertex(); i++) {
			Vertex v = new Vertex(i + voffset);
			allVertex.add(v);
			vertexAdded(v);
		}

		for (int i = 0; i < from.numTriangle(); i++) {
			int v1 = triangles.get() + voffset;
			int v2 = triangles.get() + voffset;
			int v3 = triangles.get() + voffset;

			Face face = new Face();
			face.vertex[0] = vertex(v1);
			face.vertex[1] = vertex(v2);
			face.vertex[2] = vertex(v3);
			allFace.add(face);
			faceAdded(face);
			
			tmpEdge.start = vertex(v1);
			tmpEdge.end = vertex(v2);
			Edge already = (Edge) alreadySeenEdges.get(tmpEdge);
			if (already == null) {

				Edge newEdge = new Edge();
				newEdge.start = tmpEdge.start;
				newEdge.end = tmpEdge.end;
				allEdge.add(newEdge);
				edgeAdded(newEdge);
				already = newEdge;
				alreadySeenEdges.put(newEdge, newEdge);
			} else {
			}
			already.faces.add(face);
			face.edges[0] = already;

			vertex(v1).edges.add(already);
			vertex(v2).edges.add(already);

			tmpEdge.start = vertex(v1);
			tmpEdge.end = vertex(v3);
			already = (Edge) alreadySeenEdges.get(tmpEdge);
			if (already == null) {
				Edge newEdge = new Edge();
				newEdge.start = tmpEdge.start;
				newEdge.end = tmpEdge.end;
				allEdge.add(newEdge);
				edgeAdded(newEdge);
				alreadySeenEdges.put(newEdge, newEdge);
				already = newEdge;
			} else {
			}
			already.faces.add(face);
			face.edges[1] = already;

			vertex(v1).edges.add(already);
			vertex(v3).edges.add(already);

			tmpEdge.start = vertex(v2);
			tmpEdge.end = vertex(v3);
			already = (Edge) alreadySeenEdges.get(tmpEdge);
			if (already == null) {
				Edge newEdge = new Edge();
				newEdge.start = tmpEdge.start;
				newEdge.end = tmpEdge.end;
				allEdge.add(newEdge);
				edgeAdded(newEdge);
				alreadySeenEdges.put(newEdge, newEdge);
				already = newEdge;
			} else {
			}
			already.faces.add(face);
			face.edges[2] = already;

			vertex(v2).edges.add(already);
			vertex(v3).edges.add(already);
		}


		for (int i = 0; i < allEdge.size(); i++) {
			Edge e = edge(i);

			for (int n = 0; n < e.faces.size(); n++) {
				Face f = (Face) e.faces.get(n);
				for (int q = 0; q < f.vertex.length; q++) {
					if ((f.vertex[q] != e.start) && (f.vertex[q] != e.end)) {
						e.nonEdgeVertex.add(f.vertex[q]);
						break;
					}
				}
			}
		}

		allVertex_change.dirty();
		allFace_change.dirty();
		allEdge_change.dirty();

		endOperation();

		return this;
	}
	
	public CompleteEdgeRep2 add(TriangleMesh_long from) {
		IntBuffer triangles = from.longTriangle();
		Edge tmpEdge = new Edge();
		HashMap alreadySeenEdges = new HashMap();


		FloatBuffer vertex = from.vertex();
		beginOperation();

		int voffset = allVertex.size();

		for (int i = 0; i < from.numVertex(); i++) {
			Vertex v = new Vertex(i + voffset);
			allVertex.add(v);
			vertexAdded(v);
		}

		for (int i = 0; i < from.numTriangle(); i++) {
			int v1 = triangles.get() + voffset;
			int v2 = triangles.get() + voffset;
			int v3 = triangles.get() + voffset;

			if (v1==v2 || v2 == v3 || v1 == v3) {
				;//System.out.println(" degenerate triangle");
				continue;
			}
			
			
			Face face = new Face();
			face.vertex[0] = vertex(v1);
			face.vertex[1] = vertex(v2);
			face.vertex[2] = vertex(v3);
			allFace.add(face);
			faceAdded(face);
			
			tmpEdge.start = vertex(v1);
			tmpEdge.end = vertex(v2);
			Edge already = (Edge) alreadySeenEdges.get(tmpEdge);
			if (already == null) {

				Edge newEdge = new Edge();
				newEdge.start = tmpEdge.start;
				newEdge.end = tmpEdge.end;
				allEdge.add(newEdge);
				edgeAdded(newEdge);
				already = newEdge;
				alreadySeenEdges.put(newEdge, newEdge);
			} else {
			}
			already.faces.add(face);
			face.edges[0] = already;

			vertex(v1).edges.add(already);
			vertex(v2).edges.add(already);

			tmpEdge.start = vertex(v1);
			tmpEdge.end = vertex(v3);
			already = (Edge) alreadySeenEdges.get(tmpEdge);
			if (already == null) {
				Edge newEdge = new Edge();
				newEdge.start = tmpEdge.start;
				newEdge.end = tmpEdge.end;
				allEdge.add(newEdge);
				edgeAdded(newEdge);
				alreadySeenEdges.put(newEdge, newEdge);
				already = newEdge;
			} else {
			}
			already.faces.add(face);
			face.edges[1] = already;

			vertex(v1).edges.add(already);
			vertex(v3).edges.add(already);

			tmpEdge.start = vertex(v2);
			tmpEdge.end = vertex(v3);
			already = (Edge) alreadySeenEdges.get(tmpEdge);
			if (already == null) {
				Edge newEdge = new Edge();
				newEdge.start = tmpEdge.start;
				newEdge.end = tmpEdge.end;
				allEdge.add(newEdge);
				edgeAdded(newEdge);
				alreadySeenEdges.put(newEdge, newEdge);
				already = newEdge;
			} else {
			}
			already.faces.add(face);
			face.edges[2] = already;

			vertex(v2).edges.add(already);
			vertex(v3).edges.add(already);
		}


		for (int i = 0; i < allEdge.size(); i++) {
			Edge e = edge(i);

			for (int n = 0; n < e.faces.size(); n++) {
				Face f = (Face) e.faces.get(n);
				for (int q = 0; q < f.vertex.length; q++) {
					if ((f.vertex[q] != e.start) && (f.vertex[q] != e.end)) {
						e.nonEdgeVertex.add(f.vertex[q]);
						break;
					}
				}
			}
		}

		allVertex_change.dirty();
		allFace_change.dirty();
		allEdge_change.dirty();

		endOperation();

		return this;
	}

	public Face addCompletelyNewFace() {
		beginOperation();
		Face ret = new Face();
		allFace.add(ret);
		faceAdded(ret);
		Edge e1 = new Edge();
		allEdge.add(e1);
		edgeAdded(e1);
		Edge e2 = new Edge();
		allEdge.add(e2);
		edgeAdded(e2);
		Edge e3 = new Edge();
		allEdge.add(e3);
		edgeAdded(e3);

		Vertex v1 = new Vertex(allVertex.size());
		allVertex.add(v1);
		vertexAdded(v1);
		Vertex v2 = new Vertex(allVertex.size());
		allVertex.add(v2);
		vertexAdded(v2);
		Vertex v3 = new Vertex(allVertex.size());
		allVertex.add(v3);
		vertexAdded(v3);

		ret.edges[0] = e1;
		ret.edges[1] = e2;
		ret.edges[2] = e3;

		e1.faces.add(ret);
		e2.faces.add(ret);
		e3.faces.add(ret);

		e1.start = v1;
		e1.end = v2;
		e2.start = v2;
		e2.end = v3;
		e3.start = v3;
		e3.end = v1;
		e1.nonEdgeVertex.add(v3);
		e2.nonEdgeVertex.add(v1);
		e3.nonEdgeVertex.add(v2);

		v1.edges.add(e1);
		v1.edges.add(e3);
		v2.edges.add(e1);
		v2.edges.add(e2);
		v3.edges.add(e2);
		v3.edges.add(e3);

		ret.vertex[0] = v1;
		ret.vertex[1] = v2;
		ret.vertex[2] = v3;

		allVertex_change.dirty();
		allFace_change.dirty();
		allEdge_change.dirty();

		endOperation();
		return ret;
	}

	public BiFace addCompletelyNewBiFace(int innerFaceNumber) {
		beginOperation();
		Face f1 = addCompletelyNewFace();
		Face f2 = addVertexToMakeNewTriangle(f1.edges[innerFaceNumber]);
		BiFace b = newBiFace(f1, f2);
		endOperation();
		return b;
	}

	public Vertex addIsolatedVertex() {
		beginOperation();
		Vertex v = new Vertex(allVertex.size());
		allVertex.add(v);
		vertexAdded(v);
		endOperation();
		return v;
	}

	public BiFace extendBiFaceEdge(BiFace biface, int edgeNumber) {
		Face f = edgeNumber < 2 ? biface.face1 : biface.face2;
		edgeNumber = edgeNumber % 2;
		Edge e = null;
		int found = -1;
		int x = 0;
		while (found != edgeNumber) {
			assert x < f.edges.length : "couldn't find edge number <" + edgeNumber + "> that isn't <" + biface.hiddenEdge + "> in <" + Arrays.asList(f.edges) + ">";
			if (f.edges[x] != biface.hiddenEdge) {
				found++;
				e = f.edges[x];
			}
			x++;
		}

		beginOperation();
		Face fNew1 = addVertexToMakeNewTriangle(e);

		// get an edge that isn't e
		Edge eFound = null;
		x = 0;
		while (eFound == null) {
			if (fNew1.edges[x] != e) {
				eFound = fNew1.edges[x];
			}
			x++;
		}

		Face fNew2 = addVertexToMakeNewTriangle(eFound);

		BiFace biFace = newBiFace(fNew1, fNew2);
		endOperation();

		return biFace;
	}

	/**
	 * hmmm.
	 */
	public BiFace[] quadSectBiFace(BiFace biface) {
		BiFace[] ret = new BiFace[4];

		beginOperation();

		Face[] nf1 = quadSectFace(biface.face1);
		Face[] nf2 = quadSectFace(biface.face2);

		// two of these faces share a
		// vertex that should be merged,
		// it's the vertex that is on the
		// hidden edge between the biface
		// we know that its one of the
		// vertices that is on nf1[0] and
		// nf2[0]

		Vertex cross1 = biface.hiddenEdge.start;
		Vertex cross2 = biface.hiddenEdge.end;

		Vertex v1 = null;
		Vertex v2 = null;
		for (int i = 0; i < 3; i++) {
			if (edgeExists(nf1[0].vertex[i], cross1) && edgeExists(nf1[0].vertex[i], cross2)) {
				assert v1 == null;
				v1 = nf1[0].vertex[i];
			}
			if (edgeExists(nf2[0].vertex[i], cross1) && edgeExists(nf2[0].vertex[i], cross2)) {
				assert v2 == null;
				v2 = nf2[0].vertex[i];
			}
		}

		assert v1 != null;
		assert v2 != null;

		ArrayList v1list = new ArrayList();
		v1list.add(v1);

		replaceVerticesWithVertex(v1list, v2);


		// ok, now we have stitched those
		// together, we have five new
		// vertices, 8 new faces
		// we have to marry these faces
		// into 4 bifaces.
		// unfortunately we cannot guess
		// the order so, we're going to
		// have to work it out
		// hmm, if we had a regular
		// expression language for
		// topologies...
		// we know that
		// all four new bifaces involve
		// v2 and one and only one of
		// biface.vertices
		// an original vertex has either
		// one or two faces in the face
		// list depending on which corner
		// it is

		HashSet targetVertex = new HashSet();
		targetVertex.addAll(Arrays.asList(biface.face1.vertex));
		targetVertex.addAll(Arrays.asList(biface.face2.vertex));

		assert targetVertex.size() == 4 : targetVertex;
		Iterator i = targetVertex.iterator();

		int index = 0;
		int one = 0;
		int two = 0;

		while (i.hasNext()) {
			Vertex vtarget = (Vertex) i.next();

			List faceList = findFace(nf1, nf2, vtarget);
			assert faceList.size() == 1 || faceList.size() == 2 : faceList;
			if (faceList.size() == 1) {
				Face face = (Face) faceList.get(0);

				// ok,
				// now
				// we
				// need
				// to
				// find
				// an
				// edge
				// that
				// doesn't
				// contain
				// this
				// vertex
				Edge n = findEdgeThatDoesntContain(face.edges, vtarget);

				assert n != null;
				// ok,
				// now
				// we
				// need
				// to
				// find
				// a
				// face
				// that
				// contains
				// this
				// edge

				Face face2 = findFace(n.faces, face);
				assert face2 != null;

				BiFace nbf = new BiFace();
				nbf.face1 = face;
				nbf.face2 = face2;
				nbf.hiddenEdge = n;
				face.bifaces.add(nbf);
				face2.bifaces.add(nbf);

				biFaceAdded(nbf);
				allBiFace.add(nbf);
				assert nbf.hiddenEdge != null;

				ret[index++] = nbf;
				one++;

			} else {
				Face face1 = (Face) faceList.get(0);
				Face face2 = (Face) faceList.get(1);

				BiFace nbf = new BiFace();
				nbf.face1 = face1;
				nbf.face2 = face2;
				face1.bifaces.add(nbf);
				face2.bifaces.add(nbf);
				biFaceAdded(nbf);

				nbf.hiddenEdge = commonEdge(face1, face2);

				assert nbf.hiddenEdge != null : face1 + " " + face2 + " " + Arrays.asList(face1.edges) + " " + Arrays.asList(face2.edges);

				allBiFace.add(nbf);

				ret[index++] = nbf;
				two++;

			}
		}

		endOperation();

		assert index == 4 : index;
		assert one == 2 : one;
		assert two == 2 : two;

		return ret;
	}

	private Edge commonEdge(Face face1, Face face2) {
		for (int i = 0; i < face1.edges.length; i++)
			for (int j = 0; j < face2.edges.length; j++) {
				if (face1.edges[i] == face2.edges[j]) return face1.edges[i];
			}
		return null;
	}

	private Face findFace(List list, Face isnt) {
		assert list.size() == 2;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) != isnt) return (Face) list.get(i);
		}
		return null;
	}

	private Edge findEdgeThatDoesntContain(Edge[] edges, Vertex vtarget) {
		for (int i = 0; i < edges.length; i++)
			if ((edges[i].start != vtarget) && (edges[i].end != vtarget)) return edges[i];
		return null;
	}

	private List findFace(Face[] nf1, Face[] nf2, Vertex vtarget) {
		ArrayList list = new ArrayList(1);
		for (int i = 0; i < nf1.length; i++)
			if (indexOf(vtarget, nf1[i].vertex) != -1) list.add(nf1[i]);
		for (int i = 0; i < nf2.length; i++)
			if (indexOf(vtarget, nf2[i].vertex) != -1) list.add(nf2[i]);
		return list;
	}

	public Vertex addVertexBisectingEdgeAndFaces(Edge bisect) {
		beginOperation();
		Vertex v = new Vertex(allVertex.size());
		vertexAdded(v);
		allVertex.add(v);

		Vertex start = bisect.start;
		Vertex end = bisect.end;

		endOperation();

		List face = new ArrayList(bisect.faces);


		for (int i = 0; i < face.size(); i++) {
			Face f = (Face) face.get(i);
			Vertex other = otherVertex(start, end, f.edges);
			addFaceOverVertices(v, start, other);
			addFaceOverVertices(v, end, other);


		}

		for (int i = 0; i < face.size(); i++) {
			Face f = (Face) face.get(i);
			removeOnlyFace(f);
		}

		return v;
	}

	public Vertex otherVertex(Vertex start, Vertex end, Edge[] edges) {
		for (int i = 0; i < edges.length; i++) {
			if (edges[i].start != start && edges[i].start != end) return edges[i].start;
			if (edges[i].end != end && edges[i].end != end) return edges[i].end;
		}
		assert false : start + " " + end + " " + Arrays.asList(edges);
		return null;
	}

	/**
	 * older and newer share an edge and two vertices. The question here is of the two other vertices of newer which order should they go in in order to be in the same order as the shared two we return a vertex[4] old1, new1, old2, new2
	 */

	public Vertex[] sortNewVerticesFromExtendedBiFaceEdge(BiFace older, BiFace newer) {
		Vertex[] r = new Vertex[4];

		Set vertexOlder = new HashSet(4);
		Set vertexNewer = new HashSet(4);

		vertexOlder.addAll(Arrays.asList(older.face1.vertex));
		vertexOlder.addAll(Arrays.asList(older.face2.vertex));

		assert vertexOlder.size() == 4 : vertexOlder + " " + Arrays.asList(older.face1.vertex) + " " + Arrays.asList(older.face2.vertex);

		vertexNewer.addAll(Arrays.asList(newer.face1.vertex));
		vertexNewer.addAll(Arrays.asList(newer.face2.vertex));

		assert vertexNewer.size() == 4 : vertexNewer + " " + Arrays.asList(newer.face1.vertex) + " " + Arrays.asList(newer.face2.vertex);

		vertexOlder.retainAll(vertexNewer);
		Set shared = vertexOlder;

		vertexNewer.removeAll(vertexOlder);
		Set uniq = vertexNewer;

		assert shared.size() == 2 : shared + " " + uniq;
		assert uniq.size() == 2 : uniq + " " + shared;

		// one of shared has an edge to
		// only one vertex in uniq

		Iterator si = shared.iterator();
		Vertex o1 = (Vertex) si.next();
		Vertex o2 = (Vertex) si.next();

		Iterator ui = uniq.iterator();
		Vertex n1 = (Vertex) ui.next();
		Vertex n2 = (Vertex) ui.next();

		if (edgeExists(o1, n1)) {
			if (!edgeExists(o1, n2)) {
				assert edgeExists(o2, n2) && edgeExists(o2, n1);
				r[0] = o1;
				r[1] = n1;
				r[2] = o2;
				r[3] = n2;
				return r;
			} else {
				if (edgeExists(o2, n1)) {
					assert !edgeExists(o2, n2);
					r[0] = o2;
					r[1] = n1;
					r[2] = o1;
					r[1] = n2;
					return r;
				} else {
					assert edgeExists(o2, n2);
					r[0] = o2;
					r[1] = n2;
					r[2] = o1;
					r[2] = n1;
					return r;
				}
			}
		} else {
			assert edgeExists(o1, n2) && (edgeExists(o2, n2) && edgeExists(o2, n1));
			r[0] = o1;
			r[1] = n2;
			r[2] = o2;
			r[1] = n1;
			return r;
		}
	}

	public boolean edgeExists(Vertex o1, Vertex n1) {
		for (int i = 0; i < o1.edges.size(); i++) {
			Edge e = (Edge) o1.edges.get(i);
			if (e.start == n1) return true;
			if (e.end == n1) return true;
		}
		return false;
	}

	public List getAdjecentBiFaces(BiFace b) {
		ArrayList ret = new ArrayList();
		for (int i = 0; i < b.face1.edges.length; i++) {
			Edge e = b.face1.edges[i];
			if (e != b.hiddenEdge) {
				for (int n = 0; n < e.faces.size(); n++) {
					Face f = (Face) e.faces.get(n);
					if (f != b.face1) {
						ret.addAll(f.bifaces);
					}
				}
			}
		}
		for (int i = 0; i < b.face2.edges.length; i++) {
			Edge e = b.face2.edges[i];
			if (e != b.hiddenEdge) {
				for (int n = 0; n < e.faces.size(); n++) {
					Face f = (Face) e.faces.get(n);
					if (f != b.face2) {
						ret.addAll(f.bifaces);
					}
				}
			}
		}

		assert noDuplicates(ret) : " bizzare biface topology problem <" + ret + ">";
		return ret;
	}

	private boolean noDuplicates(ArrayList ret) {
		for (int i = 0; i < ret.size(); i++) {
			if (ret.lastIndexOf(ret.get(i)) != ret.indexOf(ret.get(i))) return false;
		}
		return true;
	}

	public Face hasFace(Vertex v1, Vertex v2, Vertex v3) {
		Edge e12 = findEdge(v1, v2);
		if (e12 == null) return null;
		Edge e23 = findEdge(v2, v3);
		if (e23 == null) return null;
		Edge e31 = findEdge(v3, v1);
		if (e31 == null) return null;

		for (int f = 0; f < e12.faces.size(); f++) {
			Face face = (Face) e12.faces.get(f);
			for (int n = 0; n < face.edges.length; n++) {
				if (face.edges[n] == e23) return face;
			}
		}
		return null;
	}

	public Face addFaceOverEdges(Edge e1, Edge e2, Edge e3) {
		beginOperation();

		assert e1 != e2;
		assert e1 != e3;
		assert e2 != e3;

		Face newFace = new Face();
		allFace.add(newFace);
		faceAdded(newFace);

		newFace.edges[0] = e1;
		newFace.edges[1] = e2;
		newFace.edges[2] = e3;

		e1.faces.add(newFace);
		e2.faces.add(newFace);
		e3.faces.add(newFace);

		HashSet vertex = new HashSet();
		vertex.add(e1.start);
		vertex.add(e1.end);
		vertex.add(e2.start);
		vertex.add(e2.end);
		vertex.add(e3.start);
		vertex.add(e3.end);

		assert vertex.size() == 3 : vertex;
		Iterator i = vertex.iterator();
		newFace.vertex[0] = (Vertex) i.next();
		newFace.vertex[1] = (Vertex) i.next();
		newFace.vertex[2] = (Vertex) i.next();

		e1.nonEdgeVertex.add((Vertex) oneNotIn(newFace.vertex, new Vertex[] { e1.start, e1.end}));
		e2.nonEdgeVertex.add((Vertex) oneNotIn(newFace.vertex, new Vertex[] { e2.start, e2.end}));
		e3.nonEdgeVertex.add((Vertex) oneNotIn(newFace.vertex, new Vertex[] { e3.start, e3.end}));

		endOperation();

		allFace_change.dirty();
		allEdge_change.dirty();

		return newFace;
	}

	public Face addFaceOverVertices(Vertex v1, Vertex v2, Vertex v3) {
		beginOperation();

		Edge e01, e02, e12;

		e01 = findEdge(v1, v2);
		if (e01 == null) {
			e01 = new Edge();
			e01.start = v1;
			e01.end = v2;
			v1.edges.add(e01);
			v2.edges.add(e01);
			allEdge.add(e01);
			edgeAdded(e01);
		} else
		e01.nonEdgeVertex.add(v3);
		e02 = findEdge(v1, v3);
		if (e02 == null) {
			e02 = new Edge();
			e02.start = v1;
			e02.end = v3;
			v1.edges.add(e02);
			v3.edges.add(e02);
			allEdge.add(e02);
			edgeAdded(e02);
		} else
		e02.nonEdgeVertex.add(v2);
		e12 = findEdge(v2, v3);
		if (e12 == null) {
			e12 = new Edge();
			e12.start = v2;
			e12.end = v3;
			v2.edges.add(e12);
			v3.edges.add(e12);
			allEdge.add(e12);
			edgeAdded(e12);
		} else
		e12.nonEdgeVertex.add(v1);

		Face ret = addFaceOverEdges(e12, e02, e01);

		endOperation();
		return ret;
	}

	public Face addNewFaceBetweenEdges(Edge e1, Edge e2) {
		Face newFace = new Face();
		allFace.add(newFace);
		faceAdded(newFace);

		e1.faces.add(newFace);
		e2.faces.add(newFace);

		// now we need to know where to
		// put a new edge

		Edge newEdge = new Edge();

		if ((e1.start != e2.start) && (e1.start != e2.end)) {
			if (newEdge.start == null) {
				newEdge.start = e1.start;
				e1.start.edges.add(newEdge);
			} else {
				assert newEdge.end == null;
				newEdge.end = e1.start;
				e1.start.edges.add(newEdge);
			}
			newFace.vertex[0] = e1.start;
			newFace.vertex[1] = e2.start;
			newFace.vertex[2] = e2.end;
		}
		if ((e1.end != e2.start) && (e1.end != e2.end)) {
			if (newEdge.start == null) {
				newEdge.start = e1.end;
				e1.end.edges.add(newEdge);
			} else {
				assert newEdge.end == null;
				newEdge.end = e1.end;
				e1.end.edges.add(newEdge);
			}
			newFace.vertex[0] = e1.end;
			newFace.vertex[1] = e2.start;
			newFace.vertex[2] = e2.end;
		}
		if ((e2.start != e1.start) && (e2.start != e1.end)) {
			if (newEdge.start == null) {
				newEdge.start = e2.start;
				e2.start.edges.add(newEdge);
			} else {
				assert newEdge.end == null;
				newEdge.end = e2.start;
				e2.start.edges.add(newEdge);
			}
			newFace.vertex[0] = e2.start;
			newFace.vertex[1] = e1.start;
			newFace.vertex[2] = e1.end;
		}
		if ((e2.end != e1.start) && (e2.end != e1.end)) {
			if (newEdge.start == null) {
				newEdge.start = e2.end;
				e2.end.edges.add(newEdge);
			} else {
				assert newEdge.end == null;
				newEdge.end = e2.end;
				e2.end.edges.add(newEdge);
			}
			newFace.vertex[0] = e2.end;
			newFace.vertex[1] = e1.start;
			newFace.vertex[2] = e1.end;
		}

		assert newEdge.start != null;
		assert newEdge.end != null;

		allEdge.add(newEdge);
		edgeAdded(newEdge);

		newEdge.start.edges.add(newEdge);
		newEdge.end.edges.add(newEdge);

		newFace.edges[0] = e1;
		newFace.edges[1] = e2;
		newFace.edges[2] = newEdge;

		newEdge.faces.add(newFace);

		allVertex_change.dirty();
		allFace_change.dirty();
		allEdge_change.dirty();

		endOperation();

		return newFace;
	}

	public void addNotification(iNotification not) {
		notifications.add(not);
	}

	public void catchupNotification(iNotification not) {
		for (int i = 0; i < allVertex.size(); i++)
			not.vertexAdded((Vertex) allVertex.get(i));
		for (int i = 0; i < allEdge.size(); i++)
			not.edgeAdded((Edge) allEdge.get(i));
		for (int i = 0; i < allFace.size(); i++)
			not.faceAdded((Face) allFace.get(i));
		for (int i = 0; i < allBiFace.size(); i++)
			not.biFaceAdded((BiFace) allBiFace.get(i));
	}

//	public Face[] addProjectedDelaunayTetrahedalization(FloatBuffer vertex3, boolean minTwo) {
//		assert vertex3.isDirect();
//
//		beginOperation();
//
//		if (thisDelaunay == null) thisDelaunay = new Delaunay();
//
//		int num = vertex3.remaining() / 3;
//
//		int voffset = allVertex.size();
//
//		for (int i = 0; i < num; i++) {
//			Vertex v = new Vertex(allVertex.size());
//			allVertex.add(v);
//			vertexAdded(v);
//		}
//
//		IntBuffer r = thisDelaunay.computeThree(vertex3);
//
//		Face[] ret = new Face[minTwo ? 2 * r.capacity() / 4 : r.capacity()];
//		float[] faceAreas = new float[4];
//		int[][] faces = new int[][] { { 0, 1, 2}, { 1, 2, 3}, { 0, 1, 3}, { 0, 2, 3}};
//		int[] faceIndex = new int[4];
//
//		for (int i = 0; i < r.capacity() / 4; i++) {
//			int n0 = r.get(i * 4 + 0);
//			int n1 = r.get(i * 4 + 1);
//			int n2 = r.get(i * 4 + 2);
//			int n3 = r.get(i * 4 + 3);
//
//			faceIndex[0] = n0;
//			faceIndex[1] = n1;
//			faceIndex[2] = n2;
//			faceIndex[3] = n3;
//
//			if (minTwo) {
//				// compute
//				// areas
//				// of
//				// each
//				// face
//				int minAreaAt = -1;
//				float minAreaIs = Float.POSITIVE_INFINITY;
//				int min2AreaAt = -1;
//				float min2AreaIs = Float.POSITIVE_INFINITY;
//				for (int n = 0; n < faceAreas.length; n++) {
//					faceAreas[n] = faceArea(faceIndex[faces[n][0]], faceIndex[faces[n][1]], faceIndex[faces[n][2]], vertex3);
//					if (faceAreas[n] < minAreaIs) {
//						min2AreaAt = minAreaAt;
//						min2AreaIs = minAreaIs;
//						minAreaAt = n;
//						minAreaIs = faceAreas[n];
//					} else if (faceAreas[n] < min2AreaIs) {
//						min2AreaIs = faceAreas[n];
//						min2AreaAt = n;
//					}
//				}
//
//				ret[i * 2 + 0] = addFaceOverVertices(vertex(faceIndex[faces[minAreaAt][0]]), vertex(faceIndex[faces[minAreaAt][1]]), vertex(faceIndex[faces[minAreaAt][2]]));
//				ret[i * 2 + 1] = addFaceOverVertices(vertex(faceIndex[faces[min2AreaAt][0]]), vertex(faceIndex[faces[min2AreaAt][1]]), vertex(faceIndex[faces[min2AreaAt][2]]));
//			} else {
//				Vertex v1 = vertex(n0 + voffset);
//				Vertex v2 = vertex(n1 + voffset);
//				Vertex v3 = vertex(n2 + voffset);
//				Vertex v4 = vertex(n3 + voffset);
//
//				ret[i * 4 + 0] = addFaceOverVertices(v1, v2, v3);
//				ret[i * 4 + 1] = addFaceOverVertices(v2, v3, v4);
//				ret[i * 4 + 2] = addFaceOverVertices(v1, v3, v4);
//				ret[i * 4 + 3] = addFaceOverVertices(v1, v2, v4);
//			}
//
//		}
//
//		endOperation();
//		return ret;
//	}
//
//	public Face[] addProjectedDelaunayTetrahedalization(Vector3[] vertex3, boolean minTwo) {
//		FloatBuffer t = ByteBuffer.allocateDirect(vertex3.length * 3 * 4).asFloatBuffer();// FloatBuffer.allocate(vertex3.length*3);
//		for (int i = 0; i < vertex3.length; i++)
//			t.put(vertex3[i].x).put(vertex3[i].y).put(vertex3[i].z);
//
//		t.rewind();
//
//		return addProjectedDelaunayTetrahedalization(t, minTwo);
//
//	}
//
//	public Face[] addProjectedDelaunayTriangulation(FloatBuffer vertex3, Vector3 rightDirection, Vector3 upDirection) {
//		beginOperation();
//
//		float dx = rightDirection.x;
//		float dy = rightDirection.y;
//		float dz = rightDirection.z;
//
//		float ux = upDirection.x;
//		float uy = upDirection.y;
//		float uz = upDirection.z;
//
//		if (thisDelaunay == null) thisDelaunay = new Delaunay();
//
//		int num = vertex3.remaining() / 3;
//
//		if ((vertex2 == null) || (vertex2.capacity() < num * 3)) {
//			vertex2 = ByteBuffer.allocateDirect(num * 4 * 3).asFloatBuffer();
//		}
//
//		vertex2.rewind();
//		vertex2.limit(num * 2);
//
//		int voffset = allVertex.size();
//
//
//		for (int i = 0; i < num; i++) {
//			float fx = vertex3.get();
//			float fy = vertex3.get();
//			float fz = vertex3.get();
//
//			float r = dx * fx + dy * fy + dz * fz;
//			float u = ux * fx + uy * fy + uz * fz;
//
//			;//System.out.println(" coordinates <" + r + "> <" + u + "> from <" + i + ">");
//
//			vertex2.put(r).put(u);
//
//			Vertex v = new Vertex(allVertex.size());
//			allVertex.add(v);
//			vertexAdded(v);
//		}
//
//		vertex2.rewind();
//
//		IntBuffer[] r = thisDelaunay.computeTwo(vertex2);
//
//		Face[] ret = new Face[r[0].capacity() / 3];
//		for (int i = 0; i < r[0].capacity() / 3; i++) {
//			;//System.out.println(" adding face <" + voffset + " + " + r[0].get(3 * i) + " " + r[0].get(3 * i + 1) + " " + r[0].get(3 * i + 2) + ">");
//
//			ret[i] = addFaceOverVertices(vertex(voffset + r[0].get(3 * i)), vertex(voffset + r[0].get(3 * i + 1)), vertex(voffset + r[0].get(3 * i + 2)));
//		}
//
//		endOperation();
//		return ret;
//	}
//
//	public Face[] addProjectedDelaunayTriangulation(Vector3[] vertex3, Vector3 rightDirection, Vector3 upDirection) {
//		FloatBuffer t = FloatBuffer.allocate(vertex3.length * 3);
//		for (int i = 0; i < vertex3.length; i++)
//			t.put(vertex3[i].x).put(vertex3[i].y).put(vertex3[i].z);
//
//		t.rewind();
//
//		return addProjectedDelaunayTriangulation(t, rightDirection, upDirection);
//	}
//
//	public Face[] addProjectedDelaunayTriangulationWithExisting(Vertex[] alreadyHere, FloatBuffer vertex3, Vector3 rightDirection, Vector3 upDirection) {
//		beginOperation();
//
//		float dx = rightDirection.x;
//		float dy = rightDirection.y;
//		float dz = rightDirection.z;
//
//		float ux = upDirection.x;
//		float uy = upDirection.y;
//		float uz = upDirection.z;
//
//		if (thisDelaunay == null) thisDelaunay = new Delaunay();
//
//		int num = vertex3.remaining() / 3;
//
//		if ((vertex2 == null) || (vertex2.capacity() < num * 3)) {
//			vertex2 = ByteBuffer.allocateDirect(num * 4 * 3).asFloatBuffer();
//		}
//
//		vertex2.rewind();
//		vertex2.limit(num * 2);
//
//		int voffset = allVertex.size();
//
//		;//System.out.println(" creating <" + num + "> vertex");
//
//		for (int i = 0; i < num; i++) {
//			float fx = vertex3.get();
//			float fy = vertex3.get();
//			float fz = vertex3.get();
//
//			float r = dx * fx + dy * fy + dz * fz;
//			float u = ux * fx + uy * fy + uz * fz;
//
//			;//System.out.println(" coordinates <" + r + "> <" + u + "> from <" + i + ">");
//
//			vertex2.put(r).put(u);
//
//			if (i >= alreadyHere.length) {
//				Vertex v = new Vertex(allVertex.size());
//				allVertex.add(v);
//				vertexAdded(v);
//			}
//		}
//
//		vertex2.rewind();
//
//		IntBuffer[] r = thisDelaunay.computeTwo(vertex2);
//
//		Face[] ret = new Face[r[0].capacity() / 3];
//		for (int i = 0; i < r[0].capacity() / 3; i++) {
//			;//System.out.println(" adding face <" + voffset + " + " + r[0].get(3 * i) + " " + r[0].get(3 * i + 1) + " " + r[0].get(3 * i + 2) + ">");
//
//			ret[i] = addFaceOverVertices(vertex(alreadyHere, voffset, r[0].get(3 * i)), vertex(alreadyHere, voffset, r[0].get(3 * i + 1)), vertex(alreadyHere, voffset, r[0].get(3 * i + 2)));
//		}
//
//		endOperation();
//		return ret;
//	}

	private Vertex vertex(Vertex[] alreadyHere, int voffset, int i) {
		if (i < alreadyHere.length) return alreadyHere[i];
		return vertex(voffset + i - alreadyHere.length);
	}

	// the first alreadyHere.length of vertex3 refer to these vertices.
//	public Face[] addProjectedDelaunayTriangulationWithExisting(Vertex[] alreadyHere, Vector3[] vertex3, Vector3 rightDirection, Vector3 upDirection) {
//		FloatBuffer t = FloatBuffer.allocate(vertex3.length * 3);
//		for (int i = 0; i < vertex3.length; i++)
//			t.put(vertex3[i].x).put(vertex3[i].y).put(vertex3[i].z);
//
//		t.rewind();
//
//		return addProjectedDelaunayTriangulationWithExisting(alreadyHere, t, rightDirection, upDirection);
//	}

	public Face addVertexToMakeNewTriangle(Edge farEdge) {
		beginOperation();

		Face ret = new Face();
		allFace.add(ret);
		faceAdded(ret);

		Vertex newVertex = new Vertex(allVertex.size());
		allVertex.add(newVertex);
		vertexAdded(newVertex);

		ret.edges[0] = farEdge;
		farEdge.faces.add(ret);
		farEdge.nonEdgeVertex.add(newVertex);

		Edge e1 = new Edge();
		e1.start = farEdge.start;
		e1.end = newVertex;
		e1.nonEdgeVertex.add(farEdge.end);
		e1.faces.add(ret);
		allEdge.add(e1);
		edgeAdded(e1);
		ret.edges[1] = e1;

		Edge e2 = new Edge();
		e2.start = farEdge.end;
		e2.end = newVertex;
		e2.nonEdgeVertex.add(farEdge.start);
		e2.faces.add(ret);
		allEdge.add(e2);
		edgeAdded(e2);
		ret.edges[2] = e2;

		farEdge.start.edges.add(e1);
		farEdge.end.edges.add(e2);

		newVertex.edges.add(e1);
		newVertex.edges.add(e2);

		ret.vertex[0] = farEdge.start;
		ret.vertex[1] = farEdge.end;
		ret.vertex[2] = newVertex;

		allVertex_change.dirty();
		allFace_change.dirty();
		allEdge_change.dirty();

		endOperation();

		return ret;
	}

	public void beginOperation() {
		beginCount++;
	}

	public boolean checkInvarients() {
		for (int i = 0; i < allFace.size(); i++) {
			Face f = (Face) allFace.get(i);
			for (int n = 0; n < f.edges.length; n++) {
				Edge e = f.edges[n];
				boolean foundStart = false;
				boolean foundEnd = false;
				for (int t = 0; t < 3; t++) {
					if (f.vertex[t] == e.start) foundStart = true;
					if (f.vertex[t] == e.end) foundEnd = true;
				}
				assert foundStart : Arrays.asList(f.vertex) + " " + e.start + " " + e.end;
				assert foundEnd;
				assert e.faces.contains(f);
				assert allEdge.contains(e);

				HashSet intersection = new HashSet(e.nonEdgeVertex);
				intersection.retainAll(Arrays.asList(f.vertex));
				assert allVertex.containsAll(e.nonEdgeVertex);
				assert intersection.size() == 1 : intersection + " " + e.nonEdgeVertex + " " + Arrays.asList(f.vertex) + " " + Arrays.asList(f.edges) + " " + n;
			}

			for (int n = 0; n < f.bifaces.size(); n++) {
				BiFace b = (BiFace) f.bifaces.get(n);
				assert allBiFace.contains(b);
				assert b.face1 == f || b.face2 == f;
				assert indexOf(b.hiddenEdge, b.face1.edges) != -1 && indexOf(b.hiddenEdge, b.face2.edges) != -1 : b.hiddenEdge + " " + Arrays.asList(b.face1.edges) + " " + Arrays.asList(b.face2.edges);
			}

			// check
			// edges
			// mention
			// different
			// vertex
			// and
			// each
			// vertex
			// mentioned
			// once.
			HashSet firstMention = new HashSet();
			HashSet secondMention = new HashSet();

			for (int n = 0; n < f.edges.length; n++) {
				Edge e = f.edges[n];
				if (!firstMention.contains(e.start))
					firstMention.add(e.start);
				else
					assert secondMention.add(e.start) : "malformed face <" + Arrays.asList(f.edges) + "> <" + Arrays.asList(f.vertex) + ">";
				if (!firstMention.contains(e.end))
					firstMention.add(e.end);
				else
					assert secondMention.add(e.end) : "malformed face <" + Arrays.asList(f.edges) + "> <" + Arrays.asList(f.vertex) + ">";
				assert e.start != e.end : "malformed edge <" + Arrays.asList(f.edges) + "> <" + Arrays.asList(f.vertex) + ">";

				assert indexOf(e.start, f.vertex) != -1 : "malformed face, edge referes to non-face vertex <" + e + "> <" + Arrays.asList(f.vertex) + ">";
				assert indexOf(e.end, f.vertex) != -1 : "malformed face, edge referes to non-face vertex <" + e + "> <" + Arrays.asList(f.vertex) + ">";
			}

			assert firstMention.equals(secondMention) : firstMention + " " + secondMention;
		}

		for (int j = 0; j < allVertex.size(); j++) {
			Vertex v = (Vertex) allVertex.get(j);
			for (int i = 0; i < v.edges.size(); i++) {
				Edge e = (Edge) v.edges.get(i);
				assert e.end == v || e.start == v : v + " " + e;
				assert e.end == v ^ e.start == v : v + " " + e;
				assert allEdge.contains(e);
			}
		}

		for (int i = 0; i < allEdge.size(); i++) {
			Edge e = (Edge) allEdge.get(i);
			for (int j = 0; j < e.faces.size(); j++) {
				Face f = (Face) e.faces.get(j);
				assert allFace.contains(f);
				assert (indexOf(e, f.edges) != -1) : Arrays.asList(f.edges) + " <-" + e + " " + f + " " + System.identityHashCode(f);
			}
		}

		return true;
	}

	public Edge edge(int i) {
		return (Edge) allEdge.get(i);
	}

	protected void edgeAdded(Edge e1) {
		if (notifications.size() == 0) return;
		opcodeList.add(opcode_edgeAdded);
		operandList.add(e1);
	}

	protected void edgeRemoved(Edge e1) {
		if (notifications.size() == 0) return;
		opcodeList.add(opcode_edgeRemoved);
		operandList.add(e1);
	}

	public void endOperation() {

		if (beginCount == 1) {
			resetVertexNumber();
			assert checkInvarients();

			if (notifications.size() == 0) return;

			for (int i = 0; i < notifications.size(); i++)
				((iNotification) notifications.get(i)).beginOperation();

			// BiQuicksort.sort(opcodeList, operandList, opcodeComparator);

			for (int i = 0; i < opcodeList.size(); i++)
				((Opcode) opcodeList.get(i)).fire();
			for (int i = 0; i < notifications.size(); i++)
				((iNotification) notifications.get(i)).endOperation();
			operandList.clear();
			opcodeList.clear();
			operand_index = 0;
		}
		beginCount--;
		assert beginCount >= 0;
	}

	protected void faceAdded(Face f1) {
		if (notifications.size() == 0) return;
		opcodeList.add(opcode_faceAdded);
		operandList.add(f1);
	}

	private float faceArea(int i, int j, int k, FloatBuffer vertex3) {
		_t1.setValue(vertex3.get(3 * i + 0), vertex3.get(3 * i + 1), vertex3.get(3 * i + 2));
		_t2.setValue(vertex3.get(3 * j + 0), vertex3.get(3 * j + 1), vertex3.get(3 * j + 2));
		_t3.setValue(vertex3.get(3 * k + 0), vertex3.get(3 * k + 1), vertex3.get(3 * k + 2));
		_t1.sub(_t1, _t3, _t1);
		_t2.sub(_t2, _t3, _t2);
		_t3.cross(_t1, _t2);
		return _t3.mag();
	}

	protected void faceRemoved(Face f1) {
		if (notifications.size() == 0) return;
		opcodeList.add(opcode_faceRemoved);
		operandList.add(f1);

	}

	public Edge findEdge(Edge[] edges, Vertex vertex, Vertex vertex2) {
		for (int i = 0; i < edges.length; i++) {
			Edge e = edges[i];
			if ((e.start == vertex && e.end == vertex2) || (e.start == vertex2 && e.end == vertex)) return e;
		}
		return null;
	}

	public Edge findEdge(Vertex v2, Vertex v3) {
		return findEdge((Edge[]) v2.edges.toArray(new Edge[v2.edges.size()]), v2, v3);
	}

	public Face getFaceBetweenEdges(Edge e1, Edge e2) {
		for (int i = 0; i < e1.faces.size(); i++) {
			for (int j = 0; j < e2.faces.size(); j++) {
				if (e1.faces.get(i).equals(e2.faces.get(j))) { return (Face) e1.faces.get(i); }
			}
		}
		return null;
	}

	static public int indexOf(Object e, Object[] edges) {
		for (int i = 0; i < edges.length; i++)
			if (edges[i] == e) return i;
		// assert false :
		// Arrays.asList(edges) +" "+e;
		return -1;
	}

	public Object oneNotIn(Object[] v1, Object[] v2) {
		for (int i = 0; i < v1.length; i++) {
			boolean found = false;
			for (int j = 0; j < v2.length; j++) {
				if (v2[j].equals(v1[i])) found = true;
			}
			if (!found) { return v1[i]; }
		}
		assert false : Arrays.asList(v1) + " " + Arrays.asList(v2);
		return null;
	}

	public List openEdgeList() {
		return (List) openEdgeList_mod.data(compute_openEdgeList);
	}

	public Face[] quadSectFace(Face f) {
		Face[] ret = new Face[4];
		beginOperation();

		Vertex[] mid = new Vertex[3];
		for (int i = 0; i < mid.length; i++) {
			mid[i] = new Vertex(allVertex.size());
			;//System.out.println(" new Vertex <" + allVertex.size() + ">");
			allVertex.add(mid[i]);
			vertexAdded(mid[i]);
		}


		Edge e11 = new Edge();
		allEdge.add(e11);
		edgeAdded(e11);
		e11.start = f.edges[0].start;
		e11.end = mid[0];
		mid[0].edges.add(e11);
		f.edges[0].start.edges.add(e11);

		Edge e12 = new Edge();
		allEdge.add(e12);
		edgeAdded(e12);
		e12.end = f.edges[0].end;
		e12.start = mid[0];
		mid[0].edges.add(e12);
		f.edges[0].end.edges.add(e12);

		Edge e21 = new Edge();
		allEdge.add(e21);
		edgeAdded(e21);
		e21.start = f.edges[1].start;
		e21.end = mid[1];
		mid[1].edges.add(e21);
		f.edges[1].start.edges.add(e21);

		Edge e22 = new Edge();
		allEdge.add(e22);
		edgeAdded(e22);
		e22.end = f.edges[1].end;
		e22.start = mid[1];
		mid[1].edges.add(e22);
		f.edges[1].end.edges.add(e22);

		Edge e31 = new Edge();
		allEdge.add(e31);
		edgeAdded(e31);
		e31.start = f.edges[2].start;
		e31.end = mid[2];
		mid[2].edges.add(e31);
		f.edges[2].start.edges.add(e31);

		Edge e32 = new Edge();
		allEdge.add(e32);
		edgeAdded(e32);
		e32.end = f.edges[2].end;
		e32.start = mid[2];
		mid[2].edges.add(e32);
		f.edges[2].end.edges.add(e32);

		Edge e01 = new Edge();
		allEdge.add(e01);
		edgeAdded(e01);
		e01.end = mid[0];
		e01.start = mid[1];
		mid[0].edges.add(e01);
		mid[1].edges.add(e01);

		Edge e02 = new Edge();
		allEdge.add(e02);
		edgeAdded(e02);
		e02.end = mid[0];
		e02.start = mid[2];
		mid[0].edges.add(e02);
		mid[2].edges.add(e02);

		Edge e012 = new Edge();
		allEdge.add(e012);
		edgeAdded(e012);
		e012.end = mid[1];
		e012.start = mid[2];
		mid[1].edges.add(e012);
		mid[2].edges.add(e012);

		ret[0] = addFaceOverEdges(e01, e02, e012);

		// now we have to find three tri
		// from these 9 edges
		boolean[] allocated = new boolean[9];
		Edge[] edges = new Edge[] { e11, e12, e21, e22, e31, e32, e01, e02, e012};

		int num = 0;
		while (num < 3) {
			Edge firstUnallocated = null;
			int at = 0;
			while (firstUnallocated == null) {
				if (allocated[at] == false) {
					allocated[at] = true;
					firstUnallocated = edges[at];
				}
				at++;
			}

			// find
			// some
			// more
			// that
			// share
			// thes
			at = 0;
			List otherTwo = new ArrayList(2);

			while (otherTwo.size() < 2) {
				assert checkInvarients() && at < allocated.length : " failed to find two unallocated edges " + Arrays.asList(edges) + " " + firstUnallocated;
				if (allocated[at] == false) {
					if ((edges[at].start == firstUnallocated.start) || (edges[at].start == firstUnallocated.end)) {
						for (int i = 0; i < edges[at].end.edges.size(); i++) {
							Edge e = (Edge) edges[at].end.edges.get(i);
							if (e != edges[at]) {
								if ((e.start == firstUnallocated.start) || (e.end == firstUnallocated.end) || (e.start == firstUnallocated.end) || (e.end == firstUnallocated.start)) if (indexOf(e, edges) != -1) {
									otherTwo.add(edges[at]);
									otherTwo.add(e);
									allocated[at] = true;
									allocated[indexOf(e, edges)] = true;
									break;
								}
							}
						}
					}
					if ((edges[at].end == firstUnallocated.start) || (edges[at].end == firstUnallocated.end)) {
						for (int i = 0; i < edges[at].start.edges.size(); i++) {
							Edge e = (Edge) edges[at].start.edges.get(i);
							if (e != edges[at]) {
								if ((e.start == firstUnallocated.start) || (e.end == firstUnallocated.end) || (e.start == firstUnallocated.end) || (e.end == firstUnallocated.start)) if (indexOf(e, edges) != -1) {
									otherTwo.add(edges[at]);
									otherTwo.add(e);
									allocated[at] = true;
									allocated[indexOf(e, edges)] = true;
									break;
								}
							}
						}
					}
				}
				at++;
			}

			ret[num + 1] = addFaceOverEdges(firstUnallocated, (Edge) otherTwo.get(0), (Edge) otherTwo.get(1));
			num++;
		}

		removeOnlyFace(f);

		System.err.println(" -- ending operation ");
		endOperation();
		System.err.println(" -- ending operation complete");

		return ret;
	}

	protected void removeDegenerateFaces(List possiblyDegenerate) {
		beginOperation();

		for (int i = 0; i < possiblyDegenerate.size(); i++) {
			Face f = (Face) possiblyDegenerate.get(i);
			if ((f.vertex[0] == f.vertex[1]) || (f.vertex[1] == f.vertex[2]) || (f.vertex[0] == f.vertex[2])) {
				removeOnlyFace(f);
			} else {
			}

			// check
			// for
			// degenerate
			// edges,
			// does
			// an
			// edge
			// appear
			// twice
			// on a
			// vertex?
			for (int n = 0; n < 3; n++) {
				HashMap s = new HashMap();
				for (int e = 0; e < f.vertex[n].edges.size(); e++) {
					Edge ed = (Edge) f.vertex[n].edges.get(e);
					if (s.containsKey(ed)) {
						// we
						// need
						// to
						// remove
						// this
						// edge,
						// replacing
						// it
						// with
						// what
						// is
						// already
						// in
						// the
						// hashset
						Edge already = (Edge) s.get(ed);


						replaceEdge(ed, already);
					} else
						s.put(ed, ed);
				}
			}
		}

		endOperation();
	}

	private void replaceEdge(Edge going, Edge with) {

		assert going.equals(with);

		List possiblyDegenerate = new ArrayList();

		for (int i = 0; i < going.faces.size(); i++) {
			Face f = (Face) going.faces.get(i);


			boolean once = false;
			for (int n = 0; n < f.edges.length; n++) {
				if (f.edges[n] == going) {
					f.edges[n] = with;
					with.faces.add(f);
					assert !once;
					once = true;
				} else if (f.edges[n] == with) {
					possiblyDegenerate.add(f);
				}
			}
			assert once;
		}

		assert going.start.edges.contains(going);
		going.start.edges.remove(going);
		if (!going.start.edges.contains(with)) going.start.edges.add(with);

		assert going.end.edges.contains(going);
		going.end.edges.remove(going);
		if (!going.end.edges.contains(with)) going.end.edges.add(with);

		allEdge.remove(going);
		allEdge.remove(with);
		allEdge.add(with);
		edgeRemoved(going);

		with.nonEdgeVertex.addAll(going.nonEdgeVertex);

		removeDegenerateFaces(possiblyDegenerate);
	}

	public void removeNotification(iNotification not) {
		notifications.remove(not);
	}

	public void removeOnlyFace(Face face) {

		beginOperation();
		faceRemoved(face);

		HashMap removeCount = new HashMap();

		for (int i = 0; i < face.edges.length; i++) {
			if (face.edges[i].faces.size() == 1) {
				edgeRemoved(face.edges[i]);
				int rc = 0;

				Integer in = ((Integer) removeCount.get(face.edges[i].start.edges));
				if (in != null) rc = in.intValue();

				if (face.edges[i].start.edges.size() - rc == 1) {
					vertexRemoved(face.edges[i].start);
				} else
					removeCount.put(face.edges[i].start.edges, new Integer(rc++));

				in = ((Integer) removeCount.get(face.edges[i].end.edges));
				if (in != null) rc = in.intValue();
				if (face.edges[i].end.edges.size() - rc == 1) {
					vertexRemoved(face.edges[i].end);
				} else
					removeCount.put(face.edges[i].end.edges, new Integer(rc++));
			}
		}

		for (int i = 0; i < face.bifaces.size(); i++) {
			BiFace bi = (BiFace) face.bifaces.get(i);
			biFaceRemoved(bi);
		}

		endOperation();

		allFace.remove(face);

		for (int i = 0; i < face.edges.length; i++) {
			if (face.edges[i].faces.size() == 1) {
				allEdge.remove(face.edges[i]);
				if (face.edges[i].start.edges.size() == 1) {
					allVertex.remove(face.edges[i].start);

					Vertex v = face.edges[i].start;
					for (int n = 0; i < v.edges.size(); i++) {
						Edge ee = (Edge) v.edges.get(n);
						for (int m = 0; m < ee.faces.size(); m++) {
							Face ff = (Face) ee.faces.get(m);
							for (int q = 0; q < ff.edges.length; q++) {
								ff.edges[q].nonEdgeVertex.remove(v);
							}
						}
					}
				} else
					face.edges[i].start.edges.remove(face.edges[i]);
				if (face.edges[i].end.edges.size() == 1) {
					allVertex.remove(face.edges[i].end);
					Vertex v = face.edges[i].end;
					for (int n = 0; i < v.edges.size(); i++) {
						Edge ee = (Edge) v.edges.get(n);
						for (int m = 0; m < ee.faces.size(); m++) {
							Face ff = (Face) ee.faces.get(m);
							for (int q = 0; q < ff.edges.length; q++) {
								ff.edges[q].nonEdgeVertex.remove(v);
							}
						}
					}
				} else
					face.edges[i].end.edges.remove(face.edges[i]);
			} else {
				face.edges[i].faces.remove(face);
			}
		}

		for (int i = 0; i < face.bifaces.size(); i++) {
			BiFace bi = (BiFace) face.bifaces.get(i);
			allBiFace.remove(bi);

			bi.face1.bifaces.remove(bi);
			bi.face2.bifaces.remove(bi);
		}

		resetVertexNumber();

		allVertex_change.dirty();
		allFace_change.dirty();
		allEdge_change.dirty();
	}

	public void replaceVerticesWithVertex(List vertices, Vertex replacement) {
		beginOperation();
		for (int i = 0; i < vertices.size(); i++) {
			Vertex v = (Vertex) vertices.get(i);
			vertexRemoved(v);
		}

		endOperation();

		List possiblyDegenerateFaces = new ArrayList();
		// resetVertexNumber();

		// any faces that are shared
		// between more than one of
		// (vertices and replacement) are
		// to be removed \u2014 they are
		// degenerate
		// and faces that are shared by
		// only one need to be merged
		// we'll do this is a number of
		// steps, the intermediate ones
		// will have degenerate faces
		for (int i = 0; i < vertices.size(); i++) {
			Vertex v = (Vertex) vertices.get(i);
			allVertex.remove(v);
			for (int j = 0; j < v.edges.size(); j++) {
				Edge e = (Edge) v.edges.get(j);
				if (e.start == v)
				// if
				// (e.end!=replacement)
				{
					for (int k = 0; k < e.faces.size(); k++) {
						Face f = (Face) e.faces.get(k);
						for (int h = 0; h < 3; h++) {
							if (f.vertex[h] == e.start) {
								f.vertex[h] = replacement;
							}
							if (f.edges[h].nonEdgeVertex.removeAll(vertices)) {
								if (!f.edges[h].nonEdgeVertex.contains(replacement)) f.edges[h].nonEdgeVertex.add(replacement);
							}
						}
						possiblyDegenerateFaces.add(f);
					}
					e.start = replacement;
					replacement.edges.add(e);
				}
				if (e.end == v)
				// if
				// (e.start!=replacement)
				{
					for (int k = 0; k < e.faces.size(); k++) {
						Face f = (Face) e.faces.get(k);
						for (int h = 0; h < 3; h++) {
							if (f.vertex[h] == e.end) {
								f.vertex[h] = replacement;
							}
							if (f.edges[h].nonEdgeVertex.removeAll(vertices)) {
								if (!f.edges[h].nonEdgeVertex.contains(replacement)) f.edges[h].nonEdgeVertex.add(replacement);
							}
						}
						possiblyDegenerateFaces.add(f);
					}
					e.end = replacement;
					replacement.edges.add(e);
				}
				boolean had = false;
				for (int n = 0; n < e.nonEdgeVertex.size(); n++) {
					if (vertices.contains(e.nonEdgeVertex.get(n))) {
						had = true;
						e.nonEdgeVertex.remove(n);
						n--;
					}
				}
				if (had && !e.nonEdgeVertex.contains(replacement)) e.nonEdgeVertex.add(replacement);
				e.nonEdgeVertex.remove(e.start);
				e.nonEdgeVertex.remove(e.end);

			}
		}

		resetVertexNumber();

		removeDegenerateFaces(possiblyDegenerateFaces);

	}

	public void resetVertexNumber() {
		// if (true) return;
		for (int i = 0; i < allVertex.size(); i++) {
			((Vertex) allVertex.get(i)).vertexNumber = i;
		}
		for (int i = 0; i < allFace.size(); i++) {
			((Face) allFace.get(i)).faceNumber = i;
		}
		for (int i = 0; i < allEdge.size(); i++) {
			((Edge) allEdge.get(i)).edgeNumber = i;
		}
	}

	public FloatBuffer returnVertex2FromDelaunayTriangulation() {
		return vertex2;
	}

	public String simpleDescription() {
		return "<" + allFace.size() + "> face <" + allEdge.size() + "> edge <" + allVertex.size() + "> vertex";
	}

	public Face[] triSectFace(Face f) {
		Face[] ret = new Face[3];
		beginOperation();

		Vertex mid = new Vertex(allVertex.size());
		allVertex.add(mid);
		vertexAdded(mid);

		Edge e0 = new Edge();
		allEdge.add(e0);
		edgeAdded(e0);
		e0.start = f.vertex[0];
		e0.end = mid;
		mid.edges.add(e0);
		f.vertex[0].edges.add(e0);

		Edge e1 = new Edge();
		allEdge.add(e1);
		edgeAdded(e1);
		e1.start = f.vertex[1];
		e1.end = mid;
		mid.edges.add(e1);
		f.vertex[1].edges.add(e1);

		Edge e2 = new Edge();
		allEdge.add(e2);
		edgeAdded(e2);
		e2.start = f.vertex[2];
		e2.end = mid;
		mid.edges.add(e2);
		f.vertex[2].edges.add(e2);

		ret[0] = addFaceOverEdges(e0, e1, findEdge(f.edges, f.vertex[0], f.vertex[1]));
		ret[1] = addFaceOverEdges(e0, e2, findEdge(f.edges, f.vertex[0], f.vertex[2]));
		ret[2] = addFaceOverEdges(e1, e2, findEdge(f.edges, f.vertex[1], f.vertex[2]));

		removeOnlyFace(f);

		endOperation();

		return ret;
	}

	public BiFace newBiFace(Face f1, Face f2) {
		// find common edge
		HashSet intersection = new HashSet();
		intersection.addAll(Arrays.asList(f1.edges));
		intersection.retainAll(Arrays.asList(f2.edges));

		assert intersection.size() == 1;

		if (intersection.size() == 0) return null;

		Edge e = (Edge) (intersection.iterator().next());

		BiFace bi = new BiFace();
		bi.face1 = f1;
		bi.face2 = f2;
		bi.hiddenEdge = e;
		f1.bifaces.add(bi);
		f2.bifaces.add(bi);

		beginOperation();

		allBiFace.add(bi);
		biFaceAdded(bi);

		endOperation();

		return bi;
	}

	public Vertex vertex(int v1) {
		return (Vertex) allVertex.get(v1);
	}

	protected void vertexAdded(Vertex v1) {
		if (notifications.size() == 0) return;
		opcodeList.add(opcode_vertexAdded);
		operandList.add(v1);
	}

	protected void vertexRemoved(Vertex v1) {
		if (notifications.size() == 0) return;
		opcodeList.add(opcode_vertexRemoved);
		operandList.add(v1);
	}

	protected void biFaceAdded(BiFace v1) {
		if (notifications.size() == 0) return;
		opcodeList.add(opcode_biFaceAdded);
		operandList.add(v1);
	}

	protected void biFaceRemoved(BiFace v1) {
		if (notifications.size() == 0) return;
		opcodeList.add(opcode_biFaceRemoved);
		operandList.add(v1);
	}

	public CompleteEdgeRep2 duplicate(CompleteEdgeRep2 ret) {
		ret.beginOperation();

		HashMap vertexDuplicated = new HashMap();
		HashMap edgeDuplicated = new HashMap();
		HashMap faceDuplicated = new HashMap();
		HashMap biFaceDuplicated = new HashMap();

		int vo = ret.allVertex.size();
		for (int i = 0; i < allVertex.size(); i++) {
			Vertex f = (Vertex) allVertex.get(i);
			Vertex d = new Vertex(f.vertexNumber + vo);
			d.uniqeNumber = f.uniqeNumber;
			ret.allVertex.add(d);
			vertexDuplicated.put(f, d);
			ret.vertexAdded(d);
		}

		for (int i = 0; i < allEdge.size(); i++) {
			Edge f = (Edge) allEdge.get(i);
			Edge d = new Edge();
			d.uniqeNumber = f.uniqeNumber;
			ret.allEdge.add(d);
			edgeDuplicated.put(f, d);
			ret.edgeAdded(d);
		}

		int fo = ret.allFace.size();

		for (int i = 0; i < allFace.size(); i++) {
			Face f = (Face) allFace.get(i);
			Face d = new Face();
			d.uniqeNumber = f.uniqeNumber;
			d.faceNumber = f.faceNumber + fo;
			ret.allFace.add(d);
			faceDuplicated.put(f, d);
			ret.faceAdded(d);
		}

		for (int i = 0; i < allBiFace.size(); i++) {
			BiFace f = (BiFace) allBiFace.get(i);
			BiFace d = new BiFace();
			d.uniqeNumber = f.uniqeNumber;
			ret.allBiFace.add(d);
			biFaceDuplicated.put(f, d);
			ret.biFaceAdded(d);
		}

		for (int i = 0; i < allVertex.size(); i++) {
			Vertex f = (Vertex) allVertex.get(i);
			Vertex q = (Vertex) vertexDuplicated.get(f);
			for (int n = 0; n < f.edges.size(); n++) {
				q.edges.add((Edge) edgeDuplicated.get(f.edges.get(n)));
			}
		}

		for (int i = 0; i < allEdge.size(); i++) {
			Edge f = (Edge) allEdge.get(i);
			Edge q = (Edge) edgeDuplicated.get(f);
			for (int n = 0; n < f.nonEdgeVertex.size(); n++)
				q.nonEdgeVertex.add((Vertex) vertexDuplicated.get(f.nonEdgeVertex.get(n)));
			q.start = (Vertex) vertexDuplicated.get(f.start);
			q.end = (Vertex) vertexDuplicated.get(f.end);
			for (int n = 0; n < f.faces.size(); n++)
				q.faces.add((Face) faceDuplicated.get(f.faces.get(n)));
		}

		for (int i = 0; i < allFace.size(); i++) {
			Face f = (Face) allFace.get(i);
			Face q = (Face) faceDuplicated.get(f);
			for (int n = 0; n < f.bifaces.size(); n++)
				q.bifaces.add(biFaceDuplicated.get(f.bifaces.get(n)));
			for (int n = 0; n < f.edges.length; n++)
				q.edges[n] = (Edge) edgeDuplicated.get(f.edges[n]);
			for (int n = 0; n < f.vertex.length; n++)
				q.vertex[n] = (Vertex) vertexDuplicated.get(f.vertex[n]);
		}

		for (int i = 0; i < allBiFace.size(); i++) {
			BiFace f = (BiFace) allBiFace.get(i);
			BiFace q = (BiFace) biFaceDuplicated.get(f);
			q.face1 = (Face) faceDuplicated.get(f.face1);
			q.face2 = (Face) faceDuplicated.get(f.face2);
			q.hiddenEdge = (Edge) edgeDuplicated.get(f.hiddenEdge);
		}

		ret.endOperation();

		return ret;
	}

	public void addBiFaceOverFaces(Face f, Face f2, Edge longest2) {
		beginOperation();
		assert findEdge(f.edges, longest2.start, longest2.end) != null;
		assert findEdge(f2.edges, longest2.start, longest2.end) != null;

		BiFace newBiFace = new BiFace();
		newBiFace.face1 = f;
		newBiFace.face2 = f2;
		newBiFace.hiddenEdge = longest2;
		allBiFace.add(newBiFace);
		biFaceAdded(newBiFace);

		endOperation();
	}

	public void save(String filename) {
		try {
			ObjectOutputStream oos;
			oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(filename))));
			oos.writeObject(allVertex);
			oos.writeObject(allEdge);
			oos.writeObject(allFace);
			oos.writeObject(allBiFace);
			oos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static CompleteEdgeRep2 load(String filename) {
		try {
			ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(filename))));
			CompleteEdgeRep2 r = new CompleteEdgeRep2();
			r.allVertex = (ArrayList) ois.readObject();
			r.allEdge = (ArrayList) ois.readObject();
			r.allFace = (ArrayList) ois.readObject();
			r.allBiFace = (ArrayList) ois.readObject();
			ois.close();
			return r;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		opcodeComparator = new Comparator(){
			public int compare(Object o1, Object o2) {
				return ((Opcode) o1).priority < ((Opcode) o2).priority ? 1 : (((Opcode) o1).priority == ((Opcode) o2).priority ? 0 : -1);
			}
		};
	}

	// not fast... not tested...
	public void removeVertex(Vertex v) {
		beginOperation();
		allVertex.remove(v);
		vertexRemoved(v);

		for (int i = 0; i < v.edges.size(); i++) {
			Edge e = (Edge) v.edges.get(i);
			if (allEdge.remove(e)) {
				edgeRemoved(e);
				if (e.start != v) e.start.edges.remove(e);
				if (e.end != v) e.end.edges.remove(e);
				for (int j = 0; j < e.faces.size(); j++) {
					Face f = (Face) e.faces.get(j);
					if (allFace.remove(f)) faceRemoved(f);
				}
			}
		}

		endOperation();
	}
}
