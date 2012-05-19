package field.core.execution;

import field.core.dispatch.Mixins;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.execution.PythonScriptingSystem.Promise;
import field.math.graph.GraphNodeSearching.VisitCode;

public class LocalTimeMixin extends field.core.dispatch.iVisualElementOverrides.DefaultOverride {

	static public void mixin(iVisualElement e) {
		new Mixins().mixInOverride(LocalTimeMixin.class, e);
	}

	public VisualElementProperty<PythonScriptingSystem> localScriptingSystem = new VisualElementProperty<PythonScriptingSystem>("localPSS_");

	@Override
	public <T> VisitCode getProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> ref) {
		if (isChild(source)) {
			if (prop.equals(PythonScriptingSystem.pythonScriptingSystem)) {
				PythonScriptingSystem overrides = forElement.getProperty(localScriptingSystem);

				if (overrides == null) {
					overrides = makeLocalPSS();
					if (overrides != null) {
						forElement.setProperty(localScriptingSystem, overrides);
					}
				}

				if (overrides != null) {

					PythonScriptingSystem parent = PythonScriptingSystem.pythonScriptingSystem.get(forElement);
					Promise oldPromise = parent.revokePromise(source);
					if (oldPromise != null) {
						overrides.promisePythonScriptingElement(source, oldPromise);
					}
					ref.set((T) overrides);
					return VisitCode.stop;
				}
			}
		}
		return super.getProperty(source, prop, ref);
	}

	private boolean isChild(iVisualElement source) {
		return forElement.getParents().contains(source);
	}

	protected PythonScriptingSystem makeLocalPSS() {
		return new PythonScriptingSystem();
	}

}
