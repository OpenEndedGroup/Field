package field.extras.reload;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import field.bytecode.protect.ReloadingSupport.ReloadingDomain;
import field.bytecode.protect.Trampoline2;
import field.bytecode.protect.Trampoline2.MyClassLoader;
import field.core.Platform;
import field.core.Platform.OS;
import field.core.dispatch.iVisualElement;
import field.core.execution.PythonInterface;
import field.core.plugins.BaseSimplePlugin;
import field.core.plugins.selection.ToolBarFolder;
import field.core.ui.GraphNodeToTreeFancy;
import field.core.ui.SmallMenu;
import field.core.windowing.overlay.OverlayAnimationManager;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.namespace.generic.Bind.iFunction;
import field.util.AutoPersist;

public class ReloadPlugin extends BaseSimplePlugin {

	private Composite container;
	private Tree tree;
	private Composite toolbar;

	LinkedHashSet<Class> internal = new LinkedHashSet<Class>();

	LinkedHashSet<String> reloadDomains = new AutoPersist().persist("reloadDomans", new LinkedHashSet<String>());

	@Override
	public void registeredWith(iVisualElement root) {
		super.registeredWith(root);
		this.root = root;

		this.container = new Composite(ToolBarFolder.currentFolder.getContainer(), SWT.NO_BACKGROUND);

		ToolBarFolder.currentFolder.add("icons/reload.png", container);

		toolbar = new Composite(container, SWT.BACKGROUND);
		Color backgroundColor = ToolBarFolder.currentFolder.firstLineBackground;
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

		Link label = new Link(toolbar, SWT.MULTI | SWT.NO_BACKGROUND | SWT.CENTER);
		label.setText("Java class reloading");
		label.setFont(new Font(Launcher.display, label.getFont().getFontData()[0].getName(), label.getFont().getFontData()[0].getHeight() + 2, SWT.NORMAL));
		label.setBackground(ToolBarFolder.firstLineBackground);

		toolbar.setLayout(new GridLayout(1, true));
		GridData gd = new GridData(SWT.CENTER, SWT.CENTER, true, true);
		// gd.verticalIndent = 1;
		label.setLayoutData(gd);

		tree.setBackground(ToolBarFolder.background);

		tree.addListener(SWT.MouseDown, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				if (Platform.isPopupTrigger(arg0)) {
					TreeItem[] s = tree.getSelection();
					if (s.length == 1)
						popupFor(s[0], arg0);
				}
			}
		});

		tree.addListener(SWT.MouseDoubleClick, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				if (Platform.isPopupTrigger(arg0))
					return;

				TreeItem[] s = tree.getSelection();
				if (s.length == 1)
					doubleClickFor(s[0], arg0);
			}
		});

		for (String s : reloadDomains) {
			Trampoline2.trampoline.reloadingSupport.addDomain(s);
		}

	}

	public class Node {
		String name;
		Node parent;

		public Node(String name) {
			this.name = name;
		}

		List<String> classesHere = new ArrayList<String>();
		List<Node> children = new ArrayList<Node>();
	}

	TreeMap<String, Node> packageList = new TreeMap<String, Node>();

	int hashWas = 0;

	private void populateTree() {

		TreeItem[] items = tree.getItems();
		for (int i = 0; i < items.length; i++) {
			items[i].dispose();
		}

		// ;//;//System.out.println(" ----------- reload domains ------------");
		List<ReloadingDomain> domains = Trampoline2.trampoline.reloadingSupport.getDomains();

		for (ReloadingDomain d : domains) {
			TreeItem i = new TreeItem(tree, 0);

			// ;//;//System.out.println(" domain :" + d.matcher);

			i.setText("Reload: <b>" + d.matcher.toString() + "</b>");
			i.setData(d);
			populateCaught(i, d);
		}

		Set<Class> allClasses = ((MyClassLoader) Trampoline2.trampoline.getClassLoader()).getAllLoadedClasses();

		// ;//;//System.out.println(" we have <" + allClasses.size() +
		// "> classes");

		packageList = new TreeMap<String, ReloadPlugin.Node>();

		TreeItem internalClasses = new TreeItem(tree, 0);
		internalClasses.setText("<i>internal classes</i>");
		internalClasses.setData(null);

		for (Class c : allClasses) {
			if (internal.contains(c))
				continue;

			// if (c.getName().startsWith("field.")) internal =
			// true;
			if (c.getName().startsWith("org.eclipse."))
				continue;
			if (c.getName().startsWith("org.python."))
				continue;
			if (c.getName().startsWith("java."))
				continue;
			if (c.getName().startsWith("javax."))
				continue;
			if (c.getName().startsWith("com.kenai"))
				continue;
			if (c.getName().startsWith("com.sun"))
				continue;
			if (c.getName().startsWith("com.thoughtworks"))
				continue;
			if (c.getName().startsWith("org.lwjgl"))
				continue;

			String name = c.getName();
			String[] parts = name.split("\\.");
			// ;//;//System.out.println("          class :" + c.getName()
			// + " " + parts.length);
			String x = "";
			Node p = null;
			for (int i = 0; i < parts.length - 1; i++) {
				Node n = nodeFor(p, x + parts[i]);
				p = n;
				x = x + parts[i] + ".";
			}
			if (p != null) {
				p.classesHere.add(name);
			}
		}

		Set<Entry<String, Node>> pp = packageList.entrySet();
		for (Entry<String, Node> e : pp) {

			// ;//;//System.out.println(" scanning node <" + e + ">");

			if (e.getValue().parent == null) {

				// ;//;//System.out.println(" adding element :" +
				// e.getValue().name);
				TreeItem i;
				if (e.getValue().name.startsWith("field"))

				{
					i = new TreeItem(internalClasses, 0);
				} else {
					i = new TreeItem(tree, 0);
				}
				i.setText(e.getValue().name);
				i.setData(e.getValue());
				populateLoaded(i, e.getValue());
			}
		}

		hashWas = allClasses.hashCode();

	}

	int tick = 0;

	int willRep = -1;

	public void update() {

		tick++;
		if (tick == 2) {
			internal = new LinkedHashSet<Class>(((MyClassLoader) Trampoline2.trampoline.getClassLoader()).getAllLoadedClasses());
			;//;//System.out.println(" ----------- reload plugin ----------");
			populateTree();
		} else if (tick > 2) {

			Control c = Launcher.display.getFocusControl();
			if (c == null)
				return;
			Shell s = c.getShell();
			if (s.getText().trim().length() == 0)
				return;

			Set<Class> allClass = ((MyClassLoader) Trampoline2.trampoline.getClassLoader()).getAllLoadedClasses();
			int hashNow = allClass.hashCode();
			if (hashNow != hashWas && willRep < 0) {
				willRep = 5;
			}
		}

		willRep--;
		if (willRep == 0) {
			populateTree();
		}

	}

	protected void doubleClickFor(TreeItem s, Event event) {
		final Object d = s.getData();
		if (d instanceof ReloadingDomain) {
			try {
				((ReloadingDomain) d).reload(new iFunction<Object, Class>() {
					@Override
					public Object f(Class in) {

						// def
						// doReload(c):
						// was =
						// globals()[c.getSimpleName()]
						// globals()[c.getSimpleName()]=c

						Object v = PythonInterface.getPythonInterface().getVariable(in.getSimpleName());
						if (v != null) {
							PythonInterface.getPythonInterface().setVariable(in.getSimpleName(), in);
						}

						OverlayAnimationManager.notifyAsText(root, "Reloaded classes", null);

						return null;
					}
				});
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			populateTree();
		} else if (d instanceof String) {
			Trampoline2.reloadingSupport.addDomain(((String) d) + ".*");
			reloadDomains.add(((String) d) + ".*");
			populateTree();
		}

	}

	protected void popupFor(TreeItem s, Event event) {
		;//;//System.out.println(" popup for :" + s);
		final Object d = s.getData();

		LinkedHashMap<String, iUpdateable> up = new LinkedHashMap<String, iUpdateable>();

		if (d instanceof String) {
			up.put("Java package or class", null);
			up.put("<b>Make reloadable</b> <i>(restart required)</i>", new iUpdateable() {

				@Override
				public void update() {
					Trampoline2.reloadingSupport.addDomain(((String) d) + ".*");
					reloadDomains.add(((String) d) + ".*");
					
					populateTree();
				}
			});
		} else if (d instanceof ReloadingDomain) {
			up.put("Reloadable domain", null);
			up.put("<b>Reload</b> now", new iUpdateable() {

				@Override
				public void update() {

					try {
						((ReloadingDomain) d).reload(new iFunction<Object, Class>() {
							@Override
							public Object f(Class in) {

								// def
								// doReload(c):
								// was =
								// globals()[c.getSimpleName()]
								// globals()[c.getSimpleName()]=c

								Object v = PythonInterface.getPythonInterface().getVariable(in.getSimpleName());
								if (v != null) {
									PythonInterface.getPythonInterface().setVariable(in.getSimpleName(), in);
								}

								OverlayAnimationManager.notifyAsText(root, "Reloaded classes", null);

								return null;
							}
						});
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
					populateTree();

				}
			});

			up.put("<b>Remove</b> <i>(restart required)</i>", new iUpdateable() {

				@Override
				public void update() {

					reloadDomains.remove(((ReloadingDomain) d).matcher.toString());
					Trampoline2.reloadingSupport.getDomains().remove(d);
					populateTree();

				}
			});

		}

		new SmallMenu().createMenu(up, container.getShell(), null).show(Launcher.display.map(tree, tree.getShell(), new Point(event.x, event.y)));

	}

	private void populateLoaded(TreeItem i, Node value) {
		Collections.sort(value.classesHere);
		for (String c : value.classesHere) {
			TreeItem ii = new TreeItem(i, 0);
			ii.setText("<i>" + c + "</i>");
			ii.setData(c);
		}
		Collections.sort(value.children, new Comparator<Node>() {

			@Override
			public int compare(Node arg0, Node arg1) {
				return String.CASE_INSENSITIVE_ORDER.compare(arg0.name, arg1.name);
			}
		});
		for (Node c : value.children) {
			TreeItem ii = new TreeItem(i, 0);
			ii.setText(c.name);
			ii.setData(c.name);
			populateLoaded(ii, c);
		}
	}

	private Node nodeFor(Node p, String string) {
		Node n = packageList.get(string);
		if (n == null) {
			n = new Node(string);
			if (p != null) {
				p.children.add(n);
			}
			n.parent = p;
			packageList.put(string, n);
		}
		return n;
	}

	private void populateCaught(TreeItem i, ReloadingDomain d) {

		Set<String> k = d.loaded.keySet();

		String longest = longestCommonSubstring(k);

		for (String kk : k) {
			String kks = kk.replace(longest, "");
			TreeItem ii = new TreeItem(i, 0);
			ii.setText("<i>" + kks + "</i>");
			ii.setData(d.loaded.get(kk));
		}
	}

	public String longestCommonSubstring(Collection<String> ss) {
		if (ss.size() < 2)
			return "";
		Iterator<String> iterator = ss.iterator();
		String longest = iterator.next();
		while (iterator.hasNext()) {
			String q = iterator.next();

			int max = -1;
			for (int i = 0; i < Math.min(q.length(), longest.length()); i++) {
				if (q.charAt(i) != longest.charAt(i)) {
					max = i;
					break;
				}
			}

			if (max == -1)
				continue;
			longest = longest.substring(0, max);
		}
		return longest;
	}

	@Override
	protected String getPluginNameImpl() {
		return "reload";
	}

}
