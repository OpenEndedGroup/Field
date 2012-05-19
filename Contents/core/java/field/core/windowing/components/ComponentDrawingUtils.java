package field.core.windowing.components;

import field.core.dispatch.iVisualElement.Rect;
import field.graphics.core.Base;
import field.graphics.dynamic.DynamicLine;
import field.graphics.dynamic.DynamicPointlist;
import field.graphics.dynamic.iDynamicMesh;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;

public class ComponentDrawingUtils {

	public static void diagonalStripe(Rect bounds, DynamicLine line, Vector4 color, int pitch) {
		line.open();

		int start = (int)(bounds.x/pitch);
		int end = (int)((bounds.x+2*Math.max(bounds.w, bounds.h))/pitch);

		for(int i=start;i<end;i++)
		{
			float rawStartX= i*pitch;
			float rawStartY = (float) bounds.y;

			// now we need to intersect rawStartX, rawStartY and the line -1, +1 with the rect

			float l1 = (float) ((bounds.x+bounds.w-rawStartX)/(-1));
			if (l1<0) l1 = 0;

			float l2 = (float) ((bounds.x-rawStartX)/(-1));
			float l3 = (float) ((bounds.y+bounds.h-rawStartY)/(1));
			if (l3<l2) l2 = l3;

			drawLine(line, rawStartX, rawStartY, -1, 1, l1, l2, color);

		}

		line.close();
	}

	static public void drawRectangle(iDynamicMesh triangle, DynamicLine line, DynamicPointlist point, float x, float y, float w, float h, Vector4 triangleColor,
				Vector4 lineColor) {

		if (line != null) {
			line.open();

			line.beginSpline(null);

			line.setAuxOnSpline(Base.color0_id, lineColor.x, lineColor.y, lineColor.z, lineColor.w);
			line.setAuxOnSpline(4, 1,1,1,1);
			line.moveTo(new Vector3(x, y-0f, 0));
			line.setAuxOnSpline(Base.color0_id, lineColor.x, lineColor.y, lineColor.z, lineColor.w);
			line.setAuxOnSpline(4, 1,1,1,1);
			line.lineTo(new Vector3(x + w+0f, y-0f, 0));
			line.setAuxOnSpline(Base.color0_id, lineColor.x, lineColor.y, lineColor.z, lineColor.w);
			line.setAuxOnSpline(4, 1,1,1,1);
			line.lineTo(new Vector3(x + w+0f, y + h, 0));
			line.setAuxOnSpline(Base.color0_id, lineColor.x, lineColor.y, lineColor.z, lineColor.w);
			line.setAuxOnSpline(4, 1,1,1,1);
			line.lineTo(new Vector3(x, y + h, 0));
			line.setAuxOnSpline(Base.color0_id, lineColor.x, lineColor.y, lineColor.z, lineColor.w);
			line.setAuxOnSpline(4, 1,1,1,1);
			line.lineTo(new Vector3(x, y-0f, 0));
			line.setAuxOnSpline(Base.color0_id, lineColor.x, lineColor.y, lineColor.z, lineColor.w);
			line.setAuxOnSpline(4, 1,1,1,1);
			line.endSpline();


			line.close();
		}
		if (triangle != null) {
			triangle.open();

			int v0 = triangle.nextVertex(new Vector3(x, y, 0));
			int v1 = triangle.nextVertex(new Vector3(x + w, y, 0));
			int v2 = triangle.nextVertex(new Vector3(x + w, y + h, 0));
			int v3 = triangle.nextVertex(new Vector3(x, y + h, 0));

			triangle.nextFace(v0, v1, v2);
			triangle.nextFace(v0, v2, v3);

			triangle.setAux(v0, Base.color0_id, triangleColor.x, triangleColor.y, triangleColor.z, triangleColor.w*2);
			triangle.setAux(v1, Base.color0_id, triangleColor.x, triangleColor.y, triangleColor.z, triangleColor.w*1.5f);
			triangle.setAux(v2, Base.color0_id, triangleColor.x, triangleColor.y, triangleColor.z, triangleColor.w*0.2f);
			triangle.setAux(v3, Base.color0_id, triangleColor.x, triangleColor.y, triangleColor.z, triangleColor.w*0.6f);

			triangle.setAux(v0, 4, 1.0f, 1.0f, 1.0f, 1.0f);
			triangle.setAux(v1, 4, 1.0f, 1.0f, 1.0f, 1.0f);
			triangle.setAux(v2, 4, 1.0f, 1.0f, 1.0f, 1.0f);
			triangle.setAux(v3, 4, 1.0f, 1.0f, 1.0f, 1.0f);

			triangle.close();
		}
		if (point != null) {
			point.open();
			int v0 = point.nextVertex(new Vector3(x, y, 0));
			int v1 = point.nextVertex(new Vector3(x + w, y, 0));
			int v2 = point.nextVertex(new Vector3(x + w, y + h, 0));
			int v3 = point.nextVertex(new Vector3(x, y + h, 0));

			point.setAux(v0, Base.color0_id, lineColor.x, lineColor.y, lineColor.z, lineColor.w / 2);
			point.setAux(v1, Base.color0_id, lineColor.x, lineColor.y, lineColor.z, lineColor.w / 2);
			point.setAux(v2, Base.color0_id, lineColor.x, lineColor.y, lineColor.z, lineColor.w / 2);
			point.setAux(v3, Base.color0_id, lineColor.x, lineColor.y, lineColor.z, lineColor.w / 2);

			point.setAux(v0, 4, 1.0f, 1.0f, 1.0f, 1.0f);
			point.setAux(v1, 4, 1.0f, 1.0f, 1.0f, 1.0f);
			point.setAux(v2, 4, 1.0f, 1.0f, 1.0f, 1.0f);
			point.setAux(v3, 4, 1.0f, 1.0f, 1.0f, 1.0f);

			point.close();
		}
	}

	private static void drawLine(DynamicLine line, float sx, float sy, float mx, float my, float l1, float l2, Vector4 color) {
		if (l2<=l1) return;
		int v1 = line.nextVertex(new Vector3(sx+l1*mx,sy+l1*my, 0));
		int v2 = line.nextVertex(new Vector3(sx+l2*mx,sy+l2*my, 0));
		line.nextFace(v1, v2);
		line.setAux(v1, Base.color0_id, color.x, color.y, color.z, color.w);
		line.setAux(v2, Base.color0_id, color.x, color.y, color.z, color.w);

	}

}
