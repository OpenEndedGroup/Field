package field.namespace.generic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import field.math.linalg.Vector2;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.Generics.Pair;

/**
 * monads, marc style
 * 
 * @author marc
 * 
 */
public class Bind {

	// will have to wait until we can refactor this into filter

	public interface iFunction<t_out, t_in> {
		public t_out f(t_in in);
	}
	
	public interface iFunction2<t_out, t_in1, t_in2> {
		public t_out f(t_in1 in, t_in2 in2);
	}

	public interface iOutput<t_out> {
		public t_out get();
	}

	public <A, B, C> iFunction<A, C> bind(final iFunction<A, B> two, final iFunction<B, C> one) {
		return new iFunction<A, C>() {
			public A f(C in) {
				B b = one.f(in);
				A a = two.f(b);
				return a;
			}
		};
	}

	public <A, B> iOutput<A> bind(final iFunction<A, B> on, final iOutput<B> in) {
		return new iOutput<A>() {
			public A get() {
				return on.f(in.get());
			}
		};
	}

	public <A, B, C> iFunction<A, C> bind(final iOutput<iFunction<A, B>> two, final iOutput<iFunction<B, C>> one) {
		return new iFunction<A, C>() {
			public A f(C in) {
				return two.get().f(one.get().f(in));
			}
		};
	}

	public <A, B, C> iFunction<A, C> bind(final iOutput<iFunction<A, B>> two, final iFunction<B, C> one) {
		return new iFunction<A, C>() {
			public A f(C in) {
				return two.get().f(one.f(in));
			}
		};
	}

	public <A> iOutput<A> offset(final A a) {
		return new iOutput<A>() {
			public A get() {
				return a;
			}
		};
	}

	public <A, B> iFunction<A, B> call(Class<? extends A> out, final Method m, final Object on, Class<? extends B> ini) {
		return new iFunction<A, B>() {
			@SuppressWarnings("unchecked")
			public A f(B in) {
				try {
					return (A) m.invoke(on, new Object[] { in });
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					throw (IllegalArgumentException) (new IllegalArgumentException().initCause(e));
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					throw (IllegalArgumentException) (new IllegalArgumentException().initCause(e));
				} catch (InvocationTargetException e) {
					e.printStackTrace();
					throw (IllegalArgumentException) (new IllegalArgumentException().initCause(e));
				}
			}
		};
	}

	public <A, B, C> iFunction<A, Pair<B, C>> call(Class<? extends A> out, final Method m, Class<? extends B> onClass, Class<? extends C> paramClass) {
		return new iFunction<A, Pair<B, C>>() {
			@SuppressWarnings("unchecked")
			public A f(Pair<B, C> in) {
				try {
					return (A) m.invoke(in.left, new Object[] { in.right });
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					throw (IllegalArgumentException) (new IllegalArgumentException().initCause(e));
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					throw (IllegalArgumentException) (new IllegalArgumentException().initCause(e));
				} catch (InvocationTargetException e) {
					e.printStackTrace();
					throw (IllegalArgumentException) (new IllegalArgumentException().initCause(e));
				}
			}
		};
	}

	public <B, C> iFunction<Pair<B, C>, B> wrap(final iFunction<C, B> w) {
		return new iFunction<Pair<B, C>, B>() {
			public Pair<B, C> f(B in) {
				C c = w.f(in);
				return new Pair<B, C>(in, c);
			}
		};
	}

	public <A, B> iFunction<A, B> call(Class<? extends A> out, final Method m, Class<? extends B> ini) {
		return new iFunction<A, B>() {
			@SuppressWarnings("unchecked")
			public A f(B in) {
				try {
					return (A) m.invoke(in, new Object[] {});
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					throw (IllegalArgumentException) (new IllegalArgumentException().initCause(e));
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					throw (IllegalArgumentException) (new IllegalArgumentException().initCause(e));
				} catch (InvocationTargetException e) {
					e.printStackTrace();
					throw (IllegalArgumentException) (new IllegalArgumentException().initCause(e));
				}
			}
		};
	}

	public <A, B> iFunction<A, B> multipleApply(final iFunction<A, B> function, final int num) {
		return new iFunction<A, B>() {
			public A f(B in) {
				A a = null;
				for (int i = 0; i < num; i++) {
					A l = function.f(in);
					if (l != null) {
						a = l;
					}
				}
				return a;
			}
		};
	}

	public <A, B> iFunction<A, B> callAll(final List<iFunction<A, B>> of) {
		return new iFunction<A, B>() {
			public A f(B in) {
				A r = null;
				for (iFunction<A, B> f : of) {
					r = f.f(in);
				}
				return r;
			}
		};
	}

	public <A, B> iFunction<A, B> collapseAll(final List<iFunction<A, B>> of, final iFunction<A, Pair<A, A>> over) {
		return new iFunction<A, B>() {
			public A f(B in) {
				A r = null;
				for (iFunction<A, B> f : of) {
					A r2 = f.f(in);
					r = over.f(new Pair<A, A>(r, r2));
				}
				return r;
			}
		};
	}

	// odd monads

	public <T> iOutput<T> randomOf(final List<T> of) {
		return new iOutput<T>() {
			public T get() {
				int index = (int) (Math.random() * of.size());
				return of.get(index);
			}
		};
	}

	// util

	static public <T> T argMax(Collection<T> t, iFunction<? extends Number, T> f) {
		double v = Double.NEGATIVE_INFINITY;
		T best = null;

		for (T tt : t) {
			Number m = f.f(tt);
			if (m.doubleValue() > v) {
				v = m.doubleValue();
				best = tt;
			}
		}

		return best;
	}

	static public <T> T argMin(Collection<T> t, iFunction<? extends Number, T> f) {
		double v = Double.POSITIVE_INFINITY;
		T best = null;

		for (T tt : t) {
			Number m = f.f(tt);
			if (m.doubleValue() < v) {
				v = m.doubleValue();
				best = tt;
			}
		}

		return best;
	}

	static public <T> Number  max(Collection<T> t, iFunction<? extends Number, T> f) {
		double v = Double.NEGATIVE_INFINITY;
		Number best = null;

		for (T tt : t) {
			Number m = f.f(tt);
			if (m.doubleValue() > v) {
				v = m.doubleValue();
				best = m;
			}
		}

		return best;
	}

	static public <T> Number min(Collection<T> t, iFunction<? extends Number, T> f) {
		double v = Double.POSITIVE_INFINITY;
		Number best = null;

		for (T tt : t) {
			Number m = f.f(tt);
			if (m.doubleValue() < v) {
				v = m.doubleValue();
				best = m;
			}
		}

		return best;
	}

	public static <T> int indexMin(Collection<T> t, iFunction<? extends Number, T> f) {

		double v = Double.POSITIVE_INFINITY;
		int best = 0;

		int i=0;
		for (T tt : t) {
			
			Number m = f.f(tt);
			if (m.doubleValue() < v) {
				v = m.doubleValue();
				best = i;
			}
			i++;
		}

		return best;
	}

}
