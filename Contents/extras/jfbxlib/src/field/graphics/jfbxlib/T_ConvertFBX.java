package field.graphics.jfbxlib;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import field.graphics.jfbxlib.CoordinateSystemAnimation.AnimatedCoordinateSystem;
import field.graphics.jfbxlib.Persistence.Storage;
import field.launch.SystemProperties;
import field.math.BaseMath;
import field.math.graph.GraphNodeSearching;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Quaternion;
import field.math.util.CubicInterpolatorDynamic;
import field.math.util.CubicInterpolatorDynamic.Sample;
import field.namespace.generic.Generics.Pair;

public class T_ConvertFBX {

	public static void main(String[] args) {

		;//;//System.out.println(" markers");
		JFBXMain main = new JFBXMain();
		main.importFile(SystemProperties.getProperty("in"));
		int takes = main.getNumTakes();
		;//;//System.out.println(" file has <" + takes + "> takes");

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
		;//;//System.out.println(" take <" + take + "> runs from <" + startSeconds + "> <" + endSeconds + ">");

		double fps = SystemProperties.getDoubleProperty("fps", 30);

		int num = (int) ((endSeconds - startSeconds) * fps) + 1;
		
		//num = 1;
		;//;//System.out.println(" ?x? ");
		
		for (int i = 0; i < num; i++) {
			float t = i;

			long s = start + i * (end - start) / num;

			;//;//System.out.println(" time <" + startSeconds + " -> " + BaseMath.toDP(AbstractVisitor.convertJFBXTimeToSeconds(s), 3) + " -> " + endSeconds);
			main.acceptTime(s, markerAnimation.getVisitorForTime(t, (i == 0 ? treeMaker : null))); 

			if (i == 0) {
				main.noMoreGeometry();
			}

		}

		treeMaker.finalizeTransformNames(treeMaker.getTransformMap());

		if (SystemProperties.getDoubleProperty("error", 0) > 0)
		{
			Iterator<AnimatedCoordinateSystem> i = markerAnimation.roots.iterator();
			while (i.hasNext()) {
				new GraphNodeSearching.GraphNodeVisitor_depthFirst<CoordinateSystemAnimation.AnimatedCoordinateSystem>(false) {

					private float distance(CoordinateFrame previous, CoordinateFrame data) {
						float d1 = previous.getTranslation(null).distanceFrom(data.getTranslation(null));
						float d2 = previous.getScale(null).distanceFrom(data.getScale(null));
						float d3 = (float) Quaternion.distAngular(previous.getRotation(null), data.getRotation(null));
						return d1+d2+d3;
					}

					private int eliminateRun(int runStart, int runStop, CubicInterpolatorDynamic<CoordinateFrame> animation) {

						int rem = 0;
						for(int i=runStart+3;i<runStop-3;i++)
						{
							animation.removeSample(runStart+3);
							rem++;
						}
						;//;//System.out.println(" removed <"+rem+"> from <"+runStart+" > "+runStop+">");
						return rem;
					}

					@Override
					protected VisitCode visit(AnimatedCoordinateSystem n) {

						// if (n.name.toLowerCase().contains("spine_dummy"))
						// n.animation.printSamples(System.out);

						for (int i = 0; i < n.animation.getNumSamples(); i++) {
							Sample sample = n.animation.getSample(i);

							// ;//;//System.out.println(((CoordinateFrame)sample.data).getRotation(null).mag());
							if (((CoordinateFrame) sample.data).getRotation(null).mag() < 0.5) {
								;//;//System.out.println(" -- warning, non unit rotation, replacing with nearest sample");
								boolean found = false;
								int q = i;
								while (q > 0) {
									if ((n.animation.getSample(q).data).getRotation(null).mag() > 0.5) {
										(n.animation.getSample(i).data).setRotation((n.animation.getSample(q).data).getRotation(null));
										found = true;
										break;
									}
									q--;
								}
								if (!found) {
									q = i;
									while (q < n.animation.getNumSamples()) {
										if ((n.animation.getSample(q).data).getRotation(null).mag() > 0.5) {
											(n.animation.getSample(i).data).setRotation((n.animation.getSample(q).data).getRotation(null));
											found = true;
											break;
										}
										q++;
									}
								}
								if (!found)
									;//;//System.out.println(" -- warning, found nothing");
								else
									;//;//System.out.println(" found <" + ((CoordinateFrame) sample.data).getRotation(null) + ">");
							}
						}

						int runStart = 0;
						int runStop = 0;

						CoordinateFrame previous = null;

						for (int i = 0; i < n.animation.getNumSamples(); i++) {
							CubicInterpolatorDynamic<CoordinateFrame>.Sample sample = n.animation.getSample(i);
							if (previous == null)
							{
								previous = sample.data;
							}
							else
							{
								float d = distance(previous, sample.data);
								if (d<1e-10)
								{
									runStop = i;
								}
								else
								{
									if (runStop-runStart>7)
									{
										int removed = eliminateRun(runStart, runStop, n.animation);
										i-=removed;
									}
									previous = sample.data;
									runStart = i;
									runStop = i;
								}
							}
						}

						if (runStop-runStart>7)
						{
							int removed = eliminateRun(runStart, runStop, n.animation);
						}

						// ;//;//System.out.println("\n\n\n\n\n\n\n\n\n\n visited <"+n.name+">");
						// ;//;//System.out.println(" for marker <" + n.name + ">");
						// int num = n.animation.getNumSamples();
						// for (int i = 0; i < num; i++) {
						// CubicInterpolatorDynamic<CoordinateFrame>.Sample s = n.animation.getSample(i);
						// ;//;//System.out.println(" <" + s + ">");
						// }
						//
						// DownsampleCubic<CoordinateFrame> down = new DownsampleCubic<CoordinateFrame>(new iMetric<CoordinateFrame, CoordinateFrame>(){
						//
						// Vector3 t1 = new Vector3();
						//
						// Vector3 t2 = new Vector3();
						//
						// Quaternion q1 = new Quaternion();
						//
						// Quaternion q2 = new Quaternion();
						//
						// public float distance(CoordinateFrame from, CoordinateFrame to) {
						//
						// float d1 = from.getTranslation(t1).distanceFrom(to.getTranslation(t2))/10.0f;
						// float d2 = (float) BaseMath.safeAcos(from.getRotation(q1).dot(to.getRotation(q2)));
						//
						// return d1+ d2;
						// }
						// });
						//
						// n.animation = down.greedyUpsampleWithError(n.animation, (float) SystemProperties.getDoubleProperty("error", 0), 1);

						return VisitCode.cont;
					}

				}.apply(i.next());
			}
		}
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
			for (AnimatedCoordinateSystem a : storage.coordinateSystemAnimationRoots)
			{
				;//;//System.out.println(" animted <"+a+">");
				new GraphNodeSearching.GraphNodeVisitor_depthFirst<AnimatedCoordinateSystem>(true) {
					@Override
					protected VisitCode visit(AnimatedCoordinateSystem n) {
						;//;//System.out.println(" checking <"+n.name+">");
						if (n.name.toLowerCase().contains("optics") || n.name.contains("TRC")) {
							if (n.getParents().size()>0)
								unlink.add(new Pair<AnimatedCoordinateSystem, AnimatedCoordinateSystem>((AnimatedCoordinateSystem) n.getParents().get(0), n));
						}
						return VisitCode.cont;
					}
				}.visit(a);
			}
			for(Pair<AnimatedCoordinateSystem, AnimatedCoordinateSystem> pp : unlink)
			{
				;//;//System.out.println(" deleting <"+pp+">");
				pp.left.removeChild(pp.right);
			}
		}

		File f = new File(SystemProperties.getProperty("out", SystemProperties.getProperty("in") + ".xml"));
		;//;//System.out.println(" saving xml ...");
		storage.save(p, f);
		;//;//System.out.println(" loading it back in again to test ... ");
		storage = storage.load(p, f);
		;//;//System.out.println(" finished. Roots are:");
		;//;//System.out.println(storage.meshes.keySet());
		System.exit(0);
	}

	protected static String spaces(int indent) {
		String s = "";
		for (int i = 0; i < indent * 2; i++)
			s = s + " ";
		return s;
	}
}
