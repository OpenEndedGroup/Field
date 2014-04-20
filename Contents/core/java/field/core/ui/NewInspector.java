package field.core.ui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.text.NumberFormat;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;

import field.core.Constants;
import field.core.plugins.python.Action;
import field.core.ui.TristateCheckBox.TristateState;
import field.core.ui.text.embedded.CustomInsertDrawing;
import field.core.windowing.GLComponentWindow;
import field.math.linalg.Vector4;

/**
 * a rewrite of the inspector system
 * 
 * @author marc
 * 
 */
public class NewInspector {

	public enum Status {
		valid, unset, mixed;
	}

	static public abstract class Inspected<T, C extends JComponent> {
		public String name;

		public C editor;

		abstract public T getValue();

		abstract public void setValue(T s);

		abstract public Status getStatus();

	}

	private JFrame frame;
	private JPanel contents;
	private JButton actions;
	private JScrollPane scroller;
	private JPopupMenu menu;
	String waterText = "Properties";


	public void setWaterText(String waterText) {
		this.waterText = waterText;
	}


	protected JButton getActions() {
		if (actions == null) {
			actions = new JButton();
			actions.setPreferredSize(new Dimension(24, 24));
			actions.setMaximumSize(new Dimension(24, 24));
			actions.setMinimumSize(new Dimension(24, 24));
			actions.setIcon(new ImageIcon("content/icons/Gear.png"));
			actions.setIconTextGap(0);
			actions.putClientProperty("Quaqua.Button.style", "square");
			actions.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JPopupMenu menu = getMenu();
					if (menu != null)
						menu.show(actions, 20, 10);
				}
			});

			actions.setOpaque(false);
			actions.setFocusable(false);
		}

		return actions;
	}

	protected JPopupMenu getMenu() {
		return menu;
	}

	public void setMenu(JPopupMenu menu) {
		this.menu = menu;
	}

	public Inspected<String, TextControl> debugTextControl(String name, final String s) {
		Inspected<String, TextControl> i = new Inspected<String, TextControl>() {

			@Override
			public String getValue() {
				return s;
			}

			@Override
			public void setValue(String s) {
				;//System.out.println(" set string to <" + s + ">");
			}

			@Override
			public Status getStatus() {
				return Status.valid;
			}

		};
		i.name = name;
		i.editor = new TextControl(i);

		return i;
	}

	public Inspected<Integer, IntControl> debugIntControl(String name, final Integer s) {
		Inspected<Integer, IntControl> i = new Inspected<Integer, IntControl>() {

			@Override
			public Integer getValue() {
				return s;
			}

			@Override
			public void setValue(Integer s) {
				;//System.out.println(" set value to <" + s + ">");
			}

			@Override
			public Status getStatus() {
				return Status.mixed;
			}

		};
		i.name = name;
		i.editor = new IntControl(i);

		return i;
	}

	public Inspected debugColorControl(String name, final Vector4 s) {
		Inspected<Vector4, ColorControl> i = new Inspected<Vector4, ColorControl>() {

			@Override
			public Vector4 getValue() {
				return s;
			}

			@Override
			public void setValue(Vector4 s2) {
				s.setValue(s2);
			}

			@Override
			public Status getStatus() {
				return Status.mixed;
			}

		};
		i.name = name;
		i.editor = new ColorControl(i);

		return i;
	}

	public Inspected debugBooleanControl(String name, final Boolean s) {
		Inspected<Boolean, BooleanControl> i = new Inspected<Boolean, BooleanControl>() {

			@Override
			public Boolean getValue() {
				return s;
			}

			@Override
			public void setValue(Boolean s2) {
			}

			@Override
			public Status getStatus() {
				return Status.mixed;
			}

		};
		i.name = name;
		i.editor = new BooleanControl(i);

		return i;
	}

	static public class TextControl extends JComponent implements ActionListener, SetValueForControl<String, TextControl> {

		private JTextField textField;
		private Inspected<String, TextControl> i;

		public TextControl(Inspected<String, TextControl> i) {
			this.i = i;
			setLayout(new BorderLayout(0, 0));
			textField = new JTextField() {
				protected void processKeyEvent(java.awt.event.KeyEvent e) {
					super.processKeyEvent(e);
					actionPerformed(null);
				}
			};
			textField.putClientProperty("JComponent.sizeVariant", "small");
			textField.setFont(new Font(Constants.defaultFont, 0, 10));
			textField.addActionListener(this);
			add(textField, BorderLayout.CENTER);
			setValueForControl(i);
		}

		public void setValueForControl(Inspected<String, TextControl> ii) {
			this.i = ii;
			if (i.getStatus() == Status.valid) {
				if (!textField.getText().equals(i.getValue())) {
					textField.setText(i.getValue());
				}
			} else if (i.getStatus() == Status.mixed) {
				if (!textField.getText().equals("\u2014"))
					textField.setText("\u2014");
			} else if (i.getStatus() == Status.unset)
				if (!textField.getText().equals(" "))
					textField.setText(" ");
		}

		public void actionPerformed(ActionEvent e) {
			i.setValue(textField.getText());
		}

		@Override
		public void paint(Graphics g) {
			setValueForControl(i);
			super.paint(g);
		}
	}

	static public class BooleanControl extends JComponent implements ActionListener, SetValueForControl<Boolean, BooleanControl> {

		private TristateCheckBox textField;
		private Inspected<Boolean, BooleanControl> i;

		public BooleanControl(Inspected<Boolean, BooleanControl> i) {
			this.i = i;
			setLayout(new BorderLayout(0, 0));
			textField = new TristateCheckBox("");
			textField.putClientProperty("JComponent.sizeVariant", "small");
			textField.setFont(new Font(Constants.defaultFont, 0, 10));
			textField.addActionListener(this);
			add(textField, BorderLayout.CENTER);
			setValueForControl(i);
		}

		public void setValueForControl(Inspected<Boolean, BooleanControl> ii) {
			this.i = ii;
			if (i.getStatus() == Status.valid) {
				Object value = i.getValue();
				if (value instanceof Number)
					value = ((Number) value).intValue() != 0;
				if (textField.isSelected() != ((Boolean) value))
					textField.setSelected((Boolean) value);
			} else if (i.getStatus() == Status.mixed) {
				if (!textField.isIndeterminate())
					textField.setIndeterminate();
			} else if (i.getStatus() == Status.unset)
				if (!textField.isIndeterminate())
					textField.setIndeterminate();
		}

		public void actionPerformed(ActionEvent e) {
			TristateState s = textField.getState();
			i.setValue(s == s.SELECTED);
		}

		@Override
		public void paint(Graphics g) {
			setValueForControl(i);
			super.paint(g);
		}

	}

	static public class ActionControl extends JComponent implements ActionListener, SetValueForControl<Action, ActionControl> {

		private Inspected<Action, ActionControl> i;
		private JButton textField;

		public ActionControl(Inspected<Action, ActionControl> i) {
			this.i = i;
			setLayout(new BorderLayout(0, 0));
			textField = new JButton("Perform...");
			textField.putClientProperty("Quaqua.Button.style", "square");
			textField.putClientProperty("JComponent.sizeVariant", "small");
			textField.setFont(new Font(Constants.defaultFont, Font.BOLD, 10));
			// textField.addActionListener(this);
			add(textField, BorderLayout.CENTER);
			setValueForControl(i);

			textField.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					;//System.out.println(" mouse clicked in action control");
					super.mouseClicked(e);
					actionPerformed(null);
				}
			});
		};

		public void setValueForControl(Inspected<Action, ActionControl> ii) {
			this.i = ii;
		}

		public void actionPerformed(ActionEvent e) {

			;//System.out.println(" action performed here <" + e + ">");

			i.getValue().apply();
		}

	}

	static public class InfoControl extends JComponent {

		private JLabel textField;

		public InfoControl(Inspected<String, InfoControl> i) {
			setLayout(new BorderLayout(0, 0));
			textField = new JLabel("");
			textField.putClientProperty("JComponent.sizeVariant", "small");
			textField.setFont(new Font(Constants.defaultFont, 1, 10));
			textField.setText(i.getValue());
			add(textField, BorderLayout.CENTER);
		}

	}

	static public class ColorControl extends JComponent implements ActionListener {

		private JComponent textField;
		private final Inspected i;

		public ColorControl(final Inspected<Vector4, ColorControl> i) {
			this.i = i;
			FlowLayout fl = new FlowLayout(FlowLayout.LEADING, 0, 5);
			setLayout(fl);
			textField = new JComponent() {
				@Override
				protected void paintComponent(Graphics g) {

					g.translate(5, 5);
					
					enableEvents(AWTEvent.MOUSE_EVENT_MASK);
					if (i.getStatus() == Status.unset) {
						g.setColor(new Color(0, 0, 0, 0.1f));
						g.drawRect(0, 0, this.getWidth(), this.getHeight());
						return;
					}

					Vector4 color = i.getValue();
					Rectangle bounds = this.getBounds();

					Graphics2D g2 = (Graphics2D) g;

					int dd = 10;
					for (int x = 0; x < 1 + this.getWidth() / dd; x++) {
						for (int y = 0; y < 1 + this.getHeight() / dd; y++) {
							g.setColor((x + y) % 2 == 0 ? new Color(0.35f, 0.35f, 0.35f, 1f) : new Color(0.65f, 0.65f, 0.65f, 1f));
							g.fillRect(x * dd, y * dd, dd, dd);
						}
					}
					if (i.getStatus() == Status.mixed) {

					} else {

						g2.setColor(new Color(color.x, color.y, color.z, 1));
						GeneralPath upper = new GeneralPath();
						upper.moveTo(0, 0);
						upper.lineTo(bounds.width, 0);
						upper.lineTo(0, bounds.height);
						upper.lineTo(0, 0);
						g2.fill(upper);

						g2.setColor(new Color(color.x, color.y, color.z, color.w));
						GeneralPath lower = new GeneralPath();
						lower.moveTo(bounds.width, 0);
						lower.lineTo(bounds.width, bounds.height);
						lower.lineTo(0, bounds.height);
						lower.lineTo(bounds.width, 0);
						g2.fill(lower);
					}

				}

				@Override
				protected void processMouseEvent(MouseEvent e) {
					;//System.out.println(" mouse event should precipitate a cd :"+e);
					if (e.getID() == e.MOUSE_PRESSED && e.getButton() == 1) {

						Point x = e.getLocationOnScreen();
						
//						Point x = new Point(20, 20);
//						SwingUtilities.convertPointToScreen(x, this);
						Vector4 color = i.getValue();
						if (color == null)
							color = new Vector4(0.5, 0.5, 0.5, 0);
						
						
						;//System.out.println(" here we go for a color dialog ....... ");

						org.eclipse.swt.widgets.ColorDialog d = new org.eclipse.swt.widgets.ColorDialog(GLComponentWindow.getCurrentWindow(null).getFrame(), SWT.PRIMARY_MODAL);
						d.setRGB(new RGB((int) (Math.max(0, Math.min(255, 255 * color.x))), (int) (Math.max(0, Math.min(255, 255 * color.y))), (int) (Math.max(0, Math.min(255, 255 * color.z)))));
//						d.setAlpha((int) (color.w * 255f));
						d.open();
						RGB rgb = d.getRGB();

						i.setValue(new Vector4(rgb.red/255f, rgb.green/255f, rgb.blue/255, 1/*d.getAlpha()/255f*/));
						
//						new ColorDialog(new Color(color.x, color.y, color.z, color.w), (Frame) SwingUtilities.getWindowAncestor(this), x) {
//							public void setColor(Color color) {
//								super.setColor(color);
//								i.setValue(new Vector4(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f));
//							};
//						};
					}
				}

			};
			textField.setPreferredSize(new Dimension(16, 16));
			textField.setMaximumSize(new Dimension(16, 16));
			textField.putClientProperty("JComponent.sizeVariant", "small");
			textField.setFont(new Font(Constants.defaultFont, 0, 10));
			add(textField);
			enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);

		}

		@Override
		public void actionPerformed(ActionEvent e) {
		}

	}

	public interface SetValueForControl<T, C extends JComponent> {
		public void setValueForControl(Inspected<T, C> i);
	}

	static public class IntControl extends JComponent implements ChangeListener, SetValueForControl<Integer, IntControl> {

		private DraggableNumber textField;
		private Inspected<Integer, IntControl> i;

		public IntControl(Inspected<Integer, IntControl> i) {
			this.i = i;
			FlowLayout fl = new FlowLayout(FlowLayout.LEADING, 0, 0);
			setLayout(fl);
			textField = new DraggableNumber();
			textField.putClientProperty("JComponent.sizeVariant", "small");
			textField.setFont(new Font(Constants.defaultFont, 0, 10));
			textField.addChangeListener(this);
			add(textField);

			NumberFormat nf = NumberFormat.getNumberInstance();
			nf.setMaximumFractionDigits(0);
			nf.setMinimumFractionDigits(0);
			textField.setNumberFormat(nf);
			setValueForControl(i);
			enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
		}

		public void setValueForControl(Inspected<Integer, IntControl> i) {
			this.i = i;
			Integer vv = i.getValue();
			if (vv == null)
				vv = 0;
			if (i.getStatus() == Status.valid) {
				if (textField.getValue() != vv)
					textField.setValue(vv);
			} else if (i.getStatus() == Status.mixed) {
				textField.setStatusMixed(vv);
			} else if (i.getStatus() == Status.unset) {
				textField.setStatusUnset(vv);
			}
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			i.setValue((int) Float.parseFloat("" + textField.getValue()));
		}

		@Override
		public void paint(Graphics g) {
			setValueForControl(i);
			super.paint(g);
		}

	}

	public void setContents(List<Inspected> inspected) {
		
		
		
		clear();
		int row = 0;
		for (Inspected ii : inspected) {

			Row r = new Row(ii);

			GridBagConstraints rowConstraints = new GridBagConstraints();
			rowConstraints.gridx = 0;
			rowConstraints.gridy = row;
			rowConstraints.fill = GridBagConstraints.HORIZONTAL;
			rowConstraints.weightx = 1.0;

			contents.add(r, rowConstraints);
			row++;
		}

		JLabel filler = new JLabel();
		GridBagConstraints fillerConstraints = new GridBagConstraints();
		fillerConstraints.gridx = 0;
		fillerConstraints.gridy = row;
		fillerConstraints.fill = GridBagConstraints.BOTH;
		fillerConstraints.weighty = 1.0;
		fillerConstraints.gridwidth = GridBagConstraints.REMAINDER;
		contents.add(filler, fillerConstraints);
		contents.validate();

		contents.repaint();
	}

	static public int labelWidth = 150;
	static int rowHeight = 30;

	static public class Row extends JComponent {
		private JLabel label;
		public Inspected i;

		int labelWidth = NewInspector.labelWidth;

		public Row(final Inspected i) {
			this(i, NewInspector.labelWidth);
		}

		public Row(final Inspected i, int lw) {
			this.i = i;
			this.labelWidth = lw;
			setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
			label = new JLabel(i.editor instanceof InfoControl ? "" : i.name) {
				public void paint(java.awt.Graphics g) {
					Graphics2D g2 = (Graphics2D) g;
					g2.setColor(new Color(0, 0, 0, 0.8f));
					g2.setFont(getFont());
					int textX = labelWidth - g2.getFontMetrics().stringWidth(getText()) - 10;
					int textY = (getHeight() - g2.getFont().getSize()) / 2 + 7;
					g2.drawString(getText(), textX, textY);

					if (i.editor instanceof InfoControl) {
						g2.setColor(new Color(0, 0, 0, 0.02f));
						g2.drawLine(0, getHeight() / 2, getWidth() - 4, getHeight() / 2);
					}

				};
			};
			label.setBorder(null);
			label.setPreferredSize(new Dimension(labelWidth, rowHeight));
			label.setMinimumSize(new Dimension(labelWidth, rowHeight));
			label.setMaximumSize(new Dimension(labelWidth, rowHeight));
			label.setFont(new Font(Constants.defaultFont, Font.BOLD, 10));
			this.add(label);
			this.add(Box.createHorizontalStrut(5));
			this.add(i.editor);

			i.editor.setPreferredSize(new Dimension(labelWidth, rowHeight));
			i.editor.setMinimumSize(new Dimension(labelWidth, rowHeight));
			i.editor.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowHeight));

			this.add(Box.createHorizontalGlue());

			enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
		}

		public boolean drawBackground = false;

		@Override
		public void paint(Graphics g) {

			if (drawBackground) {
				Graphics2D g2 = ((Graphics2D) g);
				g2.setPaint(new GradientPaint(0, 0, new Color(1, 1, 1, 0.0f), labelWidth, 0, new Color(1, 1, 1, 0.1f)));
				g2.fillRect(0, 0, getWidth(), this.getHeight());

				g2.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 0.0f), labelWidth, 0, new Color(0, 0, 0, 0.1f)));
				g2.fillRect(0, 0, labelWidth, this.getHeight());
			}

			super.paint(g);
		}

	}

	public void clear() {
		contents.removeAll();
		contents.add(getActions());
	}

	public JFrame getFrame() {
		return frame;
	}

}
