package field.graphics.imageprocessing;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;

import java.lang.reflect.Method;
import java.nio.FloatBuffer;

import field.bytecode.protect.dispatch.Cont;
import field.bytecode.protect.dispatch.Cont.ReturnCode;
import field.bytecode.protect.dispatch.Cont.aRun;
import field.core.dispatch.iVisualElement.Rect;
import field.graphics.core.Base;
import field.graphics.core.Base.StandardPass;
import field.graphics.core.Base.iSceneListElement;
import field.graphics.core.BasicContextManager;
import field.graphics.core.BasicFrameBuffers.iHasFBO;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.BasicGeometry;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.graphics.core.BasicUtilities;
import field.graphics.imageprocessing.ImageProcessing.TextureWrapper;
import field.graphics.windowing.FullScreenCanvasSWT;
import field.graphics.windowing.FullScreenCanvasSWT.StereoSide;
import field.math.abstraction.iProvider;
import field.math.linalg.Vector4;

/**
 * binds two image processors together
 * 
 * @author marc
 * 
 */
public class TwoPassImageProcessing implements iImageProcessor {

	private final ImageProcessing left;

	private final ImageProcessing right;

	private final boolean doBothPasses;

	private final boolean useRect;

	boolean leftJustFinished = false;

	private FloatBuffer storage;

	public TwoPassImageProcessing(ImageProcessing i, int width, int height, final boolean useRect, boolean genMipmap, boolean useFloat, boolean doBothPasses) {
		this(new iProvider[] { i.getFBOOutput() }, width, height, useRect, genMipmap, useFloat, doBothPasses);
	}

	public TwoPassImageProcessing(int width, int height, final boolean useRect, boolean genMipmap, boolean useFloat, boolean doBothPasses) {
		this(new iProvider[] {}, width, height, useRect, genMipmap, useFloat, doBothPasses);
	}

	public TwoPassImageProcessing(final iHasFBO i, int width, int height, final boolean useRect, boolean genMipmap, boolean useFloat, boolean doBothPasses) {
		this(new iProvider[] { new iProvider<Integer>() {
			public Integer get() {
				return i.getFBO();
			}
		} }, width, height, useRect, genMipmap, useFloat, doBothPasses);
	}

	public TwoPassImageProcessing(TwoPassImageProcessing i, int width, int height, final boolean useRect, boolean genMipmap, boolean useFloat, boolean doBothPasses) {
		this(new iProvider[] { i.getOutput(0), }, width, height, useRect, genMipmap, useFloat, doBothPasses);
	}

	public TwoPassImageProcessing(TwoPassImageProcessingTwoOutput i, int width, int height, final boolean useRect, boolean genMipmap, boolean useFloat, boolean doBothPasses) {
		this(new iProvider[] { i.getOutput(0), i.getOutput(1), }, width, height, useRect, genMipmap, useFloat, doBothPasses);
	}

	@SuppressWarnings("unchecked")
	public TwoPassImageProcessing(iProvider<Integer>[] fboInputs, int width, int height, final boolean useRect, boolean genMipmap, boolean useFloat, boolean doBothPasses) {
		this.useRect = useRect;
		this.doBothPasses = doBothPasses;

		iProvider<Integer>[] fboInputsLeft = new iProvider[fboInputs.length + 1];
		System.arraycopy(fboInputs, 0, fboInputsLeft, 0, fboInputs.length);
		fboInputsLeft[fboInputs.length] = new iProvider<Integer>() {
			public Integer get() {
				return right.getOutput(0).get();
			}
		};

		iProvider<Integer>[] fboInputsRight = new iProvider[fboInputs.length + 1];
		if (!doBothPasses) {
			System.arraycopy(fboInputs, 0, fboInputsRight, 0, fboInputs.length);
			fboInputsRight[fboInputs.length] = new iProvider<Integer>() {
				public Integer get() {
					return left.getOutput(0).get();
				}
			};
		} else {
			System.arraycopy(fboInputs, 0, fboInputsRight, 1, fboInputs.length);
			fboInputsRight[0] = new iProvider<Integer>() {
				public Integer get() {
					return left.getOutput(0).get();
				}
			};

		}

		left = new ImageProcessing(fboInputsLeft, width, height, useRect, genMipmap, useFloat);
		right = new ImageProcessing(fboInputsRight, width, height, useRect, genMipmap, useFloat);
	}

	public void addChild(iSceneListElement e) {
		left.addChild(e);
		right.addChild(e);
	}

	public void addChildOne(iSceneListElement e) {
		left.addChild(e);
	}

	public void addChildTwo(iSceneListElement e) {
		right.addChild(e);
	}

	public iProvider<Integer> getOutput(int num) {
		final iProvider<Integer> lo = left.getOutput(0);
		final iProvider<Integer> ro = right.getOutput(0);
		return new iProvider<Integer>() {
			public Integer get() {
				return leftJustFinished ? lo.get() : ro.get();
			}
		};
	}

	public BasicUtilities.TwoPassElement getOutputElement(int num) {
		return new BasicUtilities.TwoPassElement("", StandardPass.preRender, StandardPass.postRender) {

			@Override
			protected void post() {
				glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
				glDisable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			}

			@Override
			protected void pre() {

				if (!left.initialized)
					left.initialize();
				if (!right.initialized)
					right.initialize();

				glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, getOutput(0).get());
				glEnable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			}

			@Override
			protected void setup() {
				BasicContextManager.putId(this, 1);
			}
		};
	}

////	// you can only call this inside the gl render pass
////	protected FloatBuffer getFloatImage(FloatBuffer storage) {
////		if (storage == null && this.storage == null) {
////			this.storage = storage = ByteBuffer.allocateDirect(left.width * left.height * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
////		} else if (storage == null)
////			storage = this.storage;
////
////		int[] a = new int[1];
////		left.glGetIntegerv(left.GL_FRAMEBUFFER_BINDING, a, 0);
////		left.glBindFramebufferEXT(left.GL_FRAMEBUFFER, getOutput(0).get());
////		left.glReadPixels(0, 0, left.width, left.height, left.GL_RGBA, left.GL_FLOAT, storage);
////		left.glBindFramebufferEXT(left.GL_FRAMEBUFFER, a[0]);
////
////		return storage;
////	}
//
//	public void copyOutputToFloatArrayNow(final float[][] out) {
//		FloatBuffer image = getFloatImage(null);
//
//
//		for (int i = 0; i < out.length; i++) {
//			for (int j = 0; j < out[i].length; j++) {
//				out[i][j] = image.get();
//				image.get();
//				image.get();
//				image.get();
//			}
//		}
//	}
//
//	public void copyOutputToFloatArray(final float[][] output) {
//		left.queue.new Task() {
//
//			@Override
//			protected void run() {
//				FloatBuffer image = left.getFloatImage(null);
//				for (int y = 0; y < output[0].length; y++) {
//					for (int x = 0; x < output.length; x++) {
//						output[x][y] = image.get(y * 4 * left.width + 4 * x);
//					}
//				}
//			}
//		};
//		right.queue.new Task() {
//
//			@Override
//			protected void run() {
//				FloatBuffer image = right.getFloatImage(null);
//				for (int y = 0; y < output[0].length; y++) {
//					for (int x = 0; x < output.length; x++) {
//						output[x][y] = image.get(y * 4 * left.width + 4 * x);
//					}
//				}
//			}
//		};
//	}

	boolean paused = false;

	public void join(FullScreenCanvasSWT c) {
		aRun arun = new Cont.aRun() {
			@Override
			public ReturnCode head(Object calledOn, Object[] args) {
				if (paused)
					return super.head(calledOn, args);
				update();
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

	

	public void update() {
		if (paused)
			return;
		for (int i = 0; i < (doBothPasses ? 2 : 1); i++) {
			if (leftJustFinished) {
				right.update();
				leftJustFinished = false;
			} else {
				left.update();
				leftJustFinished = true;
			}
		}
	}

	public void setClearColor(Vector4 color) {
		left.setClearColor(color);
		right.setClearColor(color);
	}

	public void useHighResolutionMesh(int devision) {
		left.useHighResolutionMesh(devision);
		right.useHighResolutionMesh(devision);
	}

	public void pause() {
		paused = true;
	}

	public void unpause() {
		paused = false;
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
		mesh.aux(Base.texture0_id, 2).put(useRect ? left.width : 1).put(0).put(useRect ? left.width : 1).put(useRect ? left.height : 1).put(0).put(useRect ? left.height : 1).put(0).put(0);
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
	
}
