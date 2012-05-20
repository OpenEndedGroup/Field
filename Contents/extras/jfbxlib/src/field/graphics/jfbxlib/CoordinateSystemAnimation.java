package field.graphics.jfbxlib;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map.Entry;

import field.graphics.jfbxlib.BuildTransformTreeVisitor.Transform;
import field.math.graph.NodeImpl;
import field.math.graph.SimpleNode;
import field.math.graph.iMutable;
import field.math.linalg.CoordinateFrame;
import field.math.util.CubicInterpolatorDynamic;

/**
 * a marker animation is a named tree of AnimatedCoordinateSystem
 */
public class CoordinateSystemAnimation {

	static public class AnimatedCoordinateSystem extends NodeImpl<AnimatedCoordinateSystem> implements Serializable {
		public String name;

		public CubicInterpolatorDynamic<CoordinateFrame> animation = new CubicInterpolatorDynamic<CoordinateFrame>();

		long uid;
	}

	public class Visitor extends BuildTransformTreeVisitor {

		private final double time;

		public Visitor(double time, JFBXVisitor next) {
			super(next);
			this.time = time;
		}

		@Override
		public void endScene() {
			if (time < start) start = time;
			if (time > end) end = time;
			super.endScene();
			computeLocals();

			// capture the stack

			Set<Entry<Long, SimpleNode<Transform>>> all = this.tree.entrySet();
			Iterator<Entry<Long, SimpleNode<Transform>>> i = all.iterator();
			while (i.hasNext()) {
				Entry<Long, SimpleNode<Transform>> e = i.next();
				AnimatedCoordinateSystem animation = animationFor(e.getKey(), e.getValue());
				animation.animation.new Sample(e.getValue().payload().createLocalCoordinateFrame(), (float) time);
			}
		}
	}

	public LinkedHashMap<Long, AnimatedCoordinateSystem> markers = new LinkedHashMap<Long, AnimatedCoordinateSystem>();

	public LinkedHashSet<AnimatedCoordinateSystem> roots = new LinkedHashSet<AnimatedCoordinateSystem>();

	double start = Float.POSITIVE_INFINITY;

	double end = Float.NEGATIVE_INFINITY;

	public AnimatedCoordinateSystem animationFor(Long key, SimpleNode<Transform> value) {
		AnimatedCoordinateSystem animation = markers.get(key);
		if (animation == null) {
			;//;//System.out.println(" new animation animation for <"+key+"> <"+value.payload().name+">");
			markers.put(key, animation = new AnimatedCoordinateSystem());
			animation.name = value.payload().name;
			animation.uid = key;

			if (value.getParents().size() == 0) {
				CoordinateSystemAnimation.this.roots.add(animation);
			}

			for (SimpleNode<Transform> z : value.getChildren()) {
				AnimatedCoordinateSystem c = animationFor(z.payload().uid, z);
				if (!animation.getChildren().contains(c)) {
					animation.addChild(c);
				}
			}
			for (iMutable<SimpleNode<Transform>> z : value.getParents()) {
				AnimatedCoordinateSystem c = animationFor(((SimpleNode<Transform>) z).payload().uid, ((SimpleNode<Transform>) z));
				if (!c.getChildren().contains(animation)) {
					c.addChild(animation);
				}
			}

		}
		return animation;
	}

	public LinkedHashSet<AnimatedCoordinateSystem> getRoots() {
		return roots;
	}

	public JFBXVisitor getVisitorForTime(final double time, JFBXVisitor next) {
		return new Visitor(time, next);
	}

}
