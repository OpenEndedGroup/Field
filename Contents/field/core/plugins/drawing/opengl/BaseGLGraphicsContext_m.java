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

import field.core.plugins.drawing.opengl.BaseGLGraphicsContext;

public class BaseGLGraphicsContext_m {
static public final Method windowDisplayEnter_m = ReflectionTools.methodOf("windowDisplayEnter", field.core.plugins.drawing.opengl.BaseGLGraphicsContext.class);
static public final Mirroring.MirrorNoReturnNoArgsMethod<field.core.plugins.drawing.opengl.BaseGLGraphicsContext> windowDisplayEnter_s = new Mirroring.MirrorNoReturnNoArgsMethod<field.core.plugins.drawing.opengl.BaseGLGraphicsContext>(field.core.plugins.drawing.opengl.BaseGLGraphicsContext.class, "windowDisplayEnter");

public final iUpdateable windowDisplayEnter;
static public final Method windowDisplayExit_m = ReflectionTools.methodOf("windowDisplayExit", field.core.plugins.drawing.opengl.BaseGLGraphicsContext.class);
static public final Mirroring.MirrorNoReturnNoArgsMethod<field.core.plugins.drawing.opengl.BaseGLGraphicsContext> windowDisplayExit_s = new Mirroring.MirrorNoReturnNoArgsMethod<field.core.plugins.drawing.opengl.BaseGLGraphicsContext>(field.core.plugins.drawing.opengl.BaseGLGraphicsContext.class, "windowDisplayExit");

public final iUpdateable windowDisplayExit;
public BaseGLGraphicsContext_m(final BaseGLGraphicsContext x) {
windowDisplayEnter = windowDisplayEnter_s.updateable(x);
windowDisplayExit = windowDisplayExit_s.updateable(x);

}
}

