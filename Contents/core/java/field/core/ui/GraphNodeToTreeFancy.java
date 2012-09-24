package field.core.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import field.core.Constants;
import field.core.Platform;
import field.launch.Launcher;
import field.math.graph.iMutable;

public class GraphNodeToTreeFancy {

	public static int baseFontHeight(Control install) {
		if (Platform.isMac())
			return install.getFont().getFontData()[0].getHeight() - 1;
		else
			return 8;
	}

	private final Tree target;

	public GraphNodeToTreeFancy(Tree target) {
		this.target = target;
	}

	public void reset(iMutable m) {
		target.removeAll();

		for (Object o : m.getChildren())
			populate((iMutable) o, target);

	}

	private void populate(iMutable m, Widget i) {

		TreeItem item = i instanceof TreeItem ? new TreeItem(((TreeItem) i), 0) : new TreeItem((Tree) i, 0);
		item.setText(("" + m));
		item.setData(m);

		List<iMutable> c = m.getChildren();
		for (iMutable cc : c) {
			populate(cc, item);
		}

	}

	static public class Pretty {

		Pattern SMALLER_PATTERN = Pattern.compile("(<font size=-3 color='#" + Constants.defaultTreeColorDim + "'>)(?=\\S)(.+?)(?<=\\S)(</font>)");
		Pattern BOLD_PATTERN = Pattern.compile("(<b>)(?=\\S)(.+?[*_]*)(?<=\\S)(</b>)");
		Pattern ITALIC_PATTERN = Pattern.compile("(<i>)(.+?)(</i>)");
		Pattern SEP_PATTERN = Pattern.compile("_____________________________");

		private Font boldFont;
		private Font smallerFont;
		private Font italicFont;
		private Font normalFont;

		int indent = 5;
		int vertSpace = Platform.isMac() ? 3 : 3;

		public Pretty(Tree install, final int fixedWidth) {
			String name = install.getFont().getFontData()[0].getName();

			name = Constants.defaultFont;

			smallerFont = new Font(Launcher.display, name, (int) (baseFontHeight(install) * 0.66f), SWT.NORMAL);
			boldFont = new Font(Launcher.display, name, baseFontHeight(install), SWT.BOLD);
			italicFont = new Font(Launcher.display, name, baseFontHeight(install), SWT.ITALIC);
			normalFont = new Font(Launcher.display, name, baseFontHeight(install), SWT.NORMAL);

			install.setBackground(install.getShell().getBackground());

			install.addListener(SWT.MeasureItem, new Listener() {

				@Override
				public void handleEvent(Event event) {
					String textToDraw = ((TreeItem) event.item).getText();
					Point dim = measure(textToDraw, event.gc);
					event.width = Math.max(fixedWidth, dim.x + indent);
					event.height = dim.y + vertSpace * 2;

				}
			});
			install.addListener(SWT.PaintItem, new Listener() {

				@Override
				public void handleEvent(Event event) {
					String textToDraw = ((TreeItem) event.item).getText();
					draw(textToDraw, event.gc, event.x + indent, event.y + vertSpace);
				}
			});
			install.addListener(SWT.EraseItem, new Listener() {

				@Override
				public void handleEvent(Event event) {
					event.detail &= ~SWT.FOREGROUND;
				}
			});
		}

		protected Point measure(String textToDraw, GC gc) {

			if (SEP_PATTERN.matcher(textToDraw).matches())
				return new Point(200, 10);

			List<Area> area = new ArrayList<Area>();

			Matcher m = SMALLER_PATTERN.matcher(textToDraw);
			while (m.find()) {
				area.add(new Area(m.start(), m.end(), m.group(2), smallerFont));
			}

			m = BOLD_PATTERN.matcher(textToDraw);
			while (m.find()) {
				area.add(new Area(m.start(), m.end(), m.group(2), boldFont));
			}

			m = ITALIC_PATTERN.matcher(textToDraw);
			while (m.find()) {
				area.add(new Area(m.start(), m.end(), m.group(2), italicFont));
			}

			Collections.sort(area, new Comparator<Area>() {
				@Override
				public int compare(Area o1, Area o2) {
					return Float.compare(o1.start, o2.start);
				}
			});

			int cx = 0;

			int index = 0;
			int areaIndex = 0;
			while (index < textToDraw.length()) {
				if (areaIndex < area.size() && index >= area.get(areaIndex).start) {
					Area a = area.get(areaIndex);

					gc.setFont(a.font);
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

			return new Point(cx, gc.textExtent(textToDraw).y - (Platform.isLinux() ? 3 : 0));
		}

		public class Area {
			int start;
			int end;
			String text;
			Font font;

			public Area(int start, int end, String text, Font font) {
				super();
				this.start = start;
				this.end = end;
				this.text = text;
				this.font = font;
			}

		}

		protected void draw(String textToDraw, GC gc, int x, int y) {

			if (SEP_PATTERN.matcher(textToDraw).matches()) {
				int height = 10;

				gc.setForeground(Launcher.display.getSystemColor(SWT.COLOR_GRAY));
				gc.setBackground(Launcher.display.getSystemColor(SWT.COLOR_GRAY));
				gc.drawLine(x, y + height / 2, x + 200, y + height / 2);
				return;
			}

			List<Area> area = new ArrayList<Area>();

			Matcher m = SMALLER_PATTERN.matcher(textToDraw);
			while (m.find()) {
				area.add(new Area(m.start(), m.end(), m.group(2), smallerFont));
			}

			m = BOLD_PATTERN.matcher(textToDraw);
			while (m.find()) {
				area.add(new Area(m.start(), m.end(), m.group(2), boldFont));
			}

			m = ITALIC_PATTERN.matcher(textToDraw);
			while (m.find()) {
				area.add(new Area(m.start(), m.end(), m.group(2), italicFont));
			}

			Collections.sort(area, new Comparator<Area>() {
				@Override
				public int compare(Area o1, Area o2) {
					return Float.compare(o1.start, o2.start);
				}
			});

			int cx = 0;

			gc.setFont(normalFont);
			int dasc = gc.getFontMetrics().getAscent();

			int index = 0;
			int areaIndex = 0;
			while (index < textToDraw.length()) {
				if (areaIndex < area.size() && index >= area.get(areaIndex).start) {
					Area a = area.get(areaIndex);

					gc.setFont(a.font);
					int asc = gc.getFontMetrics().getAscent();
					gc.drawText(a.text, cx + x, y + dasc - asc, true);
					cx += gc.textExtent(a.text).x;

					areaIndex++;
					index = a.end;
				} else {
					int start = index;
					int end = areaIndex < area.size() ? area.get(areaIndex).start : textToDraw.length();

					gc.setFont(normalFont);
					gc.drawText(textToDraw.substring(start, end), cx + x, y);

					cx += gc.textExtent(textToDraw.substring(start, end)).x;

					index = end;
				}
			}
		}

	}

}
