package field.extras.scrubber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.LightweightGroup;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.util.PythonCallableMap;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.DraggableComponent;
import field.launch.iUpdateable;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Triple;

public class TimeWarpGroup extends LightweightGroup implements iUpdateable {

	public interface iRemapTime {
		public float map(iVisualElement in, float _t);
	}

	static public VisualElementProperty<Warp> warp = new VisualElementProperty<Warp>("warp");

	static public VisualElementProperty<PythonCallableMap> computesWarp = new VisualElementProperty<PythonCallableMap>("computesWarp_");
	static public VisualElementProperty<Boolean> isWarper = new VisualElementProperty<Boolean>("isWarper");

	static public VisualElementProperty<Vector2> originalExtension = new VisualElementProperty<Vector2>("originalExtension");

	static public class Warp implements iRemapTime {
		int start;
		int end;

		float[] forward;

		boolean dirty = true;

		public Warp(int start, int end) {
			this.start = start;
			this.end = end;

			forward = new float[end - start];

			for (int x = start; x < end; x++) {
				forward[x - start] = x;
			}
		}

		public float map(float in) {
			if (in < start)
				return in;

			float index = in - start;

			int indexI = (int) index;
			if (indexI > end - 2 - start)
				return in;

			float indexA = index - indexI;

			return forward[indexI] * (1 - indexA) + forward[indexI + 1] * indexA;

		}

		public float mapBackwards(float in) {
			if (in <= start)
				return in;
			if (in >= end)
				return in;

			int x = Arrays.binarySearch(forward, in);
			if (x >= 0)
				return forward[x];

			x = -x - 1;
			float left = forward[x];
			float right = forward[x + 1];

			float a = (in - left) / (right - left);

			return start + x * (1 - a) + a * (x + 1);
		}

		public void clear() {
			for (int x = start; x < end; x++) {
				forward[x - start] = x;
			}
		}

		public float map(iVisualElement in, float t) {
			Vector2 ex = in.getProperty(originalExtension);
			if (ex == null)
				return t;

			Rect m = in.getFrame(null);
			float a = ex.x + t * (ex.y - ex.x);
			float a2 = map(a);
			float t2 = (float) ((a2 - m.x) / (m.w));
			return t2;
		}
	}

	public void update() {

		Rect f = forElement.getFrame(null);
		;//;//System.out.println(" checking group <" + f + "> <" + warp + ">");
		Warp warp = forElement.getProperty(TimeWarpGroup.warp);
		boolean dirty = false;
		if (warp == null) {
			forElement.setProperty(TimeWarpGroup.warp, warp = new Warp((int) f.x, (int) (f.x + f.w)));
		} else {
			if (warp.start != (int) f.x || warp.end != (int) (f.x + f.w)) {
				forElement.setProperty(TimeWarpGroup.warp, warp = new Warp((int) f.x, (int) (f.x + f.w)));
			}
		}

		if (dirty || warp.dirty) {

			;//;//System.out.println(" recomputing warp <" + warp + ">");
			recomputeWarp(warp);
		}

		Ref<List<CachedLine>> ref = new Ref<List<CachedLine>>(null);
		getLinesToDraw(ref);
		List<CachedLine> lines = ref.get();

		lines.clear();

		forElement.setProperty(noFrame, true);

		CachedLine line = drawWarp(warp, f);

		lines.add(line);
		Rect frame = forElement.getFrame(new Rect());

		CachedLine cl = new CachedLine();
		iLine in = cl.getInput();
		in.moveTo((float) (frame.x), (float) (frame.y));
		in.lineTo((float) (frame.x + frame.w), (float) (frame.y));
		in.lineTo((float) (frame.x + frame.w), (float) (frame.y + frame.h));
		in.lineTo((float) (frame.x), (float) (frame.y + frame.h));
		in.lineTo((float) (frame.x), (float) (frame.y));

		cl.getProperties().put(iLinearGraphicsContext.derived, 1f);
		cl.getProperties().put(iLinearGraphicsContext.color, new Vector4(0, 0, 0, 0.1));
		cl.getProperties().put(iLinearGraphicsContext.filled, false);
		cl.getProperties().put(iLinearGraphicsContext.stroked, false);

		lines.add(cl);

	}

	private CachedLine drawWarp(Warp w, Rect f) {

		CachedLine cl = new CachedLine();
		iLine in = cl.getInput();

		for (int i = (int) f.x; i < f.x + f.w; i += 5) {
			in.moveTo(i, (float) f.y - 20);
			float x = w.map(i);
			in.lineTo(x, (float) f.y);
			in.lineTo(x, (float) (f.y + f.h));
			in.lineTo(i, (float) (f.y + f.h + 20));
		}

		cl.getProperties().put(iLinearGraphicsContext.derived, 1f);
		cl.getProperties().put(iLinearGraphicsContext.color, new Vector4(0, 0, 0, 0.1));
		cl.getProperties().put(iLinearGraphicsContext.pointed, true);

		return cl;
	}

	private void recomputeWarp(Warp warp) {

		List<iVisualElement> contents = new ArrayList<iVisualElement>((List<iVisualElement>) forElement.getParents());
		
		Collections.sort(contents, new Comparator<iVisualElement>() {

			public int compare(iVisualElement o1, iVisualElement o2) {
				Rect r1 = o1.getFrame(new Rect());
				Rect r2 = o2.getFrame(new Rect());
				int c = Double.compare(r1.y, r2.y);
				return c==0 ? Double.compare(System.identityHashCode(o1), System.identityHashCode(o2)) : c;
			}
		});

		warp.clear();
		boolean dirty = false;
		for (iVisualElement v : contents) {
			if (shouldBound(v))
				continue;
			
			PythonCallableMap computer = v.getProperty(computesWarp);
			if (computer!=null)
				computer.invoke(warp);
			
		}
		
		postCheck(warp);
		
		recomputeContents(warp);
	}

	private void postCheck(Warp w) {
		
		int head = 10;
		for(int i=0;i<Math.min(w.forward.length, head);i++)
		{
			float a = (float) (1-head/10.0);
			w.forward[i] = w.forward[i]*(1-a)+a*(w.start+i); 
		}

		for(int i=0;i<Math.min(w.forward.length, head);i++)
		{
			float a = (float) (1-head/10.0);
			w.forward[w.forward.length-1-i] = w.forward[w.forward.length-1-i]*(1-a)+a*(w.forward.length-1-i+w.start); 
		}

	}

	private void recomputeContents(Warp warp) {

		List<iVisualElement> contents = (List<iVisualElement>) forElement.getParents();

		boolean dirty = false;
		for (iVisualElement v : contents) {
			if (!shouldBound(v))
				continue;

			;//;//System.out.println(" checking child <" + v + "> <" + warp + ">");
			Vector2 ex = originalExtension.get(v);
			Rect r = v.getFrame(null);
			if (ex == null) {
				float a1 = warp.mapBackwards((float) r.x);
				float a2 = warp.mapBackwards((float) (r.x + r.w));
				originalExtension.set(v, v, ex = new Vector2(a1, a2));
			}

			;//;//System.out.println(" <" + ex + ">");

			float a = warp.map(ex.x);
			float b = warp.map(ex.y);

			if (Math.abs(r.x - a) > 5e-1 || Math.abs((r.x + r.w) - b) > 5e-1) {
				;//;//System.out.println(" warp updated contents to be <" + originalExtension + " -> " + a + " " + b);

				v.setFrame(new Rect(a, r.y, b - a, r.h));
				GLComponentWindow.getCurrentWindow(null).requestRepaint();
				dirty = true;
			}
		}
		if (dirty) {
			forElement.setFrame(computeNewBoundingFrame(contents, forElement.getFrame(null), getOutset()));
			GLComponentWindow.getCurrentWindow(null).requestRepaint();
		}
	}

	@Override
	public VisitCode menuItemsFor(iVisualElement source, Map<String, iUpdateable> items) {
		if (source == forElement) {
			items.put(" Warp ", null);
			items.put(" New computing warp element ", new iUpdateable() {
				public void update() {

					float min = forElement.getFrame(null).bottomRight().y + 10;
					for (iVisualElement v : (List<iVisualElement>) forElement.getParents()) {
						if (!shouldBound(v)) {
							min = Math.max(min, v.getFrame(null).bottomRight().y + 10);
						}
					}

					GLComponentWindow frame = iVisualElement.enclosingFrame.get(forElement);

					Rect bounds = new Rect(30, min, 100, 15);
					if (frame != null) {
						bounds.x = frame.getCurrentMousePosition().x;
					}

					Triple<VisualElement, DraggableComponent, DefaultOverride> created = VisualElement.createWithName(bounds, forElement, VisualElement.class, DraggableComponent.class, DefaultOverride.class, "computed warp");

					created.left.setProperty(isWarper, true);
				}
			});
		}
		return super.menuItemsFor(source, items);
	}

	@Override
	public <T> VisitCode getProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> ref) {
		if (prop.equals(computesWarp) && forElement.getParents().contains(source)) {
			if (ref.get() == null) {
				PythonCallableMap pythonCallableMap = new PythonCallableMap();
				ref.set((T) pythonCallableMap, forElement);
				source.setProperty(computesWarp, pythonCallableMap);
			}
		}
		return super.getProperty(source, prop, ref);
	}

	@Override
	protected boolean subElementHasChangedWillChangeBounds() {
		return true;
	}
	
	@Override
	protected boolean shouldBound(iVisualElement ve) {
		Boolean m = ve.getProperty(isWarper);
		return !(m != null && m);
	}

}
