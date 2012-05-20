package field.graphics.ci;

import java.awt.Frame;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Shell;

import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext.DrawingResult;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext.DrawingResultCode;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext.iDrawingAcceptor;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.windowing.GLComponentWindow;
import field.graphics.ci.CoreImageCanvasUtils.Accumulator;
import field.graphics.ci.CoreImageCanvasUtils.Image;
import field.graphics.dynamic.iDynamicMesh;
import field.launch.iUpdateable;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;
import field.util.Dict;

public class SimpleImageDrawing {

	static int cacheSize = 50;

	boolean useRawDrawing = false;

	public class DrawsImage implements iDrawingAcceptor<CachedLine> {
		private final BaseGLGraphicsContext context;

		public DrawsImage(BaseGLGraphicsContext context) {
			this.context = context;
		}

		public DrawingResult accept(List<iDynamicMesh> soFar, final CachedLine line, final Dict properties) {
			if (!properties.isTrue(iLinearGraphicsContext.containsImages, false))
				return null;

			DrawingResult result = new DrawingResult(DrawingResultCode.cont, new iUpdateable() {
				private Vector4 black = new Vector4(1, 1, 1, 1f);

				public void update() {

					for (Event e : line.events) {
						if (e.attributes != null && e.attributes.get(iLinearGraphicsContext.image_v) != null) {
							float opacityMul = 1;
							Number o = line.getProperties().get(iLinearGraphicsContext.totalOpacity);
							opacityMul *= o == null ? 1 : o.floatValue();

							render(e.getDestination(new Vector2()), e.attributes.get(iLinearGraphicsContext.image_v), colorFor(e), opacityMul, e.attributes.get(iLinearGraphicsContext.imageDrawScale_v));
						}
					}
				}

				private void render(Vector2 destination, Object image, Vector4 colorFor, float opacityMul, Number v) {

					if (image instanceof Image) {

						Rect e = ((Image) image).getExtents();
						if (shouldCull(destination, e))
							return;

						if (useRawDrawing) {
							if (v == null)
								((Image) image).drawNowRaw(destination.x, destination.y, 1);
							else
								((Image) image).drawNowRaw(destination.x, destination.y, v.floatValue());

						} else {
							if (v == null)
								((Image) image).drawNow(destination.x, destination.y);
							else
								((Image) image).drawNow(destination.x, destination.y, v.floatValue());
						}
					} else if (image instanceof Accumulator) {
						if (useRawDrawing) {
							if (v == null)
								((Accumulator) image).getOutputImage().drawNowRaw(destination.x, destination.y, 1);
							else
								((Accumulator) image).getOutputImage().drawNowRaw(destination.x, destination.y, v.floatValue());
						} else {

							if (v == null)
								((Accumulator) image).getOutputImage().drawNow(destination.x, destination.y);
							else
								((Accumulator) image).getOutputImage().drawNow(destination.x, destination.y, v.floatValue());
						}
					}
				}

				private Vector4 colorFor(Event current) {
					Vector4 color = null;

					if (color == null)
						color = current.attributes != null ? current.attributes.get(iLinearGraphicsContext.fillColor_v) : null;
					if (color == null)
						color = properties.get(iLinearGraphicsContext.color);
					if (color == null)
						color = properties.get(iLinearGraphicsContext.fillColor);
					if (color == null)
						color = black;
					return color;
				}
			}, new iDynamicMesh[0]);
			return result;
		}

		protected boolean shouldCull(Vector2 destination, Rect e) {

			Vector2 topLeft = new Vector2(destination.x + e.x, destination.y + e.y);
			GLComponentWindow.getCurrentWindow(null).transformDrawingToWindow(topLeft);
			Vector2 bottomRight = new Vector2(destination.x + e.w + e.x, destination.y + e.h + e.y);
			GLComponentWindow.getCurrentWindow(null).transformDrawingToWindow(bottomRight);

			Shell f = GLComponentWindow.getCurrentWindow(null).getFrame();

			if (topLeft.x < f.getSize().x && topLeft.y < f.getSize().y && bottomRight.x > 0 && bottomRight.y > 0)
				return false;

			return true;
		}
	}

	static public Vector4 colorPickFromImage(BaseGLGraphicsContext installedContext, Vector2 position) {
		Set<CachedLine> lines = installedContext.getAllLines();
		for (CachedLine c : lines) {
			if (!c.getProperties().isTrue(iLinearGraphicsContext.containsImages, false))
				continue;

			for (Event e : c.events) {
				if (e.attributes != null && e.attributes.get(iLinearGraphicsContext.image_v) != null) {
					float opacityMul = 1;
					Number o = c.getProperties().get(iLinearGraphicsContext.totalOpacity);
					opacityMul *= o == null ? 1 : o.floatValue();

					;//System.out.println(" checking image <" + e.attributes.get(iLinearGraphicsContext.image_v) + ">");

					Vector4 v = pick(position, e.getDestination(new Vector2()), e.attributes.get(iLinearGraphicsContext.image_v));
					if (v != null)
						return v;
				}
			}
		}
		return null;
	}

	private static Vector4 pick(Vector2 target, Vector2 point, Image image) {

		;//System.out.println(target + " " + point + " " + image + " " + image.getExtents());

		Rect x = image.getExtents();
		x.x += point.x;
		x.y += point.y;

		;//System.out.println(" is <" + target + "> inside <" + x + ">");
		if (!x.isInside(target))
			return null;

		int px = (int) (target.x - point.x);
		int py = (int) (target.y - point.y);
		;//System.out.println(" is <" + px + ", " + py + "> inside <" + x.w + " " + x.h + ">");
		if (px >= 0 && px < (int) x.w && py >= 0 && py < (int) x.h) {
			FloatBuffer f = image.toFloatBuffer();
			float r = f.get(py * ((int) (x.w)) * 4 + px * 4 + 0);
			float g = f.get(py * ((int) (x.w)) * 4 + px * 4 + 1);
			float b = f.get(py * ((int) (x.w)) * 4 + px * 4 + 2);
			float a = f.get(py * ((int) (x.w)) * 4 + px * 4 + 3);
			return new Vector4(r, g, b, a);
		}
		return null;
	}

	public SimpleImageDrawing installInto(BaseGLGraphicsContext installedContext) {
		installedContext.addAcceptor(new DrawsImage(installedContext));
		return this;
	}

	public SimpleImageDrawing setUseRawDrawing(boolean useRawDrawing) {
		this.useRawDrawing = useRawDrawing;
		return this;
	}
}
