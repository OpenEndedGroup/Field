package field.core.ui.text.embedded;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.TimingStatistics;
import field.core.dispatch.iVisualElement.Rect;

@Woven
public class TripleQuote {

	public TripleQuote() {
	}

	public void draw(JEditorPane ed, Graphics2D g) {

		try {
			scanAndDraw(ed, g, "\"\"\"");
			scanAndDraw(ed, g, "'''");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void scanAndDraw(JEditorPane ed, Graphics2D g, String ss) throws BadLocationException {
		Document d = ed.getDocument();
		String t = d.getText(0, d.getLength());

		int ii = t.indexOf(ss);
		if (ii == -1)
			return;

		boolean on = true;
		while (ii != -1) {
			int ne = t.indexOf(ss, ii + 1);
			if (ne == -1)
				return;

			if (on) {
				drawHighlight(ed, g, ii, ne);
				on = false;
			} else
				on = true;
			ii = ne + 1;
			if (ii > t.length() - 1)
				return;
		}
	}

	@TimingStatistics
	protected void drawHighlight(JEditorPane ed, Graphics2D g, int start, int stop) throws BadLocationException {

		Rectangle left = ed.modelToView(start);
		Rectangle right = ed.modelToView(stop + 3);

		if (left.y == right.y) {
			Rectangle union = left.union(right);
			Shape m = new Rectangle2D.Double(union.x, union.y, union.width, union.height);
			drawHighlightRect(m, g);

		} else {
			left.width = ed.getWidth() - left.x;

			Rect rr = new Rect(left.x, left.y, left.width, left.height);

			Area m = new Area(new Rectangle2D.Double(left.x, left.y, left.width, left.height));
			// drawHighlightRect(left, g);
			left.y += left.height;
			while (left.y < right.y) {
				left.x = 0;
				left.width = ed.getWidth();
				Shape m2 = new Rectangle2D.Double(left.x, left.y, left.width, left.height);
				((Area) m).add(new Area(m2));

				// rr = rr.union(new Rect(left.x, left.y,
				// left.width, left.height));

				// drawHighlightRect(left, g);
				left.y += left.height;
			}
			left.y = right.y;
			left.height = right.height;
			left.x = 0;
			left.width = 0;
			left.add(right);
			Shape m2 = new Rectangle2D.Double(left.x, left.y, left.width, left.height);
			((Area) m).add(new Area(m2));

			// rr = rr.union(new Rect(left.x, left.y,
			// left.width, left.height));

			// Shape m2 = new Rectangle2D.Double(rr.x, rr.y, rr.w,
			// rr.h);

			drawHighlightRect(m, g);
		}
	}

	private void drawHighlightRect(Shape m, Graphics2D g) {
		g.setColor(new Color(0, 0, 0, 0.1f));
		g.fill(m);
		g.draw(m);
	}

}
