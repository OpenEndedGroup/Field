package field.extras.plugins.hierarchy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import field.core.dispatch.VisualElementOverrides;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.plugins.BaseSimplePlugin;
import field.core.plugins.drawing.SplineComputingOverride;
import field.core.plugins.drawing.ThreedComputingOverride;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.drawing.threed.ThreedContext;
import field.core.plugins.drawing.tweak.Freehand3dUtils;
import field.core.plugins.drawing.tweak.FreehandTool3d_tweak;
import field.core.plugins.drawing.tweak.FreehandTool3d_tweak.RawSplineData;
import field.core.ui.UbiquitousLinks;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.iComponent;
import field.extras.plugins.hierarchy.HierarchyPlugin.Mode;
import field.extras.plugins.hierarchy.HierarchyPlugin.iEventHandler;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;

public class FreehandTool3d extends BaseSimplePlugin {

	// we have data stored in sheets that reference FreehandTool3d.RawSplineData
	static public class RawSplineData extends FreehandTool3d_tweak.RawSplineData
	{
		public RawSplineData copy() {
			RawSplineData rsd = new RawSplineData();
			rsd.points = new ArrayList<Vector2>(this.points);
			rsd.pressureData = new ArrayList<Vector3>(this.pressureData);
			rsd.timestamps = new ArrayList<Long>(this.timestamps);
			rsd.elementFrameAtDrawTime = this.elementFrameAtDrawTime;
			rsd.transformState = this.transformState;
			return rsd;
		}
	}
	
	static public final VisualElementProperty<List<RawSplineData>> rawSplineData = new VisualElementProperty<List<RawSplineData>>("rawSplineData");

	private HierarchyPlugin hp;

	protected String getPluginNameImpl() {
		return "freehand";
	}

	public FreehandTool3d() {
	}

	String description = "# Freehand 3d (3)\n\n This <i>(highly experimental)</i> tool allows you to draw freehand splines directly \"into\" properties associated with the currently selected <b>3d</b> element(s). The main property is called " + UbiquitousLinks.links.link("<i>rawSplineData</i>", UbiquitousLinks.links.code_copyTextToClipboard("rawSplineData"));
	private CachedLine currentLine1;
	private CachedLine currentLine2;

	@Override
	public void registeredWith(final iVisualElement root) {
		super.registeredWith(root);

		field.launch.Launcher.getLauncher().registerUpdateable(new iUpdateable() {

			public void update() {
				hp = HierarchyPlugin.hierarchyPlugin.get(root);
				hp.addTool("icons/pen3_16x16.png", getEventHandler(), new iUpdateable() {
					public void update() {
					}
				}, "Freehand spline drawer tool", "Freehand spline", description);
				Launcher.getLauncher().deregisterUpdateable(this);
			}
		});

	}

	public class Hover implements iEventHandler {

		public iEventHandler idle() {
			// iVisualElement.enclosingFrame.get(root).getOverlayAnimationManager().requestRepaint();
			return this;
		}

		public iEventHandler key(char character, int modifiers) {
			return this;
		}

		public iEventHandler mouse(Vector2 at, int buttons) {
			return this;
		}

		public iEventHandler transition(Mode from, Mode to, Vector2 at, int buttons) {
			if (to == Mode.down) {
				e = getSelection();
				;//;//System.out.println(" selection at drag start is <" + e + ">");
			}

			if (to == Mode.down) {

				;//;//System.out.println(" transition out ");

				Drag d = new Drag(at);
				updateOngoing(d.creating);
				// updateOngoing(d.creating);
				d.idle();
				return d;
			}

			return this;
		}

		public void paintNow() {

			Set<iVisualElement> ss = getSelection();
			for (iVisualElement s : ss) {
				iVisualElementOverrides o = iVisualElement.overrides.get(s);
				if (o instanceof ThreedComputingOverride) {
					Freehand3dUtils u = new Freehand3dUtils(s);
					u.paintGuidePlaneNow(SplineComputingOverride.computed_linesToDraw.get(s), ((ThreedComputingOverride) o).defaultContext);
				}
			}

		}

	}

	public class Drag implements iEventHandler {
		private Vector2 start;

		RawSplineData creating = new RawSplineData();

		public Drag(Vector2 at) {
			start = at;
			creating = new RawSplineData();

			creating.points.add(at);
			creating.pressureData.add(getPressureDataNow());
			creating.timestamps.add(System.currentTimeMillis());
			currentLine1 = null;
			currentLine2 = null;
		}

		public iEventHandler idle() {
			;//;//System.out.println(" should requesting draw");
			if (currentLine1 != null) {
				if (hp.fastContext != null) {
					hp.fastContext.submitLine(currentLine1, currentLine1.getProperties());
					hp.fastContext.submitLine(currentLine2, currentLine2.getProperties());

					;//;//System.out.println(" requesting draw");
					// iVisualElement.enclosingFrame.get(root).getOverlayAnimationManager().requestRepaint();
				}
			}
			return this;
		}

		public iEventHandler key(char character, int modifiers) {
			return this;
		}

		public iEventHandler mouse(Vector2 at, int buttons) {
			creating.points.add(at);
			creating.pressureData.add(getPressureDataNow());
			creating.timestamps.add(System.currentTimeMillis());

			Iterator<iVisualElement> vi = e.iterator();
			while (vi.hasNext()) {
				iVisualElement v = vi.next();

				iVisualElementOverrides over = v.getProperty(iVisualElement.overrides);
				if (over instanceof ThreedComputingOverride) {
					ThreedContext targetContext = ((ThreedComputingOverride) over).defaultContext;
					creating.transformStates.add(targetContext.getTransformState());

				}
			}

			// todo, update drawing?
			// or ask visual element to
			// update the drawing?

			updateOngoing(creating);
			// iVisualElement.enclosingFrame.get(root).getOverlayAnimationManager().requestRepaint();

			return this;
		}

		public iEventHandler transition(Mode from, Mode to, Vector2 at, int buttons) {

			;//;//System.out.println(" transition in drag <" + from + " -> " + to + ">");

			if (to == Mode.up) {
				handleNewRawSplineData(creating);

				// iVisualElement.enclosingFrame.get(root).getOverlayAnimationManager().requestRepaint();

				return new Hover();
			}
			return this;
		}

		public void paintNow() {

			Set<iVisualElement> ss = getSelection();
			for (iVisualElement s : ss) {
				iVisualElementOverrides o = iVisualElement.overrides.get(s);
				if (o instanceof ThreedComputingOverride) {
					Freehand3dUtils u = new Freehand3dUtils(s);
					u.paintGuidePlaneNow(SplineComputingOverride.computed_linesToDraw.get(s), ((ThreedComputingOverride) o).defaultContext);
				}
			}

		}
	}

	protected iEventHandler getEventHandler() {
		return new Hover();
	}

	protected void updateOngoing(RawSplineData creating) {
		CachedLine cl = new CachedLine();
		iLine input = cl.getInput();
		boolean first = true;
		for (Vector2 v : creating.points) {
			if (first) {
				first = false;
				input.moveTo(v.x, v.y);
			} else
				input.lineTo(v.x, v.y);
		}
		cl.getProperties().put(iLinearGraphicsContext.derived, 1f);
		cl.getProperties().put(iLinearGraphicsContext.thickness, 1f);
		cl.getProperties().put(iLinearGraphicsContext.color, new Vector4(0, 0, 0, 1f));

		currentLine1 = new LineUtils().transformLine(cl, null, null, null, null);
		hp.fastContext.submitLine(currentLine1, currentLine1.getProperties());

		cl.getProperties().put(iLinearGraphicsContext.derived, 1f);
		cl.getProperties().put(iLinearGraphicsContext.thickness, 2f);
		cl.getProperties().put(iLinearGraphicsContext.color, new Vector4(0, 0, 0, 0.25f));

		currentLine2 = new LineUtils().transformLine(cl, null, null, null, null);
		hp.fastContext.submitLine(currentLine2, currentLine2.getProperties());

	}

	Set<iVisualElement> e = new HashSet<iVisualElement>();

	protected void handleNewRawSplineData(RawSplineData data) {

		Iterator<iVisualElement> vi = e.iterator();
		while (vi.hasNext()) {
			iVisualElement v = vi.next();
			handleNewRawSplineData(v, data);
			if (vi.hasNext())
				data = data.copy();
		}
	}

	private Set<iVisualElement> getSelection() {
		SelectionGroup<iComponent> selectionGroup = iVisualElement.selectionGroup.get(root);
		Set<iComponent> s = selectionGroup.getSelection();
		Set<iVisualElement> e = new LinkedHashSet<iVisualElement>();
		for (iComponent c : s) {
			iVisualElement ve = c.getVisualElement();
			if (ve != null)
				e.add(ve);
		}
		return e;
	}

	protected void handleNewRawSplineData(iVisualElement v, RawSplineData data) {
		rawSplineData.addToList(ArrayList.class, v, data);

		data.elementFrameAtDrawTime = v.getFrame(null);

		iVisualElementOverrides over = v.getProperty(iVisualElement.overrides);
		if (over instanceof ThreedComputingOverride) {
			ThreedContext targetContext = ((ThreedComputingOverride) over).defaultContext;
			data.transformState = targetContext.getTransformState();

			;//;//System.out.println(" transform state is " + data.transformState);

		}

		// we need callbacks and the like, no?
		SplineComputingOverride.executeMain(v);
	}

	public Vector3 getPressureDataNow() {

		// try {
		// NSEvent v = NSApplication.sharedApplication().currentEvent();
		// float p = v.pressure();
		// ;//;//System.out.println("current event: " + v+
		// " / "+p+" "+v.tilt());
		// } catch (Throwable t) {
		// }
		return new Vector3(0.5, 0.5, 0.5);
	}

}
