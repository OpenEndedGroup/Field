package field.core.ui.text.embedded;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.LinkedHashMap;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Canvas;

import field.bytecode.protect.Woven;
import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.ui.SmallMenu;
import field.core.ui.text.embedded.CustomInsertDrawing.iAcceptsInsertRenderingContext;
import field.core.ui.text.embedded.CustomInsertDrawing.iInsertRenderingContext;
import field.core.ui.text.embedded.CustomInsertSystem.ExecutesWhat;
import field.core.ui.text.embedded.CustomInsertSystem.ExecutesWhen;
import field.core.ui.text.embedded.CustomInsertSystem.ProvidedComponent;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.linalg.Vector4;

@Woven
public class MinimalColorWell extends JComponent implements iAcceptsInsertRenderingContext {

	public ExecutesWhen when;

	public ExecutesWhat what;
	protected iInsertRenderingContext irc;

	@Override
	public void setInsertRenderingContext(iInsertRenderingContext context) {
		this.irc = context;
	}

	static public class Component extends ProvidedComponent {

		protected Vector4 value = new Vector4(0, 0, 0, 01.0f);
		public transient iUpdateable notify;

		protected ExecutesWhen when = null;
		protected ExecutesWhat what = null;

		@Override
		public void deserialize(iVisualElement inside) {
			super.deserialize(inside);

			if (when == null)
				when = ExecutesWhen.never;
			if (what == null)
				what = ExecutesWhat.line;

			component = new MinimalColorWell() {
				@Override
				public void setValue(Vector4 lvalue) {
					super.setValue(lvalue);
					Component.this.value = lvalue;
					Component.this.updateValue();
				};

				@Override
				protected void execute() {
					executeThisLine(irc, what);
				}
			};

			((MinimalColorWell) component).setValue(value);

			if (when == null)
				when = ExecutesWhen.never;
			if (what == null)
				what = ExecutesWhat.line;

			((MinimalColorWell) component).when = when;
			((MinimalColorWell) component).what = what;

		}

		@Override
		public String getCurrentRepresentedString() {
			return "Vector4(" + value.x + ", " + value.y + ", " + value.z + ", " + value.w + ")";
		}

		@Override
		public void preserialize() {
			if (component != null)
				when = ((MinimalColorWell) component).when;
			if (component != null)
				what = ((MinimalColorWell) component).what;
		}

		protected void updateValue() {
			if (notify != null)
				notify.update();
		}
	}

	static public class Component255 extends ProvidedComponent {
		protected Vector4 value = new Vector4(0, 0, 0, 01.0f);
		public transient iUpdateable notify;

		protected ExecutesWhen when = ExecutesWhen.never;
		protected ExecutesWhat what = ExecutesWhat.line;

		@Override
		public void deserialize(iVisualElement inside) {
			super.deserialize(inside);
			component = new MinimalColorWell() {
				@Override
				public void setValue(Vector4 lvalue) {
					super.setValue(lvalue);
					Component255.this.value = lvalue;
					Component255.this.updateValue();
				};

				// @Override
				// protected void execute() {
				// executeThisLine(what);
				// }
			};

			((MinimalColorWell) component).setValue(value);

			((MinimalColorWell) component).when = when;
			((MinimalColorWell) component).what = what;

		}

		@Override
		public String getCurrentRepresentedString() {
			return "Vector4(" + value.x + "*255, " + value.y + "*255, " + value.z + "*255, " + value.w + "*255)";
		}

		@Override
		public void preserialize() {
			if (component != null)
				when = ((MinimalColorWell) component).when;
			if (component != null)
				what = ((MinimalColorWell) component).what;
		}

		protected void updateValue() {
			if (notify != null)
				notify.update();
		}
	}

	int height = 12;

	int width = 12;

	Vector4 color = new Vector4(0, 0, 0, 01.0f);

	JDialog d;

	boolean needsExecuting = false;

	public void changeColor() {
		if (Platform.isMac()) {
			changeColorMac();
		}
	}

	protected void changeColorMac() {
		org.eclipse.swt.widgets.ColorDialog d = new org.eclipse.swt.widgets.ColorDialog(CustomInsertDrawing.currentNub.canvas.getShell(), SWT.PRIMARY_MODAL);
		d.setRGB(new RGB((int) (Math.max(0, Math.min(255, 255 * color.x))), (int) (Math.max(0, Math.min(255, 255 * color.y))), (int) (Math.max(0, Math.min(255, 255 * color.z)))));
		d.setAlpha((int) (color.w * 255f));
		d.open();
		RGB rgb = d.getRGB();

		color.x = rgb.red / 255f;
		color.y = rgb.green / 255f;
		color.z = rgb.blue / 255f;

		color.w = d.getAlpha() / 255f;

		setValue(color);

		if (when == ExecutesWhen.always) {
			needsExecuting = true;
			repaint();
		}
	}

	protected void execute() {
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
		return new Dimension(width, height);
	}

	@Override
	public void setBounds(Rectangle r) {
		super.setBounds(new Rectangle(r.x, r.y, width, height));
	}

	public void setValue(Vector4 value) {
		boolean changed = false;
		if (color.distanceFrom(value) > 1e-5f)
			changed = true;
		color.set(value);
		if (changed) {
			repaint();
			if (when != ExecutesWhen.never)
				needsExecuting = true;
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		Rectangle bounds = this.getBounds();

		Graphics2D g2 = (Graphics2D) g;

		g2.setColor(new Color(color.x, color.y, color.z, 1));
		GeneralPath upper = new GeneralPath();
		upper.moveTo(0, 0);
		upper.lineTo(0, bounds.height);
		upper.lineTo(bounds.width, bounds.height);
		upper.lineTo(0, 0);
		g2.fill(upper);

		g2.setColor(new Color(color.x, color.y, color.z, color.w));
		GeneralPath lower = new GeneralPath();
		lower.moveTo(0, 0);
		lower.lineTo(bounds.width, bounds.height);
		lower.lineTo(bounds.width, 0);
		lower.lineTo(0, 0);
		g2.fill(lower);

		enableEvents(AWTEvent.MOUSE_EVENT_MASK);

		if (needsExecuting) {
			needsExecuting = false;
			execute();
		}
	}

	@Override
	protected void processMouseEvent(MouseEvent e) {

		if (Platform.isPopupTrigger(e)) {

			LinkedHashMap<String, iUpdateable> menu = new LinkedHashMap<String, iUpdateable>();
			menu.put("Auto execute: when", null);
			menu.put((when == ExecutesWhen.always ? "!" : "") + "  always", new iUpdateable() {

				public void update() {
					when = ExecutesWhen.always;
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

			Canvas canvas = irc.getControl();
			Point p = Launcher.display.map(canvas, irc.getControl().getShell(), new Point(e.getX(), e.getY()));
			new SmallMenu().createMenu(menu, irc.getControl().getShell(), null).show(p);

		} else if (e.getID() == e.MOUSE_PRESSED) {
			// if (d == null)
			{

				Vector4 color = this.color;
				if (color == null)
					color = new Vector4(0.5, 0.5, 0.5, 0);

				changeColor();

			}
			// d.setBackground(new Color(0.3f, 0.3f, 0.3f, 1f));
			// d.show();
		}
	}

}
