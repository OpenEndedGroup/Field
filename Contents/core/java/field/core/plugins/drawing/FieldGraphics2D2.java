package field.core.plugins.drawing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.drawing.text.AdvancedTextToCachedLine;
import field.math.linalg.Vector4;
import field.util.Dict;

public class FieldGraphics2D2 extends AbstractGraphics2D {

	List<CachedLine> geometry = new ArrayList<CachedLine>();
	private GraphicsDevice dev;
	private BufferedImage image;
	private Graphics2D g_fake;

	public FieldGraphics2D2() {
		super(true);
		gc = new GraphicContext();

		dev = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		image = dev.getDefaultConfiguration().createCompatibleImage(1,1);
		g_fake = (Graphics2D) image.getGraphics();

	}


	boolean strokeQuality = false;
	BasicStroke defaultStroke = new BasicStroke();

	@Override
	public void draw(Shape s) {

		new Exception().printStackTrace();
		
		boolean needsTransform = true;

//		if (gc.clip != null) {
//			Area a = new Area(gc.clip);
//			Area b = new Area(s);
//			b.transform(gc.transform);
//			a.intersect(b);
//			s = new GeneralPath(a);
//			needsTransform = false;
//		}

		float w = 1;
		if (gc.stroke != null) {
			if (gc.stroke instanceof BasicStroke) {
				w = ((BasicStroke) gc.stroke).getLineWidth();
			}
			if (strokeQuality && !gc.stroke.equals(defaultStroke)) {
				Shape ss = gc.stroke.createStrokedShape(s);
				AffineTransform was = gc.transform;
				if (!needsTransform)
					gc.transform = new AffineTransform();

//				System.out.println(" -- drawing line with stroke <"+gc.stroke+">");
				
				fill(ss);
				gc.transform = was;

				geometry.get(geometry.size() - 1).getProperties().put(iLinearGraphicsContext.windingRule, 1f);

				return;
			}
		}

		CachedLine c = new LineUtils().piToCachedLine(s.getPathIterator(needsTransform ? gc.getTransform() : null));

		if (c == null)
			return;

		Dict p = c.getProperties();
		p.put(iLinearGraphicsContext.stroked, true);
		if (gc.getColor() != null)
			p.put(iLinearGraphicsContext.strokeColor, new Vector4(gc.getColor().getRed() / 255f, gc.getColor().getGreen() / 255f, gc.getColor().getBlue() / 255f, gc.getColor().getAlpha() / 255f));
		p.put(iLinearGraphicsContext.thickness, w);

		geometry.add(c);

	}

	@Override
	public void fill(Shape s) {

		new Exception().printStackTrace();

		boolean needsTransform = true;

//		if (gc.clip != null) {
//			Area a = new Area(gc.clip);
//			Area b = new Area(s);
//			b.transform(gc.transform);
//			a.intersect(b);
//			s = new GeneralPath(a);
//			needsTransform = false;
//		}

		CachedLine c = new LineUtils().piToCachedLine(s.getPathIterator(needsTransform ? gc.getTransform() : null));

		if (c == null)
			return;

		Dict p = c.getProperties();
		p.put(iLinearGraphicsContext.stroked, false);
		p.put(iLinearGraphicsContext.filled, true);

		System.out.println(" inside background <" + gc.getBackground() + "> <" + gc.getPaint() + "> <" + gc.getColor() + ">, shape is <" + s + " " + s.getBounds() + ">");

		if (gc.getColor() != null)
			p.put(iLinearGraphicsContext.fillColor, new Vector4(gc.getColor().getRed() / 255f, gc.getColor().getGreen() / 255f, gc.getColor().getBlue() / 255f, gc.getColor().getAlpha() / 255f));

		geometry.add(c);
		System.out.println(" geometry now <" + geometry.size() + ">");

	}

	@Override
	public void drawRenderableImage(RenderableImage arg0, AffineTransform arg1) {
	}

	@Override
	public void drawRenderedImage(RenderedImage arg0, AffineTransform arg1) {
	}

	@Override
	public void drawString(String arg0, float arg1, float arg2) {
		
		AdvancedTextToCachedLine atcl = new AdvancedTextToCachedLine(getFont());
		CachedLine g = atcl.getLine(arg0, arg1, arg2);
		
		AffineTransform t = gc.getTransform();
		
		System.out.println(" current transform is <"+t+">");
		
		g = new LineUtils().transformLine(g, t);

		Dict p = g.getProperties();
		p.put(iLinearGraphicsContext.stroked, true);
		p.put(iLinearGraphicsContext.filled, true);

		if (gc.getColor() != null)
		{
			p.put(iLinearGraphicsContext.fillColor, new Vector4(gc.getColor().getRed() / 255f, gc.getColor().getGreen() / 255f, gc.getColor().getBlue() / 255f, gc.getColor().getAlpha() / 255f));
			p.put(iLinearGraphicsContext.strokeColor, new Vector4(gc.getColor().getRed() / 255f, gc.getColor().getGreen() / 255f, gc.getColor().getBlue() / 255f, 0.65f*gc.getColor().getAlpha() / 255f));
		}		
		
		geometry.add(g);
	}

	@Override
	public void drawString(AttributedCharacterIterator arg0, float arg1, float arg2) {
		char m = arg0.first();
		String a = "";
		while (m!=AttributedCharacterIterator.DONE)
		{
			a+=m;
			m = arg0.next();
		}
		if (a.length()>0)
		{
			drawString(a, arg1, arg2);
		}
	}


	@Override
	public void copyArea(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
	}

	@Override
	public Graphics create() {
		FieldGraphics2D2 f = new FieldGraphics2D2();
		f.gc = (GraphicContext) gc.clone();
		f.geometry = geometry;
		return f;
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean drawImage(Image arg0, int arg1, int arg2, ImageObserver arg3) {
		return false;
	}

	@Override
	public boolean drawImage(Image arg0, int arg1, int arg2, int arg3, int arg4, ImageObserver arg5) {
		return false;
	}


	@Override
	public void setXORMode(Color arg0) {
	}


	@Override
	public GraphicsConfiguration getDeviceConfiguration() {
		return null;
	}

	@Override
	public void setRenderingHints(Map arg0) {
		
	}

	@Override
	public FontMetrics getFontMetrics(Font arg0) {
		return g_fake.getFontMetrics(arg0);
	}

}
