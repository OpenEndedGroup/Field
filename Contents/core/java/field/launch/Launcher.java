package field.launch;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Display;

import field.core.Platform;
import field.core.util.AppleScript;
import field.util.MiscNative;

//import com.apple.eawt.ApplicationEvent;

public class Launcher {

	public interface iOpenFileHandler {
		public void open(String file);
	}

	public static String resourcesDirectory;

	public static iLaunchable mainInstance;

	protected static Launcher launcher = null;

	static List<iOpenFileHandler> openFileHandlers = new ArrayList<iOpenFileHandler>();

	
	public static String[] args = {};

	static public final Object lock = new Object();

	static {
		try {
			resourcesDirectory = new File(".").getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static public Display display;

	public static Launcher getLauncher() {
		return launcher;
	}

	public static void main(final String[] args) {

		// ]System.out.println(" main ? "+NSThread.isMainThread());

		// GLProfile.initSingleton(true);

		display = new Display();
		display.setAppName("Field");
		display.syncExec(new Runnable() {

			@Override
			public void run() {
				Launcher.args = args;

				if (SystemProperties.getIntProperty("headless", 0) == 1) {
					System.out.println(" we are headless ");
					System.setProperty("java.awt.headless", "true");
				}

				System.out.println(" args are <" + Arrays.asList(args) + ">");

				// Application.getApplication().addAppEventListener(new
				// AppEventListener() {
				//
				// });

				launcher = new Launcher(args);
				new MiscNative().doSplash();

				if (Platform.getOS() == Platform.OS.mac && SystemProperties.getIntProperty("above", 0) == 1)
					new AppleScript("tell application \"Field\"\n activate\nend tell", false);

			}
		});

		while (!display.isDisposed()) {

			try {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		System.out.println(" display has gone");
		Launcher.getLauncher().runRegisteredShutdownHooks();
	}

	static public void registerOpenHandler(iOpenFileHandler h) {
		if (openFileHandlers.size() == 0) {
			// for (ApplicationEvent a : openEvents) {
			// h.open(a.getFilename());
			// }
			// openEvents.clear();

		}
		openFileHandlers.add(h);
	}

	public Thread mainThread = null;

	protected List updateables = Collections.synchronizedList(new ArrayList());
	protected List postUpdateables = Collections.synchronizedList(new ArrayList());

	protected Set paused = Collections.synchronizedSet(new LinkedHashSet());

	protected Set willPause = Collections.synchronizedSet(new LinkedHashSet());

	protected Set willUnPause = Collections.synchronizedSet(new LinkedHashSet());

	protected Set willRemove = Collections.synchronizedSet(new LinkedHashSet());

	protected Set willAdd = Collections.synchronizedSet(new LinkedHashSet());

	protected iUpdateable currentUpdating;

	protected boolean isPaused = false;

	boolean dying = false;

	public Launcher(String[] args) {

		assert launcher == null : launcher;
		Launcher.launcher = this;
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				dying = true;
			}
		}));
		String trampoline = System.getProperty("trampoline.class", "field.bytecode.protect.StandardTrampoline");
		boolean isTrampoline = trampoline != null;

		if (trampoline == null)
			trampoline = System.getProperty("main.class");
		boolean success = false;
		try {
			final Class c = Class.forName(trampoline);
			try {
				Method main = c.getDeclaredMethod("main", new Class[] { new String[0].getClass() });
				try {
					main.invoke(null, new Object[] { args });
					constructMainTimer();
					return;
				} catch (Throwable e) {
					e.printStackTrace();
					if (SystemProperties.getIntProperty("exitOnException", 0) == 1 /*
													 * ||
													 * e
													 * instanceof
													 * Error
													 * ||
													 * e
													 * .
													 * getCause
													 * (
													 * )
													 * instanceof
													 * Error
													 */)
						System.exit(1);
				}
				return;
			} catch (SecurityException e) {
				e.printStackTrace();
				return;
			} catch (NoSuchMethodException e) {
			}

			// swing utilities?
			if (!isTrampoline) {

				mainThread = Thread.currentThread();
				(mainInstance = (iLaunchable) c.newInstance()).launch();
			} else {
				(mainInstance = (iLaunchable) c.newInstance()).launch();
				constructMainTimer();
			}

		} catch (Throwable e) {
			e.printStackTrace();
			if (SystemProperties.getIntProperty("exitOnException", 0) == 1 /*
											 * ||
											 * e
											 * instanceof
											 * Error
											 * ||
											 * e
											 * .
											 * getCause
											 * (
											 * )
											 * instanceof
											 * Error
											 */)
				System.exit(1);
		}

	}

	public void deregisterUpdateable(iUpdateable up) {
		getWillRemove().add(up);
	}

	public boolean isRegisteredUpdateable(iUpdateable up) {
		return getUpdateables().contains(up) || getPaused().contains(up);
	}

	/**
	 * Call this method to register a <code>iUpadteable</code> for updating
	 * at the main timer's frequency.
	 * 
	 * @param target
	 *                - the <code>iUpdateable</code> which will be updated.
	 */
	public void registerUpdateable(iUpdateable target) {
		getWillAdd().add(target);
	}

	public void addPostUpdateable(iUpdateable target) {
		postUpdateables.add(target);
	}

	/**
	 * Call this method to register an <code>iUpdateable</code> for updating
	 * at the specified divisor (<code>updateDivisor</code>) of the main
	 * timer's update frequency. For example, if <code>updateDivisor</code>
	 * is 2 and the main timer runs at 60 Hz, then the target will be
	 * updated at 30 Hz.
	 * 
	 * @param target
	 *                The <code>iUpdateable</code> which will be updated.
	 * @param updateDivisor
	 *                Will be updated at the main timer's frequency divided
	 *                by this parameter.
	 */
	public void registerUpdateable(final iUpdateable target, final int updateDivisor) {
		// The anonymous class used here wraps the
		// update target and implements
		// the logic necessary to support updating at a
		// specified frequency
		// relative to the main timer.
		registerUpdateable(new iUpdateable() {
			int tick = 0;

			public void update() {
				tick++;
				if (tick % updateDivisor == 0) {
					target.update();
				}
			}
		});
	}

	public interface iContinuation {
		public void next();
	}

	private iContinuation continuation;

	private Runnable timer;

	public void setContinuation(iContinuation continuation) {
		this.continuation = continuation;
	}

	protected void constructMainTimer() {
		final double interval = SystemProperties.getDoubleProperty("timer.interval", 0.01f);

		timer = new Runnable() {

			int in = 0;

			public void run() {

				// new Exception().printStackTrace();

				if (!dying)
					display.timerExec((int) (interval * 1000), timer);

				if (continuation != null) {
					try {
						iContinuation was = continuation;
						continuation = null;
						was.next();
					} catch (Throwable t) {
						System.out.println(" exception thrown in continuation <" + continuation + ">");
						t.printStackTrace();
					}

					return;
				}

				mainThread = Thread.currentThread();
				synchronized (lock) {
					if (dying)
						return;

					if (!isPaused) {
						in++;
						try {
							if (in == 1)
								for (int i = 0; i < getUpdateables().size(); i++) {
									iUpdateable up = (iUpdateable) getUpdateables().get(i);
									if (!getPaused().contains(up))
										try {
											setCurrentUpdating(up);

											up.update();

											if (continuation != null) {
												return;
											}

										} catch (Throwable tr) {
											System.err.println(("Launcher reporting an exception while updating <" + up + ">"));
											tr.printStackTrace();
											handle(tr);
											if (SystemProperties.getIntProperty("exitOnException", 0) == 1 /*
																			 * ||
																			 * tr
																			 * instanceof
																			 * Error
																			 * ||
																			 * tr
																			 * .
																			 * getCause
																			 * (
																			 * )
																			 * instanceof
																			 * Error
																			 */)
												System.exit(1);
										}
									setCurrentUpdating(null);
								}
						} finally {
							in--;
						}
						getPaused().addAll(getWillPause());
						getPaused().removeAll(getWillUnPause());
						getWillPause().clear();
						getWillUnPause().clear();
					}
					getUpdateables().addAll(getWillAdd());
					getUpdateables().removeAll(getWillRemove());
					getWillRemove().clear();
					getWillAdd().clear();

					for (iUpdateable u : new ArrayList<iUpdateable>(postUpdateables)) {
						u.update();
					}
				}
			}
		};

		display.timerExec((int) (interval * 1000), timer);
	}

	public interface iExceptionHandler {
		public boolean handle(Throwable t);
	}

	List<iExceptionHandler> exceptionHandlers = new ArrayList<iExceptionHandler>();

	public void handle(Throwable tr) {
		for (int i = 0; i < exceptionHandlers.size(); i++)
			if (exceptionHandlers.get(i).handle(tr))
				return;
	}

	public void addExceptionHandler(iExceptionHandler e) {
		exceptionHandlers.add(e);
	}

	protected iUpdateable getCurrentUpdating() {
		return currentUpdating;
	}

	protected Set getPaused() {
		return paused;
	}

	protected List getUpdateables() {
		return updateables;
	}

	protected Set getWillAdd() {
		return willAdd;
	}

	protected Set getWillPause() {
		return willPause;
	}

	protected Set getWillRemove() {
		return willRemove;
	}

	protected Set getWillUnPause() {
		return willUnPause;
	}

	protected void setCurrentUpdating(iUpdateable currentUpdating) {
		this.currentUpdating = currentUpdating;
	}

	protected void setPaused(Set paused) {
		this.paused = paused;
	}

	protected void setUpdateables(List updateables) {
		this.updateables = updateables;
	}

	protected void setWillAdd(Set willAdd) {
		this.willAdd = willAdd;
	}

	protected void setWillPause(Set willPause) {
		this.willPause = willPause;
	}

	protected void setWillRemove(Set willRemove) {
		this.willRemove = willRemove;
	}

	protected void setWillUnPause(Set willUnPause) {
		this.willUnPause = willUnPause;
	}

	public void nextCycle(final iUpdateable updateable) {
		registerUpdateable(new iUpdateable() {

			public void update() {
				updateable.update();
				deregisterUpdateable(this);
			}
		});
	}

	List<iUpdateable> shutdown = new ArrayList<iUpdateable>();

	volatile static public boolean shuttingDown = false;

	public void runRegisteredShutdownHooks() {

		shuttingDown = true;
		System.out.println(" running down ");
		for (iUpdateable u : shutdown) {
			u.update();
		}

		System.out.println(" exiting ");
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	public void addShutdown(iUpdateable u) {
		shutdown.add(u);
	}

	public void removeShutdownHook(iUpdateable shutdownhook) {
		shutdown.remove(shutdownhook);
	}
}
