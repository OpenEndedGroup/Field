package field.graphics.windowing;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.GestureEvent;

import field.graphics.core.BasicCamera;
import field.graphics.windowing.KeyboardControlledCamera_smooth.CharacterIsDown;
import field.launch.Launcher;
import field.math.BaseMath;
import field.math.BaseMath.MutableFloat;

/**
 * @author marc <I>Created on Mar 7, 2003</I>
 */
public class KeyboardControlledCamera_smooth extends CoordinateFrameCamera implements KeyListener, org.eclipse.swt.events.KeyListener, org.eclipse.swt.events.GestureListener {

	class CharacterIsDown {
		char character;

		boolean isShift;
		int code;

		boolean doFade = false;

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

		public CharacterIsDown setDoFade(boolean doFade) {
			this.doFade = doFade;
			return this;
		}
	}

	static float speed = 0.3f;

	public float movementScale = 1;

	public float rotationScale = 1;

	LinkedHashMap<CharacterIsDown, Float> charactersDown = new LinkedHashMap<CharacterIsDown, Float>();
	LinkedHashMap<CharacterIsDown, BaseMath.MutableFloat> charactersDownBuffer = new LinkedHashMap<CharacterIsDown, BaseMath.MutableFloat>();

	/**
	 * @param camera
	 */
	public KeyboardControlledCamera_smooth(BasicCamera camera) {
		super(camera);
		Launcher.getLauncher().registerUpdateable(this);
	}

	public void keyPressed(KeyEvent e) {

		if (e.getKeyCode() == KeyEvent.VK_CLOSE_BRACKET) {
			movementScale /= 2;
			rotationScale /= 2;
			return;
		}
		if (e.getKeyCode() == KeyEvent.VK_OPEN_BRACKET) {
			movementScale *= 2;
			rotationScale *= 2;
			return;
		}

		CharacterIsDown d = null;
		if ((e.getModifiers() & KeyEvent.SHIFT_MASK) != 0) {
			d = new CharacterIsDown(e.getKeyChar(), true, e.getKeyCode());
		} else
			d = new CharacterIsDown(e.getKeyChar(), false, e.getKeyCode());

		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			charactersDown.clear();
		}

		if (e.getKeyCode() == KeyEvent.VK_MULTIPLY) {
			FullScreenCanvasSWT.totalFlip = true;
		} else if (e.getKeyCode() == KeyEvent.VK_SUBTRACT) {
			FullScreenCanvasSWT.totalFlip = false;
			System.err.println(" not flipped!");
		}

		if (!charactersDown.containsKey(d))
			charactersDown.put(d, 1f);
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

		updateDownToCurrent();

		Iterator<Map.Entry<CharacterIsDown, BaseMath.MutableFloat>> cc = charactersDownBuffer.entrySet().iterator();
		while (cc.hasNext()) {
			Entry<CharacterIsDown, MutableFloat> e = cc.next();
			CharacterIsDown c = (CharacterIsDown) e.getKey();
			float f = e.getValue().floatValue();

			if (c.isShift) {
				switch (c.code) {
				case KeyEvent.VK_UP:
				case SWT.ARROW_UP:
					this.moveForwardAlong(1 * speed * movementScale * f);
					break;
				case KeyEvent.VK_DOWN:
				case SWT.ARROW_DOWN:
					this.moveForwardAlong(-1 * speed * movementScale * f);
					break;
				case KeyEvent.VK_LEFT:
				case SWT.ARROW_LEFT:
					this.rotateLeftAround(0.1f * speed * rotationScale * f);
					break;
				case KeyEvent.VK_RIGHT:
				case SWT.ARROW_RIGHT:
					this.rotateLeftAround(-0.1f * speed * rotationScale * f);
					break;
				case KeyEvent.VK_PAGE_UP:
				case SWT.PAGE_UP:
					this.rotateUpAround(0.1f * speed * rotationScale * f);
					break;
				case KeyEvent.VK_PAGE_DOWN:
				case SWT.PAGE_DOWN:
					this.rotateUpAround(-0.1f * speed * rotationScale * f);
					break;
				}
			} else {
				switch (c.code) {
				case KeyEvent.VK_UP:
				case SWT.ARROW_UP:
					this.moveForward(1 * speed * movementScale * f);
					break;
				case KeyEvent.VK_DOWN:
				case SWT.ARROW_DOWN:
					this.moveForward(-1 * speed * movementScale * f);
					break;
				case KeyEvent.VK_LEFT:
				case SWT.ARROW_LEFT:
					this.rotateLeft(0.1f * speed * rotationScale * f);
					break;
				case KeyEvent.VK_RIGHT:
				case SWT.ARROW_RIGHT:
					this.rotateLeft(-0.1f * speed * rotationScale * f);
					break;
				case KeyEvent.VK_PAGE_UP:
				case SWT.PAGE_UP:
					this.rotateUp(0.1f * speed * rotationScale * f);
					break;
				case KeyEvent.VK_PAGE_DOWN:
				case SWT.PAGE_DOWN:
					this.rotateUp(-0.1f * speed * rotationScale * f);
					break;

				case KeyEvent.VK_S:
				case (int) 's':
					this.panUp(speed * movementScale * f);
					break;
				case KeyEvent.VK_W:
				case (int) 'w':
					this.panDown(speed * movementScale * f);
					break;
				case KeyEvent.VK_A:
				case (int) 'a':
					this.panLeft(speed * movementScale * f);
					break;
				case KeyEvent.VK_D:
				case (int) 'd':
					this.panRight(speed * movementScale * f);
					break;

				case KeyEvent.VK_NUMPAD7:
				case SWT.KEYPAD_7:
					this.rollLeft(0.1f * speed * rotationScale * f);
					break;
				case KeyEvent.VK_NUMPAD9:
				case SWT.KEYPAD_9:
					this.rollLeft(-0.1f * speed * rotationScale * f);
					break;
				}

			}
			super.update();
		}

		Iterator<Entry<CharacterIsDown, Float>> ii = charactersDown.entrySet().iterator();
		while (ii.hasNext()) {
			Entry<CharacterIsDown, Float> n = ii.next();
			CharacterIsDown c = n.getKey();
			float f = n.getValue().floatValue();

			if (c.doFade && gestureIsDown) {
				f = f * 0.5f;
				if (f < 1e-3) {
					ii.remove();
					charactersDownBuffer.remove(n.getKey());
				}

				n.setValue(f);
				MutableFloat x = charactersDownBuffer.get(n.getKey());
				float alpha = 0.5f;
				if (x == null)
					charactersDownBuffer.put(n.getKey(), new MutableFloat(f * (1 - alpha)));
				else
					charactersDownBuffer.put(n.getKey(), new MutableFloat(x.floatValue() * alpha + (1 - alpha) * f));
			} else if (c.doFade) {
				f = f * 0.4f;
				if (f < 1e-3) {
					ii.remove();
					// charactersDownBuffer.remove(n.getKey());
				}

				n.setValue(f);
			}
		}

	}

	private void updateDownToCurrent() {
		HashSet<CharacterIsDown> seen = new LinkedHashSet<CharacterIsDown>();
		for (Map.Entry<CharacterIsDown, Float> s : charactersDown.entrySet()) {
			seen.add(s.getKey());
			moveTowards(s.getKey(), s.getValue());
		}

		for (Map.Entry<CharacterIsDown, BaseMath.MutableFloat> s : new ArrayList<Map.Entry>(charactersDownBuffer.entrySet())) {
			if (!seen.contains(s.getKey())) {
				moveTowards(s.getKey(), 0f);
			}
		}
	}

	float smoothing = 0.96f;
	float smoothing_up = 0.96f;

	private FullScreenCanvasSWT canvas;

	private void moveTowards(CharacterIsDown key, float f) {
		MutableFloat m = charactersDownBuffer.get(key);
		if (m == null)
			charactersDownBuffer.put(key, m = new MutableFloat(0));
		if (f > m.d)
			m.d = m.d * smoothing_up + (1 - smoothing_up) * f;
		else
			m.d = m.d * smoothing + (1 - smoothing) * f;
		if (m.d < 1e-4 && m.d > f)
			charactersDownBuffer.remove(key);
	}

	public void setCanvas(FullScreenCanvasSWT c) {
		this.canvas = c;
	}

	@Override
	public void keyPressed(org.eclipse.swt.events.KeyEvent e) {

		// System.out.println(" key pressed in keyboard controlled camera smooth <"+e+">");

		if (e.character == ']') {
			movementScale /= 2;
			rotationScale /= 2;
			return;
		}
		if (e.character == '[') {
			movementScale *= 2;
			rotationScale *= 2;
			return;
		}

		CharacterIsDown d = null;
		if ((e.stateMask & SWT.SHIFT) != 0) {
			d = new CharacterIsDown((char) e.keyCode, true, (int) e.keyCode);
		} else
			d = new CharacterIsDown((char) e.keyCode, false, (int) e.keyCode);

		if (e.keyCode == SWT.KEYPAD_CR) {
			charactersDown.clear();
		}

		if (e.keyCode == SWT.KEYPAD_MULTIPLY) {
			if (this.canvas == null) {
				FullScreenCanvasSWT.totalFlip = true;
			} else {
				canvas.totalFlip_instance = true;
			}
		} else if (e.keyCode == SWT.KEYPAD_SUBTRACT) {
			if (this.canvas == null) {
				FullScreenCanvasSWT.totalFlip = false;
			} else {
				canvas.totalFlip_instance = false;
			}
			System.err.println(" not flipped!");
		}

		if (!charactersDown.containsKey(d))
			charactersDown.put(d, 1f);
	}

	@Override
	public void keyReleased(org.eclipse.swt.events.KeyEvent e) {
		CharacterIsDown d = null;
		if ((e.stateMask & SWT.SHIFT) != 0) {
			d = new CharacterIsDown((char) e.keyCode, true, e.keyCode);
		} else
			d = new CharacterIsDown((char) e.keyCode, false, e.keyCode);

		charactersDown.remove(d);
	}

	float lastMag = 0;
	boolean gestureIsDown = false;

	@Override
	public void gesture(GestureEvent e) {
		if (e.detail == SWT.GESTURE_BEGIN) {
			lastMag = 1.0f;
			gestureIsDown = true;
		}

		System.out.println(e);

		if (e.detail == SWT.GESTURE_ROTATE) {

			if (e.rotation > 0) {
				CharacterIsDown d = new CharacterIsDown((char) SWT.KEYPAD_7, true, (int) SWT.KEYPAD_7).setDoFade(true);
				charactersDown.put(d, (float) e.rotation);
				// charactersDownBuffer.put(d, new
				// MutableFloat(e.xDirection));
				CharacterIsDown d2 = new CharacterIsDown((char) SWT.KEYPAD_9, true, (int) SWT.KEYPAD_9).setDoFade(true);
				charactersDown.remove(d2);
				charactersDownBuffer.remove(d2);
			}
			if (e.rotation< 0) {
				CharacterIsDown d = new CharacterIsDown((char) SWT.KEYPAD_9, true, (int) SWT.KEYPAD_9).setDoFade(true);
				charactersDown.put(d, (float) Math.abs(e.rotation));
				// charactersDownBuffer.put(d, new
				// MutableFloat(Math.abs(e.xDirection)));
				CharacterIsDown d2 = new CharacterIsDown((char) SWT.KEYPAD_7, true, (int) SWT.KEYPAD_7).setDoFade(true);
				charactersDown.remove(d2);
				charactersDownBuffer.remove(d2);
			}
		}

		if (e.detail == SWT.GESTURE_PAN) {

			if (e.xDirection > 0) {
				CharacterIsDown d = new CharacterIsDown((char) SWT.ARROW_RIGHT, true, (int) SWT.ARROW_RIGHT).setDoFade(true);
				charactersDown.put(d, (float) e.xDirection);
				// charactersDownBuffer.put(d, new
				// MutableFloat(e.xDirection));
				CharacterIsDown d2 = new CharacterIsDown((char) SWT.ARROW_LEFT, true, (int) SWT.ARROW_LEFT).setDoFade(true);
				charactersDown.remove(d2);
				charactersDownBuffer.remove(d2);
			}
			if (e.xDirection < 0) {
				CharacterIsDown d = new CharacterIsDown((char) SWT.ARROW_LEFT, true, (int) SWT.ARROW_LEFT).setDoFade(true);
				charactersDown.put(d, (float) Math.abs(e.xDirection));
				// charactersDownBuffer.put(d, new
				// MutableFloat(Math.abs(e.xDirection)));
				CharacterIsDown d2 = new CharacterIsDown((char) SWT.ARROW_RIGHT, true, (int) SWT.ARROW_RIGHT).setDoFade(true);
				charactersDown.remove(d2);
				charactersDownBuffer.remove(d2);
			}
			if (e.yDirection > 0) {
				CharacterIsDown d = new CharacterIsDown((char) SWT.PAGE_UP, true, (int) SWT.PAGE_UP).setDoFade(true);
				charactersDown.put(d, (float) e.yDirection);
				// charactersDownBuffer.put(d, new
				// MutableFloat(Math.abs(e.yDirection)));
				CharacterIsDown d2 = new CharacterIsDown((char) SWT.PAGE_DOWN, true, (int) SWT.PAGE_DOWN).setDoFade(true);
				charactersDown.remove(d2);
				charactersDownBuffer.remove(d2);
			}
			if (e.yDirection < 0) {
				CharacterIsDown d = new CharacterIsDown((char) SWT.PAGE_DOWN, true, (int) SWT.PAGE_DOWN).setDoFade(true);
				charactersDown.put(d, (float) Math.abs(e.yDirection));
				// charactersDownBuffer.put(d, new
				// MutableFloat(Math.abs(e.yDirection)));
				CharacterIsDown d2 = new CharacterIsDown((char) SWT.PAGE_UP, true, (int) SWT.PAGE_UP).setDoFade(true);
				charactersDown.remove(d2);
				charactersDownBuffer.remove(d2);
			}
		} else if (e.detail == SWT.GESTURE_MAGNIFY) {
			if (e.magnification > lastMag) {
				CharacterIsDown d = new CharacterIsDown((char) SWT.ARROW_UP, true, (int) SWT.ARROW_UP).setDoFade(true);

				float q = (float) (e.magnification - lastMag) / lastMag;

				charactersDown.put(d, (float) Math.min(1, q * 20));

				CharacterIsDown d2 = new CharacterIsDown((char) SWT.ARROW_DOWN, true, (int) SWT.ARROW_DOWN).setDoFade(true);
				charactersDown.remove(d2);
				charactersDownBuffer.remove(d2);

				// charactersDownBuffer.put(d, new
				// MutableFloat(e.xDirection));
			}

			else if (e.magnification < lastMag && e.magnification > 0.0) {
				CharacterIsDown d = new CharacterIsDown((char) SWT.ARROW_DOWN, true, (int) SWT.ARROW_DOWN).setDoFade(true);
				float q = (float) (lastMag - e.magnification) / lastMag;

				charactersDown.put(d, (float) Math.min(1, q * 20));
				// charactersDownBuffer.put(d, new
				// MutableFloat(e.xDirection));

				CharacterIsDown d2 = new CharacterIsDown((char) SWT.ARROW_UP, true, (int) SWT.ARROW_UP).setDoFade(true);
				charactersDown.remove(d2);
				charactersDownBuffer.remove(d2);

			}
			if (e.magnification > 0 && e.magnification < 3)
				lastMag = (float) e.magnification;
		}

		if (e.detail == SWT.GESTURE_END) {
			gestureIsDown = false;
		}
	}

}
