package field.core.plugins.drawing.threed;

import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import field.graphics.core.BasicCamera;
import field.graphics.core.BasicSceneList;
import field.graphics.core.Base.iGeometry;
import field.graphics.core.Base.iSceneListElement;
import field.graphics.core.BasicCamera.Projector;
import field.graphics.core.BasicCamera.State;
import field.launch.iUpdateable;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.IntersectionPrimatives;
import field.math.linalg.Vector3;
import field.math.linalg.IntersectionPrimatives.LinePointIntersectionInfo;
import field.math.util.CubicTools;
import field.namespace.generic.Generics.Pair;
import field.util.TaskQueue;

/**
 * adds a few things to the default KeyboardCameraControl
 * 
 * @author marc
 * 
 */
public class BetterCameraControl implements iUpdateable {

	private final BasicSceneList list;

	public BetterCameraControl(BasicSceneList list) {
		this.list = list;
	}

	TaskQueue q = new TaskQueue();

	public void update() {
		q.update();
	}

	public boolean needsUpdate() {
		return q.getNumTasks() > 0;
	}

	public void transitionToState(final State in, final BasicCamera on, final int over) {

		q.new Task() {

			int m = 0;

			@Override
			protected void run() {
				float at = f(m / (float) over);
				float next = f((m + 1) / (float) over);

				float fraction = (next - at) / (1 - at);

				State current = on.getState();

				;//System.out.println(" moving from <" + current + "> to <" + in + "> by <" + fraction + ">");

				current = State.blend(current, in, fraction);

				on.setState(current);

				m++;

				if (m < over)
					recur();
			}

			private float f(float f) {
				return CubicTools.smoothStep(f);
			}
		};

	}

	public void transitionToState(final State in, final BasicCamera on, final int over, final iUpdateable post) {

		q.new Task() {

			int m = 0;

			@Override
			protected void run() {
				float at = f(m / (float) over);
				float next = f((m + 1) / (float) over);

				float fraction = (next - at) / (1 - at);

				State current = on.getState();

				;//System.out.println(" moving from <" + current + "> to <" + in + "> by <" + fraction + ">");

				current = State.blend(current, in, fraction);

				on.setState(current);

				m++;

				post.update();
				if (m < over)
					recur();
			}

			private float f(float f) {
				return CubicTools.smoothStep(f);
			}
		};

	}

	public State lookAtMiddle(State in) {

		State o = in.duplicate();

		Pair<Vector3, Vector3> x = aabb(in);
		if (x == null)
			return in;

		o.target = new Vector3().lerp(x.left, x.right, 0.5f);
		o = fixUp(o);
		return o;
	}

	public State lookAtMiddleOfVisible(State in) {

		State o = in.duplicate();

		Vector3 x = centerVisible(in);
		if (x == null)
			return in;

		o.target = x;
		o = fixUp(o);
		return o;
	}

	public State lookAtMiddleOfVisibleDepth(State in) {

		State o = in.duplicate();

		Vector3 x = centerVisible(in);
		if (x == null)
			return in;

		float d = o.position.distanceFrom(x);

		o.target = new Vector3(o.target).sub(o.position).setMagnitude(d).add(o.position);
		o = fixUp(o);
		return o;
	}

	public State frameAll(State in) {

		float f = in.fov;

		Vector3 view = new Vector3(in.target).sub(in.position);
		Vector3 target = in.target;
		view.normalize();
		Vector3 p = in.position;

		Collection<iGeometry> allMeshes = allMeshes();

		float dMin = Float.POSITIVE_INFINITY;

		for (iGeometry g : allMeshes) {
			FloatBuffer v = g.vertex();
			int n = g.numVertex();
			CoordinateFrame ff = new CoordinateFrame();
			g.getCoordinateProvider().get(ff);
			int step = 1;
			if (n > 1000)
				step = (int) (Math.log10(n) - 1);

			for (int i = 0; i < n; i += step) {
				v.position(3 * i);
				Vector3 vv = new Vector3(v);

				;//System.out.println(" position <" + vv + ">");

				if (vv.isNaN())
					continue;
				ff.transformPosition(vv);
				;//System.out.println(" position <" + vv + "> transformed ");

				LinePointIntersectionInfo ii = IntersectionPrimatives.lineToPoint(p, view, vv);
				float o = ii.intersectionDistance;
				float at = ii.distanceAlongLine;
				Vector3 aFrom = ii.closestPoint;

				;//System.out.println(" at :" + at + " " + f + " " + o);

				float aMin = at - (float) (o / Math.tan( (2*Math.PI*f/360.0)/2));

				if (aMin < dMin) {
					dMin = aMin;
				}

				;//System.out.println(" min is <" + dMin + ">");
			}
		}

		State o = in.duplicate();

		view.normalize();
		;//System.out.println(" frame <" + view + "> <" + dMin + "> <" + p + ">");

		o.position = (p.add(view.scale(dMin)));
		return o;
	}

	public State fixUp(State o) {
		Vector3 up = o.up;
		Vector3 view = o.getView();

		Vector3 left = new Vector3().cross(up, view);
		up = new Vector3().cross(view, left);

		o = o.duplicate();
		o.up = up;
		return o;
	}

	public Pair<Vector3, Vector3> aabb(State in) {
		Vector3 min = new Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
		Vector3 max = new Vector3(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

		Collection<iGeometry> allMeshes = allMeshes();

		boolean set = false;
		for (iGeometry g : allMeshes) {
			FloatBuffer v = g.vertex();
			int n = g.numVertex();
			CoordinateFrame ff = new CoordinateFrame();
			g.getCoordinateProvider().get(ff);
			int step = 1;
			if (n > 1000)
				step = (int) (Math.log10(n) - 1);

			for (int i = 0; i < n; i += step) {
				v.position(3 * i);
				Vector3 vv = new Vector3(v);
				if (vv.isNaN())
					continue;
				ff.transformPosition(vv);
				;//System.out.println(vv);
				min.min(vv);
				max.max(vv);
				set = true;
			}
		}

		if (!set)
			return null;
		return new Pair<Vector3, Vector3>(min, max);
	}

	public Pair<Vector3, Vector3> aabbVisible(State in) {
		Vector3 min = new Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
		Vector3 max = new Vector3(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

		Projector p = in.getProjector();
		Collection<iGeometry> allMeshes = allMeshes();

		boolean set = false;
		for (iGeometry g : allMeshes) {
			FloatBuffer v = g.vertex();
			int n = g.numVertex();
			CoordinateFrame ff = new CoordinateFrame();
			g.getCoordinateProvider().get(ff);
			int step = 1;
			if (n > 1000)
				step = (int) (Math.log10(n) - 1);

			for (int i = 0; i < n; i += step) {
				v.position(3 * i);
				Vector3 vv = new Vector3(v);
				if (vv.isNaN())
					continue;
				Vector3 pixel = p.toPixel(vv, 1, 1);
				ff.transformPosition(vv);
				if (pixel.x >= 0 && pixel.x <= 1 && pixel.y >= 0 && pixel.y <= 1 && pixel.z > 0) {

					min.min(vv);
					max.max(vv);
					set = true;
				}
			}
		}

		if (!set)
			return null;
		return new Pair<Vector3, Vector3>(min, max);
	}

	public Vector3 centerVisible(State in) {

		Vector3 c = new Vector3();

		Projector p = in.getProjector();
		Collection<iGeometry> allMeshes = allMeshes();

		int num = 0;
		boolean set = false;
		for (iGeometry g : allMeshes) {
			FloatBuffer v = g.vertex();
			int n = g.numVertex();
			CoordinateFrame ff = new CoordinateFrame();
			g.getCoordinateProvider().get(ff);
			int step = 1;
			if (n > 1000)
				step = (int) (Math.log10(n) - 1);

			for (int i = 0; i < n; i += step) {
				v.position(3 * i);
				Vector3 vv = new Vector3(v);
				if (vv.isNaN())
					continue;
				Vector3 pixel = p.toPixel(vv, 1, 1);
				ff.transformPosition(vv);
				if (pixel.x >= 0 && pixel.x <= 1 && pixel.y >= 0 && pixel.y <= 1 && pixel.z > 0) {

					c.add(vv);
					num++;
					set = true;
				}
			}
		}

		if (!set)
			return null;
		return c.scale(1f / num);
	}

	private Collection<iGeometry> allMeshes() {

		LinkedHashSet<iGeometry> g = new LinkedHashSet<iGeometry>();
		addMeshes(list, g);

		return g;
	}

	private void addMeshes(BasicSceneList s, LinkedHashSet<iGeometry> g) {
		List<iSceneListElement> e = s.getChildren();
		for (iSceneListElement ee : e) {
			if (ee instanceof iGeometry)
				g.add((iGeometry) ee);
			if (ee instanceof BasicSceneList)
				addMeshes(((BasicSceneList) ee), g);
		}
	}

}
