package field;

import field.core.execution.AutoEngage;
import field.core.execution.PhantomFluidSheet;
import field.launch.Launcher;
import field.launch.SystemProperties;
import field.launch.iLaunchable;
import field.launch.iUpdateable;
import field.util.MiscNative;

public class Faceless implements iLaunchable {
	public double t;

	private PhantomFluidSheet phantom;

	public PhantomFluidSheet getSheet() {
		return phantom;
	}

	public void launch() {

		phantom= new PhantomFluidSheet(System.getProperty("user.home") + "/Documents/FieldWorkspace/"+SystemProperties.getProperty("field.scratch", "field.Blank.field"), false, false);
		
		Launcher.getLauncher().registerUpdateable(new iUpdateable() {

			public void update() {
				phantom.update((float) t);
			}
		});

		if (SystemProperties.getIntProperty("black", 0) == 1) {
			MiscNative.goToBlack();

		}

		String a = SystemProperties.getProperty("auto", "");
		if (!a.equals("")) {
			AutoEngage auto = new AutoEngage(phantom.getRoot());
			auto.start(a);
		}
	}
}
