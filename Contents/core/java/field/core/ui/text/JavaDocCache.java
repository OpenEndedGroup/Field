package field.core.ui.text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.JavaClass;

public class JavaDocCache {
	
	static public JavaDocCache cache;
	
	public JavaDocCache() {
		cache = this;
	}
	
	JavaDocBuilder db = new JavaDocBuilder();
	private JavaClass[] classes = db.getClasses();

	static public final boolean debug = true;

	LinkedHashMap<JarFile, Map<String, JarEntry>> jarCache = new LinkedHashMap<JarFile, Map<String, JarEntry>>();

	public JavaClass getClass(Class c) {
		String cname = c.getName();
		if (debug)
			System.out.println(" looking for <" + cname + " : " + c + ">");
		for (JavaClass cc : classes) {
			if (debug)
				System.out.println(" name :" + cc.getFullyQualifiedName());
			if (cc.getFullyQualifiedName().equals(cname))
				return cc;
		}

		return null;
	}

	public void addJarFile(String name) throws IOException {

		if (debug)
			System.out.println(" add jar file <" + name + ">");

		for (JarFile j : jarCache.keySet())
			if (j.getName().equals(name))
				return;

		if (debug)
			System.out.println(" new jar file <" + name + ">");

		JarFile ff = new JarFile(name);
		Enumeration<JarEntry> entries = ff.entries();
		HashMap<String, JarEntry> map;
		jarCache.put(ff, map = new LinkedHashMap<String, JarEntry>());
		while (entries.hasMoreElements()) {
			JarEntry j = entries.nextElement();
			map.put(j.getName(), j);
		}

		// note, we don't actually close the jar file here
	}

	public JavaClass loadNameFromJar(String pathName, String className) throws IOException {
		Set<Entry<JarFile, Map<String, JarEntry>>> e = jarCache.entrySet();
		for (Entry<JarFile, Map<String, JarEntry>> ee : e) {
			Map<String, JarEntry> v = ee.getValue();

			JarEntry entry = v.get(pathName);
			if (debug)
				System.out.println(" looked in jar <" + ee.getKey().getName() + "> for <" + pathName + "> and found <" + entry + ">");

			if (entry == null) {
				// some jars (like the processing src jar) have
				// complicated prefix structure
				Collection<JarEntry> pre = v.values();
				for (JarEntry je : pre) {
					if (je.getName().endsWith(pathName)) {
						entry = je;
						if (debug)
							System.out.println(" found suffix <" + entry + ">");
						break;
					}
				}
				;
			}

			if (entry != null) {
				InputStreamReader reader = new InputStreamReader(ee.getKey().getInputStream(entry));
				db.addSource(reader);
				classes = db.getClasses();
				return db.getClassByName(className);
			}
		}
		return null;
	}

	LinkedHashSet<String> triedAndFailed = new LinkedHashSet<String>();

	public JavaClass loadFromFile(String pathName, String className) throws FileNotFoundException {
//		System.out.println(" looking for path <" + pathName + " / " + className + ">");

		if (!new File(pathName).exists())
			return null;
		db.addSource(new FileReader(pathName));
		// JavaClass c = db.getClassByName(className);
		JavaClass r = null;
		// classes = db.getClasses();

		r = db.getClassByName(className);

		int s = className.lastIndexOf('.');
		String maybeInner = className.subSequence(0, s)+"$"+className.substring(s+1);
		
//		System.out.println(" maybe inner "+maybeInner);
		JavaClass r2 = db.getClassByName(maybeInner);

		if (r2.getMethods().length>r.getMethods().length){
			r = r2;
		}
		
		
		
//		if (className.contains("$")) {
//			String matchName = className.substring(className.lastIndexOf(".") + 1);
//
//			for (JavaClass c : classes) {
//				if (c.getName().equals(matchName)) {
//					r = c;
//					break;
//				}
//			}
//		}

		if (r == null && !triedAndFailed.contains(className)) {
			String matchName = className.substring(className.lastIndexOf(".") + 1);

			for (JavaClass c : classes) {
				if (c.getName().equals(matchName)) {
					r = c;
					break;
				}
			}

			if (r == null)
				triedAndFailed.add(className);
		}

//		System.out.println(" found <"+(r!=null)+">");
		
		return r;
	}

}
