package field.graphics.ci;

import static org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_ZERO;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import field.core.dispatch.iVisualElement.Rect;
import field.core.util.FieldPyObjectAdaptor.iHandlesAttributes;
import field.core.util.FieldPyObjectAdaptor2;
import field.core.windowing.GLComponentWindow;
import field.graphics.core.Base;
import field.graphics.core.BasicFrameBuffers.SingleFrameBuffer;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.CoreHelpers;
import field.graphics.core.ResourceMonitor;
import field.graphics.dynamic.DynamicMesh;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.util.TaskQueue;

/**
 * textures need to be declared and drawn to inside the drawing method
 */
public class CoreImageCanvasUtils {

	HashSet<Long> alreadyFinalized = new HashSet<Long>();

	DynamicMesh mesh = DynamicMesh.coloredTexturedMesh(null);

	// in general, fbo's act as places to put images and ways of
	// getting those images onto the screen
	public class Destination {
		public SingleFrameBuffer fbo;
		private final int width;
		private final int height;
		// DynamicMesh mesh;

		Image drawn;
		private TaskQueue deletionQueue;

		public Destination(int width, int height) {
			this.width = width;
			this.height = height;
			fbo = new SingleFrameBuffer(width, height, false, false, true);
		}

		public void drawIntoCanvasNow(Vector3 a, Vector3 b, Vector3 c, Vector3 d, Vector4 tint) {

			System.out.println(" draw destination into canvas now in 3d  --- drew <" + drawn + " " + (drawn != null ? drawn.coreimage : null) + ">");

			CoreHelpers.glPushAttrib(GL_ALL_ATTRIB_BITS);

			// glActiveTexture(GL_TEXTURE0+1);
			glActiveTexture(1);
			deletionQueue = ResourceMonitor.resourceMonitor.getQueue();
			fbo.pre();

			mesh.open();
			int v1 = mesh.nextVertex(a);
			int v2 = mesh.nextVertex(b);
			int v3 = mesh.nextVertex(c);
			int v4 = mesh.nextVertex(d);

			mesh.setAux(v1, Base.color0_id, tint.x, tint.y, tint.z, tint.w);
			mesh.setAux(v2, Base.color0_id, tint.x, tint.y, tint.z, tint.w);
			mesh.setAux(v3, Base.color0_id, tint.x, tint.y, tint.z, tint.w);
			mesh.setAux(v4, Base.color0_id, tint.x, tint.y, tint.z, tint.w);

			mesh.setAux(v1, 4, 1, 1, 1, 1);
			mesh.setAux(v2, 4, 1, 1, 1, 1);
			mesh.setAux(v3, 4, 1, 1, 1, 1);
			mesh.setAux(v4, 4, 1, 1, 1, 1);

			mesh.setAux(v1, Base.texture0_id, 0, 0);
			mesh.setAux(v2, Base.texture0_id, 1, 0);
			mesh.setAux(v3, Base.texture0_id, 1, 1);
			mesh.setAux(v4, Base.texture0_id, 0, 1);

			mesh.nextFace(v1, v2, v3);
			mesh.nextFace(v1, v3, v4);
			mesh.close();

			int was = BasicGLSLangProgram.currentProgram.getShader();
			GL20.glUseProgram(was);

			glEnable(GL_DEPTH_TEST);

			System.out.println(" ---------- about to draw <" + mesh.getUnderlyingGeometry() + "> with program :");
			BasicGLSLangProgram.currentProgram.debugPrintUniforms();

			mesh.getUnderlyingGeometry().performPass(null);
			fbo.post();

			GL20.glUseProgram(was);

			CoreHelpers.glPopAttrib();

		}

		IntBuffer ii = ByteBuffer.allocateDirect(4*4).order(ByteOrder.nativeOrder()).asIntBuffer();
		
		public void moveCIImageToHere(Image c, int width, int height) {
			if (drawn != null && drawn.coreimage == c.coreimage)
				return;

			// GL gl = BasicContextManager.getGl();
			CoreHelpers.glPushAttrib(GL_ALL_ATTRIB_BITS);

			drawn = c;
			fbo.enter();

			int ff[] = new int[4];
			ii.rewind();
			GL11.glGetInteger(GL11.GL_VIEWPORT, ii);
			ii.get(ff);
			ii.rewind();
			
			glViewport(0, 0, this.width, this.height);
			CoreHelpers.glMatrixMode(GL_PROJECTION);
			CoreHelpers.glPushMatrix();
			CoreHelpers.glLoadIdentity();
			CoreHelpers.glOrtho(0, this.width, 0, this.height, -1, 1);
			CoreHelpers.glMatrixMode(GL_MODELVIEW);
			CoreHelpers.glPushMatrix();
			CoreHelpers.glLoadIdentity();
			glBlendFunc(GL_ONE, GL_ZERO);
			glEnable(GL_BLEND);

			glClearColor(1, 0, 1, 1);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			GL20.glUseProgram(0);

			new CoreImage().context_drawImageNow(getContext(), c.coreimage, 0, 0, width, height, 0, 0, width, height);

			fbo.exit();

			GL20.glUseProgram(BasicGLSLangProgram.currentProgram.getShader());
			CoreHelpers.glPopMatrix();
			CoreHelpers.glMatrixMode(GL_PROJECTION);
			CoreHelpers.glPopMatrix();
			CoreHelpers.glMatrixMode(GL_MODELVIEW);

			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			glEnable(GL_BLEND);
			glViewport(ff[0], ff[1], ff[2], ff[3]);

			CoreHelpers.glPopAttrib();

		}

		public void moveCIImageToHere(Image c, int x, int y, int width, int height) {
			if (drawn != null && drawn.coreimage == c.coreimage)
				return;

			// GL gl = BasicContextManager.getGl();
			CoreHelpers.glPushAttrib(GL_ALL_ATTRIB_BITS);

			drawn = c;
			fbo.enter();

			// int ff[] = new int[4];
			// ff[0] = glGetInteger(GL11.GL_VIEWPORT, 0);
			// ff[1] = glGetInteger(GL11.GL_VIEWPORT, 1);
			// ff[2] = glGetInteger(GL11.GL_VIEWPORT, 2);
			// ff[3] = glGetInteger(GL11.GL_VIEWPORT, 3);

			glViewport(0, 0, this.width, this.height);
			CoreHelpers.glMatrixMode(GL_PROJECTION);
			CoreHelpers.glPushMatrix();
			CoreHelpers.glLoadIdentity();
			CoreHelpers.glOrtho(0, this.width, 0, this.height, -1, 1);
			CoreHelpers.glMatrixMode(GL_MODELVIEW);
			CoreHelpers.glPushMatrix();
			CoreHelpers.glLoadIdentity();
			glBlendFunc(GL_ONE, GL_ZERO);
			glEnable(GL_BLEND);

			glClearColor(1, 1, 1, 1);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			GL20.glUseProgram(0);

			new CoreImage().context_drawImageNow(getContext(), c.coreimage, 0, 0, width, height, x, y, width, height);

			fbo.exit();

			GL20.glUseProgram(BasicGLSLangProgram.currentProgram.getShader());
			CoreHelpers.glPopMatrix();
			CoreHelpers.glMatrixMode(GL_PROJECTION);
			CoreHelpers.glPopMatrix();
			CoreHelpers.glMatrixMode(GL_MODELVIEW);

			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			glEnable(GL_BLEND);
			// glViewport(ff[0], ff[1], ff[2], ff[3]);
			CoreHelpers.glPopAttrib();

		}

		@Override
		protected void finalize() throws Throwable {
			System.out.println(" finalizing destination ");
			if (deletionQueue != null)
				fbo.delete(deletionQueue);
			super.finalize();
		}
	}

	static {
		FieldPyObjectAdaptor2.isHandlesAttributes(Filter.class);
	}

	public class Filter implements iHandlesAttributes {
		protected long filter;

		protected Filter(long name) {
			this.filter = name;
		}

		public Filter(String name) {
			filter = new CoreImage().filter_createWithName(name);
			new CoreImage().filter_setDefaults(filter);
		}

		public Object getAttribute(String name) {
			return getImage(name);
		}

		public Image getImage(String key) {
			if (checkOutput(key)) {
				long ii = new CoreImage().filter_getImage(filter, key);
				Image i = wrapImage(new Image(ii));
				return i;
			} else
				return null;
		}

		public List<String> inputs() {
			String s = new CoreImage().filter_getInputKeys(filter);
			String[] x = s.split("#");
			List<String> r = new ArrayList<String>();
			for (String ss : x) {
				ss = ss.trim();
				if (ss.length() > 0)
					r.add(ss);
			}

			System.out.println(" inputs are <" + s + "> -> " + r);
			return r;
		}

		public List<String> outputs() {
			String s = new CoreImage().filter_getOutputKeys(filter);
			String[] x = s.split("#");
			List<String> r = new ArrayList<String>();
			for (String ss : x) {
				ss = ss.trim();
				if (ss.length() > 0)
					r.add(ss);
			}
			return r;
		}

		public void set(String key, float value) {
			if (checkInput(key))
				new CoreImage().filter_setValueFloat(filter, key, value);
		}

		public void set(String key, Image value) {
			if (value.coreimage == 0) {
				throw new IllegalArgumentException("No image for '" + key + "'");
			}
			if (checkInput(key))
				new CoreImage().filter_setValueImage(filter, key, value.coreimage);
		}

		public void set(String key, Vector2 value) {
			if (checkInput(key))
				new CoreImage().filter_setValueVector2(filter, key, value.x, value.y);
		}

		public void set(String key, Vector4 value) {
			if (checkInput(key))
				new CoreImage().filter_setValueVector4(filter, key, value.x, value.y, value.z, value.w);
		}

		public void set(String key, Rect value) {
			if (checkInput(key))
				new CoreImage().filter_setValueVector4(filter, key, (float) value.x, (float) value.y, (float) value.w, (float) value.h);
		}

		public void setAttribute(String name, Object value) {
			if (value instanceof Number)
				set(name, ((Number) value).floatValue());
			else if (value instanceof Vector2)
				set(name, ((Vector2) value));
			else if (value instanceof Image)
				set(name, ((Image) value));
			else if (value instanceof Vector4)
				set(name, ((Vector4) value));
			else if (value instanceof Rect)
				set(name, ((Rect) value));
			else
				throw new ClassCastException(" can't handle type <" + value.getClass() + ">");
		}

		private boolean checkInput(String key) {
			return inputs().contains(key);
		}

		private boolean checkOutput(String key) {
			return outputs().contains(key);
		}

		@Override
		protected void finalize() throws Throwable {
			super.finalize();
			System.out.println(" finalize for filter <" + this.filter + ">");
			if (this.filter != 0) {
				new CoreImage().filter_release(filter);
				this.filter = 0;
			}
		}

		protected Image wrapImage(Image image) {
			return image;
		}

	}

	public class Accumulator {
		protected long accumulator;
		Image outputImage;

		public Accumulator(double x, double y, double w, double h, boolean isFloat) {
			accumulator = new CoreImage().accumulator_createWithExtent(x, y, w, h, isFloat);
			outputImage = new Image(new CoreImage().accumulator_getOutputImage(accumulator));
		}

		public Image getOutputImage() {
			return outputImage = new Image(new CoreImage().accumulator_getOutputImage(accumulator));
		}

		public void setImage(Image i) {
			Rect e = i.getExtents();
			new CoreImage().accumulator_setImage(accumulator, i.coreimage, e.x, e.y, e.w, e.h);
		}

		@Override
		protected void finalize() throws Throwable {
			super.finalize();
			if (this.accumulator != 0) {
				System.out.println(" releasing accumulator " + accumulator);
				new CoreImage().accumulator_release(accumulator);
				outputImage.coreimage = 0;
				this.accumulator = 0;
			}
		}

	}

	/**
	 * some structure ontop of those naked longs
	 */
	public class Image {
		public long coreimage;

		boolean blending = true;

		public Image(int[] raw, int width, int height) {
			ByteBuffer b = ByteBuffer.allocateDirect(raw.length * 4);
			for (int i = 0; i < raw.length; i++)
				b.putInt(raw[i]);
			b.rewind();
			coreimage = new CoreImage().image_createWithARGBData(b, width, height);
			System.out.println("A image <" + this + "> is pointing to <" + coreimage + ">");
		}

		public Image(byte[] raw, int width, int height) {
			ByteBuffer b = ByteBuffer.allocateDirect(raw.length);
			b.put(raw);
			b.rewind();
			coreimage = new CoreImage().image_createWithARGBData(b, width, height);
			System.out.println("A image <" + this + "> is pointing to <" + coreimage + ">");
		}

		public Image(FloatBuffer rgbaf, int width, int height) {
			coreimage = new CoreImage().image_createWithARGBFData(rgbaf, width, height);
			System.out.println("B image <" + this + "> is pointing to <" + coreimage + ">");
		}

		public Image(FloatBuffer greyf, int width, int height, boolean grey) {
			if (!grey) {
				coreimage = new CoreImage().image_createWithARGBFData(greyf, width, height);
				System.out.println("B image <" + this + "> is pointing to <" + coreimage + ">");
			} else {
				FloatBuffer a = ByteBuffer.allocateDirect(greyf.capacity() * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
				for (int i = 0; i < greyf.capacity(); i++) {
					a.put(greyf.get(i));
					a.put(greyf.get(i));
					a.put(greyf.get(i));
					a.put(1f);
				}
				coreimage = new CoreImage().image_createWithARGBFData(a, width, height);
				System.out.println("B image <" + this + "> is pointing to <" + coreimage + ">");
			}
		}

		public Image(ByteBuffer rgbaf, int width, int height) {
			coreimage = new CoreImage().image_createWithARGBData(rgbaf, width, height);
			System.out.println("B image <" + this + "> is pointing to <" + coreimage + ">");
		}

		public Image(int width, int height) {
			ByteBuffer b = ByteBuffer.allocateDirect(width * height * 4);
			for (int i = 0; i < b.capacity(); i++) {
				b.put((byte) (255 * Math.random()));
			}
			b.rewind();
			coreimage = new CoreImage().image_createWithARGBData(b, width, height);
			System.out.println("C image <" + this + "> is pointing to <" + coreimage + ">");
		}

		public Image(String url) {

			if (url.indexOf(":") == -1)
				try {
					url = new File(url).toURL().toExternalForm();
				} catch (MalformedURLException e) {
				}

			coreimage = new CoreImage().image_createWithURL(url);
			System.out.println("D image <" + this + "> is pointing to <" + coreimage + ">");
		}

		protected Image(long i) {
			coreimage = i;
			new CoreImage().image_retain(coreimage);
			System.out.println("E image <" + this + "> is pointing to <" + coreimage + ">");
		}

		public void drawNow(float x, float y) {
			if (coreimage == 0)
				System.out.println(" drawing released image");

			System.out.println(" drawing an image");

			drawNow(x, y, new CoreImage().image_getExtentWidth(coreimage), new CoreImage().image_getExtentHeight(coreimage), new CoreImage().image_getExtentX(coreimage), new CoreImage().image_getExtentY(coreimage), new CoreImage().image_getExtentWidth(coreimage), new CoreImage().image_getExtentHeight(coreimage));
		}

		public void drawNow(float x, float y, float scale) {
			if (coreimage == 0)
				System.out.println(" drawing released image");

			System.out.println(" drawing an image " + scale);

			drawNow(x, y, new CoreImage().image_getExtentWidth(coreimage) * scale, new CoreImage().image_getExtentHeight(coreimage) * scale, new CoreImage().image_getExtentX(coreimage), new CoreImage().image_getExtentY(coreimage), new CoreImage().image_getExtentWidth(coreimage), new CoreImage().image_getExtentHeight(coreimage));
		}

		public void drawNowRaw(float x, float y, float scale) {
			if (coreimage == 0)
				System.out.println(" drawing released image");

			System.out.println(" drawing an image " + scale);

			drawNowRaw(x, y, new CoreImage().image_getExtentWidth(coreimage) * scale, new CoreImage().image_getExtentHeight(coreimage) * scale, new CoreImage().image_getExtentX(coreimage), new CoreImage().image_getExtentY(coreimage), new CoreImage().image_getExtentWidth(coreimage), new CoreImage().image_getExtentHeight(coreimage));
		}

		public long getCoreimage() {
			return coreimage;
		}

		public void drawNow(float x, float y, float dw, float dh, float sx, float sy, float sw, float sh) {
			if (coreimage == 0) {
				System.out.println(" drawing released image");
				return;
			}

			System.out.println(" drawing an image " + x + " " + y + " " + dw + " " + dh + " " + sx + " " + sy + " " + sw + " " + sh);

			// System.out.println(" drawnow <" + x + " " + y + " " +
			// dw + " " + dh + " " + sx + " " + sy + " " + sw + " "
			// + sh);

			// glViewport(0, 0,
			// this.width, this.height);
			// glMatrixMode(GL_PROJECTION);
			// glPushMatrix();
			// glLoadIdentity();
			// glOrtho(0, this.width, 0,
			// this.height, -1, 1);
			// glMatrixMode(GL_MODELVIEW);
			// glPushMatrix();
			// glLoadIdentity();
			if (!blending)
				glBlendFunc(GL_ONE, GL_ZERO);
			else
				glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
			glEnable(GL_BLEND);

			// glClearColor(1, 1, 1, 1);
			// glClear(GL_COLOR_BUFFER_BIT
			// | GL_DEPTH_BUFFER_BIT);


			GL20.glUseProgram(0);
			
			GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			

			// glu.gluOrtho2D(0 + tx, w * sx + tx, h * sy + ty, ty);

			GLComponentWindow current = GLComponentWindow.getCurrentWindow(null);

			if (false) {

				CoreHelpers.glMatrixMode(GL_PROJECTION);
				CoreHelpers.glPushMatrix();
				CoreHelpers.glLoadIdentity();
				CoreHelpers.gluOrtho2D(0, (float) current.getWidth(), (float) current.getHeight(), 0);

				Vector2 topLeft = new Vector2(x, y);
				Vector2 bottomRight = new Vector2(x + dw, y + dh);

				current.transformDrawingToWindow(topLeft);
				current.transformDrawingToWindow(bottomRight);

				new CoreImage().context_drawImageNow(getContext(), coreimage, topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y, sx, sy, sw, sh);

				CoreHelpers.glPopMatrix();
			} else {

				System.out.println(" -- drawing plainly onto screen ");

				// new Exception().printStackTrace();
				if (!CoreHelpers.isCore)
					glEnable(GL_TEXTURE_2D);

				new CoreImage().context_drawImageNow(getContext(), coreimage, x, y, dw, dh, sx, sy, sw, sh);

			}
			GL20.glUseProgram(BasicGLSLangProgram.currentProgram.getShader());
			// glPopMatrix();
			// glMatrixMode(GL_PROJECTION);
			// glPopMatrix();
			// glMatrixMode(GL_MODELVIEW);
			//
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			glEnable(GL_BLEND);

			// glViewport(0, 0,
			// GLComponentWindow.getCurrentWindow(null).getCanvas().getWidth(),
			// GLComponentWindow.getCurrentWindow(null).getCanvas().getHeight());

		}

		public void drawNowRaw(float x, float y, float dw, float dh, float sx, float sy, float sw, float sh) {
			if (coreimage == 0) {
				System.out.println(" drawing released image");
				return;
			}

			glPushMatrix();
			System.out.println(" raw draw");
			new CoreImage().context_drawImageNow(getContext(), coreimage, x, y, dw, dh, sx, sy, sw, sh);
			glPopMatrix();

		}

		public Rect getExtents() {
			return new Rect(new CoreImage().image_getExtentX(coreimage), new CoreImage().image_getExtentY(coreimage), new CoreImage().image_getExtentWidth(coreimage), new CoreImage().image_getExtentHeight(coreimage));
		}

		FloatBuffer gotten = null;

		public FloatBuffer toFloatBuffer() {
			if (gotten != null)
				return gotten;

			Rect e = getExtents();
			FloatBuffer f = ByteBuffer.allocateDirect(4 * 4 * ((int) (e.w) * (int) (e.h))).order(ByteOrder.nativeOrder()).asFloatBuffer();
			gotten = f;
			new CoreImage().context_drawImageToFloatBuffer(getContextNoCreate(), coreimage, f, (int) e.w, (int) e.h);
			return f;
		}

		@Override
		protected void finalize() throws Throwable {
			super.finalize();
			System.out.println(" finalize for image <" + this.coreimage + ">");
			if (this.coreimage != 0) {
				// if (!alreadyFinalized.contains((long)
				// this.coreimage))
				// {
				new CoreImage().image_release(coreimage);
				// alreadyFinalized.add((long) this.coreimage);
				this.coreimage = 0;
				// }
			}
		}

		public void saveAsPNG(String url) {
			System.out.println(" saving...");
			new CoreImage().image_save(context, this.coreimage, url, "public.png");
			System.out.println(" ... saving complete");
		}

		public void saveAsTIFF(String url) {
			System.out.println(" saving...");
			new CoreImage().image_save(context, this.coreimage, url, "public.tiff");
			System.out.println(" ... saving complete");
		}

		public void drawNow3d(float x, float y, float z) {

			if (coreimage == 0) {
				System.out.println(" drawing released image");
				return;
			}

			if (!blending)
				glBlendFunc(GL_ONE, GL_ZERO);
			else
				glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
			glEnable(GL_BLEND);

			GL20.glUseProgram(0);

			GLComponentWindow current = GLComponentWindow.getCurrentWindow(null);

			glMatrixMode(GL_MODELVIEW);
			glPushMatrix();

			System.out.println(" drawing with depth <" + z + ">");

			glTranslatef(0, 0, z);

			float scale = 1;

			new CoreImage().context_drawImageNow(getContext(), coreimage, x, y, new CoreImage().image_getExtentWidth(coreimage) * scale, new CoreImage().image_getExtentHeight(coreimage) * scale, new CoreImage().image_getExtentX(coreimage), new CoreImage().image_getExtentY(coreimage), new CoreImage().image_getExtentWidth(coreimage), new CoreImage().image_getExtentHeight(coreimage));
			glPopMatrix();

			GL20.glUseProgram(BasicGLSLangProgram.currentProgram.getShader());
			// glPopMatrix();
			// glMatrixMode(GL_PROJECTION);
			// glPopMatrix();
			// glMatrixMode(GL_MODELVIEW);
			//
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			glEnable(GL_BLEND);

			// glViewport(0, 0,
			// GLComponentWindow.getCurrentWindow(null).getCanvas().getWidth(),
			// GLComponentWindow.getCurrentWindow(null).getCanvas().getHeight());

		}
	}

	long context;

	public Filter customFilter(String text, List<String> args) {
		String a = "";
		for (String s : args) {
			if (a.length() > 0)
				a = a + "#";
			a += s;
		}
		long f = new CoreImage().filter_createGenericWithText(text, a);
		return new Filter(f);
	}

	public Filter filter(String byName) {
		return new Filter(byName);
	}

	public long getContext() {
		if (context == 0) {
			context = new CoreImage().context_createOpenGLCIContextNow();
		}
		return context;
	}

	public long getContextNoCreate() {
		if (context == 0) {
			return new CoreImage().context_createOpenGLCIContextNow();
		}
		return context;
	}

	public Image image(String url) {
		return new Image(url);
	}

	// public void initialize(iVisualElement on)
	// {
	// GLComponentWindow w;
	//
	//
	// w.
	// }

}
