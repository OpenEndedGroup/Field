package field.graphics.ci;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.util.PythonCallableMap;
import field.math.graph.GraphNodeSearching.VisitCode;

public class DrawImageOverride extends field.core.dispatch.iVisualElementOverrides.DefaultOverride {

	VisualElementProperty<PythonCallableMap> images = new VisualElementProperty<PythonCallableMap>("images_");

	public DrawImageOverride() {
	}

	@Override
	public DefaultOverride setVisualElement(iVisualElement ve) {
		ve.setProperty(images, new PythonCallableMap());
		super.setVisualElement(ve);
		return this;
	}
	
	@Override
	public <T> VisitCode getProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> ref) {
		if (source == forElement)
		{
			if (forElement.getProperty(images)==null)				
				forElement.setProperty(images, new PythonCallableMap());
		}
		return super.getProperty(source, prop, ref);
	}

	@Override
	public VisitCode paintNow(iVisualElement source, Rect bounds, boolean visible) {
		;//System.out.println(" ? ");
		if (source == forElement) {
			PythonCallableMap m = images.get(source);
			if (m != null) {
				m.invoke();
			}
		}
		return super.paintNow(source, bounds, visible);
	}
}
