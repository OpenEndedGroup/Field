package field.math.abstraction;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.Deque;

import field.launch.iUpdateable;
import field.namespace.generic.ReflectionTools;


public interface iProvider<T> {
	static public class BufferedOne<T> implements iProvider<T> {
		private final iProvider<T> in;

		T last = null;

		boolean first = true;

		public BufferedOne(iProvider<T> in) {
			this.in = in;
		}

		public T get() {
			if (first) {
				last = in.get();
				first = false;
				return last;
			}
			T old = last;
			last = in.get();
			return old;
		}
	}

	static public class BufferedOneUpdate<T> implements iProvider<T>, iUpdateable {
		private final iProvider<T> in;

		T last = null;

		boolean first = true;

		int tick = 0;

		boolean needsSwap = false;

		public BufferedOneUpdate(iProvider<T> in) {
			this.in = in;
		}

		public T get() {
			if (first) {
				last = in.get();
				first = false;
				return last;
			}
			if (needsSwap) {
				T old = last;
				last = in.get();
				return old;
			} else
				return last;
		}

		public void update() {
			tick++;
			needsSwap = true;
		}
	}

	public class BufferedTwo<T> implements iProvider<T>{
		private final iProvider<T> in;

		T last = null;
		T last2 = null;

		int first = 0;


		boolean skip = false;

		public BufferedTwo(iProvider<T> in) {
			this.in = in;
		}

		public T get() {
			if (first==0) {
				last2 = last = in.get();
				first = 1;
				return last;
			}

			T old = last2;
			last2 = last;
			last = in.get();
			if (skip)
			{
				old = last2 = last;
				skip = false;
			}
			return old;
		}

		public void skip()
		{
			skip = true;
		}

	}

	public class BufferedN<T> implements iProvider<T>{
		private final iProvider<T> in;

		Deque<T> dec = new ArrayDeque<T>();
		int first = 0;

		private int n;

		public BufferedN(iProvider<T> in, int n) {
			this.in = in;
			this.n = n;
		}

		public T get() {
			dec.add(in.get());
			if (dec.size()>n)
			{
				return dec.removeFirst();
			}
			else
			{
				return dec.peekFirst();
			}
		}

	}

	static public class Constant<T> implements iProvider<T>, iAcceptor<T> {
		T t;

		public Constant() {
		}

		public Constant(T t) {
			this.t = t;
		}

		public T get() {
			return t;
		}

		public Constant<T> set(T t) {
			this.t = t;
			return this;
		}
		
		@Override
		public String toString() {
			return "iProvider.Constant("+t+")";
		}
	}

	static public class FieldProvider<T> implements iProvider<T> {
		private java.lang.reflect.Field field;

		private Object from;

		public FieldProvider(Object from, String field) {
			java.lang.reflect.Field[] allFields = ReflectionTools.getAllFields(from.getClass());
			for (java.lang.reflect.Field f : allFields) {
				if (f.getName().equals(field)) {
					this.field = f;
					this.from = from;
					return;
				}
			}
			throw new IllegalStateException(" no field called <" + field + "> in <" + from + "> <" + from.getClass() + ">");
		}

		public T get() {
			try {
				return (T) field.get(from);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	static public class HasChanged<T> implements iProvider<T> {
		private final iProvider<T> to;

		boolean needsLoad = true;

		T last = null;

		T now = null;

		public HasChanged(iProvider<T> to) {
			this.to = to;
		}

		public T get() {
			if (needsLoad) {
				last = now;
				now = to.get();
			} else {
				needsLoad = true;
			}
			return now;
		}

		public boolean hasChanged() {
			if (needsLoad) {
				last = now;
				now = to.get();
				needsLoad = false;
			}

			return now == null ? (last != null) : (last == null ? (now != null) : (!last.equals(now)));
		}

		public boolean hasChanged(boolean b) {
			if (needsLoad || b) {
				last = now;
				now = to.get();
				needsLoad = false;
			}

			return now == null ? (last != null) : (last == null ? (now != null) : (!last.equals(now)));
		}
	}

	static public class ProxyProvider<T> {
		public <T> T makeProxyFor(Class<T> clazz, final iProvider< ? extends T> p) {
			return (T) Proxy.newProxyInstance(p.getClass().getClassLoader(), new Class[] { clazz}, new InvocationHandler(){

				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					return method.invoke(p.get(), args);
				}
			});
		}
	}

	// this can be used to make a provider look like what it provides (see also the caching "Up"

	static public class SerializationBarrier<T> implements iProvider<T> {
		public T last;

		transient private final iProvider<T> through;

		public SerializationBarrier(iProvider<T> through) {
			this.through = through;
		}

		protected SerializationBarrier() {
			this.through = null;
		}

		public T get() {
			if (through == null) return last;
			return last = through.get();
		}

	}

	

	public T get();

}
