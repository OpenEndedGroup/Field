package field.core.ui.text.embedded;

import java.util.ArrayList;
import java.util.List;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.Ref;
import field.math.graph.TopologySearching;
import field.math.graph.TopologyViewOfGraphNodes;
import field.math.graph.GraphNodeSearching.VisitCode;


public interface iReferenceAlgorithm {

	static public abstract class BaseReferenceAlgorithm implements iReferenceAlgorithm
	{
		public List<iVisualElement> evaluate(iVisualElement root, String uniqueReferenceID, String algorithmName, iVisualElement forElement)
		{
			VisualElementProperty pr = new VisualElementProperty(uniqueReferenceID);
			List<iVisualElement> prop = (List<iVisualElement>) forElement.getProperty(pr);
			List<iVisualElement> newProp = doEvaluation(root, prop, forElement);


			//forElement.setProperty(	pr, newProp)

			String name = algorithmName;
			forElement.setProperty(	new VisualElementProperty(uniqueReferenceID+"-source"), name);
			new iVisualElementOverrides.MakeDispatchProxy().getBackwardsOverrideProxyFor(forElement).setProperty(forElement, pr, new Ref(newProp));
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).setProperty(forElement, pr, new Ref(newProp));

			assert newProp!=null;
			return newProp;
		}



		protected List<iVisualElement> allVisualElements(iVisualElement root)
		{


			final List<iVisualElement> ret = new ArrayList<iVisualElement>();
			new TopologySearching.TopologyVisitor_breadthFirst<iVisualElement>(true){
				@Override
				protected VisitCode visit(iVisualElement n) {
					String name = n.getProperty(iVisualElement.name);
					;//System.out.println(" adding <"+n+" called <"+name+">");
					ret.add(n);
					return VisitCode.cont;
				}

			}.apply(new TopologyViewOfGraphNodes<iVisualElement>(false).setEverything(true), root);
			return ret;
		}

		abstract protected List<iVisualElement> doEvaluation(iVisualElement root, List<iVisualElement> old, iVisualElement forElement);

	}


	/**
	 * in addition to computing and returning, this must set a property uniqueReferenceID on forElement
	 *
	 * we can scrub these, upon deletion, when changes are posted to the text, and __minimalReferences dissappear from the python source
	 *
	 * (we'll have an additional plugin here for that)
	 * @param algorithmName TODO
	 */
	public List<iVisualElement> evaluate(iVisualElement root, String uniqueReferenceID, String algorithmName, iVisualElement forElement) ;
}
