package field.core.plugins.drawing.tweak;

import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLine_m;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.math.linalg.Vector2;

/**
 * for add and delete nodes, these operate, "illegally" by mutating cachedlines
 * 
 * @author marc
 * 
 */
public class NodeModifiers {

	public interface iNodeModifier {
		public void apply(CachedLine inside, CachedLine.Event event, int index);
	}

	static public class SubdivideLeft implements iNodeModifier {
		Vector2 c12 = new Vector2();

		Vector2 c21 = new Vector2();

		Vector2 m = new Vector2();

		Vector2 tmp = new Vector2();

		private float alpha;

		public SubdivideLeft() {
			this(0.5f);
		}

		public SubdivideLeft(float alpha) {
			this.alpha = alpha;
		}

		public SubdivideLeft(Number alpha) {
			this.alpha = alpha.floatValue();
		}

		public void apply(CachedLine inside, CachedLine.Event event, int index) {

			if (event.method.equals(iLine_m.lineTo_m)) {
				Vector2 from = inside.events.get(index - 1).getDestination();
				Vector2 to = event.getDestination();

				Event e = inside.new Event();
				e.method = iLine_m.lineTo_m;
				e.args = new Object[] { (float) (to.x*alpha + from.x*(1-alpha)) , (float) (to.y*alpha + from.y*(1-alpha)) };
				inside.finalized = false;
				inside.events.add(index, e);
			} else if (event.method.equals(iLine_m.cubicTo_m)) {
				Vector2 from = inside.events.get(index - 1).getDestination();
				Vector2 c1 = event.getAt(0);
				Vector2 c2 = event.getAt(1);
				Vector2 to = event.getDestination();

				LineUtils.splitCubicFrame(from, c1, c2, to, (float) alpha, c12, m, c21, tmp);

				Event e = inside.new Event();
				e.method = iLine_m.cubicTo_m;
				e.args = new Object[] { c1.x, c1.y, c12.x, c12.y, m.x, m.y};
				inside.finalized = false;
				inside.events.add(index, e);

				event.args = new Object[] { c21.x, c21.y, c2.x, c2.y, to.x, to.y};
			}
		}
	}

	static public class SubdivideRight implements iNodeModifier {
		Vector2 c12 = new Vector2();

		Vector2 c21 = new Vector2();

		Vector2 m = new Vector2();

		Vector2 tmp = new Vector2();

		public SubdivideRight() {
		}

		public void apply(CachedLine inside, CachedLine.Event event, int index) {

			if (index > inside.events.size() - 1) return;

			event = inside.events.get(index + 1);
			index = index + 1;
			new SubdivideLeft().apply(inside, event, index);
		}
	}

	static public class EnsureCubicLeft implements iNodeModifier {

		public EnsureCubicLeft() {
		}

		public void apply(CachedLine inside, CachedLine.Event event, int index) {
			promoteToCubic(inside, event, index);
		}
	}

	static public class EnsureLinearLeft implements iNodeModifier {

		public EnsureLinearLeft() {
		}

		public void apply(CachedLine inside, CachedLine.Event event, int index) {
			demoteToLine(inside, event, index);
		}
	}

	static public class EnsureCubicRight implements iNodeModifier {

		public EnsureCubicRight() {
		}

		public void apply(CachedLine inside, CachedLine.Event event, int index) {
			if (inside.events.size() > index + 1) promoteToCubic(inside, inside.events.get(index + 1), index + 1);
		}
	}

	static public class EnsureLinearRight implements iNodeModifier {

		public EnsureLinearRight() {
		}

		public void apply(CachedLine inside, CachedLine.Event event, int index) {
			if (inside.events.size() > index + 1) demoteToLine(inside, inside.events.get(index + 1), index + 1);
		}
	}

	static public class Delete implements iNodeModifier {

		public Delete() {
		}

		public void apply(CachedLine inside, CachedLine.Event event, int index) {

			if (event.method.equals(iLine_m.moveTo_m)) {
				if (inside.events.size() > index + 1) {
					Event next = inside.events.get(index + 1);
					if (next.method.equals(iLine_m.moveTo_m)) {
						inside.events.remove(index);
					} else if (next.method.equals(iLine_m.close_m)) {
						inside.events.remove(index);
						inside.events.remove(index);
					} else {
						Vector2 to = next.getDestination();
						next.method = iLine_m.moveTo_m;
						next.args = new Object[] { to.x, to.y};
						inside.events.remove(index);
					}
				}
			} else if (inside.events.size() > index + 1) {
				if (inside.events.get(index + 1).method.equals(iLine_m.cubicTo_m)) {
					Vector2 p1 = inside.events.get(index + 1).getAt(0);
					Vector2 p = event.getDestination();
					Vector2 n = p1.sub(p).add(inside.events.get(index - 1).getDestination());
					inside.events.get(index + 1).setAt(0, n);
				}
				inside.events.remove(index);
			} else {
				inside.events.remove(index);
			}
		}
	}

	static public class Break implements iNodeModifier {
		public void apply(CachedLine inside, Event event, int index) {

			Event e = inside.new Event();
			e.method = iLine_m.moveTo_m;
			Vector2 v = event.getDestination();
			e.args = new Object[] { v.x, v.y};
			inside.events.add(index + 1, e);
		}
	}

	static public boolean promoteToCubic(CachedLine line, CachedLine.Event in, int index) {
		if (in.method.equals(iLine_m.lineTo_m)) {

			Vector2 from = line.events.get(index - 1).getDestination();
			Vector2 to = in.getDestination();

			Vector2 d1 = new Vector2(to).sub(from).scale(1 / 3).add(from);
			Vector2 d2 = new Vector2(from).sub(to).scale(1 / 3).add(to);

			in.method = iLine_m.cubicTo_m;
			in.args = new Object[] { (float) d1.x, (float) d1.y, (float) d2.x, (float) d2.y, (float) to.x, (float) to.y};

			return true;
		} else if (in.method.equals(iLine_m.cubicTo_m)) { return true; }
		return false;
	}

	static public boolean demoteToLine(CachedLine line, CachedLine.Event in, int index) {
		if (in.method.equals(iLine_m.lineTo_m)) {
			return true;
		} else if (in.method.equals(iLine_m.cubicTo_m)) {
			Vector2 to = in.getDestination();

			in.method = iLine_m.lineTo_m;
			in.args = new Object[] { (float) to.x, (float) to.y};

			return true;
		}
		return false;
	}

}
