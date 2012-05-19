/**
 * 
 */
package field.core.windowing.components;

import java.util.Set;

public interface SelectionGroup<T> {
	public void deselectAll();

	public void addToSelection(T draggable);

	public void removeFromSelection(T draggable);

	public Set<T> getSelection();

	public void register(T draggable);
	
	public interface iSelectionChanged<C>
	{
		public void selectionChanged(Set<C> selected);
	}
	
	public void registerNotification(iSelectionChanged<T> selection);
}