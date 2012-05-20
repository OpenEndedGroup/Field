package field.core.plugins.log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.python.core.Py;
import org.python.core.PyFunction;
import org.python.core.PyInstance;
import org.python.core.PyObject;

import field.core.execution.PythonInterface;
import field.core.plugins.log.ElementInvocationLogging.iProvidesContextStack;
import field.core.plugins.log.Logging.iLoggingEvent;
import field.core.plugins.python.PythonPlugin.CapturedEnvironment;
import field.launch.Launcher;
import field.launch.iLaunchable;
import field.launch.iUpdateable;
import field.math.abstraction.iBlendAlgebra;
import field.math.abstraction.iBlendable;
import field.math.abstraction.iProvider;
import field.math.graph.GraphNodeSearching;
import field.math.graph.NodeImpl;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.namespace.generic.ReflectionTools;
import field.util.Dict;
import field.util.Dict.Prop;

public class AssemblingLogging extends InvocationLogging {

	public interface iBlendSupport<T> {

		public T blend(List<T> t, List<Number> weights);

		public boolean isBlendSupported(Object o);

	}

	public interface iCloneSupport<T> {
		public boolean isCloneSupported(Object o);

		public T save(T t);
	}

	public interface iConstructionSupport<T> {
		public String construct(T t);

		public boolean isConstructionSupported(Object o);
	}

	public class ImmutableCloner<T> implements iCloneSupport<T> {
		private final Class<T> c;

		public ImmutableCloner(Class<T> c) {
			this.c = c;
		}

		public boolean isCloneSupported(Object o) {
			if (o == null)
				return false;
			return c.isAssignableFrom(o.getClass());
		}

		public T save(T t) {
			return t;
		}
	}

	public interface iMutationSupport<T> {
		public boolean isMutationSupported(Object o);

		// as in
		// .setValue(Vector3(1,1,1))

		// as in just
		// call
		// target.setValue(value);
		public void mutate(T target, T value);

		public void mutate(T target, T value, float amount);

		public String mutationExpression(T target, T value, String valueText);
	}

	public interface iProvidesSimpleChange {
		public SimpleChange getSimpleChange();
	}

	public interface iUndoable {
		public void executeSimpleUndo();

		public String getDoExpression();

		public String getUndoExpression();
	}

	public class Link {
		LinkType type;

		String name;

		ArrayList<Object> args;

		Object before;

		Object current;

		Object to;

		Object after;

		String path;

		// should be reevaluated before application, i.e., use 'to'
		// rather than 'after' when applying
		boolean tainted = false;

		@Override
		public String toString() {
			String r = name + "(" + type + ", " + path + ")";
			if (type == LinkType.call)
				r += "args=" + args + " <" + before + " -> " + after + ">";
			if (type == LinkType.set)
				r += " <" + before + " <- " + to + " -> " + after + "> " + (before != current ? " (now <" + current + ">)" : "");
			if (type == LinkType.dot)
				r += " <" + before + ">" + (before != current ? " (now <" + current + ">)" : "");
			if (type == LinkType.origin)
				r += " currently <" + current + ">";
			return r;
		}

	}

	public class LinkNode extends NodeImpl<LinkNode> {
		Link link;

		boolean consumed = false;

		public LinkNode(Link l) {
			this.link = l;
		}

		@Override
		public String toString() {
			return link.toString();
		}
	}

	public enum LinkType {
		origin, dot, call, set;
	}

	public class Move<T> {

		ArrayList<Link> links = new ArrayList<Link>();

		T before;

		String expression;

		T after;

		// out of order
		// expressions
		// are typically
		// get's that
		// ultimately
		// had no side
		// effect (they
		// were printed,
		// or just
		// taken)
		// they almost
		// certainly
		// should be
		// filtered out,
		// unless one is
		// especially
		// interested in
		// them
		boolean isOutOfOrder;

		Object current;
	}

	public class MoveEvent implements Logging.iLoggingEvent, iUndoable, iProvidesSimpleChange, iProvidesContextStack {
		private final String prefix;

		Move move;

		LinkedHashSet<iLoggingEvent> suspendedContext = new LinkedHashSet<iLoggingEvent>();

		public MoveEvent(String prefix, Move move) {
			super();
			this.move = move;
			this.prefix = prefix;
		}

		public void executeSimpleUndo() {
			if (((Link) move.links.get(move.links.size() - 1)).type == LinkType.call) {
				Object current = move.current;
				Object before = move.before;
				Object after = move.after;

				iMutationSupport m = getMutationFor(current);
				if (m != null) {
					m.mutate(current, before);
				}

			} else if (((Link) move.links.get(move.links.size() - 1)).type == LinkType.set) {

				Object current = move.current;
				Object before = move.before;
				Object after = move.after;

				ReflectionTools.illegalSetObjectPrimativeAware(((Link) move.links.get(move.links.size() - 2)).current, ((Link) move.links.get(move.links.size() - 1)).name, before);
			}
		}

		public String getDoExpression() {
			try {
				Object current = move.current;
				Object before = move.before;
				Object after = move.after;

				if (((Link) move.links.get(move.links.size() - 1)).type == LinkType.call) {
					iMutationSupport m = getMutationFor(current);
					return expressionForLink(move, move.links.size() - 3) + m.mutationExpression(current, after, getConstructionFor(after));
				}
				if (((Link) move.links.get(move.links.size() - 1)).type == LinkType.set) {
					return expressionForLink(move, move.links.size() - 2) + "." + ((Link) move.links.get(move.links.size() - 1)).name + " = " + getConstructionFor(after);
				}
			} catch (Throwable t) {
			}
			return null;
		}

		public String getDoExpressionLHS() {
			if (((Link) move.links.get(move.links.size() - 1)).type == LinkType.call) {
				return expressionForLink(move, move.links.size() - 3);
			}
			if (((Link) move.links.get(move.links.size() - 1)).type == LinkType.set) {
				return expressionForLink(move, move.links.size() - 2) + "." + ((Link) move.links.get(move.links.size() - 1)).name;
			}
			return null;
		}

		public String getLongDescription() {
			return prefix + "\n" + move.links.get(move.links.size() - 1) + "\n" + suspendedContext;
		}

		public String getReplayExpression() {
			return move.expression;
		}

		public SimpleChange getSimpleChange() {
			if (((Link) move.links.get(move.links.size() - 1)).type == LinkType.call) {

				final iMutationSupport m = getMutationFor(move.current);
				;//System.out.println(" got mutation support <" + m + "> for <" + move.current + "> rolling back to <" + move.before + ">");

				final Object tar = move.current;

				if (m != null) {

					return new SimpleChange(move.before, move.after, getDoExpressionLHS()) {

						@Override
						public iProvidesSimpleChange getSource() {
							return MoveEvent.this;
						}

						@Override
						public String toString() {
							return "call:" + getSource();
						}

						@Override
						public void writeChange(Object value) {
							m.mutate(tar, value);
						}

						@Override
						public void writeChange(Object value, float amount) {
							m.mutate(tar, value, amount);
						}

					};
				} else {
					;//System.out.println(" no simple change for <" + m + "> no mutation support");

					;//System.out.println(" trying deep change");
					Link link = (Link) move.links.get(move.links.size() - 1);

					;//System.out.println(" link target is <" + link.name + ">");
					;//System.out.println(" link arguments are <" + link.args + ">");

					final String methodName = ((Link) move.links.get(move.links.size() - 2)).name;
					final Object tt = link.current;

					return new SimpleChange(link.args, link.args, link.path) {

						@Override
						public iProvidesSimpleChange getSource() {
							return MoveEvent.this;
						}

						@Override
						public String toString() {
							return "call:" + getSource();
						}

						@Override
						public void writeChange(Object value) {

							ArrayList newArgs = (ArrayList) value;
							;//System.out.println("   looking for <" + methodName + "> in <" + tt + "> <" + tt.getClass() + ">");
							Method m = ReflectionTools.findFirstMethodCalled(tt.getClass(), methodName, newArgs.size());
							try {
								;//System.out.println(" calling <" + m + "> on <" + tt + "> with args <" + newArgs + ">");

								for (int i = 0; i < newArgs.size(); i++) {
									;//System.out.println("    arg <" + i + "> is <" + newArgs.get(i) + "> of class <" + (newArgs.get(i) == null ? null : newArgs.get(i).getClass()) + "    " + m.getParameterTypes()[i]);
								}

								m.invoke(tt, newArgs.toArray());
							} catch (IllegalArgumentException e) {
								e.printStackTrace();
							} catch (IllegalAccessException e) {
								e.printStackTrace();
							} catch (InvocationTargetException e) {
								e.printStackTrace();
							}
						}

						@Override
						public void writeChange(Object value, float amount) {

							ArrayList newArgs = (ArrayList) value;
							Method m = ReflectionTools.findFirstMethodCalled(tt.getClass(), methodName, newArgs.size());
							try {
								;//System.out.println(" calling <" + m + "> on <" + tt + "> with args <" + newArgs + ">");

								for (int i = 0; i < newArgs.size(); i++) {
									;//System.out.println("    arg <" + i + "> is <" + newArgs.get(i) + "> of class <" + (newArgs.get(i) == null ? null : newArgs.get(i).getClass()) + "    " + m.getParameterTypes()[i]);
								}

								m.invoke(tt, newArgs.toArray());
							} catch (IllegalArgumentException e) {
								e.printStackTrace();
							} catch (IllegalAccessException e) {
								e.printStackTrace();
							} catch (InvocationTargetException e) {
								e.printStackTrace();
							}
						}
					};
				}

			} else if (((Link) move.links.get(move.links.size() - 1)).type == LinkType.set) {

				final Object target = ((Link) move.links.get(move.links.size() - 2)).current;
				final String field = ((Link) move.links.get(move.links.size() - 1)).name;

				// current
				// becomes
				// null
				// for
				// set's
				// and
				// rightly
				// so,
				// for
				// we
				// have
				// overwritten
				// this
				// thing
				// not
				// mutated
				// it

				// what
				// we
				// need
				// to
				// do
				// is
				// set
				// the
				// reflected
				// field
				// on

				return new SimpleChange(move.before, move.after, getDoExpressionLHS()) {

					@Override
					public iProvidesSimpleChange getSource() {
						return MoveEvent.this;
					}

					@Override
					public String toString() {
						return "set:" + getSource();
					}

					@Override
					public void writeChange(Object value) {
						Object c = ((Link) move.links.get(move.links.size() - 2)).current;
						String n = ((Link) move.links.get(move.links.size() - 1)).name;

						if (c instanceof PyInstance) {
							((PyInstance) c).__setattr__(n, Py.java2py(value));
						} else if (c instanceof PyObject) {
							((PyObject) c).__setattr__(n, Py.java2py(value));
						} else {
							ReflectionTools.illegalSetObjectPrimativeAware(c, n, value);
						}
					}

					@Override
					public void writeChange(Object value, float amount) {
						Object c = ((Link) move.links.get(move.links.size() - 2)).current;
						String n = ((Link) move.links.get(move.links.size() - 1)).name;

						if (c instanceof PyInstance || c instanceof PyObject) {

							Object current = (((PyObject) c).__getattr__(n)).__tojava__(Object.class);

							iBlendSupport bs = getBlendFor(value);

							if (bs == null) {
								writeChange(value);
							} else {
								List in = new ArrayList();
								in.add(value);
								in.add(current);
								List weights = new ArrayList();
								weights.add(amount);
								weights.add(1 - amount);
								Object o = bs.blend(in, weights);

								writeChange(o);
							}

						} else {
							Object current = ReflectionTools.illegalGetObject(c, n);

							iBlendSupport bs = getBlendFor(value);

							if (bs == null)
								ReflectionTools.illegalSetObjectPrimativeAware(c, n, value);
							else {
								List in = new ArrayList();
								in.add(value);
								in.add(current);
								List weights = new ArrayList();
								weights.add(amount);
								weights.add(1 - amount);
								Object o = bs.blend(in, weights);
								ReflectionTools.illegalSetObjectPrimativeAware(c, n, o);
							}
						}
					}
				};
			}

			return null;
		}

		public LinkedHashSet<iLoggingEvent> getSuspendedContext() {
			return suspendedContext;
		}

		public String getTextDescription() {
			return (prefix.equals("") ? "" : (prefix + " \u2014 ")) + move.expression;
		}

		public String getUndoExpression() {
			try {
				Object current = move.current;
				Object before = move.before;
				Object after = move.after;

				iMutationSupport m = getMutationFor(current);

				if (((Link) move.links.get(move.links.size() - 1)).type == LinkType.call) {
					return expressionForLink(move, move.links.size() - 3) + m.mutationExpression(current, before, getConstructionFor(before));
				}
				if (((Link) move.links.get(move.links.size() - 1)).type == LinkType.set) {
					return expressionForLink(move, move.links.size() - 2) + "." + ((Link) move.links.get(move.links.size() - 1)).name + " = " + getConstructionFor(before);
				}
			} catch (Throwable t) {
			}

			return null;
		}

		public boolean isError() {
			return false;
		}

		@Override
		public String toString() {
			return "event:<'" + getDoExpression() + "' / '" + getUndoExpression() + "'>";
		}
	}

	static public class PartiallyEvaluatedFunction {
		Object evalautedTo;
		transient iProvider<Object> function;
		transient CapturedEnvironment env;

		Dict attributes = new Dict();

		public PartiallyEvaluatedFunction(Object j, final PyFunction f) {
			evalautedTo = j;
			function = new iProvider<Object>() {
				public Object get() {
					if (env != null)
						env.enter();
					try {
						return f.__call__().__tojava__(Object.class);
					} finally {
						if (env != null)
							env.exit();
					}
				}
			};

			PyObject dir = f.__dir__();
			for (int i = 0; i < dir.__len__(); i++) {
				PyObject key = dir.__getitem__(i);
				PyObject value = f.__dict__.__finditem__(key);
				if (value != null)
					attributes.put(new Prop((String) key.__tojava__(String.class)), value.__tojava__(Object.class));
			}

		}

		public Object call() {
			if (function != null)
				return function.get();
			return evalautedTo;
		}
	}

	public abstract class SimpleChange {
		Object value;

		Object previousValue;

		String target;

		public SimpleChange(Object previousValue, Object value, String target) {
			super();
			this.previousValue = previousValue;
			this.value = value;
			this.target = target;
		}

		public abstract iProvidesSimpleChange getSource();

		public Object getValue() {
			if (value instanceof PartiallyEvaluatedFunction) {
				return ((PartiallyEvaluatedFunction) value).evalautedTo;
			}
			return value;
		}

		public void update() {
			if (value instanceof PartiallyEvaluatedFunction) {
				((PartiallyEvaluatedFunction) value).evalautedTo = ((PartiallyEvaluatedFunction) value).call();
			}
		}

		public abstract void writeChange(Object value);

		public abstract void writeChange(Object value, float amount);
	}

	LinkedHashSet<LinkNode> roots = new LinkedHashSet<LinkNode>();

	IdentityHashMap<PyObject, LinkNode> currentLinkNodes = new IdentityHashMap<PyObject, LinkNode>();

	IdentityHashMap<PyObject, LinkNode> partialLinkNodes = new IdentityHashMap<PyObject, LinkNode>();

	boolean installed = false;

	Map<Class, iCloneSupport> cloneSupportCache = new HashMap<Class, iCloneSupport>();

	List<iCloneSupport> cloners = new ArrayList<iCloneSupport>();

	Map<Class, iConstructionSupport> constructionSupportCache = new HashMap<Class, iConstructionSupport>();

	List<iConstructionSupport> constructions = new ArrayList<iConstructionSupport>();

	Map<Class, iMutationSupport> mutationSupportCache = new HashMap<Class, iMutationSupport>();

	List<iMutationSupport> mutations = new ArrayList<iMutationSupport>();

	Map<Class, iBlendSupport> blendSupportCache = new HashMap<Class, iBlendSupport>();

	List<iBlendSupport> blendings = new ArrayList<iBlendSupport>();

	public AssemblingLogging() {
		// default set
		cloners.add(new iCloneSupport<Vector3>() {

			public boolean isCloneSupported(Object o) {
				return o instanceof Vector3;
			}

			public Vector3 save(Vector3 t) {
				return new Vector3(t);
			}
		});
		cloners.add(new iCloneSupport<Vector4>() {

			public boolean isCloneSupported(Object o) {
				return o instanceof Vector4;
			}

			public Vector4 save(Vector4 t) {
				return new Vector4(t);
			}
		});
		cloners.add(new ImmutableCloner<String>(String.class));
		cloners.add(new ImmutableCloner<Number>(Number.class));
		cloners.add(new ImmutableCloner<iLaunchable>(iLaunchable.class));

		cloners.add(new iCloneSupport() {
			public boolean isCloneSupported(Object o) {
				return o instanceof PyFunction;
			}

			public Object save(Object t) {
				PyFunction f = ((PyFunction) t);
				PyObject result = f.__call__();
				Object j = result.__tojava__(Object.class);
				return new PartiallyEvaluatedFunction(j, f);
			}
		});

		constructions.add(new iConstructionSupport<Vector3>() {
			public String construct(Vector3 t) {
				return "Vector3(" + t.x + ", " + t.y + ", " + t.z + ")";
			}

			public boolean isConstructionSupported(Object o) {
				return o instanceof Vector3;
			}
		});
		constructions.add(new iConstructionSupport<Vector4>() {
			public String construct(Vector4 t) {
				return "Vector4(" + t.x + ", " + t.y + ", " + t.z + ", " + t.w + " )";
			}

			public boolean isConstructionSupported(Object o) {
				return o instanceof Vector4;
			}
		});
		constructions.add(new iConstructionSupport<String>() {
			public String construct(String t) {
				return "\"" + t + "\"";
			}

			public boolean isConstructionSupported(Object o) {
				return o instanceof String;
			}
		});
		constructions.add(new iConstructionSupport<Number>() {
			public String construct(Number t) {
				return "" + t;
			}

			public boolean isConstructionSupported(Object o) {
				return o instanceof Number;
			}
		});

		mutations.add(new iMutationSupport<Vector3>() {
			public boolean isMutationSupported(Object o) {
				return o instanceof Vector3;
			}

			public void mutate(Vector3 target, Vector3 value) {
				target.setValue(value);
			}

			public void mutate(Vector3 target, Vector3 value, float amount) {
				target.lerp(target, value, amount);
			}

			public String mutationExpression(Vector3 target, Vector3 value, String valueText) {
				return ".setValue(" + valueText + ")";
			}

		});

		mutations.add(new iMutationSupport<Vector4>() {
			public boolean isMutationSupported(Object o) {
				return o instanceof Vector4;
			}

			public void mutate(Vector4 target, Vector4 value) {
				target.setValue(value);
			}

			public void mutate(Vector4 target, Vector4 value, float amount) {
				target.lerp(target, value, amount);
			}

			public String mutationExpression(Vector4 target, Vector4 value, String valueText) {
				return ".setValue(" + valueText + ")";
			}
		});

		blendings.add(new iBlendSupport<iBlendAlgebra>() {

			public iBlendAlgebra blend(List<iBlendAlgebra> t, List<Number> weights) {
				if (t.size() == 0)
					return null;
				iBlendAlgebra a = t.get(0);
				Object z = a.blendRepresentation_newZero();
				Iterator<Number> w = weights.iterator();
				float tot = 0;
				for (iBlendAlgebra b : t) {
					Number ww = w.next();
					z = b.blendRepresentation_add(z, b.blendRepresentation_multiply(ww.floatValue(), b.blendRepresentation_duplicate(b)), z);
					tot += ww.floatValue();
				}
				return (iBlendAlgebra) a.blendRepresentation_multiply(1 / tot, z);
			}

			public boolean isBlendSupported(Object o) {
				return o instanceof iBlendable;
			}

		});
		blendings.add(new iBlendSupport<Integer>() {

			public Integer blend(List<Integer> t, List<Number> weights) {
				if (t.size() == 0)
					return null;

				double z = 0;
				Iterator<Number> w = weights.iterator();
				float tot = 0;
				for (Number b : t) {
					Number ww = w.next();
					z += ww.doubleValue() * b.doubleValue();
					tot += ww.doubleValue();
				}

				// fixme

				Constructor[] c = t.get(0).getClass().getConstructors();
				for (Constructor cc : c) {
					if (cc.getParameterTypes().length == 1 && cc.getParameterTypes()[0].isPrimitive()) {
						try {
							return (Integer) cc.newInstance((int) (z / tot));
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (InstantiationException e) {
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						}
					}
				}
				return null;
			}

			public boolean isBlendSupported(Object o) {
				return o instanceof Integer;
			}
		});
		blendings.add(new iBlendSupport<Float>() {

			public Float blend(List<Float> t, List<Number> weights) {
				if (t.size() == 0)
					return null;

				double z = 0;
				Iterator<Number> w = weights.iterator();
				float tot = 0;
				for (Number b : t) {
					Number ww = w.next();
					z += ww.doubleValue() * b.doubleValue();
					tot += ww.doubleValue();
				}

				// fixme

				Constructor[] c = t.get(0).getClass().getConstructors();
				for (Constructor cc : c) {
					if (cc.getParameterTypes().length == 1 && cc.getParameterTypes()[0].isPrimitive()) {
						try {
							return (Float) cc.newInstance((float) (z / tot));
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (InstantiationException e) {
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						}
					}
				}
				return null;
			}

			public boolean isBlendSupported(Object o) {
				return o instanceof Float;
			}
		});

		blendings.add(new iBlendSupport<Double>() {

			public Double blend(List<Double> t, List<Number> weights) {
				if (t.size() == 0)
					return null;

				double z = 0;
				Iterator<Number> w = weights.iterator();
				float tot = 0;
				for (Number b : t) {
					Number ww = w.next();
					z += ww.doubleValue() * b.doubleValue();
					tot += ww.doubleValue();
				}

				// fixme

				Constructor[] c = t.get(0).getClass().getConstructors();
				for (Constructor cc : c) {
					if (cc.getParameterTypes().length == 1 && cc.getParameterTypes()[0].isPrimitive()) {
						try {
							return (Double) cc.newInstance((z / tot));
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (InstantiationException e) {
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						}
					}
				}
				return null;
			}

			public boolean isBlendSupported(Object o) {
				return o instanceof Double;
			}
		});

		blendings.add(new iBlendSupport<Number>() {

			public Number blend(List<Number> t, List<Number> weights) {
				if (t.size() == 0)
					return null;

				double z = 0;
				Iterator<Number> w = weights.iterator();
				float tot = 0;
				for (Number b : t) {
					Number ww = w.next();
					z += ww.doubleValue() * b.doubleValue();
					tot += ww.doubleValue();
				}

				// fixme

				Constructor[] c = t.get(0).getClass().getConstructors();
				for (Constructor cc : c) {
					if (cc.getParameterTypes().length == 1 && cc.getParameterTypes()[0].isPrimitive()) {
						try {
							return (Number) cc.newInstance(z / tot);
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (InstantiationException e) {
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						}
					}
				}
				return null;
			}

			public boolean isBlendSupported(Object o) {
				return o instanceof Number;
			}
		});

		blendings.add(new iBlendSupport<Object[]>() {

			public Object[] blend(List<Object[]> t, List<Number> weights) {
				if (t.size() == 0)
					return null;

				Object[] oo = t.get(0);

				Object[] ret = new Object[oo.length];
				for (int i = 0; i < oo.length; i++) {
					iBlendSupport sub = getBlendFor(oo[i]);
					List<Object> v = new ArrayList<Object>();
					for (int q = 0; q < t.size(); q++) {
						v.add(t.get(q)[i]);
					}
					Object m = sub.blend(v, weights);
					ret[i] = m;
				}

				return ret;
			}

			public boolean isBlendSupported(Object o) {
				return o instanceof Object[];
			}
		});
		blendings.add(new iBlendSupport<ArrayList>() {

			public ArrayList blend(List<ArrayList> t, List<Number> weights) {
				if (t.size() == 0)
					return null;

				ArrayList oo = t.get(0);

				ArrayList ret = new ArrayList(oo.size());
				for (int i = 0; i < oo.size(); i++) {
					iBlendSupport sub = getBlendFor(oo.get(i));
					List<Object> v = new ArrayList<Object>();
					for (int q = 0; q < t.size(); q++) {
						v.add(t.get(q).get(i));
					}
					Object m = sub.blend(v, weights);
					ret.add(m);
				}

				return ret;
			}

			public boolean isBlendSupported(Object o) {
				return o instanceof ArrayList;
			}
		});

	}

	public Move buildMoveFor(LinkNode leaf) {
		Move move = linearize(leaf);
		return move;
	}

	public void dumpDebugInfo() {
		;//System.out.println("-------------- assembling logging debug info ");
		;//System.out.println(" there are <" + roots.size() + "> roots, of <" + currentLinkNodes.size() + "> nodes (and <" + partialLinkNodes.size() + "> unfinished)");
		;//System.out.println(" walking nodes ... ");
		for (LinkNode a : roots)
			new GraphNodeSearching.GraphNodeVisitor_depthFirst<LinkNode>(false) {
				@Override
				protected VisitCode visit(LinkNode n) {
					;//System.out.println(spaces(stack.size() * 3) + " node <" + n + ">");
					return VisitCode.cont;
				}
			}.apply(a);
		;//System.out.println(" linearizing leaves ... ");
		for (LinkNode a : roots)
			new GraphNodeSearching.GraphNodeVisitor_depthFirst<LinkNode>(false) {
				@Override
				protected VisitCode visit(LinkNode n) {
					if (n.link.type == LinkType.set) {
						Move m = linearize(n);
					} else if (n.link.type == LinkType.call) {
						Move m = linearize(n);
					} else if (n.getChildren().size() == 0) {
						Move m = linearize(n);
					}
					return VisitCode.cont;
				}
			}.apply(a);
	}

	public String expressionForLink(Move m, int n) {
		String expression = "";
		assert n < m.links.size();
		for (int i = n; i >= 0; i--) {
			Link at = ((Link) m.links.get(i));

			if (at.type == LinkType.dot)
				expression = "." + at.name + expression;
			if (at.type == LinkType.origin)
				expression = at.path + expression;
			if (at.type == LinkType.set)
				expression = "." + at.name + expression + " = " + getConstructionFor(at.to);
			if (at.type == LinkType.call)
				expression = "(" + getConstructionsFor(at.args) + ")" + expression;
		}
		return expression;
	}

	public iBlendSupport getBlendFor(Object o) {
		if (o == null)
			return null;

		Class<? extends Object> c = o.getClass();
		iBlendSupport cached = blendSupportCache.get(c);
		if (cached != null)
			return cached;

		for (iBlendSupport cs : blendings) {
			boolean v = cs.isBlendSupported(o);
			if (v) {
				blendSupportCache.put(c, cs);
				return cs;
			}
		}

		;//System.out.println(" warning, no blend support for <" + o + "> of class <" + o.getClass() + ">");

		return null;
	}

	public ArrayList<SimpleChange> getSimpleChangeSet(ArrayList<Move> m) {
		ArrayList<SimpleChange> c = new ArrayList<SimpleChange>();
		for (Move move : m) {
			MoveEvent e = new MoveEvent("", move);
			SimpleChange s = e.getSimpleChange();
			if (s != null) {
				c.add(s);
			}
		}

		return c;
	}

	public void install() {

		if (!installed) {
			Launcher.getLauncher().registerUpdateable(new iUpdateable() {
				public void update() {

					ArrayList<Move> moves = resetReturningMoves();
					for (int i = 0; i < moves.size(); i++) {
						moves.get(i).isOutOfOrder = true;
						if (Logging.enabled())
						Logging.logging.addEvent(new MoveEvent("(out of order)", moves.get(i)));
					}
				}
			});
		}
		installed = true;
	}

	public Move linearize(LinkNode leaf) {
		Move m = new Move();
		String expression = "";

		LinkNode at = leaf;
		while (at != null) {
			if (at.link.type == LinkType.dot)
				expression = "." + at.link.name + expression;
			if (at.link.type == LinkType.origin)
				expression = at.link.path + expression;
			if (at.link.type == LinkType.set)
				expression = "." + at.link.name + expression + " = " + getConstructionFor(at.link.to);
			if (at.link.type == LinkType.call)
				expression = "(" + getConstructionsFor(at.link.args) + ")" + expression;

//			;//System.out.println(" link <" + at + "> expression so far <" + expression + ">");

			m.links.add(at.link);

			at.consumed = true;

			if (at.getParents() == null)
				at = null;
			else if (at.getParents().size() == 0)
				at = null;
			else
				at = (LinkNode) at.getParents().get(0);
		}
		;//System.out.println(" finished ");

		Collections.reverse(m.links);

		m.expression = expression;
		m.before = leaf.link.before;
		m.current = (leaf.link.current);
		m.after = leaf.link.tainted ? leaf.link.to : leaf.link.after;

		return m;

	}

	@Override
	public void linkCall(PyObject in, PyObject args, String path, boolean isRoot, Object result, PyObject from) {

		LinkNode parent = currentLinkNodes.get(from);
		if (parent == null) {

			assert isRoot : "call cannot be root";

			Link origin = new Link();
			origin.type = LinkType.origin;
			origin.name = path;
			origin.path = path;

			parent = new LinkNode(origin);
			currentLinkNodes.put(from, parent);
			roots.add(parent);
		}

		Link call = new Link();
		call.type = LinkType.call;
		call.name = null;
		call.args = new ArrayList<Object>();
		call.path = path;

		for (int i = 0; i < args.__len__(); i++) {
			call.args.add(args.__getitem__(i).__tojava__(Object.class));
		}

		LinkNode partialNode = new LinkNode(call);
		parent.addChild(partialNode);

		// the before is
		// two nodes up

		LinkNode invokingLink = (LinkNode) parent.getParents().get(0);

		assert invokingLink.link.type == LinkType.origin || invokingLink.link.type == LinkType.dot : invokingLink.link.type;

		call.before = getCloneFor(invokingLink.link.before);
		call.to = null;
		call.current = invokingLink.link.current;

		super.linkCall(in, args, path, isRoot, result, from);

		partialLinkNodes.put(from, partialNode);

	}

	@Override
	public void linkCallFinish(PyObject parent, PyObject newChild) {
		LinkNode childNode = partialLinkNodes.remove(parent);

		assert childNode != null;

		currentLinkNodes.put(newChild, childNode);

		LinkNode invokingLink = (LinkNode) currentLinkNodes.get(parent).getParents().get(0);

		childNode.link.after = getCloneFor(invokingLink.link.current);

		super.linkCallFinish(parent, newChild);

		if (installed)
			if (Logging.enabled())
			Logging.logging.addEvent(new MoveEvent("call", buildMoveFor(childNode)));

		callFinished(childNode);
	}

	@Override
	public PyObject linkGetAttr(PyObject in, String name, boolean isRoot, String path, PyObject from) {

		;//System.out.println(" link get attr in <" + this + ">");

		LinkNode parent = currentLinkNodes.get(from);
		if (parent == null) {
			if (!isRoot) {
				System.err.println(" warning, no root found for <" + name + "> <" + path + "> <" + from + ">, yet this is not marked as root ? ");
			}

			Link origin = new Link();
			origin.type = LinkType.origin;
			origin.name = path;
			origin.path = path;

			// not,
			// this
			// is
			// deliberatly
			// uncloned
			// ---
			// we
			// need
			// access
			// to
			// the
			// live
			// mutating
			// object
			// in
			// linkCall()
			origin.current = in.__tojava__(Object.class);

			parent = new LinkNode(origin);
			currentLinkNodes.put(from, parent);
			roots.add(parent);

		}

		Link get = new Link();
		get.type = LinkType.dot;
		get.name = name;
		get.path = path;

		LinkNode partialNode = new LinkNode(get);

		parent.addChild(partialNode);

		partialLinkNodes.put(from, partialNode);

		PyObject ret = super.linkGetAttr(in, name, isRoot, path, from);
		get.before = getCloneFor(get.current = ret.__tojava__(Object.class));

		return ret;

	}

	@Override
	public void linkGetAttrFinish(PyObject parent, PyObject newChild) {
		LinkNode childNode = partialLinkNodes.remove(parent);

		assert childNode != null;

		currentLinkNodes.put(newChild, childNode);

		super.linkGetAttrFinish(parent, newChild);
	}

	@Override
	public void linkSetAttr(PyObject in, String name, PyObject value, boolean isRoot, String path, PyObject from) {

		LinkNode parent = currentLinkNodes.get(from);
		if (parent == null) {
			if (!isRoot) {
				System.err.println(" warning, no root found for <" + name + "> <" + path + "> <" + from + ">, yet this is not marked as root ? ");
			}

			Link origin = new Link();
			origin.type = LinkType.origin;
			origin.name = path;
			origin.path = path;
			// note,
			// this
			// is
			// deliberatly
			// uncloned
			// ---
			// we
			// need
			// access
			// to
			// the
			// live
			// mutating
			// object
			// in
			// linkCall()
			origin.current = in.__tojava__(Object.class);

			parent = new LinkNode(origin);
			currentLinkNodes.put(from, parent);
			roots.add(parent);
		} else {
			if (isRoot) {
				assert parent.link.type == LinkType.origin : parent;
			}
		}

		final Link set = new Link();
		set.type = LinkType.set;
		set.name = name;
		set.path = path;

		LinkNode child = new LinkNode(set);
		parent.addChild(child);

		set.before = getCloneFor(super.linkGetAttr(in, name, isRoot, path, from).__tojava__(Object.class));
		set.to = getCloneFor(value != null ? value.__tojava__(Object.class) : value);
		set.current = value.__tojava__(Object.class);

		;//System.out.println(" before set <" + set.before + " " + set.to + " " + set.current + ">");

		super.linkSetAttr(in, name, value, isRoot, path, from, new iTypeErrorRecovery() {
			public void recover(Object javaObject, String field, Object setToBe) {

				Object c = getCloneFor(setToBe);
				if (c instanceof PartiallyEvaluatedFunction) {
					PartiallyEvaluatedFunction f = ((PartiallyEvaluatedFunction) c);

					f.env = (CapturedEnvironment) PythonInterface.getPythonInterface().getVariable("_environment");

					Object q = f.evalautedTo;
					ReflectionTools.illegalSetObjectPrimativeAware(javaObject, field, q);
					set.tainted = true;
				}
			}
		}, false);

		set.after = getCloneFor(super.linkGetAttr(in, name, isRoot, path, from).__tojava__(Object.class));

		;//System.out.println(" after set <" + set.after + ">");

		if (installed)
			if (Logging.enabled())
			Logging.logging.addEvent(new MoveEvent("set ", buildMoveFor(child)));

		setFinished(child);
	}

	public ArrayList<Move> resetReturningMoves() {
		final ArrayList<Move> moves = new ArrayList<Move>();
		for (LinkNode a : roots)
			new GraphNodeSearching.GraphNodeVisitor_depthFirst<LinkNode>(false) {
				@Override
				protected VisitCode visit(LinkNode n) {
					if (n.consumed)
						return VisitCode.cont;

					if (n.link.type == LinkType.set) {
						Move m = linearize(n);
						moves.add(m);
					} else if (n.link.type == LinkType.call) {
						Move m = linearize(n);
						moves.add(m);
					} else if (n.getChildren().size() == 0) {
						Move m = linearize(n);
						moves.add(m);
					}
					return VisitCode.cont;
				}
			}.apply(a);

		roots.clear();
		partialLinkNodes.clear();
		currentLinkNodes.clear();

		return moves;
	}

	protected void callFinished(LinkNode childNode) {
	}

	protected Object getCloneFor(Object o) {
		if (o == null)
			return null;

		Class<? extends Object> c = o.getClass();
		iCloneSupport cached = cloneSupportCache.get(c);
		if (cached != null)
			return cached.save(o);

		for (iCloneSupport cs : cloners) {
			boolean v = cs.isCloneSupported(o);
			if (v) {
				cloneSupportCache.put(c, cs);
				return cs.save(o);
			}
		}

		;//System.out.println(" warning, no clone support for <" + o + "> of class <" + o.getClass() + ">");

		return null;
	}

	protected String getConstructionFor(Object o) {
		if (o == null)
			return null;

		Class<? extends Object> c = o.getClass();
		iConstructionSupport cached = constructionSupportCache.get(c);
		if (cached != null)
			return cached.construct(o);

		for (iConstructionSupport cs : constructions) {
			boolean v = cs.isConstructionSupported(o);
			if (v) {
				constructionSupportCache.put(c, cs);
				return cs.construct(o);
			}
		}

		;//System.out.println(" warning, no construction support for <" + o + "> of class <" + o.getClass() + ">");

		return null;
	}

	protected String getConstructionsFor(ArrayList<Object> args) {
		String r = "";
		for (int i = 0; i < args.size(); i++) {
			Object o = args.get(i);
			String c = getConstructionFor(o);
			r += c;
			if (i < args.size() - 1)
				r += ", ";
		}
		return r;
	}

	protected iMutationSupport getMutationFor(Object o) {
		if (o == null)
			return null;

		Class<? extends Object> c = o.getClass();
		iMutationSupport cached = mutationSupportCache.get(c);
		if (cached != null)
			return cached;

		for (iMutationSupport cs : mutations) {
			boolean v = cs.isMutationSupported(o);
			if (v) {
				mutationSupportCache.put(c, cs);
				return cs;
			}
		}

		return null;
	}

	protected void setFinished(LinkNode childNode) {
	}

}
