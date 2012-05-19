package field.core.ui.text.rulers;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Event;

public interface iRuler {

	public abstract void keyEvent(Event e, int position, int start, int end);
	public abstract void mouseExited();

	public boolean mouseOverAt(float x, float y);
	public void off();
	public void on();
	public abstract void paintNow(GC g, StyledText p, Canvas ruler);

	public abstract void mouseMove(MouseEvent e);
	public abstract void mouseDown(MouseEvent arg0);
	public abstract void mouseUp(MouseEvent arg0);

}