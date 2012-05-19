package field.core.plugins.drawing.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.sampled.LineUnavailableException;

import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.text.TextBrackets.BracketElement;
import field.core.plugins.drawing.text.TextBrackets.BracketElementType;
import field.core.plugins.drawing.text.TextBrackets.iBracketFactory;
import field.math.linalg.Vector2;


public class StandardTextBrackets {

	static public class South implements iBracketFactory {

		private final float underline;

		private final float footsize;

		private final float extraspace;

		// underline is a continuous parameter underline == 0 means at the baseline, underline == 1 means at the descenderline, underline == 2 means at the bottom of leading line
		public South(float underline, float extraspace, float footsize) {
			this.underline = underline;
			this.extraspace = extraspace;
			this.footsize = footsize;
		}

		public List<BracketElement> getBracketFor(AdvancedTextToCachedLine layout) {
			BracketElement foot1 = new BracketElement();
			BracketElement line = new BracketElement();
			BracketElement foot2 = new BracketElement();

			line.type = BracketElementType.line;

			Rect bottom = layout.getBottomBounds();
			Rect desc = layout.getDescenderBounds();
			Rect full = layout.getFullRect();

			Rect rect = interpolateRect(bottom, desc, full, underline);

			Vector2 left = new Vector2(rect.x, rect.y + rect.h);
			Vector2 right = new Vector2(rect.x + rect.w, rect.y + rect.h);

			left.x -= extraspace;
			left.y += extraspace;
			right.x += extraspace;
			right.y += extraspace;

			line.start = left;
			line.end = right;

			foot1.type = BracketElementType.foot;
			foot1.start = left;
			foot1.end = new Vector2(left.x, left.y - footsize * layout.getFontSize());

			foot2.type = BracketElementType.foot;
			foot2.start = right;
			foot2.end = new Vector2(right.x, right.y - footsize * layout.getFontSize());

			return Arrays.asList(new BracketElement[] { foot1, line, foot2});

		}

		public Vector2 getPositionForLink(AdvancedTextToCachedLine layout) {

			Rect bottom = layout.getBottomBounds();
			Rect desc = layout.getDescenderBounds();
			Rect full = layout.getFullRect();

			Rect rect = interpolateRect(bottom, desc, full, underline);

			Vector2 left = new Vector2(rect.x, rect.y + rect.h);
			Vector2 right = new Vector2(rect.x + rect.w, rect.y + rect.h);

			left.x -= extraspace;
			left.y += extraspace;
			right.x += extraspace;
			right.y += extraspace;

			return new Vector2(left).interpolate(right, 0.5f);
		}
	}

	static public class North implements iBracketFactory {

		private final float overline;

		private final float footsize;

		private final float extraspace;

		// overline is a continuous parameter overline == 0 means at the top of the midline , underline == 1 means at top of the topline, underline == 2 means at the top of the ascender line
		public North(float overline, float extraspace, float footsize) {
			this.overline = overline;
			this.extraspace = extraspace;
			this.footsize = footsize;
		}

		public List<BracketElement> getBracketFor(AdvancedTextToCachedLine layout) {
			BracketElement foot1 = new BracketElement();
			BracketElement line = new BracketElement();
			BracketElement foot2 = new BracketElement();

			line.type = BracketElementType.line;

			Rect top = layout.getMiddleBounds();
			Rect desc = layout.getTopBounds();
			Rect full = layout.getFullRect();

			Rect rect = interpolateRect(top, desc, full, overline);

			Vector2 left = new Vector2(rect.x, rect.y);
			Vector2 right = new Vector2(rect.x + rect.w, rect.y);

			left.x -= extraspace;
			left.y -= extraspace;
			right.x += extraspace;
			right.y -= extraspace;

			line.start = left;
			line.end = right;

			foot1.type = BracketElementType.foot;
			foot1.start = left;
			foot1.end = new Vector2(left.x, left.y + footsize * layout.getFontSize());

			foot2.type = BracketElementType.foot;
			foot2.start = right;
			foot2.end = new Vector2(right.x, right.y + footsize * layout.getFontSize());

			return Arrays.asList(new BracketElement[] { foot1, line, foot2});

		}

		public Vector2 getPositionForLink(AdvancedTextToCachedLine layout) {
			Rect top = layout.getMiddleBounds();
			Rect desc = layout.getTopBounds();
			Rect full = layout.getFullRect();

			Rect rect = interpolateRect(top, desc, full, overline);

			Vector2 left = new Vector2(rect.x, rect.y);
			Vector2 right = new Vector2(rect.x + rect.w, rect.y);

			left.x -= extraspace;
			left.y -= extraspace;
			right.x += extraspace;
			right.y -= extraspace;

			return new Vector2(left).interpolate(right, 0.5f);
		}
	}

	static public class East implements iBracketFactory {

		private final float overline;

		private final float footsize;

		private final float extraspace;

		private final float underline;

		public East(float overline, float underline, float extraspace, float footsize) {
			this.overline = overline;
			this.underline = underline;
			this.extraspace = extraspace;
			this.footsize = footsize;
		}

		public List<BracketElement> getBracketFor(AdvancedTextToCachedLine layout) {
			BracketElement foot1 = new BracketElement();
			BracketElement line = new BracketElement();
			BracketElement foot2 = new BracketElement();

			line.type = BracketElementType.line;

			Rect rectTop, rectBottom;
			{
				Rect top = layout.getMiddleBounds();
				Rect desc = layout.getTopBounds();
				Rect full = layout.getFullRect();

				rectTop = interpolateRect(top, desc, full, overline);
			}
			{
				Rect bottom = layout.getBottomBounds();
				Rect desc = layout.getDescenderBounds();
				Rect full = layout.getFullRect();

				rectBottom = interpolateRect(bottom, desc, full, underline);
			}

			Vector2 top = new Vector2(Math.min(rectTop.x, rectBottom.x), rectTop.y);
			Vector2 bottom = new Vector2(Math.min(rectTop.x, rectBottom.x), rectBottom.y + rectBottom.h);

			top.x -= extraspace;
			top.y -= extraspace;
			bottom.x -= extraspace;
			bottom.y += extraspace;

			line.start = top;
			line.end = bottom;

			foot1.type = BracketElementType.foot;
			foot1.start = top;
			foot1.end = new Vector2(top.x + footsize * layout.getFontSize(), top.y);

			foot2.type = BracketElementType.foot;
			foot2.start = bottom;
			foot2.end = new Vector2(bottom.x + footsize * layout.getFontSize(), bottom.y);

			return Arrays.asList(new BracketElement[] { foot1, line, foot2});

		}

		public Vector2 getPositionForLink(AdvancedTextToCachedLine layout) {
			Rect rectTop, rectBottom;
			{
				Rect top = layout.getMiddleBounds();
				Rect desc = layout.getTopBounds();
				Rect full = layout.getFullRect();

				rectTop = interpolateRect(top, desc, full, overline);
			}
			{
				Rect bottom = layout.getBottomBounds();
				Rect desc = layout.getDescenderBounds();
				Rect full = layout.getFullRect();

				rectBottom = interpolateRect(bottom, desc, full, underline);
			}

			Vector2 top = new Vector2(Math.min(rectTop.x, rectBottom.x), rectTop.y);
			Vector2 bottom = new Vector2(Math.min(rectTop.x, rectBottom.x), rectBottom.y + rectBottom.h);

			top.x -= extraspace;
			top.y -= extraspace;
			bottom.x -= extraspace;
			bottom.y += extraspace;

			return new Vector2(top).interpolate(bottom, 0.5f);
		}
	}

	static public class West implements iBracketFactory {

		private final float overline;

		private final float footsize;

		private final float extraspace;

		private final float underline;

		public West(float overline, float underline, float extraspace, float footsize) {
			this.overline = overline;
			this.underline = underline;
			this.extraspace = extraspace;
			this.footsize = footsize;
		}

		public List<BracketElement> getBracketFor(AdvancedTextToCachedLine layout) {
			BracketElement foot1 = new BracketElement();
			BracketElement line = new BracketElement();
			BracketElement foot2 = new BracketElement();

			line.type = BracketElementType.line;

			Rect rectTop, rectBottom;
			{
				Rect top = layout.getMiddleBounds();
				Rect desc = layout.getTopBounds();
				Rect full = layout.getFullRect();

				rectTop = interpolateRect(top, desc, full, overline);
			}
			{
				Rect bottom = layout.getBottomBounds();
				Rect desc = layout.getDescenderBounds();
				Rect full = layout.getFullRect();

				rectBottom = interpolateRect(bottom, desc, full, underline);
			}

			Vector2 top = new Vector2(Math.max(rectTop.x + rectTop.w, rectBottom.x + rectBottom.w), rectTop.y);
			Vector2 bottom = new Vector2(Math.max(rectTop.x + rectTop.w, rectBottom.x + rectBottom.w), rectBottom.y + rectBottom.h);

			top.x += extraspace;
			top.y -= extraspace;
			bottom.x += extraspace;
			bottom.y += extraspace;

			line.start = top;
			line.end = bottom;

			foot1.type = BracketElementType.foot;
			foot1.start = top;
			foot1.end = new Vector2(top.x - footsize * layout.getFontSize(), top.y);

			foot2.type = BracketElementType.foot;
			foot2.start = bottom;
			foot2.end = new Vector2(bottom.x - footsize * layout.getFontSize(), bottom.y);

			return Arrays.asList(new BracketElement[] { foot1, line, foot2});

		}

		public Vector2 getPositionForLink(AdvancedTextToCachedLine layout) {

			Rect rectTop, rectBottom;
			{
				Rect top = layout.getMiddleBounds();
				Rect desc = layout.getTopBounds();
				Rect full = layout.getFullRect();

				rectTop = interpolateRect(top, desc, full, overline);
			}
			{
				Rect bottom = layout.getBottomBounds();
				Rect desc = layout.getDescenderBounds();
				Rect full = layout.getFullRect();

				rectBottom = interpolateRect(bottom, desc, full, underline);
			}

			Vector2 top = new Vector2(Math.max(rectTop.x + rectTop.w, rectBottom.x + rectBottom.w), rectTop.y);
			Vector2 bottom = new Vector2(Math.max(rectTop.x + rectTop.w, rectBottom.x + rectBottom.w), rectBottom.y + rectBottom.h);

			top.x += extraspace;
			top.y -= extraspace;
			bottom.x += extraspace;
			bottom.y += extraspace;

			return new Vector2(top).interpolate(bottom, 0.5f);
		}
	}

	static public class NorthEast implements iBracketFactory {
		private final float overline;

		private final float footsize;

		private final float extraspace;

		private final float underline;

		public NorthEast(float overline, float underline, float extraspace, float footsize) {
			this.overline = overline;
			this.underline = underline;
			this.extraspace = extraspace;
			this.footsize = footsize;
		}

		public List<BracketElement> getBracketFor(AdvancedTextToCachedLine layout) {
			BracketElement foot1 = new BracketElement();
			BracketElement line1 = new BracketElement();
			BracketElement corner = new BracketElement();
			BracketElement line2 = new BracketElement();
			BracketElement foot2 = new BracketElement();

			Rect rectTop, rectBottom;
			{
				Rect top = layout.getMiddleBounds();
				Rect desc = layout.getTopBounds();
				Rect full = layout.getFullRect();

				rectTop = interpolateRect(top, desc, full, overline);
			}
			{
				Rect bottom = layout.getBottomBounds();
				Rect desc = layout.getDescenderBounds();
				Rect full = layout.getFullRect();

				rectBottom = interpolateRect(bottom, desc, full, underline);
			}

			Vector2 top = new Vector2(Math.min(rectTop.x, rectBottom.x), rectTop.y);
			{
				Vector2 bottom = new Vector2(Math.min(rectTop.x, rectBottom.x), rectBottom.y + rectBottom.h);

				top.x -= extraspace;
				top.y -= extraspace;
				bottom.x -= extraspace;
				bottom.y += extraspace;

				line1.start = bottom;
				line1.end = top;

				foot1.type = BracketElementType.foot;
				foot1.start = bottom;
				foot1.end = new Vector2(bottom.x + footsize * layout.getFontSize(), bottom.y);

				line1.type = BracketElementType.line;
			}
			{
				Vector2 left = top;
				Vector2 right = new Vector2(rectTop.x + rectTop.w, left.y);

				// left.x -= extraspace;
				// left.y -= extraspace;
				right.x += extraspace;
				// right.y -= extraspace;

				line2.start = left;
				line2.end = right;

				foot2.type = BracketElementType.foot;
				foot2.start = right;
				foot2.end = new Vector2(right.x, right.y + footsize * layout.getFontSize());

				line2.type = BracketElementType.line;
			}

			corner.type = BracketElementType.corner;
			corner.start = top;
			corner.end = top;

			return Arrays.asList(new BracketElement[] { foot1, line1, corner, line2, foot2});

		}

		public Vector2 getPositionForLink(AdvancedTextToCachedLine layout) {
			Rect rectTop, rectBottom;
			{
				Rect top = layout.getMiddleBounds();
				Rect desc = layout.getTopBounds();
				Rect full = layout.getFullRect();

				rectTop = interpolateRect(top, desc, full, overline);
			}
			{
				Rect bottom = layout.getBottomBounds();
				Rect desc = layout.getDescenderBounds();
				Rect full = layout.getFullRect();

				rectBottom = interpolateRect(bottom, desc, full, underline);
			}

			Vector2 top = new Vector2(Math.min(rectTop.x, rectBottom.x), rectTop.y);
			{
				Vector2 bottom = new Vector2(Math.min(rectTop.x, rectBottom.x), rectBottom.y + rectBottom.h);

				top.x -= extraspace;
				top.y -= extraspace;
				bottom.x -= extraspace;
				bottom.y += extraspace;

			}
			return top;

		}
	}

	static public class NorthWest implements iBracketFactory {
		private final float overline;

		private final float footsize;

		private final float extraspace;

		private final float underline;

		public NorthWest(float overline, float underline, float extraspace, float footsize) {
			this.overline = overline;
			this.underline = underline;
			this.extraspace = extraspace;
			this.footsize = footsize;
		}

		public List<BracketElement> getBracketFor(AdvancedTextToCachedLine layout) {
			BracketElement foot1 = new BracketElement();
			BracketElement line1 = new BracketElement();
			BracketElement corner = new BracketElement();
			BracketElement line2 = new BracketElement();
			BracketElement foot2 = new BracketElement();

			Rect rectTop, rectBottom;
			{
				Rect top = layout.getMiddleBounds();
				Rect desc = layout.getTopBounds();
				Rect full = layout.getFullRect();

				rectTop = interpolateRect(top, desc, full, overline);
			}
			{
				Rect bottom = layout.getBottomBounds();
				Rect desc = layout.getDescenderBounds();
				Rect full = layout.getFullRect();

				rectBottom = interpolateRect(bottom, desc, full, underline);
			}

			Vector2 top = new Vector2(Math.max(rectTop.x + rectTop.w, rectBottom.x + rectBottom.w), rectTop.y);
			{
				Vector2 bottom = new Vector2(Math.max(rectTop.x + rectTop.w, rectBottom.x + rectBottom.w), rectBottom.y + rectBottom.h);

				top.x += extraspace;
				top.y -= extraspace;
				bottom.x += extraspace;
				bottom.y += extraspace;

				line1.end = top;
				line1.start = bottom;

				foot1.type = BracketElementType.foot;
				foot1.start = bottom;
				foot1.end = new Vector2(bottom.x - footsize * layout.getFontSize(), bottom.y);

				line1.type = BracketElementType.line;
			}
			{
				Vector2 left = top;
				Vector2 right = new Vector2(rectTop.x, left.y);

				// left.x -= extraspace;
				// left.y -= extraspace;
				right.x -= extraspace;
				// right.y += extraspace;

				line2.start = left;
				line2.end = right;

				foot2.type = BracketElementType.foot;
				foot2.start = right;
				foot2.end = new Vector2(right.x, right.y + footsize * layout.getFontSize());

				line2.type = BracketElementType.line;
			}

			corner.type = BracketElementType.corner;
			corner.start = top;
			corner.end = top;

			return Arrays.asList(new BracketElement[] { foot1, line1, corner, line2, foot2});

		}

		public Vector2 getPositionForLink(AdvancedTextToCachedLine layout) {
			BracketElement foot1 = new BracketElement();
			BracketElement line1 = new BracketElement();

			Rect rectTop, rectBottom;
			{
				Rect top = layout.getMiddleBounds();
				Rect desc = layout.getTopBounds();
				Rect full = layout.getFullRect();

				rectTop = interpolateRect(top, desc, full, overline);
			}
			{
				Rect bottom = layout.getBottomBounds();
				Rect desc = layout.getDescenderBounds();
				Rect full = layout.getFullRect();

				rectBottom = interpolateRect(bottom, desc, full, underline);
			}

			Vector2 top = new Vector2(Math.max(rectTop.x + rectTop.w, rectBottom.x + rectBottom.w), rectTop.y);
			{
				Vector2 bottom = new Vector2(Math.max(rectTop.x + rectTop.w, rectBottom.x + rectBottom.w), rectBottom.y + rectBottom.h);

				top.x += extraspace;
				top.y -= extraspace;
				bottom.x += extraspace;
				bottom.y += extraspace;

				line1.start = top;
				line1.end = bottom;

				foot1.type = BracketElementType.foot;
				foot1.start = bottom;
				foot1.end = new Vector2(bottom.x - footsize * layout.getFontSize(), bottom.y);

				line1.type = BracketElementType.line;
			}
			return top;
		}
	}

	static public class SouthWest implements iBracketFactory {
		private final float overline;

		private final float footsize;

		private final float extraspace;

		private final float underline;

		public SouthWest(float overline, float underline, float extraspace, float footsize) {
			this.overline = overline;
			this.underline = underline;
			this.extraspace = extraspace;
			this.footsize = footsize;
		}

		public List<BracketElement> getBracketFor(AdvancedTextToCachedLine layout) {
			BracketElement foot1 = new BracketElement();
			BracketElement line1 = new BracketElement();
			BracketElement corner = new BracketElement();
			BracketElement line2 = new BracketElement();
			BracketElement foot2 = new BracketElement();

			Rect rectTop, rectBottom;
			{
				Rect top = layout.getMiddleBounds();
				Rect desc = layout.getTopBounds();
				Rect full = layout.getFullRect();

				rectTop = interpolateRect(top, desc, full, overline);
			}
			{
				Rect bottom = layout.getBottomBounds();
				Rect desc = layout.getDescenderBounds();
				Rect full = layout.getFullRect();

				rectBottom = interpolateRect(bottom, desc, full, underline);
			}

			Vector2 bottom = new Vector2(Math.max(rectTop.x + rectTop.w, rectBottom.x + rectBottom.w), rectBottom.y + rectBottom.h);
			{
				Vector2 top = new Vector2(Math.max(rectTop.x + rectTop.w, rectBottom.x + rectBottom.w), rectTop.y);

				top.x += extraspace;
				top.y -= extraspace;
				bottom.x += extraspace;
				bottom.y += extraspace;

				line1.start = top;
				line1.end = bottom;

				foot1.type = BracketElementType.foot;
				foot1.start = top;
				foot1.end = new Vector2(top.x - footsize * layout.getFontSize(), top.y);

				line1.type = BracketElementType.line;
			}
			{
				Vector2 left = bottom;
				Vector2 right = new Vector2(rectBottom.x, left.y);

				// left.x -= extraspace;
				// left.y -= extraspace;
				right.x -= extraspace;
				// right.y += extraspace;

				line2.start = left;
				line2.end = right;

				foot2.type = BracketElementType.foot;
				foot2.start = right;
				foot2.end = new Vector2(right.x, right.y - footsize * layout.getFontSize());

				line2.type = BracketElementType.line;
			}

			corner.type = BracketElementType.corner;
			corner.start = bottom;
			corner.end = bottom;

			return Arrays.asList(new BracketElement[] { foot1, line1, corner, line2, foot2});

		}

		public Vector2 getPositionForLink(AdvancedTextToCachedLine layout) {
			BracketElement foot1 = new BracketElement();
			BracketElement line1 = new BracketElement();
			BracketElement corner = new BracketElement();
			BracketElement line2 = new BracketElement();
			BracketElement foot2 = new BracketElement();

			Rect rectTop, rectBottom;
			{
				Rect top = layout.getMiddleBounds();
				Rect desc = layout.getTopBounds();
				Rect full = layout.getFullRect();

				rectTop = interpolateRect(top, desc, full, overline);
			}
			{
				Rect bottom = layout.getBottomBounds();
				Rect desc = layout.getDescenderBounds();
				Rect full = layout.getFullRect();

				rectBottom = interpolateRect(bottom, desc, full, underline);
			}

			Vector2 bottom = new Vector2(Math.max(rectTop.x + rectTop.w, rectBottom.x + rectBottom.w), rectBottom.y + rectBottom.h);
			{
				Vector2 top = new Vector2(Math.max(rectTop.x + rectTop.w, rectBottom.x + rectBottom.w), rectTop.y);

				top.x += extraspace;
				top.y -= extraspace;
				bottom.x += extraspace;
				bottom.y += extraspace;

				line1.start = top;
				line1.end = bottom;

				foot1.type = BracketElementType.foot;
				foot1.start = top;
				foot1.end = new Vector2(top.x - footsize * layout.getFontSize(), top.y);

				line1.type = BracketElementType.line;
			}
			{
				Vector2 left = bottom;
				Vector2 right = new Vector2(rectBottom.x, left.y);

				// left.x -= extraspace;
				// left.y -= extraspace;
				right.x -= extraspace;
				// right.y += extraspace;

				line2.start = left;
				line2.end = right;

				foot2.type = BracketElementType.foot;
				foot2.start = right;
				foot2.end = new Vector2(right.x, right.y - footsize * layout.getFontSize());

				line2.type = BracketElementType.line;
			}

			corner.type = BracketElementType.corner;
			corner.start = bottom;
			return bottom;
		}
	}

	static public class SouthEast implements iBracketFactory {
		private final float overline;

		private final float footsize;

		private final float extraspace;

		private final float underline;

		public SouthEast(float overline, float underline, float extraspace, float footsize) {
			this.overline = overline;
			this.underline = underline;
			this.extraspace = extraspace;
			this.footsize = footsize;
		}

		public List<BracketElement> getBracketFor(AdvancedTextToCachedLine layout) {
			BracketElement foot1 = new BracketElement();
			BracketElement line1 = new BracketElement();
			BracketElement corner = new BracketElement();
			BracketElement line2 = new BracketElement();
			BracketElement foot2 = new BracketElement();

			Rect rectTop, rectBottom;
			{
				Rect top = layout.getMiddleBounds();
				Rect desc = layout.getTopBounds();
				Rect full = layout.getFullRect();

				rectTop = interpolateRect(top, desc, full, overline);
			}
			{
				Rect bottom = layout.getBottomBounds();
				Rect desc = layout.getDescenderBounds();
				Rect full = layout.getFullRect();

				rectBottom = interpolateRect(bottom, desc, full, underline);
			}

			Vector2 bottom = new Vector2(Math.min(rectTop.x, rectBottom.x), rectBottom.y + rectBottom.h);
			{
				Vector2 top = new Vector2(Math.min(rectTop.x, rectBottom.x), rectTop.y);

				top.x -= extraspace;
				top.y -= extraspace;
				bottom.x -= extraspace;
				bottom.y += extraspace;

				line1.start = top;
				line1.end = bottom;

				foot1.type = BracketElementType.foot;
				foot1.start = top;
				foot1.end = new Vector2(top.x + footsize * layout.getFontSize(), top.y);

				line1.type = BracketElementType.line;
			}
			{
				Vector2 left = bottom;
				Vector2 right = new Vector2(rectTop.x + rectTop.w, left.y);

				// left.x -= extraspace;
				// left.y -= extraspace;
				right.x += extraspace;
				// right.y -= extraspace;

				line2.start = left;
				line2.end = right;

				foot2.type = BracketElementType.foot;
				foot2.start = right;
				foot2.end = new Vector2(right.x, right.y - footsize * layout.getFontSize());

				line2.type = BracketElementType.line;
			}

			corner.type = BracketElementType.corner;
			corner.start = bottom;
			corner.end = bottom;

			return Arrays.asList(new BracketElement[] { foot1, line1, corner, line2, foot2});
		}

		public Vector2 getPositionForLink(AdvancedTextToCachedLine layout) {
			BracketElement foot1 = new BracketElement();
			BracketElement line1 = new BracketElement();
			BracketElement corner = new BracketElement();
			BracketElement line2 = new BracketElement();
			BracketElement foot2 = new BracketElement();

			Rect rectTop, rectBottom;
			{
				Rect top = layout.getMiddleBounds();
				Rect desc = layout.getTopBounds();
				Rect full = layout.getFullRect();

				rectTop = interpolateRect(top, desc, full, overline);
			}
			{
				Rect bottom = layout.getBottomBounds();
				Rect desc = layout.getDescenderBounds();
				Rect full = layout.getFullRect();

				rectBottom = interpolateRect(bottom, desc, full, underline);
			}

			Vector2 bottom = new Vector2(Math.min(rectTop.x, rectBottom.x), rectBottom.y + rectBottom.h);
			{
				Vector2 top = new Vector2(Math.min(rectTop.x, rectBottom.x), rectTop.y);

				top.x -= extraspace;
				top.y -= extraspace;
				bottom.x -= extraspace;
				bottom.y += extraspace;

				line1.start = top;
				line1.end = bottom;

				foot1.type = BracketElementType.foot;
				foot1.start = top;
				foot1.end = new Vector2(top.x + footsize * layout.getFontSize(), top.y);

				line1.type = BracketElementType.line;
			}
			{
				Vector2 left = bottom;
				Vector2 right = new Vector2(rectTop.x + rectTop.w, left.y);

				// left.x -= extraspace;
				// left.y -= extraspace;
				right.x += extraspace;
				// right.y -= extraspace;

				line2.start = left;
				line2.end = right;

				foot2.type = BracketElementType.foot;
				foot2.start = right;
				foot2.end = new Vector2(right.x, right.y - footsize * layout.getFontSize());

				line2.type = BracketElementType.line;
			}

			corner.type = BracketElementType.corner;
			corner.start = bottom;
			corner.end = bottom;

			return bottom;
		}
	}

	static public iBracketFactory[] connectTwoTextElements(float extraSpace, float feetSize, float proximity, AdvancedTextToCachedLine from, AdvancedTextToCachedLine to) {
		ArrayList<CachedLine> q = new ArrayList<CachedLine>();

		iBracketFactory[] factories = { new North(proximity, extraSpace, feetSize), new South(proximity, extraSpace, feetSize), new West(proximity, proximity, extraSpace, feetSize), new East(proximity, proximity, extraSpace, feetSize), new NorthEast(proximity, proximity, extraSpace, feetSize), new NorthWest(proximity, proximity, extraSpace, feetSize),
			new SouthWest(proximity, proximity, extraSpace, feetSize), new SouthEast(proximity, proximity, extraSpace, feetSize)};

		float[] weights = { 1, 1, 1.5f, 1.5f, 2.5f, 2.5f, 2.5f, 2.5f};

		int[] best = { -1, -1};
		float bestIs = Float.POSITIVE_INFINITY;

		for (int x = 0; x < factories.length; x++) {
			List<BracketElement> l1 = factories[x].getBracketFor(from);
			CachedLine line = TextBrackets.drawBracketElementsNoFeet(l1);
			float d1 = (line.events.size() - 1) * 0.5f;
			Vector2 middle1 = LineUtils.getPointOnLine(line, d1);
			Vector2 cheap1 = LineUtils.getPointOnLine(line, d1 - 0.25f).sub(LineUtils.getPointOnLine(line, d1 + 0.25f));

			for (int y = 0; y < factories.length; y++) {
				List<BracketElement> l2 = factories[y].getBracketFor(to);
				CachedLine line2 = TextBrackets.drawBracketElementsNoFeet(l2);
				float d2 = (line2.events.size() - 1) * 0.5f;
				Vector2 middle2 = LineUtils.getPointOnLine(line2, d2);
				Vector2 cheap2 = LineUtils.getPointOnLine(line, d1 - 0.25f).sub(LineUtils.getPointOnLine(line, d1 + 0.25f));

				float a1 = Math.min(cheap1.mag() / middle1.distanceFrom(middle2), 0.5f);
				float dd1 = new Vector2(middle1).sub(middle2).projectOut(cheap1, a1).mag() * weights[x];
				float a2 = Math.min(cheap2.mag() / middle1.distanceFrom(middle2), 0.5f);

				a1 = a1 * a1;
				a2 = a2 * a2;

				float dd2 = new Vector2(middle1).sub(middle2).projectOut(cheap2, a2).mag() * weights[y];
				float dd = dd1 + dd2;

				if (dd < bestIs) {
					bestIs = dd;
					best[0] = x;
					best[1] = y;
				}

			}
		}

		return new iBracketFactory[] { factories[best[0]], factories[best[1]]};

	}

	static public iBracketFactory connectOneTextElement(float extraSpace, float feetSize, float proximity, AdvancedTextToCachedLine from, Vector2 to) {
		ArrayList<CachedLine> q = new ArrayList<CachedLine>();

		iBracketFactory[] factories = { new North(proximity, extraSpace, feetSize), new South(proximity, extraSpace, feetSize), new West(proximity, proximity, extraSpace, feetSize), new East(proximity, proximity, extraSpace, feetSize), new NorthEast(proximity, proximity, extraSpace, feetSize), new NorthWest(proximity, proximity, extraSpace, feetSize),
			new SouthWest(proximity, proximity, extraSpace, feetSize), new SouthEast(proximity, proximity, extraSpace, feetSize)};

		float[] weights = { 1, 1, 1.5f, 1.5f, 2.5f, 2.5f, 2.5f, 2.5f};

		int[] best = { -1, -1};
		float bestIs = Float.POSITIVE_INFINITY;

		for (int x = 0; x < factories.length; x++) {
			List<BracketElement> l1 = factories[x].getBracketFor(from);
			CachedLine line = TextBrackets.drawBracketElementsNoFeet(l1);
			float d1 = (line.events.size() - 1) * 0.5f;
			Vector2 middle1 = LineUtils.getPointOnLine(line, d1);
			Vector2 cheap1 = LineUtils.getPointOnLine(line, d1 - 0.25f).sub(LineUtils.getPointOnLine(line, d1 + 0.25f));

			Vector2 middle2 = to;
			Vector2 cheap2 = LineUtils.getPointOnLine(line, d1 - 0.25f).sub(LineUtils.getPointOnLine(line, d1 + 0.25f));

			float a1 = Math.min(cheap1.mag() / middle1.distanceFrom(middle2), 0.5f);
			float dd1 = new Vector2(middle1).sub(middle2).projectOut(cheap1, a1).mag() * weights[x];
			float a2 = Math.min(cheap2.mag() / middle1.distanceFrom(middle2), 0.5f);

			a1 = a1 * a1;
			a2 = a2 * a2;

			float dd2 = new Vector2(middle1).sub(middle2).projectOut(cheap2, a2).mag();
			float dd = dd1 + dd2;

			if (dd < bestIs) {
				bestIs = dd;
				best[0] = x;
			}

		}

		return factories[best[0]];

	}

	static public Vector2 middleFor(List<BracketElement> b) {
		CachedLine line2 = TextBrackets.drawBracketElementsNoFeet(b);
		float d2 = (line2.events.size() - 1) * 0.5f;
		Vector2 middle2 = LineUtils.getPointOnLine(line2, d2);
		return middle2;
	}

	static public Rect interpolateRect(Rect bottom, Rect desc, Rect full, float underline) {
		Rect left = null;
		Rect right = null;
		float alpha;
		if (underline < 1) {
			alpha = underline;
			left = bottom;
			right = desc;
		} else {
			alpha = underline - 1;
			left = desc;
			right = full;
		}

		double x = left.x * (1 - alpha) + alpha * right.x;
		double y = left.y * (1 - alpha) + alpha * right.y;

		double w = (left.x + left.w) * (1 - alpha) + alpha * (right.x + right.w) - x;
		double h = (left.y + left.h) * (1 - alpha) + alpha * (right.y + right.h) - y;

		return new Rect(x, y, w, h);
	}

}
