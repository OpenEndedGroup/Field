package field.graphics.core;

public class SubCamera extends StereoCamera {

	private final BasicCamera parent;

	public SubCamera(BasicCamera parent) {
		this.parent = parent;
		this.setState(parent.getState());
		this.setViewport(parent.oX, parent.oY, parent.width, parent.height);
		this.setAspect(parent.aspect);
	}

	@Override
	protected void finalPost() {
		super.finalPost();
		parent.performPass();
	}
}
