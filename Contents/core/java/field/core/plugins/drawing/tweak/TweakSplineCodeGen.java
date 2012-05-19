package field.core.plugins.drawing.tweak;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.drawing.tweak.TweakSplineUI.MouseInfo;
import field.core.plugins.drawing.tweak.TweakSplineUI.SelectedVertex;
import field.launch.iUpdateable;


/**
 * the task here is to take a set of selection verticies that go from "frozen" to "abs" and make the (python) code (and invisible properties) that perform this task
 * 
 * @author marc
 * 
 */
public class TweakSplineCodeGen {

	// identifies a node in a spline
	public interface iNodeDesc {
		// mutates input "selection" to grab the ones that it recognizes fully
		public List<iResult> describe(List<CachedLine> index, List<SelectedVertex> selection);
	}

	// describes the transformation of a group of verticies to another group (from "frozen" to "abs")
	public interface iCoordDesc {
		// mutates input "claimed" to grab the ones that it recognizes fully
		public iResult describe(List<CachedLine> index, List<SelectedVertex> claimed, MouseInfo mi);
	}

	public interface iResult {
		public List<SelectedVertex> getClaimedVertex();

		public String toExpression();

		public void toProperties(iVisualElement e, Map<String, Object> soFar);
	}

	static public final iResult abort = new iResult(){

		public List<SelectedVertex> getClaimedVertex() {
			return null;
		}

		public String toExpression() {
			return null;
		}

		public void toProperties(iVisualElement e, Map<String, Object> soFar) {
		}
	};

	/**
	 * each node desc gets an expression
	 * 
	 * @author marc
	 * 
	 */
	static public class BaseTool {
		List<iNodeDesc> nodes = new ArrayList<iNodeDesc>();

		List<iCoordDesc> coords = new ArrayList<iCoordDesc>();

		String toolExpressionName = "baseTool";
		
		Map<String, String> extraArguments = new HashMap<String, String>();
		
		public Map<String, String> getExtraArguments() {
			return extraArguments;
		}

		public iResult obtainExpressions(iVisualElement e, List<CachedLine> index, final List<SelectedVertex> selected, MouseInfo mi) {

			ArrayList<SelectedVertex> toClaim = new ArrayList<SelectedVertex>(selected);
			
			for(SelectedVertex v : new ArrayList<SelectedVertex>(selected))
			{
				if (v.vertex.getDict().isTrue(iLinearGraphicsContext.noTweak_v, false))
					toClaim.remove(v);
			}
			
			ArrayList<iResult> nodeDescriptions = new ArrayList<iResult>();
			for (int i = nodes.size() - 1; i >= 0; i--) {
				List<iResult> res = null;
				do {
					res = nodes.get(i).describe(index, toClaim);
					if (res != null) nodeDescriptions.addAll(res);
				} while (res != null);
			}
			assert toClaim.size() == 0 : "unclaimed node descriptions " + toClaim + ":" + selected + ":" + nodeDescriptions;

			String expression = "";
			final Map<String, Object> property = new HashMap<String, Object>();

			for (int i = 0; i < nodeDescriptions.size(); i++) {
				iResult nd = nodeDescriptions.get(i);
				ArrayList<SelectedVertex> ndToClaim = new ArrayList<SelectedVertex>(nd.getClaimedVertex());
				ArrayList<iResult> coordDescriptions = new ArrayList<iResult>();

				boolean aborted = false;

				outer: for (int j = coords.size() - 1; j >= 0; j--) {
					iResult res = null;
					do {
						res = coords.get(j).describe(index, ndToClaim, mi);
						if (res == abort) {
							nodeDescriptions.remove(i);
							i--;
							aborted = true;
							break outer;
						}
						if (res != null) coordDescriptions.add(res);
					} while (res != null);
				}

				if (!aborted) {
					assert ndToClaim.size() == 0 : "unclaimed coord transformations " + ndToClaim + ":" + selected + ":" + nodeDescriptions + "  : " + nd;

					expression += assembleExpression(e, nd, coordDescriptions, property, mi) + "\n";
				}
			}
			final String fexpression = expression;
			return new iResult(){
				public List<SelectedVertex> getClaimedVertex() {
					return new ArrayList<SelectedVertex>(selected);
				}

				public String toExpression() {
					return fexpression;
				}

				public void toProperties(iVisualElement e, Map<String, Object> soFar) {
					soFar.putAll(property);
				}
			};
		}

		public String getCoordinateDescription(List<CachedLine> index, final List<SelectedVertex> selected, MouseInfo mi) {
			ArrayList<SelectedVertex> sel = new ArrayList<SelectedVertex>(selected);
			String ex = "";
			for (int i = 0; i < coords.size(); i++) {
				iResult r = coords.get(i).describe(index, sel, mi);
				if (r != null) {
					String e = r.toExpression();
					ex += e + ",";
				}
			}

			final String fex = "(" + ex + ")";
			return fex;
		}

		public String assembleExpression(iVisualElement e, iResult nd, ArrayList<iResult> coordDescriptions, Map<String, Object> property, MouseInfo mi) {

			String exp = toolExpressionName + "( ";

			exp += "node=" + nd.toExpression() + ", ";

			nd.toProperties(e, property);

			if (coordDescriptions.size() > 0) {
				exp += "coords=(";
				for (int i = 0; i < coordDescriptions.size(); i++) {
					exp += coordDescriptions.get(i).toExpression() + ", ";
					coordDescriptions.get(i).toProperties(e, property);
				}
				exp += ")";
			}

			Set<Entry<String, String>> extra = extraArguments.entrySet();
			Iterator<Entry<String, String>> ee = extra.iterator();
			while(ee.hasNext())
			{
				Entry<String, String> ii = ee.next();
				exp+=","+ii.getKey()+"="+ii.getValue();
			}
			
			
			exp += " )";
			return exp;
		}

		public void populateParameters(iVisualElement inside, iUpdateable continuation) {
			continuation.update();
		}

	}

	static public String uniqProperty(iVisualElement e, Map<String, Object> p) {
		String base = "_tweakProperty";
		int n = 0;
		while (e.getProperty(new VisualElementProperty(base + n)) != null || (p!=null && p.containsKey(base + n)))
			n++;
		return base + n;
	}

}
