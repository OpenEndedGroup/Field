package field.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import field.core.StandardFluidSheet;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;


public class RectangleAllocator {

	public LinkedHashMap<Object, Rect> rectangles = new LinkedHashMap<Object, Rect>();

	public enum Move {
		up, down, left, right;
	}

	public Rect allocate(Object token, Rect rect, Move move, float buffer) {
		rectangles.remove(token);
		boolean clear = false;
		while (!clear) {
			clear = true;
			Iterator<Rect> i = rectangles.values().iterator();
			while (i.hasNext()) {
				Rect r = i.next();
				if (r.overlaps(rect)) {
					switch (move) {
					case up:
						rect.y = r.y - rect.h - buffer;
						clear = false;
						break;
					case down:
						rect.y = r.y + r.h + buffer;
						clear = false;
						break;
					case left:
						rect.x = r.x - rect.w - buffer;
						clear = false;
						break;
					case right:
						rect.x = r.x + r.w + buffer;
						clear = false;
						break;
					}
				}
			}
		}
		rectangles.put(token, rect);
		return rect;
	}

	static public Rect allocateOnSheet(iVisualElement root, Rect rect, Move move, float buffer)
	{
		RectangleAllocator allocator = new RectangleAllocator();
		List<iVisualElement> all = StandardFluidSheet.allVisualElements(root);
		for(iVisualElement e : all)
		{
			Rect ff = e.getFrame(null);
			if (ff!=null)
				allocator.rectangles.put(e.getUniqueID(), ff);
		}
		return allocator.allocate("!!", rect, move, buffer);
	}

}
