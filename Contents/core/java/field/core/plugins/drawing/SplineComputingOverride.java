package field.core.plugins.drawing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.python.core.Py;
import org.python.core.PyObject;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.dispatch.Mixins;
import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.execution.PythonInterface;
import field.core.execution.PythonScriptingSystem;
import field.core.execution.PythonScriptingSystem.DerivativePromise;
import field.core.execution.PythonScriptingSystem.Promise;
import field.core.execution.iExecutesPromise;
import field.core.persistance.VEList;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.OnCanvasLines;
import field.core.plugins.drawing.opengl.OnCanvasLines.DirectLayer;
import field.core.plugins.drawing.opengl.iLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.drawing.tweak.TweakSplineUI;
import field.core.plugins.python.PythonPlugin;
import field.core.plugins.python.PythonPluginEditor;
import field.core.ui.NewInspector2.BooleanControl;
import field.core.ui.NewInspectorFromProperties;
import field.core.ui.PopupTextBox;
import field.core.ui.text.PythonTextEditor.EditorExecutionInterface;
import field.core.ui.text.embedded.ExecutableAreaFinder;
import field.core.util.FieldPyObjectAdaptor2;
import field.core.util.LoadInternalWorkspaceFile;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.PlainDraggableComponent;
import field.core.windowing.components.RootComponent;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.iComponent;
import field.core.windowing.overlay.OverlayAnimationManager;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.abstraction.iAcceptor;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.graph.TopologySearching;
import field.math.graph.iTopology;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Triple;

@Woven
public class SplineComputingOverride extends DefaultOverride implements iVisualElementOverrides, iChangeParticipant {

	public final class PLineList extends ArrayList {
		public PLineList() {
			super();
		}

		public PLineList(Collection<CachedLine> c) {
			super(c);
		}

		public PLineList(int initialCapacity) {
			super(initialCapacity);
		}

		public void put(String name, Object element) {
			int x = 0;
			((CachedLine) element).getProperties().put(iLinearGraphicsContext.internalName, name);

			for (Object c : this) {
				Object n = ((CachedLine) c).getProperties().get(iLinearGraphicsContext.internalName);
				if (n != null && n.equals(name)) {
					this.set(x, element);
					return;
				}
				x++;
			}

			add(element);
		}

		public Object get(String name) {
			int x = 0;
			for (Object c : this) {
				Object n = ((CachedLine) c).getProperties().get(iLinearGraphicsContext.internalName);
				if (n != null && n.equals(name)) {
					return c;
				}
				x++;
			}

			return null;
		}

		@Override
		public boolean remove(Object o) {
			return super.remove(filter(o));
		}

		@Override
		public boolean removeAll(Collection c) {
			boolean t = true;
			for (Object o : c) {
				t &= remove(o);
			}
			return t;
		}

		@Override
		public void add(int index, Object element) {
			element = filter(element);
			if (element != null)
				super.add(index, element);
		}

		@Override
		public boolean add(Object o) {
			o = filter(o);
			if (o instanceof CachedLine)
				return super.add(o);
			return false;
		}

		@Override
		public boolean addAll(Collection c) {
			boolean t = true;
			for (Object o : c) {
				t &= add(o);
			}
			return false;
		}

		@Override
		public boolean addAll(int index, Collection c) {
			for (Object o : c) {
				add(index++, o);
			}
			return true;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object set(int index, Object element) {
			element = filter(element);
			if (element != null)
				return super.set(index, element);
			return null;
		}

		private Object filter(Object o) {
			return SplineComputingOverride.this.filter(o);
		}
	}

	static public final VisualElementProperty<List<CachedLine>> computed_linesToDraw = new VisualElementProperty<List<CachedLine>>("lines").setFreezable();

	static public final VisualElementProperty<VEList> computed_elaborates = new VisualElementProperty<VEList>("elaborates");
	static public final VisualElementProperty<VEList> computed_elaboratedBy = new VisualElementProperty<VEList>("elaboratedBy");

	static public final VisualElementProperty<Integer> computed_computeWhen = new VisualElementProperty<Integer>("lineWhen");

	static public final VisualElementProperty<String> onChange = new VisualElementProperty<String>("onChange_v");

	static public final VisualElementProperty<String> onFrameChange = new VisualElementProperty<String>("onFrameChange_v");

	static public final VisualElementProperty<String> onSelection = new VisualElementProperty<String>("onSelection_v");

	static public final VisualElementProperty<String> tweak = new VisualElementProperty<String>("tweak_v");

	static public final VisualElementProperty<Rect> computed_rect = new VisualElementProperty<Rect>("outRect_");

	static public final VisualElementProperty<Boolean> shouldAutoComputeRect = new VisualElementProperty<Boolean>("shouldAutoComputeRect");

	static public final VisualElementProperty<Number> global_opacity = new VisualElementProperty<Number>("globalOpacity");

	static public final VisualElementProperty<Boolean> noFrame = new VisualElementProperty<Boolean>("noFrame");

	static public final VisualElementProperty<DirectLayer> direct = new VisualElementProperty<DirectLayer>("direct_");

	static public final VisualElementProperty<List<iUpdateable>> computed_drawingInstructions = new VisualElementProperty<List<iUpdateable>>("computed_drawingInstructions");

	static {
		PythonPluginEditor.knownPythonProperties.put("Spline tweaks", SplineComputingOverride.tweak);

		NewInspectorFromProperties.knownProperties.put(shouldAutoComputeRect.getName(), BooleanControl.class);
		NewInspectorFromProperties.knownAliases.put(shouldAutoComputeRect.getName(), "Compute frame ?");

		NewInspectorFromProperties.knownProperties.put(noFrame.getName(), BooleanControl.class);
		NewInspectorFromProperties.knownAliases.put(noFrame.getName(), "Hide background ?");

		FieldPyObjectAdaptor2.injectedSelfMethods.put("tweaks", PythonInterface.getPythonInterface().executeStringReturnPyObject("def __tweaks(self):\n" + "\tself.overrides.executeTweaks(self)\n", "__tweaks"));

		PythonInterface.getPythonInterface().execString("from field.core.plugins.drawing.opengl import OptimizeCachedLineSet");

		FieldPyObjectAdaptor2.injectedSelfMethods.put("optimizeLines", PythonInterface.getPythonInterface().executeStringReturnPyObject("def __optimize(self, exclude=None):\n" + "\tOptimizeCachedLineSet().doOptimization(_self.lines,exclude)\n", "__optimize"));
	}

	@Override
	public DefaultOverride setVisualElement(iVisualElement ve) {
		DefaultOverride r = super.setVisualElement(ve);
		ve.setProperty(noFrame, false);

		if (ve.getProperty(computed_linesToDraw) == null) {
			Ref<List<CachedLine>> rr = new Ref<List<CachedLine>>(null);
			initializeDrawngInstructions(rr);
			ve.setProperty(computed_linesToDraw, rr.get());
		} else if (!(ve.getProperty(computed_linesToDraw) instanceof PLineList)) {
			PLineList ll = new PLineList();
			ll.addAll(ve.getProperty(computed_linesToDraw));
			ve.setProperty(computed_linesToDraw, ll);
		}

		return r;
	}

	int frame;

	static public void executeMain(iVisualElement e) {
		executePropertyOfElement(PythonPlugin.python_source, e);
	}

	static public void executePropertyOfElement(VisualElementProperty<String> property, iVisualElement source) {
		System.err.println(" execute property <" + property + "> of <" + source + ">");
		try {
			String e = property.get(source);
			if (e == null)
				return;
			if (e.equals(""))
				return;

			Ref<PythonScriptingSystem> refPss = new Ref<PythonScriptingSystem>(null);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(source).getProperty(source, PythonScriptingSystem.pythonScriptingSystem, refPss);
			assert refPss.get() != null;

			Ref<iExecutesPromise> refRunner = new Ref<iExecutesPromise>(null);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(source).getProperty(source, iExecutesPromise.promiseExecution, refRunner);
			assert refRunner.get() != null;

			EditorExecutionInterface eei = PythonPluginEditor.editorExecutionInterface.get(source);

			Promise promise = refPss.get().promiseForKey(source);

			if (promise instanceof DerivativePromise) {
				promise = ((DerivativePromise) promise).getDerivativeWithText(property);
			}

			promise.beginExecute();
			String text = promise.getText();

			System.err.println(" about to execute <" + text + "> inside <" + promise + ">");
			if (eei != null)
				eei.executeFragment(text);
			else
				PythonInterface.getPythonInterface().execString(text);
			System.err.println(" finished ");
			promise.endExecute();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public <T> VisitCode getProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> ref) {
		if (source == forElement) {
			if (prop.equals(direct)) {
				if (forElement.getProperty(direct) == null) {
					DirectLayer l = new OnCanvasLines(forElement).getDirectLayer("fast");
					l.animate.clear();
					forElement.setProperty(direct, l);
					ref.set((T) l, forElement);
				} else {
					ref.set((T) forElement.getProperty(direct), forElement);
				}
				return VisitCode.stop;
			}
		}
		return super.getProperty(source, prop, ref);
	}

	static public void executeStringInElement(VisualElementProperty<String> property, iVisualElement source, String s) {
		try {
			String e = property.get(source);
			if (e == null)
				return;

			Ref<PythonScriptingSystem> refPss = new Ref<PythonScriptingSystem>(null);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(source).getProperty(source, PythonScriptingSystem.pythonScriptingSystem, refPss);
			assert refPss.get() != null;

			Ref<iExecutesPromise> refRunner = new Ref<iExecutesPromise>(null);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(source).getProperty(source, iExecutesPromise.promiseExecution, refRunner);
			assert refRunner.get() != null;

			Promise promise = refPss.get().promiseForKey(source);
			EditorExecutionInterface eei = PythonPluginEditor.editorExecutionInterface.get(source);

			if (promise instanceof DerivativePromise) {
				promise = ((DerivativePromise) promise).getDerivativeWithText(property);
			}

			promise.beginExecute();
			if (eei != null)
				eei.executeFragment(e);
			else
				PythonInterface.getPythonInterface().execString(e);
			promise.endExecute();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static public void executeTweaks(iVisualElement e) {
		executePropertyOfElement(tweak, e);

		PLineList lines = (PLineList) computed_linesToDraw.get(e);
		lines = TweakSplineUI.postProcessLineHook(e, lines);
		computed_linesToDraw.set(e, e, lines);
	}

	static public void executueTweaks(iVisualElement e) {
		executePropertyOfElement(tweak, e);
	}

	static public void fireChange(final iVisualElement e) {

		new TopologySearching.TopologyVisitor_breadthFirst<iVisualElement>(true) {
			@Override
			protected VisitCode visit(iVisualElement root) {
				System.err.println(" fire change on <" + root + "> from <" + e + ">");
				iVisualElementOverrides ov = root.getProperty(iVisualElement.overrides);
				if (root == e)
					return VisitCode.cont;
				if (ov instanceof iChangeParticipant) {
					System.err.println(" will call ");
					long was = ((iChangeParticipant) ov).getHash();
					executePropertyOfElement(onChange, root);
					long now = ((iChangeParticipant) ov).getHash();
					System.err.println(" change ? " + was + " " + now);

					if (ov instanceof SplineComputingOverride)

						((SplineComputingOverride) ov).delayedRepaint();
					if (was != now) {
						return VisitCode.cont;
					}

				}
				return VisitCode.skip;
			}
		}.apply(new iTopology<iVisualElement>() {
			public List<iVisualElement> getChildrenOf(iVisualElement of) {
				VEList p = of.getProperty(computed_elaboratedBy);
				if (p != null)
					return p;
				return Collections.EMPTY_LIST;
			}

			public List<iVisualElement> getParentsOf(iVisualElement of) {
				VEList p = of.getProperty(computed_elaborates);
				if (p != null)
					return p;
				return Collections.EMPTY_LIST;
			}
		}, e);
	}

	static public void mixin(iVisualElement e) {
		new Mixins().mixInOverride(SplineComputingOverride.class, e);
	}

	private TweakSplineUI tweaking;

	SelectionGroup<iComponent> cachedGroup = null;

	SelectionGroup<iComponent> cachedMarkingGroup = null;

	iComponent cachedComponent = null;

	long previousHashSplineHash = 0;

	boolean previousWasSelected = false;

	boolean previousWasFocused = false;

	public VisualElement createSubElaboration(String name, CachedLine line) {

		System.err.println(" creating sub elaboration of ");
		List<iVisualElement> c = forElement.getChildren();

		Rect currentFrame = new Rect(0, 0, 0, 0);
		forElement.getFrame(currentFrame);
		Rect bounds = new Rect(currentFrame.x + currentFrame.w, currentFrame.y, currentFrame.w, currentFrame.h);

		Triple<VisualElement, PlainDraggableComponent, SplineComputingOverride> created = VisualElement.createWithName(bounds, c.get(0), VisualElement.class, PlainDraggableComponent.class, SplineComputingOverride.class, forElement.getProperty(iVisualElement.name) + " -> ");

		VEList e1 = new VEList();
		created.left.setProperty(computed_elaborates, e1);
		e1.add(forElement);
		computed_elaboratedBy.addToList(VEList.class, forElement, (iVisualElement) created.left);

		System.err.println(" initializing python for element ");

		try {
			new LoadInternalWorkspaceFile().copyPlainTextToProperty("SubElaboration_python_source.py", created.left, PythonPlugin.python_source);
			new LoadInternalWorkspaceFile().copyPlainTextToProperty("SubElaboration_tweak.py", created.left, tweak);
			new LoadInternalWorkspaceFile().copyPlainTextToProperty("SubElaboration_onChange.py", created.left, onChange);

		} catch (IOException e) {
			e.printStackTrace();
		}

		created.left.setProperty(new VisualElementProperty("inputSpline_"), line);

		return created.left;

	}

	@Override
	public VisitCode deleted(iVisualElement source) {

		VEList list = computed_elaborates.get(forElement);
		if (list != null)
			if (list.remove(source))
				fireChange();
		list = computed_elaboratedBy.get(forElement);
		if (list != null)
			if (list.remove(source)) {
			}
		return super.deleted(source);
	}

	public void fireAndFireChange() {
		long was = ((iChangeParticipant) this).getHash();
		executePropertyOfElement(onChange, forElement);
		long now = ((iChangeParticipant) this).getHash();
		delayedRepaint();
		fireChange();
	}

	public void fireChange() {
		fireChange(forElement);
	}

	public long getHash() {
		return previousHashSplineHash = longSplineHash(forElement.getProperty(computed_linesToDraw));
	}

	public TweakSplineUI getTweaking() {
		return tweaking;
	}

	@Override
	public VisitCode handleKeyboardEvent(iVisualElement newSource, Event event) {

		if (event == null)
			return VisitCode.cont;

		if (newSource == forElement) {
			if (isSelected()) {
				if (event.character == '[') {
					moveToElaborates(1);
				}
				if (event.character == ']') {
					moveToElaboratedBy(1);
				}
				if (event.character == '{') {
					moveToElaborates(0.1f);
				}
				if (event.character == '}') {
					moveToElaboratedBy(0.1f);
				}
			}
		}

		return super.handleKeyboardEvent(newSource, event);
	}

	@Override
	public VisitCode isHit(iVisualElement source, Event event, Ref<Boolean> is) {
		if (source == forElement) {

			if (!injectOpacity(null)) {
				is.set(false);
				return VisitCode.cont;
			}

			Rect frame = forElement.getFrame(null);
			boolean forbidDrag = false;

			if (frame.isInside(new Vector2(event.x, event.y))) {
				is.set(true);
			} else {
				List<CachedLine> lines = forElement.getProperty(computed_linesToDraw);
				if (lines != null) {
					for (CachedLine l : lines) {
						if (l != null) {
							if (l.getProperties().isTrue(iLinearGraphicsContext.derived, false)) {
							} else if (LineUtils.hitTest(l, new Vector2(event.x, event.y), 5)) {
								is.set(true);
								forbidDrag = true;
							}
						}
					}
				}
			}

			if (tweaking != null && tweaking.somethingSelected())
				is.set(true);

			if (forbidDrag || (tweaking != null && tweaking.somethingSelected())) {
				iComponent c = source.getProperty(iVisualElement.localView);
				if (c instanceof PlainDraggableComponent)
					((PlainDraggableComponent) c).canDrag(false);
			} else {
				iComponent c = source.getProperty(iVisualElement.localView);
				if (c instanceof PlainDraggableComponent)
					((PlainDraggableComponent) c).canDrag(true);
			}

		}
		return VisitCode.cont;
	}

	public boolean isSelected() {
		try {
			final Ref<SelectionGroup<iComponent>> group = new Ref<SelectionGroup<iComponent>>(null);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).getProperty(forElement, iVisualElement.selectionGroup, group);
			final SelectionGroup<iComponent> g = group.get();
			if (g.getSelection().contains(iVisualElement.localView.get(forElement)))
				return true;
			return false;
		} catch (NullPointerException e) {
			return false;
		}
	}

	@Override
	public VisitCode menuItemsFor(iVisualElement source, Map<String, iUpdateable> items) {

		if (source == forElement) {
			final Ref<SelectionGroup<iComponent>> group = new Ref<SelectionGroup<iComponent>>(null);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).getProperty(forElement, iVisualElement.selectionGroup, group);
			final SelectionGroup<iComponent> g = group.get();

			final Ref<SelectionGroup<iComponent>> markingGroup = new Ref<SelectionGroup<iComponent>>(null);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).getProperty(forElement, iVisualElement.markingGroup, group);
			final SelectionGroup<iComponent> mg = group.get();
			items.put("Spline Operations (" + forElement.getProperty(iVisualElement.name) + ")", null);

			// items.put("   \u276f  Make <b>new elaboration</b> of ",
			// new iUpdateable() {
			//
			// public void update() {
			//
			// List<iVisualElement> c = forElement.getChildren();
			//
			// Rect currentFrame = new Rect(0, 0, 0, 0);
			// forElement.getFrame(currentFrame);
			// Rect bounds = new Rect(currentFrame.x +
			// currentFrame.w, currentFrame.y, currentFrame.w,
			// currentFrame.h);
			//
			// Triple<VisualElement, PlainDraggableComponent,
			// SplineComputingOverride> created =
			// VisualElement.createAddAndName(bounds, c.get(0),
			// forElement.getProperty(iVisualElement.name) + " -> ",
			// VisualElement.class, PlainDraggableComponent.class,
			// SplineComputingOverride.class, null);
			//
			// VEList e1 = new VEList();
			// created.left.setProperty(computed_elaborates, e1);
			// e1.add(forElement);
			// computed_elaboratedBy.addToList(VEList.class,
			// forElement, (iVisualElement) created.left);
			//
			// System.err.println(" initializing python for element ");
			//
			// // initialize
			// // python
			//
			// try {
			// new
			// LoadInternalWorkspaceFile().copyPlainTextToProperty("Elaboration_python_source.py",
			// created.left, PythonPlugin.python_source);
			// new
			// LoadInternalWorkspaceFile().copyPlainTextToProperty("Elaboration_tweak.py",
			// created.left, tweak);
			// new
			// LoadInternalWorkspaceFile().copyPlainTextToProperty("Elaboration_onChange.py",
			// created.left, onChange);
			//
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
			//
			// System.err.println(" and selected it");
			// }
			// });

			// commented out, completely broken right now

			new iUpdateable() {

				public void update() {
					PopupTextBox.Modal.getString(PopupTextBox.Modal.elementAt(forElement), "filename : ", "", new iAcceptor<String>() {
						public iAcceptor<String> set(String to) {
							if (to.trim().length() > 0) {
								// saveAsSVG(to);
							}
							return this;
						}
					});
				}
			};
			// items.put("    Save as <b>SVG</b>", );
			items.put("   Save as <b>PDF</b>", new iUpdateable() {

				public void update() {

					// TODO swt filechooser

					FileDialog d = new FileDialog(iVisualElement.enclosingFrame.get(forElement).getFrame(), SWT.SHEET | SWT.SAVE);
					d.setOverwrite(false);
					String name = d.open();

					if (name != null) {
						saveAsPDF(name);
					}

					// JFileChooser chooser = new
					// JFileChooser(new
					// File(System.getProperty("user.home")));
					// Quaqua15TigerLookAndFeelField.deRound(chooser);
					// chooser.setDialogType(JFileChooser.SAVE_DIALOG);
					// chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					// JSheet.showSaveSheet(chooser,
					// iVisualElement.enclosingFrame.get(forElement).getFrame(),
					// new SheetListener() {
					// public void optionSelected(final
					// SheetEvent evt) {
					// if (evt.getOption() ==
					// JFileChooser.APPROVE_OPTION) {
					// File file =
					// evt.getFileChooser().getSelectedFile();
					// saveAsPDF(file.getAbsolutePath());
					// }
					// }
					// });
				}
			});

			final Set<iVisualElement> sp = new HashSet<iVisualElement>();
			if (mg != null) {
				if (mg.getSelection() != null)
					for (iComponent c : mg.getSelection()) {
						iVisualElement q = c.getVisualElement();
						if (q != null) {
							if (q != forElement)
								if (q.getProperty(iVisualElement.overrides) instanceof SplineComputingOverride) {
									sp.add(q);
								}
						}
					}
			}

			// if (sp.size() > 0) {
			// items.put("   \\u2295  Add marked element" +
			// (sp.size() > 1 ? "s" : " ( " +
			// sp.iterator().next().getProperty(iVisualElement.name)
			// + " )") + " as elaboration of", new iUpdateable() {
			// public void update() {
			//
			// for (iVisualElement e : sp) {
			// computed_elaboratedBy.addToList(VEList.class,
			// forElement, e);
			// computed_elaborates.addToList(VEList.class, e,
			// forElement);
			//
			// fireAndFireChange();
			// }
			// }
			// });
			// items.put("   \u2296  Remove marked element" +
			// (sp.size() > 1 ? "s" : " ( " +
			// sp.iterator().next().getProperty(iVisualElement.name)
			// + " )") + " from the elaboration of", new
			// iUpdateable() {
			// public void update() {
			//
			// for (iVisualElement e : sp) {
			// VEList elaborates = computed_elaborates.get(e);
			// if (elaborates != null &&
			// elaborates.contains(forElement)) {
			// elaborates.remove(forElement);
			// computed_elaboratedBy.get(forElement).remove(e);
			// }
			// fireAndFireChange();
			// }
			// }
			// });
			// items.put("   \u2295  Add marked element" +
			// (sp.size() > 1 ? "s" : " ( " +
			// sp.iterator().next().getProperty(iVisualElement.name)
			// + " )") + " as elaborant of", new iUpdateable() {
			// public void update() {
			//
			// for (iVisualElement e : sp) {
			// computed_elaborates.addToList(VEList.class,
			// forElement, e);
			// computed_elaboratedBy.addToList(VEList.class, e,
			// forElement);
			// fireAndFireChange();
			// }
			// }
			// });
			// items.put("   \u2296  Remove marked element" +
			// (sp.size() > 1 ? "s" : " ( " +
			// sp.iterator().next().getProperty(iVisualElement.name)
			// + " )") + " from the elaborant of", new iUpdateable()
			// {
			// public void update() {
			//
			// for (iVisualElement e : sp) {
			// VEList elaborates = computed_elaboratedBy.get(e);
			// if (elaborates != null &&
			// elaborates.contains(forElement)) {
			// elaborates.remove(forElement);
			// computed_elaborates.get(forElement).remove(e);
			// }
			// fireAndFireChange();
			// }
			// }
			// });
			// }

			// VEList q = computed_elaboratedBy.get(forElement);
			// if (q != null && q.size() != 0 && g != null) {
			// items.put("Spline Operations \u2014 Outputs (" +
			// forElement.getProperty(iVisualElement.name) + ")",
			// null);
			//
			// for (final iVisualElement e : q) {
			// items.put("   \u276f  <b>Select elaboration<b> \"" +
			// e.getProperty(iVisualElement.name) + "\"", new
			// iUpdateable() {
			//
			// public void update() {
			// g.deselectAll();
			// g.addToSelection(e.getProperty(iVisualElement.localView));
			// global_opacity.set(e, e, 1f);
			// }
			// });
			// }
			// for (final iVisualElement e : q) {
			// items.put("   \u276f  Select elaboration \"" +
			// e.getProperty(iVisualElement.name) +
			// "\" <b>focus, and hide this</b>", new iUpdateable() {
			//
			// public void update() {
			// g.deselectAll();
			// g.addToSelection(e.getProperty(iVisualElement.localView));
			// global_opacity.set(e, e, 1f);
			// global_opacity.set(forElement, forElement, 0f);
			// }
			// });
			// }
			// for (final iVisualElement e : q) {
			// items.put("   \u276f  Select elaboration \"" +
			// e.getProperty(iVisualElement.name) +
			// "\" focus, and <b>ghost</b> this", new iUpdateable()
			// {
			//
			// public void update() {
			// g.deselectAll();
			// g.addToSelection(e.getProperty(iVisualElement.localView));
			// global_opacity.set(e, e, 1f);
			// global_opacity.set(forElement, forElement, 0.1f);
			// }
			// });
			// }
			// }
			// q = computed_elaborates.get(forElement);
			// if (q != null && q.size() != 0 && g != null) {
			// items.put("Spline Operations \u2014 Inputs (" +
			// forElement.getProperty(iVisualElement.name) + ")",
			// null);
			//
			// for (final iVisualElement e : q) {
			// items.put("   \u2770  <b>Select elaborant</b> \"" +
			// e.getProperty(iVisualElement.name) + "\"", new
			// iUpdateable() {
			//
			// public void update() {
			// g.deselectAll();
			// g.addToSelection(e.getProperty(iVisualElement.localView));
			// global_opacity.set(e, e, 1f);
			// }
			// });
			// }
			// for (final iVisualElement e : q) {
			// items.put("   \u2770  Select elaborant \"" +
			// e.getProperty(iVisualElement.name) +
			// "\" <b>focus, and hide this</b>", new iUpdateable() {
			//
			// public void update() {
			// g.deselectAll();
			// g.addToSelection(e.getProperty(iVisualElement.localView));
			// global_opacity.set(e, e, 1f);
			// global_opacity.set(forElement, forElement, 0f);
			// }
			// });
			// }
			// for (final iVisualElement e : q) {
			// items.put("   \u2770  Select elaborant \"" +
			// e.getProperty(iVisualElement.name) +
			// "\" focus, and <b>ghost</b> this", new iUpdateable()
			// {
			//
			// public void update() {
			// g.deselectAll();
			// g.addToSelection(e.getProperty(iVisualElement.localView));
			// global_opacity.set(e, e, 1f);
			// global_opacity.set(forElement, forElement, 0.1f);
			// }
			// });
			// }
			// }
		}

		return super.menuItemsFor(source, items);
	}

	HashMap<CachedLine, CachedLine> offsetCache = new HashMap<CachedLine, CachedLine>();
	Rect offsetCachedAt = null;
	float offsetOpacityAt = 1f;

	@Override
	public VisitCode paintNow(iVisualElement source, Rect bounds, boolean visible) {
		if (source == forElement) {
			// workaround for #54
			if (source.getProperty(OfferedAlignment.alignment_doNotParticipate) == null) {
				OfferedAlignment.alignment_doNotParticipate.set(source, source, true);
			}

			try {

				List<iUpdateable> cc = computed_drawingInstructions.get(source);
				if (cc != null)
					for (iUpdateable c : cc)
						c.update();

				SelectionGroup<iComponent> group = getGroup();
				boolean isSelected = false;
				if (group != null) {
					isSelected = group.getSelection().contains(cachedComponent);
				}
				group = getMarkingGroup();
				boolean isMarked = false;
				if (group != null) {
					isMarked = group.getSelection().contains(cachedComponent);
				}

				Ref<List<CachedLine>> ref = new Ref<List<CachedLine>>(null);

				Rect currentFrame = new Rect(0, 0, 0, 0);
				source.getFrame(currentFrame);
				Rect newFrame = new Rect(0, 0, 0, 0).setValue(currentFrame);

				getLinesToDraw(ref);

				Boolean bfocused = PlainDraggableComponent.hasMouseFocus.get(source);
				boolean focused = bfocused == null ? false : bfocused;
				focused = focused & !isSelected;

				if (isSelected && !previousWasSelected) {
					new VisualElementProperty<Boolean>("isSelected_").set(source, source, isSelected);
					executePropertyOfElement(onSelection, source);
					if (ref.get() != null) {
						List<CachedLine> instructions = ref.get();
						if (tweaking != null)
							tweaking.deinstallMousePeer(forElement);
						tweaking = new TweakSplineUI(instructions).copyFrom(tweaking);
						tweaking.installMousePeer(forElement);
					}
				} else if (!isSelected && previousWasSelected) {
					new VisualElementProperty<Boolean>("isSelected_").set(source, source, isSelected);
					executePropertyOfElement(onSelection, source);
					if (tweaking != null) {
						tweaking.deinstallMousePeer(forElement);
						tweaking = null;
					}
				}
				previousWasSelected = isSelected;

				if (tweaking != null && tweaking.needsRecomputation()) {
					executePropertyOfElement(PythonPlugin.python_source, forElement);
					;//System.out.println(" post line ...");
					TweakSplineUI.postProcessLineHook(forElement, (PLineList) ref.get());
				}

				boolean forceNew = false;

				if (ref.get() != null) {
					List<CachedLine> instructions = ref.get();
					if (longSplineHash(instructions) != previousHashSplineHash) {

						if (isSelected && instructions != null) {
							if (tweaking != null)
								tweaking.deinstallMousePeer(forElement);
							tweaking = new TweakSplineUI(instructions).copyFrom(tweaking);
							tweaking.installMousePeer(forElement);
						}
						if (shouldAutoComputeRect.getFloat(forElement, 1) > 0) {
							newFrame = recomputeBounds(instructions, currentFrame);
							source.setFrame(newFrame);
							currentFrame.setValue(newFrame);
						}
						previousHashSplineHash = longSplineHash(instructions);
						forceNew = true;
					}
				}

				// forceNew |= (previousWasFocused != focused);

				if (ref.get() != null) {
					List<CachedLine> instructions = ref.get();

					if (tweaking != null) {
						instructions = tweaking.getOutput();
					}

					if (GLComponentWindow.currentContext != null) {
						int n = 0;
						Number g = forElement.getProperty(global_opacity);
						if (g == null)
							g = 1f;
						if (offsetCachedAt == null || !offsetCachedAt.equals(bounds) || !g.equals((Float) offsetOpacityAt)) {
							offsetCachedAt = new Rect(bounds);
							offsetOpacityAt = g.floatValue();
							offsetCache = new HashMap<CachedLine, CachedLine>();
						}

						for (CachedLine c : instructions) {

							if (c != null) {
								injectOpacity(c);
								n += c.events.size();
								if (n < 100)
									injectFocused(c, focused);
								else
									injectFocused(c, false);

								boolean fn = c.getProperties().isTrue(iLinearGraphicsContext.forceNew, false);

								iLinearGraphicsContext context = c.getProperties().get(iLinearGraphicsContext.context);
								if (context == null)
									context = GLComponentWindow.currentContext;
								else {
									// ;//System.out.println("submitting to alternative context");
								}

								Vector2 offset = c.getProperties().get(iLinearGraphicsContext.offsetFromSource);

								if (offset != null) {
									CachedLine previous = offsetCache.get(c);
									if (previous == null || fn) {
										CachedLine c2 = LineUtils.transformLineOffsetFrom(c, source.getFrame(null), offset);
										offsetCache.put(c, c2);
										c = c2;
									} else {
										c = previous;
									}

								}

								if (fn || forceNew || (previousWasFocused != focused && n < 100))
									context.resubmitLine(c, c.getProperties());
								else
									context.submitLine(c, c.getProperties());

								if (fn)
									c.getProperties().remove(iLinearGraphicsContext.forceNew);

							}
							n++;
						}
					} else
						System.err.println(" No drawing context for window ? ");
				}

				previousWasFocused = focused;

				if (isSelected) {
					CachedLine selectedLine = new CachedLine();
					iLine in = selectedLine.getInput();
					in.moveTo((float) currentFrame.x, (float) currentFrame.y);
					in.lineTo((float) (currentFrame.x + currentFrame.w), (float) currentFrame.y);
					in.lineTo((float) (currentFrame.x + currentFrame.w), (float) (currentFrame.y + currentFrame.h));
					in.lineTo((float) (currentFrame.x), (float) (currentFrame.y + currentFrame.h));
					in.lineTo((float) (currentFrame.x), (float) currentFrame.y);
					selectedLine.getProperties().put(iLinearGraphicsContext.thickness, 1f * (focused ? 15 : 1));
					selectedLine.getProperties().put(iLinearGraphicsContext.color, new Vector4(0.0f, 0.0f, 0.0f, 0.6f * (focused ? 0.5f : 1)));
					selectedLine.getProperties().put(iLinearGraphicsContext.stroked, true);
					selectedLine.getProperties().put(iLinearGraphicsContext.notForExport, true);
					injectOpacity(selectedLine);

					if (GLComponentWindow.currentContext != null)
						GLComponentWindow.currentContext.submitLine(selectedLine, selectedLine.getProperties());

					if (tweaking != null)
						tweaking.paint();

				}
				if (isMarked) {
					CachedLine selectedLine = new CachedLine();
					iLine in = selectedLine.getInput();
					in.moveTo((float) currentFrame.x - 5, (float) currentFrame.y - 5);
					in.lineTo((float) (currentFrame.x + currentFrame.w + 5), (float) currentFrame.y - 5);
					in.lineTo((float) (currentFrame.x + currentFrame.w + 5), (float) (currentFrame.y + currentFrame.h + 5));
					in.lineTo((float) (currentFrame.x - 5), (float) (currentFrame.y + currentFrame.h + 5));
					in.close();
					injectOpacity(selectedLine);
					selectedLine.getProperties().put(iLinearGraphicsContext.thickness, 1f * (focused ? 15 : 1));
					selectedLine.getProperties().put(iLinearGraphicsContext.color, new Vector4(1.0f, 0.0f, 0.0f, 0.6f * (focused ? 0.5f : 1)));
					selectedLine.getProperties().put(iLinearGraphicsContext.notForExport, true);
					if (GLComponentWindow.currentContext != null)
						GLComponentWindow.currentContext.submitLine(selectedLine, selectedLine.getProperties());

					VEList el = computed_elaboratedBy.get(source);
					if (el != null) {
						for (iVisualElement ve : el) {
							CachedLine elBy = new CachedLine();
							iLine elByLine = elBy.getInput();

							Rect toFrame = ve.getFrame(null);

							elByLine.moveTo((float) (currentFrame.x + currentFrame.w / 2), (float) (currentFrame.y + currentFrame.h / 2));
							elByLine.lineTo((float) (toFrame.x + toFrame.w / 2), (float) (toFrame.y + toFrame.h / 2));
							elByLine.close();
							injectOpacity(elBy);
							elBy.getProperties().put(iLinearGraphicsContext.thickness, 1f);
							elBy.getProperties().put(iLinearGraphicsContext.color, new Vector4(1.0f, 0.0f, 0.0f, 0.2f));

							elBy.getProperties().put(iLinearGraphicsContext.notForExport, true);
							if (GLComponentWindow.currentContext != null)
								GLComponentWindow.currentContext.submitLine(elBy, elBy.getProperties());

							// CachedLine elArrow =
							// new
							// Arrows().getArrowForMiddle(elBy,
							// 10, 0);
							// elArrow.getProperties().put(iLinearGraphicsContext.thickness,
							// 1f);
							// elArrow.getProperties().put(iLinearGraphicsContext.color,
							// new Vector4(1.0f,
							// 0.0f, 0.0f, 0.2f));
							// elArrow.getProperties().put(iLinearGraphicsContext.filled,
							// true);
							// elArrow.getProperties().put(iLinearGraphicsContext.notForExport,
							// true);
							// if
							// (GLComponentWindow.currentContext
							// != null)
							// GLComponentWindow.currentContext.submitLine(elArrow,
							// elArrow.getProperties());
						}
					}

				} else if (!isSelected && !isMarked && !noFrame.getBoolean(forElement, false)) {

					{

						CachedLine selectedLine = new CachedLine();
						iLine in = selectedLine.getInput();
						in.moveTo((float) currentFrame.x, (float) currentFrame.y);
						in.lineTo((float) (currentFrame.x + currentFrame.w), (float) currentFrame.y);
						in.lineTo((float) (currentFrame.x + currentFrame.w), (float) (currentFrame.y + currentFrame.h));
						in.lineTo((float) (currentFrame.x), (float) (currentFrame.y + currentFrame.h));
						in.close();
						injectOpacity(selectedLine);
						selectedLine.getProperties().put(iLinearGraphicsContext.thickness, 1f * (focused ? 15 : 1));
						selectedLine.getProperties().put(iLinearGraphicsContext.color, new Vector4(0.0f, 0.0f, 0.0f, 0.3f * (focused ? 0.5f : 1)));
						boolean isNoFrame = noFrame.getBoolean(forElement, false);

						selectedLine.getProperties().put(iLinearGraphicsContext.filled, !isNoFrame);
						selectedLine.getProperties().put(iLinearGraphicsContext.fillColor, new Vector4(0.67, 0.67, 0.67, 0.15f));
						selectedLine.getProperties().put(iLinearGraphicsContext.notForExport, true);
						in.moveTo((float) (currentFrame.x + currentFrame.w / 2), (float) (currentFrame.y + currentFrame.h + 5));
						selectedLine.getProperties().put(iLinearGraphicsContext.containsText, !isNoFrame);
						in.setPointAttribute(iLinearGraphicsContext.text_v, source.getProperty(iVisualElement.name));
						// in.setPointAttribute(iLinearGraphicsContext.font_v,
						// new
						// Font(Constants.defaultFont,
						// 0, 8));
						in.setPointAttribute(iLinearGraphicsContext.fillColor_v, new Vector4(0, 0, 0, 1f));
						in.setPointAttribute(iLinearGraphicsContext.alignment_v, 0f);
						if (GLComponentWindow.currentContext != null && !isNoFrame)
							GLComponentWindow.currentContext.submitLine(selectedLine, selectedLine.getProperties());
					}
				}

				if (focused)
					wasFocused();
				else
					wasntFocused();

			} catch (Throwable ex) {
				ex.printStackTrace();
			}
			frame++;
		}
		return visible ? VisitCode.cont : VisitCode.stop;
	}

	protected void getLinesToDraw(Ref<List<CachedLine>> ref) {
		List<CachedLine> g = computed_linesToDraw.get(forElement);
		ref.set(g);

		if (ref.get() == null) {
			initializeDrawngInstructions(ref);
			if (ref.get() != null) {
				computed_linesToDraw.set(forElement, forElement, ref.get());
			}
		}
	}

	protected void wasntFocused() {
	}

	protected void wasFocused() {
	}

	public Rect recomputeBounds(List<CachedLine> instructions, Rect currentFrame) {

		float minx = Float.POSITIVE_INFINITY;
		float miny = Float.POSITIVE_INFINITY;
		float maxx = Float.NEGATIVE_INFINITY;
		float maxy = Float.NEGATIVE_INFINITY;

		boolean touched = false;

		for (CachedLine c : instructions) {
			Vector2[] bounds = LineUtils.fastBoundsMoveTos(c);

			if (c == null)
				continue;

			if (c.getProperties().get(iLinearGraphicsContext.offsetFromSource) != null)
				return currentFrame;

			if (bounds != null) {
				touched = true;
				if (bounds[0].x < minx)
					minx = bounds[0].x;
				if (bounds[0].y < miny)
					miny = bounds[0].y;
				if (bounds[1].x > maxx)
					maxx = bounds[1].x;
				if (bounds[1].y > maxy)
					maxy = bounds[1].y;
				break;
			}
		}

		Rect r = forElement.getProperty(computed_rect);
		if (r != null)
			return r;

		return touched ? new Rect(minx - 10, miny - 10, maxx - minx + 20, maxy - miny + 20) : currentFrame;
	}

	public void saveAsPDF(String to) {
		PythonInterface.getPythonInterface().setVariable("__s", Py.java2py(forElement));
		PythonInterface.getPythonInterface().execString("makePDF(geometry=__s.lines, filename=\"" + to + "\")");
		OverlayAnimationManager.notifyAsText(forElement, "saved '" + forElement.getProperty(iVisualElement.name) + "' to '" + to + "'", null);

		// List<CachedLine> g = computed_linesToDraw.get(forElement);
		//
		// BasePDFGraphicsContext tc = new BasePDFGraphicsContext();
		// tc.install(null, new File(to));
		// new SimplePDFLineDrawing().installInto(tc);
		//
		// if (g != null) {
		// for (CachedLine c : g) {
		// if (c != null) {
		// System.err.println(" ----------------------------------------------- saving line <"
		// + c.events + ">");
		// tc.submitLine(c, c.getProperties());
		// }
		// }
		// }
		// tc.finish();
	}

	// public void saveAsSVG(String to) {
	//
	// List<CachedLine> g = computed_linesToDraw.get(forElement);
	//
	// BaseSVGGraphicsContext tc = new BaseSVGGraphicsContext();
	// tc.install(null, new File(to));
	// new SimpleSVGLineDrawing().installInto(tc);
	//
	// if (g != null) {
	// for (CachedLine c : g) {
	// if (c != null) {
	// System.err.println(" ----------------------------------------------- saving line <"
	// + c.events + ">");
	// tc.submitLine(c, c.getProperties());
	// }
	// }
	// }
	// tc.finish();
	// }

	@Override
	public <T> VisitCode setProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> to) {
		if (source == forElement && prop.equals(computed_rect)) {
			Rect r = (Rect) to.get();
			if (r != null) {
				source.setFrame(r);
			}
		} else if (source == forElement && prop.equals(global_opacity)) {
			previousHashSplineHash = -1;
		} else if (source == forElement && prop.equals(PlainDraggableComponent.hasMouseFocus)) {
			delayedRepaint();
		}
		return super.setProperty(source, prop, to);
	}

	@Override
	public VisitCode shouldChangeFrame(iVisualElement source, Rect newFrame, Rect oldFrame, boolean now) {

		if (source == forElement) {
			;//System.out.println(" should change frame from <" + oldFrame + " -> " + newFrame + ">");
			new VisualElementProperty<Rect>("oldFrame_").set(source, source, oldFrame);
		}
		VisitCode c = super.shouldChangeFrame(source, newFrame, oldFrame, now);
		if (source == forElement) {

			;//System.out.println(" source <" + source + "> <" + newFrame + "> <" + oldFrame + ">");
			new VisualElementProperty<Rect>("newFrame_").set(source, source, newFrame);
			executePropertyOfElement(onFrameChange, source);
		}
		return c;
	}

	private long longSplineHash(List<CachedLine> instructions) {
		int hashCode = 1;
		// if (true)
		// return 0;
		Iterator<CachedLine> i = instructions.iterator();
		while (i.hasNext()) {
			CachedLine obj = (CachedLine) filter(i.next());
			hashCode = 31 * hashCode + (obj == null ? 0 : System.identityHashCode(obj));
		}
		return hashCode;
	}

	@NextUpdate(delay = 1)
	private void moveToElaboratedBy(float thisOpacity) {
		final Ref<SelectionGroup<iComponent>> group = new Ref<SelectionGroup<iComponent>>(null);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).getProperty(forElement, iVisualElement.selectionGroup, group);
		final SelectionGroup<iComponent> g = group.get();

		VEList q = computed_elaboratedBy.get(forElement);
		if (q != null) {
			g.deselectAll();
			for (final iVisualElement e : q) {
				if (e != forElement) {
					g.addToSelection(e.getProperty(iVisualElement.localView));
					global_opacity.set(e, e, 1f);
					global_opacity.set(forElement, forElement, thisOpacity);
				}
			}
		}
	}

	@NextUpdate(delay = 1)
	private void moveToElaborates(float thisOpacity) {
		final Ref<SelectionGroup<iComponent>> group = new Ref<SelectionGroup<iComponent>>(null);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).getProperty(forElement, iVisualElement.selectionGroup, group);
		final SelectionGroup<iComponent> g = group.get();

		VEList q = computed_elaborates.get(forElement);
		if (q != null) {
			g.deselectAll();
			for (final iVisualElement e : q) {
				if (e != forElement) {
					g.addToSelection(e.getProperty(iVisualElement.localView));
					global_opacity.set(e, e, 1f);
					global_opacity.set(forElement, forElement, thisOpacity);
				}
			}
		}
	}

	@NextUpdate(delay = 2)
	protected void delayedRepaint() {
		RootComponent q = iVisualElement.rootComponent.get(forElement);
		if (q != null)
			q.repaint();
	}

	protected Object filter(Object o) {
		if (o instanceof PyObject) {
			PyObject ll = ((PyObject) o).__getattr__("line");
			if (ll != null) {
				Object cl = ll.__tojava__(CachedLine.class);
				return cl;
			} else {
				System.err.println(" bad entry into line list <" + o + ">");
				return null;
			}
		} else
			return o;
	}

	protected SelectionGroup<iComponent> getGroup() {
		if (cachedGroup == null) {
			cachedGroup = iVisualElement.selectionGroup.get(forElement);
		}
		if (cachedComponent == null) {
			cachedComponent = forElement.getProperty(iVisualElement.localView);
		}
		return cachedGroup;
	}

	protected SelectionGroup<iComponent> getMarkingGroup() {
		if (cachedMarkingGroup == null) {
			cachedMarkingGroup = iVisualElement.markingGroup.get(forElement);
		}
		if (cachedComponent == null) {
			cachedComponent = forElement.getProperty(iVisualElement.localView);
		}
		return cachedMarkingGroup;
	}

	@SuppressWarnings("unchecked")
	protected void initializeDrawngInstructions(Ref<List<CachedLine>> ref) {
		ref.set(new PLineList());

		if (shouldAutoComputeRect.get(forElement) == null) {
			forElement.setProperty(shouldAutoComputeRect, true);
		}

	}

	protected boolean injectFocused(CachedLine c, boolean f) {
		if (f) {
			if (c.getProperties().isTrue(iLinearGraphicsContext.noHit, false)) {
			} else {
				c.getProperties().put(iLinearGraphicsContext.shouldHighlight, true);
			}
		} else
			c.getProperties().put(iLinearGraphicsContext.shouldHighlight, false);
		return true;
	}

	protected boolean injectOpacity(CachedLine c) {
		if (c != null)
			c.getProperties().put(iLinearGraphicsContext.source, forElement);
		Number g = forElement.getProperty(global_opacity);
		if (g != null) {
			if (c != null)
				c.getProperties().put(iLinearGraphicsContext.totalOpacity, g);
			return g.floatValue() > 0;
		}
		return true;
	}

	protected void removeThisElement() {
		Launcher.getLauncher().registerUpdateable(new iUpdateable() {
			public void update() {
				Launcher.getLauncher().deregisterUpdateable(this);
				new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).deleted(forElement);
				new iVisualElementOverrides.MakeDispatchProxy().getBackwardsOverrideProxyFor(forElement).deleted(forElement);
				for (iVisualElement ve : new ArrayList<iVisualElement>((Collection<iVisualElement>) forElement.getParents())) {
					ve.removeChild(forElement);
				}
				for (iVisualElement ve : new ArrayList<iVisualElement>(forElement.getChildren())) {
					forElement.removeChild(ve);
				}
			}
		});
	}

	public static void executeMainWithLabel(iVisualElement source, String label) {

		try {
			String e = PythonPlugin.python_source.get(source);
			if (e == null)
				return;

			Ref<PythonScriptingSystem> refPss = new Ref<PythonScriptingSystem>(null);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(source).getProperty(source, PythonScriptingSystem.pythonScriptingSystem, refPss);
			assert refPss.get() != null;

			Ref<iExecutesPromise> refRunner = new Ref<iExecutesPromise>(null);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(source).getProperty(source, iExecutesPromise.promiseExecution, refRunner);
			assert refRunner.get() != null;

			Promise promise = refPss.get().promiseForKey(source);

			String string = new ExecutableAreaFinder().findExecutableSubstring(promise.getText(), label);

			;//System.out.println(" found and about to execute <" + string + ">");

			promise.beginExecute();
			PythonInterface.getPythonInterface().execString(string);
			promise.endExecute();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}