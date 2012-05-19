package field.core.plugins.drawing.opengl;
import java.lang.reflect.Method;
import field.namespace.generic.ReflectionTools;
import field.bytecode.apt.*;

import field.namespace.generic.Bind.*;

import field.namespace.generic.Bind.iFunction;

import java.lang.reflect.*;

import java.util.*;

import field.math.abstraction.*;

import field.launch.*;

import field.core.plugins.drawing.opengl.iLine;

import field.util.Dict.Prop;
public class iLine_m {
static public final Method moveTo_m = ReflectionTools.methodOf("moveTo", field.core.plugins.drawing.opengl.iLine.class, float.class, float.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.plugins.drawing.opengl.iLine, Object[]> moveTo_s = new Mirroring.MirrorNoReturnMethod<field.core.plugins.drawing.opengl.iLine, Object[]>(field.core.plugins.drawing.opengl.iLine.class, "moveTo", new Class[]{float.class, float.class});

public interface moveTo_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void moveTo( final float p0, final float p1);
	public iUpdateable updateable(final float p0, final float p1);}

public final moveTo_interface moveTo;

static public final Method lineTo_m = ReflectionTools.methodOf("lineTo", field.core.plugins.drawing.opengl.iLine.class, float.class, float.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.plugins.drawing.opengl.iLine, Object[]> lineTo_s = new Mirroring.MirrorNoReturnMethod<field.core.plugins.drawing.opengl.iLine, Object[]>(field.core.plugins.drawing.opengl.iLine.class, "lineTo", new Class[]{float.class, float.class});

public interface lineTo_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void lineTo( final float p0, final float p1);
	public iUpdateable updateable(final float p0, final float p1);}

public final lineTo_interface lineTo;

static public final Method cubicTo_m = ReflectionTools.methodOf("cubicTo", field.core.plugins.drawing.opengl.iLine.class, float.class, float.class, float.class, float.class, float.class, float.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.plugins.drawing.opengl.iLine, Object[]> cubicTo_s = new Mirroring.MirrorNoReturnMethod<field.core.plugins.drawing.opengl.iLine, Object[]>(field.core.plugins.drawing.opengl.iLine.class, "cubicTo", new Class[]{float.class, float.class, float.class, float.class, float.class, float.class});

public interface cubicTo_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void cubicTo( final float p0, final float p1, final float p2, final float p3, final float p4, final float p5);
	public iUpdateable updateable(final float p0, final float p1, final float p2, final float p3, final float p4, final float p5);}

public final cubicTo_interface cubicTo;

static public final Method setPointAttribute_m = ReflectionTools.methodOf("setPointAttribute", field.core.plugins.drawing.opengl.iLine.class, field.util.Dict.Prop.class, Object.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.plugins.drawing.opengl.iLine, Object[]> setPointAttribute_s = new Mirroring.MirrorNoReturnMethod<field.core.plugins.drawing.opengl.iLine, Object[]>(field.core.plugins.drawing.opengl.iLine.class, "setPointAttribute", new Class[]{field.util.Dict.Prop.class, Object.class});

public interface setPointAttribute_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void setPointAttribute( final field.util.Dict.Prop p0, final Object p1);
	public iUpdateable updateable(final field.util.Dict.Prop p0, final Object p1);}

public final setPointAttribute_interface setPointAttribute;

static public final Method close_m = ReflectionTools.methodOf("close", field.core.plugins.drawing.opengl.iLine.class);
static public final Mirroring.MirrorNoReturnNoArgsMethod<field.core.plugins.drawing.opengl.iLine> close_s = new Mirroring.MirrorNoReturnNoArgsMethod<field.core.plugins.drawing.opengl.iLine>(field.core.plugins.drawing.opengl.iLine.class, "close");

public final iUpdateable close;
public iLine_m(final iLine x) {
		moveTo = new moveTo_interface()
		{
			
			iAcceptor a = moveTo_s.acceptor(x);
			iFunction f = moveTo_s.function(x);

			
			public void moveTo (final float p0, final float p1)
			{
				 x.moveTo(p0, p1 );
			}
			
			public iAcceptor<Object[]> set(Object[] p)
			{
				a.set(p);
				return this;
			}
			
			public Object f(Object[] p)
			{
				return (Object) f.f(p);
			}
			
		public iUpdateable updateable(final float p0, final float p1)
	{
		return new iUpdateable()
		{
			public void update()
			{
				moveTo(p0, p1);
			}
		};
	}
		};

		lineTo = new lineTo_interface()
		{
			
			iAcceptor a = lineTo_s.acceptor(x);
			iFunction f = lineTo_s.function(x);

			
			public void lineTo (final float p0, final float p1)
			{
				 x.lineTo(p0, p1 );
			}
			
			public iAcceptor<Object[]> set(Object[] p)
			{
				a.set(p);
				return this;
			}
			
			public Object f(Object[] p)
			{
				return (Object) f.f(p);
			}
			
		public iUpdateable updateable(final float p0, final float p1)
	{
		return new iUpdateable()
		{
			public void update()
			{
				lineTo(p0, p1);
			}
		};
	}
		};

		cubicTo = new cubicTo_interface()
		{
			
			iAcceptor a = cubicTo_s.acceptor(x);
			iFunction f = cubicTo_s.function(x);

			
			public void cubicTo (final float p0, final float p1, final float p2, final float p3, final float p4, final float p5)
			{
				 x.cubicTo(p0, p1, p2, p3, p4, p5 );
			}
			
			public iAcceptor<Object[]> set(Object[] p)
			{
				a.set(p);
				return this;
			}
			
			public Object f(Object[] p)
			{
				return (Object) f.f(p);
			}
			
		public iUpdateable updateable(final float p0, final float p1, final float p2, final float p3, final float p4, final float p5)
	{
		return new iUpdateable()
		{
			public void update()
			{
				cubicTo(p0, p1, p2, p3, p4, p5);
			}
		};
	}
		};

		setPointAttribute = new setPointAttribute_interface()
		{
			
			iAcceptor a = setPointAttribute_s.acceptor(x);
			iFunction f = setPointAttribute_s.function(x);

			
			public void setPointAttribute (final field.util.Dict.Prop p0, final Object p1)
			{
				 x.setPointAttribute(p0, p1 );
			}
			
			public iAcceptor<Object[]> set(Object[] p)
			{
				a.set(p);
				return this;
			}
			
			public Object f(Object[] p)
			{
				return (Object) f.f(p);
			}
			
		public iUpdateable updateable(final field.util.Dict.Prop p0, final Object p1)
	{
		return new iUpdateable()
		{
			public void update()
			{
				setPointAttribute(p0, p1);
			}
		};
	}
		};

close = close_s.updateable(x);

}
}

