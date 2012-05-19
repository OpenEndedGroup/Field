package field.graph;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import field.core.util.ExecuteCommand;
import field.math.linalg.Vector2;
import field.namespace.generic.Bind.iFunction;

public class DotUtil {

	static Pattern p = Pattern.compile("(.*?)\\[.*?pos=\"(.*?)\"");

	static public HashMap<String, Vector2> readDot(String file) throws IOException {

		HashMap<String, Vector2> r = new HashMap<String, Vector2>();

		BufferedReader reader = new BufferedReader(new FileReader(new File(file)));
		while (reader.ready()) {
			String s = reader.readLine();
			Matcher m = p.matcher(s);
			if (m.find()) {
				String n = m.group(1);
				if (n.contains("--"))
					continue;
				String location = m.group(2);
				String[] d = location.split(",");

				System.out.println(" read <" + n + " " + location + "> -> " + d[0] + " " + d[1]);

				Vector2 v = new Vector2(Float.parseFloat(d[0].trim()), Float.parseFloat(d[1].trim()));

				r.put(n.trim(), v);

			}
		}

		reader.close();

		return r;
	}

	static public void writeDot(String file, Collection nodes, iFunction<Collection<Object>, Object> connected, iFunction<String, Object> names) throws IOException {

		BufferedWriter w = new BufferedWriter(new FileWriter(new File(file)));

		boolean first = true;

		w.append("graph G {\n");
		for (Object n : nodes) {
			String nn = names.f(n);
			for (Object m : connected.f(n)) {
				String mm = names.f(m);

				w.append(nn + " -- " + mm + ";\n");
			}
		}

		w.append("}\n");
		w.close();
	}

	static public HashMap<String, Vector2> dot(String executable, Collection nodes, iFunction<Collection<Object>, Object> connected, iFunction<String, Object> names) throws IOException {
		String fn = File.createTempFile("field", ".dot").getAbsolutePath();
		String fnOut = File.createTempFile("field", "_out.dot").getAbsolutePath();

		System.out.println(" dotting :" + fn + " " + fnOut);

		writeDot(fn, nodes, connected, names);
		new ExecuteCommand(".", new String[] { executable, "-Tdot", fn, "-o", fnOut }, true).waitFor();
		return readDot(fnOut);
	}

}
