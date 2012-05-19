package field.core.plugins.drawing.tweak;

import java.lang.reflect.InvocationTargetException;

import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLineCursor;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLine;
import field.core.plugins.drawing.opengl.iLine_m;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.plugins.drawing.tweak.TweakSplineUI.SubSelection;
import field.math.linalg.Vector2;

/**
 * visitors (and filter classes) for cached lines
 * 
 * @author marc
 * 
 */
public class Visitors {

	static public class BaseFilter {
		CachedLineCursor currentInput;

		CachedLineCursor currentOutput;

		CachedLine out = new CachedLine();

		private CachedLine in;

		public BaseFilter() {
		}

		public CachedLine visitPositions(CachedLine in, PositionVisitor v) {
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
					Vector2 at = new Vector2(((Number) currentEvent.args[0]).floatValue(), ((Number) currentEvent.args[1]).floatValue());
					v.visitPosition(at, SubSelection.postion, this);
					currentEvent.args[0] = at.x;
					currentEvent.args[1] = at.y;
				} else if (currentEvent.method.equals(iLine_m.lineTo_m)) {
					Vector2 at = new Vector2(((Number) currentEvent.args[0]).floatValue(), ((Number) currentEvent.args[1]).floatValue());
					v.visitPosition(at, SubSelection.postion, this);
					currentEvent.args[0] = at.x;
					currentEvent.args[1] = at.y;
				} else if (currentEvent.method.equals(iLine_m.cubicTo_m)) {
					Vector2 at = new Vector2(((Number) currentEvent.args[2]).floatValue(), ((Number) currentEvent.args[3]).floatValue());
					v.visitPosition(at, SubSelection.previousControl, this);
					currentEvent.args[2] = at.x;
					currentEvent.args[3] = at.y;
					at = new Vector2(((Number) currentEvent.args[4]).floatValue(), ((Number) currentEvent.args[5]).floatValue());
					v.visitPosition(at, SubSelection.postion, this);
					currentEvent.args[4] = at.x;
					currentEvent.args[5] = at.y;
				}
				if (c.getAfter() != null && c.getAfter().method.equals(iLine_m.cubicTo_m)) {
					Vector2 at = new Vector2(((Number) c.getAfter().args[0]).floatValue(), ((Number) c.getAfter().args[1]).floatValue());
					v.visitPosition(at, SubSelection.nextControl, this);
					c.getAfter().args[0] = at.x;
					c.getAfter().args[1] = at.y;
				}
			}

			return out;
		}

		public CachedLine visitNodes(CachedLine in, NodeVisitor v) {
			out = LineUtils.transformLine(in, null, null, null, null);
			CachedLineCursor c = new CachedLineCursor(out);
			currentOutput = c;
			currentInput = new CachedLineCursor(in);
			this.in = in;

			while (c.hasNextSegment()) {
				currentInput.next();
				c.next();

				Event currentEvent = c.getCurrent();

				Vector2 last = null;
				boolean lastIsTangent = false;
				Event lastEventToSet = null;
				int lastIndexToSet = 0;

				Vector2 current = null;
				Event currentEventToSet = null;
				int currentIndexToSet = 0;

				Vector2 next = null;
				boolean nextIsTangent = false;
				Event nextEventToSet = null;
				int nextIndexToSet = 0;

				if (currentEvent.method.equals(iLine_m.moveTo_m)) {
					// skip forward to find any close

					boolean found = false;
					int foundAt = 0;
					for (int ci = c.getCurrentIndex() + 1; ci < out.events.size(); ci++) {
						if (out.events.get(ci).method.equals(iLine_m.close_m)) {
							Vector2 closeAt = out.events.get(ci - 1).getDestination();
							if (closeAt.distanceFrom(currentEvent.getDestination()) < 1e-5) {
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
						last = out.events.get(foundAt).getDestination();
						lastEventToSet = out.events.get(foundAt);
						lastIndexToSet = -1;
					}

					current = currentEvent.getDestination();
					currentEventToSet = currentEvent;
					currentIndexToSet = -1;
				} else if (currentEvent.method.equals(iLine_m.lineTo_m)) {
					last = c.getBefore().getDestination();
					lastIsTangent = false;
					lastIndexToSet = -1;
					lastEventToSet = c.getBefore();

					current = currentEvent.getDestination();
					currentEventToSet = currentEvent;
					currentIndexToSet = -1;

				} else if (currentEvent.method.equals(iLine_m.cubicTo_m)) {
					last = currentEvent.getAt(1);
					lastIsTangent = true;
					lastIndexToSet = 1;
					lastEventToSet = currentEvent;

					current = currentEvent.getDestination();
					currentEventToSet = currentEvent;
					currentIndexToSet = -1;
				} else {
					continue;
				}

				if (c.getAfter() != null) {
					Event after = c.getAfter();
					if (after.method.equals(iLine_m.cubicTo_m)) {
						next = after.getAt(0);
						nextIsTangent = true;
						nextIndexToSet = 0;
						nextEventToSet = after;
					} else {
						next = after.getDestination();
						nextIsTangent = false;
						nextIndexToSet = -1;
						nextEventToSet = after;
					}
				}

				System.out.println(last + "\n   " + current + "\n       " + next);

				v.visitNode(last, current, next, lastIsTangent, nextIsTangent);

				if (last != null && lastIsTangent)
					lastEventToSet.setAt(lastIndexToSet, last);
				if (current != null)
					currentEventToSet.setAt(currentIndexToSet, current);
				if (next != null && nextIsTangent)
					nextEventToSet.setAt(nextIndexToSet, next);

			}

			return out;
		}

		public CachedLine visitRewriting(CachedLine l, WritingVisitor ret) {
			CachedLine out = new CachedLine();
			iLine line = out.getInput();
			currentOutput = null;
			currentInput = new CachedLineCursor(l);

			WritingReturn previousReturn = null;
			while (currentInput.hasNextSegment()) {
				currentInput.next();

				WritingReturn r = ret.rewrite(currentInput.before, currentInput.current, currentInput.after, this);
				System.err.println(" has next segment <" + currentInput.getCurrentIndex() + "> <" + previousReturn + " " + r + ">");

				if (currentInput.current.method.equals(iLine_m.moveTo_m))
					previousReturn = null;

				boolean set = false;

				if (r == null) {
					if (previousReturn == null || previousReturn.needsRightTangent() == Needs.dontcare || (previousReturn.needsRightTangent() == Needs.no && !currentInput.current.method.equals(iLine_m.cubicTo_m)) || currentInput.current.method.equals(iLine_m.close_m) || currentInput.current.method.equals(iLine_m.moveTo_m)) {
						try {

							System.err.println(" -- passthrough -- ");

							currentInput.current.method.invoke(line, currentInput.current.args);
							set = true;
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						}
					} else {
						if ((previousReturn.needsRightTangent() == Needs.yes && currentInput.current.method.equals(iLine_m.cubicTo_m))) {
							// need to overright
							// outgoing tangent

							Vector2 oldTangent = currentInput.current.getAt(0);
							Vector2 newTangent = previousReturn.setRightTangent(oldTangent, currentInput.previous());
							currentInput.next();

							Vector2 o2 = currentInput.current.getAt(1);
							Vector2 d = currentInput.current.getAt(2);

							line.cubicTo(newTangent.x, newTangent.y, o2.x, o2.y, d.x, d.y);
							set = true;
						} else if ((previousReturn.needsRightTangent() == Needs.yes && !currentInput.current.method.equals(iLine_m.cubicTo_m))) {
							// need to upgrade this
							// from a line segment

							Vector2 newTangent = previousReturn.setRightTangent(null, currentInput.previous());
							currentInput.next();
							if (newTangent != null) {
								Vector2 d = currentInput.current.getDestination();
								Vector2 linear = new Vector2(d).sub(out.events.get(out.events.size() - 1).getDestination()).scale(1 / 3f).add(d);
								line.cubicTo(newTangent.x, newTangent.y, linear.x, linear.y, d.x, d.y);
								set = true;
							} else {
								try {
									currentInput.current.method.invoke(line, currentInput.current.args);
									set = true;
								} catch (IllegalArgumentException e) {
									e.printStackTrace();
								} catch (IllegalAccessException e) {
									e.printStackTrace();
								} catch (InvocationTargetException e) {
									e.printStackTrace();
								}
							}
						} else if ((previousReturn.needsRightTangent() == Needs.no && currentInput.current.method.equals(iLine_m.cubicTo_m))) {

							// downgrade to linear
							Vector2 d = currentInput.current.getDestination();
							line.lineTo(d.x, d.y);
							set = true;
						}
					}

				} else {
					if (currentInput.current.method.equals(iLine_m.cubicTo_m)) {

						if (r.needsLeftTangent() == Needs.yes) {
							if (previousReturn == null || previousReturn.needsRightTangent() != Needs.no) {
								// don't
								// downgrade

								Vector2 c1 = new Vector2(currentInput.current.getAt(0));
								if (previousReturn != null && previousReturn.needsRightTangent() == Needs.yes) {
									previousReturn.setRightTangent(c1, currentInput.previous());
									currentInput.next();
								}

								Vector2 c2 = new Vector2(currentInput.current.getAt(1));
								r.setLeftTangent(c2, currentInput);
								Vector2 pos = new Vector2(currentInput.current.getAt(2));
								r.setPosition(pos, currentInput);

								line.cubicTo(c1.x, c1.y, c2.x, c2.y, pos.x, pos.y);
								set = true;
							} else {
								Vector2 c1 = currentInput.current.getDestination().sub(new Vector2(currentInput.before.getDestination())).scale(1 / 3f).add(currentInput.before.getDestination());

								Vector2 c2 = new Vector2(currentInput.current.getAt(1));
								r.setLeftTangent(c2, currentInput);
								Vector2 pos = new Vector2(currentInput.current.getAt(2));
								r.setPosition(pos, currentInput);

								line.cubicTo(c1.x, c1.y, c2.x, c2.y, pos.x, pos.y);
								set = true;
							}
						} else if (r.needsLeftTangent() == Needs.no) {
							if (previousReturn == null || previousReturn.needsRightTangent() != Needs.yes) {
								// downgrade

								Vector2 c = currentInput.current.getDestination();
								r.setPosition(c, currentInput);
								line.lineTo(c.x, c.y);
								set = true;
							} else {
								// passthrough
								// with previous
								// tangent. no
								// incomming

								Vector2 c1 = new Vector2(currentInput.current.getAt(0));
								previousReturn.setRightTangent(c1, currentInput.previous());
								currentInput.next();

								Vector2 c2 = currentInput.current.getDestination().sub(new Vector2(currentInput.before.getDestination())).scale(-1 / 3f).add(currentInput.current.getDestination());
								Vector2 pos = new Vector2(currentInput.current.getAt(2));
								r.setPosition(pos, currentInput);

								line.cubicTo(c1.x, c1.y, c2.x, c2.y, pos.x, pos.y);
								set = true;
							}
						} else if (r.needsLeftTangent() == Needs.dontcare) {
							if (previousReturn != null && previousReturn.needsRightTangent() == Needs.yes) {
								// passthrough
								// with previous
								// tangent. no
								// incomming

								Vector2 c1 = new Vector2(currentInput.current.getAt(0));
								previousReturn.setRightTangent(c1, currentInput.previous());
								currentInput.next();

								Vector2 c2 = new Vector2(currentInput.current.getAt(1));
								Vector2 pos = new Vector2(currentInput.current.getAt(2));
								r.setPosition(pos, currentInput);

								line.cubicTo(c1.x, c1.y, c2.x, c2.y, pos.x, pos.y);
								set = true;
							} else {
								Vector2 c1 = new Vector2(currentInput.current.getAt(0));
								if (previousReturn != null && previousReturn.needsRightTangent() == Needs.no) {
									c1 = currentInput.current.getDestination().sub(new Vector2(currentInput.before.getDestination())).scale(1 / 3f).add(currentInput.before.getDestination());
								}

								Vector2 c2 = new Vector2(currentInput.current.getAt(1));
								Vector2 pos = new Vector2(currentInput.current.getAt(2));
								r.setPosition(pos, currentInput);

								line.cubicTo(c1.x, c1.y, c2.x, c2.y, pos.x, pos.y);
								set = true;
							}
						}
					} else if (currentInput.current.method.equals(iLine_m.lineTo_m)) {
						if (r.needsLeftTangent() == Needs.yes) {
							// upgrade
							if (previousReturn == null || previousReturn.needsRightTangent() != Needs.yes) {
								// upgrade, with
								// no outgoing
								// tangent from
								// previous

								Vector2 c1 = currentInput.current.getDestination().sub(new Vector2(currentInput.before.getDestination())).scale(1 / 3f).add(currentInput.before.getDestination());

								Vector2 c2 = r.setLeftTangent(null, currentInput);
								Vector2 pos = new Vector2(currentInput.current.getDestination());
								r.setPosition(pos, currentInput);

								line.cubicTo(c1.x, c1.y, c2.x, c2.y, pos.x, pos.y);
								set = true;

							} else {
								Vector2 c1 = previousReturn.setRightTangent(null, currentInput.previous());
								currentInput.next();
								Vector2 c2 = r.setLeftTangent(null, currentInput);
								Vector2 pos = new Vector2(currentInput.current.getDestination());
								r.setPosition(pos, currentInput);

								line.cubicTo(c1.x, c1.y, c2.x, c2.y, pos.x, pos.y);
								set = true;
							}
						} else if (r.needsLeftTangent() == Needs.no || r.needsLeftTangent() == Needs.dontcare) {
							if (previousReturn == null || previousReturn.needsRightTangent() != Needs.yes) {
								Vector2 pos = r.setPosition(currentInput.current.getDestination(), currentInput);
								line.lineTo(pos.x, pos.y);
								set = true;
							} else if (previousReturn.needsRightTangent() == Needs.yes) {
								// upgrade with
								// no incomming
								// tangent

								Vector2 c1 = null;
								if (previousReturn.needsRightTangent() == Needs.yes) {
									c1 = previousReturn.setRightTangent(c1, currentInput.previous());
									currentInput.next();
								}

								Vector2 c2 = currentInput.current.getDestination().sub(new Vector2(currentInput.before.getDestination())).scale(-1 / 3f).add(currentInput.current.getDestination());
								Vector2 pos = new Vector2(currentInput.current.getAt(2));
								r.setPosition(pos, currentInput);

								line.cubicTo(c1.x, c1.y, c2.x, c2.y, pos.x, pos.y);
								set = true;

							}
						}
					} else if (currentInput.current.method.equals(iLine_m.moveTo_m)) {
						Vector2 p = r.setPosition(currentInput.current.getDestination(), currentInput);
						line.moveTo(p.x, p.y);
						set = true;
					} else if (currentInput.current.method.equals(iLine_m.close_m)) {
						line.close();
						set = true;
					}
				}

				assert set : info(r) + " " + info(previousReturn);
				previousReturn = r;

			}

			return out;
		}

		public boolean isCubicInto() {
			return currentInput.current.method.equals(iLine_m.cubicTo_m);
		}

		public boolean isCubicOutOf() {
			return currentInput.after == null ? false : currentInput.after.method.equals(iLine_m.cubicTo_m);
		}

		public String info(WritingReturn previousReturn) {
			return previousReturn.needsLeftTangent() + " " + previousReturn.needsRightTangent() + " " + previousReturn.getClass();
		}

	}

	public interface PositionVisitor {
		public void visitPosition(Vector2 v, SubSelection part, BaseFilter inside);
	}

	public interface NodeVisitor {
		public void visitNode(Vector2 before, Vector2 now, Vector2 after, boolean beforeIsTangent, boolean afterIsTangent);
	}

	public enum Needs {
		no, yes, dontcare;
	}

	// python can't enum yet, and, anyway, some of this is dynamic
	static public interface WritingReturn {

		public Needs needsLeftTangent();

		public Needs needsRightTangent();

		// note: these might get called even if needsXTangent has
		// returned no, in the case where it's impossible to get rid of
		// the tangent altogether
		public Vector2 setLeftTangent(Vector2 out, CachedLineCursor now);

		public Vector2 setRightTangent(Vector2 out, CachedLineCursor now);

		public Vector2 setPosition(Vector2 out, CachedLineCursor now);
	}

	static public class Corner implements WritingReturn {
		final Vector2 left;

		final Vector2 right;

		// set either of these to None to actually make a "corner"
		public Corner(Vector2 left, Vector2 right) {
			this.left = left;
			this.right = right;
		}

		public Needs needsLeftTangent() {
			if (left == null)
				return Needs.no;
			return Needs.dontcare;
		}

		public Vector2 setLeftTangent(Vector2 out, CachedLineCursor now) {
			if (out == null)
				return null;
			if (left != null)
				out.setValue(left);
			else {
				if (now.before == null) {
					assert false;
					return null;
				}

				Vector2 beforeAt = now.before.getDestination(null);
				Vector2 hereAt = now.current.getDestination(null);
				beforeAt.sub(hereAt).scale(1 / 3f).add(hereAt);
				out.setValue(beforeAt);
			}
			return out;
		}

		public Needs needsRightTangent() {
			if (right == null)
				return Needs.no;
			return Needs.dontcare;
		}

		public Vector2 setRightTangent(Vector2 out, CachedLineCursor now) {
			if (out == null)
				return null;
			if (left != null)
				out.setValue(right);
			else {
				if (now.after == null) {
					assert false;
					return null;
				}

				Vector2 nextAt = now.after.getDestination(null);
				Vector2 hereAt = now.current.getDestination(null);
				nextAt.sub(hereAt).scale(1 / 3f).add(hereAt);
				out.setValue(nextAt);
			}
			return out;
		}

		public Vector2 setPosition(Vector2 out, CachedLineCursor now) {
			return out;
		}
	}

	// for ApplyTool
	static public class TCorner extends Corner {
		public TCorner() {
			super(null, null);
		}
	}

	static public class Smooth implements WritingReturn {
		final Vector2 left;

		final Vector2 right;

		private final float amount;

		// set either of these to some non-None value to move the
		// tangent, otherwise it becomes smooth
		public Smooth(Vector2 left, Vector2 right, float amount) {
			this.left = left;
			this.right = right;
			this.amount = amount;
		}

		public Needs needsLeftTangent() {
			return Needs.yes;
		}

		public Needs needsRightTangent() {
			return Needs.yes;
		}

		public Vector2 setLeftTangent(Vector2 out, CachedLineCursor now) {

			System.err.println(" set left tangent for " + now.getCurrentIndex() + " " + now.current.getDestination() + " " + out);

			if (left != null) {
				if (out != null) {
					out.setValue(left);
					return out;
				}
				return left;
			}

			Vector2 nextAt = now.after == null ? null : now.getNextDestination(null);
			Vector2 beforeAt = now.before == null ? null : now.before.getDestination(null);
			Vector2 hereAt = now.current.getDestination(null);

			System.err.println("        " + nextAt + " " + hereAt + " " + beforeAt);

			if (nextAt == null) {
				if (beforeAt == null)
					return out;

				beforeAt.sub(hereAt).scale(1 / 3f).add(hereAt);
				if (out != null)
					beforeAt.interpolate(out, 1 - amount);
				if (out == null) {
					return beforeAt;
				}
				out.setValue(beforeAt);

				System.err.println("       no next <" + out + ">");

				return out;
			}

			if (beforeAt == null) {
				if (nextAt == null)
					return out;

				nextAt.sub(hereAt).scale(-1 / 3f).add(hereAt);
				if (out != null)
					nextAt.interpolate(out, 1 - amount);
				if (out == null) {
					return nextAt;
				}
				out.setValue(nextAt);

				System.err.println("       no before <" + out + ">");

				return out;
			}

			float d = beforeAt.distanceFrom(hereAt);

			beforeAt.sub(hereAt).normalize();
			nextAt.sub(hereAt).normalize().scale(-1);

			System.err.println(" beforeAt:" + beforeAt + " " + nextAt);

			beforeAt.interpolate(nextAt, 0.5f);
			beforeAt.scale(d / (3 * beforeAt.mag()));
			beforeAt.add(hereAt);

			System.err.println(" afterage <" + beforeAt + ">");

			if (out != null)
				beforeAt.interpolate(out, 1 - amount);
			if (out == null) {
				return beforeAt;
			}
			out.setValue(beforeAt);
			return out;
		}

		public Vector2 setRightTangent(Vector2 out, CachedLineCursor now) {

			System.err.println(" set right tangent for " + now.getCurrentIndex() + " " + now.current.getDestination() + " " + out);

			if (left != null) {
				if (out != null) {
					out.setValue(right);
					return out;
				}
				return right;
			}

			Vector2 nextAt = now.after == null ? null : now.after.getDestination(null);
			Vector2 beforeAt = now.before == null ? null : now.before.getDestination(null);
			Vector2 hereAt = now.current.getDestination(null);

			System.err.println("        " + nextAt + " " + hereAt + " " + beforeAt);

			if (nextAt == null) {
				if (beforeAt == null)
					return out;

				beforeAt.sub(hereAt).scale(-1 / 3f).add(hereAt);
				if (out != null)
					beforeAt.interpolate(out, 1 - amount);
				if (out == null) {
					return beforeAt;
				}
				out.setValue(beforeAt);
				return out;
			}

			if (beforeAt == null) {
				if (nextAt == null)
					return out;

				nextAt.sub(hereAt).scale(1 / 3f).add(hereAt);
				if (out != null)
					nextAt.interpolate(out, 1 - amount);
				if (out == null) {
					return nextAt;
				}
				out.setValue(nextAt);
				return out;
			}

			float d = nextAt.distanceFrom(hereAt);

			beforeAt.sub(hereAt).normalize().scale(-1);
			nextAt.sub(hereAt).normalize();

			System.err.println(" beforeAt:" + beforeAt + " " + nextAt);

			beforeAt.interpolate(nextAt, 0.5f);
			beforeAt.scale(d / (3 * beforeAt.mag()));
			beforeAt.add(hereAt);

			System.err.println(" afterage <" + beforeAt + ">");

			if (out != null)
				beforeAt.interpolate(out, 1 - amount);
			if (out == null) {
				return beforeAt;
			}
			out.setValue(beforeAt);
			return out;
		}

		public Vector2 setPosition(Vector2 out, CachedLineCursor now) {
			return out;
		}

	}

	static public class TSmooth extends Smooth {
		public TSmooth() {
			super(null, null, 1);
		}
	}

	static public class NormalizeTangentLength implements WritingReturn {
		private final float normalizeAmount;

		private final float normalizeTo;

		private final float equalAmount;

		private final float applyAmount;

		public NormalizeTangentLength(float normalizeAmount, float normalizeTo, float equalAmount, float applyAmount) {
			this.normalizeAmount = normalizeAmount;
			this.normalizeTo = normalizeTo;
			this.equalAmount = equalAmount;
			this.applyAmount = applyAmount;
		}

		public Needs needsLeftTangent() {
			return Needs.dontcare;
		}

		public Needs needsRightTangent() {
			return Needs.dontcare;
		}

		public Vector2 setLeftTangent(Vector2 out, CachedLineCursor now) {

			Vector2 nextAt = now.after == null ? null : now.after.getDestination(null);
			Vector2 beforeAt = now.before == null ? null : now.before.getDestination(null);
			Vector2 hereAt = now.current.getDestination(null);

			if (nextAt == null) {
				if (beforeAt == null)
					return out;
				float m = beforeAt.sub(hereAt).mag();
				if (m < 1e-10)
					return out;

				float m2 = m * (1 - normalizeAmount) + normalizeAmount * normalizeTo * m;
				if (m2 < 1 - 10)
					return out;

				beforeAt.sub(hereAt).scale(m2 / m).add(hereAt);
				if (out == null)
					return beforeAt;
				out.interpolate(beforeAt, applyAmount);
				return out;
			} else if (beforeAt == null) {
				if (nextAt == null)
					return out;

				float m = nextAt.sub(hereAt).mag();
				if (m < 1e-10)
					return out;

				float m2 = m * (1 - normalizeAmount) + normalizeAmount * normalizeTo * m;
				if (m2 < 1 - 10)
					return out;

				nextAt.sub(hereAt).scale(-m2 / m).add(hereAt);
				if (out == null)
					return beforeAt;

				out.interpolate(nextAt, applyAmount);
				return out;
			}

			float m1 = beforeAt.sub(hereAt).mag();
			float m2 = nextAt.sub(hereAt).mag();

			float m = m1 * (1 - equalAmount) + equalAmount * (m1 + m2) / 2;

			float m3 = m * (1 - normalizeAmount) + normalizeAmount * normalizeTo * m;
			if (m3 < 1 - 10)
				return out;
			if (m1 < 1 - 10)
				return out;

			beforeAt.sub(hereAt).scale(m3 / m1).add(hereAt);
			if (out == null)
				return beforeAt;
			out.interpolate(beforeAt, applyAmount);
			return out;
		}

		public Vector2 setRightTangent(Vector2 out, CachedLineCursor now) {

			Vector2 nextAt = now.after == null ? null : now.after.getDestination(null);
			Vector2 beforeAt = now.before == null ? null : now.before.getDestination(null);
			Vector2 hereAt = now.current.getDestination(null);

			if (nextAt == null) {
				if (beforeAt == null)
					return out;
				float m = beforeAt.sub(hereAt).mag();
				if (m < 1e-10)
					return out;

				float m2 = m * (1 - normalizeAmount) + normalizeAmount * normalizeTo * m;
				if (m2 < 1 - 10)
					return out;

				beforeAt.sub(hereAt).scale(-m2 / m).add(hereAt);
				if (out == null)
					return beforeAt;
				out.interpolate(beforeAt, applyAmount);
				return out;
			} else if (beforeAt == null) {
				if (nextAt == null)
					return out;

				float m = nextAt.sub(hereAt).mag();
				if (m < 1e-10)
					return out;

				float m2 = m * (1 - normalizeAmount) + normalizeAmount * normalizeTo * m;
				if (m2 < 1 - 10)
					return out;

				nextAt.sub(hereAt).scale(m2 / m).add(hereAt);
				if (out == null)
					return beforeAt;

				out.interpolate(nextAt, applyAmount);
				return out;
			}

			float m1 = beforeAt.sub(hereAt).mag();
			float m2 = nextAt.sub(hereAt).mag();

			float m = m1 * (1 - equalAmount) + equalAmount * (m1 + m2) / 2;

			float m3 = m * (1 - normalizeAmount) + normalizeAmount * normalizeTo * m;
			if (m3 < 1 - 10)
				return out;
			if (m1 < 1 - 10)
				return out;

			nextAt.sub(hereAt).scale(m2 / m1).add(hereAt);
			if (out == null)
				return beforeAt;
			out.interpolate(nextAt, applyAmount);
			return out;
		}

		public Vector2 setPosition(Vector2 out, CachedLineCursor now) {
			return out;
		}
	}

	static public class TNormalizeTangentLength extends NormalizeTangentLength {
		public TNormalizeTangentLength() {
			super(1, 0.33f, 0, 1f);
		}
	}

	static public class ContinueLeftward implements WritingReturn {
		final float scaleTotal;

		final float scaleInOut;

		private final float applyAmount;

		public ContinueLeftward(float scaleTotal, float scaleInOut, float applyAmount) {
			this.scaleTotal = scaleTotal;
			this.scaleInOut = scaleInOut;
			this.applyAmount = applyAmount;
		}

		public Needs needsLeftTangent() {
			return Needs.yes;
		}

		public Needs needsRightTangent() {
			return Needs.dontcare;
		}

		public Vector2 setLeftTangent(Vector2 out, CachedLineCursor now) {

			Vector2 nextAt = now.after == null ? null : (now.after.method.equals(iLine_m.cubicTo_m) ? now.after.getAt(0) : now.after.getDestination(null));
			Vector2 hereAt = now.current.getDestination(null);

			if (nextAt == null)
				return out;

			hereAt.sub(nextAt).scale(now.after.method.equals(iLine_m.cubicTo_m) ? -1 : -1 / 3f).add(hereAt);

			if (out != null) {
				out.interpolate(hereAt, applyAmount);
				return out;
			}
			return hereAt;
		}

		public Vector2 setRightTangent(Vector2 out, CachedLineCursor now) {
			return out;
		}

		public Vector2 setPosition(Vector2 out, CachedLineCursor now) {
			return out;
		}
	}

	static public class TContinueLeftward extends ContinueLeftward {
		public TContinueLeftward() {
			super(1, 1, 1);
		}
	}

	static public class ContinueRightward implements WritingReturn {
		final float scaleTotal;

		final float scaleInOut;

		private final float applyAmount;

		public ContinueRightward(float scaleTotal, float scaleInOut, float applyAmount) {
			this.scaleTotal = scaleTotal;
			this.scaleInOut = scaleInOut;
			this.applyAmount = applyAmount;
		}

		public Needs needsRightTangent() {
			return Needs.yes;
		}

		public Needs needsLeftTangent() {
			return Needs.dontcare;
		}

		public Vector2 setRightTangent(Vector2 out, CachedLineCursor now) {

			Vector2 beforeAt = now.before == null ? null : (now.current.method.equals(iLine_m.cubicTo_m) ? now.current.getAt(1) : now.before.getDestination(null));
			Vector2 hereAt = now.current.getDestination(null);

			if (beforeAt == null)
				return out;

			hereAt.sub(beforeAt).scale(now.current.method.equals(iLine_m.cubicTo_m) ? -1 : -1 / 3f).add(hereAt);

			if (out != null) {
				out.interpolate(hereAt, applyAmount);
				return out;
			}
			return hereAt;
		}

		public Vector2 setLeftTangent(Vector2 out, CachedLineCursor now) {
			return out;
		}

		public Vector2 setPosition(Vector2 out, CachedLineCursor now) {
			return out;
		}

	}

	static public class TContinueRightward extends ContinueLeftward {
		public TContinueRightward() {
			super(1, 1, 1);
		}
	}

	// float t1 = (float)
	// (0.5*((1-tcbm2.x)*(1-tcbm2.y)*(1+tcbm2.z)*(next-now)+
	// (1-tcbm2.x)*(1+tcbm2.y)*(1-tcbm2.z)*(after-next))*tcbm2.w);
	// float t2 = (float)
	// (0.5*((1-tcbm1.x)*(1+tcbm1.y)*(1+tcbm1.z)*(now-before)+
	// (1-tcbm1.x)*(1-tcbm1.y)*(1-tcbm1.z)*(next-now))*tcbm1.w);

	static public class TCB implements WritingReturn {
		private final float[] tcb1;

		private final boolean changeLeft;

		private final boolean changeRight;

		private final float[] tcb2;

		public TCB(float t, float c, float b) {
			this(new float[] { t, c, b }, true, new float[] { t, c, b }, true);
		}

		public TCB(float[] tcb1, boolean changeLeft, float[] tcb2, boolean changeRight) {
			this.tcb1 = tcb1;
			this.changeLeft = changeLeft;
			this.tcb2 = tcb2;
			this.changeRight = changeRight;
		}

		public Needs needsLeftTangent() {
			return changeLeft ? Needs.yes : Needs.dontcare;
		}

		public Needs needsRightTangent() {
			return changeRight ? Needs.yes : Needs.dontcare;
		}

		public Vector2 setLeftTangent(Vector2 out, CachedLineCursor now) {
			if (!changeLeft)
				return out;

			Vector2 nextAt = now.after == null ? null : now.after.getDestination(null);
			Vector2 beforeAt = now.before == null ? null : now.before.getDestination(null);
			Vector2 hereAt = now.current.getDestination(null);

			if (nextAt == null) {
				if (beforeAt == null)
					return out;
				else
					nextAt = new Vector2(hereAt).sub(beforeAt).add(hereAt);
			}
			if (beforeAt == null)
				beforeAt = new Vector2(hereAt).sub(nextAt).scale(-1).add(hereAt);

			Vector2 o = new Vector2();
			o.x = hereAt.x - 0.5f * ((1 - tcb1[0]) * (1 + tcb1[1]) * (1 + tcb1[2]) * (hereAt.x - beforeAt.x) + (1 - tcb1[0]) * (1 - tcb1[1]) * (1 - tcb1[2]) * (nextAt.x - hereAt.y)) / 3;
			o.y = hereAt.y - 0.5f * ((1 - tcb1[0]) * (1 + tcb1[1]) * (1 + tcb1[2]) * (hereAt.y - beforeAt.y) + (1 - tcb1[0]) * (1 - tcb1[1]) * (1 - tcb1[2]) * (nextAt.y - hereAt.y)) / 3;

			if (out == null)
				return o;
			out.setValue(o);
			return out;
		}

		public Vector2 setRightTangent(Vector2 out, CachedLineCursor now) {
			if (!changeRight)
				return out;

			Vector2 nextAt = now.after == null ? null : now.after.getDestination(null);
			Vector2 beforeAt = now.before == null ? null : now.before.getDestination(null);
			Vector2 hereAt = now.current.getDestination(null);

			if (nextAt == null) {
				if (beforeAt == null)
					return out;
				else
					nextAt = new Vector2(hereAt).sub(beforeAt).add(hereAt);
			}
			if (beforeAt == null)
				beforeAt = new Vector2(hereAt).sub(nextAt).scale(-1).add(hereAt);

			Vector2 o = new Vector2();
			o.x = hereAt.x + 0.5f * ((1 - tcb1[0]) * (1 + tcb1[1]) * (1 + tcb1[2]) * (hereAt.x - beforeAt.x) + (1 - tcb1[0]) * (1 - tcb1[1]) * (1 - tcb1[2]) * (nextAt.x - hereAt.y)) / 3;
			o.y = hereAt.y + 0.5f * ((1 - tcb1[0]) * (1 - tcb1[1]) * (1 + tcb1[2]) * (hereAt.y - beforeAt.y) + (1 - tcb1[0]) * (1 + tcb1[1]) * (1 - tcb1[2]) * (nextAt.y - hereAt.y)) / 3;

			if (out == null)
				return o;
			out.setValue(o);
			return out;
		}

		public Vector2 setPosition(Vector2 out, CachedLineCursor now) {
			return out;
		}

	}

	public interface WritingVisitor {
		public WritingReturn rewrite(Event before, Event now, Event after, BaseFilter inside);
	}

}
