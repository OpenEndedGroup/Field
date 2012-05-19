package field.core.plugins.selection;

import java.util.HashSet;
import java.util.Set;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElementOverrides;
import field.core.plugins.drawing.SplineComputingOverride;
import field.core.plugins.selection.SelectionSetDriver.iSelectionPredicate;
import field.core.windowing.components.DraggableComponent;
import field.core.windowing.components.PlainDraggableComponent;
import field.core.windowing.components.iComponent;

public class ComputedSelectionSets {

	static public class ByClass implements iSelectionPredicate {

		private final Set<Class<?>> c;

		public ByClass(Class c) {
			this.c = new HashSet<Class<?>>();
			this.c.add(c);
		}

		public ByClass(Set<Class<?>> c2) {
			this.c = c2;
		}

		public void begin(Set<iVisualElement> everything, Set<iVisualElement> currentlySelected, Set<iVisualElement> previousCache) {
		}

		public boolean is(iVisualElement e) {
			iComponent component = iVisualElement.localView.get(e);
			iVisualElementOverrides overrides = iVisualElement.overrides.get(e);
			for (Class c : this.c) {
				if (c.isInstance(overrides))
					return true;
			}
			return false;
		}
	}

	static public class ByComponentClass implements iSelectionPredicate {

		private final Set<Class<?>> c;

		public ByComponentClass(Class c) {
			this.c = new HashSet<Class<?>>();
			this.c.add(c);
		}

		public ByComponentClass(Set<Class<?>> c2) {
			this.c = c2;
		}

		public void begin(Set<iVisualElement> everything, Set<iVisualElement> currentlySelected, Set<iVisualElement> previousCache) {
		}

		public boolean is(iVisualElement e) {
			iComponent component = iVisualElement.localView.get(e);
			for (Class c : this.c) {
				if (c.isInstance(component))
					return true;
			}
			return false;
		}
	}

	static public class ByClass_computedSpline implements iSelectionPredicate {
		public void begin(Set<iVisualElement> everything, Set<iVisualElement> currentlySelected, Set<iVisualElement> previousCache) {
		}

		public boolean is(iVisualElement e) {
			iComponent component = iVisualElement.localView.get(e);
			iVisualElementOverrides overrides = iVisualElement.overrides.get(e);
			return component instanceof PlainDraggableComponent && overrides instanceof SplineComputingOverride;
		}
	}

	static public class ByClass_plainPython implements iSelectionPredicate {
		public void begin(Set<iVisualElement> everything, Set<iVisualElement> currentlySelected, Set<iVisualElement> previousCache) {
		}

		public boolean is(iVisualElement e) {
			iComponent component = iVisualElement.localView.get(e);
			iVisualElementOverrides overrides = iVisualElement.overrides.get(e);
			return component instanceof DraggableComponent && overrides instanceof iVisualElementOverrides.DefaultOverride;
		}
	}

	

	static public class CurrentlySelected implements iSelectionPredicate {
		private Set<iVisualElement> currentlySelected;

		public void begin(Set<iVisualElement> everything, Set<iVisualElement> currentlySelected, Set<iVisualElement> previousCache) {
			this.currentlySelected = currentlySelected;
		}

		public boolean is(iVisualElement e) {
			return currentlySelected.contains(e);
		}
	}

	static public class Saved implements iSelectionPredicate {
		private final Set<String> saved;

		public Saved(Set<iVisualElement> saved) {
			super();
			this.saved = new HashSet<String>();
			for (iVisualElement v : saved) {
				this.saved.add(v.getUniqueID());
			}
		}

		public void begin(Set<iVisualElement> everything, Set<iVisualElement> currentlySelected, Set<iVisualElement> previousCache) {
		}

		public boolean is(iVisualElement e) {
			return saved.contains(e.getUniqueID());
		}
	}

}
