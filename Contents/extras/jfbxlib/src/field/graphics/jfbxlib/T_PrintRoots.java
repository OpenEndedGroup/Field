package field.graphics.jfbxlib;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import field.graphics.jfbxlib.Persistence;
import field.graphics.jfbxlib.BuildMeshVisitor.Mesh;
import field.graphics.jfbxlib.BuildTransformTreeVisitor.Transform;
import field.graphics.jfbxlib.CoordinateSystemAnimation.AnimatedCoordinateSystem;
import field.graphics.jfbxlib.MarkerAnimation.AnimatedMarker;
import field.graphics.jfbxlib.Persistence.Storage;
import field.launch.SystemProperties;
import field.launch.iLaunchable;
import field.math.graph.GraphNodeSearching;
import field.math.graph.SimpleNode;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.linalg.CoordinateFrame;
import field.math.util.CubicInterpolatorDynamic;
import field.namespace.generic.Generics.Pair;
import field.util.ANSIColorUtils;

public class T_PrintRoots implements iLaunchable {
	public void launch() {
		System.out.println(" loading");

		Persistence p = new Persistence();
		final Storage s = Persistence.Storage.load(p, new File(SystemProperties.getProperty("animation")));

		System.out.println(" file contains:");

		if (s.meshes != null)
			System.out.println("      " + s.meshes.size() + " meshes");
		if (s.skinningInfo != null)
			System.out.println("      " + s.skinningInfo.size() + " skins");
		if (s.animationRoots != null)
			System.out.println("      " + s.animationRoots.size() + " marker animations");
		if (s.coordinateSystemAnimationRoots != null)
			System.out.println("      " + s.coordinateSystemAnimationRoots.size() + " coordinate system root animations");

		if (s.transforms!=null)
		{
			System.out.println("      "+s.transforms.size()+" transforms");
			for(SimpleNode<Transform> sn : s.transforms.values())
			{
				if (sn.getParents().size()==0)
					new GraphNodeSearching.GraphNodeVisitor_depthFirst<SimpleNode<Transform>>(true) {
						@Override
						protected VisitCode visit(SimpleNode<Transform> n) {
							System.out.println(spaces(stack.size())+" transform <"+n.payload().name+">");
	//						System.out.println(n.payload().properties+" from "+n.payload().name);
	//
	//						if (n.payload().properties.size()!=0)
	//						{
	//							System.out.println("                     "+n.payload().name+" "+n.payload().properties);
	//						}
							return VisitCode.cont;
						}
					}.apply(sn);
			}
		}

		if (s.coordinateSystemAnimationRoots != null) {
			final int[] c = { 0 };
			for (AnimatedCoordinateSystem rr : s.coordinateSystemAnimationRoots)
				new GraphNodeSearching.GraphNodeVisitor_depthFirst<AnimatedCoordinateSystem>(true) {
					@Override
					protected VisitCode visit(AnimatedCoordinateSystem n) {
						c[0]++;

						CubicInterpolatorDynamic<CoordinateFrame> s =  n.animation;
						System.out.println(spaces(stack.size()*3)+" animation for <"+n.name+">");
						return VisitCode.cont;
					}
				}.apply(rr);
			System.out.println("      " + c[0] + " coordinate system animations in total");
		}
		if (s.meshes != null) {

			System.out.println(" mesh info (v/t): ");
			Set<Entry<String, Mesh>> e = s.meshes.entrySet();
			for (Entry<String, Mesh> name : e) {
				System.out.print("      " + ANSIColorUtils.red(name.getKey() + "") + " " + name.getValue().numVertex + "/" + name.getValue().numTriangle);
				if (s.skinningInfo.containsKey(name.getKey()))
					System.out.println(" skinned to <" + s.skinningInfo.get(name.getKey()).boneNames.length + "> bones");
				else
					System.out.println();
			}
		}
		if (s.meshAnimations != null) {
			System.out.println(" animated mesh info: ");
			Set<Entry<String, Collection<Pair<Double, Mesh>>>> ea = s.meshAnimations.entrySet();
			for (Entry<String, Collection<Pair<Double, Mesh>>> name : ea) {
				System.out.println("      " + ANSIColorUtils.red(name.getKey() + "") + " " + name.getValue().size() + " frames");
			}
		}
		System.out.println("root coordinate system animation info: ");
		Iterator<AnimatedCoordinateSystem> i = s.coordinateSystemAnimationRoots.iterator();
		while (i.hasNext()) {
			AnimatedCoordinateSystem marker = i.next();
			System.out.println("      coordinate system <" + ANSIColorUtils.red("" + marker.name) + "> is animated with <" + marker.animation.getNumSamples() + "> keyframes from <" + marker.animation.getDomainMin() + " -> " + marker.animation.getDomainMax() + ">");
		}

		Iterator<AnimatedMarker> i2 = s.animationRoots.iterator();
		while (i2.hasNext()) {
			AnimatedMarker marker = i2.next();
			System.out.println("      marker <" + marker.name + " is animated with <" + marker.animation.getNumSamples() + "> keyframes from <" + marker.animation.getDomainMin() + " -> " + marker.animation.getDomainMax() + ">");
			final List<String> names = new ArrayList<String>();
			new GraphNodeSearching.GraphNodeVisitor_depthFirst<AnimatedMarker>(false)
			{
				@Override
				protected VisitCode visit(AnimatedMarker n) {
					names.add(n.name);
					return VisitCode.cont;
				}

			}.apply(marker);
			names.remove(marker.name);
			System.out.println("             children are <"+names+">");
		}
		System.exit(0);

	}
}