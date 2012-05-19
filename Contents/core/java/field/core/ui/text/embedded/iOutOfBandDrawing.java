package field.core.ui.text.embedded;

import java.awt.Graphics2D;

import javax.swing.JComponent;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;

import field.core.dispatch.iVisualElement.Rect;

public interface iOutOfBandDrawing {
	
	public void paintOutOfBand(GC gc, StyledText ed);
	
	public void expandDamage(Rect d);
	
}
