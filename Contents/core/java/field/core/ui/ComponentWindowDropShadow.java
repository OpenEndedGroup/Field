package field.core.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.LinearGradientPaint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

import field.core.Platform;
import field.core.windowing.GLComponentWindow;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.BasicGeometry;
import field.graphics.core.BasicGLSLangProgram.SetIntegerUniform;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.graphics.core.TextSystem;
import field.graphics.core.TextSystem.AribitraryComponent;
import field.graphics.dynamic.DynamicMesh;
import field.launch.iUpdateable;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.iCoordinateFrame.iMutable;
import field.math.linalg.iToFloatArray;

public class ComponentWindowDropShadow {

	int width = 20;
	int height = 2560;

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

	public ComponentWindowDropShadow(final GLComponentWindow inside, TextSystem text) {
		this.inside = inside;
		inside.getPostQueue().addUpdateable(new iUpdateable() {
			public void update() {
				ComponentWindowDropShadow.this.paintNow();
			}
		});

		try {
			component = text.new AribitraryComponent(width, height);
		} catch (NullPointerException e) {

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

	int neutral = Platform.isMac() ? 200 : 220;

	Color c1 = new Color(neutral / 255f, neutral / 255f, neutral / 255f, 0f);
	Color c2 = new Color(0, 0, 0, 0.0f);
	Color c3 = new Color(neutral / 255f, neutral / 255f, neutral / 255f, 0.15f);

	protected void paintNow() {
		if (component==null) return;
		
		if (off)
			return;
		// if (true) return;

		int w = inside.getCanvas().getSize().x;
		int h = inside.getCanvas().getSize().y;
		int inset = 5;

		// if (!(h == lw))
		{

			lw = h;

			Graphics2D g2 = component.getG2();
			g2.clearRect(0, 0, width, height);
			g2.setComposite(AlphaComposite.Src);
			g2.setColor(new Color(0, 0, 0, 0.0f));
			g2.fillRect(0, 0, width, height);

			LinearGradientPaint paint = new LinearGradientPaint(new Point2D.Double(0, 0), new Point2D.Double(width, 0), new float[] { 0, 0.5f, 1 }, new Color[] { c1, c2, c3 });

			g2.setPaint(paint);
			g2.fillRect(0, 0, width, height);

			component.reupload();

		}

		component.drawIntoMesh(labelTriangle, 1, 1, 1, 1, w - width, 0);

		// component.drawIntoMesh(labelTriangle, 1, 1, 1, 1, 50, 50);

		component.on();
		labelTriangles.performPass(null);
		component.off();

		component.drawIntoMeshXFlipped(labelTriangle, 1, 1, 1, 1, 0, 0);

		// component.drawIntoMesh(labelTriangle, 1, 1, 1, 1, 50, 50);

		component.on();
		labelTriangles.performPass(null);
		component.off();

	}

	boolean off = false;

	public void off() {
		off = true;
	}

	public void on() {
		off = false;
	}

}
