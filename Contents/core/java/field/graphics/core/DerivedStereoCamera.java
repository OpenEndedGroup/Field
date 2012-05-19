package field.graphics.core;

import field.graphics.core.Base.StandardPass;

public class DerivedStereoCamera extends BasicUtilities.OnePassListElement{

	private final StereoCamera derivedFrom;
	private final StereoCamera target;

	public DerivedStereoCamera(StereoCamera derivedFrom)
	{
		super(StandardPass.preRender, StandardPass.preRender);
		this.derivedFrom = derivedFrom;
		target = new StereoCamera();
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
