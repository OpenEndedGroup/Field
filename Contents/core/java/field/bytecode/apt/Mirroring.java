package field.bytecode.apt;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import field.launch.iUpdateable;
import field.math.abstraction.iAcceptor;
import field.math.abstraction.iDoubleProvider;
import field.math.abstraction.iFloatProvider;
import field.math.abstraction.iProvider;
import field.namespace.context.CT.ContextTopology;
import field.namespace.context.CT.Dispatch;
import field.namespace.generic.ReflectionTools;
import field.namespace.generic.Bind.iFunction;

/**
 * these classes are special, and are not to be refactored without looking at
 * the annotation processors that exploit them
 *
 * @author marc
 *
 */
public class Mirroring {

	public interface iBoundFloatMember extends iFloatProvider, iBoundMember<Float>  {
	}

	public interface iBoundDoubleMember extends iDoubleProvider, iBoundMember<Double>  {
	}

	public interface iBoundMember<t_is> extends iProvider<t_is>, iAcceptor<t_is> {
	}

	public interface iBoundNoArgsMethod<t_returns> extends iProvider<t_returns>, iUpdateable {
	}
 
	public interface iMethodFunction<t_class, t_returns, t_accepts> {
		public <A extends t_class> iFunction<t_returns, t_accepts> function(A to);

		public <A extends t_class> iFunction<Collection<? extends t_returns>, t_accepts> function(final Collection<A> to);
	}

	public interface iMethodMember<t_class, t_is> {
		public <A extends t_class> iAcceptor<t_is> acceptor(A to);

		public <A extends t_class> iProvider<t_is> provider(A to);
	}

	// the Mirror implementations

	static public class MirrorFloatMember<t_class> extends MirrorMember<t_class, Float> {

		public MirrorFloatMember(Class on, String name) {
			super(on, name, Float.TYPE);
		}

		@Override
		public <A extends t_class> iBoundFloatMember boundMember(final A to) {
			final iAcceptor<Float> a = acceptor(to);
			final iProvider<Float> p = provider(to);
			final iFloatProvider f = floatProvider(to);

			return new iBoundFloatMember() {

				public float evaluate() {
					return f.evaluate();
				}

				public Float get() {
					return p.get();
				}

				public iAcceptor<Float> set(Float to) {
					a.set(to);
					return this;
				}

			};
		}

		public <A extends t_class> iDoubleProvider doubleProvider(final A to) {
			return new iDoubleProvider() {
				public double evaluate() {
					try {
						return ((Number) field.get(to)).doubleValue();
					} catch (IllegalAccessException e) {
						throw new IllegalArgumentException(e);
					}
				}
			};
		}

		public <A extends t_class> iFloatProvider floatProvider(final A to) {
			return new iFloatProvider() {
				public float evaluate() {
					try {
						return ((Number) field.get(to)).floatValue();
					} catch (IllegalAccessException e) {
						throw new IllegalArgumentException(e);
					}
				}
			};
		}

	}

	static public class MirrorDoubleMember<t_class> extends MirrorMember<t_class, Double> {

		public MirrorDoubleMember(Class on, String name) {
			super(on, name, Double.TYPE);
		}

		@Override
		public <A extends t_class> iBoundDoubleMember boundMember(final A to) {
			final iAcceptor<Double> a = acceptor(to);
			final iProvider<Double> p = provider(to);
			final iDoubleProvider f = doubleProvider(to);

			return new iBoundDoubleMember() {

				public double evaluate() {
					return f.evaluate();
				}

				public Double get() {
					return p.get();
				}

				public iAcceptor<Double> set(Double to) {
					a.set(to);
					return this;
				}

			};
		}

		public <A extends t_class> iDoubleProvider doubleProvider(final A to) {
			return new iDoubleProvider() {
				public double evaluate() {
					try {
						return ((Number) field.get(to)).doubleValue();
					} catch (IllegalAccessException e) {
						throw new IllegalArgumentException(e);
					}
				}
			};
		}

		public <A extends t_class> iFloatProvider floatProvider(final A to) {
			return new iFloatProvider() {
				public float evaluate() {
					try {
						return ((Number) field.get(to)).floatValue();
					} catch (IllegalAccessException e) {
						throw new IllegalArgumentException(e);
					}
				}
			};
		}

	}


	static public class MirrorMember<t_class, t_is> implements iMethodMember<t_class, t_is> {
		protected Field field;

		public MirrorMember(Class on, String name, Class type) {
			field = ReflectionTools.getFirstFIeldCalled(on, name, type);
		}

		public <A extends t_class> iAcceptor<t_is> acceptor(final A to) {
			return new iAcceptor<t_is>() {
				public iAcceptor<t_is> set(t_is val) {
					try {
						field.set(to, val);
					} catch (IllegalAccessException e) {
						throw new IllegalArgumentException(e);
					}
					return this;
				}
			};
		}

		public <A extends t_class> iBoundMember<t_is> boundMember(final A to) {
			final iAcceptor<t_is> a = acceptor(to);
			final iProvider<t_is> p = provider(to);
			return new iBoundMember<t_is>() {

				public t_is get() {
					return p.get();
				}

				public iAcceptor<t_is> set(t_is to) {
					a.set(to);
					return this;
				}

			};
		}

		public <A extends t_class> iProvider<t_is> provider(final A to) {
			return new iProvider<t_is>() {
				@SuppressWarnings("unchecked")
				public t_is get() {
					try {
						return (t_is) field.get(to);
					} catch (IllegalAccessException e) {
						throw new IllegalArgumentException(e);
					}
				}
			};
		}

	}

	static public class MirrorMethod<t_class, t_returns, t_accepts> implements iMethodFunction<t_class, t_returns, t_accepts> {
		protected Method method;

		public MirrorMethod(Class on, String name, Class[] parameters) {
			method = ReflectionTools.methodOf(name, on, parameters);
			method.setAccessible(true);
		}

		protected MirrorMethod(Method m) {
			this.method = m;
			this.method.setAccessible(true);
		}

		public <A extends t_class> iAcceptor<t_accepts> acceptor(final A to) {
			return new iAcceptor<t_accepts>() {

				public iAcceptor<t_accepts> set(t_accepts parameter) {
					invoke(to, parameter);
					return this;
				}
			};
		}

		public MirrorMethod<t_class, t_returns, t_accepts> dispatchBackward(final ContextTopology<t_class, ?> topology) {
			return new MirrorMethod<t_class, t_returns, t_accepts>(method) {
				Dispatch<t_class, ?> d = new Dispatch(topology);

				@Override
				protected Object invoke(t_class to, Object... with) {
					Collection dd = d.dispatchBackward(to, method, with);
					return dd;
				}
			};
		}

		public MirrorMethod<t_class, Collection<t_returns>, t_accepts> dispatchForward(final ContextTopology<t_class, ?> topology) {
			return new MirrorMethod<t_class, Collection<t_returns>, t_accepts>(method) {
				Dispatch<t_class, ?> d = new Dispatch(topology);

				@Override
				protected Object invoke(t_class to, Object... with) {
					Collection dd = d.dispatchForward(to, method, with);
					return dd;
				}
			};
		}

		public <T> MirrorMethod<T, Collection<t_returns>, t_accepts> dispatchForwardOverProxy(final ContextTopology<T, t_class> topology) {
			return new MirrorMethod<T, Collection<t_returns>, t_accepts>(method) {
				Dispatch<T, t_class> d = new Dispatch(topology);

				@Override
				protected Object invoke(T to, Object... with) {
					Collection dd = d.dispatchForward(to, method, with);
					return dd;
				}
			};
		}

		public <A extends t_class> iFunction<t_returns, t_accepts> function(final A to) {
			return new iFunction<t_returns, t_accepts>() {
				@SuppressWarnings("unchecked")
				public t_returns f(t_accepts in) {
					return (t_returns) invoke(to, in);
				}
			};
		}

		public <A extends t_class> iFunction<Collection<? extends t_returns>, t_accepts> function(final Collection<A> to) {
			return new iFunction<Collection<? extends t_returns>, t_accepts>() {
				@SuppressWarnings("unchecked")
				public Collection<? extends t_returns> f(t_accepts in) {
					// fixme,
					// this
					// could
					// be
					// done
					// one
					// stage
					// earlier
					if (to.size() == 0)
						return Collections.EMPTY_LIST;
					if (to.size() == 1)
						return (Collection<? extends t_returns>) Collections.singletonList(invoke(to.iterator().next(), in));
					ArrayList<t_returns> ret = new ArrayList<t_returns>();
					for (A a : to) {
						ret.add((t_returns) invoke(a, in));
					}
					return ret;
				}
			};
		}

		public <A extends t_class, B extends t_accepts> iUpdateable updateable(final A to, final B with) {
			return new iUpdateable() {

				public void update() {
					invoke(to, with);
				}

			};
		}

		public <A extends t_class> iUpdateable updateable(final A to, final Object... with) {
			return new iUpdateable() {

				public void update() {
					invoke(to, with);
				}
			};
		}

		public <A extends t_class, B extends t_accepts> iUpdateable updateable(final Collection<A> to, final B with) {
			return new iUpdateable() {

				public void update() {
					for (A a : to)
						invoke(a, with);
				}

			};
		}

		public <A extends t_class> iUpdateable updateable(final Collection<A> to, final Object... with) {
			return new iUpdateable() {

				public void update() {
					for (A a : to)
						invoke(a, with);
				}
			};
		}

		protected Object invoke(t_class target, Object... with) {
			try {
				if (with.length==1 && with[0] instanceof Object[])
					return method.invoke(target, (Object[]) with[0]);
				else
					return method.invoke(target, with);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			} catch (InvocationTargetException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	static public class MirrorNoArgsMethod<t_class, t_returns> implements iMethodFunction<t_class, t_returns, Object>, iFunction<t_returns, t_class> {
		protected Method method;

		public MirrorNoArgsMethod(Class on, String name) {
			method = ReflectionTools.methodOf(name, on, new Class[] {});
			method.setAccessible(true);
		}

		public <A extends t_class> iBoundNoArgsMethod<t_returns> bind(final A to) {
			return new iBoundNoArgsMethod<t_returns>() {
				public t_returns get() {
					return f(to);
				}

				public void update() {
					f(to);
				}
			};
		}

		public <A extends t_class> iBoundNoArgsMethod<Collection<? extends t_returns>> bind(final Collection<A> to) {

			final iFunction<Collection<? extends t_returns>, Object> ff = function(to);
			return new iBoundNoArgsMethod<Collection<? extends t_returns>>() {
				public Collection<? extends t_returns> get() {
					return ff.f(null);
				}

				public void update() {
					ff.f(null);
				}
			};
		}

		public t_returns f(t_class in) {
			try {
				return (t_returns) method.invoke(in);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			} catch (InvocationTargetException e) {
				throw new IllegalArgumentException(e);
			}
		}

		public <A extends t_class> iFunction<t_returns, Object> function(final A to) {
			return new iFunction<t_returns, Object>() {
				@SuppressWarnings("unchecked")
				public t_returns f(Object in) {
					return (t_returns) invoke(to, in);
				}
			};
		}

		public <A extends t_class> iFunction<Collection<? extends t_returns>, Object> function(final Collection<A> to) {
			return new iFunction<Collection<? extends t_returns>, Object>() {
				@SuppressWarnings("unchecked")
				public Collection<? extends t_returns> f(Object in) {
					// fixme,
					// this
					// could
					// be
					// done
					// one
					// stage
					// earlier
					if (to.size() == 0)
						return Collections.EMPTY_LIST;
					if (to.size() == 1)
						return (Collection<? extends t_returns>) Collections.singletonList(invoke(to.iterator().next(), in));
					ArrayList<t_returns> ret = new ArrayList<t_returns>();
					for (A a : to) {
						ret.add((t_returns) invoke(a, in));
					}
					return ret;
				}
			};
		}

		public <A extends t_class> iUpdateable updateable(final A to) {
			return new iUpdateable() {

				public void update() {
					invoke(to);
				}

			};
		}

		public <A extends t_class> iUpdateable updateable(final Collection<A> to) {
			return new iUpdateable() {

				public void update() {
					for (A a : to)
						invoke(a);
				}

			};
		}

		protected <A> Object invoke(final A to, final Object... with) {
			try {
				if (with.length==1 && with[0] instanceof Object[])
					return method.invoke(to, (Object[]) with[0]);
				else
					return method.invoke(to, with);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			} catch (InvocationTargetException e) {
				throw new IllegalArgumentException(e);
			}
		}

	}

	static public class MirrorNoReturnMethod<t_class, t_accepts> implements iMethodFunction<t_class, Object, t_accepts> {
		protected Method method;

		public MirrorNoReturnMethod(Class on, String name, Class[] parameters) {
			method = ReflectionTools.methodOf(name, on, parameters);
			method.setAccessible(true);
		}

		protected MirrorNoReturnMethod(Method method) {
			this.method = method;
			this.method.setAccessible(true);
		}

		public <A extends t_class> iAcceptor<t_accepts> acceptor(final A to) {
			return new iAcceptor<t_accepts>() {

				public iAcceptor<t_accepts> set(t_accepts parameter) {
					invoke(to, parameter);
					return this;

				}
			};
		}

		public <A extends t_class> iAcceptor<t_accepts> acceptor(final Collection<A> to) {
			return new iAcceptor<t_accepts>() {

				public iAcceptor<t_accepts> set(t_accepts parameter) {
					for (A a : to)
						invoke(a, parameter);
					return this;

				}
			};
		}

		public MirrorNoReturnMethod<t_class, t_accepts> dispatchBackward(final ContextTopology<t_class, ?> topology) {
			return new MirrorNoReturnMethod<t_class, t_accepts>(method) {
				Dispatch d = new Dispatch(topology);

				@Override
				protected <A> Object invoke(A to, Object... with) {
					d.dispatchBackward(to, method, with);
					return null;
				}
			};
		}

		public MirrorNoReturnMethod<t_class, t_accepts> dispatchForward(final ContextTopology<t_class, ?> topology) {
			return new MirrorNoReturnMethod<t_class, t_accepts>(method) {
				Dispatch d = new Dispatch(topology);

				@Override
				protected <A> Object invoke(A to, Object... with) {
					d.dispatchForward(to, method, with);
					return null;
				}
			};
		}

		public <A extends t_class> iFunction<Object, t_accepts> function(final A to) {
			return new iFunction<Object, t_accepts>() {
				@SuppressWarnings("unchecked")
				public Object f(t_accepts in) {
					return invoke(to, in);
				}
			};
		}

		public <A extends t_class> iFunction<Collection<? extends Object>, t_accepts> function(final Collection<A> to) {
			return new iFunction<Collection<? extends Object>, t_accepts>() {
				@SuppressWarnings("unchecked")
				public Collection<? extends Object> f(t_accepts in) {
					if (to.size() == 0)
						return Collections.EMPTY_LIST;
					if (to.size() == 1)
						return Collections.singletonList(invoke(to.iterator().next(), in));
					ArrayList ret = new ArrayList();
					for (A a : to) {
						ret.add(invoke(a, in));
					}
					return ret;
				}
			};
		}

		public <A extends t_class, B extends t_accepts> iUpdateable updateable(final Collection<A> to, final B with) {
			return new iUpdateable() {

				public void update() {
					for (A a : to)
						invoke(a, with);
				}

			};
		}

		public <A extends t_class> iUpdateable updateable(final Collection<A> to, final Object... with) {
			return new iUpdateable() {

				public void update() {
					for (A a : to)
						invoke(to, with);
				}

			};
		}

		protected <A> Object invoke(final A to, final Object... with) {
			try {
				if (with.length==1 && with[0] instanceof Object[])
					return method.invoke(to, (Object[]) with[0]);
				else
					return method.invoke(to, with);
			} catch (IllegalAccessException e) {
				System.err.println(" while invoking <" + method + "> on <" + to + "> with <" + with.length + "> arguments");
				throw new IllegalArgumentException(e);
			} catch (InvocationTargetException e) {
				System.err.println(" while invoking <" + method + "> on <" + to + "> with <" + with.length + "> arguments");
				throw new IllegalArgumentException(e);
			} catch (IllegalArgumentException e) {
				System.err.println(" while invoking <" + method + "> on <" + to + "> with <" + with.length + "> arguments");
				throw new IllegalArgumentException(e);
			}
		}

	}

	static public class MirrorNoReturnNoArgsMethod<t_class> implements iMethodFunction<t_class, Object, Object> {
		protected Method method;

		public MirrorNoReturnNoArgsMethod(Class on, String name) {
			method = ReflectionTools.methodOf(name, on, new Class[] {});
			method.setAccessible(true);
		}

		protected MirrorNoReturnNoArgsMethod(Method method) {
			this.method = method;

		}

		public MirrorNoReturnNoArgsMethod<t_class> dispatchBackward(final ContextTopology<t_class, ?> topology) {
			return new MirrorNoReturnNoArgsMethod<t_class>(method) {
				Dispatch d = new Dispatch(topology);

				@Override
				protected <A> void invoke(A to, Object... with) {
					d.dispatchBackward(to, method, with);
				}
			};
		}

		public MirrorNoReturnNoArgsMethod<t_class> dispatchForward(final ContextTopology<t_class, ?> topology) {
			return new MirrorNoReturnNoArgsMethod<t_class>(method) {
				Dispatch d = new Dispatch(topology);

				@Override
				protected <A> void invoke(A to, Object... with) {
					d.dispatchForward(to, method, with);
				}
			};
		}

		public <A extends t_class> iFunction<Object, Object> function(final A to) {
			return new iFunction<Object, Object>() {
				@SuppressWarnings("unchecked")
				public Object f(Object in) {
					invoke(to);
					return null;
				}
			};
		}

		public <A extends t_class> iFunction<Collection<? extends Object>, Object> function(final Collection<A> to) {
			return new iFunction<Collection<? extends Object>, Object>() {
				@SuppressWarnings("unchecked")
				public Collection<Object> f(Object in) {
					for (A a : to)
						invoke(to);
					return null;
				}
			};
		}

		public <A extends t_class> iUpdateable updateable(final A to) {
			return new iUpdateable() {

				public void update() {
					invoke(to);
				}

			};
		}

		public <A extends t_class> iUpdateable updateable(final A to, final Object... with) {
			return new iUpdateable() {

				public void update() {
					invoke(to, with);
				}

			};
		}

		public <A extends t_class> iUpdateable updateable(final Collection<A> to) {
			return new iUpdateable() {

				public void update() {
					for (A a : to)
						invoke(a);

				}

			};
		}

		protected <A> void invoke(final A to, final Object... with) {
			try {
				if (with.length==1 && with[0] instanceof Object[])
					method.invoke(to, (Object[]) with[0]);
				else
					method.invoke(to, with);

			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			} catch (InvocationTargetException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

}
