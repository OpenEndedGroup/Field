package field.core.plugins.drawing;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.drawing.BasicDrawingPlugin.iDragParticipant;
import field.core.plugins.drawing.align.HorizontalBottomToBottomAlign;
import field.core.plugins.drawing.align.HorizontalBottomToTopAlign;
import field.core.plugins.drawing.align.HorizontalCenterToHorizontalCenter;
import field.core.plugins.drawing.align.HorizontalTopToBottomAlign;
import field.core.plugins.drawing.align.HorizontalTopToTopAlign;
import field.core.plugins.drawing.align.VerticalCenterToVerticalCenter;
import field.core.plugins.drawing.align.VerticalLeftToLeftAlign;
import field.core.plugins.drawing.align.VerticalLeftToRightAlign;
import field.core.plugins.drawing.align.VerticalRightToLeftAlign;
import field.core.plugins.drawing.align.VerticalRightToRightAlign;
import field.core.windowing.components.RootComponent;
import field.core.windowing.components.DraggableComponent.Resize;
import field.core.windowing.components.RootComponent.iPaintPeer;
import field.launch.Launcher;
import field.launch.SystemProperties;
import field.launch.iUpdateable;
import field.math.graph.TopologySearching;
import field.math.graph.TopologyViewOfGraphNodes;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.graph.TopologySearching.TopologyVisitor_breadthFirst;
import field.util.HashMapOfLists;

/**
 * receives feedback in the dragging process to both offer alignment (i.e.
 * change the ultimate position of the dragged thing) and annotations of those
 * constraints
 * 
 * registers as a drag participant with the root component
 * 
 */

public class OfferedAlignment implements iDragParticipant, iPaintPeer {

	static public VisualElementProperty<DynamicAlignment> guides = new VisualElementProperty<DynamicAlignment>(
			"guides_");

	public enum ConstraintOrigin {
		dimensionX(Resize.translate), dimensionLW(Resize.left), dimensionRW(
				Resize.right), dimensionY(Resize.translate), dimensionTH(
				Resize.up), dimensionBH(Resize.down),

		centerX(Resize.left, Resize.right, Resize.translate), centerY(
				Resize.up, Resize.down, Resize.left,
				Resize.right, Resize.translate), center(
				Resize.up, Resize.down, Resize.left,
				Resize.right, Resize.translate), width(
				Resize.left, Resize.right), height(Resize.up,
				Resize.down);

		private ConstraintOrigin(Resize... from) {
			for (Resize r : from)
				implication.addToList(r, this);
		}
	}

	public interface iDrawable {
		public void draw(OfferedAlignment alignment);

		public float getScore();

		public String getToken();

		public boolean hasStopped();

		public boolean isStopping();

		public void process(Rect currentrect, Rect newRect);

		public boolean restart();

		public void stop();

		public void update(Rect currentrect, Rect newRect);

		public iDrawable merge(iDrawable d);

	}

	public interface iOffering {
		public void createConstraint(iVisualElement root,
				LinkedHashMap<iVisualElement, Rect> current,
				iVisualElement element, Rect or,
				Rect originalRect, Rect currentRect);

		public iDrawable score(
				LinkedHashMap<iVisualElement, Rect> current,
				iVisualElement element, Rect originalRect,
				Rect currentRect, Rect newRect);

	}

	public static final VisualElementProperty<Boolean> alignment_doNotParticipate = new VisualElementProperty<Boolean>(
			"alignment_doNotParticipate");

	static HashMapOfLists<Resize, ConstraintOrigin> implication = new HashMapOfLists<Resize, ConstraintOrigin>();

	// public DynamicLine thickLine = null;
	//
	// public DynamicLine thinLine = null;
	//
	// public DynamicLine hairLine = null;

	// public DynamicPointlist thickPoint = null;
	//
	// public DynamicPointlist leftArrows = null;
	//
	// public DynamicPointlist rightArrows = null;
	//
	// public DynamicPointlist upArrows = null;
	//
	// public DynamicPointlist downArrows = null;

	private final iVisualElement root;

	HashMap<iVisualElement, Set<Resize>> resizingOngoing = new LinkedHashMap<iVisualElement, Set<Resize>>();

	HashMap<iVisualElement, Set<ConstraintOrigin>> constraintsOngoing = new LinkedHashMap<iVisualElement, Set<ConstraintOrigin>>();

	HashMapOfLists<ConstraintOrigin, iOffering> offerings = new HashMapOfLists<ConstraintOrigin, iOffering>();

	LinkedHashMap<iVisualElement, Rect> currentFrames = new LinkedHashMap<iVisualElement, Rect>();

	LinkedHashMap<iVisualElement, Rect> currentFramesMinusOngoing = new LinkedHashMap<iVisualElement, Rect>();

	LinkedHashMap<String, iDrawable> drawQueue = new LinkedHashMap<String, iDrawable>();

	HashSet<String> touch = new HashSet<String>();
	int touchCount = 0;
	boolean stopAllNext = false;

	public OfferedAlignment(iVisualElement root) {
		this.root = root;

		Launcher.getLauncher().registerUpdateable(new iUpdateable() {
			int last = touchCount;

			public void update() {
				if (last != touchCount)
					cullUntouched();

				last = touchCount;
				if (stopAllNext) {
					for (iDrawable d : drawQueue.values()) {
						d.stop();
					}

					stopAllNext = false;
				}
			}
		});

		// install default offers

		ConstraintOrigin.values();
		
		if (SystemProperties.getIntProperty("useDefaultConstraints", 1) == 1) {
			offerings.addToList(ConstraintOrigin.dimensionX,
					new VerticalLeftToLeftAlign(2));
			offerings.addToList(ConstraintOrigin.dimensionX,
					new VerticalRightToRightAlign(1.5f));
			offerings.addToList(ConstraintOrigin.dimensionX,
					new VerticalLeftToRightAlign(1.25f));
			offerings.addToList(ConstraintOrigin.dimensionX,
					new VerticalRightToLeftAlign(1.25f));

			offerings.addToList(ConstraintOrigin.dimensionY,
					new HorizontalTopToTopAlign(2));
			offerings
					.addToList(
							ConstraintOrigin.dimensionY,
							new HorizontalBottomToBottomAlign(
									1.5f));
			offerings.addToList(ConstraintOrigin.dimensionY,
					new HorizontalTopToBottomAlign(1.25f));
			offerings.addToList(ConstraintOrigin.dimensionY,
					new HorizontalBottomToTopAlign(1.25f));

			offerings.addToList(ConstraintOrigin.dimensionLW,
					new VerticalLeftToLeftAlign.Resize(1));
			offerings.addToList(ConstraintOrigin.dimensionRW,
					new VerticalRightToLeftAlign.Resize(
							0.25f));
			offerings.addToList(ConstraintOrigin.dimensionLW,
					new VerticalLeftToRightAlign.Resize(
							0.25f));
			offerings.addToList(ConstraintOrigin.dimensionRW,
					new VerticalRightToRightAlign.Resize(
							0.75f));

			offerings.addToList(ConstraintOrigin.dimensionTH,
					new HorizontalTopToTopAlign.Resize(1));
			offerings
					.addToList(
							ConstraintOrigin.dimensionBH,
							new HorizontalBottomToBottomAlign.Resize(
									0.25f));
			offerings.addToList(ConstraintOrigin.dimensionTH,
					new HorizontalTopToBottomAlign.Resize(
							0.25f));
			offerings.addToList(ConstraintOrigin.dimensionBH,
					new HorizontalBottomToTopAlign.Resize(
							0.75f));

			offerings.addToList(ConstraintOrigin.centerX,
					new VerticalCenterToVerticalCenter(1));
			offerings.addToList(ConstraintOrigin.centerY,
					new HorizontalCenterToHorizontalCenter(
							1));
			offerings
					.addToList(
							ConstraintOrigin.width,
							new VerticalCenterToVerticalCenter.Resize(
									0.5f));
			offerings
					.addToList(
							ConstraintOrigin.height,
							new HorizontalCenterToHorizontalCenter.Resize(
									0.5f));
		}
	}

	public void addOffering(iOffering o, ConstraintOrigin... origins) {
		for (ConstraintOrigin c : origins) {
			offerings.addToList(c, o);
		}
	}

	public void addTestOffering(iOffering o) {
		for (ConstraintOrigin c : ConstraintOrigin.values())
			addOffering(o, c);
	}

	public HashMapOfLists<ConstraintOrigin, iOffering> getAdditionalOfferings(
			iVisualElement forElement) {
		HashMapOfLists<ConstraintOrigin, iOffering> r = new HashMapOfLists<ConstraintOrigin, iOffering>();
		List<DynamicAlignment> t = guides.accumulateList(forElement);

		for (Object tt : t) {
			if (tt instanceof DynamicAlignment) { // NICK: hole
								// here: Python
								// can inject
								// past the type
								// system.
				for (ConstraintOrigin c : ConstraintOrigin
						.values())
					r.addToList(c, (DynamicAlignment) tt);
			} else {
				System.err
						.println("*** alignment: wrong type for dynamic alignment, found "
								+ tt.getClass());
			}
		}
		return r;
	}

	public void beginDrag(Set<Resize> resizeType, iVisualElement element,
			Rect originalRect, int modifiers) {

		if (isTrue(element.getProperty(alignment_doNotParticipate)))
			return;

		Set<Resize> r = resizingOngoing.get(element);
		if (r == null)
			resizingOngoing.put(element, r = new HashSet<Resize>(
					resizeType));
		else
			r.addAll(resizeType);

		Set<ConstraintOrigin> c = constraintsOngoing.get(element);
		if (c == null)
			constraintsOngoing.put(element,
					c = new HashSet<ConstraintOrigin>());
		for (Resize rr : r)
			c.addAll(implication.get(rr));
		updateAll();
	}

	public void endDrag(Set<Resize> resizeType, iVisualElement element,
			Rect inOutRect, boolean createConstraint, int modifiers) {
		if (isTrue(element.getProperty(alignment_doNotParticipate)))
			return;

		;//System.out.println(" ending drag <" + inOutRect + ">");

		interpretRect(element,
				new Rect(0, 0, 0, 0).setValue(inOutRect),
				inOutRect, false, createConstraint);

		;//System.out.println(" drag ended <" + inOutRect + ">");

		Set<Resize> r = resizingOngoing.remove(element);
		Set<ConstraintOrigin> c = constraintsOngoing.remove(element);

		stopAllNext = true;

	}

	public void stopAll() {
		this.stopAllNext = true;
	}

	public void interpretRect(iVisualElement element, Rect originalRect,
			Rect currentRect) {
		if (isTrue(element.getProperty(alignment_doNotParticipate)))
			return;
		this.interpretRect(element, originalRect, currentRect, true,
				false);
	}

	private boolean isTrue(Boolean property) {
		if (property == null)
			return false;
		return property;
	}

	public void interpretRect(iVisualElement element, Rect originalRect,
			Rect currentRect, boolean update,
			boolean createConstraint) {
		if (isTrue(element.getProperty(alignment_doNotParticipate)))
			return;

		if (currentFrames.get(element) == null)
			return;

		touchCount++;

		Rect or = new Rect(0, 0, 0, 0).setValue(currentFrames
				.get(element));
		Set<ConstraintOrigin> ongoing = constraintsOngoing.get(element);
		float bestScore = Float.NEGATIVE_INFINITY;
		Set<iDrawable> bestDrawable = new HashSet<iDrawable>();
		Set<iOffering> bestOffer = new HashSet<iOffering>();

		HashMapOfLists<ConstraintOrigin, iOffering> offeringsHere = new HashMapOfLists<ConstraintOrigin, iOffering>();
		offeringsHere.putAll(offerings);
		offeringsHere.putAll(getAdditionalOfferings(element));

		if (ongoing != null) {
			for (ConstraintOrigin co : ongoing) {
				Collection<iOffering> col = offeringsHere
						.getCollection(co);
				if (col != null)
					for (iOffering o : col) {
						iDrawable s = null;
						try { // NICK.
							s = o
									.score(
											currentFramesMinusOngoing,
											element,
											or,
											originalRect,
											currentRect);
						} catch (Exception exn) { // Perhaps
										// from
										// Python
										// monkey-patched
										// alignment
										// calculations?
							exn.printStackTrace();
						}
						if (s == null)
							continue;

						float sc = s.getScore();
						if (sc > bestScore) {
							bestDrawable.clear();
							bestDrawable.add(s);
							bestScore = sc;
							bestOffer.clear();
							bestOffer.add(o);
						} else if (sc == bestScore
								&& bestScore > Float.NEGATIVE_INFINITY) {
							bestDrawable.add(s);
							bestOffer.add(o);
						}
					}
			}

			Rect ore = new Rect(0, 0, 0, 0).setValue(originalRect);

			if (bestDrawable.size() != 0) {
				for (iDrawable d : bestDrawable) {
					String tok = d.getToken();
					touch.add(tok);
					iDrawable old = drawQueue.get(tok);
					if (old == null) {
						drawQueue.put(tok, old = d);
					} else if (!old.isStopping()) {
						// do nothing

						// should merge
						drawQueue.put(tok, old = old
								.merge(d));

					} else
						old.restart();

					if (update) {
						Rect ore2 = new Rect(0, 0, 0, 0)
								.setValue(originalRect);
						old.update(ore, ore2);
						ore.setValue(ore2);
					} else {
						old.process(ore, currentRect);
						ore.setValue(currentRect);
					}

				}

				for (iDrawable d : bestDrawable) {
					String tok = d.getToken();
					iDrawable old = drawQueue.get(tok);

					if (update) {
						Rect ore2 = new Rect(0, 0, 0, 0)
								.setValue(originalRect);
						old.update(ore, ore2);
						ore.setValue(ore2);
					} else {
						old.process(ore, currentRect);
						ore.setValue(currentRect);
					}
				}

				if (createConstraint) {
					for (iOffering o : bestOffer) {
						iDrawable s = o
								.score(
										currentFramesMinusOngoing,
										element,
										or,
										originalRect,
										currentRect);
						if (s == null)
							continue;

						// disable constrat creation,
						// they don't work
						// o.createConstraint(root,
						// currentFramesMinusOngoing,
						// element, or, originalRect,
						// currentRect);

					}

				}

			}

		}

	}

	public boolean needsRepainting() {
		return drawQueue.size() > 0;
	}

	public void paint(RootComponent inside) {
		// if (leftArrows == null) {
		// thickLine = DynamicLine.coloredLine(null, 3);
		// thinLine = DynamicLine.coloredLine(null, 0.01f);
		// hairLine = DynamicLine.coloredLine(null, 0.01f);
		// thickPoint = DynamicPointlist.coloredPoints(null, 1);
		// (thickPoint.getUnderlyingGeometry()).setSize(3);

		// some useful arrows
		//
		// leftArrows = DynamicPointlist.unshadedPoints(null);
		// (leftArrows.getUnderlyingGeometry()).setSize(30);
		// leftArrows.getUnderlyingGeometry().addChild((new
		// PointSpriteTexture("content/icons/smallArrowLeft.tif").use_gl_texture_rectangle_ext(false).getMipMaps()));
		// upArrows = DynamicPointlist.unshadedPoints(null);
		// (upArrows.getUnderlyingGeometry()).setSize(30);
		// upArrows.getUnderlyingGeometry().addChild((new
		// PointSpriteTexture("content/icons/smallArrowUp.tif").use_gl_texture_rectangle_ext(false).getMipMaps()));
		// downArrows = DynamicPointlist.unshadedPoints(null);
		// (downArrows.getUnderlyingGeometry()).setSize(30);
		// downArrows.getUnderlyingGeometry().addChild((new
		// PointSpriteTexture("content/icons/smallArrowDown.tif").use_gl_texture_rectangle_ext(false).getMipMaps()));
		// rightArrows = DynamicPointlist.unshadedPoints(null);
		// (rightArrows.getUnderlyingGeometry()).setSize(30);
		// rightArrows.getUnderlyingGeometry().addChild((new
		// PointSpriteTexture("content/icons/smallArrowRight.tif").use_gl_texture_rectangle_ext(false).getMipMaps()));

		// }

		// thickLine.open();
		// thinLine.open();
		// hairLine.open();
		// leftArrows.open();
		// upArrows.open();
		// downArrows.open();
		// rightArrows.open();

		Iterator<iDrawable> i = drawQueue.values().iterator();
		while (i.hasNext()) {
			iDrawable n = i.next();
			n.draw(this);
		}

		i = drawQueue.values().iterator();
		while (i.hasNext()) {
			iDrawable n = i.next();
			if (n.isStopping() && n.hasStopped())
				i.remove();
		}
		if (drawQueue.size() > 0)
			inside.repaint();
		// leftArrows.close();
		// upArrows.close();
		// downArrows.close();
		// rightArrows.close();
		// hairLine.close();
		// thinLine.close();
		// thickLine.close();

		// leftArrows.getUnderlyingGeometry().performPass(null);
		// rightArrows.getUnderlyingGeometry().performPass(null);
		// upArrows.getUnderlyingGeometry().performPass(null);
		// downArrows.getUnderlyingGeometry().performPass(null);

		// hairLine.getUnderlyingGeometry().performPass(null);
		// thinLine.getUnderlyingGeometry().performPass(null);
		// thickLine.getUnderlyingGeometry().performPass(null);
	}

	protected void cullUntouched() {
		for (String d : drawQueue.keySet()) {
			if (!touch.contains(d)) {
				if (!drawQueue.get(d).isStopping())
					drawQueue.get(d).stop();
			}
		}
		Iterator<iDrawable> i = drawQueue.values().iterator();
		while (i.hasNext()) {
			iDrawable n = i.next();
			if (n.isStopping() && n.hasStopped())
				i.remove();
		}

		touch.clear();
	}

	protected void updateAll() {
		currentFrames.clear();
		currentFramesMinusOngoing.clear();

		TopologyVisitor_breadthFirst<iVisualElement> search = new TopologySearching.TopologyVisitor_breadthFirst<iVisualElement>(
				true) {
			@Override
			protected VisitCode visit(iVisualElement n) {
				Rect fr = n.getFrame(null);
				if (fr != null) {
					currentFrames.put(n, fr);
					if (!constraintsOngoing.containsKey(n))
						currentFramesMinusOngoing.put(
								n, fr);
				}
				return VisitCode.cont;
			}

		};

		search.apply(
				new TopologyViewOfGraphNodes<iVisualElement>(
						false).setEverything(true),
				root);
	}
}
