package field.namespace.diagram;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.DispatchOverTopology;
import field.bytecode.protect.annotations.Inside;
import field.bytecode.protect.annotations.InsideParameter;
import field.bytecode.protect.dispatch.Cont;
import field.bytecode.protect.dispatch.mRun;
import field.launch.iUpdateable;
import field.namespace.diagram.DiagramZero.aMarkerNotify;
import field.namespace.diagram.DiagramZero.iChannel;
import field.namespace.diagram.DiagramZero.iChannelNotify;
import field.namespace.diagram.DiagramZero.iMarker;
import field.namespace.diagram.DiagramZero.iMarkerIterator;


@Woven
public class PostingChannel<T> implements iChannel<T>, iUpdateable {

	static public class ContingentMarker<T, Q> extends mRun<PostingChannel<T>> {

		protected final iMarker<? extends Q> contingentOn;

		protected T payload;

		protected boolean in = false;

		protected aMarkerNotify notify;

		protected WeakReference<PostingChannel<? super T>> ref;

		protected final Object token;

		protected boolean shouldRemove = true;

		protected int goneFor = 0;
		@SuppressWarnings("unchecked")
		public ContingentMarker(PostingChannel<? super T> postTo, Object token, iMarker<? extends Q> contingentOn, T payload) {
			super(iUpdateable.method_update);
			this.token = token;
			this.contingentOn = contingentOn;
			this.payload = payload;
			if (this.contingentOn.getRootChannel().getIterator().remaining().contains(contingentOn) && postTo.getExistingMarker(token) == null) {
				in = true;
				contingentOn.addNotify(notify = new aMarkerNotify() {
					@Override
					public void markerRemoved(iMarker thisMarker) {
						in = false;
					}
				});
			}
			ref = new WeakReference<PostingChannel<? super T>>(postTo);
			Cont.linkWith(ref.get(), method_update, this);
		}

		public void stop() {
			if (ref.get() != null)
				Cont.unlinkWith(ref.get(), method_update, this);
		}

		public void update() {

			if (!in && shouldRemove) {
				remove();
				stop();
			} else {
				cthis.touchNonMarker(token, contingentOn.getTime(), contingentOn.getDuration(), payload = updatePayload(contingentOn.getPayload(), payload));
			}
			if (!in)
				goneFor++;
		}

		protected T updatePayload(Q currentContingentPayload, T lastPayload) {
			return lastPayload;
		}
	}

	public class NonMarker {
		Object source;

		double start;

		double duration;

		T payload;
	}

	static public class PostAlways<T> extends mRun<PostingChannel<T>> {
		private final iChannel<T> channelToPost;

		WeakReference<PostingChannel<T>> ref;

		public PostAlways(PostingChannel<T> postTo, iChannel<T> channelToPost) {
			super(iUpdateable.method_update);
			this.channelToPost = channelToPost;

			ref = new WeakReference<PostingChannel<T>>(postTo);
			Cont.linkWith(ref.get(), method_update, this);
		}

		public void stop() {
			if (ref.get() != null)
				Cont.unlinkWith(ref.get(), method_update, this);
		}

		public void update() {
			cthis.touchChannel(channelToPost);
		}
	}

	@InsideParameter(group = "process")
	protected ProcessChannel<T> posting;


	protected Channel<T> channel;

	HashSet<iMarker<? extends T>> touched = new HashSet<iMarker<? extends T>>();

	HashSet<NonMarker> others = new HashSet<NonMarker>();

	public PostingChannel() {
		channel = new Channel<T>();
		posting = new ProcessChannel<T>(channel, channel);
	}

	public PostingChannel(Channel<T> channel) {
		this.channel = channel;
		posting = new ProcessChannel<T>(channel, channel);
	}

	public void addNotify(iChannelNotify notify) {
		channel.addNotify(notify);
	}

	public void beginOperation() {
		channel.beginOperation();
	}

	public void catchUpNotify(iChannelNotify<T> notify) {
		channel.catchUpNotify(notify);
	}

	public void endOperation() {
		channel.endOperation();
	}

	public List<iChannel<T>> getChildrenChannels() {
		return channel.getChildrenChannels();
	}

	public iMarker<T> getExistingMarker(Object token) {
		return posting.getExistingMarker(token);
	}

	public iMarker<T> getExistingMarker(Object token, boolean touch) {
		iMarker<T> r = posting.getExistingMarker(token);
		if (r != null && touch) {
			posting.getMarker(token, r.getTime(), r.getDuration(), null, false);
		}
		return r;
	}

	public iMarkerIterator<T> getIterator() {
		return channel.getIterator();
	}

	public iMarker<T> getMarker(Object token, double start, double duration, T point) {
		return posting.getMarker(token, start, duration, point, true);
	}

	public Channel<T> getOutputChannel() {

		return channel;
	}

	public iChannel<T> getSlice(double from, double to) {
		return channel.getSlice(from, to);
	}

	public iMarker<T> makeMarker(iChannel<T> inside, double start, double duration, T payload) {
		return channel.makeMarker(inside, start, duration, payload);
	}

	public List propagateTo(String tag, Class clazz, Method method, Object... args) {
		return channel.propagateTo(tag, clazz, method, args);
	}

	public void removeMarker(iChannel<T> from, iMarker<T> marker) {
		channel.removeMarker(from, marker);
	}

	public void removeNotify(iChannelNotify notify) {
		channel.removeNotify(notify);
	}

	public void touchChannel(iChannel<? extends T> channel) {


		touched.addAll(channel.getIterator().remaining());
	}

	public void touchMarker(iMarker<? extends T> marker) {
		touched.add(marker);
	}

	public void touchNonMarker(Object source, double start, double duration, T payload) {
		NonMarker nm = new NonMarker();
		nm.source = source;
		nm.start = start;
		nm.duration = duration;
		nm.payload = payload;
		others.add(nm);
	}

	// attach things in order to semi-perminantly post things
	@DispatchOverTopology(topology = Cont.class)
	@Inside(group = "process")
	public void update() {
		for (iMarker<? extends T> m : touched) {
			posting.getMarker(m, m.getTime(), m.getDuration(), m.getPayload(), true);
		}
		for (NonMarker nm : others) {
			posting.getMarker(nm.source, nm.start, nm.duration, nm.payload, true);
		}
		others.clear();
		touched.clear();

	}

}
