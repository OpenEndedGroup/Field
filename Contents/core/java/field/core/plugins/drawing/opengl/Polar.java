package field.core.plugins.drawing.opengl;

import java.util.ArrayList;
import java.util.List;

import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.math.linalg.AxisAngle;
import field.math.linalg.Quaternion;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.namespace.generic.Generics.Pair;

/**
 * tools for generating and filtering lines "polar style"
 * 
 * @author marc
 * 
 */
public class Polar {

	static public class PolarMove {
		public float alpha;

		public float beta1;

		public float beta2;

		public float rMain;

		public float r1;

		public float r2;

		public boolean isBegining = false;
		public Vector2 from = new Vector2();
		public Vector2 to = new Vector2();

		public PolarMove(float alpha, float r) {
			this.alpha = alpha;
			beta1 = 0;
			beta2 = 0;
			this.rMain = r;
			this.r1 = rMain / 3;
			this.r2 = rMain / 3;
		}

		public PolarMove() {
		}

		@Override
		public String toString() {
			return "pm : " + alpha + " (" + beta1 + ", " + beta2 + ") @ " + rMain + " (" + r1 + ", " + r2 + ")";
		}

		public PolarMove copy() {
			PolarMove m = new PolarMove();
			m.alpha = alpha;
			m.rMain = rMain;
			m.beta1 = beta1;
			m.beta2 = beta2;
			m.r1 = r1;
			m.r2 = r2;
			return m;
		}
	}

	static public class PolarOutput {
		private final CachedLine on;

		boolean first = true;

		public Vector2 at;

		public Vector2 heading;

		PolarMove lastMove = null;

		private iLine onInput;

		public PolarOutput(CachedLine on, Vector2 at, Vector2 heading) {
			this.on = on;
			this.onInput = on.getInput();
			if (at == null) {
				assert this.on.events.size() > 0;
				this.at = this.on.events.get(this.on.events.size() - 1).getDestination();
				first = false;
			} else {
				this.at = new Vector2(at);
				this.heading = new Vector2(heading);
			}
		}

		public PolarOutput(Vector2 at, Vector2 heading) {
			this(new CachedLine(), at, heading);
		}

		public Vector2 move(PolarMove move) {

			Vector2 currentlyAt = new Vector2(at);
			Vector2 previouslyAt = null;
			Vector2 currentlyHeading = new Vector2(heading);

			if (first) {
				onInput.moveTo(currentlyAt.x, currentlyAt.y);
				previouslyAt = new Vector2(currentlyAt).sub(currentlyHeading);
				first = false;
			} else {
				previouslyAt = on.events.get(on.events.size() - 1).getDestination();
			}

			Vector2 newHeading = rotate(currentlyHeading, move.alpha, move.rMain);
			Vector2 newAt = new Vector2(currentlyAt).add(newHeading);

			Vector2 c1 = new Vector2(currentlyAt).add(rotate(currentlyHeading, move.beta1, move.r1));
			Vector2 interminC2 = new Vector2(newAt).sub(rotate(newHeading, move.beta2, move.r2));

			// if (on.events.get(on.events.size() -
			// 1).method.equals(iLine_m.cubicTo_m) && lastMove !=
			// null) {
			// Event p = on.events.get(on.events.size() - 1);
			// Vector2 finalC2 = new
			// Vector2(currentlyAt).sub(rotate(currentlyHeading,
			// move.alpha / 2 + lastMove.beta2, lastMove.r2));
			// p.setAt(1, finalC2);
			// }
			onInput.cubicTo(c1.x, c1.y, interminC2.x, interminC2.y, newAt.x, newAt.y);

			at.setValue(newAt);
			heading.set(newHeading);
			lastMove = move;
			return newAt;
		}

		public Vector2 advance(PolarMove move) {
			Vector2 currentlyAt = new Vector2(at);
			Vector2 previouslyAt = null;
			Vector2 currentlyHeading = new Vector2(heading);

			if (first) {
				// onInput.moveTo(currentlyAt.x, currentlyAt.y);
				previouslyAt = new Vector2(currentlyAt).sub(currentlyHeading);
				// first = false;
			} else {
				previouslyAt = on.events.get(on.events.size() - 1).getDestination();
			}

			Vector2 newHeading = rotate(currentlyHeading, move.alpha, move.rMain);
			Vector2 newAt = new Vector2(currentlyAt).add(newHeading);

			Vector2 c1 = new Vector2(currentlyAt).add(rotate(currentlyHeading, move.beta1, move.r1));
			Vector2 interminC2 = new Vector2(newAt).sub(rotate(newHeading, move.beta2, move.r2));

			// if (on.events.get(on.events.size() -
			// 1).method.equals(iLine_m.cubicTo_m) && lastMove !=
			// null) {
			// Event p = on.events.get(on.events.size() - 1);
			// Vector2 finalC2 = new
			// Vector2(currentlyAt).sub(rotate(currentlyHeading,
			// move.alpha / 2 + lastMove.beta2, lastMove.r2));
			// p.setAt(1, finalC2);
			// }

			at.setValue(newAt);
			heading.set(newHeading);
			lastMove = move;

			first = true;

			return newAt;
		}

		public Vector2 speculative(PolarMove m) {
			Vector2 currentlyAt = new Vector2(at);
			Vector2 previouslyAt = null;
			Vector2 currentlyHeading = new Vector2(heading);

			if (first) {
				onInput.moveTo(currentlyAt.x, currentlyAt.y);
				previouslyAt = new Vector2(currentlyAt).sub(currentlyHeading);
			} else {
				previouslyAt = on.events.get(on.events.size() - 1).getDestination();
			}
			Vector2 newHeading = rotate(currentlyHeading, m.alpha, m.rMain);
			Vector2 newAt = new Vector2(currentlyAt).add(newHeading);

			return newAt;
		}

		public CachedLine getOutput() {
			return on;
		}
	}

	public interface iPolarVisitor {
		public void beginSubpath(Vector2 at, Vector2 heading);

		public void visitPolarMove(PolarMove m, PolarFilter in);
	}

	static public class PolarFilter {
		private final CachedLine input;

		CachedLineCursor c = null;

		private PolarOutput on;

		public PolarFilter(CachedLine input) {
			this.input = input;
		}

		public CachedLine visitPolar(iPolarVisitor visit) {
			c = new CachedLineCursor(input);

			System.out.println(" inside visitPolar " + c.hasNextSegment() + " " + input.events);

			List<CachedLine> cl = new ArrayList<CachedLine>();

			Vector2 last = new Vector2();

			boolean nextIsBeginning = false;
			while (c.hasNextSegment()) {

				c.next();

				if (!c.getCurrent().hasDestination())
					continue;

				Event at = c.current;

				Vector2 previous = null;
				Vector2 previousPrevious = null;

				Vector2 pos = c.getCurrent().getDestination();
				if (c.getBefore() == null) {
					if (c.getAfter() == null) {
					} else {
						Vector2 heading = c.getAfter().getDestination().sub(pos);
						visit.beginSubpath(pos, heading);
						on = new PolarOutput(pos, heading);
						cl.add(on.getOutput());
						nextIsBeginning = true;
						continue;
					}
				} else {
					previous = c.getBefore().getDestination();
					previousPrevious = (c.getCurrentIndex() < 2) ? new Vector2(previous).sub(new Vector2(pos).sub(previous)) : input.events.get(c.getCurrentIndex() - 2).getDestination();
				}

				Vector2 oldHeading = new Vector2(previous).sub(previousPrevious);
				Vector2 nowHeading = new Vector2(pos).sub(previous);

				float alpha = angleFor(oldHeading, nowHeading);

				Vector2 c1 = c.getCurrent().method.equals(iLine_m.cubicTo_m) ? c.getCurrent().getAt(0).sub(previous) : new Vector2(pos).sub(previous).scale(1 / 3f);
				Vector2 c2 = c.getCurrent().method.equals(iLine_m.cubicTo_m) ? c.getCurrent().getAt(1).sub(pos) : new Vector2(previous).sub(pos).scale(1 / 3f);

				Vector2 nextHeading = (c.getAfter() != null && c.getAfter().hasDestination()) ? c.getAfter().getDestination(null).sub(pos) : new Vector2(nowHeading);

				float nextAlpha = angleFor(nowHeading, nextHeading);

				float beta1 = angleFor(oldHeading, c1);
				float beta2 = angleFor(nowHeading, new Vector2(c2).scale(-1));

				float r1 = c1.mag();
				float r2 = c2.mag();
				float rMain = nowHeading.mag();

				System.err.println(" polar: at <" + alpha + "> <" + beta1 + "> <" + beta2 + ">");
				System.err.println("             from:" + previousPrevious + " " + previous + " " + pos);
				System.err.println("             headings:" + oldHeading + " " + nowHeading + " " + nextHeading);

				PolarMove pm = new PolarMove();
				pm.to = c.getCurrent().getAt();
				pm.isBegining = nextIsBeginning;
				pm.from = new Vector2(previous);

				pm.alpha = alpha;
				pm.beta1 = beta1;
				pm.beta2 = beta2;
				pm.rMain = rMain;
				pm.r1 = r1;
				pm.r2 = r2;

				visit.visitPolarMove(pm, this);

				on.move(pm);

				nextIsBeginning = false;
			}

			CachedLine clout = new CachedLine();
			for (CachedLine c : cl)
				clout.events.addAll(c.events);
			return clout;
		}
	}

	static public class AllMoves implements iPolarVisitor {
		public List<List<PolarMove>> allMoves = new ArrayList<List<PolarMove>>();
		public List<Pair<Vector2, Vector2>> allStarts = new ArrayList<Pair<Vector2, Vector2>>();

		public void beginSubpath(Vector2 at, Vector2 heading) {
			System.out.println(" begin subpath ");
			Pair<Vector2, Vector2> p = new Pair<Vector2, Vector2>(at, heading);
			allStarts.add(p);
			allMoves.add(new ArrayList<PolarMove>());
		}

		public void visitPolarMove(PolarMove m, PolarFilter in) {
			System.out.println(" move ");
			allMoves.get(allMoves.size() - 1).add(m);
		}
	}

	static public Vector2 rotate(Vector2 currentlyHeading, float alpha, float main) {
		Vector2 q = new Quaternion().set(new Vector3(0, 0, -1), alpha).rotateVector(new Vector2(currentlyHeading));
		float m = q.mag();
		if (m == 0)
			return q;
		q.scale(main / m);
		return q;
	}

	static public float angleFor(Vector2 from, Vector2 to) {
		Quaternion r = new Quaternion(to.toVector3().normalize(), from.toVector3().normalize());
		AxisAngle aa = new AxisAngle().set(r);

		float ret = aa.angle;
		if (aa.z > 0)
			return -ret;
		return ret;
	}

}
