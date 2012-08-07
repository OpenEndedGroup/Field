package field.core.plugins.python;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.python.core.Py;
import org.python.core.PyFunction;
import org.python.core.PyObject;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.Platform;
import field.core.StandardFluidSheet;
import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElementOverrides.Ref;
import field.core.execution.PythonGeneratorStack;
import field.core.execution.PythonInterface;
import field.core.execution.PythonScriptingSystem.DerivativePromise;
import field.core.execution.PythonScriptingSystem.Promise;
import field.core.execution.TimeMarker;
import field.core.plugins.drawing.OverDrawing;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.help.ContextualHelp;
import field.core.plugins.help.HelpBrowser;
import field.core.plugins.history.VersionMenu;
import field.core.plugins.log.Logging;
import field.core.plugins.pseudo.PseudoPropertiesPlugin;
import field.core.plugins.pseudo.PseudoPropertiesPlugin.Beginner;
import field.core.plugins.pseudo.PseudoPropertiesPlugin.Ender;
import field.core.plugins.selection.SelectionSetDriver;
import field.core.ui.BetterComboBox;
import field.core.ui.GraphNodeToTreeFancy;
import field.core.ui.NewTemplates;
import field.core.ui.text.GlobalKeyboardShortcuts;
import field.core.ui.text.PythonTextEditor;
import field.core.ui.text.PythonTextEditor.EditorExecutionInterface;
import field.core.ui.text.StyledTextUndo;
import field.core.ui.text.StyledTextUndo.Memo;
import field.core.ui.text.embedded.CustomInsertDrawing;
import field.core.ui.text.embedded.CustomInsertSystem;
import field.core.ui.text.embedded.CustomInsertSystem.ProvidedComponent;
import field.core.ui.text.embedded.CustomInsertSystem.iPossibleComponent;
import field.core.ui.text.embedded.ExecutableAreaFinder;
import field.core.ui.text.embedded.TextAnnotations;
import field.core.ui.text.embedded.TripleQuote;
import field.core.ui.text.rulers.ExecutedAreas;
import field.core.ui.text.rulers.ExecutedAreas.Area;
import field.core.ui.text.rulers.ExecutedAreas.State;
import field.core.ui.text.rulers.ExecutionRuler;
import field.core.ui.text.rulers.OpportinisticSlider;
import field.core.ui.text.util.OpenInEclipse;
import field.core.ui.text.util.OpenInTrac;
import field.core.util.LocalFuture;
import field.core.util.PythonCallableMap;
import field.core.util.StringAdaptations;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.DraggableComponent;
import field.core.windowing.components.MainSelectionGroup;
import field.core.windowing.components.PlainDraggableComponent;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.iComponent;
import field.core.windowing.overlay.OverlayAnimationManager;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.abstraction.iProvider;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.linalg.Vector4;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.Generics.Pair;
import field.namespace.generic.Generics.Triple;
import field.util.Dict.Prop;
import field.util.TaskQueue;

@Woven
public class PythonPluginEditor extends PythonPlugin {

	public class InsertPossibleComponentHere implements iUpdateable {

		private final iPossibleComponent pc;

		public InsertPossibleComponentHere(iPossibleComponent pc) {
			this.pc = pc;
		}

		public void update() {
			try {
				ProvidedComponent component = pc.clazz.newInstance();
				pc.prep(component, currentlyEditing, editor.getInputEditor().getSelectionText());
				ProvidedComponent added = customInsertSystem.addComponent(component);
				String insert = customInsertSystem.getStringForComponent(added);

				int sstart = editor.getInputEditor().getSelectionRanges()[0];
				int send = editor.getInputEditor().getSelectionRanges()[1] + sstart;
				boolean isOverwrite = send > sstart;
				if (send > sstart)
					editor.getInputEditor().replaceTextRange(sstart, send - sstart, "");

				editor.getInputEditor().getContent().replaceTextRange(editor.getInputEditor().getCaretOffset(), 0, insert);

				// editor.getInputDocument().insertString(
				// editor.getInputEditor().getCaretPosition(),
				// insert,
				// editor.getInputDocument().getStyle("regular"));

				customInsertSystem.updateAllStyles(editor.getInputEditor(), currentlyEditing);

				if (!isOverwrite) {
					editor.getInputEditor().setCaretOffset(editor.getInputEditor().getCaretOffset() + insert.length());
					editor.getInputEditor().insert("\n");
				}
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	public class InsertPossibleWrapHere implements iUpdateable {

		private final Triple<String, iPossibleComponent, iPossibleComponent> pc;

		public InsertPossibleWrapHere(Triple<String, iPossibleComponent, iPossibleComponent> pc) {
			this.pc = pc;
		}

		public void update() {
			try {
				Point rr = editor.getInputEditor().getSelectionRange();
				{
					ProvidedComponent component = pc.right.clazz.newInstance();
					pc.right.prep(component, currentlyEditing, editor.getInputEditor().getSelectionText());
					ProvidedComponent added = customInsertSystem.addComponent(component);
					String insert = "\n" + customInsertSystem.getStringForComponent(added) + "\n";

					editor.getInputEditor().getContent().replaceTextRange(rr.x + rr.y, 0, insert);

					// editor.getInputDocument().insertString(editor.getInputEditor().getSelectionEnd(),
					// insert,
					// editor.getInputDocument().getStyle("regular"));
				}
				{
					ProvidedComponent component = pc.middle.clazz.newInstance();
					pc.middle.prep(component, currentlyEditing, editor.getInputEditor().getSelectionText());
					ProvidedComponent added = customInsertSystem.addComponent(component);
					String insert = customInsertSystem.getStringForComponent(added) + "\n";

					editor.getInputEditor().getContent().replaceTextRange(rr.x, 0, insert);
					// editor.getInputDocument().insertString(editor.getInputEditor().getSelectionStart(),
					// insert,
					// editor.getInputDocument().getStyle("regular"));
				}
				customInsertSystem.updateAllStyles(editor.getInputEditor(), currentlyEditing);

			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	public class LocalEditablelPromise extends LocalPromise implements Promise, DerivativePromise {

		public LocalEditablelPromise(iVisualElement element) {
			super(element);
		}

		@Override
		public void beginExecute() {
			// ExecutionMonitor.pane = editor.getInputEditor();
			PythonInterface.getPythonInterface().pushOutput(editor.getOutput(), editor.getErrorOutput());
			super.beginExecute();
		}

		@Override
		public void endExecute() {
			super.endExecute();
			PythonInterface.getPythonInterface().popOutput();
		}

		@Override
		public Promise getDerivativeWithText(final VisualElementProperty<String> prop) {
			LocalEditablelPromise p2 = new LocalEditablelPromise(element);
			p2.property = prop;
			return p2;
		}

		@Override
		public String getText() {
			if (currentlyEditing == element && currentlyEditingProperty == python_source) {
				iVisualElementOverrides.topology.begin(lve);
				insideUpdate = true;
				try {
					iVisualElementOverrides.forward.setProperty.setProperty(element, python_source_forExecution, new Ref<String>(stringAtSwapIn));
				} finally {
					insideUpdate = false;
					iVisualElementOverrides.topology.end(lve);
				}
			} else if (currentlyEditing == element && currentlyEditing == property) {
				return stringAtSwapIn;
			}
			return super.getText();
		}

	}

	NewTemplates templates;

	public class MenuOverrides extends Overrides {
		@Override
		public <T> VisitCode getProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> ref) {
			if (prop.equals(OutputInsertsOnSheet.outputInsertsOnSheet)) {
				if (ref.get() == null) {
					OutputInsertsOnSheet oios = new OutputInsertsOnSheet(source);
					source.setProperty(OutputInsertsOnSheet.outputInsertsOnSheet, oios);
					ref.set((T) oios);
					return VisitCode.stop;
				}
			}
			return super.getProperty(source, prop, ref);
		}

		@Override
		public VisitCode paintNow(iVisualElement source, Rect bounds, boolean visible) {
			Boolean group = source.getProperty(python_isDefaultGroup);
			if (group != null && group) {
				if (GLComponentWindow.currentContext != null && GLComponentWindow.draft) {
					CachedLine l = new CachedLine();
					l.getInput().moveTo((float) (bounds.x + bounds.w - 87), (float) (bounds.y + bounds.h + 5));
					l.getInput().setPointAttribute(iLinearGraphicsContext.text_v, " default super-element ");
					l.getInput().setPointAttribute(iLinearGraphicsContext.textIsBlured_v, true);
					l.getInput().setPointAttribute(iLinearGraphicsContext.font_v, new java.awt.Font("Gill Sans", java.awt.Font.ITALIC, 10));
					l.getInput().setPointAttribute(iLinearGraphicsContext.fillColor_v, new Vector4(0, 0, 0, 0.3f));
					l.getProperties().put(iLinearGraphicsContext.containsText, true);
					GLComponentWindow.currentContext.submitLine(l, l.getProperties());
				}
			}
			if (source != null && ExecutionRuler.hasUnitTests(source, python_source)) {
				int num = ExecutionRuler.hasFailingTests(source, python_source);
				if (num > 0) {
					{
						CachedLine l = new CachedLine();
						l.getInput().moveTo((float) (bounds.x + bounds.w - 20), (float) (bounds.y - 3));
						l.getInput().setPointAttribute(iLinearGraphicsContext.text_v, " \u24e4");
						l.getInput().setPointAttribute(iLinearGraphicsContext.textIsBlured_v, false);
						l.getInput().setPointAttribute(iLinearGraphicsContext.font_v, new java.awt.Font("Gill Sans", java.awt.Font.BOLD, 25));
						l.getInput().setPointAttribute(iLinearGraphicsContext.fillColor_v, new Vector4(1, 0, 0, 0.85f));
						l.getProperties().put(iLinearGraphicsContext.containsText, true);
						GLComponentWindow.currentContext.submitLine(l, l.getProperties());
					}
					{
						CachedLine l = new CachedLine();
						l.getInput().moveTo((float) (bounds.x + bounds.w + 12), (float) (bounds.y - 3));
						l.getInput().setPointAttribute(iLinearGraphicsContext.text_v, "" + num);
						l.getInput().setPointAttribute(iLinearGraphicsContext.textIsBlured_v, false);
						l.getInput().setPointAttribute(iLinearGraphicsContext.font_v, new java.awt.Font("Gill Sans", java.awt.Font.BOLD, 20));
						l.getInput().setPointAttribute(iLinearGraphicsContext.fillColor_v, new Vector4(1, 0, 0, 0.85f));
						l.getProperties().put(iLinearGraphicsContext.containsText, true);
						GLComponentWindow.currentContext.submitLine(l, l.getProperties());
					}
				} else {
					CachedLine l = new CachedLine();
					l.getInput().moveTo((float) (bounds.x + bounds.w - 20), (float) (bounds.y - 3));
					l.getInput().setPointAttribute(iLinearGraphicsContext.text_v, " \u24e4 ");
					l.getInput().setPointAttribute(iLinearGraphicsContext.textIsBlured_v, false);
					l.getInput().setPointAttribute(iLinearGraphicsContext.font_v, new java.awt.Font("Gill Sans", java.awt.Font.ITALIC, 25));
					l.getInput().setPointAttribute(iLinearGraphicsContext.fillColor_v, new Vector4(0, 0.5f, 0, 0.3f));
					l.getProperties().put(iLinearGraphicsContext.containsText, true);
					GLComponentWindow.currentContext.submitLine(l, l.getProperties());
				}
			}
			return super.paintNow(source, bounds, visible);
		}

		@Override
		public VisitCode menuItemsFor(final iVisualElement source, Map<String, iUpdateable> items) {

			LinkedHashMap<String, iUpdateable> old = new LinkedHashMap<String, iUpdateable>(items);

			items.clear();

			items.put("Boxes", null);

			items.put(" \u21e3\tCreate <b>new</b> visual element here ///N///", new iUpdateable() {

				public void update() {

					List<iVisualElement> all = StandardFluidSheet.allVisualElements(root);
					iVisualElement ee = root;
					boolean exclusive = false;
					for (iVisualElement a : all) {
						Boolean f = a.getProperty(python_isDefaultGroup);
						if (f != null && f) {
							Boolean ex = a.getProperty(python_isDefaultGroupExclusive);
							if (ex != null && ex)
								exclusive = true;
							ee = a;
							break;
						}
					}

					GLComponentWindow frame = iVisualElement.enclosingFrame.get(root);

					Rect bounds = new Rect(30, 30, 50, 50);
					if (frame != null) {
						bounds.x = frame.getCurrentMousePosition().x;
						bounds.y = frame.getCurrentMousePosition().y;
					}

					Triple<VisualElement, DraggableComponent, DefaultOverride> created = VisualElement.createAddAndName(bounds, ee, "untitled", VisualElement.class, DraggableComponent.class, DefaultOverride.class, null);

					if (ee != root && !exclusive) {
						created.left.addChild(root);
					}
				}

			});

			final SelectionGroup<iComponent> g = iVisualElement.selectionGroup.get(root);

			// items.put("   \u2709  <b>group selected elements</b> together in a new group",
			// new iUpdateable() {
			//
			// public void update() {
			// Rect bounds = new Rect(30, 30, 250, 250);
			// DraggableComponent c1 = new
			// DraggableComponent(bounds);
			// VisualElement group = new VisualElement(c1);
			// GroupOverride groupOverride = new GroupOverride();
			// group.setElementOverride(groupOverride);
			// groupOverride.setVisualElement(group);
			// group.setFrame(bounds);
			// c1.setVisualElement(group);
			// group.setProperty(iVisualElement.name,
			// "untitled group");
			//
			// Set<iComponent> selection = g.getSelection();
			//
			// for (iComponent c : selection) {
			// iVisualElement ve = c.getVisualElement();
			// if (ve != null) {
			// ve.addChild(group);
			// }
			// }
			//
			// iVisualElementOverrides.topology.begin(root);
			// iVisualElementOverrides.forward.added.f(group);
			// iVisualElementOverrides.backward.added.f(group);
			// iVisualElementOverrides.topology.end(root);
			//
			// group.addChild(root);
			//
			// }
			// });

			items.put(" \u0236\tCreate a vertical <b>time marker</b> here", new iUpdateable() {

				public void update() {

					GLComponentWindow frame = iVisualElement.enclosingFrame.get(root);

					Rect bounds = new Rect(30, 30, 50, 50);
					if (frame != null) {
						bounds.x = frame.getCurrentMousePosition().x;
						bounds.y = frame.getCurrentMousePosition().y;
					}

					PlainDraggableComponent c1 = new PlainDraggableComponent(bounds);
					VisualElement element = new VisualElement(c1);
					element.setFrame(bounds);
					element.setProperty(iVisualElement.name, "untitled");
					element.addChild(root);

					TimeMarker tm = new TimeMarker();
					element.setElementOverride(tm);
					tm.setVisualElement(element);
					c1.setVisualElement(element);

					iVisualElementOverrides.topology.begin(root);
					iVisualElementOverrides.forward.added.f(element);
					iVisualElementOverrides.backward.added.f(element);
					iVisualElementOverrides.topology.end(root);
				}
			});

			items.put(" \u232b\t<b>Delete</b> element ///meta BACK_SPACE///", new iUpdateable() {
				public void update() {
					if (source == null)
						return;
					Rect r = source.getFrame(null);
					delete(source);
					OverlayAnimationManager.notifyAsText(root, "deleted element " + iVisualElement.name.get(source), r);
				}
			});

			if (source != null) {
				Boolean def = source.getProperty(python_isDefaultGroup);
				if (def != null && def) {
					items.put(" \u25a2\t<b>Reset the default super-element</b>", new iUpdateable() {

						public void update() {
							source.deleteProperty(python_isDefaultGroup);
							source.deleteProperty(python_isDefaultGroupExclusive);
							iVisualElement.dirty.set(source, source, true);
						}

					});
				} else {
					items.put(" \u25a2\tMake this element the <b>default super element</b>", new iUpdateable() {
						public void update() {
							source.setProperty(python_isDefaultGroup, true);
							source.setProperty(python_isDefaultGroupExclusive, false);
							iVisualElement.dirty.set(source, source, true);
						}
					});
					items.put(" \u25a2\tMake this element the <b>exclusive default super element</b>", new iUpdateable() {
						public void update() {
							source.setProperty(python_isDefaultGroup, true);
							source.setProperty(python_isDefaultGroupExclusive, true);
							iVisualElement.dirty.set(source, source, true);
						}
					});
				}

				// if (ThreadedLauncher.getLauncher() != null) {
				// boolean r =
				// iVisualElement.isRenderer.getBoolean(source,
				// false);
				// if (r) {
				// items.put(" \u1d3f\tExecute in <b>UI Thread</b>",
				// new iUpdateable() {
				//
				// public void update() {
				// iVisualElement.isRenderer.set(source, source,
				// 0);
				// iExecutesPromise.promiseExecution.set(source,
				// source, BasicRunner.basicRunner.get(source));
				// }
				// });
				// } else {
				// items.put(" \u1d3f\tExecute in <b>low latency thread</b> <i>(experiemental)</i>",
				// new iUpdateable() {
				//
				// public void update() {
				// iVisualElement.isRenderer.set(source, source,
				// 1);
				// iExecutesPromise.promiseExecution.set(source,
				// source,
				// iVisualElement.multithreadedRunner.get(source));
				// }
				// });
				// }
				// }
			}

			if (source != null) {
				if (ExecutionRuler.hasUnitTests(source, python_source)) {
					items.put(" \u24ca\tRun <b>all unit tests</b>", new iUpdateable() {
						public void update() {
							// make sure that we're
							// selected
							SelectionGroup<iComponent> s = iVisualElement.selectionGroup.get(source);
							s.deselectAll();
							iComponent view = iVisualElement.localView.get(source);
							view.setSelected(true);

							// need to wait a frame
							// for the selection to
							// activate.

							// TODO swt unit tests
							// (need ruler)
							runAllUnitTests();
						}
					});
				}
			}

			if (source != null) {
				boolean added = false;
				Map<Object, Object> properties = source.payload();
				for (final Entry<Object, Object> e : properties.entrySet()) {
					if (e.getKey() instanceof VisualElementProperty) {
						if (((VisualElementProperty) e.getKey()).containsSuffix("m")) {
							if (e.getValue() instanceof PyFunction) {
								if (!added) {
									added = true;
									items.put("Python menu items", null);
								}
								items.put(" \u22ef\t" + ((VisualElementProperty) e.getKey()).getNameNoSuffix(), new iUpdateable() {
									public void update() {
										configurePythonEnvironment(source);
										((PyFunction) e.getValue()).__call__();
										configurePythonPostEnvironment(source);
									}
								});
							}
						}
					}
				}
			}

			items.putAll(old);

			return VisitCode.cont;
		}

		@Override
		public VisitCode prepareForSave() {
			// if (currentlyEditing != null)
			// {
			// swapOut(currentlyEditing,
			// "");
			// swapIn(currentlyEditing);
			// }
			return VisitCode.cont;
		}

		@Override
		public <T> VisitCode setProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> to) {
			if (knownPythonProperties.values().contains(prop)) {
				informationFor(source);
			}
			if (source.equals(currentlyEditing) && prop.equals(currentlyEditingProperty) && !insideUpdate) {
				swapIn(currentlyEditing, currentlyEditingProperty);
			} else if (source.equals(currentlyEditing) && prop.equals(python_stopEditingNow)) {
				swapOut(currentlyEditing, null);
				changeSelection(new LinkedHashSet<iComponent>(), currentlyEditingProperty);
				currentlySelected = new LinkedHashSet<iComponent>();
			} else if (source.equals(currentlyEditing) && prop.equals(python_customToolbar)) {
				swapInCustomToolbar();
			}

			return super.setProperty(source, prop, to);
		}
	}

	@NextUpdate(delay = 4)
	protected void runAllUnitTests() {
		ExecutionRuler.runAllUnitTests(editor.getExecutionRuler());
	}

	protected final class DefaultEditorExecutionInterface implements EditorExecutionInterface {
		public class Delegate implements EditorExecutionInterface {

			private final iVisualElement current;

			public Delegate(iVisualElement current) {
				this.current = current;

			}

			public void executeFragment(String fragment) {
				try {
					editor.getInput().append(filterFragment(fragment));
				} catch (IOException e) {
					e.printStackTrace();
				}

				promiseFor(current).beginExecute();
				PythonInterface.getPythonInterface().clearTopic();
				promiseFor(current).willExecuteSubstring(fragment, 0, 0);

				// PythonInterface.getPythonInterface().execString(fragment);

				editor.pausedText = null;
				messaging.getMessages();

				PythonInterface.getPythonInterface().execStringWithContinuation(fragment, new iUpdateable() {

					@Override
					public void update() {
						if (editor.pausedText == null) {
							editor.pausedText = "Executing ... ";
							// TODO swt - this
							// should be a
							// delayed repaint
							editor.getFrame().redraw();
						}
						;// System.out.println(" waiting ........... (could be swinging....)");

						List<String> m = messaging.getMessages();
						if (m.size() > 0) {
							editor.pausedText = m.get(m.size() - 1);
							// TODO swt - this
							// should be a
							// delayed repaint
							editor.getFrame().redraw();
						}
					}
				}, new iUpdateable() {

					@Override
					public void update() {

						editor.pausedText = null;

						Launcher.display.asyncExec(new Runnable() {

							public void run() {
								editor.getFrame().redraw();

								// TODO swt
								// rulers
								editor.getExecutionRuler().unhighlight();

								String topic = PythonInterface.getPythonInterface().getTopic();

								if (topic != null) {
									int c = Logging.getContext();
									Logging.setContext(0);
									try {
										editor.setTopicString("[" + topic + " : " + PythonInterface.getPythonInterface().getVariable(topic) + "]");
									} finally {
										Logging.setContext(c);
									}
								}
								promiseFor(current).endExecute();

								GLComponentWindow window = root.getProperty(iVisualElement.enclosingFrame);
								if (window != null) {
									window.getRoot().requestRedisplay();
								}
							}
						});
					}
				});
			}

			public Object executeReturningValue(String string) {

				promiseFor(current).beginExecute();
				promiseFor(current).willExecuteSubstring(string, 0, 0);
				Object r = PythonInterface.getPythonInterface().eval(string);
				promiseFor(current).endExecute();

				// r = PythonUtils.maybeToJava(r);

				GLComponentWindow window = root.getProperty(iVisualElement.enclosingFrame);
				if (window != null) {
					window.getRoot().requestRedisplay();
				}
				return r;
			}
		}

		private final iVisualElement root;

		protected DefaultEditorExecutionInterface(iVisualElement root) {
			this.root = root;
		}

		public void executeFragment(String fragment) {
			editor.clearPositionAnnotations();
			fragment = customInsertSystem.convertUserTextToExecutableText(fragment);

			iVisualElementOverrides.topology.begin(currentlyEditing);
			Ref<EditorExecutionInterface> ref = new Ref<EditorExecutionInterface>(new Delegate(currentlyEditing));
			iVisualElementOverrides.forward.getProperty.getProperty(currentlyEditing, editorExecutionInterface, ref);
			iVisualElementOverrides.topology.end(currentlyEditing);

			if (ref.get() != null) {
				ref.get().executeFragment(fragment);
			}

			return;
		}

		public Object executeReturningValue(String string) {
			editor.clearPositionAnnotations();

			string = customInsertSystem.convertUserTextToExecutableText(string);

			if (currentlyEditing == null) {
				Object r = PythonInterface.getPythonInterface().eval(string);
				return r;
			}

			iVisualElementOverrides.topology.begin(currentlyEditing);
			Ref<EditorExecutionInterface> ref = new Ref<EditorExecutionInterface>(new Delegate(currentlyEditing));
			iVisualElementOverrides.forward.getProperty.getProperty(currentlyEditing, editorExecutionInterface, ref);
			iVisualElementOverrides.topology.end(currentlyEditing);

			if (ref.get() != null) {
				return ref.get().executeReturningValue(string);
			}

			return null;
		}

	}

	static public final VisualElementProperty<List<Pair<String, iUpdateable>>> python_customToolbar = new VisualElementProperty<List<Pair<String, iUpdateable>>>("python_customToolbar_");
	static public final VisualElementProperty<StyledTextUndo.Memo> python_undoStack = new VisualElementProperty<StyledTextUndo.Memo>("python_undoStack");

	static public final VisualElementProperty<Object> python_customInsertPersistanceInfo = new VisualElementProperty<Object>("python_customInsertPersistanceInfo");

	static public final VisualElementProperty<Pair<iVisualElement, VisualElementProperty<String>>> python_currentlyEditingProperty = new VisualElementProperty<Pair<iVisualElement, VisualElementProperty<String>>>("python_currentlyEditingProperty");

	static public final VisualElementProperty<EditorExecutionInterface> editorExecutionInterface = new VisualElementProperty<EditorExecutionInterface>("editorExecutionInterface_");

	static public final VisualElementProperty<String> python_executionScratch = new VisualElementProperty<String>("python_executionScratch_v");

	static public final VisualElementProperty<String> python_isTemplate = new VisualElementProperty<String>("python_isTemplate_v");

	static public final VisualElementProperty<String> python_isTemplateHead = new VisualElementProperty<String>("python_isTemplateHead_v");

	static public final LinkedHashMap<String, VisualElementProperty> knownPythonProperties = new LinkedHashMap<String, VisualElementProperty>();

	static {
		knownPythonProperties.put("Default execution", python_source);
		knownPythonProperties.put("Scratch", python_executionScratch);
	}
	static public final VisualElementProperty<Number> python_noEdit = new VisualElementProperty<Number>("python_noEdit");

	static public final VisualElementProperty<Boolean> python_stopEditingNow = new VisualElementProperty<Boolean>("python_stopEditingNow_");

	static public final VisualElementProperty<Boolean> python_isDefaultGroup = new VisualElementProperty<Boolean>("python_isDefaultGroup");
	static public final VisualElementProperty<Boolean> python_isDefaultGroupExclusive = new VisualElementProperty<Boolean>("python_isDefaultGroupExclusive");

	static public void delete(iVisualElement node, iVisualElement root) {

		if (root == null)
			root = node;
		iVisualElementOverrides.topology.begin(root);
		iVisualElementOverrides.forward.deleted.f(node);
		iVisualElementOverrides.backward.deleted.f(node);
		iVisualElementOverrides.topology.end(root);

		for (iVisualElement ve : new ArrayList<iVisualElement>((Collection<iVisualElement>) node.getParents())) {
			ve.removeChild(node);

			// if there are parents that
			// have no children right now,
			// delete them too
			if (ve.getChildren().size() == 0 && ve.getParents().size() == 0)
				delete(ve, root);

		}
		for (iVisualElement ve : new ArrayList<iVisualElement>(node.getChildren())) {
			node.removeChild(ve);

			// if there are parents that
			// have no children right now,
			// delete them too
			if (ve.getChildren().size() == 0 && ve.getParents().size() == 0)
				delete(ve, root);

		}
	}

	private PythonTextEditor editor;

	private BetterComboBox button;

	private final String pathToRepository;

	private final String sheetname;

	private State lastPutAreas;

	protected String banner = "disabled (no selection)";

	protected String stringAtSwapIn = null;

	protected CustomInsertSystem customInsertSystem;

	protected ExecutableAreaFinder executableAreaFinder;

	boolean insideUpdate = false;

	iVisualElement currentlyEditing = null;

	VisualElementProperty<String> currentlyEditingProperty = python_source;
	static public final VisualElementProperty<Integer> currentlyEditingCaretPosition = new VisualElementProperty<Integer>("__currentlyEditingCaretPosition");

	Set<iComponent> currentlySelected = new HashSet<iComponent>();

	Ref<String> editingRef;

	boolean areasChanged = true;

	public PythonPluginEditor(String pathToRepository, String sheetname) {
		this.pathToRepository = pathToRepository;
		this.sheetname = sheetname;
	}

	public CustomInsertSystem getCustomInsertSystem() {
		return customInsertSystem;
	}

	public PythonTextEditor getEditor() {
		return editor;
	}

	public PythonCallableMap getTextDecorator() {
		return editor.getTextDecoration();
	}

	public Writer getTextEditorInput() {
		return editor.getInput();
	}

	public Writer getTextEditorOutput() {
		return editor.getOutput();
	}

	TextAnnotations annotations = new TextAnnotations();
	TripleQuote quotes = new TripleQuote();

	private Messaging messaging = new Messaging();
	private VersionMenu versionMenu;

	@Override
	public void registeredWith(final iVisualElement root) {
		super.registeredWith(root);

		customInsertSystem = new CustomInsertSystem();
		CustomInsertSystem.defaultPossibleInserts();
		executableAreaFinder = new ExecutableAreaFinder();
		// register for selection updates
		group.registerNotification(new SelectionGroup.iSelectionChanged<iComponent>() {
			public void selectionChanged(Set<iComponent> selected) {

				changeSelection(selected, currentlyEditingProperty, true);
			}
		});

		editor = (PythonTextEditor) new PythonTextEditor() {
			protected Pair<String, Object> localCopyRewritten;

			@Woven
			@NextUpdate
			private void resetSelectionTo(int oldStart, int oldEnd) {
				editor.getInputEditor().setSelection(oldStart, oldEnd);
			}

			@Woven
			@NextUpdate
			private void unhighlight() {
				executionRuler.unhighlight();
			}

			@Override
			protected boolean globalShortcutHook(VerifyEvent e) {
				if (e.keyCode == '=' && (e.stateMask == (Platform.getCommandModifier() | SWT.SHIFT))) {
					;// System.out.println(" -- next tab --");
					nextTab();
					return true;
				} else {

					GlobalKeyboardShortcuts g = GlobalKeyboardShortcuts.shortcuts.get(currentlyEditing);
					return g.fire(e);
				}
			}

			@Override
			public void executeArea(Area currentArea) {
				executionRuler.highlight(currentArea);

				int oldStart = editor.getInputEditor().getSelectionRanges()[0];
				int oldEnd = editor.getInputEditor().getSelectionRanges()[1] + oldStart;

				;// System.out.println(" old selection is <" +
					// oldStart + "> <" + oldEnd + ">");

				String plainText = customInsertSystem.convertUserTextToExecutableText(ed.getText());
				int p1 = ExecutedAreas.positionForLineStart(currentArea.lineStart, ed.getText());
				int p2 = ExecutedAreas.positionForLineEnd(currentArea.lineEnd, ed.getText());

				editor.getInputEditor().setSelection(p1, p2);

				String expression = customInsertSystem.convertUserTextToExecutableText(ed.getText().substring(p1, p2));
				Area area = executionRuler.getExecutedAreas().execute(ed.getSelectionRanges()[0], ed.getSelectionRanges()[0] + ed.getSelectionRanges()[1], expression);

				this.inter.executeFragment(expression);

				// ;//System.out.println(" executing area <" +
				// area
				// + ">");
				// if (area.exec.size() > 1) {
				// OpportinisticSlider o = new
				// OpportinisticSlider();
				// boolean m =
				// o.execute(area.exec.get(area.exec.size() -
				// 2).stringAtExecution,
				// area.exec.get(area.exec.size() -
				// 1).stringAtExecution);
				// if (m) {
				// area.textend.put(OpportinisticSlider.oSlider,
				// o);
				// } else
				// area.textend.remove(OpportinisticSlider.oSlider);
				// }

				resetSelectionTo(oldStart, oldEnd);
			}

			@Override
			protected void executeAreaAndRewrite(Area currentArea, iFunction<String, String> convert) {
				executionRuler.highlight(currentArea);

				int oldStart = editor.getInputEditor().getSelectionRanges()[0];
				int oldEnd = editor.getInputEditor().getSelectionRanges()[1] + oldStart;

				String plainText = customInsertSystem.convertUserTextToExecutableText(ed.getText());
				int p1 = ExecutedAreas.positionForLineStart(currentArea.lineStart, ed.getText());
				int p2 = ExecutedAreas.positionForLineEnd(currentArea.lineEnd, ed.getText());

				editor.getInputEditor().setSelection(p1, p2);

				currentArea.freeze();

				String expression = customInsertSystem.convertUserTextToExecutableText(ed.getText().substring(p1, p2));

				expression = convert.f(expression);

				// TODO swt momentum
				// editor.getInputEditor().replaceSelection(expression);

				currentArea.update(editor.getInputEditor());

				Area area = executionRuler.getExecutedAreas().execute(ed.getSelectionRanges()[0], ed.getSelectionRanges()[0] + ed.getSelectionRanges()[1], expression);

				this.inter.executeFragment(expression);

				;// System.out.println(" executing area <" +
					// area + ">");
				if (area.exec.size() > 1) {
					OpportinisticSlider o = new OpportinisticSlider();
					boolean m = o.execute(area.exec.get(area.exec.size() - 2).stringAtExecution, area.exec.get(area.exec.size() - 1).stringAtExecution);
					if (m) {
						area.textend.put(OpportinisticSlider.oSlider, o);
					} else
						area.textend.remove(OpportinisticSlider.oSlider);
				}

				unhighlight();
				resetSelectionTo(oldStart, oldEnd);
			}

			@Override
			protected LocalFuture<Boolean> runAndCheckArea(Area area) {
				final String[] o = { null };

				final iOutputCapture was = setOutputCapture(new iOutputCapture() {

					public void output(String s) {

						;// System.out.println(" -- output is <"
							// + s +
							// "> -- end output");
						o[0] = s;
					}
				});
				try {
					executeArea(area);

					int p1 = ExecutedAreas.positionForLineStart(area.lineStart, ed.getText());
					int p2 = ExecutedAreas.positionForLineEnd(area.lineEnd, ed.getText());
					String expression = customInsertSystem.convertUserTextToExecutableText(ed.getText().substring(p1, p2));

					final String expectedOutput = UnitTestHelper.expectedOutputForExpression(expression);
					return waitForOutput(expression, o, new iProvider<Boolean>() {

						public Boolean get() {
							setOutputCapture(was);

							if (expectedOutput != null) {
								getOutputFlusher().update();
								;// System.out.println(" output from unit test was <"
									// +
									// o[0]
									// +
									// ">");
								return o[0].trim().equals(expectedOutput.trim());
							}
							return null;
						}
					});
				} finally {
				}
			};

			private LocalFuture<Boolean> waitForOutput(final String expression, final String[] o, final iProvider<Boolean> iProvider) {
				final LocalFuture<Boolean> r = new LocalFuture<Boolean>();
				getOutputFlusher().update();
				queue.new Task() {

					int t = 0;

					@Override
					protected void run() {
						t++;
						if (o[0] != null) {
							r.set(iProvider.get());
							return;
						}
						if (t < 30)
							recur();
						else {
							PythonInterface.getPythonInterface().printError("Unit test timed out with no output. The test has to print something. Expression was > \n" + expression);
							r.set(false);
						}
					}
				};
				queue.update();
				return r;
			}

			@Override
			protected void runAndReviseArea(Area area) {

				final String[] o = { null };
				setOutputCapture(new iOutputCapture() {

					public void output(String s) {
						o[0] = s;
					}
				});
				try {
					executeArea(area);

					final int p1 = ExecutedAreas.positionForLineStart(area.lineStart, ed.getText());
					int p2 = ExecutedAreas.positionForLineEnd(area.lineEnd, ed.getText());
					final String expression = customInsertSystem.convertUserTextToExecutableText(ed.getText().substring(p1, p2));

					final String expectedOutput = UnitTestHelper.expectedOutputForExpression(expression);
					getOutputFlusher().update();
					waitForOutput(expression, o, new iProvider<Boolean>() {

						public Boolean get() {

							;// System.out.println(" output from unit test was <"
								// + o[0] +
								// ">");
							if (expectedOutput == null || !o[0].trim().equals(expectedOutput.trim())) {
								;// System.out.println(" about to revise <"
									// +
									// expression
									// +
									// "> to <"
									// +
									// o[0]
									// +
									// ">");
								UnitTestHelper.reviseOutputForExpression(ed, p1, expression, expectedOutput, o[0].trim());
							}
							return null;
						}
					});

				} finally {

				}
			};

			protected void executeAreaSpecial(Area currentArea) {
				// TODO swt rulers
				executionRuler.highlight(currentArea);

				int oldStart = editor.getInputEditor().getSelectionRanges()[0];
				int oldEnd = editor.getInputEditor().getSelectionRanges()[1] + oldStart;

				String plainText = customInsertSystem.convertUserTextToExecutableText(ed.getText());
				int p1 = ExecutedAreas.positionForLineStart(currentArea.lineStart, ed.getText());
				int p2 = ExecutedAreas.positionForLineEnd(currentArea.lineEnd, ed.getText());

				editor.getInputEditor().setSelection(p1, p2);

				currentlyEditing.setProperty(new VisualElementProperty("__eas_"), currentArea);
				currentArea.textend.remove(new Prop("monitor"));
				executeHandleSpecial();

				unhighlight();
				resetSelectionTo(oldStart, oldEnd);
			}

			@Override
			public void executeHandleSpecial(String preamble, String postamble) {

				if (preamble.equals(""))
					preamble = "PythonUtils.installed.mostRecentStack=None";
				if (postamble.equals(""))
					postamble = "print _self.__eas_\nprint PythonUtils.installed.mostRecentStack\nif (_self.__eas_): _self.__eas_.textend.monitor=PythonUtils.installed.mostRecentStack";

				String s = ed.getSelectionText();
				Area area;
				if (s == null || s.equals("")) {
					int pos = ed.getCaretOffset();
					String text = ed.getText();
					int a = text.lastIndexOf("\n", pos - 1);
					if (a == -1)
						a = 0;
					int b = text.indexOf("\n", pos);
					if (b == -1)
						b = text.length();
					s = text.substring(a, b);
					area = executionRuler.getExecutedAreas().execute(a + 2, b - 1, s);
				} else
					area = executionRuler.getExecutedAreas().execute(ed.getSelectionRanges()[0], ed.getSelectionRanges()[1] + ed.getSelectionRanges()[0] - 1, s);

				currentlyEditing.setProperty(new VisualElementProperty("__eas_"), area);
				area.textend.remove(new Prop("monitor"));

				s = detab(s);

				// pad each line
				// of s with a
				// tab
				String[] lines = s.split("\n");
				String total = "";
				for (String l : lines) {
					total += "\t" + l + "\n";
				}
				total += preamble;
				total = "def __tmp" + uniq + "():\n" + total;
				total = "__env" + uniq + "=_environment\n" + total;
				total = "def __enter" + uniq + "():\n\t __env" + uniq + ".enter()\n" + total;
				total = "def __exit" + uniq + "():\n\t __env" + uniq + ".exit()\n" + total;
				total = total + "\nu.stackPrePost(__enter" + uniq + ", __tmp" + uniq + "(), __exit" + uniq + ")\n";
				total += postamble;
				inter.executeFragment(total);
				uniq++;
			}

			@Override
			protected void executeBrowseHandle(final String s) {
				editor.clearPositionAnnotations();
				Object x = inter.executeReturningValue(s);
				if (x instanceof LocalFuture) {
					final LocalFuture lf = ((LocalFuture) x);
					lf.addContinuation(new iUpdateable() {

						public void update() {
							// BrowserTools.browse(lf.get(),
							// currentlyEditing, s);
						}
					});
				} else {
					// BrowserTools.browse(x,
					// currentlyEditing, s);
				}
			}

			@Override
			protected void executeSpecial(int i) {
				editor.clearPositionAnnotations();

				String plainText = customInsertSystem.convertUserTextToExecutableText(ed.getText());
				int position = customInsertSystem.convertUserTextPositionToExecutableTextPosition(ed.getText(), ed.getCaretOffset());
				String fragmentToExecute = executableAreaFinder.findExecutableSubstring(position, plainText, i);

				int oldStart = editor.getInputEditor().getSelectionRanges()[0];
				int oldEnd = editor.getInputEditor().getSelectionRanges()[1] + oldStart;

				editor.getInputEditor().setSelection(executableAreaFinder.lastSubstring_start, executableAreaFinder.lastSubstring_end);
				this.inter.executeFragment(fragmentToExecute);

				resetSelectionTo(oldStart, oldEnd);

			}

			@Override
			protected void executionBegin() {
				;// System.out.println(" BEGIN EXECUTION IN TEXT ED");
				super.executionBegin();
				Beginner beginner = PseudoPropertiesPlugin.begin.get(currentlyEditing);
				beginner.call(new Object[] {});
			}

			@Override
			protected void executionEnd() {

				;// System.out.println(" END EXECUTION IN TEXT ED");

				super.executionEnd();
				Ender beginner = PseudoPropertiesPlugin.end.get(currentlyEditing);
				beginner.call(new Object[] {});
			}

			@Override
			protected void executeSpecial(String i) {
				editor.clearPositionAnnotations();

				String plainText = customInsertSystem.convertUserTextToExecutableText(ed.getText());
				String fragmentToExecute = executableAreaFinder.findExecutableSubstring(plainText, i);
				this.inter.executeFragment(fragmentToExecute);

			}

			@Override
			protected void executeSpecialPrintHandle() {
				super.executeSpecialPrintHandle();

				String s = ed.getSelectionText();
				if (s == null || s.equals("")) {
					int pos = ed.getCaretOffset();
					String text = ed.getText();
					int a = text.lastIndexOf("\n", pos - 1);
					if (a == -1)
						a = 0;
					int b = text.indexOf("\n", pos);
					if (b == -1)
						b = text.length();
					s = text.substring(a, b);
				}

				if (s.startsWith("\n"))
					s = s.substring(1);

				Object ll = PythonInterface.getPythonInterface().eval(s);
				if (ll instanceof PyObject)
					ll = ((PyObject) ll).__tojava__(Object.class);
				OutputInserts.specialPrint(ll, currentlyEditing);
			}

			@Override
			protected String getBanner() {
				return banner;
			}

			@Override
			protected void getMenuItems(LinkedHashMap<String, iUpdateable> items) {
				super.getMenuItems(items);

				if (currentlyEditingProperty.equals(python_source)) {
					items.put("Insert Embedded UI", null);
					List<iPossibleComponent> comps = customInsertSystem.possibleComponents;
					for (iPossibleComponent pc : comps) {
						items.put(pc.name, new InsertPossibleComponentHere(pc));
					}
					if (ed.getSelectionRange().y > 2) {
						items.put("Wrap selection", null);
						List<Triple<String, iPossibleComponent, iPossibleComponent>> wraps = customInsertSystem.possibleWrappers;
						for (Triple<String, iPossibleComponent, iPossibleComponent> pc : wraps) {
							items.put(pc.left, new InsertPossibleWrapHere(pc));
						}
					}
				}

				final int oldStart = editor.getInputEditor().getSelectionRanges()[0];
				final int oldEnd = editor.getInputEditor().getSelectionRanges()[0] + oldStart;
				boolean annotationsYes = false;
				if (oldEnd - oldStart > 0) {
					final String sel = editor.getInputEditor().getSelectionText();

					iVisualElement existing = globals.getExistingDefinitionFor(sel.trim());
					boolean title = false;
					if (existing != null) {
						title = true;
						final iVisualElement ex = existing;
						items.put("Definitions", null);
						items.put(" \u2345 Defined in element '" + SelectionSetDriver.nameFor(existing) + "'", new iUpdateable() {
							public void update() {
								SelectionSetDriver.travelTo(Collections.singleton(ex));
							}
						});
					} else {
						existing = globals.getPotentialDefinitionFor(sel.trim());
						if (existing != null) {
							final iVisualElement ex = existing;
							title = true;
							items.put("Definitions", null);
							items.put("Will be defined by auto-executing element '" + SelectionSetDriver.nameFor(existing) + "'", new iUpdateable() {
								public void update() {
									SelectionSetDriver.travelTo(Collections.singleton(ex));
								}
							});
						}
					}

					List<iVisualElement> uses = globals.getUses(sel.trim());
					if (uses != null && uses.size() > 0) {
						if (!title) {
							items.put("Definitions", null);
							title = true;
						}
						for (final iVisualElement u : uses) {
							final iVisualElement ex = existing;
							items.put(" \u2345 Definition has been used by '" + SelectionSetDriver.nameFor(existing) + "'", new iUpdateable() {
								public void update() {
									SelectionSetDriver.travelTo(Collections.singleton(ex));
								}
							});
						}
					}

					;// System.out.println(" -- hello ?? -- ");

					items.put("Browse", null);
					items.put(" B\tEvaluate, show in <b>browser</b>", new iUpdateable() {

						@Override
						public void update() {
							try {
								Object o = PythonInterface.getPythonInterface().eval(sel);
								if (o != null) {
									HelpBrowser.helpBrowser.get(currentlyEditing).browseObject(o);
								}
							} catch (Throwable t) {
								t.printStackTrace();
							}
						}
					});

					items.put("Search Utilities", null);
					items.put(" \u263d\t<b>Open in Eclipse</b> ", new iUpdateable() {
						public void update() {
							new OpenInEclipse(sel);
						}
					});
					items.put(" \u2370\t<b>Open documentation</b> ", new iUpdateable() {
						public void update() {
							HelpBrowser.helpBrowser.get(root).goToWiki(sel);
						}
					});
					items.put(" \u2370\t<b>Search over openendedgroup.com/field</b> ", new iUpdateable() {
						public void update() {
							OpenInTrac.searchFor(sel);
						}
					});
					annotationsYes = true;

				}

				items.put("Standard Library & Plugins", null);
				items.put(" \u222b\tAlways <b>run this text at Field startup</b>", new iUpdateable() {
					public void update() {
						AddToStandardLibrary.addThis(currentlyEditing, currentlyEditingProperty.get(currentlyEditing), true, ed);
					}
				});
				items.put(" \u222b\tMake a <b>new startup module</b> with this text in it", new iUpdateable() {
					public void update() {
						AddToStandardLibrary.addThis(currentlyEditing, currentlyEditingProperty.get(currentlyEditing), false, ed);
					}
				});

			}

			@Override
			protected void handleMouseEventOnArea(MouseEvent e, Area currentArea) {
				super.handleMouseEventOnArea(e, currentArea);

				;// System.out.println(" handle mouse on area <"
					// + e + " " + currentArea + ">");

				if ((e.stateMask & SWT.ALT) != 0 && currentArea != null && (e.stateMask & SWT.SHIFT) == 0) {
					Object m = currentArea.textend.get(new Prop("monitor"));
					if (executionRuler.updateMonitor(m)) {
						;// System.out.println(" stopping area ");
						stopArea(m);
					} else {
						executeArea(currentArea);
					}
				} else if ((e.stateMask & SWT.ALT) != 0 && currentArea != null && (e.stateMask & SWT.SHIFT) != 0) {
					executeAreaSpecial(currentArea);
				} else
					executionRuler.unhighlight();

				rulerCanvas.redraw();
			}

			private void stopArea(Object m) {
				if (m instanceof PythonGeneratorStack) {
					((PythonGeneratorStack) m).stop();
				}
			}

			protected void goButton(String uidDesc, int parseInt) {
				try {
					String[] m = uidDesc.split("\\[");
					iVisualElement found = StandardFluidSheet.findVisualElement(root, m[1]);
					if (found != null) {
						SelectionSetDriver.travelTo(Collections.singleton(found));
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}

			public iVisualElement getThisBox() {
				return currentlyEditing;
			}

			public VisualElementProperty getThisProperty() {
				return currentlyEditingProperty;
			}

			protected iVisualElement getRoot() {
				return root;
			}

			@Override
			protected void globalEditorPaintHook(org.eclipse.swt.events.PaintEvent e, Control ed) {
				OverDrawing od = OverDrawing.overdraw.get(currentlyEditing == null ? root : currentlyEditing);
				if (od!=null)
					od.draw(e, ed);
			};
			
			protected void navigateTo(iVisualElement box, VisualElementProperty prop, int start, int end) {
				if (box != currentlyEditing) {
					changeSelection(Collections.singleton(iVisualElement.localView.get(box)), prop, false);
				}

				ed.setSelection(start, end);
			}

			@Override
			protected String localCopy(String s) {

				// rewrite
				// string

				Pair<String, Object> r = customInsertSystem.mergeOutText(s);
				localCopyRewritten = r;

				super.localCopy(r.left);
				return localCopyRewritten.left;
			}

			@Override
			protected String localPaste(String was, String s) {

				;// System.out.println(" local paste!!");

				String text = customInsertSystem.mergeInText(localCopyRewritten);

				boolean b = ed.getSelectionCount() > 1;
				ed.insert(text);
				if (!b)
					ed.setCaretOffset(ed.getCaretOffset() + text.length());

				customInsertSystem.updateAllStyles(this.ed, currentlyEditing);

				;// System.out.println(" ::::::::::::::::::::::::::: did local paste :::::::::::::::::::::: <"
					// + b + ">");

				localCopy(text);

				return text;
			}

			@Override
			protected void nextTab() {
				button.selectNextWithSkip();
			}

			// protected void
			// paintPositionAnnotations(java.awt.Graphics2D g2,
			// int width) {
			// super.paintPositionAnnotations(g2, width);
			// annotations.draw(getInputEditor(), g2);
			// quotes.draw(getInputEditor(), g2);
			// };

			protected void previousSelection() {

				if (currentlyEditing != null)
					((MainSelectionGroup) iVisualElement.selectionGroup.get(currentlyEditing)).popSelection();

			};

			protected void nextSelection() {
				if (currentlyEditing != null)
					((MainSelectionGroup) iVisualElement.selectionGroup.get(currentlyEditing)).moveForwardSelection();

			};

			protected void getStyleForEmbeddedString(org.eclipse.swt.custom.StyleRange s, String substring) {

				customInsertSystem.styleForTag(substring, currentlyEditing, s);

			};

			protected void doContextualHelpFor(Class c) {
				try {
					PythonInterface.getPythonInterface().setVariable("__c1", c);
					PythonInterface.getPythonInterface().setVariable("__c2", this);
					String s = Py.tojava((PyObject) PythonInterface.getPythonInterface().eval("markdownForJavaClass(__c2, __c1)"), String.class);

					;// System.out.println(" got <" + s +
						// ">");

					HelpBrowser.helpBrowser.get(currentlyEditing).help.offerHelp("completion", s);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}.setInterface(new DefaultEditorExecutionInterface(root)).setVisible(true);

		new CustomInsertDrawing(editor.getInputEditor()) {
			public void updateAllStylesNow() {
				;// System.out.println(" -- doing update of all styles --");
				customInsertSystem.updateAllStyles(editor.getInputEditor(), currentlyEditing);
			};
		};

		// editor.setEnabled(false);

		// InterconnectSystem.getInterconnectSystem().
		// registerInterest(new
		// iUpdateable() {
		// public void update() {
		// GLComponentWindow sheet =
		// root.getProperty(iVisualElement.enclosingFrame
		// );
		// if (sheet != null) {
		// sheet.getCanvas().display();
		// }
		// }
		//
		// }, editor, root);

		Launcher.getLauncher().registerUpdateable(new iUpdateable() {
			int t = 0;

			int lc = 0;

			public void update() {
				if (t++ % 10 == 0) {
					if (currentlyEditing != null) {
						swapOut(currentlyEditing, null);
						swapOutAreas(currentlyEditing, false);
					}
					if (button != null) {
						if (lc != knownPythonProperties.hashCode()) {
							lc = knownPythonProperties.hashCode();
							updateLabels(button);
						}
					}
				}
			}
		});

		final GLComponentWindow window = root.getProperty(iVisualElement.enclosingFrame);

		ToolBar toolbar = editor.getToolbar();
		versionMenu = new VersionMenu(editor.getToolbar(), editor) {
			@Override
			protected void swapInText(iVisualElement element, String contents) {
				currentlyEditingProperty.set(element, element, contents);
			}
		};

		String[] validProperties = new ArrayList<String>(knownPythonProperties.keySet()).toArray(new String[0]);

		button = new BetterComboBox(toolbar, validProperties) {
			@Override
			public void updateLabels() {
				if (currentlyEditing == null) {
					setLabels(new ArrayList<String>(knownPythonProperties.keySet()).toArray(new String[0]));
					return;
				}

				String[] lab = new String[knownPythonProperties.size()];
				int i = 0;
				Set<Entry<String, VisualElementProperty>> es = knownPythonProperties.entrySet();
				for (Entry<String, VisualElementProperty> e : es) {

					String ll = e.getKey().replace("<html>", "");

					Ref rr = new Ref(null);
					iVisualElementOverrides.topology.begin(currentlyEditing);
					iVisualElementOverrides.forward.getProperty.getProperty(currentlyEditing, e.getValue(), rr);

					if (rr.isUnset()) {
						ll = "\u2606 <font color='#333333'>" + ll + "</font>";
					} else if (rr.getStorageSource() != currentlyEditing && currentlyEditing.getProperty(e.getValue()) == null) {

						;// System.out.println(" storage source is <"
							// +
							// rr.getStorageSource()
							// + "> <" +
							// currentlyEditing +
							// ">");

						ll = "\u2041 " + ll;
					} else
						ll = "\u2605 " + ll;

					ll = "<html> " + ll;
					lab[i++] = ll;
				}
				setLabels(lab);
			}

			@Override
			public void updateSelection(int index, String text) {
				VisualElementProperty pp = new ArrayList<VisualElementProperty>(knownPythonProperties.values()).get(index);

				changeSelection(PythonPluginEditor.this.currentlySelected, pp);
			}

			protected boolean shouldSkip(int index) {
				try {
					ArrayList<Entry<String, VisualElementProperty>> es = new ArrayList<Entry<String, VisualElementProperty>>(knownPythonProperties.entrySet());

					Object m = es.get(index).getValue().get(currentlyEditing);
					return m == null || m.toString().trim().length() == 0;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			};
		};
		updateLabels(button);
		toolbar.layout();
		button.combo.setBackground(button.combo.getParent().getBackground());

		// button.setPreferredSize(new Dimension(300, 25));
		// button.setMinimumSize(new Dimension(100, 25));
		// button.setMaximumSize(new Dimension(300, 25));
		// toolbar.add(button);
		//
		makeOutputActions();
		//
		// SpringUtilities.makeCompactGrid(toolbar, 1,
		// toolbar.getComponentCount(), 2, 0, 0, 0);
		//
		// root.setProperty(OutputInserts.outputInserts, new
		// OutputInserts((DefaultStyledDocument)
		// editor.getOutputDocument()));

		root.setProperty(Messaging.messaging, messaging);
		root.setProperty(Messaging.feedback, messaging.getFile());

		installHelpBrowser(root);
	}

	@NextUpdate(delay = 3)
	private void installHelpBrowser(final iVisualElement root) {
		HelpBrowser h = HelpBrowser.helpBrowser.get(root);
		ContextualHelp ch = h.getContextualHelp();
		ch.addContextualHelpForWidget("textEditor", editor.getInputEditor(), ch.providerForStaticMarkdownResource("contextual/textInput.md"), 50);
		ch.addContextualHelpForWidget("outputEditor", editor.getOutputEditor(), ch.providerForStaticMarkdownResource("contextual/textOutput.md"), 50);
	}

	private void makeOutputActions() {
		LinkedHashMap<String, iUpdateable> outputActions = new LinkedHashMap<String, iUpdateable>();
		outputActions.put("Output", null);
		outputActions.put(" \u2327 <b>Clear everything</b>", new iUpdateable() {
			public void update() {
				editor.getOutputEditor().getContent().replaceTextRange(0, editor.getOutputEditor().getContent().getCharCount(), "");
			}
		});

		outputActions.put(((editor.outputLength == 100 * 1024) ? "!" : "") + " \u2807 <b>Long</b> history", new iUpdateable() {
			public void update() {
				editor.outputLength = 100 * 1024;
				makeOutputActions();
			}
		});
		outputActions.put(((editor.outputLength == 2 * 1024) ? "!" : "") + " \u2807 <b>Medium</b> history", new iUpdateable() {
			public void update() {
				editor.outputLength = 2 * 1024;
				makeOutputActions();
			}
		});
		outputActions.put(((editor.outputLength == 50) ? "!" : "") + " \u2807 <b>Short</b> history (fastest)", new iUpdateable() {
			public void update() {
				editor.outputLength = 50;
				makeOutputActions();
			}
		});

		outputActions.put(" \u2807 <b>Copy</b> ", new iUpdateable() {
			public void update() {
				editor.getOutputEditor().copy();
			}
		});

		editor.setOutputActionMenu(outputActions);
	}

	private boolean canEdit(iVisualElement current) {
		Number c = python_noEdit.get(current);
		if (c == null)
			return true;
		if (c.doubleValue() > 0)
			return false;
		return true;
	}

	private void swapOutAreas(iVisualElement on, boolean freeze) {
		// TODO swt rulers
		State aa = editor.getAreas();

		if (aa.equals(lastPutAreas) && !freeze)
			return;

		lastPutAreas = aa;
		if (freeze) {
			for (Area a : aa.areas) {
				a.freeze();
			}
			for (Iterator i = aa.areas.iterator(); i.hasNext();) {
				Area n = (Area) i.next();
				if (n.invalid)
					i.remove();
			}

			annotations.swapOut(on);
		}

		python_areas.putInMap(on, currentlyEditingProperty.getName(), aa);
		if (freeze)
			editor.setAreas(new State());
		// if (!freeze)
		// editor.setContentsForHistory(stringAtSwapIn);

	}

	private void updateLabels(BetterComboBox button) {
		button.setLabels(new ArrayList<String>(knownPythonProperties.keySet()).toArray(new String[0]));

		button.updateLabels();
		editor.getToolbar().layout();
	}

	protected VisualElementProperty<String> autoSwapProperty(iVisualElement visualElement, VisualElementProperty<String> prop) {
		if (visualElement == null)
			return prop;

		String m = visualElement.getProperty(prop);
		if (m == null || m.trim().equals("")) {

			Set<Entry<String, VisualElementProperty>> ve = knownPythonProperties.entrySet();
			int index = 0;
			updateLabels(button);
			for (Map.Entry<String, VisualElementProperty> p : ve) {
				Object pp = visualElement.getProperty(p.getValue());
				if (pp != null && pp instanceof String && ((String) pp).trim().length() > 0) {
					button.forceSelection(button.getLabels()[index]);
					return p.getValue();
				}
				index++;
			}
		}
		return prop;
	}

	protected void changeSelection(Set<iComponent> selected, VisualElementProperty<String> prop) {
		changeSelection(selected, prop, false);
	}

	protected void changeSelection(Set<iComponent> selected, VisualElementProperty<String> prop, boolean considerOtherProperties) {

		if (selected.size() == 0) {
			if (currentlyEditing != null) {
				if (editor.getInputEditor().getCaretOffset() > 0)
					currentlyEditingCaretPosition.set(currentlyEditing, currentlyEditing, editor.getInputEditor().getCaretOffset());
				swapOut(currentlyEditing, "");
				swapOutAreas(currentlyEditing, true);
			}
			banner = "disabled (no selection)";
			currentlyEditing = null;
			;// System.out.println(" disabled ");
			editor.setEnabled(false);

			currentlyEditingProperty = prop;
			editor.setActionMenu(null);

			button.combo.setEnabled(false);

		} else if (selected.size() > 1) {
			if (currentlyEditing != null) {
				if (editor.getInputEditor().getCaretOffset() > 0)
					currentlyEditingCaretPosition.set(currentlyEditing, currentlyEditing, editor.getInputEditor().getCaretOffset());
				swapOut(currentlyEditing, "");
				swapOutAreas(currentlyEditing, true);
			}
			banner = "disabled (multiple selection)";
			currentlyEditing = null;
			;// System.out.println(" disabled ");
			editor.setEnabled(false);
			currentlyEditingProperty = prop;
			editor.setActionMenu(null);

			button.combo.setEnabled(false);

		} else {
			if (currentlyEditing != null) {
				if (editor.getInputEditor().getCaretOffset() > 0)
					currentlyEditingCaretPosition.set(currentlyEditing, currentlyEditing, editor.getInputEditor().getCaretOffset());
				swapOut(currentlyEditing, "");
				swapOutAreas(currentlyEditing, true);
			}
			iVisualElement visualElement = selected.iterator().next().getVisualElement();

			if (considerOtherProperties)
				prop = autoSwapProperty(visualElement, prop);

			swapIn(visualElement, prop);
			currentlyEditing = visualElement;
			currentlyEditingProperty = prop;
			python_currentlyEditingProperty.set(lve, lve, new Pair<iVisualElement, VisualElementProperty<String>>(visualElement, prop));
			if (visualElement == null) {
				;// System.out.println(" disabled ");
				editor.setEnabled(false);
				banner = "disabled (no selection)";
				editor.setActionMenu(null);
				button.combo.setEnabled(false);
			} else if (!canEdit(visualElement)) {
				;// System.out.println(" disabled (can't edit)");
				editor.setEnabled(false);
				banner = "disabled (python_noEdit has been set)";
				editor.setActionMenu(null);
				button.combo.setEnabled(false);
			} else {
				editor.setEnabled(true);
				button.combo.setEnabled(true);
			}
			swapInCustomToolbar();

		}

		currentlySelected = new HashSet<iComponent>(selected);
		updateLabels(button);
	}

	@Override
	protected iVisualElementOverrides createElementOverrides() {
		return new MenuOverrides().setVisualElement(lve);
	}

	public void delete(iVisualElement node) {
		delete(node, root);
	}

	protected String filterFragment(String fragment) {
		return fragment.replaceAll("r\"\"\".*\"\"\"", "(raw string)");
	}

	@Override
	protected void handleExceptionThrownDuringRunning(String when, final iVisualElement element, final iVisualElement parentElement, final Throwable t) {

		if (Logging.logging != null) {
			PythonInterface.handlePythonException(element, parentElement, t);
		}

		System.err.println(" exception thrown inside element <" + element + "> <" + t + ">");

		// this is where we'd create a new
		// unacknowledged error thing (and possibly blow
		// away the old one, simply by trimming the
		// list)

		// UnacknowledgedError error = new UnacknowledgedError(when, t,
		// element, parentElement);
		//
		// UnacknowledgedErrors.makeError(element, error);

		super.handleExceptionThrownDuringRunning(when, element, parentElement, t);
	}

	static public void handlePythonException(final iVisualElement element, final iVisualElement parentElement, final Throwable t) {
		PythonInterface.handlePythonException(element, parentElement, t);
	}

	@Override
	protected Promise newPromiseFor(iVisualElement element) {
		return new LocalEditablelPromise(element);
	}

	protected void swapIn(iVisualElement element, VisualElementProperty<String> prop) {
		editor.clearPositionAnnotations();
		editor.setInside(element);
		if (element != null) {
			informationFor(element);
			Ref<String> r = new Ref<String>(null);

			iVisualElementOverrides.topology.begin(element);
			iVisualElementOverrides.forward.getProperty.getProperty(element, prop, r);

			if (r.get() == null) {
				if (prop.equals(python_source)) {
					customInsertSystem.swapInText(new Pair<String, Object>("", null));
				}
				editor.setText("");
				stringAtSwapIn = "";
				banner = "new text for element";
				editingRef = r;
			} else {
				String markedText = r.get();
				Object r2 = python_customInsertPersistanceInfo.get(element);

				if (prop.equals(python_source)) {
					markedText = customInsertSystem.swapInText(new Pair<String, Object>(markedText, r2));
				}

				currentlyEditing = element;

				editor.setText(markedText);

				if (prop.equals(python_source)) {
					customInsertSystem.updateAllStyles(editor.getInputEditor(), element);
				}
				stringAtSwapIn = r.get();
				editingRef = r;
				if (editingRef.getStorageSource() == element) {
					banner = "local";
				} else {
					banner = "inherited from '" + editingRef.getStorageSource().getProperty(iVisualElement.name) + "'";
					if (iVisualElement.localView.get(editingRef.getStorageSource()) instanceof DraggableComponent)
						((DraggableComponent) iVisualElement.localView.get(editingRef.getStorageSource())).setMarked(true);
				}

				versionMenu.swapIn(element, prop);

			}

			iVisualElementOverrides.topology.end(element);

			StringAdaptations.stringAdaptation_promise.set(promises.get(element));

			// TODO swt rulers
			Map<String, State> areas = element.getProperty(python_areas);
			if (areas != null) {
				State areaz = areas.get(prop.getName());

				if (areaz != null) {
					editor.setAreas(areaz);
				} else
					editor.setAreas(new State());
			} else {
				editor.setAreas(new State());
			}
			//
			// editor.setFilename(new
			// HistoryExplorerHG(pathToRepository,
			// sheetname), pathToRepository + "/" + sheetname + "/"
			// +
			// element.getUniqueID() + "/" + prop.getName() +
			// ".property",
			// stringAtSwapIn);

			LinkedHashMap<String, iUpdateable> inheritanceMenu = new LinkedHashMap<String, iUpdateable>();

			inheritanceMenu.put("Property Inheritance", null);
			inheritanceMenu.put(" \u232b <b>delete this property</b> from this element", new iUpdateable() {

				public void update() {
					swapOut(currentlyEditing, "");
					currentlyEditingProperty.delete(currentlyEditing, currentlyEditing);
					swapIn(currentlyEditing, currentlyEditingProperty);
				}
			});

			// TODO swt syntax highlighter
			// inheritanceMenu.put("Sytle", null);
			// inheritanceMenu.put(" \u29fd <b>customize syntax highlighting </b> ",
			// new iUpdateable() {
			//
			// public void update() {
			// SyntaxHighlightingStyles.openCustomizer((JavaEditorKit)
			// editor.getInputEditor().getEditorKit(),
			// editor.getInputEditor(),
			// Constants.defaultFont_editorSize);
			// }
			// });

			// TODO swt menus for text editor
			// editor.setActionMenu(new
			// SmallMenu().createMenu(inheritanceMenu,
			// null));
			editor.setName(iVisualElement.name.get(element));

			Memo e = python_undoStack.get(element);
			editor.getUndoHelper().fromMemo(e);

			Integer cp = currentlyEditingCaretPosition.get(element);
			if (cp != null) {
				try {

					scrollLater(cp);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

		} else {
			banner = "disabled (no selection)";
			editor.setActionMenu(null);
			editor.setName("(disabled)");
		}
	}

	@NextUpdate(delay = 1)
	protected void scrollLater(Integer cp) {
		editor.getInputEditor().setCaretOffset(cp);
		editor.getInputEditor().showSelection();
	}

	List<Control> customToolbarFromProperties = new ArrayList<Control>();

	public void swapInCustomToolbar() {

		ToolBar t = editor.getToolbar();

		for (Control b : customToolbarFromProperties)
			b.dispose();

		customToolbarFromProperties.clear();

		if (currentlyEditing == null)
			return;

		List<Pair<String, iUpdateable>> list = python_customToolbar.get(currentlyEditing);

		if (list == null)
			return;

		LinkedHashMap<String, iUpdateable> pp = new LinkedHashMap<String, iUpdateable>();
		for (final Pair<String, iUpdateable> ppp : list) {

			final iUpdateable was = pp.get(ppp.left);

			if (was == null)
				pp.put(ppp.left, ppp.right);
			else {
				pp.put(ppp.left, new iUpdateable() {

					@Override
					public void update() {
						ppp.right.update();
						was.update();
					}
				});
			}
		}

		for (final Map.Entry<String, iUpdateable> e : pp.entrySet()) {
			// Button b = new Button(too);
			// b.putClientProperty("Quaqua.Button.style", "square");
			// b.setFont(new Font(Constants.defaultFont, 0, 8));

			Button b = new Button(t, SWT.FLAT);
			b.setText(e.getKey());
			b.setFont(new Font(b.getFont().getDevice(), b.getFont().getFontData()[0].name, (int) (GraphNodeToTreeFancy.baseFontHeight(b)), SWT.NORMAL));
			b.addListener(SWT.MouseDown, new Listener() {

				@Override
				public void handleEvent(Event event) {
					editor.clearPositionAnnotations();
					int oldStart = editor.getInputEditor().getSelectionRanges()[0];
					int oldEnd = editor.getInputEditor().getSelectionRanges()[1] + oldStart;

					editor.getInputEditor().setSelection(0, editor.getInputEditor().getText().length());
					e.getValue().update();

					editor.getInputEditor().setSelection(oldStart, oldEnd);
				}
			});
			customToolbarFromProperties.add(b);
			b.setBackground(b.getParent().getBackground());

		}
		t.layout();
		//
		// // TODO swt icons
		// // Icon icon =
		// SmallMenu.makeIconFromCharacter(e.getKey().charAt(0),
		// 25, 18, 0, new Color(0, 0, 0, 0.1f), new Color(1, 1, 1,
		// 0.1f));
		// // b.setIcon(icon);
		// //
		// b.setPreferredSize(new Dimension(25, 25));
		// b.setMaximumSize(new Dimension(25, 25));
		// b.setMinimumSize(new Dimension(25, 25));
		// b.addActionListener(new ActionListener() {
		//
		// public void actionPerformed(ActionEvent x) {
		// editor.clearPositionAnnotations();
		// editor.getInputEditor().setSelectionStart(0);
		// editor.getInputEditor().setSelectionEnd(editor.getInputEditor().getText().length());
		// e.getValue().update();
		// }
		// });
		// p.add(b);
		// customToolbarFromProperties.add(b);
		// } else {
		// b.setText(e.getKey());
		// b.setPreferredSize(new Dimension(25, 25));
		// b.setMaximumSize(new Dimension(125, 25));
		// b.setMinimumSize(new Dimension(25, 25));
		// b.addActionListener(new ActionListener() {
		//
		// public void actionPerformed(ActionEvent x) {
		// editor.clearPositionAnnotations();
		// editor.getInputEditor().setSelectionStart(0);
		// editor.getInputEditor().setSelectionEnd(editor.getInputEditor().getText().length());
		// e.getValue().update();
		// }
		// });
		// p.add(b);
		// customToolbarFromProperties.add(b);
		// }
		// }
		//
		// SpringUtilities.makeCompactGrid(p, 1, p.countComponents(), 2,
		// 0, 0,
		// 0);
		//
		// p.validate();
		// ;//System.out.println(" panel <" + p.countComponents() +
		// "> <" +
		// Arrays.asList(p.getComponents()) + ">");

	}

	protected void swapOut(iVisualElement element, String string) {

		if (editor.getInputEditor().isDisposed())
			return;

		if (element != null) {

			python_undoStack.set(element, element, editor.getUndoHelper().toMemo());

			informationFor(element);
			String text = editor.getText();

			if (stringAtSwapIn == null || !stringAtSwapIn.equals(customInsertSystem.convertUserTextToExecutableText(text)))
			// if (true)
			{
				String plainText = editor.getText();

				iVisualElementOverrides.topology.begin(lve);
				if (currentlyEditingProperty.equals(python_source)) {
					Pair<String, Object> swappedOut = customInsertSystem.swapOutText(plainText);

					// ;//System.out.println(" swapping out text <"
					// + swappedOut + ">");

					python_customInsertPersistanceInfo.set(element, element, swappedOut.right);

					plainText = swappedOut.left;
				}
				editingRef.set(plainText);
				insideUpdate = true;
				iVisualElementOverrides.backward.setProperty.setProperty(element, currentlyEditingProperty, editingRef);
				insideUpdate = false;
				iVisualElementOverrides.topology.end(lve);
				stringAtSwapIn = plainText;
			}

		}
		if (string != null)
			editor.setText(string);
	}

	TaskQueue queue = new TaskQueue();

	@Override
	public void update() {
		queue.update();
	}

	static public void removeBoxLocalEverywhere(final String m) {
		PythonInterface.getPythonInterface().specialVariables_read.remove(m);
		PythonInterface.getPythonInterface().specialVariables_write.remove(m);
	}

	static public void makeBoxLocalEverywhere(final String m) {
		PythonInterface.getPythonInterface().specialVariables_read.put(m, new iFunction<PyObject, String>() {

			@Override
			public PyObject f(String in) {

				iVisualElement inside = (iVisualElement) PythonInterface.getPythonInterface().getVariable("_self");
				if (inside == null)
					throw new NullPointerException(" looked up <" + in + "> as box local with no context set");
				return Py.java2py(getAttr(inside, in + "_"));
			}
		});
		PythonInterface.getPythonInterface().specialVariables_write.put(m, new iFunction<PyObject, PyObject>() {

			@Override
			public PyObject f(PyObject in) {

				iVisualElement inside = (iVisualElement) PythonInterface.getPythonInterface().getVariable("_self");
				if (inside == null)
					throw new NullPointerException(" looked up <" + in + "> as box local with no context set");

				setAttr(inside, m + "_", Py.tojava(in, Object.class));

				return in;
			}
		});
	}

}
