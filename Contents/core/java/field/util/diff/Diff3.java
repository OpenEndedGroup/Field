package field.util.diff;

import java.util.List;

import field.namespace.diagram.Channel;
import field.namespace.diagram.DiagramZero.iMarker;
import field.namespace.diagram.DiagramZero.iMarkerIterator;
import field.util.diff.ChannelDifferences.EditType;


/**
 * not this _isn't_ the diff3 algorithm. primacy is given to one of the children
 */
public class Diff3 {

	private final String primary;

	private final String secondary;

	private final String old;

	private final String result;

	public Diff3(String primary, String old, String secondary) {
		this.primary = primary;
		this.old = old;
		this.secondary = secondary;

		Channel<Character> p = toArray(primary);
		Channel<Character> o = toArray(old);
		Channel<Character> s = toArray(secondary);

		ChannelDifferences<Character> c1 = new ChannelDifferences<Character>(o, p);
		ChannelDifferences<Character> c2 = new ChannelDifferences<Character>(o, s);

		Channel<ChannelDifferences<Character>.EditRelationship> rel1 = c1.makeRelationships();
		Channel<ChannelDifferences<Character>.EditRelationship> rel2 = c2.makeRelationships();

		for (iMarker<ChannelDifferences<Character>.EditRelationship> m : rel1) {

		}
		for (iMarker<ChannelDifferences<Character>.EditRelationship> m : rel2) {
		}

		// remove any deletion from rel2 that overlaps with any non-equivalant change
		{
			iMarkerIterator<ChannelDifferences<Character>.EditRelationship> i1 = rel1.getIterator();
			while (i1.hasNext()) {
				iMarker<ChannelDifferences<Character>.EditRelationship> n1 = i1.next();
				if (n1.getPayload().type != EditType.equivalence) {
					List<iMarker<ChannelDifferences<Character>.EditRelationship>> se = rel2.getSlice(n1.getTime(), n1.getTime() + n1.getDuration()).getIterator().remaining();
					for (iMarker<ChannelDifferences<Character>.EditRelationship> m : se) {
						rel2.removeMarker(rel2, m);
					}
				}

			}
		}

		for (iMarker<ChannelDifferences<Character>.EditRelationship> m : rel2) {
		}

		// iterate through these things together
		iMarkerIterator<ChannelDifferences<Character>.EditRelationship> i1 = rel1.getIterator();
		iMarkerIterator<ChannelDifferences<Character>.EditRelationship> i2 = rel2.getIterator();

		StringBuffer output = new StringBuffer();

		while (i1.hasNext() || i2.hasNext()) {
			iMarker<ChannelDifferences<Character>.EditRelationship> n1 = i1.hasNext() ? i1.next() : null;
			iMarker<ChannelDifferences<Character>.EditRelationship> n2 = i2.hasNext() ? i2.next() : null;

			if (n1 == null || n1.getPayload().type == EditType.equivalence) {
				if (n2 == null || n2.getPayload().type == EditType.equivalence) {
					if (n1 != null) {
						output.append(n1.getPayload().left.get(0).getPayload());
						if (n2 != null) {
							assert n1.getPayload().left.get(0).getPayload().equals(n2.getPayload().left.get(0).getPayload()) : n1 + "!= " + n2;
						}
					}
				} else if (n2.getPayload().type == EditType.insertion) {
					// if (n1 != null) if (n1.getTime()<=n2.getTime()) output.append(n1.getPayload().left.get(0).getPayload());
					for (int i = 0; i < n2.getPayload().right.size(); i++) {
						output.append(n2.getPayload().right.get(i).getPayload());
					}
					// if (n1 != null) if (n1.getTime()>n2.getTime()) output.append(n1.getPayload().left.get(0).getPayload());
					i1.previous();
				} else if (n2.getPayload().type == EditType.deletion) {
					// we have to skip over
					// if (n1 != null) for (int i = 0; i < n2.getPayload().left.size() - 1; i++) {
					// iMarker<ChannelDifferences<Character>.EditRelationship> q = i1.next();
					// // we know that these are going to be equivilences
					// assert q.getPayload().type == EditType.equivalence : q;
					// }
					if (n1 != null) {
						while (i2.hasNext() && n2 != null && n2.getTime() + n2.getDuration() <= n1.getTime() + n1.getDuration())
							n2 = i2.hasNext() ? i2.next() : null;
						i2.previous();
					}

				}
			} else if (n1.getPayload().type == EditType.insertion) {
				for (int i = 0; i < n1.getPayload().right.size(); i++) {
					output.append(n1.getPayload().right.get(i).getPayload());
				}
				i2.previous();
			} else if (n1.getPayload().type == EditType.deletion) {
				// don't have to copySource anything
				while (i2.hasNext() && n2 != null && n2.getTime() + n2.getDuration() <= n1.getTime() + n1.getDuration())
					n2 = i2.hasNext() ? i2.next() : null;
				i2.previous();
				// for (int i = 0; i < n1.getPayload().left.size();) {
				// if (!i2.hasNext()) break;
				// iMarker<ChannelDifferences<Character>.EditRelationship> q = i2.hasNext() ? i2.next() : null;
				// i += q.getPayload().left != null ? q.getPayload().left.size() : 0;
				// }
			}
		}
		result = output.toString();
	}

	public String getResult() {
		return result;
	};

	private Channel toArray(String l) {
		Channel<Character> c = new Channel<Character>();
		for (int i = 0; i < l.length(); i++) {
			c.makeMarker(c, i, 1f, l.charAt(i));
		}
		return c;
	}

}
