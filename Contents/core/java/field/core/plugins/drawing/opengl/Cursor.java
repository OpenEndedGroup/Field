package field.core.plugins.drawing.opengl;

import java.util.ArrayList;
import java.util.List;

import field.bytecode.protect.annotations.HiddenInAutocomplete;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.plugins.drawing.opengl.LineUtils.AllDistanceMinima;
import field.core.plugins.drawing.opengl.LineUtils.AllDistanceMinima.Minimum;
import field.core.plugins.drawing.opengl.LineUtils.ClosestPointToSpline;
import field.core.plugins.drawing.opengl.LineUtils.ClosestPointToSpline3;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Pair;
import field.util.Dict.Prop;

/**
 * wraps a CachedLineCursor to provide the ability to move around the line to
 * points that are not at control nodes
 */
public class Cursor {

	private static final Prop<PathFlattener3> flattener = new Prop<PathFlattener3>("flattener");

	float t;
	private CachedLineCursor cursor;
	LineUtils u = new LineUtils();
	private final CachedLine line;

	public Cursor(CachedLine line) {
		this.line = line;
		cursor = new CachedLineCursor(line, 0);
	}

	public Cursor(Cursor copy) {
		this(copy.line, copy.currentT());
	}

	public Cursor(CachedLine line, float dot) {
		cursor = new CachedLineCursor(line, (int) dot);
		t = dot - (int) dot;
		this.line = line;
	}

	public Cursor(CachedLine.Event event) {
		cursor = new CachedLineCursor(event.getContainer(), event.getContainer().events.indexOf(event));
		this.t = 0;
		this.line = event.getContainer();
	}

	protected PathFlattener3 getFlattener() {
		PathFlattener3 f = line.getProperties().get(flattener);
		if (f == null) {
			line.getProperties().put(flattener, f = new PathFlattener3(line, 0.05f));
		}
		return f;
	}

	protected void deleteFlattener() {
		line.getProperties().remove(flattener);
	}

	/**
	 * Move to the start of the line.
	 */
	public Cursor start() {
		cursor = new CachedLineCursor(line, 0);
		return this;
	}

	/**
	 * Move to the end of the line.
	 */
	public Cursor end() {
		cursor = new CachedLineCursor(line, line.events.size() - 1);
		return this;
	}

	/**
	 * Returns the 'current' node that this Cursor is near.
	 * 
	 * This can be then used to change the position (and control points) of
	 * this node, or change the properties associated with this node.
	 */
	public Event node() {
		return cursor.getCurrent();
	}

	/**
	 * Return the total length of this line
	 */
	public float length() {
		return getFlattener().length();
	}

	/**
	 * Return the number of nodes in this line
	 */
	public float lengthT() {
		return line.events.size();
	}

	/**
	 * Return the current position of this cursor as a distance along the
	 * line
	 */
	public float currentD() {
		return getFlattener().dotToLength(currentT());
	}

	/**
	 * Return the current position of this cursor in node.fraction notation.
	 * 
	 * For example, 3.25 is 25% of the way between node 3 and node 4. (Note,
	 * nodes start at 0)
	 */
	public float currentT() {
		return cursor.currentIndex + t;
	}

	/**
	 * Convert from node.fraction to distance along line
	 */
	public float convertTToD(float t) {
		return getFlattener().dotToLength(t);
	}

	/**
	 * Convert from distance along line to node.fraction
	 */
	public float convertDToT(float d) {
		return getFlattener().lengthToDot(d);
	}

	/**
	 * Move forward by a certain distance
	 */
	public Cursor forwardD(float distance) {
		float a = getFlattener().dotToLength(cursor.currentIndex + t);
		float d = getFlattener().lengthToDot(a + distance);

		cursor = new CachedLineCursor(line, (int) d);
		t = d - (int) d;

		return this;
	}

	/**
	 * Insert a node here
	 */
	public Cursor insert() {
		if (cursor.nextIsCubic()) {
			Vector2 a = new Vector2();
			Vector2 c1 = new Vector2();
			Vector2 c2 = new Vector2();
			Vector2 b = new Vector2();
			cursor.nextCubicFrame(a, c1, c2, b);

			Vector2 c12 = new Vector2();
			Vector2 c21 = new Vector2();
			Vector2 m = new Vector2();
			Vector2 tmp = new Vector2();

			LineUtils.splitCubicFrame(a, c1, c2, b, t, c12, m, c21, tmp);
			cursor.getAfter().setAt(0, c1);
			cursor.getAfter().setAt(1, c12);
			cursor.getAfter().setAt(2, m);

			Event inserted = line.new Event();
			inserted.method = iLine_m.cubicTo_m;
			inserted.args = new Object[6];
			inserted.setAt(0, c21);
			inserted.setAt(1, c2);
			inserted.setAt(2, b);
			line.events.add(cursor.currentIndex + 2, inserted);
		} else if (!cursor.nextIsSkip()) {

			Vector2 a = new Vector2();
			Vector2 b = new Vector2();
			cursor.nextLinearFrame(a, b);

			if (cursor.getAfter() != null) {
				cursor.getAfter().setAt(0, new Vector2(a).lerp(a, b, t));

				Event inserted = line.new Event();
				inserted.method = iLine_m.lineTo_m;
				inserted.args = new Object[2];
				inserted.setAt(0, b);
				line.events.add(cursor.currentIndex + 2, inserted);
			} else {
				Event e = line.events.get(cursor.currentIndex);
				Event inserted = line.new Event();
				inserted.method = iLine_m.lineTo_m;
				inserted.args = new Object[2];
				inserted.setAt(0, e.getDestination());
				line.events.add(cursor.currentIndex, inserted);
			}
		}

		cursor = new CachedLineCursor(line, this.cursor.currentIndex + 1);
		this.t = 0;

		deleteFlattener();

		return this;
	}

	public Cursor removeNode() {
		if (this.cursor.current.method == iLine_m.moveTo_m) {
			if (this.cursor.currentIndex == this.line.events.size() - 1) {
			} else {
				Event e = this.line.events.get(this.cursor.currentIndex + 1);
				Vector2 d = e.getDestination();
				e.method = iLine_m.moveTo_m;
				e.args = new Object[2];
				e.setAt(d);
			}
		} else if (this.cursor.current.method == iLine_m.lineTo_m) {
		} else if (this.cursor.current.method == iLine_m.cubicTo_m) {
		}

		this.line.events.remove(cursor.current);
		cursor = new CachedLineCursor(line, this.cursor.currentIndex + 1);
		this.t = 0;

		deleteFlattener();
		return this;
	}

	/**
	 * Splits a line here returning the line leading up to this
	 * 
	 * @return
	 */
	public CachedLine splitLeft() {
		CachedLine left = null;

		if (cursor.nextIsCubic()) {
			Vector2 a = new Vector2();
			Vector2 c1 = new Vector2();
			Vector2 c2 = new Vector2();
			Vector2 b = new Vector2();
			cursor.nextCubicFrame(a, c1, c2, b);

			Vector2 c12 = new Vector2();
			Vector2 c21 = new Vector2();
			Vector2 m = new Vector2();
			Vector2 tmp = new Vector2();

			LineUtils.splitCubicFrame(a, c1, c2, b, t, c12, m, c21, tmp);
			cursor.getAfter().setAt(0, c1);
			cursor.getAfter().setAt(1, c12);
			cursor.getAfter().setAt(2, m);

			{
				Event inserted = line.new Event();
				inserted.method = iLine_m.cubicTo_m;
				inserted.args = new Object[6];
				inserted.setAt(0, c21);
				inserted.setAt(1, c2);
				inserted.setAt(2, b);
				line.events.add(cursor.currentIndex + 2, inserted);
			}
			{
				Event inserted = line.new Event();
				inserted.method = iLine_m.moveTo_m;
				inserted.args = new Object[2];
				inserted.setAt(0, m);
				line.events.add(cursor.currentIndex + 2, inserted);
			}

			left = new CachedLine();

			left.events.addAll(line.events.subList(0, cursor.currentIndex + 2));

			for (Event e : left.events)
				e.setContainer(left);

		} else if (!cursor.nextIsSkip()) {

			Vector2 a = new Vector2();
			Vector2 b = new Vector2();
			cursor.nextLinearFrame(a, b);

			Vector2 m = new Vector2(a).lerp(a, b, t);
			cursor.getAfter().setAt(0, m);

			{
				Event inserted = line.new Event();
				inserted.method = iLine_m.lineTo_m;
				inserted.args = new Object[2];
				inserted.setAt(0, b);
				line.events.add(cursor.currentIndex + 2, inserted);
			}
			{
				Event inserted = line.new Event();
				inserted.method = iLine_m.moveTo_m;
				inserted.args = new Object[2];
				inserted.setAt(0, m);
				line.events.add(cursor.currentIndex + 2, inserted);
			}

			left = new CachedLine();

			left.events.addAll(line.events.subList(0, cursor.currentIndex + 2));

			for (Event e : left.events)
				e.setContainer(left);

		} else {
			left = new CachedLine();

			left.events.addAll(line.events.subList(0, cursor.currentIndex + 1));

			for (Event e : left.events)
				e.setContainer(left);

		}
		return left;
	}

	/**
	 * Splits a line here, returning two new lines. One leading up to this
	 * position, one starting here and continuing.
	 */
	public Pair<CachedLine, CachedLine> split() {
		Pair<CachedLine, CachedLine> r = new Pair<CachedLine, CachedLine>(null, null);

		if (cursor.nextIsCubic()) {
			Vector3 a = new Vector3();
			Vector3 c1 = new Vector3();
			Vector3 c2 = new Vector3();
			Vector3 b = new Vector3();
			cursor.nextCubicFrame3(a, c1, c2, b);

			Vector3 c12 = new Vector3();
			Vector3 c21 = new Vector3();
			Vector3 m = new Vector3();
			Vector3 tmp = new Vector3();

			LineUtils.splitCubicFrame3(a, c1, c2, b, t, c12, m, c21, tmp);
			cursor.getAfter().setAt3(0, c1);
			cursor.getAfter().setAt3(1, c12);
			cursor.getAfter().setAt3(2, m);

			{
				Event inserted = line.new Event();
				inserted.method = iLine_m.cubicTo_m;
				inserted.args = new Object[6];
				inserted.setAt3(0, c21);
				inserted.setAt3(1, c2);
				inserted.setAt3(2, b);
				line.events.add(cursor.currentIndex + 2, inserted);
			}
			{
				Event inserted = line.new Event();
				inserted.method = iLine_m.moveTo_m;
				inserted.args = new Object[2];
				inserted.setAt3(0, m);
				line.events.add(cursor.currentIndex + 2, inserted);
			}

			r.left = new CachedLine();
			r.right = new CachedLine();

			r.left.events.addAll(line.events.subList(0, cursor.currentIndex + 2));
			r.right.events.addAll(line.events.subList(cursor.currentIndex + 2, line.events.size()));

			for (Event e : r.right.events)
				e.setContainer(r.right);
			for (Event e : r.left.events)
				e.setContainer(r.left);

		} else if (!cursor.nextIsSkip()) {

			Vector3 a = new Vector3();
			Vector3 b = new Vector3();
			cursor.nextLinearFrame3(a, b);

			Vector3 m = new Vector3(a).lerp(a, b, t);
			cursor.getAfter().setAt3(0, m);

			{
				Event inserted = line.new Event();
				inserted.method = iLine_m.lineTo_m;
				inserted.args = new Object[2];
				inserted.setAt3(0, b);
				line.events.add(cursor.currentIndex + 2, inserted);
			}
			{
				Event inserted = line.new Event();
				inserted.method = iLine_m.moveTo_m;
				inserted.args = new Object[2];
				inserted.setAt3(0, m);
				line.events.add(cursor.currentIndex + 2, inserted);
			}

			r.left = new CachedLine();
			r.right = new CachedLine();

			r.left.events.addAll(line.events.subList(0, cursor.currentIndex + 2));
			r.right.events.addAll(line.events.subList(cursor.currentIndex + 2, line.events.size()));

			for (Event e : r.right.events)
				e.setContainer(r.right);
			for (Event e : r.left.events)
				e.setContainer(r.left);

		} else {
			r.left = new CachedLine();
			r.right = new CachedLine();

			r.left.events.addAll(line.events.subList(0, cursor.currentIndex + 1));
			r.right.events.addAll(line.events.subList(cursor.currentIndex + 1, line.events.size()));

			for (Event e : r.right.events)
				e.setContainer(r.right);
			for (Event e : r.left.events)
				e.setContainer(r.left);

		}

		return r;
	}

	/**
	 * Move forward one node
	 */
	public Cursor forwardNode() {
		cursor.next();
		t = 0;
		return this;
	}

	/**
	 * Move backwards one node
	 */
	public Cursor backNode() {
		if (t > 0)
			t = 0;
		else
			cursor.previous();
		return this;
	}

	/**
	 * Jump to a specific distance along this line
	 */
	public Cursor setD(float distance) {
		return setT(convertDToT(distance));
	}

	/**
	 * Jump to a specific 't' or node.fracttion along this line.
	 * 
	 * For example .setT(3.25) moves to 25% of the way between node 3 and
	 * node 4 (node numbers start at 0)
	 * 
	 * Negative numbers start from the end of the line
	 */
	public Cursor setT(float dot) {
		// System.out.println(" set T <"+dot+">");
		if (dot < 0)
			return setT(lengthT() - 1 - Math.abs(dot));

		if (dot > lengthT())
			dot = lengthT();

		cursor = new CachedLineCursor(line, (int) dot);
		t = dot - (int) dot;
		return this;
	}

	/**
	 * move forward by node.fraction.
	 * 
	 * For example .forwardT(1.25) moves to the next node + 25% of the way
	 * to the node after that.
	 */
	public Cursor forwardT(float dt) {
		if (dt == 0)
			return this;
		if (dt < 0)
			return backwardT(-dt);

		t += dt;
		while (t >= 1) {
			if (isEndOfLine()) {
				t = 0;
				return this;
			}

			float s = t - 1;
			forwardNode();
			t = s;
		}
		return this;
	}

	/**
	 * move backward by node.fraction.
	 * 
	 * For example .backwardT(1.25) moves to the previous node + 25% of the
	 * way to the node after that.
	 */
	public Cursor backwardT(float dt) {
		if (dt == 0)
			return this;
		if (dt < 0)
			return forwardT(-dt);

		t -= dt;

		while (t <= 0) {
			if (!cursor.hasPreviousSegment()) {
				t = 0;
				return this;
			}

			float s = 1 + t;
			backNode();
			t = s;
		}
		return this;
	}

	/**
	 * returns the position of this Cursor.
	 */
	public Vector2 position() {
		if (t == 0) {
			Vector2 v = new Vector2();
			if (!cursor.getAt(v)) {
				return null;
			}
			return v;
		} else {
			if (cursor.nextIsCubic()) {
				Vector2 va = new Vector2();
				Vector2 vc1 = new Vector2();
				Vector2 vc2 = new Vector2();
				Vector2 vb = new Vector2();
				cursor.nextCubicFrame(va, vc1, vc2, vb);

				return LineUtils.evaluateCubicFrame(va, vc1, vc2, vb, t, new Vector2());
			} else if (cursor.nextIsSkip()) {

				Vector2 a = new Vector2();
				cursor.getAt(a);

				return a;
			} else {
				Vector2 va = new Vector2();
				Vector2 vb = new Vector2();
				cursor.nextLinearFrame(va, vb);

				return new Vector2(va).interpolate(vb, t);
			}
		}
	}

	public Vector3 position3() {
		if (t == 0) {
			Vector3 v = new Vector3();
			if (!cursor.getAt(v)) {
				return null;
			}
			return v;
		} else {
			if (cursor.nextIsCubic()) {
				Vector3 va = new Vector3();
				Vector3 vc1 = new Vector3();
				Vector3 vc2 = new Vector3();
				Vector3 vb = new Vector3();
				cursor.nextCubicFrame3(va, vc1, vc2, vb);

				return LineUtils.evaluateCubicFrame3(va, vc1, vc2, vb, t, new Vector3());
			} else if (cursor.nextIsSkip()) {

				Vector3 a = new Vector3();
				cursor.getAt(a);

				return a;
			} else {
				Vector3 va = new Vector3();
				Vector3 vb = new Vector3();
				cursor.nextLinearFrame3(va, vb);

				
//				System.out.println(" next linear frame for interpolation <"+va+" "+vb+">");
				
				return new Vector3(va).interpolate(vb, t);
			}
		}
	}

	@HiddenInAutocomplete
	public <T> T[] attribute(Prop<T> p, Class<? super T> t) {
		return cursor.nextAttributeFrame(p, t);
	}

	/**
	 * returns an (per-vertex) attribute interpolated at this current position
	 */
	public Object attribute(String p) {
		Object[] o = cursor.nextAttributeFrame(new Prop(p), Object.class);

		if (o == null)
			return null;
		if (o[0] instanceof Vector3)
			return new Vector3().lerp((Vector3) o[0], (Vector3) o[1], t);
		if (o[0] instanceof Vector4)
			return new Vector4().lerp((Vector4) o[0], (Vector4) o[1], t);
		if (o[0] instanceof Vector2)
			return new Vector2().lerp((Vector2) o[0], (Vector2) o[1], t);
		if (o[0] instanceof Number)
			return ((Number) o[0]).floatValue() * (1 - t) + t * ((Number) o[1]).floatValue();
		return null;
	}

	/**
	 * returns the tangent at this position of the line.
	 * 
	 * This is a direction that, starting at .position(), points along the
	 * direction that this line is instantaneously heading in.
	 * 
	 * Because you might be at a sharp corner, there's also an associated
	 * method 'tangentBackward'. But usually, at smooth sections of the line
	 * these methods return things that at least point in the same
	 * direction, if they are not the same length.
	 */
	public Vector2 tangentForward() {
		Vector2 va = new Vector2();
		Vector2 vc1 = new Vector2();
		Vector2 vc2 = new Vector2();
		Vector2 vb = new Vector2();

		if (!cursor.nextCubicFrame(va, vc1, vc2, vb)) {
			return null;
		} else {
			return LineUtils.evaluateDCubicFrame(va, vc1, vc2, vb, t, new Vector2()).scale(0.333333333f);
		}
	}

	/**
	 * returns the tangent at this position of the line.
	 * 
	 * This is a direction that, starting at .position(), points along the
	 * direction that this line is instantaneously heading in.
	 * 
	 * Because you might be at a sharp corner, there's also an associated
	 * method 'tangentBackward'. But usually, at smooth sections of the line
	 * these methods return things that at least point in the same
	 * direction, if they are not the same length.
	 */
	public Vector3 tangentForward3() {
		Vector3 va = new Vector3();
		Vector3 vc1 = new Vector3();
		Vector3 vc2 = new Vector3();
		Vector3 vb = new Vector3();

		if (!cursor.nextCubicFrame3(va, vc1, vc2, vb)) {
			return null;
		} else {
			return LineUtils.evaluateDCubicFrame3(va, vc1, vc2, vb, t, new Vector3()).scale(0.333333333f);
		}
	}

	/**
	 * returns the acceleration at this position of the line.
	 * 
	 * This is a direction that, starting at .position(), points along the
	 * direction that this line is instantaneously curving towards.
	 * 
	 * Because you might be at a sharp corner, there's also an associated
	 * method 'accelerationBackward'. But usually, at smooth sections of the
	 * line these methods return things that at least point in the same
	 * direction, if they are not the same length.
	 */
	public Vector2 accelerationForward() {
		Vector2 va = new Vector2();
		Vector2 vc1 = new Vector2();
		Vector2 vc2 = new Vector2();
		Vector2 vb = new Vector2();
		if (!cursor.nextCubicFrame(va, vc1, vc2, vb)) {
			return null;
		} else {
			return LineUtils.evaluateD2CubicFrame(va, vc1, vc2, vb, t, new Vector2()).scale(0.333333333f);
		}
	}

	/**
	 * returns the acceleration at this position of the line.
	 * 
	 * This is a direction that, starting at .position(), points along the
	 * direction that this line is instantaneously curving towards.
	 * 
	 * Because you might be at a sharp corner, there's also an associated
	 * method 'accelerationBackward'. But usually, at smooth sections of the
	 * line these methods return things that at least point in the same
	 * direction, if they are not the same length.
	 */
	public Vector3 accelerationForward3() {
		Vector3 va = new Vector3();
		Vector3 vc1 = new Vector3();
		Vector3 vc2 = new Vector3();
		Vector3 vb = new Vector3();
		if (!cursor.nextCubicFrame3(va, vc1, vc2, vb)) {
			return null;
		} else {
			return LineUtils.evaluateD2CubicFrame3(va, vc1, vc2, vb, t, new Vector3()).scale(0.333333333f);
		}
	}

	public Vector2 tangentBackward() {
		Vector2 va = new Vector2();
		Vector2 vc1 = new Vector2();
		Vector2 vc2 = new Vector2();
		Vector2 vb = new Vector2();
		if (t == 0) {
			if (!cursor.previousCubicFrame(va, vc1, vc2, vb))
				return null;

			System.out.println(" previous cubic frame is <" + va + " " + vc1 + " " + vc2 + " " + vb + ">");

			return LineUtils.evaluateDCubicFrame(va, vc1, vc2, vb, 1, new Vector2()).scale(0.3333333333f);
		} else {
			if (!cursor.nextCubicFrame(va, vc1, vc2, vb))
				return null;

			return LineUtils.evaluateDCubicFrame(va, vc1, vc2, vb, t, new Vector2()).scale(0.3333333333f);
		}
	}

	public Vector2 accelerationBackward() {
		Vector2 va = new Vector2();
		Vector2 vc1 = new Vector2();
		Vector2 vc2 = new Vector2();
		Vector2 vb = new Vector2();
		if (t == 0) {
			if (!cursor.previousCubicFrame(va, vc1, vc2, vb))
				return null;
			return LineUtils.evaluateD2CubicFrame(va, vc1, vc2, vb, 1, new Vector2()).scale(0.3333333333f);
		} else {
			if (!cursor.nextCubicFrame(va, vc1, vc2, vb))
				return null;

			return LineUtils.evaluateD2CubicFrame(va, vc1, vc2, vb, t, new Vector2()).scale(0.3333333333f);
		}
	}

	public Vector3 tangentBackward3() {
		Vector3 va = new Vector3();
		Vector3 vc1 = new Vector3();
		Vector3 vc2 = new Vector3();
		Vector3 vb = new Vector3();
		if (t == 0) {
			if (!cursor.previousCubicFrame3(va, vc1, vc2, vb))
				return null;

			System.out.println(" previous cubic frame is <" + va + " " + vc1 + " " + vc2 + " " + vb + ">");

			return LineUtils.evaluateDCubicFrame3(va, vc1, vc2, vb, 1, new Vector3()).scale(0.3333333333f);
		} else {
			if (!cursor.nextCubicFrame3(va, vc1, vc2, vb))
				return null;

			return LineUtils.evaluateDCubicFrame3(va, vc1, vc2, vb, t, new Vector3()).scale(0.3333333333f);
		}
	}

	public Vector3 accelerationBackward3() {
		Vector3 va = new Vector3();
		Vector3 vc1 = new Vector3();
		Vector3 vc2 = new Vector3();
		Vector3 vb = new Vector3();
		if (t == 0) {
			if (!cursor.previousCubicFrame3(va, vc1, vc2, vb))
				return null;
			return LineUtils.evaluateD2CubicFrame3(va, vc1, vc2, vb, 1, new Vector3()).scale(0.3333333333f);
		} else {
			if (!cursor.nextCubicFrame3(va, vc1, vc2, vb))
				return null;

			return LineUtils.evaluateD2CubicFrame3(va, vc1, vc2, vb, t, new Vector3()).scale(0.3333333333f);
		}
	}

	public float curvatureForward() {
		Vector2 p = position();
		Vector2 d = tangentForward();
		Vector2 d2 = accelerationForward();

		float k = (float) ((d.x * d2.y - d.y * d2.x) / Math.pow(d.x * d.x + d.y * d.y, 3 / 2));
		return k / 243;
	}

	/**
	 * is the cursor at the end of a segment (the next position will start
	 * after a jump)?
	 */
	public boolean isEndOfSSegment() {
		return !cursor.hasNextInSpline();
	}

	/**
	 * is the cursor at the start of a segment?
	 */
	public boolean isStartOfSegment() {
		return !cursor.hasPreviousInSpline();
	}

	/**
	 * is this cursor at the end of a line
	 */
	public boolean isEndOfLine() {
		return !cursor.hasNextSegment();
	}

	public boolean isStartOfLine() {
		return !cursor.hasPreviousSegment() && t == 0;
	}

	static public List<Cursor> cursorsFromMinimalApproach(CachedLine l1, Vector2 to) {
		AllDistanceMinima minima = new LineUtils.AllDistanceMinima(l1, to);
		List<Minimum> a = minima.minima;
		ArrayList<Cursor> cc = new ArrayList<Cursor>();
		for (Minimum m : a) {
			cc.add(new Cursor(l1, m.minT + m.minAtIndex));
		}
		return cc;
	}

	static public Cursor cursorFromClosestPoint(CachedLine l1, Vector2 to) {
		ClosestPointToSpline cp = new LineUtils.ClosestPointToSpline(l1, to);

		System.out.println(" closest point is <" + cp.minAtIndex + " " + cp.minT + ">");

		return new Cursor(l1, cp.minAtIndex + cp.minT);
	}

	static public Vector2 pointFromClosestPoint(CachedLine ll, Vector2 to) {
		ClosestPointToSpline cp = new LineUtils.ClosestPointToSpline(ll, to);

		return cp.getMinPoint();
	}

	static public Cursor cursorFromClosestPoint(CachedLine l1, Vector3 to) {
		ClosestPointToSpline3 cp = new LineUtils.ClosestPointToSpline3(l1, to);

		System.out.println(" closest point is <" + cp.minAtIndex + " " + cp.minT + ">");

		return new Cursor(l1, cp.minAtIndex + cp.minT);
	}

	static public Vector3 pointFromClosestPoint(CachedLine ll, Vector3 to) {
		ClosestPointToSpline3 cp = new LineUtils.ClosestPointToSpline3(ll, to);

		return cp.getMinPoint();
	}

}
