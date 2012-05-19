package field.core.windowing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.components.iComponent;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;

public class WindowSpaceBox {

	static public final VisualElementProperty<Boolean> isWindowSpace = new VisualElementProperty<Boolean>("isWindowSpace");
	private GLComponentWindow window;

	public WindowSpaceBox(GLComponentWindow window) {
		this.window = window;
	}

	HashMap<iVisualElement, Rect> frozen = new HashMap<iVisualElement, Rect>();
	private Vector4 frozenAt;

	public void freeze() {
		ComponentContainer root = window.getRoot();

		frozen  = new HashMap<iVisualElement, iVisualElement.Rect>();
		frozenAt = new Vector4(window.getXScale(), window.getYScale(), window.getXTranslation(), window.getYTranslation());
		
		doFreeze(root);
	}
	
	public void thaw()
	{
		Vector4 thawAt = new Vector4(window.getXScale(), window.getYScale(), window.getXTranslation(), window.getYTranslation());

		if (thawAt.equals(frozenAt)) return;
		
		for(Map.Entry<iVisualElement, Rect> e : frozen.entrySet())
		{
			Rect r = transform(e.getValue(), frozenAt, thawAt);

			e.getKey().getProperty(iVisualElement.overrides).shouldChangeFrame(e.getKey(), r, e.getKey().getFrame(null), true);
		}
		window.requestRepaint();
	}

	private Rect transform(Rect was, Vector4 from, Vector4 to) {
		
		Vector2 p1 = new Vector2(was.x, was.y);
		Vector2 p2 = new Vector2(was.x+was.w, was.y+was.h);
		
		transform(p1, from, to);
		transform(p2, from, to);
		
		return new Rect(p1.x, p1.y, p2.x-p1.x, p2.y-p1.y);
		
	}

	private void transform(Vector2 p, Vector4 from, Vector4 to) {
		p.x = (p.x-from.z)/from.x;
		p.y = (p.y-from.w)/from.y;
		
		p.x = p.x*to.x+to.z;
		p.y = p.y*to.y+to.w;
	}

	protected void doFreeze(iComponent root) {
		iVisualElement x = root.getVisualElement();
		if (x != null && isWindowSpace.getBoolean(x, false)) {
			frozen.put(x, x.getFrame(null));
		}

		if (root instanceof ComponentContainer) {
			List<iComponent> children = ((ComponentContainer) root).components;
			for (iComponent cc : children) {
				doFreeze(cc);
			}
		}
	}

}
