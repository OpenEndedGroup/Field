package field.graphics.jfbxlib;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;

import field.graphics.core.BasicGeometry.TriangleMesh_long;
import field.namespace.generic.Generics.Triple;

public class StreamingMeshTools {

	public StreamingMeshTools() {
	}

	static public class StreamingMeshCreation {

		private final String baseFilename;

		public StreamingMeshCreation(String baseFilename) {
			this.baseFilename = baseFilename;
		}

		public void add(ByteBuffer vertex, ByteBuffer triangle) throws IOException {
			RandomAccessFile raf = new RandomAccessFile(new File(baseFilename + ".streamingmesh"), "rw");
			FileChannel c = raf.getChannel();
			c.position(c.size());
			long vertexStart = c.size();
			c.write(vertex);
			long triangleStart = c.size();
			c.write(triangle);
			long end = raf.length();
			raf.close();

			List<Triple<Long, Long, Long>> frames = new ArrayList<Triple<Long, Long, Long>>();
			try {
				ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(baseFilename + ".streamingmeshoffsets"))));
				try {
					frames = (List<Triple<Long, Long, Long>>) ois.readObject();
				} finally {
					ois.close();
				}
			} catch (Throwable t) {
			}
			frames.add(new Triple<Long, Long, Long>(vertexStart, triangleStart, end));
			ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(baseFilename + ".streamingmeshoffsets"))));
			oos.writeObject(frames);
			oos.close();
		}
	}

	static public class StreamingMeshPlayback {
		List<Triple<Long, Long, Long>> frames = new ArrayList<Triple<Long, Long, Long>>();
		private RandomAccessFile raf;
		private MappedByteBuffer source;
		private int end;

		public StreamingMeshPlayback(String baseFilename) throws FileNotFoundException, IOException {

			ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(baseFilename + ".streamingmeshoffsets"))));
			try {
				frames = (List<Triple<Long, Long, Long>>) ois.readObject();
				;//;//System.out.println("frames :"+frames);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			ois.close();

			raf = new RandomAccessFile(new File(baseFilename + ".streamingmesh"), "r");
			source = raf.getChannel().map(MapMode.READ_ONLY, 0, raf.length());
			source.order(ByteOrder.nativeOrder());
		}

		public int[] getMaximums() {
			int maxVertex = 0;
			int maxTriangle = 0;

			for (Triple<Long, Long, Long> f : frames) {
				int numVertex = (int) ((f.middle - f.left) / (4 * 3));
				int numTirangle = (int) ((f.right - f.middle) / (4 * 3));
				if (numVertex > maxVertex)
					maxVertex = numVertex;
				if (numTirangle > maxTriangle)
					maxTriangle = numTirangle;

			}
			return new int[] { maxVertex, maxTriangle };
		}

		// we can do this in one less copy than this
		public void copyTo(int frame, TriangleMesh_long m) {
			
			FloatBuffer v = m.vertex();
			System.err.println(" A1");
			Triple<Long, Long, Long> f = frames.get(frame);
			System.err.println(" A2");
			int numVertex = (int) ((f.middle - f.left) / (4 * 3));
			System.err.println(" A3 - "+numVertex);
			int numTirangle = (int) ((f.right - f.middle) / (4 * 3));
			System.err.println(" A4");
			source.position((int) (long) f.left);
			System.err.println(" A5");
			FloatBuffer sliced = source.slice().order(ByteOrder.nativeOrder()).asFloatBuffer();
			System.err.println(" A6");
			sliced.limit(numVertex * 3);
			System.err.println(" A7");
			v.limit(v.capacity());
			System.err.println(" A8");
			v.put(sliced);
			System.err.println(" A9");
			m.setVertexLimit(numVertex);
			System.err.println(" A11");

			source.position((int) (long) f.middle);
			System.err.println(" A12");
			IntBuffer ssliced = source.slice().order(ByteOrder.nativeOrder()).asIntBuffer();
			System.err.println(" A13");
			ssliced.limit(numTirangle * 3);
			
			System.err.println(" A14");
			System.err.println(" A15");
			System.err.println("from <"+ssliced+"> to <"+m.longTriangle()+">");
			System.err.println(" A16");
			m.longTriangle().put(ssliced);
			System.err.println(" A17");

			m.setTriangleLimit(numTirangle);
			m.setVertexLimit(numVertex);
			
//			m.setTriangleLimit(50);
//			System.err.println(" A17b");
//			m.setVertexLimit(100);
//			System.err.println(" A18");
		}
	}

}
