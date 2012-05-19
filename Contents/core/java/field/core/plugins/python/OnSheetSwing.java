package field.core.plugins.python;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.components.PlainDraggableComponent;
import field.core.windowing.components.SwingBridgeComponent;
import field.core.windowing.components.iComponent;
import field.math.linalg.Vector2;
import field.math.linalg.iCoordinateFrame;
import field.util.MiscNative;

// unlike output inserts on sheet, this lets you just write swing code directly to the canvas
// of course, it's next to impossible to actually persist this, so we don't try

public class OnSheetSwing {

	@Woven
	static public class Wrap extends SwingBridgeComponent {
		public JPanel panel = new JPanel();

		boolean heavy = false;

		@Override
		public PlainDraggableComponent setVisualElement(iVisualElement element) {

			this.componentToRender = panel;

			panel.setBounds(new Rectangle(0, 0, 50, 50));
			panel.setBackground(new Color(0.8f, 0.3f, 0.2f, 0.5f));
			panel.setOpaque(true);
			hookupNotifications();

			return super.setVisualElement(element);
		}

		@Override
		protected int getMaxDimension() {
			return 1024;

		}

		public void setComponent(JComponent component, boolean heavy) {
			if (component instanceof JScrollPane) {
				JFrame f = new JFrame();
				f.add(component);
				f.setLocation(-6000, -6000);
				f.setSize((int) this.getBounds().w, (int) this.getBounds().h + 20);
				f.setVisible(true);
				f.setVisible(false);
			}

			this.componentToRender = component;
			component.setSize((int) this.getBounds().w, (int) this.getBounds().h);
			component.setPreferredSize(new Dimension((int) this.getBounds().w, (int) this.getBounds().h));
			this.heavy = heavy;
			this.hookupNotifications();
			this.setDirty();

		}

		@Override
		protected boolean shouldSendUpwards(Event arg0) {
			if (arg0.count == 2 && heavy) {
				openEditingWindow();
				return true;
			} else {
				if (heavy)
					return true;
				return (arg0.stateMask & SWT.SHIFT) != 0;
			}
		}

		int disabled = 0;

		@Override
		public void paint(ComponentContainer inside, iCoordinateFrame frameSoFar, boolean visible) {
			super.paint(inside, frameSoFar, visible);
			while (disabled > 0) {
				new MiscNative().enableScreenUpdates();
				disabled--;
			}
		}

		JFrame editingFrame = null;

		@NextUpdate(delay = 2)
		public void openEditingWindow() {
			if (editingFrame == null) {
				editingFrame = new JFrame();
				// FloatingPalettes.registerFloatingPalette(editingFrame);

				// we need to
				// find out
				// where this
				// rect is on
				// the screen,
				// this is
				// unlikely to
				// work in a
				// rescaled view
				// (we'll settle
				// for getting
				// the top left
				// corner
				// correct)
				GLComponentWindow frame = iVisualElement.enclosingFrame.get(getVisualElement());
				Rect bounds = this.getBounds();
				Vector2 topLeft = new Vector2(bounds.x, bounds.y);
				frame.transformDrawingToWindow(topLeft);
				Point p = new Point(frame.getFrame().getBounds().x, frame.getFrame().getBounds().y);

				topLeft.x += p.x;
				topLeft.y += p.y;


				System.out.println(" canvas bounds are :"+frame.getCanvas().getBounds());
				
				topLeft.x += frame.getCanvas().getBounds().x;
				topLeft.y += frame.getCanvas().getBounds().y;
				

				editingFrame.setAlwaysOnTop(true);
				editingFrame.setUndecorated(true);
				int ww = componentToRender.getWidth();
				int hh = componentToRender.getHeight();
				editingFrame.setBounds((int) topLeft.x, (int) topLeft.y, ww, hh);
				editingFrame.getContentPane().add(componentToRender);
				componentToRender.setBounds(0, 0, ww, hh);
				editingFrame.setVisible(true);

			}
		}

		public void closeEditingWindow() {
			if (editingFrame != null) {
				editingFrame.getContentPane().remove(componentToRender);
				deferredClosing(editingFrame);
				setDirty();
				editingFrame = null;
			}
		}

		@NextUpdate(delay = 2)
		public void deferredClosing(JFrame editingFrame) {
			editingFrame.setVisible(false);
			editingFrame.dispose();
		}
	}

	static public void upgrade(iVisualElement e) {

		iComponent c = iVisualElement.localView.get(e);
		System.out.println(" upgrading a <" + e + "> from <" + c + ">");
		if (c instanceof Wrap)
			return;
		if (c == null)
			return;

		System.out.println(" something doing");

		iVisualElement.enclosingFrame.get(e).getRoot().removeComponent(c);

		Wrap w = new Wrap();
		w.setVisualElement(e);
		iVisualElement.localView.set(e, e, w);

		iVisualElement.enclosingFrame.get(e).getRoot().addComponent(w);
	}

}
