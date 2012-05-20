package field.core.windowing.components;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElementOverrides.Ref;
import field.core.ui.SmallMenu;
import field.core.ui.SmallMenu.BetterPopup;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.components.DraggableComponent.Resize;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.graphics.core.PointList;
import field.graphics.core.TextSystem.iLabel;
import field.graphics.dynamic.DynamicPointlist;
import field.graphics.dynamic.iDynamicMesh;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Vector2;
import field.math.linalg.iCoordinateFrame;
import field.namespace.generic.ReflectionTools;

public class RootComponent implements iComponent {

	public interface iMousePeer {
		public void keyPressed(ComponentContainer inside, Event arg0);

		public void keyReleased(ComponentContainer inside, Event arg0);

		public void keyTyped(ComponentContainer inside, Event arg0);

		public void mouseClicked(ComponentContainer inside, Event arg0);

		public void mouseDragged(ComponentContainer inside, Event arg0);

		public void mouseEntered(ComponentContainer inside, Event arg0);

		public void mouseExited(ComponentContainer inside, Event arg0);

		public void mouseMoved(ComponentContainer inside, Event arg0);

		public void mousePressed(ComponentContainer inside, Event arg0);

		public void mouseReleased(ComponentContainer inside, Event arg0);
	}

	public interface iPaintPeer {
		public void paint(RootComponent inside);
	}

	MouseDragger dragger = new MouseDragger();

	public class MouseDragger {

		private float lastX;

		private float lastY;

		private float downX;

		private float downY;

		Set<Resize> currentResize = new HashSet<Resize>();

		float zone = 15;

		public MouseDragger() {
		}

		public void mouseClicked(Event arg0) {
		}

		public void mouseDragged(Event arg0) {

			if ((arg0.stateMask & SWT.ALT) != 0)
				return;

			float lx = RootComponent.this.getX();
			float ly = RootComponent.this.getY();

			// GLComponentWindow.getCurrentWindow(RootComponent.this).untransformMouseEvent(arg0);

			Vector2 mv = GLComponentWindow.mousePositionForEvent(arg0);

			float dx = (mv.x) - (lastX - 0 * lx);
			float dy = (mv.y) - (lastY - 0 * ly);

			boolean handled = false;
			for (SelectionGroup<iComponent> s : selectionGroups) {
				for (iComponent d : s.getSelection()) {
					if (d != RootComponent.this) {
						handled = true;
						// d
						// .
						// handleResize
						// (
						// currentResize
						// ,
						// dx
						// ,
						// dy
						// )
						// ;
					}
				}
			}

			if (handled && arg0.button != 2)
				return;

			// GLComponentWindow.getCurrentWindow(RootComponent.this).transformMouseEvent(arg0);

			RootComponent.this.handleResize(currentResize, dx, dy, arg0.x, arg0.y);

			// GLComponentWindow.getCurrentWindow(RootComponent.this).untransformMouseEvent(arg0);
			// need to rexpress lx in light
			// of the potentially new
			// viewport scale
			mv = GLComponentWindow.mousePositionForEvent(arg0);

			float lx2 = RootComponent.this.getX();
			float ly2 = RootComponent.this.getY();

			lastX = (mv.x) - (lx2 - lx);
			lastY = (mv.y) - (ly2 - ly);

			GLComponentWindow.getCurrentWindow(RootComponent.this).transformMouseEvent(arg0);

		}

		public void mouseEntered(Event arg0) {
		}

		public void mouseExited(Event arg0) {
			// GLComponentWindow.
			// getCurrentWindow
			// (RootComponent
			// .this).getCanvas
			// ().setCursor(Cursor
			// .getDefaultCursor());
		}

		public void mouseMoved(Event arg0) {

			if (true)
				return;

			Set<Resize> currentResize = new HashSet<Resize>();

			currentResize.clear();
			if (arg0.x < zone)
				currentResize.add(Resize.left);
			if (arg0.x > RootComponent.this.getWidth() - zone)
				currentResize.add(Resize.right);
			if (arg0.y < zone)
				currentResize.add(Resize.up);
			if (arg0.y > RootComponent.this.getHeight() - zone - 50)
				currentResize.add(Resize.down);
			if (currentResize.size() == 0)
				currentResize.add(Resize.translate);

			if (inside != null) {
				// TODO swt cursor
				// if (currentResize.contains(Resize.left) &&
				// currentResize.contains(Resize.down))
				// GLComponentWindow.getCurrentWindow(RootComponent.this).getCanvas().setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
				// else if (currentResize.contains(Resize.left)
				// &&
				// currentResize.contains(Resize.up))
				// GLComponentWindow.getCurrentWindow(RootComponent.this).getCanvas().setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
				// else if (currentResize.contains(Resize.right)
				// &&
				// currentResize.contains(Resize.up))
				// GLComponentWindow.getCurrentWindow(RootComponent.this).getCanvas().setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
				// else if (currentResize.contains(Resize.right)
				// &&
				// currentResize.contains(Resize.down))
				// GLComponentWindow.getCurrentWindow(RootComponent.this).getCanvas().setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
				// else if
				// (currentResize.contains(Resize.right))
				// GLComponentWindow.getCurrentWindow(RootComponent.this).getCanvas().setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
				// else if (currentResize.contains(Resize.left))
				// GLComponentWindow.getCurrentWindow(RootComponent.this).getCanvas().setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
				// else if (currentResize.contains(Resize.up))
				// GLComponentWindow.getCurrentWindow(RootComponent.this).getCanvas().setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
				// else if (currentResize.contains(Resize.down))
				// GLComponentWindow.getCurrentWindow(RootComponent.this).getCanvas().setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
				// else if
				// (currentResize.contains(Resize.translate))
				// GLComponentWindow.getCurrentWindow(RootComponent.this).getCanvas().setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			}
		}

		public void mousePressed(Event arg0) {

			// if ((arg0.getModifiers() &
			// Event.SHIFT_MASK) ==
			// 0) for
			// (SelectionGroup<Draggable> d
			// :
			// selectionGroups)
			// d.deselectAll();
			// for
			// (SelectionGroup<Draggable> d
			// : selectionGroups)
			// d.addToSelection(
			// WindowResizingComponent
			// .this);
			//
			// setSelected(true);

			currentResize.clear();
			if (arg0.button == 2) {
				if ((arg0.stateMask & SWT.SHIFT) == 0)
					currentResize.add(Resize.innerTranslate);
				else
					currentResize.add(Resize.innerScale);
			} else {

				if (arg0.x < zone)
					currentResize.add(Resize.left);
				if (arg0.x > RootComponent.this.getWidth() - zone)
					currentResize.add(Resize.right);
				if (arg0.y < zone)
					currentResize.add(Resize.up);
				if (arg0.y > RootComponent.this.getHeight() - zone - 100)
					currentResize.add(Resize.down);
				if (currentResize.size() == 0)
					currentResize.add(Resize.translate);
			}
			float lx = RootComponent.this.getX();
			float ly = RootComponent.this.getY();

			// GLComponentWindow.getCurrentWindow(RootComponent.this).untransformMouseEvent(arg0);

			float xxx = GLComponentWindow.mousePositionForEvent(arg0).x;
			float yyy = GLComponentWindow.mousePositionForEvent(arg0).y;

			downX = lastX = xxx + 0 * lx;
			downY = lastY = yyy + 0 * ly;
		}

		public void mouseReleased(Event arg0) {
		}

	}

	private final List<SelectionGroup<iComponent>> selectionGroups = new ArrayList<SelectionGroup<iComponent>>();

	private Rect bounds;

	private boolean dirty;

	private CoordinateFrame coordSys;

	private PointList points;

	private DynamicPointlist point;

	private ComponentContainer inside;

	private iLabel label;

	private TriangleMesh labelTriangles;

	private iDynamicMesh labelTriangle;

	private final Shell frame;

	private iVisualElementOverrides overrides;

	private int ox;

	private int oy;

	boolean justSelected = false;

	boolean selected = false;

	List<iPaintPeer> paintPeers = new ArrayList<iPaintPeer>();

	List<iMousePeer> mousePeers = new ArrayList<iMousePeer>();
	Set<iMousePeer> rootOnlyMousePeers = new HashSet<iMousePeer>();

	iMousePeer mousePeers_list = ReflectionTools.listProxy(mousePeers, iMousePeer.class);

	private boolean hasFocus;

	public RootComponent(Shell frame) {
		this.frame = frame;
		updateBounds();
	}

	public void addMousePeer(iMousePeer peer) {
		if (!mousePeers.contains(peer))
			mousePeers.add(peer);
		if (inside != null)
			inside.addAsAllEventHandler(this);
	}

	public void addMousePeer(iMousePeer peer, boolean allEvents) {
		if (!mousePeers.contains(peer))
			mousePeers.add(peer);
		if (inside != null && allEvents)
			inside.addAsAllEventHandler(this);
		if (!allEvents)
			rootOnlyMousePeers.add(peer);
	}

	public void addPaintPeer(iPaintPeer peer) {
		paintPeers.add(peer);
	}

	public RootComponent addToSelectionGroup(SelectionGroup<iComponent> group) {
		selectionGroups.add(group);
		group.register(this);
		return this;
	}

	public void beginMouseFocus(ComponentContainer inside) {
		hasFocus = true;
	}

	public void endMouseFocus(ComponentContainer inside) {
		hasFocus = false;
	}

	public Rect getBounds() {
		return bounds;
	}

	public float getHeight() {
		return (float) bounds.h;
	}

	public iVisualElement getVisualElement() {
		return null;
	}

	public float getWidth() {
		return (float) bounds.w;
	}

	public float getX() {
		return (float) bounds.x;
	}

	public float getY() {
		return (float) bounds.y;
	}

	public void handleResize(Set<Resize> currentResize, float dx, float dy) {
		if (currentResize.contains(Resize.translate))
			RootComponent.this.shouldSetSize((float) RootComponent.this.bounds.x + dx, (float) RootComponent.this.bounds.y + dy, RootComponent.this.getWidth(), RootComponent.this.getHeight());
		if (currentResize.contains(Resize.left)) {
			RootComponent.this.shouldSetSize((float) RootComponent.this.bounds.x + dx, (float) RootComponent.this.bounds.y, (float) RootComponent.this.bounds.w - dx, (float) RootComponent.this.bounds.h);
		}
		if (currentResize.contains(Resize.right)) {
			RootComponent.this.shouldSetSize((float) RootComponent.this.bounds.x, (float) RootComponent.this.bounds.y, (float) RootComponent.this.bounds.w + dx, (float) RootComponent.this.bounds.h);
		}
		if (currentResize.contains(Resize.up)) {
			RootComponent.this.shouldSetSize((float) RootComponent.this.bounds.x, (float) RootComponent.this.bounds.y + dy, (float) RootComponent.this.bounds.w, (float) RootComponent.this.bounds.h - dy);
		}
		if (currentResize.contains(Resize.down)) {
			RootComponent.this.shouldSetSize((float) RootComponent.this.bounds.x, (float) RootComponent.this.bounds.y, (float) RootComponent.this.bounds.w, (float) RootComponent.this.bounds.h + dy);
		}
		if (currentResize.contains(Resize.innerTranslate)) {

			System.err.println(" inner translated <" + dx + " " + GLComponentWindow.getCurrentWindow(this).getXScale() + ">");
			GLComponentWindow.getCurrentWindow(this).setXTranslation((float) (GLComponentWindow.getCurrentWindow(this).getXTranslation() - dx));
			GLComponentWindow.getCurrentWindow(this).setYTranslation((float) (GLComponentWindow.getCurrentWindow(this).getYTranslation() - dy));
			inside.requestRedisplay();
		}
		if (currentResize.contains(Resize.innerScale)) {

			float sx = 1;
			float sy = 1;
			dx *= -1;
			dy *= -1;
			if (dx > 0) {
				sx = (float) Math.pow(1.01, dx);
			} else {
				sx = (float) Math.pow(0.99, -dx);
			}

			if (dy > 0) {
				sy = (float) Math.pow(1.01, dy);
			} else {
				sy = (float) Math.pow(0.99, -dy);
			}

			// lets try and translate around
			// the center of the
			// window

			int windowWidth = GLComponentWindow.getCurrentWindow(this).getFrame().getSize().x;
			int windowHeight = GLComponentWindow.getCurrentWindow(this).getFrame().getSize().y;

			// the center of the window in
			// transformed coords is:

			float transformedCenterX = GLComponentWindow.getCurrentWindow(this).getXTranslation() + windowWidth * GLComponentWindow.getCurrentWindow(this).getXScale() / 2;
			float transformedCenterY = GLComponentWindow.getCurrentWindow(this).getYTranslation() + windowHeight * GLComponentWindow.getCurrentWindow(this).getXScale() / 2;

			// and we want this to remain
			// constant dispite the
			// change in scale

			// currently, it's uniform scale
			// only
			GLComponentWindow.getCurrentWindow(this).setXScale(Math.min(3, Math.max(0.2f, GLComponentWindow.getCurrentWindow(this).getXScale() * sy)));
			GLComponentWindow.getCurrentWindow(this).setYScale(Math.min(3, Math.max(0.2f, GLComponentWindow.getCurrentWindow(this).getYScale() * sy)));

			float ntransformedCenterX = GLComponentWindow.getCurrentWindow(this).getXTranslation() + windowWidth * GLComponentWindow.getCurrentWindow(this).getXScale() / 2;
			float ntransformedCenterY = GLComponentWindow.getCurrentWindow(this).getYTranslation() + windowHeight * GLComponentWindow.getCurrentWindow(this).getXScale() / 2;

			float deltaX = transformedCenterX - ntransformedCenterX;
			float deltaY = transformedCenterY - ntransformedCenterY;

			GLComponentWindow.getCurrentWindow(this).setXTranslation(GLComponentWindow.getCurrentWindow(this).getXTranslation() + deltaX);
			GLComponentWindow.getCurrentWindow(this).setYTranslation(GLComponentWindow.getCurrentWindow(this).getYTranslation() + deltaY);

			inside.requestRedisplay();
		}
	}

	// atX and atY in window coordinates
	public void handleResize(Set<Resize> currentResize, float dx, float dy, float atX, float atY) {

		currentResize.remove(Resize.translate);
		currentResize.remove(Resize.left);
		currentResize.remove(Resize.up);

		if (currentResize.contains(Resize.translate))
			RootComponent.this.shouldSetSize((float) RootComponent.this.bounds.x + dx * GLComponentWindow.getCurrentWindow(this).getXScale(), (float) RootComponent.this.bounds.y + dy * GLComponentWindow.getCurrentWindow(this).getXScale(), RootComponent.this.getWidth(), RootComponent.this.getHeight());
		if (currentResize.contains(Resize.left)) {
			RootComponent.this.shouldSetSize((float) RootComponent.this.bounds.x + dx * GLComponentWindow.getCurrentWindow(this).getXScale(), (float) RootComponent.this.bounds.y, (float) RootComponent.this.bounds.w - dx, (float) RootComponent.this.bounds.h);
		}
		if (currentResize.contains(Resize.right)) {
			RootComponent.this.shouldSetSize((float) RootComponent.this.bounds.x, (float) RootComponent.this.bounds.y, (float) RootComponent.this.bounds.w + dx * GLComponentWindow.getCurrentWindow(this).getXScale(), (float) RootComponent.this.bounds.h);
		}
		if (currentResize.contains(Resize.up)) {
			RootComponent.this.shouldSetSize((float) RootComponent.this.bounds.x, (float) RootComponent.this.bounds.y + dy * GLComponentWindow.getCurrentWindow(this).getXScale(), (float) RootComponent.this.bounds.w, (float) RootComponent.this.bounds.h - dy * GLComponentWindow.getCurrentWindow(this).getXScale());
		}
		if (currentResize.contains(Resize.down)) {
			RootComponent.this.shouldSetSize((float) RootComponent.this.bounds.x, (float) RootComponent.this.bounds.y, (float) RootComponent.this.bounds.w, (float) RootComponent.this.bounds.h + dy * GLComponentWindow.getCurrentWindow(this).getXScale());
		}
		if (currentResize.contains(Resize.innerTranslate)) {

			System.err.println(" inner translated <" + dx + " " + atX + " " + GLComponentWindow.getCurrentWindow(this).getXScale() + ">");
			GLComponentWindow.getCurrentWindow(this).setXTranslation(GLComponentWindow.getCurrentWindow(this).getXTranslation() - dx * GLComponentWindow.getCurrentWindow(this).getXScale());
			GLComponentWindow.getCurrentWindow(this).setYTranslation(GLComponentWindow.getCurrentWindow(this).getYTranslation() - dy * GLComponentWindow.getCurrentWindow(this).getYScale());

			;//System.out.println(" requesting redisplay ");

			inside.requestRedisplay();
		}
		if (currentResize.contains(Resize.innerScale)) {

			float sx = 1;
			float sy = 1;
			dx *= -1;
			dy *= -1;
			if (dx > 0) {
				sx = (float) Math.pow(1.01, dx);
			} else {
				sx = (float) Math.pow(0.99, -dx);
			}

			if (dy > 0) {
				sy = (float) Math.pow(1.01, dy);
			} else {
				sy = (float) Math.pow(0.99, -dy);
			}

			// lets try and translate around
			// the center of the
			// window

			int windowWidth = GLComponentWindow.getCurrentWindow(this).getFrame().getSize().x;
			int windowHeight = GLComponentWindow.getCurrentWindow(this).getFrame().getSize().y;

			// the center of the window in
			// transformed coords is:

			float transformedCenterX = GLComponentWindow.getCurrentWindow(this).getXTranslation() + atX * GLComponentWindow.getCurrentWindow(this).getXScale();
			float transformedCenterY = GLComponentWindow.getCurrentWindow(this).getYTranslation() + atY * GLComponentWindow.getCurrentWindow(this).getXScale();

			// and we want this to remain
			// constant dispite the
			// change in scale

			// currently, it's uniform scale
			// only
			GLComponentWindow.getCurrentWindow(this).setXScale(Math.min(100, Math.max(0.01f, GLComponentWindow.getCurrentWindow(this).getXScale() * sy)));
			GLComponentWindow.getCurrentWindow(this).setYScale(Math.min(100, Math.max(0.01f, GLComponentWindow.getCurrentWindow(this).getYScale() * sy)));

			float ntransformedCenterX = GLComponentWindow.getCurrentWindow(this).getXTranslation() + atX * GLComponentWindow.getCurrentWindow(this).getXScale();
			float ntransformedCenterY = GLComponentWindow.getCurrentWindow(this).getYTranslation() + atY * GLComponentWindow.getCurrentWindow(this).getXScale();

			float deltaX = transformedCenterX - ntransformedCenterX;
			float deltaY = transformedCenterY - ntransformedCenterY;

			GLComponentWindow.getCurrentWindow(this).setXTranslation(GLComponentWindow.getCurrentWindow(this).getXTranslation() + deltaX);
			GLComponentWindow.getCurrentWindow(this).setYTranslation(GLComponentWindow.getCurrentWindow(this).getYTranslation() + deltaY);

			inside.requestRedisplay();
		}
	}

	public iComponent hit(Event event) {
		return this;
	}

	public float isHit(Event event) {
		return -Float.MAX_VALUE;
	}

	public boolean isSelected() {
		return false;
	}

	public void keyPressed(ComponentContainer inside, Event arg0) {
		this.inside = inside;
		mousePeers_list.keyPressed(inside, arg0);
		if (mousePeers.size() > 0) {
			// inside.requestRedisplay();
			if (!arg0.doit)
				return;
		}
	}

	public void keyReleased(ComponentContainer inside, Event arg0) {
		this.inside = inside;
		mousePeers_list.keyReleased(inside, arg0);
		if (mousePeers.size() > 0) {
			inside.requestRedisplay();
			if (!arg0.doit)
				return;
		}
	}

	public void keyTyped(ComponentContainer inside, Event arg0) {
		this.inside = inside;
		mousePeers_list.keyTyped(inside, arg0);
		if (mousePeers.size() > 0) {
			// inside.requestRedisplay();
			if (!arg0.doit)
				return;
		}
	}

	public void mouseClicked(ComponentContainer inside, Event arg0) {
		this.inside = inside;
		mousePeers_list.mouseClicked(inside, arg0);

		if (mousePeers.size() > 0) {
			inside.requestRedisplay();
		}
		if (!arg0.doit)
			return;
		GLComponentWindow.getCurrentWindow(this).untransformMouseEvent(arg0);
		if (arg0.count==2 && mousePeers.size()==1) // just the hierarchy plugin
			GLComponentWindow.getCurrentWindow(this).togglePanelVisiblity();
	}

	public void mouseDragged(ComponentContainer inside, Event arg0) {

		// new Exception().printStackTrace();

		this.inside = inside;
		mousePeers_list.mouseDragged(inside, arg0);
		if (mousePeers.size() > 0) {
			inside.requestRedisplay();
		}

		executionGesture.drag(arg0);

		if (!arg0.doit)
			return;
		GLComponentWindow.getCurrentWindow(this).untransformMouseEvent(arg0);
		this.inside = inside;
		updateBounds();
		dragger.mouseDragged(arg0);
		GLComponentWindow.getCurrentWindow(this).transformMouseEvent(arg0);
	}

	public void mouseEntered(ComponentContainer inside, Event arg0) {
		this.inside = inside;
		mousePeers_list.mouseEntered(inside, arg0);
		if (mousePeers.size() > 0) {
			inside.requestRedisplay();
		}
		if (!arg0.doit)
			return;
		GLComponentWindow.getCurrentWindow(this).untransformMouseEvent(arg0);
		this.inside = inside;
		updateBounds();
		dragger.mouseEntered(arg0);
		GLComponentWindow.getCurrentWindow(this).transformMouseEvent(arg0);
	}

	public void mouseExited(ComponentContainer inside, Event arg0) {
		this.inside = inside;
		mousePeers_list.mouseExited(inside, arg0);
		if (mousePeers.size() > 0) {
			inside.requestRedisplay();
		}
		if (!arg0.doit)
			return;
		GLComponentWindow.getCurrentWindow(this).untransformMouseEvent(arg0);
		this.inside = inside;
		updateBounds();
		dragger.mouseExited(arg0);
		GLComponentWindow.getCurrentWindow(this).transformMouseEvent(arg0);
	}

	public void mouseMoved(ComponentContainer inside, Event arg0) {
		this.inside = inside;
		mousePeers_list.mouseMoved(inside, arg0);
		if (mousePeers.size() > 0) {
			// inside.requestRedisplay();
		}
		if (!arg0.doit)
			return;
		GLComponentWindow.getCurrentWindow(this).untransformMouseEvent(arg0);
		this.inside = inside;
		updateBounds();
		dragger.mouseMoved(arg0);
		GLComponentWindow.getCurrentWindow(this).transformMouseEvent(arg0);
	}

	ExecutionGesture executionGesture = new ExecutionGesture();

	public void mousePressed(ComponentContainer inside, Event arg0) {
		this.inside = inside;
		mousePeers_list.mousePressed(inside, arg0);
		if (mousePeers.size() > 0) {
			inside.requestRedisplay();
		}

		if (!arg0.doit)
			return;
		GLComponentWindow.getCurrentWindow(this).untransformMouseEvent(arg0);
		try {

			if (Platform.isPopupTrigger(arg0)) {

				;//System.out.println(" attempting to show root menu");

				if (GLComponentWindow.getCurrentWindow(this).present)
					return;

				// assemble and
				// present menu

				LinkedHashMap<String, iUpdateable> items = new LinkedHashMap<String, iUpdateable>();

				;//System.out.println(" -- menu items for ");
				overrides.menuItemsFor(null, items);

				BetterPopup menu = new SmallMenu().createMenu(items, GLComponentWindow.getCurrentWindow(this).getCanvas().getShell(), null);

				//
				Ref<GLComponentWindow> ref = new Ref<GLComponentWindow>(null);
				overrides.getProperty(null, iVisualElement.enclosingFrame, ref);
				if (ref.get() != null) {

					// menu.show(ref.get().getCanvas(),
					// (int)
					// ref.get().getCurrentMouseInWindowCoordinates().x,
					// (int)
					// ref.get().getCurrentMouseInWindowCoordinates().y);

					;//System.out.println(" here we go .... ");

					menu.show(Launcher.display.map(GLComponentWindow.getCurrentWindow(this).getCanvas(), GLComponentWindow.getCurrentWindow(this).getCanvas().getShell(), new Point(arg0.x, arg0.y)));

				} else {
				}

			} else if (arg0.button == 1 && (arg0.stateMask & SWT.ALT) != 0) {

				;//System.out.println(" beginning execution gesture ");

				executionGesture.begin(arg0);
			} else {
				updateBounds();
				this.inside = inside;
				dragger.mousePressed(arg0);
				ox = arg0.x;
				oy = arg0.y;

				if (!selected && hasFocus) {
					if ((arg0.stateMask & SWT.SHIFT) == 0 && arg0.button == 1)
						for (SelectionGroup<iComponent> d : selectionGroups)
							d.deselectAll();
					for (SelectionGroup<iComponent> d : selectionGroups)
						d.addToSelection(RootComponent.this);
					RootComponent.this.setSelected(true);
					justSelected = true;
				} else
					justSelected = false;
			}

		} finally {
			GLComponentWindow.getCurrentWindow(this).transformMouseEvent(arg0);
		}
	}

	public void mouseReleased(ComponentContainer inside, Event arg0) {
		this.inside = inside;

		mousePeers_list.mouseReleased(inside, arg0);
		if (mousePeers.size() > 0) {
			inside.requestRedisplay();
		}

		executionGesture.end(arg0);

		if (!arg0.doit)
			return;
		GLComponentWindow.getCurrentWindow(this).untransformMouseEvent(arg0);
		updateBounds();
		this.inside = inside;
		if (arg0.x == ox && arg0.y == oy) {
			if (selected) {
				if ((arg0.stateMask & SWT.SHIFT) != 0) {
					if (!justSelected) {
						for (SelectionGroup<iComponent> d : selectionGroups)
							d.removeFromSelection(RootComponent.this);
						RootComponent.this.setSelected(false);
					}
				} else {
					for (SelectionGroup<iComponent> d : selectionGroups)
						d.deselectAll();
					for (SelectionGroup<iComponent> d : selectionGroups)
						d.addToSelection(RootComponent.this);
					RootComponent.this.setSelected(true);
				}
			}
		}
		dragger.mouseReleased(arg0);
		GLComponentWindow.getCurrentWindow(this).transformMouseEvent(arg0);
	}

	public void paint(ComponentContainer inside, iCoordinateFrame frameSoFar, boolean visible) {
		this.inside = inside;
		for (iPaintPeer p : paintPeers) {
			p.paint(this);
		}

		executionGesture.paint();

	}

	public void removeMousePeer(iMousePeer peer) {
		mousePeers.remove(peer);
		rootOnlyMousePeers.remove(peer);

		ArrayList<iMousePeer> mp = new ArrayList<iMousePeer>(mousePeers);
		mp.removeAll(rootOnlyMousePeers);

		if (mp.size() == 0) {
			if (inside != null)
				inside.removeAsAllEventHandler(this);
		}
	}

	public void removePaintPeer(iPaintPeer marqueeTool) {
		paintPeers.remove(marqueeTool);
	}

	public void repaint() {
		if (inside != null)
			inside.requestRedisplay();
	}

	public void setBounds(Rect r) {
		bounds = r;
	}

	public RootComponent setOverrides(iVisualElementOverrides overrides) {
		this.overrides = overrides;
		return this;
	}

	public void setSelected(boolean selected) {
	}

	public iComponent setVisualElement(iVisualElement ve) {
		return this;
	}

	private void shouldSetSize(float x, float y, float w, float h) {

		// anything other than innerXXX is disabled
		if (true)
			return;

		boolean onlyTranslate = true;
		bounds.x = x;
		bounds.y = y;
		if (bounds.w != w)
			onlyTranslate = false;
		if (bounds.h != h)
			onlyTranslate = false;
		bounds.w = Math.max(w, 2);
		bounds.h = Math.max(h, 2);

		frame.setBounds((int) x, (int) y, (int) bounds.w, (int) bounds.h);

		if (inside != null) {
			dirty = true;
			inside.requestRedisplay();
		}
	}

	private void updateBounds() {
		Rectangle b = frame.getBounds();
		if (b != null) {
			bounds = new Rect(0, 0, 0, 0);
			bounds.w = (float) b.width;
			bounds.h = (float) b.height;
			bounds.x = (float) b.x;
			bounds.y = (float) b.y;
		}
	}

}
