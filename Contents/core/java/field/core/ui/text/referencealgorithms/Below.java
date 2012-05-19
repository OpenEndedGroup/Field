package field.core.ui.text.referencealgorithms;

import java.util.List;

import field.core.dispatch.iVisualElement;


public class Below extends Above {
	@Override
	protected List<iVisualElement> doEvaluation(iVisualElement root, List<iVisualElement> old, iVisualElement forElement) {
		atPoint.set(0.5f, 1, 0);
		targetDirection.set(0,1,0);
		otherDirection.set(1,0,0);
		return super.doEvaluation(root, old, forElement);
	}
}
