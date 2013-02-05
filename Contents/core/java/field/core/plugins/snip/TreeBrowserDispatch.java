package field.core.plugins.snip;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.python.core.PyFunction;

import field.bytecode.protect.dispatch.Cont;
import field.bytecode.protect.dispatch.Cont.Run;
import field.core.execution.PythonInterface;
import field.core.util.PythonCallableMap;
import field.core.windowing.GLComponentWindow;
import field.graphics.core.BasicFrameBuffers.DoubleFrameBuffer;
import field.graphics.core.BasicFrameBuffers.SingleFrameBuffer;
import field.graphics.core.BasicSceneList;
import field.graphics.windowing.FullScreenCanvasSWT;
import field.namespace.generic.Bind.iFunction;
import field.util.HashMapOfLists;

public class TreeBrowserDispatch {
    
	static final TreeBrowserDispatch childrenOf = new TreeBrowserDispatch();
	static final TreeBrowserDispatch doubleClick = new TreeBrowserDispatch();
	static final TreeBrowserDispatch textFor = new TreeBrowserDispatch();
	static final TreeBrowserDispatch selectionText = new TreeBrowserDispatch();
    
	HashMapOfLists<Class, PythonCallableMap> dispatch = new HashMapOfLists<Class, PythonCallableMap>();
    
	public interface iLabelled {
		public String getLabel();
        
		public List<Object> getChildren();
	}
    
	static {
		textFor.register(Object.class, "tostring", new iFunction<Object, Object>() {
			@Override
			public Object f(Object in) {
                
				if (in instanceof iLabelled)
					return ((iLabelled) in).getLabel();
                
				Method m;
                
				String name = PythonInterface.getPythonInterface().getLocalDictionary().findReverse(in, in.getClass());
                
				try {
					m = in.getClass().getMethod("toString");
					Class<?> c = m.getDeclaringClass();
					if (c.equals(Object.class)) {
						String n = in.getClass().getName();
						String[] n2 = n.split("\\.");
						n = n2[n2.length - 1];
						return (name == null ? "" : ("<b>" + name + "</b> ")) + "(" + n + ")";
					}
					System.out.println(" custom tostring for <" + in.getClass() + "> <" + m + ">");
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return (name == null ? "" : ("<b>" + name + "</b> ")) + in;
			}
		});
        
		childrenOf.register(Object.class, "list", new iFunction<Object, Object>() {
            
			@Override
			public Object f(Object in) {
                
				System.out.println(" -- " + in + " -- :" + (in instanceof Iterable));
                
				if (in instanceof Iterable) {
					List<Object> o = new ArrayList<Object>();
					int num = 0;
					Iterator ii = ((Iterable) in).iterator();
					while (ii.hasNext() && num < 40) {
						o.add(ii.next());
						num++;
					}
					return o;
				}
				return null;
			}
		});
        
		childrenOf.register(Object.class, "sc", new iFunction<Object, Object>() {
			@Override
			public Object f(final Object in) {
                
				if (in instanceof iLabelled) {
					return ((iLabelled) in).getChildren();
				}
				if (in instanceof GLComponentWindow) {
					ArrayList<Object> a = new ArrayList<Object>();
					a.add(((GLComponentWindow) in).getSceneList());
					return a;
				} else if (in instanceof FullScreenCanvasSWT) {
					ArrayList<Object> a = new ArrayList<Object>();
                    
					a.add(new iLabelled() {
                        
						@Override
						public String getLabel() {
							return "<b>.getSceneList()</b>";
						}
                        
						@Override
						public List<Object> getChildren() {
							ArrayList<Object> oo = new ArrayList<Object>();
							oo.addAll(((FullScreenCanvasSWT) in).getSceneList().getChildren());
							return oo;
						}
					});
                    
					if (((FullScreenCanvasSWT) in).leftSceneList.getChildren().size() > 0) {
						a.add(new iLabelled() {
                            
							@Override
							public String getLabel() {
								return "Stereo left <b> scene list</b>";
							}
                            
							@Override
							public List<Object> getChildren() {
								ArrayList<Object> oo = new ArrayList<Object>();
								oo.addAll(((FullScreenCanvasSWT) in).leftSceneList.getChildren());
								return oo;
							}
						});
					}
					if (((FullScreenCanvasSWT) in).rightSceneList.getChildren().size() > 0) {
						a.add(new iLabelled() {
                            
							@Override
							public String getLabel() {
								return "Stereo right <b> scene list</b>";
							}
                            
							@Override
							public List<Object> getChildren() {
								ArrayList<Object> oo = new ArrayList<Object>();
								oo.addAll(((FullScreenCanvasSWT) in).leftSceneList.getChildren());
								return oo;
							}
						});
					}
                    
					HashMap<Method, List<Run>> m = Cont.instance_links.get(in);
					if (m != null) {
						List<Run> ff = m.get(FullScreenCanvasSWT.method_beforeFlush);
						if (ff != null) {
							a.add(labelledList("Updates (before flush)", ff));
						}
						ff = m.get(FullScreenCanvasSWT.method_beforeLeftFlush);
						if (ff != null) {
							a.add(labelledList("Updates (before flush left)", ff));
						}
						ff = m.get(FullScreenCanvasSWT.method_beforeRightFlush);
						if (ff != null) {
							a.add(labelledList("Updates (before flush right)", ff));
						}
					}
                    
					return a;
				} else if (in instanceof BasicSceneList) {
					ArrayList<Object> a = new ArrayList<Object>();
					a.addAll(((BasicSceneList) in).getChildren());
					return a;
				} else if (in instanceof SingleFrameBuffer) {
					ArrayList<Object> a = new ArrayList<Object>();
					a.add(((SingleFrameBuffer) in).getSceneList());
					return a;
				} else if (in instanceof DoubleFrameBuffer) {
					ArrayList<Object> a = new ArrayList<Object>();
					a.add(((DoubleFrameBuffer) in).getSceneList());
					return a;
				}
				return null;
			}
            
			private Object labelledList(final String string, final List<Run> ff) {
				return new iLabelled() {
                    
					@Override
					public String getLabel() {
						return string;
					}
                    
					@Override
					public List<Object> getChildren() {
                        
						List<Object> outer = new ArrayList<Object>();
						for (Run rr : ff) {
							try {
								Field f = rr.getClass().getDeclaredField("val$o");
								f.setAccessible(true);
								outer.add(f.get(rr));
							} catch (Throwable t) {
								t.printStackTrace();
								outer.add(rr);
							}
						}
                        
						return outer;
					}
				};
			}
		});
	}
    
	public void register(Class c, String name, iFunction<Object, Object> f) {
		List<PythonCallableMap> m = dispatch.getList(c);
		if (m == null) {
			dispatch.put(c, m = new ArrayList<PythonCallableMap>());
		}
		if (m.size() == 0) {
			m.add(new PythonCallableMap());
		}
        
		m.get(0).register(name, f);
	}
    
	public void register(Class c, PyFunction f) {
		List<PythonCallableMap> m = dispatch.getList(c);
		if (m == null) {
			dispatch.put(c, m = new ArrayList<PythonCallableMap>());
		}
		if (m.size() == 0) {
			m.add(new PythonCallableMap());
		}
        
		m.get(0).register(f);
	}
    
	public Object call(Object... args) {
		List<Class> l = linearize(args[0] == null ? Object.class : args[0].getClass());
		for (Class cc : l) {
			Collection<PythonCallableMap> o = dispatch.get(cc);
			if (o == null)
				continue;
			if (o.size() == 0)
				continue;
			for (PythonCallableMap map : o) {
				Object out = map.invoke(args);
				if (out != null)
					return out;
			}
		}
		return null;
	}
    
	public List<Object> gather(Object... args) {
		List<Object> r = new ArrayList<Object>();
		List<Class> l = linearize(args[0] == null ? Object.class : args[0].getClass());
		for (Class cc : l) {
			Collection<PythonCallableMap> o = dispatch.get(cc);
			if (o == null)
				continue;
			if (o.size() == 0)
				continue;
			for (PythonCallableMap map : o) {
				Collection<Object> out = map.gather(args);
				if (out != null)
					r.addAll(out);
			}
		}
		return r;
	}
    
	private List<Class> linearize(Class<? extends Object> c) {
		LinkedHashSet<Class> tot = new LinkedHashSet<Class>();
		while (c != null) {
			tot.add(c);
			for (Class cc : c.getInterfaces())
				tot.add(cc);
			c = c.getSuperclass();
		}
		return new ArrayList<Class>(tot);
	}
    
}
