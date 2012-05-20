package field.core.plugins.drawing.pdf;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.lowagie.text.BadElementException;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfName;

import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.plugins.drawing.pdf.BasePDFGraphicsContext.DrawingResult;
import field.core.plugins.drawing.pdf.BasePDFGraphicsContext.DrawingResultCode;
import field.core.plugins.drawing.pdf.BasePDFGraphicsContext.iDrawingAcceptor;
import field.graphics.ci.CoreImageCanvasUtils.Image;
import field.launch.iUpdateable;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.util.Dict;

public class SimplePDFImageDrawing {
	public class DrawsImage implements iDrawingAcceptor {
		private final BasePDFGraphicsContext context;
		private final SimplePDFLineDrawing drawing;

		public DrawsImage(BasePDFGraphicsContext context, SimplePDFLineDrawing drawing) {
			this.context = context;
			this.drawing = drawing;
		}

		public DrawingResult accept(List<iUpdateable> soFar, final CachedLine line, final Dict properties) {
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

				private void render(Vector2 destination, Object image, Vector4 colorFor, float opacityMul, Number s) {
					//todo, color / opacity
					if (image instanceof Image) {
						try {
							File tmp = File.createTempFile("fieldImagePDFExport", ".png");
							tmp.deleteOnExit();
							((Image) image).saveAsPNG("file://"+tmp.getAbsolutePath());
							
							Rect extents = ((Image)image).getExtents();
							float scale = s == null ? 1 : s.floatValue();
							
							Rect placement = new Rect(destination.x, destination.y, extents.w*scale, extents.h*scale);
							Vector4 ot = drawing.outputTransform;
							Vector2 topLeft = transform(ot, placement.topLeft());
							Vector2 bottomLeft = transform(ot, placement.bottomLeft());
							Vector2 bottomRight= transform(ot, placement.bottomRight());
							Vector2 topRight= transform(ot, placement.topRight());

							placement.x = Math.min(topLeft.x, topRight.x);
							placement.y = Math.min(topLeft.y, bottomLeft.y);
							placement.w = Math.max(topLeft.x, topRight.x)-placement.x;
							placement.h = Math.max(topLeft.y, bottomLeft.y)-placement.y;
							
							com.lowagie.text.Image pdfimage = com.lowagie.text.Image.getInstance(tmp.getAbsolutePath());
							context.getOutput().saveState();
							PdfGState gs1 = new PdfGState();
							float opacity = 1;
							gs1.setFillOpacity(opacity);
							gs1.setStrokeOpacity(opacity);
							gs1.setBlendMode(new PdfName("Multiply"));
							context.getOutput().setGState(gs1);
							
							;//System.out.println(" rev eng :"+destination+" "+extents+" "+scale+" "+placement);
							
							context.getOutput().addImage(pdfimage, (float) (placement.w), 0, 0, (float) (placement.h), (float) placement.x, (float) (-destination.y+ot.w-(placement.h)));
							context.getOutput().restoreState();
							
						} catch (IOException e) {
							e.printStackTrace();
						} catch (BadElementException e) {
							e.printStackTrace();
						} catch (DocumentException e) {
							e.printStackTrace();
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
			});
			return result;
		}

		protected Vector2 transform(Vector4 outputTransform, Vector3 point) {
			return new Vector2(point.x * outputTransform.x + outputTransform.z, point.y * outputTransform.y + outputTransform.w);
		}
	}
	
	public void installInto(BasePDFGraphicsContext context, SimplePDFLineDrawing drawing) {
		context.addDrawingAcceptor(new DrawsImage(context, drawing));
	}
}
