package field.core.execution;

import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.execution.PythonScriptingSystem.Promise;
import field.math.abstraction.iFloatProvider;

public interface iExecutesPromise {

	static public final VisualElementProperty<iExecutesPromise> promiseExecution = new VisualElementProperty<iExecutesPromise>("promiseExecution_");

	public abstract void addActive(iFloatProvider timeProvider, Promise p);

	public abstract void removeActive(Promise p);

	public void stopAll(float t);
	
}