package field.graphics.imageprocessing;

import java.lang.reflect.Method;

import field.bytecode.protect.dispatch.Cont;
import field.bytecode.protect.dispatch.Cont.ReturnCode;
import field.bytecode.protect.dispatch.Cont.aRun;
import field.core.dispatch.iVisualElement.Rect;
import field.graphics.core.Base;
import field.graphics.core.Base.StandardPass;
import field.graphics.core.Base.iSceneListElement;
import field.graphics.core.BasicContextManager;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.BasicGeometry;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.graphics.core.BasicUtilities;
import field.graphics.imageprocessing.ImageProcessing.TextureWrapper;
import field.graphics.windowing.FullScreenCanvasSWT;
import field.graphics.windowing.FullScreenCanvasSWT.StereoSide;
import field.math.abstraction.iAcceptor;
import field.math.abstraction.iProvider;
import field.math.linalg.Vector4;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;
/**
 * binds two image processors together
 * 
 * @author marc
 * 
 */
public class TwoPassImageProcessingTwoOutputMultisampled implements iImageProcessor {

	private ImageProcessingTwoOutputMultisampled left;

	private ImageProcessingTwoOutputMultisampled right;

	private final boolean doBothPasses;

	private final boolean useRect;

	@SuppressWarnings("unchecked")
	public TwoPassImageProcessingTwoOutputMultisampled(iProvider<Integer>[] fboInputs, int width, int height, final boolean useRect, boolean genMipmap, boolean useFloat, boolean doBothPasses) {
		this.useRect = useRect;
		this.doBothPasses = doBothPasses;

		iProvider<Integer>[] fboInputsLeft = (iProvider<Integer>[]) new iProvider[fboInputs.length + 2];
		System.arraycopy(fboInputs, 0, fboInputsLeft, 0, fboInputs.length);
		fboInputsLeft[fboInputs.length] = new iProvider<Integer>() {
			public Integer get() {
				return right.getOutput(0).get();
			}
		};
		fboInputsLeft[fboInputs.length + 1] = new iProvider<Integer>() {
			public Integer get() {
				return right.getOutput(1).get();
			}
		};

		iProvider<Integer>[] fboInputsRight = (iProvider<Integer>[]) new iProvider[fboInputs.length + 2];
		System.arraycopy(fboInputs, 0, fboInputsRight, 0, fboInputs.length);
		fboInputsRight[fboInputs.length] = new iProvider<Integer>() {
			public Integer get() {
				return left.getOutput(0).get();
			}
		};
		fboInputsRight[fboInputs.length + 1] = new iProvider<Integer>() {
			public Integer get() {
				return left.getOutput(1).get();
			}
		};

		left = new ImageProcessingTwoOutputMultisampled(fboInputsLeft, width, height, useRect, genMipmap, useFloat);
		right = new ImageProcessingTwoOutputMultisampled(fboInputsRight, width, height, useRect, genMipmap, useFloat);
	}

	public void join(FullScreenCanvasSWT c) {
		aRun arun = new Cont.aRun() {
			@Override
			public ReturnCode head(Object calledOn, Object[] args) {
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

	boolean leftJustFinished = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * innards.graphics.basic.iImageProcessor#addChild(innards.graphics.
	 * Base.iSceneListElement)
	 */
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

	public iAcceptor<Vector4[]> addFadePlane()
	{
		final iAcceptor<Vector4[]> lp = left.addFadePlane();
		final iAcceptor<Vector4[]> rp = right.addFadePlane();
		
		return new iAcceptor<Vector4[]>() {
			@Override
			public iAcceptor<Vector4[]> set(Vector4[] to) {
				lp.set(to);
				rp.set(to);
				return this;
			}
		};
	}
	
	public void update() {
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see innards.graphics.basic.iImageProcessor#getOutput()
	 */
	public iProvider<Integer> getOutput(int num) {
		final iProvider<Integer> lo = left.getOutput(num);
		final iProvider<Integer> ro = right.getOutput(num);
		return new iProvider<Integer>() {
			public Integer get() {
				return leftJustFinished ? lo.get() : ro.get();
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see innards.graphics.basic.iImageProcessor#getOutputElement()
	 */
	public BasicUtilities.TwoPassElement getOutputElement(final int num) {
		return new BasicUtilities.TwoPassElement("", StandardPass.preRender, StandardPass.postRender) {

			@Override
			protected void setup() {
				BasicContextManager.putId(this, 1);
			}

			@Override
			protected void pre() {

				if (!left.initialized)
					left.initialize();
				if (!right.initialized)
					right.initialize();

				glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, getOutput(num).get());
				glEnable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			}

			@Override
			protected void post() {
				glBindTexture(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, 0);
				glDisable(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D);
			}
		};
	}

	public void useHighResolutionMesh(int devision) {
		left.useHighResolutionMesh(devision);
		right.useHighResolutionMesh(devision);
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
		onscreenProgram.addChild(new TextureWrapper(genMip, useRect, getOutput(output), 0));
		onscreenProgram.addChild(new BasicUtilities.DisableDepthTest(true));

		return onscreenProgram;
	}
}
