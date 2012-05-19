package field.core.plugins.constrain.constraints;

import field.core.plugins.constrain.ComplexConstraints;
import field.core.plugins.constrain.ComplexConstraints.VariablesForRect;
import field.core.plugins.constrain.cassowary.ClLinearEquation;
import field.core.plugins.constrain.cassowary.ClLinearExpression;
import field.core.plugins.constrain.cassowary.ExCLNonlinearExpression;
import field.math.linalg.Vector3;

public class VerticalRightToBlendLeftConstraint extends VerticalLeftToBlendLeftConstraint{
	
	@Override
	protected ClLinearEquation createConstraint(VariablesForRect vLeft, VariablesForRect vRightA, VariablesForRect rightB, float alpha) {
		try {
			return new ClLinearEquation(ClLinearExpression.Plus(new ClLinearExpression(vLeft.variableX), new ClLinearExpression(vLeft.variableW)), ClLinearExpression.Times(vRightA.variableX, 1-alpha).addExpression(ClLinearExpression.Times(rightB.variableX, alpha)));
//			return new ClLinearEquation(vLeft.variableX, ClLinearExpression.Times(rightB.variableX, 1.0));
		} catch (ExCLNonlinearExpression e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	@Override
	protected Vector3 point1(VariablesForRect vLeft) {
		return new Vector3(vLeft.variableX.value()+vLeft.variableW.value(), vLeft.variableY.value() + vLeft.variableH.value() / 2, 0);
	}
	@Override
	protected Vector3 point2(VariablesForRect vRight, VariablesForRect rightB) {
		Vector3 v1 = new Vector3(vRight.variableX.value(), vRight.variableY.value() + vRight.variableH.value() / 2, 0);
		Vector3 v2 = new Vector3(rightB.variableX.value(), rightB.variableY.value() + rightB.variableH.value() / 2, 0);
		Float f = verticalLeftToBlendLeftConstraint_constraintBlend.get(forElement);
		if (f == null) f = 0f;
		return v1.lerp(v1, v2, f);
	}
}
