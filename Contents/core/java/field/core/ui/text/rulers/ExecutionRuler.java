package field.core.ui.text.rulers;

import java.awt.FontMetrics;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Event;

import field.core.Constants;
import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.execution.PythonGeneratorStack;
import field.core.plugins.python.PythonPluginEditor;
import field.core.ui.SmallMenu;
import field.core.ui.text.rulers.ExecutedAreas.Area;
import field.core.ui.text.rulers.ExecutedAreas.State;
import field.core.util.LocalFuture;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.abstraction.iAcceptor;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;
import field.math.util.Histogram;
import field.math.util.iAverage;
import field.namespace.generic.Bind.iFunction;
import field.util.Dict.Prop;

public class ExecutionRuler implements iRuler {

	public enum DragType {
		nothing, dragTop, dragBottom, dragBody;
	}

	private final StyledText p;

	private final Canvas ruler;

	private Vector2 mouseAt = new Vector2();

	private final ExecutedAreas areas;

	private Area currentBest;

	Set<Area> highlighted = new HashSet<Area>();

	DragType currentDrag;

	Area dragTarget;

	public ExecutionRuler(StyledText p, Canvas ruler) {
		this.p = p;
		this.ruler = ruler;
		areas = new ExecutedAreas(p) {
			@Override
			protected void merge(Area a, Area b) {

				// b is disappearing
				for (Area remaining : areas) {
					if (remaining != b) {
						Histogram<Area> t = remaining.extend.get(ExecutedAreas.forwardTransitions);
						if (t != null) {
							float m = t.get(b, 0);
							t.visit(a, m);
							t.removeKey(b);
						}
					}
				}
			}
		};
	}

	public void drawConnection(GC g, Vector2 from, Area to, Vector4 color, float scale) {

		setForeground(g, color);
		Vector2 toto = hitPointFor(to, scale);
		Path path = new Path(g.getDevice());

		toto.x = to.allocation*10+2;
		
		System.out.println(" draw connection <"+from+" -> "+toto+"> <"+scale+">");
		
		if (from.distanceFrom(toto) > 0) {

			Vector2 forward = new Vector2().sub(toto, from);
			Vector2 left = new Vector2(forward.y, -forward.x).normalize().scale(5);

			Vector2 i1 = new Vector2().lerp(from, toto, 1 / 3f).add(left);
			Vector2 i2 = new Vector2().lerp(from, toto, 2 / 3f).add(left);

			
			if (toto.x>from.x)
			{
				i1.x = from.x+5;
				i1.y = from.y;
				i2.x = (from.x+5+toto.x)/2;
				i2.y = (from.y+toto.y)/2;
			}
			else
			{
				i1.x = from.x-5;
				i1.y = from.y;
				i2.x = (from.x-5+toto.x)/2;
				i2.y = (from.y+toto.y)/2;
			}
			
			
			path.moveTo(from.x, from.y);
			path.cubicTo(i1.x, i1.y, i2.x, i2.y, toto.x, toto.y);
			g.setForeground(new Color(Launcher.display, 255, 128, 0));
			g.setAlpha(64);
			g.setLineWidth((int) (scale*5+1));
			g.setLineCap(SWT.CAP_ROUND);
			g.drawPath(path);
		}

	}

	private void setForeground(GC g, Vector4 color) {
		g.setForeground(new Color(g.getDevice(), (int) (color.x * 255), (int) (color.y * 255), (int) (color.z * 255)));
		g.setAlpha((int) (color.w * 255));
	}

	private void setBackground(GC g, Vector4 color) {
		g.setBackground(new Color(g.getDevice(), (int) (color.x * 255), (int) (color.y * 255), (int) (color.z * 255)));
		g.setAlpha((int) (color.w * 255));
	}

	public Area getCurrentArea() {
		return currentBest;
	}

	public ExecutedAreas getExecutedAreas() {
		return areas;
	}

	public void highlight(Area a) {
		highlighted.add(a);
	}

	public void keyEvent(Event e, int caretPosition, int selectionStart, int selectionEnd) {

		if ((e.stateMask & SWT.COMMAND) != 0 && (e.stateMask & SWT.ALT) != 0) {

			if (e.keyCode == SWT.ARROW_LEFT) {

				// find the lowest allocation area that this is
				// in
				Area minIs = getMin(caretPosition);
				System.out.println(" arrow left it is <" + minIs + ">");
				if (minIs != null && (e.stateMask & SWT.SHIFT) == 0) {
					executeArea(minIs);
				} else if (minIs != null && (e.stateMask & SWT.SHIFT) != 0) {
					executeAreaSpecial(minIs);
				}
			}
			if (e.keyCode == SWT.ARROW_UP) {
				// find the lowest allocation area that this is
				// in
				Area minIs = getMin(caretPosition);
				if (minIs != null) {
					// executeArea(minIs);
					OpportinisticSlider s = minIs.textend.get(OpportinisticSlider.oSlider);
					if (s != null) {
						executeAreaAndRewrite(minIs, s.getUp());
					}
				}
			}
			if (e.keyCode == SWT.ARROW_DOWN) {
				// find the lowest allocation area that this is
				// in
				Area minIs = getMin(caretPosition);
				if (minIs != null) {
					// executeArea(minIs);
					OpportinisticSlider s = minIs.textend.get(OpportinisticSlider.oSlider);
					if (s != null) {
						executeAreaAndRewrite(minIs, s.getDown());
					}
				}
			}
			if (e.keyCode == SWT.ARROW_RIGHT) {
				// find the lowest allocation area that this is
				// in
				int alloc = Integer.MIN_VALUE;
				int lfp = lineForPosition(caretPosition, p);
				System.out.println(" line for position <" + caretPosition + "> is <" + lfp + ">");
				Area minIs = null;
				for (Area a : areas.areas) {
					a.update(p);
					System.out.println("looking at area <" + a.allocation + " " + a.lineStart + " " + a.lineEnd + "> <" + !a.invalid + ">");
					if (a.lineStart <= lfp && a.lineEnd >= lfp && !a.invalid) {
						if (a.allocation > alloc) {
							alloc = a.allocation;
							minIs = a;
						}
					}
				}
				if (minIs != null && (e.stateMask & SWT.SHIFT) == 0) {
					executeArea(minIs);
				} else if (minIs != null && (e.stateMask & SWT.SHIFT) != 0) {
					executeAreaSpecial(minIs);
				}
			}

		}

		if ((e.stateMask & SWT.CTRL) != 0 && (e.stateMask & SWT.COMMAND) != 0) {

			System.out.println(" looking for shortcut");

			List<Area> candidate = new ArrayList<Area>();
			int code = e.keyCode;
			for (Area a : areas.areas) {
				Character c = a.extend.get(ExecutedAreas.keyboardShortcut);
				System.out.println(" c is <" + c + "> code is <" + code + " " + (c == null ? -1 : (int) c));
				if (c != null && (c.toUpperCase(c)) == code && !a.invalid) {
					candidate.add(a);
				}
			}
			System.out.println(" found <" + candidate + ">");
			if (candidate.size() == 0)
				return;
			if (candidate.size() == 1) {
				executeArea(candidate.get(0));
				return;
			}
			int lfp = lineForPosition(caretPosition, p);
			float d = Float.POSITIVE_INFINITY;
			Area dIs = null;
			for (Area a : candidate) {
				float mz = Float.POSITIVE_INFINITY;
				if (a.lineStart >= lfp && a.lineEnd <= lfp) {
					mz = (float) (1 - Math.exp(-Math.abs(a.lineEnd - a.lineStart)));
				} else {
					mz = Math.min(Math.abs(a.lineStart - lfp), Math.abs(a.lineEnd - lfp));
				}
				if (mz < d) {
					d = mz;
					dIs = a;
				}
			}

			executeArea(dIs);

		} else if ((e.stateMask & SWT.CTRL) != 0 && e.keyCode == 'u') {
			System.out.println(" unit test !");

			Area minIs = getMin(caretPosition);

			if (minIs == null && selectionStart == selectionEnd) {

				String text = p.getText();
				int a = text.lastIndexOf("\n", caretPosition - 1);
				if (a == -1)
					a = 0;
				int b = text.indexOf("\n", caretPosition);
				if (b == -1)
					b = text.length();
				String s = text.substring(a, b);
				minIs = getExecutedAreas().execute(a + 2, b - 1, s);
			}

			if (selectionStart != selectionEnd) {

				minIs = getExecutedAreas().execute(selectionStart, selectionEnd - 1);

			}

			if (minIs != null) {

				boolean isTest = minIs.extend.isTrue(ExecutedAreas.isUnitTest, false);
				System.out.println(" unit test on <" + minIs + "> <" + isTest + "> <" + selectionStart + "> <" + selectionEnd + ">");

				if (isTest) {
					final Area fminIs = minIs;

					if ((e.stateMask & SWT.SHIFT) != 0) {
						runAndReviseArea(minIs);
					} else {
						runAndCheckArea(minIs).addContinuation(new iAcceptor<Boolean>() {
							public iAcceptor<Boolean> set(Boolean to) {
								fminIs.extend.put(ExecutedAreas.passedUnitTest, to);
								p.redraw();
								return null;
							}
						});
					}
					return;
				} else {
					runAndReviseArea(minIs);
					minIs.extend.put(ExecutedAreas.isUnitTest, true);
					minIs.extend.put(ExecutedAreas.passedUnitTest, true);
					p.redraw();
				}
			}

		} else if ((e.stateMask & SWT.CTRL) != 0 && e.keyCode == 'h') {
			Area m = getMin(caretPosition);
			if (m != null)
				browseExecutionHistory(m).update();
		}
	}

	public Area getMin(int caretPosition) {
		int alloc = Integer.MAX_VALUE;
		int lfp = lineForPosition(caretPosition, p);
		System.out.println(" line for position <" + caretPosition + "> is <" + lfp + ">");
		Area minIs = null;
		for (Area a : areas.areas) {
			a.update(p);
			System.out.println("looking at area <" + a.allocation + " " + a.lineStart + " " + a.lineEnd + "> <" + !a.invalid + ">");
			if (a.lineStart <= lfp && a.lineEnd >= lfp && !a.invalid) {
				if (a.allocation < alloc) {
					alloc = a.allocation;
					minIs = a;
				}
			}
		}
		return minIs;
	}

	@Override
	public void mouseDown(MouseEvent e) {

		System.out.println(" mouse down <" + e.x + " " + e.y + ">");

		DragType cd = computeDrag(e);
		currentDrag = cd;
		if (currentDrag != null && Platform.isPopupTrigger(e)) {
			popup(e, currentDrag, dragTarget);
		}

	}

	public void mouseUp(MouseEvent e) {

		System.out.println(" mouse down on area");

		DragType cd = computeDrag(e);
		currentDrag = cd;
		// if (currentDrag != null && Platform.isPopupTrigger(e)) {
		// popup(e, currentDrag, dragTarget);
		// }

	}

	@Override
	public void mouseMove(MouseEvent e) {

		DragType cd = computeDrag(e);
		mouseOverAt(e.x, e.y);

	}

	public void mouseEvent(Event e) {

		// we are interested in drag events that drag vertically from
		// the edges of areas or horizontally from not-edges

		// TODO swt momentum
		// if (e.getID() == e.MOUSE_PRESSED) {
		// DragType cd = computeDrag(e);
		// currentDrag = cd;
		//
		// if (currentDrag != null && Platform.isPopupTrigger(e)) {
		// popup(e, currentDrag, dragTarget);
		// }
		//
		// } else if (e.getID() == e.MOUSE_RELEASED && currentDrag !=
		// DragType.nothing) {
		// finalizeDrag(e);
		// currentDrag = DragType.nothing;
		// } else if (e.getID() == e.MOUSE_EXITED && currentDrag !=
		// DragType.nothing) {
		// finalizeDrag(e);
		// currentDrag = DragType.nothing;
		// } else if (currentDrag != DragType.nothing)
		// updateDrag(e);
		// else if (e.getID() == e.MOUSE_MOVED) {
		// DragType cd = computeDrag(e);

		// TODO swt cursor
		// if (cd == DragType.dragTop)
		// e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
		// else if (cd == DragType.dragBottom)
		// e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
		// else
		// e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		// }

	}

	public void mouseExited() {
		currentBest = null;
	}

	public boolean mouseOverAt(float x, float y) {
		mouseAt = new Vector2(x, y);
		Area nextBest = computeMouseSelection();
		if (nextBest != currentBest) {
			currentBest = nextBest;
			ruler.redraw();
			return true;
		}
		return false;
	}

	public void off() {
	}

	public void on() {
	}

	public void paintNow(final GC g, StyledText p, Canvas ruler) {
		g.setAdvanced(true);
		g.setAntialias(SWT.ON);

		Rectangle r = p.getClientArea();

		String[] splat = p.getText().split("\n");
		int at = 0;

		Font font = new Font(g.getDevice(), Constants.defaultFont, 8, SWT.NORMAL);
		g.setFont(font);
		g.setForeground(new Color(Launcher.display, 0, 0, 0));
		g.setAlpha(64);

		int last = 0;
		for (int i = 0; i < splat.length; i++) {
			if (splat[i].trim().length() == 0)
				continue;
			int pix = p.getLinePixel(i);

			if (pix < 0)
				continue;
			if (pix > 2000)
				break;

			Point e = g.textExtent("" + (i + 1));

			if ((last + 1) / 10 != (i + 1) / 10)
				g.setAlpha(128);
			else
				g.setAlpha(64);

			g.drawString("" + (i + 1), (int) (ruler.getClientArea().width - 5 - e.x), pix + 4);
			at += splat[i].length() + 1;

			last = i;
		}

		areas.coalesceAreas();

		int n = 0;
		List<Area> all = areas.areas;
		for (Area a : all) {
			a.update(p);

			if (!a.invalid) {
				Path drawBracket = drawBracket(g, a.allocation * 10 + 2, 10, a.lineStart, a.lineEnd, a == currentBest ? 2 : 0.5f, highlighted.contains(a) ? 0.5f : 0);

				if (a.extend.isTrue(ExecutedAreas.isUnitTest, false)) {
					float[] bounds = new float[4];
					drawBracket.getBounds(bounds);

					font = new Font(g.getDevice(), Constants.defaultFont, 28, SWT.NORMAL);
					g.setFont(font);
					Boolean passed = a.extend.get(ExecutedAreas.passedUnitTest);
					if (passed != null && passed) {
						setForeground(g, new Vector4(0, 0.5, 0, 0.75));
						g.drawString("\u24e4", (int) (bounds[0] + 15), (int) (bounds[1] + bounds[3] / 2 - 13), true);

					} else {
						setForeground(g, new Vector4(1, 0, 0, 0.75));
						g.drawString("\u24e4", (int) (bounds[0] + 15), (int) (bounds[1] + bounds[3] / 2 - 13), true);
					}
				}

				Character k = a.extend.get(ExecutedAreas.keyboardShortcut);
				if (k != null) {
					drawKeyboardShortcut(g, k, a.allocation * 10 + 2, a.lineStart, a.lineEnd, a == currentBest);
				}
				Object monitor = a.textend.get(new Prop("monitor"));
				if (monitor != null) {
					if (!updateMonitor(monitor))
						a.textend.remove(new Prop("monitor"));
					else
						drawMonitor(g, a.allocation * 10 + 2, 10, a.lineStart, a.lineEnd);
				}

				OpportinisticSlider slider = a.textend.get(OpportinisticSlider.oSlider);
				if (slider != null) {
					drawOpportunisticSlider(slider, g, a.allocation * 10 + 2, a.lineStart, a.lineEnd);
				}

			}
			n++;
			if (n > 100)
				break;

		}
		Area histogram = areas.getCurrentArea();
		if (histogram != null) {
			drawForwardTransitions(g, histogram, histogram.extend.get(ExecutedAreas.forwardTransitions));
			drawOrigin(g, histogram, new Vector4(0, 0, 0, 0.5f));
		} else {
			Histogram<Area> h = areas.getRootHistogram();
			drawForwardTransitions(g, null, h);
			h.average(new iAverage<Area, Object>() {

				public <X extends Area> void accept(X accept, double weight) {
					drawOrigin(g, accept, new Vector4(0, 0, 0, weight / 2));
				}

				public void begin(int num, double totalWeight) {
				}

				public Object end() {
					return null;
				}
			});

		}

	}

	private void drawOpportunisticSlider(OpportinisticSlider slider, GC g, int i, int lineStart, int lineEnd) {

		// not ready for prime time
		if (true)
			return;

		try {
			Rectangle start = rectangleForLine(lineStart);
			Rectangle end = rectangleForLine(lineEnd);

			float y = (start.y + end.y + end.height) / 2;
			g.setFont(new Font(g.getDevice(), "Gill Sans", 20, SWT.NORMAL));
			setForeground(g, new Vector4(0, 0, 0, 0.5f));
			g.drawString("\u21c5", i + 8, (int) (y + 3), true);

		} catch (Exception e) {
		}
	}

	private void drawMonitor(GC g, int x, int q, int lineStart, int lineEnd) {
		boolean over = false;
		Rectangle start = rectangleForLine(lineStart);
		Rectangle end = rectangleForLine(lineEnd);

		float y = (start.y + end.y + end.height) / 2;
		g.setFont(new Font(g.getDevice(), "Gill Sans", 10, SWT.NORMAL));
		setForeground(g, new Vector4(0, 0, 0, 0.5f));

		g.drawString("(running...)", x + 8, (int) (y - 6), true);

		// Icon i = SmallMenu.makeIconFromCharacterShadowed('\u00d7',
		// 20, 18,
		// 0, new Color(0, 0, 0, 0.15f), new Color(0, 0, 0, 0.15f));
		// g.drawImage(((ImageIcon) i).getImage(), x + 6, (int) (y - 8),
		// null);

	}

	public boolean updateMonitor(Object monitor) {
		if (monitor instanceof PythonGeneratorStack) {
			return !((PythonGeneratorStack) monitor).isOver();
		}
		return false;
	}

	public void unhighlight() {
		highlighted.clear();
	}

	private DragType computeDrag(MouseEvent arg0) {
		mouseAt.x = arg0.x;
		mouseAt.y = arg0.y;

		dragTarget = computeMouseSelection();

		// mouseAt.x = e.getX();
		// mouseAt.y = e.getY();

		if (dragTarget != null) {
			Rectangle start = rectangleForLine(dragTarget.lineStart);
			Rectangle end = rectangleForLine(dragTarget.lineEnd);
			float startX = dragTarget.allocation * 10 + 2;
			float midY = (start.y + end.y + end.height) / 2;

			// boolean inside = e.getX() > startX &&
			// e.getY() > start.y && e.getY() < (end.y +
			// end.height);
			if (Math.abs(arg0.y - start.y) < 5)
				return DragType.dragTop;
			if (Math.abs(arg0.y - end.y - end.height) < 5)
				return DragType.dragBottom;
			return DragType.dragBody;
		}
		return DragType.nothing;
	}

	private float distance(Vector2 mouseAt2, Area a) {
		Rectangle start = rectangleForLine(a.lineStart);
		Rectangle end = rectangleForLine(a.lineEnd);

		float startX = a.allocation * 10 + 2;
		float midY = (start.y + end.y + end.height) / 2;

		boolean inside = mouseAt2.x > startX && mouseAt.y > start.y && mouseAt.y < (end.y + end.height);
		if (inside) {
			float d = (mouseAt2.x - startX) * (mouseAt2.x - startX) + (mouseAt2.y - midY) * (mouseAt2.y - midY);
			float size = Math.abs(end.y - start.y);

			return d + size;
		} else {
			return Float.POSITIVE_INFINITY;
		}

	}

	private void drawForwardTransitions(final GC g, Area a, Histogram<Area> histogram) {
		if (histogram == null)
			return;

		final Vector2 at = a == null ? null : hitPointFor(a, 1);

		if (at!=null)		at.x = a.allocation*10+2;
		
		histogram.average(new iAverage<Area, Object>() {

			private double t;

			public <X extends Area> void accept(X accept, double weight) {
				if (accept != null) {
					float amount = (float) (weight);
					try {
						drawBracket(g, accept.allocation * 10 + 2, 10, accept.lineStart, accept.lineEnd, new Vector4(0, 0, 0, amount), new Vector4(0, 0, 0, 0.0), 0.5f);

						if (at != null)
							drawConnection(g, at, accept, new Vector4(0, 0, 0, amount / 2), (float) (weight));
					} catch (IllegalArgumentException e) {
					}
					;
				}
			}

			public void begin(int num, double totalWeight) {
				t = totalWeight;
			}

			public Object end() {
				return null;
			}
		});
	}

	private void drawKeyboardShortcut(GC g, char c, int x, int lineStart, int lineEnd, boolean best) {
		try {
			Rectangle start = rectangleForLine(lineStart);
			Rectangle end = rectangleForLine(lineEnd);

			float y = (start.y + end.y + end.height) / 2;
			g.setFont(new Font(g.getDevice(), "Gill Sans", 20, best ? SWT.BOLD : SWT.NORMAL));
			setForeground(g, new Vector4(0, 0, 0, 0.5f));
			g.drawString((best ? "\u2303\u2318" : "") + c, x + 8, (int) (y + 3 + (best ? 2 : 0)), true);
		} catch (Exception e) {
		}
	}

	private void drawOrigin(GC g, Area a, Vector4 fillColor) {
		Rectangle start = rectangleForLine(a.lineStart);
		Rectangle end = rectangleForLine(a.lineEnd);

		float x = a.allocation * 10 + 2;
		float midY = (start.y + end.y + end.height) / 2;

		float r = 5;
		Ellipse2D hit = new Ellipse2D.Float(x, midY - r / 2, r, r);

		setForeground(g, new Vector4(fillColor.x, fillColor.y, fillColor.z, fillColor.w / 3));
		setBackground(g, new Vector4(fillColor.x, fillColor.y, fillColor.z, fillColor.w / 3));
//		g.fillOval((int) x, (int) (midY - r / 2), (int) r, (int) r);
//		g.drawOval((int) x, (int) (midY - r / 2), (int) r, (int) r);
	}

	private void finalizeDrag(Event e) {
	}

	private Vector2 hitPointFor(Area a, float scale) {
		Rectangle start = rectangleForLine(a.lineStart);
		Rectangle end = rectangleForLine(a.lineEnd);

		float midY = (start.y + end.y + end.height) / 2;

		float r = 5;
		return new Vector2(start.x + r / 2, midY);
	}

	private int lineForPosition(int i, StyledText p) {
		String[] lines = p.getText().split("\n");
		int at = 0;
		for (int n = 0; n < lines.length; n++) {
			at += lines[n].length() + 1;
			if (at > i)
				return n;
		}
		return lines.length;
	}

	private int positionForLine(int i, StyledText p) {
		String[] lines = p.getText().split("\n");
		int at = 0;
		for (int n = 0; n < Math.min(lines.length, i); n++) {
			at += lines[n].length() + 1;
		}
		return at;
	}

	private void updateDrag(Event e) {

		if (currentDrag == DragType.dragTop) {
			int l = lineForY(e.y);

			dragTarget.freeze();
			dragTarget.lineStart = l + 1;
			dragTarget.update(this.p);
			this.p.redraw();
		}
		if (currentDrag == DragType.dragBottom) {
			int l = lineForY(e.y);

			dragTarget.freeze();
			dragTarget.lineEnd = l + 1;
			dragTarget.update(this.p);
			this.p.redraw();
		}
	}

	protected Area computeMouseSelection() {
		List<Area> all = areas.areas;

		float d = Float.POSITIVE_INFINITY;

		Area best = null;
		for (Area a : all) {
			if (!a.invalid) {
				float d2 = distance(mouseAt, a);
				if (d2 < d) {
					d = d2;
					best = a;
				}
			}
		}

		return best;
	}

	public Path drawBracket(GC g, int x, int width, int lineStart, int lineEnd, float weight, float shade) {
		return drawBracket(g, x, width, lineStart, lineEnd, new Vector4((1 - shade) * shade, shade, 0, weight * 0.3f), new Vector4((1 - shade) * shade, shade, 0, weight * 0.3f), 1);
	}

	protected Path drawBracket(GC g, int x, int width, int lineStart, int lineEnd, Vector4 fillColor, Vector4 strokeColor, float scale) {
		Rectangle start = rectangleForLine(lineStart);
		Rectangle end = rectangleForLine(lineEnd);

		Path bracket = new Path(g.getDevice());
		bracket.moveTo(x + width, start.y);
		bracket.cubicTo(x, start.y, x + width, (start.y + end.y + end.height) / 2, x, (start.y + end.y + end.height) / 2);
		bracket.cubicTo(x + width, (start.y + end.y + end.height) / 2, x, (end.y + end.height), x + width, (end.y + end.height));

		float s = 5;
		bracket.cubicTo( x+s, (end.y + end.height), x + width, (start.y + end.y + end.height) / 2, x, (start.y + end.y + end.height) / 2);
		bracket.cubicTo( x + width, (start.y + end.y + end.height) / 2, x+s, start.y, x+width, start.y);

		setForeground(g, new Vector4(strokeColor.x, strokeColor.y, strokeColor.z, strokeColor.w));

		g.fillPath(bracket);
		
		
		float[] bounds = new float[4];
		bracket.getBounds(bounds);
		double cx = bounds[0] + bounds[2] / 2;
		double cy = bounds[1] + bounds[3] / 2;

		if (scale != 1) {
			Transform t = new Transform(g.getDevice());
			t.translate(-(float) cx, -(float) cy);
			t.scale(scale, scale);
			t.translate((float) cx, (float) cy);
		}

//		g.drawPath(bracket);

		Path fillBracket = new Path(g.getDevice());
		fillBracket.moveTo(x + 5000, start.y);
		fillBracket.lineTo(x + width, start.y);
		fillBracket.cubicTo(x, start.y, x + width, (start.y + end.y + end.height) / 2, x, (start.y + end.y + end.height) / 2);
		fillBracket.cubicTo(x + width, (start.y + end.y + end.height) / 2, x, (end.y + end.height), x + width, (end.y + end.height));
		fillBracket.lineTo(x + 5000, (end.y + end.height));

		if (scale != 1) {
			Transform t = new Transform(g.getDevice());
			t.translate(-(float) cx, -(float) cy);
			t.scale(scale, scale);
			t.translate((float) cx, (float) cy);
		}

		setForeground(g, new Vector4(fillColor.x, fillColor.y, fillColor.z, fillColor.w / 3));
		setBackground(g, new Vector4(fillColor.x, fillColor.y, fillColor.z, fillColor.w / 3));
		g.fillPath(fillBracket);
//		g.drawPath(fillBracket);

		
		
		
		if (scale == 1) {
			int r = 5;
			Ellipse2D hit = new Ellipse2D.Float(x, (start.y + end.y + end.height) / 2 - r / 2, r, r);

			setForeground(g, new Vector4(fillColor.x, fillColor.y, fillColor.z, fillColor.w / 3));
//			g.fillOval((int) x, (int) (start.y + end.y + end.height) / 2 - r / 2, r, r);
			setForeground(g, new Vector4(fillColor.x, fillColor.y, fillColor.z, fillColor.w));
//			g.drawOval(x, (start.y + end.y + end.height) / 2 - r / 2, r, r);
		}

		return bracket;
	}

	protected void executeAreaAndRewrite(Area minIs, iFunction<String, String> up) {
	}

	public void executeArea(Area area) {
	}

	protected void executeAreaSpecial(Area area) {
	}

	protected LocalFuture<Boolean> runAndCheckArea(Area area) {
		return null;
	}

	protected void runAndReviseArea(Area area) {
	}

	protected int lineForY(float y) {
		return p.getLineIndex((int) y);
		// System.out.println(" visi rect <" + p.getVisibleRect().y +
		// ">");
		// int position = p.viewToModel(new Point(0, (int) y +
		// p.getVisibleRect().y));
		// return lineForPosition(position, p);
	}

	protected void popup(MouseEvent e, DragType dt, final Area target) {
		LinkedHashMap<String, iUpdateable> menu = new LinkedHashMap<String, iUpdateable>();
		if (target != null) {
			menu.put("Operations on Area", null);
			menu.put("  \u232b <b>Delete Area</b>", new iUpdateable() {
				public void update() {
					areas.deleteArea(target);
					p.redraw();
				}
			});
			menu.put("  \u2190 <b>Promote</b> area ", new iUpdateable() {
				public void update() {
					areas.promote(target);
					p.redraw();
				}
			});
			menu.put("  \u2328 Assign <b>keyboard shortcut</b>", new iUpdateable() {
				public void update() {

					Rectangle r1 = rectangleForLine(target.lineStart);
					Rectangle r2 = rectangleForLine(target.lineEnd);
					Point pp = new Point(r1.x, (r1.y + r2.y) / 2);
					Launcher.display.map(null, ruler, pp);

					Character d = target.extend.get(ExecutedAreas.keyboardShortcut);

					// TODO swt popuptextbox

					// PopupTextBox.Modal.getString(pp,
					// "\u2303\u2318", d == null ? ""
					// : ("" + d),
					// new iAcceptor<String>() {
					// public iAcceptor<String> set(
					// String to) {
					// target.extend
					// .put(ExecutedAreas.keyboardShortcut,
					// to.charAt(0));
					// return this;
					// }
					// });
				}
			});

			// menu.put(" \u27f2 Browse <b>execution history</b> ///control H/// ",
			// browseExecutionHistory(target));

			menu.put("Unit testing", null);
			boolean isTest = target.extend.isTrue(ExecutedAreas.isUnitTest, false);
			if (isTest) {
				menu.put(" \u24e4 <b>Run</b> unit test ///control U///", new iUpdateable() {
					public void update() {
						runAndCheckArea(target).addContinuation(new iAcceptor<Boolean>() {
							public field.math.abstraction.iAcceptor<Boolean> set(Boolean to) {
								target.extend.put(ExecutedAreas.passedUnitTest, to);
								p.redraw();
								return this;
							};
						});
					}
				});
				menu.put(" \u24e4 Run & <b>revise</b> unit test ///shift control U///", new iUpdateable() {
					public void update() {
						runAndReviseArea(target);
						target.extend.put(ExecutedAreas.passedUnitTest, true);
						p.redraw();
					}
				});
				menu.put(" \u24e4 <b>Forget</b> unit test", new iUpdateable() {
					public void update() {
						target.extend.put(ExecutedAreas.isUnitTest, false);
						p.redraw();
					}
				});
			} else {
				menu.put(" \u24e4 Make <b>new unit test</b> ///control U///", new iUpdateable() {

					public void update() {
						runAndReviseArea(target);
						target.extend.put(ExecutedAreas.isUnitTest, true);
						target.extend.put(ExecutedAreas.passedUnitTest, true);
						p.redraw();
					}
				});
			}

		}

		boolean found = hasUnitTests(this);

		if (found) {
			if (target == null) {
				menu.put("Unit testing", null);
			}

			menu.put(" \u24ca Run <b>all unit tests</b> in this element", new iUpdateable() {

				public void update() {
					runAllUnitTests(ExecutionRuler.this);
				}
			});

			menu.put(" \u24ca <b>Revise all</b> unit tests in this element", new iUpdateable() {

				public void update() {
					for (Area a : areas.areas) {
						if (a.extend.isTrue(ExecutedAreas.isUnitTest, true)) {
							runAndReviseArea(target);
							target.extend.put(ExecutedAreas.passedUnitTest, true);
						}
						p.redraw();
					}
				}
			});
			menu.put(" \u24ca <b>Forget all</b> unit tests in this element", new iUpdateable() {

				public void update() {
					for (Area a : areas.areas) {
						if (a.extend.isTrue(ExecutedAreas.isUnitTest, true)) {
							target.extend.put(ExecutedAreas.isUnitTest, false);
						}
						p.redraw();
					}
				}
			});
		}
		if (menu.size() > 0)
			new SmallMenu().createMenu(menu, ruler.getShell(), null).show(Launcher.display.map(ruler, ruler.getShell(), new Point(e.x, e.y)));
	}

	private iUpdateable browseExecutionHistory(final Area target) {
		return null;
		// TODO swt bettertextselection
		// return new iUpdateable() {
		//
		// @Override
		// public void update() {
		// try {
		//
		// Set<String> options = new LinkedHashSet<String>();
		//
		// // final String[] options = new String[target.exec.size()];
		//
		// Iterator<ExecutionRecord> i = target.exec.iterator();
		// int ii = 0;
		// while (i.hasNext()) {
		// ExecutionRecord rec = i.next();
		// options.add(rec.stringAtExecution.trim());
		// }
		//
		// Rectangle r1 = rectangleForLine(target.lineStart);
		// Rectangle r2 = rectangleForLine(target.lineEnd);
		// Point pp = new Point(r1.x, (r1.y + r2.y) / 2);
		// SwingUtilities.convertPointToScreen(pp, ruler);
		//
		// if (options.size() > 10) {
		// options = new LinkedHashSet<String>(
		// new ArrayList<String>(options).subList(0, 10));
		// }
		//
		// final String[] oo = options.toArray(new String[0]);
		// new BetterTextSelection(pp.getX(), pp.getY(), "Execution",
		// oo, 300) {
		// @Override
		// protected String getBannerForMouse(int e) {
		// if ((e & SWT.ALT) != 0) {
		// return "Execute";
		// } else
		// return "Copy";
		// }
		//
		// @Override
		// protected void escape() {
		// frame.setVisible(false);
		// }
		//
		// @Override
		// protected void act(final int fi, int modifiers) {
		// escape();
		//
		// if ((modifiers & SWT.ALT) != 0) {
		// executeAreaAndRewrite(target,
		// new iFunction<String, String>() {
		// @Override
		// public String f(String in) {
		// return oo[fi];
		// }
		// });
		// } else {
		// Clipboard c = Toolkit.getDefaultToolkit()
		// .getSystemClipboard();
		// c.setContents(new StringSelection(oo[fi]), null);
		// }
		// }
		// };
		// } catch (BadLocationException e) {
		// e.printStackTrace();
		// }
		//
		// }
		// };
	}

	static public boolean hasUnitTests(ExecutionRuler ruler) {
		boolean found = false;
		for (Area a : ruler.areas.areas) {
			if (a.extend.isTrue(ExecutedAreas.isUnitTest, false)) {
				found = true;
				break;
			}
		}
		return found;
	}

	public Rectangle rectangleForLine(int line) {

		int h = p.getLineHeight(positionForLine(line, p));
		Point p1 = p.getLocationAtOffset(positionForLine(line, p));
		Point p2 = Launcher.display.map(p, ruler, p1);

		return new Rectangle(p2.x, p2.y, 0, h);
	}

	public Rectangle rectangleForPosition(int pos) {

		int h = p.getLineHeight(pos);
		Point p1 = p.getLocationAtOffset(pos);
		Point p2 = Launcher.display.map(p, ruler, p1);

		return new Rectangle(p2.x, p2.y, 0, h);
	}

	static public void runAllUnitTests(final ExecutionRuler ruler) {
		for (final Area a : ruler.areas.areas) {
			if (a.extend.isTrue(ExecutedAreas.isUnitTest, false)) {
				LocalFuture<Boolean> r = ruler.runAndCheckArea(a);
				r.addContinuation(new iAcceptor<Boolean>() {

					public iAcceptor<Boolean> set(Boolean to) {
						a.extend.put(ExecutedAreas.passedUnitTest, to);
						ruler.p.redraw();
						return this;
					}
				});
			}
			ruler.p.redraw();
		}
	}

	static public boolean hasUnitTests(iVisualElement element, VisualElementProperty<String> p) {
		Map<String, State> state = element.getProperty(PythonPluginEditor.python_areas);
		if (state == null)
			return false;
		State a = state.get(p.getName());
		if (a == null)
			return false;
		boolean found = false;
		for (Area aa : a.areas) {
			if (aa.extend.isTrue(ExecutedAreas.isUnitTest, false)) {
				found = true;
				break;
			}
		}
		return found;
	}

	public static int hasFailingTests(iVisualElement element, VisualElementProperty<String> p) {
		Map<String, State> state = element.getProperty(PythonPluginEditor.python_areas);
		if (state == null)
			return 0;
		State a = state.get(p.getName());
		if (a == null)
			return 0;
		boolean found = false;
		int r = 0;
		for (Area aa : a.areas) {
			if (aa.extend.isTrue(ExecutedAreas.isUnitTest, false)) {
				if (!aa.extend.isTrue(ExecutedAreas.passedUnitTest, true)) {
					found = true;
					r++;
				}
			}
		}
		return r;
	}
}
