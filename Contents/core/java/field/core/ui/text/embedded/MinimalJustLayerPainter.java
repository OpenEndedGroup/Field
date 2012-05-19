package field.core.ui.text.embedded;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.rmi.server.UID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import field.core.Constants;
import field.core.dispatch.iVisualElement;
import field.core.ui.text.embedded.CustomInsertSystem.ProvidedComponent;
import field.namespace.key.OKey;
import field.util.PythonUtils;

public class MinimalJustLayerPainter extends MinimalExpandable implements iRegistersMinimalLayerPainter {

	static public class Component_tuple2 extends ProvidedComponent {

		String name = "Component_just_provider:" + new UID().toString() + ".transient";

		OKey<WithWidget> localKey;

		WithWidget interpolator;

		@Override
		public void deserialize(iVisualElement inside) {

			component = new MinimalJustLayerPainter() {
			};

			interpolator = new WithWidget();

			((MinimalJustLayerPainter) component).setInterpolator(interpolator);
		}

		@Override
		public String getCurrentRepresentedString() {
			if (localKey == null)
				localKey = new OKey<WithWidget>(name).rootSet(interpolator);
			interpolator.widget = (MinimalJustLayerPainter) component;
			return "OKeyByName(\"" + name + "\", u.fromXML(r\"\"\"" + new PythonUtils().toXML(interpolator, true) + "\"\"\"))";
		}

		@Override
		public void preserialize() {
		}

		protected void updateValue() {
			if (localKey == null)
				localKey = new OKey<WithWidget>(name).rootSet(interpolator);
			interpolator.widget = (MinimalJustLayerPainter) component;
			localKey.rootSet(interpolator);
		}
	}

	static public class WithWidget {
		public transient MinimalJustLayerPainter widget;

		public WithWidget() {
		}
	}

	private int initialDown;

	private int initialDownY;

	private Font font;

	private int initialDownOn;

	WithWidget interpolator = new WithWidget();

	int sliderWidth = 12;

	int sliderHeight = 12;

	boolean on = false;

	float alignment = 0.9f;

	HashMap<String, iMinimalLayerPainter> layerPainters = new LinkedHashMap<String, iMinimalLayerPainter>();

	HashSet<iMinimalLayerPainter> first = new LinkedHashSet<iMinimalLayerPainter>();

	public MinimalJustLayerPainter() {
	}

	public MinimalJustLayerPainter(int h) {
		height = h;
		sliderHeight = h;

	}

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

	public iMinimalLayerPainter painterForName(String name) {
		return layerPainters.get(name);
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
	}

	public void setInterpolator(WithWidget interpolator2) {
		this.interpolator = interpolator2;
	}

	public iMinimalLayerPainter setPainterForName(String name, iMinimalLayerPainter p) {
		first.add(p);
		return layerPainters.put(name, p);
	}

	@Override
	protected void paintComponent(Graphics g) {
		Rectangle bounds = this.getBounds();

		Graphics2D g2 = (Graphics2D) g;

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		// paint layers

		Double d = new Rectangle2D.Double(0,0, this.getSize().getWidth(), this.getSize().getHeight());


		g2.setColor(new Color(0,0,0,0.15f));
		g2.fill(d);
		g2.setColor(new Color(1,1,1,0.2f));
		g2.setStroke(new BasicStroke(1.75f));
		g2.draw(d);

		for (iMinimalLayerPainter painter : layerPainters.values()) {
			if (first.remove(painter)) {
				painter.associate(this);
			}
			painter.paintNow(g2, this.getSize());
		}

		enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);

		if (collapseness > 1)
			g2.drawImage(Constants.plus, bounds.width - 14, 2, 12, 12, null);
		else
			g2.drawImage(Constants.minus, bounds.width - 14, 2, 12, 12, null);


	}

	@Override
	protected void processMouseEvent(MouseEvent e) {
		super.processMouseEvent(e);
		int x = e.getX() - this.bounds().x;
		int y = e.getY() - this.bounds().y;
		if (e.getID() == e.MOUSE_ENTERED) {
			expand();
			shouldColapseOnOff = false;
		}
		if (e.getID() == e.MOUSE_EXITED && !on) {
			colapse();
			shouldColapseOnOff = false;
		}
		if (e.getID() == e.MOUSE_EXITED && on) {
			shouldColapseOnOff = true;
		}

	}
}
