package field.graphics.jfbxlib;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import field.graphics.core.Base.iGeometry;
import field.graphics.core.BasicGeometry.BasicMesh;
import field.graphics.core.BasicGeometry.TriangleMesh_long;
import field.graphics.jfbxlib.CoordinateSystemAnimation.AnimatedCoordinateSystem;
import field.graphics.jfbxlib.HierarchyOfCoordinateFrames.Element;
import field.graphics.jfbxlib.Persistence.Storage;
import field.launch.SystemProperties;
import field.math.abstraction.iFilter;
import field.math.abstraction.iInplaceProvider;
import field.math.graph.GraphNodeSearching;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.linalg.CoordinateFrame;
import field.namespace.generic.Bind.iFunction;

public class Loader2 {

	public Storage source;
	public SkinningTools skinning;
	private Map<String, ? extends BasicMesh> meshes;
	public HierarchyOfCoordinateFrames hierarchy;

	static public boolean automaticallyCache = false;
	
	public Loader2(String filename) {
		this(filename, 0);
	}

	public Loader2(String filename, int take) {

		System.out.println(" loading ....");

		SystemProperties.setProperty("take", take + "");

		if (filename.endsWith(".fbx")) {
			
			System.out.println(" cache ? "+new File(filename+".cached.objects").exists());
			
			if (automaticallyCache && new File(filename+".cached.objects").exists())
			{
				long m1 = new File(filename).lastModified();
				long m2 = new File(filename+".cached.objects").lastModified();
				if (m2>=m1)
				{
					boolean loaded = false;
					try{
						
						Persistence p = new Persistence();
						source = Persistence.Storage.load(p, new File(filename+".cached.objects"));

						hierarchy = new HierarchyOfCoordinateFrames();

						skinning = new SkinningTools(source);
						meshes = new LinkedHashMap<String, BasicMesh>(skinning.constructAllMeshes(getTransformMap(), TriangleMesh_long.class, true));

						applyAnimation(0);
						
						loaded = true;
					}
					catch(Throwable t)
					{
						System.out.println(" benign error while loading cached objects file, rebuilding cache "+t);
					}
					if (loaded)
						return;
				}
					
			}
			
			source = new T_ConvertFBXInplace().convert(filename);

			hierarchy = new HierarchyOfCoordinateFrames();

			skinning = new SkinningTools(source);
			meshes = new LinkedHashMap<String, BasicMesh>(skinning.constructAllMeshes(getTransformMap(), TriangleMesh_long.class, true));

			applyAnimation(0);
			
			if (automaticallyCache)
			{
				source.save(new Persistence(), new File(filename+".cached.objects"));
			}
			

		} else {

			Persistence p = new Persistence();
			source = Persistence.Storage.load(p, new File(filename));

			hierarchy = new HierarchyOfCoordinateFrames();

			skinning = new SkinningTools(source);
			meshes = new LinkedHashMap<String, BasicMesh>(skinning.constructAllMeshes(getTransformMap(), TriangleMesh_long.class, true));

			applyAnimation(0);
		}

		System.out.println(" loaded ....");
	}

	public Map<String, ? extends BasicMesh> getMeshes() {
		return meshes;
	}

	public Map<String, ? extends iGeometry> getBindMeshes() {
		return skinning.getBindMeshes();
	}

	public Map<String, SlowSkinner> getSkinners() {
		return skinning.getSkinners();
	}

	public LinkedHashSet<AnimatedCoordinateSystem> getAnimationRoots() {
		return source.coordinateSystemAnimationRoots;
	}

	public LinkedHashMap<String, Element> getTransformRoots() {

		Set<Element> e = hierarchy.getRoots();
		LinkedHashMap<String, Element> r = new LinkedHashMap<String, Element>();
		for (Element ee : e)
			r.put(ee.name, ee);

		return r;
	}

	public Map<String, iInplaceProvider<CoordinateFrame>> getTransformMap() {
		Collection<AnimatedCoordinateSystem> coordinateSystemAnimationRoots = source.coordinateSystemAnimationRoots;
		final Map<String, iInplaceProvider<CoordinateFrame>> localMap = new HashMap<String, iInplaceProvider<CoordinateFrame>>();

		Iterator<AnimatedCoordinateSystem> i = coordinateSystemAnimationRoots.iterator();
		while (i.hasNext()) {
			new GraphNodeSearching.GraphNodeVisitor_depthFirst<CoordinateSystemAnimation.AnimatedCoordinateSystem>(false) {

				CoordinateFrame out = new CoordinateFrame();

				@Override
				protected VisitCode visit(CoordinateSystemAnimation.AnimatedCoordinateSystem n) {

					Element e = null;
					if (this.stack.size() > 1) {
						e = hierarchy.getChildOf(hierarchy.getNamed(this.stack.get(this.stack.size() - 2).name), n.name);
					} else {
						e = hierarchy.getRoot(n.name);
					}

					localMap.put(n.name, e);
					return VisitCode.cont;
				}

			}.apply(i.next());
		}

		return localMap;
	}

	public Map<String, iInplaceProvider<CoordinateFrame>> applyAnimation(final double t) {
		LinkedHashSet<AnimatedCoordinateSystem> coordinateSystemAnimationRoots = source.coordinateSystemAnimationRoots;
		Iterator<AnimatedCoordinateSystem> i = coordinateSystemAnimationRoots.iterator();

		final Map<String, iInplaceProvider<CoordinateFrame>> localMap = new HashMap<String, iInplaceProvider<CoordinateFrame>>();

		while (i.hasNext()) {

			new GraphNodeSearching.GraphNodeVisitor_depthFirst<CoordinateSystemAnimation.AnimatedCoordinateSystem>(false) {

				CoordinateFrame out = new CoordinateFrame();

				@Override
				protected VisitCode visit(CoordinateSystemAnimation.AnimatedCoordinateSystem n) {

					Element e = null;
					if (this.stack.size() > 1) {
						e = hierarchy.getChildOf(hierarchy.getNamed(this.stack.get(this.stack.size() - 2).name), n.name);
					} else {
						e = hierarchy.getRoot(n.name);
					}

					n.animation.getValue((float) t, out);

					e.setLocal(out);
					localMap.put(n.name, e);
					return VisitCode.cont;
				}

			}.apply(i.next());
		}

		return localMap;
	}

	public Map<String, iInplaceProvider<CoordinateFrame>> applyAnimation(final double t, Loader2 source, final iFunction<String, String> nameRemapper) {
		LinkedHashSet<AnimatedCoordinateSystem> coordinateSystemAnimationRoots = source.source.coordinateSystemAnimationRoots;
		Iterator<AnimatedCoordinateSystem> i = coordinateSystemAnimationRoots.iterator();

		final Map<String, iInplaceProvider<CoordinateFrame>> localMap = new HashMap<String, iInplaceProvider<CoordinateFrame>>();

		while (i.hasNext()) {

			new GraphNodeSearching.GraphNodeVisitor_depthFirst<CoordinateSystemAnimation.AnimatedCoordinateSystem>(false) {

				CoordinateFrame out = new CoordinateFrame();

				@Override
				protected VisitCode visit(CoordinateSystemAnimation.AnimatedCoordinateSystem n) {

					try {

						Element e = null;
						if (this.stack.size() > 1) {
							e = hierarchy.getChildOf(hierarchy.getNamed(nameRemapper.f(this.stack.get(this.stack.size() - 2).name)), nameRemapper.f(n.name));
						} else {
							e = hierarchy.getRoot(nameRemapper.f(n.name));
						}

						n.animation.getValue((float) t, out);

						e.setLocal(out);
						localMap.put(n.name, e);
					} catch (NullPointerException e) {
						System.err.println(" missing transform ? " + n.name);
					}
					return VisitCode.cont;
				}

			}.apply(i.next());
		}

		return localMap;
	}

	static public class NamedCoordinateFrame {
		public String name;
		public CoordinateFrame f;
		public Element e;

		public NamedCoordinateFrame(String name, CoordinateFrame f) {
			super();
			this.name = name;
			this.f = f;
		}

	}

	public Map<String, iInplaceProvider<CoordinateFrame>> applyAnimation(final double t, Collection<String> names, final iFilter<NamedCoordinateFrame, NamedCoordinateFrame> m) {
		LinkedHashSet<AnimatedCoordinateSystem> coordinateSystemAnimationRoots = source.coordinateSystemAnimationRoots;
		Iterator<AnimatedCoordinateSystem> i = coordinateSystemAnimationRoots.iterator();

		final Map<String, iInplaceProvider<CoordinateFrame>> localMap = new HashMap<String, iInplaceProvider<CoordinateFrame>>();

		while (i.hasNext()) {

			AnimatedCoordinateSystem n = i.next();

			if (names == null || names.contains(n.name))
				new GraphNodeSearching.GraphNodeVisitor_depthFirst<CoordinateSystemAnimation.AnimatedCoordinateSystem>(false) {

					CoordinateFrame out = new CoordinateFrame();

					@Override
					protected VisitCode visit(CoordinateSystemAnimation.AnimatedCoordinateSystem n) {

						Element e = null;
						if (this.stack.size() > 1) {
							e = hierarchy.getChildOf(hierarchy.getNamed(this.stack.get(this.stack.size() - 2).name), n.name);
						} else {
							e = hierarchy.getRoot(n.name);
						}

						n.animation.getValue((float) t, out);
						e.setLocal(out);

						NamedCoordinateFrame nn = new NamedCoordinateFrame(n.name, out);
						nn.e = e;
						NamedCoordinateFrame o = m.filter(nn);

						if (o == null) {
							return VisitCode.skip;
						}

						e.setLocal(o.f);
						localMap.put(n.name, e);
						return VisitCode.cont;
					}

				}.apply(n);
		}

		return localMap;
	}

	public float getAnimationStart() {
		return (float) source.animationStart;
	}

	public float getAnimationEnd() {
		return (float) source.animationEnd;
	}

	public void updateSkinning() {
		skinning.update();
	}

	public void applyVertexAnimations(double t) {
		skinning.applyVertexAnimations1(t);
	}

	@Override
	public String toString() {
		return "fbxLoader with " + getTransformMap().size() + " transforms, " + getMeshes().size() + " meshes, " + getSkinners().size() + " skinners";
	}


	public void sortMeshes(Comparator<Map.Entry<String, BasicMesh>> m)
	{
		Set<Map.Entry<String, BasicMesh>> entrySet = new LinkedHashMap<String, BasicMesh>(meshes).entrySet();
		
		ArrayList<Entry<String, BasicMesh>> al = new ArrayList<Map.Entry<String, BasicMesh>>(entrySet);
		
		Collections.sort(al, m);
		
		LinkedHashMap<String, BasicMesh> meshes = new LinkedHashMap<String, BasicMesh>();
		
		for(Entry<String, BasicMesh> e : al)
		{
			meshes.put(e.getKey(), e.getValue());
		}
		
		this.meshes = meshes;
	}
	

	public void reverseAnimations(AnimatedCoordinateSystem s)
	{
		
		final float start = getAnimationStart();
		final float end = getAnimationEnd();
		
		s.animation.remapTime(new iFunction<Number, Number>(){

			public Number f(Number in) {
				
				return new Float(end-(in.floatValue()-start));
				
			}});
	}
	
}
