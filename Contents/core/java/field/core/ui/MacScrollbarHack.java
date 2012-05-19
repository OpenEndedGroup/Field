package field.core.ui;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.internal.cocoa.NSView;
import org.eclipse.swt.internal.cocoa.NSWindow;
import org.eclipse.swt.internal.cocoa.OS;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import field.core.Platform;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.namespace.generic.ReflectionTools;

public class MacScrollbarHack {

	public MacScrollbarHack(Control hack) {

		if (Platform.getOS() != Platform.OS.mac)
			return;
		Field f = ReflectionTools.getFirstFIeldCalled(hack.getClass(), "scrollView");
		try {
			Object h = f.get(hack);

			ReflectionTools.findFirstMethodCalled(h.getClass(), "setAutohidesScrollers").invoke(h, false);

			
			Object sv = ReflectionTools.findFirstMethodCalled(h.getClass(), "subviews").invoke(h);
			for (int i = 0; i < 3; i++) {
				try {
					Object s = ReflectionTools.findFirstMethodCalled(sv.getClass(), "objectAtIndex").invoke(sv, i);
					long id = ((Number) ReflectionTools.getFirstFIeldCalled(s.getClass(), "id").get(s)).longValue();
					OS.objc_msgSend(id, OS.sel_setControlSize_, 1);
				} catch (Throwable t) {
					// t.printStackTrace();
				}
			}
		} catch (Throwable t) {
			// t.printStackTrace();
		}

		hack.getParent().layout();

		Object w = hack.getShell().view.window();
		try {
			Method m = w.getClass().getDeclaredMethod("setCollectionBehavior");
			m.invoke(w, (int) (1 << 7));
		} catch (Throwable t) {
			// t.printStackTrace();
		}
	}

	public MacScrollbarHack(Browser id) {
		if (Platform.getOS() != Platform.OS.mac)
			return;

		try {

			NSView view = id.view;

			long a = OS.objc_msgSend(view.id, OS.sel_subviews);
			long c = OS.objc_msgSend(a, OS.sel_count);
			long b = OS.objc_msgSend(a, OS.sel_objectAtIndex_, 0);

			long x = OS.objc_msgSend(b, OS.sel_subviews);
			c = OS.objc_msgSend(x, OS.sel_count);
			long x2 = OS.objc_msgSend(x, OS.sel_objectAtIndex_, 0);

			long x3 = OS.objc_msgSend(x2, OS.sel_subviews);
			c = OS.objc_msgSend(x3, OS.sel_count);
			long x4 = OS.objc_msgSend(x3, OS.sel_objectAtIndex_, 0);

			long x5 = OS.objc_msgSend(x4, OS.sel_subviews);
			c = OS.objc_msgSend(x5, OS.sel_count);

			for (int i = 0; i < c; i++) {
				OS.objc_msgSend(OS.objc_msgSend(x5, OS.sel_objectAtIndex_, i), OS.sel_setControlSize_, 1);
			}
		} catch (Throwable t) {

		}
	}

	static public void skipAFrame(Shell s) {
		if (Platform.getOS() != Platform.OS.mac)
			return;
		try {
			final Object window = ReflectionTools.getFirstFIeldCalled(Shell.class, "window").get(s);

			final Method[] disable = ReflectionTools.findAllMethodsCalled(window.getClass(), "disableFlushWindow");
			final Method[] enable = ReflectionTools.findAllMethodsCalled(window.getClass(), "enableFlushWindow");

			disable[0].invoke(window);
			Launcher.getLauncher().registerUpdateable(new iUpdateable() {

				@Override
				public void update() {
					try {
						enable[0].invoke(window);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
					Launcher.getLauncher().deregisterUpdateable(this);
				}
			});

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
