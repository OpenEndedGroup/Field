package field.core.windowing.components;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import field.core.DragDuplicator;
import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.components.DraggableComponent.Resize;
import field.core.windowing.components.RootComponent.iMousePeer;
import field.math.linalg.iCoordinateFrame;

public class GlassComponent implements iComponent {

	private final RootComponent root;

	Set<iMousePeer> mousePeers = new LinkedHashSet<iMousePeer>();
	Set<iMousePeer> transparentMousePeers = new LinkedHashSet<iMousePeer>();

	
	DragDuplicator duplicator;

	public GlassComponent(RootComponent root, DragDuplicator duplicator)
	{
		this.root = root;
		this.duplicator = duplicator;
	}

	public void addMousePeer(iMousePeer mousePeer) {
		this.mousePeers.add(mousePeer);
	}
	public void addTransparentMousePeer(iMousePeer mousePeer) {
		this.transparentMousePeers.add(mousePeer);
	}

	public void beginMouseFocus(ComponentContainer inside) {
	}

	public void endMouseFocus(ComponentContainer inside) {
	}

	public Rect getBounds() {
		return root.getBounds();
	}

	public iVisualElement getVisualElement() {
		return null;
	}

	public void handleResize(Set<Resize> currentResize, float dx, float dy) {
	}

	public iComponent hit(Event event) {
		return this;
	}

	public float isHit(Event event) {
		return mousePeers.size()>0 ? Float.POSITIVE_INFINITY :  Float.NEGATIVE_INFINITY;
	}

	public boolean isSelected() {
		return false;
	}

	public void keyPressed(ComponentContainer inside, Event arg0) {
		if (arg0==null)
			return;
		if (!arg0.doit) return;
		for (iMousePeer m : new LinkedHashSet<iMousePeer>(mousePeers)) {
			m.keyPressed(inside, arg0);
			if (!arg0.doit) return;
		}
		for (iMousePeer m : transparentMousePeers) {
			m.keyPressed(inside, arg0);
			if (!arg0.doit) return;
		}
	}

	public void keyReleased(ComponentContainer inside, Event arg0) {
		if (!arg0.doit) return;
		for (iMousePeer m : new LinkedHashSet<iMousePeer>(mousePeers)) {
			m.keyReleased(inside, arg0);
			if (!arg0.doit) return;
		}
		for (iMousePeer m : transparentMousePeers) {
			m.keyReleased(inside, arg0);
			if (!arg0.doit) return;
		}
	}

	public void keyTyped(ComponentContainer inside, Event arg0) {
		if (!arg0.doit) return;
		for (iMousePeer m : new LinkedHashSet<iMousePeer>(mousePeers)) {
			m.keyTyped(inside, arg0);
			if (!arg0.doit) return;
		}
		for (iMousePeer m : transparentMousePeers) {
			m.keyTyped(inside, arg0);
			if (!arg0.doit) return;
		}
	}

	public void mouseClicked(ComponentContainer inside, Event arg0) {
		if (!arg0.doit) return;
		for (iMousePeer m : new LinkedHashSet<iMousePeer>(mousePeers)) {
			m.mouseClicked(inside, arg0);
			if (!arg0.doit) return;
		}
		for (iMousePeer m : transparentMousePeers) {
			m.mouseClicked(inside, arg0);
			if (!arg0.doit) return;
		}

	}

	public void mouseDragged(ComponentContainer inside, Event arg0) {
		if (!arg0.doit) return;

		duplicator.drag(arg0);

		
		for (iMousePeer m : new LinkedHashSet<iMousePeer>(mousePeers)) {
			m.mouseDragged(inside, arg0);
			if (!arg0.doit) return;
		}
		for (iMousePeer m : transparentMousePeers) {
			m.mouseDragged(inside, arg0);
			if (!arg0.doit) return;
		}
	}

	public void mouseEntered(ComponentContainer inside, Event arg0) {
	}

	public void mouseExited(ComponentContainer inside, Event arg0) {
	}

	public void mouseMoved(ComponentContainer inside, Event arg0) {
		if (!arg0.doit) return;
		for (iMousePeer m : mousePeers) {
			m.mouseMoved(inside, arg0);
			if (!arg0.doit) return;
		}
		for (iMousePeer m : transparentMousePeers) {
			m.mouseMoved(inside, arg0);
			if (!arg0.doit) return;
		}
	}

	public void mousePressed(ComponentContainer inside, Event arg0) {
		if (!arg0.doit) return;

		if (arg0.doit && (arg0.stateMask & Platform.getCommandModifier()) != 0
				&& (arg0.stateMask & SWT.SHIFT) != 0
				&& !Platform.isPopupTrigger(arg0)) {

			if (arg0.type == SWT.MouseDown) {
				duplicator.begin(arg0);
				arg0.doit = false;
			}
			
		} 

		for (iMousePeer m : mousePeers) {
			m.mousePressed(inside, arg0);
			if (!arg0.doit) return;
		}
		for (iMousePeer m : transparentMousePeers) {
			m.mousePressed(inside, arg0);
			if (!arg0.doit) return;
		}
	}

	public void mouseReleased(ComponentContainer inside, Event arg0) {
		if (!arg0.doit) return;
		
		
		duplicator.end(arg0);

		for (iMousePeer m : mousePeers) {
			m.mouseReleased(inside, arg0);
			if (!arg0.doit) return;
		}
		for (iMousePeer m : transparentMousePeers) {
			m.mouseReleased(inside, arg0);
			if (!arg0.doit) return;
		}
	}

	public void paint(ComponentContainer inside, iCoordinateFrame frameSoFar, boolean visible) {
		inside.addAsAllEventHandler(this);
	}

	public void removeMousePeer(iMousePeer mousePeer) {
		this.mousePeers.remove(mousePeer);
		this.transparentMousePeers.remove(mousePeer);
	}

	public void setBounds(Rect r) {
	}

	public void setSelected(boolean selected) {
	}

	public iComponent setVisualElement(iVisualElement ve) {
		return this;
	}



}
