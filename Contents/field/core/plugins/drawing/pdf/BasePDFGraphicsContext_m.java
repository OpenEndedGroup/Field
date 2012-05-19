package field.core.plugins.drawing.pdf;
import java.lang.reflect.Method;
import field.namespace.generic.ReflectionTools;
import field.bytecode.apt.*;

import field.namespace.generic.Bind.*;

import field.namespace.generic.Bind.iFunction;

import java.lang.reflect.*;

import java.util.*;

import field.math.abstraction.*;

import field.launch.*;

import field.core.plugins.drawing.pdf.BasePDFGraphicsContext;

public class BasePDFGraphicsContext_m {
static public final Method windowDisplayEnter_m = ReflectionTools.methodOf("windowDisplayEnter", field.core.plugins.drawing.pdf.BasePDFGraphicsContext.class);
static public final Mirroring.MirrorNoReturnNoArgsMethod<field.core.plugins.drawing.pdf.BasePDFGraphicsContext> windowDisplayEnter_s = new Mirroring.MirrorNoReturnNoArgsMethod<field.core.plugins.drawing.pdf.BasePDFGraphicsContext>(field.core.plugins.drawing.pdf.BasePDFGraphicsContext.class, "windowDisplayEnter");

public final iUpdateable windowDisplayEnter;
static public final Method windowDisplayExit_m = ReflectionTools.methodOf("windowDisplayExit", field.core.plugins.drawing.pdf.BasePDFGraphicsContext.class);
static public final Mirroring.MirrorNoReturnNoArgsMethod<field.core.plugins.drawing.pdf.BasePDFGraphicsContext> windowDisplayExit_s = new Mirroring.MirrorNoReturnNoArgsMethod<field.core.plugins.drawing.pdf.BasePDFGraphicsContext>(field.core.plugins.drawing.pdf.BasePDFGraphicsContext.class, "windowDisplayExit");

public final iUpdateable windowDisplayExit;
public BasePDFGraphicsContext_m(final BasePDFGraphicsContext x) {
windowDisplayEnter = windowDisplayEnter_s.updateable(x);
windowDisplayExit = windowDisplayExit_s.updateable(x);

}
}

