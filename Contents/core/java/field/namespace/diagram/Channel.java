package field.namespace.diagram;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import field.bytecode.protect.dispatch.iContainer;
import field.namespace.diagram.DiagramZero.aChannelNotify;
import field.namespace.diagram.DiagramZero.iChannel;
import field.namespace.diagram.DiagramZero.iChannelNotify;
import field.namespace.diagram.DiagramZero.iConnection;
import field.namespace.diagram.DiagramZero.iMarker;
import field.namespace.diagram.DiagramZero.iMarkerIterator;
import field.namespace.diagram.DiagramZero.iMarkerNotify;
import field.namespace.generic.ReflectionTools;
import field.util.BetterWeakHashMap;
import field.util.WeakArrayList;

public class Channel<T> implements DiagramZero.iChannel<T>, DiagramZero.iMarkerFactory<T>, Iterable<iMarker<T>> {

	public static final Comparator leftComparator = new Comparator() {

		public int compare(Object o1, Object o2) {
			double f1 = o1 instanceof iMarker ? ((iMarker) o1).getTime() : ((Number) o1).doubleValue();
			double f2 = o2 instanceof iMarker ? ((iMarker) o2).getTime() : ((Number) o2).doubleValue();

			if (f2 > f1)
				return -1;
			if (f2 == f1)
				return 0;
			return 1;
		}
	};

	int beginCount = 0;

	WeakArrayList<iChannel<T>> childrenChannels = new WeakArrayList<iChannel<T>>();

	boolean indiciesAreCorrect = true;

	WeakArrayList<iChannelNotify> notifcationList = new WeakArrayList<iChannelNotify>();

	ArrayList<Marker<T>> root = new ArrayList<Marker<T>>();

	boolean sliceIsGreedy = false;

	public void addNotify(iChannelNotify notify) {
		assert notify != null;
		this.notifcationList.clean();
		this.notifcationList.add(notify);
	}

	public void beginOperation() {
		if (beginCount == 0) {
			ReflectionTools.apply(notifcationList, DiagramZero.iChannelNotify.beginMarkerNotify);
		}
		beginCount++;
	}

	public void catchUpNotify(iChannelNotify<T> notify) {
		notify.beginMarkerNotify();
		for (int i = 0; i < root.size(); i++) {
			iMarker<T> m = root.get(i);
			notify.markerAdded(m);
		}
		notify.endMarkerNotify();
	}

	public void checkIndex() {
		if (indiciesAreCorrect)
			return;
		for (int i = 0; i < root.size(); i++) {
			root.get(i).setIndex(i);
		}
		indiciesAreCorrect = true;
	}

	public void endOperation() {
		beginCount--;
		if (beginCount == 0) {
			ReflectionTools.apply(notifcationList, DiagramZero.iChannelNotify.endMarkerNotify);
		}
	}

	public List<iChannel<T>> getChildrenChannels() {
		childrenChannels.clean();
		return childrenChannels;
	}

	public iMarkerIterator<T> getIterator() {
		return new LocalMarkerIterator();
	}

	public iChannel<T> getSlice(final double from, final double to) {
		if (!sliceIsGreedy)
			return new ChannelFilter(new iMarkerPredicate<T>() {
				public boolean is(iMarker<? extends T> marker) {
					return marker.getTime() >= from && marker.getTime() < to;
				}
			}, this) {
				@Override
				protected List sublistRootStorage() {
					int left = Collections.binarySearch(channel.root, new Double(from) - 1, leftComparator);
					if (left < 0)
						left = -left - 1;
					int right = Collections.binarySearch(channel.root, new Double(to) + 1, leftComparator);
					if (right < 0)
						right = -right - 1;

					ArrayList r = new ArrayList();
					for (int i = left; (i < right + 1 && i < channel.root.size()); i++) {
						field.namespace.diagram.Channel.Marker<T> m = channel.root.get(i);
						if (predicate.is(m)) {
							r.add(m);
						}
					}

					return r;
				}
			};

		return new ChannelFilter(new iMarkerPredicate<T>() {
			public boolean is(iMarker<? extends T> marker) {
				return marker.getTime() < to && marker.getTime() + marker.getDuration() > from;
			}

			// can't use fast path sublist
		}, this);
	}

	public List propagateTo(String tag, Class clazz, Method method, Object... args) {
		if (tag.startsWith("channel"))
			return getChildrenChannels();
		else {
			return root;
		}
	}

	public void removeNotify(iChannelNotify notify) {
		this.notifcationList.remove(notify);
	}

	public Channel<T> setSliceIsGreedy(boolean sliceIsGreedy) {
		this.sliceIsGreedy = sliceIsGreedy;
		return this;
	}

	protected void add(Marker marker) {
		int index = 0;
		if (root.size() == 0)
			index = 0;
		else {
			int i = Collections.binarySearch(root, marker, leftComparator);
			if (i < 0)
				i = -i - 1;

			index = i;
		}
		root.add(index, marker);
		if (indiciesAreCorrect)
			for (int i = index; i < root.size(); i++)
				root.get(i).setIndex(i);

		beginOperation();
		ReflectionTools.apply(notifcationList, DiagramZero.iChannelNotify.markerAdded, marker);
		endOperation();
	}

	protected void becomeUnsorted(Marker marker, double old, double at) {

		checkIndex();
		int e = marker.index;
		ListIterator<Marker<T>> le = root.listIterator(e);

		Marker o = marker;
		if (old < at) {

			o = le.next();
			if (!le.hasNext()) {
				return;
			}

			// special case
			if (root.get(marker.index + 1).getTime() >= at)
				return;

			le.remove();
			while (o != null) {
				if (!le.hasNext()) {
					le.add(marker);
					break;
				}
				Marker eNext = le.next();
				if (at <= eNext.t) {
					le.previous();
					le.add(marker);
					break;
				}
				o = eNext;
			}
			for (int i = e; i < root.size(); i++)
				root.get(i).setIndex(i);
		} else {
			if (!le.hasPrevious()) {
				return;
			}
			o = le.next();
			if (root.get(marker.index - 1).getTime() <= at)
				return;

			// Marker<T> nn = le.previous();
			// if (nn.getTime()<at)
			// {
			// return;
			// }
			// le.next();

			le.remove();
			while (o != null) {
				if (!le.hasPrevious()) {
					le.add(marker);
					break;
				}
				Marker eNext = (Marker) le.previous();
				if (at >= eNext.t) {
					le.next();
					le.add(marker);
					break;
				}
				o = eNext;
			}
			for (int i = 0; i < e + 1; i++)
				root.get(i).setIndex(i);
		}

	}

	public void fireMarkerChanged(Marker marker) {
		ReflectionTools.apply(notifcationList, DiagramZero.iChannelNotify.markerChanged, marker);
	}

	protected iMarkerIterator<T> getIterator(int index) {
		return new LocalMarkerIterator(index);
	}

	protected void remove(Marker marker) {
		root.remove(marker);
		ReflectionTools.apply(notifcationList, DiagramZero.iChannelNotify.markerRemoved, marker);
	}

	public class ChannelFilter implements DiagramZero.iChannel<T> {

		protected List cachedStorage = null;

		protected Channel<T> channel;

		protected final iMarkerPredicate<T> predicate;

		boolean dirty = true;

		private BetterWeakHashMap<iChannelNotify, iChannelNotify> wrappedNotifies;

		private aChannelNotify<T> notify;

		public ChannelFilter(final iMarkerPredicate<T> predicate) {
			this.predicate = predicate;
			this.channel = Channel.this;
			channel.addNotify(notify = new aChannelNotify<T>() {
				@Override
				public void markerAdded(iMarker<T> added) {
					if (predicate.is(added)) {
						dirty = true;
					}
				}

				@Override
				public void markerChanged(iMarker<T> changed) {
					if (cachedStorage == null) {
						return;
					}

					boolean inside = cachedStorage.contains(changed);
					if (predicate.is(changed) && !inside) {
						// we need an extra added
						if (wrappedNotifies != null)
							ReflectionTools.apply(getWrappedNotifies().keySet(), iChannelNotify.markerAdded, changed);
						dirty = true;
					} else if (!predicate.is(changed) && inside) {
						if (wrappedNotifies != null)
							ReflectionTools.apply(getWrappedNotifies().keySet(), iChannelNotify.markerRemoved, changed);
						dirty = true;
					}
				}

				@Override
				public void markerRemoved(iMarker<T> removed) {
					if (predicate.is(removed))
						dirty = true;
				}
			});
		}

		
		public ChannelFilter(final iMarkerPredicate<T> predicate, Channel<T> channel) {
			this.predicate = predicate;
			this.channel = channel;
			channel.addNotify(notify = new aChannelNotify<T>() {
				@Override
				public void markerAdded(iMarker<T> added) {
					if (predicate.is(added)) {
						dirty = true;
					}
				}

				@Override
				public void markerChanged(iMarker<T> changed) {
					if (cachedStorage == null) {
						return;
					}

					boolean inside = cachedStorage.contains(changed);
					if (predicate.is(changed) && !inside) {
						// we need an extra added
						if (wrappedNotifies != null)
							ReflectionTools.apply(getWrappedNotifies().keySet(), iChannelNotify.markerAdded, changed);
						dirty = true;
					} else if (!predicate.is(changed) && inside) {
						if (wrappedNotifies != null)
							ReflectionTools.apply(getWrappedNotifies().keySet(), iChannelNotify.markerRemoved, changed);
						dirty = true;
					}
				}

				@Override
				public void markerRemoved(iMarker<T> removed) {
					if (predicate.is(removed))
						dirty = true;
				}
			});
		}

		public ChannelFilter(final iMarkerPredicate<T> predicate, final ChannelFilter channel) {
			this.predicate = new iMarkerPredicate<T>() {
				public boolean is(iMarker<? extends T> marker) {
					return predicate.is(marker) && channel.predicate.is(marker);
				}
			};
			this.channel = channel.channel;
			channel.addNotify(notify = new aChannelNotify<T>() {
				@Override
				public void markerAdded(iMarker<T> added) {
					if (ChannelFilter.this.predicate.is(added))
						dirty = true;
				}

				@Override
				public void markerChanged(iMarker<T> changed) {
					if (cachedStorage == null)
						return;

					boolean inside = cachedStorage.contains(changed);
					if (predicate.is(changed) && !inside) {
						// we need an extra added
						if (wrappedNotifies != null)
							ReflectionTools.apply(getWrappedNotifies().keySet(), iChannelNotify.markerAdded, changed);
						dirty = true;
					} else if (!predicate.is(changed) && inside) {
						if (wrappedNotifies != null)
							ReflectionTools.apply(getWrappedNotifies().keySet(), iChannelNotify.markerRemoved, changed);
						dirty = true;
					}
				}

				@Override
				public void markerRemoved(iMarker<T> removed) {
					if (ChannelFilter.this.predicate.is(removed))
						dirty = true;
				}
			});
		}

		public void addNotify(final iChannelNotify notify) {
			iChannelNotify wrapped = wrap(notify);

			getWrappedNotifies().put(notify, wrapped);
			channel.addNotify(wrapped);
		}

		public void beginOperation() {
			Channel.this.beginOperation();
		}

		public void catchUpNotify(iChannelNotify notify) {
			channel.catchUpNotify(wrap(notify));
		}

		public void endOperation() {
			Channel.this.endOperation();
		}

		public List getChildrenChannels() {
			return channel.getChildrenChannels();
		}

		public iMarkerIterator<T> getIterator() {
			checkCache();
			return new LocalMarkerIterator();
		}

		public iChannel<T> getSlice(final double from, final double to) {
			checkCache();
			return new ChannelFilter(new iMarkerPredicate<T>() {
				public boolean is(iMarker<? extends T> marker) {
					return predicate.is(marker) && marker.getTime() + marker.getDuration() > from && marker.getTime() < to;
				}
			}, this);
		}

		public List propagateTo(String tag, Class clazz, Method method, Object... args) {
			if (tag.startsWith("channel")) {
				return channel.getChildrenChannels();
			} else {
				checkCache();
				return cachedStorage;
			}
		}

		public void removeNotify(iChannelNotify notify) {
			channel.removeNotify(unwrap(notify));
		}

		private void checkCache() {
			if (dirty) {
				dirty = false;
				cachedStorage = sublistRootStorage();
			}
		}

		protected List sublistRootStorage() {

			// for slices, this needs to be
			// overridden in the subclass to
			// use binary search

			ArrayList r = new ArrayList();
			iMarkerIterator mi = channel.getIterator();
			while (mi.hasNext()) {
				iMarker m = mi.next();
				if (predicate.is(m)) {
					r.add(m);
				}
			}
			return r;
		}

		protected iChannelNotify unwrap(iChannelNotify notify) {
			return (iChannelNotify) getWrappedNotifies().get(notify);
		}

		protected iChannelNotify wrap(final iChannelNotify notify) {
			return new iChannelNotify() {

				boolean first = true;

				public void beginMarkerNotify() {
					first = true;
				}

				public void endMarkerNotify() {
					if (!first)
						notify.endMarkerNotify();
					first = true;
				}

				public void markerAdded(iMarker added) {
					if (predicate.is(added)) {
						if (first) {
							first = false;
							notify.beginMarkerNotify();
						}
						notify.markerAdded(added);
					}
				}

				public void markerChanged(iMarker changed) {
					if (predicate.is(changed)) {
						if (first) {
							first = false;
							notify.beginMarkerNotify();
						}
						notify.markerChanged(changed);
					}
				}

				public void markerRemoved(iMarker removed) {
					if (predicate.is(removed)) {
						if (first) {
							first = false;
							notify.beginMarkerNotify();
						}
						notify.markerRemoved(removed);
					}
				}
			};
		}

		void setWrappedNotifies(BetterWeakHashMap<iChannelNotify, iChannelNotify> wrappedNotifies) {
			this.wrappedNotifies = wrappedNotifies;
		}

		BetterWeakHashMap<iChannelNotify, iChannelNotify> getWrappedNotifies() {
			return wrappedNotifies == null ? wrappedNotifies = new BetterWeakHashMap<iChannelNotify, iChannelNotify>() : wrappedNotifies;
		}

		private final class LocalMarkerIterator implements iMarkerIterator<T> {
			private iMarker lastRet;

			int at = 0;

			ListIterator i = cachedStorage.listIterator();

			@Override
			public iMarkerIterator<T> clone() {
				LocalMarkerIterator i = new LocalMarkerIterator();
				i.i = cachedStorage.listIterator(at);
				return i;
			}

			public boolean hasNext() {
				return i.hasNext();
			}

			public boolean hasPrevious() {
				return i.hasPrevious();
			}

			public iMarker next() {
				at++;
				lastRet = (iMarker) i.next();
				return lastRet;
			}

			public iMarker<T> previous() {
				at--;
				lastRet = (Marker) i.previous();
				return lastRet;
			}

			public List remaining() {
				return new ArrayList(cachedStorage.subList(at, cachedStorage.size()));
			}

			public void remove() {
				throw new IllegalArgumentException(" remove not implemented on slice");
			}
		}
	}

	public interface iMarkerPredicate<T> {
		public boolean is(iMarker<? extends T> marker);
	}

	static public class Marker<T> implements DiagramZero.iMarker<T>, iContainer {

		protected Channel<T> channel;

		protected double duration;

		protected T payload;

		protected double t;

		HashMap<Object, iConnection> connections = new HashMap<Object, iConnection>();

		int index = 0;

		WeakArrayList notify = new WeakArrayList();

		Map<Object, iConnection> uconnections = Collections.unmodifiableMap(connections);

		public Marker(double at, double duration, T payload, Channel<T> channel) {
			this.channel = channel;
			this.t = at;
			this.duration = duration;

			this.payload = payload;
			channel.add(this);
		}

		public Marker(float at, float duration, T payload, Channel<T>.ChannelFilter channel) {
			this.channel = channel.channel;
			this.t = at;
			this.duration = duration;

			this.payload = payload;
			this.channel.add(this);
		}

		public void addConnection(Object label, iConnection c) {
			Object o = connections.put(label, c);
			if (o != null && !o.equals(c)) {

				if (o != null) {
					((iConnection) o).invalidate(this);
				}

				ReflectionTools.apply(notify, iMarkerNotify.beginMarkerNotify);
				ReflectionTools.apply(notify, iMarkerNotify.markerChanged, this);
				ReflectionTools.apply(notify, iMarkerNotify.endMarkerNotify);
				channel.fireMarkerChanged(this);
			}
		}

		public void addNotify(iMarkerNotify notify) {
			this.notify.add(notify);
		}

		public Map<Object, iConnection> getConnections() {
			return uconnections;
		}

		public double getDuration() {
			return duration;
		}

		public iMarkerIterator<T> getIterator() {
			channel.checkIndex();
			return channel.new LocalMarkerIterator(index);
		}

		public T getPayload() {
			return payload;
		}

		public iChannel getRootChannel() {
			return channel;
		}

		public double getTime() {
			return t;
		}

		public void remove() {
			if (channel==null) return;
			
			channel.remove(this);

			ReflectionTools.apply(notify, iMarkerNotify.beginMarkerNotify);
			ReflectionTools.apply(notify, iMarkerNotify.markerRemoved, this);
			ReflectionTools.apply(notify, iMarkerNotify.endMarkerNotify);

			// should this automatically disconnect from everything?

			// this needs to update the indicies correctly
			channel.indiciesAreCorrect = false;

			// bold and new
			//channel = null;
		}
		
		public boolean isPresent()
		{
			return channel!=null;
		}

		public void removeConnection(Object relationshipFrom) {
			Object o = connections.remove(relationshipFrom);
			if (o != null) {
				ReflectionTools.apply(notify, iMarkerNotify.beginMarkerNotify);
				ReflectionTools.apply(notify, iMarkerNotify.markerChanged, this);
				ReflectionTools.apply(notify, iMarkerNotify.endMarkerNotify);

				channel.fireMarkerChanged(this);
			}
		}

		public void removeNotify(iMarkerNotify notify) {
			this.notify.remove(notify);
		}

		public void setDuration(double at) {
			boolean changed = this.duration != at;
			this.duration = at;
			if (changed) {
				ReflectionTools.apply(notify, iMarkerNotify.beginMarkerNotify);
				ReflectionTools.apply(notify, iMarkerNotify.markerChanged, this);
				ReflectionTools.apply(notify, iMarkerNotify.endMarkerNotify);

				channel.fireMarkerChanged(this);
			}
		}

		public void setIndex(int index) {
			this.index = index;
		}

		public void setPayload(T o) {
			boolean changed = payload != o;
			payload = o;
			if (changed) {
				ReflectionTools.apply(notify, iMarkerNotify.beginMarkerNotify);
				ReflectionTools.apply(notify, iMarkerNotify.markerChanged, this);
				ReflectionTools.apply(notify, iMarkerNotify.endMarkerNotify);
				channel.fireMarkerChanged(this);
			}
		}

		public void setStart(double at) {
			double old = this.t;
			boolean changed = old != at;
			this.t = at;
			if (changed) {
				ReflectionTools.apply(notify, iMarkerNotify.beginMarkerNotify);
				ReflectionTools.apply(notify, iMarkerNotify.markerChanged, this);
				ReflectionTools.apply(notify, iMarkerNotify.endMarkerNotify);

				channel.becomeUnsorted(this, old, at);
				channel.fireMarkerChanged(this);
			}
		}

		@Override
		public String toString() {
			return "m:" + t + "+" + duration + "=" + payload;
		}

		public List propagateTo(String tag, Class clazz, Method method, Object... args) {
			return Collections.singletonList(getPayload());
		}
	}

	protected class LocalMarkerIterator implements iMarkerIterator<T> {
		private Marker lastReturned;

		int at = 0;

		ListIterator<Marker<T>> i = root.listIterator();

		public LocalMarkerIterator() {
			i = root.listIterator();
		}

		public LocalMarkerIterator(int index) {
			i = root.listIterator(index);
			at = index;
		}

		@Override
		public iMarkerIterator<T> clone() {
			LocalMarkerIterator i = new LocalMarkerIterator(at);
			return i;
		}

		public boolean hasNext() {
			return i.hasNext();
		}

		public boolean hasPrevious() {
			return i.hasPrevious();
		}

		public iMarker<T> next() {
			at++;
			lastReturned = (Marker) i.next();
			return lastReturned;
		}

		public iMarker<T> previous() {
			at--;
			lastReturned = (Marker) i.previous();
			return lastReturned;
		}

		public List<iMarker<T>> remaining() {
			
			// crazy experiemental code
			if (at == 0) return (List)root;
			
			return new ArrayList<iMarker<T>>(at == 0 ? root : root.subList(at, root.size()));
		}

		public void remove() {
			i.remove();
			lastReturned.remove();
		}
	}

	public iMarker<T> makeMarker(iChannel<T> inside, double start, double duration, T payload) {
		assert inside instanceof Channel : inside.getClass();
		return new Marker<T>(start, duration, payload, (Channel<T>) inside);
	}

	public void removeMarker(iChannel<T> from, iMarker<T> marker) {
		assert marker instanceof Marker : marker.getClass();
		((Marker) marker).remove();
	}

	public Iterator<iMarker<T>> iterator() {
		return getIterator().remaining().iterator();
	}


}