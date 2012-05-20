package field.graphics.core;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import field.graphics.core.MeshBlast.Frame;
import field.graphics.core.MeshBlast.Header;
import field.math.abstraction.iProvider;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Matrix4;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.math.linalg.iCoordinateFrame;
import field.util.PythonUtils;
import field.util.TaskQueue;
import static org.lwjgl.opengl.APPLEVertexArrayObject.glBindVertexArrayAPPLE;
import static org.lwjgl.opengl.APPLEVertexArrayObject.glGenVertexArraysAPPLE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_SHORT;
import static org.lwjgl.opengl.GL11.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnableClientState;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glVertexPointer;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STREAM_DRAW;
import static org.lwjgl.opengl.GL15.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.glMapBuffer;
import static org.lwjgl.opengl.GL15.glUnmapBuffer;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL12.glDrawRangeElements;

/**
 * doesn't implement iGeometry. Renders BuildMeshVisitor.Mesh's
 */
public class RawMesh2 extends BasicUtilities.OnePassListElement {

	protected final Matrix4 tmpStorage = new Matrix4();

	protected float matrix[] = null;

	protected int[] attributeBuffers = new int[] { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };

	protected int[] elementBuffer = new int[] { -1 };

	protected boolean[] needsReconstruction = new boolean[] { true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true };

	protected boolean elementBufferNeedsReconstruction = true;

	protected iProvider<iCoordinateFrame> inFrame = null;

	boolean first = true;

	protected boolean dirty = true;

	protected Header header;

	protected RandomAccessFile file;
	protected MappedByteBuffer source;

	public RawMesh2(String filename) throws IOException {
		super(Base.StandardPass.render, Base.StandardPass.render);
		header = (Header) new PythonUtils().loadAsXML(filename + ".xmlHeader");
		file = new RandomAccessFile(filename, "r");
		source = file.getChannel().map(MapMode.READ_ONLY, 0, file.length());
		source.order(ByteOrder.nativeOrder());
		setFrame(0);
	}

	public RawMesh2(String filename, MappedByteBuffer source) throws IOException {
		super(Base.StandardPass.render, Base.StandardPass.render);
		header = (Header) new PythonUtils().loadAsXML(filename + ".xmlHeader");
		this.source = source;
		setFrame(0);
	}

	protected HashMap<Integer, Frame> frame = new HashMap<Integer, Frame>();

	public void setCoordinateFrame(iProvider<iCoordinateFrame> inFrame) {
		this.inFrame = inFrame;
	}

	public void setCoordinateFrame(iCoordinateFrame inFrame) {
		this.inFrame = new iProvider.Constant<iCoordinateFrame>(inFrame);
	}

	public void setFrame(int n) {

		Set<Entry<Integer, Collection<Frame>>> r = header.frames.entrySet();
		for (Entry<Integer, Collection<Frame>> e : r) {
			Frame previous = frame.get(e.getKey());
			Frame next = ((List<Frame>) e.getValue()).get(n);
			frame.put(e.getKey(), next);
			dirty |= (previous != next);
		}
	}

	public void setVertexPatch(int aux, int frame) {
		header.maximumDimensions.put(aux, header.maximumDimensions.get(0));
		header.strides.put(aux, header.strides.get(0));

		Frame previous = this.frame.get(aux);
		Frame next = ((List<Frame>) header.frames.get(0)).get(frame);
		this.frame.put(aux, next);

		dirty |= previous != next;
	}

	public int getNumFrames() {
		return header.numFrames;
	}

	public void deallocate(TaskQueue in) {

		in.new Task() {
			@Override
			public void run() {
				gl = BasicContextManager.getGl();
				for (int i = 0; i < attributeBuffers.length; i++) {
					if (attributeBuffers[i] != -1)
						glDeleteBuffers(attributeBuffers[i]);
					;//System.out.println(" deleting <" + attributeBuffers[i] + ">");
				}
				glDeleteBuffers(elementBuffer[0]);
				BasicContextManager.putId(this, BasicContextManager.ID_NOT_FOUND);
			}
		};
	}
	FloatBuffer matrixm = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

	public void doPerformPass() {

		glBindVertexArrayAPPLE(0);
		int vertexObjectID = BasicContextManager.getId(this);
		clean();

		if (inFrame != null) {
			glPushMatrix();

			matrix = inFrame.get().getMatrix(tmpStorage).getColumnMajor(matrix);
			matrixm.rewind();
			matrixm.put(matrix);
			matrixm.rewind();
			glMultMatrix(matrixm);

		}

		glBindVertexArrayAPPLE(vertexObjectID);

		// ;//System.out.println(" drawing <"+frame.get(0).length/3+" / <"+frame.get(-1).length+">");

		//;//System.out.println(" drawing <" + frame.get(0).length / 3 + " / <" + frame.get(-1).length + "> mesh <" + matrix + "> <" + System.identityHashCode(this) + ">");

		glDrawRangeElements(GL_TRIANGLES, 0, (int) frame.get(0).length / 3, (int) frame.get(-1).length, GL_UNSIGNED_INT, 0);
		glBindVertexArrayAPPLE(0);

		if (inFrame != null) {
			glPopMatrix();
		}
	}

	public void doSetup() {

		int vertexObjectID = BasicContextManager.getId(this);

		assert (glGetError() == 0);
		glBindVertexArrayAPPLE(0);

		assert (glGetError() == 0);

		if (attributeBuffers[0] == -1 || needsReconstruction[0]) {
			if (attributeBuffers[0] == -1)
				attributeBuffers[0] = glGenBuffers();
			glBindBuffer(GL_ARRAY_BUFFER, attributeBuffers[0]);
			glBufferData(GL_ARRAY_BUFFER, header.maximumDimensions.get(0) * 4, GL_STREAM_DRAW);
			glBindBuffer(GL_ARRAY_BUFFER, 0);

			needsReconstruction[0] = false;
		}

		if ((elementBuffer[0] == -1 || elementBufferNeedsReconstruction) && !(this instanceof RawPointList2)) {
			if (elementBuffer[0] == -1)
				elementBuffer[0] = glGenBuffers();
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementBuffer[0]);
			glBufferData(GL_ELEMENT_ARRAY_BUFFER, header.maximumDimensions.get(-1) * 4,  GL_STREAM_DRAW);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

			elementBufferNeedsReconstruction = false;
		}

		;//System.out.println(" inside doSetpup for RawMesh2 <" + header.maximumDimensions + ">");

		for (int i = 1; i < 16; i++) {

			if (header.maximumDimensions.get(i) == null)
				continue;

			int aid = i;

			if (attributeBuffers[aid] == -1 || needsReconstruction[aid]) {

				;//System.out.println(" setting up <" + i + ">");
				;//System.out.println(" length in bytes are <" + (header.maximumDimensions.get(i) * 4) + "> <" + header.strides.get(i) + ">");

				if (attributeBuffers[aid] == -1)
					attributeBuffers[aid] = glGenBuffers();
				glBindBuffer(GL_ARRAY_BUFFER, attributeBuffers[aid]);
				glBufferData(GL_ARRAY_BUFFER, header.maximumDimensions.get(i) * 4, GL_STREAM_DRAW);

				needsReconstruction[aid] = false;
			}
			glBindBuffer(GL_ARRAY_BUFFER, 0);
		}
		assert (glGetError() == 0);

		// creating vertex object
		if (vertexObjectID == BasicContextManager.ID_NOT_FOUND) {
			int[] id = new int[1];
			id[0] = glGenVertexArraysAPPLE();
			vertexObjectID = id[0];
			BasicContextManager.putId(this, vertexObjectID);
		}
		assert (glGetError() == 0);

		glBindVertexArrayAPPLE(vertexObjectID);
		assert (glGetError() == 0);

		for (int i = 1; i < 16; i++) {

			if (header.maximumDimensions.get(i) == null)
				continue;

			int aid = i;

			glBindBuffer(GL_ARRAY_BUFFER, attributeBuffers[aid]);
			assert (glGetError() == 0);
			glVertexAttribPointer(aid, header.strides.get(i), GL_FLOAT, false, 0, 0);
			assert (glGetError() == 0);
			glEnableVertexAttribArray(aid);
			assert (glGetError() == 0);
		}
		assert (glGetError() == 0);

		glBindBuffer(GL_ARRAY_BUFFER, attributeBuffers[0]);

		glVertexPointer(3, GL_FLOAT, 0, 0);
		glEnableClientState(GL_VERTEX_ARRAY);

		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementBuffer[0]);

		BasicContextManager.markAsValidInThisContext(this);
		assert (glGetError() == 0);

	}

	@Override
	public void performPass() {

		if (!isOn)
			return;

		int id = BasicContextManager.getId(this);
		if (first || (id == BasicContextManager.ID_NOT_FOUND) || (!BasicContextManager.isValid(this))) {
			doSetup();
			first = false;
		}

		pre();
		doPerformPass();
		post();
	}

	public void setInFrame(iProvider<iCoordinateFrame> inFrame) {
		this.inFrame = inFrame;
	}

	public void setInFrame(final iCoordinateFrame inFrame) {
		this.inFrame = new iProvider<iCoordinateFrame>() {

			public iCoordinateFrame get() {
				return inFrame;
			}
		};
	}

	protected void clean() {
		if (!dirty)
			return;

		;//System.out.println(" cleaning raw mesh 2");

		Object context = BasicContextManager.getCurrentContext();
		{
			glBindBuffer(GL_ARRAY_BUFFER, attributeBuffers[0]);
			glBufferData(GL_ARRAY_BUFFER, header.maximumDimensions.get(0) * 4, GL_STREAM_DRAW);
			ByteBuffer buffer = glMapBuffer(GL_ARRAY_BUFFER, GL_WRITE_ONLY, null);
			buffer.rewind();
			source.limit(source.capacity());
			source.position((int) frame.get(0).offset * 4);
			source.limit((int) (frame.get(0).offset * 4 + frame.get(0).length * 4));
			source.order(ByteOrder.nativeOrder());

			// FloatBuffer peek = source.asFloatBuffer();
			// while(peek.remaining()>0)
			// {
			// ;//System.out.println(new Vector3(peek));
			// }

			// ;//System.out.println(" copying <"+source+"> into vertex");
			buffer.put(source);

			glUnmapBuffer(GL_ARRAY_BUFFER);
		}
		if (frame.get(-1) != null && frame.get(-1).offset > 0) {
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementBuffer[0]);
			glBufferData(GL_ELEMENT_ARRAY_BUFFER, header.maximumDimensions.get(-1) * 4,  GL_STREAM_DRAW);
			ByteBuffer buffer = glMapBuffer(GL_ELEMENT_ARRAY_BUFFER, GL_WRITE_ONLY,null);
			buffer.rewind();

			source.limit(source.capacity());
			source.position((int) frame.get(-1).offset * 4);
			source.limit((int) (frame.get(-1).offset * 4 + frame.get(-1).length * 4));
			source.order(ByteOrder.nativeOrder());
			// IntBuffer peek = source.asIntBuffer();
			// while(peek.remaining()>0)
			// {
			// ;//System.out.println(peek.get());
			// }
			//			
			// ;//System.out.println(" copying <"+source+"> into triangle ");
			buffer.put(source);

			glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);
		}

		for (int i = 1; i < 16; i++) {

			if (header.maximumDimensions.get(i) == null)
				continue;

			int aid = i;
			{
				glBindBuffer(GL_ARRAY_BUFFER, attributeBuffers[aid]);
				glBufferData(GL_ARRAY_BUFFER, header.maximumDimensions.get(aid) * 4, GL_STREAM_DRAW);
				ByteBuffer buffer = glMapBuffer(GL_ARRAY_BUFFER, GL_WRITE_ONLY, null);
				buffer.rewind();

				buffer.rewind();

				source.limit(source.capacity());
				source.position((int) frame.get(i).offset * 4);
				source.limit((int) (frame.get(i).offset * 4 + frame.get(i).length * 4));

				// FloatBuffer peek = source.asFloatBuffer();
				// ;//System.out.println(" dumping attributes for <"+aid+"> (assuming stride of 4 = "+header.strides.get(i)+")");
				// while (peek.remaining() > 0) {
				// ;//System.out.println(new Vector4(peek));
				// }

				buffer.put(source);

				glUnmapBuffer(GL_ARRAY_BUFFER);
			}
		}
		assert (glGetError() == 0);
		dirty = false;
	}

	public FloatBuffer vertex() {
		Frame f = frame.get(0);
		source.clear();
		try {
			source.position((int) (f.offset * 4));
			source.limit((int) (f.offset * 4 + f.length * 4));
			return source.asFloatBuffer();
		} finally {
			source.clear();
		}
	}

	public FloatBuffer aux(int n) {
		Frame f = frame.get(n);
		source.clear();
		try {
			source.position((int) (f.offset * 4));
			source.limit((int) (f.offset * 4 + f.length * 4));
			return source.asFloatBuffer();
		} finally {
			source.clear();
		}
	}

	public iCoordinateFrame getCoordinateFrame() {
		if (inFrame == null)
			return new CoordinateFrame();
		else
			return inFrame.get();
	}

	/**
	 * super slow
	 * 
	 * @return
	 */
	public List<Vector3> positions() {
		FloatBuffer m = vertex();
		List<Vector3> positions = new ArrayList<Vector3>();
		while (m.remaining() > 0) {
			positions.add(new Vector3(m));
		}
		return positions;
	}

	/**
	 * super slow
	 * 
	 * @return
	 */
	public float radius(Vector3 c) {
		FloatBuffer m = vertex();
		float r = 0;
		int q = 0;
		while (m.remaining() > 0) {
			float x = m.get();
			float y = m.get();
			float z = m.get();
			float d = (float) Math.sqrt((x - c.x) * (x - c.x) + (y - c.y) * (y - c.y) + (z - c.z) * (z - c.z));
			r += d;
			q++;
		}
		r /= q;
		return r;
	}

	public List<Vector4> aux4(int n) {
		FloatBuffer m = aux(n);
		List<Vector4> positions = new ArrayList<Vector4>();
		while (m.remaining() > 0) {
			positions.add(new Vector4(m));
		}
		return positions;
	}

	boolean isOn = true;

	public void on() {
		isOn = true;
	}

	public void off() {
		isOn = false;
	}

}
