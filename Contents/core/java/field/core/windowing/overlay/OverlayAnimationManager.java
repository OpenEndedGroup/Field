package field.core.windowing.overlay;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.Yield;
import field.bytecode.protect.yield.YieldUtilities;
import field.core.Constants;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.windowing.GLComponentWindow;
import field.math.abstraction.iProvider;
import field.math.linalg.Vector4;
import field.util.TaskQueue;

public class OverlayAnimationManager {

	static public void notifyAsText(final iVisualElement root, String to, Rect rect) {
		final GLComponentWindow frame = iVisualElement.enclosingFrame.get(root);
		if (frame != null) {
				OverlayAnimationManager.notifyTextOnWindow(frame, to,rect, 1, new Vector4(1,1,1,0.15f));
		}
	}

	public static void requestRepaint(iVisualElement root) {
	}

	public static void warnAsText(iVisualElement root, String to, Rect rect) {
	}


	TaskQueue animationQueue = new TaskQueue();

	int n = 0;

	boolean request = false;

	int probation = 0;

	boolean locked = false;


	public void fadeUpAndDown(final List<CachedLine> draw, final int fadeInOver, final int remainFor, final int fadeOutOver) {

		final iProvider<Object> p = new iProvider<Object>() {
			@Woven
			@Yield
			public Object get() {
				int i = 0;
				for (i = 1; i < fadeInOver; i++) {
					float a = i / (float) (fadeInOver - 1);
					YieldUtilities.yield(a);
				}
				for (i = 0; i < remainFor; i++) {
					YieldUtilities.yield(1f);
				}
				for (i = 0; i < fadeOutOver; i++) {
					float a = 1 - i / (float) (fadeOutOver - 1);
					YieldUtilities.yield(a);
				}
				return 0f;
			}
		};
		animationQueue.new Task() {
			@Woven
			@Yield
			@Override
			protected void run() {

				while (locked) {
					recur();
					YieldUtilities.yield(false);
				}

				while (true) {
					locked = true;

					Float o = (Float) p.get();
					for (CachedLine c : draw) {
						c.getProperties().put(iLinearGraphicsContext.totalOpacity, o);
						iLinearGraphicsContext context = GLComponentWindow.fastContext == null ? GLComponentWindow.currentContext : GLComponentWindow.fastContext;
						
						context.submitLine(LineUtils.transformLine(c, null, null, null, null), c.getProperties());
					}
					if (o > 0f) {
						recur();
						YieldUtilities.yield(false);
					} else {
						locked = false;
						return;
					}
				}
			}
		};
	}

	static public void fadeUpAndDownOnWindow(final GLComponentWindow window, final List<CachedLine> draw, final int fadeInOver, final int remainFor, final int fadeOutOver) {

		final iProvider<Object> p = new iProvider<Object>() {
			@Woven
			@Yield
			public Object get() {
				int i = 0;
				for (i = 1; i < fadeInOver; i++) {
					float a = i / (float) (fadeInOver - 1);
					YieldUtilities.yield(a);
				}
				for (i = 0; i < remainFor; i++) {
					YieldUtilities.yield(1f);
				}
				for (i = 0; i < fadeOutOver; i++) {
					float a = 1 - i / (float) (fadeOutOver - 1);
					YieldUtilities.yield(a);
				}
				return 0f;
			}
		};
		window.getPreQueue().new Task() {
			@Woven
			@Yield
			@Override
			protected void run() {

				while (true) {

					Float o = (Float) p.get();
					for (CachedLine c : draw) {
						c.getProperties().put(iLinearGraphicsContext.totalOpacity, o);
						iLinearGraphicsContext context = GLComponentWindow.fastContext == null ? GLComponentWindow.currentContext : GLComponentWindow.fastContext;
						
						context.submitLine(LineUtils.transformLine(c, null, null, null, null), c.getProperties());
					}
					if (o > 0f) {
						recur();
						window.requestRepaint();
						YieldUtilities.yield(false);
					} else {
						return;
					}
				}
			}
		};
		window.requestRepaint();
	}

	public TaskQueue getAnimationQueue() {
		return animationQueue;
	}


	public void notifyText(GLComponentWindow window, String text, Rect mark) {
		notifyText(window, text, mark, 1, new Vector4(1, 1, 1, 0.15f));
	}

	public static void notifyTextOnWindow(GLComponentWindow window, String text, Rect mark, float speed, Vector4 back) {

		if (window.getCanvas().isDisposed()) return;
		
		int size = text.length() > 50 ? 24 : 36;

		float ow = window.getCanvas().getSize().x * window.getXScale();
		float oh = window.getCanvas().getSize().y * window.getYScale();

		float ox = window.getXTranslation();
		float oy = window.getYTranslation();

		CachedLine textLine = new CachedLine();
		textLine.getProperties().put(iLinearGraphicsContext.containsText, true);
		textLine.getInput().moveTo(ox + ow / 2, oy + oh / 2);
		textLine.getInput().setPointAttribute(iLinearGraphicsContext.text_v, text);
		textLine.getInput().setPointAttribute(iLinearGraphicsContext.alignment_v, 0f);
		textLine.getInput().setPointAttribute(iLinearGraphicsContext.font_v, new Font(Constants.defaultFont, Font.PLAIN, size));

		Rectangle2D bounds = new Font(Constants.defaultFont, Font.PLAIN, size).getStringBounds(text, new FontRenderContext(null, true, true));

		bounds.setRect(bounds.getX() + ox + ow / 2 - bounds.getWidth() / 2, bounds.getY() + oy + oh / 2 - 6 + bounds.getHeight() / 2, bounds.getWidth(), bounds.getHeight());

		CachedLine outerLine = new CachedLine();
		outerLine.getInput().moveTo(ox, oy);
		outerLine.getInput().lineTo(ox + ow, oy);
		outerLine.getInput().lineTo(ox + ow, oy + oh);
		outerLine.getInput().lineTo(ox, oy + oh);
		outerLine.getInput().lineTo(ox, oy);
		outerLine.getProperties().put(iLinearGraphicsContext.filled, true);
		outerLine.getProperties().put(iLinearGraphicsContext.color, new Vector4(0, 0, 0, 0.1));


		java.awt.geom.RoundRectangle2D.Double rr = new RoundRectangle2D.Double(bounds.getX()-20, bounds.getY()-23, bounds.getWidth()+20+20, bounds.getHeight()+23+23, 30,30);
		
		CachedLine innerLine = new LineUtils().piToCachedLine(rr.getPathIterator(null));
		
		innerLine.getProperties().put(iLinearGraphicsContext.filled, true);
		innerLine.getProperties().put(iLinearGraphicsContext.stroked, false);
		innerLine.getProperties().put(iLinearGraphicsContext.color, back);

		ArrayList<CachedLine> cl = new ArrayList<CachedLine>();
		cl.add(innerLine);
		// cl.add(mainText);
		cl.add(textLine);
		cl.add(outerLine);

		fadeUpAndDownOnWindow(window, cl, 3, (int) (20 * speed), (int) (20 * speed));
	}

	
	public void notifyText(GLComponentWindow window, String text, Rect mark, float speed, Vector4 back) {

		int size = text.length() > 50 ? 24 : 36;

		float ow = window.getCanvas().getSize().x * window.getXScale();
		float oh = window.getCanvas().getSize().y * window.getYScale();

		float ox = window.getXTranslation();
		float oy = window.getYTranslation();

		CachedLine textLine = new CachedLine();
		textLine.getProperties().put(iLinearGraphicsContext.containsText, true);
		textLine.getInput().moveTo(ox + ow / 2, oy + oh / 2);
		textLine.getInput().setPointAttribute(iLinearGraphicsContext.text_v, text);
		textLine.getInput().setPointAttribute(iLinearGraphicsContext.alignment_v, 0f);
		textLine.getInput().setPointAttribute(iLinearGraphicsContext.font_v, new Font(Constants.defaultFont, Font.PLAIN, size));

		Rectangle2D bounds = new Font(Constants.defaultFont, Font.PLAIN, size).getStringBounds(text, new FontRenderContext(null, true, true));

		bounds.setRect(bounds.getX() + ox + ow / 2 - bounds.getWidth() / 2, bounds.getY() + oy + oh / 2 - 6 + bounds.getHeight() / 2, bounds.getWidth(), bounds.getHeight());

		CachedLine outerLine = new CachedLine();
		outerLine.getInput().moveTo(ox, oy);
		outerLine.getInput().lineTo(ox + ow, oy);
		outerLine.getInput().lineTo(ox + ow, oy + oh);
		outerLine.getInput().lineTo(ox, oy + oh);
		outerLine.getInput().lineTo(ox, oy);
		outerLine.getProperties().put(iLinearGraphicsContext.filled, true);
		outerLine.getProperties().put(iLinearGraphicsContext.color, new Vector4(0, 0, 0, 0.1));


		java.awt.geom.RoundRectangle2D.Double rr = new RoundRectangle2D.Double(bounds.getX()-20, bounds.getY()-23, bounds.getWidth()+20+20, bounds.getHeight()+23+23, 30,30);
		
		CachedLine innerLine = new LineUtils().piToCachedLine(rr.getPathIterator(null));
		
		innerLine.getProperties().put(iLinearGraphicsContext.filled, true);
		innerLine.getProperties().put(iLinearGraphicsContext.stroked, false);
		innerLine.getProperties().put(iLinearGraphicsContext.color, back);

		ArrayList<CachedLine> cl = new ArrayList<CachedLine>();
		cl.add(innerLine);
		// cl.add(mainText);
		cl.add(textLine);
		cl.add(outerLine);

		fadeUpAndDown(cl, 3, (int) (50 * speed), (int) (30 * speed));
	}

	public void requestRepaint() {
		request = true;
	}

}
