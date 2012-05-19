package field.graphics.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import field.graphics.core.Base.iLongGeometry;
import field.graphics.core.BasicGeometry.LineList_long;
import field.graphics.core.BasicGeometry.TriangleMesh_long;
import field.graphics.core.BasicGeometry.VertexBuffer;
import field.util.HashMapOfLists;
import field.util.PythonUtils;

/**
 * a compressed file format for fast playback, only does _long forms
 * 
 * @author marc
 * 
 */
public class MeshBlast {

	static public class Frame {
		public long offset;
		public long length;
	}

	static public class Header implements Serializable {
		private static final long serialVersionUID = 1L;
		public HashMapOfLists<Integer, Frame> frames = new HashMapOfLists<Integer, Frame>();
		public int numFrames = 0;
		public HashMap<Integer, Integer> maximumDimensions = new HashMap<Integer, Integer>();
		public HashMap<Integer, Integer> strides = new HashMap<Integer, Integer>();
	}

	Header header = new Header();
	private FileChannel channel;
	private final String filename;

	public MeshBlast(String filename) throws FileNotFoundException {
		this.filename = filename;
		channel = new RandomAccessFile(filename, "rw").getChannel();
	}

	/**
	 * size in Float/Integer's
	 */
	public void configureChannel(int channel, int stride, int maximumSize) {
		header.strides.put(channel, stride);
		header.maximumDimensions.put(channel, maximumSize);
	}

	public void configureChannel(PointList from, int numVertex) {
		configureChannel(0,3, numVertex*3);
		configureChannel(-1, 3, 0);

		Map<Integer, VertexBuffer> max = from.auxBuffers();
		for (Map.Entry<Integer, VertexBuffer> e : max.entrySet()) {
			if (e.getKey() > 0)
				configureChannel(e.getKey(), e.getValue().elementSize, e.getValue().elementSize*numVertex);
		}
	}
	
	public void addGeometry(PointList mesh) throws IOException
	{
		emit(0, mesh.vertexBuffer.bBuffer, mesh.numVertex() * 3);
		
		Set<Entry<Integer, VertexBuffer>> ee = mesh.auxBuffers.entrySet();
		for (Entry<Integer, VertexBuffer> e : ee) {
			if (e.getKey() > 0)
				emit(e.getKey(), e.getValue().bBuffer, mesh.numVertex() * e.getValue().elementSize);
		}
		header.numFrames++;
		new PythonUtils().persistAsXML(header, filename+".xmlheader");
	}
	
	public void configureChannel(iLongGeometry from, int numVertex, int numTriangle, int primativeSize) {
		configureChannel(0,3, numVertex*3);
		configureChannel(-1, primativeSize, numTriangle*primativeSize);

		Map<Integer, VertexBuffer> max = from.auxBuffers();
		for (Map.Entry<Integer, VertexBuffer> e : max.entrySet()) {
			if (e.getKey() > 0)
				configureChannel(e.getKey(), e.getValue().elementSize, e.getValue().elementSize*numVertex);
		}
	}
	
	public void configureChannel(iLongGeometry from) {
		configureChannel(0, 3, from.numVertex() * 3);
		configureChannel(-1, from.longTriangle().limit() / from.numTriangle(), from.longTriangle().limit());

		Map<Integer, VertexBuffer> max = from.auxBuffers();
		for (Map.Entry<Integer, VertexBuffer> e : max.entrySet()) {
			if (e.getKey() > 0)
				configureChannel(e.getKey(), e.getValue().elementSize, e.getValue().buffer.limit());
		}
	}

	public void addGeometry(TriangleMesh_long mesh) throws IOException {
		emit(0, mesh.vertexBuffer.bBuffer, mesh.numVertex() * 3);
		emit(-1, mesh.triangleBuffer.bBuffer, mesh.numTriangle() * 3);

		Set<Entry<Integer, VertexBuffer>> ee = mesh.auxBuffers.entrySet();
		for (Entry<Integer, VertexBuffer> e : ee) {
			if (e.getKey() > 0)
				emit(e.getKey(), e.getValue().bBuffer, mesh.numVertex() * e.getValue().elementSize);
		}
		header.numFrames++;
		new PythonUtils().persistAsXML(header, filename+".xmlheader");
	}

	public void addGeometry(LineList_long mesh) throws IOException {
		emit(0, mesh.vertexBuffer.bBuffer, mesh.numVertex() * 3);
		emit(-1, mesh.triangleBuffer.bBuffer, mesh.numTriangle() * 2);

		Set<Entry<Integer, VertexBuffer>> ee = mesh.auxBuffers.entrySet();
		for (Entry<Integer, VertexBuffer> e : ee) {
			if (e.getKey() > 0)
				emit(e.getKey(), e.getValue().bBuffer, mesh.numVertex() * e.getValue().elementSize);
		}
		header.numFrames++;
		new PythonUtils().persistAsXML(header, filename+".xmlheader");

	}

	public void close() throws IOException
	{
		channel.close();
		new PythonUtils().persistAsXML(header, filename+".xmlheader");
	}
	
	long offset;

	public void emit(int i, ByteBuffer buffer, int length) throws IOException {
		Frame f = new Frame();
		f.offset = offset;
		f.length = length;

		
		buffer.position(0);
		System.out.println(" emit <"+i+"> <"+buffer+"> <"+length+"> <"+(length*4)+">");
		buffer.limit(length * 4);
		
		if (length>header.maximumDimensions.get(i))
		{
			System.err.println(" WARNING: truncated buffer <"+i+"> too long <"+header.maximumDimensions.get(i)+">");
			buffer.limit(header.maximumDimensions.get(i)*4);
		}
			
		channel.write(buffer);

		offset += length;
		header.frames.addToList(i, f);
	}

}
