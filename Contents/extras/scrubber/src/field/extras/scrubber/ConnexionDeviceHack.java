package field.extras.scrubber;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import org.python.core.PyObject;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElement.Rect;
import field.core.persistance.VisualElementReference;
import field.core.plugins.python.PythonPlugin.CapturedEnvironment;
import field.extras.scrubber.ScrubberPlugin.Connection;
import field.launch.iUpdateable;
import field.namespace.generic.Bind.iFunction;
import field.util.HashMapOfLists;

@Woven
public class ConnexionDeviceHack implements iConnexionSource {

	static public ConnexionDeviceHack globalDevice;

	float[] axes = new float[6];
	int buttons = 0;
	int lastButton = 0;

	private final ScrubberPlugin registerWith;

	public ConnexionDeviceHack(ScrubberPlugin registerWith) throws IOException {

		this.registerWith = registerWith;
		if (globalDevice == null) {

			ProcessBuilder k = new ProcessBuilder("killall", "-9", "3DxClientTest");
			try {
				k.start().waitFor();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			
			ProcessBuilder b = new ProcessBuilder("3DxClientTest.app/Contents/MacOS/3DxClientTest");
			Process proc = b.start();
			final InputStream stream = proc.getErrorStream();
			final BufferedReader buffer = new BufferedReader(new InputStreamReader(stream), 10);
			new Thread(new Runnable() {
				int n = 0;

				public void run() {
					while (true) {
						try {
							String r = buffer.readLine();
							if (r != null) {
								String[] s = r.split(" ");
								for (int i = 0; i < 6; i++) {
									axes[i] = map(i, Integer.parseInt(s[i]) / 255f);
								}
								buttons = Integer.parseInt(s[6]);
								if (buttons != lastButton)
									firecallbacks(buttons, lastButton);
								lastButton = buttons;
								n++;
								if (n % 40 == 0)
									System.out.println(" (( connexion alive ))");
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}).start();

			globalDevice = this;
		}

		if (registerWith!=null)
			registerFactories(registerWith);
	}

	protected void registerFactories(ScrubberPlugin registerWith) {

		registerWith.addFactory("scrub <b>horizontally</b> based on <b>z-axis</b> rotation", new iFunction<Connection, iVisualElement>() {
			public Connection f(iVisualElement in) {
				return new GeneralConnection(in, "horizontal(" + in.getProperty(iVisualElement.name) + ")", 5, 10, 0);
			}
		});
		registerWith.addFactory("scrub <b>horizontally</b> based on <b>z-axis</b> translation", new iFunction<Connection, iVisualElement>() {
			public Connection f(iVisualElement in) {
				return new GeneralConnection(in, "horizontal(" + in.getProperty(iVisualElement.name) + ")", 2, 10, 0);
			}
		});
		registerWith.addFactory("<b> execute <i>while</i></b> left button down", new iFunction<Connection, iVisualElement>() {
			public Connection f(iVisualElement in) {
				return new PressExecution(in, "pressLeft(" + in.getProperty(iVisualElement.name) + ")", 1);
			}
		});
		registerWith.addFactory("<b> <i>toggle</i> execution</b> with left button", new iFunction<Connection, iVisualElement>() {
			public Connection f(iVisualElement in) {
				return new ToggleExecution(in, "toggleLeft(" + in.getProperty(iVisualElement.name) + ")", 1);
			}
		});
		registerWith.addFactory("<b> execute <i>while</i></b> right button down", new iFunction<Connection, iVisualElement>() {
			public Connection f(iVisualElement in) {
				return new PressExecution(in, "pressRight(" + in.getProperty(iVisualElement.name) + ")", 2);
			}
		});
		registerWith.addFactory("<b> <i>toggle</i> execution</b> with right button", new iFunction<Connection, iVisualElement>() {
			public Connection f(iVisualElement in) {
				return new ToggleExecution(in, "toggleRight(" + in.getProperty(iVisualElement.name) + ")", 2);
			}
		});
	}

	static public class GeneralConnection extends Connection {

		public GeneralConnection(iVisualElement to, String name, int axis, float scale, int aspect) {
			this.outputTo = new VisualElementReference(to);
			this.name = name;
			this.axis = axis;
			this.scale = scale;
			this.targetAspect = aspect;
			enabled = true;
		}

		int axis = 0;
		int targetAspect = 0;
		float scale = 0;

		@Override
		public void update(iVisualElement root) {

			System.out.println(" update generale conncetion");

			if (ConnexionDeviceHack.globalDevice == null)
				return;
			float f = ConnexionDeviceHack.globalDevice.axes[axis];
			System.out.println(f);

			f *= scale;
			Rect fr = outputTo.get(root).getFrame(null);
			if (targetAspect == 0)
				fr.x += f;
			else if (targetAspect == 1)
				fr.y += f;

			System.out.println("updating frame to <" + fr + ">");
			iVisualElement old = iVisualElementOverrides.topology.setAt(outputTo.get(root));
			iVisualElementOverrides.forward.shouldChangeFrame.shouldChangeFrame(outputTo.get(root), fr, outputTo.get(root).getFrame(null), true);
			outputTo.get(root).setProperty(iVisualElement.dirty, true);
			iVisualElementOverrides.topology.setAt(old);
		}
	}

	static public class ToggleExecution extends Connection {
		private final int button;

		public ToggleExecution(iVisualElement to, String name, int button) {
			this.outputTo = new VisualElementReference(to);
			this.name = name;
			this.button = button;
		}

		boolean lastWasDown = false;
		boolean on = false;

		@Override
		public void update(iVisualElement root) {
			if (ConnexionDeviceHack.globalDevice == null)
				return;
			int b = ConnexionDeviceHack.globalDevice.buttons;
			boolean stat = (b & (1 << button)) != 0;
			if (stat && !lastWasDown) {
				toggle(root);
			}

			lastWasDown = stat;
		}

		private void toggle(iVisualElement root) {
			if (on) {
				iVisualElement old = iVisualElementOverrides.topology.setAt(outputTo.get(root));
				iVisualElementOverrides.forward.endExecution.endExecution(outputTo.get(root));
				iVisualElementOverrides.topology.setAt(old);
				on = false;
			} else {
				iVisualElement old = iVisualElementOverrides.topology.setAt(outputTo.get(root));
				iVisualElementOverrides.forward.beginExecution.beginExecution(outputTo.get(root));
				iVisualElementOverrides.topology.setAt(old);
				on = true;
			}
		}
	}

	static public class PressExecution extends Connection {
		private final int button;

		public PressExecution(iVisualElement to, String name, int button) {
			this.outputTo = new VisualElementReference(to);
			this.name = name;
			this.button = button;
		}

		boolean lastWasDown = false;

		@Override
		public void update(iVisualElement root) {
			if (ConnexionDeviceHack.globalDevice == null)
				return;
			int b = ConnexionDeviceHack.globalDevice.buttons;
			boolean stat = (b & (1 << button)) != 0;
			if (stat && !lastWasDown) {
				toggle(true, root);
			} else if (!stat && lastWasDown) {
				toggle(false, root);
			}

			lastWasDown = stat;
		}

		private void toggle(boolean on, iVisualElement root) {
			if (!on) {
				iVisualElement old = iVisualElementOverrides.topology.setAt(outputTo.get(root));
				iVisualElementOverrides.forward.endExecution.endExecution(outputTo.get(root));
				iVisualElementOverrides.topology.setAt(old);

			} else {
				iVisualElement old = iVisualElementOverrides.topology.setAt(outputTo.get(root));
				iVisualElementOverrides.forward.beginExecution.beginExecution(outputTo.get(root));
				iVisualElementOverrides.topology.setAt(old);
			}
		}
	}

	@NextUpdate
	protected void firecallbacks(int newButtons, int oldButtons) {
		int m = 1;
		for (int i = 0; i < 8; i++) {

			System.out.println(newButtons+" "+oldButtons+" "+m);

			if ((newButtons & m) !=0 && (oldButtons & m) == 0) {
				Collection<iUpdateable> c = buttonToggles.get(i);
				if (c != null)
					for (iUpdateable u : c)
						u.update();
			}
			m <<= 1;
		}
	}

	HashMapOfLists<Integer, iUpdateable> buttonToggles = new HashMapOfLists<Integer, iUpdateable>();

	public void addToogle(int button, iUpdateable u) {
		buttonToggles.addToList(button, u);
	}

	public void addToogle(int button, final PyObject u, final CapturedEnvironment env) {
		buttonToggles.addToList(button, new iUpdateable() {

			public void update() {
				env.enter();
				try {
					u.__call__();
				} finally {
					env.exit();
				}
			}
		});
	}

	protected float map(int i, float f) {
		return Math.signum(f) * f * f;
	}

	public float[] getAxes() {
		return axes;
	}

	public int getButtons() {
		return buttons;
	}
}