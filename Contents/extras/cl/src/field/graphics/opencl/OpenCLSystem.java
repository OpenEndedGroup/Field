package field.graphics.opencl;

import static org.lwjgl.opencl.CL10.CL_BUILD_PROGRAM_FAILURE;
import static org.lwjgl.opencl.CL10.CL_MEM_COPY_HOST_PTR;
import static org.lwjgl.opencl.CL10.CL_MEM_READ_WRITE;
import static org.lwjgl.opencl.CL10.CL_MEM_WRITE_ONLY;
import static org.lwjgl.opencl.CL10.CL_PROGRAM_BUILD_LOG;
import static org.lwjgl.opencl.CL10.clBuildProgram;
import static org.lwjgl.opencl.CL10.clCreateBuffer;
import static org.lwjgl.opencl.CL10.clCreateCommandQueue;
import static org.lwjgl.opencl.CL10.clCreateKernel;
import static org.lwjgl.opencl.CL10.clCreateProgramWithSource;
import static org.lwjgl.opencl.CL10.clEnqueueNDRangeKernel;
import static org.lwjgl.opencl.CL10.clEnqueueReadBuffer;
import static org.lwjgl.opencl.CL10.clEnqueueWriteBuffer;
import static org.lwjgl.opencl.CL10.clFinish;
import static org.lwjgl.opencl.CL10.clSetKernelArg;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CL10GL;
import org.lwjgl.opencl.CLBuildProgramCallback;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.GL11;

import field.bytecode.protect.DeferedInQueue.iProvidesQueue;
import field.bytecode.protect.DeferedInQueue.iRegistersUpdateable;
import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.InQueue;
import field.bytecode.protect.annotations.InQueueThrough;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.execution.PythonInterface;
import field.core.plugins.python.PythonPluginEditor;
import field.graphics.core.AdvancedTextures.BaseFastNoStorageTexture;
import field.graphics.core.BasicFrameBuffers.iHasTexture;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.BasicGLSLangProgram.iErrorHandler;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.launch.iUpdateable;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Pair;
import field.util.MiscNative;
import field.util.TaskQueue;

@Woven
public class OpenCLSystem implements iProvidesQueue {

	private CLContext context;
	private CLCommandQueue commands;
	private final TaskQueue renderQueue;
	private List<CLDevice> devices;

	public OpenCLSystem(TaskQueue renderQueue) throws LWJGLException {
		this.renderQueue = renderQueue;
		init();
	}

	public iRegistersUpdateable getQueueFor(Method m) {
		return renderQueue;
	}

	@InQueue
	protected void init() throws LWJGLException {
		CL.create();

		CLPlatform platform = CLPlatform.getPlatforms().get(0);
		devices = platform.getDevices(CL10.CL_DEVICE_TYPE_GPU);

		;//;//System.out.println(" devices are <" + devices + ">");
		final long group = new MiscNative().getCurrentContextShareGroup();

		context = CLContext.create(platform, devices, null, new Drawable() {

			@Override
			public void setCLSharingProperties(PointerBuffer arg0) throws LWJGLException {
				arg0.put(0x10000000);
				arg0.put(group);
				arg0.put(0);
			}

			@Override
			public void releaseContext() throws LWJGLException {
			}

			@Override
			public void makeCurrent() throws LWJGLException {
			}

			@Override
			public boolean isCurrent() throws LWJGLException {
				return false;
			}

			@Override
			public void destroy() {
			}
		}, null);
		commands = clCreateCommandQueue(context, devices.get(0), CL10.CL_QUEUE_PROFILING_ENABLE, null);

	}

	IntBuffer err = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
	private iErrorHandler eh;

	@Woven
	public class Program implements iProvidesQueue {
		private String source;

		private String kernelName;

		private CLProgram program;

		private CLKernel kernel;

		private String lastLog;

		public Program(String source, String kernel) {
			this.source = source;
			this.kernelName = kernel;
			construct();
		}

		@InQueue
		public void construct() {

			;//;//System.out.println(" create program with source ");
			program = clCreateProgramWithSource(context, new CharSequence[] { source }, err);
			checkErr();
			;//;//System.out.println(" error is <" + err.get(0) + ">");
			Object ret = clBuildProgram(program, devices.get(0), "", new CLBuildProgramCallback() {

				@Override
				protected void handleMessage(CLProgram arg0) {
					System.err.println(" !! message from build program");
					System.err.println(" !! message from build program <" + arg0.isValid() + ">");
				}
			});
			;//;//System.out.println(" ret is <" + ret + ">");
			if (ret == (Integer) CL_BUILD_PROGRAM_FAILURE) {
				String log = program.getBuildInfoString(devices.get(0), CL_PROGRAM_BUILD_LOG);
				;//;//System.out.println(" log :" + log);
				if (eh != null) {
					eh.beginError();
					eh.errorOnLine(-1, log);
					new Exception().printStackTrace();
				}
				lastLog = log;
				kernel = null;
			} else {
				kernel = clCreateKernel(program, kernelName, err);
			}
		}

		public iRegistersUpdateable getQueueFor(Method m) {
			return renderQueue;
		}

	}

	@Woven
	public class Memory implements iProvidesQueue {
		private CLMem memory;
		protected ByteBuffer buffer;
		private final int byteLength;

		public Memory(int byteLength) {
			this.byteLength = byteLength;
			buffer = ByteBuffer.allocateDirect(byteLength).order(ByteOrder.nativeOrder());

			init();
		}

		@InQueue
		protected void init() {
			memory = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, buffer, err);
		}

		public ByteBuffer getBuffer() {

			return buffer;
		}

		@InQueue
		public void copyFromHost() {
			buffer.rewind();
			check(clEnqueueWriteBuffer(commands, memory, 1, 0, buffer, null, null));
		}

		@InQueue
		public void copyFromHost(int start, int length) {
			buffer.limit(start + length);
			buffer.position(start);
			check(clEnqueueWriteBuffer(commands, memory, 1, 0, buffer, null, null));
			buffer.reset();
		}

		@InQueue
		public void copyToHost() {
			buffer.rewind();
			check(clEnqueueReadBuffer(commands, memory, 1, 0, buffer, null, null));
		}

		@InQueue
		public void copyToHost(int start, int length) {
			buffer.limit(start + length);
			buffer.position(start);
			check(clEnqueueReadBuffer(commands, memory, 1, 0, buffer, null, null));
			buffer.reset();
		}

		public iRegistersUpdateable getQueueFor(Method m) {
			return renderQueue;
		}

	}

	@Woven
	public class Image implements iProvidesQueue {
		private CLMem memory;
		protected ByteBuffer buffer;
		private int width;
		private int height;
		private PointerBuffer origin;
		private PointerBuffer region;

		public Image(int width, int height) {
			buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
			this.width = width;
			this.height = height;

			origin = BufferUtils.createPointerBuffer(3);
			origin.put(0);
			origin.put(0);
			origin.put(0);
			origin.rewind();
			region = BufferUtils.createPointerBuffer(3);
			region.put(width);
			region.put(height);
			region.put(1);
			region.rewind();
			
			init();
		}

		@InQueue
		protected void init() {

			ByteBuffer d = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder());
			IntBuffer di = d.asIntBuffer();
			di.put(CL10.CL_RGBA).put(CL10.CL_UNSIGNED_INT8);
			di.rewind();

			memory = CL10.clCreateImage2D(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, d, width, height, width * 4, buffer, err);
			checkErr();
		}

		public ByteBuffer getBuffer() {

			return buffer;
		}

		@InQueue
		public void copyFromHost() {
			buffer.rewind();
			check(CL10.clEnqueueWriteImage(commands, memory, 1, origin, region, width*4, 0, buffer, null, null));
		}

		@InQueue
		public void copyFromHost(int start, int length) {
			buffer.limit(start + length);
			buffer.position(start);
			check(CL10.clEnqueueWriteImage(commands, memory, 1, origin, region, width*4, 0, buffer, null, null));
			buffer.reset();
		}

		@InQueue
		public void copyToHost() {
			buffer.rewind();
			check(CL10.clEnqueueReadImage(commands, memory, 1, origin, region, width*4, 0, buffer, null, null));
		}

		@InQueue
		public void copyToHost(int start, int length) {
			buffer.limit(start + length);
			buffer.position(start);
			check(CL10.clEnqueueReadImage(commands, memory, 1, origin, region, width*4, 0, buffer, null, null));
			buffer.reset();
		}

		public iRegistersUpdateable getQueueFor(Method m) {
			return renderQueue;
		}

	}

	@Woven
	public class GLMemory implements iProvidesQueue {
		private CLMem memory;
		private TriangleMesh mesh;
		private int aux;
		private ByteBuffer buffer;
		private int byteLength;

		public GLMemory(TriangleMesh mesh, int aux) {
			this.mesh = mesh;
			this.aux = aux;
			init(mesh, aux);
			byteLength = 4 * mesh.aux(aux, 0).capacity();
			buffer = ByteBuffer.allocateDirect(byteLength).order(ByteOrder.nativeOrder());
		}

		public ByteBuffer getBuffer() {
			return buffer;
		}

		@InQueue
		public void copyFromHost() {
			buffer.rewind();
			check(clEnqueueWriteBuffer(commands, memory, 1, 0, buffer, null, null));
		}

		@InQueue
		void init(TriangleMesh mesh, int aux) {
			memory = CL10GL.clCreateFromGLBuffer(context, CL_MEM_READ_WRITE, mesh.getOpenGLBufferName(aux), err);
			checkErr();
		}

		public iRegistersUpdateable getQueueFor(Method m) {
			return renderQueue;
		}
	}

	@Woven
	public class GLTexture implements iProvidesQueue {
		private final BaseFastNoStorageTexture texture;
		private CLMem image;

		public GLTexture(BaseFastNoStorageTexture texture) {
			this.texture = texture;
			init();
		}

		@InQueue
		protected void init() {

			;//;//System.out.println(" creating gl image form texture 2d <" + texture.textureTarget + "> <" + texture.getTextureID() + ">");

			image = CL10GL.clCreateFromGLTexture2D(context, CL_MEM_WRITE_ONLY, texture.textureTarget, 0, texture.getTextureID(), null);

			;//;//System.out.println(" got <" + image + ">");

		}

		@InQueue
		public void copyBufferToTexture(Memory m, int ox, int oy, int w, int h) {

			PointerBuffer origin = BufferUtils.createPointerBuffer(3);
			origin.put(ox);
			origin.put(oy);
			origin.put(0);
			origin.rewind();
			PointerBuffer region = BufferUtils.createPointerBuffer(3);
			region.put(w);
			region.put(h);
			region.put(1);
			region.rewind();
			check(CL10GL.clEnqueueAcquireGLObjects(commands, image, null, null));
			check(CL10.clEnqueueCopyBufferToImage(commands, m.memory, image, 0, origin, region, null, null));
			check(CL10GL.clEnqueueReleaseGLObjects(commands, image, null, null));
		}

		public iRegistersUpdateable getQueueFor(Method m) {
			return renderQueue;
		}
	}

	@Woven
	public class GLFBO implements iProvidesQueue {
		private final iHasTexture texture;
		private CLMem image;

		public GLFBO(iHasTexture texture) {
			this.texture = texture;
			init();
		}

		@InQueue
		protected void init() {

			image = CL10GL.clCreateFromGLTexture2D(context, CL10.CL_MEM_READ_ONLY, GL11.GL_TEXTURE_2D, 0, texture.getOutput().get(), err);

			checkErr();

			;//;//System.out.println(" got fbo image <" + image + "> <" + image.isValid() + "> <" + texture.getOutput().get() + ">");

		}

		@InQueue
		public void copyTextureToBuffer(Memory m, int ox, int oy, int w, int h) {

			PointerBuffer origin = BufferUtils.createPointerBuffer(3);
			origin.put(ox);
			origin.put(oy);
			origin.put(0);
			origin.rewind();
			PointerBuffer region = BufferUtils.createPointerBuffer(3);
			region.put(w);
			region.put(h);
			region.put(1);
			region.rewind();
			check(CL10GL.clEnqueueAcquireGLObjects(commands, image, null, null));
			check(CL10.clEnqueueCopyImageToBuffer(commands, image, m.memory, origin, region, 0, null, null));
			check(CL10GL.clEnqueueReleaseGLObjects(commands, image, null, null));
		}

		public iRegistersUpdateable getQueueFor(Method m) {
			return renderQueue;
		}
	}

	public interface iHasBuffer {
		public ByteBuffer getBuffer();

		public boolean isDirty();

		public void clean();
	}

	public class MemoryBackedFloat4 implements iHasBuffer {
		FloatBuffer buffer;
		boolean dirty = true;
		private ByteBuffer bytes;

		public MemoryBackedFloat4() {
			bytes = ByteBuffer.allocateDirect(4 * 4).order(ByteOrder.nativeOrder());
			buffer = bytes.asFloatBuffer();
		}

		public MemoryBackedFloat4 set(Vector4 v) {
			buffer.rewind();
			buffer.put(v.x);
			buffer.put(v.y);
			buffer.put(v.z);
			buffer.put(v.w);
			buffer.rewind();
			dirty = true;
			return this;
		}

		public ByteBuffer getBuffer() {
			return bytes;
		}

		public boolean isDirty() {
			return dirty;
		}

		public void clean() {
			dirty = false;
		}
	}

	@Woven
	public class ExecutableKernel implements iProvidesQueue, field.core.util.FieldPyObjectAdaptor.iHandlesFindItem {

		private Program program;
		private Program nextProgram;

		FloatBuffer t0 = ByteBuffer.allocateDirect(4 * 1).order(ByteOrder.nativeOrder()).asFloatBuffer();
		IntBuffer i0 = ByteBuffer.allocateDirect(4 * 1).order(ByteOrder.nativeOrder()).asIntBuffer();
		FloatBuffer t1 = ByteBuffer.allocateDirect(4 * 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
		FloatBuffer t2 = ByteBuffer.allocateDirect(4 * 3).order(ByteOrder.nativeOrder()).asFloatBuffer();
		FloatBuffer t3 = ByteBuffer.allocateDirect(4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

		public ExecutableKernel(Program program) {
			this.program = program;
		}

		public void reload(String source) {
			this.nextProgram = new Program(source, this.program.kernelName);
		}

		LinkedHashMap<Integer, Object> mapped = new LinkedHashMap<Integer, Object>();

		@InQueueThrough
		public void setParameter(int parameter, Memory m) {
			check(clSetKernelArg(program.kernel, parameter, m.memory));
			mapped.put(parameter, m);
		}

		@InQueueThrough
		public void setParameter(int parameter, CLMem m) {
			check(clSetKernelArg(program.kernel, parameter, m));
			mapped.put(parameter, m);
		}

		@InQueueThrough
		public void setParameter(int parameter, GLMemory m) {
			check(clSetKernelArg(program.kernel, parameter, m.memory));
			mapped.put(parameter, m);
		}

		@InQueueThrough
		public void setParameter(int parameter, Image m) {
			check(clSetKernelArg(program.kernel, parameter, m.memory));
			mapped.put(parameter, m);
		}

		@InQueueThrough
		public void setParameter(int parameter, GLFBO m) {

			check(clSetKernelArg(program.kernel, parameter, m.image));
			mapped.put(parameter, m);
		}

		@InQueueThrough
		public void setParameter(int parameter, GLTexture m) {
			check(clSetKernelArg(program.kernel, parameter, m.image));
			mapped.put(parameter, m);
		}

		@InQueueThrough
		public void setParameter(int parameter, float f) {
			t0.rewind();
			t0.put(f);
			t0.rewind();
			check(clSetKernelArg(program.kernel, parameter, t0));
			mapped.put(parameter, f);
			;//;//System.out.println(" set float <" + parameter + "> f <" + f + "> now");
		}

		@InQueueThrough
		public void setParameter(int parameter, int i) {
			i0.rewind();
			i0.put(i);
			i0.rewind();
			check(clSetKernelArg(program.kernel, parameter, i0));
			mapped.put(parameter, i);
		}

		@InQueueThrough
		public void setParameter(int parameter, FloatBuffer b) {
			check(clSetKernelArg(program.kernel, parameter, b));
			mapped.put(parameter, b);
		}

		@InQueueThrough
		public void setParameter(int parameter, MemoryBackedFloat4 b) {
			Buffer bb = b.getBuffer();
			check(clSetKernelArg(program.kernel, parameter, b.buffer));
			b.clean();
			mapped.put(parameter, b);
		}

		public void set(int parameter, Object o) {
			if (o instanceof Memory)
				setParameter(parameter, ((Memory) o));
			else if (o instanceof GLFBO)
				setParameter(parameter, ((GLFBO) o));
			else if (o instanceof GLMemory)
				setParameter(parameter, ((GLMemory) o));
			else if (o instanceof Image)
				setParameter(parameter, ((Image) o));
			else if (o instanceof GLTexture)
				setParameter(parameter, ((GLTexture) o));
			else if (o instanceof Float)
				setParameter(parameter, ((Float) o));
			else if (o instanceof Double)
				setParameter(parameter, ((Double) o).floatValue());
			else if (o instanceof Integer)
				setParameter(parameter, ((Integer) o));
			else if (o instanceof MemoryBackedFloat4)
				setParameter(parameter, ((MemoryBackedFloat4) o));
			else if (o instanceof Vector4) {
				Object oo = mapped.get(parameter);
				if (oo instanceof MemoryBackedFloat4) {
					;//;//System.out.println(" resetting ");
					((MemoryBackedFloat4) oo).set(((Vector4) o));
				} else {
					;//;//System.out.println(" settting first ");
					setParameter(parameter, new MemoryBackedFloat4().set(((Vector4) o)));
				}
			} else
				throw new ClassCastException(" couldn't do anything with <" + o + "> <" + (o == null ? null : o.getClass()) + ">");
		}

		@InQueue
		public void execute1D(int globalSize, int localSize) {

			preamble();

			try {
				PointerBuffer pb_globalSize = BufferUtils.createPointerBuffer(1);
				pb_globalSize.put(globalSize);
				PointerBuffer pb_localSize = BufferUtils.createPointerBuffer(1);
				pb_localSize.put(localSize);

				pb_globalSize.rewind();
				pb_localSize.rewind();

				if (program.kernel != null)
					check(clEnqueueNDRangeKernel(commands, program.kernel, 1, null, pb_globalSize, localSize == -1 ? null : pb_localSize, null, null));
			} finally {
				postamble();
			}
		}

		private void postamble() {
			for (Object o : mapped.values()) {
				if (o instanceof GLMemory)
					CL10GL.clEnqueueReleaseGLObjects(commands, ((GLMemory) o).memory, null, null);
				if (o instanceof GLTexture)
					CL10GL.clEnqueueReleaseGLObjects(commands, ((GLTexture) o).image, null, null);
				if (o instanceof GLFBO)
					CL10GL.clEnqueueReleaseGLObjects(commands, ((GLFBO) o).image, null, null);
			}
		}

		private void preamble() {

			GL11.glFlush();

			boolean forceAll = false;
			if (nextProgram != null) {
				if (nextProgram.kernel != null) {
					System.err.println(" swapping program to next program");
					program = nextProgram;
					for (Map.Entry<Integer, Object> o : mapped.entrySet()) {

						;//;//System.out.println(" dirty :" + o);

						if (o.getValue() instanceof MemoryBackedFloat4) {
							((MemoryBackedFloat4) o.getValue()).dirty = true;
						}
					}
					forceAll = true;
					nextProgram = null;
				} else {
					System.err.println(" error building next program ? ");
					nextProgram = null;
				}
			}

			for (Map.Entry<Integer, Object> o : mapped.entrySet()) {
				if (o.getValue() instanceof GLMemory)
					CL10GL.clEnqueueAcquireGLObjects(commands, ((GLMemory) o.getValue()).memory, null, null);
				else if (o.getValue() instanceof GLTexture)
					CL10GL.clEnqueueAcquireGLObjects(commands, ((GLTexture) o.getValue()).image, null, null);
				else if (o.getValue() instanceof GLFBO)
					CL10GL.clEnqueueAcquireGLObjects(commands, ((GLFBO) o.getValue()).image, null, null);
				else if (o.getValue() instanceof MemoryBackedFloat4) {
					MemoryBackedFloat4 b = (MemoryBackedFloat4) o.getValue();
					if (b.isDirty()) {
						clSetKernelArg(program.kernel, o.getKey(), b.buffer);
						b.clean();
					}
				} else if (forceAll) {
					set(o.getKey(), o.getValue());
				}
			}
		}

		@InQueue
		public void execute2D(int globalXSize, int globalYSize, int localXSize, int localYSize) {
			preamble();
			try {
				PointerBuffer pb_globalSize = BufferUtils.createPointerBuffer(2);
				pb_globalSize.put(globalXSize);
				pb_globalSize.put(globalYSize);
				PointerBuffer pb_localSize = BufferUtils.createPointerBuffer(2);
				pb_localSize.put(localXSize);
				pb_localSize.put(localYSize);

				pb_globalSize.rewind();
				pb_localSize.rewind();

				if (program.kernel != null)
					check(clEnqueueNDRangeKernel(commands, program.kernel, 2, null, pb_globalSize, localXSize == -1 ? null : pb_localSize, null, null));
			} finally {
				postamble();
			}
		}

		public iRegistersUpdateable getQueueFor(Method m) {
			return renderQueue;
		}

		public Object getItem(Object object) {
			return null;
		}

		public void setItem(Object name, Object value) {
			set(((Number) name).intValue(), value);
		}

		public Object getAttribute(String name) {
			return null;
		}

		public void setAttribute(String name, Object value) {
		}
	}

	protected void check(int e) {
		if (e != 0) {
			if (eh == null)
				throw new IllegalArgumentException("error code :" + e);
			else {
				eh.beginError();
				eh.errorOnLine(-1, "OpenCL error: " + e);
				new Exception().printStackTrace();
			}
		}

	}

	protected void checkErr() {
		;//;//System.out.println(" error :" + err.get(0));
		if (err.get(0) != 0)
			throw new IllegalArgumentException("error code :" + err.get(0));

	}

	@InQueue
	public void then(iUpdateable u) {
		u.update();
	}

	@InQueue
	public void finish() {
		check(clFinish(commands));
	}

	public ExecutableKernel makeKernel(String source, String kernel) {
		return new ExecutableKernel(new Program(source, kernel));
	}

	public Memory makeMemory(int sizeInBytes) {
		return new Memory(sizeInBytes);
	}

	public GLTexture addTextureOutput(int unit, field.graphics.core.Base.iAcceptsSceneListElement e, final int width, final int height) {
		final BaseFastNoStorageTexture texture = new BaseFastNoStorageTexture(width, height);
		texture.use_gl_texture_rectangle_ext(false);
		e.addChild(new field.graphics.core.BasicTextures.TextureUnit(unit, texture));
		return new GLTexture(texture) {
			public void copyBufferToTexture(Memory m) {
				super.copyBufferToTexture(m, 0, 0, width, height);
				// texture.genMipsNext = true;
			}
		};
	}

	public GLFBO addFBOInput(iHasTexture rb, final int width, final int height) {
		GLFBO f = new GLFBO(rb);
		return f;
	}

	public Image makeImage(final int width, final int height) {
		return new Image(width, height);
	}

	static public final VisualElementProperty<String> opencl_v = new VisualElementProperty<String>("opencl_v");
	static {
		PythonPluginEditor.knownPythonProperties.put("<b>OpenCL</b> - <font size=-2>opencl_v</font>", opencl_v);
	}

	static public ExecutableKernel makeKernelFromElement(OpenCLSystem s, final iVisualElement element) {

		final Stack<Writer> redir = new Stack<Writer>();
		redir.addAll(PythonInterface.getPythonInterface().getErrorRedirects());
		final Stack<Writer> reodir = new Stack<Writer>();
		reodir.addAll(PythonInterface.getPythonInterface().getOutputRedirects());

		String e = opencl_v.get(element);
		if (e == null || e.trim().length() == 0) {
			e = "__kernel void main_kernel( __global uchar4 *result )\n" + "{\n" + "	int tx = get_global_id(0);\n" + "	int ty = get_global_id(1);\n" + "	int sx = get_global_size(0);\n" + "	int sy = get_global_size(1);\n" + "	int index = ty * sx + tx;\n" + "\n" + "	result[index] = (uchar4)(255,128,0,255);\n" + "}\n";
			opencl_v.set(element, element, e);
		}

		final ExecutableKernel ex = s.makeKernel(e, "main_kernel");

		s.setErrorHandler(new BasicGLSLangProgram.iErrorHandler() {

			@Override
			public void noError() {
				if (reodir.size() > 0) {
					Writer e = reodir.peek();
					try {
						e.write(" Reloaded OpenCL program successfully \n");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				} else {
					System.err.println(" Reloaded OpenCL program successfully \n");
				}
			}

			@Override
			public void errorOnLine(int line, String error) {
				Writer e = redir.peek();
				try {
					e.write("on line " + line + " '" + error + "\n");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}

			@Override
			public void endError() {
			}

			@Override
			public void beginError() {
				if (redir.size() > 0) {
					Writer e = redir.peek();
					try {
						e.write(" Errors occured on OpenCL program reload \n");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				} else {
					System.err.println(" Errors occured on OpenCL program reload ");
				}
			}
		});

		PythonPluginEditor.python_customToolbar.addToList(ArrayList.class, element, new Pair<String, iUpdateable>("Refresh kernel", new iUpdateable() {
			public void update() {
				ex.reload(opencl_v.get(element));
			}
		}));

		return ex;
	}

	private void setErrorHandler(iErrorHandler iErrorHandler) {
		eh = iErrorHandler;
	}
}
