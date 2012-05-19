package field.bytecode.protect.yield;

import java.util.Arrays;
import java.util.HashMap;
import java.util.WeakHashMap;

public class YieldSupport {

	static WeakHashMap<Object, YieldInfo> globalYieldInfo = new WeakHashMap<Object, YieldInfo>();

	/**
	 * note, yields are stored per method, not per instance (globally, that is)
	 */
	static public YieldInfo findYieldInfoFor(Object o)
	{
		return globalYieldInfo.get(o);
	}
	
	public WeakHashMap<Object, YieldInfo> localYieldInfo = new WeakHashMap<Object, YieldInfo>();

	public YieldSupport() {
	}

	public int yieldIndexFor(String uniq, Object fromThis, HashMap<String, Object> parameters) {

		YieldInfo yi = localYieldInfo.get(fromThis);
		if (yi == null) {
			// TODO:  handle recursion correctly
			localYieldInfo.put(fromThis, yi = new YieldInfo());
			globalYieldInfo.put(fromThis, yi);
		}
		return yi.next;
	}

	public Object yieldStore(Object wasReturn, Object[] localStorage, String string, Object fromThis, int resumeLabel) {
		
		YieldInfo yi;
		localYieldInfo.put(fromThis, yi = new YieldInfo());
		globalYieldInfo.put(fromThis, yi);
		assert yi != null : "yieldStore befor yeildIndexFor";
		yi.localStack = localStorage;
		yi.lastReturn = wasReturn;
		yi.next = resumeLabel;
		
		//System.out.println(" yield store is <"+yi+">");
		
		return wasReturn;
	}

	public Object[] yieldLoad(Object fromThis) {
		YieldInfo yi = localYieldInfo.remove(fromThis);
		globalYieldInfo.remove(fromThis);
		assert yi != null : "yieldLoad befor yeildIndexFor";
		
		//System.out.println(" yield load is <"+yi+">");

		return yi.localStack;
	}

	public class YieldInfo {
		public Object[] localStack;

		public Object lastReturn;

		public int next;
		
		@Override
		public String toString() {
			return "yi:"+next+"-> "+lastReturn+" ("+Arrays.asList(localStack)+")";
		}
	}

	public static void setYieldInfoFor(Object on, YieldInfo y) {
		YieldInfo info = globalYieldInfo.get(on);
		if (info==null)
			assert false : " no global yield info for <"+on+"> this probably means that set yield info for was either called while this method was executing, after this method >returned< rather than yielded, or before this method was ever called";
		info.lastReturn = y.lastReturn;
		info.localStack = y.localStack;
		info.next = info.next;
	}

}
