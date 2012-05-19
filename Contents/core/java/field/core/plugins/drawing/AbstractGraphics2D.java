package field.core.plugins.drawing;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class AbstractGraphics2D extends Graphics2D implements Cloneable {
  /**
   * Current state of the Graphic Context. The GraphicsContext class manages the
   * state of this <tt>Graphics2D</tt> graphic context attributes.
   */
  protected GraphicContext gc;

  /**
   * Text handling strategy.
   */
  protected boolean textAsShapes = false;

  /**
   * @param textAsShapes
   *          if true, all text is turned into shapes in the convertion. No text
   *          is output.
   * 
   */
  public AbstractGraphics2D(boolean textAsShapes) {
    this.textAsShapes = textAsShapes;
  }

  /**
   * Creates a new AbstractGraphics2D from an existing instance.
   * 
   * @param g
   *          the AbstractGraphics2D whose properties should be copied
   */
  public AbstractGraphics2D(AbstractGraphics2D g) {
    this.gc = (GraphicContext) g.gc.clone();
    this.gc.validateTransformStack();
    this.textAsShapes = g.textAsShapes;
  }

  /**
   * Translates the origin of the graphics context to the point (<i>x</i>,&nbsp;<i>y</i>)
   * in the current coordinate system. Modifies this graphics context so that
   * its new origin corresponds to the point (<i>x</i>,&nbsp;<i>y</i>) in
   * this graphics context's original coordinate system. All coordinates used in
   * subsequent rendering operations on this graphics context will be relative
   * to this new origin.
   * 
   * @param x
   *          the <i>x</i> coordinate.
   * @param y
   *          the <i>y</i> coordinate.
   */
  public void translate(int x, int y) {
    gc.translate(x, y);
  }

  /**
   * Gets this graphics context's current color.
   * 
   * @return this graphics context's current color.
   * @see java.awt.Color
   * @see java.awt.Graphics#setColor
   */
  public Color getColor() {
    return gc.getColor();
  }

  /**
   * Sets this graphics context's current color to the specified color. All
   * subsequent graphics operations using this graphics context use this
   * specified color.
   * 
   * @param c
   *          the new rendering color.
   * @see java.awt.Color
   * @see java.awt.Graphics#getColor
   */
  public void setColor(Color c) {
    gc.setColor(c);
  }

  /**
   * Sets the paint mode of this graphics context to overwrite the destination
   * with this graphics context's current color. This sets the logical pixel
   * operation function to the paint or overwrite mode. All subsequent rendering
   * operations will overwrite the destination with the current color.
   */
  public void setPaintMode() {
    gc.setComposite(AlphaComposite.SrcOver);
  }

  /**
   * Gets the current font.
   * 
   * @return this graphics context's current font.
   * @see java.awt.Font
   * @see java.awt.Graphics#setFont
   */
  public Font getFont() {
    return gc.getFont();
  }

  /**
   * Sets this graphics context's font to the specified font. All subsequent
   * text operations using this graphics context use this font.
   * 
   * @param font
   *          the font.
   * @see java.awt.Graphics#getFont
   */
  public void setFont(Font font) {
    gc.setFont(font);
  }

  /**
   * Returns the bounding rectangle of the current clipping area. This method
   * refers to the user clip, which is independent of the clipping associated
   * with device bounds and window visibility. If no clip has previously been
   * set, or if the clip has been cleared using <code>setClip(null)</code>,
   * this method returns <code>null</code>. The coordinates in the rectangle
   * are relative to the coordinate system origin of this graphics context.
   * 
   * @return the bounding rectangle of the current clipping area, or
   *         <code>null</code> if no clip is set.
   * @see java.awt.Graphics#getClip
   * @see java.awt.Graphics#clipRect
   * @see java.awt.Graphics#setClip(int, int, int, int)
   * @see java.awt.Graphics#setClip(Shape)
   * @since JDK1.1
   */
  public Rectangle getClipBounds() {
    return gc.getClipBounds();
  }

  /**
   * Intersects the current clip with the specified rectangle. The resulting
   * clipping area is the intersection of the current clipping area and the
   * specified rectangle. If there is no current clipping area, either because
   * the clip has never been set, or the clip has been cleared using
   * <code>setClip(null)</code>, the specified rectangle becomes the new
   * clip. This method sets the user clip, which is independent of the clipping
   * associated with device bounds and window visibility. This method can only
   * be used to make the current clip smaller. To set the current clip larger,
   * use any of the setClip methods. Rendering operations have no effect outside
   * of the clipping area.
   * 
   * @param x
   *          the x coordinate of the rectangle to intersect the clip with
   * @param y
   *          the y coordinate of the rectangle to intersect the clip with
   * @param width
   *          the width of the rectangle to intersect the clip with
   * @param height
   *          the height of the rectangle to intersect the clip with
   * @see #setClip(int, int, int, int)
   * @see #setClip(Shape)
   */
  public void clipRect(int x, int y, int width, int height) {
    gc.clipRect(x, y, width, height);
  }

  /**
   * Sets the current clip to the rectangle specified by the given coordinates.
   * This method sets the user clip, which is independent of the clipping
   * associated with device bounds and window visibility. Rendering operations
   * have no effect outside of the clipping area.
   * 
   * @param x
   *          the <i>x</i> coordinate of the new clip rectangle.
   * @param y
   *          the <i>y</i> coordinate of the new clip rectangle.
   * @param width
   *          the width of the new clip rectangle.
   * @param height
   *          the height of the new clip rectangle.
   * @see java.awt.Graphics#clipRect
   * @see java.awt.Graphics#setClip(Shape)
   * @since JDK1.1
   */
  public void setClip(int x, int y, int width, int height) {
    gc.setClip(x, y, width, height);
  }

  /**
   * Gets the current clipping area. This method returns the user clip, which is
   * independent of the clipping associated with device bounds and window
   * visibility. If no clip has previously been set, or if the clip has been
   * cleared using <code>setClip(null)</code>, this method returns
   * <code>null</code>.
   * 
   * @return a <code>Shape</code> object representing the current clipping
   *         area, or <code>null</code> if no clip is set.
   * @see java.awt.Graphics#getClipBounds()
   * @see java.awt.Graphics#clipRect(int, int, int, int)
   * @see java.awt.Graphics#setClip(int, int, int, int)
   * @see java.awt.Graphics#setClip(Shape)
   * @since JDK1.1
   */
  public Shape getClip() {
    return gc.getClip();
  }

  /**
   * Sets the current clipping area to an arbitrary clip shape. Not all objects
   * that implement the <code>Shape</code> interface can be used to set the
   * clip. The only <code>Shape</code> objects that are guaranteed to be
   * supported are <code>Shape</code> objects that are obtained via the
   * <code>getClip</code> method and via <code>Rectangle</code> objects.
   * This method sets the user clip, which is independent of the clipping
   * associated with device bounds and window visibility.
   * 
   * @param clip
   *          the <code>Shape</code> to use to set the clip
   * @see java.awt.Graphics#getClip()
   * @see java.awt.Graphics#clipRect
   * @see java.awt.Graphics#setClip(int, int, int, int)
   * @since JDK1.1
   */
  public void setClip(Shape clip) {
    gc.setClip(clip);
  }

  /**
   * Draws a line, using the current color, between the points
   * <code>(x1,&nbsp;y1)</code> and <code>(x2,&nbsp;y2)</code> in this
   * graphics context's coordinate system.
   * 
   * @param x1
   *          the first point's <i>x</i> coordinate.
   * @param y1
   *          the first point's <i>y</i> coordinate.
   * @param x2
   *          the second point's <i>x</i> coordinate.
   * @param y2
   *          the second point's <i>y</i> coordinate.
   */
  public void drawLine(int x1, int y1, int x2, int y2) {
    Line2D line = new Line2D.Float(x1, y1, x2, y2);
    draw(line);
  }

  /**
   * Fills the specified rectangle. The left and right edges of the rectangle
   * are at <code>x</code> and <code>x&nbsp;+&nbsp;width&nbsp;-&nbsp;1</code>.
   * The top and bottom edges are at <code>y</code> and
   * <code>y&nbsp;+&nbsp;height&nbsp;-&nbsp;1</code>. The resulting rectangle
   * covers an area <code>width</code> pixels wide by <code>height</code>
   * pixels tall. The rectangle is filled using the graphics context's current
   * color.
   * 
   * @param x
   *          the <i>x</i> coordinate of the rectangle to be filled.
   * @param y
   *          the <i>y</i> coordinate of the rectangle to be filled.
   * @param width
   *          the width of the rectangle to be filled.
   * @param height
   *          the height of the rectangle to be filled.
   * @see java.awt.Graphics#clearRect
   * @see java.awt.Graphics#drawRect
   */
  public void fillRect(int x, int y, int width, int height) {
    Rectangle rect = new Rectangle(x, y, width, height);
    fill(rect);
  }

  public void drawRect(int x, int y, int width, int height) {
    Rectangle rect = new Rectangle(x, y, width, height);
    draw(rect);
  }

  /**
   * Clears the specified rectangle by filling it with the background color of
   * the current drawing surface. This operation does not use the current paint
   * mode.
   * <p>
   * Beginning with Java&nbsp;1.1, the background color of offscreen images may
   * be system dependent. Applications should use <code>setColor</code>
   * followed by <code>fillRect</code> to ensure that an offscreen image is
   * cleared to a specific color.
   * 
   * @param x
   *          the <i>x</i> coordinate of the rectangle to clear.
   * @param y
   *          the <i>y</i> coordinate of the rectangle to clear.
   * @param width
   *          the width of the rectangle to clear.
   * @param height
   *          the height of the rectangle to clear.
   * @see java.awt.Graphics#fillRect(int, int, int, int)
   * @see java.awt.Graphics#drawRect
   * @see java.awt.Graphics#setColor(java.awt.Color)
   * @see java.awt.Graphics#setPaintMode
   * @see java.awt.Graphics#setXORMode(java.awt.Color)
   */
  public void clearRect(int x, int y, int width, int height) {
    Paint paint = gc.getPaint();
    gc.setColor(gc.getBackground());
    fillRect(x, y, width, height);
    gc.setPaint(paint);
  }

  /**
   * Draws an outlined round-cornered rectangle using this graphics context's
   * current color. The left and right edges of the rectangle are at
   * <code>x</code> and <code>x&nbsp;+&nbsp;width</code>, respectively. The
   * top and bottom edges of the rectangle are at <code>y</code> and
   * <code>y&nbsp;+&nbsp;height</code>.
   * 
   * @param x
   *          the <i>x</i> coordinate of the rectangle to be drawn.
   * @param y
   *          the <i>y</i> coordinate of the rectangle to be drawn.
   * @param width
   *          the width of the rectangle to be drawn.
   * @param height
   *          the height of the rectangle to be drawn.
   * @param arcWidth
   *          the horizontal diameter of the arc at the four corners.
   * @param arcHeight
   *          the vertical diameter of the arc at the four corners.
   * @see java.awt.Graphics#fillRoundRect
   */
  public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    RoundRectangle2D rect = new RoundRectangle2D.Float(x, y, width, height, arcWidth, arcHeight);
    draw(rect);
  }

  /**
   * Fills the specified rounded corner rectangle with the current color. The
   * left and right edges of the rectangle are at <code>x</code> and
   * <code>x&nbsp;+&nbsp;width&nbsp;-&nbsp;1</code>, respectively. The top
   * and bottom edges of the rectangle are at <code>y</code> and
   * <code>y&nbsp;+&nbsp;height&nbsp;-&nbsp;1</code>.
   * 
   * @param x
   *          the <i>x</i> coordinate of the rectangle to be filled.
   * @param y
   *          the <i>y</i> coordinate of the rectangle to be filled.
   * @param width
   *          the width of the rectangle to be filled.
   * @param height
   *          the height of the rectangle to be filled.
   * @param arcWidth
   *          the horizontal diameter of the arc at the four corners.
   * @param arcHeight
   *          the vertical diameter of the arc at the four corners.
   * @see java.awt.Graphics#drawRoundRect
   */
  public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    RoundRectangle2D rect = new RoundRectangle2D.Float(x, y, width, height, arcWidth, arcHeight);
    fill(rect);
  }

  /**
   * Draws the outline of an oval. The result is a circle or ellipse that fits
   * within the rectangle specified by the <code>x</code>, <code>y</code>,
   * <code>width</code>, and <code>height</code> arguments.
   * <p>
   * The oval covers an area that is <code>width&nbsp;+&nbsp;1</code> pixels
   * wide and <code>height&nbsp;+&nbsp;1</code> pixels tall.
   * 
   * @param x
   *          the <i>x</i> coordinate of the upper left corner of the oval to
   *          be drawn.
   * @param y
   *          the <i>y</i> coordinate of the upper left corner of the oval to
   *          be drawn.
   * @param width
   *          the width of the oval to be drawn.
   * @param height
   *          the height of the oval to be drawn.
   * @see java.awt.Graphics#fillOval
   */
  public void drawOval(int x, int y, int width, int height) {
    Ellipse2D oval = new Ellipse2D.Float(x, y, width, height);
    draw(oval);
  }

  /**
   * Fills an oval bounded by the specified rectangle with the current color.
   * 
   * @param x
   *          the <i>x</i> coordinate of the upper left corner of the oval to
   *          be filled.
   * @param y
   *          the <i>y</i> coordinate of the upper left corner of the oval to
   *          be filled.
   * @param width
   *          the width of the oval to be filled.
   * @param height
   *          the height of the oval to be filled.
   * @see java.awt.Graphics#drawOval
   */
  public void fillOval(int x, int y, int width, int height) {
    Ellipse2D oval = new Ellipse2D.Float(x, y, width, height);
    fill(oval);
  }

  /**
   * Draws the outline of a circular or elliptical arc covering the specified
   * rectangle.
   * <p>
   * The resulting arc begins at <code>startAngle</code> and extends for
   * <code>arcAngle</code> degrees, using the current color. Angles are
   * interpreted such that 0&nbsp;degrees is at the 3&nbsp;o'clock position. A
   * positive value indicates a counter-clockwise rotation while a negative
   * value indicates a clockwise rotation.
   * <p>
   * The center of the arc is the center of the rectangle whose origin is (<i>x</i>,&nbsp;<i>y</i>)
   * and whose size is specified by the <code>width</code> and
   * <code>height</code> arguments.
   * <p>
   * The resulting arc covers an area <code>width&nbsp;+&nbsp;1</code> pixels
   * wide by <code>height&nbsp;+&nbsp;1</code> pixels tall.
   * <p>
   * The angles are specified relative to the non-square extents of the bounding
   * rectangle such that 45 degrees always falls on the line from the center of
   * the ellipse to the upper right corner of the bounding rectangle. As a
   * result, if the bounding rectangle is noticeably longer in one axis than the
   * other, the angles to the start and end of the arc segment will be skewed
   * farther along the longer axis of the bounds.
   * 
   * @param x
   *          the <i>x</i> coordinate of the upper-left corner of the arc to be
   *          drawn.
   * @param y
   *          the <i>y</i> coordinate of the upper-left corner of the arc to be
   *          drawn.
   * @param width
   *          the width of the arc to be drawn.
   * @param height
   *          the height of the arc to be drawn.
   * @param startAngle
   *          the beginning angle.
   * @param arcAngle
   *          the angular extent of the arc, relative to the start angle.
   * @see java.awt.Graphics#fillArc
   */
  public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    Arc2D arc = new Arc2D.Float(x, y, width, height, startAngle, arcAngle, Arc2D.OPEN);
    draw(arc);
  }

  /**
   * Fills a circular or elliptical arc covering the specified rectangle.
   * <p>
   * The resulting arc begins at <code>startAngle</code> and extends for
   * <code>arcAngle</code> degrees. Angles are interpreted such that
   * 0&nbsp;degrees is at the 3&nbsp;o'clock position. A positive value
   * indicates a counter-clockwise rotation while a negative value indicates a
   * clockwise rotation.
   * <p>
   * The center of the arc is the center of the rectangle whose origin is (<i>x</i>,&nbsp;<i>y</i>)
   * and whose size is specified by the <code>width</code> and
   * <code>height</code> arguments.
   * <p>
   * The resulting arc covers an area <code>width&nbsp;+&nbsp;1</code> pixels
   * wide by <code>height&nbsp;+&nbsp;1</code> pixels tall.
   * <p>
   * The angles are specified relative to the non-square extents of the bounding
   * rectangle such that 45 degrees always falls on the line from the center of
   * the ellipse to the upper right corner of the bounding rectangle. As a
   * result, if the bounding rectangle is noticeably longer in one axis than the
   * other, the angles to the start and end of the arc segment will be skewed
   * farther along the longer axis of the bounds.
   * 
   * @param x
   *          the <i>x</i> coordinate of the upper-left corner of the arc to be
   *          filled.
   * @param y
   *          the <i>y</i> coordinate of the upper-left corner of the arc to be
   *          filled.
   * @param width
   *          the width of the arc to be filled.
   * @param height
   *          the height of the arc to be filled.
   * @param startAngle
   *          the beginning angle.
   * @param arcAngle
   *          the angular extent of the arc, relative to the start angle.
   * @see java.awt.Graphics#drawArc
   */
  public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    Arc2D arc = new Arc2D.Float(x, y, width, height, startAngle, arcAngle, Arc2D.PIE);
    fill(arc);
  }

  /**
   * Draws a sequence of connected lines defined by arrays of <i>x</i> and <i>y</i>
   * coordinates. Each pair of (<i>x</i>,&nbsp;<i>y</i>) coordinates defines
   * a point. The figure is not closed if the first point differs from the last
   * point.
   * 
   * @param xPoints
   *          an array of <i>x</i> points
   * @param yPoints
   *          an array of <i>y</i> points
   * @param nPoints
   *          the total number of points
   * @see java.awt.Graphics#drawPolygon(int[], int[], int)
   * @since JDK1.1
   */
  public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
    if (nPoints > 0) {
      GeneralPath path = new GeneralPath();
      path.moveTo(xPoints[0], yPoints[0]);
      for (int i = 1; i < nPoints; i++)
        path.lineTo(xPoints[i], yPoints[i]);

      draw(path);
    }
  }

  /**
   * Draws a closed polygon defined by arrays of <i>x</i> and <i>y</i>
   * coordinates. Each pair of (<i>x</i>,&nbsp;<i>y</i>) coordinates defines
   * a point.
   * <p>
   * This method draws the polygon defined by <code>nPoint</code> line
   * segments, where the first <code>nPoint&nbsp;-&nbsp;1</code> line segments
   * are line segments from
   * <code>(xPoints[i&nbsp;-&nbsp;1],&nbsp;yPoints[i&nbsp;-&nbsp;1])</code> to
   * <code>(xPoints[i],&nbsp;yPoints[i])</code>, for 1&nbsp;&le;&nbsp;<i>i</i>&nbsp;&le;&nbsp;<code>nPoints</code>.
   * The figure is automatically closed by drawing a line connecting the final
   * point to the first point, if those points are different.
   * 
   * @param xPoints
   *          a an array of <code>x</code> coordinates.
   * @param yPoints
   *          a an array of <code>y</code> coordinates.
   * @param nPoints
   *          a the total number of points.
   * @see java.awt.Graphics#fillPolygon(int[],int[],int)
   * @see java.awt.Graphics#drawPolyline
   */
  public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
    Polygon polygon = new Polygon(xPoints, yPoints, nPoints);
    draw(polygon);
  }

  /**
   * Fills a closed polygon defined by arrays of <i>x</i> and <i>y</i>
   * coordinates.
   * <p>
   * This method draws the polygon defined by <code>nPoint</code> line
   * segments, where the first <code>nPoint&nbsp;-&nbsp;1</code> line segments
   * are line segments from
   * <code>(xPoints[i&nbsp;-&nbsp;1],&nbsp;yPoints[i&nbsp;-&nbsp;1])</code> to
   * <code>(xPoints[i],&nbsp;yPoints[i])</code>, for 1&nbsp;&le;&nbsp;<i>i</i>&nbsp;&le;&nbsp;<code>nPoints</code>.
   * The figure is automatically closed by drawing a line connecting the final
   * point to the first point, if those points are different.
   * <p>
   * The area inside the polygon is defined using an even-odd fill rule, also
   * known as the alternating rule.
   * 
   * @param xPoints
   *          a an array of <code>x</code> coordinates.
   * @param yPoints
   *          a an array of <code>y</code> coordinates.
   * @param nPoints
   *          a the total number of points.
   * @see java.awt.Graphics#drawPolygon(int[], int[], int)
   */
  public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
    Polygon polygon = new Polygon(xPoints, yPoints, nPoints);
    fill(polygon);
  }

  /**
   * Draws the text given by the specified string, using this graphics context's
   * current font and color. The baseline of the first character is at position (<i>x</i>,&nbsp;<i>y</i>)
   * in this graphics context's coordinate system.
   * 
   * @param str
   *          the string to be drawn.
   * @param x
   *          the <i>x</i> coordinate.
   * @param y
   *          the <i>y</i> coordinate.
   * @see java.awt.Graphics#drawBytes
   * @see java.awt.Graphics#drawChars
   */
  public void drawString(String str, int x, int y) {
    drawString(str, (float) x, (float) y);
  }

  /**
   * Draws the text given by the specified iterator, using this graphics
   * context's current color. The iterator has to specify a font for each
   * character. The baseline of the first character is at position (<i>x</i>,&nbsp;<i>y</i>)
   * in this graphics context's coordinate system.
   * 
   * @param iterator
   *          the iterator whose text is to be drawn
   * @param x
   *          the <i>x</i> coordinate.
   * @param y
   *          the <i>y</i> coordinate.
   * @see java.awt.Graphics#drawBytes
   * @see java.awt.Graphics#drawChars
   */
  public void drawString(AttributedCharacterIterator iterator, int x, int y) {
    drawString(iterator, (float) x, (float) y);
  }

  /**
   * Draws as much of the specified image as is currently available. The image
   * is drawn with its top-left corner at (<i>x</i>,&nbsp;<i>y</i>) in this
   * graphics context's coordinate space. Transparent pixels are drawn in the
   * specified background color.
   * <p>
   * This operation is equivalent to filling a rectangle of the width and height
   * of the specified image with the given color and then drawing the image on
   * top of it, but possibly more efficient.
   * <p>
   * This method returns immediately in all cases, even if the complete image
   * has not yet been loaded, and it has not been dithered and converted for the
   * current output device.
   * <p>
   * If the image has not yet been completely loaded, then
   * <code>drawImage</code> returns <code>false</code>. As more of the
   * image becomes available, the process that draws the image notifies the
   * specified image observer.
   * 
   * @param img
   *          the specified image to be drawn.
   * @param x
   *          the <i>x</i> coordinate.
   * @param y
   *          the <i>y</i> coordinate.
   * @param bgcolor
   *          the background color to paint under the non-opaque portions of the
   *          image.
   * @param observer
   *          object to be notified as more of the image is converted.
   * @see java.awt.Image
   * @see java.awt.image.ImageObserver
   * @see java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int,
   *      int, int, int)
   */
  public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
    return drawImage(img, x, y, img.getWidth(null), img.getHeight(null), bgcolor, observer);
  }

  /**
   * Draws as much of the specified image as has already been scaled to fit
   * inside the specified rectangle.
   * <p>
   * The image is drawn inside the specified rectangle of this graphics
   * context's coordinate space, and is scaled if necessary. Transparent pixels
   * are drawn in the specified background color. This operation is equivalent
   * to filling a rectangle of the width and height of the specified image with
   * the given color and then drawing the image on top of it, but possibly more
   * efficient.
   * <p>
   * This method returns immediately in all cases, even if the entire image has
   * not yet been scaled, dithered, and converted for the current output device.
   * If the current output representation is not yet complete then
   * <code>drawImage</code> returns <code>false</code>. As more of the
   * image becomes available, the process that draws the image notifies the
   * specified image observer.
   * <p>
   * A scaled version of an image will not necessarily be available immediately
   * just because an unscaled version of the image has been constructed for this
   * output device. Each size of the image may be cached separately and
   * generated from the original data in a separate image production sequence.
   * 
   * @param img
   *          the specified image to be drawn.
   * @param x
   *          the <i>x</i> coordinate.
   * @param y
   *          the <i>y</i> coordinate.
   * @param width
   *          the width of the rectangle.
   * @param height
   *          the height of the rectangle.
   * @param bgcolor
   *          the background color to paint under the non-opaque portions of the
   *          image.
   * @param observer
   *          object to be notified as more of the image is converted.
   * @see java.awt.Image
   * @see java.awt.image.ImageObserver
   * @see java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int,
   *      int, int, int)
   */
  public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor,
      ImageObserver observer) {
    Paint paint = gc.getPaint();
    gc.setPaint(bgcolor);
    fillRect(x, y, width, height);
    gc.setPaint(paint);
    drawImage(img, x, y, width, height, observer);

    return true;
  }

  /**
   * Draws as much of the specified area of the specified image as is currently
   * available, scaling it on the fly to fit inside the specified area of the
   * destination drawable surface. Transparent pixels do not affect whatever
   * pixels are already there.
   * <p>
   * This method returns immediately in all cases, even if the image area to be
   * drawn has not yet been scaled, dithered, and converted for the current
   * output device. If the current output representation is not yet complete
   * then <code>drawImage</code> returns <code>false</code>. As more of the
   * image becomes available, the process that draws the image notifies the
   * specified image observer.
   * <p>
   * This method always uses the unscaled version of the image to render the
   * scaled rectangle and performs the required scaling on the fly. It does not
   * use a cached, scaled version of the image for this operation. Scaling of
   * the image from source to destination is performed such that the first
   * coordinate of the source rectangle is mapped to the first coordinate of the
   * destination rectangle, and the second source coordinate is mapped to the
   * second destination coordinate. The subimage is scaled and flipped as needed
   * to preserve those mappings.
   * 
   * @param img
   *          the specified image to be drawn
   * @param dx1
   *          the <i>x</i> coordinate of the first corner of the destination
   *          rectangle.
   * @param dy1
   *          the <i>y</i> coordinate of the first corner of the destination
   *          rectangle.
   * @param dx2
   *          the <i>x</i> coordinate of the second corner of the destination
   *          rectangle.
   * @param dy2
   *          the <i>y</i> coordinate of the second corner of the destination
   *          rectangle.
   * @param sx1
   *          the <i>x</i> coordinate of the first corner of the source
   *          rectangle.
   * @param sy1
   *          the <i>y</i> coordinate of the first corner of the source
   *          rectangle.
   * @param sx2
   *          the <i>x</i> coordinate of the second corner of the source
   *          rectangle.
   * @param sy2
   *          the <i>y</i> coordinate of the second corner of the source
   *          rectangle.
   * @param observer
   *          object to be notified as more of the image is scaled and
   *          converted.
   * @see java.awt.Image
   * @see java.awt.image.ImageObserver
   * @see java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int,
   *      int, int, int)
   * @since JDK1.1
   */
  public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1,
      int sx2, int sy2, ImageObserver observer) {
    BufferedImage src = new BufferedImage(img.getWidth(null), img.getHeight(null),
        BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = src.createGraphics();
    g.drawImage(img, 0, 0, null);
    g.dispose();

    src = src.getSubimage(sx1, sy1, sx2 - sx1, sy2 - sy1);

    return drawImage(src, dx1, dy1, dx2 - dx1, dy2 - dy1, observer);
  }

  /**
   * Draws as much of the specified area of the specified image as is currently
   * available, scaling it on the fly to fit inside the specified area of the
   * destination drawable surface.
   * <p>
   * Transparent pixels are drawn in the specified background color. This
   * operation is equivalent to filling a rectangle of the width and height of
   * the specified image with the given color and then drawing the image on top
   * of it, but possibly more efficient.
   * <p>
   * This method returns immediately in all cases, even if the image area to be
   * drawn has not yet been scaled, dithered, and converted for the current
   * output device. If the current output representation is not yet complete
   * then <code>drawImage</code> returns <code>false</code>. As more of the
   * image becomes available, the process that draws the image notifies the
   * specified image observer.
   * <p>
   * This method always uses the unscaled version of the image to render the
   * scaled rectangle and performs the required scaling on the fly. It does not
   * use a cached, scaled version of the image for this operation. Scaling of
   * the image from source to destination is performed such that the first
   * coordinate of the source rectangle is mapped to the first coordinate of the
   * destination rectangle, and the second source coordinate is mapped to the
   * second destination coordinate. The subimage is scaled and flipped as needed
   * to preserve those mappings.
   * 
   * @param img
   *          the specified image to be drawn
   * @param dx1
   *          the <i>x</i> coordinate of the first corner of the destination
   *          rectangle.
   * @param dy1
   *          the <i>y</i> coordinate of the first corner of the destination
   *          rectangle.
   * @param dx2
   *          the <i>x</i> coordinate of the second corner of the destination
   *          rectangle.
   * @param dy2
   *          the <i>y</i> coordinate of the second corner of the destination
   *          rectangle.
   * @param sx1
   *          the <i>x</i> coordinate of the first corner of the source
   *          rectangle.
   * @param sy1
   *          the <i>y</i> coordinate of the first corner of the source
   *          rectangle.
   * @param sx2
   *          the <i>x</i> coordinate of the second corner of the source
   *          rectangle.
   * @param sy2
   *          the <i>y</i> coordinate of the second corner of the source
   *          rectangle.
   * @param bgcolor
   *          the background color to paint under the non-opaque portions of the
   *          image.
   * @param observer
   *          object to be notified as more of the image is scaled and
   *          converted.
   * @see java.awt.Image
   * @see java.awt.image.ImageObserver
   * @see java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int,
   *      int, int, int)
   * @since JDK1.1
   */
  public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1,
      int sx2, int sy2, Color bgcolor, ImageObserver observer) {
    Paint paint = gc.getPaint();
    gc.setPaint(bgcolor);
    fillRect(dx1, dy1, dx2 - dx1, dy2 - dy1);
    gc.setPaint(paint);
    return drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
  }

  /**
   * Renders an image, applying a transform from image space into user space
   * before drawing. The transformation from user space into device space is
   * done with the current <code>Transform</code> in the
   * <code>Graphics2D</code>. The specified transformation is applied to the
   * image before the transform attribute in the <code>Graphics2D</code>
   * context is applied. The rendering attributes applied include the
   * <code>Clip</code>, <code>Transform</code>, and <code>Composite</code>
   * attributes. Note that no rendering is done if the specified transform is
   * noninvertible.
   * 
   * @param img
   *          the <code>Image</code> to be rendered
   * @param xform
   *          the transformation from image space into user space
   * @param obs
   *          the {@link ImageObserver} to be notified as more of the
   *          <code>Image</code> is converted
   * @return <code>true</code> if the <code>Image</code> is fully loaded and
   *         completely rendered; <code>false</code> if the <code>Image</code>
   *         is still being loaded.
   * @see #transform
   * @see #setTransform
   * @see #setComposite
   * @see #clip
   * @see #setClip(Shape)
   */
  public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
    boolean retVal = true;

    if (xform.getDeterminant() != 0) {
      AffineTransform inverseTransform = null;
      try {
        inverseTransform = xform.createInverse();
      } catch (NoninvertibleTransformException e) {
        // Should never happen since we checked the
        // matrix determinant
        throw new Error(e.getMessage());
      }

      gc.transform(xform);
      retVal = drawImage(img, 0, 0, null);
      gc.transform(inverseTransform);
    } else {
      AffineTransform savTransform = new AffineTransform(gc.getTransform());
      gc.transform(xform);
      retVal = drawImage(img, 0, 0, null);
      gc.setTransform(savTransform);
    }

    return retVal;

  }

  /**
   * Renders a <code>BufferedImage</code> that is filtered with a
   * {@link BufferedImageOp}. The rendering attributes applied include the
   * <code>Clip</code>, <code>Transform</code> and <code>Composite</code>
   * attributes. This is equivalent to:
   * 
   * <pre>
   * img1 = op.filter(img, null);
   * drawImage(img1, new AffineTransform(1f, 0f, 0f, 1f, x, y), null);
   * </pre>
   * 
   * @param img
   *          the <code>BufferedImage</code> to be rendered
   * @param op
   *          the filter to be applied to the image before rendering
   * @param x
   *          the x coordinate in user space where the image is rendered
   * @param y
   *          the y coordinate in user space where the image is rendered
   * @see #transform
   * @see #setTransform
   * @see #setComposite
   * @see #clip
   * @see #setClip(Shape)
   */
  public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
    img = op.filter(img, null);
    drawImage(img, x, y, null);
  }

  /**
   * Renders the text of the specified {@link GlyphVector} using the
   * <code>Graphics2D</code> context's rendering attributes. The rendering
   * attributes applied include the <code>Clip</code>, <code>Transform</code>,
   * <code>Paint</code>, and <code>Composite</code> attributes. The
   * <code>GlyphVector</code> specifies individual glyphs from a {@link Font}.
   * The <code>GlyphVector</code> can also contain the glyph positions. This
   * is the fastest way to render a set of characters to the screen.
   * 
   * @param g
   *          the <code>GlyphVector</code> to be rendered
   * @param x
   *          the x position in user space where the glyphs should be rendered
   * @param y
   *          the y position in user space where the glyphs should be rendered
   * 
   * @see java.awt.Font#createGlyphVector(FontRenderContext, char[])
   * @see java.awt.font.GlyphVector
   * @see #setPaint
   * @see java.awt.Graphics#setColor
   * @see #setTransform
   * @see #setComposite
   * @see #setClip(Shape)
   */
  public void drawGlyphVector(GlyphVector g, float x, float y) {
    Shape glyphOutline = g.getOutline(x, y);
    fill(glyphOutline);
  }

  /**
   * Checks whether or not the specified <code>Shape</code> intersects the
   * specified {@link Rectangle}, which is in device space. If
   * <code>onStroke</code> is false, this method checks whether or not the
   * interior of the specified <code>Shape</code> intersects the specified
   * <code>Rectangle</code>. If <code>onStroke</code> is <code>true</code>,
   * this method checks whether or not the <code>Stroke</code> of the
   * specified <code>Shape</code> outline intersects the specified
   * <code>Rectangle</code>. The rendering attributes taken into account
   * include the <code>Clip</code>, <code>Transform</code>, and
   * <code>Stroke</code> attributes.
   * 
   * @param rect
   *          the area in device space to check for a hit
   * @param s
   *          the <code>Shape</code> to check for a hit
   * @param onStroke
   *          flag used to choose between testing the stroked or the filled
   *          shape. If the flag is <code>true</code>, the
   *          <code>Stroke</code> oultine is tested. If the flag is
   *          <code>false</code>, the filled <code>Shape</code> is tested.
   * @return <code>true</code> if there is a hit; <code>false</code>
   *         otherwise.
   * @see #setStroke
   * @see #fill(Shape)
   * @see #draw(Shape)
   * @see #transform
   * @see #setTransform
   * @see #clip
   * @see #setClip(Shape)
   */
  public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
    if (onStroke) {
      s = gc.getStroke().createStrokedShape(s);
    }

    s = gc.getTransform().createTransformedShape(s);

    return s.intersects(rect);
  }

  /**
   * Sets the <code>Composite</code> for the <code>Graphics2D</code>
   * context. The <code>Composite</code> is used in all drawing methods such
   * as <code>drawImage</code>, <code>drawString</code>, <code>draw</code>,
   * and <code>fill</code>. It specifies how new pixels are to be combined
   * with the existing pixels on the graphics device during the rendering
   * process.
   * <p>
   * If this <code>Graphics2D</code> context is drawing to a
   * <code>Component</code> on the display screen and the
   * <code>Composite</code> is a custom object rather than an instance of the
   * <code>AlphaComposite</code> class, and if there is a security manager,
   * its <code>checkPermission</code> method is called with an
   * <code>AWTPermission("readDisplayPixels")</code> permission.
   * 
   * @param comp
   *          the <code>Composite</code> object to be used for rendering
   * @throws SecurityException
   *           if a custom <code>Composite</code> object is being used to
   *           render to the screen and a security manager is set and its
   *           <code>checkPermission</code> method does not allow the
   *           operation.
   * @see java.awt.Graphics#setXORMode
   * @see java.awt.Graphics#setPaintMode
   * @see java.awt.AlphaComposite
   */
  public void setComposite(Composite comp) {
    gc.setComposite(comp);
  }

  /**
   * Sets the <code>Paint</code> attribute for the <code>Graphics2D</code>
   * context. Calling this method with a <code>null</code> <code>Paint</code>
   * object does not have any effect on the current <code>Paint</code>
   * attribute of this <code>Graphics2D</code>.
   * 
   * @param paint
   *          the <code>Paint</code> object to be used to generate color
   *          during the rendering process, or <code>null</code>
   * @see java.awt.Graphics#setColor
   */
  public void setPaint(Paint paint) {
    gc.setPaint(paint);
  }

  /**
   * Sets the <code>Stroke</code> for the <code>Graphics2D</code> context.
   * 
   * @param s
   *          the <code>Stroke</code> object to be used to stroke a
   *          <code>Shape</code> during the rendering process
   */
  public void setStroke(Stroke s) {
    gc.setStroke(s);
  }

  /**
   * Sets the value of a single preference for the rendering algorithms. Hint
   * categories include controls for rendering quality and overall time/quality
   * trade-off in the rendering process. Refer to the
   * <code>RenderingHints</code> class for definitions of some common keys and
   * values.
   * 
   * @param hintKey
   *          the key of the hint to be set.
   * @param hintValue
   *          the value indicating preferences for the specified hint category.
   * @see RenderingHints
   */
  public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
    gc.setRenderingHint(hintKey, hintValue);
  }

  /**
   * Returns the value of a single preference for the rendering algorithms. Hint
   * categories include controls for rendering quality and overall time/quality
   * trade-off in the rendering process. Refer to the
   * <code>RenderingHints</code> class for definitions of some common keys and
   * values.
   * 
   * @param hintKey
   *          the key corresponding to the hint to get.
   * @return an object representing the value for the specified hint key. Some
   *         of the keys and their associated values are defined in the
   *         <code>RenderingHints</code> class.
   * @see RenderingHints
   */
  public Object getRenderingHint(RenderingHints.Key hintKey) {
    return gc.getRenderingHint(hintKey);
  }

  /**
   * Replaces the values of all preferences for the rendering algorithms with
   * the specified <code>hints</code>. The existing values for all rendering
   * hints are discarded and the new set of known hints and values are
   * initialized from the specified {@link Map} object. Hint categories include
   * controls for rendering quality and overall time/quality trade-off in the
   * rendering process. Refer to the <code>RenderingHints</code> class for
   * definitions of some common keys and values.
   * 
   * @param hints
   *          the rendering hints to be set
   * @see RenderingHints
   */
  public void setRenderingHints(Map hints) {
    gc.setRenderingHints(hints);
  }

  /**
   * Sets the values of an arbitrary number of preferences for the rendering
   * algorithms. Only values for the rendering hints that are present in the
   * specified <code>Map</code> object are modified. All other preferences not
   * present in the specified object are left unmodified. Hint categories
   * include controls for rendering quality and overall time/quality trade-off
   * in the rendering process. Refer to the <code>RenderingHints</code> class
   * for definitions of some common keys and values.
   * 
   * @param hints
   *          the rendering hints to be set
   * @see RenderingHints
   */
  public void addRenderingHints(Map hints) {
    gc.addRenderingHints(hints);
  }

  /**
   * Gets the preferences for the rendering algorithms. Hint categories include
   * controls for rendering quality and overall time/quality trade-off in the
   * rendering process. Returns all of the hint key/value pairs that were ever
   * specified in one operation. Refer to the <code>RenderingHints</code>
   * class for definitions of some common keys and values.
   * 
   * @return a reference to an instance of <code>RenderingHints</code> that
   *         contains the current preferences.
   * @see RenderingHints
   */
  public RenderingHints getRenderingHints() {
    return gc.getRenderingHints();
  }

  /**
   * Concatenates the current <code>Graphics2D</code> <code>Transform</code>
   * with a translation transform. Subsequent rendering is translated by the
   * specified distance relative to the previous position. This is equivalent to
   * calling transform(T), where T is an <code>AffineTransform</code>
   * represented by the following matrix:
   * 
   * <pre>
   *           [   1    0    tx  ]
   *           [   0    1    ty  ]
   *           [   0    0    1   ]
   * </pre>
   * 
   * @param tx
   *          the distance to translate along the x-axis
   * @param ty
   *          the distance to translate along the y-axis
   */
  public void translate(double tx, double ty) {
    gc.translate(tx, ty);
  }

  /**
   * Concatenates the current <code>Graphics2D</code>
   * <code>Transform</code>
   * with a rotation transform. Subsequent rendering is rotated by the specified
   * radians relative to the previous origin. This is equivalent to calling
   * <code>transform(R)</code>, where R is an <code>AffineTransform</code>
   * represented by the following matrix:
   * 
   * <pre>
   *           [   cos(theta)    -sin(theta)    0   ]
   *           [   sin(theta)     cos(theta)    0   ]
   *           [       0              0         1   ]
   * </pre>
   * 
   * Rotating with a positive angle theta rotates points on the positive x axis
   * toward the positive y axis.
   * 
   * @param theta
   *          the angle of rotation in radians
   */
  public void rotate(double theta) {
    gc.rotate(theta);
  }

  /**
   * Concatenates the current <code>Graphics2D</code>
   * <code>Transform</code>
   * with a translated rotation transform. Subsequent rendering is transformed
   * by a transform which is constructed by translating to the specified
   * location, rotating by the specified radians, and translating back by the
   * same amount as the original translation. This is equivalent to the
   * following sequence of calls:
   * 
   * <pre>
   * translate(x, y);
   * rotate(theta);
   * translate(-x, -y);
   * </pre>
   * 
   * Rotating with a positive angle theta rotates points on the positive x axis
   * toward the positive y axis.
   * 
   * @param theta
   *          the angle of rotation in radians
   * @param x
   *          the x coordinate of the origin of the rotation
   * @param y
   *          the y coordinate of the origin of the rotation
   */
  public void rotate(double theta, double x, double y) {
    gc.rotate(theta, x, y);
  }

  /**
   * Concatenates the current <code>Graphics2D</code>
   * <code>Transform</code>
   * with a scaling transformation Subsequent rendering is resized according to
   * the specified scaling factors relative to the previous scaling. This is
   * equivalent to calling <code>transform(S)</code>, where S is an
   * <code>AffineTransform</code> represented by the following matrix:
   * 
   * <pre>
   *           [   sx   0    0   ]
   *           [   0    sy   0   ]
   *           [   0    0    1   ]
   * </pre>
   * 
   * @param sx
   *          the amount by which X coordinates in subsequent rendering
   *          operations are multiplied relative to previous rendering
   *          operations.
   * @param sy
   *          the amount by which Y coordinates in subsequent rendering
   *          operations are multiplied relative to previous rendering
   *          operations.
   */
  public void scale(double sx, double sy) {
    gc.scale(sx, sy);
  }

  /**
   * Concatenates the current <code>Graphics2D</code>
   * <code>Transform</code>
   * with a shearing transform. Subsequent renderings are sheared by the
   * specified multiplier relative to the previous position. This is equivalent
   * to calling <code>transform(SH)</code>, where SH is an
   * <code>AffineTransform</code> represented by the following matrix:
   * 
   * <pre>
   *           [   1   shx   0   ]
   *           [  shy   1    0   ]
   *           [   0    0    1   ]
   * </pre>
   * 
   * @param shx
   *          the multiplier by which coordinates are shifted in the positive X
   *          axis direction as a function of their Y coordinate
   * @param shy
   *          the multiplier by which coordinates are shifted in the positive Y
   *          axis direction as a function of their X coordinate
   */
  public void shear(double shx, double shy) {
    gc.shear(shx, shy);
  }

  /**
   * Composes an <code>AffineTransform</code> object with the
   * <code>Transform</code> in this <code>Graphics2D</code> according to the
   * rule last-specified-first-applied. If the current <code>Transform</code>
   * is Cx, the result of composition with Tx is a new <code>Transform</code>
   * Cx'. Cx' becomes the current <code>Transform</code> for this
   * <code>Graphics2D</code>. Transforming a point p by the updated
   * <code>Transform</code> Cx' is equivalent to first transforming p by Tx
   * and then transforming the result by the original <code>Transform</code>
   * Cx. In other words, Cx'(p) = Cx(Tx(p)). A copy of the Tx is made, if
   * necessary, so further modifications to Tx do not affect rendering.
   * 
   * @param Tx
   *          the <code>AffineTransform</code> object to be composed with the
   *          current <code>Transform</code>
   * @see #setTransform
   * @see AffineTransform
   */
  public void transform(AffineTransform Tx) {
    gc.transform(Tx);
  }

  /**
   * Sets the <code>Transform</code> in the <code>Graphics2D</code> context.
   * 
   * @param Tx
   *          the <code>AffineTransform</code> object to be used in the
   *          rendering process
   * @see #transform
   * @see AffineTransform
   */
  public void setTransform(AffineTransform Tx) {
    gc.setTransform(Tx);
  }

  /**
   * Returns a copy of the current <code>Transform</code> in the
   * <code>Graphics2D</code> context.
   * 
   * @return the current <code>AffineTransform</code> in the
   *         <code>Graphics2D</code> context.
   * @see #transform
   * @see #setTransform
   */
  public AffineTransform getTransform() {
    return gc.getTransform();
  }

  /**
   * Returns the current <code>Paint</code> of the <code>Graphics2D</code>
   * context.
   * 
   * @return the current <code>Graphics2D</code> <code>Paint</code>, which
   *         defines a color or pattern.
   * @see #setPaint
   * @see java.awt.Graphics#setColor
   */
  public Paint getPaint() {
    return gc.getPaint();
  }

  /**
   * Returns the current <code>Composite</code> in the <code>Graphics2D</code>
   * context.
   * 
   * @return the current <code>Graphics2D</code> <code>Composite</code>,
   *         which defines a compositing style.
   * @see #setComposite
   */
  public Composite getComposite() {
    return gc.getComposite();
  }

  /**
   * Sets the background color for the <code>Graphics2D</code> context. The
   * background color is used for clearing a region. When a
   * <code>Graphics2D</code> is constructed for a <code>Component</code>,
   * the background color is inherited from the <code>Component</code>.
   * Setting the background color in the <code>Graphics2D</code> context only
   * affects the subsequent <code>clearRect</code> calls and not the
   * background color of the <code>Component</code>. To change the background
   * of the <code>Component</code>, use appropriate methods of the
   * <code>Component</code>.
   * 
   * @param color
   *          the background color that isused in subsequent calls to
   *          <code>clearRect</code>
   * @see #getBackground
   * @see java.awt.Graphics#clearRect
   */
  public void setBackground(Color color) {
    gc.setBackground(color);
  }

  /**
   * Returns the background color used for clearing a region.
   * 
   * @return the current <code>Graphics2D</code> <code>Color</code>, which
   *         defines the background color.
   * @see #setBackground
   */
  public Color getBackground() {
    return gc.getBackground();
  }

  /**
   * Returns the current <code>Stroke</code> in the <code>Graphics2D</code>
   * context.
   * 
   * @return the current <code>Graphics2D</code> <code>Stroke</code>, which
   *         defines the line style.
   * @see #setStroke
   */
  public Stroke getStroke() {
    return gc.getStroke();
  }

  /**
   * Intersects the current <code>Clip</code> with the interior of the
   * specified <code>Shape</code> and sets the <code>Clip</code> to the
   * resulting intersection. The specified <code>Shape</code> is transformed
   * with the current <code>Graphics2D</code>
   * <code>Transform</code> before
   * being intersected with the current <code>Clip</code>. This method is
   * used to make the current <code>Clip</code> smaller. To make the
   * <code>Clip</code> larger, use <code>setClip</code>. The <i>user clip</i>
   * modified by this method is independent of the clipping associated with
   * device bounds and visibility. If no clip has previously been set, or if the
   * clip has been cleared using
   * {@link java.awt.Graphics#setClip(Shape) setClip} with a <code>null</code>
   * argument, the specified <code>Shape</code> becomes the new user clip.
   * 
   * @param s
   *          the <code>Shape</code> to be intersected with the current
   *          <code>Clip</code>. If <code>s</code> is <code>null</code>,
   *          this method clears the current <code>Clip</code>.
   */
  public void clip(Shape s) {
    gc.clip(s);
  }

  /**
   * Get the rendering context of the <code>Font</code> within this
   * <code>Graphics2D</code> context. The {@link FontRenderContext}
   * encapsulates application hints such as anti-aliasing and fractional
   * metrics, as well as target device specific information such as
   * dots-per-inch. This information should be provided by the application when
   * using objects that perform typographical formatting, such as
   * <code>Font</code> and <code>TextLayout</code>. This information should
   * also be provided by applications that perform their own layout and need
   * accurate measurements of various characteristics of glyphs such as advance
   * and line height when various rendering hints have been applied to the text
   * rendering.
   * 
   * @return a reference to an instance of FontRenderContext.
   * @see java.awt.font.FontRenderContext
   * @see java.awt.Font#createGlyphVector(FontRenderContext,char[])
   * @see java.awt.font.TextLayout
   * @since JDK1.2
   */
  public FontRenderContext getFontRenderContext() {
    return gc.getFontRenderContext();
  }

  /**
   * @return the {@link GraphicContext} of this <code>Graphics2D</code>.
   */
  public GraphicContext getGraphicContext() {
    return gc;
  }
}

/*
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */

/**
 * Handles the attributes in a graphic context:<br> + Composite <br> + Font
 * <br> + Paint <br> + Stroke <br> + Clip <br> + RenderingHints <br> +
 * AffineTransform <br>
 * 
 * @author <a href="mailto:cjolif@ilog.fr">Christophe Jolif</a>
 * @author <a href="mailto:vincent.hardy@eng.sun.com">Vincent Hardy</a>
 * @version $Id: GraphicContext.java 479564 2006-11-27 09:56:57Z dvholten $
 */
class GraphicContext implements Cloneable {
  /**
   * Default Transform to be used for creating FontRenderContext.
   */
  protected AffineTransform defaultTransform = new AffineTransform();

  /**
   * Current AffineTransform. This is the concatenation of the original
   * AffineTransform (i.e., last setTransform invocation) and the following
   * transform invocations, as captured by originalTransform and the
   * transformStack.
   */
  protected AffineTransform transform = new AffineTransform();

  /**
   * Transform stack
   */
  protected List transformStack = new ArrayList();

  /**
   * Defines whether the transform stack is valide or not. This is for use by
   * the class clients. The client should validate the stack every time it
   * starts using it. The stack becomes invalid when a new transform is set.
   * 
   * @see #invalidateTransformStack()
   * @see #isTransformStackValid
   * @see #setTransform
   */
  protected boolean transformStackValid = true;

  /**
   * Current Paint
   */
  protected Paint paint = Color.black;

  /**
   * Current Stroke
   */
  protected Stroke stroke = new BasicStroke();

  /**
   * Current Composite
   */
  protected Composite composite = AlphaComposite.SrcOver;

  /**
   * Current clip
   */
  protected Shape clip = null;

  /**
   * Current set of RenderingHints
   */
  protected RenderingHints hints = new RenderingHints(null);

  /**
   * Current Font
   */
  protected Font font = new Font("sanserif", Font.PLAIN, 12);

  /**
   * Current background color.
   */
  protected Color background = new Color(0, 0, 0, 0);

  /**
   * Current foreground color
   */
  protected Color foreground = Color.black;

  /**
   * Default constructor
   */
  public GraphicContext() {
    // to workaround a JDK bug
    hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_DEFAULT);
  }

  /**
   * @param defaultDeviceTransform
   *          Default affine transform applied to map the user space to the user
   *          space.
   */
  public GraphicContext(AffineTransform defaultDeviceTransform) {
    this();
    defaultTransform = new AffineTransform(defaultDeviceTransform);
    transform = new AffineTransform(defaultTransform);
    if (!defaultTransform.isIdentity())
      transformStack.add(TransformStackElement.createGeneralTransformElement(defaultTransform));
  }

  /**
   * @return a deep copy of this context
   */
  public Object clone() {
    GraphicContext copyGc = new GraphicContext(defaultTransform);

    //
    // Now, copy each GC element in turn
    //

    // Default transform
    /* Set in constructor */

    // Transform
    copyGc.transform = new AffineTransform(this.transform);

    // Transform stack
    copyGc.transformStack = new ArrayList(transformStack.size());
    for (int i = 0; i < this.transformStack.size(); i++) {
      TransformStackElement stackElement = (TransformStackElement) this.transformStack.get(i);
      copyGc.transformStack.add(stackElement.clone());
    }

    // Transform stack validity
    copyGc.transformStackValid = this.transformStackValid;

    // Paint (immutable by requirement)
    copyGc.paint = this.paint;

    // Stroke (immutable by requirement)
    copyGc.stroke = this.stroke;

    // Composite (immutable by requirement)
    copyGc.composite = this.composite;

    // Clip
    if (clip != null)
      copyGc.clip = new GeneralPath(clip);
    else
      copyGc.clip = null;

    // RenderingHints
    copyGc.hints = (RenderingHints) this.hints.clone();

    // Font (immutable)
    copyGc.font = this.font;

    // Background, Foreground (immutable)
    copyGc.background = this.background;
    copyGc.foreground = this.foreground;

    return copyGc;
  }

  /**
   * Gets this graphics context's current color.
   * 
   * @return this graphics context's current color.
   * @see java.awt.Color
   * @see java.awt.Graphics#setColor
   */
  public Color getColor() {
    return foreground;
  }

  /**
   * Sets this graphics context's current color to the specified color. All
   * subsequent graphics operations using this graphics context use this
   * specified color.
   * 
   * @param c
   *          the new rendering color.
   * @see java.awt.Color
   * @see java.awt.Graphics#getColor
   */
  public void setColor(Color c) {
    if (c == null)
      return;

    if (paint != c)
      setPaint(c);
  }

  /**
   * Gets the current font.
   * 
   * @return this graphics context's current font.
   * @see java.awt.Font
   * @see java.awt.Graphics#setFont
   */
  public Font getFont() {
    return font;
  }

  /**
   * Sets this graphics context's font to the specified font. All subsequent
   * text operations using this graphics context use this font.
   * 
   * @param font
   *          the font.
   * @see java.awt.Graphics#getFont
   */
  public void setFont(Font font) {
    if (font != null)
      this.font = font;
  }

  /**
   * Returns the bounding rectangle of the current clipping area. This method
   * refers to the user clip, which is independent of the clipping associated
   * with device bounds and window visibility. If no clip has previously been
   * set, or if the clip has been cleared using <code>setClip(null)</code>,
   * this method returns <code>null</code>. The coordinates in the rectangle
   * are relative to the coordinate system origin of this graphics context.
   * 
   * @return the bounding rectangle of the current clipping area, or
   *         <code>null</code> if no clip is set.
   * @see java.awt.Graphics#getClip
   * @see java.awt.Graphics#clipRect
   * @see java.awt.Graphics#setClip(int, int, int, int)
   * @see java.awt.Graphics#setClip(Shape)
   * @since JDK1.1
   */
  public Rectangle getClipBounds() {
    Shape c = getClip();
    if (c == null)
      return null;
    else
      return c.getBounds();
  }

  /**
   * Intersects the current clip with the specified rectangle. The resulting
   * clipping area is the intersection of the current clipping area and the
   * specified rectangle. If there is no current clipping area, either because
   * the clip has never been set, or the clip has been cleared using
   * <code>setClip(null)</code>, the specified rectangle becomes the new
   * clip. This method sets the user clip, which is independent of the clipping
   * associated with device bounds and window visibility. This method can only
   * be used to make the current clip smaller. To set the current clip larger,
   * use any of the setClip methods. Rendering operations have no effect outside
   * of the clipping area.
   * 
   * @param x
   *          the x coordinate of the rectangle to intersect the clip with
   * @param y
   *          the y coordinate of the rectangle to intersect the clip with
   * @param width
   *          the width of the rectangle to intersect the clip with
   * @param height
   *          the height of the rectangle to intersect the clip with
   * @see #setClip(int, int, int, int)
   * @see #setClip(Shape)
   */
  public void clipRect(int x, int y, int width, int height) {
    clip(new Rectangle(x, y, width, height));
  }

  /**
   * Sets the current clip to the rectangle specified by the given coordinates.
   * This method sets the user clip, which is independent of the clipping
   * associated with device bounds and window visibility. Rendering operations
   * have no effect outside of the clipping area.
   * 
   * @param x
   *          the <i>x</i> coordinate of the new clip rectangle.
   * @param y
   *          the <i>y</i> coordinate of the new clip rectangle.
   * @param width
   *          the width of the new clip rectangle.
   * @param height
   *          the height of the new clip rectangle.
   * @see java.awt.Graphics#clipRect
   * @see java.awt.Graphics#setClip(Shape)
   * @since JDK1.1
   */
  public void setClip(int x, int y, int width, int height) {
    setClip(new Rectangle(x, y, width, height));
  }

  /**
   * Gets the current clipping area. This method returns the user clip, which is
   * independent of the clipping associated with device bounds and window
   * visibility. If no clip has previously been set, or if the clip has been
   * cleared using <code>setClip(null)</code>, this method returns
   * <code>null</code>.
   * 
   * @return a <code>Shape</code> object representing the current clipping
   *         area, or <code>null</code> if no clip is set.
   * @see java.awt.Graphics#getClipBounds()
   * @see java.awt.Graphics#clipRect
   * @see java.awt.Graphics#setClip(int, int, int, int)
   * @see java.awt.Graphics#setClip(Shape)
   * @since JDK1.1
   */
  public Shape getClip() {
    try {
      return transform.createInverse().createTransformedShape(clip);
    } catch (NoninvertibleTransformException e) {
      return null;
    }
  }

  /**
   * Sets the current clipping area to an arbitrary clip shape. Not all objects
   * that implement the <code>Shape</code> interface can be used to set the
   * clip. The only <code>Shape</code> objects that are guaranteed to be
   * supported are <code>Shape</code> objects that are obtained via the
   * <code>getClip</code> method and via <code>Rectangle</code> objects.
   * This method sets the user clip, which is independent of the clipping
   * associated with device bounds and window visibility.
   * 
   * @param clip
   *          the <code>Shape</code> to use to set the clip
   * @see java.awt.Graphics#getClip()
   * @see java.awt.Graphics#clipRect
   * @see java.awt.Graphics#setClip(int, int, int, int)
   * @since JDK1.1
   */
  public void setClip(Shape clip) {
    if (clip != null)
      this.clip = transform.createTransformedShape(clip);
    else
      this.clip = null;
  }

  /**
   * Sets the <code>Composite</code> for the <code>Graphics2D</code>
   * context. The <code>Composite</code> is used in all drawing methods such
   * as <code>drawImage</code>, <code>drawString</code>, <code>draw</code>,
   * and <code>fill</code>. It specifies how new pixels are to be combined
   * with the existing pixels on the graphics device during the rendering
   * process.
   * <p>
   * If this <code>Graphics2D</code> context is drawing to a
   * <code>Component</code> on the display screen and the
   * <code>Composite</code> is a custom object rather than an instance of the
   * <code>AlphaComposite</code> class, and if there is a security manager,
   * its <code>checkPermission</code> method is called with an
   * <code>AWTPermission("readDisplayPixels")</code> permission.
   * 
   * @param comp
   *          the <code>Composite</code> object to be used for rendering
   * @throws SecurityException
   *           if a custom <code>Composite</code> object is being used to
   *           render to the screen and a security manager is set and its
   *           <code>checkPermission</code> method does not allow the
   *           operation.
   * @see java.awt.Graphics#setXORMode
   * @see java.awt.Graphics#setPaintMode
   * @see java.awt.AlphaComposite
   */
  public void setComposite(Composite comp) {
    this.composite = comp;
  }

  /**
   * Sets the <code>Paint</code> attribute for the <code>Graphics2D</code>
   * context. Calling this method with a <code>null</code> <code>Paint</code>
   * object does not have any effect on the current <code>Paint</code>
   * attribute of this <code>Graphics2D</code>.
   * 
   * @param paint
   *          the <code>Paint</code> object to be used to generate color
   *          during the rendering process, or <code>null</code>
   * @see java.awt.Graphics#setColor
   * @see java.awt.GradientPaint
   * @see java.awt.TexturePaint
   */
  public void setPaint(Paint paint) {
    if (paint == null)
      return;

    this.paint = paint;
    if (paint instanceof Color)
      foreground = (Color) paint;
  }

  /**
   * Sets the <code>Stroke</code> for the <code>Graphics2D</code> context.
   * 
   * @param s
   *          the <code>Stroke</code> object to be used to stroke a
   *          <code>Shape</code> during the rendering process
   * @see BasicStroke
   */
  public void setStroke(Stroke s) {
    stroke = s;
  }

  /**
   * Sets the value of a single preference for the rendering algorithms. Hint
   * categories include controls for rendering quality and overall time/quality
   * trade-off in the rendering process. Refer to the
   * <code>RenderingHints</code> class for definitions of some common keys and
   * values.
   * 
   * @param hintKey
   *          the key of the hint to be set.
   * @param hintValue
   *          the value indicating preferences for the specified hint category.
   * @see RenderingHints
   */
  public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
    hints.put(hintKey, hintValue);
  }

  /**
   * Returns the value of a single preference for the rendering algorithms. Hint
   * categories include controls for rendering quality and overall time/quality
   * trade-off in the rendering process. Refer to the
   * <code>RenderingHints</code> class for definitions of some common keys and
   * values.
   * 
   * @param hintKey
   *          the key corresponding to the hint to get.
   * @return an object representing the value for the specified hint key. Some
   *         of the keys and their associated values are defined in the
   *         <code>RenderingHints</code> class.
   * @see RenderingHints
   */
  public Object getRenderingHint(RenderingHints.Key hintKey) {
    return hints.get(hintKey);
  }

  /**
   * Replaces the values of all preferences for the rendering algorithms with
   * the specified <code>hints</code>. The existing values for all rendering
   * hints are discarded and the new set of known hints and values are
   * initialized from the specified {@link Map} object. Hint categories include
   * controls for rendering quality and overall time/quality trade-off in the
   * rendering process. Refer to the <code>RenderingHints</code> class for
   * definitions of some common keys and values.
   * 
   * @param hints
   *          the rendering hints to be set
   * @see RenderingHints
   */
  public void setRenderingHints(Map hints) {
    this.hints = new RenderingHints(hints);
  }

  /**
   * Sets the values of an arbitrary number of preferences for the rendering
   * algorithms. Only values for the rendering hints that are present in the
   * specified <code>Map</code> object are modified. All other preferences not
   * present in the specified object are left unmodified. Hint categories
   * include controls for rendering quality and overall time/quality trade-off
   * in the rendering process. Refer to the <code>RenderingHints</code> class
   * for definitions of some common keys and values.
   * 
   * @param hints
   *          the rendering hints to be set
   * @see RenderingHints
   */
  public void addRenderingHints(Map hints) {
    this.hints.putAll(hints);
  }

  /**
   * Gets the preferences for the rendering algorithms. Hint categories include
   * controls for rendering quality and overall time/quality trade-off in the
   * rendering process. Returns all of the hint key/value pairs that were ever
   * specified in one operation. Refer to the <code>RenderingHints</code>
   * class for definitions of some common keys and values.
   * 
   * @return a reference to an instance of <code>RenderingHints</code> that
   *         contains the current preferences.
   * @see RenderingHints
   */
  public RenderingHints getRenderingHints() {
    return hints;
  }

  /**
   * Translates the origin of the graphics context to the point (<i>x</i>,&nbsp;<i>y</i>)
   * in the current coordinate system. Modifies this graphics context so that
   * its new origin corresponds to the point (<i>x</i>,&nbsp;<i>y</i>) in
   * this graphics context's original coordinate system. All coordinates used in
   * subsequent rendering operations on this graphics context will be relative
   * to this new origin.
   * 
   * @param x
   *          the <i>x</i> coordinate.
   * @param y
   *          the <i>y</i> coordinate.
   */
  public void translate(int x, int y) {
    if (x != 0 || y != 0) {
      transform.translate(x, y);
      transformStack.add(TransformStackElement.createTranslateElement(x, y));
    }
  }

  /**
   * Concatenates the current <code>Graphics2D</code> <code>Transform</code>
   * with a translation transform. Subsequent rendering is translated by the
   * specified distance relative to the previous position. This is equivalent to
   * calling transform(T), where T is an <code>AffineTransform</code>
   * represented by the following matrix:
   * 
   * <pre>
   *           [   1    0    tx  ]
   *           [   0    1    ty  ]
   *           [   0    0    1   ]
   * </pre>
   * 
   * @param tx
   *          the distance to translate along the x-axis
   * @param ty
   *          the distance to translate along the y-axis
   */
  public void translate(double tx, double ty) {
    transform.translate(tx, ty);
    transformStack.add(TransformStackElement.createTranslateElement(tx, ty));
  }

  /**
   * Concatenates the current <code>Graphics2D</code>
   * <code>Transform</code>
   * with a rotation transform. Subsequent rendering is rotated by the specified
   * radians relative to the previous origin. This is equivalent to calling
   * <code>transform(R)</code>, where R is an <code>AffineTransform</code>
   * represented by the following matrix:
   * 
   * <pre>
   *           [   cos(theta)    -sin(theta)    0   ]
   *           [   sin(theta)     cos(theta)    0   ]
   *           [       0              0         1   ]
   * </pre>
   * 
   * Rotating with a positive angle theta rotates points on the positive x axis
   * toward the positive y axis.
   * 
   * @param theta
   *          the angle of rotation in radians
   */
  public void rotate(double theta) {
    transform.rotate(theta);
    transformStack.add(TransformStackElement.createRotateElement(theta));
  }

  /**
   * Concatenates the current <code>Graphics2D</code>
   * <code>Transform</code>
   * with a translated rotation transform. Subsequent rendering is transformed
   * by a transform which is constructed by translating to the specified
   * location, rotating by the specified radians, and translating back by the
   * same amount as the original translation. This is equivalent to the
   * following sequence of calls:
   * 
   * <pre>
   * translate(x, y);
   * rotate(theta);
   * translate(-x, -y);
   * </pre>
   * 
   * Rotating with a positive angle theta rotates points on the positive x axis
   * toward the positive y axis.
   * 
   * @param theta
   *          the angle of rotation in radians
   * @param x
   *          x coordinate of the origin of the rotation
   * @param y
   *          y coordinate of the origin of the rotation
   */
  public void rotate(double theta, double x, double y) {
    transform.rotate(theta, x, y);
    transformStack.add(TransformStackElement.createTranslateElement(x, y));
    transformStack.add(TransformStackElement.createRotateElement(theta));
    transformStack.add(TransformStackElement.createTranslateElement(-x, -y));
  }

  /**
   * Concatenates the current <code>Graphics2D</code>
   * <code>Transform</code>
   * with a scaling transformation Subsequent rendering is resized according to
   * the specified scaling factors relative to the previous scaling. This is
   * equivalent to calling <code>transform(S)</code>, where S is an
   * <code>AffineTransform</code> represented by the following matrix:
   * 
   * <pre>
   *           [   sx   0    0   ]
   *           [   0    sy   0   ]
   *           [   0    0    1   ]
   * </pre>
   * 
   * @param sx
   *          the amount by which X coordinates in subsequent rendering
   *          operations are multiplied relative to previous rendering
   *          operations.
   * @param sy
   *          the amount by which Y coordinates in subsequent rendering
   *          operations are multiplied relative to previous rendering
   *          operations.
   */
  public void scale(double sx, double sy) {
    transform.scale(sx, sy);
    transformStack.add(TransformStackElement.createScaleElement(sx, sy));
  }

  /**
   * Concatenates the current <code>Graphics2D</code>
   * <code>Transform</code>
   * with a shearing transform. Subsequent renderings are sheared by the
   * specified multiplier relative to the previous position. This is equivalent
   * to calling <code>transform(SH)</code>, where SH is an
   * <code>AffineTransform</code> represented by the following matrix:
   * 
   * <pre>
   *           [   1   shx   0   ]
   *           [  shy   1    0   ]
   *           [   0    0    1   ]
   * </pre>
   * 
   * @param shx
   *          the multiplier by which coordinates are shifted in the positive X
   *          axis direction as a function of their Y coordinate
   * @param shy
   *          the multiplier by which coordinates are shifted in the positive Y
   *          axis direction as a function of their X coordinate
   */
  public void shear(double shx, double shy) {
    transform.shear(shx, shy);
    transformStack.add(TransformStackElement.createShearElement(shx, shy));
  }

  /**
   * Composes an <code>AffineTransform</code> object with the
   * <code>Transform</code> in this <code>Graphics2D</code> according to the
   * rule last-specified-first-applied. If the current <code>Transform</code>
   * is Cx, the result of composition with Tx is a new <code>Transform</code>
   * Cx'. Cx' becomes the current <code>Transform</code> for this
   * <code>Graphics2D</code>. Transforming a point p by the updated
   * <code>Transform</code> Cx' is equivalent to first transforming p by Tx
   * and then transforming the result by the original <code>Transform</code>
   * Cx. In other words, Cx'(p) = Cx(Tx(p)). A copy of the Tx is made, if
   * necessary, so further modifications to Tx do not affect rendering.
   * 
   * @param Tx
   *          the <code>AffineTransform</code> object to be composed with the
   *          current <code>Transform</code>
   * @see #setTransform
   * @see AffineTransform
   */
  public void transform(AffineTransform Tx) {
    transform.concatenate(Tx);
    transformStack.add(TransformStackElement.createGeneralTransformElement(Tx));
  }

  /**
   * Sets the <code>Transform</code> in the <code>Graphics2D</code> context.
   * 
   * @param Tx
   *          the <code>AffineTransform</code> object to be used in the
   *          rendering process
   * @see #transform
   * @see AffineTransform
   */
  public void setTransform(AffineTransform Tx) {
    transform = new AffineTransform(Tx);
    invalidateTransformStack();
    if (!Tx.isIdentity())
      transformStack.add(TransformStackElement.createGeneralTransformElement(Tx));
  }

  /**
   * Marks the GraphicContext's isNewTransformStack to false as a memento that
   * the current transform stack was read and has not been reset. Only the
   * setTransform method can override this memento.
   */
  public void validateTransformStack() {
    transformStackValid = true;
  }

  /**
   * Checks the status of the transform stack
   */
  public boolean isTransformStackValid() {
    return transformStackValid;
  }

  /**
   * @return array containing the successive transforms that were concatenated
   *         with the original one.
   */
  public TransformStackElement[] getTransformStack() {
    TransformStackElement[] stack = new TransformStackElement[transformStack.size()];
    transformStack.toArray(stack);
    return stack;
  }

  /**
   * Marks the GraphicContext's isNewTransformStack to true as a memento that
   * the current transform stack was reset since it was last read. Only
   * validateTransformStack can override this memento
   */
  protected void invalidateTransformStack() {
    transformStack.clear();
    transformStackValid = false;
  }

  /**
   * Returns a copy of the current <code>Transform</code> in the
   * <code>Graphics2D</code> context.
   * 
   * @return the current <code>AffineTransform</code> in the
   *         <code>Graphics2D</code> context.
   * @see #transform
   * @see #setTransform
   */
  public AffineTransform getTransform() {
    return new AffineTransform(transform);
  }

  /**
   * Returns the current <code>Paint</code> of the <code>Graphics2D</code>
   * context.
   * 
   * @return the current <code>Graphics2D</code> <code>Paint</code>, which
   *         defines a color or pattern.
   * @see #setPaint
   * @see java.awt.Graphics#setColor
   */
  public Paint getPaint() {
    return paint;
  }

  /**
   * Returns the current <code>Composite</code> in the <code>Graphics2D</code>
   * context.
   * 
   * @return the current <code>Graphics2D</code> <code>Composite</code>,
   *         which defines a compositing style.
   * @see #setComposite
   */
  public Composite getComposite() {
    return composite;
  }

  /**
   * Sets the background color for the <code>Graphics2D</code> context. The
   * background color is used for clearing a region. When a
   * <code>Graphics2D</code> is constructed for a <code>Component</code>,
   * the background color is inherited from the <code>Component</code>.
   * Setting the background color in the <code>Graphics2D</code> context only
   * affects the subsequent <code>clearRect</code> calls and not the
   * background color of the <code>Component</code>. To change the background
   * of the <code>Component</code>, use appropriate methods of the
   * <code>Component</code>.
   * 
   * @param color
   *          the background color that isused in subsequent calls to
   *          <code>clearRect</code>
   * @see #getBackground
   * @see java.awt.Graphics#clearRect
   */
  public void setBackground(Color color) {
    if (color == null)
      return;

    background = color;
  }

  /**
   * Returns the background color used for clearing a region.
   * 
   * @return the current <code>Graphics2D</code> <code>Color</code>, which
   *         defines the background color.
   * @see #setBackground
   */
  public Color getBackground() {
    return background;
  }

  /**
   * Returns the current <code>Stroke</code> in the <code>Graphics2D</code>
   * context.
   * 
   * @return the current <code>Graphics2D</code> <code>Stroke</code>, which
   *         defines the line style.
   * @see #setStroke
   */
  public Stroke getStroke() {
    return stroke;
  }

  /**
   * Intersects the current <code>Clip</code> with the interior of the
   * specified <code>Shape</code> and sets the <code>Clip</code> to the
   * resulting intersection. The specified <code>Shape</code> is transformed
   * with the current <code>Graphics2D</code>
   * <code>Transform</code> before
   * being intersected with the current <code>Clip</code>. This method is
   * used to make the current <code>Clip</code> smaller. To make the
   * <code>Clip</code> larger, use <code>setClip</code>. The <i>user clip</i>
   * modified by this method is independent of the clipping associated with
   * device bounds and visibility. If no clip has previously been set, or if the
   * clip has been cleared using
   * {@link java.awt.Graphics#setClip(Shape) setClip} with a <code>null</code>
   * argument, the specified <code>Shape</code> becomes the new user clip.
   * 
   * @param s
   *          the <code>Shape</code> to be intersected with the current
   *          <code>Clip</code>. If <code>s</code> is <code>null</code>,
   *          this method clears the current <code>Clip</code>.
   */
  public void clip(Shape s) {
    if (s != null)
      s = transform.createTransformedShape(s);

    if (clip != null) {
      Area newClip = new Area(clip);
      newClip.intersect(new Area(s));
      clip = new GeneralPath(newClip);
    } else {
      clip = s;
    }
  }

  /**
   * Get the rendering context of the <code>Font</code> within this
   * <code>Graphics2D</code> context. The {@link FontRenderContext}
   * encapsulates application hints such as anti-aliasing and fractional
   * metrics, as well as target device specific information such as
   * dots-per-inch. This information should be provided by the application when
   * using objects that perform typographical formatting, such as
   * <code>Font</code> and <code>TextLayout</code>. This information should
   * also be provided by applications that perform their own layout and need
   * accurate measurements of various characteristics of glyphs such as advance
   * and line height when various rendering hints have been applied to the text
   * rendering.
   * 
   * @return a reference to an instance of FontRenderContext.
   * @see java.awt.font.FontRenderContext
   * @see java.awt.Font#createGlyphVector(FontRenderContext,char[])
   * @see java.awt.font.TextLayout
   * @since JDK1.2
   */
  public FontRenderContext getFontRenderContext() {
    //
    // Find if antialiasing should be used.
    //
    Object antialiasingHint = hints.get(RenderingHints.KEY_TEXT_ANTIALIASING);
    boolean isAntialiased = true;
    if (antialiasingHint != RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        && antialiasingHint != RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT) {

      // If antialias was not turned off, then use the general rendering
      // hint.
      if (antialiasingHint != RenderingHints.VALUE_TEXT_ANTIALIAS_OFF) {
        antialiasingHint = hints.get(RenderingHints.KEY_ANTIALIASING);

        // Test general hint
        if (antialiasingHint != RenderingHints.VALUE_ANTIALIAS_ON
            && antialiasingHint != RenderingHints.VALUE_ANTIALIAS_DEFAULT) {
          // Antialiasing was not requested. However, if it was not turned
          // off explicitly, use it.
          if (antialiasingHint == RenderingHints.VALUE_ANTIALIAS_OFF)
            isAntialiased = false;
        }
      } else
        isAntialiased = false;

    }

    //
    // Find out whether fractional metrics should be used.
    //
    boolean useFractionalMetrics = true;
    if (hints.get(RenderingHints.KEY_FRACTIONALMETRICS) == RenderingHints.VALUE_FRACTIONALMETRICS_OFF)
      useFractionalMetrics = false;

    FontRenderContext frc = new FontRenderContext(defaultTransform, isAntialiased,
        useFractionalMetrics);
    return frc;
  }
}

/*
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */

/**
 * Contains a description of an elementary transform stack element, such as a
 * rotate or translate. A transform stack element has a type and a value, which
 * is an array of double values.<br>
 * 
 * @author <a href="mailto:vincent.hardy@eng.sun.com">Vincent Hardy</a>
 * @author <a href="mailto:paul_evenblij@compuware.com">Paul Evenblij</a>
 * @version $Id: TransformStackElement.java 478249 2006-11-22 17:29:37Z dvholten $
 */
abstract class TransformStackElement implements Cloneable {

  /**
   * Transform type
   */
  private TransformType type;

  /**
   * Value
   */
  private double[] transformParameters;

  /**
   * @param type
   *          transform type
   * @param transformParameters
   *          parameters for transform
   */
  protected TransformStackElement(TransformType type, double[] transformParameters) {
    this.type = type;
    this.transformParameters = transformParameters;
  }

  /**
   * @return an object which is a deep copy of this one
   */
  public Object clone() {
    TransformStackElement newElement = null;

    // start with a shallow copy to get our implementations right
    try {
      newElement = (TransformStackElement) super.clone();
    } catch (java.lang.CloneNotSupportedException ex) {
    }

    // now deep copy the parameter array
    double[] transformParameters = new double[this.transformParameters.length];
    System.arraycopy(this.transformParameters, 0, transformParameters, 0,
        transformParameters.length);
    newElement.transformParameters = transformParameters;
    return newElement;
  }

  /*
   * Factory methods
   */

  public static TransformStackElement createTranslateElement(double tx, double ty) {
    return new TransformStackElement(TransformType.TRANSLATE, new double[] { tx, ty }) {
      boolean isIdentity(double[] parameters) {
        return parameters[0] == 0 && parameters[1] == 0;
      }
    };
  }

  public static TransformStackElement createRotateElement(double theta) {
    return new TransformStackElement(TransformType.ROTATE, new double[] { theta }) {
      boolean isIdentity(double[] parameters) {
        return Math.cos(parameters[0]) == 1;
      }
    };
  }

  public static TransformStackElement createScaleElement(double scaleX, double scaleY) {
    return new TransformStackElement(TransformType.SCALE, new double[] { scaleX, scaleY }) {
      boolean isIdentity(double[] parameters) {
        return parameters[0] == 1 && parameters[1] == 1;
      }
    };
  }

  public static TransformStackElement createShearElement(double shearX, double shearY) {
    return new TransformStackElement(TransformType.SHEAR, new double[] { shearX, shearY }) {
      boolean isIdentity(double[] parameters) {
        return parameters[0] == 0 && parameters[1] == 0;
      }
    };
  }

  public static TransformStackElement createGeneralTransformElement(AffineTransform txf) {
    double[] matrix = new double[6];
    txf.getMatrix(matrix);
    return new TransformStackElement(TransformType.GENERAL, matrix) {
      boolean isIdentity(double[] m) {
        return (m[0] == 1 && m[2] == 0 && m[4] == 0 && m[1] == 0 && m[3] == 1 && m[5] == 0);
      }
    };
  }

  /**
   * Implementation should determine if the parameter list represents an
   * identity transform, for the instance transform type.
   */
  abstract boolean isIdentity(double[] parameters);

  /**
   * @return true iff this transform is the identity transform
   */
  public boolean isIdentity() {
    return isIdentity(transformParameters);
  }

  /**
   * @return array of values containing this transform element's parameters
   */
  public double[] getTransformParameters() {
    return transformParameters;
  }

  /**
   * @return this transform type
   */
  public TransformType getType() {
    return type;
  }

  /*
   * Concatenation utility. Requests this transform stack element to concatenate
   * with the input stack element. Only elements of the same types are
   * concatenated. For example, if this element represents a translation, it
   * will concatenate with another translation, but not with any other kind of
   * stack element. @param stackElement element to be concatenated with this
   * one. @return true if the input stackElement was concatenated with this one.
   * False otherwise.
   */
  public boolean concatenate(TransformStackElement stackElement) {
    boolean canConcatenate = false;

    if (type.toInt() == stackElement.type.toInt()) {
      canConcatenate = true;
      switch (type.toInt()) {
      case TransformType.TRANSFORM_TRANSLATE:
        transformParameters[0] += stackElement.transformParameters[0];
        transformParameters[1] += stackElement.transformParameters[1];
        break;
      case TransformType.TRANSFORM_ROTATE:
        transformParameters[0] += stackElement.transformParameters[0];
        break;
      case TransformType.TRANSFORM_SCALE:
        transformParameters[0] *= stackElement.transformParameters[0];
        transformParameters[1] *= stackElement.transformParameters[1];
        break;
      case TransformType.TRANSFORM_GENERAL:
        transformParameters = matrixMultiply(transformParameters, stackElement.transformParameters);
        break;
      default:
        canConcatenate = false;
      }
    }

    return canConcatenate;
  }

  /**
   * Multiplies two 2x3 matrices of double precision values
   */
  private double[] matrixMultiply(double[] matrix1, double[] matrix2) {
    double[] product = new double[6];
    AffineTransform transform1 = new AffineTransform(matrix1);
    transform1.concatenate(new AffineTransform(matrix2));
    transform1.getMatrix(product);
    return product;
  }

}

/*
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */

/**
 * Enumeration for transformation types.
 * 
 * @author <a href="mailto:vincent.hardy@eng.sun.com">Vincent Hardy</a>
 * @version $Id: TransformType.java 504084 2007-02-06 11:24:46Z dvholten $
 */
class TransformType {
  /*
   * Transform type constants
   */
  public static final int TRANSFORM_TRANSLATE = 0;

  public static final int TRANSFORM_ROTATE = 1;

  public static final int TRANSFORM_SCALE = 2;

  public static final int TRANSFORM_SHEAR = 3;

  public static final int TRANSFORM_GENERAL = 4;

  /**
   * Strings describing the elementary transforms
   */
  public static final String TRANSLATE_STRING = "translate";

  public static final String ROTATE_STRING = "rotate";

  public static final String SCALE_STRING = "scale";

  public static final String SHEAR_STRING = "shear";

  public static final String GENERAL_STRING = "general";

  /**
   * TransformType values
   */
  public static final TransformType TRANSLATE = new TransformType(TRANSFORM_TRANSLATE,
      TRANSLATE_STRING);

  public static final TransformType ROTATE = new TransformType(TRANSFORM_ROTATE, ROTATE_STRING);

  public static final TransformType SCALE = new TransformType(TRANSFORM_SCALE, SCALE_STRING);

  public static final TransformType SHEAR = new TransformType(TRANSFORM_SHEAR, SHEAR_STRING);

  public static final TransformType GENERAL = new TransformType(TRANSFORM_GENERAL, GENERAL_STRING);

  private String desc;

  private int val;

  /**
   * Constructor is private so that no instances other than the ones in the
   * enumeration can be created.
   * 
   * @see #readResolve
   */
  private TransformType(int val, String desc) {
    this.desc = desc;
    this.val = val;
  }

  /**
   * @return description
   */
  public String toString() {
    return desc;
  }

  /**
   * Convenience for enumeration switching. That is,
   * 
   * <pre>
   *    switch(transformType.toInt()){
   *        case TransformType.TRANSFORM_TRANSLATE:
   *         ....
   *        case TransformType.TRANSFORM_ROTATE:
   * </pre>
   */
  public int toInt() {
    return val;
  }

  /**
   * This is called by the serialization code before it returns an unserialized
   * object. To provide for unicity of instances, the instance that was read is
   * replaced by its static equivalent
   */
  public Object readResolve() {
    switch (val) {
    case TRANSFORM_TRANSLATE:
      return TransformType.TRANSLATE;
    case TRANSFORM_ROTATE:
      return TransformType.ROTATE;
    case TRANSFORM_SCALE:
      return TransformType.SCALE;
    case TRANSFORM_SHEAR:
      return TransformType.SHEAR;
    case TRANSFORM_GENERAL:
      return TransformType.GENERAL;
    default:
      throw new Error("Unknown TransformType value:" + val);
    }
  }
}
