package field.core.plugins.drawing.embedded;
import java.lang.reflect.Method;
import field.namespace.generic.ReflectionTools;
import field.bytecode.apt.*;

import field.namespace.generic.Bind.*;

import field.namespace.generic.Bind.iFunction;

import java.lang.reflect.*;

import java.util.*;

import field.math.abstraction.*;

import field.launch.*;

import field.core.plugins.drawing.embedded.iNodeCallBack;

import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.math.linalg.Vector2;
import java.awt.event.MouseEvent;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.math.linalg.Vector2;
import java.awt.event.MouseEvent;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.math.linalg.Vector2;
import java.awt.event.MouseEvent;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.math.linalg.Vector2;
import java.awt.event.MouseEvent;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.math.linalg.Vector2;
import java.awt.event.MouseEvent;
public class iNodeCallBack_m {
static public final Method mouseDown_m = ReflectionTools.methodOf("mouseDown", field.core.plugins.drawing.embedded.iNodeCallBack.class, field.core.plugins.drawing.opengl.CachedLine.class, field.core.plugins.drawing.opengl.CachedLine.Event.class, field.math.linalg.Vector2.class, java.awt.event.MouseEvent.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.plugins.drawing.embedded.iNodeCallBack, Object[]> mouseDown_s = new Mirroring.MirrorNoReturnMethod<field.core.plugins.drawing.embedded.iNodeCallBack, Object[]>(field.core.plugins.drawing.embedded.iNodeCallBack.class, "mouseDown", new Class[]{field.core.plugins.drawing.opengl.CachedLine.class, field.core.plugins.drawing.opengl.CachedLine.Event.class, field.math.linalg.Vector2.class, java.awt.event.MouseEvent.class});

public interface mouseDown_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void mouseDown( final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3);
	public iUpdateable updateable(final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3);}

public final mouseDown_interface mouseDown;

static public final Method mouseDragged_m = ReflectionTools.methodOf("mouseDragged", field.core.plugins.drawing.embedded.iNodeCallBack.class, field.core.plugins.drawing.opengl.CachedLine.class, field.core.plugins.drawing.opengl.CachedLine.Event.class, field.math.linalg.Vector2.class, java.awt.event.MouseEvent.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.plugins.drawing.embedded.iNodeCallBack, Object[]> mouseDragged_s = new Mirroring.MirrorNoReturnMethod<field.core.plugins.drawing.embedded.iNodeCallBack, Object[]>(field.core.plugins.drawing.embedded.iNodeCallBack.class, "mouseDragged", new Class[]{field.core.plugins.drawing.opengl.CachedLine.class, field.core.plugins.drawing.opengl.CachedLine.Event.class, field.math.linalg.Vector2.class, java.awt.event.MouseEvent.class});

public interface mouseDragged_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void mouseDragged( final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3);
	public iUpdateable updateable(final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3);}

public final mouseDragged_interface mouseDragged;

static public final Method mouseUp_m = ReflectionTools.methodOf("mouseUp", field.core.plugins.drawing.embedded.iNodeCallBack.class, field.core.plugins.drawing.opengl.CachedLine.class, field.core.plugins.drawing.opengl.CachedLine.Event.class, field.math.linalg.Vector2.class, java.awt.event.MouseEvent.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.plugins.drawing.embedded.iNodeCallBack, Object[]> mouseUp_s = new Mirroring.MirrorNoReturnMethod<field.core.plugins.drawing.embedded.iNodeCallBack, Object[]>(field.core.plugins.drawing.embedded.iNodeCallBack.class, "mouseUp", new Class[]{field.core.plugins.drawing.opengl.CachedLine.class, field.core.plugins.drawing.opengl.CachedLine.Event.class, field.math.linalg.Vector2.class, java.awt.event.MouseEvent.class});

public interface mouseUp_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void mouseUp( final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3);
	public iUpdateable updateable(final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3);}

public final mouseUp_interface mouseUp;

static public final Method mouseClicked_m = ReflectionTools.methodOf("mouseClicked", field.core.plugins.drawing.embedded.iNodeCallBack.class, field.core.plugins.drawing.opengl.CachedLine.class, field.core.plugins.drawing.opengl.CachedLine.Event.class, field.math.linalg.Vector2.class, java.awt.event.MouseEvent.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.plugins.drawing.embedded.iNodeCallBack, Object[]> mouseClicked_s = new Mirroring.MirrorNoReturnMethod<field.core.plugins.drawing.embedded.iNodeCallBack, Object[]>(field.core.plugins.drawing.embedded.iNodeCallBack.class, "mouseClicked", new Class[]{field.core.plugins.drawing.opengl.CachedLine.class, field.core.plugins.drawing.opengl.CachedLine.Event.class, field.math.linalg.Vector2.class, java.awt.event.MouseEvent.class});

public interface mouseClicked_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void mouseClicked( final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3);
	public iUpdateable updateable(final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3);}

public final mouseClicked_interface mouseClicked;

static public final Method menu_m = ReflectionTools.methodOf("menu", field.core.plugins.drawing.embedded.iNodeCallBack.class, field.core.plugins.drawing.opengl.CachedLine.class, field.core.plugins.drawing.opengl.CachedLine.Event.class, field.math.linalg.Vector2.class, java.awt.event.MouseEvent.class);
static public final Mirroring.MirrorMethod<field.core.plugins.drawing.embedded.iNodeCallBack, java.util.Map, Object[]> menu_s = new Mirroring.MirrorMethod<field.core.plugins.drawing.embedded.iNodeCallBack, java.util.Map, Object[]>(field.core.plugins.drawing.embedded.iNodeCallBack.class, "menu", new Class[]{field.core.plugins.drawing.opengl.CachedLine.class, field.core.plugins.drawing.opengl.CachedLine.Event.class, field.math.linalg.Vector2.class, java.awt.event.MouseEvent.class});

public interface menu_interface extends iAcceptor<Object[]>, iFunction<java.util.Map ,Object[] >
	{
		public java.util.Map<java.lang.String,java.lang.Object> menu( final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3);
	public iUpdateable updateable(final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3);
public iProvider<java.util.Map> bind(final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3);
}

public final menu_interface menu;

public iNodeCallBack_m(final iNodeCallBack x) {
		mouseDown = new mouseDown_interface()
		{
			
			iAcceptor a = mouseDown_s.acceptor(x);
			iFunction f = mouseDown_s.function(x);

			
			public void mouseDown (final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3)
			{
				 x.mouseDown(p0, p1, p2, p3 );
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
			
		public iUpdateable updateable(final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3)
	{
		return new iUpdateable()
		{
			public void update()
			{
				mouseDown(p0, p1, p2, p3);
			}
		};
	}
		};

		mouseDragged = new mouseDragged_interface()
		{
			
			iAcceptor a = mouseDragged_s.acceptor(x);
			iFunction f = mouseDragged_s.function(x);

			
			public void mouseDragged (final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3)
			{
				 x.mouseDragged(p0, p1, p2, p3 );
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
			
		public iUpdateable updateable(final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3)
	{
		return new iUpdateable()
		{
			public void update()
			{
				mouseDragged(p0, p1, p2, p3);
			}
		};
	}
		};

		mouseUp = new mouseUp_interface()
		{
			
			iAcceptor a = mouseUp_s.acceptor(x);
			iFunction f = mouseUp_s.function(x);

			
			public void mouseUp (final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3)
			{
				 x.mouseUp(p0, p1, p2, p3 );
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
			
		public iUpdateable updateable(final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3)
	{
		return new iUpdateable()
		{
			public void update()
			{
				mouseUp(p0, p1, p2, p3);
			}
		};
	}
		};

		mouseClicked = new mouseClicked_interface()
		{
			
			iAcceptor a = mouseClicked_s.acceptor(x);
			iFunction f = mouseClicked_s.function(x);

			
			public void mouseClicked (final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3)
			{
				 x.mouseClicked(p0, p1, p2, p3 );
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
			
		public iUpdateable updateable(final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3)
	{
		return new iUpdateable()
		{
			public void update()
			{
				mouseClicked(p0, p1, p2, p3);
			}
		};
	}
		};

		menu = new menu_interface()
		{
			
			iAcceptor a = menu_s.acceptor(x);
			iFunction f = menu_s.function(x);

			
			public java.util.Map<java.lang.String,java.lang.Object> menu (final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3)
			{
				return x.menu(p0, p1, p2, p3 );
			}
			
			public iAcceptor<Object[]> set(Object[] p)
			{
				a.set(p);
				return this;
			}
			
			public java.util.Map f(Object[] p)
			{
				return (java.util.Map) f.f(p);
			}
			
		public iUpdateable updateable(final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3)
	{
		return new iUpdateable()
		{
			public void update()
			{
				menu(p0, p1, p2, p3);
			}
		};
	}
	public iProvider<java.util.Map> bind(final field.core.plugins.drawing.opengl.CachedLine p0, final field.core.plugins.drawing.opengl.CachedLine.Event p1, final field.math.linalg.Vector2 p2, final java.awt.event.MouseEvent p3){
return new iProvider(){public Object get(){return menu(p0, p1, p2, p3);}};}};


}
}

