package field.core;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

public class Platform {

	public enum OS {
		mac, windows, linux;
	}

	static final OS os = System.getProperty("os.name").toLowerCase().contains("mac os x") | System.getProperty("os.name").toLowerCase().contains("darwin") ? OS.mac : OS.linux;
	static
	{
		System.err.println(" platform is :"+os+" / "+System.getProperty("os.name")+"  "+isMac());
	}
	static public final int META_MASK = (os == OS.mac ? InputEvent.META_MASK
			: InputEvent.CTRL_MASK);

	static public boolean isPopupTrigger(Event e) {

		return e.button == 3;

	}

	static public OS getOS() {
		return os;
	}

	static public boolean shouldUseQuaqua() {
		return getOS() == OS.mac;
	}

	static public Point getLocationOnScreen(MouseEvent e) {
		Point at = e.getPoint();
		try {
			Component c = e.getComponent();
			Point ll = c.getLocationOnScreen();
			return new Point(at.x + ll.x, at.y + ll.y);
		} catch (Exception ee) {
			return at;
		}
	}

	static public int getCommandModifier() {
		if (os == OS.mac)
			return SWT.COMMAND;
		else
			return SWT.CTRL;
	}

	public static boolean isPopupTrigger(MouseEvent e) {
		return e.getButton() == e.BUTTON3 && e.getID() == MouseEvent.MOUSE_PRESSED;
	}

	public static boolean isPopupTrigger(org.eclipse.swt.events.MouseEvent e) {
		return e.button == 3;
	}

	public static boolean isMac() {
		return Platform.getOS() == OS.mac;
	}

	public static boolean isLinux() {
		return Platform.getOS() == OS.linux;
	}

	public static <T> String getCanonicalName(Class<T> class1) {
		try {
			return class1.getCanonicalName();
		} catch (InternalError r) {
			return ""+class1;
		}
	}

	public static boolean is17() {
		
		System.out.println(" java version is <"+System.getProperty("java.version")+">");
		
		return System.getProperty("java.version").contains("1.7");
	}
	static public boolean willBe17 = is17();

}
