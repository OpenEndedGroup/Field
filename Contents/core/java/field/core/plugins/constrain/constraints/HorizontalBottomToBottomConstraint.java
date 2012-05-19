package field.core.plugins.constrain.constraints;

import field.core.plugins.constrain.ComplexConstraints.VariablesForRect;
import field.core.plugins.constrain.cassowary.ClLinearEquation;
import field.core.plugins.constrain.cassowary.ClLinearExpression;
import field.math.linalg.Vector3;

public class HorizontalBottomToBottomConstraint extends HorizontalTopToTopConstraint{

	@Override
	protected ClLinearEquation createConstraint(VariablesForRect vLeft, VariablesForRect vRight) {
		return new ClLinearEquation(ClLinearExpression.Plus(vLeft.variableY, new ClLinearExpression(vLeft.variableH)), ClLinearExpression.Plus(vRight.variableY, new ClLinearExpression(vRight.variableH)));
	}

	@Override
	protected Vector3 point1(VariablesForRect vLeft) {
		return new Vector3(vLeft.variableX.value()+vLeft.variableW.value()/2, vLeft.variableY.value()+vLeft.variableH.value(),0);
	}

	@Override
	protected Vector3 point2(VariablesForRect vRight) {
		return new Vector3(vRight.variableX.value()+vRight.variableW.value()/2, vRight.variableY.value()+vRight.variableH.value(),0);
	}
}
