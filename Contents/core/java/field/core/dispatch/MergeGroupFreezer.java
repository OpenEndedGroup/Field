package field.core.dispatch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.plugins.python.PythonPlugin;
import field.core.ui.text.embedded.FreezeProperties;
import field.core.ui.text.embedded.FreezeProperties.Freeze;
import field.core.windowing.components.iComponent;
import field.namespace.generic.Generics.Triple;
import field.util.diff.Diff3;

public class MergeGroupFreezer extends MergeGroup {
	static public final VisualElementProperty<HashMap<iVisualElement, Freeze>> mergeGroup_initialParameters = new VisualElementProperty<HashMap<iVisualElement, Freeze>>("mergeGroup_initialParameters");

	protected HashSet<iVisualElement> newlyCreated = new HashSet<iVisualElement>();

	Set<String> include = new HashSet<String>();

	FreezeProperties freezer = new FreezeProperties();

	HashMap<iVisualElement, Freeze> frozen = new HashMap<iVisualElement, Freeze>();

	public MergeGroupFreezer() {
		super();
	}

	public MergeGroupFreezer(iVisualElement owner) {
		super(owner);
		HashMap<iVisualElement, Freeze> q = mergeGroup_initialParameters.get(owner);
		if (q == null)
			mergeGroup_initialParameters.set(owner, owner, frozen);
		else
			frozen = q;

		FreezeProperties.standardCloneHelpers(freezer);

		freezer.setInclude(include);

	}

	public void addInclude(String a) {
		include.add(a);
		freezer.setInclude(include);
	}

	@Override
	public void begin() {
		if (open == 0) {
			newlyCreated.clear();
		}
		super.begin();
	}

	@Override
	public void end() {
		super.end();
		if (open == 0) {
			for (iVisualElement v : newlyCreated) {
				frozen.put(v, freezer.new Freeze().freeze((VisualElement) v));
			}
		}
	}

	public void reset(iVisualElement a) {
		assert open > 0;
		newlyCreated.add(a);
		frozen.remove(a);
	}

	// these echo those in PythonPlugin, but can diff3 merge on set
	// it might be better to defer these until end() time
	public void setAttr(iVisualElement a, String name, Object value) {
		System.err.println(" setting property <" + a + "> <" + name + "> <" + value + ">");
		if (frozen.containsKey(a)) {
			name = PythonPlugin.externalPropertyNameToInternalName(name);
			Freeze f = frozen.get(a);
			VisualElementProperty vp = new VisualElementProperty(name);
			Object initValue = f.getMap().get(vp);
			Object currentValue = vp.get(a, a);

			if (initValue != null && currentValue != null && value != null) if (initValue instanceof String && currentValue instanceof String && value instanceof String) {
				Diff3 d = new Diff3((String) currentValue, (String) initValue, (String) value);
				String c = d.getResult();
				vp.set(a, a, c);

				f.getMap().put(vp, value);

				return;
			}

			vp.set(a, a, value);
		} else {
			PythonPlugin.setAttr(a, name, value);
		}
	}

	@Override
	protected <T extends VisualElement, S extends iComponent, U extends DefaultOverride> void newlyCreated(Triple<T, S, U> triple) {
		super.newlyCreated(triple);
		newlyCreated.add(triple.left);
	}

	@Override
	protected boolean shouldCull(VisualElement element, int hash, int oldHash) {

		boolean a = super.shouldCull(element, hash, oldHash);

		a = true;

		if (a) frozen.remove(element);




		return a;
	}

}
