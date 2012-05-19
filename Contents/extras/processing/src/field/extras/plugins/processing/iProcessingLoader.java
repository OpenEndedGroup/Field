package field.extras.plugins.processing;

import field.core.execution.iExecutesPromise;
import field.core.ui.text.PythonTextEditor.EditorExecutionInterface;
import field.launch.iUpdateable;

public interface iProcessingLoader extends  iUpdateable{

	public void close();
	public EditorExecutionInterface getEditorExecutionInterface(EditorExecutionInterface delegateTo);
	public iExecutesPromise getExecutesPromise(iExecutesPromise delegateTo);

	public void init();
	public void injectIntoGlobalNamespace();
	public void setOntop(Boolean s);
}
