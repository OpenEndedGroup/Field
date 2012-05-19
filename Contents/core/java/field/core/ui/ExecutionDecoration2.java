package field.core.ui;



import javax.swing.SwingUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;

import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.iComponent;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.linalg.Vector2;
import field.namespace.generic.Bind.iFunction;

public abstract class ExecutionDecoration2 {

	private Vector2 downAt;
	private final iComponent component;

	public ExecutionDecoration2(iComponent component) {
		this.component = component;

	}

	public iFunction<Boolean, iComponent> down(Event event) {
		if ((event.stateMask & SWT.ALT)!=0) {
			downAt = new Vector2(event.x, event.y);
			if (isExecuting() && isPaused()) {
				continueToBeActiveAfterUp();
				showUnPauseMenu();
			} else if (isExecuting()) {
				continueToBeActiveAfterUp();
				showStopMenu();

				return null;
			} else {
				// need to defer this a frame to see if we end
				// up "isExecuting"
				return new iFunction<Boolean, iComponent>() {
					public Boolean f(iComponent d) {
						if (isExecuting()) {
							showStartMenu();
						} else {
						}
						return false;
					}
				};
			}
		}
		return null;
	}

	protected void showStartMenu() {
		MarkingMenuBuilder builder = new MarkingMenuBuilder();
		builder.marking("Continue...", "NH");
		builder.call(new iUpdateable() {
			public void update() {
				continueToBeActiveAfterUp();
			}
		});
		
		GLComponentWindow.getCurrentWindow(component).transformDrawingToWindow(downAt);
		Point s = new Point((int) downAt.x, (int) downAt.y);

		s = Launcher.display.map(GLComponentWindow.getCurrentWindow(component).getCanvas(), null, s);

		System.out.println(" popping open builder at <"+s+">");
		PopupMarkingArea menu = builder.getMenu(GLComponentWindow.getCurrentWindow(component).getCanvas(), s);
	}

	protected void showStopMenu() {
		MarkingMenuBuilder builder = new MarkingMenuBuilder();
		builder.marking("Stop", "SH");
		builder.call(new iUpdateable() {
			public void update() {
				stopBeingActiveNow();
			}
		});
		builder.marking("Pause", "W");
		builder.call(new iUpdateable() {
			public void update() {
				pauseBeingActiveNow();
			}
		});
		GLComponentWindow.getCurrentWindow(component).transformDrawingToWindow(downAt);
		Point s = new Point((int) downAt.x, (int) downAt.y);
		s = Launcher.display.map(GLComponentWindow.getCurrentWindow(component).getCanvas(), null, s);
		
		PopupMarkingArea menu = builder.getMenu(GLComponentWindow.getCurrentWindow(component).getCanvas(), s);

	}

	protected void showUnPauseMenu() {
		MarkingMenuBuilder builder = new MarkingMenuBuilder();
		builder.marking("Stop", "SH");
		builder.call(new iUpdateable() {
			public void update() {
				stopBeingActiveNow();
			}
		});
		builder.marking("Go", "E");
		builder.call(new iUpdateable() {
			public void update() {
				unpauseBeingActiveNow();
			}
		});
		GLComponentWindow.getCurrentWindow(component).transformDrawingToWindow(downAt);
		Point s = new Point((int) downAt.x, (int) downAt.y);
		s = Launcher.display.map(GLComponentWindow.getCurrentWindow(component).getCanvas(), null, s);

		PopupMarkingArea menu = builder.getMenu(GLComponentWindow.getCurrentWindow(component).getCanvas(), s);

	}

	abstract public boolean isExecuting();

	abstract public boolean isPaused();

	abstract protected void stopBeingActiveNow();

	abstract protected void pauseBeingActiveNow();
	abstract protected void unpauseBeingActiveNow();

	abstract protected void continueToBeActiveAfterUp();

	public boolean drag(Event arg0) {
		return false;
	}

	public boolean up(Event arg0) {
		return false;
	}

	public void paintNow() {

	}

}
