package field.core.plugins.drawing.opengl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.python.core.Py;
import org.python.core.PyFunction;
import org.python.core.PyObject;

import field.core.dispatch.iVisualElement;
import field.core.execution.PythonInterface;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.plugins.python.PythonPlugin.CapturedEnvironment;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.namespace.generic.Generics.Pair;
import field.util.Dict;

public class LateExecutingDrawing {

	HashMap<Pair<iVisualElement, Object>, CachedLine> knownLines = new HashMap<field.namespace.generic.Generics.Pair<iVisualElement, Object>, CachedLine>();
	HashMap<Pair<iVisualElement, Object>, CachedLine.Event> knownEvents = new HashMap<field.namespace.generic.Generics.Pair<iVisualElement, Object>, CachedLine.Event>();

	static public class WrappedObject {
		public WrappedObject(Object to, CapturedEnvironment variable) {
			this.o = to;
			this.env = variable;
		}

		Object o;
		CapturedEnvironment env;
	}

	public void begin() {
		knownLines.clear();
		knownEvents.clear();
	}

	public void scanLine(CachedLine line, Dict properties) {
		Object named = properties.get(iLinearGraphicsContext.name);
		if (named != null) {
			iVisualElement s = properties.get(iLinearGraphicsContext.source);
			if (s != null) {
				knownLines.put(new Pair<iVisualElement, Object>(s, named), line);
				knownLines.put(new Pair<iVisualElement, Object>(null, named), line);
			}
		}

		if (!properties.isTrue(iLinearGraphicsContext.containsCode, false))
			return;

		for (CachedLine.Event e : line.events) {
			if (e.attributes != null) {
				Object name = e.attributes.get(iLinearGraphicsContext.name_v);
				if (name != null) {
					iVisualElement s = properties.get(iLinearGraphicsContext.source);
					if (s != null) {
						knownEvents.put(new Pair<iVisualElement, Object>(s, name), e);
						knownEvents.put(new Pair<iVisualElement, Object>(null, name), e);
					}
				}

			}
		}
	}

	public CachedLine prepLine(CachedLine line, Dict properties) {

		if (properties == null)
			return line;

		if (!properties.isTrue(iLinearGraphicsContext.containsCode, false))
			return line;

		for (CachedLine.Event e : line.events) {
			if (e.attributes != null) {
				Object code = e.attributes.get(iLinearGraphicsContext.code_v);
				if (code != null)
					line = executeCode(line, code, e);
			}
		}

		return line;
	}

	protected CachedLine executeCode(CachedLine line, Object code, Event e) {
		if (code instanceof WrappedObject && ((WrappedObject) code).env != null) {
			((WrappedObject) code).env.enter();
			try {
				return executeCode(line, ((WrappedObject) code).o, e);
			} finally {
				((WrappedObject) code).env.exit();
			}
		} else if (code instanceof List) {
			for (Object cc : ((List) code)) {
				line = executeCode(line, cc, e);
			}
			return line;
		} else if (code instanceof PyFunction) {
			PyFunction f = (PyFunction) code;
			PyObject result = f.__call__(new PyObject[] { Py.java2py(line), Py.java2py(e), Py.java2py(this) }, new String[] { "line", "event", "context" });
			return interpretResult(result.__tojava__(Object.class), line, code, e);
		} else if (code instanceof iInterpretLine) {
			Object r = ((iInterpretLine) code).execute(line, e, this);
			return interpretResult(r, line, code, e);
		} else {
			throw new IllegalArgumentException(" don't know what to do with <" + code + ">");
		}
	}

	protected CachedLine interpretResult(Object result, CachedLine line, Object code, Event e) {
		if (result instanceof Vector2)
			e.setAt(-1, ((Vector2) result));
		if (result instanceof Vector3)
			e.setAt(-1, ((Vector3) result).toVector2());

		// ?

		return line;
	}

	public CachedLine.Event findEvent(iVisualElement ev, Object r) {
		Event e = knownEvents.get(new Pair<iVisualElement, Object>(ev, r));
		return e;
	}

	public CachedLine findLine(iVisualElement e, String name) {
		return knownLines.get(new Pair<iVisualElement, Object>(e, name));
	}

	public interface iInterpretLine {
		public Object execute(CachedLine line, CachedLine.Event event, LateExecutingDrawing context);
	}

	static public void addCode(CachedLine.Event e, Object to) {

		to = new WrappedObject(to, (CapturedEnvironment) PythonInterface.getPythonInterface().getVariable("_environment"));

		e.container.getProperties().put(iLinearGraphicsContext.containsCode, true);
		Object x = e.getAttributes().get(iLinearGraphicsContext.code_v);
		if (x == null)
			e.getAttributes().put(iLinearGraphicsContext.code_v, to);
		else if (x instanceof List)
			((List) x).add(to);
		else {
			ArrayList a = new ArrayList();
			a.add(x);
			a.add(to);
			e.getAttributes().put(iLinearGraphicsContext.code_v, a);
		}

		System.out.println("code now is :" + e.getAttributes().get(iLinearGraphicsContext.code_v));
	}

	static public Object nameEvent(CachedLine.Event e, Object key) {
		e.container.getProperties().put(iLinearGraphicsContext.containsCode, true);
		Object name = e.getAttributes().get(iLinearGraphicsContext.name_v);
		if (name == null) {
			e.getAttributes().put(iLinearGraphicsContext.name_v, key);
			return key;
		}
		return name;

	}

	static public class Relativize implements iInterpretLine {
		private Vector2 atSet;
		private Vector2 atSetSource;
		private Object name;

		public Relativize(CachedLine target, CachedLine.Event source) {
			for (CachedLine.Event e : target.events) {
				new Relativize(e, source);
			}
			source.getContainer().getProperties().putToList(iLinearGraphicsContext.codeDependsTo, target);
		}

		public Relativize(CachedLine.Event target, CachedLine.Event source) {
			atSet = new Vector2();
			target.getDestination(atSet);
			atSetSource = new Vector2();
			source.getDestination(atSetSource);

			name = nameEvent(source, this);

			addCode(target, this);
		}

		public Object execute(CachedLine line, Event event, LateExecutingDrawing context) {
			Event e = context.findEvent(null, name);
			if (e != null) {
				Vector2 nowSource = new Vector2();
				e.getDestination(nowSource);

				Vector2 offset = new Vector2().sub(nowSource, atSetSource);

				Vector2 offset2 = new Vector2().sub(event.getDestination(), atSet);

				offset.sub(offset2);

				for (int i = 0; i < event.args.length / 2; i++) {
					float xx = ((Number) event.args[i * 2 + 0]).floatValue() + offset.x;
					float yy = ((Number) event.args[i * 2 + 1]).floatValue() + offset.y;

					event.args[i * 2 + 0] = xx;
					event.args[i * 2 + 1] = yy;
				}

			} else
				System.out.println(" couldn't find marker");
			return null;
		}
	}

}
