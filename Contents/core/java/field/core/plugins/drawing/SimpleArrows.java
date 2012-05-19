package field.core.plugins.drawing;

import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.Cursor;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.math.linalg.Vector2;

/**
 * we used to have an SVG based arrow class, but that brought in all of batik to
 * our classpath
 * 
 * @author marc
 * 
 */
public class SimpleArrows {

	public CachedLine triangleAt(Vector2 v, Vector2 backward, float width) {
		CachedLine c = new CachedLine();
		c.getInput().moveTo(v.x, v.y);

		c.getInput().lineTo(v.x - backward.x - backward.y * width, v.y - backward.y + backward.x * width);
		c.getInput().lineTo(v.x - backward.x + backward.y * width, v.y - backward.y - backward.x * width);
		c.getInput().lineTo(v.x, v.y);

		c.getProperties().put(iLinearGraphicsContext.derived, 1f);

		return c;
	}

	public CachedLine arrowForMiddle(CachedLine c, float size, float width) {
		return arrowFor(c, 0.5f, size, width);
	}

	public CachedLine arrowFor(CachedLine c, float along, float size, float width) {
		Cursor cc = new Cursor(c);
		float len = cc.length();
		cc.forwardD(len * along);

		Vector2 p = cc.position();
		Vector2 t = cc.tangentForward();

		if (t == null || t.mag() == 0 || t.isNaN() || along == 1)
			t = cc.tangentBackward();

		Vector2 n = t.normalize();
		n.scale(size);

		return triangleAt(p, n, width);
	}

	public CachedLine arrowForEnd(CachedLine c, float distanceBack, float size, float width) {
		Cursor cc = new Cursor(c).end();
		cc.forwardD(-distanceBack);

		Vector2 p = cc.position();
		Vector2 t = cc.tangentForward();

		if (t == null || t.mag() == 0 || t.isNaN())
			t = cc.tangentBackward();

		Vector2 n = t.normalize();
		n.scale(size);

		return triangleAt(p, n, width);
	}

	public CachedLine circleFor(CachedLine c, float along, float size) {
		Cursor cc = new Cursor(c);
		float len = cc.length();
		cc.forwardD(len * along);

		Vector2 p = cc.position();

		return circleAt(p, size);
	}

	public CachedLine circleAt(Vector2 center, float r) {
		float k = (float) (4 * (Math.sqrt(2) - 1) / 3);

		CachedLine c = new CachedLine();
		c.getInput().moveTo(0, 1);

		c.getInput().cubicTo(k, 1, 1, k, 1, 0);
		c.getInput().cubicTo(1, -k, k, -1, 0, -1);
		c.getInput().cubicTo(-k, -1, -1, -k, -1, 0);
		c.getInput().cubicTo(-1, k, -k, 1, 0, 1);

		c.getProperties().put(iLinearGraphicsContext.derived, 1f);

		return LineUtils.transformLine(c, null, new Vector2(r,r), null, center);

	}

}
