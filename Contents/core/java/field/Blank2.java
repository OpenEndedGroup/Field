package field;

import java.io.File;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.Platform;
import field.core.Platform.OS;
import field.core.execution.AutoEngage;
import field.core.execution.PhantomFluidSheet;
import field.core.plugins.log.SimpleLogging;
import field.core.ui.FieldMenus2;
import field.launch.SystemProperties;
import field.launch.iLaunchable;
import field.util.MiscNative;
import field.util.WorkspaceDirectory;

@Woven
public class Blank2 implements iLaunchable {
	File openFileProvokation;
	boolean finished = false;
	PhantomFluidSheet loaded = null;

	// ImportPythonFile pythonFileImporter = new ImportPythonFile();
	// PackageTools fieldPackageImporter = new PackageTools();

	public PhantomFluidSheet getSheet() {
		return loaded;
	}

	public void launch() {
		if (Platform.getOS() == OS.mac)
			new MiscNative().allViewsAcceptFirstMouse();

		// TODO - swt
		new WorkspaceDirectory();
		new SimpleLogging();

		finished = false;

		// cause FieldMenus2 to init;
		FieldMenus2.getCanonicalVersioningDir();

		part2();
	}

	@NextUpdate(delay = 2)
	public void part2() {
		System.out.println(" sheets are :" + FieldMenus2.fieldMenus.openSheets);
		if (FieldMenus2.fieldMenus.openSheets.size() > 0)
			return;

		finished = true;

		// if 'black' is set, set the screen transfer
		// table so that
		// everything is black
		// this is useful for installed pieces that,
		// having loaded, will
		// presumably fade up once they are ready to run
		// this has to be done here, before we open
		// windows, run code &c
		if (SystemProperties.getIntProperty("black", 0) == 1) {
			MiscNative.goToBlack();

		}

		if (SystemProperties.getProperty("field.scratch", null) == null) {
			;// System.out.println(" -- FieldMenus2.fieldMenus --");
				// return;

		}

		if (openFileProvokation == null) {
			String mcName = SystemProperties.getProperty("main.class");
			;// System.out.println(" main class <" + mcName + "> <"
				// + Platform.getCanonicalName(this.getClass())
				// + ">");
			if (mcName.equals(Platform.getCanonicalName(this.getClass())))
				mcName = "Default";
			// loaded = new
			// PhantomFluidSheet(System.getProperty("user.home") +
			// "/Documents/FieldWorkspace/"+SystemProperties.getProperty("field.scratch",
			// mcName + ".field"), true, true);
			loaded = FieldMenus2.fieldMenus.open(SystemProperties.getProperty("field.scratch", mcName + ".field"));
			// loaded =
			// FieldMenus.fieldMenus.openSheet(SystemProperties.getProperty("field.scratch",
			// mcName + ".field"), false);

		} else {

		}
		String a = SystemProperties.getProperty("auto", "");
		if (!a.equals("") && loaded != null) {
			AutoEngage auto = new AutoEngage(loaded.getRoot());
			auto.start(a);
		}
	}

}
