package field.extras.jsrscripting;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import field.core.Platform;
import field.core.Platform.OS;
import field.core.dispatch.iVisualElement;
import field.core.plugins.BaseSimplePlugin;
import field.core.plugins.selection.ToolBarFolder;
import field.core.ui.GraphNodeToTreeFancy;
import field.launch.Launcher;
import field.namespace.generic.ReflectionTools;
import field.util.AutoPersist;

public class JSRPlugin extends BaseSimplePlugin {

	static public HashMap<String, String> knownLanguages = new LinkedHashMap<String, String>();
	static public HashMap<String, String> knownTests = new LinkedHashMap<String, String>();

	static {
		// knownLanguages.put("JavaScript",
		// JavaScriptScriptingInterface.class.getName());

		knownLanguages.put("JavaC", JavaCScriptingInterface.class.getName());
		knownLanguages.put("Clojure", ClojureScriptingInterface.class.getName());
		knownLanguages.put("JRuby", JRubyScriptingInterface.class.getName());
		//knownLanguages.put("Nashorn", NashornScriptingInterface.class.getName());
		//knownLanguages.put("Scala", ScalaScriptingInterface.class.getName());

		knownTests.put("JavaC", JavaCScriptingInterface.class.getName());
		knownTests.put("Clojure", ClojureScriptingInterface.class.getName());
		knownTests.put("JRuby", JRubyScriptingInterface_test.class.getName());
		//knownTests.put("Nashorn", NashornScriptingInterface.class.getName());
		//knownTests.put("Scala", ScalaScriptingInterface.class.getName());
	}

	static public HashSet<String> attemptedLanguages = new AutoPersist().persist("JSRPlugin.attemptedLanguages", new HashSet<String>());

	static public HashMap<String, Object> instantiatedInterfaces = new LinkedHashMap<String, Object>();

	public boolean[] shouldAttempt = new AutoPersist().persist("JSRPlugin.shouldAttempt", new boolean[] { false });

	private Composite toolbar;

	@Override
	protected String getPluginNameImpl() {
		return "jsrscripting";
	}

	@Override
	public void registeredWith(final iVisualElement root) {
		super.registeredWith(root);

		;// ;//System.out.println(" ??? -- clojure -- ???");

		ToolBarFolder stack = ToolBarFolder.currentFolder;

		Composite container = new Composite(stack.getContainer(), Platform.isMac() ? SWT.NO_BACKGROUND : 0);

		toolbar = new Composite(container, 0);
		Color backgroundColor = ToolBarFolder.firstLineBackground;
		toolbar.setBackground(backgroundColor);

		container.setBackground(ToolBarFolder.background);

		GridLayout gl = new GridLayout(1, true);
		gl.marginHeight = 0;
		gl.marginWidth = 0;

		if (Platform.getOS() == OS.linux) {
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

			RowLayout rl = new RowLayout();
			rl.fill = true;
			toolbar.setLayout(rl);
		}

		Composite ui = new Composite(container, Platform.isMac() ? SWT.NO_BACKGROUND : 0);
		{
			GridData data = new GridData();
			data.grabExcessVerticalSpace = true;
			data.grabExcessHorizontalSpace = true;
			data.verticalAlignment = SWT.FILL;
			data.horizontalAlignment = SWT.FILL;
			ui.setLayoutData(data);
		}

		ui.setBackground(ui.getParent().getBackground());
		toolbar.setLayout(new GridLayout(1, true));

		Label where = new Label(toolbar, SWT.CENTER);
		where.setText("There are " + knownLanguages.size() + " additional languages known to Field");
		where.setFont(new Font(Launcher.display, where.getFont().getFontData()[0].getName(), GraphNodeToTreeFancy.baseFontHeight(where), SWT.NORMAL));
		where.setBackground(where.getParent().getBackground());
		GridData gd = new GridData(SWT.CENTER, SWT.CENTER, true, true);
		gd.verticalIndent = 1;
		where.setLayoutData(gd);

		GridLayout rl = new GridLayout(1, true);
		ui.setLayout(rl);

		// ToolBarPaletteInspector stack =
		// SavedFramePositions.getCurrentInspector();
		//
		// JPanel ui = new JPanel();
		// JLabel where = new JLabel("<html><font face=\"" +
		// Constants.defaultFont + "\">There are <b>" +
		// knownLanguages.size() +
		// "</b> additional (and optional) languages known to Field:</font>");
		// ui.add(where);
		// where.setHorizontalAlignment(where.CENTER);
		HashMap<String, Button> buttons = new HashMap<String, Button>();

		for (final String s : knownLanguages.keySet()) {
			final Button b = new Button(ui, SWT.FLAT | SWT.WRAP);
			// UbiquitousLinks.links.install(b);
			// b.setHorizontalAlignment(b.LEFT);
			// b.setFont(new Font(Constants.defaultFont, 0, 10));
			String classname = knownLanguages.get(s);
			try {
				Class<?> test = this.getClass().getClassLoader().loadClass(knownTests.get(s));
				final Class<?> potential = this.getClass().getClassLoader().loadClass(knownLanguages.get(s));
				String error = (String) test.getMethod("installed", JSRPlugin.class).invoke(null, this);
				boolean e = !error.equals("");
				if (!e)
					error = "click to initialize";
				else {
					error = "(not installed)	 '" + error + "'";
					b.setFont(new Font(Launcher.display, Launcher.display.getSystemFont().getFontData()[0].name, (int) (Launcher.display.getSystemFont().getFontData()[0].height * 0.8), Launcher.display.getSystemFont().getFontData()[0].style));
				}

				b.setText(s + " - " + error);
				// b.putClientProperty("Quaqua.Button.style",
				// "square");

				b.setBackground(b.getParent().getBackground());
				GridData g = new GridData();
				g.grabExcessHorizontalSpace = true;
				g.widthHint = 400;
				b.setLayoutData(g);

				// RowData d = new RowData();
				// d.width = 400;
				// b.setLayoutData(d);

				b.addListener(SWT.Selection, new Listener() {

					public void handleEvent(Event arg0) {
						try {
							Object n = (Object) potential.newInstance();
							instantiatedInterfaces.put(s, n);
							ReflectionTools.findFirstMethodCalled(n.getClass(), "setRoot").invoke(n, root);
							attemptedLanguages.add(s);
							b.setText(s + " - installed");
						} catch (InstantiationException e1) {
							b.setText(s + " - " + e1.getMessage());
							e1.printStackTrace();
						} catch (IllegalAccessException e1) {
							b.setText(s + " - " + e1.getMessage());
							e1.printStackTrace();
						} catch (Throwable t) {
							t.printStackTrace();
							attemptedLanguages.remove(s);
						}
					}
				});

				if (e)
					b.setEnabled(false);

				buttons.put(s, b);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
		}

		final Button checkbox = new Button(ui, SWT.CHECK);
		checkbox.setText("Always initialize selected languages on startup");
		checkbox.setBackground(checkbox.getParent().getBackground());
		checkbox.setFont(new Font(Launcher.display, where.getFont().getFontData()[0].getName(), GraphNodeToTreeFancy.baseFontHeight(checkbox), SWT.NORMAL));

		if (shouldAttempt[0]) {

			checkbox.setSelection(true);
			for (String s : attemptedLanguages) {
				;// ;//System.out.println(" attemping <" + s +
					// ">");
				Button bb = buttons.get(s);
				if (bb != null && bb.isEnabled()) {
					;// ;//System.out.println(" doing ----------- ");
					bb.setSelection(true);
					for (Listener m : bb.getListeners(SWT.Selection))
						m.handleEvent(null);
				}
			}
		} else {
			attemptedLanguages.clear();
		}
		checkbox.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				shouldAttempt[0] = checkbox.getSelection();
			}
		});

		ui.layout();

		stack.add("icons/lambda.png", container);
	}

	static public Object getInterface(String name) {
		return (Object) instantiatedInterfaces.get(name);
	}

}
