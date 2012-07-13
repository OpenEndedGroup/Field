package field.core.plugins.autoexecute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.python.core.Py;
import org.python.core.PyObject;

import com.sun.org.apache.xpath.internal.compiler.PsuedoNames;

import field.core.StandardFluidSheet;
import field.core.dispatch.iVisualElement;
import field.core.execution.PythonInterface;
import field.core.execution.ScriptingInterface.iGlobalTrap;
import field.core.plugins.drawing.SplineComputingOverride;
import field.core.plugins.log.ElementInvocationLogging;
import field.core.plugins.log.Logging;
import field.core.plugins.pseudo.PseudoPropertiesPlugin;
import field.core.plugins.python.OutputInserts;
import field.core.plugins.python.PythonPlugin;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Pair;
import field.util.HashMapOfLists;

public class Globals {

	HashMap<String, iVisualElement> potentialDeclarations = new HashMap<String, iVisualElement>() {
		protected java.util.Collection<iVisualElement> newList() {
			return new LinkedHashSet();
		};
	};
	LinkedHashSet<String> knownMultipleDeclarations = new LinkedHashSet<String>();

	HashMap<String, iVisualElement> knownDeclarations = new HashMap<String, iVisualElement>() {
		protected java.util.Collection<iVisualElement> newList() {
			return new LinkedHashSet();
		};
	};

	HashMapOfLists<String, iVisualElement> recordedUses = new HashMapOfLists<String, iVisualElement>() {
		protected java.util.Collection<iVisualElement> newList() {
			return new LinkedHashSet();
		};
	};

	Object nothing = new Object();

	Stack<iVisualElement> running = new Stack<iVisualElement>();

	public Globals() {
	}

	public void declare(iVisualElement e, String name) {
		knownDeclarations.put(name, e);
	}

	public List<String> getPotentialDefinedBy(iVisualElement oneThing) {
		List<String> r = new ArrayList<String>();
		Set<Entry<String, iVisualElement>> es = potentialDeclarations.entrySet();
		for (Entry<String, iVisualElement> e : es) {
			if (e.getValue().equals(oneThing)) {
				r.add(e.getKey());
			}
		}
		return r;
	}

	public List<Pair<String, iVisualElement>> getUsedBy(iVisualElement oneThing) {
		Collection<Pair<String, iVisualElement>> r = new LinkedHashSet<Pair<String, iVisualElement>>();
		Set<Entry<String, Collection<iVisualElement>>> m = recordedUses.entrySet();
		for (Entry<String, Collection<iVisualElement>> e : m) {
			for (iVisualElement v : e.getValue()) {
				if (v.equals(oneThing)) {
					r.add(new Pair<String, iVisualElement>(e.getKey(), knownDeclarations.get(e.getKey())));
				}
			}
		}
		return new ArrayList<Pair<String, iVisualElement>>(r);
	}

	public HashMapOfLists<String, iVisualElement> getUsedDefinedBy(iVisualElement oneThing) {
		HashMapOfLists<String, iVisualElement> r = new HashMapOfLists<String, iVisualElement>() {
			@Override
			protected Collection<iVisualElement> newList() {
				return new LinkedHashSet<iVisualElement>();
			}
		};

		Set<Entry<String, iVisualElement>> es = knownDeclarations.entrySet();
		for (Entry<String, iVisualElement> e : es) {
			if (e.getValue().equals(oneThing)) {
				String m = e.getKey();

				Collection<iVisualElement> q = recordedUses.get(m);
				if (q == null)
					q = Collections.EMPTY_LIST;

				r.addAllToList(m, q);
			}
		}
		return r;
	}

	public iVisualElement getExistingDefinitionFor(String name) {
		return knownDeclarations.get(name);
	}

	public iVisualElement getPotentialDefinitionFor(String name) {
		return potentialDeclarations.get(name);
	}

	public List<iVisualElement> getUses(String name) {
		Collection<iVisualElement> rr = recordedUses.get(name);
		if (rr == null)
			return null;
		return new ArrayList<iVisualElement>(rr);
	}

	public iGlobalTrap globalTrapFor(final iVisualElement e) {
		return new iGlobalTrap() {

			public Object findItem(String name, Object actuallyIs) {

				if (name.startsWith("_")) {
					if (Logging.enabled())
						Logging.logging.addEvent(new ElementInvocationLogging.DidGetLocalVariable(name, actuallyIs));
					return actuallyIs;
				}
				if (actuallyIs == null) {
					iVisualElement willExec = knownDeclarations.get(name);
					if (willExec != null) {
						if (Logging.enabled())
							Logging.logging.addEvent(new ElementInvocationLogging.WillGetLocalVariableByAutoExecution(name, willExec));

						Object o = trapAutoExec(e, name);
						if (o == nothing) {
							Logging.logging.addEvent(new ElementInvocationLogging.DidGetLocalVariableByAutoExecution(name, willExec, null));
							return actuallyIs;
						}
						if (Logging.enabled())
							Logging.logging.addEvent(new ElementInvocationLogging.DidGetLocalVariableByAutoExecution(name, willExec, o));
						recordedUses.addToList(name, e);
						return o;
					}
					if (Logging.enabled())
						Logging.logging.addEvent(new ElementInvocationLogging.DidGetLocalVariable(name, null));
					return actuallyIs;
				} else if (potentialDeclarations.containsKey(name)) {
					trapFinalizeDeclaration(e, name, actuallyIs);
				}

				if (Logging.enabled())
					Logging.logging.addEvent(new ElementInvocationLogging.DidGetLocalVariable(name, actuallyIs));
				return actuallyIs;
			}

			public Object setItem(String name, Object was, Object to) {

				if (Logging.enabled())
					Logging.logging.addEvent(new ElementInvocationLogging.DidSetLocalVariable(name, to, was));
				if (name.startsWith("_"))
					return to;
				if ((was == null || was == Py.None) && (to != null && to != Py.None)) {
					trapDeclareInside(e, name, to);
				}
				return to;
			}
		};
	}

	private String declForName(String name) {
		return "_a.python_globals_.declare(_self, \"" + name + "\")\n";
	}

	private String declForNameNew(String name) {
		return "_self.python_globals_.declare(_self, \"" + name + "\")\n";
	}

	protected Object trapAutoExec(iVisualElement e, String name) {
		iVisualElement known = knownDeclarations.get(name);
		if (known != null) {
			if (!running.contains(known)) {
				running.add(known);
				try {

					// PythonInterface
					// .
					// getPythonInterface
					// (
					// )
					// .
					// execString
					// (
					// "print 'trapAutoExec:"
					// +
					// known
					// .
					// getProperty
					// (
					// iVisualElement
					// .
					// name
					// )
					// +
					// " for "
					// +
					// name
					// +
					// "'"
					// )
					// ;

					PythonInterface.getPythonInterface().print("Automatically executing " + known.getProperty(iVisualElement.name) + "' to resolve '" + name + "'\n");

					;//System.out.println("auto executing '" + known.getProperty(iVisualElement.name) + "' to resolve '" + name + "'");
					// OutputInserts.printFoldStart("auto executing '"
					// +
					// known.getProperty(iVisualElement.name)
					// + "' to resolve '" + name + "'", e,
					// new Vector4(0, 0, 0.5f, 0.25f));
					SplineComputingOverride.executeMain(known);
					// OutputInserts.printFoldEnd("auto executing '"
					// +
					// known.getProperty(iVisualElement.name)
					// + "' to resolve '" + name + "'", e);

					// PythonInterface
					// .
					// getPythonInterface
					// (
					// )
					// .
					// execString
					// (
					// "print 'trapAutoExec:"
					// +
					// known
					// .
					// getProperty
					// (
					// iVisualElement
					// .
					// name
					// )
					// +
					// " complete'"
					// )
					// ;

					PythonInterface.getPythonInterface().setVariable("_self", e);
					PythonPlugin.toolsModule.__dict__.__setitem__("_self".intern(), Py.java2py(e));

					PyObject now = PythonInterface.getPythonInterface().getLocalDictionary().__superfinditem__(name);
					;//System.out.println(" executed and now we get <" + now + ">");
					if (now == null) {
						System.err.println(" warning: tried to execute <" + iVisualElement.name.get(known) + "> in order to declare a <" + name + "> but got nothing");
						// remove
						// the
						// autodecl
						String current = AutoExecutePythonPlugin.python_autoExec.get(e);
						if (current == null)
							return now;

						current = current.replaceAll(declForName(name), "\n");
						current = current.replaceAll(declForNameNew(name), "\n");
						AutoExecutePythonPlugin.python_autoExec.set(e, e, current);
						knownDeclarations.remove(name);
					}
					return now;
				} finally {
					iVisualElement a = running.pop();
					assert a == known;
				}
			}
		}

		// need to look up result

		return nothing;
	}

	protected void trapDeclareInside(iVisualElement e, String name, Object to) {
		if (!knownMultipleDeclarations.contains(name)) {
			iVisualElement x = potentialDeclarations.put(name, e);
			if (x != e && x != null) {
				potentialDeclarations.remove(name);
				knownMultipleDeclarations.add(name);
			}
		}
	}

	protected void trapFinalizeDeclaration(iVisualElement e, String name, Object actuallyIs) {
		if (knownMultipleDeclarations.contains(name))
			return;

		iVisualElement declaredBy = potentialDeclarations.get(name);
		if (declaredBy != e) {
			iVisualElement allready = knownDeclarations.get(name);
			if (allready == null) {

				if (Logging.enabled())
					Logging.logging.addEvent(new ElementInvocationLogging.MakeAutoExecutionTarget(name, declaredBy, actuallyIs));
				
				String current = AutoExecutePythonPlugin.python_autoExec.get(declaredBy);
				if (current == null)
					current = "\n";
				if (!current.endsWith("\n"))
					current += "\n";
				current += declForNameNew(name);
				AutoExecutePythonPlugin.python_autoExec.set(declaredBy, declaredBy, current);

				knownDeclarations.put(name, declaredBy);
			}
			recordedUses.addToList(name, e);
		}
	}

}
