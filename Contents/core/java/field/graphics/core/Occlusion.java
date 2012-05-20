package field.graphics.core;

import static org.lwjgl.opengl.GL15.GL_QUERY_RESULT;
import static org.lwjgl.opengl.GL15.GL_QUERY_RESULT_AVAILABLE;
import static org.lwjgl.opengl.GL15.GL_SAMPLES_PASSED;
import static org.lwjgl.opengl.GL15.glBeginQuery;
import static org.lwjgl.opengl.GL15.glEndQuery;
import static org.lwjgl.opengl.GL15.glGenQueries;
import static org.lwjgl.opengl.GL15.glGetQueryObjectui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import field.graphics.core.Base.StandardPass;
import field.graphics.core.BasicFrameBuffers.SingleFrameBuffer;
import field.graphics.core.BasicGLSLangProgram.ElementType;
import field.graphics.core.BasicGeometry.TriangleMesh_long;
import field.graphics.core.BasicTextures.BaseTexture;
import field.graphics.windowing.FullScreenCanvasSWT;
import field.math.abstraction.iFloatProvider;
import field.math.linalg.Vector2;
import field.namespace.generic.Bind.iFunction;
import field.util.TaskQueue;

public class Occlusion {

	static public class Histogram {
		private float[] bins;
		private BaseTexture source;
		private SingleFrameBuffer dest;

		TaskQueue queue = new TaskQueue();

		int binNum = 0;
		private BasicGLSLangProgram shader;
		private TriangleMesh_long quad;

		Vector2 range = new Vector2(0, 1);
		private ArrayList<AsynchronousQuery> aq;

		Set<AsynchronousQuery> inited = new HashSet<AsynchronousQuery>();

		public Histogram(FullScreenCanvasSWT surface, final float[] bins, final int updateNum, BasicTextures.BaseTexture source, int width, int height) {
			this.bins = bins;
			this.source = source;

			dest = new SingleFrameBuffer(width, height, true, false, false);
			dest.join(surface);
			dest.getSceneList().addChild(new BasicUtilities.OnePassElement(StandardPass.render) {

				@Override
				public void performPass() {
					queue.update();
				}
			});
			

			boolean isRect = source.textureTarget != GL11.GL_TEXTURE_2D;
			float sx = isRect ? width : 1;
			float sy = isRect ? height : 1;
			
			
			
			shader = new BasicGLSLangProgram();
			shader.new BasicGLSLangElement("#version 150\n" + "\n" + "in vec3 position;\n" + "\n" + "uniform mat4 _projMatrix;\n" + "uniform mat4 _viewMatrix;\n" + "\n" + "out vec2 tc;\n" + "\n" + "void main()\n" + "{\n" + "	gl_Position =vec4(position, 1.0);\n" + "\n" + "	tc.xy = (position.xy+vec2(1,1))*0.5;\n" + "\n" + "}", ElementType.vertex);
			shader.new BasicGLSLangElement("#version 150\n" + "\n" + "in vec2 tc;\n" + "out vec4 _output;\n" + "\n" + "uniform sampler2D"+(isRect ? "Rect" : "")+" inp;\n" + "uniform vec2 range;\n" + "\n" + "void main()\n" + "{\n" + "	vec4 x =texture(inp, tc.xy*vec2("+sx+","+sy+"));\n" + "\n" + "	float luma = dot(x.xyz, vec3(1,1,1)/3);\n" + "\n" + "	if (luma<range.x || luma>range.y) discard;\n" + "\n" + "	_output  = vec4(1,0,1,1);\n" + "}\n", ElementType.fragment);
			shader.new SetUniform("range", range);

			quad = new TriangleMesh_long(StandardPass.render);

			quad.rebuildVertex(4);
			quad.rebuildTriangle(2);

			quad.vertex().put(new float[] { -1, -1, 0, 1, -1, 0, 1, 1, 0, -1, 1, 0 });
			quad.longTriangle().put(new int[] { 0, 1, 2, 0, 2, 3 });

			aq = new ArrayList<AsynchronousQuery>();
			for (int i = 0; i < bins.length - 1; i++) {
				aq.add(new AsynchronousQuery("" + i, StandardPass.preTransform, StandardPass.postRender));
			}

			queue.new Task() {

				@Override
				protected void run() {
					for (int i = 0; i < updateNum; i++) {
						update(binNum);
						binNum = (binNum + 1) % (bins.length - 1);
					}
					this.recur();
				}
			};

		}

		protected void update(int binNum) {

			;//System.out.println(" updating biin <" + binNum + ">");

			range.x = bins[binNum];
			range.y = bins[binNum + 1];
			source.pre();
			
			AsynchronousQuery a = aq.get(binNum);
			if (!inited.contains(a)) {
				a.setup();
				inited.add(a);
			}
			a.pre();
			shader.performPass(null);
			quad.performPass(null);
			a.post();

			source.post();
			;//System.out.println(" current value for <" + binNum + "> is <" + a.evaluate() + ">");

		}

	}

	/**
	 * not, the second pass of this thing ends _all_ queries because only
	 * one query can be active at a time
	 * 
	 * @author marc
	 * 
	 */
	static public class AsynchronousQuery extends BasicUtilities.TwoPassElement implements iFloatProvider {

		static public AsynchronousQuery active = null;

		int[] sampleCount = { -1 };

		boolean hasRun = false;

		boolean neverSkip = false;

		int[] query = { -1 };

		private iFunction<Void, Number> f;

		public AsynchronousQuery(String name, StandardPass prePass, StandardPass postPass) {
			super(name, prePass, postPass);
		}

		public float evaluate() {
			return sampleCount[0];
		}

		public void setFunction(iFunction<Void, Number> f) {
			this.f = f;
		}

		public AsynchronousQuery setNeverSkip(boolean neverSkip) {
			this.neverSkip = neverSkip;
			return this;
		}

		@Override
		protected void post() {
			assert active == this;
			glEndQuery(GL_SAMPLES_PASSED);
			active = null;
			hasRun = true;
		}

		@Override
		protected void pre() {

			assert active == null;
			active = this;
			int[] available = { 0 };
			if (hasRun) {
				available[0] = glGetQueryObjectui(query[0], GL_QUERY_RESULT_AVAILABLE);

				if (available[0] != 0 || neverSkip) {
					sampleCount[0] = glGetQueryObjectui(query[0], GL_QUERY_RESULT);

					if (f != null)
						f.f(sampleCount[0]);

				} else {
				}
			}

			glBeginQuery(GL_SAMPLES_PASSED, query[0]);

		}

		@Override
		protected void setup() {
			query[0] = glGenQueries();
			BasicContextManager.putId(this, query[0]);
		}
	}

}
