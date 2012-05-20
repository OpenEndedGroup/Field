package field.extras.typing;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.LinearGradientPaint;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

import field.core.Constants;
import field.core.windowing.GLComponentWindow;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.BasicGeometry;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.graphics.core.TextSystem;
import field.graphics.core.TextSystem.AribitraryComponent;
import field.graphics.dynamic.DynamicMesh;
import field.launch.iUpdateable;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Vector2;
import field.math.linalg.iCoordinateFrame.iMutable;
import field.math.linalg.iToFloatArray;

public class TypingOverlay {

	int width = 1024;
	int height = 128;

	private GraphicsDevice dev;
	private BufferedImage image;
	private Graphics2D g2;
	private Font font;
	private int[] data;
	private IntBuffer intBuffer;
	private TriangleMesh labelTriangles;
	private DynamicMesh labelTriangle;
	private final GLComponentWindow inside;
	private AribitraryComponent component;
	private BasicGLSLangProgram dcvertex;

	public TypingOverlay(final GLComponentWindow inside, TextSystem text) {
		this.inside = inside;
		inside.getPostQueue().addUpdateable(new iUpdateable() {
			public void update() {
				TypingOverlay.this.paintNow();
			}
		});

		component = text.new AribitraryComponent(width, height);

		labelTriangles = new BasicGeometry.TriangleMesh(new field.math.abstraction.iInplaceProvider<iMutable>() {
			public iMutable get(iMutable o) {
				return new CoordinateFrame();
			}
		});
		labelTriangle = new DynamicMesh(labelTriangles);

		dcvertex = new BasicGLSLangProgram("content/shaders/DCvertex.glslang", "content/shaders/Texture2DRectTimesDiffuseFragment.glslang");
		dcvertex.new SetUniform("screenDimensions", new iToFloatArray() {

			public float[] get() {
				;//;//System.out.println(" dimensions are <" + inside.getCanvas().getSize().x + "> <" + inside.getCanvas().getSize().y + ">");

				return new float[] { 2f / inside.getCanvas().getSize().x, -2f / inside.getCanvas().getSize().y, 1, -1 };
			}
		});
		dcvertex.new SetUniform("add", new iToFloatArray() {

			public float[] get() {
				return new float[] { 0, 0, 0, 0 };
			}
		});
		dcvertex.new SetUniform("mul", new iToFloatArray() {

			public float[] get() {
				return new float[] { 1, 1, 1, 1 };
			}
		});
		dcvertex.new SetUniform("fade", new iToFloatArray() {

			public float[] get() {
				return new float[] { 0, 0, 0, 0 };
			}
		});

		labelTriangles.addChild(dcvertex);
	}

	int lw = 0;
	String lt = "(nothing)";
	private Rectangle2D bounds;

	Vector2 popupPoint = null;
	
	protected void paintNow() {
		int w = inside.getCanvas().getSize().x;
		int h = inside.getCanvas().getSize().y;
		int inset = 5;
		String tt = getText();
		
		if (tt.length()==0) return;
		
		;//;//System.out.println(" text is <"+tt+">");
		
		if (!(w == lw && tt.equals(lt))) {

			lw = w;
			lt = tt;

			Graphics2D g2 = component.getG2();
			g2.clearRect(0, 0, width, height);
			g2.setComposite(AlphaComposite.Src);
			g2.setColor(new Color(0, 0, 0, 0.0f));
			g2.fillRect(0, 0, width, height);

			g2.setComposite(AlphaComposite.SrcOver);
			g2.setColor(new Color(0, 0, 0, 0.2f));

			Font ff = new Font(Constants.defaultFont, 0, getFontSize(g2, tt));
			g2.setFont(ff);
			
			bounds = g2.getFontMetrics(ff).getStringBounds(tt, g2);
			g2.fillRect(0, 0, (int)bounds.getWidth()+inset*2, height);
			
			float a = 0.86f;
			float b = 0.65f;

			LinearGradientPaint paint = new LinearGradientPaint(new Point2D.Double(0, 0), new Point2D.Double(0, height), new float[] { 0, 0.85f, 1 }, new Color[] { new Color(a, a, a, 0.5f), new Color((a + b) / 2, (a + b) / 2, (a + b) / 2, 0.5f), new Color(b, b, b, 0.5f) });

			g2.setPaint(paint);


			g2.fillRect(0, 0, (int)bounds.getWidth()+inset*2, height);
			
			g2.setColor(new Color(0, 0, 0, 0.75f));
			g2.drawString(tt, 0, height-g2.getFontMetrics().getMaxDescent()-inset);

			component.reupload();

		}
		component.drawIntoMesh(labelTriangle, 1, 1, 1, 1, (float) (w/2-(bounds.getWidth()-inset*2)/2), h - 50 - height);

		popupPoint = new Vector2(w/2+(bounds.getWidth()-inset*2)/2, h - 50 - height/2);
		
		component.on();
		labelTriangles.performPass(null);
		component.off();

	}
	
	
	

	private int getFontSize(Graphics2D g2, String tt) {
		
		float fontSize = 200;
		Font f = new Font(Constants.defaultFont, 0, (int)fontSize);
		Rectangle2D bounds = g2.getFontMetrics(f).getStringBounds(tt, g2);
		while((bounds.getWidth()>width-10 || bounds.getHeight()>height-10) && fontSize>10)
		{
		
			fontSize *= 0.9;
			f = f.deriveFont(fontSize);
			bounds = g2.getFontMetrics(f).getStringBounds(tt, g2);
		}
		return (int)fontSize;
	}




	protected String getText() {
		return "bipity";
	}

}
