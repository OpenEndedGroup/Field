package field.core.plugins.pseudo;

import java.util.List;
import java.util.Set;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.Ref;
import field.core.plugins.pseudo.PseudoPropertiesPlugin.Collector;
import field.core.windowing.components.DraggableComponent.Resize;
import field.math.linalg.Vector2;

public class CustomResize {

	public interface iApplyResize {
		public boolean apply(float x, float y, Vector2 delta, iVisualElement to);
	}

	static public final VisualElementProperty<iApplyResize> resizer = new VisualElementProperty<iApplyResize>("resizer_");
	
	static public boolean applyResize(iVisualElement to, Set<Resize> resize, Vector2 delta) {
		Ref<Collector> rr = new Ref<Collector>(null);
		PseudoPropertiesPlugin.getCollector(to, rr);
		Collector c = rr.get();
		List<iApplyResize> collected = (List<iApplyResize>) c.getAttribute(resizer.getName());

		float x = 0.5f, y = 0.5f;
		for (Resize r : resize) {
			switch (r) {
			case down:
				y = 1;
				break;
			case innerScale:
				break;
			case innerTranslate:
				break;
			case left:
				x = 0;
				break;
			case right:
				x = 1;
				break;
			case translate:
				break;
			case up:
				y = 0;
				break;

			}
		}
		boolean q = false;
		for (iApplyResize cc : collected) {
			q |= cc.apply(x, y, delta, to);
		}

		return q;
	}

}
