package field.core.plugins.help;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.python.core.PyObject;

import sun.security.x509.UniqueIdentity;

import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.Type;

import field.bytecode.protect.annotations.HiddenInAutocomplete;
import field.core.Platform;
import field.core.ui.text.JavaDocCache;
import field.core.ui.text.PythonTextEditor;
import field.math.abstraction.iProvider;

public class ObjectToMarkDown {

	public Map<String, SoftReference> forward = new HashMap<String, SoftReference>();
	public Map<Object, String> backward = new WeakHashMap<Object, String>();
	static public Map<String, String> browsed = new LinkedHashMap<String, String>() {
		protected boolean removeEldestEntry(Map.Entry<String, String> arg0) {
			return (this.size() > 10);

		};
	};

	static public Map<String, iProvider<String>> invokeMap = new LinkedHashMap<String, iProvider<String>>() {
		protected boolean removeEldestEntry(Map.Entry<String, field.math.abstraction.iProvider<String>> arg0) {
			return this.size() > 100;
		};
	};

	public ObjectToMarkDown() {

	}

	int q = 0;

	public String convert(Object o) {

		;//System.out.println(" object to markdown --- <" + o + ">");

		if (o == null)
			return "Cannot find object";

		if (o instanceof PyObject)
			o = ((PyObject) o).__tojava__(Object.class);

		if (o instanceof Collection)
			return convertCollection((Collection) o);

		if (o instanceof Map)
			return convertMap((Map) o);

		Class<? extends Object> c = o.getClass();

		String s = "";

		s += "<h1>" + o + "</h1>\n";
		s += "Object is of class *" + o.getClass().getName() + "*, its ";
		s += "unique id is *" + System.identityHashCode(o.getClass()) + "*\n\n";

		JavaClass cc = resolveJavaClass(c);

		;//System.out.println(" get class is <" + cc + ">");

		if (cc != null)
			if (cc.getComment() != null && cc.getComment().trim().length() > 0)
				s += "\n" + cc.getComment() + "\n";

		q = 0;
		for (Field f : o.getClass().getFields()) {
			HiddenInAutocomplete a = f.getAnnotation(HiddenInAutocomplete.class);
			if (a != null)
				continue;

			try {
				Object n = f.get(o);

				if (f.getType().isPrimitive() && n == null) {
					s += "#### <a href=\"#" + q + "\">" + f.getName() + "</a> = " + n + "<sub> (*" + trim("" + f.getType()) + "*) </sub>\n";
				} else {
					String uniq = makeUniq(n);
					s += "#### <a href=\"#" + q + "\">" + f.getName() + "</a> = <a href=\"http://localhost:10010/otmd/" + uniq + "\">" + n + "</a><sub> (*" + trim("" + f.getType()) + "*) </sub>\n";
				}
				if (n != null & cc != null) {
					s += "\n[fold]\n\n";
					JavaField fbm = cc.getFieldByName(f.getName());
					if (fbm != null && fbm.getComment() != null)
						s += "> " + fbm.getComment().replace("\n", "\n> ") + "\n";
					s += "\n[/fold]\n\n";
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			q++;
		}

		for (Method f : o.getClass().getMethods()) {
			HiddenInAutocomplete a = f.getAnnotation(HiddenInAutocomplete.class);
			if (a != null)
				continue;

			try {
				s += getCompletionFor(o, o.getClass(), f, "");

			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			q++;
		}

		;//System.out.println(" -- returning <" + s + ">");

		return s;
	}

	private JavaClass resolveJavaClass(Class<? extends Object> c) {
		JavaClass cc = JavaDocCache.cache.getClass(c);

		if (cc == null) {
			String name = c.getName();
			String fname = name;

			if (c.getName().contains("$")) {
				fname = c.getName().substring(0, c.getName().indexOf("$"));
			}

			String[] sd = PythonTextEditor.getSourceDirs();
			for (int n = 0; n < sd.length; n++) {
				try {
					if (sd[n].endsWith(".jar")) {
						JavaDocCache.cache.addJarFile(sd[n]);
						String p2 = fname.replace(".", "/") + ".java";
						String p1 = "src/" + p2;
						;//System.out.println(" trying :" + p1);
						cc = JavaDocCache.cache.loadNameFromJar(p1, name);
						if (cc != null)
							break;
						cc = JavaDocCache.cache.loadNameFromJar(p2, name);
						;//System.out.println(" trying :" + p2);
						if (cc != null)
							break;

					} else {
						cc = JavaDocCache.cache.loadFromFile(sd[n] + "/" + fname.replace(".", "/") + ".java", name);
						if (cc != null)
							break;
					}
				} catch (Exception e) {
					;//System.out.println(" resource is <" + sd[n] + ">");
					e.printStackTrace();
				}
			}

		}
		return cc;
	}

	private String convertMap(Map o) {
		String s = "";

		s += "<h1>Map (" + o.size() + " element" + (o.size() == 1 ? "" : "s") + ")</h1>\n";
		s += "Object is of class *" + o.getClass().getName() + "*, its ";
		s += "unique id is *" + System.identityHashCode(o.getClass()) + "*\n\n";

		int m = Math.min(100, o.size());
		Iterator<Map.Entry> ii = ((Map) o).entrySet().iterator();
		for (int i = 0; i < m; i++) {
			Map.Entry nn = ii.next();
			String uniq = makeUniq(nn.getValue());
			s += nn.getKey() + " -> <a href=\"http://localhost:10010/otmd/" + uniq + "\">" + nn.getValue() + "</a>\n\n";
		}

		return s;

	}

	private String convertCollection(Collection o) {
		String s = "";

		s += "<h1>Collection (" + o.size() + " element" + (o.size() == 1 ? "" : "s") + ")</h1>\n";
		s += "Object is of class *" + o.getClass().getName() + "*, its ";
		s += "unique id is *" + System.identityHashCode(o.getClass()) + "*\n\n";

		int m = Math.min(100, o.size());
		Iterator ii = o.iterator();
		for (int i = 0; i < m; i++) {
			Object nn = ii.next();
			String uniq = makeUniq(nn);
			s += "[" + i + "] = <a href=\"http://localhost:10010/otmd/" + uniq + "\">" + nn + "</a>\n\n";
		}

		return s;
	}

	String makeUniq(Object n) {
		String q = backward.get(n);
		if (q != null) {
			forward.put(q, new SoftReference(n));
			return q;
		} else {
			q = (n == null ? "" : ("" + System.identityHashCode(n)));
			backward.put(n, q);
			forward.put(q, new SoftReference(n));
			return q;
		}
	}

	public String trim(String m) {
		if (m.lastIndexOf('.') == -1)
			return m;
		return m.substring(Math.max(m.lastIndexOf('$'), m.lastIndexOf('.')) + 1);
	}

	static int stringUrlUniq = 0;

	static public String urlForString(String m) {
		stringUrlUniq++;
		browsed.put("" + stringUrlUniq, m);
		return "http://localhost:10010/browsed/" + stringUrlUniq;
	}

	private String getCompletionFor(final Object ret, Class<? extends Object> class1, final Method m, final String right) {

		String ss = "";

		String parameterNames = Arrays.asList(m.getParameterTypes()).toString();
		parameterNames = parameterNames.substring(1, parameterNames.length() - 1);

		// ss = "#### "+strip(m.getName()) + "(" +
		// (m.getParameterTypes().length == 0 ? "" : parameterNames) +
		// ")\n";
		ss = "";
		Class<?> declaringClass = m.getDeclaringClass();
		String name = Platform.getCanonicalName(declaringClass);
		if (name != null) {

			JavaClass jc = resolveJavaClass(class1);

			if (jc != null) {
				Type[] types = new Type[m.getParameterTypes().length];
				for (int i = 0; i < m.getParameterTypes().length; i++) {
					String typeName = Platform.getCanonicalName(m.getParameterTypes()[i]).replace("[", "").replace("]", "");

					if (m.getParameterTypes()[i].isMemberClass()) {
						typeName = m.getParameterTypes()[i].getName();

						typeName = typeName.substring(m.getParameterTypes()[i].getDeclaringClass().getName().length() + 1);

						;//System.out.println("parameter type is member class <" + m.getParameterTypes()[i] + "> names are <" + typeName + "> <" + m.getParameterTypes()[i].getName() + "> <" + Platform.getCanonicalName(m.getParameterTypes()[i]) + ">");

						typeName = m.getParameterTypes()[i].getName();

						if (typeName.contains("$")) {
							String root = typeName.substring(0, typeName.indexOf("$"));

							;//System.out.println(" root = " + root + " " + typeName + " " + name);

							if (root.equals(name)) {
								;//System.out.println(" is inside class ");
							} else {
								;//System.out.println(" is outside class");
								typeName = typeName.replace("$", ".");
							}
						}
					}

					types[i] = new Type(typeName, m.getParameterTypes()[i].isArray() ? 1 : 0);
				}
				JavaMethod method = jc.getMethodBySignature(m.getName(), types);

				if (method != null) {
					if (method.getParameters() != null) {

						String ctext = "<b>" + m.getName() + "</b>" + " ( ";
						for (int i = 0; i < method.getParameters().length; i++) {
							ctext += strip(Platform.getCanonicalName(m.getParameterTypes()[i])) + " <b><i>" + method.getParameters()[i].getName() + "</i></b>" + (i != method.getParameters().length - 1 ? ", " : "");
						}
						ctext += " )";

						Class<?> tt = m.getReturnType();
						if (tt.equals(Void.class) || tt.getName().toLowerCase().equals("void")) {

						} else {
							ctext += " &rarr; " + strip(tt.getName());
						}

						String comment = method.getComment();
						if (comment == null)
							comment = "";

						ss = "#### <a href='#" + q + "'> " + ctext + "</a>";

						if (method.getParameters().length == 0) {

							String mapName = "___invoke_" + q + "__";

							invokeMap.put(mapName, new iProvider<String>() {
								@Override
								public String get() {
									Object q;
									try {
										q = m.invoke(ret);
										return HelpBrowser.textFromMarkdown(convert(q));
									} catch (IllegalArgumentException e) {
										e.printStackTrace();
										return convert(e);
									} catch (IllegalAccessException e) {
										e.printStackTrace();
										return convert(e);
									} catch (InvocationTargetException e) {
										e.printStackTrace();
										return convert(e);
									}
								}
							});

							ss += "  <a href='/field/invoke/" + mapName + "' style='float:right'>(perform)</a>";
						}

						ss += "\n[fold]\n\n";

						ss += comment + "\n";
						ss += method.getSourceCode().replace("\n", "\n\t");
						ss += "\n[/fold]\n\n";
						q++;
					} else {
						;//System.out.println(" does this ever happen ? ");
					}
				} else {
					;//System.out.println(" couldn't find method by signature <" + m.getName() + "> <" + Arrays.asList(types) + "> <" + m.isBridge() + " " + m.isSynthetic() + ">");
					;//System.out.println("methods are <" + Arrays.asList(jc.getMethods()) + ">");
				}
			}
		}

		return ss;
	}

	private String strip(Method m) {
		Class<?>[] types = m.getParameterTypes();
		String s = "";
		for (int i = 0; i < types.length; i++) {
			s += strip(types[i].getName());
			if (i < types.length - 1)
				s += ", ";
		}
		return s;
	}

	private String strip(String name) {

		if (name.indexOf('.') != -1) {
			String[] sp = name.split("\\.");

			if (sp[sp.length - 1].contains("$")) {
				sp = sp[sp.length - 1].split("$");
			}

			return sp[sp.length - 1];
		}

		name = name.replaceAll("[<>]", "");

		return name;
	}

}
