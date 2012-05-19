package field.core.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Decorations;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.Constants;
import field.core.Platform;
import field.core.Platform.OS;
import field.core.ui.SimpleVoronoi.Pnt;
import field.core.ui.SmallMenu.BetterPopup;
import field.core.uii.HTMLLabelTools;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.linalg.Vector2;
import field.namespace.generic.ReflectionTools;

@Woven
public class PopupMarkingArea {

	Shell shell;

	int width = 600;
	int height = 400;
	int boxWidth = 200;
	int boxHeight = 20;

	public enum Position {
		N(0, -1), S(0, 1), E(1, 0), W(-1, 0), NE(0.5, -0.5), NW(-0.5, -0.5), SE(0.5, 0.5), SW(-0.5, 0.5), Z(0, 0), NW2(-1, -1), NE2(1, -1), SE2(1, 1), SW2(-1, 1), NH(0, -0.5), SH(0, 0.5), EH(0.5, 0), WH(-0.5, 0), N2(0, -1.5), W2(-1.5, 0);

		final double x, y;

		Position(double x, double y) {
			this.x = x;
			this.y = y;
		}
	}

	List<MenuArea> components = new ArrayList<MenuArea>();

	MenuArea opened = null;

	public boolean inverted = false;

	public void setInverted(boolean inverted) {
		this.inverted = inverted;
	}

	HTMLLabelTools htmlLabelTools = new HTMLLabelTools();

	@Woven
	public class MenuArea {
		Vector2 labelAt = new Vector2();
		private final String label;
		private LinkedHashMap menu;

		public MenuArea(String label, LinkedHashMap<String, iUpdateable> menu, Vector2 labelAt, iUpdateable mainCallback) {
			this.label = label;
			this.menu = menu;
			this.labelAt = labelAt;
			this.callback = mainCallback;
		}

		boolean popped = false;
		protected BetterPopup pop;
		public Pnt site;
		public Region area;
		public Region textArea;
		public Path parea;
		protected float armAmount;

		public void paint(GC g) {

			Point extents = g.textExtent(label);
			extents = htmlLabelTools.measure(label, g);
			g.setAdvanced(true);

			int outset = 10;
			g.drawRoundRectangle((int) (labelAt.x - extents.x / 2) - outset, (int) (labelAt.y - extents.y / 2) - outset, (int) (extents.x) + outset * 2, (int) (extents.y) + outset * 2, 4, 4);

			float a = (popped && (pop != null && pop.menu.isVisible())) ? 0.5f : 0.6f;
			float b = 0.4f;

			float blue = (popped && (pop != null && pop.menu.isVisible())) ? 0.7f : 0.7f * armAmount;
			int oo = Platform.isMac() ? 0 : 128;
			Color paint = new Color(Launcher.display, oo + (int) (0 * blue), oo + (int) (0 * blue), oo + (int) (128 * blue));

			g.setAlpha((int) (20 + 100 * blue));
			g.drawRoundRectangle((int) (labelAt.x - extents.x / 2) - outset, (int) (labelAt.y - extents.y / 2) - outset, (int) (extents.x) + outset * 2, (int) (extents.y) + outset * 2, 4, 4);

			g.setLineWidth(3);
			g.setAlpha((int) (20 + 100 * blue) / 3);
			g.drawRoundRectangle((int) (labelAt.x - extents.x / 2) - outset, (int) (labelAt.y - extents.y / 2) - outset, (int) (extents.x) + outset * 2, (int) (extents.y) + outset * 2, 4, 4);
			g.setLineWidth(1);

			g.setBackground(paint);
			g.setAlpha((int) (20 + 100 * blue));

			g.fillRoundRectangle((int) (labelAt.x - extents.x / 2) - outset, (int) (labelAt.y - extents.y / 2) - outset, (int) (extents.x) + outset * 2, (int) (extents.y) + outset * 2, 4, 4);

			g.setAlpha((int) (20 + 100 * blue));
			g.drawRoundRectangle((int) (labelAt.x - extents.x / 2) - outset, (int) (labelAt.y - extents.y / 2) - outset, (int) (extents.x) + outset * 2, (int) (extents.y) + outset * 2, 4, 4);

			g.setAlpha(255);

			g.setForeground(Launcher.display.getSystemColor(SWT.COLOR_BLACK));
			// g.drawText(label, (int) (labelAt.x - extents.x / 2),
			// (int) (labelAt.y - extents.y / 2), true);

			htmlLabelTools.draw(label, g, (int) (labelAt.x - extents.x / 2), (int) (labelAt.y - extents.y / 2));

			if (poppedAt != null) {
				paintSplat(g, poppedAt);

			}
		}

		private String wrap(String label) {
			if (!label.contains("<")) {
				return "<html><font face='" + Constants.defaultFont + "' color=#000><p align='center'>" + label;
			}
			return label;

		}

		public void paintArea(GC g) {

			if (true)
				return;

			g.setBackground(Launcher.display.getSystemColor(SWT.COLOR_BLACK));
			g.setAlpha(2);

			if (popped && (pop != null && pop.menu.isVisible()) || armAmount > 0) {
				float a = true ? 0.5f : 0.6f;
				float b = 0.5f;

				float blue = true ? 0.07f : 0f;
				Color paint = new Color(Launcher.display, 0, 0, (int) (255 * blue));

				g.setBackground(paint);
				g.fillPath(parea);
			} else {
				float a = 0.6f;
				g.setBackground(Launcher.display.getSystemColor(SWT.COLOR_BLACK));
				g.fillPath(parea);
			}

			int size = 10;
			for (int n = size; n >= 1; n -= 1) {
				g.setLineWidth(n);
				if (inverted) {
					g.setBackground(Launcher.display.getSystemColor(SWT.COLOR_BLACK));
					g.setAlpha(2);
				} else {
					g.setBackground(Launcher.display.getSystemColor(SWT.COLOR_BLACK));
					g.setAlpha(2);
				}
				g.drawPath(parea);
			}

		}

		Vector2 poppedAt = null;

		public void pop(Event e) {

			System.out.println(" popping up ? ");

			if (menu == null) {

				for (MenuArea m : PopupMarkingArea.this.components) {
					if (m.pop != null) {
						m.pop.shell.setVisible(false);
					}
				}
				popped = true;
				return;
			}

			pop = (BetterPopup) new SmallMenu().createMenu(menu, shell, null);
			Point m = Launcher.display.map((Control) e.widget, shell, new Point(e.x, e.y));
			System.out.println(" popping up <" + e + ">");

			pop.doneHook = new iUpdateable() {
				@Override
				public void update() {
					closeThis();
				}
			};

			pop.show(new Point(m.x, m.y));
			popped = true;
			poppedAt = new Vector2(e.x, e.y);
			shell.redraw();

		}

		@NextUpdate(delay = 10)
		public void pop(int x, int y) {
			System.out.println(" popping up ? ");

			if (menu == null) {

				for (MenuArea m : PopupMarkingArea.this.components) {
					if (m.pop != null) {
						m.pop.shell.setVisible(false);
					}
				}
				popped = true;
				return;
			}
			pop = (BetterPopup) new SmallMenu().createMenu(menu, shell, null);
			Point m = Launcher.display.map(null, shell, new Point(x, y));

			pop.doneHook = new iUpdateable() {

				@Override
				public void update() {
					closeThis();
				}
			};

			System.out.println(" create menu :" + menu + " " + shell.getLocation() + " " + x + " " + y);

			pop.show(new Point(x, y));
			popped = true;
			poppedAt = new Vector2(x, y);
			shell.redraw();
		}

		iUpdateable callback;

		public Region getTextArea() {

			GC g = new GC(shell);

			Point extents = g.textExtent(label);
			extents = htmlLabelTools.measure(label, g);

			int outset = 10;

			Region r = new Region(Launcher.display);
			r.add(new Rectangle(((int) (labelAt.x - extents.x / 2)) - outset, (int) (labelAt.y - extents.y / 2) - outset, 1 + (int) (extents.x) + outset * 2, 1 + (int) (extents.y) + outset * 2));

			g.dispose();
			return r;
		}
	}

	private Point mouseAt;
	private iUpdateable timer;

	MenuArea hoverToOpen = null;
	boolean hoverFast = false;
	Event hoverEvent = null;

	List<Vector2> gesture = new ArrayList<Vector2>();
	private final Control invoker;
	private Pnt centerSite;
	private Region centerArea;

	private int realLocation;

	static public class PopMenuSpec {
		public Position position;
		public LinkedHashMap<String, iUpdateable> menu;
		public iUpdateable mainCallback;
		public String mainLabel;

		public boolean initiallyOpen = false;
		public boolean solo = false;

		public PopMenuSpec(Position position, LinkedHashMap<String, iUpdateable> menu, iUpdateable mainCallback, String mainLabel) {
			super();
			this.position = position;
			this.menu = menu;
			this.mainCallback = mainCallback;
			this.mainLabel = mainLabel;
		}

		public PopMenuSpec copy() {

			PopMenuSpec r = new PopMenuSpec(position, new LinkedHashMap<String, iUpdateable>(menu), mainCallback, mainLabel);
			r.initiallyOpen = initiallyOpen;
			r.solo = solo;

			return r;

		}
	}

	double distanceMoved = 0;

	public PopupMarkingArea(Control invoker, Point screenPoint, List<PopMenuSpec> spec) {
		this.invoker = invoker;
		final long openedAt = System.currentTimeMillis();

		shell = new Shell(Launcher.display, SWT.NO_TRIM | SWT.ON_TOP);
		// shell = new Shell(Launcher.display, 0);

		shell.setBackground(new Color(Launcher.display, 180, 180, 180));

		shell.setBounds(1000000000, screenPoint.y - height / 2, 0, 0);
		// shell.setBounds(screenPoint.x, screenPoint.y - height / 2, 0,
		// 0);
		boolean noFakePositionWorkaround = false;

		for (PopMenuSpec s : spec) {
			MenuArea ma = new MenuArea(s.mainLabel, wrap(s.menu), new Vector2(width / 2 + width / 4 * s.position.x, height / 2 + height / 4 * s.position.y), s.mainCallback);
			components.add(ma);
			if (s.initiallyOpen) {

				shell.setBounds(screenPoint.x - width / 2, screenPoint.y - height / 2, width, height);
				noFakePositionWorkaround = true;

				ma.pop((int) ma.labelAt.x, (int) ma.labelAt.y);
				opened = ma;
				ma.armAmount = 0.5f;
			}
		}

		buildVoronoi();

		shell.addListener(SWT.MouseMove, new Listener() {

			@Override
			public void handleEvent(Event event) {
				if (!event.doit)
					return;

				System.out.println(" event in mouse move");

				hoverToOpen = null;
				Point loc = new Point(event.x, event.y);

				loc = Launcher.display.map((Control) event.widget, shell, loc);
				clampToWindow(loc);

				event.x = loc.x;
				event.y = loc.y;

				gesture.add(new Vector2(loc.x, loc.y));
				handleMouse(event, loc);

				event.doit = false;
			}
		});

		shell.addListener(SWT.MouseUp, new Listener() {

			@Override
			public void handleEvent(Event event) {
				System.out.println(" mouse up, who knows?");
			}
		});

		shell.addListener(SWT.Paint, new Listener() {

			@Override
			public void handleEvent(Event event) {
				for (MenuArea a : components) {
					a.paintArea(event.gc);
				}
				for (MenuArea a : components) {
					a.paint(event.gc);
				}
			}
		});

		Launcher.display.addFilter(SWT.MouseMove, new Listener() {

			@Override
			public void handleEvent(Event event) {

				if (Launcher.display.getFocusControl() == shell) {
					System.out.println(" event in FILTERED mouse move");

					hoverToOpen = null;
					Point loc = new Point(event.x, event.y);

					loc = Launcher.display.map((Control) event.widget, shell, loc);
					clampToWindow(loc);

					int wasx = event.x;
					int wasy = event.y;

					event.x = loc.x;
					event.y = loc.y;

					gesture.add(new Vector2(loc.x, loc.y));
					handleMouse(event, loc);

					event.doit = false;

					event.x = wasx;
					event.y = wasy;
				}
			}
		});

		final long timeOpen = System.currentTimeMillis();

		Listener f = new Listener() {
			Point m = null;

			@Override
			public void handleEvent(Event event) {

				if (event.type == SWT.MouseUp) {
					if (Launcher.display.getFocusControl() == shell) {
						// need to select a button here,
						// if the
						// button has no menu
						if (opened != null && opened.pop != null && opened.pop.menu.isVisible()) {
						}

						if ((System.currentTimeMillis() - timeOpen) > 500 || distanceMoved > 10)
						{
							closeThis();
							Launcher.display.removeFilter(SWT.MouseUp, this);
							Launcher.display.removeFilter(SWT.MouseMove, this);
						}

					} else {

						if ((System.currentTimeMillis() - timeOpen) > 500)
						{
							shell.setVisible(false);
							Launcher.display.removeFilter(SWT.MouseUp, this);
							Launcher.display.removeFilter(SWT.MouseMove, this);
						}
					}
				}
				
				if (m == null) {
					m = new Point(event.x, event.y);
				} else {
					distanceMoved += Math.abs(event.x - m.x) + Math.abs(event.y - m.y);
					m = new Point(event.x, event.y);
				}
			}
		};
		Launcher.display.addFilter(SWT.MouseUp, f);
		Launcher.display.addFilter(SWT.MouseMove, f);

		timer = new iUpdateable() {

			int sameFor = 0;
			MenuArea last = null;

			int badFocus = 0;

			public void update() {

				if (hoverToOpen == null)
					sameFor = 0;
				else if (hoverToOpen != last)
					sameFor = 0;
				else {
					sameFor++;
					if (hoverFast && sameFor < 10)
						sameFor = 10;
					if (sameFor == 10) {
						if (hoverToOpen != opened)
							hoverToOpen.pop(hoverEvent);
						shell.redraw();
						opened = hoverToOpen;

						for (MenuArea m : components) {
							m.armAmount = m == opened ? 1 : 0;
						}

					} else if (sameFor < 10) {
						for (MenuArea m : components) {
							m.armAmount = m == hoverToOpen ? sameFor / 10f : 0;
						}
						shell.redraw();
					}

				}
				last = hoverToOpen;

				// focus.
				// focus can either be the shell or something
				// that we've popped
				// open
				// if it isn't for more than three ticks then
				// abandon the whole
				// thing

				Control control = Launcher.display.getFocusControl();

				// System.out.println(" focus is <" + control +
				// "> <"
				// + (control == shell) + "> <"
				// + (control instanceof Table) +
				// "> <"+badFocus+">");

				if ((control == shell) || (control instanceof Table)) {
					badFocus = 0;
				} else if (control == null) {
					shell.forceFocus();
					badFocus++;
				} else {
					badFocus++;
				}

//				if (badFocus > 50) {
//					
//					System.out.println(" focus was <"+control+">");
//					
//					closeThis();
//				}
			}
		};

		Launcher.getLauncher().registerUpdateable(timer);

		realLocation = screenPoint.x - width / 2;
		shell.setLocation((!noFakePositionWorkaround && Platform.getOS() == OS.mac) ? 100000 : realLocation, screenPoint.y - height / 2);
		openNext();

		System.out.println("\n\n opening pop at <" + shell.getBounds() + ">\n\n");

	}

	@NextUpdate
	private void openNext() {
		shell.open();
		shell.setSize(width, height);
	}

	protected void handleMouse(Event event, Point loc) {
		System.out.println(" entering handlemouse <" + event + " @ " + loc + ">");

		shell.redraw();

		if (centerArea != null && centerArea.contains(loc)) {

			// probably need this to close anything poped

			if (opened != null)
				if (opened.pop != null)
					opened.pop.shell.setVisible(false);

			hoverToOpen = null;

			for (MenuArea m : components) {
				m.armAmount = 0;
			}

			opened = null;

			System.out.println(" in middle ");
		} else

			for (MenuArea b : components) {

				float d1 = (float) Math.sqrt((loc.x - b.labelAt.x) * (loc.x - b.labelAt.x) + (loc.y - b.labelAt.y) * (loc.y - b.labelAt.y));
				float d2 = b.area.contains(new Point(Math.min(shell.getSize().x - 1, Math.max(1, loc.x)), Math.min(shell.getSize().y - 1, Math.max(1, loc.y)))) ? 0 : 1;

				if (d1 < 20) {
					System.out.println(" d1<20, <" + opened + "> <" + b + ">");
					if (opened != b) {
						if (opened != null) {
							if (opened.pop != null) {
								opened.pop.shell.setVisible(false);
							}

							// if
							// (opened.check(mouseAt))
							// continue;
						}

						hoverToOpen = b;
						hoverEvent = event;
						hoverFast = true;

						System.out.println(" now we have <" + hoverToOpen + "> <" + event + ">");

						// b.pop((MouseEvent)
						// event);
						// opened
						// = b;
					}
				} else if (d2 < 1) {
					if (opened != b) {

						if (opened != null) {
							if (opened.pop != null) {
								opened.pop.shell.setVisible(false);

							}
						}

						hoverToOpen = b;
						hoverEvent = event;
						hoverFast = false;

						// b.pop((MouseEvent)event);
						// opened
						// = b;
					} else {

					}
				} else {
				}
			}
	}

	protected void clampToWindow(Point loc) {
		if (loc.x < 0)
			loc.x = 55;
		if (loc.y < 0)
			loc.y = 55;
		if (loc.x > shell.getSize().x)
			loc.x = shell.getSize().x - 55;
		if (loc.y > shell.getSize().y)
			loc.y = shell.getSize().y - 55;
	}

	private LinkedHashMap<String, iUpdateable> wrap(LinkedHashMap<String, iUpdateable> menu) {
		if (menu == null)
			return null;

		LinkedHashMap<String, iUpdateable> ret = new LinkedHashMap<String, iUpdateable>();
		Set<Entry<String, iUpdateable>> es = menu.entrySet();
		for (final Entry<String, iUpdateable> e : es) {

			ret.put(e.getKey(), e.getValue() == null ? null : new iUpdateable() {

				public void update() {
					e.getValue().update();
					closeThis();
				}
			});
		}
		return ret;
	}

	protected void buildVoronoi() {
		SimpleVoronoi v = new SimpleVoronoi();

		centerArea = null;
		boolean hasCenter = false;

		for (MenuArea m : components) {
			m.site = v.add(m.labelAt);

			System.out.println(" voronoi :" + m.labelAt);

			if (m.labelAt.distanceFrom(new Vector2(width / 2, height / 2)) < 5)
				hasCenter = true;
		}
		if (!hasCenter) {
			System.out.println(" missing center ");
			centerSite = v.add(new Vector2(width / 2, height / 2));
		}

		float f = 1.1f;
		v.add(new Vector2(width / 2, height * 0));
		v.add(new Vector2(width / 2, height * f));
		v.add(new Vector2(width * f, height / 2));
		v.add(new Vector2(width * 0, height / 2));

		for (MenuArea m : components) {
			m.area = v.makeRegion(v.getContourForSite(m.site));
			m.parea = v.makePath(v.getContourForSite(m.site));
		}

		if (centerSite != null)
			centerArea = v.makeRegion(v.getContourForSite(centerSite));

		Region all = new Region();
		for (MenuArea m : components) {
			all.add(m.getTextArea());
		}

		if (!hasCenter) {
			System.out.println(" center area is <" + centerArea + ">");
			// all.add(centerArea);
			// centerArea = new Region();
			// centerArea.add(0, 0, 1, 1);
			// all.add(centerArea);
		}

		System.out.println(" region bounds are :"+all.getBounds());
		Rectangle r = all.getBounds();
		
		// work around SWT issue on Linux
		if (!Platform.isMac())
			all.add(r.x-100, r.y+100, 1, 1);
		
		shell.setRegion(all);

		if (Platform.getOS() == OS.mac) {
			Object ww = ReflectionTools.illegalGetObject(shell, "window");
			try {
				ReflectionTools.findFirstMethodCalled(ww.getClass(), "setHasShadow").invoke(ww, true);
				ReflectionTools.findFirstMethodCalled(ww.getClass(), "setAlphaValue").invoke(ww, 0.85f);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}

			Launcher.getLauncher().registerUpdateable(new iUpdateable() {

				@Override
				public void update() {
					// ((NSWindow)
					// ReflectionTools.illegalGetObject(shell,
					// "window"))
					// .invalidateShadow();
					// ((NSWindow)
					// ReflectionTools.illegalGetObject(shell,
					// "window"))
					// .setAlphaValue(0.85f);
					Launcher.getLauncher().deregisterUpdateable(this);

					shell.setLocation(realLocation, shell.getLocation().y);
				}
			});
		} else
			shell.setLocation(realLocation, shell.getLocation().y);

	}

	int paint = 0;

	// public void paintInside(Graphics g) {
	// if (paint == 0) {
	// // createBufferStrategy(2);
	// }
	//
	// Composite was = ((Graphics2D) g).getComposite();
	// ((Graphics2D) g).setComposite(AlphaComposite.Src);
	// g.setColor(new Color(0, 0, 0, 0.0f));
	// g.fillRect(0, 0, width, height);
	// ((Graphics2D) g).setComposite(was);
	//
	// // g = getBufferStrategy().getDrawGraphics();
	//
	// System.out.println(" clearing background ? ");
	//
	// HashMap hints = new HashMap();
	// hints.put(RenderingHints.KEY_ANTIALIASING,
	// RenderingHints.VALUE_ANTIALIAS_ON);
	// ((Graphics2D) g).addRenderingHints(hints);
	//
	// was = ((Graphics2D) g).getComposite();
	// ((Graphics2D) g).setComposite(AlphaComposite.Src);
	// g.setColor(new Color(0, 0, 0, 0.0f));
	// g.fillRect(0, 0, width, height);
	//
	// ((Graphics2D) g).setComposite(was);
	//
	// // super.paint(g);
	//
	// for (MenuArea a : components) {
	// a.paintArea((Graphics2D) g);
	// }
	//
	// paintSplat((Graphics2D) g, new Vector2(width / 2, height / 2));
	//
	// if (gesture.size() > 1) {
	// GeneralPath p = new GeneralPath();
	//
	// for (int i = 0; i < gesture.size(); i++) {
	// if (i == 0)
	// p.moveTo(gesture.get(i).x, gesture.get(i).y);
	// else
	// p.lineTo(gesture.get(i).x, gesture.get(i).y);
	// }
	// ((Graphics2D) g).setColor(new Color(0, 0, 0, 0.1f));
	// ((Graphics2D) g).draw(p);
	// }
	//
	// for (MenuArea a : components) {
	// a.paint((Graphics2D) g);
	// }
	//
	// if (paint++ == 1)
	// this.getRootPane().putClientProperty(
	// "apple.awt.windowShadow.revalidateNow", new Object());
	// // getBufferStrategy().show();
	// }

	protected void closeThis() {

		System.out.println(" close this ");

		new Exception().printStackTrace();

		float max = 0;
		MenuArea best = null;
		for (MenuArea m : components) {
			if (m.armAmount > max) {
				max = m.armAmount;
				best = m;
			}
		}

		if (best == null) {
			Point loc = Launcher.display.map(null, shell, Launcher.display.getCursorLocation());
			for (MenuArea b : components) {

				float d2 = b.area.contains(new Point((int) Math.min(shell.getSize().x - 15, (int) Math.max(15, loc.x)), Math.min(shell.getSize().y - 15, (int) Math.max(15, loc.y)))) ? 0 : 1;

				if (d2 < 1)
					best = b;
			}
		}

		if (best != null) {
			if (best.callback != null) {
				System.out.println("\n\n\n executing area callback \n\n\n");
				best.callback.update();
			}
			best.callback = null;

		}
			
		Launcher.getLauncher().deregisterUpdateable(timer);
		closeThisDelayed();
	}

	@NextUpdate(delay = 2)
	protected void closeThisDelayed() {
		shell.setVisible(false);

		// setLocation(new Point(400,400));

		System.out.println(" will grab back focus ");
		// grabBackFocus();

	}

	// @NextUpdate(delay = 2)
	// protected void grabBackFocus() {
	// // if (true)
	// // return;
	//
	// Window window = invoker instanceof Window ? (Window) invoker
	// : SwingUtilities.getWindowAncestor(invoker);
	// System.out.println(" trying to get this to the front <" + window
	// + " / <" + invoker + ">");
	//
	// try {
	// GLComponentWindow inside = (GLComponentWindow) ReflectionTools
	// .findFirstMethodCalled(window.getClass(),
	// "getGLComponentWindow").invoke(window);
	// GLCanvas canvas = inside.getCanvas();
	// System.out.println(" forcing focus to window ? <" + canvas + ">");
	// window.requestFocus();
	// canvas.requestFocusInWindow();
	// } catch (Exception e) {
	// // e.printStackTrace();
	// System.out
	// .println(" failed to focus window (probably just fine...)");
	// }
	// // window.toFront();
	// // window.requestFocus();
	//
	// }

	protected void paintSplat(GC g, Vector2 poppedAt) {
		int size = 3;
		g.setBackground(new Color(Launcher.display, 0, 0, 0));
		g.setAlpha(40);
		g.fillRectangle((int) poppedAt.x - size, (int) poppedAt.y - size, size * 2, size * 2);
	}

	@NextUpdate(delay = 2)
	protected void checkFocus() {
		if (Launcher.display.isDisposed())
			return;

		Control control = Launcher.display.getFocusControl();

		System.out.println(" -- focus out to control <" + control + ">  = " + (control == null ? null : control.getBounds()) + " (" + System.identityHashCode(control) + "> shell is <" + System.identityHashCode(shell) + ">");

		// TODO swt \u2014 bit of a hack this one

		if (control != null && !(control instanceof Table) && control != shell) {

			System.out.println(" forcing a close");

			shell.dispose();
		}
	}

}
