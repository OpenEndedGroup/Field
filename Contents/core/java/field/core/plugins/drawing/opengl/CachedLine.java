package field.core.plugins.drawing.opengl;

import java.awt.Font;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import field.core.util.FieldPyObjectAdaptor.iExtensible;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.math.linalg.iToFloatArray;
import field.util.BiMap;
import field.util.Dict;
import field.util.Dict.Prop;

/**
 * this is like a LineProperties3 class for extremely high level caching of
 * lines
 * 
 * @author marc
 * 
 */
public class CachedLine {

	static public BiMap<Method, String> methodMap = new BiMap<Method, String>();
	static {
		methodMap.put(iLine_m.moveTo_m, "m");
		methodMap.put(iLine_m.lineTo_m, "l");
		methodMap.put(iLine_m.close_m, ".");
		methodMap.put(iLine_m.cubicTo_m, "c");
	}

	public class Event<T extends Number> implements iExtensible {
		/* transient */public Method method;

		public Object[] args;

		public Dict attributes = null;

		public Dict getAttributes() {
			if (attributes == null)
				attributes = new Dict();
			return attributes;
		}

		// private void readObject(java.io.ObjectInputStream stream)
		// throws IOException, ClassNotFoundException {
		// stream.defaultReadObject();
		// String methodName = (String) stream.readObject();
		// method = methodMap.getBackwards(methodName);
		// }
		//
		// private void writeObject(java.io.ObjectOutputStream stream)
		// throws IOException {
		// stream.defaultWriteObject();
		// stream.writeObject(methodMap.get(method));
		// }

		@Override
		public String toString() {

			Object zz = getAttributes().get(iLinearGraphicsContext.z_v);

			return (" " + method.getName() + " " + (args == null ? null : Arrays.asList(args)) + " " + (zz == null ? "" : ("z=" + zz)));
		}

		public CachedLine container = CachedLine.this;

		public Event<T> copy() {
			Event e = new Event();
			e.method = method;
			if (attributes != null)
				e.attributes = new Dict().putAll(attributes);
			if (args != null) {
				// 1.6
				 e.args = Arrays.copyOf(args, args.length);

				// FIXME
//				e.args = new Object[args.length];
				System.arraycopy(args, 0, e.args, 0, args.length);
			}
			return e;
		}

		public Vector2 getAt(int i, Vector2... s) {
			Vector2 v = null;
			if (s == null)
				v = new Vector2();
			else if (s.length == 0)
				v = new Vector2();
			else
				v = s[0];

			if (i == -1)
				i = args.length / 2 - 1;

			v.x = ((Number) args[i * 2]).floatValue();
			v.y = ((Number) args[i * 2 + 1]).floatValue();

			return v;
		}

		public Vector2 getDestination() {
			return getDestination((Vector2[]) null);
		}

		public Vector3 getDestination3() {
			return LineUtils.getDestination3(this);
		}

		public Vector2 getAt() {
			return getDestination((Vector2[]) null);
		}

		public Vector2 getDestination(Vector2... s) {
			Vector2 v = null;
			if (s == null)
				v = new Vector2();
			else if (s.length == 0)
				v = new Vector2();
			else
				v = s[0];

			if (v == null)
				v = new Vector2();

			v.x = ((Number) args[args.length - 2]).floatValue();
			v.y = ((Number) args[args.length - 1]).floatValue();

			return v;
		}

		public void setAt(Vector2 last) {
			setAt(-1, last);
		}

		public void setAt(int lastIndexToSet, Vector2 last) {
			if (lastIndexToSet == -1)
				lastIndexToSet = args.length / 2 - 1;
			args[2 * lastIndexToSet] = (T) new Float(last.x);
			args[2 * lastIndexToSet + 1] = (T) new Float(last.y);
		}

		public void setAt3(int lastIndexToSet, Vector3 last) {
			if (lastIndexToSet == -1)
				lastIndexToSet = args.length / 2 - 1;

			args[2 * lastIndexToSet] = (T) new Float(last.x);
			args[2 * lastIndexToSet + 1] = (T) new Float(last.y);

			Object zwas = getAttributes().get(iLinearGraphicsContext.z_v);

			if (zwas == null) {
				if (args.length == 6) {
					zwas = new Vector3();
					getAttributes().put(iLinearGraphicsContext.z_v, zwas);
					if (zwas instanceof Vector3)
						((Vector3) zwas).set(lastIndexToSet, last.z);
					else
						getAttributes().put(iLinearGraphicsContext.z_v, last.z);
				} else {
					getAttributes().put(iLinearGraphicsContext.z_v, last.z);
				}
			} else {
				if (args.length == 6) {
					if (zwas instanceof Vector3)
						((Vector3) zwas).set(lastIndexToSet, last.z);
					else
						getAttributes().put(iLinearGraphicsContext.z_v, last.z);
				} else {
					getAttributes().put(iLinearGraphicsContext.z_v, last.z);
				}
			}

		}

		public boolean hasDestination() {
			return args != null && args.length > 0;
		}

		public CachedLine getContainer() {
			return container;
		}

		public void setContainer(CachedLine c) {
			container = c;
		}

		public Dict getDict() {
			return getAttributes();
		}

		// public boolean checkArgs() {
		// if ((args) instanceof float[]) {
		// float[] fa = ((float[]) (args));
		// Object[] oa = new Object[fa.length];
		// for (int i = 0; i < fa.length; i++) {
		// oa[i] = fa[i];
		// }
		// args = (T[]) oa;
		// return true;
		// }
		// return false;
		// }
	}

	public List<Event> events = new ArrayList<Event>();

	Event<?> currentEvent;

	public boolean finalized = false;

	Dict properties = new Dict();

	transient iLine cachedInput = null;

	public iLine getInput() {
		if (cachedInput != null)
			return cachedInput;
		return cachedInput = (iLine) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { iLine.class }, new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

				if (!method.equals(iLine_m.setPointAttribute_m)) {
					Event e = new Event();
					e.method = method;
					e.args = args;

					addEvent(e);
				} else {
					currentEvent.getAttributes().put((Prop) args[0], args[1]);
				}

				return null;
			}
		});
	}

	public void finish() {
		if (finalized)
			return;

		// interpolate all attribute classes linearly

		HashMap<Prop, Integer> lastSeenAt = new HashMap<Prop, Integer>(4);
		int lastMoveToAt = 0;

		for (int i = 0; i < events.size(); i++) {
			Event<?> e = events.get(i);

			if (e.method.equals(iLine_m.moveTo_m)) {
				lastMoveToAt = i;
				lastSeenAt.clear();
			}
			if (e.attributes != null) {
				Map<Prop, Object> map = e.attributes.getMap();
				for (Map.Entry<Prop, Object> a : map.entrySet()) {
					try {
						interpolateAttribute(a.getKey(), a.getValue(), lastSeenAt, lastMoveToAt, i, events);
					} catch (NotInterpolateable ew) {
					}
				}
			}

		}

	}

	// public void checkArgs() {
	// for (Event e : events) {
	// e.checkArgs();
	// }
	// }

	public Dict getProperties() {
		return properties;
	}

	protected void interpolateAttribute(Prop property, Object propertyValue, HashMap<Prop, Integer> lastSeenAt, int lastMoveToAt, int currentIndex, List<Event> eventStack) throws NotInterpolateable {

		// strings are not interpolated
		if (propertyValue instanceof String)
			return;

		// fonts are not interpolated
		if (propertyValue instanceof Font)
			return;

		// backward looking
		int backAt = lastMoveToAt;
		Object backValue = null;
		if (lastSeenAt.containsKey(property)) {
			backAt = lastSeenAt.get(property);
			backValue = eventStack.get(lastSeenAt.get(property)).attributes.get(property);
		}

		int forwardAt = currentIndex;
		Object forwardValue = null;

		for (int i = currentIndex + 1; i < eventStack.size(); i++) {
			if (events.get(i).method.equals(iLine_m.moveTo_m)) {
				forwardAt = i - 1;
				break;
			} else if (events.get(i).attributes != null) {
				if (events.get(i).attributes.getMap().containsKey(property)) {
					forwardAt = currentIndex;
					forwardValue = events.get(i).attributes.getMap().get(property);
					lastSeenAt.put(property, forwardAt);
					break;
				}
			}
		}

		interpolateAttribute(property, backAt, backValue, currentIndex, propertyValue, forwardAt, forwardValue, eventStack);
	}

	protected void interpolateAttribute(Prop property, int backAt, Object backValue, int currentIndex, Object propertyValue, int forwardAt, Object fowardValue, List<Event> eventStack) throws NotInterpolateable {
		try {
			for (int i = backAt; i < currentIndex; i++) {
				Object in = interpolateProperty(property, backValue, propertyValue, (i - backAt) / (float) (currentIndex - backAt));
				if (in != null) {
					eventStack.get(i).getAttributes().put(property, in);
				}
			}

			for (int i = currentIndex + 1; i < forwardAt + 1; i++) {
				Object in = interpolateProperty(property, propertyValue, fowardValue, (i - currentIndex) / (float) (forwardAt - currentIndex));
				if (in != null) {
					eventStack.get(i).getAttributes().put(property, in);
				}
			}
		} catch (NullPointerException e) {
			System.out.println(" problem while interpolating property <"+property+">");
			e.printStackTrace();
		}
	}

	class NotInterpolateable extends Exception {
	}

	protected Object interpolateProperty(Prop property, Object a, Object b, float alpha) throws NotInterpolateable {
		if (a == null)
			return b;
		if (b == null)
			return a;

		Object oa = a;

		if (property.getName().endsWith(".noInterpolate"))
			return null;

		if (!(a instanceof float[]))
			a = convert(a);
		if (!(b instanceof float[]))
			b = convert(b);

		if (a == null)
			throw new NotInterpolateable();
		if (b == null)
			throw new NotInterpolateable();

		float[] fa = (float[]) a;
		float[] fb = (float[]) b;

		int min = Math.min(fa.length, fb.length);
		int max = Math.min(fa.length, fb.length);

		float[] r = new float[max];

		for (int i = 0; i < max; i++) {
			if (i < min) {
				r[i] = fa[i] * (1 - alpha) + alpha * fb[i];
			} else {
				r[i] = i < fa.length ? fa[i] : fb[i];
			}
		}

		return convertBack(oa, r);
	}

	protected float[] convert(Object o) {
		if (o instanceof iToFloatArray)
			return (float[]) ((iToFloatArray) o).get();
		if (o instanceof Float)
			return new float[] { ((Number) o).floatValue() };
		if (o instanceof Double)
			return new float[] { ((Number) o).floatValue() };
		return null;
	}

	protected Object convertBack(Object o, float[] now) {
		if (o instanceof Vector4)
			return new Vector4(now[0], now[1], now[2], now[3]);
		if (o instanceof Float)
			return now[0];
		if (o instanceof Double)
			return now[0];
		return o;
	}

	protected void addEvent(Event e) {
		events.add(e);
		currentEvent = e;
	}

	public void getClassCustomCompletion(String prefix, Object o) {
		System.out.println(" gccc <" + prefix + "> <" + o + ">");
	}

	int modCount = 0;

	public int getModCount() {
		return modCount;
	}

	public void mod() {
		modCount++;
	}

}
