package field.core.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import field.launch.iUpdateable;
import field.namespace.generic.Generics.Pair;

public class MenuFolding {

	public interface iProduction {
		public Pair<String, iUpdateable> condense(String g1, String n1, iUpdateable u1, String g2, String n2, iUpdateable u2);
	}

	static public class SameName implements iProduction {

		public Pair<String, iUpdateable> condense(String g1, String n1, iUpdateable u1, String g2, String n2, iUpdateable u2) {
			if (n1.equals(n2)) {
				return new Pair<String, iUpdateable>(n1, both(u1, u2));
			}
			return null;
		}
	}
	/*
	 * looks for patterns like " operation on 'banana' " and " operation on 'peach' " and returns "operation on 'banana, peach' "
	 */
	static public class SubejctGroupCompositor implements iProduction {

		Pattern p = Pattern.compile("(.*)'(.*)'");

		public Pair<String, iUpdateable> condense(String g1, String n1, iUpdateable u1, String g2, String n2, iUpdateable u2) {
			if (u1 != null)
				return null;
			if (u2 != null)
				return null;

			Matcher m1 = p.matcher(n1);
			Matcher m2 = p.matcher(n2);


			if (m1.matches() && m2.matches()) {
				if (m1.group(1).equals(m2.group(1))) {
					return new Pair<String, iUpdateable>(m1.group(1) + "'" + m1.group(2) + ", " + m2.group(2) + "'", null);
				}
			}
			return null;
		}
	}
	static public iUpdateable both(final iUpdateable u1, final iUpdateable u2) {
		return new iUpdateable() {

			public void update() {
				u1.update();
				u2.update();
			}
		};
	}

	private ArrayList<Pair<String, iUpdateable>> l1;

	private ArrayList<Pair<String, iUpdateable>> l2;

	List<iProduction> productions = new ArrayList<iProduction>();

	public MenuFolding() {
	}

	public MenuFolding addProduction(iProduction p) {
		productions.add(p);
		return this;
	}

	public LinkedHashMap<String, iUpdateable> fold(LinkedHashMap<String, iUpdateable> left, LinkedHashMap<String, iUpdateable> right) {
		l1 = new ArrayList<Pair<String, iUpdateable>>();
		{
			Set<Entry<String, iUpdateable>> ee = left.entrySet();
			for (Entry<String, iUpdateable> e : ee) {
				l1.add(new Pair<String, iUpdateable>(e.getKey(), e.getValue()));
			}
		}
		l2 = new ArrayList<Pair<String, iUpdateable>>();
		{
			Set<Entry<String, iUpdateable>> ee = right.entrySet();
			for (Entry<String, iUpdateable> e : ee) {
				l2.add(new Pair<String, iUpdateable>(e.getKey(), e.getValue()));
			}
		}

		LinkedHashMap<String, iUpdateable> out = new LinkedHashMap<String, iUpdateable>();

		Set<Pair<String, iUpdateable>> foundLeft = new HashSet<Pair<String, iUpdateable>>();
		Set<Pair<String, iUpdateable>> foundRight = new HashSet<Pair<String, iUpdateable>>();

		String g1 = null;
		for (int i = 0; i < l1.size(); i++) {
			if (l1.get(i).right == null)
				g1 = l1.get(i).left;

			String g2 = null;
			boolean found = false;

			inner: for (int j = 0; j < l2.size(); j++) {
				if (l2.get(j).right == null)
					g2 = l2.get(j).left;

				for (iProduction p : productions) {
					Pair<String, iUpdateable> cc = p.condense(g1, l1.get(i).left, l1.get(i).right, g2, l2.get(j).left, l2.get(j).right);
					if (cc != null) {
						found = true;

						out.put(cc.left, cc.right);

						foundLeft.add(l1.get(i));
						foundRight.add(l2.get(j));
						break inner;
					}
				}
			}
		}

		{
			Pair<String, iUpdateable> gr1 = null;
			boolean groupAdded = false;
			for (int i = 0; i < l1.size(); i++) {
				if (l1.get(i).right == null) {
					gr1 = l1.get(i);
					groupAdded = false;
				} else if (!foundLeft.contains(l1.get(i))) {
					if (!groupAdded && gr1 != null) {
						out.put(gr1.left, gr1.right);
						groupAdded = true;
					}
					out.put(l1.get(i).left, l1.get(i).right);
				}
			}
		}
		{
			Pair<String, iUpdateable> gr1 = null;
			boolean groupAdded = false;
			for (int i = 0; i < l2.size(); i++) {
				if (l2.get(i).right == null) {
					gr1 = l2.get(i);
					groupAdded = false;
				} else if (!foundRight.contains(l2.get(i))) {
					if (!groupAdded && gr1 != null) {
						out.put(gr1.left, gr1.right);
						groupAdded = true;
					}
					out.put(l2.get(i).left, l2.get(i).right);
				}
			}
		}

		Iterator<Entry<String, iUpdateable>> ii = out.entrySet().iterator();
		boolean lastWasGroup = false;
		String last = null;
		Set<String> toRemove = new HashSet<String>();

		while (ii.hasNext()) {
			Entry<String, iUpdateable> i = ii.next();
			if (i.getValue() == null) {
				if (lastWasGroup) {
					toRemove.add(i.getKey());
				}
				lastWasGroup = true;
				last = i.getKey();
			} else
				lastWasGroup = false;
		}
		for (String s : toRemove)
			out.remove(s);

		return out;
	}
}
