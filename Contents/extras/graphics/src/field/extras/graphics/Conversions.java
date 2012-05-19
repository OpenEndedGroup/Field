package field.extras.graphics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import field.graphics.core.AdvancedTextures.Base4FloatTexture;
import field.graphics.core.AdvancedTextures.BaseFloatTexture;

/**
 * converting between float arrays, buffers, and other things and 1 and 4 float
 * Textures
 */
public class Conversions {

	static public void update(float[] from, Object to) {
		if (to instanceof BaseFloatTexture)
			copyFloat1dArrayToTexture(from, (BaseFloatTexture) to);
		else if (to instanceof Base4FloatTexture)
			copyFloat1dArrayToTexture4(from, (Base4FloatTexture) to);
		else
			throw new IllegalArgumentException(" cant update <" + to + "> with float array");
	}

	static public void update(float[][] from, Object to) {
		if (to instanceof BaseFloatTexture)
			copyFloat2dArrayToTexture(from, (BaseFloatTexture) to);
		else
			throw new IllegalArgumentException(" cant update <" + to + "> with 2d float array");
	}

	static public void update(int[][] from, Object to) {
		if (to instanceof BaseFloatTexture)
			copyInteger2dArrayToTexture(from, (BaseFloatTexture) to, 1);
		else
			throw new IllegalArgumentException(" cant update <" + to + "> with 2d int array");
	}

	static public void copyFloat1dArrayToTexture(float[] from, BaseFloatTexture to) {
		if (from.length != to.getWidth() * to.getHeight())
			throw new IllegalArgumentException("dimension mismatch \u2014 float array has length " + from.length + " but texture has dimensions " + to.getWidth() + " " + to.getHeight());

		FloatBuffer f = to.getStorage();
		to.dirty();

		for (int i = 0; i < from.length; i++) {
			f.put(from[i]);
		}
		f.rewind();
	}

	static public void copyFloat2dArrayToTexture(float[][] from, BaseFloatTexture to) {
		if (from.length != to.getWidth())
			throw new IllegalArgumentException("dimension mismatch \u2014 float array has length " + from.length + " but texture has x dimension " + to.getWidth());
		if (from[0].length != to.getHeight())
			throw new IllegalArgumentException("dimension mismatch \u2014 float array has length " + from.length + " but texture has y dimension " + to.getHeight());

		FloatBuffer f = to.getStorage();
		to.dirty();

		for (int i = 0; i < from.length; i++) {
			for (int j = 0; j < from[i].length; j++) {
				f.put(from[i][j]);
			}
		}
		f.rewind();
	}

	static public void copyInteger2dArrayToTexture(int[][] from, BaseFloatTexture to, float scale) {
		if (from.length != to.getWidth())
			throw new IllegalArgumentException("dimension mismatch \u2014 float array has length " + from.length + " but texture has x dimension " + to.getWidth());
		if (from[0].length != to.getHeight())
			throw new IllegalArgumentException("dimension mismatch \u2014 float array has length " + from.length + " but texture has y dimension " + to.getHeight());

		FloatBuffer f = to.getStorage();
		to.dirty();

		for (int i = 0; i < from.length; i++) {
			for (int j = 0; j < from[i].length; j++) {
				f.put(from[i][j] / scale);
			}
		}

		f.rewind();
	}

	static public void copyFloat1dArrayToTexture4(float[] from, Base4FloatTexture to) {
		if (from.length != to.getWidth() * to.getHeight()*4)
			throw new IllegalArgumentException("dimension mismatch \u2014 float array has length " + from.length + " but texture has dimensions " + to.getWidth() + " " + to.getHeight());

		FloatBuffer f = to.getStorage();
		to.dirty();

		for (int i = 0; i < from.length; i++) {
			f.put(from[i]);
		}
		f.rewind();
	}

	static public Base4FloatTexture make4FloatTexture(float[] f, int width, int height) {
		FloatBuffer s = ByteBuffer.allocateDirect(4 * width * height * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		s.put(f);
		s.rewind();
		return new Base4FloatTexture(s, width, height);
	}

	static public BaseFloatTexture makeFloatTexture(float[] f, int width, int height) {
		FloatBuffer s = ByteBuffer.allocateDirect(4 * width * height).order(ByteOrder.nativeOrder()).asFloatBuffer();
		s.put(f);
		s.rewind();
		return new BaseFloatTexture(s, width, height);
	}

	static public BaseFloatTexture makeFloatTexture(float[][] f) {
		FloatBuffer s = ByteBuffer.allocateDirect(4 * f.length * f[0].length).order(ByteOrder.nativeOrder()).asFloatBuffer();
		return new BaseFloatTexture(s, f.length, f[0].length);
	}

	static public BaseFloatTexture makeFloatTexture(int[][] f) {
		FloatBuffer s = ByteBuffer.allocateDirect(4 * f.length * f[0].length).order(ByteOrder.nativeOrder()).asFloatBuffer();
		return new BaseFloatTexture(s, f.length, f[0].length);
	}

}
