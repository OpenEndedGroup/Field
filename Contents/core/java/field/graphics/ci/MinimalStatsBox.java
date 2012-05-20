package field.graphics.ci;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JPopupMenu;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;

import field.core.Constants;
import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.python.PythonPlugin;
import field.core.ui.SmallMenu;
import field.core.ui.text.embedded.CustomInsertSystem.ProvidedComponent;
import field.core.ui.text.embedded.MinimalExpandable;
import field.core.windowing.GLComponentWindow;
import field.graphics.ci.MiniDraggable.SubControl;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.BaseMath;
import field.math.BinSelection;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;
import field.math.util.Histogram;
import field.namespace.generic.Bind.iFunction;
import field.namespace.key.OKey;
import field.util.PythonUtils;

public class MinimalStatsBox extends MinimalExpandable {

	static public class State {
		List<Float> previousSample = new ArrayList<Float>();
		List<Float> ongoingSample = new ArrayList<Float>();

		int insertAt = 0;
		int maxHistorySize = 200;
		int numBins = 20;

		float blackIn = 0;
		float blackOut = 0;
		float whiteIn = 1;
		float whiteOut = 1;
		float greyIn = 0.5f;
		float greyOut = 0.5f;
		float gammaIn = 1f;
		float gammaOut = 1f;

		boolean changed = false;

		boolean inputRangeLocked = false;

		public void set(State state) {
			previousSample = new ArrayList<Float>(state.previousSample);
			ongoingSample = new ArrayList<Float>(state.ongoingSample);
			this.insertAt = state.insertAt;
			this.maxHistorySize = state.maxHistorySize;
			this.numBins = state.numBins;
		}

		public float add(float f) {

			if (autoSwap)
				PythonPlugin.ongoingEnvironment.addTransientHandler("" + System.identityHashCode(this), new iUpdateable() {

					public void update() {
						swap();
					}
				});

			if (ongoingSample.size() < maxHistorySize) {
				ongoingSample.add(f);
				insertAt = 0;
			} else {
				ongoingSample.set(insertAt, f);
				insertAt = (insertAt + 1) % ongoingSample.size();
			}

			changed = true;

			return remap(f);
		}

		protected float remap(float x) {

			x = (x - inputMin) / (inputMax - inputMin);

			float f1 = (x - blackIn) / (whiteIn - blackIn);
			if (f1 < 0)
				f1 = 0;
			if (f1 > 1)
				f1 = 1;

			f1 = (float) Math.pow(f1, gammaIn);
			f1 = (float) Math.pow(f1, gammaOut) * (whiteOut - blackOut) + blackOut;
			return f1;
		}

		public void swap() {
			previousSample = ongoingSample;
			ongoingSample = new ArrayList<Float>();
			insertAt = 0;
			changed = true;
		}

		float inputMin, inputMax, inputAverage, input2Average;
		String histogramStillborn = "(no data)";
		private Histogram<Integer> binned;

		float outputMin, outputMax, outputAverage, output2Average;
		private Histogram<Integer> outputBinned;

		float maxBin = 0;
		float maxOutputBin = 0;
		public boolean autoSwap = true;

		transient BinSelection bs;

		protected void recomputeDisplayParameters() {

			if (previousSample.size() < 2)
				histogramStillborn = "Too few samples to show";
			else
				histogramStillborn = "";

			if (!inputRangeLocked) {
				inputMin = Float.POSITIVE_INFINITY;
				inputMax = Float.NEGATIVE_INFINITY;
			}
			inputAverage = 0;
			input2Average = 0;
			maxBin = 0;

			bs = new BinSelection(previousSample, 500);
			numBins = bs.getNumBins();

			;//System.out.println(" computed num bins to be <" + numBins + ">");

			for (Float f : previousSample) {
				if (!inputRangeLocked) {
					inputMin = Math.min(inputMin, f);
					inputMax = Math.max(inputMax, f);
				}
				inputAverage += f;
				input2Average += f * f;
			}

			inputAverage /= previousSample.size();
			input2Average /= previousSample.size();

			binned = new Histogram<Integer>();
			for (Float f : previousSample) {
				int bin = (int) (inputMax == inputMin ? 0 : (numBins * ((f - inputMin) / (inputMax - inputMin))));
				float count = (float) (binned.visit(bin, 1) * binned.getTotal());
				if (count > maxBin)
					maxBin = count;
			}

			outputMin = Float.POSITIVE_INFINITY;
			outputMax = Float.NEGATIVE_INFINITY;
			outputAverage = 0;
			output2Average = 0;
			maxOutputBin = 0;

			for (Float f : previousSample) {

				f = remap(f);

				outputMin = Math.min(outputMin, f);
				outputMax = Math.max(outputMax, f);
				outputAverage += f;
				output2Average += f * f;
			}

			outputAverage /= previousSample.size();
			output2Average /= previousSample.size();

			outputBinned = new Histogram<Integer>();
			for (Float f : previousSample) {
				f = remap(f);

				int bin = (int) (outputMax == outputMin ? 0 : (numBins * ((f - 0) / (1 - 0))));
				float count = (float) (outputBinned.visit(bin, 1) * outputBinned.getTotal());
				if (count > maxOutputBin)
					maxOutputBin = count;
			}
		}

	}

	static public class Component extends ProvidedComponent {
		State state;
		OKey<State> localKey;
		String name;

		public void deserialize(iVisualElement inside) {

			name = "Component:" + new UID().toString() + ".transient";

			if (state == null)
				state = new State();
			localKey = new OKey<State>(name).rootSet(state);

			component = new MinimalStatsBox() {
				@Override
				public void paintComponent(Graphics g) {
					super.paintComponent(g);
					localKey.rootSet(Component.this.state);
					Component.this.state.set(getState());
				}
			};

			((MinimalStatsBox) component).setState(state);
		}

		@Override
		public String getCurrentRepresentedString() {
			if (localKey == null)
				localKey = new OKey<State>(name).rootSet(state);
			return "OKeyByName(\"" + name + "\", u.fromXML(r\"\"\"" + new PythonUtils().toXML(state, true) + "\"\"\")).get()";
		}

		@Override
		public void makeNew() {
			name = "Component:" + new UID().toString() + ".transient";
			localKey = new OKey<State>(name).rootSet(state);
		}

		@Override
		public void preserialize() {
		}

		protected void updateValue() {
		}
	}

	private final SubControl blackIn;

	private final SubControl whiteIn;

	private final SubControl greyIn;

	private final SubControl blackOut;

	private final SubControl greyOut;

	private final SubControl whiteOut;

	MiniDraggable draggables = new MiniDraggable() {
		@Override
		protected field.math.linalg.Vector2 derelativize(field.math.linalg.Vector2 v) {
			return new Vector2(v.x * (getWidth() - standoff), v.y * getHeight());
		};

		@Override
		protected Rect derelativize(Rect v) {

			float lx = (float) (v.x * (getWidth() - v.w - standoff));
			float ly = (float) (v.y * (getHeight() - v.h - 1));

			return new Rect(lx, ly, v.w, v.h);
		};

		@Override
		protected field.math.linalg.Vector2 relativize(field.math.linalg.Vector2 v) {
			return new Vector2(v.x / (getWidth() - standoff), v.y / getHeight());
		}

		@Override
		protected Rect relativize(Rect v) {

			float lx = (float) (v.x / (getWidth() - v.w - standoff));
			float ly = (float) (v.y / (getHeight() - v.h - 1 - standoff));

			return new Rect(lx, ly, v.w, v.h);
		}
	};

	private boolean remapChanged = false;

	int standoff = 20;

	public MinimalStatsBox() {

		blackIn = draggables.new SubControl() {
			@Override
			protected void constrainBox() {
				hitBox.y = 0;
				hitBox.x = Math.max(Math.min(hitBox.x, 1), 0);
				if (hitBox.x > greyIn.hitBox.x) {
					setRemapChanged(true);
					greyIn.hitBox.x = hitBox.x;
				}
				if (hitBox.x > whiteIn.hitBox.x) {
					setRemapChanged(true);
					whiteIn.hitBox.x = hitBox.x;
				}

				if (state.blackIn != (float) hitBox.x / (1))
					setRemapChanged(true);
				state.blackIn = (float) hitBox.x / (1);
				if (state.gammaIn != 1 / (float) (Math.log(0.5) / Math.log((state.greyIn - state.blackIn) / (state.whiteIn - state.blackIn))))
					setRemapChanged(true);
				state.gammaIn = 1 / (float) (Math.log(0.5) / Math.log((state.greyIn - state.blackIn) / (state.whiteIn - state.blackIn)));

			}
		};
		greyIn = draggables.new SubControl() {
			@Override
			protected void constrainBox() {
				hitBox.y = 0;
				hitBox.x = Math.max(Math.min(hitBox.x, 1), 0);
				if (hitBox.x < blackIn.hitBox.x) {
					setRemapChanged(true);
					blackIn.hitBox.x = hitBox.x;
				}
				if (hitBox.x > whiteIn.hitBox.x) {
					setRemapChanged(true);
					whiteIn.hitBox.x = hitBox.x;
				}

				if (state.greyIn != (float) hitBox.x / (1))
					setRemapChanged(true);

				state.greyIn = (float) hitBox.x / (1);
				if (state.gammaIn != 1 / (float) (Math.log(0.5) / Math.log((state.greyIn - state.blackIn) / (state.whiteIn - state.blackIn))))
					setRemapChanged(true);

				state.gammaIn = 1 / (float) (Math.log(0.5) / Math.log((state.greyIn - state.blackIn) / (state.whiteIn - state.blackIn)));

			}
		};
		whiteIn = draggables.new SubControl() {
			@Override
			protected void constrainBox() {
				hitBox.y = 0;
				hitBox.x = Math.max(Math.min(hitBox.x, 1), 0);
				if (hitBox.x < blackIn.hitBox.x) {
					setRemapChanged(true);
					blackIn.hitBox.x = hitBox.x;
				}
				if (hitBox.x < greyIn.hitBox.x) {
					setRemapChanged(true);
					greyIn.hitBox.x = hitBox.x;
				}

				if (state.whiteIn != (float) hitBox.x / (1))
					setRemapChanged(true);

				state.whiteIn = (float) hitBox.x / (1);
				if (state.gammaIn != 1 / (float) (Math.log(0.5) / Math.log((state.greyIn - state.blackIn) / (state.whiteIn - state.blackIn))))
					setRemapChanged(true);
				state.gammaIn = 1 / (float) (Math.log(0.5) / Math.log((state.greyIn - state.blackIn) / (state.whiteIn - state.blackIn)));

			}
		};
		blackOut = draggables.new SubControl() {
			@Override
			protected void constrainBox() {
				hitBox.y = 1;
				hitBox.x = Math.max(Math.min(hitBox.x, 1), 0);
				if (hitBox.x > greyOut.hitBox.x) {
					setRemapChanged(true);
					greyOut.hitBox.x = hitBox.x;
				}
				if (hitBox.x > whiteOut.hitBox.x) {
					setRemapChanged(true);
					whiteOut.hitBox.x = hitBox.x;
				}

				if (state.blackOut != (float) hitBox.x / (1))
					setRemapChanged(true);
				state.blackOut = (float) hitBox.x / (1);

				if (state.gammaOut != 1 / (float) (Math.log(0.5) / Math.log((state.greyOut - state.blackOut) / (state.whiteOut - state.blackOut))))
					setRemapChanged(true);
				state.gammaOut = 1 / (float) (Math.log(0.5) / Math.log((state.greyOut - state.blackOut) / (state.whiteOut - state.blackOut)));

			}
		};
		greyOut = draggables.new SubControl() {
			@Override
			protected void constrainBox() {
				hitBox.y = 1;
				hitBox.x = Math.max(Math.min(hitBox.x, 1), 0);
				if (hitBox.x < blackOut.hitBox.x) {
					setRemapChanged(true);
					blackOut.hitBox.x = hitBox.x;
				}
				if (hitBox.x > whiteOut.hitBox.x) {
					setRemapChanged(true);
					whiteOut.hitBox.x = hitBox.x;
				}

				if (state.greyOut != (float) hitBox.x / (1))
					setRemapChanged(true);
				state.greyOut = (float) hitBox.x / (1);

				if (state.gammaOut != 1 / (float) (Math.log(0.5) / Math.log((state.greyOut - state.blackOut) / (state.whiteOut - state.blackOut))))
					setRemapChanged(true);
				state.gammaOut = 1 / (float) (Math.log(0.5) / Math.log((state.greyOut - state.blackOut) / (state.whiteOut - state.blackOut)));

			}
		};
		whiteOut = draggables.new SubControl() {
			@Override
			protected void constrainBox() {
				hitBox.y = 1;
				hitBox.x = Math.max(Math.min(hitBox.x, 1), 0);
				if (hitBox.x < blackOut.hitBox.x) {
					setRemapChanged(true);
					blackOut.hitBox.x = hitBox.x;
				}
				if (hitBox.x < greyOut.hitBox.x) {
					setRemapChanged(true);
					greyOut.hitBox.x = hitBox.x;
				}

				if (state.whiteOut != (float) hitBox.x / (1))
					setRemapChanged(true);
				state.whiteOut = (float) hitBox.x / (1);
				if (state.gammaOut != 1 / (float) (Math.log(0.5) / Math.log((state.greyOut - state.blackOut) / (state.whiteOut - state.blackOut))))
					setRemapChanged(true);
				state.gammaOut = 1 / (float) (Math.log(0.5) / Math.log((state.greyOut - state.blackOut) / (state.whiteOut - state.blackOut)));

			}
		};

		blackIn.color = new Vector4(0, 0, 0, 0.5f);
		greyIn.color = new Vector4(0.75f, 0.75f, 0.75f, 0.5f);
		whiteIn.color = new Vector4(1f, 1f, 1f, 0.5f);
		blackOut.color = new Vector4(0, 0, 0, 0.5f);
		greyOut.color = new Vector4(0.75f, 0.75f, 0.75f, 0.5f);
		whiteOut.color = new Vector4(1f, 1f, 1f, 0.5f);
	}

	@Override
	protected void updateSize(int w, int h) {
		draggables.reconstrainAll();
	}

	private State state;

	protected State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;

		blackIn.hitBox.x = state.blackIn * (1);
		greyIn.hitBox.x = state.greyIn * (1);
		whiteIn.hitBox.x = state.whiteIn * (1);

		blackOut.hitBox.x = state.blackOut * (1);
		greyOut.hitBox.x = state.greyOut * (1);
		whiteOut.hitBox.x = state.whiteOut * (1);
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

	int insetAmount = 1;

	@Override
	protected void paintComponent(Graphics g) {

		if (state.changed || isRemapChanged()) {
			state.changed = false;
			setRemapChanged(false);
			state.recomputeDisplayParameters();
		}

		Rectangle b = this.getBounds();
		Rectangle bounds = b;

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

		g2.setPaint(new GradientPaint(0, 0, new Color(1, 1, 1, 0.1f), 0, bounds.height, new Color(0, 0, 0, 0.1f)));

		GeneralPath path = new GeneralPath();
		path.moveTo(0, 0);
		path.lineTo(bounds.width - standoff, 0);
		path.lineTo(bounds.width - standoff, bounds.height - 1);
		path.lineTo(0, bounds.height - 1);
		path.lineTo(0, 0);
		g2.fill(path);
		g2.draw(path);
		g2.draw(path);

		enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);

		draggables.draw(g2);

		g2.setColor(new Color(1, 1, 1, 0.2f));
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

		for (int i = 0; i < getWidth() / 4; i++) {
			float a = (i / (getWidth() / 4f));

			float to = state.remap(a * (state.inputMax - state.inputMin) + state.inputMin);

			float x1 = a * (getWidth() - standoff);
			float x2 = to * (getWidth() - standoff);

			Line2D.Float ll = new Line2D.Float(x1, 0, x2, getHeight());
			g2.setStroke(new BasicStroke(0.5f));
			g2.setColor(new Color(0, 0, 0, 0.1f));
			g2.draw(ll);

		}

		if (state.histogramStillborn.equals("") && state.binned != null) {
			Rectangle inset = this.getBounds();

			// inset.x += insetAmount;
			// inset.width -= insetAmount *
			// 2;
			// inset.y += insetAmount;
			// inset.height -= insetAmount *
			// 2;
			//

			Rectangle insetUp = (Rectangle) inset.clone();
			Rectangle insetDown = (Rectangle) inset.clone();

			insetUp.height = insetUp.height / 2;

			insetDown.height = -insetDown.height / 2;
			insetDown.y = -insetDown.height * 2;
			insetDown.width -= standoff;
			insetUp.width -= standoff;

			;//System.out.println(" output <" + state.outputBinned + "> \n<" + state.binned + ">");
			paintHistogram(g2, insetUp, state.numBins, new Color(0, 0, 0, 1f), state.binned, state.maxBin, 0);
			paintHistogram(g2, insetDown, state.numBins, new Color(0, 0, 0, 1f), state.outputBinned, state.maxOutputBin, 1);

			if (state.bs != null) {

				float hresf = 100f;
				GeneralPath p = new GeneralPath();
				for (int i = 0; i < hresf; i++) {
					float x = state.inputMin + (state.inputMax - state.inputMin) * i / (hresf - 1f);

					float n = state.bs.evaluate(x) * 100f;
					;//System.out.println(" n = " + n);
					if (i == 0)
						p.moveTo((bounds.width-standoff) * i / (hresf - 1), inset.height / 2 - n);
					else
						p.lineTo((bounds.width-standoff) * i / (hresf - 1), inset.height / 2 - n);
				}
				g2.setColor(new Color(1, 0.9f, 0, 1f));
				g2.draw(p);

				p = new GeneralPath();
				iFunction<Number, Number> ff = new iFunction<Number, Number>() {

					@Override
					public Number f(Number in) {
						return state.remap(in.floatValue());
					}
				};
				for (int i = 0; i < hresf; i++) {
					float x =  i / (hresf - 1f);

					float n = state.bs.evaluate(x, ff) * 100f;
//					;//System.out.println(" n = " + n);
					if (i == 0)
						p.moveTo((bounds.width-standoff) * i / (hresf - 1), inset.height / 2 + n);
					else
						p.lineTo((bounds.width-standoff) * i / (hresf - 1), inset.height / 2 + n);
				}
				g2.setColor(new Color(1, 0.9f, 0, 1f));
				g2.draw(p);
			}
		}

		if (font == null) {
			font = new Font(Constants.defaultFont, Font.PLAIN, 10);
		}

		g.setColor(new Color(1f, 0.9f, 0f, 0.75f));
		g.setFont(font);

		String left = new Formatter().format("%.2f", state.inputMin).out().toString();
		String right = new Formatter().format("%.2f", state.inputMax).out().toString();
		int w1 = g.getFontMetrics(font).charsWidth(right.toCharArray(), 0, right.length());
		g.drawString(left, 2, (int) (b.getHeight() / 2 - 20));

		int w2 = g.getFontMetrics(font).charsWidth(right.toCharArray(), 0, right.length());
		g.drawString(right, (int) (b.getWidth() - standoff - 2 - w2), (int) (b.getHeight() / 2 - 20));

		g.setColor(new Color(1f, 0.9f, 0f, 0.15f));
		g.drawLine(2+w1+2, (int) (b.getHeight() / 2 - 24), (int) (b.getWidth() - standoff - 2 - w2)-2, (int) (b.getHeight() / 2 - 24));
		
		drawLabel(g, draggables.derelativize(blackIn.hitBox.midpoint2()), 0, new Vector2(5, 5), blackIn.hitBox.x * (state.inputMax - state.inputMin) + state.inputMin);
		drawLabel(g, draggables.derelativize(greyIn.hitBox.midpoint2()), 0.5f, new Vector2(0, 5), greyIn.hitBox.x * (state.inputMax - state.inputMin) + state.inputMin);
		drawLabel(g, draggables.derelativize(whiteIn.hitBox.midpoint2()), 1, new Vector2(-5, 5), whiteIn.hitBox.x * (state.inputMax - state.inputMin) + state.inputMin);

		drawLabel(g, draggables.derelativize(blackOut.hitBox.midpoint2()), 0, new Vector2(5, -5), blackOut.hitBox.x);
		drawLabel(g, draggables.derelativize(greyOut.hitBox.midpoint2()), 0.5f, new Vector2(0, -5), greyOut.hitBox.x);
		drawLabel(g, draggables.derelativize(whiteOut.hitBox.midpoint2()), 1, new Vector2(-5, -5), whiteOut.hitBox.x);

		super.paintComponent(g);

		if (collapseness > 1)
			g2.drawImage(Constants.plus, bounds.width - 14, 2, 12, 12, null);
		else
			g2.drawImage(Constants.minus, bounds.width - 14, 2, 12, 12, null);

	}

	private void drawLabel(Graphics g, Vector2 at, float alignment, Vector2 off, double x) {
		String label = BaseMath.toDP(x, 3);
		int w = g.getFontMetrics(font).charsWidth(label.toCharArray(), 0, label.length());
		g.drawString(label, (int) (at.x + off.x - w * alignment), (int) (at.y + off.y));
	}

	private Font font;

	private void paintHistogram(Graphics2D g, Rectangle size, int numBins, Color c, Histogram<Integer> binned, float maxBin, float per) {

		for (int i = 0; i < numBins + 1; i++) {
			float v = binned.get(i, 0);

			float a = size.width * (i + 0) / (float) (numBins + 1);
			float b = size.width * (i + 1) / (float) (numBins + 1);

			draw(a, b, v / maxBin, g, size, c);
		}

		GeneralPath p = new GeneralPath();
		p.moveTo(size.x - 1, size.y + (float) (size.getHeight() - 0) + 1);
		p.lineTo(size.x - 1, size.y + (float) (size.getHeight() - 1 * size.getHeight()) - 1);
		p.lineTo(size.x + size.width + 1, size.y + (float) (size.getHeight() - 1 * size.getHeight()) - 1);
		p.lineTo(size.x + size.width + 1, size.y + (float) (size.getHeight() - 0) + 1);
		p.closePath();
		g.setColor(new Color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f * 0.1f));
		g.setStroke(new BasicStroke(1));
		g.draw(p);

		// {
		//
		// String label = "d:  " + BaseMath.toDP(minX,
		// 3) + "";
		// g.setFont(font);
		// g.setColor(baseColor);
		// int w =
		// g.getFontMetrics(font).charsWidth(label.
		// toCharArray(), 0,
		// label.length());
		// g.drawString(label, 10, (int)
		// (size.getHeight() - 10));
		// }
		// {
		//
		// String label = "d: " + BaseMath.toDP(maxX, 3)
		// + "";
		// g.setFont(font);
		// g.setColor(baseColor);
		// int w =
		// g.getFontMetrics(font).charsWidth(label.
		// toCharArray(), 0,
		// label.length());
		// g.drawString(label, (int) (size.getWidth() -
		// 10 - w), (int)
		// (size.getHeight() - 10));
		// }
		// {
		//
		// String label = "r: " + BaseMath.toDP(maxY, 3)
		// + "";
		// g.setFont(font);
		// g.setColor(baseColor);
		// int w =
		// g.getFontMetrics(font).charsWidth(label.
		// toCharArray(), 0,
		// label.length());
		// g.drawString(label, (int) ((maxYat - minX) /
		// (maxX - minX) *
		// size.getWidth() + 10), (10));
		// }
	}

	Color baseColor = new Color(1f, 0.3f, 0.3f, 0.4f);

	protected void draw(float x1, float x2, float h, Graphics2D g, Rectangle size, Color c) {

		g.setStroke(new BasicStroke(1));

		{
			GeneralPath p = new GeneralPath();
			p.moveTo(size.x + (x1 + x2) / 2, size.y + (float) (size.getHeight() - 0));
			p.lineTo(size.x + (x1 + x2) / 2, size.y + (float) (size.getHeight() - h * size.getHeight()));
			p.closePath();

			g.setColor(new Color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f * 0.5f));
			g.draw(p);
		}
		{
			GeneralPath p = new GeneralPath();
			p.moveTo(size.x + x1, size.y + (float) (size.getHeight() - 0));
			p.lineTo(size.x + x1, size.y + (float) (size.getHeight() - h * size.getHeight()));
			p.lineTo(size.x + x2, size.y + (float) (size.getHeight() - h * size.getHeight()));
			p.lineTo(size.x + x2, size.y + (float) (size.getHeight() - 0));
			p.closePath();

			g.setColor(new Color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f * 0.25f));
			g.fill(p);
		}
	}

	@Override
	protected void processMouseEvent(MouseEvent e) {
		super.processMouseEvent(e);

		if (Platform.isPopupTrigger(e)) {
			popup(e);
		}

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

	private void popup(MouseEvent e) {
		LinkedHashMap<String, iUpdateable> menu = new LinkedHashMap<String, iUpdateable>();

		menu.put("Statistics Update", null);
		menu.put((state.autoSwap ? "!" : "") + "\u21ba update <b>automatically</b>", new iUpdateable() {
			public void update() {
				state.autoSwap = true;
				setRemapChanged(true);
				irc.getControl().redraw();
			}
		});
		menu.put((!state.autoSwap ? "!" : "") + "\u21ba update <b>explicitly</b>", new iUpdateable() {
			public void update() {
				state.autoSwap = false;
				setRemapChanged(true);
				irc.getControl().redraw();
			}
		});
		menu.put("\u21ba update <b>now</b> ", new iUpdateable() {
			public void update() {
				state.swap();
				irc.getControl().redraw();
			}
		});

		menu.put("History Size (currently " + state.maxHistorySize + " items)", null);
		menu.put("  \u2191 Increase history to <b>" + (int) (state.maxHistorySize * 1.5) + "</b> items", new iUpdateable() {

			public void update() {
				state.maxHistorySize *= 1.5;
			}
		});
		menu.put("  \u2193 Decrease history to <b>" + (int) (state.maxHistorySize * 0.666) + "</b> items", new iUpdateable() {

			public void update() {
				state.maxHistorySize *= 0.66;
			}
		});
		menu.put((state.maxHistorySize == 200 ? "!" : "") + "\u02d1 <b>200</b> items", new iUpdateable() {

			public void update() {
				state.maxHistorySize = 200;
			}
		});
		menu.put((state.maxHistorySize == 100 ? "!" : "") + "\u02d1 <b>100</b> items", new iUpdateable() {

			public void update() {
				state.maxHistorySize = 100;
			}
		});
		menu.put((state.maxHistorySize == 50 ? "!" : "") + "\u02d1 <b>50</b> items", new iUpdateable() {

			public void update() {
				state.maxHistorySize = 50;
			}
		});

//		menu.put("Histogram Resolution (currently " + state.numBins + " bins)", null);
//		menu.put("  \u2191 Increase number of bins to <b>" + (int) (state.numBins * 1.5) + "</b> ", new iUpdateable() {
//
//			public void update() {
//				state.numBins *= 1.5;
//				state.changed = true;
//			}
//		});
//		menu.put("  \u2193 Decrease number of bins to<b>" + (int) (state.maxHistorySize * 0.666) + "</b> ", new iUpdateable() {
//
//			public void update() {
//				state.numBins *= 0.666;
//				state.changed = true;
//			}
//		});

		menu.put("Input tracking", null);
		menu.put((state.inputRangeLocked ? "!" : "") + "\u2016 <b>Lock input</b> range", new iUpdateable() {

			public void update() {
				state.inputRangeLocked = true;
			}
		});
		menu.put((!state.inputRangeLocked ? "!" : "") + "\u21AD <b>Fit input</b> range to data", new iUpdateable() {

			public void update() {
				state.inputRangeLocked = false;
			}
		});
		// TODO
		
		Canvas canvas = irc.getControl();
		Point p = Launcher.display.map(canvas, irc.getControl().getShell(), new Point(e.getX(), e.getY()));
		new SmallMenu().createMenu(menu, irc.getControl().getShell(), null).show(p);

	}

	private void pop(JPopupMenu menu, MouseEvent e) {
		if (isDisplayable()) {
			menu.show(getParent(), e.getX() - getParent().bounds().x, e.getY() - getParent().bounds().y);
		} else {
			GLComponentWindow window = GLComponentWindow.getCurrentWindow(null);
			// why the -30 ?
			// TODO
			// menu.show(window.getCanvas(), e.getX() +
			// this.getBounds().x, e.getY() + this.getBounds().y -
			// 30);
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

	boolean isRemapChanged() {
		return remapChanged;
	}

	void setRemapChanged(boolean remapChanged) {
		this.remapChanged = remapChanged;
	}
}
