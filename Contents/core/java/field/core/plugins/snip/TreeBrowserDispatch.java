package field.core.plugins.snip;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.python.core.PyFunction;

import field.core.execution.PythonInterface;
import field.core.util.PythonCallableMap;
import field.core.windowing.GLComponentWindow;
import field.graphics.core.BasicSceneList;
import field.graphics.windowing.FullScreenCanvasSWT;
import field.math.abstraction.iHasScalar;
import field.namespace.generic.Bind.iFunction;
import field.util.HashMapOfLists;

public class TreeBrowserDispatch {

	static final TreeBrowserDispatch childrenOf = new TreeBrowserDispatch();
	static final TreeBrowserDispatch doubleClick = new TreeBrowserDispatch();
	static final TreeBrowserDispatch textFor = new TreeBrowserDispatch();
	static final TreeBrowserDispatch selectionText = new TreeBrowserDispatch();

	HashMapOfLists<Class, PythonCallableMap> dispatch = new HashMapOfLists<Class, PythonCallableMap>();

	static {
		textFor.register(Object.class, "tostring", new iFunction<Object, Object>() {
			@Override
			public Object f(Object in) {
				Method m;
				
				String name = PythonInterface.getPythonInterface().getLocalDictionary().findReverse(in);
				
				try {
					m = in.getClass().getMethod("toString");
					Class<?> c = m.getDeclaringClass();
					if (c.equals(Object.class)) {
						String n = in.getClass().getName();
						String[] n2 = n.split("\\.");
						n = n2[n2.length - 1];
						return (name==null ? "" : ("<b>"+name+"</b> "))+"(" + n + ")";
					}
					System.out.println(" custom tostring for <"+in.getClass()+"> <"+m+">");
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return (name==null ? "" : ("<b>"+name+"</b> ")) + in;
			}
		});

		childrenOf.register(Object.class, "list", new iFunction<Object, Object>() {

			@Override
			public Object f(Object in) {

				System.out.println(" -- " + in + " -- :" + (in instanceof Iterable));

				if (in instanceof Iterable) {
					List<Object> o = new ArrayList<Object>();
					int num = 0;
					Iterator ii = ((Iterable) in).iterator();
					while (ii.hasNext() && num < 40) {
						o.add(ii.next());
						num++;
					}
					return o;
				}
				return null;
			}
		});

		childrenOf.register(Object.class, "sc", new iFunction<Object, Object>() {
			@Override
			public Object f(Object in) {

				System.out.println(" sc " + in);

				if (in instanceof GLComponentWindow) {
					ArrayList<Object> a = new ArrayList<Object>();
					a.add(((GLComponentWindow) in).getSceneList());
					return a;
				} else if (in instanceof FullScreenCanvasSWT) {
					ArrayList<Object> a = new ArrayList<Object>();
					a.add(((FullScreenCanvasSWT) in).getSceneList());
					return a;
				} else if (in instanceof BasicSceneList) {
					ArrayList<Object> a = new ArrayList<Object>();
					a.addAll(((BasicSceneList) in).getChildren());
					return a;
				}
				return null;
			}
		});
	}

	public void register(Class c, String name, iFunction<Object, Object> f) {
		List<PythonCallableMap> m = dispatch.getList(c);
		if (m == null) {
			dispatch.put(c, m = new ArrayList<PythonCallableMap>());
		}
		if (m.size() == 0) {
			m.add(new PythonCallableMap());
		}

		m.get(0).register(name, f);
	}

	public void register(Class c, PyFunction f) {
		List<PythonCallableMap> m = dispatch.getList(c);
		if (m == null) {
			dispatch.put(c, m = new ArrayList<PythonCallableMap>());
		}
		if (m.size() == 0) {
			m.add(new PythonCallableMap());
		}

		m.get(0).register(f);
	}

	public Object call(Object... args) {
		List<Class> l = linearize(args[0] == null ? Object.class : args[0].getClass());
		for (Class cc : l) {
			Collection<PythonCallableMap> o = dispatch.get(cc);
			if (o == null)
				continue;
			if (o.size() == 0)
				continue;
			for (PythonCallableMap map : o) {
				Object out = map.invoke(args);
				if (out != null)
					return out;
			}
		}
		return null;
	}

	public List<Object> gather(Object... args) {
		List<Object> r = new ArrayList<Object>();
		List<Class> l = linearize(args[0] == null ? Object.class : args[0].getClass());
		for (Class cc : l) {
			Collection<PythonCallableMap> o = dispatch.get(cc);
			if (o == null)
				continue;
			if (o.size() == 0)
				continue;
			for (PythonCallableMap map : o) {
				Collection<Object> out = map.gather(args);
				if (out != null)
					r.addAll(out);
			}
		}
		return r;
	}

	private List<Class> linearize(Class<? extends Object> c) {
		LinkedHashSet<Class> tot = new LinkedHashSet<Class>();
		while (c != null) {
			tot.add(c);
			for (Class cc : c.getInterfaces())
				tot.add(cc);
			c = c.getSuperclass();
		}
		return new ArrayList<Class>(tot);
	}

}
