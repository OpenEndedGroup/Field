package field.core.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.selection.DrawTopology;
import field.core.plugins.selection.DrawTopology.DefaultDrawer;
import field.math.graph.iTopology;

public class ReferencePlugin2 extends BaseSimplePlugin {

	@Override
	public void registeredWith(iVisualElement root) {
		super.registeredWith(root);

		DrawTopology.addTopologyAsSelectionAxis("<html><font face='gill sans'>Embedded GUI Element <b>References</b></font>", root, new iTopology<iVisualElement>() {

			public List<iVisualElement> getChildrenOf(iVisualElement source) {

				List<iVisualElement> r = new ArrayList<iVisualElement>();

				Map<Object, Object> allProperties = source.payload();
				for (Entry<Object, Object> o : new HashMap<Object, Object>(allProperties).entrySet()) {
					if (o.getKey() instanceof VisualElementProperty) {
						if (((VisualElementProperty) o.getKey()).getName().startsWith("__minimalReference")) {
							if (o.getValue() instanceof List) {
								List<iVisualElement> connectedTo = (List<iVisualElement>) o.getValue();
								if (connectedTo != null) {
									r.addAll(connectedTo);
								}
							}
						}
					}
				}

				return r;
			}

			public List<iVisualElement> getParentsOf(iVisualElement of) {
				return Collections.EMPTY_LIST;
			}
		}, new DefaultDrawer(), "referencing", "referend", "r1", "r2");

	}

	@Override
	protected String getPluginNameImpl() {
		return "referencePlugin2";
	}

}
