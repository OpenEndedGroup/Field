package field.core.ui.text.referencealgorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.ui.text.embedded.iReferenceAlgorithm.BaseReferenceAlgorithm;
import field.math.linalg.Vector2;


public class Called extends BaseReferenceAlgorithm implements Comparator<iVisualElement> {

	private Pattern pattern;

	private Vector2 center;

	public Called(String reg) {
		pattern = Pattern.compile(reg);
	}

	@Override
	protected List<iVisualElement> doEvaluation(iVisualElement root, List<iVisualElement> old, iVisualElement forElement) {
		ArrayList<iVisualElement> r = new ArrayList<iVisualElement>();

		List<iVisualElement> all = allVisualElements(root);

		center = forElement.getFrame(null).midpoint2();

		for (iVisualElement v : all) {
			if (pattern.matcher(v.getProperty(iVisualElement.name)).matches()) r.add(v);
		}

		Collections.sort(r, this);

		return r;
	}

	Rect tFrame = new Rect(0, 0, 0, 0);

	public int compare(iVisualElement o1, iVisualElement o2) {
		float d1 = o1.getFrame(tFrame).midpoint2().distanceFrom(center);
		float d2 = o2.getFrame(tFrame).midpoint2().distanceFrom(center);
		return d2 < d1 ? 1 : -1;
	}
}
