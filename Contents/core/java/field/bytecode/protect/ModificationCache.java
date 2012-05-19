package field.bytecode.protect;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class ModificationCache {

	static public class Modification implements Serializable {
		boolean instrumented;
		long at;

		public Modification(boolean instrumented, long at) {
			super();
			this.instrumented = instrumented;
			this.at = at;
		}

	}

	HashMap<String, Modification> known = new LinkedHashMap<String, Modification>();

	public ModificationCache() {
		known = persist("instrumentedClassCache", new HashMap<String, Modification>());
	}

	public boolean is(String name, boolean def, long at) {
		Modification m = known.get(name);
		if (m == null)
		{
			//System.out.println(" cache miss :"+name+" "+at);
			return def;
		}
		if (m.at < at) {
			//System.out.println(" cache stale :"+name+" "+at+" > "+m.at);
			known.remove(name);
			return def;
		}
		//System.out.println(" cache hit :"+name+" "+m.instrumented);
		return m.instrumented;
	}
	
	public void state(String name, boolean is, long at)
	{
		//System.out.println(" marking <"+name+"> as <"+is+"> @ "+at);
		known.put(name, new Modification(is, at));
	}
	

	public long modificationForURL(URL u)
	{
		if (u==null) return 0;
		String external = u.toExternalForm();
		if (external.startsWith("file:"))
		{
			return new File(external.substring("file:".length())).lastModified();
		}
		if (external.startsWith("jar:"))
		{
			String e = external.substring("jar:file:".length(), external.indexOf("!"));
			return new File(e).lastModified();
		}
		System.out.println(" no modification date available from <"+u.toExternalForm()+">");
		return 0;
	}
	
	
	public <T> T persist(String name, T defaultValue) {
		T t = defaultValue;
		final String filename = System.getProperty("user.home") + "/Library/Application Support/Field" + "/" + name + ".xml";
		try {

			ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(filename))));
			t = (T) ois.readObject();
			ois.close();
		} catch (Throwable x) {
			x.printStackTrace();
			t = defaultValue;
		}

		if (t == null)
			t = defaultValue;

		final T ft = t;
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				try {
					System.err.println(" saving <"+known.size()+"> elements");
					ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(filename))));
					oos.writeObject(ft);
					oos.close();
				} catch (Throwable t) {
					new File(filename).delete();
				}
			}
		}));
		return ft;
	}
}
