package field.core.plugins.drawing.opengl;

import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Event;
import org.python.core.PyObject;

import field.bytecode.protect.annotations.HiddenInAutocomplete;
import field.bytecode.protect.dispatch.Cont;
import field.bytecode.protect.dispatch.Cont.ReturnCode;
import field.bytecode.protect.dispatch.Cont.aRun;
import field.core.dispatch.Mixins;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.Mixins.iMixinProxy;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElementOverrides;
import field.core.plugins.drawing.ThreedComputingOverride;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.components.RootComponent;
import field.core.windowing.components.RootComponent.iMousePeer;
import field.graph.FLineInterpolator;
import field.graphics.ci.CoreImageCanvasUtils;
import field.graphics.ci.DeferredImageDrawing;
import field.graphics.ci.SimpleImageDrawing;
import field.graphics.core.Base.StandardPass;
import field.graphics.core.Base.iAcceptsSceneListElement;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.dynamic.DynamicLine_long;
import field.graphics.dynamic.DynamicMesh_long;
import field.graphics.dynamic.DynamicPointlist;
import field.graphics.windowing.FullScreenCanvasSWT;
import field.graphics.windowing.MouseEventDelegate;
import field.launch.iUpdateable;
import field.math.abstraction.iProvider;
import field.util.HashMapOfLists;
import field.util.TaskQueue;

public class OnCanvasLines {

	private BaseGLGraphicsContext context;
	/**
	 * This is the list of FLine-s to draw each animation frame. Add things
	 * to this line to cause them to appear.
	 */
	public List<Object> submit = new ArrayList<Object>();

	@HiddenInAutocomplete
	public CoreImageCanvasUtils coreGraphicsContext;

	@HiddenInAutocomplete
	public HashMapOfLists<String, Object> layers = new HashMapOfLists<String, Object>();
	@HiddenInAutocomplete
	public Map<String, CachedLineLayer> fastMeshLayer = new LinkedHashMap<String, CachedLineLayer>();
	@HiddenInAutocomplete
	public Map<String, CachedLineLayer> fastLineLayer = new LinkedHashMap<String, CachedLineLayer>();
	@HiddenInAutocomplete
	public Map<String, CachedLineLayer> fastPointLayer = new LinkedHashMap<String, CachedLineLayer>();
	@HiddenInAutocomplete
	public Map<String, DirectLayer> directLayer = new LinkedHashMap<String, DirectLayer>();
	@HiddenInAutocomplete
	private final iAcceptsSceneListElement on;

	public OnCanvasLines(iVisualElement element) {
		this(iVisualElement.enclosingFrame.get(element).getSceneList(), element);
	}

	TaskQueue animationQueue = new TaskQueue();

	public OnCanvasLines(iAcceptsSceneListElement on, final iVisualElement element) {

		iVisualElementOverrides o = iVisualElement.overrides.get(element);
		final ThreedComputingOverride canvas;
		if (o instanceof ThreedComputingOverride) {
			canvas = ((ThreedComputingOverride) o);
		} else {
			// canvas = new
			// Mixins().mixInOverride(ThreedComputingOverride.class,
			// element);
			canvas = null;
		}

		this.on = on;
		context = new BaseGLGraphicsContext(on, false);

		// context.getGlobalProperties().put(iLinearGraphicsContext.geometricScale,
		// 50f);
		// context.getGlobalProperties().put(iLinearGraphicsContext.flatnessScale,
		// 50f);

		new SimpleLineDrawing().installInto(context).doFakeAa = true;
		new SimpleImageDrawing().installInto(context);// .setUseRawDrawing(true);
		System.out.println(" installing oncanvaspline system into <" + on + ">");
		new SimpleTextDrawing(false).setForceScale(0.1f).installInto(context);
		coreGraphicsContext = new CoreImageCanvasUtils();
		new DeferredImageDrawing().installInto(context, coreGraphicsContext);

		final LineInteraction3d interaction;
		if (canvas != null) {
			interaction = new LineInteraction3d(canvas.getDefaultContext().getCamera()) {
				@Override
				protected GeneralPath filter(GeneralPath gp) {
					// Rect f =
					// canvas.forElement.getFrame(null);
					// gp.transform(AffineTransform.getTranslateInstance(f.x,
					// f.y));
					return gp;
				}
			};
			context.setLineInteraction(interaction);
		} else
			interaction = null;

		GLComponentWindow window = iVisualElement.enclosingFrame.get(element);

		if (on instanceof BasicGLSLangProgram) {

			((BasicGLSLangProgram) on).getWhen().getMap(StandardPass.render).register("ocpl" + System.identityHashCode(this), new iUpdateable() {

				@Override
				public void update() {
					context.windowDisplayEnter();

					animationQueue.update();

					for (Object o : submit) {
						resolve(o);
					}

					for (Object o : layers.values()) {
						resolve(o);
					}

					boolean work = false;

					for (DirectLayer o : directLayer.values()) {
						o.distribute();
						resolve(o.fallback);
						work |= o.didWork;
					}
					context.windowDisplayExit();

					if (work)
						iVisualElement.dirty.set(element, element, true);

				}
			});

		} else {
			aRun arun = new Cont.aRun() {

				@Override
				public ReturnCode head(Object calledOn, Object[] args) {
					context.windowDisplayEnter();

					animationQueue.update();

					for (Object o : submit) {
						resolve(o);
					}

					for (Object o : layers.values()) {
						resolve(o);
					}
					boolean work = false;

					for (DirectLayer o : directLayer.values()) {
						o.distribute();
						resolve(o.fallback);
						work |= o.didWork;
					}
					if (work)
						iVisualElement.dirty.set(element, element, true);

					return super.head(calledOn, args);
				}

				@Override
				public ReturnCode tail(Object calledOn, Object[] args, Object returnWas) {
					context.windowDisplayExit();
					return super.tail(calledOn, args, returnWas);
				}
			};
			Cont.linkWith(window, window.method_doRender, arun);
		}

		RootComponent rootComponent = iVisualElement.rootComponent.get(element);

		if (interaction != null) {
			iMousePeer mousePeer = new iMousePeer() {

				@Override
				public void mouseReleased(ComponentContainer inside, Event arg0) {
					Rect f = canvas.forElement.getFrame(null);
					arg0.x -= f.x;
					arg0.y -= f.y;
					try {
						interaction.mouseReleased(null, arg0);
					} finally {
						arg0.x += f.x;
						arg0.y += f.y;
					}
				}

				@Override
				public void mousePressed(ComponentContainer inside, Event arg0) {
					Rect f = canvas.forElement.getFrame(null);
					arg0.x -= f.x;
					arg0.y -= f.y;
					try {
						interaction.mousePressed(null, arg0);
					} finally {
						arg0.x += f.x;
						arg0.y += f.y;
					}
				}

				@Override
				public void mouseMoved(ComponentContainer inside, Event arg0) {
					Rect f = canvas.forElement.getFrame(null);
					arg0.x -= f.x;
					arg0.y -= f.y;
					try {
						interaction.mouseMoved(null, arg0);
					} finally {
						arg0.x += f.x;
						arg0.y += f.y;
					}
				}

				@Override
				public void mouseExited(ComponentContainer inside, Event arg0) {
				}

				@Override
				public void mouseEntered(ComponentContainer inside, Event arg0) {
				}

				@Override
				public void mouseDragged(ComponentContainer inside, Event arg0) {
					Rect f = canvas.forElement.getFrame(null);
					arg0.x -= f.x;
					arg0.y -= f.y;
					try {
						interaction.mouseDragged(null, arg0);
					} finally {
						arg0.x += f.x;
						arg0.y += f.y;
					}
				}

				@Override
				public void mouseClicked(ComponentContainer inside, Event arg0) {
				}

				@Override
				public void keyTyped(ComponentContainer inside, Event arg0) {
				}

				@Override
				public void keyReleased(ComponentContainer inside, Event arg0) {
				}

				@Override
				public void keyPressed(ComponentContainer inside, Event arg0) {
				}
			};

			rootComponent.addMousePeer(mousePeer, false);
		}
	}

	public OnCanvasLines(iAcceptsSceneListElement on, FullScreenCanvasSWT canvas) {
		this.on = on;
		context = new BaseGLGraphicsContext(on, false);

		// context.getGlobalProperties().put(iLinearGraphicsContext.geometricScale,
		// 50f);
		// context.getGlobalProperties().put(iLinearGraphicsContext.flatnessScale,
		// 50f);

		new SimpleLineDrawing().installInto(context).doFakeAa = true;
		new SimpleImageDrawing().installInto(context);// .setUseRawDrawing(true);
		System.out.println(" installing oncanvaspline system into <" + on + ">");
		new SimpleTextDrawing(false).setForceScale(0.1f).installInto(context);
		coreGraphicsContext = new CoreImageCanvasUtils();
		new DeferredImageDrawing().installInto(context, coreGraphicsContext);

		final LineInteraction3d interaction = new LineInteraction3d(canvas.getCamera());
		context.setLineInteraction(interaction);

		aRun arun = new Cont.aRun() {

			@Override
			public ReturnCode head(Object calledOn, Object[] args) {
				context.windowDisplayEnter();
				animationQueue.update();

				for (Object o : submit) {
					resolve(o);
				}

				for (Object o : layers.values()) {
					resolve(o);
				}

				for (DirectLayer o : directLayer.values()) {
					o.distribute();
					resolve(o.fallback);
				}

				return super.head(calledOn, args);
			}

			@Override
			public ReturnCode tail(Object calledOn, Object[] args, Object returnWas) {
				context.windowDisplayExit();
				return super.tail(calledOn, args, returnWas);
			}
		};
		Cont.linkWith(canvas, canvas.method_beforeFlush, arun);

		canvas.registerMouseEventDelegate(new MouseEventDelegate() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				Event ee = toEvent(e);
				interaction.mouseScrolled(null, ee);

			}

			boolean down = false;

			@Override
			public void mouseMove(MouseEvent e) {

				System.out.println(" moved :" + e);

				Event ee = toEvent(e);
				if (down)
					interaction.mouseDragged(null, ee);
				else
					interaction.mouseMoved(null, ee);
			}

			protected Event toEvent(MouseEvent e) {
				Event ee = new Event();
				ee.x = e.x;
				ee.y = e.y;
				ee.button = e.button;
				ee.stateMask = e.stateMask;
				ee.count = e.count;
				ee.data = e.data;
				ee.doit = true;

				return ee;
			}

			@Override
			public void mouseUp(MouseEvent e) {
				down = false;
				Event ee = toEvent(e);
				interaction.mouseReleased(null, ee);
			}

			@Override
			public void mouseDown(MouseEvent e) {
				Event ee = toEvent(e);
				interaction.mousePressed(null, ee);
				down = true;
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
			}

		});
	}

	private void resolve(Object o) {
		if (o instanceof iProvider)
			o = ((iProvider) o).get();

		if (o instanceof PyObject)
			o = filter((PyObject) o);

		if (o instanceof Collection) {
			for (Object oo : ((Collection) o)) {
				resolve(oo);
			}
		} else if (o instanceof CachedLine) {
			context.submitLine(((CachedLine) o), ((CachedLine) o).getProperties());
			return;
		}
	}

	protected Object filter(PyObject o) {
		if (o instanceof PyObject) {
			PyObject ll = ((PyObject) o).__getattr__("line");
			if (ll != null) {
				Object cl = ll.__tojava__(CachedLine.class);
				return cl;
			} else {
				System.err.println(" bad entry into line list <" + o + ">");
				return null;
			}
		} else
			return o;
	}

	@HiddenInAutocomplete
	public CoreImageCanvasUtils getCoreGraphicsContext() {
		return coreGraphicsContext;
	}

	public List<Object> getLayer(String name) {
		List<Object> c = (List<Object>) layers.getAndMakeCollection(name);
		return c;
	}

	DirectMesh dm = new DirectMesh();
	DirectPoint dp = new DirectPoint();
	DirectLine dl = new DirectLine();

	public CachedLineLayer getDirectLineLayer(String name) {
		CachedLineLayer l = fastLineLayer.get(name);
		if (l == null) {
			l = new CachedLineLayer();
			DynamicLine_long boundTo = DynamicLine_long.unshadedLine(null, 1);
			boundTo.getUnderlyingGeometry().doFakeAntialias(true);
			on.addChild(boundTo.getUnderlyingGeometry());
			boundTo.getUnderlyingGeometry().addChild(l.new UpdateLine(boundTo, dl));
			fastLineLayer.put(name, l);

			l.geometry = boundTo;

		}
		return l;
	}

	public CachedLineLayer getDirectMeshLayer(String name) {
		CachedLineLayer l = fastMeshLayer.get(name);
		if (l == null) {
			l = new CachedLineLayer();
			DynamicMesh_long boundTo = DynamicMesh_long.unshadedMesh(StandardPass.render);
			on.addChild(boundTo.getUnderlyingGeometry());
			boundTo.getUnderlyingGeometry().addChild(l.new UpdateMesh(boundTo, dm));
			fastMeshLayer.put(name, l);

			l.geometry = boundTo;

		}
		return l;
	}

	public CachedLineLayer getDirectPointLayer(String name) {
		CachedLineLayer l = fastPointLayer.get(name);
		if (l == null) {
			l = new CachedLineLayer();
			DynamicPointlist boundTo = DynamicPointlist.unshadedPoints(null);
			on.addChild(boundTo.getUnderlyingGeometry());
			boundTo.getUnderlyingGeometry().addChild(l.new UpdatePoint(boundTo, dp));
			fastPointLayer.put(name, l);

			l.geometry_points = boundTo;

		}
		return l;
	}

	public class DirectLayer {
		private CachedLineLayer line;
		private CachedLineLayer mesh;
		private CachedLineLayer point;
		private List<CachedLine> fallback;

		public ArrayList_Mod<CachedLine> submit;

		public String hash = "n";
		public int duration = 100;
		public ArrayList_Mod<CachedLine> animate = new ArrayList_Mod<CachedLine>();
		FLineInterpolator interpolator = new FLineInterpolator();

		int oldMod = -1;
		int oldAnimMod = -1;

		int fixedCurveSampling = 10;

		boolean first = true;
		boolean didWork = false;

		protected void distribute() {

			DirectMesh.fixedCurveSampling = fixedCurveSampling;
			DirectLine.fixedCurveSampling = fixedCurveSampling;
			DirectPoint.fixedCurveSampling = fixedCurveSampling;

			if (animate.getMod() != oldAnimMod) {
				interpolator.setTarget(animate, hash, duration);
				animate.clear();
				oldAnimMod = animate.getMod();
			}
			List<CachedLine> also = interpolator.update();

			System.out.println(" embedded interpolator <" + interpolator.isDoingWork() + ">");
			didWork = false;

			if (submit.getMod() == oldMod && !interpolator.isDoingWork())
				return;

			line.line.clear();
			mesh.line.clear();
			point.line.clear();
			fallback.clear();

			for (CachedLine c : submit) {
				dispatch(c);
			}

			if (also != null) {
				for (CachedLine c : also) {
					dispatch(c);
				}
			}
			oldMod = submit.getMod();
			didWork = true;

			first = false;

		}

		private void dispatch(CachedLine c) {
			if (c.getProperties().isTrue(iLinearGraphicsContext.containsText, false)) {
				fallback.add(c);
			} else {
				if (c.getProperties().isTrue(iLinearGraphicsContext.stroked, true)) {
					line.line.add(c);
				}
				if (c.getProperties().isTrue(iLinearGraphicsContext.filled, false)) {
					mesh.line.add(c);
				}
				if (c.getProperties().isTrue(iLinearGraphicsContext.pointed, false)) {
					point.line.add(c);
				}
			}
		}
	}

	public DirectLayer getDirectLayer(String name) {

		DirectLayer d = directLayer.get(name);
		if (d == null) {
			d = new DirectLayer();
			d.line = getDirectLineLayer(name);
			d.mesh = getDirectMeshLayer(name);
			d.point = getDirectPointLayer(name);
			d.fallback = new ArrayList<CachedLine>();
			d.submit = new ArrayList_Mod<CachedLine>();
			directLayer.put(name, d);
		}
		return d;
	}

	static public class ArrayList_Mod<T> extends ArrayList<T> {
		int getMod() {
			return modCount;
		}

	}

	public void installWebbrowserSupport(boolean useRectTextures) {
		new SimpleWebpageDrawing(useRectTextures).installInto(context);
	}

}
