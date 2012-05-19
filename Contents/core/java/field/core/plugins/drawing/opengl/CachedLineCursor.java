package field.core.plugins.drawing.opengl;

import java.lang.reflect.Array;

import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.util.Dict;

public class CachedLineCursor {

	public CachedLine.Event before;

	public CachedLine.Event current;

	public CachedLine.Event after;

	private final CachedLine on;

	int currentIndex;

	int lastMoveTo = 0;

	Vector2 ta = new Vector2();

	Vector2 tc1 = new Vector2();

	Vector2 tc2 = new Vector2();

	Vector2 tb = new Vector2();

	public CachedLineCursor(CachedLine on) {
		this.on = on;
		currentIndex = -1;
	}

	public CachedLineCursor(CachedLine on, int minAtIndex) {
		this.on = on;
		currentIndex = minAtIndex - 1;
		next();
	}

	public Vector2 evaluate(float alpha) {
		if (nextIsCubic()) {
			nextCubicFrame(ta, tc1, tc2, tb);
			return LineUtils.evaluateCubicFrame(ta, tc1, tc2, tb, alpha, new Vector2());
		} else {
			nextLinearFrame(ta, tb);
			return new Vector2(ta).interpolate(tb, alpha);
		}
	}

	public CachedLine.Event getAfter() {
		return after;
	}

	public boolean getAt(Vector2 a) {
		if (current.method.equals(iLine_m.close_m)) {
			a.x = ((Number) (on.events.get(lastMoveTo).args)[0]).floatValue();
			a.y = ((Number) (on.events.get(lastMoveTo).args)[1]).floatValue();
			return true;
		}
		if (current.args == null)
			return false;
		if (current.args[current.args.length - 2] == null)
			return false;
		if (current.args[current.args.length - 1] == null)
			return false;
		a.x = ((Number) (current.args)[current.args.length - 2]).floatValue();
		a.y = ((Number) (current.args)[current.args.length - 1]).floatValue();
		return true;
	}

	public boolean getAt(Vector3 a) {

		if (current.method.equals(iLine_m.close_m)) {
			a.x = ((Number) (on.events.get(lastMoveTo).args)[0]).floatValue();
			a.y = ((Number) (on.events.get(lastMoveTo).args)[1]).floatValue();
			a.z = interpret(on.events.get(lastMoveTo).getAttributes().get(iLinearGraphicsContext.z_v), -1);
			return true;
		}
		if (current.args == null)
			return false;
		if (current.args[current.args.length - 2] == null)
			return false;
		if (current.args[current.args.length - 1] == null)
			return false;
		a.x = ((Number) (current.args)[current.args.length - 2]).floatValue();
		a.y = ((Number) (current.args)[current.args.length - 1]).floatValue();
		a.z = interpret(current.getAttributes().get(iLinearGraphicsContext.z_v), -1);
		return true;
	}

	public CachedLine.Event getBefore() {
		return before;
	}

	public CachedLine.Event getCurrent() {
		return current;
	}

	public int getCurrentIndex() {
		return currentIndex;
	}

	public Vector2 getNextDestination(Vector2 a) {
		if (after == null)
			return null;
		if (after.method.equals(iLine_m.close_m))
			return on.events.get(lastMoveTo).getDestination(a);

		System.err.println(" get in <" + after + ">");

		return after.getDestination(a);
	}

	public boolean hasNextInSpline() {
		return after != null;
	}

	public boolean hasNextSegment() {
		return currentIndex < on.events.size() - 1;
	}

	public boolean hasPreviousInSpline() {
		return before != null;
	}

	public boolean hasPreviousSegment() {
		return currentIndex > 0;
	}

	public CachedLineCursor next() {
		if (after == null) {
			currentIndex++;
			if (currentIndex >= on.events.size())
				currentIndex = on.events.size() - 1;
			current = on.events.get(currentIndex);
			before = currentIndex > 0 ? on.events.get(currentIndex - 1) : null;
			after = currentIndex < on.events.size() - 1 ? on.events.get(currentIndex + 1) : null;
			if (after != null)
				if (after.method.equals(iLine_m.moveTo_m))
					after = null;
		} else {
			before = current;
			current = after;
			currentIndex++;
			after = currentIndex < on.events.size() - 1 ? on.events.get(currentIndex + 1) : null;
			if (after != null)
				if (after.method.equals(iLine_m.moveTo_m))
					after = null;
		}

		if (current.method.equals(iLine_m.moveTo_m))
			lastMoveTo = currentIndex;
		return this;

	}

	public boolean nextCubicFrame(Vector2 a, Vector2 c1, Vector2 c2, Vector2 b) {
		if (after == null)
			return false;
		if (after.method.equals(iLine_m.cubicTo_m)) {
			a.x = ((Number) (current.args)[current.args.length - 2]).floatValue();
			a.y = ((Number) (current.args)[current.args.length - 1]).floatValue();

			c1.x = ((Number) (after.args)[0]).floatValue();
			c1.y = ((Number) (after.args)[1]).floatValue();

			c2.x = ((Number) (after.args)[2]).floatValue();
			c2.y = ((Number) (after.args)[3]).floatValue();

			b.x = ((Number) (after.args)[4]).floatValue();
			b.y = ((Number) (after.args)[5]).floatValue();

			return true;
		}
		if (after.method.equals(iLine_m.lineTo_m)) {
			a.x = ((Number) (current.args)[current.args.length - 2]).floatValue();
			a.y = ((Number) (current.args)[current.args.length - 1]).floatValue();

			b.x = ((Number) (after.args)[0]).floatValue();
			b.y = ((Number) (after.args)[1]).floatValue();

			c1.x = a.x + (b.x - a.x) / 3;
			c1.y = a.y + (b.y - a.y) / 3;
			c2.x = b.x - (b.x - a.x) / 3;
			c2.y = b.y - (b.y - a.y) / 3;
			return true;
		}

		if (after.method.equals(iLine_m.close_m)) {
			a.x = ((Number) (current.args)[current.args.length - 2]).floatValue();
			a.y = ((Number) (current.args)[current.args.length - 1]).floatValue();

			b.x = ((Number) (on.events.get(lastMoveTo).args)[0]).floatValue();
			b.y = ((Number) (on.events.get(lastMoveTo).args)[1]).floatValue();

			c1.x = a.x + (b.x - a.x) / 3;
			c1.y = a.y + (b.y - a.y) / 3;
			c2.x = b.x - (b.x - a.x) / 3;
			c2.y = b.y - (b.y - a.y) / 3;
			return true;
		}
		assert false : current.method;
		return false;
	}

	public boolean nextCubicFrame3(Vector3 a, Vector3 c1, Vector3 c2, Vector3 b) {
		if (after == null)
			return false;

		// System.out.println(" current <"+current+"> <"+current.attributes+">");

		if (after.method.equals(iLine_m.cubicTo_m)) {
			a.x = ((Number) (current.args)[current.args.length - 2]).floatValue();
			a.y = ((Number) (current.args)[current.args.length - 1]).floatValue();
			a.z = interpret(current.getAttributes().get(iLinearGraphicsContext.z_v), -1);

			c1.x = ((Number) (after.args)[0]).floatValue();
			c1.y = ((Number) (after.args)[1]).floatValue();
			c1.z = interpret(after.getAttributes().get(iLinearGraphicsContext.z_v), 0);

			c2.x = ((Number) (after.args)[2]).floatValue();
			c2.y = ((Number) (after.args)[3]).floatValue();
			c2.z = interpret(after.getAttributes().get(iLinearGraphicsContext.z_v), 1);

			b.x = ((Number) (after.args)[4]).floatValue();
			b.y = ((Number) (after.args)[5]).floatValue();
			b.z = interpret(after.getAttributes().get(iLinearGraphicsContext.z_v), -1);

			return true;
		}
		if (after.method.equals(iLine_m.lineTo_m)) {
			a.x = ((Number) (current.args)[current.args.length - 2]).floatValue();
			a.y = ((Number) (current.args)[current.args.length - 1]).floatValue();
			a.z = interpret(current.getAttributes().get(iLinearGraphicsContext.z_v), -1);

			b.x = ((Number) (after.args)[0]).floatValue();
			b.y = ((Number) (after.args)[1]).floatValue();
			b.z = interpret(after.getAttributes().get(iLinearGraphicsContext.z_v), -1);

			c1.x = a.x + (b.x - a.x) / 3;
			c1.y = a.y + (b.y - a.y) / 3;
			c1.z = a.z + (b.z - a.z) / 3;

			c2.x = b.x - (b.x - a.x) / 3;
			c2.y = b.y - (b.y - a.y) / 3;
			c2.z = b.z - (b.z - a.z) / 3;
			return true;
		}

		if (after.method.equals(iLine_m.close_m)) {
			a.x = ((Number) (current.args)[current.args.length - 2]).floatValue();
			a.y = ((Number) (current.args)[current.args.length - 1]).floatValue();
			a.z = interpret(current.getAttributes().get(iLinearGraphicsContext.z_v), -1);

			b.x = ((Number) (on.events.get(lastMoveTo).args)[0]).floatValue();
			b.y = ((Number) (on.events.get(lastMoveTo).args)[1]).floatValue();
			b.z = interpret(on.events.get(lastMoveTo).getAttributes().get(iLinearGraphicsContext.z_v), -1);

			c1.x = a.x + (b.x - a.x) / 3;
			c1.y = a.y + (b.y - a.y) / 3;
			c1.z = a.z + (b.z - a.z) / 3;

			c2.x = b.x - (b.x - a.x) / 3;
			c2.y = b.y - (b.y - a.y) / 3;
			c2.z = b.z - (b.z - a.z) / 3;
			return true;
		}
		assert false : current.method;
		return false;
	}

	public <T> T[] nextAttributeFrame(Dict.Prop<T> p, Class<? super T> c) {

		T[] oo = (T[]) Array.newInstance(c, 2);

		if (after == null)
			return null;

		// System.out.println(" current <"+current+"> <"+current.attributes+">");

		if (after.method.equals(iLine_m.cubicTo_m)) {
			oo[0] = current.attributes.get(p);
			oo[1] = after.attributes.get(p);

			return oo;
		}
		if (after.method.equals(iLine_m.lineTo_m)) {
			oo[0] = current.attributes.get(p);
			oo[1] = after.attributes.get(p);
			return oo;
		}

		if (after.method.equals(iLine_m.close_m)) {
			oo[0] = current.attributes.get(p);
			oo[1] = on.events.get(lastMoveTo).attributes.get(p);
			return oo;
		}
		assert false : current.method;
		return null;
	}

	private float interpret(Object object, int i) {
		if (object == null)
			return 0;
		if (object instanceof Number)
			return ((Number) object).floatValue();
		if (object instanceof Vector3) {
			Vector3 v = ((Vector3) object);
			if (i == -1)
				return v.get(2);
			return v.get(i);
		}
		return 0;
	}

	public boolean nextIsCubic() {
		if (after == null)
			return false;
		if (after.method.equals(iLine_m.cubicTo_m))
			return true;
		return false;
	}

	public boolean nextIsSkip() {
		return after == null && hasNextSegment();
	}

	public boolean nextLinearFrame(Vector2 a, Vector2 b) {

		try {
			if (after == null) {
				if (current != null) {
					if (current.hasDestination()) {
						a.setValue(current.getDestination());
						b.setValue(current.getDestination());
					}
				}
				return false;
			}

			if (after.method.equals(iLine_m.cubicTo_m)) {
				a.x = ((Number) (current.args)[current.args.length - 2]).floatValue();
				a.y = ((Number) (current.args)[current.args.length - 1]).floatValue();

				b.x = ((Number) (after.args)[4]).floatValue();
				b.y = ((Number) (after.args)[5]).floatValue();

				return true;
			}
			if (after.method.equals(iLine_m.lineTo_m)) {

				a.x = ((Number) (current.args)[current.args.length - 2]).floatValue();
				a.y = ((Number) (current.args)[current.args.length - 1]).floatValue();

				b.x = ((Number) (after.args)[0]).floatValue();
				b.y = ((Number) (after.args)[1]).floatValue();

				return true;
			}

			if (after.method.equals(iLine_m.close_m)) {
				a.x = ((Number) (current.args)[current.args.length - 2]).floatValue();
				a.y = ((Number) (current.args)[current.args.length - 1]).floatValue();

				b.x = ((Number) (on.events.get(lastMoveTo).args)[0]).floatValue();
				b.y = ((Number) (on.events.get(lastMoveTo).args)[1]).floatValue();

				return true;
			}
			assert false : after.method + " " + iLine_m.lineTo_m + " " + after.method.hashCode() + " " + iLine_m.lineTo_m.hashCode() + " " + after.method.equals(iLine_m.lineTo_m) + " " + (iLine_m.lineTo_m == after.method);
			return false;
		} catch (NullPointerException e) {
			e.printStackTrace();
			throw e;
		}
	}

	public boolean nextLinearFrame3(Vector3 a, Vector3 b) {

		try {
			if (after == null) {
				if (current != null) {
					a.x = ((Number) (current.args)[current.args.length - 2]).floatValue();
					a.y = ((Number) (current.args)[current.args.length - 1]).floatValue();
					a.z = interpret(current.getAttributes().get(iLinearGraphicsContext.z_v), -1);
					b.x = ((Number) (current.args)[current.args.length - 2]).floatValue();
					b.y = ((Number) (current.args)[current.args.length - 1]).floatValue();
					b.z = interpret(current.getAttributes().get(iLinearGraphicsContext.z_v), -1);
				}
				return false;
			}
			if (after.method.equals(iLine_m.cubicTo_m)) {
				a.x = ((Number) (current.args)[current.args.length - 2]).floatValue();
				a.y = ((Number) (current.args)[current.args.length - 1]).floatValue();
				a.z = interpret(current.getAttributes().get(iLinearGraphicsContext.z_v), -1);

				b.x = ((Number) (after.args)[4]).floatValue();
				b.y = ((Number) (after.args)[5]).floatValue();
				b.z = interpret(after.getAttributes().get(iLinearGraphicsContext.z_v), -1);

				return true;
			}
			if (after.method.equals(iLine_m.lineTo_m)) {

//				System.out.println(" looking at <"+current.getAttributes()+">");
				
				a.x = ((Number) (current.args)[current.args.length - 2]).floatValue();
				a.y = ((Number) (current.args)[current.args.length - 1]).floatValue();
				a.z = interpret(current.getAttributes().get(iLinearGraphicsContext.z_v), -1);

				b.x = ((Number) (after.args)[0]).floatValue();
				b.y = ((Number) (after.args)[1]).floatValue();
				b.z = interpret(after.getAttributes().get(iLinearGraphicsContext.z_v), -1);

				return true;
			}

			if (after.method.equals(iLine_m.close_m)) {
				a.x = ((Number) (current.args)[current.args.length - 2]).floatValue();
				a.y = ((Number) (current.args)[current.args.length - 1]).floatValue();
				a.z = interpret(current.getAttributes().get(iLinearGraphicsContext.z_v), -1);

				b.x = ((Number) (on.events.get(lastMoveTo).args)[0]).floatValue();
				b.y = ((Number) (on.events.get(lastMoveTo).args)[1]).floatValue();
				b.z = interpret(on.events.get(lastMoveTo).getAttributes().get(iLinearGraphicsContext.z_v), -1);

				return true;
			}
			assert false : after.method + " " + iLine_m.lineTo_m + " " + after.method.hashCode() + " " + iLine_m.lineTo_m.hashCode() + " " + after.method.equals(iLine_m.lineTo_m) + " " + (iLine_m.lineTo_m == after.method);
			return false;
		} catch (NullPointerException e) {
			e.printStackTrace();
			throw e;
		}
	}

	public void getThreeFrame(Vector3 b, Vector3 c, Vector3 a) {
		c.x = ((Number) current.args[0]).floatValue();
		c.y = ((Number) current.args[1]).floatValue();
		c.z = 0;
		if (before == null) {
			b.x = c.x;
			b.y = c.y;
			b.z = 0;
		} else {
			b.x = ((Number) before.args[0]).floatValue();
			b.y = ((Number) before.args[1]).floatValue();
			b.z = -1;
		}
		if (after == null) {
			a.x = c.x;
			a.y = c.y;
			a.z = 0;
		} else {
			a.x = ((Number) before.args[0]).floatValue();
			a.y = ((Number) before.args[1]).floatValue();
			a.z = 1;
		}
	}

	public CachedLineCursor previous() {
		assert before != null;
		after = current;
		current = before;
		currentIndex--;
		if (currentIndex < 0)
			currentIndex = 0;
		before = currentIndex > 0 ? on.events.get(currentIndex - 1) : null;
		return this;
	}

	public boolean previousCubicFrame(Vector2 a, Vector2 c1, Vector2 c2, Vector2 b) {
		if (before == null)
			return false;
		if (current.method.equals(iLine_m.cubicTo_m)) {
			a.x = ((Number) (before.args)[before.args.length - 2]).floatValue();
			a.y = ((Number) (before.args)[before.args.length - 1]).floatValue();

			c1.x = ((Number) (current.args)[0]).floatValue();
			c1.y = ((Number) (current.args)[1]).floatValue();

			c2.x = ((Number) (current.args)[2]).floatValue();
			c2.y = ((Number) (current.args)[3]).floatValue();

			b.x = ((Number) (current.args)[4]).floatValue();
			b.y = ((Number) (current.args)[5]).floatValue();

			return true;
		}
		if (current.method.equals(iLine_m.lineTo_m)) {
			a.x = ((Number) (before.args)[before.args.length - 2]).floatValue();
			a.y = ((Number) (before.args)[before.args.length - 1]).floatValue();

			b.x = ((Number) (current.args)[0]).floatValue();
			b.y = ((Number) (current.args)[1]).floatValue();

			c1.x = a.x + (b.x - a.x) / 3;
			c1.y = a.y + (b.y - a.y) / 3;
			c2.x = b.x - (b.x - a.x) / 3;
			c2.y = b.y - (b.y - a.y) / 3;
			return true;
		}

		if (current.method.equals(iLine_m.close_m)) {
			a.x = ((Number) (before.args)[before.args.length - 2]).floatValue();
			a.y = ((Number) (before.args)[before.args.length - 1]).floatValue();

			b.x = ((Number) (on.events.get(lastMoveTo).args)[0]).floatValue();
			b.y = ((Number) (on.events.get(lastMoveTo).args)[1]).floatValue();

			c1.x = a.x + (b.x - a.x) / 3;
			c1.y = a.y + (b.y - a.y) / 3;
			c2.x = b.x - (b.x - a.x) / 3;
			c2.y = b.y - (b.y - a.y) / 3;
			return true;
		}
		assert false : current.method;
		return false;
	}

	public boolean previousCubicFrame3(Vector3 a, Vector3 c1, Vector3 c2, Vector3 b) {
		if (before == null)
			return false;
		if (current.method.equals(iLine_m.cubicTo_m)) {
			a.x = ((Number) (before.args)[before.args.length - 2]).floatValue();
			a.y = ((Number) (before.args)[before.args.length - 1]).floatValue();
			a.z = interpret(before.getAttributes().get(iLinearGraphicsContext.z_v), -1);

			c1.x = ((Number) (current.args)[0]).floatValue();
			c1.y = ((Number) (current.args)[1]).floatValue();
			c1.z = interpret(current.getAttributes().get(iLinearGraphicsContext.z_v), 0);

			c2.x = ((Number) (current.args)[2]).floatValue();
			c2.y = ((Number) (current.args)[3]).floatValue();
			c2.z = interpret(current.getAttributes().get(iLinearGraphicsContext.z_v), 1);

			b.x = ((Number) (current.args)[4]).floatValue();
			b.y = ((Number) (current.args)[5]).floatValue();
			b.z = interpret(current.getAttributes().get(iLinearGraphicsContext.z_v), -1);

			return true;
		}
		if (current.method.equals(iLine_m.lineTo_m)) {
			a.x = ((Number) (before.args)[before.args.length - 2]).floatValue();
			a.y = ((Number) (before.args)[before.args.length - 1]).floatValue();
			a.z = interpret(before.getAttributes().get(iLinearGraphicsContext.z_v), -1);

			b.x = ((Number) (current.args)[0]).floatValue();
			b.y = ((Number) (current.args)[1]).floatValue();
			b.z = interpret(current.getAttributes().get(iLinearGraphicsContext.z_v), 0);

			c1.x = a.x + (b.x - a.x) / 3;
			c1.y = a.y + (b.y - a.y) / 3;
			c2.x = b.x - (b.x - a.x) / 3;
			c2.y = b.y - (b.y - a.y) / 3;

			c1.z = a.z + (b.z - a.z) / 3;
			c2.z = b.z - (b.z - a.z) / 3;

			return true;
		}

		if (current.method.equals(iLine_m.close_m)) {
			a.x = ((Number) (before.args)[before.args.length - 2]).floatValue();
			a.y = ((Number) (before.args)[before.args.length - 1]).floatValue();
			a.z = interpret(before.getAttributes().get(iLinearGraphicsContext.z_v), -1);

			b.x = ((Number) (on.events.get(lastMoveTo).args)[0]).floatValue();
			b.y = ((Number) (on.events.get(lastMoveTo).args)[1]).floatValue();
			b.z = interpret(on.events.get(lastMoveTo).getAttributes().get(iLinearGraphicsContext.z_v), 0);

			c1.x = a.x + (b.x - a.x) / 3;
			c1.y = a.y + (b.y - a.y) / 3;
			c2.x = b.x - (b.x - a.x) / 3;
			c2.y = b.y - (b.y - a.y) / 3;

			c1.z = a.z + (b.z - a.z) / 3;
			c2.z = b.z - (b.z - a.z) / 3;

			return true;
		}
		assert false : current.method;
		return false;
	}

}
