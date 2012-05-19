package field.core.plugins.drawing;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import field.core.Platform;
import field.core.Platform.OS;
import field.core.StandardFluidSheet;
import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.execution.PythonInterface;
import field.core.plugins.PluginList;
import field.core.plugins.iPlugin;
import field.core.plugins.constrain.ComplexConstraints;
import field.core.plugins.drawing.opengl.BaseGLGraphicsContext;
import field.core.plugins.drawing.opengl.LineInteraction;
import field.core.plugins.drawing.opengl.SimpleLineDrawing;
import field.core.plugins.drawing.opengl.SimpleTextDrawing;
import field.core.plugins.drawing.opengl.SimpleWebpageDrawing;
import field.core.plugins.selection.SelectionSetDriver;
import field.core.plugins.selection.SelectionSetDriver.SavedView;
import field.core.plugins.selection.SelectionSetDriver.SelectionSet;
import field.core.plugins.selection.ToolBarFolder;
import field.core.plugins.snip.SnippetsPlugin;
import field.core.ui.NewInspector2;
import field.core.util.PythonCallableMap;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.DraggableComponent.Resize;
import field.core.windowing.components.GlassComponent;
import field.core.windowing.components.PlainDraggableComponent;
import field.core.windowing.components.RootComponent;
import field.core.windowing.components.RootComponent.iPaintPeer;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.SelectionGroup.iSelectionChanged;
import field.core.windowing.components.iComponent;
import field.launch.iUpdateable;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.graph.NodeImpl;
import field.math.graph.iMutableContainer;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.Generics.Pair;
import field.namespace.generic.Generics.Triple;
import field.namespace.generic.ReflectionTools;

public class BasicDrawingPlugin implements iPlugin {
    
	static public final VisualElementProperty<PythonCallableMap> selectionStateCallback = new VisualElementProperty<PythonCallableMap>("selectionStateCallback_");
	static public final VisualElementProperty<Number> allwaysConstrain = new VisualElementProperty<Number>("allwaysConstrain");
    
	static {
		SnippetsPlugin.addURLHandler(new iFunction<Boolean, Pair<URL, SnippetsPlugin>>() {
            
			public Boolean f(Pair<URL, SnippetsPlugin> in) {
				boolean s = in.left.getPath().endsWith(".svg");
				if (s) {
					String forms = in.left.getPath() + "\n" + "lines = [PLine(x) for x in SVGImport(File(\"" + in.left.getPath() + "\")).lines(None, None)]";
					in.right.addText(forms, "svg file", new String[] { "filename", "load & extract PLines" }, "operation");
					return true;
				} else
					return false;
			}
		});
        
		String[] images = { ".jpg", ".tif", ".tiff", ".psd", ".png" };
		for (final String i : images) {
            
			SnippetsPlugin.addURLHandler(new iFunction<Boolean, Pair<URL, SnippetsPlugin>>() {
                
				public Boolean f(Pair<URL, SnippetsPlugin> in) {
					boolean s = in.left.getPath().endsWith(i);
					if (s) {
						String forms = in.left.getPath() + "\n" + "ii = image(\"" + in.left.toExternalForm() + "\")";
						in.right.addText(forms, "image file", new String[] { "filename", "load" }, "operation");
						return true;
					} else
						return false;
				}
			});
		}
	}
    
	static public class FrameManipulation {
		public Rect originalFrame;
        
		Set<Resize> resizeType;
        
		int modifersDown;
        
		public FrameManipulation(Set<Resize> resizeType, Rect originalFrame) {
			super();
			this.resizeType = resizeType;
			this.originalFrame = originalFrame;
		}
        
		public FrameManipulation(Set<Resize> resizeType, Rect originalFrame, int modifiersDown) {
			super();
			this.resizeType = resizeType;
			this.originalFrame = originalFrame;
			this.modifersDown = modifiersDown;
		}
        
		@Override
		public String toString() {
			return "fm:" + resizeType + " " + originalFrame;
		}
        
	}
    
	public interface iDragParticipant {
		public void beginDrag(Set<Resize> resizeType, iVisualElement element, Rect originalRect, int modifiers);
        
		public void endDrag(Set<Resize> reseizeType, iVisualElement element, Rect inOutRect, boolean createConstraint, int modifiers);
        
		public void interpretRect(iVisualElement element, Rect originalRect, Rect currentRect);
        
		public boolean needsRepainting();
        
		public void stopAll();
	}
    
	public class LocalVisualElement extends NodeImpl<iVisualElement> implements iVisualElement {
        
		public <T> void deleteProperty(VisualElementProperty<T> p) {
		}
        
		public void dispose() {
		}
        
		public Rect getFrame(Rect out) {
			return null;
		}
        
		public <T> T getProperty(iVisualElement.VisualElementProperty<T> p) {
			if (p == overrides)
				return (T) elementOverride;
			Object o = properties.get(p);
			return (T) o;
		}
        
		public String getUniqueID() {
			return pluginId;
		}
        
		public Map<Object, Object> payload() {
			return properties;
		}
        
		public void setFrame(Rect out) {
		}
        
		public iMutableContainer<Map<Object, Object>, iVisualElement> setPayload(Map<Object, Object> t) {
			properties = t;
			return this;
		}
        
		public <T> iVisualElement setProperty(iVisualElement.VisualElementProperty<T> p, T to) {
			properties.put(p, to);
			return this;
		}
        
		public void setUniqueID(String uid) {
		}
	}
    
	public class Overrides extends iVisualElementOverrides.DefaultOverride {
		@Override
		public VisitCode deleted(iVisualElement source) {
            
			return VisitCode.cont;
		}
        
		@Override
		public VisitCode handleKeyboardEvent(iVisualElement newSource, Event event) {
            
			// TODO swt
            
			if (event == null)
				return VisitCode.cont;
			if (event.character == '0' && event.type == SWT.KeyDown && event.doit) {
				if (tick) {
					tick = false;
					GLComponentWindow frame = iVisualElement.enclosingFrame.get(root);
					frame.disableRepaintNow();
				}
			}
			//
			// if (event.getKeyCode() == KeyEvent.VK_CLEAR &&
			// event.getID() == KeyEvent.KEY_PRESSED) {
			// if (tick) {
			// tick = false;
			// System.out.println(" disabling repaint ");
			// frame.toggleContinuousRepaintNow();
			// }
			// }
			if (event.keyCode == SWT.F2 && event.type == SWT.KeyDown) {
				if (tick) {
                    
					System.out.println(" toggling presentation mode");
					if (GLComponentWindow.present) {
						GLComponentWindow.present = false;
						GLComponentWindow.draft = true;
						installedContext.draft = true;
					} else {
						GLComponentWindow.present = true;
						GLComponentWindow.draft = false;
						installedContext.draft = false;
					}
                    
					rootComponent.repaint();
					tick = false;
				}
                
			}
			return VisitCode.cont;
			//
			// if (event.getKeyCode() == KeyEvent.VK_F3) {
			// if (tick) {
			// SelectionGroup<iComponent> selectionGroup =
			// iVisualElement.selectionGroup.get(root);
			// selectionGroup.deselectAll();
			//
			// tick = false;
			// }
			// }
			// if (event.getKeyCode() == KeyEvent.VK_F4) {
			// if (tick) {
			// MainSelectionGroup selectionGroup =
			// (MainSelectionGroup)
			// iVisualElement.selectionGroup.get(root);
			// selectionGroup.popSelection();
			//
			// tick = false;
			// }
			// }
			//
			// return super.handleKeyboardEvent(newSource, event);
		}
        
		@Override
		public VisitCode menuItemsFor(iVisualElement source, Map<String, iUpdateable> items) {
            
			items.put("Drawing", null);
			items.put(" \u301c\tCreate a <b>new spline drawer</b> here ///P///", new iUpdateable() {
				private GLComponentWindow frame;
                
				public void update() {
                    
					frame = iVisualElement.enclosingFrame.get(root);
                    
					Rect bounds = new Rect(30, 30, 50, 50);
					if (frame != null) {
						bounds.x = frame.getCurrentMousePosition().x;
						bounds.y = frame.getCurrentMousePosition().y;
					}
                    
					Triple<VisualElement, PlainDraggableComponent, SplineComputingOverride> created = VisualElement.createAddAndName(bounds, root, "untitled spline computation", VisualElement.class, PlainDraggableComponent.class, SplineComputingOverride.class, null);
				}
			});
            
			items.put(" \u301c\tCreate new <b>3d</b> spline drawer here ", new iUpdateable() {
				private GLComponentWindow frame;
                
				public void update() {
                    
					frame = iVisualElement.enclosingFrame.get(root);
                    
					Rect bounds = new Rect(30, 30, 500, 500);
					if (frame != null) {
						bounds.x = frame.getCurrentMousePosition().x;
						bounds.y = frame.getCurrentMousePosition().y;
					}
                    
					Triple<VisualElement, PlainDraggableComponent, ThreedComputingOverride> created = VisualElement.createAddAndName(bounds, root, "untitled spline computation", VisualElement.class, PlainDraggableComponent.class, ThreedComputingOverride.class, null);
				}
			});
            
			return super.menuItemsFor(source, items);
		}
        
		@Override
		public <T> VisitCode setProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> to) {
			if (prop == frameManipulationBegin) {
                
				if ((Platform.getOS() == OS.mac && Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK)) || shouldConstrain(source))
					dragParticipants_list.beginDrag(((FrameManipulation) to.get()).resizeType, source, ((FrameManipulation) to.get()).originalFrame, ((FrameManipulation) to.get()).modifersDown);
			} else if (prop == frameManipulationEnd) {
                
				boolean shouldCreateConstraint = (((FrameManipulation) to.get()).modifersDown) == 256;
				if (Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK) || shouldConstrain(source)) {
					System.out.println(" caps lock down ");
					dragParticipants_list.endDrag(((FrameManipulation) to.get()).resizeType, source, ((FrameManipulation) to.get()).originalFrame, shouldCreateConstraint, ((FrameManipulation) to.get()).modifersDown);
				}
                
				dragParticipants_list.stopAll();
                
			} else if (prop.equals(iVisualElement.hasFocusLock)) {
				// TODO swt lock selection
				// Object x = ((Ref<Object>) to).get();
				// if (x != null && ((x instanceof Number &&
				// ((Number) x).intValue() > 0) || (x instanceof
				// Boolean && ((Boolean) x).booleanValue()))) {
				// selectionSets.lockSelection();
				// } else {
				// selectionSets.unlockSelection();
				// }
			}
			return super.setProperty(source, prop, to);
		}
        
		@Override
		public VisitCode shouldChangeFrame(iVisualElement source, Rect newFrame, Rect oldFrame, boolean now) {
            
			dragParticipants_list.interpretRect(source, oldFrame, newFrame);
            
			return VisitCode.cont;
		}
	}
    
	static public final String pluginId = "//plugin_basicDrawingPlugin";
    
	public static final VisualElementProperty<BasicDrawingPlugin> simpleConstraints_plugin = new VisualElementProperty<BasicDrawingPlugin>("plugin_basicDrawingPlugin");
    
	static public final VisualElementProperty<FrameManipulation> frameManipulationBegin = new VisualElementProperty<FrameManipulation>("frameManipulationBegin_");
    
	static public final VisualElementProperty<FrameManipulation> frameManipulationEnd = new VisualElementProperty<FrameManipulation>("frameManipulationEnd_");
    
	private final LocalVisualElement lve;
    
	private iVisualElement root;
    
	private SelectionGroup<iComponent> group;
    
	private SelectionSetDriver selectionSets;
    
	private ComplexConstraints complexConstraints;
    
	private BaseGLGraphicsContext installedContext;
    
	public BaseGLGraphicsContext getInstalledContext() {
		return installedContext;
	};
    
	public boolean shouldConstrain(iVisualElement source) {
		Number m = allwaysConstrain.get(source);
		return m == null ? false : m.floatValue() > 0;
	}
    
	private RootComponent rootComponent;
    
	private SimpleLineDrawing sld;
    
	private BaseGLGraphicsContext installedFastContext;
    
	private SimpleLineDrawing sldFast;
    
	List<iDragParticipant> dragParticipants = new ArrayList<iDragParticipant>();
    
	iDragParticipant dragParticipants_list = ReflectionTools.listProxy(dragParticipants, iDragParticipant.class);
    
	iVisualElementOverrides elementOverride;
    
	Map<Object, Object> properties = new HashMap<Object, Object>();
    
	boolean tick = false;
    
	public BasicDrawingPlugin() {
		lve = new LocalVisualElement();
        
	}
    
	public void close() {
	}
    
	public Object getPersistanceInformation() {
		// return new Pair<String, Set<SelectionSet>>(pluginId +
		// "version_1", selectionSets.getSavedSelectionSets());
		if (selectionSets != null)
			return new Triple<String, Set<SelectionSet>, Set<SavedView>>(pluginId + "version_2", selectionSets.getSavedSelectionSets(), selectionSets.getSavedViews());
		else
			return new Triple<String, Set<SelectionSet>, Set<SavedView>>(pluginId + "version_2", new HashSet<SelectionSet>(), new HashSet<SavedView>());
	}
    
	public SelectionSetDriver getSelectionSetDriver() {
		return selectionSets;
	}
    
	public iVisualElement getWellKnownVisualElement(String id) {
		if (id.equals(pluginId))
			return lve;
		return null;
	}
    
	public void registeredWith(final iVisualElement root) {
        
		PythonInterface.getPythonInterface().execString("from field.core.plugins.drawing.opengl import CachedLine");
		PythonInterface.getPythonInterface().execString("from field.core.plugins.drawing.opengl import Cursor");
		PythonInterface.getPythonInterface().execString("from field.core.plugins.drawing.text import AdvancedTextToCachedLine");
		PythonInterface.getPythonInterface().execString("from field.core.dispatch import Mixins");
		PythonInterface.getPythonInterface().execString("from field.core.util import *");
        
		this.root = root;
        
		// add a next to root that adds some overrides
		root.addChild(lve);
        
		lve.setProperty(simpleConstraints_plugin, this);
		group = root.getProperty(iVisualElement.selectionGroup);
        
		elementOverride = createElementOverrides();
        
		GLComponentWindow frame;
		frame = iVisualElement.enclosingFrame.get(root);
        
		installedContext = new BaseGLGraphicsContext(frame.getSceneList(), false);
		installedContext.install(frame);
		new SimpleTextDrawing(false).installInto(installedContext);
		new SimpleWebpageDrawing(true).installInto(installedContext).setRefreshHandle(new iUpdateable() {
            
			@Override
			public void update() {
				iVisualElement.enclosingFrame.get(root).requestRepaint();
			}
		});
		sld = new SimpleLineDrawing();
		sld.installInto(installedContext);
        
		// frame.makeOverlay();
        
		// TODO swt
		// if (SystemProperties.getIntProperty("noOverlay", 0) == 0) {
		// installedFastContext = new
		// BaseGLGraphicsContext(frame.getOverlayCanvas().getSceneList(),
		// true);
		// installedFastContext.install(frame.getOverlayAnimationManager());
		// sldFast = new SimpleLineDrawing();
		// sldFast.installInto(installedFastContext);
		// new SimpleTextDrawing().installInto(installedFastContext);
		//
		// root.setProperty(iVisualElement.fastContext,
		// installedFastContext);
		//
		// }
        
		// drag alignment (and ultimately constraint creation)
		// -----------------------------
        
		dragParticipants.add(new OfferedAlignment(root));
        
		rootComponent = iVisualElement.rootComponent.get(root);
		rootComponent.addPaintPeer(new iPaintPeer() {
			public void paint(RootComponent inside) {
                
				// if
				// (Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK))
				{
					for (iDragParticipant d : dragParticipants) {
						if (d instanceof iPaintPeer) {
							((iPaintPeer) d).paint(inside);
						}
					}
				}
			}
		});
        
		// rootComponent.addMousePeer(new
		// EmbeddedInteraction.MousePeer(installedContext));
        
		complexConstraints = ComplexConstraints.complexConstraints_plugin.get(root);
        
		selectionSets = new SelectionSetDriver(root, ToolBarFolder.currentFolder);
		PluginList plugins = new PluginList(root, ToolBarFolder.currentFolder);
        
		group.registerNotification(new iSelectionChanged<iComponent>() {
            
			Set<iVisualElement> currentSelection = new LinkedHashSet<iVisualElement>();
            
			public void selectionChanged(Set<iComponent> selected) {
                
				Set<iVisualElement> currentAll = new LinkedHashSet<iVisualElement>(StandardFluidSheet.allVisualElements(root));
                
				Set<iVisualElement> sel = new LinkedHashSet<iVisualElement>();
				try {
					for (iComponent c : selected) {
						iVisualElement ve = c.getVisualElement();
						if (ve != null) {
							sel.add(ve);
						}
					}
                    
					for (iVisualElement n : currentSelection) {
						if (!sel.contains(n) && currentAll.contains(n)) {
							deselected(n);
						}
					}
                    
					for (iVisualElement n : sel) {
						if (!currentSelection.contains(n) && currentAll.contains(n)) {
							selected(n);
						}
					}
                    
				} finally {
					currentSelection = sel;
				}
			}
            
			private void deselected(iVisualElement n) {
                
				try {
					PythonCallableMap callback = selectionStateCallback.get(n);
					if (callback != null)
						callback.invoke(n, false);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
            
			private void selected(iVisualElement n) {
				try {
					PythonCallableMap callback = selectionStateCallback.get(n);
					if (callback != null)
						callback.invoke(n, true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
            
		});
        
		// selectionSets.addNotibleOverridesClass(BaseConstraintOverrides.class,
		// "Constraints");
		// selectionSets.addNotibleComponentClass(SwingBridgeComponent.class,
		// "Swing components");
        
		GlassComponent glassComponent = iVisualElement.glassComponent.get(root);
        
		LineInteraction lineInteraction = new LineInteraction();
		glassComponent.addTransparentMousePeer(lineInteraction);
		installedContext.setLineInteraction(lineInteraction);
        
	}
    
	public void setPersistanceInformation(Object o) {
		if (selectionSets == null)
			return;
        
		if (o instanceof Pair) {
			Pair<String, Set<SelectionSet>> p = (Pair<String, Set<SelectionSet>>) o;
			if (p.left.equals(pluginId + "version_1"))
				selectionSets.addSavedSelectionSets(p.right);
		}
		if (o instanceof Triple) {
			Triple<String, Set<SelectionSet>, Set<SavedView>> p = (Triple<String, Set<SelectionSet>, Set<SavedView>>) o;
			if (p.left.equals(pluginId + "version_2")) {
				selectionSets.addSavedSelectionSets(p.middle);
				selectionSets.addSavedViews(p.right);
			}
		}
	}
    
	public void update() {
        
		// if
		// (Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK))
		{
            
			boolean needs = false;
			for (int i = 0; i < dragParticipants.size(); i++) {
				needs = needs | dragParticipants.get(i).needsRepainting();
			}
			if (needs) {
				iVisualElement.enclosingFrame.get(root).getRoot().requestRedisplay();
			}
		}
        
		tick = true;
	}
    
	protected iVisualElementOverrides createElementOverrides() {
		return new Overrides() {
		}.setVisualElement(lve);
	}
    
}
