package field.core.windowing.components;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.IllegalComponentStateException;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.lwjgl.opengl.GL11;

import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElementOverrides.Ref;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.graphics.core.BasicGeometry;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.graphics.core.BasicUtilities;
import field.graphics.core.TextSystem;
import field.graphics.core.TextSystem.AribitraryComponent;
import field.graphics.dynamic.DynamicMesh;
import field.graphics.dynamic.iDynamicMesh;
import field.math.linalg.Vector4;
import field.math.linalg.iCoordinateFrame;
import field.namespace.generic.ReflectionTools;

/**
 * a single (minimal) swing component rendered as a texture map
 * 
 * right now, we're going to use a minimalslider as an example
 */
public abstract class SwingBridgeComponent extends PlainDraggableComponent {

	static public class NamedComponent extends SwingBridgeComponent {

		public String className;

		public NamedComponent() {
			this(new Rect(0, 0, 0, 0));
		}

		public NamedComponent(Rect rect) {
		}

		public JComponent getComponent() {
			return componentToRender;
		}

		@Override
		public void paint(ComponentContainer inside, iCoordinateFrame frameSoFar, boolean visible) {
			if (componentToRender == null) {

				if (className != null)
					try {
						setClass((Class<? extends JComponent>) this.getClass().getClassLoader().loadClass(className));
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
			}
			super.paint(inside, frameSoFar, visible);
		}

		public void setClass(Class<? extends JComponent> className) {
			if (componentToRender != null && className.isInstance(componentToRender))
				return;
			this.className = className.getName();
			try {
				componentToRender = className.newInstance();
				hookupNotifications();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		public void setInstance(JComponent className) {
			this.className = className.getClass().getName();

			componentToRender = className;
			hookupNotifications();
		}
	}

	static public int defaultMaxComponentDimension = 512;

	public transient JComponent componentToRender;

	protected TriangleMesh triangles;

	private AribitraryComponent aribitraryComponent;

	protected iDynamicMesh labelTriangle;

	boolean dirty = true;

	Rect lastPaintedRect = new Rect(0, 0, -1, -1);

	public SwingBridgeComponent() {
		this(new Rect(0, 0, 0, 0));
	}

	public SwingBridgeComponent(Rect bounds) {
		super(bounds);
		triangles = new BasicGeometry.TriangleMesh(new BasicUtilities.Position());
		triangles.rebuildTriangle(0).rebuildVertex(0);
		labelTriangle = new DynamicMesh(triangles);

		// this.isSelctedable = false;

	}

	protected int getMaxDimension() {
		return defaultMaxComponentDimension;
	}

	public void hookupNotifications() {
		if (componentToRender != null) {
			componentToRender.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					Dimension size = componentToRender.getSize();
					if (Math.abs(size.width - bounds.w) > 1 || Math.abs(size.height - bounds.h) > 1) {
						shouldSetSize((float) bounds.x, (float) bounds.y, size.width, size.height);
					}
					setDirty();
				}
			});
		}
	}

	@Override
	public void setSelected(boolean selected) {
		if (this.selected != selected)
			setDirty();

		super.setSelected(selected);

		if (this.selected) {
			backgroundR = obackgroundR;
			backgroundG = obackgroundG;
			backgroundB = obackgroundB;
			backgroundA = obackgroundA;
		} else {
			backgroundR = obackgroundR;
			backgroundG = obackgroundG;
			backgroundB = obackgroundB;
			backgroundA = obackgroundA;

		}

	}

	@Override
	public float isHit(Event event) {
		if (componentToRender == null)
			return Float.NEGATIVE_INFINITY;

		if (event.x >= bounds.x && event.y >= bounds.y && event.x <= (bounds.x + bounds.w) && event.y <= (bounds.y + bounds.h)) {
			Ref<Boolean> is = new Ref<Boolean>(true);
			overridingInterface.isHit(element, event, is);
			return is.get() ? bounds.size() : Float.NEGATIVE_INFINITY;
		} else {
			Ref<Boolean> is = new Ref<Boolean>(false);
			overridingInterface.isHit(element, event, is);
			return is.get() ? 1 - bounds.size() : Float.NEGATIVE_INFINITY;
		}
	}

	@Override
	public void mouseClicked(ComponentContainer inside, Event arg0) {
		if (!shouldSendUpwards(arg0)) {
			if (componentToRender != null) {
				MouseEvent me = rewriteCoordinates(arg0, false);

				if (me == null)
					return;

				Component comp = SwingUtilities.getDeepestComponentAt(componentToRender, me.getX(), me.getY());
				me = SwingUtilities.convertMouseEvent(componentToRender, me, comp);
				me.setSource(comp);
				componentToRender.dispatchEvent(me);

				dirty = true;
				inside.requestRedisplay();
			}
		} else
			super.mouseClicked(inside, arg0);

		setDirty();
	}

	// @Override
	// public void keyPressed(ComponentContainer inside, KeyEvent
	// arg0) {
	// if (arg0 != null)
	// if (componentToRender != null) {
	// componentToRender.requestFocus();
	// }
	// super.keyPressed(inside, arg0);
	// }
	//
	// @Override
	// public void keyReleased(ComponentContainer inside, KeyEvent
	// arg0) {
	// if (arg0 != null)
	// if (componentToRender != null) {
	// componentToRender.requestFocus();
	// componentToRender.dispatchEvent(arg0);
	// }
	// super.keyReleased(inside, arg0);
	// }
	//
	// @Override
	// public void keyTyped(ComponentContainer inside, KeyEvent
	// arg0) {
	// if (arg0 != null)
	// if (componentToRender != null) {
	// componentToRender.requestFocus();
	// componentToRender.dispatchEvent(arg0);
	// }
	// super.keyTyped(inside, arg0);
	// }

	@Override
	public void mouseDragged(ComponentContainer inside, Event arg0) {

		;//System.out.println(" wrap drag ");

		if (!shouldSendUpwards(arg0))
			try {
				if (componentToRender != null) {
					;//System.out.println(" mouse dragged AA :"+arg0);

					MouseEvent me = rewriteCoordinates(arg0, true);

					
					if (me == null)
						return;

					
					;//System.out.println(" mouse dragged A :"+me.getX());

					Component comp = SwingUtilities.getDeepestComponentAt(componentToRender, me.getX(), me.getY());
					if (comp==null) return;
					me = SwingUtilities.convertMouseEvent(componentToRender, me, comp);
					me.setSource(comp);


					;//System.out.println(" converted to :"+me.getX()+" via "+comp);

					comp.dispatchEvent(me);

					// componentToRender.dispatchEvent(me);

					dirty = true;
					inside.requestRedisplay();
				}
			} catch (IllegalComponentStateException icse) {
				icse.printStackTrace();
			}
		else
			super.mouseDragged(inside, arg0);
	}

	@Override
	public void mouseEntered(ComponentContainer inside, Event arg0) {
		if (!shouldSendUpwards(arg0)) {
			if (componentToRender != null) {
				try {
					MouseEvent me = rewriteCoordinates(arg0, false);
					if (me == null)
						return;

					Component comp = SwingUtilities.getDeepestComponentAt(componentToRender, me.getX(), me.getY());
					if (comp==null) return;
					me = SwingUtilities.convertMouseEvent(componentToRender, me, comp);
					me.setSource(comp);

					comp.dispatchEvent(me);

					// componentToRender.dispatchEvent(me);

					dirty = true;
					inside.requestRedisplay();
				} catch (Error r) {

				}
			}
		} else
			super.mouseEntered(inside, arg0);
	}

	@Override
	public void mouseExited(ComponentContainer inside, Event arg0) {
		if (!shouldSendUpwards(arg0)) {
			if (componentToRender != null) {

				try {
					MouseEvent me = rewriteCoordinates(arg0, false);
					if (me == null)
						return;

					// Component comp =
					// SwingUtilities.getDeepestComponentAt(
					// componentToRender, me.getX(),
					// me.getY());
					// SwingUtilities.convertMouseEvent(componentToRender,
					// me, comp);
					// me.setSource(comp);
					// componentToRender.dispatchEvent(me);

					Component comp = SwingUtilities.getDeepestComponentAt(componentToRender, me.getX(), me.getY());
					if (comp==null) return;
					
					me = SwingUtilities.convertMouseEvent(componentToRender, me, comp);
					me.setSource(comp);

					comp.dispatchEvent(me);

					dirty = true;
					inside.requestRedisplay();
				} catch (Error r) {

				}
			}
		} else
			super.mouseExited(inside, arg0);
	}

	@Override
	public void mousePressed(ComponentContainer inside, Event arg0) {

		if (!shouldSendUpwards(arg0)) {
			if (componentToRender != null) {
				
				;//System.out.println(" mouse pressed AA :"+arg0);
				
				MouseEvent me = rewriteCoordinates(arg0, false);
				if (me == null)
					return;

				
				
				// Component comp =
				// SwingUtilities.getDeepestComponentAt(
				// componentToRender, me.getX(), me.getY());
				// SwingUtilities.convertMouseEvent(componentToRender,
				// me, comp);
				// me.setSource(comp);
				// componentToRender.dispatchEvent(me);
				
				
				;//System.out.println(" mouse pressed A :"+me.getX());
				
				
				Component comp = SwingUtilities.getDeepestComponentAt(componentToRender, me.getX(), me.getY());
				if (comp==null) return;
				me = SwingUtilities.convertMouseEvent(componentToRender, me, comp);
				me.setSource(comp);

				;//System.out.println(" converted to <"+me.getX()+"> via <"+comp+">");
				
				comp.dispatchEvent(me);

				dirty = true;
				inside.requestRedisplay();
			}
		} else
			super.mousePressed(inside, arg0);

	}

	public float obackgroundR = 0.5f;
	public float obackgroundG = 0.5f;
	public float obackgroundB = 0.5f;
	public float obackgroundA = 1f;

	public float backgroundR = obackgroundR;
	public float backgroundG = obackgroundG;
	public float backgroundB = obackgroundB;
	public float backgroundA = obackgroundA;

	@Override
	public void mouseReleased(ComponentContainer inside, Event arg0) {
		if (!shouldSendUpwards(arg0)) {
			if (componentToRender != null) {
				MouseEvent me = rewriteCoordinates(arg0, false);
				if (me == null)
					return;

				// Component comp =
				// SwingUtilities.getDeepestComponentAt(
				// componentToRender, me.getX(), me.getY());
				// SwingUtilities.convertMouseEvent(componentToRender,
				// me, comp);
				// me.setSource(comp);
				// componentToRender.dispatchEvent(me);
				Component comp = SwingUtilities.getDeepestComponentAt(componentToRender, me.getX(), me.getY());
				if (comp==null) return;
				me = SwingUtilities.convertMouseEvent(componentToRender, me, comp);
				me.setSource(comp);

				;//System.out.println(" deepest component at returns <" + comp + "> <" + me + ">");

				comp.dispatchEvent(me);

				dirty = true;
				inside.requestRedisplay();
			}
		} else
			super.mouseReleased(inside, arg0);
	}

	public boolean doName = true;

	@Override
	public void paint(ComponentContainer inside, iCoordinateFrame frameSoFar, boolean visible) {
		this.inside = inside;

		Object oldParent = null;

//		;//System.out.println(" component to render is <" + componentToRender + ">");

		try {
			oldParent = ReflectionTools.illegalGetObject(componentToRender, "next");
			ReflectionTools.illegalSetObject(componentToRender, "next", GLComponentWindow.getCurrentWindow(this).getFrame());
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

		super.paint(inside, frameSoFar, visible);

//		dirty = true;

		if (componentToRender != null) {
			if (dirty) {
				componentToRender.setBounds((int) this.getBounds().x, (int) this.getBounds().y, (int) this.getBounds().w, (int) this.getBounds().h);
				// componentToRender.setBounds(0,0,
				// (int)
				// this.getBounds().w,
				// (int)
				// this.getBounds().h);
				getAribitraryComponent().resetImage(componentToRender, backgroundR, backgroundG, backgroundB, backgroundA);
			}
			getAribitraryComponent().drawIntoMesh(labelTriangle, 1, 1, 1, 1, (float) bounds.x, (float) bounds.y);
			GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
			GL11.glDisable(GL11.GL_DEPTH_TEST);

			glActiveTexture(GL_TEXTURE1);
			getAribitraryComponent().on();
			// BasicContextManager.gl.glBlendFunc(GL.GL_ZERO,
			// GL.GL_SRC_COLOR);
			triangles.performPass(null);
			// BasicContextManager.gl.glBlendFunc(GL.GL_SRC_ALPHA,
			// GL.GL_ONE_MINUS_SRC_ALPHA);
			getAribitraryComponent().off();
			glActiveTexture(GL_TEXTURE0);
			dirty = false;
		}

		iLinearGraphicsContext cc = GLComponentWindow.currentContext;
		if (doName) {
			CachedLine c = new CachedLine();
			c.getInput().moveTo((float) (this.getBounds().x + this.getBounds().w + 4), (float) (this.getBounds().y + this.getBounds().h - 6));
			c.getProperties().put(iLinearGraphicsContext.containsText, true);
			c.getInput().setPointAttribute(iLinearGraphicsContext.text_v, "(" + this.element.getProperty(iVisualElement.name) + ")");
			c.getInput().setPointAttribute(iLinearGraphicsContext.font_v, new Font("Gill Sans", 0, 10));
			cc.submitLine(c, c.getProperties());
		}
		{
			CachedLine c = new CachedLine();
			c.getInput().moveTo((float) this.getBounds().x, (float) (this.getBounds().y));
			c.getInput().lineTo((float) (this.getBounds().x + this.getBounds().w), (float) this.getBounds().y);
			c.getInput().lineTo((float) (this.getBounds().x + this.getBounds().w), (float) (this.getBounds().y + this.getBounds().h));
			c.getInput().lineTo((float) (this.getBounds().x), (float) (this.getBounds().y + this.getBounds().h));
			c.getInput().lineTo((float) (this.getBounds().x), (float) (this.getBounds().y));

			c.getProperties().put(iLinearGraphicsContext.color, selected ? new Vector4(0, 0, 0, 1f) : new Vector4(1, 1, 1, 0.1f));
			c.getProperties().put(iLinearGraphicsContext.thickness, selected ? 2f : 1f);

			cc.submitLine(c, c.getProperties());
		}

		// this.bounds.w = componentToRender.getWidth();
		// this.bounds.h =
		// componentToRender.getHeight();

		try {
			ReflectionTools.illegalSetObject(componentToRender, "next", oldParent);
		} catch (Exception e) {
			e.printStackTrace();
		}

		lastPaintedRect.setValue(bounds);
	}

	@Override
	public void setBounds(Rect r) {
		if (!lastPaintedRect.equals(bounds)) {
			if (bounds.w != lastPaintedRect.w || bounds.h != lastPaintedRect.h)
				dirty = true;
			setDirty();
		}
		super.setBounds(r);
	}

	@Override
	public void setDirty() {
		super.setDirty();
		this.dirty = true;
	}

	protected MouseEvent rewriteCoordinates(Event arg0, boolean isDrag) {

		int x = arg0.x;
		int y = arg0.y;

		// Rectangle b = componentToRender.getBounds();

		x -= bounds.x;
		y -= bounds.y;

		// return new Event(arg0.getComponent(),
		// arg0.getID(), arg0.getWhen(),
		// arg0.getModifiers(), x, y,
		// arg0.getClickCount(), arg0.isPopupTrigger(),
		// arg0.getButton());

		switch (arg0.type) {
		case (SWT.MouseDown): {
			MouseEvent m = new MouseEvent(componentToRender, MouseEvent.MOUSE_PRESSED, arg0.time, 0, x, y, x, y, arg0.count, Platform.isPopupTrigger(arg0), arg0.button);
			return m;
		}
		case (SWT.MouseUp): {
			MouseEvent m = new MouseEvent(componentToRender, MouseEvent.MOUSE_RELEASED, arg0.time, 0, x, y, x, y, arg0.count, Platform.isPopupTrigger(arg0), arg0.button);
			return m;
		}
		case (SWT.MouseMove): {
			if (!isDrag) {
				MouseEvent m = new MouseEvent(componentToRender, MouseEvent.MOUSE_MOVED, arg0.time, 0, x, y, x, y, arg0.count, Platform.isPopupTrigger(arg0), arg0.button);
				return m;
			} else {
				MouseEvent m = new MouseEvent(componentToRender, MouseEvent.MOUSE_DRAGGED, arg0.time, 0, x, y, x, y, arg0.count, Platform.isPopupTrigger(arg0), arg0.button);
				return m;

			}
		}
		}

		return null;

	}

	protected boolean shouldSendUpwards(Event arg0) {
		return true;
	}

	protected void shouldSetSize(float x, float y, float w, float h) {
		Rect oldBounds = new Rect(bounds.x, bounds.y, bounds.w, bounds.h);
		bounds.x = x;
		bounds.y = y;
		bounds.w = Math.max(w, 2);
		bounds.h = Math.max(h, 2);
		if (element != null) {
			overridingInterface.shouldChangeFrame(element, getBounds(), oldBounds, true);
		}

		if (inside != null) {
			dirty = true;
			inside.requestRedisplay();
		}
	}

	protected void setAribitraryComponent(AribitraryComponent aribitraryComponent) {
		this.aribitraryComponent = aribitraryComponent;
	}

	protected AribitraryComponent getAribitraryComponent() {
		if (aribitraryComponent == null)
			aribitraryComponent = TextSystem.textSystem.new AribitraryComponent(getMaxDimension(), getMaxDimension());
		aribitraryComponent.getTexture().use_gl_texture_rectangle_ext(false);
		return aribitraryComponent;
	}
}
