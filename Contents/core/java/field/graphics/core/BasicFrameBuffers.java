package field.graphics.core;

import static org.lwjgl.opengl.ARBPixelBufferObject.GL_PIXEL_PACK_BUFFER_ARB;
import static org.lwjgl.opengl.ARBTextureRg.GL_RG;
import static org.lwjgl.opengl.ARBTextureRg.GL_RG16F;
import static org.lwjgl.opengl.ARBTextureRg.GL_RG8;
import static org.lwjgl.opengl.GL11.GL_ALWAYS;
import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColorMask;
import static org.lwjgl.opengl.GL11.glCopyTexSubImage2D;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDrawBuffer;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glReadBuffer;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameterf;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT16;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT1;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT2;
import static org.lwjgl.opengl.GL30.GL_DEPTH24_STENCIL8;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_HALF_FLOAT;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_RENDERBUFFER;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindRenderbuffer;
import static org.lwjgl.opengl.GL30.glBlitFramebuffer;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glDeleteRenderbuffers;
import static org.lwjgl.opengl.GL30.glFramebufferRenderbuffer;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenRenderbuffers;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.opengl.GL30.glRenderbufferStorage;
import static org.lwjgl.opengl.GL30.glRenderbufferStorageMultisample;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.NVPathRendering;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.DispatchOverTopology;
import field.bytecode.protect.annotations.HiddenInAutocomplete;
import field.bytecode.protect.dispatch.Cont;
import field.bytecode.protect.dispatch.Cont.ReturnCode;
import field.bytecode.protect.dispatch.Cont.aRun;
import field.core.dispatch.iVisualElement.Rect;
import field.graphics.core.Base.StandardPass;
import field.graphics.core.Base.iAcceptsSceneListElement;
import field.graphics.core.Base.iGeometry;
import field.graphics.core.Base.iPass;
import field.graphics.core.Base.iSceneListElement;
import field.graphics.core.BasicGeometry.Instance;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.graphics.core.BasicTextures.BaseTexture;
import field.graphics.core.BasicTextures.TextureUnit;
import field.graphics.core.BasicUtilities.OnePassElement;
import field.graphics.core.BasicUtilities.OnePassListElement;
import field.graphics.imageprocessing.ImageProcessing.TextureWrapper;
import field.graphics.imageprocessing.ImageProcessing.iReposition;
import field.graphics.windowing.FullScreenCanvasSWT;
import field.launch.SystemProperties;
import field.launch.iUpdateable;
import field.math.BaseMath;
import field.math.abstraction.iAcceptor;
import field.math.abstraction.iFloatProvider;
import field.math.abstraction.iProvider;
import field.math.graph.NodeImpl;
import field.math.graph.iMutable;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Matrix4;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.namespace.generic.ReflectionTools;
import field.util.LinkedHashMapOfLists;
import field.util.TaskQueue;

/**
 * support for FBO's
 * 
 * @author marc
 * 
 */
@Woven
public class BasicFrameBuffers {

	static public boolean use32 = false;

	static public class Any implements iMatchRule {
		public boolean match(Object o) {
			return true;
		}
	}

	static public interface iDisplayable {
		public void display();
	}

	static public interface iHasFBO {
		public int getFBO();
	}

	static public interface iHasRB {
		public int getRB();
	}

	static public interface iHasTexture {
		public iProvider<Integer> getOutput();
	}

	static public class BaseFrameBufferObjectTexture extends BasicTextures.BaseTexture implements iDisplayable {
		private final int width;

		private final int height;

		private int status;

		private final boolean useRect;

		private float r;

		private float g;

		private float b;

		private float a;

		private boolean doClear;

		private final int target;

		protected BasicCamera camera;

		protected BasicSceneList rootSceneList;

		protected BasicSceneList sceneList;

		int[] fbo = { -1 };

		int[] rb = { -1 };

		int[] tex = { -1 };

		int internalFormat = GL_RGBA;

		int format = GL_RGBA;

		int type = GL_UNSIGNED_BYTE;

		boolean isFloat = false;

		public BaseFrameBufferObjectTexture(int width, int height, boolean useRect) {
			this.width = width;
			this.height = height;
			this.useRect = useRect;
			target = useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D;
			// camera.setViewport(0, 0,
			// width, height);
			// camera.setPerspective(camera.getFov(),
			// width / (float) height,
			// camera.getNear(),
			// camera.getFar());

			r = (float) SystemProperties.getDoubleProperty("background.red", 0);
			g = (float) SystemProperties.getDoubleProperty("background.green", 0);
			b = (float) SystemProperties.getDoubleProperty("background.blue", 0);
			a = (float) SystemProperties.getDoubleProperty("background.alpha", 1);

			doClear = SystemProperties.getIntProperty("background.clear", 1) == 1;
			createInitialLists();
		}

		public BaseFrameBufferObjectTexture becomeFloat() {
			this.internalFormat = GL_RGBA32F;
			this.format = GL_RGBA;
			this.type = (use32 ? GL_FLOAT : GL_HALF_FLOAT);
			isFloat = true;
			createInitialLists();
			return this;
		}

		public int bindForFrameBuffer() {
			int[] a = new int[1];
			a[0] = glGetInteger(GL_FRAMEBUFFER_BINDING);
			glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
			return a[0];
		}

		public void display() {
			if (FullScreenCanvasSWT.dropFrame > 0)
				return;

			currentFBOContext.push(this);
			try {
				gl = BasicContextManager.getGl();
				glu = BasicContextManager.getGlu();
				if (BasicContextManager.getId(this) == BasicContextManager.ID_NOT_FOUND)
					setup();

				glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
				glViewport(0, 0, width, height);
				if (isFloat) {
					CoreHelpers.glEnable(GL_DEPTH_TEST);
					glDepthFunc(GL_ALWAYS);
					glDepthMask(true);
				}
				assert glGetError() == 0;
				assert glGetError() == 0;

				// glClearColor(0.3f,
				// 0.5f, 0.2f,
				// 0);
				// glClear(GL_COLOR_BUFFER_BIT
				// );

				rootSceneList.update();
				sceneList.update();

				if (isFloat) {
					glDepthFunc(GL_LESS);
					CoreHelpers.glEnable(GL_DEPTH_TEST);
					glDepthMask(true);
				}

				// glFlush();
				glBindFramebuffer(GL_FRAMEBUFFER, 0);
				assert glGetError() == 0;
			} finally {
				Object popped = currentFBOContext.pop();
				assert popped == this : popped;
			}
		}

		public BasicCamera getCamera() {
			return camera;
		}

		public int getHeight() {
			return height;
		}

		// for debugging only, we should have a frame
		// buffer that supports async reads from a
		// renderbuffer as well
		public ByteBuffer getImage(ByteBuffer storage) {
			if (storage == null) {
				storage = ByteBuffer.allocateDirect(width * height * 4);
			}

			int[] a = new int[1];
			a[0] = glGetInteger(GL_FRAMEBUFFER_BINDING);
			glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
			glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, storage);
			glBindFramebuffer(GL_FRAMEBUFFER, a[0]);

			return storage;
		}

		public BasicSceneList getSceneList() {
			return sceneList;
		}

		public BasicSceneList getList() {
			return sceneList;
		}

		// I guess this should be called at the end of
		// display(...) in full screen canvas

		public int getWidth() {
			return width;
		}

		public BaseFrameBufferObjectTexture setBackground(float r, float g, float b, float a, boolean doClear) {
			this.r = r;
			this.g = g;
			this.b = b;
			this.a = a;
			this.doClear = doClear;
			createInitialLists();

			return this;
		}

		public void unbindForFrameBuffer(int a) {
			glBindFramebuffer(GL_FRAMEBUFFER, a);
		}

		protected void createInitialLists() {
			// defaults for color

			// camera = new BasicCamera();

			rootSceneList = new BasicSceneList();
			rootSceneList.addChild(new BasicUtilities.Standard());
			if (doClear) {
				// BasicUtilities.Clear
				// clear = new
				// BasicUtilities.Clear(new
				// Vector3(0,0,0),
				// (float) 1);
				if (!isFloat) {
					BasicUtilities.Clear clear = new BasicUtilities.Clear(new Vector3(r, g, b), a);
					rootSceneList.addChild(clear);
				}

				// rootSceneList.addChild(new
				// BasicUtilities.MotionBlur(new
				// iFloatProvider.Constant(1),0,0,0));

			} else {
				if (!isFloat) {
					rootSceneList.addChild(new BasicUtilities.ClearOnce(new Vector3(0, 0, 0), 1));
				}
			}
			// rootSceneList.addChild(camera);
			if (sceneList == null)
				sceneList = new BasicSceneList();
		}

		@Override
		protected void post() {
			assert glGetError() == 0;
			glBindTexture(target, 0);
			assert glGetError() == 0;
			CoreHelpers.glDisable(target);
			assert glGetError() == 0;
		}

		@Override
		protected void pre() {

			assert glGetError() == 0;
			glBindTexture(target, tex[0]);
			assert glGetError() == 0;
			CoreHelpers.glEnable(target);
			assert glGetError() == 0;
		}

		@Override
		protected void setup() {
			assert glGetError() == 0;
			fbo[0] = glGenFramebuffers();
			assert glGetError() == 0;
			rb[0] = glGenRenderbuffers();
			assert glGetError() == 0;
			tex[0] = glGenTextures();
			assert glGetError() == 0;

			glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
			assert glGetError() == 0;

			glBindTexture(target, tex[0]);
			assert glGetError() == 0;
			glTexImage2D(target, 0, internalFormat, width, height, 0, format, type, (ByteBuffer) null);
			assert glGetError() == 0;
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, target, tex[0], 0);
			if (!isFloat) {
				assert glGetError() == 0;
				glTexParameteri(target, GL_TEXTURE_WRAP_S, gl_texture_wrap_s);
				assert glGetError() == 0;
				glTexParameteri(target, GL_TEXTURE_WRAP_T, gl_texture_wrap_t);
				assert glGetError() == 0;
				glTexParameteri(target, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
				assert glGetError() == 0;
				glTexParameteri(target, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);
				assert glGetError() == 0;

				glBindRenderbuffer(GL_RENDERBUFFER, rb[0]);
				glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
				glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rb[0]);
			}
			status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
			assert status == GL_FRAMEBUFFER_COMPLETE : "status is <" + status + ">";
			assert glGetError() == 0;
			BasicContextManager.putId(this, fbo[0]);

			glClearColor(0, 0, 0, 0);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			glBindFramebuffer(GL_FRAMEBUFFER, 0);
		}
	}

	static public class BiMultipasser extends MultiPasser implements iDisplayable {

		private final boolean useFloat;

		protected int[] secondTex = new int[2];

		protected final int secondUnit;

		public BiMultipasser(int width, int height, boolean useRect, int secondUnit) {
			super(width, height, useRect);
			this.secondUnit = secondUnit;
			this.useFloat = false;
		}

		public BiMultipasser(int width, int height, boolean useRect, int secondUnit, boolean useFloat) {
			super(width, height, useRect);
			this.secondUnit = secondUnit;
			this.useFloat = useFloat;
		}

		@Override
		public void bindOtherTexture() {
			super.bindOtherTexture();

			int[] acitve = { 0 };
			acitve[0] = glGetInteger(GL_ACTIVE_TEXTURE);

			glActiveTexture(GL_TEXTURE0 + secondUnit);

			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, flip ? secondTex[1] : secondTex[0]);
			glActiveTexture(acitve[0]);
		}

		@Override
		protected void post() {
			super.post();

			int[] acitve = { 0 };
			acitve[0] = glGetInteger(GL_ACTIVE_TEXTURE);
			glActiveTexture(GL_TEXTURE0 + secondUnit);

			assert !deallocated;
			assert glGetError() == 0;
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
			assert glGetError() == 0;
			CoreHelpers.glDisable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			assert glGetError() == 0;
			glActiveTexture(acitve[0]);

		}

		@Override
		protected void postDisplay() {
			super.postDisplay();
			assert glGetError() == 0;
			glDrawBuffer(GL_BACK);
			assert glGetError() == 0;
		}

		@Override
		protected void pre() {

			super.pre();
			int[] acitve = { 0 };
			assert glGetError() == 0;
			acitve[0] = glGetInteger(GL_ACTIVE_TEXTURE);
			assert glGetError() == 0;
			glActiveTexture(GL_TEXTURE0 + secondUnit);

			assert !deallocated;
			assert glGetError() == 0;

			if (flip) {
				glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, secondTex[0]);
				CoreHelpers.glEnable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			} else {
				glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, secondTex[1]);
				CoreHelpers.glEnable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			}

			glActiveTexture(acitve[0]);
			assert glGetError() == 0;

		}

		@Override
		protected void preDisplay() {
			super.preDisplay();
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, flip ? secondTex[0] : secondTex[1], 0);
			// TODO lwjgl momentum
			// glDrawBuffers(2, new int[] { GL_COLOR_ATTACHMENT0,
			// GL_COLOR_ATTACHMENT1 }, 0);
			assert glGetError() == 0;
		}

		@Override
		protected void setup() {
			assert !deallocated;
			assert glGetError() == 0;

			fbo[0] = glGenFramebuffers();
			assert glGetError() == 0;

			rb[0] = glGenRenderbuffers();
			assert glGetError() == 0;

			tex[0] = glGenTextures();
			tex[1] = glGenTextures();
			secondTex[0] = glGenTextures();
			secondTex[1] = glGenTextures();
			assert glGetError() == 0;

			assert glGetError() == 0;

			glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);

			gl_texture_min_filter = GL_LINEAR;
			gl_texture_mag_filter = GL_LINEAR;

			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[0]);
			glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0, useFloat ? (use32 ? GL_RGBA32F : GL_RGBA16F) : GL_RGBA8, width, height, 0, GL_RGBA, useFloat ? (use32 ? GL_FLOAT : GL_HALF_FLOAT) : GL_UNSIGNED_BYTE, (ByteBuffer) null);
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[0], 0);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, gl_texture_wrap_s);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, gl_texture_wrap_t);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);

			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, secondTex[0]);
			glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0, useFloat ? (use32 ? GL_RGBA32F : GL_RGBA16F) : GL_RGBA8, width, height, 0, GL_RGBA, useFloat ? (use32 ? GL_FLOAT : GL_HALF_FLOAT) : GL_UNSIGNED_BYTE, (ByteBuffer) null);
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, secondTex[0], 0);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, gl_texture_wrap_s);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, gl_texture_wrap_t);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);

			glBindRenderbuffer(GL_RENDERBUFFER, rb[0]);
			glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, width, height);
			glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rb[0]);
			status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
			assert status == GL_FRAMEBUFFER_COMPLETE : status;
			;// System.out.println(" status <" + status + "> <" +
				// GL_FRAMEBUFFER_COMPLETE + ">");
			BasicContextManager.putId(this, fbo[0]);
			glClearColor(0, 0, 0, 0);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			glBindFramebuffer(GL_FRAMEBUFFER, 0);
			assert glGetError() == 0;

			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[1]);
			glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0, useFloat ? (use32 ? GL_RGBA32F : GL_RGBA16F) : GL_RGBA8, width, height, 0, GL_RGBA, useFloat ? (use32 ? GL_FLOAT : GL_HALF_FLOAT) : GL_UNSIGNED_BYTE, (ByteBuffer) null);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, gl_texture_wrap_s);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, gl_texture_wrap_t);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);
			assert glGetError() == 0;

			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, secondTex[1]);
			glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0, useFloat ? (use32 ? GL_RGBA32F : GL_RGBA16F) : GL_RGBA8, width, height, 0, GL_RGBA, useFloat ? (use32 ? GL_FLOAT : GL_HALF_FLOAT) : GL_UNSIGNED_BYTE, (ByteBuffer) null);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, gl_texture_wrap_s);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, gl_texture_wrap_t);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);
			assert glGetError() == 0;

		}

	}

	static public class DepthMultipasser extends MultiPasser implements iDisplayable {

		protected int[] depthTex = new int[2];

		// protected int[] rbDepth = new int[1];

		protected final int depthUnit;

		public DepthMultipasser(int width, int height, boolean useRect, int depthUnit) {
			super(width, height, useRect);
			fbo = new int[] { -1, -1 };
			depthTex = new int[] { -1, -1 };
			tex = new int[] { -1, -1 };

			this.depthUnit = depthUnit;

			assert useRect == false;
			if (useRect) {
				System.err.println(" on ati this will cause a kernel panic");
				System.exit(1);
			}

		}

		@Override
		public void bindOtherTexture() {
			super.bindOtherTexture();

			int[] acitve = { 0 };
			acitve[0] = glGetInteger(GL_ACTIVE_TEXTURE);

			glActiveTexture(GL_TEXTURE0 + depthUnit);

			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, flip ? depthTex[1] : depthTex[0]);
			glActiveTexture(acitve[0]);
		}

		@Override
		@DispatchOverTopology(topology = Cont.class)
		public void display() {
			if (FullScreenCanvasSWT.dropFrame > 0)
				return;

			assert !deallocated;
			assert glGetError() == 0;
			glBindFramebuffer(GL_FRAMEBUFFER, flip ? fbo[0] : fbo[1]);
			assert glGetError() == 0;
			assert glGetError() == 0;
			preDisplay();
			assert glGetError() == 0;
			glViewport(0, 0, width, height);
			assert glGetError() == 0;

			if (fboCamera != null)
				fboCamera.enter();

			assert glGetError() == 0;
			rootSceneList.update();
			assert glGetError() == 0;
			if (first) {
				first = false;
			} else {
				sceneList.update();
			}
			assert glGetError() == 0;

			queue.update();

			assert glGetError() == 0;
			glBindFramebuffer(GL_FRAMEBUFFER, 0);
			assert glGetError() == 0;

			postDisplay();
			assert glGetError() == 0;
			flip = !flip;

		}

		@Override
		protected void post() {
			super.post();

			int[] acitve = { 0 };
			acitve[0] = glGetInteger(GL_ACTIVE_TEXTURE);
			glActiveTexture(GL_TEXTURE0 + depthUnit);

			assert !deallocated;
			assert glGetError() == 0;
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
			assert glGetError() == 0;
			CoreHelpers.glDisable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			assert glGetError() == 0;
			glActiveTexture(acitve[0]);
		}

		@Override
		protected void pre() {

			super.pre();

			int[] acitve = { 0 };
			assert glGetError() == 0;
			acitve[0] = glGetInteger(GL_ACTIVE_TEXTURE);
			assert glGetError() == 0;
			glActiveTexture(GL_TEXTURE0 + depthUnit);

			assert !deallocated;
			assert glGetError() == 0;

			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 0);
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, depthTex[0]);
			CoreHelpers.glEnable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);

			glActiveTexture(acitve[0]);
			assert glGetError() == 0;
		}

		@Override
		protected void setup() {

			assert !deallocated;
			assert glGetError() == 0;

			fbo[0] = glGenFramebuffers();
			fbo[1] = glGenFramebuffers();
			tex[0] = glGenTextures();
			tex[1] = glGenTextures();
			depthTex[0] = glGenTextures();
			depthTex[1] = glGenTextures();
			assert glGetError() == 0;

			{
				glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);

				gl_texture_min_filter = GL_LINEAR_MIPMAP_LINEAR;
				gl_texture_mag_filter = GL_LINEAR;

				// texture 0

				glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[0]);
				// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE,
				// 0);

				glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, gl_texture_wrap_s);
				glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, gl_texture_wrap_t);
				glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
				glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);

				glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
				glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[0], 0);

				// and now the
				// depth texture

				glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, depthTex[0]);
				glTexParameterf(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
				glTexParameterf(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
				glTexParameterf(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
				glTexParameterf(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
				glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, width, height, 0, GL_DEPTH_COMPONENT, (use32 ? GL_FLOAT : GL_HALF_FLOAT), (ByteBuffer) null);
				glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, depthTex[0], 0);

				assert glGetError() == 0;

				status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
				assert status == GL_FRAMEBUFFER_COMPLETE : status;
				BasicContextManager.putId(this, fbo[0]);

				glClearColor(0, 0, 0, 0);
				glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

				glBindFramebuffer(GL_FRAMEBUFFER, 0);
				assert glGetError() == 0;
				glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
			}
			{

				glBindFramebuffer(GL_FRAMEBUFFER, fbo[1]);

				gl_texture_min_filter = GL_LINEAR_MIPMAP_LINEAR;
				gl_texture_mag_filter = GL_LINEAR;

				// texture 0

				glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[1]);
				// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE,
				// 0);

				glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, gl_texture_wrap_s);
				glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, gl_texture_wrap_t);
				glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
				glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);

				glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
				glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[1], 0);

				// and now the
				// depth texture

				glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, depthTex[1]);
				glTexParameterf(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
				glTexParameterf(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
				glTexParameterf(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
				glTexParameterf(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
				glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, width, height, 0, GL_DEPTH_COMPONENT, (use32 ? GL_FLOAT : GL_HALF_FLOAT), (ByteBuffer) null);
				glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, depthTex[1], 0);

				assert glGetError() == 0;

				status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
				assert status == GL_FRAMEBUFFER_COMPLETE : status;
				BasicContextManager.putId(this, fbo[0]);

				glClearColor(0, 0, 0, 0);
				glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

				glBindFramebuffer(GL_FRAMEBUFFER, 0);
				assert glGetError() == 0;
				glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);

			}

			assert glGetError() == 0;
		}

	}

	static public class DoubleFrameBuffer extends BasicTextures.BaseTexture implements iDisplayable {
		private final int width;

		private int status;

		private BasicSceneList rootSceneList;

		private BasicSceneList sceneList;

		private Vector4 c1;

		private Vector4 c2;

		private final int height;

		int[] fbo = { -1 };

		int[] rb = { -1 };

		int[] tex = { -1, -1 };

		boolean useRect = false;

		boolean useFloat = true;

		public DoubleFrameBuffer(int width) {
			this.width = width;
			this.height = width;
			createInitialLists();
			this.textureTarget = useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D;
		}

		public DoubleFrameBuffer(int depthWidth, int depthHeight) {
			this.width = depthWidth;
			this.height = depthHeight;
			this.textureTarget = useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D;
			createInitialLists();
		}

		public DoubleFrameBuffer(int depthWidth, int depthHeight, boolean useFloat) {
			this.width = depthWidth;
			this.height = depthHeight;
			this.useFloat = useFloat;
			this.textureTarget = useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D;
			createInitialLists();
		}

		public DoubleFrameBuffer(int depthWidth, int depthHeight, boolean useRect, boolean useFloat) {
			this.width = depthWidth;
			this.height = depthHeight;
			this.useRect = useRect;
			this.useFloat = useFloat;
			this.textureTarget = useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D;
			createInitialLists();
		}

		public iHasFBO getFBO(final int out) {
			return new iHasFBO() {

				@Override
				public int getFBO() {
					return fbo[out];
				}
			};
		}

		public iProvider<Integer> getFBOOutput(final int out) {
			return new iProvider<Integer>() {
				@Override
				public Integer get() {
					return fbo[out];
				}
			};
		}

		public iAcceptor<Number> addFadePlane(final Vector4 color1, final Vector4 color2) {
			final TriangleMesh mesh = new BasicGeometry.TriangleMesh(Base.StandardPass.render);
			mesh.rebuildTriangle(2);
			mesh.rebuildVertex(4);

			mesh.vertex().put(-1f).put(-1f).put(0f).put(-1f).put(1f).put(0f).put(1f).put(1f).put(0f).put(1f).put(-1f).put(0f);
			mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 0).put((short) 2).put((short) 3);
			mesh.addChild(new BasicGLSLangProgram("content/shaders/NDC2ColorVertex.glslang", "content/shaders/VertexColor2Fragment.glslang"));
			mesh.addChild(new BasicUtilities.DepthMask(Base.StandardPass.transform, Base.StandardPass.postRender));

			float colorAlpha = 0.1f;
			float alphaAlpha = 0.5f;
			mesh.aux(Base.color0_id, 4).put(new float[] { color1.x, color1.y, color1.z, color1.w, color1.x, color1.y, color1.z, color1.w, color1.x, color1.y, color1.z, color1.w, color1.x, color1.y, color1.z, color1.w, });
			mesh.aux(Base.color0_id + 1, 4).put(new float[] { color2.x, color2.y, color2.z, color2.w, color2.x, color2.y, color2.z, color2.w, color2.x, color2.y, color2.z, color2.w, color2.x, color2.y, color2.z, color2.w, });

			final FloatBuffer root = ByteBuffer.allocate(mesh.vertex().limit() * 4).asFloatBuffer().put(mesh.vertex());

			// driver bug. Horrible seam
			// down middle of trianglulation
			mesh.addChild(new BasicUtilities.OnePassElement(StandardPass.preRender) {
				boolean first = true;

				@Override
				public void performPass() {
				}
			});
			// mesh.addChild(new
			// BasicUtilities.SetBlendMode(StandardPass.preRender,
			// GL_CONSTANT_ALPHA,
			// GL_ONE_MINUS_CONSTANT_ALPHA,
			// new Vector4(0, 0, 0,
			// 0.05f)));
			// mesh.addChild(new
			// BasicUtilities.SetBlendMode(StandardPass.preDisplay,
			// GL_SRC_ALPHA,
			// GL_ONE_MINUS_SRC_ALPHA,
			// new Vector4(0, 0, 0,
			// 0.05f)));

			rootSceneList.addChild(mesh);

			return new iAcceptor<Number>() {

				float last = -1;

				@Override
				public iAcceptor<Number> set(Number to) {
					if (to.floatValue() != last) {
						mesh.aux(Base.color0_id, 4).put(new float[] { color1.x, color1.y, color1.z, to.floatValue(), color1.x, color1.y, color1.z, to.floatValue(), color1.x, color1.y, color1.z, to.floatValue(), color1.x, color1.y, color1.z, to.floatValue(), });
						mesh.aux(Base.color0_id + 1, 4).put(new float[] { color2.x, color2.y, color2.z, to.floatValue(), color2.x, color2.y, color2.z, to.floatValue(), color2.x, color2.y, color2.z, to.floatValue(), color2.x, color2.y, color2.z, to.floatValue(), });
					}
					last = to.floatValue();
					return this;
				}
			};
		}

		public void addFadePlane(iFloatProvider amount1, Vector4 color1, iFloatProvider amount2, Vector4 color2) {
			final TriangleMesh mesh = new BasicGeometry.TriangleMesh(Base.StandardPass.render);
			mesh.rebuildTriangle(2);
			mesh.rebuildVertex(4);

			mesh.vertex().put(-1f).put(-1f).put(0f).put(-1f).put(1f).put(0f).put(1f).put(1f).put(0f).put(1f).put(-1f).put(0f);
			mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 0).put((short) 2).put((short) 3);
			mesh.addChild(new BasicGLSLangProgram("content/shaders/NDC2ColorVertex.glslang", "content/shaders/VertexColor2Fragment.glslang"));
			mesh.addChild(new BasicUtilities.DepthMask(Base.StandardPass.transform, Base.StandardPass.postRender));

			float colorAlpha = 0.1f;
			float alphaAlpha = 0.5f;
			mesh.aux(Base.color0_id, 4).put(new float[] { 0, 0, 0, colorAlpha, 0, 0, 0, colorAlpha, 0, 0, 0, colorAlpha, 0, 0, 0, colorAlpha });
			mesh.aux(Base.color0_id + 1, 4).put(new float[] { 0.5f, 0.5f, 0.5f, alphaAlpha, 0.5f, 0.5f, 0.5f, alphaAlpha, 0.5f, 0.5f, 0.5f, alphaAlpha, 0.5f, 0.5f, 0.5f, alphaAlpha });

			final FloatBuffer root = ByteBuffer.allocate(mesh.vertex().limit() * 4).asFloatBuffer().put(mesh.vertex());

			// driver bug. Horrible seam
			// down middle of trianglulation
			mesh.addChild(new BasicUtilities.OnePassElement(StandardPass.preRender) {
				boolean first = true;

				@Override
				public void performPass() {

					// if (!first)
					// glColorMask(false, false, false,
					// true);
					// glClearColor(0, 0, 0, 0);
					// glClear(GL_COLOR_BUFFER_BIT);
					// glColorMask(true, true, true,
					// true);
					//
					// first = false;

					// FloatBuffer
					// m =
					// mesh.vertex();
					// for
					// (int
					// i =
					// 0; i
					// <
					// m.capacity();
					// i++)
					// {
					// m.put(i,
					// (float)
					// (root.get(i)
					// * (1
					// +
					// Math.random()
					// *
					// 0.01f)));
					// }

				}
			});
			// mesh.addChild(new
			// BasicUtilities.SetBlendMode(StandardPass.preRender,
			// GL_CONSTANT_ALPHA,
			// GL_ONE_MINUS_CONSTANT_ALPHA,
			// new Vector4(0, 0, 0,
			// 0.05f)));
			// mesh.addChild(new
			// BasicUtilities.SetBlendMode(StandardPass.preDisplay,
			// GL_SRC_ALPHA,
			// GL_ONE_MINUS_SRC_ALPHA,
			// new Vector4(0, 0, 0,
			// 0.05f)));

			rootSceneList.addChild(mesh);
		}

		IntBuffer buffers = ByteBuffer.allocateDirect(4 * 2).order(ByteOrder.nativeOrder()).asIntBuffer();
		boolean disable2 = false;

		public void display() {
			;// System.out.println(" --> ");
			currentFBOContext.push(this);
			BasicGeometry.insideDoubleFloatFrameBuffer = false;
			if (FullScreenCanvasSWT.dropFrame > 0)
				return;

			try {
				gl = BasicContextManager.getGl();
				glu = BasicContextManager.getGlu();
				if (BasicContextManager.getId(this) == BasicContextManager.ID_NOT_FOUND) {
					// assert
					// false;
					// System.exit(0);
					setup();
				}

				glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);

				glDrawBuffer(GL_COLOR_ATTACHMENT0);

				if (c1 != null) {
					glClearColor(c1.x, c1.y, c1.z, c1.w);
					glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
				} else {
					glClear(GL_DEPTH_BUFFER_BIT);
				}

				if (!disable2) {
					glDrawBuffer(GL_COLOR_ATTACHMENT1);
					if (c2 != null) {
						glClearColor(c2.x, c2.y, c2.z, c2.w);
						glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
					} else {
						glClear(GL_DEPTH_BUFFER_BIT);
					}
				}
				// TODO lwjgl momentum
				// glDrawBuffers(2, new int[] {
				// GL_COLOR_ATTACHMENT0,
				// GL_COLOR_ATTACHMENT1 }, 0);

				if (disable2) {
					glDrawBuffer(GL_COLOR_ATTACHMENT0);
				} else {
					buffers.put(GL_COLOR_ATTACHMENT0).put(GL_COLOR_ATTACHMENT1).rewind();
					glDrawBuffers(buffers);
				}
				glClear(GL_DEPTH_BUFFER_BIT);

				glViewport(0, 0, width, height);
				rootSceneList.update();
				glViewport(0, 0, width, height);
				sceneList.update();

				// glFlush();
				glBindFramebuffer(GL_FRAMEBUFFER, 0);

				glDrawBuffer(GL_BACK);

				assert glGetError() == 0;
			} finally {
				Object popped = currentFBOContext.pop();
				BasicGeometry.insideDoubleFloatFrameBuffer = false;

				assert popped == this : popped;
				;// System.out.println(" <-- ");

			}
		}

		public void copyToVBO(final TriangleMesh mesh, final int aux, final boolean first) {
			sceneList.add(StandardPass.preDisplay).register("__copyToVbo__" + System.identityHashCode(mesh) + " " + aux + " " + first, new iUpdateable() {
				@Override
				public void update() {
					;// System.out.println(" copying to aux buffer ");
					copyToVBONow(mesh, aux, first);
				}
			});
		}

		protected void copyToVBONow(TriangleMesh mesh, int aux, boolean first) {
			int target = mesh.getOpenGLBufferName(aux);

			if (target == -1)
				return;

			// glFinish();
			int e = glGetError();
			;// System.out.println(" error1 : "+e);
			glBindBuffer(GL_PIXEL_PACK_BUFFER_ARB, target);
			e = glGetError();
			;// System.out.println(" error2 : "+e);
			GL11.glReadBuffer(first ? GL_COLOR_ATTACHMENT0 : GL_COLOR_ATTACHMENT1);
			e = glGetError();
			;// System.out.println(" error3 : "+e);
			glReadPixels(0, 0, width, height, GL_RGBA, GL_FLOAT, 0);
			e = glGetError();
			;// System.out.println(" error4 : "+e);
			glBindBuffer(GL_PIXEL_PACK_BUFFER_ARB, 0);
			// glFinish();
		}

		public Vector4[] getClearColors() {
			return new Vector4[] { c1, c2 };
		}

		public iProvider<Integer> getOutput(final int num) {
			return new iProvider<Integer>() {
				public Integer get() {
					return tex[num];
				}
			};
		}

		public NodeImpl<iSceneListElement> getRootSceneList() {
			return rootSceneList;
		}

		public BasicSceneList getSceneList() {
			return sceneList;
		}

		public void setClearColors(Vector4 c1, Vector4 c2) {
			this.c1 = c1;
			this.c2 = c2;
		}

		protected void createInitialLists() {
			// defaults for color

			// camera = new BasicCamera();

			rootSceneList = new BasicSceneList();
			rootSceneList.addChild(new BasicUtilities.Standard());
			if (sceneList == null)
				sceneList = new BasicSceneList();
		}

		public int[] bindToTexture = { 0, 1 };

		@Override
		protected void post() {
			assert glGetError() == 0;
			glActiveTexture(GL_TEXTURE0 + bindToTexture[0]);
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
			CoreHelpers.glDisable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			glActiveTexture(GL_TEXTURE0 + bindToTexture[1]);
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
			CoreHelpers.glDisable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			glActiveTexture(GL_TEXTURE0);
		}

		@Override
		protected void pre() {

			glActiveTexture(GL_TEXTURE + bindToTexture[0]);
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[0]);
			CoreHelpers.glEnable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			glActiveTexture(GL_TEXTURE + bindToTexture[1]);
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[1]);
			CoreHelpers.glEnable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			glActiveTexture(GL_TEXTURE0);
		}

		boolean lumenOnly = false;

		@Override
		protected void setup() {

			fbo[0] = glGenFramebuffers();
			rb[0] = glGenRenderbuffers();
			tex[0] = glGenTextures();
			tex[1] = glGenTextures();

			glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);

			gl_texture_min_filter = GL_LINEAR;
			gl_texture_mag_filter = GL_LINEAR;

			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[0]);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 0);
			glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0, useFloat ? (use32 ? GL_RGBA32F : GL_RGBA16F) : GL_RGBA8, width, height, 0, GL_RGBA, useFloat ? (use32 ? GL_FLOAT : GL_HALF_FLOAT) : GL_UNSIGNED_BYTE, (ByteBuffer) null);
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[0], 0);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, gl_texture_wrap_s);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, gl_texture_wrap_t);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[1]);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 0);

			if (lumenOnly) {
				glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0, useFloat ? GL_RG16F : GL_RG8, width, height, 0, GL_RG, useFloat ? (use32 ? GL_FLOAT : GL_HALF_FLOAT) : GL_UNSIGNED_BYTE, (ByteBuffer) null);
				glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[1], 0);
			} else {
				glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0, useFloat ? (use32 ? GL_RGBA32F : GL_RGBA16F) : GL_RGBA8, width, height, 0, GL_RGBA, useFloat ? (use32 ? GL_FLOAT : GL_HALF_FLOAT) : GL_UNSIGNED_BYTE, (ByteBuffer) null);
				glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[1], 0);
			}
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, gl_texture_wrap_s);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, gl_texture_wrap_t);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);

			glBindRenderbuffer(GL_RENDERBUFFER, rb[0]);
			glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
			glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rb[0]);
			status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
			assert status == GL_FRAMEBUFFER_COMPLETE : status;
			BasicContextManager.putId(this, fbo[0]);

			glClearColor(0, 0, 0, 0);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			glBindFramebuffer(GL_FRAMEBUFFER, 0);

		}

		public iSceneListElement placeOnscreen(final Rect r, int output) {
			return getOnscreenList(output, r, new Vector4(0, 0, 0, 0), new Vector4(1, 1, 1, 1), false);
		}

		public iSceneListElement getOnscreenList(final Rect r, Vector4 offset, Vector4 mul, final boolean genMip, int output) {
			return getOnscreenList(output, r, offset, mul, genMip);
		}

		public iSceneListElement placeOnscreen(BasicGLSLangProgram p, final Rect r, int output) {
			return getOnscreenList(p, output, r, new Vector4(0, 0, 0, 0), new Vector4(1, 1, 1, 1), false);
		}

		public iSceneListElement getOnscreenList(BasicGLSLangProgram p, final Rect r, Vector4 offset, Vector4 mul, final boolean genMip, int output) {
			return getOnscreenList(p, output, r, offset, mul, genMip);
		}

		@HiddenInAutocomplete
		public iSceneListElement getOnscreenList(BasicGLSLangProgram onscreenProgram, int output, final Rect r, Vector4 offset, Vector4 mul, final boolean genMip) {
			final TriangleMesh mesh = new BasicGeometry.TriangleMesh(StandardPass.render);
			mesh.rebuildTriangle(2);
			mesh.rebuildVertex(4);

			mesh.vertex().put((float) (r.x + r.w)).put((float) r.y).put(0.5f).put((float) (r.x + r.w)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y)).put(0.5f);
			mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 0).put((short) 2).put((short) 3);
			mesh.aux(Base.texture0_id, 2).put(useRect ? width : 1).put(0).put(useRect ? width : 1).put(useRect ? height : 1).put(0).put(useRect ? height : 1).put(0).put(0);
			mesh.aux(Base.color0_id, 4).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1);

			onscreenProgram.new SetIntegerUniform("depthTexture", 0);
			onscreenProgram.new SetUniform("offset", offset);
			onscreenProgram.new SetUniform("mul", mul);
			onscreenProgram.addChild(mesh);
			onscreenProgram.addChild(new TextureWrapper(genMip, useRect, this.getOutput(output), 0));
			onscreenProgram.addChild(new BasicUtilities.DisableDepthTest(true));

			return onscreenProgram;
		}

		@HiddenInAutocomplete
		public iSceneListElement getOnscreenList(int output, final Rect r, Vector4 offset, Vector4 mul, final boolean genMip) {
			final TriangleMesh mesh = new BasicGeometry.TriangleMesh(StandardPass.render);
			mesh.rebuildTriangle(2);
			mesh.rebuildVertex(4);

			mesh.vertex().put((float) (r.x + r.w)).put((float) r.y).put(0.5f).put((float) (r.x + r.w)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y)).put(0.5f);
			mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 0).put((short) 2).put((short) 3);
			mesh.aux(Base.texture0_id, 2).put(useRect ? width : 1).put(0).put(useRect ? width : 1).put(useRect ? height : 1).put(0).put(useRect ? height : 1).put(0).put(0);
			mesh.aux(Base.color0_id, 4).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1);

			// onscreen program
			// BasicGLSLangProgram onscreenProgram = (!useRect ? new
			// BasicGLSLangProgram("content/shaders/NDCvertex.glslang",
			// "content/shaders/PutImageProcessingOnscreenFragmentSquare.glslang")
			// : new
			// BasicGLSLangProgram("content/shaders/NDCvertex.glslang",
			// "content/shaders/PutImageProcessingOnscreenFragmentRect.glslang"));
			BasicGLSLangProgram onscreenProgram = (!useRect ? new BasicGLSLangProgram("content/shaders/NDCvertex.glslang", "content/shaders/PutImageProcessingOnscreenFragmentSquare.glslang") : new BasicGLSLangProgram("content/shaders/NDCvertex.glslang", "content/shaders/PutImageProcessingOnscreenFragmentRect.glslang"));
			onscreenProgram.new SetIntegerUniform("depthTexture", 0);
			onscreenProgram.new SetUniform("offset", offset);
			onscreenProgram.new SetUniform("mul", mul);
			onscreenProgram.addChild(mesh);
			onscreenProgram.addChild(new TextureWrapper(genMip, useRect, this.getOutput(output), 0));
			onscreenProgram.addChild(new BasicUtilities.DisableDepthTest(true));

			return onscreenProgram;
		}
	}

	static public class TripleFrameBuffer extends BasicTextures.BaseTexture implements iDisplayable {
		private final int width;

		private int status;

		private BasicSceneList rootSceneList;

		private BasicSceneList sceneList;

		private Vector4 c1, c2, c3;

		private final int height;

		int[] fbo = { -1, -1, -1 };

		int[] rb = { -1, -1, -1 };

		int[] tex = { -1, -1, -1 };

		boolean useRect = false;

		boolean useFloat = true;

		public TripleFrameBuffer(int width) {
			this.width = width;
			this.height = width;
			createInitialLists();
		}

		public TripleFrameBuffer(int depthWidth, int depthHeight) {
			this.width = depthWidth;
			this.height = depthHeight;
			createInitialLists();
		}

		public TripleFrameBuffer(int depthWidth, int depthHeight, boolean useFloat) {
			this.width = depthWidth;
			this.height = depthHeight;
			this.useFloat = useFloat;
			createInitialLists();
		}

		public void addFadePlane(iFloatProvider amount1, Vector4 color1, iFloatProvider amount2, Vector4 color2) {
			final TriangleMesh mesh = new BasicGeometry.TriangleMesh(Base.StandardPass.render);
			mesh.rebuildTriangle(2);
			mesh.rebuildVertex(4);

			mesh.vertex().put(-1f).put(-1f).put(0f).put(-1f).put(1f).put(0f).put(1f).put(1f).put(0f).put(1f).put(-1f).put(0f);
			mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 0).put((short) 2).put((short) 3);
			mesh.addChild(new BasicGLSLangProgram("content/shaders/NDC2ColorVertex.glslang", "content/shaders/VertexColor2Fragment.glslang"));
			mesh.addChild(new BasicUtilities.DepthMask(Base.StandardPass.transform, Base.StandardPass.postRender));

			float colorAlpha = 0.1f;
			float alphaAlpha = 0.95f;
			mesh.aux(Base.color0_id, 4).put(new float[] { 0, 0, 0, colorAlpha, 0, 0, 0, colorAlpha, 0, 0, 0, colorAlpha, 0, 0, 0, colorAlpha });
			mesh.aux(Base.color0_id + 1, 4).put(new float[] { 0.5f, 0.5f, 0.5f, alphaAlpha, 0.5f, 0.5f, 0.5f, alphaAlpha, 0.5f, 0.5f, 0.5f, alphaAlpha, 0.5f, 0.5f, 0.5f, alphaAlpha });

			final FloatBuffer root = ByteBuffer.allocate(mesh.vertex().limit() * 4).asFloatBuffer().put(mesh.vertex());

			// driver bug. Horrible seam
			// down middle of trianglulation
			mesh.addChild(new BasicUtilities.OnePassElement(StandardPass.preRender) {
				@Override
				public void performPass() {
					glColorMask(false, false, false, true);
					glClearColor(0, 0, 0, 0);
					glClear(GL_COLOR_BUFFER_BIT);
					glColorMask(true, true, true, true);

					// FloatBuffer
					// m =
					// mesh.vertex();
					// for
					// (int
					// i =
					// 0; i
					// <
					// m.capacity();
					// i++)
					// {
					// m.put(i,
					// (float)
					// (root.get(i)
					// * (1
					// +
					// Math.random()
					// *
					// 0.01f)));
					// }

				}
			});
			// mesh.addChild(new
			// BasicUtilities.SetBlendMode(StandardPass.preRender,
			// GL_CONSTANT_ALPHA,
			// GL_ONE_MINUS_CONSTANT_ALPHA,
			// new Vector4(0, 0, 0,
			// 0.05f)));
			// mesh.addChild(new
			// BasicUtilities.SetBlendMode(StandardPass.preDisplay,
			// GL_SRC_ALPHA,
			// GL_ONE_MINUS_SRC_ALPHA,
			// new Vector4(0, 0, 0,
			// 0.05f)));

			rootSceneList.addChild(mesh);
		}

		boolean first = true;

		public void display() {
			currentFBOContext.push(this);
			BasicGeometry.insideDoubleFloatFrameBuffer = false;
			if (FullScreenCanvasSWT.dropFrame > 0)
				return;

			try {
				gl = BasicContextManager.getGl();
				glu = BasicContextManager.getGlu();
				if (BasicContextManager.getId(this) == BasicContextManager.ID_NOT_FOUND) {
					// assert
					// false;
					// System.exit(0);
					setup();
				}

				glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);

				glDrawBuffer(GL_COLOR_ATTACHMENT0);

				if (first) {
					glClearColor(0, 0, 0, 1);
					glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
				}

				if (c1 != null) {
					glClearColor(c1.x, c1.y, c1.z, c1.w);
					glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
				} else {
				}

				glDrawBuffer(GL_COLOR_ATTACHMENT1);
				if (first) {
					glClearColor(0, 0, 0, 1);
					glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
				}
				if (c2 != null) {
					glClearColor(c2.x, c2.y, c2.z, c2.w);
					glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
				} else {
				}

				glDrawBuffer(GL_COLOR_ATTACHMENT2);
				if (first) {
					glClearColor(0, 0, 0, 1);
					glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
				}
				if (c3 != null) {
					glClearColor(c3.x, c3.y, c3.z, c3.w);
					glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
				} else {
				}

				// TODO lwjgl momemntum
				// glDrawBuffers(3, new int[] {
				// GL_COLOR_ATTACHMENT0,
				// GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2 },
				// 0);
				glClear(GL_DEPTH_BUFFER_BIT);

				glViewport(0, 0, width, height);
				rootSceneList.update();
				glViewport(0, 0, width, height);
				sceneList.update();

				// glFlush();
				glBindFramebuffer(GL_FRAMEBUFFER, 0);

				glDrawBuffer(GL_BACK);

				assert glGetError() == 0;
			} finally {
				Object popped = currentFBOContext.pop();
				BasicGeometry.insideDoubleFloatFrameBuffer = false;

				first = false;

				assert popped == this : popped;
			}
		}

		public Vector4[] getClearColors() {
			return new Vector4[] { c1, c2 };
		}

		public iProvider<Integer> getOutput(final int num) {
			return new iProvider<Integer>() {
				public Integer get() {
					return tex[num];
				}
			};
		}

		public NodeImpl<iSceneListElement> getRootSceneList() {
			return rootSceneList;
		}

		public BasicSceneList getSceneList() {
			return sceneList;
		}

		public void setClearColors(Vector4 c1, Vector4 c2, Vector4 c3) {
			this.c1 = c1;
			this.c2 = c2;
			this.c3 = c3;
		}

		protected void createInitialLists() {
			// defaults for color

			// camera = new BasicCamera();

			rootSceneList = new BasicSceneList();
			rootSceneList.addChild(new BasicUtilities.Standard());
			if (sceneList == null)
				sceneList = new BasicSceneList();
		}

		@Override
		public void post() {
			assert glGetError() == 0;
			glActiveTexture(GL_TEXTURE2);
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
			CoreHelpers.glDisable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);

			glActiveTexture(GL_TEXTURE1);
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
			CoreHelpers.glDisable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);

			glActiveTexture(GL_TEXTURE0);
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
			CoreHelpers.glDisable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
		}

		@Override
		public void pre() {

			glActiveTexture(GL_TEXTURE0);
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[0]);
			CoreHelpers.glEnable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[1]);
			CoreHelpers.glEnable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			glActiveTexture(GL_TEXTURE2);
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[2]);
			CoreHelpers.glEnable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			glActiveTexture(GL_TEXTURE0);
		}

		@Override
		public void setup() {
			if (BasicContextManager.getId(this) != -1)
				return;

			fbo[0] = glGenFramebuffers();
			rb[0] = glGenRenderbuffers();
			tex[0] = glGenTextures();
			tex[1] = glGenTextures();
			tex[2] = glGenTextures();

			glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);

			gl_texture_min_filter = GL_LINEAR;
			gl_texture_mag_filter = GL_LINEAR;

			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[0]);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 0);
			glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0, useFloat ? (use32 ? GL_RGBA32F : GL_RGBA16F) : GL_RGBA8, width, height, 0, GL_RGBA, useFloat ? (use32 ? GL_FLOAT : GL_HALF_FLOAT) : GL_UNSIGNED_BYTE, (ByteBuffer) null);
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[0], 0);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, gl_texture_wrap_s);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, gl_texture_wrap_t);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);

			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[1]);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 0);
			glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0, useFloat ? (use32 ? GL_RGBA32F : GL_RGBA16F) : GL_RGBA8, width, height, 0, GL_RGBA, useFloat ? (use32 ? GL_FLOAT : GL_HALF_FLOAT) : GL_UNSIGNED_BYTE, (ByteBuffer) null);
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[1], 0);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, gl_texture_wrap_s);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, gl_texture_wrap_t);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);

			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[2]);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 0);
			glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0, useFloat ? (use32 ? GL_RGBA32F : GL_RGBA16F) : GL_RGBA8, width, height, 0, GL_RGBA, useFloat ? (use32 ? GL_FLOAT : GL_HALF_FLOAT) : GL_UNSIGNED_BYTE, (ByteBuffer) null);
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT2, useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[2], 0);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, gl_texture_wrap_s);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, gl_texture_wrap_t);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);

			glBindRenderbuffer(GL_RENDERBUFFER, rb[0]);
			glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
			glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rb[0]);
			status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
			assert status == GL_FRAMEBUFFER_COMPLETE : status;

			;// System.out.println(" status is <" + status + ">");

			glDrawBuffer(GL_COLOR_ATTACHMENT0);

			glClearColor(0, 0, 0, 1);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			glDrawBuffer(GL_COLOR_ATTACHMENT1);
			glClearColor(0, 0, 0, 1);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			glDrawBuffer(GL_COLOR_ATTACHMENT2);
			glClearColor(0, 0, 0, 1);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			BasicContextManager.putId(this, fbo[0]);
			glBindFramebuffer(GL_FRAMEBUFFER, 0);

		}

		public iReposition placeOnscreen(final BasicSceneList into, final int layer, int output, final Rect r, float width, float height, Vector4 offset, Vector4 mul, final boolean genMip) {
			final TriangleMesh mesh = new BasicGeometry.TriangleMesh(StandardPass.render);
			mesh.rebuildTriangle(2);
			mesh.rebuildVertex(4);

			mesh.vertex().put((float) (r.x + r.w)).put((float) r.y).put(0.5f).put((float) (r.x + r.w)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y)).put(0.5f);
			mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 0).put((short) 2).put((short) 3);
			mesh.aux(Base.texture0_id, 2).put(width).put(0).put(width).put(height).put(0).put(height).put(0).put(0);
			mesh.aux(Base.color0_id, 4).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1);

			final boolean useRect = !(width == 1 && height == 1);
			// onscreen program
			final BasicGLSLangProgram onscreenProgram = (width == 1 && height == 1 ? new BasicGLSLangProgram("content/shaders/NDCvertex.glslang", "content/shaders/PutImageProcessingOnscreenFragmentSquare.glslang") : new BasicGLSLangProgram("content/shaders/NDCvertex.glslang", "content/shaders/PutImageProcessingOnscreenFragmentRect.glslang"));
			onscreenProgram.new SetIntegerUniform("depthTexture", 0);
			onscreenProgram.new SetUniform("offset", offset);
			onscreenProgram.new SetUniform("mul", mul);
			onscreenProgram.addChild(mesh);
			onscreenProgram.addChild(new TextureWrapper(genMip, useRect, new iProvider<Integer>() {

				public Integer get() {
					return tex[layer];
				}
			}, 0));
			onscreenProgram.addChild(new BasicUtilities.DisableDepthTest(true));
			into.addChild(onscreenProgram);

			return new iReposition() {

				Rect current = new Rect(0, 0, 0, 0).setValue(r);

				public Rect getRect() {
					return new Rect(0, 0, 0, 0).setValue(current);
				}

				public void setRect(Rect r) {
					current.setValue(r);
					mesh.vertex().put((float) (r.x + r.w)).put((float) r.y).put(0.5f).put((float) (r.x + r.w)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y)).put(0.5f);
				}

				public void setRectRotated(Rect r) {
					current.setValue(r);
					mesh.vertex().put((float) (r.x + r.w)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y)).put(0.5f).put((float) (r.x + r.w)).put((float) r.y).put(0.5f);

				}

				@Override
				public void remove() {
					into.removeChild(onscreenProgram);
				}

				public void add() {
					if (!into.isChild(onscreenProgram))
						into.addChild(onscreenProgram);
				}

			};
		}
	}

	/**
	 * matches a camera, but with a subsection of the screen
	 */
	static public class FBOCamera {
		protected final BasicCamera rootCamera;

		protected final Rect ndcSubsetRect;

		public FBOCamera(BasicCamera rootCamera, Rect ndcSubsetRect) {
			this.rootCamera = rootCamera;
			this.ndcSubsetRect = ndcSubsetRect;
		}

		public void enter() {

			assert glGetError() == 0;
			CoreHelpers.glMatrixMode(GL_PROJECTION);
			CoreHelpers.glLoadIdentity();

			// moved aspect
			float oright = (float) (rootCamera.near * Math.tan((Math.PI * rootCamera.fov / 180f) / 2) * rootCamera.aspect) * rootCamera.frustrumMul;
			float otop = (float) (rootCamera.near * Math.tan((Math.PI * rootCamera.fov / 180f) / 2)) * rootCamera.frustrumMul;

			// the above correspond to the
			// camea calculation.
			// if ndcSubsetRect is (-1,-1,
			// 2, 2)

			float left = -oright + oright * rootCamera.rshift;
			float bottom = -otop + otop * rootCamera.tshift;
			float right = oright + oright * rootCamera.rshift;
			float top = otop + otop * rootCamera.tshift;

			float nleft = (float) (left + (right - left) * (ndcSubsetRect.x + 1) / 2);
			float nright = (float) (left + (right - left) * (ndcSubsetRect.x + 1 + ndcSubsetRect.w) / 2);
			float ntop = (float) (bottom + (top - bottom) * (ndcSubsetRect.y + 1 + ndcSubsetRect.h) / 2);
			float nbottom = (float) (bottom + (top - bottom) * (ndcSubsetRect.y + 1) / 2);

			float io_frustra = rootCamera instanceof StereoCamera ? ((StereoCamera) rootCamera).io_frustra : 0;
			float x = io_frustra * FullScreenCanvasSWT.getSide().x;

			// inverted top and botom ?

			CoreHelpers.glFrustum(nleft + right * x, nright + right * x, nbottom, ntop, rootCamera.near, rootCamera.far);

			CoreHelpers.glMatrixMode(GL_MODELVIEW);
			CoreHelpers.glLoadIdentity();

			Vector3 leftOffset = new Vector3().cross(rootCamera.getViewRay(null), rootCamera.getUp(null)).normalize();
			leftOffset.x = leftOffset.x * (rootCamera instanceof StereoCamera ? ((StereoCamera) rootCamera).io_position.x * FullScreenCanvasSWT.getSide().x : 0);
			leftOffset.y = leftOffset.y * (rootCamera instanceof StereoCamera ? ((StereoCamera) rootCamera).io_position.y * FullScreenCanvasSWT.getSide().x : 0);
			leftOffset.z = leftOffset.z * (rootCamera instanceof StereoCamera ? ((StereoCamera) rootCamera).io_position.z * FullScreenCanvasSWT.getSide().x : 0);

			CoreHelpers.gluLookAt(rootCamera.position.x + leftOffset.x, rootCamera.position.y + leftOffset.y, rootCamera.position.z + leftOffset.z, rootCamera.lookAt.x, rootCamera.lookAt.y, rootCamera.lookAt.z, rootCamera.up.x, rootCamera.up.y, rootCamera.up.z);
			assert glGetError() == 0;
		}

		public void exit() {
			assert glGetError() == 0;
			CoreHelpers.glMatrixMode(GL_PROJECTION);
			CoreHelpers.glMatrixMode(GL_MODELVIEW);

			assert glGetError() == 0;
		}

		public BasicCamera getRootCamera() {
			return rootCamera;
		}

		public void setRect(Rect r) {
			ndcSubsetRect.setValue(r);
		}
	}

	public interface iMatchRule {
		public boolean match(Object o);
	}

	static public class Inside implements iMatchRule {
		private final Object o;

		public Inside(Object o) {
			this.o = o;
		}

		public boolean match(Object o) {
			return this.o == o;
		}

	}

	@Woven
	static public class MultiPasser extends BasicTextures.BaseTexture implements iDisplayable {

		static public final Method method_display = ReflectionTools.methodOf("display", MultiPasser.class);

		private float r;

		private float g;

		private float b;

		private float a;

		private boolean doClear;

		protected final int width;

		protected final int height;

		protected BasicCamera camera;

		protected BasicSceneList rootSceneList;

		protected BasicSceneList sceneList;

		protected int status;

		protected final boolean useRect;

		protected FBOCamera fboCamera;

		int[] fbo = { -1, -1 };

		int[] rb = { -1, -1 };

		int[] tex = { -1, -1 };

		boolean flip = false;

		boolean deallocated = false;

		boolean first = true;

		TaskQueue queue = new TaskQueue();

		public MultiPasser(int width, int height, boolean useRect) {
			this.width = width;
			this.height = height;
			this.useRect = useRect;
			createInitialLists();
			// camera.setViewport(0, 0,
			// width, height);
			// camera.setPerspective(camera.getFov(),
			// width / (float) height,
			// camera.getNear(),
			// camera.getFar());

			r = (float) SystemProperties.getDoubleProperty("background.red", 0);
			g = (float) SystemProperties.getDoubleProperty("background.green", 0);
			b = (float) SystemProperties.getDoubleProperty("background.blue", 0);
			a = (float) SystemProperties.getDoubleProperty("background.alpha", 1);

			doClear = SystemProperties.getIntProperty("background.clear", 1) == 1;
		}

		public void addOtherTexture(final BasicGLSLangProgram mesh) {
			Cont.linkWith(mesh, mesh.method_performPass, new aRun() {
				@Override
				public ReturnCode head(Object calledOn, Object[] args) {
					assert !deallocated;
					bindOtherTexture();
					return super.head(calledOn, args);
				}

				@Override
				public ReturnCode tail(Object calledOn, Object[] args, Object returnWas) {
					glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
					return super.tail(calledOn, args, returnWas);
				}
			});
		}

		public void addOtherTexture(final TriangleMesh mesh) {
			Cont.linkWith(mesh, mesh.method_performPass, new aRun() {
				@Override
				public ReturnCode head(Object calledOn, Object[] args) {
					bindOtherTexture();
					return super.head(calledOn, args);
				}

				@Override
				public ReturnCode tail(Object calledOn, Object[] args, Object returnWas) {
					assert !deallocated;
					glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
					return super.tail(calledOn, args, returnWas);
				}
			});
		}

		public void bindOtherTexture() {
			assert !deallocated;
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, flip ? tex[1] : tex[0]);
			// glBindTexture(useRect ?
			// GL_TEXTURE_RECTANGLE :
			// GL_TEXTURE_2D, tex[0]);
		}

		/**
		 * you still need to add a program (or two) to the mesh
		 */
		public Base.iGeometry constructDefaultDrawingPlane() {
			assert !deallocated;
			final TriangleMesh mesh = new BasicGeometry.TriangleMesh(Base.StandardPass.preRender);
			mesh.rebuildTriangle(2);
			mesh.rebuildVertex(4);

			mesh.vertex().put(-1).put(-1).put(0.5f).put(-1).put(1).put(0.5f).put(1).put(1).put(0.5f).put(1).put(-1).put(0.5f);
			mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 0).put((short) 2).put((short) 3);
			if (useRect)
				mesh.aux(Base.texture0_id, 2).put(0).put(0).put(0).put(height).put(width).put(height).put(width).put(0);
			else
				mesh.aux(Base.texture0_id, 2).put(0).put(0).put(0).put(1).put(1).put(1).put(1).put(0);

			addOtherTexture(mesh);

			// attached twice???

			// sceneList.addChild(mesh);

			return mesh;
		}

		public void delete() {
			deallocated = true;

			/*
			 * glGenFramebuffers(1, fbo, 0); assert glGetError() ==
			 * 0;
			 * 
			 * glGenRenderbuffers(1, rb, 0); assert glGetError() ==
			 * 0;
			 * 
			 * glGenTextures(2, tex, 0);
			 */

			glDeleteFramebuffers(fbo[0]);
			glDeleteRenderbuffers(rb[0]);
			glDeleteTextures(tex[0]);
			glDeleteTextures(tex[1]);
		}

		// this should be called at the end of
		// display(...) in full screen canvas

		@DispatchOverTopology(topology = Cont.class)
		public void display() {
			if (FullScreenCanvasSWT.dropFrame > 0)
				return;

			;// System.out.println(" inside multipasser display ");

			gl = BasicContextManager.getGl();
			glu = BasicContextManager.getGlu();

			if (BasicContextManager.getId(this) == BasicContextManager.ID_NOT_FOUND) {
				// assert
				// false;
				// System.exit(0);
				setup();
			}

			currentFBOContext.push(this);
			try {
				assert !deallocated;
				assert glGetError() == 0;
				glBindFramebuffer(GL_FRAMEBUFFER, flip ? fbo[0] : fbo[1]);
				assert glGetError() == 0;
				preDisplay();
				assert glGetError() == 0;
				glViewport(0, 0, width, height);
				assert glGetError() == 0;

				if (fboCamera != null)
					fboCamera.enter();

				assert glGetError() == 0;
				rootSceneList.update();
				assert glGetError() == 0;
				if (first) {
					first = false;
				} else {
					sceneList.update();
				}
				assert glGetError() == 0;

				queue.update();

				assert glGetError() == 0;
				glBindFramebuffer(GL_FRAMEBUFFER, 0);
				assert glGetError() == 0;

				postDisplay();
				assert glGetError() == 0;
				flip = !flip;

			} finally {
				Object popped = currentFBOContext.pop();
				assert popped == this : popped;
			}
		}

		public BasicCamera getCamera() {
			return fboCamera.getRootCamera();
		}

		public iProvider<Integer> getFBO() {
			return new iProvider<Integer>() {
				public Integer get() {
					return flip ? fbo[0] : fbo[1];
				}
			};
		}

		public iProvider<Integer> getTexture() {
			return new iProvider<Integer>() {
				public Integer get() {
					return flip ? tex[0] : tex[1];
				}
			};
		}

		public int getHeight() {
			return height;
		}

		// for debugging only, we should have a frame
		// buffer that supports async reads from a
		// renderbuffer as well
		// you can only call this at render time, see
		// "savePNG" below
		public ByteBuffer getImage(ByteBuffer storage) {
			if (storage == null) {
				storage = ByteBuffer.allocateDirect(width * height * 4);
			}

			int[] a = new int[1];
			a[0] = glGetInteger(GL_FRAMEBUFFER_BINDING);
			glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, flip ? tex[0] : tex[1], 0);
			glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, storage);
			glBindFramebuffer(GL_FRAMEBUFFER, a[0]);

			return storage;
		}

		public BasicSceneList getSceneList() {
			return sceneList;
		}

		public int getWidth() {
			return width;
		}

		public void saveImageToNullTexture(final NullTexture nt) {

			assert nt.width == this.width : "dimension mismatch " + nt.width + " " + this.width;
			assert nt.height == this.height : "dimension mismatch " + nt.height + " " + this.height;

			queue.new Task() {
				@Override
				protected void run() {
					nt.pre();
					assert glGetError() == 0;
					glCopyTexSubImage2D(GL_TEXTURE_RECTANGLE, 0, 0, 0, 0, 0, width, height);
					assert glGetError() == 0;
					nt.post();
				}
			};
		}

		public void savePNG(final FullScreenCanvasSWT canvas, final String filename, final ByteBuffer s) {
			Cont.linkWith(canvas, canvas.method_beforeFlush, new Cont.aRun() {
				@Override
				public ReturnCode tail(Object calledOn, Object[] args, Object returnWas) {

					ByteBuffer storage = getImage(s);

					BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

					WritableRaster tile = bi.getWritableTile(0, 0);
					DataBuffer buffer = tile.getDataBuffer();

					IntBuffer storagei = storage.asIntBuffer();
					for (int y = 0; y < height; y++) {
						for (int x = 0; x < width; x++) {
							int r = BaseMath.intify(storage.get());
							int g = BaseMath.intify(storage.get());
							int b = BaseMath.intify(storage.get());
							int a = BaseMath.intify(storage.get());

							int d = (g << 8) | (r << 16) | (b);// |
							// (a
							// <<
							// 24);
							buffer.setElem((height - 1 - y) * width + x, d);
						}
					}

					FileOutputStream fos;
					RenderedOp op = JAI.create("filestore", bi, filename, "PNG");

					Cont.unlinkWith(canvas, canvas.method_beforeFlush, this);

					return ReturnCode.cont;
				}
			});
		}

		public MultiPasser setBackground(float r, float g, float b, float a, boolean doClear) {
			this.r = r;
			this.g = g;
			this.b = b;
			this.a = a;
			this.doClear = doClear;
			createInitialLists();

			return this;
		}

		public MultiPasser setFBOCamera(FBOCamera fboCamera) {
			this.fboCamera = fboCamera;
			return this;
		}

		protected void createInitialLists() {
			// defaults for color

			// camera = new BasicCamera();

			rootSceneList = new BasicSceneList();
			// rootSceneList.addChild(new
			// BasicUtilities.Standard());
			if (doClear) {
				BasicUtilities.Clear clear = new BasicUtilities.Clear(new Vector3(r, g, b), a);
				rootSceneList.addChild(clear);
			} else {
				rootSceneList.addChild(new BasicUtilities.ClearOnce(new Vector3(0, 0, 0), 1));
			}
			// rootSceneList.addChild(camera);
			if (sceneList == null)
				sceneList = new BasicSceneList();
		}

		@Override
		protected void post() {
			assert !deallocated;
			assert glGetError() == 0;
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
			assert glGetError() == 0;
			CoreHelpers.glDisable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			assert glGetError() == 0;
		}

		protected void postDisplay() {
		}

		@Override
		protected void pre() {
			assert !deallocated;
			assert glGetError() == 0;

			if (flip) {
				glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[0]);
				// glGenerateMipmap(GL_TEXTURE_2D);

				CoreHelpers.glEnable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			} else {
				glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[1]);
				// glGenerateMipmap(GL_TEXTURE_2D);
				CoreHelpers.glEnable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			}

			assert glGetError() == 0;
		}

		protected void preDisplay() {
		}

		boolean useFloat = true;

		public MultiPasser setUseFloat(boolean useFloat) {
			this.useFloat = useFloat;
			return this;
		}

		@Override
		protected void setup() {

			assert !deallocated;
			assert glGetError() == 0;

			fbo[0] = glGenFramebuffers();
			fbo[1] = glGenFramebuffers();
			assert glGetError() == 0;

			rb[0] = glGenRenderbuffers();
			rb[1] = glGenRenderbuffers();
			assert glGetError() == 0;

			tex[0] = glGenTextures();
			tex[1] = glGenTextures();
			assert glGetError() == 0;

			{
				glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);

				gl_texture_min_filter = GL_LINEAR;
				gl_texture_mag_filter = GL_LINEAR;

				glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[0]);
				// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE,
				// 0);

				// glTexImage2D(useRect ?
				// GL_TEXTURE_RECTANGLE :
				// GL_TEXTURE_2D, 0, GL_RGBA8, width,
				// height, 0, GL_RGBA, GL_UNSIGNED_BYTE,
				// null);
				glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0, useFloat ? (use32 ? GL_RGBA32F : GL_RGBA16F) : GL_RGBA8, width, height, 0, GL_RGBA, useFloat ? (use32 ? GL_FLOAT : GL_HALF_FLOAT) : GL_UNSIGNED_BYTE, (ByteBuffer) null);

				glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[0], 0);
				// glTexParameteri(useRect ?
				// GL_TEXTURE_RECTANGLE :
				// GL_TEXTURE_2D, GL_TEXTURE_WRAP_S,
				// gl_texture_wrap_s);
				// glTexParameteri(useRect ?
				// GL_TEXTURE_RECTANGLE :
				// GL_TEXTURE_2D, GL_TEXTURE_WRAP_T,
				// gl_texture_wrap_t);
				glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
				glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);

				// glBindRenderbuffer(GL_RENDERBUFFER,
				// rb[0]);
				// glRenderbufferStorage(GL_RENDERBUFFER,
				// GL_DEPTH_COMPONENT24, width, height);
				// glFramebufferRenderbuffer(GL_FRAMEBUFFER,
				// GL_DEPTH_ATTACHMENT,
				// GL_RENDERBUFFER, rb[0]);

				status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
				assert status == GL_FRAMEBUFFER_COMPLETE : status;
				BasicContextManager.putId(this, fbo[0]);

				glClearColor(0, 0, 0, 0);
				glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

				glBindFramebuffer(GL_FRAMEBUFFER, 0);

			}

			{
				glBindFramebuffer(GL_FRAMEBUFFER, fbo[1]);

				gl_texture_min_filter = GL_LINEAR;
				gl_texture_mag_filter = GL_LINEAR;

				glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[1]);
				// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE,
				// 0);
				glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0, useFloat ? (use32 ? GL_RGBA32F : GL_RGBA16F) : GL_RGBA8, width, height, 0, GL_RGBA, useFloat ? (use32 ? GL_FLOAT : GL_HALF_FLOAT) : GL_UNSIGNED_BYTE, (ByteBuffer) null);

				// glTexImage2D(useRect ?
				// GL_TEXTURE_RECTANGLE :
				// GL_TEXTURE_2D, 0, GL_RGBA8, width,
				// height, 0, GL_RGBA, GL_UNSIGNED_BYTE,
				// null);
				glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[1], 0);
				// glTexParameteri(useRect ?
				// GL_TEXTURE_RECTANGLE :
				// GL_TEXTURE_2D, GL_TEXTURE_WRAP_S,
				// gl_texture_wrap_s);
				// glTexParameteri(useRect ?
				// GL_TEXTURE_RECTANGLE :
				// GL_TEXTURE_2D, GL_TEXTURE_WRAP_T,
				// gl_texture_wrap_t);
				glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
				glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);

				// glBindRenderbuffer(GL_RENDERBUFFER,
				// rb[1]);
				// glRenderbufferStorage(GL_RENDERBUFFER,
				// GL_DEPTH_COMPONENT24, width, height);
				// glFramebufferRenderbuffer(GL_FRAMEBUFFER,
				// GL_DEPTH_ATTACHMENT,
				// GL_RENDERBUFFER, rb[1]);

				status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
				assert status == GL_FRAMEBUFFER_COMPLETE : status;
				BasicContextManager.putId(this, fbo[1]);

				glClearColor(0, 0, 0, 0);
				glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

				glBindFramebuffer(GL_FRAMEBUFFER, 0);

			}
		}

		public iReposition placeOnscreen(final BasicSceneList into, final Rect r, float width, float height, Vector4 offset, Vector4 mul, final boolean genMip) {
			final TriangleMesh mesh = new BasicGeometry.TriangleMesh(StandardPass.render);
			mesh.rebuildTriangle(2);
			mesh.rebuildVertex(4);

			mesh.vertex().put((float) (r.x + r.w)).put((float) r.y).put(0.5f).put((float) (r.x + r.w)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y)).put(0.5f);
			mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 0).put((short) 2).put((short) 3);
			mesh.aux(Base.texture0_id, 2).put(width).put(0).put(width).put(height).put(0).put(height).put(0).put(0);
			mesh.aux(Base.color0_id, 4).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1);

			final boolean useRect = !(width == 1 && height == 1);
			// onscreen program
			final BasicGLSLangProgram onscreenProgram = (width == 1 && height == 1 ? new BasicGLSLangProgram("content/shaders/NDCvertex.glslang", "content/shaders/PutImageProcessingOnscreenFragmentSquare.glslang") : new BasicGLSLangProgram("content/shaders/NDCvertex.glslang", "content/shaders/PutImageProcessingOnscreenFragmentRect.glslang"));
			// BasicGLSLangProgram onscreenProgram = (width == 1 &&
			// height == 1 ? new
			// BasicGLSLangProgram("content/shaders/NDCvertex.glslang",
			// "content/shaders/Whiet.glslang") : new
			// BasicGLSLangProgram("content/shaders/NDCvertex.glslang",
			// "content/shaders/PutImageProcessingOnscreenFragmentRect.glslang"));
			onscreenProgram.new SetIntegerUniform("depthTexture", 0);
			onscreenProgram.new SetUniform("offset", offset);
			onscreenProgram.new SetUniform("mul", mul);
			onscreenProgram.addChild(mesh);
			onscreenProgram.addChild(new TextureWrapper(genMip, useRect, new iProvider<Integer>() {

				public Integer get() {
					int ff = flip ? tex[1] : tex[0];
					;// System.out.println(" texturing <" +
						// ff + ">");
					return ff;
				}
			}, 0));
			onscreenProgram.addChild(new BasicUtilities.DisableDepthTest(true));
			into.addChild(onscreenProgram);

			return new iReposition() {

				Rect current = new Rect(0, 0, 0, 0).setValue(r);

				public Rect getRect() {
					return new Rect(0, 0, 0, 0).setValue(current);
				}

				public void setRect(Rect r) {
					current.setValue(r);
					mesh.vertex().put((float) (r.x + r.w)).put((float) r.y).put(0.5f).put((float) (r.x + r.w)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y)).put(0.5f);
				}

				public void setRectRotated(Rect r) {
					current.setValue(r);
					mesh.vertex().put((float) (r.x + r.w)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y)).put(0.5f).put((float) (r.x + r.w)).put((float) r.y).put(0.5f);

				}

				@Override
				public void remove() {
					into.removeChild(onscreenProgram);
				}

				public void add() {
					if (!into.isChild(onscreenProgram))
						into.addChild(onscreenProgram);
				}

			};
		}

	}

	// untested (but likely to work, needs a driver)
	static public class NTextureCrossfader extends BasicUtilities.TwoPassElement {
		private final MultiPasser source;

		private final OnePassElement bug;

		NullTexture[] textureA;

		TextureUnit[] textureAWrapped;

		boolean[] copyToA;

		boolean hasData = false;

		boolean hasSetup = false;

		public NTextureCrossfader(MultiPasser source, int[] unitA) {
			super("", Base.StandardPass.preRender, Base.StandardPass.postRender);
			this.source = source;

			textureA = new NullTexture[unitA.length];
			for (int i = 0; i < textureA.length; i++)
				textureA[i] = new NullTexture(source.width, source.height);
			textureAWrapped = new TextureUnit[unitA.length];
			for (int i = 0; i < textureA.length; i++)
				textureAWrapped[i] = new TextureUnit(unitA[i], textureA[i]);
			copyToA = new boolean[unitA.length];
			for (int i = 0; i < copyToA.length; i++)
				copyToA[i] = true;

			bug = new BasicUtilities.OnePassElement(StandardPass.preDisplay) {
				@Override
				public void performPass() {
					if (!hasSetup)
						return;

					for (int i = 0; i < copyToA.length; i++)
						if (copyToA[i]) {
							assert glGetError() == 0;
							textureAWrapped[i].pre();
							textureAWrapped[i].in(gl);
							assert glGetError() == 0;
							glCopyTexSubImage2D(GL_TEXTURE_RECTANGLE, 0, 0, 0, 0, 0, NTextureCrossfader.this.source.width, NTextureCrossfader.this.source.height);
							assert glGetError() == 0;
							textureAWrapped[i].out(gl);
							textureAWrapped[i].post();
							assert glGetError() == 0;
							copyToA[i] = false;
						}
					hasData = true;
				}
			};
			source.getSceneList().addChild(bug);
		}

		public void delete() {
			for (int i = 0; i < textureA.length; i++)
				textureA[i].delete();

			source.removeChild(bug);
		}

		public void doCopyTo(int i) {
			copyToA[i] = true;
		}

		public int getLength() {
			return copyToA.length;
		}

		@Override
		protected void post() {
			if (hasData) {
				for (int i = 0; i < textureAWrapped.length; i++) {
					textureAWrapped[i].gl = BasicContextManager.getGl();
					textureAWrapped[i].glu = BasicContextManager.getGlu();
					textureAWrapped[i].post();
				}
			}
		}

		@Override
		protected void pre() {
			if (hasData) {

				for (int i = 0; i < textureAWrapped.length; i++) {
					textureAWrapped[i].gl = BasicContextManager.getGl();
					textureAWrapped[i].glu = BasicContextManager.getGlu();
					textureAWrapped[i].pre();
				}
			}
		}

		@Override
		protected void setup() {
			hasSetup = true;

			for (int i = 0; i < textureAWrapped.length; i++) {
				textureAWrapped[i].gl = BasicContextManager.getGl();
				textureAWrapped[i].glu = BasicContextManager.getGlu();
				textureAWrapped[i].setup();
			}

			BasicContextManager.putId(this, 0);
			BasicContextManager.markAsValidInThisContext(this);
		}
	}

	static public class NullTexture extends BaseTexture implements iHasTexture {
		private final int width;

		private final int height;

		private ByteBuffer fakeStorage;

		int textureId = 0;

		boolean deallocated = false;

		boolean dirty = false;

		public NullTexture(int width, int height) {
			this.width = width;
			this.height = height;

			// fakeStorage =
			// ByteBuffer.allocateDirect(width*height*4);
		}

		public void delete() {
			glDeleteTextures(textureId);
			deallocated = true;
		}

		public void dirty() {
			dirty = true;
		}

		@Override
		public void post() {
			assert !deallocated;
			CoreHelpers.glDisable(GL_TEXTURE_2D);
		}

		@Override
		public void pre() {
			assert !deallocated;
			int textureId = BasicContextManager.getId(this);
			if (textureId == BasicContextManager.ID_NOT_FOUND) {
				setup();
				textureId = BasicContextManager.getId(this);
				assert textureId != BasicContextManager.ID_NOT_FOUND : "called setup() in texture, didn't get an ID has subclass forgotten to call BasicContextIDManager.pudId(...) ?";
			}
			assert (glGetError() == 0) : this.getClass().getName();
			glBindTexture(GL_TEXTURE_2D, textureId);
			assert (glGetError() == 0) : this.getClass().getName() + " " + BasicContextManager.getCurrentContext();
			CoreHelpers.glEnable(GL_TEXTURE_2D);
			assert (glGetError() == 0) : this.getClass().getName();
		}

		@Override
		protected void setup() {
			assert !deallocated;
			int[] textures = new int[1];
			textures[0] = glGenTextures();
			textureId = textures[0];
			BasicContextManager.putId(this, textureId);

			int[] a = new int[1];
			a[0] = glGetInteger(GL_ACTIVE_TEXTURE);

			glBindTexture(GL_TEXTURE_2D, textureId);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 0);

			// new
			// Exception().printStackTrace();
			// glTexParameteri(GL_TEXTURE_2D,
			// GL_TEXTURE_STORAGE_HINT_APPLE,
			// GL_STORAGE_CACHED_APPLE);

			if (!doGenMip) {
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			} else {
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

			}
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			assert (glGetError() == 0);
			// glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8,
			// width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE,
			// fakeStorage);
			glTexImage2D(GL_TEXTURE_2D, 0, (use32 ? GL_RGBA32F : GL_RGBA16F), width, height, 0, GL_RGBA, GL11.GL_FLOAT, fakeStorage);

			if (doGenMip) {
				glGenerateMipmap(GL_TEXTURE_2D);
			}
			assert (glGetError() == 0);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 1);
		}

		@Override
		public iProvider<Integer> getOutput() {
			return new iProvider<Integer>() {

				@Override
				public Integer get() {
					return textureId;
				}
			};
		}
	}

	static public class NullTextureInt extends BaseTexture implements iHasTexture {
		private final int width;

		private final int height;

		private ByteBuffer fakeStorage;

		int textureId = 0;

		boolean deallocated = false;

		boolean dirty = false;

		public NullTextureInt(int width, int height) {
			this.width = width;
			this.height = height;
		}

		public void delete() {
			glDeleteTextures(textureId);
			deallocated = true;
		}

		public void dirty() {
			dirty = true;
		}

		@Override
		public void post() {
			assert !deallocated;
			CoreHelpers.glDisable(GL_TEXTURE_2D);
		}

		@Override
		public void pre() {
			assert !deallocated;
			int textureId = BasicContextManager.getId(this);
			if (textureId == BasicContextManager.ID_NOT_FOUND) {
				setup();
				textureId = BasicContextManager.getId(this);
				assert textureId != BasicContextManager.ID_NOT_FOUND : "called setup() in texture, didn't get an ID has subclass forgotten to call BasicContextIDManager.pudId(...) ?";
			}
			assert (glGetError() == 0) : this.getClass().getName();
			glBindTexture(GL_TEXTURE_2D, textureId);
			assert (glGetError() == 0) : this.getClass().getName() + " " + BasicContextManager.getCurrentContext();
			CoreHelpers.glEnable(GL_TEXTURE_2D);
			assert (glGetError() == 0) : this.getClass().getName();
		}

		@Override
		protected void setup() {
			assert !deallocated;
			int[] textures = new int[1];
			textures[0] = glGenTextures();
			textureId = textures[0];
			BasicContextManager.putId(this, textureId);

			int[] a = new int[1];
			a[0] = glGetInteger(GL_ACTIVE_TEXTURE);

			glBindTexture(GL_TEXTURE_2D, textureId);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 0);

			// new
			// Exception().printStackTrace();
			// glTexParameteri(GL_TEXTURE_2D,
			// GL_TEXTURE_STORAGE_HINT_APPLE,
			// GL_STORAGE_CACHED_APPLE);

			if (!doGenMip) {
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			} else {
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

			}
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			assert (glGetError() == 0);
			// glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8,
			// width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE,
			// fakeStorage);
			glTexImage2D(GL_TEXTURE_2D, 0, GL30.GL_R32UI, width, height, 0, GL30.GL_R32UI, GL11.GL_UNSIGNED_INT, fakeStorage);

			if (doGenMip) {
				glGenerateMipmap(GL_TEXTURE_2D);
			}
			assert (glGetError() == 0);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 1);
		}

		@Override
		public iProvider<Integer> getOutput() {
			return new iProvider<Integer>() {

				@Override
				public Integer get() {
					return textureId;
				}
			};
		}
	}

	static public class SingleFrameBuffer extends BasicTextures.BaseTexture implements iDisplayable, iHasFBO, iHasTexture {
		private final int width;

		private final int height;

		private int status;

		private BasicSceneList rootSceneList;

		private BasicSceneList sceneList;

		int[] fbo = { -1 };

		int[] rb = { -1 };

		int[] tex = { -1 };
		int[] colorTex = { -1 };

		boolean useRect = false;

		boolean useFloat = true;

		boolean genMip = false;

		private Vector4 c1 = new Vector4(0, 0, 0, 1);

		public SingleFrameBuffer(int width) {
			this.width = width;
			this.height = width;
			this.textureTarget = useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D;
			createInitialLists();
		}

		public SingleFrameBuffer(int width, int height, boolean useRect, boolean useFloat, boolean genMip) {
			this.width = width;
			this.height = height;
			this.useFloat = useFloat;
			this.useRect = useRect;
			this.genMip = genMip;
			this.textureTarget = useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D;
			createInitialLists();
		}

		public void setClearColor(Vector4 c1) {
			this.c1 = c1;
		}

		boolean multisample = false;

		boolean[] clearMask = { true, true, true, true };

		public void display() {
			if (deleted)
				return;

			if (FullScreenCanvasSWT.dropFrame > 0)
				return;

			// ;//System.out.println(" -- single frame buffer <"+this+"> display -- ");

			currentFBOContext.push(this);
			try {

				gl = BasicContextManager.getGl();
				glu = BasicContextManager.getGlu();
				if (BasicContextManager.getId(this) == BasicContextManager.ID_NOT_FOUND) {
					// assert
					// false;
					// System.exit(0);
					setup();
				}

				glBindFramebuffer(GL_FRAMEBUFFER, multisample ? fbo_multisample : fbo[0]);

				rootSceneList.update();
				if (c1 != null) {

					glColorMask(clearMask[0], clearMask[1], clearMask[2], clearMask[3]);

					glClearColor(c1.x, c1.y, c1.z, c1.w);
					glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

					glColorMask(true, true, true, true);

				} else {
					glClear(GL_DEPTH_BUFFER_BIT);
				}
				glViewport(0, 0, width, height);
				sceneList.update();

				assert glGetError() == 0;

				glBindFramebuffer(GL_FRAMEBUFFER, 0);

				if (multisample) {
					glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo_multisample);
					glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo[0]);
					glDrawBuffers(GL_COLOR_ATTACHMENT0);
					glReadBuffer(GL_COLOR_ATTACHMENT0);
					glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
				}

				glBindFramebuffer(GL_FRAMEBUFFER, 0);

			} finally {
				Object popped = currentFBOContext.pop();
				assert popped == this : popped;
			}
		}

		public iAcceptor<Number> addFadePlane() {
			final TriangleMesh mesh = new BasicGeometry.TriangleMesh(Base.StandardPass.transform);
			mesh.rebuildTriangle(2);
			mesh.rebuildVertex(4);

			mesh.vertex().put(-1f).put(-1f).put(0f).put(-1f).put(1f).put(0f).put(1f).put(1f).put(0f).put(1f).put(-1f).put(0f);
			mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 0).put((short) 2).put((short) 3);
			mesh.addChild(new BasicGLSLangProgram("content/shaders/NDC2ColorVertex.glslang", "content/shaders/VertexColor2Fragment.glslang", Base.StandardPass.preTransform));
			mesh.addChild(new BasicUtilities.DepthMask(Base.StandardPass.preTransform, Base.StandardPass.postRender));

			float colorAlpha = 0.1f;
			float alphaAlpha = 0.5f;
			mesh.aux(Base.color0_id, 4).put(new float[] { 0, 0, 0, colorAlpha, 0, 0, 0, colorAlpha, 0, 0, 0, colorAlpha, 0, 0, 0, colorAlpha });
			mesh.aux(Base.color0_id + 1, 4).put(new float[] { 0.5f, 0.5f, 0.5f, alphaAlpha, 0.5f, 0.5f, 0.5f, alphaAlpha, 0.5f, 0.5f, 0.5f, alphaAlpha, 0.5f, 0.5f, 0.5f, alphaAlpha });

			final FloatBuffer root = ByteBuffer.allocate(mesh.vertex().limit() * 4).asFloatBuffer().put(mesh.vertex());

			rootSceneList.addChild(mesh);

			return new iAcceptor<Number>() {

				float last = -1;

				TriangleMesh geometry = mesh;

				@Override
				public iAcceptor<Number> set(Number to) {
					if (to.floatValue() != last)
						mesh.aux(Base.color0_id, 4).put(new float[] { 0, 0, 0, to.floatValue(), 0, 0, 0, to.floatValue(), 0, 0, 0, to.floatValue(), 0, 0, 0, to.floatValue() });
					last = to.floatValue();
					return this;
				}
			};
		}

		public void copyToVBO(final TriangleMesh mesh, final int aux) {
			sceneList.add(StandardPass.preDisplay).register("__copyToVbo__" + System.identityHashCode(mesh) + " " + aux, new iUpdateable() {
				@Override
				public void update() {
					;// System.out.println(" copying to aux buffer ");
					copyToVBONow(mesh, aux);
				}
			});
		}

		protected void copyToVBONow(TriangleMesh mesh, int aux) {
			int target = mesh.getOpenGLBufferName(aux);

			if (target == -1)
				return;

			glFinish();
			glBindBuffer(GL_PIXEL_PACK_BUFFER_ARB, target);
			glReadPixels(0, 0, width, height, GL_RGBA, GL_FLOAT, 0);
			glBindBuffer(GL_PIXEL_PACK_BUFFER_ARB, 0);
			glFinish();
		}

		public void copyToNullTexture(final NullTexture nt) {

			sceneList.add(StandardPass.preDisplay).register("__copyToNullTexture__" + System.identityHashCode(nt), new iUpdateable() {
				@Override
				public void update() {
					nt.pre();
					glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
					nt.post();
				}
			});
		}

		public void copyToNullTextureOnce(final NullTexture nt) {

			final String name = "__copyToNullTexture__" + System.identityHashCode(nt);
			sceneList.add(StandardPass.preDisplay).register(name, new iUpdateable() {
				@Override
				public void update() {
					nt.pre();
					glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
					nt.post();
					sceneList.add(StandardPass.preDisplay).remove(name);
				}
			});
		}

		// advanced use
		public void enter() {
			currentFBOContext.push(this);
			gl = BasicContextManager.getGl();
			glu = BasicContextManager.getGlu();
			if (BasicContextManager.getId(this) == BasicContextManager.ID_NOT_FOUND) {
				setup();
			}
			glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
			glViewport(0, 0, width, height);
		}

		public void exit() {
			glBindFramebuffer(GL_FRAMEBUFFER, 0);

			assert glGetError() == 0;

			Object popped = currentFBOContext.pop();
			assert popped == this : popped;
		}

		public int getFBO() {
			if (fbo[0] == -1)
				setup();

			return fbo[0];
		}

		public int getRB() {
			if (rb[0] == -1)
				setup();

			return rb[0];
		}

		public iProvider<Integer> getOutput() {
			return new iProvider<Integer>() {
				public Integer get() {
					return tex[0];
				}
			};
		}

		public NodeImpl<iSceneListElement> getRootSceneList() {
			return rootSceneList;
		}

		public BasicSceneList getSceneList() {
			return sceneList;
		}

		public void join(FullScreenCanvasSWT canvas) {
			aRun arun = new Cont.aRun() {
				@Override
				public ReturnCode head(Object calledOn, Object[] args) {
					display();
					return super.head(calledOn, args);
				}
			};
			Cont.linkWith(canvas, canvas.method_beforeFlush, arun);
		}

		public void replaceSceneList(BasicSceneList sceneList) {
			this.sceneList = sceneList;
		}

		protected void createInitialLists() {
			// defaults for color

			// camera = new BasicCamera();

			rootSceneList = new BasicSceneList();
			rootSceneList.addChild(new BasicUtilities.Standard());
			// BasicUtilities.Clear clear = new
			// BasicUtilities.Clear(new Vector3(0, 0, 0), 1);
			// rootSceneList.addChild(clear);
			if (sceneList == null)
				sceneList = new BasicSceneList();
		}

		@Override
		public void post() {
			if (deleted)
				return;

			if (tex[0] == -1) {
				;// System.out.println(" attempt to bind texture before it has been updated ");
				return;
			}

			assert glGetError() == 0;
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
			assert glGetError() == 0;
			CoreHelpers.glDisable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			assert glGetError() == 0;
			// ;//System.out.println(" <<< unbinding texture " +
			// this);
		}

		@Override
		public void pre() {
			if (deleted)
				return;

			if (tex[0] == -1) {
				;// System.out.println(" attempt to bind texture before it has been updated ");
				return;
			}

			assert glGetError() == 0;
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[0]);
			assert glGetError() == 0;
			CoreHelpers.glEnable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			assert glGetError() == 0;
			if (genMip) {
				glGenerateMipmap(GL_TEXTURE_2D);
			}
		}

		public boolean doDepth = false;

		int fbo_multisample = -1;

		@Override
		protected void setup() {

			this.gl = BasicContextManager.getGl();
			this.glu = BasicContextManager.getGlu();

			fbo[0] = glGenFramebuffers();
			rb[0] = glGenRenderbuffers();
			tex[0] = glGenTextures();

			if (multisample) {
				fbo_multisample = glGenFramebuffers();
				int rb_multisample = glGenRenderbuffers();
				int rb_multisample_depth = glGenRenderbuffers();
				int converageSamples = 8;
				int depthSamples = 8;

				glBindFramebuffer(GL_FRAMEBUFFER, fbo_multisample);

				glBindRenderbuffer(GL_RENDERBUFFER, rb_multisample);
				glRenderbufferStorageMultisample(GL_RENDERBUFFER, converageSamples, useFloat ? (use32 ? GL_RGBA32F : GL_RGBA16F) : GL_RGBA, width, height);
				glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, rb_multisample);

				glBindRenderbuffer(GL_RENDERBUFFER, rb_multisample_depth);
				glRenderbufferStorageMultisample(GL_RENDERBUFFER, depthSamples, GL_DEPTH24_STENCIL8, width, height);
				glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rb_multisample_depth);
				int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);

				if (status != GL_FRAMEBUFFER_COMPLETE)
					throw new IllegalArgumentException();
			}

			glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);

			gl_texture_min_filter = GL_LINEAR;
			gl_texture_mag_filter = GL_LINEAR;

			if (genMip) {
				gl_texture_min_filter = GL_LINEAR_MIPMAP_LINEAR;
				gl_texture_mag_filter = GL_LINEAR;
			}

			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[0]);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 0);
			glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0, useFloat ? (use32 ? GL_RGBA32F : GL_RGBA16F) : GL_RGBA, width, height, 0, GL_RGBA, useFloat ? (use32 ? GL_FLOAT : GL_HALF_FLOAT) : GL_UNSIGNED_BYTE, (ByteBuffer) null);
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[0], 0);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, gl_texture_wrap_s);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, gl_texture_wrap_t);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);

			if (genMip) {
				// glTexParameterf(useRect ?
				// GL_TEXTURE_RECTANGLE :
				// GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT,
				// 16.0f);
			}

			if (doDepth) {
				glBindRenderbuffer(GL_RENDERBUFFER, rb[0]);
				glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
				glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rb[0]);
			}

			status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
			assert status == GL_FRAMEBUFFER_COMPLETE : status;
			BasicContextManager.putId(this, fbo[0]);

			glClearColor(0, 0, 0, 0);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			glBindFramebuffer(GL_FRAMEBUFFER, 0);

		}

		public iSceneListElement placeOnscreen(final Rect r) {
			return getOnscreenList(0, r, new Vector4(0, 0, 0, 0), new Vector4(1, 1, 1, 1), false);
		}

		public iSceneListElement getOnscreenList(final Rect r, Vector4 offset, Vector4 mul, final boolean genMip) {
			return getOnscreenList(0, r, offset, mul, genMip);
		}

		@HiddenInAutocomplete
		public iSceneListElement getOnscreenList(int output, final Rect r, Vector4 offset, Vector4 mul, final boolean genMip) {
			final TriangleMesh mesh = new BasicGeometry.TriangleMesh(StandardPass.render);
			mesh.rebuildTriangle(2);
			mesh.rebuildVertex(4);

			mesh.vertex().put((float) (r.x + r.w)).put((float) r.y).put(0.5f).put((float) (r.x + r.w)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y)).put(0.5f);
			mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 0).put((short) 2).put((short) 3);
			mesh.aux(Base.texture0_id, 2).put(useRect ? width : 1).put(0).put(useRect ? width : 1).put(useRect ? height : 1).put(0).put(useRect ? height : 1).put(0).put(0);
			mesh.aux(Base.color0_id, 4).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1);

			// onscreen program
			// BasicGLSLangProgram onscreenProgram = (!useRect ? new
			// BasicGLSLangProgram("content/shaders/NDCvertex.glslang",
			// "content/shaders/PutImageProcessingOnscreenFragmentSquare.glslang")
			// : new
			// BasicGLSLangProgram("content/shaders/NDCvertex.glslang",
			// "content/shaders/PutImageProcessingOnscreenFragmentRect.glslang"));
			BasicGLSLangProgram onscreenProgram = (!useRect ? new BasicGLSLangProgram("content/shaders/NDCvertex.glslang", "content/shaders/PutImageProcessingOnscreenFragmentSquare.glslang") : new BasicGLSLangProgram("content/shaders/NDCvertex.glslang", "content/shaders/PutImageProcessingOnscreenFragmentRect.glslang"));
			onscreenProgram.new SetIntegerUniform("depthTexture", 0);
			onscreenProgram.new SetUniform("offset", offset);
			onscreenProgram.new SetUniform("mul", mul);
			onscreenProgram.addChild(mesh);
			onscreenProgram.addChild(new TextureWrapper(genMip, useRect, this.getOutput(), 0));
			onscreenProgram.addChild(new BasicUtilities.DisableDepthTest(true));

			return onscreenProgram;
		}

		public iSceneListElement placeOnscreen(BasicGLSLangProgram onscreenProgram, final Rect r) {
			return getOnscreenList(onscreenProgram, 0, r, new Vector4(0, 0, 0, 0), new Vector4(1, 1, 1, 1), false);
		}

		public iSceneListElement getOnscreenList(BasicGLSLangProgram onscreenProgram, final Rect r, Vector4 offset, Vector4 mul, final boolean genMip) {
			return getOnscreenList(onscreenProgram, 0, r, offset, mul, genMip);
		}

		@HiddenInAutocomplete
		public iSceneListElement getOnscreenList(BasicGLSLangProgram onscreenProgram, int output, final Rect r, Vector4 offset, Vector4 mul, final boolean genMip) {
			final TriangleMesh mesh = new BasicGeometry.TriangleMesh(StandardPass.render);
			mesh.rebuildTriangle(2);
			mesh.rebuildVertex(4);

			mesh.vertex().put((float) (r.x + r.w)).put((float) r.y).put(0.5f).put((float) (r.x + r.w)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y)).put(0.5f);
			mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 0).put((short) 2).put((short) 3);
			mesh.aux(Base.texture0_id, 2).put(useRect ? width : 1).put(0).put(useRect ? width : 1).put(useRect ? height : 1).put(0).put(useRect ? height : 1).put(0).put(0);
			mesh.aux(Base.color0_id, 4).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1);

			onscreenProgram.new SetIntegerUniform("depthTexture", 0);
			onscreenProgram.new SetUniform("offset", offset);
			onscreenProgram.new SetUniform("mul", mul);
			onscreenProgram.addChild(mesh);
			onscreenProgram.addChild(new TextureWrapper(genMip, useRect, this.getOutput(), 0));
			onscreenProgram.addChild(new BasicUtilities.DisableDepthTest(true));

			return onscreenProgram;
		}

		boolean deleted = false;

		public void delete(TaskQueue deletionQueue) {
			deletionQueue.new Task() {

				@Override
				protected void run() {
					if (deleted)
						return;

					deleted = true;

					glDeleteFramebuffers(fbo[0]);
					glDeleteRenderbuffers(fbo[1]);
					glDeleteTextures(tex[0]);

				}
			};
		}

	}

	static public class Switcher extends BasicSceneList implements iSceneListElement {

		static public final Method method_performPass = ReflectionTools.methodOf("performPass", OnePassListElement.class);

		private final StandardPass ourPass;

		protected Base.StandardPass requestPass;

		protected Set renderPass = new HashSet();

		protected iPass ourRenderPass;

		protected boolean preCalled = false;

		protected boolean postCalled = false;

		LinkedHashMapOfLists<iMatchRule, OnePassListElement> known = new LinkedHashMapOfLists<iMatchRule, OnePassListElement>();

		Set<OnePassListElement> running = new HashSet<OnePassListElement>();

		boolean skipIfEmpty = false;

		public Switcher() {
			this.ourPass = StandardPass.preRender;
			this.ourRenderPass = this.requestPass(StandardPass.preRender);
			this.requestPass = StandardPass.preRender;
		}

		public Switcher(Base.StandardPass parentPass, Base.StandardPass ourPass) {
			this.ourPass = ourPass;
			this.ourRenderPass = this.requestPass(ourPass);
			this.requestPass = parentPass;
		}

		public Switcher add(iMatchRule rule, OnePassListElement o) {
			known.addToList(rule, o);
			return this;
		}

		@Override
		public void addChild(iSceneListElement newChild) {

			HashSet<OnePassListElement> ex = new HashSet<OnePassListElement>();

			for (Collection<OnePassListElement> q1 : known.values())
				for (OnePassListElement q2 : q1) {
					ex.add(q2);
				}
			for (OnePassListElement e : ex)
				e.addChild(newChild);
		}

		@Override
		public void notifyAddParent(iMutable<iSceneListElement> newParent) {
			super.notifyAddParent(newParent);
			renderPass.add(((iSceneListElement) newParent).requestPass(requestPass));
		}

		public void performPass() {
			pre();
			for (OnePassListElement e : running) {

				e.performPass();
			}
			post();
		}

		@Override
		public void performPass(iPass p) {

			if ((p == null) || (renderPass.contains(p))) {

				running.clear();

				Object key = currentFBOContext.size() == 0 ? null : currentFBOContext.peek();

				for (Entry<iMatchRule, Collection<OnePassListElement>> e : known.entrySet()) {
					if (e.getKey().match(key))
						running.addAll(e.getValue());
				}

				if (skipIfEmpty && this.getChildren().size() == 0)
					return;

				preCalled = false;
				postCalled = false;
				assert (glGetError() == 0) : this.getClass();
				performPass();
				assert (glGetError() == 0) : this.getClass();
				assert preCalled;
				assert postCalled;
			}
		}

		public void setSkipIfEmpty() {
			skipIfEmpty = true;
		}

		protected void post() {
			postCalled = true;
		}

		/**
		 * subclasses must call these, typically on entry and exit to
		 * performPass ()
		 */
		protected void pre() {
			preCalled = true;

		}

	}

	static public class TextureCrossfader extends BasicUtilities.TwoPassElement {
		private final MultiPasser source;

		NullTexture textureA;

		TextureUnit textureAWrapped;

		NullTexture textureB;

		TextureUnit textureBWrapped;

		boolean copyToA = true;

		boolean copyToB = true;

		boolean hasData = false;

		boolean hasSetup = false;

		public TextureCrossfader(MultiPasser source, int unitA, int unitB) {
			super("", Base.StandardPass.preRender, Base.StandardPass.postRender);
			this.source = source;

			textureA = new NullTexture(source.width, source.height);
			textureAWrapped = new TextureUnit(unitA, textureA);
			textureB = new NullTexture(source.width, source.height);
			textureBWrapped = new TextureUnit(unitB, textureB);

			source.getSceneList().addChild(new BasicUtilities.OnePassElement(StandardPass.preDisplay) {
				@Override
				public void performPass() {
					if (!hasSetup)
						return;

					if (copyToA) {
						assert glGetError() == 0;
						long a = System.currentTimeMillis();
						textureAWrapped.pre();
						textureAWrapped.in(gl);
						assert glGetError() == 0;
						long b = System.currentTimeMillis();
						glCopyTexSubImage2D(GL_TEXTURE_RECTANGLE, 0, 0, 0, 0, 0, TextureCrossfader.this.source.width, TextureCrossfader.this.source.height);
						long c = System.currentTimeMillis();
						assert glGetError() == 0;
						textureAWrapped.out(gl);
						textureAWrapped.post();
						assert glGetError() == 0;
						long d = System.currentTimeMillis();

						;// System.out.println(" timing information <"
							// + (d - a) + " " + (c
							// - b) + ">");
					}
					if (copyToB) {
						assert glGetError() == 0;
						textureBWrapped.pre();
						textureBWrapped.in(gl);
						assert glGetError() == 0;
						glCopyTexSubImage2D(GL_TEXTURE_RECTANGLE, 0, 0, 0, 0, 0, TextureCrossfader.this.source.width, TextureCrossfader.this.source.height);
						assert glGetError() == 0;
						assert glGetError() == 0;
						textureBWrapped.out(gl);
						textureBWrapped.post();
						assert glGetError() == 0;
					}
					copyToA = false;
					copyToB = false;
					hasData = true;
				}
			});
		}

		public void doCopyToA() {
			copyToA = true;
		}

		public void doCopyToB() {
			copyToB = true;
		}

		@Override
		protected void post() {
			if (hasData) {
				textureAWrapped.gl = BasicContextManager.getGl();
				textureAWrapped.glu = BasicContextManager.getGlu();

				textureBWrapped.gl = BasicContextManager.getGl();
				textureBWrapped.glu = BasicContextManager.getGlu();
				textureAWrapped.post();
				textureBWrapped.post();
			}
		}

		@Override
		protected void pre() {
			if (hasData) {
				textureAWrapped.gl = BasicContextManager.getGl();
				textureAWrapped.glu = BasicContextManager.getGlu();

				textureBWrapped.gl = BasicContextManager.getGl();
				textureBWrapped.glu = BasicContextManager.getGlu();

				textureAWrapped.pre();
				textureBWrapped.pre();
			}
		}

		@Override
		protected void setup() {
			hasSetup = true;

			textureAWrapped.gl = BasicContextManager.getGl();
			textureAWrapped.glu = BasicContextManager.getGlu();

			textureBWrapped.gl = BasicContextManager.getGl();
			textureBWrapped.glu = BasicContextManager.getGlu();

			textureAWrapped.setup();
			textureBWrapped.setup();
			BasicContextManager.putId(this, 0);
			BasicContextManager.markAsValidInThisContext(this);
		}
	}

	static public class TextureCrossfader2 extends BasicUtilities.TwoPassElement {
		private final iAcceptsSceneListElement source;

		NullTexture textureA;

		TextureUnit textureAWrapped;

		NullTexture textureB;

		TextureUnit textureBWrapped;

		boolean copyToA = true;

		boolean copyToB = true;

		boolean hasDataA = false;
		boolean hasDataB = false;

		boolean hasSetup = false;

		private int width;

		private int height;

		public TextureCrossfader2(iAcceptsSceneListElement source, int unitA, int unitB, int width, int height) {
			super("", Base.StandardPass.preRender, Base.StandardPass.postRender);
			this.source = source;

			this.width = width;
			this.height = height;

			textureA = new NullTexture(width, height);
			textureAWrapped = new TextureUnit(unitA, textureA);
			textureB = new NullTexture(width, height);
			textureBWrapped = new TextureUnit(unitB, textureB);

			source.addChild(new BasicUtilities.OnePassElement(StandardPass.preDisplay) {
				@Override
				public void performPass() {
					if (!hasSetup)
						return;

					if (copyToA || !hasDataA) {
						;// System.out.println(" copy to A ");
						assert glGetError() == 0;
						textureAWrapped.pre();
						textureAWrapped.in(gl);
						assert glGetError() == 0;
						glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, TextureCrossfader2.this.width, TextureCrossfader2.this.height);
						assert glGetError() == 0;
						textureAWrapped.out(gl);
						textureAWrapped.post();
						assert glGetError() == 0;

						;// System.out.println(" copy to a ");
						hasDataA = true;

					}
					if (copyToB || !hasDataB) {
						;// System.out.println(" copy to B ");
						assert glGetError() == 0;
						textureBWrapped.pre();
						textureBWrapped.in(gl);
						assert glGetError() == 0;
						glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, TextureCrossfader2.this.width, TextureCrossfader2.this.height);
						assert glGetError() == 0;
						assert glGetError() == 0;
						textureBWrapped.out(gl);
						textureBWrapped.post();
						assert glGetError() == 0;

						;// System.out.println(" copy to b ");
						hasDataB = true;

					}
					copyToA = false;
					copyToB = false;
				}
			});
		}

		public void doCopyToA() {
			copyToA = true;
		}

		public void doCopyToB() {
			copyToB = true;
		}

		@Override
		protected void post() {
			if (hasDataA && hasDataB) {
				textureAWrapped.gl = BasicContextManager.getGl();
				textureAWrapped.glu = BasicContextManager.getGlu();

				textureBWrapped.gl = BasicContextManager.getGl();
				textureBWrapped.glu = BasicContextManager.getGlu();
				textureAWrapped.post();
				textureBWrapped.post();
			}
		}

		@Override
		protected void pre() {
			if (hasDataA && hasDataB) {
				textureAWrapped.gl = BasicContextManager.getGl();
				textureAWrapped.glu = BasicContextManager.getGlu();

				textureBWrapped.gl = BasicContextManager.getGl();
				textureBWrapped.glu = BasicContextManager.getGlu();

				textureAWrapped.pre();
				textureBWrapped.pre();
			}
		}

		@Override
		protected void setup() {
			hasSetup = true;

			textureAWrapped.gl = BasicContextManager.getGl();
			textureAWrapped.glu = BasicContextManager.getGlu();

			textureBWrapped.gl = BasicContextManager.getGl();
			textureBWrapped.glu = BasicContextManager.getGlu();

			textureAWrapped.setup();
			textureBWrapped.setup();
			BasicContextManager.putId(this, 0);
			BasicContextManager.markAsValidInThisContext(this);
		}
	}

	static public final Stack<Object> currentFBOContext = new Stack<Object>();

	static public class Wrap extends BasicUtilities.OnePassElement {

		private final iGeometry geometry;

		private Method doPerformPass;

		private Method doSetup;

		public Wrap(iGeometry geometry) {
			super(StandardPass.render);
			this.geometry = geometry;
			doPerformPass = ReflectionTools.findFirstMethodCalled(geometry.getClass(), "doPerformPass");
			doSetup = ReflectionTools.findFirstMethodCalled(geometry.getClass(), "doSetup");
		}

		CoordinateFrame frame = new CoordinateFrame();
		private float matrix[] = null;
		private Matrix4 tmpStorage = new Matrix4();

		FloatBuffer mm = ByteBuffer.allocateDirect(4 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

		@Override
		public void performPass() {

			try {
				if (BasicContextManager.getId(geometry) == BasicContextManager.ID_NOT_FOUND) {
					doSetup.invoke(geometry);
				}

				geometry.getCoordinateProvider().get(frame);

				CoreHelpers.glPushMatrix();

				matrix = frame.getMatrix(tmpStorage).getColumnMajor(matrix);

				mm.rewind();
				mm.put(matrix);
				mm.rewind();

				CoreHelpers.glMultMatrix(mm);

				CoreHelpers.glActiveTexture(GL_TEXTURE5);
				CoreHelpers.glMatrixMode(GL_TEXTURE);
				CoreHelpers.glLoadIdentity();
				CoreHelpers.glMultMatrix(mm);
				CoreHelpers.glActiveTexture(GL_TEXTURE0);
				CoreHelpers.glMatrixMode(GL_MODELVIEW);

				doPerformPass.invoke(geometry);

				glPopMatrix();

			} catch (IllegalAccessException e) {
				e.printStackTrace();
				// throw new IllegalArgumentException(e);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				// throw new IllegalArgumentException(e);
			}
		}

		public iGeometry getGeometry() {
			return geometry;
		}

	}

	static public class WrapInstance extends BasicUtilities.OnePassElement {

		private final Instance geometry;

		private Method doPerformPass;

		private Method doSetup;

		public WrapInstance(Instance geometry) {
			super(StandardPass.render);
			this.geometry = geometry;
			doPerformPass = ReflectionTools.findFirstMethodCalled(geometry.getClass(), "performPass");
		}

		CoordinateFrame frame = new CoordinateFrame();
		private float matrix[] = null;
		private Matrix4 tmpStorage = new Matrix4();

		FloatBuffer mm = ByteBuffer.allocateDirect(4 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

		@Override
		public void performPass() {

			try {

				CoordinateFrame frame = new CoordinateFrame().setValue(geometry.getFrame());

				matrix = frame.getMatrix(tmpStorage).getColumnMajor(matrix);

				mm.rewind();
				mm.put(matrix);
				mm.rewind();

				CoreHelpers.glActiveTexture(GL_TEXTURE5);
				CoreHelpers.glMatrixMode(GL_TEXTURE);
				CoreHelpers.glLoadIdentity();
				CoreHelpers.glMultMatrix(mm);
				CoreHelpers.glActiveTexture(GL_TEXTURE0);
				CoreHelpers.glMatrixMode(GL_MODELVIEW);

				doPerformPass.invoke(geometry);

			} catch (IllegalAccessException e) {
				e.printStackTrace();
				// throw new IllegalArgumentException(e);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				// throw new IllegalArgumentException(e);
			}
		}

		public Instance getGeometry() {
			return geometry;
		}

	}

}

/*
 * 
 * float left = -oright + oright rootCamera.rshift/(ndcSubsetRect.w / 2); float
 * bottom = -otop + otop rootCamera.tshift/(ndcSubsetRect.h / 2); float right =
 * oright + oright rootCamera.rshift/(ndcSubsetRect.w / 2); float top = otop +
 * otop rootCamera.tshift/(ndcSubsetRect.h / 2);
 * 
 * float owidth = (right - left) ndcSubsetRect.w / 2; float oheight = (top -
 * bottom) ndcSubsetRect.h / 2;
 * 
 * left = left (-ndcSubsetRect.x); bottom = bottom (-ndcSubsetRect.y);
 * 
 * right = left + owidth; top = bottom + oheight; glFrustum(left, right, bottom,
 * top, rootCamera.near, rootCamera.far);
 */
