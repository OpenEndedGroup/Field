package field.math.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

public class GraphNodeSearching {

	static public class VisitCode {
		static public final VisitCode cont = new VisitCode("cont");

		static public final VisitCode stop = new VisitCode("stop");

		static public final VisitCode skip = new VisitCode("skip");

		private final String string;

		public VisitCode(String string) {
			this.string = string;
		}
		
		@Override
		public String toString() {
			return string;
		}
	}

	static public class SkipMultiple extends VisitCode {
		public Collection c;

		public SkipMultiple(Collection c) {
			super("skip_multiple");
			this.c = c;
		}
	}

	static public class SkipMultipleBut extends VisitCode {
		public Collection c;

		public Object o;

		public SkipMultipleBut(Collection c, Object o) {
			super("skip_multiple_but");
			this.c = c;
			this.o = o;
		}
	}

	static public abstract class GraphNodeVisitor_depthFirst<T extends iGraphNode> {
		private boolean avoidLoops;

		protected HashSet<T> seen = new HashSet<T>();

		protected Stack<T> stack = new Stack<T>();

		private boolean reverse = false;

		public GraphNodeVisitor_depthFirst(boolean avoidLoops) {
			this.avoidLoops = avoidLoops;
		}

		public GraphNodeVisitor_depthFirst(boolean avoidLoops, boolean reverse) {
			this.avoidLoops = avoidLoops;
			this.reverse = reverse;
		}

		boolean clearSeenOnExit = true;

		int maxDepth = 3000;

		public GraphNodeVisitor_depthFirst<T> setClearSeenOnExit(boolean clearSeenOnExit) {
			this.clearSeenOnExit = clearSeenOnExit;
			return this;
		}

		public void apply(T root) {
			depth = 0;
			enter(root);
			stack.push(root);
			seen.add(root);
			
			VisitCode code = visit(root);
			if (code == VisitCode.stop) return;
			if (code == VisitCode.skip) {
				stack.pop();
				exit(root);
				return;
			}
			if (code instanceof SkipMultiple) {
				seen.addAll(((SkipMultiple) code).c);
				avoidLoops = true;
			}
			if (code instanceof SkipMultipleBut) {
				seen.addAll(((SkipMultipleBut) code).c);
				seen.remove(((SkipMultipleBut) code).o);
				avoidLoops = true;
			}
			List<T> c = root.getChildren();
			_apply(c);
			stack.pop();
			if (clearSeenOnExit) seen.clear();
			exit(root);
		}

		public boolean hasSeen(T a) {
			return seen.contains(a);
		}

		protected void enter(T root) {
		}

		protected void exit(T root) {
		}

		int depth = 0;

		protected VisitCode _apply(List<T> c) {
			depth++;
			if (depth > maxDepth*2) {
				;//System.out.println(" (( warning, max depth exceeded in search ))");
				return VisitCode.stop;
			}

			ListIterator<T> li = c.listIterator(reverse ? c.size() : 0);
			// for (T n : c)
			while (reverse ? li.hasPrevious() : li.hasNext()) {
				T n = reverse ? li.previous() : li.next();
				if (!avoidLoops || !seen.contains(n)) {
					if (avoidLoops) seen.add(n);
					enter(n);
					stack.push(n);
					VisitCode code = visit(n);
					if (code == VisitCode.stop) return VisitCode.stop;
					if (code instanceof SkipMultiple) {
						seen.addAll(((SkipMultiple) code).c);
						avoidLoops = true;
					} else if (code instanceof SkipMultipleBut) {
						seen.addAll(((SkipMultipleBut) code).c);
						seen.remove(((SkipMultipleBut) code).o);
						avoidLoops = true;
					}
					if (code != VisitCode.skip) {
						VisitCode vc = _apply(n.getChildren());
						if (vc == VisitCode.stop) return VisitCode.stop;
					}
					stack.pop();
					exit(n);
				}
			}
			depth--;
			return VisitCode.cont;
		}

		abstract protected VisitCode visit(T n);

		protected String spaces(int n) {
			StringBuffer buf = new StringBuffer(n);
			for (int i = 0; i < n; i++)
				buf.append(' ');
			return buf.toString();
		}
	}

	static public SimpleNode<String> makeRandomTree(int numNodes, final float factor) {
		SimpleNode<String> root = new SimpleNode<String>().setPayload("root");
		ArrayList<SimpleNode<String>> nodes = new ArrayList<SimpleNode<String>>();
		nodes.add(root);
		final SimpleNode<String>[] ref = new SimpleNode[1];
		class RandomSearch extends GraphNodeVisitor_depthFirst<SimpleNode<String>> {
			public RandomSearch() {
				super(false);
			}

			protected VisitCode visit(SimpleNode<String> n) {
				if (Math.random() < factor) {
					ref[0] = n;
					return VisitCode.stop;
				}
				return VisitCode.cont;
			}
		}
		RandomSearch rs = new RandomSearch();
		int c = 0;
		while (nodes.size() < numNodes) {
			rs.apply(root);
			if (ref[0] != null) {
				SimpleNode<String> nn = new SimpleNode<String>().setPayload("new node <" + (c++) + "> =" + c + "> = somethingelse");
				ref[0].addChild(nn);
				nodes.add(nn);
			}
		}
		return root;
	}

	static public <T extends iGraphNode<T>> List<T> findPath_avoidingLoops(T from, final T to) {
		final ArrayList<T> path = new ArrayList<T>();
		if (from == to) {
			path.add(from);
			return path;
		}
		new GraphNodeVisitor_depthFirst<T>(true){
			@Override
			protected VisitCode visit(T n) {
				VisitCode vc = n == to ? VisitCode.stop : VisitCode.cont;
				if (vc == VisitCode.stop) path.addAll(stack);
				return vc;
			}
		}.apply(from);
		if (path.size() == 0) return null;
		return path.size() > 0 ? path : null;
	}

}
