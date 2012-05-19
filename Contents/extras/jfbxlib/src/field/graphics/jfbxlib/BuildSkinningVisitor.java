package field.graphics.jfbxlib;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import field.math.linalg.Quaternion;
import field.math.linalg.Vector3;
import field.util.ANSIColorUtils;


public class BuildSkinningVisitor extends AbstractVisitor {

	private int numVertex;

	Map<Long, SkinningInfo> infos = new HashMap<Long, SkinningInfo>();
	Map<String, SkinningInfo> namedInfos = new HashMap<String, SkinningInfo>();

	public BuildSkinningVisitor() {
	}

	public BuildSkinningVisitor(JFBXVisitor delegate) {
		this.delegate = delegate;
	}

	public Map<String, SkinningInfo> getSkinningInfos() {
		return namedInfos;
	}

	HashMap<Integer, Float>[] tempStorage;
	HashMap<Integer, Long> boneDefinitions;
	HashMap<Integer, BindPoseDescription> boneBindPoses;

	private SkinningInfo currentSkinning;

	private long currentUID;

	private String currentName;

	@Override
	public void visitTransformBegin(String name, int type, long uid) {
		super.visitTransformBegin(name, type, uid);
		currentUID = uid;
		currentName = name;
	}

	@Override
	public void visitMeshBegin(int numVertex, int numPolygon) {
		super.visitMeshBegin(numVertex, numPolygon);

		this.numVertex = numVertex;
	}

	@Override	
	public void visitMeshSkinDeformBegin() {
		super.visitMeshSkinDeformBegin();
		
		System.out.println(" skin deform begin ");
		
		currentSkinning = new SkinningInfo();
		tempStorage = new HashMap[numVertex];
		boneDefinitions = new HashMap<Integer, Long>();
		boneBindPoses = new HashMap<Integer, BindPoseDescription>();
	}

	@Override
	public void visitMeshSkinDeformDefineBone(int boneNumber, long isUiq, float oq0, float oq1, float oq2, float oq3, float ot0, float ot1, float ot2, float os0, float os1, float os2, float gq0, float gq1, float gq2, float gq3, float gt0, float gt1, float gt2, float gs0, float gs1, float gs2) {
		super.visitMeshSkinDeformDefineBone(boneNumber, isUiq, oq0, oq1, oq2, oq3, ot0, ot1, ot2, os0, os1, os2, gq0, gq1, gq2, gq3, gt0, gt1, gt2, gs0, gs1, gs2);
		boneDefinitions.put(boneNumber, isUiq);
		
		BindPoseDescription desc = new BindPoseDescription();
		desc.translation = new Vector3(ot0,ot1,ot2);
		desc.rotation= new Quaternion(oq0,oq1,oq2,oq3);
		desc.scale= new Vector3(os0,os1,os2);

		desc.geometryTranslation = new Vector3(gt0,gt1,gt2);
		desc.geometryRotation= new Quaternion(gq0,gq1,gq2,gq3);
		desc.geometryScale= new Vector3(gs0,gs1,gs2);
		
		
		System.out.println(" defined bone <"+desc+">");
		
		boneBindPoses.put(boneNumber, desc);
	}

	@Override
	public void visitMeshSkinDeformSetInfluence(int boneNumber, int vertexIndex, float isWeight) {
		System.out.println(" bone number <"+boneNumber+"> vertex index <"+vertexIndex+">");
		super.visitMeshSkinDeformSetInfluence(boneNumber, vertexIndex, isWeight);
		HashMap<Integer, Float> map = tempStorage[vertexIndex];
		if (map == null) {
			map = tempStorage[vertexIndex] = new HashMap<Integer, Float>(5);
		}
		//assert !map.containsKey(boneNumber) : map + " " + boneNumber;
		if (map.containsKey(boneNumber)) System.err.println(ANSIColorUtils.red("funk?: multiply defined bone <"+boneNumber+"> for vertex <"+vertexIndex+"> weight now <"+isWeight+"> weight was <"+map.get(boneNumber)+">"));
		map.put(boneNumber, isWeight);
	}

	@Override
	public void visitMeshEnd() {
		super.visitMeshEnd();
		System.out.println(" skin deform end ");

		if (currentSkinning != null) {
			System.out.println(" (read skin) ");
			Iterator<Entry<Integer, Long>> i = boneDefinitions.entrySet().iterator();
			int max = 0;
			while (i.hasNext()) {
				Entry<Integer, Long> e = i.next();
				if (e.getKey() > max) max = e.getKey();
			}
			currentSkinning.boneReferences = new long[max+1];
			currentSkinning.bindPoseDescriptions = new BindPoseDescription[max+1];
			i = boneDefinitions.entrySet().iterator();
			while (i.hasNext()) {
				Entry<Integer, Long> e = i.next();
				currentSkinning.boneReferences[e.getKey()] = e.getValue();
				currentSkinning.bindPoseDescriptions[e.getKey()] = boneBindPoses.get(e.getKey());
			}

			currentSkinning.compressedVertexWeights = new float[numVertex][];
			currentSkinning.compressedVertexIndices= new int[numVertex][];
			for (int n = 0; n < numVertex; n++) {
				System.out.println(" compressed vertex <"+n+">");
				currentSkinning.compressedVertexWeights[n] = new float[tempStorage[n].size()];
				currentSkinning.compressedVertexIndices[n] = new int[tempStorage[n].size()];
				int a = 0;
				Iterator<Entry<Integer, Float>> i2 = tempStorage[n].entrySet().iterator();
				while (i2.hasNext()) {
					Entry<Integer, Float> e = i2.next();
					currentSkinning.compressedVertexWeights[n][a] = e.getValue();
					currentSkinning.compressedVertexIndices[n][a] = e.getKey().intValue();
					a++;
				}
			}

			infos.put(currentUID, currentSkinning);
			currentSkinning = null;
		}
		
		System.out.println(" and out ");
		
	}
	
	public void finalizeTransformNames(Map<Long, String> map)
	{
		super.finalizeTransformNames(map);
		Iterator<Entry<Long, SkinningInfo>> i = infos.entrySet().iterator();
		while(i.hasNext())
		{
			Entry<Long, SkinningInfo> e = i.next();
			SkinningInfo value = e.getValue();
			value.boneNames = new String[value.boneReferences.length];
			for(int n=0;n<value.boneReferences.length;n++)
			{
				value.boneNames[n] = map.get(value.boneReferences[n]);
			}
			
			namedInfos.put(map.get(e.getKey()), e.getValue());
		}
	}
	
	static public class BindPoseDescription implements Serializable
	{
		Vector3 translation;
		Quaternion rotation;
		Vector3 scale;
		
		Vector3 geometryTranslation;
		Quaternion geometryRotation;
		Vector3 geometryScale;
		
		
		@Override
		public String toString() {
			return "link init <"+translation+" "+rotation+" "+scale+">\n geometry 'current' <"+geometryTranslation+" "+geometryRotation+" "+geometryScale+">";
		}
	}

	static public class SkinningInfo implements Serializable{
		public long[] boneReferences;
		public String[] boneNames;
		public BindPoseDescription[] bindPoseDescriptions;

		public float[][] compressedVertexWeights;

		public int[][] compressedVertexIndices;
		
		@Override
		public String toString() {
			return Arrays.asList(boneNames)+"";
		}
	}
}
