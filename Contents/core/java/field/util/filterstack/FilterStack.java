package field.util.filterstack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.Aliasing;
import field.core.util.FieldPyObjectAdaptor.iHandlesAttributes;
import field.launch.iUpdateable;
import field.math.abstraction.iDoubleProvider;
import field.math.abstraction.iProvider;
import field.util.filterstack.Unit.WindowStatus;
import field.util.filterstack.Unit.iProvidesWindowStatus;

@Woven
public abstract class FilterStack<T> implements iUpdateable, iProvider<T>, iHandlesAttributes {

	public interface iSelectPosition<T> {
		public Position select(Unit<T> at, Position desc);
	}

	public enum Position {
		after, before, end, start, anywhere, nothere;
	}

	private final iProvider<T> input;

	private final iDoubleProvider clock;

	LinkedHashMap<String, Unit<T>> lookupStack = new LinkedHashMap<String, Unit<T>>();

	List<Unit<T>> stack = new ArrayList<Unit<T>>();

	T out;

	double time;

	public FilterStack(iProvider<T> input, iDoubleProvider clock) {
		this.input = input;
		this.clock = clock;
	}

	public void add(Unit<T> unit, iSelectPosition<T> s) {
		for (int i = -1; i < stack.size(); i++) {
			Unit<T> a = i >= 0 ? stack.get(i) : null;
			Position selection = s.select(a, i == -1 ? Position.start : (i == stack.size() - 1 ? Position.end : Position.anywhere));
			if (selection == Position.after) {
				stack.add(i + 1, unit);
				lookupStack.put(unit.name, unit);
				return;
			} else if (selection == Position.before) {
				stack.add(Math.max(i, 0), unit);
				lookupStack.put(unit.name, unit);
				return;
			} else if (selection == Position.end) {
				stack.add(unit);
				lookupStack.put(unit.name, unit);
				return;
			} else if (selection == Position.start) {
				stack.add(0, unit);
				lookupStack.put(unit.name, unit);
				return;
			} else if (i == stack.size() - 1) {
				stack.add(0, unit);
				lookupStack.put(unit.name, unit);
				return;
			}
		}
	};

	public void addFirst(Unit<T> unit) {
		stack.add(0, unit);
		lookupStack.put(unit.name, unit);
	}

	abstract public T addImpl(T a, double w, T to);

	public void addLast(Unit<T> unit) {
		stack.add(unit);
		lookupStack.put(unit.name, unit);
	}

	public T blend(T a, T b, double ea) {
		if (ea == 0)
			return a;
		if (ea == 1)
			return b;
		return blendImpl(a, b, ea);
	}

	public T get() {
		return out;
	}

	public double getTime() {
		return time;
	}

	public void removeUnit(Unit<T> u) {
		stack.remove(u);
		lookupStack.remove(u.name);
	}

	@Aliasing(key = "filterStack")
	public void update() {

		Unit<T> previousUnit = null;
		ArrayList<Unit<T>> savedStack = new ArrayList<Unit<T>>(stack);
		double time = clock.evaluate();
		this.time = time;
		for (Unit<T> t : savedStack) {

			if (t.inputName == null)
				t.input = previousUnit == null ? this.input.get() : previousUnit.lastOutput;
			else {
				Unit<T> a = lookupStack.get(t.inputName);
				t.input = a == null ? (previousUnit == null ? this.input.get() : previousUnit.lastOutput) : a.lastOutput;
			}

			if (t.blendInputName == null)
				t.blendInput = previousUnit == null ? this.input.get() : previousUnit.lastOutput;
			else {
				Unit<T> a = lookupStack.get(t.blendInputName);
				t.blendInput = a == null ? (previousUnit == null ? this.input.get() : previousUnit.lastOutput) : a.lastOutput;
			}

			;
			t.update(time);
			previousUnit = t;
		}
		out = this.input.get();
		for (Unit<T> t : savedStack) {
			if (t.outputName == null)
				out = t.lastTotalOutput = previousUnit.lastOutput;
			else {
				Unit<T> a = lookupStack.get(t.outputName);
				out = t.lastTotalOutput = a == null ? previousUnit.lastOutput : a.lastOutput;
			}
		}

		for (Unit<T> t : savedStack) {
			if (t instanceof iProvidesWindowStatus)
				if (((iProvidesWindowStatus) t).getWindowState() == WindowStatus.finished)
					stack.remove(t);
		}

	}

	abstract protected T blendImpl(T a, T b, double ea);

	public Object getAttribute(String name) {
		for (Unit u : stack) {
			if (u.getName().equals(name))
				return u;
		}
		return null;
	}

	public void setAttribute(String name, Object value) {
		for(Unit u : stack)
		{
			if (u.getName().equals(name))
			{
				int ii = stack.indexOf(u);
				stack.set(ii, (Unit<T>) value);
				((Unit<T>)value).name = name;
				lookupStack.put(name, (Unit<T>) value);
				return;
			}
		}
		
		addLast((Unit<T>) value);
		
	}
}
