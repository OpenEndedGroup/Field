package field.namespace.diagram;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.WeakHashMap;

import field.bytecode.protect.annotations.AliasedParameter;
import field.bytecode.protect.annotations.Aliases;
import field.launch.iUpdateable;
import field.math.abstraction.iDoubleProvider;
import field.namespace.diagram.DiagramZero.aChannelNotify;
import field.namespace.diagram.DiagramZero.iChannel;
import field.namespace.diagram.DiagramZero.iMarker;
import field.namespace.generic.ReflectionTools;

/**
 * classes for "complex-dispatch" deferred execution
 * 
 * @author marc Created on Jan 29, 2005 \u2014 alison
 */
public class Deferred {

	/**
	 * it's expected that this will be a good base class for things that get
	 * executed
	 */
	static public abstract class Executable implements iUpdateable, Serializable {

		public String name;

		public Object source;

		protected int consumed = 0;

		boolean isStarted = false;

		public Executable(String name, Object source) {
			this.name = name;
			this.source = source;
		}

		protected Executable() {
		}

		public void consume() {
			consumed++;
		}

		abstract public void cont();

		public String getName() {
			return name;
		};

		public Object getSource() {
			return source;
		};

		public boolean isConsumed() {
			return consumed > 0;
		}

		public boolean isStarted() {
			return isStarted;
		}

		public void start() {
			isStarted = true;
		}

		public void stop() {
			isStarted = false;
		}

		@Override
		public String toString() {
			return "exec:" + name + (isConsumed() ? "!" : "") + "(" + source + ")";
		}

		public void unconsume() {
			consumed--;
		}

		public void update() {
			if (!isConsumed()) {
				cont();
			}
		}
	}

	static public class Increment extends OneShot {
		private final Object in;

		private final Field field;

		private final Number increment;

		public Increment(String name, Object source, Object in, String field, Number value) {
			super(name, source);
			this.in = in;
			this.increment = value;
			this.field = ReflectionTools.getFirstFIeldCalled(in.getClass(), field);
			this.field.setAccessible(true);
		}

		@Override
		protected void fire() {
			try {
				Number n = (Number) field.get(in);
				double v = n.doubleValue() + increment.doubleValue();
				if (field.getType() == Double.TYPE || field.getType() == Double.class)
					field.set(in, v);
				else if (field.getType() == Float.TYPE || field.getType() == Float.class)
					field.set(in, (float) v);
				else if (field.getType() == Integer.TYPE || field.getType() == Integer.class)
					field.set(in, (int) v);
				else if (field.getType() == Long.TYPE || field.getType() == Long.class)
					field.set(in, (long) v);
				else if (field.getType() == Short.TYPE || field.getType() == Short.class)
					field.set(in, (short) v);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	static public abstract class OneShot extends Executable implements Serializable {

		private boolean first = false;

		public OneShot(String name, Object source) {
			super(name, source);
		}

		protected OneShot() {
		}

		@Override
		public void cont() {
			if (first) {
				fire();
			}
			first = false;
		}

		@Override
		public void start() {
			first = true;
		}

		abstract protected void fire();
	}

	/**
	 * use this to produce something, or not, in the future, so that it can
	 * be patterned, fused, etc.
	 * 
	 * a more complicated subclass of this would recognize things other than
	 * the ones that it created as its own
	 */
	static public class Production {

		private final iChannel into;

		private final iDoubleProvider clock;

		private float durationBetweenEvents;

		private double lastProducedAt;

		List<iMarker> created = new ArrayList<iMarker>();

		public Production(iChannel into, iDoubleProvider clock) {
			this.into = into;
			this.clock = clock;
			into.addNotify(new aChannelNotify() {

				@Override
				public void markerRemoved(DiagramZero.iMarker removed) {
					created.remove(removed);
				}
			});
		}

		public void can() {
			double now = clock.evaluate();
			if (now - lastProducedAt >= durationBetweenEvents)
				produce(now);
			for (int i = 0; i < created.size(); i++) {
				iMarker m = created.get(i);
				if (m.getTime() + m.getDuration() < now) {
					created.remove(i);
					i--;
				}
			}
		}

		public void can(double holdoff) {
			double now = clock.evaluate() + holdoff;
			if (now - lastProducedAt >= durationBetweenEvents)
				produce(now);
			now = clock.evaluate();

			for (int i = 0; i < created.size(); i++) {
				iMarker m = created.get(i);
				if (m.getTime() + m.getDuration() < now) {
					created.remove(i);
					i--;
				}
			}
		}

		public void cant() {
			double now = clock.evaluate();
			for (int i = 0; i < created.size(); i++) {
				iMarker m = created.get(i);
				if (m.getTime() > now) {
					delete(m, into);
				}
			}
			lastProducedAt = Math.min(now, lastProducedAt);
		}

		public void setDefaultParameters(float durationBetweenEvents) {
			lastProducedAt += this.durationBetweenEvents;
			this.durationBetweenEvents = durationBetweenEvents;
			lastProducedAt -= this.durationBetweenEvents;
		}

		protected boolean delete(iMarker m, iChannel from) {
			return false;
		}

		protected iMarker makeMarker(double now, iChannel into) {
			return null;
		}

		protected void produce(double now) {
			iMarker marker = makeMarker(now, into);
			if (marker != null) {
				lastProducedAt = marker.getTime() + marker.getDuration();
				created.add(marker);
			}
		}
	}

	static public class Set extends OneShot {
		private final Object in;

		private final Field field;

		private final Object value;

		public Set(String name, Object source, Object in, String field, Object value) {
			super(name, source);
			this.in = in;
			this.value = value;
			this.field = ReflectionTools.getFirstFIeldCalled(in.getClass(), field);
			this.field.setAccessible(true);
		}

		@Override
		protected void fire() {
			try {
				field.set(in, value);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	static public class SetToProgress extends Executable {

		private final Object in;

		private final Field field;

		private final Object value;

		@AliasedParameter
		double executionClock = 0;

		@AliasedParameter
		iMarker executionMarker = null;

		public SetToProgress(String name, Object source, Object in, String field, Object value) {
			super(name, source);
			this.in = in;
			this.value = value;
			this.field = ReflectionTools.getFirstFIeldCalled(in.getClass(), field);
			this.field.setAccessible(true);
		}

		@Override
		@Aliases
		public void cont() {
			set((executionClock - executionMarker.getTime()) / executionMarker.getDuration());
		}

		@Override
		public void start() {
			set(0);
		}

		@Override
		public void stop() {
			set(1);
		}

		private void set(double v) {
			try {
				if (field.getType() == Double.TYPE || field.getType() == Double.class)
					field.set(in, v);
				else if (field.getType() == Float.TYPE || field.getType() == Float.class)
					field.set(in, (float) v);
				else if (field.getType() == Integer.TYPE || field.getType() == Integer.class)
					field.set(in, (int) v);
				else if (field.getType() == Long.TYPE || field.getType() == Long.class)
					field.set(in, (long) v);
				else if (field.getType() == Short.TYPE || field.getType() == Short.class)
					field.set(in, (short) v);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	static public class SimpleExecutingHorizon extends Horizon {

		WeakHashMap<iMarker<Object>, Object> alreadySeen = new WeakHashMap<iMarker<Object>, Object>();

		HashSet<iMarker<Object>> active = new LinkedHashSet<iMarker<Object>>();

		public SimpleExecutingHorizon(iDoubleProvider nowClock, iChannel outputStream) {
			super(nowClock, outputStream);
			if (outputStream instanceof Channel)
				((Channel) outputStream).setSliceIsGreedy(true);
		}

		public void stopAll() {
			for (iMarker<Object> o : active) {
				Object p = o.getPayload();
				if (p instanceof Executable) {
					((Executable) p).stop();
				}
			}
			active.clear();
		}

		protected void start(iMarker<Object> m) {
			Object p = m.getPayload();
			if (p instanceof Executable) {
				((Executable) p).start();
			}
		}

		protected void stop(iMarker<Object> m) {
			Object p = m.getPayload();
			if (p instanceof Executable) {
				((Executable) p).stop();
			}
		}

		protected void update(iMarker<Object> m) {

			Object p = m.getPayload();

			if (p instanceof Executable) {
				((Executable) p).update();
			} else if (p instanceof iUpdateable) {
				((iUpdateable) p).update();
			}
		}

		@Override
		protected void updateWithList(double now, List l) {

			LinkedHashSet<iMarker<Object>> newSet = new LinkedHashSet<iMarker<Object>>(l);
			newSet.removeAll(alreadySeen.keySet());
			Iterator<iMarker<Object>> i = newSet.iterator();
			while (i.hasNext()) {
				// runner.addActive((float)
				// now,
				// (Promise)
				// ((DiagramZero.iMarker)
				// i.next()).getPayload());
				iMarker<Object> p = (i.next());
				active.add(p);
				start(p);
			}

			LinkedHashSet<iMarker<Object>> goneSet = new LinkedHashSet<iMarker<Object>>(alreadySeen.keySet());
			goneSet.removeAll(l);
			i = goneSet.iterator();
			while (i.hasNext()) {
				// runner.removeActive((float)
				// now,
				// (Promise)
				// ((DiagramZero.iMarker)
				// i.next()).getPayload());
				iMarker p = (i.next());
				active.remove(p);
				stop(p);
			}

			// runner.update((float)
			// now);
			i = active.iterator();
			while (i.hasNext()) {
				iMarker<Object> p = i.next();
				update(p);
			}
			alreadySeen.clear();
			for (iMarker o : (List<iMarker>) l) {
				alreadySeen.put(o, null);
			}

		}

	}

}
