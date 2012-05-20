package field.core.plugins.drawing.pdf;

import java.awt.Color;
import java.awt.geom.CubicCurve2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfTemplate;

import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLineCursor;
import field.core.plugins.drawing.opengl.Small3dLineEmitter_long;
import field.core.plugins.drawing.opengl.SmallLineEmitter;
import field.core.plugins.drawing.opengl.TessLine3dEmitter_long;
import field.core.plugins.drawing.opengl.iLine_m;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext.iTransformingContext;
import field.core.plugins.drawing.pdf.BasePDFGraphicsContext.DrawingResult;
import field.core.plugins.drawing.pdf.BasePDFGraphicsContext.DrawingResultCode;
import field.core.plugins.drawing.pdf.BasePDFGraphicsContext.iDrawingAcceptor;
import field.core.util.PythonCallableMap;
import field.graphics.core.Base;
import field.graphics.dynamic.DynamicLine;
import field.graphics.dynamic.DynamicMesh;
import field.graphics.dynamic.SubLine;
import field.graphics.dynamic.iDynamicMesh;
import field.launch.iUpdateable;
import field.math.abstraction.iMetric;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Pair;
import field.util.Dict;
import field.util.Dict.Prop;

/**
 * a version of SimplePDFLineDrawing that's aware of other contexts (which can
 * transform things arbitrarily)
 * 
 * This is a fair bit slower than SimplePDFLineDrawing because of all the
 * vector3'ing around
 * 
 * @author marc
 * 
 */

public class SimplePDFLineDrawing_3d {

	static public class ExportedStyle {
		Vector4 fillColor = new Vector4();

		Vector4 strokeColor = new Vector4();

		float thickness = 0;

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final ExportedStyle other = (ExportedStyle) obj;
			if (fillColor == null) {
				if (other.fillColor != null)
					return false;
			} else if (!fillColor.equals(other.fillColor))
				return false;
			if (strokeColor == null) {
				if (other.strokeColor != null)
					return false;
			} else if (!strokeColor.equals(other.strokeColor))
				return false;
			if (Float.floatToIntBits(thickness) != Float.floatToIntBits(other.thickness))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fillColor == null) ? 0 : fillColor.hashcodeFor());
			result = prime * result + ((strokeColor == null) ? 0 : strokeColor.hashcodeFor());
			result = prime * result + Float.floatToIntBits(thickness);
			return result;
		}

		@Override
		public String toString() {
			return "es[f" + fillColor + " s" + strokeColor + " " + thickness + "]";

		}
	}

	static public PythonCallableMap thicknessOpacity = new PythonCallableMap();

	protected Pair<Float, Float> filteredThickness(float thick, float w) {

		if (!thicknessOpacity.isEmpty()) {
			Vector2 v = (Vector2) thicknessOpacity.invokeChained(thick, w);
			if (v != null)
				return new Pair<Float, Float>(v.x, v.y);
		}
		return null;
	}

	static public PythonCallableMap colorTransform = new PythonCallableMap();

	protected Vector4 filteredColor(Vector4 c) {

		if (!colorTransform.isEmpty()) {
			Vector4 v = (Vector4) colorTransform.invokeChained(c);
			if (v != null)
				return v;
		}
		return c;
	}

	public class PlainPDFLine implements iDrawingAcceptor {

		private final BasePDFGraphicsContext context;

		private String opName;

		PdfContentByte path = null;

		boolean firstOut = true;

		SmallLineEmitter lineEmitter = new SmallLineEmitter() {
			Vector2 lastOut = null;

			Vector4 black = new Vector4(0, 0, 0, 1);

			@Override
			public void emitLinearFrame(Vector2 a, Vector2 b, java.util.List<Object> name, java.util.List<Object> name2, Dict properties, iLinearGraphicsContext contex) {
				if (firstOut || lastOut.distanceFrom(a) > 1e-5) {

					PlainPDFLine.this.context.getOutput().stroke();
					PlainPDFLine.this.context.getOutput().newPath();

					Vector2 at = transform(a);

					path.moveTo(at.x, at.y);
					setStrokeProperties(name, properties);
					firstOut = false;
					lastOut = new Vector2(b);
				}

				Vector2 at = transform(b);
				path.lineTo(at.x, at.y);
				lastOut.x = b.x;
				lastOut.y = b.y;
			}

			protected void setStrokeProperties(List<Object> name, Dict properties) {

				Vector4 color = (Vector4) name.get(0);
				if (color == null)
					color = properties.get(iLinearGraphicsContext.strokeColor);
				if (color == null)
					color = properties.get(iLinearGraphicsContext.color);
				if (color == null)
					color = black;

				float thick = widthFor(context, properties);

				thick = remapProperties(color = new Vector4(color), null, thick);

				Pair<Float, Float> filtered = filteredThickness(thick, color.w);
				if (filtered != null) {
					thick = filtered.left;
					if (filtered.right != color.w) {
						color = new Vector4(color);
						color.w = filtered.right;
					}
				}

				if (lastStyle != null && ongoingStyle != null) {
					if (lastStyle.equals(ongoingStyle) && (lastStyleOp != null && opName.equals(lastStyleOp))) {
						// System.err.println(" skipping
						// set stroke prop");
						// return;

					}
				}

				PdfContentByte o = context.getOutput();
				o.setMiterLimit(2);
				if (thick > 30) {
					o.setLineJoin(path.LINE_JOIN_ROUND);
					o.setLineCap(path.LINE_CAP_ROUND);
				}

				System.err.println(" set stoke properties <" + color + "> <" + opName + ">");
				o.setColorStroke(new Color(clamp(color.x), clamp(color.y), clamp(color.z)));
				o.setLineWidth(thick);

				if (opName.equals("Opaque")) {
					PdfGState gs1 = new PdfGState();
					gs1.setFillOpacity(1);
					gs1.setStrokeOpacity(1);
					gs1.setBlendMode(new PdfName("Normal"));
					o.setGState(gs1);
				} else {
					PdfGState gs1 = new PdfGState();
					// gs1.put(new PdfName("SA"), new
					// PdfName(PdfBoolean.FALSE));

					gs1.setFillOpacity(color.w);
					gs1.setStrokeOpacity(color.w);
					gs1.setBlendMode(new PdfName(opName));
					o.setGState(gs1);
				}

				ongoingStyle = lastStyle;
				lastStyleOp = opName;

			}
		};

		public PlainPDFLine(BasePDFGraphicsContext context) {
			this.context = context;
			lineEmitter.addTrackedProperty(iLinearGraphicsContext.strokeColor_v, new iMetric<Vector4, Vector4>() {
				public float distance(Vector4 from, Vector4 to) {
					if (from == null || to == null)
						return 0;
					float d = from.distanceFrom(to);
					return d;
				}
			});

			lineEmitter.setCanEmitCubic(false);
		}

		public DrawingResult accept(List<iUpdateable> soFar, final CachedLine line, final Dict properties) {
			if (!properties.isTrue(iLinearGraphicsContext.stroked, true))
				return null;
			if (properties.isTrue(iLinearGraphicsContext.containsDepth, false))
				return null;

			Vector4 color = null;
			if (color == null)
				color = properties.get(iLinearGraphicsContext.strokeColor);
			if (color == null)
				color = properties.get(iLinearGraphicsContext.color);
			if (color != null) {
				if (color.w < 0.001)
					return null;
			}

			return new DrawingResult(DrawingResultCode.cont, new iUpdateable() {

				public void update() {
					if (!context.isLayer(line))
						return;

					opName = properties.get(iLinearGraphicsContext.outputOpacityType);
					if (opName == null)
						opName = defaultOpType;

					// todo, transp

					path = context.getOutput();
					path.saveState();

					lineEmitter.setCanEmitCubic(false);

					lineEmitter.globalGeometricScale = 0;
					lineEmitter.globalFlatnessScale = context.getGlobalProperties().getFloat(iLinearGraphicsContext.flatnessScale, 1);

					beginTransform(line, properties);

					drawInto(line, properties, null, context, lineEmitter);

					firstOut = true;

					Number dd = properties.get(PDFDash);
					if (dd != null) {
						context.getOutput().setLineDash(dd.floatValue(), dd.floatValue() * 1.4f, 0);
					}

					context.getOutput().stroke();
					context.getOutput().newPath();

					System.err.println(" submitting path ");
					path.restoreState();
				}
			});

		}

		public float widthFor(BasePDFGraphicsContext context, Dict properties) {
			Number f = properties.get(iLinearGraphicsContext.thickness);
			Number m = context.getGlobalProperties().get(iLinearGraphicsContext.strokeThicknessMul);

			return (f == null ? 1 : f.floatValue()) * (m == null ? 1 : m.floatValue());
		}

	}

	public class Plain3dPDFLine implements iDrawingAcceptor {

		private final BasePDFGraphicsContext context;

		private String opName;

		PdfContentByte path = null;

		boolean firstOut = true;

		Small3dLineEmitter_long lineEmitter = new Small3dLineEmitter_long() {
			Vector3 lastOut = null;

			Vector4 black = new Vector4(0, 0, 0, 1);

			@Override
			public void begin() {
				lastOut = new Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
			}

			@Override
			public void emitLinearFrame(Vector3 a, Vector3 b, java.util.List<Object> name, java.util.List<Object> name2, Dict properties, iLinearGraphicsContext contex) {
				if (firstOut || lastOut.distanceFrom(a) > 1e-2) {

					Plain3dPDFLine.this.context.getOutput().stroke();
					Plain3dPDFLine.this.context.getOutput().newPath();

					Vector2 at = transform(a);

					path.moveTo(at.x, at.y);
					setStrokeProperties(name, properties);
					firstOut = false;
					lastOut = new Vector3(b);
				}

				Vector2 at = transform(b);
				if (!shouldClip) {
					path.lineTo(at.x, at.y);
				}
				lastOut.x = b.x;
				lastOut.y = b.y;
				lastOut.z = b.z;
			}

			protected void setStrokeProperties(List<Object> name, Dict properties) {

				Vector4 color = (Vector4) name.get(1);
				if (color == null)
					color = properties.get(iLinearGraphicsContext.strokeColor);
				if (color == null)
					color = properties.get(iLinearGraphicsContext.color);
				if (color == null)
					color = black;

				float thick = widthFor(context, properties);

				thick = remapProperties(color = new Vector4(color), null, thick);

				Pair<Float, Float> filtered = filteredThickness(thick, color.w);
				if (filtered != null) {
					thick = filtered.left;
					if (filtered.right != color.w) {
						color = new Vector4(color);
						color.w = filtered.right;
					}
				}

				if (lastStyle != null && ongoingStyle != null) {
					if (lastStyle.equals(ongoingStyle) && (lastStyleOp != null && opName.equals(lastStyleOp))) {
						// System.err.println(" skipping
						// set stroke prop");
						// return;

					}
				}

				PdfContentByte o = context.getOutput();
				o.setMiterLimit(2);
				if (thick > 30) {
					o.setLineJoin(path.LINE_JOIN_ROUND);
					o.setLineCap(path.LINE_CAP_ROUND);
				}

				System.err.println(" set stoke properties <" + color + "> <" + opName + ">");
				o.setColorStroke(new Color(clamp(color.x), clamp(color.y), clamp(color.z)));
				o.setLineWidth(thick);

				if (opName.equals("Opaque")) {
					PdfGState gs1 = new PdfGState();
					gs1.setFillOpacity(1);
					gs1.setStrokeOpacity(1);
					gs1.setBlendMode(new PdfName("Normal"));
					o.setGState(gs1);
				} else {
					PdfGState gs1 = new PdfGState();
					// gs1.put(new PdfName("SA"), new
					// PdfName(PdfBoolean.FALSE));

					gs1.setFillOpacity(color.w);
					gs1.setStrokeOpacity(color.w);
					gs1.setBlendMode(new PdfName(opName));
					o.setGState(gs1);
				}

				ongoingStyle = lastStyle;
				lastStyleOp = opName;
			}

			@Override
			protected boolean shouldTerm(float flatness, Vector3 a, Vector3 c1, Vector3 c2, Vector3 b, int n) {
				if (c1 == null)
					return super.shouldTerm(flatness, a, c1, c2, b, n);

				Vector2 a2 = transform(a);
				Vector2 b2 = transform(b);
				Vector2 c12 = transform(c1);
				Vector2 c22 = transform(c2);

				float geometric1 = c1 == null ? 0 : (float) CubicCurve2D.getFlatness(a2.x, a2.y, c12.x, c12.y, c22.x, c22.y, b2.x, b2.y);

				return n > 5 || geometric1 < globalGeometricScale;
			}
		};

		public Plain3dPDFLine(BasePDFGraphicsContext context) {
			this.context = context;
			lineEmitter.addTrackedProperty(iLinearGraphicsContext.strokeColor_v, new iMetric<Vector4, Vector4>() {
				public float distance(Vector4 from, Vector4 to) {
					if (from == null || to == null)
						return 0;
					float d = from.distanceFrom(to);
					return d;
				}
			});

			lineEmitter.setCanEmitCubic(false);
		}

		public DrawingResult accept(List<iUpdateable> soFar, final CachedLine line, final Dict properties) {
			if (!properties.isTrue(iLinearGraphicsContext.stroked, true))
				return null;
			if (!properties.isTrue(iLinearGraphicsContext.containsDepth, false))
				return null;

			Vector4 color = null;
			if (color == null)
				color = properties.get(iLinearGraphicsContext.strokeColor);
			if (color == null)
				color = properties.get(iLinearGraphicsContext.color);
			if (color != null) {
				if (color.w < 0.001)
					return null;
			}

			return new DrawingResult(DrawingResultCode.cont, new iUpdateable() {

				public void update() {
					if (!context.isLayer(line))
						return;

					opName = properties.get(iLinearGraphicsContext.outputOpacityType);
					if (opName == null)
						opName = defaultOpType;

					// todo, transp

					path = context.getOutput();
					path.saveState();

					lineEmitter.setCanEmitCubic(false);

					lineEmitter.globalGeometricScale = 0;
					lineEmitter.globalFlatnessScale = context.getGlobalProperties().getFloat(iLinearGraphicsContext.flatnessScale, 1);

					beginTransform(line, properties);

					drawInto(line, properties, null, context, lineEmitter);

					firstOut = true;

					Number dd = properties.get(PDFDash);
					if (dd != null) {
						context.getOutput().setLineDash(dd.floatValue(), dd.floatValue() * 1.4f, 0);
					}

					context.getOutput().stroke();
					context.getOutput().newPath();

					System.err.println(" submitting path ");
					path.restoreState();
				}
			});

		}

		public float widthFor(BasePDFGraphicsContext context, Dict properties) {
			Number f = properties.get(iLinearGraphicsContext.thickness);
			Number m = context.getGlobalProperties().get(iLinearGraphicsContext.strokeThicknessMul);

			return (f == null ? 1 : f.floatValue()) * (m == null ? 1 : m.floatValue());
		}

	}

	public class SimplePDFFill implements iDrawingAcceptor {

		private final BasePDFGraphicsContext context;

		PdfContentByte path = null;

		boolean firstOut = true;

		String opName;

		Small3dLineEmitter_long lineEmitter = new Small3dLineEmitter_long() {
			Vector3 lastOut = null;

			Vector4 black = new Vector4(0, 0, 0, 1);

			@Override
			public void emitLinearFrame(Vector3 a, Vector3 b, java.util.List<Object> name, java.util.List<Object> name2, Dict properties, iLinearGraphicsContext contex) {
				Vector4 outputTransform = SimplePDFLineDrawing_3d.this.outputTransform;
				if (properties.isTrue(iLinearGraphicsContext.noTransform, false))
					outputTransform = new Vector4(1, 1, 0, 0);

				setStrokeProperties(name, properties);
				if (firstOut || lastOut.distanceFrom(a) > 1e-5) {
					Vector2 at = transform(a);
					path.moveTo(at.x, at.y);
					firstOut = false;
					lastOut = new Vector3(b);
				}
				Vector2 at = transform(b);
				
				;//System.out.println(" line to <"+b+" -> "+at);
				if (!shouldClip)
					path.lineTo(at.x, at.y);
				lastOut.x = b.x;
				lastOut.y = b.y;
			}

			protected void setStrokeProperties(List<Object> name, Dict properties) {

				Vector4 color = (Vector4) name.get(1);
				if (color == null)
					color = properties.get(iLinearGraphicsContext.fillColor);
				if (color == null)
					color = properties.get(iLinearGraphicsContext.color);
				if (color == null)
					color = black;

				float thick = widthFor(context, properties);

				thick = remapProperties(color = new Vector4(color), null, thick);

				PdfContentByte o = context.getOutput();

				o.setColorFill(new Color(clamp(color.x), clamp(color.y), clamp(color.z)));

				if (opName.equals("Opaque")) {
					PdfGState gs1 = new PdfGState();
					gs1.setFillOpacity(1);
					gs1.setStrokeOpacity(1);
					gs1.setBlendMode(new PdfName("Normal"));
					o.setGState(gs1);
				}
				{
					PdfGState gs1 = new PdfGState();
					gs1.setFillOpacity(color.w);
					gs1.setStrokeOpacity(color.w);
					gs1.setBlendMode(new PdfName(opName));
					o.setGState(gs1);
				}

			}
			
			@Override
			protected boolean shouldTerm(float flatness, Vector3 a, Vector3 c1, Vector3 c2, Vector3 b, int n) {
				if (c1 == null)
					return super.shouldTerm(flatness, a, c1, c2, b, n);

				Vector2 a2 = transform(a);
				Vector2 b2 = transform(b);
				Vector2 c12 = transform(c1);
				Vector2 c22 = transform(c2);

				float geometric1 = c1 == null ? 0 : (float) CubicCurve2D.getFlatness(a2.x, a2.y, c12.x, c12.y, c22.x, c22.y, b2.x, b2.y);

				return n > 5 || geometric1 < 0.05f;
			}
		};

		public SimplePDFFill(BasePDFGraphicsContext context) {
			this.context = context;
			lineEmitter.addTrackedProperty(iLinearGraphicsContext.strokeColor_v, new iMetric<Vector4, Vector4>() {
				public float distance(Vector4 from, Vector4 to) {
					if (from == null || to == null)
						return 0;
					float d = from.distanceFrom(to);
					return d;
				}
			});

		}

		public DrawingResult accept(List<iUpdateable> soFar, final CachedLine line, final Dict properties) {
			if (!properties.isTrue(iLinearGraphicsContext.filled, false))
				return null;
			if (properties.isTrue(iLinearGraphicsContext.needVertexShading, false))
				return null;

			System.err.println(" -- non special fill -- ");

			Vector4 color = null;
			if (color == null)
				color = properties.get(iLinearGraphicsContext.fillColor);
			if (color == null)
				color = properties.get(iLinearGraphicsContext.color);
			if (color != null) {
				if (color.w < 0.001) {
					System.err.println(" rejected: too transparent <" + color + ">");
					return null;
				} else {
					System.err.println(" color is <" + color + ">");
				}
			}

			float w = widthFor(context, properties);
			if (w < 1e-5) {
				System.err.println(" rejected: too thin");
			}

			final DynamicMesh outputLine = DynamicMesh.unshadedMesh();

			final TessLine3dEmitter_long fillEmitter = new TessLine3dEmitter_long() {

				@Override
				protected void decorateVertex(int v1, List<Object> name) {
					Object color = name == null ? null : name.get(0);
					if (color == null)
						color = properties.get(iLinearGraphicsContext.fillColor);
					if (color == null)
						color = properties.get(iLinearGraphicsContext.color);
					if (color == null)
						color = black;

					if (color instanceof float[])
						color = new Vector4(((float[]) color)[0], ((float[]) color)[1], ((float[]) color)[2], ((float[]) color)[3]);

					remapProperties(null, (Vector4) (color = new Vector4((Vector4) color)), 0);

					mesh.setAux(v1, Base.color0_id, ((Vector4) color).x, ((Vector4) color).y, ((Vector4) color).z, ((Vector4) color).w);
				}

			};

			fillEmitter.addTrackedProperty(iLinearGraphicsContext.fillColor_v, new iMetric<Vector4, Vector4>() {
				public float distance(Vector4 from, Vector4 to) {
					if (from == null || to == null)
						return 0;

					float d = (from).distanceFrom((to)) * 100;
					return d;
				}
			});

			return new DrawingResult(DrawingResultCode.cont, new iUpdateable() {

				public void update() {

					;//System.out.println(" will update fill ");

					if (!context.isLayer(line))
						return;

					opName = properties.get(iLinearGraphicsContext.outputOpacityType);
					if (opName == null)
						opName = defaultOpType;

					path = context.getOutput();
					path.saveState();
					path.setMiterLimit(2);

					lineEmitter.setCanEmitCubic(false);

					//lineEmitter.globalGeometricScale = 0;
					lineEmitter.globalFlatnessScale = context.getGlobalProperties().getFloat(iLinearGraphicsContext.flatnessScale, 1);

					;//System.out.println(" begin transform ");
					beginTransform(line, properties);

					;//System.out.println(" drawing now ");
					drawInto(line, properties, null, context, lineEmitter);

					firstOut = true;

					// context.getOutput().clip();
					context.getOutput().fill();

					;//System.out.println(" call that a path ");
					context.getOutput().newPath();

					fillEmitter.globalFlatnessScale = 1;
					fillEmitter.globalGeometricScale = 1;

					// drawInto(line, properties, null,
					// context, fillEmitter);
					//
					// MeshToPdfType4.dilateMesh(fillEmitter.mesh.getUnderlyingGeometry(),
					// line);
					//
					// PdfShading_type4 shading = new
					// MeshToPdfType4.PdfShading_type4(context.getWriter(),
					// (TriangleMesh)
					// fillEmitter.mesh.getUnderlyingGeometry());
					// context.getOutput().saveState();
					// context.getOutput().paintShading(shading);
					// context.getOutput().restoreState();
					//
					//

					System.err.println(" submitting path ");
					path.restoreState();
				}
			});

		}

		public float widthFor(BasePDFGraphicsContext context, Dict properties) {
			Number f = properties.get(iLinearGraphicsContext.thickness);
			Number m = context.getGlobalProperties().get(iLinearGraphicsContext.strokeThicknessMul);

			return (f == null ? 1 : f.floatValue()) * (m == null ? 1 : m.floatValue());
		}

	}

	public class SquarePointer implements iDrawingAcceptor {

		BasePDFGraphicsContext context;

		PdfTemplate unitSquare = null;

		float unitSquareLastSize = -1;

		Vector4 unitSquareLastColor = null;

		public SquarePointer(BasePDFGraphicsContext context) {
			this.context = context;
		}

		public DrawingResult accept(List<iUpdateable> soFar, final CachedLine line, final Dict properties) {

			if (!properties.isTrue(iLinearGraphicsContext.pointed, false))
				return null;

			line.finish();

			unitSquare = null;

			DrawingResult res = new DrawingResult(DrawingResultCode.cont, new iUpdateable() {

				public void update() {
					if (!context.isLayer(line))
						return;

					boolean notEnds = properties.isTrue(pointNotAtEnds, false);

					beginTransform(line, properties);

					for (CachedLine.Event e : line.events) {
						if (notEnds && e.attributes != null && e.attributes.isTrue(isClippedEnd_v, false)) {
							System.err.println(" skipped clipped end");
							continue;
						}

						if (!e.method.equals(iLine_m.close_m)) {

							float size = 1f / outputTransform.x;

							Number ps = e.getAttributes().get(iLinearGraphicsContext.pointSize_v);
							if (ps == null)
								ps = properties.get(iLinearGraphicsContext.pointSize);
							if (ps != null)
								size = ps.floatValue() / outputTransform.x;

							size *= 0.25f;

							Vector4 color = e.getAttributes().get(iLinearGraphicsContext.pointColor_v);
							if (color == null)
								color = properties.get(iLinearGraphicsContext.pointColor);
							if (color == null)
								color = properties.get(iLinearGraphicsContext.strokeColor);
							if (color == null)
								color = properties.get(iLinearGraphicsContext.color);
							if (color == null)
								color = new Vector4(0, 0, 0, 1);

							color.w *= properties.getFloat(iLinearGraphicsContext.totalOpacity, 1f);

							size *= context.getGlobalProperties().getFloat(pointSizeMul, 1);

							remapProperties(null, color = new Vector4(color), 0);

							if (unitSquare == null || unitSquareLastColor.distanceFrom(color) > 0 || Math.abs(unitSquareLastSize - size) > 0.25f) {
								constructUnitSquare(color, size);

								unitSquareLastColor = color;
								unitSquareLastSize = size;
								PdfGState gs = new PdfGState();
								gs.setFillOpacity(color.w);
								String opName = properties.get(iLinearGraphicsContext.outputOpacityType);
								if (opName == null)
									opName = defaultOpType;

								gs.setBlendMode(new PdfName(opName));
								context.getOutput().setGState(gs);
							}

							Vector2 at = transform(e.getDestination());

							context.getOutput().addTemplate(unitSquare, -size / 2 + at.x, -size / 2 + at.y);

							// CachedLine markerLine
							// = new CachedLine();
							// iLine in =
							// markerLine.getInput();
							//
							// Vector2 dest =
							// e.getDestination();
							// in.moveTo(dest.x -
							// size / 2f, dest.y -
							// size / 2);
							// in.lineTo(dest.x +
							// size / 2f, dest.y -
							// size / 2);
							// in.lineTo(dest.x +
							// size / 2f, dest.y +
							// size / 2);
							// in.lineTo(dest.x -
							// size / 2f, dest.y +
							// size / 2);
							// in.lineTo(dest.x -
							// size / 2f, dest.y -
							// size / 2);
							// markerLine.getProperties().put(iLinearGraphicsContext.line_isStroked,
							// false);
							// markerLine.getProperties().put(iLinearGraphicsContext.line_isFilled,
							// true);
							// markerLine.getProperties().put(iLinearGraphicsContext.line_globalColor,
							// color);
							//
							// System.err.println("
							// point si8ze
							// <"+size+">");
							//
							// System.err.println("
							// making fill proxy ,,,
							// ");
							//
							// new
							// SimplePDFFill(context).accept(new
							// ArrayList<iUpdateable>(),
							// markerLine,
							// markerLine.getProperties()).compute.update();
							// System.err.println("
							// making finished ,,,
							// ");
						}
					}

				}
			});

			return res;
		}

		protected void constructUnitSquare(Vector4 color, float size) {
			unitSquare = context.getOutput().createTemplate(size, size);

			unitSquare.moveTo(0, 0);
			unitSquare.lineTo(size, 0);
			unitSquare.lineTo(size, size);
			unitSquare.lineTo(0, size);
			unitSquare.lineTo(0, 0);

			unitSquare.setColorFill(new Color(clamp(color.x), clamp(color.y), clamp(color.z)));
			unitSquare.fill();
		}

	}

	public class CircularPointer implements iDrawingAcceptor {

		BasePDFGraphicsContext context;

		PdfTemplate unitSquare = null;

		float unitSquareLastSize = -1;

		Vector4 unitSquareLastColor = null;

		public CircularPointer(BasePDFGraphicsContext context) {
			this.context = context;
		}

		public DrawingResult accept(List<iUpdateable> soFar, final CachedLine line, final Dict properties) {

			if (!properties.isTrue(iLinearGraphicsContext.pointed, false))
				return null;

			if (properties.isTrue(iLinearGraphicsContext.containsDepth, false))
				return null;

			line.finish();

			unitSquare = null;

			DrawingResult res = new DrawingResult(DrawingResultCode.cont, new iUpdateable() {

				public void update() {
					if (!context.isLayer(line))
						return;

					boolean notEnds = properties.isTrue(pointNotAtEnds, false);

					for (CachedLine.Event e : line.events) {
						if (notEnds && e.attributes != null && e.attributes.isTrue(isClippedEnd_v, false)) {
							System.err.println(" skipped clipped end");
							continue;
						}

						if (!e.method.equals(iLine_m.close_m)) {

							float size = 1f / outputTransform.x;

							Number ps = e.getAttributes().get(iLinearGraphicsContext.pointSize_v);
							if (ps == null)
								ps = properties.get(iLinearGraphicsContext.pointSize);
							if (ps != null)
								size = ps.floatValue() / outputTransform.x;

							// size *= 0.25f;

							Vector4 color = e.getAttributes().get(iLinearGraphicsContext.pointColor_v);
							if (color == null)
								color = properties.get(iLinearGraphicsContext.pointColor);
							if (color == null)
								color = properties.get(iLinearGraphicsContext.strokeColor);
							if (color == null)
								color = properties.get(iLinearGraphicsContext.color);
							if (color == null)
								color = new Vector4(0, 0, 0, 1);

							color.w *= properties.getFloat(iLinearGraphicsContext.totalOpacity, 1f);

							size *= context.getGlobalProperties().getFloat(pointSizeMul, 1);

							remapProperties(null, color = new Vector4(color), 0);

							if (unitSquare == null || unitSquareLastColor.distanceFrom(color) > 0 || Math.abs(unitSquareLastSize - size) > 0.25f) {
								constructUnitSquare(color, size);

								unitSquareLastColor = color;
								unitSquareLastSize = size;
								PdfGState gs = new PdfGState();
								gs.setFillOpacity(color.w);
								String opName = properties.get(iLinearGraphicsContext.outputOpacityType);
								if (opName == null)
									opName = defaultOpType;

								gs.setBlendMode(new PdfName(opName));
								context.getOutput().setGState(gs);
							}

							Vector2 at = transform(e.getDestination());

							context.getOutput().addTemplate(unitSquare, -size / 2 + at.x, -size / 2 + at.y);

							// CachedLine markerLine
							// = new CachedLine();
							// iLine in =
							// markerLine.getInput();
							//
							// Vector2 dest =
							// e.getDestination();
							// in.moveTo(dest.x -
							// size / 2f, dest.y -
							// size / 2);
							// in.lineTo(dest.x +
							// size / 2f, dest.y -
							// size / 2);
							// in.lineTo(dest.x +
							// size / 2f, dest.y +
							// size / 2);
							// in.lineTo(dest.x -
							// size / 2f, dest.y +
							// size / 2);
							// in.lineTo(dest.x -
							// size / 2f, dest.y -
							// size / 2);
							// markerLine.getProperties().put(iLinearGraphicsContext.line_isStroked,
							// false);
							// markerLine.getProperties().put(iLinearGraphicsContext.line_isFilled,
							// true);
							// markerLine.getProperties().put(iLinearGraphicsContext.line_globalColor,
							// color);
							//
							// System.err.println("
							// point si8ze
							// <"+size+">");
							//
							// System.err.println("
							// making fill proxy ,,,
							// ");
							//
							// new
							// SimplePDFFill(context).accept(new
							// ArrayList<iUpdateable>(),
							// markerLine,
							// markerLine.getProperties()).compute.update();
							// System.err.println("
							// making finished ,,,
							// ");
						}
					}

				}
			});

			return res;
		}

		protected void constructUnitSquare(Vector4 color, float size) {
			unitSquare = context.getOutput().createTemplate(size, size);

			unitSquare.circle(size / 2, size / 2, size / 2);

			unitSquare.setColorFill(new Color(clamp(color.x), clamp(color.y), clamp(color.z)));
			unitSquare.fill();
		}

	}

	public class CircularPointer3d implements iDrawingAcceptor {

		BasePDFGraphicsContext context;

		PdfTemplate unitSquare = null;

		float unitSquareLastSize = -1;

		Vector4 unitSquareLastColor = null;

		public CircularPointer3d(BasePDFGraphicsContext context) {
			this.context = context;
		}

		public DrawingResult accept(List<iUpdateable> soFar, final CachedLine line, final Dict properties) {

			if (!properties.isTrue(iLinearGraphicsContext.pointed, false))
				return null;

			if (!properties.isTrue(iLinearGraphicsContext.containsDepth, false))
				return null;
			
			;//System.out.println(" circular pointer 3d");
			

			line.finish();

			unitSquare = null;

			beginTransform(line, properties);
			
			DrawingResult res = new DrawingResult(DrawingResultCode.cont, new iUpdateable() {

				public void update() {
					if (!context.isLayer(line))
						return;

					boolean notEnds = properties.isTrue(pointNotAtEnds, false);

					for (CachedLine.Event e : line.events) {
						if (notEnds && e.attributes != null && e.attributes.isTrue(isClippedEnd_v, false)) {
							System.err.println(" skipped clipped end");
							continue;
						}

						if (!e.method.equals(iLine_m.close_m)) {

							float size = 1f / outputTransform.x;

							Number ps = e.getAttributes().get(iLinearGraphicsContext.pointSize_v);
							if (ps == null)
								ps = properties.get(iLinearGraphicsContext.pointSize);
							if (ps != null)
								size = ps.floatValue() / outputTransform.x;

							// size *= 0.25f;

							Vector4 color = e.getAttributes().get(iLinearGraphicsContext.pointColor_v);
							if (color == null)
								color = properties.get(iLinearGraphicsContext.pointColor);
							if (color == null)
								color = properties.get(iLinearGraphicsContext.strokeColor);
							if (color == null)
								color = properties.get(iLinearGraphicsContext.color);
							if (color == null)
								color = new Vector4(0, 0, 0, 1);

							color.w *= properties.getFloat(iLinearGraphicsContext.totalOpacity, 1f);

							size *= context.getGlobalProperties().getFloat(pointSizeMul, 1);

							remapProperties(null, color = new Vector4(color), 0);

							if (unitSquare == null || unitSquareLastColor.distanceFrom(color) > 0 || Math.abs(unitSquareLastSize - size) > 0.25f) {
								constructUnitSquare(color, size);

								unitSquareLastColor = color;
								unitSquareLastSize = size;
								PdfGState gs = new PdfGState();
								gs.setFillOpacity(color.w);
								String opName = properties.get(iLinearGraphicsContext.outputOpacityType);
								if (opName == null)
									opName = defaultOpType;

								gs.setBlendMode(new PdfName(opName));
								context.getOutput().setGState(gs);
							}

							Vector2 a = e.getDestination();
							float z = 0;
							Object d = e.getAttributes().get(iLinearGraphicsContext.z_v);
							if (d == null)
								z = 0;
							else if (d instanceof Number)
								z = ((Number) d).floatValue();
							else if (d instanceof Vector3)
								z = ((Vector3) d).z;

							Vector2 at = transform(new Vector3(a.x, a.y, z));
							
							;//System.out.println(" transformed <"+a+" "+z+"> to <"+at+"> <"+shouldClip+">");
							
							if (!shouldClip)
								context.getOutput().addTemplate(unitSquare, -size / 2 + at.x, -size / 2 + at.y);
						}
					}

				}
			});

			return res;
		}

		protected void constructUnitSquare(Vector4 color, float size) {
			unitSquare = context.getOutput().createTemplate(size, size);

			unitSquare.circle(size / 2, size / 2, size / 2);

			unitSquare.setColorFill(new Color(clamp(color.x), clamp(color.y), clamp(color.z)));
			unitSquare.fill();
		}

	}

	public static final Prop<Number> PDFDash = new Prop<Number>("line_globalPDFDash");

	// static public Vector4 outputTransform = new Vector4(1,1,0,0);

	public static final Prop<Number> pointNotAtEnds = new Prop<Number>("global_pointNotAtEnds");

	public static final Prop<Number> isClippedEnd_v = new Prop<Number>("isClippedEnd_v");

	public static final Prop<Number> pointSizeMul = new Prop<Number>("pointSizeMul");

	static public Vector4 outputTransform = new Vector4(2, -2, 0, 2000);

	public static String defaultOpType = "Multiply";

	static ExportedStyle ongoingStyle = null;

	static ExportedStyle lastStyle = null;

	static String lastStyleOp = null;

	HashSet<ExportedStyle> lastExportedStyle = new HashSet<ExportedStyle>();

	HashMap<ExportedStyle, ExportedStyle> transformedStyle = new HashMap<ExportedStyle, ExportedStyle>();

	ExportedStyle _tmpStyle = new ExportedStyle();

	private iTransformingContext transformContext;

	public SimplePDFLineDrawing_3d() {
	}

	public void drawInto(CachedLine line, Dict properties, iDynamicMesh outputLine, iLinearGraphicsContext context, SmallLineEmitter lineEmitter) {

		CachedLineCursor cursor = new CachedLineCursor(line);

		line.finish();

		if (outputLine != null)
			outputLine.open();
		Vector2 a = new Vector2();
		Vector2 b = new Vector2();
		Vector2 c1 = new Vector2();
		Vector2 c2 = new Vector2();
		lineEmitter.begin();
		while (cursor.hasNextSegment()) {
			if (outputLine instanceof DynamicLine)
				((DynamicLine) outputLine).startLine();
			if (outputLine instanceof SubLine)
				((SubLine) outputLine).getLine().startLine();
			lineEmitter.beginContour();
			while (cursor.hasNextInSpline()) {

				if (cursor.nextIsCubic()) {
					if (cursor.nextCubicFrame(a, c1, c2, b)) {
						lineEmitter.flattenCubicFrame(a, c1, c2, b, lineEmitter.packet(cursor.getCurrent()), lineEmitter.packet(cursor.getAfter()), properties, context, 0);
					}
				} else {

					if (cursor.nextLinearFrame(a, b)) {
						lineEmitter.flattenLinearFrame(a, b, lineEmitter.packet(cursor.getCurrent()), lineEmitter.packet(cursor.getAfter()), properties, context, 0);
					}
				}

				cursor.next();
			}
			if (outputLine instanceof DynamicLine)
				((DynamicLine) outputLine).endLine();
			if (outputLine instanceof SubLine)
				((SubLine) outputLine).getLine().endLine();
			lineEmitter.endContour();

			if (cursor.hasNextSegment())
				cursor.next();
		}
		lineEmitter.end();
		if (outputLine != null)
			outputLine.close();
	}

	public void drawInto(CachedLine line, Dict properties, iDynamicMesh outputLine, iLinearGraphicsContext context, Small3dLineEmitter_long lineEmitter) {

		CachedLineCursor cursor = new CachedLineCursor(line);

		line.finish();

		if (outputLine != null)
			outputLine.open();
		Vector2 a = new Vector2();
		Vector2 b = new Vector2();
		Vector2 c1 = new Vector2();
		Vector2 c2 = new Vector2();
		lineEmitter.begin();
		while (cursor.hasNextSegment()) {
			if (outputLine instanceof DynamicLine)
				((DynamicLine) outputLine).startLine();
			if (outputLine instanceof SubLine)
				((SubLine) outputLine).getLine().startLine();
			lineEmitter.beginContour();
			while (cursor.hasNextInSpline()) {

				if (cursor.nextIsCubic()) {
					if (cursor.nextCubicFrame(a, c1, c2, b)) {
						lineEmitter.flattenCubicFrame(a, c1, c2, b, lineEmitter.packet(cursor.getCurrent()), lineEmitter.packet(cursor.getAfter()), properties, context, 0);
					}
				} else {

					if (cursor.nextLinearFrame(a, b)) {
						lineEmitter.flattenLinearFrame(a, b, lineEmitter.packet(cursor.getCurrent()), lineEmitter.packet(cursor.getAfter()), properties, context, 0);
					}
				}

				cursor.next();
			}
			if (outputLine instanceof DynamicLine)
				((DynamicLine) outputLine).endLine();
			if (outputLine instanceof SubLine)
				((SubLine) outputLine).getLine().endLine();
			lineEmitter.endContour();

			if (cursor.hasNextSegment())
				cursor.next();
		}
		lineEmitter.end();
		if (outputLine != null)
			outputLine.close();
	}

	public HashSet<ExportedStyle> getLastExportedStyle() {
		return lastExportedStyle;
	}

	public HashMap<ExportedStyle, ExportedStyle> getTransformedStyle() {
		return transformedStyle;
	}

	public void installInto(BasePDFGraphicsContext context) {
		context.addDrawingAcceptor(new CircularPointer(context));
		context.addDrawingAcceptor(new CircularPointer3d(context));
		context.addDrawingAcceptor(new SimplePDFFill(context));
		context.addDrawingAcceptor(new Plain3dPDFLine(context));
		context.addDrawingAcceptor(new PlainPDFLine(context));
	}

	protected float clamp(float z) {
		return Math.min(1, Math.max(0, z));
	}

	protected void beginTransform(CachedLine line, Dict properties) {
		iLinearGraphicsContext context = properties.get(iLinearGraphicsContext.context);

		;//System.out.println(" line context is <" + context + ">");

		if (context instanceof iTransformingContext) {
			transformContext = (iTransformingContext) context;
		} else {
			transformContext = null;
		}
	}

	protected Vector2 transform(Vector2 a) {
		Vector2 m = new Vector2(a);
		if (transformContext != null) {
			transformContext.convertIntermediateSpaceToDrawingSpace(a.toVector3(), m);
			shouldClip = transformContext.shouldClip(a.toVector3());


		} else
			shouldClip = false;

		m.x = m.x * outputTransform.x + outputTransform.z;
		m.y = m.y * outputTransform.y + outputTransform.w;

		if ((m.x)>1e6) m.x = 1e5f;
		if ((m.y)>1e6) m.y = 1e5f;
		if ((m.x)<-1e6) m.x = -1e5f;
		if ((m.y)<-1e6) m.y = -1e5f;

		return m;
	}

	boolean shouldClip = false;

	protected Vector2 transform(Vector3 a) {
		Vector2 m = new Vector2(a.x, a.y);
		if (transformContext != null) {
			transformContext.convertIntermediateSpaceToDrawingSpace(a, m);
			shouldClip = transformContext.shouldClip(a);
			
			
		} else
			shouldClip = false;

		m.x = m.x * outputTransform.x + outputTransform.z;
		m.y = m.y * outputTransform.y + outputTransform.w;

		if ((m.x)>1e6) m.x = 1e5f;
		if ((m.y)>1e6) m.y = 1e5f;
		if ((m.x)<-1e6) m.x = -1e5f;
		if ((m.y)<-1e6) m.y = -1e5f;

		return m;
	}

	protected float remapProperties(Vector4 strokeColor, Vector4 fillColor, float thick) {

		ExportedStyle theStyle = _tmpStyle;

		_tmpStyle.fillColor = fillColor;
		_tmpStyle.strokeColor = strokeColor;
		_tmpStyle.thickness = thick;

		boolean added = lastExportedStyle.add(_tmpStyle);

		if (added) {
			_tmpStyle = new ExportedStyle();
		}

		ExportedStyle newStyle = transformedStyle.get(theStyle);

		if (newStyle != null) {
			if (strokeColor != null)
				strokeColor.set(newStyle.strokeColor);
			if (fillColor != null)
				fillColor.set(newStyle.fillColor);

			lastStyle = newStyle;

			return newStyle.thickness;
		} else {
			lastStyle = theStyle;
			return thick;
		}

	}

}
