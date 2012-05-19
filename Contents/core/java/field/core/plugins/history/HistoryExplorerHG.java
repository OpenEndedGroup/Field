package field.core.plugins.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import field.bytecode.protect.Woven;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.history.HGTools.HGLog;
import field.math.graph.GraphNodeSearching;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.namespace.diagram.Channel;
import field.namespace.diagram.Channel.Marker;
import field.util.diff.ChannelDifferences;
import field.util.diff.Diff;
import field.util.diff.Diff.Change;

@Woven
public class HistoryExplorerHG {

	public class DiffSetVertical {
		public List<Diff.Change> changes;

		public Channel<String> from;

		public Channel<String> to;

		public long fromVersion;

		public long toVersion;

		public Channel<ChannelDifferences<String>.EditRelationship> relationships;

		public DiffSetVertical(Diff.Change head, String[] left, String[] right, long leftVersion, long rightVersion) {
			from = new Channel<String>();
			to = new Channel<String>();
			for (int i = 0; i < left.length; i++)
				new Marker<String>(i, 1, left[i], from);
			for (int i = 0; i < right.length; i++)
				new Marker<String>(i, 1, right[i], to);

			fromVersion = leftVersion;
			toVersion = rightVersion;
			changes = new ArrayList<Change>();
			Change ch = head;
			while (ch != null) {
				changes.add(ch);
				ch = ch.link;
			}

			ChannelDifferences<String> diff = new ChannelDifferences<String>(from, to);
			relationships = diff.makeRelationships();
			relationships = diff.generateUnequivalences(relationships, 1);
		}

	}

	static public class VersionNode extends field.math.graph.NodeImpl<VersionNode> {
		public VersionNodeType type;

		public long revision;

		public String logEntry;

		public String path;

		public Date date;

		public String copyToFilename;

		public String copiedFromFilename;
		public VersionNode next;
		public VersionNode previous;

		public VersionNode copyTo;

		public VersionNode copiedFrom;

		@Override
		public List<VersionNode> getChildren() {
			if (next == null)
				return Collections.EMPTY_LIST;
			return Collections.singletonList(next);

		}

		@Override
		public List<VersionNode> getParents() {
			if (previous== null)
				return Collections.EMPTY_LIST;
			return Collections.singletonList(previous);

		}

		@Override
		public String toString() {
			return type + " " + path + " '" + logEntry + "'" + revision+" "+(copyToFilename==null ? "" : "copied to "+copyToFilename)+" "+(copiedFromFilename==null ? "" : "copied from "+copiedFromFilename);
		}
	}

	public enum VersionNodeType {
		origin, edit, copySource, tag, copyTarget;
	}

	private final String repositoryRoot;

	private final String prefix;

	private final HGTools tools;

	private final String sheetName;

	public HistoryExplorerHG(String repositoryRoot, String sheetName) {
		this.sheetName = sheetName;
		this.repositoryRoot = repositoryRoot;
		this.prefix = repositoryRoot + "/";
		tools = new HGTools(repositoryRoot, sheetName, null);
	}

	public Map<VersionNode, DiffSetVertical> buildCompleteDiffHistory(VersionNode root) {
		return buildCompleteDiffHistory(root, null);
	}

	public Map<VersionNode, DiffSetVertical> buildCompleteDiffHistory(VersionNode root, final String head) {
		final Map<VersionNode, DiffSetVertical> ret = new HashMap<VersionNode, DiffSetVertical>();
		final Map<VersionNode, String> cache = new HashMap<VersionNode, String>();

//		System.out.println(" building diff history ");

		new GraphNodeSearching.GraphNodeVisitor_depthFirst<VersionNode>(false) {
			@Override
			protected VisitCode visit(VersionNode n) {

//				System.out.println(" at <"+n+">");

				List<VersionNode> parents = n.getChildren();
				String left = null;
				long leftVersion = 0;
				if (parents.size()>0)
				{
//					System.out.println(" fetching parent <"+parents.get(0).revision+"> <"+parents.get(0).path+">");
					left = (String) getVersionProperty(parents.get(0).revision, parents.get(0).path, null);
					leftVersion = parents.get(0).revision;
				}
				else
				{
					left = "";
				}

				String right = (String) getVersionProperty(n.revision, n.path, null);

				long rightVersion = n.revision;
//				System.out.println(" comparing\n"+left+"("+leftVersion+")\n"+right+"("+n.revision+")");

				String l = left;
				left = right;
				right = l;
				long ll = leftVersion;
				leftVersion= rightVersion;
				rightVersion = ll;

				Diff diff = new Diff(left.split("\n"), right.split("\n"));
				Change change = diff.diff_2(false);
				DiffSetVertical vert = new DiffSetVertical(change, left.split("\n"), right.split("\n"), leftVersion, rightVersion);

				ret.put(n, vert);

				return VisitCode.cont;
			}

		}.apply(root);

		return ret;
	}

	public Set<VersionNode> buildHistoryGraph(String string) {
		System.err.println(" building history graph for file <" + string + ">");
		HGLog log = new HGLog(string);
		List<VersionNode> vn = log.getVersionNodes();

	//	System.out.println(" nodes are <"+vn+">");

//		for (VersionNode n : vn) {
//			System.out.println("node <" + System.identityHashCode(n) + ">");
//			System.out.println("   " + n);
//			System.out.println("   path:"+n.path);
//
//			System.out.println("   next:" + System.identityHashCode(n.next));
//			System.out.println("   copiedFrom:" + System.identityHashCode(n.copiedFrom));
//			System.out.println("   copiedTo:" + System.identityHashCode(n.copyTo));
//		}

		return new LinkedHashSet<VersionNode>(vn);
	}

	public String getPrefix() {
		return prefix;
	}

	public String getSheetPrefix() {
		return prefix + "/" + sheetName + "/";
	}

	public Object getVersionProperty(long revision, String path, VisualElementProperty property) {

		return tools.getPropertyAtVersion(revision, path, property);

	}

}
