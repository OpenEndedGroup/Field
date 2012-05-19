package field.core.ui.text.embedded;

import java.awt.Graphics2D;
import java.awt.geom.Dimension2D;

import javax.swing.JComponent;

public interface iMinimalLayerPainter {
	
	public void associate(JComponent inside);
	public void paintNow(Graphics2D g, Dimension2D size);
	
}
