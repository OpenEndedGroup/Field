package field.core.plugins.drawing.tweak.python;

import java.util.List;

import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.tweak.TweakSplineUI.SelectedVertex;
import field.namespace.generic.Generics.Pair;


public interface iNodeSelection {

	public List<Pair<SelectedVertex, Float>> selectFrom(List<CachedLine> here);
	
}
