package field.core.plugins.drawing.opengl;

import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;

import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.List;

import field.core.Constants;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext.DrawingResult;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext.DrawingResultCode;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext.iDrawingAcceptor;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.windowing.GLComponentWindow;
import field.graphics.core.BasicContextManager;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.TextSystem;
import field.graphics.core.TextSystem.RectangularLabel;
import field.graphics.dynamic.DynamicMesh;
import field.graphics.dynamic.iDynamicMesh;
import field.launch.iUpdateable;
import field.math.linalg.Quaternion;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Triple;
import field.util.Dict;

public class SimpleTextDrawing {

	public class DrawsText implements iDrawingAcceptor<CachedLine> {

		private final BaseGLGraphicsContext context;

		public DrawsText(BaseGLGraphicsContext context) {
			this.context = context;
		}

		public DrawingResult accept(List<iDynamicMesh> soFar, final CachedLine line, final Dict properties) {
			if (!properties.isTrue(iLinearGraphicsContext.containsText, false))
				return null;

			DrawingResult result = new DrawingResult(DrawingResultCode.cont, new iUpdateable() {
				private Vector4 black = new Vector4(0, 0, 0, 1f);

				float scale = 1;

				public void update() {

					scale = 1/getGlobalScale();

					for (Event e : line.events) {
						if (e.attributes != null && e.attributes.get(iLinearGraphicsContext.text_v) != null) {
							float opacityMul = 1;
							Number o = line.getProperties().get(iLinearGraphicsContext.totalOpacity);
							opacityMul *= o == null ? 1 : o.floatValue();

							Number f = e.attributes.get(iLinearGraphicsContext.alignment_v);
							if (f == null)
								f = 1f;

							mesh.open();
							String s = e.attributes.getMap().get(iLinearGraphicsContext.text_v) + "";
							float ts = e.attributes.getFloat(iLinearGraphicsContext.textScale_v, 1f);//getGlobalScale();

							float z = 0.5f;
							Object zz = e.attributes.getMap().get(iLinearGraphicsContext.z_v);
							if (zz == null)
								z = 0.5f;
							else if (zz instanceof Number)
								z = ((Number) zz).floatValue();
							else if (zz instanceof Vector3)
								z = ((Vector3) zz).z;

							Vector2 at = e.getDestination(new Vector2());
							Object of = e.attributes.get(iLinearGraphicsContext.textOffset_v);
							if (of != null) {
								if (of instanceof Vector2) {
									at = new Vector2(at).add((Vector2) of);
								} else if (of instanceof Vector3) {
									at = new Vector2(at).add(((Vector3) of).toVector2());
									z += ((Vector3) of).z;
								}
							}
							Object r = e.attributes.getMap().get(iLinearGraphicsContext.textRotation_v);

							render(at, s, e.attributes.get(iLinearGraphicsContext.font_v), e.attributes.get(iLinearGraphicsContext.textIsBlured_v), colorFor(e), opacityMul, f.floatValue(), ts, z, r);

							mesh.close();
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

				protected void render(Vector2 destination, String string, java.awt.Font font, Boolean blur, Vector4 colorFor, float op, float alignment, float totalScale, float z, Object rotation) {

					if (font == null)
						font = new Font(Constants.defaultFont, Font.PLAIN, 1).deriveFont(11 * scale);
					else
						font = font.deriveFont(font.getSize() * scale);

					Triple<String, Font, String> c = new Triple<String, Font, String>(string, font, blur + "" + colorFor);
					RectangularLabel r = cache.get(c);
					if (r == null) {
						r = TextSystem.textSystem.new RectangularLabel(string, font, 0);

						;//System.out.println(" rendering singleline test <" + string + "> with rect <" + useRect + ">");

						r.getTexture().use_gl_texture_rectangle_ext(useRect);
						r.setFont(font);

						if (string.startsWith("<html>")) {
							;//System.out.println(" reset text as label <" + string + ">");
							r.resetTextAsLabel(string, 1, 1, 1, 0, colorFor.x, colorFor.y, colorFor.z, colorFor.w);
						} else {
							if (blur != null && blur)
								r.resetTextDropShadow(string, 1, 1, 1, 0, colorFor.x, colorFor.y, colorFor.z, colorFor.w);
							else {

								;//System.out.println(" color is :" + colorFor);

								r.resetText(string, 1, 1, 1, 0, colorFor.x, colorFor.y, colorFor.z, colorFor.w);
								// r.resetText(string,0,0,0,1,
								// colorFor.x,
								// colorFor.y,
								// colorFor.z,
								// colorFor.w);
							}
						}
						cache.put(c, r);
					}

					if (alignment < 0)
						r.setJustification(TextSystem.Justification.right);
					else if (alignment > 0)
						r.setJustification(TextSystem.Justification.left);
					else
						r.setJustification(TextSystem.Justification.center);
					r.setTotalScale(totalScale);
					r.setZ(z);

					Quaternion rr = new Quaternion();
					if (rotation instanceof Number)
						rr = new Quaternion(((Number) rotation).doubleValue());
					else if (rotation instanceof Quaternion)
						rr = (Quaternion) rotation;

					r.drawIntoMeshScaledRotated(mesh, 1, 1, 1, op, destination.x, destination.y, scale, rr);

					// if (BasicContextManager.getGl() !=
					// null)
					{
						// if (context.inside instanceof
						// BasicGLSLangProgram)
						// ((BasicGLSLangProgram)
						// context.inside).performPass(null);
						
						
//						;//System.out.println(" about to draw context text <"+string+"> at <"+destination.x+", "+destination.y+"> "+scale+" "+rr);
//						BasicGLSLangProgram.currentProgram.debugPrintUniforms();
						
						glActiveTexture(GL_TEXTURE1);
						TextSystem.textSystem.beginRender(BasicContextManager.getGl(), BasicContextManager.getGlu());
						r.on();
						glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
						mesh.getUnderlyingGeometry().performPass(null);
						glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
						r.off();
						glActiveTexture(GL_TEXTURE0);
					}
				}
			}, new iDynamicMesh[] {});
			return result;
		}

		public LinkedHashMap<Triple<String, Font, String>, TextSystem.RectangularLabel> cache = new LinkedHashMap<Triple<String, Font, String>, TextSystem.RectangularLabel>() {
			@Override
			protected boolean removeEldestEntry(java.util.Map.Entry<Triple<String, Font, String>, RectangularLabel> eldest) {
				if (this.size() > cacheSize) {
					eldest.getValue().dispose();
					return true;
				}
				return false;
			}
		};

		int cacheSize = 150;
	}

	public class DrawsMultilineText implements iDrawingAcceptor<CachedLine> {

		private final BaseGLGraphicsContext context;

		public DrawsMultilineText(BaseGLGraphicsContext context) {
			this.context = context;
		}

		public DrawingResult accept(List<iDynamicMesh> soFar, final CachedLine line, final Dict properties) {
			if (!properties.isTrue(iLinearGraphicsContext.containsMultilineText, false))
				return null;

			DrawingResult result = new DrawingResult(DrawingResultCode.cont, new iUpdateable() {
				private Vector4 black = new Vector4(0, 0, 0, 1f);

				float scale = 1;

				public void update() {

					scale = 1 / GLComponentWindow.getCurrentWindow(null).getXScale();

					for (Event e : line.events) {
						if (e.attributes != null && e.attributes.get(iLinearGraphicsContext.text_v) != null) {
							float opacityMul = 1;
							Number o = line.getProperties().get(iLinearGraphicsContext.totalOpacity);
							opacityMul *= o == null ? 1 : o.floatValue();

							Number f = e.attributes.get(iLinearGraphicsContext.alignment_v);
							if (f == null)
								f = 1f;

							mesh.open();
							String s = e.attributes.getMap().get(iLinearGraphicsContext.text_v) + "";
							float ts = e.attributes.getFloat(iLinearGraphicsContext.textScale_v, 1f);

							float z = 0.5f;
							Object zz = e.attributes.getMap().get(iLinearGraphicsContext.z_v);
							if (zz == null)
								z = 0.5f;
							else if (zz instanceof Number)
								z = ((Number) zz).floatValue();
							else if (zz instanceof Vector3)
								z = ((Vector3) zz).z;

							float w = 200;
							Object ww = e.attributes.getMap().get(iLinearGraphicsContext.multilineWidth_v);
							if (ww instanceof Number)
								w = ((Number) ww).floatValue();
							else if (ww instanceof Vector3)
								w = ((Vector3) ww).z;

							render(e.getDestination(new Vector2()), s, e.attributes.get(iLinearGraphicsContext.font_v), e.attributes.get(iLinearGraphicsContext.textIsBlured_v), colorFor(e), opacityMul, f.floatValue(), ts, z, w);
							mesh.close();
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

				protected void render(Vector2 destination, String string, Font font, Boolean blur, Vector4 colorFor, float op, float alignment, float totalScale, float z, float width) {

					if (font == null)
						font = new Font(Constants.defaultFont, Font.PLAIN, 1).deriveFont(11 * scale);
					else
						font = font.deriveFont(font.getSize() * scale);

					Triple<String, Font, String> c = new Triple<String, Font, String>(string, font, blur + "" + colorFor);
					RectangularLabel r = cache.get(c);
					if (r == null) {
						r = TextSystem.textSystem.new RectangularLabel(string, font, 0);

						;//System.out.println(" rendering multiline test <" + string + "> with rect <" + useRect + ">");

						r.getTexture().use_gl_texture_rectangle_ext(useRect);
						r.getTexture().getMipMaps();
						r.setFont(font);

						if (alignment < 0)
							r.setJustification(TextSystem.Justification.right);
						else if (alignment > 0)
							r.setJustification(TextSystem.Justification.left);
						else
							r.setJustification(TextSystem.Justification.center);

						r.resetTextAsLabelWithMaxSize(string, 1, 1, 1, 0, colorFor.x, colorFor.y, colorFor.z, colorFor.w, width);

						cache.put(c, r);
					}

					r.setTotalScale(totalScale);
					r.setZ(z);

					;//System.out.println(" drawing multiline text ");
					r.getTexture().use_gl_texture_rectangle_ext(useRect);
					r.drawIntoMeshAll(mesh, 1, 1, 1, op, destination.x, destination.y);

					// Object gl =
					// BasicContextManager.getGl();
					// if (gl != null)
					{

						// if (context.inside instanceof
						// BasicGLSLangProgram)
						// ((BasicGLSLangProgram)
						// context.inside).performPass(null);

						glActiveTexture(GL_TEXTURE1);

						TextSystem.textSystem.beginRender(null, BasicContextManager.getGlu());
						r.on();
						glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
						// glTexParameterfv(useRect ?
						// GL_TEXTURE_RECTANGLE :
						// GL_TEXTURE_2D,
						// GL_TEXTURE_BORDER_COLOR, new
						// float[]{0,0,0,0},0);
						glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
						glTexParameteri(useRect ? GL_TEXTURE_RECTANGLE : GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
						mesh.getUnderlyingGeometry().performPass(null);
						glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
						r.off();
						glActiveTexture(GL_TEXTURE0);
					}
				}
			}, new iDynamicMesh[] {});
			return result;
		}

		public LinkedHashMap<Triple<String, Font, String>, TextSystem.RectangularLabel> cache = new LinkedHashMap<Triple<String, Font, String>, TextSystem.RectangularLabel>() {
			@Override
			protected boolean removeEldestEntry(java.util.Map.Entry<Triple<String, Font, String>, RectangularLabel> eldest) {
				if (this.size() > cacheSize) {
					eldest.getValue().dispose();
					return true;
				}
				return false;
			}
		};

		int cacheSize = 50;
	}

	private boolean useRect;

	public SimpleTextDrawing(boolean useRect) {
		this.useRect = useRect;
	}

	Float forceScale = null;

	public SimpleTextDrawing() {
		this(true);
	}

	public SimpleTextDrawing setForceScale(Float forceScale) {
		this.forceScale = forceScale;
		return this;
	}

	protected float getGlobalScale() {
		if (forceScale != null)
			return forceScale;
		return GLComponentWindow.getCurrentWindow(null).getXScale();
	}

	final DynamicMesh mesh = DynamicMesh.unshadedMesh();

	public void installInto(BaseGLGraphicsContext installedContext) {

		installedContext.addAcceptor(new DrawsText(installedContext));
		installedContext.addAcceptor(new DrawsMultilineText(installedContext));
	}

}
