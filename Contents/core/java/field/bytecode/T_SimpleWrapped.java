package field.bytecode;

import java.lang.reflect.Method;

import field.bytecode.protect.Woven;
import field.bytecode.protect.SimplyWrappedInQueue.iProvidesWrapping;
import field.bytecode.protect.SimplyWrappedInQueue.iWrappedExit;
import field.bytecode.protect.annotations.SimplyWrapped;
import field.launch.iLaunchable;
import field.math.linalg.Vector3;

public class T_SimpleWrapped implements iLaunchable {
	
	@Woven
	public class Banana implements iProvidesWrapping
	{
		
		@SimplyWrapped
		public Object banana()
		{
			System.out.println(" I'm a banana ");
			return new Vector3(0,0,0);
		}
		
		public iWrappedExit enter(Method m) {
			System.out.println(" entering <"+m+">");
			return null;
		}
		
	}
	
	public void launch() {

		new Banana().banana();
		
	}
}
