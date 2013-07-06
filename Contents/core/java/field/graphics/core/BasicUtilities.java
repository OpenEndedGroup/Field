package field.graphics.core;

import static org.lwjgl.opengl.GL11.GL_ALPHA_TEST;
import static org.lwjgl.opengl.GL11.GL_ALWAYS;
import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CONSTANT_ALPHA;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FILL;
import static org.lwjgl.opengl.GL11.GL_FRONT;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11.GL_GREATER;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_LINE;
import static org.lwjgl.opengl.GL11.GL_LINE_SMOOTH;
import static org.lwjgl.opengl.GL11.GL_LINE_SMOOTH_HINT;
import static org.lwjgl.opengl.GL11.GL_LINE_STIPPLE;
import static org.lwjgl.opengl.GL11.GL_NICEST;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_POINT_SMOOTH;
import static org.lwjgl.opengl.GL11.GL_POINT_SMOOTH_HINT;
import static org.lwjgl.opengl.GL11.GL_POLYGON_OFFSET_FILL;
import static org.lwjgl.opengl.GL11.GL_POLYGON_OFFSET_LINE;
import static org.lwjgl.opengl.GL11.GL_POLYGON_SMOOTH;
import static org.lwjgl.opengl.GL11.GL_POLYGON_SMOOTH_HINT;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_ZERO;
import static org.lwjgl.opengl.GL11.glAlphaFunc;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glClearDepth;
import static org.lwjgl.opengl.GL11.glColorMask;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glHint;
import static org.lwjgl.opengl.GL11.glIsEnabled;
import static org.lwjgl.opengl.GL11.glLineStipple;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glPolygonMode;
import static org.lwjgl.opengl.GL11.glPolygonOffset;
import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL14.glBlendColor;
import static org.lwjgl.opengl.GL14.glBlendEquation;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.ConstantContext;
import field.bytecode.protect.annotations.DispatchOverTopology;
import field.bytecode.protect.annotations.HiddenInAutocomplete;
import field.bytecode.protect.dispatch.Cont;
import field.graphics.core.Base.StandardPass;
import field.graphics.core.Base.iPass;
import field.graphics.core.Base.iSceneListElement;
import field.graphics.windowing.FullScreenCanvasSWT;
import field.graphics.windowing.FullScreenCanvasSWT.StereoSide;
import field.launch.iUpdateable;
import field.math.abstraction.iFloatProvider;
import field.math.graph.iMutable;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Quaternion;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.ReflectionTools;

/**
 * some good, useful scenegraph elements that you might be interested in,
 * including:
 * 
 * clearing the screen, blending the screen ('motion' blur), controlling stencil
 * state (also useful with 'motion' blur);
 * 
 * and:
 * 
 * important base classes for making SceneListElements (BasicOnePassElement) and
 * BasicSceneList (BasicOnePassList) subclasses
 * 
 */
public class BasicUtilities {

	// for debugging only
	static public final boolean thinState = false;

	static public class ChangeLineWidth extends BasicUtilities.TwoPassElement implements iSceneListElement {

		private float o;

		private final iFloatProvider f;

		public ChangeLineWidth(String name, iFloatProvider f) {
			super(name, StandardPass.preTransform, StandardPass.preDisplay);
			this.f = f;
		}

		@Override
		protected void post() {
			BasicGeometry.globalLineScale = o;
		}

		@Override
		protected void pre() {
			o = BasicGeometry.globalLineScale;
			BasicGeometry.globalLineScale = f.evaluate() * o;
		}

		@Override
		protected void setup() {
		}
	}

	// public static FKey phase = new FKey("blurPhase").rootSet(0);
	//
	// public static FKey freq = new FKey("blurFreq").rootSet(1);

	static public class Clear extends OnePassElement implements iSceneListElement {

		Vector3 background = new Vector3(0.1, 0.1, 0.1);

		float alpha = 0;

		boolean disable = false;

		int x;

		public Clear(Vector3 colour) {
			super(StandardPass.preRender);
			background = colour;
		}

		public Clear(Vector3 colour, float alpha) {
			super(StandardPass.preRender);
			background = colour;
			this.alpha = alpha;
		}

		@Override
		public void performPass() {

			glColorMask(true, true, true, true);

			assert glGetError() == 0;
			if (disable)
				return;
			x++;

			if (FullScreenCanvasSWT.currentCanvas == null || (!FullScreenCanvasSWT.currentCanvas.passiveStereo || FullScreenCanvasSWT.currentCanvas.getSide() == StereoSide.right)) {
				glClearColor(background.get(0), background.get(1), background.get(2), alpha);
				assert glGetError() == 0;
				glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
				assert glGetError() == 0;
			}
		}

		public void setClearColor(Vector3 c) {
			background = c;
		}

		public void setClearColor(Vector3 c, float alpha) {
			background = c;
			this.alpha = alpha;
		}

		public void setDisable(boolean disable) {
			this.disable = disable;
		}
	}

	static public class ClearAlpha extends OnePassElement implements iSceneListElement {

		private final float to;

		public ClearAlpha(float to) {
			super(StandardPass.preRender);
			this.to = to;
		}

		public ClearAlpha(StandardPass pass, float to) {
			super(pass);
			this.to = to;
		}

		@Override
		public void performPass() {
			// glClear(GL_COLOR_BUFFER_BIT
			// | GL_DEPTH_BUFFER_BIT |
			// GL_STENCIL_BUFFER_BIT);

			glClearColor(0, 0, 0, to);
			glColorMask(false, false, false, true);
			glClear(GL_COLOR_BUFFER_BIT);
			glColorMask(true, true, true, true);
		}
	}

	static public class ClearColor extends OnePassElement implements iSceneListElement {

		Vector3 background = new Vector3(0.1, 0.1, 0.1);

		float alpha = 0;

		boolean disable = false;

		int x;

		public ClearColor(Vector3 colour) {
			super(StandardPass.preRender);
			background = colour;
		}

		public ClearColor(Vector3 colour, float alpha) {
			super(StandardPass.preRender);
			background = colour;
			this.alpha = alpha;
		}

		@Override
		public void performPass() {
			if (disable)
				return;
			x++;
			glClearColor(background.get(0), background.get(1), background.get(2), alpha);
			glClear(GL_COLOR_BUFFER_BIT);
			// glClear(
			// GL_DEPTH_BUFFER_BIT |
			// GL_STENCIL_BUFFER_BIT);
		}

		public void setClearColor(Vector3 c) {
			background = c;
		}

		public void setClearColor(Vector3 c, float alpha) {
			background = c;
			this.alpha = alpha;
		}

		public void setDisable(boolean disable) {
			this.disable = disable;
		}
	}

	static public class ClearDepth extends OnePassElement implements iSceneListElement {
		public ClearDepth() {
			super(StandardPass.preRender);
		}

		public ClearDepth(StandardPass pass) {
			super(pass);
		}

		@Override
		public void performPass() {
			// glClear(GL_COLOR_BUFFER_BIT
			// | GL_DEPTH_BUFFER_BIT |
			// GL_STENCIL_BUFFER_BIT);

			glClearDepth(1);
			glClear(GL_DEPTH_BUFFER_BIT);
		}
	}

	/**
	 * @author marc
	 * 
	 *         To change the template for this generated type comment go to
	 *         Window - Preferences - Java - Code Generation - Code and
	 *         Comments
	 */
	static public class ClearOnce extends OnePassElement implements iSceneListElement {

		Vector3 background = new Vector3(0.1, 0.1, 0.1);

		float alpha = 0;

		int tick = 0;

		public ClearOnce(Vector3 colour) {
			super(StandardPass.render);
			background = colour;
		}

		public ClearOnce(Vector3 colour, float alpha) {
			super(StandardPass.render);
			background = colour;
			this.alpha = alpha;
		}

		@Override
		public void performPass() {
			tick++;
			if (tick < 4) {
				glClearColor(background.get(0), background.get(1), background.get(2), alpha);
				glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
				// glClear(GL_DEPTH_BUFFER_BIT);
			}

		}

		public void setClearColor(Vector3 c) {
			background = c;
		}

		public void setClearColor(Vector3 c, float alpha) {
			background = c;
			this.alpha = alpha;
		}

	}

	static abstract public class ContextWrapper extends BasicSceneList implements iSceneListElement {

		protected iSceneListElement[] one;

		public ContextWrapper(iSceneListElement[] one) {
			this.one = one;
		}

		@Override
		public void notifyAddParent(iMutable<iSceneListElement> list) {
			super.notifyAddParent(list);
			for (int i = 0; i < one.length; i++)
				one[i].notifyAddParent(list);
		}

		@Override
		public void performPass(iPass p) {
			one[indexForContext(BasicContextManager.getCurrentContext())].performPass(p);
		}

		abstract protected int indexForContext(Object object);

	}

	static public class DepthMask extends TwoPassElement {

		boolean enable = false;

		public DepthMask() {
			super("", StandardPass.preRender, StandardPass.postRender);
		}

		public DepthMask(StandardPass pre, StandardPass post) {
			super("", pre, post);
		}

		@Override
		protected void post() {
			glDepthMask(true);
		}

		@Override
		protected void pre() {
			glDepthMask(false);
		}

		@Override
		protected void setup() {
			BasicContextManager.putId(this, 0);
		}
	}

	static public class DisableBlending extends OnePassElement {
		public DisableBlending() {
			super(StandardPass.preRender);

		}

		@Override
		public void performPass() {
			glDisable(GL_BLEND);
			glBlendFunc(GL_ONE, GL_ZERO);
		}
	}

	/**
	 * @author marc created on Jul 20, 2003
	 */
	static public class DisableCull extends OnePassElement {

		public DisableCull() {
			super(StandardPass.render);

		}

		@Override
		public void performPass() {
			glDisable(GL_CULL_FACE);
		}

	}

	static public class DisableDepthTest extends OnePassElement {

		boolean enable = false;

		public DisableDepthTest() {
			super(StandardPass.preRender);

			// assert false : "disable depth
			// test makes point sprites
			// disappear";
		}

		/**
		 * @param b
		 */
		public DisableDepthTest(boolean b) {
			super(StandardPass.preRender);
			enable = !b;
			// assert false : "disable depth
			// test makes point sprites
			// disappear";
		}

		@Override
		public void performPass() {

			if (thinState)
				return;

			if (!enable) {
				glEnable(GL_DEPTH_TEST);
				glDepthFunc(GL_ALWAYS);
				glDepthMask(true);
			} else {
				glDepthFunc(GL_LESS);
				glEnable(GL_DEPTH_TEST);
				glDepthMask(true);
			}
		}
	}

	static public class DisableDepthTestWrap extends TwoPassElement {

		boolean enable = false;

		public DisableDepthTestWrap() {
			super("standard", StandardPass.preRender, StandardPass.postRender);
		}

		@Override
		protected void post() {
			glDepthFunc(GL_LESS);
		}

		@Override
		protected void pre() {
			glDepthFunc(GL_ALWAYS);
		}

		@Override
		protected void setup() {
		}

	}

	static public class EnableDepthTestWrap extends TwoPassElement {

		boolean enable = false;

		public EnableDepthTestWrap() {
			super("standard", StandardPass.postRender, StandardPass.preDisplay);
		}

		@Override
		protected void post() {
			glDepthFunc(GL_ALWAYS);
		}

		@Override
		protected void pre() {
			glDepthFunc(GL_LESS);
		}

		@Override
		protected void setup() {
		}

	}

	static public class EnableBlending extends OnePassElement {
		public EnableBlending() {
			super(StandardPass.preRender);

		}

		@Override
		public void performPass() {
			glEnable(GL_BLEND);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		}
	}

	static public class AlphaTest extends TwoPassElement {
		public AlphaTest() {
			super("", StandardPass.preRender, StandardPass.postRender);

		}

		int func = GL_LESS;
		float val = 0.5f;

		@Override
		public void pre() {
			glEnable(GL_ALPHA_TEST);
			glAlphaFunc(func, val);
		}

		@Override
		public void post() {
			glDisable(GL_ALPHA_TEST);
		}

		protected void setup() {
		};
	}

	static public class EnableCull extends OnePassElement {

		private Boolean b;

		public EnableCull() {
			super(StandardPass.render);
			this.b = null;
		}

		public EnableCull(Boolean b) {
			super(StandardPass.render);
			this.b = b;
		}

		@Override
		public void performPass() {
			if (thinState)
				return;

			glCullFace((b == null ? back : b) ? GL_BACK : GL_FRONT);
			glEnable(GL_CULL_FACE);
		}

	}

	static public class FlipDepthTest extends OnePassElement {

		boolean enable = false;

		public FlipDepthTest() {
			super(StandardPass.preRender);

		}

		/**
		 * @param b
		 */
		public FlipDepthTest(boolean b) {
			super(StandardPass.preRender);
			enable = !b;
		}

		@Override
		public void performPass() {
			glDepthFunc(GL_GREATER);
		}
	}

	// /**
	// * a base class for activating a stencil command set and then
	// resetting
	// * it.
	// */
	// static public class GenericStencilControl extends TwoPassElement {
	//
	// // saved state
	// int sfunc, sref, smask, sfail, sdfail, sdpass;
	//
	// boolean senabled = false;
	//
	// int func, ref, mask, fail, dfail, dpass;
	//
	// boolean noColour;
	//
	// public GenericStencilControl(int function, int ref, int mask, int
	// op_fail, int op_dfail, int op_pass, boolean noColour) {
	// super("unnamed", StandardPass.preRender, StandardPass.postRender);
	// this.func = function;
	// this.ref = ref;
	// this.mask = mask;
	// this.fail = op_fail;
	// this.dfail = op_dfail;
	// this.dpass = op_pass;
	// this.noColour = noColour;
	// }
	//
	// @Override
	// public void post() {
	// // restore state
	// glStencilFunc(sfunc, sref, smask);
	// glStencilOp(sfail, sdfail, sdpass);
	// if (!senabled)
	// glDisable(GL_STENCIL_TEST);
	// if (noColour) {
	// glColorMask(true, true, true, true);
	// glDepthMask(true);
	// }
	// }
	//
	// @Override
	// public void pre() {
	// // save state
	// int[] i = new int[1];
	// glGetIntegerv(GL_STENCIL_FUNC, i, 0);
	// sfunc = i[0];
	// glGetIntegerv(GL_STENCIL_REF, i, 0);
	// smask = i[0];
	// glGetIntegerv(GL_STENCIL_VALUE_MASK, i, 0);
	// smask = i[0];
	// glGetIntegerv(GL_STENCIL_FAIL, i, 0);
	// sfail = i[0];
	// glGetIntegerv(GL_STENCIL_PASS_DEPTH_FAIL, i, 0);
	// sdfail = i[0];
	// glGetIntegerv(GL_STENCIL_PASS_DEPTH_PASS, i, 0);
	// sdpass = i[0];
	// glGetIntegerv(GL_STENCIL_TEST, i, 0);
	// senabled = i[0] != 1;
	//
	// // push state
	// glStencilFunc(func, ref, mask);
	// glStencilOp(fail, dfail, dpass);
	// glEnable(GL_STENCIL_TEST);
	// glEnable(GL_DEPTH_TEST);
	// if (noColour) {
	// glColorMask(false, false, false, false);
	// glDepthMask(false);
	// }
	// }
	//
	// @Override
	// public void setup() {
	// }
	// }
	//
	// @Woven
	// static public class InSubContext extends BasicSceneList {
	// private final String name;
	//
	// protected GL2 gl = null;
	//
	// protected GLU glu = null;
	//
	// Set preRender = new HashSet();
	//
	// Set postRender = new HashSet();
	//
	// Base.StandardPass prePass;
	//
	// Base.StandardPass postPass;
	//
	// Object first = new Object();
	//
	// public InSubContext(String name) {
	// this.name = name;
	// this.prePass = Base.StandardPass.preTransform;
	// this.postPass = Base.StandardPass.preDisplay;
	// }
	//
	// @Override
	// public void notifyAddParent(iMutable<iSceneListElement> newParent) {
	// super.notifyAddParent(newParent);
	// preRender.add(preRender.add(((iSceneListElement)
	// newParent).requestPass(prePass)));
	// postRender.add(postRender.add(((iSceneListElement)
	// newParent).requestPass(postPass)));
	// }
	//
	// // no annotation
	// @Override
	// public void performPass(iPass p) {
	// gl = BasicContextManager.getGl();
	// glu = BasicContextManager.getGlu();
	//
	// assert (glGetError() == 0);
	// if ((p == null) || (preRender.contains(p))) {
	// if (!BasicContextManager.isValid(first)) {
	// BasicContextManager.markAsValidInThisContext(first);
	// assert (glGetError() == 0);
	// setup();
	// assert (glGetError() == 0);
	// }
	// assert (glGetError() == 0);
	// pre();
	// assert (glGetError() == 0) : this.getClass();
	// } else if ((p == null) || (postRender.contains(p))) {
	// assert (glGetError() == 0);
	// post();
	// assert (glGetError() == 0);
	// }
	// super.performPass(p);
	// }
	//
	// protected void post() {
	// Base.context.end(name);
	// }
	//
	// protected void pre() {
	// Base.context.begin(name);
	// }
	//
	// protected void setup() {
	// BasicContextManager.putId(this, 0);
	// }
	// }

	static public class LogicOpWrap extends TwoPassElement {

		private final int op;
		boolean enable = false;

		public LogicOpWrap(int op) {
			super("standard", StandardPass.preTransform, StandardPass.preDisplay);
			this.op = op;
		}

		@Override
		protected void post() {
			// glDisable(GL_LOGIC_OP);
			// System.err.println(" logic op
			// off ");
		}

		@Override
		protected void pre() {
			// System.err.println(" logic op
			// on ");
			// glEnable(GL_LOGIC_OP);
			// glLogicOp(op);
		}

		@Override
		protected void setup() {
		}

	}

	static public class Masquerade extends OnePassListElement {

		private final OnePassListElement copyFrom;

		private final OnePassListElement execute;

		public Masquerade(OnePassListElement copyFrom, OnePassListElement execute) {
			super(execute.requestPass, copyFrom.ourPass);
			this.copyFrom = copyFrom;
			this.execute = execute;
		}

		@Override
		public void performPass() {
			this.pre();
			copyFrom.pre();
			execute.performPass();
			copyFrom.post();
			this.post();
		}
	}

	static public class NoMeshSmoothing extends TwoPassElement {
		private boolean b;

		public NoMeshSmoothing() {
			super("", StandardPass.preRender, StandardPass.postRender);
		}

		@Override
		protected void post() {
			if (b)
				glEnable(GL_POLYGON_SMOOTH);
		}

		@Override
		protected void pre() {
			b = glIsEnabled(GL_POLYGON_SMOOTH);
			glDisable(GL_POLYGON_SMOOTH);
		}

		@Override
		protected void setup() {
		}
	}

	// abstract classes that are very useful
	@Woven
	@HiddenInAutocomplete
	static public abstract class OnePassElement extends BasicSceneList implements iSceneListElement {

		static public final Method method_performPass = ReflectionTools.methodOf("performPass", OnePassElement.class);

		protected Set renderPass = new HashSet();

		protected Base.iPass requestPass;

		protected Object gl = null;

		protected Object glu = null;

		public OnePassElement(Base.iPass requestPass) {
			this.requestPass = requestPass;
		}

		@Override
		public void notifyAddParent(iMutable<iSceneListElement> newParent) {
			super.notifyAddParent(newParent);
			renderPass.add(((iSceneListElement) newParent).requestPass(requestPass));
		}

		// this is where you do the work of the element
		@DispatchOverTopology(topology = Cont.class)
		abstract public void performPass();

		// performs pass if renderPass or null pass
		@Override
		@DispatchOverTopology(topology = Cont.class)
		@ConstantContext(immediate = false, topology = Base.class)
		public void performPass(iPass p) {
			gl = BasicContextManager.getGl();
			glu = BasicContextManager.getGlu();
			if ((p == null) || (renderPass.contains(p))) {
				uniform.push();
				performPass();
			}
		}

		ContextualUniform.TagGroup uniform = new ContextualUniform.TagGroup();

		public void setTag(String key, String value) {
			uniform.put(key, value);
		}

	}

	@Woven
	abstract static public class OnePassListElement extends BasicSceneList implements iSceneListElement {

		static public final Method method_performPass = ReflectionTools.methodOf("performPass", OnePassListElement.class);

		public Object gl = null;

		public Object glu = null;

		private final StandardPass ourPass;

		protected Base.StandardPass requestPass;

		protected Set renderPass = new HashSet();

		protected iPass ourRenderPass;

		protected boolean preCalled = false;

		protected boolean postCalled = false;

		boolean skipIfEmpty = false;

		iFunction<Boolean, OnePassListElement> guard;

		public OnePassListElement(Base.StandardPass parentPass, Base.StandardPass ourPass) {
			this.ourPass = ourPass;
			this.ourRenderPass = this.requestPass(ourPass);
			this.requestPass = parentPass;
		}

		@Override
		public void notifyAddParent(iMutable<iSceneListElement> newParent) {
			super.notifyAddParent(newParent);
			renderPass.add(((iSceneListElement) newParent).requestPass(requestPass));
		}

		@DispatchOverTopology(topology = Cont.class)
		@ConstantContext(immediate = false, topology = Base.class)
		abstract public void performPass();

		boolean visible = true;

		@Override
		public void performPass(iPass p) {
			gl = BasicContextManager.getGl();
			glu = BasicContextManager.getGlu();

			if ((p == null) || (renderPass.contains(p))) {
				if (skipIfEmpty && this.getChildren().size() == 0)
					return;

				if (guard != null && !guard.f(this)) {

					visible = false;
					return;
				}
				visible = true;

				preCalled = false;
				postCalled = false;
				assert (glGetError() == 0) : this.getClass();
				performPass();
				assert (glGetError() == 0) : this.getClass();
				assert preCalled : this.getClass();
				assert postCalled : this.getClass();
			}
			// System.err.println("pp "+p+"
			// "+System.identityHashCode(this)+"
			// "+this.getClass()+" out ");
		}

		public void setSkipIfEmpty() {
			skipIfEmpty = true;
		}

		protected void post() {
			postCalled = true;
			updateFromButNotIncluding(ourRenderPass);
		}

		/**
		 * subclasses must call these, typically on entry and exit to
		 * performPass ()
		 */
		protected void pre() {
			preCalled = true;
			updateUpToAndIncluding(ourRenderPass);

			uniform.push();
		}

		public boolean isVisible() {
			return visible;
		}

		ContextualUniform.TagGroup uniform = new ContextualUniform.TagGroup();

		public void setTag(String key, String value) {
			uniform.put(key, value);
		}

		public void setGaurd(iFunction<Boolean, OnePassListElement> guard) {
			this.guard = guard;
		}
	}

	static public class PolygonOffset extends TwoPassElement {

		float factor;

		float units;

		boolean doLine = false;

		public PolygonOffset(float factor, float units) {
			super("polygonoffset", StandardPass.preRender, StandardPass.postRender);
			this.factor = factor;
			this.units = units;
		}

		/**
		 * @param i
		 * @param j
		 * @param b
		 */
		public PolygonOffset(float factor, float units, boolean b) {
			super("polygonoffset", StandardPass.preRender, StandardPass.postRender);
			this.factor = factor;
			this.units = units;
			doLine = b;
		}

		@Override
		protected void post() {
			if (thinState)
				return;
			glDisable(GL_POLYGON_OFFSET_FILL);
			if (doLine)
				glDisable(GL_POLYGON_OFFSET_LINE);
		}

		@Override
		protected void pre() {
			if (thinState)
				return;
			glPolygonOffset(factor, units);
			glEnable(GL_POLYGON_OFFSET_FILL);
			if (doLine)
				glEnable(GL_POLYGON_OFFSET_LINE);
		}

		@Override
		protected void setup() {
		}

	}

	static public class Position extends CoordinateFrame {

		public Position() {
		}

		public Position(Vector3 v) {
			super.setTranslation(v);
		}

		public Position(Vector3 v, Quaternion q) {
			super.setTranslation(v);
			super.setRotation(q);
		}

	}

	static public class SepEnableBlending extends OnePassElement {
		private final iFloatProvider alpha;

		public SepEnableBlending(iFloatProvider alpha) {
			super(StandardPass.preRender);
			this.alpha = alpha;
		}

		@Override
		public void performPass() {
			if (thinState)
				return;

			glEnable(GL_BLEND);
			glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_CONSTANT_ALPHA, GL_ONE);
			glBlendColor(0, 0, 0, alpha.evaluate());
		}
	}

	static public class SetBlendEquation extends OnePassElement {

		private final int mode;

		public SetBlendEquation(StandardPass requestPass, int mode) {
			super(requestPass);
			this.mode = mode;
		}

		@Override
		public void performPass() {
			if (thinState)
				return;

			glBlendEquation(mode);
			glEnable(GL_BLEND);
		}
	}

	static public class SetBlendMode extends OnePassElement {

		private final int src;

		private final int dest;

		private Vector4 constant;

		static public Vector4 constantMul = new Vector4(1, 1, 1, 1);

		public SetBlendMode(StandardPass requestPass, int src, int dest) {
			super(requestPass);
			this.src = src;
			this.dest = dest;
		}

		public SetBlendMode(StandardPass preRender, int gl_constant_alpha, int gl_one_minus_constant_alpha, Vector4 vector4) {
			this(preRender, gl_constant_alpha, gl_one_minus_constant_alpha);
			this.constant = vector4;
		}

		@Override
		public void performPass() {
			if (thinState)
				return;

			if (constant != null)
				glBlendColor(constant.x, constant.y, constant.z, constant.w);

			glBlendFunc(src, dest);
			glBlendEquation(GL_FUNC_ADD);
			glEnable(GL_BLEND);
		}

	}

	static public class SetBlendAdd extends SetBlendMode {

		public SetBlendAdd() {
			super(Base.StandardPass.preRender, GL_SRC_ALPHA, GL_ONE);
		}
	}

	static public class SetBlendStraightAdd extends SetBlendMode {

		public SetBlendStraightAdd() {
			super(Base.StandardPass.preRender, GL_ONE, GL_ONE);
		}
	}

	static public class SetBlendColor extends OnePassElement {

		private Vector4 constant;

		public SetBlendColor(StandardPass preRender, Vector4 vector4) {
			super(preRender);
			this.constant = vector4;
		}

		@Override
		public void performPass() {
			if (thinState)
				return;

			if (constant != null)
				glBlendColor(constant.x, constant.y, constant.z, constant.w);
		}

	}

	static public class SetColorMask extends TwoPassElement {
		private boolean b;

		private final boolean red;

		private final boolean green;

		private final boolean blue;

		private final boolean alpha;

		public SetColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
			super("", StandardPass.preTransform, StandardPass.postRender);
			this.red = red;
			this.green = green;
			this.blue = blue;
			this.alpha = alpha;
		}

		boolean first = true;

		@Override
		protected void post() {

			if (thinState)
				return;

			if (first) {
				first = false;
				glColorMask(true, true, true, true);
			}
		}

		@Override
		protected void pre() {
			glColorMask(red, green, blue, alpha);
		}

		@Override
		protected void setup() {
		}
	}

	static public class SetColorMaskWrap extends TwoPassElement {
		private boolean b;

		private final boolean red;

		private final boolean green;

		private final boolean blue;

		private final boolean alpha;

		public SetColorMaskWrap(boolean red, boolean green, boolean blue, boolean alpha) {
			super("", StandardPass.preTransform, StandardPass.postRender);
			this.red = red;
			this.green = green;
			this.blue = blue;
			this.alpha = alpha;
		}

		boolean first = true;

		@Override
		protected void post() {

			if (thinState)
				return;

			glColorMask(true, true, true, true);
		}

		@Override
		protected void pre() {
			glColorMask(red, green, blue, alpha);
		}

		@Override
		protected void setup() {
		}
	}

	// static public class SetMatrix extends BasicUtilities.TwoPassElement
	// implements iSceneListElement {
	//
	// iInplaceProvider<iCoordinateFrame> provider;
	//
	// CoordinateFrame coordinateFrameNow = new CoordinateFrame();
	//
	// float matrix[] = null;
	//
	// public SetMatrix(float[] matrix) {
	// super("setmatrix", StandardPass.preRender, StandardPass.postRender);
	// setMatrix(matrix);
	// }
	//
	// public SetMatrix(iInplaceProvider<iCoordinateFrame> provider) {
	// super("set matrix", StandardPass.preRender, StandardPass.postRender);
	// this.provider = provider;
	// }
	//
	// @Override
	// public void post() {
	// glPopMatrix();
	// }
	//
	// /**
	// * main entry point, do your work for Pass 'p' here
	// */
	// @Override
	// public void pre() {
	// if (thinState)
	// return;
	//
	// if (provider != null) {
	// provider.get(coordinateFrameNow);
	//
	// // possible
	// // ordering
	// // problem!
	// matrix = coordinateFrameNow.getMatrix(null).get(matrix);
	// }
	// glPushMatrix();
	// glMultMatrixf(matrix, 0);
	// // glMatrixMode(GL_MATRIX0_ARB);
	// }
	//
	// public void setMatrix(float[] matrix) {
	// if (this.matrix == null)
	// this.matrix = new float[16];
	// for (int i = 0; i < 16; i++)
	// this.matrix[i] = matrix[i];
	// }
	//
	// @Override
	// public void setup() {
	// }
	// }

	// static public class Shadow extends GenericStencilControl {
	//
	// public Shadow() {
	// super(GL_NOTEQUAL, 0, ~0, GL_KEEP, GL_KEEP, GL_KEEP, false);
	// }
	//
	// @Override
	// public void post() {
	// super.post();
	// glDepthFunc(GL_LESS);
	// }
	//
	// @Override
	// public void pre() {
	// super.pre();
	// glDepthFunc(GL_ALWAYS);
	// }
	// }
	//
	// static public class ShadowBack extends GenericStencilControl {
	//
	// public ShadowBack() {
	// super(GL_ALWAYS, 0, ~0, GL_KEEP, GL_KEEP, GL_INVERT, true);
	// }
	//
	// @Override
	// public void post() {
	// super.post();
	// glCullFace(GL_BACK);
	// }
	//
	// @Override
	// public void pre() {
	// super.pre();
	// glCullFace(GL_FRONT);
	// }
	// }
	//
	// static public class ShadowFront extends GenericStencilControl {
	//
	// public ShadowFront() {
	// super(GL_ALWAYS, 0, ~0, GL_KEEP, GL_KEEP, GL_INVERT, true);
	// }
	//
	// @Override
	// public void post() {
	// super.post();
	// glCullFace(GL_BACK);
	// }
	//
	// @Override
	// public void pre() {
	// super.pre();
	// glCullFace(GL_BACK);
	// }
	// }

	static public class Smooth extends OnePassElement {

		public Smooth() {
			super(StandardPass.preRender);

		}

		@Override
		public void performPass() {

			if (thinState)
				return;

			glEnable(GL_BLEND);
			// glEnable(GL_LINE_SMOOTH);
			// glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
			glEnable(GL_POLYGON_SMOOTH);
			glHint(GL_POLYGON_SMOOTH_HINT, GL_NICEST);

			// glEnable(GL_POINT_SMOOTH);
			// glHint(GL_POINT_SMOOTH_HINT, GL_NICEST);
		}
	}

	static public class LineSmooth extends OnePassElement {

		public LineSmooth() {
			super(StandardPass.preRender);

		}

		@Override
		public void performPass() {

			if (thinState)
				return;

			glEnable(GL_BLEND);

			glEnable(GL_LINE_SMOOTH);
			glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);

			// glEnable(GL_POLYGON_SMOOTH);
			// glHint(GL_POLYGON_SMOOTH_HINT, GL_NICEST);

			glEnable(GL_POINT_SMOOTH);
			glHint(GL_POINT_SMOOTH_HINT, GL_NICEST);
		}
	}

	static public class NoLineSmooth extends OnePassElement {

		public NoLineSmooth() {
			super(StandardPass.preRender);

		}

		@Override
		public void performPass() {

			if (thinState)
				return;

			glEnable(GL_BLEND);

			glDisable(GL_LINE_SMOOTH);
			glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);

			// glEnable(GL_POLYGON_SMOOTH);
			// glHint(GL_POLYGON_SMOOTH_HINT, GL_NICEST);

			if (!CoreHelpers.isCore) {
				glDisable(GL_POINT_SMOOTH);
				glHint(GL_POINT_SMOOTH_HINT, GL_NICEST);
			}
		}
	}

	static public class LineWidth extends OnePassElement {

		public float width = 1;

		public LineWidth() {
			super(StandardPass.preRender);
		}

		@Override
		public void performPass() {

			glLineWidth(width);
		}
	}

	static public class LineStipple extends TwoPassElement {

		public LineStipple() {
			super("", StandardPass.preRender, StandardPass.postRender);
		}

		int a = 1;
		short mask = (short) 0xffff;

		@Override
		public void pre() {
			if (thinState)
				return;

			glEnable(GL_LINE_STIPPLE);
			glLineStipple(a, mask);

		}

		@Override
		protected void post() {
			glDisable(GL_LINE_STIPPLE);
		}

		@Override
		protected void setup() {
			// TODO Auto-generated method stub

		}

	}

	/**
	 * sets up culling, blending and depthing just like we'd expect the
	 * defaults to be for general 3d graphics
	 */
	static public class Standard extends OnePassElement {

		boolean cull = false;

		public Standard() {
			this(false);
		}

		public Standard(boolean cull) {
			super(StandardPass.render);
			this.cull = cull;
		}

		@Override
		public void performPass() {
			if (thinState)
				return;

			if (cull) {
				glEnable(GL_CULL_FACE);
				glCullFace(GL_BACK);
			} else {
				glDisable(GL_CULL_FACE);
			}
			// glEnable(GL_MULTISAMPLE_ARB);
			glEnable(GL_BLEND);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			glEnable(GL_DEPTH_TEST);
			// glEnable(GL_POLYGON_SMOOTH);
			// glHint(GL_POLYGON_SMOOTH_HINT,
			// GL_NICEST);
		}
	}

	/**
	 * call update to swap
	 */

	static public class ToggleWrapper extends BasicSceneList implements iSceneListElement, iUpdateable {

		protected iSceneListElement one;

		protected iSceneListElement two;

		boolean first = true;

		public ToggleWrapper(iSceneListElement one, iSceneListElement two) {
			this.one = one;
			this.two = two;
		}

		@Override
		public void notifyAddParent(iMutable<iSceneListElement> list) {
			super.notifyAddParent(list);
			one.notifyAddParent(list);
			two.notifyAddParent(list);
		}

		@Override
		public void performPass(iPass p) {
			if (first)
				one.performPass(p);
			else
				two.performPass(p);
		}

		@Override
		public void update() {
			first = !first;
		}

	}

	@Woven
	abstract static public class TwoPassElement extends BasicSceneList implements iSceneListElement {

		public Object gl = null;

		public Object glu = null;

		Set preRender = new HashSet();

		Set postRender = new HashSet();

		Base.StandardPass prePass;

		Base.StandardPass postPass;

		Object first = new Object();

		public TwoPassElement(String name, Base.StandardPass prePass, Base.StandardPass postPass) {
			this.prePass = prePass;
			this.postPass = postPass;
		}

		@Override
		public void notifyAddParent(iMutable<iSceneListElement> newParent) {
			super.notifyAddParent(newParent);

			preRender.add(((iSceneListElement) newParent).requestPass(prePass));
			postRender.add(((iSceneListElement) newParent).requestPass(postPass));
		}

		// a two pass calls pre() on preRender and
		// post() on postRender
		@Override
		@DispatchOverTopology(topology = Cont.class)
		@ConstantContext(immediate = false, topology = Base.class)
		public void performPass(iPass p) {
			gl = BasicContextManager.getGl();
			glu = BasicContextManager.getGlu();

			// System.err.println("Tp "+p+"
			// "+System.identityHashCode(this)+"
			// "+this.getClass()+" in
			// pre<"+preRender+">
			// post<"+postRender+">");

			assert (glGetError() == 0);
			if ((p == null) || (preRender.contains(p))) {
				if (!BasicContextManager.isValid(first)) {
					BasicContextManager.markAsValidInThisContext(first);
					assert (glGetError() == 0);
					setup();
					assert (glGetError() == 0);
				}
				assert (glGetError() == 0);

				// System.err.println("
				// update to and
				// including
				// <"+p+">"+this);
				super.updateUpToAndIncluding(p);
				// System.err.println("
				// x1"+this);

				pre();
				uniform.push();
				// System.err.println("
				// y1"+this);
				assert (glGetError() == 0) : this.getClass();
			} else if ((p == null) || (postRender.contains(p))) {
				assert (glGetError() == 0);

				// System.err.println("
				// update from
				// and not
				// including
				// <"+(iPass)preRender.iterator().next()+"
				// -> "+p+">");
				super.updateFromButNotIncluding((iPass) preRender.iterator().next(), p);

				post();
				uniform.pop();

				// System.err.println("
				// update from
				// including
				// "+p+">");
				super.updateFromButNotIncluding(p);
				assert (glGetError() == 0);
			}
			// assert (glGetError() ==
			// 0);
			// super.performPass(p);
			// assert (glGetError() ==
			// 0);
			// System.err.println("Tp "+p+"
			// "+System.identityHashCode(this)+"
			// "+this.getClass()+" out
			// pre<"+preRender+">
			// post<"+postRender+">");

		}

		@ConstantContext(immediate = false, topology = Base.class)
		abstract protected void post();

		@ConstantContext(immediate = false, topology = Base.class)
		abstract protected void pre();

		@ConstantContext(immediate = false, topology = Base.class)
		abstract protected void setup();

		ContextualUniform.TagGroup uniform = new ContextualUniform.TagGroup();

		public void setTag(String key, String value) {
			uniform.put(key, value);
		}

	}

	static public class WireFrame extends BasicUtilities.TwoPassElement implements iSceneListElement {

		boolean b = true;

		int width = -1;

		public WireFrame() {
			super("wireframe", StandardPass.preRender, StandardPass.postRender);
		}

		public WireFrame(boolean b) {
			super("wireframe", StandardPass.preRender, StandardPass.postRender);
			this.b = b;
		}

		public WireFrame(int i) {
			super("wireframe", StandardPass.preRender, StandardPass.postRender);
			width = i;
		}

		@Override
		protected void post() {
			// if (b)
			glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
			// else
			// glPolygonMode(GL_FRONT_AND_BACK,
			// GL_LINE);
		}

		@Override
		protected void pre() {

			if (b) {
				glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
				// glPointSize(1f);
			} else
				glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
			if (width != -1) {
				glLineWidth(width);
			}
			// glEnable(GL_LINE_SMOOTH);
			// glHint(GL_LINE_SMOOTH_HINT,
			// GL_NICEST);
			// glLineWidth(0.5f);

		}

		@Override
		protected void setup() {
		}
	}

	static public boolean back = false;
}