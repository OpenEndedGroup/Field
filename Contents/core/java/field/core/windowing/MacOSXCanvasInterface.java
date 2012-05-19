package field.core.windowing;

import org.eclipse.swt.widgets.Canvas;

public class MacOSXCanvasInterface implements iCanvasInterface{

	private GLCanvas_field canvas;

	public MacOSXCanvasInterface(Canvas c)
	{
		this.canvas = ((GLCanvas_field)c);
	}
	
	@Override
	public void swapBuffers() {
		canvas.swapBuffers();
	}

	@Override
	public void setCurrent() {
		canvas.setCurrent();
	}

}
