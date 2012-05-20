package field.core.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

import javax.swing.JLabel;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

import field.core.Constants;
import field.core.Platform;
import field.core.windowing.GLComponentWindow;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.BasicGeometry;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.graphics.core.TextSystem;
import field.graphics.core.TextSystem.AribitraryComponent;
import field.graphics.dynamic.DynamicMesh;
import field.launch.iUpdateable;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.iCoordinateFrame.iMutable;
import field.math.linalg.iToFloatArray;

public class ComponentWindowStatusbar {
	
	int width = 2560;
	int height = 20;
	
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
	
	public ComponentWindowStatusbar(final GLComponentWindow inside, TextSystem text) {
		this.inside = inside;
		inside.getPostQueue().addUpdateable(new iUpdateable() {
			public void update() {
				ComponentWindowStatusbar.this.paintNow();
			}
		});

		try{
			component = text.new AribitraryComponent(width, height);
		}
		catch(NullPointerException e)
		{
			
		}
		labelTriangles = new BasicGeometry.TriangleMesh(new field.math.abstraction.iInplaceProvider<iMutable>() {
			public iMutable get(iMutable o) {
				return new CoordinateFrame();
			}
		});
		labelTriangle = new DynamicMesh(labelTriangles);
		
		dcvertex = new BasicGLSLangProgram("content/shaders/DCvertex_status.glslang", "content/shaders/Texture2DRectTimesDiffuseFragment_status.glslang");
		dcvertex.new SetUniform("screenDimensions2", new iToFloatArray() {
			
			public float[] get() {
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
		dcvertex.new SetIntegerUniform("tex", 0);
		
		labelTriangles.addChild(dcvertex);
	}
	
	int lw = 0;
	String lt = "";
	
	protected boolean backgroundHeavy = false;
	
	protected void paintNow() {
		if (off) return;
		//		if (true) return;
		
		if (component==null) return;
		
		int w = inside.getCanvas().getSize().x;
		int h = inside.getCanvas().getSize().y;
		int inset = 5;
		String tt = getText();
		
		
		if (!(w == lw && tt.equals(lt))) 
		{
			
			lw = w;
			lt = tt;
			
			Graphics2D g2 = component.getG2();
			g2.clearRect(0, 0, width, height);
//			g2.setComposite(AlphaComposite.Src);
//			g2.setColor(new Color(0, 0, 0, 0.0f));
//			g2.fillRect(0, 0, width, height);
//			
//			g2.setComposite(AlphaComposite.SrcOver);
			g2.setColor(new Color(0, 0, 0, 0.2f));
//			g2.fillRect(0, 0, w - inset * 2, height);
			
			float a = 1f;
			float b = 0.7f;
			
			LinearGradientPaint paint = new LinearGradientPaint(new Point2D.Double(0, 0), new Point2D.Double(0, height), new float[] { 0, 0.85f, 1 }, new Color[] { new Color(a, a, a, 0.5f), new Color((a + b) / 2, (a + b) / 2, (a + b) / 2, 0.5f), new Color(b, b, b, 0.5f) });
			
			g2.setPaint(paint);
			
			if (backgroundHeavy)
				g2.setColor(new Color(1,1,1, 0.75f));
			
			g2.fillRect(0, 0, w - inset * 2, height);
			
			g2.setColor(new Color(0, 0, 0, 0.75f));
			g2.setFont(new Font(Constants.defaultFont, 0, 12));
			
			JLabel l = new JLabel();
			l.setFont(font);
			View view = BasicHTML.createHTMLView(l, tt);
			l.setFont(font);
			view.setSize(width, height);
			g2.setClip(0, 0, width, height);
			
			
			view.paint(g2, new Rectangle(5, 2, w - inset * 2-5, height));
			
			//			g2.setColor(new Color(1,0,0,1f));
			//			g2.fillRect(-1000,-1000,3000,3000);
			
			
			;//System.out.println(" reuploading texture ");
			component.reupload();
			;//System.out.println(" reuploading texture complete ");
			
		}
		
		component.drawIntoMesh(labelTriangle, 1, 1, 1, 1, inset, (Platform.isMac() ? 0 : -30)+h - inset - height);
		//		component.drawIntoMesh(labelTriangle, 1, 1, 1, 1, 50, 50);
		
		component.on();
		labelTriangles.performPass(null);
		component.off();
		
		
	}
	
	protected String getText() {
		return "<html><font face='" + Constants.defaultFont + "' size=-3><p align='right'><b>banana</b> <i>pear</i>";
	}
	
	boolean off = false;

	public void off() {
		off = true;
	}

	public void on() {
		off = false;
	}
	
}
