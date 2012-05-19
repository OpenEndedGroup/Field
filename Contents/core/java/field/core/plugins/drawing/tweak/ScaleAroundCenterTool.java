package field.core.plugins.drawing.tweak;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import field.core.dispatch.iVisualElement;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.tweak.TweakSplineCodeGen.BaseTool;
import field.core.plugins.drawing.tweak.TweakSplineCodeGen.iCoordDesc;
import field.core.plugins.drawing.tweak.TweakSplineCodeGen.iNodeDesc;
import field.core.plugins.drawing.tweak.TweakSplineCodeGen.iResult;
import field.core.plugins.drawing.tweak.TweakSplineUI.MouseInfo;
import field.core.plugins.drawing.tweak.TweakSplineUI.SelectedVertex;
import field.core.plugins.drawing.tweak.TweakSplineUI.SubSelection;


public class ScaleAroundCenterTool extends BaseTool {
	static public class AllNodesDescription implements iNodeDesc {
		public List<iResult> describe(List<CachedLine> index, List<SelectedVertex> claimed) {
			if (claimed.size() == 0) return null;

			final ArrayList<SelectedVertex> cclaimed = new ArrayList<SelectedVertex>(claimed);
			claimed.clear();

			String expression = "";
			for (int i = 0; i < cclaimed.size(); i++) {
				int ind = index.indexOf(cclaimed.get(i).onLine);
				int v = cclaimed.get(i).vertexIndex;

				int code = 0;
				if (cclaimed.get(i).whatSelected.contains(SubSelection.previousControl)) code += 1;
				if (cclaimed.get(i).whatSelected.contains(SubSelection.postion)) code += 2;
				if (cclaimed.get(i).whatSelected.contains(SubSelection.nextControl)) code += 4;

				expression = expression + ind + "," + v + "," + code + ", ";
			}
			final String fexpression = expression;

			iResult r = new iResult(){
				public List<SelectedVertex> getClaimedVertex() {
					return cclaimed;
				}

				public String toExpression() {
					return "DirectMultiple( (" + fexpression + "))";
				}

				public void toProperties(iVisualElement e, Map<String, Object> soFar) {
				}
			};
			return Collections.singletonList(r);
		}
	}

	static public class ScaleAroundCenterChange implements iCoordDesc {

		String toolname = "ScaleAroundCenter";

		public ScaleAroundCenterChange() {
		}

		public ScaleAroundCenterChange(String toolname) {
			this.toolname = toolname;
		}

		public iResult describe(List<CachedLine> index, List<SelectedVertex> claimed, final MouseInfo mi) {
			if (claimed.size() == 0) return null;
			final ArrayList<SelectedVertex> cclaimed = new ArrayList<SelectedVertex>(claimed);
			claimed.clear();

			return new iResult(){
				public List<SelectedVertex> getClaimedVertex() {
					return cclaimed;
				}

				public String toExpression() {
					return toolname + "(" + mi.lastOx + ", " + mi.lastOy + ", " + mi.lastOdx + ", " + mi.lastOdy + ")";
				}

				public void toProperties(iVisualElement e, Map<String, Object> soFar) {
				}
			};
		}
	}

	public ScaleAroundCenterTool() {
		nodes.add(new AllNodesDescription());
		coords.add(new ScaleAroundCenterChange());
	}

	public ScaleAroundCenterTool(iNodeDesc nd, iCoordDesc cd) {
		nodes.add(nd);
		coords.add(cd);
	}

	public ScaleAroundCenterTool(String toolname) {
		nodes.add(new AllNodesDescription());
		coords.add(new ScaleAroundCenterChange(toolname));
	}

}
