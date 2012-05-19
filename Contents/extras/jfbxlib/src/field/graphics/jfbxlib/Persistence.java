package field.graphics.jfbxlib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.basic.AbstractBasicConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import field.graphics.jfbxlib.BuildMeshVisitor.Mesh;
import field.graphics.jfbxlib.BuildSkinningVisitor.SkinningInfo;
import field.graphics.jfbxlib.BuildTransformTreeVisitor.Transform;
import field.graphics.jfbxlib.CoordinateSystemAnimation.AnimatedCoordinateSystem;
import field.graphics.jfbxlib.MarkerAnimation.AnimatedMarker;
import field.math.graph.SimpleNode;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Quaternion;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Pair;
import field.util.HashMapOfLists;
import field.util.PythonUtils;

public class Persistence {

	static public class Storage implements Serializable {
		static public Storage load(Persistence p, File f) {
			if (f.getName().endsWith(".xml")) {
				try {
					ObjectInputStream ois = p.stream.createObjectInputStream(new BufferedReader(new FileReader(f), 1024 * 1024));
					Persistence.Storage ps = (Persistence.Storage) ois.readObject();
					ois.close();
					return ps;
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			} else {
				return (Storage) new PythonUtils().loadAsSerialization(f.getAbsolutePath());
			}
			return null;
		}

		public Map<Long, SimpleNode<Transform>> transforms = new HashMap<Long, SimpleNode<Transform>>();
		public Map<String, BuildMeshVisitor.Mesh> meshes = new HashMap<String, Mesh>();

		public HashMapOfLists<String, Pair<Double, BuildMeshVisitor.Mesh>> meshAnimations = new HashMapOfLists<String, Pair<Double, Mesh>>();

		public Map<String, BuildSkinningVisitor.SkinningInfo> skinningInfo = new HashMap<String, SkinningInfo>();

		public LinkedHashSet<AnimatedMarker> animationRoots = new LinkedHashSet<AnimatedMarker>();

		public LinkedHashSet<AnimatedCoordinateSystem> coordinateSystemAnimationRoots = new LinkedHashSet<AnimatedCoordinateSystem>();

		public double animationStart = 0;

		public double animationEnd = 0;

		public <T> Map<String, T> remapToNames(Map<Integer, T> input) {
			HashMap<String, T> ret = new HashMap<String, T>();
			Iterator<Entry<Integer, T>> i = input.entrySet().iterator();
			while (i.hasNext()) {
				Entry<Integer, T> e = i.next();
				Integer key = e.getKey();
				SimpleNode<Transform> n = transforms.get(key);
				if (n != null) {
					ret.put(n.payload().name, e.getValue());
				}
			}
			return ret;
		}

		public void save(Persistence p, File f) {

			if (f.getName().endsWith(".xml")) {

				try {
					ObjectOutputStream oos = p.stream.createObjectOutputStream(new BufferedWriter(new FileWriter(f), 1024 * 1024));
					oos.writeObject(this);
					oos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				new PythonUtils().persistAsSerialization(this, f.getAbsolutePath());
			}
		}

		@Override
		public String toString() {
			return "storage: " + transforms.size() + " transforms, " + meshes.size() + " meshes";
		}
	}

	protected XStream stream;

	public Persistence() {
		stream = new XStream();
		stream.setMode(XStream.ID_REFERENCES);

		stream.registerConverter(new AbstractBasicConverter() {
			@Override
			public boolean canConvert(Class type) {
				return Quaternion.class.isAssignableFrom(type);
			}

			@Override
			protected Object fromString(String str) {
				String[] s = str.split(",");
				assert s.length == 4 : Arrays.asList(s);
				Quaternion q = new Quaternion(Float.parseFloat(s[0]), Float.parseFloat(s[1]), Float.parseFloat(s[2]), Float.parseFloat(s[3]));
				return q;
			}

			@Override
			protected String toString(Object obj) {
				Quaternion q = (Quaternion) obj;
				return q.x + "," + q.y + "," + q.z + "," + q.w;
			}
		});

		stream.registerConverter(new AbstractBasicConverter() {
			@Override
			public boolean canConvert(Class type) {
				return Vector4.class.isAssignableFrom(type);
			}

			@Override
			protected Object fromString(String str) {
				String[] s = str.split(",");
				assert s.length == 4 : Arrays.asList(s);
				Vector4 q = new Vector4(Float.parseFloat(s[0]), Float.parseFloat(s[1]), Float.parseFloat(s[2]), Float.parseFloat(s[3]));
				return q;
			}

			@Override
			protected String toString(Object obj) {
				Vector4 q = (Vector4) obj;
				return q.x + "," + q.y + "," + q.z + "," + q.w;
			}
		});

		stream.registerConverter(new AbstractBasicConverter() {
			@Override
			public boolean canConvert(Class type) {
				return Vector3.class.isAssignableFrom(type);
			}

			@Override
			protected Object fromString(String str) {
				String[] s = str.split(",");
				assert s.length == 3 : Arrays.asList(s);
				Vector3 q = new Vector3(Float.parseFloat(s[0]), Float.parseFloat(s[1]), Float.parseFloat(s[2]));
				return q;
			}

			@Override
			protected String toString(Object obj) {
				Vector3 q = (Vector3) obj;
				return q.x + "," + q.y + "," + q.z;
			}
		});

		// actually, something special for CoordinateFrame, clears trs
		// and saves only them
		stream.registerConverter(new AbstractBasicConverter() {
			Quaternion qTmp = new Quaternion();

			Vector3 vTmp = new Vector3();

			Vector3 vTmp2 = new Vector3();

			@Override
			public boolean canConvert(Class type) {
				return CoordinateFrame.class.isAssignableFrom(type);
			}

			@Override
			protected Object fromString(String str) {
				String[] s = str.split("[,rts]");
				assert s.length == 10 : Arrays.asList(s) + " <" + str + ">";
				Quaternion r = new Quaternion(Float.parseFloat(s[0]), Float.parseFloat(s[1]), Float.parseFloat(s[2]), Float.parseFloat(s[3]));
				Vector3 q = new Vector3(Float.parseFloat(s[4]), Float.parseFloat(s[5]), Float.parseFloat(s[6]));
				Vector3 q2 = new Vector3(Float.parseFloat(s[7]), Float.parseFloat(s[8]), Float.parseFloat(s[9]));
				return new CoordinateFrame(r, q, q2);
			}

			@Override
			protected String toString(Object obj) {
				CoordinateFrame q = (CoordinateFrame) obj;
				q.getRotation(qTmp);
				q.getTranslation(vTmp);
				q.getScale(vTmp2);

				return qTmp.x + "," + qTmp.y + "," + qTmp.z + "," + qTmp.w + "t" + vTmp.x + "," + vTmp.y + "," + vTmp.z + "s" + vTmp2.x + "," + vTmp2.y + "," + vTmp2.z;

			}
		});

		// something special for Matrix4?

		stream.registerConverter(new Converter() {

			public boolean canConvert(Class type) {
				return type.isArray() && type.getComponentType().equals(float.class);
			}

			public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
				float[] f = (float[]) source;
				writer.startNode("length");
				writer.setValue("" + f.length);
				writer.endNode();
				writer.startNode("data");
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < f.length; i++) {
					sb.append(f[i] + " ");
				}
				writer.setValue(sb.toString());
				writer.endNode();
			}

			public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
				reader.moveDown();
				String original2 = reader.getValue();
				int len = Integer.parseInt(original2);
				reader.moveUp();
				reader.moveDown();
				String original = reader.getValue();
				String[] v = original.split(" ", 0);
				float[] f = new float[len];
				assert v.length == f.length || (v.length == 1 && f.length == 0) : v.length + " " + f.length + " <" + original2 + "> <" + original + ">";
				for (int i = 0; i < f.length; i++) {
					f[i] = Float.parseFloat(v[i]);
				}
				reader.moveUp();
				return f;
			}

		});

		stream.registerConverter(new Converter() {

			public boolean canConvert(Class type) {
				return type.isArray() && type.getComponentType().equals(short.class);
			}

			public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
				short[] f = (short[]) source;
				writer.startNode("length");
				writer.setValue("" + f.length);
				writer.endNode();
				writer.startNode("data");
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < f.length; i++) {
					sb.append(f[i] + " ");
				}
				writer.setValue(sb.toString());
				writer.endNode();
			}

			public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
				reader.moveDown();
				int len = Integer.parseInt(reader.getValue());
				reader.moveUp();
				reader.moveDown();
				String[] v = reader.getValue().split(" ");
				short[] f = new short[len];
				assert v.length == f.length || (v.length == 1 && f.length == 0) : v.length + " " + f.length;
				for (int i = 0; i < f.length; i++) {
					f[i] = Short.parseShort(v[i]);
				}
				reader.moveUp();
				return f;
			}

		});
	}

}
