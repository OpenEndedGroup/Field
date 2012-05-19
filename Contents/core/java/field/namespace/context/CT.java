package field.namespace.context;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import field.bytecode.protect.BaseRef;
import field.math.graph.iTopology;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.namespace.dispatch.DispatchOverTopology;

/**
 * rewrite of context tree / merge with fluid sheet attributes
 *
 * @author marc
 *
 */
public class CT {

	static public abstract class ContextTopology<t_Key, t_Interface> {
		public iContextStorage<t_Key, t_Interface> storage;

		protected final Class<t_Key> keyClass;

		protected  Class<t_Interface> interfaceClass;

		ThreadLocal<t_Key> at = new ThreadLocal<t_Key>() {
			@Override
			protected t_Key initialValue() {
				return root();
			}
		};

		protected ContextTopology(Class<t_Key> keyClass, Class<t_Interface> interfaceClass) {
			this.keyClass = keyClass;
			this.interfaceClass = interfaceClass;

		}

		protected ContextTopology(Class<t_Key> keyClass) {
			this.keyClass = keyClass;
		}
		
		public void setInterfaceClass(Class<t_Interface> interfaceClass) {
			this.interfaceClass = interfaceClass;
		}

		abstract public Set<t_Key> childrenOf(t_Key p);

		public void delete(t_Key child) {
			throw new IllegalArgumentException(" not implemented (optional delete key)");
		}

		abstract public void deleteChild(t_Key parent, t_Key child);

		public t_Key getAt() {
			return at.get();
		}

		public Class<t_Interface> getInterfaceClass() {
			return interfaceClass;
		}

		public Class<t_Key> getKeyClass() {
			return keyClass;
		}

		abstract public Set<t_Key> parentsOf(t_Key k);

		abstract public t_Key root();

		// returns where
		// we were
		public t_Key setAt(t_Key k) {
			t_Key was = at.get();
			at.set(k);
			return was;
		}

	}

	public static class Dispatch<t_Key, t_Interface> {

		final ContextTopology<t_Key, t_Interface> topology;

		final iTopology<t_Key> topBackwards = new iTopology<t_Key>() {
			public List<t_Key> getChildrenOf(t_Key of) {
				Collection<t_Key> pp = topology.parentsOf(of);
				if (pp != null)
					return new ArrayList(pp);
				else
					return Collections.EMPTY_LIST;
			}

			public List<t_Key> getParentsOf(t_Key of) {
				Collection<t_Key> pp = topology.childrenOf(of);
				if (pp != null)
					return new ArrayList(pp);
				else
					return Collections.EMPTY_LIST;
			}

		};

		final iTopology<t_Key> top = new iTopology<t_Key>() {
			public List<t_Key> getChildrenOf(t_Key of) {
				Collection<t_Key> pp = topology.childrenOf(of);
				if (pp != null)
					return new ArrayList(pp);
				else
					return Collections.EMPTY_LIST;
			}

			public List<t_Key> getParentsOf(t_Key of) {
				Collection<t_Key> pp = topology.parentsOf(of);
				if (pp != null)
					return new ArrayList(pp);
				else
					return Collections.EMPTY_LIST;
			}
		};

		DispatchOverTopology<t_Key> dispatch = new DispatchOverTopology<t_Key>(top);

		DispatchOverTopology<t_Key> dispatchBackwards = new DispatchOverTopology<t_Key>(topBackwards);

		Method method;

		public Dispatch(ContextTopology<t_Key, t_Interface> topology) {
			this.topology = topology;
		}

		public Collection dispatchBackward(final Method m, Object... args) {
			DispatchOverTopology<t_Key>.Raw rawBackwards = getRawBackwards();
			VisitCode o = rawBackwards.dispatch(m, topology.getAt(), args);
			return rawBackwards.returns();
		}

		public Collection dispatchBackward(t_Key startingFrom, final Method m, Object... args) {
			DispatchOverTopology<t_Key>.Raw rawBackwards = getRawBackwards();
			VisitCode o = rawBackwards.dispatch(m, startingFrom, args);
			return rawBackwards.returns();
		}

		public Collection dispatchBackwardAbove(final t_Key startingFrom, final Method m, Object... args) {
			DispatchOverTopology<t_Key>.Raw rawBackwards = dispatchBackwards.new Raw(true) {
				@Override
				public Object getObject(t_Key e) {
					if (e == startingFrom)
						return null;
					return topology.storage.get(e, method);
				}
			};

			VisitCode o = rawBackwards.dispatch(m, startingFrom, args);
			return rawBackwards.returns();
		}

		public Collection dispatchForward(final Method m, Object... args) {
			DispatchOverTopology<t_Key>.Raw raw = getRaw();
			VisitCode o = raw.dispatch(m, topology.getAt(), args);
			return raw.returns();
		}

		public Collection dispatchForward(t_Key startingFrom, final Method m, Object... args) {
			DispatchOverTopology<t_Key>.Raw raw = getRaw();
			VisitCode o = raw.dispatch(m, startingFrom, args);
			return raw.returns();
		}

		public Collection dispatchForwardAbove(final t_Key startingFrom, final Method m, Object... args) {
			DispatchOverTopology<t_Key>.Raw raw = dispatch.new Raw(true) {
				@Override
				public Object getObject(t_Key e) {
					if (e == startingFrom)
						return null;
					return topology.storage.get(e, method);
				}
			};

			VisitCode o = raw.dispatch(m, startingFrom, args);
			return raw.returns();
		}

		public <T> T getAboveOverrideProxyFor(Class<T> interf) {

			Object p = Proxy.newProxyInstance(interf.getClassLoader(), new Class[] { interf }, new InvocationHandler() {

				public Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable {
					method = arg1;
					arg1.setAccessible(true);

					final t_Key startingFrom = topology.getAt();
					DispatchOverTopology<t_Key>.Raw raw = dispatch.new Raw(true) {
						@Override
						public Object getObject(t_Key e) {
							if (e == startingFrom)
								return null;
							return topology.storage.get(e, method);
						}
					};
					VisitCode o = raw.dispatch(arg1, topology.getAt(), arg2);
					return o;
				}
			});
			return (T) p;
		}

		public <T> T getBackwardsOverrideProxyFor(Class<T> interf) {

			Object p = Proxy.newProxyInstance(interf.getClassLoader(), new Class[] { interf }, new InvocationHandler() {

				public Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable {
					method = arg1;
					arg1.setAccessible(true);
					DispatchOverTopology<t_Key>.Raw rawBackwards = getRawBackwards();
					VisitCode o = rawBackwards.dispatch(arg1, topology.getAt(), arg2);
					return o;
				}
			});
			return (T) p;
		}

		// what I really
		// want to right
		// here is
		// t_Interface
		// extends T, or
		// T super
		// t_Interface,
		// but I can't

		public <T> T getBackwardsOverrideProxyFor(final t_Key startAt, Class<T> interf) {

			Object p = Proxy.newProxyInstance(interf.getClassLoader(), new Class[] { interf }, new InvocationHandler() {

				public Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable {
					method = arg1;
					arg1.setAccessible(true);
					DispatchOverTopology<t_Key>.Raw rawBackwards = getRawBackwards();
					VisitCode o = rawBackwards.dispatch(arg1, startAt, arg2);
					return o;
				}
			});
			return (T) p;
		}

		public <T> T getOverrideProxyFor(Class<T> interf) {

			Object p = Proxy.newProxyInstance(interf.getClassLoader(), new Class[] { interf }, new InvocationHandler() {

				public Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable {
					method = arg1;
					arg1.setAccessible(true);
					DispatchOverTopology<t_Key>.Raw raw = getRaw();
					VisitCode o = raw.dispatch(arg1, topology.getAt(), arg2);
					return o;
				}
			});
			return (T) p;
		}

		public <T> T getOverrideProxyFor(final t_Key startAt, Class<T> interf) {

			Object p = Proxy.newProxyInstance(interf.getClassLoader(), new Class[] { interf }, new InvocationHandler() {

				public Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable {
					method = arg1;
					arg1.setAccessible(true);
					DispatchOverTopology<t_Key>.Raw raw = getRaw();
					VisitCode o = raw.dispatch(arg1, startAt, arg2);
					return o;
				}
			});
			return (T) p;
		}

		DispatchOverTopology<t_Key>.Raw getRaw() {
			return dispatch.new Raw(true) {
				@Override
				public Object getObject(t_Key e) {
					return topology.storage.get(e, method);
				}
			};
		}

		DispatchOverTopology<t_Key>.Raw getRawBackwards() {
			return dispatchBackwards.new Raw(true) {
				@Override
				public Object getObject(t_Key e) {
					return topology.storage.get(e, method);
				}
			};
		}
	}

	public interface iContextStorage<t_Key, t_Interface> {

		/**
		 * t_Interface has method m in it which returns VisitCode's
		 */
		public t_Interface get(t_Key at, Method m);
	}

	static public interface iProvidesContextTopology<t_Key, t_Interface> {
		public ContextTopology<t_Key, t_Interface> getContextTopology();
	}

	// some useful interfaces for t_Interface's to
	// implement
	public interface iStorage<T> {
		public VisitCode get(String name, BaseRef<? super T> result);

		public VisitCode set(String name, BaseRef<? extends T> value);

		public VisitCode unset(String key);
	}

	static public interface iSupportsBeginEnd<t_Key> {
		public void begin(t_Key b);

		public void end(t_Key b);

		public Class<t_Key> getBeginEndSupportedClass();
	}

	static public class SimpleKey<t_Key, T> {
		public final String name;

		private final ContextTopology<t_Key, iStorage> c;

		private final Dispatch<t_Key, ? extends iStorage> d;

		public SimpleKey(String name, ContextTopology<t_Key, ? extends iStorage> c) {
			this.name = name;
			this.c = (ContextTopology<t_Key, iStorage>) c;
			d = new Dispatch(this.c);
		}

		public T get() {
			BaseRef<T> ref = new BaseRef<T>(null);
			iStorage s = d.getOverrideProxyFor(iStorage.class);
			s.get(name, ref);
			return ref.get();
		}

		public T get(t_Key k) {
			t_Key was = c.getAt();
			c.setAt(k);
			try {
				BaseRef<T> ref = new BaseRef<T>(null);
				iStorage s = d.getOverrideProxyFor(iStorage.class);
				s.get(name, ref);
				return ref.get();
			} finally {
				c.setAt(was);
			}
		}

		public T set(T to) {
			BaseRef<T> ref = new BaseRef<T>(to);
			iStorage s = d.getOverrideProxyFor(iStorage.class);
			s.set(name, ref);
			return ref.get();
		}

		public T set(t_Key k, T to) {
			t_Key was = c.getAt();
			c.setAt(k);
			try {
				BaseRef<T> ref = new BaseRef<T>(to);
				iStorage s = d.getOverrideProxyFor(iStorage.class);
				s.set(name, ref);
				return ref.get();
			} finally {
				c.setAt(was);
			}
		}
	}

}
