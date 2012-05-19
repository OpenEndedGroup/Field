package field.core.plugins.drawing;

import static org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS;
import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_BACK_LEFT;
import static org.lwjgl.opengl.GL11.GL_BACK_RIGHT;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_STEREO;
import static org.lwjgl.opengl.GL11.glDrawBuffer;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glPopAttrib;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushAttrib;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glTranslated;

import org.eclipse.swt.widgets.Event;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.plugins.drawing.threed.ArcBall;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.iComponent;
import field.graphics.core.BasicCamera;
import field.graphics.core.BasicCamera.State;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.BasicSceneList;
import field.graphics.core.CoreHelpers;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.linalg.Vector2;

public class SceneListOverrides extends DefaultOverride {

	public VisualElementProperty<BasicCamera.State> camera = new VisualElementProperty<BasicCamera.State>("camera");
	public VisualElementProperty<BasicSceneList> canvas = new VisualElementProperty<BasicSceneList>("canvas");
	public VisualElementProperty<BasicSceneList> canvasLeft = new VisualElementProperty<BasicSceneList>("canvasLeft");
	public VisualElementProperty<BasicSceneList> canvasRight = new VisualElementProperty<BasicSceneList>("canvasRight");

	public BasicSceneList list = new BasicSceneList();
	public BasicSceneList left = new BasicSceneList();
	public BasicSceneList right = new BasicSceneList();

	BasicCamera onDemandCamera = null;
	ArcBall ball = null;

	public VisualElementProperty<Number> orthoDisparity = new VisualElementProperty<Number>("orthoDisparity");

	static public float globalDisparity = 1f;

	@Override
	public VisitCode paintNow(iVisualElement source, Rect bounds, boolean visible) {
		if (source == forElement) {

			CoreHelpers.glPushAttrib(GL_ALL_ATTRIB_BITS);

			CoreHelpers.glMatrixMode(GL_PROJECTION);
			CoreHelpers.glMatrixMode(GL_MODELVIEW);

			CoreHelpers.glPushMatrix();
			CoreHelpers.glTranslated(bounds.x, bounds.y, 0);

			BasicGLSLangProgram was = BasicGLSLangProgram.currentProgram;
			State s = camera.get(forElement);
			try {

				if (s != null) {
					CoreHelpers.glMatrixMode(GL_PROJECTION);

					CoreHelpers.glPushMatrix();
					CoreHelpers.glLoadIdentity();

					CoreHelpers.glMatrixMode(GL_MODELVIEW);
					CoreHelpers.glPushMatrix();
					CoreHelpers.glLoadIdentity();

					if (onDemandCamera == null) {
						onDemandCamera = new BasicCamera();

						ball = new ArcBall(onDemandCamera, forElement.getFrame(null), GLComponentWindow.getCurrentWindow(null)) {
							@Override
							protected void updateCamera() {
								camera.set(forElement, forElement, on.getState());
							}

							public void keyPressed(field.core.windowing.GLComponentWindow.ComponentContainer inside, Event arg0) {
								if (isSelected())
									super.keyPressed(inside, arg0);
							};
						};

						iVisualElement.glassComponent.get(forElement).addTransparentMousePeer(ball);
						iVisualElement.rootComponent.get(forElement).addPaintPeer(ball);

					}

					onDemandCamera.setState(s);

					Rect r = forElement.getFrame(null);
					Vector2 topLeft = new Vector2(r.x, r.y);
					GLComponentWindow.getCurrentWindow(null).transformDrawingToWindow(topLeft);
					Vector2 bottomRight = new Vector2(r.x + r.w, r.y + r.h);
					GLComponentWindow.getCurrentWindow(null).transformDrawingToWindow(bottomRight);

					int h = GLComponentWindow.getCurrentWindow(null).getFrame().getSize().y;
					onDemandCamera.setViewport((int) topLeft.x, h - (int) bottomRight.y, (int) (bottomRight.x - topLeft.x), (int) (bottomRight.y - topLeft.y));
					onDemandCamera.setAspect(Math.abs(bottomRight.x - topLeft.x) / Math.abs(bottomRight.y - topLeft.y));
					onDemandCamera.performPass(null);
				}

				list.update();
				CoreHelpers.glPopAttrib();

				glEnable(GL_STEREO);

				float d = orthoDisparity.getFloat(forElement, 0) * globalDisparity;

				CoreHelpers.glTranslated(-d, 0, 0);

				glDrawBuffer(GL_BACK_LEFT);
				left.update();
				CoreHelpers.glTranslated(d * 2, 0, 0);
				glDrawBuffer(GL_BACK_RIGHT);
				right.update();
				glDrawBuffer(GL_BACK);
			} finally {

				if (s != null) {
					CoreHelpers.glMatrixMode(GL_PROJECTION);
					CoreHelpers.glPopMatrix();

					CoreHelpers.glMatrixMode(GL_MODELVIEW);
					CoreHelpers.glPopMatrix();
				}
				CoreHelpers.glPopMatrix();

				was.performPass(null);
			}
		}
		return super.paintNow(source, bounds, visible);
	}

	public boolean isSelected() {
		try {
			final Ref<SelectionGroup<iComponent>> group = new Ref<SelectionGroup<iComponent>>(null);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).getProperty(forElement, iVisualElement.selectionGroup, group);
			final SelectionGroup<iComponent> g = group.get();
			if (g.getSelection().contains(iVisualElement.localView.get(forElement)))
				return true;
			return false;
		} catch (NullPointerException e) {
			return false;
		}
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
		}
		return super.getProperty(source, prop, ref);
	}

}
