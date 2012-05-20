package field.core.plugins.history;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import field.core.dispatch.Mixins;
import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.Mixins.iMixinProxy;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.dispatch.iVisualElementOverrides.Ref;
import field.core.dispatch.iVisualElementOverrides.iDefaultOverride;
import field.core.execution.TemporalSliderOverrides;
import field.core.persistance.FluidCopyPastePersistence;
import field.core.plugins.drawing.SplineComputingOverride;
import field.core.plugins.python.OutputInsertsOnSheet;
import field.core.plugins.python.PythonPlugin;
import field.core.plugins.python.PythonPluginEditor;
import field.core.util.LoadInternalWorkspaceFile;
import field.core.windowing.components.iComponent;
import field.launch.SystemProperties;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.Generics.Pair;
import field.namespace.generic.Generics.Triple;

public class Templating {

	static public HashMap<String, VisualElementProperty<String>> shortForms = new HashMap<String, VisualElementProperty<String>>();
	static {
		shortForms.put("main", PythonPlugin.python_source);
		shortForms.put("update", SplineComputingOverride.onChange);
		shortForms.put("resize", SplineComputingOverride.onFrameChange);
		shortForms.put("select", SplineComputingOverride.onSelection);
		shortForms.put("tweak", SplineComputingOverride.tweak);
	}

	static public iVisualElement elementFromKnownTemplate(String name, iVisualElement root) throws IOException {
		File found = new LoadInternalWorkspaceFile().findTemplateCalled(name);
		if (found == null)
			return null;

		String canonicalPath = found.getCanonicalPath();

		if (canonicalPath.endsWith("/"))
			canonicalPath = canonicalPath.substring(0, canonicalPath.length() - 1);
		String[] split = canonicalPath.split("/");

		String uid = split[split.length - 1];
		String sheetname = split[split.length - 2];

		HashSet<iVisualElement> loaded = FluidCopyPastePersistence.copyFromNonloaded(Collections.singleton(uid), new File(canonicalPath).getParent() + "/sheet.xml", root, iVisualElement.copyPaste.get(root));

		for (iVisualElement e : loaded) {
			PythonPluginEditor.python_isTemplateHead.delete(e, e);
		}

		return loaded.iterator().next();
	}

	static public ArrayList<iVisualElement> elementsFromKnownSheet(String name, iVisualElement root) throws IOException {
		File sheetName = new File(SystemProperties.getDirProperty("versioning.dir") + name);
		if (!sheetName.exists())
			return null;

		HashSet<iVisualElement> loaded = FluidCopyPastePersistence.copyFromNonloaded(null, sheetName.getCanonicalPath() + "/sheet.xml", root, iVisualElement.copyPaste.get(root));

		return new ArrayList<iVisualElement>(loaded);
	}

	static public ArrayList<iVisualElement> elementsFromKnownSheetNoTimeslider(String name, iVisualElement root) throws IOException {
		File sheetName = new File(SystemProperties.getDirProperty("versioning.dir") + name);
		if (!sheetName.exists())
			return null;

		HashSet<iVisualElement> loaded = FluidCopyPastePersistence.copyFromNonloadedPredicate(new iFunction<Boolean, iVisualElement>() {
			public Boolean f(iVisualElement in) {

				iVisualElementOverrides i = in.getProperty(iVisualElement.overrides);
				if (i == null)
					return false;
				if (i instanceof TemporalSliderOverrides)
					return false;
				return true;

			}
		}, sheetName.getCanonicalPath() + "/sheet.xml", root, iVisualElement.copyPaste.get(root));

		return new ArrayList<iVisualElement>(loaded);
	}

	static public Pair<String, String> findTemplateInstance(final String templateName) {
		File dir = new File(SystemProperties.getDirProperty("versioning.dir"));
		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				try {
					if (pathname.isDirectory() && new File(pathname.getCanonicalPath() + "/sheet.xml").exists())
						return true;
				} catch (IOException e) {
				}
				return false;
			}
		});

		final Pair<String, String> ret = new Pair<String, String>(null, null);

		for (File f : files) {
			f.listFiles(new FileFilter() {

				public boolean accept(File pathname) {
					try {
						File ff = new File(pathname.getCanonicalPath() + "/python_isTemplate.+.property");
						if (pathname.isDirectory() && ff.exists()) {
							BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ff)));
							StringBuffer read = new StringBuffer();
							while (reader.ready()) {
								read.append(reader.readLine() + "\n");
							}
							reader.close();
							String name = (String) VersioningSystem.objectRepresentationFor(read.toString());

							if (name.trim().equals(templateName)) {

								String cp = pathname.getCanonicalPath();
								String[] split = cp.split("/");

								String uid = split[split.length - 1];
								String sheetname = split[split.length - 2];

								ret.left = uid;
								ret.right = pathname.getParentFile() + "/sheet.xml";
							}
						}
					} catch (IOException e) {
					}
					return false;
				}
			});
			if (ret.left != null)
				return ret;
		}
		return null;
	}

	static public void merge(VisualElement source, iVisualElement copy, boolean preferTemplate, boolean becomeVisual) throws IOException {

		LoadInternalWorkspaceFile liwf = new LoadInternalWorkspaceFile();

		Map<Object, Object> sourceP = source.payload();
		Map<Object, Object> m = copy.payload();
		Set<Entry<Object, Object>> es = m.entrySet();
		for (Entry<Object, Object> e : es) {
			VisualElementProperty key = (VisualElementProperty) e.getKey();
			if (key.containsSuffix("v")) {
				Object originalText = liwf.getOriginalText(copy, key);
				if (originalText instanceof String) {
					boolean c = liwf.diff3DoesConflict((String) e.getValue(), (String) originalText, (String) source.getProperty(key));
					if (c) {
						if (becomeVisual) {
							String o = liwf.performThreeWayMerge((String) e.getValue(), (String) source.getProperty(key), (String) originalText);
							key.set(copy, copy, o);
						} else {
							performCopy(source, (VisualElement) copy, key, source.getProperty(key));
						}
					} else {
						String o = liwf.performThreeWayMergeNonVisually((String) e.getValue(), (String) source.getProperty(key), (String) originalText);
						key.set(copy, copy, o);
					}
				} else {
					String o = liwf.performThreeWayMergeNonVisually((String) e.getValue(), (String) source.getProperty(key), (String) originalText);
					key.set(copy, copy, o);
				}
			} else if (preferTemplate && sourceP.containsKey(key)) {
				key.set(copy, copy, sourceP.get(key));
			}
		}

	}

	static public void newEditableProperty(String propertyName, iVisualElement inside) {
		VisualElementProperty p = new VisualElementProperty(propertyName);
		PythonPluginEditor.knownPythonProperties.put("Template \u2014 " + propertyName, p);
	}

	static public iVisualElement simpleCopy(VisualElement source, iVisualElement dispatchTo) {
		iVisualElementOverrides o = source.getProperty(iVisualElement.overrides);
		iComponent c = source.getProperty(iVisualElement.localView);

		Rect f = source.getFrame(null);
		f.x += 10;
		f.y += 10;

		Class oclass = o.getClass();
		List<iVisualElementOverrides> callList = null;

		if (o instanceof iMixinProxy) {
			;//System.out.println(" o is <" + o + ">");

			callList = ((iMixinProxy) o).getCallList();
			oclass = DefaultOverride.class;
		}

		Triple<VisualElement, iComponent, DefaultOverride> created = VisualElement.createWithName(f, dispatchTo, (Class<VisualElement>) source.getClass(), (Class<iComponent>) c.getClass(), (Class<iVisualElementOverrides.DefaultOverride>) oclass, iVisualElement.name.get(source) + " (copy)");

		if (callList != null) {
			iVisualElementOverrides[] over = new iVisualElementOverrides[callList.size()];
			for (int i = 0; i < over.length; i++) {
				try {
					over[i] = callList.get(i).getClass().newInstance();
				} catch (InstantiationException e1) {
					e1.printStackTrace();
				} catch (IllegalAccessException e1) {
					e1.printStackTrace();
				}
			}
			iVisualElementOverrides newOver = new Mixins().make(iVisualElementOverrides.class, Mixins.visitCodeCombiner, over);

			((iDefaultOverride) newOver).setVisualElement(created.left);
			created.left.setElementOverride(newOver);
		}

		Map<Object, Object> properties = source.payload();
		Set<Entry<Object, Object>> es = properties.entrySet();
		VisualElement newElement = created.left;
		for (Entry<Object, Object> e : es) {
			VisualElementProperty p = (VisualElementProperty) e.getKey();

			Object v = e.getValue();
			if (created.left.getProperty(p) == null && shouldCopy(p))
				performCopy(source, newElement, p, v);
		}

		return newElement;
	}

	/**
	 * TODO: this needs rearchitecting
	 */
	public static boolean shouldCopy(VisualElementProperty p) {
		;//System.out.println(" should copy ? "+p);
		if (p.equals(OutputInsertsOnSheet.outputInsertsOnSheet_knownComponents)) return false;
		if (p.equals(OutputInsertsOnSheet.outputInsertsOnSheet)) return false;

		return true;
	}

	public static void performCopy(VisualElement source, VisualElement newElement, VisualElementProperty p, Object v) {
		Ref<Object> r = new Ref<Object>(v);
		r.set(v, source);
		new iVisualElementOverrides.MakeDispatchProxy().getBackwardsOverrideProxyFor(newElement).setProperty(newElement, p, r);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(newElement).setProperty(newElement, p, r);
	}

}
