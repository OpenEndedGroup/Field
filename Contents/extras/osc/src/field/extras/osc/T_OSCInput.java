package field.extras.osc;

import java.util.Arrays;

import field.core.network.OSCInput;
import field.launch.Launcher;
import field.launch.iLaunchable;

/**
 * @author marc Created on Oct 11, 2003
 */
public class T_OSCInput implements iLaunchable {
	public void launch() {	
		OSCInput input = new OSCInput(7001);
		input.setDefaultHandler(new OSCInput.DispatchableHandler() {
			public void handle(String s, Object[] args) {
				System.out.println(s+" "+Arrays.asList(args));
			}
		});
		input.registerHandler("", new OSCInput.Handler() {
			public void handle(float number)
			{
				System.out.println(" start <"+number+">");
			}
		});
		
		input.registerHandler("interlude1", new OSCInput.DispatchableHandler() {
			public void handle(String s, Object[] args) {
				System.out.println("!!"+s+" "+Arrays.asList(args));
			}
		});
		Launcher.getLauncher().registerUpdateable(input);
	}
}
