package field.util;

import java.awt.Component;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.swing.JComponent;

import field.launch.Launcher;
import field.launch.iUpdateable;

public class MiscNative {

	static boolean nativeAvailable = true;

	static {
		MiscNative.load();
		// System.loadLibrary("miscNative2");
	}

	static public void goToBlack() {
		FloatBuffer wrOut = ByteBuffer.allocateDirect(256 * 4).asFloatBuffer();
		FloatBuffer wgOut = ByteBuffer.allocateDirect(256 * 4).asFloatBuffer();
		FloatBuffer wbOut = ByteBuffer.allocateDirect(256 * 4).asFloatBuffer();

		for (int i = 0; i < wrOut.capacity(); i++) {
			wrOut.put(0);
			wgOut.put(0);
			wbOut.put(0);
		}

		wrOut.rewind();
		wgOut.rewind();
		wbOut.rewind();

		new MiscNative().setScreenTransferTable(wrOut, wgOut, wbOut);
	}

	static public void fadeUp() {
		final FloatBuffer wrOut = ByteBuffer.allocateDirect(256 * 4).asFloatBuffer();
		final FloatBuffer wgOut = ByteBuffer.allocateDirect(256 * 4).asFloatBuffer();
		final FloatBuffer wbOut = ByteBuffer.allocateDirect(256 * 4).asFloatBuffer();

		Launcher.getLauncher().registerUpdateable(new iUpdateable() {

			int t = 0;
			float length = 100;

			public void update() {
				t++;

				for (int i = 0; i < wrOut.capacity(); i++) {
					wrOut.put(t / length);
					wgOut.put(t / length);
					wbOut.put(t / length);
				}

				wrOut.rewind();
				wgOut.rewind();
				wbOut.rewind();

				new MiscNative().setScreenTransferTable(wrOut, wgOut, wbOut);
				if (t >= length)
					Launcher.getLauncher().deregisterUpdateable(this);
			}
		});
	}

	static public void load() {
		try {
			;//System.out.println(" loading native (snow)");
			System.loadLibrary("miscNative2_snow");

		} catch (UnsatisfiedLinkError e) {
			try {
				;//System.out.println(" loading native (non-snow)");
				System.loadLibrary("miscNative2");
			} catch (UnsatisfiedLinkError ee) {
				nativeAvailable = false;
				;//System.out.println("\n\n -- no native lib available (tried snow leopard and leopard)-- " + System.getProperty("java.library.path") + "\n\n");
			}
		}
	}

	public native int connexionStart();

	public boolean disableScreenUpdates() {
		return true;
	}

	public boolean disableScreenUpdates(JComponent inside) {
		// Graphics g = inside.getGraphics();
		// if (g != null) {
		// final SurfaceData sd;
		// if (g instanceof sun.java2d.SunGraphics2D) {
		// sd = ((sun.java2d.SunGraphics2D) g).surfaceData;
		// } else
		// sd = null;
		//
		// if (sd != null) {
		// sd.disableFlushing();
		// Launcher.getLauncher().registerUpdateable(new iUpdateable() {
		//
		// public void update() {
		// sd.enableFlushing();
		// Launcher.getLauncher().deregisterUpdateable(this);
		// }
		// });
		// return true;
		// }
		// }
		return false;
	}

	public void doSplash() {
		//
		// new Thread(new Runnable() {
		// public void run() {
		// }
		// }).start();
		Launcher.getLauncher().registerUpdateable(new iUpdateable() {
			int t = 0;

			public void update() {
				t++;
				if (t == 1)
					new Thread(new Runnable() {
						public void run() {
							new MiscNative().splashDown_safe();
						}
					}).start();
			}
		});
	}

	public boolean enableScreenUpdates() {
		return false;
	}

	native public void performFloat(Buffer matrix, int rows, int cols, Buffer output);

	public native int fastRandom(FloatBuffer floats, int length);

	public native int fastRandomAdd(FloatBuffer floats, int length, float mul);

	public native int fastRandomAmp(FloatBuffer floats, int length, float mul, long seed);

	public boolean fixTransparentWindowBug_safe(Object frame, float opacity) {
		if (nativeAvailable)
			return fixTransparentWindowBug(frame, opacity);
		return false;
	}

	native public void makeScreenSaverLevel(Object frame);

	native public void makePopUpLevel(Object frame);

	public native boolean fixTransparentWindowBug(Object frame, float opacity);

	native public void goMultithreadedRenderer();

	public boolean makeChildWindowOf_safe(Object insideParent, Object insideChild) {
		if (nativeAvailable)
			return makeChildWindowOf(insideParent, insideChild);
		return false;
	}

	public native boolean makeChildWindowOf(Object insideParent, Object insideChild);

	public boolean makeReallyTransparent_safe(long nsContext) {
		if (nativeAvailable)
			return makeReallyTransparent(nsContext);
		return false;
	}

	public native boolean makeReallyTransparent(long nsContext);

	public native void nativeFourSSED(Buffer buffer);

	public native void nativeFourSSEDInit(int width, int height);

	public native int nativeRLEDecompress(ByteBuffer input, int length, ByteBuffer output);

	native public void setFrameVisibleSpecial(long nsContext, boolean b);

	public void setScreenTransferTable(FloatBuffer red, FloatBuffer green, FloatBuffer blue) {
		assert (red.capacity() == green.capacity());
		assert (green.capacity() == blue.capacity());

		setGammaRamp(red, green, blue, red.capacity());
	}

	public void splashDown_safe() {
		if (nativeAvailable)
			splashDown();
	}

	native public void splashDown();

	public void splashUp_safe() {
		if (nativeAvailable)
			splashUp();
	}

	native public void splashUp();

	public void noteDown_safe() {
		if (nativeAvailable)
			noteDown();
	}

	native public void noteDown();

	public void noteUp_safe(String text, int x, int y, Object windowParent) {
		if (nativeAvailable)
			noteUp(text, x, y, windowParent);
	}

	native public void noteUp(String text, int x, int y, Object windowParent);

	public boolean unmakeChildWindowOf_safe(Object insideParent, Object insideChild) {
		if (nativeAvailable)
			return unmakeChildWindowOf(insideParent, insideChild);

		return false;
	}

	native public boolean unmakeChildWindowOf(Object insideParent, Object insideChild);

	public void forceFullScreenNow_safe(long context) {
		if (nativeAvailable)
			forceFullScreenNow(context);
	}

	public native void forceFullScreenNow(long context);

	public native void vDivergence(Buffer input, int width, int height, Buffer tmp);

	protected native void setGammaRamp(FloatBuffer red, FloatBuffer green, FloatBuffer blue, int len);

	public void enterKiosk_safe() {
		if (nativeAvailable)
			enterKiosk();
	}

	public native void enterKiosk();

	public void exitKiosk_safe() {
		if (nativeAvailable)
			exitKiosk();
	}

	public void becomeKeyWindow_safe(Component c) {
		if (nativeAvailable)
			becomeKeyWindow(c);
	}

	public native void becomeKeyWindow(Component c);

	public native void exitKiosk();

	public String getDefaultsProperty_safe(String key) {
		if (nativeAvailable)
			return getDefaultsProperty(key);
		return null;
	}

	public native String getDefaultsProperty(String key);

	public String getPropertyKeys_safe() {
		if (nativeAvailable)
			return getPropertyKeys();
		return "";
	}

	public native String getPropertyKeys();

	public native long getCurrentContextShareGroup();

	public native void lookupAndBindIOSurfaceNow(int surface);

	public native void printBeamPositions();

	public native void forceStereoOnAllDisplays();

	public native long windowNumberFor(Component c);

	public native void forceVsync();

	public native void allViewsAcceptFirstMouse();
}
