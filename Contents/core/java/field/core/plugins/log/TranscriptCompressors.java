package field.core.plugins.log;

import java.util.ArrayList;
import java.util.Stack;

import field.core.plugins.log.ElementInvocationLogging.iProvidesContextStack;
import field.core.plugins.log.ElementInvocationLogging.iSuspendedContext;
import field.core.plugins.log.Logging.UpdateCycle;
import field.core.plugins.log.Logging.iLoggingEvent;
import field.core.plugins.log.Logging.iTranscriptCompression;


public class TranscriptCompressors {

	static public class MergeSuspendedContext implements Logging.iTranscriptCompression {
		public ArrayList<iLoggingEvent> compress(ArrayList<iLoggingEvent> events) {

			int q = 0;

			Stack<iLoggingEvent> currentContext = new Stack<iLoggingEvent>();

			for (int i = 0; i < events.size(); i++) {
				iLoggingEvent e = events.get(i);

				if (e instanceof iSuspendedContext) {
					iSuspendedContext s = (iSuspendedContext) e;

					Class beginMatching = s.getMatchingBegin();
					if (beginMatching != null) {
						boolean found = false;
						iLoggingEvent foundIs = null;
						// this is a contextural end, find matching begin, and, possibly, remove both
						for (int c = 0; c < currentContext.size(); c++) {
							int a = currentContext.size() - 1 - c;
							if (beginMatching.isInstance(currentContext.get(a)) && ((iSuspendedContext) currentContext.get(a)).getToken().equals(s.getToken())) {
								foundIs = currentContext.remove(a);
								found = true;
								break;
							}
						}
						//assert found : "no begining for end in stack" + s + " " + currentContext;
						if (!found) continue;
						
						if (s.isExclusivelyContextural()) {
							found = false;
							// !! this is suboptimal
							for (int c = i; c >= 0; c--) {
								if (events.get(c) == foundIs) {
									events.remove(c);
									i--;
									found = true;
									break;
								}
							}

							events.remove(i);
							i--;

							assert found : "no beginign for end in transcript" + s + " " + currentContext + " " + foundIs;
						}
					} else {
						Class endMatching = s.getMatchingEnd();
						// this is acontextural begin, push it onto the stack
						currentContext.push(e);
					}

				}
				if (e instanceof iProvidesContextStack) {
					iProvidesContextStack pc = (iProvidesContextStack) e;
				//	pc.getSuspendedContext().clear();
					pc.getSuspendedContext().addAll(currentContext);
				}
			}
			return events;
		}
	}

	static public class EventlessUpdates implements Logging.iTranscriptCompression {
		public ArrayList<iLoggingEvent> compress(ArrayList<iLoggingEvent> events) {

			int q = 0;
			for (int i = events.size() - 1; i >= 0; i--) {
				iLoggingEvent e = events.get(i);
				if (e instanceof UpdateCycle) {
					if (q == i + 1) {
						events.remove(i + 1);
					}
					if (i == 0) events.remove(0);
					q = i;
				}
			}
			return events;
		}
	}

	static public class Join implements Logging.iTranscriptCompression {
		iTranscriptCompression[] compression;

		public Join(iTranscriptCompression[] compression) {
			super();
			this.compression = compression;
		}

		public ArrayList<iLoggingEvent> compress(ArrayList<iLoggingEvent> events) {
			for (int i = 0; i < compression.length; i++) {
				events = compression[i].compress(events);
			}
			return events;
		}

	}

	static public class LastNumberOfCycles implements Logging.iTranscriptCompression {
		private final int maxCycle;

		public LastNumberOfCycles(int maxCycle) {
			this.maxCycle = maxCycle;

		}

		public ArrayList<iLoggingEvent> compress(ArrayList<iLoggingEvent> events) {
			int q = 0;
			for (int i = events.size() - 1; i >= 0; i--) {
				iLoggingEvent e = events.get(i);
				if (e instanceof UpdateCycle) {
					q++;
					if (q > maxCycle) { return new ArrayList<iLoggingEvent>(events.subList(i, events.size())); }
				}
			}
			return events;
		}
	}

	static public class MaxNumEvents implements Logging.iTranscriptCompression {
		private final int maxEvents;

		public MaxNumEvents(int maxEvents) {
			this.maxEvents = maxEvents;

		}

		public ArrayList<iLoggingEvent> compress(ArrayList<iLoggingEvent> events) {
			if (events.size() > maxEvents) return new ArrayList<iLoggingEvent>(events.subList(events.size() - maxEvents, events.size()));
			return events;
		}
	}

}
