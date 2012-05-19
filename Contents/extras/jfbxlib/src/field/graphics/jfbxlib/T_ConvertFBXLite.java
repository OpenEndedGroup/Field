package field.graphics.jfbxlib;

import java.io.File;

import field.graphics.jfbxlib.Persistence.Storage;
import field.launch.SystemProperties;
import field.math.BaseMath;

public class T_ConvertFBXLite {

	public static void main(String[] args) {

		JFBXMain main = new JFBXMain();
		main.importFile(SystemProperties.getProperty("in"));
		int takes = main.getNumTakes();
		System.out.println(" file has <" + takes + "> takes");

		int take = SystemProperties.getIntProperty("take", 1);

		main.setTake(take);
		long start = main.getTakeStartTime();
		long end = main.getTakeEndTime();

		CoordinateSystemAnimation markerAnimation = new CoordinateSystemAnimation();

		PrintJFBXVisitor printMaker = new PrintJFBXVisitor(null);
		BuildMeshVisitor meshMaker = new BuildMeshVisitor(false, null);
		BuildSkinningVisitor skinMaker = new BuildSkinningVisitor(meshMaker);
		BuildTransformTreeVisitor treeMaker = new BuildTransformTreeVisitor(skinMaker);

		double startSeconds = AbstractVisitor.convertJFBXTimeToSeconds(start);
		double endSeconds = AbstractVisitor.convertJFBXTimeToSeconds(end);
		System.out.println(" take <" + take + "> runs from <" + startSeconds + "> <" + endSeconds + ">");

		double fps = SystemProperties.getDoubleProperty("fps", 30);

		int num = (int) ((endSeconds - startSeconds) * fps) + 1;
		for (int i = 0; i < num; i++) {
			float t = i;

			long s = start + i * (end - start) / num;

			System.out.println(" time <" + startSeconds + " -> " + BaseMath.toDP(AbstractVisitor.convertJFBXTimeToSeconds(s), 3) + " -> " + endSeconds);
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

		File f = new File(SystemProperties.getProperty("out", SystemProperties.getProperty("in") + ".xml"));
		System.out.println(" saving xml ...");
		storage.save(p, f);
		System.out.println(" finished. Roots are:");
		System.out.println(storage.meshes.keySet());
		System.exit(0);
	}

}
