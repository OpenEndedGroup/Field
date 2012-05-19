package field.core.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.ui.NewInspector2.BaseControl;
import field.core.ui.NewInspector2.BooleanControl;
import field.core.ui.NewInspector2.ColorControl;
import field.core.ui.NewInspector2.InfoControl;
import field.core.ui.NewInspector2.SpinnerControl;
import field.core.ui.NewInspector2.Status;
import field.core.ui.NewInspector2.TextControl;
import field.core.ui.NewInspector2.iIO;
import field.launch.iUpdateable;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Triple;

public class NewInspectorFromProperties {

	private final NewInspector2 inside;

	public NewInspectorFromProperties(NewInspector2 inside) {
		this.inside = inside;
	}

	static public LinkedHashMap<String, Class<? extends BaseControl>> knownProperties = new LinkedHashMap<String, Class<? extends BaseControl>>();
	static public LinkedHashMap<String, String> knownAliases = new LinkedHashMap<String, String>();

	static public List<Triple<String, LinkedHashMap<String, Class<? extends BaseControl>>, Boolean>> activeSets = new ArrayList<Triple<String, LinkedHashMap<String, Class<? extends BaseControl>>, Boolean>>();

	LinkedHashMap<String, iIO> build = new LinkedHashMap<String, iIO>();

	public void begin() {
		build.clear();
	}

	public void add(iIO i) {
		iIO b = build.get(i.name);
		if (b == null)
			build.put(i.name, i);
		else
			build.put(i.name, coallesce(b, i));
	}

	public ArrayList<BaseControl> complete() {
		ArrayList<iIO> m = new ArrayList<iIO>(build.values());
		ArrayList<BaseControl> i = new ArrayList<BaseControl>();

		if (m.size() == 0) {
			m.add(getHeading("Inspector"));
		}
		m.add(inside.getMenuItemsIO());

		for (iIO mm : m) {
			try {

				i.add((BaseControl) mm.editor.getDeclaredConstructors()[0].newInstance(inside, mm));
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		return i;

	}

	public <A> iIO<A> coallesce(final iIO<A> a, final iIO<A> b) {

		System.out.println(" coallescing <" + a + "> <" + b + ">");

		iIO<A> ii = new iIO<A>(a.name) {

			@Override
			public A getValue() {
				return a.getValue();
			}

			@Override
			public void setValue(A s) {
				a.setValue(s);
				b.setValue(s);
			}

			@Override
			public Status getStatus() {
				if (a.getStatus() == Status.unset)
					return Status.unset;
				if (b.getStatus() == Status.unset)
					return Status.unset;
				if (a.getStatus() == Status.mixed)
					return Status.mixed;
				if (b.getStatus() == Status.mixed)
					return Status.mixed;

				return a.getValue().equals(b.getValue()) ? Status.valid : Status.mixed;
			}

		};

		ii.editor = a.editor;

		return ii;
	}

	public ArrayList<BaseControl> rebuild(List<iVisualElement> sel) {

		if (sel.size() == 0) {
			begin();
			return complete();
		}

		begin();

		iIO<Object> heading = getHeading("Basic");
		add(heading);

		
		System.out.println(" hello ??? inside rebuild ");
		
		for (final iVisualElement e : sel) {

			iIO<String> name = new iIO<String>("name") {

				@Override
				public void setValue(String s) {
					iVisualElement.name.set(e, e, s);
				}

				@Override
				public String getValue() {
					return iVisualElement.name.get(e);
				}

				@Override
				public Status getStatus() {
					return Status.valid;
				}
			};

			name.editor = TextControl.class;
			add(name);

			

			iIO<Number> x = new iIO<Number>("x") {

				@Override
				public Number getValue() {
					return (float) (e.getFrame(null).x);
				}

				@Override
				public void setValue(Number s) {

					Rect f = e.getFrame(null);
					f.x = s.floatValue();

					iVisualElementOverrides.topology.begin(e);
					iVisualElementOverrides.forward.shouldChangeFrame.shouldChangeFrame(e, f, e.getFrame(null), true);
					iVisualElementOverrides.topology.end(e);
					iVisualElement.dirty.set(e, e, true);
				}

				@Override
				public Status getStatus() {
					return Status.valid;
				}
			};
			x.editor = SpinnerControl.class;
			add(x);

			iIO<Number> y = new iIO<Number>("y") {

				@Override
				public Number getValue() {
					return (float) (e.getFrame(null).y);
				}

				@Override
				public void setValue(Number s) {

					Rect f = e.getFrame(null);
					f.y = s.floatValue();

					iVisualElementOverrides.topology.begin(e);
					iVisualElementOverrides.forward.shouldChangeFrame.shouldChangeFrame(e, f, e.getFrame(null), true);
					iVisualElementOverrides.topology.end(e);
					iVisualElement.dirty.set(e, e, true);
				}

				@Override
				public Status getStatus() {
					return Status.valid;
				}
			};
			y.editor = SpinnerControl.class;
			add(y);

			iIO<Number> w = new iIO<Number>("width") {

				@Override
				public Number getValue() {
					return (float) (e.getFrame(null).w);
				}

				@Override
				public void setValue(Number s) {

					Rect f = e.getFrame(null);
					f.w = s.floatValue();

					iVisualElementOverrides.topology.begin(e);
					iVisualElementOverrides.forward.shouldChangeFrame.shouldChangeFrame(e, f, e.getFrame(null), true);
					iVisualElementOverrides.topology.end(e);
					iVisualElement.dirty.set(e, e, true);
				}

				@Override
				public Status getStatus() {
					return Status.valid;
				}
			};
			w.editor = SpinnerControl.class;
			add(w);
			iIO<Number> h = new iIO<Number>("height") {

				@Override
				public Number getValue() {
					return (float) (e.getFrame(null).h);
				}

				@Override
				public void setValue(Number s) {

					Rect f = e.getFrame(null);
					f.h = s.floatValue();

					iVisualElementOverrides.topology.begin(e);
					iVisualElementOverrides.forward.shouldChangeFrame.shouldChangeFrame(e, f, e.getFrame(null), true);
					iVisualElementOverrides.topology.end(e);
					iVisualElement.dirty.set(e, e, true);
				}

				@Override
				public Status getStatus() {
					return Status.valid;
				}
			};
			h.editor = SpinnerControl.class;
			add(h);

			iIO<String> boundTo = new iIO<String>("boundTo") {

				@Override
				public void setValue(String s) {

					iVisualElement.boundTo.set(e, e, s);
				}

				@Override
				public String getValue() {
					String m = iVisualElement.boundTo.get(e);
					if (m == null)
						return "";
					return m;
				}

				@Override
				public Status getStatus() {
					return Status.valid;
				}
			};

			boundTo.editor = TextControl.class;
			add(boundTo);
			
			boolean first = true;

			Set<Entry<String, Class<? extends BaseControl>>> kp = knownProperties.entrySet();
			for (Entry<String, Class<? extends BaseControl>> ee : kp) {
				final VisualElementProperty prop = new VisualElementProperty(ee.getKey());
				Object n = prop.get(e);
				if (n == null)
					continue;

				iIO k = new iIO(knownAliases.get(ee.getKey())) {

					@Override
					public Object getValue() {
						return prop.get(e);
					}

					@Override
					public void setValue(Object v) {
						System.out.println(" setting value to <" + v + ">");
						prop.set(e, e, v);
					}

					@Override
					public Status getStatus() {
						return Status.valid;
					}
				};
				k.editor = ee.getValue();

				add(k);
			}

			first = true;
			Map<Object, Object> m = e.payload();
			for (Entry<Object, Object> o : m.entrySet()) {
				if (o.getKey() instanceof VisualElementProperty && o.getValue() != null && ((VisualElementProperty) o.getKey()).containsSuffix("i")) {

					final VisualElementProperty p = ((VisualElementProperty) o.getKey());

					Class<? extends BaseControl> c = componentForValue(o.getValue());
					if (c == null)
						continue;

					iIO k = new iIO(p.getName()) {

						@Override
						public Object getValue() {
							return p.get(e);
						}

						@Override
						public void setValue(Object v) {
							System.out.println(" setting value to <" + v + ">");
							p.set(e, e, v);
						}

						@Override
						public Status getStatus() {
							return Status.valid;
						}
					};
					k.name = p.getName();
					k.editor = c;

					if (first) {
						first = false;
						add(getHeading("Properties ending in _i"));
					}

					add(k);
				}
			}

			// actually add this set

			for (final Triple<String, LinkedHashMap<String, Class<? extends BaseControl>>, Boolean> t : activeSets) {
				if (!t.right)
					continue;
				add(getHeading(t.left));
				for (Map.Entry<String, Class<? extends BaseControl>> ee : t.middle.entrySet()) {
					final VisualElementProperty p = ((VisualElementProperty) new VisualElementProperty(ee.getKey()));

					iIO k = new iIO(p.getName()) {

						@Override
						public Object getValue() {
							Object object = p.get(e);
							System.out.println(" >>>>>>>>>>>>>>>>> getting value <" + p + " = " + e + "> = " + object);
							return object;
						}

						@Override
						public void setValue(Object v) {
							System.out.println(" >>>>>>>>>>>>>>>>> setting value <" + p + " = " + e + "> = " + v);
							p.set(e, e, v);
						}

						@Override
						public Status getStatus() {
							return p.get(e) != null ? Status.valid : Status.unset;
						}
					};

					System.out.println(" about to make something for <" + k.name + ">");

					k.editor = ee.getValue();

					add(k);
				}
			}
		}

		return complete();
	}

	// TODO swt popup and put on canvas
	// private void setPopupMenu(JComponent editor, final
	// VisualElementProperty
	// p,
	// final iVisualElement e) {
	//
	// LinkedHashMap<String, iUpdateable> pop = new LinkedHashMap<String,
	// iUpdateable>();
	//
	// pop.put("Property", null);
	// pop.put("  Put property editor on canvas", new iUpdateable() {
	//
	// @Override
	// public void update() {
	// OutputInsertsOnSheet.printProperty(e, p.getName());
	// }
	// });
	//
	// editor.setComponentPopupMenu(new SmallMenu().createMenu(pop));
	// }

	private iIO<Object> getHeading(final String string) {
		iIO<Object> heading = new iIO<Object>(string) {

			@Override
			public Object getValue() {
				return "";
			}

			@Override
			public Status getStatus() {
				return Status.valid;
			}

			@Override
			public void setValue(Object s) {
			}
		};

		heading.editor = InfoControl.class;
		return heading;
	}

	static public final Class<? extends BaseControl> componentForValue(Object value) {
		if (value instanceof String)
			return TextControl.class;
		if (value instanceof Number)
			return SpinnerControl.class;
		if (value instanceof Boolean)
			return BooleanControl.class;
		if (value instanceof Vector4)
			return ColorControl.class;
		// if (value instanceof Action)
		// return ActionControl.class;

		return null;
	}

	public LinkedHashMap<String, iUpdateable> getMenuItems(final iUpdateable rebuild) {
		LinkedHashMap<String, iUpdateable> map = new LinkedHashMap<String, iUpdateable>();

		map.put("Additional property sets", null);

		for (final Triple<String, LinkedHashMap<String, Class<? extends BaseControl>>, Boolean> t : activeSets) {
			String x = t.right ? "!" : "";
			map.put(x + " \u26aa\t" + t.left, new iUpdateable() {

				@Override
				public void update() {
					t.right = !t.right;
					rebuild.update();
				}
			});
		}

		return map;
	}
}
