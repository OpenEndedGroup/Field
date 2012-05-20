package field.core.plugins.drawing.opengl;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.Collection;

import org.eclipse.swt.widgets.Event;

import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.opengl.LineInteraction.EventHandler;
import field.graphics.core.BasicCamera;
import field.graphics.core.BasicCamera.Projector;
import field.namespace.generic.Generics.Pair;
import field.util.Dict.Prop;

public class LineInteraction3d extends LineInteraction {

	protected final BasicCamera camera;
	protected Projector projector;

	public LineInteraction3d(BasicCamera camera)
	{
		this.camera = camera;
	}
	
	@Override
	public void setAllCachedLines(Collection<CachedLine> c) {
		super.setAllCachedLines(c);
		Projector projector = camera.getState().getProjector();
		
		if (this.projector==null || !this.projector.equals(projector))
			intersectionCache.clear();
		
		this.projector = projector;
	}
	
	public static final Prop<CachedLine> areaForEventHandler = new Prop<CachedLine>("areaForEventHandler");

	protected Pair<CachedLine, EventHandler> locate(Event arg0) {
		float x = 0;
		Pair<CachedLine, EventHandler> xIs = null;
		if (all == null)
			return null;

		float r = 5;
		Rect hit = new Rect(arg0.x - r, arg0.y - r, 2 * r, 2 * r);
		Rectangle2D.Double d = new Rectangle2D.Double(hit.x, hit.y, hit.w, hit.h);
		Area a2 = new Area(d);

		for (CachedLine cc : all) {
			// should be scaled by viewport scaling.
			if (cc.properties != null) {
				EventHandler ev = cc.getProperties().get(eventHandler);
				if (ev != null) {

					Area cachedArea = getCachedArea(cc);
					
					;//System.out.println(" cachedArea for event is <"+cachedArea+">");
					
					if (cachedArea==null) continue;
					
					Area dd = new Area(cachedArea);
					dd.intersect(a2);

					Rectangle res = dd.getBounds();
					float amount = (float) ((res.width * res.height));
					
					;//System.out.println(" intersection amount is <"+amount+">");
					
					if (amount > x) {
						x = amount;
						xIs = new Pair<CachedLine, EventHandler>(cc, ev);
					}
				}
			}
		}
				
		return xIs;
	}

	protected Area getCachedArea(CachedLine cc) {
		
		;//System.out.println(" projector is :"+projector);
		
		CachedLine oc = cc.getProperties().get(areaForEventHandler);
		if (oc==null) oc = cc;
		
		Area a = intersectionCache.get(System.identityHashCode(oc));
		
		if (a == null) {
			
			GeneralPath gp = new LineUtils().lineToGeneralPathProjected(oc, projector, camera.width, camera.height);
			gp = filter(gp);
			if (gp==null) return null;
			intersectionCache.put(System.identityHashCode(oc), a = new Area(gp));
		}
//		if (a!=null && a.getBounds().width*a.getBounds().height>camera.width*camera.height) return null;
		return a;
	}

	protected GeneralPath filter(GeneralPath gp) {
		return gp;
	}


}
