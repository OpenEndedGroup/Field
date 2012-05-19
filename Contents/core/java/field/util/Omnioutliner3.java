package field.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Omnioutliner3 {

	static public Map<String, String> stringToProperties(String s) {

		// controversial
		s.replace(" ", "");

		String[] expressions = s.split(",");
		Map<String, String> ret = new HashMap<String, String>();
		for (String exp : expressions) {
			String[] e = exp.split("=");
			if (e.length == 2) ret.put(e[0].trim(), e[1].trim());
		}
		return ret;
	}

	private DocumentBuilder builder;

	private Document document;

	private List<Node> items;

	public Omnioutliner3() {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(true);
		try {
			builder = factory.newDocumentBuilder();


			builder.setEntityResolver(new EntityResolver() {
				public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {


					return new InputSource(new ByteArrayInputStream(new byte[]{}));
				}
			});

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	public void addRow(String[] rowInformation) {
		List<Node> roots = findAllNodesCalled(document, "root");
		assert roots.size() == 1;
		Element newItem = document.createElement("item");
		Element newValues = document.createElement("values");
		newItem.appendChild(newValues);

		for (int i = 0; i < rowInformation.length; i++) {
			Element newText = document.createElement("text");
			Element newP = document.createElement("p");
			Element newRun = document.createElement("run");
			Element newLit = document.createElement("lit");
			newLit.setTextContent(rowInformation[i]);
			newRun.appendChild(newLit);
			newP.appendChild(newRun);
			newText.appendChild(newP);
			newValues.appendChild(newText);
		}
		roots.get(0).appendChild(newItem);

		items.add(newItem);
	}

	public List<Node> findAllNodesCalled(Node doc, String called) {
		List<Node> ret = new ArrayList<Node>();
		NodeList childNodes = doc.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			_findAllNodesCalled(childNodes.item(i), ret, called);
		}
		return ret;
	}
	public Document getDocument() {
		return document;
	}

	public List<List<String>> getItems() {
		List<List<String>> ret = new ArrayList<List<String>>();

		for (Node n : items) {
			List<String> valuesForNode = getValuesForNode(n);
			ret.add(valuesForNode);
		}
		return ret;
	}

	public LinkedHashMap<String, List<String>> getItemsByName() {
		LinkedHashMap<String, List<String>> ret = new LinkedHashMap<String, List<String>>();

		for (Node n : items) {
			List<String> valuesForNode = getValuesForNode(n);
			if (valuesForNode.size()==0)
			{
				Object replaces = ret.put("unnamed:"+n, valuesForNode);
				assert replaces == null : "duplicate (unnamed node) unnamed:"+n;
			}
			else
			{
				Object replaced = ret.put(valuesForNode.get(0), valuesForNode);
				assert replaced == null : "duplicate note <"+valuesForNode.get(0)+">";
			}
		}

		return ret;
	}

	public List<Node> getNodes()
	{
		return items;
	}

	public List<String> getValuesForNode(Node node) {
		assert node.getNodeName().equals("item");
		List<Node> values = getChildrenNodesCalled(node, Pattern.compile("values"));
		assert values.size() == 1;
		List<Node> texts = getChildrenNodesCalled(values.get(0), Pattern.compile("(text)|(null)"));
		List<String> ret = new ArrayList<String>();
		for (Node n : texts) {
			List<Node> lits = findAllNodesCalled(n, "lit");
			String s = "";
			for (Node l : lits) {
				s += (s.length()==0 ? "" : "\n")+ l.getTextContent() + " ";
			}
			ret.add(s.trim());
		}
		return ret;
	}

	public Omnioutliner3 read(File file) {
		try {

			if (file.isDirectory())
			{
				file = new File(file.getAbsolutePath()+"/contents.xml");
			}

			document = builder.parse(file);
			items = findAllNodesCalled(document, "item");
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this;
	}

	public void setColumnTextForItem(int item, int column, String text) {
		Node n = items.get(item);
		setColumnTextForNode(document, n, column, text);
	}

	public void setColumnTextForNode(Document doc, Node node, int column, String text) {
		List<Node> values = getChildrenNodesCalled(node, Pattern.compile("values"));
		assert values.size() == 1;
		List<Node> texts = getChildrenNodesCalled(values.get(0), Pattern.compile("(text)|(null)"));
		assert texts.size() > column;
		Node n = texts.get(column);
		if (n.getNodeName() == "null") {
			Element newText = doc.createElement("text");
			Element newP = doc.createElement("p");
			Element newRun = doc.createElement("run");
			Element newLit = doc.createElement("lit");
			newLit.setTextContent(text);
			newRun.appendChild(newLit);
			newP.appendChild(newRun);
			newText.appendChild(newP);
			values.get(0).appendChild(newText);
			values.get(0).replaceChild(newText, n);
		} else {
			List<Node> lits = findAllNodesCalled(n, "lit");
			assert lits.size() == 1;
			Node l = lits.get(0);
			l.setTextContent(text);
		}
	}

	public void write(File file) {

		if (!file.getName().endsWith("contents.xml"))
		{
			file = new File(file.getAbsolutePath()+"/contents.xml");
		}
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = tFactory.newTransformer();

			DOMSource source = new DOMSource(document);

			transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//omnigroup.com//DTD OUTLINE 3.0//EN");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.omnigroup.com/namespace/OmniOutliner/xmloutline-v3.dtd");
			StreamResult result = new StreamResult(new BufferedOutputStream(new FileOutputStream(file)));
			transformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}

	private void _findAllNodesCalled(Node node, List<Node> ret, String called) {
		if (node.getNodeName().equals(called)) ret.add(node);
		NodeList c = node.getChildNodes();
		for (int i = 0; i < c.getLength(); i++) {
			_findAllNodesCalled(c.item(i), ret, called);
		}
	}



	protected List<Node> getChildrenNodesCalled(Node node, Pattern called) {
		NodeList childNodes = node.getChildNodes();
		List<Node> ret = new ArrayList<Node>();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node n = childNodes.item(i);
			if (called.matcher(n.getNodeName()).matches()) {
				ret.add(n);
			}
		}
		return ret;
	}

}
