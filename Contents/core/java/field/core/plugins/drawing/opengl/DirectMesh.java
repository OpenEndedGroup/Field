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
import field.graphics.dynamic.DynamicMesh_long;
import field.graphics.dynamic.SubMesh_long;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.util.Dict.Prop;

public class DirectMesh {

	static public int fixedCurveSampling = 50;

	public class MeshCache extends BasicUtilities.OnePassElement {

		private final DynamicMesh_long inside;

		public MeshCache(DynamicMesh_long inside) {
			super(Base.StandardPass.transform);
			this.inside = inside;
		}

		List<LineAdaptor> adaptors = new ArrayList<LineAdaptor>();

		@Override
		public void performPass() {

			// ;//System.out.println(" refreshing line <" + inside +
			// ">");

			this.inside.open();

			for (LineAdaptor a : adaptors) {
				a.clearAndCopy(inside);
			}

			this.inside.close();

			// ;//System.out.println(" line now <" +
			// inside.getUnderlyingGeometry() + ">");

		}

	}

	HashMap<DynamicMesh_long, MeshCache> lineCache = new HashMap<DynamicMesh_long, MeshCache>();
	private float currentTotalOpacity;

	public DirectMesh() {
	}

	// public void install() {
	// PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__rmod__", 1)
	// {
	// @Override
	// public PyObject __call__(PyObject composeWith) {
	//
	// DynamicMesh_long inside = Py.tojava(composeWith,
	// DynamicMesh_long.class);
	//
	// CachedLine target = Py.tojava(self, CachedLine.class);
	//
	// MeshCache c = lineCache.get(inside);
	// if (c == null) {
	// lineCache.put(inside, c = new MeshCache(inside));
	// inside.getUnderlyingGeometry().addChild(c);
	// }
	//
	// for (LineAdaptor a : c.adaptors) {
	// if (a.from == target)
	// return composeWith;
	// }
	//
	// c.adaptors.add(new LineAdaptor(target));
	//
	// return composeWith;
	// }
	// };
	// PyType.fromClass(CachedLine.class).addMethod(meth);
	//
	// }

	public void wire(DynamicMesh_long inside, CachedLine target) {
		MeshCache c = lineCache.get(inside);
		if (c == null) {
			lineCache.put(inside, c = new MeshCache(inside));
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

	public void unwire(DynamicMesh_long inside, CachedLine target) {
		MeshCache c = lineCache.get(inside);
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

	public class LineAdaptor {

		SubMesh_long cache;
		int mod = -1;
		private CachedLine from;

		public LineAdaptor(CachedLine from) {
			this.from = from;
		}

		public void clearAndCopy(DynamicMesh_long inside) {

			boolean s = from.getProperties().isTrue(iLinearGraphicsContext.filled, false);
			if (!s)
				return;

			// ;//System.out.println(" -- clear and copy -- <" + from +
			// " -> " + " " + inside + ">");

			if (cache == null) {
				// ;//System.out.println(" uncached");
				from.finish();
				cache = makeConcrete(from, null);

				// ;//System.out.println(" cache is <" +
				// cache.delegate.getUnderlyingGeometry() +
				// ">");

				mod = from.getModCount();
				inside.copyFrom(cache, false);
			} else if (mod != from.getModCount()) {
//				;//System.out.println(" cache miss");

				from.finish();
				mod = from.getModCount();

				// we can do better than this --- we can reuse the
				// storage if we have a fixed-output linearizer
				// and
				// only the positions have changed

				cache = makeConcrete(from, cache);
				inside.copyFrom(cache, false);
			} else {

				// ;//System.out.println(" cache valid ");

				inside.copyFrom(cache, true);
			}
		}
	}

	public SubMesh_long makeConcrete(CachedLine from, SubMesh_long prev) {

		final SubMesh_long cache = prev == null ? new SubMesh_long() : prev;
		cache.open();

		Vector4 defaultColor = from.getProperties().get(iLinearGraphicsContext.fillColor);
		if (defaultColor == null)
			defaultColor = from.getProperties().get(iLinearGraphicsContext.color);
		if (defaultColor == null)
			defaultColor = new Vector4(1, 1, 1, 1);

		LinkedHashMap<Prop, Integer> trackedProperties = new LinkedHashMap<Prop, Integer>();
		LinkedHashMap<Prop, Object> defaults = new LinkedHashMap<Prop, Object>();

		Map<String, Number> shaderAttributes = from.getProperties().get(iLinearGraphicsContext.shaderAttributes);
		if (shaderAttributes != null) {
			for (Map.Entry<String, Number> s : shaderAttributes.entrySet()) {
				trackedProperties.put(new Prop(s.getKey()), s.getValue().intValue());
				defaults.put(new Prop(s.getKey()), null);
			}
		}

		trackedProperties.put(iLinearGraphicsContext.fillColor_v, Base.color0_id);
		defaults.put(iLinearGraphicsContext.fillColor_v, defaultColor);

		List<Prop> propertiesInOrder = new ArrayList<Prop>(trackedProperties.keySet());
		final List<Integer> channelsInOrder = new ArrayList<Integer>(trackedProperties.values());
		List<Object> defaultsInOrder = new ArrayList<Object>(defaults.values());

		// special cases

		currentTotalOpacity = from.getProperties().getFloat(iLinearGraphicsContext.totalOpacity, 1f);

		if (from.events.size() < 6) {
			if (from.events.size() >= 4) {
				if (from.events.get(0).method.equals(iLine_m.moveTo_m) && from.events.get(1).method.equals(iLine_m.lineTo_m) && from.events.get(2).method.equals(iLine_m.lineTo_m) && from.events.get(3).method.equals(iLine_m.lineTo_m) && (from.events.size() == 4 || from.events.get(4).method.equals(iLine_m.close_m) || (from.events.get(4).method.equals(iLine_m.lineTo_m) && from.events.get(4).getDestination3().distanceFrom(from.events.get(0).getDestination3()) < 1e-4))) {

					;//System.out.println(" square fastpath");
					int a = cache.nextVertex(from.events.get(0).getDestination3());

					for (int i = 0; i < propertiesInOrder.size(); i++) {
						if (from.events.get(0).attributes != null) {
							Object d = from.events.get(0).attributes.get(propertiesInOrder.get(i));
							if (d == null)
								d = defaultsInOrder.get(i);

							setAux(cache, channelsInOrder.get(i), d);

						} else {
							setAux(cache, channelsInOrder.get(i), defaultsInOrder.get(i));
						}
					}

					int b = cache.nextVertex(from.events.get(1).getDestination3());
					for (int i = 0; i < propertiesInOrder.size(); i++) {
						if (from.events.get(1).attributes != null) {
							Object d = from.events.get(1).attributes.get(propertiesInOrder.get(i));
							if (d == null)
								d = defaultsInOrder.get(i);

							setAux(cache, channelsInOrder.get(i), d);

						} else {
							setAux(cache, channelsInOrder.get(i), defaultsInOrder.get(i));
						}
					}

					int c = cache.nextVertex(from.events.get(2).getDestination3());

					for (int i = 0; i < propertiesInOrder.size(); i++) {
						if (from.events.get(2).attributes != null) {
							Object d = from.events.get(2).attributes.get(propertiesInOrder.get(i));
							if (d == null)
								d = defaultsInOrder.get(i);

							setAux(cache, channelsInOrder.get(i), d);

						} else {
							setAux(cache, channelsInOrder.get(i), defaultsInOrder.get(i));
						}
					}

					int d = cache.nextVertex(from.events.get(3).getDestination3());

					for (int i = 0; i < propertiesInOrder.size(); i++) {
						if (from.events.get(2).attributes != null) {
							Object dd = from.events.get(3).attributes.get(propertiesInOrder.get(i));
							if (dd == null)
								dd = defaultsInOrder.get(i);

							setAux(cache, channelsInOrder.get(i), dd);

						} else {
							setAux(cache, channelsInOrder.get(i), defaultsInOrder.get(i));
						}
					}

					cache.nextFace(a, b, c);
					cache.nextFace(a, c, d);

					cache.close();
					cache.makeCopy();
					return cache;

				}
			} else if (from.events.size() >= 3) {
				if (from.events.get(0).method.equals(iLine_m.moveTo_m) && from.events.get(1).method.equals(iLine_m.lineTo_m) && from.events.get(2).method.equals(iLine_m.lineTo_m) && (from.events.size() == 3 || from.events.get(3).method.equals(iLine_m.close_m) || (from.events.get(3).method.equals(iLine_m.lineTo_m) && from.events.get(3).getDestination3().distanceFrom(from.events.get(0).getDestination3()) < 1e-4))) {
					;//System.out.println(" triangle fastpath");
					int a = cache.nextVertex(from.events.get(0).getDestination3());

					for (int i = 0; i < propertiesInOrder.size(); i++) {
						if (from.events.get(0).attributes != null) {
							Object d = from.events.get(0).attributes.get(propertiesInOrder.get(i));
							if (d == null)
								d = defaultsInOrder.get(i);

							setAux(cache, channelsInOrder.get(i), d);

						} else {
							setAux(cache, channelsInOrder.get(i), defaultsInOrder.get(i));
						}
					}

					int b = cache.nextVertex(from.events.get(1).getDestination3());
					for (int i = 0; i < propertiesInOrder.size(); i++) {
						if (from.events.get(1).attributes != null) {
							Object d = from.events.get(1).attributes.get(propertiesInOrder.get(i));
							if (d == null)
								d = defaultsInOrder.get(i);

							setAux(cache, channelsInOrder.get(i), d);

						} else {
							setAux(cache, channelsInOrder.get(i), defaultsInOrder.get(i));
						}
					}

					int c = cache.nextVertex(from.events.get(2).getDestination3());

					for (int i = 0; i < propertiesInOrder.size(); i++) {
						if (from.events.get(2).attributes != null) {
							Object d = from.events.get(2).attributes.get(propertiesInOrder.get(i));
							if (d == null)
								d = defaultsInOrder.get(i);

							setAux(cache, channelsInOrder.get(i), d);

						} else {
							setAux(cache, channelsInOrder.get(i), defaultsInOrder.get(i));
						}
					}

					cache.nextFace(a, b, c);
					cache.close();
					cache.makeCopy();
					return cache;

				}
			}
		}

		if (from.getProperties().isTrue(iLinearGraphicsContext.starConvex, false))
			doStarTess(from, cache, propertiesInOrder, channelsInOrder, defaultsInOrder);
		else
			doSlowTess(from, cache, propertiesInOrder, channelsInOrder, defaultsInOrder);

		return cache;
	}

	private void doStarTess(CachedLine from, final SubMesh_long cache, List<Prop> propertiesInOrder, List<Integer> channelsInOrder, List<Object> defaultsInOrder) {

		boolean ongoing = false;
		Vector3 lastClose = new Vector3(0, 0, 0);
		Vector3 at = new Vector3(0, 0, 0);

		SimpleTess_starConvex tess = new SimpleTess_starConvex() {

			@Override
			protected int nextVertex(Vector3 a, Map<Integer, Object> center) {
				int z = cache.nextVertex(a);
				for (Map.Entry<Integer, Object> oo : center.entrySet())
					setAux(cache, oo.getKey(), oo.getValue());
				return z;
			}

			@Override
			protected void nextFace(int a, int b, int c) {
				cache.nextFace(a, b, c);
			}
		};

		Map<Integer, Object> lastProperties = new LinkedHashMap<Integer, Object>();
		Map<Integer, Object> closeProperties = new LinkedHashMap<Integer, Object>();

		Event lastE = null;

		for (Event e : from.events) {
			if (e.method.equals(iLine_m.moveTo_m)) {

				if (ongoing) {
					tess.endContour();
				}

				ongoing = true;
				tess.beginContour();

				Vector3 z = LineUtils.getDestination3(e);
				lastClose = z;
				at = z;

				lastProperties = new LinkedHashMap<Integer, Object>();
				closeProperties = lastProperties;
				for (int i = 0; i < propertiesInOrder.size(); i++) {
					if (e.attributes != null) {
						Object d = e.attributes.get(propertiesInOrder.get(i));
						if (d == null)
							d = defaultsInOrder.get(i);
						lastProperties.put(channelsInOrder.get(i), d);
					} else {
						lastProperties.put(channelsInOrder.get(i), defaultsInOrder.get(i));
					}
				}

				lastE = e;
			} else if (e.method.equals(iLine_m.lineTo_m)) {
				Vector3 z = LineUtils.getDestination3(e);

				Map<Integer, Object> thisProperties = new LinkedHashMap<Integer, Object>();
				for (int i = 0; i < propertiesInOrder.size(); i++) {
					if (e.attributes != null) {
						Object d = e.attributes.get(propertiesInOrder.get(i));
						if (d == null)
							d = defaultsInOrder.get(i);
						thisProperties.put(channelsInOrder.get(i), d);
					} else {
						thisProperties.put(channelsInOrder.get(i), defaultsInOrder.get(i));
					}
				}

//				tess.line(at, z, lastProperties, thisProperties);
				tess.next(z, thisProperties);

				lastProperties = thisProperties;
				at = z;
				lastE = e;

			} else if (e.method.equals(iLine_m.cubicTo_m)) {
				Vector3 z = LineUtils.getDestination3(e);
				Vector3 c1 = LineUtils.getControl1(e);
				Vector3 c2 = LineUtils.getControl2(e);

				ArrayList<Object> thisProperties = new ArrayList<Object>();
				for (int i = 0; i < propertiesInOrder.size(); i++) {
					if (e.attributes != null) {
						Object d = e.attributes.get(propertiesInOrder.get(i));
						if (d == null)
							d = defaultsInOrder.get(i);
						thisProperties.add(d);
					} else {
						thisProperties.add(defaultsInOrder.get(i));
					}
				}

				Vector3 oat = new Vector3(at);
				for (int i = 0; i < fixedCurveSampling; i++) {
					Vector3 o = new Vector3();
					LineUtils.evaluateCubicFrame3(oat, c1, c2, z, (i + 1f) / fixedCurveSampling, o);

					Map<Integer, Object> xProperties = new LinkedHashMap<Integer, Object>();
					for (int j = 0; j < propertiesInOrder.size(); j++) {
						xProperties.put(channelsInOrder.get(j), blend(lastProperties.get(j), thisProperties.get(j), (i + 1f) / fixedCurveSampling));
					}

//					tess.line(at, o, lastProperties, xProperties);

					tess.next(o, xProperties);
					
					lastProperties = xProperties;
					at = o;
				}

			} else if (e.method.equals(iLine_m.close_m)) {
				Vector3 z = lastClose;

				tess.next(z, closeProperties);
				
				at = z;
				lastE = e;
			}
		}

		if (ongoing) {
			tess.endContour();
		}

		cache.close();

		cache.makeCopy();
		
	}

	protected void doSlowTess(CachedLine from, final SubMesh_long cache, List<Prop> propertiesInOrder, final List<Integer> channelsInOrder, List<Object> defaultsInOrder) {
		Vector3 lastClose = new Vector3(0, 0, 0);
		Vector3 at = new Vector3(0, 0, 0);

		boolean ongoing = false;

		SimpleTess tess = new SimpleTess() {

			@Override
			protected int nextVertex(Vector3 position) {
				return cache.nextVertex(position);
			}

			@Override
			protected void nextFace(int a, int b, int c) {
				cache.nextFace(a, b, c);
			}

			@Override
			protected void decorateVertex(int vertex, List<Object> properties) {
				for (int i = 0; i < properties.size(); i++) {
					Integer c = channelsInOrder.get(i);
					setAux(cache, c, properties.get(i));
				}
			}
		};

		tess.begin();

		Event lastE = null;

		List<Object> lastProperties = new ArrayList();
		List<Object> closeProperties = new ArrayList();

		for (Event e : from.events) {
			if (e.method.equals(iLine_m.moveTo_m)) {

				if (ongoing) {
					tess.endContour();
				}

				ongoing = true;
				tess.beginContour();

				Vector3 z = LineUtils.getDestination3(e);
				lastClose = z;
				at = z;

				lastProperties = new ArrayList<Object>();
				closeProperties = lastProperties;
				for (int i = 0; i < propertiesInOrder.size(); i++) {
					if (e.attributes != null) {
						Object d = e.attributes.get(propertiesInOrder.get(i));
						if (d == null)
							d = defaultsInOrder.get(i);
						lastProperties.add(d);
					} else {
						lastProperties.add(defaultsInOrder.get(i));
					}
				}

				lastE = e;
			} else if (e.method.equals(iLine_m.lineTo_m)) {
				Vector3 z = LineUtils.getDestination3(e);

				ArrayList<Object> thisProperties = new ArrayList<Object>();
				for (int i = 0; i < propertiesInOrder.size(); i++) {
					if (e.attributes != null) {
						Object d = e.attributes.get(propertiesInOrder.get(i));
						if (d == null)
							d = defaultsInOrder.get(i);
						thisProperties.add(d);
					} else {
						thisProperties.add(defaultsInOrder.get(i));
					}
				}

				tess.line(at, z, lastProperties, thisProperties);

				lastProperties = thisProperties;
				at = z;
				lastE = e;

			} else if (e.method.equals(iLine_m.cubicTo_m)) {
				Vector3 z = LineUtils.getDestination3(e);
				Vector3 c1 = LineUtils.getControl1(e);
				Vector3 c2 = LineUtils.getControl2(e);

				ArrayList<Object> thisProperties = new ArrayList<Object>();
				for (int i = 0; i < propertiesInOrder.size(); i++) {
					if (e.attributes != null) {
						Object d = e.attributes.get(propertiesInOrder.get(i));
						if (d == null)
							d = defaultsInOrder.get(i);
						thisProperties.add(d);
					} else {
						thisProperties.add(defaultsInOrder.get(i));
					}
				}

				Vector3 oat = new Vector3(at);
				for (int i = 0; i < fixedCurveSampling; i++) {
					Vector3 o = new Vector3();
					LineUtils.evaluateCubicFrame3(oat, c1, c2, z, (i + 1f) / fixedCurveSampling, o);

					ArrayList<Object> xProperties = new ArrayList<Object>(thisProperties.size());
					for (int j = 0; j < propertiesInOrder.size(); j++) {
						xProperties.add(blend(lastProperties.get(j), thisProperties.get(j), (i + 1f) / fixedCurveSampling));
					}

					tess.line(at, o, lastProperties, xProperties);

					lastProperties = xProperties;
					at = o;
				}

			} else if (e.method.equals(iLine_m.close_m)) {
				Vector3 z = lastClose;

				tess.line(at, z, lastProperties, closeProperties);


				at = z;
				lastE = e;
			}
		}

		if (ongoing) {
			tess.endContour();
		}
		tess.end();

		cache.close();

		cache.makeCopy();
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

	private void setAux(SubMesh_long cache, Integer value, Object a) {

		// ;//System.out.println(" set aux :" + value + " " + a);

		if (a == null)
			return;

		if (a instanceof Number)
			cache.setAux(cache.getVertexCursor() - 1, value, ((Float) a).floatValue());
		else if (a instanceof Vector2)
			cache.setAux(cache.getVertexCursor() - 1, value, ((Vector2) a).x, ((Vector2) a).y);
		else if (a instanceof Vector3)
			cache.setAux(cache.getVertexCursor() - 1, value, ((Vector3) a).x, ((Vector3) a).y, ((Vector3) a).z);
		else if (a instanceof Vector4)
			if (value == Base.color0_id)
				cache.setAux(cache.getVertexCursor() - 1, value, ((Vector4) a).x, ((Vector4) a).y, ((Vector4) a).z, ((Vector4) a).w * currentTotalOpacity);
			else
				cache.setAux(cache.getVertexCursor() - 1, value, ((Vector4) a).x, ((Vector4) a).y, ((Vector4) a).z, ((Vector4) a).w);
		else
			System.err.println(" can't setAux for <" + a + "> <" + a.getClass() + ">");
	}
}
