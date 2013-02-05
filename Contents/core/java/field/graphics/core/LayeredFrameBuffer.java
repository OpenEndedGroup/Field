package field.graphics.core;

import static org.lwjgl.opengl.ARBPixelBufferObject.GL_PIXEL_PACK_BUFFER_ARB;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGBA;
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
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL12.glTexImage3D;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_HALF_FLOAT;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_2D_ARRAY;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL32.glFramebufferTexture;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL12;

import field.bytecode.protect.annotations.HiddenInAutocomplete;
import field.bytecode.protect.dispatch.Cont;
import field.bytecode.protect.dispatch.Cont.ReturnCode;
import field.bytecode.protect.dispatch.Cont.aRun;
import field.core.dispatch.iVisualElement.Rect;
import field.graphics.core.Base.StandardPass;
import field.graphics.core.Base.iSceneListElement;
import field.graphics.core.BasicFrameBuffers.NullTexture;
import field.graphics.core.BasicFrameBuffers.iDisplayable;
import field.graphics.core.BasicFrameBuffers.iHasFBO;
import field.graphics.core.BasicFrameBuffers.iHasTexture;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.graphics.imageprocessing.ImageProcessing.TextureWrapper;
import field.graphics.windowing.FullScreenCanvasSWT;
import field.launch.iUpdateable;
import field.math.abstraction.iAcceptor;
import field.math.abstraction.iProvider;
import field.math.graph.NodeImpl;
import field.math.linalg.Vector4;
import field.math.linalg.iToFloatArray;
import field.util.TaskQueue;

public class LayeredFrameBuffer extends BasicTextures.BaseTexture implements iDisplayable, iHasFBO, iHasTexture {
	private final int width;

	private final int height;

	private int status;

	private BasicSceneList rootSceneList;

	private BasicSceneList sceneList;

	int[] fbo = { -1 };

	int[] tex = { -1 };
	int[] colorTex = { -1 };

	boolean useFloat = true;

	private Vector4 c1 = new Vector4(0, 0, 0, 1);

	final int depth;

	public LayeredFrameBuffer(int width) {
		this.width = width;
		this.height = width;
		this.depth = 1;
		createInitialLists();
	}

	public LayeredFrameBuffer(int width, int height, int depth, boolean useFloat) {
		this.width = width;
		this.height = height;
		this.useFloat = useFloat;
		this.textureTarget = GL_TEXTURE_2D_ARRAY;
		this.depth = depth;
		createInitialLists();
	}

	public void setClearColor(Vector4 c1) {
		this.c1 = c1;
	}

	boolean[] clearMask = { true, true, true, true };

	boolean displayable = true;
	
	public void display() {
		if (deleted)
			return;

		if (!displayable) return;
		
		if (FullScreenCanvasSWT.dropFrame > 0)
			return;

//		System.out.println(" -- layeredframe buffer <" + this + "> display -- " + status + " " + GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT);

		BasicFrameBuffers.currentFBOContext.push(this);
		try {

			gl = BasicContextManager.getGl();
			glu = BasicContextManager.getGlu();
			if (BasicContextManager.getId(this) == BasicContextManager.ID_NOT_FOUND) {
				setup();
			}

			glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);

			glViewport(0, 0, width, height);
			rootSceneList.update();
			if (c1 != null) {

				glColorMask(clearMask[0], clearMask[1], clearMask[2], clearMask[3]);

				glClearColor(c1.x, c1.y, c1.z, c1.w);
				glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

				glColorMask(true, true, true, true);

			} else {
				glClear(GL_DEPTH_BUFFER_BIT);
			}
			sceneList.update();

			assert glGetError() == 0;

			glBindFramebuffer(GL_FRAMEBUFFER, 0);

		} finally {
			Object popped = BasicFrameBuffers.currentFBOContext.pop();
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
		BasicFrameBuffers.currentFBOContext.push(this);
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

		Object popped = BasicFrameBuffers.currentFBOContext.pop();
		assert popped == this : popped;
	}

	public int getFBO() {
		if (fbo[0] == -1)
			setup();

		return fbo[0];
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
		glBindTexture(textureTarget, 0);
		assert glGetError() == 0;
		CoreHelpers.glDisable(textureTarget);
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
		glBindTexture(textureTarget, tex[0]);
		assert glGetError() == 0;
		CoreHelpers.glEnable(textureTarget);
		assert glGetError() == 0;
	}

	public boolean doDepth = false;

	int fbo_multisample = -1;

	@Override
	protected void setup() {

		this.gl = BasicContextManager.getGl();
		this.glu = BasicContextManager.getGlu();

		fbo[0] = glGenFramebuffers();
		tex[0] = glGenTextures();

		glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);

		gl_texture_min_filter = GL_LINEAR;
		gl_texture_mag_filter = GL_LINEAR;

		glBindTexture(textureTarget, tex[0]);
		glTexImage3D(textureTarget, 0, useFloat ? (BasicFrameBuffers.use32 ? GL_RGBA32F : GL_RGBA16F) : GL_RGBA, width, height, depth, 0, GL12.GL_BGRA, useFloat ? (BasicFrameBuffers.use32 ? GL_FLOAT : GL_HALF_FLOAT) : GL_UNSIGNED_BYTE, (ByteBuffer) null);
		glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, tex[0], 0);
		glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, gl_texture_wrap_s);
		glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, gl_texture_wrap_t);
		glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
		glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);

		status = glCheckFramebufferStatus(GL_FRAMEBUFFER);

		System.out.println(" FBO STATUS is :" + status);

		assert status == GL_FRAMEBUFFER_COMPLETE : status;
		BasicContextManager.putId(this, fbo[0]);

		glClearColor(0, 0, 0, 0);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		glBindFramebuffer(GL_FRAMEBUFFER, 0);

	}

	public iSceneListElement placeOnscreen(int output, final Rect r) {
		return getOnscreenList(output, r, new Vector4(0, 0, 0, 0), new Vector4(1, 1, 1, 1), false);
	}

	@HiddenInAutocomplete
	public iSceneListElement getOnscreenList(final int output, final Rect r, Vector4 offset, Vector4 mul, final boolean genMip) {
		final TriangleMesh mesh = new BasicGeometry.TriangleMesh(StandardPass.render);
		mesh.rebuildTriangle(2);
		mesh.rebuildVertex(4);

		mesh.vertex().put((float) (r.x + r.w)).put((float) r.y).put(0.5f).put((float) (r.x + r.w)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y)).put(0.5f);
		mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 0).put((short) 2).put((short) 3);
		mesh.aux(Base.texture0_id, 2).put(1).put(0).put(1).put(1).put(0).put(1).put(0).put(0);
		mesh.aux(Base.color0_id, 4).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1);

		BasicGLSLangProgram onscreenProgram = (new BasicGLSLangProgram("content/shaders/NDCvertex.glslang", "content/shaders/PutImageProcessingOnscreenFragmentSquareArray.glslang"));
		onscreenProgram.new SetIntegerUniform("depthTexture", 0);
		onscreenProgram.new SetUniform("offset", offset);
		onscreenProgram.new SetUniform("mul", mul);
		onscreenProgram.new SetUniform("layer", new iToFloatArray() {
			@Override
			public float[] get() {
				return new float[] { (float)output };
			}
		});

		onscreenProgram.addChild(mesh);
		onscreenProgram.addChild(new TextureWrapper(genMip, false, this.getOutput(), 0).setTarget(GL_TEXTURE_2D_ARRAY));
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
		mesh.aux(Base.texture0_id, 2).put(1).put(0).put(1).put(1).put(0).put(1).put(0).put(0);
		mesh.aux(Base.color0_id, 4).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1);

		onscreenProgram.new SetIntegerUniform("depthTexture", 0);
		onscreenProgram.new SetUniform("offset", offset);
		onscreenProgram.new SetUniform("mul", mul);
		onscreenProgram.addChild(mesh);
		onscreenProgram.addChild(new TextureWrapper(genMip, false, this.getOutput(), 0).setTarget(GL_TEXTURE_2D_ARRAY));
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
				glDeleteTextures(tex[0]);
			}
		};
	}

}