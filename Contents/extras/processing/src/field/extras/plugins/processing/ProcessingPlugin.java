package field.extras.plugins.processing;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.python.core.PySystemState;

import field.bytecode.protect.Trampoline2;
import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.execution.iExecutesPromise;
import field.core.plugins.BaseSimplePlugin;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.python.PythonPluginEditor;
import field.core.plugins.selection.ToolBarFolder;
import field.core.ui.GraphNodeToTreeFancy;
import field.core.ui.NewInspector2;
import field.core.ui.NewInspector2.BooleanControl;
import field.core.ui.NewInspector2.Status;
import field.core.ui.NewInspector2.iIO;
import field.core.ui.text.PythonTextEditor.EditorExecutionInterface;
import field.core.windowing.GLComponentWindow;
import field.launch.Launcher;
import field.launch.SystemProperties;
import field.launch.iUpdateable;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.linalg.Vector4;
import field.util.AutoPersist;

public class ProcessingPlugin extends BaseSimplePlugin {

	public class LocalOverride extends DefaultOverride {
		@Override
		public <T> VisitCode getProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> ref) {

			if (prop.equals(iExecutesPromise.promiseExecution)) {
				if (needsProcessing(source)) {
					ref.set((T) shim.getExecutesPromise((iExecutesPromise) ref.get()));
				}
			} else if (prop.equals(PythonPluginEditor.editorExecutionInterface)) {
				if (needsProcessing(source)) {
					ref.set((T) shim.getEditorExecutionInterface((EditorExecutionInterface) ref.get()));
				}
			}
			return super.getProperty(source, prop, ref);
		}

		@Override
		public VisitCode menuItemsFor(final iVisualElement source, Map<String, iUpdateable> items) {
			if (source != null) {
				if (needsProcessing(source)) {
					items.put("Processing", null);
					items.put("\u24c5 <b>Remove bridge</b> to Processing", new iUpdateable() {
						public void update() {
							needsProcessing.set(source, source, false);
							iVisualElement.dirty.set(source, source, true);
						}
					});
				} else {
					items.put("Processing", null);
					items.put("\u24c5 Bridge element to <b>Processing</b>", new iUpdateable() {
						public void update() {
							needsProcessing.set(source, source, true);
							iVisualElement.dirty.set(source, source, true);
						}
					});
				}
			}
			return super.menuItemsFor(source, items);
		}

		@Override
		public VisitCode paintNow(iVisualElement source, Rect bounds, boolean visible) {
			Boolean n = source.getProperty(needsProcessing);
			if (n != null && n) {
				if (GLComponentWindow.currentContext != null && GLComponentWindow.draft) {
					CachedLine l = new CachedLine();
					l.getInput().moveTo((float) (bounds.x + bounds.w - 12), (float) (bounds.y + bounds.h - 12));
					l.getInput().setPointAttribute(iLinearGraphicsContext.text_v, " p ");
					l.getInput().setPointAttribute(iLinearGraphicsContext.textIsBlured_v, true);
					l.getInput().setPointAttribute(iLinearGraphicsContext.font_v, new java.awt.Font("Gill Sans", java.awt.Font.ITALIC, 25));
					l.getInput().setPointAttribute(iLinearGraphicsContext.fillColor_v, new Vector4(0, 0, 0, 0.4f));
					l.getProperties().put(iLinearGraphicsContext.containsText, true);
					GLComponentWindow.currentContext.submitLine(l, l.getProperties());
				}
			}
			return super.paintNow(source, bounds, visible);
		}

	}

	static public final VisualElementProperty<Boolean> needsProcessing = new VisualElementProperty<Boolean>("needsProcessing");

	static public String rootProcessingPath = SystemProperties.getProperty("processingApplication", "<<search>>");

	static public String processingShim = "field.extras.plugins.processing.ProcessingLoader";

	private iProcessingLoader shim;

	static public boolean alreadyLoaded = false;

	boolean stillborn = false;

	@Override
	public void close() {
		super.close();
		if (shim != null)
			shim.close();

		// VM crash if this is uncommented in.
		// alreadyLoaded = false;

		;//;//System.out.println(" ----------- processing is closing ---------");

	}

	public boolean needsProcessing(iVisualElement source) {
		Object o = needsProcessing.get(source);
		if (o == null)
			return false;
		if (o instanceof Boolean)
			return ((Boolean) o).booleanValue();
		if (o instanceof Number)
			return ((Number) o).intValue() > 0;
		return false;
	}

	boolean[] injectProcessing = { false };

	private boolean error;

	private boolean nothing;
	static public int[] defaultProcessingFullscreen = { 0 };
	static public int[] defaultProcessingRenderer = { 0 };

	static {
		defaultProcessingFullscreen = new AutoPersist().persist("processingFullscreen", defaultProcessingFullscreen);

		defaultProcessingRenderer = new AutoPersist().persist("processingRenderer", defaultProcessingRenderer);

	}

	static boolean warnOnce = true;
	public boolean ontop = false;
	
	@Override
	public void registeredWith(final iVisualElement root) {
		if (alreadyLoaded) {
			// stillborn = true;
			//
			// if (warnOnce)
			// JOptionPane.showMessageDialog(null,
			// "Currently the Processing Plugin only works with one sheet at a time \u2014 this second sheet will not be able to access your applet (although it can do everything else).\nTo start up with the sheet that you are opening, close all other sheets and select 'Automatically open at launch' from the file menu or set a file from the command line explicitly.");
			//
			// warnOnce = false;
			//
			// return;
		} else {
		}

		alreadyLoaded = true;

		super.registeredWith(root);

		error = false;
		nothing = false;

		if (rootProcessingPath == "<<search>>") {
			rootProcessingPath = searchForProcessing();
		}
		if (rootProcessingPath == null) {
			nothing = true;
		} else if (!new File(rootProcessingPath).exists()) {
			System.err.println("warning: cannot find processing application at <" + rootProcessingPath + ">");
			error = true;
		} else {
				if (Platform.isMac())
				{
					Trampoline2.trampoline.addJar(new File(rootProcessingPath+"/Contents/Resources/Java/core.jar").getAbsolutePath());
					try {
						Trampoline2.trampoline.addWildcardPath(new File(rootProcessingPath+"/Contents/Resources/Java/modes/java/libraries/opengl/library/").getAbsolutePath());
						Trampoline2.trampoline.addWildcardPath(new File(rootProcessingPath+"/Contents/Resources/Java/modes/java/libraries/bin/").getAbsolutePath());
					} catch (MalformedURLException e1) {
						e1.printStackTrace();
					}
				}
				else
				{
					Trampoline2.trampoline.addJar(new File(rootProcessingPath+"/lib/core.jar").getAbsolutePath());
					try {
						Trampoline2.trampoline.addWildcardPath(new File(rootProcessingPath+"/modes/java/libraries/opengl/library/").getAbsolutePath());
						Trampoline2.trampoline.addWildcardPath(new File(rootProcessingPath+"/modes/java/libraries/bin/").getAbsolutePath());
					} catch (MalformedURLException e1) {
						e1.printStackTrace();
					}
					
				}


			error = true;

			try {
				shim = (iProcessingLoader) Trampoline2.trampoline.getClassLoader().loadClass(processingShim).getConstructor(iVisualElement.class).newInstance(root);
				error = false;

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}

		}

		if (!error && shim == null) {
			nothing = true;
			error = true;
		}

		if (!error) {

			;//;//System.out.println(" -- about to throw an exception -- ");
			try {
				shim.init();
			} catch (RuntimeException e) {
			}
			shim.update();
			;//;//System.out.println(" -- finished --");

		}

		extendClasspath();

		System.err.println(" processing plugin complete");

		// and some UI

		// DiscloseStack stack =
		// WorkspaceManager.getManager
		// ().getDiscloseStack();

		// ToolBarPaletteInspector stack =
		// SavedFramePositions.getCurrentInspector();

		ToolBarFolder folder = ToolBarFolder.currentFolder;

		if (nothing) {
			Composite status = new Composite(folder.getContainer(), 0);

			folder.add("P", status);

			;//;//System.out.println(" status is :" + nothing + " " + error);
			status.setBackground(ToolBarFolder.firstLineBackground);

			GridLayout gl = new GridLayout(1, true);
			gl.marginHeight = 5;
			gl.marginWidth = 0;

			Link label = new Link(status, SWT.MULTI | SWT.NO_BACKGROUND | SWT.CENTER);
			label.setText("Didn't find a Processing app");
			label.setFont(new Font(Launcher.display, label.getFont().getFontData()[0].getName(), GraphNodeToTreeFancy.baseFontHeight(label) + 2, SWT.NORMAL));
			label.setBackground(ToolBarFolder.firstLineBackground);

			status.setLayout(gl);
			GridData gd = new GridData(SWT.CENTER, SWT.TOP, true, true);
			gd.verticalIndent = 1;
			label.setLayoutData(gd);

		} else if (error) {
			Composite status = new Composite(folder.getContainer(), 0);

			folder.add("P", status);

			;//;//System.out.println(" status is :" + nothing + " " + error);
			status.setBackground(ToolBarFolder.firstLineBackground);

			GridLayout gl = new GridLayout(1, true);
			gl.marginHeight = 5;
			gl.marginWidth = 0;
			Link label = new Link(status, SWT.MULTI | SWT.NO_BACKGROUND | SWT.CENTER);
			label.setText("Processing bridge didn't initialize");
			if (Platform.is17() && Platform.isMac())
			{
				label.setText("Processing bridge didn't initialize � please deselect \"Use Open JFK 1.7\" from the app menu and restart Field");
			}
			label.setFont(new Font(Launcher.display, label.getFont().getFontData()[0].getName(), GraphNodeToTreeFancy.baseFontHeight(label) + 2, SWT.NORMAL));
			label.setBackground(ToolBarFolder.firstLineBackground);

			status.setLayout(gl);
			GridData gd = new GridData(SWT.CENTER, SWT.TOP, true, true);
			gd.verticalIndent = 1;
			label.setLayoutData(gd);

		} else {
			// Link label = new Link(status, SWT.MULTI |
			// SWT.NO_BACKGROUND | SWT.CENTER);
			// label.setText("Field/Processing has initialized");
			// label.setFont(new Font(Launcher.display,
			// label.getFont().getFontData()[0].getName(),
			// label.getFont().getFontData()[0]
			// .getHeight() + 2, SWT.NORMAL));
			// label.setBackground(ToolBarFolder.firstLineBackground);
			// status.setLayout(gl);
			// GridData gd = new GridData(SWT.CENTER, SWT.TOP, true,
			// true);
			// gd.verticalIndent=1;
			// gd.grabExcessHorizontalSpace = true;
			// gd.heightHint = 25;
			//
			// label.setLayoutData(gd);

			injectProcessing = new AutoPersist().persist("injectProcessing", injectProcessing);

			ScrolledComposite scroller;
			Composite contents;

			scroller = new ScrolledComposite(folder.getContainer(), SWT.VERTICAL | SWT.HORIZONTAL);
			contents = new Composite(scroller, SWT.NONE);

			NewInspector2 inspect = new NewInspector2("P", folder, scroller, contents);

			inspect.new InfoControl(new iIO<Object>("Field/Processing is enabled") {

				@Override
				public Object getValue() {
					return null;
				}

				@Override
				public Status getStatus() {
					return null;
				}

				@Override
				public void setValue(Object s) {
				}
			});

			inspect.new BooleanControl(new iIO<Boolean>("Inject namespace") {

				@Override
				public Boolean getValue() {
					return injectProcessing[0];
				}

				@Override
				public Status getStatus() {
					return Status.valid;
				}

				@Override
				public void setValue(Boolean s) {
					if (s)
						shim.injectIntoGlobalNamespace();
				}
			});

			inspect.new BooleanControl(new iIO<Boolean>("Always inject") {

				@Override
				public Boolean getValue() {
					return injectProcessing[0];
				}

				@Override
				public Status getStatus() {
					return Status.valid;
				}

				@Override
				public void setValue(Boolean s) {
					injectProcessing[0] = s;
				}
			});

			BooleanControl c = inspect.new BooleanControl(new iIO<Boolean>("Fullscreen ?") {

				@Override
				public Boolean getValue() {
					return defaultProcessingFullscreen[0] > 0;
				}

				@Override
				public Status getStatus() {
					return Status.valid;
				}

				@Override
				public void setValue(Boolean s) {
					defaultProcessingFullscreen[0] = s ? 1 : 0;
				}
			});


			c.setUnits("(restart required)");

			BooleanControl o = inspect.new BooleanControl(new iIO<Boolean>("On top ?") {
				
				@Override
				public Boolean getValue() {
					return ontop;
				}
				
				@Override
				public Status getStatus() {
					return Status.valid;
				}
				
				@Override
				public void setValue(Boolean s) {
					ontop = s;
					shim.setOntop(s);
				}
			});

		}

		if (injectProcessing[0]) {
			shim.injectIntoGlobalNamespace();
		}

	}

	private void extendClasspath() {
		if (field.core.Platform.isMac()) {
			Trampoline2.trampoline.addExtensionsDirectory(new File(rootProcessingPath + "/Contents/Resources/Java/"));

			try {
				Trampoline2.trampoline.addWildcardPathRecursively(new File(rootProcessingPath).getAbsolutePath());
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			}

			addLibraryPathRecursively(new File(rootProcessingPath + "/Contents/Resources/Java/"));
		} else {
			Trampoline2.trampoline.addExtensionsDirectory(new File(rootProcessingPath + "/lib/"));
			try {
				Trampoline2.trampoline.addWildcardPathRecursively(new File(rootProcessingPath).getAbsolutePath());
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			}

			addLibraryPathRecursively(new File(rootProcessingPath + "/lib/"));
		}
	}

	@Override
	public void update() {
		if (shim != null)
			shim.update();
	}

	private void addLibraryPathRecursively(File file) {

		;//;//System.out.println(" adding extensions directory <" + file + ">");

		// Trampoline2.trampoline.addExtensionsDirectory(file);

		if (file.isDirectory()) {
			PySystemState.add_extdir(file.getAbsolutePath());
		}

		String can;
		try {

			can = file.getCanonicalPath();
			if (file.isDirectory()) {
				for (File f : file.listFiles()) {
					if (f.isDirectory()) {
						if (f.getCanonicalPath().startsWith(can)) {
							addLibraryPathRecursively(f);
						}
					}

					// else if
					// (f.getCanonicalPath().endsWith(".jar"))
					// {
					// ;//;//System.out.println(" adding package
					// <" + f +
					// ">");
					// PyJavaPackage p =
					// PySystemState.add_package(f.getCanonicalPath());
					// p.fillDir();
					// ;//;//System.out.println(" filled <" +
					// p.__dir__()
					// + ">");
					// }
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private String searchForProcessing() {

		String[] lookIn = { "/Applications", System.getProperty("user.home") };

		for (String l : lookIn) {
			File appDir = new File(l);
			File[] d = appDir.listFiles(new FileFilter() {

				public boolean accept(File arg0) {
					boolean a = arg0.getName().toLowerCase().startsWith("processing");
					return a;
				}
			});

			if (d != null && d.length > 0) {
				for (File f : d) {

					;//;//System.out.println(" looking at <" + f + ">");

					if (f.isDirectory() && field.core.Platform.isMac()) {
						if (f.getName().toLowerCase().startsWith("processing") && f.getName().toLowerCase().endsWith(".app"))
							return f.getAbsolutePath();
						File[] ff = f.listFiles(new FileFilter() {
							public boolean accept(File arg0) {
								if (arg0.getName().toLowerCase().startsWith("processing") && arg0.getName().toLowerCase().endsWith(".app"))
									return true;
								return false;
							}
						});
						if (ff != null && ff.length > 0) {
							return ff[0].getAbsolutePath();
						}
					}
					if (f.isDirectory() && field.core.Platform.isLinux()) {
						if (f.getName().toLowerCase().startsWith("processing"))
							return f.getAbsolutePath();
						File[] ff = f.listFiles(new FileFilter() {
							public boolean accept(File arg0) {
								if (arg0.getName().toLowerCase().startsWith("processing"))
									return true;
								return false;
							}
						});
						if (ff != null && ff.length > 0) {
							return ff[0].getAbsolutePath();
						}
					}
				}

			}
		}
		return null;
	}

	@Override
	protected String getPluginNameImpl() {
		return "processingplugin";
	}

	@Override
	protected DefaultOverride newVisualElementOverrides() {
		if (stillborn)
			return new DefaultOverride();
		return new LocalOverride();
	}

}
