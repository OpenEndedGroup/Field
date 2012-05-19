package field.core.windowing.components;
	
import java.awt.BasicStroke;
import java.awt.Font;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.iLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.windowing.GLComponentWindow;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;
import field.namespace.generic.Bind.iFunction;

/**
 * better than the "option-click dance"
 * 
 * this is the UI / state machine for executing components with the mouse
 * 
 */
public abstract class ExecutionDecoration {

	private final iComponent component;

	public enum Direction {
		north(new Vector2(0, 1)), south(new Vector2(0, -1)), east(new Vector2(1, 0)), west(new Vector2(-1, 0));

		Vector2 d;

		Direction(Vector2 d) {
			this.d = d;
		}
	}

	public enum Decoration {
		nothing(null), start(Direction.east), stop(Direction.west);

		private Direction d;

		Decoration(Direction d) {
			this.d = d;
		}

		public Direction getDirection() {
			return d;
		}
	}

	Decoration decoration = Decoration.nothing;
	boolean armed = false;

	public ExecutionDecoration(iComponent component) {
		this.component = component;
	}

	Vector2 downAt = null;

	public iFunction<Boolean, iComponent> down(Event event) {
		if ((event.stateMask & SWT.ALL)!=0) {
			downAt = new Vector2(event.x, event.y);
			if (isExecuting()) {
				decoration = Decoration.stop;
				armed = false;
				return null;
			} else {
				// need to defer this a frame to see if we end
				// up "isExecuting"
				return new iFunction<Boolean, iComponent>(){
					public Boolean f(iComponent d) {
						if (isExecuting())
						{
							decoration = Decoration.start;
							armed = false;
							iVisualElement.dirty.set(d.getVisualElement(), d.getVisualElement(), true);
						}
						else
						{
						}
						return false;
					}
				};
			}
		}
		return null;
	}

	abstract protected boolean isExecuting();

	public boolean up(Event event) {
		if (decoration == Decoration.nothing)
			return false;
		if (intersectsWithDecoration(event)) {
			if (decoration == Decoration.start) {
				continueToBeActiveAfterUp();
				decoration = Decoration.nothing;
				return true;
			} else if (decoration == Decoration.stop) {
				stopBeingActiveNow();
				decoration = Decoration.nothing;
				return true;
			}
		}

		decoration = Decoration.nothing;
		return true;
	}

	public void paintNow() {
		if (decoration == Decoration.nothing)
			return;

		
		Rect bounds = component.getBounds();

		bounds = new Rect(0, 0, 0, 0).setValue(bounds);
		bounds.x = downAt.x;
		bounds.w = 20 * decoration.d.d.x;
		bounds.h = 50;
		bounds.y = downAt.y - bounds.h / 2;

		{
			CachedLine frame = new CachedLine();
			iLine in = frame.getInput();

			in.moveTo((float) (bounds.x + bounds.w + 1), (float) bounds.y);
			in.setPointAttribute(iLinearGraphicsContext.fillColor_v, armed ? new Vector4(0.5, 0, 0, 0.25) : new Vector4(1, 1, 1, 0.15));
			in.lineTo((float) (bounds.x + bounds.w + 1), (float) (bounds.y + bounds.h));
			in.setPointAttribute(iLinearGraphicsContext.fillColor_v, armed ? new Vector4(0.5, 0, 0, 0.25) : new Vector4(1, 1, 1, 0.15));
			in.lineTo((float) (bounds.x + bounds.w + 100 * decoration.d.d.x), (float) (bounds.y + bounds.h + 50));
			in.setPointAttribute(iLinearGraphicsContext.fillColor_v, new Vector4(0, 0, 0, 0));
			in.lineTo((float) (bounds.x + bounds.w + 100 * decoration.d.d.x), (float) (bounds.y - 50));
			in.setPointAttribute(iLinearGraphicsContext.fillColor_v, new Vector4(0, 0, 0, 0));
			in.lineTo((float) (bounds.x + bounds.w + 1), (float) (bounds.y));
			in.setPointAttribute(iLinearGraphicsContext.fillColor_v, new Vector4(0, 0, 0, 0.25));

			frame.getProperties().put(iLinearGraphicsContext.filled, true);
			frame.getProperties().put(iLinearGraphicsContext.stroked, false);

			GLComponentWindow.currentContext.submitLine(frame, frame.getProperties());
		}
		{
			CachedLine frame = new CachedLine();
			iLine in = frame.getInput();
			in.moveTo((float) (bounds.x + bounds.w + 100 * decoration.d.d.x), (float) (bounds.y - 50));
			in.setPointAttribute(iLinearGraphicsContext.strokeColor_v, armed ? new Vector4(0, 0, 0, 0) : new Vector4(0, 0, 0, 0.0));
			in.setPointAttribute(iLinearGraphicsContext.fillColor_v, armed ? new Vector4(0, 0, 0, 0.0) : new Vector4(0, 0, 0, 0.0));
			in.lineTo((float) (bounds.x + bounds.w + 1), (float) bounds.y);
			in.setPointAttribute(iLinearGraphicsContext.strokeColor_v, armed ? new Vector4(0, 0, 0, 0.5f) : new Vector4(0, 0, 0, 0.3));
			in.setPointAttribute(iLinearGraphicsContext.fillColor_v, armed ? new Vector4(0, 0, 0, 0.5f) : new Vector4(0, 0, 0, 0.3));
			in.lineTo((float) (bounds.x + bounds.w + 1), (float) (bounds.y + bounds.h));
			in.setPointAttribute(iLinearGraphicsContext.strokeColor_v, armed ? new Vector4(0, 0, 0, 0.5f) : new Vector4(0, 0, 0, 0.3));
			in.setPointAttribute(iLinearGraphicsContext.fillColor_v, armed ? new Vector4(0, 0, 0, 0.5f) : new Vector4(0, 0, 0, 0.3));
			in.lineTo((float) (bounds.x + bounds.w + 100 * decoration.d.d.x), (float) (bounds.y + bounds.h + 50));
			in.setPointAttribute(iLinearGraphicsContext.strokeColor_v, armed ? new Vector4(0, 0, 0, 0) : new Vector4(0, 0, 0, 0.0));
			in.setPointAttribute(iLinearGraphicsContext.fillColor_v, armed ? new Vector4(0, 0, 0, 0) : new Vector4(0, 0, 0, 0.0));

			frame = new LineUtils().lineAsStroked(frame, new BasicStroke(3), true);

			frame.getProperties().put(iLinearGraphicsContext.stroked, true);
			frame.getProperties().put(iLinearGraphicsContext.filled, true);
			frame.getProperties().put(iLinearGraphicsContext.thickness, 1f);
			GLComponentWindow.currentContext.submitLine(frame, frame.getProperties());
		}
		{
			CachedLine frame = new CachedLine();
			iLine in = frame.getInput();

			in.moveTo((float) (bounds.x + bounds.w + 10 * decoration.d.d.x), (float) (bounds.y + bounds.h / 2 - 3));
			frame.getProperties().put(iLinearGraphicsContext.containsText, true);
			if (decoration == Decoration.start)
				in.setPointAttribute(iLinearGraphicsContext.text_v, "\u25b6 Continue running");
			else {
				in.setPointAttribute(iLinearGraphicsContext.text_v, "\u00d7 Stop");
				in.setPointAttribute(iLinearGraphicsContext.alignment_v, -1f);
			}
			//in.setPointAttribute(iLinearGraphicsContext.font_v, new Font("Gill Sans", Font.ITALIC, 10));
			GLComponentWindow.currentContext.submitLine(frame, frame.getProperties());
		}
	}

	abstract protected void stopBeingActiveNow();

	abstract protected void continueToBeActiveAfterUp();

	public boolean drag(Event event) {
		if (intersectsWithDecoration(event)) {
			if (!armed) {
				armed = true;
			}
			return true;
		}
		if (armed) {
			armed = false;
			return true;
		}
		return false;
	}

	public boolean intersectsWithDecoration(Event event) {
		try {
			Rect b = component.getBounds();

			return event.x * decoration.d.d.x >= (downAt.x + 20f * decoration.d.d.x) * decoration.d.d.x && event.y * decoration.d.d.y >= (downAt.y + 20f * decoration.d.d.y) * decoration.d.d.y;

			// return event.getX() > downAt.x + 20f && event.getY()
			// >= b.y && event.getY() < b.y + b.h;
		} catch (Exception e) {

			return false;
		}

	}

}
