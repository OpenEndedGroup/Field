package field.core.persistance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import field.core.dispatch.iVisualElement;


// a tag class for special handling of loading
public class VEList extends ArrayList<iVisualElement> implements List<iVisualElement>{

	public VEList(VEList list) {
		super(list);
	}

	public VEList() {
	}

	@Override
	public Iterator<iVisualElement> iterator() {
		scrubNull();
		return super.iterator();
	}

	protected void scrubNull() {
		for (int i = 0; i < this.size(); i++) {
			if (this.get(i) == null) {
				this.remove(i);
				i--;
			}
		}
	}
}
