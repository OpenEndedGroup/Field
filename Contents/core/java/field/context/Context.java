package field.context;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.python.core.Py;
import org.python.core.PyFunction;
import org.python.core.PyObject;

import field.bytecode.protect.annotations.HiddenInAutocomplete;
import field.core.util.FieldPyObjectAdaptor.iCallable_keywords;
import field.core.util.FieldPyObjectAdaptor.iHandlesAttributes;
import field.core.util.FieldPyObjectAdaptor.iHandlesDeletionOfAttributes;
import field.launch.iUpdateable;
import field.math.abstraction.iProvider;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.Bind.iFunction2;
import field.util.Dict;
import field.util.Dict.Prop;

public class Context {

	public interface iVisitor {
		public void enter(Cobj c);

		public void visit(Cobj c);

		public void exit(Cobj c);
	}

	public interface iSimpleVisitor {
		public void visit(Cobj c);
	}

	static public Object _ = new Object();

	static public class Cobj implements iHandlesAttributes, iHandlesDeletionOfAttributes, iCallable_keywords {
		private Cobj parent;
		protected List<Cobj> children = null;

		private Dict properties = new Dict();

		/**
		 * if a property 'x' is missing, check to see if compute_x
		 * exists at this level of the tree. If so call compute_x(this)
		 * to compute the value.
		 */
		public boolean allowComputedProperties = false;

		/**
		 * special constructor for Python (allows use of keywords)
		 */
		@HiddenInAutocomplete
		public Cobj(PyObject[] a, String[] kw) {
			if (a.length != kw.length)
				throw new IllegalArgumentException();

			Map<String, Object> o = new HashMap<String, Object>();
			for (int i = 0; i < a.length; i++) {
				o.put(kw[i], Py.tojava(a[i], Object.class));
			}
			callWithKeywords(new Object[] {}, o);
		}

		/**
		 * constructs a Cobj.
		 * 
		 * Note, you can use Python keyboard syntax as well:
		 * 
		 * Cobj(myProperty=4, myOtherProperty="banana")
		 * 
		 * This sets some properties on construction
		 */
		public Cobj() {
		}

		/**
		 * returns the parent (if any) for this Cobj
		 */
		public Cobj parent() {
			return parent;
		}

		/**
		 * returns all child Cobj
		 */
		public Collection<Cobj> children() {
			if (children != null)
				return children;

			ArrayList<Cobj> x = new ArrayList<Cobj>();

			Collection<Object> c = properties.getMap().values();
			buildChildrenList(x, c);
			return x;
		}

		/**
		 * disconnects this Cobj from its parent
		 */
		public void disconnect() {
			Cobj p = parent();
			if (p != null)
				p.removeProperty(this);
		}

		/**
		 * removes a property from this Cobj
		 */
		public void removeProperty(Object c) {
			children = null;
			if (c instanceof Prop)
				properties.remove(((Prop) c));
			else {
				properties.removeValue(c);
			}
		}

		protected void buildChildrenList(ArrayList<Cobj> x, Collection<Object> c) {
			for (Object o : c) {
				if (o == this)
					continue;

				if (o instanceof Cobj) {
					x.add((Cobj) o);
					((Cobj) o).parent = this;
				} else if (o instanceof Collection) {
					buildChildrenList(x, ((Collection<Object>) o));
				}
			}
		}

		@HiddenInAutocomplete
		static ThreadLocal<Cobj> _start = new ThreadLocal<Context.Cobj>();
		
		/**
		 * gets a property from this Cobj (typesafe java interface)
		 */
		@HiddenInAutocomplete
		public <T> T getProperty(Prop<T> p) {
			T q = properties.get(p);

			if (q == null && allowComputedProperties) {
				Prop<iFunction2<T, Cobj,Cobj>> c = new Prop<iFunction2<T, Cobj,Cobj>>("compute_" + p.getName());
				iFunction2<T, Cobj, Cobj> q2 = properties.get(c);
				if (q2 != null) {
					return q2.f(this, _start.get());
				}
			}

			if (q == null && parent != null)
				return (T) parent.getProperty(p);

			return q;
		}

		/**
		 * locates a property from this Cobj (typesafe java interface)
		 */
		@HiddenInAutocomplete
		public <T> Cobj whereProperty(Prop<T> p) {
			T q = properties.get(p);
			if (q == null && parent != null)
				return parent.whereProperty(p);

			return this;
		}

		/**
		 * returns the Cobj that would supply this property if it exists
		 */
		public Cobj where(String p) {
			return whereProperty(new Prop<Object>(p));
		}

		@HiddenInAutocomplete
		public <T> T find(iProvider<T> defaultValue, String... names) {
			for (String n : names) {
				Object v = getAttribute(n);
				if (v != null)
					return (T) v;
			}
			if (defaultValue != null)
				return defaultValue.get();
			return null;
		}

		@HiddenInAutocomplete
		public Object find(String... names) {
			return find(null, names);
		}

		@HiddenInAutocomplete
		public boolean hasProperty(Prop p) {
			Object q = properties.get(p);
			return q != null;
		}

		/**
		 * returns true if this Cobj has a property
		 */
		public boolean hasProperty(String p) {
			Object q = properties.get(new Prop(p));
			return q != null;
		}

		@HiddenInAutocomplete
		public <T> void setProperty(Prop<T> p, T value) {

			if (value == _)
				value = (T) this;

			T old = properties.get(p);
			properties.put(p, value);
			if (old instanceof Cobj || value instanceof Cobj || old instanceof Collection || value instanceof Collection)
				children = null;

			if (value instanceof Cobj && this != value) {
				((Cobj) value).parent = this;
			} else if (old instanceof Cobj) {
				((Cobj) old).parent = null;
			} else if (value instanceof Collection) {
				for (Object o : ((Collection) value)) {
					if (o instanceof Cobj) {
						((Cobj) o).parent = this;
					}
				}

			}
		}

		@HiddenInAutocomplete
		public <T> void rewriteProperty(Prop<T> p, T value) {
			Cobj where = whereProperty(p);
			where.setProperty(p, value);
		}

		/**
		 * Finds this value in the tree and rewrites it there. Will
		 * throw an exception if it can't be found. You can also write
		 * cobj.where_property=newValue
		 */
		public void rewriteProperty(String o, Object value) {
			Cobj where = where(o);
			where.setAttribute(o, value);
		}

		@Override
		@HiddenInAutocomplete
		public Object getAttribute(String name) {

			_start.set(this);
			
			if (name.startsWith("down_")) {
				String sub = name.substring("down_".length());
				System.out.println(" getall <" + sub + ">");
				return getAll(sub);
			}
			if (name.startsWith("where_")) {
				String sub = name.substring("where_".length());
				System.out.println(" getall <" + sub + ">");
				return where(sub);
			}

			return getProperty(new Prop(name));
		}

		@Override
		@HiddenInAutocomplete
		public void setAttribute(String name, Object value) {

			if (name.startsWith("where_")) {
				String sub = name.substring("where_".length());
				rewriteProperty(sub, value);
			} else if (name.startsWith("compute_")) {
				allowComputedProperties = true;
				if (value instanceof PyFunction) {
					setProperty(new Prop(name), ((PyFunction) value).__tojava__(iFunction2.class));
				} else
					setProperty(new Prop(name), value);
			} else
				setProperty(new Prop(name), value);
		}

		@Override
		@HiddenInAutocomplete
		public void deleteAttribute(String name) {
			removeProperty(new Prop(name));
		}

		@HiddenInAutocomplete
		public Object invoke(boolean startHere, Method m, Object... args) {

			// System.out.println(">>>>>>>>> invoke :"+startHere+" "+m+" on "+this);

			Object r = null;
			if (startHere) {

				Class<?> xx = m.getDeclaringClass();
				if (xx.isAssignableFrom(this.getClass())) {
					try {
						// System.out.println(" will invoke on this ");
						r = m.invoke(this, args);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
						throw e;
					} catch (IllegalAccessException e) {
						e.printStackTrace();
						IllegalArgumentException iae = new IllegalArgumentException();
						iae.initCause(e);
						throw iae;
					} catch (InvocationTargetException e) {
						e.printStackTrace();
						IllegalArgumentException iae = new IllegalArgumentException();
						iae.initCause(e);
						throw iae;
					}
				}
			}

			for (Object o : new ArrayList<Object>(properties.getMap().values())) {

				// System.out.println(" recuring to :"+o);

				if (o == this)
					continue;

				Object q = invoke(o, m, args);
				if (r == null && q != null)
					r = q;

			}

			// System.out.println("<<<<<<<<<<<<");

			return r;
		}

		@HiddenInAutocomplete
		public Object invokeBackwards(boolean startHere, Method m, Object... args) {

			Object r = null;
			ArrayList<Object> v = new ArrayList<Object>(properties.getMap().values());
			Collections.reverse(v);
			for (Object o : v) {
				if (o == this)
					continue;
				Object q = invokeBackwards(o, m, args);
				if (r == null && q != null)
					r = q;

			}

			Class<?> xx = m.getDeclaringClass();
			if (xx.isAssignableFrom(this.getClass())) {
				try {
					r = m.invoke(this, args);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					throw e;
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					IllegalArgumentException iae = new IllegalArgumentException();
					iae.initCause(e);
					throw iae;
				} catch (InvocationTargetException e) {
					e.printStackTrace();
					IllegalArgumentException iae = new IllegalArgumentException();
					iae.initCause(e);
					throw iae;
				}
			}

			return r;
		}

		private Object invoke(Object o, Method m, Object[] args) {

			// System.out.println(" o is a "+o+" "+(o==null ? null :
			// o.getClass())+" "+m.getDeclaringClass().isAssignableFrom(o.getClass()));

			if (o instanceof Cobj) {
				((Cobj) o).parent = this;
				return ((Cobj) o).invoke(true, m, args);
			} else if (o instanceof Collection) {

				Object r = null;
				for (Object oo : (Collection) o) {
					Object q = invoke(oo, m, args);
					if (r == null && q != null)
						r = q;
				}
				return r;
			} else if (m.getDeclaringClass().isAssignableFrom(o.getClass())) {
				try {
					return m.invoke(o, args);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		private Object invokeBackwards(Object o, Method m, Object[] args) {
			if (o instanceof Cobj) {
				((Cobj) o).parent = this;
				return ((Cobj) o).invokeBackwards(true, m, args);
			} else if (o instanceof Collection) {
				Object r = null;
				for (Object oo : (Collection) o) {
					Object q = invoke(oo, m, args);
					if (r == null && q != null)
						r = q;
				}
				return r;
			} else if (m.getDeclaringClass().isAssignableFrom(o.getClass())) {
				try {
					return m.invoke(o, args);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		private Map<Class, Object> proxyMap = new HashMap<Class, Object>();

		/**
		 * returns a proxy for the Java interface 't' that calls any
		 * Cobj in the tree that implements 't' in a depth first fashion
		 */
		public <T> T proxy(Class<T> t) {
			return proxy(true, t);
		}

		@HiddenInAutocomplete
		public <T> T proxy(final boolean startHere, Class<T> t) {
			Object r = proxyMap.get(t);
			if (r != null)
				return (T) r;

			r = (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { t }, new InvocationHandler() {

				@Override
				public Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable {

					// System.out.println(" --- enter proxy -- :"+arg0+" "+arg1);

					return Cobj.this.invoke(startHere, arg1, arg2);
				}
			});
			proxyMap.put(t, r);
			return (T) r;
		}

		/**
		 * returns a proxy for the Java interface 't' that calls any
		 * Cobj in the tree that implements 't' in a depth first
		 * fashion, starting at the bottom of the tree
		 */
		public <T> T proxyBackwards(final boolean startHere, Class<T> t) {
			Object r = proxyMap.get(t);
			if (r != null)
				return (T) r;

			r = (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { t }, new InvocationHandler() {

				@Override
				public Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable {
					return Cobj.this.invokeBackwards(startHere, arg1, arg2);
				}
			});
			proxyMap.put(t, r);
			return (T) r;
		}

		@HiddenInAutocomplete
		public void depthFirst(iVisitor v) {
			v.enter(this);
			try {
				v.visit(this);
				Collection<Cobj> c = children();
				for (Cobj cc : c)
					cc.depthFirst(v);
			} finally {
				v.exit(this);
			}
		}

		/**
		 * calls v(x) with each 'x' in the tree depth first
		 */
		public void depthFirst(iSimpleVisitor v) {
			v.visit(this);
			Collection<Cobj> c = children();
			for (Cobj cc : c)
				cc.depthFirst(v);
		}

		/**
		 * returns all values for all properties 'a' in the tree (in
		 * depth first order)
		 */
		public Collection getAll(String a) {
			return getAll(new Prop(a));
		}

		@HiddenInAutocomplete
		public <T> Collection<T> getAll(final Prop<T> p) {
			final ArrayList<T> tt = new ArrayList<T>();
			depthFirst(new iVisitor() {

				@Override
				public void visit(Cobj c) {
					T t = c.properties.get(p);
					if (t != null)
						tt.add(t);
				}

				@Override
				public void exit(Cobj c) {
				}

				@Override
				public void enter(Cobj c) {
				}
			});
			return tt;
		}

		/**
		 * returns all Cobj x in the tree for which predicate(x) returns
		 * true
		 */
		public Collection findAll(final iFunction<Boolean, Cobj> predicate) {
			final ArrayList<Cobj> tt = new ArrayList<Cobj>();
			depthFirst(new iVisitor() {

				@Override
				public void visit(Cobj c) {
					Object x = predicate.f(c);
					if (x != null) {
						if (x instanceof Boolean && ((Boolean) x).booleanValue())
							tt.add(c);
						if (x instanceof Number && ((Number) x).intValue() > 0)
							tt.add(c);
					}
				}

				@Override
				public void exit(Cobj c) {
				}

				@Override
				public void enter(Cobj c) {
				}
			});
			return tt;
		}

		@Override
		@HiddenInAutocomplete
		public String toString() {
			Map<Prop, Object> m = properties.getMap();
			Set<Entry<Prop, Object>> es = m.entrySet();
			String s = "";
			for (Entry<Prop, Object> e : es) {
				s += e.getKey().getName() + ":" + (e.getValue() == this ? "(this)" : e.getValue()) + " ";
			}
			return (this.getClass().getSimpleName().length() == 0 ? this.getClass().getName() : this.getClass().getSimpleName()) + "[" + s.trim() + "]";
		}

		@Override
		@HiddenInAutocomplete
		public Object callWithKeywords(Object[] args, Map<String, Object> kw) {
			Set<Entry<String, Object>> es = kw.entrySet();
			for (Entry<String, Object> e : es) {
				setAttribute(e.getKey(), e.getValue());
			}
			return this;
		}

		@Override
		@HiddenInAutocomplete
		public Object call(Object[] args) {
			throw new IllegalArgumentException();
		}
	}

	static public class CobjList<E> extends Cobj implements List<E> {

		private Prop<List<E>> name;

		public CobjList(List<E> e, String name) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(name, e);
			callWithKeywords(new Object[] {}, m);
			this.name = new Prop<List<E>>(name);
		}

		public CobjList(PyObject[] a, String[] kw) {
			if (a.length != kw.length)
				throw new IllegalArgumentException();

			Map<String, Object> o = new HashMap<String, Object>();
			for (int i = 0; i < a.length; i++) {
				Object oo = Py.tojava(a[i], Object.class);
				o.put(kw[i], oo);
				if (i == 0) {
					this.name = new Prop<List<E>>(kw[0]);
					if (!(oo instanceof List))
						if (o.size() == 0)
							throw new IllegalArgumentException("Expected a List, got " + oo + "(" + (oo == null ? "" : oo.getClass()) + ") instead");
				}
			}
			callWithKeywords(new Object[] {}, o);
			if (o.size() == 0)
				throw new IllegalArgumentException("CobjList needs a default property to hold its list");
		}

		@Override
		public boolean add(E e) {
			return getProperty(name).add(e);
		}

		@Override
		public void add(int index, E element) {
			getProperty(name).add(index, element);
		}

		@Override
		public boolean addAll(Collection<? extends E> c) {
			return getProperty(name).addAll(c);
		}

		@Override
		public boolean addAll(int index, Collection<? extends E> c) {
			return getProperty(name).addAll(index, c);
		}

		@Override
		public void clear() {
			getProperty(name).clear();
		}

		@Override
		public boolean contains(Object o) {
			return getProperty(name).contains(o);
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			return getProperty(name).containsAll(c);
		}

		@Override
		public E get(int index) {
			return getProperty(name).get(index);
		}

		@Override
		public int indexOf(Object o) {
			return getProperty(name).indexOf(o);
		}

		@Override
		public boolean isEmpty() {
			return getProperty(name).isEmpty();
		}

		@Override
		public Iterator<E> iterator() {
			return getProperty(name).iterator();
		}

		@Override
		public int lastIndexOf(Object o) {
			return getProperty(name).lastIndexOf(o);
		}

		@Override
		public ListIterator<E> listIterator() {
			return getProperty(name).listIterator();
		}

		@Override
		public ListIterator<E> listIterator(int index) {
			return getProperty(name).listIterator(index);
		}

		@Override
		public boolean remove(Object o) {
			return getProperty(name).remove(o);
		}

		@Override
		public E remove(int index) {
			return getProperty(name).remove(index);
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			return getProperty(name).removeAll(c);
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			return getProperty(name).retainAll(c);
		}

		@Override
		public E set(int index, E element) {
			return getProperty(name).set(index, element);
		}

		@Override
		public int size() {
			return getProperty(name).size();
		}

		@Override
		public List<E> subList(int fromIndex, int toIndex) {
			return getProperty(name).subList(fromIndex, toIndex);
		}

		@Override
		public Object[] toArray() {
			return getProperty(name).toArray();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			return getProperty(name).toArray(a);
		}

	}

	static public class DebugToString implements iVisitor {

		String indent = "";

		@Override
		public void enter(Cobj c) {
			indent = indent + " ";
		}

		@Override
		public void visit(Cobj c) {
			System.out.println(indent + c);
		}

		@Override
		public void exit(Cobj c) {
			indent = indent.substring(1);
		}

	}

	static public abstract class UpdatableCobj extends Cobj implements iUpdateable {

	}

	// helper for Jython
	static public class UpdatableCobjAsFunction extends UpdatableCobj {

		private iFunction<Object, UpdatableCobj> u;

		public UpdatableCobjAsFunction(iFunction<Object, UpdatableCobj> u) {
			this.u = u;
		}

		@Override
		public void update() {
			u.f(this);
		}
	}

}
