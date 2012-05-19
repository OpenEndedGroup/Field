package field.core.ui.text.embedded;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Dimension2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Ellipse2D.Double;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JComponent;

import field.core.Constants;
import field.math.BaseMath;
import field.math.linalg.Vector2;
import field.math.util.Histogram;
import field.math.util.iAverage;

/** displays a histogram */

public class HistogramLayerPainter implements iMinimalLayerPainter {

	public enum DrawStyle {
		histogram, stripchart;
	}

	private Font font;

	Histogram<Number> forDisplayHistogram = new Histogram<Number>();

	Color baseColor = new Color(1f, 0.3f, 0.3f, 0.4f);

	DrawStyle drawStyle = DrawStyle.histogram;

	float minX = Float.POSITIVE_INFINITY;

	float maxX = Float.NEGATIVE_INFINITY;

	float minY = Float.POSITIVE_INFINITY;

	float maxY = Float.NEGATIVE_INFINITY;

	float maxYat = Float.NEGATIVE_INFINITY;

	int n = 0;

	public HistogramLayerPainter() {
	}

	public void associate(JComponent inside) {
	}

	public void paintNow(Graphics2D g, Dimension2D size) {
		if (drawStyle == DrawStyle.histogram)
			paintNowHistogram(g, size);
		else if (drawStyle == DrawStyle.stripchart) paintNowStripchart(g, size);
	}

	public void paintNowHistogram(Graphics2D g, Dimension2D size) {
		minX = Float.POSITIVE_INFINITY;
		maxX = Float.NEGATIVE_INFINITY;
		minY = Float.POSITIVE_INFINITY;
		maxY = Float.NEGATIVE_INFINITY;

		final List<Vector2> toDraw = new ArrayList<Vector2>();

		maxYat = 0;

		forDisplayHistogram.average(new iAverage<Number, Object>(){

			public <X extends Number> void accept(X accept, double weight) {
				if (accept.floatValue() > maxX) maxX = accept.floatValue();
				if (accept.floatValue() < minX) minX = accept.floatValue();
				if (weight > maxY) {
					maxY = (float) weight;
					maxYat = (accept.floatValue());
				}
				if (weight < minY) minY = (float) weight;

				toDraw.add(new Vector2(accept.floatValue(), weight));

			}

			public void begin(int num, double totalWeight) {
			}

			public Object end() {
				return null;
			}
		});

		if (maxX == minX) maxX = minX + 1;
		if (maxY == minY) maxY = minY + 1;

		Collections.sort(toDraw, new Comparator<Vector2>(){
			public int compare(Vector2 o1, Vector2 o2) {
				return o1.x < o2.x ? -1 : 1;
			}
		});

		for (int i = 0; i < toDraw.size(); i++) {
			float nx = (toDraw.get(i).x - minX) / (maxX - minX);
			float ny = (toDraw.get(i).y - 0) / (maxY - 0);

			float prevx = (i > 0 ? ((toDraw.get(i - 1).x - minX) / (maxX - minX) + nx) / 2 : 0);
			float nextx = (i < toDraw.size() - 1 ? ((toDraw.get(i + 1).x - minX) / (maxX - minX) + nx) / 2 : 1);

			draw(prevx, nextx, nx, ny, g, size, toDraw.get(i).x, toDraw.get(i).y);
		}

		if (font == null) {
			font = new Font(Constants.defaultFont, Font.PLAIN, 8);
		}

		{

			String label = "d:  " + BaseMath.toDP(minX, 3) + "";
			g.setFont(font);
			g.setColor(baseColor);
			int w = g.getFontMetrics(font).charsWidth(label.toCharArray(), 0, label.length());
			g.drawString(label, 10, (int) (size.getHeight() - 10));
		}
		{

			String label = "d: " + BaseMath.toDP(maxX, 3) + "";
			g.setFont(font);
			g.setColor(baseColor);
			int w = g.getFontMetrics(font).charsWidth(label.toCharArray(), 0, label.length());
			g.drawString(label, (int) (size.getWidth() - 10 - w), (int) (size.getHeight() - 10));
		}
		{

			String label = "r: " + BaseMath.toDP(maxY, 3) + "";
			g.setFont(font);
			g.setColor(baseColor);
			int w = g.getFontMetrics(font).charsWidth(label.toCharArray(), 0, label.length());
			g.drawString(label, (int) ((maxYat - minX) / (maxX - minX) * size.getWidth() + 10), (10));
		}

	}

	public void paintNowStripchart(Graphics2D g, Dimension2D size) {
		minX = Float.POSITIVE_INFINITY;
		maxX = Float.NEGATIVE_INFINITY;
		minY = Float.POSITIVE_INFINITY;
		maxY = Float.NEGATIVE_INFINITY;

		final List<Vector2> toDraw = new ArrayList<Vector2>();

		maxYat = 0;

		forDisplayHistogram.average(new iAverage<Number, Object>(){

			public <X extends Number> void accept(X accept, double weight) {
				if (accept.floatValue() > maxX) maxX = accept.floatValue();
				if (accept.floatValue() < minX) minX = accept.floatValue();
				if (weight > maxY) {
					maxY = (float) weight;
					maxYat = (accept.floatValue());
				}
				if (weight < minY) minY = (float) weight;

				toDraw.add(new Vector2(accept.floatValue(), weight));

			}

			public void begin(int num, double totalWeight) {
			}

			public Object end() {
				return null;
			}
		});

		if (maxX == minX) maxX = minX + 1;
		if (maxY == minY) maxY = minY + 1;

		Collections.sort(toDraw, new Comparator<Vector2>(){
			public int compare(Vector2 o1, Vector2 o2) {
				return o1.x < o2.x ? -1 : 1;
			}
		});

		for (int i = 0; i < toDraw.size(); i++) {
			float nx = (toDraw.get(i).x - minX) / (maxX - minX);
			float ny = (toDraw.get(i).y - 0) / (maxY - 0);

			float prevx = (i > 0 ? ((toDraw.get(i - 1).x - minX) / (maxX - minX) )  : 0);
			float prevy = (i > 0 ? ((toDraw.get(i - 1).y - 0) / (maxY - 0))  : 0);
			float nextx = (i < toDraw.size() - 1 ? ((toDraw.get(i + 1).x - minX) / (maxX - minX) )  : 1);
			float nexty = (i < toDraw.size() - 1 ? ((toDraw.get(i + 1).y - 0) / (maxY ) )  : 1);

			draw(prevx, prevy, nextx, nexty, nx, ny, g, size, toDraw.get(i).x, toDraw.get(i).y);
		}

		if (font == null) {
			font = new Font(Constants.defaultFont, Font.PLAIN, 8);
		}

		{

			String label = "d:  " + BaseMath.toDP(minX, 3) + "";
			g.setFont(font);
			g.setColor(baseColor);
			int w = g.getFontMetrics(font).charsWidth(label.toCharArray(), 0, label.length());
			g.drawString(label, 10, (int) (size.getHeight() - 10));
		}
		{

			String label = "d: " + BaseMath.toDP(maxX, 3) + "";
			g.setFont(font);
			g.setColor(baseColor);
			int w = g.getFontMetrics(font).charsWidth(label.toCharArray(), 0, label.length());
			g.drawString(label, (int) (size.getWidth() - 10 - w), (int) (size.getHeight() - 10));
		}
		{

			String label = "r: " + BaseMath.toDP(maxY, 3) + "";
			g.setFont(font);
			g.setColor(baseColor);
			int w = g.getFontMetrics(font).charsWidth(label.toCharArray(), 0, label.length());
			g.drawString(label, (int) ((maxYat - minX) / (maxX - minX) * size.getWidth() + 10), (10));
		}

	}

	public iMinimalLayerPainter setHistogram(Histogram<Number> r) {
		forDisplayHistogram = r;
		return this;
	}

	private void draw(float prevx, float prevy, float nextx, float nexty, float nx, float ny, Graphics2D g, Dimension2D size, float vx, float vy) {
		prevx *= size.getWidth();
		nextx *= size.getWidth();
		prevy *= size.getHeight();
		nexty *= size.getHeight();
		nx *= size.getWidth();
		ny *= size.getHeight();

		g.setStroke(new BasicStroke(1));

		GeneralPath p = new GeneralPath();
		p.moveTo(prevx, (float) (size.getHeight() - prevy));
		p.lineTo(nx, (float) (size.getHeight() - ny));
		p.lineTo(nextx, (float) (size.getHeight() - nexty));

		g.setColor(new Color(0.8f * baseColor.getRed() / 255, 0.85f * baseColor.getGreen() / 255, 0.82f * baseColor.getBlue() / 255, 0.5f));
		g.draw(p);
		g.setStroke(new BasicStroke(1));


		g.setStroke(new BasicStroke(0.5f, 1, 1, 4, new float[] { 5, 10}, 0));

		p = new GeneralPath();
		p.moveTo(nx, (float) (size.getHeight() - ny));
		p.lineTo(nx, 0);

		g.setColor(new Color(0.8f * baseColor.getRed() / 255, 0.85f * baseColor.getGreen() / 255, 0.82f * baseColor.getBlue() / 255, 0.2f));
		g.draw(p);

		p = new GeneralPath();
		p.moveTo(0, (float) (size.getHeight()-ny));
		p.lineTo(nx, (float) (size.getHeight()-ny));

		g.setColor(new Color(0.8f * baseColor.getRed() / 255, 0.85f * baseColor.getGreen() / 255, 0.82f * baseColor.getBlue() / 255, 0.2f));
		g.draw(p);

		float w = 3;
		Double dd = new Ellipse2D.Double(nx - w / 2, size.getHeight() - ny - w / 2, w, w);

		g.setColor(new Color(0.8f * baseColor.getRed() / 255, 0.85f * baseColor.getGreen() / 255, 0.82f * baseColor.getBlue() / 255, 0.5f));
		g.fill(dd);
		g.setColor(new Color(0.8f * baseColor.getRed() / 255, 0.85f * baseColor.getGreen() / 255, 0.82f * baseColor.getBlue() / 255, 0.15f));
		g.draw(dd);

		g.setFont(font);
		g.setColor(baseColor);
		g.drawString(BaseMath.toDP(vx,2)+"/"+BaseMath.toDP(vy, 2), (prevx+nextx)/2, (float) (size.getHeight()/2- ny/2));

	}

	protected void draw(float prevx, float nextx, float nx, float ny, Graphics2D g, Dimension2D size, float vx, float vy) {

		prevx *= size.getWidth();
		nextx *= size.getWidth();
		nx *= size.getWidth();
		ny *= size.getHeight();

		g.setStroke(new BasicStroke(1));

		GeneralPath p = new GeneralPath();
		p.moveTo(prevx, (float) (size.getHeight() - 0));
		p.lineTo(prevx, (float) (size.getHeight() - ny));
		p.lineTo(nextx, (float) (size.getHeight() - ny));
		p.lineTo(nextx, (float) (size.getHeight() - 0));

		g.setColor(new Color(0.8f * baseColor.getRed() / 255, 0.85f * baseColor.getGreen() / 255, 0.82f * baseColor.getBlue() / 255, 0.5f));
		g.draw(p);
		g.setColor(new Color(0.8f * baseColor.getRed() / 255, 0.85f * baseColor.getGreen() / 255, 0.82f * baseColor.getBlue() / 255, 0.15f));
		g.fill(p);

		float w = 3;
		Double dd = new Ellipse2D.Double(nx - w / 2, size.getHeight() - ny - w / 2, w, w);

		g.setColor(new Color(0.8f * baseColor.getRed() / 255, 0.85f * baseColor.getGreen() / 255, 0.82f * baseColor.getBlue() / 255, 0.5f));
		g.fill(dd);
		g.setColor(new Color(0.8f * baseColor.getRed() / 255, 0.85f * baseColor.getGreen() / 255, 0.82f * baseColor.getBlue() / 255, 0.15f));
		g.draw(dd);

		g.setStroke(new BasicStroke(0.5f, 1, 1, 4, new float[] { 5, 10}, 0));

		p = new GeneralPath();
		p.moveTo(nx, (float) (size.getHeight() - 0));
		p.lineTo(nx, 0);

		g.setColor(new Color(0.8f * baseColor.getRed() / 255, 0.85f * baseColor.getGreen() / 255, 0.82f * baseColor.getBlue() / 255, 0.2f));
		g.draw(p);
		p = new GeneralPath();
		p.moveTo(0, (float) (size.getHeight() - ny));
		p.lineTo((float)size.getWidth(), (float)size.getHeight() - ny);

		g.draw(p);

		g.setFont(font);
		g.setColor(baseColor);
		g.drawString(BaseMath.toDP(vx,2)+"/"+BaseMath.toDP(vy, 2), (prevx+nextx)/2, (float) (size.getHeight()/2- ny/2));

		g.setStroke(new BasicStroke(1));
	}

}
