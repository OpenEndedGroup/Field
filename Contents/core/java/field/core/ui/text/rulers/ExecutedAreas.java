package field.core.ui.text.rulers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.swt.custom.StyledText;

import field.core.ui.text.rulers.StyledTextPositionSystem.Position;
import field.math.BaseMath.MutableFloat;
import field.math.util.Histogram;
import field.namespace.generic.Bind;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.ReflectionTools.Pair;
import field.util.Dict;
import field.util.Dict.Prop;

public class ExecutedAreas {

	static public class Area {

		public int allocation;

		public long lastSeenAt;

		public int lineStart;

		public int lineEnd;

		public boolean invalid = false;

		public Dict extend = new Dict();

		transient Position trackingStart;

		transient Position trackingEnd;

		transient public Dict textend = new Dict();

		public List<ExecutionRecord> exec = new ArrayList<ExecutionRecord>();

		public void freeze() {
			if (exec == null)
				exec = new ArrayList<ExecutionRecord>();
			trackingStart = null;
			trackingEnd = null;
		}

		@Override
		public String toString() {
			return "area:<" + lineStart + " -> " + lineEnd + ">"
					+ (invalid ? "invalid" : "") + " " + trackingStart + " "
					+ trackingEnd;
		}

		public void update(StyledText text) {
			if (textend == null)
				textend = new Dict();
			if (exec == null)
				exec = new ArrayList<ExecutionRecord>();

			if (trackingStart == null) {
				StyledTextPositionSystem tracker = StyledTextPositionSystem
						.get(text);
				trackingStart = tracker.createPosition(positionForLineStart(
						lineStart, text.getText()));
				trackingEnd = tracker.createPosition(positionForLineEnd(
						lineEnd, text.getText()));
				
			} else {
				int x1 = lineForOffset(trackingStart.at, text.getText());
				int x2 = lineForOffset(trackingEnd.at, text.getText());
				if (x1 == -1 || x2 == -1)
					invalid = true;
				else {
					lineStart = x1;
					lineEnd = x2;
				}
			}
			if (extend == null)
				extend = new Dict();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + lineEnd;
			result = prime * result + lineStart;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Area other = (Area) obj;
			if (lineEnd != other.lineEnd)
				return false;
			if (lineStart != other.lineStart)
				return false;
			return true;
		}

	}

	static public class ExecutionRecord {
		public Date when = new Date();
		public String stringAtExecution;

		public ExecutionRecord(String stringAtExecution) {
			this.stringAtExecution = stringAtExecution;
		}
	}

	public void coalesceAreas() {
		for (Area a : areas) {
			if (a.lineStart > a.lineEnd) {
				deleteArea(a);
				coalesceAreas();
				return;
			}
			for (Area b : areas) {
				if (a != b) {
					if (a.equals(b)) {
						deleteArea(b);
						merge(a, b);
						coalesceAreas();
						return;
					}
				}
			}
		}

		for (Entry<Area, MutableFloat> a : rootHistogram.getEntries()) {
			if (!areas.contains(a.getKey())) {
				deleteArea(a.getKey());
				return;
			}
		}

	}

	protected void merge(Area a, Area b) {

	}

	/**
	 * one per visual element per text property
	 * 
	 * @author marc
	 * 
	 */
	static public class State {
		public List<Area> areas = new ArrayList<Area>();

		public Histogram<Area> rootHistogram = new Histogram<Area>();

		transient Area lastExecuted;

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			State other = (State) obj;
			if (areas == null) {
				if (other.areas != null)
					return false;
			} else if (!areas.equals(other.areas))
				return false;
			if (rootHistogram == null) {
				if (other.rootHistogram != null)
					return false;
			} else if (!rootHistogram.equals(other.rootHistogram))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((areas == null) ? 0 : areas.hashCode());
			result = prime * result
					+ ((rootHistogram == null) ? 0 : rootHistogram.hashCode());
			return result;
		}

	}

	static public final Prop<Histogram<Area>> forwardTransitions = new Prop<Histogram<Area>>(
			"forwardTransitions");
	static public final Prop<Character> keyboardShortcut = new Prop<Character>(
			"keyboardShortcut");

	static public final Prop<Boolean> isUnitTest = new Prop<Boolean>(
			"isUnitTest");
	static public final Prop<Boolean> passedUnitTest = new Prop<Boolean>(
			"passedUnitTest");

	static public int lineForOffset(int offset, String text) {
		String[] lines = text.split("\n");
		int at = 0;
		for (int n = 0; n < lines.length; n++) {
			at += lines[n].length() + 1;
			if (at > offset)
				return n;
		}
		if (at >= offset)
			return lines.length;
		return -1;
	}

	static public int positionForLineEnd(int i, String text) {
		String[] lines = text.split("\n");
		int at = 0;
		for (int n = 0; n < Math.min(lines.length, i + 1); n++) {
			at += lines[n].length() + 1;
		}
		return Math.max(0, at - 1);
	}

	static public int positionForLineStart(int i, String text) {
		String[] lines = text.split("\n");
		int at = 0;
		for (int n = 0; n < Math.min(lines.length, i); n++) {
			at += lines[n].length() + 1;
		}
		return at;
	}

	public List<Area> areas = new ArrayList<Area>();

	private final StyledText text;

	private Area lastExecuted;

	Histogram<Area> rootHistogram = new Histogram<Area>();

	long clock = 0;

	public ExecutedAreas(StyledText p) {
		this.text = p;
	}

	public void allocateNew(Area newArea, List<Area> others) {
		for (Area a : others) {
			if (a != newArea) {
				if (overlaps(newArea, a)) {
					if (newArea.lastSeenAt > a.lastSeenAt) {
						a.allocation++;
						allocateNew(a, others);
					} else {
						newArea.allocation++;
						allocateNew(newArea, others);
						return;
					}
				}
			}
		}
	}

	public void deleteArea(Area target) {
		areas.remove(target);
		for (Area a : areas) {
			Histogram<Area> t = a.extend.get(forwardTransitions);
			if (t != null)
				t.removeKey(target);
		}
		rootHistogram.removeKey(target);
	}

	public Area execute(int offsetStart, int offsetEnd) {
		return execute(offsetStart, offsetEnd, null);
	}

	public Area execute(int offsetStart, int offsetEnd, String textAtExecution) {

		clock++;

		int ls = lineForOffset(offsetStart, text.getText());
		int le = lineForOffset(offsetEnd, text.getText());

		Area found = null;
		for (Area a : areas) {
			a.update(text);
			if (a.lineStart == ls && a.lineEnd == le && !a.invalid) {
				found = a;
			}
		}
		if (found != null) {
			found.lastSeenAt = clock;
			markTransition(lastExecuted, found);
			lastExecuted = found;
			found.exec.add(new ExecutionRecord(textAtExecution));
			return found;
		} else {
			Area newArea = new Area();
			newArea.lastSeenAt = clock;
			newArea.lineStart = ls;
			newArea.lineEnd = le;
			newArea.update(text);

			allocateNew(newArea, areas);

			areas.add(newArea);

			markTransition(lastExecuted, found);
			lastExecuted = found;

			ExecutionRecord r = new ExecutionRecord(textAtExecution);
			newArea.exec.add(r);

			return newArea;
		}
	}

	public Area getCurrentArea() {
		return lastExecuted;
	}

	public Histogram<Area> getRootHistogram() {
		return rootHistogram;
	}

	public State getState() {
		State s = new State();
		s.rootHistogram = rootHistogram;
		s.areas = new ArrayList<Area>(areas);
		s.lastExecuted = lastExecuted;
		return s;
	}

	public void markTransition(Area from, Area to) {
		Histogram<Area> t;
		if (from != null) {
			if (from.extend == null)
				from.extend = new Dict();
			t = from.extend.get(forwardTransitions);
			if (t == null)
				from.extend.put(forwardTransitions, t = new Histogram<Area>());
		} else {
			t = rootHistogram;
		}
		t.visit(to, 1);
		scrub(t);

	}

	public void promote(Area a) {
		clock++;
		a.lastSeenAt = clock;
		a.allocation = 0;
		areas.remove(a);
		allocateNew(a, areas);
		areas.add(a);

	}

	public void setAreas(Collection<Area> a) {
		areas.clear();
		areas.addAll(a);
		for (Area aa : a) {
			if (aa.lastSeenAt > clock)
				clock = aa.lastSeenAt + 1;
			aa.update(text);
		}

	}

	public void setState(State s) {
		setAreas(s.areas);
		rootHistogram = s.rootHistogram;
		lastExecuted = s.lastExecuted;
	}

	private boolean overlaps(Area newArea, Area a) {
		if (newArea.allocation == a.allocation
				&& newArea.lineStart <= a.lineEnd
				&& newArea.lineEnd >= a.lineStart)
			return true;
		return false;
	}

	private void scrub(Histogram<Area> t) {
		iFunction<Boolean, Pair<Area, Number>> f = new Bind.iFunction<Boolean, Pair<Area, Number>>() {
			public Boolean f(Pair<Area, Number> in) {
				if (in.left == null)
					return true;
				if (in.left.invalid)
					return true;
				if (in.right.floatValue() < 0.25f
						&& in.left.lastSeenAt < clock - 20)
					return true;
				return false;
			}
		};
		t.remove(f);
	}

}
