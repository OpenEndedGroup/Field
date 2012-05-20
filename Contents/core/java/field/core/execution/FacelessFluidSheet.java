package field.core.execution;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.widgets.Event;

import field.bytecode.protect.Trampoline2;
import field.core.StandardFluidSheet;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.execution.PythonScriptingSystem.Promise;
import field.core.persistance.FluidPersistence;
import field.core.plugins.SimpleConstraints;
import field.core.plugins.iPlugin;
import field.core.plugins.autoexecute.AutoExecutePythonPlugin;
import field.core.plugins.history.VersioningSystem;
import field.core.plugins.pseudo.PseudoPropertiesPlugin;
import field.core.plugins.python.PythonPlugin;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.abstraction.iFloatProvider;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.graph.NodeImpl;
import field.math.graph.iMutableContainer;
import field.util.Dict.Prop;
import field.util.PythonUtils;

public class FacelessFluidSheet implements iVisualElementOverrides, iUpdateable{

	public class RootSheetElement extends NodeImpl<iVisualElement> implements iVisualElement {

		public <T> void deleteProperty(VisualElementProperty<T> p) {
			rootProperties.remove(p);
		}

		public void dispose() {
		}

		public Rect getFrame(Rect out) {
			return null;
		}

		public <T> T getProperty(iVisualElement.VisualElementProperty<T> p) {
			if (p == overrides)
				return (T) FacelessFluidSheet.this;

			Object o = rootProperties.get(p);
			return (T) o;
		}

		public String getUniqueID() {
			return StandardFluidSheet.rootSheetElement_uid;
		}

		public Map<Object, Object> payload() {
			return rootProperties;
		}

		public void setFrame(Rect out) {
		}

		public iMutableContainer<Map<Object, Object>, iVisualElement> setPayload(Map<Object, Object> t) {
			return this;
		}

		public <T> iVisualElement setProperty(iVisualElement.VisualElementProperty<T> p, T to) {
			rootProperties.put(p, to);
			return this;
		}

		public void setUniqueID(String uid) {
		}

		@Override
		public String toString() {
			return "standardFluidSheet root element";
		}

	}

	private final RootSheetElement rootSheetElement;
	private final FluidPersistence persistence;
	private final PythonScriptingSystem pss;
	private final BasicRunner basicRunner;

	protected HashMap<Object, Object> rootProperties = new HashMap<Object, Object>();

	protected VersioningSystem vs;


	List<iPlugin> plugins = new ArrayList<iPlugin>();

	public FacelessFluidSheet()
	{

		rootSheetElement = new RootSheetElement();

		rootSheetElement.setProperty(iVisualElement.enclosingFrame, null);
		rootSheetElement.setProperty(iVisualElement.localView, null);
		rootSheetElement.setProperty(iVisualElement.sheetView, null);
		rootSheetElement.setProperty(iVisualElement.selectionGroup, null);

		persistence = new FluidPersistence(new FluidPersistence.iWellKnownElementResolver() {
			public iVisualElement getWellKnownElement(String uid) {
				if (uid.equals(StandardFluidSheet.rootSheetElement_uid))
					return rootSheetElement;
				for (iPlugin p : plugins) {
					iVisualElement ve = p.getWellKnownVisualElement(uid);
					if (ve != null) {
						return ve;
					}
				}
				return null;
			}
		},1);

		pss = new PythonScriptingSystem();
		basicRunner = new BasicRunner(pss, 0)
		{
			@Override
			protected boolean filter(Promise p) {
				iVisualElement v = (iVisualElement) system.keyForPromise(p);

				return iExecutesPromise.promiseExecution.get(v) == this;
			}
		};


		rootSheetElement.setProperty(PythonScriptingSystem.pythonScriptingSystem, pss);
		rootSheetElement.setProperty(iExecutesPromise.promiseExecution, basicRunner);
		rootSheetElement.setProperty(BasicRunner.basicRunner, basicRunner);

	}

	public VisitCode added(iVisualElement newSource) {
		return VisitCode.cont;
	}

	public void addToSheet(iVisualElement newSource) {
		newSource.addChild(rootSheetElement);
		new iVisualElementOverrides.MakeDispatchProxy().getBackwardsOverrideProxyFor(newSource).added(newSource);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(newSource).added(newSource);
	}

	public VisitCode beginExecution(final iVisualElement source) {

		// should be lookup to support remoting
		Ref<PythonScriptingSystem> refPss = new Ref<PythonScriptingSystem>(null);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(source).getProperty(source, PythonScriptingSystem.pythonScriptingSystem, refPss);
		assert refPss.get() != null;

		Ref<iExecutesPromise> refRunner = new Ref<iExecutesPromise>(null);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(source).getProperty(source, iExecutesPromise.promiseExecution, refRunner);
		assert refRunner.get() != null;
		System.err.println(" runner in faceless is is <" + refRunner.get() + ">");

		Promise promise = refPss.get().promiseForKey(source);
		if (promise != null) {
			System.err.println(" promise isn't null <" +promise+  ">");
			refRunner.get().addActive(new iFloatProvider() {

				public float evaluate() {
					return 0;
				}

			}, promise);
		}
		return VisitCode.cont;
	}

	public VisitCode deleted(iVisualElement source) {
		source.getProperty(iVisualElement.localView);
		return VisitCode.cont;
	}

//	 implementation of iVisualElementOverrides

	public <T> VisitCode deleteProperty(iVisualElement source, VisualElementProperty<T> prop) {
		if (source == rootSheetElement) {
			VisualElementProperty<T> a = prop.getAliasedTo();
			while (a != null) {
				prop = a;
				a = a.getAliasedTo();
			}

			rootSheetElement.deleteProperty(prop);
		}
		return VisitCode.cont;
	}

	public VisitCode endExecution(iVisualElement source) {

		Ref<PythonScriptingSystem> refPss = new Ref<PythonScriptingSystem>(null);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(source).getProperty(source, PythonScriptingSystem.pythonScriptingSystem, refPss);
		assert refPss.get() != null;

		Ref<iExecutesPromise> refRunner = new Ref<iExecutesPromise>(null);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(source).getProperty(source, iExecutesPromise.promiseExecution, refRunner);
		assert refRunner.get() != null;

		Promise p = refPss.get().promiseForKey(source);
		if (p != null) {
			refRunner.get().removeActive(p);
		}

		return VisitCode.cont;
	}

	public <T> VisitCode getProperty(iVisualElement source, iVisualElement.VisualElementProperty<T> property, Ref<T> ref) {

//		;//System.out.println(" root prop faceless <"+rootProperties+"> / <"+property+">");

		if (rootProperties.containsKey(property)) {
			VisualElementProperty<T> a = property.getAliasedTo();
			while (a != null) {
				property = a;
				a = a.getAliasedTo();
			}

			//System.err.println(" ref. get< "+ref.get()+"> <"+rootProperties.get(property)+">");
			// major change

			if (ref.get() == null)
				ref.set((T) rootProperties.get(property));

			// return VisitCode.stop;
		}
		return VisitCode.cont;
	}

	public iVisualElement getRoot() {
		return rootSheetElement;
	}

	public VisitCode handleKeyboardEvent(iVisualElement newSource, Event event) {
		return VisitCode.cont;
	}

	public VisitCode inspectablePropertiesFor(iVisualElement source, List<Prop> properties) {
		return VisitCode.cont;
	}

	public VisitCode isHit(iVisualElement source, Event event, Ref<Boolean> is) {
		return VisitCode.cont;
	}

	public void load(Reader reader) {
		LinkedHashSet<iVisualElement> created = new LinkedHashSet<iVisualElement>();

		ObjectInputStream objectInputStream = persistence.getObjectInputStream(reader, created);
		try {
			String version = (String) objectInputStream.readObject();
			//assert version.equals("version_1") : version;
			iVisualElement oldRoot = (iVisualElement) objectInputStream.readObject();


			assert oldRoot == rootSheetElement : oldRoot;
			while (true) {
				Object persistanceInformation = objectInputStream.readObject();
				for (iPlugin p : plugins) {
					p.setPersistanceInformation(persistanceInformation);
				}
			}

		} catch (EOFException e) {
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		// was:
		// for (iVisualElement ve : created) {
		// this.added(ve);
		// }

		for (iVisualElement ve : created) {
			new iVisualElementOverrides.MakeDispatchProxy().getBackwardsOverrideProxyFor(ve).added(ve);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(ve).added(ve);
		}
	}

	public VisitCode menuItemsFor(iVisualElement source, Map<String, iUpdateable> items) {
		return VisitCode.cont;
	}

	public VisitCode paintNow(iVisualElement source, Rect bounds, boolean visible) {
		return VisitCode.cont;
	}

	public VisitCode prepareForSave() {
		return VisitCode.cont;
	}

	public FacelessFluidSheet registerPlugin(iPlugin plugin) {

		plugins.add(plugin);

		plugin.registeredWith(this.rootSheetElement);

		return this;
	}

	public void save(Writer writer) {

		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(rootSheetElement).prepareForSave();
		new iVisualElementOverrides.MakeDispatchProxy().getBackwardsOverrideProxyFor(rootSheetElement).prepareForSave();

		Set<iVisualElement> saved = new HashSet<iVisualElement>();
		ObjectOutputStream objectOutputStream = persistence.getObjectOutputStream(writer, saved);

		try {
			objectOutputStream.writeObject("version_1");
			objectOutputStream.writeObject(rootSheetElement);
			for (iPlugin p : plugins) {
				Object persistanceInformation = p.getPersistanceInformation();
				objectOutputStream.writeObject(persistanceInformation);
			}
			objectOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void registerExtendedPlugins(final FacelessFluidSheet sheet) {
		HashSet<String> p = Trampoline2.plugins;
		;//System.out.println(" extended plugins are <" + p + ">");
		for (String s : p) {
			;//System.out.println("   loading plugin <" + s + ">");
			try {
				Class<?> loaded = sheet.getClass().getClassLoader().loadClass(s);
				iPlugin instance = (iPlugin) loaded.newInstance();
				sheet.registerPlugin(instance);
			} catch (ClassNotFoundException e) {
				;//System.out.println("   error loading plugin <" + s + ">, continuing");
				e.printStackTrace();
			} catch (InstantiationException e) {
				;//System.out.println("   error loading plugin <" + s + ">, continuing");
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				;//System.out.println("   error loading plugin <" + s + ">, continuing");
				e.printStackTrace();
			} catch (Throwable t) {
				;//System.out.println("   error loading plugin <" + s + ">, continuing");
				t.printStackTrace();
			}
		}
	}

	public <T> VisitCode setProperty(iVisualElement source, iVisualElement.VisualElementProperty<T> property, Ref<T> to) {
		if (rootProperties.containsKey(property) || source == getRoot())
		{
			VisualElementProperty<T> a = property.getAliasedTo();
			while (a != null) {
				property = a;
				a = a.getAliasedTo();
			}
			rootProperties.put(property, to.get());
			// return VisitCode.stop;
		}

		return VisitCode.cont;
	}

	public VisitCode shouldChangeFrame(iVisualElement source, Rect newFrame, Rect oldFrame, boolean now) {
		return VisitCode.cont;
	}

	public void standard(String filename)
	{

		this.registerPlugin(new PythonPlugin());
		SimpleConstraints constraints = new SimpleConstraints();
		this.registerPlugin(constraints);
		this.registerPlugin(new AutoExecutePythonPlugin());

		this.registerPlugin(new PseudoPropertiesPlugin());

		//this.registerPlugin(new RemotePlugin());
		//this.registerPlugin(new ReferencePlugin());

		PythonInterface.getPythonInterface().setVariable("T", Launcher.mainInstance);

		new PythonUtils().install();

		registerExtendedPlugins(this);
		
		try {
			this.load(new BufferedReader(new FileReader(filename)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void update() {

		for (iPlugin p : plugins)
			p.update();

		basicRunner.update(0);

	}
}
