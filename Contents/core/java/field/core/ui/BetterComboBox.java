package field.core.ui;

import java.util.LinkedHashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import field.core.ui.SmallMenu.BetterPopup;
import field.core.ui.SmallMenu.iHoverUpdate;
import field.launch.Launcher;
import field.launch.iUpdateable;

public class BetterComboBox {
    
	private String[] labels;
    
	protected float redHighlight = 0;
    
	public int currentlySelected = 0;
    
	boolean suspend = false;
    
	// public Button combo;
    
	private Menu menu;
    
	public String titleText = "Editable Properties...";
    
	public Button combo;
    
	private iHoverUpdate hu;
    
	protected boolean rightJustified = false;
	protected boolean noDrag = false;
	protected boolean fixedText = false;
    
	public BetterComboBox(Composite inside, String[] labels) {
        
		combo = new Button(inside, SWT.FLAT | SWT.LEFT);
        
		this.labels = labels;
        
		combo.setFont(new Font(combo.getFont().getDevice(), combo.getFont().getFontData()[0].name, (int) (GraphNodeToTreeFancy.baseFontHeight(combo)), SWT.NORMAL));
        
		for (int i = 0; i < labels.length; i++)
			labels[i] = dehtml(labels[i]);
        
		updateLabels();
        
		combo.setText(labels[0]);
        
		combo.addMouseListener(new MouseListener() {
            
			@Override
			public void mouseUp(MouseEvent e) {
				if (noDrag)
					go(e);
			}
            
			@Override
			public void mouseDown(MouseEvent e) {
                
				if (!noDrag)
					go(e);
			}
            
			private void go(MouseEvent e) {
				System.out.println(" -- making menu ");
				try {
					makeMenu();
				} catch (Exception ee) {
					ee.printStackTrace();
				}
				System.out.println(" -- making menu complete ");
                
				try {
					LinkedHashMap<String, iUpdateable> map = new LinkedHashMap<String, iUpdateable>();
					if (titleText != null) {
						map.put(titleText, null);
					}
					for (int n = 0; n < BetterComboBox.this.labels.length; n++) {
						final int fn = n;
						map.put((n == currentlySelected ? "!" : "") + BetterComboBox.this.labels[n], new iUpdateable() {
                            
							@Override
							public void update() {
								currentlySelected = fn;
								if (!fixedText)
									combo.setText(BetterComboBox.this.labels[currentlySelected]);
								updateSelection(fn, BetterComboBox.this.labels[currentlySelected]);
							}
						});
					}
                    
					BetterPopup b = new SmallMenu().createMenu(map, combo.getShell(), null, hu);
					Point x = Launcher.display.map(combo, combo.getShell(), new Point(e.x, e.y));
                    
					if (rightJustified) {
						b.setRightJustified(true);
					}
                    
					b.show(x);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
            
			@Override
			public void mouseDoubleClick(MouseEvent e) {
			}
		});
        
	}
    
	protected void makeMenu() {
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
				if (!fixedText)
                    combo.setText(label);
				combo.redraw();
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
		if (!fixedText)
            combo.setText(labels[currentlySelected]);
		combo.redraw();
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
        
		if (currentlySelected != -1 && !fixedText)
			combo.setText(labels[currentlySelected]);
	}
    
	public void setLabelsWithHTML(String[] labels) {
        
		String c = currentlySelected == -1 ? "" : this.labels[currentlySelected];
		this.labels = labels;
		for (int i = 0; i < this.labels.length; i++)
			if (labels[i].equals(c))
				currentlySelected = i;
		if (currentlySelected >= labels.length)
			currentlySelected = labels.length - 1;
        
		// if (currentlySelected!=-1)
		// combo.setText(labels[currentlySelected]);
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
    
	public int getCurrentlySelected() {
		return currentlySelected;
	}
    
	public void setHoverUpdate(iHoverUpdate iHoverUpdate) {
		this.hu = iHoverUpdate;
        
	}
    
	public BetterComboBox setRightJustified(boolean rightJustified) {
		this.rightJustified = rightJustified;
		return this;
	}
    
	public BetterComboBox setNoDrag(boolean noDrag) {
		this.noDrag = noDrag;
		return this;
	}
	
	public BetterComboBox setFixedText(boolean fixedText) {
		this.fixedText = fixedText;
		return this;
	}
}
