package field.core.windowing.components;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.swing.JPopupMenu;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;

import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElementOverrides.Ref;
import field.core.ui.SmallMenu;
import field.core.ui.SmallMenu.BetterPopup;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.components.DraggableComponent.Resize;
import field.launch.iUpdateable;
import field.math.linalg.iCoordinateFrame;
import field.namespace.context.CT.Dispatch;

public class PlainComponent implements iComponent {
	private int ox;

	private int oy;

	private boolean selected;

	private boolean justSelected;

	protected Rect bounds;

	protected iVisualElement visualElement;

	protected iVisualElementOverrides override;

	protected ComponentContainer inside;

	protected boolean isSelctedable = true;

	public PlainComponent() {
		this(new Rect(0, 0, 0, 0));
	}

	public PlainComponent(Rect bounds) {
		this.bounds = bounds;
	}

	public void beginMouseFocus(ComponentContainer inside) {
	}

	public void dispose() {
	}

	public void endMouseFocus(ComponentContainer inside) {
	}

	public Rect getBounds() {
		return bounds;
	}

	public iVisualElement getVisualElement() {
		return visualElement;
	}

	public void handleResize(Set<Resize> currentResize, float dx, float dy) {
	}

	public iComponent hit(Event event) {
		return this;
	}

	public float isHit(Event event) {
		if (event.button == 2)
			return Float.NEGATIVE_INFINITY;

		if (event.x > bounds.x && event.y > bounds.y
				&& event.x < (bounds.x + bounds.w)
				&& event.y < (bounds.y + bounds.h)) {
			Ref<Boolean> is = new Ref<Boolean>(false);
			override.isHit(this.visualElement, event, is);
			return is.get() ? 1 - bounds.size() : Float.NEGATIVE_INFINITY;
		} else {
			Ref<Boolean> is = new Ref<Boolean>(false);
			override.isHit(this.visualElement, event, is);
			return is.get() ? 1 - bounds.size() : Float.NEGATIVE_INFINITY;
		}
	}

	public boolean isSelected() {
		return selected;
	}

	public void keyPressed(ComponentContainer inside, Event arg0) {
	}

	public void keyReleased(ComponentContainer inside, Event arg0) {
	}

	public void keyTyped(ComponentContainer inside, Event arg0) {
	}

	public void mouseClicked(ComponentContainer inside, Event arg0) {
	}

	public void mouseDragged(ComponentContainer inside, Event arg0) {
	}

	public void mouseEntered(ComponentContainer inside, Event arg0) {
	}

	public void mouseExited(ComponentContainer inside, Event arg0) {
	}

	public void mouseMoved(ComponentContainer inside, Event arg0) {
	}

	public void mousePressed(ComponentContainer inside, Event arg0) {
		this.inside = inside;
		arg0.doit = false;
		if (Platform.isPopupTrigger(arg0)) {

			// assemble
			// and
			// present
			// menu

			LinkedHashMap<String, iUpdateable> items = new LinkedHashMap<String, iUpdateable>();
			override.menuItemsFor(this.visualElement, items);


			Ref<GLComponentWindow> ref = new Ref<GLComponentWindow>(null);
			this.override.getProperty(visualElement,
					iVisualElement.enclosingFrame, ref);
			if (ref.get() != null) {
				BetterPopup menu = new SmallMenu().createMenu(items, ref.get().getCanvas().getShell(), null);
				// menu.show(ref.get().getCanvas(), (int)
				// ref.get().getCurrentMouseInWindowCoordinates().x, (int)
				// ref.get().getCurrentMouseInWindowCoordinates().y);
				GLComponentWindow.getCurrentWindow(this).untransformMouseEvent(
						arg0);

				menu.show(new Point(arg0.x, arg0.y));
				// menu.show(ref.get().getCanvas(), arg0.x,arg0.y);

				GLComponentWindow.getCurrentWindow(this).transformMouseEvent(
						arg0);
			} else {
			}
		} else {

			if ((arg0.stateMask & SWT.ALT) != 0 && arg0.button == 1) {
				override.beginExecution(getVisualElement());
			} else {

				ox = arg0.x;
				oy = arg0.y;

				if (!selected && isSelctedable) {
					if ((arg0.stateMask & SWT.SHIFT) == 0)
						for (SelectionGroup<iComponent> d : getSelectionGroups())
							d.deselectAll();
					for (SelectionGroup<iComponent> d : getSelectionGroups())
						d.addToSelection(this);
					this.setSelected(true);
					justSelected = true;
				} else
					justSelected = false;
			}
		}
	}

	public void mouseReleased(ComponentContainer inside, Event arg0) {
		arg0.doit = false;
		if ((arg0.stateMask & SWT.ALT) != 0) {
			override.endExecution(getVisualElement());
		} else {

			this.inside = inside;
			if (arg0.x == ox && arg0.y == oy) {
				if (selected) {
					if ((arg0.stateMask & SWT.SHIFT) != 0) {
						if (!justSelected && isSelctedable) {
							for (SelectionGroup<iComponent> d : getSelectionGroups())
								d.removeFromSelection(this);
							this.setSelected(false);
						}
					} else {
						if (isSelctedable) {
							for (SelectionGroup<iComponent> d : getSelectionGroups())
								d.deselectAll();
							for (SelectionGroup<iComponent> d : getSelectionGroups())
								d.addToSelection(this);
							this.setSelected(true);
						}
					}
				}
			}
		}
	}

	public void paint(ComponentContainer inside, iCoordinateFrame frameSoFar,
			boolean visible) {
		override.paintNow(visualElement, bounds, visible);
	}

	public void setBounds(Rect r) {
		this.bounds = r;
	}

	public void setDirty() {
		if (inside != null)
			inside.requestRedisplay();
	}

	public void setSelected(boolean selected) {
		boolean changed = this.selected != selected;
		this.selected = selected;
		// if (changed)
		{
			if (inside != null)
				inside.requestRedisplay();
		}
	}

	public iComponent setVisualElement(iVisualElement ve) {
		visualElement = ve;
		override = new Dispatch<iVisualElement, iVisualElementOverrides>(
				iVisualElementOverrides.topology).getOverrideProxyFor(ve,
				iVisualElementOverrides.class);
		return this;
	}

	protected List<SelectionGroup<iComponent>> getSelectionGroups() {
		ArrayList<SelectionGroup<iComponent>> sel = new ArrayList<SelectionGroup<iComponent>>();
		Ref<SelectionGroup<iComponent>> out = new Ref<SelectionGroup<iComponent>>(
				null);
		override.getProperty(visualElement, iVisualElement.selectionGroup, out);
		if (out.get() != null)
			sel.add(out.get());
		return sel;
	}
}
