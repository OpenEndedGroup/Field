package field.core.plugins.drawing.opengl;

import static org.lwjgl.opengl.GL11.GL_CLAMP;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;

import java.util.List;

import org.eclipse.swt.widgets.Event;

import field.core.plugins.drawing.opengl.BaseGLGraphicsContext.DrawingResult;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext.DrawingResultCode;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext.iDrawingAcceptor;
import field.core.plugins.drawing.opengl.LineInteraction.EventHandler;
import field.graphics.core.AdvancedTextures.BaseSlowRawTexture;
import field.graphics.core.Base;
import field.graphics.core.Base.iAcceptsSceneListElement;
import field.graphics.core.BasicCamera;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.dynamic.DynamicMesh;
import field.graphics.dynamic.iDynamicMesh;
import field.launch.iUpdateable;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.namespace.generic.Bind.iFunction;
import field.util.Dict;
import field.util.Dict.Prop;

public class SimpleWebpageDrawing {

	public static final Prop<String> browser_url = new Prop<String>("browser_url");
	public static final Prop<TextureBackedWebBrowser> browser = new Prop<TextureBackedWebBrowser>("browser");
	public static final Prop<iFunction<Vector3, Vector2>> browser_pageToSpace = new Prop<iFunction<Vector3, Vector2>>("browser_pageToSpace");
	public static final Prop<Vector2> browser_dimensions = new Prop<Vector2>("browser_dimensions");

	final DynamicMesh mesh = DynamicMesh.unshadedMesh();
	private BaseGLGraphicsContext installedContext;
	private iUpdateable refreshHandle;
	private boolean useRect;

	public class DrawsBrowser implements iDrawingAcceptor<CachedLine> {
		private final BaseGLGraphicsContext context;

		public DrawsBrowser(BaseGLGraphicsContext context) {
			this.context = context;
		}

		public DrawingResult accept(List<iDynamicMesh> soFar, final CachedLine line, final Dict properties) {
			String b = properties.get(browser_url);
			if (b == null)
				return null;

			if (line.events.size() < 3)
				return null;

			final TextureBackedWebBrowser[] browser = { null };

			DrawingResult result = new DrawingResult(DrawingResultCode.cont, new iUpdateable() {

				String lastURL = null;
				boolean first = true;

				@Override
				public void update() {
					Vector4 color = line.getProperties().get(iLinearGraphicsContext.fillColor);
					if (color==null)
						color =  line.getProperties().get(iLinearGraphicsContext.color);
					if (color==null)
						color =  new Vector4(1,1,1,1);
					
					float opacityMul = 1f;
					Number o = line.getProperties().get(iLinearGraphicsContext.totalOpacity);
					opacityMul *= o == null ? 1 : o.floatValue();

					color.w *=opacityMul;
					
					String url = properties.get(browser_url);
					Vector2 dim = properties.get(browser_dimensions);
					if (dim == null)
						dim = new Vector2(500, 1000);

					final Vector3 v = line.events.get(0).getDestination3();
					final Vector3 right = new Vector3(line.events.get(1).getDestination3()).sub(v);
					final Vector3 down = new Vector3(line.events.get(2).getDestination3()).sub(line.events.get(1).getDestination3());

					TextureBackedWebBrowser alreadyMade = line.getProperties().get(SimpleWebpageDrawing.browser);
					if (alreadyMade!=null && alreadyMade.getURL()!=null)
					{
						browser[0] = alreadyMade;
						lastURL = browser[0].getURL();
					}
					
					if (browser[0] == null) {
						browser[0] = new TextureBackedWebBrowser((int) dim.x, (int) dim.y) {
							@Override
							protected void notifyUpdate() {
								SimpleWebpageDrawing.this.notifyUpdate();
							}
						};
						browser[0].getTexture().use_gl_texture_rectangle_ext(false & useRect);
						if (false & useRect) {
							browser[0].getTexture().genMip = false;
						}
					}
					
					;//System.out.println(" // last url :"+lastURL+" "+url);
					
					if (url != null && (lastURL == null || !lastURL.equals(url))) {
						
						;//System.out.println(" _-- browser set url to be <"+url+">");
						
						browser[0].setURL(url);
						lastURL = url;

						CachedLine oc = new CachedLine();
						oc.getInput().moveTo(v.x, v.y);
						oc.getInput().setPointAttribute(iLinearGraphicsContext.z_v, v.z);
						oc.getInput().lineTo(v.x + right.x, v.y + right.y);
						oc.getInput().setPointAttribute(iLinearGraphicsContext.z_v, v.z + right.z);
						oc.getInput().lineTo(v.x + right.x + down.x, v.y + right.y + down.y);
						oc.getInput().setPointAttribute(iLinearGraphicsContext.z_v, v.z + right.z + down.z);
						oc.getInput().lineTo(v.x + down.x, v.y + down.y);
						oc.getInput().setPointAttribute(iLinearGraphicsContext.z_v, v.z + down.z);
						oc.getInput().lineTo(v.x, v.y);
						oc.getInput().setPointAttribute(iLinearGraphicsContext.z_v, v.z);

						line.getProperties().put(LineInteraction3d.areaForEventHandler, oc);

						final BasicCamera camera = BasicCamera.currentCamera;

						if (camera != null) {
							EventHandler handler = new EventHandler() {
								@Override
								public boolean up(Event e) {

									rewriteEvent(properties, v, right, down, camera, e);

									return super.up(e);
								}

								@Override
								public boolean down(Event e) {

									rewriteEvent(properties, v, right, down, camera, e);

									browser[0].doClick(e);

									return super.up(e);
								}

								@Override
								public boolean scroll(Event e) {

									;//System.out.println(" scroll != " + e.count);

									browser[0].scroll(0, -e.count * 4.0f);

									return super.scroll(e);
								}
							};

							if (line.getProperties().get(LineInteraction3d.eventHandler) == null) {
								line.getProperties().put(LineInteraction3d.eventHandler, handler);
							}
						}
						else
						{
							EventHandler handler = new EventHandler() {
								@Override
								public boolean up(Event e) {

//									rewriteEvent(properties, v, right, down, camera, e);
									;//System.out.println(" up :"+e.x+" "+e.y);

									return super.up(e);
								}

								@Override
								public boolean down(Event e) {

//									rewriteEvent(properties, v, right, down, camera, e);

									Vector2 dim = properties.get(browser_dimensions);
									if (dim == null)
										dim = new Vector2(500, 1000);

									float dx = new Vector2(e.x-v.x, e.y-v.y).dot(right.toVector2().normalize()) * (dim.x/right.mag());
									float dy = new Vector2(e.x-v.x, e.y-v.y).dot(down.toVector2().normalize()) * (dim.y/down.mag());
									
									e.x = (int) dx;
									e.y = (int)dy;
									;//System.out.println(" down :"+e.x+" "+e.y);
									browser[0].doClick(e);

									return super.down(e);
								}

								@Override
								public boolean scroll(Event e) {

									;//System.out.println(" scroll != " + e.count);

									browser[0].scroll(0, -e.count * 4.0f);

									return super.scroll(e);
								}
							};

							if (line.getProperties().get(LineInteraction.eventHandler) == null) {
								line.getProperties().put(LineInteraction.eventHandler, handler);
							}
							
						}
					}

					line.getProperties().put(SimpleWebpageDrawing.browser, browser[0]);
					line.getProperties().put(browser_pageToSpace, newPageToSpace(v, right, down, dim));

					iAcceptsSceneListElement p = context.getVertexProgram();
					BasicGLSLangProgram was = null;
					;//System.out.println(" -- drawing mesh for texture backed webpage -- ");

					if (!useRect && p instanceof BasicGLSLangProgram && p != BasicGLSLangProgram.currentProgram) {
						was = BasicGLSLangProgram.currentProgram;

						;//System.out.println(" forcing shade on");
						;//System.out.println("    current program is <" + BasicGLSLangProgram.currentProgram.getAllCode());
						;//System.out.println("    bound program is <" + ((BasicGLSLangProgram) p).getAllCode());

						((BasicGLSLangProgram) p).bindNow();
					}


					mesh.open();
					int a = mesh.nextVertex(v);
					int b = mesh.nextVertex(new Vector3(v.x + right.x, v.y + right.y, v.z + right.z));
					int c = mesh.nextVertex(new Vector3(v.x + right.x + down.x, v.y + right.y + down.y, v.z + right.z + down.z));
					int d = mesh.nextVertex(new Vector3(v.x + down.x, v.y + down.y, v.z + down.z));

					mesh.nextFace(a, b, c);
					mesh.nextFace(a, c, d);

					mesh.setAux(a, Base.color0_id, color.x, color.y, color.z, color.w);
					mesh.setAux(b, Base.color0_id, color.x, color.y, color.z, color.w);
					mesh.setAux(c, Base.color0_id, color.x, color.y, color.z, color.w);
					mesh.setAux(d, Base.color0_id, color.x, color.y, color.z, color.w);

					mesh.setAux(a, 4, 0);
					mesh.setAux(b, 4, 0);
					mesh.setAux(c, 4, 0);
					mesh.setAux(d, 4, 0);

					if (useRect) {
//						mesh.setAux(a, Base.texture0_id, 0, 0);
//						mesh.setAux(b, Base.texture0_id, dim.x, 0);
//						mesh.setAux(c, Base.texture0_id, dim.x, dim.y);
//						mesh.setAux(d, Base.texture0_id, 0, dim.y);

						
						mesh.setAux(a, Base.texture0_id, 0, 0);
						mesh.setAux(b, Base.texture0_id, 1, 0);
						mesh.setAux(c, Base.texture0_id, 1, 1);
						mesh.setAux(d, Base.texture0_id, 0, 1);

					} else {
						mesh.setAux(a, Base.texture0_id, 0, 0);
						mesh.setAux(b, Base.texture0_id, 1, 0);
						mesh.setAux(c, Base.texture0_id, 1, 1);
						mesh.setAux(d, Base.texture0_id, 0, 1);
					}
					mesh.close();

					glActiveTexture(GL_TEXTURE1);
					BaseSlowRawTexture t = browser[0].getTexture();
					if (first)
					{
						;//System.out.println(" texture first, doing setup");
						t.setup();
						t.dirty();
					}
					first = false;
					t.pre();

					// glBlendFunc(GL_SRC_ALPHA,
					// GL_ONE_MINUS_SRC_ALPHA);
					glTexParameteri(false & useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
					glTexParameteri(false & useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
					
					mesh.getUnderlyingGeometry().performPass(null);
					
					t.post();
					glActiveTexture(GL_TEXTURE0);

					if (was != null) {
						was.bindNow();
					}
				}

				private iFunction<Vector3, Vector2> newPageToSpace(final Vector3 v, final Vector3 right, final Vector3 down, final Vector2 dim) {
					return new iFunction<Vector3, Vector2>() {

						@Override
						public Vector3 f(Vector2 in) {
							float x = in.x / dim.x;
							float y = in.y / dim.y;

							return new Vector3(v).add(right, x).add(down, y);

						}
					};
				}
			}, new iDynamicMesh[] {});

			result.finalize = new iUpdateable() {

				@Override
				public void update() {

					;//System.out.println(" -- deallocating browser <" + browser[0] + ">");

					if (browser[0] != null)
						browser[0].dispose();
				}
			};

			return result;

		}
	}

	public SimpleWebpageDrawing() {
		this(false);
	}

	public SimpleWebpageDrawing(boolean useRect) {
		this.useRect = useRect;
	}

	public SimpleWebpageDrawing installInto(BaseGLGraphicsContext installedContext) {
		installedContext.addAcceptor(new DrawsBrowser(installedContext));
		this.installedContext = installedContext;
		return this;
	}

	public SimpleWebpageDrawing setRefreshHandle(iUpdateable u) {
		refreshHandle = u;
		return this;
	}

	protected void notifyUpdate() {
		if (refreshHandle != null)
			refreshHandle.update();
	}

	protected void rewriteEvent(final Dict properties, final Vector3 v, Vector3 right, Vector3 down, final BasicCamera camera, Vector2 e) {
		;//System.out.println(" up event at :" + e);

		Vector3 r1 = new Vector3();
		Vector3 r2 = new Vector3();
		camera.getState().getProjector().createIntersectionRay(e.x, camera.height - e.y, r1, r2, camera.width, camera.height);

		Vector2 dim = properties.get(browser_dimensions);
		if (dim == null)
			dim = new Vector2(500, 1000);

		;//System.out.println(" view ray is <" + r1 + " -> " + r2);

		Vector3 normal = new Vector3().cross(right, down);

		r2 = new Vector3().sub(r2, r1).normalize();

		float d1 = (v.dot(normal) - r1.dot(normal)) / r2.dot(normal);

		Vector3 a = new Vector3().add(r1).add(r2, d1);

		;//System.out.println(" intersection at <" + a + ">");

		float xx = new Vector3(a).sub(v).dot(new Vector3(right).normalize());
		float yy = new Vector3(a).sub(v).dot(new Vector3(down).normalize());

		;//System.out.println("        " + xx + " " + yy);

		xx /= right.mag();
		yy /= down.mag();

		xx *= dim.x;
		yy *= dim.y;

		;//System.out.println(" click on <" + xx + " " + yy + ">");

		e.x = (int) xx;
		e.y = (int) yy;
	}

	protected void rewriteEvent(final Dict properties, final Vector3 v, Vector3 right, Vector3 down, final BasicCamera camera, Event e) {
		;//System.out.println(" up event at :" + e);

		Vector3 r1 = new Vector3();
		Vector3 r2 = new Vector3();
		camera.getState().getProjector().createIntersectionRay(e.x, camera.height - e.y, r1, r2, camera.width, camera.height);

		Vector2 dim = properties.get(browser_dimensions);
		if (dim == null)
			dim = new Vector2(500, 1000);

		;//System.out.println(" view ray is <" + r1 + " -> " + r2);

		Vector3 normal = new Vector3().cross(right, down);

		r2 = new Vector3().sub(r2, r1).normalize();

		float d1 = (v.dot(normal) - r1.dot(normal)) / r2.dot(normal);

		Vector3 a = new Vector3().add(r1).add(r2, d1);

		;//System.out.println(" intersection at <" + a + ">");

		float xx = new Vector3(a).sub(v).dot(new Vector3(right).normalize());
		float yy = new Vector3(a).sub(v).dot(new Vector3(down).normalize());

		;//System.out.println("        " + xx + " " + yy);

		xx /= right.mag();
		yy /= down.mag();

		xx *= dim.x;
		yy *= dim.y;

		;//System.out.println(" click on <" + xx + " " + yy + ">");

		e.x = (int) xx;
		e.y = (int) yy;
	}

}
