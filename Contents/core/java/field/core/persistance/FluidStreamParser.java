package field.core.persistance;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.Sun14ReflectionProvider;
import com.thoughtworks.xstream.core.ReferenceByIdMarshallingStrategy;

import field.namespace.generic.Generics.Pair;
import field.util.ChannelSerializer;
import field.util.FloatBufferSerializer;
import field.util.MarkerSerializer;
import field.util.PythonUtils;

public class FluidStreamParser {

	private DocumentBuilder builder;

	private Document document;

	HashMap<String, HashMap<String, Object>> properties = new LinkedHashMap<String, HashMap<String, Object>>();

	private final File file;
	public boolean debug = false;

	public boolean loadProperties = false;

	public FluidStreamParser(File file) {
		this(file, false);
	}

	public FluidStreamParser(File file, boolean loadProperties) {
		this.file = file;
		this.loadProperties = loadProperties;

		System.err.println(" reading <" + file + ">");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		try {
			builder = factory.newDocumentBuilder();

			builder.setEntityResolver(new EntityResolver() {
				public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
					return new InputSource(new ByteArrayInputStream(new byte[] {}));
				}
			});

		} catch (ParserConfigurationException e) {
		}
		try {

			;//System.out.println(" about to prse <" + file + ">");

			StringBuffer ddoc = new StringBuffer(1024 * 1024);
			BufferedReader r = new BufferedReader(new FileReader(file));
			while (r.ready()) {
				ddoc.append(r.readLine() + "\n");
			}

			String doc = ddoc.toString().replace((char) 0x0b, ' ');

			document = builder.parse(new InputSource(new StringReader(doc)));

			;//System.out.println(" parse elements <" + file + ">");

			// pre-walk, pulling out ids
			findIds(document);

			// walk the document, pulling out salient properties

			parseElements(document);

			;//System.out.println(" finished <" + file + ">");

		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	HashMap<String, Node> nodeMap = new HashMap<String, Node>();

	private void findIds(Node document) {
		NamedNodeMap a = document.getAttributes();
		if (a != null) {
			Node id = document.getAttributes().getNamedItem("id");
			if (id != null) {
				String idIs = id.getTextContent();
				;//System.out.println(" node <" + idIs + "> is <" + document);
				nodeMap.put(document.getNodeName()+"::"+idIs, document);
			}
		}
		NodeList d = document.getChildNodes();
		for (int i = 0; i < d.getLength(); i++) {
			findIds(d.item(i));
		}
	}

	public HashMap<String, HashMap<String, Object>> getProperties() {
		return properties;
	}

	private void parseElements(Node node) {

		if (debug)
			System.out.println(" inside parse elements for <" + file + ">");

//		System.out.println(node.getNodeName());
		if (node.getNodeName().equals("field.core.dispatch.VisualElement")) {
			System.out.println(" looking at visual element ");
			NodeList nl = node.getChildNodes();

			String uid = null;

			for (int i = 0; i < nl.getLength(); i++) {
				Node ii = nl.item(i);

				ii = resolve(ii);

				if (ii.getNodeName().equals("uid")) {
					uid = ii.getTextContent().trim();

					System.out.println(" got <" + uid + ">");

				}
				if (ii.getNodeName().equals("properties")) {
					HashMap<String, Object> props = parseProperties(ii);
					;//System.out.println(" parsed props, got <" + props + ">");

					if (props != null) {
						properties.put(uid, props);

						System.out.println(" we now have <" + properties.keySet() + "> for UID :"+uid);
						Set<Entry<String, HashMap<String, Object>>> es = properties.entrySet();
						for (Entry<String, HashMap<String, Object>> e : es) {
							System.out.println("       "+e.getKey()+" "+e.getValue().get("name"));
						}
					}
				}
			}
		}
		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++)
			parseElements(nl.item(i));

	}

	private Node resolve(Node ii) {
		NamedNodeMap a = ii.getAttributes();
		if (a == null)
			return ii;
		Node reference = a.getNamedItem("reference");
		if (reference == null)
			return ii;
		String t = reference.getTextContent();
		
		
		Node n = nodeMap.get(ii.getNodeName()+"::"+t);
		System.out.println(" looking up reference <"+t+"> to find <"+n+">");
		if (n == null)
			return ii;
		return n;
	}

	private Pair<String, Object> parseEntry(Node node) {
		NodeList nl = node.getChildNodes();
		String name = null;
		Object value = null;
		for (int i = 0; i < nl.getLength(); i++) {
			Node ii = nl.item(i);

			ii = resolve(ii);

			System.out.println(" entry, name <" + ii.getNodeName() + ">");

			if (ii.getNodeName().equals("field.core.dispatch.iVisualElement_-VisualElementProperty")) 
			{
				
				NodeList childNodes = ii.getChildNodes();
				for (int j = 0; j < childNodes.getLength(); j++) {

					Node cn = childNodes.item(j);
					cn = resolve(cn);

					System.out.println(" property <" + cn.getNodeName() + ">");
					if (cn.getNodeName().equals("name")) {
						name = cn.getTextContent();
						System.out.println(" name is <"+name+">");
					}
				}
			} else if (ii.getNodeName().equals("string")) {
				value = ii.getTextContent().trim();
			} else if (ii.getNodeName().equals("float")) {
				value = Float.parseFloat(ii.getTextContent().trim());
			} else if (ii.getNodeName().equals("double")) {
				value = Double.parseDouble(ii.getTextContent().trim());
			} else {

				;//System.out.println(" node is xml <" + loadProperties + ">");

				if (loadProperties) {
					try {
						String xml = nodeToString(ii);
						if (xml.trim().length() == 0)
							continue;
						if (xml.contains("reference="))
							continue;
						if (xml.contains("id="))
							continue;

						;//System.out.println(" xml is <" + xml + ">");
						xml = "<object-stream>" + xml + "</object-stream>";

						XStream stream = new XStream(new Sun14ReflectionProvider());
						stream.registerConverter(new ChannelSerializer());
						stream.registerConverter(new FloatBufferSerializer());
						stream.registerConverter(new MarkerSerializer(stream.getClassMapper()));
						stream.setMarshallingStrategy(new ReferenceByIdMarshallingStrategy());
						Object o = stream.createObjectInputStream(new StringReader(xml)).readObject();
						value = o;
						;//System.out.println(" -- loaded <" + value + ">");
					} catch (Throwable t) {
						System.out.println(" trouble loading contents of <"+ii.getNodeName()+"> continuing on");
					}
				}
			}
		}

//		System.out.println(" got <" + name + " = " + value + ">");

		if (name == null)
			return null;
		if (value == null)
			return null;

		else
			return new Pair<String, Object>(name, value);
	}

	static Transformer t;
	static {
		try {
			t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			t.setOutputProperty(OutputKeys.INDENT, "yes");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String nodeToString(Node node) {
		StringWriter sw = new StringWriter();
		try {
			t.transform(new DOMSource(node), new StreamResult(sw));
		} catch (TransformerException te) {
			;//System.out.println("nodeToString Transformer Exception");
		}
		return sw.toString();
	}

	private HashMap<String, Object> parseProperties(Node node) {
		HashMap<String, Object> ret = new LinkedHashMap<String, Object>();

		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node ii = nl.item(i);
			if (ii.getNodeName().equals("entry")) {
				Pair<String, Object> a = parseEntry(ii);
				if (a != null)
					ret.put(a.left, a.right);
			}
		}

		return ret;
	}

}
