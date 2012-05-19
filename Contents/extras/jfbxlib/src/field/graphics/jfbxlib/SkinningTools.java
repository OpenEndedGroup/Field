package field.graphics.jfbxlib;

import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import field.graphics.core.Base;
import field.graphics.core.Base.iGeometry;
import field.graphics.core.Base.iLongGeometry;
import field.graphics.core.BasicGeometry.BasicMesh;
import field.graphics.core.BasicGeometry.TriangleMesh_long;
import field.graphics.dynamic.DynamicLine;
import field.graphics.dynamic.DynamicPointlist;
import field.graphics.dynamic.LineIdentifier;
import field.graphics.dynamic.iLineOutput;
import field.graphics.jfbxlib.BuildMeshVisitor.Mesh;
import field.graphics.jfbxlib.BuildSkinningVisitor.BindPoseDescription;
import field.graphics.jfbxlib.BuildSkinningVisitor.SkinningInfo;
import field.graphics.jfbxlib.CoordinateSystemAnimation.AnimatedCoordinateSystem;
import field.graphics.jfbxlib.HierarchyOfCoordinateFrames.Element;
import field.graphics.jfbxlib.Persistence.Storage;
import field.launch.iUpdateable;
import field.math.abstraction.iInplaceProvider;
import field.math.graph.GraphNodeSearching;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.graph.TopologySearching;
import field.math.graph.iTopology;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Vector3;
import field.math.linalg.iCoordinateFrame;
import field.namespace.generic.Generics.Pair;
import field.util.TaskQueue;

public class SkinningTools implements iUpdateable {

	private final Storage s;

	private Map<String, CoordinateFrame> frames;

	private Map<String, ? extends iGeometry> meshes;

	private Map<String, ? extends iGeometry> bindMeshes;

	private final Map<String, SlowSkinner> skinners = new LinkedHashMap<String, SlowSkinner>();

	TaskQueue q = new TaskQueue();

	public SkinningTools(Storage s) {
		this.s = s;
	}

	public void applyVertexAnimations0(float time) {

		for (Map.Entry<String, ? extends iGeometry> e : meshes.entrySet()) {
			String name = e.getKey();
			Collection<Pair<Double, Mesh>> m = s.meshAnimations.get(name);
			if (m.size() > 1) {
				int at = (int) (m.size() * (time - s.animationStart) / (s.animationEnd - s.animationStart + 1e-5));

				at = Math.min(m.size() - 1, Math.max(0, at));
				Pair<Double, Mesh> p = ((List<Pair<Double, Mesh>>) m).get(at);
				e.getValue().vertex().put(p.right.vertexArray);
			}
		}
	}

	public void applyVertexAnimations1(double time) {

		if (s.meshAnimations == null)
			return;
		if (s.meshAnimations.size() == 0)
			return;

		for (Map.Entry<String, ? extends iGeometry> e : meshes.entrySet()) {
			String name = e.getKey();
			Collection<Pair<Double, Mesh>> m = s.meshAnimations.get(name);
			if (m.size() > 1) {
				double atr = m.size() * (time - s.animationStart) / (s.animationEnd - s.animationStart + 1e-5);
				int at = (int) (atr);

				at = Math.min(m.size() - 1, Math.max(0, at));

				float alpha = (float) (atr - at);
				int at1 = Math.min(m.size() - 1, Math.max(0, at + 1));

				Pair<Double, Mesh> p1 = ((List<Pair<Double, Mesh>>) m).get(at);
				Pair<Double, Mesh> p2 = ((List<Pair<Double, Mesh>>) m).get(at1);
				FloatBuffer v = e.getValue().vertex();

				System.out.println(" at  " + at + " " + alpha);

				for (int i = 0; i < p1.right.vertexArray.length; i++) {
					v.put(p1.right.vertexArray[i] * (1 - alpha) + alpha * p2.right.vertexArray[i]);
				}
			}
		}
	}

	public Map<String, TriangleMesh_long> constructAllMeshes(Map<String, iInplaceProvider<CoordinateFrame>> localMap) {
		return constructAllMeshes(localMap, TriangleMesh_long.class, true);
	}

	public <A extends iLongGeometry> Map<String, A> constructAllMeshes(Map<String, iInplaceProvider<CoordinateFrame>> localMap, Class<A> geometryClazz) {
		return constructAllMeshes(localMap, geometryClazz, true);
	}

	// public <A extends iGeometry> Map<String, A>
	// constructAllMeshesWithProjection(Map<String,
	// iInplaceProvider<CoordinateFrame>> localMap,
	// HierarchyOfCoordinateFrames h, Class<A> geometryClazz) {
	// BaseLoader loader = new BaseLoader();
	//
	// frames = loader.createFramesForTransforms_flat(s,
	// CoordinateFrame.class);
	// meshes = loader.createGeometryForMeshes(frames, s, geometryClazz,
	// true);
	// // we need the
	// // bind mesh, we
	// // pull this
	// // from the same
	// // file, because
	// // the meshes
	// // are always
	// // pre skinning
	// bindMeshes = loader.createGeometryForMeshes(frames, s,
	// BasicGeometry.TriangleMesh.class, true);
	//
	// for (Map.Entry<String, SkinningInfo> e : s.skinningInfo.entrySet()) {
	// final SlowSkinner skinner = new SlowSkinner(localMap, e.getValue());
	// final iGeometry bindMesh = bindMeshes.get(e.getKey());
	// final iGeometry destMesh = meshes.get(e.getKey());
	//
	// ProjectToSkeleton pts = new ProjectToSkeleton(bindMesh, h,
	// e.getValue());
	// this.goToBindPose(h);
	// FloatBuffer tb = pts.iterativeProjectAndShrink();
	//
	// // overwrite
	// // bind
	// // post
	// bindMesh.vertex().put(tb);
	//
	// assert bindMesh != null;
	// assert destMesh != null;
	//
	// q.new Updateable(new iUpdateable() {
	// public void update() {
	// skinner.performSkinning(bindMesh, destMesh);
	// }
	// });
	// }
	//
	// return (Map<String, A>) meshes;
	// }

	public <A extends iLongGeometry> Map<String, A> constructAllMeshes(Map<String, iInplaceProvider<CoordinateFrame>> localMap, Class<A> geometryClazz, boolean doAux) {
		BaseLoader loader = new BaseLoader();

		frames = loader.createFramesForTransforms_flat(s, CoordinateFrame.class);
		meshes = loader.createGeometryForMeshes((Map) localMap, s, geometryClazz, doAux);
		// we need the
		// bind mesh, we
		// pull this
		// from the same
		// file, because
		// the meshes
		// are always
		// pre skinning
		bindMeshes = loader.createGeometryForMeshes((Map)frames, s, geometryClazz, true);

		if (s.skinningInfo != null)
			for (Map.Entry<String, SkinningInfo> e : s.skinningInfo.entrySet()) {
				System.out.println(" constructing skins <" + e.getValue() + ">");
				final SlowSkinner skinner = new SlowSkinner(localMap, e.getValue());
				final iGeometry bindMesh = bindMeshes.get(e.getKey());
				final iGeometry destMesh = meshes.get(e.getKey());

				skinners.put(e.getKey(), skinner);

				assert bindMesh != null;
				assert destMesh != null;

				q.new Updateable(new iUpdateable() {
					public void update() {
						if (((BasicMesh) destMesh).isOn())
							skinner.performSkinning(bindMesh, destMesh);
					}
				});
			}

		return (Map<String, A>) meshes;
	}

	

	public Map<String, ? extends iGeometry> getBindMeshes() {
		return bindMeshes;
	}

	public Map<String, SlowSkinner> getSkinners() {
		return skinners;
	}

	public void drawCurrentSkeleton(final iLineOutput line, final HierarchyOfCoordinateFrames hierarchy, final float minDistance) {
		line.open();
		Iterator<AnimatedCoordinateSystem> i = s.coordinateSystemAnimationRoots.iterator();
		while (i.hasNext()) {

			new GraphNodeSearching.GraphNodeVisitor_depthFirst<CoordinateSystemAnimation.AnimatedCoordinateSystem>(false) {
				CoordinateFrame outFrame = new CoordinateFrame();

				Vector3 out = new Vector3();

				Vector3 out2 = new Vector3();

				@Override
				protected VisitCode visit(CoordinateSystemAnimation.AnimatedCoordinateSystem n) {
					Element e = hierarchy.getNamed(n.name);
					assert e != null : n.name;

					e.getWorld(outFrame);

					outFrame.getTranslation(out);

					if (e.parent != null) {
						e.parent.getWorld(outFrame);
						outFrame.getTranslation(out2);
						if (out.distanceFrom(out2) > minDistance) {
							line.beginSpline(new LineIdentifier(e.parent.name + " " + e.name));
							line.moveTo(new Vector3(out));
							line.lineTo(new Vector3(out2));
							line.endSpline();
						}
					}

					return VisitCode.cont;
				}

			}.apply(i.next());
		}
		line.close();
	}

	public void drawCurrentSkeleton2(final iLineOutput line, Element root) {
		line.open();
		new TopologySearching.TopologyVisitory_depthFirst<Element>(false, new iTopology<Element>() {

			public List<Element> getParentsOf(Element of) {
				return Collections.singletonList(of.parent);
			}

			public List<Element> getChildrenOf(Element of) {
				return of.children;
			}
		}) {

			@Override
			protected VisitCode visit(Element e) {
				if (e.parent != null) {
					line.beginSpline(new LineIdentifier(e.parent.name + " " + e.name));
					line.moveTo(e.get(null).getTranslation(null));
					line.lineTo(e.parent.get(null).getTranslation(null));
					line.endSpline();
				}
				return VisitCode.cont;
			}

		}.apply(root);
		line.close();
	}

	public void drawCurrentSkeleton2(final iLineOutput line, Element root, final float scale) {
		line.open();

		final Vector3 rootAt = root.get(null).getTranslation(null);

		new TopologySearching.TopologyVisitory_depthFirst<Element>(false, new iTopology<Element>() {

			public List<Element> getParentsOf(Element of) {
				return Collections.singletonList(of.parent);
			}

			public List<Element> getChildrenOf(Element of) {
				return of.children;
			}
		}) {

			@Override
			protected VisitCode visit(Element e) {
				if (e.parent != null) {
					line.beginSpline(new LineIdentifier(e.parent.name + " " + e.name));
					Vector3 a = e.get(null).getTranslation(null);
					a.sub(rootAt).scale(scale).add(rootAt);
					line.moveTo(a);
					Vector3 b = e.parent.get(null).getTranslation(null);
					b.sub(rootAt).scale(scale).add(rootAt);
					line.lineTo(b);
					line.endSpline();
				}
				return VisitCode.cont;
			}

		}.apply(root);
		line.close();
	}

	public Map<String, ? extends iGeometry> getMeshes() {
		return meshes;
	}

	public void goToBindPose(final HierarchyOfCoordinateFrames hierarchy) {

		final Map<Element, CoordinateFrame> map = new HashMap<Element, CoordinateFrame>();

		for (Map.Entry<String, SkinningInfo> e : s.skinningInfo.entrySet()) {

			int n = 0;
			for (BindPoseDescription bpd : e.getValue().bindPoseDescriptions) {

				Element q = hierarchy.getNamed(e.getValue().boneNames[n]);
				if (q != null) {
					CoordinateFrame cf = new CoordinateFrame(bpd.rotation, bpd.translation, bpd.scale);

					cf.invert();

					map.put(q, cf);
				} else {
					System.out.println("warning: no <" + e.getValue().boneNames[n] + ">");
				}
				n++;
			}
		}

		Iterator<Element> i = hierarchy.getRoots().iterator();
		while (i.hasNext())
			new TopologySearching.TopologyVisitory_depthFirst<Element>(false, hierarchy.getTopology()) {

				@Override
				protected VisitCode visit(Element n) {

					if (n.parent != null) {
						CoordinateFrame worldForChild = map.get(n);

						if (worldForChild != null) {
							CoordinateFrame parentFrame = n.parent.get(new CoordinateFrame());

							parentFrame.invert();
							CoordinateFrame out = parentFrame.multiply(parentFrame, worldForChild);
							n.setLocal(out);

						}
					} else {
						CoordinateFrame worldForChild = map.get(n);

						if (worldForChild != null) {
							n.setLocal(worldForChild);
						}
					}
					return VisitCode.cont;
				}

			}.apply(i.next());

	}

	public void showCurrentSkeleton(final DynamicPointlist point, final DynamicLine line, final HierarchyOfCoordinateFrames hierarchy) {
		point.open();
		line.open();
		Iterator<AnimatedCoordinateSystem> i = s.coordinateSystemAnimationRoots.iterator();
		while (i.hasNext()) {

			new GraphNodeSearching.GraphNodeVisitor_depthFirst<CoordinateSystemAnimation.AnimatedCoordinateSystem>(false) {
				CoordinateFrame outFrame = new CoordinateFrame();

				Vector3 out = new Vector3();

				Vector3 out2 = new Vector3();

				@Override
				protected VisitCode visit(CoordinateSystemAnimation.AnimatedCoordinateSystem n) {
					Element e = hierarchy.getNamed(n.name);
					assert e != null : n.name;

					e.getWorld(outFrame);
					outFrame.getTranslation(out);

					if (point != null)
						point.nextVertex(out);

					if (e.parent != null) {
						e.parent.getWorld(outFrame);
						outFrame.getTranslation(out2);
						int v = line.nextEdge(out2, out);
						int[] vv = line.vertexForEdge(v);
						line.setAux(vv[0], Base.color0_id, 1, 1, 1, 1);
						line.setAux(vv[1], Base.color0_id, 1, 1, 1, 1);
					}

					return VisitCode.cont;
				}

			}.apply(i.next());
		}
		line.close();
		point.close();
	}
	
	public void showCurrentSkeletonAndAxes(final DynamicLine line, final HierarchyOfCoordinateFrames hierarchy, final float scale) {
		line.open();
		Iterator<AnimatedCoordinateSystem> i = s.coordinateSystemAnimationRoots.iterator();
		while (i.hasNext()) {

			new GraphNodeSearching.GraphNodeVisitor_depthFirst<CoordinateSystemAnimation.AnimatedCoordinateSystem>(false) {
				CoordinateFrame outFrame = new CoordinateFrame();

				Vector3 out = new Vector3();
				Vector3 out2 = new Vector3();

				@Override
				protected VisitCode visit(CoordinateSystemAnimation.AnimatedCoordinateSystem n) {
					Element e = hierarchy.getNamed(n.name);
					assert e != null : n.name;

					e.getWorld(outFrame);
					outFrame.getTranslation(out);

					
					
					int c1 = line.nextVertex(out);
					int c2 = line.nextVertex(out);
					int c3 = line.nextVertex(out);
					
					Vector3 x = outFrame.transformDirection(new Vector3(1,0,0));
					int cx = line.nextVertex(new Vector3(out).add(x, scale));
					Vector3 y = outFrame.transformDirection(new Vector3(0,1,0));
					int cy = line.nextVertex(new Vector3(out).add(y, scale));
					Vector3 z = outFrame.transformDirection(new Vector3(0,0,1));
					int cz = line.nextVertex(new Vector3(out).add(z, scale));
					
					line.setAux(c1, Base.color0_id, 1, 0, 0, 0.7f); 
					line.setAux(cx, Base.color0_id, 1, 0, 0, 0.7f); 
					line.setAux(c2, Base.color0_id, 0, 1, 0, 0.7f); 
					line.setAux(cy, Base.color0_id, 0, 1, 0, 0.7f); 
					line.setAux(c3, Base.color0_id, 0, 0, 1, 0.7f); 
					line.setAux(cz, Base.color0_id, 0, 0, 1, 0.7f); 
					
					line.nextFace(c1, cx);
					line.nextFace(c2, cy);
					line.nextFace(c3, cz);

					if (e.parent != null) {
						e.parent.getWorld(outFrame);
						outFrame.getTranslation(out2);
						int v = line.nextEdge(out2, out);
						int[] vv = line.vertexForEdge(v);
						line.setAux(vv[0], Base.color0_id, 1, 1, 1, 0.4f);
						line.setAux(vv[1], Base.color0_id, 1, 1, 1, 0.4f);
					}
					return VisitCode.cont;
				}

			}.apply(i.next());
		}
		line.close();
	}
	
	

	public void update() {
		q.update();
	}
}
