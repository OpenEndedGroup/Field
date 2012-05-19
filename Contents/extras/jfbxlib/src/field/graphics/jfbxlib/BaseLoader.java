package field.graphics.jfbxlib;

import java.lang.reflect.InvocationTargetException;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import field.graphics.core.Base;
import field.graphics.core.BasicUtilities;
import field.graphics.jfbxlib.BuildMeshVisitor.Mesh;
import field.graphics.jfbxlib.BuildTransformTreeVisitor.Transform;
import field.math.abstraction.iInplaceProvider;
import field.math.graph.SimpleNode;
import field.math.linalg.iCoordinateFrame;


/**
 * tools for loading things from the fbx_xml file format
 */
public class BaseLoader {

	public <T extends iCoordinateFrame.iMutable> Map<String, T> createFramesForTransforms_flat(Persistence.Storage storage, Class<T> createClass) {
		if (storage.transforms == null) return new HashMap<String, T>();

		Iterator<Entry<Long, SimpleNode<Transform>>> i = storage.transforms.entrySet().iterator();
		Map<String, T> out = new HashMap<String, T>();

		while (i.hasNext()) {
			Entry<Long, SimpleNode<Transform>> e = i.next();
			long id = e.getKey();
			Transform t = e.getValue().payload();

			try {
				T mutable = createClass.newInstance();
				mutable.setRotation(t.worldRotation);
				mutable.setScale(t.worldScale);
				mutable.setTranslation(t.worldTranslation);

				out.put(t.name, mutable);
			} catch (InstantiationException e1) {
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			}
		}
		return out;
	}

	public <T extends Base.iLongGeometry> Map<String, T> createGeometryForMeshes(Map<String, iInplaceProvider<iCoordinateFrame.iMutable>> toTransforms, Persistence.Storage storage, Class<T> createClass, boolean doAux) {
		if (storage.meshes== null) return new HashMap<String, T>();

		Map<String, T> out = new HashMap<String, T>();

		Iterator<Entry<String, Mesh>> i = storage.meshes.entrySet().iterator();
		while (i.hasNext()) {
			Entry<String, Mesh> e = i.next();
			String id = e.getKey();
			Mesh mesh = e.getValue();

			iInplaceProvider<iCoordinateFrame.iMutable> cf = null;
			if (toTransforms != null) {
				cf = toTransforms.get(id);
			}
			if (cf == null) cf = new BasicUtilities.Position();

			try {
				T geometry = createClass.getDeclaredConstructor(new Class[] { iInplaceProvider.class}).newInstance(cf);

				geometry.rebuildVertex(mesh.numVertex).rebuildTriangle(mesh.numTriangle);

				assert geometry.vertex().capacity() == mesh.vertexArray.length;
				geometry.vertex().put(mesh.vertexArray);
				assert geometry.triangle().capacity() == mesh.triangleArray.length;
				geometry.longTriangle().put(mesh.triangleArray);


				if (doAux)
				for (int j = 0; j < mesh.extraInfo.length; j++)
					if (mesh.extraInfo[j] != null) geometry.aux(j, mesh.extraInfo[j].length / mesh.numVertex).put(mesh.extraInfo[j]);

				out.put(id, geometry);
			} catch (InstantiationException e1) {
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			} catch (IllegalArgumentException e1) {
				e1.printStackTrace();
			} catch (SecurityException e1) {
				e1.printStackTrace();
			} catch (InvocationTargetException e1) {
				e1.printStackTrace();
			} catch (NoSuchMethodException e1) {
				e1.printStackTrace();
			}
		}
		return out;
	}
}
