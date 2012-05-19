package field.core.plugins.drawing.opengl;

import java.awt.BasicStroke;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import field.core.plugins.drawing.opengl.BaseGLGraphicsContext.DrawingResult;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext.DrawingResultCode;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext.iDrawingAcceptor;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.plugins.drawing.opengl.TessLine3dEmitter_long.VInfo;
import field.graphics.core.Base;
import field.graphics.core.Base.StandardPass;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.graphics.core.BasicUtilities.EnableDepthTestWrap;
import field.graphics.dynamic.DynamicLine;
import field.graphics.dynamic.DynamicLine_long;
import field.graphics.dynamic.DynamicMesh_long;
import field.graphics.dynamic.DynamicPointlist;
import field.graphics.dynamic.SubLine;
import field.graphics.dynamic.SubLine_long;
import field.graphics.dynamic.iDynamicMesh;
import field.launch.iUpdateable;
import field.math.abstraction.iMetric;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.namespace.generic.Bind.iFunction;
import field.util.BiMap;
import field.util.Dict;
import field.util.Dict.Prop;

public class SimpleLineDrawing {

	public boolean doFakeAa = false;

	public class ObjectFastHighlight implements iDrawingAcceptor<CachedLine> {

		private final BaseGLGraphicsContext context;

		public ObjectFastHighlight(BaseGLGraphicsContext context) {
			this.context = context;
		}

		public DrawingResult accept(List<iDynamicMesh> soFar, final CachedLine line, Dict properties) {
			if (!properties.isTrue(iLinearGraphicsContext.shouldHighlight, false))
				return null;

			final DynamicLine_long outputLine = getCachedLineForWidth(context, Math.min(20, Math.max(widthFor(context, properties) * 10, 5)));

			DrawingResult result = new DrawingResult(DrawingResultCode.cont, new iUpdateable() {
				public void update() {
					// copySource
					// Mesh
					outputLine.open();
					Vector3 v = new Vector3();
					Vector2 v2 = new Vector2();

					int cursorStart = outputLine.getVertexCursor();
					boolean started = false;
					int ii = 0;
					if (line.events.size() < 200)
						for (Event e : line.events) {
							if (e.hasDestination()) {
								e.getDestination(v2);
								v.setValue(v2.x, v2.y, 0);
							}
							if (e.method.equals(iLine_m.moveTo_m)) {
								if (started)
									outputLine.endLine();
								outputLine.startLine();
								outputLine.moveTo(v);
								started = true;
							} else if (e.method.equals(iLine_m.lineTo_m))
								outputLine.lineTo(v);
							else if (e.method.equals(iLine_m.cubicTo_m))
								outputLine.lineTo(v);
							ii++;
							if (ii > 200)
								break;
						}
					if (started)
						outputLine.endLine();
					int cursorEnd = outputLine.getVertexCursor();
					for (int i = cursorStart; i < cursorEnd; i++) {
						outputLine.setAux(i, Base.color0_id, 0, 0, 0, 0.1f);
					}
					outputLine.close();
				}
			}, outputLine);
			return result;
		}
	}

	public class PlainGLFillDefault implements iDrawingAcceptor<CachedLine> {

		private final BaseGLGraphicsContext context;

		private Dict properties;

		public PlainGLFillDefault(BaseGLGraphicsContext context) {
			this.context = context;
		}

		public DrawingResult accept(List<iDynamicMesh> soFar, final CachedLine line, final Dict properties) {
			this.properties = properties;

			if (!properties.isTrue(iLinearGraphicsContext.filled, false))
				return null;

			if (properties.isTrue(iLinearGraphicsContext.containsDepth, false))
				return null;

			float opacityMul = 1;
			Number o = line.getProperties().get(iLinearGraphicsContext.totalOpacity);
			opacityMul *= o == null ? 1 : o.floatValue();
			final float fopacityMul = opacityMul;

			if (fopacityMul == 0)
				return null;

			final DynamicMesh_long outputLine = getCachedMesh(context, properties.isTrue(iLinearGraphicsContext.soloCache, false));
			final DynamicMesh_long outputLineOld = getOldCachedMesh(context, properties.isTrue(iLinearGraphicsContext.soloCache, false));

			final TessLineEmitter_long fillEmitter = new TessLineEmitter_long() {

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

					mesh.setAux(v1, Base.color0_id, ((Vector4) color).x, ((Vector4) color).y, ((Vector4) color).z, ((Vector4) color).w * fopacityMul);
				}

			};

			float wr = properties.getFloat(iLinearGraphicsContext.windingRule, 0);

			fillEmitter.setWidingRule(wr);

			fillEmitter.addTrackedProperty(iLinearGraphicsContext.fillColor_v, new iMetric<Vector4, Vector4>() {
				public float distance(Vector4 from, Vector4 to) {
					if (from == null || to == null)
						return 0;

					float d = (from).distanceFrom((to)) * 10;
					return d;
				}
			});

			drawInto(line, properties, outputLine, context, fillEmitter);

			DrawingResult result = new DrawingResult(DrawingResultCode.cont, new iUpdateable() {
				int seen = 0;

				public void update() {

					DynamicMesh_long x = seen > 2 ? outputLineOld : outputLine;

					if (!context.isLayer(line))
						return;

					// copySource
					// Mesh
					x.open();
					x.copyFrom(fillEmitter.mesh);
					x.close();

					if (fillEmitter.mesh.getCopy() == null)
						fillEmitter.mesh.makeCopy();

					seen++;
				}
			}, outputLine);

			return result;
		}
	}

	public class PlainGL3dFillDefault implements iDrawingAcceptor<CachedLine> {

		private final BaseGLGraphicsContext context;

		private Dict properties;

		public PlainGL3dFillDefault(BaseGLGraphicsContext context) {
			this.context = context;
		}

		float constantDistance = 0;
		List<Integer> extendedAttributes = null;
		
		CoordinateFrame transform = null;

		public DrawingResult accept(List<iDynamicMesh> soFar, final CachedLine line, final Dict properties) {
			this.properties = properties;

			if (!properties.isTrue(iLinearGraphicsContext.filled, false))
				return null;

			if (!properties.isTrue(iLinearGraphicsContext.containsDepth, false))
				return null;

			float opacityMul = 1;
			Number o = line.getProperties().get(iLinearGraphicsContext.totalOpacity);
			opacityMul *= o == null ? 1 : o.floatValue();
			final float fopacityMul = opacityMul;

			if (fopacityMul == 0)
				return null;

			boolean late = properties.isTrue(iLinearGraphicsContext.lateRendering, false);

			System.out.println(" late is <" + late + ">");

			final DynamicMesh_long outputLine = late ? getCachedMeshLate(context) : getCachedMesh(context, properties.isTrue(iLinearGraphicsContext.soloCache, false));
			final DynamicMesh_long outputLineOld = properties.isTrue(iLinearGraphicsContext.soloCache, false) ? outputLine : (late ? outputLine : getOldCachedMesh(context, properties.isTrue(iLinearGraphicsContext.soloCache, false)));

			Dict p = line.getProperties();
			CoordinateFrame f = p.get(iLinearGraphicsContext.fastTransform);
			if (f != null) {
				((TriangleMesh) outputLine.getUnderlyingGeometry()).setCoordindateFrameProvider(f);
			}

			final TessLine3dEmitter_long fillEmitter = new TessLine3dEmitter_long() {

				@Override
				protected void decorateVertex(int v1, List<Object> name) {
					Object color = name == null ? null : name.get(1);
					if (color == null)
						color = properties.get(iLinearGraphicsContext.fillColor);
					if (color == null)
						color = properties.get(iLinearGraphicsContext.color);
					if (color == null)
						color = black;

					if (color instanceof float[])
						color = new Vector4(((float[]) color)[0], ((float[]) color)[1], ((float[]) color)[2], ((float[]) color)[3]);

					mesh.setAux(v1, Base.color0_id, ((Vector4) color).x, ((Vector4) color).y, ((Vector4) color).z, ((Vector4) color).w * fopacityMul);

					if (extendedAttributes != null) {
						int q = 2;
						for (Integer ii : extendedAttributes) {
							Object c = name.get(q);
							if (c instanceof Vector4) {
								Vector4 cc = (Vector4) c;
								mesh.setAux(v1, ii, cc.x, cc.y, cc.z, cc.w);
							}
							q++;
						}
					}
				}

				@Override
				protected boolean shouldTerm(float flatness, Vector3 a, Vector3 c1, Vector3 c2, Vector3 b, int n) {
					return constantDistance == 0 ? super.shouldTerm(flatness, a, c1, c2, b, n) : a.distanceFrom(b) < constantDistance;
				}
				
				protected void nextVertex(VInfo info) {
					if (transform==null)
					{
						super.nextVertex(info);
						return;
					}
					
					if (info.vertex == -1) {
						info.vertex = mesh.nextVertex(maybeTransform(transform,info.position));
						decorateVertex(info.vertex, info.properties);
					}
				}
			};

			Map<String, Number> shaderAttributes = properties.get(iLinearGraphicsContext.shaderAttributes);
			constantDistance = properties.getFloat(iLinearGraphicsContext.constantDistanceResampling, 0);

			if (shaderAttributes != null) {
				fillEmitter.clearTrackedProperties();
				fillEmitter.addTrackedProperty(iLinearGraphicsContext.strokeColor_v, new iMetric<Vector4, Vector4>() {
					public float distance(Vector4 from, Vector4 to) {
						if (from == null || to == null)
							return 0;
						float d = from.distanceFrom(to);
						return d;
					}
				});
				extendedAttributes = new ArrayList<Integer>();
				Set<Entry<String, Number>> es = shaderAttributes.entrySet();
				int q = 2;
				for (Entry<String, Number> e : es) {
					fillEmitter.addTrackedProperty(new Prop(e.getKey()), new iMetric() {
						public float distance(Object from, Object to) {
							return 0;
						}
					});
					extendedAttributes.add(e.getValue().intValue());
				}
			} else {
				extendedAttributes = null;
				fillEmitter.clearTrackedProperties();
				fillEmitter.addTrackedProperty(iLinearGraphicsContext.strokeColor_v, new iMetric<Vector4, Vector4>() {
					public float distance(Vector4 from, Vector4 to) {
						if (from == null || to == null)
							return 0;
						float d = from.distanceFrom(to);
						return d;
					}
				});
			}

			transform = properties.get(iLinearGraphicsContext.transform);

			drawInto(line, properties, outputLine, context, fillEmitter);

			DrawingResult result = new DrawingResult(DrawingResultCode.cont, new iUpdateable() {
				int seen = 0;

				CoordinateFrame prevTransform = transform==null ? null : transform.duplicate();
				
				public void update() {

					DynamicMesh_long x = seen > 2 ? outputLineOld : outputLine;

					if (!context.isLayer(line))
						return;

					if (!SimpleLineDrawing.equals(prevTransform, transform))
					{
						drawInto(line, properties, outputLine, context, fillEmitter);
						prevTransform = transform==null ? null : transform.duplicate();
					}
					
					// copySource
					// Mesh
					x.open();
					x.copyFrom(fillEmitter.mesh);
					x.close();

					if (fillEmitter.mesh.getCopy() == null)
						fillEmitter.mesh.makeCopy();

					seen++;

				}
			}, outputLine);

			return result;
		}
	}

	public class PlainGLLineDefault implements iDrawingAcceptor<CachedLine> {

		private final BaseGLGraphicsContext context;

		private float fopacityMul;

		protected SubLine_long outputLine;

		CoordinateFrame transform;
		
		SmallLineEmitter_long lineEmitter = new SmallLineEmitter_long() {
			@Override
			public void emitLinearFrame(Vector2 a, Vector2 b, java.util.List<Object> name, java.util.List<Object> name2, Dict properties, iLinearGraphicsContext contex) {

				int v1 = outputLine.nextVertex(maybeTransform(transform, a.toVector3()));
				int v2 = outputLine.nextVertex(maybeTransform(transform, b.toVector3()));

				decorateVertex(v1, name, outputLine, properties);
				decorateVertex(v2, name2, outputLine, properties);
			}

			protected void decorateVertex(int v1, List<Object> name, SubLine_long outputLine, Dict properties) {

				Object color = name.get(0);
				if (color == null)
					color = properties.get(iLinearGraphicsContext.strokeColor);
				if (color == null)
					color = properties.get(iLinearGraphicsContext.color);
				if (color == null)
					color = black;

				outputLine.setAux(v1, Base.color0_id, ((Vector4) color).x, ((Vector4) color).y, ((Vector4) color).z, ((Vector4) color).w * fopacityMul);
			}
		};

		public PlainGLLineDefault(BaseGLGraphicsContext context) {
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

		public DrawingResult accept(List<iDynamicMesh> soFar, final CachedLine line, final Dict properties) {
			if (!properties.isTrue(iLinearGraphicsContext.stroked, true))
				return null;

			if (properties.isTrue(iLinearGraphicsContext.strokeType, false))
				return null;

			if (properties.isTrue(iLinearGraphicsContext.containsDepth, false))
				return null;

			float opacityMul = 1;
			Number o = line.getProperties().get(iLinearGraphicsContext.totalOpacity);
			opacityMul *= o == null ? 1 : o.floatValue();
			fopacityMul = opacityMul;

			if (fopacityMul == 0)
				return null;

			final float fwidth = widthFor(context, properties);

			transform = properties.get(iLinearGraphicsContext.transform);

			final SubLine_long subLine = new SubLine_long();
			PlainGLLineDefault.this.outputLine = subLine;
			subLine.open();
			drawInto(line, properties, subLine, context, lineEmitter);
			subLine.close();

			final DynamicLine_long outputLine = getCachedLineForWidth(context, fwidth);
			final DynamicLine_long outputLineOld = getOldCachedLineForWidth(context, fwidth);

			final boolean disabledSkipCache = properties.isTrue(iLinearGraphicsContext.slow, false);

			
			DrawingResult result = new DrawingResult(DrawingResultCode.cont, new iUpdateable() {

				int seen = 0;
				CoordinateFrame prevTransform = transform==null ? null : transform.duplicate();
				
				public void update() {

					transform = line.getProperties().get(iLinearGraphicsContext.transform);

					DynamicLine_long x = seen > 2 ? outputLineOld : outputLine;

					if (!context.isLayer(line))
						return;

					if (!SimpleLineDrawing.equals(prevTransform, transform))
					{
						PlainGLLineDefault.this.outputLine = subLine;
						subLine.open();
						drawInto(line, properties, subLine, context, lineEmitter);
						subLine.close();
						prevTransform = transform==null ? null : transform.duplicate();
					}
					
					x.open();
					x.copyFrom(subLine, !disabledSkipCache);

					x.close();
					if (subLine.getCopy() == null)
						subLine.makeCopy();

					seen++;
				}

			}, outputLine);

			return result;
		}

	}

	public class PlainGLPointDefault implements iDrawingAcceptor<CachedLine> {
		private final BaseGLGraphicsContext context;

		private Dict properties;

		public PlainGLPointDefault(BaseGLGraphicsContext context) {
			this.context = context;
		}

		CoordinateFrame transform;
		
		public DrawingResult accept(List<iDynamicMesh> soFar, final CachedLine line, final Dict properties) {
			this.properties = properties;

			if (!properties.isTrue(iLinearGraphicsContext.pointed, false))
				return null;

			if (properties.isTrue(iLinearGraphicsContext.containsDepth, false))
				return null;

			float opacityMul = 1;
			Number o = line.getProperties().get(iLinearGraphicsContext.totalOpacity);
			opacityMul *= o == null ? 1 : o.floatValue();
			final float fopacityMul = opacityMul;

			if (fopacityMul == 0)
				return null;

			final DynamicPointlist outputLine = getCachedPoints(context);
			// final DynamicPointlist
			// outputLine =
			// DynamicPointlist.unshadedPoints(context.getVertexProgram());
			DrawingResult result = new DrawingResult(DrawingResultCode.cont, new iUpdateable() {
				Vector4 black = new Vector4(0, 0, 0, 1);

				public void update() {
					if (!context.isLayer(line))
						return;

					CachedLineCursor cursor = new CachedLineCursor(line);

					outputLine.open();
					while (cursor.hasNextSegment()) {
						cursor.next();
						Vector2 v2 = new Vector2();
						cursor.getAt(v2);

						int v = outputLine.nextVertex(maybeTransform(transform, v2.toVector3()));
						Vector4 color = colorFor(cursor.getCurrent());
						outputLine.setAux(v, Base.color0_id, color.x, color.y, color.z, color.w * fopacityMul);
						Vector4 point = pointSizeFor(cursor.getCurrent());
						outputLine.setAux(v, 13, point.x, point.y, point.z, point.w);
					}
					outputLine.close();
				}

				private Vector4 colorFor(Event current) {
					Vector4 color = null;

					if (color == null)
						color = current.attributes != null ? current.attributes.get(iLinearGraphicsContext.pointColor_v) : null;
					if (color == null)
						color = current.attributes != null ? current.attributes.get(iLinearGraphicsContext.fillColor_v) : null;
					if (color == null)
						color = properties.get(iLinearGraphicsContext.pointColor);
					if (color == null)
						color = properties.get(iLinearGraphicsContext.color);
					if (color == null)
						color = properties.get(iLinearGraphicsContext.fillColor);
					if (color == null)
						color = black;
					return color;
				}

				private Vector4 pointSizeFor(Event current) {
					Number size = null;
					if (size == null)
						size = properties.get(iLinearGraphicsContext.pointSize);
					if (size == null)
						size = current.getAttributes().getFloat(iLinearGraphicsContext.pointSize_v, 5f);
					if (size == null)
						size = 5f;

					Vector4 color = new Vector4(size.floatValue(), 0, 0, 0);
					return color;
				}

			}, outputLine);

			return result;
		}

	}

	public class PlainGL3dPointDefault implements iDrawingAcceptor<CachedLine> {
		private final BaseGLGraphicsContext context;

		private Dict properties;

		public PlainGL3dPointDefault(BaseGLGraphicsContext context) {
			this.context = context;
		}
		
		CoordinateFrame transform;

		public DrawingResult accept(List<iDynamicMesh> soFar, final CachedLine line, final Dict properties) {
			this.properties = properties;

			// System.out.println(" inside PlainGL3dPointDefault ---------- <"+properties+">");

			if (!properties.isTrue(iLinearGraphicsContext.pointed, false))
				return null;

			if (!properties.isTrue(iLinearGraphicsContext.containsDepth, false))
				return null;

			// System.out.println(" drawing in 3d ");

			float opacityMul = 1;
			Number o = line.getProperties().get(iLinearGraphicsContext.totalOpacity);
			opacityMul *= o == null ? 1 : o.floatValue();
			final float fopacityMul = opacityMul;

			if (fopacityMul == 0)
				return null;

			final DynamicPointlist outputLine = getCachedPoints(context);
			
			
			// final DynamicPointlist
			// outputLine =
			// DynamicPointlist.unshadedPoints(context.getVertexProgram());
			DrawingResult result = new DrawingResult(DrawingResultCode.cont, new iUpdateable() {
				Vector4 black = new Vector4(0, 0, 0, 1);

				public void update() {

					transform = line.getProperties().get(iLinearGraphicsContext.transform);
					
					// System.out.println(" inside point drawer for <"+line+">");

					if (!context.isLayer(line))
						return;

					CachedLineCursor cursor = new CachedLineCursor(line);

					outputLine.open();
					while (cursor.hasNextSegment()) {
						cursor.next();
						Vector3 v2 = new Vector3();
						cursor.getAt(v2);

						int v = outputLine.nextVertex(maybeTransform(transform, v2));
						Vector4 color = colorFor(cursor.getCurrent());
						outputLine.setAux(v, Base.color0_id, color.x, color.y, color.z, color.w * fopacityMul);
						Vector4 point = pointSizeFor(cursor.getCurrent());
						outputLine.setAux(v, 13, point.x, point.y, point.z, point.w);
					}
					outputLine.close();
				}

				private Vector4 colorFor(Event current) {
					Vector4 color = null;
					try {

						if (color == null)
							color = current.attributes != null ? current.attributes.get(iLinearGraphicsContext.pointColor_v) : null;
						if (color == null)
							color = current.attributes != null ? current.attributes.get(iLinearGraphicsContext.fillColor_v) : null;
						if (color == null)
							color = properties.get(iLinearGraphicsContext.pointColor);
						if (color == null)
							color = properties.get(iLinearGraphicsContext.color);
						if (color == null)
							color = properties.get(iLinearGraphicsContext.fillColor);
					} catch (Exception e) {
					}
					if (color == null)
						color = black;
					return color;
				}

				private Vector4 pointSizeFor(Event current) {
					Number size = null;
					if (size == null)
						size = properties.get(iLinearGraphicsContext.pointSize);
					if (size == null)
						size = current.getAttributes().getFloat(iLinearGraphicsContext.pointSize_v, 5f);
					if (size == null)
						size = 5f;

					Vector4 color = new Vector4(size.floatValue(), 0, 0, 0);
					return color;
				}

			}, outputLine);

			return result;
		}


	}

	static public boolean equals(CoordinateFrame a, CoordinateFrame b) {
		if (a==null) return b==null;
		if (b==null) return false;
		return a.equals(b);
	}

	static public Vector3 maybeTransform(CoordinateFrame transform, Vector3 v2) {
		if (transform==null) return v2;
		
		return transform.transformPosition(new Vector3(v2));
	}

	public class StrokedGLLinelDefault implements iDrawingAcceptor<CachedLine> {

		private final BaseGLGraphicsContext context;

		private Dict properties;

		public StrokedGLLinelDefault(BaseGLGraphicsContext context) {
			this.context = context;
		}

		public DrawingResult accept(List<iDynamicMesh> soFar, final CachedLine xline, final Dict properties) {

			this.properties = properties;
			float opacityMul = 1;

			Number o = xline.getProperties().get(iLinearGraphicsContext.totalOpacity);
			opacityMul *= o == null ? 1 : o.floatValue();
			final float fopacityMul = opacityMul;

			if (fopacityMul == 0)
				return null;

			if (!properties.isTrue(iLinearGraphicsContext.stroked, true))
				return null;

			BasicStroke stroke = properties.get(iLinearGraphicsContext.strokeType);
			if (stroke == null)
				return null;

			xline.finish();

			final CachedLine line = new LineUtils().lineAsStroked(xline, stroke, true);
			if (line == null)
				return null;

			final DynamicMesh_long outputLine = getCachedMesh(context, properties.isTrue(iLinearGraphicsContext.soloCache, false));

			final TessLineEmitter_long fillEmitter = new TessLineEmitter_long() {

				@Override
				protected void decorateVertex(int v1, List<Object> name) {
					Object color = name == null ? null : name.get(0);
					if (color == null)
						color = properties.get(iLinearGraphicsContext.strokeColor);
					if (color == null)
						color = properties.get(iLinearGraphicsContext.color);
					if (color == null)
						color = black;

					if (color instanceof float[])
						color = new Vector4(((float[]) color)[0], ((float[]) color)[1], ((float[]) color)[2], ((float[]) color)[3]);

					mesh.setAux(v1, Base.color0_id, ((Vector4) color).x, ((Vector4) color).y, ((Vector4) color).z, ((Vector4) color).w * fopacityMul);
				}

			};

			fillEmitter.addTrackedProperty(iLinearGraphicsContext.fillColor_v, new iMetric<Vector4, Vector4>() {
				public float distance(Vector4 from, Vector4 to) {
					if (from == null || to == null)
						return 0;

					float d = (from).distanceFrom((to)) * 1;
					return d;
				}
			});

			drawInto(line, properties, outputLine, context, fillEmitter);

			// System.out.println(" PLine system computed a fil of length <"+fillEmitter.mesh.getUnderlyingGeometry().numVertex()+" "+fillEmitter.mesh.getUnderlyingGeometry().numTriangle()+">");

			DrawingResult result = new DrawingResult(DrawingResultCode.cont, new iUpdateable() {
				public void update() {

					if (!context.isLayer(line))
						return;

					// copySource
					// Mesh
					outputLine.open();
					outputLine.copyFrom(fillEmitter.mesh);
					outputLine.close();

					// System.out.println(" PLine system copied a fil of length <"+fillEmitter.mesh.getUnderlyingGeometry().numVertex()+" "+fillEmitter.mesh.getUnderlyingGeometry().numTriangle()+">");

				}
			}, outputLine);

			return result;
		}
	}

	private BaseGLGraphicsContext context;

	public SimpleLineDrawing() {
	}

	static public void drawInto(CachedLine line, Dict properties, iDynamicMesh outputLine, BaseGLGraphicsContext context, SmallLineEmitter_long lineEmitter) {

		CachedLineCursor cursor = new CachedLineCursor(line);

		lineEmitter.globalGeometricScale = context == null ? 0.4f : context.getGlobalProperties().getFloat(iLinearGraphicsContext.geometricScale, 1f);
		lineEmitter.globalFlatnessScale = context == null ? 1f : context.getGlobalProperties().getFloat(iLinearGraphicsContext.flatnessScale, 1);

		line.finish();

		outputLine.open();
		Vector2 a = new Vector2();
		Vector2 b = new Vector2();
		Vector2 c1 = new Vector2();
		Vector2 c2 = new Vector2();

		lineEmitter.begin();
		while (cursor.hasNextSegment()) {
			if (outputLine instanceof DynamicLine)
				((DynamicLine) outputLine).startLine();
			if (outputLine instanceof DynamicLine_long)
				((DynamicLine_long) outputLine).startLine();
			if (outputLine instanceof SubLine)
				((SubLine) outputLine).getLine().startLine();
			if (outputLine instanceof SubLine_long)
				((SubLine_long) outputLine).getLine().startLine();
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
			if (outputLine instanceof DynamicLine_long)
				((DynamicLine_long) outputLine).endLine();
			if (outputLine instanceof SubLine)
				((SubLine) outputLine).getLine().endLine();
			if (outputLine instanceof SubLine_long)
				((SubLine_long) outputLine).getLine().endLine();
			lineEmitter.endContour();

			if (cursor.hasNextSegment())
				cursor.next();
		}
		lineEmitter.end();
		outputLine.close();
	}

	static Vector2 zOffset = new Vector2();

	static public iFunction<Vector3, Vector3> thisCamera;

	public class Plain3dLine implements iDrawingAcceptor<CachedLine> {

		private final BaseGLGraphicsContext context;

		private float fopacityMul;

		protected SubLine_long outputLine;

		float constantDistance = 0;

		List<Integer> extendedAttributes = null;
		
		CoordinateFrame transform = null;

		Small3dLineEmitter_long lineEmitter = new Small3dLineEmitter_long() {

			Vector3 lastOut = new Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);

			@Override
			public void begin() {
				lastOut = new Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
			}

			@Override
			public void emitLinearFrame(Vector3 a, Vector3 b, java.util.List<Object> name, java.util.List<Object> name2, Dict properties, iLinearGraphicsContext contex) {

				Vector3 c1 = camera(a);
				Vector3 c2 = camera(b);
				if (c1.distanceFrom(lastOut) > 1e-10) {
					int v1 = outputLine.nextVertex(maybeTransform(transform, c1));
					decorateVertex(v1, name, outputLine, properties);
				}
				int v2 = outputLine.nextVertex(maybeTransform(transform, c2));

				decorateVertex(v2, name2, outputLine, properties);
				lastOut = c2;
			}

			protected void decorateVertex(int v1, List<Object> name, SubLine_long outputLine, Dict properties) {

				Object color = name.get(1);
				if (color == null)
					color = properties.get(iLinearGraphicsContext.strokeColor);
				if (color == null)
					color = properties.get(iLinearGraphicsContext.color);
				if (color == null)
					color = black;

				try {
					outputLine.setAux(v1, Base.color0_id, ((Vector4) color).x, ((Vector4) color).y, ((Vector4) color).z, ((Vector4) color).w * fopacityMul);
				} catch (ClassCastException e) {
					e.printStackTrace();
				}
				// outputLine.setAux(v1, 13, 5, 1f, 5f, 0);

				if (extendedAttributes != null) {
					int q = 2;
					for (Integer ii : extendedAttributes) {
						Object c = name.get(q);
						if (c instanceof Vector4) {
							Vector4 cc = (Vector4) c;
							outputLine.setAux(v1, ii, cc.x, cc.y, cc.z, cc.w);
						}
						q++;
					}
				}
			}

			@Override
			protected boolean shouldTerm(float flatness, Vector3 a, Vector3 c1, Vector3 c2, Vector3 b, int n) {
				return constantDistance == 0 ? super.shouldTerm(flatness, a, c1, c2, b, n) : a.distanceFrom(b) < constantDistance;
			}
		};

		public Plain3dLine(BaseGLGraphicsContext context) {
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

		public DrawingResult accept(List<iDynamicMesh> soFar, final CachedLine line, final Dict properties) {
			if (!properties.isTrue(iLinearGraphicsContext.stroked, true))
				return null;

			if (!properties.isTrue(iLinearGraphicsContext.containsDepth, false))
				return null;

			if (properties.isTrue(iLinearGraphicsContext.strokeType, false))
				return null;

			constantDistance = properties.getFloat(iLinearGraphicsContext.constantDistanceResampling, 0);

			Map<String, Number> shaderAttributes = properties.get(iLinearGraphicsContext.shaderAttributes);

			if (shaderAttributes != null) {
				lineEmitter.clearTrackedProperties();
				lineEmitter.addTrackedProperty(iLinearGraphicsContext.strokeColor_v, new iMetric<Vector4, Vector4>() {
					public float distance(Vector4 from, Vector4 to) {
						if (from == null || to == null)
							return 0;
						float d = from.distanceFrom(to);
						return d;
					}
				});
				extendedAttributes = new ArrayList<Integer>();
				Set<Entry<String, Number>> es = shaderAttributes.entrySet();
				int q = 2;
				for (Entry<String, Number> e : es) {
					lineEmitter.addTrackedProperty(new Prop(e.getKey()), new iMetric() {
						public float distance(Object from, Object to) {
							return 0;
						}
					});
					extendedAttributes.add(e.getValue().intValue());
				}
			} else {
				extendedAttributes = null;
				lineEmitter.clearTrackedProperties();
				lineEmitter.addTrackedProperty(iLinearGraphicsContext.strokeColor_v, new iMetric<Vector4, Vector4>() {
					public float distance(Vector4 from, Vector4 to) {
						if (from == null || to == null)
							return 0;
						float d = from.distanceFrom(to);
						return d;
					}
				});
			}
			iFunction<Vector3, Vector3> camera = properties.get(iLinearGraphicsContext.camera);

			float opacityMul = 1;
			Number o = line.getProperties().get(iLinearGraphicsContext.totalOpacity);
			opacityMul *= o == null ? 1 : o.floatValue();
			fopacityMul = opacityMul;

			if (fopacityMul == 0)
				return null;

			final float fwidth = widthFor(context, properties);
			final boolean dosolo = properties.isTrue(iLinearGraphicsContext.soloCache, false);
			final DynamicLine_long outputLine = getCachedLineForWidth(context, fwidth, dosolo);
			final DynamicLine_long outputLineOld = getOldCachedLineForWidth(context, fwidth);

			transform = properties.get(iLinearGraphicsContext.transform);

			final SubLine_long subLine = new SubLine_long();
			Plain3dLine.this.outputLine = subLine;
			subLine.open();
			thisCamera = camera;
			drawInto(line, properties, subLine, context, lineEmitter);
			subLine.close();

			// System.out.println(" PLine system computed a line of length <"+subLine.getUnderlyingGeometry().numVertex()+" "+subLine.getUnderlyingGeometry().numTriangle()+">");

			DrawingResult result = new DrawingResult(DrawingResultCode.cont, new iUpdateable() {

				int seen = 0;

				CoordinateFrame prevTransform = transform==null ? null : transform.duplicate();
				
				public void update() {

					transform = line.getProperties().get(iLinearGraphicsContext.transform);

					DynamicLine_long x = (dosolo ? outputLine : (seen > 2 ? outputLineOld : outputLine));

					if (!context.isLayer(line))
						return;

					if (!SimpleLineDrawing.equals(prevTransform, transform))
					{
						prevTransform = transform==null ? null : transform.duplicate();
						Plain3dLine.this.outputLine = subLine;
						subLine.open();
						drawInto(line, properties, subLine, context, lineEmitter);
						subLine.close();
					}
					
					
					x.open();
					x.copyFrom(subLine);
					x.close();

					// System.out.println(" PLine system copied a line of length <"+subLine.getUnderlyingGeometry().numVertex()+" "+subLine.getUnderlyingGeometry().numTriangle()+">");

					Dict p = line.getProperties();
					CoordinateFrame f = p.get(iLinearGraphicsContext.fastTransform);
					if (f != null) {
						x.getUnderlyingGeometry().setCoordindateFrameProvider(f);
					}

					boolean usesAdjacencyForLines = context.getGlobalProperties().isTrue(iLinearGraphicsContext.usesAdjacency, false);
					if (usesAdjacencyForLines)
						outputLine.getUnderlyingGeometry().sendAdjacency();

					if (subLine.getCopy() == null)
						subLine.makeCopy();

					seen++;
				}
			}, outputLine);

			return result;
		}

	}

	public DynamicLine_long getCachedLineForWidth(BaseGLGraphicsContext context, float f) {
		if (f < 0)
			f = 1;
		if (f > 100)
			f = 100;
		iDynamicMesh already = context.allreadyConstructedLines.get(f);
		if (already != null) {
			ensureParent(context, already);
			return (DynamicLine_long) already;
		}
		DynamicLine_long nl = DynamicLine_long.unshadedLine(context.getVertexProgram(), f);
		context.allreadyConstructedLines.put(f, nl);
		nl.open();

		nl.getUnderlyingGeometry().doFakeAntialias(doFakeAa);

		return nl;
	}

	public DynamicLine_long getCachedLineForWidth(BaseGLGraphicsContext context, float f, boolean mustbeuniq) {
		if (mustbeuniq) {
			DynamicLine_long nl = DynamicLine_long.unshadedLine(context.getVertexProgram(), f);
			context.allreadyConstructedLines.put(uniqueFor(context.allreadyConstructedLines), nl);
			nl.open();
			nl.getUnderlyingGeometry().doFakeAntialias(doFakeAa);
			return nl;
		}
		if (f < 0)
			f = 1;
		if (f > 100)
			f = 100;
		iDynamicMesh already = context.allreadyConstructedLines.get(f);
		if (already != null) {
			ensureParent(context, already);
			return (DynamicLine_long) already;
		}
		DynamicLine_long nl = DynamicLine_long.unshadedLine(context.getVertexProgram(), f);
		context.allreadyConstructedLines.put(f, nl);
		nl.open();

		nl.getUnderlyingGeometry().doFakeAntialias(doFakeAa);

		return nl;
	}

	static public int cacheUniq = 0;

	private Float uniqueFor(BiMap<Float, iDynamicMesh> allreadyConstructedLines) {
		float f = (float) Math.random() + cacheUniq;
		while (allreadyConstructedLines.containsKey(f + cacheUniq)) {
			cacheUniq++;
			f = (float) Math.random();
		}
		return f + cacheUniq;
	}

	public DynamicLine_long getOldCachedLineForWidth(BaseGLGraphicsContext context, float f) {
		if (f < 0)
			f = 1;
		if (f > 100)
			f = 100;

		f += 200;

		iDynamicMesh already = context.allreadyConstructedLines.get(f);
		if (already != null) {
			ensureParent(context, already);
			return (DynamicLine_long) already;
		}
		DynamicLine_long nl = DynamicLine_long.unshadedLine(context.getVertexProgram(), f - 200);
		context.allreadyConstructedLines.put(f, nl);
		nl.open();

		nl.getUnderlyingGeometry().doFakeAntialias(doFakeAa);

		return nl;
	}

	public DynamicMesh_long getCachedMesh(BaseGLGraphicsContext context, boolean mustBeUniq) {
		float f = -2;
		if (!mustBeUniq) {
			iDynamicMesh already = context.allreadyConstructedLines.get(f);
			if (already != null) {
				ensureParent(context, already);
				return (DynamicMesh_long) already;
			}
		}
		DynamicMesh_long nl = DynamicMesh_long.unshadedMesh();
		context.getVertexProgram().addChild(nl.getUnderlyingGeometry());
		if (!mustBeUniq) {
			context.allreadyConstructedLines.put(f, nl);
		} else {
			// FIXME
			context.allreadyConstructedLines.put(-(float) Math.random(), nl);
		}
		nl.open();
		return nl;
	}

	public DynamicMesh_long getCachedMeshLate(BaseGLGraphicsContext context) {
		float f = -2;
		DynamicMesh_long nl = DynamicMesh_long.unshadedMesh(StandardPass.postRender);
		context.getVertexProgram().addChild(nl.getUnderlyingGeometry());

		nl.getUnderlyingGeometry().addChild(new EnableDepthTestWrap());

		System.out.println(" new cached mesh late <" + nl.getUnderlyingGeometry() + ">");

		// FIXME
		context.allreadyConstructedLines.put(-(float) Math.random(), nl);

		nl.open();
		return nl;
	}

	public DynamicMesh_long getOldCachedMesh(BaseGLGraphicsContext context, boolean mustBeUniq) {
		float f = -3;
		if (!mustBeUniq) {
			iDynamicMesh already = context.allreadyConstructedLines.get(f);
			if (already != null) {
				ensureParent(context, already);
				return (DynamicMesh_long) already;
			}
		}
		DynamicMesh_long nl = DynamicMesh_long.unshadedMesh();
		context.getVertexProgram().addChild(nl.getUnderlyingGeometry());
		if (!mustBeUniq) {
			context.allreadyConstructedLines.put(f, nl);
		} else {
			// FIXME
			context.allreadyConstructedLines.put(-(float) Math.random() - 1, nl);
		}
		nl.open();
		return nl;
	}

	public DynamicPointlist getCachedPoints(BaseGLGraphicsContext context) {
		float f = -1;
		iDynamicMesh already = context.allreadyConstructedLines.get(f);
		if (already != null) {
			ensureParent(context, already);
			return (DynamicPointlist) already;
		}
		DynamicPointlist nl = DynamicPointlist.unshadedPoints(context.getVertexProgram());
		context.allreadyConstructedLines.put(f, nl);
		nl.open();
		return nl;
	}

	public SimpleLineDrawing installInto(BaseGLGraphicsContext context) {

		this.context = context;
		context.addAcceptor(new ObjectFastHighlight(context));
		context.addAcceptor(new StrokedGLLinelDefault(context));

		context.addAcceptor(new PlainGLFillDefault(context));
		context.addAcceptor(new PlainGL3dFillDefault(context));

		context.addAcceptor(new PlainGLPointDefault(context));
		context.addAcceptor(new PlainGL3dPointDefault(context));

		context.addAcceptor(new Plain3dLine(context));
		context.addAcceptor(new PlainGLLineDefault(context));

		return this;
	}

	public float widthFor(BaseGLGraphicsContext context, Dict properties) {
		Number f = properties.get(iLinearGraphicsContext.thickness);
		Number m = context.getGlobalProperties().get(iLinearGraphicsContext.strokeThicknessMul);

		return (f == null ? 1 : f.floatValue()) * (m == null ? 1 : m.floatValue());
	}

	private void ensureParent(BaseGLGraphicsContext context, iDynamicMesh already) {
		if (!context.getVertexProgram().isChild(already.getUnderlyingGeometry())) {
			context.getVertexProgram().addChild(already.getUnderlyingGeometry());
		}
	}

	public void drawInto(CachedLine line, Dict properties, iDynamicMesh outputLine, BaseGLGraphicsContext context, Small3dLineEmitter_long lineEmitter) {

		CachedLineCursor cursor = new CachedLineCursor(line);

		lineEmitter.globalGeometricScale = context.getGlobalProperties().getFloat(iLinearGraphicsContext.geometricScale, 1f);
		lineEmitter.globalFlatnessScale = context.getGlobalProperties().getFloat(iLinearGraphicsContext.flatnessScale, 1);

		boolean usesAdjacencyForLines = context.getGlobalProperties().isTrue(iLinearGraphicsContext.usesAdjacency, false);

		line.finish();

		outputLine.open();
		Vector2 a = new Vector2();
		Vector2 b = new Vector2();
		Vector2 c1 = new Vector2();
		Vector2 c2 = new Vector2();

		lineEmitter.begin();
		while (cursor.hasNextSegment()) {
			if (outputLine instanceof DynamicLine)
				((DynamicLine) outputLine).startLine();

			if (outputLine instanceof DynamicLine_long)
				((DynamicLine_long) outputLine).startLine();

			if (outputLine instanceof SubLine)
				((SubLine) outputLine).getLine().startLine();
			if (outputLine instanceof SubLine_long)
				((SubLine_long) outputLine).getLine().startLine();
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
				if (usesAdjacencyForLines)
					((DynamicLine) outputLine).endLineWithAdjacency();
				else
					((DynamicLine) outputLine).endLine();

			if (outputLine instanceof DynamicLine_long)
				if (usesAdjacencyForLines)
					((DynamicLine_long) outputLine).endLineWithAdjacency();
				else
					((DynamicLine_long) outputLine).endLine();
			if (outputLine instanceof SubLine)
				if (usesAdjacencyForLines)
					((SubLine) outputLine).getLine().endLineWithAdjacency();
				else
					((SubLine) outputLine).getLine().endLine();
			if (outputLine instanceof SubLine_long)
				if (usesAdjacencyForLines)
					((SubLine_long) outputLine).getLine().endLineWithAdjacency();
				else
					((SubLine_long) outputLine).getLine().endLine();
			lineEmitter.endContour();

			if (cursor.hasNextSegment())
				cursor.next();
		}

		if (usesAdjacencyForLines) {
			if (outputLine instanceof DynamicLine)
				((DynamicLine) outputLine).getUnderlyingGeometry().sendAdjacency();
			if (outputLine instanceof DynamicLine_long)
				((DynamicLine_long) outputLine).getUnderlyingGeometry().sendAdjacency();
			if (outputLine instanceof SubLine)
				((SubLine) outputLine).getLine().getUnderlyingGeometry().sendAdjacency();
			if (outputLine instanceof SubLine_long)
				((SubLine_long) outputLine).getLine().getUnderlyingGeometry().sendAdjacency();
		}

		lineEmitter.end();
		outputLine.close();
	}

	static public Vector3 camera(Vector3 a) {
		if (thisCamera != null) {
			return thisCamera.f(a);
		}
		return new Vector3(a.x + zOffset.x * a.z, a.y + zOffset.y * a.z, a.z);
	}
}
