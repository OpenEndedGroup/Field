package field.core.plugins.drawing.tweak;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import field.core.dispatch.iVisualElement;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLine_m;
import field.core.plugins.drawing.tweak.AbsoluteTool.AbsoluteNodeDescription;
import field.core.plugins.drawing.tweak.TweakSplineCodeGen.BaseTool;
import field.core.plugins.drawing.tweak.TweakSplineCodeGen.iCoordDesc;
import field.core.plugins.drawing.tweak.TweakSplineCodeGen.iResult;
import field.core.plugins.drawing.tweak.TweakSplineUI.MouseInfo;
import field.core.plugins.drawing.tweak.TweakSplineUI.SelectedVertex;
import field.core.plugins.drawing.tweak.TweakSplineUI.SubSelection;

public class NodeVisitorTool extends BaseTool{

	private final Class< ?> wrClass;

	public class ApplyToolCoordinateChange implements iCoordDesc {
		public iResult describe(List<CachedLine> index, List<SelectedVertex> claimed, MouseInfo mi) {
			if (claimed.size()==0) return null;
			
			final SelectedVertex c = claimed.remove(0);
		
			String[] parts = wrClass.getName().split("[\\.\\$]");
			final String name = parts[parts.length-1];
			
			int[] fmask= { 0,0,0};
			if (c.whatSelected.contains(SubSelection.previousControl) || !c.vertex.method.equals(iLine_m.cubicTo_m)){
				fmask[0] = 1;
			}
			if (c.whatSelected.contains(SubSelection.postion)){
				fmask[1] = 1;
			}
			if (c.whatSelected.contains(SubSelection.nextControl) || (c.vertexIndex==c.onLine.events.size()-1 || !c.onLine.events.get(c.vertexIndex+1).method.equals(iLine_m.cubicTo_m))){
				fmask[2] = 1;
			}

			if (fmask[0] == 1 && fmask[1] == 1 && fmask[2] == 1)
				fmask = null;
			
			final int[] mask = fmask;
			
			return new iResult(){
				public List<SelectedVertex> getClaimedVertex() {
					return Collections.singletonList(c);
				}

				public String toExpression() {
					return "ApplyTool(\"" + name+"\""+(mask == null ? ")" : ", "+mask[0]+", "+mask[1]+", "+mask[2]+")");
				}

				public void toProperties(iVisualElement e, Map<String, Object> soFar) {
				}
			};
		}
	}
	
	

	public NodeVisitorTool(Class wrClass)
	{
		this.wrClass = wrClass;
		
		nodes.add(new AbsoluteNodeDescription());
		coords.add(new ApplyToolCoordinateChange());
	}
}
