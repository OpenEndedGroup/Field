package field.core.windowing.components;

import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.math.linalg.Quaternion;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;

public class NSEWMarkingMenu {

	public enum Direction {
		north(new Vector2(0, 0), -0.25f, 0), south(new Vector2(1, 1), 0.25f, 0), east(new Vector2(1, 0), 0, 1), west(new Vector2(0, 1), 0.5f, -1);

		Vector2 d;
		float r;
		float textAlignment;
		Direction(Vector2 d, float r, float textAlignment) {
			this.d = d;
			this.r = r;
			this.textAlignment = textAlignment;
		}
	}
	
	HashMap<Direction, String> menus = new HashMap<Direction, String>();
	
	public List<CachedLine> paintNow(Rect center, HashSet<Direction> armed)
	{
		List<CachedLine>c = new ArrayList<CachedLine>();
		
		if (menus.size()==0)
			return c;
		
		for(Map.Entry<Direction, String> e : menus.entrySet())
		{
			Direction d = e.getKey();
			CachedLine frame = new CachedLine();
			iLine in = frame.getInput();

			float shim = 1;
			float size = 100;
			
			Vector2[] shape = new Vector2[]{new Vector2(0,0), new Vector2(size, -size/3), new Vector2(size, size+size/3), new Vector2(0, size), new Vector2(0,0)};
			Vector4[] color = new Vector4[]{armed.contains(d) ? new Vector4(0.5, 0, 0, 0.25f) :new  Vector4(1,1,1,0.1f), armed.contains(d) ? new Vector4(0.5, 0, 0, 0.25f) :  new Vector4(1,1,1,0.1f), new Vector4(), new Vector4(), new Vector4(0,0,0,0.25f)};
			
			Quaternion q = new Quaternion(d.r*Math.PI*2);
			int i = 0;
			for(Vector2 r : shape)
			{
				q.rotateVector(r);
				r.x+=size*d.d.x+center.x+center.w/2;
				r.y+=size*d.d.y+center.h+center.h/2;
				if (i==0)
					frame.getInput().moveTo(r.x, r.y);
				else
					frame.getInput().lineTo(r.x, r.y);
				frame.getInput().setPointAttribute(iLinearGraphicsContext.fillColor_v, color[i]);
				i++;
			}

			frame.getProperties().put(iLinearGraphicsContext.filled, true);
			frame.getProperties().put(iLinearGraphicsContext.stroked, false);
			
			CachedLine text = new CachedLine();
			Vector2 c1 = new Vector2().lerp(shape[0], shape[3], 0.5f);
			Vector2 c2 = new Vector2().lerp(shape[1], shape[2], 0.5f);
			Vector2 cc = new Vector2().lerp(c1, c2, 0.2f);
			text.getInput().moveTo(cc.x, cc.y);
			
			text.getInput().setPointAttribute(iLinearGraphicsContext.font_v, new Font("Gill Sans", Font.ITALIC, 10));
			text.getInput().setPointAttribute(iLinearGraphicsContext.text_v, "banana <"+d+">");
			text.getInput().setPointAttribute(iLinearGraphicsContext.alignment_v,d.textAlignment);
			
			text.getProperties().put(iLinearGraphicsContext.containsText, true);

			c.add(text);
			c.add(frame);
		}
		
		return c;
	}
	
}
