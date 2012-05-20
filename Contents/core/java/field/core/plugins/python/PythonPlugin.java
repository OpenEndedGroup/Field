package field.core.plugins.python;

import static field.core.dispatch.iVisualElementOverrides.forward;
import static field.core.dispatch.iVisualElementOverrides.topology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.python.core.Py;
import org.python.core.PyModule;
import org.python.core.PyObject;

import field.core.Constants;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.Ref;
import field.core.execution.PythonInterface;
import field.core.execution.PythonScriptingSystem;
import field.core.execution.PythonScriptingSystem.DerivativePromise;
import field.core.execution.PythonScriptingSystem.Promise;
import field.core.execution.ScriptingInterface.Language;
import field.core.execution.ScriptingInterface.iGlobalTrap;
import field.core.persistance.VisualElementReference;
import field.core.plugins.iPlugin;
import field.core.plugins.autoexecute.Globals;
import field.core.plugins.drawing.SplineComputingOverride;
import field.core.plugins.log.ElementInvocationLogging;
import field.core.plugins.log.Logging;
import field.core.plugins.log.ElementInvocationLogging.ElementExecutionBegin;
import field.core.plugins.log.ElementInvocationLogging.ElementExecutionEnd;
import field.core.plugins.log.ElementInvocationLogging.ElementExecutionFocusBegin;
import field.core.plugins.log.ElementInvocationLogging.ElementExecutionFocusEnd;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.ComponentDrawingUtils;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.iComponent;
import field.graphics.core.BasicGeometry;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.graphics.dynamic.DynamicMesh;
import field.graphics.dynamic.iDynamicMesh;
import field.launch.iUpdateable;
import field.math.graph.GraphNodeSearching;
import field.math.graph.NodeImpl;
import field.math.graph.iMutableContainer;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.Generics.Pair;
import field.util.ANSIColorUtils;

;

public class PythonPlugin implements iPlugin {

	public class CapturedEnvironment {
		private final iVisualElement element;
		HashMap<String, iUpdateable> exitHandler = new HashMap<String, iUpdateable>();
		HashMap<String, iUpdateable> transientExitHandler = new HashMap<String, iUpdateable>();

		public CapturedEnvironment(iVisualElement element) {
			this.element = element;
		}

		public void addExitHandler(String key, iUpdateable updateable) {
			exitHandler.put(key, updateable);
		}

		public void addTransientHandler(String key, iUpdateable updateable) {
			transientExitHandler.put(key, updateable);
		}

		public boolean hasTransientHandler(String key) {
			return transientExitHandler.containsKey(key);
		}

		public void enter() {
			Promise p = promiseFor(element);
			p.beginExecute();
			configurePythonEnvironment(element);
		}

		public void exit() {
			for (iUpdateable e : exitHandler.values()) {
				e.update();
			}
			for (iUpdateable e : transientExitHandler.values()) {
				e.update();
			}
			transientExitHandler.clear();
			configurePythonPostEnvironment(element);
			Promise p = promiseFor(element);
			p.endExecute();
		}

		public boolean hasExitHandler(String h) {
			return exitHandler.containsKey(h);
		}

		public void runExit() {
			for (iUpdateable e : exitHandler.values()) {
				e.update();
			}
			for (iUpdateable e : transientExitHandler.values()) {
				e.update();
			}
			transientExitHandler.clear();

		}

		public boolean throwException(String when, Throwable t, CapturedEnvironment parent) {
			handleExceptionThrownDuringRunning(when, element, parent == null ? null : parent.element, t);

			return false;
		}

	}

	public class LocalPromise implements Promise, DerivativePromise {

		public final iVisualElement element;

		public Stack<CapturedEnvironment> ongoingEnvironments = new Stack<CapturedEnvironment>();

		private final iVisualElementOverrides forward;

		private final iVisualElementOverrides backward;

		int isExecutionCount = 0;

		VisualElementProperty<String> property = python_source;

		public LocalPromise(iVisualElement element) {
			this.element = element;
			forward = new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(element);
			backward = new iVisualElementOverrides.MakeDispatchProxy().getBackwardsOverrideProxyFor(element);
		}

		public void beginExecute() {
			Logging.external();

			stackOfPythonPositionablesExecuting.push(element);
			CapturedEnvironment ce = configurePythonEnvironment(element);

			if (Logging.enabled())
				Logging.logging.addEvent(new ElementExecutionFocusBegin(element));

			ongoingEnvironments.push(ce);
			ongoingEnvironment = ce;
		}

		public void endExecute() {
			ongoingEnvironments.pop();

			if (Logging.enabled())
				Logging.logging.addEvent(new ElementExecutionFocusEnd(element));
			configurePythonPostEnvironment(element);
			stackOfPythonPositionablesExecuting.pop();

			ongoingEnvironment = environments.size() > 0 ? environments.peek() : null;

			Logging.internal();
		}

		public PyObject getAttributes() {
			return (PyObject) getAttributesForElement(element);
		}

		public Promise getDerivativeWithText(final VisualElementProperty<String> prop) {
			LocalPromise p2 = new LocalPromise(element);
			p2.property = prop;
			return p2;
		}

		public float getEnd() {
			Rect o = new Rect(0, 0, 0, 0);
			element.getFrame(o);
			return (float) (o.x + o.w);
		}

		public Stack<CapturedEnvironment> getOngoingEnvironments() {
			return ongoingEnvironments;
		}

		public float getPriority() {
			Rect o = new Rect(0, 0, 0, 0);
			element.getFrame(o);
			return (float) (o.y + o.x / 1000f);
		}

		public float getStart() {
			Rect o = new Rect(0, 0, 0, 0);
			element.getFrame(o);
			return (float) o.x;
		}

		public String getText() {
			String s = property.get(element);
			iFunction<String, String> f = python_sourceFilter.get(element);
			if (f != null)
				s = f.f(s);

			if (s == null)
				s = "";
			return s;
		}

		@Override
		public String toString() {
			return element.getProperty(iVisualElement.name);
		}

		public void willExecute() {
			backward.setProperty(element, python_isExecuting, new Ref<Boolean>(true));
			backward.setProperty(element, iVisualElement.dirty, new Ref<Boolean>(true));
			isExecutionCount++;
			Logging.external();
			if (Logging.enabled())
				Logging.logging.addEvent(new ElementExecutionBegin(element));
			Logging.internal();
		}

		public void willExecuteSubstring(String actualSubstring, int start, int end) {
			Logging.external();
			if (Logging.enabled())
				Logging.logging.addEvent(new ElementInvocationLogging.ElementTextFragmentWasExecuted(actualSubstring, element));
			Logging.internal();
		}

		public void wontExecute() {
			Logging.external();
			if (Logging.enabled())
				Logging.logging.addEvent(new ElementExecutionEnd(element));
			Logging.internal();

			isExecutionCount = 0;
			if (isExecutionCount < 0)
				isExecutionCount = 0;
			if (isExecutionCount == 0) {
				backward.setProperty(element, python_isExecuting, new Ref<Boolean>(false));
				backward.setProperty(element, iVisualElement.dirty, new Ref<Boolean>(false));
			}
		}

		@Override
		public boolean isPaused() {
			Boolean n = element.getProperty(python_isPaused);
			return (n != null && n);
		}

	}

	public class LocalVisualElement extends NodeImpl<iVisualElement> implements iVisualElement {

		public <T> void deleteProperty(VisualElementProperty<T> p) {
			properties.remove(p);
		}

		public void dispose() {
		}

		public Rect getFrame(Rect out) {
			return null;
		}

		public <T> T getProperty(iVisualElement.VisualElementProperty<T> p) {
			if (p == overrides)
				return (T) elementOverride;
			Object o = properties.get(p);
			return (T) o;
		}

		public String getUniqueID() {
			return pluginId;
		}

		public Map<Object, Object> payload() {
			return properties;
		}

		public void setFrame(Rect out) {
		}

		public iMutableContainer<Map<Object, Object>, iVisualElement> setPayload(Map<Object, Object> t) {
			properties = t;
			return this;
		}

		public <T> iVisualElement setProperty(iVisualElement.VisualElementProperty<T> p, T to) {
			properties.put(p, to);
			return this;
		}

		public void setUniqueID(String uid) {
		}
	}

	public class Overrides extends iVisualElementOverrides.DefaultOverride {
		private TriangleMesh triangles;

		private iDynamicMesh triangle;

		@Override
		public VisitCode added(iVisualElement newSource) {

			informationFor(newSource);
			return VisitCode.cont;
		}

		@Override
		public VisitCode deleted(iVisualElement source) {
			deleteElement(source);
			return VisitCode.cont;
		}

		@Override
		public VisitCode paintNow(iVisualElement source, Rect bounds, boolean visible) {

			// is
			// it
			// executing?
			Boolean is = source.getProperty(python_isExecuting);
			if (is != null && is.booleanValue() && GLComponentWindow.draft) {
				if (triangles == null) {
					triangles = new BasicGeometry.TriangleMesh();
					triangles.rebuildVertex(0).rebuildTriangle(0);
					triangle = new DynamicMesh(triangles);
				}

				Boolean p = source.getProperty(python_isPaused);
				boolean paused = p != null && p;

				p = source.getProperty(SplineComputingOverride.noFrame);
				if (p != null && p) {
					ComponentDrawingUtils.drawRectangle(triangle, null, null, (float) bounds.x+2, (float) bounds.y+2, (float) 15, (float) 15, paused ? Constants.paused_execution_color : Constants.execution_color, null);
					triangles.performPass(null);
				} else {
					ComponentDrawingUtils.drawRectangle(triangle, null, null, (float) bounds.x, (float) bounds.y, (float) bounds.w, (float) bounds.h, paused ? Constants.paused_execution_color : Constants.execution_color, null);
					triangles.performPass(null);
				}
			}

			return super.paintNow(source, bounds, visible);
		}

	}

	static public class PythonTextualInformation {
		String uid;

		HashMap<String, Object> persistantVariables = new HashMap();
	}

	static public class UnacknowledgedError {
		transient Throwable cause;
		String when;
		VisualElementReference parent;
		VisualElementReference inside;
		VisualElementReference connectedTo;

		public UnacknowledgedError(String when, Throwable cause, iVisualElement inside, iVisualElement parent) {
			this.when = when;
			this.cause = cause;
			this.parent = parent == null ? null : new VisualElementReference(parent);
			this.inside = new VisualElementReference(inside);
		}

	}

	static public final String pluginId = "//plugin_python";

	static public final VisualElementProperty<String> python_source = new VisualElementProperty<String>("python_source_v");

	static public final VisualElementProperty<String> python_source_forExecution = new VisualElementProperty<String>("python_source_forExecution", python_source);

	static public final VisualElementProperty<Boolean> python_isExecuting = new VisualElementProperty<Boolean>("python_isExecuting_");
	static public final VisualElementProperty<Boolean> python_isPaused = new VisualElementProperty<Boolean>("python_isPaused_");

	static public final VisualElementProperty<Stack<UnacknowledgedError>> python_unacknowledgedError = new VisualElementProperty<Stack<UnacknowledgedError>>("python_unacknowledgedError");

	static public final VisualElementProperty<PythonPlugin> python_plugin = new VisualElementProperty<PythonPlugin>("python_plugin_");

	static public final VisualElementProperty<Globals> python_globals = new VisualElementProperty<Globals>("python_globals_");

	static public final VisualElementProperty<iFunction<String, String>> python_sourceFilter = new VisualElementProperty<iFunction<String, String>>("python_sourceFilter");

	static public final VisualElementProperty<Map<String, field.core.ui.text.rulers.ExecutedAreas.State>> python_areas = new VisualElementProperty<Map<String, field.core.ui.text.rulers.ExecutedAreas.State>>("python_areas");

	public static String externalPropertyNameToInternalName(String name) {
		return name;

		// if (name.endsWith("__")) {
		// name = name.substring(0, name.length() - 2) +
		// ".+";
		// } else if (name.endsWith("_") ||
		// name.startsWith("_menu"))
		// name = name + ".//";
		// else {
		// if (name.startsWith("_i"))
		// name = name.substring(2) + ".inspect";
		// }
		// return name;
	}

	static public Object getAttr(iVisualElement from, iVisualElement to, String name) {
		name = externalPropertyNameToInternalName(name);

		// ;//System.out.println(" get attr <"+from+" "+to+" "+name+">");

		VisualElementProperty<Object> n = new iVisualElement.VisualElementProperty<Object>(name);
		Ref<Object> r = new Ref<Object>(null);

		topology.begin(to);
		forward.getProperty.getProperty(to, n, r);
		topology.end(to);

		// ;//System.out.println(" returning <"+r.get()+">");
		return r.get();
	}

	static public Object getAttr(iVisualElement from, String name) {
		return getAttr(from, from, name);
	}

	static public Object getLocalProperty(iVisualElement of, String name) {
		name = externalPropertyNameToInternalName(name);
		VisualElementProperty<Object> n = new iVisualElement.VisualElementProperty<Object>(name);
		return of.getProperty(n);
	}

	public static String internalPropertyNameToExternalName(VisualElementProperty p) {
		String name = p.getName();
		// if (name.contains("."))
		// name = name.split("\\.")[0];
		//
		// if (p.containsSuffix("+")) {
		// name = name + "__";
		// }
		// if (p.containsSuffix("//")) {
		// name = name + "_";
		// }
		// if (p.containsSuffix("inspect")) {
		// name = "_i" + name;
		// }
		//
		return name;
	}

	static public List<String> listAttr(iVisualElement from, iVisualElement to) {

		final List<String> rr = new ArrayList<String>();

		new GraphNodeSearching.GraphNodeVisitor_depthFirst<iVisualElement>(true) {

			@Override
			protected VisitCode visit(iVisualElement from) {
				Map<Object, Object> q = from.payload();
				for (Map.Entry<Object, Object> e : q.entrySet()) {
					if (e.getKey() instanceof VisualElementProperty) {
						String n = internalPropertyNameToExternalName(((VisualElementProperty) e.getKey()));
						if (!rr.contains(n))
							rr.add(n);
					}

				}
				return VisitCode.cont;
			}

		}.apply(from);

		return rr;
	}

	static public void redraw(iVisualElement o) {
		o.setProperty(iVisualElement.dirty, true);
	}

	static public void setAttr(iVisualElement from, iVisualElement to, String name, Object value) {
		name = externalPropertyNameToInternalName(name);
		VisualElementProperty<Object> n = new iVisualElement.VisualElementProperty<Object>(name);
		n.set(to, to, value);

		// topology.begin(from);
		// backward.setProperty.setProperty(to, n, new
		// Ref<Object>(value));
		// topology.end(from);
	}

	static public void setAttr(iVisualElement to, String name, Object value) {
		name = externalPropertyNameToInternalName(name);
		VisualElementProperty<Object> n = new iVisualElement.VisualElementProperty<Object>(name);

		// topology.begin(to);
		// backward.setProperty.setProperty(to, n, new
		// Ref<Object>(value));
		// topology.end(to);

		n.set(to, to, value);

	}

	static public PyModule toolsModule;

	protected LocalVisualElement lve;

	protected SelectionGroup<iComponent> group;

	protected iVisualElement root;

	protected Stack<iVisualElement> stackOfPythonPositionablesExecuting = new Stack<iVisualElement>();

	HashMap<iVisualElement, Object> cachedAttributeAccess = new HashMap<iVisualElement, Object>();

	Stack<Object> attributeDicts = new Stack<Object>();

	Stack<CapturedEnvironment> environments = new Stack<CapturedEnvironment>();

	static public CapturedEnvironment ongoingEnvironment;

	Globals globals = new Globals();

	HashMap<String, PythonTextualInformation> database = new HashMap<String, PythonTextualInformation>();

	HashMap<iVisualElement, PythonScriptingSystem.Promise> promises = new HashMap<iVisualElement, PythonScriptingSystem.Promise>();

	Map<Object, Object> properties = new HashMap<Object, Object>();

	iVisualElementOverrides elementOverride;

	public PythonPlugin() {
		lve = new LocalVisualElement();
	}

	public void close() {
	}

	public Object getPersistanceInformation() {
		return new Pair<String, HashMap<String, PythonTextualInformation>>(pluginId + "version_1", database);
	}

	public iVisualElement getWellKnownVisualElement(String id) {
		if (id.equals(pluginId))
			return lve;
		return null;
	}

	public void registeredWith(iVisualElement root) {

		this.root = root;

		// PythonInterface.getPythonInterface().execString("from FluidTools import *");
		PythonInterface.getPythonInterface().execString("import FluidTools");
		PyObject module = (PyObject) PythonInterface.getPythonInterface().executeStringReturnRawValue("_fluidTools_module=FluidTools", "_fluidTools_module");
		toolsModule = (PyModule) module;
		lve.setProperty(python_plugin, this);
		lve.setProperty(python_globals, globals);
		// add a next
		// to root that
		// adds some
		// overrides
		root.addChild(lve);

		// register for
		// selection
		// updates? (no,
		// do it in
		// subclass)
		group = root.getProperty(iVisualElement.selectionGroup);

		elementOverride = createElementOverrides();

	}

	public void setPersistanceInformation(Object o) {
		if (o instanceof Pair) {
			Pair<String, HashMap<String, PythonTextualInformation>> p = (Pair<String, HashMap<String, PythonTextualInformation>>) o;
			if (p.left.equals(pluginId + "version_1")) {
				// database
				// =
				// p.right;
			}
		}
	}

	public void update() {
	}

	private iGlobalTrap globalTrapFor(iVisualElement element) {
		return globals.globalTrapFor(element);
	}

	protected CapturedEnvironment configurePythonEnvironment(iVisualElement element) {
		final Object rwas = PythonInterface.getPythonInterface().getVariable("_r");
		final Object swas = PythonInterface.getPythonInterface().getVariable("_self");
		PythonInterface.getPythonInterface().setVariable("_self", element);
		if (PythonInterface.getPythonInterface().getLanguage() == Language.python) {

			Object object = getAttributesForElement(element);
			attributeDicts.push(object);
			PythonInterface.getPythonInterface().setVariable("_a", object);

			toolsModule.__dict__.__setitem__("_self".intern(), Py.java2py(element));
		}
		CapturedEnvironment capenv = new CapturedEnvironment(element);
		PythonInterface.getPythonInterface().setVariable("_environment", capenv);

		final String modWas = PythonInterface.getPythonInterface().getModuleName();
		PythonInterface.getPythonInterface().setModuleName(element.getProperty(iVisualElement.name) + "[" + element.getUniqueID());

		capenv.exitHandler.put("restore _r", new iUpdateable() {
			public void update() {
				PythonInterface.getPythonInterface().setVariable("_r", rwas);
				PythonInterface.getPythonInterface().setVariable("_self", swas);
				PythonInterface.getPythonInterface().setModuleName(modWas);
			}
		});

		PythonInterface.getPythonInterface().setVariable("_r", null);

		PythonInterface.getPythonInterface().pushGlobalTrap(globalTrapFor(element));
		environments.push(capenv);
		ongoingEnvironment = capenv;
		return capenv;
	}

	protected void configurePythonPostEnvironment(iVisualElement element) {
		PythonInterface.getPythonInterface().popGlobalTrap();

		if (environments.size() > 0) {
			CapturedEnvironment was = environments.pop();
			was.runExit();
		}

		if (attributeDicts.size() > 0)
			attributeDicts.pop();

		if (attributeDicts.size() > 0)
			PythonInterface.getPythonInterface().setVariable("_a", attributeDicts.peek());

		if (environments.size() > 0)
			PythonInterface.getPythonInterface().setVariable("_environment", environments.peek());
		ongoingEnvironment = environments.size() > 0 ? environments.peek() : null;
	}

	protected iVisualElementOverrides createElementOverrides() {
		return new Overrides().setVisualElement(lve);
	}

	protected void deleteElement(iVisualElement element) {

		;//System.out.println(" delete element <" + element.getUniqueID() + "> database is <" + database + ">");

		database.remove(element.getUniqueID());
		Promise p = promises.get(element);
		Ref<PythonScriptingSystem> pss = new Ref<PythonScriptingSystem>(null);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(element).getProperty(element, PythonScriptingSystem.pythonScriptingSystem, pss);
		if (pss.get() != null)
			pss.get().revokePromise(element);
	}

	protected Object getAttributesForElement(iVisualElement element) {
		Object object = cachedAttributeAccess.get(element);
		if (object == null) {
			PythonInterface.getPythonInterface().setVariable("__f", element);
			PythonInterface.getPythonInterface().setVariable("__t", element);

			Object a = PythonInterface.getPythonInterface().executeStringReturnRawValue("__a = _self", "__a");
			cachedAttributeAccess.put(element, object = a);
		}
		return object;
	}

	protected void handleExceptionThrownDuringRunning(String when, iVisualElement element, iVisualElement parentElement, Throwable t) {

	}

	protected PythonTextualInformation informationFor(iVisualElement element) {

		PythonTextualInformation information = database.get(element.getUniqueID());

		if (information == null || promises.get(element) == null) {
			information = newPythonTextualInformation(element);
			database.put(element.getUniqueID(), information);
		}

		return information;
	}

	protected Promise newPromiseFor(iVisualElement element) {
		return new LocalPromise(element);
	}

	protected PythonTextualInformation newPythonTextualInformation(iVisualElement element) {
		PythonTextualInformation info = new PythonTextualInformation();
		info.uid = element.getUniqueID();

		promises.put(element, newPromiseFor(element));

		// tell any
		// scripting
		// interfrace

		PythonScriptingSystem pss = PythonScriptingSystem.pythonScriptingSystem.get(element);

		if (pss != null) {
			pss.promisePythonScriptingElement(element, promises.get(element));
		} else {
			System.err.println(ANSIColorUtils.red(" warning: no pss for element <" + element + "? ??"));
		}

		return info;
	}

	protected Promise promiseFor(iVisualElement element) {
		Promise p = promises.get(element);
		if (p == null) {
			promises.put(element, p = newPromiseFor(element));
		}
		return p;
	}

}
