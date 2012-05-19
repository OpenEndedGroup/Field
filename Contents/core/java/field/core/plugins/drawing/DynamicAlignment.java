package field.core.plugins.drawing;

import java.util.LinkedHashMap;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.OfferedAlignment.iDrawable;
import field.core.plugins.drawing.OfferedAlignment.iOffering;
import field.core.plugins.drawing.align.PointOffering.BaseDrawable;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.windowing.GLComponentWindow;

/**
 * allows an element to offer up alignment options dynamically
 * 
 * @author marc
 * 
 */
@Woven
public abstract class DynamicAlignment  implements iOffering {

	public DynamicAlignment() {
	}

	public iDrawable drawableForDict(final AnAlignment align) {
		if (align == null)
			return null;
		if (align.score == 0)
			return null;
		return new BaseDrawable(align.name, align.score, null, null) {
			@Override
			protected void drawWithOpacity(OfferedAlignment alignment, float alpha) {
				System.out.println(" drawing <"+align.toDraw+">");

				// set opacity, and make sure to resbumit
				if (align.toDraw != null) {

					for (CachedLine c : align.toDraw) {
						c.getProperties().put(iLinearGraphicsContext.totalOpacity, alpha);
						if (GLComponentWindow.fastContext!=null)
							GLComponentWindow.fastContext.resubmitLine(c, c.getProperties());
						else
							GLComponentWindow.currentContext.resubmitLine(c, c.getProperties());
					}
				}
			}

			@Override
			public void process(Rect currentrect, Rect newRect) {

				System.out.println(" process <"+currentrect+", "+newRect+", "+align.newRect+" "+align.score+" "+align.name+">");

				newRect.setValue(align.newRect);
				
				deferredFinish(align);
			}

			@Override
			public void update(Rect currentrect, Rect newRect) {
				
				System.out.println(" update <"+currentrect+", "+newRect+", "+align.newRect+" "+align.score+" "+align.name+">");
				
				newRect.setValue(align.newRect);

			}
			
			@Override
			public iDrawable merge(iDrawable d) {
				((BaseDrawable)d).alpha = this.alpha;
			
				return d;
			}

		};
	}

	@NextUpdate
	protected void deferredFinish(AnAlignment align) {
		finish(align);
	}
	
	protected void finish(AnAlignment align)
	{
	}
	

	public void createConstraint(iVisualElement root, LinkedHashMap<iVisualElement, Rect> current, iVisualElement element, Rect or, Rect originalRect, Rect currentRect) {
	}

	public abstract iDrawable score(LinkedHashMap<iVisualElement, Rect> current, iVisualElement element, Rect originalRect, Rect currentRect, Rect newRect);
}
