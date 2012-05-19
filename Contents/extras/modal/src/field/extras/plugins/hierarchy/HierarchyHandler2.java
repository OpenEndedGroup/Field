package field.extras.plugins.hierarchy;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.swt.widgets.Event;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.Constants;
import field.core.StandardFluidSheet;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.persistance.VisualElementReference;
import field.core.plugins.connection.Connections;
import field.core.plugins.connection.LineDrawingOverride;
import field.core.plugins.drawing.SimpleArrows;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.Cursor;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.pseudo.PseudoPropertiesPlugin;
import field.core.plugins.pseudo.PseudoPropertiesPlugin.Subelements;
import field.core.plugins.python.PythonPluginEditor;
import field.core.ui.PresentationMode;
import field.core.ui.PresentationParameters;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.SwingBridgeComponent;
import field.core.windowing.components.iComponent;
import field.launch.iUpdateable;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;
import field.namespace.generic.Bind;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.Generics.Pair;

public class HierarchyHandler2 extends HierarchyHandler {

	private Connections connections;

	public HierarchyHandler2(iVisualElement root) {
		super(root);
		connections = Connections.connections_plugin.get(root);

	}

	public void topologyChanged() {
		verify();
	}

	@Override
	public void finalizeConnection(iVisualElement origin, iVisualElement target, int buttons) {
		if (buttons == 1) {
			iVisualElement c = connections.connect(origin, target, DispatchConnective.class, false);
			PresentationParameters pp = new PresentationParameters();
			pp.hidden = true;
			PresentationMode.present.set(c, c, pp);

			Subelements s = (Subelements) PseudoPropertiesPlugin.subelements.get(origin);
			s.add(target);

			verify();
		}
	}

	@Override
	public iVisualElement over(Vector2 at) {
		iVisualElement o = super.over(at);

		if (o==null) return null;
		if (o.getProperty(iVisualElement.overrides) instanceof DispatchConnective) return null;
		
		return o;
	}
	
	public void verify() {
		
		List<iVisualElement> all = new ArrayList<iVisualElement>(StandardFluidSheet.allVisualElements(root));

		for (iVisualElement e : all) {
			if (iVisualElement.localView.get(e) == null)
				continue;
			List<iVisualElement> m = (List<iVisualElement>) e.getParents();
			for (iVisualElement p : m) {

				if (iVisualElement.localView.get(p) == null)
					continue;

				if (isConnected(e, p) == null) {
					if (p != root && e != root && !filter(p) && !filter(e)) {
						System.out.println(" connecting :" + e + " " + p);
						iVisualElement c = connections.connect(e, p, DispatchConnective.class, false);
						PresentationParameters pp = new PresentationParameters();
						pp.hidden = true;
						PresentationMode.present.set(c, c, pp);

					}
				}
			}
		}

	}

	@Override
	public String getMessage(iVisualElement origin, iVisualElement target, int buttons) {
		String no = iVisualElement.name.get(origin);
		String nt = iVisualElement.name.get(target);
		if (buttons == 1) {
			final iVisualElement c1 = isConnected(origin, target);
			if (c1 != null) {
				return "\u2014\u2014 do nothing \u2014\u2014";
			} else
				return " '" + no + "' will delegate to '" + nt + "'";
		}
		return "\u2014\u2014 do nothing \u2014\u2014";
	}

	public iVisualElement isConnected(iVisualElement from, iVisualElement to) {

		System.out.println(" connections <" + connections + "> <" + (connections != null ? connections.connections : null) + ">");

		System.out.println(" checking to see if <" + from + " and " + to + "> are connected");

		Set<Entry<String, iVisualElement>> cc = connections.connections.entrySet();
		for (Entry<String, iVisualElement> e : cc) {
			iVisualElement v = e.getValue();
			VisualElementReference f = LineDrawingOverride.lineDrawing_from.get(v);
			VisualElementReference t = LineDrawingOverride.lineDrawing_to.get(v);

			System.out.println("       :checked " + f.get(root) + " " + t.get(root));

			if (f.get(root) == from && t.get(root) == to) {
				return v;
			}
		}
		System.out.println("   not connected ");
		return null;
	}

	private boolean filter(iVisualElement to) {
		return (to.getProperty(iVisualElement.localView) instanceof SwingBridgeComponent);
	}

	@Woven
	static public class DispatchConnective extends DefaultOverride {
		static public final VisualElementProperty<VisualElementReference> lineDrawing_from = new VisualElementProperty<VisualElementReference>("lineDrawing_from");

		static public final VisualElementProperty<VisualElementReference> lineDrawing_to = new VisualElementProperty<VisualElementReference>("lineDrawing_to");

		@Override
		public DefaultOverride setVisualElement(iVisualElement ve) {
			iVisualElement.doNotSave.set(ve, ve, true);			
			return super.setVisualElement(ve);
		}
		
		public VisitCode deleted(iVisualElement source) {

			if (source == from() || source == to()) {
				new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).deleted(forElement);
				for (iVisualElement ve : new ArrayList<iVisualElement>((Collection<iVisualElement>) forElement.getParents())) {
					ve.removeChild(forElement);
				}
			} else if (source == forElement) {
				iVisualElement f = from();
				iVisualElement t = to();

				if (f != null && t != null) {
					Subelements s = (Subelements) PseudoPropertiesPlugin.subelements.get(f);
					s.remove(t);
				}
			}
			return super.deleted(source);
		}

		protected iVisualElement from() {
			VisualElementReference p = this.forElement.getProperty(lineDrawing_from);
			if (p == null) {
				return null;
			}
			iVisualElement r = p.get(forElement);
			return r;
		}

		protected iVisualElement to() {
			VisualElementReference p = this.forElement.getProperty(lineDrawing_to);
			if (p == null) {
				return null;
			}
			return p.get(forElement);
		}

		@Override
		public VisitCode isHit(iVisualElement source, Event event, Ref<Boolean> is) {
			if (source == forElement) {
				iVisualElement f = from();
				iVisualElement t = to();
				if (f == null || t == null) {
					needsDeletion();
					return VisitCode.cont;
				}

				Rect fr = f.getFrame(null);
				Rect tr = t.getFrame(null);

				if (fr == null || tr == null) {
					needsDeletion();
					return VisitCode.cont;
				}

				final Vector2 tm = tr.midpoint2();

				int o = 3;

				Vector2[] fPositions = new Vector2[] { new Vector2(fr.x - o, fr.y - o), /*
													 * new
													 * Vector2
													 * (
													 * fr
													 * .
													 * x
													 * +
													 * fr
													 * .
													 * w
													 * +
													 * o
													 * ,
													 * fr
													 * .
													 * y
													 * -
													 * o
													 * )
													 * ,
													 */new Vector2(fr.x + fr.w + o, fr.y + fr.h + o), /*
																			 * new
																			 * Vector2
																			 * (
																			 * fr
																			 * .
																			 * x
																			 * -
																			 * o
																			 * ,
																			 * fr
																			 * .
																			 * y
																			 * +
																			 * fr
																			 * .
																			 * h
																			 * +
																			 * o
																			 * )
																			 */};

				Vector2 fromPosition = Bind.argMin(Arrays.asList(fPositions), new iFunction<Double, Vector2>() {

					@Override
					public Double f(Vector2 in) {
						return (double) in.distanceFrom(tm);
					}
				});

				CachedLine c = new CachedLine();
				c.getInput().moveTo(fromPosition.x, fromPosition.y);

				SelectionGroup<iComponent> selected = iVisualElement.selectionGroup.get(source);
				boolean isSelected = selected.getSelection().contains(iVisualElement.localView.get(source)) || selected.getSelection().contains(iVisualElement.localView.get(t)) || selected.getSelection().contains(iVisualElement.localView.get(f));

				Vector2 tm2 = lineToRect(fromPosition, tm, tr);

				if (tm2 == null)
					tm2 = tm;
				Vector2 tm3 = new Vector2(tm2).sub(fromPosition);
				Vector2 left = new Vector2(-tm3.y, tm3.x).normalize().scale(8);
				c.getInput().cubicTo(fromPosition.x * 0.66f + tm2.x * 0.33f - left.x, fromPosition.y * 0.66f + tm2.y * 0.33f - left.y, fromPosition.x * 0.33f + tm2.x * 0.66f - left.x, fromPosition.y * 0.33f + tm2.y * 0.66f - left.y, tm2.x, tm2.y);

				int w = 4;
				if (new LineUtils().isIntersecting(c, new Rect(event.x - w, event.y - w, w * 2, w * 2))) {
					is.set(true);
					return VisitCode.stop;
				}

			}
			return super.isHit(source, event, is);
		}

		@Override
		public VisitCode menuItemsFor(iVisualElement source, Map<String, iUpdateable> items) {
			return super.menuItemsFor(source, items);
		}

		@Override
		public VisitCode paintNow(iVisualElement source, Rect bounds, boolean visible) {
			if (source == forElement) {

				PresentationParameters pp = new PresentationParameters();
				pp.hidden = true;
				PresentationMode.present.set(source, source, pp);

				iVisualElement f = from();
				iVisualElement t = to();
				if (f == null || t == null) {
					needsDeletion();
					return VisitCode.cont;
				}

				Rect fr = f.getFrame(null);
				Rect tr = t.getFrame(null);

				if (fr == null || tr == null) {
					needsDeletion();
					return VisitCode.cont;
				}

				final Vector2 tm = tr.midpoint2();

				int o = 3;

				Vector2[] fPositions = new Vector2[] { new Vector2(fr.x - o, fr.y - o), /*
													 * new
													 * Vector2
													 * (
													 * fr
													 * .
													 * x
													 * +
													 * fr
													 * .
													 * w
													 * +
													 * o
													 * ,
													 * fr
													 * .
													 * y
													 * -
													 * o
													 * )
													 * ,
													 */new Vector2(fr.x + fr.w + o, fr.y + fr.h + o), /*
																			 * new
																			 * Vector2
																			 * (
																			 * fr
																			 * .
																			 * x
																			 * -
																			 * o
																			 * ,
																			 * fr
																			 * .
																			 * y
																			 * +
																			 * fr
																			 * .
																			 * h
																			 * +
																			 * o
																			 * )
																			 */};

				Vector2 fromPosition = Bind.argMin(Arrays.asList(fPositions), new iFunction<Double, Vector2>() {

					@Override
					public Double f(Vector2 in) {
						return (double) in.distanceFrom(tm);
					}
				});

				CachedLine c = new CachedLine();
				c.getInput().moveTo(fromPosition.x, fromPosition.y);

				SelectionGroup<iComponent> selected = iVisualElement.selectionGroup.get(source);
				boolean isSelected = selected.getSelection().contains(iVisualElement.localView.get(source)) || selected.getSelection().contains(iVisualElement.localView.get(t)) || selected.getSelection().contains(iVisualElement.localView.get(f));

				Vector2 tm2 = lineToRect(fromPosition, tm, tr);

				if (tm2 == null)
					tm2 = tm;
				Vector2 tm3 = new Vector2(tm2).sub(fromPosition);
				Vector2 left = new Vector2(-tm3.y, tm3.x).normalize().scale(8);
				c.getInput().cubicTo(fromPosition.x * 0.66f + tm2.x * 0.33f - left.x, fromPosition.y * 0.66f + tm2.y * 0.33f - left.y, fromPosition.x * 0.33f + tm2.x * 0.66f - left.x, fromPosition.y * 0.33f + tm2.y * 0.66f - left.y, tm2.x, tm2.y);
				// c.getInput().lineTo(tm2.x, tm2.y);

				// GLComponentWindow.currentContext.submitLine(c,
				// c.getProperties());

				CachedLine a = new SimpleArrows().arrowForEnd(c, 3, 8, 0.75f);
				a.getProperties().put(iLinearGraphicsContext.thickness, isSelected ? 1.5f : 1.0f);
				a.getProperties().put(iLinearGraphicsContext.filled, true);
				a.getProperties().put(iLinearGraphicsContext.color, isSelected ? new Vector4(0, 0, 0.0, 0.5f) : new Vector4(0, 0, 0.0, 0.05f));
				GLComponentWindow.currentContext.submitLine(a, a.getProperties());

				CachedLine b = new CachedLine();
				b.getInput().moveTo(fromPosition.x, fromPosition.y);
				b.getProperties().put(iLinearGraphicsContext.pointed, true);
				b.getProperties().put(iLinearGraphicsContext.pointSize, 7f);
				b.getProperties().put(iLinearGraphicsContext.color, isSelected ? new Vector4(0, 0, 0.0, 0.5f) : new Vector4(0, 0, 0.0, 0.05f));
				GLComponentWindow.currentContext.submitLine(b, b.getProperties());

				CachedLine c2 = new CachedLine();
				c2.getInput().moveTo(fromPosition.x, fromPosition.y);

				tm3 = new Vector2(tm2).sub(fromPosition);
				left = new Vector2(-tm3.y, tm3.x).normalize().scale(10);

				// float m = tm3.mag();
				// tm3.normalize().scale(m-8).add(fromPosition);

				Pair<CachedLine, CachedLine> split = new Cursor(c).end().forwardD(-10).split();

				c2 = split.left;
				c2.getProperties().put(iLinearGraphicsContext.thickness, isSelected ? 1.5f : 1.0f);
				c2.getProperties().put(iLinearGraphicsContext.color, isSelected ? new Vector4(0, 0, 0.0, 0.5f) : new Vector4(0, 0, 0.0, 0.05f));
				GLComponentWindow.currentContext.submitLine(c2, c2.getProperties());

				if (selected.getSelection().contains(iVisualElement.localView.get(source))) {
					CachedLine r = new CachedLine();
					r.getInput().moveTo(fromPosition.x, fromPosition.y);
					r.getInput().lineTo(fromPosition.x, tm2.y);
					r.getInput().lineTo(tm2.x, tm2.y);
					r.getInput().lineTo(tm2.x, fromPosition.y);
					r.getInput().lineTo(fromPosition.x, fromPosition.y);
					r.getProperties().put(iLinearGraphicsContext.thickness, 1f);
					r.getProperties().put(iLinearGraphicsContext.color, new Vector4(0, 0, 0, 0.05f));

					GLComponentWindow.currentContext.submitLine(r, r.getProperties());
					CachedLine text = new CachedLine();
					text.getProperties().put(iLinearGraphicsContext.containsText, true);
					Vector2 m = new Cursor(c2).forwardT(0.5f).position();
					text.getInput().moveTo(m.x + 0, m.y + 5);
					text.getInput().setPointAttribute(iLinearGraphicsContext.text_v, forElement.getProperty(iVisualElement.name));
					text.getInput().setPointAttribute(iLinearGraphicsContext.alignment_v, 0f);
					text.getInput().setPointAttribute(iLinearGraphicsContext.font_v, new Font(Constants.defaultFont, 0, 11));
					text.getInput().setPointAttribute(iLinearGraphicsContext.fillColor_v, new Vector4(0.0, 0, 0, 1f));
					GLComponentWindow.currentContext.submitLine(text, text.getProperties());

				}

			}
			return super.paintNow(source, bounds, visible);
		}

		@NextUpdate
		protected void needsDeletion() {
			PythonPluginEditor.delete(forElement, null);
		}

	}

	public void install(HierarchyPlugin h) {
		h.addTool("icons/fork_16x16.png", new Hover(), new iUpdateable() {

			public void update() {
				all = StandardFluidSheet.allVisualElements(root);
			}
		}, "Modify dispatch hierarchy tool 2", "Modify dispatch 2", "Modify dispatch tool (D)\n" + 
		"========================\n" + 
		"\n" + 
		"This tool manipulates the dispatch graph of elements. Specifically you can use this tool to create new connections between elements in the *dispatch graph*. Drag between parent and child to create a new connection (you can delete these later by selecting them). After a connection has been made `_self.something` in the parent element will be visible in the child. To quickly select this tool without going to the trouble of mousing down to its toolbar, hold down the D key.\n" + 
		"");

		plugin = h;

	}

	static private boolean SAME_SIGNS(double a, double b) {
		return Math.signum(a) == Math.signum(b);
	}

	public static Vector2 lineToRect(final Vector2 a, Vector2 b, Rect r) {
		Vector2 a1 = lines_intersect(a.x, a.y, b.x, b.y, r.x, r.y, r.x + r.w, r.y);
		Vector2 a2 = lines_intersect(a.x, a.y, b.x, b.y, r.x, r.y, r.x, r.y + r.h);
		Vector2 a3 = lines_intersect(a.x, a.y, b.x, b.y, r.x + r.w, r.y, r.x + r.w, r.y + r.h);
		Vector2 a4 = lines_intersect(a.x, a.y, b.x, b.y, r.x, r.y + r.h, r.x + r.w, r.y + r.h);

		List<Vector2> aa = new ArrayList<Vector2>();
		if (a1 != null)
			aa.add(a1);
		if (a2 != null)
			aa.add(a2);
		if (a3 != null)
			aa.add(a3);
		if (a4 != null)
			aa.add(a4);

		if (aa.size() == 0)
			return null;

		Collections.sort(aa, new Comparator<Vector2>() {

			@Override
			public int compare(Vector2 o1, Vector2 o2) {
				return Double.compare(o1.distanceFrom(a), o2.distanceFrom(a));
			}
		});

		return aa.get(0);

	}

	static public Vector2 lines_intersect(double x1, double y1, /*
								 * First line
								 * segment
								 */
	double x2, double y2,

	double x3, double y3, /* Second line segment */
	double x4, double y4

	) {
		double a1, a2, b1, b2, c1, c2; /* Coefficients of line eqns. */
		double r1, r2, r3, r4; /* 'Sign' values */
		double denom, offset, num; /* doubleermediate values */

		/*
		 * Compute a1, b1, c1, where line joining podoubles 1 and 2 is
		 * "a1 x  +  b1 y  +  c1  =  0".
		 */

		a1 = y2 - y1;
		b1 = x1 - x2;
		c1 = x2 * y1 - x1 * y2;

		/*
		 * Compute r3 and r4.
		 */

		r3 = a1 * x3 + b1 * y3 + c1;
		r4 = a1 * x4 + b1 * y4 + c1;

		/*
		 * Check signs of r3 and r4. If both podouble 3 and podouble 4
		 * lie on same side of line 1, the line segments do not
		 * doubleersect.
		 */

		if (r3 != 0 && r4 != 0 && SAME_SIGNS(r3, r4))
			return null;

		/* Compute a2, b2, c2 */

		a2 = y4 - y3;
		b2 = x3 - x4;
		c2 = x4 * y3 - x3 * y4;

		/* Compute r1 and r2 */

		r1 = a2 * x1 + b2 * y1 + c2;
		r2 = a2 * x2 + b2 * y2 + c2;

		/*
		 * Check signs of r1 and r2. If both podouble 1 and podouble 2
		 * lie on same side of second line segment, the line segments do
		 * not doubleersect.
		 */

		if (r1 != 0 && r2 != 0 && SAME_SIGNS(r1, r2))
			return null;

		/*
		 * Line segments doubleersect: compute doubleersection podouble.
		 */

		denom = a1 * b2 - a2 * b1;
		if (denom == 0)
			return null;
		offset = denom < 0 ? -denom / 2 : denom / 2;

		/*
		 * The denom/2 is to get rounding instead of truncating. It is
		 * added or subtracted to the numerator, depending upon the sign
		 * of the numerator.
		 */

		num = b1 * c2 - b2 * c1;
		double xx = (num < 0 ? num - offset : num + offset) / denom;

		num = a2 * c1 - a1 * c2;
		double yy = (num < 0 ? num - offset : num + offset) / denom;

		return new Vector2(xx, yy);
	} /* lines_doubleersect */

}
