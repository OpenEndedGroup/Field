package field.core.windowing.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;

import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElementOverrides.Ref;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.python.PythonPlugin;
import field.core.ui.ExtendedMenuMap;
import field.core.ui.PresentationMode;
import field.core.ui.SmallMenu;
import field.core.ui.SmallMenu.BetterPopup;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.components.DraggableComponent.Resize;
import field.launch.Launcher;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Vector2;
import field.math.linalg.iCoordinateFrame;
import field.namespace.context.CT.Dispatch;
import field.namespace.generic.Bind.iFunction;
import field.util.PythonUtils;
import field.util.TaskQueue;

public class PlainDraggableComponent implements iComponent, iDraggableComponent {

	static public final VisualElementProperty<Boolean> hasMouseFocus = new VisualElementProperty<Boolean>("hasMouseFocus");

	protected Rect bounds = new Rect(0, 0, 0, 0);

	private boolean dirty;

	private CoordinateFrame coordSys;

	protected ComponentContainer inside;

	public iVisualElement element;

	public iVisualElementOverrides overridingInterface;

	public PlainDraggableComponent() {
		this(new Rect(0, 0, 0, 0));
	}

	public PlainDraggableComponent(Rect bounds) {
		this.bounds = bounds;

		coordSys = new CoordinateFrame();

	}

	public void dispose() {
		assert getInside() != null;

	}

	public PlainDraggableComponent setVisualElement(iVisualElement element) {
		this.element = element;
		overridingInterface = new Dispatch<iVisualElement, iVisualElementOverrides>(iVisualElementOverrides.topology).getOverrideProxyFor(element, iVisualElementOverrides.class);
		this.setBounds(element.getFrame(new Rect(0, 0, 0, 0)));
		return this;
	}

	public iVisualElement getVisualElement() {
		return this.element;
	}

	protected List<SelectionGroup<iComponent>> getSelectionGroups() {
		ArrayList<SelectionGroup<iComponent>> sel = new ArrayList<SelectionGroup<iComponent>>();
		Ref<SelectionGroup<iComponent>> out = new Ref<SelectionGroup<iComponent>>(null);
		// overridingInterface.getProperty(element,
		// iVisualElement.selectionGroup, out);

		SelectionGroup<iComponent> s = iVisualElement.selectionGroup.get(element);

		if (s != null)
			sel.add(s);
		return sel;
	}

	boolean selectable = true;

	private int ox;

	private int oy;

	private boolean hidden;

	public void setSelected(boolean selected) {
		boolean changed = this.selected != selected;
		this.selected = selected;
		if (selected == false) {
			if (marked)
				changed = true;
			this.marked = false;
		}
		if (changed) {
			this.dirty = true;
			if (getInside() != null)
				getInside().requestRedisplay();
		}

	}

	public boolean isSelected() {
		return selected;
	}

	public float isHit(Event event) {
		if (event.button == 2)
			return Float.NEGATIVE_INFINITY;

		if (event.x > bounds.x && event.y > bounds.y && event.x < (bounds.x + bounds.w) && event.y < (bounds.y + bounds.h)) {
			Ref<Boolean> is = new Ref<Boolean>(true);
			overridingInterface.isHit(element, event, is);
			return is.get() ? 1 - bounds.size() : Float.NEGATIVE_INFINITY;
		} else {
			Ref<Boolean> is = new Ref<Boolean>(false);
			overridingInterface.isHit(element, event, is);
			return is.get() ? 1 - bounds.size() : Float.NEGATIVE_INFINITY;
		}
	}

	public iComponent hit(Event event) {
		return this;
	}

	public Rect getBounds() {
		return bounds;
	}

	public void setBounds(Rect r) {
		boolean dirty = false;
		if (r.x != bounds.x || r.w != bounds.w || r.h != bounds.h || r.y != bounds.y)
			this.setDirty();
		bounds.setValue(r);
	}

	public void setHidden(boolean hidden) {
		boolean dirty = this.hidden != hidden;
		this.hidden = hidden;
		if (dirty)
			this.setDirty();
	}

	public void keyTyped(ComponentContainer inside, Event arg0) {
		this.setInside(inside);
	}

	public void keyPressed(ComponentContainer inside, Event arg0) {
		overridingInterface.handleKeyboardEvent(getVisualElement(), arg0);
	}

	public void keyReleased(ComponentContainer inside, Event arg0) {
	}

	public void mouseClicked(ComponentContainer inside, Event arg0) {
	}

	boolean justSelected = false;

	public boolean marked = false;

	protected List<SelectionGroup<iComponent>> getMarkingGroups() {
		ArrayList<SelectionGroup<iComponent>> sel = new ArrayList<SelectionGroup<iComponent>>();
		Ref<SelectionGroup<iComponent>> out = new Ref<SelectionGroup<iComponent>>(null);
		overridingInterface.getProperty(element, iVisualElement.markingGroup, out);
		if (out.get() != null)
			sel.add(out.get());
		overridingInterface.getProperty(element, iVisualElement.markingGroup, out);
		return sel;
	}

	public boolean isMarked() {
		return marked;
	}

	public void setMarked(boolean marked) {
		boolean changed = this.marked != marked;
		this.marked = marked;
		if (changed) {
			this.dirty = true;
			if (getInside() != null)
				getInside().requestRedisplay();

			for (SelectionGroup g : getMarkingGroups()) {
				g.register(this);
				if (marked)
					g.addToSelection(this);
			}

		}

	}

	public void mousePressed(final ComponentContainer inside, Event arg0) {
		this.setInside(inside);
		if (arg0.button == 2)
			return;

		if (!GLComponentWindow.getCurrentWindow(this).present) {

			final iFunction<Boolean, iComponent> f = decoration.down(arg0);
			if (f != null) {
				inside.requestRedisplay();
				paintQueue.new Task() {

					int i = 0;

					@Override
					protected void run() {
						i++;
						if (i == 15) {
							f.f(PlainDraggableComponent.this);
							inside.requestRedisplay();
						} else {
							inside.requestRedisplay();
							recur();
						}
					}
				};
			}
		}
		arg0.doit=false;
		if (Platform.isPopupTrigger(arg0)) {

			// assemble and present menu

			ExtendedMenuMap items = new ExtendedMenuMap();
			overridingInterface.menuItemsFor(element, items);

			Ref<GLComponentWindow> ref = new Ref<GLComponentWindow>(null);
			this.overridingInterface.getProperty(element, iVisualElement.enclosingFrame, ref);
			if (ref.get() != null) {
				// menu.show(ref.get().getCanvas(), (int)
				// ref.get().getCurrentMouseInWindowCoordinates().x,
				// (int)
				// ref.get().getCurrentMouseInWindowCoordinates().y);
				GLComponentWindow.getCurrentWindow(this).untransformMouseEvent(arg0);
				
				
				;//System.out.println(" doing menu <"+items+">");
				
				items.doMenu(ref.get().getCanvas(), new Point(arg0.x, arg0.y));

//				BetterPopup menu = new SmallMenu().createMenu(items, ref.get().getCanvas().getShell(), null);
//				menu.show(new Point(arg0.x, arg0.y));
				
				//items.doMenu(ref.get().getCanvas(), arg0.getPoint());

				// menu.show(ref.get().getCanvas(),
				// arg0.x,arg0.y);
				GLComponentWindow.getCurrentWindow(this).transformMouseEvent(arg0);
			} else {
			}
		} else {

			if ((arg0.stateMask & SWT.ALT) != 0 && arg0.button == 1) {
				if (!decoration.isExecuting())
					overridingInterface.beginExecution(getVisualElement());
			} else if ((arg0.stateMask & SWT.COMMAND) != 0) {
//				this.setMarked(!this.isMarked());
				if (isMarked()) {
//					for (SelectionGroup<iComponent> d : getMarkingGroups())
//						d.addToSelection(PlainDraggableComponent.this);
				} else {
					for (SelectionGroup<iComponent> d : getMarkingGroups())
						d.removeFromSelection(PlainDraggableComponent.this);
				}
			} else {

				dragger.mousePressed(arg0);
				ox = arg0.x;
				oy = arg0.y;

				if (!selected) {
					if ((arg0.stateMask & SWT.SHIFT) == 0) {
						for (SelectionGroup<iComponent> d : getSelectionGroups())
							d.deselectAll();
						for (SelectionGroup<iComponent> d : getMarkingGroups())
							d.deselectAll();
					}
					if (PresentationMode.isSelectable(this)) {

						for (SelectionGroup<iComponent> d : getSelectionGroups())
							d.addToSelection(PlainDraggableComponent.this);
						PlainDraggableComponent.this.setSelected(true);
						justSelected = true;
					}
				} else
					justSelected = false;
			}
		}
	}

	public void mouseReleased(ComponentContainer inside, Event arg0) {
		if (arg0.button == 2)
			return;
		arg0.doit=false;
		if (decoration.up(arg0))
			inside.requestRedisplay();

		if ((arg0.stateMask & SWT.ALT) != 0) {
			if (!ignoreAltMouseUp)
				overridingInterface.endExecution(getVisualElement());
		} else {

			this.setInside(inside);
			if (arg0.x == ox && arg0.y == oy) {
				if (selected) {
					if ((arg0.stateMask & SWT.SHIFT) != 0) {
						if (!justSelected) {
							// for
							// (SelectionGroup<iComponent>
							// d :
							// getSelectionGroups())
							// d.removeFromSelection(PlainDraggableComponent.this);
							// PlainDraggableComponent.this.setSelected(false);
						}
					} else {
						for (SelectionGroup<iComponent> d : getSelectionGroups())
							d.deselectAll();
						for (SelectionGroup<iComponent> d : getSelectionGroups())
							d.addToSelection(PlainDraggableComponent.this);

						for (SelectionGroup<iComponent> d : getMarkingGroups())
							d.deselectAll();

						if (PresentationMode.isSelectable(this)) {
							PlainDraggableComponent.this.setSelected(true);
						}
					}
				}
			}
			dragger.mouseReleased(arg0);
		}
	}

	public void mouseEntered(ComponentContainer inside, Event arg0) {
		this.setInside(inside);
		if (arg0.button == 2)
			return;
		arg0.doit=false;
		dragger.mouseEntered(arg0);
	}

	public void mouseExited(ComponentContainer inside, Event arg0) {
		this.setInside(inside);
		if (arg0.button == 2)
			return;
		arg0.doit=false;
		dragger.mouseExited(arg0);
	}

	public void mouseDragged(ComponentContainer inside, Event arg0) {
		this.setInside(inside);
		if (arg0.button != 1)
			return;

		if (decoration.drag(arg0))
			inside.requestRedisplay();
		if ((arg0.stateMask & SWT.ALT)!=0)
			return;
		if ((arg0.stateMask & SWT.COMMAND)!=0)
			return;

		arg0.doit=false;
		dragger.mouseDragged(arg0);
	}

	public void mouseMoved(ComponentContainer inside, Event arg0) {
		this.setInside(inside);
		if (arg0.button == 2)
			return;
		arg0.doit=false;
		dragger.mouseMoved(arg0);
	}

	public void beginMouseFocus(ComponentContainer inside) {
		hasMouseFocus.set(getVisualElement(), getVisualElement(), true);
	}

	public void endMouseFocus(ComponentContainer inside) {
		hasMouseFocus.set(getVisualElement(), getVisualElement(), false);
	}

	boolean ignoreAltMouseUp = false;

	ExecutionDecoration decoration = new ExecutionDecoration(this) {
		protected void continueToBeActiveAfterUp() {
			ignoreAltMouseUp = true;
		};

		protected boolean isExecuting() {
			Boolean b = getVisualElement().getProperty(PythonPlugin.python_isExecuting);
			return b != null && b;
		};

		protected void stopBeingActiveNow() {
			overridingInterface.endExecution(getVisualElement());
		};
	};
	TaskQueue paintQueue = new TaskQueue();

	boolean selected = false;

	String lastName = "";

	HashMap<CachedLine, CachedLine> offsetCache = new HashMap<CachedLine, CachedLine>();
	Rect offsetCachedAt = null;

	private void submitDecoratingLine(Object v) {
		if (v instanceof CachedLine) {
			CachedLine c = (CachedLine) v;

			if (!c.getProperties().isTrue(iLinearGraphicsContext.onSourceSelectedOnly, false) || isSelected()) {

				Vector2 offset = c.getProperties().get(iLinearGraphicsContext.offsetFromSource);

				if (offset != null) {
					CachedLine previous = offsetCache.get(c);
					if (previous == null) {
						CachedLine c2 = LineUtils.transformLineOffsetFrom(c, element.getFrame(null), offset);
						c.getProperties().put(iLinearGraphicsContext.offsetedLine, c2);
						offsetCache.put(c, c2);
						c = c2;
					} else {
						c = previous;
					}
				}

				GLComponentWindow.currentContext.submitLine(c, c.getProperties());
			}
		}
	}

	public void paint(ComponentContainer inside, iCoordinateFrame frameSoFar, boolean visible) {
		paintQueue.update();

		if (isHidden())
			return;

		decoration.paintNow();

		if (offsetCachedAt == null || !offsetCachedAt.equals(bounds)) {
			offsetCachedAt = new Rect(bounds);
			offsetCache = new HashMap<CachedLine, CachedLine>();
		}
		Map<Object, Object> pp = element.payload();
		Set<Entry<Object, Object>> e = pp.entrySet();
		for (Entry<Object, Object> ee : e) {
			Object k = ee.getKey();
			if (k instanceof VisualElementProperty && ((VisualElementProperty) k).getName().startsWith("decoration_")) {
				Object v = ee.getValue();
				v = PythonUtils.maybeToJava(v);
				submitDecoratingLine(v);
				if (v instanceof List) {
					for (Object o : ((List) v)) {
						o = PythonUtils.maybeToJava(o);
						submitDecoratingLine(o);
					}
				}
			}
		}

		if (overridingInterface != null) {
			overridingInterface.paintNow(element, this.getBounds(), visible);
		}
	}

	MouseDragger dragger = new MouseDragger();

	boolean canDrag = true;

	public class MouseDragger {

		Set<Resize> currentResize = new HashSet<Resize>();

		private int lastX;

		private int lastY;

		private int downX;

		private int downY;

		float zone = 5;

		public MouseDragger() {
		}

		public void mouseClicked(Event arg0) {
			arg0.doit=false;
		}

		boolean first = false;

		public void mousePressed(Event arg0) {
			arg0.doit=false;
			currentResize.clear();
			if (arg0.x - getX() < zone)
				currentResize.add(Resize.left);
			if (arg0.x - getX() > PlainDraggableComponent.this.getWidth() - zone)
				currentResize.add(Resize.right);
			if (arg0.y - getY() < zone)
				currentResize.add(Resize.up);
			if (arg0.y - getY() > PlainDraggableComponent.this.getHeight() - zone)
				currentResize.add(Resize.down);

			if (bounds.w < 15 && bounds.h < 15)
				currentResize.clear();

			if (currentResize.size() == 0)
				currentResize.add(Resize.translate);

			if (!canDrag)
				currentResize.clear();

			PresentationMode.filterResize(PlainDraggableComponent.this, currentResize);

			int lx = (int) PlainDraggableComponent.this.getX();
			int ly = (int) PlainDraggableComponent.this.getY();

			downX = lastX = arg0.x;
			downY = lastY = arg0.y;

			first = true;

		}

		public void mouseReleased(Event arg0) {
			arg0.doit=false;
			if (!first) {
				DraggableComponent.finalizeRect(element, currentResize, 0);

				for (SelectionGroup<iComponent> s : getSelectionGroups()) {
					for (iComponent d : s.getSelection()) {
						if (d != PlainDraggableComponent.this && d.getVisualElement() != null) {
							DraggableComponent.finalizeRect(d.getVisualElement(), currentResize, 0);
						}
					}
				}
			}

		}

		public void mouseEntered(Event arg0) {
		}

		public void mouseExited(Event arg0) {
			//TODO swt cursor
//			GLComponentWindow.getCurrentWindow(PlainDraggableComponent.this).getCanvas().setCursor(Cursor.getDefaultCursor());
		GLComponentWindow.getCurrentWindow(PlainDraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_ARROW));
		}

		public void mouseDragged(Event arg0) {
			arg0.doit=false;

			int lx = (int) PlainDraggableComponent.this.getX();
			int ly = (int) PlainDraggableComponent.this.getY();

			float dx = (arg0.x) - lastX;
			float dy = (arg0.y) - lastY;

			if (first) {
				DraggableComponent.initiateRect(element, currentResize);
				for (SelectionGroup<iComponent> s : getSelectionGroups()) {
					for (iComponent d : s.getSelection()) {
						if (d != PlainDraggableComponent.this)
							DraggableComponent.initiateRect(d.getVisualElement(), currentResize);
					}
				}

				first = false;
			}

			for (SelectionGroup<iComponent> s : getSelectionGroups()) {
				for (iComponent d : s.getSelection()) {
					if (d != PlainDraggableComponent.this)
						d.handleResize(currentResize, dx, dy);
				}
			}

			PlainDraggableComponent.this.handleResize(currentResize, dx, dy);
			lastX = arg0.x;
			lastY = arg0.y;

		}

		public void mouseMoved(Event arg0) {
			arg0.doit=false;
			Set<Resize> currentResize = new HashSet<Resize>();

			currentResize.clear();
			if (arg0.x - getX() < zone)
				currentResize.add(Resize.left);
			if (arg0.x - getX() > PlainDraggableComponent.this.getWidth() - zone)
				currentResize.add(Resize.right);
			if (arg0.y - getY() < zone)
				currentResize.add(Resize.up);
			if (arg0.y - getY() > PlainDraggableComponent.this.getHeight() - zone)
				currentResize.add(Resize.down);
			if (currentResize.size() == 0)
				currentResize.add(Resize.translate);

			PresentationMode.filterResize(PlainDraggableComponent.this, currentResize);

			if (getInside() != null) {
				// TODO: 64 \u2014 confront mouse cursor setting in
				// pure java
				if (currentResize.contains(Resize.left) && currentResize.contains(Resize.down))
					// NSCursor.crosshairCursor().set();
					GLComponentWindow.getCurrentWindow(PlainDraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_SIZESW));
				else if (currentResize.contains(Resize.left) && currentResize.contains(Resize.up))
					// NSCursor.crosshairCursor().set();
					GLComponentWindow.getCurrentWindow(PlainDraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_SIZENW));
				else if (currentResize.contains(Resize.right) && currentResize.contains(Resize.up))
					// NSCursor.crosshairCursor().set();
					GLComponentWindow.getCurrentWindow(PlainDraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_SIZENE));
				else if (currentResize.contains(Resize.right) && currentResize.contains(Resize.down))
					// NSCursor.crosshairCursor().set();
					GLComponentWindow.getCurrentWindow(PlainDraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_SIZESE));
				else if (currentResize.contains(Resize.right))
					// NSCursor.resizeRightCursor().set();
					GLComponentWindow.getCurrentWindow(PlainDraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_SIZEE));
				else if (currentResize.contains(Resize.left))
					GLComponentWindow.getCurrentWindow(PlainDraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_SIZEW));
				// NSCursor.resizeLeftCursor().set();
				else if (currentResize.contains(Resize.up))
					GLComponentWindow.getCurrentWindow(PlainDraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_SIZEN));
				// NSCursor.resizeUpCursor().set();
				else if (currentResize.contains(Resize.down))
					GLComponentWindow.getCurrentWindow(PlainDraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_SIZES));
				// NSCursor.resizeDownCursor().set();
				else if (currentResize.contains(Resize.translate))
					// NSCursor.crosshairCursor().set();
					GLComponentWindow.getCurrentWindow(PlainDraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_HAND));
			}
			}

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

	public float getHeight() {
		return (float) bounds.h;
	}

	public void handleResize(Set<Resize> currentResize, float dx, float dy) {
		if (currentResize.contains(Resize.translate))
			PlainDraggableComponent.this.shouldSetSize((float) PlainDraggableComponent.this.bounds.x + dx, (float) PlainDraggableComponent.this.bounds.y + dy, PlainDraggableComponent.this.getWidth(), PlainDraggableComponent.this.getHeight());
		if (currentResize.contains(Resize.left)) {
			PlainDraggableComponent.this.shouldSetSize((float) PlainDraggableComponent.this.bounds.x + dx, (float) PlainDraggableComponent.this.bounds.y, (float) PlainDraggableComponent.this.bounds.w - dx, (float) PlainDraggableComponent.this.bounds.h);
		}
		if (currentResize.contains(Resize.right)) {
			PlainDraggableComponent.this.shouldSetSize((float) PlainDraggableComponent.this.bounds.x, (float) PlainDraggableComponent.this.bounds.y, (float) PlainDraggableComponent.this.bounds.w + dx, (float) PlainDraggableComponent.this.bounds.h);
		}
		if (currentResize.contains(Resize.up)) {
			PlainDraggableComponent.this.shouldSetSize((float) PlainDraggableComponent.this.bounds.x, (float) PlainDraggableComponent.this.bounds.y + dy, (float) PlainDraggableComponent.this.bounds.w, (float) PlainDraggableComponent.this.bounds.h - dy);
		}
		if (currentResize.contains(Resize.down)) {
			PlainDraggableComponent.this.shouldSetSize((float) PlainDraggableComponent.this.bounds.x, (float) PlainDraggableComponent.this.bounds.y, (float) PlainDraggableComponent.this.bounds.w, (float) PlainDraggableComponent.this.bounds.h + dy);
		}
	}

	private void shouldSetSize(float x, float y, float w, float h) {
		Rect oldBounds = new Rect(bounds.x, bounds.y, bounds.w, bounds.h);
		bounds.x = x;
		bounds.y = y;
		bounds.w = Math.max(w, 2);
		bounds.h = Math.max(h, 2);

		if (element != null) {
			overridingInterface.shouldChangeFrame(element, getBounds(), oldBounds, true);
		}

		if (getInside() != null) {
			dirty = true;
			getInside().requestRedisplay();
		}
	}

	public void setDirty() {
		this.dirty = true;
		if (getInside() != null)
			getInside().requestRedisplay();
	}

	private void setInside(ComponentContainer inside) {
		this.inside = inside;
	}

	private ComponentContainer getInside() {

		if (inside != null)
			return inside;
		if (element == null)
			return null;

		GLComponentWindow ec = iVisualElement.enclosingFrame.get(element);
		if (ec == null)
			return null;
		return ec.getRoot();
	}

	public void canDrag(boolean b) {
		this.canDrag = b;
	}

	private boolean isHidden() {
		return hidden;
	}

}
