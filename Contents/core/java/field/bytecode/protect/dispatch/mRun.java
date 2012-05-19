package field.bytecode.protect.dispatch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import field.bytecode.protect.dispatch.Cont.ReturnCode;
import field.bytecode.protect.dispatch.Cont.aRun;
import field.namespace.generic.ReflectionTools;


public class mRun<T> extends aRun {

	private final Method m;

	private Method localM;

	protected T cthis;

	private final boolean before;

	public mRun(Method m) {
		this(m, true);
	}

	public mRun(Method m, boolean before) {
		this.m = m;
		this.before = before;
		// find a method with the same arguments
		this.localM = ReflectionTools.findMethodWithParameters(m.getName(), m.getParameterTypes(), ReflectionTools.getAllMethods(this.getClass()));
		if (this.localM == null) this.localM =  ReflectionTools.findMethodWithParameters(m.getParameterTypes(), this.getClass().getDeclaredMethods());
		assert this.localM !=null : " no method for parameters "+Arrays.asList( m.getParameterTypes());
		this.localM.setAccessible(true);
	}

	@Override
	public ReturnCode head(Object calledOn, Object[] args) {
		if (before) {
			cthis = (T) calledOn;
			try {
				localM.invoke(this, args);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			} catch (InvocationTargetException e) {
				throw new IllegalArgumentException(e);
			}
			// important for gc
			cthis = null;
		}
		return ReturnCode.cont;
	}
	
	protected void remove()
	{
		Cont.unlinkWith(cthis, m, this);
	}

	@Override
	public ReturnCode tail(Object calledOn, Object[] args, Object returnWas) {
		if (!before)
		{
			cthis = (T) calledOn;
			try {
				localM.invoke(this, args);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			} catch (InvocationTargetException e) {
				throw new IllegalArgumentException(e);
			}	
			// important for gc
			cthis = null;
		}
		return ReturnCode.cont;
	}

}
