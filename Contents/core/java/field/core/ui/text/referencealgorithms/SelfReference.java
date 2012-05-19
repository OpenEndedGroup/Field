package field.core.ui.text.referencealgorithms;

import java.util.ArrayList;
import java.util.List;

import field.core.dispatch.iVisualElement;
import field.core.ui.text.embedded.iReferenceAlgorithm;


public class SelfReference extends  iReferenceAlgorithm.BaseReferenceAlgorithm{

	@Override
	protected List<iVisualElement> doEvaluation(iVisualElement root, List<iVisualElement> old, iVisualElement forElement) {
		ArrayList<iVisualElement> r = new ArrayList<iVisualElement>();
		r.add(forElement);
		r.add(forElement);
		return r;
	}
}
