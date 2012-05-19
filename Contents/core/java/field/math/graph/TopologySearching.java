package field.math.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;

import field.math.BaseMath;
import field.math.BaseMath.MutableFloat;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.namespace.generic.Generics.Pair;
import field.util.HashMapOfLists;

public class TopologySearching {

	public interface AStarMetric<T> {

		public double distance(T from, T to);
	}

	static public abstract class PathCache<T> {
		public class KnownPath {
			List<T> path;

			float distance;
		}

		HashMap<Pair<T, T>, KnownPath> cache = new HashMap<Pair<T, T>, KnownPath>();

		Pair<T, T> tmp = new Pair<T, T>(null, null);

		public void declarePath(T from, T to, List<T> isPath, float totalDistance) {
			if (isPath.size() == 0) {
				isPath = new ArrayList<T>();
				isPath.add(from);
				isPath.add(to);
			} else if (isPath.get(0) != from) {
				isPath = new ArrayList<T>(isPath);
				isPath.add(0, from);
			}
			if (isPath.get(isPath.size() - 1) != to) {
				isPath = new ArrayList<T>(isPath);
				isPath.add(to);
			}

			float d = totalDistance;
			for (int i = 0; i < isPath.size() - 1; i++) {
				from = isPath.get(i);
				Pair<T, T> q = new Pair<T, T>(from, to);
				KnownPath qd = cache.get(q);
				if (qd == null || qd.distance > d) {
					KnownPath kp = new KnownPath();
					kp.path = isPath.subList(i, isPath.size());
					kp.distance = d;
					cache.put(q, kp);
				}
				d -= distance(isPath.get(i), isPath.get(i + 1));
			}

		}

		public KnownPath getPath(T from, T to) {
			tmp.left = from;
			tmp.right = to;
			KnownPath ll = cache.get(tmp);
			return ll;
		}

		abstract protected float distance(T t, T t2);

	}

	static public class TopologyAStarSearch<T> {
		private final iTopology<T> topology;

		private final AStarMetric<T> metric;

		TreeElement<T> treeRoot;

		HashMap<T, TreeElement<T>> treeElements;

		T goalNode;

		HashSet<T> avoid = new HashSet<T>();

		public TopologyAStarSearch(iTopology<T> topology, AStarMetric<T> metric) {
			this.topology = topology;
			this.metric = metric;
		}

		public double calcF(T N, AStarMetric<T> metric) {
			double F = calcG(N) + calcH(N);
			return F;
		}

		public double calcG(T N) {
			double dist = 0;
			TreeElement<T> at = treeElements.get(N);
			while (at.parent != null) {
				dist += metric.distance(at.parent.pointer, at.pointer);
				at = at.parent;
			}
			return dist;
		}

		public double calcH(T N) {
			double H = metric.distance(N, goalNode);
			return H;
		}

		public double calcK(T N, T Nprime) {
			return metric.distance(N, Nprime);
		}

		public TopologyAStarSearch<T> preSee(Collection<T> t) {
			avoid.addAll(t);
			return this;
		}

		public List<T> search(T from, T to) {

			avoid.remove(to);

			if (from.equals(to)) {
				if (!topology.getChildrenOf(from).contains(from))
					return searchNotIncluding(from, to);
			}
			goalNode = to;

			HashSet<T> visited = new HashSet<T>();

			TreeMap<MutableFloat, T> open = new TreeMap<MutableFloat, T>(new Comparator<MutableFloat>() {
				public int compare(MutableFloat o1, MutableFloat o2) {
					if (o1 == o2)
						return 0;
					int c = Float.compare(o1.floatValue(), o2.floatValue());
					return c == 0 ? (System.identityHashCode(o1) < System.identityHashCode(o2) ? -1 : 1) : c;
				}
			});
			HashMap<T, MutableFloat> openBackwards = new HashMap<T, MutableFloat>();

			treeRoot = new TreeElement<T>(from, null);
			treeElements = new HashMap<T, TreeElement<T>>();
			treeElements.put(from, treeRoot);

			double f = calcF(from, metric);
			BaseMath.MutableFloat F = new BaseMath.MutableFloat((float) f);
			open.put(F, from);
			openBackwards.put(from, F);

			visited.add(from);
			T N = null;
			while (open.size() != 0) {
				N = open.get(open.firstKey());
				openBackwards.remove(open.remove(open.firstKey())); // hmm...

				if (N.equals(to)) {
					break;
				}

				for (T Nprime : topology.getChildrenOf(N)) {
					if (!avoid.contains(Nprime)) {

						if (!visited.contains(Nprime)) {
							treeElements.put(Nprime, new TreeElement<T>(Nprime, treeElements.get(N)));

							int oldLength = open.size();

							f = calcF(Nprime, metric);
							BaseMath.MutableFloat FNprime = new BaseMath.MutableFloat((float) f);
							Object o = open.put(FNprime, Nprime);
							openBackwards.put(Nprime, FNprime);

							int newLength = open.size();

							assert newLength > oldLength;

							visited.add(Nprime);
						} else {
							double gOfNprime = calcG(Nprime);
							double gOfN = calcG(N);
							double kOfNNprime = calcK(N, Nprime);

							if (gOfNprime > (gOfN + kOfNNprime)) {
								TreeElement<T> te = treeElements.get(Nprime);
								te.parent = treeElements.get(N);

								if (openBackwards.containsKey(Nprime)) {
									Object value = openBackwards.remove(Nprime);
									open.remove(value);
								}
								double fNprime = calcF(Nprime, metric);
								BaseMath.MutableFloat FNprime = new BaseMath.MutableFloat((float) fNprime);
								open.put(FNprime, Nprime);
								openBackwards.put(Nprime, FNprime);
							}
						}
					}
				}
			}

			if (N.equals(to)) // change
			// from
			// document
			{
				// construct
				// path
				Vector<T> path = new Vector<T>();
				path.add(N);
				TreeElement<T> at = treeElements.get(to);
				while (at.pointer != from) {
					at = at.parent;
					path.add(at.pointer);
				}
				return path;
			}
			return null;
		}

		public float sumDistance(List<T> r) {
			float total = 0;
			T last = null;
			for (T t : r) {
				if (last != null)
					total += metric.distance(last, t);
				last = t;
			}
			return total;
		}

		/**
		 * @param from
		 * @param metric
		 * @param to
		 * @return
		 */
		private List<T> searchNotIncluding(T from, T to) {
			List<T> best = null;
			float bestDistance = Float.POSITIVE_INFINITY;

			for (T newFrom : topology.getChildrenOf(from)) {
				List<T> r = search(newFrom, to);
				if (r != null) {
					float d = sumDistance(r);
					if (d < bestDistance) {
						bestDistance = d;
						best = r;
					}
				}
			}
			if (best != null) {
				best.add(from);
			}
			return best;
		}

	}

	static public abstract class TopologyVisitor_breadthFirst<T> {
		private final boolean avoidLoops;

		HashSet<T> seen = new HashSet<T>();

		LinkedHashSet<T> fringe = new LinkedHashSet<T>();

		LinkedHashSet<T> fringe2 = new LinkedHashSet<T>();

		public TopologyVisitor_breadthFirst(boolean avoidLoops) {
			this.avoidLoops = avoidLoops;
		}

		public void apply(iTopology<T> top, T root) {
			seen.clear();
			fringe.clear();
			_apply(top, root, fringe, fringe2);
		}

		public void preSee(Collection<T> seen2) {
			seen.addAll(seen2);
		}

		private void _apply(iTopology<T> top, T root, LinkedHashSet<T> localFringe, LinkedHashSet<T> tempFringe) {
			VisitCode code = visit(root);
			if (code == VisitCode.stop)
				return;
			if (code == VisitCode.skip) {
				return;
			}

			List<T> c = top.getChildrenOf(root);
			fringe.addAll(c);

			visitFringe(fringe);
			while (fringe.size() > 0) {
				for (T t : maybeWrap(fringe)) {
					if (!avoidLoops || !seen.contains(t)) {
						VisitCode vc = visit(t);
						if (vc == VisitCode.stop)
							return;
						if (vc == VisitCode.skip) {
						} else {
							List<T> childrenOf = top.getChildrenOf(t);
							fringe2.addAll(childrenOf);
						}
						if (avoidLoops)
							seen.add(t);
					}
				}
				LinkedHashSet<T> t = fringe;
				fringe = fringe2;

				visitFringe(fringe);

				fringe2 = t;
				fringe2.clear();
			}
		}

		protected LinkedHashSet<T> maybeWrap(LinkedHashSet<T> f) {
			return f;
		}

		abstract protected VisitCode visit(T root);

		protected void visitFringe(Collection<T> fringe) {
		}
	}

	static public abstract class TopologyVisitor_cachingTreeBreadthFirst<T> {

		public HashMapOfLists<T, Pair<T, List<T>>> knownPaths = new HashMapOfLists<T, Pair<T, List<T>>>();

		private final iTopology<T> t;

		HashMap<T, T> parented = new HashMap<T, T>();

		int maxDepth = -1;

		HashSet<T> currentFringe = new LinkedHashSet<T>();

		HashSet<T> nextFringe = new LinkedHashSet<T>();

		public TopologyVisitor_cachingTreeBreadthFirst(iTopology<T> t) {
			this.t = t;
		}

		public void apply(Collection<T> root) {
			currentFringe.clear();
			nextFringe.clear();
			parented.clear();

			for (T t : root)
				parented.put(t, null);

			for (T x : root) {
				List<T> children = t.getChildrenOf(x);
				currentFringe.clear();
				currentFringe.addAll(children);
				for (T c : currentFringe) {
					parented.put(c, x);
				}
			}

			_apply(null);
		}

		public void apply(T root) {
			currentFringe.clear();
			nextFringe.clear();
			parented.clear();

			parented.put(root, null);

			List<T> children = t.getChildrenOf(root);
			currentFringe.clear();
			currentFringe.addAll(children);
			for (T c : currentFringe) {
				parented.put(c, root);
			}
			_apply(root);

		}

		public void copyCache(TopologyVisitor_cachingTreeBreadthFirst<T> ls2) {
			ls2.knownPaths.putAll(knownPaths);
		}

		public List<T> getPath(T to) {
			List<T> r = new ArrayList<T>();
			r.add(to);
			T p = parented.get(to);
			while (p != null && r.size() < 20) {
				r.add(p);
				T op = parented.get(p);
				if (p == op)
					break;
				p = op;
			}
			if (r.size() == 20) {
				System.err.println(" warning, self parenting path ? ");
			}
			return r;
		}

		public TopologyVisitor_cachingTreeBreadthFirst<T> setMaxDepth(int maxDepth) {
			this.maxDepth = maxDepth;
			return this;
		}

		private void _apply(T root) {

			int m = 0;
			do {
				m++;
				nextFringe.clear();

				for (T c : currentFringe) {
					synchronized (this.getClass()) {
						VisitCode code = visit(c);

						if (code == VisitCode.stop) {
							return;
						}
						if (code == VisitCode.skip) {
						} else {
							List<T> l = t.getChildrenOf(c);
							for (T cc : l) {
								if (!parented.containsKey(cc)) {
									parented.put(cc, c);
									nextFringe.add(cc);
								}
							}
						}
					}
				}

				HashSet<T> tmp = currentFringe;
				currentFringe = nextFringe;
				nextFringe = tmp;

				visitFringe(nextFringe);

			} while (currentFringe.size() > 0 && (maxDepth == -1 || m < maxDepth));
		}

		protected List<T> getCachedPath(T from, T to) {
			Collection<Pair<T, List<T>>> q = knownPaths.get(from);
			if (q == null)
				return null;
			for (Pair<T, List<T>> p : q) {
				if (p.left.equals(to)) {
					return p.right;
				}
			}
			return null;
		}

		protected void markPathAsCached(List<T> path) {
			for (int i = 0; i < path.size() - 1; i++) {
				knownPaths.addToList(path.get(i), new Pair<T, List<T>>(path.get(path.size() - 1), path.subList(i + 1, path.size())));
			}
		}

		abstract protected VisitCode visit(T c);

		protected void visitFringe(HashSet<T> nextFringe) {
		}

	}

	static public abstract class TopologyVisitor_directedBreadthFirst<T> implements Comparator<T> {
		private final boolean avoidLoops;

		HashSet<T> seen = new HashSet<T>();

		LinkedHashSet<T> fringe = new LinkedHashSet<T>();

		LinkedHashSet<T> fringe2 = new LinkedHashSet<T>();

		public TopologyVisitor_directedBreadthFirst(boolean avoidLoops) {
			this.avoidLoops = avoidLoops;
		}

		public void apply(iTopology<T> top, T root) {
			seen.clear();
			_apply(top, root, fringe, fringe2);
		}

		abstract public int compare(T o1, T o2);

		public void preSee(Collection<T> a) {
			seen.addAll(a);
		};

		private void _apply(iTopology<T> top, T root, LinkedHashSet<T> localFringe, LinkedHashSet<T> tempFringe) {
			VisitCode code = visit(root);
			if (code == VisitCode.stop)
				return;
			if (code == VisitCode.skip) {
				return;
			}

			List<T> c = top.getChildrenOf(root);
			fringe.addAll(c);

			epoch();

			while (fringe.size() > 0) {
				for (T t : fringe) {
					if (!avoidLoops || !seen.contains(t)) {
						VisitCode vc = visit(t);
						if (vc == VisitCode.stop)
							return;
						if (vc == VisitCode.skip) {
						} else {
							List<T> childrenOf = top.getChildrenOf(t);
							fringe2.addAll(childrenOf);
						}
						if (avoidLoops)
							seen.add(t);
					}
				}
				LinkedHashSet<T> t = fringe;
				fringe = fringe2;
				fringe2 = t;
				fringe2.clear();

				ArrayList<T> aa = new ArrayList<T>(fringe);
				sort(aa);
				fringe.clear();
				fringe.addAll(aa);

				epoch();
			}
		}

		protected void epoch() {
		}

		protected void sort(ArrayList<T> aa) {
			Collections.sort(aa, this);
		}

		abstract protected VisitCode visit(T root);
	}

	static public abstract class TopologyVisitor_directedDepthFirst<T> implements Comparator<T> {
		public HashSet<T> seen = new HashSet<T>();

		private final boolean avoidLoops;

		private final iTopology<T> topology;

		protected Stack<T> stack = new Stack<T>();

		public TopologyVisitor_directedDepthFirst(boolean avoidLoops, iTopology<T> topology) {
			this.avoidLoops = avoidLoops;
			this.topology = topology;
		}

		public void apply(T root) {
			stack.push(root);
			VisitCode code = visit(root);
			if (code == VisitCode.stop)
				return;
			if (code == VisitCode.skip) {
				stack.pop();
				return;
			}
			List<T> c = topology.getChildrenOf(root);
			_apply(c);
			stack.pop();
			seen.clear();
		}

		abstract public int compare(T o1, T o2);

		public void preSee(Set<T> s) {
			seen.addAll(s);
		}

		protected VisitCode _apply(List<T> c) {
			ArrayList<T> cs = new ArrayList<T>(c);
			Collections.sort(cs, this);
			for (T n : cs) {
				if (!avoidLoops || !seen.contains(n)) {
					if (avoidLoops)
						seen.add(n);
					stack.push(n);
					VisitCode code = visit(n);
					if (code == VisitCode.stop)
						return VisitCode.stop;
					if (code != VisitCode.skip) {
						VisitCode vc = _apply(topology.getChildrenOf(n));
						if (vc == VisitCode.stop)
							return VisitCode.stop;
					}
					exit(stack.pop());
				}
			}
			return VisitCode.cont;
		}

		protected void exit(T t) {
		}

		protected String spaces(int n) {
			StringBuffer buf = new StringBuffer(n);
			for (int i = 0; i < n; i++)
				buf.append(' ');
			return buf.toString();
		}

		abstract protected VisitCode visit(T n);
	}

	static public abstract class TopologyVisitor_longDirectedBreadthFirst<T> {

		public class Key implements Comparable<Key> {
			public List<T> path;

			public float accumulatedDistance;

			public int compareTo(Key o) {
				return -Float.compare(accumulatedDistance, o.accumulatedDistance);
			}

			@Override
			public String toString() {
				return "<<" + path + "> = " + accumulatedDistance + ">";
			}

		}

		protected PathCache<T>.KnownPath doShortPath = null;

		HashMap<T, Float> seen = new HashMap<T, Float>();

		List<Key> fringe = new ArrayList<Key>();

		int maxPop = Integer.MAX_VALUE;

		float maxDistance = Float.POSITIVE_INFINITY;

		public TopologyVisitor_longDirectedBreadthFirst<T> setMaxDistance(float maxDistance) {
			this.maxDistance = maxDistance;
			return this;
		}

		protected iTopology<T> top;

		public TopologyVisitor_longDirectedBreadthFirst() {
		}

		public void apply(iTopology<T> top, List<T> name) {
			seen.clear();
			this.top = top;
			_apply(top, name);
		}

		public void apply(iTopology<T> top, T name) {
			seen.clear();
			this.top = top;
			_apply(top, Collections.singletonList(name));
		}

		public void clear() {
			seen.clear();
			fringe.clear();
			doShortPath = null;
		}

		public TopologyVisitor_longDirectedBreadthFirst<T> setMaxPop(int maxPop) {
			this.maxPop = maxPop;
			return this;
		}

		private void _apply(iTopology<T> top, List<T> lroot) {
			VisitCode code;
			fringe.clear();
			for (T root : lroot) {
				Key rootK = new Key();
				rootK.path = new ArrayList<T>(1);
				rootK.path.add(root);
				rootK.accumulatedDistance = 0;
				seen.put(root, 0f);
				code = visit(rootK);

				if (code == VisitCode.stop)
					return;
				if (code == VisitCode.skip) {
				} else
					fringe.add(rootK);
			}
			while (fringe.size() > 0) {
				Key k = fringe.remove(fringe.size() - 1);

				T r = k.path.get(k.path.size() - 1);

				code = visit(k);
				if (code == VisitCode.stop)
					return;
				if (code == VisitCode.skip) {
					if (doShortPath != null) {
						float ad = doShortPath.distance;
						T end = doShortPath.path.get(doShortPath.path.size() - 1);
						Float q = seen.get(end);
						if (q == null || ad + k.accumulatedDistance < q) {
							Key k2 = new Key();
							k2.path = new ArrayList<T>(k.path.size() + doShortPath.path.size());
							k2.path.addAll(k.path);
							k2.path.addAll(doShortPath.path.subList(1, doShortPath.path.size()));
							k2.accumulatedDistance = ad + k.accumulatedDistance;
							seen.put(end, ad + k.accumulatedDistance);
							assert doShortPath.path.get(0) == r;
							if (k2.accumulatedDistance < maxDistance)
								insert(fringe, k2);
						}
					}
					doShortPath = null;
					continue;
				}
				List<T> c = top.getChildrenOf(r);

				for (T t : c) {
					float d = distance(r, t);
					Float q = seen.get(t);
					if (q == null || d + k.accumulatedDistance < q) {
						Key k2 = new Key();
						k2.path = new ArrayList<T>(k.path.size() + 1);
						k2.path.addAll(k.path);
						k2.path.add(t);
						k2.accumulatedDistance = d + k.accumulatedDistance;
						seen.put(t, d + k.accumulatedDistance);
						if (k2.accumulatedDistance < maxDistance)
							insert(fringe, k2);
					}
				}
				if (fringe.size() > maxPop) {
					fringe = new ArrayList<Key>(fringe.subList(0, maxPop));
				}

			}
		}

		private void insert(List<Key> in, Key k) {
			int q = Collections.binarySearch(in, k);
			if (q < 0) {
				in.add(-q - 1, k);
			} else
				in.add(q, k);
			assert invarients();
		}

		private boolean invarients() {
			for (int i = 1; i < fringe.size(); i++) {
				if (fringe.get(i).accumulatedDistance > fringe.get(i - 1).accumulatedDistance) {
					throw new Error("" + fringe);
				}
			}
			return true;
		}

		abstract protected float distance(T root, T t);

		abstract protected VisitCode visit(Key k);
	}

	static public abstract class TopologyVisitor_treeBreadthFirst<T> {
		protected final iTopology<T> t;

		protected HashMap<T, T> parented = new HashMap<T, T>();

		int maxDepth = -1;

		HashSet<T> currentFringe = new LinkedHashSet<T>();

		HashSet<T> nextFringe = new LinkedHashSet<T>();

		boolean all = false;

		public TopologyVisitor_treeBreadthFirst(iTopology<T> t) {
			this.t = t;
		}

		public void apply(Collection<T> root) {
			for (T t : root)
				parented.put(t, null);

			for (T x : root) {
				List<T> children = t.getChildrenOf(x);
				currentFringe.clear();
				currentFringe.addAll(children);
				for (T c : currentFringe) {
					parented.put(c, x);
				}
			}

			_apply(null);
		}

		public void apply(T root) {
			VisitCode r = visit(root);
			if (r == VisitCode.cont) {
				parented.put(root, null);

				List<T> children = t.getChildrenOf(root);
				currentFringe.clear();
				currentFringe.addAll(children);
				for (T c : currentFringe) {
					parented.put(c, root);
				}
				_apply(root);
			}
		}

		public List<T> getPath(T to) {
			List<T> r = new ArrayList<T>();
			r.add(to);
			T p = parented.get(to);
			while (p != null) {
				r.add(p);
				T np = parented.get(p);
				if (np == p) {
					System.err.println(" warning: self parenting path ? ");
					break;
				}
				p = np;
			}
			// if (r.size() == 20) {
			// System.err.println(" warning,
			// self parenting path ? ");
			// }
			return r;
		}

		public TopologyVisitor_treeBreadthFirst<T> setMaxDepth(int maxDepth) {
			this.maxDepth = maxDepth;
			return this;
		}

		private void _apply(T root) {

			int m = 0;
			do {
				m++;
				nextFringe.clear();

				for (T c : currentFringe) {

					VisitCode code = visit(c);

					if (code == VisitCode.stop)
						return;
					if (code == VisitCode.skip) {
					} else {
						List<T> l = t.getChildrenOf(c);
						for (T cc : l) {
							if (!parented.containsKey(cc) || all) {
								parented.put(cc, c);
								nextFringe.add(cc);
							}
						}
					}
				}

				HashSet<T> tmp = currentFringe;
				currentFringe = nextFringe;
				nextFringe = tmp;

				visitFringe(currentFringe);

			} while (currentFringe.size() > 0 && (maxDepth == -1 || m < maxDepth));
			System.out.println(" exhaused fringe ");
		}

		abstract protected VisitCode visit(T c);

		protected void visitFringe(HashSet<T> nextFringe) {
		}
	}

	static public abstract class TopologyVisitory_depthFirst<T> {
		private final boolean avoidLoops;

		private final iTopology<T> topology;

		protected HashSet<T> seen = new HashSet<T>();

		protected Stack<T> stack = new Stack<T>();

		boolean setCleanOnExit = true;

		public TopologyVisitory_depthFirst(boolean avoidLoops, iTopology<T> topology) {
			this.avoidLoops = avoidLoops;
			this.topology = topology;
		}

		public void apply(T root) {
			stack.push(root);
			VisitCode code = visit(root);
			if (code == VisitCode.stop)
				return;
			if (code == VisitCode.skip) {
				stack.pop();
				return;
			}
			List<T> c = topology.getChildrenOf(root);
			_apply(c);
			stack.pop();
			if (setCleanOnExit)
				seen.clear();
		}

		public boolean hasSeen(T t) {
			return seen.contains(t);
		}

		public TopologyVisitory_depthFirst<T> setSetCleanOnExit(boolean setCleanOnExit) {
			this.setCleanOnExit = setCleanOnExit;
			return this;
		}

		protected VisitCode _apply(List<T> c) {
			for (T n : c) {
				if (!avoidLoops || !seen.contains(n)) {
					if (avoidLoops)
						seen.add(n);
					stack.push(n);
					VisitCode code = visit(n);
					if (code == VisitCode.stop)
						return VisitCode.stop;
					if (code != VisitCode.skip) {
						VisitCode vc = _apply(topology.getChildrenOf(n));
						if (vc == VisitCode.stop)
							return VisitCode.stop;
					}
					exit(stack.pop());
				}
			}
			return VisitCode.cont;
		}

		protected void exit(T t) {
		}

		protected String spaces(int n) {
			StringBuffer buf = new StringBuffer(n);
			for (int i = 0; i < n; i++)
				buf.append(' ');
			return buf.toString();
		}

		abstract protected VisitCode visit(T n);
	}

	static public class TreeElement<T> {
		public T pointer;

		public TreeElement<T> parent;

		public TreeElement(T pointer, TreeElement<T> parent) {
			this.pointer = pointer;
			this.parent = parent;
		}

		@Override
		public String toString() {
			return pointer + " ";
		}
	}

	static public <T> List<T> allBelow(T root, iTopology<T> topology) {
		final ArrayList<T> r = new ArrayList<T>();
		new TopologyVisitory_depthFirst<T>(true, topology) {
			@Override
			protected VisitCode visit(T n) {
				r.add(n);
				return VisitCode.cont;
			};
		}.apply(root);
		return r;
	}
}
