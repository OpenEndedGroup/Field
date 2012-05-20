package field.graphics.ci;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Shell;
import org.lwjgl.opengl.GL11;

import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext.DrawingResult;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext.DrawingResultCode;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext.iDrawingAcceptor;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.util.PythonCallableMap;
import field.core.windowing.GLComponentWindow;
import field.graphics.ci.CoreImageCanvasUtils.Accumulator;
import field.graphics.ci.CoreImageCanvasUtils.Destination;
import field.graphics.ci.CoreImageCanvasUtils.Filter;
import field.graphics.ci.CoreImageCanvasUtils.Image;
import field.graphics.core.Base.StandardPass;
import field.graphics.core.BasicContextManager;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.When;
import field.graphics.dynamic.iDynamicMesh;
import field.launch.iUpdateable;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Pair;
import field.util.Dict;
import field.util.Dict.Prop;
import field.util.TaskQueue;

public class DeferredImageDrawing {

	public static final Prop<Boolean> containsDeferredImages = new Prop<Boolean>("containsDeferredImages");
	public static final Prop<Vector3> image3d_right = new Prop<Vector3>("image3d_right_v");
	public static final Prop<Vector3> image3d_up = new Prop<Vector3>("image3d_up_v");

	public enum Thumbnail {
		notLoaded(0), full(1), half(0.5f), quarter(0.25f), eighth(0.125f);

		private float scale;

		Thumbnail(float scale) {
			this.scale = scale;
		}

		public float getScale() {
			return scale;
		}
	}

	static public class ImageReference {
		final public String filename;

		public ImageReference(String filename) {
			this.filename = filename;
		}
		
		public ImageReference(String filename, Image master) {
			this.filename = filename;
			this.master = master;
			this.extents = master.getExtents();
		}
		

		Image master = null;
		boolean uploaded = false;
		Rect extents;

		Image half = null;
		Image quarter = null;
		Image eighth = null;

		Destination destination = null;

		Thumbnail lastDrawn = Thumbnail.notLoaded;

		@Override
		public String toString() {
			return "IR:" + filename + "(" + master + ")";
		}

		public Image getMasterImage(final CoreImageCanvasUtils utils) {
			lastDrawn = Thumbnail.full;
			if (master != null)
				return master;

			final GLComponentWindow m = GLComponentWindow.getCurrentWindow(null);

			workerThread.new Task() {

				@Override
				protected void run() {

					if (master != null)
						return;

					master = utils.new Image(filename);
					extents = master.getExtents();
					uploaded = false;

					// force evaluation of this thing?

					;//System.out.println(" requesting repaint <" + filename + "> is now available <" + master + ">");

					if (m != null) {
						m.requestRepaint();
					}

				}
			};

			return null;
		}

		public Destination getDestination(CoreImageCanvasUtils u) {
			if (destination == null) {
				if (master == null)
					getMasterImage(u);

				if (master == null)
					return null;

				destination = u.new Destination((int) extents.w, (int) extents.h);
				destination.moveCIImageToHere(master, (int)extents.x, (int)extents.y, (int) extents.w, (int) extents.h);
			}
			return destination;
		}

		public Thumbnail getLastDrawn() {
			return lastDrawn;
		}

		public Image getHalfImage(final CoreImageCanvasUtils utils) {
			lastDrawn = Thumbnail.half;

			if (half != null)
				return half;

			if (master != null) {

				final GLComponentWindow m = GLComponentWindow.getCurrentWindow(null);

				workerThread.new Task() {

					@Override
					protected void run() {

						if (half != null)
							return;

						Filter f = utils.new Filter("CILanczosScaleTransform");
						f.set("inputImage", master);
						f.set("inputScale", 0.5f);
						f.set("inputAspectRatio", 1.0f);
						half = f.getImage("outputImage");

						// force evaluation of this
						// thing?

						;//System.out.println(" requesting repaint <" + filename + "> is now available @ half <" + half + ">");

						if (m != null) {
							m.requestRepaint();
						}

					}
				};

			} else {

				final GLComponentWindow m = GLComponentWindow.getCurrentWindow(null);

				workerThread.new Task() {

					@Override
					protected void run() {
						if (half != null)
							return;

						if (master == null)
							master = utils.new Image(filename);

						Filter f = utils.new Filter("CILanczosScaleTransform");
						f.set("inputImage", master);
						f.set("inputScale", 0.5f);
						f.set("inputAspectRatio", 1.0f);
						half = f.getImage("outputImage");

						extents = master.getExtents();
						uploaded = false;

						// force evaluation of this
						// thing?

						;//System.out.println(" requesting repaint <" + filename + "> is now available @ half <" + half + ">");

						if (m != null) {
							m.requestRepaint();
						}

					}
				};
			}
			return null;
		}

		public Image getQuarterImage(final CoreImageCanvasUtils utils) {
			lastDrawn = Thumbnail.quarter;

			if (quarter != null)
				return quarter;

			if (master != null) {

				final GLComponentWindow m = GLComponentWindow.getCurrentWindow(null);

				workerThread.new Task() {

					@Override
					protected void run() {

						if (quarter != null)
							return;

						Filter f = utils.new Filter("CILanczosScaleTransform");
						f.set("inputImage", master);
						f.set("inputScale", 0.25f);
						f.set("inputAspectRatio", 1.0f);
						quarter = f.getImage("outputImage");

						// force evaluation of this
						// thing?

						;//System.out.println(" requesting repaint <" + filename + "> is now available @ half <" + half + ">");

						if (m != null) {
							m.requestRepaint();
						}

					}
				};

			} else {

				final GLComponentWindow m = GLComponentWindow.getCurrentWindow(null);

				workerThread.new Task() {

					@Override
					protected void run() {
						if (quarter != null)
							return;

						if (master == null)
							master = utils.new Image(filename);

						Filter f = utils.new Filter("CILanczosScaleTransform");
						f.set("inputImage", master);
						f.set("inputScale", 0.25f);
						f.set("inputAspectRatio", 1.0f);
						quarter = f.getImage("outputImage");

						extents = master.getExtents();
						uploaded = false;

						// force evaluation of this
						// thing?

						;//System.out.println(" requesting repaint <" + filename + "> is now available @ half <" + half + ">");

						if (m != null) {
							m.requestRepaint();
						}

					}
				};
			}
			return null;
		}

		public Image getEighthImage(final CoreImageCanvasUtils utils) {
			lastDrawn = Thumbnail.eighth;

			if (eighth != null)
				return eighth;

			if (master != null) {

				final GLComponentWindow m = GLComponentWindow.getCurrentWindow(null);

				workerThread.new Task() {

					@Override
					protected void run() {

						if (eighth != null)
							return;

						Filter f = utils.new Filter("CILanczosScaleTransform");
						f.set("inputImage", master);
						f.set("inputScale", 0.125f);
						f.set("inputAspectRatio", 1.0f);
						eighth = f.getImage("outputImage");

						// force evaluation of this
						// thing?

						;//System.out.println(" requesting repaint <" + filename + "> is now available @ eighth <" + half + ">");

						if (m != null) {
							m.requestRepaint();
						}

					}
				};

			} else {

				final GLComponentWindow m = GLComponentWindow.getCurrentWindow(null);

				workerThread.new Task() {

					@Override
					protected void run() {
						if (eighth != null)
							return;

						if (master == null)
							master = utils.new Image(filename);

						Filter f = utils.new Filter("CILanczosScaleTransform");
						f.set("inputImage", master);
						f.set("inputScale", 0.125f);
						f.set("inputAspectRatio", 1.0f);
						eighth = f.getImage("outputImage");

						extents = master.getExtents();
						uploaded = false;

						// force evaluation of this
						// thing?

						;//System.out.println(" requesting repaint <" + filename + "> is now available @ eighth <" + half + ">");

						if (m != null) {
							m.requestRepaint();
						}

					}
				};
			}
			return null;
		}

		public Pair<Image, Float> getScaleOptimalImage(CoreImageCanvasUtils u) {
			
			
			if (true)
			{
				return new Pair<Image, Float>(getMasterImage(u), 1f);
			}
			
			GLComponentWindow m = GLComponentWindow.getCurrentWindow(null);

			float x = 1 / m.getXScale();

			float previousScale = lastDrawn.getScale();

			;//System.out.println(" computing optimal scale for <" + x + " " + lastDrawn + ">");

			if (previousScale > x * 0.75 && previousScale < x * 1.5) {

				;//System.out.println(" accepted last drawn ");

				if (lastDrawn == Thumbnail.full)
					return new Pair<Image, Float>(getMasterImage(u), 1f);
				else if (lastDrawn == Thumbnail.half)
					return new Pair<Image, Float>(getHalfImage(u), 0.5f);
				else if (lastDrawn == Thumbnail.quarter)
					return new Pair<Image, Float>(getQuarterImage(u), 0.25f);
				else if (lastDrawn == Thumbnail.eighth)
					return new Pair<Image, Float>(getEighthImage(u), 0.125f);
				return null;
			}

			Thumbnail lower = null;
			for (Thumbnail t : Thumbnail.values()) {
				if (t.scale <= x && t != Thumbnail.notLoaded) {
					lower = t;
					break;
				}
			}

			;//System.out.println(" chose <" + lower + ">");

			if (lower == null)
				lower = Thumbnail.eighth;

			if (lower == Thumbnail.full) {
				if (master != null)
					return new Pair<Image, Float>(getMasterImage(u), 1f);
				else {
					new Pair<Image, Float>(getMasterImage(u), 1f);
					if (half != null)
						return new Pair<Image, Float>(getHalfImage(u), 0.5f);
					else if (quarter != null)
						return new Pair<Image, Float>(getQuarterImage(u), 0.25f);
					else if (eighth != null)
						return new Pair<Image, Float>(getEighthImage(u), 0.125f);
					return new Pair<Image, Float>(getMasterImage(u), 1f);
				}
			} else if (lower == Thumbnail.half) {
				if (half != null)
					return new Pair<Image, Float>(getHalfImage(u), 0.5f);
				else {
					new Pair<Image, Float>(getHalfImage(u), 0.5f);

					if (quarter != null)
						return new Pair<Image, Float>(getQuarterImage(u), 0.25f);
					else if (eighth != null)
						return new Pair<Image, Float>(getEighthImage(u), 0.125f);

					return new Pair<Image, Float>(getMasterImage(u), 1f);

				}
			} else if (lower == Thumbnail.quarter) {
				if (quarter != null)
					return new Pair<Image, Float>(getQuarterImage(u), 0.25f);
				else {
					new Pair<Image, Float>(getQuarterImage(u), 0.25f);
					if (eighth != null)
						return new Pair<Image, Float>(getEighthImage(u), 0.125f);
					if (half != null)
						return new Pair<Image, Float>(getHalfImage(u), 0.5f);
					return new Pair<Image, Float>(getQuarterImage(u), 1f);

				}
			} else if (lower == Thumbnail.eighth) {
				if (eighth != null)
					return new Pair<Image, Float>(getEighthImage(u), 0.125f);
				else {
					new Pair<Image, Float>(getEighthImage(u), 0.125f);
					if (quarter != null)
						return new Pair<Image, Float>(getQuarterImage(u), 0.25f);
					if (half != null)
						return new Pair<Image, Float>(getHalfImage(u), 0.5f);

					return new Pair<Image, Float>(getEighthImage(u), 1f);

				}

			}
			return null;

		}

	}

	static TaskQueue workerThread = new TaskQueue();
	static public Object workerLock = new Object();

	static {
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						synchronized (workerLock) {
							if (workerThread.getNumTasks() > 0)
								;//System.out.println(" deferred image worker thread has <" + workerThread.getNumTasks() + "> tasks ");
							workerThread.update();
						}

						Thread.sleep(500);

					} catch (Throwable t) {
						System.err.println("exceptioin <" + t + "> inside deferredimagedrawing worker thread ------------------------------------");
						t.printStackTrace();
					}
				}
			}
		}).start();
	}

	// used for notification of images having been loaded
	TaskQueue drawingQueue = new TaskQueue();

	private CoreImageCanvasUtils utils;

	public class DrawsImage implements iDrawingAcceptor<CachedLine> {
		private final BaseGLGraphicsContext context;

		public DrawsImage(BaseGLGraphicsContext context) {
			this.context = context;
		}

		public DrawingResult accept(List<iDynamicMesh> soFar, final CachedLine line, final Dict properties) {
			if (!properties.isTrue(containsDeferredImages, false))
				return null;

			DrawingResult result = new DrawingResult(DrawingResultCode.cont, new iUpdateable() {
				private Vector4 black = new Vector4(1, 1, 1, 1f);

				public void update() {

					for (final Event e : line.events) {
						if (e.attributes != null && e.attributes.get(iLinearGraphicsContext.image_v) != null) {
							final float opacityMul;
							Number o = line.getProperties().get(iLinearGraphicsContext.totalOpacity);
							opacityMul = o == null ? 1 : o.floatValue();

							When w = ((BasicGLSLangProgram) context.getVertexProgram()).getWhen();
							final PythonCallableMap m = w.getMap(StandardPass.render);

							m.register("o" + System.identityHashCode(e), new iUpdateable() {

								@Override
								public void update() {

									;//System.out.println(" inside drawing result callback for defered image ");
									BasicGLSLangProgram.currentProgram.debugPrintUniforms();
									
									
									Vector2 a = e.getDestination();
									float z = 0;
									Object zr = e.getDict().get(iLinearGraphicsContext.z_v);
									if (zr instanceof Number)
										z = ((Number) zr).floatValue();
									else if (zr instanceof Vector3)
										z = ((Vector3) zr).x;

									if (zr != null)
										render(a.x, a.y, z, e.attributes.get(iLinearGraphicsContext.image_v), colorFor(e), opacityMul, e.attributes.get(iLinearGraphicsContext.imageDrawScale_v), e.attributes.get(image3d_right), e.attributes.get(image3d_up));
									else
										render(a, e.attributes.get(iLinearGraphicsContext.image_v), colorFor(e), opacityMul, e.attributes.get(iLinearGraphicsContext.imageDrawScale_v));

									Object gl = BasicContextManager.getGl();
									int[] side = {0}; 
									side[0] = GL11.glGetInteger(GL11.GL_DRAW_BUFFER);
																		
									if (side[0] == GL11.GL_BACK || side[0] == GL11.GL_BACK_RIGHT)
										m.remove();
								}

							});
						}
					}
				}

				private void render(float x, float y, float z, Object image, Vector4 colorFor, float opacityMul, Number number, Vector3 right, Vector3 up) {

					;//System.out.println(" inside deferred rendering system ");

					if (image instanceof ImageReference) {

						ImageReference ir = ((ImageReference) image);

						Destination dest = ir.getDestination(utils);

						if (right==null)
							right = new Vector3(1,0,0);
						if (up==null)
							up= new Vector3(0,1,0);
						
						if (dest != null) {
							;//System.out.println(" draw into canvas now in 3d");

							Rect e = ir.extents;

							dest.drawIntoCanvasNow(new Vector3(x, y, z).add(right, (float) -e.w * 0.5f).add(up, (float) -e.h * 0.5f), new Vector3(x, y, z).add(right, (float) e.w * 0.5f).add(up, (float) -e.h * 0.5f), new Vector3(x, y, z).add(right, (float) e.w * 0.5f).add(up, (float) e.h * 0.5f), new Vector3(x, y, z).add(right, (float) -e.w * 0.5f).add(up, (float) e.h * 0.5f), new Vector4(1, 1, 1, opacityMul));

						} else {
							;//System.out.println(" deferred rendering system \u2014 missing image <" + ir + ">");
						}

					}
				}

				private void render(Vector2 destination, Object image, Vector4 colorFor, float opacityMul, Number v) {

					;//System.out.println(" inside deferred rendering system ");

					if (image instanceof ImageReference) {

						ImageReference ir = ((ImageReference) image);

						Rect e = ir.extents;
						if (e != null) {
							if (shouldCull(destination, e))
								return;
						}

						if (shouldCull(destination))
							return;

						Pair<Image, Float> image2 = ir.getScaleOptimalImage(utils);

						if (image2.left != null) {
							if (shouldCull(destination, ir.extents))
								return;
							;//System.out.println(" rendering image with scale <" + 1 / image2.right + ">");
							((Image) image2.left).drawNow(destination.x, destination.y, 1 / image2.right);
							ir.uploaded = true;
						} else {
							;//System.out.println(" deferred rendering system \u2014 missing image <" + ir + ">");
						}

					} else if (image instanceof Image) {

						Rect e = ((Image) image).getExtents();
						if (shouldCull(destination, e))
							return;

						if (v == null)
							((Image) image).drawNow(destination.x, destination.y);
						else
							((Image) image).drawNow(destination.x, destination.y, v.floatValue());
					} else if (image instanceof Accumulator) {
						if (v == null)
							((Accumulator) image).getOutputImage().drawNow(destination.x, destination.y);
						else
							((Accumulator) image).getOutputImage().drawNow(destination.x, destination.y, v.floatValue());
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

		float max_size = 3000;

		protected boolean shouldCull(Vector2 destination) {
			Vector2 topLeft = new Vector2(destination.x, destination.y);
			Vector2 bottomRight = new Vector2(topLeft.x + max_size, topLeft.y + max_size);
			GLComponentWindow.getCurrentWindow(null).transformDrawingToWindow(topLeft);
			GLComponentWindow.getCurrentWindow(null).transformDrawingToWindow(bottomRight);
			Shell f = GLComponentWindow.getCurrentWindow(null).getFrame();

			if (topLeft.x < f.getSize().x && topLeft.y < f.getSize().y && bottomRight.x > 0 && bottomRight.y > 0)
				return false;

			return true;
		}

		protected boolean shouldCull(Vector2 destination, Rect e) {

			if (e == null)
				return true;

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

	public void installInto(BaseGLGraphicsContext installedContext, CoreImageCanvasUtils utils) {
		this.utils = utils;
		installedContext.addAcceptor(new DrawsImage(installedContext));
	}
}
