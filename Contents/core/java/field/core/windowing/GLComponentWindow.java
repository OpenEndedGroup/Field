package field.core.windowing;

import static org.lwjgl.opengl.GL11.GL_VENDOR;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.glGetString;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.WeakHashMap;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.GestureEvent;
import org.eclipse.swt.events.GestureListener;
import org.eclipse.swt.events.TouchEvent;
import org.eclipse.swt.events.TouchListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.GLU;

import field.bytecode.protect.DeferedInQueue.iProvidesQueue;
import field.bytecode.protect.DeferedInQueue.iRegistersUpdateable;
import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.DispatchOverTopology;
import field.bytecode.protect.annotations.InQueue;
import field.bytecode.protect.annotations.Yield;
import field.bytecode.protect.dispatch.Cont;
import field.bytecode.protect.yield.YieldUtilities;
import field.core.Constants;
import field.core.Platform;
import field.core.Platform.OS;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.drawing.threed.ThreedContext.iThreedDrawingSurface;
import field.core.plugins.selection.ToolBarFolder;
import field.core.ui.ComponentWindowDropShadow;
import field.core.ui.ComponentWindowStatusbar;
import field.core.ui.PresentationMode;
import field.core.windowing.components.DraggableComponent;
import field.core.windowing.components.DraggableComponent.Resize;
import field.core.windowing.components.PlainComponent;
import field.core.windowing.components.PlainDraggableComponent;
import field.core.windowing.components.iComponent;
import field.graphics.core.BasicContextManager;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.BasicSceneList;
import field.graphics.core.CoreHelpers;
import field.graphics.core.ResourceMonitor;
import field.graphics.core.TextSystem;
import field.launch.Launcher;
import field.launch.SystemProperties;
import field.launch.iUpdateable;
import field.math.BaseMath;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;
import field.math.linalg.iCoordinateFrame;
import field.namespace.context.SimpleContextTopology;
import field.namespace.generic.ReflectionTools;
import field.util.AutoPersist;
import field.util.TaskQueue;

@Woven
public class GLComponentWindow implements Listener, iUpdateable, iProvidesQueue, iThreedDrawingSurface {

	static public Set<Integer> keysDown = new LinkedHashSet<Integer>();

	public LinkedHashMap<Object, String> extraTextStatusbarDescriptions = new LinkedHashMap<Object, String>();

	static public boolean fieldStereo = SystemProperties.getIntProperty("fieldStereo", 0) == 1;

	static public Rectangle defaultRect = new AutoPersist().persist("canvas", new Rectangle(0, 0, 0, 0));

	static public class ComponentContainer implements iComponent {

		private final ComponentContainer parent;

		List<iComponent> components = new ArrayList<iComponent>();

		List<iComponent> allEvents = new ArrayList<iComponent>();

		private boolean needsRedisplay = false;

		boolean focusLocked = false;

		iComponent focusLockedTo = null;

		iComponent mouseFocus = null;

		Stack<iComponent> mouseFocusStack = new Stack<iComponent>();

		iComponent lastOver = null;

		public ComponentContainer(ComponentContainer parent) {
			this.parent = parent;
		}

		public void addAsAllEventHandler(iComponent component) {
			if (!allEvents.contains(component))
				allEvents.add(component);
		}

		public void addComponent(iComponent component) {
			components.add(component);

		}

		public void beginMouseFocus(ComponentContainer inside) {
		}

		public void display(ComponentContainer inside, iCoordinateFrame frameSoFar) {
			setNeedsRedisplay(false);
			for (iComponent c : components)
				c.paint(this, new CoordinateFrame(), true);
		}

		public void endMouseFocus(ComponentContainer inside) {
		}

		public Rect getBounds() {
			return null;
		}

		public ComponentContainer getParent() {
			return parent;
		}

		public iRegistersUpdateable getQueueFor(Method m) {
			return getCurrentWindow(this).eventProcessingTaskQueue;
		}

		public ComponentContainer getRoot() {
			ComponentContainer cc = this;
			ComponentContainer was = this;
			while (cc != null) {
				was = cc;
				cc = cc.getParent();
			}
			return was;
		}

		public iVisualElement getVisualElement() {
			return null;
		}

		public void handleResize(Set<Resize> currentResize, float dx, float dy) {
		}

		public iComponent hit(Event arg0) {
			if (focusLocked)
				return focusLockedTo;
			iComponent hit = null;
			float best = Float.NEGATIVE_INFINITY;
			for (int i = components.size() - 1; i >= 0; i--) {
				iComponent c = components.get(i);

				if (PresentationMode.isHidden(c))
					continue;

				float z = c.isHit(arg0);
				if (z > best) {
					iComponent cc = c.hit(arg0);
					if (cc != null) {
						hit = cc;
						best = z;
					}
				}
			}
			return hit;
		}

		public iComponent hit(GLComponentWindow window, Vector2 arg0) {

			// MouseEvent e = new MouseEvent(window.getCanvas(),
			// MouseEvent.MOUSE_PRESSED, 0, 0, (int) arg0.x, (int)
			// arg0.y, 1,
			// false);
			// window.transformMouseEvent(e);

			Event e = new Event();
			e.data = window.getCanvas();
			e.type = SWT.MouseDown;
			e.x = (int) arg0.x;
			e.y = (int) arg0.y;

			if (focusLocked)
				return focusLockedTo;
			iComponent hit = null;
			float best = Float.NEGATIVE_INFINITY;
			for (int i = components.size() - 1; i >= 0; i--) {
				iComponent c = components.get(i);

				if (PresentationMode.isHidden(c))
					continue;

				float z = c.isHit(e);
				if (z > best) {
					iComponent cc = c.hit(e);
					if (cc != null) {
						hit = cc;
						best = z;
					}
				}
			}
			return hit;
		}

		public float isHit(Event event) {
			return hit(event) != null ? 1 : Float.NEGATIVE_INFINITY;
		}

		public boolean isSelected() {
			return false;
		}

		public void keyPressed(ComponentContainer from, Event arg0) {

			// TODO swt keyboard dispatch
			if (arg0.keyCode == SWT.CAPS_LOCK)
				setNeedsRedisplay(true);

			if (keysDown.add(arg0.keyCode))
				setNeedsRedisplay(true);

			if (arg0.keyCode == SWT.F1) {
				getCurrentWindow(this).resetViewParameters();
				setNeedsRedisplay(true);
			} else if (arg0.keyCode == '=' && (arg0.stateMask & Platform.getCommandModifier()) != 0 && (arg0.stateMask & SWT.SHIFT) == 0) {
				;//System.out.println(" zoom in ");
				getCurrentWindow(this).sx /= 1.25;
				getCurrentWindow(this).sy /= 1.25;
				getCurrentWindow(this).clampViewParameters();
				setNeedsRedisplay(true);
			} else if (arg0.keyCode == '-' && (arg0.stateMask & Platform.getCommandModifier()) != 0) {
				;//System.out.println(" zoom in ");
				getCurrentWindow(this).sx *= 1.25;
				getCurrentWindow(this).sy *= 1.25;
				getCurrentWindow(this).clampViewParameters();
				setNeedsRedisplay(true);
			} else if (arg0.character=='*') {
				getCurrentWindow(this).toggleContinuousRepaintNow();
			}

			for (iComponent c : allEvents)
				if (c != mouseFocus) {
					c.keyPressed(this, arg0);
				}
			// if (arg0.getKeyCode() == arg0.VK_BACK_QUOTE) {
			// if (focusLocked) {
			// if (focusLockedTo != null)
			// if (focusLockedTo.getVisualElement() != null)
			// iVisualElement.hasFocusLock.set(focusLockedTo.getVisualElement(),
			// focusLockedTo.getVisualElement(), false);
			// focusLocked = false;
			// } else {
			//
			// // need
			// // to
			// // gen
			// // a
			// // mouse
			// // event
			// // !
			// final MouseEvent me = new
			// MouseEvent(arg0.getComponent(),
			// MouseEvent.MOUSE_CLICKED, arg0.getWhen(),
			// arg0.getModifiers(),
			// (int) currentWindow.currentMouse.x, (int)
			// currentWindow.currentMouse.y, 1, false);
			//
			// final List<Pair<Float, iComponent>> orderedList = new
			// ArrayList<Pair<Float, iComponent>>();
			//
			// iComponent hit = null;
			// float best = Float.NEGATIVE_INFINITY;
			// for (int i = components.size() - 1; i >= 0; i--) {
			// iComponent c = components.get(i);
			//
			// if (PresentationMode.isHidden(c))
			// continue;
			//
			// float z = c.isHit(me);
			// if (z > Float.NEGATIVE_INFINITY) {
			// iComponent cc = c.hit(me);
			// orderedList.add(new Pair<Float, iComponent>(z, cc));
			// }
			// }
			//
			// Collections.sort(orderedList, new
			// Comparator<Pair<Float,
			// iComponent>>() {
			// public int compare(Pair<Float, iComponent> o1,
			// Pair<Float,
			// iComponent> o2) {
			// return o1.left < o2.left ? 1 : -1;
			// }
			// });
			//
			// if (orderedList.size() > 0) {
			// LinkedHashMap<String, iUpdateable> options = new
			// LinkedHashMap<String, iUpdateable>();
			// options.put("Select...", null);
			// for (int j = 0; j < orderedList.size(); j++) {
			// final int i = j;
			// iVisualElement q =
			// orderedList.get(i).right.getVisualElement();
			// if (q != null)
			// options.put("    " + iVisualElement.name.get(q), new
			// iUpdateable() {
			//
			// public void update() {
			// focusLocked = true;
			// iComponent hit = orderedList.get(i).right;
			// focusLockedTo = hit;
			// if (focusLockedTo != null)
			// if (focusLockedTo.getVisualElement() != null)
			// iVisualElement.hasFocusLock.set(focusLockedTo.getVisualElement(),
			// focusLockedTo.getVisualElement(), true);
			//
			// mouseClicked(ComponentContainer.this, me);
			// }
			// });
			// }
			// options.put("    nothing", new iUpdateable() {
			//
			// public void update() {
			// focusLocked = false;
			// mouseClicked(ComponentContainer.this, me);
			// }
			// });
			// Vector2 win = new
			// Vector2(currentWindow.currentMouse);
			// currentWindow.transformDrawingToWindow(win);
			// new
			// SmallMenu().createMenu(options).show(currentWindow.getCanvas(),
			// (int) win.x, (int) win.y);
			// }
			// }
			// }

			for (iComponent c : new ArrayList<iComponent>(components))
				c.keyPressed(this, arg0);

			// if (arg0.getKeyCode() == Event.VK_F1) {
			// if (getCurrentWindow(this).isOnGraphicsWindow)
			// getCurrentWindow(this).fadeOverlay();
			// }
		}

		public void keyReleased(ComponentContainer from, Event arg0) {

			for (iComponent c : allEvents)
				if (c != mouseFocus) {
					c.keyReleased(this, arg0);
				}

			for (iComponent c : new ArrayList<iComponent>(components))
				c.keyReleased(this, arg0);

			if (arg0.keyCode == SWT.CAPS_LOCK)
				setNeedsRedisplay(true);

			if (keysDown.remove(arg0.keyCode))
				setNeedsRedisplay(true);

		}

		public void keyTyped(ComponentContainer from, Event arg0) {
			for (iComponent c : allEvents)
				if (c != mouseFocus) {
					c.keyTyped(this, arg0);
				}
			for (iComponent c : new ArrayList<iComponent>(components))
				c.keyTyped(this, arg0);
		}

		public void mouseClicked(ComponentContainer from, Event arg0) {
			beginMouseFocus(this, arg0);

			for (iComponent c : new ArrayList<iComponent>(allEvents))
				if (c != mouseFocus)
					c.mouseClicked(this, arg0);

			PresentationMode.transformOptionClick(mouseFocus, arg0);

			if (mouseFocus != null && arg0.doit) {
				mouseFocus.mouseClicked(this, arg0);
			}

			endMouseFocus(this, arg0);
		}

		public void mouseDragged(ComponentContainer from, Event arg0) {
			GLComponentWindow.currentWindow.alwaysVisible = false;

			for (iComponent c : allEvents)
				if (c != mouseFocus)
					c.mouseDragged(this, arg0);

			PresentationMode.transformOptionClick(mouseFocus, arg0);

			if (mouseFocus != null && arg0.doit) {
				mouseFocus.mouseDragged(this, arg0);
			}
		}

		public void mouseEntered(ComponentContainer from, Event arg0) {
			beginMouseFocus(this, arg0);
			if (mouseFocus != null) {
				mouseFocus.mouseEntered(this, arg0);
			}
		}

		public void mouseExited(ComponentContainer from, Event arg0) {
			if (mouseFocus != null) {
				mouseFocus.mouseExited(this, arg0);
			}
			endMouseFocus(this, arg0);
		}

		public void mouseMoved(ComponentContainer from, Event arg0) {
			iComponent h = hit(arg0);
			if (h != lastOver) {
				if (lastOver != null) {
					lastOver.mouseExited(from, arg0);
					endMouseFocus(this, arg0);
				}
				if (h != null) {
					beginMouseFocus(this, arg0);
					h.mouseEntered(from, arg0);
				}
				lastOver = h;
			}
			if (h != null)
				h.mouseMoved(this, arg0);

			for (iComponent c : allEvents)
				if (c != h)
					c.mouseMoved(this, arg0);

		}

		public void mousePressed(ComponentContainer from, Event arg0) {
			beginMouseFocus(this, arg0);

			for (iComponent c : allEvents)
				if (c != mouseFocus) {
					c.mousePressed(this, arg0);
				}

			// TODO swt
			PresentationMode.transformOptionClick(mouseFocus, arg0);

			if (PresentationMode.isSpace(mouseFocus, arg0)) {

				// Event syntheticSpace = new Event((Component)
				// arg0.getSource(), Event.KEY_PRESSED,
				// arg0.getWhen(), 0, Event.VK_SPACE, ' ');

				arg0.character = ' ';
				arg0.keyCode = ' ';
				arg0.type = SWT.KeyDown;
				mouseFocus.keyPressed(this, arg0);

			} else {
				if (mouseFocus != null && arg0.doit) {
					mouseFocus.mousePressed(this, arg0);
				}
			}
		}

		public void mouseReleased(ComponentContainer from, Event arg0) {
			GLComponentWindow.currentWindow.alwaysVisible = true;

			for (iComponent c : allEvents)
				if (c != mouseFocus) {
					c.mouseReleased(this, arg0);
				}

			// TODO swt
			PresentationMode.transformOptionClick(mouseFocus, arg0);

			if (mouseFocus != null && arg0.doit) {
				mouseFocus.mouseReleased(this, arg0);
			}
			endMouseFocus(this, arg0);

		}

		public void moveToBack(iComponent component) {
			components.remove(component);
			components.add(0, component);
		}

		public void moveToFront(iComponent component) {
			components.remove(component);
			components.add(component);
		}

		public void paint(ComponentContainer inside, iCoordinateFrame frameSoFar, boolean visible) {

			float w = GLComponentWindow.currentWindow.w;
			float h = GLComponentWindow.currentWindow.h;
			float tx = GLComponentWindow.currentWindow.tx;
			float ty = GLComponentWindow.currentWindow.ty;
			float sx = GLComponentWindow.currentWindow.sx;
			float sy = GLComponentWindow.currentWindow.sy;

			for (iComponent c : new ArrayList<iComponent>(components)) {
				// tmp hack,
				// vfculling
				boolean skip = false;
				if (c instanceof DraggableComponent) {
					Rect bounds = (c).getBounds();
					if (bounds.x > w * sx + tx || bounds.y > h * sy + ty || (bounds.x + bounds.w) < tx || (bounds.y + bounds.h) < ty) {
						skip = true;
					}
				} else if (c instanceof PlainComponent) {
					Rect bounds = ((PlainComponent) c).getBounds();
					if (bounds.x > w * sx + tx || bounds.y > h * sy + ty || (bounds.x + bounds.w) < tx || (bounds.y + bounds.h) < ty) {
						skip = true;
					}
				} else if (c instanceof PlainDraggableComponent) {
					Rect bounds = ((PlainDraggableComponent) c).getBounds();
					if (bounds.x > w * sx + tx || bounds.y > h * sy + ty || (bounds.x + bounds.w) < tx || (bounds.y + bounds.h) < ty) {
						skip = true;
					}
				}

				if (PresentationMode.isHidden(c))
					continue;

				// skip = false;
				// if (!skip)

				c.paint(inside, frameSoFar, true || GLComponentWindow.currentWindow.alwaysVisible || !skip);
			}
		}

		public void removeAsAllEventHandler(iComponent component) {
			allEvents.remove(component);
		}

		public void removeComponent(iComponent component) {
			components.remove(component);
		}

		public void requestRedisplay() {
			setNeedsRedisplay(true);
			if (parent != null)
				parent.requestRedisplay();

		}

		public void setBounds(Rect r) {
		}

		public void setSelected(boolean selected) {
		}

		public iComponent setVisualElement(iVisualElement ve) {
			return this;
		}

		public void update() {
		}

		private void beginMouseFocus(ComponentContainer from, Event arg0) {
			iComponent hit = hit(arg0);
			if (hit != null) {
				mouseFocusStack.push(hit);
				mouseFocus = hit;
				mouseFocus.beginMouseFocus(this);
			}
		}

		private void endMouseFocus(ComponentContainer from, Event arg0) {

			if (mouseFocus != null)
				mouseFocus.endMouseFocus(this);
			if (mouseFocusStack.size() > 0)
				mouseFocusStack.pop();
			if (mouseFocusStack.size() > 0)
				mouseFocus = mouseFocusStack.peek();
			else
				mouseFocus = null;
		}

		public void lockFocusTo(iComponent component) {
			focusLocked = true;
			focusLockedTo = component;
			iVisualElement.hasFocusLock.set(focusLockedTo.getVisualElement(), focusLockedTo.getVisualElement(), true);
		}

		public void unlockFocus() {
			if (focusLocked) {
				focusLocked = false;
				iVisualElement.hasFocusLock.set(focusLockedTo.getVisualElement(), focusLockedTo.getVisualElement(), false);
			}
		}

		void setNeedsRedisplay(boolean needsRedisplay) {
			this.needsRedisplay = needsRedisplay;
		}

		boolean isNeedsRedisplay() {
			return needsRedisplay;
		}

	}

	public static String rendererInfo = null;

	// TODO swt

	@Woven
	public class NUpPNGSaver extends TaskQueue.Task {
		private final int numWide;

		private final int numHigh;

		private final String filename;

		private final TaskQueue queue;

		private final int maxW;

		private final int maxH;

		float w, h, tx, ty, sx, sy;

		public NUpPNGSaver(String filename, TaskQueue queue, int width, int height) {
			this(filename, queue, width, height, width, height);
		}

		public NUpPNGSaver(String filename, TaskQueue queue, int width, int height, int maxW, int maxH) {
			queue.super();
			this.filename = filename;
			this.queue = queue;
			this.numWide = width;
			this.numHigh = height;
			this.maxW = maxW;
			this.maxH = maxH;
			w = GLComponentWindow.currentWindow.w;
			h = GLComponentWindow.currentWindow.h - 22;
			tx = GLComponentWindow.currentWindow.tx;
			ty = GLComponentWindow.currentWindow.ty;
			sx = GLComponentWindow.currentWindow.sx;
			sy = GLComponentWindow.currentWindow.sy;
		}

		@Override
		@Yield
		protected void run() {
			GLComponentWindow.currentWindow.alwaysVisible = alwaysVisibleForSave;

			int width = (int) w;
			int height = (int) h;
			ByteBuffer storage = ByteBuffer.allocateDirect(width * height * 4);

			for (int cy = 0; cy < maxH; cy++) {
				for (int cx = 0; cx < maxW; cx++) {

					GLComponentWindow.currentWindow.sx = sx / numWide;
					GLComponentWindow.currentWindow.sy = sy / numHigh;
					GLComponentWindow.currentWindow.tx = tx + w * sx * cx / numWide;
					GLComponentWindow.currentWindow.ty = ty + h * sy * cy / numHigh;

					componentWindowStatusbar.off();
					componentWindowDropShadow.off();

					root.setNeedsRedisplay(true);

					recur();
					YieldUtilities.yield(null);

					GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, storage);
					BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

					WritableRaster tile = bi.getWritableTile(0, 0);
					DataBuffer buffer = tile.getDataBuffer();

					IntBuffer storagei = storage.asIntBuffer();
					for (int y = 0; y < height; y++) {
						for (int x = 0; x < width; x++) {
							int r = BaseMath.intify(storage.get());
							int g = BaseMath.intify(storage.get());
							int b = BaseMath.intify(storage.get());
							int a = BaseMath.intify(storage.get());

							int d = (g << 8) | (r << 16) | (b);
							buffer.setElem((height - 1 - y) * width + x, d);
						}
					}

					FileOutputStream fos;
					RenderedOp op = JAI.create("filestore", bi, filename + cx + "." + cy + ".png", "PNG");
					storage.rewind();
				}
			}

			queue.removeTask(this);

			GLComponentWindow.currentWindow.sx = sx;
			GLComponentWindow.currentWindow.sy = sy;
			GLComponentWindow.currentWindow.tx = tx;
			GLComponentWindow.currentWindow.ty = ty;

			root.setNeedsRedisplay(true);
			GLComponentWindow.currentWindow.alwaysVisible = false;
		}
	}

	static public final Method method_display = ReflectionTools.methodOf("display", GLComponentWindow.class);
	static public final Method method_update = ReflectionTools.methodOf("update", GLComponentWindow.class);
	static public final Method method_doRender = ReflectionTools.methodOf("doRender", GLComponentWindow.class);

	static public boolean isTransparent = false;

	static public iLinearGraphicsContext currentContext;

	static public iLinearGraphicsContext fastContext;

	static public GLComponentWindow glComponentWindowThatLastGotFocus = null;

	static public boolean draft = SystemProperties.getIntProperty("presentationMode", 0) == 0;
	static public boolean present = SystemProperties.getIntProperty("presentationMode", 0) == 1;

	static public final SimpleContextTopology context = new SimpleContextTopology();

	static public boolean pure2d = true;

	static public boolean alwaysVisibleForSave = true;

	static protected int uniq = 0;

	static protected GLComponentWindow currentWindow;

	static WeakHashMap<Event, Vector2> floatResMouseEvents = new WeakHashMap<Event, Vector2>();

	static public GLComponentWindow getCurrentWindow(iComponent component) {
		return currentWindow;
	}

	static public Vector2 mousePositionForEvent(Event arg0) {
		Vector2 at = floatResMouseEvents.get(arg0);
		if (at == null) {
			at = new Vector2();
			at.x = arg0.x;
			at.y = arg0.y;
		}

		return at;
	}

	private final Shell frame;

	final Canvas canvas;
	// final org.eclipse.swt.widgets.Canvas canvas;

	public TextSystem textSystem;

	private final String name;

	private BasicGLSLangProgram masterProgram;
	private int w;

	private int h;

	private Object gl;
	private Object glu;

	protected TaskQueue runQueue = new TaskQueue();

	protected ComponentContainer root = new ComponentContainer(null);

	protected WindowSpaceBox windowSpaceHelper = new WindowSpaceBox(this);

	boolean alwaysVisible = true;

	TaskQueue eventProcessingTaskQueue;

	boolean isOnGraphicsWindow = false;

	public boolean hasReset = false;

	Vector4 resetTo = new Vector4();

	boolean fadingIn = false;

	TaskQueue preQueue = new TaskQueue();

	TaskQueue postQueue = new TaskQueue();

	/**
	 * x translation of window
	 */
	float tx = -2;

	/**
	 * y translation of window
	 */
	float ty = -50 + 27;

	/**
	 * scale translation of window (right now we assume a uniform scale sx
	 * == sy)
	 */
	float sx = 1;

	/**
	 * scale translation of window (right now we assume a uniform scale sx
	 * == sy)
	 */
	float sy = 1;

	float near = 50;

	float far = 5000;

	// Vector4 background = new Vector4(0.67f, 0.67f, 0.67f, 0);

	Vector4 background = new Vector4(SystemProperties.getDoubleProperty("canvasBackground.r", (Platform.isMac() ? 200 : 220) / 255f), SystemProperties.getDoubleProperty("canvasBackground.g", (Platform.isMac() ? 200 : 220) / 255f), SystemProperties.getDoubleProperty("canvasBackground.b", (Platform.isMac() ? 200 : 220) / 255f), 0);

	boolean disableRepaint = false;
	boolean continuousRepaint = false;
	int continuousRepaintCount = 1;
	int continuousRepaintStrobe = 1;

	BasicSceneList sceneList = new BasicSceneList();

	int frameNumber = 0;

	Vector4 fragmentMul = new Vector4(1, 1, 1, 1);

	// Vector4 background = new Vector4(1f,1f,1f,0);

	Vector4 fragmentAdd = new Vector4(0, 0, 0, 0);

	Vector2 currentMouse = null;
	private boolean wasTransparent;
	protected boolean onlyRoot;
	private Long nsContext;
	private Rectangle oldbounds;
	private ComponentWindowStatusbar componentWindowStatusbar;
	private ComponentWindowDropShadow componentWindowDropShadow;

	private ResourceMonitor resourceMonitor;

	private float canvasFps;

	public Composite rightComp;

	public ToolBar toolbar;

	public Composite leftComp1;

	public SashForm leftComp2;

	private SashForm rsplit;

	private SashForm hsplit;

	static public GLComponentWindow lastCreatedWindow;

	iCanvasInterface canvasInterface;

	private BetterSash hBetterSash;

	private BetterSash rBetterSash;

	// public ToolBar toolbar;

	static public boolean doMultisampling = SystemProperties.getIntProperty("needsFSAA", 0) == 1;

	public GLComponentWindow(String name, TaskQueue q) {

		lastCreatedWindow = this;

		this.name = name;
		this.eventProcessingTaskQueue = q;
		context.begin(name);

		frame = new Shell(Launcher.display, SWT.SHELL_TRIM | (Platform.getOS() == OS.mac ? (0) : 0));

		frame.setText("Field");

		// toolbar = frame.getToolBar();

		// new ToolItem(toolbar, SWT.SEPARATOR_FILL);

		if (defaultRect.width == 0) {
			Rectangle b = Launcher.display.getBounds();
			defaultRect.x = 50;
			defaultRect.y = 50;
			defaultRect.width = Math.min(2000, b.width - 200);
			defaultRect.height = Math.min(1600, b.height - 200);
		}

		frame.setBounds(defaultRect);
		frame.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent e) {
				Rectangle b = frame.getBounds();
				defaultRect.x = b.x;
				defaultRect.y = b.y;
				defaultRect.width = b.width;
				defaultRect.height = b.height;

				;//System.out.println(" shell glcomponentwindow moved <" + b + ">");

			}

			@Override
			public void controlMoved(ControlEvent e) {
				Rectangle b = frame.getBounds();
				defaultRect.x = b.x;
				defaultRect.y = b.y;
				defaultRect.width = b.width;
				defaultRect.height = b.height;

				;//System.out.println(" shell glcomponentwindow moved <" + b + ">");

			}
		});

		// GLCapabilities cap = new GLCapabilities();
		// GLProfile.get(GLProfile.GL2);
		// ;//System.out.println(" cap is <" + cap + ">");

		frame.setSize(defaultRect.width, defaultRect.height);

		rsplit = new SashForm(frame, SWT.HORIZONTAL);
		rsplit.setSashWidth(10);
		rsplit.setLayout(new FillLayout());
		rsplit.setBackground(ToolBarFolder.sashBackground);

		final int SASH_LIMIT = 200;

		leftComp1 = new SashForm(rsplit, SWT.VERTICAL) {
			public void setWeights(int[] weights) {
				if (weights.length == 3) {

					float t = weights[0] + weights[1];

					float h = leftComp1.getSize().y;

					int margin = Platform.isMac() ? 50 : 58;

					float z = margin * t / (h - margin);
					
					;//System.out.println(" setting weight to be <" + z + "> <" + weights[0] + " " + weights[1] + ">");

					float norm = 1000f / weights[0];

					weights[2] = (int) (z * norm);
					weights[1] = (int) (weights[1] * norm);
					weights[0] = (int) ((int) weights[0] * norm);

					super.setWeights(weights);

				} else
					super.setWeights(weights);
			};
		};
		GridLayout gridlayout = new GridLayout();
		gridlayout.marginWidth = 0;
		gridlayout.marginHeight = 0;
		gridlayout.verticalSpacing = 0;
		leftComp1.setLayout(gridlayout);
		((SashForm) leftComp1).setSashWidth(10);
		leftComp1.setSize(defaultRect.width / 5, defaultRect.height);
		leftComp1.setBackground(ToolBarFolder.sashBackground);

		frame.setBackground(new Color(Launcher.display, 200, 200, 200));

		hsplit = new SashForm(rsplit, SWT.HORIZONTAL);
		hsplit.setSashWidth(10);
		hsplit.setLayout(new FillLayout());

		// SashForm s = ((SashForm)
		// GLComponentWindow.lastCreatedWindow.leftComp1);

		rsplit.setWeights(new int[] { 1, 6 });

		GLData data = new GLData();
		data.doubleBuffer = true;
		data.depthSize = 24;
		// data.stencilSize = 8;

		if (doMultisampling || Platform.isLinux()) {
			data.samples = 2;
//			data.sampleBuffers = 2;
		}
		// data.stereo = fieldStereo;

		// GLDrawableFactory factory =
		// GLDrawab.resizeleFactory.getFactory(GLProfile.getDefault());

		if (Platform.isMac()) {

			// canvas = new GLCanvas_field(hsplit,
			// SWT.NO_BACKGROUND, data);
			// canvasInterface = new MacOSXCanvasInterface(canvas);

			// Composite parent, int style, GLData data
			try {
				canvas = (Canvas) this.getClass().getClassLoader().loadClass("field.core.windowing.GLCanvas_field").getConstructor(Composite.class, Integer.TYPE, GLData.class).newInstance(hsplit, SWT.NO_BACKGROUND, data);
				canvasInterface = (iCanvasInterface) this.getClass().getClassLoader().loadClass("field.core.windowing.MacOSXCanvasInterface").getConstructor(Canvas.class).newInstance(canvas);
			} catch (Throwable t) {
				t.printStackTrace();
				throw new IllegalStateException("gl canvas failed to init. This is fatal");
			}

		} else {
			canvas = new GLCanvas(hsplit, SWT.NO_BACKGROUND, data);
			canvasInterface = new LinuxCanvasInterface(canvas);
		}
		canvas.setBackground(Launcher.display.getSystemColor(SWT.COLOR_BLACK));
		canvasInterface.setCurrent();

		canvas.setSize(defaultRect.width / 2, defaultRect.height);
		try {
			GLContext.useContext(canvas, CoreHelpers.isCore);

		} catch (LWJGLException e1) {
			e1.printStackTrace();
		}

		// System.err.println(" is current ? "+canvas.isCurrent());
		// glcontext = factory.createExternalGLContext();

		// canvas = new Canvas(frame, SWT.None);

		frame.addListener(SWT.Resize, new Listener() {

			@Override
			public void handleEvent(Event event) {
				rsplit.setSize(frame.getSize());
				// comp.setSize(frame.getSize());
				// canvas.setSize(frame.getSize());

				canvasInterface.setCurrent();
				try {
					GLContext.useContext(canvas);
				} catch (LWJGLException e) {
					e.printStackTrace();
				}

				Rectangle bounds = canvas.getBounds();
				GLComponentWindow.this.reshape(bounds.x, bounds.y, bounds.width, bounds.height);
				frame.redraw();
			}
		});

		canvas.addListener(SWT.Resize, new Listener() {

			@Override
			public void handleEvent(Event event) {
				// hsplit.setSize(frame.getSize());
				canvasInterface.setCurrent();
				try {
					GLContext.useContext(canvas);
				} catch (LWJGLException e) {
					e.printStackTrace();
				}

				Rectangle bounds = canvas.getBounds();
				GLComponentWindow.this.reshape(bounds.x, bounds.y, bounds.width, bounds.height);
				frame.redraw();
			}
		});

		canvas.addTouchListener(new TouchListener() {

			@Override
			public void touch(TouchEvent e) {
				;//System.out.println(" touch event :" + e);

			}
		});

		canvas.addGestureListener(new GestureListener() {

			boolean begin = false;

			float oscale = 1;
			float ox, oy;

			float lastMag = 1;

			@Override
			public void gesture(GestureEvent e) {
				;//System.out.println(" gesture event :" + e + " " + e.magnification);

				if (e.detail == SWT.GESTURE_BEGIN) {
					begin = true;
				}

				if (e.detail == SWT.GESTURE_MAGNIFY && e.magnification > 0.1) {

					if (begin) {
						lastMag = 1.0f;
						ox = tx;
						oy = ty;
						oscale = sx;
						begin = false;
					}

					float sy = lastMag / (float) e.magnification;
					lastMag = (float) e.magnification;

					;//System.out.println(" setting mag momentum to be <" + sy + ">");

					magnificationMomentum = sy;
					magnificationCenter.x = e.x;
					magnificationCenter.y = e.y;

					requestRepaint();
				}
			}
		});

		if (Platform.isLinux())
			canvas.addListener(SWT.Paint, new Listener() {

				@Override
				public void handleEvent(Event event) {
					;//System.out.println(" paint listener called ");
					requestRepaint();
//					display();
				}
			});

		// canvas.setSize(defaultRect.width, defaultRect.height);

		init();
		canvasInterface.swapBuffers();

		canvas.addListener(SWT.KeyDown, this);
		canvas.addListener(SWT.KeyUp, this);
		canvas.addListener(SWT.MouseDown, this);
		canvas.addListener(SWT.MouseUp, this);
		canvas.addListener(SWT.MouseMove, this);
		canvas.addListener(SWT.MouseHover, this);
		canvas.addListener(SWT.MouseVerticalWheel, this);
		canvas.addListener(SWT.MouseHorizontalWheel, this);
		canvas.addListener(SWT.MouseDoubleClick, this);

		textSystem = new TextSystem();
		TextSystem.textSystem = textSystem;

		alwaysVisible = true;

		resourceMonitor = new ResourceMonitor() {
			@Override
			public String toString() {
				return "resource monitor for gl component window";
			}
		};

		wasTransparent = isTransparent;

		new Thread(new Runnable() {

			public void run() {
				while (true) {
					long in = System.currentTimeMillis();
					int frameIn = frameNumber;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					int frameOut = frameNumber;
					long out = System.currentTimeMillis();
					float fps = 1000 * (frameOut - frameIn) / (float) (out - in);
					if (fps > 0)
						System.err.println("canvas at fps:" + fps);
					canvasFps = fps;
				}
			}

		}).start();

		componentWindowDropShadow = new ComponentWindowDropShadow(this, this.textSystem);

		componentWindowStatusbar = new ComponentWindowStatusbar(this, this.textSystem) {
			@Override
			protected String getText() {

				// TODO swt;
				// return "";

				String t = "<html><font face='" + Constants.defaultFont + "' size=-2><p align='center'>";

				boolean caps = Platform.getOS() == OS.mac ? (Toolkit.getDefaultToolkit().getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK)) : false;

				t += " Layout constraints: " + (caps ? "<b>on</b>" : "<b>off</b> <i>(toggle with caps lock)</i>");

				if (present) {
					t += " <b>Presentation</b> mode: " + (draft ? "<b>drafting</b> <i>(toggle with F2)</i>" : "<b>preview</b> <i>(toggle with F2)</i>");
				} else {
					t += " Rendering mode: " + (draft ? "<b>drafting</b> <i>(toggle with F2)</i>" : "<b>preview</b> <i>(toggle with F2)</i>");
				}

				t += " Zoom: <b>" + BaseMath.toDP((1 / sx) * 100.0, 1) + "%</b> <i>(shift middle mouse, F1 resets)</i>";

				t += " Focus: <b>" + (root.focusLocked ? "locked</b> <i>(press ` to unlock)</i>" : "free</b> <i>(press ` to select)</i>");

				// if (keysDown.size() > 0) {
				// String codes = "";
				// for (Integer i : keysDown) {
				// codes += (char) i.intValue() + " ";
				// }
				//
				// t += " Extra keys: <b>" + codes + "</b>";
				// }

				Collection<String> v = extraTextStatusbarDescriptions.values();

				for (String vv : v)
					t += " " + vv;

				if (disableRepaint || disableRepaintNext) {
					t += " <font face='" + Constants.defaultFont + "' size=-2 color='#660000'>Repainting is disabled</font>";
					// ((JFrame)
					// frame).getRootPane().putClientProperty("Window.alpha",
					// disableRepaint || disableRepaintNext
					// ? 0.75 : 1f);
					backgroundHeavy = true;
				} else {
					// ((JFrame)
					// frame).getRootPane().putClientProperty("Window.alpha",
					// disableRepaint || disableRepaintNext
					// ? 0.75 : 1f);
					backgroundHeavy = false;
				}

				if (continuousRepaint) {
					t += " <font face='" + Constants.defaultFont + "' size=-2 color='#660000'><b>Canvas is animating</b></font>";
				}

				return t;
			}
		};

		rightComp = new Composite(hsplit, SWT.NONE);
		gridlayout = new GridLayout();
		gridlayout.marginWidth = 0;
		gridlayout.marginHeight = 0;
		gridlayout.verticalSpacing = 0;
		rightComp.setLayout(gridlayout);
		rightComp.setSize(defaultRect.width / 2, defaultRect.height);

		rightComp.setBackground(ToolBarFolder.sashBackground);
		hsplit.setBackground(ToolBarFolder.sashBackground);

		// TODO swt
		// new CanvasDragAndDropSupport(this);

		frame.open();

		// TODO swt mac native

		try {
			// Object window =
			// ReflectionTools.illegalGetObject(frame, "window");
			// Method setLevel =
			// ReflectionTools.findFirstMethodCalled(window.getClass(),
			// "setLevel");
			// setLevel.invoke(window, 24);
			// ReflectionTools.findFirstMethodCalled(ReflectionTools.illegalGetObject(frame,
			// "window").getClass(),"setHidesOnDeactivate").invoke(ReflectionTools.illegalGetObject(frame,
			// "window"), true);

		} catch (Exception e) {
			// e.printStackTrace();
		}

		hsplit.setWeights(new int[] { 2, 1 });
		// frame.layout();

		Control sash = hsplit.getChildren()[hsplit.getChildren().length - 1];
		;//System.out.println(" SASH is :" + sash);

		hBetterSash = new BetterSash(hsplit, true);
		rBetterSash = new BetterSash(rsplit, false);

	}

	float magnificationMomentum = 1.0f;
	Vector2 magnificationCenter = new Vector2();

	public GLComponentWindow(TaskQueue queue) {
		this("uniq++" + (uniq++), queue);
	}

	// public org.eclipse.swt.openGL11.GLCanvas getCanvas() {
	// return canvas;
	// }

	public Canvas getCanvas() {
		return canvas;
	}

	int yoffset = Platform.getOS() == OS.mac ? -20 : 0;

	public Vector2 getCurrentMouseInWindowCoordinates() {

		Vector2 currentMouse = getCurrentMousePosition();

		return new Vector2((currentMouse.x) * sx + tx, (Platform.isLinux() ? -53 : 0) + yoffset + (currentMouse.y) * sy + ty);
	}

	public Vector2 getCurrentMousePosition() {
		if (currentMouse == null) {
			Point loc = Launcher.display.getCursorLocation();
			loc = Launcher.display.map(null, getCanvas(), loc);
			currentMouse = new Vector2(loc.x, loc.y);
		}
		return currentMouse;
	}

	public Shell getFrame() {
		return frame;
	}

	public String getName() {
		return name;
	}

	public TaskQueue getPostQueue() {
		return postQueue;
	}

	public TaskQueue getPreQueue() {
		return preQueue;
	}

	public iRegistersUpdateable getQueueFor(Method m) {
		return eventProcessingTaskQueue;
	}

	public ComponentContainer getRoot() {
		return root;
	}

	public TaskQueue getRunQueue() {
		return runQueue;
	}

	public BasicSceneList getSceneList() {
		return sceneList;
	}

	public float getXScale() {
		return sx;
	}

	public float getXTranslation() {
		return tx;
	}

	public float getYScale() {
		return sy;
	}

	public float getYTranslation() {
		return ty;
	}

	@InQueue
	public void keyPressed(Event arg0) {
		currentWindow = this;
		root.keyPressed(root, arg0);
	}

	@InQueue
	public void keyReleased(Event arg0) {
		currentWindow = this;
		root.keyReleased(root, arg0);
	}

	@InQueue
	public void keyTyped(Event arg0) {
		currentWindow = this;
		root.keyTyped(root, arg0);
	}

	Vector2 eyeOffset = new Vector2();
	Vector2 eyeTargetOffset = new Vector2();

	// public void makeOverlay() {
	// if (SystemProperties.getIntProperty("noOverlay", 0) == 1)
	// return;
	//
	// if (overlay != null)
	// return;
	//
	// overlay = new OverlayCanvas(this, this.getFrame(), this.canvas,
	// false) {
	// @Override
	// protected void preupdate() {
	// GL gl = BasicContextManager.getGl();
	// GLU glu = BasicContextManager.getGlu();
	// GL11.glViewport(0, 0, w, h);
	// GL11.glMatrixMode(GL11.GL_PROJECTION);
	// GL11.glLoadIdentity();
	//
	// if (pure2d) {
	// glu.gluOrtho2D(0 + tx, w * sx + tx, h * sy + ty, ty);
	// } else {
	// GL11.glFrustum(tx, w * sx + tx, h * sy + ty, ty, near, far);
	// }
	//
	// GL11.glMatrixMode(GL11.GL_MODELVIEW);
	//
	// GL11.glLoadIdentity();
	// if (!pure2d) {
	// glu.gluLookAt(eyeOffset.x, eyeOffset.y, near, eyeTargetOffset.x,
	// eyeTargetOffset.y, 0, 0, 1, 0);
	// }
	//
	// GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
	//
	// // GL11.glEnable(GL11.GL_LINE_SMOOTH);
	// // GL11.glHint(GL11.GL_LINE_SMOOTH_HINT,
	// // GL11.GL_NICEST);
	// GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
	// GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
	// GL11.glEnable(GL11.GL_POINT_SMOOTH);
	// GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);
	// GL11.glEnable(GL11.GL_BLEND);
	// GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	//
	// super.preupdate();
	// }
	// };
	//
	// overlayAnimationManager = new OverlayAnimationManager(overlay);
	//
	// }

	@InQueue
	public void mouseClicked(Event arg0) {
		transformMouseEvent(arg0);
		currentWindow = this;
		// if (arg0.getComponent() == canvas)
		// currentMouse.set(arg0.getX(), arg0.getY());

		root.mouseClicked(root, arg0);
	}

	@InQueue
	public void mouseDragged(Event arg0) {
		transformMouseEvent(arg0);
		currentWindow = this;
		root.mouseDragged(root, arg0);
	}

	@InQueue
	public void mouseEntered(Event arg0) {
		transformMouseEvent(arg0);
		currentWindow = this;
		root.mouseEntered(root, arg0);
	}

	@InQueue
	public void mouseExited(Event arg0) {
		transformMouseEvent(arg0);
		currentWindow = this;
		root.mouseExited(root, arg0);
	}

	@InQueue
	public void mouseMoved(Event arg0) {
		transformMouseEvent(arg0);
		currentWindow = this;
		root.mouseMoved(root, arg0);
	}

	@InQueue
	public void mousePressed(Event arg0) {

		enableRepaint();

		transformMouseEvent(arg0);
		currentWindow = this;
		root.mousePressed(root, arg0);
	}

	private void enableRepaint() {
		if (disableRepaint) {
			disableRepaint = false;
			disableRepaintNext = false;
			requestRepaint();
		}
	}

	public void disableRepaintNow() {
		if (!disableRepaint) {
			disableRepaint = false;
			disableRepaintNext = true;
			requestRepaint();
		}
	}

	public void toggleContinuousRepaintNow() {
		continuousRepaint = !continuousRepaint;
		requestRepaint();
	}

	@InQueue
	public void mouseReleased(Event arg0) {

		transformMouseEvent(arg0);
		currentWindow = this;
		root.mouseReleased(root, arg0);
	}

	public void requestRepaint() {
		root.setNeedsRedisplay(true);
	}

	public void resetViewParameters() {
		if (!hasReset) {
			hasReset = true;
			resetTo.x = sx;
			resetTo.y = sy;
			resetTo.z = tx;
			resetTo.w = ty;

			setXScale(1);
			setYScale(1);
			setXTranslation(-2);
			setYTranslation(-50 + 27);

			// sx = 1;
			// sy = 1;
			// tx = -2;
			// ty = -50 + 27;
		} else {
			hasReset = false;

			setXScale(resetTo.x);
			setYScale(resetTo.y);
			setXTranslation(resetTo.z);
			setYTranslation(resetTo.w);

			sx = resetTo.x;
			sy = resetTo.y;
			tx = resetTo.z;
			ty = resetTo.w;
		}
	}

	public void saveNUpWindowAsPNG(final String filename, int width, int height) {

		new NUpPNGSaver(filename, getRunQueue(), width, height);

		root.setNeedsRedisplay(true);
	}

	//
	// public void saveNUpWindowAsPNG(final String filename, int width, int
	// height, int maxW, int maxH) {
	//
	// new NUpPNGSaver(filename, getRunQueue(), width, height, maxW, maxH);
	//
	// root.setNeedsRedisplay(true);
	// }
	//
	// public void saveWindowAsPNG(final String filename) {
	// getRunQueue().new Task() {
	// @Override
	// public void run() {
	// GL gl = BasicContextManager.getGl();
	//
	// int width = frame.getWidth();
	// int height = frame.getHeight();
	// ByteBuffer storage = ByteBuffer.allocateDirect(width * height * 4);
	//
	// GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA,
	// GL11.GL_UNSIGNED_BYTE,
	// storage);
	// BufferedImage bi = new BufferedImage(width, height,
	// BufferedImage.TYPE_INT_RGB);
	//
	// WritableRaster tile = bi.getWritableTile(0, 0);
	// DataBuffer buffer = tile.getDataBuffer();
	//
	// IntBuffer storagei = storage.asIntBuffer();
	// for (int y = 0; y < height; y++) {
	// for (int x = 0; x < width; x++) {
	// int r = BaseMath.intify(storage.get());
	// int g = BaseMath.intify(storage.get());
	// int b = BaseMath.intify(storage.get());
	// int a = BaseMath.intify(storage.get());
	//
	// int d = (g << 8) | (r << 16) | (b);
	// buffer.setElem((height - 1 - y) * width + x, d);
	// }
	// }
	//
	// FileOutputStream fos;
	// RenderedOp op = JAI.create("filestore", bi, filename, "PNG");
	//
	// }
	// };
	//
	// root.setNeedsRedisplay(true);
	// }
	//
	// public void saveWindowAsPNGCropped(final String filename, final int
	// ox,
	// final int oy, final int width, final int height) {
	// getRunQueue().new Task() {
	// @Override
	// public void run() {
	// GL gl = BasicContextManager.getGl();
	//
	// ByteBuffer storage = ByteBuffer.allocateDirect(width * height * 4);
	//
	// GL11.glReadPixels(ox, oy, width, height, GL11.GL_RGBA,
	// GL11.GL_UNSIGNED_BYTE,
	// storage);
	// BufferedImage bi = new BufferedImage(width, height,
	// BufferedImage.TYPE_INT_RGB);
	//
	// WritableRaster tile = bi.getWritableTile(0, 0);
	// DataBuffer buffer = tile.getDataBuffer();
	//
	// IntBuffer storagei = storage.asIntBuffer();
	// for (int y = 0; y < height; y++) {
	// for (int x = 0; x < width; x++) {
	// int r = BaseMath.intify(storage.get());
	// int g = BaseMath.intify(storage.get());
	// int b = BaseMath.intify(storage.get());
	// int a = BaseMath.intify(storage.get());
	//
	// int d = (g << 8) | (r << 16) | (b);
	// buffer.setElem((height - 1 - y) * width + x, d);
	// }
	// }
	//
	// FileOutputStream fos;
	// RenderedOp op = JAI.create("filestore", bi, filename, "PNG");
	//
	// }
	// };
	//
	// root.setNeedsRedisplay(true);
	// }

	public void setEventProcessingTaskQueue(TaskQueue eventProcessingTaskQueue) {
		this.eventProcessingTaskQueue = eventProcessingTaskQueue;
	}

	public void setXScale(float f) {
		sx = f;
		clampViewParameters();

	}

	private void clampViewParameters() {
		if (sx < 0.2)
			sx = 0.2f;
		if (sy < 0.2)
			sy = 0.2f;
		if (sx > 10)
			sx = 10f;
		if (sy > 10)
			sy = 10f;

	}

	public float setXTranslation(float t) {

		if (tx == t)
			return t;

		windowSpaceHelper.freeze();
		tx = t;
		windowSpaceHelper.thaw();

		return tx;
	}

	public void setYScale(float f) {
		sy = f;
		clampViewParameters();
	}

	public float setYTranslation(float t) {
		if (ty == t)
			return t;

		windowSpaceHelper.freeze();
		ty = t;
		windowSpaceHelper.thaw();
		return ty;
	}

	public void transformMouseEvent(Event arg0) {

		Vector2 at = floatResMouseEvents.get(arg0);
		if (at == null) {
			at = new Vector2();
			at.x = arg0.x;
			at.y = arg0.y;
		}

		at.y -= yoffset;

		ReflectionTools.illegalSetObject(arg0, "x", Math.round(at.x * sx + tx));
		ReflectionTools.illegalSetObject(arg0, "y", Math.round(at.y * sy + ty));

		Vector2 a = new Vector2(at.x * sx + tx, at.y * sy + ty);

		floatResMouseEvents.put(arg0, a);
	}

	public void transformDrawingToWindow(Vector2 v) {
		v.x = (v.x - tx) / sx;
		v.y = (v.y - ty) / sy;
	}

	public void transformWindowToDrawing(Vector2 v) {
		v.x = (v.x * sx + tx);
		v.y = (v.y * sy + ty);
	}

	FloatBuffer model = ByteBuffer.allocateDirect(4 * 16).order(ByteOrder.nativeOrder()).asFloatBuffer();
	FloatBuffer object1 = ByteBuffer.allocateDirect(4 * 16).order(ByteOrder.nativeOrder()).asFloatBuffer();
	FloatBuffer object2 = ByteBuffer.allocateDirect(4 * 16).order(ByteOrder.nativeOrder()).asFloatBuffer();
	FloatBuffer projection = ByteBuffer.allocateDirect(4 * 16).order(ByteOrder.nativeOrder()).asFloatBuffer();
	IntBuffer viewport = ByteBuffer.allocateDirect(4 * 16).order(ByteOrder.nativeOrder()).asIntBuffer();

	public Vector2 transformWindowToCanvas(Vector2 v) {
		assert currentWindow == this;

		assert GL11.glGetError() == 0;

		model.rewind();
		CoreHelpers.getModelView(model);
		// glGetFloat(GL_MODELVIEW_MATRIX, model);
		model.rewind();

		projection.rewind();
		// glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
		CoreHelpers.getProjection(projection);
		projection.rewind();

		viewport.rewind();
		glGetInteger(GL11.GL_VIEWPORT, viewport);
		viewport.rewind();
		object1.rewind();

		// ;//System.out.println(" transform window to canvas is :"+printMat4(model)+" "+printMat4(projection)+" "+printMat4(viewport));

		GLU.gluUnProject(v.x * viewport.get(2), v.y * viewport.get(3), 0, model, projection, viewport, object1);
		// GLU.gluUnProject(v.x , v.y , 0, model, projection, viewport,
		// object1);

		// ;//System.out.println(" transform window to canvas <"+v+"> <"+object1.get(0)+"> <"+object1.get(1)+">");

		return new Vector2(object1.get(0), object1.get(1));
	}

	private String printMat4(IntBuffer viewport2) {
		String r = "\n";
		for (int i = 0; i < viewport2.capacity(); i++) {
			if (i % 4 == 0)
				r += "\n";
			r += " " + viewport2.get(i);
		}
		return r;
	}

	private String printMat4(FloatBuffer viewport2) {
		String r = "\n";
		for (int i = 0; i < viewport2.capacity(); i++) {
			if (i % 4 == 0)
				r += "\n";
			r += " " + viewport2.get(i);
		}
		return r;
	}

	public void untransformMouseEvent(Event arg0) {

		Vector2 at = floatResMouseEvents.get(arg0);
		if (at == null) {
			at = new Vector2();
			at.x = arg0.x;
			at.y = arg0.y;
		}
		ReflectionTools.illegalSetObject(arg0, "x", Math.round((at.x - tx) / sx));
		ReflectionTools.illegalSetObject(arg0, "y", Math.round((at.y - ty) / sy + yoffset));
		floatResMouseEvents.put(arg0, new Vector2((at.x - tx) / sx, (at.y - ty) / sy + yoffset));
	}

	int safety = 0;

	@DispatchOverTopology(topology = Cont.class)
	public void update() {

		// ;//System.out.println(" inside glcomponent window update <"+root.isNeedsRedisplay()+">");

		if (!disableRepaint && (root.isNeedsRedisplay() || (continuousRepaint && ((continuousRepaintCount++ % continuousRepaintStrobe) == 0)))) {
			frameNumber++;
			root.setNeedsRedisplay(false);

			canvasInterface.setCurrent();
			try {
				GLContext.useContext(canvas);
			} catch (LWJGLException e) {
				e.printStackTrace();
			}
			GLComponentWindow.this.display();

			canvasInterface.swapBuffers();
			// glcontext.release();
		}

	}

	boolean disableRepaintNext = false;

	@DispatchOverTopology(topology = Cont.class)
	protected void display() {

		// ThreadedLauncher.lock2.lock();
		try {

			if (disableRepaint)
				return;

			if (isOnGraphicsWindow && fragmentMul.w < 0.01f) {
				preQueue.update();
				postQueue.update();
				runQueue.update();
				return;
			}

			doMagnificationGesture();

			context.begin(name);

			BasicContextManager.setCurrentContextFor(this, null, null);
			currentWindow = this;

			resourceMonitor.performPass(null);
			textSystem.beginRender(gl, glu);

			TextSystem.textSystem = textSystem;

			initGL(true);

			masterProgram.performPass(null);
			if (rendererInfo == null) {
				rendererInfo = glGetString(GL_VENDOR).toLowerCase() + ":" + glGetString(GL11.GL_RENDERER);
			}

			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthFunc(GL11.GL_ALWAYS);
			GL11.glPolygonOffset(1, 1);
			GL11.glDisable(GL11.GL_CULL_FACE);

			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

			GL11.glEnable(GL13.GL_MULTISAMPLE);
			
			GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
			GL11.glEnable(GL11.GL_LINE_SMOOTH);

			GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
			if (!Platform.isLinux())
				GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
			else
				GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
				
			// iVisualElementOverrides.MakeDispatchProxy.
			// dispatchForwardCount
			// = 0;
			// iVisualElementOverrides.MakeDispatchProxy.
			// dispatchBackwardCount
			// = 0;

			preQueue.update();

			if (fieldStereo) {

				GL11.glEnable(GL11.GL_STEREO);
				GL11.glDrawBuffer(GL11.GL_BACK);
			}

			GL11.glViewport(0, 0, canvas.getSize().x, canvas.getSize().y);

			root.paint(root, new CoordinateFrame(), true);

			postQueue.update();
			textSystem.endRender();

			doRender();

			if (disableRepaintNext) {
				disableRepaint = true;
				disableRepaintNext = false;
			}
		} finally {
			// ThreadedLauncher.lock2.unlock();
		}
	}

	private void doMagnificationGesture() {

		float sy = magnificationMomentum;

		float alpha = 0.85f;
		magnificationMomentum = magnificationMomentum * alpha + (1 - alpha) * 1;

		float cx = magnificationCenter.x * sx + tx;
		float cy = (magnificationCenter.y - yoffset) * sx + ty;

		// this point shouldn't move regardless of sx changing

		// if (sy<1) sy = 0.95f;
		// if (sy>1) sy = 1.05f;

		float nsx = sx * sy;

		if (nsx < 0.3) {
			magnificationMomentum = 1;
			return;
		}
		if (nsx > 5) {
			magnificationMomentum = 1;
			return;
		}

		sx = nsx;
		GLComponentWindow.this.sy = nsx;

		float ncx = magnificationCenter.x * sx + tx;
		float ncy = (magnificationCenter.y - yoffset) * sx + ty;

		float dx = cx - ncx;
		float dy = cy - ncy;

		tx += dx;
		ty += dy;

		clampViewParameters();

		if (Math.abs(1 - magnificationMomentum) > 1e-2)
			requestRepaint();

	}

	@DispatchOverTopology(topology = Cont.class)
	protected void doRender() {
		initGL(false);
		sceneList.update();
		context.end(name);

		runQueue.update();
	}

	protected void init() {
		// cgContext = CgGL11.cgCreateContext();

		// masterProgram =
		// (BasicCGProgram.fromFile(
		// "content/shaders/glComponentWindow_master.cg"
		// ,
		// false));
		masterProgram = new BasicGLSLangProgram("content/shaders/glComponentVertex.glslang", "content/shaders/glComponentGLSLangFragment.glslang");
		masterProgram.new SetIntegerUniform("tex", 1);
		masterProgram.new SetIntegerUniform("texture2", 0);
		masterProgram.new SetUniform("mul", fragmentMul);
		masterProgram.new SetUniform("add", fragmentAdd);
	}

	protected void initGL(boolean clear) {
		GL11.glViewport(0, 0, w, h);

		CoreHelpers.glMatrixMode(GL11.GL_PROJECTION);
		CoreHelpers.glLoadIdentity();

		if (pure2d) {
			CoreHelpers.gluOrtho2D(0 + tx, w * sx + tx, h * sy + ty, ty);
		} else {
			GL11.glFrustum(tx, w * sx + tx, h * sy + ty, ty, near, far);
		}

		// MacOSXGLContext context = (MacOSXGLContext) ReflectionTools
		// .illegalGetObject(canvas, "context");
		// nsContext = (Long) ReflectionTools.illegalGetObject(context,
		// "nsContext");
		// if (wasTransparent) {
		// if (nsContext != 0) {
		// MiscNative.load();
		// new MiscNative().makeReallyTransparent_safe(nsContext);
		// }
		// }

		// glu.gluPerspective(45, 1, 0.1, 1000);
		CoreHelpers.glMatrixMode(GL11.GL_MODELVIEW);

		CoreHelpers.glLoadIdentity();
		if (!pure2d) {
			GLU.gluLookAt(eyeOffset.x, eyeOffset.y, near, eyeTargetOffset.x, eyeTargetOffset.y, 0, 0, 1, 0);
		}

		// glu.gluLookAt(0, 0, -140, 30, 30, 0, 0, 1,
		// 0);

		if (clear) {

			GL11.glClearColor(background.x, background.y, background.z, background.w);
			// GL11.glClearColor(1, 0, 0, 1);
			// GL11.glClearColor(1,0,0,1);
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
		}
		// GL11.glEnable(GL_MULTISAMPLE);

		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
		GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
		GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
		if (!CoreHelpers.isCore) {
			GL11.glEnable(GL11.GL_POINT_SMOOTH);
			GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);
		}
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

	}

	protected void reshape(int x, int y, int w, int h) {
		this.w = w;
		this.h = h;
		// GL gl = glcontext.getGL();
		// canvas.setCurrent();

		;//System.out.println(" -- reshape to <" + w + " " + h + ">");

		GL11.glViewport(0, 0, w, h);

		// GL11.glMatrixMode(GL11.GL_PROJECTION);
		// GL11.glLoadIdentity();
		// GL11.glOrtho(0, w, h, 0, -1, 1);
		// GL11.glMatrixMode(GL11.GL_MODELVIEW);
		root.requestRedisplay();
	}

	public double getHeight() {
		return h;
	}

	public double getWidth() {
		return w;
	}

	Set<Integer> handleEvent_mouseDown = new LinkedHashSet<Integer>();

	@Override
	public void handleEvent(Event event) {

		switch (event.type) {
		case SWT.KeyDown:
			keyPressed(event);
			break;
		case SWT.KeyUp:
			keyReleased(event);
			keyTyped(event);
			break;
		case SWT.MouseDown:
			currentMouse = new Vector2(event.x, event.y);

			mousePressed(event);
			if ((event.stateMask & SWT.BUTTON1) != 0)
				handleEvent_mouseDown.add(1);
			if ((event.stateMask & SWT.BUTTON2) != 0)
				handleEvent_mouseDown.add(2);
			if ((event.stateMask & SWT.BUTTON3) != 0)
				handleEvent_mouseDown.add(3);
			handleEvent_mouseDown.add(event.button);
			break;
		case SWT.MouseUp:
			mouseReleased(event);
			handleEvent_mouseDown.clear();
			break;
		case SWT.MouseMove:

			currentMouse = new Vector2(event.x, event.y);

			if (handleEvent_mouseDown.size() == 0) {
				mouseMoved(event);
			} else {
				event.button = handleEvent_mouseDown.contains(1) ? 1 : (handleEvent_mouseDown.contains(2) ? 2 : (handleEvent_mouseDown.contains(3) ? 3 : 0));
				mouseDragged(event);
			}

			// if ((event.stateMask & SWT.BUTTON1) == 0
			// && (event.stateMask & SWT.BUTTON2) == 0
			// && (event.stateMask & SWT.BUTTON3) == 0)
			// mouseMoved(event);
			// else {
			// ;//System.out.println(" dragged at <"+event.time+">");
			// // !
			// if ((event.stateMask & SWT.BUTTON1) != 0)
			// event.button = 1;
			// if ((event.stateMask & SWT.BUTTON2) != 0)
			// event.button = 2;
			// if ((event.stateMask & SWT.BUTTON3) != 0)
			// event.button = 3;
			// mouseDragged(event);
			// }
			break;
		case SWT.MouseVerticalWheel:
			setYTranslation(ty - event.count * sy * 6);
			requestRepaint();
			break;
		case SWT.MouseHorizontalWheel:
			setXTranslation(tx - event.count * sx * 6);
			requestRepaint();
			break;
		case SWT.MouseDoubleClick:
			event.count = 2;
			mouseClicked(event);

		default:
			break;
		}

	}

	public Set<Integer> getMouseButtonsDown() {
		return handleEvent_mouseDown;
	}

	public Object getGL() {
		return new Object();
	}

	public void togglePanelVisiblity() {

		;//System.out.println(" TOGGLE !");

		hBetterSash.toggle();
		rBetterSash.toggle();
	}

}
