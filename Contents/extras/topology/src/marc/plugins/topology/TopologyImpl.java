/**
 * 
 */
package marc.plugins.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import marc.plugins.topology.TopologyOverElements.Node;
import field.core.dispatch.iVisualElement;
import field.math.graph.iTopology;

public class TopologyImpl implements iTopology<iVisualElement> {
	private final TopologyOverElements inside;
	private final iVisualElement e;

	TopologyImpl(TopologyOverElements topologyOverElements) {
		inside = topologyOverElements;
		e = null;
	}
	
	TopologyImpl(TopologyOverElements topologyOverElements, iVisualElement e) {
		inside = topologyOverElements;
		this.e = e;
	}
	
	
	public List<iVisualElement> up()
	{
		return getParentsOf(e);
	}

	public List<iVisualElement> down()
	{
		return getChildrenOf(e);
	}


	/**
	 * Returns the children of this element
	 */
	public List<iVisualElement> getChildrenOf(iVisualElement of) {
		Node n = inside.getNode(of, false);
		if (n == null)
			return Collections.EMPTY_LIST;
		List<iVisualElement> r = new ArrayList<iVisualElement>();
		for (Node nn : n.getChildren()) {
			r.add(nn.payload());
		}
		return r;
	}

	/**
	 * Returns the parents of this element
	 */
	public List<iVisualElement> getParentsOf(iVisualElement of) {
		Node n = inside.getNode(of, false);
		if (n == null)
			return Collections.EMPTY_LIST;
		List<iVisualElement> r = new ArrayList<iVisualElement>();
		for (Node nn : ((Collection<Node>) n.getParents())) {
			r.add(nn.payload());
		}
		return r;
	}
}