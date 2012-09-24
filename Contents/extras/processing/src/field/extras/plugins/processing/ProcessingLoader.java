package field.extras.plugins.processing;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

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
import field.core.ui.text.GlobalKeyboardShortcuts;
import field.core.ui.text.BaseTextEditor2.Completion;
import field.core.ui.text.PythonTextEditor.EditorExecutionInterface;
import field.core.ui.text.embedded.MinimalTextField_blockMenu;
import field.core.util.LocalFuture;
import field.core.util.PythonCallableMap;
import field.core.windowing.GLComponentWindow;
import field.launch.Launcher;
import field.launch.SystemProperties;
import field.launch.iUpdateable;
import field.math.abstraction.iFloatProvider;
import field.namespace.generic.Generics.Pair;
import field.util.MiscNative;
import field.util.TaskQueue;

/*
 */
@Woven
public class ProcessingLoader implements iProcessingLoader, iProvidesQueue {

	static public Frame frame;
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

	public ProcessingLoader(final iVisualElement root) {
		this.root = root;

		installDelegateInterpretations();

		runner = new BasicRunner(PythonScriptingSystem.pythonScriptingSystem.get(root), 0) {
			@Override
			protected boolean filter(Promise p) {
				iVisualElement v = (iVisualElement) system.keyForPromise(p);
				iExecutesPromise ep = iExecutesPromise.promiseExecution.get(v);
				return ep == ProcessingLoader.this.ep;
			}
		};

		if (appletShouldBeFullscreen == 110) {
			frame = new JFrame("Field/Processing") {
				public java.awt.im.InputContext getInputContext() {
					return null;
				};

				Rectangle lastBounds = null;

				@Override
				public void paint(Graphics g) {
					super.paint(g);

					if (lastBounds == null || !this.getBounds().equals(lastBounds)) {
						drawQueue.new Task() {
							@Override
							protected void run() {
								((PApplet) applet).size(applet.getWidth(), applet.getHeight(), applet.g.getClass().getName(), "");
								applet.background(0);
							}
						};
					}
					lastBounds = this.getBounds();
				}
			};
			((JFrame) frame).setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			((JFrame) frame).addWindowStateListener(new WindowStateListener() {

				public void windowStateChanged(WindowEvent arg0) {
					int ns = arg0.getNewState();
					if (ns == WindowEvent.WINDOW_CLOSING) {
					}
				}
			});
			((JFrame) frame).addWindowListener(new WindowAdapter() {

				public void windowClosing(WindowEvent arg0) {
					((JFrame) frame).setState(JFrame.ICONIFIED);
				}

			});

			frame.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					drawQueue.new Task() {
						@Override
						protected void run() {
							applet.background(0);
						}
					};
				}
			});
		} else {
			frame = new Frame("Processing") {
				@Override
				public void resize(int width, int height) {
					super.resize(width, height);

					drawQueue.new Task() {
						int n = 0;

						@Override
						protected void run() {
							applet.background(0);
							if (n++ < 2)
								recur();
						}
					};
				}

				@Override
				public void setVisible(boolean b) {
					super.setVisible(b);
					drawQueue.new Task() {
						@Override
						protected void run() {
							applet.background(0);
						}
					};
				}
			};
			if (appletShouldBeFullscreen == 1)
				new MiscNative().enterKiosk_safe();

			// frame.setAlwaysOnTop(true);
			// Launcher.getLauncher().registerUpdateable(new
			// iUpdateable() {
			//
			// int n = 0;
			//
			// public void update() {
			// n++;
			// if (n == 3) {
			// AppleScript s = new
			// AppleScript("tell application \"Field\"\n activate\nend tell",
			// false);
			// ;//;//System.out.println("error : " + s.getError());
			// ;//;//System.out.println("out : " + s.getOutput());
			//
			// Launcher.getLauncher().deregisterUpdateable(this);
			// }
			// }
			// });
		}
		// frame = new Frame("Field/Processing");

		Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
		applet = new PApplet() {

			public ProcessingLoader loader = ProcessingLoader.this;

			public PythonCallableMap onMouseMove = new PythonCallableMap();
			public PythonCallableMap onMousePress = new PythonCallableMap();
			public PythonCallableMap onMouseClick = new PythonCallableMap();
			public PythonCallableMap onMouseDrag = new PythonCallableMap();
			public PythonCallableMap onMouseRelease = new PythonCallableMap();

			public PythonCallableMap onKeyPressed = new PythonCallableMap();
			public PythonCallableMap onKeyReleased = new PythonCallableMap();

			TimeSystem t;

			public int mouseX;
			public int mouseY;
			public int pmouseX, pmouseY;
			public int dmouseX, dmouseY;

			int frameNumber;

			@Override
			public void draw() {
				System.out.println(" draw_impl for <" + System.identityHashCode(this) + "> <" + drawQueue.getNumTasks() + ">");
				theApplet = this;

				if (Platform.isMac())
					synchronized (Launcher.lock) {
						draw_impl();
					}
				else
					draw_impl();
			}

			private void draw_impl() {

				frameNumber++;
				if (g != null && g.getClass().getName().toLowerCase().contains("opengl") && ProcessingLoader.this.frame != null && frameNumber == 4) {
					if (ProcessingLoader.this.frame.isResizable())
						ProcessingLoader.this.frame.setResizable(false);
				}

				int d1 = 0;
				try {

				} catch (Throwable t) {
				}

				try {
					PythonInterface.getPythonInterface().setVariable("p", applet);
					try {
						System.out.println(" num tasks <" + drawQueue.getNumTasks() + "> in <" + System.identityHashCode(drawQueue) + ">");

						try {
							inside = true;

							pmouseX = dmouseX;
							pmouseY = dmouseY;
							drawQueue.update();
							dmouseX = mouseX;
							dmouseY = mouseY;

						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							inside = false;
						}
						if (t == null)
							t = TemporalSliderOverrides.currentTimeSystem.get(ProcessingLoader.this.root);
						runner.update(t == null ? 0 : (float) t.evaluate());
					} catch (Throwable e) {
						e.printStackTrace();
					}
				} finally {

				}

			}

			public TaskQueue getDrawQueue() {
				return drawQueue;
			}

			@Override
			public void exit() {
				// super.exit();
			}

			public void hideCursor() {
				int[] pixels = new int[16 * 16];
				Image image = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16, 16, pixels, 0, 16));
				Cursor transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(0, 0), "nothing");
				frame.setCursor(transparentCursor);
				this.setCursor(transparentCursor);
			}

			@Override
			public void mouseClicked() {
				super.mouseClicked();
				mouseX = mouseEvent.getX();
				mouseY = mouseEvent.getY();

				try {
					onMouseClick.invoke();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void mouseDragged() {
				super.mouseDragged();
				mouseX = mouseEvent.getX();
				mouseY = mouseEvent.getY();
				try {
					onMouseDrag.invoke();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void mouseMoved() {
				super.mouseMoved();
				mouseX = mouseEvent.getX();
				mouseY = mouseEvent.getY();
				try {
					onMouseMove.invoke();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void mousePressed() {
				super.mousePressed();

				mouseX = mouseEvent.getX();
				mouseY = mouseEvent.getY();
				try {
					onMousePress.invoke();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			public boolean isMousePressed() {
				return mousePressed;
			}

			@Override
			public void mouseReleased() {
				super.mouseReleased();
				try {
					onMouseRelease.invoke();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void keyPressed() {

				super.keyPressed();
				try {
					onKeyPressed.invoke();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void keyReleased() {
				super.keyReleased();
				try {
					onKeyReleased.invoke();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
				super.keyPressed(e);
				GlobalKeyboardShortcuts gks = GlobalKeyboardShortcuts.shortcuts.get(root);
				gks.fire(e);
			}

			@Override
			public void keyReleased(KeyEvent e) {
				super.keyReleased(e);
			}

			private int firstRun = 0;

			@Override
			public void setup() {

				if (firstRun++ < 3) {
					if (appletShouldBeFullscreen == 0)
						size(appletWidth, appletHeight, indexToRenderer[ProcessingPlugin.defaultProcessingRenderer[0]]);
					else
						size(loader.frame.getWidth(), loader.frame.getHeight(), indexToRenderer[ProcessingPlugin.defaultProcessingRenderer[0]]);

					// doHints();

					background(0);
				}
			}

			@Override
			public void setSize(int width, int height) {

				super.setSize(width, height);
			}

			@Override
			public void setSize(Dimension d) {

				super.setSize(d);
			}

			@Override
			public void size(int iwidth, int iheight, String irenderer, String ipath) {
				if (appletShouldBeFullscreen != 0) {
					super.size(iwidth, iheight, irenderer, ipath);
					return;
				}
				synchronized (Launcher.lock) {

					ProcessingLoader.this.frame.setSize(iwidth, iheight + 22);

					super.size(iwidth, iheight, irenderer, ipath);

					resizeRenderer(iwidth, iheight);
				}
			}

			public void die(String what) {
				throw new IllegalStateException("(processing.die called) -- " + what);
			};

			public void die(String what, Exception ee) {
				IllegalStateException e = new IllegalStateException("(processing.die called) -- " + what);
				e.initCause(ee);
				throw e;
			};
		};

		applet.sketchPath = PseudoPropertiesPlugin.sheetDataFolder.get(root);

		applet.setPreferredSize(new Dimension(appletWidth, appletHeight));
		applet.setMinimumSize(new Dimension(appletWidth, appletHeight));
		applet.setMaximumSize(new Dimension(2600, 2600));

		frame.add(applet);
		theApplet = applet;

		if (Platform.isMac()) {
			// try {
			// Class c =
			// this.getClass().getClassLoader().loadClass("quicktime.QTSession");
			// c.getDeclaredMethod("open").invoke(null);
			// } catch (Throwable e) {
			// e.printStackTrace();
			// }
		}

		// SavedFramePositions.doFrame(frame, "Field/Processing");
		if (appletShouldBeFullscreen > 0) {

			frame.setUndecorated(true);
			GraphicsDevice[] devs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
			GraphicsDevice dev = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
			for (GraphicsDevice device : devs) {
				if (device == GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice() && appletShouldBeFullscreen == 1) {
					dev = device;
					break;
				} else if (device != GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice() && appletShouldBeFullscreen == 2) {
					dev = device;
					break;
				}
			}

			Rectangle fullscreenbounds = dev.getDefaultConfiguration().getBounds();

			frame.setSize(fullscreenbounds.width, fullscreenbounds.height);
			Dimension sz = new Dimension(fullscreenbounds.width, fullscreenbounds.height);
			frame.setMinimumSize(sz);
			frame.setMaximumSize(sz);

			applet.setPreferredSize(sz);
			applet.setMinimumSize(sz);
			applet.setMaximumSize(sz);

			// applet.size(fullscreenbounds.width,
			// fullscreenbounds.height,
			// applet.g.getClass().getName(), null);
			// frame.setLocation(fullscreenbounds.x,
			// fullscreenbounds.y);
			frame.setBounds(fullscreenbounds.x, fullscreenbounds.y, fullscreenbounds.width, fullscreenbounds.height);
			// dev.setFullScreenWindow(frame);
		} else {
			// applet.addComponentListener(new ComponentAdapter() {
			// @Override
			// public void componentResized(ComponentEvent e) {
			// ;//;//System.out.println(" applet is resized");
			// }
			// });

		}
		// SavedFramePositions.deferVisibilityChange(frame, true);

		deferSetVisible(true);

		// if (appletShouldBeFullscreen==0)
		// frame.setBounds(50, 50, 500, 500);
		// frame.setVisible(true);

		Launcher.getLauncher().registerUpdateable(new iUpdateable() {
			public void update() {

				applet.init();
				applet.noLoop();
				applet.setLocation(0, 22);
				Launcher.getLauncher().deregisterUpdateable(this);
			}
		});

		ep = new iExecutesPromise() {

			public void addActive(iFloatProvider timeProvider, Promise p) {
				ProcessingLoader.this.addActive(timeProvider, p, null);
			}

			public void removeActive(Promise p) {
				ProcessingLoader.this.removeActive(p, null);
			}

			public void stopAll(float t) {
				ProcessingLoader.this.stopAll(t, null);
			}

		};

		PythonInterface.getPythonInterface().execString("def InProcessing(x):\n" + "	def ff(*a):\n" + "		def original():\n" + "			x(*a)\n" + "		u.callLater(original, p.getDrawQueue())\n" + "	return ff\n");

		PythonInterface.getPythonInterface().execString(
				"from field.extras.plugins.processing import * \n" + "def processing(*args): \n" + "	def processingy(a,b,c): \n" + "		ish = ProcessingIsh(b) \n" + "		print \"ish = \", ish\n" + "		x = ish.allCode.replace(\"public class\", \"@Woven class\") \n" + "		x = x.replace(\"static public void main\", \"static public void _main\") \n" + "		x = x.replace(\"extends PApplet\", \"extends HollowPApplet implements iProvidesWrapping\\n\") \n" + "		x = \"import field.extras.plugins.processing.HollowPApplet;\\n\"+x 	\n" + "		x = \"import field.bytecode.protect.SimplyWrappedInQueue.iProvidesWrapping;\\n\"+x \n" + "		x = \"import field.bytecode.protect.annotations.*;\\n\"+x \n" + "		x = \"import field.bytecode.protect.*;\\n\"+x \n"
						+ "		x = \"import static processing.core.PApplet.*;\\n\"+x\n" + "		x = x.replace(\"public void\", \"@SimplyWrapped public void\") \n" + "		print x\n" + "		c[name]=JavaC(a, x, c)() \n" + "		return c[name]\n" + "	if (len(args))==1:\n" + "		name = args[0]\n" + "		return processingy \n" + "	else:\n" + "		print \"making applet called %s \" % _self.name\n" + "		name = _self.name\n" + "		x = processingy(args[0], args[1], args[2])\n" + "		args[2][\"_r\"] = x\n" + "		return x\n" + "");

		PythonInterface.getPythonInterface().setVariable("p", theApplet);
		MinimalTextField_blockMenu.knownTextTransforms.add(new Pair<String, String>("processing", "Lets you use Processing Applet source code directly (makes an applet called after the name of this box)"));

	}

	@NextUpdate(delay = 15)
	public void deferSetVisible(boolean b) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				frame.setVisible(true);
				// frame.pack();
				applet.setLocation(0, 22);
			}
		});
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

		frame.hide();
		applet.stop();

		// actually disposing of the applet causes JOGL to deadlock us
		// frame.dispose();
		// applet.destroy();
	}

	public EditorExecutionInterface getEditorExecutionInterface(final EditorExecutionInterface delegateTo) {

		return new EditorExecutionInterface() {
			public void executeFragment(final String fragment) {
				ProcessingLoader.this.executeFragment(fragment, delegateTo);
			}

			public Object executeReturningValue(final String string) {

				final LocalFuture lf = new LocalFuture();

				ProcessingLoader.this.executeReturningValue(string, delegateTo, lf);

				return lf;
			}

			@Override
			public boolean globalCompletionHook(String leftText, boolean publicOnly, ArrayList<Completion> comp) {
				return false;
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

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				System.out.println(" about to take lock");
				synchronized (Launcher.lock) {
					System.out.println(" took lock ");
					if (appletShouldBeFullscreen == 0)
						applet.size(500, 500, indexToRenderer[ProcessingPlugin.defaultProcessingRenderer[0]]);
					else
						applet.size(frame.getWidth(), frame.getHeight(), indexToRenderer[ProcessingPlugin.defaultProcessingRenderer[0]]);

				}

			}
		});
	}

	@InQueue
	public void injectIntoGlobalNamespace() {
		PythonInterface.getPythonInterface().setVariable("p", applet);
		PythonInterface.getPythonInterface().execString("from processing.core import PApplet\n" + "methodNames = [m.name for m in PApplet.getDeclaredMethods()]\n" + "def bind(meth, inst):	\n" + "	globals()[meth.__name__]=meth\n" + "\n" + "for n in dir(p):\n" + "	try:\n" + "		if n not in methodNames: continue\n" + "		aa = getattr(p, n)\n" + "		bind(aa, p)\n" + "\n" + "	except: pass\n");

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
				// if (natives != null)
				// for (String n : natives) {
				// try {
				// System.err
				// .println(" Processing Plugin is preemptivly loading native library <"
				// + n + ">");
				// System.load(new File(s,
				// n).getCanonicalPath());
				// } catch (Throwable t) {
				// t.printStackTrace();
				// }
				// }
			}

			firstUpdate = false;
		}

		applet.redraw();
	}

	@InQueue
	public void tellOpenGlAboutThatSetSize(int width, int height) {
		//
		// try {
		//
		// GL gl = (GL) ReflectionTools.illegalGetObject(this.applet.g,
		// "gl");
		// System.err.println(" tell open gl about that set size <" +
		// width
		// + "> <" + height + "> <" + gl + "> <" + inside + ">");
		//
		// if (gl == null)
		// return;
		// gl.glViewport(0, 0, width, height);
		//
		// } catch (Throwable t) {
		// t.printStackTrace();
		// }
	}

	@InQueue
	protected void addActive(iFloatProvider timeProvider, final Promise p, iExecutesPromise delegateTo) {

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

		System.out.println(" -- exec fragment in queue :" + this.getQueueFor(null) + " " + System.identityHashCode(this.getQueueFor(null)));

		PythonInterface.getPythonInterface().setVariable("p", applet);
		if (delegateTo == null) {
			PythonInterface.getPythonInterface().execString(fragment);
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
