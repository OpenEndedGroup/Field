package field.core.plugins.history;

import java.util.ArrayList;
import java.util.List;

import field.core.ui.FieldMenus2;
import field.core.util.ExecuteCommand;
import field.namespace.diagram.Channel;
import field.namespace.diagram.DiagramZero.iMarker;
import field.util.diff.ChannelDifferences;
import field.util.diff.ChannelDifferences.EditRelationship;
import field.util.diff.ChannelDifferences.EditType;

public class GitVersioningQueries {

	private final String workspace;

	enum Op {
		added, modified, deleted, inmemory;
	}

	public class VersionsOfFile {
		String commit;
		String contents;
		String date;
		String author;
		String path;
		Op op;

		List<VersionsOfFile> parents = new ArrayList<VersionsOfFile>();

		public String getContents() {
			if (contents == null) {

				String p2 = path.replace(FieldMenus2.getCanonicalVersioningDir(), "");
				while (p2.startsWith("/")) {
					p2 = p2.substring(1);
				}
				while (p2.contains("//"))
					p2 = p2.replaceAll("//", "/");
				ExecuteCommand e = new ExecuteCommand(workspace, new String[] { GitVersioningSystem.gitCommand, "show", commit + ":" + p2 }, true);
				e.waitFor(true);
				contents = e.getOutput();
			}
			return contents;
		}

	}

	public class Snippet {
		int start, stop;
		String contents;

		public Snippet() {
			super();
		}
	}

	public class DiffSet {
		String[] a;
		String[] b;

		Channel<ChannelDifferences<String>.EditRelationship> relationships;

		public DiffSet(String[] a, String[] b) {
			this.a = a;
			this.b = b;
			;//System.out.println(" -- diff constructor 0");
			Channel<String> left = new Channel<String>();
			Channel<String> right = new Channel<String>();

			for (int i = 0; i < a.length; i++)
				left.makeMarker(left, i, 0, a[i]);
			for (int i = 0; i < b.length; i++)
				right.makeMarker(right, i, 0, b[i]);

			;//System.out.println(" -- diff constructor ");
			ChannelDifferences<String> m = new ChannelDifferences<String>(left, right);
			;//System.out.println(" -- diff constructor A ");
			relationships = m.makeRelationships();
			;//System.out.println(" -- diff constructor B ");
			relationships = m.generateUnequivalences(relationships, 10);
			;//System.out.println(" -- diff constructor C ");
			relationships = m.clumpEquivalences(relationships);
			;//System.out.println(" -- diff constructor D ");
			;//System.out.println(" -- diff constructor out ");
		}

		public Snippet snippetsForVersions(int lineStart, int lineEnd) {
			Snippet s = new Snippet();
			s.start = Integer.MIN_VALUE;
			s.stop = Integer.MAX_VALUE;
			s.contents = "";
			for (int i = lineStart; i < lineEnd; i++) {
				Integer m = mapLineOut(i);
				if (m != null) {
					s.start = Math.min(s.start, m);
					s.stop = Math.max(s.stop, m + 1);
					s.contents += b[m].replaceFirst("string>", "") + "\n";
				}
			}
			return s;
		}

		public ChannelDifferences<String>.EditRelationship mapLine(int a) {
			for (iMarker<ChannelDifferences<String>.EditRelationship> e : relationships) {
				for (iMarker<String> m : e.getPayload().left) {
					if (m.getTime() == a) {
						return e.getPayload();
					}
				}
			}
			return null;
		}

		public Integer mapLineOut(int a) {
			try {
				for (iMarker<ChannelDifferences<String>.EditRelationship> e : relationships) {

					int index = 0;
					
					;//System.out.println("ER "+e.getPayload());
					
					if (e.getPayload().left != null && e.getPayload().right != null)
						for (iMarker<String> m : e.getPayload().left) {
							;//System.out.println(" time is "+m);
							if (m.getTime() == a && e.getPayload().right.size()>0) {
								EditRelationship p = e.getPayload();
								if (p.type == EditType.equivalence) {
									return (int) e.getPayload().right.get(Math.min(e.getPayload().right.size()-1, index)).getTime();
								} else if (p.type == EditType.unequivalence) {
									return (int) e.getPayload().right.get(Math.min(e.getPayload().right.size()-1, index)).getTime();
								} else
									return null;
							}
							index++;
						}
				}
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	public GitVersioningQueries(String workspace) {
		this.workspace = workspace;
	}

	public List<VersionsOfFile> versionsForFile(String now, String path) {
		ExecuteCommand e = new ExecuteCommand(workspace, new String[] { GitVersioningSystem.gitCommand, "log", "--date", "local", "--name-status", path }, true);
		e.waitFor(true);
		String out = e.getOutput();
		String[] lines = out.split("\n");
		int i = 0;
		VersionsOfFile v = null;
		List<VersionsOfFile> all = new ArrayList<VersionsOfFile>();
		v = new VersionsOfFile();
		v.commit = null;
		v.contents = now;
		v.op = Op.inmemory;
		all.add(v);
		v = null;
		while (i < lines.length) {
			if (lines[i].startsWith("commit")) {
				VersionsOfFile prev = v;
				v = new VersionsOfFile();
				;//System.out.println(" lines <" + lines[i] + ">");
				v.commit = lines[i].split(" ")[1];
				v.path = path;
				if (prev != null) {
					prev.parents.add(v);
				}
				all.add(v);
			}
			if (lines[i].startsWith("Author")) {
				v.author = lines[i].substring("Author:".length()).trim();
			}
			if (lines[i].startsWith("Date")) {
				v.date = lines[i].substring("Date:".length()).trim();
			}
			if (lines[i].startsWith("A "))
				v.op = Op.added;
			if (lines[i].startsWith("M "))
				v.op = Op.modified;
			if (lines[i].startsWith("D "))
				v.op = Op.deleted;

			i++;
		}

		return all;
	}

}
