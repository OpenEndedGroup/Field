package field.core.persistance;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.python.core.PyObject;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.Sun14ReflectionProvider;
import com.thoughtworks.xstream.core.ReferenceByIdMarshallingStrategy;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import field.core.StandardFluidSheet;
import field.core.dispatch.Mixins;
import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.Mixins.iMixinProxy;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.iDefaultOverride;
import field.core.plugins.drawing.SplineComputingOverride;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLineCompression;
import field.core.util.FieldPyObjectAdaptor.PyVisualElement;
import field.core.windowing.components.iComponent;
import field.math.util.CubicInterpolatorDynamic;
import field.namespace.generic.ReflectionTools;
import field.namespace.key.Key;
import field.util.ANSIColorUtils;
import field.util.PythonUtils;

public class FluidPersistence {

	public interface iWellKnownElementResolver {
		public iVisualElement getWellKnownElement(String uid);
	}

	public interface iRequiresSerialization {
	}

	private final XStream stream;

	private Set<iVisualElement> created;

	private Set<iVisualElement> saved;

	protected UnmarshallingContext context;

	int version = 0;

	List<String> warnings = new ArrayList<String>();

	public FluidPersistence(final iWellKnownElementResolver resolver) {
		this(resolver, 0);
	}

	public FluidPersistence(final iWellKnownElementResolver resolver, int version) {

		stream = new XStream(new Sun14ReflectionProvider()) {
			@Override
			protected MapperWrapper wrapMapper(MapperWrapper next) {
				return new MapperWrapper(next) {
					@Override
					public Class realClass(String elementName) {
						
						if (elementName.equals("field.core.plugins.drawing.SplineComputingOverride$16"))
						{
							return SplineComputingOverride.PLineList.class;
						}
						
						if (elementName.equals("field.math.util.CubicInterpolatorDynamic$1"))
						{
							return CubicInterpolatorDynamic.MComparator.class;
						}
						
						if (elementName.equals(iMixinProxy.class.getName().replace("$", "-")))
							return iMixinProxy.class;
						try {
							Class r = super.realClass(elementName);
							return r;
						} catch (CannotResolveClassException e) {
							;//System.out.println(" context is <"+context+">");
							Class lookingFor = context.getRequiredType();
							;//System.out.println(" looking for <" + elementName + "> can't load it <" + e + ">, needed a type <" + lookingFor + "> guessing iVisualElement");
							return VisualElement.class;
						}
					}

					@Override
					public String serializedClass(Class type) {
						if (type == null)
							return super.serializedClass(type);
						if (type.getName().equals(iMixinProxy.class.getName()))
							return iMixinProxy.class.getName().replace("$", "-");
						if (iMixinProxy.class.isAssignableFrom(type))
							return iMixinProxy.class.getName().replace("$", "-");
						return super.serializedClass(type);
					}
				};
			}
		};

		
		
		stream.setMarshallingStrategy(new ReferenceByIdMarshallingStrategy());

		stream.registerConverter(new Converter() {

			public boolean canConvert(Class type) {
				return iVisualElement.class.isAssignableFrom(type);
			}

			public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
				try {
					iVisualElement element = (iVisualElement) source;
					writer.startNode("uid");
					writer.setValue(element.getUniqueID());
					writer.endNode();

					indent++;
					;//System.out.println(indentString()+" >>>>>>>>> saving element <"+element.getUniqueID()+"> <"+element+">");
					
					Boolean doNot = element.getProperty(iVisualElement.doNotSave);
					if (element.getUniqueID().startsWith("//")) {
						// that's
						// it,
						// no
						// more
						// for
						// this
						// one

						writer.startNode("parents");
						context.convertAnother(element.getParents());
						writer.endNode();
						writer.startNode("children");
						context.convertAnother(element.getChildren());
						writer.endNode();
					} else if (doNot != null && doNot) {
						writer.startNode("aborted");
						writer.endNode();
					} else {
						saved.add((iVisualElement) source);

						writer.startNode("class");
						context.convertAnother(element.getClass());
						writer.endNode();

						// get
						// all
						// the
						// properties
						// for
						// this
						// thing
						Map<Object, Object> properties = new HashMap<Object, Object>(element.payload());
						Iterator<Entry<Object, Object>> i = properties.entrySet().iterator();
						while (i.hasNext()) {
							Entry<Object, Object> e = i.next();
							if (((iVisualElement.VisualElementProperty<?>) e.getKey()).getName().endsWith("_"))
								i.remove();
							else if (!checkProperty(element, e)) {
								i.remove();
							}

						}
						;//System.out.println(indentString()+" -- properties");
						writer.startNode("properties");
						context.convertAnother(properties);
						writer.endNode();
						writer.startNode("rect");
						context.convertAnother(element.getFrame(new Rect(0, 0, 0, 0)));
						writer.endNode();

						;//System.out.println(indentString()+" -- parents");
						writer.startNode("parents");
						context.convertAnother(element.getParents());
						writer.endNode();
						;//System.out.println(indentString()+" -- children");
						writer.startNode("children");
						context.convertAnother(element.getChildren());
						writer.endNode();
					}
				} catch (RuntimeException t) {
					t.printStackTrace();
				}
				finally
				{
					;//System.out.println(indentString()+" <<<< <"+((iVisualElement)source).getUniqueID()+"> <"+((iVisualElement)source).getProperty(iVisualElement.name)+">");
					indent--;
				}
			}

			public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
				FluidPersistence.this.context = context;
				reader.moveDown();
				assert reader.getNodeName().equals("uid");
				String uid = reader.getValue();

				try {
					reader.moveUp();
					if (uid.startsWith("//")) {
						iVisualElement ve = resolver.getWellKnownElement(uid);
						if (ve == null)
							return null;

						reader.moveDown();
						List<iVisualElement> parents = (List) context.convertAnother(ve, List.class);
						reader.moveUp();
						reader.moveDown();
						List<iVisualElement> children = (List) context.convertAnother(ve, List.class);
						reader.moveUp();

						for (iVisualElement e : parents) {
							if (e != null)
								if (!e.getChildren().contains(ve)) {
									e.addChild(ve);
								}
						}
						for (iVisualElement e : children) {
							if (e != null)
								if (!ve.getChildren().contains(e)) {
									ve.addChild(e);
								}
						}
						return ve;
					}

					try {
						if (!reader.hasMoreChildren())
							return null;
						
						reader.moveDown();
						if (reader.getNodeName().equals("aborted")) {
							reader.moveUp();
							return null;
						}

						Class c = (Class) context.convertAnother(null, Class.class);
						reader.moveUp();

						iVisualElement ve = (iVisualElement) c.newInstance();

						reader.moveDown();
						Map<Object, Object> properties = null;
						try {
							properties = (Map<Object, Object>) context.convertAnother(ve, Map.class);
						} catch (Exception e) {
							e.printStackTrace();
							System.err.println(" .. continuing");
						}
						reader.moveUp();

						reader.moveDown();
						Rect r = (Rect) context.convertAnother(ve, Rect.class);
						reader.moveUp();
						created.add(ve);

						ve.setUniqueID(uid);
						ve.setFrame(r);
						if (properties != null)
							ve.setPayload(properties);

						reader.moveDown();
						List<iVisualElement> parents = (List) context.convertAnother(ve, List.class);
						reader.moveUp();
						reader.moveDown();
						List<iVisualElement> children = (List) context.convertAnother(ve, List.class);
						reader.moveUp();

						for (iVisualElement e : parents) {
							if (e != null) {
								if (!e.getChildren().contains(ve)) {
									e.addChild(ve);
								}
							}
						}
						for (iVisualElement e : children) {
							if (e != null) {
								if (!ve.getChildren().contains(e)) {
									ve.addChild(e);
								}
							}
						}

						return ve;
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}

					return null;
				} finally {
				}
			}

		});

		stream.registerConverter(new Converter() {

			public boolean canConvert(Class type) {
				return iComponent.class.isAssignableFrom(type);
			}

			public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
				iComponent component = (iComponent) source;
				writer.startNode("class");
				context.convertAnother(component.getClass());
				writer.endNode();

				if (component.getVisualElement() == null) {
					;//System.out.println(ANSIColorUtils.red(" persistance leak ? <" + ReflectionTools.illegalGetObject(writer, "elementStack") + ">"));
					System.exit(1);
				}

				writer.startNode("visualElement");
				context.convertAnother(component.getVisualElement());
				writer.endNode();
			}

			public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
				FluidPersistence.this.context = context;

				reader.moveDown();
				Class c = (Class) context.convertAnother(null, Class.class);
				reader.moveUp();

				try {
					iComponent o = (iComponent) c.newInstance();

					reader.moveDown();
					iVisualElement ve = (iVisualElement) context.convertAnother(o, iVisualElement.class);
					reader.moveUp();

					if (ve != null)
						o.setVisualElement(ve);
					return o;
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				return null;
			}

		});

		stream.registerConverter(new Converter() {

			public boolean canConvert(Class type) {
				return iVisualElementOverrides.iDefaultOverride.class.isAssignableFrom(type);
			}

			public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
				writer.startNode("class");
				context.convertAnother(source.getClass());
				writer.endNode();
				writer.startNode("element");
				context.convertAnother(((iVisualElementOverrides.DefaultOverride) source).forElement);
				writer.endNode();
			}

			public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
				FluidPersistence.this.context = context;
			reader.moveDown();
				Class c = (Class) context.convertAnother(null, Class.class);
				iDefaultOverride def = null;
				try {
					def = (iDefaultOverride) c.newInstance();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				reader.moveUp();

				reader.moveDown();
				iVisualElement ve = (iVisualElement) context.convertAnother(def, iVisualElement.class);
				def.setVisualElement(ve);
				reader.moveUp();
				return def;
			}
		});

		stream.registerConverter(new Converter() {

			public boolean canConvert(Class type) {
				return iMixinProxy.class.isAssignableFrom(type);
			}

			public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
				writer.startNode("class");
				context.convertAnother(((iMixinProxy) source).getMixinInterface());
				writer.endNode();
				writer.startNode("callList");
				List cl = ((iMixinProxy) source).getCallList();
				context.convertAnother(cl);
				writer.endNode();
			}

			public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
				FluidPersistence.this.context = context;
				reader.moveDown();
				Class cc = (Class) context.convertAnother(null, Class.class);
				reader.moveUp();
				iMixinProxy m = (iMixinProxy) new Mixins().make(cc, Mixins.visitCodeCombiner);
				reader.moveDown();
				List ll = (List) context.convertAnother(m, List.class);
				reader.moveUp();

				m.getCallList().addAll(ll);

				return m;
			}
		}, 100);

		stream.registerConverter(new Converter() {

			public boolean canConvert(Class type) {
				return Key.class.isAssignableFrom(type);
			}

			public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
				writer.startNode("keyName");
				writer.setValue(((Key) source).toString());
				writer.endNode();
			}

			public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
				FluidPersistence.this.context = context;
				reader.moveDown();
				String value = reader.getValue();
				reader.moveUp();
				if (value.endsWith(".transient")) {
					Reference<Key> o = (Reference<Key>) Key.internedKeys.get(value);
					if (o == null) {
						return null;
					}
					return o.get();

				} else {
					Reference<Key> o = (Reference<Key>) Key.internedKeys.get(value);
					assert o != null : "no key for <" + value + ">";
					return o.get();
				}
			}

		});

		if (version == 1) {

			stream.registerConverter(new Converter() {

				boolean inside = false;

				public boolean canConvert(Class type) {
					return !inside && CachedLine.class.isAssignableFrom(type);
				}

				public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
					writer.startNode("compressedCachedLine");
					writer.setValue(CachedLineCompression.compress((CachedLine) source));
					writer.endNode();
				}

				public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
					FluidPersistence.this.context = context;
					try {
						reader.moveDown();
						String value = reader.getValue();
						reader.moveUp();
						Object dc = CachedLineCompression.decompress(value);
						return dc;
					} catch (Throwable t) {
						t.printStackTrace();
						return null;
					}
				}

			});
			stream.registerConverter(new Converter() {

				boolean inside = false;

				ByteArrayOutputStream bos = new ByteArrayOutputStream(1024 * 1024);

				public boolean canConvert(Class type) {
					return !inside && iRequiresSerialization.class.isAssignableFrom(type);
				}

				public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
					writer.startNode("requiredserialization");

					bos.reset();
					try{
						ObjectOutputStream oos = new ObjectOutputStream(bos);
						oos.writeObject(source);
						oos.close();
						writer.setValue(new Base64().encode(bos.toByteArray()));
					}
					catch(IOException x)
					{
						x.printStackTrace();
					}
					writer.endNode();
				}

				public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
					FluidPersistence.this.context = context;
					try {
						reader.moveDown();
						String value = reader.getValue();
						reader.moveUp();

						//Object dc = CachedLineCompression.decompress(value);
						ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(new Base64().decode(value)));
						Object o = ois.readObject();
						ois.close();

						return o;
					} catch (Throwable t) {
						t.printStackTrace();
						return null;
					}
				}

			});
		}
	}

	public ObjectInputStream getObjectInputStream(Reader reader, Set<iVisualElement> created) {
		warnings.clear();
		this.created = created;
		try {
			return stream.createObjectInputStream(reader);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public ObjectOutputStream getObjectOutputStream(Writer delegate, Set<iVisualElement> saved) {

		warnings.clear();
		this.saved = saved;
		try {
			return stream.createObjectOutputStream(delegate);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public <T> T xmlDuplicate(T object) {
		try {
			StringWriter sw = new StringWriter();
			ObjectOutputStream s = getObjectOutputStream(sw, new HashSet<iVisualElement>());
			s.writeObject(object);
			s.close();
			ObjectInputStream i = getObjectInputStream(new StringReader(sw.toString()), new HashSet<iVisualElement>());
			Object object2 = i.readObject();
			i.close();
			return (T) object2;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected boolean checkProperty(iVisualElement element, Entry<Object, Object> e) {
		//beta1
		e.setValue(PythonUtils.maybeToJava(e.getValue()));
		if (e.getValue() instanceof PyObject) {
			warnings.add("warning: property " + e.getKey() + " inside " + element.payload().get(iVisualElement.name) + " is persistent, yet holds " + e.getValue() + " (of class " + e.getValue().getClass() + ")");
			return false;
		}
		if (e.getValue() instanceof PyVisualElement)
		{
			return checkVisualElementReference((VisualElementProperty) e.getKey(), element, (iVisualElement)((PyVisualElement)e.getValue()).__tojava__(VisualElement.class));
		}
		if (e.getValue() instanceof iVisualElement)
		{
			return checkVisualElementReference((VisualElementProperty) e.getKey(),  element, (iVisualElement)e.getValue());
		}

		
//		if (!(e.getValue() instanceof Serializable) && !e.getValue().getClass().getPackage().getName().startsWith("field.")) 
//		{
//			warnings.add("warning: property " + e.getKey() + " inside " + element.payload().get(iVisualElement.name) + " is persistent, yet holds " + e.getValue() + " (of class " + e.getValue().getClass() + ")");
//			return false;
//		}

		
		return true;
	}

	private boolean checkVisualElementReference(VisualElementProperty p, iVisualElement inside, iVisualElement potential) {
		List<iVisualElement> possible = StandardFluidSheet.allVisualElements(inside);
		if (!possible.contains(potential))
		{
			warnings.add("warning: property " + p+ " inside " + inside.payload().get(iVisualElement.name) + " is persistent, yet holds a cross sheet reference to " + potential.payload().get(iVisualElement.name));
			return false;
		}
		return true;
	}

	static public int indent = 0;
	static public String indentString()
	{
		StringBuffer ii = new StringBuffer(indent+1);
		for(int i=0;i<indent;i++)
		{
			ii.append(' ');
		}
		return ii.toString();
	}
}
