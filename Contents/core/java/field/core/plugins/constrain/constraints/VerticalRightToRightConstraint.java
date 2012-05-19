package field.core.plugins.constrain.constraints;

import field.core.plugins.constrain.ComplexConstraints;
import field.core.plugins.constrain.ComplexConstraints.VariablesForRect;
import field.core.plugins.constrain.cassowary.ClLinearEquation;
import field.core.plugins.constrain.cassowary.ClLinearExpression;
import field.math.linalg.Vector3;

public class VerticalRightToRightConstraint extends VerticalLeftToLeftConstraint{

	@Override
	protected ClLinearEquation createConstraint(VariablesForRect vLeft, VariablesForRect vRight) {
		return new ClLinearEquation(ClLinearExpression.Plus(vLeft.variableX, new ClLinearExpression(vLeft.variableW)), ClLinearExpression.Plus(vRight.variableX, new ClLinearExpression(vRight.variableW)));
	}
	
	@Override
	protected Vector3 point1(VariablesForRect vLeft) {
		return new Vector3(vLeft.variableX.value()+vLeft.variableW.value(), vLeft.variableY.value()+vLeft.variableH.value()/2,0);
	}

	@Override
	protected Vector3 point2(VariablesForRect vRight) {
		return new Vector3(vRight.variableX.value()+vRight.variableW.value(), vRight.variableY.value()+vRight.variableH.value()/2,0);
	}
}
