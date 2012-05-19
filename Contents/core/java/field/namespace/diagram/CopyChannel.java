package field.namespace.diagram;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import field.namespace.diagram.Channel.Marker;
import field.namespace.diagram.DiagramZero.aChannelNotify;
import field.namespace.diagram.DiagramZero.iChannel;
import field.namespace.diagram.DiagramZero.iChannelNotify;
import field.namespace.diagram.DiagramZero.iMarker;
import field.namespace.diagram.DiagramZero.iMarkerFactory;


/**
 * maintains one channel as a copySource of another --- goes potentially both ways
 *
 * @author marc
 *
 */
public class CopyChannel<t_left, t_right> implements iMapsMarkers<t_left, t_right> {

	static public class StraightCopy<t_both> extends CopyChannel<t_both, t_both> {

		public StraightCopy(iChannel<t_both> left, iMarkerFactory<t_both> leftFactory, iChannel<t_both> right, iMarkerFactory<t_both> rightFactory) {
			super(left, leftFactory, right, rightFactory);
		}

		@Override
		protected iMarker<t_both> createLeftToRight(iMarker<t_both> added) {
			if (rightFactory != null)
				return rightFactory.makeMarker(right, added.getTime(), added.getDuration(), added.getPayload());
			return null;
		}

		@Override
		protected iMarker<t_both> createRightToLeft(iMarker<t_both> added) {
			if (leftFactory != null)
				return leftFactory.makeMarker(left, added.getTime(), added.getDuration(), added.getPayload());
			return null;
		}

		@Override
		protected iMarker<t_both> updateLeftToRight(iMarker<t_both> changed, iMarker<t_both> counterpart) {
			if (counterpart instanceof Marker) {
				((Marker<t_both>) counterpart).setStart(changed.getTime());
				((Marker<t_both>) counterpart).setDuration(changed.getDuration());
			}
			return counterpart;
		}

		@Override
		protected iMarker<t_both> updateRightToLeft(iMarker<t_both> changed, iMarker<t_both> counterpart) {
			if (counterpart instanceof Marker) {
				((Marker<t_both>) counterpart).setStart(changed.getTime());
				((Marker<t_both>) counterpart).setDuration(changed.getDuration());
			}
			return counterpart;
		}
	}

	private final iChannelNotify<t_left> leftNotify;

	private final iChannelNotify<t_right> rightNotify;

	protected final iChannel<t_left> left;

	protected final iMarkerFactory<t_left> leftFactory;

	protected final iChannel<t_right> right;

	protected final iMarkerFactory<t_right> rightFactory;

	protected boolean leftInsideAdd = false;

	protected boolean leftInsideChange = false;

	protected boolean leftInsideRemove = false;

	protected boolean rightInsideAdd = false;

	protected boolean rightInsideChange = false;

	protected boolean rightInsideRemove = false;

	Map<iMarker<t_left>, iMarker<t_right>> forwardsMap = new HashMap<iMarker<t_left>, iMarker<t_right>>();

	Map<iMarker<t_right>, iMarker<t_left>> backwardsMap = new HashMap<iMarker<t_right>, iMarker<t_left>>();

	Set<iMarker<t_left>> ownedByLeft = new HashSet<iMarker<t_left>>();

	Set<iMarker<t_right>> ownedByRight = new HashSet<iMarker<t_right>>();

	/**
	 */
	public CopyChannel(iChannel<t_left> left, iMarkerFactory<t_left> leftFactory, iChannel<t_right> right, iMarkerFactory<t_right> rightFactory) {
		this.left = left;
		this.leftFactory = leftFactory;
		this.right = right;
		this.rightFactory = rightFactory;

		assert right.getIterator().remaining().size() == 0;

		left.addNotify(leftNotify = makeLeftNotify());
		left.catchUpNotify(leftNotify);

		right.addNotify(rightNotify = makeRightNotify());
		right.catchUpNotify(rightNotify);

	}

	public void collapse()
	{
		left.removeNotify(leftNotify);
		right.removeNotify(rightNotify);
	}

	public iMarker<t_right> mapLeftToRight(iMarker<t_left> left) {
		return forwardsMap.get(left);
	}

	public iMarker<t_left> mapRightToLeft(iMarker<t_right> right) {
		return backwardsMap.get(right);
	}

	public void updateLeftToRightAll() {
		leftInsideChange = true;
		try {
			Set<Entry<iMarker<t_left>, iMarker<t_right>>> forward = forwardsMap.entrySet();
			for (Entry<iMarker<t_left>, iMarker<t_right>> e : forward) {
				if (e.getKey().isPresent())
				updateLeftToRight(e.getKey(), e.getValue());
			}
		} finally {
			leftInsideChange = false;
		}
	}

	protected iMarker<t_right> createLeftToRight(iMarker<t_left> added) {
		return null;
	}

	protected iMarker<t_left> createRightToLeft(iMarker<t_right> added) {
		return null;
	}

	protected boolean isOwnedByLeft(iMarker<t_left> left) {
		return ownedByLeft.contains(left);
	}

	protected boolean isOwnedByRight(iMarker<t_right> right) {
		return ownedByRight.contains(right);
	}

	protected iChannelNotify<t_left> makeLeftNotify() {
		return new aChannelNotify<t_left>() {
			@Override
			public void markerAdded(iMarker<t_left> added) {
				if (leftInsideAdd) {
					return;
				}
				leftInsideAdd = true;
				try {
					super.markerAdded(added);
					if (!forwardsMap.containsKey(added)) {
						if (!rightInsideAdd) {
							iMarker<t_right> created = createLeftToRight(added);

							ownedByLeft.add(added);

							if (created != null) {
								forwardsMap.put(added, created);
								backwardsMap.put(created, added);
							}
						} else {
						}
					} else {
						markerChanged(added);
					}
				} finally {
					leftInsideAdd = false;
				}
			}

			@Override
			public void markerChanged(iMarker<t_left> changed) {
				if (leftInsideChange) {
					return;
				}
				leftInsideChange = true;
				try {
					super.markerChanged(changed);
					iMarker<t_right> counterpart = forwardsMap.get(changed);
					if (counterpart != null) {
						if (!rightInsideChange && changed.isPresent()) {
							iMarker<t_right> created = updateLeftToRight(changed, counterpart);
							if (created != counterpart) {
								forwardsMap.remove(changed);
								backwardsMap.remove(counterpart);
								forwardsMap.put(changed, created);
								backwardsMap.put(created, changed);
							}
						} else {
						}
					} else {
						markerAdded(changed);
					}
				} finally {
					leftInsideChange = false;
				}
			}

			@Override
			public void markerRemoved(iMarker<t_left> removed) {
				if (leftInsideRemove)
					return;
				leftInsideRemove = true;
				try {
					super.markerRemoved(removed);
					iMarker<t_right> counterpart = forwardsMap.get(removed);
					if (counterpart != null) {
						removeLeftToRight(removed, counterpart);
						forwardsMap.remove(removed);
						backwardsMap.remove(counterpart);
					}
					ownedByLeft.remove(removed);
				} finally {
					leftInsideRemove = false;
				}
			}

			@Override
			protected void internalBegin() {
				super.internalBegin();
				right.beginOperation();
			}

			@Override
			protected void internalEnd() {
				super.internalEnd();
				right.endOperation();
			}
		};
	}

	protected iChannelNotify<t_right> makeRightNotify() {
		return new aChannelNotify<t_right>() {
			@Override
			public void markerAdded(iMarker<t_right> added) {
				if (rightInsideAdd)
					return;
				rightInsideAdd = true;
				try {
					super.markerAdded(added);
					if (!backwardsMap.containsKey(added)) {
						if (!leftInsideAdd) {
							iMarker<t_left> created = createRightToLeft(added);
							if (created != null) {
								backwardsMap.put(added, created);
								forwardsMap.put(created, added);
								ownedByRight.add(added);
							}
						}
					} else {
						markerChanged(added);
					}
				} finally {
					rightInsideAdd = false;
				}
			}

			@Override
			public void markerChanged(iMarker<t_right> changed) {
				if (rightInsideChange)
					return;
				rightInsideChange = true;
				try {
					super.markerChanged(changed);
					iMarker<t_left> counterpart = backwardsMap.get(changed);
					if (counterpart != null) {
						//if (!leftInsideChange)
						if (changed.isPresent())
						{
							iMarker<t_left> created = updateRightToLeft(changed, counterpart);
							if (created != counterpart) {
								forwardsMap.remove(changed);
								backwardsMap.remove(counterpart);
								backwardsMap.put(changed, created);
								forwardsMap.put(created, changed);
							}
						}
					} else {
						markerAdded(changed);
					}
				} finally {
					rightInsideChange = false;
				}
			}

			@Override
			public void markerRemoved(iMarker<t_right> removed) {
				if (rightInsideRemove)
					return;
				try {
					rightInsideRemove = true;
					super.markerRemoved(removed);
					iMarker<t_left> counterpart = backwardsMap.get(removed);
					if (counterpart != null) {
						removeRightToLeft(removed, counterpart);
						backwardsMap.remove(removed);
						forwardsMap.remove(counterpart);
					}
					ownedByRight.remove(removed);
				} finally {
					rightInsideRemove = false;
				}
			}

			@Override
			protected void internalBegin() {
				super.internalBegin();
				left.beginOperation();
			}

			@Override
			protected void internalEnd() {
				super.internalEnd();
				left.endOperation();
			}
		};

	}

	protected void removeLeftToRight(iMarker<t_left> removed, iMarker<t_right> counterpart) {
		if (rightFactory != null)
			rightFactory.removeMarker(right, counterpart);
	}

	protected void removeRightToLeft(iMarker<t_right> removed, iMarker<t_left> counterpart) {
		if (leftFactory != null)
			leftFactory.removeMarker(left, counterpart);
	}

	protected iMarker<t_right> updateLeftToRight(iMarker<t_left> changed, iMarker<t_right> counterpart) {
		return counterpart;
	}


	protected iMarker<t_left> updateRightToLeft(iMarker<t_right> changed, iMarker<t_left> counterpart) {
		return counterpart;
	}

}
