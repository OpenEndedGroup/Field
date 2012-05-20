package field.core.plugins.snip;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Link;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.Constants;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.BaseSimplePlugin;
import field.core.plugins.history.TextSearching;
import field.core.plugins.history.TextSearching.iProvidesSearchTerms;
import field.core.plugins.selection.SelectionSetUI;
import field.core.plugins.selection.ToolBarFolder;
import field.core.ui.GraphNodeToTreeFancy;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.SelectionGroup.iSelectionChanged;
import field.core.windowing.components.iComponent;
import field.core.windowing.overlay.OverlayAnimationManager;
import field.launch.Launcher;
import field.math.graph.NodeImpl;
import field.math.graph.iMutable;
import field.math.linalg.Vector4;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.Generics.Pair;

@Woven
public class SnippetsPlugin extends BaseSimplePlugin {

	static public final VisualElementProperty<SnippetsPlugin> snippets = new VisualElementProperty<SnippetsPlugin>("snippets");

	static public class Snippet extends NodeImpl<Snippet> implements iProvidesSearchTerms, field.math.graph.iMutableContainer<String, Snippet> {

		protected String text;
		protected boolean multiline;
		protected String annotation;
		private final String subgroupname;

		public Snippet(String text) {
			this.text = text;
			multiline = text.trim().contains("\n");
			annotation = "";
			subgroupname = "line";
		}

		public Snippet(String text, String annotation, String subgroupname) {
			this.text = text;
			this.subgroupname = subgroupname;
			multiline = text.trim().contains("\n");
			this.annotation = " - <i>" + annotation + "</i>";
		}

		public iMutable duplicate() {
			if (annotation.equals(""))
				return new Snippet(text);
			return new Snippet(text, annotation, subgroupname);
		}

		public void found(int n) {
		}

		public String[] getTerms() {
			return new String[] { text };
		}

		public void notFound() {
		}

		public field.math.graph.iMutableContainer<String, Snippet> setPayload(String t) {
			text = t;
			multiline = text.trim().contains("\n");
			return this;
		}

		public String payload() {
			return text;
		}

		public String getSelectionText() {
			if (annotation.equals(""))
				return text;
			else
				return text.split("\n")[0];
		}

		public void addChildFirst(Snippet newChild) {
			children.add(0, newChild);
			newChild.notifyAddParent(this);
		}

		public void removeLastChild() {
			Snippet removed = children.remove(children.size() - 1);
			removed.notifyRemoveParent(this);
		}

		@Override
		public String toString() {
			String[] ss = text.split("\n");
			return (multiline ? ("<b>" + ss[0] + "</b>" + " " + annotation + " " + smaller("and " + (ss.length - 1) + " " + subgroupname + "" + (ss.length == 2 ? "" : "s") + "")) : "<b>" + text + "</b>" + " " + annotation);
		}
	}

	Snippet root = new Snippet("root");

	private SelectionSetUI snippetUI;

	private SelectionGroup<iComponent> group;

	static public final List<iFunction<Boolean, Pair<URL, SnippetsPlugin>>> urlHandlers = new ArrayList<iFunction<Boolean, Pair<URL, SnippetsPlugin>>>();

	static public void addURLHandler(iFunction<Boolean, Pair<URL, SnippetsPlugin>> f) {
		urlHandlers.add(f);
	}

	@Override
	public void registeredWith(final iVisualElement root) {
		ToolBarFolder parent = ToolBarFolder.currentFolder;
		super.registeredWith(root);

		;//System.out.println(" snippet ui starting up");

		snippetUI = new SelectionSetUI("icons/right_quote_32x32.png", parent) {

			// @Override
			// protected void searchTextUpdated(KeyEvent e) {
			// SnippetsPlugin.this.searchTextUpdated(e);
			// }
			//
			// @Override
			// protected JButton getActions() {
			// return null;
			// }

			protected void selectionChanged(org.eclipse.swt.widgets.TreeItem[] selection) {
				if (selection.length > 0) {
					Snippet model = (Snippet) selection[0].getData();

					// TODO swt clipboard
					Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
					c.setContents(new StringSelection(model.getSelectionText()), null);

					// snippetUI.setFlash("copied \u279d");

					OverlayAnimationManager.notifyTextOnWindow(iVisualElement.enclosingFrame.get(root), "Copied snippet", null, 1, new Vector4(1, 1, 1, 0.15f));

				}
			};
		};

		new GraphNodeToTreeFancy(snippetUI.getTree()).reset(root);

		// TODO SWT d&d
		// snippetUI.enableURLDropTarget(new iAcceptor<URL>() {
		//
		// public iAcceptor<URL> set(URL to) {
		// for (iFunction<Boolean, Pair<URL, SnippetsPlugin>> f :
		// urlHandlers) {
		// if (f.f(new Pair<URL, SnippetsPlugin>(to,
		// SnippetsPlugin.this))) {
		// return this;
		// }
		// }
		// String forms = to.toExternalForm() + "\n" + to.getPath();
		// addText(forms, "dropped URL", new String[] { "URL", "Path" },
		// "alternative form");
		// return this;
		// }
		// });

		snippetUI.setWaterText("Text Snippets");

		Link label = new Link(snippetUI.getToolbarPanel(), SWT.MULTI | SWT.NO_BACKGROUND | SWT.CENTER);
		label.setText("Helpful text snippets");
		label.setFont(new Font(Launcher.display, label.getFont().getFontData()[0].getName(), GraphNodeToTreeFancy.baseFontHeight(label) + 2, SWT.NORMAL));
		label.setBackground(ToolBarFolder.firstLineBackground);

		snippetUI.getToolbarPanel().setLayout(new GridLayout(1, true));
		GridData gd = new GridData(SWT.CENTER, SWT.CENTER, true, true);
		gd.verticalIndent = 1;
		label.setLayoutData(gd);
		// snippetUI.makeSearchField();

		root.setProperty(snippets, this);

		group = root.getProperty(iVisualElement.selectionGroup);

		group.registerNotification(new iSelectionChanged<iComponent>() {
			public void selectionChanged(Set<iComponent> selected) {
				LinkedHashSet<iComponent> c = new LinkedHashSet<iComponent>();
				for (iComponent cc : selected) {
					if (cc.getVisualElement() != null) {
						String forms = cc.getVisualElement().getProperty(iVisualElement.name) + "\n_self.find[\"" + cc.getVisualElement().getProperty(iVisualElement.name) + "\"]";
						addText(forms, "selected", new String[] { "element name", "element search" }, "alternative form");
					}
				}

			}
		});

		allPlugins.add(this);

		addText("");
	}

	@NextUpdate(delay = 1)
	protected void searchTextUpdated(KeyEvent e) {
		rebuild();
	}

	static protected String smaller(String text) {
		return "<font size=-3 color='#" + Constants.defaultTreeColorDim + "'>" + text + "</font>";
	}

	public void addText(String text) {
		text = text.trim();
		if (text.length() > 0) {
			Snippet s = new Snippet(text);

			List<Snippet> c = new ArrayList<Snippet>(root.getChildren());

			for (Snippet ss : c) {
				if (ss.text.equals(text)) {
					root.removeChild(ss);
				}
			}

			root.addChildFirst(s);

			if (root.getChildren().size() > 50) {
				root.removeLastChild();
			}

			String[] x = text.trim().split("\n");
			if (x.length > 1) {
				for (String xx : x) {
					s.addChild(new Snippet(xx));
				}
			}
		}
		rebuild();
	}

	public void addText(String text, String annotation, String[] subannotation, String groupname) {
		text = text.trim();
		Snippet s = new Snippet(text, annotation, groupname);

		List<Snippet> c = new ArrayList<Snippet>(root.getChildren());

		for (Snippet ss : c) {
			if (ss.text.equals(text)) {
				root.removeChild(ss);
			}
		}

		root.addChildFirst(s);

		if (root.getChildren().size() > 50) {
			root.removeLastChild();
		}

		String[] x = text.trim().split("\n");
		if (x.length > 1) {
			int i = 0;
			for (String xx : x) {
				s.addChild(new Snippet(xx, subannotation[i % subannotation.length], "line"));
				i++;
			}
		}

		rebuild();
	}

	public void addText(String text, String annotation, String groupname) {
		text = text.trim();
		Snippet s = new Snippet(text, annotation, groupname);

		List<Snippet> c = new ArrayList<Snippet>(root.getChildren());

		for (Snippet ss : c) {
			if (ss.text.equals(text)) {
				root.removeChild(ss);
			}
		}

		root.addChildFirst(s);

		if (root.getChildren().size() > 50) {
			root.removeLastChild();
		}

		String[] x = text.trim().split("\n");
		if (x.length > 1) {
			for (String xx : x) {
				s.addChild(new Snippet(xx));
			}
		}

		rebuild();
	}

	private void rebuild() {
		// if (snippetUI.getSearchField().getText().length() > 0) {
		// new
		// GraphNodeToTreeFancy(snippetUI.getTree()).reset(search(root,
		// snippetUI.getSearchField().getText()));
		// } else
		new GraphNodeToTreeFancy(snippetUI.getTree()).reset(root);
	}

	protected Snippet search(Snippet root, String text) {
		return new TextSearching(text).search(root);
	}

	@Override
	protected String getPluginNameImpl() {
		return "snip";
	}

	static public void addText(iVisualElement context, String text) {
		SnippetsPlugin plugin = snippets.get(context);
		if (plugin != null) {
			plugin.addText(text);
		}
	}

	static public void addText(iVisualElement context, String text, String annotation, String groupname) {
		SnippetsPlugin plugin = snippets.get(context);
		if (plugin != null) {
			plugin.addText(text, annotation, groupname);
		}
	}

	static public void addText(iVisualElement context, String text, String annotation, String[] subannotation, String groupname) {
		SnippetsPlugin plugin = snippets.get(context);
		if (plugin != null) {
			plugin.addText(text, annotation, subannotation, groupname);
		}
	}

	static public final List<SnippetsPlugin> allPlugins = new ArrayList<SnippetsPlugin>();

	@Override
	public void close() {
		allPlugins.remove(this);
	}

	static public void addPasteboardText(String text) {
		for (SnippetsPlugin s : allPlugins) {
			s.addText(text);
		}
	}

}
