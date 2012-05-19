package field.core.ui.text.embedded;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.GeneralPath;

import javax.swing.JComponent;

public class MinimalButton extends JComponent {

	int height = 16;

	int sliderWidth = 25;

	private Font font;

	protected final String caption;

	protected boolean arm;

	public MinimalButton(String caption) {
		this.caption = caption;
		addMouseListener(new MouseListener() {

			public void mouseClicked(MouseEvent e) {
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}

			public void mousePressed(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
			}
		});
		
		setPreferredSize(new Dimension(200, height));
		setMinimumSize(new Dimension(200, height));
		setMaximumSize(new Dimension(200, height));
		
	}

	int width = 300;
	
	public MinimalButton(String name, int width) {
		this(name);
		this.width = width;
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
	}

	@Override
	public float getAlignmentY() {
		return 0.9f;
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(width, height);
	}

	@Override
	public Dimension getMaximumSize() {
		return new Dimension(width, height);
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension p = super.getPreferredSize();
		return new Dimension(width, height);
	}

	@Override
	public Insets getInsets() {
		return new Insets(-20, -20, -20, -20);
	}

	@Override
	protected void paintComponent(Graphics g) {
		Rectangle bounds = this.getBounds();

		Graphics2D g2 = (Graphics2D) g;

		GeneralPath path = new GeneralPath();
		path.moveTo(0, 0);
		path.lineTo(bounds.width - 1, 0);
		path.lineTo(bounds.width - 1, bounds.height - 1);
		path.lineTo(0, bounds.height - 1);
		path.lineTo(0, 0);
		if (arm) {
			g2.setColor(new Color(0.22f, 0.22f, 0.2f, 0.1f));
			g2.fill(path);
		}
		else
		{
			g2.setColor(new Color(0.62f, 0.62f, 0.62f, 0.5f));
			g2.fill(path);			
		}
		g2.setColor(new Color(0.22f, 0.22f, 0.2f, 0.4f));
		g2.draw(path);
		g2.draw(path);

		int width = g2.getFontMetrics().charsWidth(caption.toCharArray(), 0, caption.toCharArray().length);
		g2.setColor(new Color(0.02f, 0.02f, 0.1f, 0.4f));

		g2.drawString(caption, (bounds.width / 2 - width / 2), 12);

	}

	@Override
	protected void processMouseEvent(MouseEvent e) {
		super.processMouseEvent(e);
		if ((e.getModifiers() & Event.ALT_MASK) == 0 && e.getID() == MouseEvent.MOUSE_RELEASED) {
			arm = false;
			repaint();
			execute();

		} else if ((e.getModifiers() & Event.ALT_MASK) == 0 && e.getID() == MouseEvent.MOUSE_PRESSED) {
			arm = true;
			repaint();
		}
	}

	protected void execute() {
	}
}
