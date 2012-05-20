package field.graphics.jfbxlib;

import java.nio.FloatBuffer;
import java.util.Map;

import field.graphics.core.Base;
import field.graphics.core.Base.iGeometry;
import field.graphics.jfbxlib.BuildSkinningVisitor.SkinningInfo;
import field.math.abstraction.iInplaceProvider;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Matrix4;
import field.math.linalg.Vector3;


/**
 * the first jfbx / xstream compatiable skinner
 *
 * @author marc
 *
 */
public class SlowSkinner {

	public class CurrentLinkInfo {
		String boneReference;

		iInplaceProvider<CoordinateFrame> frameProvider;

		BuildSkinningVisitor.BindPoseDescription bindInfo;

		Matrix4 bound = new Matrix4();

		Matrix4 inverseGeometry = new Matrix4();

		Matrix4 current = new Matrix4();
	}

	public final Map<String, iInplaceProvider<CoordinateFrame>> uidToProviders;

	private final SkinningInfo skinningInfo;

	private int[][] compressedVertexIndices;

	// some temp per vertex storage

	private float[][] compressedVertexWeights;

	CurrentLinkInfo[] currentLinkInfo;

	Matrix4 temp = new Matrix4();

	public SlowSkinner(Map<String, iInplaceProvider<CoordinateFrame>> uidToProviders, BuildSkinningVisitor.SkinningInfo skinningInfo) {
		this.uidToProviders = uidToProviders;
		this.skinningInfo = skinningInfo;

		currentLinkInfo = new CurrentLinkInfo[skinningInfo.boneReferences.length];
		int maxVertex = Integer.MIN_VALUE;
		int minVertex = Integer.MAX_VALUE;
		for (int i = 0; i < skinningInfo.boneReferences.length; i++) {
			currentLinkInfo[i] = new CurrentLinkInfo();
			currentLinkInfo[i].boneReference = skinningInfo.boneNames[i];
			currentLinkInfo[i].frameProvider = uidToProviders.get(currentLinkInfo[i].boneReference);

			assert (currentLinkInfo[i].frameProvider != null) : "couldn't find frame provider for uid <" + currentLinkInfo[i].boneReference + "> in <" + uidToProviders + ">";
			currentLinkInfo[i].bindInfo = skinningInfo.bindPoseDescriptions[i];
			compressedVertexIndices = skinningInfo.compressedVertexIndices;
			compressedVertexWeights = skinningInfo.compressedVertexWeights;

			// compute initial matrices

			;//;//System.out.println(" initial bind pose description is <"+skinningInfo.bindPoseDescriptions[i]+">");
			
			currentLinkInfo[i].bound.set(skinningInfo.bindPoseDescriptions[i].rotation, skinningInfo.bindPoseDescriptions[i].translation, skinningInfo.bindPoseDescriptions[i].scale);
			
			;//;//System.out.println(skinningInfo.boneNames[i]+" "+skinningInfo.bindPoseDescriptions[i].geometryRotation +" "+skinningInfo.bindPoseDescriptions[i].geometryTranslation+" "+skinningInfo.bindPoseDescriptions[i].geometryScale);
			currentLinkInfo[i].inverseGeometry.set(skinningInfo.bindPoseDescriptions[i].geometryRotation, skinningInfo.bindPoseDescriptions[i].geometryTranslation, skinningInfo.bindPoseDescriptions[i].geometryScale);

			//;//;//System.out.println(" initial link matricies\n bound:" + currentLinkInfo[i].bound + "\n inverseGeometry (not invert yet):" + currentLinkInfo[i].inverseGeometry);
			;//;//System.out.println(" about to invert <"+currentLinkInfo[i].inverseGeometry+">");
			currentLinkInfo[i].inverseGeometry.invert();
		}

		maxVertex = compressedVertexIndices.length;

	}

	public void performSkinning(iGeometry bindMesh, iGeometry destMesh) {
		assert bindMesh.vertex().capacity() / 3 == compressedVertexIndices.length;
		CoordinateFrame o = new CoordinateFrame();
		Matrix4 centerMatrix = new Matrix4();
		for (int i = 0; i < currentLinkInfo.length; i++) {
			CurrentLinkInfo cli = currentLinkInfo[i];
			cli.frameProvider.get(o);

			o.getMatrix(centerMatrix);

			// correct order?
			cli.current.mul(cli.inverseGeometry, centerMatrix);
			centerMatrix.mul(cli.current, cli.bound);

			cli.current.set(centerMatrix);

		}

		Matrix4 temp = new Matrix4();
		Vector3 positionIn = new Vector3();
		Vector3 positionOut = new Vector3();

		FloatBuffer vertexIn = bindMesh.vertex();
		FloatBuffer vertexOut = destMesh.vertex();

		FloatBuffer normalIn = bindMesh.hasAux(Base.normal_id) ?  bindMesh.aux(Base.normal_id, 3) : null;
		FloatBuffer normalOut = (normalIn!=null) ?  destMesh.aux(Base.normal_id, 3) : null;


		for (int v = 0; v < compressedVertexIndices.length; v++) {
			float totalWeight = 0;
			temp.zero();


			for (int i = 0; i < compressedVertexIndices[v].length; i++) {
				Matrix4 m = currentLinkInfo[compressedVertexIndices[v][i]].current;
				float w = compressedVertexWeights[v][i];

				temp.madd(w, m);

				totalWeight += w;
			}

			assert totalWeight > 0;
			temp.mul(1 / totalWeight);

			int a0 = v * 3 + 0;
			int a1 = v * 3 + 1;
			int a2 = v * 3 + 2;

			positionIn.x = vertexIn.get(a0);
			positionIn.y = vertexIn.get(a1);
			positionIn.z = vertexIn.get(a2);

			temp.transformPosition(positionIn, positionOut);

			if (false)
			{
			;//;//System.out.println(" skinning update <"+positionIn+" -> "+positionOut+"> <"+totalWeight+"> via <"+temp+">");

			if (positionIn.distanceFrom(positionOut)>2)
			{
				;//;//System.out.println(" ------- went somewhere, why?");
				for(int i=0;i<compressedVertexIndices[v].length;i++)
				{
					CurrentLinkInfo li = currentLinkInfo[compressedVertexIndices[v][i]];
					;//;//System.out.println(" bone <"+li.boneReference+">\ncurrently\n"+li.current+"\nfrom:"+li.frameProvider.get(new CoordinateFrame())+"\nand\n"+li.inverseGeometry+"\nand\n"+li.bound);
				}
			}
			}
			vertexOut.put(a0, positionOut.x);
			vertexOut.put(a1, positionOut.y);
			vertexOut.put(a2, positionOut.z);

			if (normalIn!=null)
			{
				positionIn.x = normalIn.get(a0);
				positionIn.y = normalIn.get(a1);
				positionIn.z = normalIn.get(a2);

				temp.transformDirection(positionIn, positionOut);


				normalOut.put(a0, positionOut.x);
				normalOut.put(a1, positionOut.y);
				normalOut.put(a2, positionOut.z);
			}


		}
	}

	public void skinPoint(Vector3 vInOut, int weightsFromVertex)
	{
		float totalWeight = 0;
		temp.zero();


		for (int i = 0; i < compressedVertexIndices[weightsFromVertex].length; i++) {
			Matrix4 m = currentLinkInfo[compressedVertexIndices[weightsFromVertex][i]].current;
			float w = compressedVertexWeights[weightsFromVertex][i];

			temp.madd(w, m);

			totalWeight += w;
		}

		assert totalWeight > 0;
		temp.mul(1 / totalWeight);


		Vector3 positionIn = new Vector3(vInOut);

		temp.transformPosition(positionIn, vInOut);


	}

}
