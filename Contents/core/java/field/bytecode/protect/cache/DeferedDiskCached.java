/**
 *
 */
package field.bytecode.protect.cache;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

import field.bytecode.protect.BasicInstrumentation2;
import field.bytecode.protect.Protected;
import field.bytecode.protect.StandardTrampoline;
import field.bytecode.protect.annotations.CacheParameter;
import field.launch.SystemProperties;
import field.util.ANSIColorUtils;
import field.util.PythonUtils;

public final class DeferedDiskCached extends BasicInstrumentation2.DeferCallingFast {

	static public final String diskCacheRoot = SystemProperties.getDirProperty("deferedDiskCachedRoot", "/var/tmp/expCache/");

	private final HashMap<String, Object> parameters;

	Map<ImmutableArrayWrapper, Object> cache = new WeakHashMap<ImmutableArrayWrapper, Object>();

	List<Field> implicatedFields = null;

	java.lang.reflect.Method original = null;

	Map<ImmutableArrayWrapper, Object> strongCache = new HashMap<ImmutableArrayWrapper, Object>();

	public DeferedDiskCached(String name, int access, Method method, ClassVisitor delegate, MethodVisitor to, String signature, HashMap<String, Object> parameters) {
		super(name, access, method, delegate, to, signature, parameters);
		this.parameters = parameters;
	}

	@Override
	public Object handle(int fromName, Object fromThis, String originalMethod, Object[] argArray) {
		if (original == null) {
			java.lang.reflect.Method[] all = StandardTrampoline.getAllMethods(fromThis.getClass());
			for (java.lang.reflect.Method m : all) {
				if (m.getName().equals(originalMethod)) {
					original = m;
					break;
				}
			}
			assert original != null : originalMethod;

			original.setAccessible(true);

			String storageType = (String) parameters.get("storage");
			if (storageType != null && storageType.equals("strong")) {
				cache = strongCache;
			}
		}
		if (implicatedFields == null) {
			implicatedFields = new ArrayList<Field>();
			Field[] allFields = StandardTrampoline.getAllFields(fromThis.getClass());
			for (Field f : allFields) {

				CacheParameter ann = f.getAnnotation(CacheParameter.class);
				if (ann != null) {
					if ((ann.name() == null && parameters.get("name") == null) || ann.name().equals(parameters.get("name"))) {
						f.setAccessible(true);
						implicatedFields.add(f);
					}
				}
			}
		}

		Object[] na = null;

		if (implicatedFields.size() != 0) {
			na = new Object[argArray.length + implicatedFields.size()];
			System.arraycopy(argArray, 0, na, 0, argArray.length);
			for (int i = 0; i < implicatedFields.size(); i++)
				try {
					na[argArray.length + i] = implicatedFields.get(i).get(fromThis);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
		} else {
			na = argArray;
		}

		ImmutableArrayWrapper iaw = new ImmutableArrayWrapper(na, false);

		Object object = cache.get(iaw);
		if (object == null && !cache.containsKey(iaw)) {

			// now check the disk cache

			String string = iaw.toString();
			string = string.replace('<', '_');
			string = string.replace('>', '_');
			string = string.replace('$', '_');
			string = string.replace('[', '_');
			string = string.replace(']', '_');
			string = string.replace('/', '_');
			string = string.replace(" ", "");

			String name = (String) parameters.get("name");
			name = name == null ? "unknown" : name;
			name += "_" + fromThis.getClass();

			final String filename = diskCacheRoot + name + "/" + string;

			if (new File(filename).exists()) {
				Object ty = parameters.get("type");
				Object loaded = null;
				if (ty == null || ty.equals("xml")) {
					try {
						loaded = new PythonUtils().loadAsXML(filename);
					} catch (Throwable t) {
						;//System.out.println(" (( non fatal error while attempting to load (xml) cache from <" + filename + "> is ))");
						t.printStackTrace();
					}
				} else {
					loaded = new Protected().loadSerialized(filename);
				}

				if (loaded != null) {
					cache.put(iaw, loaded);
					return loaded;
				}
			} else {
			}

			try {
				cache.put(iaw, object = original.invoke(fromThis, argArray));
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}

			if (object != null) {
				new File(diskCacheRoot + name).mkdirs();
				Object ty = parameters.get("type");
				if (ty == null || ty.equals("xml")) {
					try {
						new PythonUtils().persistAsXML(object, filename);
					} catch (Throwable t) {
						System.err.println(" (( non fatal error while attempting to save (xml) cache from <" + filename + "> --- will not try again, " + ANSIColorUtils.yellow("WARNING: cache may be out of date") + "))");
						t.printStackTrace();
					}
				} else {
					try {
						ObjectOutputStream ois = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(filename))));
						ois.writeObject(object);
						ois.close();
					} catch (Throwable t) {
						System.err.println(" (( non fatal error while attempting to save (serializable) cache from <" + filename + "> is -- will not try again, " + ANSIColorUtils.yellow("WARNING: cache may be out of date") + "))");
						t.printStackTrace();
					}
				}

			}
		}
		return object;

	}
}