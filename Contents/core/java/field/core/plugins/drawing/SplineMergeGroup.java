package field.core.plugins.drawing;

import java.util.ArrayList;

import field.core.dispatch.MergeGroupFreezer;
import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElementOverrides;
import field.core.persistance.VEList;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.windowing.components.PlainDraggableComponent;
import field.core.windowing.components.iComponent;
import field.namespace.generic.Generics.Triple;


public class SplineMergeGroup extends MergeGroupFreezer {

	public SplineMergeGroup() {
		super();
	}

	public SplineMergeGroup(iVisualElement owner) {
		super(owner);
	}

	public iVisualElement create(Object token) {
		Triple<VisualElement, PlainDraggableComponent, SplineComputingOverride> r = super.create(token, owner.getFrame(null), VisualElement.class, PlainDraggableComponent.class, SplineComputingOverride.class);

		return r.left;
	}

	@Override
	protected <T extends VisualElement, S extends iComponent, U extends iVisualElementOverrides.DefaultOverride> void newlyCreated(Triple<T, S, U> r) {
		super.newlyCreated(r);
		SplineComputingOverride.computed_linesToDraw.set(owner, r.left, new ArrayList<CachedLine>());
		SplineComputingOverride.computed_elaborates.addToList(VEList.class, r.left, (iVisualElement) owner);
		SplineComputingOverride.computed_elaboratedBy.addToList(VEList.class, owner, (iVisualElement) r.left);
	}

	@Override
	protected void lost(VisualElement element) {
		super.lost(element);
	}

	@Override
	protected boolean shouldCull(VisualElement element, int hash, int oldHash) {
		boolean q = super.shouldCull(element, hash, oldHash);
		return q;
	}

	@Override
	public void end() {
		super.end();
		if (open == 0) {
			for (iVisualElement v : newlyCreated) {
				SplineComputingOverride.executeMain(v);
			}
		}
	}
}
