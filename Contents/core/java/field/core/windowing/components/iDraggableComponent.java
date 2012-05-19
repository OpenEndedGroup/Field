package field.core.windowing.components;

import org.eclipse.swt.widgets.Event;

import field.core.dispatch.iVisualElement.Rect;


public interface iDraggableComponent {

	public abstract void setSelected(boolean selected);

	public abstract boolean isSelected();

	public abstract float isHit(Event event);

	public abstract iComponent hit(Event event);

	public abstract Rect getBounds();

	public abstract void setBounds(Rect r);

	public abstract void setHidden(boolean hidden);

	public abstract void setDirty();

}