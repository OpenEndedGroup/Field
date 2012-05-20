package field.graphics.core;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import field.core.util.FieldPyObjectAdaptor.iExtensible;
import field.graphics.core.BasicGeometry.LineList_long;
import field.graphics.core.BasicGeometry.TriangleMesh_long;
import field.graphics.core.BasicGeometry.VertexBuffer;
import field.util.Dict;
import field.util.PythonUtils;

/**
 * tools for creating mesh blast files
 * 
 * @author marc
 * 
 */
public class SavedMeshSet {

	private final String rootFilename;

	public SavedMeshSet(String rootFilename) {
		this.rootFilename = rootFilename;
	}

	static public class Frame implements iExtensible, Serializable {

		Dict info = new Dict();

		int numMesh = 0;

		transient List<Mesh> meshes = new ArrayList();

		public Dict getDict() {
			return info;
		}
	}

	List<Frame> frame = new ArrayList<Frame>();

	int allocated = 0;

	public Frame addFrameWithAux(List<TriangleMesh_long> from) {
		Frame f = new Frame();
		f.numMesh = from.size();

		for (TriangleMesh_long g : from) {
			Mesh m = new Mesh();

			if (g instanceof LineList_long)
				((LineList_long) g).checkLine();
			m.numVertex = g.numVertex();
			m.numTriangle = g.numTriangle();
			m.primativeSize = g instanceof LineList_long ? 2 : 3;
			ByteBuffer v2 = ByteBuffer.allocate(4 * 3 * m.numVertex).order(ByteOrder.nativeOrder());
			ByteBuffer v = g.vertexBuffer.bBuffer;
			v.position(0);
			v.limit(4 * 3 * m.numVertex);
			v2.put(v);
			v2.rewind();
			m.vertex = v2;

			Map<Integer, VertexBuffer> a = (Map<Integer, VertexBuffer>) g.auxBuffers();
			for (Map.Entry<Integer, VertexBuffer> e : a.entrySet()) {
				if (e.getKey() <= 0)
					continue;

				int len = 4; // e.getValue().buffer.limit() /
				// m.numVertex;

				ByteBuffer a2 = ByteBuffer.allocate(4 * len * m.numVertex).order(ByteOrder.nativeOrder());
				ByteBuffer av = g.auxBuffers.get(e.getKey()).bBuffer;
				av.limit(4 * len * m.numVertex);
				av.position(0);
				a2.put(av);
				a2.rewind();
				m.aux.put(e.getKey(), a2);
			}

			ByteBuffer t2 = ByteBuffer.allocate(4 * m.primativeSize * m.numTriangle).order(ByteOrder.nativeOrder());
			ByteBuffer t = g.triangleBuffer.bBuffer;
			t.limit(m.primativeSize * m.numTriangle * 4);
			t.position(0);
			t2.put(t);
			t2.rewind();
			m.triangle = t2;

			allocated += m.primativeSize * m.numTriangle * 4;
			allocated += 4 * 3 * m.numVertex;

			f.meshes.add(m);
		}

		frame.add(f);
		return f;
	}

	public Frame addFrameWithAux(PointList g) {
		Frame f = new Frame();
		f.numMesh = 1;

		Mesh m = new Mesh();
		m.numVertex = g.numVertex();
		m.numTriangle = g.numTriangle();
		m.primativeSize = 1;
		ByteBuffer v2 = ByteBuffer.allocate(4 * 3 * m.numVertex).order(ByteOrder.nativeOrder());
		ByteBuffer v = g.vertexBuffer.bBuffer;
		v.position(0);
		v.limit(4 * 3 * m.numVertex);
		v2.put(v);
		v2.rewind();
		m.vertex = v2;

		Map<Integer, VertexBuffer> a = (Map<Integer, VertexBuffer>) g.auxBuffers();
		for (Map.Entry<Integer, VertexBuffer> e : a.entrySet()) {
			if (e.getKey() <= 0)
				continue;

			int len = 4; // e.getValue().buffer.limit() /
			// m.numVertex;

			ByteBuffer a2 = ByteBuffer.allocate(4 * len * m.numVertex).order(ByteOrder.nativeOrder());
			ByteBuffer av = g.auxBuffers.get(e.getKey()).bBuffer;
			av.limit(4 * len * m.numVertex);
			av.position(0);
			a2.put(av);
			a2.rewind();
			m.aux.put(e.getKey(), a2);
			allocated += len * m.numVertex* 4;
		}

		allocated += 4 * 3 * m.numVertex;

		f.meshes.add(m);

		frame.add(f);
		return f;
	}

	public Frame addFrameWithAuxNoTopology(LineList_long g) {
		Frame f = new Frame();
		f.numMesh = 1;

		Mesh m = new Mesh();
		m.numVertex = g.numVertex();
		m.numTriangle = g.numTriangle();
		m.primativeSize = 1;
		ByteBuffer v2 = ByteBuffer.allocate(4 * 3 * m.numVertex).order(ByteOrder.nativeOrder());
		ByteBuffer v = g.vertexBuffer.bBuffer;
		v.position(0);
		v.limit(4 * 3 * m.numVertex);
		v2.put(v);
		v2.rewind();
		m.vertex = v2;

		Map<Integer, VertexBuffer> a = (Map<Integer, VertexBuffer>) g.auxBuffers();
		for (Map.Entry<Integer, VertexBuffer> e : a.entrySet()) {
			if (e.getKey() <= 0)
				continue;

			int len = 4; // e.getValue().buffer.limit() /
			// m.numVertex;

			ByteBuffer a2 = ByteBuffer.allocate(4 * len * m.numVertex).order(ByteOrder.nativeOrder());
			ByteBuffer av = g.auxBuffers.get(e.getKey()).bBuffer;
			av.limit(4 * len * m.numVertex);
			av.position(0);
			a2.put(av);
			a2.rewind();
			m.aux.put(e.getKey(), a2);

			allocated += 4 * 4 * m.numVertex;
		}

		allocated += 4 * 3 * m.numVertex;

		f.meshes.add(m);

		frame.add(f);
		return f;
	}

	public Frame addFrame(List<TriangleMesh_long> from) {
		Frame f = new Frame();
		f.numMesh = from.size();

		for (TriangleMesh_long g : from) {
			Mesh m = new Mesh();
			m.numVertex = g.numVertex();
			m.numTriangle = g.numTriangle();
			m.primativeSize = g instanceof LineList_long ? 2 : 3;
			ByteBuffer v2 = ByteBuffer.allocate(4 * 3 * m.numVertex).order(ByteOrder.nativeOrder());
			ByteBuffer v = g.vertexBuffer.bBuffer;
			v.position(0);
			v.limit(4 * 3 * m.numVertex);
			v2.put(v);
			v2.rewind();
			m.vertex = v2;

			// Map<Integer, VertexBuffer> a = (Map<Integer,
			// VertexBuffer>) g.auxBuffers();
			// for (Map.Entry<Integer, VertexBuffer> e :
			// a.entrySet()) {
			// if (e.getKey() <= 0)
			// continue;
			//
			// int len = e.getValue().buffer.capacity() /
			// (v.capacity() / 3);
			//
			// ByteBuffer a2 = ByteBuffer.allocate(4 * len *
			// m.numVertex).order(ByteOrder.nativeOrder());
			// ByteBuffer av = g.auxBuffers.get(e.getKey()).bBuffer;
			// av.limit(4 * len * m.numVertex);
			// a2.put(av);
			// a2.rewind();
			// m.aux.put(e.getKey(), a2);
			// }

			ByteBuffer t2 = ByteBuffer.allocate(4 * m.primativeSize * m.numTriangle).order(ByteOrder.nativeOrder());
			ByteBuffer t = g.triangleBuffer.bBuffer;
			t.limit(m.primativeSize * m.numTriangle * 4);
			t.position(0);
			t2.put(t);
			t2.rewind();
			m.triangle = t2;

			allocated += m.primativeSize * m.numTriangle * 4;
			allocated += 4 * 3 * m.numVertex;

			f.meshes.add(m);
		}

		frame.add(f);
		return f;
	}

	public void save() throws IOException {
		new PythonUtils().persistAsXML(frame, rootFilename + ".index");
		for (int i = 0; i < frame.get(0).numMesh; i++) {
			MeshBlast blast = new MeshBlast(rootFilename + "." + i + ".mesh");

			int numVertex = 0;
			int numTriangle = 0;

			for (int q = 0; q < frame.size(); q++) {
				Mesh m = frame.get(q).meshes.get(i);
				if (m.numTriangle > numTriangle)
					numTriangle = m.numTriangle;
				if (m.numVertex > numVertex)
					numVertex = m.numVertex;
			}

			;//System.out.println(" mesh has max <" + numVertex + "> <" + numTriangle + "> prim <" + frame.get(0).meshes.get(i).primativeSize);
			blast.configureChannel(0, 3, numVertex * 3);
			blast.configureChannel(-1, frame.get(0).meshes.get(i).primativeSize, numTriangle * frame.get(0).meshes.get(i).primativeSize);
			// for (Map.Entry<Integer, ByteBuffer> e :
			// frame.get(0).meshes.get(i).aux.entrySet()) {
			// blast.configureChannel(e.getKey(),
			// e.getValue().capacity() /
			// (frame.get(0).meshes.get(i).numVertex / 3), numVertex
			// * e.getValue().capacity() /
			// (frame.get(0).meshes.get(i).numVertex / 3));
			// }

			for (int q = 0; q < frame.size(); q++) {
				Mesh m = frame.get(q).meshes.get(i);
				blast.emit(0, m.vertex, m.numVertex * 3);
				blast.emit(-1, m.triangle, m.numTriangle * m.primativeSize);
				// for (Map.Entry<Integer, ByteBuffer> e :
				// m.aux.entrySet()) {
				//
				// blast.emit(e.getKey(), e.getValue(),
				// e.getValue().capacity() / 4);
				// }
				blast.header.numFrames++;
			}

			blast.close();
		}
	}

	public void saveWithAux() throws IOException {
		new PythonUtils().persistAsXML(frame, rootFilename + ".index");
		for (int i = 0; i < frame.get(0).numMesh; i++) {
			MeshBlast blast = new MeshBlast(rootFilename + "." + i + ".mesh");

			int numVertex = 0;
			int numTriangle = 0;

			HashMap<Integer, Integer> elementSizes = new HashMap<Integer, Integer>();

			for (int q = 0; q < frame.size(); q++) {
				Mesh m = frame.get(q).meshes.get(i);
				if (m.numTriangle > numTriangle)
					numTriangle = m.numTriangle;
				if (m.numVertex > numVertex)
					numVertex = m.numVertex;
			}

			;//System.out.println(" mesh has max <" + numVertex + "> <" + numTriangle + "> prim <" + frame.get(0).meshes.get(i).primativeSize);
			;//System.out.println(" configuring channels");
			blast.configureChannel(0, 3, numVertex * 3);
			blast.configureChannel(-1, frame.get(0).meshes.get(i).primativeSize, numTriangle * frame.get(0).meshes.get(i).primativeSize);
			for (Map.Entry<Integer, ByteBuffer> e : frame.get(0).meshes.get(i).aux.entrySet()) {
				int stride = (e.getValue().limit() / (frame.get(0).meshes.get(i).numVertex)) / 4;

				stride = 4;

				;//System.out.println(e.getKey() + " -> " + " stride is " + stride + " max length " + (numVertex * stride));
				blast.configureChannel(e.getKey(), stride, numVertex * stride);
			}

			for (int q = 0; q < frame.size(); q++) {
				Mesh m = frame.get(q).meshes.get(i);
				;//System.out.println(" frame <" + q + "> <" + m.numVertex + "> <" + m.numTriangle + ">");
				blast.emit(0, m.vertex, m.numVertex * 3);
				blast.emit(-1, m.triangle, m.numTriangle * m.primativeSize);
				for (Map.Entry<Integer, ByteBuffer> e : m.aux.entrySet()) {
					int stride = (e.getValue().limit() / (frame.get(0).meshes.get(i).numVertex)) / 4;

					stride = 4;

					;//System.out.println(" entry <" + e + "> <" + m.numVertex + " " + stride + "> = " + (m.numVertex * stride) + " <" + e.getValue().limit() / 4.0 + ">");
					blast.emit(e.getKey(), e.getValue(), m.numVertex * stride);
				}
				blast.header.numFrames++;
			}

			blast.close();
		}
	}

	public void saveWithAuxNoTopology() throws IOException {
		new PythonUtils().persistAsXML(frame, rootFilename + ".index");
		for (int i = 0; i < frame.get(0).numMesh; i++) {
			MeshBlast blast = new MeshBlast(rootFilename + "." + i + ".mesh");

			int numVertex = 0;

			HashMap<Integer, Integer> elementSizes = new HashMap<Integer, Integer>();

			for (int q = 0; q < frame.size(); q++) {
				Mesh m = frame.get(q).meshes.get(i);
				if (m.numVertex > numVertex)
					numVertex = m.numVertex;
			}

			;//System.out.println(" mesh has max <" + numVertex + "> prim <" + frame.get(0).meshes.get(i).primativeSize);
			;//System.out.println(" configuring channels");
			blast.configureChannel(0, 3, numVertex * 3);
			for (Map.Entry<Integer, ByteBuffer> e : frame.get(0).meshes.get(i).aux.entrySet()) {
				int stride = (e.getValue().limit() / (frame.get(0).meshes.get(i).numVertex)) / 4;

				stride = 4;

				;//System.out.println(e.getKey() + " -> " + " stride is " + stride + " max length " + (numVertex * stride));
				blast.configureChannel(e.getKey(), stride, numVertex * stride);
			}

			for (int q = 0; q < frame.size(); q++) {
				Mesh m = frame.get(q).meshes.get(i);
				;//System.out.println(" frame <" + q + "> <" + m.numVertex + "> <" + m.numTriangle + ">");
				blast.emit(0, m.vertex, m.numVertex * 3);
				for (Map.Entry<Integer, ByteBuffer> e : m.aux.entrySet()) {
					int stride = (e.getValue().limit() / (frame.get(0).meshes.get(i).numVertex)) / 4;

					stride = 4;

					;//System.out.println(" entry <" + e + "> <" + m.numVertex + " " + stride + "> = " + (m.numVertex * stride) + " <" + e.getValue().limit() / 4.0 + ">");
					blast.emit(e.getKey(), e.getValue(), m.numVertex * stride);
				}
				blast.header.numFrames++;
			}

			blast.close();
		}
	}

	static public class Mesh {
		int numVertex, numTriangle, primativeSize;
		ByteBuffer vertex;
		ByteBuffer triangle;
		HashMap<Integer, ByteBuffer> aux = new LinkedHashMap<Integer, ByteBuffer>();
	}

}
