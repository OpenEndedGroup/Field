package field.graphics.qt;

import java.io.File;
import java.nio.ByteBuffer;

import field.graphics.ci.CoreImage;
import field.graphics.ci.CoreImageCanvasUtils.Image;
import field.graphics.core.BasicContextManager;

public class ByteImage {

	private String filename;
	private int width;
	private int height;
	private ByteBuffer byteBuffer;
	private long image;

	public ByteImage(String filename) {
		this.filename = filename;
	}
	
	public ByteImage(Image filename) {
		this.image = filename.getCoreimage();
		width = (int) new CoreImage().image_getExtentWidth(image);
		height = (int) new CoreImage().image_getExtentHeight(image);
		byteBuffer = ByteBuffer.allocateDirect(4 * width * height);

		long context = BasicContextManager.coreImageContext;
		;//System.out.println(" context is <"+context+">");
		if (context == 0)
			context = new CoreImage().context_createBitmapCIContextNow();
		new CoreImage().context_drawImageToByteBuffer(context, image, byteBuffer, width, height);

	}

	public ByteBuffer getByteImage() {
		if (byteBuffer != null)
			return (ByteBuffer) byteBuffer.duplicate().rewind();

		if (image == 0)
		{
			System.err.println(" about to load image <"+filename+">");
			if (filename.indexOf(":")==-1)
			{
				if (!new File(filename).exists())
				{
					System.err.println(" file <"+filename+"> does not exist");
					return null;
				}
				filename = "file://"+filename;
			}
			
			image = new CoreImage().image_createWithURL(filename);
			System.err.println(" loaded image <"+image+">");
			if (image == 0) return null;
		}

		width = (int) new CoreImage().image_getExtentWidth(image);
		height = (int) new CoreImage().image_getExtentHeight(image);
		
		;//System.out.println(" dimensions are <"+width+" -> "+height+">");
		
		byteBuffer = ByteBuffer.allocateDirect(4 * width * height);

		long context = BasicContextManager.coreImageContext;
		;//System.out.println(" context is <"+context+">");
		if (context == 0)
			context = new CoreImage().context_createBitmapCIContextNow();
		new CoreImage().context_drawImageToByteBuffer(context, image, byteBuffer, width, height);


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
	
	public int samplesPerPixel()
	{
		return 4;
	}
	
	public ByteBuffer getImage()
	{
		return getByteImage();
	}
}
