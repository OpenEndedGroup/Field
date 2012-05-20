package field.core.ui.text.embedded;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.image.ImageObserver;

import javax.swing.JComponent;

import org.eclipse.swt.graphics.Image;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.Yield;
import field.bytecode.protect.yield.YieldUtilities;
import field.core.ui.text.embedded.CustomInsertDrawing.iAcceptsInsertRenderingContext;
import field.core.ui.text.embedded.CustomInsertDrawing.iInsertRenderingContext;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.abstraction.iAcceptor;

public class MinimalExpandable extends JComponent implements iAcceptsInsertRenderingContext {

	
	private static final int maxHeight = 130;
	public float maxCollapse = 2f;
	public float collapseness = maxCollapse;
	public boolean isCollapsing = true;
	public iUpdateable animationThread;
	protected int height = 14;
	protected boolean shouldColapseOnOff = false;
	protected float alignment = 0.9f;
	boolean hover = false;

	protected void colapse() {
		if (animationThread == null) {
			newAnimationThread();
		}
		isCollapsing = true;

	}

	protected void expand() {
		if (animationThread == null) {
			;//System.out.println(" needs to expand ");
			newAnimationThread();
		}
		isCollapsing = false;
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		Rectangle bounds = getBounds();

	}

	@Override
	protected void processMouseEvent(MouseEvent e) {

		;//System.out.println(" hello are we compiling ? :" + this.getClass());

		super.processMouseEvent(e);
		int x = e.getX() - 0 * this.bounds().x;
		int y = (e.getY() - 0 * this.bounds().y);

		if (e.getClickCount() == 2 && e.getID() == e.MOUSE_PRESSED) {
			if (isCollapsing && this.getBounds().height != maxHeight)
				expand();
			else
				colapse();
			return;
		}

		if (e.getID() == e.MOUSE_PRESSED) {

			;//System.out.println(" mouse pressed <" + x + "> <" + this.getBounds().width + "> <" + y + ">");

			if (x > this.getBounds().width - 12 && y < 12 /*
								 * ||
								 * e.getButton()
								 * ==
								 * MouseEvent.
								 * BUTTON2
								 */) {
				if (isCollapsing && this.getBounds().height != maxHeight)
					expand();
				else
					colapse();
			}
		}
		if (e.getID() == e.MOUSE_EXITED) {
			if (hover)
				repaint();
			hover = false;
		}
		if (e.getID() == e.MOUSE_MOVED) {
			if (x > this.getBounds().width - 12 && y < 12) {
				if (!hover)
					repaint();
				hover = true;

			} else {
				if (hover)
					repaint();
				hover = false;
			}
		}

	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		collapseness = maxCollapse * (height - maxHeight) / (12 - maxHeight);
	}

	boolean insideAnimationThread = false;
	protected iInsertRenderingContext irc;

	void newAnimationThread() {
		Launcher.getLauncher().registerUpdateable(animationThread = new iUpdateable() {
			public void update() {
				if (isCollapsing && collapseness >= maxCollapse) {
					Launcher.getLauncher().deregisterUpdateable(this);
					animationThread = null;
				}
				if (!isCollapsing && collapseness <= 0) {
					Launcher.getLauncher().deregisterUpdateable(this);
					animationThread = null;
				}

				if (isCollapsing)
					collapseness++;
				else
					collapseness--;

				if (collapseness > maxCollapse)
					collapseness = maxCollapse;
				if (collapseness < 0)
					collapseness = 0;

				;//System.out.println(" collapseness :" + collapseness);

				float alpha = collapseness / maxCollapse;
				height = (int) (12 * alpha + maxHeight * (1 - alpha));
				alignment = (float) (0.9 * alpha + 0.5 * (1 - alpha));

				;//System.out.println(" set size <" + getSize().width + " " + height + ">");
				insideAnimationThread = true;
				try {
					setSize(getSize().width, height);
					updateSize(getSize().width, height);
				} finally {
					insideAnimationThread = false;
				}
			}
		});
	}

	iUpdateable interpolationThread;

	void newInterpolationThread(final int frames, final iAcceptor<Integer> call) {
		interpolationThread = new iUpdateable() {

			int f = frames;
			iAcceptor<Integer> callback = call;

			@Woven
			@Yield
			public void update() {
				for (int i = 0; i < f; i++) {
					callback = callback.set(i);
					setSize(getSize());
					YieldUtilities.yield(null);
				}
				Launcher.getLauncher().deregisterUpdateable(this);
				interpolationThread = null;
			}
		};
		Launcher.getLauncher().registerUpdateable(interpolationThread);
	}

	protected void updateSize(int w, int h) {
	}

	@Override
	public void setInsertRenderingContext(iInsertRenderingContext context) {
		irc = context;
	}

}
