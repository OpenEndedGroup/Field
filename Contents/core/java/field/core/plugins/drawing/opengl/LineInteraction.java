package field.core.plugins.drawing.opengl;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.execution.PythonScriptingSystem;
import field.core.execution.PythonScriptingSystem.Promise;
import field.core.ui.SmallMenu;
import field.core.ui.SmallMenu.BetterPopup;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.components.RootComponent.iMousePeer;
import field.launch.iUpdateable;
import field.namespace.generic.Generics.Pair;
import field.util.Dict;
import field.util.Dict.Prop;
import field.util.PythonUtils;

public class LineInteraction implements iMousePeer {

	static public abstract class EventHandler {
		public void begin() {
		}

		public boolean enter(Event e) {
			return false;
		}

		public boolean exit(Event e) {
			return false;
		}

		public boolean moved(Event e) {
			return false;
		}

		public boolean down(Event e) {
			return false;
		}

		public boolean up(Event e) {
			return false;
		}

		public boolean drag(Event e) {
			return false;
		}
		
		public boolean scroll(Event e)
		{
			return false;
		}

		public void popupMenu(Event event, Map<String, iUpdateable> source) {

			// this might not actually be a map<string,
			// iUpdateable>, we might have something from Python
			LinkedHashMap<String, iUpdateable> menu = new LinkedHashMap<String, iUpdateable>();

			Set<Entry<String, iUpdateable>> es = source.entrySet();
			for (Entry<String, iUpdateable> e : es) {

				Object u = e.getValue();

				if (u != null) {
					final iUpdateable c = PythonUtils.installed.toUpdateable(u);

					iUpdateable c2 = new iUpdateable() {

						public void update() {
							resyncMouse();
							c.update();
						}
					};

					menu.put(e.getKey(), c2);
				} else
					menu.put(e.getKey(), null);
			}

			BetterPopup m = new SmallMenu().createMenu(menu, GLComponentWindow.getCurrentWindow(null).getCanvas().getShell(), null);

			GLComponentWindow.getCurrentWindow(null).untransformMouseEvent(event);
			m.show(new Point(event.x, event.y));
		}

		protected void resyncMouse() {
		}


		protected boolean key(Event k) {
			return false;
		}
}

	public static final Prop<EventHandler> eventHandler = new Prop<EventHandler>("eventHandler");

	public void keyPressed(ComponentContainer inside, Event arg0) {
	}

	public void keyReleased(ComponentContainer inside, Event arg0) {
	}

	public void keyTyped(ComponentContainer inside, Event arg0) {
		if (last!=null)
			last.right.key(arg0);
	}

	public void mouseClicked(ComponentContainer inside, Event arg0) {
	}

	public void mouseDragged(ComponentContainer inside, Event arg0) {
	
		/*Pair<CachedLine, EventHandler> h = locate(arg0);
		if (h != null) {
			Promise p = enter(h.left);
			try {
				if (h.right.drag(arg0)) {
					inside.requestRedisplay();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (p != null)
					p.endExecute();
			}
		}
*/
		boolean b1 = false;
		
		if (last!=null)
		{
			Promise p = enter(last.left);
			try {
				b1 |= last.right.drag(arg0);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (p != null)
					p.endExecute();
			}
		}
//		
//		Pair<CachedLine, EventHandler> h = locate(arg0);
//		
//
//		System.out.println(" drag <"+arg0+">");
//		
//
////		if (last != null)
////			System.out.println("     " + eq(h, last) + " " + stillHere(last));
//		if (last != null && !eq(h, last) && stillHere(last)) {
//
//			Promise p = enter(last.left);
//			try {
//				b1 = last.right.exit(arg0);
//			} catch (Exception e) {
//				e.printStackTrace();
//			} finally {
//				if (p != null)
//					p.endExecute();
//			}
//		}
//		if (h != null && !eq(h, last)) {
//			Promise p = enter(h.left);
//			try {
//				b1 = b1 | h.right.enter(arg0);
//				
//			} catch (Exception e) {
//				e.printStackTrace();
//			} finally {
//				if (p != null)
//					p.endExecute();
//			}
//		}
//
//		if (h != null) {
//			Promise p = enter(h.left);
//			try {
//				b1 |= h.right.drag(arg0);
//			} catch (Exception e) {
//				e.printStackTrace();
//			} finally {
//				if (p != null)
//					p.endExecute();
//			}
//		}
//
//		last = h;
		if (b1 && inside!=null)
			inside.requestRedisplay();

	}

	public void mouseEntered(ComponentContainer inside, Event arg0) {
	}

	public void mouseExited(ComponentContainer inside, Event arg0) {
	}

	Pair<CachedLine, EventHandler> last = null;
	Pair<CachedLine, EventHandler> lastDown = null;
	protected Collection<CachedLine> all;

	public void mouseMoved(ComponentContainer inside, Event arg0) {
		Pair<CachedLine, EventHandler> h = locate(arg0);
		
		boolean b1 = false;

//		if (last != null)
//			System.out.println("     " + eq(h, last) + " " + stillHere(last));
		if (last != null && !eq(h, last) && stillHere(last)) {

			Promise p = enter(last.left);
			try {
				b1 = last.right.exit(arg0);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (p != null)
					p.endExecute();
			}
		}
		if (h != null && !eq(h, last)) {
			Promise p = enter(h.left);
			try {
				b1 = b1 | h.right.enter(arg0);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (p != null)
					p.endExecute();
			}
		}
		last = h;
		if (b1 && inside!=null)
			inside.requestRedisplay();
	}
	
	public void mouseScrolled(ComponentContainer inside, Event ee) {
		boolean  b1 = false;
		if (last!=null)
		{
			Promise p = enter(last.left);
			try {
				b1 = last.right.scroll(ee);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (p != null)
					p.endExecute();
			}
			
		}
		if (b1 && inside!=null)
			inside.requestRedisplay();
	}


	private boolean stillHere(Pair<CachedLine, EventHandler> last) {
		if (!all.contains(last.left))
			return false;

		Dict p = last.left.getProperties();
		if (p != null) {
			EventHandler e = p.get(eventHandler);
			return eq(e, last.right);
		}
		return false;
	}

	private boolean eq(Object a, Object b) {
		if (a == null)
			return b == null;
		if (b == null)
			return false;
		return a.equals(b);
	}

	public void mousePressed(ComponentContainer inside, Event arg0) {
		
		if (arg0.button==2) return;
		
		Pair<CachedLine, EventHandler> h = locate(arg0);
		if (h != null) {
			Promise p = enter(h.left);
			try {
				if (h.right.down(arg0) && inside!=null)
					inside.requestRedisplay();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (p != null)
					p.endExecute();
			}
			lastDown = h;
		}
	}

	public void mouseReleased(ComponentContainer inside, Event arg0) {
		Pair<CachedLine, EventHandler> h = locate(arg0);
		if (h != null) {
			Promise p = enter(h.left);
			try {
				if (h.right.up(arg0) && inside!=null)
					inside.requestRedisplay();
				if (eq(h, lastDown))
					lastDown = null;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (p != null)
					p.endExecute();
			}
		}
		if (lastDown != null && stillHere(lastDown)) {
			
			Promise p = enter(lastDown.left);
			try {
				lastDown.right.up(arg0);
				lastDown.right.exit(arg0);
				if (inside!=null)
					inside.requestRedisplay();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (p != null)
					p.endExecute();
			}
		}
		
		mouseMoved(inside, arg0);
		
		lastDown = null;
	}

	public void setAllCachedLines(Collection<CachedLine> c) {
		this.all = c;
		for (CachedLine cc : all) {
			if (cc.properties != null) {
				EventHandler ev = cc.getProperties().get(eventHandler);
				if (ev != null) {
					Promise p = enter(cc);
					try {
						ev.begin();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if (p != null)
							p.endExecute();
					}
				}
			}
		}
	}

	protected Promise enter(CachedLine cc) {
		if (cc.properties != null) {
			Object o = cc.properties.get(iLinearGraphicsContext.source);
			if (!(o instanceof iVisualElement)) return null;
			iVisualElement source = (iVisualElement)o;
			if (source==null) return null;
			PythonScriptingSystem pss = PythonScriptingSystem.pythonScriptingSystem.get(source);
			Promise p = pss.promiseForKey(source);
			p.beginExecute();
			return p;
		}
		return null;
	}

	int cacheSize = 100;

	LinkedHashMap<Integer, Area> intersectionCache = new LinkedHashMap<Integer, Area>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Integer, Area> eldest) {
			return (size() > cacheSize);
		};
	};

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
					Area dd = new Area(cachedArea);
					dd.intersect(a2);

					Rectangle res = dd.getBounds();
					float amount = (float) ((res.width * res.height));
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
		Area a = intersectionCache.get(System.identityHashCode(cc));
		if (a == null) {
			GeneralPath gp = new LineUtils().lineToGeneralPath(cc);
			intersectionCache.put(System.identityHashCode(cc), a = new Area(gp));
		}
		return a;
	}

	public void uncache(CachedLine line) {
		intersectionCache.remove(System.identityHashCode(line));
	}

}
