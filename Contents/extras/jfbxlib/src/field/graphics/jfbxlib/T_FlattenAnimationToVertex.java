package field.graphics.jfbxlib;

import java.io.Serializable;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import field.graphics.core.BasicGeometry.TriangleMesh;
import field.launch.SystemProperties;
import field.launch.iLaunchable;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Vector3;
import field.math.util.CubicInterpolatorDynamic;
import field.util.PythonUtils;

/**
 * plays back an animation and captures all of the mesh information you get one
 * cubicinterpolator<Vector3> per vertex
 * 
 * @author marc
 * 
 */
public class T_FlattenAnimationToVertex implements iLaunchable {

	static public class Animation implements Serializable{
		private static final long serialVersionUID = 8818124864378818618L;
		public List<CubicInterpolatorDynamic<Vector3>> animations = new ArrayList<CubicInterpolatorDynamic<Vector3>>();
	}

	static public class RawMeshAnimations implements Serializable{
		
		private static final long serialVersionUID = -6659408156789260776L;
		public float start;
		public float end;
		public int numFrames;
		public HashMap<String, Animation> raw = new HashMap<String, Animation>();
	}

	public void launch() {

		Loader2 loaded = new Loader2(SystemProperties.getProperty("in"));
		float start = loaded.getAnimationStart();
		float end = loaded.getAnimationEnd();

		int numFrames = SystemProperties.getIntProperty("numFrames", 50);

		RawMeshAnimations ret = new RawMeshAnimations();


		ret.numFrames = numFrames;
		ret.start = start;
		ret.end = end;
		
		for (int i = 0; i < numFrames; i++) {
			float time = start + (end - start) * i / (numFrames - 1f);

			;//;//System.out.println(" ------------- <"+time+"> ------------------");
			
			loaded.applyAnimation(time);
			loaded.applyVertexAnimations(time);
			loaded.updateSkinning();

			Map<String, TriangleMesh> m = (Map<String, TriangleMesh>) loaded.getMeshes();
			for (Map.Entry<String, TriangleMesh> e : m.entrySet()) {
				Animation a = ret.raw.get(e.getKey());
				if (a == null) {
					ret.raw.put(e.getKey(), a = create(e.getValue()));
				}

				FloatBuffer vv = e.getValue().vertex();
				for (int j = 0; j < e.getValue().numVertex(); j++) {
					a.animations.get(j).new Sample(e.getValue().getCoordinateProvider().get(new CoordinateFrame()).transformPosition(new Vector3(vv)), time);
				}
			}
		}

		;//;//System.out.println(" - --- finishing ");
		
		;//;//System.out.println(" persisting ....");
		new PythonUtils().persistAsSerialization(ret, SystemProperties.getProperty("in") + ".rawMesh");
		;//;//System.out.println(" complete");
		System.exit(0);

	}

	private Animation create(TriangleMesh value) {
		Animation a = new Animation();
		for (int i = 0; i < value.numVertex(); i++) {
			a.animations.add(new CubicInterpolatorDynamic<Vector3>());
		}
		return a;
	}

}
