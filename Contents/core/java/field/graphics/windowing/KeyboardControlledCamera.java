package field.graphics.windowing;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Iterator;
import java.util.LinkedHashSet;

import field.graphics.core.BasicCamera;
import field.launch.Launcher;

/**
 * @author marc <I>Created on Mar 7, 2003</I>
 */
public class KeyboardControlledCamera extends CoordinateFrameCamera implements KeyListener {

	class CharacterIsDown {
		char character;

		boolean isShift;
		int code;

		public CharacterIsDown(boolean b, int code) {
			isShift = b;
			this.code = code;
		}

		public CharacterIsDown(char c, boolean b, int code) {
			character = c;
			isShift = b;
			this.code = code;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof CharacterIsDown) {
				CharacterIsDown c = (CharacterIsDown) obj;
				return (c.character == character) && (c.isShift == isShift) && (c.code == code);
			}
			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return character + (isShift ? 100 : -100) + code;
		}
	}

	static float speed = 0.3f;

	public float movementScale = 1;

	public float rotationScale = 1;

	LinkedHashSet charactersDown = new LinkedHashSet();

	/**
	 * @param camera
	 */
	public KeyboardControlledCamera(BasicCamera camera) {
		super(camera);
		Launcher.getLauncher().registerUpdateable(this);
	}

	public void keyPressed(KeyEvent e) {
		CharacterIsDown d = null;
		if ((e.getModifiers() & KeyEvent.SHIFT_MASK) != 0) {
			d = new CharacterIsDown(e.getKeyChar(), true, e.getKeyCode());
		} else
			d = new CharacterIsDown(e.getKeyChar(), false, e.getKeyCode());

		if (e.getKeyCode() == KeyEvent.VK_ENTER)
		{
			charactersDown.clear();
		}
		
		if (!charactersDown.contains(d))
			charactersDown.add(d);
	}

	public void keyReleased(KeyEvent e) {
		CharacterIsDown d = null;
		if ((e.getModifiers() & KeyEvent.SHIFT_MASK) != 0) {
			d = new CharacterIsDown(e.getKeyChar(), true, e.getKeyCode());
		} else
			d = new CharacterIsDown(e.getKeyChar(), false, e.getKeyCode());

		// if (!charactersDown.contains(d))
		charactersDown.remove(d);
	}

	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void update() {
		// super.update();

		Iterator cc = charactersDown.iterator();
		while (cc.hasNext()) {
			CharacterIsDown c = (CharacterIsDown) cc.next();
			if (c.isShift) {
				switch (c.code) {
				case KeyEvent.VK_UP:
					this.moveForwardAlong(1 * speed * movementScale);
					break;
				case KeyEvent.VK_DOWN:
					this.moveForwardAlong(-1 * speed * movementScale);
					break;
				case KeyEvent.VK_LEFT:
					this.rotateLeftAround(0.1f * speed * rotationScale);
					break;
				case KeyEvent.VK_RIGHT:
					this.rotateLeftAround(-0.1f * speed * rotationScale);
					break;
				case KeyEvent.VK_PAGE_UP:
					this.rotateUpAround(0.1f * speed * rotationScale);
					break;
				case KeyEvent.VK_PAGE_DOWN:
					this.rotateUpAround(-0.1f * speed * rotationScale);
					break;
				}
			} else {
				switch (c.code) {
				case KeyEvent.VK_UP:
					this.moveForward(1 * speed * movementScale);
					break;
				case KeyEvent.VK_DOWN:
					this.moveForward(-1 * speed * movementScale);
					break;
				case KeyEvent.VK_LEFT:
					this.rotateLeft(0.1f * speed * rotationScale);
					break;
				case KeyEvent.VK_RIGHT:
					this.rotateLeft(-0.1f * speed * rotationScale);
					break;
				case KeyEvent.VK_PAGE_UP:
					this.rotateUp(0.1f * speed * rotationScale);
					break;
				case KeyEvent.VK_PAGE_DOWN:
					this.rotateUp(-0.1f * speed * rotationScale);
					break;

				case KeyEvent.VK_NUMPAD8:
					this.panUp(speed * movementScale);
					break;
				case KeyEvent.VK_NUMPAD2:
					this.panDown(speed * movementScale);
					break;
				case KeyEvent.VK_NUMPAD4:
					this.panLeft(speed * movementScale);
					break;
				case KeyEvent.VK_NUMPAD6:
					this.panRight(speed * movementScale);
					break;
					
				case KeyEvent.VK_NUMPAD7:
					this.rollLeft(0.1f* speed * rotationScale);
					break;
				case KeyEvent.VK_NUMPAD9:
					this.rollLeft(-0.1f* speed * rotationScale);
					break;
				}

			}
			super.update();
		}

	}

}
