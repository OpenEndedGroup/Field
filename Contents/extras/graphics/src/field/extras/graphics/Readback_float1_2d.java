package field.extras.graphics;

import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

import field.bytecode.protect.dispatch.Cont;
import field.bytecode.protect.dispatch.Cont.ReturnCode;
import field.bytecode.protect.dispatch.Cont.aRun;
import field.graphics.core.BasicContextManager;
import field.graphics.windowing.FullScreenCanvasSWT;
import field.math.abstraction.iProvider;

/**
 * readback for 2d array of 1-floats
 * @author marc
 *
 */
public class Readback_float1_2d {

	private final iProvider<Integer> from;
	private final int width;
	private final int height;
	private ByteBuffer storage;

	public Readback_float1_2d(iProvider<Integer> from, int width, int height)
	{
		this.from = from;
		this.width = width;
		this.height = height;
		
		storage = ByteBuffer.allocateDirect(4*width*height*4).order(ByteOrder.nativeOrder());
	}
	
	public void join(FullScreenCanvasSWT canvas)
	{
		aRun arun = new Cont.aRun() {
			@Override
			public ReturnCode head(Object calledOn, Object[] args) {
				readbackNow(BasicContextManager.getGl());
				return super.head(calledOn, args);
			}
		};
		Cont.linkWith(canvas, canvas.method_beforeFlush, arun);
	}
	
	protected void readbackNow(Object gl)
	{
		
		;//;//System.out.println(" reading from <"+from.get()+">");
		
		if (from.get()<1) return ;
	
		int[] a = {0};
		a[0] = glGetInteger(GL_FRAMEBUFFER_BINDING);
		glBindFramebuffer(GL_FRAMEBUFFER, from.get());
		glReadPixels(0, 0, width, height, GL_RGBA, GL11.GL_FLOAT, storage);
		glBindFramebuffer(GL_FRAMEBUFFER, a[0]);

		// this could be better \u2014 let's try to push the finish (or the fence) into the update
		glFinish();
	}
	
	
	public void update(float[][] out)
	{
		if (out.length != width)
			throw new IllegalArgumentException("dimension mismatch \u2014 float array has length " + out.length+ " but texture has x dimension " + width);
		if (out[0].length != height)
			throw new IllegalArgumentException("dimension mismatch \u2014 float array has length " + out.length + " but texture has y dimension " + height);

		// we could push the finish into here
		
		FloatBuffer f = storage.asFloatBuffer();
		
		for (int i = 0; i < out.length; i++) {
			for (int j = 0; j < out[i].length; j++) {
				out[i][j] = f.get();
			}
		}		
	}
}
