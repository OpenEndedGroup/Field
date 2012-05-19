package field.graphics.ci;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.rmi.server.UID;

import javax.swing.JComponent;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.ui.text.embedded.CustomInsertSystem;
import field.core.ui.text.embedded.CustomInsertSystem.ProvidedComponent;
import field.core.ui.text.embedded.CustomInsertSystem.iPossibleComponent;
import field.graphics.ci.MiniDraggable.SubControl;
import field.launch.iUpdateable;
import field.math.abstraction.iFilter;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;
import field.namespace.key.OKey;
import field.util.PythonUtils;

public class MinimalImageHistogram extends JComponent {
	static public class Component extends ProvidedComponent {

		transient public iUpdateable notify;

		ImageHistogramState state;

		String name;

		OKey<ImageHistogramState> localKey;

		@Override
		public void deserialize(iVisualElement inside) {

			name = "Component:" + new UID().toString() + ".transient";

			if (state == null)
				state = new ImageHistogramState();
			localKey = new OKey<ImageHistogramState>(name).rootSet(state);

			component = new MinimalImageHistogram() {
				@Override
				public void paintComponent(Graphics g) {
					super.paintComponent(g);
					localKey.rootSet(Component.this.state);
					Component.this.state.set(getState());
					System.out.println(" set state <" + Component.this.state + "> to <" + this + ">");

					Component.this.state.setOutput(this);
				}
			};

			((MinimalImageHistogram) component).setState(state);
			state.setOutput((MinimalImageHistogram) component);

		}

		@Override
		public String getCurrentRepresentedString() {
			if (localKey == null)
				localKey = new OKey<ImageHistogramState>(name).rootSet(state);
			return "OKeyByName(\"" + name + "\", u.fromXML(r\"\"\"" + new PythonUtils().toXML(state, true) + "\"\"\")).get()";
		}

		@Override
		public void makeNew() {
			name = "Component:" + new UID().toString() + ".transient";
			localKey = new OKey<ImageHistogramState>(name).rootSet(state);
		}

		@Override
		public void preserialize() {
		}

		protected void updateValue() {
		}
	}

	static public class ImageHistogramState implements Serializable, iFilter<Float, Float> {
		private static final long serialVersionUID = 1L;
		float blackIn = 0;
		float greyIn = 0.5f;
		float whiteIn = 1;

		float blackOut = 0;
		float greyOut = 0.5f;
		float whiteOut = 1;

		transient MinimalImageHistogram histogram;
		transient FloatBuffer f;

		public Float filter(Float value) {
			return performMapping(blackIn, greyIn, whiteIn, blackOut, greyOut, whiteOut, value);
		}

		public FloatBuffer getRemapBuffer(int size) {
			return makeRemapBuffer(size, this);
		}

		public CoreImageCanvasUtils.Image getRemapImage(int size) {
			FloatBuffer f = getRemapBuffer(size);
			return new CoreImageCanvasUtils().new Image(f, size, 1);
		}

		public void set(ImageHistogramState s) {
			blackIn = s.blackIn;
			blackOut = s.blackOut;
			greyIn = s.greyIn;
			greyOut = s.greyOut;
			whiteIn = s.whiteIn;
			whiteOut = s.whiteOut;
		}

		public void setHistogram(FloatBuffer f) {
			if (histogram != null) {
				histogram.setHistogram(f);
			}
			this.f = f;
		}

		protected void setOutput(MinimalImageHistogram histogram) {
			this.histogram = histogram;
			if (f != null) {
				histogram.setHistogram(f);
			}
		}
	}

	private static final long serialVersionUID = 1L;

	static public void install() {
		CustomInsertSystem.possibleComponents.add(new iPossibleComponent("x Image Histogram", Component.class));
	}

	static public FloatBuffer makeRemapBuffer(int size, ImageHistogramState state) {
		FloatBuffer f = ByteBuffer.allocateDirect(size * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		for (int i = 0; i < size; i++) {
			float a = i / (size - 1f);
			float b = performMapping(state.blackIn, state.greyIn, state.whiteIn, state.blackOut, state.greyOut, state.whiteOut, a);
			f.put(b);
			f.put(b);
			f.put(b);
			f.put(b);
		}
		return f;
	}

	static public float performMapping(float blackIn, float greyIn, float whiteIn, float blackOut, float greyOut, float whiteOut, float x) {
		float gammaIn = 1 / (float) (Math.log(0.5) / Math.log((greyIn - blackIn) / (whiteIn - blackIn)));
		float gammaOut = 1 / (float) (Math.log(0.5) / Math.log((greyOut - blackOut) / (whiteOut - blackOut)));

		float x1 = (x - blackIn) / (whiteIn - blackIn);

		if (x1 < 0)
			x1 = 0;
		if (x1 > 1)
			x1 = 1;

		x1 = (float) Math.pow(x1, gammaIn);

		float x2 = (float) (Math.pow(x1, gammaOut) * (whiteOut - blackOut) + blackOut);
		return x2;
	}

	private final SubControl blackIn;

	private final SubControl whiteIn;

	private final SubControl greyIn;

	private final SubControl blackOut;

	private final SubControl greyOut;

	private final SubControl whiteOut;

	private FloatBuffer remapped;

	MiniDraggable draggables = new MiniDraggable() {
		@Override
		protected field.math.linalg.Vector2 derelativize(field.math.linalg.Vector2 v) {
			return new Vector2(v.x * getWidth(), v.y * getHeight());
		};

		@Override
		protected Rect derelativize(Rect v) {

			float lx = (float) (v.x * (getWidth() - v.w));
			float ly = (float) (v.y * (getHeight() - v.h - 1));

			return new Rect(lx, ly, v.w, v.h);
		};

		@Override
		protected field.math.linalg.Vector2 relativize(field.math.linalg.Vector2 v) {
			return new Vector2(v.x / getWidth(), v.y / getHeight());
		}

		@Override
		protected Rect relativize(Rect v) {

			float lx = (float) (v.x / (getWidth() - v.w));
			float ly = (float) (v.y / (getHeight() - v.h - 1));

			return new Rect(lx, ly, v.w, v.h);
		}
	};

	FloatBuffer histogram;

	boolean changed = true;

	float alignment = 0.5f;
	int height = 75;

	Vector4[] colors = { new Vector4(1, 0, 0, 0.5f), new Vector4(0, 1, 0, 0.5f), new Vector4(0, 0, 1, 0.5f), new Vector4(1, 1, 1, 0.15f) };

	public MinimalImageHistogram() {

		histogram = ByteBuffer.allocate(4 * 128 * 4).asFloatBuffer();
		remapped = ByteBuffer.allocate(4 * 128 * 4).asFloatBuffer();
		for (int i = 0; i < 128; i++) {
			histogram.put(0);
			histogram.put(0);
			histogram.put(0);
			histogram.put(0);
		}

		blackIn = draggables.new SubControl() {
			@Override
			protected void constrainBox() {
				hitBox.y = 0;
				hitBox.x = Math.max(Math.min(hitBox.x, 1), 0);
				if (hitBox.x > greyIn.hitBox.x)
					greyIn.hitBox.x = hitBox.x;
				if (hitBox.x > whiteIn.hitBox.x)
					whiteIn.hitBox.x = hitBox.x;
				changed = true;
			}
		};
		greyIn = draggables.new SubControl() {
			@Override
			protected void constrainBox() {
				hitBox.y = 0;
				hitBox.x = Math.max(Math.min(hitBox.x, 1), 0);
				if (hitBox.x < blackIn.hitBox.x)
					blackIn.hitBox.x = hitBox.x;
				if (hitBox.x > whiteIn.hitBox.x)
					whiteIn.hitBox.x = hitBox.x;
				changed = true;
			}
		};
		whiteIn = draggables.new SubControl() {
			@Override
			protected void constrainBox() {
				hitBox.y = 0;
				hitBox.x = Math.max(Math.min(hitBox.x, 1), 0);
				if (hitBox.x < blackIn.hitBox.x)
					blackIn.hitBox.x = hitBox.x;
				if (hitBox.x < greyIn.hitBox.x)
					greyIn.hitBox.x = hitBox.x;
				changed = true;
			}
		};
		blackOut = draggables.new SubControl() {
			@Override
			protected void constrainBox() {
				hitBox.y = 1;
				hitBox.x = Math.max(Math.min(hitBox.x, 1), 0);
				if (hitBox.x > greyOut.hitBox.x)
					greyOut.hitBox.x = hitBox.x;
				if (hitBox.x > whiteOut.hitBox.x)
					whiteOut.hitBox.x = hitBox.x;
				changed = true;
			}
		};
		greyOut = draggables.new SubControl() {
			@Override
			protected void constrainBox() {
				hitBox.y = 1;
				hitBox.x = Math.max(Math.min(hitBox.x, 1), 0);
				if (hitBox.x < blackOut.hitBox.x)
					blackOut.hitBox.x = hitBox.x;
				if (hitBox.x > whiteOut.hitBox.x)
					whiteOut.hitBox.x = hitBox.x;
				changed = true;
			}
		};
		whiteOut = draggables.new SubControl() {
			@Override
			protected void constrainBox() {
				hitBox.y = 1;
				hitBox.x = Math.max(Math.min(hitBox.x, 1), 0);
				if (hitBox.x < blackOut.hitBox.x)
					blackOut.hitBox.x = hitBox.x;
				if (hitBox.x < greyOut.hitBox.x)
					greyOut.hitBox.x = hitBox.x;
				changed = true;
			}
		};

		blackIn.hitBox.x = 0;
		greyIn.hitBox.x = 0.5f;
		whiteIn.hitBox.x = 1;

		blackOut.hitBox.x = 0;
		greyOut.hitBox.x = 0.5f;
		whiteOut.hitBox.x = 1;

		blackOut.hitBox.y = 1;
		greyOut.hitBox.y = 1;
		whiteOut.hitBox.y = 1;

		blackIn.color = new Vector4(0, 0, 0, 0.5f);
		greyIn.color = new Vector4(0.5f, 0.5f, 0.5f, 0.5f);
		whiteIn.color = new Vector4(1f, 1f, 1f, 0.5f);
		blackOut.color = new Vector4(0, 0, 0, 0.5f);
		greyOut.color = new Vector4(0.5f, 0.5f, 0.5f, 0.5f);
		whiteOut.color = new Vector4(1f, 1f, 1f, 0.5f);
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

	public ImageHistogramState getState() {
		ImageHistogramState state = new ImageHistogramState();

		state.blackIn = (float) blackIn.hitBox.x;
		state.greyIn = (float) greyIn.hitBox.x;
		state.whiteIn = (float) whiteIn.hitBox.x;
		state.blackOut = (float) blackOut.hitBox.x;
		state.greyOut = (float) greyOut.hitBox.x;
		state.whiteOut = (float) whiteOut.hitBox.x;

		return state;
	}

	public void setHistogram(FloatBuffer f) {
		changed = histogram != f;
		if (histogram != f) {
			histogram = f;
			remapped = ByteBuffer.allocate(f.capacity() * 4).asFloatBuffer();
			repaint();
		}
	}

	public void setState(ImageHistogramState state) {
		blackIn.hitBox.x = state.blackIn;
		greyIn.hitBox.x = state.greyIn;
		whiteIn.hitBox.x = state.whiteIn;

		blackOut.hitBox.x = state.blackOut;
		greyOut.hitBox.x = state.greyOut;
		whiteOut.hitBox.x = state.whiteOut;
		changed = true;
		repaint();
	}

	protected void drawHistogram(int width, int height, FloatBuffer h, Graphics2D g) {
		if (h == null)
			return;

		if (changed)
			remap(h, remapped);

		changed = false;
		h = remapped;

		int n = h.capacity() / 4;
		for (int c = 0; c < colors.length; c++) {
			g.setColor(new Color(colors[c].x, colors[c].y, colors[c].z, colors[c].w));
			GeneralPath p = new GeneralPath();
			float lx = 0;
			float fx = 0;
			for (int x = 0; x < n; x++) {
				float v = h.get(4 * x + c);
				float rx = x / ((float) n - 1);

				// float tx =
				// performMapping((float)blackIn.hitBox.x,(float)greyIn.hitBox.x,(float)whiteIn.hitBox.x,
				// (float)blackOut.hitBox.x,(float)greyOut.hitBox.x,(float)whiteOut.hitBox.x,rx);
				// System.out.println("
				// remapped
				// <"+rx+" ->
				// "+tx+">");
				float tx = rx;
				float cx = getWidth() * tx;
				float cy = getHeight() * (1 - v);
				if (x == 0)
					p.moveTo(cx, cy);
				else
					p.lineTo(cx, cy);
				lx = cx;
			}
			p.lineTo(lx, getHeight());
			p.lineTo(0, getHeight());
			p.closePath();
//			g.setComposite(SVGComposite.SCREEN);
			g.draw(p);
			g.fill(p);
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		Rectangle bounds = this.getBounds();

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

		g2.setColor(new Color(0.22f, 0.22f, 0.23f, 0.4f));

		GeneralPath path = new GeneralPath();
		path.moveTo(0, 0);
		path.lineTo(bounds.width - 1, 0);
		path.lineTo(bounds.width - 1, bounds.height - 1);
		path.lineTo(0, bounds.height - 1);
		path.lineTo(0, 0);
		g2.fill(path);
		g2.setColor(new Color(1 - 0.22f, 1 - 0.22f, 1 - 0.23f, 0.1f));
		g2.draw(path);

		// first, let's draw the histogram itself (in
		// glorious RGB)
		drawHistogram(bounds.width, bounds.height, histogram, (Graphics2D) g2.create());

		enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);

		draggables.draw(g2);

		g2.setColor(new Color(1 - 0.22f, 1 - 0.22f, 1 - 0.23f, 0.1f));
		{
			Rect f1 = new Rect(0, 0, 0, 0).setValue(whiteIn.hitBox);
			f1.w = 0;
			f1 = draggables.derelativize(f1);
			Rect f2 = new Rect(0, 0, 0, 0).setValue(whiteOut.hitBox);
			f2.w = 0;
			f2 = draggables.derelativize(f2);
			g2.setStroke(new BasicStroke(1, 1, 1, 1, new float[] { 3, 3 }, 0));
			g2.drawLine((int) f1.x, (int) (f1.y + f1.h), (int) f2.x, (int) f2.y);
		}
		{
			Rect f1 = new Rect(0, 0, 0, 0).setValue(greyIn.hitBox);
			f1.w = 0;
			f1 = draggables.derelativize(f1);
			Rect f2 = new Rect(0, 0, 0, 0).setValue(greyOut.hitBox);
			f2.w = 0;
			f2 = draggables.derelativize(f2);
			g2.setStroke(new BasicStroke(1, 1, 1, 1, new float[] { 3, 3 }, 0));
			g2.drawLine((int) f1.x, (int) (f1.y + f1.h), (int) f2.x, (int) f2.y);
		}
		{
			Rect f1 = new Rect(0, 0, 0, 0).setValue(blackIn.hitBox);
			f1.w = 0;
			f1 = draggables.derelativize(f1);
			Rect f2 = new Rect(0, 0, 0, 0).setValue(blackOut.hitBox);
			f2.w = 0;
			f2 = draggables.derelativize(f2);
			g2.setStroke(new BasicStroke(1, 1, 1, 1, new float[] { 3, 3 }, 0));
			g2.drawLine((int) f1.x, (int) (f1.y + f1.h), (int) f2.x, (int) f2.y);
		}
	}

	@Override
	protected void processMouseEvent(MouseEvent e) {
		super.processMouseEvent(e);
		if (e.getID() == e.MOUSE_PRESSED) {
			if (draggables.mouseDown(e))
				repaint();
		} else if (e.getID() == e.MOUSE_RELEASED) {
			if (draggables.mouseUp(e))
				repaint();
		} else if (e.getID() == e.MOUSE_ENTERED) {
			this.setCursor(Cursor.getDefaultCursor());
		}
	}

	@Override
	protected void processMouseMotionEvent(MouseEvent e) {
		super.processMouseMotionEvent(e);
		if (e.getID() == e.MOUSE_DRAGGED) {
			if (draggables.mouseDragged(e))
				repaint();
		} else if (e.getID() == e.MOUSE_MOVED) {
			if (draggables.mouseMoved(e))
				repaint();
		} else if (e.getID() == e.MOUSE_ENTERED) {
			this.setCursor(Cursor.getDefaultCursor());
		}
	}

	protected void remap(FloatBuffer h, FloatBuffer out) {

		for (int i = 0; i < out.capacity(); i++)
			out.put(i, 0);

		int overSample = 160;
		for (int c = 0; c < colors.length; c++) {
			int n = h.capacity() / 4;
			int lastBin = 0;

			for (int x = 0; x < n * overSample; x++) {
				int tx = Math.round(((n - 1f) * performMapping((float) blackIn.hitBox.x, (float) greyIn.hitBox.x, (float) whiteIn.hitBox.x, (float) blackOut.hitBox.x, (float) greyOut.hitBox.x, (float) whiteOut.hitBox.x, x / (n * overSample - 1f))));

				if (tx > n - 1)
					tx = n - 1;
				float v = h.get(4 * Math.min(n - 1, x / overSample) + c);
				// distribute
				// evenly
				// between
				// lastBin and
				// tx inclusive

				out.put(4 * tx + c, out.get(4 * tx + c) + v);
				lastBin = tx;
			}
		}
		for (int i = 0; i < out.capacity(); i++)
			out.put(i, out.get(i) / overSample);
	}

}
