package field.core.plugins.drawing.text;

import java.awt.Font;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.plugins.drawing.text.AdvancedTextToCachedLineCache.CacheRecord;


public class AdvancedTextToCachedLine {

	static public final AdvancedTextToCachedLineCache cache = new AdvancedTextToCachedLineCache();

	public float alignment = 0;

	public float justification= 0;

	private final AdvancedText at;

	private final int fontSize;

	private final float fontScale;

	private final String fontName;

	private CachedLine lastCachedLine;

	Rect upperRect = new Rect(0, 0, 0, 0);

	Rect fullRect = new Rect(0, 0, 0, 0);

	public AdvancedTextToCachedLine(String fontName, int fontSize) {
		this.fontName = fontName;
		this.fontSize = fontSize;
		at = new AdvancedText();
		at.setFont(fontName, 200);

		fontScale = 200f / fontSize;

	}
	public AdvancedTextToCachedLine(Font font) {
		this.fontName = font.getName();
		this.fontSize = font.getSize();
		at = new AdvancedText();
		at.setFont(fontName, fontSize);
		fontScale = 1f;
	}

	public Rect getBottomBounds() {
		GeneralPath p = new LineUtils().lineToGeneralPath(lastCachedLine, true);
		float f = 0.2f;
		Area a = new Area(p);
		Rectangle2D.Double def = new Rectangle2D.Double(upperRect.x, upperRect.y + upperRect.h * (1 - f), upperRect.w, upperRect.h * f);
		a.intersect(new Area(def));

		Rectangle2D bounds = a.getBounds2D();
		if (a.isEmpty()) {
			bounds = def;
		}

		return new Rect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
	}


	public Rect getDescenderBounds() {
		GeneralPath p = new LineUtils().lineToGeneralPath(lastCachedLine, true);
		float f = 0.1f;
		Area a = new Area(p);
		Rectangle2D.Double def = new Rectangle2D.Double(fullRect.x, upperRect.y + upperRect.h * (1 - f), upperRect.w, fullRect.y + fullRect.h - (upperRect.y + upperRect.h * (1 - f)));
		a.intersect(new Area(def));
		Rectangle2D bounds = a.getBounds2D();
		if (a.isEmpty()) {
			bounds = def;
		}
		return new Rect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());

	}

	public float getFontSize() {
		return fontScale;
	}

	public Rect getFullRect() {
		return fullRect;
	}

	public Rect getLastRect() {
		return upperRect;
	}

	public CachedLine getLine(String text, final float ox, final float oy) {
		return getLine(text, ox, oy, 0);
	}

	public CachedLine getLine(String text, final float ox, final float oy, float wrap) {
		if (wrap == 0) {
			CacheRecord found = cache.find(fontName, text, fontSize);
			if (found != null) { return lastCachedLine = cache.convert(this, ox, oy, found); }
		}
		if (wrap<0) wrap = 0;
		CachedLine cl = new CachedLine();

		final iLine line = cl.getInput();

		fullRect = null;
		upperRect = null;

		aNativeTextLayoutVisitor vv = new aNativeTextLayoutVisitor(){

			@Override
			public void visitBegin(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float oxy) {

				oxy/=fontScale;

				x1 /= fontScale;
				x2 /= fontScale;
				y1 /= fontScale;
				y2 /= fontScale;
				x3 /= fontScale;
				x4 /= fontScale;
				y3 /= fontScale;
				y4 /= fontScale;

				float minx = Math.min(Math.min(Math.min(x1, x2), x3), x4);
				float miny = Math.min(Math.min(Math.min(y1, y2), y3), y4)+oxy;
				float maxx = Math.max(Math.max(Math.max(x1, x2), x3), x4);
				float maxy = Math.max(Math.max(Math.max(y1, y2), y3), y4)+oxy;

				Rect fullRect = new Rect(0,0,0,0);
				Rect upperRect= new Rect(0,0,0,0);

				fullRect.x = ox + minx;
				fullRect.y = oy + miny;
				fullRect.w = maxx - minx;
				fullRect.h = maxy - miny;

				AdvancedTextToCachedLine.this.fullRect =  fullRect.union(AdvancedTextToCachedLine.this.fullRect);

				maxy = Math.min(oxy, maxy);

				upperRect.x = ox + minx;
				upperRect.y = oy + miny;
				upperRect.w = maxx - minx;
				upperRect.h = maxy - miny;
				AdvancedTextToCachedLine.this.upperRect =  upperRect.union(AdvancedTextToCachedLine.this.upperRect);
			}

			@Override
			public void visitPathClose() {
				line.close();
			}

			@Override
			public void visitPathCubicCurveTo(float x, float y, float x1, float y1, float x2, float y2) {
				x /= fontScale;
				y /= fontScale;
				x1 /= fontScale;
				y1 /= fontScale;
				x2 /= fontScale;
				y2 /= fontScale;
				line.cubicTo(ox + x1, oy + y1, ox + x2, oy + y2, ox + x, oy + y);
			}

			@Override
			public void visitPathLineTo(float x, float y) {
				x /= fontScale;
				y /= fontScale;
				line.lineTo(x + ox, y + oy);
			}

			@Override
			public void visitPathMoveTo(float x, float y) {
				x /= fontScale;
				y /= fontScale;
				line.moveTo(ox + x, oy + y);
			}
		};
		if (wrap==0)
			at.acceptString(text, vv);
		else
			at.acceptStringWrap(text, vv, wrap*200f/fontSize, alignment, justification);

		lastCachedLine = cl;

		cl.getProperties().put(iLinearGraphicsContext.noHit, true);

		markWithMoveLock(cl);

		cl.getProperties().put(iLinearGraphicsContext.isText_info, true);

		if (wrap==0)
			cache.record(cl, fontName, text, fontSize, ox, oy, upperRect, fullRect);




		return cl;
	}

	public Rect getMiddleBounds() {
		GeneralPath p = new LineUtils().lineToGeneralPath(lastCachedLine, true);
		float f1 = 0.45f;
		float f2 = 0.1f;
		Area a = new Area(p);
		Rectangle2D.Double def = new Rectangle2D.Double(upperRect.x, upperRect.y + upperRect.h * f1, upperRect.w, upperRect.h * f2);
		a.intersect(new Area(def));
		Rectangle2D bounds = a.getBounds2D();
		if (a.isEmpty()) {
			bounds = def;
		}
		return new Rect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
	}

	public Rect getTopBounds() {
		GeneralPath p = new LineUtils().lineToGeneralPath(lastCachedLine, true);
		float f = 0.2f;
		Area a = new Area(p);
		Rectangle2D.Double def = new Rectangle2D.Double(upperRect.x, upperRect.y, upperRect.w, upperRect.h * f);
		a.intersect(new Area(def));
		Rectangle2D bounds = a.getBounds2D();
		if (a.isEmpty()) {
			bounds = def;
		}
		return new Rect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
	}

	private void markWithMoveLock(CachedLine cl) {
		int a = -Math.abs(System.identityHashCode(cl));
		for(Event e : cl.events)
		{
			e.getAttributes().put(iLinearGraphicsContext.defaultMoveLock, a);
		}
	}
}
