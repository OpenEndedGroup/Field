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
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.rmi.server.UID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import javax.swing.JPopupMenu;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;

import field.core.Constants;
import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.ui.SmallMenu;
import field.core.ui.text.embedded.CustomInsertSystem.ExecutesWhat;
import field.core.ui.text.embedded.CustomInsertSystem.ExecutesWhen;
import field.core.ui.text.embedded.CustomInsertSystem.ProvidedComponent;
import field.core.windowing.GLComponentWindow;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.linalg.Vector2;
import field.math.util.BreakpointFloat;
import field.math.util.CubicInterpolatorDynamic;
import field.math.util.BreakpointFloat.Next;
import field.math.util.CubicInterpolatorDynamic.Sample;
import field.namespace.key.OKey;
import field.util.PythonUtils;

public class MinimalGraphWidget2 extends MinimalExpandable implements iRegistersMinimalLayerPainter {

	static public class Component_tuple extends ProvidedComponent {

		transient public iUpdateable notify;

		public CubicInterpolatorDynamic<BreakpointFloat> interpolator;

		protected ExecutesWhen when;

		protected ExecutesWhat what;
		String name = "Component_graph_provider:" + new UID().toString() + ".transient";
		OKey<CubicInterpolatorDynamic<BreakpointFloat>> localKey;

		@Override
		public void deserialize(iVisualElement inside) {
			super.deserialize(inside);
			if (when == null)
				when = ExecutesWhen.never;
			if (what == null)
				what = ExecutesWhat.line;

			component = new MinimalGraphWidget2() {
				@Override
				public void execute() {
					executeThisLine(irc, what);
				}

				@Override
				protected void setValue(int on, float x, float y) {
					super.setValue(on, x, y);

					updateValue();
				};
			};

			if (interpolator == null) {
				interpolator = new CubicInterpolatorDynamic<BreakpointFloat>();
				interpolator.new Sample(new BreakpointFloat(0.5f), 0);
				interpolator.new Sample(new BreakpointFloat(0.7f), 0.5f);
				interpolator.new Sample(new BreakpointFloat(0.5f), 1);
			}

			((MinimalGraphWidget2) component).setInterpolator(interpolator);
			((MinimalGraphWidget2) component).when = when;
			((MinimalGraphWidget2) component).what = what;

		}

		@Override
		public String getCurrentRepresentedString() {
			if (localKey == null)
				localKey = new OKey<CubicInterpolatorDynamic<BreakpointFloat>>(name).rootSet(interpolator);
			return "OKeyByName(\"" + name + "\", u.fromXML(r\"\"\"" + new PythonUtils().toXML(interpolator, true) + "\"\"\"))";

		}

		@Override
		public void makeNew() {
			name = "Component_2graph_provider:" + new UID().toString() + ".transient";
			localKey = new OKey<CubicInterpolatorDynamic<BreakpointFloat>>(name).rootSet(interpolator);
		}

		@Override
		public void preserialize() {
			if (component != null)
				when = ((MinimalGraphWidget2) component).when;
			if (component != null)
				what = ((MinimalGraphWidget2) component).what;
		}

		protected void updateValue() {
			if (localKey == null)
				localKey = new OKey<CubicInterpolatorDynamic<BreakpointFloat>>(name).rootSet(interpolator);
			localKey.rootSet(interpolator);
			if (notify != null) {
				notify.update();
			}
		}
	}

	static public class Component_tuple2 extends ProvidedComponent {

		protected ExecutesWhen when;

		protected ExecutesWhat what;

		String name = "Component_2graph_provider:" + new UID().toString() + ".transient";
		OKey<InterpolatorWithWidget> localKey;
		InterpolatorWithWidget interpolator;

		@Override
		public void deserialize(iVisualElement inside) {
			super.deserialize(inside);
			if (when == null)
				when = ExecutesWhen.never;
			if (what == null)
				what = ExecutesWhat.line;

			component = new MinimalGraphWidget2() {
				@Override
				public void execute() {
					executeThisLine(irc, what);
				}

				@Override
				protected void setValue(int on, float x, float y) {
					super.setValue(on, x, y);
				};
			};

			if (interpolator == null) {
				interpolator = new InterpolatorWithWidget();
				interpolator.new Sample(new BreakpointFloat(0.5f), 0);
				interpolator.new Sample(new BreakpointFloat(0.7f), 0.5f);
				interpolator.new Sample(new BreakpointFloat(0.5f), 1);
			}

			((MinimalGraphWidget2) component).setInterpolator(interpolator);
			((MinimalGraphWidget2) component).when = when;
			((MinimalGraphWidget2) component).what = what;

		}

		@Override
		public String getCurrentRepresentedString() {
			if (localKey == null)
				localKey = new OKey<InterpolatorWithWidget>(name).rootSet(interpolator);

			interpolator.widget = (iRegistersMinimalLayerPainter) component;
			return "transFilter(OKeyByName(\"" + name + "\", u.fromXML(r\"\"\"" + new PythonUtils().toXML(interpolator, true) + "\"\"\")).get())";
		}

		@Override
		public void makeNew() {
			name = "Component_2graph_provider:" + new UID().toString() + ".transient";
			localKey = new OKey<InterpolatorWithWidget>(name).rootSet(interpolator);
		}

		@Override
		public void preserialize() {
			if (component != null)
				when = ((MinimalGraphWidget2) component).when;
			if (component != null)
				what = ((MinimalGraphWidget2) component).what;

		}

		protected void updateValue() {
			if (localKey == null)
				localKey = new OKey<InterpolatorWithWidget>(name).rootSet(interpolator);
			interpolator.widget = (iRegistersMinimalLayerPainter) component;
			localKey.rootSet(interpolator);
		}
	}

	static public class InterpolatorWithWidget extends CubicInterpolatorDynamic<BreakpointFloat> {
		public transient iRegistersMinimalLayerPainter widget;

		public InterpolatorWithWidget() {
		}
	}

	public ExecutesWhat what = ExecutesWhat.line;
	private int initialDown;

	private int initialDownY;

	private Font font;

	private int initialDownOn;

	ExecutesWhen when = ExecutesWhen.never;

	CubicInterpolatorDynamic<BreakpointFloat> interpolator = new CubicInterpolatorDynamic<BreakpointFloat>();

	int sliderWidth = 12;

	int sliderHeight = 12;

	enum Dragging {
		off, on, controlBefore, controlAfter
	}

	Dragging on = Dragging.off;

	HashMap<String, iMinimalLayerPainter> layerPainters = new LinkedHashMap<String, iMinimalLayerPainter>();

	HashSet<iMinimalLayerPainter> first = new LinkedHashSet<iMinimalLayerPainter>();

	@Override
	public float getAlignmentY() {
		return alignment;
	}

	@Override
	public Insets getInsets() {
		return new Insets(-20, -20, -20, -20);
	}

	public CubicInterpolatorDynamic<BreakpointFloat> getInterpolator() {
		return interpolator;
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

	public iMinimalLayerPainter painterForName(String name) {
		return layerPainters.get(name);
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
	}

	public void setInterpolator(CubicInterpolatorDynamic<BreakpointFloat> interpolator) {
		this.interpolator = interpolator;
	}

	public iMinimalLayerPainter setPainterForName(String name, iMinimalLayerPainter p) {
		first.add(p);
		return layerPainters.put(name, p);
	}

	private int bottomY(int i) {
		return (int) ((this.getBounds().height - sliderHeight) * valueY(i) + sliderHeight);
	}

	private int leftX(int i) {
		return (int) ((this.getBounds().width - sliderWidth) * value(i));
	}

	private int rightX(int i) {
		return (int) ((this.getBounds().width - sliderWidth) * value(i) + sliderWidth);
	}

	private int topY(int i) {
		return (int) ((this.getBounds().height - sliderHeight) * valueY(i));
	}

	private float value(int i) {
		return interpolator.getSample(i).time;
	}

	private float valueY(int i) {
		return interpolator.getSample(i).data.value;
	}

	protected void drawBox(Graphics2D g2, CubicInterpolatorDynamic<BreakpointFloat>.Sample breakpointFilter, Vector2 p, CubicInterpolatorDynamic<BreakpointFloat>.Sample before, CubicInterpolatorDynamic<BreakpointFloat>.Sample after) {
		int width = 5;
		Rectangle r = new Rectangle((int) (p.x - width / 2), (int) (p.y - width / 2), width, width);
		g2.setColor(new Color(0.85f, 0.85f, 0.85f, 0.7f));

		// switch (breakpointFloat) {
		// case normal:
		// g2.setColor(new Color(0.85f, 0.85f, 0.85f, 0.7f));
		// break;
		// case sharp:
		// g2.setColor(new Color(0.3f, 0.85f, 0.85f, 0.7f));
		// break;
		// case fat:
		// g2.setColor(new Color(0.3f, 0.85f, 0.3f, 0.7f));
		// break;
		// case forward:
		// g2.setColor(new Color(0.3f, 0.3f, 0.85f, 0.7f));
		// break;
		// case backward:
		// g2.setColor(new Color(0.85f, 0.85f, 0.3f, 0.7f));
		// break;
		// }
		g2.setStroke(new BasicStroke(1));
		g2.draw(r);
		g2.fill(r);
		g2.setStroke(new BasicStroke(1, 0, 0, 1, new float[] { 1, 1, 1, 1 }, 0));
		GeneralPath path = new GeneralPath();
		path.moveTo(p.x - width * 4, p.y);
		path.lineTo(p.x + width * 4, p.y);
		path.moveTo(p.x, p.y - width * 4);
		path.lineTo(p.x, p.y + width * 4);
		g2.draw(path);

		if (after != null && breakpointFilter.data.next == Next.cubic) {
			Vector2 controlIs = transformRelativeToFrame(this.getBounds(), new Vector2(breakpointFilter.time + (after.time - breakpointFilter.time) / 3, breakpointFilter.data.controlNext));

			Ellipse2D.Float circle = new Ellipse2D.Float(controlIs.x - width / 2, controlIs.y - width / 2, width, width);
			g2.draw(new Line2D.Float(p.x, p.y, controlIs.x, controlIs.y));
			g2.draw(circle);
			g2.fill(circle);
		}
		if (before != null && before.data.next == Next.cubic) {
			Vector2 controlIs = transformRelativeToFrame(this.getBounds(), new Vector2(breakpointFilter.time + (before.time - breakpointFilter.time) / 3, breakpointFilter.data.controlPrevious));

			Ellipse2D.Float circle = new Ellipse2D.Float(controlIs.x - width / 2, controlIs.y - width / 2, width, width);
			g2.draw(new Line2D.Float(p.x, p.y, controlIs.x, controlIs.y));
			g2.draw(circle);
			g2.fill(circle);

		}
	}

	protected void execute() {
	}

	@Override
	protected void paintComponent(Graphics g) {
		Rectangle bounds = this.getBounds();

		Graphics2D g2 = (Graphics2D) g;

		// g2.setColor(new Color(0.22f, 0.22f, 0.23f, 0.4f));
		g2.setPaint(new GradientPaint(0, 0, new Color(1, 1, 1, 0.1f), 0, bounds.height, new Color(0, 0, 0, 0.1f)));

		GeneralPath path = new GeneralPath();
		path.moveTo(0, 0);
		path.lineTo(bounds.width - 1, 0);
		path.lineTo(bounds.width - 1, bounds.height - 1);
		path.lineTo(0, bounds.height - 1);
		path.lineTo(0, 0);
		g2.fill(path);
		g2.setPaint(new Color(0,0,0,1f));
		g2.draw(path);
		g2.draw(path);

		super.paintComponent(g);

		// first, lets just draw the curve itself
		int numSamples = 100;
		GeneralPath theCurve = new GeneralPath();
		Vector2 p = new Vector2();
		g2.setColor(new Color(0.3f, 0.33f, 0.43f, 0.7f));

		int lastIndex = -1;
		for (int i = 0; i < numSamples; i++) {
			float alpha = i / (numSamples + 1f);
			int prev = interpolator.findSampleIndexBefore(alpha);
			if (prev != lastIndex) {
				addPoint(bounds, theCurve, p, interpolator.getSample(prev).time);
				addPoint(bounds, theCurve, p, interpolator.getSample(prev).time + 1e-3f);
				lastIndex = prev;
			}
			addPoint(bounds, theCurve, p, alpha);
		}

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(new Color(0.0f, 0.0f, 0.0f, 0.35f));
		g2.setStroke(new BasicStroke(2));
		g2.draw(theCurve);
		g2.setColor(new Color(0.0f, 0.0f, 0.0f, 0.7f));
		g2.setStroke(new BasicStroke(1));
		g2.draw(theCurve);

		// next lets draw the control points

		for (int i = 0; i < interpolator.getNumSamples(); i++) {
			CubicInterpolatorDynamic<BreakpointFloat>.Sample sample = interpolator.getSample(i);
			transformRelativeToFrame(bounds, p.set(sample.time, sample.data.value));
			drawBox(g2, sample, p, i == 0 ? null : interpolator.getSample(i - 1), i == interpolator.getNumSamples() - 1 ? null : interpolator.getSample(i + 1));
		}

		// paint layers

		for (iMinimalLayerPainter painter : layerPainters.values()) {
			if (first.remove(painter)) {
				painter.associate(this);
			}
			painter.paintNow(g2, this.getSize());
		}

		enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
		
		g2.setPaint(new Color(0,0,0,1f));
		g2.draw(path);

		if (collapseness > 1)
			g2.drawImage(Constants.plus, bounds.width - 14, 2, 12, 12, null);
		else
			g2.drawImage(Constants.minus, bounds.width - 14, 2, 12, 12, null);


	}

	private void addPoint(Rectangle bounds, GeneralPath theCurve, Vector2 p, float alpha) {
		BreakpointFloat v = new BreakpointFloat(0);
		interpolator.getValue(alpha, v);
		transformRelativeToFrame(bounds, p.set(alpha, v.value));

		if (theCurve.getCurrentPoint() == null)
			theCurve.moveTo(p.x, p.y);
		else
			theCurve.lineTo(p.x, p.y);
	}

	protected LinkedHashMap<String, iUpdateable> popup() {
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
		
		return menu;
	}

	@Override
	protected void processMouseEvent(MouseEvent e) {
		super.processMouseEvent(e);

		if (collapseness > 0.5)
			return;


		int x = e.getX() - 0 * this.bounds().x;
		int y = this.getHeight() - (e.getY() - 0 * this.bounds().y);

		e.consume();

		if (e.getID() == e.MOUSE_PRESSED) {

			if (Platform.isPopupTrigger(e)) {

				initialDownOn = -1;
				for (int i = 0; i < interpolator.getNumSamples(); i++) {
					int leftX = leftX(i);
					int rightX = rightX(i);
					int bottomY = bottomY(i);
					int topY = topY(i);
					if (x >= leftX && x <= rightX && y >= topY && y <= bottomY) {
						initialDown = x;
						initialDownY = y;
						initialDownOn = i;
						on = Dragging.on;
						break;
					}
				}

				if (initialDownOn != -1) {
					LinkedHashMap<String, iUpdateable> items = new LinkedHashMap<String, iUpdateable>();

					items.put("Tangent type, currently \"" + interpolator.getSample(initialDownOn).data.next.toString() + "\"", null);
					for (final Next t : Next.values()) {
						String prefix = (t == interpolator.getSample(initialDownOn).data.next) ? "!" : "";
						items.put(prefix+"    " + t.toString(), new iUpdateable() {
							public void update() {
								interpolator.getSample(initialDownOn).data.next = t;
								repaint();
							}
						});
					}

					LinkedHashMap<String, iUpdateable> m = popup();

					items.putAll(m);
					//TODO swt

					Canvas canvas = irc.getControl();
					Point p = Launcher.display.map(canvas, irc.getControl().getShell(), new Point(e.getX(), e.getY()));
					
					new SmallMenu().createMenu(items, irc.getControl().getShell(), null).show(p);

//					JPopupMenu menu = new SmallMenu().createMenu(items);
					// menu.show(this, x, y);
//					pop(menu, e);
					
				} else {
					LinkedHashMap<String, iUpdateable> m = popup();
					Canvas canvas = irc.getControl();
					Point p = Launcher.display.map(canvas, irc.getControl().getShell(), new Point(e.getX(), e.getY()));
					
					new SmallMenu().createMenu(m, irc.getControl().getShell(), null).show(p);

					//TODO swt
//					JPopupMenu menu = new SmallMenu().createMenu(m);
					// menu.show(this, x, y);
//					pop(menu, e);
				}
			} else if ((e.isShiftDown())) {
				on = Dragging.off;

				float alpha = x / (float) this.bounds().width;
				float t = y / (float) this.bounds().height;

				interpolator.new Sample(new BreakpointFloat(t), alpha);
				for (int i = 0; i < interpolator.getNumSamples(); i++) {
					int leftX = leftX(i);
					int rightX = rightX(i);
					int bottomY = bottomY(i);
					int topY = topY(i);
					if (x >= leftX && x <= rightX && y >= topY && y <= bottomY) {
						initialDown = x;
						initialDownY = y;
						initialDownOn = i;
						on = Dragging.on;

						break;
					}
				}
				repaint();
			} else if ((e.isMetaDown() || e.isAltDown())) {
				on = Dragging.off;
				initialDownOn = -1;
				for (int i = 0; i < interpolator.getNumSamples(); i++) {
					int leftX = leftX(i);
					int rightX = rightX(i);
					int bottomY = bottomY(i);
					int topY = topY(i);
					if (x >= leftX && x <= rightX && y >= topY && y <= bottomY) {
						initialDown = x;
						initialDownY = y;
						initialDownOn = i;
						on = Dragging.on;

						break;
					}
				}
				if (initialDownOn != -1) {
					interpolator.removeSample(initialDownOn);
					repaint();
				}
			} else {
				on = Dragging.off;
				initialDown = -1;
				for (int i = 0; i < interpolator.getNumSamples(); i++) {
					int leftX = leftX(i);
					int rightX = rightX(i);
					int bottomY = bottomY(i);
					int topY = topY(i);
					if (x >= leftX && x <= rightX && y >= topY && y <= bottomY) {
						initialDown = x;
						initialDownY = y;
						initialDownOn = i;
						on = Dragging.on;

						break;
					}
				}

				if (initialDown == -1) {
					for (int i = 0; i < interpolator.getNumSamples() - 1; i++) {
						if (interpolator.getSample(i).data.next != Next.cubic)
							continue;

						Vector2 controlIs = transformRelativeToFrame(this.getBounds(), new Vector2(interpolator.getSample(i).time + (interpolator.getSample(i + 1).time - interpolator.getSample(i).time) / 3, interpolator.getSample(i).data.controlNext));
						Vector2 m = new Vector2(x, this.getHeight() - y);

						if (controlIs.distanceFrom(m) < 5) {
							initialDown = x;
							initialDownY = y;
							initialDownOn = i;
							on = Dragging.controlAfter;

						}
					}
				}
				if (initialDown == -1) {
					for (int i = 1; i < interpolator.getNumSamples(); i++) {
						if (interpolator.getSample(i-1).data.next != Next.cubic)
							continue;

						Vector2 controlIs = transformRelativeToFrame(this.getBounds(), new Vector2(interpolator.getSample(i).time + (interpolator.getSample(i -1).time - interpolator.getSample(i).time) / 3, interpolator.getSample(i).data.controlPrevious));
						Vector2 m = new Vector2(x, this.getHeight() - y);


						if (controlIs.distanceFrom(m) < 5) {
							initialDown = x;
							System.out.println(" found it");
							initialDownY = y;
							initialDownOn = i;
							on = Dragging.controlBefore;

						}
					}
				}

			}
		}
		if (e.getID() == e.MOUSE_RELEASED) {
			on = Dragging.off;
			if (shouldColapseOnOff) {
				colapse();

			}
			repaint();
			if (when == ExecutesWhen.always || when == ExecutesWhen.onMouseUp)
				execute();

		}
	}

	private void pop(JPopupMenu menu, MouseEvent e) {
		if (isDisplayable())
		{
			menu.show(getParent(), e.getX() - getParent().bounds().x, e.getY() - getParent().bounds().y);
		}
		else
		{
			GLComponentWindow window = GLComponentWindow.getCurrentWindow(null);
			// why the -30 ?
			//TODO swt
//			menu.show(window.getCanvas(), e.getX()+this.getBounds().x,e.getY()+this.getBounds().y-30);
		}
	}

	@Override
	protected void processMouseMotionEvent(MouseEvent e) {
		super.processMouseMotionEvent(e);
		int x = e.getX() - 0 * this.bounds().x;
		int y = this.getHeight() - (e.getY() - 0 * this.bounds().y);
		if (e.getID() == e.MOUSE_DRAGGED && on == Dragging.on) {
			int dx = x - initialDown;
			initialDown = x;
			int dy = y - initialDownY;
			initialDownY = y;

			float lvalue = value(initialDownOn) + dx / (float) (this.getBounds().width - sliderWidth);
			if (lvalue > 1)
				lvalue = 1;
			if (lvalue < 0)
				lvalue = 0;
			float lvalueY = valueY(initialDownOn) + dy / (float) (this.getBounds().height - sliderHeight);
			if (lvalueY > 1)
				lvalueY = 1;
			if (lvalueY < 0)
				lvalueY = 0;
			setValue(initialDownOn, lvalue, lvalueY);
			repaint();

			if (when == ExecutesWhen.always)
				execute();
		}
		if (e.getID() == e.MOUSE_DRAGGED && on == Dragging.controlAfter) {
			int dx = x - initialDown;
			initialDown = x;
			int dy = y - initialDownY;
			initialDownY = y;

			float lvalue = interpolator.getSample(initialDownOn).time + dx / (float) (this.getBounds().width - sliderWidth);
			if (lvalue > 1)
				lvalue = 1;
			if (lvalue < 0)
				lvalue = 0;
			float lvalueY = interpolator.getSample(initialDownOn).data.controlNext + dy / (float) (this.getBounds().height - sliderHeight);
			if (lvalueY > 1)
				lvalueY = 1;
			if (lvalueY < 0)
				lvalueY = 0;
			setControlNext(initialDownOn, lvalue, lvalueY);
			repaint();

			if (when == ExecutesWhen.always)
				execute();
		}
		if (e.getID() == e.MOUSE_DRAGGED && on == Dragging.controlBefore) {
			int dx = x - initialDown;
			initialDown = x;
			int dy = y - initialDownY;
			initialDownY = y;

			float lvalue = interpolator.getSample(initialDownOn).time + dx / (float) (this.getBounds().width - sliderWidth);
			if (lvalue > 1)
				lvalue = 1;
			if (lvalue < 0)
				lvalue = 0;
			float lvalueY = interpolator.getSample(initialDownOn).data.controlPrevious + dy / (float) (this.getBounds().height - sliderHeight);
			if (lvalueY > 1)
				lvalueY = 1;
			if (lvalueY < 0)
				lvalueY = 0;
			setControlPrevious(initialDownOn, lvalue, lvalueY);
			repaint();

			if (when == ExecutesWhen.always)
				execute();
		}
	}

	protected void setValue(int on, float x, float y) {
		CubicInterpolatorDynamic<BreakpointFloat>.Sample sample = interpolator.getSample(on);
		interpolator.removeSample(sample);
		boolean changed = sample.time != x || sample.data.value != y;
		BreakpointFloat bf = new BreakpointFloat(y);
		bf.setValue(sample.data);
		bf.value = y;
		Sample s2 = interpolator.new Sample(bf, x);

	}

	protected void setControlNext(int on, float x, float y) {
		CubicInterpolatorDynamic<BreakpointFloat>.Sample sample = interpolator.getSample(on);
		interpolator.removeSample(sample);
		boolean changed = sample.time != x || sample.data.value != y;
		BreakpointFloat bf = new BreakpointFloat(sample.data.value);
		bf.setValue(sample.data);
		bf.controlNext = y;
		Sample s2 = interpolator.new Sample(bf, x);

	}

	protected void setControlPrevious(int on, float x, float y) {
		CubicInterpolatorDynamic<BreakpointFloat>.Sample sample = interpolator.getSample(on);
		interpolator.removeSample(sample);
		boolean changed = sample.time != x || sample.data.value != y;
		BreakpointFloat bf = new BreakpointFloat(sample.data.value);
		bf.setValue(sample.data);
		bf.controlPrevious = y;
		Sample s2 = interpolator.new Sample(bf, x);

	}

	protected Vector2 transformRelativeToFrame(Rectangle bounds, Vector2 object) {
		object.x = object.x * bounds.width;
		object.y = bounds.height - object.y * bounds.height;
		return object;
	}

}
