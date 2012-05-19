package field.core.execution;

import java.util.List;

import field.core.StandardFluidSheet;
import field.core.dispatch.iVisualElement;
import field.core.plugins.pseudo.PseudoPropertiesPlugin;
import field.launch.Launcher;
import field.launch.SystemProperties;
import field.launch.iUpdateable;

/**
 * used to parse a command line for executing loaded sheets automatically
 */
public class AutoEngage {

	private final iVisualElement root;

	int delay = SystemProperties.getIntProperty("autoDelay", 0);

	public AutoEngage(iVisualElement root) {
		this.root = root;
	}

	public void start(final String spec) {
		Launcher.getLauncher().registerUpdateable(new iUpdateable() {

			int t = 0;

			public void update() {

				if (t == delay) {

					String[] parts = spec.split(";");

					for (String s : parts) {
						System.out.println(" looking up <" + s + ">");
						startElement(s);
					}
					Launcher.getLauncher().deregisterUpdateable(this);
				}

				t++;

			}
		});

//		new Thread() {
//			long m = System.currentTimeMillis();
//
//			public void run() {
//
//				while (System.currentTimeMillis() - m < (1000 * 60 * 30L)) {
//					try {
//						Thread.sleep(5000);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//					
//				}
//				System.err.println(" forcing exit ");
//				Runtime.getRuntime().halt(0);
//
//			};
//		}.start();

	}

	protected void startElement(iVisualElement v) {

		System.out.println(" :::::::::::::: starting <" + v + ">");
		PseudoPropertiesPlugin.begin.get(v).call(new Object[] {});

		// PythonScriptingSystem pss =
		// PythonScriptingSystem.pythonScriptingSystem.get(v);
		// iExecutesPromise executesPromise =
		// iExecutesPromise.promiseExecution.get(v);
		//
		// Promise promise = pss.promiseForKey(v);
		//
		// executesPromise.addActive(new iFloatProvider() {
		// public float evaluate() {
		// return 0;
		// }
		// }, promise);
	}

	protected void startElement(String s) {
		List<iVisualElement> found = StandardFluidSheet.findVisualElementWithNameExpression(root, s);
		for (iVisualElement v : found) {
			startElement(v);
		}
	}

}
