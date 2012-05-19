package field.core.ui;

import java.util.LinkedHashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import field.core.ui.SmallMenu.BetterPopup;
import field.launch.Launcher;
import field.launch.iUpdateable;

public class BetterComboBoxToolItem {

	private String[] labels;

	protected float redHighlight = 0;

	int currentlySelected = 0;

	boolean suspend = false;

	// public Button combo;

	private Menu menu;

	public String titleText = "Editable Properties...";

	public ToolItem combo;


	private ToolBar inside;

	public BetterComboBoxToolItem(final ToolBar inside, String[] labels) {

		this.inside = inside;
		combo = new ToolItem(inside, SWT.PUSH | SWT.FLAT | SWT.LEFT);

		this.labels = labels;
		// combo.setFont(new Font(combo.getFont().getDevice(),
		// combo.getFont().getFontData()[0].name, (int)
		// (combo.getFont().getFontData()[0].height * 0.85),
		// SWT.NORMAL));

		for (int i = 0; i < labels.length; i++)
			labels[i] = dehtml(labels[i]);

		updateLabels();

		combo.setText(labels[0]);

		combo.addListener(SWT.MouseDown, new Listener() {
			@Override
			public void handleEvent(Event e) {
				try {
					LinkedHashMap<String, iUpdateable> map = new LinkedHashMap<String, iUpdateable>();
					if (titleText != null) {
						map.put(titleText, null);
					}
					for (int n = 0; n < BetterComboBoxToolItem.this.labels.length; n++) {
						final int fn = n;
						map.put((n == currentlySelected ? "!" : "") + BetterComboBoxToolItem.this.labels[n], new iUpdateable() {

							@Override
							public void update() {
								currentlySelected = fn;
								combo.setText(BetterComboBoxToolItem.this.labels[currentlySelected]);
								updateSelection(fn, BetterComboBoxToolItem.this.labels[currentlySelected]);
							}
						});
					}

					BetterPopup b = new SmallMenu().createMenu(map, inside.getShell(), null);
					Point x = Launcher.display.map(inside.getShell(), inside.getShell(), new Point(e.x, e.y));

					b.show(x);
				} catch (Throwable t) {
					t.printStackTrace();
				}

			}
		});

	}

	private String dehtml(String string) {
		return string.replaceAll("<(.+?)>", "");
	}

	public void addOption(String label) {
		label = dehtml(label);
		String[] n = new String[labels.length + 1];
		System.arraycopy(labels, 0, n, 0, labels.length);
		n[labels.length] = label;
		labels = n;
	}

	public void forceSelection(String label) {
		for (int n = 0; n < labels.length; n++) {
			if (labels[n].equals(label)) {
				currentlySelected = n;
				combo.setText(label);
				inside.redraw();
			}
		}

		// Launcher.getLauncher().registerUpdateable(new iUpdateable() {
		//
		// int t = 0;
		//
		// public void update() {
		// BetterComboBox.this.redHighlight = 1 - t / 10f;
		// t++;
		// if (t == 11) {
		// BetterComboBox.this.redHighlight = 0;
		// Launcher.getLauncher().deregisterUpdateable(this);
		// }
		// combo.redraw();
		// }
		// });
	}

	protected void finishSelection() {

	}

	// TODO swt \u2014 red splash

	// @Override
	// public void paint(Graphics g) {
	// super.paint(g);
	// if (redHighlight > 0) {
	// Color c = new Color(redHighlight, 0, 0, redHighlight / 4);
	// g.setColor(c);
	// ((Graphics2D) g).fill(this.getBounds());
	// }
	//
	// }

	public void selectNext() {
		currentlySelected = (currentlySelected + 1) % labels.length;
		combo.setText(labels[currentlySelected]);
		inside.redraw();
		updateSelection(currentlySelected, labels[currentlySelected]);
	}

	public void setLabels(String[] labels) {

		for (int i = 0; i < labels.length; i++)
			labels[i] = dehtml(labels[i]);

		String c = this.labels[currentlySelected];
		this.labels = labels;
		for (int i = 0; i < this.labels.length; i++)
			if (labels[i].equals(c))
				currentlySelected = i;
		if (currentlySelected >= labels.length)
			currentlySelected = labels.length - 1;

		combo.setText(labels[currentlySelected]);
	}

	public void updateLabels() {
	}

	public void updateSelection(int index, String text) {
	}

	public String[] getLabels() {
		return labels;
	}

	public void selectNextWithSkip() {
		int n = 0;
		do {
			selectNext();
			n++;
		} while (shouldSkip(currentlySelected) && n < 100);
	}

	protected boolean shouldSkip(int index) {
		return false;
	}
}
