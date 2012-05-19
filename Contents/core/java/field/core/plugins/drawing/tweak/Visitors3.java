package field.core.plugins.drawing.tweak;

import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.plugins.drawing.opengl.CachedLineCursor;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLine_m;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.drawing.tweak.TweakSplineUI.SubSelection;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;

/**
 * visitors (and filter classes) for cached lines
 * 
 * @author marc
 * 
 */
public class Visitors3 {

	static public class BaseFilter3 {
		CachedLineCursor currentInput;

		CachedLineCursor currentOutput;

		CachedLine out = new CachedLine();

		private CachedLine in;

		public BaseFilter3() {
		}

		public CachedLine visitPositions(CachedLine in, PositionVisitor3 v) {
			out = LineUtils.transformLine(in, null, null, null, null);
			CachedLineCursor c = new CachedLineCursor(out);
			currentOutput = c;
			currentInput = new CachedLineCursor(in);
			this.in = in;

			while (c.hasNextSegment()) {
				c.next();
				currentInput.next();
				Event currentEvent = c.getCurrent();
				if (currentEvent.method.equals(iLine_m.moveTo_m)) {
					Vector3 at = LineUtils.getDestination3(currentEvent);
					v.visitPosition(at, SubSelection.postion, this);
					currentEvent.args[0] = at.x;
					currentEvent.args[1] = at.y;
					currentEvent.getAttributes().put(iLinearGraphicsContext.z_v, at.z);
				} else if (currentEvent.method.equals(iLine_m.lineTo_m)) {
					Vector3 at = LineUtils.getDestination3(currentEvent);
					v.visitPosition(at, SubSelection.postion, this);
					currentEvent.args[0] = at.x;
					currentEvent.args[1] = at.y;
					currentEvent.getAttributes().put(iLinearGraphicsContext.z_v, at.z);
				} else if (currentEvent.method.equals(iLine_m.cubicTo_m)) {
					Vector3 at0 = LineUtils.getControl1(currentEvent);
					v.visitPosition(at0, SubSelection.nextControl, this);
					currentEvent.args[0] = at0.x;
					currentEvent.args[1] = at0.y;

					Vector3 at1 = LineUtils.getControl2(currentEvent);
					v.visitPosition(at1, SubSelection.previousControl, this);
					currentEvent.args[2] = at1.x;
					currentEvent.args[3] = at1.y;

					Vector3 at2 = LineUtils.getDestination3(currentEvent);
					v.visitPosition(at2, SubSelection.postion, this);
					currentEvent.args[4] = at2.x;
					currentEvent.args[5] = at2.y;

					currentEvent.getAttributes().put(iLinearGraphicsContext.z_v, new Vector3(at0.z, at1.z, at2.z));
				}
			}

			return out;
		}

		public CachedLine visitNodes(CachedLine in, NodeVisitor3 v) {
			out = LineUtils.transformLine(in, null, null, null, null);
			CachedLineCursor c = new CachedLineCursor(out);
			currentOutput = c;
			currentInput = new CachedLineCursor(in);
			this.in = in;

			while (c.hasNextSegment()) {
				currentInput.next();
				c.next();

				Event currentEvent = c.getCurrent();

				Vector3 last = null;
				boolean lastIsTangent = false;
				Event lastEventToSet = null;
				int lastIndexToSet = 0;

				Vector3 current = null;
				Event currentEventToSet = null;
				int currentIndexToSet = 0;

				Vector3 next = null;
				boolean nextIsTangent = false;
				Event nextEventToSet = null;
				int nextIndexToSet = 0;

				if (currentEvent.method.equals(iLine_m.moveTo_m)) {
					// skip forward to find any close

					boolean found = false;
					int foundAt = 0;
					for (int ci = c.getCurrentIndex() + 1; ci < out.events.size(); ci++) {
						if (out.events.get(ci).method.equals(iLine_m.close_m)) {
							Vector3 closeAt = LineUtils.getDestination3(out.events.get(ci - 1));
							if (closeAt.distanceFrom(LineUtils.getDestination3(currentEvent))< 1e-5) {
								if (ci > c.getCurrentIndex() + 1) {
									foundAt = ci - 2;
									found = true;
								}
							} else {
								foundAt = ci - 1;
								found = true;
							}
						} else if (out.events.get(ci).method.equals(iLine_m.moveTo_m))
							break;
					}

					if (found) {
						last = out.events.get(foundAt).getDestination3();
						lastEventToSet = out.events.get(foundAt);
						lastIndexToSet = -1;
					}

					current = currentEvent.getDestination3();
					currentEventToSet = currentEvent;
					currentIndexToSet = -1;
				} else if (currentEvent.method.equals(iLine_m.lineTo_m)) {
					last = c.getBefore().getDestination3();
					lastIsTangent = false;
					lastIndexToSet = -1;
					lastEventToSet = c.getBefore();

					current = currentEvent.getDestination3();
					currentEventToSet = currentEvent;
					currentIndexToSet = -1;

				} else if (currentEvent.method.equals(iLine_m.cubicTo_m)) {
					last = LineUtils.getControl2(currentEvent);
					lastIsTangent = true;
					lastIndexToSet = 1;
					lastEventToSet = currentEvent;

					current = currentEvent.getDestination3();
					currentEventToSet = currentEvent;
					currentIndexToSet = -1;
				} else {
					continue;
				}

				if (c.getAfter() != null) {
					Event after = c.getAfter();
					if (after.method.equals(iLine_m.cubicTo_m)) {
						next = LineUtils.getControl1(after);
						nextIsTangent = true;
						nextIndexToSet = 0;
						nextEventToSet = after;
					} else {
						next = after.getDestination3();
						nextIsTangent = false;
						nextIndexToSet = -1;
						nextEventToSet = after;
					}
				}

				System.out.println(last + "\n   " + current + "\n       " + next);

				v.visitNode(last, current, next, lastIsTangent, nextIsTangent);

				if (last != null && lastIsTangent)
				{
					lastEventToSet.setAt3(lastIndexToSet, last);
				}
				if (current != null)
					currentEventToSet.setAt3(currentIndexToSet, current);
				if (next != null && nextIsTangent)
					nextEventToSet.setAt3(nextIndexToSet, next);

			}

			return out;
		}

	}

	public interface PositionVisitor3 {
		public void visitPosition(Vector3 v, SubSelection part, BaseFilter3 inside);
	}

	public interface NodeVisitor3 {
		public void visitNode(Vector3 before, Vector3 now, Vector3 after, boolean beforeIsTangent, boolean afterIsTangent);
	}

}
