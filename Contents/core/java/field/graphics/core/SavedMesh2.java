package field.graphics.core;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import field.graphics.core.Base.iLongGeometry;
import field.graphics.core.BasicGeometry.LineList_long;
import field.graphics.core.BasicGeometry.TriangleMesh_long;
import field.graphics.core.MeshBlast.Frame;
import field.graphics.core.MeshBlast.Header;
import field.util.PythonUtils;

/**
 * just uses the MeshBlast format
 * 
 * @author marc
 * 
 */
public class SavedMesh2 {

	static public void save(iLongGeometry mesh, String name) throws IOException {
		MeshBlast b = new MeshBlast(name);
		b.configureChannel(mesh, mesh.numVertex(), mesh.numTriangle(), mesh instanceof LineList_long ? 2 : 3);
		if (mesh instanceof LineList_long)
			b.addGeometry(((LineList_long) mesh));
		else if (mesh instanceof TriangleMesh_long)
			b.addGeometry(((TriangleMesh_long) mesh));
		b.close();
	}

	static public void save(PointList mesh, String name) throws IOException {
		MeshBlast b = new MeshBlast(name);
		b.configureChannel(mesh, mesh.numVertex());
		b.addGeometry(mesh);
		b.close();
	}

	static public TriangleMesh_long loadTriangle(String filename) throws IOException {
		try {
			return loadTriangle(filename, TriangleMesh_long.class);
		} catch (InstantiationException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

	static public TriangleMesh_long loadTriangle(String filename, Class<? extends TriangleMesh_long> meshClass) throws IOException, InstantiationException, IllegalAccessException {
		Header header = (Header) new PythonUtils().loadAsXML(filename + ".xmlHeader");

		RandomAccessFile file = new RandomAccessFile(filename, "r");
		MappedByteBuffer source = file.getChannel().map(MapMode.READ_ONLY, 0, file.length());
		source.order(ByteOrder.nativeOrder());

		TriangleMesh_long mesh = meshClass.newInstance();

		HashMap<Integer, Frame> frame = new HashMap<Integer, Frame>();

		Set<Entry<Integer, Collection<Frame>>> r = header.frames.entrySet();
		for (Entry<Integer, Collection<Frame>> e : r) {
			frame.put(e.getKey(), ((List<Frame>) e.getValue()).get(0));
		}

		mesh.rebuildVertex(header.maximumDimensions.get(0) / 3);
		mesh.rebuildTriangle(header.maximumDimensions.get(-1) / 3);

		source.limit(source.capacity());
		source.position((int) frame.get(0).offset * 4);
		source.limit((int) (frame.get(0).offset * 4 + frame.get(0).length * 4));
		source.order(ByteOrder.nativeOrder());
		mesh.vertex().put(source.asFloatBuffer());

		source.limit(source.capacity());
		source.position((int) frame.get(-1).offset * 4);
		source.limit((int) (frame.get(-1).offset * 4 + frame.get(-1).length * 4));
		source.order(ByteOrder.nativeOrder());
		mesh.longTriangle().put(source.asIntBuffer());

		for (int i = 1; i < 16; i++) {
			if (header.maximumDimensions.get(i) == null)
				continue;

			System.out.println(" loading aux <" + i + " " + header.maximumDimensions.get(i) + " " + header.maximumDimensions.get(0) + " = " + (header.maximumDimensions.get(i) * 3 / header.maximumDimensions.get(0)));

			FloatBuffer a = mesh.aux(i, header.maximumDimensions.get(i) * 3 / header.maximumDimensions.get(0));

			source.limit(source.capacity());
			source.position((int) frame.get(i).offset * 4);
			source.limit((int) (frame.get(i).offset * 4 + frame.get(i).length * 4));
			source.order(ByteOrder.nativeOrder());
			a.put(source.asFloatBuffer());
		}

		file.getChannel().close();

		return mesh;
	}

	static public LineList_long loadLine(String filename) throws IOException {
		Header header = (Header) new PythonUtils().loadAsXML(filename + ".xmlHeader");

		RandomAccessFile file = new RandomAccessFile(filename, "r");
		MappedByteBuffer source = file.getChannel().map(MapMode.READ_ONLY, 0, file.length());
		source.order(ByteOrder.nativeOrder());

		LineList_long mesh = new LineList_long(new BasicUtilities.Position());

		HashMap<Integer, Frame> frame = new HashMap<Integer, Frame>();

		Set<Entry<Integer, Collection<Frame>>> r = header.frames.entrySet();
		for (Entry<Integer, Collection<Frame>> e : r) {
			frame.put(e.getKey(), ((List<Frame>) e.getValue()).get(0));
		}

		mesh.rebuildVertex(header.maximumDimensions.get(0) / 3);
		mesh.rebuildTriangle(header.maximumDimensions.get(-1) / 2);

		source.limit(source.capacity());
		source.position((int) frame.get(0).offset * 4);
		source.limit((int) (frame.get(0).offset * 4 + frame.get(0).length * 4));
		source.order(ByteOrder.nativeOrder());
		mesh.vertex().put(source.asFloatBuffer());

		source.limit(source.capacity());
		source.position((int) frame.get(-1).offset * 4);
		source.limit((int) (frame.get(-1).offset * 4 + frame.get(-1).length * 4));
		source.order(ByteOrder.nativeOrder());
		mesh.longTriangle().put(source.asIntBuffer());

		for (int i = 1; i < 16; i++) {
			if (header.maximumDimensions.get(i) == null)
				continue;

			FloatBuffer a = mesh.aux(i, header.maximumDimensions.get(i) * 3 / header.maximumDimensions.get(0));

			source.limit(source.capacity());
			source.position((int) frame.get(i).offset * 4);
			source.limit((int) (frame.get(i).offset * 4 + frame.get(i).length * 4));
			source.order(ByteOrder.nativeOrder());
			a.put(source.asFloatBuffer());
		}

		file.getChannel().close();

		return mesh;
	}

	static public PointList loadPoint(String filename) throws IOException {
		try {
			return loadPoint(filename, PointList.class);
		} catch (InstantiationException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

	static public PointList loadPoint(String filename, Class<? extends PointList> meshClass) throws IOException, InstantiationException, IllegalAccessException {
		Header header = (Header) new PythonUtils().loadAsXML(filename + ".xmlHeader");

		RandomAccessFile file = new RandomAccessFile(filename, "r");
		MappedByteBuffer source = file.getChannel().map(MapMode.READ_ONLY, 0, file.length());
		source.order(ByteOrder.nativeOrder());

		PointList mesh = meshClass.newInstance();

		HashMap<Integer, Frame> frame = new HashMap<Integer, Frame>();

		Set<Entry<Integer, Collection<Frame>>> r = header.frames.entrySet();
		for (Entry<Integer, Collection<Frame>> e : r) {
			frame.put(e.getKey(), ((List<Frame>) e.getValue()).get(0));
		}

		mesh.rebuildVertex(header.maximumDimensions.get(0) / 3);

		source.limit(source.capacity());
		source.position((int) frame.get(0).offset * 4);
		source.limit((int) (frame.get(0).offset * 4 + frame.get(0).length * 4));
		source.order(ByteOrder.nativeOrder());
		mesh.vertex().put(source.asFloatBuffer());

		for (int i = 1; i < 16; i++) {
			if (header.maximumDimensions.get(i) == null)
				continue;

			FloatBuffer a = mesh.aux(i, header.maximumDimensions.get(i) * 3 / header.maximumDimensions.get(0));

			source.limit(source.capacity());
			source.position((int) frame.get(i).offset * 4);
			source.limit((int) (frame.get(i).offset * 4 + frame.get(i).length * 4));
			source.order(ByteOrder.nativeOrder());
			a.put(source.asFloatBuffer());
		}

		file.getChannel().close();

		return mesh;
	}

}
