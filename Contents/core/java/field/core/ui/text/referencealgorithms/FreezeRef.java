package field.core.ui.text.referencealgorithms;

import java.util.List;

import field.core.dispatch.iVisualElement;
import field.core.ui.text.embedded.iReferenceAlgorithm.BaseReferenceAlgorithm;


public class FreezeRef extends BaseReferenceAlgorithm {
	@Override
	protected List<iVisualElement> doEvaluation(iVisualElement root, List<iVisualElement> old, iVisualElement forElement) {
		return old;
	}
}
