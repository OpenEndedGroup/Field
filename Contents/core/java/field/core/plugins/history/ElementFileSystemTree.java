package field.core.plugins.history;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NewThread;
import field.core.Constants;
import field.core.Platform;
import field.core.Platform.OS;
import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.persistance.FluidCopyPastePersistence;
import field.core.persistance.FluidStreamParser;
import field.core.persistance.PackageTools;
import field.core.plugins.python.PythonPlugin;
import field.core.plugins.selection.ToolBarFolder;
import field.core.ui.FieldMenus2;
import field.core.ui.GraphNodeToTreeFancy;
import field.core.ui.MacScrollbarHack;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.DraggableComponent;
import field.core.windowing.components.iComponent;
import field.core.windowing.overlay.OverlayAnimationManager;
import field.launch.Launcher;
import field.launch.SystemProperties;
import field.launch.iUpdateable;
import field.math.linalg.Vector2;
import field.namespace.generic.Generics.Pair;
import field.namespace.generic.Generics.Triple;
import field.util.PythonUtils;

@Woven
public class ElementFileSystemTree {

	Tree tree;
	private File root;
	private Composite toolbar;
	ToolBarFolder open;

	static public class ObjectTransfer extends ByteArrayTransfer {
		static public String name = "field_object";
		static public int id = Transfer.registerType(name);

		@Override
		protected int[] getTypeIds() {
			return new int[] { id };
		}

		@Override
		protected String[] getTypeNames() {
			return new String[] { name };
		}

		@Override
		protected void javaToNative(Object object, TransferData transferData) {

			;//System.out.println(" java to native <" + object + " -> " + transferData + ">");

			if (object == null)
				return;
			if (isSupportedType(transferData)) {
				String o = new PythonUtils().toXML(object, false);
				super.javaToNative(o.getBytes(), transferData);
			}
		}

		@Override
		protected Object nativeToJava(TransferData transferData) {
			;//System.out.println(" native to java " + transferData);
			if (isSupportedType(transferData)) {
				return new PythonUtils().fromXML(new String((byte[]) super.nativeToJava(transferData)));
			}
			return super.nativeToJava(transferData);
		}
	}

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

	public ElementFileSystemTree() {

		Composite target = GLComponentWindow.lastCreatedWindow.leftComp1;

		open = ToolBarFolder.currentFolder;

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
		Link label = new Link(toolbar, SWT.MULTI | SWT.NO_BACKGROUND | SWT.CENTER);
		label.setText("Filesystem");
		label.setFont(new Font(Launcher.display, label.getFont().getFontData()[0].getName(), label.getFont().getFontData()[0].getHeight() + 2, SWT.NORMAL));
		label.setBackground(ToolBarFolder.firstLineBackground);

		toolbar.setLayout(new GridLayout(1, true));
		GridData gd = new GridData(SWT.CENTER, SWT.CENTER, true, true);
		// gd.verticalIndent = 1;
		label.setLayoutData(gd);

		root = new File(SystemProperties.getDirProperty("versioning.dir", System.getProperty("user.home") + "/Documents/FieldWorkspace/"));

		File[] f = root.listFiles(new FileFilter() {

			@Override
			public boolean accept(File arg0) {

				;//System.out.println(" check <" + arg0 + "> <" + new File(arg0, "sheet.xml") + ">");

				return (arg0.isDirectory() && new File(arg0, "sheet.xml").exists());
			}
		});

		if (f != null) {

			TreeItem templateRoot = new TreeItem(tree, 0);
			templateRoot.setText("<i>Templates</i>");
			templateRoot.setData(new TemplateRootMarker() {
			});
			File[] tt = new File(root, "templates").listFiles(new FileFilter() {

				@Override
				public boolean accept(File arg0) {
					return arg0.getName().endsWith(".template");

				}
			});

			if (tt != null)
				for (File ff : tt) {
					TreeItem i = new TreeItem(templateRoot, 0);
					i.setText("<b>" + ff.getName().replace(".template", "") + "</b>");
					i.setData(new TemplateMarker(ff));
				}

			TreeItem recentRoot = new TreeItem(tree, 0);
			recentRoot.setText("<i>Recent</i>");
			recentRoot.setData(new TemplateRootMarker() {
			});

			File[] fs = new File[f.length];
			System.arraycopy(f, 0, fs, 0, fs.length);
			Arrays.sort(fs, new Comparator<File>() {

				@Override
				public int compare(File arg0, File arg1) {
					return -Double.compare(new File(arg0, "sheet.xml").lastModified(), new File(arg1, "sheet.xml").lastModified());
				}
			});

			for (int j = 0; j < Math.min(10, fs.length); j++) {
				TreeItem i = new TreeItem(recentRoot, 0);
				i.setText("<b>" + fs[j].getName().replace(".field", "") + "</b>");
				i.setData(fs[j]);
				TreeItem dummy = new TreeItem(i, 0);
				dummy.setText("(loading...)");
			}

			for (File ff : f) {
				TreeItem i = new TreeItem(tree, 0);
				i.setText("<b>" + ff.getName().replace(".field", "") + "</b>");
				;//System.out.println(" adding " + ff);
				i.setData(ff);
				TreeItem dummy = new TreeItem(i, 0);
				dummy.setText("(loading...)");
			}
		}

		tree.setBackground(ToolBarFolder.background);

		tree.addListener(SWT.Expand, new Listener() {

			@Override
			public void handleEvent(Event event) {

				TreeItem item = (TreeItem) event.item;

				Object x = item.getData();

				if (x instanceof File) {
					expandFile((File) x, item);
				} else if (x instanceof Pair) {
					Pair<File, Pair<String, HashMap<String, Object>>> p = (Pair<File, Pair<String, HashMap<String, Object>>>) x;
					disposeChildren(item);
					expandElement(p, item);
				}

			}
		});

		tree.addListener(SWT.MouseDoubleClick, new Listener() {
			@Override
			public void handleEvent(Event event) {
				;//System.out.println(" mouse double click <" + event + "> <" + event.item + ">");

				TreeItem[] s = tree.getSelection();
				;//System.out.println("        selection is :" + Arrays.asList(s));

				if (s.length == 1) {
					if (s[0].getData() instanceof File) {
						FieldMenus2.fieldMenus.openAnyFile(((File) s[0].getData()).getAbsolutePath(), tree.getShell());
					}
				}

			}
		});

		DragSource source = new DragSource(tree, DND.DROP_COPY);
		source.setTransfer(new Transfer[] { transfer });
		source.addDragListener(new DragSourceListener() {

			@Override
			public void dragStart(DragSourceEvent event) {
				TreeItem[] item = tree.getSelection();
				if (item.length > 0) {
					event.doit = true;
					lastInternalDragSelection = item;
				} else
					event.doit = false;
			}

			@Override
			public void dragSetData(DragSourceEvent event) {

				event.data = lastInternalDragSelection[0].getData();
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
			}
		});

		new MacScrollbarHack(tree);
	}

	protected void disposeChildren(TreeItem item) {
		TreeItem[] items = item.getItems();
		for (int i = 0; i < items.length; i++) {
			items[i].dispose();
		}
	}

	static public class SheetDropSupport {
		private DropTarget target;

		public SheetDropSupport(final Canvas canvas, final iVisualElement root) {
			target = new DropTarget(canvas, DND.DROP_COPY | DND.DROP_DEFAULT);
			target.setTransfer(new Transfer[] { transfer, FileTransfer.getInstance() });
			target.addDropListener(new DropTargetListener() {

				@Override
				public void dropAccept(DropTargetEvent event) {
					;//System.out.println(" -- accept--");
				}

				@Override
				public void drop(DropTargetEvent event) {
					;//System.out.println(" -- drop-- :" + event.data);

					if (event.data instanceof String[]) {
						GLComponentWindow gk = iVisualElement.enclosingFrame.get(root);
						Vector2 a = new Vector2(event.x * gk.getXScale() + gk.getXTranslation(), event.y * gk.getXScale() + gk.getYTranslation());

						a.x -= gk.getFrame().getBounds().x;
						a.y -= gk.getFrame().getBounds().y;
						a.x -= gk.getCanvas().getParent().getBounds().x;
						a.x -= 40;
						a.y -= 40;

						for (String s : ((String[]) event.data)) {
							importFile(s, root, a);
							a.y += 55;
						}
					}

					if (event.data instanceof Pair) {
						Pair<File, Pair<String, HashMap<String, Object>>> p = (Pair<File, Pair<String, HashMap<String, Object>>>) event.data;

						HashSet<iVisualElement> loaded = FluidCopyPastePersistence.copyFromNonloaded(Collections.singleton(p.right.left), p.left.getAbsolutePath(), root, iVisualElement.copyPaste.get(root));
						Vector2 center = new Vector2();
						for (iVisualElement ee : loaded) {
							Rect f = ee.getFrame(null);
							center.add(f.midpoint2());
						}
						center.scale(1f / loaded.size());

						;//System.out.println(" event is <" + event.x + " " + event.y + "> center is " + center + " of " + loaded);

						GLComponentWindow gk = iVisualElement.enclosingFrame.get(root);
						Vector2 a = new Vector2(event.x * gk.getXScale() + gk.getXTranslation(), event.y * gk.getXScale() + gk.getYTranslation());

						a.x -= gk.getCanvas().getParent().getBounds().x;
						a.x -= gk.getFrame().getBounds().x;
						a.y -= gk.getFrame().getBounds().y;
//						a.x -= 40;
//						a.y -= 40;

						for (iVisualElement ee : loaded) {
							Rect f = ee.getFrame(null);
							f.x += -center.x + a.x;
							f.y += -center.y + a.y;
							ee.setFrame(f);
						}

						OverlayAnimationManager.notifyAsText(root, "Inserted '" + p.right.right.get("name") + "' into this sheet", null);

					} else if (event.data instanceof File) {
						HashSet<iVisualElement> loaded = FluidCopyPastePersistence.copyFromNonloaded(null, ((File) event.data).getAbsolutePath() + "/sheet.xml", root, iVisualElement.copyPaste.get(root));
						Vector2 center = new Vector2();
						for (iVisualElement ee : loaded) {
							Rect f = ee.getFrame(null);
							center.add(f.midpoint2());
						}
						center.scale(1f / loaded.size());

						;//System.out.println(" event is <" + event.x + " " + event.y + "> center is " + center + " of " + loaded);

						GLComponentWindow gk = iVisualElement.enclosingFrame.get(root);
						Vector2 a = new Vector2((event.x - gk.getFrame().getBounds().x - gk.getCanvas().getParent().getBounds().x) * gk.getXScale() + gk.getXTranslation(), (event.y - gk.getFrame().getBounds().y) * gk.getXScale() + gk.getYTranslation());

						for (iVisualElement ee : loaded) {
							Rect f = ee.getFrame(null);
							f.x += -center.x + a.x;
							f.y += -center.y + a.y;
							ee.setFrame(f);
						}

						OverlayAnimationManager.notifyAsText(root, "Inserted '" + ((File) event.data).getName() + "' into this sheet", null);

					} else if (event.data instanceof Triple) {
						GLComponentWindow gk = iVisualElement.enclosingFrame.get(root);
						Vector2 a = new Vector2((event.x - gk.getFrame().getBounds().x - gk.getCanvas().getParent().getBounds().x) * gk.getXScale() + gk.getXTranslation(), (event.y - gk.getFrame().getBounds().y) * gk.getXScale() + gk.getYTranslation());
						// a.x -= 40;
						// a.y -= 40;
						iComponent hut = gk.getRoot().hit(gk, new Vector2(a));

						;//System.out.println(" hit test again <" + a + " -> " + hut);

						if (hut != null && hut.getVisualElement() != null) {
							VisualElementProperty prop = new VisualElementProperty(((Triple<File, String, Object>) event.data).middle);
							prop.set(hut.getVisualElement(), hut.getVisualElement(), ((Triple<File, String, Object>) event.data).right);

							OverlayAnimationManager.notifyAsText(root, "Set property '" + ((Triple<File, String, Object>) event.data).middle + "' in '" + iVisualElement.name.get(hut.getVisualElement()) + "'", null);

							iVisualElement.dirty.set(hut.getVisualElement(), hut.getVisualElement(), true);
						}
					} else if (event.data instanceof TemplateMarker) {
						GLComponentWindow gk = iVisualElement.enclosingFrame.get(root);
						Vector2 a = new Vector2((event.x - gk.getFrame().getBounds().x - gk.getCanvas().getParent().getBounds().x) * gk.getXScale() + gk.getXTranslation(), (event.y - gk.getFrame().getBounds().y) * gk.getXScale() + gk.getYTranslation());

						new PackageTools().importFieldPackage(root, ((TemplateMarker) event.data).f.getAbsolutePath(), a);
					}

				}

				@Override
				public void dragOver(DropTargetEvent event) {
					;//System.out.println(" -- over--");
					;//System.out.println(" event is <" + event.x + " " + event.y + "> <" + lastInternalDragSelection + ">");

					if (lastInternalDragSelection != null) {
						;//System.out.println(" data : " + lastInternalDragSelection[0].getData());
						if (lastInternalDragSelection[0].getData() instanceof Triple) {
							GLComponentWindow gk = iVisualElement.enclosingFrame.get(root);
							Vector2 a = new Vector2((event.x - gk.getFrame().getBounds().x - gk.getCanvas().getParent().getBounds().x) * gk.getXScale() + gk.getXTranslation(), (event.y - gk.getFrame().getBounds().y) * gk.getXScale() + gk.getYTranslation());
							// Vector2 a = new
							// Vector2((event.x -
							// gk.getCanvas().getParent().getBounds().x)
							// *
							// gk.getXScale() +
							// gk.getXTranslation(),
							// event.y *
							// gk.getXScale() +
							// gk.getYTranslation());

							// a.x -= 40;
							// a.y -= 40;

							iComponent hut = gk.getRoot().hit(gk, new Vector2(a));

							;//System.out.println(" hit test again <" + a + " -> " + hut);

							if (hut != null && hut.getVisualElement() != null) {
								event.detail = DND.DROP_COPY;
								;//System.out.println(" element is <" + hut.getVisualElement() + ">");
							} else {
								event.detail = DND.DROP_NONE;
							}
						}
					}
				}

				@Override
				public void dragOperationChanged(DropTargetEvent event) {
					;//System.out.println(" -- opchaged--");
					if (event.detail == DND.DROP_DEFAULT) {
						event.detail = DND.DROP_COPY;
					}

				}

				@Override
				public void dragLeave(DropTargetEvent event) {
					;//System.out.println(" -- leave--");
				}

				@Override
				public void dragEnter(DropTargetEvent event) {
					;//System.out.println(" -- enter --");
					if (event.detail == DND.DROP_DEFAULT) {
						event.detail = DND.DROP_COPY;
					}
				}
			});
		}

		protected void importFile(String s, iVisualElement root, Vector2 a) {

			try {
				
				;//System.out.println(" importing file <"+s+">");
				
				BufferedReader r = new BufferedReader(new FileReader(new File(s)));
				StringBuffer contents = new StringBuffer();
				while (r.ready()) {
					contents.append(r.readLine() + "\n");
				}

				Triple<VisualElement, DraggableComponent, DefaultOverride> n = VisualElement.createWithName(new Rect(a.x, a.y, 50, 50), root, VisualElement.class, DraggableComponent.class, DefaultOverride.class, new File(s).getName());

				PythonPlugin.python_source.set(n.left, n.left, contents.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected void expandElement(Pair<File, Pair<String, HashMap<String, Object>>> p, TreeItem item) {
		TreeMap<String, Object> m = new TreeMap<String, Object>(p.right.right);
		for (Entry<String, Object> ee : m.entrySet()) {
			if (ee.getKey().equals("name"))
				continue;
			if (ee.getValue() != null) {
				String f = safePrint(ee.getValue());
				if ((ee.getValue().toString()).length() > 0 && !ignoredProperties.contains(ee.getKey().toString())) {
					TreeItem x = new TreeItem(item, 0);
					x.setText("<i>" + ee.getKey() + "</i> = " + f);
					x.setData(new Triple<File, String, Object>(p.left, ee.getKey(), ee.getValue()));
				}
			}
		}
	}

	private String safePrint(Object value) {
		String x = value.toString();
		if (x.length() > 30)
			if (x instanceof String) {
				String[] x0 = x.split("\n");
				if (x0[0].trim().length() == 0)
					return "((long text))";
				else
					return x0[0].substring(0, Math.min(30, x0[0].length())) + ((x0.length > 1) ? " ... (+" + (x0.length - 1) + " line" + (x0.length - 1 == 1 ? "" : "s") + ")" : "");
			} else {
				x = x.replace("\n", " ").replace("\r", " ");
				return x.substring(0, 30) + " ... ";
			}
		x = x.replace("\n", " ").replace("\r", " ");
		String nn = x.getClass().getName();
		if (nn.lastIndexOf('.') != -1)
			nn.substring(nn.lastIndexOf('.') + 1);
		return x + " <font size=-3 color='#" + Constants.defaultTreeColorDim + "'>" + nn + "</font>";
	}

	static HashSet<String> ignoredProperties = new HashSet<String>();
	static {
		ignoredProperties.add("dirty");
		ignoredProperties.add("python_customInsertPersistanceInfo");
		ignoredProperties.add("python_areas");
		ignoredProperties.add("hasMouseFocus");
	}

	@NewThread
	protected void expandFile(File x, final TreeItem item) {
		x = new File(x, "sheet.xml");

		final File fx = x;

		FluidStreamParser p = new FluidStreamParser(x, true);
		final HashMap<String, HashMap<String, Object>> pmap = p.getProperties();
		final Map<String, HashMap<String, Object>> properties = new TreeMap<String, HashMap<String, Object>>(new Comparator<String>() {

			@Override
			public int compare(String arg0, String arg1) {
				Object n0 = pmap.get(arg0).get("name");
				Object n1 = pmap.get(arg1).get("name");

				if (n0 == null)
					return 0;
				if (n1 == null)
					return 0;

				return ((String) n0+arg0).compareTo(((String) n1+arg1));
			}
		});
		properties.putAll(pmap);

		System.out.println(" final map "+properties.keySet());
		
		Launcher.getLauncher().registerUpdateable(new iUpdateable() {

			@Override
			public void update() {

				TreeItem[] items = item.getItems();

				for (Entry<String, HashMap<String, Object>> ee : properties.entrySet()) {
					if (ee.getValue().get("name") != null && !ee.getValue().get("name").equals("timeSlider") && !ee.getValue().containsKey("lineDrawing_from")) {
						TreeItem i = new TreeItem(item, 0);
						i.setText("<i> " + ee.getValue().get("name") + " </i>");
						i.setData(new Pair<File, Pair<String, HashMap<String, Object>>>(fx, new Pair(ee.getKey(), ee.getValue())));
						new TreeItem(i, 0);
					}
				}

				for (int i = 0; i < items.length; i++) {
					items[i].dispose();
				}
				Launcher.getLauncher().deregisterUpdateable(this);

			}
		});
	}
}
