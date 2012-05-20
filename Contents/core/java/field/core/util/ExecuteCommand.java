package field.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class ExecuteCommand {

	private ProcessBuilder pb;

	private Process executing;

	private Thread t;

	private List<String> command;

	StringBuffer allOut = new StringBuffer();

	boolean finished = false;

	public ExecuteCommand(String directory, String command) {
		this(directory, command, true);
	}

	public ExecuteCommand(String directory, String command, boolean redirectError) {
		String[] s = command.split(" ");
		go(directory, redirectError, s);
	}

	public ExecuteCommand(String directory, String[] s, boolean redirectError) {
		go(directory, redirectError, s);
	}

	public String getOutput() {
		// ;//System.out.println(" output is <\n"+allOut+"\n>");
		return allOut.toString();
	}

	public int waitFor() {
		return waitFor(true);
	}

	public int waitFor(final boolean waitForOutput, long millis) {
		try {
			
			Thread t = new Thread()
			{
				public void run() {
					waitFor(waitForOutput);
				};
			};
			t.start();

			t.join(millis);
			if (t.isAlive())
			{
				return -1;
			}
			return 0;

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public int waitFor(boolean waitForOuput) {
		try {
			int rr = executing.waitFor();
			if (waitForOuput)
				t.join();

			if (rr != 0)
				return rr;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public Thread.State getState() {
		return t.getState();
	}

	public boolean isFinished() {
		return t.getState().equals(Thread.State.TERMINATED);
	}

	public class Runner extends Thread {

		private InputStream is;

		public Runner(InputStream is) {
			this.is = is;
		}

		public void run() {
			try {
				int i = 0;
				while ((i = is.read()) != -1) {
					System.out.print((char) i);

					allOut.append((char) i);
				}
			} catch (IOException e) {
			}

			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			finished = true;
		};
	}

	private void go(String directory, boolean redirectError, String[] s) {

		;//System.out.println(":::::::::::::::::::::::::::::::::::: " + Arrays.asList(s) + " " + redirectError);
		// new Exception().printStackTrace();

		command = Arrays.asList(s);
		pb = new ProcessBuilder(s);
		pb.directory(new File(directory));

		pb.environment().put("VERSIONER_PYTHON_PREFER_32_BIT", "yes");

		pb.redirectErrorStream(redirectError);
		if (redirectError)
			pb.redirectErrorStream();
		try {
			executing = pb.start();
			final InputStream is = executing.getInputStream();
			t = new Runner(is);
			t.start();

			if (!redirectError) {
				final InputStream is2 = executing.getErrorStream();
				Runner t2 = new Runner(is2);
				t2.start();

			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}
