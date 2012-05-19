package field.core.ui;

import java.util.List;

import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import field.math.graph.iMutable;
import field.math.graph.iMutableContainer;

public class GraphNodeToTree {

	
	private final Tree target;

	public GraphNodeToTree(Tree target)
	{
		this.target = target;
	}
	
	public void reset(iMutableContainer m)
	{
		target.removeAll();
		
		for (Object o : m.getChildren())
			populate((iMutableContainer) o, target);

	}

	private void populate(iMutableContainer m, Widget i) {
		
		TreeItem item = i instanceof TreeItem ? new TreeItem(((TreeItem)i), 0) : new TreeItem((Tree)i, 0);
		item.setText(""+m);
		item.setData(m.payload());

		List<iMutableContainer> c = m.getChildren();
		for(iMutableContainer cc : c)
		{
			populate(cc, item);
		}
		
	}
	
}
