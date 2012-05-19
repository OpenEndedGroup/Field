package field.math.graph;

import java.lang.reflect.Method;
import java.util.List;

import field.namespace.generic.ReflectionTools;


public interface iTopology<T> {

	public List<T> getParentsOf(T of);
	public List<T> getChildrenOf(T of);
	
	/**
	 * can act as it's own notification
	 */
	public interface iMutableTopology<T> extends iTopology<T>
	{
		static public Method method_begin = ReflectionTools.methodOf("begin", iMutableTopology.class);;
		static public Method method_end = ReflectionTools.methodOf("end", iMutableTopology.class);;
		static public Method method_addChild = ReflectionTools.methodOf("addChild", iMutableTopology.class, Object.class, Object.class);;
		static public Method method_removeChild = ReflectionTools.methodOf("removeChild", iMutableTopology.class, Object.class, Object.class);;
		
		public void begin();
		public void end();
		
		public void addChild(T from, T to);
		public void removeChild(T from, T to);
		
		public void registerNotify(iMutableTopology<? super T> here);
		public void deregisterNotify(iMutableTopology<? super T> here);
	}
	
	public interface iHasTopology 
	{
		public iTopology getTopology();
	}
	
	
}
