package field.core.ui.text.embedded;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;

import field.core.Constants;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.util.FieldPyObjectAdaptor.iExtensible;
import field.math.linalg.Vector4;
import field.util.Dict;
import field.util.HashMapOfLists;

public class TextAnnotations {

	static public final VisualElementProperty<Package> textEditorAnnotationPackage = new VisualElementProperty<Package>("__textEditorAnnotationPackage");

	static public class Annotation implements iExtensible {
		Vector4 color = new Vector4(0.5, 0.5, 0.6, 0.3);
		public String name = "";

		int savedStart;
		int savedEnd;

		transient Position start = null;
		transient Position end = null;

		public boolean attach(Document d) {

			;//System.out.println(" attaching <" + savedStart + "> <" + savedEnd + "> <" + d.getLength() + ">");

			try {
				start = d.createPosition(savedStart);
				end = d.createPosition(savedEnd);
				return true;
			} catch (BadLocationException e) {
				e.printStackTrace();
				return false;
			}
		}

		public void detach() {
			// savedStart = start.getOffset();
			// savedEnd = end.getOffset();

			start = null;
			end = null;
		}

		public boolean update(Document d) {
			if (start == null)
				return false;
			savedStart = start.getOffset();
			savedEnd = end.getOffset();

			;//System.out.println(" start and end are <" + savedStart + "> <" + savedEnd + ">");

			return savedEnd > savedStart;
		}

		Dict d = new Dict();

		public Dict getDict() {
			return d;
		}

		public boolean draw(JEditorPane ed, Graphics2D g) {

			try {

				if (!update(ed.getDocument())) {
					;//System.out.println(" anotation is stillborn ");

					return true;
				}

				Rectangle left = ed.modelToView(savedStart + 1);
				Rectangle right = ed.modelToView(savedEnd);

				if (left.y == right.y) {
					Rectangle union = left.union(right);
					Shape m = new RoundRectangle2D.Double(union.x, union.y, union.width, union.height, 5, 5);
					drawHighlightRect(m, g);

				} else {
					left.width = ed.getWidth() - left.x;
					Area m = new Area(new RoundRectangle2D.Double(left.x, left.y, left.width, left.height, 5, 5));
					// drawHighlightRect(left, g);
					left.y += left.height;
					while (left.y < right.y) {
						left.x = 0;
						left.width = ed.getWidth();
						Shape m2 = new RoundRectangle2D.Double(left.x, left.y, left.width, left.height, 5, 5);
						((Area) m).add(new Area(m2));

						// drawHighlightRect(left, g);
						left.y += left.height;
					}
					left.y = right.y;
					left.height = right.height;
					left.x = 0;
					left.width = 0;
					left.add(right);
					Shape m2 = new RoundRectangle2D.Double(left.x, left.y, left.width, left.height, 5, 5);
					((Area) m).add(new Area(m2));
					drawHighlightRect(m, g);
				}
				return true;

			} catch (BadLocationException e) {
				e.printStackTrace();
				return false;
			}
		}

		private void drawHighlightRect(Shape m, Graphics2D g) {
			int size = 40;
			for (int n = size; n >= 2; n -= 4) {
				g.setStroke(new BasicStroke(n, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.setColor(new Color(0, 0, 0.25f, 0.05f / n));
				g.draw(m);
			}
			g.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(new Color(0f, 0, 0.25f, 0.1f));
			g.fill(m);

			BasicStroke outside = new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			Area m2 = new Area(outside.createStrokedShape(m));
			m2.add(new Area(m));

			g.draw(m2);
			Rectangle2D b = m2.getBounds2D();

			float y = (float) (b.getMaxY() + 10);
			float x = (float) ((b.getMinX() + b.getMaxX()) / 2);
			g.setFont(new Font(Constants.defaultFont, Font.ITALIC, 20));
			g.setColor(new Color(0.125f, 0.125f, 0.25f, 0.5f));
			g.drawString(name, x, y);

		}

		public void set(int oldStart, int oldEnd, Document d) {
			savedStart = oldStart - 1;
			savedEnd = oldEnd;
			attach(d);
		}
	}

	static public class Package {

		private HashMapOfLists<VisualElementProperty, Annotation> annotations = new HashMapOfLists<VisualElementProperty, Annotation>();

		void setAnnotations(HashMapOfLists<VisualElementProperty, Annotation> annotations) {
			this.annotations = annotations;
		}

		List<Annotation> getAnnotations(VisualElementProperty p) {
			return (List<Annotation>) annotations.getAndMakeCollection(p);
		}

	}

	Package pack = new Package();
	private VisualElementProperty property;

	public TextAnnotations() {

	}

	public void swapIn(iVisualElement e, Document d, VisualElementProperty property) {

		this.property =property;
//		try {
//			;//System.out.println(" annotation swap to <" + e + "> <" + d + "> <" + d.getText(0, d.getLength()) + ">");
//		} catch (BadLocationException e1) {
//			e1.printStackTrace();
//		}

		Package pack = textEditorAnnotationPackage.get(e);
		if (pack == null)
			pack = new Package();

		this.pack = pack;
		LinkedHashSet<Annotation> dead = new LinkedHashSet<Annotation>();
		for (Annotation a : pack.getAnnotations(property)) {
			if (!a.attach(d)) {
				dead.add(a);
			}
		}
		pack.getAnnotations(property).removeAll(dead);
	}

	public void swapOut(iVisualElement e) {
//		;//System.out.println(" setting annotation package to <" + pack.getAnnotations(property) + ">");
		textEditorAnnotationPackage.set(e, e, pack);
		for (Annotation a : pack.getAnnotations(property)) {
			a.detach();
		}
		
		//texteditorannotations disabled for workshop
		textEditorAnnotationPackage.set(e,e, null);
	}

	public void update(Document d) {
		LinkedHashSet<Annotation> dead = new LinkedHashSet<Annotation>();
		for (Annotation a : pack.getAnnotations(property)) {
			if (!a.update(d)) {
				dead.add(a);
			}

		}
		pack.getAnnotations(property).removeAll(dead);
	}

	public void draw(JEditorPane d, Graphics2D g) {
		LinkedHashSet<Annotation> dead = new LinkedHashSet<Annotation>();
		for (Annotation a : pack.getAnnotations(property)) {
			if (!a.draw(d, g)) {
				dead.add(a);
			}
		}
		pack.getAnnotations(property).removeAll(dead);
	}

	public Annotation newAnnotation() {
		Annotation aa = new Annotation();
		pack.getAnnotations(property).add(aa);
		return aa;
	}

	public List<Annotation> isInside(int position) {
		List<Annotation> a = new ArrayList<Annotation>();
		for (Annotation aa : pack.getAnnotations(property)) {
			if (aa.savedStart <= position && aa.savedEnd >= position) {
				a.add(aa);
			}
		}
		return a;
	}

	public void remove(Annotation lll) {
		pack.getAnnotations(property).remove(lll);
	}
	
	public boolean hasAnnotations()
	{
		return pack.getAnnotations(property).size()>0;
	}

	public void removeAll() {
		pack.getAnnotations(property).clear();
	}

}
