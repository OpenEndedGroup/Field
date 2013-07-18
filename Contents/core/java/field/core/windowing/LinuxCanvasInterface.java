package field.core.windowing;

import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.widgets.Canvas;
import org.lwjgl.opengl.GL11;

public class LinuxCanvasInterface implements iCanvasInterface {

	private GLCanvas c;

	public LinuxCanvasInterface(Canvas c) {
		this.c = (GLCanvas) c;
	}

	@Override
	public void setCurrent() {
		c.setCurrent();
	}

	long last = 0;
	@Override
	public void swapBuffers() {
		c.swapBuffers();
		
//		GL11.glFlush();
//		GL11.glFinish();
		
	}

}
