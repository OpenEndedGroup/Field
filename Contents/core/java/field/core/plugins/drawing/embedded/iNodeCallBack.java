/**
 * 
 */
package field.core.plugins.drawing.embedded;

import java.awt.event.MouseEvent;
import java.util.Map;

import field.bytecode.protect.annotations.GenerateMethods;
import field.bytecode.protect.annotations.Mirror;
import field.core.dispatch.iVisualElement;
import field.core.plugins.drawing.opengl.CachedLine;
import field.math.linalg.Vector2;

@GenerateMethods
public interface iNodeCallBack
{
	@Mirror
	public void mouseDown( CachedLine l, CachedLine.Event e, Vector2 at, MouseEvent ev);
	@Mirror
	public void mouseDragged( CachedLine l, CachedLine.Event e, Vector2 at, MouseEvent ev);
	@Mirror
	public void mouseUp( CachedLine l, CachedLine.Event e, Vector2 at, MouseEvent ev);

	@Mirror
	public void mouseClicked( CachedLine l, CachedLine.Event e, Vector2 at, MouseEvent ev);
	

	@Mirror
	public Map<String, Object> menu( CachedLine l, CachedLine.Event e, Vector2 at, MouseEvent ev);
}