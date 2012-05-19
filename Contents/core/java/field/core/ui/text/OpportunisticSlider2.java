package field.core.ui.text;

import java.math.BigDecimal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import field.core.ui.text.rulers.ExecutedAreas.Area;
import field.launch.Launcher;
import field.util.HashQueue;
import field.util.HashQueue.Task;

public class OpportunisticSlider2 {

	private BaseTextEditor2 inside;

	public OpportunisticSlider2(BaseTextEditor2 b) {
		this.inside = b;
		Launcher.getLauncher().registerUpdateable(deferedUpdate);

		inside.rulerCanvas.addListener(SWT.MouseVerticalWheel, new Listener() {

			@Override
			public void handleEvent(Event arg0) {

				String text = inside.ed.getSelectionText();

				System.out.println(" selected is <" + text + ">");

				if (isInteger(text) || isFloat(text)) {
					arg0.doit = false;
					scroll(arg0);
				} else {
					arg0.doit = true;
				}
				// arg0.doit = false;
			}
		});

		inside.rulerCanvas.addPaintListener(new PaintListener() {

			@Override
			public void paintControl(PaintEvent arg0) {

				System.out.println("paint event :" + arg0);

				String text = inside.ed.getSelectionText();
				if (isInteger(text) || isFloat(text)) {

					Area best = inside.executionRuler.getMin(inside.ed.getSelection().x);
					if (best != null) {
						Rectangle a = inside.executionRuler.rectangleForPosition(inside.ed.getSelectionRanges()[0]);
						// arg0.gc.setBackground(Launcher.getLauncher().display.getSystemColor(SWT.COLOR_RED));
						// arg0.gc.fillRectangle(0, a.y,
						// 10,
						// a.height);
						float cy = a.y + a.height / 2;

						arg0.gc.setForeground(new Color(Launcher.getLauncher().display, 0, 0, 0));
						arg0.gc.setAlpha(128);
						arg0.gc.setLineDash(new int[] { 2, 2 });
						arg0.gc.drawLine((int) arg0.width, (int) cy, (int) (arg0.width - 15), (int) cy);
						arg0.gc.setLineDash(null);
						arg0.gc.setLineWidth(1);

						{
							arg0.gc.setForeground(new Color(Launcher.getLauncher().display, 0, 0, 0));
							arg0.gc.setAlpha(128);

							Path bracket = new Path(arg0.gc.getDevice());

							float x = arg0.width - 15;

							bracket.moveTo(x, cy);
							bracket.cubicTo(x - 13, cy, x - 10, cy - 20, x - 10, cy - 20);

							float s = 3;
							bracket.moveTo(x - 10 - s, cy - 20 + s);
							bracket.lineTo(x - 10, cy - 20);
							bracket.lineTo(x - 10 + s, cy - 20 + s);

							arg0.gc.drawPath(bracket);
						}
						{
							arg0.gc.setForeground(new Color(Launcher.getLauncher().display, 0, 0, 0));
							arg0.gc.setAlpha(128);

							Path bracket = new Path(arg0.gc.getDevice());

							float x = arg0.width - 15;

							bracket.moveTo(x, cy);
							bracket.cubicTo(x - 13, cy, x - 10, cy + 20, x - 10, cy + 20);

							float s = 3;
							bracket.moveTo(x - 10 - s, cy + 20 - s);
							bracket.lineTo(x - 10, cy + 20);
							bracket.lineTo(x - 10 + s, cy + 20 - s);

							arg0.gc.drawPath(bracket);
						}

//						Area best = inside.executionRuler.getCurrentArea();

						
//						Rectangle rstart = inside.executionRuler.rectangleForLine(best.lineStart);
//						Rectangle rend = inside.executionRuler.rectangleForLine(best.lineEnd);
//						
//						float midy = (rstart.y + rend.y + rend.height)/2;
//						float midx = best.allocation*10+2;
//
//						arg0.gc.setLineDash(new int[] { 2, 2 });
//						arg0.gc.drawLine((int)(arg0.width-15), (int)cy, (int)midx, (int)midy);
						
						
					}

				}
			}
		});

	}

	protected boolean isInteger(String text) {

		try {
			Integer.parseInt(text);
			return true;
		} catch (NumberFormatException e) {

		}
		return false;

	}

	protected boolean isFloat(String text) {
		try {
			Float.parseFloat(text);
			return true;
		} catch (NumberFormatException e) {

		}
		return false;
	}

	public void scroll(Event arg0) {
		scroll(arg0.count, (arg0.stateMask & SWT.ALT) != 0);
	}
	HashQueue deferedUpdate = new HashQueue();

	public void scroll(int arg0, boolean exec) {

		String text = inside.ed.getSelectionText();

		if (isInteger(text)) {

			int n = Integer.parseInt(text);
			n += arg0;

			int start = inside.ed.getSelectionRanges()[0];
			System.out.println(" replace " + start + " " + inside.ed.getSelectionRanges()[1]);
			inside.ed.replaceTextRange(start, inside.ed.getSelectionRanges()[1], "" + n);
			inside.ed.setSelectionRange(start, ("" + n).length());

			if (exec) {
				deferedUpdate.new Task("q") {
					int t = 0;

					@Override
					public void run() {
						t++;
						if (t < 4)
							recur();

						if (t == 4) {
							
							Area best = inside.executionRuler.getCurrentArea();
//							Area best = inside.executionRuler.getMin(inside.ed.getSelection().x);
							if (best != null)
								inside.executionRuler.executeArea(best);
						}

					}
				};
			}

		} else if (isFloat(text)) {

			BigDecimal u = new BigDecimal(text).ulp();
			u = u.multiply(new BigDecimal(arg0));
			u = new BigDecimal(text).add(u);
			u.toString();

			int start = inside.ed.getSelectionRanges()[0];
			System.out.println(" replace " + start + " " + inside.ed.getSelectionRanges()[1]);
			inside.ed.replaceTextRange(start, inside.ed.getSelectionRanges()[1], "" + u.toPlainString());
			inside.ed.setSelectionRange(start, ("" + u.toPlainString()).length());

			if (exec) {
				deferedUpdate.new Task("q") {
					int t = 0;

					@Override
					public void run() {
						t++;
						if (t < 4)
							recur();

						if (t == 4) {
//							Area best = inside.executionRuler.getMin(inside.ed.getSelection().x);
							Area best = inside.executionRuler.getCurrentArea();
							if (best != null)
								inside.executionRuler.executeArea(best);
						}

					}
				};
			}

		}
	}
}
