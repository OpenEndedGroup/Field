package field.core.plugins.constrain.constraints;

import java.util.Map;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.constrain.BaseConstraintOverrides;
import field.core.plugins.constrain.ComplexConstraints;
import field.core.plugins.constrain.ComplexConstraints.VariablesForRect;
import field.core.plugins.constrain.cassowary.ClConstraint;
import field.core.plugins.constrain.cassowary.ClLinearEquation;
import field.core.plugins.constrain.cassowary.ClLinearExpression;
import field.core.plugins.constrain.cassowary.ExCLNonlinearExpression;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.iComponent;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;


public class VerticalLeftToBlendLeftConstraint extends BaseConstraintOverrides {

	static public final VisualElementProperty<Float> verticalLeftToBlendLeftConstraint_constraintBlend = new VisualElementProperty<Float>("verticalLeftToBlendLeftConstraint_constraintBlend");

	private Boolean isSelected() {
		iComponent v = forElement.getProperty(iVisualElement.localView);
		if (v!=null)
			return v.isSelected();
		return false;
	}

	@Override
	protected ClConstraint createConstraint(Map<String, iVisualElement> property) {

		ComplexConstraints cc = getComplexConstraintsPlugin();
		assert cc != null;
		if (cc == null) return null;
		iVisualElement left = property.get("left");
		iVisualElement rightA = property.get("rightA");
		iVisualElement rightB = property.get("rightB");
		assert left != null : property;
		assert rightA != null : property;

		if (left == null || rightA == null || rightB == null) return null;

		Float f = verticalLeftToBlendLeftConstraint_constraintBlend.get(forElement);
		if (f == null) f = 0f;

		VariablesForRect vLeft = cc.getVariablesFor(left);
		VariablesForRect vRightA = cc.getVariablesFor(rightA);
		VariablesForRect vRightB = cc.getVariablesFor(rightB);

		return createConstraint(vLeft, vRightA, vRightB, f);
	}
	protected ClLinearEquation createConstraint(VariablesForRect vLeft, VariablesForRect vRightA, VariablesForRect rightB, float alpha) {
		try {
			return new ClLinearEquation(vLeft.variableX, ClLinearExpression.Times(vRightA.variableX, 1-alpha).addExpression(ClLinearExpression.Times(rightB.variableX, alpha)));
//			return new ClLinearEquation(vLeft.variableX, ClLinearExpression.Times(rightB.variableX, 1.0));
		} catch (ExCLNonlinearExpression e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void paint(Rect bounds, boolean visible) {
		ComplexConstraints cc = getComplexConstraintsPlugin();
		assert cc != null;
		if (cc == null) return;

		CachedLine cl = new CachedLine();
		iLine  line = cl.getInput();

		Map<String, iVisualElement> parameters = getConstraintParameters();
		iVisualElement left = parameters.get("left");
		iVisualElement rightA = parameters.get("rightA");
		iVisualElement rightB = parameters.get("rightB");
		assert left != null : parameters;
		assert rightA != null : parameters;

		if (left == null || rightA == null || rightB == null) return;
		VariablesForRect vLeft = cc.getVariablesFor(left);
		VariablesForRect vRightA = cc.getVariablesFor(rightA);
		VariablesForRect vRightB = cc.getVariablesFor(rightB);

		Vector3 p1 = point1(vLeft);
		Vector3 p2 = point2(vRightA, vRightB);

		line.moveTo(p1.x, p1.y);
		line.lineTo(p1.x-5, p1.y+(p2.y-p1.y)*0.1f);
		line.lineTo(p1.x-5, p1.y+(p2.y-p1.y)*0.9f);
		line.lineTo(p2.x, p2.y);
		line.lineTo(p1.x+5, p1.y+(p2.y-p1.y)*0.9f);
		line.lineTo(p1.x+5, p1.y+(p2.y-p1.y)*0.1f);
		line.lineTo(p1.x, p1.y);

		cl.getProperties().put(iLinearGraphicsContext.color, new Vector4(0.25, 0, 0, 0.15f));
		cl.getProperties().put(iLinearGraphicsContext.thickness, 0.5f);
		cl.getProperties().put(iLinearGraphicsContext.filled, true);

		cl.getProperties().put(iLinearGraphicsContext.shouldHighlight, isSelected());

		iLinearGraphicsContext context =GLComponentWindow.currentContext;
		context.submitLine(cl, cl.getProperties());
	}

	protected Vector3 point1(VariablesForRect vLeft) {
		return new Vector3(vLeft.variableX.value(), vLeft.variableY.value() + vLeft.variableH.value() / 2, 0);
	}

	protected Vector3 point2(VariablesForRect vRight, VariablesForRect rightB) {
		Vector3 v1 = new Vector3(vRight.variableX.value(), vRight.variableY.value() + vRight.variableH.value() / 2, 0);
		Vector3 v2 = new Vector3(rightB.variableX.value(), rightB.variableY.value() + rightB.variableH.value() / 2, 0);
		Float f = verticalLeftToBlendLeftConstraint_constraintBlend.get(forElement);
		if (f == null) f = 0f;
		return v1.lerp(v1, v2, f);
	}
}
