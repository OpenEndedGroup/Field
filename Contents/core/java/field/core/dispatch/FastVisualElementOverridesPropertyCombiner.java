package field.core.dispatch;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.Ref;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.graph.iMutable;

public class FastVisualElementOverridesPropertyCombiner<U, T> {

	/**
	 * oh look, it's a monad
	 */
	public interface iCombiner<U, T>
	{
		public T unit();
		public T bind(T t, U u);
	}
	
	private final boolean backwards;

	public FastVisualElementOverridesPropertyCombiner(boolean backwards)
	{
		this.backwards = backwards;
	}
	
	public T getProperty(iVisualElement source, VisualElementProperty<U> prop, iCombiner<U, T> combiner) {

		List<iVisualElement> fringe = new LinkedList<iVisualElement>();
		fringe.add(source);

		T at = combiner.unit();
		
		HashSet<iVisualElement> seen = new LinkedHashSet<iVisualElement>();

		
		Ref<U> ref = new Ref<U>(null);
		
		while (fringe.size() > 0) {
			iVisualElement next = fringe.remove(0);
			if (!seen.contains(next)) {
				iVisualElementOverrides over = next.getProperty(iVisualElement.overrides);
				if (over != null) {
					
					
					ref.set(null);
					VisitCode o = over.getProperty(source, prop, ref);
					U u = ref.get();
					at = combiner.bind(at, u);
					
					if (o.equals(VisitCode.skip)) {
					} else if (o.equals(VisitCode.stop)) {
						return at;
					} else {
						if (backwards)
							fringe.addAll((Collection<? extends iVisualElement>) sort(next.getParents()));
						else
							fringe.addAll(sort(next.getChildren()));
					}
				}
				seen.add(next);
			}
		}
		return at;
	}

	protected Collection<? extends iVisualElement> sort(List<? extends iMutable<iVisualElement>> parents) {
		return (Collection<? extends iVisualElement>) parents;
	}

}
