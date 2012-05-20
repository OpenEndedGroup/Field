package field.core.plugins.log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import field.bytecode.protect.DeferedInQueue.iProvidesQueue;
import field.bytecode.protect.DeferedInQueue.iRegistersUpdateable;
import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.InQueue;
import field.core.Constants;
import field.core.dispatch.iVisualElement;
import field.core.plugins.log.AssemblingLogging.Link;
import field.core.plugins.log.AssemblingLogging.LinkType;
import field.core.plugins.log.AssemblingLogging.MoveEvent;
import field.core.plugins.log.AssemblingLogging.iUndoable;
import field.core.plugins.log.ElementInvocationLogging.DidGetLocalVariable;
import field.core.plugins.log.ElementInvocationLogging.DidGetLocalVariableByAutoExecution;
import field.core.plugins.log.ElementInvocationLogging.DidSetLocalVariable;
import field.core.plugins.log.ElementInvocationLogging.ElementExecutionBegin;
import field.core.plugins.log.ElementInvocationLogging.ElementExecutionFocusBegin;
import field.core.plugins.log.ElementInvocationLogging.iProvidesContextStack;
import field.core.plugins.log.ElementInvocationLogging.iSuspendedContext;
import field.core.plugins.log.Logging.LoggingEventModel;
import field.core.plugins.log.Logging.LoggingEventType;
import field.core.plugins.log.Logging.UpdateCycle;
import field.core.plugins.log.Logging.iLoggingEvent;
import field.core.plugins.log.Logging.iTranscriptViewGeneration;
import field.core.plugins.log.Logging.iTranscriptViewTransformation;
import field.core.plugins.selection.SelectionSetDriver;
import field.math.graph.GraphNodeSearching;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.util.PythonUtils;
import field.util.TaskQueue;

public class TranscriptViewing {

	@Woven
	static public class FilterEventsWithNoDirectEffect implements iProvidesQueue, iTranscriptViewTransformation {
		TaskQueue q = new TaskQueue();

		public void filterView(LoggingEventModel root) {

			new GraphNodeSearching.GraphNodeVisitor_depthFirst<LoggingEventModel>(false) {
				@Override
				protected VisitCode visit(LoggingEventModel n) {

					// check
					// to
					// see
					// if
					// there
					// is a
					// call
					// that
					// follows
					// directly
					// on
					// from
					// a
					// set
					for (int i = 0; i < n.getChildren().size(); i++) {
						LoggingEventModel c = n.getChildren().get(i);
						if (c.node != null && (c.node instanceof MoveEvent)) {
							Link last = (Link) ((MoveEvent) c.node).move.links.get(((MoveEvent) c.node).move.links.size() - 1);
							if (last.before != null && last.after != null && last.before.equals(last.after))
								removeChild(n, c);
						}
					}

					return VisitCode.cont;
				}
			}.apply(root);
			q.update();
		}

		public iRegistersUpdateable getQueueFor(Method m) {
			return q;
		}

		@InQueue
		protected void removeChild(LoggingEventModel model, LoggingEventModel child) {
			model.removeChild(child);
		}
		
		@Override
		public String toString() {
			return "Only Direct Effects";
		}
	}

	@Woven
	static public class FilterPotentiallyRedundantCalls implements iProvidesQueue, iTranscriptViewTransformation {

		TaskQueue q = new TaskQueue();

		public void filterView(LoggingEventModel root) {

			new GraphNodeSearching.GraphNodeVisitor_depthFirst<LoggingEventModel>(false) {
				@Override
				protected VisitCode visit(LoggingEventModel n) {

					// check
					// to
					// see
					// if
					// there
					// is a
					// call
					// that
					// follows
					// directly
					// on
					// from
					// a
					// set
					for (int i = 0; i < n.getChildren().size(); i++) {
						LoggingEventModel c = n.getChildren().get(i);
						if (c.node != null && (c.node instanceof MoveEvent)) {
							Link last = (Link) ((MoveEvent) c.node).move.links.get(((MoveEvent) c.node).move.links.size() - 1);
							if (last.type == LinkType.set) {
								if (i < n.getChildren().size() - 1) {
									LoggingEventModel m = n.getChildren().get(i + 1);
									if (m.node instanceof MoveEvent) {
										Link last2 = (Link) ((MoveEvent) m.node).move.links.get(((MoveEvent) m.node).move.links.size() - 1);
										if (last2.type == LinkType.call && (((MoveEvent) c.node).move.expression.startsWith((((MoveEvent) m.node).move.expression)))) {
											removeChild(n, n.getChildren().get(i + 1));
										}
									}
								}
							}
						}
					}

					return VisitCode.cont;
				}
			}.apply(root);
			q.update();
		}

		public iRegistersUpdateable getQueueFor(Method m) {
			return q;
		}

		@InQueue
		protected void removeChild(LoggingEventModel model, LoggingEventModel child) {
			model.removeChild(child);
		}
		
		@Override
		public String toString() {
			return "No Redundant Calls";
		}
	}

	@Woven
	static public class GroupByElementWithin implements iProvidesQueue, iTranscriptViewTransformation {

		TaskQueue q = new TaskQueue();

		public void filterView(LoggingEventModel root) {

			new GraphNodeSearching.GraphNodeVisitor_depthFirst<LoggingEventModel>(false) {
				@Override
				protected VisitCode visit(LoggingEventModel n) {

					iVisualElement currentFocus = null;
					LoggingEventModel currentIntermediate = null;

					// check
					// to
					// see
					// if
					// there
					// is a
					// call
					// that
					// follows
					// directly
					// on
					// from
					// a
					// set
					for (int i = 0; i < n.getChildren().size(); i++) {
						LoggingEventModel c = n.getChildren().get(i);

						if (c.node instanceof iProvidesContextStack) {
							LinkedHashSet<iLoggingEvent> context = ((iProvidesContextStack) c.node).getSuspendedContext();
							iVisualElement newFocus = null;
							boolean isFragment = true;

							for (iLoggingEvent cc : context) {

								if (cc instanceof ElementExecutionBegin) {
									newFocus = (iVisualElement) ((ElementExecutionBegin) cc).getToken();
									isFragment = false;
								}
								if (cc instanceof ElementExecutionFocusBegin) {
									newFocus = (iVisualElement) ((ElementExecutionFocusBegin) cc).getToken();
								}
							}

							if (newFocus == currentFocus) {
							} else {
								currentFocus = newFocus;
								if (newFocus == null) {
									currentIntermediate = null;
								} else
									currentIntermediate = new LoggingEventModel(LoggingEventType.informative, null, "<HTML><font color='#" + Constants.defaultTreeColor + "'> execution inside <" + ElementInvocationLogging.describeElementLink(newFocus) + " " + (isFragment ? "(fragment)" : ""));
							}
							insertParent(n, currentIntermediate, c);
						} else {
							currentFocus = null;
							currentIntermediate = null;
						}
					}

					return VisitCode.cont;
				}
			}.apply(root);
			q.update();
		}

		public iRegistersUpdateable getQueueFor(Method m) {
			return q;
		}

		@InQueue
		protected void insertParent(LoggingEventModel parent, LoggingEventModel intermediate, LoggingEventModel child) {
			if (intermediate == null)
				return;

			if (!parent.getChildren().contains(intermediate))
				parent.addChild(intermediate);
			if (parent.getChildren().contains(child))
				parent.removeChild(child);
			intermediate.addChild(child);
		}

		@InQueue
		protected void removeChild(LoggingEventModel model, LoggingEventModel child) {
			model.removeChild(child);
		}
		
		@Override
		public String toString() {
			return "Grouped by Element";
		}
	}

	static public class Join implements iTranscriptViewGeneration {
		private final iTranscriptViewGeneration gen;

		private final iTranscriptViewTransformation[] trans;

		public Join(iTranscriptViewGeneration gen, iTranscriptViewTransformation... trans) {
			this.gen = gen;
			this.trans = trans;
		}

		public boolean generateView(ArrayList<iLoggingEvent> transcript, LoggingEventModel root) {
			gen.generateView(transcript, root);
			for (int i = 0; i < trans.length; i++) {
				trans[i].filterView(root);
			}
			return true;
		}
		
		@Override
		public String toString() {
			return gen+" & "+Arrays.asList(trans);
		}
	}

	// suitible for a "hard bake"
	@Woven
	static public class OnlyLastEffect implements iProvidesQueue, iTranscriptViewTransformation {
		TaskQueue q = new TaskQueue();

		public void filterView(LoggingEventModel root) {

			// order goes from most recent
			// to least recent, unless we
			// reverse it

			final HashMap<Object, LoggingEventModel> targets = new HashMap<Object, LoggingEventModel>();

			new GraphNodeSearching.GraphNodeVisitor_depthFirst<LoggingEventModel>(false, false) {
				@Override
				protected VisitCode visit(LoggingEventModel n) {

					if (n.node instanceof MoveEvent) {
						MoveEvent m = ((MoveEvent) n.node);

						if (m.move.after != null && m.move.current != null && targets.get(m.move.current) == null) {
							// keep
							// it,
							// but
							// repurpose
							// it
							LoggingEventModel q = new LoggingEventModel(LoggingEventType.replayable, new SimpleStringEvent(m.move.current, m.move.after, ((Link) m.move.links.get(m.move.links.size() - 1)).path, m.getDoExpression(), m.getUndoExpression(), m.isError()), null);
							addChild((LoggingEventModel) n.getParents().get(0), q);
							removeChild((LoggingEventModel) n.getParents().get(0), n);
							targets.put(m.move.current, q);
						} else if (m.move.after != null && m.move.current != null && targets.get(m.move.current) != null) {
							removeChild((LoggingEventModel) n.getParents().get(0), n);
						}
					}

					return VisitCode.cont;
				}
			}.apply(root);
			q.update();
		}

		public iRegistersUpdateable getQueueFor(Method m) {
			return q;
		}

		@InQueue
		protected void addChild(LoggingEventModel model, LoggingEventModel child) {
			model.addChild(child);
		}

		@InQueue
		protected void removeChild(LoggingEventModel model, LoggingEventModel child) {
			model.removeChild(child);
		}
		
		@Override
		public String toString() {
			return "Only last effect";
		}

	}

	// groups by update cycle, except for the most recent update
	// cycle
	// further groups by box execution ?
	static public class Overview implements iTranscriptViewGeneration {
		public boolean generateView(ArrayList<iLoggingEvent> transcript, LoggingEventModel root) {
			ArrayList<LoggingEventModel> a = new ArrayList<LoggingEventModel>(root.getChildren());
			for (LoggingEventModel m : a)
				root.removeChild(m);

			LoggingEventModel parent = root;

			for (int i = transcript.size() - 1; i >= 0; i--) {
				iLoggingEvent e = transcript.get(i);

				if (e instanceof UpdateCycle) {
					LoggingEventModel em = new LoggingEventModel(LoggingEventType.replayable, e, null);
					// if
					// (next
					// !=
					// null
					// &&
					// next
					// !=
					// root
					// &&
					// next.getChildren().size()
					// ==
					// 0)
					// root.removeChild(next);
					root.addChild(em);
					// next
					// =
					// em;

					parent = em;
				} else {
					LoggingEventModel em = new LoggingEventModel(LoggingEventType.replayable, e, null);
					parent.addChild(em);
				}
			}
			// if (next != null && next !=
			// root &&
			// next.getChildren().size() ==
			// 0) root.removeChild(next);

			return true;
		}

		@Override
		public String toString() {
			return "Overview";
		}
		
	}

	static public class PivotToCommonTarget implements iTranscriptViewTransformation {

		public void filterView(LoggingEventModel root) {
			final LoggingEventModel tmpRoot = new LoggingEventModel(LoggingEventType.informative, null, "tmpRoot");

			final HashMap<Object, LoggingEventModel> targets = new HashMap<Object, LoggingEventModel>();

			new GraphNodeSearching.GraphNodeVisitor_depthFirst<LoggingEventModel>(false, false) {
				@Override
				protected VisitCode visit(LoggingEventModel n) {

					;//System.out.println(" filtering node " + n.node + " " + (n.node == null ? null : n.node.getClass()));

					if (n.node instanceof MoveEvent) {
						MoveEvent m = ((MoveEvent) n.node);

						if (m.move.after != null && m.move.current != null) {
							LoggingEventModel parent = targets.get(m.move.current);
							if (parent == null) {
								parent = new LoggingEventModel(LoggingEventType.informative, null, "var \u2014 <b>" + m.getDoExpressionLHS()+ "</b>");
								tmpRoot.addChild(parent);
								targets.put(m.move.current, parent);
							}
							parent.addChild(new LoggingEventModel(LoggingEventType.replayable, n.node, n.label));
						}
					} else if (n.node instanceof DidSetLocalVariable) {
						DidSetLocalVariable m = ((DidSetLocalVariable) n.node);
						LoggingEventModel parent = targets.get(m.name);
						if (parent == null) {
							parent = new LoggingEventModel(LoggingEventType.informative, null, "var \u2014 <b>" + m.name + "</b>");
							tmpRoot.addChild(parent);
							targets.put(m.name, parent);
						}
						parent.addChild(new LoggingEventModel(LoggingEventType.replayable, n.node, n.label));
					} else if (n.node instanceof DidGetLocalVariable) {
						DidGetLocalVariable m = ((DidGetLocalVariable) n.node);
						LoggingEventModel parent = targets.get(m.name);
						if (parent == null) {
							parent = new LoggingEventModel(LoggingEventType.informative, null, "var \u2014 <b>" + m.name + "</b>");
							tmpRoot.addChild(parent);
							targets.put(m.name, parent);
						}
						parent.addChild(new LoggingEventModel(LoggingEventType.replayable, n.node, n.label));
					} else if (n.node instanceof DidGetLocalVariableByAutoExecution) {
						DidGetLocalVariableByAutoExecution m = ((DidGetLocalVariableByAutoExecution) n.node);
						LoggingEventModel parent = targets.get(m.name);
						if (parent == null) {
							parent = new LoggingEventModel(LoggingEventType.informative, null, "var \u2014 <b>" + m.name + "</b>");
							tmpRoot.addChild(parent);
							targets.put(m.name, parent);
						}
						parent.addChild(new LoggingEventModel(LoggingEventType.replayable, n.node, n.label));
					}

					return VisitCode.cont;
				}
			}.apply(root);

			{
				ArrayList<LoggingEventModel> a = new ArrayList<LoggingEventModel>(root.getChildren());
				for (LoggingEventModel m : a)
					root.removeChild(m);
			}
			{
				ArrayList<LoggingEventModel> a = new ArrayList<LoggingEventModel>(tmpRoot.getChildren());
				for (LoggingEventModel m : a) {
					tmpRoot.removeChild(m);
					root.addChild(m);
				}
			}
		}
		
		@Override
		public String toString() {
			return "By Name";
		}
		
	}

	static public class PivotToCommonClass implements iTranscriptViewTransformation {

		public void filterView(LoggingEventModel root) {
			final LoggingEventModel tmpRoot = new LoggingEventModel(LoggingEventType.informative, null, "tmpRoot");

			final HashMap<Object, LoggingEventModel> targets = new HashMap<Object, LoggingEventModel>();

			new GraphNodeSearching.GraphNodeVisitor_depthFirst<LoggingEventModel>(false, false) {
				@Override
				protected VisitCode visit(LoggingEventModel n) {

					;//System.out.println(" filtering node " + n.node + " " + (n.node == null ? null : n.node.getClass()));

					if (n.node instanceof DidSetLocalVariable) {
						DidSetLocalVariable m = ((DidSetLocalVariable) n.node);
						LoggingEventModel parent = targets.get(safeClass(m.o));
						if (parent == null) {
							parent = new LoggingEventModel(LoggingEventType.informative, null, "var \u2014 <b>" + safeClass(m.o) + "</b>");
							tmpRoot.addChild(parent);
							targets.put(safeClass(m.o), parent);
						}
						parent.addChild(new LoggingEventModel(LoggingEventType.replayable, n.node, n.label));
					} else if (n.node instanceof DidGetLocalVariable) {
						DidGetLocalVariable m = ((DidGetLocalVariable) n.node);
						LoggingEventModel parent = targets.get(safeClass(m.o));
						if (parent == null) {
							parent = new LoggingEventModel(LoggingEventType.informative, null, "var \u2014 <b>" + safeClass(m.o) + "</b>");
							tmpRoot.addChild(parent);
							targets.put(safeClass(m.o), parent);
						}
						parent.addChild(new LoggingEventModel(LoggingEventType.replayable, n.node, n.label));
					} else if (n.node instanceof DidGetLocalVariableByAutoExecution) {
						DidGetLocalVariableByAutoExecution m = ((DidGetLocalVariableByAutoExecution) n.node);
						LoggingEventModel parent = targets.get(safeClass(m.got));
						if (parent == null) {
							parent = new LoggingEventModel(LoggingEventType.informative, null, "var \u2014 <b>" + safeClass(m.got) + "</b>");
							tmpRoot.addChild(parent);
							targets.put(safeClass(m.got), parent);
						}
						parent.addChild(new LoggingEventModel(LoggingEventType.replayable, n.node, n.label));
					}

					return VisitCode.cont;
				}

				private Object safeClass(Object o) {
					if (o == null) return "<i>unknown</i>";
					o = PythonUtils.maybeToJava(o);
					return o.getClass().getSimpleName();
				}
			}.apply(root);

			{
				ArrayList<LoggingEventModel> a = new ArrayList<LoggingEventModel>(root.getChildren());
				for (LoggingEventModel m : a)
					root.removeChild(m);
			}
			{
				ArrayList<LoggingEventModel> a = new ArrayList<LoggingEventModel>(tmpRoot.getChildren());
				for (LoggingEventModel m : a) {
					tmpRoot.removeChild(m);
					root.addChild(m);
				}
			}
		}

		@Override
		public String toString() {
			return "By Value";
		}

	}

	static public class PivotToElement implements iTranscriptViewTransformation {

		public void filterView(LoggingEventModel root) {
			final LoggingEventModel tmpRoot = new LoggingEventModel(LoggingEventType.informative, null, "tmpRoot");

			final List<LoggingEventModel> flat = new ArrayList<LoggingEventModel>();

			new GraphNodeSearching.GraphNodeVisitor_depthFirst<LoggingEventModel>(false, false) {
				@Override
				protected VisitCode visit(LoggingEventModel n) {

					if (n.getParents().size() > 0)
						flat.add(n);
					return VisitCode.cont;
				}
			}.apply(root);

			HashMap<List<iLoggingEvent>, LoggingEventModel> contexts = new LinkedHashMap<List<iLoggingEvent>, LoggingEventModel>();
			// every time there is a
			// different context, change
			// next;

			List<iLoggingEvent> currentContext = new ArrayList<iLoggingEvent>();

			LoggingEventModel currentParent = tmpRoot;

			for (int i = 0; i < flat.size(); i++) {
				LoggingEventModel f = flat.get(i);
				LinkedHashSet<iLoggingEvent> sc;
				ArrayList<iLoggingEvent> aca;
				if (f.node instanceof iProvidesContextStack) {
					sc = ((iProvidesContextStack) f.node).getSuspendedContext();
					aca = new ArrayList<iLoggingEvent>(sc);
				} else {
					sc = new LinkedHashSet<iLoggingEvent>();
					aca = new ArrayList<iLoggingEvent>();
				}
				if (!aca.equals(currentContext)) {
					// remove
					// eveything
					// that
					// isn't
					// the
					// common
					// root
					// between
					// currentContext
					// and
					// sc
					for (int q = currentContext.size() - 1; q >= 0; q--) {
						if (currentContext.size() > aca.size() || !currentContext.equals(aca.subList(0, currentContext.size()))) {
							contexts.remove(currentContext);
							currentContext = new ArrayList<iLoggingEvent>(currentContext.subList(0, currentContext.size() - 1));
							currentParent = (LoggingEventModel) currentParent.getParents().get(0);
						} else {
							break;
						}
					}
					currentContext = new ArrayList<iLoggingEvent>(currentContext);
					while (!currentContext.equals(aca)) {
						iLoggingEvent added = aca.get(currentContext.size());
						Object tok = ((iSuspendedContext) added).getToken();
						if (tok instanceof iVisualElement)
							tok = SelectionSetDriver.nameFor((iVisualElement) tok);
						LoggingEventModel newParent = new LoggingEventModel(LoggingEventType.informative, null, "<HTML><font color='#" + Constants.defaultTreeColor + "'> inside \u2014 " + tok);
						currentParent.addChild(newParent);
						currentContext = new ArrayList<iLoggingEvent>(currentContext);
						currentContext.add(added);
						contexts.put(currentContext, newParent);
						currentParent = newParent;
					}
				}

				{
					f.getParents().get(0).removeChild(f);
				}

				currentParent.addChild(f);
			}

			{
				ArrayList<LoggingEventModel> a = new ArrayList<LoggingEventModel>(root.getChildren());
				for (LoggingEventModel m : a)
					root.removeChild(m);
			}
			{
				ArrayList<LoggingEventModel> a = new ArrayList<LoggingEventModel>(tmpRoot.getChildren());
				for (LoggingEventModel m : a) {
					tmpRoot.removeChild(m);
					root.addChild(m);
				}
			}
		}

		@Override
		public String toString() {
			return "By Visual Element";
		}

	}

	static public class SimpleStringEvent implements Logging.iLoggingEvent, iUndoable {
		private final Object target;

		private final Object value;

		private final String path;

		private final String forwardExpression;

		private final String backwardsExpression;

		private final boolean isError;

		public SimpleStringEvent(Object target, Object value, String path, String forwardExpression, String backwardsExpression, boolean isError) {
			this.target = target;
			this.value = value;
			this.path = path;
			this.forwardExpression = forwardExpression;
			this.backwardsExpression = backwardsExpression;
			this.isError = isError;
		}

		public void executeSimpleUndo() {
		}

		public String getDoExpression() {
			return forwardExpression;
		}

		public String getLongDescription() {
			return "on target <" + target + " / " + path + ">\n set value <" + value + ">\n fowards <" + forwardExpression + ">\n backwards <" + backwardsExpression + ">";
		}

		public String getReplayExpression() {
			return forwardExpression;
		}

		public String getTextDescription() {
			return "<html><font color='#" + Constants.defaultTreeColor + ">baked \u2014 " + forwardExpression;
		}

		public String getUndoExpression() {
			return backwardsExpression;
		}

		public boolean isError() {
			return isError;
		}

	}
}
