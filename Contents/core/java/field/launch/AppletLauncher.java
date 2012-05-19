package field.launch;

import java.applet.Applet;

import javax.swing.JLabel;

public class AppletLauncher extends Applet {

	static public final Applet mainApplet = new Applet();
	
	@Override
	public void init() {
		super.init();
		
		add(new JLabel("banana"));
		
		System.out.println(" about to launch ");
		Launcher.main(new String[]{"-main.class", "field.nonpackage.HelloApplet"});
		System.out.println(" launched ");
	}
}
