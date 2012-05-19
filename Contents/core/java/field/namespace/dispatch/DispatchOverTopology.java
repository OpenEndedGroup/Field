package field.namespace.dispatch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import field.math.graph.iTopology;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.graph.TopologySearching.TopologyVisitor_breadthFirst;

public class DispatchOverTopology<T> {

	public class And extends TopologyDispatchVaryingMethod<Boolean> {

		public And(boolean avoidLoops) {
			super(avoidLoops);
		}

		@Override
		public Boolean dispatch(Method method, T on, Object... args) {
			ret = true;
			super.dispatch(method, on, args);
			return ret;
		}

		@Override
		protected VisitCode visit(T root) {
			try {
				Object r = getMethod(root).invoke(root, args);
				if (!((Boolean) r)) {
					ret = false;
					return VisitCode.stop;
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			} catch (InvocationTargetException e) {
				throw new IllegalArgumentException(e);
			}
			return VisitCode.cont;
		}
	}

	public class Interpreted extends RawBase<Object> {

		private final ReturnInterpretation retint;

		public Interpreted(boolean avoidLoops, ReturnInterpretation retint) {
			super(avoidLoops);
			this.retint = retint;
		}

		@Override
		protected VisitCode interpretReturn(Object invoke) {
			if (retint == ReturnInterpretation.always)
				return VisitCode.cont;

			if (retint == ReturnInterpretation.untilNotNull)
				return invoke != null ? VisitCode.stop : VisitCode.cont;
			if (retint == ReturnInterpretation.untilNull)
				return invoke != null ? VisitCode.cont : VisitCode.stop;
			if (retint == ReturnInterpretation.skipNotNull)
				return invoke != null ? VisitCode.skip : VisitCode.cont;
			if (retint == ReturnInterpretation.skipNull)
				return invoke != null ? VisitCode.cont : VisitCode.skip;

			return VisitCode.cont;
		}

	}

	public class Or extends TopologyDispatchVaryingMethod<Boolean> {

		public Or(boolean avoidLoops) {
			super(avoidLoops);
		}

		@Override
		public Boolean dispatch(Method method, T on, Object... args) {
			ret = false;
			super.dispatch(method, on, args);
			return ret;
		}

		@Override
		public Boolean dispatch(T on, Object... args) {
			ret = false;
			super.dispatch(on, args);
			return ret;
		}

		@Override
		protected VisitCode visit(T root) {
			try {
				Object r = getMethod(root).invoke(root, args);
				if (((Boolean) r)) {
					ret = true;
					return VisitCode.stop;
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			} catch (InvocationTargetException e) {
				throw new IllegalArgumentException(e);
			}
			return VisitCode.cont;
		}
	}

	public class Raw extends RawBase<VisitCode> {
		public Raw(boolean avoidLoops) {
			super(avoidLoops);
		}

		@Override
		protected VisitCode interpretReturn(VisitCode invoke) {
			return invoke;
		}
	}

	public class RawBase<R> extends TopologyDispatchVaryingMethod<R> {

		ArrayList<R> allret;

		public RawBase(boolean avoidLoops) {
			super(avoidLoops);
		}



		@Override
		public void apply(iTopology<T> top, T root) {
			allret  = new ArrayList<R>();
			super.apply(top, root);
		}


		public ArrayList<R> returns() {
			return allret;
		}

		protected Object getObject(T root) {
			return root;
		}

		protected VisitCode interpretReturn(R invoke) {
			if (invoke instanceof VisitCode)
				return ((VisitCode) invoke);
			return VisitCode.cont;
		}

		@Override
		protected LinkedHashSet<T> maybeWrap(LinkedHashSet<T> f) {
			return new LinkedHashSet<T>(f);
		}

		protected void postamble(Object o, Object[] args, Object ret) {
		}

		protected void preamble(Object o, Object[] args) {
		}

		@Override
		protected VisitCode visit(T root) {
			try {
				Object o = getObject(root);
				if (o == null) {
					return VisitCode.cont;
				}

				preamble(o, args);
				Method m = getMethod(root);
				R rr = (R) m.invoke(o, args);
				VisitCode r = interpretReturn(rr);
				postamble(o, args, rr);
				if (trace)
				ret = rr;
				return r;
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			} catch (InvocationTargetException e) {


				throw new IllegalArgumentException(e);
			}
		}

	}

	public enum ReturnInterpretation {
		always, untilNotNull, untilNull, skipNotNull, skipNull;
	}

	public abstract class TopologyDispatchVaryingMethod<R> extends TopologyVisitor_breadthFirst<T> {
		private final boolean maintainCache;

		private final boolean doCache;

		protected Object[] args;

		protected R ret;

		protected Method fixedMethod;

		protected boolean trace = false;

		Map<Class, Method> cache = new HashMap<Class, Method>();

		public TopologyDispatchVaryingMethod(boolean avoidLoops) {
			super(avoidLoops);
			this.doCache = false;
			this.maintainCache = false;
		}

		public TopologyDispatchVaryingMethod(boolean avoidLoops, boolean cache, boolean maintainCache) {
			super(avoidLoops);
			doCache = cache;
			this.maintainCache = maintainCache;
		}

		public Method computeMethod(T root) {
			throw new NotImplementedException();
		}

		public R dispatch(Method m, T on, Object... args) {
			if (m.getName().equals("added"))
			{
				trace = true;
			}
			fixedMethod = m;
			this.args = args;
			apply(topology, on);

			if (m.getName().equals("added"))
			{
				trace = false;
			}

			return ret;
		}

		public R dispatch(T on, Object... args) {
			fixedMethod = null;
			this.args = args;
			apply(topology, on);
			return ret;
		}

		public Method getMethod(T root) {
			Method m = null;
			if (fixedMethod != null)
				return fixedMethod;
			else if (!doCache)
				return computeMethod(root);
			else {
				m = cache.get(root.getClass());
				if (m == null) {
					cache.put(root.getClass(), m = computeMethod(root));
				}
				return m;
			}
		}

	}

	public class UntilNull<R> extends TopologyDispatchVaryingMethod<T> {

		private Method method;

		private Object[] args;

		protected List<R> ret = new ArrayList<R>();

		public UntilNull(boolean avoidLoops) {
			super(avoidLoops);
		}

		public List<R> accumulate(Method method, T on, Object... args) {
			ret.clear();
			this.fixedMethod = method;
			this.args = args;
			this.apply(topology, on);
			return ret;
		}

		public List<R> accumulate(T on, Object... args) {
			ret.clear();
			this.args = args;
			this.apply(topology, on);
			return ret;
		}

		@Override
		protected VisitCode visit(T root) {
			try {
				Object r = getMethod(root).invoke(root, args);
				if (r != null) {
					ret.add((R) r);
					return VisitCode.stop;
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			} catch (InvocationTargetException e) {
				throw new IllegalArgumentException(e);
			}
			return VisitCode.cont;
		}
	}

	private final iTopology<T> topology;

	public DispatchOverTopology(iTopology<T> topology) {
		this.topology = topology;
	}

}
