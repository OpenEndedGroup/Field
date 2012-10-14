package field.graphics.qt;

import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import field.graphics.ci.CoreImage;
import field.graphics.core.BasicContextManager;

public class ByteImage {

	private String filename;
	private int width;
	private int height;
	private ByteBuffer byteBuffer;

	public ByteImage(String filename) {
		this.filename = filename;
	}

	public ByteBuffer getByteImage() {
		if (byteBuffer != null)
			return (ByteBuffer) byteBuffer.duplicate().rewind();

		if (filename.indexOf(":") == -1) {
			if (!new File(filename).exists()) {
				System.err.println(" file <" + filename + "> does not exist");
				return null;
			}
			filename = filename.replaceAll(" ", "%20");
			filename = "file://" + filename;
		}

		System.out.println(" reading :" + filename);
		try {
			BufferedImage read = ImageIO.read(new URL(filename));
			System.out.println(" read :" + filename);
			int w = read.getWidth();
			int h = read.getHeight();
			int[] pixels = new int[w * h];
			new PixelGrabber(read, 0, 0, w, h, pixels, 0, w).grabPixels();
			byteBuffer = ByteBuffer.allocateDirect(4 * pixels.length);
			IntBuffer a = byteBuffer.asIntBuffer();
			a.put(pixels);
			width = w;
			height = h;
			System.out.println(" dim :" + width + " " + height);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return byteBuffer;
	}

	public int getWidth() {
		getByteImage();
		return width;
	}

	public int getHeight() {
		getByteImage();
		return height;
	}

	public int pixelsWide() {
		return getWidth();
	}

	public int pixelsHigh() {
		return getHeight();
	}

	public int samplesPerPixel() {
		return 4;
	}

	public ByteBuffer getImage() {
		return getByteImage();
	}
}
