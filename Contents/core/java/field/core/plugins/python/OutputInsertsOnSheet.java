package field.core.plugins.python;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.python.core.Py;
import org.python.core.PyObject;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.StandardFluidSheet;
import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.execution.PythonInterface;
import field.core.plugins.SimpleConstraints;
import field.core.plugins.python.PythonPlugin.CapturedEnvironment;
import field.core.ui.NewInspector.Inspected;
import field.core.ui.NewInspector.Row;
import field.core.ui.NewInspector.Status;
import field.core.ui.NewInspector.TextControl;
import field.core.ui.NewInspectorFromProperties;
import field.core.ui.NewInspectorFromProperties_swing;
import field.core.ui.text.embedded.CustomInsertSystem.ProvidedComponent;
import field.core.ui.text.embedded.MinimalGraphWidget2;
import field.core.ui.text.embedded.MinimalLazyBox;
import field.core.ui.text.embedded.MinimalSlider;
import field.core.ui.text.embedded.MinimalXYSlider;
import field.core.ui.text.embedded.MinimalXYSlider.Component_tuple;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.PlainDraggableComponent;
import field.core.windowing.components.SwingBridgeComponent;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.abstraction.iAcceptor;
import field.math.abstraction.iProvider;
import field.math.abstraction.iProviderAcceptor;
import field.math.linalg.Vector2;
import field.math.linalg.iToFloatArray;
import field.math.util.BreakpointFloat;
import field.math.util.CubicInterpolatorDynamic;
import field.namespace.generic.Generics.Triple;
import field.util.MiscNative;
import field.util.RectangleAllocator;

/**
 * given the success of OutputInserts, can we put them directly on the sheet as
 * well ?
 * 
 * @author marc
 * 
 */
@Woven
public class OutputInsertsOnSheet {
	private static final int defaultHeight = 14;

	@Woven
	static public class Wrap extends SwingBridgeComponent {
		public Wrap() {
			super();
		}

		public Wrap(Rect r) {
			super(r);
		}

		@Override
		public PlainDraggableComponent setVisualElement(iVisualElement ve) {
			super.setVisualElement(ve);
			if (this.componentToRender == null) {
				ProvidedComponent provided = ve.getProperty(outputInsertsOnSheet_providedComponent);

				if (provided != null) {
					provided.deserialize(ve);
					this.componentToRender = provided.component;
					hookupNotifications();

					if (this.componentToRender != null) {
						this.componentToRender.validate();
						this.componentToRender.doLayout();
					}

				}
			}
			return this;
		}

		boolean heavy = false;

		@Override
		protected boolean shouldSendUpwards(Event arg0) {
			if (arg0.count == 2 && heavy) {
				openEditingWindow();
				return true;
			}
			// return false;
			float nearEdge = (float) Math.min(Math.min(Math.abs(arg0.x - bounds.x), Math.abs(arg0.x - bounds.x - bounds.w)), Math.min(Math.abs(arg0.y - bounds.y), Math.abs(arg0.y - bounds.y - bounds.h)));
			return (arg0.stateMask & SWT.SHIFT) != 0;// && nearEdge
									// < 4
									// ||
			// !bounds.isInside(new
			// Vector2(arg0.getX(),
			// arg0.getY()));
		}

		JFrame editingFrame = null;

		@NextUpdate(delay = 2)
		public void openEditingWindow() {
			if (editingFrame == null) {
				
				;//System.out.println(" gosh that's heavy ");
				
				editingFrame = new JFrame();
				// FloatingPalettes.registerFloatingPalette(editingFrame);

				// we need to
				// find out
				// where this
				// rect is on
				// the screen,
				// this is
				// unlikely to
				// work in a
				// rescaled view
				// (we'll settle
				// for getting
				// the top left
				// corner
				// correct)
				GLComponentWindow frame = iVisualElement.enclosingFrame.get(getVisualElement());
				Rect bounds = this.getBounds();
				Vector2 topLeft = new Vector2(bounds.x, bounds.y);
				frame.transformDrawingToWindow(topLeft);
				Point p = new Point(frame.getFrame().getBounds().x, frame.getFrame().getBounds().y);

				topLeft.x += p.x;
				topLeft.y += p.y;
				;//System.out.println(" canvas bounds are :"+frame.getCanvas().getBounds());
				
				org.eclipse.swt.graphics.Point z = Launcher.getLauncher().display.map(frame.getCanvas(), frame.getFrame(), new org.eclipse.swt.graphics.Point(0,0));
				
				topLeft.x += z.x;
				topLeft.y += z.y;
				
				editingFrame.setAlwaysOnTop(true);
				editingFrame.setUndecorated(true);
				int ww = componentToRender.getWidth();
				int hh = componentToRender.getHeight();
				editingFrame.setBounds((int) topLeft.x, (int) topLeft.y, ww, hh);
				editingFrame.getContentPane().add(componentToRender);
				componentToRender.setBounds(0, 0, ww, hh);
				editingFrame.setVisible(true);

				layoutHierarchy(editingFrame);
				layoutHierarchy(editingFrame.getContentPane());

				editingFrame.addWindowFocusListener(new WindowFocusListener() {

					public void windowGainedFocus(WindowEvent e) {
					}

					public void windowLostFocus(WindowEvent e) {
						new MiscNative().disableScreenUpdates();
						closeEditingWindow();
					}
				});

				editingFrame.addKeyListener(new KeyListener() {

					@Override
					public void keyTyped(KeyEvent arg0) {

						;//System.out.println(" typing ? :" + arg0);

						if (arg0.getKeyCode() == KeyEvent.VK_ENTER) {
							closeEditingWindow();
						}
					}

					@Override
					public void keyReleased(KeyEvent arg0) {
					}

					@Override
					public void keyPressed(KeyEvent arg0) {
					}
				});

			}
		}

		@NextUpdate(delay = 1)
		private void layoutHierarchy(Container c) {
			c.validate();
			c.doLayout();
			for (Component cc : c.getComponents()) {
				cc.validate();
				cc.doLayout();
				if (cc instanceof Container)
					layoutHierarchy((Container) cc);
			}
		}

		public void closeEditingWindow() {
			if (editingFrame != null) {
				editingFrame.getContentPane().remove(componentToRender);
				deferredClosing(editingFrame);
				setDirty();
				editingFrame = null;
			}
		}

		@NextUpdate(delay = 2)
		public void deferredClosing(JFrame editingFrame) {
			editingFrame.setVisible(false);
			editingFrame.dispose();
		}
	}

	static public class WrapNoEvents extends SwingBridgeComponent {
		public WrapNoEvents() {
			super();
		}

		public WrapNoEvents(Rect r) {
			super(r);
		}

		@Override
		public PlainDraggableComponent setVisualElement(iVisualElement ve) {
			super.setVisualElement(ve);
			if (this.componentToRender == null) {
				ProvidedComponent provided = ve.getProperty(outputInsertsOnSheet_providedComponent);

				if (provided != null) {
					provided.deserialize(ve);
					this.componentToRender = provided.component;
					hookupNotifications();

				}
			}
			return this;
		}

		@Override
		protected boolean shouldSendUpwards(Event arg0) {
			return false;
		}
	}

	static public VisualElementProperty<OutputInsertsOnSheet> outputInsertsOnSheet = new VisualElementProperty<OutputInsertsOnSheet>("outputInsertsOnSheet_");

	static public VisualElementProperty<RectangleAllocator> outputInsertsOnSheet_allocactor = new VisualElementProperty<RectangleAllocator>("outputInsertsOnSheet_allocactor");
	static public VisualElementProperty<RectangleAllocator> outputInsertsOnSheet_allocactorVert = new VisualElementProperty<RectangleAllocator>("outputInsertsOnSheet_allocactorVert");

	static public VisualElementProperty<ProvidedComponent> outputInsertsOnSheet_providedComponent = new VisualElementProperty<ProvidedComponent>("outputInsertsOnSheet_providedComponent");

	static public VisualElementProperty<Map<String, String>> outputInsertsOnSheet_knownComponents = new VisualElementProperty<Map<String, String>>("outputInsertsOnSheet_knownComponents");

	private static boolean lastWasNew;

	static public iProvider<Object> printCurve2(String name, final iVisualElement inside, final PyObject onChange, boolean below) {
		OutputInsertsOnSheet oi = outputInsertsOnSheet.get(inside);

		iVisualElement alreadyCreated = findAlreadyCreated(oi, inside, name);
		Map<String, String> map = getKnownMap(inside);

		final MinimalGraphWidget2.Component_tuple tuple;

		if (alreadyCreated != null) {
			tuple = (MinimalGraphWidget2.Component_tuple) alreadyCreated.getProperty(outputInsertsOnSheet_providedComponent);
		} else {

			map.remove(name);
			tuple = new MinimalGraphWidget2.Component_tuple();
			tuple.deserialize(inside);
			Rect frame = inside.getFrame(null);
			if (below)
				alreadyCreated = oi.makeComponentWrapperVerticalNoEvents((float) (frame.x), (float) (frame.y + frame.h + 5), (float) frame.w, defaultHeight, inside, tuple);
			else
				alreadyCreated = oi.makeComponentWrapperNoEvents((float) (frame.x + frame.w + 5), (float) (frame.y), 150, defaultHeight, inside, tuple);

			map.put(name, alreadyCreated.getUniqueID());
		}

		final iVisualElement falreadyCreated = alreadyCreated;

		falreadyCreated.setProperty(iVisualElement.name, name);

		PythonPlugin ed = PythonPlugin.python_plugin.get(inside);

		final CapturedEnvironment captured = ed.new CapturedEnvironment(inside);

		final boolean[] insideChange = { false };

		final iProvider<Object> r = new iProvider<Object>() {

			JComponent component = tuple.component;

			public Object get() {
				captured.enter();
				try {
					CubicInterpolatorDynamic<BreakpointFloat> s = tuple.interpolator;
					return s;
				} finally {
					captured.exit();
				}
			}

			public void repaint() {
				iVisualElement.dirty.set(falreadyCreated, falreadyCreated, true);
			}
		};
		tuple.notify = new iUpdateable() {
			public void update() {
				if (insideChange[0])
					return;
				if (onChange == null || onChange == Py.None)
					return;
				try {
					captured.enter();
					onChange.__call__(Py.java2py(r.get()));

				} catch (Throwable e) {
					Writer ww = PythonInterface.getPythonInterface().getErrorRedirects().peek();
					e.printStackTrace(new PrintWriter(ww));
				} finally {
					captured.exit();
				}
			}
		};

		return r;

	}

	static public iProviderAcceptor<Object> printSlider(String name, final iVisualElement inside, final PyObject onChange, boolean below) {
		OutputInsertsOnSheet oi = outputInsertsOnSheet.get(inside);

		;//System.out.println(" inside print slider for <" + inside + "> got <" + oi + ">");

		iVisualElement alreadyCreated = findAlreadyCreated(oi, inside, name);
		Map<String, String> map = getKnownMap(inside);

		final MinimalSlider.Component tuple;

		if (alreadyCreated != null) {
			tuple = (MinimalSlider.Component) alreadyCreated.getProperty(outputInsertsOnSheet_providedComponent);
			lastWasNew = false;
			;//System.out.println(" already there");
		} else {

			;//System.out.println(" genuinely new");

			map.remove(name);
			tuple = new MinimalSlider.Component();
			tuple.deserialize(inside);
			Rect frame = inside.getFrame(null);
			if (below)
				alreadyCreated = oi.makeComponentWrapperVertical((float) (frame.x), (float) (frame.y + frame.h + 5), (float) frame.w, defaultHeight, inside, tuple);
			else
				alreadyCreated = oi.makeComponentWrapper((float) (frame.x + frame.w + 5), (float) (frame.y), 150, defaultHeight, inside, tuple);

			map.put(name, alreadyCreated.getUniqueID());
			;//System.out.println(" created, map now <" + map + ">");
			lastWasNew = true;
		}

		final iVisualElement falreadyCreated = alreadyCreated;

		falreadyCreated.setProperty(iVisualElement.name, name);

		PythonPlugin ed = PythonPlugin.python_plugin.get(inside);

		final CapturedEnvironment captured = ed.new CapturedEnvironment(inside);

		final boolean[] insideChange = { false };

		final iProviderAcceptor<Object> r = new iProviderAcceptor<Object>() {

			JComponent component = tuple.component;

			public Object get() {
				captured.enter();
				try {
					String s = tuple.getCurrentRepresentedString();
					Object o = PythonInterface.getPythonInterface().eval(s);
					return o;
				} catch (Throwable e) {
					Writer ww = PythonInterface.getPythonInterface().getErrorRedirects().peek();
					e.printStackTrace(new PrintWriter(ww));
					return null;
				} finally {
					captured.exit();
				}
			}

			public void repaint() {
				iVisualElement.dirty.set(falreadyCreated, falreadyCreated, true);
			}

			public iAcceptor<Object> set(Object to) {

				insideChange[0] = true;
				try {
					if (to instanceof iToFloatArray) {
						float[] fa = ((iToFloatArray) to).get();
						((MinimalSlider) tuple.component).setValue(fa[0]);
					} else if (to instanceof PyObject) {
						PyObject p = ((PyObject) to);
						float v1 = ((Number) p.__getitem__(0).__tojava__(Number.class)).floatValue();
						((MinimalSlider) tuple.component).setValue(v1);
					} else if (to instanceof Number) {
						((MinimalSlider) tuple.component).setValue(((Number) to).floatValue());
					}
					iVisualElement.dirty.set(falreadyCreated, falreadyCreated, true);
				} catch (Throwable e) {
					Writer ww = PythonInterface.getPythonInterface().getErrorRedirects().peek();
					e.printStackTrace(new PrintWriter(ww));
				} finally {
					insideChange[0] = false;
				}
				return this;
			}
		};
		tuple.notify = new iUpdateable() {
			public void update() {
				if (insideChange[0])
					return;
				if (onChange == null || onChange == Py.None)
					return;
				try {
					captured.enter();
					onChange.__call__(Py.java2py(r.get()));
				} catch (Throwable e) {
					Writer ww = PythonInterface.getPythonInterface().getErrorRedirects().peek();
					e.printStackTrace(new PrintWriter(ww));
				} finally {
					captured.exit();
				}
			}
		};

		return r;
	}

	static public iProviderAcceptor<Object> printLazy(String name, final iVisualElement inside, boolean below) {
		OutputInsertsOnSheet oi = outputInsertsOnSheet.get(inside);

		;//System.out.println(" inside print slider for <" + inside + "> got <" + oi + ">");

		iVisualElement alreadyCreated = findAlreadyCreated(oi, inside, name);
		Map<String, String> map = getKnownMap(inside);

		final MinimalLazyBox.Component tuple;

		if (alreadyCreated != null) {
			tuple = (MinimalLazyBox.Component) alreadyCreated.getProperty(outputInsertsOnSheet_providedComponent);
			lastWasNew = false;
			;//System.out.println(" already there");
		} else {

			;//System.out.println(" genuinely new");

			map.remove(name);
			tuple = new MinimalLazyBox.Component();
			tuple.deserialize(inside);
			Rect frame = inside.getFrame(null);
			if (below)
				alreadyCreated = oi.makeComponentWrapperVertical((float) (frame.x), (float) (frame.y + frame.h + 5), (float) frame.w, defaultHeight, inside, tuple);
			else
				alreadyCreated = oi.makeComponentWrapper((float) (frame.x + frame.w + 5), (float) (frame.y), 150, defaultHeight, inside, tuple);

			((Wrap)alreadyCreated.getProperty(iVisualElement.localView)).heavy = false;
			
			
			map.put(name, alreadyCreated.getUniqueID());
			;//System.out.println(" created, map now <" + map + ">");
			lastWasNew = true;
		}

		final iVisualElement falreadyCreated = alreadyCreated;

		falreadyCreated.setProperty(iVisualElement.name, name);

		PythonPlugin ed = PythonPlugin.python_plugin.get(inside);

		final CapturedEnvironment captured = ed.new CapturedEnvironment(inside);

		final boolean[] insideChange = { false };

		final iProviderAcceptor<Object> r = new iProviderAcceptor<Object>() {

			JComponent component = tuple.component;

			public Object get() {
				captured.enter();
				try {
					String s = tuple.getCurrentRepresentedString();
					Object o = PythonInterface.getPythonInterface().eval(s);
					return o;
				} catch (Throwable e) {
					Writer ww = PythonInterface.getPythonInterface().getErrorRedirects().peek();
					e.printStackTrace(new PrintWriter(ww));
					return null;
				} finally {
					captured.exit();
				}
			}

			public void repaint() {
				iVisualElement.dirty.set(falreadyCreated, falreadyCreated, true);
			}

			public iAcceptor<Object> set(Object to) {
				((MinimalLazyBox.Component) tuple).updateValue("" + to);
				repaint();
				return this;
			}
		};

		return r;
	}

	//
	// static public ComboState printCombo(String name, final iVisualElement
	// inside, final PyObject onChange, boolean below) {
	// OutputInsertsOnSheet oi = outputInsertsOnSheet.get(inside);
	//
	// ;//System.out.println(" inside print slider for <" + inside + "> got <"
	// + oi + ">");
	//
	// iVisualElement alreadyCreated = findAlreadyCreated(oi, inside, name);
	// Map<String, String> map = getKnownMap(inside);
	//
	// final MinimalCombo.Component tuple;
	//
	// if (alreadyCreated != null) {
	// tuple = (MinimalCombo.Component)
	// alreadyCreated.getProperty(outputInsertsOnSheet_providedComponent);
	// lastWasNew = false;
	// ;//System.out.println(" already there");
	// } else {
	//
	// ;//System.out.println(" genuinely new");
	//
	// map.remove(name);
	// tuple = new MinimalCombo.Component();
	// tuple.deserialize(inside);
	// Rect frame = inside.getFrame(null);
	// if (below)
	// alreadyCreated = oi.makeComponentWrapperVertical((float) (frame.x),
	// (float) (frame.y + frame.h + 5), (float) frame.w, 12, inside, tuple);
	// else
	// alreadyCreated = oi.makeComponentWrapper((float) (frame.x + frame.w +
	// 5), (float) (frame.y), 150, 12, inside, tuple);
	//
	// map.put(name, alreadyCreated.getUniqueID());
	// ;//System.out.println(" created, map now <" + map + ">");
	// lastWasNew = true;
	// }
	//
	// final iVisualElement falreadyCreated = alreadyCreated;
	//
	// falreadyCreated.setProperty(iVisualElement.name, name);
	//
	// PythonPlugin ed = PythonPlugin.python_plugin.get(inside);
	//
	// final CapturedEnvironment captured = ed.new
	// CapturedEnvironment(inside);
	//
	// final boolean[] insideChange = { false };
	//
	// tuple.notify = new iUpdateable() {
	// public void update() {
	// if (insideChange[0])
	// return;
	//
	// iVisualElement.dirty.set(falreadyCreated, falreadyCreated, true);
	//
	// if (onChange == null || onChange == Py.None)
	// return;
	// try {
	// captured.enter();
	// onChange.__call__(Py.java2py(tuple.state));
	// } catch (Throwable e) {
	// Writer ww =
	// PythonInterface.getPythonInterface().getErrorRedirects().peek();
	// e.printStackTrace(new PrintWriter(ww));
	// } finally {
	// captured.exit();
	// }
	// }
	// };
	//
	// // no idea why this needs to be defered.
	// Launcher.getLauncher().registerUpdateable(new iUpdateable() {
	//
	// public void update() {
	// tuple.component.setSize(150, 12);
	// Launcher.getLauncher().deregisterUpdateable(this);
	// }
	// });
	//
	// return tuple.state;
	// }

	static public iProviderAcceptor<Object> printXYSlider(String name, final iVisualElement inside, final PyObject onChange, boolean below) {
		OutputInsertsOnSheet oi = outputInsertsOnSheet.get(inside);
		Map<String, String> map = getKnownMap(inside);

		iVisualElement alreadyCreated = findAlreadyCreated(oi, inside, name);

		final Component_tuple tuple;

		if (alreadyCreated != null) {
			tuple = (Component_tuple) alreadyCreated.getProperty(outputInsertsOnSheet_providedComponent);
			lastWasNew = false;
		} else {

			map.remove(name);
			tuple = new MinimalXYSlider.Component_tuple();
			tuple.deserialize(inside);
			Rect frame = inside.getFrame(null);
			if (below)
				alreadyCreated = oi.makeComponentWrapperVertical((float) (frame.x), (float) (frame.y + frame.h + 5), (float) frame.w, defaultHeight, inside, tuple);
			else
				alreadyCreated = oi.makeComponentWrapper((float) (frame.x + frame.w + 5), (float) (frame.y), 150, defaultHeight, inside, tuple);

			map.put(name, alreadyCreated.getUniqueID());
			lastWasNew = true;
		}

		final iVisualElement falreadyCreated = alreadyCreated;
		falreadyCreated.setProperty(iVisualElement.name, name);

		PythonPlugin ed = PythonPlugin.python_plugin.get(inside);

		final CapturedEnvironment captured = ed.new CapturedEnvironment(inside);

		final boolean[] insideChange = { false };

		final iProviderAcceptor<Object> r = new iProviderAcceptor<Object>() {

			JComponent component = tuple.component;

			public Object get() {
				captured.enter();
				try {
					String s = tuple.getCurrentRepresentedString();
					System.err.println(" about to eval <" + s + ">");
					Object o = PythonInterface.getPythonInterface().eval(s);
					return o;
				} finally {
					captured.exit();
				}
			}

			public void repaint() {
				iVisualElement.dirty.set(falreadyCreated, falreadyCreated, true);
			}

			public iAcceptor<Object> set(Object to) {
				if (to instanceof iToFloatArray) {
					float[] fa = ((iToFloatArray) to).get();
					((MinimalXYSlider) tuple.component).setValue(fa[0], fa[1]);
				} else if (to instanceof PyObject) {
					PyObject p = ((PyObject) to);
					float v1 = ((Number) p.__getitem__(0).__tojava__(Number.class)).floatValue();
					float v2 = ((Number) p.__getitem__(1).__tojava__(Number.class)).floatValue();
					((MinimalXYSlider) tuple.component).setValue(v1, v2);
				}
				iVisualElement.dirty.set(falreadyCreated, falreadyCreated, true);

				return this;
			}
		};

		tuple.notify = new iUpdateable() {
			public void update() {
				if (insideChange[0])
					return;
				if (onChange == null || onChange == Py.None)
					return;
				try {
					captured.enter();
					onChange.__call__(Py.java2py(r.get()));
				} catch (Throwable e) {
					Writer ww = PythonInterface.getPythonInterface().getErrorRedirects().peek();
					e.printStackTrace(new PrintWriter(ww));
				} finally {
					captured.exit();
				}
			}
		};
		return r;
	}

	/*
	 * currently doesn't work. The provided component becomes invisible in
	 * the text editor \u2014 see ticket #6
	 */
	public static void wrapExisting(String name, iVisualElement inside, ProvidedComponent minimalSlider) {
		OutputInsertsOnSheet oi = outputInsertsOnSheet.get(inside);
		if (oi != null) {
			iVisualElement already = findAlreadyCreated(oi, inside, name);
			if (already == null) {
				Rect frame = inside.getFrame(null);
				already = oi.makeComponentWrapperExisting((float) (frame.x + frame.w + 5), (float) (frame.y), 150, defaultHeight, inside, minimalSlider);
				Map<String, String> map = getKnownMap(inside);
				map.put(name, already.getUniqueID());

				// todo update ?
			} else {

				Wrap wrap = (Wrap) already.getProperty(iVisualElement.localView);

				wrap.componentToRender = minimalSlider.component;
				wrap.hookupNotifications();

				already.setProperty(outputInsertsOnSheet_providedComponent, minimalSlider);

			}
		}
	}

	public static void printProperty(final iVisualElement inside, final String propertyName) {
		printProperty(inside, propertyName, null);
	}

	public static void printProperty(final iVisualElement inside, final String propertyName, final PyObject callback) {
		Object p = new VisualElementProperty(propertyName).get(inside);
		if (p == null)
			return;

		Class<? extends JComponent> componentClass = NewInspectorFromProperties_swing.knownProperties.get(propertyName);
		String alias = NewInspectorFromProperties.knownAliases.get(propertyName);

		if (componentClass == null) {
			componentClass = NewInspectorFromProperties_swing.componentForValue(p);
		}

		OutputInsertsOnSheet oi = outputInsertsOnSheet.get(inside);

		String name = "_property_" + propertyName;
		iVisualElement alreadyCreated = findAlreadyCreated(oi, inside, name);
		Map<String, String> map = getKnownMap(inside);

		JComponent tuple = null;

		if (alreadyCreated != null) {
			ProvidedComponent pc = (ProvidedComponent) alreadyCreated.getProperty(outputInsertsOnSheet_providedComponent);
			tuple = (JComponent) (pc).component;
			lastWasNew = false;

			Inspected inspected = ((Row) tuple).i;

			try {
				inspected.getClass().getField("cc").set(inspected, callback);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			}

			;//System.out.println(" already there");
		} else {

			;//System.out.println(" genuinely new");

			map.remove(name);

			boolean heavy = false;

			try {
				Inspected<Object, JComponent> ii = new Inspected<Object, JComponent>() {

					public PyObject cc = callback;

					@Override
					public Object getValue() {
						Object p = new VisualElementProperty(propertyName).get(inside);
						return p;
					}

					@Override
					public void setValue(Object s) {
						new VisualElementProperty(propertyName).set(inside, inside, s);

						if (cc != null && Py.None != cc) {
							// ;//System.out.println(" -- calling callback for property --");

							try {
								cc.__call__(new PyObject[] { Py.java2py(inside), Py.java2py(s) });
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}

					@Override
					public Status getStatus() {
						return Status.valid;
					}
				};
				
				;//System.out.println(" component class is <"+componentClass+">");
				Constructor<? extends JComponent> c = componentClass.getConstructor(Inspected.class);
				
				;//System.out.println(" looked up constructor for <"+componentClass+"> got <"+c+">");
				
				tuple = (JComponent) componentClass.getConstructor(Inspected.class).newInstance(ii);
				ii.editor = tuple;
				ii.name = propertyName;

				heavy = tuple instanceof TextControl;

				tuple = new Row(ii, 110);
				((Row) tuple).drawBackground = true;

			} catch (Exception e) {
				e.printStackTrace();
			}
			Rect frame = inside.getFrame(null);
			alreadyCreated = oi.makeComponentWrapperVerticalMinWidth((float) (frame.x), (float) (frame.y + frame.h + 5), (float) frame.w, 30, inside, new ProvidedComponent((JComponent) tuple) {
				@Override
				public String getCurrentRepresentedString() {
					return "";
				}
			});

			iVisualElement.doNotSave.set(alreadyCreated, alreadyCreated, true);

			Wrap comp = (Wrap) iVisualElement.localView.get(alreadyCreated);
			comp.backgroundA = comp.obackgroundA = 0;
			comp.backgroundR = comp.obackgroundR = 0.67f;
			comp.backgroundG = comp.obackgroundG = 0.67f;
			comp.backgroundB = comp.obackgroundB = 0.67f;
			comp.doName = false;

			comp.heavy = heavy;

			map.put(name, alreadyCreated.getUniqueID());
			;//System.out.println(" created, map now <" + map + ">");
			lastWasNew = true;

			oi.layoutHierarchy(comp.componentToRender, alreadyCreated);
		}

		final iVisualElement falreadyCreated = alreadyCreated;

		falreadyCreated.setProperty(iVisualElement.name, name);

	}

	private static iVisualElement findAlreadyCreated(OutputInsertsOnSheet oi, iVisualElement inside, String name) {
		Map<String, String> map = getKnownMap(inside);
		if (map == null) {
			outputInsertsOnSheet_knownComponents.set(inside, inside, map = new HashMap<String, String>());
		}

		String alreadyCreatedUID = map.get(name);
		iVisualElement alreadyCreated = null;
		if (alreadyCreatedUID != null) {
			alreadyCreated = StandardFluidSheet.findVisualElement(oi.root, alreadyCreatedUID);
		}
		return alreadyCreated;
	}

	private static Map<String, String> getKnownMap(final iVisualElement inside) {
		Map<String, String> map = outputInsertsOnSheet_knownComponents.get(inside);
		if (map == null)
			outputInsertsOnSheet_knownComponents.set(inside, inside, map = new HashMap<String, String>());
		return map;
	}

	private final iVisualElement root;

	public OutputInsertsOnSheet(iVisualElement root) {
		this.root = root;
		root.setProperty(outputInsertsOnSheet, this);

		;//System.out.println("                    oios is :" + this + " for " + root);
	}

	public void delete(iVisualElement inside, String name) {
		iVisualElement a = findAlreadyCreated(this, inside, name);
		if (a != null) {
			Map<String, String> map = getKnownMap(inside);
			map.remove(name);
			PythonPluginEditor.delete(a, root);
		}
	}

	public iVisualElement makeComponentWrapper(float x, float y, float w, float h, iVisualElement inside, ProvidedComponent component) {

		Triple<VisualElement, Wrap, DefaultOverride> r2 = VisualElement.create(new Rect(x, y, w, h), VisualElement.class, Wrap.class, iVisualElementOverrides.DefaultOverride.class);
		r2.left.addChild(root);

		r2.left.setProperty(outputInsertsOnSheet_providedComponent, component);
		r2.middle.setVisualElement(r2.left);

		Rect at = r2.left.getFrame(null);

		allocate(inside, r2.left, at);
		r2.left.setFrame(new Rect(at.x, (float) inside.getFrame(null).y, at.w, at.h));

		new iVisualElementOverrides.MakeDispatchProxy().getBackwardsOverrideProxyFor(r2.left).added(r2.left);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(r2.left).added(r2.left);

		field.core.plugins.drawing.OfferedAlignment.alignment_doNotParticipate.set(r2.left, r2.left, true);

		SimpleConstraints constraints = SimpleConstraints.simpleConstraints_plugin.get(root);
		constraints.addConstraint(new SimpleConstraints.AtPoint(inside, inside, r2.left, 0, (float) (inside.getFrame(null).y - at.y)));
		// constraints.addConstraint(new
		// RectangleAllocatorConstraint(inside, inside, r2.left,
		// outputInsertsOnSheet_allocactor));

		return r2.left;
	}

	public iVisualElement makeComponentWrapperVerticalNoEvents(float x, float y, float w, float h, iVisualElement inside, ProvidedComponent component) {

		Triple<VisualElement, WrapNoEvents, DefaultOverride> r2 = VisualElement.create(new Rect(x, y, w, h), VisualElement.class, WrapNoEvents.class, iVisualElementOverrides.DefaultOverride.class);
		r2.left.addChild(root);

		r2.left.setProperty(outputInsertsOnSheet_providedComponent, component);
		r2.middle.setVisualElement(r2.left);

		Rect at = r2.left.getFrame(null);

		allocateVert(inside, r2.left, at);
		r2.left.setFrame(new Rect(at.x, at.y, at.w, at.h));

		new iVisualElementOverrides.MakeDispatchProxy().getBackwardsOverrideProxyFor(r2.left).added(r2.left);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(r2.left).added(r2.left);

		field.core.plugins.drawing.OfferedAlignment.alignment_doNotParticipate.set(r2.left, r2.left, true);

		SimpleConstraints constraints = SimpleConstraints.simpleConstraints_plugin.get(root);
		constraints.addConstraint(new SimpleConstraints.AtPointBelow(inside, inside, r2.left, 0, (float) at.y));
		// constraints.addConstraint(new
		// RectangleAllocatorConstraint(inside, inside, r2.left,
		// outputInsertsOnSheet_allocactor));

		return r2.left;
	}

	public iVisualElement makeComponentWrapperNoEvents(float x, float y, float w, float h, iVisualElement inside, ProvidedComponent component) {

		Triple<VisualElement, WrapNoEvents, DefaultOverride> r2 = VisualElement.create(new Rect(x, y, w, h), VisualElement.class, WrapNoEvents.class, iVisualElementOverrides.DefaultOverride.class);
		r2.left.addChild(root);

		r2.left.setProperty(outputInsertsOnSheet_providedComponent, component);
		r2.middle.setVisualElement(r2.left);

		Rect at = r2.left.getFrame(null);

		allocate(inside, r2.left, at);
		r2.left.setFrame(new Rect(at.x, (float) inside.getFrame(null).y, at.w, at.h));

		new iVisualElementOverrides.MakeDispatchProxy().getBackwardsOverrideProxyFor(r2.left).added(r2.left);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(r2.left).added(r2.left);

		field.core.plugins.drawing.OfferedAlignment.alignment_doNotParticipate.set(r2.left, r2.left, true);

		SimpleConstraints constraints = SimpleConstraints.simpleConstraints_plugin.get(root);
		constraints.addConstraint(new SimpleConstraints.AtPoint(inside, inside, r2.left, 0, (float) (inside.getFrame(null).y - at.y)));
		// constraints.addConstraint(new
		// RectangleAllocatorConstraint(inside, inside, r2.left,
		// outputInsertsOnSheet_allocactor));

		return r2.left;
	}

	public iVisualElement makeComponentWrapperVerticalMinWidth(float x, float y, float w, float h, iVisualElement inside, ProvidedComponent component) {

		w = Math.max(320, w);

		Triple<VisualElement, Wrap, DefaultOverride> r2 = VisualElement.create(new Rect(x, y, w, h), VisualElement.class, Wrap.class, iVisualElementOverrides.DefaultOverride.class);
		r2.left.addChild(root);

		r2.left.setProperty(outputInsertsOnSheet_providedComponent, component);

		r2.middle.setVisualElement(r2.left);

		Rect at = r2.left.getFrame(null);

		allocateVert(inside, r2.left, at);
		r2.left.setFrame(new Rect(at.x, at.y, at.w, at.h));

		new iVisualElementOverrides.MakeDispatchProxy().getBackwardsOverrideProxyFor(r2.left).added(r2.left);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(r2.left).added(r2.left);

		field.core.plugins.drawing.OfferedAlignment.alignment_doNotParticipate.set(r2.left, r2.left, true);

		SimpleConstraints constraints = SimpleConstraints.simpleConstraints_plugin.get(root);
		constraints.addConstraint(new SimpleConstraints.AtPointBelowMinWidth(inside, inside, r2.left, 0, (float) at.y, 320));
		// constraints.addConstraint(new
		// RectangleAllocatorConstraint(inside, inside, r2.left,
		// outputInsertsOnSheet_allocactor));

		return r2.left;
	}

	public iVisualElement makeComponentWrapperVertical(float x, float y, float w, float h, iVisualElement inside, ProvidedComponent component) {

		Triple<VisualElement, Wrap, DefaultOverride> r2 = VisualElement.create(new Rect(x, y, w, h), VisualElement.class, Wrap.class, iVisualElementOverrides.DefaultOverride.class);
		r2.left.addChild(root);

		r2.left.setProperty(outputInsertsOnSheet_providedComponent, component);
		r2.middle.setVisualElement(r2.left);

		Rect at = r2.left.getFrame(null);

		allocateVert(inside, r2.left, at);
		r2.left.setFrame(new Rect(at.x, at.y, at.w, at.h));

		new iVisualElementOverrides.MakeDispatchProxy().getBackwardsOverrideProxyFor(r2.left).added(r2.left);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(r2.left).added(r2.left);

		field.core.plugins.drawing.OfferedAlignment.alignment_doNotParticipate.set(r2.left, r2.left, true);

		SimpleConstraints constraints = SimpleConstraints.simpleConstraints_plugin.get(root);
		constraints.addConstraint(new SimpleConstraints.AtPointBelow(inside, inside, r2.left, 0, (float) at.y));
		// constraints.addConstraint(new
		// RectangleAllocatorConstraint(inside, inside, r2.left,
		// outputInsertsOnSheet_allocactor));

		return r2.left;
	}

	public iVisualElement makeComponentWrapperVertical(float x, float y, float w, float h, iVisualElement inside, JComponent component) {

		Triple<VisualElement, Wrap, DefaultOverride> r2 = VisualElement.create(new Rect(x, y, w, h), VisualElement.class, Wrap.class, iVisualElementOverrides.DefaultOverride.class);
		r2.left.addChild(root);

		// !!
		r2.left.setProperty((VisualElementProperty) outputInsertsOnSheet_providedComponent, component);
		r2.middle.setVisualElement(r2.left);

		Rect at = r2.left.getFrame(null);

		allocateVert(inside, r2.left, at);
		r2.left.setFrame(new Rect(at.x, at.y, at.w, at.h));

		new iVisualElementOverrides.MakeDispatchProxy().getBackwardsOverrideProxyFor(r2.left).added(r2.left);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(r2.left).added(r2.left);

		field.core.plugins.drawing.OfferedAlignment.alignment_doNotParticipate.set(r2.left, r2.left, true);

		SimpleConstraints constraints = SimpleConstraints.simpleConstraints_plugin.get(root);
		constraints.addConstraint(new SimpleConstraints.AtPointBelow(inside, inside, r2.left, 0, (float) at.y));
		// constraints.addConstraint(new
		// RectangleAllocatorConstraint(inside, inside, r2.left,
		// outputInsertsOnSheet_allocactor));

		return r2.left;
	}

	public iVisualElement makeComponentWrapperExisting(float x, float y, float w, float h, iVisualElement inside, ProvidedComponent component) {

		Triple<VisualElement, Wrap, DefaultOverride> r2 = VisualElement.create(new Rect(x, y, w, h), VisualElement.class, Wrap.class, iVisualElementOverrides.DefaultOverride.class);

		r2.middle.componentToRender = component.component;
		r2.middle.hookupNotifications();

		r2.left.addChild(root);

		r2.left.setProperty(outputInsertsOnSheet_providedComponent, component);
		r2.middle.setVisualElement(r2.left);

		Rect at = r2.left.getFrame(null);

		allocate(inside, r2.left, at);
		r2.left.setFrame(new Rect(at.x, (float) inside.getFrame(null).y, at.w, at.h));

		new iVisualElementOverrides.MakeDispatchProxy().getBackwardsOverrideProxyFor(r2.left).added(r2.left);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(r2.left).added(r2.left);

		field.core.plugins.drawing.OfferedAlignment.alignment_doNotParticipate.set(r2.left, r2.left, true);

		SimpleConstraints constraints = SimpleConstraints.simpleConstraints_plugin.get(root);
		constraints.addConstraint(new SimpleConstraints.AtPoint(inside, inside, r2.left, 0, (float) (inside.getFrame(null).y - at.y)));

		return r2.left;
	}

	@NextUpdate(delay = 20)
	private void layoutHierarchy(Container c, iVisualElement e) {
		c.validate();
		c.doLayout();

		iVisualElement.dirty.set(e, e, true);

		for (Component cc : c.getComponents()) {
			cc.validate();
			cc.doLayout();
			if (cc instanceof Container)
				layoutHierarchy((Container) cc, e);
		}
	}

	private void allocate(iVisualElement parent, iVisualElement inside, Rect at) {
		RectangleAllocator alloc = parent.getProperty(outputInsertsOnSheet_allocactor);
		if (alloc == null) {
			alloc = new RectangleAllocator();
			parent.setProperty(outputInsertsOnSheet_allocactor, alloc);
		} else {
			Iterator<Entry<Object, Rect>> i = alloc.rectangles.entrySet().iterator();
			while (i.hasNext()) {
				Entry<Object, Rect> n = i.next();
				String uid = (String) n.getKey();
				iVisualElement e = StandardFluidSheet.findVisualElement(root, uid);

				if (e == null)
					i.remove();
				else {
					n.setValue(e.getFrame(null));
				}
			}
		}
		Rect x = alloc.allocate(inside.getUniqueID(), at, RectangleAllocator.Move.down, 5);
		at.setValue(x);
	}

	private void allocateVert(iVisualElement parent, iVisualElement inside, Rect at) {
		RectangleAllocator alloc = parent.getProperty(outputInsertsOnSheet_allocactorVert);
		if (alloc == null) {
			;//System.out.println(" new allocator ");
			alloc = new RectangleAllocator();
			parent.setProperty(outputInsertsOnSheet_allocactorVert, alloc);
		} else {
			Iterator<Entry<Object, Rect>> i = alloc.rectangles.entrySet().iterator();
			while (i.hasNext()) {
				Entry<Object, Rect> n = i.next();
				String uid = (String) n.getKey();
				iVisualElement e = StandardFluidSheet.findVisualElement(root, uid);

				if (e == null)
					i.remove();
				else {
					n.setValue(e.getFrame(null));
				}
			}
		}

		;//System.out.println(" allocating <" + inside.getFrame(null) + " with " + at);
		;//System.out.println(" allocator is <" + alloc.rectangles + ">");

		Rect x = alloc.allocate(inside.getUniqueID(), at, RectangleAllocator.Move.down, 5);
		at.setValue(x);
		;//System.out.println(" got <" + x + ">");
	}
}
