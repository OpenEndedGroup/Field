package field.core.plugins.drawing.embedded;

import field.core.plugins.drawing.embedded.iNodeCallBack_m;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Event;

import field.bytecode.apt.Mirroring.MirrorNoReturnMethod;
import field.core.dispatch.iVisualElement;
import field.core.execution.PythonScriptingSystem;
import field.core.execution.PythonScriptingSystem.Promise;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLineCursor;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.components.RootComponent.iMousePeer;
import field.math.linalg.Vector2;
import field.namespace.generic.Generics.Triple;
import field.util.Dict.Prop;

/*
 * callbacks embedded inside CacheLines using attributes
 */
public class EmbeddedInteraction {

	static public class MousePeer implements iMousePeer {

		private final BaseGLGraphicsContext context;

		List<Triple<iNodeCallBack, CachedLine, CachedLine.Event>> releaseStack = new ArrayList<Triple<iNodeCallBack, CachedLine, CachedLine.Event>>();

		public MousePeer(BaseGLGraphicsContext inside) {
			this.context = inside;
		}

		public void keyPressed(ComponentContainer inside, Event arg0) {
		}

		public void keyReleased(ComponentContainer inside, Event arg0) {
		}

		public void keyTyped(ComponentContainer inside, Event arg0) {
		}

		public void mouseClicked(ComponentContainer inside, Event arg0) {
			Set<CachedLine> al = context.getAllLines();
			perform(al, iNodeCallBack_m.mouseClicked_s, new Vector2(arg0.x, arg0.y), arg0, true);
		}

		public void mouseDragged(ComponentContainer inside, Event arg0) {
			Set<CachedLine> al = context.getAllLines();
			perform(al, iNodeCallBack_m.mouseDragged_s, new Vector2(arg0.x, arg0.y), arg0, true);
		}

		public void mouseEntered(ComponentContainer inside, Event arg0) {
		}

		public void mouseExited(ComponentContainer inside, Event arg0) {
		}

		public void mouseMoved(ComponentContainer inside, Event arg0) {
		}

		public void mousePressed(ComponentContainer inside, Event arg0) {
			Set<CachedLine> al = context.getAllLines();
			releaseStack.addAll(perform(al, iNodeCallBack_m.mouseDown_s, new Vector2(arg0.x, arg0.y), arg0, true));
		}

		public void mouseReleased(ComponentContainer inside, Event arg0) {
			for (Triple<iNodeCallBack, CachedLine, CachedLine.Event> t : releaseStack) {
				fire(t.left, t.middle, t.right, iNodeCallBack_m.mouseUp_s, new Vector2(arg0.x, arg0.y), arg0);
			}
			releaseStack.clear();
		}

	}

	public interface NodeTrigger {
		public List<iNodeCallBack> edgeAfter();

		public List<iNodeCallBack> edgeBefore();

		public List<iNodeCallBack> insideLine();

		public List<iNodeCallBack> node();
	}

	static public class NodeTriggerAdaptor implements NodeTrigger {
		public List<iNodeCallBack> edgeAfter() {
			return null;
		}

		public List<iNodeCallBack> edgeBefore() {
			return null;
		}

		public List<iNodeCallBack> insideLine() {
			return null;
		}

		public List<iNodeCallBack> node() {
			return null;
		}

	}

	static public final Prop<NodeTrigger> nodeTrigger_v = new Prop<NodeTrigger>("nodeTrigger_v");

	public static List<Triple<iNodeCallBack, CachedLine, CachedLine.Event>> perform(Set<CachedLine> al, MirrorNoReturnMethod<iNodeCallBack, Object[]> mirrorNoReturnMethod, Vector2 vector2, Event arg0, boolean doPerLine) {

		float scale = 15;

		List<Triple<iNodeCallBack, CachedLine, CachedLine.Event>> sent = new ArrayList<Triple<iNodeCallBack, CachedLine, CachedLine.Event>>();

		// per node hit test
		for (CachedLine l : al) {

			CachedLineCursor b = LineUtils.hitTest2(l, vector2, scale);

			Set<iNodeCallBack> doNotCall = new HashSet<iNodeCallBack>();
			if (b != null) {
				if (b.current != null && b.current.attributes != null) {
					NodeTrigger nt = b.current.attributes.get(nodeTrigger_v);
					if (nt != null) {
						{
							List<iNodeCallBack> n1 = nt.node();
							if (n1 != null && n1.size() > 0) {
								Vector2 is = b.current.getAt(-1, null);
								if (is.distanceFrom(vector2) < scale) {
									for (iNodeCallBack c : n1) {
										if (doNotCall.contains(c))
											continue;
										fire(c, l, b.current, mirrorNoReturnMethod, vector2, arg0);
										sent.add(new Triple<iNodeCallBack, CachedLine, CachedLine.Event>(c, l, b.current));
										doNotCall.add(c);
									}
								}
							}
						}
						{
							List<iNodeCallBack> n1 = nt.edgeBefore();
							if (n1 != null && n1.size() > 0) {
								for (iNodeCallBack c : n1) {
									if (doNotCall.contains(c))
										continue;
									fire(c, l, b.current, mirrorNoReturnMethod, vector2, arg0);
									sent.add(new Triple<iNodeCallBack, CachedLine, CachedLine.Event>(c, l, b.current));
									doNotCall.add(c);
								}
							}
						}
					}
				}
				if (b.before != null && b.before.attributes != null) {
					NodeTrigger nt = b.before.attributes.get(nodeTrigger_v);
					if (nt != null) {
						{
							List<iNodeCallBack> n1 = nt.node();
							if (n1 != null && n1.size() > 0) {
								Vector2 is = b.current.getAt(-1, null);
								if (is.distanceFrom(vector2) < scale) {
									for (iNodeCallBack c : n1) {
										if (doNotCall.contains(c))
											continue;
										fire(c, l, b.current, mirrorNoReturnMethod, vector2, arg0);
										sent.add(new Triple<iNodeCallBack, CachedLine, CachedLine.Event>(c, l, b.current));
										doNotCall.add(c);
									}
								}
							}
						}
						{
							List<iNodeCallBack> n1 = nt.edgeAfter();
							if (n1 != null && n1.size() > 0) {
								for (iNodeCallBack c : n1) {
									if (doNotCall.contains(c))
										continue;
									fire(c, l, b.current, mirrorNoReturnMethod, vector2, arg0);
									sent.add(new Triple<iNodeCallBack, CachedLine, CachedLine.Event>(c, l, b.current));
									doNotCall.add(c);
								}
							}
						}
					}
				}
			}
		}

		return sent;
	}

	private static void fire(iNodeCallBack n1, CachedLine l, CachedLine.Event current, MirrorNoReturnMethod<iNodeCallBack, Object[]> method, Vector2 vector2, Event arg0) {

		iVisualElement inside = l.getProperties().get(iLinearGraphicsContext.source);

		Promise promise = null;
		if (inside != null) {
			PythonScriptingSystem pss = PythonScriptingSystem.pythonScriptingSystem.get(inside);
			if (pss != null) {
				promise = pss.promiseForKey(inside);
				if (promise != null) {
					promise.beginExecute();
				}
			}
		}
		try {
			method.acceptor(n1).set(new Object[] { l, current, vector2, arg0 });
		} finally {
			if (promise != null)
				promise.endExecute();
		}
	}

}
