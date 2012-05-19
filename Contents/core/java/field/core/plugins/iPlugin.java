package field.core.plugins;

import field.core.dispatch.iVisualElement;
import field.launch.iUpdateable;

public interface iPlugin extends iUpdateable{

	public void close();

	public Object getPersistanceInformation();

	public iVisualElement getWellKnownVisualElement(String id);

	public void registeredWith(iVisualElement root);
	public void setPersistanceInformation(Object o);

}
