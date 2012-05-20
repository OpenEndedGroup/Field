package field.graphics.jfbxlib;

import java.util.LinkedHashMap;
import java.util.Map;

public class MultiLoader {

	Map<String, Loader2> loaders = new LinkedHashMap<String, Loader2>();
	Map<String, String> resources = new LinkedHashMap<String, String>();

	public MultiLoader() {

	}

	static public String prefix(String name)
	{
		return name.split("/")[0];
	}

	static public String suffix(String name)
	{
		return name.split("/")[1];
	}

	public Loader2 get(String name) {
		
		;//;//System.out.println(" -- inside get <"+name+">");
		
		if (name.contains("/")) name = prefix(name);
		
		Loader2 loaded = loaders.get(name);

		if (loaded == null) {
			String m = resources.get(name);
			if (m == null)
				throw new NullPointerException("can't find resource called <" + name + ">");

			;//;//System.out.println(" -- loading <" + name + "> from <" + m + ">");

			Loader2 loader = new Loader2(m);
			loaders.put(name, loader);

			return loader;
		}

		return loaded;
	}

	public void declare(String name, String file) {
		resources.put(name, file);
	}

}
