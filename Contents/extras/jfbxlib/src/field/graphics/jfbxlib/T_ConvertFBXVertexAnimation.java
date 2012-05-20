package field.graphics.jfbxlib;

import java.io.File;
import java.util.Iterator;

import field.graphics.jfbxlib.CoordinateSystemAnimation.AnimatedCoordinateSystem;
import field.graphics.jfbxlib.Persistence.Storage;
import field.launch.SystemProperties;
import field.math.BaseMath;
import field.math.graph.GraphNodeSearching;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.linalg.CoordinateFrame;
import field.math.util.CubicInterpolatorDynamic.Sample;


public class T_ConvertFBXVertexAnimation {

	public static void main(String[] args) {

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
		BuildMeshVisitor meshMaker = new BuildMeshVisitor(false, printMaker).setCaptureResultMeshes(true);
		//BuildSkinningVisitor skinMaker = new BuildSkinningVisitor(meshMaker);
		BuildTransformTreeVisitor treeMaker = new BuildTransformTreeVisitor(meshMaker);

		double startSeconds =  printMaker.convertJFBXTimeToSeconds(start);
		double endSeconds = printMaker.convertJFBXTimeToSeconds(end);
		;//;//System.out.println(" take <" + take + "> runs from <" + startSeconds + " / "+start+"> <" + endSeconds + ">");

		double fps = SystemProperties.getDoubleProperty("fps", 30);

		int num = (int) ((endSeconds - startSeconds) * fps) + 1;
		for (int i = 0; i < num; i++) {
			float t = i;

			long s = start + i * (end - start) / num;

			;//;//System.out.println(" time <" + startSeconds + " -> " + BaseMath.toDP(printMaker.convertJFBXTimeToSeconds(s), 3) + " -> " + endSeconds);
			main.acceptTime(s, markerAnimation.getVisitorForTime(t,  treeMaker));

//			//if (i == 0) 
//			{
//				main.noMoreGeometry();
//			}

		}

		treeMaker.finalizeTransformNames(treeMaker.getTransformMap());

		// if (SystemProperties.getDoubleProperty("error", 0) > 0)
		{
			Iterator<AnimatedCoordinateSystem> i = markerAnimation.roots.iterator();
			while (i.hasNext()) {
				new GraphNodeSearching.GraphNodeVisitor_depthFirst<CoordinateSystemAnimation.AnimatedCoordinateSystem>(false){

					@Override
					protected VisitCode visit(AnimatedCoordinateSystem n) {

//						if (n.name.toLowerCase().contains("spine_dummy")) 
							n.animation.printSamples(System.out);
						
						for(int i=0;i<n.animation.getNumSamples();i++)
						{
							Sample sample = n.animation.getSample(i);
							
							;//;//System.out.println(((CoordinateFrame)sample.data).getRotation(null).mag());
							if (((CoordinateFrame)sample.data).getRotation(null).mag()<0.5)
							{
								;//;//System.out.println(" -- warning, non unit rotation, replacing with nearest sample");
								boolean found = false;
								int q = i;
								while(q>0)
								{
									if (((CoordinateFrame)n.animation.getSample(q).data).getRotation(null).mag()>0.5)
									{
										((CoordinateFrame)n.animation.getSample(i).data).setRotation(((CoordinateFrame)n.animation.getSample(q).data).getRotation(null));
										found = true;
										break;
									}
									q--;
								}
								if (!found)
								{
									q = i;
									while(q<n.animation.getNumSamples())
									{
										if (((CoordinateFrame)n.animation.getSample(q).data).getRotation(null).mag()>0.5)
										{
											((CoordinateFrame)n.animation.getSample(i).data).setRotation(((CoordinateFrame)n.animation.getSample(q).data).getRotation(null));
											found = true;
											break;
										}
										q++;
									}
								}
								if (!found)
									;//;//System.out.println(" -- warning, found nothing");
								else
									;//;//System.out.println(" found <"+((CoordinateFrame)sample.data).getRotation(null)+">");
							}
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
		
		storage.meshAnimations = meshMaker.getMeshAnimations();
		//storage.skinningInfo = skinMaker.getSkinningInfos();

		File f = new File(SystemProperties.getProperty("out", SystemProperties.getProperty("in") + ".xml"));
		storage.save(p, f);
		storage = storage.load(p, f);
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
