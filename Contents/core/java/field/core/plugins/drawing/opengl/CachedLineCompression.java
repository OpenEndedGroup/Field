package field.core.plugins.drawing.opengl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.python.core.PyObject;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import field.core.dispatch.iVisualElement;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.util.Dict;
import field.util.Dict.Prop;

public class CachedLineCompression {

	static class CompressedCachedLine implements Serializable {
		List<Dict> allProperties = new ArrayList<Dict>();

		List<Object[]> allArgs = new ArrayList<Object[]>();

		List<String> allMethods = new ArrayList<String>();

		Map<Prop, Object> properties = new LinkedHashMap<Prop, Object>();
	}

	static ByteArrayOutputStream bos = new ByteArrayOutputStream(1024 * 1024);

	static ByteArrayInputStream bis;

	public static Converter converter = new Converter() {

		boolean inside = false;

		public boolean canConvert(Class type) {
			return !inside && CachedLine.class.isAssignableFrom(type);
		}

		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			writer.startNode("compressedCachedLine");
			writer.setValue(CachedLineCompression.compress((CachedLine) source));
			writer.endNode();
		}

		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			try {
				reader.moveDown();
				String value = reader.getValue();
				reader.moveUp();
				Object dc = CachedLineCompression.decompress(value);
				return dc;
			} catch (Throwable t) {
				t.printStackTrace();
				return null;
			}
		}

	};

	public static String compress(CachedLine source) {
		bos.reset();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(cachedLineToCCL(source));
			oos.close();
			return new Base64().encode(bos.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
		assert false;
		return null;
	}

	public static Object decompress(String value) {
		byte[] r = new Base64().decode(value);
		bis = new ByteArrayInputStream(r);
		try {
			CompressedCachedLine ccl = (CompressedCachedLine) new ObjectInputStream(bis).readObject();
			return CCLToCachedLine(ccl);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		System.out.println(" -- got error, returning no line -- ");

		return new CachedLine();
	}

	private static Object cachedLineToCCL(CachedLine source) {
		CompressedCachedLine ccl = new CompressedCachedLine();

		for (Event e : source.events) {
			Dict s = scrub(e.attributes);
			ccl.allProperties.add(s);
			ccl.allArgs.add(e.args);
			ccl.allMethods.add(CachedLine.methodMap.get(e.method));
		}

		Map<Prop, Object> m = scrub(source.getProperties()).getMap();
		ccl.properties.putAll(m);

		return ccl;
	}

	private static Object CCLToCachedLine(CompressedCachedLine ccl) {
		CachedLine ret = new CachedLine();

		for (int i = 0; i < ccl.allProperties.size(); i++) {
			Event e = ret.new Event();
			e.args = ccl.allArgs.get(i);
			e.method = CachedLine.methodMap.getBackwards(ccl.allMethods.get(i));
			e.attributes = ccl.allProperties.get(i);
			ret.events.add(e);
		}

		ret.getProperties().getMap().putAll(ccl.properties);

		return ret;
	}

	private static Dict scrub(Dict attributes) {
		if (attributes == null)
			return null;
		Dict r = new Dict();
		Map<Prop, Object> m = attributes.getMap();

		outer: for (Map.Entry<Prop, Object> q : m.entrySet()) {

			if (q.getValue() instanceof iVisualElement)
				continue;

			if (!(q.getValue() instanceof Serializable)) {
			} else if (q.getValue() instanceof PyObject) {
			} else {
				if (q.getValue() instanceof Collection) {
					Iterator mm = ((Collection) q.getValue()).iterator();
					while (mm.hasNext())
						if (!(mm.next() instanceof Serializable)) {
							continue outer;
						}
				}

				r.put(q.getKey(), q.getValue());
			}
		}
		return r;
	}

}
