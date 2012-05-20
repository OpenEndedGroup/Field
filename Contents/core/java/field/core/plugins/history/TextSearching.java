package field.core.plugins.history;

import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

import field.math.graph.GraphNodeSearching;
import field.math.graph.NodeImpl;
import field.math.graph.iMutable;
import field.math.graph.GraphNodeSearching.VisitCode;


public class TextSearching {

	public interface iProvidesSearchTerms {
		public iMutable duplicate();

		public void found(int n);

		public String[] getTerms();

		public void notFound();
	}

	private final Pattern pattern;

	private final String expression;

	public TextSearching(String expression) {
		this.expression = expression;
		pattern = Pattern.compile(expression);
	}

	public <T extends NodeImpl<T>> T search(T root) {

		final HashSet<Object> keep = new HashSet<Object>();

		new GraphNodeSearching.GraphNodeVisitor_depthFirst<T>(false){

			@Override
			protected VisitCode visit(T n) {

				if (n instanceof iProvidesSearchTerms) {
					String[] c = ((iProvidesSearchTerms) n).getTerms();
					boolean found = false;
					int q = 0;
					for (int i = 0; i < c.length; i++) {


						if (c[i] != null) if (pattern.matcher(c[i]).find()) {
							q = i;


							((iProvidesSearchTerms) n).found(i);
							found = true;
						}
					}
					if (!found) {
						((iProvidesSearchTerms) n).notFound();
					} else {
						for (int i = 0; i < stack.size(); i++) {
							keep.add(stack.get(i));
						}
					}
				}

				return VisitCode.cont;

			}
		}.apply(root);

		final HashMap<T, T> created = new HashMap<T, T>();
		final iMutable[] newroot = { null};

		new GraphNodeSearching.GraphNodeVisitor_depthFirst<T>(false){

			@Override
			protected VisitCode visit(T n) {

				if (keep.contains(n)) {
					;//System.out.println(" cloning <"+n+">");
					iMutable cloned = ((iProvidesSearchTerms) n).duplicate();
					created.put(n, (T) cloned);

					if (n.getParents().size() > 0) {
						T p = created.get(n.getParents().get(0));
						((iMutable) p).addChild(cloned);
					} else {
						assert newroot[0] == null;
						newroot[0] = cloned;
					}
				}

				return VisitCode.cont;

			}
		}.apply(root);

		return (T) newroot[0];
	}

}
