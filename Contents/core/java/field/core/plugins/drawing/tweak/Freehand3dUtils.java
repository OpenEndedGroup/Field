package field.core.plugins.drawing.tweak;

import java.util.Iterator;
import java.util.List;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.ThreedComputingOverride;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLine;
import field.core.plugins.drawing.opengl.iLine_m;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.plugins.drawing.threed.ThreedContext;
import field.core.plugins.drawing.tweak.FreehandTool3d_tweak.RawSplineData;
import field.core.windowing.GLComponentWindow;
import field.graphics.core.BasicCamera.Projector;
import field.graphics.core.BasicCamera.State;
import field.math.BaseMath;
import field.math.linalg.IntersectionPrimatives;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.math.linalg.IntersectionPrimatives.LinePointIntersectionInfo;

public class Freehand3dUtils {

	private final iVisualElement inside;

	public Freehand3dUtils(iVisualElement inside) {
		this.inside = inside;
	}

	public CachedLine projectAtZeroPoint(RawSplineData d) {
		CachedLine l = new CachedLine();
		iLine input = l.getInput();

		boolean first = true;
		State s = (State) d.transformState;
		Projector p = s.getProjector();
		Rect frame = d.elementFrameAtDrawTime;
		
		Iterator<Object> states = d.transformStates == null ? null : d.transformStates.iterator();
		
		for (Vector2 v : d.points) {
			
			if (states!=null && states.hasNext())
				s = (State)states.next();
			
			Vector3 x = singleProject(v, p, s, frame);
			if (first) {
				input.moveTo(x.x, x.y);
				input.setPointAttribute(iLinearGraphicsContext.z_v, x.z);
				first = false;
			} else {
				input.lineTo(x.x, x.y);
				input.setPointAttribute(iLinearGraphicsContext.z_v, x.z);
				first = false;
			}
		}

		l.getProperties().put(iLinearGraphicsContext.containsDepth, 1f);

		return l;
	}

	private Vector3 singleProject(Vector2 v, Projector p, State s, Rect frame) {

		Vector3 o1 = new Vector3();
		Vector3 o2 = new Vector3();

		p.createIntersectionRay((float) (v.x - frame.x), (float) (frame.h - v.y + frame.y), o1, o2, (int) frame.w, (int) frame.h);

		Vector3 target = s.getLookAt(null);
		Vector3 ray = s.getView();

		LinePointIntersectionInfo intersection = IntersectionPrimatives.lineToPlane(o1, new Vector3(o2).sub(o1).normalize(), target, ray);

		return intersection.closestPoint;
	}

	public void paintGuidePlaneNow(List<CachedLine> contents, ThreedContext defaultContext) {
		State camera = ThreedComputingOverride.camera.get(inside);
		Vector3 center = camera.getLookAt(null);
		Vector3 up = camera.getUp(null);
		Vector3 ray = camera.getView();
		Vector3 left = new Vector3().cross(up, ray);
		Vector3 position = camera.getPosition(null);

		up = new Vector3().cross(left, ray);

		up = up.normalize().scale(new Vector3().sub(center, position).mag() * 2);
		left = left.normalize().scale(new Vector3().sub(center, position).mag() * 2);

		/*
		 * center = _self.camera.target up = _self.camera.up left =
		 * Vector3().cross(up, _self.camera.view)
		 * 
		 * up =
		 * up.normalize()*(_self.camera.target-_self.camera.position
		 * ).mag()*2 left=
		 * left.normalize()*(_self.camera.target-_self.camera
		 * .position).mag()*2
		 * 
		 * rr = PLine3().moveTo(* center+up+left).lineTo(*
		 * center+up-left).lineTo(* center-up-left).lineTo(*
		 * center-up+left).lineTo(* center+up+left) rr(filled=1,
		 * color=Vector4(0.67,0.67,0.67,0.78))
		 */

		CachedLine ll = new CachedLine();
		iLine input = ll.getInput();

		input.moveTo(center.x + up.x + left.x, center.y + up.y + left.y);
		input.setPointAttribute(iLinearGraphicsContext.z_v, center.z + up.z + left.z);
		input.lineTo(center.x + up.x - left.x, center.y + up.y - left.y);
		input.setPointAttribute(iLinearGraphicsContext.z_v, center.z + up.z - left.z);
		input.lineTo(center.x - up.x - left.x, center.y - up.y - left.y);
		input.setPointAttribute(iLinearGraphicsContext.z_v, center.z - up.z - left.z);
		input.lineTo(center.x - up.x + left.x, center.y - up.y + left.y);
		input.setPointAttribute(iLinearGraphicsContext.z_v, center.z - up.z + left.z);
		input.lineTo(center.x + up.x + left.x, center.y + up.y + left.y);
		input.setPointAttribute(iLinearGraphicsContext.z_v, center.z + up.z + left.z);

		ll.getProperties().put(iLinearGraphicsContext.containsDepth, 1f);
		ll.getProperties().put(iLinearGraphicsContext.filled, true);
		ll.getProperties().put(iLinearGraphicsContext.color, new Vector4(0.67f, 0.67f, 0.67f, 0.75f));
		ll.getProperties().put(iLinearGraphicsContext.lateRendering, true);

		defaultContext.submitLine(ll, ll.getProperties());
		
		ray.normalize();

		for (CachedLine c : contents) {
			scanAndIntersect(c, center, ray, position,defaultContext);
		}

	}

	private void scanAndIntersect(CachedLine c, Vector3 center, Vector3 ray, Vector3 position, ThreedContext defaultContext) {

		float max = Float.NEGATIVE_INFINITY;
		float min = Float.POSITIVE_INFINITY;

		for(Event ee : c.events)
		{
			Vector3 d = LineUtils.getDestination3(ee);
			d.sub(center);
			float f = d.dot(ray);
			if (f>max) max = f;
			if (f<min) min = f;
		}

		if (max-min<0.1) return;
		
		;//System.out.println(" max - min for line <"+(max-min)+">");
		
		List<Event> e = c.events;
		Vector3 last = null;
		for (Event ee : e) {
			if (ee.method.equals(iLine_m.moveTo_m)) {
				last = LineUtils.getDestination3(ee);
			} else if (last != null && ee.method.equals(iLine_m.lineTo_m)) {
				intersect(last, LineUtils.getDestination3(ee), center, ray, position, defaultContext);
				last = LineUtils.getDestination3(ee);
			}
		}
	}

	Vector3 t1 = new Vector3();
	Vector3 t2 = new Vector3();

	private void intersect(Vector3 ll, Vector3 dd, Vector3 center, Vector3 ray, Vector3 position, ThreedContext defaultContext) {
		t1.sub(ll, center);
		float q = t1.dot(ray);
		if (Math.abs(q)<1e-6) return;
		
		int a1 = BaseMath.sgn(q);
		t1.sub(dd, center);
		q = t1.dot(ray);
		if (Math.abs(q)<1e-6) return;
		int a2 = BaseMath.sgn(q);
		if (a1!=a2)
		{
			LinePointIntersectionInfo a = IntersectionPrimatives.lineToPlane(new Vector3(ll), new Vector3(dd).sub(ll).normalize(), center, ray);
			if (a!=null && a.distanceAlongLine>=0 && a.distanceAlongLine<=dd.distanceFrom(ll))
			{
				CachedLine point = new CachedLine();
				
				//a.closestPoint.lerp(a.closestPoint, position, 0.1f);

				Vector2 out = new Vector2();
				defaultContext.convertIntermediateSpaceToDrawingSpace(a.closestPoint, out);

				point.getInput().moveTo(out.x, out.y); 
				point.getProperties().put(iLinearGraphicsContext.pointed, true);
				point.getProperties().put(iLinearGraphicsContext.pointSize, 10f);
				point.getProperties().put(iLinearGraphicsContext.pointColor, new Vector4(1, 0.1f, 0.1f, 0.3f));

				GLComponentWindow.currentContext.submitLine(point, point.getProperties());
			}
		}
	}

}
