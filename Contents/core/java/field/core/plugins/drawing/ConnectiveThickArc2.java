package field.core.plugins.drawing;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.LineUtils;
import field.launch.iUpdateable;
import field.math.abstraction.iFloatProvider;
import field.math.abstraction.iProvider;
import field.math.abstraction.iProvider.HasChanged;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.namespace.generic.ReflectionTools;
import field.namespace.generic.Generics.Pair;

public class ConnectiveThickArc2 implements iUpdateable {


	public static enum Compass {
		left(0, 0.5f, -1, 0, 0, -1), right(1, 0.5f, 1, 0, 0, -1), up(0.5f, 0, 0, -1, 1, 0), down(0.5f, 1, 0, 1, 1, 0);

		final public float x;

		final public float y;

		final public float dx;

		final public float dy;

		public Vector3 point;

		public Vector3 normal;

		public Vector3 tangent;

		Compass(float x, float y, float dx, float dy, float tx, float ty) {
			this.x = x;
			this.y = y;
			point = new Vector3(x, y, 0);
			this.dx = dx;
			this.dy = dy;
			normal = new Vector3(dx, dy, 0);
			tangent = new Vector3(tx, ty, 0);
		}
	}

	static List<Pair<Compass, Compass>> options = new ArrayList<Pair<Compass, Compass>>();

	static {
		Compass[] c = Compass.values();
		for (Compass c1 : c) {
			for (Compass c2 : c) {
				options.add(new Pair<Compass, Compass>(c1, c2));
			}
		}
	}

	static public Pair<Compass, Compass> findMinimumRectangleConnection(final Rect start, final Rect end) {

		Pair<Compass, Compass> option = ReflectionTools.argMin(options, new Object() {
			public float distance(Pair<Compass, Compass> option) {
				return start.relativize(option.left.point).distanceFrom(end.relativize(option.right.point));
			}
		});

		return option;
	}

	private final HasChanged<Vector4> fillColor;

	private final HasChanged<Vector4> outlineColor;

	private final field.math.abstraction.iFloatProvider.HasChanged startThickness;

	private final field.math.abstraction.iFloatProvider.HasChanged endThickness;

	private final iVisualElement start;

	private final iVisualElement end;

	private final HasChanged<Rect> startRect;

	private final HasChanged<Rect> endRect;

	private iFloatProvider gate;

	private final iVisualElement dest;

	CachedLine destination;

	public ConnectiveThickArc2(iVisualElement dest, iProvider<Vector4> fillColor, iProvider<Vector4> outlineColor, iVisualElement start, iFloatProvider startThickness, iVisualElement end, iFloatProvider endThickness) {

		this.dest = dest;
		this.fillColor = new iProvider.HasChanged<Vector4>(fillColor);
		this.outlineColor = new iProvider.HasChanged<Vector4>(outlineColor);

		this.start = start;
		this.end = end;

		this.startRect = new iProvider.HasChanged<Rect>(new iProvider<Rect>() {
			public Rect get() {
				return ConnectiveThickArc2.this.start.getFrame(null);
			}
		});

		this.endRect = new iProvider.HasChanged<Rect>(new iProvider<Rect>() {
			public Rect get() {
				return ConnectiveThickArc2.this.end.getFrame(null);
			}
		});

		this.startThickness = new iFloatProvider.HasChanged(startThickness);
		this.endThickness = new iFloatProvider.HasChanged(endThickness);
	}

	public void addGate(iFloatProvider gate) {
		this.gate = gate;
	}

	public void dispose() {
	}

	public CachedLine getDestination() {
		return destination;
	}

	public void update() {
		List<CachedLine> lines = dest.getProperty(SplineComputingOverride.computed_linesToDraw);
		drawNow(lines);
	}

	private Shape reconstructGlyph() {
		GeneralPath path = new GeneralPath();

		final Rect start = startRect.get();
		final Rect end = endRect.get();

		Pair<Compass, Compass> option = findMinimumRectangleConnection(start, end);

		// there is a problem here. we, may still have to flip one of the end tangents should these lines cross.

		Vector3 startOrigin = start.relativize(option.left.point);

		Vector3 startTop = Vector3.add(option.left.tangent, 0 * startThickness.evaluate(), startOrigin, null);
		Vector3 startBottom = Vector3.add(option.left.tangent, -startThickness.evaluate(), startOrigin, null);

		float normalAmount = 35;

		Vector3 startTopIntermediate = Vector3.add(option.left.normal, normalAmount, startTop, null);
		Vector3 startTopIntermediate1 = Vector3.add(option.left.normal, 1, startTop, null);
		Vector3 startBottomIntermediate = Vector3.add(option.left.normal, normalAmount, startBottom, null);

		Vector3 endOrigin = end.relativize(option.right.point);

		Vector3 endTop = Vector3.add(option.right.tangent, 0 * endThickness.evaluate(), endOrigin, null);
		Vector3 endBottom = Vector3.add(option.right.tangent, -endThickness.evaluate(), endOrigin, null);

		Vector3 endTopIntermediate = Vector3.add(option.right.normal, normalAmount, endTop, null);
		Vector3 endTopIntermediate1 = Vector3.add(option.right.normal, 1, endTop, null);

		Vector3 endBottomIntermediate = Vector3.add(option.right.normal, normalAmount, endBottom, null);

		// path.moveTo(startTop.x, startTop.y);
		// path.curveTo(startTopIntermediate.x, startTopIntermediate.y, endTopIntermediate.x, endTopIntermediate.y, endTop.x, endTop.y);
		// path.lineTo(endBottom.x, endBottom.y);
		// path.curveTo(endBottomIntermediate.x, endBottomIntermediate.y, startBottomIntermediate.x, startBottomIntermediate.y, startBottom.x, startBottom.y);
		// path.lineTo(startTop.x, startTop.y);
		//
		//

		path.moveTo(startTop.x, startTop.y);
		path.lineTo(startTopIntermediate1.x, startTopIntermediate1.y);
		path.curveTo(startTopIntermediate.x, startTopIntermediate.y, endTopIntermediate.x, endTopIntermediate.y, endTopIntermediate1.x, endTopIntermediate1.y);
		path.lineTo(endTopIntermediate1.x, endTopIntermediate1.y);

		return path;
	}

	protected void drawNow(List<CachedLine> lines) {
		if (gate.evaluate() == 0) {
			lines.remove(destination);
		}


		if (fillColor.hasChanged(true) || outlineColor.hasChanged(true) || startRect.hasChanged(true) || endRect.hasChanged(true) || startThickness.hasChanged(true) || endThickness.hasChanged(true)) {
			Shape shape = reconstructGlyph();
			// glyph.shapeToDynamicMesh(shape, 0.05f, destination == null ? null : (destination.get() == null ? null : destination.get().get()), outline == null ? null : (outline.get() == null ? null : outline.get().get()), fillColor.get(), outlineColor.get());

			final float e = endThickness.evaluate();
			final float s = startThickness.evaluate();
			lines.remove(destination);
			destination = new LineUtils().piToCachedLine(shape.getPathIterator(null));
			lines.add(destination);
		}
	}
}
