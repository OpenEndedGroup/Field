package field.graphics.dynamic;

import field.graphics.core.Base;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.BasicSceneList;
import field.graphics.core.BasicUtilities;
import field.graphics.core.PointList;
import field.graphics.core.Base.iAcceptsSceneListElement;
import field.math.linalg.Vector3;

/**
 * Created on Nov 2, 2003
 *
 * @author marc
 */
public class DynamicPointlist extends DynamicMesh implements iDynamicMesh {

	/**
	 * @param from
	 */
	public DynamicPointlist(Base.iGeometry from) {
		super(from);
	}

	/** @return */
	@Override
	public PointList getUnderlyingGeometry() {
		return (PointList) basis;
	}


	@Override
	public int nextFace(Vector3 v1, Vector3 v2, Vector3 v3) {
		nextVertex(v1);
		nextVertex(v2);
		nextVertex(v3);
		return -1;
	}

	@Override
	public int nextFace(int v1, int v2, int v3) {
		return -1;
	}

	@Override
	public int nextVertex(Vector3 v1) {
		return super.nextVertex(v1);
	}


	public static DynamicPointlist coloredPoints(BasicSceneList sceneList, float width) {

		BasicGLSLangProgram program = new BasicGLSLangProgram("content/shaders/TestGLSLangVertex.glslang", "content/shaders/VertexColorFragment.glslang");
		PointList lines = new PointList(new BasicUtilities.Position()).setSize(width);
		lines.rebuildTriangle(0);
		lines.rebuildVertex(0);
		lines.addChild(program);
		if (sceneList != null)
			sceneList.addChild(lines);
		DynamicPointlist ret = new DynamicPointlist(lines);
		return ret;
	}

	public static DynamicPointlist unshadedPoints(BasicSceneList sceneList, float width) {

		PointList lines = new PointList(new BasicUtilities.Position()).setSize(width);
		lines.rebuildTriangle(0);
		lines.rebuildVertex(0);
		if (sceneList != null)
			sceneList.addChild(lines);
		DynamicPointlist ret = new DynamicPointlist(lines);
		return ret;
	}

	public static DynamicPointlist texturedColoredPoints(iAcceptsSceneListElement acceptsSceneListElement, float width) {

		BasicGLSLangProgram program = new BasicGLSLangProgram("content/shaders/TestGLSLangVertex_withPointsize.glslang", "content/shaders/Texture2DForPointSprites.glslang");
//		BasicGLSLangProgram program = new BasicGLSLangProgram("content/shaders/TestGLSLangVertex_withPointsize.glslang", "content/shaders/WhiteFragment.glslang");
		program.new SetIntegerUniform("texture", 0);
		program.setDoPointSize();
		PointList lines = new PointList(new BasicUtilities.Position()).setSize(width);
		lines.rebuildTriangle(0);
		lines.rebuildVertex(0);
		lines.addChild(program);
		if (acceptsSceneListElement != null)
			acceptsSceneListElement.addChild(lines);
		DynamicPointlist ret = new DynamicPointlist(lines);
		return ret;
	}

	public static DynamicPointlist texturedColoredPoints2(iAcceptsSceneListElement acceptsSceneListElement, float width) {

		BasicGLSLangProgram program = new BasicGLSLangProgram("content/shaders/TestGLSLangVertex_withPointsize.glslang", "content/shaders/Texture2DForPointSprites2.glslang");
		program.new SetIntegerUniform("texture", 0);
		program.setDoPointSize();
		PointList lines = new PointList(new BasicUtilities.Position()).setSize(width);
		lines.rebuildTriangle(0);
		lines.rebuildVertex(0);
		lines.addChild(program);
		if (acceptsSceneListElement != null)
			acceptsSceneListElement.addChild(lines);
		DynamicPointlist ret = new DynamicPointlist(lines);
		return ret;
	}
}
