package field.core.ui;

import java.awt.event.MouseEvent;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.DraggableComponent.Resize;
import field.core.windowing.components.iComponent;
import field.namespace.generic.ReflectionTools;

public class PresentationMode {

    static public final VisualElementProperty<PresentationParameters> present = new VisualElementProperty<PresentationParameters>("present");

    public PresentationMode() {

    }

    static public boolean isHidden(iComponent c) {
	iVisualElement v = c.getVisualElement();
	if (v == null)
	    return false;
	PresentationParameters p = v.getProperty(present);
	if (p == null)
	    return false;
	if (!p.always && !GLComponentWindow.getCurrentWindow(c).present)
	    return false;
	return p.hidden;
    }

    static public boolean canResizeWidth(iComponent c) {
	iVisualElement v = c.getVisualElement();
	if (v == null)
	    return true;
	PresentationParameters p = v.getProperty(present);
	if (p == null)
	    return true;
	if (!p.always && !GLComponentWindow.getCurrentWindow(c).present)
	    return false;
	return !p.fixedWidth;
    }

    static public boolean canResizeHeight(iComponent c) {
	iVisualElement v = c.getVisualElement();
	if (v == null)
	    return true;
	PresentationParameters p = v.getProperty(present);
	if (p == null)
	    return true;
	if (!p.always && !GLComponentWindow.getCurrentWindow(c).present)
	    return false;
	return !p.fixedHeight;
    }

    static public boolean canMove(iComponent c) {
	iVisualElement v = c.getVisualElement();
	if (v == null)
	    return true;
	PresentationParameters p = v.getProperty(present);
	if (p == null)
	    return true;
	if (!p.always && !GLComponentWindow.getCurrentWindow(c).present)
	    return false;
	return !p.fixedPosition;
    }

    static public boolean transformOptionClick(iComponent c, Event e) {

	iVisualElement v = c.getVisualElement();
	if (v == null)
	    return false;
	PresentationParameters p = v.getProperty(present);
	if (p == null)
	    return false;
	if (!p.always && !GLComponentWindow.getCurrentWindow(c).present)
	    return false;
	if (p.clickBecomesOptionClick) {
	    if (e.button==1)
		{
		    e.stateMask = e.stateMask | SWT.ALT;
		    return true;
		}
	} else if (p.rightClickBecomesOptionClick) {
	    if (Platform.isPopupTrigger(e))
		{
		    e.stateMask = e.stateMask | SWT.ALT;
		    e.button = 1;
		}
	}
	return false;
    }

    static public boolean isSpace(iComponent c, Event e) {
	iVisualElement v = c.getVisualElement();
	if (v == null)
	    return false;
	PresentationParameters p = v.getProperty(present);
	if (p == null)
	    return false;
	if (!p.always && !GLComponentWindow.getCurrentWindow(c).present)
	    return false;

	if (Platform.isPopupTrigger(e)) {
	    return p.rightClickBecomesSpace || p.rightClickBecomesOptionClick;
	}

	return false;
    }

    public static void filterResize(iComponent c, Set<Resize> currentResize) {
	if (!canMove(c)) {
	    currentResize.remove(Resize.translate);
	    currentResize.remove(Resize.left);
	    currentResize.remove(Resize.right);
	    currentResize.remove(Resize.up);
	    currentResize.remove(Resize.down);
	    currentResize.remove(Resize.innerScale);
	    currentResize.remove(Resize.innerTranslate);
	}
	if (!canResizeHeight(c)) {
	    currentResize.remove(Resize.up);
	    currentResize.remove(Resize.down);
	}
	if (!canResizeWidth(c)) {
	    currentResize.remove(Resize.left);
	    currentResize.remove(Resize.right);
	    currentResize.remove(Resize.right);
	}
    }

    public static boolean isSelectable(iComponent c) {
	iVisualElement v = c.getVisualElement();
	if (v == null)
	    return true;
	PresentationParameters p = v.getProperty(present);
	if (p == null)
	    return true;
	if (!p.always && !GLComponentWindow.getCurrentWindow(c).present)
	    return false;

	return !p.notSelectable;
    }

}
