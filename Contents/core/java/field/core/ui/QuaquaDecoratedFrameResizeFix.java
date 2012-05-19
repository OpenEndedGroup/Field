/**
 * 
 */
package field.core.ui;

import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import field.core.Platform;
import field.namespace.generic.ReflectionTools;
import field.util.MiscNative;

/**
 * PLAF decorated frames seem to have problems resizing when a component overlaps in any way the grow box, this is an issue because we want to use small palettes, which can only be made, right now, on 1.5 using LAF decorated frames.
 * 
 * to use this, you need to add this listener to the component that overlaps the grow box (typically the scroll pane)
 * 
 * you should also MiscNative().enableScreenUpdates in the paint method of the frame that you're in to get relatively smooth resizes.
 * 
 * @author marc
 * 
 */
public class QuaquaDecoratedFrameResizeFix implements MouseListener, MouseMotionListener {

	private final JComponent component;
	private final JFrame frame;
	private boolean inside;

	public QuaquaDecoratedFrameResizeFix(JComponent container, JFrame frame) {
		this.component = container;
		this.frame = frame;
	}

	public void mouseClicked(MouseEvent e) {
		inside = redispatchMouse(e, false);
	}

	public void mouseEntered(MouseEvent e) {
		redispatchMouse(e, false);
	}

	public void mouseExited(MouseEvent e) {
		redispatchMouse(e, false);
	}

	public void mousePressed(MouseEvent e) {
		inside = redispatchMouse(e, false);
	}

	public void mouseReleased(MouseEvent e) {
		redispatchMouse(e, false);
		inside = false;
	}

	public void mouseDragged(MouseEvent e) {
		if (inside) {
			new MiscNative().disableScreenUpdates();
			redispatchMouseMotion(e, false);
			frame.repaint();
		}
	}

	public void mouseMoved(MouseEvent e) {
		redispatchMouseMotion(e, false);
	}

	private boolean redispatchMouse(MouseEvent e, boolean repaint) {
		Point componentPoint = e.getPoint();
		Container container = component;
		Rectangle b = container.getBounds();

		if (componentPoint.x > b.width - 15 && componentPoint.y > b.height - 15) {
			Point rootPanePoint = SwingUtilities.convertPoint(component, componentPoint, frame.getRootPane());

			Method processMouseevent = ReflectionTools.findFirstMethodCalled(JFrame.class, "processMouseEvent");
			try {
				processMouseevent.invoke(frame, new MouseEvent(frame, e.getID(), e.getWhen(), e.getModifiers(), rootPanePoint.x, rootPanePoint.y, e.getClickCount(), Platform.isPopupTrigger(e)));
			} catch (IllegalArgumentException e1) {
			} catch (IllegalAccessException e1) {
			} catch (InvocationTargetException e1) {
			}
			return true;
		}
		return false;
	}

	private void redispatchMouseMotion(MouseEvent e, boolean repaint) {
		Point componentPoint = e.getPoint();
		Container container = component;
		Rectangle b = container.getBounds();

		Point rootPanePoint = SwingUtilities.convertPoint(component, componentPoint, frame.getRootPane());

		Method processMouseevent = ReflectionTools.findFirstMethodCalled(JFrame.class, "processMouseMotionEvent");
		try {
			processMouseevent.invoke(frame, new MouseEvent(frame, e.getID(), e.getWhen(), e.getModifiers(), rootPanePoint.x, rootPanePoint.y, e.getClickCount(), Platform.isPopupTrigger(e)));
		} catch (IllegalArgumentException e1) {
		} catch (IllegalAccessException e1) {
		} catch (InvocationTargetException e1) {
		}
	}
}