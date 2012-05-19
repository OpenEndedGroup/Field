package field.extras.osc;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.execution.PythonInterface;
import field.core.network.OSCInput;
import field.core.network.OSCInput.DispatchableHandler;
import field.core.util.FieldPyObjectAdaptor.iCallable;
import field.core.util.PythonCallableMap;

public class OSCIn implements iCallable {

	static public VisualElementProperty<PythonCallableMap> handleOsc = new VisualElementProperty<PythonCallableMap>("handleOSC_");

	static public HashMap<Integer, OSCIn> knownInterfaces = new LinkedHashMap<Integer, OSCIn>();

	private final iVisualElement root;

	private OSCInput in;

	Set<OSCIn> children = new LinkedHashSet<OSCIn>();

	public OSCIn(iVisualElement root, int port) {
		this.root = root;

		System.out.println(" opening input for <" + root + " : " + port + ">");

		in = new OSCInput(port);
		in.setDefaultHandler(new DispatchableHandler() {
			public void handle(String s, Object[] args) {

				System.out.println(" default handle :" + s + " " + args);

				defaultHandle(s, args);
			}
		});
	}

	public void dispatchOver(iVisualElement r) {
		dispatchOver.add(r);
	}

	Set<iVisualElement> dispatchOver = new LinkedHashSet<iVisualElement>();

	protected void defaultHandle(String s, Object[] args) {
		String[] split = s.split("/");
		Stack<String> elements = new Stack<String>();
		elements.addAll(Arrays.asList(split));
		Collections.reverse(elements);

		System.out.println(" looking at default handle for <" + s + "> starting with <" + dispatchOver + ">");

		Set<iVisualElement> fringe = new LinkedHashSet<iVisualElement>(dispatchOver);
		Set<iVisualElement> nextFringe = new LinkedHashSet<iVisualElement>();
		Set<iVisualElement> success = new LinkedHashSet<iVisualElement>(dispatchOver);

		while (elements.size() > 0) {
			if (elements.peek().trim().length() == 0) {
				elements.pop();
				continue;
			}

			nextFringe.clear();

			for (iVisualElement e : fringe) {
				for (iVisualElement p : ((Collection<iVisualElement>) e.getParents())) {

					System.out.println(" checking <" + elements.peek() + ">");

					if (matches(elements.peek(), p)) {
						success.remove(e);
						success.add(p);
						nextFringe.add(p);
					}
				}
			}
			elements.pop();

			fringe.clear();
			fringe.addAll(nextFringe);
		}
		if (success.size() == 0) {
			noMatch(s, args);
		} else
			for (iVisualElement e : success) {
				match(e, s, args);
			}
	}

	private void match(iVisualElement e, String s, Object[] args) {
		PythonCallableMap map = handleOsc.get(e, e);
		System.out.println(" does <" + e + "> have a map ? " + map + " " + e.payload());
		if (map != null) {
			Object[] o = new Object[args.length + 1];
			System.arraycopy(args, 0, o, 1, args.length);
			o[0] = s;

			System.out.println(" about to call map <" + map + "> <" + s + ">");
			map.invoke(o);
		}
	}

	protected boolean matches(String peek, iVisualElement e) {
		peek = peek.replace(".", "\\.");
		peek = peek.replace("*", ".*");
		peek = peek.replace("?", ".");
		String name = iVisualElement.name.get(e);

		System.out.println(" checking <" + peek + "> against <" + name + ">");

		boolean b = Pattern.matches(peek, name);

		System.out.println(" got <" + b + ">");
		return b;
	}

	protected void noMatch(String s, Object[] args) {
		System.err.println(" warning, nothing handled OSC message '" + s + "' " + Arrays.asList(args));
	}

	void update() {

//		System.out.println(" -- update of OSCIn --");
		iVisualElement e = (iVisualElement) PythonInterface.getPythonInterface().getVariable("_self");
		if (e != null)
			dispatchOver.add(e);
		in.update();
		for (OSCIn o : children)
			o.update();

		if (e != null)
			dispatchOver.remove(e);

	}

	public Object call(Object[] args) {

		if (args.length == 0)
			return this;
		if (args.length == 1) {
			OSCIn x = knownInterfaces.get(((Number) args[0]).intValue());
			if (x == null)
				knownInterfaces.put(((Number) args[0]).intValue(), x = new OSCIn(root, ((Number) args[0]).intValue()));
			children.add(x);
			return x;
		}
		throw new IllegalArgumentException(" expected 0, or 1 args to OSCIn()");
	}

	public void close() {
		in.close();
		for (OSCIn ii : children) {
			ii.close();
		}
	}

}
