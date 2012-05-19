package field.extras.plugins.hierarchy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.BaseSimplePlugin;
import field.core.plugins.drawing.SplineComputingOverride;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.ui.UbiquitousLinks;
import field.core.util.FieldPyObjectAdaptor.iExtensible;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.iComponent;
import field.extras.plugins.hierarchy.HierarchyPlugin.Mode;
import field.extras.plugins.hierarchy.HierarchyPlugin.iEventHandler;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.util.Dict;

public class FreehandTool extends BaseSimplePlugin {

	static public final VisualElementProperty<List<RawSplineData>> rawSplineData = new VisualElementProperty<List<RawSplineData>>("rawSplineData");

	static public class RawSplineData implements iExtensible {
		public List<Vector2> points = new ArrayList<Vector2>();
		public List<Vector3> pressureData = new ArrayList<Vector3>();
		public List<Long> timestamps = new ArrayList<Long>();

		CachedLine cached;

		Rect elementFrameAtDrawTime;

		Dict d = new Dict();

		public RawSplineData copy() {
			RawSplineData rsd = new RawSplineData();
			rsd.points = new ArrayList<Vector2>(this.points);
			rsd.pressureData = new ArrayList<Vector3>(this.pressureData);
			rsd.timestamps = new ArrayList<Long>(this.timestamps);
			rsd.elementFrameAtDrawTime = this.elementFrameAtDrawTime;
			return rsd;
		}

		public Dict getDict() {
			if (d == null)
				d = new Dict();
			return d;
		}
	}

	private HierarchyPlugin hp;

	protected String getPluginNameImpl() {
		return "freehand";
	}

	public FreehandTool() {
	}

	String description = "# Freehand\n\n This tool allows you to draw freehand splines directly \"into\" properties associated with the currently selected element(s). The main property is called " + UbiquitousLinks.links.link("<i>rawSplineData</i>", UbiquitousLinks.links.code_copyTextToClipboard("rawSplineData"));
	private CachedLine currentLine1;
	private CachedLine currentLine2;

	@Override
	public void registeredWith(final iVisualElement root) {
		super.registeredWith(root);

		field.launch.Launcher.getLauncher().registerUpdateable(new iUpdateable() {

			public void update() {
				hp = HierarchyPlugin.hierarchyPlugin.get(root);
				hp.addTool("icons/pen_16x16.png", getEventHandler(), new iUpdateable() {
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
				System.out.println(" selection at drag start is <" + e + ">");
			}

			if (to == Mode.down) {

				System.out.println(" transition out ");

				Drag d = new Drag(at);
				updateOngoing(d.creating);
				updateOngoing(d.creating);
				d.idle();
				return d;
			}

			return this;
		}

		public void paintNow() {
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
			System.out.println(" should requesting draw");
			if (currentLine1 != null) {
				if (hp.fastContext != null) {
					hp.fastContext.submitLine(currentLine1, currentLine1.getProperties());
					hp.fastContext.submitLine(currentLine2, currentLine2.getProperties());
					System.out.println(" requesting draw");
					// iVisualElement.enclosingFrame.get(root).getOverlayAnimationManager().requestRepaint();
				}
			}
			return this;
		}

		public iEventHandler key(char character, int modifiers) {
			return this;
		}

		public iEventHandler mouse(Vector2 at, int buttons) {

			System.out.println(" --------- mouse inside freehand tool drag ---------");
			new Exception().printStackTrace();

			creating.points.add(at);
			creating.pressureData.add(getPressureDataNow());
			creating.timestamps.add(System.currentTimeMillis());

			// todo, update drawing?
			// or ask visual element to
			// update the drawing?

			updateOngoing(creating);
			// iVisualElement.enclosingFrame.get(root).getOverlayAnimationManager().requestRepaint();

			return this;
		}

		public iEventHandler transition(Mode from, Mode to, Vector2 at, int buttons) {

			System.out.println(" transition in drag <" + from + " -> " + to + ">");

			if (to == Mode.up) {
				handleNewRawSplineData(creating);

				// iVisualElement.enclosingFrame.get(root).getOverlayAnimationManager().requestRepaint();

				return new Hover();
			}
			return this;
		}

		public void paintNow() {
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

		// we need callbacks and the like, no?
		try {
			SplineComputingOverride.executeMain(v);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Vector3 getPressureDataNow() {

		// try {
		// NSEvent v = NSApplication.sharedApplication().currentEvent();
		// float p = v.pressure();
		// System.out.println("current event: " + v+
		// " / "+p+" "+v.tilt());
		// } catch (Throwable t) {
		// }
		return new Vector3(0.5, 0.5, 0.5);
	}

}
