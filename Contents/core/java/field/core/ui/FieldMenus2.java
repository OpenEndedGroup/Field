package field.core.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import field.core.Platform;
import field.core.Platform.OS;
import field.core.StandardFluidSheet;
import field.core.execution.PhantomFluidSheet;
import field.core.persistance.PackageTools;
import field.core.plugins.log.Logging;
import field.core.util.ExecuteCommand;
import field.launch.Launcher;
import field.launch.SystemProperties;
import field.launch.iUpdateable;
import field.util.WorkspaceDirectory;

public class FieldMenus2 {

	static public FieldMenus2 fieldMenus = new FieldMenus2();

	static public class Sheet {
		public PhantomFluidSheet sheet;
		public Shell setup;
		public String filename = "";
		boolean auto = false;
	}

	public List<Sheet> openSheets = new ArrayList<FieldMenus2.Sheet>();
	public Shell hiddenWindow;

	public FieldMenus2() {
		Logging.registerCycleUpdateable();
		hiddenWindow = new Shell(Launcher.display);

		if (Platform.getOS() == OS.mac && false) {
			CocoaUIEnhancer enhancer = new CocoaUIEnhancer("Field");
			enhancer.hookApplicationMenu(Launcher.display, new iUpdateable() {

				@Override
				public void update() {

					MessageBox mb = new MessageBox(hiddenWindow, SWT.OK | SWT.ICON_INFORMATION);
					mb.setText("About Field");
					mb.setMessage("Field 15 - http://openendedgroup.com/field");
					mb.open();

				}
			}, new iUpdateable() {

				@Override
				public void update() {
					;// System.out.println(" preferences ");
				}
			});
		}

		System.out.println(" -- registering open document handler --");
		Launcher.display.addListener(SWT.OpenDocument, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				System.out.println(" -- open document handler --  <"+arg0.text+">");
				openAnyFile(arg0.text, hiddenWindow);
			}
		});
		System.out.println(" -- registering open document handler complete --");

		Menu appMenuBar = Launcher.display.getMenuBar();

		if (Launcher.display.getSystemMenu() != null && Platform.getOS() == OS.mac) {

			Menu q = Launcher.display.getSystemMenu();

			final MenuItem mia = new MenuItem(q, SWT.SEPARATOR, q.getItemCount() - 2);

			final MenuItem mi = new MenuItem(q, SWT.CHECK, q.getItemCount() - 2);
			mi.setText("Use OpenJDK 1.7");
			boolean is17 = Platform.is17();
			if (is17) {
				mi.setSelection(true);
			} else {
				mi.setSelection(false);
			}

			mi.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent arg0) {
					Platform.willBe17 = !Platform.willBe17;
					mi.setText("Use OpenJDK 1.7 (restart required)");
					mi.setSelection(Platform.willBe17);

					new ExecuteCommand(".", new String[] { "/usr/bin/defaults", "write", "com.openendedgroup.Field", "use16", Platform.willBe17 ? "NO" : "YES" }, true);

				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetDefaultSelected(arg0);
				}
			});

		}

		if (appMenuBar != null) {
			MenuItem filei = new MenuItem(appMenuBar, SWT.CASCADE);
			filei.setText("File");

			Menu file = new Menu(null, SWT.DROP_DOWN);
			filei.setMenu(file);

			MenuItem newFile = new MenuItem(filei.getMenu(), 0);
			newFile.setText("New File...");
			newFile.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					doNewFile();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});

			MenuItem openFile = new MenuItem(filei.getMenu(), 0);
			openFile.setText("Open File...");
			openFile.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {

					;// System.out.println(" OPEN ");

					doOpenFile(hiddenWindow);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});

		}
	}

	public PhantomFluidSheet open(String filename) {
		for (Sheet s : openSheets)
			if (s.filename.equals(filename))
				return s.sheet;

		PhantomFluidSheet loaded = new PhantomFluidSheet(filename, true, true);

		final Sheet s = new Sheet();
		s.sheet = loaded;
		s.setup = loaded.getUI().getWindow().getFrame();
		s.filename = filename;
		openSheets.add(s);

		s.sheet.getUI().getWindow().getFrame().addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (!Launcher.shuttingDown) {
					doClose(s);
				}
			}
		});

		makeMenuForSheet(s);

		s.sheet.getUI().getWindow().getFrame().setText("Field (" + filename + ")");

		return s.sheet;
	}

	private void makeMenuForSheet(final Sheet s) {

		if (Platform.isLinux())
			return;

		Menu bar = s.setup.getMenuBar();
		if (bar == null) {
			bar = new Menu(s.setup, SWT.BAR);
			s.setup.setMenuBar(bar);
		}
		MenuItem filei = new MenuItem(bar, SWT.CASCADE);
		filei.setText("File");

		Menu file = new Menu(s.setup, SWT.DROP_DOWN);
		filei.setMenu(file);

		MenuItem newFile = new MenuItem(filei.getMenu(), 0);
		newFile.setText("New File...");
		newFile.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				doNewFile();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		MenuItem openFile = new MenuItem(filei.getMenu(), 0);
		openFile.setText("Open File...");
		openFile.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				;// System.out.println(" OPEN ");

				doOpenFile(s.setup);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		MenuItem closeFile = new MenuItem(filei.getMenu(), 0);
		closeFile.setText("Close");
		closeFile.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				doClose(s);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		MenuItem saveAsFile = new MenuItem(filei.getMenu(), 0);
		saveAsFile.setText("Save As...");
		saveAsFile.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				doSaveAs(s, s.setup);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

	}

	public Sheet sheetForSheet(StandardFluidSheet s) {
		for (Sheet ss : openSheets) {
			if (ss.sheet.getUI() == s)
				return ss;
		}
		return null;
	}

	boolean insideSaveAs = false;

	protected void doClose(Sheet s) {

		if (insideSaveAs)
			return;

		if (openSheets.size() == 1) {
			try {
				s.sheet.saveFacefull();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.exit(0);
		} else {
			s.sheet.getUI().saveNow();
			s.sheet.close();
			openSheets.remove(s);
		}
		// if (openSheets.size() == 0) {
		// System.exit(0);
		// }
	}

	protected void doOpenFile(Shell setup) {

		if (Platform.isMac()) {
			FileDialog d = new FileDialog(setup, SWT.OPEN);
			d.setFilterPath(WorkspaceDirectory.dir[0]);
			d.setOverwrite(false);
			d.setFilterExtensions(new String[] { ".field" });
			d.setText("Load");
			String name = d.open();
			openAnyFile(name, d.getParent());
		} else {
			DirectoryDialog d = new DirectoryDialog(setup, SWT.OPEN);
			d.setFilterPath(WorkspaceDirectory.dir[0]);
			d.setText("Load (select a .field directory)");
			String name = d.open();
			openAnyFile(name, d.getParent());

		}
	}

	public void openAnyFile(String path, Shell parent) {

		if (path.endsWith(".fieldpackage")) {
			new PackageTools().importFieldPackage(openSheets.get(0).sheet.getRoot(), path);
		} else if (path.startsWith(getCanonicalVersioningDir())) {
			open(path.substring(getCanonicalVersioningDir().length()));
		} else {
			new PathNotInWorkspaceHelperMenu2(this).open(parent, path);
		}

	}

	public void doSaveAs(Sheet s, Shell setup) {
		FileDialog d = new FileDialog(setup, SWT.SAVE | SWT.SHEET);
		d.setOverwrite(true);
		d.setFilterPath(WorkspaceDirectory.dir[0]);
		d.setFilterExtensions(new String[] { ".field" });

		String fn = d.open();

		;// System.out.println(" -- fn : " + fn);

		if (fn != null) {

			if (!fn.endsWith(".field"))
				fn = fn + ".field";
			fn = fn.replace("..field", ".field");
			fn = fn.replace("..", ".");

			// check path prefix

			if (fn.startsWith(getCanonicalVersioningDir())) {

				if (new File(fn).mkdir()) {

					insideSaveAs = true;
					try {
						s.sheet.setFilename(fn, false);

						// fn =
						// fn.substring(getCanonicalVersioningDir().length());

						openSheets.remove(s);
						openAnyFile(fn, hiddenWindow);
					} finally {
						insideSaveAs = false;
					}

				} else {
					;// System.out.println(" no mkdir ");
				}
			} else {
				;// System.out.println(" no prefix <" + fn +
					// "> <" + getCanonicalVersioningDir() +
					// ">");
			}
		}
	}

	public void doNewFile() {

		FileDialog d = new FileDialog(hiddenWindow, SWT.SAVE);
		d.setOverwrite(false);
		d.setFilterPath(WorkspaceDirectory.dir[0]);

		String fn = d.open();
		if (fn != null) {
			if (!fn.endsWith(".field")) {
				fn = fn + ".field";
			}
			fn = fn.replace("..", "");
			openAnyFile(fn, hiddenWindow);
		}

	}

	static public String fieldDir = System.getProperty("user.home") + "/Library/Application Support/Field";
	static {
		getFieldDir();
	}

	static public String getFieldDir() {
		if (!new File(fieldDir).exists())
			new File(fieldDir).mkdirs();
		return fieldDir;
	}

	public static String getCanonicalVersioningDir() {
		try {
			return new File(SystemProperties.getDirProperty("versioning.dir")).getCanonicalPath() + "/";
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
