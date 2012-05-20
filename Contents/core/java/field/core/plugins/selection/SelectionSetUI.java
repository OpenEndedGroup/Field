package field.core.plugins.selection;

import java.util.Arrays;
import java.util.LinkedHashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import field.bytecode.protect.Woven;
import field.core.Platform;
import field.core.Platform.OS;
import field.core.plugins.history.ElementFileSystemTree.ObjectTransfer;
import field.core.ui.GraphNodeToTreeFancy;
import field.core.ui.MacScrollbarHack;
import field.core.ui.SmallMenu;
import field.core.ui.SmallMenu.BetterPopup;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.util.PythonUtils;

@Woven
public class SelectionSetUI {

	Composite toolbar;
	protected Tree tree;
	Composite container;
	private String frameName;
	private Button gearButton;


	/**
	 * This is the default constructor
	 */
	public SelectionSetUI(String name, ToolBarFolder folder) {
		frameName = name;

		this.container = new Composite(folder.getContainer(), SWT.NO_BACKGROUND);

		folder.add(name, container);

		toolbar = new Composite(container, SWT.BACKGROUND);
		Color backgroundColor = folder.firstLineBackground;
		toolbar.setBackground(backgroundColor);
		// container.setBackground(backgroundColor);

		tree = new Tree(container, SWT.H_SCROLL | SWT.V_SCROLL | SWT.NO_BACKGROUND);

		new GraphNodeToTreeFancy.Pretty(tree, 50);

		DropTarget target = new DropTarget(tree, DND.DROP_COPY | DND.DROP_LINK);

		target.setTransfer(new Transfer[]{FileTransfer.getInstance()});
		target.addDropListener(new DropTargetListener() {

			@Override
			public void dropAccept(DropTargetEvent arg0) {
				;//System.out.println(" -- accept--- "+arg0.data);
				
			}

			@Override
			public void drop(DropTargetEvent arg0) {
				;//System.out.println(" -- drop--"+((String[])arg0.data)[0]);

				
				handleDrop(((String[])arg0.data)[0]);
				
			}

			@Override
			public void dragOver(DropTargetEvent arg0) {

				;//System.out.println(" -- dragover--");
				
				arg0.detail = DND.DROP_COPY;
				
			}

			@Override
			public void dragOperationChanged(DropTargetEvent arg0) {

				if (arg0.detail == DND.DROP_DEFAULT) {
					arg0.detail = DND.DROP_COPY;
				}
			}

			@Override
			public void dragLeave(DropTargetEvent arg0) {

			}

			@Override
			public void dragEnter(DropTargetEvent arg0) {
				;//System.out.println(" -- enter--"+arg0.data+Arrays.asList(arg0.dataTypes));
				
				if (arg0.detail == DND.DROP_DEFAULT) {
					arg0.detail = DND.DROP_COPY;
				}
			}
		});

		// tree.setBackground(backgroundColor);

		// gearButton = new Button(tree, SWT.FLAT);
		// gearButton.setText("g");
		// GridData ignore = new GridData();
		// ignore.exclude = true;
		// gearButton.setLayoutData(ignore);

		// tree.addListener(SWT.Paint, new Listener() {
		//
		// @Override
		// public void handleEvent(Event event) {
		// LinkedHashMap<String, iUpdateable> items = getMenuItems();
		// if (items == null) return;
		//
		// if (items.size()>0)
		// {
		// Rectangle ca = tree.getClientArea();
		// gearButton.setEnabled(true);
		// ;//System.out.println(" setting bounds <"+ca+">");
		// gearButton.setBounds(ca.width-50+ca.x, 5+ca.y, 25, 25);
		// gearButton.redraw();
		// }
		// else
		// {
		// Rectangle ca = tree.getClientArea();
		// gearButton.setEnabled(false);
		// ;//System.out.println(" setting bounds <"+ca+">");
		// gearButton.setBounds(ca.width-50+ca.y, 5+ca.y, 25, 25);
		// gearButton.redraw();
		// }
		// }
		// });
		//
		// gearButton.addListener(SWT.Selection, new Listener(){
		//
		// @Override
		// public void handleEvent(Event event) {
		// LinkedHashMap<String, iUpdateable> items = getMenuItems();
		// BetterPopup m = new SmallMenu().createMenu(items,
		// gearButton.getShell(), null);
		// m.show(Launcher.display.map(gearButton,
		// gearButton.getShell(), new
		// Point(event.x, event.y)));
		// }});

		GridLayout gl = new GridLayout(1, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;

		if (Platform.getOS() == OS.linux) {
			gl.marginHeight = 0;
			gl.marginWidth = 0;
			gl.verticalSpacing = 0;
			gl.horizontalSpacing = 0;
		}

		container.setLayout(gl);
		{
			GridData data = new GridData();
			data.heightHint = 28;
			if (Platform.getOS() == OS.linux) {
				data.heightHint = 33;
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
			tree.setLayoutData(data);
		}

		RowLayout rl = new RowLayout();
		rl.fill = true;
		toolbar.setLayout(rl);

		tree.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				;//System.out.println(" selection changed ...");
				selectionChanged(tree.getSelection());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		tree.addListener(SWT.MouseDown, new Listener() {

			@Override
			public void handleEvent(Event event) {
				if (Platform.isPopupTrigger(event)) {
					LinkedHashMap<String, iUpdateable> items = getMenuItems();
					popUpMenu(event, items);
					BetterPopup m = new SmallMenu().createMenu(items, tree.getShell(), null);
					m.show(Launcher.display.map(tree, tree.getShell(), new Point(event.x, event.y)));
				} else if (event.count == 2) {
					TreeItem item = tree.getSelection()[0];
					doubleClickRow(item);
				}
			}
		});

		tree.setBackground(ToolBarFolder.background);

		new MacScrollbarHack(tree);
	}

	protected void handleDrop(String string) {
		
		
		
	}

	protected void selectionChanged(TreeItem[] selection) {

	}

	public Tree getTree() {
		return tree;
	}

	public Composite getToolbarPanel() {
		return toolbar;
	}

	// TODO selectionSet
	protected LinkedHashMap<String, iUpdateable> getMenuItems() {
		return null;
	}

	// TODO selectionSet
	protected void doubleClickRow(TreeItem item) {
	}

	// TODO selectionSet
	public void setLabel(String label) {
	}

	// TODO selectionSEt
	public void setWaterText(String string) {

	}

	// TODO selectionSEt
	protected void popUpMenu(Event ev, LinkedHashMap<String, iUpdateable> u) {
	}
}