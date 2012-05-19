package field.core.plugins.drawing.opengl;

import java.awt.geom.CubicCurve2D;
import java.util.ArrayList;
import java.util.List;

import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.math.abstraction.iBlendable;
import field.math.abstraction.iMetric;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;
import field.util.Dict;
import field.util.Dict.Prop;

public abstract class SmallLineEmitter {

	public float globalGeometricScale;

	public float globalFlatnessScale;

	List<Prop< ? >> trackedProperties = new ArrayList<Prop< ? >>();

	List<iMetric< ? , ? >> trackedPropertiesFlatnesses = new ArrayList<iMetric< ? , ? >>();

	boolean canEmitCubic = false;

	public SmallLineEmitter setCanEmitCubic(boolean canEmitCubic) {
		this.canEmitCubic = canEmitCubic;
		return this;
	}

	public <T> void addTrackedProperty(Prop<T> p, iMetric<T, T> m) {
		trackedProperties.add(p);
		trackedPropertiesFlatnesses.add(m);

	}

	abstract public void emitLinearFrame(Vector2 a, Vector2 b, List<Object> name, List<Object> name2, Dict properties, iLinearGraphicsContext context);

	public void emitCubicFrame(Vector2 a, Vector2 c1, Vector2 c2, Vector2 b, List<Object> name, List<Object> name2, Dict properties, iLinearGraphicsContext context) {
		throw new IllegalArgumentException();
	};

	protected Vector4 black = new Vector4(0, 0, 0, 1);

	public void begin() {
	}

	public void beginContour() {
	}

	public void endContour() {
	}

	public void end() {
	}

	Vector2 tmp = new Vector2();

	
	
	public void flattenCubicFrame(Vector2 a, Vector2 c1, Vector2 c2, Vector2 b, List<Object> propertiesA, List<Object> propertiesB, Dict globalProperties, iLinearGraphicsContext context, int n) {

		float flatness = flatnessFor(a, c1, c2, b, propertiesA, propertiesB, context);

		if (!shouldTerm(flatness, a,c1,c2,b, n)) {
			Vector2 c12 = new Vector2();
			Vector2 c21 = new Vector2();
			List<Object> in = subdivide(propertiesA, propertiesB);

			// flattenCubicFrame(a, c1, c12, new Vector2(cx, cy), propertiesA, in, globalProperties, context, n+1, outputLine);
			// flattenCubicFrame(new Vector2(cx, cy), c21, c2, b, in, propertiesB, globalProperties, context, n+1, outputLine);

			Vector2 m = new Vector2();
			LineUtils.splitCubicFrame(a, c1 = new Vector2(c1), c2 = new Vector2(c2), b, 0.5f, c12, m, c21, tmp);

			flattenCubicFrame(a, c1, c12, m, propertiesA, in, globalProperties, context, n + 1);
			flattenCubicFrame(m, c21, c2, b, in, propertiesB, globalProperties, context, n + 1);
		} else {

			if (canEmitCubic) {
				emitCubicFrame(a, c1, c2, b, propertiesA, propertiesB, globalProperties, context);
			} else {
				emitLinearFrame(a, b, propertiesA, propertiesB, globalProperties, context);
			}
		}
	}

	protected boolean shouldTerm(float flatness, Vector2 a, Vector2 c1, Vector2 c2, Vector2 b, int n) {
		return !(flatness > 0.3f && n < 10);
	}

	public void flattenLinearFrame(Vector2 a, Vector2 b, List<Object> propertiesA, List<Object> propertiesB, Dict properties, iLinearGraphicsContext context, int n) {
		float flatness = flatnessFor(a, null, null, b, propertiesA, propertiesB, context);

		if (!shouldTerm(flatness, a,null,null,b, n)) {

			Vector2 m = new Vector2(a).interpolate(b, 0.5f);
			Vector2 c21 = new Vector2();
			List<Object> in = subdivide(propertiesA, propertiesB);

			flattenLinearFrame(a, m, propertiesA, in, properties, context, n + 1);
			flattenLinearFrame(m, b, in, propertiesB, properties, context, n + 1);
		} else {
			emitLinearFrame(a, b, propertiesA, propertiesB, properties, context);
		}
	}

	private List<Object> subdivide(List<Object> propertiesA, List<Object> propertiesB) {
		List<Object> q = new ArrayList<Object>();
		for (int i = 0; i < propertiesA.size(); i++) {
			Object a = propertiesA.get(i);
			Object b = propertiesB.get(i);

			if (a == null) {
				assert b == null : b;
				q.add(null);
				continue;
			}
			if (b==null)
			{
				assert a == null : a+" "+b;
				q.add(null);
				continue;
			}
			if (a instanceof iBlendable) {
				Object c = ((iBlendable) ((iBlendable) a).blendRepresentation_newZero()).lerp(a, b, 0.5f);
				q.add(c);
			} else if (a instanceof float[]) {
				float[] c = new float[((float[]) a).length];
				for (int z = 0; z < ((float[]) a).length; z++) {
					c[z] = (((float[]) a)[z] + ((float[]) b)[z]) / 2;
				}
				q.add(c);
			} else
				assert false : a;
		}
		return q;
	}

	private float flatnessFor(Vector2 a, Vector2 c1, Vector2 c2, Vector2 b, List<Object> propertiesA, List<Object> propertiesB, iLinearGraphicsContext context) {

		float geometric = c1 == null ? 0 : (float) CubicCurve2D.getFlatness(a.x, a.y, c1.x, c1.y, c2.x, c2.y, b.x, b.y) * globalGeometricScale;
		float property = propertyFlatness(propertiesA, propertiesB);

		float f = geometric + property;

		return f * globalFlatnessScale;

	}

	private float propertyFlatness(List<Object> propertiesA, List<Object> propertiesB) {
		assert propertiesA.size() == propertiesB.size() : propertiesA + " " + propertiesB;
		assert propertiesA.size() == trackedPropertiesFlatnesses.size();

		float m = 0;
		for (int i = 0; i < propertiesA.size(); i++) {
			float q = ((iMetric<Object, Object>) trackedPropertiesFlatnesses.get(i)).distance(propertiesA.get(i), propertiesB.get(i));
			if (q > m) m = q;
		}
		return m;
	}

	public List<Object> packet(Event event) {
		ArrayList<Object> r = new ArrayList<Object>();

		for (Prop< ? > t : trackedProperties) {
			Object value = event.getAttributes().get(t);
			r.add(value);
		}

		return r;
	}

}
