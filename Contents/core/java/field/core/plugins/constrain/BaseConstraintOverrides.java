package field.core.plugins.constrain;

import java.util.Map;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.constrain.cassowary.ClConstraint;
import field.math.graph.GraphNodeSearching.VisitCode;


public abstract class BaseConstraintOverrides extends iVisualElementOverrides.DefaultOverride {

	static public final VisualElementProperty<Map<String, iVisualElement>> constraintParameters = new VisualElementProperty<Map<String, iVisualElement>>("constraintParameters");

	private ComplexConstraints cachedComplex;

	boolean constraintsHaveChanged = false;


	@Override
	public VisitCode paintNow(iVisualElement source, Rect bounds, boolean visible) {
		ensureConstraint();
		if (source == this.forElement) {
			paint(bounds, visible);
		}

		return super.paintNow(source, bounds, visible);
	}

	@Override
	public <T> VisitCode setProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> to) {
		if (source == forElement && prop.equals(constraintParameters))
		{
			constraintsHaveChanged= true;
			forElement.setProperty(iVisualElement.dirty, true);
		}
		return super.setProperty(source, prop, to);
	}

	abstract protected ClConstraint createConstraint(Map<String, iVisualElement> property) ;

	protected void ensureConstraint()
	{
		ComplexConstraints cc= getComplexConstraintsPlugin();
		if (cc!=null || constraintsHaveChanged)
		{
			ClConstraint constraint = cc.getConstraintForElement(forElement);
			if (constraint==null || constraintsHaveChanged)
			{
				constraint = createConstraint(getConstraintParameters());

				assert constraint!=null;

				cc.setConstraintForElement(forElement, constraint);
			}
			constraintsHaveChanged = false;
		}
	}
	protected ComplexConstraints getComplexConstraintsPlugin() {
		if (cachedComplex != null) return cachedComplex;
		ComplexConstraints c1 = ComplexConstraints.complexConstraints_plugin.get(forElement);
		return cachedComplex = c1;
	}

	protected Map<String, iVisualElement> getConstraintParameters() {
		return forElement.getProperty(constraintParameters);
	}

	abstract protected void paint(Rect bounds, boolean visible) ;




}