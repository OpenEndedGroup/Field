package field.extras.scrubber;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import field.bytecode.protect.Trampoline2;
import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.execution.DisposableTimeSliderOverrides;
import field.core.execution.TemporalSliderOverrides;
import field.core.persistance.VisualElementReference;
import field.core.plugins.BaseSimplePlugin;
import field.core.plugins.drawing.SplineComputingOverride;
import field.core.ui.ExtendedMenuMap;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.components.GlassComponent;
import field.core.windowing.components.PlainDraggableComponent;
import field.core.windowing.components.RootComponent.iMousePeer;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.iComponent;
import field.core.windowing.overlay.OverlayAnimationManager;
import field.launch.Launcher;
import field.launch.SystemProperties;
import field.launch.iUpdateable;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.Generics.Pair;
import field.namespace.generic.Generics.Triple;
import field.util.TaskQueue;

/**
 * a plugin for scrubbing timelines with devices (in this case, the Connexion
 * SpaceBNavigator
 * 
 * @author marc
 * 
 */
public class ScrubberPlugin extends BaseSimplePlugin {

	List<Connection> connections = new ArrayList<Connection>();

	static public abstract class Connection {
		boolean enabled;
		String name;
		VisualElementReference outputTo;

		abstract public void update(iVisualElement root);

		public void addMenuItems(Map<String, iUpdateable> to) {
		}
	}

	List<Pair<String, iFunction<Connection, iVisualElement>>> factory = new ArrayList<Pair<String, iFunction<Connection, iVisualElement>>>();

	public class ScrubberOverrides extends DefaultOverride {
		@Override
		public VisitCode menuItemsFor(final iVisualElement source, Map<String, iUpdateable> items) {

			boolean section = false;
			if (source != null) {
				iVisualElementOverrides o = source.getProperty(iVisualElement.overrides);
				if (o instanceof TemporalSliderOverrides || o instanceof DisposableTimeSliderOverrides && false) {

					if (items instanceof ExtendedMenuMap)
						items = ((ExtendedMenuMap) items).getBuilder().newMenu("Scrubber", "NE2").getMap();

					if (factory.size() > 0) {
						items.put("Scrubber", null);
						section = true;
						for (final Pair<String, iFunction<Connection, iVisualElement>> f : factory) {
							items.put(f.left, new iUpdateable() {
								public void update() {
									connections.add(f.right.f(source));
								}
							});
						}
					}
					for (final Connection c : connections) {
						if (c.outputTo == source) {
							if (!section) {
								section = true;
								items.put("Scrubber", null);
							}
							items.put(" remove <b>" + c.name + "</b>", new iUpdateable() {

								public void update() {
									connections.remove(c);
								}
							});
							if (c.enabled)
								items.put(" disable <b>" + c.name + "</b>", new iUpdateable() {

									public void update() {
										connections.remove(c);
									}
								});
							else
								items.put(" enable <b>" + c.name + "</b>", new iUpdateable() {

									public void update() {
										c.enabled = true;
									}
								});
						}
					}
				}
			} else {
//				items.put("Scrubber", null);
//				items.put(" \u21ad <b>New time warp group</b> here <i>(experimental)</i> ", new iUpdateable() {
//
//					public void update() {
//
//						GLComponentWindow frame = iVisualElement.enclosingFrame.get(root);
//
//						Rect bounds = new Rect(30, 30, 50, 50);
//						if (frame != null) {
//							bounds.x = frame.getCurrentMousePosition().x;
//							bounds.y = frame.getCurrentMousePosition().y;
//						}
//
//						Triple<VisualElement, PlainDraggableComponent, TimeWarpGroup> c = VisualElement.createAddAndName(bounds, root, "untitled warp group", VisualElement.class, PlainDraggableComponent.class, TimeWarpGroup.class, null);
//
//						c.left.setProperty(SplineComputingOverride.computed_drawingInstructions, new ArrayList<iUpdateable>(Collections.singletonList(c.right)));
//
//					}
//				});
			}

			return VisitCode.cont;
		}

		int once = 0;

		@Override
		public VisitCode handleKeyboardEvent(iVisualElement newSource, Event event) {
			if (event == null)
				return super.handleKeyboardEvent(newSource, event);

			if (!event.doit)
				return super.handleKeyboardEvent(newSource, event);

			if (event.type == SWT.KeyDown && (GLComponentWindow.keysDown.contains((int) 'k') || GLComponentWindow.keysDown.contains((int) 's'))) {
				if (!installed)
					install();

				relative = GLComponentWindow.keysDown.contains((int) 'k');
			} else if (event.type == SWT.KeyUp && (GLComponentWindow.keysDown.contains((int) 'k') || GLComponentWindow.keysDown.contains((int) 's'))) {
				if (installed)
					uninstall();

			} else if (event.type == SWT.KeyDown && event.character == 'l' && once < tick) {
				if (!local)
					installLocal();
				else {
					;//;//System.out.println(" deleting local");
					uninstallLocal();
				}
				once = tick;
			} else if (event.type == SWT.KeyDown && event.character == 'L' && once < tick) {
				if (!local)
					installLocal();
				else {
					;//;//System.out.println(" pausing local");
					stopLocal();
				}
				once = tick;
			} else if (event.type == SWT.KeyDown && event.character == '[' && once < tick) {
				if (local) {
					((DisposableTimeSliderOverrides) localwas.getProperty(iVisualElement.overrides)).skipBack();
				}
				once = tick;
			} else if (event.type == SWT.KeyDown && event.character == ']' && once < tick) {
				if (local) {
					((DisposableTimeSliderOverrides) localwas.getProperty(iVisualElement.overrides)).skipForward();
				}
				once = tick;
			} else if (event.type == SWT.KeyDown && bookmarks.contains(event.keyCode) && once < tick) {

				once = tick;

				if ((event.stateMask & SWT.SHIFT) != 0) {
					Vector4 v = new Vector4(GLComponentWindow.getCurrentWindow(null).getXTranslation(), GLComponentWindow.getCurrentWindow(null).getYTranslation(), GLComponentWindow.getCurrentWindow(null).getXScale(), GLComponentWindow.getCurrentWindow(null).getYScale());
					bookmarkedAs.set(bookmarks.indexOf(event.keyCode), v);
					OverlayAnimationManager.notifyAsText(root, "Bookmarked " + event.character, new Rect(0, 0, 0, 0));
				} else {
					Vector4 v = bookmarkedAs.get(bookmarks.indexOf(event.keyCode));

					GLComponentWindow.getCurrentWindow(null).setXTranslation(v.x);
					GLComponentWindow.getCurrentWindow(null).setYTranslation(v.y);
					GLComponentWindow.getCurrentWindow(null).setXScale(v.z);
					GLComponentWindow.getCurrentWindow(null).setYScale(v.w);
				}
			}

			return super.handleKeyboardEvent(newSource, event);
		}

	}

	List<Integer> bookmarks = Arrays.asList(new Integer[] { SWT.KEYPAD_1, SWT.KEYPAD_2, SWT.KEYPAD_3 });
	List<Vector4> bookmarkedAs = Arrays.asList(new Vector4[] { new Vector4(0, 0, 1, 1), new Vector4(0, 0, 1, 1), new Vector4(0, 0, 1, 1) });

	int tick = 0;

	boolean installed = false;
	boolean local = false;
	boolean relative = false;

	iMousePeer peer = new iMousePeer() {

		public void keyPressed(ComponentContainer inside, Event arg0) {
		}

		public void keyReleased(ComponentContainer inside, Event event) {
			if ((event.keyCode == 's' || event.keyCode == 'k') && event.type == SWT.KeyUp) {
				;//;//System.out.println(" deinstalling ");
				if (installed)
					uninstall();
			}
		}

		public void keyTyped(ComponentContainer inside, Event arg0) {
		}

		public void mouseClicked(ComponentContainer inside, Event arg0) {
			Vector2 pos = GLComponentWindow.mousePositionForEvent(arg0);
			mouseTo(pos, false);
		}

		public void mouseDragged(ComponentContainer inside, Event arg0) {

			Vector2 pos = GLComponentWindow.mousePositionForEvent(arg0);
			;//;//System.out.println(" moving <" + pos + ">");
			mouseTo(pos, true);
		}

		public void mouseEntered(ComponentContainer inside, Event arg0) {
		}

		public void mouseExited(ComponentContainer inside, Event arg0) {
		}

		public void mouseMoved(ComponentContainer inside, Event arg0) {
		}

		public void mousePressed(ComponentContainer inside, Event arg0) {
			Vector2 pos = GLComponentWindow.mousePositionForEvent(arg0);
			mouseTo(pos, false);
		}

		public void mouseReleased(ComponentContainer inside, Event arg0) {
		}
	};

	private Vector2 mouseAt;
	private iVisualElement localwas;

	protected void install() {
		GlassComponent glass = iVisualElement.glassComponent.get(root);
		glass.addMousePeer(peer);
		installed = true;

		GLComponentWindow.getCurrentWindow(null).extraTextStatusbarDescriptions.put(this + "k", "<b>(dragging time slider)</b>");

	}

	protected void uninstall() {
		;//;//System.out.println(" uninstalling ");

		GlassComponent glass = iVisualElement.glassComponent.get(root);
		glass.removeMousePeer(peer);
		installed = false;

		GLComponentWindow.getCurrentWindow(null).extraTextStatusbarDescriptions.remove(this + "k");
		GLComponentWindow.getCurrentWindow(null).extraTextStatusbarDescriptions.remove(this);

	}

	public void uninstallLocal() {
		if (localwas != null) {
			GLComponentWindow.getCurrentWindow(null).extraTextStatusbarDescriptions.remove(this + "k");

			// todo, need to stop everything
			// VisualElement.delete(localwas);
			((DisposableTimeSliderOverrides) iVisualElement.overrides.get(localwas)).stopAndDelete();

			local = false;
			GLComponentWindow.getCurrentWindow(null).extraTextStatusbarDescriptions.remove(this);
			localwas = null;
		}
	}

	public void stopLocal() {
		if (localwas != null) {
			GLComponentWindow.getCurrentWindow(null).extraTextStatusbarDescriptions.remove(this + "k");

			// todo, need to stop everything
			// VisualElement.delete(localwas);
			((DisposableTimeSliderOverrides) iVisualElement.overrides.get(localwas)).stop();

			local = false;
			GLComponentWindow.getCurrentWindow(null).extraTextStatusbarDescriptions.remove(this);

		}
	}

	public void installLocal() {

		GLComponentWindow.getCurrentWindow(null).extraTextStatusbarDescriptions.put(this, "<b>(local time slider)</b>");

		SelectionGroup<iComponent> components = iVisualElement.selectionGroup.get(root);
		LinkedHashSet ve = new LinkedHashSet<iVisualElement>();
		for (iComponent c : components.getSelection())
			if (c.getVisualElement() != null)
				ve.add(c.getVisualElement());

		if (ve.size() > 0) {
			Vector2 pos = GLComponentWindow.getCurrentWindow(null).getCurrentMouseInWindowCoordinates();

			localwas = DisposableTimeSliderOverrides.createDTSO2(new ArrayList<iVisualElement>(ve), root, pos.x);
			local = true;
			components.deselectAll();
			localwas.getProperty(iVisualElement.localView).setSelected(true);
		}

	}

	protected void mouseTo(Vector2 pos, boolean b) {

		if (relative) {
			if (!b) {
				mouseAt = pos;
			}
		}

		if (relative && mouseAt == null)
			return;

		// / first mode, move default temporal slider to this point
		iVisualElement target = localwas != null ? localwas : iVisualElement.timeSlider.get(root);

		Rect oldFrame = target.getFrame(null);
		Rect newFrame = new Rect(oldFrame);
		if (relative) {
			newFrame.x += (pos.x - mouseAt.x) / 10.0;
		} else {
			newFrame.x = pos.x;
		}

		iVisualElement old = iVisualElementOverrides.topology.setAt(target);
		iVisualElementOverrides.forward.shouldChangeFrame.shouldChangeFrame(target, newFrame, oldFrame, true);
		target.setProperty(iVisualElement.dirty, true);
		iVisualElementOverrides.topology.setAt(old);

		mouseAt = pos;
	}

	@Override
	public void registeredWith(iVisualElement root) {
		super.registeredWith(root);
		String devices = SystemProperties.getProperty("scrubber.devices", null);
		;//;//System.out.println(" property is <" + devices + ">");
		if (devices == null)
			return;
		String[] dev = devices.split(":");
		for (String d : dev) {
			try {
				Class c = Trampoline2.trampoline.getClassLoader().loadClass(d);
				Constructor cc = c.getConstructor(ScrubberPlugin.class);
				cc.newInstance(this);
			} catch (Exception e) {
				System.err.println(" non-fatal exception thrown setting up device <" + d + ">");
				e.printStackTrace();
				Launcher.getLauncher().handle(e);
			}
		}
	}

	TaskQueue u = new TaskQueue();

	@Override
	public void update() {
		super.update();
		u.update();
		for (Connection c : connections) {
			c.update(root);
		}

		tick++;
	}

	public void addFactory(String displayedName, iFunction<Connection, iVisualElement> factory) {
		this.factory.add(new Pair<String, iFunction<Connection, iVisualElement>>(displayedName, factory));
	}

	@Override
	protected String getPluginNameImpl() {
		return "scrubber";
	}

	@Override
	protected DefaultOverride newVisualElementOverrides() {
		return new ScrubberOverrides();
	}

	@Override
	public Object getPersistanceInformation() {
		return connections;
	}
}
