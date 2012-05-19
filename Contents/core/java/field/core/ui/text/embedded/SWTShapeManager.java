package field.core.ui.text.embedded;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import org.eclipse.swt.graphics.Rectangle;

/**
 * SWT shape manager.
 *
 * @author Lance Good
 */
public class SWTShapeManager {
    private static AffineTransform IDENTITY_XFORM = new AffineTransform();
    private static Point2D aPoint = new Point2D.Double();
    private static ArrayList segList = new ArrayList();
    private static double[] pts = new double[8];

    /**
     * Apply the specified transform to the specified rectangle, modifying the
     * rect.
     *
     * @param rect The rectangle to be transformed
     * @param at The transform to use to transform the rectangle
     */
    public static void transform(final Rectangle2D rect, final AffineTransform at) {
        // First, transform all 4 corners of the rectangle
        pts[0] = rect.getX(); // top left corner
        pts[1] = rect.getY();
        pts[2] = rect.getX() + rect.getWidth(); // top right corner
        pts[3] = rect.getY();
        pts[4] = rect.getX() + rect.getWidth(); // bottom right corner
        pts[5] = rect.getY() + rect.getHeight();
        pts[6] = rect.getX(); // bottom left corner
        pts[7] = rect.getY() + rect.getHeight();
        at.transform(pts, 0, pts, 0, 4);

        // Then, find the bounds of those 4 transformed points.
        double minX = pts[0];
        double minY = pts[1];
        double maxX = pts[0];
        double maxY = pts[1];
        int i;
        for (i = 1; i < 4; i++) {
            if (pts[2 * i] < minX) {
                minX = pts[2 * i];
            }
            if (pts[2 * i + 1] < minY) {
                minY = pts[2 * i + 1];
            }
            if (pts[2 * i] > maxX) {
                maxX = pts[2 * i];
            }
            if (pts[2 * i + 1] > maxY) {
                maxY = pts[2 * i + 1];
            }
        }
        rect.setRect(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Populates the SWT rectangle with the provided Swing Rectangle2D's
     * coordinates. Rounding up to the nearest integer.
     *
     * @param aRect awt rectangle to extract coordinates from
     * @param sRect swt rectangle to populate
     */
    public static void awtToSWT(final Rectangle2D aRect, final Rectangle sRect) {
        sRect.x = (int) (aRect.getX() + 0.5);
        sRect.y = (int) (aRect.getY() + 0.5);
        sRect.width = (int) (aRect.getWidth() + 0.5);
        sRect.height = (int) (aRect.getHeight() + 0.5);
    }

    /**
     * Converts the provided shape into an array of point coordinates given as
     * one dimensional array with this format: x1,y1,x2,y3,....
     *
     * @param shape shape to convert
     * @return point coordinates given as one dimensional array with this
     *         format: x1,y1,x2,y3,...
     */
    public static double[] shapeToPolyline(final Shape shape) {
        segList.clear();
        aPoint.setLocation(0, 0);

        final PathIterator pi = shape.getPathIterator(IDENTITY_XFORM, 0.000000001);
        while (!pi.isDone()) {
            final int segType = pi.currentSegment(pts);
            switch (segType) {
                case PathIterator.SEG_MOVETO:
                    aPoint.setLocation(pts[0], pts[1]);
                    segList.add(new Point2D.Double(pts[0], pts[1]));
                    break;
                case PathIterator.SEG_LINETO:
                    segList.add(new Point2D.Double(pts[0], pts[1]));
                    break;
                case PathIterator.SEG_CLOSE:
                    segList.add(new Point2D.Double(aPoint.getX(), aPoint.getY()));
                    break;
                default:
            }
            pi.next();
        }

        final double[] polyObj = new double[2 * segList.size()];
        for (int i = 0; i < segList.size(); i++) {
            final Point2D p2 = (Point2D) segList.get(i);
            polyObj[2 * i] = (int) (p2.getX() + 0.5);
            polyObj[2 * i + 1] = (int) (p2.getY() + 0.5);
        }

        return polyObj;
    }

    /**
     * Transforms the given points by the transform provided, leaving the
     * original points untouched.
     *
     * @param points points to transform
     * @param at transform to apply
     * @return transformed coordinates given in format x1,y2,x2,y2,...
     */
    public static int[] transform(final double[] points, final AffineTransform at) {
        final int[] intPts = new int[points.length];
        for (int i = 0; i < points.length / 2; i++) {
            aPoint.setLocation(points[2 * i], points[2 * i + 1]);
            at.transform(aPoint, aPoint);
            intPts[2 * i] = (int) (aPoint.getX() + 0.5);
            intPts[2 * i + 1] = (int) (aPoint.getY() + 0.5);
        }
        return intPts;
    }
}