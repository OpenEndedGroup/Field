package field.core.plugins.drawing.opengl;

import java.awt.geom.CubicCurve2D;
import java.util.ArrayList;
import java.util.List;

import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.math.abstraction.iBlendable;
import field.math.abstraction.iMetric;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.util.Dict;
import field.util.Dict.Prop;

public abstract class Small3dLineEmitter_long {

	public float globalGeometricScale;

	public float globalFlatnessScale;

	List<Prop<?>> trackedProperties = new ArrayList<Prop<?>>();

	List<iMetric<?, ?>> trackedPropertiesFlatnesses = new ArrayList<iMetric<?, ?>>();

	boolean canEmitCubic = false;

	public Small3dLineEmitter_long setCanEmitCubic(boolean canEmitCubic) {
		this.canEmitCubic = canEmitCubic;
		return this;
	}

	public <T> void addTrackedProperty(Prop<T> p, iMetric<T, T> m) {
		trackedProperties.add(p);
		trackedPropertiesFlatnesses.add(m);
	}
	
	public void clearTrackedProperties()
	{
		trackedProperties.clear();
		trackedPropertiesFlatnesses.clear();
	}

	
	abstract public void emitLinearFrame(Vector3 a, Vector3 b, List<Object> name, List<Object> name2, Dict properties, iLinearGraphicsContext context);

	public void emitCubicFrame(Vector3 a, Vector3 c1, Vector3 c2, Vector3 b, List<Object> name, List<Object> name2, Dict properties, iLinearGraphicsContext context) {
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

	Vector3 tmp = new Vector3();

	public void flattenCubicFrame(Vector3 a, Vector3 c1, Vector3 c2, Vector3 b, List<Object> propertiesA, List<Object> propertiesB, Dict globalProperties, iLinearGraphicsContext context, int n) {

		float flatness = flatnessFor(a, c1, c2, b, propertiesA, propertiesB, context);
		if (!shouldTerm(flatness, a, c1, c2, b, n)) {
			Vector3 c12 = new Vector3();
			Vector3 c21 = new Vector3();
			List<Object> in = subdivide(propertiesA, propertiesB);

			// flattenCubicFrame(a, c1, c12,
			// new Vector2(cx, cy),
			// propertiesA, in,
			// globalProperties, context,
			// n+1, outputLine);
			// flattenCubicFrame(new
			// Vector2(cx, cy), c21, c2, b,
			// in, propertiesB,
			// globalProperties, context,
			// n+1, outputLine);

			Vector3 m = new Vector3();

			LineUtils.splitCubicFrame3(a, c1 = new Vector3(c1), c2 = new Vector3(c2), b, 0.5f, c12, m, c21, tmp);

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

	public void flattenCubicFrame(Vector2 a, Vector2 c1, Vector2 c2, Vector2 b, List<Object> propertiesA, List<Object> propertiesB, Dict globalProperties, iLinearGraphicsContext context, int n) {

		flattenCubicFrame(new Vector3(a.x, a.y, depthFor(propertiesA)), new Vector3(c1.x, c1.y, control1for(propertiesB)), new Vector3(c2.x, c2.y, control2for(propertiesB)), new Vector3(b.x, b.y, depthFor(propertiesB)), propertiesA, propertiesB, globalProperties, context, n);

	}

	private float control2for(List<Object> propertiesB) {
		Object o = propertiesB.get(0);
		if (o instanceof Number)
			return ((Number) o).floatValue();
		if (o instanceof Vector3)
			return ((Vector3) o).y;
		return 0;
	}

	private float control1for(List<Object> propertiesB) {
		Object o = propertiesB.get(0);
		if (o instanceof Number)
			return ((Number) o).floatValue();
		if (o instanceof Vector3)
			return ((Vector3) o).x;
		return 0;
	}

	private float depthFor(List<Object> propertiesA) {
		Object o = propertiesA.get(0);
		if (o instanceof Number)
			return ((Number) o).floatValue();
		if (o instanceof Vector3)
			return ((Vector3) o).z;
		return 0;
	}

	protected boolean shouldTerm(float flatness, Vector3 a, Vector3 c1, Vector3 c2, Vector3 b, int n) {
		return !(flatness > 0.3f && n < 10);
	}

	public void flattenLinearFrame(Vector3 a, Vector3 b, List<Object> propertiesA, List<Object> propertiesB, Dict properties, iLinearGraphicsContext context, int n) {

		float flatness = flatnessFor(a, null, null, b, propertiesA, propertiesB, context);

		if (!shouldTerm(flatness, a, null, null, b, n)) {

			Vector3 m = new Vector3(a).interpolate(b, 0.5f);
			Vector3 c21 = new Vector3();
			List<Object> in = subdivide(propertiesA, propertiesB);

			flattenLinearFrame(a, m, propertiesA, in, properties, context, n + 1);
			flattenLinearFrame(m, b, in, propertiesB, properties, context, n + 1);
		} else {
			emitLinearFrame(a, b, propertiesA, propertiesB, properties, context);
		}
	}

	public void flattenLinearFrame(Vector2 a, Vector2 b, List<Object> propertiesA, List<Object> propertiesB, Dict properties, iLinearGraphicsContext context, int n) {

		flattenLinearFrame(new Vector3(a.x, a.y, depthFor(propertiesA)), new Vector3(b.x, b.y, depthFor(propertiesB)), propertiesA, propertiesB, properties, context, n);

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
			if (b == null) {
				assert a == null : a + " " + b;
				q.add(null);
				continue;
			}

			if (a instanceof Number) {
				if (b instanceof Number) {
					q.add((((Number) a).floatValue() + ((Number) b).floatValue()) / 2);
				} else if (b instanceof Vector3) {
					float n = ((Number) a).floatValue();
					Vector3 v = ((Vector3) b);
					q.add(new Vector3(v.x + n, v.y + n, v.z + n).scale(0.5f));
				}
			} else if (a instanceof Vector3) {
				if (b instanceof Number) {
					float n = ((Number) b).floatValue();
					Vector3 v = ((Vector3) a);
					q.add(new Vector3(v.x + n, v.y + n, v.z + n).scale(0.5f));
				} else if (b instanceof Vector3) {
					q.add(new Vector3().add(((Vector3) a), ((Vector3) b)).scale(0.5f));
				}
			}
			else if (a instanceof iBlendable) {
				Object c = ((iBlendable) ((iBlendable) a).blendRepresentation_newZero()).lerp(a, b, 0.5f);
				q.add(c);
			} else if (a instanceof float[]) {
				float[] c = new float[((float[]) a).length];
				for (int z = 0; z < ((float[]) a).length; z++) {
					c[z] = (((float[]) a)[z] + ((float[]) b)[z]) / 2;
				}
				q.add(c);
			} 
			}
		return q;
	}

	private float flatnessFor(Vector3 a, Vector3 c1, Vector3 c2, Vector3 b, List<Object> propertiesA, List<Object> propertiesB, iLinearGraphicsContext context) {

		float geometric1 = c1 == null ? 0 : (float) CubicCurve2D.getFlatness(a.x, a.y, c1.x, c1.y, c2.x, c2.y, b.x, b.y) * globalGeometricScale;
		float geometric2 = c1 == null ? 0 : (float) CubicCurve2D.getFlatness(a.x, a.z, c1.x, c1.z, c2.x, c2.z, b.x, b.z) * globalGeometricScale;
		float geometric3 = c1 == null ? 0 : (float) CubicCurve2D.getFlatness(a.z, a.y, c1.z, c1.y, c2.z, c2.y, b.z, b.y) * globalGeometricScale;
		float property = propertyFlatness(propertiesA, propertiesB);
	
		float f = geometric1 + geometric2 + geometric3 + property;
	//	;//System.out.println(" flatness of <"+a+" "+c1+" "+c2+" "+b+"> is <"+f+"> <"+globalGeometricScale+"> <"+globalFlatnessScale+">");

		return f * globalFlatnessScale;

	}

	private float propertyFlatness(List<Object> propertiesA, List<Object> propertiesB) {
		assert propertiesA.size() == propertiesB.size() : propertiesA + " " + propertiesB;
		assert propertiesA.size() == trackedPropertiesFlatnesses.size();

		float m = 0;
		//;//System.out.println(" properties size <" + propertiesA + " " + propertiesB + "> <" + trackedPropertiesFlatnesses + ">");
		for (int i = 0; i < propertiesA.size() - 1; i++) {
			float q = ((iMetric<Object, Object>) trackedPropertiesFlatnesses.get(i)).distance(propertiesA.get(i + 1), propertiesB.get(i + 1));
			if (q > m)
				m = q;
		}
		return m;
	}

	public List<Object> packet(Event event) {
		ArrayList<Object> r = new ArrayList<Object>();

		Object d = event.getAttributes().get(iLinearGraphicsContext.z_v);
		r.add(d);

		for (Prop<?> t : trackedProperties) {
			Object value = event.getAttributes().get(t);
			r.add(value);
		}

		return r;
	}

}
