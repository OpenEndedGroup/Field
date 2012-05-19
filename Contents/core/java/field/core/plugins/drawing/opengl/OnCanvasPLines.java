package field.core.plugins.drawing.opengl;

import field.graphics.core.Base.iAcceptsSceneListElement;
import field.graphics.windowing.FullScreenCanvasSWT;

/**
 * class for backwards compatability with Field 12 and 13
 *
 */
public class OnCanvasPLines extends OnCanvasLines {

	public OnCanvasPLines(iAcceptsSceneListElement on, FullScreenCanvasSWT canvas) {
		super(on, canvas);
	}

}
