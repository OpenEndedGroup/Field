package field.util;

import java.util.List;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;

public class CascadedResize {

	private List<iVisualElement> e;

	public CascadedResize(List<iVisualElement> e)
	{
		this.e = e;
	}

	public void cascadedResize(iVisualElement on, Rect from, Rect to, int capture)
	{
		// TODO: other directions and some fuzz on this
		if (from.x!=to.x) return;
		if (from.y!=to.y) return;
		if (from.w!=to.w) return;

		for(iVisualElement ee : e)
		{
			if (ee==on) continue;
		
			Rect t = ee.getFrame(null);
			
			if (t.y>from.y+from.h && t.y<from.y+from.h+capture)
			{
				float d = (float) (to.h-from.h);
				Rect tt = new Rect(t);
				tt.y += d;
				
				resize(ee, t, tt, capture);
			}
			
		}
	}

	private void resize(iVisualElement ee, Rect to, Rect tt, int capture) {
		ee.getProperty(iVisualElement.overrides).shouldChangeFrame(ee, tt, to, true);
		cascadedResize(ee, to, tt, capture);
	}
	
}
