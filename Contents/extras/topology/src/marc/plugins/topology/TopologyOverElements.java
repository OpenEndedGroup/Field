package marc.plugins.topology;

import java.util.LinkedHashMap;
import java.util.List;

import field.core.StandardFluidSheet;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.persistance.VisualElementReference;
import field.core.plugins.connection.LineDrawingOverride;
import field.math.graph.NodeImpl;
import field.math.graph.TopologySearching;
import field.math.graph.iMutableContainer;
import field.math.graph.iTopology;
import field.math.graph.TopologySearching.AStarMetric;
import field.namespace.generic.Bind.iFunction;

public class TopologyOverElements {

	private final iVisualElement root;

	private final iFunction<Boolean, iVisualElement> f;

	public TopologyOverElements(iVisualElement root, iFunction<Boolean, iVisualElement> f) {
		this.root = root;
		this.f = f;

		reconstruct();
	}

	public class Node extends NodeImpl<Node> implements iMutableContainer<iVisualElement, Node> {

		iVisualElement p;

		public Node setPayload(iVisualElement t) {
			p = t;
			return this;
		}

		public iVisualElement payload() {
			return p;
		}

		@Override
		public String toString() {
			return "" + p;
		}
	}

	LinkedHashMap<iVisualElement, Node> all = new LinkedHashMap<iVisualElement, Node>();

	protected void reconstruct() {
		all.clear();

		List<iVisualElement> e = StandardFluidSheet.allVisualElements(root);
		for (iVisualElement ee : e) {
			VisualElementReference a = ee.getProperty(LineDrawingOverride.lineDrawing_to);
			VisualElementReference b = ee.getProperty(LineDrawingOverride.lineDrawing_from);
			if (a != null && b != null) {
				iVisualElement ae = a.get(root);
				iVisualElement be = b.get(root);

				if (ae != null && be != null) {
					Boolean m = f == null ? true : interpret(f.f(ee));
					if (m) {
						Node nae = getNode(ae, true);
						Node nbe = getNode(be, true);
						nae.addChild(nbe);
					}
				}
			}
		}
	}

	private boolean interpret(Object f) {
		if (f == null)
			return false;
		if (f instanceof Boolean)
			return ((Boolean) f);
		if (f instanceof Number)
			return ((Number) f).intValue() > 0;
		return true;
	}

	public LinkedHashMap<iVisualElement, Node> getAll() {
		return all;
	}

	public iTopology<iVisualElement> getTopology() {
		return new TopologyImpl(this);
	}
	
	public iTopology<iVisualElement> getTopology(iVisualElement d) {
		return new TopologyImpl(this, d);
	}

	Node getNode(iVisualElement ae, boolean create) {
		Node n = all.get(ae);
		if (n == null && create) {
			all.put(ae, n = new Node().setPayload(ae));
		}
		return n;
	}

	public List<iVisualElement> findPath(iVisualElement from, iVisualElement to) {
		return new TopologySearching.TopologyAStarSearch<iVisualElement>(getTopology(), new AStarMetric<iVisualElement>() {

			Rect r1 = new Rect(0, 0, 0, 0);
			Rect r2 = new Rect(0, 0, 0, 0);

			public double distance(iVisualElement from, iVisualElement to) {
				from.getFrame(r1);
				to.getFrame(r2);
				return r1.midpoint2().distanceFrom(r2.midpoint2());
			}
		}).search(from, to);
	}

}
