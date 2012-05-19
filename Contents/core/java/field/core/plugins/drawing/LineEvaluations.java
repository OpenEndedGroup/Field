package field.core.plugins.drawing;

import java.util.Collections;
import java.util.List;

import field.core.dispatch.iVisualElement;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.Intersections;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.math.linalg.Vector2;
import field.namespace.generic.Generics.Pair;

public class LineEvaluations {

	private CachedLine on;
	private Vector2[] bounds;

	public LineEvaluations(CachedLine on) {
		this.on = on;

		if (on.getProperties().get(iLinearGraphicsContext.offsetFromSource) != null) {
			CachedLine mm = on.getProperties().get(iLinearGraphicsContext.offsetedLine);
			if (mm == null) {

				iVisualElement source = on.getProperties().get(iLinearGraphicsContext.source);

				if (source == null) {
					throw new IllegalArgumentException(" line is offset but not source");
				}

				CachedLine c2 = LineUtils.transformLineOffsetFrom(on, source.getFrame(null), on.getProperties().get(iLinearGraphicsContext.offsetFromSource));
				on.getProperties().put(iLinearGraphicsContext.offsetedLine, c2);
				this.on = c2;
			} else {
				this.on = mm;
			}
		}

		bounds = new LineUtils().fastBounds(this.on);
	}

	public List<Vector2> eval(float x) {
		List<Vector2> r = Intersections.intersect(on, new Vector2(x, bounds[0].y - 10), new Vector2(x, bounds[1].y + 10));
		return r;
	}

	static public CachedLine intersect(CachedLine on, float x) {
		LineEvaluations e = new LineEvaluations(on);
		CachedLine v = new CachedLine();
		v.getInput().moveTo(x, e.bounds[0].y - 10);
		v.getInput().lineTo(x, e.bounds[1].y + 10);
		List<Pair<Event, Event>> o = Intersections.intersectAndSubdivide(Collections.singletonList(e.on), Collections.singletonList(v), 4);
		return e.on;
	}

}
