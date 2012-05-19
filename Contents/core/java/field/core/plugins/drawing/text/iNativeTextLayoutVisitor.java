package field.core.plugins.drawing.text;

public interface iNativeTextLayoutVisitor {

	public void visitBegin(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float oy);
	public void visitCharacterBegin(int charcode);
	public void visitPathBegin(int curveDegree);
	public void visitPathMoveTo(float x, float y);
	public void visitPathCubicCurveTo(float x, float y, float x1, float y1, float x2, float y2);
	public void visitPathQuadCurveTo(float x, float y, float x1, float y1);
	public void visitPathLineTo(float x, float y);
	public void visitPathClose();
	public void visitPathEnd();
	public void visitGlyphEnd(float imageBoundingX, float imageBoundingY, float imageBoundingW, float imageBoundingH);
	public void visitCharacterEnd();
	
}
