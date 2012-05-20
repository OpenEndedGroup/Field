/*
 * Created on Oct 11, 2003
 */
package field.extras.osc;

import field.core.network.OSCOutput;
import field.core.network.UDPNIOSender;
import field.launch.Launcher;
import field.launch.iLaunchable;
import field.launch.iUpdateable;

/**
 * @author marc
 */
public class T_OSCOutput implements iLaunchable {
	private OSCOutput output;

	public void launch() {
		output = new OSCOutput(1000, new UDPNIOSender(5500, "255.255.255.255"));
//		Launcher.getLauncher().registerUpdateable(this);
		Launcher.getLauncher().registerUpdateable(new iUpdateable() {
			int tt = 0;
			
			public void update() {
				tt++;
				
				;//;//System.out.println(" tt = <"+tt+">");
				float x = (float) Math.sin(tt/40f);
				
				;//;//System.out.println(" sendgin <"+x+"> <"+tt+">");
				
//				output.encode("scrubPosition", new OSCElement[]{new OscFloat((t/100f)%1)}).send();				
				
				//output.encode("hello", new OSCElement[]{new OscFloat(x)}).send();	
				output.simpleSend("hello", x, (int)tt, "bipity");
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//				
				//System.exit(1);
			}
		});
	}
}
