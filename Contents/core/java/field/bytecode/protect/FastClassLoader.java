package field.bytecode.protect;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

public class FastClassLoader extends URLClassLoader {

	public FastClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
		super(urls, parent, factory);
	}

	public FastClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	public FastClassLoader(URL[] urls) {
		super(urls);
	}

	Map<String, String> a = new LinkedHashMap<String, String>();
	long maphash = -1;
	long loadedmaphash = -1;

	{
		final String filename = System.getProperty("user.home") + "/Library/Application Support/Field" + "/classmap.xml";
		try {

			ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(filename))));
			a = (Map<String, String>) ois.readObject();
			ois.close();
			String o = a.get("__maphash__");
			if (o != null) {
				loadedmaphash = Long.parseLong(o);
			}
			System.out.println(" loaded classmap with <" + a + ">");
		} catch (Throwable x) {
			x.printStackTrace();
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {

				try {
					a.put("__maphash__", "" + maphash);

					ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(filename))));
					oos.writeObject(a);
					oos.close();

					System.out.println(" wrote :" + a.size() + " classmap");

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	protected void addURL(URL url) {
		super.addURL(url);
		maphash = 31 * maphash + url.hashCode();
	}

	boolean skipped = false;

	@Override
	public URL findResource(String name) {
		if (maphash == loadedmaphash) {
			String q = a.get(name);
			if (q != null) {
				try {
					return new URL(q);
				} catch (MalformedURLException e) {
				}
			}

			if (a.containsKey(name))
				return null;
		} else {
			if (!skipped) {
				System.err.println("WARNING: skipping cache, classpath is not final or correct <" + maphash + " " + loadedmaphash + ">. This is completely benign, but starting Field might take longer than usual.");
				skipped = true;
			}
		}
		// long a = System.nanoTime();
		URL o = super.findResource(name);
		// long b = System.nanoTime();

		{
			this.a.put(name, o == null ? null : o.toString());
		}
		return o;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		System.out.println(" find class <" + name + ">");
		return super.findClass(name);
	}

	@Override
	protected String findLibrary(String libname) {
		System.out.println(" find library <" + libname + ">");
		return super.findLibrary(libname);
	}

	@Override
	public Enumeration<URL> findResources(String name) throws IOException {
		System.out.println(" find resources <" + name + ">");
		return super.findResources(name);
	}

}
