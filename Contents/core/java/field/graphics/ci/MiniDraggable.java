package field.graphics.ci;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.util.ArrayList;
import java.util.List;

import field.core.dispatch.iVisualElement.Rect;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;

public class MiniDraggable {

	public class SubControl {
		protected Rect hitBox = new Rect(0,0,7,7);
		protected Vector4 color = new Vector4(1, 1, 1, 0.3f);
		boolean hover = false;
		boolean armed = false;

		public SubControl() {
			controls.add(this);
		}

		public void drawNow(Graphics2D g) {
			g.setColor(new Color(color.x, color.y, color.z, armed ? 0.8f : color.w));

			Rect hitBox = derelativize(this.hitBox);

//			Double p = new Rectangle2D.Double(hitBox.x, hitBox.y, hitBox.w, hitBox.h);
			Ellipse2D.Double p = new Ellipse2D.Double(hitBox.x, hitBox.y, hitBox.w, hitBox.h);
			g.fill(p);
			g.draw(p);

			if (hover) {
				Double p2 = new Rectangle2D.Double(hitBox.x - 5, hitBox.y - 5, hitBox.w + 10, hitBox.h + 10);
				g.draw(p2);
			}
		}

		public void drag(float dx, float dy) {
			Rect hitBox = derelativize(this.hitBox);
			hitBox.x += dx;
			hitBox.y += dy;
			this.hitBox = relativize(hitBox);

			constrainBox();
		}

		protected void constrainBox() {

		}
	}

	public void draw(Graphics2D g) {
		for (SubControl c : controls) {
			c.drawNow(g);
		}
	}

	List<SubControl> controls = new ArrayList<SubControl>();

	SubControl on = null;

	Vector2 downAt = new Vector2();

	public boolean mouseDown(MouseEvent e) {

		on = hit(e);
		if (on != null) {
			on.armed = true;
			on.hover = false;
			downAt.x = e.getX();
			downAt.y = e.getY();
		}

		return on != null;
	}

	public boolean mouseDragged(MouseEvent e) {
		if (on != null) {
			float dx = e.getX() - downAt.x;
			float dy = e.getY() - downAt.y;
			on.drag(dx, dy);
			downAt.x = e.getX();
			downAt.y = e.getY();
			return true;
		}
		return false;
	}

	public boolean mouseUp(MouseEvent e) {
		if (on != null) {
			on.armed = false;
			on = null;
			mouseMoved(e);
			return true;
		}
		return false;
	}

	public boolean mouseMoved(MouseEvent e) {
		boolean r = false;
		SubControl c = hit(e);
		for (SubControl ss : controls) {
			if (ss == c) {
				if (!ss.hover) {
					ss.hover = true;
					r = true;
				}
			} else if (ss.hover) {
				ss.hover = false;
				r = true;
			}
		}
		return r;
	}

	private SubControl hit(MouseEvent e) {
		for (SubControl c : controls) {
			if (derelativize(c.hitBox).insetAbsolute(-4).isInside(new Vector2(e.getX(), e.getY())))
				return c;
		}
		return null;
	}

	protected Vector2 relativize(Vector2 v) {
		return v;
	}

	protected Vector2 derelativize(Vector2 v) {
		return v;
	}

	protected Rect derelativize(Rect v) {
		Rect r = new Rect(0, 0, 0, 0);
		Vector2 tl = derelativize(v.topLeft().toVector2());
		Vector2 br = derelativize(v.bottomRight().toVector2());

		r.x = Math.min(tl.x, br.x);
		r.y = Math.min(tl.y, br.y);
		//r.w = Math.abs(br.x - tl.x);
		//r.h = Math.abs(br.y - tl.y);
		r.w = v.w;
		r.h = v.h;
		
		return r;
	}

	protected Rect relativize(Rect v) {
		Rect r = new Rect(0, 0, 0, 0);
		Vector2 tl = relativize(v.topLeft().toVector2());
		Vector2 br = relativize(v.bottomRight().toVector2());

		r.x = Math.min(tl.x, br.x);
		r.y = Math.min(tl.y, br.y);
		//r.w = Math.abs(br.x - tl.x);
		//r.h = Math.abs(br.y - tl.y);
		r.w = v.w;
		r.h = v.h;
		
		return r;
	}

	public void reconstrainAll() {
		for(SubControl c: controls)
		{
			c.constrainBox();
		}
	}

}
