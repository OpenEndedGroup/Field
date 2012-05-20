package field.core.plugins.drawing.threed;

import java.awt.Frame;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Stack;



import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL41.*;
import static org.lwjgl.opengl.ARBShaderObjects.*;


import org.eclipse.swt.widgets.Shell;
import org.lwjgl.util.glu.GLU;

import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext.iTransformingContext;
import field.core.windowing.GLComponentWindow;
import field.graphics.core.Base.StandardPass;
import field.graphics.core.Base.iAcceptsSceneListElement;
import field.graphics.core.BasicCamera;
import field.graphics.core.CoreHelpers;
import field.graphics.core.BasicCamera.State;
import field.graphics.core.BasicUtilities;
import field.launch.iUpdateable;
import field.math.linalg.SingularMatrixException;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.util.Dict.Prop;
import field.util.TaskQueue;

/**
 * just like a BaseGLGraphicsContext, only takes a few more base classes of
 * things other than just PLines
 * 
 * it also knows how to transform to and from sheet (2d) coordinates. This
 * transformation echo's that of a OpenGL transformation that does the same
 * thing
 * 
 * @author marc
 * 
 */
public class ThreedContext extends BaseGLGraphicsContext implements iTransformingContext<Vector3> {

	public interface iThreedDrawingSurface {
		public void transformDrawingToWindow(Vector2 drawing);

		public Shell getFrame();

		public TaskQueue getPostQueue();
	}

	private final Rect viewportRectangle;
	private BasicCamera subCamera;

	int[] viewPort = new int[4];
	double[] mm = new double[16];
	double[] pm = new double[16];
	private final iThreedDrawingSurface parent;
	protected int windowHeight;

	public ThreedContext(iAcceptsSceneListElement inside, final iThreedDrawingSurface p, Rect viewportRectangle) {
		super(inside, false);
		this.parent = p;
		this.viewportRectangle = viewportRectangle;

		subCamera = new BasicCamera();
		subCamera.setViewport(0, 0, parent.getFrame().getSize().x, parent.getFrame().getSize().y);

		vertexProgram.addChild(new BasicUtilities.TwoPassElement("subCamera", StandardPass.preTransform, StandardPass.preDisplay) {

			@Override
			protected void post() {
				CoreHelpers.glMatrixMode(GL_MODELVIEW);
				CoreHelpers.glPopMatrix();
				CoreHelpers.glMatrixMode(GL_PROJECTION);
				CoreHelpers.glPopMatrix();
				CoreHelpers.glMatrixMode(GL_MODELVIEW);

				// we need to concat the scale and translation
				// matrix here?
				// we do need to do something about the viewport
			}

			@Override
			protected void pre() {
				CoreHelpers.glMatrixMode(GL_MODELVIEW);
				CoreHelpers.glPushMatrix();
				CoreHelpers.glMatrixMode(GL_PROJECTION);
				CoreHelpers.glPushMatrix();
				CoreHelpers.glMatrixMode(GL_MODELVIEW);

				Vector2 tl = ThreedContext.this.viewportRectangle.topLeft().toVector2();
				Vector2 br = ThreedContext.this.viewportRectangle.bottomRight().toVector2();

				parent.transformDrawingToWindow(tl);
				parent.transformDrawingToWindow(br);

				int ox = (int) Math.min(tl.x, br.x);
				int oy = (int) Math.min(tl.y, br.y);
				int w = (int) Math.max(tl.x, br.x) - ox;
				int h = (int) Math.max(tl.y, br.y) - oy;

				windowHeight = parent.getFrame().getSize().y;

				// ;//System.out.println(" viewport <" + ox + " " +
				// oy + " " + w + " " + h + ">");
				// ;//System.out.println(" actuall viewport <" + ox
				// + " " + (windowHeight - oy - (h) *
				// parent.getYScale()) + " " + w + " " + h +
				// ">");

				subCamera.setViewport(ox, (int) (windowHeight - (oy + h)), w, h);
				subCamera.setAspect(w / (float) h);

				subCamera.gl = gl;
				subCamera.glu = glu;

				
				subCamera.performPass();

				viewPort[0] = subCamera.oX;
				viewPort[1] = subCamera.oY;
				viewPort[2] = subCamera.width;
				viewPort[3] = subCamera.height;

				for (int i = 0; i < 16; i++)
					mm[i] = subCamera.modelView[i];
				for (int i = 0; i < 16; i++)
					pm[i] = subCamera.projection[i];

			}

			@Override
			protected void setup() {
			}

		});

		parent.getPostQueue().addUpdateable(new iUpdateable() {

			public void update() {
				paintNow();
			}
		});

	}

	boolean repaintRequested = false;

	public void repaint() {
		repaintRequested = true;
	}

	public void paintNow() {
		if (!repaintRequested)
			return;

		windowDisplayExit();
	}

	float[] tmp = new float[3];
	double[] outX = new double[3];


	public void setViewportRectangle(Rect viewportRectangle) {
		this.viewportRectangle.setValue(viewportRectangle);
		repaint();
	}

	FloatBuffer model = ByteBuffer.allocateDirect(4*16).order(ByteOrder.nativeOrder()).asFloatBuffer();
	FloatBuffer object1 = ByteBuffer.allocateDirect(4*16).order(ByteOrder.nativeOrder()).asFloatBuffer();
	FloatBuffer object2 = ByteBuffer.allocateDirect(4*16).order(ByteOrder.nativeOrder()).asFloatBuffer();
	FloatBuffer projectionn = ByteBuffer.allocateDirect(4*16).order(ByteOrder.nativeOrder()).asFloatBuffer();
	IntBuffer viewport = ByteBuffer.allocateDirect(4*16).order(ByteOrder.nativeOrder()).asIntBuffer();

	
	public boolean convertIntermediateSpaceToDrawingSpace(Vector3 three, Vector2 drawing) {
		float[] tt = three.get(tmp);
		
//		;//System.out.println(" convert intermediate space to drawing space");
		
		model.rewind();
		projectionn.rewind();
		for(int n=0;n<mm.length;n++)
			model.put((float)mm[n]);
		for(int n=0;n<mm.length;n++)
			projectionn.put((float)pm[n]);
		model.rewind();
		projectionn.rewind();
		object1.rewind();
		
		viewport.rewind();
		viewport.put(viewPort);
		viewport.rewind();
		
		GLU.gluProject(three.x, three.y, three.z, model, projectionn, viewport, object1);

		Vector3 two = new Vector3(object1);
		object1.rewind();
		
		two.x = (float) (viewportRectangle.x + viewportRectangle.w * (two.x - viewPort[0]) / (float) viewPort[2]);
		two.y = (float) (viewportRectangle.y + viewportRectangle.h - (viewportRectangle.h * (two.y - viewPort[1]) / (float) viewPort[3]));

		drawing.x = two.x;
		drawing.y = two.y;

		float d = new Vector3().sub(three, subCamera.getPosition(null)).dot(subCamera.getViewRay(null).normalize());

//		;//System.out.println(" convert intermediate space to drawing space <"+three+" -> "+drawing+">");

		
		Vector3 t2 = new Vector3(three);
		Vector2 d2 = new Vector2(drawing);
		Vector3 t3 = new Vector3();
		convertDrawingSpaceToIntermediate(d2, t3);
//		;//System.out.println(" round trip ? "+t2+" "+d2+" "+t3);
		
		
		return d > nearPlane;

	}

	static public float nearPlane = 1;

	public boolean shouldClip(Vector3 three) {
		model.rewind();
		projectionn.rewind();
		for(int n=0;n<mm.length;n++)
			model.put((float)mm[n]);
		for(int n=0;n<mm.length;n++)
			projectionn.put((float)pm[n]);
		model.rewind();
		projectionn.rewind();
		object1.rewind();
		
		viewport.rewind();
		viewport.put(viewPort);
		viewport.rewind();
		
		GLU.gluProject(three.x, three.y, three.z, model, projectionn, viewport, object1);

		float d = new Vector3().sub(three, subCamera.getPosition(null)).dot(subCamera.getViewRay(null).normalize());

		// ;//System.out.println(" CLIP :"+three+" "+outX[2]);

		return d < nearPlane;
	}

	// gives 'ray' position, 1 unit from camera
	public void convertDrawingSpaceToIntermediate(Vector2 drawing, Vector3 world) {

		drawing = new Vector2(drawing);
		//parent.transformDrawingToWindow(drawing);

		float s = GLComponentWindow.getCurrentWindow(null).getXScale();
		drawing.x = (float) (drawing.x-viewportRectangle.x)/s;
		drawing.y = (float) (drawing.y-viewportRectangle.y)/s;

		Vector3 r1 = new Vector3();
		Vector3 r2 = new Vector3();

		
		subCamera.createIntersectionRay(drawing.x, viewPort[3]-drawing.y, r1, r2, viewPort[2], viewPort[3]);

		
		r2.sub(subCamera.getPosition(null)).normalize().add(subCamera.getPosition(null));

		world.setValue(r2);
		
//		;//System.out.println(" world :"+world);
		
	}

	public Vector3 getIntermediateSpaceForEvent(CachedLine line, CachedLine.Event e, int index) {
		
		
		Vector2 v2 = new Vector2();
		e.getAt(index, v2);
		Object z = e.getAttributes().get(iLinearGraphicsContext.z_v);
		if (index == -1 || (index == 0 && e.args.length == 2) || (index == 2 && e.args.length == 6))
			return new Vector3(v2.x, v2.y, depthFor(z));
		else if (index == 0)
			return new Vector3(v2.x, v2.y, control1for(z));
		else if (index == 1)
			return new Vector3(v2.x, v2.y, control2for(z));
		throw new ArrayIndexOutOfBoundsException(e.method + " " + index);
		// return v2.toVector3();
	}

	public void setIntermediateSpaceForEvent(CachedLine onLine, Event vertex, int index, Vector3 currentIntermediate, Vector2 currentDrawing, Vector2 targetDrawing) {
		
		// plane perp to camera is
		Vector3 perp = subCamera.getViewRay(null).normalize();
		Vector3 c1 = new Vector3(currentIntermediate).sub(subCamera.getPosition(null));
		float currentPerpDistance = c1.mag();
		Vector3 newIntermediate = new Vector3();

		
		
		convertDrawingSpaceToIntermediate(targetDrawing, newIntermediate);
		
		Vector3 newRay = newIntermediate.sub(subCamera.getPosition(null));


		float newPerpDistance = newRay.mag();
		;//System.out.println(" new perp distance "+newPerpDistance);
		newRay.scale(currentPerpDistance / newPerpDistance);

		Vector3 inter = newRay.add(subCamera.getPosition(null));

		
		Vector2 backAgain = new Vector2();
		convertIntermediateSpaceToDrawingSpace(inter, backAgain);
		
		setExtendedDrawingSpaceForEvent(onLine, vertex, index, inter);
	}

	public void setExtendedDrawingSpaceForEvent(CachedLine onLine, Event e, int index, Vector3 inter) {


		e.setAt(index, inter.toVector2());
		Object o = e.getAttributes().get(iLinearGraphicsContext.z_v);
		if (index == -1 || (index == 0 && e.args.length == 2) || (index == 2 && e.args.length == 6)) {
			if (o instanceof Number)
				e.getAttributes().put(iLinearGraphicsContext.z_v, inter.z);
			else if (o instanceof Vector3)
				((Vector3) o).z = inter.z;
		} else if (index == 0) {
			if (o instanceof Number)
				e.getAttributes().put(iLinearGraphicsContext.z_v, inter.z);
			else if (o instanceof Vector3)
				((Vector3) o).x = inter.z;
		} else if (index == 1) {
			if (o instanceof Number)
				e.getAttributes().put(iLinearGraphicsContext.z_v, inter.z);
			else if (o instanceof Vector3)
				((Vector3) o).y = inter.z;
		}
	}

	public State getTransformState() {
		State s = subCamera.getState();
		return s;
	}

	Stack<State> state = new Stack<State>();

	public void pushTransformState(Object s) {
		state.push(getTransformState());
		subCamera.setState((State) s);

		subCamera.computeModelViewNow();
		for (int i = 0; i < 16; i++)
			mm[i] = subCamera.modelView[i];
	}

	public void setTransformState(Object s) {
		state.clear();
		subCamera.setState((State) s);

		try {
			subCamera.computeModelViewNow();
			if (subCamera.modelView != null) {
				for (int i = 0; i < 16; i++)
					mm[i] = subCamera.modelView[i];
			}
		} catch (SingularMatrixException e) {
			;//System.out.println(" state not invertable <" + s + ">");
			e.printStackTrace();
		}
	}

	public void popTransformState() {
		subCamera.setState(state.pop());
		subCamera.computeModelViewNow();
		for (int i = 0; i < 16; i++)
			mm[i] = subCamera.modelView[i];
	}

	private float control2for(Object o) {
		if (o instanceof Number)
			return ((Number) o).floatValue();
		if (o instanceof Vector3)
			return ((Vector3) o).y;
		return 0;
	}

	private float control1for(Object o) {
		if (o instanceof Number)
			return ((Number) o).floatValue();
		if (o instanceof Vector3)
			return ((Vector3) o).x;
		return 0;
	}

	private float depthFor(Object o) {
		if (o instanceof Number)
			return ((Number) o).floatValue();
		if (o instanceof Vector3)
			return ((Vector3) o).z;
		return 0;
	}

	public BasicCamera getCamera() {
		return subCamera;
	}

}
