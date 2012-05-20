package field.core.execution;

import java.awt.Point;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;

import field.launch.SystemProperties;
import field.util.MiscNative;

public class ExecutionMonitor implements Runnable {

	static public JEditorPane pane;

	private Thread tt;

	boolean stillborn = !(SystemProperties.getIntProperty("executionMonitor", 0)==1);
	
	public ExecutionMonitor() {
		if (stillborn) return;
		
		tt = new Thread(this);

		tt.start();
	}

	Object lock;

	boolean inside;
	boolean outside;

	long at = System.currentTimeMillis();

	long lastSpeculativeDown = 0;
	
	public void run() {

		while (true) {

			while (System.currentTimeMillis() - at < 500) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}

			at = System.currentTimeMillis();

			if (inside && !outside) {
				;//System.out.println(" down");
				new MiscNative().noteDown_safe();
				lastSpeculativeDown = System.currentTimeMillis();
				inside = false;
			} else if (!inside && outside) {

				if (pane != null) {
					Point c = new Point(pane.getWidth() / 2, pane.getHeight() / 2);
					SwingUtilities.convertPointToScreen(c, pane);

					c.x -= 150;
					c.y -= 25;
					new MiscNative().noteUp_safe("!\u2014", c.x, c.y, SwingUtilities.getWindowAncestor(pane));
				}
				inside = true;
			} else {
				if (inside) {
				}
			}
			
//			if (!outside)
//			{
//				if (System.currentTimeMillis()-lastSpeculativeDown>2000)
//				{
//					new MiscNative().noteDown_safe();
//					lastSpeculativeDown = System.currentTimeMillis(); 
//				}
//			}
		}
	}

	public void enter() {
		outside = true;
		at = System.currentTimeMillis();
	}

	public void exit() {
		outside = false;
		at = System.currentTimeMillis() - 1000;
		if (tt!=null)
			tt.interrupt();
	}

}
