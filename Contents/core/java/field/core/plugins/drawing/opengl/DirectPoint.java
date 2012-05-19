package field.core.plugins.drawing.opengl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.graphics.core.Base;
import field.graphics.core.BasicUtilities;
import field.graphics.dynamic.DynamicPointlist;
import field.graphics.dynamic.SubLine_long;
import field.graphics.dynamic.SubMesh;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.util.Dict.Prop;

/**
 * dynamicLine<< FLine()
 * 
 */
public class DirectPoint {

	static public int fixedCurveSampling = 50;

	public class LineCache extends BasicUtilities.OnePassElement {

		private final DynamicPointlist inside;

		public LineCache(DynamicPointlist inside) {
			super(Base.StandardPass.transform);
			this.inside = inside;
		}

		List<LineAdaptor> adaptors = new ArrayList<LineAdaptor>();

		@Override
		public void performPass() {

//			System.out.println(" refreshing line <" + inside + ">");

			this.inside.open();

			for (LineAdaptor a : adaptors) {
				a.clearAndCopy(inside);
			}

			this.inside.close();

//			System.out.println(" line now <" + inside.getUnderlyingGeometry() + ">");

		}

	}

	HashMap<DynamicPointlist, LineCache> lineCache = new HashMap<DynamicPointlist, DirectPoint.LineCache>();

	public DirectPoint() {
	}

//	public void install() {
//		PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__rmod__", 1) {
//			@Override
//			public PyObject __call__(PyObject composeWith) {
//
//				DynamicPointlist inside = Py.tojava(composeWith, DynamicPointlist.class);
//
//				CachedLine target = Py.tojava(self, CachedLine.class);
//
//				wire(inside, target);
//
//				return composeWith;
//			}
//		};
//		PyType.fromClass(CachedLine.class).addMethod(meth);
//
//	}

	public class LineAdaptor {

		SubMesh cache;
		int mod = -1;
		private CachedLine from;

		public LineAdaptor(CachedLine from) {
			this.from = from;
		}

		public void clearAndCopy(DynamicPointlist inside) {
			
			
			boolean s = from.getProperties().isTrue(iLinearGraphicsContext.pointed, true);
			if (!s) return;

//			System.out.println(" -- clear and copy -- <" + from + " -> " + " " + inside + ">");

			if (cache == null) {
//				System.out.println(" uncached");
				from.finish();
				cache = makeConcrete(from,null);

//				System.out.println(" cache is <" + cache.getUnderlyingGeometry() + ">");

				mod = from.getModCount();
				inside.copyFrom(cache);
			} else if (mod != from.getModCount()) {
//				System.out.println(" cache miss");
				from.finish();
				mod = from.getModCount();

				// we can do better than this --- we can reuse the
				// storage if we have a fixed-output linearizer
				// and
				// only the positions have changed

				cache = makeConcrete(from, cache);
				inside.copyFrom(cache);
			} else {

//				System.out.println(" cache valid target: "+" "+inside.getVertexCursor()+" "+inside.getUnderlyingGeometry());

				inside.copyFrom(cache);
				
//				System.out.println(" target :"+inside+" "+inside.getUnderlyingGeometry());
//				System.out.println(" cache :"+cache+" "+cache.getUnderlyingGeometry());
				
			}
			
			inside.getUnderlyingGeometry().setSize(from.getProperties().getFloat(iLinearGraphicsContext.pointSize, 1));
			
			
		}
	}

	float currentTotalOpacity = 1;
	
	public SubMesh makeConcrete(CachedLine from, SubMesh prev) {

		Vector3 lastClose = new Vector3(0, 0, 0);
		Vector3 at = new Vector3(0, 0, 0);

		SubMesh cache = prev == null ? new SubMesh() : prev;
		cache.open();
//		cache.beginSpline(null);

		boolean ongoing = false;

		Vector4 defaultColor = from.getProperties().get(iLinearGraphicsContext.strokeColor);
		if (defaultColor == null)
			defaultColor = from.getProperties().get(iLinearGraphicsContext.color);
		if (defaultColor == null)
			defaultColor = new Vector4(1, 1, 1, 1);

		currentTotalOpacity = from.getProperties().getFloat(iLinearGraphicsContext.totalOpacity, 1f);
		
		LinkedHashMap<Prop, Integer> trackedProperties = new LinkedHashMap<Prop, Integer>();
		LinkedHashMap<Prop, Object> defaults = new LinkedHashMap<Prop, Object>();

		Map<String, Number> shaderAttributes = from.getProperties().get(iLinearGraphicsContext.shaderAttributes);

		trackedProperties.put(iLinearGraphicsContext.pointSize_v, 13);
		defaults.put(iLinearGraphicsContext.pointSize_v, from.getProperties().getFloat(iLinearGraphicsContext.pointSize, 1f));

		if (shaderAttributes != null) {
			for (Map.Entry<String, Number> s : shaderAttributes.entrySet()) {
				trackedProperties.put(new Prop(s.getKey()), s.getValue().intValue());
				defaults.put(new Prop(s.getKey()), null);
			}
		}

		trackedProperties.put(iLinearGraphicsContext.strokeColor_v, Base.color0_id);
		defaults.put(iLinearGraphicsContext.strokeColor_v, defaultColor);

		Event lastE = null;

		for (Event e : from.events) {
			if (e.method.equals(iLine_m.moveTo_m)) {

				ongoing = true;

				Vector3 z = LineUtils.getDestination3(e);
				lastClose = z;
				at = z;
				cache.nextVertex(z);

				for (Map.Entry<Prop, Integer> p : trackedProperties.entrySet()) {
					Object a = defaults.get(p.getKey());
					if (e.attributes != null) {
						Object d = e.getAttributes().get(p.getKey());
						if (d != null)
							a = d;

					}

					setAux(cache, p.getValue(), a);
				}

				lastE = e;
			} else if (e.method.equals(iLine_m.lineTo_m)) {
				Vector3 z = LineUtils.getDestination3(e);
				cache.nextVertex(z);
				at = z;

				lastE = e;

				for (Map.Entry<Prop, Integer> p : trackedProperties.entrySet()) {
					Object a = defaults.get(p.getKey());
					if (e.attributes != null) {
						Object d = e.getAttributes().get(p.getKey());
						if (d != null)
							a = d;
					}

					setAux(cache, p.getValue(), a);
				}

			} else if (e.method.equals(iLine_m.cubicTo_m)) {
				Vector3 z = LineUtils.getDestination3(e);

				Vector3 c1 = LineUtils.getControl1(e);
				Vector3 c2 = LineUtils.getControl2(e);

				Vector3 o = new Vector3();
				for (int i = 0; i < 1; i++) {
					LineUtils.evaluateCubicFrame3(at, c1, c2, z, (i + 1f) / 1, o);
					cache.nextVertex(o);

					for (Map.Entry<Prop, Integer> p : trackedProperties.entrySet()) {
						Object a = defaults.get(p.getKey());
						Object b = defaults.get(p.getKey());
						if (e.attributes != null) {
							Object d = e.getAttributes().get(p.getKey());
							if (d != null)
								a = d;
						}
						if (lastE.attributes != null) {
							Object d = e.getAttributes().get(p.getKey());
							if (d != null)
								b = d;
						}

						setAux(cache, p.getValue(), blend(a, b, (i + 1f) / 1));
					}
				}
				at = z;
			} else if (e.method.equals(iLine_m.close_m)) {
				at = lastClose;
			}
		}

		cache.close();

//		cache.makeCopy();

		return cache;
	}

	private Object blend(Object a, Object b, float f) {
		if (a == null)
			return b;
		if (b == null)
			return a;

		if (a instanceof Number)
			return ((Number) a).floatValue() * (1 - f) + f * ((Number) b).floatValue();
		if (a instanceof Vector2)
			return new Vector2().lerp((Vector2) a, (Vector2) b, f);
		if (a instanceof Vector3)
			return new Vector3().lerp((Vector3) a, (Vector3) b, f);
		if (a instanceof Vector4)
			return new Vector4().lerp((Vector4) a, (Vector4) b, f);

		return null;
	}

	private void setAux(SubMesh  cache, Integer value, Object a) {


		if (a == null)
			return;

		if (a instanceof Number)
			cache.setAux(cache.getVertexCursor() - 1, value, ((Number) a).floatValue());
		else if (a instanceof Vector2)
			cache.setAux(cache.getVertexCursor() - 1, value, ((Vector2) a).x, ((Vector2) a).y);
		else if (a instanceof Vector3)
			cache.setAux(cache.getVertexCursor() - 1, value, ((Vector3) a).x, ((Vector3) a).y, ((Vector3) a).z);
		else if (a instanceof Vector4)
			if(value==Base.color0_id)
				cache.setAux(cache.getVertexCursor() - 1, value, ((Vector4) a).x, ((Vector4) a).y, ((Vector4) a).z, ((Vector4) a).w*currentTotalOpacity);
			else
				cache.setAux(cache.getVertexCursor() - 1, value, ((Vector4) a).x, ((Vector4) a).y, ((Vector4) a).z, ((Vector4) a).w);
		else
			System.err.println(" can't setAux for <" + a + "> <" + a.getClass() + ">");
	}

	public void wire(DynamicPointlist inside, CachedLine target) {
		LineCache c = lineCache.get(inside);
		if (c == null) {
			lineCache.put(inside, c = new LineCache(inside));
			inside.getUnderlyingGeometry().addChild(c);
		}

		boolean skip = false;

		for (LineAdaptor a : c.adaptors) {
			if (a.from == target) {
				skip = true;
				break;
			}
		}

		if (!skip)
			c.adaptors.add(new LineAdaptor(target));
	}

	public void unwire(DynamicPointlist inside, CachedLine target) {
		LineCache c = lineCache.get(inside);
		if (c == null) {
			return;
		}

		boolean skip = false;

		HashSet<LineAdaptor> remove = new LinkedHashSet<LineAdaptor>();
		
		for (LineAdaptor a : c.adaptors) {
			if (a.from == target) {
				remove.add(a);
			}
		}

		c.adaptors.removeAll(remove);
	}

}
