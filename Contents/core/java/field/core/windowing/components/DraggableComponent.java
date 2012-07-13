package field.core.windowing.components;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.awt.Font;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.text.NumberFormatter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.CachedPerUpdate;
import field.core.Constants;
import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElementOverrides.Ref;
import field.core.plugins.drawing.BasicDrawingPlugin;
import field.core.plugins.drawing.BasicDrawingPlugin.FrameManipulation;
import field.core.plugins.drawing.SplineComputingOverride;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.help.ContextualHelp;
import field.core.plugins.help.HelpBrowser;
import field.core.plugins.python.PythonPlugin;
import field.core.plugins.python.PythonPluginEditor;
import field.core.ui.ExecutionDecoration2;
import field.core.ui.ExtendedMenuMap;
import field.core.ui.PresentationMode;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.graphics.core.BasicGeometry;
import field.graphics.core.BasicGeometry.LineList;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.graphics.core.CoreHelpers;
import field.graphics.core.PointList;
import field.graphics.core.TextSystem;
import field.graphics.core.TextSystem.RectangularLabel;
import field.graphics.core.TextSystem.iLabel;
import field.graphics.dynamic.DynamicLine;
import field.graphics.dynamic.DynamicMesh;
import field.graphics.dynamic.DynamicPointlist;
import field.graphics.dynamic.iDynamicMesh;
import field.launch.Launcher;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;
import field.math.linalg.iCoordinateFrame;
import field.math.linalg.iCoordinateFrame.iMutable;
import field.namespace.context.CT.Dispatch;
import field.namespace.generic.Bind.iFunction;
import field.util.PythonUtils;
import field.util.TaskQueue;

@Woven
public class DraggableComponent implements iComponent, iDraggableComponent {

	static public final VisualElementProperty<Number> text_opacity = new VisualElementProperty<Number>("textOpacity");

	// private List<SelectionGroup<iComponent>>
	// selectionGroups =
	// new ArrayList<SelectionGroup<iComponent>>();

	public class MouseDragger {

		private int lastX;

		private int lastY;

		private int downX;

		private int downY;

		Set<Resize> currentResize = new HashSet<Resize>();

		float zone = 10;

		boolean first = true;

		public MouseDragger() {
		}

		public void mouseClicked(Event arg0) {
			arg0.doit = false;
		}

		public void mouseDragged(Event arg0) {
			arg0.doit = false;
			if (!down)
				return;

			int lx = (int) DraggableComponent.this.getX();
			int ly = (int) DraggableComponent.this.getY();

			int dx = (arg0.x) - lastX;
			int dy = (arg0.y) - lastY;

			if (first) {
				initiateRect(element, currentResize);
				for (SelectionGroup<iComponent> s : getSelectionGroups()) {
					for (iComponent d : s.getSelection()) {
						if (d != DraggableComponent.this)
							initiateRect(d.getVisualElement(), currentResize);
					}
				}

				first = false;
			}

			for (SelectionGroup<iComponent> s : getSelectionGroups()) {
				for (iComponent d : s.getSelection()) {
					if (d != DraggableComponent.this)
						d.handleResize(currentResize, dx, dy);
				}
			}

			DraggableComponent.this.handleResize(currentResize, dx, dy);
			lastX = arg0.x;
			lastY = arg0.y;

		}

		public void mouseEntered(Event arg0) {
		}

		public void mouseExited(Event arg0) {
			// TODO: 64 \u2014 confront mouse cursor setting in pure
			// java
			// NSCursor.arrowCursor().set();

			// TODO swt cursor
			// GLComponentWindow.getCurrentWindow(DraggableComponent.this).getCanvas().setCursor(Cursor.getDefaultCursor());

			GLComponentWindow.getCurrentWindow(DraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_ARROW));

		}

		public void mouseMoved(Event arg0) {
			arg0.doit = false;

			Set<Resize> currentResize = new HashSet<Resize>();

			currentResize.clear();
			if (arg0.x - getX() < zone / GLComponentWindow.getCurrentWindow(DraggableComponent.this).getXScale())
				currentResize.add(Resize.left);
			if (arg0.x - getX() > DraggableComponent.this.getWidth() - zone / GLComponentWindow.getCurrentWindow(DraggableComponent.this).getXScale())
				currentResize.add(Resize.right);
			if (arg0.y - getY() < zone / GLComponentWindow.getCurrentWindow(DraggableComponent.this).getXScale())
				currentResize.add(Resize.up);
			if (arg0.y - getY() > DraggableComponent.this.getHeight() - zone / GLComponentWindow.getCurrentWindow(DraggableComponent.this).getXScale())
				currentResize.add(Resize.down);
			if (currentResize.size() == 0)
				currentResize.add(Resize.translate);

			PresentationMode.filterResize(DraggableComponent.this, currentResize);
			// TODO swt cursor

			if (getInside() != null) {
				// TODO: 64 \u2014 confront mouse cursor setting
				// in
				// pure java
				if (currentResize.contains(Resize.left) && currentResize.contains(Resize.down))
					// NSCursor.crosshairCursor().set();
					GLComponentWindow.getCurrentWindow(DraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_SIZESW));
				else if (currentResize.contains(Resize.left) && currentResize.contains(Resize.up))
					// NSCursor.crosshairCursor().set();
					GLComponentWindow.getCurrentWindow(DraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_SIZENW));
				else if (currentResize.contains(Resize.right) && currentResize.contains(Resize.up))
					// NSCursor.crosshairCursor().set();
					GLComponentWindow.getCurrentWindow(DraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_SIZENE));
				else if (currentResize.contains(Resize.right) && currentResize.contains(Resize.down))
					// NSCursor.crosshairCursor().set();
					GLComponentWindow.getCurrentWindow(DraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_SIZESE));
				else if (currentResize.contains(Resize.right))
					// NSCursor.resizeRightCursor().set();
					GLComponentWindow.getCurrentWindow(DraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_SIZEE));
				else if (currentResize.contains(Resize.left))
					GLComponentWindow.getCurrentWindow(DraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_SIZEW));
				// NSCursor.resizeLeftCursor().set();
				else if (currentResize.contains(Resize.up))
					GLComponentWindow.getCurrentWindow(DraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_SIZEN));
				// NSCursor.resizeUpCursor().set();
				else if (currentResize.contains(Resize.down))
					GLComponentWindow.getCurrentWindow(DraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_SIZES));
				// NSCursor.resizeDownCursor().set();
				else if (currentResize.contains(Resize.translate))
					// NSCursor.crosshairCursor().set();
					GLComponentWindow.getCurrentWindow(DraggableComponent.this).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_HAND));
			}
		}

		public void mousePressed(Event arg0) {
			arg0.doit = false;

			zone = Math.min(10, Math.max(2, Math.min(DraggableComponent.this.getHeight() / 4, DraggableComponent.this.getWidth())));

			down = true;

			currentResize.clear();
			if (arg0.x - getX() < zone)
				currentResize.add(Resize.left);
			if (arg0.x - getX() > DraggableComponent.this.getWidth() - zone)
				currentResize.add(Resize.right);
			if (arg0.y - getY() < zone)
				currentResize.add(Resize.up);
			if (arg0.y - getY() > DraggableComponent.this.getHeight() - zone)
				currentResize.add(Resize.down);
			if (currentResize.size() == 0)
				currentResize.add(Resize.translate);

			PresentationMode.filterResize(DraggableComponent.this, currentResize);

			int lx = (int) DraggableComponent.this.getX();
			int ly = (int) DraggableComponent.this.getY();

			downX = lastX = arg0.x;
			downY = lastY = arg0.y;

			first = true;

		}

		boolean down = true;

		public void mouseReleased(Event arg0) {
			arg0.doit = false;

			down = false;
			if (!first) {

				finalizeRect(element, currentResize, arg0.stateMask);

				for (SelectionGroup<iComponent> s : getSelectionGroups()) {
					for (iComponent d : s.getSelection()) {
						if (d != DraggableComponent.this && d.getVisualElement() != null) {
							finalizeRect(d.getVisualElement(), currentResize, arg0.stateMask);
						}
					}
				}
			}

		}

	}

	public enum Resize {
		left, right, up, down, translate, innerTranslate, innerScale
	}

	static public void finalizeRect(iVisualElement toFinalize, Set<Resize> currentResize, int modifiersDown) {
		Rect frameIn = toFinalize.getFrame(new Rect(0, 0, 0, 0));
		Rect frameOut = toFinalize.getFrame(new Rect(0, 0, 0, 0));
		BasicDrawingPlugin.frameManipulationEnd.set(toFinalize, toFinalize, new FrameManipulation(currentResize, frameOut, modifiersDown));

		if (!frameIn.equals(frameOut)) {
			iVisualElementOverrides.topology.begin(toFinalize);
			iVisualElementOverrides.forward.shouldChangeFrame.shouldChangeFrame(toFinalize, frameOut, frameIn, true);
			iVisualElementOverrides.topology.end(toFinalize);

			if (GLComponentWindow.getCurrentWindow(null) != null) {
				GLComponentWindow.getCurrentWindow(null).getRoot().requestRedisplay();
			}
		}
	}

	static public void initiateRect(iVisualElement toInitiate, Set<Resize> with) {
		if (toInitiate != null) {
			BasicDrawingPlugin.frameManipulationBegin.set(toInitiate, toInitiate, new FrameManipulation(with, toInitiate.getFrame(new Rect(0, 0, 0, 0))));
		}
	}

	public iVisualElement element;

	public iVisualElementOverrides overridingInterface;

	public boolean marked = false;

	private Rect bounds = new Rect(0, 0, 0, 0);

	private final LineList lines;

	private final TriangleMesh triangles;

	private final DynamicLine line;

	private final iDynamicMesh triangle;

	private final CoordinateFrame coordSys;

	private final PointList points;

	private final DynamicPointlist point;

	private ComponentContainer inside;

	private iLabel label;

	private final TriangleMesh labelTriangles;

	private final iDynamicMesh labelTriangle;

	private int ox;

	private int oy;

	private boolean hidden;

	boolean dirty;

	boolean selectable = true;

	boolean justSelected = false;

	boolean selected = false;

	String lastName = "";

	Vector4 previousViewParameters = new Vector4();

	MouseDragger dragger = new MouseDragger();

	public DraggableComponent() {
		this(new Rect(0, 0, 0, 0));
	}

	public DraggableComponent(Rect bounds) {
		this.bounds = bounds;

		coordSys = new CoordinateFrame();

		lines = new BasicGeometry.LineList(new field.math.abstraction.iInplaceProvider<iMutable>() {
			public iMutable get(iMutable o) {
				return coordSys;
			}
		}).setWidth(1.5f);
		triangles = new BasicGeometry.TriangleMesh(new field.math.abstraction.iInplaceProvider<iMutable>() {
			public iMutable get(iMutable o) {
				return coordSys;
			}
		});
		points = new PointList(new field.math.abstraction.iInplaceProvider<iMutable>() {
			public iMutable get(iMutable o) {
				return coordSys;
			}
		});
		labelTriangles = new BasicGeometry.TriangleMesh(new field.math.abstraction.iInplaceProvider<iMutable>() {
			public iMutable get(iMutable o) {
				return coordSys;
			}
		});

		lines.rebuildVertex(0).rebuildTriangle(0);
		triangles.rebuildVertex(0).rebuildTriangle(0);
		labelTriangles.rebuildVertex(0).rebuildTriangle(0);
		points.rebuildVertex(0).rebuildTriangle(0);

		line = new DynamicLine(lines);
		triangle = new DynamicMesh(triangles);
		labelTriangle = new DynamicMesh(labelTriangles);
		point = new DynamicPointlist(points);
		dirty = true;

	}

	public void beginMouseFocus(ComponentContainer inside) {
		String help = ContextualHelp.contextualHelp.get(getVisualElement());
		if (help != null) {
			HelpBrowser browser = HelpBrowser.helpBrowser.get(getVisualElement());
			ContextualHelp ch = browser.getContextualHelp();
			ch.offerHelp("fromElement", help);
		}
	}

	public void dispose() {
		assert getInside() != null;

		lines.deallocate(GLComponentWindow.getCurrentWindow(this).getRunQueue());
		triangles.deallocate(GLComponentWindow.getCurrentWindow(this).getRunQueue());
		points.deallocate(GLComponentWindow.getCurrentWindow(this).getRunQueue());
		if (label != null)
			label.dispose();
	}

	public void endMouseFocus(ComponentContainer inside) {
	}

	public Rect getBounds() {
		return bounds;
	}

	public float getHeight() {
		return (float) bounds.h;
	}

	public iVisualElement getVisualElement() {
		return this.element;
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
			DraggableComponent.this.shouldSetSize((float) DraggableComponent.this.bounds.x + dx, (float) DraggableComponent.this.bounds.y + dy, DraggableComponent.this.getWidth(), DraggableComponent.this.getHeight());
		if (currentResize.contains(Resize.left)) {
			DraggableComponent.this.shouldSetSize((float) DraggableComponent.this.bounds.x + dx, (float) DraggableComponent.this.bounds.y, (float) DraggableComponent.this.bounds.w - dx, (float) DraggableComponent.this.bounds.h);
		}
		if (currentResize.contains(Resize.right)) {
			DraggableComponent.this.shouldSetSize((float) DraggableComponent.this.bounds.x, (float) DraggableComponent.this.bounds.y, (float) DraggableComponent.this.bounds.w + dx, (float) DraggableComponent.this.bounds.h);
		}
		if (currentResize.contains(Resize.up)) {
			DraggableComponent.this.shouldSetSize((float) DraggableComponent.this.bounds.x, (float) DraggableComponent.this.bounds.y + dy, (float) DraggableComponent.this.bounds.w, (float) DraggableComponent.this.bounds.h - dy);
		}
		if (currentResize.contains(Resize.down)) {
			DraggableComponent.this.shouldSetSize((float) DraggableComponent.this.bounds.x, (float) DraggableComponent.this.bounds.y, (float) DraggableComponent.this.bounds.w, (float) DraggableComponent.this.bounds.h + dy);
		}
	}

	public iComponent hit(Event event) {
		return this;
	}

	public float isHit(Event event) {
		if (event.button == 2)
			return Float.NEGATIVE_INFINITY;

		if (event.x > bounds.x && event.y > bounds.y && event.x < (bounds.x + bounds.w) && event.y < (bounds.y + bounds.h)) {
			Ref<Boolean> is = new Ref<Boolean>(true);
			overridingInterface.isHit(element, event, is);
			return is.get() ? 1 - bounds.size() : Float.NEGATIVE_INFINITY;
		}
		return Float.NEGATIVE_INFINITY;
	}

	public boolean isMarked() {
		return marked;
	}

	public boolean isSelected() {
		return selected;
	}

	public void keyPressed(ComponentContainer inside, Event arg0) {
		overridingInterface.handleKeyboardEvent(getVisualElement(), arg0);
	}

	public void keyReleased(ComponentContainer inside, Event arg0) {
		overridingInterface.handleKeyboardEvent(getVisualElement(), arg0);
	}

	public void keyTyped(ComponentContainer inside, Event arg0) {
		this.setInside(inside);
		overridingInterface.handleKeyboardEvent(getVisualElement(), arg0);
	}

	public void mouseClicked(ComponentContainer inside, Event arg0) {
	}

	public void mouseDragged(ComponentContainer inside, Event arg0) {
		this.setInside(inside);

		if (arg0.button == 2)
			return;

		if (decoration.drag(arg0))
			inside.requestRedisplay();

		if ((arg0.stateMask & SWT.ALT) != 0)
			return;

		dragger.mouseDragged(arg0);
		arg0.doit = false;
	}

	public void mouseEntered(ComponentContainer inside, Event arg0) {
		this.setInside(inside);
		if (arg0.button == 2)
			return;
		dragger.mouseEntered(arg0);
		arg0.doit = false;
	}

	public void mouseExited(ComponentContainer inside, Event arg0) {
		this.setInside(inside);
		if (arg0.button == 2)
			return;
		dragger.mouseExited(arg0);
		arg0.doit = false;
	}

	public void mouseMoved(ComponentContainer inside, Event arg0) {
		this.setInside(inside);
		if (arg0.button == 2)
			return;
		dragger.mouseMoved(arg0);
		arg0.doit = false;
	}

	boolean ignoreAltMouseUp = false;

	ExecutionDecoration2 decoration = new ExecutionDecoration2(this) {
		protected void continueToBeActiveAfterUp() {
			if (isExecuting())
				ignoreAltMouseUp = true;
			else {
				if (!decoration.isExecuting())
					overridingInterface.beginExecution(getVisualElement());
				inside.requestRedisplay();
			}
		};

		public boolean isExecuting() {
			Boolean b = getVisualElement().getProperty(PythonPlugin.python_isExecuting);
			return b != null && b;
		};

		public boolean isPaused() {
			Boolean b = getVisualElement().getProperty(PythonPlugin.python_isPaused);
			return b != null && b;
		};

		protected void stopBeingActiveNow() {
			getVisualElement().deleteProperty(PythonPluginEditor.python_isPaused);
			overridingInterface.endExecution(getVisualElement());
		};

		protected void pauseBeingActiveNow() {
			getVisualElement().setProperty(PythonPluginEditor.python_isPaused, true);
			inside.requestRedisplay();
		};

		protected void unpauseBeingActiveNow() {
			getVisualElement().deleteProperty(PythonPluginEditor.python_isPaused);
			inside.requestRedisplay();
		};
	};

	TaskQueue paintQueue = new TaskQueue();

	boolean mouseIsDown = false;

	public void mousePressed(final ComponentContainer inside, Event arg0) {
		this.setInside(inside);

		if (arg0.button == SWT.BUTTON2)
			return;

		if (!GLComponentWindow.getCurrentWindow(this).present) {
			mouseIsDown = true;
			final iFunction<Boolean, iComponent> f = decoration.down(arg0);
			inside.requestRedisplay();
			if (f != null) {
				paintQueue.new Task() {

					int i = 0;

					@Override
					protected void run() {
						if (!mouseIsDown)
							return;

						i++;
						if (i == 2) {
							f.f(DraggableComponent.this);
							inside.requestRedisplay();
						} else {
							inside.requestRedisplay();
							recur();
						}
					}
				};
			}
		}
		arg0.doit = false;
		if (Platform.isPopupTrigger(arg0)) {

			// assemble
			// and
			// present
			// menu

			// LinkedHashMap<String, iUpdateable> items = new
			// LinkedHashMap<String, iUpdateable>();
			ExtendedMenuMap items = new ExtendedMenuMap();
			overridingInterface.menuItemsFor(element, items);

			// JPopupMenu menu = new SmallMenu().createMenu(items);
			Ref<GLComponentWindow> ref = new Ref<GLComponentWindow>(null);
			this.overridingInterface.getProperty(element, iVisualElement.enclosingFrame, ref);
			if (ref.get() != null) {
				// menu.show(ref.get().getCanvas(), (int)
				// ref.get().getCurrentMouseInWindowCoordinates().x,
				// (int)
				// ref.get().getCurrentMouseInWindowCoordinates().y);
				GLComponentWindow.getCurrentWindow(this).untransformMouseEvent(arg0);

				items.doMenu(ref.get().getCanvas(), new Point(arg0.x, arg0.y));

				// menu.show(ref.get().getCanvas(),
				// arg0.getX(),arg0.getY());
				GLComponentWindow.getCurrentWindow(this).transformMouseEvent(arg0);
			} else {
			}
		} else {

			if ((arg0.stateMask & SWT.ALT) != 0 && arg0.button == 1) {
				if (!decoration.isExecuting())
					overridingInterface.beginExecution(getVisualElement());
			} else if ((arg0.stateMask & Platform.getCommandModifier()) != 0 && (arg0.stateMask & SWT.SHIFT) == 0) {
				this.setMarked(!this.isMarked());
				if (isMarked()) {
					for (SelectionGroup<iComponent> d : getMarkingGroups())
						d.addToSelection(DraggableComponent.this);
				} else {
					for (SelectionGroup<iComponent> d : getMarkingGroups())
						d.removeFromSelection(DraggableComponent.this);
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
							d.addToSelection(DraggableComponent.this);

						DraggableComponent.this.setSelected(true);
						justSelected = true;
					}
				} else
					justSelected = false;
			}
		}
	}

	public void mouseReleased(ComponentContainer inside, Event arg0) {
		mouseIsDown = false;
		if (arg0.button == SWT.BUTTON2)
			return;
		arg0.doit = false;

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
							for (SelectionGroup<iComponent> d : getSelectionGroups())
								d.removeFromSelection(DraggableComponent.this);
							DraggableComponent.this.setSelected(false);
						}
					} else {
						for (SelectionGroup<iComponent> d : getSelectionGroups())
							d.deselectAll();
						for (SelectionGroup<iComponent> d : getSelectionGroups())
							d.addToSelection(DraggableComponent.this);

						for (SelectionGroup<iComponent> d : getMarkingGroups())
							d.deselectAll();

						if (PresentationMode.isSelectable(this)) {
							DraggableComponent.this.setSelected(true);
						}
					}
				}
			}
			dragger.mouseReleased(arg0);
		}
		ignoreAltMouseUp = false;
	}

	public void paint(ComponentContainer inside, iCoordinateFrame frameSoFar, boolean visible) {

		paintQueue.update();
		if (isHidden()) {

			;// System.out.println(" component is hidden");

			if (overridingInterface != null) {
				overridingInterface.paintNow(element, this.getBounds(), visible);
			}
			return;
		}

		if (visible) {

			decoration.paintNow();

			Ref<String> r = new Ref<String>("");
			overridingInterface.getProperty(element, iVisualElement.name, r);

			boolean d = scaleHasChanged();

			Number op = element.getProperty(SplineComputingOverride.global_opacity);
			if (op == null)
				op = 1f;

			Number opt = element.getProperty(text_opacity);
			if (opt == null)
				opt = 1f;

			{
				CachedLine text = new CachedLine();

				text.getInput().moveTo((float) (bounds.x + bounds.w / 2), (float) (bounds.y + bounds.h / 2));
				text.getInput().setPointAttribute(iLinearGraphicsContext.text_v, element.getProperty(iVisualElement.name));
				text.getProperties().put(iLinearGraphicsContext.containsText, true);
				text.getProperties().put(iLinearGraphicsContext.color, new Vector4(0, 0, 0, 0.9f));
				text.getInput().setPointAttribute(iLinearGraphicsContext.font_v, new Font(Constants.defaultFont, Font.PLAIN, 14));// (float)
																			// (previousViewParameters.z
																			// *
																			// Math.min(bounds.h
																			// -
																			// 2,
																			// 14))));
				text.getInput().setPointAttribute(iLinearGraphicsContext.alignment_v, 0f);

				GLComponentWindow.currentContext.submitLine(text, text.getProperties());
			}

			String b = element.getProperty(iVisualElement.boundTo);

			if (b != null && b.trim().length() > 0) {
				CachedLine text = new CachedLine();

				text.getInput().moveTo((float) (bounds.x + bounds.w / 2), (float) (bounds.y + bounds.h / 2 + 14));
				text.getInput().setPointAttribute(iLinearGraphicsContext.text_v, "\"" + b + "\"");
				text.getProperties().put(iLinearGraphicsContext.containsText, true);
				text.getProperties().put(iLinearGraphicsContext.color, new Vector4(0, 0, 0, 0.9f));
				text.getInput().setPointAttribute(iLinearGraphicsContext.font_v, new Font(Constants.defaultFont, Font.BOLD, 10));// (float)
																			// (previousViewParameters.z
																			// *
																			// Math.min(bounds.h
																			// -
																			// 2,
																			// 10))));
				text.getInput().setPointAttribute(iLinearGraphicsContext.alignment_v, 0f);

				GLComponentWindow.currentContext.submitLine(text, text.getProperties());
			}

			/*
			 * 
			 * if ((!lastName.equals(r.get()) || d)) { if (label ==
			 * null) { label = TextSystem.textSystem.new
			 * RectangularLabel(r.get(), new
			 * Font(Constants.defaultFont, Font.PLAIN,
			 * 1).deriveFont((float) (previousViewParameters.z *
			 * Math.min(bounds.h - 2, 14))), 0); label.setFont(new
			 * Font(Constants.defaultFont, Font.PLAIN,
			 * 1).deriveFont((float) (previousViewParameters.z *
			 * Math.min(bounds.h - 2, 15))));
			 * 
			 * ((RectangularLabel)
			 * label).getTexture().use_gl_texture_rectangle_ext
			 * (false);
			 * 
			 * // label = TextSystem.textSystem.new //
			 * RectangularLabel(r.get(), new //
			 * Font(Constants.defaultFont, // Font.PLAIN,
			 * 1).deriveFont((float) // (1.0f * Math.min(bounds.h -
			 * 2, 14))), // 0); // label.setFont(new //
			 * Font(Constants.defaultFont, // Font.PLAIN,
			 * 1).deriveFont((float) // (1.0f * Math.min(bounds.h -
			 * 2, // 15))));
			 * 
			 * if (!Constants.invertedTextInCanvas)
			 * label.resetText(r.get(), 1, 1, 1, 0, 0, 0, 0, 1);
			 * else label.resetText(r.get(), 0, 0, 0, 0, 1, 1, 1,
			 * 1);
			 * 
			 * } else { label.dispose(); label =
			 * TextSystem.textSystem.new RectangularLabel(r.get(),
			 * new Font(Constants.defaultFont, Font.PLAIN,
			 * 1).deriveFont((float) (previousViewParameters.z *
			 * Math.min(bounds.h - 2, 14))), 0); label.setFont(new
			 * Font(Constants.defaultFont, Font.PLAIN,
			 * 1).deriveFont((float) (previousViewParameters.z *
			 * Math.min(bounds.h - 2, 15)))); ((RectangularLabel)
			 * label
			 * ).getTexture().use_gl_texture_rectangle_ext(false);
			 * 
			 * if (!Constants.invertedTextInCanvas)
			 * label.resetText(r.get(), 1, 1, 1, 0, 0, 0, 0, 1);
			 * else label.resetText(r.get(), 0, 0, 0, 0, 1, 1, 1,
			 * 1); } lastName = r.get(); if (label != null)
			 * ((RectangularLabel)
			 * label).drawIntoMeshScaled(labelTriangle, 1, 1, 1,
			 * opt.floatValue() * op.floatValue(), (float) bounds.x
			 * + (float) bounds.w / 2, (float) (bounds.y) + (float)
			 * bounds.h / 2, previousViewParameters.z);
			 * 
			 * }
			 */
			if (dirty) {
				dirty = false;

				Vector4 color1 = element.getProperty(iVisualElement.color1);
				if (color1 == null)
					color1 = new Vector4(1, 1, 1, 0.5f);
				else
					color1 = new Vector4(color1);

				Vector4 color2 = element.getProperty(iVisualElement.color2);
				if (color2 == null)
					color2 = new Vector4(0, 0, 0, 0.1f);
				else
					color2 = new Vector4(color2);

				color1.w *= op.floatValue();
				color2.w *= op.floatValue();

				if (selected) {
					color1 = new Vector4(color1.x - 0.4, color1.y - 0.4, color1.z - 0.4, color1.w - 0.1f);
					ComponentDrawingUtils.drawRectangle(triangle, line, point, (float) bounds.x, (float) bounds.y, (float) bounds.w, (float) bounds.h, color1, new Vector4(color2.x, color2.y, color2.z, 0.85f * op.floatValue()));
				} else {
					ComponentDrawingUtils.drawRectangle(triangle, line, point, (float) bounds.x, (float) bounds.y, (float) bounds.w, (float) bounds.h, color1, color2);
				}

				if (marked)
					ComponentDrawingUtils.drawRectangle(null, line, null, (float) bounds.x - 5, (float) bounds.y - 5, (float) bounds.w + 10, (float) bounds.h + 10, new Vector4(0.27f, 0.07f, 0.0f, 0.25f), new Vector4(0.25f, 0, 0, 0.5f));

				if (selected)
					points.setSize(1.5f);
				else
					points.setSize(0.5f);

				if (label != null)
					((RectangularLabel) label).drawIntoMeshScaled(labelTriangle, 1, 1, 1, opt.floatValue() * op.floatValue(), 0.5f + (int) (bounds.x + (float) bounds.w / 2), 0.5f + (int) ((bounds.y) + (float) bounds.h / 2), previousViewParameters.z);

			}

			if (selected)
				CoreHelpers.glLineWidth(6.5f);
			else if (marked)
				CoreHelpers.glLineWidth(5.5f);
			else
				CoreHelpers.glLineWidth(3.5f);

			triangles.performPass(null);
			lines.setWidth(2.5f);
			lines.performPass(null);
			if (!GLComponentWindow.doMultisampling && !CoreHelpers.isCore)
				points.performPass(null);

			/*
			 * if (label != null) { glActiveTexture(GL_TEXTURE1);
			 * label.on();
			 * 
			 * 
			 * if (!Constants.invertedTextInCanvas)
			 * glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			 * else glBlendFunc(GL_ONE, GL_ONE);
			 * 
			 * labelTriangles.performPass(null);
			 * glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			 * label.off(); glActiveTexture(GL_TEXTURE0); }
			 */

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

		}
		if (overridingInterface != null) {
			overridingInterface.paintNow(element, this.getBounds(), visible);
		}
	}

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

	public void setBounds(Rect r) {
		boolean dirty = false;
		if (r.x != bounds.x || r.w != bounds.w || r.h != bounds.h || r.y != bounds.y)
			this.setDirty();
		bounds.setValue(r);
	}

	public void setDirty() {
		this.dirty = true;
		if (getInside() != null)
			getInside().requestRedisplay();
	}

	public void setHidden(boolean hidden) {
		boolean dirty = this.hidden != hidden;
		this.hidden = hidden;
		if (dirty)
			this.setDirty();
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

			for (SelectionGroup g : getSelectionGroups()) {
				g.register(this);
				if (selected)
					g.addToSelection(this);
				else
					g.removeFromSelection(this);
			}
		}

	}

	public iComponent setVisualElement(iVisualElement element) {
		this.element = element;
		overridingInterface = new Dispatch<iVisualElement, iVisualElementOverrides>(iVisualElementOverrides.topology).getOverrideProxyFor(element, iVisualElementOverrides.class);
		this.setBounds(element.getFrame(new Rect(0, 0, 0, 0)));
		return this;
	}

	private ComponentContainer getInside() {
		if (inside != null)
			return inside;

		GLComponentWindow ec = iVisualElement.enclosingFrame.get(element);
		if (ec == null)
			return null;
		return ec.getRoot();
	}

	private boolean scaleHasChanged() {
		Vector4 nextViewParameters = new Vector4(0 * GLComponentWindow.getCurrentWindow(this).getXTranslation(), 0 * GLComponentWindow.getCurrentWindow(this).getYTranslation(), 1 / GLComponentWindow.getCurrentWindow(this).getXScale(), 1 / GLComponentWindow.getCurrentWindow(this).getYScale());

		float d = nextViewParameters.distanceFrom(previousViewParameters);
		previousViewParameters.set(nextViewParameters);
		return d > 1e-2;
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

	@CachedPerUpdate
	protected List<SelectionGroup<iComponent>> getMarkingGroups() {
		ArrayList<SelectionGroup<iComponent>> sel = new ArrayList<SelectionGroup<iComponent>>();
		Ref<SelectionGroup<iComponent>> out = new Ref<SelectionGroup<iComponent>>(null);
		overridingInterface.getProperty(element, iVisualElement.markingGroup, out);
		if (out.get() != null)
			sel.add(out.get());
		overridingInterface.getProperty(element, iVisualElement.markingGroup, out);
		return sel;
	}

	@CachedPerUpdate
	protected List<SelectionGroup<iComponent>> getSelectionGroups() {
		ArrayList<SelectionGroup<iComponent>> sel = new ArrayList<SelectionGroup<iComponent>>();
		Ref<SelectionGroup<iComponent>> out = new Ref<SelectionGroup<iComponent>>(null);
		overridingInterface.getProperty(element, iVisualElement.selectionGroup, out);
		if (out.get() != null)
			sel.add(out.get());
		overridingInterface.getProperty(element, iVisualElement.selectionGroup, out);
		return sel;
	}

	void setInside(ComponentContainer inside) {
		this.inside = inside;
	}

	private boolean isHidden() {
		return hidden;
	}

}
