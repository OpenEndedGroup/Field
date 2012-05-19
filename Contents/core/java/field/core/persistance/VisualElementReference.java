package field.core.persistance;

import field.core.StandardFluidSheet;
import field.core.dispatch.iVisualElement;

public class VisualElementReference {

	String uid;
	transient iVisualElement cached;
	
	public VisualElementReference(String uid)
	{
		this.uid = uid;
	}

	public VisualElementReference(iVisualElement element)
	{
		this.uid = element.getUniqueID();
		this.cached = element;
	}
	
	public iVisualElement get(iVisualElement root)
	{
		return cached==null ? cached = StandardFluidSheet.findVisualElement(root, uid) : cached;
	}
	
	@Override
	public String toString() {
		return uid+":"+cached;
	}
}
