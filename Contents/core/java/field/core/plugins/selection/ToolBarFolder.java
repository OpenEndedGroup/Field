package field.core.plugins.selection;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.Platform;
import field.core.Platform.OS;
import field.core.windowing.GLComponentWindow;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.namespace.generic.ReflectionTools;
import field.util.AutoPersist;

@Woven
public class ToolBarFolder {

	public static ToolBarFolder currentFolder = null;
	public static ToolBarFolder helpFolder = null;

	private Composite container;
	private ToolBar toolbar;
	private final Composite into;
	private StackLayout stack;

	private Composite tools;

	// TODO mac only?
	public static Color background = Platform.isMac()  ? new Color(Launcher.display, 200, 200, 200) : new Color(Launcher.display, 200, 200, 200);
	public static Color firstLineBackground = Platform.isMac() ? new Color(Launcher.display, 170, 170, 170) : new Color(Launcher.display, 220, 220, 220);
	public static Color sashBackground = Platform.isMac()  ? new Color(Launcher.display, 200, 200, 200) : new Color(Launcher.display, 200, 200, 200);

	private HashMap<ToolItem, SelectionListener> listeners = new HashMap<ToolItem, SelectionListener>();

	static public Rectangle defaultRect = new AutoPersist().persist("toolBarPosition", new Rectangle(700, 150, 300, 400));

	public ToolBarFolder() {
		this(defaultRect);
	}

	static boolean useGLComponentWindowLeftSash = true;

	public ToolBarFolder(final Rectangle r) {
		this(r, false);
	}

	public ToolBarFolder(final Rectangle r, boolean forceWindow) {
		if (useGLComponentWindowLeftSash && !forceWindow) {
			if (Platform.isMac())
				tools = new Composite(GLComponentWindow.lastCreatedWindow.leftComp1, SWT.NO_BACKGROUND);
			
			else if (Platform.isLinux())
			{
				tools = new Composite(GLComponentWindow.lastCreatedWindow.leftComp1, 0);
				tools.setBackground(ToolBarFolder.firstLineBackground);
			}
		
		} else {
			tools = new Shell(Launcher.display, SWT.SHELL_TRIM | (Platform.getOS() == OS.mac ? (SWT.TOOL | SWT.ON_TOP) : 0));
			((Shell) tools).setText("Tools");
		}

		FillLayout fl = new FillLayout();
		fl.marginHeight = 0;
		fl.marginWidth = 0;
		fl.spacing = 0;
		tools.setLayout(fl);

		tools.setBounds(r);

		tools.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent e) {

				Rectangle b = tools.getBounds();
				r.x = b.x;
				r.y = b.y;
				r.width = b.width;
				r.height = b.height;

				;//System.out.println(" shell <" + tools + "> resized <" + b + ">");

				;//System.out.println(" toolbar has <" + toolbar.getItemCount() + "> items");

				ToolItem[] items = toolbar.getItems();
				for (ToolItem c : items) {
					;//System.out.println(" item :" + c + " " + c.getBounds());
				}
			}

			@Override
			public void controlMoved(ControlEvent e) {
				Rectangle b = tools.getBounds();
				r.x = b.x;
				r.y = b.y;
				r.width = b.width;
				r.height = b.height;

				;//System.out.println(" shell <" + tools + "> moved <" + b + ">");

				;//System.out.println(" toolbar has <" + toolbar.getItemCount() + "> items");

				ToolItem[] items = toolbar.getItems();
				for (ToolItem c : items) {
					;//System.out.println(" item :" + c + " " + c.getBounds());
				}
			}
		});

		this.into = tools;

		GridLayout gl = new GridLayout(1, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.verticalSpacing = 0;

		// not ready yet
		// toolbar = tools.getToolBar();

		if (toolbar == null) {

			toolbar = new ToolBar(into, SWT.NO_BACKGROUND | SWT.NO_FOCUS | SWT.FLAT);
			{
				GridData data = new GridData();
				data.heightHint = Platform.isMac() ? 23 : 33;
				data.widthHint = 1000;
				data.horizontalAlignment = SWT.FILL;
				data.grabExcessHorizontalSpace = true;
				toolbar.setLayoutData(data);
			}

			toolbar.setBackground(background);
		}
		container = new Composite(into, (Platform.isMac() ? SWT.NO_BACKGROUND : 0) | SWT.NO_FOCUS | SWT.FLAT);
		{
			GridData data = new GridData();
			data.grabExcessVerticalSpace = true;
			data.grabExcessHorizontalSpace = true;
			data.verticalAlignment = SWT.FILL;
			data.horizontalAlignment = SWT.FILL;
			container.setLayoutData(data);
		}
		
		if (Platform.isLinux())
		{
			container.setBackground(background);
		}

		into.setLayout(gl);
		stack = new StackLayout();
		stack.marginHeight = 0;
		stack.marginWidth = 0;
		container.setLayout(stack);

		// RowLayout rl = new RowLayout();
		// rl.marginHeight = 3;
		// rl.marginWidth = 3;
		// rl.spacing = 3;
		// toolbar.setLayout(rl);

		Composite cc = new Composite(container, 0);
		stack.topControl = cc;
		container.layout();

		System.err.println(" about to open tools ");
		deferredOpen();
		System.err.println(" about to open tools finished ");

		try {
			if (Platform.getOS() == OS.mac)
				ReflectionTools.findFirstMethodCalled(ReflectionTools.illegalGetObject(tools, "window").getClass(), "setHidesOnDeactivate").invoke(ReflectionTools.illegalGetObject(tools, "window"), true);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (Throwable t) {
		}

		if (useGLComponentWindowLeftSash) {
			GLComponentWindow.lastCreatedWindow.leftComp1.layout();
		}
	}

	@NextUpdate(delay = 10)
	protected void deferredOpen() {
		if (tools instanceof Shell)
			((Shell) tools).open();
	}

	public Shell getShell() {
		if (tools instanceof Shell)
			return ((Shell) tools);
		return null;
	}

	public Composite getContainer() {
		return container;
	}

	public void add(Image icon, final Control c) {

		ToolItem toolItem = new ToolItem(toolbar, SWT.FLAT | SWT.RADIO | SWT.NO_FOCUS);
		toolItem.setImage(icon);
		
		SelectionListener sl = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				stack.topControl = c;
				c.setBounds(0, 0, container.getSize().x, container.getSize().y);
				container.layout();
				container.redraw();
				into.setBounds(into.getLocation().x, into.getLocation().y, into.getSize().x, into.getSize().y);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
		toolItem.addSelectionListener(sl);
		listeners.put(toolItem, sl);

		into.layout(true);
		toolbar.traverse(SWT.TRAVERSE_TAB_NEXT);
	}

	public void add(final String icon, final Control c) {

		ToolItem toolItem = new ToolItem(toolbar, SWT.FLAT | SWT.RADIO | SWT.NO_FOCUS);

		if (icon.startsWith("icons/")) {
			Image ii = (new Image(Launcher.display, icon.replace("32", "16").replace("icons/", "icons/grey/")));
			toolItem.setImage(ii);
		} else {
			toolItem.setText(icon);
		}
		// toolItem.setImage(new Image(Launcher.display,
		// "/Users/marc/Downloads/open-source-icons/PNG/grey/configuration.png"));
		SelectionListener sl = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				stack.topControl = c;
				c.setBounds(0, 0, container.getSize().x, container.getSize().y);
				container.layout();
				container.redraw();
				into.setBounds(into.getLocation().x, into.getLocation().y, into.getSize().x, into.getSize().y);

				// tools.setText("Tools - " + icon);

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
		toolItem.addSelectionListener(sl);
		listeners.put(toolItem, sl);

		// Point s = into.getSize();
		// toolbar.pack();
		into.layout(true);
		// into.setSize(s);

		// toolbar.traverse(SWT.TRAVERSE_TAB_NEXT);

	}

	public ToolItem add(final Image icon, final Control c, final iUpdateable u) {

		ToolItem toolItem = new ToolItem(toolbar, SWT.FLAT | SWT.RADIO | SWT.NO_FOCUS);
		toolItem.setImage(icon);

		SelectionListener sl = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				stack.topControl = c;
				c.setBounds(0, 0, container.getSize().x, container.getSize().y);
				container.layout();
				container.redraw();
				into.setBounds(into.getLocation().x, into.getLocation().y, into.getSize().x, into.getSize().y);

				if (u != null)
					u.update();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};

		toolItem.addSelectionListener(sl);
		listeners.put(toolItem, sl);
		into.layout(true);


		return toolItem;
		
	}

	public void selectFirst() {

		select(0);
	}

	public void select(int n) {
		ToolItem[] items = toolbar.getItems();
		for (ToolItem tt : items) {
			tt.setSelection(false);
		}
		toolbar.getItem(n).setSelection(true);
		listeners.get(toolbar.getItem(n)).widgetSelected(null);
	}

	public int get() {
		ToolItem[] items = toolbar.getItems();
		for (int i=0;i<items.length;i++) {
			if (items[i].getSelection()) return i;
		}
		return -1;
	}

}
