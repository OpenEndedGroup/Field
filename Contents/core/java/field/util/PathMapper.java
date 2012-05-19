package field.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import field.namespace.generic.Generics.Pair;

public class PathMapper {

	static public class Mapping {
		String prefix;
		String becomes;

		public Mapping(String prefix, String becomes) {
			super();
			this.prefix = prefix;
			this.becomes = becomes;
		}

	}

	List<Mapping> mappings = new ArrayList<Mapping>();
	List<Pair<String, String>> mapped = new ArrayList<Pair<String, String>>();

	public PathMapper() {

	}

	public void addMapping(String from, String to) {
		mappings.add(new Mapping(from, to));
	}

	public String map(String f) {
		for (Mapping m : mappings) {
			if (f.startsWith(m.prefix)) {
				f = m.becomes + f.substring(m.prefix.length());
			}
		}
		return f;

	}

	public File map(File ff) throws IOException {
		String f = ff.getCanonicalPath();
		for (Mapping m : mappings) {
			if (f.startsWith(m.prefix)) {
				f = m.becomes + f.substring(m.prefix.length());
			}
		}
		return new File(f);
	}

}
