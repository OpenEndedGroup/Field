package field.util;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import field.launch.Launcher;
import field.launch.iUpdateable;

/**
 * Submits runnables to the main Field update cycle.
 * 
 * @author marc
 * 
 */
public class FieldExecutorService extends AbstractExecutorService {

	static public final FieldExecutorService service = new FieldExecutorService();

	@Override
	public boolean awaitTermination(long arg0, TimeUnit arg1) throws InterruptedException {
		return false;
	}

	@Override
	public boolean isShutdown() {
		return false;
	}

	@Override
	public boolean isTerminated() {
		return false;
	}

	@Override
	public void shutdown() {
	}

	@Override
	public List<Runnable> shutdownNow() {
		return null;
	}

	@Override
	public void execute(final Runnable arg0) {
		Launcher.getLauncher().registerUpdateable(new iUpdateable() {

			@Override
			public void update() {
				arg0.run();
				Launcher.getLauncher().deregisterUpdateable(this);
			}
		});
	}

	public void executeLater(final Runnable arg0, final int delay) {
		Launcher.getLauncher().registerUpdateable(new iUpdateable() {

			int t = 0;

			@Override
			public void update() {
				if (t == delay) {
					arg0.run();
					Launcher.getLauncher().deregisterUpdateable(this);
				}
				t++;
			}
		});
	}

}
