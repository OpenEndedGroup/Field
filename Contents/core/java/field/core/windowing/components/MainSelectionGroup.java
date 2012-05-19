/**
 *
 */
package field.core.windowing.components;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import field.launch.Launcher;
import field.launch.iUpdateable;

public class MainSelectionGroup implements SelectionGroup<iComponent> {

	boolean lastSelectionWasPopped = false;

	Set<iComponent> registered = new HashSet<iComponent>();

	ArrayList<Set<iComponent>> selectionStack = new ArrayList<Set<iComponent>>();
	ArrayList<Set<iComponent>> forwardStack = new ArrayList<Set<iComponent>>();

	Set<iComponent> selected = new HashSet<iComponent>();

	List<iSelectionChanged<iComponent>> notes = new ArrayList<iSelectionChanged<iComponent>>();

	public MainSelectionGroup() {
		Launcher.getLauncher().registerUpdateable(new iUpdateable() {

			Set<iComponent> lastSelection = new HashSet<iComponent>();

			public void update() {
				
				if (!lastSelection.equals(selected)) {
					if (!lastSelectionWasPopped) {
						pushSelection(lastSelection);
						forwardStack.clear();
					} else {
					}
					lastSelectionWasPopped = false;

					lastSelection.clear();
					lastSelection.addAll(selected);
					for (iSelectionChanged<iComponent> c : notes)
						c.selectionChanged(lastSelection);

				}
			}

		});
	}

	public void addToSelection(iComponent draggable) {
		if (draggable.getVisualElement() != null)
			selected.add(draggable);
	}

	public boolean canGoBack()
	{
		return selectionStack.size()>0;
	}

	public boolean canGoForward()
	{
		return forwardStack.size()>0;
	}

	public void deselectAll() {
		for (iComponent c : new ArrayList<iComponent>(selected))
			if (c instanceof iDraggableComponent)
				((iDraggableComponent) c).setSelected(false);
		selected.clear();
	}

	public Set<iComponent> getSelection() {
		return selected;
	}
	public void moveForwardSelection() {
		if (forwardStack.size() > 0) {
			pushSelection(new HashSet<iComponent>(selected));

			Set<iComponent> a = forwardStack.remove(forwardStack.size() - 1);
			if (a != null) {
				deselectAll();
				for (iComponent c : a) {
					addToSelection(c);
					c.setSelected(true);
				}
			}
			lastSelectionWasPopped = true;
		}
	}

	public void popSelection() {
		if (selectionStack.size() > 0) {
			forwardStack.add(new HashSet<iComponent>(selected));
			Set<iComponent> a = selectionStack.remove(selectionStack.size() - 1);
			if (a != null) {
				deselectAll();
				for (iComponent c : a) {
					addToSelection(c);
					c.setSelected(true);
				}
			}
			lastSelectionWasPopped = true;
		}
	}

	public void pushSelection(Set<iComponent> ls) {
		selectionStack.add(new HashSet<iComponent>(ls));
		if (selectionStack.size() > 10)
			selectionStack.remove(0);
	}

	public void register(iComponent draggable) {
		registered.add(draggable);
	}

	public void registerNotification(iSelectionChanged<iComponent> selection) {
		notes.add(selection);
	}

	public void removeFromSelection(iComponent draggable) {		
		selected.remove(draggable);
	}
}