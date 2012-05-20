package field.core.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.plugins.python.Action;
import field.core.plugins.python.OutputInsertsOnSheet;
import field.core.ui.NewInspector.ActionControl;
import field.core.ui.NewInspector.BooleanControl;
import field.core.ui.NewInspector.ColorControl;
import field.core.ui.NewInspector.InfoControl;
import field.core.ui.NewInspector.Inspected;
import field.core.ui.NewInspector.IntControl;
import field.core.ui.NewInspector.SetValueForControl;
import field.core.ui.NewInspector.Status;
import field.core.ui.NewInspector.TextControl;
import field.launch.iUpdateable;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Triple;

public class NewInspectorFromProperties_swing {

	public NewInspectorFromProperties_swing() {
	}

	static public LinkedHashMap<String, Class<? extends JComponent>> knownProperties = new LinkedHashMap<String, Class<? extends JComponent>>();
	static public LinkedHashMap<String, String> knownAliases = new LinkedHashMap<String, String>();

	static public List<Triple<String, LinkedHashMap<String, Class<? extends JComponent>>, Boolean>> activeSets = new ArrayList<Triple<String, LinkedHashMap<String, Class<? extends JComponent>>, Boolean>>();

	LinkedHashMap<String, Inspected> build = new LinkedHashMap<String, Inspected>();

	public void begin() {
		build.clear();
	}

	public void add(Inspected i) {
		Inspected b = build.get(i.name);
		if (b == null)
			build.put(i.name, i);
		else
			build.put(i.name, coallesce(b, i));
	}

	public List<Inspected> complete() {
		return new ArrayList<Inspected>(build.values());
	}

	public <A, B extends JComponent> Inspected<A, B> coallesce(final Inspected<A, B> a, final Inspected<A, B> b) {

		;//System.out.println(" coallescing <" + a + "> <" + b + ">");

		Inspected<A, B> ii = new Inspected<A, B>() {

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
		ii.name = a.name;
		ii.editor = a.editor;
		if (ii.editor instanceof SetValueForControl)
			((SetValueForControl) ii.editor).setValueForControl(ii);
		return ii;
	}

	public List<Inspected> rebuild(List<iVisualElement> sel) {

		if (sel.size()==0)
		{
			begin();
			return complete();
		}

		begin();
		
		Inspected<String, InfoControl> heading = getHeading("Basic");
		add(heading);
		for (final iVisualElement e : sel) {
			Inspected<String, TextControl> name = new Inspected<String, TextControl>() {

				@Override
				public String getValue() {
					return iVisualElement.name.get(e);
				}

				@Override
				public void setValue(String s) {
					iVisualElement.name.set(e, e, s);
				}

				@Override
				public Status getStatus() {
					return Status.valid;
				}

			};
			name.name = "name";
			name.editor = new TextControl(name);
			add(name);

			Inspected<Integer, IntControl> x = new Inspected<Integer, IntControl>() {

				@Override
				public Integer getValue() {
					return (int) (e.getFrame(null).x);
				}

				@Override
				public void setValue(Integer s) {

					Rect f = e.getFrame(null);
					f.x = s;

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
			x.name = "x";
			x.editor = new IntControl(x);
			add(x);

			Inspected<Integer, IntControl> y = new Inspected<Integer, IntControl>() {

				@Override
				public Integer getValue() {
					return (int) (e.getFrame(null).y);
				}

				@Override
				public void setValue(Integer s) {

					Rect f = e.getFrame(null);
					f.y = s;

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
			y.name = "y";
			y.editor = new IntControl(y);
			add(y);
			Inspected<Integer, IntControl> width = new Inspected<Integer, IntControl>() {

				@Override
				public Integer getValue() {
					return (int) (e.getFrame(null).w);
				}

				@Override
				public void setValue(Integer s) {

					if (s < 5)
						s = 5;

					Rect f = e.getFrame(null);
					f.w = s;

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
			width.name = "width";
			width.editor = new IntControl(width);
			add(width);

			Inspected<Integer, IntControl> height = new Inspected<Integer, IntControl>() {

				@Override
				public Integer getValue() {
					return (int) (e.getFrame(null).h);
				}

				@Override
				public void setValue(Integer s) {

					if (s < 5)
						s = 5;

					Rect f = e.getFrame(null);
					f.h = s;

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
			height.name = "height";
			height.editor = new IntControl(height);
			add(height);

//			Inspected<Vector4, ColorControl> color1 = new Inspected<Vector4, ColorControl>() {
//
//				@Override
//				public Vector4 getValue() {
//					Vector4 c = iVisualElement.color1.get(e);
//					return c == null ? new Vector4(0.5, 0.5, 0.5, 0f) : c;
//				}
//
//				@Override
//				public void setValue(Vector4 c) {
//
//					iVisualElement.color1.set(e, e, c);
//					iVisualElement.dirty.set(e, e, true);
//				}
//
//				@Override
//				public Status getStatus() {
//					Vector4 c = iVisualElement.color1.get(e);
//					return c == null ? Status.unset : Status.valid;
//				}
//			};
//			color1.name = "color1";
//			color1.editor = new ColorControl(color1);
//			add(color1);
//
//			Inspected<Vector4, ColorControl> color2 = new Inspected<Vector4, ColorControl>() {
//
//				@Override
//				public Vector4 getValue() {
//					Vector4 c = iVisualElement.color2.get(e);
//					return c == null ? new Vector4(0.5, 0.5, 0.5, 0f) : c;
//				}
//
//				@Override
//				public void setValue(Vector4 c) {
//
//					iVisualElement.color2.set(e, e, c);
//					iVisualElement.dirty.set(e, e, true);
//
//				}
//
//				@Override
//				public Status getStatus() {
//					Vector4 c = iVisualElement.color2.get(e);
//					return c == null ? Status.unset : Status.valid;
//				}
//			};
//			color2.name = "color2";
//			color2.editor = new ColorControl(color2);
//			add(color2);

			boolean first = true;

			Set<Entry<String, Class<? extends JComponent>>> kp = knownProperties.entrySet();
			for (Entry<String, Class<? extends JComponent>> ee : kp) {
				final VisualElementProperty prop = new VisualElementProperty(ee.getKey());
				Object n = prop.get(e);
				if (n == null)
					continue;

				Inspected k = new Inspected() {

					@Override
					public Object getValue() {
						return prop.get(e);
					}

					@Override
					public void setValue(Object v) {
						;//System.out.println(" setting value to <" + v + ">");
						prop.set(e, e, v);
					}

					@Override
					public Status getStatus() {
						return Status.valid;
					}
				};
				k.name = knownAliases.get(ee.getKey());
				try {
					k.editor = ee.getValue().getConstructor(Inspected.class).newInstance(k);
				} catch (IllegalArgumentException e1) {
					e1.printStackTrace();
					continue;
				} catch (SecurityException e1) {
					e1.printStackTrace();
					continue;
				} catch (InstantiationException e1) {
					e1.printStackTrace();
					continue;
				} catch (IllegalAccessException e1) {
					e1.printStackTrace();
					continue;
				} catch (InvocationTargetException e1) {
					e1.printStackTrace();
					continue;
				} catch (NoSuchMethodException e1) {
					e1.printStackTrace();
					continue;
				}
				add(k);
			}

			first = true;
			Map<Object, Object> m = e.payload();
			for (Entry<Object, Object> o : m.entrySet()) {
				if (o.getKey() instanceof VisualElementProperty && o.getValue() != null && ((VisualElementProperty) o.getKey()).containsSuffix("i")) {

					final VisualElementProperty p = ((VisualElementProperty) o.getKey());

					Class<? extends JComponent> c = componentForValue(o.getValue());
					if (c == null)
						continue;

					Inspected k = new Inspected() {

						@Override
						public Object getValue() {
							return p.get(e);
						}

						@Override
						public void setValue(Object v) {
							;//System.out.println(" setting value to <" + v + ">");
							p.set(e, e, v);
						}

						@Override
						public Status getStatus() {
							return Status.valid;
						}
					};
					k.name = p.getName();
					try {
						k.editor = c.getConstructor(Inspected.class).newInstance(k);
					} catch (IllegalArgumentException e1) {
						e1.printStackTrace();
						continue;
					} catch (SecurityException e1) {
						e1.printStackTrace();
						continue;
					} catch (InstantiationException e1) {
						e1.printStackTrace();
						continue;
					} catch (IllegalAccessException e1) {
						e1.printStackTrace();
						continue;
					} catch (InvocationTargetException e1) {
						e1.printStackTrace();
						continue;
					} catch (NoSuchMethodException e1) {
						e1.printStackTrace();
						continue;
					}
					if (first)
					{
						first = false;
						add(getHeading("Properties ending in _i"));
					}
					
					add(k);
				}
			}
			
			// actually add this set
			
			for(final Triple<String, LinkedHashMap<String, Class<? extends JComponent>>, Boolean> t : activeSets)
			{
				if (!t.right) continue;
				add(getHeading(t.left));
				for(Map.Entry<String, Class<? extends JComponent>> ee : t.middle.entrySet())
				{
					final VisualElementProperty p = ((VisualElementProperty) new VisualElementProperty(ee.getKey()));

					Inspected k = new Inspected() {

						@Override
						public Object getValue() {
							return p.get(e);
						}

						@Override
						public void setValue(Object v) {
							;//System.out.println(" setting value to <" + v + ">");
							p.set(e, e, v);
						}

						@Override
						public Status getStatus() {
							return p.get(e)!=null ? Status.valid : Status.unset;
						}
					};
					k.name = p.getName();
					
					;//System.out.println(" about to make something for <"+k.name+">");
					
					try {
						k.editor = ee.getValue().getConstructor(Inspected.class).newInstance(k);
						
						setPopupMenu(k.editor, p, e);
						
						;//System.out.println(" making <"+k.editor+">");
						
					} catch (IllegalArgumentException e1) {
						e1.printStackTrace();
						continue;
					} catch (SecurityException e1) {
						e1.printStackTrace();
						continue;
					} catch (InstantiationException e1) {
						e1.printStackTrace();
						continue;
					} catch (IllegalAccessException e1) {
						e1.printStackTrace();
						continue;
					} catch (InvocationTargetException e1) {
						e1.printStackTrace();
						continue;
					} catch (NoSuchMethodException e1) {
						e1.printStackTrace();
						continue;
					}
					
					add(k);
				}
			}
		}

		return complete();
	}

	private void setPopupMenu(JComponent editor, final VisualElementProperty p, final iVisualElement e) {
		
		LinkedHashMap<String, iUpdateable> pop = new LinkedHashMap<String, iUpdateable>();
		
		pop.put("Property", null);
		pop.put("  Put property editor on canvas", new iUpdateable() {
			
			@Override
			public void update() {
				OutputInsertsOnSheet.printProperty(e, p.getName());
			}
		});
		
//		editor.setComponentPopupMenu(new SmallMenu().createMenu(pop));
	}

	private Inspected<String, InfoControl> getHeading(final String string) {
		Inspected<String, InfoControl> heading = new Inspected<String, InfoControl>() {

			@Override
			public String getValue() {
				return string;
			}

			@Override
			public void setValue(String s) {
			}

			@Override
			public Status getStatus() {
				return Status.valid;
			}

		};
		heading.name = string;
		heading.editor = new InfoControl(heading);
		return heading;
	}

	static public final  Class<? extends JComponent> componentForValue(Object value) {
		if (value instanceof String)
			return TextControl.class;
		if (value instanceof Integer)
			return IntControl.class;
		if (value instanceof Boolean)
			return BooleanControl.class;
		if (value instanceof Vector4)
			return ColorControl.class;
		if (value instanceof Action)
			return ActionControl.class;

		return null;
	}
	
}
