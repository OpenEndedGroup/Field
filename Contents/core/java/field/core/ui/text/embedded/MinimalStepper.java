package field.core.ui.text.embedded;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.rmi.server.UID;

import javax.swing.JComponent;

import field.core.Constants;
import field.core.dispatch.iVisualElement;
import field.core.ui.text.embedded.CustomInsertSystem.ProvidedComponent;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.abstraction.iProvider;
import field.namespace.key.OKey;

public class MinimalStepper extends JComponent  {

	static public class Component_Stepper extends ProvidedComponent {
		transient private iProvider value;
		String name = "Component_iFloatProvider:" + new UID().toString() + ".transient";
		OKey localKey;
		boolean go = false;

		@Override
		public void deserialize(iVisualElement inside) {
			go = false;
			value = new iProvider() {

				public Object get() {
					
//					boolean d = ((MinimalStepper) component).isDisplayable();
//					if (!d)
//						return null;

					if (((MinimalStepper) component).didExecute != !go) {
						component.repaint();
					}

					((MinimalStepper) component).didExecute = !go;
					if (!go)
						return this;

					go = false;
					return null;
				}

				public iProvider set(String caption) {
					((MinimalStepper) component).caption = caption;
					((MinimalStepper) component).repaint();
					return this;
				}

			};
			localKey = new OKey(name).rootSet(value);
			component = new MinimalStepper("\u2014\u2014") {
				@Override
				protected void execute() {
					go = true;
					executed = true;
					repaint();
				}
			};
		}

		@Override
		public String getCurrentRepresentedString() {
			if (localKey == null)
				localKey = new OKey(name).rootSet(value);
			return "OKeyByName(\"" + localKey.getName() + "\", iProvider.Constant(None)).get()";
		}

		@Override
		public void makeNew() {
			name = "Component_iFloatProvider:" + new UID().toString() + ".transient";
			localKey = new OKey(name).rootSet(value);
		}

	}

	private Font font;

	private String caption;

	private boolean arm;

	protected boolean didExecute = false;

	int height = 16;

	int sliderWidth = 25;

	int width = 40;

	iUpdateable auto = null;

	boolean executed = false;

	public MinimalStepper(String caption) {
		this.caption = caption;
		addMouseListener(new MouseAdapter() {
		});

		setPreferredSize(new Dimension(40, height));
		setMinimumSize(new Dimension(40, height));
		setMaximumSize(new Dimension(40, height));

	}

	public MinimalStepper(String name, int width) {
		this(name);
		this.width = width;
	}

	@Override
	public float getAlignmentY() {
		return 0.9f;
	}

	@Override
	public Insets getInsets() {
		return new Insets(-20, -20, -20, -20);
	}

	@Override
	public Dimension getMaximumSize() {
		return new Dimension(width, height);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(width, height);
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension p = super.getPreferredSize();
		return new Dimension(width, height);
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
	}

	protected void execute() {
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
		if (executed) {
			g2.setColor(Constants.execution_color.toColor());
			g2.fill(path);
			executed = false;
			repaint();
		} else if (didExecute) {
			g2.setColor(new Color(0.62f, 0.22f, 0.2f, 0.5f));
			g2.fill(path);
		} else {
			g2.setColor(new Color(0.62f, 0.62f, 0.62f, 0.5f));
			g2.setPaint(new GradientPaint(0, 0, new Color(1, 1, 1, 0.1f), 0, height, new Color(0, 0, 0, 0.1f)));
			g2.fill(path);
		}
		if (arm & didExecute) {
			g2.setColor(new Color(0.2f, 0.22f, 0.2f, 0.5f));
			g2.fill(path);
			g2.draw(path);
		}
		if (mouseIn & didExecute) {
			g2.setStroke(new BasicStroke(8));
			g2.setColor(new Color(0.22f, 0.22f, 0.2f, 0.4f));
			g2.draw(path);
			g2.setStroke(new BasicStroke(1));
		}
		g2.setColor(new Color(0.22f, 0.22f, 0.2f, 0.4f));
		g2.draw(path);

		int width = g2.getFontMetrics().charsWidth(caption.toCharArray(), 0, caption.toCharArray().length);
		g2.setColor(new Color(0.02f, 0.02f, 0.1f, didExecute | executed ? 0.4f : 0.1f));

		g2.drawString(didExecute ? caption + "\u2014" : caption, (bounds.width / 2 - width / 2), 12);

		enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);
	}



	boolean mouseIn = false;

	@Override
	protected void processMouseEvent(MouseEvent e) {
		super.processMouseEvent(e);
		repaint();

		;//System.out.println(" mouse over");

		if ((e.getModifiers() & Event.ALT_MASK) == 0 && e.getID() == MouseEvent.MOUSE_RELEASED) {
			arm = false;
			repaint();
			execute();

		} else if ((e.getModifiers() & Event.ALT_MASK) == 0 && e.getID() == MouseEvent.MOUSE_PRESSED) {
			arm = true;
			repaint();
		} else if ((e.getModifiers() & Event.ALT_MASK) == 0 && e.getID() == MouseEvent.MOUSE_ENTERED) {
			mouseIn = true;
			repaint();
		} else if ((e.getModifiers() & Event.ALT_MASK) == 0 && e.getID() == MouseEvent.MOUSE_EXITED) {
			mouseIn = false;
			repaint();
		} else if ((e.getModifiers() & Event.ALT_MASK) != 0 && e.getID() == MouseEvent.MOUSE_RELEASED) {
			arm = false;
			repaint();
			if (auto != null)
				Launcher.getLauncher().deregisterUpdateable(auto);
			auto = null;
			executed = false;
			repaint();
		} else if ((e.getModifiers() & Event.ALT_MASK) != 0 && e.getID() == MouseEvent.MOUSE_PRESSED) {

			arm = true;
			repaint();
			if (auto == null)
				Launcher.getLauncher().registerUpdateable(auto = new iUpdateable() {

					public void update() {
						execute();
						boolean d = !executed;
						executed = true;
						if (d)
							repaint();
					}
				});
		}
	}

}
