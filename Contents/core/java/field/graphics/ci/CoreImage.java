package field.graphics.ci;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * low-level natives for CoreImage->Java Support
 * 
 * @author marc
 *
 */
public class CoreImage {
	static
	{
		try{
			System.loadLibrary("FieldCoreImage");
		}
		catch(Throwable t)
		{
			System.err.println(" ** no core image **");
		}
	}

	// uses the current context
	native public long context_createOpenGLCIContextNow();

	native public long context_createBitmapCIContextNow();


	// draws an image into a context
	native public void context_drawImageNow(long context,long image, float x, float y, float dw, float dh, float sx, float sy, float srcw, float srch);

	// draws an image into a float buffer (can do this any time, or only inside gl draw?)
	native public void context_drawImageToFloatBuffer(long context,long image, FloatBuffer buffer, int width, int height);
	native public void context_drawImageToByteBuffer(long context,long image, ByteBuffer buffer, int width, int height);

	native public long filter_createGenericWithText(String name, String args);

	// creates a filter from a "name"
	native public long filter_createWithName(String name);

	native public long filter_getImage(long filter, String key);

	native public String filter_getInputKeys(long filter);

	native public String filter_getOutputKeys(long filter);


	native public long filter_release(long filter);
	native public void filter_setDefaults(long filter);
	native public void filter_setValueFloat(long filter, String key, float value);
	native public void filter_setValueImage(long filter, String key, long image);

	native public void filter_setValueVector2(long filter, String key, float x, float y);
	native public void filter_setValueVector4(long filter, String key, float x, float y, float w, float h);

	// creates an image from a raw buffer
	native public long image_createWithARGBData(ByteBuffer bytes, int width, int height);

	// creates an image from a raw buffer
	native public long image_createWithARGBFData(Buffer bytes, int width, int height);

	// creates an image from an opengl texture
	native public long image_createWithTexture(int unit, int width, int height, boolean flipped);
	// creates an image from disk
	native public long image_createWithURL(String url);
	native public float image_getExtentHeight(long image);
	native public float image_getExtentWidth(long image);

	native public float image_getExtentX(long image);

	native public float image_getExtentY(long image);

	native public long image_release(long image);
	native public long image_retain(long image);

	native public long image_save(long context, long image, String url, String uti);



	native public long accumulator_createWithExtent(double x, double y, double w, double h, boolean isFloat);
	native public long accumulator_getOutputImage(long accumulator);
	native public void accumulator_setImage(long accumulator, long image, double x, double y, double w, double h);
	native public void accumulator_release(long accumulator);


}
