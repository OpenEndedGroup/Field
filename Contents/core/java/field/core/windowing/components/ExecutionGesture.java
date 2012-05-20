package field.core.windowing.components;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Event;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.Yield;
import field.bytecode.protect.yield.YieldUtilities;
import field.core.Constants;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.pseudo.PseudoPropertiesPlugin;
import field.core.windowing.GLComponentWindow;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Triple;

public class ExecutionGesture {

	boolean going = false;

	ArrayList<Triple<iVisualElement, Vector2, Vector2>> ordering = new ArrayList<Triple<iVisualElement, Vector2, Vector2>>();

	public void paint() {
		if (!going)
			return;

		CachedLine c = new CachedLine();
		iLine ii = c.getInput();
		for (Vector2 v : gesture) {
			if (c.events.size() == 0)
				ii.moveTo(v.x, v.y);
			else
				ii.lineTo(v.x, v.y);
		}

		c.getProperties().put(iLinearGraphicsContext.thickness, 3f);
		c.getProperties().put(iLinearGraphicsContext.filled, false);
		c.getProperties().put(iLinearGraphicsContext.color, new Vector4(0, 0, 0, 0.3));

		GLComponentWindow.getCurrentWindow(null).currentContext.submitLine(c, c.getProperties());

		
		{
			CachedLine d = new CachedLine();
			
			Vector2 n = new Vector2(0.5f + gesture.get(0).y-gesture.get(gesture.size()-1).y, 0.25f -gesture.get(0).x+gesture.get(gesture.size()-1).x ).normalize().scale(15);
//			;//System.out.println(" label should be here <" + n + ">");

			d.getInput().moveTo(gesture.get(0).x-n.x, gesture.get(0).y-n.y);
			d.getProperties().put(iLinearGraphicsContext.containsText, true);
			d.getProperties().put(iLinearGraphicsContext.color, new Vector4(Constants.execution_color.x, Constants.execution_color.y, Constants.execution_color.z, 0.5));
			d.getProperties().put(iLinearGraphicsContext.fillColor, new Vector4(Constants.execution_color.x, Constants.execution_color.y, Constants.execution_color.z, 0.5));
			d.getInput().setPointAttribute(iLinearGraphicsContext.text_v, "Execute...");
			d.getInput().setPointAttribute(iLinearGraphicsContext.font_v, new Font(Constants.defaultFont, Font.PLAIN, 10));

//			GLComponentWindow.getCurrentWindow(null).currentContext.submitLine(d, d.getProperties());
		}

		
		int i = 1;
		for (Triple<iVisualElement, Vector2, Vector2> gt : ordering) {
			{
				CachedLine d = new CachedLine();
				d.getInput().moveTo((gt.middle.x*3 + gt.right.x) / 4, (gt.middle.y*3 + gt.right.y) / 4);
				d.getProperties().put(iLinearGraphicsContext.pointed, true);
				d.getProperties().put(iLinearGraphicsContext.pointColor, new Vector4(Constants.execution_color.x, Constants.execution_color.y, Constants.execution_color.z, 0.5));
				d.getInput().setPointAttribute(iLinearGraphicsContext.pointSize_v, 10f);
				GLComponentWindow.getCurrentWindow(null).currentContext.submitLine(d, d.getProperties());
			}

			{
				CachedLine d = new CachedLine();
				Rect f = gt.left.getFrame(null);
				f.insetAbsolute(5);
				d.getInput().moveTo((float) f.x, (float) f.y);
				d.getInput().lineTo((float) (f.x + f.w), (float) f.y);
				d.getInput().lineTo((float) (f.x + f.w), (float) (f.y + f.h));
				d.getInput().lineTo((float) (f.x), (float) (f.y + f.h));
				d.getInput().lineTo((float) (f.x), (float) (f.y));
				d.getProperties().put(iLinearGraphicsContext.color, new Vector4(Constants.execution_color.x, Constants.execution_color.y, Constants.execution_color.z, 0.15));
				d.getProperties().put(iLinearGraphicsContext.filled, true);
				d.getProperties().put(iLinearGraphicsContext.stroked, true);

				d.getInput().setPointAttribute(iLinearGraphicsContext.pointSize_v, 10f);
				GLComponentWindow.getCurrentWindow(null).currentContext.submitLine(d, d.getProperties());
			}

			{
				CachedLine d = new CachedLine();

				Vector2 n = new Vector2(0.5f + gt.middle.y - gt.right.y, 0.25f + gt.right.x - gt.middle.x).normalize().scale(15);

				d.getInput().moveTo(n.x+(gt.middle.x*3 + gt.right.x) / 4, n.y+(gt.middle.y*3 + gt.right.y) / 4);
				d.getProperties().put(iLinearGraphicsContext.containsText, true);
				d.getProperties().put(iLinearGraphicsContext.color, new Vector4(Constants.execution_color.x, Constants.execution_color.y, Constants.execution_color.z, 0.5));
				d.getProperties().put(iLinearGraphicsContext.fillColor, new Vector4(Constants.execution_color.x, Constants.execution_color.y, Constants.execution_color.z, 0.5));
				d.getInput().setPointAttribute(iLinearGraphicsContext.text_v, "" + i);
				d.getInput().setPointAttribute(iLinearGraphicsContext.font_v, new Font(Constants.defaultFont, Font.BOLD, 15));

				GLComponentWindow.getCurrentWindow(null).currentContext.submitLine(d, d.getProperties());
			}

			i++;
		}

	}

	public void end(Event arg0) {
		if (!going)
			return;
		going = false;
		GLComponentWindow.getCurrentWindow(null).requestRepaint();
		

		final ArrayList<Triple<iVisualElement, Vector2, Vector2>> ordering2 = new ArrayList<Triple<iVisualElement, Vector2, Vector2>>(ordering);
		Launcher.getLauncher().registerUpdateable(new iUpdateable(){

			@Woven
			@Yield
			public void update() {
				
				for(Triple<iVisualElement, Vector2, Vector2> gt : ordering2)
				{
					PseudoPropertiesPlugin.begin.get(gt.left).call(new Object[]{});
					YieldUtilities.yield(null);
				}
				Launcher.getLauncher().deregisterUpdateable(this);
			}});
		
		gesture.clear();
		ordering.clear();
	}

	List<Vector2> gesture = new ArrayList<Vector2>();

	public void begin(Event arg0) {
		going = true;
		gesture.clear();


		GLComponentWindow.getCurrentWindow(null).requestRepaint();

	}

	public void drag(Event arg0) {
		
		;//System.out.println(" -------- drag :"+arg0);
		
		if (gesture.size()>0)
		{
			Vector2 g1 = gesture.get(0);
			gesture.clear();
			gesture.add(g1);
			ordering.clear();
		}

		drag(new Vector2(arg0.x, arg0.y));
	}

	public void drag(Vector2 arg0) {
		if (!going)
			return;

		while (gesture.size() != 0 && gesture.get(gesture.size() - 1).distanceFrom(new Vector2(arg0.x, arg0.y)) > 3) {
			drag(new Vector2().lerp(gesture.get(gesture.size() - 1), arg0, 0.5f));
		}

		gesture.add(new Vector2(arg0.x, arg0.y));
		GLComponentWindow.getCurrentWindow(null).requestRepaint();

		Event e = new Event();
		e.x = (int) arg0.x;
		e.y = (int) arg0.y;
		iComponent h = GLComponentWindow.getCurrentWindow(null).getRoot().hit(e);

		if (h != null) {
			iVisualElement vv = h.getVisualElement();
			if (vv != null) {
				if (ordering.size() == 0 || ordering.get(ordering.size() - 1).left != vv) {
					ordering.add(new Triple<iVisualElement, Vector2, Vector2>(vv, new Vector2(arg0.x, arg0.y), new Vector2(arg0.x, arg0.y)));
				} else {
					ordering.get(ordering.size() - 1).right = new Vector2(arg0.x, arg0.y);
				}
			}
		}

	}

}
