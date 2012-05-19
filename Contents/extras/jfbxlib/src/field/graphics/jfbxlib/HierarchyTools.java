package field.graphics.jfbxlib;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import field.graphics.jfbxlib.CoordinateSystemAnimation.AnimatedCoordinateSystem;
import field.graphics.jfbxlib.HierarchyOfCoordinateFrames.Element;
import field.math.abstraction.iInplaceProvider;
import field.math.graph.GraphNodeSearching;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.linalg.CoordinateFrame;

public class HierarchyTools {

	public final HierarchyOfCoordinateFrames hierarchy;

	public HierarchyTools(HierarchyOfCoordinateFrames hierarchy) {
		this.hierarchy = hierarchy;

	}

	public Map<String, iInplaceProvider<CoordinateFrame>> getLocalMap(Collection<AnimatedCoordinateSystem> coordinateSystemAnimationRoots) {
		final Map<String, iInplaceProvider<CoordinateFrame>> localMap = new HashMap<String, iInplaceProvider<CoordinateFrame>>();

		Iterator<AnimatedCoordinateSystem> i = coordinateSystemAnimationRoots.iterator();
		while (i.hasNext()) {
			new GraphNodeSearching.GraphNodeVisitor_depthFirst<CoordinateSystemAnimation.AnimatedCoordinateSystem>(false) {

				CoordinateFrame out = new CoordinateFrame();

				@Override
				protected VisitCode visit(CoordinateSystemAnimation.AnimatedCoordinateSystem n) {

					Element e = null;
					if (this.stack.size() > 1) {
						e = hierarchy.getChildOf(hierarchy.getNamed(this.stack.get(this.stack.size() - 2).name), n.name);
					} else {
						e = hierarchy.getRoot(n.name);
					}

					localMap.put(n.name, e);
					return VisitCode.cont;
				}

			}.apply(i.next());
		}

		return localMap;
	}

	public void updateAtTime(final double t, Collection<AnimatedCoordinateSystem> coordinateSystemAnimationRoots) {
		Iterator<AnimatedCoordinateSystem> i = coordinateSystemAnimationRoots.iterator();
		while (i.hasNext()) {

			new GraphNodeSearching.GraphNodeVisitor_depthFirst<CoordinateSystemAnimation.AnimatedCoordinateSystem>(false) {

				CoordinateFrame out = new CoordinateFrame();

				@Override
				protected VisitCode visit(CoordinateSystemAnimation.AnimatedCoordinateSystem n) {

					Element e = null;
					if (this.stack.size() > 1) {
						e = hierarchy.getChildOf(hierarchy.getNamed(this.stack.get(this.stack.size() - 2).name), n.name);
					} else {
						e = hierarchy.getRoot(n.name);
					}

					n.animation.getValue((float) t, out);

					e.setLocal(out);

					return VisitCode.cont;
				}

			}.apply(i.next());
		}
	}

}
