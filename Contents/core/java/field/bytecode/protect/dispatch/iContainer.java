package field.bytecode.protect.dispatch;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import field.launch.iUpdateable;
import field.namespace.generic.ReflectionTools;


public interface iContainer {

	public List propagateTo(String tag, Class clazz, Method method, Object... args);
	
	static public class MakeProxy
	{
		DispatchOverContainer container = new DispatchOverContainer();
		
		public <T> T makeProxyFor(final Class<T> interfase, final iContainer on, final String tag)
		{
			return (T) Proxy.newProxyInstance(on.getClass().getClassLoader(), new Class[]{interfase}, new InvocationHandler(){
			
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					container.dispatch(tag, interfase, method, on, args);
					return null;
				}
			});
		}
	}
	
	static public class DispatchOverContainer
	{
		public void dispatch(String tag, Class clazz, Method method, iContainer on, Object... args)
		{
			if (clazz == null) clazz = on.getClass();
			
			List list = on.propagateTo(tag, clazz, method, args);
			if (list == null) return ;
			
			for(int i=0;i<list.size();i++)
			{
				Object o = list.get(i);
				if (clazz.isInstance(o))
				{
					try {
						method.invoke(o, args);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			}
			
			for(int i=0;i<list.size();i++)
			{
				Object o = list.get(i);
				if (o instanceof iContainer)
				{
					dispatch(tag, clazz, method, (iContainer) o, args);
				}
			}
		}
		
		public void dispatchBackwards(String tag, Class clazz, Method method, iContainer on, Object... args)
		{
			if (clazz == null) clazz = on.getClass();
			List list = on.propagateTo(tag, clazz, method, args);
			if (list == null) return ;
			for(int i=0;i<list.size();i++)
			{
				Object o = list.get(list.size()-1-i);
				if (clazz.isInstance(o))
				{
					try {
						method.invoke(o, args);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			}
			
			for(int i=0;i<list.size();i++)
			{
				Object o = list.get(list.size()-1-i);
				if (o instanceof iContainer)
				{
					dispatch(tag, clazz, method, (iContainer) o, args);
				}
			}
		}
	}
	
	public interface iContainerUpdateable extends iContainer, iUpdateable
	{
		static public final Method method_update = ReflectionTools.methodOf("update", iContainerUpdateable.class);
	}
	
	public interface iContainerUpdateableAtTime extends iContainer
	{
		static public final Method method_update = ReflectionTools.methodOf("update", iContainerUpdateable.class, Double.TYPE );

		public void update(double time);
	}
}
