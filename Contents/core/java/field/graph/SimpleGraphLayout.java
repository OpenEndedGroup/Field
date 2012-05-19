package field.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import field.core.plugins.log.KeyframeGroupOverride.Position;
import field.math.linalg.Vector3;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.Generics.Pair;

/**
 * This does a root and two level layout
 * 
 * @author marc
 * 
 */
public class SimpleGraphLayout {

	private final float d1;
	private final float d2;

	public HashMap<Object, Vector3> r;

	public HashMap<Pair<Object, Object>, EdgeType> edgeTypes;
	public HashMap<Object, NodeType> nodeTypes;
	public HashMap<Object, Vector3> parentDirection;

	public enum NodeType {
		root(10), level1(5), level2(0);

		private float z;

		private NodeType(float z) {
			this.z = z;
		}
		
		public float getZ() {
			return z;
		}
	}

	public enum EdgeType {
		root_level1, level1_level2_first, level1_level1, level1_level2_other, level2_level2, level2_level1, level2_root, fringe;
	}

	public SimpleGraphLayout(float d1, float d2) {
		this.d1 = d1;
		this.d2 = d2;
	}

	public void layout(Object root, iFunction<Collection<Object>, Object> connected) {

		r = new HashMap<Object, Vector3>();
		edgeTypes = new HashMap<Pair<Object, Object>, SimpleGraphLayout.EdgeType>();
		nodeTypes = new HashMap<Object, NodeType>();
		parentDirection= new HashMap<Object, Vector3>();

		r.put(root, new Vector3());
		nodeTypes.put(root, NodeType.root);

		Collection<Object> level1 = connected.f(root);

		layoutLevel1(root, level1, connected);
		
		for(Map.Entry<Object, Vector3> p : r.entrySet())
		{
			p.getValue().z = nodeTypes.get(p.getKey()).z;
		}
	}

	Vector3 initial = new Vector3(0, -1, 0);
	float topScale = 1;

	protected void layoutLevel1(Object root, Collection<Object> level1, iFunction<Collection<Object>, Object> connected) {

		if (level1.size() == 0)
			return;

		Iterator<Object> ii = level1.iterator();

		Vector3 delta = new Vector3(initial).scale(d1);

		while (ii.hasNext()) {
			Object o = ii.next();

			Vector3 at = new Vector3(delta);
			r.put(o, at);
			nodeTypes.put(o, NodeType.level1);
			parentDirection.put(o, new Vector3(delta).normalize());

			edgeTypes.put(new Pair<Object, Object>(root, o), EdgeType.root_level1);

			layoutLevel2(o, connected.f(o), at, at, connected);

			delta.rotateBy((float) (topScale*Math.PI * 2.0 / level1.size()));

		}

	}

	private void layoutLevel2(Object root, Collection<Object> f, Vector3 at, Vector3 dir, iFunction<Collection<Object>, Object> connected) {

		Vector3 forward = new Vector3(dir).rotateBy((float) (-Math.PI / 2)).normalize().scale(d2);

		float n = f.size();

		forward.rotateBy((float) (Math.PI / 2 / (n + 1)));

		Iterator<Object> ff = f.iterator();
		while (ff.hasNext()) {
			Object o = ff.next();

			Vector3 at2 = new Vector3(at).add(forward);
			if (r.containsKey(o)) {

				if (nodeTypes.get(o) == NodeType.root) {
				} else if (nodeTypes.get(o) == NodeType.level1)
					edgeTypes.put(new Pair<Object, Object>(root, o), EdgeType.level1_level1);
				else
					edgeTypes.put(new Pair<Object, Object>(root, o), EdgeType.level1_level2_other);

			} else {
				
				parentDirection.put(o, new Vector3(forward).normalize());

				edgeTypes.put(new Pair<Object, Object>(root, o), EdgeType.level1_level2_first);
				r.put(o, at2);
				nodeTypes.put(o, NodeType.level2);

				Collection<Object> next = connected.f(o);
				Iterator<Object> fff = next.iterator();
				while (fff.hasNext()) {
					Object oo = fff.next();
					if (nodeTypes.get(oo) == NodeType.root)
						edgeTypes.put(new Pair<Object, Object>(o, oo), EdgeType.level2_root);
					else if (nodeTypes.get(oo) == NodeType.level1)
						edgeTypes.put(new Pair<Object, Object>(o, oo), EdgeType.level2_level1);
					else if (nodeTypes.get(oo) == NodeType.level2)
						edgeTypes.put(new Pair<Object, Object>(o, oo), EdgeType.level2_level2);
					else
						edgeTypes.put(new Pair<Object, Object>(o, oo), EdgeType.fringe);
				}
			}
			forward.rotateBy((float) (Math.PI / 2 / (n + 1)));

		}

	}

}
