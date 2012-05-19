package field.core.dispatch;

import java.util.ArrayList;
import java.util.Collection;
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
public class FastVisualElementOverridesDispatchAbove implements iVisualElementOverrides {

	private final boolean backwards;

	public FastVisualElementOverridesDispatchAbove(boolean backwards) {
		this.backwards = backwards;
	}

	public VisitCode added(iVisualElement newSource) {

		List<iVisualElement> firstFringe = new ArrayList<iVisualElement>();
		if (backwards)
			firstFringe.addAll((Collection<? extends iVisualElement>) iVisualElementOverrides.topology.getAt().getParents());
		else
			firstFringe.addAll(iVisualElementOverrides.topology.getAt().getChildren());

		for (iVisualElement start : firstFringe) {
			List<iVisualElement> fringe = new LinkedList<iVisualElement>();
			fringe.add(start);

			while (fringe.size() > 0) {
				iVisualElement next = fringe.remove(0);
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
			}
		}
		return VisitCode.cont;
	}

	public VisitCode beginExecution(iVisualElement source) {
		List<iVisualElement> firstFringe = new ArrayList<iVisualElement>();
		if (backwards)
			firstFringe.addAll((Collection<? extends iVisualElement>) iVisualElementOverrides.topology.getAt().getParents());
		else
			firstFringe.addAll(iVisualElementOverrides.topology.getAt().getChildren());

		for (iVisualElement start : firstFringe) {
			List<iVisualElement> fringe = new LinkedList<iVisualElement>();
			fringe.add(start);

			while (fringe.size() > 0) {
				iVisualElement next = fringe.remove(0);
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
			}
		}
		return VisitCode.cont;
	}

	public <T> VisitCode deleteProperty(iVisualElement source, VisualElementProperty<T> prop) {
		List<iVisualElement> firstFringe = new ArrayList<iVisualElement>();
		if (backwards)
			firstFringe.addAll((Collection<? extends iVisualElement>) iVisualElementOverrides.topology.getAt().getParents());
		else
			firstFringe.addAll(iVisualElementOverrides.topology.getAt().getChildren());

		for (iVisualElement start : firstFringe) {
			List<iVisualElement> fringe = new LinkedList<iVisualElement>();
			fringe.add(start);

			while (fringe.size() > 0) {
				iVisualElement next = fringe.remove(0);
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
			}
		}
		return VisitCode.cont;
	}

	public VisitCode deleted(iVisualElement source) {
		List<iVisualElement> firstFringe = new ArrayList<iVisualElement>();
		if (backwards)
			firstFringe.addAll((Collection<? extends iVisualElement>) iVisualElementOverrides.topology.getAt().getParents());
		else
			firstFringe.addAll(iVisualElementOverrides.topology.getAt().getChildren());

		for (iVisualElement start : firstFringe) {
			List<iVisualElement> fringe = new LinkedList<iVisualElement>();
			fringe.add(start);

			while (fringe.size() > 0) {
				iVisualElement next = fringe.remove(0);
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
			}
		}
		return VisitCode.cont;
	}

	public VisitCode endExecution(iVisualElement source) {
		List<iVisualElement> firstFringe = new ArrayList<iVisualElement>();
		if (backwards)
			firstFringe.addAll((Collection<? extends iVisualElement>) iVisualElementOverrides.topology.getAt().getParents());
		else
			firstFringe.addAll(iVisualElementOverrides.topology.getAt().getChildren());

		for (iVisualElement start : firstFringe) {
			List<iVisualElement> fringe = new LinkedList<iVisualElement>();
			fringe.add(start);

			while (fringe.size() > 0) {
				iVisualElement next = fringe.remove(0);
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
			}
		}
		return VisitCode.cont;
	}

	public <T> VisitCode getProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> ref) {

		List<iVisualElement> firstFringe = new ArrayList<iVisualElement>();
		if (backwards)
			firstFringe.addAll((Collection<? extends iVisualElement>) iVisualElementOverrides.topology.getAt().getParents());
		else
			firstFringe.addAll(iVisualElementOverrides.topology.getAt().getChildren());

		for (iVisualElement start : firstFringe) {
			List<iVisualElement> fringe = new LinkedList<iVisualElement>();
			fringe.add(start);

			while (fringe.size() > 0) {
				iVisualElement next = fringe.remove(0);
				iVisualElementOverrides over = next.getProperty(iVisualElement.overrides);
				if (over != null) {
					VisitCode o = over.getProperty(start, prop, ref);
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
			}
		}
		return VisitCode.cont;
	}

	public VisitCode handleKeyboardEvent(iVisualElement newSource, Event event) {
		List<iVisualElement> firstFringe = new ArrayList<iVisualElement>();
		if (backwards)
			firstFringe.addAll((Collection<? extends iVisualElement>) iVisualElementOverrides.topology.getAt().getParents());
		else
			firstFringe.addAll(iVisualElementOverrides.topology.getAt().getChildren());

		for (iVisualElement start : firstFringe) {
			List<iVisualElement> fringe = new LinkedList<iVisualElement>();
			fringe.add(start);

			while (fringe.size() > 0) {
				iVisualElement next = fringe.remove(0);
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
			}
		}
		return VisitCode.cont;
	}

	public VisitCode inspectablePropertiesFor(iVisualElement source, List<Prop> properties) {
		List<iVisualElement> firstFringe = new ArrayList<iVisualElement>();
		if (backwards)
			firstFringe.addAll((Collection<? extends iVisualElement>) iVisualElementOverrides.topology.getAt().getParents());
		else
			firstFringe.addAll(iVisualElementOverrides.topology.getAt().getChildren());

		for (iVisualElement start : firstFringe) {
			List<iVisualElement> fringe = new LinkedList<iVisualElement>();
			fringe.add(start);

			while (fringe.size() > 0) {
				iVisualElement next = fringe.remove(0);
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
			}
		}
		return VisitCode.cont;
	}

	public VisitCode isHit(iVisualElement source, Event event, Ref<Boolean> is) {

		List<iVisualElement> firstFringe = new ArrayList<iVisualElement>();
		if (backwards)
			firstFringe.addAll((Collection<? extends iVisualElement>) iVisualElementOverrides.topology.getAt().getParents());
		else
			firstFringe.addAll(iVisualElementOverrides.topology.getAt().getChildren());

		for (iVisualElement start : firstFringe) {
			List<iVisualElement> fringe = new LinkedList<iVisualElement>();
			fringe.add(start);

			while (fringe.size() > 0) {
				iVisualElement next = fringe.remove(0);
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
			}
		}
		return VisitCode.cont;
	}

	public VisitCode menuItemsFor(iVisualElement source, Map<String, iUpdateable> items) {
		List<iVisualElement> firstFringe = new ArrayList<iVisualElement>();
		if (backwards)
			firstFringe.addAll((Collection<? extends iVisualElement>) iVisualElementOverrides.topology.getAt().getParents());
		else
			firstFringe.addAll(iVisualElementOverrides.topology.getAt().getChildren());

		for (iVisualElement start : firstFringe) {
			List<iVisualElement> fringe = new LinkedList<iVisualElement>();
			fringe.add(start);

			while (fringe.size() > 0) {
				iVisualElement next = fringe.remove(0);
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
					}
				}
			}
		}
		return VisitCode.cont;
	}

	public VisitCode paintNow(iVisualElement source, Rect bounds, boolean visible) {
		List<iVisualElement> firstFringe = new ArrayList<iVisualElement>();
		if (backwards)
			firstFringe.addAll((Collection<? extends iVisualElement>) iVisualElementOverrides.topology.getAt().getParents());
		else
			firstFringe.addAll(iVisualElementOverrides.topology.getAt().getChildren());

		for (iVisualElement start : firstFringe) {
			List<iVisualElement> fringe = new LinkedList<iVisualElement>();
			fringe.add(start);

			while (fringe.size() > 0) {
				iVisualElement next = fringe.remove(0);
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
			}
		}
		return VisitCode.cont;
	}

	public VisitCode prepareForSave() {
		List<iVisualElement> firstFringe = new ArrayList<iVisualElement>();
		if (backwards)
			firstFringe.addAll((Collection<? extends iVisualElement>) iVisualElementOverrides.topology.getAt().getParents());
		else
			firstFringe.addAll(iVisualElementOverrides.topology.getAt().getChildren());

		for (iVisualElement start : firstFringe) {
			List<iVisualElement> fringe = new LinkedList<iVisualElement>();
			fringe.add(start);

			while (fringe.size() > 0) {
				iVisualElement next = fringe.remove(0);
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
			}
		}
		return VisitCode.cont;
	}

	public <T> VisitCode setProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> to) {
		List<iVisualElement> firstFringe = new ArrayList<iVisualElement>();
		if (backwards)
			firstFringe.addAll((Collection<? extends iVisualElement>) iVisualElementOverrides.topology.getAt().getParents());
		else
			firstFringe.addAll(iVisualElementOverrides.topology.getAt().getChildren());

		for (iVisualElement start : firstFringe) {
			List<iVisualElement> fringe = new LinkedList<iVisualElement>();
			fringe.add(start);

			while (fringe.size() > 0) {
				iVisualElement next = fringe.remove(0);
				iVisualElementOverrides over = next.getProperty(iVisualElement.overrides);
				if (over != null) {
					VisitCode o = over.setProperty(start, prop, to);
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
			}
		}
		return VisitCode.cont;
	}

	public VisitCode shouldChangeFrame(iVisualElement source, Rect newFrame, Rect oldFrame, boolean now) {
		List<iVisualElement> firstFringe = new ArrayList<iVisualElement>();
		if (backwards)
			firstFringe.addAll((Collection<? extends iVisualElement>) iVisualElementOverrides.topology.getAt().getParents());
		else
			firstFringe.addAll(iVisualElementOverrides.topology.getAt().getChildren());

		for (iVisualElement start : firstFringe) {
			List<iVisualElement> fringe = new LinkedList<iVisualElement>();
			fringe.add(start);

			while (fringe.size() > 0) {
				iVisualElement next = fringe.remove(0);
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
			}
		}
		return VisitCode.cont;
	}

}
