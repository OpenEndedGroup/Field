package field.core.plugins.pseudo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.python.core.Py;

import field.core.StandardFluidSheet;
import field.core.dispatch.FastVisualElementOverridesPropertyCombiner;
import field.core.dispatch.FastVisualElementOverridesPropertyCombiner.iCombiner;
import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.dispatch.iVisualElementOverrides.Ref;
import field.core.execution.PythonInterface;
import field.core.plugins.BaseSimplePlugin;
import field.core.plugins.history.HGVersioningSystem;
import field.core.plugins.history.VersioningSystem;
import field.core.plugins.python.Action;
import field.core.plugins.python.PythonPlugin;
import field.core.util.FieldPyObjectAdaptor.iCallable;
import field.core.util.FieldPyObjectAdaptor.iHandlesAttributes;
import field.core.util.FieldPyObjectAdaptor.iHandlesFindItem;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.graph.iMutable;
import field.util.WorkspaceDirectory;

public class PseudoPropertiesPlugin extends BaseSimplePlugin {

	// static
	// {
	// FieldPyObjectAdaptor2.isHandlesAttributes(Finder.class);
	// FieldPyObjectAdaptor2.isHandlesAttributes(SuperPropertier.class);
	// FieldPyObjectAdaptor2.isHandlesAttributes(Wherer.class);
	// FieldPyObjectAdaptor2.isHandlesFindItem(Finder.class);
	// }

	public class Finder implements iHandlesAttributes, iHandlesFindItem {

		private final List<iVisualElement> all;

		public Finder(List<iVisualElement> all) {
			this.all = all;
		}

		public Object getAttribute(String name) {
			return Py.None;
		}

		public Object getItem(Object object) {

			;//System.out.println(" get item in finder called <" + object + ">");

			if (object == null || object == Py.None)
				return all;
			String r = object.toString();
			List<iVisualElement> found = StandardFluidSheet.findVisualElementWithNameExpression(root, r);
			PythonInterface.getPythonInterface().setVariable("__tmpFinderValue", found);

			return PythonInterface.getPythonInterface().eval("wl(__tmpFinderValue)");
		}

		public void setAttribute(String name, Object value) {
		}

		public void setItem(Object name, Object value) {
		}

		@Override
		public String toString() {
			return "\u2014\u2014 lets you find elements by name. For example <b>_self.find['something.*else']</b> \u2014\u2014";
		}
	}

	public class Framer {

		private final iVisualElement on;
		private Rect r;

		public Framer(iVisualElement on, Rect r) {
			this.on = on;
			this.r = r;
		}

		public double getH() {
			r = on.getFrame(null);
			if (r==null) return 0;
			return r.h;
		}

		public double getW() {
			r = on.getFrame(null);
			if (r==null) return 0;
			return r.w;
		}

		public double getX() {
			r = on.getFrame(null);
			if (r==null) return 0;
			return r.x;
		}

		public double getY() {
			r = on.getFrame(null);
			if (r==null) return 0;
			return r.y;
		}

		public void setH(double x) {
			Rect was = new Rect(0, 0, 0, 0).setValue(r);
			r.h = x;
			on.getProperty(iVisualElement.overrides).shouldChangeFrame(on, r, was, true);
			on.setProperty(iVisualElement.dirty, true);
		}

		public void setW(double x) {
			Rect was = new Rect(0, 0, 0, 0).setValue(r);
			r.w = x;
			on.getProperty(iVisualElement.overrides).shouldChangeFrame(on, r, was, true);
			on.setProperty(iVisualElement.dirty, true);
		}

		public void setX(double x) {
			Rect was = new Rect(0, 0, 0, 0).setValue(r);
			r.x = x;
			on.getProperty(iVisualElement.overrides).shouldChangeFrame(on, r, was, true);
			on.setProperty(iVisualElement.dirty, true);
		}

		public void setY(double x) {
			Rect was = new Rect(0, 0, 0, 0).setValue(r);
			r.y = x;
			on.getProperty(iVisualElement.overrides).shouldChangeFrame(on, r, was, true);
			on.setProperty(iVisualElement.dirty, true);
		}

		@Override
		public String toString() {
			return "rectangle <b>" + on.getFrame(null) + "</b>";
		}
	}

	public class PropertyInjectionOverride extends DefaultOverride {

		@Override
		public <T> VisitCode getProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> ref) {
			if (!properties.contains(prop))
				return super.getProperty(source, prop, ref);

			if (prop.equals(frame))
				return getFrame(source, ref);
			if (prop.equals(subelements))
				return getSubelements(source, ref);
			if (prop.equals(superelements))
				return getSuperelements(source, ref);
			if (prop.equals(root_))
				return getRoot(source, ref);
			if (prop.equals(collect))
				return getCollector(source, ref);
			if (prop.equals(all))
				return getAll(source, ref);
			if (prop.equals(find))
				return getFinder(source, ref);
			if (prop.equals(where))
				return getWherer(source, ref);
			if (prop.equals(superproperties))
				return getSuperpropertier(source, ref);
			if (prop.equals(begin))
				return getBeginner(source, ref);
			if (prop.equals(end))
				return getEnder(source, ref);
			if (prop.equals(action))
				return getActioner(source, ref);

			if (prop.equals(dataFolder))
				return getDataFolder(source, (Ref<String>) ref);
			if (prop.equals(workspaceFolder))
				return getWorkspaceFolder(source, (Ref<String>) ref);
			if (prop.equals(sheetFolder))
				return getSheetFolder(source, (Ref<String>) ref);
			if (prop.equals(sheetDataFolder))
				return getSheetDataFolder(source, (Ref<String>) ref);

			return super.getProperty(source, prop, ref);
		}

	}

	static public VisitCode getDataFolder(iVisualElement source, Ref<String> r) {

		String w = WorkspaceDirectory.dir[0];
		String d = w + "/data";
		File dataDir = new File(d);
		if (!dataDir.exists()) {
			boolean made = dataDir.mkdir();
			VersioningSystem vs = StandardFluidSheet.versioningSystem.get(source);
			if (vs != null) {
				((HGVersioningSystem) vs).scmAddFile(dataDir);
			}
		}

		String p = dataDir.getAbsolutePath();
		if (!p.endsWith("/"))
			p = p + "/";
		r.set(p);

		return VisitCode.stop;
	}

	public VisitCode getWorkspaceFolder(iVisualElement source, Ref<String> r) {
		String w = WorkspaceDirectory.dir[0];
		if (!w.endsWith("/"))
			w = w + "/";
		r.set(w);
		return VisitCode.stop;
	}

	static public VisitCode getSheetFolder(iVisualElement source, Ref<String> r) {
		VersioningSystem vs = StandardFluidSheet.versioningSystem.get(source);
		if (vs != null) {
			r.set(vs.getSheetPathName().replace("sheet.xml", "") + "/");
		}
		return VisitCode.stop;
	}

	static public VisitCode getSheetDataFolder(iVisualElement source, Ref<String> r) {
		getSheetFolder(source, r);
		String w = r.get();
		String d = w + "/data";
		File dataDir = new File(d);
		if (!dataDir.exists()) {
			boolean made = dataDir.mkdir();
			VersioningSystem vs = StandardFluidSheet.versioningSystem.get(source);
			if (vs != null) {
				((HGVersioningSystem) vs).scmAddFile(dataDir);
			}
		}

		String p = dataDir.getAbsolutePath();
		if (!p.endsWith("/"))
			p = p + "/";
		r.set(p);
		return VisitCode.stop;
	}

	public class SuperPropertier implements iHandlesAttributes {

		private final iVisualElement on;

		public SuperPropertier(iVisualElement on) {
			this.on = on;
		}

		public Object getAttribute(String name) {
			return new VisualElementProperty(name).getAbove(on);
		}

		public void setAttribute(String name, Object value) {
			// todo, throw error ?
		}

		public void setItem(Object name, Object value) {
			// todo, throw error ?
		}

		@Override
		public String toString() {
			return "\u2014\u2014 Lets you access properties, while skipping the local level,  for example: <b>_self.superproperties.someProperty_</b> \u2014\u2014";
		}

	}

	public class Wherer implements iHandlesAttributes {

		private final iVisualElement from;

		public Wherer(iVisualElement from) {
			this.from = from;
		}

		public Object getAttribute(String name) {
			String n = PythonPlugin.externalPropertyNameToInternalName(name);
			VisualElementProperty v = new VisualElementProperty(n);
			Ref ref = v.getRef(from);
			return ref.getStorageSource();
		}

		public void setAttribute(String name, Object value) {
		}

		public void setItem(Object name, Object value) {
		}

		@Override
		public String toString() {
			return "\u2014\u2014 Doesn't return the property, returns the visual element that actually has the property stored.  for example: <b>_self.where.someProperty_</b> \u2014\u2014";
		}
	}

	public class Collector implements iHandlesAttributes {

		private final iVisualElement from;
		FastVisualElementOverridesPropertyCombiner<Object, List<Object>> combine = new FastVisualElementOverridesPropertyCombiner<Object, List<Object>>(false) {
			protected java.util.Collection<? extends iVisualElement> sort(java.util.List<? extends field.math.graph.iMutable<iVisualElement>> parents) {

				ArrayList<iMutable<iVisualElement>> a = new ArrayList<field.math.graph.iMutable<iVisualElement>>(parents);
				Collections.sort(a, new Comparator<iMutable<iVisualElement>>() {

					@Override
					public int compare(iMutable<iVisualElement> o1, iMutable<iVisualElement> o2) {
						if (o1==null || ((iVisualElement)o1).getFrame(null)==null) return -1;
						if (o2==null || ((iVisualElement)o2).getFrame(null)==null) return 1;
						return Double.compare(((iVisualElement) o1).getFrame(null).y, ((iVisualElement) o2).getFrame(null).y);
					}
				});
				return (Collection	) a;
			};
		};

		public Collector(iVisualElement from) {
			this.from = from;
		}

		public Object getAttribute(String name) {
			String n = PythonPlugin.externalPropertyNameToInternalName(name);

			List<Object> x = combine.getProperty(from, new VisualElementProperty<Object>(n), new iCombiner<Object, List<Object>>() {

				@Override
				public List<Object> unit() {
					return new ArrayList<Object>();
				}

				@Override
				public List<Object> bind(List<Object> t, Object u) {
					if (u!=null)
						t.add(u);
					return t;
				}
			});

			// if (x.size()>0 && x.get(0) instanceof iVisualElement)
			// {
			// Collections.sort(x, new Comparator(){
			//
			// @Override
			// public int compare(Object arg0, Object arg1) {
			// if (arg0 instanceof iVisualElement && arg1 instanceof
			// iVisualElement)
			// {
			// return Double.compare(
			// ((iVisualElement)arg0).getFrame(null).y,
			// ((iVisualElement)arg1).getFrame(null).y);
			// }
			// return 0;
			// }});
			// }

			return x;
		}

		public void setAttribute(String name, Object value) {
		}

		public void setItem(Object name, Object value) {
		}

		@Override
		public String toString() {
			return "\u2014\u2014 Doesn't return the property, returns the visual element that actually has the property stored.  for example: <b>_self.where.someProperty_</b> \u2014\u2014";
		}
	}

	public class Actioner implements iHandlesAttributes {

		private final iVisualElement from;

		public Actioner(iVisualElement from) {
			this.from = from;
		}

		public Object getAttribute(String name) {

			VisualElementProperty<Action> v = new VisualElementProperty<Action>(name);
			Action m = v.get(from);

			if (m == null) {
				v.set(from, from, m = new Action());
			}

			return m;
		}

		public void setAttribute(String name, Object value) {
		}

		public void setItem(Object name, Object value) {
		}

		@Override
		public String toString() {
			return "Creates actions on demand";
		}
	}

	public class Subelements implements iHandlesFindItem, List<iVisualElement> {

		protected final iVisualElement from;

		List<iVisualElement> current;

		public Subelements(iVisualElement from) {
			this.from = from;
			refresh();
		}

		protected void refresh() {
			current = (List<iVisualElement>) from.getParents();
		}

		@Override
		public String toString() {
			return current + " (a list (and a map) of elements that delegate to this element. You can add and delete thrings from the list to change the delegation tree of Field)";
		}

		public boolean add(iVisualElement e) {
			connect(e);
			refresh();
			return true;
		}

		public void add(int index, iVisualElement element) {

			connect(element);
			refresh();
		}

		protected void connect(iVisualElement element) {
			element.addChild(from);
		}

		public boolean addAll(Collection<? extends iVisualElement> c) {
			for (iVisualElement v : c)
				add(v);
			return true;
		}

		public boolean addAll(int index, Collection<? extends iVisualElement> c) {
			for (iVisualElement v : c)
				add(v);
			return true;
		}

		public void clear() {
			for (iVisualElement v : new ArrayList<iVisualElement>(current))
				disconnect(v);
		}

		protected void disconnect(iVisualElement v) {
			v.removeChild(from);
		}

		public boolean contains(Object o) {
			return current.contains(o);
		}

		public boolean containsAll(Collection<?> c) {
			return current.contains(c);
		}

		public iVisualElement get(int index) {
			return current.get(index);
		}

		public int indexOf(Object o) {
			return current.indexOf(o);
		}

		public boolean isEmpty() {
			return current.isEmpty();
		}

		public Iterator<iVisualElement> iterator() {
			return current.iterator();
		}

		public int lastIndexOf(Object o) {
			return current.lastIndexOf(o);
		}

		public ListIterator<iVisualElement> listIterator() {
			return current.listIterator();
		}

		public ListIterator<iVisualElement> listIterator(int index) {
			return current.listIterator(index);
		}

		public boolean remove(Object o) {
			disconnect((iVisualElement) o);
			refresh();
			return true;
		}

		public iVisualElement remove(int index) {
			iVisualElement r = (iVisualElement) current.get(index);
			disconnect((iVisualElement) current.get(index));
			refresh();
			return r;
		}

		public boolean removeAll(Collection<?> c) {
			for (Object o : c)
				remove(o);
			return true;
		}

		public boolean retainAll(Collection<?> c) {
			throw new IllegalStateException(" not implemented");
		}

		public iVisualElement set(int index, iVisualElement element) {
			iVisualElement a = current.get(index);
			remove(a);
			add(index, element);
			return a;
		}

		public int size() {
			return current.size();
		}

		public List<iVisualElement> subList(int fromIndex, int toIndex) {
			return current.subList(fromIndex, toIndex);
		}

		public Object[] toArray() {
			return current.toArray();
		}

		public <T> T[] toArray(T[] a) {
			return current.toArray(a);
		}

		public Object getItem(Object object) {
			if (object instanceof Number)
				return get(((Number) object).intValue());
			for (iVisualElement e : current) {
				String nn = e.getProperty(iVisualElement.name);
				if (nn != null && nn.equals(object))
					return e;
			}
			return null;
		}

		public void setItem(Object name, Object value) {
			throw new IllegalStateException(" can't call set item to change topology ");
		}

		public Object getAttribute(String name) {
			return getItem(name);
		}

		public void setAttribute(String name, Object value) {
			throw new IllegalStateException(" can't call set item to change topology ");
		}

		public List<iVisualElement> values() {
			return this;
		}

	}

	public class Superelements extends Subelements {

		public Superelements(iVisualElement from) {
			super(from);
		}

		@Override
		protected void refresh() {
			current = (List<iVisualElement>) from.getChildren();
		}

		@Override
		public String toString() {
			return current + "(a list (and a map) of elements this element delegates to . You can add and delete thrings from the list to change the delegation tree of Field)";
		}

		protected void connect(iVisualElement element) {
			from.addChild(element);
		}

		@Override
		protected void disconnect(iVisualElement v) {
			from.removeChild(v);
		}

	}

	// static
	// {
	// FieldPyObjectAdaptor2.isCallable(Ender.class);
	// FieldPyObjectAdaptor2.isCallable(Beginner.class);
	// }

	public class Ender implements iCallable {
		private final iVisualElement source;

		public Ender(iVisualElement source) {
			this.source = source;
		}

		public Object call(Object[] args) {
			if (args.length == 0) {
				iVisualElement old = iVisualElementOverrides.topology.setAt(source);
				iVisualElementOverrides.forward.endExecution.endExecution(source);
				iVisualElementOverrides.topology.setAt(old);
			}
			return Py.None;
		}

		@Override
		public String toString() {
			return "\u2014\u2014 if you 'call' this property that box will stop running. For example <b>_self.find['something'].end()</b> \u2014\u2014";
		}
	}

	public class Beginner implements iCallable {
		private final iVisualElement source;

		public Beginner(iVisualElement source) {
			this.source = source;
		}

		public Object call(Object[] args) {
			if (args.length == 0) {
				iVisualElement old = iVisualElementOverrides.topology.setAt(source);
				iVisualElementOverrides.forward.beginExecution.beginExecution(source);
				iVisualElementOverrides.topology.setAt(old);
			}
			return Py.None;
		}

		@Override
		public String toString() {
			return "\u2014\u2014 if you 'call' this property that box will start running. For example <b>_self.find['something'].begin()</b> \u2014\u2014";
		}
	}

	static public final VisualElementProperty<Rect> frame = new VisualElementProperty<Rect>("frame");
	static public final VisualElementProperty<Map<String, iVisualElement>> subelements = new VisualElementProperty<Map<String, iVisualElement>>("subelements");
	static public final VisualElementProperty<Map<String, iVisualElement>> superelements = new VisualElementProperty<Map<String, iVisualElement>>("superelements");
	static public final VisualElementProperty<iVisualElement> root_ = new VisualElementProperty<iVisualElement>("root");
	static public final VisualElementProperty<iVisualElement> all = new VisualElementProperty<iVisualElement>("all");
	static public final VisualElementProperty<Object> find = new VisualElementProperty<Object>("find");
	static public final VisualElementProperty<List> collect = new VisualElementProperty<List>("collect");
	static public final VisualElementProperty<Wherer> where = new VisualElementProperty<Wherer>("where");
	static public final VisualElementProperty<Object> superproperties = new VisualElementProperty<Object>("superproperties");
	static public final VisualElementProperty<Beginner> begin = new VisualElementProperty<Beginner>("begin");
	static public final VisualElementProperty<Ender> end = new VisualElementProperty<Ender>("end");

	static public final VisualElementProperty<Actioner> action = new VisualElementProperty<Actioner>("action");

	static public final VisualElementProperty<String> dataFolder = new VisualElementProperty<String>("dataFolder");
	static public final VisualElementProperty<String> workspaceFolder = new VisualElementProperty<String>("workspaceFolder");
	static public final VisualElementProperty<String> sheetDataFolder = new VisualElementProperty<String>("sheetDataFolder");
	static public final VisualElementProperty<String> sheetFolder = new VisualElementProperty<String>("sheetFolder");

	static public final LinkedHashSet<VisualElementProperty> properties = new LinkedHashSet<VisualElementProperty>(Arrays.asList(new VisualElementProperty[] { frame, subelements, superelements, root_, all, find, where, superproperties, begin, end, dataFolder, workspaceFolder, sheetDataFolder, sheetFolder, action, collect }));

	public VisitCode getAll(iVisualElement source, Ref t) {
		t.set(StandardFluidSheet.allVisualElements(root));
		return VisitCode.stop;
	}

	public VisitCode getFinder(iVisualElement source, Ref t) {
		t.set(new Finder(StandardFluidSheet.allVisualElements(root)));
		return VisitCode.stop;
	}

	public VisitCode getFrame(iVisualElement source, Ref t) {
		t.set(new Framer(source, source.getFrame(null)));
		return VisitCode.stop;
	}

	public VisitCode getCollector(iVisualElement source, Ref t) {
		t.set(new Collector(source));
		return VisitCode.stop;
	}

	public VisitCode getRoot(iVisualElement source, Ref t) {
		t.set(root);
		return VisitCode.stop;
	}

	public VisitCode getSubelements(iVisualElement source, Ref t) {
		t.set(new Subelements(source));
		return VisitCode.stop;
	}

	public VisitCode getSuperelements(iVisualElement source, Ref t) {
		t.set(new Superelements(source));
		return VisitCode.stop;
	}

	private Map<String, iVisualElement> buildMap(List<iVisualElement> children) {
		HashMap<String, iVisualElement> m = new HashMap<String, iVisualElement>();
		for (iVisualElement v : children) {
			m.put(v.getProperty(iVisualElement.name), v);
		}
		return m;
	}

	public VisitCode getSuperpropertier(iVisualElement source, Ref t) {
		t.set(new SuperPropertier(source));
		return VisitCode.stop;
	}

	public VisitCode getWherer(iVisualElement source, Ref t) {
		t.set(new Wherer(source));
		return VisitCode.stop;
	}

	public VisitCode getBeginner(iVisualElement source, Ref t) {
		t.set(new Beginner(source));
		return VisitCode.stop;
	}

	public VisitCode getEnder(iVisualElement source, Ref t) {
		t.set(new Ender(source));
		return VisitCode.stop;
	}

	public VisitCode getActioner(iVisualElement source, Ref t) {
		t.set(new Actioner(source));
		return VisitCode.stop;
	}

	@Override
	protected String getPluginNameImpl() {
		return "pseudoproperties";
	}

	@Override
	protected DefaultOverride newVisualElementOverrides() {
		return new PropertyInjectionOverride();
	}

}
