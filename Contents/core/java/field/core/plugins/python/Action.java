package field.core.plugins.python;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NonSwing;
import field.launch.iUpdateable;

@Woven
public class Action extends DynamicExtensionPoint<iUpdateable>{

	public Action() {
		super(iUpdateable.class);
	}

	@NonSwing
	public void update()
	{
		getProxy().update();
	}

	@NonSwing
	public void apply()
	{
		getProxy().update();
	}
	
	public Object call(Object[] args) {
		if (args==null || args.length==0)
		{
			update();
			return null;
		}
		return super.call(args);
	}

}
