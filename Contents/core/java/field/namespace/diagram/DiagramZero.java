package field.namespace.diagram;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import field.bytecode.protect.dispatch.iContainer;
import field.namespace.generic.ReflectionTools;


/**
 * fundemental interfaces
 * 
 * for a mutable channel there needs to be a mutable notify \u2014 marker changedcalled when a marker moves position
 * 
 * @author marc Created on Sep 19, 2004 \u2014 columbia library
 */
public class DiagramZero {


	public interface iChannel<T> extends iContainer {
		public iChannel<T> getSlice(double from, double to);

		public iMarkerIterator<T> getIterator();

		public void addNotify(iChannelNotify<T> notify);

		public void catchUpNotify(iChannelNotify<T> notify);

		public void removeNotify(iChannelNotify<T> notify);

		public List<iChannel<T>> getChildrenChannels();

		public void beginOperation();

		public void endOperation();
	}

	public interface iMarker<T> {
		public double getTime();

		public double getDuration();

		public T getPayload();

		public Map<Object, iConnection> getConnections();

		public void addNotify(iMarkerNotify<? super T> notify);

		public void removeNotify(iMarkerNotify<? super T> notify);

		public iChannel<T> getRootChannel();

		public iMarkerIterator<T> getIterator();
		
		public boolean isPresent();
	}

	public interface iMutableMarker<T> extends iMarker<T> {
		public void setTime(double to);

		public void setDuration(double to);
	}

	public interface iMutableChannel<T> extends iChannel<T> {
		public iMarker<T> newMarker(double at);

		public iMarker<T> removeMarker(double at);
	}

	public interface iMarkerRef<T> {
		public iMarker<T> getMarker();

		public void addNotify(iMarkerRefNotify<T> notify);

		public void removeNotify(iMarkerRefNotify<T> notify);
	}

	public interface iConnection<A, B> {
		public iMarker<A> getMarkerUp();

		public iMarker<B> getMarkerDown();

		public void addNotify(iConnectionNotify<A, B> notify);

		public void removeNotify(iConnectionNotify<A, B> notify);

		public void invalidate(iMarker end);
		
		public boolean isValid();
	}

	public interface iMarkerIterator<T> {
		public boolean hasNext();

		public iMarker<T> next();

		public boolean hasPrevious();

		public iMarker<T> previous();

		public List<iMarker<T>> remaining();

		public iMarkerIterator<T> clone();

		// optional
		public void remove();

	}

	public interface iMarkerRefNotify<T> {
		static public final Method beginMarkerRefNotify = ReflectionTools.methodOf("beginMarkerRefNotify", iMarkerRefNotify.class);

		static public final Method endMarkerRefNotify = ReflectionTools.methodOf("endMarkerRefNotify", iMarkerRefNotify.class);

		static public final Method markerRefNowInvalid = ReflectionTools.methodOf("markerRefNowInvalid", iMarkerRefNotify.class, iMarkerRef.class);
		static public final Method markerRefNowDifferent= ReflectionTools.methodOf("markerRefNowDifferent", iMarkerRefNotify.class, iMarkerRef.class, iMarker.class);

		public void beginMarkerRefNotify();

		public void endMarkerRefNotify();

		public void markerRefNowInvalid(iMarkerRef<T> ref);

		public void markerRefNowDifferent(iMarkerRef<T> ref, iMarker<T> was);
	}

	public interface iConnectionNotify<A, B> {
		static public final Method beginConnectionNotify = ReflectionTools.methodOf("beginConnectionNotify", iConnectionNotify.class);

		static public final Method endConnectionNotify = ReflectionTools.methodOf("endConnectionNotify", iConnectionNotify.class);

		static public final Method connectionNowInvalid = ReflectionTools.methodOf("connectionNowInvalid", iConnectionNotify.class, iConnection.class);

		public void beginConnectionNotify();

		public void endConnectionNotify();

		public void connectionNowInvalid(iConnection<A, B> n);
	}

	public interface iMarkerNotify<T> {
		static public final Method beginMarkerNotify = ReflectionTools.methodOf("beginMarkerNotify", iMarkerNotify.class);

		static public final Method endMarkerNotify = ReflectionTools.methodOf("endMarkerNotify", iMarkerNotify.class);

		static public final Method markerChanged = ReflectionTools.methodOf("markerChanged", iMarkerNotify.class, iMarker.class);

		static public final Method markerRemoved = ReflectionTools.methodOf("markerRemoved", iMarkerNotify.class, iMarker.class);

		public void beginMarkerNotify();

		public void endMarkerNotify();

		public void markerChanged(iMarker<T> thisMarker);

		public void markerRemoved(iMarker<T> thisMarke);

	}

	static public class aMarkerNotify<T> implements iMarkerNotify<T> {
		public void beginMarkerNotify() {
		}

		public void endMarkerNotify() {
		}

		public void markerChanged(iMarker<T> thisMarker) {
		}

		public void markerRemoved(iMarker<T> thisMarke) {
		}

	}

	public interface iChannelNotify<T> {
		static public final Method beginMarkerNotify = ReflectionTools.methodOf("beginMarkerNotify", iChannelNotify.class);

		static public final Method endMarkerNotify = ReflectionTools.methodOf("endMarkerNotify", iChannelNotify.class);

		static public final Method markerAdded = ReflectionTools.methodOf("markerAdded", iChannelNotify.class, iMarker.class);

		static public final Method markerRemoved = ReflectionTools.methodOf("markerRemoved", iChannelNotify.class, iMarker.class);

		static public final Method markerChanged = ReflectionTools.methodOf("markerChanged", iChannelNotify.class, iMarker.class);

		public void beginMarkerNotify();

		public void endMarkerNotify();

		public void markerAdded(iMarker<T> added);

		public void markerRemoved(iMarker<T> removed);

		public void markerChanged(iMarker<T> changed);
	}

	static public class aChannelNotify<T> implements iChannelNotify<T> {
		boolean hasBegun = false;

		int openCount = 0;

		public void beginMarkerNotify() {
			if (openCount == 0) {
				hasBegun = false;
			}
			openCount++;
		}

		public void endMarkerNotify() {
			openCount--;
			if (openCount == 0) {
				if (hasBegun) {
					internalEnd();
					hasBegun = false;
				}
			}
		}

		protected void internalEnd() {
		}

		public void markerAdded(iMarker<T> added) {
			if (!hasBegun) {
				hasBegun = true;
				internalBegin();
			}
		}

		protected void internalBegin() {
		}

		public void markerRemoved(iMarker<T> removed) {
			if (!hasBegun) {
				hasBegun = true;
				internalBegin();
			}
		}

		public void markerChanged(iMarker<T> changed) {
			if (!hasBegun) {
				hasBegun = true;
				internalBegin();
			}
		}
	}

	static public interface iMarkerFactory<T> {
		public iMarker<T> makeMarker(iChannel<T> inside, double start, double duration, T payload);
		public void removeMarker(iChannel<T> from, iMarker<T> marker);
	}
}
