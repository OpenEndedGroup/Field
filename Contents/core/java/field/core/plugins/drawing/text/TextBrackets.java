package field.core.plugins.drawing.text;

import java.util.List;

import field.core.plugins.drawing.opengl.CachedLine;
import field.math.linalg.Vector2;


public class TextBrackets {

	static public enum BracketElementType {
		line, foot, corner, fixedExternalPosition;
	}

	static public class BracketElement {
		BracketElementType type;

		Vector2 start;

		Vector2 end;
	}

	static public class BracketConnection {
		BracketElementType from;

		float alphaFrom;

		BracketElementType to;

		float alphaTo;
	}

	static public interface iBracketFactory {
		public List<BracketElement> getBracketFor(AdvancedTextToCachedLine layout);

		public Vector2 getPositionForLink(AdvancedTextToCachedLine layout);

	}

	static public interface iBracketConnectionFactory {
		public BracketConnection getConnectionFor(AdvancedTextToCachedLine leftLayout, List<BracketElement> left, AdvancedTextToCachedLine rightLayout, List<BracketElement> right);
	}

	static public interface iBracketConnectionEvaluation {
		public float score(AdvancedTextToCachedLine leftLayout, List<BracketElement> left, AdvancedTextToCachedLine rightLayout, List<BracketElement> right, BracketConnection connection);
	}

	static public CachedLine drawBracketElements(List<BracketElement> elements) {
		CachedLine cl = new CachedLine();

		boolean f = true;

		for (BracketElement e : elements) {
			if (f)
				cl.getInput().moveTo(e.start.x, e.start.y);
			else
				cl.getInput().lineTo(e.start.x, e.start.y);
			cl.getInput().lineTo(e.end.x, e.end.y);
			f = false;
		}
		return cl;
	}

	static public CachedLine drawBracketElementsNoFeet(List<BracketElement> elements) {
		CachedLine cl = new CachedLine();

		boolean f = true;

		for (BracketElement e : elements) {
			if (e.type == BracketElementType.line) {
				if (f)
					cl.getInput().moveTo(e.start.x, e.start.y);
				cl.getInput().lineTo(e.end.x, e.end.y);
				f = false;
			}
		}
		return cl;
	}

}
