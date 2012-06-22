package field.graphics.imageprocessing;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import field.graphics.core.AdvancedTextures.OneDTexture;
import field.graphics.core.Base;
import field.graphics.core.Base.iGeometry;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.BasicTextures;
import field.graphics.core.BasicUtilities;
import field.graphics.core.StereoCamera;
import field.graphics.imageprocessing.ImageProcessing.iProcessesMesh;
import field.math.abstraction.iFloatProvider;
import field.math.abstraction.iProvider;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.math.linalg.iToFloatArray;


import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL41.*;
import static org.lwjgl.opengl.ARBShaderObjects.*;

public class Processors {

	static public class AddGrain extends BasicGLSLangProgram implements iProcessesMesh {
		static public Vector4 lightGrain = new Vector4(0, 0, 0.1f, 0);

		static public Vector4 heavyGrain = new Vector4(0, 0, 0.5f, 0);

		static public Vector4 grainWhite = new Vector4(0.5f, 0, 0f, 0);

		private ByteBuffer saltOneDTextureBuffer;

		private OneDTexture saltColorRemap;

		private final Grainer grainer;

		Vector4 feedbackParameters = new Vector4();

		Vector4 noiseParameters = new Vector4(0, 0, 0.1f, 0);

		int saltSize = 64;

		public AddGrain() {
			super("content/shaders/AddGrainVertex.glslang", "content/shaders/processors/AddGrainFragment.glslang");

			grainer = new Grainer(this);

			new SetUniform("noiseParameters", noiseParameters);

		}

		public void process(iGeometry process) {

			grainer.updateGrain(process);
		}

		public void setNoiseParameters(Vector4 noiseParameters) {
			this.noiseParameters.setValue(noiseParameters);
		}

	}

	static public class Combine2Lighting extends BasicGLSLangProgram
	{
		public Combine2Lighting()
		{
			super("content/shaders/NDCVertex.glslang", "content/shaders/processors/Combine2LightingFragment.glslang");

			new SetIntegerUniform("lighting0", 0);
			new SetIntegerUniform("lighting1", 1);
		}
	}

	static public class Grainer {
		private final ByteBuffer saltOneDTextureBuffer;

		private final OneDTexture saltColorRemap;

		int saltSize = 64;

		int c = 0;

		public Grainer(BasicGLSLangProgram prog) {
			saltOneDTextureBuffer = ByteBuffer.allocateDirect(4 * saltSize);
			for (int i = 0; i < saltSize; i++) {
				// xRGB
				saltOneDTextureBuffer.put((byte) (255 * StereoCamera.getRandomNumber()));
				saltOneDTextureBuffer.put((byte) (255 * StereoCamera.getRandomNumber()));
				saltOneDTextureBuffer.put((byte) (255 * StereoCamera.getRandomNumber()));
				saltOneDTextureBuffer.put((byte) (255 * StereoCamera.getRandomNumber()));
			}
			saltOneDTextureBuffer.rewind();

			saltColorRemap = new OneDTexture(saltOneDTextureBuffer, saltSize);
			saltColorRemap.gl_texture_wrap_s(GL_REPEAT);
			saltColorRemap.gl_texture_wrap_t(GL_REPEAT);
			prog.addChild(new BasicTextures.TextureUnit(4, saltColorRemap));
			prog.new SetIntegerUniform("salt", new iProvider.Constant<Integer>(4));
		}

		public void updateGrain(Base.iGeometry g) {
			FloatBuffer t0 = g.aux(Base.texture0_id + 1,4);

			for (int i = 0; i < t0.capacity(); i++)
				t0.put((float) (2147483647 * 50000.0 * StereoCamera.getRandomNumber()));

			for (int i = 0; i < saltSize; i++) {
				saltOneDTextureBuffer.put((byte) (255 * StereoCamera.getRandomNumber()));
				saltOneDTextureBuffer.put((byte) (255 * StereoCamera.getRandomNumber()));
				saltOneDTextureBuffer.put((byte) (255 * StereoCamera.getRandomNumber()));
				saltOneDTextureBuffer.put((byte) (255 * StereoCamera.getRandomNumber()));
			}
			saltOneDTextureBuffer.rewind();
				saltColorRemap.dirty();

		}public void update() {

			for (int i = 0; i < saltSize; i++) {
				saltOneDTextureBuffer.put((byte) (255 * StereoCamera.getRandomNumber()));
				saltOneDTextureBuffer.put((byte) (255 * StereoCamera.getRandomNumber()));
				saltOneDTextureBuffer.put((byte) (255 * StereoCamera.getRandomNumber()));
				saltOneDTextureBuffer.put((byte) (255 * StereoCamera.getRandomNumber()));
			}
			saltOneDTextureBuffer.rewind();
				saltColorRemap.dirty();

		}
	}

	static public class Simple7Kernel extends SimpleKernel {

		static public final Vector3[] vert7 = { new Vector3(0, -3.75, 0.05f), new Vector3(0, -2.5, 0.1f), new Vector3(0, -1.25, 0.2f), new Vector3(0, 1.25, 0.2f), new Vector3(0, 2.5, 0.1f), new Vector3(0, 3.75, 0.05f)};
		static public final float vert7_center = 1f;
		static public final Vector3[] horiz7= { new Vector3(-3.75, 0, 0.05f), new Vector3(-2.5, 0, 0.1f), new Vector3(-1.25, 0, 0.2f), new Vector3(1.25, 0, 0.2f), new Vector3(2.5, 0, 0.1f), new Vector3(3.75, 0, 0.05f)};
		static public final float horiz7_center = 1f;

		private float cw;
		public Simple7Kernel(iFloatProvider scale, Vector3[] offsets, final float centerWeight) {
			super(scale, offsets, "content/shaders/VertBlur7NDCvertex.glslang", "content/shaders/processors/VertBlur7Fragment.glslang");
			this.cw = centerWeight;
			new SetUniform("centerWeight", new iToFloatArray(){
				public float[] get() {
					return new float[] { cw};
				}
			});

			float total = cw;
			for(int i=0;i<offsets.length;i++)
			{
				float a = Math.abs(i-(offsets.length-1)/2f)/((offsets.length-1)/2f);
				
				total+=offsets[i].z = (float) Math.exp(-a*2);
				//total+=1;
				//offsets[i].z = 1;
			}

			//total/=1.1f;

			for(int i=0;i<offsets.length;i++)
			{
				offsets[i].z/=total;
			}

			cw = 1f/total;

		}

	}

	static public class SimpleCrossfader extends BasicGLSLangProgram {
		private final iFloatProvider oneToTwo;

		public SimpleCrossfader(final iFloatProvider oneToTwo, final float power) {
			super("content/shaders/NDCvertex.glslang", "content/shaders/processors/SimpleCrossfaderFragment.glslang");
			this.oneToTwo = oneToTwo;

			new SetUniform("crossfade", new iToFloatArray(){

				public float[] get() {
					float e = oneToTwo.evaluate();

					float a1 = 1 - e;
					float a2 = e;

					float norm = (float) Math.pow(Math.pow(a1, power) + Math.pow(a2, power), 1f / power);

					a1 /= norm;
					a2 /= norm;

					if (a1>1) a1 = 1;
					if (a2>1) a2 = 1;
					if (a1<0) a1 = 0;
					if (a2<0) a2 = 0;
					if (Float.isNaN(a1)) a1 = 0;
					if (Float.isNaN(a2)) a2 = 0;

					return new float[] { a1, a2, 0, 0};

				}
			}

			);
			new SetIntegerUniform("texture1", new iProvider.Constant<Integer>(0));
			new SetIntegerUniform("texture2", new iProvider.Constant<Integer>(1));
		}
	}

	static public class SimpleFeedbackDOFKernel extends SimpleKernel {
		static public final Vector4 onlyNewMaterial = new Vector4(0, 0, 1, 0);

		static public final Vector4 mediumFeedback = new Vector4(0.5, 1.9f, 1, 0);

		static public final Vector4 largeFeedback = new Vector4(0.95, 1.05f, 1, 0);

		static public final Vector4 trickle = new Vector4(0.95f, 1, 1, 0);

		static public final Vector4 halfAndHalf = new Vector4(0.75f, 1, 1, 0);

		Vector4 feedbackParameters = new Vector4();

		public SimpleFeedbackDOFKernel(iFloatProvider scale, Vector3[] offsets) {
			super(scale, offsets, "content/shaders/VertBlurNDCvertex.glslang", "content/shaders/processors/VertBlurDOFFeedbackFragment.glslang");
			new SetUniform("feedbackParameters", feedbackParameters);
			new SetIntegerUniform("depthMap", 1);
			new SetIntegerUniform("oldMaterial", 2);
			relativeOffsets = true;
			randomOffsets = true;
		}

		@Override
		public void process(iGeometry process) {
			// add texture offsets
			FloatBuffer oTexture = process.aux(Base.texture0_id, 2);

			FloatBuffer[] o = new FloatBuffer[5];
			for (int i = 0; i < o.length; i++)
				o[i] = process.aux(10 + i, 3);
			float s = scale.evaluate();

			for (int j = 0; j < o.length; j++) {
				for (int i = 0; i < oTexture.capacity() / 2; i++) {
					float x = oTexture.get(i * 2 + 0);
					float y = oTexture.get(i * 2 + 1);
					o[j].put((float) (x * (relativeOffsets ? 0 : 1) + offsets[j].x * s * (randomOffsets ? (StereoCamera.getRandomNumber() + 3) : 3)));
					o[j].put((float) (y * (relativeOffsets ? 0 : 1) + offsets[j].y * s * (randomOffsets ? (StereoCamera.getRandomNumber() + 3) : 3)));
					o[j].put(offsets[j].z);
				}
			}
		}

		public void setFeedbackParameters(Vector4 feedbackParameters) {
			this.feedbackParameters.setValue(feedbackParameters);
		}
	}

	static public class SimpleFeedbackKernel extends SimpleKernel {
		static public final Vector4 onlyNewMaterial = new Vector4(0, 0, 1, 0);

		static public final Vector4 mediumFeedback = new Vector4(0.5, 1.9f, 1, 0);

		static public final Vector4 largeFeedback = new Vector4(0.95, 1.05f, 1, 0);

		static public final Vector4 trickle = new Vector4(0.95f, 1, 1, 0);

		static public final Vector4 halfAndHalf = new Vector4(0.75f, 1, 1, 0);

		Vector4 feedbackParameters = new Vector4();

		public SimpleFeedbackKernel(iFloatProvider scale, Vector3[] offsets) {
			super(scale, offsets, "content/shaders/VertBlurNDCvertex.glslang", "content/shaders/processors/VertBlurFeedbackFragment.glslang");
			new SetUniform("feedbackParameters", feedbackParameters);
			new SetIntegerUniform("oldMaterial", 1);
		}

		public void setFeedbackParameters(Vector4 feedbackParameters) {
			this.feedbackParameters.setValue(feedbackParameters);
		}
	}

	static public class SimpleKernel extends BasicGLSLangProgram implements iProcessesMesh {
		static public final Vector3[] vertical = { new Vector3(0, -1.5, 0.1f), new Vector3(0, -0.75, 0.2f), new Vector3(0, -0, 0.4f), new Vector3(0, 0.75, 0.2f), new Vector3(0, 1.5, 0.1f)};

		static public final Vector3[] horizontal = { new Vector3(-1.5, 0, 0.1f), new Vector3(-0.75, 0, 0.2f), new Vector3(0, 0, 0.4f), new Vector3(0.75, 0, 0.2f), new Vector3(1.5, 0, 0.1f)};

		static public final Vector3[] vertical2 = { new Vector3(0, -2, 0.2f), new Vector3(0, -1, 0.2f), new Vector3(0, 0, 0.2f), new Vector3(0, 1, 0.2f), new Vector3(0, 2, 0.2f)};

		static public final Vector3[] horizontal2 = { new Vector3(-2, 0, 0.2f), new Vector3(-1, 0, 0.2f), new Vector3(0, 0, 0.2f), new Vector3(1, 0, 0.2f), new Vector3(2, 0, 0.2f)};

		static public final Vector3[] laplacian = { new Vector3(-1, -1, -1), new Vector3(1, -1, -1), new Vector3(0, 0, 4), new Vector3(1, 1, -1), new Vector3(-1, 1, -1)};

		static public final Vector3[] omni = { new Vector3(-1, -1, 0.2f), new Vector3(1, -1, 0.2f), new Vector3(0, 0, 0.2f), new Vector3(1, 1, 0.2f), new Vector3(-1, 1, 0.2f)};

		static public final Vector3[] omni2 = { new Vector3(-1, -1, 0.1f), new Vector3(1, -1, 0.1f), new Vector3(0, 0, 0.6f), new Vector3(1, 1, 0.1f), new Vector3(-1, 1, 0.1f)};

		static public final Vector4 alpha_noBlending = new Vector4(0, 1, 1, 0);

		static public final Vector4 longLPF = new Vector4(0, 0.05f, 1, 0);

		static public final Vector4 longLPF2 = new Vector4(0.05f, 0.01f, 0.1f, 0);

		private boolean attachNoBlending;

		protected final iFloatProvider scale;

		protected Vector3[] offsets;

		Vector4 alphaParameters = new Vector4(0, 1, 1, 0);

		boolean relativeOffsets = false;

		public boolean randomOffsets = false;

		int c = 0;

		public SimpleKernel(iFloatProvider scale, Vector3[] offsets) {
			super("content/shaders/VertBlurNDCvertex.glslang", "content/shaders/processors/VertBlurFragment.glslang");
			this.scale = scale;
			this.offsets = offsets;
			new SetIntegerUniform("center", 0);
			new SetUniform("alphaParameters", alphaParameters);
			this.relativeOffsets = true;
		}

		protected SimpleKernel(iFloatProvider scale, Vector3[] offsets, String vertex, String fragment) {
			super(vertex, fragment);
			this.scale = scale;
			this.offsets = offsets;
			new SetIntegerUniform("center", 0);
			new SetUniform("alphaParameters", alphaParameters);
			this.relativeOffsets = true;
		}

		int num = 0;
		
		public void process(iGeometry process) {
			if (num++>5) return;
			
			// add texture offsets
			FloatBuffer oTexture = process.aux(Base.texture0_id, 2);

			FloatBuffer[] o = new FloatBuffer[offsets.length];
			for (int i = 0; i < o.length; i++)
				o[i] = process.aux(10 + i, 3);
			float s = scale.evaluate();

			for (int j = 0; j < o.length; j++) {
				
				//;//System.out.println(" offset <"+o+">");
				
				for (int i = 0; i < oTexture.capacity() / 2; i++) {
					float x = oTexture.get(i * 2 + 0);
					float y = oTexture.get(i * 2 + 1);
					float a = (float) (x * (relativeOffsets ? 0 : 1) + offsets[j].x * s * (randomOffsets ? (StereoCamera.getRandomNumber() - 0.5) : 1));
					o[j].put(a);
					float b = (float) (y * (relativeOffsets ? 0 : 1) + offsets[j].y * s * (randomOffsets ? (StereoCamera.getRandomNumber() - 0.5) : 1));
					o[j].put(b);
					o[j].put(offsets[j].z);
										
				}
			}

			if (attachNoBlending)
			{
				process.addChild(new BasicUtilities.DisableBlending());
				attachNoBlending = false;
			}
		}

		public void setAlphaParameters(Vector4 alphaParameters) {
			this.alphaParameters.setValue(alphaParameters);

			if (this.alphaParameters == alpha_noBlending)
			{
				attachNoBlending = true;
			}

		}
	}


	static public class SimpleKernel_randomOffset extends SimpleKernel {

		public SimpleKernel_randomOffset(iFloatProvider scale, Vector3[] offsets) {
			super(scale, offsets);
		}

		@Override
		public void process(iGeometry process) {

			// add texture offsets
			FloatBuffer oTexture = process.aux(Base.texture0_id, 2);

			FloatBuffer[] o = new FloatBuffer[5];
			for (int i = 0; i < o.length; i++)
				o[i] = process.aux(10 + i, 3);
			float s = scale.evaluate();

			for (int j = 0; j < o.length; j++) {
				for (int i = 0; i < oTexture.capacity() / 2; i++) {
					float x = oTexture.get(i * 2 + 0);
					float y = oTexture.get(i * 2 + 1);
					o[j].put((float) (x + offsets[j].x * (s * (StereoCamera.getRandomNumber() - 0.5))));
					o[j].put((float) (y + offsets[j].y * (s * (StereoCamera.getRandomNumber() - 0.5))));
					o[j].put(offsets[j].z);
				}
			}
		}

	}

}
