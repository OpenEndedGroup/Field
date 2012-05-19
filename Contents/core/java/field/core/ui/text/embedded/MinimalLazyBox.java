package field.core.ui.text.embedded;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.lang.reflect.InvocationTargetException;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JTextField;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;

import field.core.Constants;
import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.ui.PopupTextBox;
import field.core.ui.text.embedded.CustomInsertDrawing.Nub;
import field.core.ui.text.embedded.CustomInsertSystem.ProvidedComponent;
import field.core.ui.text.embedded.CustomInsertSystem.ProvidesWidth;
import field.launch.Launcher;
import field.math.abstraction.iAcceptor;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.ReflectionTools;
import field.namespace.key.OKey;

public class MinimalLazyBox extends JTextField implements iOutOfBandDrawing {

	// for now, we deliberately introduce a small leak per lazy box created
	// to workaround #127
	static protected LinkedHashSet<OKey> nogc = new LinkedHashSet<OKey>();

	static public class Component extends ProvidedComponent implements ProvidesWidth {
		protected String valueString = "";
		String name = "Component:" + new UID().toString() + ".transient";
		OKey localKey;

		public Component() {
		}

		@Override
		public void deserialize(iVisualElement inside) {
			if (localKey == null)
				localKey = new OKey(name) {
					public Object evaluate() {
						Object o = super.evaluate();

						System.out.println(" get for <" + name + "> happened <" + ((MinimalLazyBox) component).getText() + ">");

						((MinimalLazyBox) component).arm = false;
						((MinimalLazyBox) component).textAtLastUpdate = ((MinimalLazyBox) component).getText();

						// cause repaint
						return ((MinimalLazyBox) component).textAtLastUpdate;
					}
				}.rootSet(valueString);

			component = new MinimalLazyBox() {
				@Override
				public void setText(String to) {

					System.out.println(" set text called <" + to + ">");

					super.setText(to);
					Component.this.valueString = to;
					updateValue(valueString);
				};
			};

			((MinimalLazyBox) component).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {

					System.out.println(" action performed called " + ((MinimalLazyBox) component).getText());

					Component.this.valueString = ((MinimalLazyBox) component).getText();
					updateValue(valueString);
				}
			});
			((MinimalLazyBox) component).setText(valueString);
			((MinimalLazyBox) component).textAtLastUpdate = valueString;

			nogc.add(localKey);

		}

		@Override
		public int getWidthNow() {
			return ((MinimalLazyBox) component).getText().length() * 8 + 50;
		}

		@Override
		public String getCurrentRepresentedString() {
			if (localKey == null)
				localKey = new OKey(name) {
					public Object evaluate() {
						Object o = super.evaluate();

						System.out.println(" get for <" + name + "> happened ");

						((MinimalLazyBox) component).arm = false;
						((MinimalLazyBox) component).textAtLastUpdate = ((MinimalLazyBox) component).getText();

						// cause repaint
						return o;
					}
				}.rootSet(valueString);

			String val = "OKeyByName(\"" + name + "\", \'" + valueString + "\').get()";
			nogc.add(localKey);

			return "eval(" + val + ")";
		}

		@Override
		public void makeNew() {
			name = "Component:" + new UID().toString() + ".transient";
			localKey = new OKey(name) {
				public Object evaluate() {
					Object o = super.evaluate();

					System.out.println(" get2 for <" + name + "> happened ");

					((MinimalLazyBox) component).arm = false;
					((MinimalLazyBox) component).textAtLastUpdate = ((MinimalLazyBox) component).getText();

					// cause repaint
					return o;
				}
			}.rootSet(valueString);
			nogc.add(localKey);
		}

		@Override
		public void preserialize() {
		}

		public void updateValue(String valueString) {
			if (component != null) {
				Component.this.valueString = ((MinimalLazyBox) component).getText();

				((MinimalLazyBox) component).setSize(((MinimalLazyBox) component).getSize());
			}
			this.valueString = valueString;
			if (localKey == null) {
				// System.out.println(" making a bad key !");
				// localKey = new
				// OKey<String>(name).rootSet(valueString);
				makeNew();
			}
			localKey.rootSet(valueString);
			nogc.add(localKey);
		}
	}

	private String caption = "";

	private String textAtLastUpdate;

	int height = 16;

	Color background = new Color(0.1f, 0, 0, 0.2f);

	boolean arm = false;

	boolean centered = false;

	public MinimalLazyBox() {
		this.setFont(new Font(Constants.defaultFont, 0, 10));
		this.setCaretColor(new Color(1, 0, 0, 1f));
	}

	public MinimalLazyBox(String label) {
		this.setFont(new Font(Constants.defaultFont, 0, 10));
		this.setText(label);

		this.setCaretColor(new Color(1, 0, 0, 1f));

	}

	public void allViewHierarchy(List<Object> into, Object from, HashSet<Object> seen, iFunction<Boolean, Object> predicate) {
		if (seen.contains(from))
			return;

		seen.add(from);
		if (predicate.f(from))
			into.add(from);

		if (from instanceof Container) {
			Container c = (Container) from;
			for (int i = 0; i < c.getComponentCount(); i++) {
				allViewHierarchy(into, c.getComponent(i), seen, predicate);
			}
		}
		if (from instanceof java.awt.Component) {
			java.awt.Component c = (java.awt.Component) from;
			allViewHierarchy(into, c.getParent(), seen, predicate);
		}
	}

	public Rect findMatchingMinimal() {
		// search entire view hierarchy for the
		// minimaltextfield with the right string

		List<MinimalLazyBox> r = new ArrayList<MinimalLazyBox>();
		allViewHierarchy((List) r, this, new HashSet<Object>(), new iFunction<Boolean, Object>() {

			public Boolean f(Object in) {
				return (in instanceof MinimalLazyBox);
			}
		});

		float d = Float.POSITIVE_INFINITY;
		for (int i = 0; i < r.size(); i++) {
			if (r.get(i) != this) {
				if (r.get(i).getText().equals("\\" + this.getText())) {
					return r.get(i).realBounds();
				}
			}
		}
		return null;
	}

	@Override
	public float getAlignmentY() {
		return 0.75f;
	}

	@Override
	public Insets getInsets() {
		return new Insets(0, 2, 0, 2);
	}

	@Override
	public Dimension getMaximumSize() {
		return new Dimension(computeWidth(), height);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(computeWidth(), height);
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension p = super.getPreferredSize();
		p.width = computeWidth();
		return new Dimension(p.width, height);
	}

	public Object getTransformerBody() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
	}

	public void paintOutOfBand(GC g, StyledText inside) {

		// {
		// if (!textAtLastUpdate.equals(getText())) {
		// Rect b = realBounds();
		//
		// g2 = (Graphics2D) g2.create();
		// g2.setFont(new Font(Constants.defaultFont, Font.PLAIN,
		// Constants.defaultFont_editorSize*2/3));
		// g2.setColor(new Color(1,1,1,0.5f));
		// g2.rotate(-Math.PI/6, (float)(b.x+b.w), (float)(b.y));
		//
		//
		// g2.drawString("was " + textAtLastUpdate, (float) (b.x + b.w),
		// (float) (b.y));
		// }
		// }
	}

	public Rect realBounds() {
		Point q = this.getParent().getLocation();
		return new Rect(q.x, q.y, this.getBounds().width, this.getBounds().height);
	}

	@Override
	public void setBackground(Color bg) {
		this.background = bg;
		super.setBackground(bg);
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
	}

	@Override
	public void setBounds(Rectangle arg0) {
		arg0.width = computeWidth();
		super.setBounds(arg0);
	}

	public void setCaption(String string) {
		this.caption = string;
	}

	@Override
	public void setSize(Dimension d) {
		d.width = computeWidth();
		super.setSize(d);
	}

	@Override
	public void setSize(int width, int height) {
		width = computeWidth();
		super.setSize(width, height);
	}

	@Override
	public void setText(String t) {
		super.setText(t);
	}

	private int computeWidth() {
		String t = super.getText();
		int w = getFontMetrics(getFont()).charsWidth(t.toCharArray(), 0, t.toCharArray().length) + 5;
		return w;
	}

	@Override
	protected void fireActionPerformed() {
		textAtLastUpdate = getText();
		super.fireActionPerformed();
	}

	@Override
	protected void paintBorder(Graphics g) {
		this.setBackground(new Color(0, 0, 0, 0f));
		arm = !getText().equals(textAtLastUpdate);

		this.setForeground(new Color(1 * (arm ? 0.5f : 1), 1 * (arm ? 0.0f : 1), 1 * (arm ? 0.0f : 1), 1f));
		Graphics2D g2 = (Graphics2D) g;
		{
			GeneralPath path = new GeneralPath();
			Rectangle bounds = this.getBounds();
			path.moveTo(0, 0);
			path.lineTo(bounds.width - 1, 0);
			path.lineTo(bounds.width - 1, bounds.height - 1);
			path.lineTo(0, bounds.height - 1);
			path.lineTo(0, 0);
			g2.setPaint(new GradientPaint(0, 0, new Color(1, 1, 1, 0.1f), 0, height, new Color(0, 0, 0, 0.1f)));
			g2.fill(path);

			g2.setStroke(new BasicStroke(1, 1, 1, 1, new float[] { 3, 3 }, 0));
			g2.setColor(new Color(0.3f * (arm ? 3 : 0), 0.0f, 0.0f, 0.35f));
			paintBorderPath(g2, path);
			// g2.setStroke(new BasicStroke(3));
			// g2.setColor(new Color(0.3f * (arm ? 3 : 0), 0.0f,
			// 0.0f, 0.05f));
			// paintBorderPath(g2, path);

			int width = g2.getFontMetrics().charsWidth(caption.toCharArray(), 0, caption.toCharArray().length);

			g2.drawString(caption, (bounds.width - 5 - width), 12);
		}

	}

	@Override
	protected void paintComponent(Graphics g) {
		paintBorder(g);
		super.paintComponent(g);
	}

	protected void paintBorderPath(Graphics2D g2, GeneralPath path) {
		g2.draw(path);
	}

	@Override
	protected void processKeyEvent(KeyEvent e) {

		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			setText(textAtLastUpdate);
		}

		super.processKeyEvent(e);

		super.getParent().validate();
		super.getParent().repaint();
		super.revalidate();

		List<JEditorPane> r = new ArrayList<JEditorPane>();
		allViewHierarchy((List) r, this, new HashSet<Object>(), new iFunction<Boolean, Object>() {
			public Boolean f(Object in) {
				return (in instanceof JEditorPane);
			}
		});
		if (r.size() > 0) {
			JEditorPane inside = r.get(0);
			try {
				ReflectionTools.findFirstMethodCalled(inside.getClass(), "dirtyText").invoke(inside);
			} catch (IllegalArgumentException e1) {
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			} catch (InvocationTargetException e1) {
				e1.printStackTrace();
			}
		}

		// fireActionPerformed();
	}

	@Override
	protected void processMouseEvent(MouseEvent e) {
		super.processMouseEvent(e);

		if (e.getClickCount() == 1 && !Platform.isPopupTrigger(e) && e.getID() == MouseEvent.MOUSE_PRESSED) {
			org.eclipse.swt.graphics.Point loc = Launcher.display.getCursorLocation();

			final Nub inside = CustomInsertDrawing.currentNub;
			new PopupTextBox.Modal().getString(new java.awt.Point(loc.x, loc.y), "Contents: ", getText(), new iAcceptor<String>() {

				@Override
				public iAcceptor<String> set(String to) {

					MinimalLazyBox.this.setText(to);
					if (inside != null) {

						System.out.println(" forcing update of all styles now ");

						try {

							inside.updateAllStylesNow();
							inside.canvas.redraw();
						} catch (SWTException e) {
						}
					}
					MinimalLazyBox.this.repaint();

					// hack to get on canvas boxes to redraw
					MinimalLazyBox.this.setSize(MinimalLazyBox.this.getSize());
					return this;
				}
			});

		}

		// if ((e.getModifiers() & Event.ALT_MASK) != 0 && e.getID() ==
		// MouseEvent.MOUSE_RELEASED) {
		// arm = false;
		// repaint();
		// List<JEditorPane> r = new ArrayList<JEditorPane>();
		// allViewHierarchy((List) r, this, new HashSet<Object>(), new
		// iFunction<Boolean, Object>() {
		// public Boolean f(Object in) {
		// return (in instanceof JEditorPane);
		// }
		// });
		// if (r.size() > 0) {
		// JEditorPane inside = r.get(0);
		// try {
		// ReflectionTools.findFirstMethodCalled(inside.getClass(),
		// "execSpecial").invoke(inside, getText());
		// } catch (IllegalArgumentException e1) {
		// e1.printStackTrace();
		// } catch (IllegalAccessException e1) {
		// e1.printStackTrace();
		// } catch (InvocationTargetException e1) {
		// e1.printStackTrace();
		// }
		// }
		// } else if ((e.getModifiers() & Event.ALT_MASK) != 0 &&
		// e.getID() == MouseEvent.MOUSE_PRESSED) {
		// arm = true;
		// repaint();
		// }
	}

	public void expandDamage(Rect d) {
	}
}
