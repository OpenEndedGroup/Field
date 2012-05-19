package field.core.ui;
import java.lang.reflect.Method;
import field.namespace.generic.ReflectionTools;
import field.bytecode.apt.*;

import field.namespace.generic.Bind.*;

import field.namespace.generic.Bind.iFunction;

import java.lang.reflect.*;

import java.util.*;

import field.math.abstraction.*;

import field.launch.*;

import field.core.ui.PathNotInWorkspaceHelperMenu2;

import java.io.File;
import java.io.File;
public class PathNotInWorkspaceHelperMenu2_m {
static public final Method copy_m = ReflectionTools.methodOf("copy", field.core.ui.PathNotInWorkspaceHelperMenu2.class, java.io.File.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.ui.PathNotInWorkspaceHelperMenu2, java.io.File> copy_s = new Mirroring.MirrorNoReturnMethod<field.core.ui.PathNotInWorkspaceHelperMenu2, java.io.File>(field.core.ui.PathNotInWorkspaceHelperMenu2.class, "copy", new Class[]{java.io.File.class});

public interface copy_interface extends iAcceptor<java.io.File>, iFunction<Object ,java.io.File >
	{
		public void copy( final java.io.File p0);
	public iUpdateable updateable(final java.io.File p0);}

public final copy_interface copy;

static public final Method move_m = ReflectionTools.methodOf("move", field.core.ui.PathNotInWorkspaceHelperMenu2.class, java.io.File.class);
static public final Mirroring.MirrorNoReturnMethod<field.core.ui.PathNotInWorkspaceHelperMenu2, java.io.File> move_s = new Mirroring.MirrorNoReturnMethod<field.core.ui.PathNotInWorkspaceHelperMenu2, java.io.File>(field.core.ui.PathNotInWorkspaceHelperMenu2.class, "move", new Class[]{java.io.File.class});

public interface move_interface extends iAcceptor<java.io.File>, iFunction<Object ,java.io.File >
	{
		public void move( final java.io.File p0);
	public iUpdateable updateable(final java.io.File p0);}

public final move_interface move;

public PathNotInWorkspaceHelperMenu2_m(final PathNotInWorkspaceHelperMenu2 x) {
		copy = new copy_interface()
		{
			
			iAcceptor a = copy_s.acceptor(x);
			iFunction f = copy_s.function(x);

			
			public void copy (final java.io.File p0)
			{
				 x.copy(p0 );
			}
			
			public iAcceptor<java.io.File> set(java.io.File p)
			{
				a.set(p);
				return this;
			}
			
			public Object f(java.io.File p)
			{
				return (Object) f.f(p);
			}
			
		public iUpdateable updateable(final java.io.File p0)
	{
		return new iUpdateable()
		{
			public void update()
			{
				copy(p0);
			}
		};
	}
		};

		move = new move_interface()
		{
			
			iAcceptor a = move_s.acceptor(x);
			iFunction f = move_s.function(x);

			
			public void move (final java.io.File p0)
			{
				 x.move(p0 );
			}
			
			public iAcceptor<java.io.File> set(java.io.File p)
			{
				a.set(p);
				return this;
			}
			
			public Object f(java.io.File p)
			{
				return (Object) f.f(p);
			}
			
		public iUpdateable updateable(final java.io.File p0)
	{
		return new iUpdateable()
		{
			public void update()
			{
				move(p0);
			}
		};
	}
		};


}
}

