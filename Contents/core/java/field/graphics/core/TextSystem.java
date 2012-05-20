package field.graphics.core;



import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.PlanarImage;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.RepaintManager;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

import field.core.Constants;
import field.graphics.core.AdvancedTextures.BaseFastRawTexture;
import field.graphics.core.AdvancedTextures.BaseSlowRawTexture;
import field.graphics.dynamic.iDynamicMesh;
import field.math.linalg.Quaternion;
import field.math.linalg.Vector3;
import field.util.TaskQueue;

public class TextSystem {

	public class AribitraryComponent {
		private final int w;

		private final int h;

		private final IntBuffer intBuffer;

		private int[] data;

		private final BaseFastRawTexture slow;

		private Rectangle2D stringBounds;

		private Rectangle bounds;

		// texture information
		BufferedImage image;

		Graphics2D g2;

		int lastBoundsX;

		int lastBoundsY;

		boolean first = true;

		public AribitraryComponent(int w, int h) {
			this.w = w;
			this.h = h;

			
			image = dev.getDefaultConfiguration().createCompatibleImage(w, h, Transparency.TRANSLUCENT);

			g2 = (Graphics2D) image.getGraphics();
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

			DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
			data = dataBuffer.getData();

			ByteBuffer ob = ByteBuffer.allocateDirect(4 * w * h).order(ByteOrder.nativeOrder());
			intBuffer = ob.asIntBuffer();
			intBuffer.put(data);
			intBuffer.rewind();

			slow = (BaseFastRawTexture) new AdvancedTextures.BaseFastRawTexture("", intBuffer, w, h, null).use_gl_texture_rectangle_ext(true);
		}

		public void dispose() {
			slow.deallocate(atRenderTime);
		}

		public void drawIntoMesh(iDynamicMesh triangle, float r, float g, float b, float a, float ox, float by) {
			triangle.open();

			int edge = 0;

			float x, y, hw, hh;

			x = ox;
			y = by;

			hw = bounds != null ? bounds.width : w;
			hh = bounds != null ? bounds.height : h;

			float AX = 0.0f;
			float AY = 0.0f;

			int v0 = triangle.nextVertex(new Vector3(AX + x, AY + y, 0.1f));
			int v1 = triangle.nextVertex(new Vector3(AX + x + hw, AY + y, 0.1f));
			int v2 = triangle.nextVertex(new Vector3(AX + x + hw, AY + y + hh, 0.1f));
			int v3 = triangle.nextVertex(new Vector3(AX + x, AY + y + hh, 0.1f));

			triangle.nextFace(v0, v1, v2);
			triangle.nextFace(v0, v2, v3);

			triangle.setAux(v0, Base.color0_id, r, g, b, a);
			triangle.setAux(v1, Base.color0_id, r, g, b, a);
			triangle.setAux(v2, Base.color0_id, r, g, b, a);
			triangle.setAux(v3, Base.color0_id, r, g, b, a);

			float scaling = 1;

			triangle.setAux(v0, Base.texture0_id, 0, 0);
			triangle.setAux(v1, Base.texture0_id, (scaling * hw), 0);
			triangle.setAux(v2, Base.texture0_id, scaling * hw, scaling * hh);
			triangle.setAux(v3, Base.texture0_id, 0, scaling * hh);

			if (slow.textureTarget == GL_TEXTURE_2D) {
				triangle.setAux(v0, Base.texture0_id, 0, 0);
				triangle.setAux(v1, Base.texture0_id, (scaling * hw) / w, 0);
				triangle.setAux(v2, Base.texture0_id, scaling * hw / w, scaling * hh / h);
				triangle.setAux(v3, Base.texture0_id, 0, scaling * hh / h);
			}

			// triangle.setAux(v0, Base.texture0_id, 0,0);
			// triangle.setAux(v1, Base.texture0_id, 1,0);
			// triangle.setAux(v2, Base.texture0_id, 1,1);
			// triangle.setAux(v3, Base.texture0_id, 0,1);

			triangle.close();
		}
		public void drawIntoMeshXFlipped(iDynamicMesh triangle, float r, float g, float b, float a, float ox, float by) {
			triangle.open();

			int edge = 0;

			float x, y, hw, hh;

			x = ox;
			y = by;

			hw = bounds != null ? bounds.width : w;
			hh = bounds != null ? bounds.height : h;

			float AX = 0.0f;
			float AY = 0.0f;

			int v0 = triangle.nextVertex(new Vector3(AX + x, AY + y, 0.1f));
			int v1 = triangle.nextVertex(new Vector3(AX + x + hw, AY + y, 0.1f));
			int v2 = triangle.nextVertex(new Vector3(AX + x + hw, AY + y + hh, 0.1f));
			int v3 = triangle.nextVertex(new Vector3(AX + x, AY + y + hh, 0.1f));

			triangle.nextFace(v0, v1, v2);
			triangle.nextFace(v0, v2, v3);

			triangle.setAux(v0, Base.color0_id, r, g, b, a);
			triangle.setAux(v1, Base.color0_id, r, g, b, a);
			triangle.setAux(v2, Base.color0_id, r, g, b, a);
			triangle.setAux(v3, Base.color0_id, r, g, b, a);

			float scaling = 1;

			triangle.setAux(v0, Base.texture0_id, (scaling * hw), 0);
			triangle.setAux(v1, Base.texture0_id, 0, 0);
			triangle.setAux(v2, Base.texture0_id, 0, scaling * hh);
			triangle.setAux(v3, Base.texture0_id, scaling * hw, scaling * hh);

			// triangle.setAux(v0, Base.texture0_id, 0,0);
			// triangle.setAux(v1, Base.texture0_id, 1,0);
			// triangle.setAux(v2, Base.texture0_id, 1,1);
			// triangle.setAux(v3, Base.texture0_id, 0,1);

			triangle.close();
		}

		public float getTextHeight() {
			return (float) (stringBounds.getHeight() / scaling);
		}

		public float getTextWidth() {
			return (float) (stringBounds.getWidth() / scaling);
		}

		public void off() {
			slow.setGL(gl);
			slow.post();
		}

		public void on() {
			assert gl != null;

			slow.setGL(gl);
			if (first) {
				slow.setup();
				first = false;
			}
			slow.pre();
		}

		public void resetImage(JComponent componentToRender, float backr, float backg, float backb, float backa) {

			g2.setColor(new Color(backr, backg, backb, backa));
			Composite was = g2.getComposite();
			g2.setComposite(AlphaComposite.Src);
			g2.fillRect(-Math.max(w, h) * 2, -Math.max(w, h) * 2, Math.max(w, h) * 4, Math.max(w, h) * 4);
			g2.setComposite(was);

			// g2.translate(lastBoundsX, lastBoundsY);
			// g2.translate(-(lastBoundsX=componentToRender.getBounds().x),
			// -(lastBoundsY=componentToRender.getBounds().y));

			g2.setColor(new Color(1, 0, 1, 1f));
			
			;//System.out.println(" painting <"+componentToRender+"> into <"+g2+">");
			
			RepaintManager.currentManager(componentToRender).setDoubleBufferingEnabled(false);
			componentToRender.paint(g2);

			// g2.fillRect(0, 0, 30, 30);
			// g2.fillRect(0, 0, w, h);
			bounds = componentToRender.getBounds();

			;//System.out.println(" bounds are <"+bounds+">");
			
			DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
			data = dataBuffer.getData();

			intBuffer.rewind();
			intBuffer.put(data);
			intBuffer.rewind();
			slow.dirty();
		}

		public Graphics2D getG2() {
			return g2;
		}

		public void reupload() {
			DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
			data = dataBuffer.getData();

			intBuffer.rewind();
			intBuffer.put(data);
			intBuffer.rewind();
			slow.dirty();
		}

		public BaseFastRawTexture getTexture() {
			return slow;
		}
	}

	public interface iLabel {
		public void dispose();

		public void drawIntoMesh(iDynamicMesh triangle, float r, float g, float b, float a, float ox, float by);

		public float getTextHeight();

		public float getTextWidth();

		public void off();

		public void on();

		public void resetText(String text, float backr, float backg, float backb, float backa, float frontr, float frontg, float frontb, float fronta);

		public void setFont(Font f);

		public void setJustification(Justification j);
	}

	public enum Justification {
		left, center, right;
	}

	public class Label implements iLabel {

		private final int w;

		private final int h;

		private Font font;

		private Justification j = Justification.center;

		private final IntBuffer intBuffer;

		private int[] data;

		private final BaseSlowRawTexture slow;

		private Rectangle2D stringBounds;

		private float descent;

		private float ascent;

		// texture information
		BufferedImage image;

		Graphics2D g2;

		boolean first = true;

		public Label(int w, int h) {
			this.w = w;
			this.h = h;
			font = new Font(Constants.defaultFont, Font.PLAIN, (int) (12 * scaling));

			image = dev.getDefaultConfiguration().createCompatibleImage(w, h);

			g2 = (Graphics2D) image.getGraphics();
			setFont(font);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			// g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
			// RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

			DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
			data = dataBuffer.getData();

			intBuffer = ByteBuffer.allocateDirect(4 * w * h).order(ByteOrder.nativeOrder()).asIntBuffer();
			intBuffer.put(data);
			intBuffer.rewind();

			slow = new AdvancedTextures.BaseSlowRawTexture("", intBuffer, w, h, null);
		}

		public void dispose() {
			slow.deallocate(atRenderTime);
		}

		public void drawIntoMesh(iDynamicMesh triangle, float r, float g, float b, float a, float ox, float by) {
			triangle.open();

			int edge = 0;

			int x, y, hw, hh;

			if (j == Justification.center) {
				x = (int) (ox - (stringBounds.getWidth() / 2) / scaling);
				y = (int) (by - ascent / scaling);

				hw = (int) ((float) stringBounds.getWidth() / scaling);
				hh = (int) (2 * (float) stringBounds.getHeight() / scaling);
			} else if (j == Justification.left) {
				x = (int) (ox);
				y = (int) (by - ascent / scaling);

				hw = (int) ((float) stringBounds.getWidth() / scaling);
				hh = (int) (2 * (float) stringBounds.getHeight() / scaling);
			} else {
				x = (int) (ox - stringBounds.getWidth() / scaling);
				y = (int) (by - ascent / scaling);

				hw = (int) ((float) stringBounds.getWidth() / scaling);
				hh = (int) (2 * (float) stringBounds.getHeight() / scaling);
			}

			float AX = 0.0f;
			float AY = 0.0f;

			int v0 = triangle.nextVertex(new Vector3(AX + x, AY + y + (int) (descent / scaling), 0));
			int v1 = triangle.nextVertex(new Vector3(AX + x + hw, AY + y + (int) (descent / scaling), 0));
			int v2 = triangle.nextVertex(new Vector3(AX + x + hw, AY + y + hh + (int) (descent / scaling), 0));
			int v3 = triangle.nextVertex(new Vector3(AX + x, AY + y + hh + (int) (descent / scaling), 0));

			triangle.nextFace(v0, v1, v2);
			triangle.nextFace(v0, v2, v3);

			triangle.setAux(v0, Base.color0_id, r, g, b, a);
			triangle.setAux(v1, Base.color0_id, r, g, b, a);
			triangle.setAux(v2, Base.color0_id, r, g, b, a);
			triangle.setAux(v3, Base.color0_id, r, g, b, a);

			// triangle.setAux(v0, Base.texture0_id, x/w, y/h);
			// triangle.setAux(v1, Base.texture0_id, (x+hw)/w, y/h);
			// triangle.setAux(v2, Base.texture0_id, (x+hw)/w,
			// (y+hh)/h);
			// triangle.setAux(v3, Base.texture0_id, x/w, (y+hh)/h);

			triangle.setAux(v0, Base.texture0_id, 0, 0);
			triangle.setAux(v1, Base.texture0_id, (scaling * hw) / w, 0);
			triangle.setAux(v2, Base.texture0_id, scaling * hw / w, scaling * hh / h);
			triangle.setAux(v3, Base.texture0_id, 0, scaling * hh / h);

			// triangle.setAux(v0, Base.texture0_id, 0,0);
			// triangle.setAux(v1, Base.texture0_id, 1,0);
			// triangle.setAux(v2, Base.texture0_id, 1,1);
			// triangle.setAux(v3, Base.texture0_id, 0,1);

			triangle.close();
		}

		public float getTextHeight() {
			return (float) (stringBounds.getHeight() / scaling);
		}

		public BaseSlowRawTexture getTexture() {
			return slow;
		}

		public float getTextWidth() {
			return (float) (stringBounds.getWidth() / scaling);
		}

		public void off() {
			slow.setGL(gl);
			slow.post();
		}

		public void on() {
			assert gl != null;

			slow.setGL(gl);
			if (first) {
				slow.setup();
				first = false;
			}
			slow.pre();
		}

		public void resetText(String text, float backr, float backg, float backb, float backa, float frontr, float frontg, float frontb, float fronta) {

			LineMetrics lineMetrics = g2.getFontMetrics(font).getLineMetrics(text, g2);

			descent = lineMetrics.getDescent();
			ascent = lineMetrics.getAscent();
			stringBounds = g2.getFontMetrics(font).getStringBounds(text, g2);

			g2.setColor(new Color(backr, backg, backb, backa));
			g2.fillRect(0, 0, w, h);
			g2.setColor(new Color(frontr, frontg, frontb, fronta));
			g2.drawString(text, 0, (int) ascent);

			DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
			data = dataBuffer.getData();

			intBuffer.put(data);
			intBuffer.rewind();
			slow.dirty();
		}

		public void setFont(Font f) {
			f = f.deriveFont(f.getSize() * scaling);
			this.font = f;
			g2.setFont(f);

		}

		public void setJustification(Justification j) {
			this.j = j;
		}
	}

	public class RectangularLabel implements iLabel {

		private int w;

		private int h;

		private Font font;

		private Justification j = Justification.center;

		private IntBuffer intBuffer;

		private int[] data;

		private BaseFastRawTexture slow;

		private Rectangle2D stringBounds;

		private float descent;

		private float ascent;

		private final FontMetrics fontMetrics;

		private final float rotation;

		private double rotationAlpha;

		private double offset;

		private String text;

		private ByteBuffer ob;

		// texture information
		BufferedImage image;

		Graphics2D g2;

		boolean first = true;

		public RectangularLabel(int w, int h) {
			this.w = w;
			this.h = h;
			font = new Font(Constants.defaultFont, Font.PLAIN, 12).deriveFont(12 * scaling);

			;//System.out.println(" made a font <"+font+"> from <"+Constants.defaultFont+">");
			
			image = dev.getDefaultConfiguration().createCompatibleImage(w, h, Transparency.OPAQUE);

			g2 = (Graphics2D) image.getGraphics();
			setFont(font);

			// g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
			// RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
			// g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
			// RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

			DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
			data = dataBuffer.getData();

			ob = ByteBuffer.allocateDirect(4 * w * h).order(ByteOrder.nativeOrder());
			intBuffer = ob.asIntBuffer();
			intBuffer.put(data);
			intBuffer.rewind();

			slow = (BaseFastRawTexture) new AdvancedTextures.BaseFastRawTexture("", intBuffer, w, h, null).use_gl_texture_rectangle_ext(false);

			fontMetrics = g2.getFontMetrics(font);

			this.rotation = 0;
		}

		public RectangularLabel(String text, Font font, float rotation) {

			this.font = font;
			this.rotation = rotation;

			;//System.out.println(" made a font <"+font.getFontName()+"> from <"+Constants.defaultFont+">");

			BufferedImage imageTmp = dev.getDefaultConfiguration().createCompatibleImage(1, 1, Transparency.TRANSLUCENT);
			g2 = (Graphics2D) imageTmp.getGraphics();
			fontMetrics = g2.getFontMetrics(font);
			Rectangle2D bounds = fontMetrics.getStringBounds(text, g2);

			if (bounds.getHeight() < font.getSize() * 3.5f) {
				bounds.setFrame(new Rectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), font.getSize() * 3.5f));
			}

			this.w = (int) (bounds.getMaxX() - bounds.getMinX() + 25);
			this.h = (int) (bounds.getHeight() * 2);

			rotationAlpha = rotation / (Math.PI / 2);

			float ow = w;
			float oh = h;
			w = (int) (ow * (1 - rotationAlpha) + rotationAlpha * oh);
			h = (int) (oh * (1 - rotationAlpha) + rotationAlpha * ow);

			offset = rotationAlpha * w;

			w = Math.max(1, w);
			h = Math.max(1,h);
			
			image = dev.getDefaultConfiguration().createCompatibleImage(w, h, Transparency.TRANSLUCENT);

			g2 = (Graphics2D) image.getGraphics();
			setFont(font);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

			DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
			data = dataBuffer.getData();

			ob = ByteBuffer.allocateDirect(4 * w * h).order(ByteOrder.nativeOrder());
			intBuffer = ob.asIntBuffer();

			intBuffer.put(data);
			intBuffer.rewind();

			slow = (BaseFastRawTexture) new AdvancedTextures.BaseFastRawTexture(text, intBuffer, w, h, null).use_gl_texture_rectangle_ext(true);
			;
			// slow = (BaseSlowRawTexture) new
			// AdvancedTextures.BaseSlowRawTexture("", intBuffer, w,
			// h, null).use_gl_texture_rectangle_ext(false);;

			g2.rotate(rotation);

		}

		public void dispose() {
			// a change
			;//System.out.println(" deallocating rect text <" + text + "> n <" + ResourceMonitor.resourceMonitor + ">");
			slow.deallocate(ResourceMonitor.resourceMonitor.getQueue());
		}

		public void drawIntoMesh(iDynamicMesh triangle, float r, float g, float b, float a, float ox, float by) {
			triangle.open();

			int edge = 0;

			float x, y, hw, hh;

			if (j == Justification.center) {
				x = (int) (ox - (stringBounds.getWidth() / 2) / scaling);
				y = (int) (by - ascent / scaling);

				hw = (int) ((float) stringBounds.getWidth() / scaling);
				hh = (int) (2 * (float) stringBounds.getHeight() / scaling);
			} else if (j == Justification.left) {
				x = (int) (ox);
				y = (int) (by - ascent  / scaling);

				hw = (int) ((float) stringBounds.getWidth() / scaling);
				hh = (int) (2 * (float) stringBounds.getHeight() / scaling);
			} else {
				x = (int) (ox - stringBounds.getWidth() / scaling);
				y = (int) (by - ascent / scaling);

				hw = (int) ((float) stringBounds.getWidth() / scaling);
				hh = (int) (2 * (float) stringBounds.getHeight() / scaling);
			}

			float AX = 0.0f;
			float AY = 0.0f;

			Vector3 vv1 = new Vector3(AX + x, AY + y + (int) (descent / scaling), 0.5f);
			int v0 = triangle.nextVertex(vv1);
			Vector3 vv2 = new Vector3(AX + x + hw, AY + y + (int) (descent / scaling), 0.5f);
			int v1 = triangle.nextVertex(vv2);
			Vector3 vv3 = new Vector3(AX + x + hw, AY + y + hh + (int) (descent / scaling), 0.5f);
			int v2 = triangle.nextVertex(vv3);
			Vector3 vv4 = new Vector3(AX + x, AY + y + hh + (int) (descent / scaling), 0.5f);
			int v3 = triangle.nextVertex(vv4);

			triangle.nextFace(v0, v1, v2);
			triangle.nextFace(v0, v2, v3);

			triangle.setAux(v0, Base.color0_id, r, g, b, a);
			triangle.setAux(v1, Base.color0_id, r, g, b, a);
			triangle.setAux(v2, Base.color0_id, r, g, b, a);
			triangle.setAux(v3, Base.color0_id, r, g, b, a);

			// triangle.setAux(v0, Base.texture0_id, x/w, y/h);
			// triangle.setAux(v1, Base.texture0_id, (x+hw)/w, y/h);
			// triangle.setAux(v2, Base.texture0_id, (x+hw)/w,
			// (y+hh)/h);
			// triangle.setAux(v3, Base.texture0_id, x/w, (y+hh)/h);

			triangle.setAux(v0, Base.texture0_id, 0, 0);
			triangle.setAux(v1, Base.texture0_id, (scaling * hw), 0);
			triangle.setAux(v2, Base.texture0_id, scaling * hw, scaling * hh);
			triangle.setAux(v3, Base.texture0_id, 0, scaling * hh);

			if (textFlipped) {
				triangle.setAux(v3, Base.texture0_id, 0, 0);
				triangle.setAux(v2, Base.texture0_id, (scaling * hw), 0);
				triangle.setAux(v1, Base.texture0_id, scaling * hw, scaling * hh);
				triangle.setAux(v0, Base.texture0_id, 0, scaling * hh);
			}

			// triangle.setAux(v0, Base.texture0_id, 0,0);
			// triangle.setAux(v1, Base.texture0_id, 1,0);
			// triangle.setAux(v2, Base.texture0_id, 1,1);
			// triangle.setAux(v3, Base.texture0_id, 0,1);

			triangle.close();
		}

		public void drawIntoMeshRotated(iDynamicMesh triangle, float r, float g, float b, float a, float ox, float by, float scale) {
			triangle.open();

			int edge = 0;
			float scaling = scale;

			float x, y, hw, hh;

			x = (ox - ascent / scaling);
			y = (by - ascent / 2 / scaling);

			hw = (((float) stringBounds.getHeight() / scaling) + descent);
			hh = ((float) stringBounds.getWidth() / scaling);

			float AX = 0.0f;
			float AY = 0.0f;

			Vector3 x1 = new Vector3(AX + x, AY + y + (descent / scaling), 0.5f);
			int v0 = triangle.nextVertex(x1);
			Vector3 x2 = new Vector3(AX + x + hw, AY + y + (descent / scaling), 0.5f);
			int v1 = triangle.nextVertex(x2);
			Vector3 x3 = new Vector3(AX + x + hw, AY + y + hh + (descent / scaling), 0.5f);
			int v2 = triangle.nextVertex(x3);
			Vector3 x4 = new Vector3(AX + x, AY + y + hh + (descent / scaling), 0.5f);
			int v3 = triangle.nextVertex(x4);

			triangle.nextFace(v0, v1, v2);
			triangle.nextFace(v0, v2, v3);

			triangle.setAux(v0, Base.color0_id, r, g, b, a);
			triangle.setAux(v1, Base.color0_id, r, g, b, a);
			triangle.setAux(v2, Base.color0_id, r, g, b, a);
			triangle.setAux(v3, Base.color0_id, r, g, b, a);

			triangle.setAux(v0, Base.texture0_id, 0, 0);
			triangle.setAux(v1, Base.texture0_id, (scaling * hw), 0);
			triangle.setAux(v2, Base.texture0_id, scaling * hw, scaling * hh);
			triangle.setAux(v3, Base.texture0_id, 0, scaling * hh);

			;//System.out.println(" text target is :"+slow.textureTarget +" "+GL_TEXTURE_2D);
			if (slow.textureTarget == GL_TEXTURE_2D) {
				;//System.out.println(" rectangular text is actually 2d texture");
				triangle.setAux(v0, Base.texture0_id, 0, 0);
				triangle.setAux(v1, Base.texture0_id, (scaling * hw) / w, 0);
				triangle.setAux(v2, Base.texture0_id, scaling * hw / w, scaling * hh / h);
				triangle.setAux(v3, Base.texture0_id, 0, scaling * hh / h);
			}

			triangle.close();
		}

		public void drawIntoMeshAll(iDynamicMesh triangle, float r, float g, float b, float a, float ox, float by) {
			triangle.open();

			int edge = 0;

			float x, y, hw, hh;

			x = ox;
			y = by;

			hw = w;
			hh = h;

			float AX = 0.0f;
			float AY = 0.0f;

			Vector3 x1 = new Vector3(AX + x, AY + y , 0.5f);
			int v0 = triangle.nextVertex(x1);
			Vector3 x2 = new Vector3(AX + x + hw, AY + y , 0.5f);
			int v1 = triangle.nextVertex(x2);
			Vector3 x3 = new Vector3(AX + x + hw, AY + y + hh, 0.5f);
			int v2 = triangle.nextVertex(x3);
			Vector3 x4 = new Vector3(AX + x, AY + y + hh, 0.5f);
			int v3 = triangle.nextVertex(x4);

			triangle.nextFace(v0, v1, v2);
			triangle.nextFace(v0, v2, v3);

			triangle.setAux(v0, Base.color0_id, r, g, b, a);
			triangle.setAux(v1, Base.color0_id, r, g, b, a);
			triangle.setAux(v2, Base.color0_id, r, g, b, a);
			triangle.setAux(v3, Base.color0_id, r, g, b, a);


			triangle.setAux(v0, Base.texture0_id, 0, 0);
			triangle.setAux(v1, Base.texture0_id, ( hw), 0);
			triangle.setAux(v2, Base.texture0_id,  hw,  hh);
			triangle.setAux(v3, Base.texture0_id, 0,  hh);
			
			;//System.out.println(" text target is :"+slow.textureTarget +" "+GL_TEXTURE_2D);
			if (slow.textureTarget == GL_TEXTURE_2D) {
				;//System.out.println(" rectangular text is actually 2d texture");
				triangle.setAux(v0, Base.texture0_id, 0, 0);
				triangle.setAux(v1, Base.texture0_id, (scaling * hw) / w, 0);
				triangle.setAux(v2, Base.texture0_id, scaling * hw / w, scaling * hh / h);
				triangle.setAux(v3, Base.texture0_id, 0, scaling * hh / h);
			}

			triangle.close();
		}

		float totalScale = 1;

		public void setTotalScale(float totalScale) {
			this.totalScale = totalScale;
		}

		float z = 0.5f;

		public void setZ(float z) {
			this.z = z;
		}

		public void drawIntoMeshScaled(iDynamicMesh triangle, float r, float g, float b, float a, float ox, float by, float scale) {
			triangle.open();

			int edge = 0;

			float x, y, hw, hh;

			float scaling = scale;

			ascent = (float) (stringBounds.getHeight() / 2);
			descent = 0;
			if (j == Justification.center) {
				x = (float) (ox - (stringBounds.getWidth() / 2) / scaling);
				y = (by - ascent / scaling);

				hw = ((float) stringBounds.getWidth() / scaling);
				hh = ((float) stringBounds.getHeight() / scaling);
			} else if (j == Justification.left) {
				x = (ox);
				y = (by - ascent  / scaling);

				hw = ((float) stringBounds.getWidth() / scaling);
				hh = ((float) stringBounds.getHeight() / scaling);
			} else {
				x = (float) (ox - stringBounds.getWidth() / scaling);
				y = (by - ascent / scaling);

				hw = ((float) stringBounds.getWidth() / scaling);
				hh = ((float) stringBounds.getHeight() / scaling);
			}

			float AX = 0.0f;
			float AY = 0.0f;

			
			Vector3 xx1 = new Vector3(AX + x, AY + y + (int) (descent / scaling), z);
			int v0 = triangle.nextVertex(xx1);
			Vector3 xx2 = new Vector3(AX + x + hw * totalScale, AY + y + (int) (descent / scaling), z);
			int v1 = triangle.nextVertex(xx2);
			Vector3 xx3 = new Vector3(AX + x + hw * totalScale, AY + y + hh * totalScale + (int) (descent / scaling), z);
			int v2 = triangle.nextVertex(xx3);
			Vector3 xx4 = new Vector3(AX + x, AY + y + hh * totalScale + (int) (descent / scaling), z);
			int v3 = triangle.nextVertex(xx4);

			triangle.nextFace(v0, v1, v2);
			triangle.nextFace(v0, v2, v3);

			triangle.setAux(v0, Base.color0_id, r, g, b, a);
			triangle.setAux(v1, Base.color0_id, r, g, b, a);
			triangle.setAux(v2, Base.color0_id, r, g, b, a);
			triangle.setAux(v3, Base.color0_id, r, g, b, a);

			// triangle.setAux(v0, Base.texture0_id, x/w, y/h);
			// triangle.setAux(v1, Base.texture0_id, (x+hw)/w, y/h);
			// triangle.setAux(v2, Base.texture0_id, (x+hw)/w,
			// (y+hh)/h);
			// triangle.setAux(v3, Base.texture0_id, x/w, (y+hh)/h);

			triangle.setAux(v0, Base.texture0_id, 0, 0);
			triangle.setAux(v1, Base.texture0_id, (scaling * hw), 0);
			triangle.setAux(v2, Base.texture0_id, scaling * hw, scaling * hh);
			triangle.setAux(v3, Base.texture0_id, 0, scaling * hh);

			if (textFlipped) {
				triangle.setAux(v3, Base.texture0_id, 0, 0);
				triangle.setAux(v2, Base.texture0_id, (scaling * hw), 0);
				triangle.setAux(v1, Base.texture0_id, scaling * hw, scaling * hh);
				triangle.setAux(v0, Base.texture0_id, 0, scaling * hh);
			}

			if (slow.textureTarget == GL_TEXTURE_2D) {
				triangle.setAux(v0, Base.texture0_id, 0, 0);
				triangle.setAux(v1, Base.texture0_id, (scaling * hw) / w, 0);
				triangle.setAux(v2, Base.texture0_id, scaling * hw / w, scaling * hh / h);
				triangle.setAux(v3, Base.texture0_id, 0, scaling * hh / h);
			}

			// triangle.setAux(v0, Base.texture0_id, 0,0);
			// triangle.setAux(v1, Base.texture0_id, 1,0);
			// triangle.setAux(v2, Base.texture0_id, 1,1);
			// triangle.setAux(v3, Base.texture0_id, 0,1);

			triangle.close();
		}
		
		public void drawIntoMeshScaledRotated(iDynamicMesh triangle, float r, float g, float b, float a, float ox, float by, float scale, Quaternion q) {
			triangle.open();

			int edge = 0;

			float x, y, hw, hh;

			float scaling = scale;

			ascent = (float) (stringBounds.getHeight() / 2);
			descent = 0;
			if (j == Justification.center) {
				x = (float) (ox - (stringBounds.getWidth() / 2) / scaling);
				y = (by - ascent / scaling);

				hw = ((float) stringBounds.getWidth() / scaling);
				hh = ((float) stringBounds.getHeight() / scaling);
			} else if (j == Justification.left) {
				x = (ox);
				y = (by - ascent  / scaling);

				hw = ((float) stringBounds.getWidth() / scaling);
				hh = ((float) stringBounds.getHeight() / scaling);
			} else {
				x = (float) (ox - stringBounds.getWidth() / scaling);
				y = (by - ascent / scaling);

				hw = ((float) stringBounds.getWidth() / scaling);
				hh = ((float) stringBounds.getHeight() / scaling);
			}

			float AX = 0.0f;
			float AY = 0.0f;

			
			Vector3 xx1 = q.rotateVector(new Vector3(AX , AY , 0)).add(new Vector3(x, y+ (int) (descent / scaling), z));
			int v0 = triangle.nextVertex(xx1);
			Vector3 xx2 = q.rotateVector(new Vector3(AX  + hw * totalScale, AY  , 0)).add(new Vector3(x, y+ (int) (descent / scaling), z));
			int v1 = triangle.nextVertex(xx2);
			Vector3 xx3 = q.rotateVector(new Vector3(AX  + hw * totalScale, AY  + hh * totalScale , 0)).add(new Vector3(x, y+ (int) (descent / scaling), z));
			int v2 = triangle.nextVertex(xx3);
			Vector3 xx4 = q.rotateVector(new Vector3(AX , AY  + hh * totalScale , 0)).add(new Vector3(x, y+ (int) (descent / scaling), z));
			int v3 = triangle.nextVertex(xx4);

			triangle.nextFace(v0, v1, v2);
			triangle.nextFace(v0, v2, v3);

			triangle.setAux(v0, Base.color0_id, r, g, b, a);
			triangle.setAux(v1, Base.color0_id, r, g, b, a);
			triangle.setAux(v2, Base.color0_id, r, g, b, a);
			triangle.setAux(v3, Base.color0_id, r, g, b, a);

			// triangle.setAux(v0, Base.texture0_id, x/w, y/h);
			// triangle.setAux(v1, Base.texture0_id, (x+hw)/w, y/h);
			// triangle.setAux(v2, Base.texture0_id, (x+hw)/w,
			// (y+hh)/h);
			// triangle.setAux(v3, Base.texture0_id, x/w, (y+hh)/h);

			triangle.setAux(v0, Base.texture0_id, 0, 0);
			triangle.setAux(v1, Base.texture0_id, (scaling * hw), 0);
			triangle.setAux(v2, Base.texture0_id, scaling * hw, scaling * hh);
			triangle.setAux(v3, Base.texture0_id, 0, scaling * hh);

			if (textFlipped) {
				triangle.setAux(v3, Base.texture0_id, 0, 0);
				triangle.setAux(v2, Base.texture0_id, (scaling * hw), 0);
				triangle.setAux(v1, Base.texture0_id, scaling * hw, scaling * hh);
				triangle.setAux(v0, Base.texture0_id, 0, scaling * hh);
			}

			if (slow.textureTarget == GL_TEXTURE_2D) {
				triangle.setAux(v0, Base.texture0_id, 0, 0);
				triangle.setAux(v1, Base.texture0_id, (scaling * hw) / w, 0);
				triangle.setAux(v2, Base.texture0_id, scaling * hw / w, scaling * hh / h);
				triangle.setAux(v3, Base.texture0_id, 0, scaling * hh / h);
			}

			// triangle.setAux(v0, Base.texture0_id, 0,0);
			// triangle.setAux(v1, Base.texture0_id, 1,0);
			// triangle.setAux(v2, Base.texture0_id, 1,1);
			// triangle.setAux(v3, Base.texture0_id, 0,1);

			triangle.close();
		}

		public FontMetrics getFontMetrics() {
			return fontMetrics;
		}

		public Graphics2D getGraphics() {
			return g2;
		}

		public float getTextHeight() {
			return (float) (stringBounds.getHeight() / scaling);
		}

		public BaseFastRawTexture getTexture() {
			return slow;
		}

		public float getTextWidth() {
			return (float) (stringBounds.getWidth() / scaling);
		}

		public void off() {
			slow.setGL(gl);
			slow.post();
		}

		public void on() {
			assert gl != null;

			
//			;//System.out.println(" label on ---------");
			slow.setGL(gl);
			if (first) {
				;//System.out.println(" setup --------");
				slow.setup();
				first = false;
			}
			slow.pre();
		}

		public void resetText(String text, float backr, float backg, float backb, float backa, float frontr, float frontg, float frontb, float fronta) {
			this.text = text;
			FontMetrics metrics = g2.getFontMetrics(font);
			LineMetrics lineMetrics = metrics.getLineMetrics(text, g2);

			descent = lineMetrics.getDescent();
			ascent = lineMetrics.getAscent();
			stringBounds = metrics.getStringBounds(text, g2);

			g2.setColor(new Color(backr, backg, backb, backa));
			g2.fillRect(-Math.max(w, h) * 2, -Math.max(w, h) * 2, Math.max(w, h) * 4, Math.max(w, h) * 4);
			g2.setColor(new Color(frontr, frontg, frontb, fronta));

			g2.drawString(text, 0, (int) (ascent - offset / 2));

			DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
			data = dataBuffer.getData();
			intBuffer.put(data);
			intBuffer.rewind();
			slow.dirty();
		}

		public void resetTextAsLabel(String text, float backr, float backg, float backb, float backa, float frontr, float frontg, float frontb, float fronta) {

			this.text = text;
			FontMetrics metrics = g2.getFontMetrics(font);
			LineMetrics lineMetrics = metrics.getLineMetrics(text, g2);

			descent = lineMetrics.getDescent();
			ascent = lineMetrics.getAscent();
			stringBounds = metrics.getStringBounds(text, g2);

			stringBounds.setRect(stringBounds.getX(), stringBounds.getY(), stringBounds.getWidth(), stringBounds.getHeight() * 2);

			g2.setColor(new Color(backr, backg, backb, backa));
			g2.fillRect(-Math.max(w, h) * 2, -Math.max(w, h) * 2, Math.max(w, h) * 4, Math.max(w, h) * 4);
			// g2.fillRect(-400,-400,1800,1800);
			g2.setColor(new Color(frontr, frontg, frontb, fronta));

			JLabel l = new JLabel();
			l.setFont(font);
			View view = BasicHTML.createHTMLView(l, text);
			l.setFont(font);

			g2.setClip(-Math.max(w, h) * 2, -Math.max(w, h) * 2, Math.max(w, h) * 4, Math.max(w, h) * 4);
			view.setSize(Math.max(w, h), Math.max(w, h));
			view.paint(g2, new Rectangle(0, (int) (30 * rotation / (Math.PI / 2) + (int) (-offset / 2)), Math.max(w, h), Math.max(w, h)));

			// resetText(text, backr, backg, backb, backa, frontr,
			// frontg, frontb, fronta);
			// g2.drawString(text, 0, (int) (ascent-offset/2));

			DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
			data = dataBuffer.getData();
			intBuffer.put(data);
			intBuffer.rewind();
			slow.dirty();
		}

		public void resetTextAsLabelWithMaxSize(String text, float backr, float backg, float backb, float backa, float frontr, float frontg, float frontb, float fronta, float width) {

			this.text = text;
			FontMetrics metrics = g2.getFontMetrics(font);
			LineMetrics lineMetrics = metrics.getLineMetrics(text, g2);

			descent = lineMetrics.getDescent();
			ascent = lineMetrics.getAscent();
			stringBounds = metrics.getStringBounds(text, g2);

			stringBounds.setRect(stringBounds.getX(), stringBounds.getY(), stringBounds.getWidth(), stringBounds.getHeight() * 2);

			g2.setColor(new Color(backr, backg, backb, backa));
			g2.fillRect(-Math.max(w, h) * 2, -Math.max(w, h) * 2, Math.max(w, h) * 4, Math.max(w, h) * 4);
			// g2.fillRect(-400,-400,1800,1800);
			g2.setColor(new Color(frontr, frontg, frontb, fronta));

			JLabel l = new JLabel();
			l.setFont(font);
			View view = BasicHTML.createHTMLView(l, text);
			l.setFont(font);
			view.setSize(width, 1000);

			;//System.out.println(" view dimensions <" + view.getPreferredSpan(view.Y_AXIS) + "> width = <"+width+">");

			if ((int)(width*1.2f) != w || view.getPreferredSpan(view.Y_AXIS) != h) {
				;//System.out.println(" redeclaring label ");
				w = (int) (width*1.2f);
				h = (int) view.getPreferredSpan(view.Y_AXIS);
				image = dev.getDefaultConfiguration().createCompatibleImage(w, h, Transparency.TRANSLUCENT);

				g2 = (Graphics2D) image.getGraphics();
				setFont(font);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
				g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

				DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
				data = dataBuffer.getData();

				ob = ByteBuffer.allocateDirect(4 * w * h).order(ByteOrder.nativeOrder());
				intBuffer = ob.asIntBuffer();

				intBuffer.put(data);
				intBuffer.rewind();
				if (slow != null)
					slow.deallocate(atRenderTime);

				slow = (BaseFastRawTexture) new AdvancedTextures.BaseFastRawTexture(text, intBuffer, w, h, null).use_gl_texture_rectangle_ext(true);
			}
			l.setSize((int) width, h);
			view.setSize(width, h);

			g2.setClip(-Math.max(w, h) * 2, -Math.max(w, h) * 2, Math.max(w, h) * 4, Math.max(w, h) * 4);
			// view.setSize(Math.max(w, h), Math.max(w, h));
			view.paint(g2, new Rectangle(0, 0, (int) width, h));

			// resetText(text, backr, backg, backb, backa, frontr,
			// frontg, frontb, fronta);
			// g2.drawString(text, 0, (int) (ascent-offset/2));

			DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
			data = dataBuffer.getData();
			intBuffer.put(data);
			intBuffer.rewind();
			slow.dirty();
		}

		public void resetTextDropShadow(String text, float backr, float backg, float backb, float backa, float frontr, float frontg, float frontb, float fronta) {

			this.text = text;
			FontMetrics metrics = g2.getFontMetrics(font);
			LineMetrics lineMetrics = metrics.getLineMetrics(text, g2);

			descent = lineMetrics.getDescent();
			ascent = lineMetrics.getAscent();
			stringBounds = metrics.getStringBounds(text, g2);

			g2.setColor(new Color(backr, backg, backb, backa));
			g2.fillRect(-Math.max(w, h) * 2, -Math.max(w, h) * 2, Math.max(w, h) * 4, Math.max(w, h) * 4);
			g2.setColor(new Color(frontr, frontg, frontb, fronta));

			g2.drawString(text, 0, (int) (ascent - offset / 2));

			DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
			data = dataBuffer.getData();

			float a = 1 / 9f;

			PlanarImage dst = JAI.create("convolve", image, new KernelJAI(3, 3, new float[] { a / 2, a / 2, a / 2, a / 2, 1, a / 2, a / 2, a / 2, a / 2 }));
			PlanarImage dst1 = dst;
			for (int i = 0; i < 5; i++) {
				dst = JAI.create("convolve", dst, new KernelJAI(3, 3, new float[] { a, a, a, a, a, a, a, a, a }));
			}

			intBuffer.rewind();

			int[] blurBuffer = ((DataBufferInt) dst.getTiles()[0].getDataBuffer()).getData();
			int[] rawBuffer = ((DataBufferInt) dst1.getTiles()[0].getDataBuffer()).getData();

			for (int i = 0; i < blurBuffer.length; i++) {
				int xr = 16;
				int xg = 8;
				int xb = 0;
				int xa = 24;

				int blurRed = (blurBuffer[i] >> xr) & 255;
				int blurGreen = (blurBuffer[i] >> xg) & 255;
				int blurBlue = (blurBuffer[i] >> xb) & 255;
				int blurAlpha = (blurBuffer[i] >> xa) & 255;

				int rawRed = (rawBuffer[i] >> xr) & 255;
				int rawGreen = (rawBuffer[i] >> xg) & 255;
				int rawBlue = (rawBuffer[i] >> xb) & 255;
				int rawAlpha = (rawBuffer[i] >> xa) & 255;

				int or, og, ob, oa;

				// if (rawGreen > blurGreen) {
				// or = rawRed;
				// og = rawGreen;
				// ob = rawBlue;
				// oa = Math.min(255, 5 * blurAlpha);
				//
				// } else {
				// or = 0;
				// og = 0;
				// ob = 0;
				// oa = Math.min(255, 5 * blurAlpha);
				//
				// }

				or = blurRed;
				og = blurGreen;
				ob = blurBlue;
				oa = blurAlpha;

				int out = (or << xr) | (og << xg) | (ob << xb) | (oa << xa);

				intBuffer.put(out);
			}

			intBuffer.rewind();
			slow.dirty();
		}

		public void setFont(Font f) {
			if (scaling != 1)
				f = f.deriveFont(f.getSize() * scaling);
			this.font = f;
			g2.setFont(f);

		}

		public void setJustification(Justification j) {
			this.j = j;
		}
	}

	static public TextSystem textSystem;

	static public boolean textFlipped = false;

	private Object gl;

	private Object glu;

	private GraphicsDevice dev;

	TaskQueue atRenderTime = new TaskQueue();

	float scaling = 1;

	public TextSystem() {
		dev = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
	}

	public void beginRender(Object gl, Object glu) {
		this.gl = gl;
		this.glu = glu;
	}

	public void endRender() {
		atRenderTime.update();
		this.gl = null;
		this.glu = null;
	}

	public iLabel newLabel(int w, int h) {
		Label l = new Label((int) (w * scaling), (int) (h * scaling));
		return l;
	}

}
