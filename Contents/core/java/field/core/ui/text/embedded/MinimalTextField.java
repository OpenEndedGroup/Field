package field.core.ui.text.embedded;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
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
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Control;

import field.core.Constants;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.ui.text.embedded.CustomInsertDrawing.Nub;
import field.core.ui.text.embedded.CustomInsertDrawing.iAcceptsInsertRenderingContext;
import field.core.ui.text.embedded.CustomInsertDrawing.iInsertRenderingContext;
import field.core.ui.text.embedded.CustomInsertSystem.ProvidedComponent;
import field.launch.Launcher;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.ReflectionTools;

public class MinimalTextField extends JTextField implements iOutOfBandDrawing, iAcceptsInsertRenderingContext {
	static public class Component extends ProvidedComponent {
		protected String valueString = "";

		@Override
		public void deserialize(iVisualElement inside) {
			component = new MinimalTextField() {
				@Override
				public void setText(String to) {
					super.setText(to);
					Component.this.valueString = to;
				};

				@Override
				protected boolean isBottom() {
					return getText().startsWith("\\");
				}
			};

			((MinimalTextField) component).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Component.this.valueString = ((MinimalTextField) component).getText();
				}
			});
			((MinimalTextField) component).setText(valueString);

		}

		@Override
		public String getCurrentRepresentedString() {
			return "#--{" + valueString + "}";
		}

		@Override
		public void preserialize() {
		}

		protected void updateValue() {
		}
	}

	static public class Component_blockStart extends ProvidedComponent {
		protected String valueString = "";

		@Override
		public void deserialize(iVisualElement inside) {
			component = new MinimalTextField() {
				@Override
				public void setText(String to) {
					super.setText(to);
					Component_blockStart.this.valueString = to;
				};
			};

			((MinimalTextField) component).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Component_blockStart.this.valueString = ((MinimalTextField) component).getText();
				}
			});

			((MinimalTextField) component).setText(valueString);
			((MinimalTextField) component).setCaption("(freeze block)");

		}

		@Override
		public String getCurrentRepresentedString() {

			// return "#--{" + valueString +
			// "}";

			return "\n" + "if (not _a.__localFreeze_):\n" + "	_a.__localFreeze_ = FreezeProperties()\n" + "	FreezeProperties.standardCloneHelpers(_a.__localFreeze_)\n" + "\n" + "_a.__localFreeze_" + name() + " = Freeze(_a.__localFreeze_).freeze(_self)\n" + "\n" + "#--{" + valueString + "}\n" + "\n" + "_a.__localFreeze_" + name() + ".thaw(_self)";

		}

		public String name() {
			String[] q = valueString.split(" ");
			if (q.length == 0)
				return "untitled";
			String m = q[q.length - 1];
			if (m.length() == 0)
				return "unititled";
			if (!Character.isJavaIdentifierStart(m.charAt(0))) {
				m = "_" + m;
			}
			StringBuffer cm = new StringBuffer(m);
			for (int i = 0; i < m.length(); i++) {
				if (!Character.isJavaIdentifierPart(m.charAt(i))) {
					cm.setCharAt(i, '_');
				}
			}
			return cm.toString();
		}

		@Override
		public void preserialize() {
		}

		protected void updateValue() {
		}
	}

	private String caption = "";

	int height = 20;

	Color background = new Color(0.1f, 0, 0, 0.3f);

	boolean arm = false;

	boolean centered = false;

	public MinimalTextField() {
		this.setFont(new Font(Constants.defaultFont, Font.BOLD | Font.ITALIC, 11));
		this.setHorizontalAlignment(JTextField.LEFT);
		this.setForeground(new Color(0, 0, 0, 1f));
		this.setCaretColor(new Color(1, 0, 0, 1f));
		this.putClientProperty("caretWidth", 3);
	}

	public MinimalTextField(String label) {
		this.setFont(new Font(Constants.defaultFont, Font.BOLD | Font.ITALIC, 11));
		this.setText(label);
		this.setHorizontalAlignment(JTextField.LEFT);
		this.setForeground(new Color(0, 0, 0, 0.5f));
		this.putClientProperty("caretWidth", 3);
	}

	public void allViewHierarchy(List<Object> into, MinimalTextField from, HashSet<Object> seen, iFunction<Boolean, Object> predicate) {

		System.out.println(" all view hierarchy <" + from.insertRenderingContext + ">");

		if (from.insertRenderingContext == null)
			return;

		StyledText tex = from.insertRenderingContext.getText();
		Control[] controls = tex.getChildren();

		for (Control cc : controls) {

			if (cc.getData() != null && (predicate == null || predicate.f(cc))) {
				into.add(cc);
			}
		}

		System.out.println(" returning <" + into + ">");
	}

	public Rect findMatchingMinimal() {
		// search entire view hierarchy for the
		// minimaltextfield with the right string

		// List<MinimalTextField> r = new ArrayList<MinimalTextField>();
		// allViewHierarchy((List) r, this, new HashSet<Object>(), new
		// iFunction<Boolean, Object>() {
		//
		// public Boolean f(Object in) {
		// return (in instanceof MinimalTextField);
		// }
		// });
		//
		// float d = Float.POSITIVE_INFINITY;
		// for (int i = 0; i < r.size(); i++) {
		// if (r.get(i) != this) {
		// if (r.get(i).getText().equals("\\" + this.getText())) {
		// return r.get(i).realBounds();
		// }
		// }
		// }
		//
		// return null;

		MinimalTextField c = findMatchingComponent(true);
		if (c == null)
			return null;

		return c.realBounds();
	}

	public int getRealWidth() {
		return getWidth();
	}

	public int getPositionFor(boolean startings) {
		return 0;
	}

	public MinimalTextField findMatchingComponent(boolean down) {
		try {
			Rect here = this.realBounds();

			List<Control> r = new ArrayList<Control>();
			allViewHierarchy((List) r, this, new HashSet<Object>(), new iFunction<Boolean, Object>() {
				public Boolean f(Object in) {

					if (!(in instanceof Control))
						return false;

					Nub nub = (Nub) ((Control) in).getData();

					if (nub == null)
						return false;

					return nub.component instanceof MinimalTextField;
				}
			});

			if (!down) {
				Collections.reverse(r);
			}

			String searchtext = this.getText();
			Pattern p = Pattern.compile(searchtext);

			float d = Float.POSITIVE_INFINITY;
			for (int i = 0; i < r.size(); i++) {

				Control c = r.get(i);
				JComponent compx = ((Nub) c.getData()).component;

				System.out.println(" compx is <" + compx + ">");

				if (compx instanceof MinimalTextField) {
					MinimalTextField comp = (MinimalTextField) compx;

					System.out.println(" checking text <" + comp.getText() + "> <" + down + " " + comp.realBounds().y + " " + here.y + ">");

					if (comp != this && ((down && comp.realBounds().y > here.y) || (!down && comp.realBounds().y < here.y))) {
						if (p.matcher(comp.getText()).find()) {
							return comp;
						}
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public float getAlignmentY() {
		return 0.75f;
	}

	@Override
	public Insets getInsets() {
		return new Insets(0, 5, 0, 0);
	}

	@Override
	public Border getBorder() {
		return new Border() {

			public Insets getBorderInsets(java.awt.Component c) {
				return new Insets(0, 5, 0, 0);
			}

			public boolean isBorderOpaque() {
				return false;
			}

			public void paintBorder(java.awt.Component c, Graphics g, int x, int y, int width, int height) {
				MinimalTextField.this.paintBorder(g);
			}
		};
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

	public void paintOutOfBand(GC g, StyledText inside) {

		System.out.println(" paint OOB ");

		Rect r = findMatchingMinimal();

		System.out.println(" matching minimal is <" + r + ">");

		if (r == null)
			return;

		Rect m = realBounds();
		if (r.y > m.y) {
			g.setAdvanced(true);
			g.setAlpha(64);
			g.setLineDash(new int[] { 5, 5 });
			g.setForeground(new org.eclipse.swt.graphics.Color(Launcher.display, 0, 0, 0));
			g.setBackground(new org.eclipse.swt.graphics.Color(Launcher.display, 0, 0, 0));
			g.drawLine((int) getRealWidth()/2, (int) (m.y + m.h), (int) getRealWidth()/2, (int) (r.y));
			int w = 5;
			g.fillRectangle((int) getRealWidth() - w / 2, (int) (m.y + m.h) - w / 2, w, w);
			g.fillRectangle((int) getRealWidth() - w / 2, (int) (r.y) - w / 2, w, w);
		}

		// Graphics2D g2 = g;
		// {
		// // Point loc =
		// // this.getLocation();
		// Rect loc = realBounds();
		// Rect lower = findMatchingMinimal();
		//
		// boolean missing = false;
		//
		// if (lower == null && !this.getText().startsWith("\\")) {
		// lower = new Rect(loc.x, inside.getBounds().height, loc.w,
		// loc.y);
		// missing = true;
		// }
		// if (lower != null) {
		// paintSpace(g2, loc, lower, missing);
		// }
		//
		// }
	}

	public Rect realBounds() {

		System.out.println(" inside realbounds ");

		if (insertRenderingContext == null)
			return new Rect(0, 0, this.getWidth(), this.getHeight());
		else {

			org.eclipse.swt.graphics.Rectangle b = insertRenderingContext.getControl().getBounds();
			System.out.println(" control is :" + b);

			return new Rect(b.x, b.y, b.width, b.height);
		}
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

	public void setCaption(String string) {
		this.caption = string;
	}

	@Override
	public void setText(String t) {
		super.setText(t);

	}

	int indent = 0;
	float indentWidth = 12;

	public iInsertRenderingContext insertRenderingContext;

	protected boolean isBottom() {
		return false;
	}

	@Override
	protected void paintComponent(Graphics g) {

		Rectangle bounds = this.getBounds();

		g.setColor(new Color(0.5f, 0.5f, 0.5f, 1f));
		g.fillRect(0, 0, bounds.width, bounds.height);

		this.setBackground(new Color(0.5f, 0.5f, 0.5f, 1f));
		super.paintComponent(g);

		// bounds.width = getRealWidth();

		Graphics2D g2 = (Graphics2D) g;

		GeneralPath path = new GeneralPath();
		path.moveTo(indent * indentWidth, 0);
		path.lineTo(bounds.width - 1, 0);
		path.lineTo(bounds.width - 1, bounds.height - 1);
		path.lineTo(indent * indentWidth, bounds.height - 1);
		path.lineTo(indent * indentWidth, 0);
		
//		java.awt.geom.Arc2D.Float f = new Arc2D.Float(bounds.width-1-bounds.height,0,0,bounds.height, bounds.height,0, 90, Arc2D.OPEN);
//		GeneralPath p2 = new GeneralPath();
//		p2.append(f, false);
//		p2.moveTo()
		
		if (arm) {
			g2.setColor(new Color(0.22f, 0.22f, 0.2f, 0.1f));
			if (!isBottom())
				g2.setPaint(new GradientPaint(0, 0, new Color(0.22f, 0.22f, 0.22f, 0.1f), 0, bounds.height - 1, new Color(0.22f, 0.22f, 0.22f, 0f)));
			else
				g2.setPaint(new GradientPaint(0, 0, new Color(0.22f, 0.22f, 0.22f, 0.0f), 0, bounds.height - 1, new Color(0.22f, 0.22f, 0.22f, 0.1f)));
			g2.fill(path);
		} else {
			g2.setColor(new Color(0.62f, 0.62f, 0.62f, 0.5f));
			if (!isBottom())
				g2.setPaint(new GradientPaint(0, 0, new Color(0.62f, 0.62f, 0.62f, 0.1f), 0, bounds.height - 1, new Color(0.62f, 0.62f, 0.62f, 0f)));
			else
				g2.setPaint(new GradientPaint(0, 0, new Color(0.62f, 0.62f, 0.62f, 0.0f), 0, bounds.height - 1, new Color(0.62f, 0.62f, 0.62f, 0.1f)));

			g2.fill(path);
		}
		// g2.setColor(new Color(0.22f, 0.22f, 0.2f, 0.4f));
		if (!isBottom())
			g2.setPaint(new GradientPaint(0, 0, new Color(1,1,1,0.1f), 0, bounds.height, new Color(0,0,0,0.1f)));
//			g2.setPaint(new GradientPaint(0, 0, new Color(0.22f, 0.22f, 0.2f, 0.4f), 0, bounds.height - 1, new Color(0.22f, 0.22f, 0.2f, 0.4f)));
		else
			g2.setPaint(new GradientPaint(0, 0, new Color(1,1,1,0.1f), 0, bounds.height, new Color(0,0,0,0.1f)));
//			g2.setPaint(new GradientPaint(0, 0, new Color(0.22f, 0.22f, 0.2f, 0.4f), 0, bounds.height - 1, new Color(0.22f, 0.22f, 0.2f, 0.4f)));
		

		g2.fill(path);
		g2.setPaint(new Color(0,0,0,0.65f));
		g2.setStroke(new BasicStroke(1));
		g2.draw(path);

		g2.setFont(new Font(field.core.Constants.defaultFont, Font.BOLD, 12));
		int width = g2.getFontMetrics().charsWidth(caption.toCharArray(), 0, caption.toCharArray().length);
		g2.setColor(new Color(0f, 0f, 0f, 0.8f));

		if (getText().trim().length() == 0) {
			g2.setFont(new Font(field.core.Constants.defaultFont, Font.BOLD, 8));
			g2.drawString("Empty text block, click to edit", 3, height - 5);
		}

	}

	@Override
	protected void paintBorder(Graphics g) {
		this.setBackground(background);
		this.setForeground(new Color(0, 0, 0, 0.7f));
		Rectangle bounds = this.getBounds();
		bounds.width = getRealWidth();

		Rect r = findMatchingMinimal();
		if (r != null)
			bounds.height = (int) r.h;

		Graphics2D g2 = (Graphics2D) g;
		{
			GeneralPath path = new GeneralPath();
			path.moveTo(0, 0);
			path.lineTo(bounds.width - 1, 0);
			path.lineTo(bounds.width - 1, bounds.height - 1);
			path.lineTo(0, bounds.height - 1);
			path.lineTo(0, 0);
			g2.setColor(new Color(0.3f * (arm ? 3 : 1), 0.33f, 0.33f, 0.7f));
			g2.setStroke(new BasicStroke(2));
			g2.draw(path);
			paintBorderPath(g2, path);

			int width = g2.getFontMetrics().charsWidth(caption.toCharArray(), 0, caption.toCharArray().length);

			g2.setColor(new Color(0.3f * (arm ? 3 : 1), 0.33f, 0.33f, 0.7f));
			g2.setFont(this.getFont());
			// g2.drawString(caption, (bounds.width - 5 - width),
			// 14);
		}
	}

	protected void paintBorderPath(Graphics2D g2, GeneralPath path) {
		if (true)
			return;

		g2.setColor(new Color(0.5f, 0.5f, 0.5f, 0.5f));
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
		g2.fill(path);
	}

	@Override
	public void repaint() {
		super.repaint();
		Rect loc = realBounds();
		Rect lower = findMatchingMinimal();
		Container parent = enclosingPane();

		if (loc == null)
			return;
		if (parent == null)
			return;

		if (lower == null) {
			parent.repaint(0, 0, 4096, 4096);
			return;
		}

		parent.repaint((int) loc.x, (int) loc.y, (int) (loc.w + 100), (int) (lower.y + lower.h));
	}

	protected void paintSpace(Graphics2D g2, Rect loc, Rect lower, boolean missing) {
		g2.setPaint(new GradientPaint(new Point2D.Double(loc.x, loc.y), new Color(1, 1, 1, 0.1f), new Point2D.Double(lower.x, lower.y), new Color(0, 0, 0, 0.1f)));

		loc.w = getRealWidth();

		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

		{
			GeneralPath path = new GeneralPath();
			Rectangle bounds = this.getBounds();
			path.moveTo((float) (loc.x), (float) (loc.y + 1));
			path.lineTo((float) (loc.x + loc.w), (float) (loc.y + 1));
			path.lineTo((float) (loc.x + loc.w), (float) (lower.y + lower.h));
			path.lineTo((float) (loc.x), (float) (lower.y + lower.h));
			path.lineTo((float) (loc.x), (float) (loc.y + 1));
			g2.fill(path);
		}
	}

	@Override
	protected void processKeyEvent(KeyEvent e) {
		super.processKeyEvent(e);
		fireActionPerformed();
	}

	public JEditorPane enclosingPane() {
		List<JEditorPane> r = new ArrayList<JEditorPane>();
		allViewHierarchy((List) r, this, new HashSet<Object>(), new iFunction<Boolean, Object>() {
			public Boolean f(Object in) {
				return (in instanceof JEditorPane);
			}
		});
		if (r.size() > 0) {
			JEditorPane inside = r.get(0);
			return inside;
		}
		return null;
	}

	@Override
	protected void processMouseEvent(MouseEvent e) {
		super.processMouseEvent(e);
		if ((e.getModifiers() & Event.ALT_MASK) != 0 && e.getID() == MouseEvent.MOUSE_RELEASED) {
			arm = false;
			repaint();
			List<JEditorPane> r = new ArrayList<JEditorPane>();
			allViewHierarchy((List) r, this, new HashSet<Object>(), new iFunction<Boolean, Object>() {
				public Boolean f(Object in) {
					return (in instanceof JEditorPane);
				}
			});
			if (r.size() > 0) {
				JEditorPane inside = r.get(0);
				try {
					ReflectionTools.findFirstMethodCalled(inside.getClass(), "execSpecial").invoke(inside, getText());
				} catch (IllegalArgumentException e1) {
					e1.printStackTrace();
				} catch (IllegalAccessException e1) {
					e1.printStackTrace();
				} catch (InvocationTargetException e1) {
					e1.printStackTrace();
				}
			}
		} else if ((e.getModifiers() & Event.ALT_MASK) != 0 && e.getID() == MouseEvent.MOUSE_PRESSED) {
			arm = true;
			repaint();
		}
	}

	public void expandDamage(Rect d) {

		Rect lower = findMatchingMinimal();
		Rectangle bounds = this.getBounds();
if (lower==null) return;

//		if (lower == null) {
//			d.x = 0;
//			d.y = 0;
//			d.w = 4000;
//			d.h = 4000;
//			return;
//		}

		
		
		if (d.y >= Math.min(lower.y, bounds.y) && d.y <= Math.max(lower.y + lower.h, bounds.y + bounds.height)) 
		{
			d.y = Math.min(lower.y, bounds.y-50);
			d.h = Math.max(lower.y + lower.h+50, bounds.y + bounds.height+50) - d.y;
			d.w += d.x+50;
			d.x = 0;
		}

	}

	public void setInsertRenderingContext(iInsertRenderingContext context) {

		this.insertRenderingContext = context;

	}

}
