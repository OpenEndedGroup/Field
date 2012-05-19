package field.core.dispatch;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Event;

import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.launch.iUpdateable;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.util.Dict.Prop;

/**
 * we're 'de-abstracting' our Dispatch<x,y> code for the purposes of speed. For
 * many things, this has become part of our inner loops.
 *
 * @author marc
 *
 */
public class FastVisualElementOverridesDispatch implements iVisualElementOverrides {

	private final boolean backwards;

	public FastVisualElementOverridesDispatch(boolean backwards) {
		this.backwards = backwards;
	}

	public VisitCode added(iVisualElement newSource) {

		List<iVisualElement> fringe = new LinkedList<iVisualElement>();
		fringe.add(iVisualElementOverrides.topology.getAt());

		HashSet<iVisualElement> seen = new LinkedHashSet<iVisualElement>();

		while (fringe.size() > 0) {
			iVisualElement next = fringe.remove(0);
			if (!seen.contains(next)) {
				iVisualElementOverrides over = next.getProperty(iVisualElement.overrides);
				if (over != null) {
					VisitCode o = over.added(newSource);
					if (o.equals(VisitCode.skip)) {
					} else if (o.equals(VisitCode.stop)) {
						return VisitCode.stop;
					} else {
						if (backwards)
							fringe.addAll((Collection<? extends iVisualElement>) next.getParents());
						else
							fringe.addAll(next.getChildren());
					}
				}
				seen.add(next);
			}
		}
		return VisitCode.cont;
	}

	public VisitCode beginExecution(iVisualElement source) {
		List<iVisualElement> fringe = new LinkedList<iVisualElement>();
		fringe.add(iVisualElementOverrides.topology.getAt());

		HashSet<iVisualElement> seen = new LinkedHashSet<iVisualElement>();

		while (fringe.size() > 0) {
			iVisualElement next = fringe.remove(0);
			if (!seen.contains(next)) {
				iVisualElementOverrides over = next.getProperty(iVisualElement.overrides);
				if (over != null) {
					VisitCode o = over.beginExecution(source);
					if (o.equals(VisitCode.skip)) {
					} else if (o.equals(VisitCode.stop)) {
						return VisitCode.stop;
					} else {
						if (backwards)
							fringe.addAll((Collection<? extends iVisualElement>) next.getParents());
						else
							fringe.addAll(next.getChildren());
					}
				}
				seen.add(next);
			}
		}
		return VisitCode.cont;
	}

	public <T> VisitCode deleteProperty(iVisualElement source, VisualElementProperty<T> prop) {
		List<iVisualElement> fringe = new LinkedList<iVisualElement>();
		fringe.add(iVisualElementOverrides.topology.getAt());

		HashSet<iVisualElement> seen = new LinkedHashSet<iVisualElement>();

		while (fringe.size() > 0) {
			iVisualElement next = fringe.remove(0);
			if (!seen.contains(next)) {
				iVisualElementOverrides over = next.getProperty(iVisualElement.overrides);
				if (over != null) {
					VisitCode o = over.deleteProperty(source, prop);
					if (o.equals(VisitCode.skip)) {
					} else if (o.equals(VisitCode.stop)) {
						return VisitCode.stop;
					} else {
						if (backwards)
							fringe.addAll((Collection<? extends iVisualElement>) next.getParents());
						else
							fringe.addAll(next.getChildren());
					}
				}
				seen.add(next);
			}
		}
		return VisitCode.cont;
	}

	public VisitCode deleted(iVisualElement source) {
		List<iVisualElement> fringe = new LinkedList<iVisualElement>();
		fringe.add(iVisualElementOverrides.topology.getAt());

		HashSet<iVisualElement> seen = new LinkedHashSet<iVisualElement>();

		while (fringe.size() > 0) {
			iVisualElement next = fringe.remove(0);
			if (!seen.contains(next)) {
				iVisualElementOverrides over = next.getProperty(iVisualElement.overrides);
				if (over != null) {
					VisitCode o = over.deleted(source);
					if (o.equals(VisitCode.skip)) {
					} else if (o.equals(VisitCode.stop)) {
						return VisitCode.stop;
					} else {
						if (backwards)
							fringe.addAll((Collection<? extends iVisualElement>) next.getParents());
						else
							fringe.addAll(next.getChildren());
					}
				}
				seen.add(next);
			}
		}
		return VisitCode.cont;
	}

	public VisitCode endExecution(iVisualElement source) {
		List<iVisualElement> fringe = new LinkedList<iVisualElement>();
		fringe.add(iVisualElementOverrides.topology.getAt());

		HashSet<iVisualElement> seen = new LinkedHashSet<iVisualElement>();

		while (fringe.size() > 0) {
			iVisualElement next = fringe.remove(0);
			if (!seen.contains(next)) {
				iVisualElementOverrides over = next.getProperty(iVisualElement.overrides);
				if (over != null) {
					VisitCode o = over.endExecution(source);
					if (o.equals(VisitCode.skip)) {
					} else if (o.equals(VisitCode.stop)) {
						return VisitCode.stop;
					} else {
						if (backwards)
							fringe.addAll((Collection<? extends iVisualElement>) next.getParents());
						else
							fringe.addAll(next.getChildren());
					}
				}
				seen.add(next);
			}
		}
		return VisitCode.cont;
	}

	public <T> VisitCode getProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> ref) {

		List<iVisualElement> fringe = new LinkedList<iVisualElement>();
		fringe.add(iVisualElementOverrides.topology.getAt());

		HashSet<iVisualElement> seen = new LinkedHashSet<iVisualElement>();

		while (fringe.size() > 0) {
			iVisualElement next = fringe.remove(0);
			if (!seen.contains(next)) {
				iVisualElementOverrides over = next.getProperty(iVisualElement.overrides);
				if (over != null) {
					VisitCode o = over.getProperty(source, prop, ref);
					if (o.equals(VisitCode.skip)) {
					} else if (o.equals(VisitCode.stop)) {
						return VisitCode.stop;
					} else {
						if (backwards)
							fringe.addAll((Collection<? extends iVisualElement>) next.getParents());
						else
							fringe.addAll(next.getChildren());
					}
				}
				seen.add(next);
			}
		}
		return VisitCode.cont;
	}

	public VisitCode handleKeyboardEvent(iVisualElement newSource, Event event) {
		List<iVisualElement> fringe = new LinkedList<iVisualElement>();
		fringe.add(iVisualElementOverrides.topology.getAt());

		HashSet<iVisualElement> seen = new LinkedHashSet<iVisualElement>();

		while (fringe.size() > 0) {
			iVisualElement next = fringe.remove(0);
			if (!seen.contains(next)) {
				iVisualElementOverrides over = next.getProperty(iVisualElement.overrides);
				if (over != null) {
					VisitCode o = over.handleKeyboardEvent(newSource, event);
					if (o.equals(VisitCode.skip)) {
					} else if (o.equals(VisitCode.stop)) {
						return VisitCode.stop;
					} else {
						if (backwards)
							fringe.addAll((Collection<? extends iVisualElement>) next.getParents());
						else
							fringe.addAll(next.getChildren());
					}
				}
				seen.add(next);
			}
		}
		return VisitCode.cont;
	}

	public VisitCode inspectablePropertiesFor(iVisualElement source, List<Prop> properties) {
		List<iVisualElement> fringe = new LinkedList<iVisualElement>();
		fringe.add(iVisualElementOverrides.topology.getAt());

		HashSet<iVisualElement> seen = new LinkedHashSet<iVisualElement>();

		while (fringe.size() > 0) {
			iVisualElement next = fringe.remove(0);
			if (!seen.contains(next)) {
				iVisualElementOverrides over = next.getProperty(iVisualElement.overrides);
				if (over != null) {
					VisitCode o = over.inspectablePropertiesFor(source, properties);
					if (o.equals(VisitCode.skip)) {
					} else if (o.equals(VisitCode.stop)) {
						return VisitCode.stop;
					} else {
						if (backwards)
							fringe.addAll((Collection<? extends iVisualElement>) next.getParents());
						else
							fringe.addAll(next.getChildren());
					}
				}
				seen.add(next);
			}
		}
		return VisitCode.cont;
	}

	public VisitCode isHit(iVisualElement source, Event event, Ref<Boolean> is) {

		List<iVisualElement> fringe = new LinkedList<iVisualElement>();
		fringe.add(iVisualElementOverrides.topology.getAt());

		HashSet<iVisualElement> seen = new LinkedHashSet<iVisualElement>();

		while (fringe.size() > 0) {
			iVisualElement next = fringe.remove(0);
			if (!seen.contains(next)) {
				iVisualElementOverrides over = next.getProperty(iVisualElement.overrides);
				if (over != null) {
					VisitCode o = over.isHit(source, event, is);
					if (o.equals(VisitCode.skip)) {
					} else if (o.equals(VisitCode.stop)) {
						return VisitCode.stop;
					} else {
						if (backwards)
							fringe.addAll((Collection<? extends iVisualElement>) next.getParents());
						else
							fringe.addAll(next.getChildren());
					}
				}
				seen.add(next);
			}
		}
		return VisitCode.cont;
	}

	public VisitCode menuItemsFor(iVisualElement source, Map<String, iUpdateable> items) {
		
		System.out.println(" menu items for <"+source+">  starting at <"+iVisualElementOverrides.topology.getAt()+">");	
		
		List<iVisualElement> fringe = new LinkedList<iVisualElement>();
		fringe.add(iVisualElementOverrides.topology.getAt());

		HashSet<iVisualElement> seen = new LinkedHashSet<iVisualElement>();

		while (fringe.size() > 0) {
			iVisualElement next = fringe.remove(0);
			if (!seen.contains(next)) {
				iVisualElementOverrides over = next.getProperty(iVisualElement.overrides);
				if (over != null) {
					VisitCode o = over.menuItemsFor(source, items);
					if (o.equals(VisitCode.skip)) {
					} else if (o.equals(VisitCode.stop)) {
						return VisitCode.stop;
					} else {
						if (backwards)
							fringe.addAll((Collection<? extends iVisualElement>) next.getParents());
						else
							fringe.addAll(next.getChildren());
						
						
						System.out.println(" fringe now <"+fringe+"> elements now <"+items.size()+">");
						
					}
				}
				seen.add(next);
			}
		}
		return VisitCode.cont;
	}

	public VisitCode paintNow(iVisualElement source, Rect bounds, boolean visible) {
		List<iVisualElement> fringe = new LinkedList<iVisualElement>();
		fringe.add(iVisualElementOverrides.topology.getAt());

		HashSet<iVisualElement> seen = new LinkedHashSet<iVisualElement>();

		while (fringe.size() > 0) {
			iVisualElement next = fringe.remove(0);
			if (!seen.contains(next)) {
				iVisualElementOverrides over = next.getProperty(iVisualElement.overrides);
				if (over != null) {
					VisitCode o = over.paintNow(source, bounds, visible);
					if (o.equals(VisitCode.skip)) {
					} else if (o.equals(VisitCode.stop)) {
						return VisitCode.stop;
					} else {
						if (backwards)
							fringe.addAll((Collection<? extends iVisualElement>) next.getParents());
						else
							fringe.addAll(next.getChildren());
					}
				}
				seen.add(next);
			}
		}
		return VisitCode.cont;
	}

	public VisitCode prepareForSave() {
		List<iVisualElement> fringe = new LinkedList<iVisualElement>();
		fringe.add(iVisualElementOverrides.topology.getAt());

		HashSet<iVisualElement> seen = new LinkedHashSet<iVisualElement>();

		while (fringe.size() > 0) {
			iVisualElement next = fringe.remove(0);
			if (!seen.contains(next)) {
				iVisualElementOverrides over = next.getProperty(iVisualElement.overrides);
				if (over != null) {
					VisitCode o = over.prepareForSave();
					if (o.equals(VisitCode.skip)) {
					} else if (o.equals(VisitCode.stop)) {
						return VisitCode.stop;
					} else {
						if (backwards)
							fringe.addAll((Collection<? extends iVisualElement>) next.getParents());
						else
							fringe.addAll(next.getChildren());
					}
				}
				seen.add(next);
			}
		}
		return VisitCode.cont;
	}

	public <T> VisitCode setProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> to) {
		List<iVisualElement> fringe = new LinkedList<iVisualElement>();
		fringe.add(iVisualElementOverrides.topology.getAt());

		HashSet<iVisualElement> seen = new LinkedHashSet<iVisualElement>();

		while (fringe.size() > 0) {
			iVisualElement next = fringe.remove(0);
			if (!seen.contains(next)) {
				iVisualElementOverrides over = next.getProperty(iVisualElement.overrides);
				if (over != null) {
					VisitCode o = over.setProperty(source, prop, to);
					if (o.equals(VisitCode.skip)) {
					} else if (o.equals(VisitCode.stop)) {
						return VisitCode.stop;
					} else {
						if (backwards)
							fringe.addAll((Collection<? extends iVisualElement>) next.getParents());
						else
							fringe.addAll(next.getChildren());
					}
				}
				seen.add(next);
			}
		}
		return VisitCode.cont;
	}

	public VisitCode shouldChangeFrame(iVisualElement source, Rect newFrame, Rect oldFrame, boolean now) {
		List<iVisualElement> fringe = new LinkedList<iVisualElement>();
		fringe.add(iVisualElementOverrides.topology.getAt());

		HashSet<iVisualElement> seen = new LinkedHashSet<iVisualElement>();

		while (fringe.size() > 0) {
			iVisualElement next = fringe.remove(0);
			if (!seen.contains(next)) {
				iVisualElementOverrides over = next.getProperty(iVisualElement.overrides);
				if (over != null) {
					VisitCode o = over.shouldChangeFrame(source, newFrame, oldFrame, now);
					if (o.equals(VisitCode.skip)) {
					} else if (o.equals(VisitCode.stop)) {
						return VisitCode.stop;
					} else {
						if (backwards)
							fringe.addAll((Collection<? extends iVisualElement>) next.getParents());
						else
							fringe.addAll(next.getChildren());
					}
				}
				seen.add(next);
			}
		}
		return VisitCode.cont;
	}

}
