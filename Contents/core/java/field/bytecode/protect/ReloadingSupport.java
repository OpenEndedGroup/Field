package field.bytecode.protect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import field.namespace.generic.Bind.iFunction;

public class ReloadingSupport {

	static public class ChildClassLoader extends ClassLoader {
		public Map<String, byte[]> map;

		public ChildClassLoader(ClassLoader parent, Map<String, byte[]> map) {
			super(parent);
			this.map = map;
		}

		public ChildClassLoader(ClassLoader parent) {
			super(parent);
			this.map = new HashMap<String, byte[]>();
		}

		public void add(String name, byte[] bytes) {
			this.map.put(name, bytes);
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			if (should(name)) {
				byte[] b = map.get(name);
				return defineClass(name, b, 0, b.length);
			} else
				throw new ClassNotFoundException(name);
		}

		@Override
		public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			Class cls = findLoadedClass(name);
			if (cls == null) {
				if (should(name))
					cls = findClass(name);
				else
					cls = super.loadClass(name, false);
			}
			if (resolve)
				this.resolveClass(cls);
			return cls;
		}

		protected boolean should(String name) {
			return map.containsKey(name);
		}
	}

	public class ReloadingDomain {
		public iFunction<Boolean, String> matcher;

		public LinkedHashMap<String, Class> loaded = new LinkedHashMap<String, Class>();

		ChildClassLoader loader;

		public int priority;

		public void reload(iFunction<Object, Class> reloadHook) throws ClassNotFoundException {
			newLoader(this);
			Set<Entry<String, Class>> es = loaded.entrySet();
			for (Entry<String, Class> e : es) {
				String k = e.getKey();
				byte[] bytes = Trampoline2.trampoline.bytesForClass(Trampoline2.trampoline.getClassLoader(), k);

				loader.add(k, bytes);
			}
			for (Entry<String, Class> e : es) {
				Class<?> c = loader.loadClass(e.getKey(), true);
				reloadHook.f(c);
			}
		}

	}

	List<ReloadingDomain> domains = new ArrayList<ReloadingDomain>();

	public void addDomain(final String regex) {
		ReloadingDomain dom = new ReloadingDomain();

		final Pattern p = Pattern.compile(regex);
		dom.matcher = new iFunction<Boolean, String>() {

			@Override
			public Boolean f(String in) {
				return p.matcher(in).matches();
			}

			public String toString() {
				return regex;
			};
		};

		dom.priority = regex.length();
		
		synchronized (domains) {
			domains.add(dom);

			Collections.sort(domains, new Comparator<ReloadingDomain>() {

				@Override
				public int compare(ReloadingDomain arg0, ReloadingDomain arg1) {
					return arg0.priority < arg1.priority ? -1 : 	1;
				}
			});

		}
	}

	public Class delegate(String name, byte[] code) {

		synchronized (domains) {
			for (ReloadingDomain d : domains) {
				Boolean nn = d.matcher.f(name);

				if (nn) {
					Class cc = d.loaded.get(name);
					if (cc != null) {
						return cc;
					}

					if (d.loader == null) {
						newLoader(d);
					}
					d.loader.add(name, code);
					try {
						cc = d.loader.loadClass(name, true);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
					d.loaded.put(name, cc);
					return cc;

				}
			}
		}
		return null;
	}

	private void newLoader(final ReloadingDomain d) {
		d.loader = new ChildClassLoader(Trampoline2.trampoline.loader) {
			protected boolean should(final String name) {

				;//System.out.println(" -- checking to see if we should be able to reload:" + name);

				if (super.should(name))
					return true;

				Boolean m = d.matcher.f(name);
				if (m) {
					byte[] b = Trampoline2.trampoline.bytesForClass(Trampoline2.trampoline.getClassLoader(), name);
					map.put(name, b);
				}
				return false;
			};
		};

	}

	public List<ReloadingDomain> getDomains() {
		return domains;
	}

}
