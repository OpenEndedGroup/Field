package field.core.plugins.drawing.tweak;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;

import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.plugins.drawing.SplineComputingOverride;
import field.core.plugins.drawing.ThreedComputingOverride;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.drawing.threed.ThreedContext;
import field.core.plugins.drawing.tweak.TweakSplineUI.SelectedVertex;
import field.core.plugins.drawing.tweak.TweakSplineUI.iSimpleSelectionTool;
import field.core.ui.MarkingMenuBuilder;
import field.core.ui.UbiquitousLinks;
import field.core.util.FieldPyObjectAdaptor.iExtensible;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.iComponent;
import field.launch.iUpdateable;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.util.Dict;

public class FreehandTool3d_tweak implements iSimpleSelectionTool {

	static public final VisualElementProperty<List<RawSplineData>> rawSplineData = new VisualElementProperty<List<RawSplineData>>("rawSplineData");

	static public class RawSplineData implements iExtensible {
		public List<Vector2> points = new ArrayList<Vector2>();
		public List<Vector3> pressureData = new ArrayList<Vector3>();
		public List<Long> timestamps = new ArrayList<Long>();
		public List<Object> transformStates = new ArrayList<Object>();

		CachedLine cached;

		public Rect elementFrameAtDrawTime;

		public Object transformState;

		Dict d = new Dict();

		public RawSplineData copy() {
			RawSplineData rsd = new RawSplineData();
			rsd.points = new ArrayList<Vector2>(this.points);
			rsd.pressureData = new ArrayList<Vector3>(this.pressureData);
			rsd.timestamps = new ArrayList<Long>(this.timestamps);
			rsd.elementFrameAtDrawTime = this.elementFrameAtDrawTime;
			rsd.transformState = this.transformState;
			return rsd;
		}

		public Dict getDict() {
			if (d == null)
				d = new Dict();
			return d;
		}
	}

	protected String getPluginNameImpl() {
		return "freehand";
	}

	public FreehandTool3d_tweak() {
	}

	String description = "This <i>(highly experimental)</i> tool allows you to draw freehand splines directly \"into\" properties associated with the currently selected <b>3d</b> element(s)";
	private CachedLine currentLine1;
	private CachedLine currentLine2;
	RawSplineData creating = new RawSplineData();

	private Vector2 start;

	public boolean mouseDown(Event at) {

		if (Platform.isPopupTrigger(at) && (at.stateMask & SWT.ALT)==0 && (at.stateMask & SWT.SHIFT)==0)
		{
			pop(at);
			return false;
		}
		if (at == null)
			return false;
		if ((at.stateMask & SWT.ALT)!=0 || (at.stateMask & SWT.SHIFT)!=0)
			return false;
		e = getSelection();
		start = new Vector2(at.x, at.y);
		creating = new RawSplineData();

		creating.points.add(new Vector2(start));
		creating.pressureData.add(getPressureDataNow());
		creating.timestamps.add(System.currentTimeMillis());
		currentLine1 = null;
		currentLine2 = null;
		return true;
	}

	private void pop(Event at) {
		
		Set<iVisualElement> s = getSelection();
		if (s.size()!=1) return;
		
		MarkingMenuBuilder builder = new MarkingMenuBuilder();
		builder.newMenu("undo", "WH");
		builder.addUpdateable("undo", new iUpdateable() {

			public void update() {
				Set<iVisualElement> s = getSelection();
				for (iVisualElement ss : s) {
					List<RawSplineData> data = rawSplineData.get(ss);
					if (data != null) {
						if (data.size() > 0)
							data.remove(data.size() - 1);
					}
					SplineComputingOverride.executeMain(ss);
				}
			}
		});

		builder.newMenu("clear", "NH");
		builder.addUpdateable("clear everything", new iUpdateable() {

			public void update() {
				Set<iVisualElement> s = getSelection();
				for (iVisualElement ss : s) {
					List<RawSplineData> data = rawSplineData.get(ss);
					if (data != null) {
						if (data.size() > 0)
							data.clear();
					}
					SplineComputingOverride.executeMain(ss);
				}

			}
		});
		
		Rectangle b = GLComponentWindow.getCurrentWindow(null).getCanvas().getShell().getBounds();
		
		builder.getMenu(GLComponentWindow.getCurrentWindow(null).getCanvas(), new Point(GLComponentWindow.getCurrentWindow(null).getCanvas().getParent().getBounds().x+at.x+b.x, at.y+b.y));
	}

	public boolean mouseDrag(Event at) {
		if (at == null)
			return false;
		if ((at.stateMask & SWT.ALT)!=0 || (at.stateMask & SWT.SHIFT)!=0)
			return true;
		creating.points.add(new Vector2(at.x, at.y));
		creating.pressureData.add(getPressureDataNow());
		creating.timestamps.add(System.currentTimeMillis());

		// todo, update drawing?
		// or ask visual element to
		// update the drawing?

		updateOngoing(creating);
		return true;
	}

	public boolean mouseUp(Event at) {
		if (at == null)
			return false;
		if ((at.stateMask & SWT.ALT)!=0 || (at.stateMask & SWT.SHIFT)!=0)
			return false;
		handleNewRawSplineData(creating);
		currentLine1 = null;
		currentLine2 = null;
		return false;
	}

	public void paint() {
		if (currentLine1 != null) {
			GLComponentWindow.currentContext.submitLine(currentLine1, currentLine1.getProperties());
			GLComponentWindow.currentContext.submitLine(currentLine2, currentLine2.getProperties());
		}
		Set<iVisualElement> ss = getSelection();
		for (iVisualElement s : ss) {
			iVisualElementOverrides o = iVisualElement.overrides.get(s);
			if (o instanceof ThreedComputingOverride) {
				Freehand3dUtils u = new Freehand3dUtils(s);
				u.paintGuidePlaneNow(SplineComputingOverride.computed_linesToDraw.get(s), ((ThreedComputingOverride) o).defaultContext);
			}
		}

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

		cl.getProperties().put(iLinearGraphicsContext.derived, 1f);
		cl.getProperties().put(iLinearGraphicsContext.thickness, 2f);
		cl.getProperties().put(iLinearGraphicsContext.color, new Vector4(0, 0, 0, 0.25f));

		currentLine2 = new LineUtils().transformLine(cl, null, null, null, null);

	}

	Set<iVisualElement> e = new HashSet<iVisualElement>();
	private iVisualElement root;

	protected void handleNewRawSplineData(RawSplineData data) {

		Iterator<iVisualElement> vi = e.iterator();
		while (vi.hasNext()) {
			iVisualElement v = vi.next();
			handleNewRawSplineData(v, data);
			if (vi.hasNext())
				data = data.copy();
		}
	}

	public int whileKey() {
		return '.';
	}

	public boolean begin(List<SelectedVertex> currentSelection, TweakSplineUI inside) {
		root = inside.inside;
		return true;
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
		System.out.println(" adding new raw spline data to element <" + rawSplineData.get(v) + ">");

		data.elementFrameAtDrawTime = v.getFrame(null);

		iVisualElementOverrides over = v.getProperty(iVisualElement.overrides);
		if (over instanceof ThreedComputingOverride) {
			ThreedContext targetContext = ((ThreedComputingOverride) over).defaultContext;
			data.transformState = targetContext.getTransformState();
		}

		// we need callbacks and the like, no?
		SplineComputingOverride.executeMain(v);
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
