package field.core.plugins.drawing.text;

import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.PathIterator;

/**
 * this used to be a native call to ATUSI, but now it's pure java.
 * 
 * @author marc
 * 
 */
public class AdvancedText {

	// from http://developer.apple.com/textfonts/Registry/index.html
	// static public final int feature_case = 3;
	// static public final int feature_case_normal= 0;
	// static public final int feature_case_smallCaps = 3;
	//
	// static public final int feature_ligatures = 1;
	// static public final int feature_ligatures_common_on = 2;
	// static public final int feature_ligatures_common_off = 3;
	// static public final int feature_ligatures_rare_on = 4;
	// static public final int feature_ligatures_rare_off= 5;
	// static public final int feature_ligatures_square_on = 12;
	// static public final int feature_ligatures_square_off= 13;
	//	
	// static public final int feature_swash = 8;
	// static public final int feature_wordInitial_on= 0;
	// static public final int feature_wordInitial_off= 1;
	// static public final int feature_wordFinal_on= 2;
	// static public final int feature_wordFinal_off= 3;

	// static
	// {
	// System.loadLibrary("AdvancedText");
	// }

	public Font font;

	public void acceptString(String string, iNativeTextLayoutVisitor visitor) {

		FontRenderContext frc = new FontRenderContext(null, false, true);

		GlyphVector v = font.createGlyphVector(frc, string);
		Shape outline = v.getOutline();
		PathIterator pi = outline.getPathIterator(null);

		float[] cc = new float[6];
		float lx = 0;
		float ly = 0;
		while (!pi.isDone()) {
			int s = pi.currentSegment(cc);
			if (s == pi.SEG_MOVETO)
				visitor.visitPathMoveTo(cc[0], cc[1]);
			else if (s == pi.SEG_LINETO)
				visitor.visitPathLineTo(cc[0], cc[1]);
			else if (s == pi.SEG_CUBICTO)
				visitor.visitPathCubicCurveTo(cc[4], cc[5], cc[0], cc[1], cc[2], cc[3]);
			else if (s == pi.SEG_QUADTO) {
//				visitor.visitPathCubicCurveTo(cc[2], cc[3], (cc[0] - lx) * (2 / 3f) + lx, (cc[1] - ly) * (2 / 3f) + ly, (cc[0] - cc[2]) * (2 / 3f) + cc[2], (cc[1] - cc[3]) * (2 / 3f) + cc[3]);
				visitor.visitPathCubicCurveTo(cc[2], cc[3], (cc[0] - lx) * (2 / 3f) + lx, (cc[1] - ly) * (2 / 3f) + ly, cc[0]+(cc[2]-cc[0])/3f, cc[1]+(cc[3]-cc[1])/3f);
			} else if (s == pi.SEG_CLOSE) {
				visitor.visitPathClose();
			}

			lx = cc[0];
			ly = cc[1];

			pi.next();
		}
	}

	public void acceptStringWrap(String string, iNativeTextLayoutVisitor visitor, float width, float al, float ju) {
		throw new IllegalArgumentException(" not implemented ");
	}

	public void setFont(String string, int size) {
		font = new Font(string, 0, size);
	}

	public void setFont(String string, int flags, int size) {
		font = new Font(string, flags, size);
	}

	// public native void setFontFeature(int type, int selector);

}
