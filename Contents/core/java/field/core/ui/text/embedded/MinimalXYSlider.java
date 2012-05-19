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

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;

import field.core.Constants;
import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.ui.SmallMenu;
import field.core.ui.text.embedded.CustomInsertSystem.ExecutesWhat;
import field.core.ui.text.embedded.CustomInsertSystem.ExecutesWhen;
import field.core.ui.text.embedded.CustomInsertSystem.ProvidedComponent;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.BaseMath;
import field.math.abstraction.iAcceptor;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.namespace.key.OKey;

public class MinimalXYSlider extends MinimalExpandable {
	static public class Component_tuple extends ProvidedComponent {

		public transient iUpdateable notify;

		protected ExecutesWhen when;

		protected ExecutesWhat what;
		float value;
		float valueY;

		@Override
		public void deserialize(iVisualElement inside) {
			super.deserialize(inside);

			component = new MinimalXYSlider() {
				// @Override
				public void execute() {
					executeThisLine(irc, what);
				};

				@Override
				public void setValue(float lvalue, float lvalueY) {
					super.setValue(lvalue, lvalueY);
					Component_tuple.this.value = lvalue;
					Component_tuple.this.valueY = lvalueY;
					Component_tuple.this.updateValue();
				};
			};
			if (when == null)
				when = ExecutesWhen.never;
			if (what == null)
				what = ExecutesWhat.line;

			((MinimalXYSlider) component).when = when;
			((MinimalXYSlider) component).what = what;

			((MinimalXYSlider) component).setValue(value, valueY);

		}

		@Override
		public String getCurrentRepresentedString() {
			return (value + "," + valueY);
		}

		@Override
		public void preserialize() {
			if (component != null)
				when = ((MinimalXYSlider) component).when;
			if (component != null)
				what = ((MinimalXYSlider) component).what;
		}

		protected void updateValue() {
			if (notify != null)
				notify.update();
		}
	}

	static public class Component_vector2 extends Component_tuple {

		@Override
		public String getCurrentRepresentedString() {
			return "Vector2(" + value + "," + valueY + ")";
		}
	}

	static public class Component_vector3 extends Component_tuple {

		@Override
		public String getCurrentRepresentedString() {
			return "Vector3(" + value + "," + valueY + ",0)";
		}
	}

	static public class Component_vector3_provider extends Component_tuple {

		String name = "Component_vector3_provider:" + new UID().toString() + ".transient";

		OKey<Vector3> localKey;
		
		@Override
		public String getCurrentRepresentedString() {
			if (localKey == null)
				localKey = new OKey<Vector3>(name).rootSet(new Vector3(value, valueY, 0));
			return "trans(OKeyByName(\"" + name + "\", Vector3(" + value + "," + valueY + ",0)))";
		}

		@Override
		public void makeNew() {
			name = "Component_vector3_provider:" + new UID().toString() + ".transient";
			localKey = new OKey<Vector3>(name).rootSet(new Vector3(value, valueY, 0));
		}

		@Override
		protected void updateValue() {
			if (localKey == null)
				localKey = new OKey<Vector3>(name).rootSet(new Vector3(value, valueY, 0));
			
			Vector3 old = localKey.get();
			old.setValue(value, valueY, 0);
			if (notify != null)
				notify.update();
		}
	}

	public ExecutesWhat what = ExecutesWhat.line;

	private int initialDown;

	private int initialDownY;

	private Font font;

	ExecutesWhen when = ExecutesWhen.never;

	int sliderWidth = 12;

	int sliderHeight = 12;

	float value = 0.5f;

	float valueY = 0.5f;

	boolean on = false;

	float alignment = 0.9f;

	@Override
	public float getAlignmentY() {
		return alignment;
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

	public Vector2 getValue() {
		return new Vector2(value, valueY);
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
	}

	public void setValue(float lvalue, float lvalueY) {

		System.out.println(" set value <" + lvalue + " " + lvalueY + ">");

		if (Float.isNaN(lvalue))
			return;
		if (Float.isNaN(lvalueY))
			return;

		boolean change = (value != lvalue) || (valueY != lvalueY);
		value = lvalue;
		valueY = lvalueY;

	}

	private int bottomY() {
		return (int) ((this.getBounds().height - sliderHeight) * valueY + sliderHeight);
	}

	private int leftX() {
		return (int) ((this.getBounds().width - sliderWidth) * value);
	}

	private int rightX() {
		return (int) ((this.getBounds().width - sliderWidth) * value + sliderWidth);
	}

	private int topY() {
		return (int) ((this.getBounds().height - sliderHeight) * valueY);
	}

	protected void execute() {
	}

	@Override
	protected void paintComponent(Graphics g) {

		Rectangle bounds = this.getBounds();

		Graphics2D g2 = (Graphics2D) g;

		// g2.setColor(new Color(0.22f, 0.22f, 0.2f, 0.4f));
		g2.setPaint(new GradientPaint(0, 0, new Color(1, 1, 1, 0.1f), 0, bounds.height, new Color(0, 0, 0, 0.1f)));

		GeneralPath path = new GeneralPath();
		path.moveTo(0, 0);
		path.lineTo(bounds.width - 1, 0);
		path.lineTo(bounds.width - 1, bounds.height - 1);
		path.lineTo(0, bounds.height - 1);
		path.lineTo(0, 0);
		g2.fill(path);
		g2.draw(path);
		g2.draw(path);

		super.paintComponent(g);

		int leftX = leftX() + 2;
		int rightX = rightX() - 2;
		int topY = topY() + 2;
		int bottomY = bottomY() - 2;

		g2.setColor(new Color(0, 0, 0, on ? 0.75f : 0.5f));
		g2.setColor(new Color(0.0f, 0.0f, 0.0f, 0.37f));
		g2.drawRect(leftX, topY, rightX - leftX, bottomY - topY);
		g2.fillRect(leftX, topY, rightX - leftX, bottomY - topY);
		if (on)
			g2.fillRect(leftX - 2, topY - 2, 5 + rightX - leftX, 5 + bottomY - topY);
		path = new GeneralPath();
		path.moveTo(0, 0);
		path.lineTo(0, bounds.height - 1);
		path.moveTo(bounds.width - 1, 0);
		path.lineTo(bounds.width - 1, bounds.height - 1);
		path.moveTo(bounds.width - 1, (topY + bottomY) / 2);
		path.lineTo(0, (topY + bottomY) / 2);
		g2.setStroke(new BasicStroke(1));
		g2.draw(path);
		g2.setStroke(new BasicStroke(2));
		g2.setColor(new Color(0.0f, 0.0f, 0.0f, 0.37f));
		g2.draw(path);

		path = new GeneralPath();
		path.moveTo(0, 0);
		path.lineTo(bounds.width - 1, 0);
		path.moveTo(bounds.width - 1, bounds.height - 1);
		path.lineTo(0, bounds.height - 1);
		path.moveTo((leftX + rightX) / 2, 0);
		path.lineTo((leftX + rightX) / 2, bounds.height - 1);
		g2.setColor(new Color(0, 0, 0, on ? 0.75f : 0.5f));
		g2.setStroke(new BasicStroke(1));
		g2.draw(path);
		g2.setColor(new Color(0.0f, 0.0f, 0.0f, 0.37f));
		g2.draw(path);

		if (font == null) {
			font = new Font(Constants.defaultFont, Font.PLAIN, 8);
		}
		String label = BaseMath.toDP(value, 3) + "," + BaseMath.toDP(valueY, 3);
		g2.setFont(font);
		g2.setColor(new Color(1, 1, 1, on ? 1f : 0.75f));
		int w = g2.getFontMetrics(font).charsWidth(label.toCharArray(), 0, label.length());
		g2.drawString(label, (maxCollapse - collapseness) * 5 + Math.max(2, Math.min(bounds.width - w - 12, (leftX + rightX - w) / 2)), Math.min(bounds.height - sliderHeight / 3, (topY + bottomY) / 2 + 3 + (maxCollapse - collapseness) * 4));

		enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);

		System.out.println(" coll :"+collapseness);
		
		if (collapseness>1)
			g2.drawImage(Constants.plus, bounds.width-14, 2, 12, 12, null);
		else
			g2.drawImage(Constants.minus, bounds.width-14, 2,12, 12,  null);

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

		Canvas canvas = irc.getControl();
		Point p = Launcher.display.map(canvas, irc.getControl().getShell(), new Point(e.getX(), e.getY()));
		new SmallMenu().createMenu(menu, irc.getControl().getShell(), null).show(p);
	}

	@Override
	protected void processMouseEvent(MouseEvent e) {
		System.out.println(" checking for shift down <"+(e.getID()==e.MOUSE_PRESSED)+"> <"+(e.getModifiers() & e.SHIFT_MASK)+">");
		super.processMouseEvent(e);
		if (Platform.isPopupTrigger(e)) {
			popup(e);
			return;
		}
		int x = e.getX() - 0 * this.bounds().x;
		int y = e.getY() - 0 * this.bounds().y;

		
		if (e.getID() == e.MOUSE_PRESSED && (e.getModifiers() & e.SHIFT_MASK) == 0) {
			int leftX = leftX();
			int rightX = rightX();
			int bottomY = bottomY();
			int topY = topY();
			if (x >= leftX && x <= rightX && y >= topY && y <= bottomY) {
				initialDown = x;
				initialDownY = y;
				on = true;
			} else
				on = false;
		} else if (e.getID() == e.MOUSE_PRESSED && (e.getModifiers() & e.SHIFT_MASK) !=0) {

			final float sx = value;
			final float sy = valueY;

			final float nx = x/(float)this.getBounds().width;
			final float ny = y/(float)this.getBounds().height;

			newInterpolationThread(100, new iAcceptor<Integer>() {

				@Override
				public iAcceptor<Integer> set(Integer to) {
					float alpha = to / 100f;

					float nnx = sx + (nx - sx) * alpha;
					float nny = sy + (ny - sy) * alpha;

					System.out.println(" -- nnx :" + nnx + " " + nny);

					setValue(nnx, collapseness > 1 ? valueY : nny);
					if (irc!=null)
					{
						irc.getControl().redraw();
					}
					return this;
				}
			});

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
		super.processMouseMotionEvent(e);
		// if (collapseness>0) return;

		System.out.println(" cc : " + collapseness + " check");

		int x = e.getX() - 0 * this.bounds().x;
		int y = e.getY() - 0 * this.bounds().y;
		if (e.getID() == e.MOUSE_DRAGGED && on) {
			int dx = x - initialDown;
			initialDown = x;
			int dy = y - initialDownY;
			initialDownY = y;

			float lvalue = value + dx / (float) (this.getBounds().width - sliderWidth);
			if (lvalue > 1)
				lvalue = 1;
			if (lvalue < 0)
				lvalue = 0;
			float lvalueY = valueY + dy / (float) (this.getBounds().height - sliderHeight);
			if (lvalueY > 1)
				lvalueY = 1;
			if (lvalueY < 0)
				lvalueY = 0;

			setValue(lvalue, collapseness > 1 ? valueY : lvalueY);

			repaint();
			if (when == ExecutesWhen.always)
				execute();

		}
	}

}