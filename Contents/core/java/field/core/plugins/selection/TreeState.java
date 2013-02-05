package field.core.plugins.selection;

import java.util.LinkedHashSet;

import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class TreeState {

	LinkedHashSet<String> expanded = new LinkedHashSet<String>();

	static public TreeState save(Tree t) {
		TreeState ts = new TreeState();
		TreeItem[] ii = t.getItems();
		for (TreeItem iii : ii) {
			if (iii.getExpanded()) {
				ts.expanded.add(iii.getText());
				save_(ts, iii);
			}
		}
		return ts;
	}

	public void load(Tree t) {
		LinkedHashSet<String> v = new LinkedHashSet<String>(expanded);

		System.out.println(" expanded is :" + v);

		load_(t, v);
	}

	private void load_(Tree t, LinkedHashSet<String> v) {
		TreeItem[] ii = t.getItems();
		for (TreeItem iii : ii) {
			System.out.println(" check :" + iii.getText());
			if (v.remove(iii.getText())) {
				iii.setExpanded(true);
				System.out.println(" expanding ");
				load_(t, v);
				return;
			} else if (iii.getExpanded()) {
				load_(t, iii, v);
			}
		}
	}

	private void load_(Tree tt, TreeItem t, LinkedHashSet<String> v) {
		TreeItem[] ii = t.getItems();
		for (TreeItem iii : ii) {
			System.out.println(" check :" + iii.getText());
			if (v.remove(iii.getText())) {
				iii.setExpanded(true);
				System.out.println(" expanding ");
				load_(tt, v);
				return;
			} else if (iii.getExpanded()) {
				load_(tt, iii, v);
			}
		}
	}

	private static void save_(TreeState ts, TreeItem iii) {
		for (TreeItem iiii : iii.getItems()) {
			if (iiii.getExpanded()) {
				ts.expanded.add(iiii.getText());
				save_(ts, iiii);
			}
		}
	}

}
