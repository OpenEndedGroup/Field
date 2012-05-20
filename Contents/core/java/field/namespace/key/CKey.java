package field.namespace.key;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import field.math.abstraction.iFloatProvider;
import field.namespace.context.CT;
import field.namespace.context.SimpleContextTopology;
import field.namespace.context.CT.iStorage;
import field.namespace.context.SimpleContextTopology.Context;
import field.namespace.generic.ReflectionTools;

/**
 * the base class for "typed keys". this is abstract, becuase it lacks the type right now, look at OKey if you don't care
 * 
 * todo: caching strategy
 * 
 * @author marc
 */
abstract public class CKey extends Key {
	
	static public final SimpleContextTopology tree= new SimpleContextTopology();
	protected final SimpleContextTopology localContextTree;

	static boolean debug = false;

	// todo: should be meta
	transient protected List executionStack = new ArrayList();

	public CKey(String s) {
		super(s);
		localContextTree = tree;
	}
	
	public CKey(String s, SimpleContextTopology ct) {
		super(s, ct);
		this.localContextTree = ct;
	}

	// here are some of the things that one can do to a key, specifically, things one can do to an execution stack

	protected interface StackElement {
		public Object enter(Object o);

		public Object exit(Object o);
	}

	public CKey debug() {
		debug = true;

		Iterator i = executionStack.iterator();
		while(i.hasNext())
			;//System.out.println(i.next());
		return this;
	}

	public CKey lookup(final String lookup) {
		executionStack.add(new StackElement() {
			public Object enter(Object o) {
				Object ret = localContextTree.get(lookup, o);
				return ret;
			}

			public Object exit(Object o) {
				return o;
			}

			public String toString() {
				return "lookup:" + lookup;
			}
		});
		return this;
	}
	
	public CKey key(final CKey key)
	{
		executionStack.add(new StackElement() {
			public Object enter(Object o) {
				Object ret = key.run(o);
				return ret;
			}

			public Object exit(Object o) {
				return o;
			}

			public String toString() {
				return "lookup(key):" + key;
			}
		});
		return this;
	}

	public CKey then(Phi f) {
		executionStack.add(f);
		return this;
	}

	public CKey otherwise(Phi f) {
		executionStack.add(new Otherwise(f));
		return this;
	}

	public CKey defaults(Phi f) {
		executionStack.add(new Defaults(f));
		return this;
	}

	public CKey with(Key k, Object e) {
		executionStack.add(new With(k, e));
		return this;
	}

	public CKey with(Key k, float e) {
		executionStack.add(new With(k, new Float(e)));
		return this;
	}

	transient List pushStack = new LinkedList();

	public CKey push(Key k) {
		pushStack.add(localContextTree.getAt());
		localContextTree.begin(k.toString());
		return this;
	}

	public CKey pop() {
		localContextTree.setAt((Context) pushStack.remove(pushStack.size() - 1));
		return this;
	}

	public CKey pushRoot() {
		pushStack.add(localContextTree.getAt());
		localContextTree.setAt(localContextTree.root());
		return this;
	}

	protected class Otherwise implements StackElement {
		Phi f;

		public Otherwise(Phi f) {
			this.f = f;
		}

		public Object enter(Object o) {
			if (o == failure)
				o = f.enter(null);
			return o;
		}

		public Object exit(Object o) {
			return o;
		}
	}

	protected class Defaults implements StackElement {
		Phi f;

		public Defaults(Phi f) {
			this.f = f;
		}

		public Object enter(Object o) {
			return o;
		}

		public Object exit(Object o) {
			if (o == failure)
				o = f.enter(null);
			return o;
		}

		public String toString() {
			return "(default to <" + f + ">)";
		}
	}

	protected class With implements StackElement {
		Key k;
		Object value;

		public With(Key k, Object value) {
			this.k = k;
			this.value = value;
		}

		Object oldValue;

		public Object enter(Object o) {
			oldValue = localContextTree.get(k.getName(), failure);
			return o;
		}

		public Object exit(Object o) {
			if (oldValue != failure)
				localContextTree.set(k.getName(), oldValue);
			return o;
		}
	}

	// here are the elements that we need

	static public abstract class Phi implements StackElement {
		boolean nullIsFailure = true;

		StackTraceElement[] allocationStackTrace;

		public Phi() {
			if (debug) {
				allocationStackTrace = new Exception().getStackTrace();
			}
		}

		Method objectCall;
		Method floatCall;
		Method intCall;
		Method voidCall;
		boolean cached = false;

		public Object base(Object o) {
			// crazy cached reflection party

			// find a method that takes either an object, a float or an int or a void that is in the declaring class
			if (!cached) {
				Class clazz = this.getClass();
				Method[] method = clazz.getDeclaredMethods();

				objectCall = ReflectionTools.findMethodWithParameters(new Class[]{Object.class}, method);
				if (objectCall == null)
					floatCall = ReflectionTools.findMethodWithParameters(new Class[]{Float.TYPE}, method);
				if ((objectCall == null) && (floatCall == null))
					intCall = ReflectionTools.findMethodWithParameters(new Class[]{Integer.TYPE}, method);
				if ((objectCall == null) && (floatCall == null) && (intCall == null))
					voidCall = ReflectionTools.findMethodWithParameters(new Class[]{}, method);

				if ((objectCall == null) && (floatCall == null) && (intCall == null) && (voidCall == null)) {
					throw new PhiException("lookup inside BasePhi failed, couldn't find any declared method with parameter type Object, float, int or void", allocationStackTrace);
				}
				cached = true;
			}

			try {
				
				System.err.println(" calling <"+objectCall+"> <"+floatCall+"> <"+intCall+"> <"+voidCall+">");
				
				if (objectCall != null)
					o = objectCall.invoke(this, new Object[]{o});
				else if (floatCall != null)
					o = floatCall.invoke(this, new Object[]{new Float(toFloat(o))});
				else if (intCall != null)
					o = floatCall.invoke(this, new Object[]{new Integer((int) toFloat(o))});
				else if (voidCall != null)
					o = voidCall.invoke(this, new Object[]{});

				return nullIsFailure ? (o == null ? failure : o) : o;
			} catch (IllegalArgumentException e) {
				PhiException e2 = new PhiException("call to BasePhi method failed", allocationStackTrace);
				e2.initCause(e);
				throw e2;
			} catch (IllegalAccessException e) {
				PhiException e2 = new PhiException("call to BasePhi method failed", allocationStackTrace);
				e2.initCause(e);
				throw e2;
			} catch (InvocationTargetException e) {
				PhiException e2 = new PhiException("call to BasePhi method failed", allocationStackTrace);
				e2.initCause(e);
				throw e2;
			}
		}

		public Object enter(Object o) {
			return base(o);
		}

		public Object exit(Object o) {
			return o;
		}
	}

	static protected float toFloat(Object o) {
		if (o instanceof Number)
			return ((Number) o).floatValue();
		if (o instanceof iFloatProvider)
			return ((iFloatProvider) o).evaluate();
		if (o instanceof CKey)
			return ((CKey) o).asFloat();
		throw new PhiException(" toFloat, failed, got class <" + o + "> <" + (o == null ? null : o.getClass()) + ">");
	}

	protected float asFloat() {
		Object o = run(failure);
		try{
		return toFloat(o);
		}
		catch( PhiException e)
		{
			e.printStackTrace();
			throw e;
		}
	}

	protected Object run(Object o) {
		if (debug)
			;//System.out.println(" run: stack is <" + executionStack + ">");

		for (int i = 0; i < executionStack.size(); i++) {
			//for (int i = executionStack.size() - 1; i >= 0; i--) {
			StackElement e = (StackElement) executionStack.get(i);
			if (debug)
				;//System.out.println("##### o <" + o + ">");
			o = e.enter(o);
			if (debug)
				;//System.out.println("##### becomes o <" + o + "> after <" + e + ">");
		}
		//for (int i = 0; i < executionStack.size(); i++)
		for (int i = executionStack.size() - 1; i >= 0; i--) {
			StackElement e = (StackElement) executionStack.get(i);
			o = e.exit(o);
		}
		return o;
	}

	static public class PhiException extends RuntimeException {
		public PhiException(String message) {
			super(message);
		}

		public PhiException(String message, StackTraceElement[] e) {
			super(message + (e == null ? "\n    turn on asserts for better stacktrace" : "allocation stacktrace is <" + Arrays.asList(e) + ">"));
		}
	}

	static class Failure {
	}

	static protected Failure failure = new Failure();
}