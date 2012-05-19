package field.core.windowing.components;
import java.lang.reflect.Method;
import field.namespace.generic.ReflectionTools;
import field.bytecode.apt.*;

import field.namespace.generic.Bind.*;

import field.namespace.generic.Bind.iFunction;

import java.lang.reflect.*;

import java.util.*;

import field.math.abstraction.*;

import field.launch.*;

import field.core.windowing.components.iComponent;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Event;
import field.core.dispatch.iVisualElement.Rect;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import org.eclipse.swt.widgets.Event;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import org.eclipse.swt.widgets.Event;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import org.eclipse.swt.widgets.Event;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import org.eclipse.swt.widgets.Event;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import org.eclipse.swt.widgets.Event;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import org.eclipse.swt.widgets.Event;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import org.eclipse.swt.widgets.Event;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import org.eclipse.swt.widgets.Event;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import org.eclipse.swt.widgets.Event;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import org.eclipse.swt.widgets.Event;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.math.linalg.iCoordinateFrame;
import java.util.Set;
import field.core.dispatch.iVisualElement;
public class iComponent_m {
static public final Method isHit_m = ReflectionTools.methodOf("isHit", field.core.windowing.components.iComponent.class, org.eclipse.swt.widgets.Event.class);
static public final Mirroring.MirrorMethod<field.core.windowing.components.iComponent, Float, org.eclipse.swt.widgets.Event> isHit_s = new Mirroring.MirrorMethod<field.core.windowing.components.iComponent, Float, org.eclipse.swt.widgets.Event>(field.core.windowing.components.iComponent.class, "isHit", new Class[]{org.eclipse.swt.widgets.Event.class});

public interface isHit_interface extends iAcceptor<org.eclipse.swt.widgets.Event>, iFunction<Float ,org.eclipse.swt.widgets.Event >
	{
		public float isHit( final org.eclipse.swt.widgets.Event p0);
	public iUpdateable updateable(final org.eclipse.swt.widgets.Event p0);
public iProvider<Float> bind(final org.eclipse.swt.widgets.Event p0);
}

public final isHit_interface isHit;

static public final Method hit_m = ReflectionTools.methodOf("hit", field.core.windowing.components.iComponent.class, org.eclipse.swt.widgets.Event.class);
static public final Mirroring.MirrorMethod<field.core.windowing.components.iComponent, field.core.windowing.components.iComponent, org.eclipse.swt.widgets.Event> hit_s = new Mirroring.MirrorMethod<field.core.windowing.components.iComponent, field.core.windowing.components.iComponent, org.eclipse.swt.widgets.Event>(field.core.windowing.components.iComponent.class, "hit", new Class[]{org.eclipse.swt.widgets.Event.class});

public interface hit_interface extends iAcceptor<org.eclipse.swt.widgets.Event>, iFunction<field.core.windowing.components.iComponent ,org.eclipse.swt.widgets.Event >
	{
		public field.core.windowing.components.iComponent hit( final org.eclipse.swt.widgets.Event p0);
	public iUpdateable updateable(final org.eclipse.swt.widgets.Event p0);
public iProvider<field.core.windowing.components.iComponent> bind(final org.eclipse.swt.widgets.Event p0);
}

public final hit_interface hit;

static public final Method getBounds_m = ReflectionTools.methodOf("getBounds", field.core.windowing.components.iComponent.class);
static public final Mirroring.MirrorNoArgsMethod<field.core.windowing.components.iComponent, field.core.dispatch.iVisualElement.Rect> getBounds_s = new Mirroring.MirrorNoArgsMethod<field.core.windowing.components.iComponent, field.core.dispatch.iVisualElement.Rect>(field.core.windowing.components.iComponent.class, "getBounds");

public final Mirroring.iBoundNoArgsMethod<field.core.dispatch.iVisualElement.Rect> getBounds;
static public final Method setBounds_m = ReflectionTools.methodOf("setBounds", field.core.windowing.components.iComponent.class, field.core.dispatch.iVisualElement.Rect.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, field.core.dispatch.iVisualElement.Rect> setBounds_s = new Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, field.core.dispatch.iVisualElement.Rect>(field.core.windowing.components.iComponent.class, "setBounds", new Class[]{field.core.dispatch.iVisualElement.Rect.class});

public interface setBounds_interface extends iAcceptor<field.core.dispatch.iVisualElement.Rect>, iFunction<Object ,field.core.dispatch.iVisualElement.Rect >
	{
		public void setBounds( final field.core.dispatch.iVisualElement.Rect p0);
	public iUpdateable updateable(final field.core.dispatch.iVisualElement.Rect p0);}

public final setBounds_interface setBounds;

static public final Method keyTyped_m = ReflectionTools.methodOf("keyTyped", field.core.windowing.components.iComponent.class, field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]> keyTyped_s = new Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]>(field.core.windowing.components.iComponent.class, "keyTyped", new Class[]{field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class});

public interface keyTyped_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void keyTyped( final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);
	public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);}

public final keyTyped_interface keyTyped;

static public final Method keyPressed_m = ReflectionTools.methodOf("keyPressed", field.core.windowing.components.iComponent.class, field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]> keyPressed_s = new Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]>(field.core.windowing.components.iComponent.class, "keyPressed", new Class[]{field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class});

public interface keyPressed_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void keyPressed( final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);
	public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);}

public final keyPressed_interface keyPressed;

static public final Method keyReleased_m = ReflectionTools.methodOf("keyReleased", field.core.windowing.components.iComponent.class, field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]> keyReleased_s = new Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]>(field.core.windowing.components.iComponent.class, "keyReleased", new Class[]{field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class});

public interface keyReleased_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void keyReleased( final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);
	public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);}

public final keyReleased_interface keyReleased;

static public final Method mouseClicked_m = ReflectionTools.methodOf("mouseClicked", field.core.windowing.components.iComponent.class, field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]> mouseClicked_s = new Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]>(field.core.windowing.components.iComponent.class, "mouseClicked", new Class[]{field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class});

public interface mouseClicked_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void mouseClicked( final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);
	public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);}

public final mouseClicked_interface mouseClicked;

static public final Method mousePressed_m = ReflectionTools.methodOf("mousePressed", field.core.windowing.components.iComponent.class, field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]> mousePressed_s = new Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]>(field.core.windowing.components.iComponent.class, "mousePressed", new Class[]{field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class});

public interface mousePressed_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void mousePressed( final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);
	public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);}

public final mousePressed_interface mousePressed;

static public final Method mouseReleased_m = ReflectionTools.methodOf("mouseReleased", field.core.windowing.components.iComponent.class, field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]> mouseReleased_s = new Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]>(field.core.windowing.components.iComponent.class, "mouseReleased", new Class[]{field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class});

public interface mouseReleased_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void mouseReleased( final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);
	public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);}

public final mouseReleased_interface mouseReleased;

static public final Method mouseEntered_m = ReflectionTools.methodOf("mouseEntered", field.core.windowing.components.iComponent.class, field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]> mouseEntered_s = new Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]>(field.core.windowing.components.iComponent.class, "mouseEntered", new Class[]{field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class});

public interface mouseEntered_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void mouseEntered( final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);
	public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);}

public final mouseEntered_interface mouseEntered;

static public final Method mouseExited_m = ReflectionTools.methodOf("mouseExited", field.core.windowing.components.iComponent.class, field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]> mouseExited_s = new Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]>(field.core.windowing.components.iComponent.class, "mouseExited", new Class[]{field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class});

public interface mouseExited_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void mouseExited( final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);
	public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);}

public final mouseExited_interface mouseExited;

static public final Method mouseDragged_m = ReflectionTools.methodOf("mouseDragged", field.core.windowing.components.iComponent.class, field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]> mouseDragged_s = new Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]>(field.core.windowing.components.iComponent.class, "mouseDragged", new Class[]{field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class});

public interface mouseDragged_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void mouseDragged( final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);
	public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);}

public final mouseDragged_interface mouseDragged;

static public final Method mouseMoved_m = ReflectionTools.methodOf("mouseMoved", field.core.windowing.components.iComponent.class, field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]> mouseMoved_s = new Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]>(field.core.windowing.components.iComponent.class, "mouseMoved", new Class[]{field.core.windowing.GLComponentWindow.ComponentContainer.class, org.eclipse.swt.widgets.Event.class});

public interface mouseMoved_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void mouseMoved( final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);
	public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1);}

public final mouseMoved_interface mouseMoved;

static public final Method beginMouseFocus_m = ReflectionTools.methodOf("beginMouseFocus", field.core.windowing.components.iComponent.class, field.core.windowing.GLComponentWindow.ComponentContainer.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, field.core.windowing.GLComponentWindow.ComponentContainer> beginMouseFocus_s = new Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, field.core.windowing.GLComponentWindow.ComponentContainer>(field.core.windowing.components.iComponent.class, "beginMouseFocus", new Class[]{field.core.windowing.GLComponentWindow.ComponentContainer.class});

public interface beginMouseFocus_interface extends iAcceptor<field.core.windowing.GLComponentWindow.ComponentContainer>, iFunction<Object ,field.core.windowing.GLComponentWindow.ComponentContainer >
	{
		public void beginMouseFocus( final field.core.windowing.GLComponentWindow.ComponentContainer p0);
	public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0);}

public final beginMouseFocus_interface beginMouseFocus;

static public final Method endMouseFocus_m = ReflectionTools.methodOf("endMouseFocus", field.core.windowing.components.iComponent.class, field.core.windowing.GLComponentWindow.ComponentContainer.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, field.core.windowing.GLComponentWindow.ComponentContainer> endMouseFocus_s = new Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, field.core.windowing.GLComponentWindow.ComponentContainer>(field.core.windowing.components.iComponent.class, "endMouseFocus", new Class[]{field.core.windowing.GLComponentWindow.ComponentContainer.class});

public interface endMouseFocus_interface extends iAcceptor<field.core.windowing.GLComponentWindow.ComponentContainer>, iFunction<Object ,field.core.windowing.GLComponentWindow.ComponentContainer >
	{
		public void endMouseFocus( final field.core.windowing.GLComponentWindow.ComponentContainer p0);
	public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0);}

public final endMouseFocus_interface endMouseFocus;

static public final Method paint_m = ReflectionTools.methodOf("paint", field.core.windowing.components.iComponent.class, field.core.windowing.GLComponentWindow.ComponentContainer.class, field.math.linalg.iCoordinateFrame.class, boolean.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]> paint_s = new Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]>(field.core.windowing.components.iComponent.class, "paint", new Class[]{field.core.windowing.GLComponentWindow.ComponentContainer.class, field.math.linalg.iCoordinateFrame.class, boolean.class});

public interface paint_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void paint( final field.core.windowing.GLComponentWindow.ComponentContainer p0, final field.math.linalg.iCoordinateFrame p1, final boolean p2);
	public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final field.math.linalg.iCoordinateFrame p1, final boolean p2);}

public final paint_interface paint;

static public final Method handleResize_m = ReflectionTools.methodOf("handleResize", field.core.windowing.components.iComponent.class, java.util.Set.class, float.class, float.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]> handleResize_s = new Mirroring.MirrorNoReturnMethod<field.core.windowing.components.iComponent, Object[]>(field.core.windowing.components.iComponent.class, "handleResize", new Class[]{java.util.Set.class, float.class, float.class});

public interface handleResize_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void handleResize( final java.util.Set p0, final float p1, final float p2);
	public iUpdateable updateable(final java.util.Set p0, final float p1, final float p2);}

public final handleResize_interface handleResize;

static public final Method getVisualElement_m = ReflectionTools.methodOf("getVisualElement", field.core.windowing.components.iComponent.class);
static public final Mirroring.MirrorNoArgsMethod<field.core.windowing.components.iComponent, field.core.dispatch.iVisualElement> getVisualElement_s = new Mirroring.MirrorNoArgsMethod<field.core.windowing.components.iComponent, field.core.dispatch.iVisualElement>(field.core.windowing.components.iComponent.class, "getVisualElement");

public interface getVisualElement_interface extends field.core.dispatch.iVisualElement, Mirroring.iBoundNoArgsMethod<field.core.dispatch.iVisualElement>	{
		public field.core.dispatch.iVisualElement getVisualElement( );
	}

public final getVisualElement_interface getVisualElement;

static public final Method setVisualElement_m = ReflectionTools.methodOf("setVisualElement", field.core.windowing.components.iComponent.class, field.core.dispatch.iVisualElement.class);
static public final Mirroring.MirrorMethod<field.core.windowing.components.iComponent, field.core.windowing.components.iComponent, field.core.dispatch.iVisualElement> setVisualElement_s = new Mirroring.MirrorMethod<field.core.windowing.components.iComponent, field.core.windowing.components.iComponent, field.core.dispatch.iVisualElement>(field.core.windowing.components.iComponent.class, "setVisualElement", new Class[]{field.core.dispatch.iVisualElement.class});

public interface setVisualElement_interface extends iAcceptor<field.core.dispatch.iVisualElement>, iFunction<field.core.windowing.components.iComponent ,field.core.dispatch.iVisualElement >
	{
		public field.core.windowing.components.iComponent setVisualElement( final field.core.dispatch.iVisualElement p0);
	public iUpdateable updateable(final field.core.dispatch.iVisualElement p0);
public iProvider<field.core.windowing.components.iComponent> bind(final field.core.dispatch.iVisualElement p0);
}

public final setVisualElement_interface setVisualElement;

public iComponent_m(final iComponent x) {
		isHit = new isHit_interface()
		{
			
			iAcceptor a = isHit_s.acceptor(x);
			iFunction f = isHit_s.function(x);

			
			public float isHit (final org.eclipse.swt.widgets.Event p0)
			{
				return x.isHit(p0 );
			}
			
			public iAcceptor<org.eclipse.swt.widgets.Event> set(org.eclipse.swt.widgets.Event p)
			{
				a.set(p);
				return this;
			}
			
			public Float f(org.eclipse.swt.widgets.Event p)
			{
				return (Float) f.f(p);
			}
			
		public iUpdateable updateable(final org.eclipse.swt.widgets.Event p0)
	{
		return new iUpdateable()
		{
			public void update()
			{
				isHit(p0);
			}
		};
	}
	public iProvider<Float> bind(final org.eclipse.swt.widgets.Event p0){
return new iProvider(){public Object get(){return isHit(p0);}};}};

		hit = new hit_interface()
		{
			
			iAcceptor a = hit_s.acceptor(x);
			iFunction f = hit_s.function(x);

			
			public field.core.windowing.components.iComponent hit (final org.eclipse.swt.widgets.Event p0)
			{
				return x.hit(p0 );
			}
			
			public iAcceptor<org.eclipse.swt.widgets.Event> set(org.eclipse.swt.widgets.Event p)
			{
				a.set(p);
				return this;
			}
			
			public field.core.windowing.components.iComponent f(org.eclipse.swt.widgets.Event p)
			{
				return (field.core.windowing.components.iComponent) f.f(p);
			}
			
		public iUpdateable updateable(final org.eclipse.swt.widgets.Event p0)
	{
		return new iUpdateable()
		{
			public void update()
			{
				hit(p0);
			}
		};
	}
	public iProvider<field.core.windowing.components.iComponent> bind(final org.eclipse.swt.widgets.Event p0){
return new iProvider(){public Object get(){return hit(p0);}};}};

getBounds = getBounds_s.bind(x);
		setBounds = new setBounds_interface()
		{
			
			iAcceptor a = setBounds_s.acceptor(x);
			iFunction f = setBounds_s.function(x);

			
			public void setBounds (final field.core.dispatch.iVisualElement.Rect p0)
			{
				 x.setBounds(p0 );
			}
			
			public iAcceptor<field.core.dispatch.iVisualElement.Rect> set(field.core.dispatch.iVisualElement.Rect p)
			{
				a.set(p);
				return this;
			}
			
			public Object f(field.core.dispatch.iVisualElement.Rect p)
			{
				return (Object) f.f(p);
			}
			
		public iUpdateable updateable(final field.core.dispatch.iVisualElement.Rect p0)
	{
		return new iUpdateable()
		{
			public void update()
			{
				setBounds(p0);
			}
		};
	}
		};

		keyTyped = new keyTyped_interface()
		{
			
			iAcceptor a = keyTyped_s.acceptor(x);
			iFunction f = keyTyped_s.function(x);

			
			public void keyTyped (final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
			{
				 x.keyTyped(p0, p1 );
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
			
		public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
	{
		return new iUpdateable()
		{
			public void update()
			{
				keyTyped(p0, p1);
			}
		};
	}
		};

		keyPressed = new keyPressed_interface()
		{
			
			iAcceptor a = keyPressed_s.acceptor(x);
			iFunction f = keyPressed_s.function(x);

			
			public void keyPressed (final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
			{
				 x.keyPressed(p0, p1 );
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
			
		public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
	{
		return new iUpdateable()
		{
			public void update()
			{
				keyPressed(p0, p1);
			}
		};
	}
		};

		keyReleased = new keyReleased_interface()
		{
			
			iAcceptor a = keyReleased_s.acceptor(x);
			iFunction f = keyReleased_s.function(x);

			
			public void keyReleased (final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
			{
				 x.keyReleased(p0, p1 );
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
			
		public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
	{
		return new iUpdateable()
		{
			public void update()
			{
				keyReleased(p0, p1);
			}
		};
	}
		};

		mouseClicked = new mouseClicked_interface()
		{
			
			iAcceptor a = mouseClicked_s.acceptor(x);
			iFunction f = mouseClicked_s.function(x);

			
			public void mouseClicked (final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
			{
				 x.mouseClicked(p0, p1 );
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
			
		public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
	{
		return new iUpdateable()
		{
			public void update()
			{
				mouseClicked(p0, p1);
			}
		};
	}
		};

		mousePressed = new mousePressed_interface()
		{
			
			iAcceptor a = mousePressed_s.acceptor(x);
			iFunction f = mousePressed_s.function(x);

			
			public void mousePressed (final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
			{
				 x.mousePressed(p0, p1 );
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
			
		public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
	{
		return new iUpdateable()
		{
			public void update()
			{
				mousePressed(p0, p1);
			}
		};
	}
		};

		mouseReleased = new mouseReleased_interface()
		{
			
			iAcceptor a = mouseReleased_s.acceptor(x);
			iFunction f = mouseReleased_s.function(x);

			
			public void mouseReleased (final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
			{
				 x.mouseReleased(p0, p1 );
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
			
		public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
	{
		return new iUpdateable()
		{
			public void update()
			{
				mouseReleased(p0, p1);
			}
		};
	}
		};

		mouseEntered = new mouseEntered_interface()
		{
			
			iAcceptor a = mouseEntered_s.acceptor(x);
			iFunction f = mouseEntered_s.function(x);

			
			public void mouseEntered (final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
			{
				 x.mouseEntered(p0, p1 );
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
			
		public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
	{
		return new iUpdateable()
		{
			public void update()
			{
				mouseEntered(p0, p1);
			}
		};
	}
		};

		mouseExited = new mouseExited_interface()
		{
			
			iAcceptor a = mouseExited_s.acceptor(x);
			iFunction f = mouseExited_s.function(x);

			
			public void mouseExited (final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
			{
				 x.mouseExited(p0, p1 );
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
			
		public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
	{
		return new iUpdateable()
		{
			public void update()
			{
				mouseExited(p0, p1);
			}
		};
	}
		};

		mouseDragged = new mouseDragged_interface()
		{
			
			iAcceptor a = mouseDragged_s.acceptor(x);
			iFunction f = mouseDragged_s.function(x);

			
			public void mouseDragged (final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
			{
				 x.mouseDragged(p0, p1 );
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
			
		public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
	{
		return new iUpdateable()
		{
			public void update()
			{
				mouseDragged(p0, p1);
			}
		};
	}
		};

		mouseMoved = new mouseMoved_interface()
		{
			
			iAcceptor a = mouseMoved_s.acceptor(x);
			iFunction f = mouseMoved_s.function(x);

			
			public void mouseMoved (final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
			{
				 x.mouseMoved(p0, p1 );
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
			
		public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final org.eclipse.swt.widgets.Event p1)
	{
		return new iUpdateable()
		{
			public void update()
			{
				mouseMoved(p0, p1);
			}
		};
	}
		};

		beginMouseFocus = new beginMouseFocus_interface()
		{
			
			iAcceptor a = beginMouseFocus_s.acceptor(x);
			iFunction f = beginMouseFocus_s.function(x);

			
			public void beginMouseFocus (final field.core.windowing.GLComponentWindow.ComponentContainer p0)
			{
				 x.beginMouseFocus(p0 );
			}
			
			public iAcceptor<field.core.windowing.GLComponentWindow.ComponentContainer> set(field.core.windowing.GLComponentWindow.ComponentContainer p)
			{
				a.set(p);
				return this;
			}
			
			public Object f(field.core.windowing.GLComponentWindow.ComponentContainer p)
			{
				return (Object) f.f(p);
			}
			
		public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0)
	{
		return new iUpdateable()
		{
			public void update()
			{
				beginMouseFocus(p0);
			}
		};
	}
		};

		endMouseFocus = new endMouseFocus_interface()
		{
			
			iAcceptor a = endMouseFocus_s.acceptor(x);
			iFunction f = endMouseFocus_s.function(x);

			
			public void endMouseFocus (final field.core.windowing.GLComponentWindow.ComponentContainer p0)
			{
				 x.endMouseFocus(p0 );
			}
			
			public iAcceptor<field.core.windowing.GLComponentWindow.ComponentContainer> set(field.core.windowing.GLComponentWindow.ComponentContainer p)
			{
				a.set(p);
				return this;
			}
			
			public Object f(field.core.windowing.GLComponentWindow.ComponentContainer p)
			{
				return (Object) f.f(p);
			}
			
		public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0)
	{
		return new iUpdateable()
		{
			public void update()
			{
				endMouseFocus(p0);
			}
		};
	}
		};

		paint = new paint_interface()
		{
			
			iAcceptor a = paint_s.acceptor(x);
			iFunction f = paint_s.function(x);

			
			public void paint (final field.core.windowing.GLComponentWindow.ComponentContainer p0, final field.math.linalg.iCoordinateFrame p1, final boolean p2)
			{
				 x.paint(p0, p1, p2 );
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
			
		public iUpdateable updateable(final field.core.windowing.GLComponentWindow.ComponentContainer p0, final field.math.linalg.iCoordinateFrame p1, final boolean p2)
	{
		return new iUpdateable()
		{
			public void update()
			{
				paint(p0, p1, p2);
			}
		};
	}
		};

		handleResize = new handleResize_interface()
		{
			
			iAcceptor a = handleResize_s.acceptor(x);
			iFunction f = handleResize_s.function(x);

			
			public void handleResize (final java.util.Set p0, final float p1, final float p2)
			{
				 x.handleResize(p0, p1, p2 );
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
			
		public iUpdateable updateable(final java.util.Set p0, final float p1, final float p2)
	{
		return new iUpdateable()
		{
			public void update()
			{
				handleResize(p0, p1, p2);
			}
		};
	}
		};

{final Mirroring.iBoundNoArgsMethod<field.core.dispatch.iVisualElement> bound = getVisualElement_s.bind(x);
		getVisualElement = (getVisualElement_interface) Proxy.newProxyInstance(x.getClass().getClassLoader(), new Class[] { getVisualElement_interface.class}, new InvocationHandler(){
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

				if (method.getName().equals("get") && args.length == 0) return bound.get();

				return method.invoke(bound.get(), args);
			}
		});}
		setVisualElement = new setVisualElement_interface()
		{
			
			iAcceptor a = setVisualElement_s.acceptor(x);
			iFunction f = setVisualElement_s.function(x);

			
			public field.core.windowing.components.iComponent setVisualElement (final field.core.dispatch.iVisualElement p0)
			{
				return x.setVisualElement(p0 );
			}
			
			public iAcceptor<field.core.dispatch.iVisualElement> set(field.core.dispatch.iVisualElement p)
			{
				a.set(p);
				return this;
			}
			
			public field.core.windowing.components.iComponent f(field.core.dispatch.iVisualElement p)
			{
				return (field.core.windowing.components.iComponent) f.f(p);
			}
			
		public iUpdateable updateable(final field.core.dispatch.iVisualElement p0)
	{
		return new iUpdateable()
		{
			public void update()
			{
				setVisualElement(p0);
			}
		};
	}
	public iProvider<field.core.windowing.components.iComponent> bind(final field.core.dispatch.iVisualElement p0){
return new iProvider(){public Object get(){return setVisualElement(p0);}};}};


}
}

