package field.graphics.qt;

import java.nio.ByteBuffer;

import field.util.MiscNative;


/**
 * deprecated. Doesn't work in 1.6 (no 64-bit quicktime). Use JavaImage instead.
 * @author marc
 *
 */
public class QTImage {
	static {
		MiscNative.load();
	}

	int width;

	int height;

	int depth;

	boolean hasAlpha;

	ByteBuffer image;

	int dvHandle;

	public QTImage() {
	}

	public QTImage becomeDV(int w, int h) {
		dvHandle = DVIn(w, h);
		return this;
	}

	public int getDVFrame() {
		return dvHandle == 0 ? 0 : getDVFrame(dvHandle);
	}

	public ByteBuffer getImage() {
		image.rewind();
		return image;
	}

	public void info() {
	}

	public native void loadTexture(String arg);

	public native void loadTextureAndScale(String arg, int width, int height, int bitsPerPixel);

	public int pixelsHigh() {
		return height;
	}

	public int pixelsWide() {
		return width;
	}

	public int samplesPerPixel() {
		return depth / 8;
	}

	public void updateDV() {
		DVUpdate(dvHandle);
	}

	protected native int DVIn(int x, int y);

	protected native void DVUpdate(int handle);

	protected native int getDVFrame(int handle);

}
