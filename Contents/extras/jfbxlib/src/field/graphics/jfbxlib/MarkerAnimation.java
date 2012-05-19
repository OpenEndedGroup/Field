package field.graphics.jfbxlib;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import field.math.graph.NodeImpl;
import field.math.linalg.Vector3;
import field.math.util.CubicInterpolatorDynamic;

/**
 * a marker animation is a named tree of Vec3CubicInterpolatorDyanmics
 */
public class MarkerAnimation implements Serializable{

	public class AnimatedMarker extends NodeImpl<AnimatedMarker> implements Serializable{
		public String name;
		public CubicInterpolatorDynamic<Vector3> animation = new CubicInterpolatorDynamic<Vector3>();

		long uid;

	}
	public class Visitor extends BuildTransformTreeVisitor {

		private AnimatedMarker currentMarker;

		double atTime;

		public Visitor(double time, JFBXVisitor next) {
			super(next);
			atTime = time;
			if (atTime<start) start = atTime;
			if (atTime>end) end = atTime;
		}

		@Override
		public void visitTransformBegin(String name, int type, long uid) {
			super.visitTransformBegin(name, type, uid);

			AnimatedMarker marker = markers.get(name);
			if (marker == null) {
				marker = new AnimatedMarker();
				marker.name = name;
				markers.put(name, marker);
				marker.uid = uid;
			}

			if (currentTransform.size() > 1) {
				AnimatedMarker parent = markers.get(currentTransform.get(currentTransform.size() - 2).payload().name);
				assert parent != null : name + " " + currentTransform.get(currentTransform.size() - 2).payload().name + " " + markers;
				if (!parent.getChildren().contains(marker)) parent.addChild(marker);
			} else {
				MarkerAnimation.this.roots.add(marker);
			}

			currentMarker = marker;
		}

		@Override
		public void visitTransformInfo(float oq0, float oq1, float oq2, float oq3, float ot0, float ot1, float ot2, float os0, float os1, float os2, float q0, float q1, float q2, float q3, float t0, float t1, float t2, float s0, float s1, float s2) {
			super.visitTransformInfo(oq0, oq1, oq2, oq3, ot0, ot1, ot2, os0, os1, os2, q0, q1, q2, q3, t0, t1, t2, s0, s1, s2);
			Vector3 t = new Vector3(ot0, ot1, ot2);

			currentMarker.animation.new Sample(t, (float) atTime);
		}
	}

	public LinkedHashMap<String, AnimatedMarker> markers = new LinkedHashMap<String, AnimatedMarker>();
	public LinkedHashSet<AnimatedMarker> roots = new LinkedHashSet<AnimatedMarker>();

	double start;

	double end;

	public LinkedHashSet<AnimatedMarker> getRoots() {
		return roots;
	}

	public JFBXVisitor getVisitorForTime(final double time, JFBXVisitor next) {
		return new Visitor(time, next);
	}

}
