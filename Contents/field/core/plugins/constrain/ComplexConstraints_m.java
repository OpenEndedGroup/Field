package field.core.plugins.constrain;
import java.lang.reflect.Method;
import field.namespace.generic.ReflectionTools;
import field.bytecode.apt.*;

import field.namespace.generic.Bind.*;

import field.namespace.generic.Bind.iFunction;

import java.lang.reflect.*;

import java.util.*;

import field.math.abstraction.*;

import field.launch.*;

import field.core.plugins.constrain.ComplexConstraints;

import field.core.plugins.constrain.ComplexConstraints.VariablesForRect;
import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.constrain.ComplexConstraints.VariablesForRect;
import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.constrain.ComplexConstraints.VariablesForRect;
import field.core.plugins.constrain.ComplexConstraints.VariablesForRect;
import field.core.dispatch.iVisualElement.Rect;
public class ComplexConstraints_m {
static public final Method addEditFor_m = ReflectionTools.methodOf("addEditFor", field.core.plugins.constrain.ComplexConstraints.class, field.core.plugins.constrain.ComplexConstraints.VariablesForRect.class, field.core.dispatch.iVisualElement.Rect.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.plugins.constrain.ComplexConstraints, Object[]> addEditFor_s = new Mirroring.MirrorNoReturnMethod<field.core.plugins.constrain.ComplexConstraints, Object[]>(field.core.plugins.constrain.ComplexConstraints.class, "addEditFor", new Class[]{field.core.plugins.constrain.ComplexConstraints.VariablesForRect.class, field.core.dispatch.iVisualElement.Rect.class});

public interface addEditFor_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void addEditFor( final field.core.plugins.constrain.ComplexConstraints.VariablesForRect p0, final field.core.dispatch.iVisualElement.Rect p1);
	public iUpdateable updateable(final field.core.plugins.constrain.ComplexConstraints.VariablesForRect p0, final field.core.dispatch.iVisualElement.Rect p1);}

public final addEditFor_interface addEditFor;

static public final Method addSuggestionFor_m = ReflectionTools.methodOf("addSuggestionFor", field.core.plugins.constrain.ComplexConstraints.class, field.core.plugins.constrain.ComplexConstraints.VariablesForRect.class, field.core.dispatch.iVisualElement.Rect.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.plugins.constrain.ComplexConstraints, Object[]> addSuggestionFor_s = new Mirroring.MirrorNoReturnMethod<field.core.plugins.constrain.ComplexConstraints, Object[]>(field.core.plugins.constrain.ComplexConstraints.class, "addSuggestionFor", new Class[]{field.core.plugins.constrain.ComplexConstraints.VariablesForRect.class, field.core.dispatch.iVisualElement.Rect.class});

public interface addSuggestionFor_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void addSuggestionFor( final field.core.plugins.constrain.ComplexConstraints.VariablesForRect p0, final field.core.dispatch.iVisualElement.Rect p1);
	public iUpdateable updateable(final field.core.plugins.constrain.ComplexConstraints.VariablesForRect p0, final field.core.dispatch.iVisualElement.Rect p1);}

public final addSuggestionFor_interface addSuggestionFor;

static public final Method updateFrameFromVariables_m = ReflectionTools.methodOf("updateFrameFromVariables", field.core.plugins.constrain.ComplexConstraints.class, field.core.plugins.constrain.ComplexConstraints.VariablesForRect.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.plugins.constrain.ComplexConstraints, field.core.plugins.constrain.ComplexConstraints.VariablesForRect> updateFrameFromVariables_s = new Mirroring.MirrorNoReturnMethod<field.core.plugins.constrain.ComplexConstraints, field.core.plugins.constrain.ComplexConstraints.VariablesForRect>(field.core.plugins.constrain.ComplexConstraints.class, "updateFrameFromVariables", new Class[]{field.core.plugins.constrain.ComplexConstraints.VariablesForRect.class});

public interface updateFrameFromVariables_interface extends iAcceptor<field.core.plugins.constrain.ComplexConstraints.VariablesForRect>, iFunction<Object ,field.core.plugins.constrain.ComplexConstraints.VariablesForRect >
	{
		public void updateFrameFromVariables( final field.core.plugins.constrain.ComplexConstraints.VariablesForRect p0);
	public iUpdateable updateable(final field.core.plugins.constrain.ComplexConstraints.VariablesForRect p0);}

public final updateFrameFromVariables_interface updateFrameFromVariables;

static public final Method updateVariablesFromFrame_m = ReflectionTools.methodOf("updateVariablesFromFrame", field.core.plugins.constrain.ComplexConstraints.class, field.core.plugins.constrain.ComplexConstraints.VariablesForRect.class, field.core.dispatch.iVisualElement.Rect.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.plugins.constrain.ComplexConstraints, Object[]> updateVariablesFromFrame_s = new Mirroring.MirrorNoReturnMethod<field.core.plugins.constrain.ComplexConstraints, Object[]>(field.core.plugins.constrain.ComplexConstraints.class, "updateVariablesFromFrame", new Class[]{field.core.plugins.constrain.ComplexConstraints.VariablesForRect.class, field.core.dispatch.iVisualElement.Rect.class});

public interface updateVariablesFromFrame_interface extends iAcceptor<Object[]>, iFunction<Object ,Object[] >
	{
		public void updateVariablesFromFrame( final field.core.plugins.constrain.ComplexConstraints.VariablesForRect p0, final field.core.dispatch.iVisualElement.Rect p1);
	public iUpdateable updateable(final field.core.plugins.constrain.ComplexConstraints.VariablesForRect p0, final field.core.dispatch.iVisualElement.Rect p1);}

public final updateVariablesFromFrame_interface updateVariablesFromFrame;

public ComplexConstraints_m(final ComplexConstraints x) {
		addEditFor = new addEditFor_interface()
		{
			
			iAcceptor a = addEditFor_s.acceptor(x);
			iFunction f = addEditFor_s.function(x);

			
			public void addEditFor (final field.core.plugins.constrain.ComplexConstraints.VariablesForRect p0, final field.core.dispatch.iVisualElement.Rect p1)
			{
				 x.addEditFor(p0, p1 );
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
			
		public iUpdateable updateable(final field.core.plugins.constrain.ComplexConstraints.VariablesForRect p0, final field.core.dispatch.iVisualElement.Rect p1)
	{
		return new iUpdateable()
		{
			public void update()
			{
				addEditFor(p0, p1);
			}
		};
	}
		};

		addSuggestionFor = new addSuggestionFor_interface()
		{
			
			iAcceptor a = addSuggestionFor_s.acceptor(x);
			iFunction f = addSuggestionFor_s.function(x);

			
			public void addSuggestionFor (final field.core.plugins.constrain.ComplexConstraints.VariablesForRect p0, final field.core.dispatch.iVisualElement.Rect p1)
			{
				 x.addSuggestionFor(p0, p1 );
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
			
		public iUpdateable updateable(final field.core.plugins.constrain.ComplexConstraints.VariablesForRect p0, final field.core.dispatch.iVisualElement.Rect p1)
	{
		return new iUpdateable()
		{
			public void update()
			{
				addSuggestionFor(p0, p1);
			}
		};
	}
		};

		updateFrameFromVariables = new updateFrameFromVariables_interface()
		{
			
			iAcceptor a = updateFrameFromVariables_s.acceptor(x);
			iFunction f = updateFrameFromVariables_s.function(x);

			
			public void updateFrameFromVariables (final field.core.plugins.constrain.ComplexConstraints.VariablesForRect p0)
			{
				 x.updateFrameFromVariables(p0 );
			}
			
			public iAcceptor<field.core.plugins.constrain.ComplexConstraints.VariablesForRect> set(field.core.plugins.constrain.ComplexConstraints.VariablesForRect p)
			{
				a.set(p);
				return this;
			}
			
			public Object f(field.core.plugins.constrain.ComplexConstraints.VariablesForRect p)
			{
				return (Object) f.f(p);
			}
			
		public iUpdateable updateable(final field.core.plugins.constrain.ComplexConstraints.VariablesForRect p0)
	{
		return new iUpdateable()
		{
			public void update()
			{
				updateFrameFromVariables(p0);
			}
		};
	}
		};

		updateVariablesFromFrame = new updateVariablesFromFrame_interface()
		{
			
			iAcceptor a = updateVariablesFromFrame_s.acceptor(x);
			iFunction f = updateVariablesFromFrame_s.function(x);

			
			public void updateVariablesFromFrame (final field.core.plugins.constrain.ComplexConstraints.VariablesForRect p0, final field.core.dispatch.iVisualElement.Rect p1)
			{
				 x.updateVariablesFromFrame(p0, p1 );
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
			
		public iUpdateable updateable(final field.core.plugins.constrain.ComplexConstraints.VariablesForRect p0, final field.core.dispatch.iVisualElement.Rect p1)
	{
		return new iUpdateable()
		{
			public void update()
			{
				updateVariablesFromFrame(p0, p1);
			}
		};
	}
		};


}
}

