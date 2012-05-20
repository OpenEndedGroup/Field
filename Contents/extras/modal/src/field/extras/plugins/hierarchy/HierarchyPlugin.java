package field.extras.plugins.hierarchy;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyObject;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.execution.PythonInterface;
import field.core.plugins.BaseSimplePlugin;
import field.core.plugins.drawing.MarqueeTool;
import field.core.plugins.drawing.ToolPalette2;
import field.core.plugins.drawing.ToolPalette2.iTool;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.help.HelpBrowser;
import field.core.plugins.python.PythonPlugin.CapturedEnvironment;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.components.GlassComponent;
import field.core.windowing.components.RootComponent;
import field.core.windowing.components.RootComponent.iMousePeer;
import field.core.windowing.components.RootComponent.iPaintPeer;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.abstraction.iProvider;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.linalg.Vector2;

/*
 * an example plugin that can manipulate the dispatch hierarchy of elements \u2014 this might end up as the modal plugin
 *
 *
 */
@Woven
public class HierarchyPlugin extends BaseSimplePlugin {

	public interface iEventHandler {
		public iEventHandler idle();

		public iEventHandler key(char character, int modifiers);

		public iEventHandler mouse(Vector2 at, int buttons);

		public iEventHandler transition(Mode from, Mode to, Vector2 at, int buttons);

		public void paintNow();
	}

	public enum Mode {
		hovering, down, drag, up, clicked;
	}

	static public final VisualElementProperty<HierarchyPlugin> hierarchyPlugin = new VisualElementProperty<HierarchyPlugin>("hierarchyPlugin");

	private iMousePeer mousePeer;
	private GlassComponent glassComponent;
	private MarqueeTool marqueeTool;

	private RootComponent rootComponent;

	private GLComponentWindow frame;
	
	private ToolPalette2 palette2;

	protected boolean marqueeToolOn = false;

	iEventHandler currentHandler;

	Mode currentMode = Mode.hovering;

	public iLinearGraphicsContext fastContext;

	public void addTool(String icon, final iEventHandler h) {
		addTool(icon, h, null, null, "unnamed modal mouse tool", "unnamed modal mouse tool");
	}

	public void addTool(final String icon, final iEventHandler h, final iUpdateable onSelect, final String toolTip, final String name, final String description) {
		// final Icon i = SmallMenu.makeIconFromCharacterShadowed(icon,
		// 40, 37, 0.5f, null, null);
		// final Icon i2 =
		// SmallMenu.makeIconFromCharacterShadowOnly(icon, 40, 37, 0.5f,
		// null, null);

		// final Icon i2 = SmallMenu.makeIconFromCharacterShadowed(icon,
		// 30, 23, 0.5f, null, null);
		// final Icon i =
		// SmallMenu.makeIconFromCharacterShadowOnly(icon, 30, 23, 0.5f,
		// null, null);

		if (palette2 != null) {
			final ToolItem a = palette2.addTool(new iTool() {

				public void begin() {
					if (onSelect != null)
						onSelect.update();
					// OverlayAnimationManager.requestRepaint(root);
					
					
					;//;//System.out.println(" setting tool to be <"+icon+"> / "+currentHandler);
					
					currentHandler = h;
				}

				public void end() {
					// OverlayAnimationManager.requestRepaint(root);
				}

				public Image getIcon() {
					Image ii = (new Image(Launcher.display, icon.replace("32", "16").replace("icons/", "icons/grey/")));
					return ii;
					// return (ImageIcon) i2;
				}

				public Image getSelectedIcon() {
					Image ii = (new Image(Launcher.display, icon.replace("32", "16").replace("icons/", "icons/grey/")));
					return ii;
					// return (ImageIcon) i;
				}

				public String getToolTip() {
					return toolTip;
				}

				public String getName() {
					return name;
				}

				public String getDescription() {
					return description;
				}

			});

			
			;//;//System.out.println(" -- adding contextual help for <"+icon+"> --"+a.getControl());
			
			HelpBrowser helpbrowser = HelpBrowser.helpBrowser.get(root);
			helpbrowser.getContextualHelp().addContextualHelpForToolItem(icon, a, new iProvider<String>() {

				@Override
				public String get() {
					return description;
				}
			}, 50);
			
			
		}
		
		
	}

	boolean isOneShot = false;

	private HierarchyHandler2 h2;

	public iEventHandler dictAsHandler(final PyDictionary d, final CapturedEnvironment env) {

		return new iEventHandler() {
			public iEventHandler idle() {
				PyObject o = d.get(Py.java2py("idle"));
				if (o == null || o == Py.None)
					return this;
				env.enter();
				try {
					PyObject m = o.__call__(d);
					if (m == null || m == Py.None)
						return null;
					if (m instanceof PyDictionary)
						return dictAsHandler((PyDictionary) m, env);
					Object e = m.__tojava__(iEventHandler.class);
					if (e instanceof iEventHandler)
						return ((iEventHandler) e);
					throw new ClassCastException(m + " " + e);
				} finally {
					env.exit();
				}
			}

			public void paintNow() {
			}

			public iEventHandler key(char character, int modifiers) {
				PyObject o = d.get(Py.java2py("key"));
				if (o == null || o == Py.None)
					return this;
				env.enter();
				try {
					PyObject m = o.__call__(d, Py.java2py(character), Py.java2py(modifiers));
					if (m == null || m == Py.None)
						return null;
					if (m instanceof PyDictionary)
						return dictAsHandler((PyDictionary) m, env);
					Object e = m.__tojava__(iEventHandler.class);
					if (e instanceof iEventHandler)
						return ((iEventHandler) e);
					throw new ClassCastException(m + " " + e);
				} finally {
					env.exit();
				}
			}

			public iEventHandler mouse(Vector2 at, int buttons) {
				PyObject o = d.get(Py.java2py("mouse"));
				if (o == null || o == Py.None)
					return this;
				env.enter();
				try {
					PyObject m = o.__call__(d, Py.java2py(at), Py.java2py(buttons));
					if (m == null || m == Py.None)
						return null;
					if (m instanceof PyDictionary)
						return dictAsHandler((PyDictionary) m, env);
					Object e = m.__tojava__(iEventHandler.class);
					if (e instanceof iEventHandler)
						return ((iEventHandler) e);
					throw new ClassCastException(m + " " + e);
				} finally {
					env.exit();
				}
			}

			public iEventHandler transition(Mode from, Mode to, Vector2 at, int buttons) {
				PyObject o = d.get(Py.java2py("transition"));
				if (o == null || o == Py.None)
					return this;
				env.enter();
				try {

					PyObject m;

					if (o instanceof PyDictionary) {
						if (((PyDictionary) o).has_key(Py.java2py(to.toString().intern()))) {
							o = o.__getitem__(Py.java2py(to.toString().intern()));
							m = o.__call__(new PyObject[] { d, Py.java2py(at), Py.java2py(buttons) });
						} else
							m = d;
					} else {

						m = o.__call__(new PyObject[] { d, Py.java2py(from.toString()), Py.java2py(to.toString()), Py.java2py(at), Py.java2py(buttons) });
					}
					if (m == null || m == Py.None)
						return null;
					if (m instanceof PyDictionary)
						return dictAsHandler((PyDictionary) m, env);
					Object e = m.__tojava__(iEventHandler.class);
					if (e instanceof iEventHandler)
						return ((iEventHandler) e);
					throw new ClassCastException(m + " " + e);
				} finally {
					env.exit();
				}
			}
		};
	}

	@Override
	public void registeredWith(iVisualElement root) {
		super.registeredWith(root);

		glassComponent = iVisualElement.glassComponent.get(root);
		rootComponent = iVisualElement.rootComponent.get(root);
		frame = iVisualElement.enclosingFrame.get(root);

		// fastContext = iVisualElement.fastContext.get(root);

		iVisualElement.rootComponent.get(root).addPaintPeer(new iPaintPeer() {

			public void paint(RootComponent inside) {
				fastContext = GLComponentWindow.currentContext;
				paintNow();
			}
		});

		;//;//System.out.println(" fastContext is <" + fastContext + ">");

		rootComponent.addMousePeer(new iMousePeer() {

			@Override
			public void mouseReleased(ComponentContainer inside, Event arg0) {
			}

			@Override
			public void mousePressed(ComponentContainer inside, Event arg0) {
			}

			@Override
			public void mouseMoved(ComponentContainer inside, Event arg0) {
			}

			@Override
			public void mouseExited(ComponentContainer inside, Event arg0) {
			}

			@Override
			public void mouseEntered(ComponentContainer inside, Event arg0) {
			}

			@Override
			public void mouseDragged(ComponentContainer inside, Event arg0) {
			}

			@Override
			public void mouseClicked(ComponentContainer inside, Event arg0) {
			}

			@Override
			public void keyTyped(ComponentContainer inside, Event arg0) {
			}

			@Override
			public void keyReleased(ComponentContainer inside, Event arg0) {
				if (arg0.keyCode == 'd')
					palette2.setTool(0);
				if(arg0.keyCode == 'm')
					palette2.setTool(0);
				if(arg0.keyCode == 't')
					palette2.setTool(0);
				if(arg0.keyCode == '3')
					palette2.setTool(0);

			}

			@Override
			public void keyPressed(ComponentContainer inside, Event arg0) {

				if (arg0.keyCode == 'd')
					if (palette2.getTool() != 2)
					{
						palette2.setTool(2);
						forceFirstMouseEvent();
					}

				if (arg0.keyCode == 'm')
					if (palette2.getTool() != 1)
					{
						palette2.setTool(1);
						forceFirstMouseEvent();
					}

				if (arg0.keyCode == 't')
					if (palette2.getTool() != 3)
					{
						palette2.setTool(3);
						forceFirstMouseEvent();
					}
				
				if (arg0.keyCode == '3')
					if (palette2.getTool() != 5)
					{
						palette2.setTool(5);
						forceFirstMouseEvent();
					}
			}

			private void forceFirstMouseEvent() {
				
				Vector2 c = GLComponentWindow.getCurrentWindow(null).getCurrentMouseInWindowCoordinates();
				c.y+=20;
				currentHandler = currentHandler.mouse(c, 0);
				
			}
		}, false);

		mousePeer = new iMousePeer() {

			Vector2 pressedAt = new Vector2();

			public void keyPressed(ComponentContainer inside, Event arg0) {
				// if (currentHandler != null)
				// currentHandler =
				// currentHandler.key(arg0.character,
				// arg0.stateMask);
				// if (arg0.keyCode == SWT.ESC) {
				// escape(arg0);
				// }

			}

			public void keyReleased(ComponentContainer inside, Event arg0) {

			}

			public void keyTyped(ComponentContainer inside, Event arg0) {
			}

			public void mouseClicked(ComponentContainer inside, Event arg0) {
			}

			public void mouseDragged(ComponentContainer inside, Event arg0) {

				;//;//System.out.println(" inside mouse drag <" + currentMode + " " + currentHandler);

				if (currentMode != Mode.drag)
					if (currentHandler != null)
						currentHandler = currentHandler.transition(currentMode, Mode.drag, new Vector2(arg0.x, arg0.y), arg0.button);
				currentMode = Mode.drag;
				if (currentHandler != null)
					currentHandler = currentHandler.mouse(new Vector2(arg0.x, arg0.y), arg0.button);
			}

			public void mouseEntered(ComponentContainer inside, Event arg0) {
			}

			public void mouseExited(ComponentContainer inside, Event arg0) {
			}

			public void mouseMoved(ComponentContainer inside, Event arg0) {
				if (currentMode == Mode.up)
					if (currentHandler != null)
						currentHandler = currentHandler.transition(currentMode, Mode.hovering, new Vector2(arg0.x, arg0.y), 0);
				currentMode = Mode.hovering;
				if (currentHandler != null)
					currentHandler = currentHandler.mouse(new Vector2(arg0.x, arg0.y), 0);
			}

			public void mousePressed(ComponentContainer inside, Event arg0) {
				pressedAt = new Vector2(arg0.x, arg0.y);
				if (currentHandler != null)
					currentHandler = currentHandler.transition(currentMode, Mode.down, new Vector2(arg0.x, arg0.y), arg0.button);
				currentMode = Mode.down;
			}

			public void mouseReleased(ComponentContainer inside, Event arg0) {
				
				;//;//System.out.println(" palette2 released : isOneShot "+isOneShot+" "+palette2.getTool());
				
				Vector2 a = new Vector2(arg0.x, arg0.y);
				if (a.distanceFrom(pressedAt) < 2) {
					if (currentHandler != null)
						currentHandler = currentHandler.transition(currentMode, Mode.clicked, new Vector2(arg0.x, arg0.y), arg0.button);
					currentMode = Mode.clicked;
				} else if (currentHandler != null)
					currentHandler = currentHandler.transition(currentMode, Mode.up, new Vector2(arg0.x, arg0.y), arg0.button);
				currentMode = Mode.up;

				if (isOneShot) {
					isOneShot = false;
					if (palette2 != null)
						palette2.setTool(0);
				}
			}
		};

		root.setProperty(hierarchyPlugin, this);

		// PythonInterface.getPythonInterface().execString("_down
		// = \"down\"\n" + "_up = \"up\"\n" + "\n" +
		// "_transition = \"transition\"\n" + "_mouse =
		// \"mouse\"\n" + "_key= \"key\"\n");
		PythonInterface.getPythonInterface().setVariable("_down", "down");
		PythonInterface.getPythonInterface().setVariable("_transition", "transition");
		PythonInterface.getPythonInterface().setVariable("_mouse", "mouse");
		PythonInterface.getPythonInterface().setVariable("_key", "key");

		// basic drawing tools
		palette2 = iVisualElement.toolPalette2.get(root);
		marqueeTool = new MarqueeTool(root);

		addTool("icons/cursor_16x16.png", null, null, "Normal mouse processing", "Normal mouse", "'Normal' mouse processing\n" + 
				"=========================\n" + 
				"\n" + 
				"This is the default mouse mode:\n" + 
				"\n" + 
				" + Select, drag & resize elements around with the *left mouse button*; \n" + 
				" + Invoke the contextual menu with the *right mouse button*. \n" + 
				" + Drag the canvas itself with the *middle mouse button* (or laptop drag gesture) to pan around; \n" + 
				" + *Shift-middle-mouse* button scales the canvas (*F1* resets the view if you get lost). ");
		new MarqueeTool2(this, root);
		// new HierarchyHandler(root).install(this);
		h2 = new HierarchyHandler2(root);
		h2.install(this);

		new FreehandTool().registeredWith(root);
		new FreehandTool3d().registeredWith(root);

	}

	public void setCurrentHandler(iEventHandler h) {
		currentHandler = h;
	}

	public void oneShootTool(int i) {
		isOneShot = true;
		if (palette2 != null)
			palette2.setTool(i);
	}

	public void paintNow() {
		// fastContext = GLComponentWindow.fastContext;
		if (currentHandler != null) {
			currentHandler.paintNow();
		}
	}

	@Override
	public void update() {

		if (glassComponent == null)
			return;

		if (currentHandler == null)
			glassComponent.removeMousePeer(mousePeer);
		else
			glassComponent.addMousePeer(mousePeer);

		super.update();

		if (currentHandler != null)
			currentHandler.idle();

		// ;//;//System.out.println(" current mode <" +
		// currentMode + ">
		// handler <" + currentHandler + ">");
	}

	protected void escape(Event arg0) {
		// sets the tool pallete back to the original
		palette2.setTool(0);
	}

	@Override
	protected DefaultOverride newVisualElementOverrides() {
		return new DefaultOverride() {
			@Override
			public VisitCode added(iVisualElement newSource) {
				topologyChanged();
				return super.added(newSource);
			}

			@Override
			public VisitCode deleted(iVisualElement source) {
				topologyChanged();
				return super.deleted(source);
			}
		};
	}

	int topMod = 0;
	int lastTopMod = 0;

	protected void topologyChanged() {
		topMod++;
		fireTopologyChanged();
	}

	@NextUpdate
	protected void fireTopologyChanged() {
		if (lastTopMod == topMod)
			return;
		try {
			h2.topologyChanged();
		} finally {
			lastTopMod = topMod;
		}
	}

	@Override
	protected String getPluginNameImpl() {
		return "//plugin_hierarchy";
	}

}
