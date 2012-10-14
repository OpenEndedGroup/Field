package field.core.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Decorations;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.Constants;
import field.core.Platform;
import field.core.ui.SmallMenu.iHoverUpdate;
import field.launch.Launcher;
import field.launch.iUpdateable;

public class SmallMenu {

	@Woven
	public class BetterPopup {
		public Shell shell;
		public Table menu;
		private final iKeystrokeUpdate u;
		private final Decorations parent;

		public BetterPopup(Decorations parent, final iKeystrokeUpdate u) {
			this.parent = parent;
			this.u = u;
			shell = new Shell(Launcher.display, SWT.ON_TOP);
			shell.setSize(0, 0);
			menu = new Table(shell, SWT.SINGLE);
			shell.setLayout(new FillLayout());
			new Pretty(menu, 200);
		}

		boolean rightJustified = false;

		public BetterPopup setRightJustified(boolean rightJustified) {
			this.rightJustified = rightJustified;
			return this;
		}

		boolean fired = false;

		public void show(Point position) {

			position = Launcher.display.map(parent, null, position);
			Point s = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);

			s.x += 5;
			s.x += 20;
			s.y += 15;

			if (s.x > Launcher.display.getBounds().width * 0.8f) {
				s.x = (int) (Launcher.display.getBounds().width * 0.8f);
			}

			if (position.y + s.y > Launcher.display.getBounds().height) {
				position.y = Math.max(30, Launcher.display.getBounds().height - s.y - 10);
			}

			if (position.x + s.x > Launcher.display.getBounds().width + Launcher.display.getBounds().x) {
				position.x = Launcher.display.getBounds().width + Launcher.display.getBounds().x - s.x - 10;
			}

			// shell.setLocation(position);

			s.y = Math.min(s.y, Launcher.display.getBounds().height - position.y);

			// shell.setSize(s);

			// ;//System.out.println("active shell is <" +
			// Launcher.display.getActiveShell() + ">");
			// shell.setVisible(Launcher.display.getActiveShell() !=
			// null);

			// shell.setBounds(position.x, position.y, s.x, s.y);
			shell.setBounds(position.x, position.y, 1, 1);
			// shell.setBounds(10000000, position.y, s.x, s.y);

			shell.addListener(SWT.MouseMove, new Listener() {

				@Override
				public void handleEvent(Event event) {

					// ;//System.out.println("SHELL  mouse move <"
					// + event + ">");

				}
			});

			menu.addListener(SWT.MouseExit, new Listener() {

				@Override
				public void handleEvent(Event event) {
					// ;//System.out.println("SHELL mouse exit ");
					menu.deselectAll();
				}
			});

			shell.addMouseMoveListener(new MouseMoveListener() {

				@Override
				public void mouseMove(MouseEvent e) {
					// ;//System.out.println("SHELL C ");
				}
			});

			menu.addMouseMoveListener(new MouseMoveListener() {

				@Override
				public void mouseMove(MouseEvent e) {
					// ;//System.out.println("SHELL C ");
				}
			});

			shell.addMouseListener(new MouseListener() {

				@Override
				public void mouseUp(MouseEvent e) {
					// ;//System.out.println("SHELL A ");
				}

				@Override
				public void mouseDown(MouseEvent e) {
					// ;//System.out.println("SHELL A ");
				}

				@Override
				public void mouseDoubleClick(MouseEvent e) {
					// ;//System.out.println("SHELL A ");
				}
			});
			menu.addMouseListener(new MouseListener() {

				@Override
				public void mouseUp(MouseEvent e) {
					// ;//System.out.println("menu B ");
				}

				@Override
				public void mouseDown(MouseEvent e) {
					// ;//System.out.println("menu b ");
				}

				@Override
				public void mouseDoubleClick(MouseEvent e) {
					// ;//System.out.println("menu b ");
				}
			});

			menu.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					if (e.widget.getData() == null)
						menu.deselectAll();
					e.doit = false;

					;// System.out.println(" SELECTION :" +
						// e);

					if (hu != null) {
						hu.update(menu.getSelectionIndex());
					}

				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});

			menu.addListener(SWT.Verify, new Listener() {

				@Override
				public void handleEvent(Event event) {
					// ;//System.out.println(" verify <" +
					// event + ">");
				}
			});
			menu.addListener(SWT.DefaultSelection, new Listener() {
				public void handleEvent(Event event) {
					// do something
					TableItem[] ss = menu.getSelection();
					TableItem m = (TableItem) (ss == null || ss.length == 0 ? null : ss[0]);

					if (event.item != null)
						m = (TableItem) event.item;

					System.out.println(" closing it (default selection)");
					shell.setVisible(false);
					Object d = m == null ? null : m.getData();
					// ;//System.out.println(" d = <" + d +
					// "> <" + m + "> <" + ss.length + "> <"
					// + event.item + ">");
					done();
					if (d instanceof iUpdateable) {
						fired = true;
						((iUpdateable) d).update();
					}
					event.doit = false;
				}
			});
			menu.addListener(SWT.MouseMove, new Listener() {

				@Override
				public void handleEvent(Event event) {

					// ;//System.out.println("SHELL mouse move event ");

					TableItem m = menu.getItem(new Point(event.x, event.y));

					menu.deselectAll();
					TableItem[] ii = menu.getItems();
					for (int i = 0; i < ii.length; i++) {
						if (ii[i] == m) {
							menu.setSelection(i);
							if (hu != null)
								hu.update(i);
						}
					}
				}
			});
			menu.addListener(SWT.MouseDown, new Listener() {

				@Override
				public void handleEvent(Event event) {
					// do something
					TableItem m = menu.getItem(new Point(event.x, event.y));
					System.out.println(" closing it (mouse down)");
					shell.setVisible(false);
					Object d = m == null ? null : m.getData();
					done();
					if (d instanceof iUpdateable) {
						fired = true;
						((iUpdateable) d).update();
					}
				}
			});

			menu.addListener(SWT.KeyDown, new Listener() {
				public void handleEvent(Event event) {

					System.out.println(" arrow down ?" + event);

					if (event.keyCode == SWT.ESC) {
						System.out.println(" closing it (escape)");
						shell.setVisible(false);
						if (hu != null)
							hu.cancel();
						done();
					}
					if (event.keyCode == '\n') {
						// ;//System.out.println(" return, taking it");

						TableItem[] ss = menu.getSelection();
						TableItem m = (TableItem) (ss == null || ss.length == 0 ? null : ss[0]);
						System.out.println(" closing it (newline)");
						shell.setVisible(false);
						Object d = m == null ? null : m.getData();
						// ;//System.out.println(" d = <"
						// +
						// d + ">");
						done();
						if (d instanceof iUpdateable) {
							fired = true;
							((iUpdateable) d).update();
						}
						event.doit = false;
					} else if (event.keyCode == SWT.ARROW_DOWN) {
						event.doit = false;
						int s = menu.getSelectionIndex();
						System.out.println(" down <" + s + ">");
						if (s == -1) {
							System.out.println(" scaning for first selection ");
							for (int i = 0; i < menu.getItemCount(); i++) {
								System.out.println(i + " " + menu.getItemCount() + " " + menu.getItem(i).getData());
								if (menu.getItem(i).getData() != null) {
									menu.setSelection(i);
									System.out.println(" set selection to be <" + i + ">");
									if (hu != null)
										hu.update(i);

									menu.redraw();
									break;
								}
							}
							System.out.println(" -- fell through --");
						} else {
							for (int i = 0; i < menu.getItemCount(); i++) {
								s = (s + 1) % menu.getItemCount();
								if (menu.getItem(s).getData() != null) {
									// ;//System.out.println(" now<"
									// + s +
									// ">");
									menu.setSelection(s);
									if (hu != null)
										hu.update(s);

									menu.redraw();
									break;
								}
							}
						}
					} else if (event.keyCode == SWT.ARROW_UP) {
						event.doit = false;
						int s = menu.getSelectionIndex();
						// ;//System.out.println(" down <"
						// + s + ">");
						if (s == -1) {
							for (int i = menu.getItemCount() - 1; i >= 0; i--) {
								if (menu.getItem(i).getData() != null) {
									// ;//System.out.println(" now<"
									// + i +
									// ">");
									menu.setSelection(i);
									menu.redraw();
									if (hu != null)
										hu.update(i);
									break;
								}
							}
						} else {
							for (int i = 0; i < menu.getItemCount(); i++) {
								s = (s - 1 + menu.getItemCount()) % menu.getItemCount();
								if (menu.getItem(s).getData() != null) {
									// ;//System.out.println(" now<"
									// + s +
									// ">");
									menu.setSelection(s);
									menu.redraw();
									if (hu != null)
										hu.update(s);
									break;
								}
							}
						}
					}
				}
			});

			menu.addListener(SWT.KeyDown, new Listener() {

				@Override
				public void handleEvent(Event event) {

					// ;//System.out.println(" actually got a key event in a menu <"
					// + event + ">");

					// TODO swt there was lots of logic in
					// here about when to
					// close
					if (u != null)
						if (u.update(event)) {
							System.out.println(" closing it ");
							shell.setVisible(false);
						}

				}
			});

			menu.addListener(SWT.MouseHover, new Listener() {

				@Override
				public void handleEvent(Event event) {
					// ;//System.out.println("SHELL hover event ");
				}
			});

			shell.addListener(SWT.Deactivate, new Listener() {

				@Override
				public void handleEvent(Event event) {
					System.out.println(" closing it (deactivate)");
					new Exception().printStackTrace();
					shell.setVisible(false);

					if (hu != null && !fired) {
						hu.cancel();
					}

				}
			});

			if (Platform.isLinux()) {
				shell.setSize(s.x, s.y);
				shell.open();

			}
			openNext(s);

			System.out.println(" ---------- OPEN ----------");
			new Exception().printStackTrace();

		}

		long down = 0;

		@NextUpdate
		protected void openNext(Point s) {
			shell.open();

			// ;//System.out.println(" opennext is setting size to be <"
			// + s + ">");

			if (rightJustified) {
				int nx = shell.getBounds().x - s.x;
				shell.setLocation(nx, shell.getBounds().y);
			}

			shell.setSize(s.x, s.y);
			shell.forceActive();
			boolean forced = menu.forceFocus();
			// selectFirst();

			// ;//System.out.println(" forced ? <" + forced + ">");

			down = System.currentTimeMillis();

			Listener f = new Listener() {

				@Override
				public void handleEvent(Event event) {

					// ;//System.out.println(" we are in a left over filter with a mouse up <"+menu.isDisposed()+"> <"+menu.isVisible()+">");
					if (menu.isDisposed() || !menu.isVisible()) {
						Launcher.display.removeFilter(SWT.MouseMove, this);
						Launcher.display.removeFilter(SWT.MouseUp, this);
						return;
					}

					if (event.type == SWT.MouseUp) {
						// ;//System.out.println(" deinstalling filter ");
						Launcher.display.removeFilter(SWT.MouseMove, this);
						Launcher.display.removeFilter(SWT.MouseUp, this);

						if ((down > 0 && System.currentTimeMillis() - down > 1000) || menu.getSelectionCount() > 0) {

							if (event.widget != menu) {

								Listener[] shakers = menu.getListeners(SWT.KeyDown);
								// ;//System.out.println(" shakers are :"
								// +
								// Arrays.asList(shakers));

								event.keyCode = '\n';
								for (Listener m : shakers) {
									m.handleEvent(event);
								}

							} else {
								// ;//System.out.println(" event fine");
							}
						}
					}
					if (event.type == SWT.MouseMove) {
						if (event.widget != menu) {
							// ;//System.out.println(" event not going to widget, would redispatch");

							Listener[] movers = menu.getListeners(SWT.MouseMove);
							;// System.out.println(" movers :"
								// +
								// Arrays.asList(movers));

							Point c = Launcher.display.map((Control) event.widget, menu, new Point(event.x, event.y));
							int ox = event.x;
							int oy = event.y;
							event.x = c.x;
							event.y = c.y;
							for (Listener m : movers) {
								m.handleEvent(event);
							}

							event.x = ox;
							event.y = oy;

						} else {
							// ;//System.out.println(" event fine");
						}
					}
				}
			};
			Launcher.display.addFilter(SWT.MouseMove, f);
			Launcher.display.addFilter(SWT.MouseUp, f);

			menu.setCapture(true);
			shell.setCapture(true);
		}

		public iUpdateable doneHook = null;
		private iHoverUpdate hu;

		protected void done() {
			new Exception().printStackTrace();
			if (doneHook != null)
				doneHook.update();
		}

		public void selectFirst() {
			for (int i = 0; i < menu.getItemCount(); i++) {
				if (menu.getItem(i).getData() != null) {
					// ;//System.out.println(" now<" + i +
					// ">");
					menu.setSelection(i);
					menu.redraw();
					break;
				}
			}
		}

		public BetterPopup setHoverUpdate(iHoverUpdate hoverUpdate) {
			hu = hoverUpdate;
			return this;
		}
	}

	public interface iKeystrokeUpdate {
		// returns should close
		public boolean update(Event ke);
	}

	public interface iHoverUpdate {
		public void update(int index);

		public void cancel();
	}

	static public class Submenu implements iUpdateable {
		public LinkedHashMap<String, iUpdateable> menu = new LinkedHashMap<String, iUpdateable>();

		public Submenu(LinkedHashMap<String, iUpdateable> menu) {
			super();
			this.menu = menu;
		}

		public void update() {
		}
	}

	static public class Documentation implements iUpdateable {

		private final String documentation;

		public Documentation(String documentation) {
			this.documentation = documentation;
		}

		public void update() {

		};

		@Override
		public String toString() {
			return documentation;
		};
	}

	public BetterPopup createMenu(LinkedHashMap<String, iUpdateable> items, Decorations parent, final iKeystrokeUpdate keystrokeUpdate) {
		return createMenu(items, parent, keystrokeUpdate, null);
	}

	public BetterPopup createMenu(LinkedHashMap<String, iUpdateable> items, Decorations parent, final iKeystrokeUpdate keystrokeUpdate, iHoverUpdate hoverUpdate) {

		Iterator<Entry<String, iUpdateable>> i = items.entrySet().iterator();

		final BetterPopup menu = new BetterPopup(parent, keystrokeUpdate).setHoverUpdate(hoverUpdate);

		int num = 0;

		while (i.hasNext()) {
			Entry<String, iUpdateable> e = i.next();
			String name = e.getKey();
			final iUpdateable act = e.getValue();

			if (num++ > 250)
				break;

			if (act != null && !(act instanceof Documentation)) {

				String accelerator = null;
				Pattern accPattern = Pattern.compile("///(.*)///");
				Matcher m = accPattern.matcher(name);
				if (m.find()) {
					accelerator = m.group(1);
					name = m.replaceAll("");
				}

				TableItem item = new TableItem(menu.menu, SWT.PUSH);
				if (name.startsWith("!")) {
					item.setData("check", true);
					name = name.substring(1);
				}

				if (name.startsWith("\t"))
					name = " " + name.substring(1);

				if (name.indexOf("\t") == -1) {
					char lead = 0;
					for (int in = 0; in < name.length(); in++) {
						if (!Character.isWhitespace(name.charAt(in)) && lead == 0) {
							lead = name.charAt(in);
						} else if (Character.isWhitespace(name.charAt(in)) && lead != 0) {
							name = " " + lead + "\t" + name.substring(in).trim();
							break;
						} else if (!Character.isWhitespace(name.charAt(in)) && lead != 0) {
							name = "\t" + name.trim();
							break;
						}
					}
				}

				item.setText(name);
				item.setData(act);
				item.addListener(SWT.Selection, new Listener() {

					@Override
					public void handleEvent(Event event) {
						act.update();

						// is this needed ?
						// menu.menu.setVisible(false);
					}
				});
				if (accelerator != null)
					item.setData("acc", accelerator);

			} else if (act instanceof Documentation) {

				String doc = ((Documentation) act).documentation;

				String[] pieces = doc.split("\n");
				for (String p : pieces) {
					if (p.trim().length() == 0)
						continue;
					TableItem item = new TableItem(menu.menu, SWT.PUSH);

					item.setText(p);
					item.setData(null);
					item.setData("documentation", p);
				}
			} else {
				TableItem item = new TableItem(menu.menu, SWT.PUSH);
				item.setText(name);
				item.setData(null);
				item.addListener(SWT.Selection, new Listener() {

					@Override
					public void handleEvent(Event event) {
					}
				});
			}
		}

		return menu;
	}

	static public class Pretty {

		Pattern SMALLER_PATTERN = Pattern.compile("(<font size=-3 color='#" + Constants.defaultTreeColorDim + "'>)(.*?)(</font>)");
		Pattern SMALLER2_PATTERN = Pattern.compile("(<font size=-2>)(.*?)(</font>)");

		Pattern GREY_PATTERN = Pattern.compile("(<g>)(.*?)(</g>)");

		Pattern BOLDITALIC_PATTERN = Pattern.compile("(<bi>)(?=\\S)(.+?[*_]*)(?<=\\S)(</bi>)");
		Pattern BOLD_PATTERN = Pattern.compile("(<b>)(.*?)(</b>)");
		Pattern ITALIC_PATTERN = Pattern.compile("(<i>)(?=\\S)(.*?)(?<=\\S)(</i>)");
		Pattern SEP_PATTERN = Pattern.compile("_____________________________");

		Pattern CLEAN_PATTERN = Pattern.compile("(<.*?>)");

		private Font boldFont;
		private Font boldItalicFont;
		private Font smallerFont;
		private Font italicFont;
		private Font normalFont;
		private Font fixedFont;
		private Font greyFont;

		int indent = 5;
		int vertSpace = 3;
		private Font bigFont;

		public Pretty(final Table install, final int fixedWidth) {
			String name = install.getFont().getFontData()[0].getName();

			// name = Constants.defaultFont;

			smallerFont = new Font(Launcher.display, name, (int) (GraphNodeToTreeFancy.baseFontHeight(install) * 0.75f), SWT.NORMAL);
			bigFont = new Font(Launcher.display, name, (int) (GraphNodeToTreeFancy.baseFontHeight(install) * 1.15f), SWT.NORMAL);
			boldItalicFont = new Font(Launcher.display, name, GraphNodeToTreeFancy.baseFontHeight(install), SWT.BOLD | SWT.ITALIC);
			boldFont = new Font(Launcher.display, name, GraphNodeToTreeFancy.baseFontHeight(install), SWT.BOLD);
			italicFont = new Font(Launcher.display, name, GraphNodeToTreeFancy.baseFontHeight(install), SWT.ITALIC);
			normalFont = new Font(Launcher.display, name, GraphNodeToTreeFancy.baseFontHeight(install), SWT.NORMAL);
			fixedFont = new Font(Launcher.display, name, GraphNodeToTreeFancy.baseFontHeight(install), SWT.NORMAL);
			greyFont = new Font(Launcher.display, name, GraphNodeToTreeFancy.baseFontHeight(install), SWT.NORMAL);

			install.setBackground(install.getShell().getBackground());

			install.addListener(SWT.MeasureItem, new Listener() {

				@Override
				public void handleEvent(Event event) {
					String textToDraw = ((TableItem) event.item).getText();
					Point dim = measure(textToDraw, event.gc);
					event.width = Math.max(fixedWidth, dim.x + indent);
					event.height = dim.y + vertSpace * 2;

					Object acc = ((TableItem) event.item).getData("acc");
					if (acc != null)
						event.width += 20;
				}
			});
			install.addListener(SWT.PaintItem, new Listener() {

				@Override
				public void handleEvent(Event event) {
					String textToDraw = ((TableItem) event.item).getText();

					Object d = ((TableItem) event.item).getData();
					if (d == null) {

						Object doc = ((TableItem) event.item).getData("documentation");

						if (doc == null) {

							event.gc.setBackground(new Color(Launcher.display, 200, 200, 200));
							event.gc.fillRectangle(event.x - 2, event.y - 2, install.getSize().x + 4, event.height + 4);
							event.gc.setForeground(Launcher.display.getSystemColor(SWT.COLOR_BLACK));

							draw(textToDraw, event.gc, event.x + indent, event.y + 2 + vertSpace, null);
						} else {
							event.gc.setBackground(new Color(Launcher.display, 220, 220, 220));
							event.gc.fillRectangle(event.x, event.y, install.getSize().x, event.height);

							event.gc.setForeground(Launcher.display.getSystemColor(SWT.COLOR_BLACK));

							draw("<b>" + textToDraw + "</b>", event.gc, event.x + indent, event.y + 2 + vertSpace, null);
						}
					} else {
						draw(textToDraw, event.gc, event.x + indent, event.y + 2 + vertSpace);
					}

					Object acc = ((TableItem) event.item).getData("acc");
					if (acc != null)
						drawAccelerator(event.gc, "" + acc, event.x, event.y, install.getSize().x, event.height);

					Object c = ((TableItem) event.item).getData("check");
					if (c != null) {
						event.gc.setForeground(Launcher.display.getSystemColor(SWT.COLOR_DARK_RED));
						event.gc.setBackground(Launcher.display.getSystemColor(SWT.COLOR_DARK_RED));
						event.gc.setAlpha(128);
						int dd = 6;
						// event.gc.fillOval(event.x +
						// event.height - 3, event.y
						// + event.height / 2 - dd / 2,
						// dd, dd);

						event.gc.fillRectangle(event.x, event.y, install.getSize().x, event.height);
					}
				}
			});
			install.addListener(SWT.EraseItem, new Listener() {

				@Override
				public void handleEvent(Event event) {

					event.detail &= ~SWT.FOREGROUND;
					if (event.item.getData() == null)
						event.detail &= ~SWT.SELECTED;
				}
			});
		}

		protected void drawAccelerator(GC gc, String string, int x, int y, int width, int height) {

			gc.setForeground(Launcher.display.getSystemColor(SWT.COLOR_BLACK));
			gc.setFont(smallerFont);
			Point m = gc.textExtent(string);
			gc.drawText(string, x + width - m.x - 10, y + height - m.y - 5, true);
		}

		protected Point measure(String textToDraw, GC gc) {
			return measure(textToDraw, gc, null);
		}

		protected Point measure(String textToDraw, GC gc, Font fixedFont) {

			if (SEP_PATTERN.matcher(textToDraw).matches())
				return new Point(200, 10);

			textToDraw = textToDraw.replace("<b><i>", "<bi>");
			textToDraw = textToDraw.replace("</i></b>", "</bi>");

			List<Area> area = new ArrayList<Area>();

			if (fixedFont == null) {
				Matcher m = SMALLER_PATTERN.matcher(textToDraw);
				while (m.find()) {
					area.add(new Area(m.start(), m.end(), m.group(2), smallerFont));
				}
				m = SMALLER2_PATTERN.matcher(textToDraw);
				while (m.find()) {
					area.add(new Area(m.start(), m.end(), m.group(2), smallerFont));
				}
				m = BOLDITALIC_PATTERN.matcher(textToDraw);
				while (m.find()) {
					area.add(new Area(m.start(), m.end(), m.group(2), boldItalicFont));
				}

				m = BOLD_PATTERN.matcher(textToDraw);
				while (m.find()) {
					area.add(new Area(m.start(), m.end(), m.group(2), boldFont));
				}

				m = ITALIC_PATTERN.matcher(textToDraw);
				while (m.find()) {
					area.add(new Area(m.start(), m.end(), m.group(2), italicFont));
				}

				m = GREY_PATTERN.matcher(textToDraw);
				while (m.find()) {
					area.add(new Area(m.start(), m.end(), m.group(2), greyFont));
				}

				Collections.sort(area, new Comparator<Area>() {
					@Override
					public int compare(Area o1, Area o2) {
						return Float.compare(o1.start, o2.start);
					}
				});
				gc.setFont(normalFont);
			} else {
				gc.setFont(fixedFont);
			}
			int cx = 0;

			int index = 0;
			int areaIndex = 0;
			while (index < textToDraw.length()) {
				if (areaIndex < area.size() && index >= area.get(areaIndex).start) {
					Area a = area.get(areaIndex);

					gc.setFont(a.font);
					a.text = a.text.replaceAll(CLEAN_PATTERN.pattern(), "");

					cx += gc.textExtent(a.text).x;

					areaIndex++;
					index = a.end;
				} else {
					int start = index;
					int end = areaIndex < area.size() ? area.get(areaIndex).start : textToDraw.length();

					gc.setFont(normalFont);

					cx += gc.textExtent(textToDraw.substring(start, end)).x;

					index = end;
				}
			}

			gc.setFont(normalFont);

			return new Point(cx, gc.textExtent(textToDraw).y);
		}

		public class Area {
			int start;
			int end;
			String text;
			Font font;
			boolean dim = false;

			boolean blank = false;

			public Area(int start, int end, String text, Font font) {
				super();
				this.start = start;
				this.end = end;
				this.text = text;
				this.font = font;
			}

			public Area setDim(boolean dim) {
				this.dim = dim;
				return this;
			}

			public Area setBlank(boolean blank) {
				this.blank = blank;
				return this;
			}

		}

		protected void draw(String textToDraw, GC gc, int x, int y) {
			draw(textToDraw, gc, x, y, null);
		}

		protected void draw(String textToDraw, GC gc, int x, int y, Font fixedFont) {

			if (SEP_PATTERN.matcher(textToDraw).matches()) {
				int height = 10;

				gc.setForeground(Launcher.display.getSystemColor(SWT.COLOR_GRAY));
				gc.setBackground(Launcher.display.getSystemColor(SWT.COLOR_GRAY));
				gc.drawLine(x, y + height / 2, x + 200, y + height / 2);
				return;
			}

			if (textToDraw.contains("<grey>")) {
				textToDraw = textToDraw.replace("<grey>", "");
				gc.setAlpha(128);
			}

			List<Area> area = new ArrayList<Area>();

			textToDraw = textToDraw.replace("<b><i>", "<bi>");
			textToDraw = textToDraw.replace("</i></b>", "</bi>");

			// ;//System.out.println(" looking at <" + textToDraw +
			// ">");
			if (fixedFont == null) {
				Matcher m = SMALLER_PATTERN.matcher(textToDraw);
				while (m.find()) {
					area.add(new Area(m.start(), m.end(), m.group(2), smallerFont));
				}
				m = SMALLER2_PATTERN.matcher(textToDraw);
				while (m.find()) {
					area.add(new Area(m.start(), m.end(), m.group(2), smallerFont));
				}

				m = BOLDITALIC_PATTERN.matcher(textToDraw);
				while (m.find()) {
					area.add(new Area(m.start(), m.end(), m.group(2), boldItalicFont));
				}

				m = BOLD_PATTERN.matcher(textToDraw);
				while (m.find()) {
					area.add(new Area(m.start(), m.end(), m.group(2), boldFont));
				}

				m = ITALIC_PATTERN.matcher(textToDraw);
				while (m.find()) {
					area.add(new Area(m.start(), m.end(), m.group(2), italicFont));
				}

				m = GREY_PATTERN.matcher(textToDraw);
				while (m.find()) {
					area.add(new Area(m.start(), m.end(), m.group(2), greyFont).setDim(true));
				}

				Collections.sort(area, new Comparator<Area>() {
					@Override
					public int compare(Area o1, Area o2) {
						return Float.compare(o1.start, o2.start);
					}
				});
				gc.setFont(normalFont);
			} else {
				gc.setFont(fixedFont);
			}
			int cx = 0;

			int dasc = gc.getFontMetrics().getAscent();

			int index = 0;
			int areaIndex = 0;
			while (index < textToDraw.length()) {
				if (areaIndex < area.size() && index >= area.get(areaIndex).start) {
					Area a = area.get(areaIndex);

					gc.setFont(a.font);
					int asc = gc.getFontMetrics().getAscent();

					a.text = a.text.replaceAll(CLEAN_PATTERN.pattern(), "");

					gc.setForeground(a.dim ? new Color(Launcher.getLauncher().display, 90, 90, 90) : Launcher.getLauncher().display.getSystemColor(SWT.COLOR_BLACK));

					gc.drawText(a.text, cx + x, y + dasc - asc, true);

					cx += gc.textExtent(a.text).x;

					areaIndex++;
					index = a.end;
				} else {
					int start = index;
					int end = areaIndex < area.size() ? area.get(areaIndex).start : textToDraw.length();
					gc.setForeground(Launcher.getLauncher().display.getSystemColor(SWT.COLOR_BLACK));

					gc.setFont(fixedFont != null ? fixedFont : normalFont);
					String ttd = textToDraw.substring(start, end).replaceAll(CLEAN_PATTERN.pattern(), "");
					if (areaIndex >= area.size() || !area.get(areaIndex).blank) {
						gc.drawText(ttd, cx + x, y, true);
					}

					cx += gc.textExtent(ttd).x;

					index = end;
				}
			}
			gc.setAlpha(255);
		}

	}
}
