package field.core.ui.text.embedded;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import field.bytecode.protect.Woven;
import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.python.PythonPlugin;

@Woven
public class FreezeProperties {

	public class Freeze {
		private final boolean optimistic;

		Map<VisualElementProperty, Object> frozen = new HashMap<VisualElementProperty, Object>();

		public Freeze() {
			optimistic = true;
		}

		public Freeze(Set<String> include, Set<String> exclude, boolean optimistic) {
			this.optimistic = optimistic;
		}

		public Freeze freeze(VisualElement ele) {
			frozen.clear();
			Map<Object, Object> properties = ele.payload();
			Set<Entry<Object, Object>> es = properties.entrySet();
			for (Entry<Object, Object> e : es) {
				if (e.getKey() instanceof VisualElementProperty && ( ((VisualElementProperty)e.getKey()).isFreezable()) || (include!=null && include.contains(((VisualElementProperty) e.getKey()).getName()))) if (include == null || include.contains(((VisualElementProperty) e.getKey()).getName())) if (exclude == null || !exclude.contains(((VisualElementProperty) e.getValue()).getName())) {
					cloneInto(frozen, (VisualElementProperty) e.getKey(), e.getValue(), !optimistic);
				}
			}
			return this;
		}

		public Map<VisualElementProperty, Object> getMap() {
			return frozen;
		}

		public void thaw(VisualElement ele) {
			Set<Entry<VisualElementProperty, Object>> es = frozen.entrySet();
			for (Entry<VisualElementProperty, Object> e : es) {
				Object v = FreezeProperties.this.clone(e.getValue());
				if (v!=null)
					e.getKey().set(ele, ele, v);
			}
		}

	}

	public interface iCloneHelper {
		public boolean canClone(Object name, Class o);

		public Object clone(Object o);
	}

	static public void standardCloneHelpers(final FreezeProperties f) {
		f.helpers.add(new iCloneHelper(){
			public boolean canClone(Object name, Class o) {
				return String.class.isAssignableFrom(o);
			}

			public Object clone(Object o) {
				return o;
			}
		});
		f.helpers.add(new iCloneHelper(){
			public boolean canClone(Object name, Class o) {
				return ArrayList.class.isAssignableFrom(o);
			}

			public Object clone(Object o) {
				List<Object> r = f.cloneList((List) o, false);
				return r;
			}
		});
		f.helpers.add(new iCloneHelper(){
			public boolean canClone(Object name, Class o) {
				return Number.class.isAssignableFrom(o);
			}

			public Object clone(Object o) {
				return o;
			}
		});
		f.helpers.add(new iCloneHelper(){
			public boolean canClone(Object name, Class o) {
				return CachedLine.class.isAssignableFrom(o);
			}

			public Object clone(Object o) {
				return LineUtils.transformLine(((CachedLine) o), null, null, null, null);
			}
		});
	}

	List<iCloneHelper> helpers = new ArrayList<iCloneHelper>();

	Set<String> include;

	Set<String> exclude;

	public Object clone(Object o) {
		if (o == null)
			return null;
		else {
			iCloneHelper ch = getCloneFor(null, o.getClass());
			if (ch == null) {
				return null;

			} else {
				return ch.clone(o);
			}
		}
	}

	public <T> void cloneInto(Map<T, Object> cc, T key, Object o, boolean nothingOnFailure) {
		if (o == null)
			cc.put(key, null);
		else {
			iCloneHelper ch = getCloneFor(key, o.getClass());
			if (ch == null && nothingOnFailure) {

			} else if (ch == null) {
				cc.put(key, o);
			} else {
				cc.put(key, ch.clone(o));
			}
		}
	}

	public List<Object> cloneList(List<Object> q, boolean nothingOnFailure) {
		ArrayList<Object> cc = new ArrayList<Object>();
		for (Object e : q) {
			if (e == null)
				cc.add(null);
			else {
				iCloneHelper ch = getCloneFor(null, e.getClass());
				if (ch == null && nothingOnFailure) {

				} else if (ch == null) {
					cc.add(e);
				} else {
					cc.add(ch.clone(e));
				}
			}
		}
		return cc;
	}

	public <T> Map<T, Object> cloneMap(Map<T, Object> q, boolean nothingOnFailure) {
		HashMap<T, Object> cc = new HashMap<T, Object>();
		Set<Entry<T, Object>> name = q.entrySet();
		for (Entry<T, Object> e : name) {
			if (e.getValue() == null)
				cc.put(e.getKey(), null);
			else {
				iCloneHelper ch = getCloneFor(e.getKey(), e.getValue().getClass());
				if (ch == null && nothingOnFailure) {

				} else if (ch == null) {
					cc.put(e.getKey(), e.getValue());
				} else {
					cc.put(e.getKey(), ch.clone(e.getValue()));
				}
			}
		}
		return cc;
	}

	//@Cached(name = "cloneHelpers")
	public iCloneHelper getCloneFor(Object name, Class c) {
		for (iCloneHelper cc : helpers) {
			if (cc.canClone(name, c)) return cc;
		}
		return null;
	}

	public FreezeProperties setExclude(Set<String> exclude) {
		if (exclude != null) {
			this.exclude = new HashSet<String>();
			for (String s : exclude) {
				this.exclude.add(PythonPlugin.externalPropertyNameToInternalName(s));
			}
		}
		return this;
	}

	public FreezeProperties setInclude(Set<String> include) {
		if (include != null) {
			this.include = new HashSet<String>();
			for (String s : include) {
				this.include.add(PythonPlugin.externalPropertyNameToInternalName(s));
			}
		}
		return this;
	}

}
