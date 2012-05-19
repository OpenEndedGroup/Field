package field.core.ui.text.embedded;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.rmi.server.UID;
import java.util.LinkedHashMap;

import javax.swing.JComponent;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;

import field.core.Constants;
import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.plugins.python.OutputInsertsOnSheet;
import field.core.ui.SmallMenu;
import field.core.ui.text.embedded.CustomInsertDrawing.iAcceptsInsertRenderingContext;
import field.core.ui.text.embedded.CustomInsertDrawing.iInsertRenderingContext;
import field.core.ui.text.embedded.CustomInsertSystem.ExecutesWhat;
import field.core.ui.text.embedded.CustomInsertSystem.ExecutesWhen;
import field.core.ui.text.embedded.CustomInsertSystem.ProvidedComponent;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.BaseMath;
import field.namespace.key.FKey;

public class MinimalSlider extends JComponent implements iAcceptsInsertRenderingContext{

	static public class Component extends ProvidedComponent {
		public transient iUpdateable notify;
		protected float value;

		protected ExecutesWhen when;
		protected ExecutesWhat what;

		String name = "Component:" + new UID().toString() + ".transient";

		@Override
		public void deserialize(final iVisualElement inside) {
			super.deserialize(inside);

			if (when == null)
				when = ExecutesWhen.never;
			if (what == null)
				what = ExecutesWhat.line;

			component = new MinimalSlider() {
				@Override
				public void execute() {
					executeThisLine(irc, what);
				}

				@Override
				public void setValue(float lvalue) {
					super.setValue(lvalue);
					Component.this.value = lvalue;
					Component.this.updateValue();
				}

				@Override
				protected void echoSlider() {
					System.out.println(" about to echo");
					OutputInsertsOnSheet.wrapExisting("testSlider", inside, Component.this);
					System.out.println(" about to echo complete");
				}
			};

			if (when == null)
				when = ExecutesWhen.never;
			if (what == null)
				what = ExecutesWhat.line;

			((MinimalSlider) component).when = when;
			((MinimalSlider) component).what = what;

			((MinimalSlider) component).setValue(value);

		}

		@Override
		public String getCurrentRepresentedString() {
			return value + "";
		}

		@Override
		public void makeNew() {

		}

		@Override
		public void preserialize() {
			if (component != null)
				when = ((MinimalSlider) component).when;
			if (component != null)
				what = ((MinimalSlider) component).what;
		}

		protected void updateValue() {
			if (notify != null)
				notify.update();
		}
	}

	static public class Component_iFloatProvider extends Component {

		String name = "Component_iFloatProvider:" + new UID().toString() + ".transient";
		FKey localKey;

		@Override
		public String getCurrentRepresentedString() {
			if (localKey == null)
				localKey = new FKey(name).rootSet(value);
			return "trans(FKeyByName(\"" + localKey.getName() + "\", " + value + " ))";
		}

		@Override
		public void makeNew() {
			name = "Component_iFloatProvider:" + new UID().toString() + ".transient";
			localKey = new FKey(name).rootSet(value);
		}

		@Override
		protected void updateValue() {
			if (localKey == null)
				localKey = new FKey(name).rootSet(value);
			localKey.rootSet(value);
		}
	}

	public ExecutesWhat what = ExecutesWhat.line;
	private int initialDown;
	private Font font;
	ExecutesWhen when = ExecutesWhen.never;

	int height = 12;

	int sliderWidth = 25;

	float value = 0.5f;

	boolean on = false;
	protected iInsertRenderingContext irc;

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
		return new Dimension(Integer.MAX_VALUE, height);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(0, height);
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension p = super.getPreferredSize();
		return new Dimension(p.width, height);
	}

	public float getValue() {
		return value;
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
	}

	public void setValue(float lvalue) {
		boolean change = value != lvalue;
		value = lvalue;
	}

	private int leftX() {
		return (int) ((this.getBounds().width - sliderWidth) * value);
	}

	private int rightX() {
		return (int) ((this.getBounds().width - sliderWidth) * value + sliderWidth);
	}

	protected void echoSlider() {
	}

	protected void execute() {
	}

	@Override
	protected void paintComponent(Graphics g) {
		Rectangle bounds = this.getBounds();

		Graphics2D g2 = (Graphics2D) g;

//		g2.setColor(new Color(0.22f, 0.22f, 0.2f, 0.4f));
		g2.setPaint(new GradientPaint(0, 0, new Color(1,1,1,0.1f), 0, bounds.height, new Color(0,0,0,0.1f)));

		GeneralPath path = new GeneralPath();
		path.moveTo(0, 0);
		path.lineTo(bounds.width - 1, 0);
		path.lineTo(bounds.width - 1, bounds.height - 1);
		path.lineTo(0, bounds.height - 1);
		path.lineTo(0, 0);
		g2.fill(path);
		g2.draw(path);
		g2.draw(path);

		int leftX = leftX();
		int rightX = rightX();

		g2.setColor(new Color(0.0f, 0.0f, 0.0f, on ? 0.7f : 0.5f ));
		g2.fillRect(leftX, 2, rightX - leftX, 12);
		if (on)
			g2.fillRect(leftX, 2, rightX - leftX, 12);
		path = new GeneralPath();
		path.moveTo(0, 0);
		path.lineTo(0, bounds.height - 1);
		path.moveTo(bounds.width - 1, 0);
		path.lineTo(bounds.width - 1, bounds.height - 1);
		path.moveTo(bounds.width - 1, (bounds.height) / 2);
		path.lineTo(0, (bounds.height) / 2);
		g2.draw(path);
		g2.setStroke(new BasicStroke(2));
		g2.setColor(new Color(0.0f, 0.0f, 0.0f, 0.35f));
		g2.draw(path);

		if (font == null) {
			font = new Font(Constants.defaultFont, Font.PLAIN, 8);
		}
		String label = BaseMath.toDP(value, 3) + "";
		g2.setFont(font);
		g2.setColor(new Color(1, 1, 1, on ? 1f : 0.75f));

		int w = g2.getFontMetrics(font).charsWidth(label.toCharArray(), 0, label.length());
		g2.drawString(label, (leftX + rightX - w) / 2, bounds.height - 4);

		enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
	}

	protected void popup(MouseEvent e) {
		LinkedHashMap<String, iUpdateable> menu = new LinkedHashMap<String, iUpdateable>();
		menu.put("Auto execute: when", null);
		menu.put((when == ExecutesWhen.always ? "!" : "") + "  always", new iUpdateable() {

			public void update() {
				when = ExecutesWhen.always;
			}
		});
		menu.put((when == ExecutesWhen.onMouseUp ? "!" : "") + "  on mouse up", new iUpdateable() {

			public void update() {
				when = ExecutesWhen.onMouseUp;
			}
		});
		menu.put((when == ExecutesWhen.never ? "!" : "") + "  never", new iUpdateable() {

			public void update() {
				when = ExecutesWhen.never;
			}
		});

		menu.put("Auto execute: what", null);
		menu.put((what == ExecutesWhat.line ? "!" : "") + "  this line", new iUpdateable() {

			public void update() {
				what = ExecutesWhat.line;
			}
		});
		menu.put((what == ExecutesWhat.enclosingBlock ? "!" : "") + "  enclosing block", new iUpdateable() {

			public void update() {
				what = ExecutesWhat.enclosingBlock;
			}
		});
		menu.put((what == ExecutesWhat.everything ? "!" : "") + "  everything", new iUpdateable() {

			public void update() {
				what = ExecutesWhat.everything;
			}
		});
//		menu.put("Experimental", null);
//		menu.put(" Echo slider on sheet ", new iUpdateable() {
//			public void update() {
//				echoSlider();
//			}
//		});

		
		Canvas canvas = irc.getControl();
		Point p = Launcher.display.map(canvas, irc.getControl().getShell(), new Point(e.getX(), e.getY()));
		new SmallMenu().createMenu(menu, irc.getControl().getShell(), null).show(p);
	}

	@Override
	protected void processMouseEvent(MouseEvent e) {
		
		System.out.println(" process mouse event in minimal slider");
		
		super.processMouseEvent(e);

		if (Platform.isPopupTrigger(e)) {
			popup(e);
		}

		int x = e.getX() - 0*this.bounds().x;
		int y = e.getY() - 0*this.bounds().y;
		if (e.getID() == e.MOUSE_PRESSED) {
			int leftX = leftX();
			int rightX = rightX();
			if (x >= leftX && x <= rightX) {
				initialDown = x;
				on = true;
			} else
				on = false;

		}
		if (e.getID() == e.MOUSE_RELEASED) {
			on = false;

			repaint();
			if (when == ExecutesWhen.always || when == ExecutesWhen.onMouseUp)
				execute();
		}
	}

	@Override
	protected void processMouseMotionEvent(MouseEvent e) {

		System.out.println(" process mouse motion event in minimal slider");

		super.processMouseMotionEvent(e);
		int x = e.getX() - 0*this.bounds().x;
		int y = e.getY() - 0*this.bounds().y;
		if (e.getID() == e.MOUSE_DRAGGED) {
			int dx = x - initialDown;
			initialDown = x;

			float lvalue = value + dx / (float) (this.getBounds().width - sliderWidth);
			if (lvalue > 1)
				lvalue = 1;
			if (lvalue < 0)
				lvalue = 0;
			setValue(lvalue);
			repaint();

			if (when == ExecutesWhen.always)
				execute();
		}
	}

	@Override
	public void setInsertRenderingContext(iInsertRenderingContext context) {
		this.irc = context;
	}
}
