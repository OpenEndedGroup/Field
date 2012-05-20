package field.core.plugins.log;

import java.util.ArrayList;

import field.core.Constants;
import field.core.plugins.history.TextSearching.iProvidesSearchTerms;
import field.core.plugins.selection.PopupInfoWindow;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.graph.NodeImpl;
import field.math.graph.iMutableContainer;

public class Logging {

	public interface iLoggingEvent {
		public String getLongDescription();

		public String getReplayExpression();

		public String getTextDescription();

		public boolean isError();
	}

	public interface iLoggingSink {
		public void addEvent(iLoggingEvent event);
	}

	public interface iTranscriptCompression {
		public ArrayList<iLoggingEvent> compress(ArrayList<iLoggingEvent> events);
	}

	public interface iTranscriptViewGeneration {
		public boolean generateView(ArrayList<iLoggingEvent> transcript, LoggingEventModel root);
	}

	public interface iTranscriptViewTransformation {
		public void filterView(LoggingEventModel root);

	}

	static public class LoggingEventModel extends NodeImpl<LoggingEventModel> implements iMutableContainer<Object, LoggingEventModel>, iProvidesSearchTerms {
		LoggingEventType type;

		iLoggingEvent node;

		String label;

		public LoggingEventModel(LoggingEventType type, iLoggingEvent node, String label) {
			this.type = type;
			this.node = node;
			this.label = label;
		}

		public LoggingEventModel duplicate() {
			return new LoggingEventModel(type, node, label);
		}

		public void found(int n) {
		}

		public String[] getTerms() {
			return new String[] { label, node == null ? null : node.getLongDescription() };
		}

		public void notFound() {
		}

		public Object payload() {
			return null;
		}

		public field.math.graph.iMutableContainer setPayload(Object t) {
			return this;
		}

		@Override
		public String toString() {
			int n = this.getChildren().size();
			return type == LoggingEventType.informative ? ("<html>" + label) : (node.getTextDescription());
		}

	}

	public enum LoggingEventType {
		informative, replayable, errorWarn, errorFatal;
	}

	static public class UpdateCycle implements iLoggingEvent {
		public final long num;

		public final long time;

		public UpdateCycle(long num, long time) {
			this.num = num;
			this.time = time;
		}

		public String getLongDescription() {
			return "<html><font color='#" + Constants.defaultTreeColor + "'>" + PopupInfoWindow.title("update cycle at") + PopupInfoWindow.content("" + this.time / 1000f + " seconds") + "<BR>" + PopupInfoWindow.title("cycle number") + PopupInfoWindow.content("" + this.num);
		}

		public String getReplayExpression() {
			return "";
		}

		public String getTextDescription() {
			return "<html><font color='#" + Constants.defaultTreeColor + "'> update cycle at <i>" + this.time / 1000f + "</i> seconds";
		}

		public boolean isError() {
			return false;
		}
	}

	public enum Status {
		internal, enabled, errorsOnly, disabled;
	}

	public enum Context {
		internal, external;
	}

	private static ThreadLocal<Integer> context = new ThreadLocal<Integer>() {
		protected Integer initialValue() {
			return 0;
		}
	};

	static public Logging logging = new Logging();

	static public boolean enabled() {
		return enabled(Status.errorsOnly) && (getContext() > 0 || disabled == Status.internal);
	}

	static public boolean enabled(Status moreThan) {
		return disabled.ordinal() < moreThan.ordinal();
	}

	static public void external() {
		setContext(getContext() + 1);
	}

	static public void internal() {
		setContext(getContext() - 1);
		if (getContext() < 0) {
			setContext(0);
//			System.err.println(" warning: internal / external mismatch ");
//			new Exception().printStackTrace();
		}
	}

	static public void registerCycleUpdateable() {
		Launcher.getLauncher().addPostUpdateable(new iUpdateable() {

			long n = 0;

			long startedAt = System.currentTimeMillis();

			public void update() {
				logging.addEvent(new UpdateCycle(n, System.currentTimeMillis() - startedAt));

				if (getContext() != 0) {
					;//System.out.println(" warning: context was left as external somewhere");
					setContext(0);
				}

				n++;
			}
		});
	}

	ArrayList<iLoggingSink> sinks = new ArrayList<iLoggingSink>();
	static public Status disabled = Status.errorsOnly;

	public void addEvent(iLoggingEvent event) {
		if (event != null)
			for (int i = 0; i < sinks.size(); i++) {
				sinks.get(i).addEvent(event);
			}
	}

	public void addSink(iLoggingSink s) {
		sinks.add(s);
	}

	public void removeSink(iLoggingSink sink) {
		sinks.remove(sink);
	}

	public static void setContext(int context) {
		Logging.context.set(context);
	}

	public static int getContext() {
		return context.get();
	}

}
