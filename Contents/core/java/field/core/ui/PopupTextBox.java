package field.core.ui;

import java.awt.Point;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import field.bytecode.protect.Woven;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.ui.SmallMenu.iKeystrokeUpdate;
import field.core.windowing.GLComponentWindow;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.abstraction.iAcceptor;

@Woven
public class PopupTextBox {

	Shell shell;

	private Label label;

	protected Text textBox;

	int surround = 20;

	public void doCompletions() {
		completion(textBox, new iKeystrokeUpdate() {
			public boolean update(Event arg0) {
				if (Character.isISOControl(arg0.character))
					return false;

				textBox.append("" + arg0.character);
				completion(textBox, this);
				return false;
			}
		});
	}

	boolean exitAllowed = false;

	public PopupTextBox setExitAllowed(boolean exitAllowed) {
		this.exitAllowed = exitAllowed;
		return this;
	}

	public PopupTextBox(String defaultText, Point screenPoint, String label) {

		shell = new Shell(Launcher.display, SWT.ON_TOP);

		RowLayout rl = new RowLayout();
		rl.center = true;
		rl.spacing = 3;
		shell.setLayout(rl);
		this.label = new Label(shell, SWT.RIGHT);
		this.label.setText(label);
		this.textBox = new Text(shell, SWT.SINGLE);
		this.textBox.setText(defaultText);
		this.textBox.setSelection(0, defaultText.length());
		this.textBox.setLayoutData(new RowData(300, SWT.DEFAULT));
		shell.setBounds(screenPoint.x, screenPoint.y, 400, 40);
		shell.open();
		shell.pack();

		// this.textBox.addVerifyListener(new VerifyListener() {
		//
		// @Override
		// public void verifyText(VerifyEvent e) {
		//
		// ;//System.out.println(" keycode is <"+e.keyCode+">");
		//
		// if (e.character == SWT.ESC) {
		// if (exitAllowed) {
		// shell.dispose();
		// exit();
		// }
		// } else if (e.text == "\n") {
		// shell.dispose();
		// }
		// // else
		// // changed(textBox.getText()+e.text);
		// }
		//
		// });

		this.textBox.addKeyListener(new KeyListener() {

			@Override
			public void keyReleased(org.eclipse.swt.events.KeyEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void keyPressed(org.eclipse.swt.events.KeyEvent e) {

				if (e.character == SWT.ESC)
					if (exitAllowed) {
						shell.dispose();
						exit();
					}
			}
		});

		this.textBox.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				try{
					changed(textBox.getText());
				}
				catch(SWTException ee)
				{
					ee.printStackTrace();
				}
				shell.dispose();
			}
		});
	}

	protected void completion(Text inside, iKeystrokeUpdate iKeystrokeUpdate) {
	}

	protected boolean validateText() {
		return true;
	}

	protected void changed(String text) {
	}

	protected void exit() {
		// TODO Auto-generated method stub

	}

	boolean gone = false;

	static public class Modal {
		static public void getString(Point at, String label, String def, final iAcceptor<String> result) {
			new PopupTextBox(def, at, label) {
				@Override
				protected void changed(String text) {
					result.set(text);
				}
			};
		}

		static public void getStringOrCancel(Point at, String label, String def, final iAcceptor<String> result) {
			new PopupTextBox(def, at, label) {
				@Override
				protected void changed(String text) {
					result.set(text);
				}
			}.setExitAllowed(true);
		}

		static public void getStringOrCancel(Point at, String label, String def, final iAcceptor<String> result, final iUpdateable onCancel) {
			new PopupTextBox(def, at, label) {
				@Override
				protected void changed(String text) {
					result.set(text);
				}

				protected void exit() {
					onCancel.update();
				};
			}.setExitAllowed(true);
		}

		static public void getFloat(Point at, String label, float def, final iAcceptor<Float> result) {
			new PopupTextBox(def + "", at, label) {
				@Override
				protected boolean validateText() {
					try {
						Float.parseFloat(textBox.getText());
						return true;
					} catch (NumberFormatException q) {
						return false;
					}
				}

				@Override
				protected void changed(String text) {
					result.set(Float.parseFloat(text));
				}
			};
		}

		static public void getInteger(Point at, String label, float def, final iAcceptor<Integer> result) {
			new PopupTextBox(def + "", at, label) {
				@Override
				protected boolean validateText() {
					try {
						Integer.parseInt(textBox.getText());
						return true;
					} catch (NumberFormatException q) {
						return false;
					}
				}

				@Override
				protected void changed(String text) {
					result.set(Integer.parseInt(text));
				}
			};
		}

		static public Point elementAt(iVisualElement e) {
			Rect frame = e.getFrame(null);
			GLComponentWindow window = iVisualElement.enclosingFrame.get(e);
			if (window == null)
				return new Point((int) frame.x, (int) frame.y);

			float sx = window.getXScale();
			float sy = window.getYScale();
			float tx = window.getXTranslation();
			float ty = window.getYTranslation();

			float fx = (float) (frame.x + frame.w / 2);
			float fy = (float) (frame.y + frame.h / 2);

			fx = (fx - tx) / sx;
			fy = (fy - ty) / sy;

			fx += window.getFrame().getLocation().x;
			fy += window.getFrame().getLocation().y;

			fx += window.getCanvas().getParent().getBounds().x;

			return new Point((int) fx, (int) fy);

		}

	}

	public void closeNow() {
		this.shell.setVisible(false);
	}

}
