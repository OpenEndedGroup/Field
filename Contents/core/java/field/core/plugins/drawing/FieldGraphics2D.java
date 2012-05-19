package field.core.plugins.drawing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.RenderingHints.Key;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.math.linalg.Vector4;
import field.util.Dict;

public class FieldGraphics2D extends Graphics2D {

	List<CachedLine> geometry = new ArrayList<CachedLine>();

	@Override
	public void addRenderingHints(Map<?, ?> hints) {
	}

	@Override
	public void clip(Shape s) {
	}

	@Override
	public void draw(Shape s) {

		float w = 1;
		if (stroke != null) {
			if (stroke instanceof BasicStroke) {
				w = ((BasicStroke) stroke).getLineWidth();
			}
		}

		CachedLine c = new LineUtils().piToCachedLine(s.getPathIterator(null));

		Dict p = c.getProperties();
		p.put(iLinearGraphicsContext.stroked, true);
		if (color != null)
			p.put(iLinearGraphicsContext.strokeColor, new Vector4(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f));
		p.put(iLinearGraphicsContext.thickness, w);

		geometry.add(c);
	}

	@Override
	public void drawGlyphVector(GlyphVector g, float x, float y) {

		Shape o = g.getOutline(x, y);

		fill(o);

		// throw new NotImplementedException();
	}

	@Override
	public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
		return false;
	}

	@Override
	public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
	}

	@Override
	public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
	}

	@Override
	public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
	}

	@Override
	public void drawString(String str, int x, int y) {
		drawString(str, (float) x, (float) y);
	}

	@Override
	public void drawString(String str, float x, float y) {
	}

	@Override
	public void drawString(AttributedCharacterIterator iterator, int x, int y) {
		drawString(iterator, (float) x, (float) y);
	}

	@Override
	public void drawString(AttributedCharacterIterator iterator, float x, float y) {
	}

	@Override
	public void fill(Shape s) {

		CachedLine c = new LineUtils().piToCachedLine(s.getPathIterator(null));

		Dict p = c.getProperties();
		p.put(iLinearGraphicsContext.stroked, false);
		p.put(iLinearGraphicsContext.filled, true);
		if (color != null)
			p.put(iLinearGraphicsContext.fillColor, new Vector4(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f));

		geometry.add(c);

	}

	@Override
	public Color getBackground() {
		return null;
	}

	@Override
	public Composite getComposite() {
		return null;
	}

	@Override
	public GraphicsConfiguration getDeviceConfiguration() {
		return null;
	}

	@Override
	public FontRenderContext getFontRenderContext() {
		return null;
	}

	Paint paint;
	private Stroke stroke;

	@Override
	public Paint getPaint() {
		return paint;
	}

	@Override
	public Object getRenderingHint(Key hintKey) {
		return null;
	}

	@Override
	public RenderingHints getRenderingHints() {
		return null;
	}

	@Override
	public Stroke getStroke() {
		return stroke;
	}

	AffineTransform transform = new AffineTransform();

	@Override
	public AffineTransform getTransform() {
		return transform;
	}

	@Override
	public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
		return false;
	}

	@Override
	public void rotate(double theta) {
		transform.rotate(theta);
	}

	@Override
	public void rotate(double theta, double x, double y) {
		transform.rotate(theta, x, y);
	}

	@Override
	public void scale(double sx, double sy) {
		transform.scale(sx, sy);
	}

	@Override
	public void setBackground(Color color) {
	}

	@Override
	public void setComposite(Composite comp) {
	}

	@Override
	public void setPaint(Paint paint) {
		this.paint = paint;
	}

	@Override
	public void setRenderingHint(Key hintKey, Object hintValue) {
	}

	@Override
	public void setRenderingHints(Map<?, ?> hints) {
	}

	@Override
	public void setStroke(Stroke s) {
		this.stroke = s;
	}

	@Override
	public void setTransform(AffineTransform Tx) {
		transform = Tx;
	}

	@Override
	public void shear(double shx, double shy) {
		transform.shear(shx, shy);
	}

	@Override
	public void transform(AffineTransform Tx) {
		transform.concatenate(Tx);
	}

	@Override
	public void translate(int x, int y) {
		transform.translate(x, y);
	}

	@Override
	public void translate(double tx, double ty) {
		transform.translate(tx, ty);
	}

	@Override
	public void clearRect(int x, int y, int width, int height) {
		Paint paint = getPaint();
		setColor(getBackground());
		fillRect(x, y, width, height);
		setPaint(paint);
	}

	@Override
	public void clipRect(int x, int y, int width, int height) {
		clip(new Rectangle(x, y, width, height));
	}

	@Override
	public void copyArea(int x, int y, int width, int height, int dx, int dy) {
	}

	@Override
	public Graphics create() {
		FieldGraphics2D m = new FieldGraphics2D();
		m.transform = (AffineTransform) transform.clone();
		return m;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		Arc2D arc = new Arc2D.Float(x, y, width, height, startAngle, arcAngle, Arc2D.OPEN);
		draw(arc);
	}

	@Override
	public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
		throw new NotImplementedException();
	}

	@Override
	public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
		return drawImage(img, x, y, img.getWidth(null), img.getHeight(null), bgcolor, observer);
	}

	@Override
	public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
		throw new NotImplementedException();
	}

	@Override
	public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
		Paint paint = getPaint();
		setPaint(bgcolor);
		fillRect(x, y, width, height);
		setPaint(paint);
		drawImage(img, x, y, width, height, observer);
		return true;
	}

	@Override
	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
		BufferedImage src = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = src.createGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();

		src = src.getSubimage(sx1, sy1, sx2 - sx1, sy2 - sy1);

		return drawImage(src, dx1, dy1, dx2 - dx1, dy2 - dy1, observer);
	}

	@Override
	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
		Paint paint = getPaint();
		setPaint(bgcolor);
		fillRect(dx1, dy1, dx2 - dx1, dy2 - dy1);
		setPaint(paint);
		return drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {
		Line2D line = new Line2D.Float(x1, y1, x2, y2);
		draw(line);
	}

	@Override
	public void drawOval(int x, int y, int width, int height) {
		throw new NotImplementedException();
	}

	@Override
	public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
		Polygon polygon = new Polygon(xPoints, yPoints, nPoints);
		draw(polygon);
	}

	@Override
	public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
		if (nPoints > 0) {
			GeneralPath path = new GeneralPath();
			path.moveTo(xPoints[0], yPoints[0]);
			for (int i = 1; i < nPoints; i++)
				path.lineTo(xPoints[i], yPoints[i]);

			draw(path);
		}
	}

	@Override
	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		RoundRectangle2D rect = new RoundRectangle2D.Float(x, y, width, height, arcWidth, arcHeight);
		draw(rect);
	}

	@Override
	public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		Arc2D arc = new Arc2D.Float(x, y, width, height, startAngle, arcAngle, Arc2D.PIE);
		fill(arc);
	}

	@Override
	public void fillOval(int x, int y, int width, int height) {
		throw new NotImplementedException();
	}

	@Override
	public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
		Polygon polygon = new Polygon(xPoints, yPoints, nPoints);
		fill(polygon);
	}

	@Override
	public void fillRect(int x, int y, int width, int height) {
		Rectangle rect = new Rectangle(x, y, width, height);
		fill(rect);
	}

	@Override
	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		RoundRectangle2D rect = new RoundRectangle2D.Float(x, y, width, height, arcWidth, arcHeight);
		fill(rect);
	}

	@Override
	public Shape getClip() {
		try {
			return transform.createInverse().createTransformedShape(clip);
		} catch (NoninvertibleTransformException e) {
			return null;
		}
	}

	  protected Shape clip = null;

	@Override
	public Rectangle getClipBounds() {
		Shape c = getClip();
		if (c == null)
			return null;
		else
			return c.getBounds();
	}

	Color color = null;

	@Override
	public Color getColor() {
		return color;
	}

	@Override
	public Font getFont() {
		return null;
	}

	@Override
	public FontMetrics getFontMetrics(Font f) {
		return null;
	}

	@Override
	public void setClip(Shape clip) {
	}

	@Override
	public void setClip(int x, int y, int width, int height) {
	}

	@Override
	public void setColor(Color c) {
		this.color = c;
	}

	@Override
	public void setFont(Font font) {
	}

	@Override
	public void setPaintMode() {
	}

	@Override
	public void setXORMode(Color c1) {
	}

	public Collection getGeometry() {
		return geometry;
	}

}
