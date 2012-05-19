/**
 * 
 */
package field.core.plugins.drawing;

import java.util.List;

import field.bytecode.protect.Notable;
import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.util.BetterPythonConstructors.SynthesizeFactory;

@Notable
@SynthesizeFactory
public class AnAlignment {
	public Rect newRect;
	public String name;
	public List<CachedLine> toDraw;
	public float score = 1;
}