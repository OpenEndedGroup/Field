package field.core.windowing;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Sash;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.launch.Launcher;
import field.launch.SystemProperties;

@Woven
public class BetterSash {

	static public final boolean lockSashes = SystemProperties.getIntProperty("lockSashes", 0)==1;
	
	private Sash sash;

	boolean hidden = false;

	private SashForm f;

	private int[] were;

	private final boolean closesRight;

	public BetterSash(final SashForm f, boolean closesRight) {
		this.f = f;
		this.closesRight = closesRight;

		for (int i = 0; i < f.getChildren().length; i++) {

			if (!(f.getChildren()[i] instanceof Sash))
				continue;

			sash = (Sash) f.getChildren()[i];
			if (lockSashes) sash.setVisible(false);
			sash.addControlListener(new ControlListener() {

				@Override
				public void controlMoved(ControlEvent e) {
					System.out.println(" sash moved :" + e);
				}

				@Override
				public void controlResized(ControlEvent e) {
					System.out.println(" sash resized:" + e);

				}
			});

			sash.setToolTipText("Double-click to hide, drag to move");

			sash.addMouseListener(new MouseListener() {

				@Override
				public void mouseUp(MouseEvent e) {
				}

				@Override
				public void mouseDown(MouseEvent e) {
					if (hidden) {
						unhide();
					}
				}

				@Override
				public void mouseDoubleClick(MouseEvent e) {
					if (!hidden) {
						hide();
					}
				}
			});

			sash.addPaintListener(new PaintListener() {

				@Override
				public void paintControl(PaintEvent e) {

					e.gc.setClipping((Rectangle) null);
					e.gc.setAdvanced(true);

					Control[] c = f.getChildren();

					e.gc.setBackground(new Color(Launcher.display, 0,0,0));
					e.gc.setForeground(new Color(Launcher.display, 0,0,0));
					e.gc.setAlpha(128);
					// e.gc.fillRectangle(0, 0, e.width,
					// e.height);
					
					if (f.getOrientation() == SWT.HORIZONTAL) {
//						Path p = new Path(Launcher.display);
//						p.moveTo(0,0);
//						p.lineTo((e.width-1)/2, (e.width-1)/2);
//						p.lineTo((e.width-1), 0);
//						p.lineTo(0, 0);
//						e.gc.drawPath(p);
//						p = new Path(Launcher.display);
//						p.moveTo(0,e.height-1);
//						p.lineTo((e.width-1)/2, e.height-1-(e.width-1)/2);
//						p.lineTo((e.width-1), e.height-1);
//						p.lineTo(0, e.height-1);
//						e.gc.drawPath(p);
						
//						e.gc.setAlpha(30);
//						e.gc.setLineDash(new int[]{4,8});
//						e.gc.drawLine( (e.width-1)/2, (e.width-1)/2, (e.width-1)/2, e.height-1-(e.width-1)/2);
//						e.gc.setAlpha(128);

					}
					else
					{
//						Path p = new Path(Launcher.display);
//						p.moveTo(0,0);
//						p.lineTo((e.height-1)/2, (e.height-1)/2);
//						p.lineTo(0, e.height-1);
//						p.lineTo(0, 0);
//						e.gc.drawPath(p);
//						p = new Path(Launcher.display);
//						p.moveTo(e.width-1,0);
//						p.lineTo(e.width-1-(e.height-1)/2, (e.height-1)/2);
//						p.lineTo(e.width-1, e.height-1);
//						p.lineTo(e.width-1, 0);
//						e.gc.drawPath(p);
//
//						e.gc.setAlpha(30);
//						e.gc.setLineDash(new int[]{4,8});
//						e.gc.drawLine ( (e.height-1)/2, (e.height-1)/2, e.width-1-(e.height-1)/2, (e.height-1)/2);
//						e.gc.setAlpha(128);
					}
					int r = 2;
					if (f.getOrientation() == SWT.HORIZONTAL) {
						e.gc.setAlpha(5);
						e.gc.setBackgroundPattern(new Pattern(Launcher.display, 0, 0, e.width/2, 0, new Color(Launcher.display, 255, 255, 255), 0, new Color(Launcher.display, 255, 255, 255), 20));
						e.gc.fillRoundRectangle(-e.width/2,0, e.width, e.height-1, r, r);
						e.gc.setBackgroundPattern(new Pattern(Launcher.display, e.width, 0, e.width/2, 0, new Color(Launcher.display, 255, 255, 255), 0, new Color(Launcher.display, 255, 255, 255), 20));
						e.gc.fillRoundRectangle(e.width/2,0, e.width, e.height-1, r, r);
//						e.gc.drawRoundRectangle(e.width/2,0, e.width, e.height-1, e.width, e.width);
//						e.gc.drawRoundRectangle(-e.width/2,0, e.width, e.height-1, e.width, e.width);
					} else {
						e.gc.setAlpha(5);
						e.gc.setBackgroundPattern(new Pattern(Launcher.display, 0, 0, 0, e.height/2, new Color(Launcher.display, 255, 255, 255), 0, new Color(Launcher.display, 255, 255, 255), 20));
						e.gc.fillRoundRectangle(0, -e.height/2, e.width-1, e.height, r,r);
						e.gc.setBackgroundPattern(new Pattern(Launcher.display, 0, e.height, 0, e.height/2, new Color(Launcher.display, 255, 255, 255), 0, new Color(Launcher.display, 255, 255, 255), 20));
						e.gc.fillRoundRectangle(0, e.height/2, e.width-1, e.height, r,r);
//						e.gc.drawRoundRectangle(0, e.height/2, e.width-1, e.height, e.height, e.height);
//						e.gc.drawRoundRectangle(0, -e.height/2, e.width-1, e.height, e.height, e.height);
					}
				}
			});
		}

	}

	@NextUpdate
	protected void hide() {

		were = f.getWeights();

		if (closesRight)
			f.setWeights(new int[] { 1, 0 });
		else
			f.setWeights(new int[] { 0, 1 });

		hidden = true;
	}

	@NextUpdate
	protected void unhide() {
		f.setWeights(were);
		hidden = false;
	}

	public void toggle() {
		if (hidden)
			unhide();
		else
			hide();
	}
}
