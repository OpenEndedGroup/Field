package field.extras.plugins.processing;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import processing.core.PApplet;
import processing.core.PConstants;
import field.bytecode.protect.DeferedInQueue.iProvidesQueue;
import field.bytecode.protect.DeferedInQueue.iRegistersUpdateable;
import field.bytecode.protect.Trampoline2;
import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.InQueue;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.execution.BasicRunner;
import field.core.execution.BasicRunner.Delegate;
import field.core.execution.InterpretPythonAsDelegate;
import field.core.execution.InterpretPythonAsDelegate.iDelegateForReturnValue;
import field.core.execution.PythonInterface;
import field.core.execution.PythonScriptingSystem;
import field.core.execution.PythonScriptingSystem.Promise;
import field.core.execution.TemporalSliderOverrides;
import field.core.execution.TimeSystem;
import field.core.execution.iExecutesPromise;
import field.core.plugins.pseudo.PseudoPropertiesPlugin;
import field.core.ui.text.PythonTextEditor.EditorExecutionInterface;
import field.core.ui.text.embedded.MinimalTextField_blockMenu;
import field.core.util.LocalFuture;
import field.core.util.PythonCallableMap;
import field.launch.Launcher;
import field.launch.SystemProperties;
import field.math.abstraction.iFloatProvider;
import field.namespace.generic.Generics.Pair;
import field.util.TaskQueue;

/*
 */
@Woven
public class ProcessingLoader2 implements iProcessingLoader, iProvidesQueue {

	public Frame frame;
	public PApplet applet;
	public final iVisualElement root;

	private final iExecutesPromise ep;

	TaskQueue drawQueue = new TaskQueue();
	BasicRunner runner;

	boolean inside = false;

	static public final int appletShouldBeFullscreen = SystemProperties.getIntProperty("processingFullscreen", ProcessingPlugin.defaultProcessingFullscreen[0]);

	static public String[] indexToRenderer = { PApplet.OPENGL };
	public static PApplet theApplet;

	public int appletWidth = SystemProperties.getIntProperty("processingWidth", 500);
	public int appletHeight = SystemProperties.getIntProperty("processingHeight", 500);

	public ProcessingLoader2(final iVisualElement root) {
		this.root = root;

		installDelegateInterpretations();

		runner = new BasicRunner(PythonScriptingSystem.pythonScriptingSystem.get(root), 0) {
			@Override
			protected boolean filter(Promise p) {
				iVisualElement v = (iVisualElement) system.keyForPromise(p);
				iExecutesPromise ep = iExecutesPromise.promiseExecution.get(v);
				return ep == ProcessingLoader2.this.ep;
			}
		};

		ep = new iExecutesPromise() {

			public void addActive(iFloatProvider timeProvider, Promise p) {
				ProcessingLoader2.this.addActive(timeProvider, p, null);
			}

			public void removeActive(Promise p) {
				ProcessingLoader2.this.removeActive(p, null);
			}

			public void stopAll(float t) {
				ProcessingLoader2.this.stopAll(t, null);
			}

		};

		PythonInterface.getPythonInterface().execString("def InProcessing(x):\n" + "	def ff(*a):\n" + "		def original():\n" + "			x(*a)\n" + "		u.callLater(original, p.getDrawQueue())\n" + "	return ff\n");

		PythonInterface.getPythonInterface().execString(
				"from field.extras.plugins.processing import * \n" + "def processing(*args): \n" + "	def processingy(a,b,c): \n" + "		ish = ProcessingIsh(b) \n" + "		print \"ish = \", ish\n" + "		x = ish.allCode.replace(\"public class\", \"@Woven class\") \n" + "		x = x.replace(\"static public void main\", \"static public void _main\") \n" + "		x = x.replace(\"extends PApplet\", \"extends HollowPApplet implements iProvidesWrapping\\n\") \n" + "		x = \"import field.extras.plugins.processing.HollowPApplet;\\n\"+x 	\n" + "		x = \"import field.bytecode.protect.SimplyWrappedInQueue.iProvidesWrapping;\\n\"+x \n" + "		x = \"import field.bytecode.protect.annotations.*;\\n\"+x \n" + "		x = \"import field.bytecode.protect.*;\\n\"+x \n"
						+ "		x = \"import static processing.core.PApplet.*;\\n\"+x\n" + "		x = x.replace(\"public void\", \"@SimplyWrapped public void\") \n" + "		print x\n" + "		c[name]=JavaC(a, x, c)() \n" + "		return c[name]\n" + "	if (len(args))==1:\n" + "		name = args[0]\n" + "		return processingy \n" + "	else:\n" + "		print \"making applet called %s \" % _self.name\n" + "		name = _self.name\n" + "		x = processingy(args[0], args[1], args[2])\n" + "		args[2][\"_r\"] = x\n" + "		return x\n" + "");

		MinimalTextField_blockMenu.knownTextTransforms.add(new Pair<String, String>("processing", "Lets you use Processing Applet source code directly (makes an applet called after the name of this box)"));

	}

	@NextUpdate(delay = 5)
	private void deferFrameVisible() {
		frame.setVisible(true);
	}

	private void installDelegateInterpretations() {

		InterpretPythonAsDelegate.factories.add(new iDelegateForReturnValue() {

			public Delegate delegateForReturnValue(float t, Promise p, boolean forwards, Object ret, boolean noDefaultBackwards) {
				if (!(ret instanceof HollowPApplet))
					return null;

				final HollowPApplet rr = ((HollowPApplet) ret);

				return new Delegate() {

					public void continueToBeActive(float t, Promise p, boolean forwards) {
						if (p.isPaused())
							return;
						p.beginExecute();
						PythonInterface.getPythonInterface().setVariable("_x", new Float(t));
						t = (t - p.getStart()) / (p.getEnd() - p.getStart());
						PythonInterface.getPythonInterface().setVariable("_t", new Float(t));
						PythonInterface.getPythonInterface().setVariable("_backwards", !forwards);
						rr.updateState();
						rr.draw();
						p.endExecute();
					}

					public void jumpStop(float t, Promise p, boolean forwards) {
						try {
							p.beginExecute();
							// rr.stop();
							p.endExecute();
						} finally {
							p.wontExecute();
						}
					}

					public void start(float t, Promise p, boolean forwards) {
						p.willExecute();
						try {
							p.beginExecute();
							PythonInterface.getPythonInterface().setVariable("_x", new Float(t));
							t = (t - p.getStart()) / (p.getEnd() - p.getStart());
							PythonInterface.getPythonInterface().setVariable("_t", new Float(t));
							PythonInterface.getPythonInterface().setVariable("_backwards", !forwards);
							rr.updateState();
							rr.setup();
						} finally {
							p.endExecute();
						}
					}

					public void startAndStop(float t, Promise p, boolean forwards) {
					}

					public void stop(float t, Promise p, boolean forwards) {
						jumpStop(t, p, forwards);
					}

				};

			}
		});
	}

	protected void doHints() {
		String p = SystemProperties.getProperty("processingHints", null);
		if (p == null)
			return;

		for (String pp : p.split(":")) {
			try {
				Field f = PConstants.class.getDeclaredField(pp);
				Integer o = (Integer) f.get(null);
				applet.hint(o);
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}

	}

	public void close() {
		frame.dispose();
		applet.stop();
		applet.destroy();
	}

	public EditorExecutionInterface getEditorExecutionInterface(final EditorExecutionInterface delegateTo) {

		System.out.println(" get editor execution interface  ");

		return new EditorExecutionInterface() {
			public void executeFragment(final String fragment) {

				System.out.println(" inside ef <" + fragment + ">");
				ProcessingLoader2.this.executeFragment(fragment, delegateTo);
			}

			public Object executeReturningValue(final String string) {

				final LocalFuture lf = new LocalFuture();

				ProcessingLoader2.this.executeReturningValue(string, delegateTo, lf);

				return lf;
			}
		};
	}

	public iExecutesPromise getExecutesPromise(final iExecutesPromise delegateTo) {
		return ep;
	}

	public iRegistersUpdateable getQueueFor(Method m) {
		return drawQueue;
	}

	public void init() {

		if (appletShouldBeFullscreen == 0)
			applet.size(500, 500, indexToRenderer[ProcessingPlugin.defaultProcessingRenderer[0]]);
		else
			applet.size(frame.getWidth(), frame.getHeight(), indexToRenderer[ProcessingPlugin.defaultProcessingRenderer[0]]);
	}

	@InQueue
	public void injectIntoGlobalNamespace() {
		System.out.println(" inecting into global namespace ");
		PythonInterface.getPythonInterface().setVariable("p", applet);
		PythonInterface.getPythonInterface().execString("from processing.core import PApplet\n" + "methodNames = [m.name for m in PApplet.getDeclaredMethods()]\n" + "def bind(meth, inst):	\n" + "	globals()[meth.__name__]=meth\n" + "\n" + "for n in dir(p):\n" + "	try:\n" + "		if n not in methodNames: continue\n" + "		aa = getattr(p, n)\n" + "		System.out.println(n)\n" + "\n" + "		bind(aa, p)\n" + "\n" + "	except: pass\n");

	}

	boolean firstUpdate = true;

	public void update() {

		if (firstUpdate) {

			for (String s : Trampoline2.extendedClassPaths) {
				System.err.println(" checking <" + s + ">");
				String[] natives = new File(s).list(new FilenameFilter() {

					public boolean accept(File dir, String name) {
						return name.endsWith(".jnilib") || name.endsWith(".dylib") || name.endsWith(".so");
					}
				});
				if (natives != null)
					for (String n : natives) {
						try {
							System.err.println(" Processing Plugin is preemptivly loading native library <" + n + ">");
							System.load(new File(s, n).getCanonicalPath());
						} catch (Throwable t) {
							t.printStackTrace();
						}
					}
			}

			firstUpdate = false;
		}

		// System.out.println(" calling applet redraw ");
		// applet.redraw();
	}

	@InQueue
	protected void addActive(iFloatProvider timeProvider, final Promise p, iExecutesPromise delegateTo) {

		System.out.println("PROCESSING about to add active <" + timeProvider + "> <" + p + "> <" + delegateTo + ">");

		try {
			runner.addActive(timeProvider, p);
		} catch (RuntimeException e) {
			e.printStackTrace();
			System.err.println(" message is <" + e.getMessage() + ">");

		} catch (Throwable t) {
			t.printStackTrace();
			System.err.println(" tmessage is <" + t.getMessage() + ">");

		}
	}

	@InQueue
	protected void executeFragment(String fragment, EditorExecutionInterface delegateTo) {
		System.out.println("PROCESSING execute fragment, delegating to <" + delegateTo + ">");
		PythonInterface.getPythonInterface().setVariable("p", applet);
		if (delegateTo == null) {
			// PythonInterface.getPythonInterface().execString(fragment);
		} else
			delegateTo.executeFragment(fragment);
	}

	@InQueue
	protected void executeReturningValue(String string, EditorExecutionInterface delegateTo, LocalFuture lf) {
		PythonInterface.getPythonInterface().setVariable("p", applet);
		Object o = delegateTo.executeReturningValue(string);
		lf.set(o);
	}

	@InQueue
	protected void removeActive(Promise p, iExecutesPromise delegateTo) {

		System.out.println(" remove active <" + p + "> from runner <" + runner + ">");

		runner.removeActive(p);
	}

	@InQueue
	protected void stopAll(float t, iExecutesPromise delegateTo) {
		runner.stopAll(t);
	}

	@Override
	public void setOntop(Boolean s) {
		frame.setAlwaysOnTop(s);
	}

}
