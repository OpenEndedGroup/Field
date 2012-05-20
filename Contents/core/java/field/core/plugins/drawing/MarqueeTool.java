package field.core.plugins.drawing;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Event;

import field.bytecode.protect.DeferedInQueue.iProvidesQueue;
import field.bytecode.protect.DeferedInQueue.iRegistersUpdateable;
import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.InQueue;
import field.core.StandardFluidSheet;
import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.components.ComponentDrawingUtils;
import field.core.windowing.components.RootComponent;
import field.core.windowing.components.RootComponent.iMousePeer;
import field.core.windowing.components.RootComponent.iPaintPeer;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.iComponent;
import field.graphics.dynamic.DynamicLine;
import field.graphics.dynamic.DynamicMesh;
import field.graphics.dynamic.iDynamicMesh;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;
import field.util.TaskQueue;

@Woven
public class MarqueeTool implements iMousePeer, iProvidesQueue, iPaintPeer {

	private final iVisualElement root;

	private Vector2 mouseDownAt;

	private iDynamicMesh mesh;

	private DynamicLine line;

	private List<iVisualElement> allElements;

	Set<iVisualElement> currentSelectedSet = new HashSet<iVisualElement>();

	TaskQueue queue = new TaskQueue();

	public MarqueeTool(iVisualElement root) {
		this.root = root;
	}

	public iRegistersUpdateable getQueueFor(Method m) {
		return queue;
	}

	public void keyPressed(ComponentContainer inside, Event arg0) {
	}

	public void keyReleased(ComponentContainer inside, Event arg0) {
	}

	public void keyTyped(ComponentContainer inside, Event arg0) {
	}

	public void mouseClicked(ComponentContainer inside, Event arg0) {
	}

	public void mouseDragged(ComponentContainer inside, Event arg0) {
		mouseDraggedImpl(inside, arg0);
		inside.requestRedisplay();
	}

	@InQueue
	public void mouseDraggedImpl(ComponentContainer inside, Event arg0) {

		// draw area
		iDynamicMesh mesh = getMesh();
		DynamicLine line = getLine();

		Rect r = new Rect(Math.min(mouseDownAt.x, arg0.x), Math.min(mouseDownAt.y, arg0.y), Math.abs(mouseDownAt.x - arg0.x), Math.abs(mouseDownAt.y - arg0.y));

		ComponentDrawingUtils.drawRectangle(mesh, line, null, (float) r.x, (float) r.y, (float) r.w, (float) r.h, new Vector4(0, 0, 0.1f, 0.1), new Vector4(1, 1, 1, 0.2));

		;//System.out.println(" marquee drawing <" + r + ">");

		mesh.getUnderlyingGeometry().performPass(null);
		line.getUnderlyingGeometry().performPass(null);

		// compute the current selected set

		Rect r2 = new Rect(0, 0, 0, 0);

		for (iVisualElement e : allElements) {
			e.getFrame(r2);

			if (r2.overlaps(r)) {
				if (!currentSelectedSet.contains(e)) {
					SelectionGroup<iComponent> group = VisualElement.selectionGroup.get(e);
					iComponent localView = VisualElement.localView.get(e);
					if (localView != null) {
						group.addToSelection(localView);
						currentSelectedSet.add(e);
						localView.setSelected(true);
					}
				}
			} else {
				if (currentSelectedSet.contains(e)) {
					SelectionGroup<iComponent> group = VisualElement.selectionGroup.get(e);
					iComponent localView = VisualElement.localView.get(e);
					if (localView != null) {
						group.removeFromSelection(localView);
						localView.setSelected(false);
					}
					currentSelectedSet.remove(e);
				}
			}

		}
	}

	public void mouseEntered(ComponentContainer inside, Event arg0) {
	}

	public void mouseExited(ComponentContainer inside, Event arg0) {
	}

	public void mouseMoved(ComponentContainer inside, Event arg0) {
	}

	public void mousePressed(ComponentContainer inside, Event arg0) {
		mouseDownAt = new Vector2(arg0.x, arg0.y);

		allElements = StandardFluidSheet.allVisualElements(root);

		// this might have to be the actual current selected set, unless
		// we are holding down shift or something
		currentSelectedSet.clear();
		inside.requestRedisplay();

		mouseDraggedImpl(inside, arg0);
	}

	public void mouseReleased(ComponentContainer inside, Event arg0) {
		inside.requestRedisplay();
	}

	public void paint(RootComponent inside) {
		paintNow();
	}

	public void paintNow() {
		queue.update();
	}

	private DynamicLine getLine() {
		if (line == null)
			line = DynamicLine.unshadedLine(null, 1);
		return line;
	}

	private iDynamicMesh getMesh() {
		if (mesh == null)
			mesh = DynamicMesh.unshadedMesh();
		return mesh;
	}

}
