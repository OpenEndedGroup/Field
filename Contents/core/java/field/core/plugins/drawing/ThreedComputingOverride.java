package field.core.plugins.drawing;

import static org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS;
import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_BACK_LEFT;
import static org.lwjgl.opengl.GL11.GL_BACK_RIGHT;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_STEREO;
import static org.lwjgl.opengl.GL11.glColorMask;
import static org.lwjgl.opengl.GL11.glDrawBuffer;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glScissor;
import static org.lwjgl.opengl.GL11.glViewport;

import java.awt.geom.GeneralPath;
import java.util.List;

import org.eclipse.swt.widgets.Event;
import org.lwjgl.opengl.GL11;
import org.python.core.PyException;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.LineInteraction3d;
import field.core.plugins.drawing.opengl.OnCanvasLines;
import field.core.plugins.drawing.opengl.OnCanvasLines.DirectLayer;
import field.core.plugins.drawing.opengl.SimpleLineDrawing;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.drawing.threed.ArcBall;
import field.core.plugins.drawing.threed.ThreedContext;
import field.core.util.FieldPyObjectAdaptor.iHandlesAttributes;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.components.RootComponent.iMousePeer;
import field.graphics.core.BasicCamera;
import field.graphics.core.BasicCamera.State;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.BasicSceneList;
import field.graphics.core.CoreHelpers;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;

public class ThreedComputingOverride extends SplineComputingOverride {

	static public final VisualElementProperty<ThreedContext> context = new VisualElementProperty<ThreedContext>("context_");
	static public final VisualElementProperty<State> camera = new VisualElementProperty<State>("camera");

	public VisualElementProperty<BasicSceneList> canvas = new VisualElementProperty<BasicSceneList>("canvas");
	public VisualElementProperty<BasicSceneList> canvasLeft = new VisualElementProperty<BasicSceneList>("canvasLeft");
	public VisualElementProperty<BasicSceneList> canvasRight = new VisualElementProperty<BasicSceneList>("canvasRight");

	public BasicSceneList_fscshim list = new BasicSceneList_fscshim();
	public BasicSceneList left = new BasicSceneList();
	public BasicSceneList right = new BasicSceneList();

	public VisualElementProperty<Number> orthoDisparity = new VisualElementProperty<Number>("orthoDisparity");
	public VisualElementProperty<Number> leftDisparity = new VisualElementProperty<Number>("leftDisparity");

	static public float globalDisparity = 1f;

	/*
	 * 
	 * from field.core.plugins.drawing.threed import ArcBall from
	 * field.core.plugins.drawing.threed import ThreedContext
	 * 
	 * rr = Rect(50, 50, 500, 200) context =
	 * ThreedContext(S.window.getSceneList(), S.window, rr)
	 * SimpleLineDrawing().installInto(context)
	 * context.subCamera.setPosition(Vector3(1,-0.3,-2))
	 * 
	 * arc = ArcBall(context.subCamera, rr, S.window)
	 * _self.glassComponent.addTransparentMousePeer(arc)
	 * _self.rootComponent.addPaintPeer(arc)
	 * _self.glassComponent.removeMousePeer(arc)
	 */

	public class BasicSceneList_fscshim extends BasicSceneList implements iHandlesAttributes {

		public float width() {
			return (float) forElement.getFrame(null).w;
		}

		public float height() {
			return (float) forElement.getFrame(null).h;
		}

		@Override
		public Object getAttribute(String name) {
			if (name.equals("camera"))
			{
				return camera.get(forElement);
			}
				
			throw new PyException();
		}

		@Override
		public void setAttribute(String name, Object value) {
			if (name.equals("camera") && value instanceof State)
			{
				camera.set(forElement, forElement, (State) value);
			}
			else
				throw new PyException();
		}

	}

	@Override
	public DefaultOverride setVisualElement(iVisualElement ve) {
		DefaultOverride o = super.setVisualElement(ve);
		ve.setProperty(shouldAutoComputeRect, false);
		// ve.setProperty(noFrame, true);

		return o;
	}

	public ThreedContext defaultContext = null;
	public ArcBall ball = null;
	private iMousePeer mousePeer;

	public void makeDefaultContext() {
		ThreedContext c = context.get(forElement);
		if (c == null) {
			GLComponentWindow element = iVisualElement.enclosingFrame.get(forElement);
			if (element != null) {

				// defaultContext = c = new
				// ThreedContext(element.getSceneList(),
				// element, forElement.getFrame(null));
				defaultContext = c = new ThreedContext(this.list, element, forElement.getFrame(null));
				defaultContext.getGlobalProperties().put(iLinearGraphicsContext.geometricScale, 5f);
				defaultContext.getGlobalProperties().put(iLinearGraphicsContext.flatnessScale, 5f);
				new SimpleLineDrawing().installInto(c);

				final LineInteraction3d interaction = new LineInteraction3d(defaultContext.getCamera()) {
					@Override
					protected GeneralPath filter(GeneralPath gp) {
						return gp;
					}
				};
				defaultContext.setLineInteraction(interaction);
				mousePeer = new iMousePeer() {

					@Override
					public void mouseReleased(ComponentContainer inside, Event arg0) {
						Rect f = forElement.getFrame(null);
						arg0.x -= f.x;
						arg0.y -= f.y;
						try {
							interaction.mouseReleased(null, arg0);
						} finally {
							arg0.x += f.x;
							arg0.y += f.y;
						}
					}

					@Override
					public void mousePressed(ComponentContainer inside, Event arg0) {
						Rect f = forElement.getFrame(null);
						arg0.x -= f.x;
						arg0.y -= f.y;
						try {
							interaction.mousePressed(null, arg0);
						} finally {
							arg0.x += f.x;
							arg0.y += f.y;
						}
					}

					@Override
					public void mouseMoved(ComponentContainer inside, Event arg0) {
						Rect f = forElement.getFrame(null);
						arg0.x -= f.x;
						arg0.y -= f.y;
						try {
							interaction.mouseMoved(null, arg0);
						} finally {
							arg0.x += f.x;
							arg0.y += f.y;
						}
					}

					@Override
					public void mouseExited(ComponentContainer inside, Event arg0) {
					}

					@Override
					public void mouseEntered(ComponentContainer inside, Event arg0) {
					}

					@Override
					public void mouseDragged(ComponentContainer inside, Event arg0) {
						Rect f = forElement.getFrame(null);
						arg0.x -= f.x;
						arg0.y -= f.y;
						try {
							interaction.mouseDragged(null, arg0);
						} finally {
							arg0.x += f.x;
							arg0.y += f.y;
						}
					}

					@Override
					public void mouseClicked(ComponentContainer inside, Event arg0) {
					}

					@Override
					public void keyTyped(ComponentContainer inside, Event arg0) {
					}

					@Override
					public void keyReleased(ComponentContainer inside, Event arg0) {
					}

					@Override
					public void keyPressed(ComponentContainer inside, Event arg0) {
					}
				};

				iVisualElement.glassComponent.get(forElement).addTransparentMousePeer(mousePeer);

				// default camera
				State state = c.getTransformState();
				state.fov = 53;
				state.far = 10000;
				Rect f = forElement.getFrame(new Rect());
				float back = (float) f.w;
				state.position = new Vector3(f.x + f.w / 2, f.y + f.h / 2, -back);
				state.up = new Vector3(0, -1, 0);
				state.target = new Vector3(f.x + f.w / 2, f.y + f.h / 2, 0);
				c.setTransformState(state);

				State cc = camera.get(forElement);
				if (cc != null)
					c.setTransformState(cc);
				else {
					camera.set(forElement, forElement, c.getTransformState());
				}

				ball = new ArcBall(c.getCamera(), forElement.getFrame(null), element) {
					@Override
					protected void updateCamera() {
						State newState = context.get(forElement).getTransformState().duplicate();
						;//System.out.println(" setting state to <" + newState + ">");

						newState.reorthogonalizeUp();
						camera.set(forElement, forElement, newState);
					}

					public void keyPressed(field.core.windowing.GLComponentWindow.ComponentContainer inside, Event arg0) {
						if (isSelected() || spaceDown)
							super.keyPressed(inside, arg0);
					};
				};
				ball.setThreedContext(defaultContext);

				iVisualElement.glassComponent.get(forElement).addTransparentMousePeer(ball);
				iVisualElement.rootComponent.get(forElement).addPaintPeer(ball);
			}
		}
		context.set(forElement, forElement, c);
	}

	public ThreedContext getDefaultContext() {
		if (defaultContext == null)
			makeDefaultContext();
		return defaultContext;
	}

	boolean anaglyph = false;
	boolean stereo = false;

	@Override
	public VisitCode paintNow(iVisualElement source, Rect bounds, boolean visible) {
		if (source == forElement) {
			State cameraState = camera.get(forElement);
			if (cameraState != null)
				cameraState = cameraState.duplicate();
			if (defaultContext == null) {
				makeDefaultContext();
			}

			if (ball != null) {
				ball.setFrame(forElement.getFrame(null));
			}

			if (defaultContext != null) {
				defaultContext.setViewportRectangle(forElement.getFrame(null));
				defaultContext.repaint();
				if (cameraState != null) {
					defaultContext.setTransformState(cameraState);
				}

				List<CachedLine> g = computed_linesToDraw.get(forElement);
				if (g != null) {
					for (CachedLine cc : g) {
						if (cc == null)
							continue;
						if (!cc.getProperties().has(iLinearGraphicsContext.context)) {
							cc.getProperties().put(iLinearGraphicsContext.context, defaultContext);
						}
					}

				}
			}

			CoreHelpers.glPushAttrib(GL_ALL_ATTRIB_BITS);

			CoreHelpers.glMatrixMode(GL_PROJECTION);
			CoreHelpers.glPushMatrix();

			CoreHelpers.glMatrixMode(GL_MODELVIEW);
			CoreHelpers.glPushMatrix();

			CoreHelpers.glTranslated(bounds.x, bounds.y, 0);

			BasicGLSLangProgram was = BasicGLSLangProgram.currentProgram;
			State s = camera.get(forElement);

			if (s != null) {
				CoreHelpers.glMatrixMode(GL_PROJECTION);
				CoreHelpers.glPushMatrix();
				CoreHelpers.glLoadIdentity();

				CoreHelpers.glMatrixMode(GL_MODELVIEW);
				CoreHelpers.glPushMatrix();
				CoreHelpers.glLoadIdentity();
			}

			try {
				if (s != null) {

					Rect r = forElement.getFrame(null);
					Vector2 topLeft = new Vector2(r.x, r.y);
					GLComponentWindow.getCurrentWindow(null).transformDrawingToWindow(topLeft);
					Vector2 bottomRight = new Vector2(r.x + r.w, r.y + r.h);
					GLComponentWindow.getCurrentWindow(null).transformDrawingToWindow(bottomRight);

					int h = GLComponentWindow.getCurrentWindow(null).getFrame().getSize().y;
					defaultContext.getCamera().setViewport((int) topLeft.x, h - (int) bottomRight.y, (int) (bottomRight.x - topLeft.x), (int) (bottomRight.y - topLeft.y));
					defaultContext.getCamera().setAspect(Math.abs(bottomRight.x - topLeft.x) / Math.abs(bottomRight.y - topLeft.y));
					defaultContext.getCamera().performPass(null);
					glScissor((int) topLeft.x, h - (int) bottomRight.y, (int) (bottomRight.x - topLeft.x), (int) (bottomRight.y - topLeft.y));
					glEnable(GL11.GL_SCISSOR_TEST);
				} else {
					Rect r = forElement.getFrame(null);
					Vector2 topLeft = new Vector2(r.x, r.y);
					GLComponentWindow.getCurrentWindow(null).transformDrawingToWindow(topLeft);
					Vector2 bottomRight = new Vector2(r.x + r.w, r.y + r.h);
					GLComponentWindow.getCurrentWindow(null).transformDrawingToWindow(bottomRight);
					int h = GLComponentWindow.getCurrentWindow(null).getFrame().getSize().y;
					glViewport((int) topLeft.x, h - (int) bottomRight.y, (int) (bottomRight.x - topLeft.x), (int) (bottomRight.y - topLeft.y));

					glScissor((int) topLeft.x, h - (int) bottomRight.y, (int) (bottomRight.x - topLeft.x), (int) (bottomRight.y - topLeft.y));
					glEnable(GL11.GL_SCISSOR_TEST);

				}

				if (!stereo)
					list.update();

				if (!(anaglyph || stereo))
					CoreHelpers.glPopAttrib();

				if (anaglyph || stereo)
					glEnable(GL_STEREO);

				float d1 = orthoDisparity.getFloat(forElement, 0) * globalDisparity;
				float d2 = leftDisparity.getFloat(forElement, 0) * globalDisparity;

				CoreHelpers.glTranslated(-d1, 0, 0);

				float dtla = 0;
				if (anaglyph || stereo) {
					State leftState = cameraState.duplicate();
					dtla = leftState.position.distanceFrom(leftState.target);
					leftState.position.add(leftState.getLeft(null).scale(d2).scale(dtla));
					defaultContext.setTransformState(leftState);
					defaultContext.getCamera().performPass(null);
				}

				if (anaglyph)
					glColorMask(true, false, false, true);
				else if (stereo)
					glDrawBuffer(GL_BACK_LEFT);

				if (stereo)
					list.update();

				left.update();
				// glTranslated(d1 * 2, 0, 0);

				if (anaglyph)
					glColorMask(false, true, true, true);
				else if (stereo)
					glDrawBuffer(GL_BACK_RIGHT);

				if (anaglyph || stereo) {
					State leftState = cameraState.duplicate();
					leftState.position.add(leftState.getLeft(null).scale(-d2).scale(dtla));
					defaultContext.setTransformState(leftState);
					defaultContext.getCamera().performPass(null);
				}
				if (stereo)
					list.update();
				right.update();

				glDrawBuffer(GL_BACK);
				if (anaglyph || stereo) {
					defaultContext.setTransformState(cameraState);
					defaultContext.getCamera().performPass(null);
				}

				if (anaglyph || stereo)
					CoreHelpers.glPopAttrib();

			} finally {
				GL11.glDisable(GL11.GL_SCISSOR_TEST);

				if (s != null) {
					CoreHelpers.glMatrixMode(GL_PROJECTION);
					CoreHelpers.glPopMatrix();

					CoreHelpers.glMatrixMode(GL_MODELVIEW);
					CoreHelpers.glPopMatrix();
				}

				CoreHelpers.glMatrixMode(GL_PROJECTION);
				CoreHelpers.glPopMatrix();

				CoreHelpers.glMatrixMode(GL_MODELVIEW);
				CoreHelpers.glPopMatrix();

				was.performPass(null);
			}

		}
		return super.paintNow(source, bounds, visible);
	}

	@Override
	public VisitCode handleKeyboardEvent(iVisualElement newSource, Event event) {

		;//System.out.println(" hke <" + newSource + "> <" + event + "> <" + isSelected() + ">");

		if (isSelected() && newSource == forElement) {
			{
				ball.keyTyped(null, event);
			}
		}
		return super.handleKeyboardEvent(newSource, event);
	}

	@Override
	protected Object filter(Object o) {
		Object cl = super.filter(o);
		if (cl instanceof CachedLine) {
			if (defaultContext != null) {
				((CachedLine) cl).getProperties().put(iLinearGraphicsContext.context, defaultContext);
			}
		}
		return cl;
	}

	@Override
	public <T> VisitCode getProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> ref) {
		if (source == forElement) {
			if (prop.equals(canvas)) {
				ref.set((T) list);
				return VisitCode.stop;
			}
			if (prop.equals(canvasLeft)) {
				ref.set((T) left);
				return VisitCode.stop;
			}
			if (prop.equals(canvasRight)) {
				ref.set((T) right);
				return VisitCode.stop;
			}

			if (prop.equals(direct)) {
				if (forElement.getProperty(direct) == null) {
					DirectLayer l = new OnCanvasLines(list, forElement).getDirectLayer("fast");
					l.animate.clear();
					forElement.setProperty(direct, l);
					ref.set((T) l, forElement);
				} else {
					ref.set((T) forElement.getProperty(direct), forElement);
				}
				return VisitCode.stop;
			}
		}
		return super.getProperty(source, prop, ref);
	}

	@Override
	public VisitCode deleted(iVisualElement source) {
		if (source == forElement) {
			iVisualElement.glassComponent.get(forElement).removeMousePeer(ball);
			iVisualElement.rootComponent.get(forElement).removePaintPeer(ball);
		}
		return super.deleted(source);
	}
}
