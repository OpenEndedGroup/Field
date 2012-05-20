package field.graphics.jfbxlib;

import java.util.ArrayList;
import java.util.List;

import field.graphics.jfbxlib.CoordinateSystemAnimation.AnimatedCoordinateSystem;
import field.graphics.jfbxlib.Persistence.Storage;
import field.launch.SystemProperties;
import field.math.BaseMath;
import field.math.graph.GraphNodeSearching;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.namespace.generic.Generics.Pair;

public class T_ConvertFBXInplace {

	public Storage convert(String filename) {

		JFBXMain main = new JFBXMain();
		main.importFile(filename);
		int takes = main.getNumTakes();

		// if (takes==0)
		// throw new
		// NullPointerException(" fbx file has no takes \u2014 the file has to have at least one take before Field can load it");

		int take = SystemProperties.getIntProperty("take", Math.min(takes, 1));

		;//;//System.out.println(" using take <" + take + ">");

		if (take > takes - 1)
			take = takes - 1;

		long start = 0, end = 0;

		if (takes != 0)
		{
			main.setTake(take);
			start = main.getTakeStartTime();
			end = main.getTakeEndTime();
		}

		CoordinateSystemAnimation markerAnimation = new CoordinateSystemAnimation();

		PrintJFBXVisitor printMaker = new PrintJFBXVisitor(null);
		BuildMeshVisitor meshMaker = new BuildMeshVisitor(false, null);
		BuildSkinningVisitor skinMaker = new BuildSkinningVisitor(meshMaker);
		BuildTransformTreeVisitor treeMaker = new BuildTransformTreeVisitor(skinMaker);

		double startSeconds = AbstractVisitor.convertJFBXTimeToSeconds(start);
		double endSeconds = AbstractVisitor.convertJFBXTimeToSeconds(end);
		;//;//System.out.println(" take <" + take + "> runs from <" + startSeconds + "> <" + endSeconds + ">");

		double fps = SystemProperties.getDoubleProperty("fps", 30);

		int num = (int) ((endSeconds - startSeconds) * fps);

		// num = 1;
		;//;//System.out.println(" ?x? ");

		if (num < 1)
			num = 1;
		main.moreGeometry();

		for (int i = 0; i < num; i++) {
			float t = i;

			long s = (long) (start + (num == 1 ? 0 : (i * (end - start) / (num - 1d))));

			;//;//System.out.println(" time <" + startSeconds + " -> " + BaseMath.toDP(AbstractVisitor.convertJFBXTimeToSeconds(s), 3) + " -> " + endSeconds);
			main.acceptTime(s, markerAnimation.getVisitorForTime(t, (i == 0 ? treeMaker : null)));

			if (i == 0) {
				main.noMoreGeometry();
			}

		}

		treeMaker.finalizeTransformNames(treeMaker.getTransformMap());

		Persistence p = new Persistence();
		Storage storage = new Persistence.Storage();
		storage.coordinateSystemAnimationRoots = markerAnimation.getRoots();

		storage.animationStart = markerAnimation.start;
		storage.animationEnd = markerAnimation.end;

		storage.transforms = treeMaker.getTransforms();
		storage.meshes = meshMaker.getMeshes();
		storage.skinningInfo = skinMaker.getSkinningInfos();

		if (SystemProperties.getIntProperty("noMarkers", 0) == 1) {
			storage.animationRoots.clear();

			final List<Pair<AnimatedCoordinateSystem, AnimatedCoordinateSystem>> unlink = new ArrayList<Pair<AnimatedCoordinateSystem, AnimatedCoordinateSystem>>();
			for (AnimatedCoordinateSystem a : storage.coordinateSystemAnimationRoots) {
				;//;//System.out.println(" animted <" + a + ">");
				new GraphNodeSearching.GraphNodeVisitor_depthFirst<AnimatedCoordinateSystem>(true) {
					@Override
					protected VisitCode visit(AnimatedCoordinateSystem n) {
						;//;//System.out.println(" checking <" + n.name + ">");
						if (n.name.toLowerCase().contains("optics") || n.name.contains("TRC")) {
							if (n.getParents().size() > 0)
								unlink.add(new Pair<AnimatedCoordinateSystem, AnimatedCoordinateSystem>((AnimatedCoordinateSystem) n.getParents().get(0), n));
						}
						return VisitCode.cont;
					}
				}.visit(a);
			}
			for (Pair<AnimatedCoordinateSystem, AnimatedCoordinateSystem> pp : unlink) {
				;//;//System.out.println(" deleting <" + pp + ">");
				pp.left.removeChild(pp.right);
			}
		}

		return storage;
	}

	protected static String spaces(int indent) {
		String s = "";
		for (int i = 0; i < indent * 2; i++)
			s = s + " ";
		return s;
	}
}
