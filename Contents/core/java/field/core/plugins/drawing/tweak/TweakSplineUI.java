package field.core.plugins.drawing.tweak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyTuple;

import field.core.Platform;
import field.core.dispatch.Mixins.iMixinProxy;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElementOverrides.Ref;
import field.core.execution.PythonInterface;
import field.core.execution.PythonScriptingSystem;
import field.core.plugins.drawing.SplineComputingOverride;
import field.core.plugins.drawing.SplineComputingOverride.PLineList;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.plugins.drawing.opengl.CachedLineCursor;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLine;
import field.core.plugins.drawing.opengl.iLine_m;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext.iTransformingContext;
import field.core.plugins.drawing.tweak.AbsoluteTool.AbsoluteNodeDescription;
import field.core.plugins.drawing.tweak.NodeModifiers.Break;
import field.core.plugins.drawing.tweak.NodeModifiers.Delete;
import field.core.plugins.drawing.tweak.NodeModifiers.EnsureCubicLeft;
import field.core.plugins.drawing.tweak.NodeModifiers.EnsureCubicRight;
import field.core.plugins.drawing.tweak.NodeModifiers.EnsureLinearLeft;
import field.core.plugins.drawing.tweak.NodeModifiers.EnsureLinearRight;
import field.core.plugins.drawing.tweak.NodeModifiers.SubdivideLeft;
import field.core.plugins.drawing.tweak.NodeModifiers.SubdivideRight;
import field.core.plugins.drawing.tweak.TweakSplineCodeGen.BaseTool;
import field.core.plugins.drawing.tweak.TweakSplineCodeGen.iResult;
import field.core.plugins.drawing.tweak.Visitors.TCorner;
import field.core.plugins.drawing.tweak.Visitors.TSmooth;
import field.core.plugins.drawing.tweak.python.iCoordTransformation;
import field.core.plugins.drawing.tweak.python.iNodeSelection;
import field.core.plugins.drawing.tweak.python.iWholeCoordTransform;
import field.core.plugins.snip.SnippetsPlugin;
import field.core.ui.MarkingMenuBuilder;
import field.core.ui.PopupTextBox;
import field.core.ui.SmallMenu;
import field.core.ui.SmallMenu.BetterPopup;
import field.core.util.PythonCallableMap;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.components.RootComponent;
import field.core.windowing.components.RootComponent.iMousePeer;
import field.launch.iUpdateable;
import field.math.abstraction.iAcceptor;
import field.math.abstraction.iFilter;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Pair;
import field.util.ANSIColorUtils;
import field.util.Dict;
import field.util.PythonUtils;

/**
 * for applying edits to computed splines (List<CachedLine>) at a node level
 * 
 * this class just helps with the hit testing, the selection and the drawing of
 * the selection
 * 
 * @author marc
 * 
 */
public class TweakSplineUI {

	static public VisualElementProperty<Object> postProcess = new VisualElementProperty<Object>("postProcessLine_");

	static public class MouseInfo {
		float lastOdx = 0;

		float lastOdy = 0;

		float lastOx = 0;

		float lastOy = 0;
	}

	public class Selectable {
		SelectedVertex current;

		Set<SubSelection> add;

		boolean alreadyAdded = false;

		public Selectable(SelectedVertex v, SubSelection... s) {
			current = v;
			add = new HashSet<SubSelection>(Arrays.asList(s));
		}

		@Override
		public String toString() {
			return "selectable[" + current + "] <- " + add + (alreadyAdded ? "" : "(new)");
		}
	}

	static public class SelectedVertex {
		public CachedLine onLine;

		public CachedLine.Event vertex;

		public int vertexIndex;

		public Set<SubSelection> whatSelected;

		boolean writeToThawInstead = false;

		HashMap<SubSelection, Vector2> frozenPosition = new HashMap<SubSelection, Vector2>();

		HashMap<SubSelection, Vector2> absPosition = new HashMap<SubSelection, Vector2>();

		HashMap<SubSelection, Float> amountSelected = new LinkedHashMap<SubSelection, Float>();

		public SelectedVertex(CachedLine l, CachedLineCursor c) {
			whatSelected = new HashSet<SubSelection>();
			whatSelected.add(SubSelection.postion);
			vertex = c.getCurrent();
			vertexIndex = c.getCurrentIndex();
			onLine = l;
		}

		public SelectedVertex(CachedLine line, int nodeNumber) {
			onLine = line;
			vertex = onLine.events.get(nodeNumber);
			vertexIndex = nodeNumber;
			whatSelected = new HashSet<SubSelection>();
		}

		public void freeze() {

			if (vertex.getDict().isTrue(iLinearGraphicsContext.noTweak_v, false))
				return;

			for (SubSelection s : whatSelected) {
				Vector2 at = new Vector2();
				positionFor(s, at);
				frozenPosition.put(s, at);
				absPosition.put(s, at);
			}
		}

		public float getAmountSelectedFor(SubSelection s) {
			if (amountSelected == null)
				return whatSelected.contains(s) ? 1 : 0;
			if (!whatSelected.contains(s)) {
				amountSelected.remove(s);
				return 0;
			}

			Float amount = amountSelected.get(s);
			if (amount == null)
				return 1;
			if (amount == 0)
				whatSelected.remove(s);
			return amount;
		}

		public void ongoingPositionFor(SubSelection s, Vector2 at) {
			if (absPosition.containsKey(s))
				at.setValue(absPosition.get(s));
			else if (frozenPosition.containsKey(s))
				at.setValue(frozenPosition.get(s));
			else
				positionFor(s, at);

		}

		public Vector2 positionFor(SubSelection s, Vector2 at) {

			iLinearGraphicsContext context = onLine.getProperties().get(iLinearGraphicsContext.context);
			if (context instanceof iTransformingContext) {
				if (at == null)
					at = new Vector2();
				if (s == SubSelection.postion)
					((iTransformingContext) context).convertIntermediateSpaceToDrawingSpace(((iTransformingContext) context).getIntermediateSpaceForEvent(onLine, vertex, -1), at);
				else if (s == SubSelection.previousControl)
					((iTransformingContext) context).convertIntermediateSpaceToDrawingSpace(((iTransformingContext) context).getIntermediateSpaceForEvent(onLine, vertex, 1), at);
				else if (s == SubSelection.nextControl)
					((iTransformingContext) context).convertIntermediateSpaceToDrawingSpace(((iTransformingContext) context).getIntermediateSpaceForEvent(onLine, onLine.events.get(vertexIndex + 1), 0), at);
				return at;
			} else {

				if (at == null)
					at = new Vector2();
				if (s == SubSelection.postion) {
					at.x = ((Number) vertex.args[vertex.args.length - 2]).floatValue();
					at.y = ((Number) vertex.args[vertex.args.length - 1]).floatValue();
				} else if (s == SubSelection.previousControl) {
					at.x = ((Number) vertex.args[2]).floatValue();
					at.y = ((Number) vertex.args[3]).floatValue();
				} else if (s == SubSelection.nextControl) {
					at.x = ((Number) onLine.events.get(vertexIndex + 1).args[0]).floatValue();
					at.y = ((Number) onLine.events.get(vertexIndex + 1).args[1]).floatValue();
				}
				return at;
			}
		}

		public void setAmountSelectedFor(SubSelection s, float a) {
			if (amountSelected == null) {
				amountSelected = new LinkedHashMap<SubSelection, Float>();
				for (SubSelection ss : whatSelected)
					amountSelected.put(ss, 1f);
			}
			assert whatSelected.contains(s);
			if (!whatSelected.contains(s)) {
				amountSelected.remove(s);
			}
			if (a == 0f) {
				whatSelected.remove(s);
			}

			amountSelected.put(s, a);
		}

		public void setPositionFor(CachedLine onLine, SubSelection s, Vector2 at) {
			if (writeToThawInstead) {
				setThawPositionFor(s, at);
			} else {

				iLinearGraphicsContext context = onLine.getProperties().get(iLinearGraphicsContext.context);

				if (context instanceof iTransformingContext) {
					if (s == SubSelection.postion) {
						Object inter = ((iTransformingContext) context).getIntermediateSpaceForEvent(onLine, vertex, -1);
						Vector2 current = new Vector2();
						((iTransformingContext) context).convertIntermediateSpaceToDrawingSpace(inter, current);
						((iTransformingContext) context).setIntermediateSpaceForEvent(onLine, vertex, -1, inter, current, at);

					} else if (s == SubSelection.previousControl) {
						Object inter = ((iTransformingContext) context).getIntermediateSpaceForEvent(onLine, vertex, 1);
						Vector2 current = new Vector2();
						((iTransformingContext) context).convertIntermediateSpaceToDrawingSpace(inter, current);
						((iTransformingContext) context).setIntermediateSpaceForEvent(onLine, vertex, 1, inter, current, at);
					} else if (s == SubSelection.nextControl) {
						Object inter = ((iTransformingContext) context).getIntermediateSpaceForEvent(onLine, onLine.events.get(vertexIndex + 1), 0);
						Vector2 current = new Vector2();
						((iTransformingContext) context).convertIntermediateSpaceToDrawingSpace(inter, current);
						((iTransformingContext) context).setIntermediateSpaceForEvent(onLine, onLine.events.get(vertexIndex + 1), 0, inter, current, at);
					}
				} else {

					if (s == SubSelection.postion) {
						((Event) onLine.events.get(vertexIndex)).args[vertex.args.length - 2] = (float) at.x;
						((Event) onLine.events.get(vertexIndex)).args[vertex.args.length - 1] = (float) at.y;
					} else if (s == SubSelection.previousControl) {
						((Event) onLine.events.get(vertexIndex)).args[2] = (float) at.x;
						((Event) onLine.events.get(vertexIndex)).args[3] = (float) at.y;
					} else if (s == SubSelection.nextControl) {
						((Event) onLine.events.get(vertexIndex + 1)).args[0] = (float) at.x;
						((Event) onLine.events.get(vertexIndex + 1)).args[1] = (float) at.y;
					}
				}
			}
		}

		public void setThawPositionFor(SubSelection s, Vector2 at) {
			absPosition.put(s, at);
		}

		public void thawInto(CachedLine onLine) {
			for (SubSelection s : whatSelected) {
				Vector2 to = absPosition.get(s);
				if (to != null) {
					setPositionFor(onLine, s, to);
				}
			}
		}

		@Override
		public String toString() {
			return "sv :" + vertexIndex + " " + whatSelected;
		}
	}

	public class SelectionMousePeer implements iMousePeer {

		private float lx;

		private float ly;

		private float ox;

		private float oy;

		private Selectable justAdded;

		boolean firstDrag = false;

		boolean down = false;

		Vector2 marqueeDownAt = new Vector2();

		public void keyPressed(ComponentContainer inside, org.eclipse.swt.widgets.Event arg0) {
			if (arg0.keyCode == SWT.ARROW_LEFT) {
				// left
				nudgeSelection(-1, 0, (arg0.stateMask & SWT.SHIFT) != 0);
			} else if (arg0.keyCode == SWT.ARROW_UP) {

				// up
				nudgeSelection(0, -1, (arg0.stateMask & SWT.SHIFT) != 0);
			} else if (arg0.keyCode == SWT.ARROW_RIGHT) {

				// right
				nudgeSelection(1, 0, (arg0.stateMask & SWT.SHIFT) != 0);
			} else if (arg0.keyCode == SWT.ARROW_DOWN) {

				// down
				nudgeSelection(0, 1, (arg0.stateMask & SWT.SHIFT) != 0);
			}

		}

		public void keyReleased(ComponentContainer inside, org.eclipse.swt.widgets.Event arg0) {
		}

		public void keyTyped(ComponentContainer inside, org.eclipse.swt.widgets.Event arg0) {

			if (arg0.character == '7') {
				List<SelectedVertex> nextselection = new ArrayList<SelectedVertex>();
				for (SelectedVertex v : selection) {
					if (v.vertexIndex >= 0) {
						SelectedVertex sv = new SelectedVertex(v.onLine, v.vertexIndex - 1);
						sv.whatSelected.addAll(v.whatSelected);
						if (!sv.vertex.method.equals(iLine_m.cubicTo_m))
							sv.whatSelected.remove(SubSelection.previousControl);
						if (!v.vertex.method.equals(iLine_m.cubicTo_m))
							sv.whatSelected.remove(SubSelection.nextControl);
						nextselection.add(sv);
					}
				}
				if ((arg0.stateMask & SWT.SHIFT) == 0)
					selection.clear();
				selection.addAll(nextselection);
			} else if (arg0.character == '9') {
				List<SelectedVertex> nextselection = new ArrayList<SelectedVertex>();
				for (SelectedVertex v : selection) {
					if (v.vertexIndex < v.onLine.events.size() - 1) {
						SelectedVertex sv = new SelectedVertex(v.onLine, v.vertexIndex + 1);
						sv.whatSelected.addAll(v.whatSelected);
						if (!sv.vertex.method.equals(iLine_m.cubicTo_m))
							sv.whatSelected.remove(SubSelection.nextControl);
						if (!v.vertex.method.equals(iLine_m.cubicTo_m))
							sv.whatSelected.remove(SubSelection.previousControl);
						nextselection.add(sv);
					}
				}
				if ((arg0.stateMask & SWT.SHIFT) == 0)
					selection.clear();
				selection.addAll(nextselection);
			} else if (arg0.character == '?') {
				selectionTextBounce();
			} else if (arg0.character == '2') {
				for (SelectedVertex v : selection) {
					if (v.onLine.events.size() > (v.vertexIndex + 1) && v.onLine.events.get(v.vertexIndex + 1).method.equals(iLine_m.cubicTo_m))
						v.whatSelected.add(SubSelection.nextControl);
					v.whatSelected.add(SubSelection.postion);
					if (v.vertex.method.equals(iLine_m.cubicTo_m))
						v.whatSelected.add(SubSelection.previousControl);
				}
			} else if (arg0.character == '5') {
				HashSet<CachedLine> touched = new LinkedHashSet<CachedLine>();
				for (SelectedVertex v : selection) {
					touched.add(v.onLine);
				}
				for (CachedLine c : touched) {
					for (int i = 0; i < c.events.size(); i++) {
						if (!known.containsKey(c.events.get(i))) {

							if (!c.events.get(i).method.equals(iLine_m.close_m)) {
								SelectedVertex sv = new SelectedVertex(c, i);
								sv.whatSelected.add(SubSelection.postion);
								selection.add(sv);
								known.put(sv.vertex, sv);
							}
						}
					}
				}
				fireRepaint();
			} else if (arg0.character == '8') {
				selection.clear();
				known.clear();
				fireRepaint();
			} else if (arg0.character == '0') {
				HashMap<CachedLine, Pair<Integer, Integer>> touched = new HashMap<CachedLine, Pair<Integer, Integer>>();
				for (SelectedVertex v : selection) {
					Pair<Integer, Integer> p = touched.get(v.onLine);
					if (p == null)
						touched.put(v.onLine, new Pair<Integer, Integer>(v.vertexIndex, v.vertexIndex));
					else {
						if (v.vertexIndex < p.left)
							p.left = v.vertexIndex;
						if (v.vertexIndex > p.right)
							p.right = v.vertexIndex;
					}
				}
				for (CachedLine c : touched.keySet()) {
					for (int i = touched.get(c).left + 1; i < touched.get(c).right; i++) {
						if (!known.containsKey(c.events.get(i))) {
							SelectedVertex sv = new SelectedVertex(c, i);
							sv.whatSelected.add(SubSelection.postion);
							selection.add(sv);
							known.put(sv.vertex, sv);
						}
					}
				}
				fireRepaint();
			}

		}

		public void mouseClicked(ComponentContainer inside, org.eclipse.swt.widgets.Event arg0) {
			if (selection.size() == 0 && down) {
				// deinstallMousePeer(TweakSplineUI.this.inside);
			}
			
			if ((arg0.stateMask & SWT.COMMAND) !=0) return;
			

			;//System.out.println(" arg0 count is <" + arg0.count + ">");

			if (arg0.count == 2) {
				MarkingMenuBuilder b = null;
				for (SelectedVertex v : selection) {
					MarkingMenuBuilder vv = v.vertex.getDict().get(iLinearGraphicsContext.infoDoubleClick_v);
					b = (b == null ? vv : b.mergeWith(vv));
				}

				;//System.out.println(" popping up for double click <" + b + ">");

				if (b != null) {
					GLComponentWindow.getCurrentWindow(inside).untransformMouseEvent(arg0);

					LinkedHashSet<Event> events = new LinkedHashSet<Event>();
					for (SelectedVertex v : selection) {
						events.add(v.vertex);
					}

					b.setExtraArguments(new Object[] { new ArrayList<Event>(events), TweakSplineUI.this });

					// TODO swt
					b.getMenu(GLComponentWindow.getCurrentWindow(inside).getCanvas(), new Point(arg0.x, arg0.y));

					GLComponentWindow.getCurrentWindow(inside).transformMouseEvent(arg0);

				}

			}
			// else if (arg0.isPopupTrigger()) {
			//
			// }
		}

		public void mouseDragged(ComponentContainer inside, org.eclipse.swt.widgets.Event arg0) {

			if (isMarquee) {

				marqueeRect = marqueeRect.includePoint(arg0.x, arg0.y);
				doMarquee((arg0.stateMask & SWT.SHIFT) != 0);
				inside.requestRedisplay();

				System.err.println(" dragging marquee iis <" + marqueeRect + ">");
			} else if (getCurrentSelectionTool() != null) {
				arg0.doit = false;
				if (!getCurrentSelectionTool().mouseDrag(arg0))
					setCurrentSelectionTool(null);
			} else {

				if (selection.size() > 0)
					arg0.doit = false;

				Vector2 mv = GLComponentWindow.mousePositionForEvent(arg0);

				float dx = mv.x - lx;
				float dy = mv.y - ly;

				if (firstDrag) {
					freezeSelection();
					firstDrag = false;
				}

				offsetSelection(dx, dy, mv.x - ox, mv.y - oy, ox, oy, arg0.stateMask);

				lx = mv.x;
				ly = mv.y;
			}
		}

		public void mouseEntered(ComponentContainer inside, org.eclipse.swt.widgets.Event arg0) {
		}

		public void mouseExited(ComponentContainer inside, org.eclipse.swt.widgets.Event arg0) {
		}

		public void mouseMoved(ComponentContainer inside, org.eclipse.swt.widgets.Event arg0) {
		}

		public void mousePressed(ComponentContainer inside, org.eclipse.swt.widgets.Event arg0) {

			;//System.out.println(" mouse pressed in tweak spline ui <"+getCurrentSelectionTool()+">");
			
			if (getCurrentSelectionTool() != null) {
				arg0.doit = false;
				if (!getCurrentSelectionTool().mouseDown(arg0)) {
					
					;//System.out.println(" mouse down says finish ");
					
					setCurrentSelectionTool(null);
				}
				return;
			}

			if ((arg0.stateMask & SWT.COMMAND) !=0) return;

			down = true;
			lx = ox = arg0.x;
			ly = oy = arg0.y;
			firstDrag = true;

			isReplacing = true;

//			System.err.println(" hittesting <" + arg0.x + " " + arg0.y + "> against <" + selectedableVertex.size() + ">");
			Selectable hit = hittest(arg0.x, arg0.y);

			if (hit != null) {

				if (!Platform.isPopupTrigger(arg0)) {
					if ((arg0.start & SWT.SHIFT) != 0) {
						if (hit.alreadyAdded && hit.current.whatSelected.containsAll(hit.add))
							removeFromSelection(hit);
						else
							addToSelection(hit);

						fireRepaint();
					} else {
						justAdded = hit;
						addToSelection(hit);
						fireRepaint();
					}
				}

				
				
				
				
				if (Platform.isPopupTrigger(arg0) && (arg0.stateMask & SWT.SHIFT) != 0) {

					;//System.out.println(" popup and shift ");

					LinkedHashMap<String, iUpdateable> upup = new LinkedHashMap<String, iUpdateable>();

					aquireToolMenu(upup);

					BetterPopup menu = new SmallMenu().createMenu(upup, GLComponentWindow.getCurrentWindow(inside).getCanvas().getShell(), null);

					GLComponentWindow.getCurrentWindow(inside).untransformMouseEvent(arg0);
					menu.show(new Point(arg0.x, arg0.y));

					GLComponentWindow.getCurrentWindow(inside).transformMouseEvent(arg0);
				} else if (Platform.isPopupTrigger(arg0) && (arg0.stateMask & SWT.SHIFT) == 0) {

					;//System.out.println(" popup over <" + selection + ">");

					MarkingMenuBuilder b = null;
					for (SelectedVertex v : selection) {
						MarkingMenuBuilder vv = v.vertex.getDict().get(iLinearGraphicsContext.infoRightClick_v);
						b = (b == null ? vv : b.mergeWith(vv));
					}

					;//System.out.println(" popping up <" + b + ">");

					if (b != null) {
						GLComponentWindow.getCurrentWindow(inside).untransformMouseEvent(arg0);

						LinkedHashSet<Event> events = new LinkedHashSet<Event>();
						for (SelectedVertex v : selection) {
							events.add(v.vertex);
						}

						b.setExtraArguments(new Object[] { new ArrayList<Event>(events), TweakSplineUI.this });

						// TODO swt marking menu
						// b.getMenu(GLComponentWindow.getCurrentWindow(inside).getCanvas(),
						// new Point(arg0.x, arg0.y));

						GLComponentWindow.getCurrentWindow(inside).transformMouseEvent(arg0);

					}

				}

			}

			if (hit != null) {
				arg0.doit = false;
				isMarquee = false;
			}

		}

		public void mouseReleased(ComponentContainer inside, org.eclipse.swt.widgets.Event arg0) {

			if (arg0.button == SWT.BUTTON3)
				return;

			if (isMarquee) {
				isMarquee = false;

			} else if (getCurrentSelectionTool() != null) {
				arg0.doit = false;
				if (!getCurrentSelectionTool().mouseUp(arg0))
					setCurrentSelectionTool(null);
			} else {

				System.err.println(" mouse released");
				if (justAdded != null) {
					if (firstDrag == true) {
						if ((arg0.stateMask & SWT.SHIFT) == 0) {
							known.clear();
							selection.clear();
						}
						addToSelection(justAdded);
						fireRepaint();
						justAdded = null;
					}
				} else if (justAdded == null && firstDrag == true) {
					if ((arg0.stateMask & SWT.SHIFT) == 0) {
						known.clear();
						selection.clear();
					}
					fireRepaint();
				}

				if (selection != null && selection.size() > 0 && !firstDrag)
					arg0.doit = false;

				if (!firstDrag && selection.size() > 0) {
					thawSelection();
				}
				for (SelectedVertex v : selection) {
					v.frozenPosition.clear();
					v.absPosition.clear();
				}
			}
			isReplacing = false;
			firstDrag = true;
			justAdded = null;
		}
	}

	public ArrayList<SelectedVertex> deselectAllNow() {

		ArrayList<SelectedVertex> old = new ArrayList<SelectedVertex>(selection);

		selection.clear();
		fireRepaint();
		for (SelectedVertex v : selection) {
			v.frozenPosition.clear();
			v.absPosition.clear();
		}

		return old;
	}

	public ArrayList<SelectedVertex> reselectNow(ArrayList<SelectedVertex> rr) {
		ArrayList<SelectedVertex> old = new ArrayList<SelectedVertex>(selection);

		selection.clear();
		selection.addAll(rr);
		fireRepaint();
		for (SelectedVertex v : selection) {
			v.freeze();
		}

		return old;

	}

	public class SelectionStringAcceptor implements iAcceptor<String> {

		public iAcceptor<String> set(String to) {
			Ref<PythonScriptingSystem> pss = new Ref<PythonScriptingSystem>(null);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(inside).getProperty(inside, PythonScriptingSystem.pythonScriptingSystem, pss);
			if (pss.get() != null)
				pss.get().promiseForKey(inside).beginExecute();

			Object o = PythonInterface.getPythonInterface().eval(to);

			o = PythonUtils.maybeToJava(o);

			// beta1
			if (o instanceof iNodeSelection) {
				iNodeSelection ns = (iNodeSelection) o;
				List<Pair<SelectedVertex, Float>> newSelection = ns.selectFrom(getOutput());

				selection.clear();
				known.clear();

				for (Pair<SelectedVertex, Float> s : newSelection) {
					selection.add(s.left);
					known.put(s.left.vertex, s.left);
				}
			} else if (o instanceof PyTuple) {
				selection.clear();
				known.clear();
				for (int i = 0; i < ((PyTuple) o).__len__(); i++) {
					Object o2 = ((PyTuple) o).get(i);
					System.err.println(" eval to <" + o2 + ">");

					iNodeSelection ns = ((iNodeSelection) o2);
					List<Pair<SelectedVertex, Float>> newSelection = ns.selectFrom(getOutput());

					for (Pair<SelectedVertex, Float> s : newSelection) {
						selection.add(s.left);
						known.put(s.left.vertex, s.left);
					}
				}
			}

			if (pss.get() != null)
				pss.get().promiseForKey(inside).endExecute();
			return this;
		}

	}

	public enum SubSelection {
		postion, previousControl, nextControl;
	}

	static HashMap<String, BaseTool> knownNodeTools = new LinkedHashMap<String, BaseTool>();

	static HashMap<String, BaseTool> knownLiveTools = new LinkedHashMap<String, BaseTool>();

	static HashMap<String, iSimpleSelectionTool> knownSelectionTools = new LinkedHashMap<String, iSimpleSelectionTool>();

	private static AbsoluteTool defaultTool;

	/**
	 * modifies selection based on current selection, lets hope this lets us
	 * replace the broken marquee
	 */
	public interface iSimpleSelectionTool {
		public boolean begin(List<SelectedVertex> currentSelection, TweakSplineUI inside);

		public void paint();

		public boolean mouseDown(org.eclipse.swt.widgets.Event at);

		public boolean mouseDrag(org.eclipse.swt.widgets.Event at);

		public boolean mouseUp(org.eclipse.swt.widgets.Event at);

		public int whileKey();
	}

	private iSimpleSelectionTool currentSelectionTool = null;

	static {

		knownLiveTools.put("Relative", defaultTool = new AbsoluteTool(true));
		knownLiveTools.put("Absolute", new AbsoluteTool(false));
		knownLiveTools.put("Scale around center", new ScaleAroundCenterTool());
		knownLiveTools.put("Scale around center (Non-Uniform)", new ScaleAroundCenterTool("ScaleAroundCenterNI"));
		knownLiveTools.put("Rotate around center", new ScaleAroundCenterTool("RotateAroundCenter"));

		knownNodeTools.put("Smooth", new NodeVisitorTool(TSmooth.class));
		knownNodeTools.put("Corner", new NodeVisitorTool(TCorner.class));

		knownNodeTools.put("Subdivide left", new NodeVisitorTool(SubdivideLeft.class));
		knownNodeTools.put("Subdivide right", new NodeVisitorTool(SubdivideRight.class));
		knownNodeTools.put("Make cubic left", new NodeVisitorTool(EnsureCubicLeft.class));
		knownNodeTools.put("Make cubic right", new NodeVisitorTool(EnsureCubicRight.class));
		knownNodeTools.put("Make linear left", new NodeVisitorTool(EnsureLinearLeft.class));
		knownNodeTools.put("Make linear right", new NodeVisitorTool(EnsureLinearRight.class));
		knownNodeTools.put("Delete", new NodeVisitorTool(Delete.class));
		knownNodeTools.put("Break", new NodeVisitorTool(Break.class));

		knownNodeTools.put("Extract whole line to elaboration", new ExtractPathTool());

		knownSelectionTools.put("3d drawing tool", new FreehandTool3d_tweak());

		knownSelectionTools.put("Marquee", new NewMarqueeTool());
		knownSelectionTools.put("Vertical marquee", new VertMarqueeTool());
		knownSelectionTools.put("Horizontal marquee", new HorizontalMarqueeTool());
		knownSelectionTools.put("Soft marquee", new SoftRadiusSelectTool());

	}

	private final List<CachedLine> on;

	private SelectionMousePeer installedPeer;

	public iVisualElement inside;

	List<SelectedVertex> selection = new ArrayList<SelectedVertex>();

	HashMap<CachedLine.Event, SelectedVertex> known = new HashMap<Event, SelectedVertex>();

	HashMap<Vector2, Selectable> selectedableVertex = new HashMap<Vector2, Selectable>();

	BaseTool currentTool = defaultTool;

	boolean isReplacing = false;

	boolean isMarquee = false;

	Rect marqueeRect = new Rect(0, 0, 0, 0);

	boolean needsRecomputation = false;

	MouseInfo currentInfo = new MouseInfo();

	public TweakSplineUI(List<CachedLine> on) {
		this.on = new ArrayList<CachedLine>(on);
	}

	public void addToSelection(Selectable hit) {
		System.err.println(" adding <" + hit + "> <" + hit.add + " to " + hit.current.vertex + ">");
		if (known.containsKey(hit.current.vertex)) {
			known.get(hit.current.vertex).whatSelected.addAll(hit.add);
		} else {
			selection.add(hit.current);
			hit.current.whatSelected.clear();
			hit.current.whatSelected.addAll(hit.add);
			known.put(hit.current.vertex, hit.current);
		}
		System.err.println(" selection now <" + selection + " / " + known + ">");
	}

	public void aquireToolMenu(LinkedHashMap<String, iUpdateable> upup) {

		upup.put("Live tools ...", null);
		for (final Map.Entry<String, BaseTool> m : knownLiveTools.entrySet()) {
			upup.put("" + (m.getValue() == currentTool ? "!" : "") + " \u2333 <b>" + m.getKey() + "</b>", new iUpdateable() {
				public void update() {
					currentTool = m.getValue();
				}
			});
		}

		upup.put("Apply tool ... ", null);

		for (final Map.Entry<String, BaseTool> m : knownNodeTools.entrySet()) {
			upup.put(" \u2335 <b>" + m.getKey() + "</b>", new iUpdateable() {
				public void update() {

					String t = inside.getProperty(SplineComputingOverride.tweak);
					if (t == null)
						t = "";
					final String ft = t;
					System.err.println(" ------- running codegen ---");

					final BaseTool tool = m.getValue();

					tool.populateParameters(inside, new iUpdateable() {
						public void update() {
							iResult expressions = tool.obtainExpressions(inside, on, selection, null);

							Map<String, Object> pp = new HashMap<String, Object>();
							expressions.toProperties(inside, pp);
							System.err.println(" expression: \n" + expressions.toExpression() + "\n properties: \n" + pp);
							System.err.println(" ------- codegen complete ---");

							String ft2 = ft + "\n" + expressions.toExpression();

							// todo,
							// properties

							SplineComputingOverride.tweak.set(inside, inside, ft2);

							needsRecomputation = true;
						}
					});
				}
			});
		}

		upup.put("Use selection tool", null);
		for (final Map.Entry<String, iSimpleSelectionTool> e : knownSelectionTools.entrySet()) {
			String accelerator = "";
			int k = e.getValue().whileKey();

			// TODO swt

			if (k != -1)
				accelerator = "///" + (char) k + "///";

			upup.put(" \u230c <b>" + e.getKey() + "</b> " + accelerator, new iUpdateable() {

				public void update() {
					setCurrentSelectionTool(e.getValue());
				}
			});
		}
	}

	public TweakSplineUI copyFrom(TweakSplineUI tweaking) {
		if (tweaking != null)
			tweaking.copyTo(this);
		return this;
	}

	public void copyTo(TweakSplineUI into) {
		if (into.on.size() == on.size()) {
			for (SelectedVertex s : selection) {
				if (s.onLine != null) {
					int i = on.indexOf(s.onLine);

					if (i != -1)
						if (into.on.get(i) != null)
							if (on.get(i).events.size() == into.on.get(i).events.size()) {
								SelectedVertex sv = new SelectedVertex(into.on.get(i), s.vertexIndex);
								sv.whatSelected.addAll(s.whatSelected);
								sv.amountSelected.putAll(s.amountSelected);
								into.selection.add(sv);
								into.known.put(sv.vertex, sv);
							}
				}
			}
		} else {
			System.err.println(" number differs ? ");
		}

		into.currentTool = currentTool;
	}

	public void deinstallMousePeer(iVisualElement e) {
		if (installedPeer != null) {
			System.err.println(ANSIColorUtils.red(" deinstall peer "));

			RootComponent rc = iVisualElement.rootComponent.get(e);
			if (rc != null)
				rc.removeMousePeer(installedPeer);
			installedPeer = null;
			if (isReplacing) {
				thawSelection();
				selection.clear();
				known.clear();
				isReplacing = false;
			}
		} else {
			System.err.println(ANSIColorUtils.red(" no need to deinstall peer "));

		}
	}

	public void fireRepaint() {
		if (inside != null) {
			GLComponentWindow ee = iVisualElement.enclosingFrame.get(inside);
			if (ee != null)
				ee.getRoot().requestRedisplay();
		}
	}

	public void freezeSelection() {
		for (SelectedVertex v : selection) {
			v.freeze();
		}
	}

	public List<CachedLine> getOutput() {
		if (!isReplacing)
			return on;

		PLineList o = getSplineComputingOverride().new PLineList();
		HashSet<CachedLine> touched = new HashSet<CachedLine>();
		HashMap<CachedLine, CachedLine> originalToNew = new HashMap<CachedLine, CachedLine>();

		for (SelectedVertex v : selection) {
			touched.add(v.onLine);
			Dict p = v.onLine.getProperties();
			Collection<CachedLine> dependsTo = p.get(iLinearGraphicsContext.codeDependsTo);
			if (dependsTo != null) {
				for (CachedLine x : dependsTo)
					touched.add(x);
			}
		}

		for (CachedLine original : on) {
			if (!touched.contains(original))
				o.add(original);
			else {
				CachedLine u = LineUtils.transformLine(original, null, null, null, null);
				new LineUtils().fixProperties(u, original);

				// Dict m = u.getProperties();
				// Set<Entry<Prop, Object>> mm =
				// m.getMap().entrySet();
				// Iterator<Entry<Prop, Object>> i =
				// mm.iterator();
				// while (i.hasNext()) {
				// Entry<Prop, Object> e = i.next();
				// if
				// (e.getKey().getName().startsWith("postProcessed"))
				// {
				// i.remove();
				// }
				// }

				originalToNew.put(original, u);
				o.add(u);
			}
		}

		for (SelectedVertex v : selection) {
			v.thawInto(originalToNew.get(v.onLine));
		}

		o = postProcessLineHook(inside, o);

		return (List<CachedLine>) o;
	}

	private SplineComputingOverride getSplineComputingOverride() {
		iVisualElementOverrides over = inside.getProperty(iVisualElement.overrides);
		if (over instanceof SplineComputingOverride)
			return (SplineComputingOverride) over;
		if (over instanceof iMixinProxy) {
			List list = ((iMixinProxy) over).getCallList();
			for (Object o : list)
				if (o instanceof SplineComputingOverride)
					return (SplineComputingOverride) o;
		}
		return null;
	}

	static public PLineList postProcessLineHook(iVisualElement inside, PLineList o) {

		Object p = postProcess.get(inside);
		if (p == null)
			return o;

		PLineList original = o;

		if (p instanceof PythonCallableMap)
			((PythonCallableMap) p).invoke(o);
		else if (p instanceof PyObject)
			o = Py.tojava(((PyObject) p).__call__(Py.java2py(o)), PLineList.class);
		else if (p instanceof iFilter)
			o = (PLineList) ((iFilter) p).filter(o);
		else
			System.err.println(" unknown object for postprocess hook <" + p + ">");

		return o == null ? original : o;
	}

	public void installMousePeer(iVisualElement e) {
		if (installedPeer == null) {

			System.err.println(ANSIColorUtils.red(" install peer "));

			RootComponent rc = iVisualElement.rootComponent.get(e);
			if (rc != null)
				rc.addMousePeer(installedPeer = new SelectionMousePeer());

			inside = e;
		} else {
			System.err.println(ANSIColorUtils.red(" no need to install peer"));
		}
	}

	public boolean needsRecomputation() {
		boolean b = needsRecomputation;

		needsRecomputation = false;
		return b;
	}

	public void nudgeSelection(int dx, int dy, boolean shift) {

		freezeSelection();
		if (shift) {
			dx *= 10;
			dy *= 10;
		}
		for (SelectedVertex v : selection) {
			System.err.println(" offsetting <" + selection + ">");
			for (SubSelection s : v.whatSelected) {
				Vector2 a = new Vector2();

				v.ongoingPositionFor(s, a);

				v.setThawPositionFor(s, new Vector2(a.x + dx, a.y + dy));
			}
		}

		thawSelection();
	}

	public void offsetSelection(float dx, float dy, float odx, float ody, float ox, float oy, int modifiers) {
		currentInfo.lastOdx = odx;
		currentInfo.lastOdy = ody;
		currentInfo.lastOx = ox;
		currentInfo.lastOy = oy;

		filterMouseInfoByModifiers(modifiers);

		if (currentTool instanceof AbsoluteTool) {
			for (SelectedVertex v : selection) {

				if (v.vertex.getDict().isTrue(iLinearGraphicsContext.noTweak_v, false))
					continue;

				for (SubSelection s : v.whatSelected) {
					Vector2 a = v.frozenPosition.get(s);

					v.setThawPositionFor(s, new Vector2(a.x + currentInfo.lastOdx * v.getAmountSelectedFor(s), a.y + currentInfo.lastOdy * v.getAmountSelectedFor(s)));
				}
			}
		} else {
			String expression = currentTool.getCoordinateDescription(on, selection, currentInfo);
			PyTuple tuple = (PyTuple) PythonInterface.getPythonInterface().eval(expression);
			int c = tuple.__len__();
			List<Pair<SelectedVertex, Float>> all = new ArrayList<Pair<SelectedVertex, Float>>();
			for (int q = 0; q < selection.size(); q++) {
				all.add(new Pair<SelectedVertex, Float>(selection.get(q), 1f));

				selection.get(q).writeToThawInstead = true;
			}
			for (int i = 0; i < c; i++) {
				Object o = tuple.get(i);
				if (o instanceof iWholeCoordTransform) {
					((iWholeCoordTransform) o).setNodes(all);
				}
				if (o instanceof iCoordTransformation) {
					for (int q = 0; q < all.size(); q++) {
						((iCoordTransformation) o).transformNode(1f, all.get(q).left);
					}
				} else {
					assert false : "unknown class in the tuple <" + o + ">";
				}
			}
			for (int q = 0; q < selection.size(); q++) {
				selection.get(q).writeToThawInstead = false;
			}
		}

	}

	public void paint() {

		// draw all nodes and overdraw all selected
		// nodes

		CachedLine line = new CachedLine();
		line.getProperties().put(iLinearGraphicsContext.pointed, true);
		line.getProperties().put(iLinearGraphicsContext.color, new Vector4(0, 0, 0, 0.5f));
		line.getProperties().put(iLinearGraphicsContext.notForExport, true);

		selectedableVertex.clear();

		iLine input = line.getInput();
		for (CachedLine l : on) {

			if (l != null) {
				boolean hidden = l.getProperties().isTrue(iLinearGraphicsContext.hiddenControls, false);

				if (!l.getProperties().isTrue(iLinearGraphicsContext.derived, false)) {
					CachedLineCursor cursor = new CachedLineCursor(l);

					iLinearGraphicsContext customContext = l.getProperties().get(iLinearGraphicsContext.context);
					if (customContext instanceof iTransformingContext) {

					} else {
						customContext = null;
					}

					while (cursor.hasNextSegment()) {
						cursor.next();

						if (cursor.current.method.equals(iLine_m.close_m)) {

						} else {

							Vector2 at = new Vector2();
							if (customContext != null) {
								Object intermediate = ((iTransformingContext) customContext).getIntermediateSpaceForEvent(l, cursor.getCurrent(), -1);
								if (!((iTransformingContext) customContext).convertIntermediateSpaceToDrawingSpace(intermediate, at)) {
									hidden = true;
									continue;
								}
							} else {
								cursor.getAt(at);
							}

							if (!hidden) {
								input.moveTo(at.x, at.y);
								input.setPointAttribute(iLinearGraphicsContext.pointSize_v, 3f);
							}

							if (known.containsKey(cursor.getCurrent()) && known.get(cursor.getCurrent()).whatSelected.contains(SubSelection.postion)) {
							} else {
								selectedableVertex.put(at, new Selectable(new SelectedVertex(l, cursor), SubSelection.postion));
							}
						}
					}
				}
			}
		}

		GLComponentWindow.currentContext.submitLine(line, line.getProperties());
		CachedLine pointLine = new CachedLine();
		pointLine.getProperties().put(iLinearGraphicsContext.pointed, true);
		pointLine.getProperties().put(iLinearGraphicsContext.pointColor, new Vector4(0.5f, 0, 0, 0.5f));
		pointLine.getProperties().put(iLinearGraphicsContext.notForExport, true);
		pointLine.getProperties().put(iLinearGraphicsContext.containsText, true);
		iLine points = pointLine.getInput();

		CachedLine tangentLine = new CachedLine();
		tangentLine.getProperties().put(iLinearGraphicsContext.color, new Vector4(0.5f, 0, 0, 0.5f));
		tangentLine.getProperties().put(iLinearGraphicsContext.notForExport, true);

		iLine tangents = tangentLine.getInput();

		CachedLine amountsLine = new CachedLine();
		amountsLine.getProperties().put(iLinearGraphicsContext.notForExport, true);
		amountsLine.getProperties().put(iLinearGraphicsContext.filled, true);
		iLine amounts = tangentLine.getInput();

		CachedLine tangentUnselectedLine = new CachedLine();
		tangentUnselectedLine.getProperties().put(iLinearGraphicsContext.color, new Vector4(0.5f, 0, 0, 0.5f));
		tangentUnselectedLine.getProperties().put(iLinearGraphicsContext.notForExport, true);

		iLine tangentsUnselected = tangentUnselectedLine.getInput();

		CachedLine connectivesLine = new CachedLine();
		connectivesLine.getProperties().put(iLinearGraphicsContext.color, new Vector4(1, 0, 0, 0.2f));
		// TODO
		// connectivesLine.getProperties().put(iLinearGraphicsContext.strokeType,
		// new BasicStroke(1, BasicStroke.CAP_BUTT,
		// BasicStroke.JOIN_BEVEL, 1,
		// new float[] { 5, 5 }, 0));
		connectivesLine.getProperties().put(iLinearGraphicsContext.notForExport, true);

		iLine connectivesLineInput = connectivesLine.getInput();

		boolean firstText = true;
		int numText = 0;

		for (SelectedVertex v : selection) {

			Vector2 at = new Vector2();
			v.ongoingPositionFor(SubSelection.postion, at);

			if (v.whatSelected.contains(SubSelection.postion)) {
				points.moveTo(at.x, at.y);
				points.setPointAttribute(iLinearGraphicsContext.pointSize_v, 10f);

				String text = v.vertex.getDict().get(iLinearGraphicsContext.infoText_v);
				if (firstText) {
					if (text == null)
						text = " _self.lines[" + on.indexOf(v.onLine) + "].events[" + v.vertexIndex + "]";
					pointLine.getProperties().put(iLinearGraphicsContext.containsText, true);
					points.setPointAttribute(iLinearGraphicsContext.text_v, text);

					SnippetsPlugin.addText(this.inside, text);

					firstText = false;
				} else {
					if (numText++ < 5) {

						if (text == null)
							text = "  " + on.indexOf(v.onLine) + ", " + v.vertexIndex;
						pointLine.getProperties().put(iLinearGraphicsContext.containsText, true);
						points.setPointAttribute(iLinearGraphicsContext.text_v, text);
						SnippetsPlugin.addText(this.inside, text);

						firstText = false;
					}
				}

				List<CachedLine> infoDecoration = v.vertex.getDict().get(iLinearGraphicsContext.infoAnnotation_v);

				if (infoDecoration != null) {
					iLinearGraphicsContext context = v.onLine.getProperties().get(iLinearGraphicsContext.context);
					if (context == null)
						context = GLComponentWindow.currentContext;

					for (CachedLine ll : infoDecoration)
						context.submitLine(ll, ll.getProperties());
				}

				if (v.amountSelected != null && v.amountSelected.containsKey(SubSelection.postion)) {
					int width = 6;
					amounts.moveTo(at.x - width, at.y - width);
					amounts.setPointAttribute(iLinearGraphicsContext.strokeColor_v, new Vector4(1, 0, 0, v.getAmountSelectedFor(SubSelection.postion)));
					amounts.lineTo(at.x + width, at.y - width);
					amounts.setPointAttribute(iLinearGraphicsContext.strokeColor_v, new Vector4(1, 0, 0, v.getAmountSelectedFor(SubSelection.postion)));
					amounts.lineTo(at.x + width, at.y + width);
					amounts.setPointAttribute(iLinearGraphicsContext.strokeColor_v, new Vector4(1, 0, 0, v.getAmountSelectedFor(SubSelection.postion)));
					amounts.lineTo(at.x - width, at.y + width);
					amounts.setPointAttribute(iLinearGraphicsContext.strokeColor_v, new Vector4(1, 0, 0, v.getAmountSelectedFor(SubSelection.postion)));
					amounts.lineTo(at.x - width, at.y - width);
					amounts.setPointAttribute(iLinearGraphicsContext.strokeColor_v, new Vector4(1, 0, 0, v.getAmountSelectedFor(SubSelection.postion)));
				}

				Selectable sv = new Selectable(v, SubSelection.postion);
				sv.alreadyAdded = true;
				selectedableVertex.put(at, sv);

				if (v.vertexIndex + 1 < v.onLine.events.size()) {
					if (v.onLine.events.get(v.vertexIndex + 1).method.equals(iLine_m.moveTo_m)) {
						connectivesLineInput.moveTo(at.x, at.y);
						Vector2 vv = v.onLine.events.get(v.vertexIndex + 1).getDestination();
						connectivesLineInput.lineTo(vv.x, vv.y);
					}
				}
				if (v.vertexIndex < v.onLine.events.size() - 1 && !v.onLine.getProperties().isTrue(iLinearGraphicsContext.containsDepth, false)) {
//					Arrows arrows = new Arrows();
//
//					float distance = v.onLine.events.get(v.vertexIndex).getDestination().distanceFrom(v.onLine.events.get(v.vertexIndex + 1).getDestination());
//
//					if (distance > 10) {
//						CachedLine x = arrows.getArrowFor(v.onLine, v.onLine.events.get(v.vertexIndex + 1), v.vertexIndex + 1, 10f / distance, 15, (float) Math.PI, 1);
//						x.getProperties().put(iLinearGraphicsContext.filled, true);
//						x.getProperties().put(iLinearGraphicsContext.fillColor, new Vector4(0, 0, 0, 0.2));
//						GLComponentWindow.currentContext.submitLine(x, x.getProperties());
//					}
				}
			}

			if (v.vertex.method.equals(iLine_m.cubicTo_m)) {
				if (!v.whatSelected.contains(SubSelection.previousControl)) {
					tangentsUnselected.moveTo(at.x, at.y);
					Vector2 c1 = new Vector2();
					v.ongoingPositionFor(SubSelection.previousControl, c1);
					tangentsUnselected.lineTo(c1.x, c1.y);
					input.moveTo(c1.x, c1.y);
					input.setPointAttribute(iLinearGraphicsContext.pointSize_v, 3f);

					Selectable sv = new Selectable(v, SubSelection.previousControl);
					sv.alreadyAdded = true;
					selectedableVertex.put(c1, sv);

				} else {
					tangents.moveTo(at.x, at.y);
					Vector2 c1 = new Vector2();
					v.ongoingPositionFor(SubSelection.previousControl, c1);
					tangents.lineTo(c1.x, c1.y);
					points.moveTo(c1.x, c1.y);
					points.setPointAttribute(iLinearGraphicsContext.pointSize_v, 10f);

					if (v.amountSelected != null && v.amountSelected.containsKey(SubSelection.previousControl)) {
						int width = 6;
						amounts.moveTo(c1.x - width, c1.y - width);
						amounts.setPointAttribute(iLinearGraphicsContext.strokeColor_v, new Vector4(0.5f, 0, 0, v.getAmountSelectedFor(SubSelection.previousControl)));
						amounts.lineTo(c1.x + width, c1.y - width);
						amounts.setPointAttribute(iLinearGraphicsContext.strokeColor_v, new Vector4(0.5f, 0, 0, v.getAmountSelectedFor(SubSelection.previousControl)));
						amounts.lineTo(c1.x + width, c1.y + width);
						amounts.setPointAttribute(iLinearGraphicsContext.strokeColor_v, new Vector4(0.5f, 0, 0, v.getAmountSelectedFor(SubSelection.previousControl)));
						amounts.lineTo(c1.x - width, c1.y + width);
						amounts.setPointAttribute(iLinearGraphicsContext.strokeColor_v, new Vector4(0.5f, 0, 0, v.getAmountSelectedFor(SubSelection.previousControl)));
						amounts.lineTo(c1.x - width, c1.y - width);
						amounts.setPointAttribute(iLinearGraphicsContext.strokeColor_v, new Vector4(0.5f, 0, 0, v.getAmountSelectedFor(SubSelection.previousControl)));
					}

					Selectable sv = new Selectable(v, SubSelection.previousControl);
					sv.alreadyAdded = false;
					selectedableVertex.put(c1, sv);
				}
			}

			if (v.onLine.events.size() > v.vertexIndex + 1) {
				Event<?> q = v.onLine.events.get(v.vertexIndex + 1);
				if (q.method.equals(iLine_m.cubicTo_m)) {
					if (v.whatSelected.contains(SubSelection.nextControl)) {
						tangents.moveTo(at.x, at.y);
						Vector2 c2 = new Vector2();
						v.ongoingPositionFor(SubSelection.nextControl, c2);
						tangents.lineTo(c2.x, c2.y);
						points.moveTo(c2.x, c2.y);
						points.setPointAttribute(iLinearGraphicsContext.pointSize_v, 10f);

						Selectable sv = new Selectable(v, SubSelection.nextControl);
						sv.alreadyAdded = true;
						selectedableVertex.put(c2, sv);

						if (v.amountSelected != null && v.amountSelected.containsKey(SubSelection.nextControl)) {
							int width = 6;
							amounts.moveTo(c2.x - width, c2.y - width);
							amounts.setPointAttribute(iLinearGraphicsContext.strokeColor_v, new Vector4(0.5f, 0, 0, v.getAmountSelectedFor(SubSelection.nextControl)));
							amounts.lineTo(c2.x + width, c2.y - width);
							amounts.setPointAttribute(iLinearGraphicsContext.strokeColor_v, new Vector4(0.5f, 0, 0, v.getAmountSelectedFor(SubSelection.nextControl)));
							amounts.lineTo(c2.x + width, c2.y + width);
							amounts.setPointAttribute(iLinearGraphicsContext.strokeColor_v, new Vector4(0.5f, 0, 0, v.getAmountSelectedFor(SubSelection.nextControl)));
							amounts.lineTo(c2.x - width, c2.y + width);
							amounts.setPointAttribute(iLinearGraphicsContext.strokeColor_v, new Vector4(0.5f, 0, 0, v.getAmountSelectedFor(SubSelection.nextControl)));
							amounts.lineTo(c2.x - width, c2.y - width);
							amounts.setPointAttribute(iLinearGraphicsContext.strokeColor_v, new Vector4(0.5f, 0, 0, v.getAmountSelectedFor(SubSelection.nextControl)));
						}

					} else {
						tangentsUnselected.moveTo(at.x, at.y);
						Vector2 c2 = new Vector2();
						v.ongoingPositionFor(SubSelection.nextControl, c2);
						tangentsUnselected.lineTo(c2.x, c2.y);
						input.moveTo(c2.x, c2.y);
						input.setPointAttribute(iLinearGraphicsContext.pointSize_v, 3f);

						Selectable sv = new Selectable(v, SubSelection.nextControl);
						sv.alreadyAdded = false;
						selectedableVertex.put(c2, sv);

					}
				} else
					v.whatSelected.remove(SubSelection.nextControl);
			}
		}

		if (isMarquee) {
			CachedLine marqueLine = new CachedLine();
			marqueLine.getProperties().put(iLinearGraphicsContext.color, new Vector4(0.5f, 0, 0, 0.9f));
			marqueLine.getProperties().put(iLinearGraphicsContext.strokeColor, new Vector4(0.5f, 0, 0, 0.9f));
			marqueLine.getProperties().put(iLinearGraphicsContext.fillColor, new Vector4(0.25f, 0, 0, 0.2f));
			// TODO swt
			// marqueLine.getProperties().put(iLinearGraphicsContext.strokeType,
			// new BasicStroke(1, BasicStroke.CAP_BUTT,
			// BasicStroke.JOIN_BEVEL,
			// 1, new float[] { 5, 5 }, 0));
			marqueLine.getProperties().put(iLinearGraphicsContext.filled, true);
			marqueLine.getProperties().put(iLinearGraphicsContext.notForExport, true);

			marqueLine.getInput().moveTo((float) marqueeRect.x, (float) marqueeRect.y);
			marqueLine.getInput().lineTo((float) (marqueeRect.x + marqueeRect.w), (float) marqueeRect.y);
			marqueLine.getInput().lineTo((float) (marqueeRect.x + marqueeRect.w), (float) (marqueeRect.y + marqueeRect.h));
			marqueLine.getInput().lineTo((float) (marqueeRect.x), (float) (marqueeRect.y + marqueeRect.h));
			marqueLine.getInput().lineTo((float) (marqueeRect.x), (float) (marqueeRect.y));

			GLComponentWindow.currentContext.submitLine(marqueLine, marqueLine.getProperties());
		}

		if (getCurrentSelectionTool() != null) {
			getCurrentSelectionTool().paint();
		}

		GLComponentWindow.currentContext.submitLine(tangentUnselectedLine, tangentUnselectedLine.getProperties());
		GLComponentWindow.currentContext.submitLine(tangentLine, tangentLine.getProperties());
		GLComponentWindow.currentContext.submitLine(pointLine, pointLine.getProperties());
		GLComponentWindow.currentContext.submitLine(connectivesLine, connectivesLine.getProperties());
	}

	public void removeFromSelection(Selectable hit) {
		hit.current.whatSelected.removeAll(hit.add);
		if (hit.current.whatSelected.size() == 0) {
			known.remove(hit.current.vertex);
			selection.remove(hit.current);
		}
	}

	public void selectionTextBounce() {
		AbsoluteNodeDescription desc = new AbsoluteNodeDescription();
		ArrayList<SelectedVertex> q = new ArrayList<SelectedVertex>(selection);

		List<iResult> description = new ArrayList<iResult>();
		while (q.size() > 0) {
			List<iResult> d = desc.describe(getOutput(), q);
			if (d == null)
				break;
			description.addAll(d);
		}

		if (description.size() == 1) {
			String string = description.get(0).toExpression();
			new PopupTextBox.Modal().getString(PopupTextBox.Modal.elementAt(inside), "selection : ", string, new SelectionStringAcceptor());
		} else if (description.size() > 1) {

			String string = "(";
			for (int i = 0; i < description.size(); i++) {
				String s1 = description.get(i).toExpression();
				string += s1 + ", ";
			}
			string += ")";

			new PopupTextBox.Modal().getString(PopupTextBox.Modal.elementAt(inside), "selection : ", string, new SelectionStringAcceptor());
		}
	}

	public boolean somethingSelected() {
		return selection.size() != 0;
	}

	public void thawSelection() {

		System.err.println(" thawing");

		System.err.println(" ------- running test codegen ---");

		if (currentTool instanceof BaseTool) {

			BaseTool tool = currentTool;

			setExtraArgumentsForTool(tool, selection);

			iResult expressions = tool.obtainExpressions(inside, on, selection, currentInfo);

			Map<String, Object> pp = new HashMap<String, Object>();
			expressions.toProperties(inside, pp);
			String ee = expressions.toExpression();
			System.err.println(" expression: \n" + ee + "\n properties: \n" + pp);
			System.err.println(" ------- codegen complete ---");

			for (SelectedVertex v : selection) {
				v.frozenPosition.clear();
				v.absPosition.clear();
			}

			String t = inside.getProperty(SplineComputingOverride.tweak);
			if (t == null)
				t = "";
			t += "\n" + ee;

			if (ee.trim().length() > 0) {
				SplineComputingOverride.tweak.set(inside, inside, t);
				// todo,
				// properties

				needsRecomputation = true;
			}
		}
	}

	public void setExtraArgumentsForTool(BaseTool tool, List<SelectedVertex> selection) {
		for (SelectedVertex v : selection) {
			iLinearGraphicsContext context = v.onLine.getProperties().get(iLinearGraphicsContext.context);
			if (context instanceof iTransformingContext) {
				Object state = ((iTransformingContext) context).getTransformState();
				String p = TweakSplineCodeGen.uniqProperty(this.inside, null);

				;//System.out.println(" setting property <" + p + "> to be <" + state + ">");
				this.inside.setProperty(new VisualElementProperty(p), state);
				tool.getExtraArguments().put("camera", "_self." + p);
				break;
			}
		}
	}

	protected void doMarquee(boolean b) {
		for (Map.Entry<Vector2, Selectable> e : selectedableVertex.entrySet()) {
			if (marqueeRect.isInside(e.getKey())) {

				if (b) {
					removeFromSelection(e.getValue());
				} else {
					addToSelection(e.getValue());
				}
			}
		}
	}

	protected void filterMouseInfoByModifiers(int modifiers) {
		if ((modifiers & java.awt.Event.SHIFT_MASK) != 0) {
			if (Math.abs(currentInfo.lastOdx) > 2 * Math.abs(currentInfo.lastOdy)) {
				currentInfo.lastOdy = 0;
			} else if (Math.abs(currentInfo.lastOdy) > 2 * Math.abs(currentInfo.lastOdx)) {
				currentInfo.lastOdx = 0;
			} else if (Math.abs(currentInfo.lastOdy) > Math.abs(currentInfo.lastOdx)) {
				currentInfo.lastOdx = (currentInfo.lastOdx < 0 ? -1 : 1) * Math.abs(currentInfo.lastOdy);
			} else if (Math.abs(currentInfo.lastOdx) > Math.abs(currentInfo.lastOdy)) {
				currentInfo.lastOdy = (currentInfo.lastOdy < 0 ? -1 : 1) * Math.abs(currentInfo.lastOdx);
			}
		}

	}

	protected Selectable hittest(float x, float y) {
		Vector2 v = new Vector2(x, y);
		float min = (float) (30 * Math.sqrt(GLComponentWindow.getCurrentWindow(null).getXScale() * GLComponentWindow.getCurrentWindow(null).getYScale()));
		Selectable selected = null;
		for (Map.Entry<Vector2, Selectable> e : selectedableVertex.entrySet()) {
			if (v.distanceFrom(e.getKey()) < min) {
				min = v.distanceFrom(e.getKey());
				selected = e.getValue();
			}
		}
		return selected;
	}

	void setCurrentSelectionTool(iSimpleSelectionTool currentSelectionTool) {
		this.currentSelectionTool = currentSelectionTool;
		if (currentSelectionTool != null)
			if (!currentSelectionTool.begin(selection, TweakSplineUI.this)) {
				this.currentSelectionTool = null;
			}
		maintainWithKey = -1;

	}

	int maintainWithKey = -1;

	iSimpleSelectionTool getCurrentSelectionTool() {

		if (currentSelectionTool != null && maintainWithKey != -1) {
			if (!GLComponentWindow.keysDown.contains(maintainWithKey)) {
				currentSelectionTool.mouseUp(null);
				setCurrentSelectionTool(null);
			}
		}

		if (currentSelectionTool == null) {
			checkKeyDowns();
			return currentSelectionTool;
		} else
			return currentSelectionTool;
	}

	private void checkKeyDowns() {
		Collection<iSimpleSelectionTool> vv = knownSelectionTools.values();
		for (iSimpleSelectionTool t : vv) {
			if (GLComponentWindow.keysDown.contains(t.whileKey())) {
				setCurrentSelectionTool(t);
				maintainWithKey = currentSelectionTool != null ? t.whileKey() : -1;
			}
		}
	}
}
