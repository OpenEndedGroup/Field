package field.core.plugins.drawing;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.plugins.drawing.opengl.iLine_m;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.windowing.GLComponentWindow;
import field.launch.Launcher;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;

public class OverDrawing {
	
	static public VisualElementProperty<OverDrawing> overdraw = new VisualElementProperty<OverDrawing>("__overdraw");
	private final BaseGLGraphicsContext context;
	private final GLComponentWindow window;
	
	
	public OverDrawing(iVisualElement root, GLComponentWindow window, BaseGLGraphicsContext context)
	{
		this.window = window;
		this.context = context;
		overdraw.set(root, root, this);	
	}
	
	public void draw(PaintEvent e, Control c)
	{
		
		Set<CachedLine> a = context.getAllLines();
		
		Set<CachedLine> bleeding = new LinkedHashSet<CachedLine>();
		for(CachedLine aa : a)
		{
			if (aa.getProperties().isTrue(iLinearGraphicsContext.bleedsOntoTextEditor, false))
			{
				bleeding.add(aa);
			}
		}

		for(CachedLine aa : bleeding)
		{
			bleed(aa, e, c);
		}
	}

	private void bleed(CachedLine aa, PaintEvent e, Control c) {
		
		GC gc = e.gc;
		
		Point top = Launcher.getLauncher().display.map(c, window.getFrame(), new Point(0,0));
		Point topC = Launcher.getLauncher().display.map(window.getCanvas(), window.getFrame(), new Point(0,0));
		
		Path p = new Path(Launcher.getLauncher().display);
		
		List<Event> events = aa.events;
		for (Event ee : events) {
			
			if (ee.method.equals(iLine_m.moveTo_m))
			{
				Vector2 a = transform(ee.getAt(), top, topC);
				p.moveTo(a.x, a.y);
			}
			else if (ee.method.equals(iLine_m.lineTo_m))
			{
				Vector2 a = transform(ee.getAt(), top, topC);
				p.lineTo(a.x, a.y);				
			}
			else if (ee.method.equals(iLine_m.cubicTo_m))
			{
				Vector2 a0 = transform(ee.getAt(0), top, topC);
				Vector2 a1 = transform(ee.getAt(1), top, topC);
				Vector2 a2 = transform(ee.getAt(2), top, topC);
				p.cubicTo(a0.x, a0.y, a1.x, a1.y, a2.x, a2.y);	
			}
		}
		
		gc.setForeground(Launcher.getLauncher().display.getSystemColor(SWT.COLOR_BLACK));
		
		if (aa.getProperties().isTrue(iLinearGraphicsContext.filled, false))
		{
			gc.setBackground(toColor(aa.getProperties().get(iLinearGraphicsContext.fillColor), aa.getProperties().get(iLinearGraphicsContext.color)));
			gc.setAlpha(toAlpha(aa.getProperties().get(iLinearGraphicsContext.fillColor), aa.getProperties().get(iLinearGraphicsContext.color)));
			gc.fillPath(p);
		}
		if (aa.getProperties().isTrue(iLinearGraphicsContext.stroked, true))
		{
			gc.setForeground(toColor(aa.getProperties().get(iLinearGraphicsContext.fillColor), aa.getProperties().get(iLinearGraphicsContext.color)));
			gc.setAlpha(toAlpha(aa.getProperties().get(iLinearGraphicsContext.fillColor), aa.getProperties().get(iLinearGraphicsContext.color)));
			gc.drawPath(p);
		}
	}

	private Color toColor(Vector4 a, Vector4 b) {
		if (a==null) a = b;
		if (b==null) return Launcher.getLauncher().display.getSystemColor(SWT.COLOR_BLACK);
		
		return new Color(Launcher.getLauncher().display, (int)Math.max(0, Math.min(255, 255*a.x)), (int)Math.max(0, Math.min(255, 255*a.y)), (int)Math.max(0, Math.min(255, 255*a.z)));
	}

	private int toAlpha(Vector4 a, Vector4 b) {
		if (a==null) a = b;
		if (b==null) return 255;
		
		return (int)Math.max(0, Math.min(255, 255*a.w));
	}

	private Vector2 transform(Vector2 at, Point top, Point topC) {
		
		at.x = (at.x-window.getDXTranslation())/window.getDXScale()+topC.x-top.x;
		at.y = (at.y-window.getDYTranslation())/window.getDYScale()+topC.y-top.y;
		return at;
	}
	
}
