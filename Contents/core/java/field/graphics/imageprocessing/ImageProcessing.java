package field.graphics.imageprocessing;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NICEST;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_PERSPECTIVE_CORRECTION_HINT;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.GL_ZERO;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColorMask;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.glHint;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenRenderbuffers;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import field.bytecode.protect.Woven;
import field.bytecode.protect.dispatch.Cont;
import field.bytecode.protect.dispatch.Cont.ReturnCode;
import field.bytecode.protect.dispatch.Cont.aRun;
import field.core.dispatch.iVisualElement.Rect;
import field.graphics.core.Base;
import field.graphics.core.Base.StandardPass;
import field.graphics.core.Base.iAcceptsSceneListElement;
import field.graphics.core.Base.iSceneListElement;
import field.graphics.core.BasicContextManager;
import field.graphics.core.BasicFrameBuffers.iHasFBO;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.BasicGeometry;
import field.graphics.core.BasicGeometry.QuadMesh;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.graphics.core.BasicSceneList;
import field.graphics.core.BasicTextures;
import field.graphics.core.BasicTextures.TextureUnit;
import field.graphics.core.BasicUtilities;
import field.graphics.core.BasicUtilities.TwoPassElement;
import field.graphics.core.CoreHelpers;
import field.graphics.windowing.FullScreenCanvasSWT;
import field.graphics.windowing.FullScreenCanvasSWT.StereoSide;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.abstraction.iFloatProvider;
import field.math.abstraction.iProvider;
import field.math.linalg.Vector4;
import field.util.TaskQueue;

/**
 * framebuffer in, framebuffer out
 * 
 * @author marc
 * 
 */
@Woven
public class ImageProcessing implements iImageProcessor, iAcceptsSceneListElement {

	public interface iProcessesMesh {
		public void process(Base.iGeometry process);
	}

	public interface iReposition {
		public Rect getRect();

		public void setRect(Rect r);

		public void setRectRotated(Rect r);

		public void remove();

		public void add();
	}

	public interface iRepositionAndSceneList extends iSceneListElement {
		public Rect getRect();

		public void setRect(Rect r);

		public void setRectRotated(Rect r);
	}

	static public enum KnownBlendMode {
		noBlending(GL_ONE, GL_ZERO, GL_ONE, GL_ZERO), noAlpha(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);

		public final int sc;

		public final int dc;

		public final int sa;

		public final int da;

		KnownBlendMode(int sc, int dc, int sa, int da) {
			this.sc = sc;
			this.dc = dc;
			this.sa = sa;
			this.da = da;
		}
	}

	
	public static class TextureWrapper extends BasicUtilities.TwoPassElement {
		private final boolean mip;

		private final boolean rect;

		private final iProvider<Integer> from;

		private final int unit;

		private int target;

		public TextureWrapper(boolean mip, boolean rect, iProvider<Integer> from, int unit) {
			super("", StandardPass.preRender, StandardPass.postRender);
			this.mip = mip;
			this.rect = rect;
			this.from = from;
			this.unit = unit;
			
			this.target = rect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D;
		}
		
		public TextureWrapper setTarget(int target) {
			this.target = target;
			return this;
		}

		@Override
		protected void post() {
			if (from.get() == -1)
				return;
			glActiveTexture(GL_TEXTURE0 + unit);
			glBindTexture(target, 0);
			CoreHelpers.glDisable(target);
			glActiveTexture(GL_TEXTURE0);
		}

		@Override
		protected void pre() {
			if (from.get() == -1)
				return;

			glActiveTexture(GL_TEXTURE0 + unit);

			glBindTexture(target, from.get());
			CoreHelpers.glEnable(target);

			if (mip) {
				glGenerateMipmap(GL_TEXTURE_2D);
			}
			glActiveTexture(GL_TEXTURE0);
		}

		@Override
		protected void setup() {
			BasicContextManager.putId(this, 1);
		}
	}

	public static class StereoTextureWrapper extends BasicUtilities.TwoPassElement {
		private final boolean mip;

		private final boolean rect;

		private final iProvider<Integer> left;
		private final iProvider<Integer> right;

		private final int unit;

		public StereoTextureWrapper(boolean mip, boolean rect, iProvider<Integer> left, iProvider<Integer> right, int unit) {
			super("", StandardPass.preRender, StandardPass.postRender);
			this.mip = mip;
			this.rect = rect;
			this.left= left;
			this.right= right;
			this.unit = unit;
		}

		@Override
		protected void post() {
			if (left.get() == -1)
				return;
			if (right.get() == -1)
				return;
			glActiveTexture(GL_TEXTURE0 + unit);
			glBindTexture(rect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
			CoreHelpers.glDisable(rect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			glActiveTexture(GL_TEXTURE0);
		}

		@Override
		protected void pre() {
			if (left.get() == -1)
				return;
			if (right.get() == -1)
				return;

			glActiveTexture(GL_TEXTURE0 + unit);

			glBindTexture(rect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, (FullScreenCanvasSWT.getSide()==StereoSide.left ? left : right).get());
			CoreHelpers.glEnable(rect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);

			if (mip) {
				glGenerateMipmap(GL_TEXTURE_2D);
			}
			glActiveTexture(GL_TEXTURE0);
		}

		@Override
		protected void setup() {
			BasicContextManager.putId(this, 1);
		}
	}

	
	static public TriangleMesh placeOnscreen(BasicSceneList into, iImageProcessor from, int output, Rect r, float width, float height, Vector4 offset, Vector4 mul) {
		final TriangleMesh mesh = new BasicGeometry.QuadMesh(StandardPass.render);
		mesh.rebuildTriangle(1);
		mesh.rebuildVertex(4);

		mesh.vertex().put((float) r.x).put((float) r.y).put(0.5f).put((float) r.x).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x + r.w)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x + r.w)).put((float) (r.y)).put(0.5f);
		mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 3);
		mesh.aux(Base.texture0_id, 2).put(width).put(0).put(width).put(height).put(0).put(height).put(0).put(0);
		mesh.aux(Base.color0_id, 4).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1);

		// onscreen program
		BasicGLSLangProgram onscreenProgram = (width == 1 && height == 1 ? new BasicGLSLangProgram("content/shaders/NDCvertex.glslang", "content/shaders/PutImageProcessingOnscreenFragmentSquare.glslang") : new BasicGLSLangProgram("content/shaders/NDCvertex.glslang", "content/shaders/PutImageProcessingOnscreenFragmentRect.glslang"));
		onscreenProgram.new SetUniform("offset", new Vector4(offset));
		onscreenProgram.new SetUniform("mul", new Vector4(mul));
		onscreenProgram.new SetIntegerUniform("depthTexture", 0);
		onscreenProgram.addChild(mesh);
		onscreenProgram.addChild(from.getOutputElement(output));
		onscreenProgram.addChild(new BasicUtilities.DisableDepthTest(true));

		into.addChild(onscreenProgram);

		return mesh;
	}

	
	
	static public iReposition placeOnscreen(BasicSceneList into, final iProvider<Integer> from, int output, Rect r, float width, float height, Vector4 offset, Vector4 mul) {
		return placeOnscreen(into, from, output, r, width, height, offset, mul, false);
	}

	static public iReposition placeOnscreen(final BasicSceneList into, final iProvider<Integer> from, int output, final Rect r, float width, float height, Vector4 offset, Vector4 mul, final boolean genMip) {
		final TriangleMesh mesh = new BasicGeometry.QuadMesh(StandardPass.render);
		mesh.rebuildTriangle(1);
		mesh.rebuildVertex(4);

		mesh.vertex().put((float) (r.x + r.w)).put((float) r.y).put(0.5f).put((float) (r.x + r.w)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y)).put(0.5f);
		mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 3);
		mesh.aux(Base.texture0_id, 2).put(width).put(0).put(width).put(height).put(0).put(height).put(0).put(0);
		mesh.aux(Base.color0_id, 4).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1);

		final boolean useRect = !(width == 1 && height == 1);
		// onscreen program
		// PutImageProcessingOnscreenFragmentSquare
		final BasicGLSLangProgram onscreenProgram = (width == 1 && height == 1 ? new BasicGLSLangProgram("content/shaders/NDCvertex.glslang", "content/shaders/PutImageProcessingOnscreenFragmentSquare.glslang") {
			@Override
			public String toString() {
				return "onscreen quad";
			}
		} : new BasicGLSLangProgram("content/shaders/NDCvertex.glslang", "content/shaders/PutImageProcessingOnscreenFragmentRect.glslang"));
		onscreenProgram.new SetIntegerUniform("depthTexture", 0);
		onscreenProgram.new SetUniform("offset", offset);
		onscreenProgram.new SetUniform("mul", mul);
		onscreenProgram.addChild(mesh);
		onscreenProgram.addChild(new TextureWrapper(genMip, useRect, from, 0));
		onscreenProgram.addChild(new BasicUtilities.DisableDepthTest(true));
		onscreenProgram.addChild(new BasicUtilities.DepthMask());

		into.addChild(onscreenProgram);

		return new iReposition() {

			BasicGLSLangProgram program = onscreenProgram;

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
	
	static public iReposition placeOnscreen(final BasicSceneList into, final iProvider<Integer> from, final Rect r, float width, float height, final BasicGLSLangProgram onscreenProgram) {
		final TriangleMesh mesh = new BasicGeometry.QuadMesh(StandardPass.render);
		mesh.rebuildTriangle(1);
		mesh.rebuildVertex(4);

		mesh.vertex().put((float) (r.x + r.w)).put((float) r.y).put(0.5f).put((float) (r.x + r.w)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y)).put(0.5f);
		mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 3);
		mesh.aux(Base.texture0_id, 2).put(width).put(0).put(width).put(height).put(0).put(height).put(0).put(0);
		mesh.aux(Base.color0_id, 4).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1);

		final boolean useRect = !(width == 1 && height == 1);
		// onscreen program
		// PutImageProcessingOnscreenFragmentSquare
		onscreenProgram.new SetIntegerUniform("depthTexture", 0);
		onscreenProgram.addChild(mesh);
		onscreenProgram.addChild(new TextureWrapper(false, useRect, from, 0));
		onscreenProgram.addChild(new BasicUtilities.DisableDepthTest(true));
		onscreenProgram.addChild(new BasicUtilities.DepthMask());
		
		into.addChild(onscreenProgram);

		return new iReposition() {

			BasicGLSLangProgram program = onscreenProgram;

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

	static public iReposition placeOnscreenWithMask(final BasicSceneList into, final iProvider<Integer> from, int output, final Rect r, final float width, final float height, Vector4 offset, Vector4 mul, final boolean genMip, String mask) {
		final TriangleMesh mesh = new BasicGeometry.QuadMesh(StandardPass.render);
		mesh.rebuildTriangle(1);
		mesh.rebuildVertex(4);

		mesh.vertex().put((float) (r.x + r.w)).put((float) r.y).put(0.5f).put((float) (r.x + r.w)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y)).put(0.5f);
		mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 3);
		mesh.aux(Base.texture0_id, 2).put(width).put(0).put(width).put(height).put(0).put(height).put(0).put(0);
		mesh.aux(Base.texture0_id + 1, 2).put(width).put(0).put(width).put(height).put(0).put(height).put(0).put(0);

		final iUpdateable u = new iUpdateable() {

			public void update() {
				float r = (float) Math.random();
				float t = (float) Math.random();
				mesh.aux(Base.texture0_id + 1, 2).put(width + r).put(t).put(width + r).put(height + t).put(r).put(height + t).put(r).put(t);
			}
		};
		Launcher.getLauncher().registerUpdateable(u);

		mesh.aux(Base.color0_id, 4).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1);

		
		
		BasicTextures.TextureFromQTImage uu = new BasicTextures.TextureFromQTImage(mask);
		uu.gl_texture_wrap_s(GL_REPEAT);
		uu.gl_texture_wrap_t(GL_REPEAT);
		mesh.addChild(new TextureUnit(1, uu.use_gl_texture_rectangle_ext(!(width == 1 && height == 1))));

		final boolean useRect = !(width == 1 && height == 1);
		// onscreen program
		final BasicGLSLangProgram onscreenProgram = (width == 1 && height == 1 ? new BasicGLSLangProgram("content/shaders/NDCvertex.glslang", "content/shaders/PutImageProcessingOnscreenFragmentSquareMask.glslang") : new BasicGLSLangProgram("content/shaders/NDCvertex.glslang", "content/shaders/PutImageProcessingOnscreenFragmentRectMask.glslang"));
		onscreenProgram.new SetIntegerUniform("depthTexture", 0);
		onscreenProgram.new SetIntegerUniform("maskTexture", 1);
		onscreenProgram.new SetUniform("offset", offset);
		onscreenProgram.new SetUniform("mul", mul);
		onscreenProgram.addChild(mesh);
		onscreenProgram.addChild(new TextureWrapper(genMip, useRect, from, 0));
		onscreenProgram.addChild(new BasicUtilities.DisableDepthTest(true));
		final Vector4 op = new Vector4(1,1,1,1);
		onscreenProgram.new SetUniform("opacity", op);
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
				Launcher.getLauncher().deregisterUpdateable(u);
			}

			public void add() {
				if (!into.isChild(onscreenProgram))
					into.addChild(onscreenProgram);
				Launcher.getLauncher().registerUpdateable(u);
			}
			
			public void setFade(float f)
			{
				op.w = f;
			}

		};
	}

	public iSceneListElement getOnscreenList(final Rect r) {
		return getOnscreenList(0, r, new Vector4(0, 0, 0, 0), new Vector4(1, 1, 1, 1), false);
	}

	public iSceneListElement getOnscreenList(int output, final Rect r, Vector4 offset, Vector4 mul, final boolean genMip) {
		final TriangleMesh mesh = new BasicGeometry.QuadMesh(StandardPass.render);
		mesh.rebuildTriangle(1);
		mesh.rebuildVertex(4);

		mesh.vertex().put((float) (r.x + r.w)).put((float) r.y).put(0.5f).put((float) (r.x + r.w)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y + r.h)).put(0.5f).put((float) (r.x)).put((float) (r.y)).put(0.5f);
		mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 3);
		mesh.aux(Base.texture0_id, 2).put(useRect ? width : 1).put(0).put(useRect ? width : 1).put(useRect ? height : 1).put(0).put(useRect ? height : 1).put(0).put(0);
		mesh.aux(Base.color0_id, 4).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1).put(1);

		// onscreen program
		BasicGLSLangProgram onscreenProgram = (!useRect ? new BasicGLSLangProgram("content/shaders/NDCvertex.glslang", "content/shaders/PutImageProcessingOnscreenFragmentSquare.glslang") : new BasicGLSLangProgram("content/shaders/NDCvertex.glslang", "content/shaders/PutImageProcessingOnscreenFragmentRect.glslang"));
		onscreenProgram.new SetIntegerUniform("depthTexture", 0);
		onscreenProgram.new SetUniform("offset", offset);
		onscreenProgram.new SetUniform("mul", mul);
		onscreenProgram.addChild(mesh);
		onscreenProgram.addChild(new TextureWrapper(genMip, useRect, getOutput(0), 0));
		onscreenProgram.addChild(new BasicUtilities.DisableDepthTest(true));

		return onscreenProgram;
	}

	private TriangleMesh mesh;

	protected iProvider<Integer>[] fboInput;

	protected final int width;

	protected final int height;

	protected final boolean useRect;

	protected final boolean genMipmap;

	protected final boolean useFloat;

	protected int[] blendMode = { GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA };


	protected int[] sampleCount = new int[1];
	boolean initialized = false;

	Vector4 clearColor = null;

	boolean first = true;

	int[] available = new int[1];

	List<iSceneListElement> children = new ArrayList<iSceneListElement>();

	TaskQueue queue = new TaskQueue();

	boolean clearOnce = false;

	int[] fbo = { -1 };

	int[] rb = { -1 };

	int[] tex = { -1 };

	int[] query = new int[1];

	private FloatBuffer storage;

	public ImageProcessing(ImageProcessing i, int width, int height, boolean useRect, boolean genMipmap, boolean useFloat) {
		this(new iProvider[] { i.getFBOOutput() }, width, height, useRect, genMipmap, useFloat);
	}

	public ImageProcessing(TwoPassImageProcessing i, int width, int height, boolean useRect, boolean genMipmap, boolean useFloat) {
		this(new iProvider[] { i.getOutput(0) }, width, height, useRect, genMipmap, useFloat);
	}

	public ImageProcessing(TwoPassImageProcessingTwoOutput i, int width, int height, boolean useRect, boolean genMipmap, boolean useFloat) {
		this(new iProvider[] { i.getOutput(0), i.getOutput(1) }, width, height, useRect, genMipmap, useFloat);
	}

	public ImageProcessing(final iHasFBO i, int width, int height, boolean useRect, boolean genMipmap, boolean useFloat) {
		this(new iProvider[] { new iProvider<Integer>() {
			public Integer get() {
				return i.getFBO();
			}
		} }, width, height, useRect, genMipmap, useFloat);
	}

	public ImageProcessing(iProvider<Integer>[] fboInput, int width, int height, boolean useRect, boolean genMipmap, boolean useFloat) {

		this.fboInput = fboInput;
		this.width = width;
		this.height = height;
		this.useRect = useRect;

		this.genMipmap = genMipmap;
		this.useFloat = useFloat;
		if (genMipmap)
			assert !useFloat;

		initializeMesh();

	}

	public void setInputs(iProvider<Integer>[] input) {
		this.fboInput = input;
	}

	public void addChild(final iSceneListElement e) {
		mesh.addChild(e);
		children.add(e);
		if (e instanceof iProcessesMesh) {
			queue.new Task() {
				@Override
				protected void run() {

					((iProcessesMesh) e).process(mesh);

					recur();
				}
			};
		}
	}

	@Override
	public void removeChild(iSceneListElement e) {
		mesh.removeChild(e);
		children.remove(e);
	}

	public boolean isChild(iSceneListElement e) {
		return mesh.isChild(e);
	}

	public void addFadePlane(final iFloatProvider amount1, Vector4 color1, final iFloatProvider amount2, Vector4 color2) {
		final QuadMesh mesh = new BasicGeometry.QuadMesh(Base.StandardPass.render);
		mesh.rebuildTriangle(1);
		mesh.rebuildVertex(4);

		mesh.vertex().put(-1f).put(-1f).put(0f).put(-1f).put(1f).put(0f).put(1f).put(1f).put(0f).put(1f).put(-1f).put(0f);
		mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 3);
		mesh.addChild(new BasicGLSLangProgram("content/shaders/NDC2ColorVertex.glslang", "content/shaders/VertexColor2Fragment.glslang"));
		mesh.addChild(new BasicUtilities.DepthMask(Base.StandardPass.transform, Base.StandardPass.postRender));

		final FloatBuffer root = ByteBuffer.allocate(mesh.vertex().limit() * 4).asFloatBuffer().put(mesh.vertex());

		// driver bug. Horrible seam down middle of trianglulation
		mesh.addChild(new BasicUtilities.OnePassElement(StandardPass.preRender) {
			@Override
			public void performPass() {
				glColorMask(false, false, false, true);
				glClearColor(0, 0, 0, 0);
				glClear(GL_COLOR_BUFFER_BIT);
				glColorMask(true, true, true, true);

				// glColorMask(false, false, false, true);
				// glClearColor(0, 0, 0, 0);
				// glClear(GL_COLOR_BUFFER_BIT |
				// GL_DEPTH_BUFFER_BIT);
				// glColorMask(true, true, true, true);

				// FloatBuffer m = mesh.vertex();
				// for (int i = 0; i < m.capacity(); i++) {
				// m.put(i, (float) (root.get(i) * (1 +
				// Math.random() * 0.01f)));
				// }

			}
		});
		// mesh.addChild(new
		// BasicUtilities.SetBlendMode(StandardPass.preRender,
		// GL_CONSTANT_ALPHA, GL_ONE_MINUS_CONSTANT_ALPHA, new
		// Vector4(0, 0, 0, 0.05f)));
		// mesh.addChild(new
		// BasicUtilities.SetBlendMode(StandardPass.preDisplay,
		// GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, new Vector4(0, 0,
		// 0, 0.05f)));

		queue.new Task() {
			int c = 0;

			@Override
			protected void run() {

				float colorAlpha = amount1.evaluate();
				float alphaAlpha = amount2.evaluate();

				c++;
				if (c < 5) {
					mesh.aux(Base.color0_id, 4).put(new float[] { 0, 0, 0, colorAlpha, 0, 0, 0, colorAlpha, 0, 0, 0, colorAlpha, 0, 0, 0, colorAlpha });
					// mesh.aux(Base.color0_id + 1,
					// 4).put(new float[] { 0.5f, 0.5f,
					// 0.5f, alphaAlpha, 0.5f, 0.5f, 0.5f,
					// alphaAlpha, 0.5f, 0.5f, 0.5f,
					// alphaAlpha, 0.5f, 0.5f, 0.5f,
					// alphaAlpha });
					mesh.aux(Base.color0_id + 1, 4).put(new float[] { 0.5f, 0.5f, 0.0f, alphaAlpha, 0.5f, 0.5f, 0.0f, alphaAlpha, 0.5f, 0.5f, 0.0f, alphaAlpha, 0.5f, 0.5f, 0.0f, alphaAlpha });
				}
				mesh.performPass(null);
				recur();
			}
		};
	}

	public void clearOnce() {
		clearOnce = true;
	}

	public iProvider<Integer> getFBOOutput() {
		return new iProvider<Integer>() {
			public Integer get() {
				return fbo[0];
			}
		};
	}

	public TriangleMesh getGeometry() {
		return mesh;
	}

	public iProvider<Integer> getOutput(int num) {
		return new iProvider<Integer>() {
			public Integer get() {
				// assert tex[0] != -1 : "not initialized";
				if (tex[0] == -1)
					initialize();
				return tex[0];
			}
		};
	}

	BasicUtilities.TwoPassElement element = null;
	BasicUtilities.TwoPassElement elementWrapped = null;

	public BasicUtilities.TwoPassElement getOutputElement(int num) {

		if (element == null)
			element = new BasicUtilities.TwoPassElement("", StandardPass.preRender, StandardPass.postRender) {

				@Override
				protected void post() {
					glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
					CoreHelpers.glDisable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
				}

				@Override
				protected void pre() {

					if (!initialized)
						initialize();

					glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, getOutput(0).get());
					CoreHelpers.glEnable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
				}

				@Override
				protected void setup() {
					BasicContextManager.putId(this, 1);
				}
			};
		return element;
	}

	public BasicUtilities.TwoPassElement getOutputElementWrapped(int num) {

		if (elementWrapped == null)
			elementWrapped = new TextureUnit(0, new BasicUtilities.TwoPassElement("", StandardPass.preRender, StandardPass.postRender) {

				@Override
				protected void post() {
					glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
					CoreHelpers.glDisable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
				}

				@Override
				protected void pre() {

					if (!initialized)
						initialize();

					glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, getOutput(0).get());
					CoreHelpers.glEnable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
				}

				@Override
				protected void setup() {
					BasicContextManager.putId(this, 1);
				}
			});
		return elementWrapped;
	}

	public TaskQueue getRenderQueue() {
		return queue;
	}

	public iUpdateable getUpdate() {
		return new iUpdateable() {
			public void update() {
				ImageProcessing.this.update();
			}
		};
	}

	public void initialize() {

		assert glGetError() == 0;

		// fbo & texture -------------------------------------------
		fbo[0] = glGenFramebuffers();
		rb[0] = glGenRenderbuffers();
		tex[0] = glGenTextures();
		glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);

		int gl_texture_min_filter = genMipmap ? GL_LINEAR_MIPMAP_LINEAR : GL_LINEAR;
		int gl_texture_mag_filter = GL_LINEAR;

		glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[0]);
//		glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 0);

		// glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE :
		// GL_TEXTURE_2D, 0, useFloat ? GL_RGBA16F_ARB :
		// GL_RGBA, width, height, 0, GL_BGRA, useFloat ?
		// GL11.GL_FLOAT : GL_UNSIGNED_INT_8_8_8_8_REV, null);

		glTexImage2D(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0, useFloat ? GL_RGBA16F : GL_RGBA8, width, height, 0, GL_RGBA, useFloat ? GL11.GL_FLOAT : GL_UNSIGNED_BYTE, (ByteBuffer)null);

		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, tex[0], 0);
		glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
		glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);

		// glBindRenderbuffer(GL_RENDERBUFFER, rb[0]);
		// glRenderbufferStorage(GL_RENDERBUFFER,
		// GL_DEPTH_COMPONENT32, width, height);
		// glFramebufferRenderbuffer(GL_FRAMEBUFFER,
		// GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rb[0]);

		int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
		assert status == GL_FRAMEBUFFER_COMPLETE : status;

		;//System.out.println(" fbo complete ? " + this + " " + status + " " + GL_FRAMEBUFFER_COMPLETE + " " + useFloat + " " + useRect + " " + width + " " + height);
		// new Exception().printStackTrace();

		BasicContextManager.putId(this, fbo[0]);

		glClearColor(0, 0, 0, 0);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		glBindFramebuffer(GL_FRAMEBUFFER, 0);

		initialized = true;
	}

	public void join(FullScreenCanvasSWT c, Method m) {
		aRun arun = new Cont.aRun() {
			@Override
			public ReturnCode head(Object calledOn, Object[] args) {
				Vector4 old = null;
				if (clearOnce) {
					old = clearColor;
					clearColor = new Vector4(0, 0, 0, 1);
				}
				update();
				if (clearOnce) {
					clearColor = old;
				}
				clearOnce = false;

				return super.head(calledOn, args);
			}
		};

		if (c.getSceneListSide() == StereoSide.middle)
			Cont.linkWith(c, c.method_beforeFlush, arun);
		else {
			Method attachMethod = c.getSceneListSide() == StereoSide.left ? c.method_beforeLeftFlush : c.method_beforeRightFlush;
			Cont.linkWith(c, attachMethod, arun);
		}

	}

	public void join(FullScreenCanvasSWT c) {
		join(c, FullScreenCanvasSWT.method_beforeFlush);
	}

	public ImageProcessing setBlendMode(int colorSrc, int colorDest, int alphaSrc, int alphaDest) {
		this.blendMode = new int[] { colorSrc, colorDest, alphaSrc, alphaDest };
		return this;
	}

	public ImageProcessing setBlendMode(KnownBlendMode known) {
		this.blendMode = new int[] { known.sc, known.dc, known.sa, known.da };
		return this;
	}

	public ImageProcessing setClearColor(Vector4 clearColor) {
		this.clearColor = clearColor;
		return this;
	}

	// for debugging only, we should have a frame
	// buffer that supports async reads from a
	// renderbuffer as well
	public ByteBuffer getByteImage(ByteBuffer storage) {
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

	// for debugging only, we should have a frame
	// buffer that supports async reads from a
	// renderbuffer as well
	public FloatBuffer getFloatImage(FloatBuffer storage) {
		if (storage == null && this.storage == null) {
			this.storage = storage = ByteBuffer.allocateDirect(width * height * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		} else if (storage == null)
			storage = this.storage;

		int[] a = new int[1];
		a[0] = glGetInteger(GL_FRAMEBUFFER_BINDING);
		glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
		glReadPixels(0, 0, width, height, GL_RGBA, GL11.GL_FLOAT, storage);
		glBindFramebuffer(GL_FRAMEBUFFER, a[0]);

		return storage;
	}

	public void update() {

		if (!initialized)
			initialize();

		int current = getCurrentFBO();

		glBindFramebuffer(GL_FRAMEBUFFER, current);
		glViewport(0, 0, width, height);

		if (first) {
			glClearColor(0, 0, 0, 1f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		}

		if (clearColor != null) {
			glClearColor(clearColor.x, clearColor.y, clearColor.z, clearColor.w);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		} else {
			glClear(GL_DEPTH_BUFFER_BIT);
		}

		for (int i = 0; i < fboInput.length; i++) {
						
			glActiveTexture(GL_TEXTURE0 + i);
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, fboInput[i].get());
			CoreHelpers.glEnable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
			
//			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
//			glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			
		}

		glBlendFuncSeparate(blendMode[0], blendMode[1], blendMode[2], blendMode[3]);

		if (!first) {
			histogramPre();
		}

		histogramPreFirst();

		queue.update();

		
		renderGeometry();

		histogramPost();

		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		for (int i = 0; i < fboInput.length; i++) {
			glActiveTexture(GL_TEXTURE0 + i);
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
		}
		glActiveTexture(GL_TEXTURE0);

		glBindFramebuffer(GL_FRAMEBUFFER, 0);

		if (genMipmap) {
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, getOutput(0).get());
			glGenerateMipmap(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
		}

		first = false;
//		;//System.out.println(" << image processing update ");

	}

	public void useHighResolutionMesh(int devision) {
		mesh = new BasicGeometry.QuadMesh(Base.StandardPass.preRender);
		mesh.rebuildTriangle(devision * devision);
		mesh.rebuildVertex((1 + devision) * (1 + devision));

		FloatBuffer v = mesh.vertex();

		FloatBuffer tex = mesh.aux(Base.texture0_id, 2);

		for (int x = 0; x < devision + 1; x++) {
			for (int y = 0; y < devision + 1; y++) {
				v.put(-1 + 2 * x / (float) devision).put(-1 + 2 * y / (float) devision).put(0.5f);
				tex.put((useRect ? width : 1) * x / (float) devision).put((useRect ? height : 1) * y / (float) devision);
			}
		}

		ShortBuffer s = mesh.triangle();

		for (int x = 0; x < devision; x++) {
			for (int y = 0; y < devision; y++) {
				s.put((short) (y * (devision + 1) + x)).put((short) (y * (devision + 1) + x + 1)).put((short) ((y + 1) * (devision + 1) + x + 1)).put((short) ((y + 1) * (devision + 1) + x));
			}
		}

	}

	private int getCurrentFBO() {
		assert fbo[0] != -1;
		return fbo[0];
	}

	protected void histogramPost() {
	}

	protected void histogramPre() {
	}

	protected void histogramPreFirst() {
	}

	protected void initializeMesh() {
		// geometry ------------------------------------
		mesh = new BasicGeometry.TriangleMesh(Base.StandardPass.preRender);
		mesh.rebuildTriangle(2);
		mesh.rebuildVertex(4);

		mesh.vertex().put(-1).put(-1).put(0.5f).put(-1).put(1).put(0.5f).put(1).put(1).put(0.5f).put(1).put(-1).put(0.5f);
		mesh.triangle().put((short) 0).put((short) 1).put((short) 2).put((short) 0).put((short) 2).put((short) 3);
		if (useRect)
			mesh.aux(Base.texture0_id, 2).put(0).put(0).put(0).put(height).put(width).put(height).put(width).put(0);
		else
			mesh.aux(Base.texture0_id, 2).put(0).put(0).put(0).put(1).put(1).put(1).put(1).put(0);
	}

	protected void renderGeometry() {
		mesh.performPass();
	}

}
