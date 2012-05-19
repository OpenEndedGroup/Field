package field.graphics.core;

import field.graphics.core.Base.StandardPass;

public class DerivedCamera extends BasicUtilities.OnePassListElement{

	private final BasicCamera derivedFrom;
	private final BasicCamera target;

	public DerivedCamera(BasicCamera derivedFrom)
	{
		super(StandardPass.preRender, StandardPass.preRender);
		this.derivedFrom = derivedFrom;
		target = new BasicCamera();
	}
	
	public BasicCamera getTarget() {
		return target;
	}
	
	@Override
	public void performPass() {
		
		derivedFrom.copyTo(target);
		
		filterCamera(derivedFrom, target);
		
		pre();
		target.gl = gl;
		target.glu = glu;
		target.performPass();
		post();
	}

	protected void filterCamera(BasicCamera derivedFrom, BasicCamera target) {
	}

}
