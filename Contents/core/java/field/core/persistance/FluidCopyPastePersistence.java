package field.core.persistance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.ref.Reference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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

import field.core.dispatch.Mixins;
import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.Mixins.iMixinProxy;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElementOverrides.iDefaultOverride;
import field.core.persistance.FluidPersistence.iWellKnownElementResolver;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLineCompression;
import field.core.windowing.components.iComponent;
import field.namespace.generic.ReflectionTools;
import field.namespace.generic.Bind.iFunction;
import field.namespace.key.Key;
import field.util.ANSIColorUtils;

// FluidPersistence is to important to edit (right now)

public class FluidCopyPastePersistence {
	

	public interface iNotifyDuplication {
		public String beginNewUID(String uidToCopy);

		// we could use this to displace the frames for
		// example...
		public void endCopy(iVisualElement newCopy, iVisualElement old);
	}

	static public HashSet<iVisualElement> copyFromNonloadedPredicate(iFunction<Boolean, iVisualElement> shouldCopy, String sheetXMLname, iVisualElement graphRoot, FluidCopyPastePersistence ultimatePaster) {
		System.err.println(" inside copySource from nonloaded");
		try {

			File tmpFile = File.createTempFile("fieldtmp_", ".fieldcopypaste");
			FileWriter out = new FileWriter(tmpFile);

			FluidCopyPastePersistence pIn = new FluidCopyPastePersistence(new iWellKnownElementResolver() {
				public iVisualElement getWellKnownElement(String uid) {
					iVisualElement element = new VisualElement();
					element.setProperty(iVisualElement.name, "(well known mock object)");
					element.setUniqueID(uid);
					return element;
				}
			}, new iNotifyDuplication() {

				public String beginNewUID(String uidToCopy) {
					return uidToCopy;
				}

				public void endCopy(iVisualElement newCopy, iVisualElement old) {
				}
			});

			HashSet<iVisualElement> created = new HashSet<iVisualElement>();
			HashSet<iVisualElement> createdElements = new HashSet<iVisualElement>();

			ObjectInputStream in1 = pIn.getObjectInputStream(new FileReader(new File(sheetXMLname)), createdElements, created);
			String version = (String) in1.readObject();
			iVisualElement root = (iVisualElement) in1.readObject();
			in1.close();

			HashSet<iVisualElement> sub = new HashSet<iVisualElement>();

			for (iVisualElement e : createdElements) {
				if (shouldCopy == null || shouldCopy.f(e)) {
					sub.add(e);
				}
			}

			FluidCopyPastePersistence pOut = new FluidCopyPastePersistence(new iWellKnownElementResolver() {
				public iVisualElement getWellKnownElement(String uid) {
					iVisualElement element = new VisualElement();
					element.setProperty(iVisualElement.name, "(well known mock object)");
					element.setUniqueID(uid);
					return element;
				}
			}, new iNotifyDuplication() {

				public String beginNewUID(String uidToCopy) {
					return uidToCopy;
				}

				public void endCopy(iVisualElement newCopy, iVisualElement old) {
				}
			});

			HashSet<iVisualElement> saved = new HashSet<iVisualElement>();
			createdElements = new HashSet<iVisualElement>();

			ObjectOutputStream oos = pOut.getObjectOutputStream(out, saved, sub);
			oos.writeObject("tmp-snippet");
			oos.writeObject(sub);
			oos.close();

			System.err.println(" saved <" + sub + "> / <" + saved + ">");

			created = new HashSet<iVisualElement>();
			createdElements = new HashSet<iVisualElement>();

			ObjectInputStream in2 = ultimatePaster.getObjectInputStream(new FileReader(tmpFile), created, createdElements);
			String tmpSnippet = (String) in2.readObject();
			HashSet<iVisualElement> root2 = (HashSet<iVisualElement>) in2.readObject();
			in2.close();

			System.err.println(" final load <" + created + "> <" + createdElements + "> / <" + root2 + ">");

			return created;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		System.err.println(" inside copySource from nonloaded finished (failure)");
		return null;
	}

	static public HashSet<iVisualElement> copyFromNonloaded(final Set<String> uidSubset, String sheetXMLname, iVisualElement graphRoot, FluidCopyPastePersistence ultimatePaster) {
		return copyFromNonloadedPredicate(uidSubset == null ? null : new iFunction<Boolean, iVisualElement>() {
			public Boolean f(iVisualElement in) {
				return uidSubset.contains(in.getUniqueID());
			}
		}, sheetXMLname, graphRoot, ultimatePaster);
	}

	private final XStream stream;
	private Set<iVisualElement> created;
	private Set<iVisualElement> saved;

	private Set<iVisualElement> subsetToSave;

	private Set<String> subsetToLoad;

	private Map<String, iVisualElement> existing;

	protected UnmarshallingContext context;

	public FluidCopyPastePersistence(final iWellKnownElementResolver resolver, final iNotifyDuplication duplication) {
		this(resolver, duplication, 1);
	}

	public FluidCopyPastePersistence(final iWellKnownElementResolver resolver, final iNotifyDuplication duplication, int version) {
		stream = new XStream(new Sun14ReflectionProvider())
		{
			@Override
			protected MapperWrapper wrapMapper(MapperWrapper next) {
				return new MapperWrapper(next) {
					@Override
					public Class realClass(String elementName) {

						;//System.out.println(" looking for real class of <"+elementName+">");

						if (elementName.equals(iMixinProxy.class.getName().replace("$", "-")))
							return iMixinProxy.class;
						try {
							Class r = super.realClass(elementName);
							return r;
						} catch (CannotResolveClassException e) {
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

					Boolean doNot = element.getProperty(iVisualElement.doNotSave);
					if (doNot != null && doNot) {
						writer.startNode("aborted");
						writer.endNode();
					} else if (element.getUniqueID().startsWith("//")) {
						// that
						// 's
						// it
						// ,
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

					} else if (!subsetToSave.contains(element)) {
						writer.startNode("fringe");
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
						}
						writer.startNode("properties");
						context.convertAnother(properties);
						writer.endNode();
						writer.startNode("rect");
						context.convertAnother(element.getFrame(new Rect(0, 0, 0, 0)));
						writer.endNode();

						writer.startNode("parents");
						context.convertAnother(element.getParents());
						writer.endNode();
						writer.startNode("children");
						context.convertAnother(element.getChildren());
						writer.endNode();
					}
				} catch (RuntimeException t) {
					t.printStackTrace();
					;//System.out.println(ANSIColorUtils.red("<" + ReflectionTools.illegalGetObject(writer, "elementStack") + ">"));
				}
			}

			public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
				FluidCopyPastePersistence.this.context = context;
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
						reader.moveDown();
						if (reader.getNodeName().equals("aborted")) {
							reader.moveUp();
							return null;
						}

						if (reader.getNodeName().equals("fringe")) {
							reader.moveUp();
							iVisualElement existing = FluidCopyPastePersistence.this.existing.get(uid);
							return existing;
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
						ve.setUniqueID(duplication.beginNewUID(uid));
						if (properties != null)
							ve.setPayload(properties);
						ve.setFrame(r);

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

						duplication.endCopy(ve, existing.get(uid));

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
//				;//System.out.println(" context :"+source+" "+iVisualElementOverrides.iDefaultOverride.class.isAssignableFrom(source.getClass()));
//				iVisualElement e = ((iVisualElementOverrides.DefaultOverride) source).forElement;
//				;//System.out.println(" e:"+e);

				iVisualElement e = (iVisualElement) ReflectionTools.illegalGetObject(source, "forElement");

				;//System.out.println(" got element <"+e+"> from <"+source+"> : "+source.getClass()+">");

				context.convertAnother(e);
				writer.endNode();
			}

			public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
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
		}

	}

	public ObjectInputStream getObjectInputStream(Reader reader, Set<iVisualElement> created, Set<iVisualElement> existing) {
		this.created = created;
		this.existing = new LinkedHashMap<String, iVisualElement>();
		this.saved = null;
		this.subsetToSave = null;

		for (iVisualElement e : existing) {
			iVisualElement displaced = this.existing.put(e.getUniqueID(), e);
			assert displaced == null;
		}
		try {
			return stream.createObjectInputStream(reader);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public ObjectOutputStream getObjectOutputStream(Writer delegate, Set<iVisualElement> saved, Set<iVisualElement> subset) {
		this.saved = saved;
		this.subsetToSave = subset;
		try {
			return stream.createObjectOutputStream(delegate);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
