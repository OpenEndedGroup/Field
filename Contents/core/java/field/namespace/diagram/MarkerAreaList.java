package field.namespace.diagram;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import field.namespace.diagram.DiagramZero.iMarker;
import field.namespace.generic.ReflectionTools;


public class MarkerAreaList<T> {

	static public final Method method_extendForward = ReflectionTools.methodOf("extendForward", MarkerAreaList.class);
	static public final Method method_extendBackward = ReflectionTools.methodOf("extendBackward", MarkerAreaList.class);
	static public final Method method_rollForward = ReflectionTools.methodOf("rollForward", MarkerAreaList.class);
	static public final Method method_rollBackward = ReflectionTools.methodOf("rollBackward", MarkerAreaList.class);
	
	static public final Method method_extendAreaForward = ReflectionTools.methodOf("extendAreaForward", MarkerAreaList.class, MarkerArea.class);
	static public final Method method_extendAreaBackward = ReflectionTools.methodOf("extendAreaBackward", MarkerAreaList.class, MarkerArea.class);
	
	
	
	static public class MarkerArea<X> {
		List<Cursor<X>> cursors = new ArrayList<Cursor<X>>();

		public double startsAt() {
			return cursors.get(0).getTime();
		}

		public double endsAtStart() {
			return cursors.get(cursors.size() - 1).getTime();
		}

		public double endsAt() {
			Cursor<X> c = cursors.get(cursors.size() - 1);
			return c.getTime() + c.getDuration();
		}

		public boolean contains(Cursor<X> to) {
			return cursors.contains(to);
		}

		public boolean isNextFor(Cursor<X> to) {
			return (cursors.get(0).clone().previous().equals(to));
		}

		public boolean isPreviousFor(Cursor<X> to) {
			return (cursors.get(cursors.size() - 1).clone().next().equals(to));
		}
	}

	List<MarkerArea<T>> areas = new ArrayList<MarkerArea<T>>();

	public List<Cursor<T>> allMarkers() {
		List<Cursor<T>> all = new ArrayList<Cursor<T>>();
		for (MarkerArea a : areas) {
			all.addAll(a.cursors);
		}
		return all;
	}

	public MarkerAreaList(Cursor<T> from) {
		MarkerArea area = new MarkerArea();
		area.cursors.add(from.clone());
		areas.add(area);
	}

	public MarkerAreaList() {
	}

	public void addMarker(Cursor<T> to) {
		boolean already = false;
		MarkerArea before = null;

		// the question is, where does this marker go?
		for (MarkerArea ma : areas) {
			if (to.getTime() >= ma.startsAt() && to.getTime() <= ma.endsAtStart()) {

				// this is
				// problematic
				assert ma.contains(to) : ma.cursors + " " + to + " this class only works on monophonic channels";

				already = true;
				break;
			} else if (to.getTime() < ma.startsAt()) {
				before = ma;
				already = false;
				break;
			}
		}
		if (!already) {
			if (before != null) {
				int i = areas.indexOf(before);
				if (before.isNextFor(to)) {
					if (i > 0 && areas.get(i - 1).isPreviousFor(to)) {
						MarkerArea a = areas.remove(i - 1);
						before.cursors.add(0, to.clone());
						before.cursors.addAll(0, a.cursors);
					} else {
						before.cursors.add(0, to.clone());
					}
				} else if (i > 0 && areas.get(i - 1).isPreviousFor(to)) {
					MarkerArea a = areas.get(i - 1);
					a.cursors.add(to.clone());
				} else {
					MarkerArea a = new MarkerArea();
					a.cursors.add(to.clone());
					areas.add(i, a);
				}
			} else {
				if (areas.size() > 0 && areas.get(areas.size() - 1).isPreviousFor(to)) {
					areas.get(areas.size() - 1).cursors.add(to.clone());
				} else {
					MarkerArea a = new MarkerArea();
					a.cursors.add(to.clone());
					areas.add(a);
				}
			}
		}
	}

	public boolean removeMarker(Cursor<T> marker) {
		for (MarkerArea area : areas) {
			int i = area.cursors.indexOf(marker);
			if (i != -1) {
				if (i > 0 && i < area.cursors.size() - 1) {
					// split
					MarkerArea left = new MarkerArea();
					MarkerArea right = new MarkerArea();
					left.cursors.addAll(area.cursors.subList(0, i));
					right.cursors.addAll(area.cursors.subList(i + 1, area.cursors.size()));
					int z = areas.indexOf(area);
					areas.remove(z);
					areas.add(z, right);
					areas.add(z, left);
					return true;
				} else {
					area.cursors.remove(marker);
					if (area.cursors.size() == 0)
						areas.remove(area);
					return true;
				}
			}
		}
		return false;
	}

	// needed: other operations
	// roll forward
	// merge gaps
	// remove random
	// remove middle
	// extend forward
	// trim backwards
	// etc.

	// perhaps an method, with a random parameter

	public boolean extendForward() {
		MarkerArea<T> area = areas.get(areas.size() - 1);
		Cursor<T> c = area.cursors.get(area.cursors.size() - 1);
		if (c.hasNext()) {
			area.cursors.add(c.clone().next());
			return true;
		}
		return false;
	}

	public boolean extendBackward() {
		MarkerArea<T> area = areas.get(0);
		Cursor<T> c = area.cursors.get(0);
		if (c.hasPrevious()) {
			area.cursors.add(0, c.clone().previous());
			return true;
		}
		return false;
	}

	public boolean rollForward() {
		MarkerArea<T> area1 = areas.get(areas.size() - 1);
		Cursor<T> c1 = area1.cursors.get(area1.cursors.size() - 1);
		MarkerArea area2 = areas.get(0);
		if (c1.hasNext()) {
			area1.cursors.add(c1.clone().next());
			area2.cursors.remove(0);
			if (area2.cursors.size() == 0)
				areas.remove(area2);
			return true;
		}
		return false;
	}

	public boolean rollBackward() {
		MarkerArea<T> area1 = areas.get(areas.size() - 1);
		Cursor<T> c1 = area1.cursors.get(area1.cursors.size() - 1);
		MarkerArea<T> area2 = areas.get(0);
		Cursor<T> c2 = area2.cursors.get(area2.cursors.size() - 1);
		if (c2.hasPrevious()) {
			area2.cursors.add(0, area2.cursors.get(0).clone().previous());
			area1.cursors.remove(area1.cursors.size() - 1);
			if (area1.cursors.size() == 0)
				areas.remove(area1);
			return true;
		}
		return false;
	}

	public boolean extendAreaForward(MarkerArea<T> area) {
		Cursor<T> c = area.cursors.get(area.cursors.size() - 1);
		if (c.hasNext()) {
			area.cursors.add(c.clone().next());
			int i = areas.indexOf(area);
			if (i < areas.size() - 1)
				potentiallyUnify(areas.get(i), areas.get(i + 1));
			return true;
		}
		return false;
	}

	public boolean extendAreaBackward(MarkerArea<T> area) {
		Cursor<T> c = area.cursors.get(area.cursors.size() - 1);
		if (c.hasNext()) {
			area.cursors.add(c.clone().next());
			int i = areas.indexOf(area);
			if (i > 0)
				potentiallyUnify(areas.get(i - 1), areas.get(i));

			return true;
		}
		return false;
	}

	public boolean potentiallyUnify(MarkerArea<T> left, MarkerArea<T> right) {
		if (left.isPreviousFor(right.cursors.get(0))) {
			areas.remove(right);
			left.cursors.addAll(right.cursors);
			return true;
		}
		return false;
	}

	public List<iMarker<T>> all() {
		ArrayList<iMarker<T>> r = new ArrayList<iMarker<T>>();
		for (MarkerArea a : areas)
			r.addAll(a.cursors);
		return r;
	}

	public String detailedToString() {
		String r = "markerAreaList has <" + areas.size() + "> areas\n";
		for (MarkerArea a : areas) {
			r += "   " + a.startsAt() + " -> " + a.endsAt() + " " + a.cursors + " \n";
		}
		return r;
	}

	public List<T> allRaw() {
		ArrayList<T> r = new ArrayList<T>();
		for (MarkerArea<T> a : areas)
			for (Cursor<T> c : a.cursors)
				r.add(c.getPayload());
		return r;
	}

	public List<MarkerArea<T>> getAreas() {
		return areas;
	}

}
