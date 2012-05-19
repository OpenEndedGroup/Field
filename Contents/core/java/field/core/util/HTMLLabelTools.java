package field.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableItem;

import field.core.Constants;
import field.core.Platform;
import field.core.ui.GraphNodeToTreeFancy;
import field.core.ui.SmallMenu.Pretty.Area;
import field.launch.Launcher;

public class HTMLLabelTools {
	Pattern SMALLER_PATTERN = Pattern.compile("(<font size=-3 color='#" + Constants.defaultTreeColorDim + "'>)(.*?)(</font>)");
	Pattern SMALLER2_PATTERN = Pattern.compile("(<font size=-2>)(.*?)(</font>)");

	Pattern BOLDITALIC_PATTERN = Pattern.compile("(<bi>)(?=\\S)(.+?[*_]*)(?<=\\S)(</bi>)");
	Pattern BOLD_PATTERN = Pattern.compile("(<b>)(.*?)(</b>)");
	Pattern ITALIC_PATTERN = Pattern.compile("(<i>)(?=\\S)(.*?)(?<=\\S)(</i>)");
	Pattern SEP_PATTERN = Pattern.compile("_____________________________");

	Pattern CLEAN_PATTERN = Pattern.compile("(<.*?>)");
	private Font boldFont;
	private Font boldItalicFont;
	private Font smallerFont;
	private Font italicFont;
	private Font normalFont;

	int indent = 0;
	int vertSpace = 10;

	public static int baseFontHeight() {
		if (Platform.isMac())
			return 12;
		else
			return 8;
	}
	
	public HTMLLabelTools() {

		String name = Constants.defaultFont;
		
		smallerFont = new Font(Launcher.display, name, (int) (baseFontHeight() * 0.75f), SWT.NORMAL);
		boldItalicFont = new Font(Launcher.display, name, baseFontHeight(), SWT.BOLD | SWT.ITALIC);
		boldFont = new Font(Launcher.display, name, baseFontHeight(), SWT.BOLD);
		italicFont = new Font(Launcher.display, name, baseFontHeight(), SWT.ITALIC);
		normalFont = new Font(Launcher.display, name, baseFontHeight(), SWT.NORMAL);

	}

	public Point measure(String textToDraw, GC gc) {

		if (SEP_PATTERN.matcher(textToDraw).matches())
			return new Point(200, 10);

		textToDraw = textToDraw.replace("<b><i>", "<bi>");
		textToDraw = textToDraw.replace("</i></b>", "</bi>");

		List<Area> area = new ArrayList<Area>();

		Matcher m = SMALLER_PATTERN.matcher(textToDraw);
		while (m.find()) {
			area.add(new Area(m.start(), m.end(), m.group(2), smallerFont));
		}
		m = SMALLER2_PATTERN.matcher(textToDraw);
		while (m.find()) {
			area.add(new Area(m.start(), m.end(), m.group(2), smallerFont));
		}
		m = BOLDITALIC_PATTERN.matcher(textToDraw);
		while (m.find()) {
			area.add(new Area(m.start(), m.end(), m.group(2), boldItalicFont));
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
		gc.setFont(normalFont);
		int cx = 0;

		int index = 0;
		int areaIndex = 0;
		while (index < textToDraw.length()) {
			if (areaIndex < area.size() && index >= area.get(areaIndex).start) {
				Area a = area.get(areaIndex);

				gc.setFont(a.font);
				a.text = a.text.replaceAll(CLEAN_PATTERN.pattern(), "");

				cx += gc.textExtent(a.text).x;

				areaIndex++;
				index = a.end;
			} else {
				int start = index;
				int end = areaIndex < area.size() ? area.get(areaIndex).start : textToDraw.length();

				gc.setFont(normalFont);
				
				String t = textToDraw.substring(start, end);
				t =t.replaceAll(CLEAN_PATTERN.pattern(), "");
				cx += gc.textExtent(t).x;

				index = end;
			}
		}

		gc.setFont(normalFont);

		return new Point(cx, gc.textExtent(textToDraw).y);
	}

	public class Area {
		int start;
		int end;
		String text;
		Font font;

		boolean blank = false;

		public Area(int start, int end, String text, Font font) {
			super();
			this.start = start;
			this.end = end;
			this.text = text;
			this.font = font;
		}

		public Area setBlank(boolean blank) {
			this.blank = blank;
			return this;
		}

	}

	public void draw(String textToDraw, GC gc, int x, int y) {

		if (SEP_PATTERN.matcher(textToDraw).matches()) {
			int height = 10;

			gc.setForeground(Launcher.display.getSystemColor(SWT.COLOR_GRAY));
			gc.setBackground(Launcher.display.getSystemColor(SWT.COLOR_GRAY));
			gc.drawLine(x, y + height / 2, x + 200, y + height / 2);
			return;
		}

		if (textToDraw.contains("<grey>")) {
			textToDraw = textToDraw.replace("<grey>", "");
			gc.setAlpha(128);
		}

		List<Area> area = new ArrayList<Area>();

		textToDraw = textToDraw.replace("<b><i>", "<bi>");
		textToDraw = textToDraw.replace("</i></b>", "</bi>");

		System.out.println(" looking at <" + textToDraw + ">");
		Matcher m = SMALLER_PATTERN.matcher(textToDraw);
		while (m.find()) {
			System.out.println(" found smaller <" + m.start() + " ->" + m.end() + ">");
			area.add(new Area(m.start(), m.end(), m.group(2), smallerFont));
		}
		m = SMALLER2_PATTERN.matcher(textToDraw);
		while (m.find()) {
			area.add(new Area(m.start(), m.end(), m.group(2), smallerFont));
		}

		m = BOLDITALIC_PATTERN.matcher(textToDraw);
		while (m.find()) {
			area.add(new Area(m.start(), m.end(), m.group(2), boldItalicFont));
		}

		System.out.println(" looking for bold <" + BOLD_PATTERN + "> inside <" + textToDraw + ">");

		m = BOLD_PATTERN.matcher(textToDraw);
		while (m.find()) {
			System.out.println(" found bold <" + m.start() + " ->" + m.end() + ">");
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
		gc.setFont(normalFont);

		int cx = 0;

		int dasc = gc.getFontMetrics().getAscent();

		int index = 0;
		int areaIndex = 0;
		while (index < textToDraw.length()) {
			if (areaIndex < area.size() && index >= area.get(areaIndex).start) {
				Area a = area.get(areaIndex);

				gc.setFont(a.font);
				int asc = gc.getFontMetrics().getAscent();

				a.text = a.text.replaceAll(CLEAN_PATTERN.pattern(), "");

				gc.drawText(a.text, cx + x, y + dasc - asc, true);

				System.out.println(" text <" + a.text + ">");

				cx += gc.textExtent(a.text).x;

				areaIndex++;
				index = a.end;
			} else {
				int start = index;
				int end = areaIndex < area.size() ? area.get(areaIndex).start : textToDraw.length();

				gc.setFont(normalFont);
				String ttd = textToDraw.substring(start, end).replaceAll(CLEAN_PATTERN.pattern(), "");
				if (areaIndex >= area.size() || !area.get(areaIndex).blank) {
					gc.drawText(ttd, cx + x, y, true);
				}

				System.out.println(" normal text <" + textToDraw.substring(start, end) + ">");

				cx += gc.textExtent(ttd).x;

				index = end;
			}
		}
		gc.setAlpha(255);
	}

}
