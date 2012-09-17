package field.core.ui.text;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Event;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.launch.iUpdateable;

/**
 * per -visualelementroot shortcuts
 * 
 * @author marc
 * 
 */
@Woven
public class GlobalKeyboardShortcuts {

	static public VisualElementProperty<GlobalKeyboardShortcuts> shortcuts = new VisualElementProperty<GlobalKeyboardShortcuts>("keyboardShortcuts_");
	
	public class Shortcut {
		int keycode = -1;
		char character = '\0';
		int andMask = 0;
		int equals = 0;

		public Shortcut(int keycode, int andMask, int equals) {
			this.keycode = keycode;
			this.andMask = andMask;
			this.equals = equals;
		}

		public Shortcut(char c, int andMask, int equals) {
			this.character = c;
			this.andMask = andMask;
			this.equals = equals;

		}

		public boolean matches(char c, int code, int state) {
			return (((character != '\0' && c== character) || (keycode != -1 && code == keycode)) && ((state & andMask) == equals));
		}
	}

	HashMap<Shortcut, iUpdateable> cuts = new HashMap<Shortcut, iUpdateable>();

	public GlobalKeyboardShortcuts() {

	}
	
	public void add(Shortcut s, iUpdateable u)
	{
		cuts.put(s, u);
	}

	public boolean fire(Event e) {
		for (Map.Entry<Shortcut, iUpdateable> c : cuts.entrySet()) {
			if (c.getKey().matches(e.character, e.keyCode, e.stateMask)) {
				c.getValue().update();
				return true;
			}
		}
		return false;
	}

	@NextUpdate
	public boolean fire(KeyEvent e) {
		for (Map.Entry<Shortcut, iUpdateable> c : cuts.entrySet()) {
			if (c.getKey().matches(e.getKeyChar(), -1, (e.isShiftDown() ? SWT.SHIFT : 0) | (e.isMetaDown() ? SWT.COMMAND : 0))) {
				c.getValue().update();
				return true;
			}
		}
		return false;
	}

	public boolean fire(VerifyEvent e) {
		for (Map.Entry<Shortcut, iUpdateable> c : cuts.entrySet()) {
			if (c.getKey().matches(e.character, e.keyCode, e.stateMask)) {
				c.getValue().update();
				return true;
			}
		}
		return false;
	}

}
