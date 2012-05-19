package field.core.plugins.drawing.threed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import field.core.Platform;
import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.components.RootComponent;
import field.core.windowing.components.RootComponent.iMousePeer;
import field.core.windowing.components.RootComponent.iPaintPeer;
import field.graphics.core.BasicCamera;
import field.graphics.core.BasicCamera.State;
import field.graphics.core.BasicSceneList;
import field.launch.iUpdateable;
import field.math.linalg.Matrix4;
import field.math.linalg.Quaternion;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.util.Dict.Prop;

public class ArcBall implements iMousePeer, iPaintPeer {

	protected final BasicCamera on;
	private final Rect rect;
	private Vector2 mouseAt;
	private final GLComponentWindow repaintt;
	private State state;
	private BaseGLGraphicsContext threedContext;
	private CachedLine g;
	private BetterCameraControl bcc;

	List<State> back = new ArrayList<State>();
	List<State> forward = new ArrayList<State>();

	private void pushBookmark() {
		if (back.size() == 0 || !back.get(back.size() - 1).equals(on.getState()))
			back.add(on.getState());
		forward.clear();
	}

	public void backward() {

		State in = on.getState();

		while (on.getState().equals(in) && back.size() > 0) {
			State n = back.remove(back.size() - 1);
			forward.add(on.getState());
			on.setState(n);
			updateCamera();
			if (repaintt != null)
				repaintt.requestRepaint();
		}

		State out = on.getState();
		on.setState(in);

		if (!out.equals(in)) {
			bcc.transitionToState(out, on, 100, new iUpdateable() {

				public void update() {
					updateCamera();
					if (repaintt != null)
						repaintt.requestRepaint();
				}
			});
		}

	}

	public void forward() {
		State in = on.getState();
		if (forward.size() > 0) {
			State n = forward.remove(forward.size() - 1);
			back.add(on.getState());
			on.setState(n);
			updateCamera();
			if (repaintt != null)
				repaintt.requestRepaint();
		}

		State out = on.getState();
		on.setState(in);

		if (!out.equals(in)) {
			bcc.transitionToState(out, on, 100, new iUpdateable() {

				public void update() {
					updateCamera();
					if (repaintt != null)
						repaintt.requestRepaint();
				}
			});
		}

	}

	public ArcBall(BasicCamera on, Rect rect, GLComponentWindow repaintt) {

		this.on = on;
		this.repaintt = repaintt;
		this.rect = rect;

		rebuildGrid(null, null);

	}

	private void rebuildGrid(Vector3 up, Vector3 origin) {

		if (up == null)
			up = new Vector3(0, 1, 0);
		if (origin == null)
			origin = new Vector3(0, 0, 0);

		Vector3 left = new Vector3(-up.y, up.z, up.x);
		left = new Vector3().cross(left, up);
		Vector3 in = new Vector3().cross(left, up);

		left.normalize();
		in.normalize();

		g = new CachedLine();
		g.getProperties().put(iLinearGraphicsContext.containsDepth, 1f);
		g.getProperties().put(iLinearGraphicsContext.strokeColor, new Vector4(1.0f, 0, 0, 0.1));
		g.getProperties().put(iLinearGraphicsContext.color, new Vector4(1.0f, 0, 0, 0.1));
		g.getProperties().put(iLinearGraphicsContext.stroked, true);

		int grid = 25;
		float scale = 40;
		for (int xx = -grid; xx < grid; xx++) {

			Vector3 a1 = new Vector3(left.x * xx + in.x * -grid, left.y * xx + in.y * -grid, left.z * xx + in.z * -grid).scale(scale).add(origin);
			Vector3 a2 = new Vector3(left.x * xx + in.x * grid, left.y * xx + in.y * grid, left.z * xx + in.z * grid).scale(scale).add(origin);

			Vector3 b1 = new Vector3(left.x * -grid + in.x * xx, left.y * -grid + in.y * xx, left.z * -grid + in.z * xx).scale(scale).add(origin);
			Vector3 b2 = new Vector3(left.x * grid + in.x * xx, left.y * grid + in.y * xx, left.z * grid + in.z * xx).scale(scale).add(origin);

			System.out.println(" grid <" + a1 + " " + a2 + " " + b1 + " " + b2 + ">");

			g.getInput().moveTo(a1.x, a1.y);
			g.getInput().setPointAttribute(iLinearGraphicsContext.z_v, a1.z);
			g.getInput().lineTo(a2.x, a2.y);
			g.getInput().setPointAttribute(iLinearGraphicsContext.z_v, a2.z);

			g.getInput().moveTo(b1.x, b1.y);
			g.getInput().setPointAttribute(iLinearGraphicsContext.z_v, b1.z);
			g.getInput().lineTo(b2.x, b2.y);
			g.getInput().setPointAttribute(iLinearGraphicsContext.z_v, b2.z);

		}
	}

	public void setThreedContext(BaseGLGraphicsContext context) {
		this.threedContext = context;
		bcc = new BetterCameraControl((BasicSceneList) threedContext.getVertexProgram());
	}

	// public void ball(Vector2 ndcStart, Vector2 ndcEnd) {
	// state.reorthogonalizeUp();
	// on.setState(state);
	//
	// Vector3 a = sphereMap(ndcStart);
	// Vector3 b = sphereMap(ndcEnd);
	//
	//
	// Matrix4 mm = state.modelViewMatrix();
	// mm.invert();
	// mm.transformDirection(a);
	// mm.transformDirection(b);
	//
	// Quaternion q = new Quaternion(b, a);
	// Vector3 newPosition =
	// q.rotateVector(on.getPosition(null).sub(on.getLookAt(null))).add(on.getLookAt(null));
	// Vector3 newUp = q.rotateVector(on.getUp(null));
	//
	// on.setPosition(newPosition);
	// on.setUp(newUp);
	//
	// updateCamera();
	// }

	public void ball(Vector2 ndcStart, Vector2 ndcEnd) {
		state.reorthogonalizeUp();
		on.setState(state);

		float aroundUp = ndcEnd.x - ndcStart.x;
		float aroundLeft = ndcEnd.y - ndcStart.y;

		// Vector3 a = sphereMap(ndcStart);
		// Vector3 b = sphereMap(ndcEnd);

		// System.out.println(" start <" + a + " -> " + b);

		Vector3 up = state.getUp(new Vector3());
		Vector3 left = state.getLeft(new Vector3());

		// Matrix4 mm = state.modelViewMatrix();
		// mm.invert();
		// mm.transformDirection(a);
		// mm.transformDirection(b);

		Quaternion q1 = new Quaternion().set(up, aroundUp);
		Quaternion q2 = new Quaternion().set(left, -aroundLeft);
		Quaternion q = q1.mul(q2);

		// Quaternion q = new Quaternion(b, a);
		Vector3 newPosition = q.rotateVector(on.getPosition(null).sub(on.getLookAt(null))).add(on.getLookAt(null));
		Vector3 newUp = q.rotateVector(on.getUp(null));

		on.setPosition(newPosition);
		on.setUp(newUp);

		updateCamera();
	}

	public void pan(Vector2 ndcStart, Vector2 ndcEnd) {
		on.setState(state);

		Vector3 up = on.getUp(null);
		Vector3 ray = on.getViewRay(null);

		Vector3 left = new Vector3().cross(up, ray);
		Vector3 upAgain = new Vector3().cross(ray, left);

		left = left.normalize().scale(-ray.mag() * 0.5f);
		up = upAgain.normalize().scale(-ray.mag() * 0.5f);

		Vector3 pan = left.scale(ndcEnd.x - ndcStart.x).add(up.scale(ndcEnd.y - ndcStart.y));

		on.setPosition(on.getPosition(null).add(pan));
		on.setLookAt(on.getLookAt(null).add(pan));

		updateCamera();
	}

	public void scale(Vector2 ndcStart, Vector2 ndcEnd) {
		on.setState(state);

		Vector3 ray = on.getViewRay(null);

		on.setPosition(on.getPosition(null).add(ray.scale(ndcStart.y - ndcEnd.y)));
		// on.setLookAt(on.getLookAt(null).add(ray.scale(ndcStart.y -
		// ndcEnd.y)));

		updateCamera();
	}

	protected void updateCamera() {
	}

	private Vector3 sphereMap(Vector2 a) {
		float length = a.mag() / 2;
		if (length > 1)
			return new Vector3(a.x, -a.y, 0).normalize();
		return new Vector3(a.x, -a.y, Math.sqrt(1 - length)).normalize();
	}

	protected boolean altDown = false;
	protected boolean spaceDown = false;
	protected boolean modifiersChanged = false;

	public void keyPressed(ComponentContainer inside, Event arg0) {
		if (!arg0.doit)
			return;

		if (arg0.keyCode == SWT.ALT) {
			altDown = true;
			modifiersChanged = true;
		}

		if (arg0.keyCode == Platform.getCommandModifier()) {
			altDown = true;
			modifiersChanged = true;
		}

		if (arg0.keyCode == ' ' || arg0.keyCode == Platform.getCommandModifier()) {
			if (!spaceDown) {
				if (repaintt != null)
					repaintt.requestRepaint();
				down.clear();
			}

			spaceDown = true;
			arg0.doit = false;
		}

		if (arg0.keyCode == 'f') {
			System.out.println(" about to frame all ");
			State f = bcc.frameAll(on.getState());
			System.out.println(" from <" + on.getState() + " -> " + f + ">");
			arg0.doit = false;
			bcc.transitionToState(f, on, 100, new iUpdateable() {

				public void update() {
					updateCamera();
					if (repaintt != null)
						repaintt.requestRepaint();
				}
			});
			// on.setState(f);
			updateCamera();
			if (repaintt != null)
				repaintt.requestRepaint();
		}

	}

	public void keyReleased(ComponentContainer inside, Event arg0) {

		boolean removed = down.remove(arg0.character);

		if (arg0.keyCode == SWT.ALT) {
			altDown = false;
			modifiersChanged = true;
		}
		if (arg0.keyCode == Platform.getCommandModifier()) {
			altDown = false;
			modifiersChanged = true;
		}

		if (arg0.keyCode == ' ' || arg0.keyCode == Platform.getCommandModifier()) {
			spaceDown = false;
			modifiersChanged = true;
			if (repaintt != null)
				repaintt.requestRepaint();

			System.out.println(" pushing bookmark ");

			pushBookmark();

			System.out.println(" stack now <" + back + ">");

		}
	}

	HashSet<Character> down = new HashSet<Character>();
	HashSet<Character> interesting = new HashSet<Character>(Arrays.asList(new Character[] { 'a', 's', 'd', 'w', 'q', 'z' }));

	public void keyTyped(ComponentContainer inside, Event arg0) {

		if (!arg0.doit)
			return;
		arg0.doit = false;

		if (interesting.contains(arg0.character)) {

			down.add(arg0.character);

			if (down.size() > 0 && repaintt != null)
				repaintt.requestRepaint();

		}

		System.out.println(" TYPE : " + arg0.type + " " + SWT.KeyDown + " " + SWT.KeyUp);

		if (arg0.keyCode == SWT.ARROW_RIGHT && arg0.type == SWT.KeyUp) {
			System.out.println(" FORWARD ");
			forward();
		}

		if (arg0.keyCode == SWT.ARROW_LEFT && arg0.type == SWT.KeyUp) {
			System.out.println(" BACKWARD ");
			backward();
		}

		if (arg0.keyCode == 'o'  && (arg0.stateMask & SWT.SHIFT)==0) {
			orthogonalize();
		}

		if (arg0.keyCode == 'o' && (arg0.stateMask & SWT.SHIFT)!=0) {
			orthogonalize2();
		}

	}

	private void orthogonalize() {

		pushBookmark();
		Vector3 m = on.getState().up;

		int ax = 0;
		int sg = 1;
		float d = Float.NEGATIVE_INFINITY;
		for (int i = 0; i < 3; i++) {
			if (Math.abs(m.get(i)) > d) {
				d = Math.abs(m.get(i));
				sg = m.get(i) < 0 ? -1 : 1;
				ax = i;
			}
		}

		System.out.println(" up is :" + m + " " + ax + " (" + sg + ")");

		State out = on.getState().duplicate();
		out.up = new Vector3().set(ax, sg);
		System.out.println(" up is :" + out.up);
		out.reorthogonalizeUp();
		System.out.println(" up is :" + out.up);

		bcc.transitionToState(out, on, 100, new iUpdateable() {

			public void update() {
				updateCamera();
				if (repaintt != null)
					repaintt.requestRepaint();
			}
		});

	}

	private void orthogonalize2() {

		pushBookmark();
		State out = on.getState().duplicate();

	

		{
			Vector3 m = on.getState().getView();
			int ax = 0;
			int sg = 1;
			float d = Float.NEGATIVE_INFINITY;
			for (int i = 0; i < 3; i++) {
				if (Math.abs(m.get(i)) > d) {
					d = Math.abs(m.get(i));
					sg = m.get(i) < 0 ? -1 : 1;
					ax = i;
				}
			}
			m = new Vector3().set(ax, sg).scale(-1).setMagnitude(on.getState().getPosition().sub(on.getState().getLookAt()).mag()).add(on.getState().getLookAt());
			out.position = m;
			out.reorthogonalizeUp();
		}

		{
			Vector3 m = on.getState().up;
			int ax = 0;
			int sg = 1;
			float d = Float.NEGATIVE_INFINITY;
			for (int i = 0; i < 3; i++) {
				if (Math.abs(m.get(i)) > d) {
					d = Math.abs(m.get(i));
					sg = m.get(i) < 0 ? -1 : 1;
					ax = i;
				}
			}
			out.up = new Vector3().set(ax, sg);
			out.reorthogonalizeUp();
		}
		
		bcc.transitionToState(out, on, 100, new iUpdateable() {

			public void update() {
				updateCamera();
				if (repaintt != null)
					repaintt.requestRepaint();
			}
		});

	}

	public void mouseClicked(ComponentContainer inside, Event arg0) {
	}

	public void mouseDragged(ComponentContainer inside, Event arg0) {
		if (!spaceDown)
			return;

		if (modifiersChanged) {
			mousePressed(inside, arg0);
			modifiersChanged = false;
		}
		if (mouseAt == null)
			return;

		if (altDown && arg0.button == 1) {
			Vector2 nextMouseAt = rectMap(arg0.x, arg0.y);
			arg0.doit = false;
			ball(mouseAt, nextMouseAt);
			// mouseAt = nextMouseAt;
			if (repaintt != null)
				repaintt.requestRepaint();
		} else if (altDown && arg0.button == 2) {
			Vector2 nextMouseAt = rectMap(arg0.x, arg0.y);
			arg0.doit = false;
			pan(mouseAt, nextMouseAt);
			if (repaintt != null)
				repaintt.requestRepaint();
		} else if (altDown && arg0.button == 3) {
			Vector2 nextMouseAt = rectMap(arg0.x, arg0.y);
			arg0.doit = false;
			scale(mouseAt, nextMouseAt);
			if (repaintt != null)
				repaintt.requestRepaint();
		}
	}

	public void paint(RootComponent inside) {
		paintNow(GLComponentWindow.currentContext);
		if (down.size() > 0 && repaintt != null && spaceDown)
			repaintt.requestRepaint();
		//
		// if (down.size() > 0) {
		// for (Character arg0 : down) {
		// if (arg0 == 'a') {
		// state = on.getState();
		// Vector2 a = new Vector2(rect.midpoint2());
		// Vector2 b = new Vector2(a).add(new Vector2(2, 0));
		// a = rectMap(a.x, a.y);
		// b = rectMap(b.x, b.y);
		// ball(a, b);
		// if (repaintt != null)
		// repaintt.requestRepaint();
		// }
		// if (arg0 == 'd') {
		// state = on.getState();
		// Vector2 a = new Vector2(rect.midpoint2());
		// Vector2 b = new Vector2(a).add(new Vector2(-2, 0));
		// a = rectMap(a.x, a.y);
		// b = rectMap(b.x, b.y);
		// ball(a, b);
		// if (repaintt != null)
		// repaintt.requestRepaint();
		// }
		// if (arg0 == 'q') {
		// state = on.getState();
		// Vector2 a = new Vector2(rect.midpoint2());
		// Vector2 b = new Vector2(a).add(new Vector2(0, 2));
		// a = rectMap(a.x, a.y);
		// b = rectMap(b.x, b.y);
		// ball(a, b);
		// if (repaintt != null)
		// repaintt.requestRepaint();
		// }
		// if (arg0 == 'z') {
		// state = on.getState();
		// Vector2 a = new Vector2(rect.midpoint2());
		// Vector2 b = new Vector2(a).add(new Vector2(0, -2));
		// a = rectMap(a.x, a.y);
		// b = rectMap(b.x, b.y);
		// ball(a, b);
		// if (repaintt != null)
		// repaintt.requestRepaint();
		// }
		//
		// if (arg0 == 'w') {
		// state = on.getState();
		// Vector2 a = new Vector2(rect.midpoint2());
		// Vector2 b = new Vector2(a).add(new Vector2(0, 2));
		// a = rectMap(a.x, a.y);
		// b = rectMap(b.x, b.y);
		// scale(a, b);
		// if (repaintt != null)
		// repaintt.requestRepaint();
		// }
		// if (arg0 == 's') {
		// state = on.getState();
		// Vector2 a = new Vector2(rect.midpoint2());
		// Vector2 b = new Vector2(a).add(new Vector2(0, -2));
		// a = rectMap(a.x, a.y);
		// b = rectMap(b.x, b.y);
		// scale(a, b);
		// if (repaintt != null)
		// repaintt.requestRepaint();
		// }
		// }
		// }
	}

	Vector3 origin;
	Vector3 up;

	/**
	 * draw coordinate system helper
	 */
	public void paintNow(iLinearGraphicsContext context) {
		float width = 20;

		if (!spaceDown)
			return;

		float cx = (float) (rect.x + rect.w - width) - width / 2;
		float cy = (float) (rect.y + rect.h - width) - width / 2;

		float[] mm = new float[16];
		on.getCurrentModelViewMatrix(mm);

		Matrix4 m = new Matrix4(mm);
		Vector3 x = m.transformDirection(new Vector3(1, 0, 0));
		Vector3 y = m.transformDirection(new Vector3(0, 1, 0));
		Vector3 z = m.transformDirection(new Vector3(0, 0, 1));

		CachedLine xl = new CachedLine();
		xl.getInput().moveTo(cx, cy);
		xl.getInput().lineTo(cx + x.x * width, cy + x.y * width);
		xl.getProperties().put(iLinearGraphicsContext.color, new Vector4(0.5, 0, 0, 0.5));
		context.submitLine(xl, xl.getProperties());

		CachedLine x2 = new CachedLine();
		x2.getInput().moveTo(cx, cy);
		x2.getInput().lineTo(cx + y.x * width, cy + y.y * width);
		x2.getProperties().put(iLinearGraphicsContext.color, new Vector4(0, 0.5, 0, 0.5));
		context.submitLine(x2, x2.getProperties());

		CachedLine x3 = new CachedLine();
		x3.getInput().moveTo(cx, cy);
		x3.getInput().lineTo(cx + z.x * width, cy + z.y * width);
		x3.getProperties().put(iLinearGraphicsContext.color, new Vector4(0, 0, 0.5, 0.5));
		context.submitLine(x3, x3.getProperties());

		float inset = 5;
		CachedLine outline = new CachedLine();
		outline.getInput().moveTo((float) rect.x, (float) rect.y);
		outline.getInput().lineTo((float) rect.x - inset, (float) rect.y + inset);
		outline.getInput().lineTo((float) rect.x - inset, (float) rect.y - inset + (float) rect.h);
		outline.getInput().lineTo((float) rect.x, (float) rect.y + (float) rect.h);
		outline.getInput().lineTo((float) rect.x + inset, (float) rect.y + (float) rect.h - inset);
		outline.getInput().lineTo((float) rect.x + inset, (float) rect.y - inset);
		outline.getInput().lineTo((float) rect.x, (float) rect.y);

		outline.getInput().moveTo((float) rect.x, (float) rect.y);
		outline.getInput().lineTo((float) rect.x + inset + 1, (float) rect.y - inset);
		outline.getInput().lineTo((float) rect.x - inset + (float) rect.w, (float) rect.y - inset);
		outline.getInput().lineTo((float) rect.x + (float) rect.w, (float) rect.y);
		outline.getInput().lineTo((float) rect.x - inset + (float) rect.w, (float) rect.y + inset);
		outline.getInput().lineTo((float) rect.x + inset, (float) rect.y + inset);
		outline.getInput().lineTo((float) rect.x, (float) rect.y);

		outline.getInput().moveTo((float) rect.w + (float) rect.x, (float) rect.y);
		outline.getInput().lineTo((float) rect.w + (float) rect.x - inset, (float) rect.y + inset);
		outline.getInput().lineTo((float) rect.w + (float) rect.x - inset, (float) rect.y - inset + (float) rect.h);
		outline.getInput().lineTo((float) rect.w + (float) rect.x, (float) rect.y + (float) rect.h);
		outline.getInput().lineTo((float) rect.w + (float) rect.x + inset, (float) rect.y + (float) rect.h - inset);
		outline.getInput().lineTo((float) rect.w + (float) rect.x + inset, (float) rect.y + inset);
		outline.getInput().lineTo((float) rect.w + (float) rect.x, (float) rect.y);

		outline.getInput().moveTo((float) rect.x, (float) rect.h + (float) rect.y);
		outline.getInput().lineTo((float) rect.x + inset, (float) rect.h + (float) rect.y - inset);
		outline.getInput().lineTo((float) rect.x - inset + (float) rect.w, (float) rect.h + (float) rect.y - inset);
		outline.getInput().lineTo((float) rect.x + (float) rect.w, (float) rect.h + (float) rect.y);
		outline.getInput().lineTo((float) rect.x - inset + (float) rect.w, (float) rect.h + (float) rect.y + inset);
		outline.getInput().lineTo((float) rect.x + inset, (float) rect.h + (float) rect.y + inset);
		outline.getInput().lineTo((float) rect.x, (float) rect.h + (float) rect.y);

		outline.getProperties().put(iLinearGraphicsContext.filled, true);
		outline.getProperties().put(iLinearGraphicsContext.fillColor, new Vector4(0, 0, 0, 0.2));
		outline.getProperties().put(iLinearGraphicsContext.stroked, false);

		context.submitLine(outline, outline.getProperties());

		// fiducial grid
		if (threedContext != null) {
			Vector3 globalUp = threedContext.getGlobalProperties().get(new Prop<Vector3>("globalUp"));
			Vector3 globalOrigin = threedContext.getGlobalProperties().get(new Prop<Vector3>("globalOrigin"));

			if (compare(globalUp, up) && compare(globalOrigin, origin)) {
				threedContext.submitLine(g, g.getProperties());
			} else {
				rebuildGrid(globalUp, globalOrigin);
				up = globalUp == null ? null : new Vector3(globalUp);
				origin = globalOrigin == null ? null : new Vector3(globalOrigin);

				threedContext.submitLine(g, g.getProperties());

			}
		}
		if (bcc != null && bcc.needsUpdate()) {
			bcc.update();
			repaintt.requestRepaint();
		}

	}

	private boolean compare(Vector3 a, Vector3 b) {
		if (a == null)
			return b == null;
		if (b == null)
			return false;
		return a.distanceFrom(b) < 1e-10;
	}

	public void mouseEntered(ComponentContainer inside, Event arg0) {
	}

	public void mouseExited(ComponentContainer inside, Event arg0) {
	}

	public void mouseMoved(ComponentContainer inside, Event arg0) {
	}

	public void mousePressed(ComponentContainer inside, Event arg0) {
		if (!spaceDown)
			return;

		mouseAt = rectMap(arg0.x, arg0.y);

		System.out.println(" mouse pressed <" + mouseAt + ">");

		state = on.getState();

		if (mouseAt.x >= -1 && mouseAt.x <= 1 && mouseAt.y >= -1 && mouseAt.y <= 1) {
			arg0.doit = false;
		} else
			mouseAt = null;
	}

	private Vector2 rectMap(float x, float y) {
		Vector3 n = rect.convertToNDC(new Vector3(x, y, 0));
		n.x = -(n.x - 0.5f) * 2;
		n.y = -(n.y - 0.5f) * 2;
		return n.toVector2();
	}

	public void mouseReleased(ComponentContainer inside, Event arg0) {
	}

	public void setFrame(Rect frame) {
		this.rect.setValue(frame);
	}

}
