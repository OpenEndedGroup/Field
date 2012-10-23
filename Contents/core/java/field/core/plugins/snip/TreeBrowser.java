package field.core.plugins.snip;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import field.core.Platform;
import field.core.Platform.OS;
import field.core.plugins.history.ElementFileSystemTree.ObjectTransfer;
import field.core.plugins.selection.ToolBarFolder;
import field.core.ui.GraphNodeToTreeFancy;
import field.core.ui.MacScrollbarHack;
import field.core.windowing.GLComponentWindow;
import field.launch.Launcher;

public class TreeBrowser {

	Tree tree;
	private Object root;
	private Composite toolbar;
	ToolBarFolder open;
	private Link label;

	static public final ObjectTransfer transfer = new ObjectTransfer();
	static public TreeItem[] lastInternalDragSelection;

	public interface TemplateRootMarker {
	}

	static public class TemplateMarker {
		File f;

		public TemplateMarker(File f) {
			this.f = f;
		}
	}

	public TreeBrowser() {

		Composite target = GLComponentWindow.lastCreatedWindow.leftComp1;

		open = ToolBarFolder.helpFolder;

		// open = new ToolBarFolder(new Rectangle(50, 50, 300, 600),
		// false);
		// ToolBarFolder.helpFolder = open;

		Composite container = new Composite(open.getContainer(), SWT.NO_BACKGROUND);
		open.add("icons/folder_stroke_16x16.png", container);

		toolbar = new Composite(container, SWT.BACKGROUND);
		Color backgroundColor = open.firstLineBackground;
		toolbar.setBackground(backgroundColor);

		tree = new Tree(container, 0);
		tree.setBackground(ToolBarFolder.firstLineBackground);
		new GraphNodeToTreeFancy.Pretty(tree, 200);

		GridLayout gl = new GridLayout(1, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		if (Platform.isLinux()) {
			gl.marginLeft = 0;
			gl.marginRight = 0;
			gl.marginTop = 0;
			gl.marginBottom = 0;
			gl.verticalSpacing = 0;
		}

		// if (Platform.getOS() == OS.linux) {
		// gl.marginHeight = 5;
		// gl.marginWidth = 5;
		// }
		container.setLayout(gl);
		{
			GridData data = new GridData();
			data.heightHint = 28;
			if (Platform.getOS() == OS.linux) {
				data.heightHint = 38;
			}
			data.widthHint = 1000;
			data.horizontalAlignment = SWT.FILL;
			data.grabExcessHorizontalSpace = true;
			toolbar.setLayoutData(data);
		}
		{
			GridData data = new GridData();
			data.grabExcessVerticalSpace = true;
			data.grabExcessHorizontalSpace = true;
			data.verticalAlignment = SWT.FILL;
			data.horizontalAlignment = SWT.FILL;
			data.verticalIndent = 0;
			data.horizontalIndent = 0;
			tree.setLayoutData(data);
		}
		label = new Link(toolbar, SWT.NO_BACKGROUND | SWT.CENTER);
		label.setText("Reusable Tree View");
		label.setFont(new Font(Launcher.display, label.getFont().getFontData()[0].getName(), label.getFont().getFontData()[0].getHeight() + 2, SWT.NORMAL));
		label.setBackground(ToolBarFolder.firstLineBackground);

		toolbar.setLayout(new GridLayout(1, true));
		GridData gd = new GridData(SWT.CENTER, SWT.CENTER, true, true);
		// gd.verticalIndent = 1;
		gd.minimumHeight=150;
		gd.verticalAlignment = gd.VERTICAL_ALIGN_CENTER;
		label.setLayoutData(gd);

		build(root, tree);

		new MacScrollbarHack(tree);

		tree.setBackground(ToolBarFolder.background);
	}

	protected void build(Object root, Object a) {

		Collection<Object> f = childrenOf(root);
		if (f == null)
			return;
		if (f.size() == 0)
			return;

		if (f != null) {

			for (Object ff : f) {
				TreeItem i = a instanceof Tree ? new TreeItem((Tree) a, 0) : new TreeItem((TreeItem) a, 0);
				i.setText(textFor(ff));
				i.setData(ff);

				build(ff, i);
			}
		}

		tree.addListener(SWT.MouseDoubleClick, new Listener() {
			@Override
			public void handleEvent(Event event) {
				TreeItem[] s = tree.getSelection();
				if (s.length == 1) {
					doubleClick(s[0].getData());
				}
			}
		});

		tree.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				TreeItem[] s = tree.getSelection();
				if (s.length == 1) {
					selectionText(s[0].getData());
				}
			}
		});

	}

	protected List<String> selectionText(Object data) {
		List o = TreeBrowserDispatch.selectionText.gather(data);
		return o;
	}

	private Collection<Object> childrenOf(Object o) {
		List cc = TreeBrowserDispatch.childrenOf.gather(o);
		List<List<Object>> oo = cc;
		List<Object> f = new ArrayList<Object>();
		for (List<Object> ooo : oo)
			f.addAll(ooo);
		return f;
	}

	protected void doubleClick(Object s) {
		TreeBrowserDispatch.doubleClick.call(s);
	}

	private String textFor(Object ff) {
		return (String) TreeBrowserDispatch.textFor.call(ff);
	}

	public void setRoot(Object r) {
		this.root = r;
		tree.removeAll();

		TreeItem i = new TreeItem(tree, 0);
		i.setText(textFor(root));
		i.setData(root);

		build(this.root, i);
	}

	private String limit(String textFor) {
		if (textFor == null)
			return "(null)";
		if (textFor.length() > 25)
			return textFor.substring(0, 22) + "...";
		return textFor;
	}

}
