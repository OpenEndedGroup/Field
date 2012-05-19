package field.graphics.qt;

import java.nio.Buffer;

import field.graphics.core.Base;
import field.graphics.core.BasicContextManager;
import field.graphics.core.BasicUtilities;

/**
 * faster (apparently) if you are just going to put it on the screen
 */
public class QTKitVideo {
	static {
		/*
		 * this will fail on (64 bit) Java 6
		 */
		try {
			System.loadLibrary("FieldVideo_32");
		} catch (java.lang.UnsatisfiedLinkError link) {
			System.loadLibrary("FieldVideo");
		}
	}

	public long handle;

	public QTKitVideo() {
		handle = nativeHandle();
	}

//	public QTKitVideo openMovie(String path) {
//		openMovie(handle, path);
//		return this;
//	}

	public QTKitVideo setRate(float rate) {
		setRate(handle, rate);
		return this;
	}

	native void setRate(long handle, float rate);

	public float getDuration() {
		return getDuration(handle);
	}

	native float getDuration(long handle);

	public void setPosition(float position) {
		setPosition(handle, position);
	}

	public float getPosition() {
		return getPosition(handle);
	}

	native void setPosition(long handle, float position);
	native float getPosition(long handle);

	native private long nativeHandle();

	native public void openMovie(long handle, String path);

	native public int bind(long handle);

	native public int bindInto(long handle, Buffer memory, int width, int height);

	native public void unbind(long handle);

	native public void cleanUp(long handle);

	public class Element extends BasicUtilities.TwoPassElement {
		public Element(String name) {
			super(name, Base.StandardPass.preRender, Base.StandardPass.postRender);
		}

		@Override
		protected void setup() {
			BasicContextManager.putId(this, 0);
		}

		@Override
		protected void pre() {
			bind(handle);
		}

		@Override
		protected void post() {
			unbind(handle);
		}
	}

	public int getHeight() {
		return getHeight(handle);
	}

	native private int getHeight(long handle);

	public int getWidth() {
		return getWidth(handle);
	}

	native private int getWidth(long handle);

}
