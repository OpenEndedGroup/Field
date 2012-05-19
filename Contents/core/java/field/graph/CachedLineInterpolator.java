package field.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.math.abstraction.iAcceptor;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.util.Dict;
import field.util.Dict.Prop;
import field.util.HashMapOfLists;

public class CachedLineInterpolator {

	public class BlendEvent implements iAcceptor<Float> {

		private Event ec;
		private CachedLine ca;
		private CachedLine cb;
		int ia, ib;

		public BlendEvent(CachedLine ca, int ia, CachedLine cb, int ib, Event ec) {
			this.ec = ec;
			this.ca = ca;
			this.cb = cb;
			this.ia = ia;
			this.ib = ib;
		}

		@Override
		public iAcceptor<Float> set(Float to) {
			if (ec.args != null) {
				Event ea = ca.events.get(ia);
				Event eb = cb.events.get(ib);

				for (int i = 0; i < ec.args.length; i++) {
					float a = ((Number) ea.args[i % ea.args.length]).floatValue();
					float b = ((Number) eb.args[i % eb.args.length]).floatValue();
					ec.args[i] = a * (1 - to) + to * b;
				}
			}
			return this;
		}

	}

	HashMapOfLists<String, CachedLine> left;
	HashMapOfLists<String, CachedLine> right;

	Map<String, Blender> constructed = new LinkedHashMap<String, Blender>();

	public CachedLineInterpolator(List<CachedLine> left, List<CachedLine> right, Prop<String> hash) {
		this.left = makeMap(left, hash);
		this.right = makeMap(right, hash);

		buildRelationships();
	}

	public List<CachedLine> blend(float a) {
		ArrayList<CachedLine> cc = new ArrayList<CachedLine>();
		for (Blender b : constructed.values())
			cc.addAll(b.blend(a));

		return cc;
	}

	private void buildRelationships() {

		Set<Entry<String, Collection<CachedLine>>> es = left.entrySet();
		for (Entry<String, Collection<CachedLine>> ee : es) {

			int i = 0;
			Iterator<CachedLine> ii = ee.getValue().iterator();
			while (ii.hasNext()) {
				CachedLine leftLine = ii.next();
				CachedLine rightLine = find(right, ee.getKey(), i);

				constructed.put(ee.getKey() + "@" + i, constructBlender(leftLine, rightLine));
				i++;
			}
		}

		es = right.entrySet();
		for (Entry<String, Collection<CachedLine>> ee : es) {

			int i = 0;
			Iterator<CachedLine> ii = ee.getValue().iterator();
			while (ii.hasNext()) {
				CachedLine rightLine = ii.next();

				Blender already = constructed.get(ee.getKey() + "@" + i);

				if (already == null)
					constructed.put(ee.getKey() + "@" + i, constructBlender(null, rightLine));
				i++;
			}
		}

	}

	public boolean allowLengthChange = true;
	
	protected Blender constructBlender(CachedLine left, CachedLine right) {

		if (left == null)
			return new FadeIn(right);
		if (right == null)
			return new FadeOut(left);

		if (left.events.size() == right.events.size() || allowLengthChange) {
			return new StraightInterpolatingBlender(left, right);
		} else {
			return new Both(new FadeOut(left), new FadeIn(right));
		}

	}

	public class Both extends Blender {

		private Blender one;
		private Blender two;

		public Both(Blender one, Blender two) {
			this.one = one;
			this.two = two;
		}

		@Override
		public List<CachedLine> blend(float alpha) {
			ArrayList<CachedLine> c = new ArrayList<CachedLine>(2);
			c.addAll(one.blend(alpha));
			c.addAll(two.blend(alpha));

			return c;
		}
	}

	public class FadeIn extends Blender {

		private CachedLine material;
		private List<CachedLine> materialL;

		public FadeIn(CachedLine cc) {
			material = LineUtils.transformLine(cc, null, null, null, null);
			materialL = Collections.singletonList(material);
		}

		@Override
		public List<CachedLine> blend(float alpha) {
			if (alpha <= 0)
				return Collections.EMPTY_LIST;
			material.getProperties().put(iLinearGraphicsContext.totalOpacity, Math.pow(alpha, outPower));
			material.getProperties().put(iLinearGraphicsContext.forceNew, 1);
			material.mod();
			return materialL;
		}
	}

	float outPower = 4;

	public class FadeOut extends Blender {

		private CachedLine material;
		private List<CachedLine> materialL;

		public FadeOut(CachedLine cc) {
			material = LineUtils.transformLine(cc, null, null, null, null);
			materialL = Collections.singletonList(material);
		}

		@Override
		public List<CachedLine> blend(float alpha) {
			if (alpha >= 1)
				return Collections.EMPTY_LIST;
			material.getProperties().put(iLinearGraphicsContext.totalOpacity, Math.pow(1 - alpha, outPower));
			material.getProperties().put(iLinearGraphicsContext.forceNew, 1);
			material.mod();

			return materialL;
		}
	}

	public class StraightInterpolatingBlender extends Blender {

		private CachedLine material;
		private List<CachedLine> materialL;

		List<iAcceptor<Float>> doBlend = new ArrayList<iAcceptor<Float>>();
		private CachedLine b;

		public StraightInterpolatingBlender(CachedLine a, CachedLine b) {

			this.b = b;

			material = LineUtils.transformLine(a.events.size()>b.events.size() ? a : b, null, null, null, null);
			materialL = Collections.singletonList(material);

			for (int i = 0; i < material.events.size(); i++) {
				int ia = Math.min(i, a.events.size()-1);
				Event ea = a.events.get(ia);
				int ib = Math.min(i, b.events.size()-1);
				Event eb = b.events.get(ib);

				Event ec = material.events.get(i);

				doBlend.add(new BlendEvent(a, ia, b, ib, ec));

				Dict dict_a = ea.getDict();
				Dict dict_b = eb.getDict();
				Dict dict_c = ec.getDict();

				propertyBlend(doBlend, a, ia, b, ib, dict_c);
			}

			propertyBlend(doBlend, a.getProperties(), b.getProperties(), material.getProperties());

		}

		@Override
		public List<CachedLine> blend(float alpha) {

			if (alpha >= 1)
				return Collections.singletonList(b);

			for (iAcceptor<Float> aa : doBlend)
				aa.set(alpha);

			material.getProperties().put(iLinearGraphicsContext.forceNew, 1);
			material.mod();

			return materialL;
		}
	}

	private CachedLine find(HashMapOfLists<String, CachedLine> right2, String key, int i) {
		Collection<CachedLine> kk = right2.get(key);
		if (kk == null)
			return null;
		if (kk.size() <= i)
			return null;
		return ((List<CachedLine>) kk).get(i);
	}

	public abstract class Blender {
		abstract public List<CachedLine> blend(float alpha);
	}

	private HashMapOfLists<String, CachedLine> makeMap(List<CachedLine> list, Prop hash) {

		HashMapOfLists<String, CachedLine> r = new HashMapOfLists<String, CachedLine>();

		for (CachedLine ll : list) {
			String m = ll.getProperties().get(hash) + "";

			r.addToList(m, ll);
		}

		return r;
	}

	protected void propertyBlend(List<iAcceptor<Float>> doBlend, Dict dict_a, Dict dict_b, Dict dict_c) {
		Map<Prop, Object> localPropA = dict_a.getMap();
		Map<Prop, Object> localPropB = dict_b.getMap();

		HashSet<Prop> seen = new HashSet<Prop>();

		Set<Entry<Prop, Object>> es = localPropA.entrySet();
		for (Entry<Prop, Object> ee : es) {

			Object aee = ee.getValue();
			Object bee = localPropB.get(ee.getKey());

			seen.add(ee.getKey());

			iAcceptor<Float> bb = blendFor(dict_c, ee.getKey(), aee, bee);
			if (bb != null)
				doBlend.add(bb);
		}

		es = localPropA.entrySet();
		for (Entry<Prop, Object> ee : es) {
			if (!seen.contains(ee.getKey())) {
				iAcceptor<Float> bb = blendFor(dict_c, ee.getKey(), null, ee.getValue());
				if (bb != null)
					doBlend.add(bb);
			}
		}
	}

	protected void propertyBlend(List<iAcceptor<Float>> doBlend, CachedLine ca, int ia, CachedLine cb, int ib, Dict dict_c) {

		Map<Prop, Object> localPropA = ca.events.get(Math.min(ia, ca.events.size()-1)).getAttributes().getMap();
		Map<Prop, Object> localPropB = cb.events.get(Math.min(ib, cb.events.size()-1)).getAttributes().getMap();

		HashSet<Prop> seen = new HashSet<Prop>();

		Set<Entry<Prop, Object>> es = localPropA.entrySet();
		for (Entry<Prop, Object> ee : es) {

			Object aee = ee.getValue();
			Object bee = localPropB.get(ee.getKey());

			seen.add(ee.getKey());

			iAcceptor<Float> bb = blendFor(dict_c, ee.getKey(), ca, ia, cb, ib);
			if (bb != null)
				doBlend.add(bb);
		}

		es = localPropA.entrySet();
		for (Entry<Prop, Object> ee : es) {
			if (!seen.contains(ee.getKey())) {
//				iAcceptor<Float> bb = blendFor(dict_c, ee.getKey(), null, ee.getValue());
				iAcceptor<Float> bb = blendFor(dict_c, ee.getKey(), ca, ia, cb, ib);
				if (bb != null)
					doBlend.add(bb);
			}
		}
	}

	private iAcceptor<Float> blendFor(final Dict dict_c, final Prop key, final Object left, final Object right) {

		if (left == null || right == null) // todo
			return null;

		if (left instanceof Vector4 && right instanceof Vector4) {
			return new iAcceptor<Float>() {
				@Override
				public iAcceptor<Float> set(Float to) {

					dict_c.put(key, new Vector4().lerp((Vector4) left, (Vector4) right, to));

					return this;
				}
			};
		}
		if (left instanceof Vector3 && right instanceof Vector3) {
			return new iAcceptor<Float>() {
				@Override
				public iAcceptor<Float> set(Float to) {

					dict_c.put(key, new Vector3().lerp((Vector3) left, (Vector3) right, to));

					return this;
				}
			};
		}
		if (left instanceof Vector2 && right instanceof Vector2) {
			return new iAcceptor<Float>() {
				@Override
				public iAcceptor<Float> set(Float to) {

					dict_c.put(key, new Vector2().lerp((Vector2) left, (Vector2) right, to));

					return this;
				}
			};
		}
		if (left instanceof Number && right instanceof Number) {
			return new iAcceptor<Float>() {
				@Override
				public iAcceptor<Float> set(Float to) {

					dict_c.put(key, ((Number) left).floatValue() * (1 - to) + to * ((Number) right).floatValue());

					return this;
				}
			};
		}

		System.out.println(" warning: cannot interpolate " + key + " (" + left + " -> " + right + ")");

		return null;

	}

	private iAcceptor<Float> blendFor(final Dict dict_c, final Prop key, final CachedLine ca, final int ia, final CachedLine cb, final int ib) {

		Object left = ca.events.get(Math.min(ia, ca.events.size()-1)).getAttributes().get(key);
		Object right = cb.events.get(Math.min(ib, cb.events.size()-1)).getAttributes().get(key);

		if (left == null || right == null) // todo
			return null;

		if (left instanceof Vector4 && right instanceof Vector4) {
			return new iAcceptor<Float>() {
				@Override
				public iAcceptor<Float> set(Float to) {

					Object left = ca.events.get(ia).getAttributes().get(key);
					Object right = cb.events.get(ib).getAttributes().get(key);
					
					dict_c.put(key, new Vector4().lerp((Vector4) left, (Vector4) right, to));

					return this;
				}
			};
		}
		if (left instanceof Vector3 && right instanceof Vector3) {
			return new iAcceptor<Float>() {
				@Override
				public iAcceptor<Float> set(Float to) {

					Object left = ca.events.get(ia).getAttributes().get(key);
					Object right = cb.events.get(ib).getAttributes().get(key);

					dict_c.put(key, new Vector3().lerp((Vector3) left, (Vector3) right, to));

					return this;
				}
			};
		}
		if (left instanceof Vector2 && right instanceof Vector2) {
			return new iAcceptor<Float>() {
				@Override
				public iAcceptor<Float> set(Float to) {

					Object left = ca.events.get(ia).getAttributes().get(key);
					Object right = cb.events.get(ib).getAttributes().get(key);

					dict_c.put(key, new Vector2().lerp((Vector2) left, (Vector2) right, to));

					return this;
				}
			};
		}
		if (left instanceof Number && right instanceof Number) {
			return new iAcceptor<Float>() {
				@Override
				public iAcceptor<Float> set(Float to) {

					Object left = ca.events.get(ia).getAttributes().get(key);
					Object right = cb.events.get(ib).getAttributes().get(key);

					dict_c.put(key, ((Number) left).floatValue() * (1 - to) + to * ((Number) right).floatValue());

					return this;
				}
			};
		}

		System.out.println(" warning: cannot interpolate " + key + " (" + left + " -> " + right + ")");

		return null;

	}

	
}
