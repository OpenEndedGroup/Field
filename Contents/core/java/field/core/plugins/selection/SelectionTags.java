package field.core.plugins.selection;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.python.core.Py;
import org.python.core.PyObject;

import field.core.StandardFluidSheet;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.execution.PythonInterface;
import field.core.util.FieldPyObjectAdaptor2;
import field.core.util.FieldPyObjectAdaptor.iHandlesAttributes;
import field.core.util.FieldPyObjectAdaptor.iHandlesFindItem;

/**
 * code interface for rapidly selected subsections of elements (to hide them, to
 * do something else) based on a set of tags, and regular expressions over those
 * tags
 * 
 * 
 * 
 * @author marc
 * 
 */
public class SelectionTags implements iHandlesAttributes, iHandlesFindItem {

	static
	{
		FieldPyObjectAdaptor2.isHandlesAttributes(SelectionTags.class);
		FieldPyObjectAdaptor2.isHandlesFindItem(SelectionTags.class);
	}
	
	private final iVisualElement root;

	public SelectionTags(iVisualElement root) {
		this.root = root;
	}

	static public final VisualElementProperty<String> selectionTag = new VisualElementProperty<String>("selectionTag");

	public Object getAttribute(String name) {
		return null;
	}

	public void setAttribute(String name, Object value) {
		
	}

	public Object getItem(Object object) {
		String exp = null;
		if (object instanceof String) {
			exp = (String) object;
		} else
			exp = (String) Py.tojava((PyObject) object, String.class);

		List<iVisualElement> found = findElementsWithExpression(exp);

		PythonInterface.getPythonInterface().setVariable("__tmpFinderValue", found);
		return PythonInterface.getPythonInterface().eval("all(__tmpFinderValue)");

	}

	private List<iVisualElement> findElementsWithExpression(String exp) {
		List<iVisualElement> all = StandardFluidSheet.allVisualElements(root);
		Pattern m = Pattern.compile(exp);
		List<iVisualElement> foundList = new ArrayList<iVisualElement>();
		for (iVisualElement a : all) {
			String g = selectionTag.get(a);
			if (g == null)
				g = "unset";
			boolean found = m.matcher(g).find();
			if (found) {
				foundList.add(a);
			}
		}
		return foundList;
	}

	public void setItem(Object name, Object value) {
	}

}
