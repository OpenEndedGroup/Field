package field.namespace.key;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import field.namespace.context.CT.ContextTopology;



/**
 * A class that is primarly meant to be used to create unique pointers which
 * have a string label.
 * <p>
 * Keys  are used extensively in this system when interacting with<code>
 * contextTrees </code> , such as <code> WorkingMemory </code>. This use is
 * documented in <code> iKeyInterfaces </code>.
 * <p>
 * if assertions are enabled at runtime (java -ea:innards.Key) this class will generate stack traces for allocations of keys.
 * (this will only be true on 1.4) for information on how to set this see:
 * http://java.sun.com/j2se/1.4/docs/guide/lang/assert.html
 * <p>
 * note, if you are going to programmatically create keys, and then expect them to be garbage collected out of the context tree
 * you might want to look at BetterWeakHashMap.BaseXXX instead.
 * @see innards.iKeyInterfaces
 * @author synchar
 */
public class Key implements Serializable
{
	static public Map internedKeys = new WeakHashMap();
	static public Map internedKeys_byTree = new WeakHashMap();
	
	protected String rep;

	protected Key(){}
	
	public Key(String s)
	{
		this.rep = s;
		
		// proof of concept - zero cost (at non-debug time) allocation stack trace
		//assert generateAllocationStackTrace() : "generate allocation stack trace (should never fail)";
	
		generateAllocationStackTrace();
		
		assert(!internedKeys.containsKey(rep)): "can't have two keys with the same string... <"+s+">";
		
		if (!internedKeys.containsKey(s)) internedKeys.put(s, new WeakReference(this));
	}
	
	public Key(String s, ContextTopology lt)
	{
		this.rep = s;
		
		
		// proof of concept - zero cost (at non-debug time) allocation stack trace
		//assert generateAllocationStackTrace() : "generate allocation stack trace (should never fail)";
	
		//assert(!internedKeys.containsKey(rep)): "can't have two keys with the same string... <"+s+">";
		
		if (!internedKeys.containsKey(s)) internedKeys.put(s, new WeakReference(this));

		Map o = (Map) internedKeys_byTree.get(lt);
		if ( o == null)
			internedKeys_byTree.put(lt, o = new HashMap());
		o.put(s, new WeakReference(this));
		
	}
	
	
	public String getName()
	{
		return rep;
	}

	public String toString()
	{
		return rep;
	}
	

	/**
	 *  for persistance --------------------------------------------------------------------------------------------------------
	 */
	
	/**
	 * throws IllegalArgumentException if the key cannot be found in the map. This call is (and should only be) used for 
	 * persisting objects that mention keys
	 */
	static public Key internKey(Key k)
	{
		Reference r = (Reference)internedKeys.get(k.rep);
		if (r==null) throw new IllegalArgumentException(" couldn't find already interned key called <"+k.rep+">");
		Key found = (Key)(r.get());
		if (found==null) throw new IllegalArgumentException(" couldn't find already interned key called <"+k.rep+">");
		return found;
	}
	
	
	/**
	 * proof of concept - zero cost (at non-debug time) allocation stack trace ----------------------------------
	 */
	
	transient protected StackTraceElement[] allocationStackTrace = null;
	protected boolean generateAllocationStackTrace()
	{
		allocationStackTrace = new Exception().getStackTrace();
		return true;
	}
	
	/**I
	 * will return null if asserts are not enabled for innards.*
	 * @return StackTraceElement[]
	 */
	
	public StackTraceElement[] getAllocationStackTrace()
	{
		return allocationStackTrace;
	}
	
	public StackTraceElement whereAllocated()
	{
		if (allocationStackTrace==null) return null;
		return allocationStackTrace[allocationStackTrace.length-2];
	}

	
	/**
	 * for the deserialization of keys from \u2014 if you really care about what this is doing, you'll have
	 * to read the Java Object Serialization Specification.
	 */
	Object readResolve()  throws ObjectStreamException
	{
		Object o = internedKeys.get(this.rep);
		o = o==null ? this : ((WeakReference)o).get();
		return o==null ? this : o;
	}
	
	public Object skaReadResolve()
	{
		try {
			return readResolve();
		} catch (ObjectStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this;
	}
	
	public static Class loadClass(Class c)
	{
		Field[] f = c.getFields();
		for(int i=0;i<f.length;i++)
		{
			if (Modifier.isStatic(f[i].getModifiers()))
				try {
					f[i].get(null);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
		}
		return c;		
	}
}