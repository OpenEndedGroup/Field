package field.core.execution;

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.swt.widgets.Event;
import org.python.constantine.Platform;

import field.core.Constants;
import field.core.Platform.OS;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.windowing.GLComponentWindow;
import field.graphics.core.BasicContextManager;
import field.graphics.core.BasicGeometry;
import field.graphics.core.TextSystem;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.graphics.core.TextSystem.RectangularLabel;
import field.graphics.dynamic.DynamicMesh;
import field.graphics.dynamic.iDynamicMesh;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;
import field.math.linalg.iCoordinateFrame.iMutable;


import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL41.*;
import static org.lwjgl.opengl.ARBShaderObjects.*;


/**
 * @author marc
 * 
 */
public class TimeMarker extends iVisualElementOverrides.DefaultOverride {

	static public final VisualElementProperty<String> keyboardShortcut = new VisualElementProperty<String>("keyboardShortcut_i");

	static public final VisualElementProperty<Object> transitionDuration = new VisualElementProperty<Object>("transitionDuration_i");

	static public final VisualElementProperty<Integer> isRealtime = new VisualElementProperty<Integer>("isRealtime_i");

	static public HashMap<iVisualElement, iUpdateable> ongoing = new HashMap<iVisualElement, iUpdateable>();

	private RectangularLabel label;

	private String lastPrintName = "";

	private TriangleMesh labelTriangles;

	private iDynamicMesh labelTriangle;

	transient List<CachedLine> cl;

	Vector4 previousViewParameters = new Vector4();

	@Override
	public VisitCode added(iVisualElement newSource) {

		String kb = newSource.getProperty(keyboardShortcut);
		if (kb == null) {
			newSource.setProperty(keyboardShortcut, "no shortcut");
		}
		Number in = (Number) newSource.getProperty(transitionDuration);
		if (in == null) {
			newSource.setProperty(transitionDuration, 100);
		}

		Object re = newSource.getProperty(isRealtime);
		if (re == null) {
			newSource.setProperty(isRealtime, 0);
		}

		return super.added(newSource);
	}

	@Override
	public VisitCode handleKeyboardEvent(iVisualElement newSource, Event event) {

		;//System.out.println(" handle keyboad event called <" + event + ">");
		if (event == null)
			return VisitCode.cont;

		if (!event.doit)
			return VisitCode.cont;
		
		if (newSource == forElement) {
			char c = event.character;
			

			String kb = newSource.getProperty(keyboardShortcut);
			;//System.out.println(" key <" + kb.charAt(0) + "> <" + c + ">");

			if (kb.charAt(0) == c) {
				executeWithoutModifiers(event, c);
				event.doit=false;
			} else if (Character.toLowerCase(c) == kb.charAt(0)) {
				executeWithShift(event, c);
				event.doit=false;
			}
		}
		return VisitCode.cont;
	}

	@Override
	public VisitCode isHit(iVisualElement source, Event event, Ref<Boolean> is) {
		if (source == forElement) {
			Rect frame = forElement.getFrame(null);
			if (frame.x - 5 <= event.x && frame.x + frame.w + 5 >= event.x)
				is.set(true);
		}
		return VisitCode.cont;
	}

	@Override
	public VisitCode paintNow(iVisualElement source, Rect bounds, boolean visible) {

		if (source == forElement) {

			

			GLComponentWindow window = GLComponentWindow.getCurrentWindow(null);
			Vector2 lower0 = window.transformWindowToCanvas(new Vector2(0, 0));
			Vector2 upper0 = window.transformWindowToCanvas(new Vector2(0, 1));

			cl = makeCachedLine(lower0, upper0);

			Rect newFrame = new Rect(bounds.x, upper0.y, 25f, -upper0.y + lower0.y);

			

			if (!newFrame.equals(bounds)) {
				source.setFrame(newFrame);
			}

			iLinearGraphicsContext cc = GLComponentWindow.currentContext;

			for (CachedLine c : cl)
				cc.submitLine(c, c.getProperties());

			String kb = source.getProperty(keyboardShortcut);

			assert kb != null : " huh?";
			String printName = source.getProperty(iVisualElement.name) + " : " + kb;

			printName = "<html><i><font size=+0 face='" + Constants.defaultFont + "'>" + source.getProperty(iVisualElement.name) + "</font></i> <font face='" + Constants.defaultFont + "' size=" + (kb.length() == 1 ? "+1" : "-2") + ">(" + kb + ")</font>";

			// printName = "peach";

			if (label == null || scaleHasChanged()) {
				if (label != null)
					label.dispose();
				label = TextSystem.textSystem.new RectangularLabel(printName, new Font(Constants.defaultFont, Font.PLAIN, 1).deriveFont(15f), (float) (Math.PI / 2));
				label.getTexture().use_gl_texture_rectangle_ext(false);
				label.setFont(new Font(Constants.defaultFont, Font.PLAIN, 1).deriveFont(15f));
				label.resetTextAsLabel(printName, 1, 1, 1, 1f, 0, 0, 0.0f, 1);
				labelTriangles = new BasicGeometry.TriangleMesh(new field.math.abstraction.iInplaceProvider<iMutable>() {
					public iMutable get(iMutable o) {
						return new CoordinateFrame();
					}
				});
				labelTriangles.rebuildVertex(0);
				labelTriangles.rebuildTriangle(0);
				labelTriangle = new DynamicMesh(labelTriangles);
			}

			if (!lastPrintName.equals(printName)) {
				label.setFont(new Font(Constants.defaultFont, Font.PLAIN, 1).deriveFont(15f));
				label.resetTextAsLabel(printName, 1, 1, 1f, 1, 0, 0, 0.0f, 1);
				lastPrintName = printName;
			}
			labelTriangle.open();
			
			(label).drawIntoMeshRotated(labelTriangle, 1, 1, 1, 1, (float) bounds.x + (float) bounds.w / 2, (float) (bounds.y + 40), previousViewParameters.z);
			labelTriangle.close();

			// ((RectangularLabel)
			// label).drawIntoMesh(labelTriangle, 1, 1, 1, 1, 50,
			// 50);

			// show it
			glActiveTexture(GL_TEXTURE1);
			label.on();
			glBlendFunc(GL_ZERO, GL_SRC_COLOR);
			labelTriangles.performPass(null);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			label.off();
			glActiveTexture(GL_TEXTURE0);

		}
		return VisitCode.cont;
	}

	@Override
	public VisitCode shouldChangeFrame(iVisualElement source, Rect newFrame, Rect oldFrame, boolean now) {
		if (source == forElement) {
			newFrame.w = 25;
		}
		return super.shouldChangeFrame(source, newFrame, oldFrame, now);
	}

	private List<CachedLine> makeCachedLine(Vector2 lower0, Vector2 upper0) {
		List<CachedLine> r = new ArrayList<CachedLine>();

		CachedLine box = new CachedLine();
		Rect bounds = forElement.getFrame(null);
		box.getInput().moveTo((float) (bounds.x), (lower0.y));
		box.getInput().lineTo((float) (bounds.x + bounds.w), (lower0.y));
		box.getInput().lineTo((float) (bounds.x + bounds.w), (upper0.y));
		box.getInput().lineTo((float) (bounds.x), (upper0.y));
		box.getInput().lineTo((float) (bounds.x), (lower0.y));

		box.getProperties().put(iLinearGraphicsContext.color, new Vector4(0, 0, 0, 0.1f));
		box.getProperties().put(iLinearGraphicsContext.filled, true);

		if (iVisualElement.selectionGroup.get(this.forElement).getSelection().contains(iVisualElement.localView.get(this.forElement)))
		{
			box.getProperties().put(iLinearGraphicsContext.strokeColor, new Vector4(0,0,0,0.85f));
			box.getProperties().put(iLinearGraphicsContext.thickness, 2f);
			
		}
		
		r.add(box);
		return r;
	}

	private boolean scaleHasChanged() {
		Vector4 nextViewParameters = new Vector4(0 * GLComponentWindow.getCurrentWindow(null).getXTranslation(), 0 * GLComponentWindow.getCurrentWindow(null).getYTranslation(), 1 / GLComponentWindow.getCurrentWindow(null).getXScale(), 1 / GLComponentWindow.getCurrentWindow(
			null).getYScale());

		float d = nextViewParameters.distanceFrom(previousViewParameters);
		previousViewParameters.set(nextViewParameters);
		return d > 0;
	}

	protected void executeWithoutModifiers(Event event, char c) {
		final Ref<iVisualElement> r = new Ref<iVisualElement>(null);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).getProperty(forElement, iVisualElement.timeSlider, r);
		if (r.get() != null) {
			iUpdateable u = ongoing.get(r.get());
			Launcher.getLauncher().deregisterUpdateable(u);

			int over = 100;
			Ref<Object> r2 = new Ref<Object>(null);
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).getProperty(forElement, transitionDuration, r2);
			if (r2.get() != null)
				over = ((Number)r2.get()).intValue();

			boolean rt = isRealtime.getBoolean(forElement, false);

			if (rt) {
				final int fover = over;
				final long timeNow = System.currentTimeMillis();

				u = new iUpdateable() {

					long lastTime = timeNow;

					float t = 0;

					float startAt = (float) r.get().getFrame(null).x;

					public void update() {

						long c = System.currentTimeMillis();

						long inc = c - lastTime;

						lastTime = c;

						t += getOngoingSpeedNow() * (inc) / 1000f;

						float alpha = t / fover;

						float to = (float) (startAt * (1 - alpha) + alpha * forElement.getFrame(null).x);

						Rect localRect = new Rect(0, 0, 0, 0);

						r.get().getFrame(localRect);
						localRect.x = to;

						r.get().getProperty(iVisualElement.overrides).shouldChangeFrame(r.get(), localRect, r.get().getFrame(null), true);
						
						
						if (t >= fover) {
							Launcher.getLauncher().deregisterUpdateable(this);
							ongoing.remove(r.get());
						}
					}
				};
			} else {

				final int fover = over;

				u = new iUpdateable() {
					float t = 0;

					float startAt = (float) r.get().getFrame(null).x;

					public void update() {

						t += getOngoingSpeedNow();

						float alpha = t / fover;

						float to = (float) (startAt * (1 - alpha) + alpha * forElement.getFrame(null).x);

						Rect localRect = new Rect(0, 0, 0, 0);

						r.get().getFrame(localRect);
						localRect.x = to;

						r.get().getProperty(iVisualElement.overrides).shouldChangeFrame(r.get(), localRect, r.get().getFrame(null), true);

						if (t >= fover) {
							Launcher.getLauncher().deregisterUpdateable(this);
							ongoing.remove(r.get());
						}
					}
				};

			}
			Launcher.getLauncher().registerUpdateable(u);
			ongoing.put(r.get(), u);

		}
	}

	protected void executeWithShift(Event event, char c) {

		Ref<iVisualElement> r = new Ref<iVisualElement>(null);
		new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(forElement).getProperty(forElement, iVisualElement.timeSlider, r);

		;//System.out.println(" execute with mask ");

		if (r.get() != null) {
			iUpdateable u = ongoing.remove(r.get());
			if (u != null)
				Launcher.getLauncher().deregisterUpdateable(u);

//			r.get().setFrame(forElement.getFrame(null));

			r.get().getProperty(iVisualElement.overrides).shouldChangeFrame(r.get(), forElement.getFrame(null), r.get().getFrame(null), true);

		}
	}

	protected float getOngoingSpeedNow() {
		try {
			boolean on = field.core.Platform.getOS()==OS.mac && Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
			return on ? 0.3333f : 1;
		} catch (UnsupportedOperationException e) {
			// this happens on 10.4
			return 1;
		}
	}

}
