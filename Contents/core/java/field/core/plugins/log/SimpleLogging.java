package field.core.plugins.log;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

import field.core.plugins.log.Logging.iLoggingEvent;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.launch.Launcher.iExceptionHandler;

/**
 * a stderr "tap" that can filter things through the logging system
 *
 * @author marc
 *
 */
public class SimpleLogging {

	static {
		System.setErr(new PrintStream(System.err) {

			ThreadLocal<StringBuffer> thisLine = new ThreadLocal<StringBuffer>() {
				@Override
				protected StringBuffer initialValue() {
					return new StringBuffer(100);
				}
			};

			@Override
			public void write(byte[] b) throws IOException {
				super.write(b);
			}

			@Override
			public void write(byte[] buf, int off, int len) {
				super.write(buf, off, len);
				if (Logging.logging == null)
					return;

				StringBuffer buffer = thisLine.get();
				for (int i = 0; i < len; i++) {
					char cc = (char) buf[i + off];
					buffer.append(cc);
					if (cc == '\n') {
						flushThisLine(buffer);
					}
				}
			}

			@Override
			public void write(int b) {
				super.write(b);
				if (Logging.logging == null)
					return;
				char cc = (char) b;
				StringBuffer buffer = thisLine.get();
				buffer.append(cc);
				if (cc == '\n') {
					flushThisLine(buffer);
				}
			}
		});

		Launcher.getLauncher().addExceptionHandler(new iExceptionHandler() {

			public boolean handle(final Throwable t) {
				if (Logging.logging!=null)
				Logging.logging.addEvent(new iLoggingEvent() {

					public String getLongDescription() {
						return getLongDescription(t);
					}

					public String getLongDescription(Throwable t) {
						String m = "<html><font color=#ffffff><i>message</i><br>" + t + "<br><br>";
						String er = isError() ? "<i>stack trace</i><br>" + Arrays.asList(t.getStackTrace()).toString().replace(",", "<br>") : "";
						Throwable cc = t.getCause();
						if (cc!=null)
							return m + er + "<br><br><i>cause</i><br>"+getLongDescription(cc);
						return m + er;
					}

					public String getReplayExpression() {
						return null;
					}

					public String getTextDescription() {
						return "<html><font bgcolor=#ffeeee><b>Exception thrown</b> \u2014 " + t;
					}

					public boolean isError() {
						return true;
					}
				});
				return false;
			}
		});
	}

	protected static void flushThisLine(final StringBuffer buffer) {
		if (Logging.logging == null)
			return;
		try {
			final String b = buffer.toString();

			Exception e = null;
			if (buffer.toString().toLowerCase().startsWith("#error#")) {
				e = new Exception();
				e.fillInStackTrace();
			} else if (!buffer.toString().toLowerCase().startsWith("#message#"))
				return;

			if (Launcher.getLauncher().mainThread == Thread.currentThread()) {
				flushNow(b, e);
			} else {
				final Exception fe = e;
				Launcher.getLauncher().registerUpdateable(new iUpdateable() {

					public void update() {
						flushNow(b, fe);
						Launcher.getLauncher().deregisterUpdateable(this);
					}
				});
			}
		} finally {
			buffer.setLength(0);
		}
	}

	private static void flushNow(final String s, final Exception e) {

		Logging.logging.addEvent(new iLoggingEvent() {

			public String getLongDescription() {
				String m = "<html><font color=#ffffff><i>message</i><br>" + s + "<br><br>";
				String er = isError() ? "<i>stack trace</i><br>" + Arrays.asList(e.getStackTrace()).toString().replace(",", "<br>") : "";
				return m + er;
			}

			public String getReplayExpression() {
				return null;
			}

			public String getTextDescription() {
				if (e != null)
					return "<html><font bgcolor=#664444><b>error" + s.substring("#error#".length())+"</b>";
				return "<html>" + s;
			}

			public boolean isError() {
				return e != null;
			}
		});
	}

}
