package field.core.plugins.drawing.tweak;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.swt.widgets.Event;

import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLine_m;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.drawing.tweak.TweakSplineUI.Selectable;
import field.core.plugins.drawing.tweak.TweakSplineUI.SelectedVertex;
import field.core.plugins.drawing.tweak.TweakSplineUI.SubSelection;
import field.core.plugins.drawing.tweak.TweakSplineUI.iSimpleSelectionTool;
import field.core.windowing.GLComponentWindow;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;

public class VertMarqueeTool implements iSimpleSelectionTool {


	private HashMap<Vector2, Selectable> selection;
	private Point downAt;
	private Point upAt;
	private TweakSplineUI inside;

	public boolean mouseDown(Event at) {
		downAt = new Point(at.x, at.y);
		return true;
	}

	public boolean mouseDrag(Event at) {
		upAt = new Point(at.x, at.y);
		return true;
	}

	public boolean mouseUp(Event at) {
		if (at !=null)
			upAt = new Point(at.x, at.y);

		doSelectionNow();
		
		downAt = null;
		upAt = null;
		
		return false;
	}


	private void doSelectionNow() {
		if (downAt == null || upAt == null)
			return;

		// inside.selection.clear();

		Set<Entry<Vector2, Selectable>> es = selection.entrySet();

		float x1 = Math.min(downAt.x, upAt.x);
		float y1 = Math.min(downAt.y, upAt.y);
		float x2 = Math.max(downAt.x, upAt.x);
		float y2 = Math.max(downAt.y, upAt.y);

		for (Entry<Vector2, Selectable> s : es) {
			Vector2 v = s.getKey();
			if (v.x > x1 && v.x < x2) {
				Selectable sel = s.getValue();
				if (!inside.selection.contains(sel.current)) {
					inside.addToSelection(sel);
					if (sel.current.onLine.events.size() > (sel.current.vertexIndex + 1) && sel.current.onLine.events.get(sel.current.vertexIndex + 1).method.equals(iLine_m.cubicTo_m))
						sel.current.whatSelected.add(SubSelection.nextControl);
					sel.current.whatSelected.add(SubSelection.postion);
					if (sel.current.vertex.method.equals(iLine_m.cubicTo_m))
						sel.current.whatSelected.add(SubSelection.previousControl);
				}
			}

		}

	}

	public void paint() {

		if (downAt == null || upAt == null)
			return;

		CachedLine marqueLine = new CachedLine();
		marqueLine.getProperties().put(iLinearGraphicsContext.color, new Vector4(0.5f, 0, 0, 0.9f));
		marqueLine.getProperties().put(iLinearGraphicsContext.strokeColor, new Vector4(0.5f, 0, 0, 0.9f));
		marqueLine.getProperties().put(iLinearGraphicsContext.fillColor, new Vector4(0.25f, 0, 0, 0.2f));
		// marqueLine.getProperties().put(iLinearGraphicsContext.strokeType,
		// new BasicStroke(1, BasicStroke.CAP_BUTT,
		// BasicStroke.JOIN_BEVEL, 1, new float[] { 5, 5 }, 0));
		marqueLine.getProperties().put(iLinearGraphicsContext.filled, true);
		marqueLine.getProperties().put(iLinearGraphicsContext.notForExport, true);

		marqueLine.getInput().moveTo((float) downAt.x, GLComponentWindow.getCurrentWindow(null).transformWindowToCanvas(new Vector2()).y);
		marqueLine.getInput().lineTo((float) (upAt.x), GLComponentWindow.getCurrentWindow(null).transformWindowToCanvas(new Vector2()).y);
		marqueLine.getInput().lineTo((float) (upAt.x), GLComponentWindow.getCurrentWindow(null).transformWindowToCanvas(new Vector2(0, 2000)).y);
		marqueLine.getInput().lineTo((float) (downAt.x), GLComponentWindow.getCurrentWindow(null).transformWindowToCanvas(new Vector2(0, 2000)).y);
		marqueLine.getInput().lineTo((float) (downAt.x), GLComponentWindow.getCurrentWindow(null).transformWindowToCanvas(new Vector2()).y);

		GLComponentWindow.currentContext.submitLine(marqueLine, marqueLine.getProperties());

	}

	public boolean begin(List<SelectedVertex> currentSelection, TweakSplineUI inside) {
		selection = inside.selectedableVertex;
		this.inside = inside;
		return true;
	}

	public int whileKey() {
		return 'v';
	}

}
