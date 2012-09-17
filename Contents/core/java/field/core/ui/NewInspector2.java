package field.core.ui;

import java.util.LinkedHashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;

import com.lowagie.tools.ToolboxAvailable;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.Constants;
import field.core.Platform;
import field.core.plugins.selection.ToolBarFolder;
import field.core.ui.SmallMenu.BetterPopup;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.linalg.Vector4;

/**
 * a rewrite of the inspector system
 * 
 * @author marc
 * 
 */
public class NewInspector2 {

	public enum Status {
		valid, unset, mixed;
	}

	static public abstract class iIO<T> {

		public iIO(String name) {
			this.name = name;
		}

		public String name;
		public Class<? extends BaseControl<T>> editor;

		abstract public T getValue();

		abstract public Status getStatus();

		abstract public void setValue(T s);
	}

	static public abstract class Inspected<T> extends Composite {

		protected iIO<T> io;

		public Inspected(Composite parent, int style, iIO<T> io) {
			super(parent, style);
			this.io = io;
			if (io != null)
				this.name = io.name;
		}

		abstract protected void setValue(T s);

		public String name;

		protected void updateValue(T s) {
			if (io != null)
				io.setValue(s);
		}

		abstract protected Status getStatus();
	}

	private Button actions;
	private BetterPopup menu;
	String waterText = "Properties";
	private Composite contents;
	private ScrolledComposite scroller;

	public NewInspector2() {
		ToolBarFolder folder = ToolBarFolder.currentFolder;
		ScrolledComposite scroller;
		Composite contents;

		scroller = new ScrolledComposite(folder.getContainer(), SWT.VERTICAL | SWT.HORIZONTAL);
		contents = new Composite(scroller, SWT.NONE);

		init("icons/pen_32x32.png", folder, scroller, contents);
		new MacScrollbarHack(scroller);
	}

	public Composite getContents() {
		return contents;
	}

	public NewInspector2(String icon, ToolBarFolder folder, ScrolledComposite scroller, Composite contents) {
		init(icon, folder, scroller, contents);
	}

	protected void init(String icon, ToolBarFolder folder, ScrolledComposite scroller, Composite contents) {

		this.contents = contents;
		this.scroller = scroller;

		GridLayout gl = new GridLayout();
		gl.makeColumnsEqualWidth = true;
		gl.numColumns = 1;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.marginTop = 0;

		contents.setLayout(gl);

		contents.addListener(SWT.Paint, new Listener() {

			@Override
			public void handleEvent(Event event) {
				LinkedHashMap<String, iUpdateable> items = getMenuItems();
				if (items == null)
					return;

			}
		});

		folder.add(icon, scroller);

		scroller.setContent(contents);
		scroller.setExpandHorizontal(true);
		scroller.setExpandVertical(true);

		contents.setBackground(ToolBarFolder.background);

	}

	protected LinkedHashMap<String, iUpdateable> getMenuItems() {
		return null;
	}

	public void setWaterText(String waterText) {
		this.waterText = waterText;
	}

	static int rowHeight = 28;
	static int labelWidth = 100+(Platform.isLinux() ? 50 : 0);

	public abstract class BaseControl<T> extends Inspected<T> {

		protected Control editor;

		public BaseControl(iIO<T> io) {
			super(contents, 0, io);

			makeControl();

			GridData d = new GridData();
			d.grabExcessHorizontalSpace = true;
			d.horizontalAlignment = SWT.FILL;
			d.verticalAlignment = SWT.CENTER;
			d.heightHint = rowHeight;
			d.widthHint = 1000;
			this.setLayoutData(d);

			editor.setFont(new Font(Launcher.display, Constants.defaultFont, 11, 0));

			scroller.setMinHeight(contents.getChildren().length * rowHeight + 50);
			scroller.setMinWidth(labelWidth * 2);

			GridLayout gl2 = new GridLayout(2, false);
			gl2.marginLeft = labelWidth;
			setLayout(gl2);

			GridData gd2 = new GridData(labelWidth, SWT.DEFAULT);
			gd2.verticalAlignment = SWT.CENTER;
			editor.setLayoutData(gd2);

			this.addListener(SWT.Paint, new Listener() {

				@Override
				public void handleEvent(Event event) {
					paint(event);

				}
			});

			layout();
			scroller.layout();
			contents.layout();

			this.setBackground(ToolBarFolder.background);

		}

		abstract protected void makeControl();

		public T getValue() {
			return io.getValue();
		}

		abstract public void setValue(T s);

		@Override
		protected Status getStatus() {
			return io.getStatus();
		}

		protected void paint(Event event) {
			if (name == null)
				name = "";

			if (Platform.isMac())
				event.gc.setFont(new Font(event.display, Constants.defaultFont, 11, SWT.NORMAL));
			else
				event.gc.setFont(new Font(event.display, Constants.defaultFont, 9, SWT.NORMAL));

			Point m = event.gc.textExtent(name);
			int a = event.gc.getFontMetrics().getAscent();
			event.gc.drawText(name, labelWidth - m.x - 10, a - (Platform.isMac() ? 1 : 3), true);

			if (editor != contents.getChildren()[0]) {
				event.gc.setForeground(new Color(Launcher.display, 0, 0, 0));
				event.gc.setAlpha(10);
				event.gc.drawLine(0, 0, 600, 0);
			}
		}
	}

	public class TextControl extends BaseControl<String> {

		public TextControl(iIO<String> io) {
			super(io);

		}

		@Override
		protected void makeControl() {
			editor = new Text(this, 0);
			((Text) editor).setText(getValue());
			editor.addListener(SWT.Verify, new Listener() {
				@Woven
				@NextUpdate
				@Override
				public void handleEvent(Event event) {
					event.doit = true;
					updateValue(((Text) editor).getText());
				}
			});
		}

		@Override
		public void setValue(String s) {
			((Text) editor).setText(s);
		}

	}

	public class InfoControl extends BaseControl<Object> {

		public InfoControl(iIO<Object> ignored) {
			super(ignored);
			this.setBackground(ToolBarFolder.firstLineBackground);

		}

		@Override
		protected void makeControl() {
			editor = new Text(this, 0);
			((Text) editor).setText("");
			editor.addListener(SWT.Verify, new Listener() {

				@Woven
				@NextUpdate
				@Override
				public void handleEvent(Event event) {
					event.doit = true;
					updateValue(((Text) editor).getText());
				}
			});
			editor.setVisible(false);

		}

		protected void paint(Event event) {
			if (name == null)
				name = "";
			Font f = event.gc.getFont();
			FontData[] data = f.getFontData();

			event.gc.setFont(new Font(f.getDevice(), data[0].getName(), data[0].getHeight() + 2, SWT.NORMAL));

			Point m = event.gc.textExtent(name);
			int a = event.gc.getFontMetrics().getAscent();
			event.gc.drawText(name, Math.max(10, labelWidth - m.x - 10), a - 8, true);

			if (editor != contents.getChildren()[0]) {
				event.gc.setForeground(new Color(Launcher.display, 220, 220, 220));
				event.gc.drawLine(0, 0, 600, 0);
			}
		}

		@Override
		public void setValue(Object s) {

		}
	}

	public class SpinnerControl extends BaseControl<Number> {

		public SpinnerControl(iIO<Number> io) {
			super(io);
		}

		protected void makeControl() {
			editor = new Spinner(this, 0);
			((Spinner) editor).setMinimum(Integer.MIN_VALUE);
			((Spinner) editor).setMaximum(Integer.MAX_VALUE);
			if (getValue() != null)
				((Spinner) editor).setSelection(getValue().intValue());

			((Spinner) editor).setBackgroundMode(SWT.NO_BACKGROUND);

			editor.addListener(SWT.Selection, new Listener() {

				@Woven
				@NextUpdate
				public void handleEvent(Event event) {
					;//System.out.println(" update value <" + ((Spinner) editor).getSelection() + ">");
					updateValue((float) ((Spinner) editor).getSelection());
				}
			});

			editor.addListener(SWT.MouseVerticalWheel, new Listener() {

				@Override
				public void handleEvent(Event arg0) {
					Control focusControl = Launcher.display.getFocusControl();

					int s = ((Spinner) editor).getSelection();
					s += arg0.count;

					((Spinner) editor).setSelection(s);
					updateValue(s);
					
					
				}
			});
		}

		@Override
		public void setValue(Number s) {
			;//System.out.println(" set value <" + s + ">");
			((Spinner) editor).setSelection(s.intValue());
		}

	}

	public class BooleanControl extends BaseControl<Boolean> {

		public BooleanControl(iIO<Boolean> io) {
			super(io);

		}

		protected void makeControl() {
			editor = new Button(this, SWT.CHECK);
			if (getStatus() != Status.valid)
				((Button) editor).setGrayed(true);
			else {
				Object g = getValue();
				if (g instanceof Number)
					g = ((Number) g).intValue() != 0;

				((Button) editor).setSelection((Boolean) g);
			}

			((Button) editor).addListener(SWT.Selection, new Listener() {

				@Woven
				@NextUpdate
				public void handleEvent(Event event) {
					updateValue(((Button) editor).getSelection());
					((Button) editor).setGrayed(getStatus() != Status.valid);
				}
			});
		}

		public void setUnits(String s) {

			GridData gd2 = new GridData(20, SWT.DEFAULT);
			gd2.verticalAlignment = SWT.CENTER;
			editor.setLayoutData(gd2);

			Label label = new Label(this, 0);
			label.setText(s);

			gd2 = new GridData(100, SWT.DEFAULT);
			gd2.verticalAlignment = SWT.CENTER;
			label.setLayoutData(gd2);

			label.setFont(new Font(Launcher.display, Constants.defaultFont, 8, 0));

		}

		@Override
		public void setValue(Boolean s) {
			((Button) editor).setSelection(s);
			((Button) editor).setGrayed(false);
		}

	}

	public class ColorControl extends BaseControl<Vector4> {

		public ColorControl(iIO<Vector4> io) {
			super(io);

		}

		Vector4 value = new Vector4();

		protected void makeControl() {
			editor = new Button(this, SWT.FLAT);

			editor.addPaintListener(new PaintListener() {

				@Override
				public void paintControl(PaintEvent e) {
					Vector4 v = getValue();
					if (v != null) {
						try {
							Color cc = new Color(Launcher.display, (int) (255 * v.x), (int) (255 * v.y), (int) (255 * v.z));
							e.gc.setBackground(cc);
							e.gc.setForeground(cc);
							e.gc.fillRectangle(e.x, e.y, e.width, e.height);
						} catch (IllegalArgumentException ex) {

						}
					}
				}
			});

			((Button) editor).addListener(SWT.Selection, new Listener() {

				@Override
				public void handleEvent(Event event) {
					value = getValue();
					if (value == null)
						value = new Vector4();
					ColorDialog d = new org.eclipse.swt.widgets.ColorDialog(editor.getShell());
					RGB rgb = new RGB((int) (Math.max(0, Math.min(255, 255 * value.x))), (int) (Math.max(0, Math.min(255, 255 * value.y))), (int) (Math.max(0, Math.min(255, 255 * value.z))));
					;//System.out.println(" initial color is <" + rgb + "> / <" + value + ">");
					d.setRGB(rgb);
					d.open();
					rgb = d.getRGB();

					if (rgb != null) {

						value.x = rgb.red / 255f;
						value.y = rgb.green / 255f;
						value.z = rgb.blue / 255f;
						updateValue(value);
						editor.redraw();
					}
				}
			});

			value = getValue();
		}

		@Override
		public void setValue(Vector4 s) {
			value.setValue(s);
			editor.redraw();
		}

	}

	public class MoreControl extends BaseControl<LinkedHashMap<String, iUpdateable>> {

		public MoreControl(iIO<LinkedHashMap<String, iUpdateable>> io) {
			super(io);

			GridData d = new GridData();
			d.grabExcessHorizontalSpace = true;
			d.grabExcessVerticalSpace = true;
			d.horizontalAlignment = SWT.RIGHT;
			d.verticalAlignment = SWT.BOTTOM;
			d.heightHint = rowHeight;
			d.widthHint = 1000;
			this.setLayoutData(d);

			GridData d2 = new GridData();
			d2.grabExcessHorizontalSpace = true;
			d2.grabExcessVerticalSpace = true;
			d2.horizontalAlignment = SWT.RIGHT;
			d2.verticalAlignment = SWT.TOP;
			d2.heightHint = rowHeight;
			d2.widthHint = 30;
			editor.setLayoutData(d2);

			scroller.setMinHeight(contents.getChildren().length * rowHeight + 50);
			scroller.setMinWidth(labelWidth * 2);

			GridLayout gl2 = new GridLayout();
			gl2.marginLeft = labelWidth;
			setLayout(gl2);

			// GridData gd2 = new GridData(labelWidth, SWT.DEFAULT);
			// gd2.verticalAlignment = SWT.CENTER;
			// editor.setLayoutData(gd2);

			this.addListener(SWT.Paint, new Listener() {

				@Override
				public void handleEvent(Event event) {
					paint(event);

				}
			});

			layout();
			scroller.layout();
			contents.layout();

		}

		protected void makeControl() {
			editor = new Button(this, SWT.FLAT);

			((Button) editor).setText("+");
			((Button) editor).setBackground(ToolBarFolder.background);

			((Button) editor).addListener(SWT.Selection, new Listener() {

				@Override
				public void handleEvent(Event event) {

					SmallMenu m = new SmallMenu();
					m.createMenu(io.getValue(), editor.getShell(), null).show(Launcher.display.map(editor, editor.getShell(), new Point(event.x, event.y)));

				}
			});
		}

		@Override
		public void setValue(LinkedHashMap<String, iUpdateable> s) {
		}

	}

	//
	// static public class ActionControl extends JComponent implements
	// ActionListener, SetValueForControl<Action, ActionControl> {
	//
	// private Inspected<Action, ActionControl> i;
	// private JButton textField;
	//
	// public ActionControl(Inspected<Action, ActionControl> i) {
	// this.i = i;
	// setLayout(new BorderLayout(0, 0));
	// textField = new JButton("Perform...");
	// textField.putClientProperty("Quaqua.Button.style", "square");
	// textField.putClientProperty("JComponent.sizeVariant", "small");
	// textField.setFont(new Font(Constants.defaultFont, Font.BOLD, 10));
	// // textField.addActionListener(this);
	// add(textField, BorderLayout.CENTER);
	// setValueForControl(i);
	//
	// textField.addMouseListener(new MouseAdapter() {
	// @Override
	// public void mouseClicked(MouseEvent e) {
	// ;//System.out.println(" mouse clicked in action control");
	// super.mouseClicked(e);
	// actionPerformed(null);
	// }
	// });
	// };
	//
	// public void setValueForControl(Inspected<Action, ActionControl> ii) {
	// this.i = ii;
	// }
	//
	// public void actionPerformed(ActionEvent e) {
	//
	// ;//System.out.println(" action performed here <" + e + ">");
	//
	// i.getValue().apply();
	// }
	//
	// }
	//
	// static public class InfoControl extends JComponent {
	//
	// private JLabel textField;
	//
	// public InfoControl(Inspected<String, InfoControl> i) {
	// setLayout(new BorderLayout(0, 0));
	// textField = new JLabel("");
	// textField.putClientProperty("JComponent.sizeVariant", "small");
	// textField.setFont(new Font(Constants.defaultFont, 1, 10));
	// textField.setText(i.getValue());
	// add(textField, BorderLayout.CENTER);
	// }
	//
	// }
	//
	// static public class ColorControl extends JComponent implements
	// ActionListener {
	//
	// private JComponent textField;
	// private final Inspected i;
	//
	// public ColorControl(final Inspected<Vector4, ColorControl> i) {
	// this.i = i;
	// FlowLayout fl = new FlowLayout(FlowLayout.LEADING, 0, 5);
	// setLayout(fl);
	// textField = new JComponent() {
	// @Override
	// protected void paintComponent(Graphics g) {
	//
	// g.translate(5, 5);
	//
	// enableEvents(AWTEvent.MOUSE_EVENT_MASK);
	// if (i.getStatus() == Status.unset) {
	// g.setColor(new Color(0, 0, 0, 0.1f));
	// g.drawRect(0, 0, this.getWidth(), this.getHeight());
	// return;
	// }
	//
	// Vector4 color = i.getValue();
	// Rectangle bounds = this.getBounds();
	//
	// Graphics2D g2 = (Graphics2D) g;
	//
	// int dd = 10;
	// for (int x = 0; x < 1 + this.getWidth() / dd; x++) {
	// for (int y = 0; y < 1 + this.getHeight() / dd; y++) {
	// g.setColor((x + y) % 2 == 0 ? new Color(0.35f, 0.35f, 0.35f, 1f) :
	// new
	// Color(0.65f, 0.65f, 0.65f, 1f));
	// g.fillRect(x * dd, y * dd, dd, dd);
	// }
	// }
	// if (i.getStatus() == Status.mixed) {
	//
	// } else {
	//
	// g2.setColor(new Color(color.x, color.y, color.z, 1));
	// GeneralPath upper = new GeneralPath();
	// upper.moveTo(0, 0);
	// upper.lineTo(bounds.width, 0);
	// upper.lineTo(0, bounds.height);
	// upper.lineTo(0, 0);
	// g2.fill(upper);
	//
	// g2.setColor(new Color(color.x, color.y, color.z, color.w));
	// GeneralPath lower = new GeneralPath();
	// lower.moveTo(bounds.width, 0);
	// lower.lineTo(bounds.width, bounds.height);
	// lower.lineTo(0, bounds.height);
	// lower.lineTo(bounds.width, 0);
	// g2.fill(lower);
	// }
	//
	// }
	//
	// @Override
	// protected void processMouseEvent(MouseEvent e) {
	// if (e.getID() == e.MOUSE_CLICKED && e.getButton() == 1) {
	//
	// Point x = e.getLocationOnScreen();
	//
	// // Point x = new Point(20, 20);
	// // SwingUtilities.convertPointToScreen(x, this);
	// Vector4 color = i.getValue();
	// if (color == null)
	// color = new Vector4(0.5, 0.5, 0.5, 0);
	// new ColorDialog(new Color(color.x, color.y, color.z, color.w),
	// (Frame)
	// SwingUtilities.getWindowAncestor(this), x) {
	// public void setColor(Color color) {
	// super.setColor(color);
	// i.setValue(new Vector4(color.getRed() / 255f, color.getGreen() /
	// 255f,
	// color.getBlue() / 255f, color.getAlpha() / 255f));
	// };
	// };
	// }
	// }
	//
	// };
	// textField.setPreferredSize(new Dimension(16, 16));
	// textField.setMaximumSize(new Dimension(16, 16));
	// textField.putClientProperty("JComponent.sizeVariant", "small");
	// textField.setFont(new Font(Constants.defaultFont, 0, 10));
	// add(textField);
	// enableEvents(AWTEvent.MOUSE_EVENT_MASK |
	// AWTEvent.MOUSE_MOTION_EVENT_MASK);
	//
	// }
	//
	// @Override
	// public void actionPerformed(ActionEvent e) {
	// }
	//
	// }
	//
	// public interface SetValueForControl<T, C extends Control> {
	// public void setValueForControl(Inspected<T, C> i);
	// }
	//
	// static public class IntControl extends JComponent implements
	// ChangeListener, SetValueForControl<Integer, IntControl> {
	//
	// private DraggableNumber textField;
	// private Inspected<Integer, IntControl> i;
	//
	// public IntControl(Inspected<Integer, IntControl> i) {
	// this.i = i;
	// FlowLayout fl = new FlowLayout(FlowLayout.LEADING, 0, 0);
	// setLayout(fl);
	// textField = new DraggableNumber();
	// textField.putClientProperty("JComponent.sizeVariant", "small");
	// textField.setFont(new Font(Constants.defaultFont, 0, 10));
	// textField.addChangeListener(this);
	// add(textField);
	//
	// NumberFormat nf = NumberFormat.getNumberInstance();
	// nf.setMaximumFractionDigits(0);
	// nf.setMinimumFractionDigits(0);
	// textField.setNumberFormat(nf);
	// setValueForControl(i);
	// enableEvents(AWTEvent.MOUSE_EVENT_MASK |
	// AWTEvent.MOUSE_MOTION_EVENT_MASK);
	// }
	//
	// public void setValueForControl(Inspected<Integer, IntControl> i) {
	// this.i = i;
	// Integer vv = i.getValue();
	// if (vv == null)
	// vv = 0;
	// if (i.getStatus() == Status.valid) {
	// if (textField.getValue() != vv)
	// textField.setValue(vv);
	// } else if (i.getStatus() == Status.mixed) {
	// textField.setStatusMixed(vv);
	// } else if (i.getStatus() == Status.unset) {
	// textField.setStatusUnset(vv);
	// }
	// }
	//
	// @Override
	// public void stateChanged(ChangeEvent e) {
	// i.setValue((int) Float.parseFloat("" + textField.getValue()));
	// }
	//
	// @Override
	// public void paint(Graphics g) {
	// setValueForControl(i);
	// super.paint(g);
	// }
	//
	// }

	public void clear() {
		Control[] c = contents.getChildren();
		for (int i = 0; i < c.length; i++) {
			// if (c[i] != gearButton)
			c[i].dispose();
		}
	}

	public iIO<LinkedHashMap<String, iUpdateable>> getMenuItemsIO() {
		iIO<LinkedHashMap<String, iUpdateable>> m = new iIO<LinkedHashMap<String, iUpdateable>>("") {

			@Override
			public LinkedHashMap<String, iUpdateable> getValue() {
				return getMenuItems();
			}

			@Override
			public Status getStatus() {
				return Status.valid;
			}

			@Override
			public void setValue(LinkedHashMap<String, iUpdateable> s) {
			}
		};

		m.editor = MoreControl.class;

		return m;
	}

	public Shell getShell() {
		return this.scroller.getShell();
	}

}
