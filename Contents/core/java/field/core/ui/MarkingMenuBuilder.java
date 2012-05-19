package field.core.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.python.core.Py;
import org.python.core.PyObject;

import field.core.execution.PythonInterface;
import field.core.plugins.python.PythonPlugin.CapturedEnvironment;
import field.core.ui.PopupMarkingArea.PopMenuSpec;
import field.core.ui.PopupMarkingArea.Position;
import field.launch.iUpdateable;

public class MarkingMenuBuilder {

	HashMap<Position, PopMenuSpec> spec = new HashMap<Position, PopMenuSpec>();

	PopMenuSpec currentlyBuilding;

	iUpdateable singleAction;

	public boolean insertCopyPasteItems = false;
	public boolean insertDeleteItem = false;

	public MarkingMenuBuilder() {

	}

	boolean inverted = false;

	public MarkingMenuBuilder setInverted(boolean invert) {
		this.inverted = invert;
		return this;
	}

	public PopupMarkingArea getMenu(Control invoker, Point screen) {

		if (singleAction != null) {
			try {
				singleAction.update();
			} catch (Throwable t) {
				t.printStackTrace();

			}
			return null;
		}

		for (PopupMarkingArea.PopMenuSpec s : spec.values()) {
			if (s.menu != null && s.menu.size() == 0)
				s.menu = null;
			else if (s.menu != null && s.menu.size() == 1 && s.mainCallback == null) {
				s.mainCallback = s.menu.entrySet().iterator().next().getValue();
				s.mainLabel = s.menu.entrySet().iterator().next().getKey();
				s.menu = null;
			}
		}

		PopupMarkingArea m = new PopupMarkingArea(invoker, new Point(screen.x, screen.y), new ArrayList<PopMenuSpec>(spec.values()));
		m.setInverted(inverted);
		m.shell.forceFocus();
		return m;
	}

	public MarkingMenuBuilder defaultMenu() {
		currentlyBuilding = getSpecFor(Position.Z);
		currentlyBuilding.mainLabel = "\u2386";
		currentlyBuilding.initiallyOpen = true;

		return this;
	}

	public void setSingleAction(iUpdateable singleAction) {
		this.singleAction = singleAction;
	}

	public void setSingleActionFunction(PyObject f) {
		this.singleAction = wrap(f);
	}

	public MarkingMenuBuilder marking(String defaultName, String position) {
		currentlyBuilding = getSpecFor(Position.valueOf(position));

		currentlyBuilding.mainLabel = defaultName;

		return this;
	}

	public MarkingMenuBuilder newMenu(String defaultName, String defaultPosition) {
		Position s = nextUnalloc(defaultPosition);

		currentlyBuilding = getSpecFor(s);
		currentlyBuilding.mainLabel = defaultName;
		currentlyBuilding.solo = true;

		return this;
	}

	private Position nextUnalloc(String defaultPosition) {
		Position s = Position.valueOf(defaultPosition);
		int i = 1;
		while (spec.containsKey(s) && i < Position.values().length) {
			s = Position.values()[(s.ordinal() + i) % Position.values().length];
			i++;
		}
		return s;
	}

	public MarkingMenuBuilder call(iUpdateable u) {
		currentlyBuilding.mainCallback = u;
		return this;
	}

	public MarkingMenuBuilder call(PyObject u) {
		return call(wrap(u));
	}

	public MarkingMenuBuilder addUpdateable(String text, iUpdateable u) {
		if (currentlyBuilding == null)
			defaultMenu();

//		if (!text.contains("<"))
//			text = "<b>" + text+"</b>";
		currentlyBuilding.menu.put(text, u);

		return this;
	}

	public MarkingMenuBuilder subtitle(String text) {
		return addUpdateable(text, (iUpdateable) null);
	}

	public MarkingMenuBuilder add(String text, PyObject u) {
		return addUpdateable(text, wrap(u));
	}

	public MarkingMenuBuilder add(String icon, String text, iUpdateable u) {
		if (currentlyBuilding == null)
			defaultMenu();

		currentlyBuilding.menu.put(" " + icon + " " + text, u);

		return this;
	}

	public MarkingMenuBuilder add(String icon, String text, PyObject u) {
		return add(icon, text, wrap(u));
	}

	public Object[] extraArguments = null;

	public void setExtraArguments(Object[] extraArguments) {
		this.extraArguments = extraArguments;
	}

	private iUpdateable wrap(final PyObject u) {

		final CapturedEnvironment env = (CapturedEnvironment) PythonInterface.getPythonInterface().getVariable("_environment");
		return new iUpdateable() {

			public void update() {

				System.out.println(" entering env <" + env + ">");

				env.enter();
				try {
					if (extraArguments == null)
						u.__call__();
					else {
						PyObject[] o = new PyObject[extraArguments.length];
						for (int i = 0; i < extraArguments.length; i++) {
							o[i] = Py.java2py(extraArguments[i]);
						}

						u.__call__(o);
					}
				} catch (Throwable t) {
					t.printStackTrace();
				} finally {
					env.exit();
				}
			}
		};
	}

	private PopMenuSpec getSpecFor(Position z) {
		PopMenuSpec m = spec.get(z);
		if (m == null) {
			spec.put(z, m = new PopMenuSpec(z, new LinkedHashMap<String, iUpdateable>(), null, "Untitled menu"));
		}
		return m;
	}

	public Map<String, iUpdateable> getMap() {
		if (currentlyBuilding == null)
			defaultMenu();

		return currentlyBuilding.menu;
	}

	public MarkingMenuBuilder mergeWith(MarkingMenuBuilder u) {
		if (u == null)
			return this;
		MarkingMenuBuilder b = new MarkingMenuBuilder();

		{
			Set<Entry<Position, PopMenuSpec>> es = this.spec.entrySet();
			for (Entry<Position, PopMenuSpec> e : es) {
				b.spec.put(e.getKey(), e.getValue().copy());
			}
		}
		{
			Set<Entry<Position, PopMenuSpec>> es = this.spec.entrySet();
			for (Entry<Position, PopMenuSpec> e : es) {
				if (b.spec.containsKey(e.getKey())) {
					if (b.spec.get(e.getKey()).initiallyOpen || e.getValue().initiallyOpen) {
						PopMenuSpec ll = b.spec.get(e.getKey());
						ll.menu.putAll(e.getValue().menu);
					} else if (b.spec.get(e.getKey()).solo && !e.getValue().solo) {
						Position m = b.nextUnalloc(e.getKey().name());
						b.spec.put(m, e.getValue());
					} else if (!b.spec.get(e.getKey()).solo && e.getValue().solo) {
						Position m = b.nextUnalloc(e.getKey().name());
						b.spec.put(m, e.getValue());
					}
				} else
					b.spec.put(e.getKey(), e.getValue().copy());
			}
		}

		b.insertCopyPasteItems = this.insertCopyPasteItems || u.insertCopyPasteItems;
		b.insertDeleteItem = this.insertDeleteItem || u.insertDeleteItem;

		return b;

	}

}
