package field.core.plugins.history;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import field.core.plugins.history.HistoryExplorerHG.DiffSetVertical;
import field.core.plugins.history.HistoryExplorerHG.VersionNode;
import field.namespace.diagram.DiagramZero.iMarker;
import field.namespace.generic.Generics.Pair;
import field.namespace.generic.Generics.Triple;
import field.util.diff.ChannelDifferences;

/*
 * history of lines
 */
public class AnnotationInformation {

	private final HistoryExplorerHG hgSupport;
	private final String file;
	private Map<VersionNode, DiffSetVertical> verticals;

	private String[] lines;
	List<VersionNode> versions = new ArrayList<VersionNode>();

	public AnnotationInformation(HistoryExplorerHG hgSupport, String file) {
		this.hgSupport = hgSupport;
		this.file = file;
	}

	public int compressedHistoryLengthForLine(int line) {
		return compressedTrace(line).size();
	}

	public ArrayList<Triple<VersionNode, Integer, Pair<String, String>>> compressedTrace(int line) {

		//;//System.out.println(" generating compressed trace for line <" + line + ">");
		List<Triple<VersionNode, Integer, Pair<String, String>>> cc = traceLine(line);

		//;//System.out.println(" trace is: ");
		//for (Triple<VersionNode, Integer, Pair<String, String>> x : cc) {
		//	;//System.out.println(x.right.left + " -> " + x.right.right);
		//}

		ArrayList<Triple<VersionNode, Integer, Pair<String, String>>> a = new ArrayList<Triple<VersionNode, Integer, Pair<String, String>>>();
		Pair<String, String> last = null;
		Triple<VersionNode, Integer, Pair<String, String>> lastt = null;
		for (Triple<VersionNode, Integer, Pair<String, String>> t : cc) {
			if (!t.right.left.equals(t.right.right)) {
				a.add(t);
			}
			last = t.right;
			lastt = t;
		}
		if (lastt != null)
			a.add(lastt);

		//;//System.out.println(" returning <" + a + ">");

		return a;

	}

	public void construct(String currentContents) {
		;//System.out.println(" inside construct "+currentContents+" building graph ...");
		Set<VersionNode> nodes = hgSupport.buildHistoryGraph(file);
		;//System.out.println(" history graph has <"+nodes.size()+">");
		versions = new ArrayList<VersionNode>(nodes);
		Iterator<VersionNode> i = nodes.iterator();
		if (i.hasNext()) {
			VersionNode n = i.next();
			;//System.out.println(" diff history step...");
			verticals = hgSupport.buildCompleteDiffHistory(n, currentContents);
		} else {
			verticals = null;
		}

		lines = currentContents.split("\n");
	}

	public Pair<Integer, Integer> domainForLine(int line, int maxLine) {
		ArrayList<Triple<VersionNode, Integer, Pair<String, String>>> c = compressedTrace(line);
		Pair<Integer, Integer> startstop = new Pair<Integer, Integer>(line, line);
		while (startstop.left > 0) {
			ArrayList<Triple<VersionNode, Integer, Pair<String, String>>> x = compressedTrace(startstop.left - 1);
			if (equiv(c, x))
				startstop.left--;
			else
				break;
		}
		while (startstop.right+1 < maxLine) {
			ArrayList<Triple<VersionNode, Integer, Pair<String, String>>> x = compressedTrace(startstop.right + 1);

//			;//System.out.println(" considering line <" + (startstop.right + 1) + ">");
//			;//System.out.println(" comparing <" + c + ">\n and <" + x + ">");
			if (equiv(c, x))
				startstop.right++;
			else {
				//;//System.out.println(" not equal");
				break;
			}
		}

		return startstop;
	}

	public Pair<Integer, Integer> domainForLine(int line, int maxLine, Date before) {
		ArrayList<Triple<VersionNode, Integer, Pair<String, String>>> c = compressedTrace(line);
		Pair<Integer, Integer> startstop = new Pair<Integer, Integer>(line, line);
		while (startstop.left > 0) {
			ArrayList<Triple<VersionNode, Integer, Pair<String, String>>> x = compressedTrace(startstop.left - 1);
			if (equiv(c, x, before))
				startstop.left--;
			else
				break;
		}
		while (startstop.right+1 < maxLine) {
			ArrayList<Triple<VersionNode, Integer, Pair<String, String>>> x = compressedTrace(startstop.right + 1);
			if (equiv(c, x, before))
				startstop.right++;
			else
				break;
		}

		return startstop;
	}

	public int historyLengthForLine(int line) {
		return traceLine(line).size();
	}

	public List<Triple<VersionNode, Integer, Pair<String, String>>> traceLine(int line) {
		//;//System.out.println(" tracing line <" + line + "> over <" + versions.size() + "> versions");
		ArrayList<Triple<VersionNode, Integer, Pair<String, String>>> a = new ArrayList<Triple<VersionNode, Integer, Pair<String, String>>>();
		String string = lines[line];

		for (VersionNode v : versions) {
			DiffSetVertical diffset = verticals.get(v);

			//;//System.out.println(" diffset.relationships <" + diffset.relationships.getIterator().remaining() + ">");

			// can never happen if currentContents!=null ?
			if (diffset != null) {
				diffset.relationships.setSliceIsGreedy(true);
				List<iMarker<ChannelDifferences<String>.EditRelationship>> m = diffset.relationships.getSlice(line, line + 1).getIterator().remaining();

				//;//System.out.println(" edit relationship <" + m.size() + ">");
				//if (m.size() > 0)
				//	;//System.out.println("           " + m.get(0).getPayload().right + " " + m.get(0).getPayload().left);

				if (m.size() > 0 && m.get(0).getPayload().right != null && m.get(0).getPayload().left != null) {
					String becomes = m.get(0).getPayload().right.get(0).getPayload();
					String was = m.get(0).getPayload().left.get(0).getPayload();
					line = (int) m.get(0).getPayload().right.get(0).getTime();
					a.add(new Triple<VersionNode, Integer, Pair<String, String>>(v, line, new Pair<String, String>(was, becomes)));
					string = was;
					//;//System.out.println(" line now <" + line + "> <" + was + " -> " + becomes + ">");
				} else if (string != null && m.size() > 0 && m.get(0).getPayload().right == null && m.get(0).getPayload().left != null) {
					// line originated here
					a.add(new Triple<VersionNode, Integer, Pair<String, String>>(v, line, new Pair<String, String>(string, null)));
					break;
				} else {
					break;
				}
			}
		}
		return a;
	}

	private boolean equiv(ArrayList<Triple<VersionNode, Integer, Pair<String, String>>> c, ArrayList<Triple<VersionNode, Integer, Pair<String, String>>> x) {
		//;//System.out.println(" equiv <" + c.size() + " " + x.size() + ">");

		if (c.size() != x.size())
			return false;
		for (int i = 0; i < c.size(); i++) {
			Triple<VersionNode, Integer, Pair<String, String>> a = c.get(i);
			Triple<VersionNode, Integer, Pair<String, String>> b = x.get(i);
			//;//System.out.println(" " + a.left.revision + " " + b.left.revision);
			if (a.left.revision != b.left.revision)
				return false;
		}
		return true;
	}

	private boolean equiv(ArrayList<Triple<VersionNode, Integer, Pair<String, String>>> c, ArrayList<Triple<VersionNode, Integer, Pair<String, String>>> x, Date before) {
		ArrayList<Triple<VersionNode, Integer, Pair<String, String>>> c2 = new ArrayList<Triple<VersionNode, Integer, Pair<String, String>>>();
		ArrayList<Triple<VersionNode, Integer, Pair<String, String>>> x2 = new ArrayList<Triple<VersionNode, Integer, Pair<String, String>>>();

		for (int i = 0; i < c.size(); i++) {
			if (c.get(i).left.date.before(before) || c.get(i).left.date.equals(before))
				c2.add(c.get(i));
		}

		for (int i = 0; i < x.size(); i++) {
			if (x.get(i).left.date.before(before) || x.get(i).left.date.equals(before))
				x2.add(x.get(i));
		}

		return equiv(c2, x2);
	}

}
