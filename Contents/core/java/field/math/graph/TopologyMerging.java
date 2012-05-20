package field.math.graph;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import field.bytecode.protect.DeferedInQueue.iProvidesQueue;
import field.bytecode.protect.DeferedInQueue.iRegistersUpdateable;
import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.InQueue;
import field.math.graph.GraphNodeSearching.GraphNodeVisitor_depthFirst;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.graph.TopologySearching.TopologyAStarSearch;
import field.math.graph.TopologySearching.TopologyVisitor_directedDepthFirst;
import field.util.TaskQueue;

public class TopologyMerging {

	static public abstract class CompressRun<T extends NodeImpl<T>> implements iProvidesQueue {
		private final List<T> all;

		TaskQueue queue = new TaskQueue();

		boolean biconnect = false;

		public CompressRun(List<T> all) {
			this.all = new ArrayList<T>(all);
		}

		public iRegistersUpdateable getQueueFor(Method m) {
			return queue;
		}

		public List<T> perform() {

			final Object ne = new Object() {
				@Override
				public boolean equals(Object obj) {
					return false;
				}
			};

			int hashcode = all.hashCode();
			int nextcode = hashcode + 1;
			List<T> q = all;
			while (nextcode != hashcode) {
				hashcode = q.hashCode();
				q = new GlossGraph<T>(q) {
					@Override
					protected Object catagoryFor(T tt) {
						if (isLineConnectedAndNotEnd(tt)) {
							return new Integer(0);
						} else
							return ne;
					}

					@Override
					protected T createNewNodeFor(Collection<T> domain, Collection<T> fringe) {
						System.err.println(" fringe size is <" + fringe.size() + "> domain size is <" + domain + ">");
						assert fringe.size() <= 2 : fringe + " " + domain;
						if (fringe.size() == 2) {

							for (T d : domain)
								assert d.getChildren().size() >= 1 && d.getChildren().size() <= 2 : d + " " + domain;
							Iterator<T> fi = fringe.iterator();
							T a = fi.next();
							T b = fi.next();

							Iterator<T> di = domain.iterator();
							T first, last;
							first = last = di.next();
							while (di.hasNext())
								last = di.next();

							if (first.getChildren().contains(b)) {
								assert last.getChildren().contains(a) : last + " " + last.getChildren() + " " + first + " " + first.getChildren() + " (" + a + " " + b + ") d"
								+ domain + " f" + fringe;
								T c = b;
								b = a;
								a = c;
							}

							return CompressRun.this.createNewNodeFor(domain, first, a, last, b);
						} else if (fringe.size() == 1) {
							for (T d : domain)
								assert d.getChildren().size() >= 1 && d.getChildren().size() <= 2 : d + " " + domain;
							Iterator<T> fi = fringe.iterator();
							T a = fi.next();

							Iterator<T> di = domain.iterator();
							T first, last;
							first = last = di.next();
							while (di.hasNext())
								last = di.next();

							return CompressRun.this.createNewNodeFor(domain, first, a, last, a);

						} else if (fringe.size() == 0) {
							System.err.println(" loop detected!? ");
							return null;
						}
						return null;
					}
				}.setBiconnect(biconnect).perform();
				nextcode = q.hashCode();
			}
			return q;

		}

		public CompressRun<T> setBiconnect(boolean biconnect) {
			this.biconnect = biconnect;
			return this;
		}

		abstract protected T createNewNodeFor(Collection<T> domain, T domainStart, T fringeStart, T domainEnd, T fringeEnd);

	}

	@Woven
	static public abstract class EliminateShortBranches<T extends NodeImpl<T>> implements iProvidesQueue {
		private final HashSet<T> all;

		TaskQueue queue = new TaskQueue();

		public EliminateShortBranches(List<T> all) {
			this.all = new LinkedHashSet<T>(all);
		}

		public List<T> getAll() {
			return new ArrayList<T>(all);
		}

		public iRegistersUpdateable getQueueFor(Method m) {

			return queue;
		}

		abstract public boolean mergeOnto(T head, List<T> tail);

		public List<T> perform() {

			GraphNodeVisitor_depthFirst<T> app = new GraphNodeSearching.GraphNodeVisitor_depthFirst<T>(true) {
				@Override
				protected VisitCode visit(T n) {

					if (isEnd(n, stack)) {
						// dead
						// end
						List<T> substack = new ArrayList<T>();
						substack.add(0, n);
						for (int i = stack.size() - 2; i >= 0; i--) {
							if (isLineConnected(stack.get(i))) {
								substack.add(0, stack.get(i));
							} else {
								substack.add(0, stack.get(i));
								break;
							}
						}
						if (shouldEliminate(substack)) {
							eliminate(substack);
						}
					}
					return VisitCode.cont;

				}

			}.setClearSeenOnExit(false);
			for (T a : all) {
				if (!app.hasSeen(a))
					app.apply(a);
				else
					;//System.out.println("skipped <" + a + ">");
			}
			queue.update();
			return getAll();

		}

		abstract public boolean shouldEliminate(List<T> branch);

		private boolean nothingConnectedTo(HashSet<T> all, NodeImpl n) {
			for (T t : all)
				assert !t.getChildren().contains(n);
			return true;
		}

		@InQueue
		protected void eliminate(List<T> stack) {
			for (T t : stack)
				if (!all.contains(t))
					return;
			if (stack.size() == 0)
				return;
			boolean b = mergeOnto(stack.get(0), stack.subList(1, stack.size()));
			if (b) {
				for (int i = 1; i < stack.size(); i++) {
					disconnectAll(stack.get(i));
					all.remove(stack.get(i));
				}
			}
		}
	}

	static public abstract class ExtractBorderOf<T> {

		private final iTopology<T> top;

		public ExtractBorderOf(iTopology<T> top) {
			this.top = top;
		}

		public List<T> extractBorder(List<T> all) {
			final HashSet<T> unseen = new HashSet<T>(all);

			for (int i = 0; i < all.size(); i++) {
				final List<T> ll = new ArrayList<T>();
				if (unseen.contains(all.get(i))) {
					if (isBorder(all.get(i))) {
						new TopologySearching.TopologyVisitory_depthFirst<T>(true, top) {
							@Override
							protected VisitCode visit(T n) {
								if (!unseen.contains(n))
									return VisitCode.skip;
								if (!isBorder(n))
									return VisitCode.skip;
								ll.add(n);
								unseen.remove(n);
								return VisitCode.cont;
							}
						}.apply(all.get(i));
					}
				}
				if (ll.size() > 0) {
					return ll;
				}
			}
			return null;
		}

		abstract protected boolean isBorder(T t);
	}

	/**
	 * expands out nodes that are the same
	 * "catagory" and replaces them with a single
	 * node, with the same connectivity
	 */
	static public abstract class GlossGraph<T extends NodeImpl<T>> implements iProvidesQueue {
		private final HashSet<T> all;

		TaskQueue queue = new TaskQueue();

		boolean biconnect = false;

		public GlossGraph(List<T> all) {
			this.all = new LinkedHashSet<T>(all);
		}

		public iRegistersUpdateable getQueueFor(Method m) {
			return queue;
		}

		public List<T> perform() {
			;//System.out.println(" glossing graph");
			boolean changed = true;
			while (changed) {
				changed = false;
				for (final T t : all) {
					final List<T> domain = new ArrayList<T>();
					final List<T> fringe = new ArrayList<T>();
					final Object catagory = catagoryFor(t);

					new GraphNodeSearching.GraphNodeVisitor_depthFirst<T>(true) {
						@Override
						protected VisitCode visit(T n) {
							if (!catagoryFor(n).equals(catagory)) {
								fringe.add(n);
								Collections.reverse(domain);
								return VisitCode.skip;
							}
							domain.add(n);
							return VisitCode.cont;
						}
					}.apply(t);
					if (domain.size() > 1) {
						T replaceWith = mergeNodes(domain, fringe);
						if (replaceWith != null) {
							changed = true;
							break;
						} else {
						}
					}
				}
			}
			return new ArrayList<T>(all);
		}

		public GlossGraph<T> setBiconnect(boolean biconnect) {
			this.biconnect = biconnect;
			return this;
		}

		protected abstract Object catagoryFor(T tt);

		abstract protected T createNewNodeFor(Collection<T> domain, Collection<T> fringe);

		protected T mergeNodes(Collection<T> domain, Collection<T> fringe) {
			T t = createNewNodeFor(domain, fringe);
			if (t != null) {
				for (T d : domain) {
					disconnectAll(d);
					all.remove(d);
				}
				for (T f : fringe) {
					t.addChild(f);
					if (biconnect)
						f.addChild(t);
				}
				all.add(t);
			}
			return t;
		}
	}

	static public class LineChaser<T> {
		private final iTopology<T> t;

		ArrayList<List<T>> lines = new ArrayList<List<T>>();

		public LineChaser(iTopology<T> t) {
			this.t = t;
		}

		public ArrayList<List<T>> chase(List<T> all) {
			final HashSet<T> a = new HashSet<T>(all);
			final HashSet<T> s = new HashSet<T>();

			while (a.size() > 0) {
				final T startAt = a.iterator().next();

				final List<T> building = new ArrayList<T>();
				building.add(startAt);

				TopologyVisitor_directedDepthFirst<T> depth = new TopologyVisitor_directedDepthFirst<T>(true, t) {
					@Override
					public int compare(T o1, T o2) {
						return 1;
					}

					@Override
					protected VisitCode visit(T n) {
						s.add(n);
						if (n == startAt)
							return VisitCode.cont;
						if (t.getChildrenOf(building.get(0)).contains(n))
							building.add(0, n);
						else if (t.getChildrenOf(building.get(building.size() - 1)).contains(n))
							building.add(n);
						else
						{
							return VisitCode.stop;
						}
						return VisitCode.cont;

					}
				};
				depth.seen.addAll(s);
				depth.apply(startAt);
				lines.add(building);
				a.removeAll(s);
				a.remove(startAt);
			}
			return lines;
		}
	}

	static public abstract class MakeSingleLoop<T extends NodeImpl<T>> {
		T at = null;

		List<T> soFar = new ArrayList<T>();

		public MakeSingleLoop() {
		}

		public List<T> next(T node) {
			if (at == null) {
				at = node;
				soFar.add(at);
				return Collections.singletonList(at);
			}
			// search
			// from
			// at
			// to
			// node;

			if (at == node)
				return soFar;

			List<T> path = shortestPath(at, node);

			Collections.reverse(path);

			// this
			// path
			// contains
			// both
			// the
			// start
			// and
			// the
			// end;
			path.remove(0);
			soFar.addAll(path);

			at = node;

			return soFar;
		}

		abstract protected float distance(T t, T root);


		protected List<T> shortestPath(T at, final T to) {
			return new TopologyAStarSearch<T>(new TopologyViewOfGraphNodes<T>(), new TopologySearching.AStarMetric<T>() {
				public double distance(T from, T to) {
					return Math.sqrt(MakeSingleLoop.this.distance(from, to));
				}
			}).search(at, to);
		}
	}

	static public abstract class MakeSingleLoop2<T extends NodeImpl<T>> {
		T at = null;

		List<T> soFar = new ArrayList<T>();

		public MakeSingleLoop2() {
		}

		public List<T> next(T node) {
			if (at == null) {
				at = node;
				soFar.add(at);
				return Collections.singletonList(at);
			}
			// search
			// from
			// at
			// to
			// node;

			if (at == node)
				return soFar;

			List<T> path = shortestPath(at, node, soFar);

			;//System.out.println(" shortest path from <" + at + "> to <" + node + "> is <" + path + ">");
			;//System.out.println(" shortest path from <" + at.getChildren() + "> to <" + node.getChildren() + "> is <" + path + ">");
			Collections.reverse(path);

			// this
			// path
			// contains
			// both
			// the
			// start
			// and
			// the
			// end;
			path.remove(0);
			soFar.addAll(path);

			at = node;

			return soFar;
		}

		abstract protected float distance(T t, T root);


		protected List<T> shortestPath(T at, final T to, List<T> soFar2) {
			return new TopologySearching.TopologyAStarSearch<T>(new TopologyViewOfGraphNodes<T>(), new TopologySearching.AStarMetric<T>() {
				public double distance(T from, T to) {
					return Math.sqrt(MakeSingleLoop2.this.distance(from, to));
				}
			}).preSee(soFar2).search(at, to);
		}
	}

	static public abstract class MakeSinglyConnected_biconnected<T extends NodeImpl<T>> {
		T r1;

		T r2;

		public void perform(List<T> all) {
			boolean changed = true;
			while (changed) {
				changed = false;

				// extract
				// domains
				List<List<T>> domains = extractDomainsBiConnected(all);
				Collections.sort(domains, new Comparator<List<T>>() {

					public int compare(List<T> o1, List<T> o2) {
						return o2.size() > o1.size() ? 1 : -1;
					}
				});

				for (int i = 0; i < domains.size(); i++) {
					;//System.out.println(" domain <" + i + "> is size <" + domains.get(i).size() + ">");
					List<T> from = domains.get(i);
					float d = Float.POSITIVE_INFINITY;
					List<T> to = null;
					T[] ato = null;

					for (int j = 0; j < domains.size(); j++) {
						if (j != i) {
							float dd = distance(from, domains.get(j));
							if (dd < d) {
								d = dd;
								to = domains.get(j);
								ato = returnConnected(from, domains.get(j));
							}
						}
					}
					if (ato != null) {
						ato[0].addChild(ato[1]);
						ato[1].addChild(ato[0]);
						;//System.out.println(" connected <" + ato[0] + "> to <" + ato[1] + ">");
					}
				}

				changed = domains.size() > 1;
			}
		}

		protected float distance(List<T> from, List<T> name) {
			float m = Float.POSITIVE_INFINITY;
			for (int i = 0; i < from.size(); i++) {
				for (int j = 0; j < name.size(); j++) {
					float d = distance(from.get(i), name.get(j));
					if (d < m) {
						m = d;
						r1 = from.get(i);
						r2 = name.get(j);
					}
				}
			}
			return m;
		}

		abstract protected float distance(T t, T t2);

		protected T[] returnConnected(List<T> from, List<T> name) {
			return (T[]) new NodeImpl[] { r1, r2 };
		}

	}

	static public abstract class MakeSinglyConnected_biconnectedTopology<T> {

		private final iTopology<T> topology;

		T r1;

		T r2;

		public MakeSinglyConnected_biconnectedTopology(iTopology<T> topology) {
			this.topology = topology;
		}

		public void perform(List<T> all) {
			boolean changed = true;
			while (changed) {
				changed = false;

				// extract
				// domains
				List<List<T>> domains = extractDomainsBiConnected(all, topology);
				Collections.sort(domains, new Comparator<List<T>>() {

					public int compare(List<T> o1, List<T> o2) {
						return o2.size() > o1.size() ? 1 : -1;
					}
				});

				for (int i = 0; i < domains.size(); i++) {
					;//System.out.println(" domain <" + i + "> is size <" + domains.get(i).size() + ">");
					List<T> from = domains.get(i);
					float d = Float.POSITIVE_INFINITY;
					List<T> to = null;
					Object[] ato = null;

					for (int j = 0; j < domains.size(); j++) {
						if (j != i) {
							float dd = distance(from, domains.get(j));
							if (dd < d) {
								d = dd;
								to = domains.get(j);
								ato = returnConnected(from, domains.get(j));
							}
						}
					}

					if (ato != null) {
						connect((T) ato[0], (T) ato[1]);
						;//System.out.println(" connected <" + ato[0] + "> to <" + ato[1] + ">");
					}
				}

				changed = domains.size() > 1;
			}
		}

		abstract protected void connect(T t, T t2);

		protected float distance(List<T> from, List<T> name) {
			float m = Float.POSITIVE_INFINITY;
			for (int i = 0; i < from.size(); i++) {
				for (int j = 0; j < name.size(); j++) {
					float d = distance(from.get(i), name.get(j));
					if (d < m) {
						m = d;
						r1 = from.get(i);
						r2 = name.get(j);
					}
				}
			}
			return m;
		}

		abstract protected float distance(T t, T t2);

		protected Object[] returnConnected(List<T> from, List<T> name) {
			return new Object[] { r1, r2 };
		}

	}

	static public void disconnectAll(NodeImpl n) {
		for (NodeImpl c : new ArrayList<NodeImpl>(n.getChildren())) {
			n.removeChild(c);
			c.removeChild(n);
		}
		for (NodeImpl c : new ArrayList<NodeImpl>(n.getParents())) {
			c.removeChild(n);
			n.removeChild(c);
		}

		// assert
		// nothingConnectedTo(all,
		// n);
	}

	// only works on biconnected graph
	static public <T extends NodeImpl<T>> List<List<T>> extractDomainsBiConnected(List<T> all) {
		List<List<T>> ret = new ArrayList<List<T>>();
		final List[] gathering = { null };

		int n = 0;

		assert isBiconnected(all);

		GraphNodeVisitor_depthFirst<T> gather = new GraphNodeSearching.GraphNodeVisitor_depthFirst<T>(true) {
			@Override
			protected VisitCode visit(T n) {
				gathering[0].add(n);
				return VisitCode.cont;
			}
		}.setClearSeenOnExit(false);

		for (T t : all) {
			if (!gather.hasSeen(t)) {
				gathering[0] = new ArrayList<T>();
				gather.apply(t);
				ret.add(gathering[0]);
				n += gathering[0].size();
				for (T g : (List<T>) gathering[0]) {
					assert gather.hasSeen(g);
				}
			}
		}

		assert n == all.size() : n + " " + all.size();

		return ret;
	}

	static public <T> List<List<T>> extractDomainsBiConnected(List<T> all, iTopology<T> top) {
		List<List<T>> ret = new ArrayList<List<T>>();
		final List[] gathering = { null };

		int n = 0;

		TopologySearching.TopologyVisitory_depthFirst<T> gather = new TopologySearching.TopologyVisitory_depthFirst<T>(true, top) {
			@Override
			protected VisitCode visit(T n) {
				gathering[0].add(n);
				return VisitCode.cont;
			}
		}.setSetCleanOnExit(false);

		for (T t : all) {
			if (!gather.hasSeen(t)) {
				gathering[0] = new ArrayList<T>();
				gather.apply(t);
				ret.add(gathering[0]);
				n += gathering[0].size();
				for (T g : (List<T>) gathering[0]) {
					assert gather.hasSeen(g);
				}
			}
		}

		assert n == all.size() : n + " " + all.size();

		return ret;
	}

	static public <T extends NodeImpl<T>> boolean isBiconnected(List<T> all) {
		for (T t : all) {
			List<T> c = t.getChildren();
			for (T child : c) {
				assert child.getChildren().contains(t);
			}
		}
		return true;
	}

	static public boolean isEnd(NodeImpl n, List<? extends NodeImpl> stack) {
		return n.getChildren().size() == 0 || (n.getChildren().size() == 1 && stack.size() > 2 && n.getChildren().get(0) == stack.get(stack.size() - 2));
	}

	static public boolean isLineConnected(NodeImpl t) {
		// either t is
		// just
		// connected to
		// one child,
		if (t.getChildren().size() == 1)
			return true;
		// or it's
		// connected to
		// two, both of
		// which are
		// connected
		// back to this
		// one
		if (t.getChildren().size() != 2)
			return false;
		return ((NodeImpl) t.getChildren().get(0)).getChildren().contains(t) || ((NodeImpl) t.getChildren().get(1)).getChildren().contains(t);
	}

	static public boolean isLineConnectedAndNotEnd(NodeImpl t) {
		// either t is
		// just
		// connected to
		// one child,
		if (t.getChildren().size() == 1) {
			if (((NodeImpl) t.getChildren().get(0)).getChildren().contains(t))
				return false;
			else
				return true;
		}
		// or it's
		// connected to
		// two, both of
		// which are
		// connected
		// back to this
		// one
		if (t.getChildren().size() != 2)
			return false;
		return ((NodeImpl) t.getChildren().get(0)).getChildren().contains(t) || ((NodeImpl) t.getChildren().get(1)).getChildren().contains(t);
	}

}
